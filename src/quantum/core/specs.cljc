(ns quantum.core.specs
  (:require
    [clojure.core            :as core]
    [quantum.core.specs.core :as s]))

;;; =====>>> PRIMITIVES <<<===== ;;;

#?(:clj (def primitive?         (s/or boolean byte char short int long float double)))

#?(:clj (def primitive-boolean? boolean))
#?(:clj (def boxed-boolean?     Boolean))
        (def boolean?           (s/or #?@(:clj  [unboxed-boolean? boxed-boolean?]
                                          :cljs [js/Boolean])))

#?(:clj (def primitive-byte?    byte))
#?(:clj (def boxed-byte?        Byte))
#?(:clj (def byte?              (s/or unboxed-byte? boxed-byte?)))

#?(:clj (def primitive-char?    char))
#?(:clj (def boxed-char?        Character))
#?(:clj (def char?              (s/or unboxed-char? boxed-char?)))

#?(:clj (def primitive-short?   short))
#?(:clj (def boxed-short?       Short))
#?(:clj (def short?             (s/or unboxed-short? boxed-short?)))

#?(:clj (def primitive-int?     int))
#?(:clj (def boxed-int?         Integer))
#?(:clj (def int?               (s/or unboxed-int? boxed-int?)))

#?(:clj (def primitive-long?    long))
#?(:clj (def boxed-long?        Long)) ; TODO CLJS may have this with goog.Long ?
#?(:clj (def long?              (s/or unboxed-long? boxed-long?)))

#?(:clj (def primitive-float?   float))
#?(:clj (def boxed-float?       Float))
#?(:clj (def float?             (s/or unboxed-float? boxed-float?)))

#?(:clj (def primitive-double?  double))
#?(:clj (def boxed-double?      (s/or Double)))
        (def double?            (s/or #?@(:clj  [unboxed-double? boxed-double?]
                                          :cljs [js/Number])))

;;; =====>>> GENERAL <<<===== ;;;

        (def nil?               (s/== nil))

        (def object?            (s/or #?(:clj Object :cljs js/Object)))

        (def any?               (s/or nil? primitive? object?
                                      #?@(:cljs [js/Boolean js/Number js/String js/Symbol])))

;;; =====>>> NUMBERS <<<===== ;;;

        (def bigint?            (s/or #?@(:clj  [clojure.lang.BigInt java.math.BigInteger]
                                          :cljs [com.gfredericks.goog.math.Integer])))

        (def integer?           (s/or byte? short? int? long? bigint?)))

#?(:clj (def bigdec?            (s/or java.math.BigDecimal))) ; TODO CLJS may have this

        (def decimal?           (s/or float? double? bigdec?))

        (def ratio?             (s/or #?(:clj  clojure.lang.Ratio
                                         :cljs quantum.core.numeric.types.Ratio))) ; TODO add this CLJS entry to the predicate after the fact

#?(:clj (def primitive-number?  (s/or short int long float double)))

        (def number?            (s/or #?@(:clj  [primitive-number? Number]
                                          :cljs [integer? decimal? ratio?])))

; ----- NUMBER LIKENESSES ----- ;

        (def integer-value?     (s/or integer? (s/and decimal? decimal-is-integer-value?)))
        (def int-like?          (s/and integer-value? (s/range-of int)))

;;; =====>>> COLLECTIONS <<<===== ;;;

; ----- TUPLES ----- ;

        (def tuple?             (s/or Tuple)) ; clojure.lang.Tuple was discontinued; we won't support it for now
#?(:clj (def map-entry?         (s/or java.util.Map$Entry)))

;; ===== SEQUENCES ===== ;; Sequential (generally not efficient Lookup / RandomAccess)

        (def cons?              (s/or #?(:clj  clojure.lang.Cons
                                         :cljs cljs.core/Cons)))
        (def lseq?              (s/or #?(:clj  clojure.lang.LazySeq
                                         :cljs cljs.core/LazySeq)))
        (def misc-seq?          (s/or
                                  #?@(:clj  [clojure.lang.APersistentMap$ValSeq
                                             clojure.lang.APersistentMap$KeySeq
                                             clojure.lang.PersistentVector$ChunkedSeq
                                             clojure.lang.IndexedSeq]
                                      :cljs [cljs.core/ValSeq
                                             cljs.core/KeySeq
                                             cljs.core/IndexedSeq
                                             cljs.core/ChunkedSeq])))

        (def non-list-seq?      (s/or cons? lseq? misc-seq?))

; ----- LISTS ----- ; Not extremely different from Sequences ; TODO clean this up

        (def cdlist?            nil
                                #_'{:clj  #{clojure.data.finger_tree.CountedDoubleList
                                            quantum.core.data.finger_tree.CountedDoubleList}
                                    :cljs #{quantum.core.data.finger-tree/CountedDoubleList}})
        (def dlist?             nil
                                #_'{:clj  #{clojure.data.finger_tree.CountedDoubleList
                                            quantum.core.data.finger_tree.CountedDoubleList}
                                   :cljs #{quantum.core.data.finger-tree/CountedDoubleList}})
        (def +list?             (s/or #?@(:clj  [clojure.lang.IPersistentList]
                                          :cljs [cljs.core/List cljs.core/EmptyList])))

