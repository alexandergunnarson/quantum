(ns quantum.ai.neural
  (:require
    [quantum.core.vars
      :refer        [#?@(:clj [defalias])]
      :refer-macros [          defalias]]
    [quantum.core.error
      :refer [->ex TODO]]
    [quantum.ai.ml.feature.extraction :as extract]
    [quantum.core.log :as log
      :include-macros true]))

(log/this-ns)

; TO EXPLORE
; - <org.apache.commons.math3.ml.neuralnet.*>
; ================

(defalias som         extract/som)
(defalias kohonen-map extract/kohonen-map)

(defn neural-gas
  "Neural Gas soft competitive learning algorithm.
   Inspired by the Self-Organizing Map for finding optimal data representations
   based on feature vectors. The algorithm was coined \"Neural Gas\" because of
   the dynamics of the feature vectors during the adaptation process, which
   distribute themselves like a gas within the data space.

   USAGE
   It is mainly applied where data compression or vector quantization is an issue.
   However, it is also used for cluster analysis as a robustly converging
   alternative to k-means clustering. A prominent extension is the Growing
   Neural Gas algorithm."
  {:implemented-by '#{smile.vq.NeuralGas}}
[?] (TODO))

(defn growing-neural-gas
  "As an extension of Neural Gas, Growing Neural Gas can add and delete nodes during
   algorithm execution. The growth mechanism is based on growing cell structures and
   competitive Hebbian learning.."
  {:implemented-by '#{smile.vq.GrowingNeuralGas}}
  [?] (TODO))

(defalias gng growing-neural-gas)

(defn neural-map
  "NeuralMap is an efficient competitive learning algorithm inspired by Growing
   Neural Gas and BIRCH.
   Employs Locality-Sensitive Hashing to speed up the learning while BIRCH uses
   balanced CF trees."
  {:implemented-by '#{smile.vq.NeuralMap}}
  [?] (TODO))
