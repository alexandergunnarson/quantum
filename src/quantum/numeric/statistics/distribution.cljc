(ns quantum.numeric.statistics.distribution
  (:require
    [quantum.core.log :as log
      :include-macros true]
    [quantum.core.numeric       :as cnum
      :refer        [*+* *-* *** *div* mod
                     #?@(:clj [abs sqrt pow e-exp floor log-e])]
      :refer-macros [          abs sqrt pow e-exp floor log-e]]
    [quantum.core.collections   :as coll
      :refer        [map+]]
    [quantum.core.fn
      :refer        [#?@(:clj [<- fn1 fn->])]
      :refer-macros [          <- fn1 fn->]]
    [quantum.core.vars
      :refer        [#?@(:clj [defalias])]
      :refer-macros [          defalias]]
    [quantum.core.error
      :refer [>ex-info TODO]]))

(log/this-ns)

; ===== DISTRIBUTION =====

(defn uniform+
  "Return uniformly distributed deviates on 0..max-val use the specified rng."
  {:adapted-from 'criterium.stats}
  ; TODO also uncomplicate.bayadera.core has probably a better version
  [max-val rng]
  (map+ (fn1 * max-val) rng))

(defn beta
  "The beta distribution is defined on the interval [0, 1] parameterized by
   two positive shape parameters, typically denoted by alpha and beta.
   It is the special case of the Dirichlet distribution with only two parameters."
  {:implemented-by '#{org.apache.commons.math3.distribution.BetaDistribution
                      uncomplicate.bayadera.core
                      smile.stat.distribution.BetaDistribution}}
  [?] (TODO))

(defn binomial
  "Approximately normal for large n and for p not too close to 0 or 1.

   The the discrete probability distribution of the number of successes
   in a sequence of n independent yes/no experiments, each of which yields
   success with probability p. Such a success/failure experiment is also
   called a Bernoulli experiment or Bernoulli trial."
  {:implemented-by '#{org.apache.commons.math3.distribution.BinomialDistribution
                      uncomplicate.bayadera.core
                      smile.stat.distribution.BinomialDistribution}}
  [?] (TODO))

(defn bernoulli
  "A discrete probability distribution."
  {:implemented-by '#{smile.stat.distribution.BernoulliDistribution}}
  [?] (TODO))

(defn cauchy
  {:implemented-by '#{org.apache.commons.math3.distribution.CauchyDistribution}}
  [?] (TODO))

(defn chi-squared
  "Chi-squared (or chi-square) distribution with k degrees of freedom is the
   distribution of a sum of the squares of k independent standard normal
   random variables.
   The chi-squared distribution is a special case of the gamma distribution.
   Approximately normal for large k."
  {:implemented-by '#{org.apache.commons.math3.distribution.ChiSquaredDistribution
                      smile.stat.distribution.ChiSquareDistribution}}
  [?] (TODO))

(defn empirical
  "An empirical distribution function is a cumulative
   probability distribution function that concentrates probability 1/n at
   each of the n numbers in a sample. As n grows the empirical distribution
   gets closer to the true distribution."
  {:implemented-by '#{org.apache.commons.math3.random.EmpiricalDistribution
                      smile.stat.distribution.EmpiricalDistribution}}
  [?] (TODO))

(defn enumerated
  "A generic implementation of a discrete probability distribution
   over a finite sample space, based on an enumerated list of
   <value, probability> pairs."
  {:implemented-by '#{org.apache.commons.math3.distribution.EnumeratedDistribution}}
  [?] (TODO))

(defn erlang
  {:implemented-by '#{uncomplicate.bayadera.core}}
  [?] (TODO))

(defn exponential
  "An exponential distribution describes the times between events in a Poisson
   process, in which events occur continuously and independently at a constant
   average rate."
  {:implemented-by '#{org.apache.commons.math3.distribution.ExponentialDistribution
                      uncomplicate.bayadera.core
                      smile.stat.distribution.ExponentialDistribution}}
  [?] (TODO))

(defn f
  "F-distribution arises in the testing of whether two observed samples have
   the same variance."
  {:implemented-by '#{org.apache.commons.math3.distribution.FDistribution
                      smile.stat.distribution.FDistribution}}
  [?] (TODO))

(defn gamma
  "A continuous probability distribution."
  {:implemented-by '#{org.apache.commons.math3.distribution.GammaDistribution
                      uncomplicate.bayadera.core
                      smile.stat.distribution.GammaDistribution}}
  [?] (TODO))

(defn geometric
  "A discrete probability distribution of the number of Bernoulli trials
   needed to get one success, supported on the set {1, 2, 3, ...}."
  {:implemented-by '#{org.apache.commons.math3.distribution.GeometricDistribution
                      uncomplicate.bayadera.core
                      smile.stat.distribution.GeometricDistribution}}
  [?] (TODO))

(defn hypergeometric
  "A discrete probability distribution that describes the number of successes
   in a sequence of n draws from a finite population *without* replacement,
   just as the binomial distribution describes the number of successes for draws
   *with* replacement."
  {:implemented-by '#{org.apache.commons.math3.distribution.HypergeometricDistribution
                      uncomplicate.bayadera.core
                      smile.stat.distribution.HyperGeometricDistribution}}
  [?] (TODO))

