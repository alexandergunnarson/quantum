(ns quantum.numeric.fitting
  "Curve fitting"
  [quantum.core.error :as err
    :refer [TODO]])

(defn fit:least-squares:gauss-newton
  {:implemented-by '#{org.apache.commons.math3.fitting.leastsquares.GaussNewtonOptimizer}}
  [f pts] (TODO))

(defn fit:least-squares:levenberg-marquardt
  {:implemented-by '#{org.apache.commons.math3.fitting.leastsquares.LevenbergMarquardtOptimizer}}
  [f pts] (TODO))

(defn fit
  "Fits points to a function."
  {:implemented-by '#{org.apache.commons.math3.fitting.*}}
  [f pts & [impl]] (TODO))

(defn regression:ols
  "OLS (ordinary least squares)"
  {:implemented-by '#{ org.apache.commons.math3.stat.regression.OLSMultipleLinearRegression}}
  [f pts] (TODO))

(defn regression:gls
  {:implemented-by '#{org.apache.commons.math3.fitting.leastsquares.LevenbergMaGLSMultipleLinearRegression}}
  [f pts] (TODO))
