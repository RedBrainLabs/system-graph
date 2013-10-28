(ns com.redbrainlabs.system-graph.utils
  "Utillity fns for working with Prismatic's Graph and fnk"
  (:require [plumbing.graph :as graph]))

(defn topo-sort
  "Returns the topological sort of a Prismatic graph"
  [g]
  ;; Prismatic Graphs are stored in an array-map *in* topological sort.  It would be
  ;; better if Graph provided a fn like this so we wouldn't have to rely on the
  ;; implementation details of it and of array-map...
  (-> g graph/->graph keys vec))

(defn comp-fnk
  "Composes the given given fnk with the provided fn. Only handles the binary case."
  [f fnk]
  ;; TODO: handle other fnks (verifying input/output schemas) and the variadic case
  (let [comped (-> (comp f fnk)
                   (with-meta (meta fnk)))]
    ;; compose the positional function as well if present
    (if [(-> fnk meta :plumbing.fnk.impl/positional-info)]
      (vary-meta comped update-in [:plumbing.fnk.impl/positional-info 0] (partial comp f))
      comped)))
