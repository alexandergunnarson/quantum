(ns quantum.numeric.functions
  "Functions, including gamma, beta, erf, and others."
  (:require
    [quantum.core.vars
      :refer        [#?(:clj defalias)]
      :refer-macros [        defalias]]
    [quantum.core.error :as err
      :refer [TODO]]))

; ========== SPECIAL ========== ;

; ===== GAMMA ===== ;

;; TODO incorporate https://github.com/thi-ng/math/blob/master/src/gamma.org
(defn ^double gamma
  "Gamma function. The extension of the factorial function to the reals.
   Breeze: exp(lgamma(a))
   Matlab: gamma(a)
   numpy:  gamma(a)
   R:      gamma(a)"
  ([^double x] (gamma x :smile))
  ([^double x impl]
    (TODO)
    (case impl
              ; Gamma function. Lanczos approximation (6 terms).
      :smile  #?(:clj  (smile.math.special.Gamma/gamma x)
                 :cljs nil)
      :breeze nil)))

(defn ^double lgamma
  "Log of the gamma function.
   Breeze: lgamma(a)
   Matlab: gammaln(a)
   numpy:  gammaln(a)
   R:      lgamma(a)"
  ([^double x] (lgamma x :smile))
  ([^double x impl]
    (TODO)
    (case impl
      :smile  #?(:clj  (smile.math.special.Gamma/lgamma x)
                 :cljs nil)
      :breeze nil)))

(defn inc-gamma
  "Incomplete gamma
   Breeze: gammp(a, x)
   Matlab: gammainc(a, x)
   numpy:  gammainc(a, x)
   R:      pgamma(a, x) (requires stats library)"
  [a x] (TODO))

(defn ^double reg-inc-gamma
  "Regularized incomplete gamma"
  ([^double s ^double x] (reg-inc-gamma s x :smile))
  ([^double s ^double x impl]
    (TODO)
    (case impl
      :smile  #?(:clj  (smile.math.special.Gamma/regularizedIncompleteGamma s x)
                 :cljs nil)
      :breeze nil)))

(defn ^double inv-reg-inc-gamma
  "Inverse regularized incomplete gamma"
  ([^double a ^double p] (inv-reg-inc-gamma a p :smile))
  ([^double a ^double p impl]
    (TODO)
    (case impl
      :smile  #?(:clj  (smile.math.special.Gamma/inverseRegularizedIncompleteGamma a p)
                 :cljs nil)
      :breeze nil)))

(defn upper-inc-gamma
  "Upper/complementary incomplete gamma
   Breeze: gammq(a, x)
   Matlab: gammainc(a, x, tail)
   numpy:  gammaincc(a, x)
   R:      pgamma(x, a, lower=FALSE) * gamma(a) (requires stats library)"
  [a x] (TODO))

(defn ^double reg-upper-inc-gamma
  "Regularized Upper/Complementary Incomplete Gamma Function"
  ([^double s ^double x] (reg-upper-inc-gamma s x :smile))
  ([^double s ^double x impl]
    (TODO)
    (case impl
      :smile  #?(:clj  (smile.math.special.Gamma/regularizedUpperIncompleteGamma s x)
                 :cljs nil)
      :breeze nil)))

(defn ^double lgamma'
  "Derivative of lgamma
   Breeze: digamma(a)
   Matlab: psi(a)
   numpy:  polygamma(0, a)
   R:      digamma(a)"
  ([^double x] (lgamma' x :smile))
  ([^double x impl]
    (case impl
      :smile  #?(:clj  (smile.math.special.Gamma/digamma x)
                 :cljs nil)
      :breeze nil)))

(defalias digamma lgamma')

(defn digamma'
  "Derivative of digamma
   Breeze: trigamma(a)
   Matlab: psi(1, a)
   numpy:  polygamma(1, a)
   R:      trigamma(a)"
  [a x] (TODO))

(defalias trigamma digamma')

(defn digamma'*
  "Nth derivative of digamma
   Matlab: psi(n, a)
   numpy:  polygamma(n, a)
   R:      psigamma(a, deriv = n)"
  [a x] (TODO))

(defalias trigamma digamma')

; ===== BETA ===== ;

(defn log-beta
  "Breeze: lbeta(a,b)
   Matlab: betaln(a, b)
   numpy:  betaln(a,b)
   R:      lbeta(a, b)"
  [a b] (TODO))

(defn ^double beta
  "Beta function, also called the Euler integral of the first kind.
   The beta function is symmetric, i.e. B(x,y)==B(y,x)."
  [^double x ^double y]
  #?(:clj  (smile.math.special.Beta/beta x y)
     :cljs (TODO)))

(defn ^double reg-inc-beta
  "Regularized Incomplete Beta function.
   Uses continued fraction approximation."
  [^double a ^double b ^double x]
  #?(:clj  (smile.math.special.Beta/regularizedIncompleteBetaFunction a b x)
     :cljs (TODO)))

(defn ^double inv-reg-inc-beta
  "Inverse of regularized Incomplete Beta function.
   Uses continued fraction approximation."
  [^double a ^double b ^double p]
  #?(:clj  (smile.math.special.Beta/inverseRegularizedIncompleteBetaFunction a b p)
     :cljs (TODO)))

(defn generalized-log-beta
  "Breeze: lbeta(a)"
  [a] (TODO))

; ===== ERROR FUNCTION ===== ;

(defn ^double erf
  "The Gauss error function.
   Breeze: erf(a)
   Matlab: erf(a)
   numpy:  erf(a)
   R:      2 * pnorm(a * sqrt(2)) - 1"
  ([^double x] (erf x :smile))
  ([^double x impl]
    (case impl
      :smile  #?(:clj  (smile.math.special.Erf/erf x)
                 :cljs (TODO))
      :breeze (TODO))))

