#?(:clj (ns quantum.core.type))

(ns
  ^{:doc "Type-checking predicates, 'transientization' checks, class aliases, etc."
    :attribution "Alex Gunnarson"}
  quantum.core.type
  (:require
    [quantum.core.ns :as ns :refer
      #?(:clj  [alias-ns defalias
                ANil ABool ADouble ANum AExactNum AInt ADecimal AKey AVec ASet
                AArrList ATreeMap ALSeq ARegex AEditable ATransient AQueue AMap AError]
         :cljs [Exception IllegalArgumentException
                Nil Bool Num ExactNum Int Decimal Key Vec Set
                ArrList TreeMap LSeq Regex Editable Transient Queue Map
  
                ANil ABool ADouble ANum AExactNum AInt ADecimal AKey AVec ASet
                AArrList ATreeMap ALSeq ARegex AEditable ATransient AQueue AMap AError])]
    [quantum.core.logic :as log :refer
      #?(:clj  [splice-or fn-and fn-or fn-not ifn if*n whenc whenf whenf*n whencf*n condf condf*n]
         :cljs [splice-or fn-and fn-or fn-not])
      #?@(:cljs [:refer-macros [ifn if*n whenc whenf whenf*n whencf*n condf condf*n]])]
    [quantum.core.function :as fn :refer
      #?@(:clj  [[compr f*n fn* unary fn->> fn-> <- jfn]]
          :cljs [[compr f*n fn* unary]
                 :refer-macros [fn->> fn-> <-]])]
    [quantum.core.data.set :as set])
  #?@(:clj
      [(:import
        clojure.core.Vec
        (quantum.core.ns
          Nil Bool Num ExactNum Int Decimal Key Set
                 ArrList TreeMap LSeq Regex Editable Transient Queue Map))
       (:gen-class)]))

#?(:clj (set! *warn-on-reflection* true))

; TODO: Should include typecasting? (/cast/)

#?(:cljs (def class type))

#?(:clj (def instance+? instance?)
   :cljs
     (defn instance+? [class-0 obj] ; inline this?
       (try
         (instance? class-0 obj)
         (catch js/TypeError _
           (try (satisfies? class-0 obj))))))

#?(:clj (def ByteArray (type (byte-array 0))))

; ====== TYPE PREDICATES ======

        (def double?     #?(:clj  (partial instance+? ADouble)
                            :cljs (fn-and ; TODO: probably a better way of finding out if it's a double/decimal
                                     number?
                                     (fn-> str (.indexOf ".") (not= -1))))) ; has decimal point
        (def boolean?    #?(:clj  (partial instance? ABool)
                            :cljs (fn-or true? false?)))
