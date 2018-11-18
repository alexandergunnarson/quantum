(ns quantum.untyped.core.data.map
  "Map functions. |map-entry|, a better merge, sorted-maps, etc."
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
    [quantum.untyped.core.identifiers
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

(defn +map-entry? [x] (instance? #?(:clj clojure.lang.MapEntry :cljs cljs.core.MapEntry) x))

;; ----- Hash maps ----- ;;

;; TODO TYPED — use `deftypet` and also typed internals
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
              (cons (>map-entry nil nil-val) s)
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
          (when has-nil? (>map-entry nil nil-val))
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

#?(:clj (def hash-map? (partial instance? clojure.lang.PersistentHashMap)))

        (defalias hash-map core/hash-map)

#?(:clj (defalias hash-map|long->ref imap/int-map))
#?(:clj (defalias int-map hash-map|long->ref))

(defn >!hash-map
  "Creates a single-threaded, mutable hash map.
   On the JVM, this is a `java.util.HashMap`.
   On JS, this is a `quantum.untyped.core.data.map.HashMap`."
  ([] #?(:clj (HashMap.) :cljs (MutableHashMap. nil 0 (js/Map.) false nil nil))))

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

(defn >!sorted-map-by
  "Creates a single-threaded, mutable sorted map with the specified comparator.
   On the JVM, this is a `java.util.TreeMap`.
   On JS, this is a `goog.structs.AvlTree`."
  ([compf] #?(:clj (TreeMap. ^java.util.Comparator compf) :cljs (AvlTree. compf))))

(defn >!sorted-map [] (>!sorted-map-by compare))

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
