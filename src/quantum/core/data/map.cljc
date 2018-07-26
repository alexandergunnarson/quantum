(ns
  ^{:doc "Useful map functions. |map-entry|, a better merge, sorted-maps, etc."
    :attribution "alexandergunnarson"}
  quantum.core.data.map
  (:refer-clojure :exclude
    [split-at, merge, sorted-map sorted-map-by])
  (:require
    #?(:clj [clojure.data.int-map])
            ;; TODO TYPED
          #_[quantum.core.reducers :as r
              :refer [reduce-pair]]
            [quantum.untyped.core.data.map :as u]
            [quantum.untyped.core.type     :as t]
            [quantum.untyped.core.type.defnt
              :refer [defnt]]
            [quantum.untyped.core.vars
              :refer [defalias]])
  (:import
#?@(:clj  [[java.util HashMap IdentityHashMap LinkedHashMap TreeMap]
           [it.unimi.dsi.fastutil.ints    Int2ReferenceOpenHashMap]
           [it.unimi.dsi.fastutil.longs   Long2LongOpenHashMap
                                          Long2ReferenceOpenHashMap]
           [it.unimi.dsi.fastutil.doubles Double2ReferenceOpenHashMap]
           [it.unimi.dsi.fastutil.objects Reference2LongOpenHashMap]]
    :cljs [[goog.structs AvlTree LinkedMap]])))

;; ===== Map entries ===== ;;

(defnt >map-entry
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
  [k _, v _ > t/+map-entry?]
  #?(:clj  (clojure.lang.MapEntry. k v)
     :cljs (cljs.core.MapEntry. k v nil)))

;; ===== Unordered identity-semantic maps ===== ;;

;; TODO generate this via macro?
(defnt >!identity-map
  "Creates a single-threaded, mutable identity map.
   On the JVM, this is a `java.util.IdentityHashMap`.
   On JS, this is a `js/Map` (ECMAScript 6 Map)."
  > t/!identity-map?
  ([] #?(:clj (IdentityHashMap.) :cljs (js/Map.)))
  ([k0 (t/ref t/any?), v0 (t/ref t/any?)]
    (doto #?(:clj (IdentityHashMap.) :cljs (js/Map.))
          (#?(:clj .put :cljs .set) k0 v0)))
  ([k0 (t/ref t/any?), v0 (t/ref t/any?), k1 (t/ref t/any?), v1 (t/ref t/any?)]
    (doto #?(:clj (IdentityHashMap.) :cljs (js/Map.))
          (#?(:clj .put :cljs .set) k0 v0)
          (#?(:clj .put :cljs .set) k1 v1)))
  ([k0 (t/ref t/any?), v0 (t/ref t/any?), k1 (t/ref t/any?), v1 (t/ref t/any?)
    k2 (t/ref t/any?), v2 (t/ref t/any?)]
    (doto #?(:clj (IdentityHashMap.) :cljs (js/Map.))
          (#?(:clj .put :cljs .set) k0 v0)
          (#?(:clj .put :cljs .set) k1 v1)
          (#?(:clj .put :cljs .set) k2 v2)))
  ([k0 (t/ref t/any?), v0 (t/ref t/any?), k1 (t/ref t/any?), v1 (t/ref t/any?)
    k2 (t/ref t/any?), v2 (t/ref t/any?), k3 (t/ref t/any?), v3 (t/ref t/any?)]
    (doto #?(:clj (IdentityHashMap.) :cljs (js/Map.))
          (#?(:clj .put :cljs .set) k0 v0)
          (#?(:clj .put :cljs .set) k1 v1)
          (#?(:clj .put :cljs .set) k2 v2)
          (#?(:clj .put :cljs .set) k3 v3)))
  ([k0 (t/ref t/any?), v0 (t/ref t/any?), k1 (t/ref t/any?), v1 (t/ref t/any?)
    k2 (t/ref t/any?), v2 (t/ref t/any?), k3 (t/ref t/any?), v3 (t/ref t/any?)
    k4 (t/ref t/any?), v4 (t/ref t/any?)]
    (doto #?(:clj (IdentityHashMap.) :cljs (js/Map.))
          (#?(:clj .put :cljs .set) k0 v0)
          (#?(:clj .put :cljs .set) k1 v1)
          (#?(:clj .put :cljs .set) k2 v2)
          (#?(:clj .put :cljs .set) k3 v3)
          (#?(:clj .put :cljs .set) k4 v4)))
  ([k0 (t/ref t/any?), v0 (t/ref t/any?), k1 (t/ref t/any?), v1 (t/ref t/any?)
    k2 (t/ref t/any?), v2 (t/ref t/any?), k3 (t/ref t/any?), v3 (t/ref t/any?)
    k4 (t/ref t/any?), v4 (t/ref t/any?), k5 (t/ref t/any?), v5 (t/ref t/any?)]
    (doto #?(:clj (IdentityHashMap.) :cljs (js/Map.))
          (#?(:clj .put :cljs .set) k0 v0)
          (#?(:clj .put :cljs .set) k1 v1)
          (#?(:clj .put :cljs .set) k2 v2)
          (#?(:clj .put :cljs .set) k3 v3)
          (#?(:clj .put :cljs .set) k4 v4)
          (#?(:clj .put :cljs .set) k5 v5)))
  ;; TODO TYPED handle varargs
#_([k0 (t/ref t/any?), v0 (t/ref t/any?), k1 (t/ref t/any?), v1 (t/ref t/any?)
    k2 (t/ref t/any?), v2 (t/ref t/any?), k3 (t/ref t/any?), v3 (t/ref t/any?)
    k4 (t/ref t/any?), v4 (t/ref t/any?), k5 (t/ref t/any?), v5 (t/ref t/any?)
    k6 (t/ref t/any?), v6 (t/ref t/any?) & kvs _]
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

(defnt >array-map
  "Creates a persistent array map. If any keys are equal, they are handled as if by repeated
   applications of `assoc`."
  > t/+array-map?
  ([] (. clojure.lang.PersistentArrayMap EMPTY))
  ;; TODO TYPED handle varargs
#_([& kvs]
     (clojure.lang.PersistentArrayMap/createAsIfByAssoc (to-array kvs))))

;; ----- Hash maps ----- ;;

(defnt >hash-map
  "Creates a persistent hash map. If any keys are equal, they are handled as if by repeated
   applications of `assoc`."
  > t/+array-map?
  ([] clojure.lang.PersistentArrayMap/EMPTY)
  ;; TODO TYPED handle varargs
#_([& keyvals]
     (clojure.lang.PersistentHashMap/create kvs)))

(def +hash-map|long->ref? (t/isa? clojure.data.int_map.PersistentIntMap))

#?(:clj
(defnt >hash-map|long->ref
  "Creates a persistent integer map that can only have non-negative integers as keys."
  > +hash-map|long->ref?
  ([] (clojure.data.int_map.PersistentIntMap. clojure.data.int_map.Nodes$Empty/EMPTY 0 nil))
  ;; TODO TYPED handle varargs
  ([k t/nneg-int? v (t/ref t/any?)] (assoc (>hash-map|long->ref) k v))
  ;; TODO TYPED handle calling other typed fns
#_([kv & kvs] (apply assoc (>hash-map|long->ref) k v kvs))))

#?(:clj (defalias int-map hash-map|long->ref))

; `(apply hash-map pairs)` <~> `lodash/fromPairs`
(defaliases u
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