#?(:clj (def bigint?     (partial instance+? clojure.lang.BigInt)))
; Unable to resolve symbol: isArray in this context
; http://stackoverflow.com/questions/2725533/how-to-see-if-an-object-is-an-array-without-using-reflection
; http://stackoverflow.com/questions/219881/java-array-reflection-isarray-vs-instanceof
;#?(:clj (def array?      (compr type (jfn java.lang.Class/isArray)))) ; uses reflection...
#?(:clj (def array?      (fn-> type .isArray)))
#?(:clj (def byte-array? (partial instance+? ByteArray)))
#?(:clj (def indexed?    (partial instance+? clojure.lang.Indexed)))
        (def array-list? (f*n splice-or #(instance+? %2 %1)
                                         AArrList
                                         #?(:clj java.util.Arrays$ArrayList)))
        (def map-entry?  #?(:clj  (partial instance+? clojure.lang.MapEntry)
                            :cljs (fn-and vector? (fn-> count (= 2)))))
        (def sorted-map? (partial instance+? ATreeMap))
        (def queue?      (partial instance+? AQueue))
        (def lseq?       (partial instance+? ALSeq))
        (def cons?       (partial instance? #?(:clj clojure.lang.Cons :cljs cljs.core.Cons)))
        (def listy?      (fn-or list? cons? lseq?))
        (def coll+?      (fn-or coll? array-list?))
        (def pattern?    (partial instance+? ARegex))
        (def regex?      pattern?)
        (def editable?   (partial instance+? AEditable))
        (def transient?  (partial instance+? ATransient))
#?(:clj (def throwable?  (partial instance+? java.lang.Throwable)))
        (def error?      (partial instance+? AError))
#?(:clj (def file?       (partial instance? java.io.File)))

(defn name-from-class
  [class-0]
  (let [^String class-str (str class-0)]
    (-> class-str
        (subs (-> class-str (.indexOf " ") inc))
        symbol)))

; ======= TRANSIENTS =======

; TODO this is just intuition. Better to |bench| it
; TODO move these vars
(def transient-threshold 3)
; macro because it will probably be heavily used  
#?(:clj
(defmacro should-transientize? [coll]
  `(and (editable? ~coll)
        (counted?  ~coll)
        (-> ~coll count (> transient-threshold)))))


(def types
  (let [map-types
          #{#?@(:clj  [clojure.lang.IPersistentMap
                       clojure.lang.PersistentArrayMap
                       ;clojure.lang.TransientArrayMap
                       clojure.lang.PersistentHashMap
                       ;clojure.lang.TransientHashMap 
                       clojure.lang.PersistentTreeMap
                       java.util.Map]
                :cljs [cljs.core/PersistentArrayMap
                       cljs.core/TransientArrayMap 
                       cljs.core/PersistentHashMap 
                       cljs.core/TransientHashMap  
                       cljs.core/PersistentTreeMap])}
        set-types
          #{#?@(:cljs [cljs.core/PersistentHashSet
                       cljs.core/TransientHashSet
                       cljs.core/PersistentTreeSet])}
        vec-types
          #{#?@(:clj  [clojure.lang.IPersistentVector
                       clojure.lang.PersistentVector]
                :cljs [cljs.core/PersistentVector
                       cljs.core/TransientVector])}
        list-types
          #{#?@(:clj  [java.util.List
                       clojure.lang.PersistentList]
                :cljs [cljs.core/List])}
        queue-types
          #{#?@(:clj  [java.util.List
                       clojure.lang.PersistentList]
                :cljs [cljs.core/PersistentQueue])}
        associative-types
          (set/union map-types set-types vec-types)
        seq-types
          (set/union
            list-types
            queue-types
            #{#?@(:clj  [clojure.lang.LazySeq]
                  :cljs [cljs.core/ValSeq
                         cljs.core/KeySeq
                         cljs.core/LazySeq
                         cljs.core/IndexedSeq
                         cljs.core/ChunkedSeq
                         cljs.core/ArrayList])})
        listy-types seq-types]
    {          'map?         map-types
               'set?         set-types
               'vec?         vec-types
               'vector?      vec-types
               'list?        list-types
               'associative? associative-types
               'seq?         seq-types
               'queue?       queue-types
               'nil?         #{nil}
     #?@(:clj ['pattern?     #{ARegex}])
     #?@(:clj ['array-list?  #{java.util.ArrayList}])
               'string?      #{#?(:clj String :cljs string)}
               ; 'reducer?     #{#?@(:clj [clojure.core.protocols.CollReduce
               ;                           quantum.core.reducers.Folder])}
     #?@(:clj ['byte-array?  #{(class (byte-array 0))}])
               'keyword?     #{#?(:clj clojure.lang.Keyword :cljs cljs.core/Keyword)}
               :default      #{:default}}))
  
#?(:clj
  (def arr-types
    {:short    (type (short-array   0)      )
     :long     (type (long-array    0)      )
     :float    (type (float-array   0)      )
     :int      (type (int-array     0)      )
     :double   (type (double-array  0.0)    )
     :boolean  (type (boolean-array [false]))
     :byte     (type (byte-array    0)      )
     :char     (type (char-array    "")     )
     :object   (type (object-array  [])     )}))
