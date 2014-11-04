(ns quanta.library.data.queue
  (:gen-class))
(require
  '[quanta.library.ns               :as ns    :refer [defalias alias-ns]])
(ns/require-all *ns* :clj)
(require
  '[clojure.core.rrb-vector    :as vec+]
  '[quanta.library.collections :as coll :refer :all]
  '[quanta.library.numeric     :as num  :refer [int+]]
  '[quanta.library.type        :as type :refer :all])


; QUEUES
; https://github.com/michalmarczyk/jumping-queues

(defn queue
  "Creates an empty persistent queue, or one populated with a collection."
  ^{:attribution "weavejester.medley"}
  ([] clojure.lang.PersistentQueue/EMPTY)
  ([coll] (into+ (queue) coll)))
(defmethod print-method clojure.lang.PersistentQueue
  [q, w]
  (print-method '<- w)
  (print-method (seq q) w)
  (print-method '-< w))
(defn linked-b-queue
  "Generates a java.util.concurrent.LinkedBlockingQueue
  and returns two functions for 'put' and 'take'"
  ^{:attribution "thebusby.bagotricks"}
  ([]
     (let [bq   (java.util.concurrent.LinkedBlockingQueue.)
           put  #(.put bq %)
           take #(.take bq)]
       [put take]))
  ([col]
     (let [bq   (java.util.concurrent.LinkedBlockingQueue. ^Integer (int+ col))
           put  #(.put bq %)
           take #(.take bq)]
       [put take])))