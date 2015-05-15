#?(:clj
(ns quantum.core.data.queue
  (:refer-clojure :exclude
    [for doseq reduce repeat repeatedly range merge count vec into first second rest
     last butlast get pop peek])))

(ns
  ^{:doc "Incorporates the semi-obscure clojure.lang.PersistentQueue into the
          quantum library."
    :attribution "Alex Gunnarson"}
  quantum.core.data.queue
  (:refer-clojure :exclude
    [for doseq range merge count vec into first second rest
     last get pop peek])
  (:require
    [quantum.core.ns :as ns :refer
      #?(:clj  [alias-ns defalias]
         :cljs [Exception IllegalArgumentException
                Nil Bool Num ExactNum Int Decimal Key Vec Set
                ArrList TreeMap LSeq Regex Editable Transient Queue Map])]
    [quantum.core.collections :as coll :refer [into]]
    [quantum.core.loops :as loops 
      #?@(:clj  [:refer        [for doseq]])
      #?@(:cljs [:refer-macros [for doseq]])]
    [quantum.core.numeric     :as num  :refer [int+]]
    [clojure.core.rrb-vector  :as vec+]
    #?(:cljs [clojure.core.rrb-vector.rrbt]))
  #?@(:clj
     [(:import
        clojure.core.Vec
        (clojure.lang MapEntry)
        (quantum.core.ns
          Nil Bool Num ExactNum Int Decimal Key Set
                 ArrList TreeMap LSeq Regex Editable Transient Queue Map))
      (:gen-class)]))
; :refer [range merge count vec into first second rest
;       last get pop peek]

#?(:clj (ns/require-all *ns* :clj))

; QUEUES
; https://github.com/michalmarczyk/jumping-queues

(defn queue
  "Creates an empty persistent queue, or one populated with a collection."
  {:attribution "weavejester.medley"}
  ([] #?(:clj  clojure.lang.PersistentQueue/EMPTY
         :cljs cljs.core.PersistentQueue/EMPTY))
  ([coll] (into (queue) coll)))

#?(:clj
  (defmethod print-method clojure.lang.PersistentQueue
    [q, w]
    (print-method '<- w)
    (print-method (seq q) w)
    (print-method '-< w)))

#?(:clj
  (defn linked-b-queue
    "Generates a java.util.concurrent.LinkedBlockingQueue
    and returns two functions for 'put' and 'take'"
    {:attribution "thebusby.bagotricks"
     :todo ["Likely inefficient to generate fns like this."]}
    ([]
       (let [bq   (java.util.concurrent.LinkedBlockingQueue.)
             put  #(.put bq %)
             take #(.take bq)]
         [put take]))
    ([col]
       (let [bq   (java.util.concurrent.LinkedBlockingQueue. ^Int (int+ col))
             put  #(.put bq %)
             take #(.take bq)]
         [put take]))))  