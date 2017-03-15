(ns quantum.numeric.statistics.core
  (:refer-clojure :exclude
    [for nth count take drop first last map mod frequencies])
  (:require
    [quantum.core.data.map
      :refer [!hash-map]]
    [quantum.core.collections                :as coll
      :refer [nth take drop lasti map take+ map+
              first for last count join frequencies]]
    [quantum.core.fn
      :refer [fn1 fnl fn-> <-]]
    [quantum.core.log                        :as log]
    [quantum.core.numeric                    :as cnum
      :refer [*+* *-* *** *div* mod
              abs sqrt pow e-exp floor log-e]]
    [quantum.numeric.core                    :as num
      :refer [sum sq sigma]]
    [quantum.numeric.tensors                 :as tens]
    [quantum.numeric.polynomial              :as poly]
    [quantum.numeric.statistics.distribution :as dist]
    [quantum.core.vars
      :refer [defalias]]
    [quantum.core.error
      :refer [->ex TODO]]))

(log/this-ns)

; TO EXPLORE
; - Mathematica
;   - Multivariate statistics libraries including fitting, hypothesis testing,
;     and probability and expectation calculations on over 160 distributions.
;   - Support for censored data, temporal data, time-series and unit based data
;   - Calculations and simulations on random processes and queues
; uncomplicate.bayadera.core
; uncomplicate.bayadera.distributions
; [bigml/sampling "3.0"]
; ================================

(defn mean
  "Arithmetic mean"
  [xs]
  (let [[sum ct] (num/sum+count xs)]
    (when (> ct 0) (*div* sum ct))))

(defalias arithmetic-mean mean)

(defn geometric-mean
  "Computes the geometric mean of a reducible collection."
  [xs] ; TODO xs is `reducible?`
  (let [[prod ct] (num/product+count xs)]
    (when (> ct 0) (pow prod (/ 1 ct)))))

(defn moving-average+
  "Moving average of a vector for a given window"
  {:todo        #{"Use reducer instead of laziness here"}
   :attribution "alexandergunnarson"}
  [v window] (->> (partition window 1 v) (map+ mean)))

