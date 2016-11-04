(ns quantum.numeric.analysis
  (:require
    [quantum.core.vars
      :refer        [#?(:clj defalias)]
      :refer-macros [        defalias]]
    [quantum.core.error :as err
      :refer [TODO]]))

; http://commons.apache.org/proper/commons-math/javadocs/api-3.3/index.html

; FUNCTIONS ;

; root finding, function integration.

; Function interfaces are intended to be implemented by user code
; to represent domain problems. The algorithms provided by the
; library operate on these functions to find their roots, or
; integrate them, or ...
; Functions can have any level of variate-ness, can be
; real, vectorial or matrix-valued
; , and they can be differentiable or not.

; univariate, bivariate, trivariate, ... multivariate

; FEATURES ;

(defn variate-ness
  "Evaluates to the number of variables of `f`."
  [f] (TODO))

(defn differentiable? [f] (TODO))

(defn function-type
  "Can be #{real, vector, matrix}."
  [f] (TODO))

; ===== HIGHER LEVEL ===== ;

; DIFFERENTIATION ;

(defn fn->differential-fn
  "Computes the differential function of a function `f`.
   y = ax + bx^2 -> y = a + 2bx"
  [f]
  (TODO))

(defn fn->gradient
  "Computes the gradient of a multivariate real function."
  [f]
  (TODO))

(defn fn->jacobian
  "Computes the Jacobian of a multivariate vector function."
  [f]
  (TODO))

; INTEGRATION ;

(defn integrate:legendre-gauss-quadrature
  "Divides the integration interval into equally-sized sub-interval
   and on each of them performs a Legendre-Gauss quadrature."
  [f interval] (TODO))

(defn integrate:midpoint  "Midpoint Rule"     [f interval] (TODO))
(defn integrate:romberg   "Romberg Algorithm" [f interval] (TODO))
(defn integrate:simpson   "Simpson's Rule"    [f interval] (TODO))
(defn integrate:trapezoid "Trapezoid Rule"    [f interval] (TODO))

; <org.apache.commons.math3.analysis.integration.gauss>

(defn integrate
  "Same as (-> f fn->integral (evaluate <interval>))."
  [f interval & [implementation]]
  (TODO)
  (case implementation
    :legendre-gauss-quadrature (integrate:legendre-gauss-quadrature f interval)
    :midpoint                  (integrate:midpoint                  f interval)
    :romberg                   (integrate:romberg                   f interval)
    :simpson                   (integrate:simpson                   f interval)
    :trapezoid                 (integrate:trapezoid                 f interval)))

(defn fn->integral
  "Computes the integral of a univariate real function."
  [f]
  (TODO))

; ===== POLYNOMIAL ===== ;

; <org.apache.commons.math3.analysis.polynomials>

#_(extend-defnt' +
  [^PolynomialFn f0 PolynomialFn f1]
  )

#_(extend-defnt' *
  [^PolynomialFn f0 ^PolynomialFn f1]
  )

#_(extend-defnt' -
  ([^PolynomialFn f0])
  ([^PolynomialFn f0 ^PolynomialFn f0])
  )

#_(defnt ->chebyshev-polynomial
  "Create a Chebyshev polynomial of the first kind."
  [^integer? degree] (TODO))

#_(defnt ->hermite-polynomial  [^integer? degree] (TODO))
#_(defnt ->laguerre-polynomial [^integer? degree] (TODO))
#_(defnt ->legendre-polynomial [^integer? degree] (TODO))
#_(defnt ->jacobi-polynomial
  [^integer? degree ^integer? v ^integer? w] (TODO))

; ===== SOLUTIONS ===== ;

; <org.apache.commons.math3.analysis.solvers>

(defn evaluate
  "Evaluating a function `f` at a particular point
   (set of variable-values) returns a value whose type is the type of `f`.
   E.g., evaluating a vector function at a particular point returns a vector."
  [f & args]
  (TODO))

(defalias solve evaluate)

; ===== INTERPOLATION, VARIOGRAMS, ETC. ===== ;

; <org.apache.commons.math3.analysis.interpolation>

; <smile.interpolation.*>
; http://commons.apache.org/proper/commons-math/javadocs/api-3.3/index.html
