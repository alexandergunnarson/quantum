(ns quantum.ai.neural
  (:require
    [quantum.core.vars
      :refer        [#?@(:clj [defalias])]
      :refer-macros [          defalias]]
    [quantum.core.error
      :refer [>ex-info TODO]]
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

(defn mlp
  "Multilayer perceptron neural network.
   An MLP consists of several layers of nodes, interconnected through weighted
   acyclic arcs from each preceding layer to the following, without lateral or
   feedback connections.
   The most popular algorithm to train MLPs is back-propagation, which is a
   gradient descent method.
   For neural networks, the input patterns usually should be scaled/standardized."
  {:implemented-by '#{smile.classification.NeuralNetwork}}
  [?] (TODO))

(defn rbf
  "Radial basis function network, an artificial neural network that uses
   radial basis functions as activation functions.
   Used in function approximation, time series prediction, and control.

   A variant on RBF networks is normalized radial basis function (NRBF)
   networks, in which we require the sum of the basis functions to be unity.
   There is no evidence that either the NRBF method is consistently superior
   to the RBF method, or vice versa.

   With similar number of support vectors/centers, SVM shows better generalization
   performance than RBF when the training data size is relatively small.
   On the other hand, RBF network gives better generalization performance than SVM
   on large training data."
  {:implemented-by '#{smile.classification.RBFNetwork
                      smile.util.SmileUtils.GaussianRadialBasis}}
  [?] (TODO))
