(ns
  ^{:doc "Type-checking predicates, 'transientization' checks, class aliases, etc."
    :attribution "Alex Gunnarson"}
  quantum.core.type
  (:require
    [quantum.core.ns :as ns :refer
      #+clj [alias-ns defalias
             ANil ABool ANum AExactNum AInt ADecimal AKey AVec ASet
             AArrList ATreeMap ALSeq ARegex AEditable ATransient AQueue AMap AError]
      #+cljs [Exception IllegalArgumentException
              Nil Bool Num ExactNum Int Decimal Key Vec Set
              ArrList TreeMap LSeq Regex Editable Transient Queue Map

              ANil ABool ANum AExactNum AInt ADecimal AKey AVec ASet
              AArrList ATreeMap ALSeq ARegex AEditable ATransient AQueue AMap AError]]
    [quantum.core.logic :as log :refer
      #+clj  [splice-or fn-and fn-or fn-not ifn if*n whenc whenf whenf*n whencf*n condf condf*n]
      #+cljs [splice-or fn-and fn-or fn-not]
      #+cljs :refer-macros
      #+cljs [ifn if*n whenc whenf whenf*n whencf*n condf condf*n]]
    [quantum.core.function :as fn :refer
      #+clj  [compr f*n fn* unary fn->> fn-> <- jfn]
      #+cljs [compr f*n fn* unary]
      #+cljs :refer-macros
      #+cljs [fn->> fn-> <-]])
  #+clj
  (:import
    (quantum.core.ns
      Nil Bool Num ExactNum Int Decimal Key Set
             ArrList TreeMap LSeq Regex Editable Transient Queue Map)
    clojure.core.Vec)
  #+clj (:gen-class))

#+clj (set! *warn-on-reflection* true)

; TODO: Should include typecasting? (/cast/)

#+cljs (def class type)

#+clj  (def instance+? instance?)
#+cljs
(defn instance+? [class-0 obj] ; inline this?
  (try
    (instance? class-0 obj)
    (catch js/TypeError _
      (try (satisfies? class-0 obj)))))

#+clj (def ByteArray (type (byte-array 0)))

; ====== TYPE PREDICATES ======

      (def double?     #+clj  (partial instance+? ADouble)
                       #+cljs (fn-and ; TODO: probably a better way of finding out if it's a double/decimal
                                number?
                                (fn-> str (.indexOf ".") (not= -1)))) ; has decimal point
      (def boolean?    #+clj  (partial instance? ABoolean)
                       #+cljs (fn-or true? false?))
#+clj (def bigint?     (partial instance+? clojure.lang.BigInt))
#+clj (def array?      (compr type (jfn isArray))) ; getClass() shouldn't really be a slow call
#+clj (def byte-array? (partial instance+? ByteArray))
      (def indexed?    (partial instance+? clojure.lang.Indexed cljs.core/IIndexed)) 
      (def array-list? (f*n splice-or #(instance+? %2 %1)
                                       AArrList
                                       #+clj java.util.Arrays$ArrayList))
      (def map-entry?  #+clj  (partial instance+? clojure.lang.MapEntry)
                       #+cljs (fn-and vector? (fn-> count (= 2))))
      (def sorted-map? (partial instance+? ATreeMap))
      (def queue?      (partial instance+? AQueue))
      (def lseq?       (partial instance+? ALSeq))
      (def coll+?      (fn-or coll? array-list?))
      (def pattern?    (partial instance+? ARegex))
      (def regex?      pattern?)
      (def editable?   (partial instance+? AEditable))
      (def transient?  (partial instance+? ATransient))
#+clj (def throwable?  (partial instance+? java.lang.Throwable))
      (def error?      (partial instance+? AError))

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
(defmacro should-transientize? [coll]
  `(and (editable? ~coll)
        (counted?  ~coll)
        (-> ~coll count (> transient-threshold))))


(def types
  {:map
    #{#+cljs cljs.core/PersistentArrayMap
      #+cljs cljs.core/TransientArrayMap 
      #+cljs cljs.core/PersistentHashMap 
      #+cljs cljs.core/TransientHashMap  
      #+cljs cljs.core/PersistentTreeMap
      #+clj  clojure.lang.IPersistentMap
      #+clj  java.util.Map}
   :set
     #{#+cljs cljs.core/PersistentHashSet
       #+cljs cljs.core/TransientHashSet
       #+cljs cljs.core/PersistentTreeSet}
   :vec
     #{#+clj  clojure.lang.IPersistentVector
       #+cljs cljs.core/PersistentVector
       #+cljs cljs.core/TransientVector}
   :iseq
     #{#+clj  java.util.List
       #+cljs cljs.core/List
       #+cljs cljs.core/PersistentQueue
       #+cljs cljs.core/ValSeq
       #+cljs cljs.core/KeySeq
       #+cljs cljs.core/LazySeq
       #+cljs cljs.core/IndexedSeq
       #+cljs cljs.core/ChunkedSeq
       #+cljs cljs.core/ArrayList}})
  
#+clj
(def arr-types
  {:short    (type (short-array   0)      )
   :long     (type (long-array    0)      )
   :float    (type (float-array   0)      )
   :int      (type (int-array     0)      )
   :double   (type (double-array  0.0)    )
   :boolean  (type (boolean-array [false]))
   :byte     (type (byte-array    0)      )
   :char     (type (char-array    "")     )
   :object   (type (object-array  [])     )})












