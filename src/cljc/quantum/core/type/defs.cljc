(ns
  ^{:doc "Some useful macros, like de-repetitivizing protocol extensions.
          Also some plumbing macros for |for| loops and the like."
    :attribution "Alex Gunnarson"}
  quantum.core.type.defs
           (:require [#?(:clj  clojure.core
                         :cljs cljs.core   )               :as core ]
                     [quantum.core.data.map                :as map
                       :refer [map-entry]                           ]
                     [quantum.core.data.set                :as set  ]
                     [quantum.core.fn                      :as fn
                       :refer [#?@(:clj [fn->])]                    ]
                     [quantum.core.logic                   :as logic
                       :refer [#?@(:clj [fn-and condf*n])]          ])
  #?(:cljs (:require-macros
                     [quantum.core.fn                      :as fn
                       :refer [fn->]                                ]
                     [quantum.core.logic                   :as logic
                       :refer [fn-and condf*n]                      ])))

(def ^{:doc "Could do <Class>/MAX_VALUE for the maxes vin Java but JS doesn't like it of course
             In JavaScript, all numbers are 64-bit floating point numbers.
             This means you can't represent in JavaScript all the Java longs
             Max 'safe' int: (dec (Math/pow 2 53))"}
  type-meta
  {'boolean {:bits 1
             :min  0
             :max  1
             #?@(:clj [:outer-type "[Z"
                       :boxed      'java.lang.Boolean])}
   'short   {:bits 16
             :min -32768
             :max  32767
             #?@(:clj [:outer-type "[S"
                       :boxed      'java.lang.Short])}
   'byte    {:bits 8
             :min -128
             :max  127
             #?@(:clj [:outer-type "[B"
                       :boxed      'java.lang.Byte])}
   'char    {:bits 16
             :min  0
             :max  65535
             #?@(:clj [:outer-type "[C"
                       :boxed      'java.lang.Character])}
   'int     {:bits 32
             :min -2147483648
             :max  2147483647
             #?@(:clj [:outer-type "[I"
                       :boxed      'java.lang.Integer])}
   'long    {:bits 64
             :min -9223372036854775808
             :max  9223372036854775807
             #?@(:clj [:outer-type "[J"
                       :boxed      'java.lang.Long])}
   ; Technically with floating-point nums, "min" isn't the most negative;
   ; it's the smallest absolute
   'float   {:bits 32
             :min  1.4E-45
             :max  3.4028235E38
             #?@(:clj [:outer-type "[F"
                       :boxed      'java.lang.Float])}
   'double  {:bits 64
             ; Because:
             ; Double/MIN_VALUE        = 4.9E-324
             ; (.-MIN_VALUE js/Number) = 5e-324
             :min  #?(:clj  Double/MIN_VALUE
                      :cljs (.-MIN_VALUE js/Number))
             :max  1.7976931348623157E308 ; Max number in JS
             #?@(:clj [:outer-type "[D"
                       :boxed      'java.lang.Double])}}) 

#?(:clj
(def inner-types
  (->> type-meta
       (map (fn [[k v]] [(:outer-type v) k]))
       (reduce
         (fn [m [k v]]
           (assoc m k v (symbol k) v))
         {}))))

#?(:clj
(def boxed-types
  (->> type-meta
       (map (fn [[k v]] [k (:boxed v)]))
       (into {}))))

#?(:clj
(def unboxed-types
  (zipmap (vals boxed-types) (keys boxed-types))))

(def max-values
  (->> type-meta
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

(defn inner-type
  {:todo ["Handle object arrays and multi-dimensional arrays"
          "Throw exception if called on an integral ('uncuttable') type"]}
  [type]
  (or #?(:clj (get inner-types type) :cljs 'object) 'Object))

#?(:clj (def class->str (fn-> str (.substring 6))))

#?(:clj
(defmacro array-nd-types [n]
  `#{`(type (apply make-array Short/TYPE     (repeat ~~n 0)))
     `(type (apply make-array Long/TYPE      (repeat ~~n 0)))
     `(type (apply make-array Float/TYPE     (repeat ~~n 0)))
     `(type (apply make-array Integer/TYPE   (repeat ~~n 0)))
     `(type (apply make-array Double/TYPE    (repeat ~~n 0)))
     `(type (apply make-array Boolean/TYPE   (repeat ~~n 0)))
     `(type (apply make-array Byte/TYPE      (repeat ~~n 0)))
     `(type (apply make-array Character/TYPE (repeat ~~n 0)))
     `(type (apply make-array Object         (repeat ~~n 0)))}))

#?(:clj
(defmacro def-types [lang]
  (let [retrieve
          (fn [lang-n sets] (->> sets (map lang-n) (remove empty?) (apply set/union)))
        cond-union
          (fn [& sets]
            {:cljc (retrieve :cljc sets)
             :clj  (retrieve :clj  sets)
             :cljs (retrieve :cljs sets)})
        primitive-type-map
           {:clj {'(type (boolean-array [false])) (symbol "[Z")
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
                    (type inc                    ) ~'function}}

        hash-map-types
         '{:clj  #{clojure.lang.PersistentHashMap
                   clojure.lang.PersistentHashMap$TransientHashMap}
           :cljs #{cljs.core/PersistentHashMap 
                   cljs.core/TransientHashMap}}
        array-map-types 
         '{:clj  #{clojure.lang.PersistentArrayMap
                   clojure.lang.PersistentArrayMap$TransientArrayMap}
           :cljs #{cljs.core/PersistentArrayMap
                   cljs.core/TransientArrayMap}}
        tree-map-types
         '{:clj  #{clojure.lang.PersistentTreeMap}
           :cljs #{cljs.core/PersistentTreeMap   }}
        map-types
          (cond-union
            hash-map-types 
            array-map-types
            tree-map-types
            '{:clj #{flatland.ordered.map.OrderedMap}}
            #_'{:clj #{clojure.lang.IPersistentMap ; For records
                       java.util.Map}})
        array-list-types
         '{:clj  #{java.util.ArrayList java.util.Arrays$ArrayList}
           :cljs #{cljs.core.ArrayList                           }
         }
        array-types
         `{:clj  {:short   (type (short-array   0)      )
                  :long    (type (long-array    0)      )
                  :float   (type (float-array   0)      )
                  :int     (type (int-array     0)      )
                  :double  (type (double-array  0.0)    )
                  :boolean (type (boolean-array [false]))
                  :byte    (type (byte-array    0)      )
                  :char    (type (char-array    "")     )
                  :object  (type (object-array  [])     )}
           :cljs #{(type (cljs.core/array))}}
        array-2d-types  {:clj (array-nd-types 2 )}
        array-3d-types  {:clj (array-nd-types 3 )}
        array-4d-types  {:clj (array-nd-types 4 )}
        array-5d-types  {:clj (array-nd-types 5 )}
        array-6d-types  {:clj (array-nd-types 6 )}
        array-7d-types  {:clj (array-nd-types 7 )}
        array-8d-types  {:clj (array-nd-types 8 )}
        array-9d-types  {:clj (array-nd-types 9 )}
        array-10d-types {:clj (array-nd-types 10)}
        any-array-types (cond-union (->> array-types vals (into #{}))
                                    array-2d-types 
                                    array-3d-types 
                                    array-4d-types 
                                    array-5d-types 
                                    array-6d-types 
                                    array-7d-types 
                                    array-8d-types 
                                    array-9d-types 
                                    array-10d-types)
        number-types
         '{:clj  #{java.lang.Long}}
        hash-set-types
         '{:clj  #{clojure.lang.PersistentHashSet
                   clojure.lang.PersistentHashSet$TransientHashSet}
           :cljs #{cljs.core/PersistentHashSet
                   cljs.core/TransientHashSet}}
        tree-set-types
         '{:clj  #{clojure.lang.PersistentTreeSet}
           :cljs #{cljs.core/PersistentTreeSet   }}
        set-types
          {:clj  '#{clojure.lang.APersistentSet
                    clojure.lang.IPersistentSet}
           :cljs (set/union (:cljs hash-set-types)
                            (:cljs tree-set-types))}
        tuple-types
          '{:clj #{clojure.lang.Tuple}}
        vec-types
         (cond-union tuple-types
            '{:clj  #{clojure.lang.APersistentVector
                      clojure.lang.PersistentVector
                      clojure.lang.APersistentVector$RSeq
                      clojure.core.rrb_vector.rrbt.Vector}
              :cljs #{cljs.core/PersistentVector
                      cljs.core/TransientVector
                      clojure.core.rrb_vector.rrbt.Vector}})
        list-types
         '{:clj  #{;java.util.List ; Because otherwise vectors get handled that same way
                   clojure.lang.PersistentList
                   clojure.lang.PersistentList$EmptyList}
           :cljs #{cljs.core/List cljs.core/EmptyList}}
        dlist-types
           {}
          #_'{:clj  #{clojure.data.finger_tree.CountedDoubleList
                    quantum.core.data.finger_tree.CountedDoubleList}
            :cljs #{quantum.core.data.finger-tree/CountedDoubleList}}
        cdlist-types
          {}
          #_'{:clj  #{clojure.data.finger_tree.CountedDoubleList
                    quantum.core.data.finger_tree.CountedDoubleList}
            :cljs #{quantum.core.data.finger-tree/CountedDoubleList}}
        map-entry-types '{:clj  #{clojure.lang.MapEntry            }}
        queue-types     '{:clj  #{clojure.lang.PersistentQueue     }
                          :cljs #{cljs.core/PersistentQueue        }}
        transient-types '{:clj  #{clojure.lang.ITransientCollection}
                          :cljs #{#_cljs.core/ITransientCollection ; Problems with this
                                  cljs.core/TransientVector
                                  cljs.core/TransientHashSet
                                  cljs.core/TransientArrayMap
                                  cljs.core/TransientHashMap}}
        regex-types     '{:clj  #{java.util.regex.Pattern          }
                          :cljs #{js/RegExp                        }}
        associative-types
          (cond-union map-types set-types vec-types)
        cons-types
         '{:clj  #{clojure.lang.Cons}
           :cljs #{cljs.core/Cons}}
        lseq-types '{:clj  #{clojure.lang.LazySeq}
                     :cljs #{cljs.core/LazySeq   }}
        misc-seq-types '{:clj  #{clojure.lang.APersistentMap$ValSeq
                                 clojure.lang.APersistentMap$KeySeq
                                 clojure.lang.PersistentVector$ChunkedSeq
                                 clojure.lang.IndexedSeq}
                         :cljs #{cljs.core/ValSeq
                                 cljs.core/KeySeq
                                 cljs.core/IndexedSeq
                                 cljs.core/ChunkedSeq}}
        seq-types       (cond-union
                           cons-types
                           list-types
                           dlist-types
                           queue-types
                           lseq-types
                           misc-seq-types)
        listy-types           seq-types
        seq-not-list-types   (cond-union cons-types queue-types
                               lseq-types
                               misc-seq-types)
        indexed-types         vec-types
        prim-bool-types       '{:clj  #{boolean}}
        bool-types            `{:clj  #{~'boolean java.lang.Boolean}
                                :cljs #{(type true)}}
        prim-byte-types       '{:clj  #{byte}}
        byte-types            '{:clj  #{byte  java.lang.Byte}}
        prim-char-types       '{:clj  #{char}}
        char-types            '{:clj  #{char  java.lang.Character}}
        prim-short-types      '{:clj  #{short}}
        short-types           '{:clj  #{short java.lang.Short     }}
        prim-int-types        '{:clj  #{int}}
        int-types             '{:clj  #{int   java.lang.Integer   }}
        prim-long-types       '{:clj  #{long}}
        long-types            '{:clj  #{long  java.lang.Long      }}
        bigint-types          '{:clj  #{clojure.lang.BigInt java.math.BigInteger}}
        integer-types         (cond-union short-types int-types
                                long-types bigint-types)
        prim-float-types      '{:clj #{float}}
        float-types           '{:clj #{float  java.lang.Float }}
        prim-double-types     '{:clj #{double}}
        double-types          '{:clj #{double java.lang.Double}}
        bigdec-types          '{:clj #{java.math.BigDecimal}}
        transientizable-types (cond-union tuple-types
                                '{:clj
                                    #{clojure.lang.PersistentArrayMap
                                      clojure.lang.PersistentHashMap
                                      clojure.lang.PersistentHashSet
                                      clojure.lang.PersistentVector}
                                  :cljs
                                    #{cljs.core/PersistentArrayMap
                                      cljs.core/PersistentHashMap
                                      cljs.core/PersistentHashSet
                                      cljs.core/PersistentVector}})
        decimal-types         (cond-union
                                float-types double-types bigdec-types)
        number-types          (cond-union integer-types decimal-types
                                `{:clj  #{java.lang.Number}
                                  :cljs #{(type 123)     }})
        integral-types        (cond-union bool-types char-types number-types)
        string-types          `{:clj #{String} :cljs #{(type "")}}
        primitive-types       (cond-union prim-bool-types prim-byte-types prim-char-types
                                prim-short-types prim-int-types prim-long-types
                                prim-float-types prim-double-types
                                `{:cljs #{(type "")}})
        fn-types              `{:clj #{clojure.lang.Fn} :cljs #{(type inc)}}
        multimethod-types     '{:clj #{clojure.lang.MultiFn}}
        coll-types            (cond-union seq-types associative-types
                                array-list-types)
        atom-types            '{:clj  #{clojure.lang.IAtom}
                                :cljs #{cljs.core/Atom}}
        types-0
          {'number?          number-types
           'num?             number-types
           'tree-map?        tree-map-types
           'sorted-map?      tree-map-types
           'map?             map-types
           'hash-map?        hash-map-types
           'array-map?       array-map-types
           'map-entry?       map-entry-types
           'set?             set-types
           'hash-set?        hash-set-types
           'tree-set?        tree-set-types
           'sorted-set?      tree-set-types
           'vec?             vec-types
           'vector?          vec-types
           'list?            list-types
           'dlist?           dlist-types
           'cdlist?          cdlist-types
           'listy?           listy-types
           'cons?            cons-types
           'misc-seq?        misc-seq-types
           'associative?     associative-types
           'lseq?            lseq-types
           'seq?             seq-types
           'seq-not-list?    seq-not-list-types
           'queue?           queue-types
           'coll?            coll-types
           'indexed?         indexed-types
           'fn?              fn-types
           'multimethod?     multimethod-types
           'nil?             '{:cljc #{nil}}
           'string?          string-types
           'symbol?          '{:clj  #{clojure.lang.Symbol}
                               :cljs #{cljs.core/Symbol}}
           'record?          '{:clj  #{clojure.lang.IRecord}
                               :cljs #{cljs.core/IRecord}}
           'char?            char-types
           'boolean?         bool-types
           'bool?            bool-types
           'byte?            byte-types
           'short?           short-types
           'int?             int-types
           'integer?         integer-types
           'pinteger?        `{:clj  #{~'long}
                               :cljs #{(type 123)}} 
           'long?            long-types
           'bigint?          bigint-types
           'float?           float-types
           'double?          double-types
           'decimal?         decimal-types
           'transient?       transient-types
           'transientizable? transientizable-types
           'editable?        {:clj  '#{clojure.lang.IEditableCollection}
                              :cljs #_#{cljs.core/IEditableCollection} ; problems with this
                                    (set/union (get transientizable-types :cljc)
                                               (get transientizable-types :cljs))}
           'pattern?         regex-types
           'regex?           regex-types
           'integral?        integral-types
           'primitive?       primitive-types
           'qreducer?        '{:clj #{clojure.core.protocols.CollReduce
                                      clojure.lang.Delay
                                      #_quantum.core.reducers.Folder}
                               :cljs #{#_cljs.core/IReduce ; CLJS problems with dispatching on interface 
                                       cljs.core/Delay}}
           'file?            '{:clj  #{java.io.File}
                               :cljs #{js/File}}
           'array-list?      array-list-types
           'array?           {:clj  (->> array-types :clj vals (into #{}))
                              :cljs (->> array-types :cljs)}

           'boolean-array?   {:clj #{(-> array-types :clj :boolean)}}
           'byte-array?      {:clj #{(-> array-types :clj :byte)}}
           'char-array?      {:clj #{(-> array-types :clj :char)}}
           'short-array?     {:clj #{(-> array-types :clj :short)}}
           'int-array?       {:clj #{(-> array-types :clj :int)}}
           'long-array?      {:clj #{(-> array-types :clj :long)}}
           'float-array?     {:clj #{(-> array-types :clj :float)}}
           'double-array?    {:clj #{(-> array-types :clj :double)}}
           'object-array?    {:clj #{(-> array-types :clj :object)}}
           
           'booleans?        {:clj #{(-> array-types :clj :boolean)}}
           'bytes?           {:clj #{(-> array-types :clj :byte)}}
           'chars?           {:clj #{(-> array-types :clj :char)}}
           'shorts?          {:clj #{(-> array-types :clj :short)}}
           'ints?            {:clj #{(-> array-types :clj :int)}}
           'longs?           {:clj #{(-> array-types :clj :long)}}
           'floats?          {:clj #{(-> array-types :clj :float)}}
           'doubles?         {:clj #{(-> array-types :clj :double)}}
           'objects?         {:clj #{(-> array-types :clj :object)}}

           'array-2d?        array-2d-types     
           'array-3d?        array-3d-types  
           'array-4d?        array-4d-types  
           'array-5d?        array-5d-types  
           'array-6d?        array-6d-types  
           'array-7d?        array-7d-types  
           'array-8d?        array-8d-types  
           'array-9d?        array-9d-types  
           'array-10d?       array-10d-types
           'any-array?       any-array-types

           'keyword?         '{:clj  #{clojure.lang.Keyword}
                               :cljs #{cljs.core/Keyword}}
           'atom?            atom-types
           :any              {:clj  (set/union (:clj primitive-types) #{'Object})
                              :cljs '#{(quote default)}}
           :obj              {:clj  '#{Object}
                              :cljs '#{(quote default)}}}
         unevaled-fn
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
                                            (map (condf*n
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
                               [~@(vals   lang-unevaled)]))
                    ~(list 'def 'primitive-types 
                      `(zipmap [~@(->   primitive-type-map (get  lang) keys)]
                                  (-> '~primitive-type-map (get ~lang) vals)))
                    ~(list 'def 'arr-types (get array-types lang)))]
    code)))
