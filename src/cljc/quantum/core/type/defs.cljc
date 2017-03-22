(ns
  ^{:doc "Definitions for types."
    :attribution "alexandergunnarson"}
  quantum.core.type.defs
  (:require
    [clojure.core          :as core]
 #?(:cljs
    [com.gfredericks.goog.math.Integer])
    [fast-zip.core]
    [quantum.core.data.map :as map
      :refer [map-entry]]
    [quantum.core.data.set :as set]
    [quantum.core.data.tuple
      #?@(:cljs [:refer [Tuple]])]
    [quantum.core.fn       :as fn
      :refer [fn-> rcomp]]
    [quantum.core.logic    :as logic
      :refer [fn-and condf1]])
  (:require-macros
    [quantum.core.type.defs :as self
      :refer [array-nd-types]])
  (:import
    #?@(:clj  [clojure.core.async.impl.channels.ManyToManyChannel
               com.google.common.util.concurrent.AtomicDouble
               quantum.core.data.tuple.Tuple]
        :cljs [goog.string.StringBuffer
               goog.structs.Map
               goog.structs.Set
               goog.structs.AvlTree
               goog.structs.Queue])))

(defrecord Folder  [coll transform])
(defrecord Reducer [coll transform])

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
   'short   {:bits 16
             :min -32768
             :max  32767
   #?@(:clj [:array-ident "S"
             :outer-type  "[S"
             :boxed       'java.lang.Short
             :unboxed     'Short/TYPE])}
   'byte    {:bits 8
             :min -128
             :max  127
   #?@(:clj [:array-ident "B"
             :outer-type  "[B"
             :boxed       'java.lang.Byte
             :unboxed     'Byte/TYPE])}
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
(def boxed-types
  (->> primitive-type-meta
       (map (fn [[k v]] [k (:boxed v)]))
       (into {}))))

#?(:clj
(def unboxed-types
  (zipmap (vals boxed-types) (keys boxed-types))))

#?(:clj
(def boxed->unboxed-types-evaled (->> primitive-type-meta vals (map (juxt :boxed :unboxed)) (into {}) eval)))

(def max-values
  (->> primitive-type-meta
       (map (fn [[k v]] [k (:max v)]))
       (into {})))

#?(:clj
(def promoted-types
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
  {:cljc (retrieve :cljc sets)
   :clj  (retrieve :clj  sets)
   :cljs (retrieve :cljs sets)})

