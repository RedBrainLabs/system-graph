(ns com.redbrainlabs.system-graph.utils-test
  (:require [midje.sweet :refer :all]
            [plumbing.core :refer [fnk]]
            [plumbing.graph :as graph]
            [schema.core :as s]

            [com.redbrainlabs.system-graph.utils :refer :all]))

(facts "#'topo-sort"
  ;; sanity/characteristic test since we are relying on Graph's impl..
  (topo-sort {:a (fnk [x] (inc x))
              :b (fnk [a] (inc a))
              :c (fnk [b] (inc b))}) => [:a :b :c])

(facts "#'comp-fnk"
  (let [square-fnk (fnk [x] (* x x))
        with-inc (comp-fnk inc square-fnk)]

    (fact "composes the regular fn with the fnk"
      (with-inc {:x 5}) => 26)

    (fact "composes the positional fn in the metadata as well"
      ((-> with-inc meta :plumbing.fnk.impl/positional-info first) 5) => 26)

    (fact "preserves the original fnk's schema"
      (s/fn-schema with-inc)  => (s/fn-schema square-fnk))))

(facts "#'fnk-deps"
  (fnk-deps (fnk [a b c])) => (just [:a :b :c] :in-any-order)
  (fact "works on compiled graphs"
    (-> {:a (fnk [x] x) :b (fnk [y] y)} graph/eager-compile fnk-deps) => (just [:x :y] :in-any-order)))
