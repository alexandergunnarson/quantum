(ns
  ^{:doc "Useful map functions. |map-entry|, a better merge, sorted-maps, etc."
    :attribution "alexandergunnarson"}
  quantum.core.data.map
  (:refer-clojure :exclude
    [split-at, merge, sorted-map sorted-map-by, array-map, hash-map])
  (:require [quantum.core.vars    :as var
              :refer [#?(:clj defalias)]  ]
            [clojure.core         :as core]
            [clojure.data.avl     :as avl ]
  #?@(:clj [[clojure.data.int-map :as imap]
            [flatland.ordered.map :as omap]
            [seqspert.hash-map            ]
            [quantum.core.collections.base :as cbase
              :refer [reduce-pair]]
            [quantum.core.core    :as qcore]]))
  (:require-macros
            [quantum.core.vars    :as var
              :refer [defalias]])
  (:import #?@(:clj  [java.util.HashMap
                      [it.unimi.dsi.fastutil.ints    Int2ReferenceOpenHashMap]
                      [it.unimi.dsi.fastutil.longs   Long2ReferenceOpenHashMap]
                      [it.unimi.dsi.fastutil.doubles Double2ReferenceOpenHashMap]]
               :cljs [goog.structs.Map])))

; TO EXPLORE
; - Optimizing Hash-Array Mapped Tries for Fast and Lean Immutable JVM Collections
;   - http://michael.steindorfer.name/publications/oopsla15.pdf
;   - Overall significantly faster on what they've chosen to measure.
;   - Alex Miller: "We have seen it and will probably investigate some of these ideas after 1.8."
; =======================

(defalias ordered-map #?(:clj omap/ordered-map :cljs array-map))
(defalias om          #?(:clj omap/ordered-map :cljs array-map))

#?(:clj
(defmacro kw-omap
  "Like `kw-map`, but preserves insertion order."
  [& ks]
  (qcore/quote-map-base om (comp keyword str) ks)))

(defalias sorted-map         core/sorted-map   )
(defalias sorted-map-by      core/sorted-map-by)
(defalias sorted-rank-map    avl/sorted-map    )
(defalias sorted-rank-map-by avl/sorted-map-by )
(defalias nearest            avl/nearest       )
(defalias rank-of            avl/rank-of       )
(defalias subrange           avl/subrange      )
(defalias split-key          avl/split-key     )
(defalias split-at           avl/split-at      )

(defn sorted-map-by-val [m-0]
  (sorted-map-by (fn [k1 k2]
                   (compare [(get m-0 k2) k2]
                            [(get m-0 k1) k1]))))

#?(:clj (def int-map       imap/int-map  ))
#?(:clj (def array-map     core/array-map))
#?(:clj (def hash-map      core/hash-map ))

; TODO look at imap/merge

; `(apply hash-map pairs)` <~> `lodash/fromPairs`

(defn map-entry
  "A performant replacement for creating 2-tuples (vectors), e.g., as return values
   in a |kv-reduce| function.

   Now overshadowed by ztellman's unrolled vectors in 1.8.0.

   Time to create 100000000 2-tuples:
   new tuple-vector 55.816415 ms
   map-entry        37.542442 ms

   However, insertion into maps is faster with map-entry:

   (def vs [[1 2] [3 4]])
   (def ms [(map-entry 1 2) (map-entry 3 4)])
   (def m0 {})
   508.122831 ms (dotimes [n 1000000] (into m0 vs))
   310.335998 ms (dotimes [n 1000000] (into m0 ms))"
  {:attribution "alexandergunnarson"}
  [k v]
  #?(:clj  (clojure.lang.MapEntry. k v)
     :cljs [k v]))

(defn map-entry-seq [args]
  (loop [[k v :as args-n] args
         accum []]
    (if (empty? args-n)
        accum
        (recur (-> args-n rest rest)
               (conj accum (map-entry k v))))))

#?(:clj (def hash-map? (partial instance? clojure.lang.PersistentHashMap)))

; TODO use |clojure.data.int-map/merge and merge-with|, |update|, |update!| for int maps.
; Benchmark these.
(defn merge
 "A performant drop-in replacement for |clojure.core/merge|.

  398.815137 msecs (core/merge m1 m2)
  188.270844 msecs (seqspert.hash-map/sequential-splice-hash-maps m1 m2)
  25.401196  msecs (seqspert.hash-map/parallel-splice-hash-maps   m1 m2)))"
  {:attribution "alexandergunnarson"
   :performance "782.922731 ms |merge+| vs. 1.133217 sec normal |merge|
                 on the CLJ version; 1.5 times faster!"}
  ([] (hash-map))
  ([m0] m0)
  ([m0 m1]
    ; To avoid NullPointerException
    #?(:clj  (cond (nil? m0) m1
                   (nil? m1) m0
                   (and (hash-map? m0) (hash-map? m1))
                      (seqspert.hash-map/sequential-splice-hash-maps m0 m1)
                     :else (core/merge m0 m1))
       :cljs (core/merge m0 m1)))
  ([m0 m1 & ms]
  #?(:clj  (reduce merge (merge m0 m1) ms)
     :cljs (if (satisfies? core/IEditableCollection m0)
               (->> ms
                    (reduce conj! (transient m0))
                    persistent!)
               (reduce core/merge (core/merge m0 m1) ms)))))