#?(:clj
(defmacro array-nd-types [n]
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

; ===== NON-NUMERIC PRIMITIVES ===== ; ; TODO CLJS

(def unboxed-bool-types    {:clj  '#{boolean}
                            :cljs `#{(type true)}})
(def boxed-bool-types      {:clj  '#{java.lang.Boolean}
                            :cljs `#{(type true)}})
(def bool-types            (cond-union unboxed-bool-types boxed-bool-types))
(def unboxed-byte-types   '{:clj  #{byte}})
(def boxed-byte-types     '{:clj  #{java.lang.Byte}})
(def byte-types            (cond-union unboxed-byte-types boxed-byte-types))
(def unboxed-char-types   '{:clj  #{char}})
(def boxed-char-types     '{:clj  #{java.lang.Character}})
(def char-types            (cond-union unboxed-char-types boxed-char-types))

; ===== NUMBERS ===== ; ; TODO CLJS

; ----- INTEGERS ----- ;

(def unboxed-short-types  '{:clj  #{short}})
(def boxed-short-types    '{:clj  #{java.lang.Short}})
(def short-types           (cond-union unboxed-short-types boxed-short-types))
(def unboxed-int-types     {:clj  '#{int}
                             ; because the integral values representable by JS numbers are in the
                             ; range of Java ints, though technically one needs to ensure that
                             ; there is only an integral value, no decimal value
                            :cljs `#{(type 123)}})
(def boxed-int-types       {:clj  '#{java.lang.Integer}
                            :cljs `#{(type 123)}})
(def int-types             (cond-union unboxed-int-types boxed-int-types))
(def unboxed-long-types   '{:clj  #{long}})
(def boxed-long-types     '{:clj  #{java.lang.Long}})
(def long-types            (cond-union unboxed-long-types boxed-long-types))

(def bigint-types         '{:clj  #{clojure.lang.BigInt java.math.BigInteger}
                            :cljs #{com.gfredericks.goog.math.Integer}})

(def integer-types         (cond-union unboxed-short-types unboxed-int-types unboxed-long-types bigint-types))

; ----- DECIMALS ----- ;

(def unboxed-float-types  '{:clj  #{float}})
(def boxed-float-types    '{:clj  #{java.lang.Float}})
(def float-types           (cond-union unboxed-float-types boxed-float-types))
(def unboxed-double-types  {:clj  '#{double}
                            :cljs `#{(type 123)}})
(def boxed-double-types    {:clj  '#{java.lang.Double}
                            :cljs `#{(type 123)}})
(def double-types          (cond-union unboxed-double-types boxed-double-types))

(def bigdec-types         '{:clj #{java.math.BigDecimal}})

(def decimal-types         (cond-union unboxed-float-types unboxed-double-types bigdec-types))

; ----- GENERAL ----- ;

(def ratio-types          '{:clj  #{clojure.lang.Ratio}
                            :cljs #{quantum.core.numeric.types.Ratio}})

(def number-types          {:clj  (set/union
                                    (:clj (cond-union unboxed-short-types unboxed-int-types unboxed-long-types
                                                      unboxed-float-types unboxed-double-types)
                                          '#{java.lang.Number}))
                            :cljs (:cljs (cond-union integer-types decimal-types ratio-types))})

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

(def +array-map-types     '{:clj  #{clojure.lang.PersistentArrayMap}
                            :cljs #{cljs.core/PersistentArrayMap}})
(def !+array-map-types    '{:clj  #{clojure.lang.PersistentArrayMap$TransientArrayMap}
                            :cljs #{cljs.core/TransientArrayMap}})
(def ?!+array-map-types    (cond-union !+array-map-types +array-map-types))
(def !array-map-types      {})
(def !!array-map-types     {})
(def array-map-types       (cond-union ?!+array-map-types
                                       !array-map-types !!array-map-types))

(def +hash-map-types      '{:clj  #{clojure.lang.PersistentHashMap}
                            :cljs #{cljs.core/PersistentHashMap}})
(def !+hash-map-types     '{:clj  #{clojure.lang.PersistentHashMap$TransientHashMap}
                            :cljs #{cljs.core/TransientHashMap}})
(def ?!+hash-map-types     (cond-union  !+hash-map-types   +hash-map-types))
(def !hash-map-types      '{:clj  #{java.util.HashMap
                                    java.util.IdentityHashMap}
                            :cljs #{goog.structs.Map}})
(def !!hash-map-types     '{:clj  #{java.util.concurrent.ConcurrentHashMap}})
(def hash-map-types        (cond-union ?!+hash-map-types
                                       !hash-map-types !!hash-map-types))

(def +unsorted-map-types   (cond-union   +hash-map-types   +array-map-types))
(def !+unsorted-map-types  (cond-union  !+hash-map-types  !+array-map-types))
(def ?!+unsorted-map-types (cond-union ?!+hash-map-types ?!+array-map-types))
(def !unsorted-map-types   !hash-map-types)
(def !!unsorted-map-types  !!hash-map-types)
(def unsorted-map-types    (cond-union ?!+unsorted-map-types
                                       !unsorted-map-types !!unsorted-map-types))

(def +sorted-map-types    '{:clj  #{clojure.lang.PersistentTreeMap}
                            :cljs #{cljs.core/PersistentTreeMap   }})
(def !+sorted-map-types    {})
(def ?!+sorted-map-types   (cond-union +sorted-map-types !+sorted-map-types))
(def !sorted-map-types    '{:clj  #{java.util.TreeMap}
                            :cljs #{goog.structs.AvlTree}})
(def !!sorted-map-types    {})
(def sorted-map-types      {:clj  (set/union (:clj ?!+sorted-map-types)
                                             '#{java.util.SortedMap})
                            :cljs (set/union (:cljs +sorted-map-types)
                                             (:cljs !sorted-map-types))})
(def !+map-types           {:clj  '#{clojure.lang.ITransientMap}
                            :cljs (set/union (:cljs !+unsorted-map-types))})
(def +map-types            {:clj  '#{clojure.lang.IPersistentMap}
                            :cljs (set/union (:cljs +unsorted-map-types)
                                             (:cljs +sorted-map-types))})
(def ?!+map-types          (cond-union !+map-types +map-types))
; technically also `object` for CLJS
(def !map-types            (cond-union !unsorted-map-types !sorted-map-types))
(def !!map-types           (cond-union !!unsorted-map-types !!sorted-map-types))
(def map-types             {:clj  (set/union (:clj !+map-types)
                                    '#{; TODO IPersistentMap as well, yes, but all persistent Clojure maps implement java.util.Map
                                       java.util.Map})
                            :cljs (set/union (:cljs ?!+map-types)
                                             (:cljs !map-types)
                                             (:cljs !!map-types))})

; ===== SETS ===== ; Associative; A special type of Map whose keys and vals are identical

(def +hash-set-types      '{:clj  #{clojure.lang.PersistentHashSet}
                            :cljs #{cljs.core/PersistentHashSet}})
(def !+hash-set-types     '{:clj  #{clojure.lang.PersistentHashSet$TransientHashSet}
                            :cljs #{cljs.core/TransientHashSet}})
(def ?!+hash-set-types     (cond-union !+hash-set-types +hash-set-types))
(def !hash-set-types      '{:clj  #{java.util.HashSet}
                            :cljs #{goog.structs.Set}})
(def !!hash-set-types      {}) ; technically you can make something from ConcurrentHashMap but...
(def hash-set-types        (cond-union ?!+hash-set-types
                             !hash-set-types !!hash-set-types))

(def +unsorted-set-types     +hash-set-types)
(def !+unsorted-set-types   !+hash-set-types)
(def ?!+unsorted-set-types ?!+hash-set-types)
(def !unsorted-set-types     !hash-set-types)
(def !!unsorted-set-types   !!hash-set-types)
(def unsorted-set-types       hash-set-types)

(def +sorted-set-types    '{:clj  #{clojure.lang.PersistentTreeSet}
                            :cljs #{cljs.core/PersistentTreeSet   }})
(def !+sorted-set-types    {})
(def ?!+sorted-set-types   (cond-union +sorted-set-types !+sorted-set-types))
(def !sorted-set-types    '{:clj  #{java.util.TreeSet}}) ; CLJS can have via AVLTree with same KVs
(def !!sorted-set-types    {})
(def sorted-set-types      {:clj  (set/union (:clj +sorted-set-types)
                                             '#{java.util.SortedSet})
                            :cljs (set/union (:cljs +sorted-set-types)
                                             (:cljs !sorted-set-types)
                                             (:cljs !!sorted-set-types))})

(def !+set-types           {:clj  '#{clojure.lang.ITransientSet}
                            :cljs (set/union (:cljs !+unsorted-set-types))})
(def +set-types            {:clj '#{clojure.lang.IPersistentSet}
                            :cljs (set/union (:cljs +unsorted-set-types)
                                             (:cljs +sorted-set-types))})
(def ?!+set-types          (cond-union !+set-types +set-types))
(def !set-types            (cond-union !unsorted-set-types !sorted-set-types))
(def !!set-types           (cond-union !!unsorted-set-types !!sorted-set-types))
(def set-types             {:clj  (set/union (:clj !+set-types)
                                    '#{; TODO IPersistentSet as well, yes, but all persistent Clojure sets implement java.util.Set
                                       java.util.Set})
                            :cljs (set/union (:cljs ?!+set-types)
                                             (:cljs !set-types)
                                             (:cljs !!set-types))})

; ===== ARRAYS ===== ; Sequential, Associative (specifically, whose keys are sequential,
                     ; dense integer values), not extensible
; TODO do e.g. {:clj {0 {:byte ...}}}
(def array-1d-types       `{:clj  {:byte          (type (byte-array    0)      )
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
(def array-2d-types        {:clj (array-nd-types 2 )})
(def array-3d-types        {:clj (array-nd-types 3 )})
(def array-4d-types        {:clj (array-nd-types 4 )})
(def array-5d-types        {:clj (array-nd-types 5 )})
(def array-6d-types        {:clj (array-nd-types 6 )})
(def array-7d-types        {:clj (array-nd-types 7 )})
(def array-8d-types        {:clj (array-nd-types 8 )})
(def array-9d-types        {:clj (array-nd-types 9 )})
(def array-10d-types       {:clj (array-nd-types 10)})
(def array-types           (cond-union (->> array-1d-types  (map (fn [[k v]] [k (-> v vals set)])) (into {}))
                                       (->> array-2d-types  (map (fn [[k v]] [k (-> v vals set)])) (into {}))
                                       (->> array-3d-types  (map (fn [[k v]] [k (-> v vals set)])) (into {}))
                                       (->> array-4d-types  (map (fn [[k v]] [k (-> v vals set)])) (into {}))
                                       (->> array-5d-types  (map (fn [[k v]] [k (-> v vals set)])) (into {}))
                                       (->> array-6d-types  (map (fn [[k v]] [k (-> v vals set)])) (into {}))
                                       (->> array-7d-types  (map (fn [[k v]] [k (-> v vals set)])) (into {}))
                                       (->> array-8d-types  (map (fn [[k v]] [k (-> v vals set)])) (into {}))
                                       (->> array-9d-types  (map (fn [[k v]] [k (-> v vals set)])) (into {}))
                                       (->> array-10d-types (map (fn [[k v]] [k (-> v vals set)])) (into {}))))

; String: A special wrapper for char array where different encodings, etc. are possible

; Mutable String
(def !string-types        '{:clj #{StringBuilder} :cljs #{goog.string.StringBuffer}})
; Immutable String
(def string-types         `{:clj #{String} :cljs #{(type "")}})

; ===== VECTORS ===== ; Sequential, Associative (specifically, whose keys are sequential,
                      ; dense integer values), extensible

(def !array-list-types    '{:clj  #{java.util.ArrayList
                                    java.util.Arrays$ArrayList} ; indexed and associative, but not extensible
                            :cljs #{cljs.core.ArrayList}}) ; TODO what is this?
; svec = "spliceable vector"
(def svec-types           '{:clj  #{clojure.core.rrb_vector.rrbt.Vector}
                            :cljs #{clojure.core.rrb_vector.rrbt.Vector}})
(def +vec-types            {:clj  '#{clojure.lang.IPersistentVector}
                            :cljs (set/union (:cljs svec-types)
                                             '#{cljs.core/PersistentVector})})
(def !+vec-types          '{:clj  #{clojure.lang.ITransientVector}
                            :cljs #{cljs.core/TransientVector}})
(def ?!+vec-types          (cond-union +vec-types !+vec-types))
(def !vec-types           '{:clj  #{java.util.ArrayList}
                            :cljs #{cljs.core.ArrayList}})
                           ; java.util.Vector is deprecated, because you can
                           ; just create a synchronized wrapper over an ArrayList
                           ; via java.util.Collections
(def !!vec-types           {})
(def vec-types             (cond-union ?!+vec-types !vec-types !!vec-types))

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
(def indexed-types         (cond-union array-types string-types vec-types
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
                             {:clj  (set/union (:clj !vec-types) (:clj !!vec-types)
                                               (:clj !map-types) (:clj !!map-types)
                                               (:clj !set-types) (:clj !!set-types)
                                               '#{clojure.lang.Counted})
                              :cljs (set/union (:cljs vec-types)
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

; ===== FUNCTIONS ===== ;

(def fn-types             `{:clj #{clojure.lang.Fn}  :cljs #{(type inc)}})
(def ifn-types            `{:clj #{clojure.lang.IFn} :cljs #{(type inc)}}) ; TODO keyword types?
(def multimethod-types    '{:clj #{clojure.lang.MultiFn}})

; ===== MISCELLANEOUS ===== ;

(def regex-types          '{:clj  #{java.util.regex.Pattern}
                            :cljs #{js/RegExp              }})

(def atom-types           '{:clj  #{clojure.lang.IAtom}
                            :cljs #{cljs.core/Atom}})
(def atomic-types          {:clj  (set/union (:clj atom-types)
                                    '#{clojure.lang.Volatile
                                       java.util.concurrent.atomic.AtomicReference
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

(def comparable-types      {:clj  (set/union '#{byte char short int long float double} '#{Comparable})
                            :cljs (:cljs number-types)})

(def record-types          '{:clj  #{clojure.lang.IRecord}
                             #_:cljs #_#{cljs.core/IRecord}}) ; because can't protocol-dispatch on protocols in CLJS

(def reducer-types         '{:clj #{#_clojure.core.protocols.CollReduce ; no, in order to find most specific type
                                    quantum.core.type.defs.Folder
                                    quantum.core.type.defs.Reducer}
                             :cljs #{#_cljs.core/IReduce ; CLJS problems with dispatching on interface
                                     quantum.core.type.defs.Folder
                                     quantum.core.type.defs.Reducer}})

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

; ===== PREDICATES ===== ;

(def types-0
  {; ----- PRIMITIVES ----- ;

   'primitive?       primitive-types
   'prim?            prim-types
   'boxed?           primitive-boxed-types
   'integral?        integral-types

   'char?            char-types
   'boolean?         bool-types
   'bool?            bool-types
   'byte?            byte-types
   'short?           short-types
   'int?             int-types
   ; The closest thing to a native int the platform has
   'nat-int?        `{:clj  #{~'int}
                      :cljs #{(type 123)}}
   'long?            long-types
   '?long            boxed-long-types
   ; The closest thing to a native long the platform has
   'nat-long?       `{:clj  #{~'long}
                      :cljs #{(type 123)}}
   'float?           float-types
   'double?          double-types
   '?double          boxed-double-types

   ; INTEGERS

   'integer?         integer-types
   'bigint?          bigint-types

   ; DECIMALS

   'decimal?         decimal-types
   'bigdec?          bigdec-types

   ; NUMBERS

   'ratio?           ratio-types

   'number?          number-types
   'num?             number-types
   'pnumber?         `{:cljs #{(type 123)}}
   'pnum?            `{:cljs #{(type 123)}}

   ; ===== COLLECTIONS ===== ;

   'coll?            coll-types

   'map-entry?       map-entry-types

   ; SEQUENTIAL

   'sequential?      sequential-types

   'cons?            cons-types
   'misc-seq?        misc-seq-types
   'lseq?            lseq-types
   'non-list-seq?    non-list-seq-types

   ; Denotes unindexed list particularly
   'dlist?           dlist-types
   'cdlist?          cdlist-types
   '+list?           +list-types
   '!+list?          '!+list-types
   '?!+list?         '?!+list-types
   '!list?           '!list-types
   '!!list?          '!!list-types
   'list?            list-types

   'seq?             seq-types

   ; ----- ASSOCIATIVE ----- ;

   'associative?     associative-types

   ; MAP

   '+array-map?      +array-map-types
   '!+array-map?     !+array-map-types
   '?!+array-map?    ?!+array-map-types
   '!array-map?      !array-map-types
   '!!array-map?     !!array-map-types
   'array-map?       array-map-types

   '+hash-map?       +hash-map-types
   '!+hash-map?      !+hash-map-types
   '?!+hash-map?     ?!+hash-map-types
   '!hash-map?       !hash-map-types
   '!!hash-map?      !!hash-map-types
   'hash-map?        hash-map-types

   '+unsorted-map?   +unsorted-map-types
   '!+unsorted-map?  !+unsorted-map-types
   '?!+unsorted-map? ?!+unsorted-map-types
   '!unsorted-map?   !unsorted-map-types
   '!!unsorted-map?  !!unsorted-map-types
   'unsorted-map?    unsorted-map-types

   '+sorted-map?     +sorted-map-types
   '!+sorted-map?    !+sorted-map-types
   '?!+sorted-map?   ?!+sorted-map-types
   '!sorted-map?     !sorted-map-types
   '!!sorted-map?    !!sorted-map-types
   'sorted-map?      sorted-map-types

   '+map?            +map-types
   '!+map?           !+map-types
   '?!+map?          ?!+map-types
   '!map?            !map-types
   '!!map?           !!map-types
   'map?             map-types

   ; SET

   '+hash-set?       +hash-set-types
   '!+hash-set?      !+hash-set-types
   '?!+hash-set?     ?!+hash-set-types
   '!hash-set?       !hash-set-types
   '!!hash-set?      !!hash-set-types
   'hash-set?        hash-set-types

   '+unsorted-set?   +unsorted-set-types
   '!+unsorted-set?  !+unsorted-set-types
   '?!+unsorted-set? ?!+unsorted-set-types
   '!unsorted-set?   !unsorted-set-types
   '!!unsorted-set?  !!unsorted-set-types
   'unsorted-set?    unsorted-set-types

   '+sorted-set?     +sorted-set-types
   '!+sorted-set?    !+sorted-set-types
   '?!+sorted-set?   ?!+sorted-set-types
   '!sorted-set?     !sorted-set-types
   '!!sorted-set?    !!sorted-set-types
   'sorted-set?      sorted-set-types

   '+set?            +set-types
   '!+set?           !+set-types
   '?!+set?          ?!+set-types
   '!set?            !set-types
   '!!+set?          !!set-types
   'set?             set-types

   ; INDEXED

   'indexed?         indexed-types

   ; SORTED

   'sorted?          sorted-types

   ; COUNTED

   'counted?         counted-types

   ; ----- ASSOCIATIVE+INDEXED ----- ;

   ; ARRAYS

   'boolean-array?       {:clj #{(-> array-1d-types :clj :boolean)}}
   'byte-array?          {:clj #{(-> array-1d-types :clj :byte   )} :cljs #{(-> array-1d-types :cljs :byte   )}}
   'ubyte-array?         {                                          :cljs #{(-> array-1d-types :cljs :ubyte  )}}
   'ubyte-array-clamped? {                                          :cljs #{(-> array-1d-types :cljs :ubyte-clamped)}}
   'char-array?          {:clj #{(-> array-1d-types :clj :char   )} :cljs #{(-> array-1d-types :cljs :char   )}}
   'short-array?         {:clj #{(-> array-1d-types :clj :short  )} :cljs #{(-> array-1d-types :cljs :short  )}}
   'ushort-array?        {                                          :cljs #{(-> array-1d-types :cljs :ushort )}}
   'int-array?           {:clj #{(-> array-1d-types :clj :int    )} :cljs #{(-> array-1d-types :cljs :int    )}}
   'uint-array?          {                                          :cljs #{(-> array-1d-types :cljs :uint  )}}
   'long-array?          {:clj #{(-> array-1d-types :clj :long   )} :cljs #{(-> array-1d-types :cljs :long   )}}
   'float-array?         {:clj #{(-> array-1d-types :clj :float  )} :cljs #{(-> array-1d-types :cljs :float  )}}
   'double-array?        {:clj #{(-> array-1d-types :clj :double )} :cljs #{(-> array-1d-types :cljs :double )}}
   'object-array?        {:clj #{(-> array-1d-types :clj :object )} :cljs #{(-> array-1d-types :cljs :object )}}

   'booleans?        {:clj #{(-> array-1d-types :clj :boolean)}}
   'bytes?           {:clj #{(-> array-1d-types :clj :byte   )} :cljs #{(-> array-1d-types :cljs :byte   )}}
   'ubytes?          {                                          :cljs #{(-> array-1d-types :cljs :ubyte  )}}
   'ubytes-clamped?  {                                          :cljs #{(-> array-1d-types :cljs :ubyte-clamped)}}
   'chars?           {:clj #{(-> array-1d-types :clj :char   )} :cljs #{(-> array-1d-types :cljs :char   )}}
   'shorts?          {:clj #{(-> array-1d-types :clj :short  )} :cljs #{(-> array-1d-types :cljs :short  )}}
   'ushorts?         {                                          :cljs #{(-> array-1d-types :cljs :ushort )}}
   'ints?            {:clj #{(-> array-1d-types :clj :int    )} :cljs #{(-> array-1d-types :cljs :int    )}}
   'uints?           {                                          :cljs #{(-> array-1d-types :cljs :uint  )}}
   'longs?           {:clj #{(-> array-1d-types :clj :long   )} :cljs #{(-> array-1d-types :cljs :long   )}}
   'floats?          {:clj #{(-> array-1d-types :clj :float  )} :cljs #{(-> array-1d-types :cljs :float  )}}
   'doubles?         {:clj #{(-> array-1d-types :clj :double )} :cljs #{(-> array-1d-types :cljs :double )}}
   'objects?         {:clj #{(-> array-1d-types :clj :object )} :cljs #{(-> array-1d-types :cljs :object )}}

   'doubles-2d?      {:clj #{(-> array-2d-types :clj :double )} :cljs #{(-> array-2d-types :cljs :double )}}
   'objects-2d?      {:clj #{(-> array-2d-types :clj :object )} :cljs #{(-> array-2d-types :cljs :object )}}

   'array-1d?        {:clj  (->> array-1d-types :clj  vals set)
                      :cljs (->> array-1d-types :cljs vals set)}

   'array-2d?        {:clj  (->> array-2d-types :clj  vals set)
                      :cljs (->> array-2d-types :cljs vals set)}
   'array-3d?        {:clj  (->> array-3d-types :clj  vals set)
                      :cljs (->> array-3d-types :cljs vals set)}
   'array-4d?        {:clj  (->> array-4d-types :clj  vals set)
                      :cljs (->> array-4d-types :cljs vals set)}
   'array-5d?        {:clj  (->> array-5d-types :clj  vals set)
                      :cljs (->> array-5d-types :cljs vals set)}
   'array-6d?        {:clj  (->> array-6d-types :clj  vals set)
                      :cljs (->> array-6d-types :cljs vals set)}
   'array-7d?        {:clj  (->> array-7d-types :clj  vals set)
                      :cljs (->> array-7d-types :cljs vals set)}
   'array-8d?        {:clj  (->> array-8d-types :clj  vals set)
                      :cljs (->> array-8d-types :cljs vals set)}
   'array-9d?        {:clj  (->> array-9d-types :clj  vals set)
                      :cljs (->> array-9d-types :cljs vals set)}
   'array-10d?       {:clj  (->> array-10d-types :clj  vals set)
                      :cljs (->> array-10d-types :cljs vals set)}
   'array?           array-types

   ; VECTOR-LIKE OBJECTS

   'string?          string-types
   '!string?         !string-types
   'char-seq?        {:clj '#{CharSequence}}

   '!array-list?     !array-list-types

   'svec?            svec-types
   '+vec?            +vec-types
   '!+vec?           !+vec-types
   '?!+vec?          ?!+vec-types
   '!vec?            !vec-types
   '!!vec?           !!vec-types
   'vec?             vec-types
   'tuple?           tuple-types

   ; QUEUES

   '+queue?          +queue-types
   '!+queue?         !+queue-types
   '?!+queue?        ?!+queue-types
   '!queue?          !queue-types
   '!!queue?         !!queue-types
   'queue?           queue-types

   ; MISCELLANEOUS

   'fn?              fn-types
   'ifn?             ifn-types
   'multimethod?     multimethod-types
   'nil?             '{:cljc #{nil}}

   'keyword?         '{:clj  #{clojure.lang.Keyword}
                       :cljs #{cljs.core/Keyword}}
   'symbol?          '{:clj  #{clojure.lang.Symbol}
                       :cljs #{cljs.core/Symbol}}

   'record?          record-types
   'transient?       transient-types
   'transientizable? transientizable-types
   'editable?        {:clj  '#{clojure.lang.IEditableCollection}
                      :cljs #_#{cljs.core/IEditableCollection} ; can't dispatch on a protocol
                            (set/union (get transientizable-types :cljc)
                                       (get transientizable-types :cljs))}
   'pattern?         regex-types
   'regex?           regex-types
   'reducer?         reducer-types
   ;'reducible?      reducible-types
   'file?            '{:clj  #{java.io.File}
                       :cljs #{#_js/File}} ; isn't always available! Use an abstraction
   'atom?            atom-types
   'atomic?          atomic-types
   'm2m-chan?        m2m-chan-types
   'chan?            chan-types
   'comparable?      comparable-types
   'any?             {:clj  (set/union (:clj prim-types) #{'java.lang.Object})
                      :cljs '#{(quote default)}}
   'default          {:clj  '#{Object}
                      :cljs '#{(quote default)}}})

; TODO make this extensible
#?(:clj
(defmacro def-types [lang]
  (let [unevaled-fn
          (fn [lang-n]
            (->> types-0
                 (map (fn [[pred types-n]]
                        (map-entry pred (set/union (:cljc types-n) (get types-n lang-n)))))
                 (remove (fn-> val empty?))
                 (into {})))
        langs #{:clj :cljs}
        unevaled
          (->> langs
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
               (into {}))
        lang-unevaled (unevaled-fn lang)
        code  `(do ~(list 'def 'types-unevaled `'~unevaled)
                   ~(list 'def 'types
                     `(zipmap    (keys '~lang-unevaled)
                              [~@(vals   lang-unevaled)])))]
    code)))
