(ns quantum.core.type
  (:require
    [quantum.core.ns :as ns :refer
            [alias-ns defalias]
                                                
                                                           
                                                                      ]
    [quantum.core.logic :as log :refer
             [splice-or fn-and fn-or fn-not ifn if*n whenc whenf whenf*n whencf*n condf condf*n]
                                            
                          
                                                                  ]
    [quantum.core.function :as fn :refer
             [compr f*n fn* unary fn->> fn-> <- jfn]
                                  
                          
                            ])
       
  (:import
    (quantum.core.ns
      Nil Bool Num ExactNum Int Decimal Key Set
             ArrList TreeMap LSeq Regex Editable Transient Queue Map)
    clojure.core.Vec)
        (:gen-class))

      (set! *warn-on-reflection* true)

; TODOS
; Should include typecasting (/cast/)

                       

(defn instance+? [class-0 obj]
  (try
    (instance? class-0 obj)
          
                         
                                     ))

; NUMBERS
(def  double?           (partial instance+? Double)
                               
                                 
                                                              ) ; has decimal point
(def boolean? 
         (partial instance? Boolean)
                             )

      (def  bigint?    (partial instance+? clojure.lang.BigInt))

; ARRAYS
      (def  ShortArray   (type (short-array   0)))
      (def  LongArray    (type (long-array    0)))
      (def  FloatArray   (type (float-array   0)))
      (def  IntArray     (type (int-array     0)))
      (def  DoubleArray  (type (double-array  0.0)))
      (def  BooleanArray (type (boolean-array [false])))
      (def  ByteArray    (type (byte-array    [])))
      (def  CharArray    (type (char-array    "")))
      (def  ObjectArray  (type (object-array  [])))

      (def  array?       (compr type (jfn isArray))) ; getClass() shouldn't really be a slow call
      (def  byte-array?  (partial instance+? ByteArray))

; TODO add this in cljs
      (def  indexed?     (partial instance+? clojure.lang.Indexed)) 

(def array-list?  (f*n splice-or #(instance+? %2 %1)
                      ArrList
                             java.util.Arrays$ArrayList))
(def map-entry?          (partial instance+? clojure.lang.MapEntry)
                                                             )

(def sorted-map?  (partial instance+? TreeMap))
(def queue?       (partial instance+? Queue))
(def lseq?        (partial instance+? LSeq))
(def coll+?       (fn-or coll? array-list?))
(def pattern?     (partial instance+? Regex))
(def regex?       pattern?)
(def editable?    (partial instance+? Editable))
(def transient?   (partial instance+? Transient))
      (def throwable? (partial instance+? java.lang.Throwable))
(def error?             throwable?                                     )

(defn name-from-class
  [class-0]
  (let [^String class-str (str class-0)]
    (-> class-str
        (subs (-> class-str (.indexOf " ") inc))
        symbol)))



;;;;;;;;;;;; This file autogenerated from src/cljx/quantum/core/type.cljx
