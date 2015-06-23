(ns
  ^{:doc "Incorporates the semi-obscure clojure.lang.PersistentQueue into the
          quantum library."
    :attribution "Alex Gunnarson"}
  quantum.core.data.queue
  (:require-quantum [ns num loops])
  (:require
    [clojure.core.rrb-vector  :as vec+]
    #?(:cljs [clojure.core.rrb-vector.rrbt])))

; QUEUES
; https://github.com/michalmarczyk/jumping-queues

(defn queue
  "Creates an empty persistent queue, or one populated with a collection."
  {:attribution "weavejester.medley"}
  ([] #?(:clj  (clojure.lang.PersistentQueue/EMPTY)
         :cljs (.-EMPTY cljs.core/PersistentQueue)))
  ([coll] (into (queue) coll)))

#?(:clj
  (defmethod print-method clojure.lang.PersistentQueue
    [q w]
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