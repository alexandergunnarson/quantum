(ns quanta.library.nondeterministic)
; from thebusby.bagotricks

(defmacro percent-chance
  "Branch true/false based on a percentage of chance"
  [chance lucky & unlucky]
  `(if (<= (* (rand) 100) ~chance)
     ~lucky
     ~@unlucky))

(defmacro cond-percent*
  [random-percent & clauses]
  (let [cond-details (->> clauses
                          (partition 2 2 nil)
                          (sort-by first >)
                          (reduce (fn [[agg total] [percent clause]]
                                    (if (not (and percent clause))
                                      (throw (IllegalArgumentException. "cond-percent requires an even number of forms"))
                                      (let [nval (+ percent total)]
                                        [(conj agg
                                               (list clojure.core/<
                                                     random-percent
                                                     nval)
                                               clause)
                                         nval])))
                                  [['clojure.core/cond] 0]))]
    (if (== (second cond-details) 100)
      (-> cond-details
          first
          seq)
      (throw (IllegalArgumentException.
              "cond-percent requires percent clauses sum to 100%")))))


(defmacro cond-percent
  "Similar to clojure.core/cond, but for each condition takes the percentage chance
   the form should be executed and returned.

   Ex. (cond-percent
         50 \"50% Chance\"
         40 \"40% Chance\"
         10 (str 10 \"% Chance\")

   NOTE: all conditions must sum to 100%"
  [& clauses]
  `(let [random-percent# (* (clojure.core/rand) 100)]
     (cond-percent* random-percent# ~@clauses)))

(defn get-random-elem
  "Provided a element distribution, choose an element randomly along the distribution"
  [distribution]
  (let [random-percent (* (clojure.core/rand)
                          100)
        cdf (->> distribution
                 (sort-by second >)
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
              "element distribution requires percent clauses sum to 100")))))