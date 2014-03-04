 (ns com.redbrainlabs.system-graph
  (:require [clojure.set :as set]

            [com.stuartsierra.component :refer [Lifecycle] :as component]
            [plumbing.core :as plumbing]
            [plumbing.graph :as graph]
            [schema.core :as s]

            [com.redbrainlabs.system-graph.utils :refer [topo-sort comp-fnk fnk-deps]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Private

(defn- lifecycle-components
  "Filters the component-keys to only the vals of computed-system that satisfy Lifecycle.
  The filtering preseves the ordering of the original component-keys."
  [component-keys computed-system]
  (->> component-keys
       (map (fn [k] [k (get computed-system k)]))
       (filter (fn [[k component]]
                 (and
                  (satisfies? Lifecycle component)
                  ;; this check is needed since 'component' now includes a default
                  ;; impl on Object... but there is an implicit requirement that
                  ;; the component is an IObj for metadata support.
                  (isa? (class component) clojure.lang.IObj))))
       (mapv first)))

(defn- attach-component-metadata [computed-system original-graph compiled-fnk lifecycle-comps]
  (let [passed-in-args (fnk-deps compiled-fnk)
        lifecycle-comps (set lifecycle-comps)
        lifecycle-deps (->> lifecycle-comps
                            (map (fn [k]
                                   (->> k
                                        (get original-graph)
                                        fnk-deps
                                        set
                                        (set/intersection lifecycle-comps)
                                        vec)))
                            (zipmap lifecycle-comps))]
    (reduce-kv (fn [m* k deps] (update-in m* [k] component/using deps))
               computed-system lifecycle-deps)))

(defn- create-system-map [original-graph compiled-fnk computed-system]
  (let [lifecycle-comps (lifecycle-components (topo-sort original-graph) computed-system)]
    (-> computed-system
        (attach-component-metadata original-graph compiled-fnk lifecycle-comps)
        component/map->SystemMap)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Public

(defn compile-as-system-graph
  "Compiles a Prismatic graph using `compile` and wraps the functionality so that
  the resulting fnk will return a 'component' SystemMap that complies with Lifecycle.

  All of the dependency information implicyt with the graph and fnks is carried over
  into the SystemMap and component vals using `component/using`. "
  [compile g]
  (if (fn? g)
    g
    (let [g (-> (plumbing/map-vals (partial compile-as-system-graph compile) g) graph/->graph)
          fnk (compile g)]
      (comp-fnk (partial create-system-map g fnk) fnk))))

(def eager-compile
  "Performs a #'plumbing.graph/eager-compile on the graph so that all the
  computed results from the compiled fnk are SystemGraphs which satisfy Lifecycle."
  (partial compile-as-system-graph graph/eager-compile))

(def eager-interpreted
  "Performs a #'plumbing.graph/eager-compile on the graph so that all the
  computed results from the compiled fnk are SystemGraphs which satisfy Lifecycle."
  (partial compile-as-system-graph graph/interpreted-eager-compile))

(defn init-system
  "Analogous to #'plumbing.graph/run, initializes the graph as a SystemGraph with the given input. "
  [g input]
  ((eager-interpreted g) input))

(defn start-system
  "Recursively starts the system components in the correct order as implicity defined in the graph."
  [system-graph]
  (component/start-system system-graph))

(defn stop-system
  "Recursively stops the system components in the reverse order in which they were started."
  [system-graph]
  (component/stop-system system-graph))