#?(:clj (def !list?             (s/or java.util.LinkedList)))
        (def list?              (s/or #?(:clj java.util.List :cljs +list?)))

; ----- GENERIC ----- ;

        (def seq?               (s/or #?(:clj  clojure.lang.ISeq
                                         :cljs non-list-seq? list?)))

;; ===== MAPS ===== ;; Associative

; ----- Generators ----- ;

(defn <fastutil-package [x]
  (if (= x 'ref) "objects" (str (name x) "s")))

(defn <fastutil-long-form [x]
  (if (= x 'ref) "Reference" (-> x name str/capitalize)))

(defn !map-type:gen [from to suffixes]
  (let [fastutil-class-name-base
          (str "it.unimi.dsi.fastutil." (<fastutil-package from)
               "." (<fastutil-long-form from) "2" (<fastutil-long-form to))]
   `{:clj '~(->> suffixes
                 (map #(symbol (str fastutil-class-name-base %)))
                 set)}))

(defn !map-types*:gen [prefix genf ref->ref]
  (let [?prefix      (when prefix (str prefix "-"))
        base-types   (conj (keys primitive-type-meta) 'ref)
        type-combos  (->> base-types
                          (<- combo/selections 2)
                          (remove (fn-> first (= 'boolean))))
        gen-same-sym (fn [t]       (symbol (str "!" ?prefix "map:" t "-types")))
        gen-map-sym  (fn [from to] (symbol (str "!" ?prefix "map:" from "->" to "-types")))
        any-*-defs
          (->> base-types
               (map (fn [t]
                      (let [any-key-sym (symbol (str "!" ?prefix "map:" "any" "->" t     "-types"))
                            any-val-sym (symbol (str "!" ?prefix "map:" t     "->" "any" "-types"))
                            cond-union:any
                             (fn [pred] (list* 'cond-union
                                          (->> type-combos (filter (fn-> pred (= t))) (map (partial apply gen-map-sym)))))]
                        `(do (def ~any-key-sym ~(cond-union:any second))
                             (def ~any-val-sym ~(cond-union:any first )))))))
        sym=>code
         (->> type-combos
              (map (fn [[from to]]
                     (let [body (genf from to)
                           sym  (gen-map-sym from to)]
                       [sym `(do (def ~sym ~body)
                                 ~(when (= from to)
                                    `(def ~(gen-same-sym from) ~sym)))])))
              (into (map/om)))]
    (concat (vals sym=>code)
            [(let [ref->ref-sym (gen-map-sym 'ref 'ref)]
               `(do (def ~ref->ref-sym ~ref->ref)
                    (def ~(gen-same-sym 'ref) ~ref->ref-sym)))]
            any-*-defs
            [`(def ~(symbol (str "!" ?prefix "map-types")) (cond-union ~@(keys sym=>code)))])))

#?(:clj
(defmacro !hash-map-types:gen [ref->ref]
  `(do ~@(!map-types*:gen "hash"
           (fn [from to] (!map-type:gen from to #{"OpenHashMap" "OpenCustomHashMap"}))
           ref->ref))))

#?(:clj
(defmacro !unsorted-map-types:gen []
  `(do ~@(!map-types*:gen "unsorted"
           (fn [from to] (symbol (str "!hash-map:" from "->" to "-types")))
           '!hash-map:ref-types))))

#?(:clj
(defmacro !sorted-map-types:gen [ref->ref]
  `(do ~@(!map-types*:gen "sorted" (fn [from to] {}) ref->ref))))

#?(:clj
(defmacro !map-types:gen []
  ; technically also `object` for CLJS
  `(do ~@(!map-types*:gen nil (fn [from to] (!map-type:gen from to #{"Map"}))
           '(cond-union !unsorted-map:ref-types !sorted-map:ref-types)))))

(defn !set-type:gen [t suffixes]
  (let [fastutil-class-name-base
          (str "it.unimi.dsi.fastutil." (<fastutil-package t) "." (<fastutil-long-form t))]
   `{:clj '~(->> suffixes
                 (map #(symbol (str fastutil-class-name-base %)))
                 set)}))

(defn !set-types*:gen [prefix genf ref-val]
  (let [?prefix (when prefix (str prefix "-"))
        sym=>code
         (->> (conj (keys primitive-type-meta) 'ref)
              (remove (fn= 'boolean))
              (map (fn [t]
                     (let [body (genf t)
                           sym  (symbol (str "!" ?prefix "set:" t "-types"))]
                       [sym `(def ~sym ~body)])))
              (into (map/om)))]
    (concat (vals sym=>code)
            [`(def ~(symbol (str "!" ?prefix "set:ref-types")) ~ref-val)
             `(def ~(symbol (str "!" ?prefix "set-types")) (cond-union ~@(keys sym=>code)))])))

#?(:clj
(defmacro !hash-set-types:gen [ref-val]
  `(do ~@(!set-types*:gen "hash"
           (fn [t] (!set-type:gen t #{"OpenHashSet" "OpenCustomHashSet"}))
           ref-val))))

#?(:clj
(defmacro !unsorted-set-types:gen []
  `(do ~@(!set-types*:gen "unsorted"
           (fn [t] (symbol (str "!hash-set:" t "-types")))
           '!hash-set:ref-types))))

#?(:clj
(defmacro !sorted-set-types:gen [ref-val]
  `(do ~@(!set-types*:gen "sorted" (fn [t] {}) ref-val))))

#?(:clj
(defmacro !set-types:gen []
  `(do ~@(!set-types*:gen nil (fn [t] (!set-type:gen t #{"Set"}))
           '(cond-union !unsorted-set:ref-types !sorted-set:ref-types)))))

; ----- ;

        (def +array-map?        (s/or #?(:clj  clojure.lang.PersistentArrayMap
                                         :cljs cljs.core/PersistentArrayMap)))
        (def !+array-map?       (s/or #?(:clj  clojure.lang.PersistentArrayMap$TransientArrayMap
                                         :cljs cljs.core/TransientArrayMap)))
        (def ?!+array-map?      (s/or +array-map? !+array-map?))
        (def !array-map?        nil)
        (def !!array-map?       nil)
        (def array-map?         (s/or ?!+array-map?
                                      !array-map?
                                      !!array-map?))

        (def +hash-map?         (s/or #?(:clj  clojure.lang.PersistentHashMap
                                         :cljs cljs.core/PersistentHashMap)))
        (def !+hash-map?        (s/or #?(:clj  clojure.lang.PersistentHashMap$TransientHashMap
                                         :cljs cljs.core/TransientHashMap)))
        (def ?!+hash-map?       (s/or +hash-map? !+hash-map?))

(!hash-map-types:gen
  '{:clj  #{java.util.HashMap
            java.util.IdentityHashMap
            it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap
            it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenCustomHashMap}
    :cljs #{goog.structs.Map}})

(def !!hash-map-types             '{:clj  #{java.util.concurrent.ConcurrentHashMap}})
(def hash-map-types                (cond-union ?!+hash-map-types
                                               !hash-map-types !!hash-map-types))

(def +unsorted-map-types           (cond-union   +hash-map-types   +array-map-types))
(def !+unsorted-map-types          (cond-union  !+hash-map-types  !+array-map-types))
(def ?!+unsorted-map-types         (cond-union ?!+hash-map-types ?!+array-map-types))

(!unsorted-map-types:gen)

(def !!unsorted-map-types          !!hash-map-types)
(def unsorted-map-types            (cond-union ?!+unsorted-map-types
                                               !unsorted-map-types !!unsorted-map-types))

(def +sorted-map-types            '{:clj  #{clojure.lang.PersistentTreeMap}
                                    :cljs #{cljs.core/PersistentTreeMap   }})
(def !+sorted-map-types            {})
(def ?!+sorted-map-types           (cond-union +sorted-map-types !+sorted-map-types))

(!sorted-map-types:gen
 '{:clj  #{java.util.TreeMap}
   :cljs #{goog.structs.AvlTree}})

(def !!sorted-map-types            {})
(def sorted-map-types              {:clj  (set/union (:clj ?!+sorted-map-types)
                                                     '#{java.util.SortedMap})
                                    :cljs (set/union (:cljs +sorted-map-types)
                                                     (:cljs !sorted-map-types))})

(def !insertion-ordered-map-types    {:clj '#{java.util.LinkedHashMap}})
(def +insertion-ordered-map-types    {:clj '#{flatland.ordered.map.OrderedMap}})
(def insertion-ordered-map-types     (cond-union !insertion-ordered-map-types
                                                 +insertion-ordered-map-types))

(def !+map-types                   {:clj  '#{clojure.lang.ITransientMap}
                                    :cljs (set/union (:cljs !+unsorted-map-types))})
(def +map-types                    {:clj  '#{clojure.lang.IPersistentMap}
                                    :cljs (set/union (:cljs +unsorted-map-types)
                                                     (:cljs +sorted-map-types))})
(def ?!+map-types                  (cond-union !+map-types +map-types))

(!map-types:gen)

(def !!map-types                   (cond-union !!unsorted-map-types !!sorted-map-types))
(def map-types                     {:clj  (set/union (:clj !+map-types)
                                            '#{; TODO IPersistentMap as well, yes, but all persistent Clojure maps implement java.util.Map
                                               ; TODO add typed maps into this definition once lazy compilation is in place
                                               java.util.Map})
                                    :cljs (set/union (:cljs ?!+map-types)
                                                     (:cljs !map-types)
                                                     (:cljs !!map-types))})

; ===== SETS ===== ; Associative; A special type of Map whose keys and vals are identical

(def +hash-set-types            '{:clj  #{clojure.lang.PersistentHashSet}
                                  :cljs #{cljs.core/PersistentHashSet}})
(def !+hash-set-types           '{:clj  #{clojure.lang.PersistentHashSet$TransientHashSet}
                                  :cljs #{cljs.core/TransientHashSet}})
(def ?!+hash-set-types           (cond-union !+hash-set-types +hash-set-types))

(!hash-set-types:gen
  '{:clj  #{java.util.HashSet
            #_java.util.IdentityHashSet}
    :cljs #{goog.structs.Set}})

(def !!hash-set-types            {}) ; technically you can make something from ConcurrentHashMap but...
(def hash-set-types              (cond-union ?!+hash-set-types
                                   !hash-set-types !!hash-set-types))

(def +unsorted-set-types        +hash-set-types)
(def !+unsorted-set-types      !+hash-set-types)
(def ?!+unsorted-set-types    ?!+hash-set-types)

(!unsorted-set-types:gen)

(def !!unsorted-set-types      !!hash-set-types)
(def unsorted-set-types          hash-set-types)

(def +sorted-set-types          '{:clj  #{clojure.lang.PersistentTreeSet}
                                  :cljs #{cljs.core/PersistentTreeSet   }})
(def !+sorted-set-types          {})
(def ?!+sorted-set-types         (cond-union +sorted-set-types !+sorted-set-types))

(!sorted-set-types:gen
  '{:clj #{java.util.TreeSet}}) ; TODO CLJS can have via AVLTree with same KVs

(def !!sorted-set-types          {})
(def sorted-set-types            {:clj  (set/union (:clj +sorted-set-types)
                                                   '#{java.util.SortedSet})
                                  :cljs (set/union (:cljs +sorted-set-types)
                                                   (:cljs !sorted-set-types)
                                                   (:cljs !!sorted-set-types))})

(def !+set-types                 {:clj  '#{clojure.lang.ITransientSet}
                                  :cljs (set/union (:cljs !+unsorted-set-types))})
(def +set-types                  {:clj '#{clojure.lang.IPersistentSet}
                                  :cljs (set/union (:cljs +unsorted-set-types)
                                                   (:cljs +sorted-set-types))})
(def ?!+set-types                (cond-union !+set-types +set-types))

(!set-types:gen)

(def !set-types:int              {:clj '#{it.unimi.dsi.fastutil.ints.IntSet}})
(def !set-types:long             {:clj '#{it.unimi.dsi.fastutil.longs.LongSet}})
(def !set-types:double           {:clj '#{it.unimi.dsi.fastutil.doubles.DoubleSet}})
(def !set-types:ref              (cond-union !unsorted-set:ref-types
                                             !sorted-set:ref-types))
(def !set-types                  (cond-union !unsorted-set-types
                                             !sorted-set-types))
(def !!set-types                 (cond-union !!unsorted-set-types !!sorted-set-types))
(def set-types                   {:clj  (set/union (:clj !+set-types)
                                          '#{; TODO IPersistentSet as well, yes, but all persistent Clojure sets implement java.util.Set
                                             java.util.Set})
                                  :cljs (set/union (:cljs ?!+set-types)
                                                   (:cljs !set-types)
                                                   (:cljs !!set-types))})

; ===== ARRAYS ===== ; Sequential, Associative (specifically, whose keys are sequential,
                     ; dense integer values), not extensible
; TODO do e.g. {:clj {0 {:byte ...}}}
(def array-1d-types*      `{:clj  {:byte          (type (byte-array    0)      )
                                   :char          (type (char-array    "")     )
                                   :short         (type (short-array   0)      )
                                   :long          (type (long-array    0)      )
                                   :float         (type (float-array   0)      )
                                   :int           (type (int-array     0)      )
                                   :double        (type (double-array  0.0)    )
                                   :boolean       (type (boolean-array [false]))
                                   :object        (type (object-array  [])     )}
                            :cljs {:byte          js/Int8Array
                                   :ubyte         js/Uint8Array
                                   :ubyte-clamped js/Uint8ClampedArray
                                   :char          js/Uint16Array ; kind of
                                   :ushort        js/Uint16Array
                                   :short         js/Int16Array
                                   :int           js/Int32Array
                                   :uint          js/Uint32Array
                                   :float         js/Float32Array
                                   :double        js/Float64Array
                                   :object        (type (cljs.core/array))}})
(def undistinguished-array-1d-types (->> array-1d-types* (map (fn [[k v]] [k (-> v vals set)])) (into {})))
(def array-2d-types*       {:clj (->array-nd-types* 2 )})
(def array-3d-types*       {:clj (->array-nd-types* 3 )})
(def array-4d-types*       {:clj (->array-nd-types* 4 )})
(def array-5d-types*       {:clj (->array-nd-types* 5 )})
(def array-6d-types*       {:clj (->array-nd-types* 6 )})
(def array-7d-types*       {:clj (->array-nd-types* 7 )})
(def array-8d-types*       {:clj (->array-nd-types* 8 )})
(def array-9d-types*       {:clj (->array-nd-types* 9 )})
(def array-10d-types*      {:clj (->array-nd-types* 10)})
(def array-types           (cond-union (->> array-1d-types*  (map (fn [[k v]] [k (-> v vals set)])) (into {}))
                                       (->> array-2d-types*  (map (fn [[k v]] [k (-> v vals set)])) (into {}))
                                       (->> array-3d-types*  (map (fn [[k v]] [k (-> v vals set)])) (into {}))
                                       (->> array-4d-types*  (map (fn [[k v]] [k (-> v vals set)])) (into {}))
                                       (->> array-5d-types*  (map (fn [[k v]] [k (-> v vals set)])) (into {}))
                                       (->> array-6d-types*  (map (fn [[k v]] [k (-> v vals set)])) (into {}))
                                       (->> array-7d-types*  (map (fn [[k v]] [k (-> v vals set)])) (into {}))
                                       (->> array-8d-types*  (map (fn [[k v]] [k (-> v vals set)])) (into {}))
                                       (->> array-9d-types*  (map (fn [[k v]] [k (-> v vals set)])) (into {}))
                                       (->> array-10d-types* (map (fn [[k v]] [k (-> v vals set)])) (into {}))))

; String: A special wrapper for char array where different encodings, etc. are possible

; Mutable String
(def !string-types        '{:clj #{StringBuilder} :cljs #{goog.string.StringBuffer}})
; Immutable String
(def string-types         `{:clj #{String} :cljs #{(type "")}})

(def char-seq-types        {:clj '#{CharSequence}})

; ===== VECTORS ===== ; Sequential, Associative (specifically, whose keys are sequential,
                      ; dense integer values), extensible

(def !array-list-types    '{:clj  #{java.util.ArrayList
                                    java.util.Arrays$ArrayList} ; indexed and associative, but not extensible
                            :cljs #_cljs.core.ArrayList ; not used
                                  #{(type (cljs.core/array))}}) ; because supports .push etc.
; svec = "spliceable vector"
(def svector-types        '{:clj  #{clojure.core.rrb_vector.rrbt.Vector}
                            :cljs #{clojure.core.rrb_vector.rrbt.Vector}})
(def +vector-types         {:clj  '#{clojure.lang.IPersistentVector}
                            :cljs (set/union (:cljs svector-types)
                                             '#{cljs.core/PersistentVector})})
(def !+vector-types       '{:clj  #{clojure.lang.ITransientVector}
                            :cljs #{cljs.core/TransientVector}})
(def ?!+vector-types       (cond-union +vector-types !+vector-types))
(def !vector:long-types   '{:clj  #{it.unimi.dsi.fastutil.longs.LongArrayList}})
(def !vector:ref-types    '{:clj  #{java.util.ArrayList}
                            :cljs #{(type (cljs.core/array))}})  ; because supports .push etc.
(def !vector-types         (cond-union !vector:long-types
                                       !vector:ref-types))
                           ; java.util.Vector is deprecated, because you can
                           ; just create a synchronized wrapper over an ArrayList
                           ; via java.util.Collections
(def !!vector-types           {})
(def vector-types             (cond-union ?!+vector-types !vector-types !!vector-types))

; ===== QUEUES ===== ; Particularly FIFO queues, as LIFO = stack = any vector

(def +queue-types         '{:clj  #{clojure.lang.PersistentQueue}
                            :cljs #{cljs.core/PersistentQueue   }})
(def !+queue-types         {})
(def ?!+queue-types        (cond-union +queue-types !+queue-types))
(def !queue-types         '{:clj  #{java.util.ArrayDeque} ; TODO *MANY* more here
                            :cljs #{goog.structs.Queue}})
(def !!queue-types         {}) ; TODO *MANY* more here
(def queue-types           {:clj  (set/union (:clj ?!+queue-types)
                                             '#{java.util.Queue})
                            :cljs (set/union (:cljs ?!+queue-types)
                                             (:cljs !queue-types)
                                             (:cljs !!queue-types))})

; ===== GENERIC ===== ;

; ----- PRIMITIVES ----- ;

(def primitive-unboxed-types (cond-union unboxed-bool-types unboxed-byte-types unboxed-char-types
                               unboxed-short-types unboxed-int-types unboxed-long-types
                               unboxed-float-types unboxed-double-types))

(def prim-types primitive-unboxed-types)

(def prim-comparable-types (cond-union unboxed-byte-types unboxed-char-types
                             unboxed-short-types unboxed-int-types unboxed-long-types
                             unboxed-float-types unboxed-double-types))

; Possibly can't check for boxedness in Java because it does auto-(un)boxing, but it's nice to have
(def primitive-boxed-types (cond-union boxed-bool-types boxed-byte-types boxed-char-types
                             boxed-short-types boxed-int-types boxed-long-types
                             boxed-float-types boxed-double-types))

(def primitive-types       (cond-union bool-types byte-types char-types
                             short-types int-types long-types
                             float-types double-types
                             #_{:cljs #{(type "")}}))

; Standard "uncuttable" types
(def integral-types        (cond-union bool-types byte-types char-types number-types))

; ----- COLLECTIONS ----- ;

                           ; TODO this might be ambiguous
                           ; TODO clojure.lang.Indexed / cljs.core/IIndexed?
(def indexed-types         (cond-union array-types string-types vector-types
                             '{:clj #{clojure.lang.APersistentVector$RSeq}}))
                           ; TODO this might be ambiguous
                           ; TODO clojure.lang.Associative / cljs.core/IAssociative?
(def associative-types     (cond-union map-types set-types indexed-types))
                           ; TODO this might be ambiguous
                           ; TODO clojure.lang.Sequential / cljs.core/ISequential?
(def sequential-types      (cond-union seq-types list-types indexed-types))
                           ; TODO this might be ambiguous
                           ; TODO clojure.lang.ICollection / cljs.core/ICollection?
(def counted-types         (cond-union array-types string-types
                             {:clj  (set/union (:clj !vector-types) (:clj !!vector-types)
                                               (:clj !map-types )   (:clj !!map-types)
                                               (:clj !set-types )   (:clj !!set-types)
                                               '#{clojure.lang.Counted})
                              :cljs (set/union (:cljs vector-types)
                                               (:cljs map-types)
                                               (:cljs set-types))}))

(def coll-types            (cond-union sequential-types associative-types))

(def sorted-types          {:clj '#{clojure.lang.Sorted java.util.SortedMap java.util.SortedSet}
                            :cljs (:cljs (cond-union sorted-set-types sorted-map-types))}) ; TODO add in `cljs.core/ISorted

(def transient-types      '{:clj  #{clojure.lang.ITransientCollection}
                            :cljs #{cljs.core/TransientVector
                                    cljs.core/TransientHashSet
                                    cljs.core/TransientArrayMap
                                    cljs.core/TransientHashMap}})

; Collections that have Transient counterparts
(def transientizable-types (cond-union #_core-tuple-types
                             '{:clj  #{clojure.lang.PersistentArrayMap
                                       clojure.lang.PersistentHashMap
                                       clojure.lang.PersistentHashSet
                                       clojure.lang.PersistentVector}
                               :cljs #{cljs.core/PersistentArrayMap
                                       cljs.core/PersistentHashMap
                                       cljs.core/PersistentHashSet
                                       cljs.core/PersistentVector}}))

(def editable-types       {:clj  '#{clojure.lang.IEditableCollection}
                           :cljs #_#{cljs.core/IEditableCollection} ; can't dispatch on a protocol
                             (get transientizable-types :cljs)})

; ===== FUNCTIONS ===== ;

(def fn-types             `{:clj #{clojure.lang.Fn}  :cljs #{(type inc)}})
(def ifn-types            `{:clj #{clojure.lang.IFn} :cljs #{(type inc)}}) ; TODO keyword types?
(def multimethod-types    '{:clj #{clojure.lang.MultiFn}})

; ===== MISCELLANEOUS ===== ;

(def regex-types          '{:clj  #{java.util.regex.Pattern}
                            :cljs #{js/RegExp              }})

(def atom-types           '{:clj  #{clojure.lang.IAtom}
                            :cljs #{cljs.core/Atom}})
(def volatile-types       '{:clj  #{clojure.lang.Volatile}
                            :cljs #{cljs.core/Volatile}})
(def atomic-types          {:clj  (set/union (:clj atom-types) (:clj volatile-types)
                                    '#{java.util.concurrent.atomic.AtomicReference
                                       ; From the java.util.concurrent package:
                                       ; "Additionally, classes are provided only for those
                                       ;  types that are commonly useful in intended applications.
                                       ;  For example, there is no atomic class for representing
                                       ;  byte. In those infrequent cases where you would like
                                       ;  to do so, you can use an AtomicInteger to hold byte
                                       ;  values, and cast appropriately. You can also hold floats
                                       ;  using Float.floatToIntBits and Float.intBitstoFloat
                                       ;  conversions, and doubles using Double.doubleToLongBits
                                       ;  and Double.longBitsToDouble conversions.
                                       java.util.concurrent.atomic.AtomicBoolean
                                     #_java.util.concurrent.atomic.AtomicByte
                                     #_java.util.concurrent.atomic.AtomicShort
                                       java.util.concurrent.atomic.AtomicInteger
                                       java.util.concurrent.atomic.AtomicLong
                                     #_java.util.concurrent.atomic.AtomicFloat
                                     #_java.util.concurrent.atomic.AtomicDouble
                                       com.google.common.util.concurrent.AtomicDouble
                                     })})

(def m2m-chan-types       '{:clj  #{clojure.core.async.impl.channels.ManyToManyChannel}
                            :cljs #{cljs.core.async.impl.channels/ManyToManyChannel}})

(def chan-types           '{:clj  #{clojure.core.async.impl.protocols.Channel}
                            :cljs #{cljs.core.async.impl.channels/ManyToManyChannel
                                    #_"TODO more?"}})

(def keyword-types        '{:clj  #{clojure.lang.Keyword}
                            :cljs #{cljs.core/Keyword}})

(def symbol-types         '{:clj  #{clojure.lang.Symbol}
                            :cljs #{cljs.core/Symbol}})

(def file-types          '{:clj  #{java.io.File}
                           :cljs #{#_js/File}}) ; isn't always available! Use an abstraction

(def any-types            {:clj  (set/union (:clj prim-types) #{'java.lang.Object})
                           :cljs '#{(quote default)}})

(def comparable-types      {:clj  (set/union '#{byte char short int long float double} '#{Comparable})
                            :cljs (:cljs number-types)})

(def record-types          '{:clj  #{clojure.lang.IRecord}
                             #_:cljs #_#{cljs.core/IRecord}}) ; because can't protocol-dispatch on protocols in CLJS

(def transformer-types     '{:clj #{#_clojure.core.protocols.CollReduce ; no, in order to find most specific type
                                    quantum.core.type.defs.Transformer}
                             :cljs #{#_cljs.core/IReduce ; CLJS problems with dispatching on protocol
                                     quantum.core.type.defs.Transformer}})

#_(def reducible-types       (cond-union
                             array-types
                             string-types
                             record-types
                             reducer-types
                             chan-types
                             {:cljs (:cljs +map-types)}
                             {:cljs (:cljs +set-types)}
                             integer-types
                             {:clj  '#{clojure.lang.IReduce
                                       clojure.lang.IReduceInit
                                       clojure.lang.IKVReduce
                                       #_clojure.core.protocols.CollReduce} ; no, in order to find most specific type
                              #_:cljs #_'#{cljs.core/IReduce}}  ; because can't protocol-dispatch on protocols in CLJS
                             {:clj  '#{fast_zip.core.ZipperLocation}
                              :cljs '#{fast-zip.core/ZipperLocation}}))

(def booleans-types       {:clj #{(-> array-1d-types*  :clj  :boolean)}})
(def bytes-types          {:clj #{(-> array-1d-types*  :clj  :byte   )} :cljs #{(-> array-1d-types* :cljs :byte   )}})
(def ubytes-types         {                                             :cljs #{(-> array-1d-types* :cljs :ubyte  )}})
(def ubytes-clamped-types {                                             :cljs #{(-> array-1d-types* :cljs :ubyte-clamped)}})
(def chars-types          {:clj #{(-> array-1d-types*  :clj  :char   )} :cljs #{(-> array-1d-types* :cljs :char   )}})
(def shorts-types         {:clj #{(-> array-1d-types*  :clj  :short  )} :cljs #{(-> array-1d-types* :cljs :short  )}})
(def ushorts-types        {                                             :cljs #{(-> array-1d-types* :cljs :ushort )}})
(def ints-types           {:clj #{(-> array-1d-types*  :clj  :int    )} :cljs #{(-> array-1d-types* :cljs :int    )}})
(def uints-types          {                                             :cljs #{(-> array-1d-types* :cljs :uint  )}})
(def longs-types          {:clj #{(-> array-1d-types*  :clj  :long   )} :cljs #{(-> array-1d-types* :cljs :long   )}})
(def floats-types         {:clj #{(-> array-1d-types*  :clj  :float  )} :cljs #{(-> array-1d-types* :cljs :float  )}})
(def doubles-types        {:clj #{(-> array-1d-types*  :clj  :double )} :cljs #{(-> array-1d-types* :cljs :double )}})
(def objects-types        {:clj #{(-> array-1d-types*  :clj  :object )} :cljs #{(-> array-1d-types* :cljs :object )}})

(def numeric-1d-types  (cond-union bytes-types
                                   ubytes-types
                                   ubytes-clamped-types
                                   chars-types
                                   shorts-types
                                   ints-types
                                   uints-types
                                   longs-types
                                   floats-types
                                   doubles-types))

(def booleans-2d-types {:clj #{(-> array-2d-types* :clj :boolean)} :cljs #{(-> array-2d-types* :cljs :boolean)}})
(def bytes-2d-types    {:clj #{(-> array-2d-types* :clj :byte   )} :cljs #{(-> array-2d-types* :cljs :byte   )}})
(def chars-2d-types    {:clj #{(-> array-2d-types* :clj :char   )} :cljs #{(-> array-2d-types* :cljs :char   )}})
(def shorts-2d-types   {:clj #{(-> array-2d-types* :clj :short  )} :cljs #{(-> array-2d-types* :cljs :short  )}})
(def ints-2d-types     {:clj #{(-> array-2d-types* :clj :int    )} :cljs #{(-> array-2d-types* :cljs :int    )}})
(def longs-2d-types    {:clj #{(-> array-2d-types* :clj :long   )} :cljs #{(-> array-2d-types* :cljs :long   )}})
(def floats-2d-types   {:clj #{(-> array-2d-types* :clj :float  )} :cljs #{(-> array-2d-types* :cljs :float  )}})
(def doubles-2d-types  {:clj #{(-> array-2d-types* :clj :double )} :cljs #{(-> array-2d-types* :cljs :double )}})
(def objects-2d-types  {:clj #{(-> array-2d-types* :clj :object )} :cljs #{(-> array-2d-types* :cljs :object )}})
(def numeric-2d-types  (cond-union bytes-2d-types
                                   chars-2d-types
                                   shorts-2d-types
                                   ints-2d-types
                                   longs-2d-types
                                   floats-2d-types
                                   doubles-2d-types))

; ===== PREDICATES ===== ;

#?(:clj
(defmacro gen-<type-pred=>type> []
  (->> (ns-interns *ns*)
       keys
       (filter (fn-> name (str/ends-with? "-types")))
       (map    (fn [t] [(list 'quote (symbol (str/replace (name t) #"-types$" "?")))
                        t]))
       (into   {}))))

(def type-pred=>type
  (merge (gen-<type-pred=>type>)
    {'default              {:clj  '#{Object}
                            :cljs '#{(quote default)}}
     'boolean-array?       {:clj #{(-> array-1d-types*  :clj  :boolean)}}
     'byte-array?          {:clj #{(-> array-1d-types*  :clj  :byte   )} :cljs #{(-> array-1d-types* :cljs :byte   )}}
     'ubyte-array?         {                                             :cljs #{(-> array-1d-types* :cljs :ubyte  )}}
     'ubyte-array-clamped? {                                             :cljs #{(-> array-1d-types* :cljs :ubyte-clamped)}}
     'char-array?          {:clj #{(-> array-1d-types*  :clj  :char   )} :cljs #{(-> array-1d-types* :cljs :char   )}}
     'short-array?         {:clj #{(-> array-1d-types*  :clj  :short  )} :cljs #{(-> array-1d-types* :cljs :short  )}}
     'ushort-array?        {                                             :cljs #{(-> array-1d-types* :cljs :ushort )}}
     'int-array?           {:clj #{(-> array-1d-types*  :clj  :int    )} :cljs #{(-> array-1d-types* :cljs :int    )}}
     'uint-array?          {                                             :cljs #{(-> array-1d-types* :cljs :uint  )}}
     'long-array?          {:clj #{(-> array-1d-types*  :clj  :long   )} :cljs #{(-> array-1d-types* :cljs :long   )}}
     'float-array?         {:clj #{(-> array-1d-types*  :clj  :float  )} :cljs #{(-> array-1d-types* :cljs :float  )}}
     'double-array?        {:clj #{(-> array-1d-types*  :clj  :double )} :cljs #{(-> array-1d-types* :cljs :double )}}
     'object-array?        {:clj #{(-> array-1d-types*  :clj  :object )} :cljs #{(-> array-1d-types* :cljs :object )}}

     'array-1d?            {:clj  (->> array-1d-types*  :clj  vals set)
                            :cljs (->> array-1d-types*  :cljs vals set)}

     'array-2d?            {:clj  (->> array-2d-types*  :clj  vals set)
                            :cljs (->> array-2d-types*  :cljs vals set)}
     'array-3d?            {:clj  (->> array-3d-types*  :clj  vals set)
                            :cljs (->> array-3d-types*  :cljs vals set)}
     'array-4d?            {:clj  (->> array-4d-types*  :clj  vals set)
                            :cljs (->> array-4d-types*  :cljs vals set)}
     'array-5d?            {:clj  (->> array-5d-types*  :clj  vals set)
                            :cljs (->> array-5d-types*  :cljs vals set)}
     'array-6d?            {:clj  (->> array-6d-types*  :clj  vals set)
                            :cljs (->> array-6d-types*  :cljs vals set)}
     'array-7d?            {:clj  (->> array-7d-types*  :clj  vals set)
                            :cljs (->> array-7d-types*  :cljs vals set)}
     'array-8d?            {:clj  (->> array-8d-types*  :clj  vals set)
                            :cljs (->> array-8d-types*  :cljs vals set)}
     'array-9d?            {:clj  (->> array-9d-types*  :clj  vals set)
                            :cljs (->> array-9d-types*  :cljs vals set)}
     'array-10d?           {:clj  (->> array-10d-types* :clj  vals set)
                            :cljs (->> array-10d-types* :cljs vals set)}}))
