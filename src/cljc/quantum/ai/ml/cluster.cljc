(ns quantum.ai.ml.cluster
  (:refer-clojure :exclude [reduce for count])
  (:require
    [#?(:clj  clojure.core
        :cljs cljs.core   )  :as core]
    [quantum.core.collections :as coll
      :refer        [red-apply map+ vals+ filter+ filter-vals+ flatten-1+
                     range+ ffilter
                     #?@(:clj [for for+ reduce join kmap count])]
      :refer-macros [          for for+ reduce join kmap count]]
    [quantum.core.numeric :as cnum]
    [quantum.numeric.core :as num
      :refer        [sum]]
    [quantum.ai.ml.distance
      :refer        [dist]]
    [quantum.core.fn :as fn
      :refer        [#?@(:clj [<- fn-> fn->>])]
      :refer-macros [          <- fn-> fn->>]]
    [quantum.core.cache
      :refer        [#?(:clj defmemoized)]
      :refer-macros [        defmemoized]]
    [quantum.core.error
      :refer        [->ex TODO]]
    [quantum.core.logic
      :refer        [#?@(:clj [condpc coll-or])]
      :refer-macros [          condpc coll-or]]
    [quantum.core.vars        :as var
      :refer        [#?@(:clj [defalias])]
      :refer-macros [          defalias]]))

(defn cost
  "Measures how expensive it is to merge 2 clusters"
  [type ci cj]
  (condp = type
    :single-linkage   (red-apply min
                        (for+ [xi ci xj cj] (dist xi xj)))
    :complete-linkage (red-apply max
                        (for+ [xi ci xj cj] (dist xi xj)))
    :average-linkage       (throw (->ex :todo))
    :average-group-linkage (throw (->ex :todo))
    :k-means          nil #_(->> clusters
                           (map+ (fn [ci]
                                   (let [r (v/centroid ci)]
                                     (->> ci (map+ (fn [p] (v/dist* p r))) sum))))
                           (join []))))


(defn cluster:db-scan
  "DBSCAN (density-based spatial clustering of applications
   with noise) algorithm."
  {:implemented-by '#{org.apache.commons.math3.ml.clustering.FuzzyKMeansClusterer}}
  [k & xs] (TODO))

(defn cluster:fuzzy-k-means
  {:implemented-by '#{org.apache.commons.math3.ml.clustering.FuzzyKMeansClusterer}}
  [k & xs] (TODO))

(defn cluster:k-means++
  "Clustering algorithm based on David Arthur and
   Sergei Vassilvitski k-means++ algorithm."
  {:implemented-by '#{org.apache.commons.math3.ml.clustering.KMeansPlusPlusClusterer}}
  [k & xs] (TODO))

(defn cluster
  "Clustering algorithm. Selects the implementation
   based on `impl`."
  [impl & args]
  (TODO))
