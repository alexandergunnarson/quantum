(ns quantum.ir.cluster
  (:refer-clojure :exclude [reduce for])
  (:require
    [#?(:clj  clojure.core
        :cljs cljs.core   )  :as core]
    [quantum.core.collections :as coll
      :refer [#?@(:clj [for lfor reduce join kmap])
              map+ vals+ filter+ filter-vals+ flatten-1+ range+ ffilter
              reduce-count]
      #?@(:cljs [:refer-macros [for lfor reduce join kmap]])]
    [quantum.core.numeric :as num]
    [quantum.numeric.core
      :refer [find-max-by]]
    [quantum.numeric.vectors :as v]
    [quantum.core.fn :as fn
      :refer [#?@(:clj [<- fn-> fn->>])]
      #?@(:clj [:refer-macros [<- fn-> fn->>]])]
    [quantum.core.cache
      :refer [#?(:clj defmemoized)]
      #?@(:cljs [:refer-macros  [defmemoized]])]
    [quantum.core.error
      :refer [->ex]]
    [quantum.core.logic
      :refer [#?@(:clj [condpc coll-or])]
      #?@(:clj [:refer-macros [condpc coll-or]])]
    [quantum.numeric.core
      :refer [∏ ∑ sum]]))

(defn cost
  "Measures how expensive it is to merge 2 clusters"
  [type ci cj]
  (condp = type
    :single-linkage   (apply min ; TODO faster algorithm
                         (lfor [xi ci xj cj]
                           (v/dist xi xj)))
    :complete-linkage (apply max ; TODO faster algorithm
                        (lfor [xi ci xj cj]
                          (v/dist xi xj)))
    :average-linkage       (throw (->ex :todo))
    :average-group-linkage (throw (->ex :todo))
    :k-means          nil #_(->> clusters
                           (map+ (fn [ci]
                                   (let [r (v/centroid ci)]
                                     (->> ci (map+ (fn [p] (v/dist* p r))) sum))))
                           (join []))))


(defn k-means-clustering [k & xs])

#_(defn k-means-clustering
  [k & xs]
  (let [clusters ?]
    (loop [change false
           i      0
           clusters' clusters]
      (if (and (> i 0) (= change false))
          clusters'
          (let [_ (for [i (range 0 N)]
                    (let [ck (centroid )
                          k' (apply min (get xs))]
                      (if )))]
            (recur ?
                   (inc i)
                   clusters')))))
  #_(let [ck ?]
    (for []
      (dist* Xi (centroid ck)))))

#_(->> clusters
     (map+ (fn [ci]
             (let [r (centroid ci)]
               (->> ci (map+ (fn [p] (dist* p r))) sum))))
     (join []))