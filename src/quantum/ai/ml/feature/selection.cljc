(ns quantum.ai.ml.feature.selection
  "Feature selection is the technique of selecting a subset of relevant
   features for building robust learning models."
  (:require
    [quantum.core.error :as err
      :refer [->ex TODO]]
    [quantum.core.vars
      :refer [defalias]]
    [quantum.core.log :as log]))

(log/this-ns)

; ===== SUBSET SELECTION ===== ;

(defn subset:genetic
  "Genetic-algorithm-based feature selection. This method finds many (random)
   subsets of variables of expected classification power using a genetic
   algorithm. The \"fitness\" of each subset of variables is determined by its
   ability to classify the samples according to a given classification
   method.
   It avoids brute-force search, but is still much slower than univariate
   feature selection."
  {:implemented-by '#{smile.feature.GAFeatureSelection}}
  [?] (TODO))

; ===== FEATURE RANKING ===== ;

(defn rank:signal-noise-ratio
  "The signal-to-noise (S2N) metric ratio is a univariate feature ranking metric,
   which can be used as a feature selection criterion for binary classification
   problems."
  {:implemented-by '#{smile.feature.SignalNoiseRatio}}
  [?] (TODO))

(defn rank:sum-squares-ratio
  "The ratio of between-groups to within-groups sum of squares is a univariate
   feature ranking metric which can be used as a feature selection criterion
   for multi-class classification problems."
  {:implemented-by '#{smile.feature.SumSquaresRatio}}
  [?] (TODO))