(defn ^double inv-erf
  "The inverse error function.
   Breeze: erfinv(a)
   Matlab: erfinv(a)
   numpy:  erfinv(a)
   R:      qnorm((1 + a) / 2) / sqrt(2)"
  ([^double p] (inv-erf p :smile))
  ([^double p impl]
    (case impl
      :smile  #?(:clj  (smile.math.special.Erf/inverf p)
                 :cljs (TODO))
      :breeze (TODO))))

(defn ^double erfc
  "The complementary error function: 1 - erf(a)
   Breeze: erfc(a)
   Matlab: erfc(a)
   numpy:  erfc(a)
   R:      2 * pnorm(a * sqrt(2), lower = FALSE)"
  ([^double x] (erfc x :smile))
  ([^double x impl]
    (case impl
      :smile  #?(:clj  (smile.math.special.Erf/erfc x)
                 :cljs (TODO))
      :breeze (TODO))))

(defn ^double erfc-concise
  "The complementary error function with fractional error everywhere less
  than 1.2*10^-7. This concise routine is faster than `erfc`."
  [^double x]
  #?(:clj  (smile.math.special.Erf/erfcc x)
     :cljs (TODO)))

(defn ^double inv-erfc
  "Inverse of erfc
   Breeze: erfcinv(a)
   Matlab: erfcinv(a)
   numpy:  erfcinv(a)
   R:      qnorm(a / 2, lower = FALSE) / sqrt(2)"
  ([^double p] (inv-erfc p :smile))
  ([^double p impl]
    (case impl
      :smile  #?(:clj  (smile.math.special.Erf/inverfc p)
                 :cljs (TODO))
      :breeze (TODO))))

; ========== RADIAL BASIS ========== ;

#_"Radial basis functions. A radial basis function is a real-valued function
   whose value depends only on the distance from the origin, so that
   φ(x)=φ(||x||); or alternatively on the distance from some other
   point c, called a center, so that φ(x,c)=φ(||x-c||)."

(defn gaussian-radial-basis
  ([]              #?(:clj (smile.math.rbf.GaussianRadialBasis.)
                      :cljs (TODO)))
  ^{:doc "scale: the scale (bandwidth/sigma) parameter."}
  ([^double scale] #?(:clj (smile.math.rbf.GaussianRadialBasis. scale)
                      :cljs (TODO))))

(defn thin-plate-radial-basis
  ([]              #?(:clj (smile.math.rbf.ThinPlateRadialBasis.)
                      :cljs (TODO)))
  ^{:doc "scale: the scale (bandwidth/sigma) parameter."}
  ([^double scale] #?(:clj (smile.math.rbf.ThinPlateRadialBasis. scale)
                      :cljs (TODO))))

(defn multiquadric-radial-basis
  ([]              #?(:clj (smile.math.rbf.MultiquadricRadialBasis.)
                      :cljs (TODO)))
  ^{:doc "scale: the scale (bandwidth/sigma) parameter."}
  ([^double scale] #?(:clj (smile.math.rbf.MultiquadricRadialBasis. scale)
                      :cljs (TODO))))

(defn inv-multiquadric-radial-basis
  ([]              #?(:clj (smile.math.rbf.InverseMultiquadricRadialBasis.)
                      :cljs (TODO)))
  ^{:doc "scale: the scale (bandwidth/sigma) parameter."}
  ([^double scale] #?(:clj (smile.math.rbf.InverseMultiquadricRadialBasis. scale)
                      :cljs (TODO))))

; ========== OTHER FUNCTIONS ========== ;

(defn sigmoid
  "logistic sigmoid
   Breeze: sigmoid(a)
   numpy:  expit(a)
   R:      sigmoid(a) (requires pracma library)"
  [a] (TODO))

(defn indicator
  "Indicator function
   Breeze: I(a)
   numpy:  where(cond, 1, 0)
   R:      0 + (a > 0)"
  [a] (TODO))
