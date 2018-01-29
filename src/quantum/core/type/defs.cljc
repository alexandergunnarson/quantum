(ns
  ^{:doc "Definitions for types."
    :attribution "alexandergunnarson"}
  quantum.core.type.defs
  (:refer-clojure :exclude [boolean byte char short int long float double])
  (:require
    [clojure.core          :as core]
 #?(:cljs
    [com.gfredericks.goog.math.Integer])
    [fast-zip.core]
    [clojure.string        :as str]
    [quantum.core.classes  :as classes]
    [quantum.core.data.map :as map
      :refer [map-entry]]
    [quantum.core.data.set :as set]
    [quantum.core.data.tuple
      #?@(:cljs [:refer [Tuple]])]
    [quantum.core.fn       :as fn
      :refer [<- fn-> rcomp]]
    [quantum.core.logic    :as logic
      :refer [fn-and condf1 fn=]]
    [quantum.untyped.core.form.evaluate
      :refer [env-lang]]
    [quantum.untyped.core.numeric.combinatorics :as combo])
#?(:cljs
  (:require-macros
    [quantum.core.type.defs :as self
      :refer [->array-nd-types*
              !hash-map-types:gen
              !unsorted-map-types:gen
              !sorted-map-types:gen
              !map-types:gen
              !hash-set-types:gen
              !unsorted-set-types:gen
              !sorted-set-types:gen
              !set-types:gen
              gen-<type-pred=>type>]]))
  (:import
    #?@(:clj  [; clojure.core.async.impl.channels.ManyToManyChannel
               com.google.common.util.concurrent.AtomicDouble
               quantum.core.data.tuple.Tuple]
        :cljs [goog.string.StringBuffer
               goog.structs.Map
               goog.structs.Set
               goog.structs.AvlTree
               goog.structs.Queue])))

#?(:clj (def boolean Boolean/TYPE))
#?(:clj (def byte    Byte/TYPE))
#?(:clj (def char    Character/TYPE))
#?(:clj (def short   Short/TYPE))
#?(:clj (def int     Integer/TYPE))
#?(:clj (def long    Long/TYPE))
#?(:clj (def float   Float/TYPE))
#?(:clj (def double  Double/TYPE))

(def ^{:doc "Could do <Class>/MAX_VALUE for the maxes in Java but JS doesn't like it of course
             In JavaScript, all numbers are 64-bit floating point numbers.
             This means you can't represent in JavaScript all the Java longs
             Max 'safe' int: (dec (Math/pow 2 53))"}
  primitive-type-meta
  {'boolean {:bits 1
             :min  0
             :max  1
   #?@(:clj [:array-ident "Z"
             :outer-type "[Z"
             :boxed      'java.lang.Boolean
             :unboxed    'Boolean/TYPE])}
   'byte    {:bits 8
             :min -128
             :max  127
   #?@(:clj [:array-ident "B"
             :outer-type  "[B"
             :boxed       'java.lang.Byte
             :unboxed     'Byte/TYPE])}
   'short   {:bits 16
             :min -32768
             :max  32767
   #?@(:clj [:array-ident "S"
             :outer-type  "[S"
             :boxed       'java.lang.Short
             :unboxed     'Short/TYPE])}
   'char    {:bits 16
             :min  0
             :max  65535
   #?@(:clj [:array-ident "C"
             :outer-type  "[C"
             :boxed       'java.lang.Character
             :unboxed     'Character/TYPE])}
   'int     {:bits 32
             :min -2147483648
             :max  2147483647
   #?@(:clj [:array-ident "I"
             :outer-type  "[I"
             :boxed       'java.lang.Integer
             :unboxed     'Integer/TYPE])}
   'long    {:bits 64
             :min -9223372036854775808
             :max  9223372036854775807
   #?@(:clj [:array-ident "J"
             :outer-type  "[J"
             :boxed       'java.lang.Long
             :unboxed     'Long/TYPE])}
   ; Technically with floating-point nums, "min" isn't the most negative;
   ; it's the smallest absolute
   'float   {:bits 32
             :min  1.4E-45
             :max  3.4028235E38
   #?@(:clj [:array-ident "F"
             :outer-type  "[F"
             :boxed       'java.lang.Float
             :unboxed     'Float/TYPE])}
   'double  {:bits 64
             ; Because:
             ; Double/MIN_VALUE        = 4.9E-324
             ; (.-MIN_VALUE js/Number) = 5e-324
             :min  #?(:clj  Double/MIN_VALUE
                      :cljs (.-MIN_VALUE js/Number))
             :max  1.7976931348623157E308 ; Max number in JS
   #?@(:clj [:array-ident "D"
             :outer-type  "[D"
             :boxed       'java.lang.Double
             :unboxed     'Double/TYPE])}})

(def array-ident->primitive-sym
  (->> primitive-type-meta (map (juxt (rcomp val :array-ident) key)) (into {})))

(def elem-types-clj
  (->> primitive-type-meta
       (map (fn [[k v]] [(:outer-type v) k]))
       (reduce
         (fn [m [k v]]
           (assoc m k v (symbol k) v))
         {})))

#?(:clj
(def boxed-types*
  (->> primitive-type-meta
       (map (fn [[k v]] [k (:boxed v)]))
       (into {}))))

#?(:clj
(def unboxed-types*
  (zipmap (vals boxed-types*) (keys boxed-types*))))

