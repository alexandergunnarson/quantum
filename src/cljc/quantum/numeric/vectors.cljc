(ns quantum.numeric.vectors
  (:refer-clojure :exclude [reduce for count])
           (:require
             [quantum.core.collections :as coll
               :refer [#?@(:clj [for lfor reduce join count])
                       reduce-count  map+ range+]]
             [quantum.core.numeric :as num
               :refer [#?@(:clj [sqrt])]
               #?@(:cljs [:refer-macros [sqrt]])]
             [quantum.numeric.core
               :refer [sq]]
             [quantum.core.fn      :as fn
               :refer [#?@(:clj [<- fn-> fn->>])]]
             [quantum.core.error   :as err
               :refer [->ex]]
             [quantum.numeric.core
               :refer [∏ ∑ sum]]
             [quantum.core.vars
               :refer [#?(:clj defalias)]])
  #?(:cljs (:require-macros
             [quantum.core.vars
               :refer [defalias]]
             [quantum.core.fn          :as fn
               :refer [<- fn-> fn->>]]
             [quantum.core.numeric     :as num]
             [quantum.core.collections :as coll
               :refer [for lfor reduce join count]])))

; TODO have reducers version of these?
; TODO use other more performance-tuned libraries for this
; TODO use numeric core functions 

(defn vlength [v]
  (->> v (map+ sq) sum num/sqrt))

(defn v-op [op v1 v2]
  (assert (= (count v1) (count v2)))
  (->> (range+ 0 (count v1))
       (map+ #(op (get v1 %) (get v2 %)))
       (join [])))

(defn v-
  {:tests `{[[1 2 3] [4 5 6]]
            [-3 -3 -3]}}
  [v1 v2]
  (v-op - v1 v2))

(defn v+
  {:tests `{[[1 2 3] [4 5 6]]
            [5 7 9]}}
  [v1 v2]
  (v-op + v1 v2))

(defn v-div
  {:tests `{[[1 2 3] [4 5 6]]
            [1/4 2/5 1/2]}}
  [v1 v2]
  (v-op / v1 v2))

(defn v*
  {:tests `{[[1 2 3] [4 5 6]]
            [4 10 2]}}
  [v1 v2]
  (v-op * v1 v2))

(defn vsq
  [v] (v* v v))

(defn dot-product [v1 v2]
  (sum (v* v1 v2)))

(defn vsum [vs]
  (reduce v+ (first vs) (rest vs)))

(defn centroid [vs]
  (->> (vsum vs)
       (map+ #(/ % (count vs)))
       (join [])))

(defalias vaverage centroid)

(defn cosine-similarity [a b]
  (/ (dot-product a b)
     (* (vlength a) (vlength b))))

(defn dist* [v1 v2]
  (assert (= (count v1) (count v2)))
  (->> (range+ 0 (count v1))
       (map+ #(- (get v1 %) (get v2 %)))
       (map+ sq)
       sum))

(defn dist [v1 v2] (sqrt (dist* v1 v2))) ; TODO use sqrt like ratios

#_(let [v1 [1 2 3 4 5] v2 [6 7 8 9 10]] ; TODO explore this
  ; These are all equal
  (println (dist* v1 v2)
     (-> (v- v1 v2) vlength sq)
     (cosine-similarity v1 v2)
     (sum (vsq (v- v1 v2)))
     (dot-product (v- v1 v2) (v- v1 v2))))