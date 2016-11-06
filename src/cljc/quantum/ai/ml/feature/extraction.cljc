(ns quantum.ai.ml.feature.extraction
  "Build a new set of features from the original feature set.
   AKA projection; dimensionality reduction.
   It transforms the data in the high-dimensional space to a
   lower-dimensional space. The data transformation may be linear,
   as in principal component analysis (PCA), but many nonlinear
   dimensionality reduction techniques also exist."
  (:require
    [quantum.core.error :as err
      :refer [->ex TODO]]
    [quantum.core.vars
      :refer        [#?@(:clj [defalias])]
      :refer-macros [          defalias]]
    [quantum.core.log :as log
      :include-macros true]))

; multidimensional scaling
; dimensionality reduction
; vector quantization
; manifold

(defn pca
  "Principal component analysis.
   Heavily influenced when there are outliers in the data."
  {:implemented-by '#{smile.projection.PCA}}
  [?] (TODO))

(defn ppca
  "Probabilistic principal component analysis."
  {:implemented-by '#{smile.projection.PPCA}}
  [?] (TODO))

(defn kpca
  "Kernel principal component analysis.
   An extension of principal component analysis (PCA) using
   techniques of kernel methods."
  {:implemented-by '#{smile.projection.KPCA}}
  [?] (TODO))

(defn gha
  "Generalized Hebbian Algorithm, a linear feed-forward neural
   network model for unsupervised learning with applications primarily in
   principal components analysis."
  {:implemented-by '#{smile.projection.GHA}}
  [?] (TODO))

(defn random
  "A promising dimensionality reduction technique for learning
   mixtures of Gaussians."
  {:implemented-by '#{smile.projection.RandomProjection}}
  [?] (TODO))

; unsupervised learning
; artificial neural network
; competitive learning
(defn som
  "Self-Organizing Map.
   May be considered a nonlinear generalization of principal component
   analysis (PCA).

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

; ===== MANIFOLD ===== ;

; Finds a low-dimensional basis for describing high-dimensional data.
; Nonlinear dimensionality reduction.
; Algorithms for this task are based on the idea that the dimensionality of many
; data sets is only artificially high; though each data point consists of perhaps
; thousands of features, it may be described as a function of only a few underlying
; parameters.

(defn laplacian-eigenmap
  "Computes a low-dimensional representation of the dataset that optimally preserves
   local neighborhood information in a certain sense.
   The locality-preserving character of the Laplacian Eigenmap algorithm makes it
   relatively insensitive to outliers and noise. It is also not prone to \"short
   circuiting\" as only the local distances are used."
  {:implemented-by '#{smile.manifold.LaplacianEigenmap}}
  [?] (TODO))

(defn c-isomap
  "Isometric feature mapping.
   Highly efficient and generally applicable to a broad range of data sources and
   dimensionalities.

   It is vulnerable to \"short-circuit errors\" if k is too large or small with
   respect to the manifold structure, or if noise in the data moves the points
   slightly off the manifold."
  {:implemented-by '#{smile.manifold.IsoMap}}
  [?] (TODO))

(defn lle
  "Locally Linear Embedding. It has several advantages over IsoMap, including
   faster optimization when implemented to take advantage of sparse matrix
   algorithms, and better results with many problems.
   It uses an eigenvector-based optimization technique to find the low-dimensional
   embedding of points, such that each point is still described with the same linear
   combination of its neighbors.
   It tends to handle non-uniform sample densities poorly because there is no fixed unit
   to prevent the weights from drifting as various regions differ in sample densities."
  {:implemented-by '#{smile.manifold.LLE}}
  [?] (TODO))
