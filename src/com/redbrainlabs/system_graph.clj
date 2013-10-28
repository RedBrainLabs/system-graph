(ns com.redbrainlabs.system-graph
  "Helper fns for Prismatic's Graph and SystemGraph fns for using Lifecycle"
  (:require [com.stuartsierra.component :refer [Lifecycle] :as lifecycle]
            [plumbing
             [core  :as plumbing]
             [graph :as graph]]
            [schema.core :as s]

            [com.redbrainlabs.system-graph.utils :refer [topo-sort comp-fnk]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Schemas

(defn- has-lifecycle-sort? [obj]
  (when-let [{:keys [lifecycle-sort]} (meta obj)]
    (every? keyword? lifecycle-sort)))

(def SystemGraph* (s/pred has-lifecycle-sort? "contains :lifecycle-sort in metadata"))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Private

(defn- map-vals-ordered [f init ordered-keys]
  ;; TODO: move in our error handling for when a component fails to start...
  (reduce (fn [m* key]
            (update-in m* [key] f))
          init
          ordered-keys))

(declare start-system stop-system)

(defrecord SystemGraph []
  Lifecycle
  (start [system-graph]
    (start-system system-graph))
  (stop [system-graph]
    (stop-system system-graph)))

(defn- lifecycle-components
  "Filters the component-keys to only the vals of computed-system that satisfy Lifecycle.
   The filtering preseves the ordering of the orginal component-keys."
  [component-keys computed-system]
  (->> component-keys
       (map (fn [k] [k (get computed-system k)]))
       (filter (fn [[k component]] (satisfies? Lifecycle component)))
       (mapv first)))

(defn- system-graph [original-graph computed-system]
  (let [topo-sort (topo-sort original-graph)]
    (-> (map->SystemGraph computed-system)
        (with-meta
          {:topo-sort topo-sort
           :lifecycle-sort (lifecycle-components topo-sort computed-system)}))))

(defn lifecycle-toposort [system-graph]
  (s/validate SystemGraph* system-graph)
  (-> system-graph meta :lifecycle-sort))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Public

(defn compile-as-system-graph
  "Compiles a Prismatic graph into using compile whose resulting fnk will return
   SystemGraphs that comply with Lifecycle"
  [compile g]
  (if (fn? g)
    g
    (let [g (-> (plumbing/map-vals (partial compile-as-system-graph compile) g) graph/->graph)
          fnk (compile g)]
      (comp-fnk (partial system-graph g) fnk))))

(def eager-compile-system-graph
  (partial compile-as-system-graph graph/eager-compile))

(def eager-interpreted-system-graph
  (partial compile-as-system-graph graph/interpreted-eager-compile))

(defn init-system
  "Analogous to graph/run, initializes the graph as a SystemGraph with the given input. "
  [g input]
  ((eager-interpreted-system-graph g) input))

(defn start-system
  "Recursively starts the system components in the correct order as implicity defined in the graph."
  [system-graph]
  (map-vals-ordered lifecycle/start system-graph (-> system-graph lifecycle-toposort)))

(defn stop-system
  "Recursively stops the system components in the reverse order in which they were started."
  [system-graph]
  (map-vals-ordered lifecycle/stop system-graph (-> system-graph lifecycle-toposort reverse)))