(defn shifted-geometric
  "A discrete probability distribution of the number of failures
   before the first success, supported on the set {1, 2, 3, ...}."
  {:implemented-by '#{smile.stat.distribution.ShiftedGeometricDistribution}}
  [?] (TODO))

(defn levy
  "Lévy distribution"
  {:implemented-by '#{org.apache.commons.math3.distribution.LevyDistribution}}
  [?] (TODO))

(defn log-normal
  "A probability distribution of a random variable whose logarithm is normally
   distributed."
  {:implemented-by '#{org.apache.commons.math3.distribution.LogNormalDistribution
                      smile.stat.distribution.LogNormalDistribution}}
  [?] (TODO))

(defn logistic
  "A continuous probability distribution whose cumulative distribution function
   is the logistic function,"
  {:implemented-by '#{smile.stat.distribution.LogisticDistribution}}
  [?] (TODO))

(defn normal
  "The normal distribution or Gaussian distribution is a continuous probability
   distribution that describes data that clusters around a mean.
   The central limit theorem states that under certain, fairly common conditions,
   the sum of a large number of random variables will have approximately normal
   distribution."
  {:implemented-by '#{org.apache.commons.math3.distribution.NormalDistribution
                      uncomplicate.bayadera.core
                      smile.stat.distribution.GaussianDistribution}}
  [?] (TODO))

(defalias gaussian normal)

(defn multivariate-normal
  "Multivariate (normal|Gaussian) distribution."
  {:implemented-by '#{smile.stat.distribution.MultivariateGaussianDistribution}}
  [?] (TODO))

(defalias multivariate-gaussian multivariate-normal)

(defn multinomial
  {:implemented-by '#{uncomplicate.bayadera.core}}
  [?] (TODO))

(defn pareto
  {:implemented-by '#{org.apache.commons.math3.distribution.ParetoDistribution}}
  [?] (TODO))

(defn pascal
  "Negative binomial distribution arises as the probability distribution of
   the number of successes in a series of independent and identically distributed
   Bernoulli trials needed to get a specified (non-random) number r of failures.
   If r is an integer, it is usually called Pascal distribution. Otherwise, it
   is often called Polya distribution for the real-valued case."
  {:implemented-by '#{org.apache.commons.math3.distribution.PascalDistribution
                      smile.stat.distribution.NegativeBinomialDistribution}}
  [?] (TODO))

(defalias negative-binomial pascal)
(defalias polya pascal)

(defn poisson
  "Expresses the probability of a number of events occurring in a fixed period
   of time if these events occur with a known average rate and independently
   of the time since the last event.
   Approximately normal for large values of lambda."
  {:implemented-by '#{org.apache.commons.math3.distribution.PoissonDistribution
                      smile.stat.distribution.PoissonDistribution}}
  [?] (TODO))

(defn t
  "(Student's) t-distribution. A probability distribution that arises in the
   problem of estimating the mean of a normally distributed population when
   the sample size is small. Student's t-distribution arises when (as in nearly
   all practical statistical work) the population standard deviation is unknown
   and has to be estimated from the data.
   A special case of the generalised hyperbolic distribution.
   Approximately normal for large values of nu."
  {:implemented-by '#{org.apache.commons.math3.distribution.TDistribution
                      uncomplicate.bayadera.core
                      smile.stat.distribution.TDistribution}}
  [?] (TODO))

(defn triangular
  {:implemented-by '#{org.apache.commons.math3.distribution.TriangularDistribution}}
  [?] (TODO))

(defn weibull
  "One of the most widely used lifetime distributions in reliability engineering.
   It is a versatile distribution that can take on the characteristics of other
   types of distributions.
   - When k = 1, it is the exponential distribution.
   - When k = 2, it becomes equivalent to the Rayleigh distribution.
   - When k = 3.4, it appears similar to the normal distribution.
   - As k goes to infinity, it asymptotically approaches the Dirac delta function."
  {:implemented-by '#{org.apache.commons.math3.distribution.WeibullDistribution
                      smile.stat.distribution.WeibullDistribution}}
  [?] (TODO))

(defn zipf
  {:implemented-by '#{org.apache.commons.math3.distribution.ZipfDistribution}}
  [?] (TODO))

; ===== UTILS ===== ;

(defn mixture
  "The finite mixture of certain distributions.
   A mixture model can be regarded as a type of
   unsupervised learning or clustering."
  {:implemented-by '#{smile.stat.distribution.Mixture
                      smile.stat.distribution.DiscreteMixture
                      smile.stat.distribution.GaussianMixture
                      smile.stat.distribution.MultivariateMixture
                      smile.stat.distribution.MultivariateGaussianMixture
                      smile.stat.distribution.ExponentialFamilyMixture
                      smile.stat.distribution.MultivariateExponentialFamilyMixture}}
  [?] (TODO))

(defn kernel-density-estimation
  "AKA the Parzen window method.
   A non-parametric way of estimating the probability density function
   of a random variable. Kernel density estimation is a fundamental data
   smoothing problem where inferences about the population are made,
   based on a finite data sample."
  {:implemented-by '#{smile.stat.distribution.KernelDensity}}
  [?] (TODO))
