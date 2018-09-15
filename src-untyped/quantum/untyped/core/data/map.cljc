(ns
  ^{:doc "Useful map functions. |map-entry|, a better merge, sorted-maps, etc."
    :attribution "alexandergunnarson"}
  quantum.untyped.core.data.map
  (:refer-clojure :exclude
    [split-at, merge, sorted-map sorted-map-by, array-map, hash-map])
  (:require
    [clojure.core                  :as core]
    [clojure.data.avl              :as avl ]
#?(:clj  [flatland.ordered.map     :as ordered-map]
   :cljs [linked.core              :as linked])
#?@(:clj
   [[clojure.data.int-map          :as imap]
    [seqspert.hash-map]])
    [quantum.untyped.core.data     :as udata]
    [quantum.untyped.core.identification
      :refer [>keyword]]
    [quantum.untyped.core.reducers :as ur
      :refer [reduce-pair]]
    [quantum.untyped.core.vars
      :refer [defalias def-]])
  (:import
#?@(:clj  [[java.util HashMap IdentityHashMap LinkedHashMap TreeMap]
           [it.unimi.dsi.fastutil.ints    Int2ReferenceOpenHashMap]
           [it.unimi.dsi.fastutil.longs   Long2LongOpenHashMap
                                          Long2ReferenceOpenHashMap]
           [it.unimi.dsi.fastutil.doubles Double2ReferenceOpenHashMap]
           [it.unimi.dsi.fastutil.objects Reference2LongOpenHashMap]]
    :cljs [[goog.structs AvlTree LinkedMap]])))

;; ----- Hash maps ----- ;;

#?(:clj (def hash-map? (partial instance? clojure.lang.PersistentHashMap)))

        (defalias hash-map core/hash-map)

#?(:clj (defalias hash-map|long->ref imap/int-map))
#?(:clj (defalias int-map hash-map|long->ref))

;; ===== Ordered value-semantic maps ===== ;;

;; ---- Insertion-ordered ----- ;;

(defalias ordered-map #?(:clj ordered-map/ordered-map :cljs linked/map))
(defalias om          ordered-map)

#?(:clj
(defmacro kw-omap
  "Like `kw-map`, but preserves insertion order."
  [& ks]
  (list* `om (udata/quote-map-base >keyword ks))))

;; ----- Comparison-ordered (sorted) ----- ;;

(defalias core/sorted-map)
(defalias core/sorted-map-by)

(defn gen-compare-by-val [m] (fn [k0 k1] (compare [(get m k1) k1] [(get m k0) k0])))

(defn sorted-map-by-val [m & kvs] (apply sorted-map-by (gen-compare-by-val m) kvs))

;; TODO `goog.structs.AvlTree` has similar to this; implement with `defnt`
(defalias sorted-rank-map    avl/sorted-map)
(defalias sorted-rank-map-by avl/sorted-map-by)
(defalias avl/nearest)
(defalias avl/rank-of)
(defalias avl/subrange)
(defalias avl/split-key)
(defalias avl/split-at)

;; ===== Interval Tree / Map ===== ;;

;; TODO this is just a placeholder until we can use `com.dean.clojure-interval-tree`
;; (Adapted from http://clj-me.cgrand.net/2012/03/16/a-poor-mans-interval-tree/)

(defn- interval< [[a b] [c d]]
  (boolean (and b c
                (if (= a b)
                    (neg? (compare b c))
                    (<= (compare b c) 0)))))

(def- interval-map|empty (sorted-map-by interval< [nil nil] #{}))

(defn- interval-map|split-at [m x]
  (if x
    (let [[[a b :as k] vs] (find m [x x])]
      (if (or (= a x) (= b x))
        m
        (-> m (dissoc k) (assoc [a x] vs [x b] vs))))
    m))

(defn- interval-map|alter [m from to f & args]
  (let [m (-> m (interval-map|split-at from) (interval-map|split-at to))
        kvs (for [[r vs]
                  (cond
                    (and from to) (subseq m >= [from from] < [to to])
                    from          (subseq m >= [from from])
                    to            (subseq m <  [to   to])
                    :else         m)]
              [r (apply f vs args)])]
    (into m kvs)))

(defn interval|assoc  [m from to v] (interval-map|alter m from to conj v))
(defn interval|dissoc [m from to v] (interval-map|alter m from to disj v))
(defn interval|get    [m x] (get m [x x]))

(defn interval-map [] interval-map|empty)

(-> (interval-map)
    (interval|assoc 0 5 :a)
    (interval|assoc 1 6 :b)
    (interval|assoc 2 7 :c)
    (interval|get 2))

;; ===== General ===== ;;

; TODO look at imap/merge

; TODO use |clojure.data.int-map/merge and merge-with|, |update|, |update!| for int maps.
; Benchmark these.
(defn merge
 "A performant drop-in replacement for `clojure.core/merge`.

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

(defn bubble-max-key [k coll] ; TODO move
  "Move a maximal element of coll according to fn k (which returns a number)
   to the front of coll."
  {:adapted-from 'clojure.set/bubble-max-key}
  (let [max (apply max-key k coll)]
    (cons max (remove #(identical? max %) coll))))

; TODO abstract with set/difference and move
(defn difference-by-key
  ([m0] m0)
  ([m0 m1] (if (< (count m0) (count m1))
               (reduce-kv (fn [m' k v]
                             (if (contains? m1 k)
                                 (dissoc m' k)
                                 m'))
                 m0 m0)
               (reduce dissoc m0 (keys m1))))
  ([m0 m1 & ms]
     (reduce difference-by-key m0 (conj ms m1))))

; TODO abstract with set/union and move
(defn union-by-key [a b] (merge a b))

 ; TODO abstract with set/intersection and move
(defn intersection-by-key
  ([m0] m0)
  ([m0 m1]
     (if (< (count m1) (count m0))
         (recur m1 m0)
         (reduce-kv (fn [m' k v]
                      (if (contains? m1 k)
                          m'
                          (dissoc m' k)))
           m0 m0)))
  ([m0 m1 & ms]
     (let [bubbled-ms (bubble-max-key #(- (count %)) (conj ms m1 m0))]
       (reduce intersection-by-key (first bubbled-ms) (rest bubbled-ms)))))
