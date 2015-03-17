(ns quantum.core.type
  (:require
    [quantum.core.ns :as ns :refer
      #+clj [alias-ns defalias]
      #+cljs [Exception IllegalArgumentException
              Nil Bool Num ExactNum Int Decimal Key Vec Set
              ArrList TreeMap LSeq Regex Editable Transient Queue Map]]
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

; TODOS
; Should include typecasting (/cast/)

#+cljs (def class type)

(defn instance+? [class-0 obj]
  (try
    (instance? class-0 obj)
    #+cljs
    (catch js/TypeError _
      (try (satisfies? class-0 obj)))))

; NUMBERS
(def  double?    #+clj  (partial instance+? Double)
                 #+cljs (fn-and
                          number?
                          (fn-> str (.indexOf ".") (not= -1)))) ; has decimal point

#+clj (def  bigint?    (partial instance+? clojure.lang.BigInt))

; ARRAYS
#+clj (def  ShortArray   (type (short-array   0)))
#+clj (def  LongArray    (type (long-array    0)))
#+clj (def  FloatArray   (type (float-array   0)))
#+clj (def  IntArray     (type (int-array     0)))
#+clj (def  DoubleArray  (type (double-array  0.0)))
#+clj (def  BooleanArray (type (boolean-array [false])))
#+clj (def  ByteArray    (type (byte-array    [])))
#+clj (def  CharArray    (type (char-array    "")))
#+clj (def  ObjectArray  (type (object-array  [])))

#+clj (def  array?       (compr type (jfn isArray))) ; getClass() shouldn't really be a slow call
#+clj (def  byte-array?  (partial instance+? ByteArray))

(def  boolean?     (partial instance+? Bool))
; TODO add this in cljs
#+clj (def  indexed?     (partial instance+? clojure.lang.Indexed)) 

(def  array-list?  (f*n splice-or #(instance+? %2 %1)
                      ArrList
                      #+clj  java.util.Arrays$ArrayList))
(def  map-entry?   #+clj  (partial instance+? clojure.lang.MapEntry)
                   #+cljs (fn-and vector? (fn-> count (= 2))))

(def  sorted-map?  (partial instance+? TreeMap))
(def  queue?       (partial instance+? Queue))
(def  lseq?        (partial instance+? LSeq))
(def  coll+?       (fn-or coll? array-list?))
(def  pattern?     (partial instance+? Regex))
(def  regex?       pattern?)
(def  editable?    (partial instance+? Editable))
(def  transient?   (partial instance+? Transient))

(defn name-from-class
  [class-0]
  (let [^String class-str (str class-0)]
    (-> class-str
        (subs (-> class-str (.indexOf " ") inc))
        symbol)))


