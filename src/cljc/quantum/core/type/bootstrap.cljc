(ns
  ^{:doc "Some useful macros, like de-repetitivizing protocol extensions.
          Also some plumbing macros for |for| loops and the like."
    :attribution "Alex Gunnarson"}
  quantum.core.type.bootstrap
  (:require-quantum [ns log pr err map set logic fn]))

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
           {:clj   {'(class (short-array   0)      ) (symbol "[S")
                    '(class (long-array    0)      ) (symbol "[J")
                    '(class (float-array   0)      ) (symbol "[F")
                    '(class (int-array     0)      ) (symbol "[I")
                    '(class (double-array  0.0)    ) (symbol "[D")
                    '(class (boolean-array [false])) (symbol "[Z")
                    '(class (byte-array    0)      ) (symbol "[B")
                    '(class (char-array    "")     ) (symbol "[C")
                    '(class (object-array  [])     ) (symbol "[Ljava.lang.Object;")}
            :cljs '{ (class ""                     ) string
                     (class 123                    ) number
                     (class (clj->js {})           ) object
                     (class true                   ) boolean
                     (class (array)                ) array
                     (class inc                    ) function}}

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
           :cljs #{cljs.core.PersistentTreeMap   }}
        map-types
          (cond-union
            hash-map-types 
            array-map-types
            tree-map-types
            #_'{:clj #{clojure.lang.IPersistentMap ; For records
                     java.util.Map}})
        array-list-types
         '{:clj  #{java.util.ArrayList java.util.Arrays$ArrayList}
           :cljs #{cljs.core.ArrayList                           }}
        array-types
         '{:clj  {:short   (class (short-array   0)      )
                  :long    (class (long-array    0)      )
                  :float   (class (float-array   0)      )
                  :int     (class (int-array     0)      )
                  :double  (class (double-array  0.0)    )
                  :boolean (class (boolean-array [false]))
                  :byte    (class (byte-array    0)      )
                  :char    (class (char-array    "")     )
                  :object  (class (object-array  [])     )}
           :cljs #{(class (array))}}
        number-types
         '{:clj  #{java.lang.Long}}
        set-types
         '{:clj  #{clojure.lang.APersistentSet
                   clojure.lang.IPersistentSet}
           :cljs #{cljs.core/PersistentHashSet
                   cljs.core/TransientHashSet
                   cljs.core/PersistentTreeSet}}
        vec-types
         '{:clj  #{clojure.lang.APersistentVector
                   clojure.lang.PersistentVector
                   clojure.lang.APersistentVector$RSeq}
           :cljs #{cljs.core/PersistentVector
                   cljs.core/TransientVector}}
        list-types
         '{:clj  #{;java.util.List ; Because otherwise vectors get handled that same way
                   clojure.lang.PersistentList
                   clojure.lang.PersistentList$EmptyList}
           :cljs #{cljs.core/List cljs.core/EmptyList}}
        dlist-types
          '{:clj #{clojure.data.finger_tree.CountedDoubleList}}
        map-entry-types '{:clj  #{clojure.lang.MapEntry            }}
        queue-types     '{:clj  #{clojure.lang.PersistentQueue     }
                          :cljs #{cljs.core.PersistentQueue        }}
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
           :cljs #{cljs.core.Cons}}
        lseq-types '{:clj  #{clojure.lang.LazySeq}
                     :cljs #{cljs.core.LazySeq   }}
        seq-types            (cond-union
                               cons-types
                               list-types
                               dlist-types
                               queue-types
                               lseq-types
                               '{:clj  #{clojure.lang.APersistentMap$ValSeq
                                         clojure.lang.APersistentMap$KeySeq
                                         clojure.lang.PersistentVector$ChunkedSeq
                                         clojure.lang.IndexedSeq}
                                 :cljs #{cljs.core/ValSeq
                                         cljs.core/KeySeq
                                         cljs.core/IndexedSeq
                                         cljs.core/ChunkedSeq}})
        listy-types           seq-types
        indexed-types         vec-types
        bool-types            '{:clj  #{java.lang.Boolean}
                                :cljs #{(class true)}}
        char-types            '{:clj  #{java.lang.Character}}
        short-types           '{:clj  #{java.lang.Short     }}
        int-types             '{:clj  #{java.lang.Integer   }}
        long-types            '{:clj  #{java.lang.Long      }}
        bigint-types          '{:clj  #{clojure.lang.BigInt java.math.BigInteger}}
        integer-types         (cond-union short-types int-types
                                long-types bigint-types)
        float-types           '{:clj #{java.lang.Float     }}
        double-types          '{:clj #{java.lang.Double    }}
        bigdec-types          '{:clj #{java.math.BigDecimal}}
        transientizable-types '{:clj
                                  #{clojure.lang.PersistentArrayMap
                                    clojure.lang.PersistentHashMap
                                    clojure.lang.PersistentHashSet
                                    clojure.lang.PersistentVector}
                                :cljs
                                  #{cljs.core/PersistentArrayMap
                                    cljs.core/PersistentHashMap
                                    cljs.core/PersistentHashSet
                                    cljs.core/PersistentVector}}
        decimal-types         (cond-union
                                float-types double-types bigdec-types)
        number-types          (cond-union integer-types decimal-types
                                '{:clj  #{java.lang.Number}
                                  :cljs #{(class 123)     }})
        integral-types        (cond-union bool-types char-types number-types)
        string-types          '{:clj #{String} :cljs #{(class "")}}
        primitive-types       (cond-union integral-types '{:cljs #{(class "")}})
        fn-types              '{:clj #{clojure.lang.Fn} :cljs #{(class inc)}}
        multimethod-types     '{:clj #{clojure.lang.MultiFn}}
        coll-types            (cond-union seq-types associative-types
                                array-list-types)
        atom-types            '{:clj  #{clojure.lang.IAtom}
                                :cljs #{cljs.core/Atom}}
        types-0
          {'char?            char-types
           'boolean?         bool-types
           'bool?            bool-types
           'number?          number-types
           'num?             number-types
           'tree-map?        tree-map-types
           'sorted-map?      tree-map-types
           'map?             map-types
           'array-map?       array-map-types
           'map-entry?       map-entry-types
           'set?             set-types
           'vec?             vec-types
           'vector?          vec-types
           'list?            list-types
           'dlist?           dlist-types
           'listy?           listy-types
           'cons?            cons-types
           'associative?     associative-types
           'lseq?            lseq-types
           'seq?             seq-types
           'queue?           queue-types
           'coll?            coll-types
           'indexed?         indexed-types
           'fn?              fn-types
           'multimethod?     multimethod-types
           'nil?             '{:cljc #{nil}}
           'string?          string-types
           'symbol?          '{:cljc #{Symbol}}
           'record?          '{:clj  #{clojure.lang.IRecord}
                               :cljs #{cljs.core.IRecord}}
           'short?           short-types
           'int?             int-types
           'long?            long-types
           'bigint?          bigint-types
           'integer?         integer-types
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
           'file?            '{:clj #{java.io.File}}
           'array-list?      array-list-types
           'array?           {:clj  (->> array-types :clj vals (into #{}))
                              :cljs (->> array-types :cljs)}
           'object-array?    {:clj #{(-> array-types :clj :object)}}
           'byte-array?      {:clj #{(-> array-types :clj :byte  )}}
           'long-array?      {:clj #{(-> array-types :clj :long  )}}
           'keyword?         '{:clj  #{clojure.lang.Keyword}
                               :cljs #{cljs.core/Keyword}}
           'atom?            atom-types
           :default          '{:clj  #{Object nil}
                               :cljs #{(quote default)}}}
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
                                                   (fn-and seq? (fn-> first name (= "class")))
                                                     (fn [obj]
                                                       (condp = lang
                                                         :cljs (get-in primitive-type-map [lang-n obj])
                                                         :clj  (get-in primitive-type-map [lang-n obj])
                                                         obj))
                                                   (fn-and seq? (fn-> first name (= "quote")))
                                                     second
                                                   :else identity))
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
         #_(log/enable! :macro-expand)
    #_(log/ppr :macro-expand "DEF-TYPES CODE" code)  #_(log/disable! :macro-expand)
    code)))
