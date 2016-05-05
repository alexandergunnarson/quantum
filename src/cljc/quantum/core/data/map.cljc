(ns
  ^{:doc "Useful map functions. |map-entry|, a better merge, sorted-maps, etc."
    :attribution "Alex Gunnarson"}
  quantum.core.data.map
  (:refer-clojure :exclude [split-at merge sorted-map sorted-map-by])
  (:require [quantum.core.vars    :as var
              :refer [#?(:clj defalias)]  ]
   #?(:clj  [clojure.core         :as core]
      :cljs [cljs.core            :as core])
            [clojure.data.avl     :as avl ]
  #?@(:clj [[clojure.data.int-map :as imap]
            [flatland.ordered.map :as omap]
            [seqspert.hash-map            ]]))
  #?(:cljs
  (:require-macros
            [quantum.core.vars    :as var
              :refer [defalias]           ])))

(defalias ordered-map #?(:clj omap/ordered-map :cljs array-map))
(defalias om          #?(:clj omap/ordered-map :cljs array-map))

(def sorted-map    avl/sorted-map)
(def sorted-map-by avl/sorted-map-by)

#?(:clj (def int-map       imap/int-map))

; TODO look at imap/merge

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
  {:attribution "Alex Gunnarson"}
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

(defn merge'
  "Like merge, but uses transients"
  {:from "clojure.tools.analyzer.utils"}
  [m & mms]
  (persistent! (reduce conj! (transient (or m {})) mms)))

; TODO use |clojure.data.int-map/merge and merge-with|, |update|, |update!| for int maps.
; Benchmark these.
(defn merge
 "A performant drop-in replacement for |clojure.core/merge|.

  398.815137 msecs (core/merge m1 m2)
  188.270844 msecs (seqspert.hash-map/sequential-splice-hash-maps m1 m2)
  25.401196  msecs (seqspert.hash-map/parallel-splice-hash-maps   m1 m2)))"
  {:attribution "Alex Gunnarson"
   :performance "782.922731 ms |merge+| vs. 1.133217 sec normal |merge|
                 on the CLJS version; 1.5 times faster!"}
  ([] nil)
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
     :cljs (if (satisfies? AEditable m0)
               (->> ms
                    (reduce conj! (transient m0))
                    persistent!)
               (apply core/merge m0 m1 ms)))))

#?(:clj
(defn pmerge
  ([m0 m1] (seqspert.hash-map/parallel-splice-hash-maps m0 m1))
  ([m0 m1 & ms]
    (reduce seqspert.hash-map/parallel-splice-hash-maps
      (pmerge m0 m1) ms))))

; Required for |reducers|
(defalias split-at clojure.data.avl/split-at)