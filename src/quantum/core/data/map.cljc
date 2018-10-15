(ns ^{:attribution "alexandergunnarson"}
  quantum.core.data.map
  "Useful map functions. |map-entry|, a better merge, sorted-maps, etc."
  (:refer-clojure :exclude
    [split-at, map?, merge, sorted-map sorted-map-by])
  (:require
    #?(:clj [clojure.data.int-map])
            ;; TODO TYPED
          #_[quantum.core.reducers         :as r
              :refer [reduce-pair]]
            [quantum.core.type             :as t]
            [quantum.untyped.core.data.map :as umap]
            ;; TODO TYPED
            [quantum.untyped.core.defnt
              :refer [defns-]]
            [quantum.untyped.core.type     :as ut]
            ;; TODO TYPED
            [quantum.untyped.core.vars     :as uvar
              :refer [defalias def- defmacro-]])
  (:import
#?@(:clj  [[java.util HashMap IdentityHashMap LinkedHashMap TreeMap]
           [it.unimi.dsi.fastutil.ints    Int2ReferenceOpenHashMap]
           [it.unimi.dsi.fastutil.longs   Long2LongOpenHashMap
                                          Long2ReferenceOpenHashMap]
           [it.unimi.dsi.fastutil.doubles Double2ReferenceOpenHashMap]
           [it.unimi.dsi.fastutil.objects Reference2LongOpenHashMap]]
    :cljs [[goog.structs AvlTree LinkedMap]])))

;; TODO make a wrapper fn/type for associative data structures such that it maintains a
;; bidirectional mapping/index between keys and values

;; TO EXPLORE
;; - Optimizing Hash-Array Mapped Tries for Fast and Lean Immutable JVM Collections
;;   - Actual usable implementation: https://github.com/usethesource/capsule
;;   - http://michael.steindorfer.name/publications/oopsla15.pdf
;;   - Overall significantly faster on what they've chosen to measure.
;;   - Alex Miller: "We have seen it and will probably investigate some of these ideas after 1.8."
;; =======================

(def- basic-type-syms-for-maps '[boolean byte short char int long float double ref])

#?(:clj
(defns- >v-sym [prefix symbol?, kind symbol? > symbol?]
  (symbol (str prefix "|" kind "?"))))

#?(:clj
(defns- >kv-sym [prefix symbol?, from-type symbol?, to-type symbol? > symbol?]
  (symbol (str prefix "|" from-type "->" to-type "?"))))

