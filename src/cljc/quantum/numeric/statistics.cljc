(ns quantum.numeric.statistics)

#?(:clj
(defn get-random-elem
  "Provided a element distribution, choose an element randomly along the distribution"
  {:attribution "thebusby.bagotricks"}
  [distribution]
  (let [random-percent (* (clojure.core/rand)
                          100)
        cdf (->> distribution
                 (sort-by (MWA second) >)
                 (reduce (fn [[agg total] [elem dist]]
                           (let [nval (+ total dist)]
                             [(conj agg [elem nval]) nval]))
                         [[] 0])
                 first)]
    (if (== (-> cdf last second) 100)
      (->> cdf
           (drop-while #(< (second %) random-percent))
           first
           first)
      (throw (IllegalArgumentException.
              "element distribution requires percent clauses sum to 100"))))))