#?(:clj
(defn pmerge
  ([] (hash-map))
  ([m0] m0)
  ([m0 m1] (seqspert.hash-map/parallel-splice-hash-maps m0 m1))
  ([m0 m1 & ms]
    (reduce pmerge
      (pmerge m0 m1) ms))))

; TODO generate these functions via macros
(defn #?(:clj ^HashMap !hash-map :cljs !hash-map)
  "Creates a single-threaded, mutable hash map.
   On the JVM, this is a java.util.HashMap.

   On JS, this is a goog.structs.Map."
  {:todo #{"Compare performance on CLJS with ECMAScript 6 Map"}}
  ([] #?(:clj (HashMap.) :cljs (Map.)))
  ([k0 v0]
    (doto #?(:clj (HashMap.) :cljs (Map.))
          (#?(:clj .put :cljs .set) k0 v0)))
  ([k0 v0 k1 v1]
    (doto #?(:clj (HashMap.) :cljs (Map.))
          (#?(:clj .put :cljs .set) k0 v0)
          (#?(:clj .put :cljs .set) k1 v1)))
  ([k0 v0 k1 v1 k2 v2]
    (doto #?(:clj (HashMap.) :cljs (Map.))
          (#?(:clj .put :cljs .set) k0 v0)
          (#?(:clj .put :cljs .set) k1 v1)
          (#?(:clj .put :cljs .set) k2 v2)))
  ([k0 v0 k1 v1 k2 v2 k3 v3]
    (doto #?(:clj (HashMap.) :cljs (Map.))
          (#?(:clj .put :cljs .set) k0 v0)
          (#?(:clj .put :cljs .set) k1 v1)
          (#?(:clj .put :cljs .set) k2 v2)
          (#?(:clj .put :cljs .set) k3 v3)))
  ([k0 v0 k1 v1 k2 v2 k3 v3 k4 v4]
    (doto #?(:clj (HashMap.) :cljs (Map.))
          (#?(:clj .put :cljs .set) k0 v0)
          (#?(:clj .put :cljs .set) k1 v1)
          (#?(:clj .put :cljs .set) k2 v2)
          (#?(:clj .put :cljs .set) k3 v3)
          (#?(:clj .put :cljs .set) k4 v4)))
  ([k0 v0 k1 v1 k2 v2 k3 v3 k4 v4 k5 v5]
    (doto #?(:clj (HashMap.) :cljs (Map.))
          (#?(:clj .put :cljs .set) k0 v0)
          (#?(:clj .put :cljs .set) k1 v1)
          (#?(:clj .put :cljs .set) k2 v2)
          (#?(:clj .put :cljs .set) k3 v3)
          (#?(:clj .put :cljs .set) k4 v4)
          (#?(:clj .put :cljs .set) k5 v5)))
  ([k0 v0 k1 v1 k2 v2 k3 v3 k4 v4 k5 v5 k6 v6 & kvs]
    (reduce-pair
      (fn [#?(:clj ^HashMap m :cljs m) k v] (doto m (#?(:clj .put :cljs .set) k v)))
      (doto #?(:clj (HashMap.) :cljs (Map.))
            (#?(:clj .put :cljs .set) k0 v0)
            (#?(:clj .put :cljs .set) k1 v1)
            (#?(:clj .put :cljs .set) k2 v2)
            (#?(:clj .put :cljs .set) k3 v3)
            (#?(:clj .put :cljs .set) k4 v4)
            (#?(:clj .put :cljs .set) k5 v5)
            (#?(:clj .put :cljs .set) k6 v6))
      kvs)))

; TODO generate these functions via macros
#?(:clj (defn ^Int2ReferenceOpenHashMap !hash-map:int->ref [] (Int2ReferenceOpenHashMap.)))
#?(:clj (defalias !hash-map:int->object !hash-map:int->ref))

#?(:clj (defn ^Long2ReferenceOpenHashMap !hash-map:long->ref [] (Long2ReferenceOpenHashMap.)))
#?(:clj (defalias !hash-map:long->object !hash-map:long->ref))

#?(:clj (defn ^Double2ReferenceOpenHashMap !hash-map:double->ref [] (Double2ReferenceOpenHashMap.)))
#?(:clj (defalias !hash-map:double->object !hash-map:double->ref))
