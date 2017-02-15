(ns quantum.ai.ml.cluster
  "Clustering analysis. Clustering is the assignment of a set of observations
   into subsets (called clusters) so that observations in the same cluster are
   similar in some sense. Clustering is a method of unsupervised learning."
  (:refer-clojure :exclude [reduce for count])
  (:require
    [clojure.core             :as core]
    [quantum.core.collections :as coll
      :refer [red-apply map+ vals+ filter+ filter-vals+ flatten-1+
              range+ ffilter for for+ reduce join kmap count]]
    [quantum.core.numeric     :as cnum]
    [quantum.core.log         :as log]
    [quantum.numeric.core     :as num
      :refer [sum]]
    [quantum.ai.ml.similarity
      :refer [dist]]
    [quantum.core.fn :as fn
      :refer [<- fn-> fn->>]]
    [quantum.core.error
      :refer [->ex TODO]]
    [quantum.core.logic
      :refer [condpc coll-or]]
    [quantum.core.vars        :as var
      :refer [defalias]]))

(log/this-ns)

; NOTES
; - org.apache.spark.mllib will be deprecated in favor of org.apache.spark.ml
;   once all features are migrated to it.
; =======================

(defn linkage:complete
  "The opposite of complete linkage.
   Distance between groups is defined as the distance between the most distant
   pair of objects, one from each group."
  {:implemented-by '#{smile.clustering.linkage.CompleteLinkage}}
  [?] (TODO))

(defn linkage:single
  "The distance between groups is defined as the distance between the closest
   pair of objects, one from each group.
   Disadvantages:
   - The so-called chaining phenomenon: clusters may be forced together due to
     single elements being close to each other, even though many of the elements
     in each cluster may be very distant to each other.

   Essentially the same as Kruskal's algorithm for minimum spanning trees.
   However, in single linkage clustering, the order in which clusters are formed
   is important, while for minimum spanning trees what matters is the set of pairs
   of points that form distances chosen by the algorithm."
  {:implemented-by '#{smile.clustering.linkage.SingleLinkage}}
  [?] (TODO))

(defn linkage:average
  "Unweighted Pair Group Method with Arithmetic mean. The distance between
   two clusters is the mean distance between all possible pairs of nodes
   in the clusters."
  {:implemented-by '#{smile.clustering.linkage.UPGMALinkage}}
  [?] (TODO))

(defalias linkage:upgma linkage:average)

(defn linkage:centroid
  "Unweighted Pair Group Method using Centroids. The distance between
   two clusters is the Euclidean distance between their centroids, as
   calculated by arithmetic mean.
   Only valid for Euclidean-distancebased proximity matrix."
  {:implemented-by '#{smile.clustering.linkage.UPGMCLinkage}}
  [?] (TODO))

(defalias linkage:upgmc linkage:centroid)

(defn linkage:wpgma
  "Weighted Pair Group Method with Arithmetic mean. Down-weights the largest
   group by giving equal weights to the two branches of the dendrogram that
   are about to fuse."
  {:implemented-by '#{smile.clustering.linkage.WPGMALinkage}}
  [?] (TODO))

(defn linkage:median
  "Weighted Pair Group Method using Centroids (also known as median linkage).
   The distance between two clusters is the Euclidean distance between their
   weighted centroids.
   Only valid for Euclidean-distance-based proximity matrix."
  {:implemented-by '#{smile.clustering.linkage.WPGMCLinkage}}
  [?] (TODO))

(defalias linkage:wpgmc linkage:median)

(defn linkage:wards
  "Ward's linkage, which follows the analysis of variance (ANOVA) approach.
   The dissimilarity between two clusters is computed as the
   increase in the \"error sum of squares\" (ESS) after fusing two clusters.
   Only valid for Euclidean-distance-based proximity matrix."
  {:implemented-by '#{smile.clustering.linkage.WardLinkage}}
  [?] (TODO))

(defn linkage
  "Determines the distance between clusters (i.e. sets of
   observations) based on as a pairwise distance function
   between observations.
   Measures how expensive it is to merge 2 clusters."
  [type ci cj]
  (condp = type
    :single        (red-apply min (for+ [xi ci xj cj] (dist xi xj)))
    :complete      (red-apply max (for+ [xi ci xj cj] (dist xi xj)))
    :median        (TODO)
    :wpgma         (TODO)
    :average       (TODO)
    :centroid      (TODO)
    :wards         (TODO)
    :average-group (TODO)))

(defalias cost          linkage)
(defalias dissimilarity linkage)

; Hierarchical algorithms find successive clusters using previously
; established clusters. These algorithms usually are either agglomerative
; ("bottom-up") or divisive ("top-down").
; Agglomerative algorithms begin with each element as a separate cluster
; and merge them into successively larger clusters.
; Divisive algorithms begin with the whole set and proceed to divide it
; into successively smaller clusters.

; Partitional algorithms typically determine all clusters at once, but can
; also be used as divisive algorithms in the hierarchical clustering.
; Breaks the observation into distinct non-overlapping groups.

; Density-based clustering algorithms are devised to discover
; arbitrary-shaped clusters.

; Subspace clustering methods look for clusters that can only be seen in
; a particular projection (subspace, manifold) of the data. These methods
; thus can ignore irrelevant attributes. The general problem is also known
; as Correlation clustering while the special case of axis-parallel subspaces
; is also known as two-way clustering, co-clustering or biclustering.

(defn cluster:k-means++
  "Clustering algorithm based on David Arthur and
   Sergei Vassilvitski k-means++ algorithm.
   Partitions n observations into k clusters in which each observation belongs
   to the cluster with the nearest mean.
   Finding an exact solution to the k-means problem for arbitrary input is
   NP-hard, but the standard approach to finding an approximate solution
   (often called Lloyd's algorithm or the k-means algorithm) is used widely
   and frequently finds reasonable solutions quickly.

   Disadvantages of standard k-means:
   1. The worst case running time of the algorithm is super-polynomial in the input size.
   2. The approximation found can be arbitrarily bad with respect to the objective function
     compared to the optimal learn.

   k-means++ addresses the second of these disadvantages by specifying a procedure to
   initialize the cluster centers before proceeding with the standard k-means algorithm.
   k-means++ is guaranteed to find a that is O(log k) competitive to the optimal k-means
   solution.

   K-means is a hard clustering method, i.e. each sample is assigned to
   a specific cluster. (I.e. not soft clustering.)
   A type of partition clustering."
  {:implemented-by '#{smile.clustering.KMeans
                      org.apache.commons.math3.ml.clustering.KMeansPlusPlusClusterer}}
  [k xs] (TODO))

(defn cluster:fuzzy-k-means
  {:implemented-by '#{org.apache.commons.math3.ml.clustering.FuzzyKMeansClusterer}}
  [k & xs] (TODO))

(defn cluster:k-medoids
  "An adaptation of the k-means algorithm. Rather than calculate the mean of the
   items in each cluster, a representative item, or medoid, is chosen for each
   cluster at each iteration."
  [k & xs] (TODO))

(defn cluster:x-means
  "An extended K-Means which tries to automatically determine the number of
   clusters based on BIC scores."
  {:implemented-by '#{smile.clustering.XMeans}}
  [?] (TODO))

(defn cluster:g-means
  "An extended K-Means which tries to automatically determine the number of
   clusters based on normality tests."
  {:implemented-by '#{smile.clustering.GMeans}}
  [?] (TODO))

(defn cluster:spectral
  "Spectral clustering techniques make use of the spectrum of the similarity
   matrix of the data to perform dimensionality reduction for clustering in
   fewer dimensions.
   Given a set of data points, the similarity matrix may be defined as a matrix
   S where S.ij represents a measure of the similarity between points."
  {:implemented-by '#{smile.clustering.SpectralClustering}}
  [k & xs] (TODO))

(defn cluster:sib
  "The Sequential Information Bottleneck algorithm. SIB clusters co-occurrence
   data such as text documents vs words. SIB is guaranteed to converge to a local
   maximum of the information. The time and space complexity are significantly
   better than the agglomerative IB algorithm.
   A type of partition clustering."
  {:implemented-by '#{smile.clustering.SIB}}
  [k & xs] (TODO))

(defn cluster:mec
  "Nonparametric Minimum Conditional Entropy Clustering. This method performs
   very well especially when the exact number of clusters is unknown.
   The method can also correctly reveal the structure of data and effectively
   identify outliers simultaneously.

   An iterative algorithm starting with an initial partition given by any other
   clustering methods, e.g. k-means, CLARANS, hierarchical clustering, etc. A
   random initialization is not appropriate.

   A type of partition clustering."
  {:implemented-by '#{smile.clustering.MEC}}
  [k & xs] (TODO))

(defn cluster:deterministic-annealing
  "Extends soft-clustering to an annealing process."
  {:implemented-by '#{smile.clustering.DeterministicAnnealing}}
  [k & xs] (TODO))

(defn cluster:density
  "Employs a cluster model based on kernel density estimation. A cluster is
   defined by a local maximum of the estimated density function.

   Doesn't work on uniformly distributed data. In high dimensional space,
   the data always look like uniformly distributed because of the curse of
   dimensionality. Thus, it doesn't work well on high-dimensional data in general."
  {:implemented-by '#{smile.clustering.DENCLUE}}
  [?] (TODO))

(defn cluster:hierarchical-agglomerative
  "Hierarchical agglomerative clustering seeks to build a hierarchy of clusters in
   a bottom up approach: each observation starts in its own cluster, and pairs of
   clusters are merged as one moves up the hierarchy. The results of hierarchical
   clustering are usually presented in a dendrogram.

   Advantages:
   - Any valid measure of distance can be used.
   - The observations themselves are not required: all that is used is a matrix of
     distances."
  {:implemented-by '#{smile.clustering.HierarchicalClustering}}
  [?] (TODO))

(defn cluster:db-scan
  "DBScan (density-based spatial clustering of applications
   with noise) algorithm.
   DBScan finds a number of clusters starting from the estimated density
   distribution of corresponding nodes.
   A type of partition clustering."
  {:implemented-by '#{org.apache.commons.math3.ml.clustering.DBSCANClusterer
                      smile.clustering.DBScan}}
  [?] (TODO))

(defn cluster:clarans
  "CLARANS: 'Clustering large applications based upon
   randomized search.'
   Better than CLARA.
   The sample must be representative of the whole.

   Efficient medoid-based clustering algorithm.
   A type of partition clustering."
  {:implemented-by '#{smile.clustering.CLARANS}}
  [?] (TODO))

(defn cluster:birch
  "Balanced Iterative Reducing and Clustering using Hierarchies.
   Performs hierarchical clustering over particularly large datasets.
   It makes full use of available memory to derive the finest possible
   sub-clusters while minimizing I/O costs.
   Advantages:
   - Can incrementally and dynamically cluster incoming, multi-dimensional
     metric data points
   - Each clustering decision is made without scanning all data points and
     currently existing clusters. This is because data space is not usually
     uniformly occupied and not every data point is equally important."
  {:implemented-by '#{smile.clustering.BIRCH}}
  [?] (TODO))

(defalias denclue cluster:density)

(defn cluster
  "Clustering algorithm. Selects the implementation
   based on `impl`."
  [impl & args]
  (TODO))
