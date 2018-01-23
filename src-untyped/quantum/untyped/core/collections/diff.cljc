(ns quantum.untyped.core.collections.diff
  (:refer-clojure :exclude [seqable?])
  (:require
    [clojure.string     :as str]
    [diffit.map         :as mdiff]
    [diffit.vec         :as vdiff]
    [quantum.core.error :as err
      :refer [err!]]
    [quantum.untyped.core.core
      :refer [kw-map istr seqable?]]))

(defn diff [a b]
  (cond (and (sequential?  a) (sequential?  b)) (vdiff/diff a b)
        (and (associative? a) (associative? b)) (mdiff/diff a b)
        (and (seqable?     a) (seqable?     b)) (vdiff/diff a b)
        :else                                   (err! "Don't know how to diff" (kw-map a b))))

(defn diff|human
  "Human-readable `diff` output." ;; TODO currently only works for diffed sequences
  {:examples '{(diff|human [1 2 3 4] [1 2 7 8 4])
                 {:edit-distance 3,
                  :steps
                    [["At position 2, insert this sequence:"        [7 8]]
                     ["At position 4, remove this number of items:" 1]]}
               (diff|human {1 2 3 4} {1 2 7 8}) :fail}}
  [a b]
  (let [[edit-distance edit-script] (diff a b)]
    {:edit-distance edit-distance
     :steps
       (->> edit-script
            (reduce
              (fn [steps [op position x]]
                (conj steps
                  [(case op
                     :+ (istr "At position ~{position}, insert this sequence:")
                     :- (istr "At position ~{position}, remove this number of items:"))
                   x]))
              []))}))


