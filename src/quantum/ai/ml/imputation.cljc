(ns quantum.ai.ml.imputation
  "Missing value imputation.
   - Missing Completely at Random (MCAR)
   - Random (MAR)
   - Covariate Dependent (CD)
   - Non-Ignorable (NI)
   If it is known that the data analysis technique which is to be used isn't
   content robust, it is good to consider imputation.

   Imputation is not the only method available for handling missing data.
   The expectation-maximization algorithm is a method for finding maximum
   likelihood estimates.
   In machine learning, it is sometimes possible to train a classifier directly
   over the original data without imputing it first. That was shown to yield
   better performance in cases where the missing data is structurally absent,
   rather than missing due to measurement noise."
  (:refer-clojure :exclude
    [for first count get])
  (:require
    [quantum.core.fn
      :refer [fn1]]
    [quantum.core.error              :as err
      :refer [>ex-info TODO]]
    [quantum.core.collections        :as coll
      :refer [for for', fori fori', fortimes fortimes:objects
              first, count, map+, remove+, get]]
    [quantum.numeric.statistics.core :as stat]))

(defn imputation-base
  "Imputes missing values in a 2D tensor `x••` with the reduction function `rf`
   of each column (could be mean, mode, etc.).
   Assumes that the rows of `x••` are of equal size."
  [x•• missing?-pred rf]
  (let [mode• (fortimes:objects [i:x (-> x•• first count)]
                (delay (->> x•• (map+ (fn1 get i:x)) (remove+ missing?-pred) rf)))]
    (for' [x• x••]
      (fori' [x x• i:x]
        (if (missing?-pred x) @(get mode• i:x) x)))))

(defn mode
  "Imputes missing values in a 2D tensor `x••` with the mode of each column.
   Assumes that the rows of `x••` are of equal size."
  [x•• missing?-pred] (imputation-base x•• missing?-pred stat/mode))

(defn mean
  "Imputes missing values in a 2D tensor `x••` with the mean of each column.
   Assumes that the rows of `x••` are of equal size."
  [x•• missing?-pred] (imputation-base x•• missing?-pred stat/mean))

(defn mean:instance
  "Impute missing values with the mean of other attributes in the instance."
  {:implemented-by '#{smile.imputation.AverageImputation}}
  [?] (TODO))

(defn k-means
  "Missing value imputation by K-Means clustering."
  {:implemented-by '#{smile.imputation.KMeansImputation}}
  [?] (TODO))

(defn knn
  "Missing value imputation by k-nearest neighbors."
  {:implemented-by '#{smile.imputation.KNNImputation}}
  [?] (TODO))

(defn lls
  "Local least squares missing value imputation."
  {:implemented-by '#{smile.imputation.LLSImputation}}
  [?] (TODO))

(defn svd
  "Missing value imputation with singular value decomposition."
  {:implemented-by '#{smile.imputation.SVDImputation}}
  [?] (TODO))
