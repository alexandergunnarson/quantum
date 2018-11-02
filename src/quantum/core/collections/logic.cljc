(ns quantum.core.collections.logic
  (:refer-clojure :exclude
    [reduce transduce, some not-any? every? not-every?])
  (:require
    [quantum.core.reducers.reduce
      :refer [reduce transduce]]
    [quantum.core.fn   :as fn
      :refer [rcomp]]
    [quantum.core.vars :as var
      :refer [defalias]]
    [quantum.untyped.core.collections.logic :as u]))

(defn seq-or
  "∃: A faster version of |some| using |reduce| instead of |seq|."
  ([xs] (seq-or identity xs))
  ([pred xs] (transduce (u/seq-or|rf pred) xs)))

(defalias some seq-or)

(def seq-nor (rcomp seq-or not))

(defalias not-any? seq-nor)

(defn seq-and
  "∀: A faster version of |every?| using |reduce| instead of |seq|."
  ([xs] (seq-and identity xs))
  ([pred xs] (transduce (u/seq-and|rf pred) xs)))

(defalias every? seq-and)

(def seq-nand (rcomp seq-and not))

(defalias not-every? seq-nand)

(defn seq-and-pair
  "`seq-and` for pairwise comparisons."
  ([pred xs]
    (reduce (fn [a b] (or (pred a b) (reduced false))) (first xs) (rest xs))))

(defalias every?-pair seq-and-pair)
