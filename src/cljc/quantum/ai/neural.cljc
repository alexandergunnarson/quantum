(ns quantum.ai.neural
  (:require
    [quantum.core.vars
      :refer        [#?@(:clj [defalias])]
      :refer-macros [          defalias]]
    [quantum.core.error
      :refer [->ex TODO]]))

; TO EXPLORE
; - <org.apache.commons.math3.ml.neuralnet.*>
; ================

; unsupervised learning
; artificial neural network
; multidimensional scaling
; competitive learning
; vector quantization
(defn som
  "Self-Organizing Map.
   An unsupervised learning method to produce
   a low-dimensional (typically two-dimensional) discretized representation
   (called a map) of the input space of the training samples. The model was
   first described as an artificial neural network by Teuvo Kohonen, and is
   sometimes called a Kohonen map.

   SOMs are useful for visualizing low-dimensional views of high-dimensional
   data, akin to multidimensional scaling.
   They belong to a large family of competitive learning processes and vector
   quantization.
   SOMs consist of components called nodes or neurons.
   SOMs form a semantic map where similar samples are mapped close together
   and dissimilar ones apart. More neurons point to regions with high training
   sample concentration and fewer where the samples are scarce.

   SOMs may be considered a nonlinear generalization of Principal components
   analysis (PCA).

   COMPARISON WITH OTHER ALGORITHMS
   It has been shown, using both artificial and real geophysical data, that SOMs
   have many advantages over the conventional feature extraction methods such as
   Empirical Orthogonal Functions (EOF) or PCA. (CITE)

   SOMs with a small number of nodes behave in a way that is similar to K-means.
   However, larger SOMs display properties which are emergent. Therefore, large
   maps are preferable to smaller ones.
   In maps consisting of thousands of nodes, it is possible to perform cluster
   operations on the map itself."
  {:implemented-by '#{smile.vq.SOM}}
  [?] (TODO))

(defalias kohonen-map som)

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