; For the moment we take the easy option of sorted samples
; TODO abstract this to partition-and-return-index
(defn median
  "Computes the median of a sorted data set.
   References: http://en.wikipedia.org/wiki/Median"
  {:adapted-from 'criterium.stats}
  [data]
  (let [n (count data)
        i (* n 2)]
    (if (even? n)
        [(/ (+ (nth data (dec i)) (nth data i)) 2)
         (take i data)
         (drop i data)]
        [(nth data (* n 2))
         (take i data)
         (drop (inc i) data)])))

(defn mode
  "Computes the mode of a reducible collection.
   If multiple elements occur with equal frequency,
   one will be chosen."
  {:attribution "alexandergunnarson"}
  [xs] ; TODO xs is `reducible?`
  (->> xs
       (frequencies (!hash-map))
       (coll/reduce-max-key val)
       key))

(defn sum-of-squares
  "Computes the sum of the squares of each data point in
   a reducible collection."
  [data] (->> data (map+ sq) sum)) ; TODO xs is `reducible?`

; AKA L2 difference
(defn square-difference [a b] (sq (- a b)))

(defn variance
  "Evaluates to the variance of a sample or population,
   depending on the optional `type` parameter
   (#{:pop :sample}) which defaults to :sample.
   :sample is what R and Excel use."
  ([xs] (variance xs :sample))
  ([xs type]
    (let [[sum' ct] (num/sum+count xs)
          mean'     (*div* sum' ct)
          diff      (case type :pop 0 :sample 1)] ; Bessel's correction
      (->> xs
           (map+ (fn-> (*-* mean') sq))
           sum
           (<- (*div* (- ct diff)))))))

(defn mse:predictor
  "The mean squared error between a vector of predictions `p•` and observed values `o•`."
  [p• o•]
  (mean (tens/v-op+ square-difference p• o•)))

(defalias mse:p•+o• mse:predictor)
(defalias mean-square-error:predictor mse:predictor)

(defn mse:estimator
  "The MSE of an estimator with respect to an unknown parameter."
  [estimator unknown] (TODO))

(defalias mean-square-error:estimator mse:estimator)

(defn mse:p•+o••
  "The mean squared error between a vector of predictions `p•` and 2D vector of observed values `o••`."
  [p• o••]
  (->> o••
       (map+ (fn [o•] (sum (tens/v-op+ square-difference p• o•))))
       mean))

(defn semivariance
  "Computes the semivariance of a set of values with respect to a given cutoff value."
  {:implemented-by '#{org.apache.commons.math3.stat.descriptive.moment.SemiVariance}}
  [?] (TODO))

(defn std-dev
  "Evaluates to the standard deviation of a sample
   or population, depending on the optional `type`
   parameter (#{:pop :sample}) which defaults to :sample.
   :sample is what R and Excel's `std-dev` functions use."
  ([xs] (std-dev xs :sample))
  ([xs type] (sqrt (variance xs type))))

(defn kurtosis
  {:implemented-by '#{org.apache.commons.math3.stat.descriptive.moment.Kurtosis}}
  [?] (TODO))

(defn second-moment
  "Computes a statistic related to the Second Central Moment."
  {:implemented-by '#{org.apache.commons.math3.stat.descriptive.moment.SecondMoment}}
  [?] (TODO))

(defn skewness
  {:implemented-by '#{org.apache.commons.math3.stat.descriptive.moment.Skewness}}
  [?] (TODO))

; ===== CONFIDENCE INTERVALS ===== ;

(defn confidence-interval
  "Find the significance of outliers given boostrapped mean and variance
   estimates. This uses the bootstrapped statistic's variance, but we should use
   BCa of ABC."
  {:adapted-from 'criterium.stats
   :todo ["Which confidence interval method does this use?"]}
  [mean variance]
  (let [n-sigma 1.96 ; use 95% confidence interval
        delta (* n-sigma (sqrt variance))]
    [(- mean delta) (+ mean delta)]))

(defn agresti-coull-interval
  "The Agresti-Coull method for creating a binomial proportion confidence interval."
  {:implemented-by '#{org.apache.commons.math3.stat.interval.AgrestiCoullInterval}}
  [?] (TODO))

(defn clopper-pearson-interval
  "The Clopper-Pearson method for creating a binomial proportion confidence interval."
  {:implemented-by '#{org.apache.commons.math3.stat.interval.ClopperPearsonInterval}}
  [?] (TODO))

(defn normal-approximation-interval
  "The normal approximation method for creating a binomial proportion confidence interval."
  {:implemented-by '#{org.apache.commons.math3.stat.interval.NormalApproximationInterval}}
  [?] (TODO))

(defn wilson-score-interval
  "The Wilson score method for creating a binomial proportion confidence interval."
  {:implemented-by '#{org.apache.commons.math3.stat.interval.WilsonScoreInterval}}
  [?] (TODO))

; ===== *ILES ===== ;

(defn quartiles
  "Calculate the quartiles of a sorted data set
   References: http://en.wikipedia.org/wiki/Quartile"
  {:source 'criterium.stats}
  [data]
  (let [[m lower upper] (median data)]
    [(first (median lower)) m (first (median upper))]))

(defn quantile
  "Calculate the quantile of a sorted data set
   References: http://en.wikipedia.org/wiki/Quantile"
  {:adapted-from 'criterium.stats}
  [quantile data]
  (let [n (lasti data)
        interp (fn [x]
                 (let [f (floor x)
                       i (long f)
                       p (- x f)]
                   (+ (* p (nth data (inc i))) (* (- 1 p) (nth data i)))))]
    (interp (* quantile n))))

(defn boxplot-outlier-thresholds
  "Outlier thresholds for given quartiles."
  {:adapted-from 'criterium.stats}
  [q1 q3]
  (let [iqr    (- q3 q1)
        severe (* iqr 3)
        mild   (* iqr #?(:clj 3/2 :cljs 1.5))]
    [(- q1 severe)
     (- q1 mild)
     (+ q3 mild)
     (+ q3 severe)]))

(defn log-likelihood
  "The log-likelihood of a given a sample set following the distribution."
  [D xs] (TODO))

(defn likelihood
  "The likelihood of a given a sample set following the distribution."
  [D xs] (TODO))

(defn shannon-entropy
  "Based on Shannon's theory of information, in which he assumes only a binary
   classification: Yes or No."
  [?] (TODO))

(defn bayesian-information-criterion
  "AKA BIC, or Schwarz Criterion.
   A criterion for model selection among a class of parametric models
   with different numbers of parameters. Choosing a model to optimize
   BIC is a form of regularization.
   A model with the larger value of BIC is the one to be preferred."
  {:adapted-from 'smile.stat.distribution.BIC/bic
   :params-doc '[[L "the log-likelihood of estimated model"]
                 [v "the number of free parameters to be estimated in the model"]
                 [n "the number of samples"]]}
  [L v n]
  (- L (* #?(:clj 1/2 :cljs 0.5) v (log-e n))))

(defalias schwartz-criterion bayesian-information-criterion)
(defalias bic bayesian-information-criterion)

; ===== SAMPLING =====

; The selection of a subset of individuals from within a statistical
; population to estimate characteristics of the whole population.

(defn sample-uniform+
  "Provide n samples from a uniform distribution on 0..max-val"
  {:adapted-from 'criterium.stats}
  [n max-val rng]
  (take+ n (dist/uniform+ max-val rng)))

(defn sample+
  "Sample with replacement."
  {:adapted-from 'criterium.stats}
  [x rng]
  (let [n (count x)]
    (map+ (fnl nth x) (sample-uniform+ n n rng))))

(defn bootstrap-sample
  "Bootstrap sampling ('bagging') of a statistic, using resampling with replacement.
   A way to improve the classification by combining classifications of randomly
   generated training sets."
  {:adapted-from 'criterium.stats
   :implemented-by '{smile.sampling.Bagging "Faster implementation using arrays"}}
  [data statistic size rng-factory]
  (tens/transpose
    (for [_ (range size)] (statistic (sort (join [] (sample+ data (rng-factory))))))))

(defn bootstrap-estimate
  "Mean, variance and confidence interval. This uses the bootstrapped
  statistic's variance for the confidence interval, but we should use BCa of
  ABC."
  {:adapted-from 'criterium.stats}
  [sampled-stat]
  (let [stats ((juxt mean variance) sampled-stat)]
    (conj stats (apply confidence-interval stats))))

(defn scale-bootstrap-estimate
  {:source 'criterium.stats}
  [estimate scale]
  [(* (first estimate) scale)
   (map (fn1 * scale) (last estimate))])

(defn erf
  "erf polynomial approximation.  Maximum error is 1.5e-7.
  Handbook of Mathematical Functions: with Formulas, Graphs, and Mathematical
  Tables. Milton Abramowitz (Editor), Irene A. Stegun (Editor), 7.1.26"
  {:adapted-from 'criterium.stats}
  [x]
  (let [x (double x)
        sign (cnum/sign' x)
        x (abs x)
        a [1.061405429 -1.453152027 1.421413741 -0.284496736 0.254829592 0.0]
        p 0.3275911
        t (/ (+ 1.0 (* p x)))
        value (- 1.0 (* (poly/value t a) (e-exp (- (* x x)))))]
    (* sign value)))

; NORMAL DISTRIBUTION

(defn normal-cdf
  "Probability p(X<x), for a normal distribution. Uses the polynomial erf
  approximation above, and so is not super accurate."
  {:adapted-from 'criterium.stats}
  [x]
  (-> (/ x (sqrt 2)) erf inc (/ 2)))

(defn normal-quantile
  "Normal quantile function. Given a quantile in (0,1), return the normal value
  for that quantile.
  Wichura, MJ. 'Algorithm AS241' The Percentage Points of the Normal
  Distribution. Applied Statistics, 37, 477-484 "
  {:adapted-from 'criterium.stats}
  [x]
  (let [x (double x)
        a [2509.0809287301226727
           33430.575583588128105
           67265.770927008700853
           45921.953931549871457
           13731.693765509461125
           1971.5909503065514427
           133.14166789178437745
           3.3871328727963666080]
        b [5226.4952788528545610
           28729.085735721942674
           39307.895800092710610
           21213.794301586595867
           5394.1960214247511077
           687.18700749205790830
           42.313330701600911252
           1.0]
        c [0.000774545014278341407640
           0.0227238449892691845833
           0.241780725177450611770
           1.27045825245236838258
           3.64784832476320460504
           5.76949722146069140550
           4.63033784615654529590
           1.42343711074968357734]
        d [1.05075007164441684324e-9
           0.000547593808499534494600
           0.0151986665636164571966
           0.148103976427480074590
           0.689767334985100004550
           1.67638483018380384940
           2.05319162663775882187
           1.0]
        e [
           2.01033439929228813265e-7
           0.0000271155556874348757815
           0.00124266094738807843860
           0.0265321895265761230930
           0.296560571828504891230
           1.78482653991729133580
           5.46378491116411436990
           6.65790464350110377720
           ]
        f [2.04426310338993978564e-15
           1.42151175831644588870e-7
           1.84631831751005468180e-5
           0.000786869131145613259100
           0.0148753612908506148525
           0.136929880922735805310
           0.599832206555887937690
           1.0]]
    (if (<= 0.075 x 0.925)
      (let [v (- x 0.5)
            r (- 180625e-6 (* v v))]
        (* v (/ (poly/value r a) (poly/value r b))))
      (let [r (if (< x 0.5) x (- 1.0 x))
            r (sqrt (- (log-e r)))]
        (if (<= r 5.0)
          (let [r (- r (double #?(:clj 16/10 :cljs 1.6)))]
            (* (cnum/sign' (double (- x 0.5)))
               (/ (poly/value r c) (poly/value r d))))
          (let [r (- r 5.0)]
            (* (cnum/sign' (double (- x 0.5)))
               (/ (poly/value r e) (poly/value r f)))))))))


#_(defn jacknife
  "Jacknife statistics on data."
  [data statistic]
  (tens/transpose
    (map #(statistic (coll/ldrop-at %1 data)) (range (count data)))))



; ===== SAMPLING ===== ;

;             Orig population | Sample
; Simple    | In-memory       | Streaming
; Reservoir | Streaming       | In-Memory
; Stream    | Streaming       | Streaming

; SIMPLE SAMPLING

; The original population is kept in memory but the resulting sample
; is a lazy sequence.
; By default, sampling is done without replacement, which is equivalent
; to a lazy Fisher-Yates shuffle.
; It could replace it though.

; (defalias bigml.sampling.simple/sample) ; could be CLJC if you ported

; A sample may be weighted using the :weigh parameter.
; (take 5 (simple/sample [:heads :tails]
                         ; :weigh {:heads 0.5 :tails 0.5}
                         ; :replace true))

; RESERVOIR SAMPLING

; Keeps the sampled population in memory (the 'reservoir').
; However, the original population is streamed through the reservoir so it
; does not need to reside in memory. This makes reservoirs useful when the
; original population is too large to fit into memory or the overall size
; of the population is unknown.

; Could be :efraimdis or :insertion implementation.

; (reduce conj (reservoir/create 3) (range 10))

; (defalias bigml.sampling.reservoir/create) ; could be CLJC if you ported

; (reservoir/sample (range 10) 5)
; (reservoir/sample (range 10) 5 :replace true :seed 1 :weigh identity)

; STREAM SAMPLING

; Neither the original population or the resulting sample are kept in memory.

; (stream/sample (range) 5 10 :replace true :seed 2)

; (defalias bigml.sampling.stream/create) ; could be CLJC if you ported

(defn get-random-elem
  "Provided a element distribution, choose an element randomly along the distribution"
  {:attribution "thebusby.bagotricks"}
  [distribution]
  (let [random-percent (* (rand) 100)
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
        (throw (->ex "element distribution requires percent clauses sum to 100")))))

(defn probability? [p] (<= 0 p 1))

; ===== CORRELATIONS / COVARIANCE ===== ;

(defn covariance
  "Computes covariances for pairs of arrays or columns of a matrix."
  {:implemented-by '#{org.apache.commons.math3.stat.correlation.Covariance}}
  [?] (TODO))

(defn covariance-storeless
  "Covariance implementation that does not require input data to be stored in memory."
  {:implemented-by '#{org.apache.commons.math3.stat.correlation.StorelessCovariance}}
  [?] (TODO))


(defn correlation-test
  "Correlation test. Correlation of two variables is a measure of the degree
   to which they vary together."
  {:implemented-by '#{}}
  [?] (TODO))

(defn kendalls-correlation
  "Kendall's Tau-b rank correlation."
  {:implemented-by '#{org.apache.commons.math3.stat.correlation.KendallsCorrelation
                      smile.stat.hypothesis.CorTest/kendall}}
  [?] (TODO))

(defn pearsons-correlation
  "Computes Pearson's product-moment correlation coefficients for
   pairs of arrays or columns of a matrix."
  {:implemented-by '#{org.apache.commons.math3.stat.correlation.PearsonsCorrelation
                      smile.stat.hypothesis.CorTest/pearson}}
  [?] (TODO))

(defn spearmans-correlation
  "Spearman's rank correlation."
  {:implemented-by '#{org.apache.commons.math3.stat.correlation.SpearmansCorrelation
                      smile.stat.hypothesis.CorTest/spearman}}
  [?] (TODO))

; ===== HYPOTHESIS/INFERENCE ===== ;

; Statistical hypothesis tests.
; A statistical hypothesis test is a method of making decisions using data,
; whether from a controlled experiment or an observational study (not controlled).
; In statistics, a result is called statistically significant if it is unlikely
; to have occurred by chance alone, according to a pre-determined threshold
; probability, the significance level.

(defn binomial-test
  {:implemented-by '#{org.apache.commons.math3.stat.inference.BinomialTest}}
  [?] (TODO))

(defn chi-squared-test
  {:implemented-by '#{org.apache.commons.math3.stat.inference.ChiSquareTest
                      smile.stat.hypothesis.ChiSqTest
                      smile.stat.hypothesis.CorTest/chisq}}
  [?] (TODO))

(defn f-test
  "F test of the hypothesis that two independent samples come from normal
   distributions with the same variance, against the alternative that they
   come from normal distributions with different variances."
  {:implemented-by '#{smile.stat.hypothesis.FTest}}
  [?] (TODO))


(defn g-test
  {:implemented-by '#{org.apache.commons.math3.stat.inference.GTest}}
  [?] (TODO))

(defn ks-test
  "Kolmogorov-Smirnov (K-S) test for equality of continuous distributions."
  {:implemented-by '#{org.apache.commons.math3.stat.inference.KolmogorovSmirnovTest
                      smile.stat.hypothesis.KSTest}}
  [?] (TODO))

(defn mann-whitney-u-test
  "The Mann-Whitney U test (also called Wilcoxon rank-sum test)."
  {:implemented-by '#{org.apache.commons.math3.stat.inference.MannWhitneyUTest}}
  [?] (TODO))

(defalias wilcoxon-rank-sum-test mann-whitney-u-test)

(defn one-way-anova-test
  "One-way ANOVA (analysis of variance) statistics.
   ANOVA is used when there is a continuous response and categorical predictors."
  {:implemented-by '#{org.apache.commons.math3.stat.inference.OneWayAnova}}
  [?] (TODO))

(defn t-test
  "Student's t-test."
  {:implemented-by '#{org.apache.commons.math3.stat.inference.TTest
                      smile.stat.hypothesis.TTest}}
  [?] (TODO))

(defn wilcoxon-signed-rank-test
  {:implemented-by '#{org.apache.commons.math3.stat.inference.WilcoxonSignedRankTest}}
  [?] (TODO))
