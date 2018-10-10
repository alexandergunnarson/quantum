(ns quantum.untyped.core.compare
  "General comparison operators and constants"
  (:refer-clojure :exclude [==])
  (:require
    [quantum.untyped.core.core :as ucore
      :refer [defaliases]]
    [quantum.untyped.core.fn
      :refer [fn']]
    [quantum.untyped.core.logic
      :refer [fn-or ifs]]))

(ucore/log-this-ns)

(def == identical?)
(def not== (comp not identical?))

(def comparison=     zero?)
(def comparison-not= (comp not comparison=))
(def comparison<     neg?)
(def comparison<=    (fn-or comparison< comparison=))
(def comparison>     pos?)
(def comparison>=    (fn-or comparison> comparison=))

(defn comp<     ([      x0 x1] (comp<            compare x0 x1))
                ([compf x0 x1] (comparison<     (compf   x0 x1))))
(defn comp<=    ([      x0 x1] (comp<=           compare x0 x1))
                ([compf x0 x1] (comparison<=    (compf   x0 x1))))
(defn comp=     ([      x0 x1] (comp=            compare x0 x1))
                ([compf x0 x1] (comparison=     (compf   x0 x1))))
(defn comp-not= ([      x0 x1] (comp-not=        compare x0 x1))
                ([compf x0 x1] (comparison-not= (compf   x0 x1))))
(defn comp>=    ([      x0 x1] (comp>=           compare x0 x1))
                ([compf x0 x1] (comparison>=    (compf   x0 x1))))
(defn comp>     ([      x0 x1] (comp>            compare x0 x1))
                ([compf x0 x1] (comparison>     (compf   x0 x1))))

;; TODO deprecate
(def class->comparator
  {#?@(:clj
        [Class (fn [^Class a ^Class b]
                 (.compareTo (.getName a) (.getName b)))])})

(defn rcompare
  "Reverse comparator."
  {:adapted-from "taoensso.encore, possibly via weavejester.medley"}
  [a b] (compare b a))

(defn comp-extrema-of
  "Returns the extreme elements of `xs` according to comparator `compf` and `comparisonf` in O(n)
   time."
  ([comparisonf xs] (comp-extrema-of comparisonf compare xs))
  ([comparisonf compf xs]
    (->> xs
         (reduce
           (fn ([[extremum extrema :as ret] x]
                 (if (identical? extremum ucore/sentinel)
                     [x [x]]
                     (let [c (int (compf x extremum))]
                       (ifs (comparison= c)
                            [x (conj extrema x)]
                            (comparisonf c)
                            [x [x]]
                            ret)))))
           [ucore/sentinel []])
         second)))

(defn comp-mins-of
  "Returns the equally 'min' elements of `xs` according to comparator `compf` in O(n) time."
  ([xs] (comp-mins-of compare xs))
  ([compf xs] (comp-extrema-of comparison< compf xs)))

(defn comp-maxes-of
  "Returns the equally 'max' elements of `xs` according to comparator `compf` in O(n) time."
  ([xs] (comp-mins-of compare xs))
  ([compf xs] (comp-extrema-of comparison> compf xs)))

(defn gen-comp-extremum|rf [compf comparisonf]
  (fn ([] nil) ([prev x] (if (comparisonf (compf x prev)) x prev))))

(defn gen-comp-min|rf [compf] (gen-comp-extremum|rf compf comparison<))

(defn comp-min-of
  "Returns the 'min' element of `xs` according to comparator `compf` in O(n) time."
  ([xs] (comp-min-of compare xs))
  ([compf xs] (->> xs (reduce (gen-comp-min|rf compf)))))

(defn gen-comp-max|rf [compf] (gen-comp-extremum|rf compf comparison>))

(defn comp-max-of
  "Returns the 'max' element of `xs` according to comparator `compf` in O(n) time."
  ([xs] (comp-max-of compare xs))
  ([compf xs] (->> xs (reduce (gen-comp-max|rf compf)))))
