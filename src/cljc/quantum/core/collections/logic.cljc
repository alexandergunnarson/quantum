(ns quantum.core.collections.logic
  (:refer-clojure :exclude [reduce some not-any? every? not-every?])
  (:require
    [quantum.core.reducers.reduce
      :refer [reduce]]
    [quantum.core.fn   :as fn
      :refer [rcomp]]
    [quantum.core.vars :as var
      :refer [defalias]]))

; LOGICAL ;

(defn seq-or
  "∃: A faster version of |some| using |reduce| instead of |seq|."
  ([xs] (seq-or identity xs))
  ([pred xs]
    (reduce (fn [_ x] (and (pred x) (reduced x))) nil xs)))

(defalias some seq-or)

(def seq-nor (rcomp seq-or not))

(defalias not-any? seq-nor)

(defn seq-and
  "∀: A faster version of |every?| using |reduce| instead of |seq|."
  ([xs] (seq-and identity xs))
  ([pred xs]
    (reduce (fn [_ x] (or (pred x) (reduced false))) nil xs)))

(defalias every? seq-and)

(def seq-nand (rcomp seq-and not))

(defalias not-every? seq-nand)

(defn apply-and [xs] (seq-and xs))
(defn apply-or  [xs] (seq-or  xs))

(defn seq-and-2
  "`seq-and` for pairwise comparisons."
  ([pred xs]
    (reduce (fn [a b] (or (pred a b) (reduced false))) (first xs) (rest xs))))

(defalias every?-2 seq-and-2)
