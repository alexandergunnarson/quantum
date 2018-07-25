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
    [quantum.untyped.core.convert  :as uconv]
    [quantum.untyped.core.data     :as udata]
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

;; TO EXPLORE
;; - Optimizing Hash-Array Mapped Tries for Fast and Lean Immutable JVM Collections
;;   - Actual usable implementation: https://github.com/usethesource/capsule
;;   - http://michael.steindorfer.name/publications/oopsla15.pdf
;;   - Overall significantly faster on what they've chosen to measure.
;;   - Alex Miller: "We have seen it and will probably investigate some of these ideas after 1.8."
;; =======================

;; ===== Map entries ===== ;;

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
     :cljs (cljs.core.MapEntry. k v nil)))

;; TODO excise?
(defn map-entry-seq [args]
  (loop [[k v :as args-n] args
         accum []]
    (if (empty? args-n)
        accum
        (recur (-> args-n rest rest)
               (conj accum (map-entry k v))))))

;; ===== Unordered identity-semantic maps ===== ;;

;; TODO generate these functions via macros
(defn #?(:clj ^IdentityHashMap !identity-map :cljs !identity-map)
  "Creates a single-threaded, mutable identity map.
   On the JVM, this is a `java.util.IdentityHashMap`.
   On JS, this is a `js/Map` (ECMAScript 6 Map)."
  ([] #?(:clj (IdentityHashMap.) :cljs (js/Map.)))
  ([k0 v0]
    (doto #?(:clj (IdentityHashMap.) :cljs (js/Map.))
          (#?(:clj .put :cljs .set) k0 v0)))
  ([k0 v0 k1 v1]
    (doto #?(:clj (IdentityHashMap.) :cljs (js/Map.))
          (#?(:clj .put :cljs .set) k0 v0)
          (#?(:clj .put :cljs .set) k1 v1)))
  ([k0 v0 k1 v1 k2 v2]
    (doto #?(:clj (IdentityHashMap.) :cljs (js/Map.))
          (#?(:clj .put :cljs .set) k0 v0)
          (#?(:clj .put :cljs .set) k1 v1)
          (#?(:clj .put :cljs .set) k2 v2)))
  ([k0 v0 k1 v1 k2 v2 k3 v3]
    (doto #?(:clj (IdentityHashMap.) :cljs (js/Map.))
          (#?(:clj .put :cljs .set) k0 v0)
          (#?(:clj .put :cljs .set) k1 v1)
          (#?(:clj .put :cljs .set) k2 v2)
          (#?(:clj .put :cljs .set) k3 v3)))
  ([k0 v0 k1 v1 k2 v2 k3 v3 k4 v4]
    (doto #?(:clj (IdentityHashMap.) :cljs (js/Map.))
          (#?(:clj .put :cljs .set) k0 v0)
          (#?(:clj .put :cljs .set) k1 v1)
          (#?(:clj .put :cljs .set) k2 v2)
          (#?(:clj .put :cljs .set) k3 v3)
          (#?(:clj .put :cljs .set) k4 v4)))
  ([k0 v0 k1 v1 k2 v2 k3 v3 k4 v4 k5 v5]
    (doto #?(:clj (IdentityHashMap.) :cljs (js/Map.))
          (#?(:clj .put :cljs .set) k0 v0)
          (#?(:clj .put :cljs .set) k1 v1)
          (#?(:clj .put :cljs .set) k2 v2)
          (#?(:clj .put :cljs .set) k3 v3)
          (#?(:clj .put :cljs .set) k4 v4)
          (#?(:clj .put :cljs .set) k5 v5)))
  ([k0 v0 k1 v1 k2 v2 k3 v3 k4 v4 k5 v5 k6 v6 & kvs]
    (reduce-pair
      (fn [#?(:clj ^IdentityHashMap m :cljs m) k v] (doto m (#?(:clj .put :cljs .set) k v)))
      (doto #?(:clj (IdentityHashMap.) :cljs (js/Map.))
            (#?(:clj .put :cljs .set) k0 v0)
            (#?(:clj .put :cljs .set) k1 v1)
            (#?(:clj .put :cljs .set) k2 v2)
            (#?(:clj .put :cljs .set) k3 v3)
            (#?(:clj .put :cljs .set) k4 v4)
            (#?(:clj .put :cljs .set) k5 v5)
            (#?(:clj .put :cljs .set) k6 v6))
      kvs)))

;; ===== Unordered value-semantic maps ===== ;;

(defalias array-map core/array-map)

;; ----- Hash maps ----- ;;

#?(:clj (def hash-map? (partial instance? clojure.lang.PersistentHashMap)))

        (defalias hash-map core/hash-map)

#?(:clj (defalias hash-map|long->ref imap/int-map))
#?(:clj (defalias int-map hash-map|long->ref))

#?(:cljs
(deftype MutableHashMap ; There can be no `undefined` values
  [meta ^:mutable ct ^js/Map m #_"Keys are int hashes; vals are map entries from k to v"
   ^:mutable ^boolean has-nil? ^:mutable nil-val ^:mutable __hash]
  Object
    (toString [this] (str (into {} (es6-iterator-seq (.values m)))))
    (equiv    [this other] (-equiv this other))
    (keys     [this] (es6-iterator (cljs.core/keys this)))
    (entries  [this] (es6-entries-iterator (seq this)))
    (values   [this] (es6-iterator (vals this)))
    (has      [this k] (contains? this k))
    (get      [this k not-found] (-lookup this k not-found))
    (forEach  [this f] (doseq [[k v] this] (f v k)))
  ICloneable
    (-clone [_] (MutableHashMap. meta ct m has-nil? nil-val __hash))
  IIterable
    (-iterator [this] (-iterator (vals this)))
  IWithMeta
    (-with-meta [this meta-] (MutableHashMap. meta- ct m has-nil? nil-val __hash))
  IMeta
    (-meta [this] meta)
  IEmptyableCollection
    (-empty [this] (MutableHashMap. meta 0 (js/Map.) false nil 0))
  IEquiv
    (-equiv [this that] (equiv-map this that))
  IHash
    (-hash [this] (caching-hash this hash-unordered-coll __hash))
  ISeqable
    (-seq [this]
      (when (pos? ct)
        (let [s (es6-iterator-seq (.values m))]
          (if has-nil?
              (cons (map-entry nil nil-val) s)
              s))))
  ICounted
    (-count [this] ct)
  ILookup
    (-lookup [this k] (-lookup this k nil))
    (-lookup [this k not-found]
      (if (nil? k)
          (if has-nil? nil-val not-found)
          (let [kv (.get m (hash k))]
            (if (undefined? kv) not-found (-val kv)))))
  IAssociative
    (-contains-key? [this k]
      (if (nil? k)
          has-nil?
          (.has m (hash k))))
  IFind
    (-find [this k]
      (if (nil? k)
          (when has-nil? (map-entry nil nil-val))
          (let [kv (.get m (hash k))]
            (if (undefined? kv) nil kv))))
  ITransientCollection
    (-conj! [this entry]
      (if (vector? entry)
          (-assoc! this (-nth entry 0) (-nth entry 1))
           (loop [ret this es (seq entry)]
             (if (nil? es)
                 ret
                 (let [e (first es)]
                   (if (vector? e)
                       (recur (-assoc! ret (-nth e 0) (-nth e 1))
                              (next es))
                       (throw (ex-info "conj on a map takes map entries or seqables of map    entries" {}))))))))
  ITransientAssociative
    (-assoc! [this k v]
      (cond
        (undefined? v)
          (throw (ex-info "Cannot `assoc` undefined value to `MutableHashMap`" {}))
        (nil? k)
          (if (and has-nil? (identical? v nil-val))
              this
              (do (when-not has-nil? (set! ct (inc ct)))
                  (set! has-nil? true)
                  (set! nil-val v)
                  (set! __hash nil) ; TODO recalculate incrementally?
                  this))
        :else
          (let [hash-k (hash k)]
            (if (.has m hash-k)
                this
                (do (.set m (hash k) (map-entry k v))
                    (set! ct (inc ct))
                    (set! __hash nil) ; TODO recalculate incrementally?
                    this)))))
  ITransientMap
    (-dissoc! [this k]
      (if (nil? k)
          (if has-nil?
              (do (set! ct (dec ct))
                  (set! has-nil? false)
                  (set! nil-val nil)
                  (set! __hash nil) ; TODO recalculate incrementally?
                  this)
              this)
          (if (.delete m (hash k))
              (do (set! ct (dec ct))
                  (set! __hash nil) ; TODO recalculate incrementally?
                  this)
              this)))
  IKVReduce
    (-kv-reduce [this f init]
      (let [init (if has-nil? (f init nil nil-val) init)]
        (if (reduced? init)
            @init
            (unreduced (reduce (fn [ret kv] (f ret (-key kv) (-val kv))) init m)))))
  IFn
    (-invoke [this k]           (-lookup this k))
    (-invoke [this k not-found] (-lookup this k not-found))))

;; TODO generate these functions via macros
(defn #?(:clj ^HashMap !hash-map :cljs !hash-map)
  "Creates a single-threaded, mutable hash map.
   On the JVM, this is a `java.util.HashMap`.
   On JS, this is a `quantum.untyped.core.data.map.HashMap`."
  ([] #?(:clj (HashMap.) :cljs (MutableHashMap. nil 0 (js/Map.) false nil nil)))
  ([k0 v0]
    (doto #?(:clj (HashMap.) :cljs (!hash-map))
          (#?(:clj .put :cljs assoc!) k0 v0)))
  ([k0 v0 k1 v1]
    (doto #?(:clj (HashMap.) :cljs (!hash-map))
          (#?(:clj .put :cljs assoc!) k0 v0)
          (#?(:clj .put :cljs assoc!) k1 v1)))
  ([k0 v0 k1 v1 k2 v2]
    (doto #?(:clj (HashMap.) :cljs (!hash-map))
          (#?(:clj .put :cljs assoc!) k0 v0)
          (#?(:clj .put :cljs assoc!) k1 v1)
          (#?(:clj .put :cljs assoc!) k2 v2)))
  ([k0 v0 k1 v1 k2 v2 k3 v3]
    (doto #?(:clj (HashMap.) :cljs (!hash-map))
          (#?(:clj .put :cljs assoc!) k0 v0)
          (#?(:clj .put :cljs assoc!) k1 v1)
          (#?(:clj .put :cljs assoc!) k2 v2)
          (#?(:clj .put :cljs assoc!) k3 v3)))
  ([k0 v0 k1 v1 k2 v2 k3 v3 k4 v4]
    (doto #?(:clj (HashMap.) :cljs (!hash-map))
          (#?(:clj .put :cljs assoc!) k0 v0)
          (#?(:clj .put :cljs assoc!) k1 v1)
          (#?(:clj .put :cljs assoc!) k2 v2)
          (#?(:clj .put :cljs assoc!) k3 v3)
          (#?(:clj .put :cljs assoc!) k4 v4)))
  ([k0 v0 k1 v1 k2 v2 k3 v3 k4 v4 k5 v5]
    (doto #?(:clj (HashMap.) :cljs (!hash-map))
          (#?(:clj .put :cljs assoc!) k0 v0)
          (#?(:clj .put :cljs assoc!) k1 v1)
          (#?(:clj .put :cljs assoc!) k2 v2)
          (#?(:clj .put :cljs assoc!) k3 v3)
          (#?(:clj .put :cljs assoc!) k4 v4)
          (#?(:clj .put :cljs assoc!) k5 v5)))
  ([k0 v0 k1 v1 k2 v2 k3 v3 k4 v4 k5 v5 k6 v6 & kvs]
    (reduce-pair
      (fn [^HashMap m k v] (doto m (#?(:clj .put :cljs assoc!) k v)))
      (doto #?(:clj (HashMap.) :cljs (!hash-map))
            (#?(:clj .put :cljs assoc!) k0 v0)
            (#?(:clj .put :cljs assoc!) k1 v1)
            (#?(:clj .put :cljs assoc!) k2 v2)
            (#?(:clj .put :cljs assoc!) k3 v3)
            (#?(:clj .put :cljs assoc!) k4 v4)
            (#?(:clj .put :cljs assoc!) k5 v5)
            (#?(:clj .put :cljs assoc!) k6 v6))
      kvs)))

; TODO generate these functions via macros
#?(:clj (defn ^Int2ReferenceOpenHashMap !hash-map|int->ref [] (Int2ReferenceOpenHashMap.)))
#?(:clj (defalias !hash-map|int->object !hash-map|int->ref))

#?(:clj (defn ^Long2LongOpenHashMap !hash-map|long->long [] (Long2LongOpenHashMap.)))
#?(:clj (defalias !hash-map|long !hash-map|long->long))

#?(:clj (defn ^Long2ReferenceOpenHashMap !hash-map|long->ref [] (Long2ReferenceOpenHashMap.)))
#?(:clj (defalias !hash-map|long->object !hash-map|long->ref))

#?(:clj (defn ^Double2ReferenceOpenHashMap !hash-map|double->ref [] (Double2ReferenceOpenHashMap.)))
#?(:clj (defalias !hash-map|double->object !hash-map|double->ref))

#?(:clj (defn ^Reference2LongOpenHashMap !hash-map|ref->long [] (Reference2LongOpenHashMap.)))
#?(:clj (defalias !hash-map|object->long !hash-map|ref->long))

;; ===== Ordered value-semantic maps ===== ;;

;; ---- Insertion-ordered ----- ;;

(defalias ordered-map #?(:clj ordered-map/ordered-map :cljs linked/map))
(defalias om          ordered-map)

#?(:clj
(defmacro kw-omap
  "Like `kw-map`, but preserves insertion order."
  [& ks]
  (list* `om (udata/quote-map-base uconv/>keyword ks))))

;; TODO generate these functions via macros
(defn #?(:clj ^LinkedHashMap !ordered-map :cljs !ordered-map)
  "Creates a single-threaded, mutable insertion-ordered map.
   On the JVM, this is a `java.util.LinkedHashMap`.
   On JS, this is a `goog.structs.LinkedMap`."
  ([] #?(:clj (LinkedHashMap.) :cljs (LinkedMap.)))
  ([k0 v0]
    (doto #?(:clj (LinkedHashMap.) :cljs (LinkedMap.))
          (#?(:clj .put :cljs .add) k0 v0)))
  ([k0 v0 k1 v1]
    (doto #?(:clj (LinkedHashMap.) :cljs (LinkedMap.))
          (#?(:clj .put :cljs .add) k0 v0)
          (#?(:clj .put :cljs .add) k1 v1)))
  ([k0 v0 k1 v1 k2 v2]
    (doto #?(:clj (LinkedHashMap.) :cljs (LinkedMap.))
          (#?(:clj .put :cljs .add) k0 v0)
          (#?(:clj .put :cljs .add) k1 v1)
          (#?(:clj .put :cljs .add) k2 v2)))
  ([k0 v0 k1 v1 k2 v2 k3 v3]
    (doto #?(:clj (LinkedHashMap.) :cljs (LinkedMap.))
          (#?(:clj .put :cl .add) k0 v0)
          (#?(:clj .put :cljs .add) k1 v1)
          (#?(:clj .put :cljs .add) k2 v2)
          (#?(:clj .put :cljs .add) k3 v3)))
  ([k0 v0 k1 v1 k2 v2 k3 v3 k4 v4]
    (doto #?(:clj (LinkedHashMap.) :cljs (LinkedMap.))
          (#?(:clj .put :cljs .add) k0 v0)
          (#?(:clj .put :cljs .add) k1 v1)
          (#?(:clj .put :cljs .add) k2 v2)
          (#?(:clj .put :cljs .add) k3 v3)
          (#?(:clj .put :cljs .add) k4 v4)))
  ([k0 v0 k1 v1 k2 v2 k3 v3 k4 v4 k5 v5]
    (doto #?(:clj (LinkedHashMap.) :cljs (LinkedMap.))
          (#?(:clj .put :cljs .add) k0 v0)
          (#?(:clj .put :cljs .add) k1 v1)
          (#?(:clj .put :cljs .add) k2 v2)
          (#?(:clj .put :cljs .add) k3 v3)
          (#?(:clj .put :cljs .add) k4 v4)
          (#?(:clj .put :cljs .add) k5 v5)))
  ([k0 v0 k1 v1 k2 v2 k3 v3 k4 v4 k5 v5 k6 v6 & kvs]
    (reduce-pair
      (fn [#?(:clj ^LinkedHashMap m :cljs m) k v] (doto m (#?(:clj .put :cljs .add) k v)))
      (doto #?(:clj (LinkedHashMap.) :cljs (LinkedMap.))
            (#?(:clj .put :cljs .add) k0 v0)
            (#?(:clj .put :cljs .add) k1 v1)
            (#?(:clj .put :cljs .add) k2 v2)
            (#?(:clj .put :cljs .add) k3 v3)
            (#?(:clj .put :cljs .add) k4 v4)
            (#?(:clj .put :cljs .add) k5 v5)
            (#?(:clj .put :cljs .add) k6 v6))
      kvs)))

;; ----- Comparison-ordered (sorted) ----- ;;

(defalias core/sorted-map)
(defalias core/sorted-map-by)

(defn gen-compare-by-val [m] (fn [k0 k1] (compare [(get m k1) k1] [(get m k0) k0])))

(defn sorted-map-by-val [m & kvs] (apply sorted-map-by (gen-compare-by-val m) kvs))

;; TODO generate these functions via macros
(defn #?(:clj ^TreeMap !sorted-map-by :cljs !sorted-map-by)
  "Creates a single-threaded, mutable sorted map with the specified comparator.
   On the JVM, this is a `java.util.TreeMap`.
   On JS, this is a `goog.structs.AvlTree`."
  ([compf] #?(:clj (TreeMap. compf) :cljs (AvlTree. compf)))
  ([compf k0 v0]
    (doto #?(:clj (TreeMap. compf) :cljs (AvlTree. compf))
          (#?(:clj .put :cljs .add) k0 v0)))
  ([compf k0 v0 k1 v1]
    (doto #?(:clj (TreeMap. compf) :cljs (AvlTree. compf))
          (#?(:clj .put :cljs .add) k0 v0)
          (#?(:clj .put :cljs .add) k1 v1)))
  ([compf k0 v0 k1 v1 k2 v2]
    (doto #?(:clj (TreeMap. compf) :cljs (AvlTree. compf))
          (#?(:clj .put :cljs .add) k0 v0)
          (#?(:clj .put :cljs .add) k1 v1)
          (#?(:clj .put :cljs .add) k2 v2)))
  ([compf k0 v0 k1 v1 k2 v2 k3 v3]
    (doto #?(:clj (TreeMap. compf) :cljs (AvlTree. compf))
          (#?(:clj .put :cljs .add) k0 v0)
          (#?(:clj .put :cljs .add) k1 v1)
          (#?(:clj .put :cljs .add) k2 v2)
          (#?(:clj .put :cljs .add) k3 v3)))
  ([compf k0 v0 k1 v1 k2 v2 k3 v3 k4 v4]
    (doto #?(:clj (TreeMap. compf) :cljs (AvlTree. compf))
          (#?(:clj .put :cljs .add) k0 v0)
          (#?(:clj .put :cljs .add) k1 v1)
          (#?(:clj .put :cljs .add) k2 v2)
          (#?(:clj .put :cljs .add) k3 v3)
          (#?(:clj .put :cljs .add) k4 v4)))
  ([compf k0 v0 k1 v1 k2 v2 k3 v3 k4 v4 k5 v5]
    (doto #?(:clj (TreeMap. compf) :cljs (AvlTree. compf))
          (#?(:clj .put :cljs .add) k0 v0)
          (#?(:clj .put :cljs .add) k1 v1)
          (#?(:clj .put :cljs .add) k2 v2)
          (#?(:clj .put :cljs .add) k3 v3)
          (#?(:clj .put :cljs .add) k4 v4)
          (#?(:clj .put :cljs .add) k5 v5)))
  ([compf k0 v0 k1 v1 k2 v2 k3 v3 k4 v4 k5 v5 k6 v6 & kvs]
    (reduce-pair
      (fn [#?(:clj ^TreeMap m :cljs m) k v] (doto m (#?(:clj .put :cljs .add) k v)))
      (doto #?(:clj (TreeMap. compf) :cljs (AvlTree. compf))
            (#?(:clj .put :cljs .add) k0 v0)
            (#?(:clj .put :cljs .add) k1 v1)
            (#?(:clj .put :cljs .add) k2 v2)
            (#?(:clj .put :cljs .add) k3 v3)
            (#?(:clj .put :cljs .add) k4 v4)
            (#?(:clj .put :cljs .add) k5 v5)
            (#?(:clj .put :cljs .add) k6 v6))
      kvs)))

;; TODO generate these functions via macros
(defn #?(:clj ^TreeMap !sorted-map :cljs !sorted-map)
  "Creates a single-threaded, mutable sorted map.
   On the JVM, this is a `java.util.TreeMap`.
   On JS, this is a `goog.structs.AvlTree`."
  ([] (!sorted-map-by compare))
  ([k0 v0] (!sorted-map-by compare k0 v0))
  ([k0 v0 k1 v1] (!sorted-map-by compare k0 v0 k1 v1))
  ([k0 v0 k1 v1 k2 v2] (!sorted-map-by compare k0 v0 k1 v1 k2 v2))
  ([k0 v0 k1 v1 k2 v2 k3 v3] (!sorted-map-by compare k0 v0 k1 v1 k2 v2 k3 v3))
  ([k0 v0 k1 v1 k2 v2 k3 v3 k4 v4] (!sorted-map-by compare k0 v0 k1 v1 k2 v2 k3 v3 k4 v4))
  ([k0 v0 k1 v1 k2 v2 k3 v3 k4 v4 k5 v5]
    (!sorted-map-by compare k0 v0 k1 v1 k2 v2 k3 v3 k4 v4 k5 v5))
  ([k0 v0 k1 v1 k2 v2 k3 v3 k4 v4 k5 v5 k6 v6 & kvs]
    (apply !sorted-map-by compare k0 v0 k1 v1 k2 v2 k3 v3 k4 v4 k5 v5 k6 v6 kvs)))

(defn !sorted-map-by-val [m & kvs] (apply !sorted-map-by (gen-compare-by-val m) kvs))

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
