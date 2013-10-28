(ns com.redbrainlabs.system-graph-test
  (:require [com.stuartsierra.component :refer [Lifecycle] :as lifecycle]
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
                  :x-cubed (fnk [x x-squared] (lifecycler :x-cubed (* x (:value x-squared))))}
        graph {:subgraph subgraph
               :y (fnk [[:subgraph x-cubed]] (lifecycler :y (inc (:value x-cubed))))
               :x-inc (fnk [x] (lifecycler :x-inc (inc x)))
               :no-lifecycle (fnk [x-inc] :something-that-doesnt-implement-lifecycle)}
        system-graph (init-system graph {:x 4})]

    (fact "only tries to start/stop components that satisfy Lifecycle"
      (-> system-graph meta lifecycle-sort) => [:x-inc :subgraph :y])
    (fact "works with hierarchical graphs"
      (-> system-graph :subgraph meta lifecycle-sort) => [:x-squared :x-cubed])
    (fact "starts the deps using the toposort"
      (lifecycle/start system-graph) => system-graph
      @deps-started => [:x-inc :x-squared :x-cubed :y])
    (fact "stops the deps in the opposite order"
      (lifecycle/stop system-graph) => system-graph
      @deps-stopped => [:y :x-cubed :x-squared :x-inc])))
