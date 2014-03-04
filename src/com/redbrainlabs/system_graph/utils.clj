(ns com.redbrainlabs.system-graph.utils
  "Utillity fns for working with Prismatic's Graph and fnk"
  (:require [plumbing.graph :as graph]
            [schema.core :as s]))

(defn topo-sort
  "Returns the topological sort of a Prismatic graph"
  [g]
  (-> g graph/->graph keys vec))

;; TODO: see if plumbing already has a fn for this... or helper fns that seem less intrusive..
(defn fnk-deps [fnk]
  (->> fnk
       s/fn-schema
       :input-schemas ;; [[#schema.core.One{:schema {Keyword Any, :funk Any, :x Any}, :optional? false, :name arg0}]]
       ffirst
       :schema
       keys
       (filter keyword?)))

;; TODO: see if plumbing's `comp-partial` does what I need (as suggested by Jason Wolfe)
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
