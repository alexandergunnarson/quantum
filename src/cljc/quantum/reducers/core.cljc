(ns quantum.reducers.core
  (:refer-clojure :exclude [frequencies])
  (:require
#?@(:clj
   [[sparkling.conf           :as conf]
    [sparkling.core           :as spark
      :refer [tuple]]
    [sparkling.destructuring  :as de]
    [sparkling.utils          :as u]])
    [quantum.core.logic
      :refer        [#?@(:clj [fn-not])]
      :refer-macros [fn-not]]
    [quantum.core.collections :as coll])
#?(:clj (:import (org.apache.spark.api.java JavaRDDLike))))

#?(:clj (defn rdd? [x] (instance? JavaRDDLike x)))

#?(:clj
(defmacro defreducer
  [name- spark-fn]
  `(defn ~name- [f# x#]
     (if (rdd? x#)
         (~spark-fn f# x#)
         (~(symbol "quantum.core.collections" (name name-)) f# x#)))))

#?(:clj (defreducer map+      spark/map))
#?(:clj (defreducer filter+   spark/filter))
#?(:clj (defreducer group-by+ spark/group-by))
#?(:clj
(defn flatten-1+ [r]
  (if (rdd? r)
      (spark/flat-map identity r)
      (coll/flatten-1+ r))))

#?(:clj
(defn remove+ [f x] (filter+ (fn-not f) x)))

(deftype Reduced [v])

#?(:clj
(defn join
  ([x]
    (if (rdd? x)
        (spark/collect x)
        (coll/join [] x)))
  ([base-coll pipeline]
    (if (rdd? pipeline)
        (.-v
          ^Reduced
          (spark/reduce
            (fn [ret elem]
              (if (instance? Reduced ret)
                  (if (instance? Reduced elem)
                      ; Combine reductions
                      (Reduced. (coll/join (.-v ^Reduced ret) (.-v ^Reduced elem)))
                      ; Reduce into
                      (Reduced. (conj (.-v ^Reduced ret) elem)))
                  ; Initial
                  (Reduced. (conj base-coll ret elem))))
            pipeline))
        (coll/join base-coll pipeline)))))

#?(:clj
(defn frequencies [to x]
  (if (rdd? x)
      (->> x
           (group-by+ identity)
           (map+      (fn [tuple] [(de/key tuple) (count (de/value tuple))]))
           (join      to))
      (coll/red-frequencies to x))))
