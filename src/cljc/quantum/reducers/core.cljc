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
      :refer        [#?@(:clj [fn-not fn-or])]
      :refer-macros [          fn-not fn-or]]
    [quantum.core.error :as err
      :refer [->ex TODO]]
    [quantum.core.macros.core
      :refer        [#?@(:clj [if-cljs])]]
    [quantum.core.vars        :as var
      :refer        [#?@(:clj [defalias defmalias])]
      :refer-macros [defalias defmalias]]
    [quantum.core.collections :as coll]
    [quantum.reducers.spark   :as spark+])
#?(:cljs (:require-macros
           [quantum.reducers.core
             :refer [defreducer]]))
#?(:clj (:import (org.apache.spark.api.java JavaRDDLike)
                 (org.apache.spark.sql      Dataset))))

#?(:clj (defn rdd?     [x] (instance? JavaRDDLike x)))
#?(:clj (defn dataset? [x] (instance? Dataset     x)))

#?(:clj
(defmacro defreducer
  [name- rdd-fn dataset-fn]
  `(defn ~name- [f# x#]
     (cond (rdd? x#)
           (~rdd-fn f# x#)
           (dataset? x#)
           (~dataset-fn f# x#)
           :else (~(symbol "quantum.core.collections" (name name-)) f# x#)))))

#?(:clj (defreducer map+      spark/map      spark+/map     ))
#?(:clj (defreducer filter+   spark/filter   spark+/filter  ))

; TODO move
(defn tuple->vector [kv] [(de/key kv) (coll/join [] (de/value kv))])

#?(:clj
(defn group-by+ [f r]
  (cond (rdd? r)
        (->> r (spark/group-by  f) (map+ tuple->vector))
        (dataset? r)
        (->> r (spark+/group-by f) (map+ tuple->vector))
        :else (coll/group-by+ f r))))

#?(:clj
(defn flatten-1+ [r]
  (cond (rdd? r)
        (spark/flat-map identity r)
        (dataset? r)
        (spark+/flat-map identity r)
        :else (coll/flatten-1+ r))))

#?(:clj
    (defn flatten-1+ [r]
      (if (rdd? r)
          (spark/flat-map identity r)
          (coll/flatten-1+ r)))
   :cljs (defalias flatten-1+ coll/flatten-1+))

(defn remove+ [f x] (filter+ (fn-not f) x))

(deftype Reduced* [v])

#?(:clj
(defn join*
  ([x]
    (cond (rdd? x)
          (spark/collect x)
          (dataset? x)
          (spark+/collect x)
          :else (coll/join [] x)))
  ([base-coll pipeline]
    (if ((fn-or rdd? dataset?) pipeline)
        (.-v
          ^Reduced
          ((if rdd? spark/reduce spark+/reduce)
            (fn [ret elem]
              (if (instance? Reduced* ret)
                  (if (instance? Reduced* elem)
                      ; Combine reductions
                      (Reduced*. (coll/join (.-v ^Reduced* ret) (.-v ^Reduced* elem)))
                      ; Reduce into
                      (Reduced*. (conj (.-v ^Reduced* ret) elem)))
                  ; Initial
                  (Reduced*. (conj base-coll ret elem))))
            pipeline))
        (coll/join base-coll pipeline)))
  ([base-coll parallel? pipeline]
    (if ((fn-or rdd? dataset?) pipeline)
        (join base-coll pipeline)
        (coll/pjoin base-coll pipeline)))))

#?(:clj
(defn frequencies [to x]
  (if (rdd? x)
      (->> x
           (group-by+ identity)
           (map+      (fn [[k v]] [k (count v)]))
           (join      to))
      #_(dataset? x)
      #_(->> x
           (group-by+ identity)
           (map+      (fn [?] ...)
           (join to)))
      (coll/red-frequencies to x))))


#?(:clj
(defn sort-by+
  ([kf x]
    (sort-by+ kf compare x))
  ([kf compf x]
    (if (rdd? x)
        (->> x
             (spark/map-to-pair (fn [elem] (tuple (kf elem) elem)))
             (spark/sort-by-key compf)
             (map+              de/value))
        (TODO)))))