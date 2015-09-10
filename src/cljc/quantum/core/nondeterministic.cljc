(ns
  ^{:doc "A few functions copied from thebusby.bagotricks.
          Not especially used at the moment."
    :attribution "Alex Gunnarson"}
  quantum.core.nondeterministic
  (:require-quantum [ns coll fn logic]))

(def ^java.util.Random random-generator (java.util.Random.))

(defn rand-int-between [a b]
  (+ a (.nextInt random-generator (inc (- b a)))))

(defn rand-char-between [a b]
  (char (rand-int-between a b)))

(defn ^String rand-chars-between [n a b]
  (let [sb (StringBuilder.)]
    (dotimes [m n]
      (.append sb (rand-char-between a b)))
    (str sb)))

(defn ^String rand-numeric
  ([]  (rand-char-between    48 57 ))
  ([n] (rand-chars-between n 48 57 )))
(defn ^String rand-upper  
  ([]  (rand-char-between    65 90 ))
  ([n] (rand-chars-between n 65 90 )))
(defn ^String rand-lower  
  ([]  (rand-char-between    97 122))
  ([n] (rand-chars-between n 97 122)))

(def generators
  {:numeric rand-numeric
   :upper   rand-upper
   :lower   rand-lower})

(defn ^String rand-string
  ([n]
    (rand-chars-between n
      (core/int Character/MIN_VALUE)
      (core/int Character/MAX_VALUE)))
  ([n opts]
    (let [opts-indexed (zipmap (coll/lrange) opts)
          sb (StringBuilder.)]
      (dotimes [i n]
        (let [generator-k (get opts-indexed (rand-int-between 0 (-> opts count dec)))
              generator (get generators generator-k)]
          (.append sb (generator))))
      (str sb))))


#?(:clj
(defmacro percent-chance
  "Branch true/false based on a percentage of chance"
  {:attribution "thebusby.bagotricks"}
  [chance lucky & unlucky]
  `(if (<= (* (rand) 100) ~chance)
     ~lucky
     ~@unlucky)))

#?(:clj
(defmacro cond-percent*
  {:attribution "thebusby.bagotricks"}
  [random-percent & clauses]
  (let [cond-details (->> clauses
                          (partition 2 2 nil)
                          (sort-by (MWA first) >)
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
              "cond-percent requires percent clauses sum to 100%"))))))

#?(:clj
(defmacro cond-percent
  "Similar to clojure.core/cond, but for each condition takes the percentage chance
   the form should be executed and returned.

   Ex. (cond-percent
         50 \"50% Chance\"
         40 \"40% Chance\"
         10 (str 10 \"% Chance\")

   NOTE: all conditions must sum to 100%"
   {:attribution "thebusby.bagotricks"}
  [& clauses]
  `(let [random-percent# (* (clojure.core/rand) 100)]
     (cond-percent* random-percent# ~@clauses))))

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