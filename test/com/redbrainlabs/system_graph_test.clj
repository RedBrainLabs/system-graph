(ns com.redbrainlabs.system-graph-test
  (:require [com.stuartsierra.component :refer [Lifecycle] :as component]
            [plumbing.core :refer [fnk]]
            [midje.sweet :refer :all]

            [com.redbrainlabs.system-graph :refer :all]))


(def lifecycle-sort :com.redbrainlabs.system-graph/lifecycle-sort)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Integration tests... since this library is 100% glue

(defrecord Lifecycler [!started !stopped key value]
  Lifecycle
  (start [this]
    (swap! !started conj key)
    this)
  (stop [this]
    (swap! !stopped conj key)
    this))

(facts "system-graph"
  (let [deps-started (atom [])
        deps-stopped (atom [])
        lifecycler (partial ->Lifecycler deps-started deps-stopped)
        subgraph {:x-squared (fnk [x] (lifecycler :x-squared (* x x)))
                  :x-cubed (fnk [x x-squared] ;;(prn {:x x :x-squared x-squared})
                                              (lifecycler :x-cubed (* x (:value x-squared))))}
        graph {:subgraph subgraph
               :y (fnk [[:subgraph x-cubed]] (lifecycler :y (inc (:value x-cubed))))
               :x-inc (fnk [x] (lifecycler :x-inc (inc x)))
               :no-lifecycle (fnk [x-inc] :something-that-doesnt-implement-lifecycle)}
        system-graph (init-system graph {:x 4})]

    (fact "starts the lifecycle deps using a toposort"
      (component/start system-graph)
      @deps-started => [:x-squared :x-cubed :y :x-inc])
    (fact "stops the deps in the opposite order"
      (component/stop system-graph)
      @deps-stopped => [:x-inc :y :x-cubed :x-squared])))

(defrecord DummyComponent [name started]
  Lifecycle
  (start [this] (assoc this :started true))
  (stop [this] (assoc this :started false)))

(defn dummy-component [name]
  (->DummyComponent name false))

(facts "dependent components"
  (let [graph {:a (fnk []
                       (dummy-component :a))
               :b (fnk [a]
                       (-> (dummy-component :b)
                           (assoc :a a)))}
        system-graph (init-system graph {})
        started-system (start-system system-graph)]
    (facts "are passed in to fnks before they are started"
      (:b system-graph) => (just {:a {:name :a, :started false}, :name :b, :started false}))
    (facts "are started and assoced onto lifecycle components before then dependent's #'start is called"
      (:b started-system) => (just {:a {:name :a, :started true}, :name :b, :started true}))))

(facts "dependent components with different names that the system's names"
  (let [graph {:a (fnk []
                       (dummy-component :a))
               :b (-> (fnk [a]
                           (-> (dummy-component :b)
                               (assoc :foo a)))
                      (component/using {:foo :a}))}
        system-graph (init-system graph {})
        started-system (start-system system-graph)]
    (facts "are passed in to fnks before they are started"
      (:b system-graph) => (just {:foo {:name :a, :started false}, :name :b, :started false}))
    (facts "are started and assoced onto lifecycle components with the different name before then dependent's #'start is called"
      (:b started-system) => (just {:foo {:name :a, :started true}, :name :b, :started true}))))

(defrecord StatefulDummyComponent [name started !counter]
  Lifecycle
  (start [this] (swap! !counter update-in [:started] inc) (assoc this :started true))
  (stop [this] (swap! !counter update-in [:stopped] inc) (assoc this :started false)))

(defn stateful-dummy-component [name]
  (->StatefulDummyComponent name false (atom {:started 0 :stopped 0})))

(facts "systems can be started and stopped multiple times"
  (let [graph {:a (fnk []
                       (stateful-dummy-component :a))
               :b (fnk [a]
                       (-> (stateful-dummy-component :b)
                           (assoc :a a)))}
        system-graph (init-system graph {})
        cycled-system (reduce (fn [sys _] (-> sys start-system stop-system)) system-graph (range 5))]


    (-> cycled-system :a :!counter deref) => {:started 5, :stopped 5}
    (-> cycled-system start-system :b :a :started) => true
    (-> cycled-system start-system stop-system :b :started) => false
    ;; TODO: investgate this to see if this is a system-graph bug or component..
    ;; it looks like component doesn't assoc the deps on stop.. but the newer version says it does..
    ;; (-> cycled-system start-system stop-system :b :a :started) => false
    ))