#?(:clj
(def boxed->unboxed-types-evaled (->> primitive-type-meta vals (map (juxt :boxed :unboxed)) (into {}) eval)))

(def max-values
  (->> primitive-type-meta
       (map (fn [[k v]] [k (:max v)]))
       (into {})))

#?(:clj
(def promoted-types*
  {'short  'int
   'byte   'short ; Because char is unsigned
   'char   'int
   'int    'long
   'float  'double}))

(defn max-type [types]
  (->> types
       (map (fn [type] [(get max-values type) type]))
       (remove (fn-> first nil?))
       (into (core/sorted-map-by >))
       first val))

#?(:clj (def class->str (fn-> str (.substring 6))))

(defn- retrieve [lang-n sets] (->> sets (map lang-n) (remove empty?) (apply set/union)))

(defn- cond-union [& sets]
  {:clj  (retrieve :clj sets)
   :cljs (retrieve :cljs sets)})

#?(:clj
(defmacro ->array-nd-types* [n]
  `{:boolean '~(symbol (str (apply str (repeat n \[)) "Z"))
    :byte    '~(symbol (str (apply str (repeat n \[)) "B"))
    :char    '~(symbol (str (apply str (repeat n \[)) "C"))
    :short   '~(symbol (str (apply str (repeat n \[)) "S"))
    :int     '~(symbol (str (apply str (repeat n \[)) "I"))
    :long    '~(symbol (str (apply str (repeat n \[)) "J"))
    :float   '~(symbol (str (apply str (repeat n \[)) "F"))
    :double  '~(symbol (str (apply str (repeat n \[)) "D"))
    :object  '~(symbol (str (apply str (repeat n \[)) "Ljava.lang.Object;"))}))

(def primitive-type-map    {:clj {'(type (boolean-array [false])) (symbol "[Z")
                                  '(type (byte-array    0)      ) (symbol "[B")
                                  '(type (char-array    "")     ) (symbol "[C")
                                  '(type (short-array   0)      ) (symbol "[S")
                                  '(type (long-array    0)      ) (symbol "[J")
                                  '(type (float-array   0)      ) (symbol "[F")
                                  '(type (int-array     0)      ) (symbol "[I")
                                  '(type (double-array  0.0)    ) (symbol "[D")
                                  '(type (object-array  [])     ) (symbol "[Ljava.lang.Object;")}
                            :cljs `{(type ""                     ) ~'string
                                    (type 123                    ) ~'number
                                    (type (cljs.core/clj->js {}) ) ~'object
                                    (type true                   ) ~'boolean
                                    (type (cljs.core/array)      ) ~'array
                                    (type inc                    ) ~'function}})

; ______________________ ;
; ===== PRIMITIVES ===== ;
; •••••••••••••••••••••• ;

(def nil-types            '{:clj #{nil} :cljs #{nil}})

; ===== NON-NUMERIC PRIMITIVES ===== ; ; TODO CLJS

(def unboxed-bool-types    {:clj  '#{boolean}
                            :cljs `#{(type true)}})
(def unboxed-boolean-types unboxed-bool-types)
(def boxed-bool-types      {:clj  '#{java.lang.Boolean}
                            :cljs `#{(type true)}})
(def boxed-boolean-types   boxed-bool-types)
(def ?bool-types           boxed-bool-types)
(def ?boolean-types        ?bool-types)
(def bool-types            (cond-union unboxed-bool-types boxed-bool-types))
(def boolean-types         bool-types)

(def unboxed-byte-types   '{:clj  #{byte}})
(def boxed-byte-types     '{:clj  #{java.lang.Byte}})
(def ?byte-types           boxed-byte-types)
(def byte-types            (cond-union unboxed-byte-types boxed-byte-types))

(def unboxed-char-types   '{:clj  #{char}})
(def boxed-char-types     '{:clj  #{java.lang.Character}})
(def ?char-types           boxed-char-types)
(def char-types            (cond-union unboxed-char-types boxed-char-types))

; ===== NUMBERS ===== ; ; TODO CLJS

; ----- INTEGERS ----- ;

(def unboxed-short-types  '{:clj  #{short}})
(def boxed-short-types    '{:clj  #{java.lang.Short}})
(def ?short-types          boxed-short-types)
(def short-types           (cond-union unboxed-short-types boxed-short-types))

(def unboxed-int-types     {:clj  '#{int}
                             ; because the integral values representable by JS numbers are in the
                             ; range of Java ints, though technically one needs to ensure that
                             ; there is only an integral value, no decimal value
                            :cljs `#{(type 123)}})
(def boxed-int-types       {:clj  '#{java.lang.Integer}
                            :cljs `#{(type 123)}})
(def ?int-types            boxed-int-types)
(def int-types             (cond-union unboxed-int-types boxed-int-types))

(def unboxed-long-types   '{:clj  #{long}})
(def boxed-long-types     '{:clj  #{java.lang.Long}})
(def ?long-types           boxed-long-types)
(def long-types            (cond-union unboxed-long-types boxed-long-types))

(def bigint-types         '{:clj  #{clojure.lang.BigInt java.math.BigInteger}
                            :cljs #{com.gfredericks.goog.math.Integer}})

(def integer-types         (cond-union unboxed-short-types unboxed-int-types unboxed-long-types bigint-types))

; ----- DECIMALS ----- ;

(def unboxed-float-types  '{:clj  #{float}})
(def boxed-float-types    '{:clj  #{java.lang.Float}})
(def ?float-types          boxed-float-types)
(def float-types           (cond-union unboxed-float-types boxed-float-types))

(def unboxed-double-types  {:clj  '#{double}
                            :cljs `#{(type 123)}})
(def boxed-double-types    {:clj  '#{java.lang.Double}
                            :cljs `#{(type 123)}})
(def ?double-types         boxed-double-types)
(def double-types          (cond-union unboxed-double-types boxed-double-types))

(def bigdec-types         '{:clj #{java.math.BigDecimal}})

(def decimal-types         (cond-union unboxed-float-types unboxed-double-types bigdec-types))

; ----- GENERAL ----- ;

(def ratio-types          '{:clj  #{clojure.lang.Ratio}
                            :cljs #{quantum.core.numeric.types.Ratio}})

(def number-types          {:clj  (set/union
                                    (:clj (cond-union unboxed-short-types unboxed-int-types unboxed-long-types
                                                      unboxed-float-types unboxed-double-types))
                                    '#{java.lang.Number})
                            :cljs (:cljs (cond-union integer-types decimal-types ratio-types))})


(def pnumber-types        `{:cljs #{(type 123)}})

; The closest thing to a native int the platform has
(def nat-int-types        `{:clj  #{~'int}
                            :cljs #{(type 123)}})

; The closest thing to a native long the platform has
(def nat-long-types       `{:clj  #{~'long}
                            :cljs #{(type 123)}})

; _______________________ ;
; ===== COLLECTIONS ===== ;
; ••••••••••••••••••••••• ;

; ===== TUPLES ===== ;

(def tuple-types          `{:clj  #{Tuple} ; clojure.lang.Tuple was discontinued; we won't support it for now
                            :cljs #{Tuple}})
(def map-entry-types      '{:clj #{java.util.Map$Entry}})

; ===== SEQUENCES ===== ; Sequential (generally not efficient Lookup / RandomAccess)

(def cons-types           '{:clj  #{clojure.lang.Cons}
                            :cljs #{cljs.core/Cons}})
(def lseq-types           '{:clj  #{clojure.lang.LazySeq}
                            :cljs #{cljs.core/LazySeq   }})
(def misc-seq-types       '{:clj  #{clojure.lang.APersistentMap$ValSeq
                                    clojure.lang.APersistentMap$KeySeq
                                    clojure.lang.PersistentVector$ChunkedSeq
                                    clojure.lang.IndexedSeq}
                            :cljs #{cljs.core/ValSeq
                                    cljs.core/KeySeq
                                    cljs.core/IndexedSeq
                                    cljs.core/ChunkedSeq}})

(def non-list-seq-types    (cond-union cons-types lseq-types misc-seq-types))

; ----- LISTS ----- ; Not extremely different from Sequences ; TODO clean this up

(def cdlist-types          {}
                        #_'{:clj  #{clojure.data.finger_tree.CountedDoubleList
                                    quantum.core.data.finger_tree.CountedDoubleList}
                            :cljs #{quantum.core.data.finger-tree/CountedDoubleList}})
(def dlist-types           {}
                        #_'{:clj  #{clojure.data.finger_tree.CountedDoubleList
                                    quantum.core.data.finger_tree.CountedDoubleList}
                            :cljs #{quantum.core.data.finger-tree/CountedDoubleList}})
(def +list-types           {:clj  '#{clojure.lang.IPersistentList}
                            :cljs (set/union (:cljs dlist-types)
                                             (:cljs cdlist-types)
                                             '#{cljs.core/List cljs.core/EmptyList})})
(def !list-types          '{:clj  #{java.util.LinkedList}})
(def list-types            {:clj  '#{java.util.List}
                            :cljs (:cljs +list-types)})

; ----- GENERIC ----- ;

(def seq-types             {:clj  '#{clojure.lang.ISeq}
                            :cljs (:cljs (cond-union non-list-seq-types list-types))})

; ===== MAPS ===== ; Associative

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

(def +array-map-types             '{:clj  #{clojure.lang.PersistentArrayMap}
                                    :cljs #{cljs.core/PersistentArrayMap}})
(def !+array-map-types            '{:clj  #{clojure.lang.PersistentArrayMap$TransientArrayMap}
                                    :cljs #{cljs.core/TransientArrayMap}})
(def ?!+array-map-types            (cond-union !+array-map-types +array-map-types))
(def !array-map-types              {})
(def !!array-map-types             {})
(def array-map-types               (cond-union ?!+array-map-types
                                               !array-map-types !!array-map-types))

(def +hash-map-types              '{:clj  #{clojure.lang.PersistentHashMap}
                                    :cljs #{cljs.core/PersistentHashMap}})
(def !+hash-map-types             '{:clj  #{clojure.lang.PersistentHashMap$TransientHashMap}
                                    :cljs #{cljs.core/TransientHashMap}})
(def ?!+hash-map-types             (cond-union !+hash-map-types +hash-map-types))

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
  '{:clj #{java.util.TreeSet}}) ; CLJS can have via AVLTree with same KVs

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
                                    quantum.untyped.core.reducers.Transformer}
                             :cljs #{#_cljs.core/IReduce ; CLJS problems with dispatching on protocol
                                     quantum.untyped.core.reducers.Transformer}})

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

(def objects-nd-types     {:clj  #{(-> array-1d-types*  :clj  :object )
                                   (-> array-2d-types*  :clj  :object )
                                   (-> array-3d-types*  :clj  :object )
                                   (-> array-4d-types*  :clj  :object )
                                   (-> array-5d-types*  :clj  :object )
                                   (-> array-6d-types*  :clj  :object )
                                   (-> array-7d-types*  :clj  :object )
                                   (-> array-8d-types*  :clj  :object )
                                   (-> array-9d-types*  :clj  :object )
                                   (-> array-10d-types* :clj  :object ) }
                           :cljs (:cljs objects-types)})

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

     #_'objects

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

; TODO make all this extensible

(defn- unevaled-fn [lang]
  (->> type-pred=>type
       (map (fn [[pred types-n]] (map-entry pred (get types-n lang))))
       (remove (fn-> val empty?))
       (into {})))

#?(:clj
(defmacro gen-types|unevaled []
  (let [langs #{:clj :cljs}
        #_code  #_`(do ~(list 'def 'types-unevaled `'~unevaled))]
    `'~(->> langs
            (map unevaled-fn)
            (zipmap langs)
            (map (fn [[lang-n type-map-n]]
                   (map-entry lang-n
                     (->> type-map-n
                          (map (fn [[pred-n types-n]]
                                 (map-entry pred-n
                                   (->> types-n
                                        (map (condf1
                                               (fn-and seq? (fn-> first name (= "type")))
                                                 (fn [obj]
                                                   (condp = lang-n
                                                     :clj  (-> obj eval class->str symbol)
                                                     :cljs (get-in primitive-type-map [lang-n obj])
                                                     obj))
                                               (fn-and seq? (fn-> first name (= "quote")))
                                                 second
                                               identity))
                                        (into #{})))))
                          (into {})))))
            (into {})))))

(defmacro gen-types []
  (let [lang-unevaled (unevaled-fn (env-lang))]
    `(zipmap '[~@(keys lang-unevaled)]
              [~@(vals lang-unevaled)])))

(def types|unevaled (gen-types|unevaled))
(def types          (gen-types))
