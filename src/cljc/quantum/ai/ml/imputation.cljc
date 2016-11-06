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
  (:require
    [quantum.core.error :as err
      :refer [->ex TODO]]))

(defn average
  "Impute missing values with the average of other attributes in the instance."
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