#?(:clj
(defmacro- def-preds|map|same-types [prefix #_symbol?]
  `(do ~@(for [kind (conj basic-type-syms-for-maps 'any)]
           (list 'def (>v-sym prefix kind) (>kv-sym prefix kind kind))))))

#?(:clj
(defmacro- def-preds|map|any [prefix #_symbol?]
  (let [anys (->> (for [kind basic-type-syms-for-maps]
                    [(list 'def (>kv-sym prefix kind 'any)
                                (->> basic-type-syms-for-maps
                                     (map #(>kv-sym prefix kind %))
                                     (list* `t/or)))
                     (list 'def (>kv-sym prefix 'any kind)
                                (->> basic-type-syms-for-maps
                                     (map #(>kv-sym prefix % kind))
                                     (list* `t/or)))])
                  (apply concat))
        any->any (list 'def (>kv-sym prefix 'any 'any)
                            (->> basic-type-syms-for-maps
                                 (map #(vector (>kv-sym prefix 'any %) (>kv-sym prefix % 'any)))
                                 (apply concat)
                                 (list* `t/or)))]
    `(do ~@(concat anys [any->any])))))

;; ===== Map entries ===== ;;

(def +map-entry? (t/isa? #?(:clj clojure.lang.MapEntry :cljs cljs.core.MapEntry)))

(t/defn ^:inline >map-entry
  "A performant replacement for creating 2-tuples (vectors), e.g., as return values
   in a `kv-reduce` function.

   Now overshadowed by ztellman's unrolled vectors in 1.8.0.

   Time to create 100000000 2-tuples:
   new tuple-vector 55.816415 ms
   map-entry        37.542442 ms

   However, insertion into maps is faster with map-entry:

   (def vs [[1 2] [3 4]])
   (def ms [(>map-entry 1 2) (>map-entry 3 4)])
   (def m0 {})
   508.122831 ms (dotimes [n 1000000] (into m0 vs))
   310.335998 ms (dotimes [n 1000000] (into m0 ms))"
  {:attribution "alexandergunnarson"}
  > +map-entry?
  [k t/ref?, v t/ref?]
  #?(:clj  (clojure.lang.MapEntry. k v)
     :cljs (cljs.core.MapEntry. k v nil)))

;; ===== Unordered identity-semantic (identity-based equality) maps ===== ;;

         (def  !identity-map|ref->ref?
               #?(:clj (t/isa? java.util.IdentityHashMap) :cljs (t/isa? js/Map)))

         (def  !identity-map? !identity-map|ref->ref?)

#?(:clj  (def !!identity-map? t/none?))

         (def   identity-map? (t/or !identity-map? #?(:clj !!identity-map?)))

;; TODO generate this via macro?
(t/defn >!identity-map
  "Creates a single-threaded, mutable identity map.
   On the JVM, this is a `java.util.IdentityHashMap`.
   On JS, this is a `js/Map` (ECMAScript 6 Map)."
  > !identity-map?
  ([] #?(:clj (IdentityHashMap.) :cljs (js/Map.)))
  ([k0 t/ref?, v0 t/ref?]
    (doto #?(:clj (IdentityHashMap.) :cljs (js/Map.))
          (#?(:clj .put :cljs .set) k0 v0)))
  ([k0 t/ref?, v0 t/ref?, k1 t/ref?, v1 t/ref?]
    (doto #?(:clj (IdentityHashMap.) :cljs (js/Map.))
          (#?(:clj .put :cljs .set) k0 v0)
          (#?(:clj .put :cljs .set) k1 v1)))
  ([k0 t/ref?, v0 t/ref?, k1 t/ref?, v1 t/ref?, k2 t/ref?, v2 t/ref?]
    (doto #?(:clj (IdentityHashMap.) :cljs (js/Map.))
          (#?(:clj .put :cljs .set) k0 v0)
          (#?(:clj .put :cljs .set) k1 v1)
          (#?(:clj .put :cljs .set) k2 v2)))
  ([k0 t/ref?, v0 t/ref?, k1 t/ref?, v1 t/ref?, k2 t/ref?, v2 t/ref?, k3 t/ref?, v3 t/ref?]
    (doto #?(:clj (IdentityHashMap.) :cljs (js/Map.))
          (#?(:clj .put :cljs .set) k0 v0)
          (#?(:clj .put :cljs .set) k1 v1)
          (#?(:clj .put :cljs .set) k2 v2)
          (#?(:clj .put :cljs .set) k3 v3)))
  ([k0 t/ref?, v0 t/ref?, k1 t/ref?, v1 t/ref?, k2 t/ref?, v2 t/ref?, k3 t/ref?, v3 t/ref?
    k4 t/ref?, v4 t/ref?]
    (doto #?(:clj (IdentityHashMap.) :cljs (js/Map.))
          (#?(:clj .put :cljs .set) k0 v0)
          (#?(:clj .put :cljs .set) k1 v1)
          (#?(:clj .put :cljs .set) k2 v2)
          (#?(:clj .put :cljs .set) k3 v3)
          (#?(:clj .put :cljs .set) k4 v4)))
  ([k0 t/ref?, v0 t/ref?, k1 t/ref?, v1 t/ref?, k2 t/ref?, v2 t/ref?, k3 t/ref?, v3 t/ref?
    k4 t/ref?, v4 t/ref?, k5 t/ref?, v5 t/ref?]
    (doto #?(:clj (IdentityHashMap.) :cljs (js/Map.))
          (#?(:clj .put :cljs .set) k0 v0)
          (#?(:clj .put :cljs .set) k1 v1)
          (#?(:clj .put :cljs .set) k2 v2)
          (#?(:clj .put :cljs .set) k3 v3)
          (#?(:clj .put :cljs .set) k4 v4)
          (#?(:clj .put :cljs .set) k5 v5)))
  ;; TODO TYPED handle varargs
#_([k0 t/ref?, v0 t/ref?, k1 t/ref?, v1 t/ref?, k2 t/ref?, v2 t/ref?, k3 t/ref?, v3 t/ref?
    k4 t/ref?, v4 t/ref?, k5 t/ref?, v5 t/ref?, k6 t/ref?, v6 t/ref? & kvs _]
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

;; ===== Unordered value-semantic (value-based equality) maps ===== ;;

;; ----- Array maps ----- ;;

(def   +array-map? (t/isa? #?(:clj  clojure.lang.PersistentArrayMap
                              :cljs cljs.core/PersistentArrayMap)))

(def  !+array-map? (t/isa? #?(:clj  clojure.lang.PersistentArrayMap$TransientArrayMap
                              :cljs cljs.core/TransientArrayMap)))

(def ?!+array-map? (t/or !+array-map? +array-map?))

(def !array-map|boolean->boolean? t/none?)
(def !array-map|boolean->byte?    t/none?)
(def !array-map|boolean->short?   t/none?)
(def !array-map|boolean->char?    t/none?)
(def !array-map|boolean->int?     t/none?)
(def !array-map|boolean->long?    t/none?)
(def !array-map|boolean->float?   t/none?)
(def !array-map|boolean->double?  t/none?)
(def !array-map|boolean->ref?     t/none?)

(def !array-map|byte->boolean?
     #?(:clj  (t/isa? it.unimi.dsi.fastutil.bytes.Byte2BooleanArrayMap)          :cljs t/none?))
(def !array-map|byte->byte?
     #?(:clj  (t/isa? it.unimi.dsi.fastutil.bytes.Byte2ByteArrayMap)             :cljs t/none?))
(def !array-map|byte->short?
     #?(:clj  (t/isa? it.unimi.dsi.fastutil.bytes.Byte2ShortArrayMap)            :cljs t/none?))
(def !array-map|byte->char?
     #?(:clj  (t/isa? it.unimi.dsi.fastutil.bytes.Byte2CharArrayMap)             :cljs t/none?))
(def !array-map|byte->int?
     #?(:clj  (t/isa? it.unimi.dsi.fastutil.bytes.Byte2IntArrayMap)              :cljs t/none?))
(def !array-map|byte->long?
     #?(:clj  (t/isa? it.unimi.dsi.fastutil.bytes.Byte2LongArrayMap)             :cljs t/none?))
(def !array-map|byte->float?
     #?(:clj  (t/isa? it.unimi.dsi.fastutil.bytes.Byte2FloatArrayMap)            :cljs t/none?))
(def !array-map|byte->double?
     #?(:clj  (t/isa? it.unimi.dsi.fastutil.bytes.Byte2DoubleArrayMap)           :cljs t/none?))
(def !array-map|byte->ref?
     #?(:clj  (t/isa? it.unimi.dsi.fastutil.bytes.Byte2ReferenceArrayMap)        :cljs t/none?))

(def !array-map|short->boolean?
     #?(:clj  (t/isa? it.unimi.dsi.fastutil.shorts.Short2BooleanArrayMap)        :cljs t/none?))
(def !array-map|short->byte?
     #?(:clj  (t/isa? it.unimi.dsi.fastutil.shorts.Short2ByteArrayMap)           :cljs t/none?))
(def !array-map|short->short?
     #?(:clj  (t/isa? it.unimi.dsi.fastutil.shorts.Short2ShortArrayMap)          :cljs t/none?))
(def !array-map|short->char?
     #?(:clj  (t/isa? it.unimi.dsi.fastutil.shorts.Short2CharArrayMap)           :cljs t/none?))
(def !array-map|short->int?
     #?(:clj  (t/isa? it.unimi.dsi.fastutil.shorts.Short2IntArrayMap)            :cljs t/none?))
(def !array-map|short->long?
     #?(:clj  (t/isa? it.unimi.dsi.fastutil.shorts.Short2LongArrayMap)           :cljs t/none?))
(def !array-map|short->float?
     #?(:clj  (t/isa? it.unimi.dsi.fastutil.shorts.Short2FloatArrayMap)          :cljs t/none?))
(def !array-map|short->double?
     #?(:clj  (t/isa? it.unimi.dsi.fastutil.shorts.Short2DoubleArrayMap)         :cljs t/none?))
(def !array-map|short->ref?
     #?(:clj  (t/isa? it.unimi.dsi.fastutil.shorts.Short2ReferenceArrayMap)      :cljs t/none?))

(def !array-map|char->ref?
     #?(:clj  (t/isa? it.unimi.dsi.fastutil.chars.Char2ReferenceArrayMap)        :cljs t/none?))
(def !array-map|char->boolean?
     #?(:clj  (t/isa? it.unimi.dsi.fastutil.chars.Char2BooleanArrayMap)          :cljs t/none?))
(def !array-map|char->byte?
     #?(:clj  (t/isa? it.unimi.dsi.fastutil.chars.Char2ByteArrayMap)             :cljs t/none?))
(def !array-map|char->short?
     #?(:clj  (t/isa? it.unimi.dsi.fastutil.chars.Char2ShortArrayMap)            :cljs t/none?))
(def !array-map|char->char?
     #?(:clj  (t/isa? it.unimi.dsi.fastutil.chars.Char2CharArrayMap)             :cljs t/none?))
(def !array-map|char->int?
     #?(:clj  (t/isa? it.unimi.dsi.fastutil.chars.Char2IntArrayMap)              :cljs t/none?))
(def !array-map|char->long?
     #?(:clj  (t/isa? it.unimi.dsi.fastutil.chars.Char2LongArrayMap)             :cljs t/none?))
(def !array-map|char->float?
     #?(:clj  (t/isa? it.unimi.dsi.fastutil.chars.Char2FloatArrayMap)            :cljs t/none?))
(def !array-map|char->double?
     #?(:clj  (t/isa? it.unimi.dsi.fastutil.chars.Char2DoubleArrayMap)           :cljs t/none?))

(def !array-map|int->boolean?
     #?(:clj  (t/isa? it.unimi.dsi.fastutil.ints.Int2BooleanArrayMap)            :cljs t/none?))
(def !array-map|int->byte?
     #?(:clj  (t/isa? it.unimi.dsi.fastutil.ints.Int2ByteArrayMap)               :cljs t/none?))
(def !array-map|int->short?
     #?(:clj  (t/isa? it.unimi.dsi.fastutil.ints.Int2ShortArrayMap)              :cljs t/none?))
(def !array-map|int->char?
     #?(:clj  (t/isa? it.unimi.dsi.fastutil.ints.Int2CharArrayMap)               :cljs t/none?))
(def !array-map|int->int?
     #?(:clj  (t/isa? it.unimi.dsi.fastutil.ints.Int2IntArrayMap)                :cljs t/none?))
(def !array-map|int->long?
     #?(:clj  (t/isa? it.unimi.dsi.fastutil.ints.Int2LongArrayMap)               :cljs t/none?))
(def !array-map|int->float?
     #?(:clj  (t/isa? it.unimi.dsi.fastutil.ints.Int2FloatArrayMap)              :cljs t/none?))
(def !array-map|int->double?
     #?(:clj  (t/isa? it.unimi.dsi.fastutil.ints.Int2DoubleArrayMap)             :cljs t/none?))
(def !array-map|int->ref?
     #?(:clj  (t/isa? it.unimi.dsi.fastutil.ints.Int2ReferenceArrayMap)          :cljs t/none?))

(def !array-map|long->boolean?
     #?(:clj  (t/isa? it.unimi.dsi.fastutil.longs.Long2BooleanArrayMap)          :cljs t/none?))
(def !array-map|long->byte?
     #?(:clj  (t/isa? it.unimi.dsi.fastutil.longs.Long2ByteArrayMap)             :cljs t/none?))
(def !array-map|long->short?
     #?(:clj  (t/isa? it.unimi.dsi.fastutil.longs.Long2ShortArrayMap)            :cljs t/none?))
(def !array-map|long->char?
     #?(:clj  (t/isa? it.unimi.dsi.fastutil.longs.Long2CharArrayMap)             :cljs t/none?))
(def !array-map|long->int?
     #?(:clj  (t/isa? it.unimi.dsi.fastutil.longs.Long2IntArrayMap)              :cljs t/none?))
(def !array-map|long->long?
     #?(:clj  (t/isa? it.unimi.dsi.fastutil.longs.Long2LongArrayMap)             :cljs t/none?))
(def !array-map|long->float?
     #?(:clj  (t/isa? it.unimi.dsi.fastutil.longs.Long2FloatArrayMap)            :cljs t/none?))
(def !array-map|long->double?
     #?(:clj  (t/isa? it.unimi.dsi.fastutil.longs.Long2DoubleArrayMap)           :cljs t/none?))
(def !array-map|long->ref?
     #?(:clj  (t/isa? it.unimi.dsi.fastutil.longs.Long2ReferenceArrayMap)        :cljs t/none?))

(def !array-map|float->boolean?
     #?(:clj  (t/isa? it.unimi.dsi.fastutil.floats.Float2BooleanArrayMap)        :cljs t/none?))
(def !array-map|float->byte?
     #?(:clj  (t/isa? it.unimi.dsi.fastutil.floats.Float2ByteArrayMap)           :cljs t/none?))
(def !array-map|float->short?
     #?(:clj  (t/isa? it.unimi.dsi.fastutil.floats.Float2ShortArrayMap)          :cljs t/none?))
(def !array-map|float->char?
     #?(:clj  (t/isa? it.unimi.dsi.fastutil.floats.Float2CharArrayMap)           :cljs t/none?))
(def !array-map|float->int?
     #?(:clj  (t/isa? it.unimi.dsi.fastutil.floats.Float2IntArrayMap)            :cljs t/none?))
(def !array-map|float->long?
     #?(:clj  (t/isa? it.unimi.dsi.fastutil.floats.Float2LongArrayMap)           :cljs t/none?))
(def !array-map|float->float?
     #?(:clj  (t/isa? it.unimi.dsi.fastutil.floats.Float2FloatArrayMap)          :cljs t/none?))
(def !array-map|float->double?
     #?(:clj  (t/isa? it.unimi.dsi.fastutil.floats.Float2DoubleArrayMap)         :cljs t/none?))
(def !array-map|float->ref?
     #?(:clj  (t/isa? it.unimi.dsi.fastutil.floats.Float2ReferenceArrayMap)      :cljs t/none?))

(def !array-map|double->boolean?
     #?(:clj  (t/isa? it.unimi.dsi.fastutil.doubles.Double2BooleanArrayMap)      :cljs t/none?))
(def !array-map|double->byte?
     #?(:clj  (t/isa? it.unimi.dsi.fastutil.doubles.Double2ByteArrayMap)         :cljs t/none?))
(def !array-map|double->short?
     #?(:clj  (t/isa? it.unimi.dsi.fastutil.doubles.Double2ShortArrayMap)        :cljs t/none?))
(def !array-map|double->char?
     #?(:clj  (t/isa? it.unimi.dsi.fastutil.doubles.Double2CharArrayMap)         :cljs t/none?))
(def !array-map|double->int?
     #?(:clj  (t/isa? it.unimi.dsi.fastutil.doubles.Double2IntArrayMap)          :cljs t/none?))
(def !array-map|double->long?
     #?(:clj  (t/isa? it.unimi.dsi.fastutil.doubles.Double2LongArrayMap)         :cljs t/none?))
(def !array-map|double->float?
     #?(:clj  (t/isa? it.unimi.dsi.fastutil.doubles.Double2FloatArrayMap)        :cljs t/none?))
(def !array-map|double->double?
     #?(:clj  (t/isa? it.unimi.dsi.fastutil.doubles.Double2DoubleArrayMap)       :cljs t/none?))
(def !array-map|double->ref?
     #?(:clj  (t/isa? it.unimi.dsi.fastutil.doubles.Double2ReferenceArrayMap)    :cljs t/none?))

(def !array-map|ref->boolean?
     #?(:clj  (t/isa? it.unimi.dsi.fastutil.objects.Reference2BooleanArrayMap)   :cljs t/none?))
(def !array-map|ref->byte?
     #?(:clj  (t/isa? it.unimi.dsi.fastutil.objects.Reference2ByteArrayMap)      :cljs t/none?))
(def !array-map|ref->short?
     #?(:clj  (t/isa? it.unimi.dsi.fastutil.objects.Reference2ShortArrayMap)     :cljs t/none?))
(def !array-map|ref->char?
     #?(:clj  (t/isa? it.unimi.dsi.fastutil.objects.Reference2CharArrayMap)      :cljs t/none?))
(def !array-map|ref->int?
     #?(:clj  (t/isa? it.unimi.dsi.fastutil.objects.Reference2IntArrayMap)       :cljs t/none?))
(def !array-map|ref->long?
     #?(:clj  (t/isa? it.unimi.dsi.fastutil.objects.Reference2LongArrayMap)      :cljs t/none?))
(def !array-map|ref->float?
     #?(:clj  (t/isa? it.unimi.dsi.fastutil.objects.Reference2FloatArrayMap)     :cljs t/none?))
(def !array-map|ref->double?
     #?(:clj  (t/isa? it.unimi.dsi.fastutil.objects.Reference2DoubleArrayMap)    :cljs t/none?))
(def !array-map|ref->ref?
     #?(:clj  (t/isa? it.unimi.dsi.fastutil.objects.Reference2ReferenceArrayMap) :cljs t/none?))

         (def-preds|map|any        !array-map)

         (def-preds|map|same-types !array-map)

         (def  !array-map? !array-map|any?)

#?(:clj  (def !!array-map? t/none?))

         (def   array-map? (t/or ?!+array-map? !array-map? #?(:clj !!array-map?)))

(t/defn >array-map
  "Creates a persistent array map. If any keys are equal, they are handled as if by repeated
   applications of `assoc`."
  > +array-map?
  ([] ^:val (. clojure.lang.PersistentArrayMap EMPTY))
  ;; TODO TYPED handle varargs
#_([& kvs]
     (clojure.lang.PersistentArrayMap/createAsIfByAssoc (to-array kvs))))

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

(def   +hash-map? (t/isa? #?(:clj  clojure.lang.PersistentHashMap
                             :cljs cljs.core/PersistentHashMap)))

(def  !+hash-map? (t/isa? #?(:clj  clojure.lang.PersistentHashMap$TransientHashMap
                             :cljs cljs.core/TransientHashMap)))

(def ?!+hash-map? (t/or !+hash-map? +hash-map?))

(t/defn >hash-map
  "Creates a persistent hash map. If any keys are equal, they are handled as if by repeated
   applications of `assoc`.

   `(->> pairs (apply concat) (apply >hash-map))` <~> `lodash/fromPairs`"
  > +hash-map?
  ([] ^:val (. clojure.lang.PersistentHashMap EMPTY))
  ;; TODO TYPED handle varargs
#_([& kvs]
     (clojure.lang.PersistentHashMap/create kvs)))

(def !hash-map|boolean->boolean? t/none?)
(def !hash-map|boolean->byte?    t/none?)
(def !hash-map|boolean->short?   t/none?)
(def !hash-map|boolean->char?    t/none?)
(def !hash-map|boolean->int?     t/none?)
(def !hash-map|boolean->long?    t/none?)
(def !hash-map|boolean->float?   t/none?)
(def !hash-map|boolean->double?  t/none?)
(def !hash-map|boolean->ref?     t/none?)

(def !hash-map|byte->boolean?
     #?(:clj  (t/or (t/isa? it.unimi.dsi.fastutil.bytes.Byte2BooleanOpenHashMap)
                    (t/isa? it.unimi.dsi.fastutil.bytes.Byte2BooleanOpenCustomHashMap))
        :cljs t/none?))
(def !hash-map|byte->byte?
     #?(:clj  (t/or (t/isa? it.unimi.dsi.fastutil.bytes.Byte2ByteOpenHashMap)
                    (t/isa? it.unimi.dsi.fastutil.bytes.Byte2ByteOpenCustomHashMap))
        :cljs t/none?))
(def !hash-map|byte->short?
     #?(:clj  (t/or (t/isa? it.unimi.dsi.fastutil.bytes.Byte2ShortOpenHashMap)
                    (t/isa? it.unimi.dsi.fastutil.bytes.Byte2ShortOpenCustomHashMap))
        :cljs t/none?))
(def !hash-map|byte->char?
     #?(:clj  (t/or (t/isa? it.unimi.dsi.fastutil.bytes.Byte2CharOpenHashMap)
                    (t/isa? it.unimi.dsi.fastutil.bytes.Byte2CharOpenCustomHashMap))
        :cljs t/none?))
(def !hash-map|byte->int?
     #?(:clj  (t/or (t/isa? it.unimi.dsi.fastutil.bytes.Byte2IntOpenHashMap)
                    (t/isa? it.unimi.dsi.fastutil.bytes.Byte2IntOpenCustomHashMap))
        :cljs t/none?))
(def !hash-map|byte->long?
     #?(:clj  (t/or (t/isa? it.unimi.dsi.fastutil.bytes.Byte2LongOpenHashMap)
                    (t/isa? it.unimi.dsi.fastutil.bytes.Byte2LongOpenCustomHashMap))
        :cljs t/none?))
(def !hash-map|byte->float?
     #?(:clj  (t/or (t/isa? it.unimi.dsi.fastutil.bytes.Byte2FloatOpenHashMap)
                    (t/isa? it.unimi.dsi.fastutil.bytes.Byte2FloatOpenCustomHashMap))
        :cljs t/none?))
(def !hash-map|byte->double?
     #?(:clj  (t/or (t/isa? it.unimi.dsi.fastutil.bytes.Byte2DoubleOpenHashMap)
                    (t/isa? it.unimi.dsi.fastutil.bytes.Byte2DoubleOpenCustomHashMap))
        :cljs t/none?))
(def !hash-map|byte->ref?
     #?(:clj  (t/or (t/isa? it.unimi.dsi.fastutil.bytes.Byte2ReferenceOpenHashMap)
                    (t/isa? it.unimi.dsi.fastutil.bytes.Byte2ReferenceOpenCustomHashMap))
        :cljs t/none?))

(def !hash-map|short->boolean?
     #?(:clj (t/or (t/isa? it.unimi.dsi.fastutil.shorts.Short2BooleanOpenHashMap)
                   (t/isa? it.unimi.dsi.fastutil.shorts.Short2BooleanOpenCustomHashMap))
        :cljs t/none?))
(def !hash-map|short->byte?
     #?(:clj (t/or (t/isa? it.unimi.dsi.fastutil.shorts.Short2ByteOpenHashMap)
                   (t/isa? it.unimi.dsi.fastutil.shorts.Short2ByteOpenCustomHashMap))
        :cljs t/none?))
(def !hash-map|short->short?
     #?(:clj (t/or (t/isa? it.unimi.dsi.fastutil.shorts.Short2ShortOpenHashMap)
                   (t/isa? it.unimi.dsi.fastutil.shorts.Short2ShortOpenCustomHashMap))
        :cljs t/none?))
(def !hash-map|short->char?
     #?(:clj (t/or (t/isa? it.unimi.dsi.fastutil.shorts.Short2CharOpenHashMap)
                   (t/isa? it.unimi.dsi.fastutil.shorts.Short2CharOpenCustomHashMap))
        :cljs t/none?))
(def !hash-map|short->int?
     #?(:clj (t/or (t/isa? it.unimi.dsi.fastutil.shorts.Short2IntOpenHashMap)
                   (t/isa? it.unimi.dsi.fastutil.shorts.Short2IntOpenCustomHashMap))
        :cljs t/none?))
(def !hash-map|short->long?
     #?(:clj (t/or (t/isa? it.unimi.dsi.fastutil.shorts.Short2LongOpenHashMap)
                   (t/isa? it.unimi.dsi.fastutil.shorts.Short2LongOpenCustomHashMap))
        :cljs t/none?))
(def !hash-map|short->float?
     #?(:clj (t/or (t/isa? it.unimi.dsi.fastutil.shorts.Short2FloatOpenHashMap)
                   (t/isa? it.unimi.dsi.fastutil.shorts.Short2FloatOpenCustomHashMap))
        :cljs t/none?))
(def !hash-map|short->double?
     #?(:clj (t/or (t/isa? it.unimi.dsi.fastutil.shorts.Short2DoubleOpenHashMap)
                   (t/isa? it.unimi.dsi.fastutil.shorts.Short2DoubleOpenCustomHashMap))
        :cljs t/none?))
(def !hash-map|short->ref?
     #?(:clj (t/or (t/isa? it.unimi.dsi.fastutil.shorts.Short2ReferenceOpenHashMap)
                   (t/isa? it.unimi.dsi.fastutil.shorts.Short2ReferenceOpenCustomHashMap))
        :cljs t/none?))

(def !hash-map|char->ref?
     #?(:clj (t/or (t/isa? it.unimi.dsi.fastutil.chars.Char2ReferenceOpenHashMap)
                   (t/isa? it.unimi.dsi.fastutil.chars.Char2ReferenceOpenCustomHashMap))
        :cljs t/none?))
(def !hash-map|char->boolean?
     #?(:clj (t/or (t/isa? it.unimi.dsi.fastutil.chars.Char2BooleanOpenHashMap)
                   (t/isa? it.unimi.dsi.fastutil.chars.Char2BooleanOpenCustomHashMap))
        :cljs t/none?))
(def !hash-map|char->byte?
     #?(:clj (t/or (t/isa? it.unimi.dsi.fastutil.chars.Char2ByteOpenHashMap)
                   (t/isa? it.unimi.dsi.fastutil.chars.Char2ByteOpenCustomHashMap))
        :cljs t/none?))
(def !hash-map|char->short?
     #?(:clj (t/or (t/isa? it.unimi.dsi.fastutil.chars.Char2ShortOpenHashMap)
                   (t/isa? it.unimi.dsi.fastutil.chars.Char2ShortOpenCustomHashMap))
        :cljs t/none?))
(def !hash-map|char->char?
     #?(:clj (t/or (t/isa? it.unimi.dsi.fastutil.chars.Char2CharOpenHashMap)
                   (t/isa? it.unimi.dsi.fastutil.chars.Char2CharOpenCustomHashMap))
        :cljs t/none?))
(def !hash-map|char->int?
     #?(:clj (t/or (t/isa? it.unimi.dsi.fastutil.chars.Char2IntOpenHashMap)
                   (t/isa? it.unimi.dsi.fastutil.chars.Char2IntOpenCustomHashMap))
        :cljs t/none?))
(def !hash-map|char->long?
     #?(:clj (t/or (t/isa? it.unimi.dsi.fastutil.chars.Char2LongOpenHashMap)
                   (t/isa? it.unimi.dsi.fastutil.chars.Char2LongOpenCustomHashMap))
        :cljs t/none?))
(def !hash-map|char->float?
     #?(:clj (t/or (t/isa? it.unimi.dsi.fastutil.chars.Char2FloatOpenHashMap)
                   (t/isa? it.unimi.dsi.fastutil.chars.Char2FloatOpenCustomHashMap))
        :cljs t/none?))
(def !hash-map|char->double?
     #?(:clj (t/or (t/isa? it.unimi.dsi.fastutil.chars.Char2DoubleOpenHashMap)
                   (t/isa? it.unimi.dsi.fastutil.chars.Char2DoubleOpenCustomHashMap))
        :cljs t/none?))

(def !hash-map|int->boolean?
     #?(:clj  (t/or (t/isa? it.unimi.dsi.fastutil.ints.Int2BooleanOpenHashMap)
                    (t/isa? it.unimi.dsi.fastutil.ints.Int2BooleanOpenCustomHashMap))
        :cljs t/none?))
(def !hash-map|int->byte?
     #?(:clj  (t/or (t/isa? it.unimi.dsi.fastutil.ints.Int2ByteOpenHashMap)
                    (t/isa? it.unimi.dsi.fastutil.ints.Int2ByteOpenCustomHashMap))
        :cljs t/none?))
(def !hash-map|int->short?
     #?(:clj  (t/or (t/isa? it.unimi.dsi.fastutil.ints.Int2ShortOpenHashMap)
                    (t/isa? it.unimi.dsi.fastutil.ints.Int2ShortOpenCustomHashMap))
        :cljs t/none?))
(def !hash-map|int->char?
     #?(:clj  (t/or (t/isa? it.unimi.dsi.fastutil.ints.Int2CharOpenHashMap)
                    (t/isa? it.unimi.dsi.fastutil.ints.Int2CharOpenCustomHashMap))
        :cljs t/none?))
(def !hash-map|int->int?
     #?(:clj  (t/or (t/isa? it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap)
                    (t/isa? it.unimi.dsi.fastutil.ints.Int2IntOpenCustomHashMap))
        :cljs t/none?))
(def !hash-map|int->long?
     #?(:clj  (t/or (t/isa? it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap)
                    (t/isa? it.unimi.dsi.fastutil.ints.Int2LongOpenCustomHashMap))
        :cljs t/none?))
(def !hash-map|int->float?
     #?(:clj  (t/or (t/isa? it.unimi.dsi.fastutil.ints.Int2FloatOpenHashMap)
                    (t/isa? it.unimi.dsi.fastutil.ints.Int2FloatOpenCustomHashMap))
        :cljs t/none?))
(def !hash-map|int->double?
     #?(:clj  (t/or (t/isa? it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap)
                    (t/isa? it.unimi.dsi.fastutil.ints.Int2DoubleOpenCustomHashMap))
        :cljs t/none?))
(def !hash-map|int->ref?
     #?(:clj  (t/or (t/isa? it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap)
                    (t/isa? it.unimi.dsi.fastutil.ints.Int2ReferenceOpenCustomHashMap))
        :cljs t/none?))

(def !hash-map|long->boolean?
     #?(:clj (t/or (t/isa? it.unimi.dsi.fastutil.longs.Long2BooleanOpenHashMap)
                   (t/isa? it.unimi.dsi.fastutil.longs.Long2BooleanOpenCustomHashMap))
        :cljs t/none?))
(def !hash-map|long->byte?
     #?(:clj (t/or (t/isa? it.unimi.dsi.fastutil.longs.Long2ByteOpenCustomHashMap)
                   (t/isa? it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap))
        :cljs t/none?))
(def !hash-map|long->short?
     #?(:clj (t/or (t/isa? it.unimi.dsi.fastutil.longs.Long2ShortOpenHashMap)
                   (t/isa? it.unimi.dsi.fastutil.longs.Long2ShortOpenCustomHashMap))
        :cljs t/none?))
(def !hash-map|long->char?
     #?(:clj (t/or (t/isa? it.unimi.dsi.fastutil.longs.Long2CharOpenHashMap)
                   (t/isa? it.unimi.dsi.fastutil.longs.Long2CharOpenCustomHashMap))
        :cljs t/none?))
(def !hash-map|long->int?
     #?(:clj (t/or (t/isa? it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap)
                   (t/isa? it.unimi.dsi.fastutil.longs.Long2IntOpenCustomHashMap))
        :cljs t/none?))
(def !hash-map|long->long?
     #?(:clj (t/or (t/isa? it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap)
                   (t/isa? it.unimi.dsi.fastutil.longs.Long2LongOpenCustomHashMap))
        :cljs t/none?))
(def !hash-map|long->float?
     #?(:clj (t/or (t/isa? it.unimi.dsi.fastutil.longs.Long2FloatOpenHashMap)
                   (t/isa? it.unimi.dsi.fastutil.longs.Long2FloatOpenCustomHashMap))
        :cljs t/none?))
(def !hash-map|long->double?
     #?(:clj (t/or (t/isa? it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap)
                   (t/isa? it.unimi.dsi.fastutil.longs.Long2DoubleOpenCustomHashMap))
        :cljs t/none?))
(def !hash-map|long->ref?
     #?(:clj (t/or (t/isa? it.unimi.dsi.fastutil.longs.Long2ReferenceOpenHashMap)
                   (t/isa? it.unimi.dsi.fastutil.longs.Long2ReferenceOpenCustomHashMap))
        :cljs t/none?))

(def !hash-map|float->boolean?
     #?(:clj (t/or (t/isa? it.unimi.dsi.fastutil.floats.Float2BooleanOpenHashMap)
                   (t/isa? it.unimi.dsi.fastutil.floats.Float2BooleanOpenCustomHashMap))
        :cljs t/none?))
(def !hash-map|float->byte?
     #?(:clj (t/or (t/isa? it.unimi.dsi.fastutil.floats.Float2ByteOpenHashMap)
                   (t/isa? it.unimi.dsi.fastutil.floats.Float2ByteOpenCustomHashMap))
        :cljs t/none?))
(def !hash-map|float->short?
     #?(:clj (t/or (t/isa? it.unimi.dsi.fastutil.floats.Float2ShortOpenHashMap)
                   (t/isa? it.unimi.dsi.fastutil.floats.Float2ShortOpenCustomHashMap))
        :cljs t/none?))
(def !hash-map|float->char?
     #?(:clj (t/or (t/isa? it.unimi.dsi.fastutil.floats.Float2CharOpenHashMap)
                   (t/isa? it.unimi.dsi.fastutil.floats.Float2CharOpenCustomHashMap))
        :cljs t/none?))
(def !hash-map|float->int?
     #?(:clj (t/or (t/isa? it.unimi.dsi.fastutil.floats.Float2IntOpenHashMap)
                   (t/isa? it.unimi.dsi.fastutil.floats.Float2IntOpenCustomHashMap))
        :cljs t/none?))
(def !hash-map|float->long?
     #?(:clj (t/or (t/isa? it.unimi.dsi.fastutil.floats.Float2LongOpenHashMap)
                   (t/isa? it.unimi.dsi.fastutil.floats.Float2LongOpenCustomHashMap))
        :cljs t/none?))
(def !hash-map|float->float?
     #?(:clj (t/or (t/isa? it.unimi.dsi.fastutil.floats.Float2FloatOpenHashMap)
                   (t/isa? it.unimi.dsi.fastutil.floats.Float2FloatOpenCustomHashMap))
        :cljs t/none?))
(def !hash-map|float->double?
     #?(:clj (t/or (t/isa? it.unimi.dsi.fastutil.floats.Float2DoubleOpenHashMap)
                   (t/isa? it.unimi.dsi.fastutil.floats.Float2DoubleOpenCustomHashMap))
        :cljs t/none?))
(def !hash-map|float->ref?
     #?(:clj (t/or (t/isa? it.unimi.dsi.fastutil.floats.Float2ReferenceOpenHashMap)
                   (t/isa? it.unimi.dsi.fastutil.floats.Float2ReferenceOpenCustomHashMap))
        :cljs t/none?))

(def !hash-map|double->boolean?
     #?(:clj  (t/or (t/isa? it.unimi.dsi.fastutil.doubles.Double2BooleanOpenHashMap)
                    (t/isa? it.unimi.dsi.fastutil.doubles.Double2BooleanOpenCustomHashMap))
        :cljs t/none?))
(def !hash-map|double->byte?
     #?(:clj  (t/or (t/isa? it.unimi.dsi.fastutil.doubles.Double2ByteOpenHashMap)
                    (t/isa? it.unimi.dsi.fastutil.doubles.Double2ByteOpenCustomHashMap))
        :cljs t/none?))
(def !hash-map|double->short?
     #?(:clj  (t/or (t/isa? it.unimi.dsi.fastutil.doubles.Double2ShortOpenHashMap)
                    (t/isa? it.unimi.dsi.fastutil.doubles.Double2ShortOpenCustomHashMap))
        :cljs t/none?))
(def !hash-map|double->char?
     #?(:clj  (t/or (t/isa? it.unimi.dsi.fastutil.doubles.Double2CharOpenHashMap)
                    (t/isa? it.unimi.dsi.fastutil.doubles.Double2CharOpenCustomHashMap))
        :cljs t/none?))
(def !hash-map|double->int?
     #?(:clj  (t/or (t/isa? it.unimi.dsi.fastutil.doubles.Double2IntOpenHashMap)
                    (t/isa? it.unimi.dsi.fastutil.doubles.Double2IntOpenCustomHashMap))
        :cljs t/none?))
(def !hash-map|double->long?
     #?(:clj  (t/or (t/isa? it.unimi.dsi.fastutil.doubles.Double2LongOpenHashMap)
                    (t/isa? it.unimi.dsi.fastutil.doubles.Double2LongOpenCustomHashMap))
        :cljs t/none?))
(def !hash-map|double->float?
     #?(:clj  (t/or (t/isa? it.unimi.dsi.fastutil.doubles.Double2FloatOpenHashMap)
                    (t/isa? it.unimi.dsi.fastutil.doubles.Double2FloatOpenCustomHashMap))
        :cljs t/none?))
(def !hash-map|double->double?
     #?(:clj  (t/or (t/isa? it.unimi.dsi.fastutil.doubles.Double2DoubleOpenHashMap)
                    (t/isa? it.unimi.dsi.fastutil.doubles.Double2DoubleOpenCustomHashMap))
        :cljs t/none?))
(def !hash-map|double->ref?
     #?(:clj  (t/or (t/isa? it.unimi.dsi.fastutil.doubles.Double2ReferenceOpenHashMap)
                    (t/isa? it.unimi.dsi.fastutil.doubles.Double2ReferenceOpenCustomHashMap))
        :cljs t/none?))

(def !hash-map|ref->boolean?
     #?(:clj  (t/or (t/isa? it.unimi.dsi.fastutil.objects.Reference2BooleanOpenHashMap)
                    (t/isa? it.unimi.dsi.fastutil.objects.Reference2BooleanOpenCustomHashMap))
        :cljs t/none?))
(def !hash-map|ref->byte?
     #?(:clj  (t/or (t/isa? it.unimi.dsi.fastutil.objects.Reference2ByteOpenHashMap)
                    (t/isa? it.unimi.dsi.fastutil.objects.Reference2ByteOpenCustomHashMap))
        :cljs t/none?))
(def !hash-map|ref->short?
     #?(:clj  (t/or (t/isa? it.unimi.dsi.fastutil.objects.Reference2ShortOpenHashMap)
                    (t/isa? it.unimi.dsi.fastutil.objects.Reference2ShortOpenCustomHashMap))
        :cljs t/none?))
(def !hash-map|ref->char?
     #?(:clj  (t/or (t/isa? it.unimi.dsi.fastutil.objects.Reference2CharOpenHashMap)
                    (t/isa? it.unimi.dsi.fastutil.objects.Reference2CharOpenCustomHashMap))
        :cljs t/none?))
(def !hash-map|ref->int?
     #?(:clj  (t/or (t/isa? it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap)
                    (t/isa? it.unimi.dsi.fastutil.objects.Reference2IntOpenCustomHashMap))
        :cljs t/none?))
(def !hash-map|ref->long?
     #?(:clj  (t/or (t/isa? it.unimi.dsi.fastutil.objects.Reference2LongOpenHashMap)
                    (t/isa? it.unimi.dsi.fastutil.objects.Reference2LongOpenCustomHashMap))
        :cljs t/none?))
(def !hash-map|ref->float?
     #?(:clj  (t/or (t/isa? it.unimi.dsi.fastutil.objects.Reference2FloatOpenHashMap)
                    (t/isa? it.unimi.dsi.fastutil.objects.Reference2FloatOpenCustomHashMap))
        :cljs t/none?))
(def !hash-map|ref->double?
     #?(:clj  (t/or (t/isa? it.unimi.dsi.fastutil.objects.Reference2DoubleOpenHashMap)
                    (t/isa? it.unimi.dsi.fastutil.objects.Reference2DoubleOpenCustomHashMap))
        :cljs t/none?))

(def !hash-map|ref->ref?
     (t/or #?@(:clj  [(t/isa? java.util.HashMap)
                      ;; Because this has different semantics
                    #_(t/isa? java.util.IdentityHashMap)
                      (t/isa? it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap)
                      (t/isa? it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenCustomHashMap)]
               :cljs [MutableHashMap])))

(def-preds|map|any        !hash-map)

(def-preds|map|same-types !hash-map)

        (def  !hash-map? !hash-map|any?)

#?(:clj (def !!hash-map? (t/isa? java.util.concurrent.ConcurrentHashMap)))
        (def   hash-map? (t/or ?!+hash-map? !hash-map? #?(:clj !!hash-map?)))

;; TODO generate this function via macro?
(t/defn >!hash-map
  "Creates a single-threaded, mutable hash map.
   On the JVM, this is a `java.util.HashMap`.
   On JS, this is a `quantum.untyped.core.data.map.HashMap`."
  > !hash-map?
  ([] #?(:clj (HashMap.) :cljs (MutableHashMap. nil 0 (js/Map.) false nil nil)))
  ([k0 t/ref?, v0 t/ref?]
    (doto #?(:clj (HashMap.) :cljs (>!hash-map))
          (#?(:clj .put :cljs assoc!) k0 v0)))
  ([k0 t/ref?, v0 t/ref?, k1 t/ref?, v1 t/ref?]
    (doto #?(:clj (HashMap.) :cljs (>!hash-map))
          (#?(:clj .put :cljs assoc!) k0 v0)
          (#?(:clj .put :cljs assoc!) k1 v1)))
  ([k0 t/ref?, v0 t/ref?, k1 t/ref?, v1 t/ref?, k2 t/ref?, v2 t/ref?]
    (doto #?(:clj (HashMap.) :cljs (>!hash-map))
          (#?(:clj .put :cljs assoc!) k0 v0)
          (#?(:clj .put :cljs assoc!) k1 v1)
          (#?(:clj .put :cljs assoc!) k2 v2)))
  ([k0 t/ref?, v0 t/ref?, k1 t/ref?, v1 t/ref?, k2 t/ref?, v2 t/ref?, k3 t/ref?, v3 t/ref?]
    (doto #?(:clj (HashMap.) :cljs (>!hash-map))
          (#?(:clj .put :cljs assoc!) k0 v0)
          (#?(:clj .put :cljs assoc!) k1 v1)
          (#?(:clj .put :cljs assoc!) k2 v2)
          (#?(:clj .put :cljs assoc!) k3 v3)))
  ([k0 t/ref?, v0 t/ref?, k1 t/ref?, v1 t/ref?, k2 t/ref?, v2 t/ref?, k3 t/ref?, v3 t/ref?
    k4 t/ref?, v4 t/ref?]
    (doto #?(:clj (HashMap.) :cljs (>!hash-map))
          (#?(:clj .put :cljs assoc!) k0 v0)
          (#?(:clj .put :cljs assoc!) k1 v1)
          (#?(:clj .put :cljs assoc!) k2 v2)
          (#?(:clj .put :cljs assoc!) k3 v3)
          (#?(:clj .put :cljs assoc!) k4 v4)))
  ([k0 t/ref?, v0 t/ref?, k1 t/ref?, v1 t/ref?, k2 t/ref?, v2 t/ref?, k3 t/ref?, v3 t/ref?
    k4 t/ref?, v4 t/ref?, k5 t/ref?, v5 t/ref?]
    (doto #?(:clj (HashMap.) :cljs (>!hash-map))
          (#?(:clj .put :cljs assoc!) k0 v0)
          (#?(:clj .put :cljs assoc!) k1 v1)
          (#?(:clj .put :cljs assoc!) k2 v2)
          (#?(:clj .put :cljs assoc!) k3 v3)
          (#?(:clj .put :cljs assoc!) k4 v4)
          (#?(:clj .put :cljs assoc!) k5 v5)))
  ;; TODO TYPED variadic support
#_([k0 t/ref?, v0 t/ref?, k1 t/ref?, v1 t/ref?, k2 t/ref?, v2 t/ref?, k3 t/ref?, v3 t/ref?
    k4 t/ref?, v4 t/ref?, k5 t/ref?, v5 t/ref?, k6 t/ref?, v6 t/ref?, & kvs]
    (reduce-pair
      (fn [^HashMap m k v] (doto m (#?(:clj .put :cljs assoc!) k v)))
      (doto #?(:clj (HashMap.) :cljs (>!hash-map))
            (#?(:clj .put :cljs assoc!) k0 v0)
            (#?(:clj .put :cljs assoc!) k1 v1)
            (#?(:clj .put :cljs assoc!) k2 v2)
            (#?(:clj .put :cljs assoc!) k3 v3)
            (#?(:clj .put :cljs assoc!) k4 v4)
            (#?(:clj .put :cljs assoc!) k5 v5)
            (#?(:clj .put :cljs assoc!) k6 v6))
      kvs)))

;; TODO generate these functions via macros
;; TODO this is incomplete
#?(:clj (t/defn >!hash-map|int->ref    > !hash-map|int->ref?    [] (Int2ReferenceOpenHashMap.)))
#?(:clj (t/defn >!hash-map|long->long  > !hash-map|long->long?  [] (Long2LongOpenHashMap.)))
#?(:clj (t/defn >!hash-map|long->ref   > !hash-map|long->ref?   [] (Long2ReferenceOpenHashMap.)))
#?(:clj (t/defn >!hash-map|double->ref > !hash-map|double->ref? [] (Double2ReferenceOpenHashMap.)))
#?(:clj (t/defn >!hash-map|ref->long   > !hash-map|ref->long?   [] (Reference2LongOpenHashMap.)))

;; ----- Unsorted Maps ----- ;; TODO Perhaps the concept of unsortedness is `(- map sorted?)`?

#?(:clj  (def   +unsorted-map|long->ref? (t/isa? clojure.data.int_map.PersistentIntMap)))

         (def   +unsorted-map? (t/or   +hash-map?   +array-map? +unsorted-map|long->ref?))
         (def  !+unsorted-map? (t/or  !+hash-map?  !+array-map?))
         (def ?!+unsorted-map? (t/or ?!+hash-map? ?!+array-map?))

(def !unsorted-map|boolean->boolean?
     (t/or !hash-map|boolean->boolean? !array-map|boolean->boolean?))
(def !unsorted-map|boolean->byte?
     (t/or !hash-map|boolean->byte?    !array-map|boolean->byte?))
(def !unsorted-map|boolean->short?
     (t/or !hash-map|boolean->short?   !array-map|boolean->short?))
(def !unsorted-map|boolean->char?
     (t/or !hash-map|boolean->char?    !array-map|boolean->char?))
(def !unsorted-map|boolean->int?
     (t/or !hash-map|boolean->int?     !array-map|boolean->int?))
(def !unsorted-map|boolean->long?
     (t/or !hash-map|boolean->long?    !array-map|boolean->long?))
(def !unsorted-map|boolean->float?
     (t/or !hash-map|boolean->float?   !array-map|boolean->float?))
(def !unsorted-map|boolean->double?
     (t/or !hash-map|boolean->double?  !array-map|boolean->double?))
(def !unsorted-map|boolean->ref?
     (t/or !hash-map|boolean->ref?     !array-map|boolean->ref?))

(def !unsorted-map|byte->boolean?
     (t/or !hash-map|byte->boolean?    !array-map|byte->boolean?))
(def !unsorted-map|byte->byte?
     (t/or !hash-map|byte->byte?       !array-map|byte->byte?))
(def !unsorted-map|byte->short?
     (t/or !hash-map|byte->short?      !array-map|byte->short?))
(def !unsorted-map|byte->char?
     (t/or !hash-map|byte->char?       !array-map|byte->char?))
(def !unsorted-map|byte->int?
     (t/or !hash-map|byte->int?        !array-map|byte->int?))
(def !unsorted-map|byte->long?
     (t/or !hash-map|byte->long?       !array-map|byte->long?))
(def !unsorted-map|byte->float?
     (t/or !hash-map|byte->float?      !array-map|byte->float?))
(def !unsorted-map|byte->double?
     (t/or !hash-map|byte->double?     !array-map|byte->double?))
(def !unsorted-map|byte->ref?
     (t/or !hash-map|byte->ref?        !array-map|byte->ref?))

(def !unsorted-map|short->boolean?
     (t/or !hash-map|short->boolean?   !array-map|short->boolean?))
(def !unsorted-map|short->byte?
     (t/or !hash-map|short->byte?      !array-map|short->byte?))
(def !unsorted-map|short->short?
     (t/or !hash-map|short->short?     !array-map|short->short?))
(def !unsorted-map|short->char?
     (t/or !hash-map|short->char?      !array-map|short->char?))
(def !unsorted-map|short->int?
     (t/or !hash-map|short->int?       !array-map|short->int?))
(def !unsorted-map|short->long?
     (t/or !hash-map|short->long?      !array-map|short->long?))
(def !unsorted-map|short->float?
     (t/or !hash-map|short->float?     !array-map|short->float?))
(def !unsorted-map|short->double?
     (t/or !hash-map|short->double?    !array-map|short->double?))
(def !unsorted-map|short->ref?
     (t/or !hash-map|short->ref?       !array-map|short->ref?))

(def !unsorted-map|char->boolean?
     (t/or !hash-map|char->boolean?    !array-map|char->boolean?))
(def !unsorted-map|char->byte?
     (t/or !hash-map|char->byte?       !array-map|char->byte?))
(def !unsorted-map|char->short?
     (t/or !hash-map|char->short?      !array-map|char->short?))
(def !unsorted-map|char->char?
     (t/or !hash-map|char->char?       !array-map|char->char?))
(def !unsorted-map|char->int?
     (t/or !hash-map|char->int?        !array-map|char->int?))
(def !unsorted-map|char->long?
     (t/or !hash-map|char->long?       !array-map|char->long?))
(def !unsorted-map|char->float?
     (t/or !hash-map|char->float?      !array-map|char->float?))
(def !unsorted-map|char->double?
     (t/or !hash-map|char->double?     !array-map|char->double?))
(def !unsorted-map|char->ref?
     (t/or !hash-map|char->ref?        !array-map|char->ref?))

(def !unsorted-map|int->boolean?
     (t/or !hash-map|int->boolean?     !array-map|int->boolean?))
(def !unsorted-map|int->byte?
     (t/or !hash-map|int->byte?        !array-map|int->byte?))
(def !unsorted-map|int->short?
     (t/or !hash-map|int->short?       !array-map|int->short?))
(def !unsorted-map|int->char?
     (t/or !hash-map|int->char?        !array-map|int->char?))
(def !unsorted-map|int->int?
     (t/or !hash-map|int->int?         !array-map|int->int?))
(def !unsorted-map|int->long?
     (t/or !hash-map|int->long?        !array-map|int->long?))
(def !unsorted-map|int->float?
     (t/or !hash-map|int->float?       !array-map|int->float?))
(def !unsorted-map|int->double?
     (t/or !hash-map|int->double?      !array-map|int->double?))
(def !unsorted-map|int->ref?
     (t/or !hash-map|int->ref?         !array-map|int->ref?))

(def !unsorted-map|long->boolean?
     (t/or !hash-map|long->boolean?     !array-map|long->boolean?))
(def !unsorted-map|long->byte?
     (t/or !hash-map|long->byte?        !array-map|long->byte?))
(def !unsorted-map|long->short?
     (t/or !hash-map|long->short?       !array-map|long->short?))
(def !unsorted-map|long->char?
     (t/or !hash-map|long->char?        !array-map|long->char?))
(def !unsorted-map|long->int?
     (t/or !hash-map|long->int?         !array-map|long->int?))
(def !unsorted-map|long->long?
     (t/or !hash-map|long->long?        !array-map|long->long?))
(def !unsorted-map|long->float?
     (t/or !hash-map|long->float?       !array-map|long->float?))
(def !unsorted-map|long->double?
     (t/or !hash-map|long->double?      !array-map|long->double?))
(def !unsorted-map|long->ref?
     (t/or !hash-map|long->ref?         !array-map|long->ref?))

(def !unsorted-map|float->boolean?
     (t/or !hash-map|float->boolean?    !array-map|float->boolean?))
(def !unsorted-map|float->byte?
     (t/or !hash-map|float->byte?       !array-map|float->byte?))
(def !unsorted-map|float->short?
     (t/or !hash-map|float->short?      !array-map|float->short?))
(def !unsorted-map|float->char?
     (t/or !hash-map|float->char?       !array-map|float->char?))
(def !unsorted-map|float->int?
     (t/or !hash-map|float->int?        !array-map|float->int?))
(def !unsorted-map|float->long?
     (t/or !hash-map|float->long?       !array-map|float->long?))
(def !unsorted-map|float->float?
     (t/or !hash-map|float->float?      !array-map|float->float?))
(def !unsorted-map|float->double?
     (t/or !hash-map|float->double?     !array-map|float->double?))
(def !unsorted-map|float->ref?
     (t/or !hash-map|float->ref?        !array-map|float->ref?))

(def !unsorted-map|double->boolean?
     (t/or !hash-map|double->boolean?   !array-map|double->boolean?))
(def !unsorted-map|double->byte?
     (t/or !hash-map|double->byte?      !array-map|double->byte?))
(def !unsorted-map|double->short?
     (t/or !hash-map|double->short?     !array-map|double->short?))
(def !unsorted-map|double->char?
     (t/or !hash-map|double->char?      !array-map|double->char?))
(def !unsorted-map|double->int?
     (t/or !hash-map|double->int?       !array-map|double->int?))
(def !unsorted-map|double->long?
     (t/or !hash-map|double->long?      !array-map|double->long?))
(def !unsorted-map|double->float?
     (t/or !hash-map|double->float?     !array-map|double->float?))
(def !unsorted-map|double->double?
     (t/or !hash-map|double->double?    !array-map|double->double?))
(def !unsorted-map|double->ref?
     (t/or !hash-map|double->ref?       !array-map|double->ref?))

(def !unsorted-map|ref->boolean?
     (t/or !hash-map|ref->boolean?      !array-map|ref->boolean?))
(def !unsorted-map|ref->byte?
     (t/or !hash-map|ref->byte?         !array-map|ref->byte?))
(def !unsorted-map|ref->short?
     (t/or !hash-map|ref->short?        !array-map|ref->short?))
(def !unsorted-map|ref->char?
     (t/or !hash-map|ref->char?         !array-map|ref->char?))
(def !unsorted-map|ref->int?
     (t/or !hash-map|ref->int?          !array-map|ref->int?))
(def !unsorted-map|ref->long?
     (t/or !hash-map|ref->long?         !array-map|ref->long?))
(def !unsorted-map|ref->float?
     (t/or !hash-map|ref->float?        !array-map|ref->float?))
(def !unsorted-map|ref->double?
     (t/or !hash-map|ref->double?       !array-map|ref->double?))
(def !unsorted-map|ref->ref?
     (t/or !identity-map|ref->ref? !hash-map|ref->ref? !array-map|ref->ref?))

         (def-preds|map|any        !unsorted-map)

         (def-preds|map|same-types !unsorted-map)

         (def  !unsorted-map? !unsorted-map|any?)

#?(:clj  (def !!unsorted-map? (t/or !!hash-map? !!array-map?)))
         (def   unsorted-map? (t/or ?!+unsorted-map? !unsorted-map? #?(:clj !!unsorted-map?)))

#?(:clj
(t/defn >unsorted-map|long->ref
  "Creates a persistent integer map that can only have non-negative integers as keys."
  > +unsorted-map|long->ref?
  ([] (clojure.data.int_map.PersistentIntMap. clojure.data.int_map.Nodes$Empty/EMPTY 0 nil))
  ;; TODO TYPED handle varargs
  ;; TODO TYPED `assoc`, `t/nneg-int?`
#_([k t/nneg-int? v t/ref?] (assoc (>unsorted-map|long->ref) k v))
  ;; TODO TYPED handle calling other typed fns
#_([kv & kvs] (apply assoc (>hash-map|long->ref) k v kvs))))

#?(:clj (defalias >map|long->ref >unsorted-map|long->ref))

;; ===== Ordered value-semantic maps ===== ;;

;; ----- Insertion-ordered ----- ;;

         (def    +insertion-ordered-map? (t/or (t/isa? linked.map.LinkedMap)
                                               ;; This is true, but we have replaced OrderedMap
                                               ;; with LinkedMap
                                       #_(:clj (t/isa? flatland.ordered.map.OrderedMap))))

         (def   !+insertion-ordered-map? t/none?
                                         ;; This is true, but we have replaced OrderedMap with
                                         ;; LinkedMap
                                       #_(t/isa? flatland.ordered.map.TransientOrderedMap))

         (def  ?!+insertion-ordered-map? (t/or +insertion-ordered-map? !+insertion-ordered-map?))

         (def    !insertion-ordered-map? (t/isa? #?(:clj java.util.LinkedHashMap :cljs LinkedMap)))

         ;; See https://github.com/ben-manes/concurrentlinkedhashmap (and links therefrom) for good implementation
#?(:clj  (def   !!insertion-ordered-map? t/none?))

         (def     insertion-ordered-map? (t/or ?!+insertion-ordered-map?
                                                 !insertion-ordered-map?
                                        #?(:clj !!insertion-ordered-map?)))

;; TODO generate this function via macro
(t/defn >!insertion-ordered-map
  "Creates a single-threaded, mutable insertion-ordered map.
   On the JVM, this is a `java.util.LinkedHashMap`.
   On JS, this is a `goog.structs.LinkedMap`."
  > !insertion-ordered-map?
  ([] #?(:clj (LinkedHashMap.) :cljs (LinkedMap.)))
  ([k0 t/ref?, v0 t/ref?]
    (doto #?(:clj (LinkedHashMap.) :cljs (LinkedMap.))
          (#?(:clj .put :cljs .add) k0 v0)))
  ([k0 t/ref?, v0 t/ref?, k1 t/ref?, v1 t/ref?]
    (doto #?(:clj (LinkedHashMap.) :cljs (LinkedMap.))
          (#?(:clj .put :cljs .add) k0 v0)
          (#?(:clj .put :cljs .add) k1 v1)))
  ([k0 t/ref?, v0 t/ref?, k1 t/ref?, v1 t/ref?, k2 t/ref?, v2 t/ref?]
    (doto #?(:clj (LinkedHashMap.) :cljs (LinkedMap.))
          (#?(:clj .put :cljs .add) k0 v0)
          (#?(:clj .put :cljs .add) k1 v1)
          (#?(:clj .put :cljs .add) k2 v2)))
  ([k0 t/ref?, v0 t/ref?, k1 t/ref?, v1 t/ref?, k2 t/ref?, v2 t/ref?, k3 t/ref?, v3 t/ref?]
    (doto #?(:clj (LinkedHashMap.) :cljs (LinkedMap.))
          (#?(:clj .put :cl .add) k0 v0)
          (#?(:clj .put :cljs .add) k1 v1)
          (#?(:clj .put :cljs .add) k2 v2)
          (#?(:clj .put :cljs .add) k3 v3)))
  ([k0 t/ref?, v0 t/ref?, k1 t/ref?, v1 t/ref?, k2 t/ref?, v2 t/ref?, k3 t/ref?, v3 t/ref?
    k4 t/ref?, v4 t/ref?]
    (doto #?(:clj (LinkedHashMap.) :cljs (LinkedMap.))
          (#?(:clj .put :cljs .add) k0 v0)
          (#?(:clj .put :cljs .add) k1 v1)
          (#?(:clj .put :cljs .add) k2 v2)
          (#?(:clj .put :cljs .add) k3 v3)
          (#?(:clj .put :cljs .add) k4 v4)))
  ([k0 t/ref?, v0 t/ref?, k1 t/ref?, v1 t/ref?, k2 t/ref?, v2 t/ref?, k3 t/ref?, v3 t/ref?
    k4 t/ref?, v4 t/ref?, k5 t/ref?, v5 t/ref?]
    (doto #?(:clj (LinkedHashMap.) :cljs (LinkedMap.))
          (#?(:clj .put :cljs .add) k0 v0)
          (#?(:clj .put :cljs .add) k1 v1)
          (#?(:clj .put :cljs .add) k2 v2)
          (#?(:clj .put :cljs .add) k3 v3)
          (#?(:clj .put :cljs .add) k4 v4)
          (#?(:clj .put :cljs .add) k5 v5)))
 ;; TODO TYPED `reduce-pair` and variadic
 #_([k0 t/ref?, v0 t/ref?, k1 t/ref?, v1 t/ref?, k2 t/ref?, v2 t/ref?, k3 t/ref?, v3 t/ref?
     k4 t/ref?, v4 t/ref?, k5 t/ref?, v5 t/ref?, k6 t/ref?, v6 & kvs]
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

;; Forward declaration
(def   +map?        (t/isa? #?(:clj  clojure.lang.IPersistentMap
                               :cljs cljs.core/IMap)))
;; Forward declaration
(def  !+map?        (t/isa? #?(:clj  clojure.lang.ITransientMap
                               :cljs cljs.core/ITransientMap)))

(def   +sorted-map? (t/and (t/isa? #?(:clj clojure.lang.Sorted :cljs cljs.core/ISorted))
                           +map?))
(def  !+sorted-map? (t/and (t/isa? #?(:clj clojure.lang.Sorted :cljs cljs.core/ISorted))
                           !+map?))
(def ?!+sorted-map? t/none? #_(t/or +sorted-map? !+sorted-map?)) ; TODO re-enable when `or` implemented properly

(def !sorted-map|boolean->boolean? t/none?)
(def !sorted-map|boolean->byte?    t/none?)
(def !sorted-map|boolean->char?    t/none?)
(def !sorted-map|boolean->short?   t/none?)
(def !sorted-map|boolean->int?     t/none?)
(def !sorted-map|boolean->long?    t/none?)
(def !sorted-map|boolean->float?   t/none?)
(def !sorted-map|boolean->double?  t/none?)
(def !sorted-map|boolean->ref?     t/none?)

(def !sorted-map|byte->boolean?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.bytes.Byte2BooleanSortedMap)          :cljs t/none?))
(def !sorted-map|byte->byte?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.bytes.Byte2ByteSortedMap)             :cljs t/none?))
(def !sorted-map|byte->short?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.bytes.Byte2ShortSortedMap)            :cljs t/none?))
(def !sorted-map|byte->char?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.bytes.Byte2CharSortedMap)             :cljs t/none?))
(def !sorted-map|byte->int?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.bytes.Byte2IntSortedMap)              :cljs t/none?))
(def !sorted-map|byte->long?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.bytes.Byte2LongSortedMap)             :cljs t/none?))
(def !sorted-map|byte->float?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.bytes.Byte2FloatSortedMap)            :cljs t/none?))
(def !sorted-map|byte->double?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.bytes.Byte2DoubleSortedMap)           :cljs t/none?))
(def !sorted-map|byte->ref?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.bytes.Byte2ReferenceSortedMap)        :cljs t/none?))

(def !sorted-map|short->boolean?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.shorts.Short2BooleanSortedMap)        :cljs t/none?))
(def !sorted-map|short->byte?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.shorts.Short2ByteSortedMap)           :cljs t/none?))
(def !sorted-map|short->short?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.shorts.Short2ShortSortedMap)          :cljs t/none?))
(def !sorted-map|short->char?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.shorts.Short2CharSortedMap)           :cljs t/none?))
(def !sorted-map|short->int?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.shorts.Short2IntSortedMap)            :cljs t/none?))
(def !sorted-map|short->long?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.shorts.Short2LongSortedMap)           :cljs t/none?))
(def !sorted-map|short->float?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.shorts.Short2FloatSortedMap)          :cljs t/none?))
(def !sorted-map|short->double?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.shorts.Short2DoubleSortedMap)         :cljs t/none?))
(def !sorted-map|short->ref?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.shorts.Short2ReferenceSortedMap)      :cljs t/none?))

(def !sorted-map|char->ref?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.chars.Char2ReferenceSortedMap)        :cljs t/none?))
(def !sorted-map|char->boolean?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.chars.Char2BooleanSortedMap)          :cljs t/none?))
(def !sorted-map|char->byte?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.chars.Char2ByteSortedMap)             :cljs t/none?))
(def !sorted-map|char->short?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.chars.Char2ShortSortedMap)            :cljs t/none?))
(def !sorted-map|char->char?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.chars.Char2CharSortedMap)             :cljs t/none?))
(def !sorted-map|char->int?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.chars.Char2IntSortedMap)              :cljs t/none?))
(def !sorted-map|char->long?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.chars.Char2LongSortedMap)             :cljs t/none?))
(def !sorted-map|char->float?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.chars.Char2FloatSortedMap)            :cljs t/none?))
(def !sorted-map|char->double?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.chars.Char2DoubleSortedMap)           :cljs t/none?))

(def !sorted-map|int->boolean?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.ints.Int2BooleanSortedMap)            :cljs t/none?))
(def !sorted-map|int->byte?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.ints.Int2ByteSortedMap)               :cljs t/none?))
(def !sorted-map|int->short?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.ints.Int2ShortSortedMap)              :cljs t/none?))
(def !sorted-map|int->char?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.ints.Int2CharSortedMap)               :cljs t/none?))
(def !sorted-map|int->int?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.ints.Int2IntSortedMap)                :cljs t/none?))
(def !sorted-map|int->long?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.ints.Int2LongSortedMap)               :cljs t/none?))
(def !sorted-map|int->float?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.ints.Int2FloatSortedMap)              :cljs t/none?))
(def !sorted-map|int->double?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.ints.Int2DoubleSortedMap)             :cljs t/none?))
(def !sorted-map|int->ref?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.ints.Int2ReferenceSortedMap)          :cljs t/none?))

(def !sorted-map|long->boolean?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.longs.Long2BooleanSortedMap)          :cljs t/none?))
(def !sorted-map|long->byte?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.longs.Long2ByteSortedMap)             :cljs t/none?))
(def !sorted-map|long->short?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.longs.Long2ShortSortedMap)            :cljs t/none?))
(def !sorted-map|long->char?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.longs.Long2CharSortedMap)             :cljs t/none?))
(def !sorted-map|long->int?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.longs.Long2IntSortedMap)              :cljs t/none?))
(def !sorted-map|long->long?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.longs.Long2LongSortedMap)             :cljs t/none?))
(def !sorted-map|long->float?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.longs.Long2FloatSortedMap)            :cljs t/none?))
(def !sorted-map|long->double?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.longs.Long2DoubleSortedMap)           :cljs t/none?))
(def !sorted-map|long->ref?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.longs.Long2ReferenceSortedMap)        :cljs t/none?))

(def !sorted-map|float->boolean?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.floats.Float2BooleanSortedMap)        :cljs t/none?))
(def !sorted-map|float->byte?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.floats.Float2ByteSortedMap)           :cljs t/none?))
(def !sorted-map|float->short?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.floats.Float2ShortSortedMap)          :cljs t/none?))
(def !sorted-map|float->char?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.floats.Float2CharSortedMap)           :cljs t/none?))
(def !sorted-map|float->int?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.floats.Float2IntSortedMap)            :cljs t/none?))
(def !sorted-map|float->long?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.floats.Float2LongSortedMap)           :cljs t/none?))
(def !sorted-map|float->float?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.floats.Float2FloatSortedMap)          :cljs t/none?))
(def !sorted-map|float->double?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.floats.Float2DoubleSortedMap)         :cljs t/none?))
(def !sorted-map|float->ref?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.floats.Float2ReferenceSortedMap)      :cljs t/none?))

(def !sorted-map|double->boolean?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.doubles.Double2BooleanSortedMap)      :cljs t/none?))
(def !sorted-map|double->byte?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.doubles.Double2ByteSortedMap)         :cljs t/none?))
(def !sorted-map|double->short?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.doubles.Double2ShortSortedMap)        :cljs t/none?))
(def !sorted-map|double->char?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.doubles.Double2CharSortedMap)         :cljs t/none?))
(def !sorted-map|double->int?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.doubles.Double2IntSortedMap)          :cljs t/none?))
(def !sorted-map|double->long?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.doubles.Double2LongSortedMap)         :cljs t/none?))
(def !sorted-map|double->float?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.doubles.Double2FloatSortedMap)        :cljs t/none?))
(def !sorted-map|double->double?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.doubles.Double2DoubleSortedMap)       :cljs t/none?))
(def !sorted-map|double->ref?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.doubles.Double2ReferenceSortedMap)    :cljs t/none?))

(def !sorted-map|ref->boolean?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.objects.Reference2BooleanSortedMap)   :cljs t/none?))
(def !sorted-map|ref->byte?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.objects.Reference2ByteSortedMap)      :cljs t/none?))
(def !sorted-map|ref->short?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.objects.Reference2ShortSortedMap)     :cljs t/none?))
(def !sorted-map|ref->char?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.objects.Reference2CharSortedMap)      :cljs t/none?))
(def !sorted-map|ref->int?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.objects.Reference2IntSortedMap)       :cljs t/none?))
(def !sorted-map|ref->long?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.objects.Reference2LongSortedMap)      :cljs t/none?))
(def !sorted-map|ref->float?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.objects.Reference2FloatSortedMap)     :cljs t/none?))
(def !sorted-map|ref->double?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.objects.Reference2DoubleSortedMap)    :cljs t/none?))
(def !sorted-map|ref->ref?
     (t/or #?@(:clj  [(t/isa? java.util.TreeMap)
                      (t/isa? it.unimi.dsi.fastutil.objects.Reference2ReferenceSortedMap)]
               :cljs [(t/isa? goog.structs.AvlTree)])))

(def-preds|map|any        !sorted-map)

(def-preds|map|same-types !sorted-map)

         (def  !sorted-map? !sorted-map|any?)

#?(:clj  (def !!sorted-map? (t/isa? java.util.concurrent.ConcurrentNavigableMap)))
         (def   sorted-map? (t/or ?!+sorted-map?
                        #?@(:clj [!!sorted-map?
                                  (t/isa? java.util.SortedMap)])
                                  !sorted-map?))

;; TODO generate this function via macro
;; TODO TYPED replaced `t/fn?` with a more specific `(t/fn [...])` named as e.g. `fn/comparator?`
;; TODO somehow the `TreeMap` constructor is not right, probably because expecting a `Comparator`
(t/defn >!sorted-map-by
  "Creates a single-threaded, mutable sorted map with the specified comparator.
   On the JVM, this is a `java.util.TreeMap`.
   On JS, this is a `goog.structs.AvlTree`."
  > !sorted-map|ref->ref?
  ([compf t/fn?] #?(:clj (TreeMap. compf) :cljs (AvlTree. compf)))
  ([compf t/fn?, k0 t/ref?, v0 t/ref?]
    (doto #?(:clj (TreeMap. compf) :cljs (AvlTree. compf))
          (#?(:clj .put :cljs .add) k0 v0)))
  ([compf t/fn?, k0 t/ref?, v0 t/ref?, k1 t/ref?, v1 t/ref?]
    (doto #?(:clj (TreeMap. compf) :cljs (AvlTree. compf))
          (#?(:clj .put :cljs .add) k0 v0)
          (#?(:clj .put :cljs .add) k1 v1)))
  ([compf t/fn?, k0 t/ref?, v0 t/ref?, k1 t/ref?, v1 t/ref?, k2 t/ref?, v2 t/ref?]
    (doto #?(:clj (TreeMap. compf) :cljs (AvlTree. compf))
          (#?(:clj .put :cljs .add) k0 v0)
          (#?(:clj .put :cljs .add) k1 v1)
          (#?(:clj .put :cljs .add) k2 v2)))
  ([compf t/fn?, k0 t/ref?, v0 t/ref?, k1 t/ref?, v1 t/ref?, k2 t/ref?, v2 t/ref?
    k3 t/ref?, v3 t/ref?]
    (doto #?(:clj (TreeMap. compf) :cljs (AvlTree. compf))
          (#?(:clj .put :cljs .add) k0 v0)
          (#?(:clj .put :cljs .add) k1 v1)
          (#?(:clj .put :cljs .add) k2 v2)
          (#?(:clj .put :cljs .add) k3 v3)))
  ([compf t/fn?, k0 t/ref?, v0 t/ref?, k1 t/ref?, v1 t/ref?, k2 t/ref?, v2 t/ref?
    k3 t/ref?, v3 t/ref?, k4 t/ref?, v4 t/ref?]
    (doto #?(:clj (TreeMap. compf) :cljs (AvlTree. compf))
          (#?(:clj .put :cljs .add) k0 v0)
          (#?(:clj .put :cljs .add) k1 v1)
          (#?(:clj .put :cljs .add) k2 v2)
          (#?(:clj .put :cljs .add) k3 v3)
          (#?(:clj .put :cljs .add) k4 v4)))
  ([compf t/fn?, k0 t/ref?, v0 t/ref?, k1 t/ref?, v1 t/ref?, k2 t/ref?, v2 t/ref?
    k3 t/ref?, v3 t/ref?, k4 t/ref?, v4 t/ref?, k5 t/ref?, v5 t/ref?]
    (doto #?(:clj (TreeMap. compf) :cljs (AvlTree. compf))
          (#?(:clj .put :cljs .add) k0 v0)
          (#?(:clj .put :cljs .add) k1 v1)
          (#?(:clj .put :cljs .add) k2 v2)
          (#?(:clj .put :cljs .add) k3 v3)
          (#?(:clj .put :cljs .add) k4 v4)
          (#?(:clj .put :cljs .add) k5 v5)))
  ;; TODO TYPED `reduce-pair`, variadic
#_([compf t/fn?, k0 t/ref?, v0 t/ref?, k1 t/ref?, v1 t/ref?, k2 t/ref?, v2 t/ref?
    k3 t/ref?, v3 t/ref?, k4 t/ref?, v4 t/ref?, k5 t/ref?, v5 t/ref?, k6 t/ref?, v6 t/ref? & kvs]
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

;; TODO generate this function via macro
;; TODO TYPED replace `compare` with typed version
(t/defn >!sorted-map
  "Creates a single-threaded, mutable sorted map.
   On the JVM, this is a `java.util.TreeMap`.
   On JS, this is a `goog.structs.AvlTree`."
  > !sorted-map|ref->ref?
  ([] (>!sorted-map-by compare))
  ([k0 t/ref?, v0 t/ref?]
    (>!sorted-map-by compare k0 v0))
  ([k0 t/ref?, v0 t/ref?, k1 t/ref?, v1 t/ref?]
    (>!sorted-map-by compare k0 v0 k1 v1))
  ([k0 t/ref?, v0 t/ref?, k1 t/ref?, v1 t/ref?, k2 t/ref?, v2 t/ref?]
    (>!sorted-map-by compare k0 v0 k1 v1 k2 v2))
  ([k0 t/ref?, v0 t/ref?, k1 t/ref?, v1 t/ref?, k2 t/ref?, v2 t/ref?, k3 t/ref?, v3 t/ref?]
    (>!sorted-map-by compare k0 v0 k1 v1 k2 v2 k3 v3))
  ([k0 t/ref?, v0 t/ref?, k1 t/ref?, v1 t/ref?, k2 t/ref?, v2 t/ref?, k3 t/ref?, v3 t/ref?
    k4 t/ref?, v4 t/ref?]
    (>!sorted-map-by compare k0 v0 k1 v1 k2 v2 k3 v3 k4 v4))
  ([k0 t/ref?, v0 t/ref?, k1 t/ref?, v1 t/ref?, k2 t/ref?, v2 t/ref?, k3 t/ref?, v3 t/ref?
    k4 t/ref?, v4 t/ref?, k5 t/ref?, v5 t/ref?]
    (>!sorted-map-by compare k0 v0 k1 v1 k2 v2 k3 v3 k4 v4 k5 v5))
  ;; TODO TYPED `apply`, `compare`, variadic
#_([k0 v0 k1 v1 k2 v2 k3 v3 k4 v4 k5 v5 k6 v6 & kvs]
    (apply >!sorted-map-by compare k0 v0 k1 v1 k2 v2 k3 v3 k4 v4 k5 v5 k6 v6 kvs)))

;; TODO TYPED `apply`, variadic
#_(t/defn !sorted-map-by-val > !sorted-map|ref->ref? [m & kvs]
  (apply !sorted-map-by (gen-compare-by-val m) kvs))

;; ----- General Maps ----- ;;

(defalias ut/+map|built-in?)

;; `+map?` and `!+map?` defined above
(def ?!+map? (t/or !+map? +map?))

(def !map|boolean->boolean? t/none?)
(def !map|boolean->byte?    t/none?)
(def !map|boolean->short?   t/none?)
(def !map|boolean->char?    t/none?)
(def !map|boolean->int?     t/none?)
(def !map|boolean->long?    t/none?)
(def !map|boolean->float?   t/none?)
(def !map|boolean->double?  t/none?)
(def !map|boolean->ref?     t/none?)

(def !map|byte->boolean?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.bytes.Byte2BooleanMap)        :cljs t/none?))
(def !map|byte->byte?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.bytes.Byte2ByteMap)           :cljs t/none?))
(def !map|byte->short?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.bytes.Byte2ShortMap)          :cljs t/none?))
(def !map|byte->char?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.bytes.Byte2CharMap)           :cljs t/none?))
(def !map|byte->int?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.bytes.Byte2IntMap)            :cljs t/none?))
(def !map|byte->long?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.bytes.Byte2LongMap)           :cljs t/none?))
(def !map|byte->float?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.bytes.Byte2FloatMap)          :cljs t/none?))
(def !map|byte->double?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.bytes.Byte2DoubleMap)         :cljs t/none?))
(def !map|byte->ref?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.bytes.Byte2ReferenceMap)      :cljs t/none?))

(def !map|short->boolean?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.shorts.Short2BooleanMap)      :cljs t/none?))
(def !map|short->byte?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.shorts.Short2ByteMap)         :cljs t/none?))
(def !map|short->short?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.shorts.Short2ShortMap)        :cljs t/none?))
(def !map|short->char?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.shorts.Short2CharMap)         :cljs t/none?))
(def !map|short->int?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.shorts.Short2IntMap)          :cljs t/none?))
(def !map|short->long?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.shorts.Short2LongMap)         :cljs t/none?))
(def !map|short->float?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.shorts.Short2FloatMap)        :cljs t/none?))
(def !map|short->double?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.shorts.Short2DoubleMap)       :cljs t/none?))
(def !map|short->ref?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.shorts.Short2ReferenceMap)    :cljs t/none?))

(def !map|char->ref?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.chars.Char2ReferenceMap)      :cljs t/none?))
(def !map|char->boolean?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.chars.Char2BooleanMap)        :cljs t/none?))
(def !map|char->byte?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.chars.Char2ByteMap)           :cljs t/none?))
(def !map|char->short?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.chars.Char2ShortMap)          :cljs t/none?))
(def !map|char->char?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.chars.Char2CharMap)           :cljs t/none?))
(def !map|char->int?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.chars.Char2IntMap)            :cljs t/none?))
(def !map|char->long?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.chars.Char2LongMap)           :cljs t/none?))
(def !map|char->float?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.chars.Char2FloatMap)          :cljs t/none?))
(def !map|char->double?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.chars.Char2DoubleMap)         :cljs t/none?))

(def !map|int->boolean?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.ints.Int2BooleanMap)          :cljs t/none?))
(def !map|int->byte?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.ints.Int2ByteMap)             :cljs t/none?))
(def !map|int->short?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.ints.Int2ShortMap)            :cljs t/none?))
(def !map|int->char?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.ints.Int2CharMap)             :cljs t/none?))
(def !map|int->int?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.ints.Int2IntMap)              :cljs t/none?))
(def !map|int->long?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.ints.Int2LongMap)             :cljs t/none?))
(def !map|int->float?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.ints.Int2FloatMap)            :cljs t/none?))
(def !map|int->double?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.ints.Int2DoubleMap)           :cljs t/none?))
(def !map|int->ref?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.ints.Int2ReferenceMap)        :cljs t/none?))

(def !map|long->boolean?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.longs.Long2BooleanMap)        :cljs t/none?))
(def !map|long->byte?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.longs.Long2ByteMap)           :cljs t/none?))
(def !map|long->short?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.longs.Long2ShortMap)          :cljs t/none?))
(def !map|long->char?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.longs.Long2CharMap)           :cljs t/none?))
(def !map|long->int?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.longs.Long2IntMap)            :cljs t/none?))
(def !map|long->long?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.longs.Long2LongMap)           :cljs t/none?))
(def !map|long->float?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.longs.Long2FloatMap)          :cljs t/none?))
(def !map|long->double?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.longs.Long2DoubleMap)         :cljs t/none?))
(def !map|long->ref?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.longs.Long2ReferenceMap)      :cljs t/none?))

(def !map|float->boolean?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.floats.Float2BooleanMap)      :cljs t/none?))
(def !map|float->byte?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.floats.Float2ByteMap)         :cljs t/none?))
(def !map|float->short?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.floats.Float2ShortMap)        :cljs t/none?))
(def !map|float->char?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.floats.Float2CharMap)         :cljs t/none?))
(def !map|float->int?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.floats.Float2IntMap)          :cljs t/none?))
(def !map|float->long?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.floats.Float2LongMap)         :cljs t/none?))
(def !map|float->float?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.floats.Float2FloatMap)        :cljs t/none?))
(def !map|float->double?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.floats.Float2DoubleMap)       :cljs t/none?))
(def !map|float->ref?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.floats.Float2ReferenceMap)    :cljs t/none?))

(def !map|double->boolean?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.doubles.Double2BooleanMap)    :cljs t/none?))
(def !map|double->byte?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.doubles.Double2ByteMap)       :cljs t/none?))
(def !map|double->short?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.doubles.Double2ShortMap)      :cljs t/none?))
(def !map|double->char?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.doubles.Double2CharMap)       :cljs t/none?))
(def !map|double->int?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.doubles.Double2IntMap)        :cljs t/none?))
(def !map|double->long?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.doubles.Double2LongMap)       :cljs t/none?))
(def !map|double->float?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.doubles.Double2FloatMap)      :cljs t/none?))
(def !map|double->double?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.doubles.Double2DoubleMap)     :cljs t/none?))
(def !map|double->ref?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.doubles.Double2ReferenceMap)  :cljs t/none?))

(def !map|ref->boolean?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.objects.Reference2BooleanMap) :cljs t/none?))
(def !map|ref->byte?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.objects.Reference2ByteMap)    :cljs t/none?))
(def !map|ref->short?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.objects.Reference2ShortMap)   :cljs t/none?))
(def !map|ref->char?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.objects.Reference2CharMap)    :cljs t/none?))
(def !map|ref->int?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.objects.Reference2IntMap)     :cljs t/none?))
(def !map|ref->long?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.objects.Reference2LongMap)    :cljs t/none?))
(def !map|ref->float?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.objects.Reference2FloatMap)   :cljs t/none?))
(def !map|ref->double?
     #?(:clj (t/isa? it.unimi.dsi.fastutil.objects.Reference2DoubleMap)  :cljs t/none?))

(def !map|ref->ref?
     (t/or #?@(:clj  [;; perhaps just `(- !map? <primitive-possibilities>)` ?
                      !unsorted-map|ref->ref?
                      !sorted-map|ref->ref?
                      (t/isa? it.unimi.dsi.fastutil.objects.Reference2ReferenceMap)]
               :cljs [(t/isa? goog.structs.AvlTree)])))

(def-preds|map|any        !map)

(def-preds|map|same-types !map)

         (def  !map? !map|any?)

#?(:clj  (def !!map? (t/or !!unsorted-map? !!sorted-map?)))

         (uvar/def map?
           "A `map?` is in some sense anything that satisfies `dc/lookup?` — i.e., a collection
            maintaining a one-to-one mapping from keys to values. However, while we define `map?`
            as being effectively `(t/- (t/and associative? lookup?) indexed?)`, in practice we
            limit to an enumerated set of concrete types."
           (t/or ?!+map? !map? #?@(:clj [!!map? (t/isa? java.util.Map)])))






#_(defaliases umap
  map-entry-seq
  ordered-map om #?(:clj !ordered-map) #?(:clj kw-omap)
  sorted-map      sorted-map-by sorted-map-by-val
  sorted-rank-map sorted-rank-map-by
  nearest rank-of subrange split-key split-at
  merge #?(:clj pmerge)
  !hash-map
  #?@(:clj [!hash-map|int->ref    !hash-map|int->object
            !hash-map|long->long  !hash-map|long
            !hash-map|long->ref   !hash-map|long->object
            !hash-map|double->ref !hash-map|double->object
            !hash-map|ref->long   !hash-map|object->long])
  bubble-max-key difference-by-key union-by-key intersection-by-key)
