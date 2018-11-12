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

(defn check-comparator-transitivity
  "To ensure the comparator maintains its contract and that `IllegalArgumentException Comparison
   method violates its general contract!` is not thrown."
  {:complexity "O(n^3) time"
   :adapted-from
    "http://code.nomad-labs.com/2015/06/02/finding-the-error-in-your-comparators-compare-method-aka-comparison-method-violates-its-general-contract/"}
  [compf xs]
  (if (< (int (bounded-count 3 xs)) 3)
      (throw (ex-info "`xs` must have at least 3 items"))
      (let [^objects xs' (into-array xs) ct (count xs')]
        (doseq [i0 (range 0 ct)]
          (doseq [i1 (range 1 ct)]
            (doseq [i2 (range 2 ct)]
              (when (and (not= i0 i1) (not= i0 i2) (not= i1 i2))
                (let [x0    (aget xs' i0)
                      x1    (aget xs' i1)
                      x2    (aget xs' i2)
                      x0+x1 (int (compf x0 x1))
                      x0+x2 (int (compf x0 x2))
                      x1+x2 (int (compf x1 x2))]
                  (when (and (< x0+x1 0) (< x1+x2 0) (not (< x0+x2 0)))
                    (println "x0 comp< x1, x1 comp< x2, but x0 not comp< x2")
                    (println "x0:" x0)
                    (println "x1:" x1)
                    (println "x2:" x2))
                  (when (and (> x0+x1 0) (> x1+x2 0) (not (> x0+x2 0)))
                    (println "x0 comp> x1, x1 comp> x2, but x0 not comp< x2")
                    (println "x0:" x0)
                    (println "x1:" x1)
                    (println "x2:" x2))))))))))
