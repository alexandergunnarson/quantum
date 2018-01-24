(ns quantum.ai.ml.neighbor
  "Nearest neighbor search, an optimization problem
   for finding closest points in metric spaces."
  (:require
    [quantum.core.error :as err
      :refer [>ex-info TODO]]
    [quantum.core.vars
      :refer        [#?@(:clj [defalias])]
      :refer-macros [          defalias]]
    [quantum.core.log :as log
      :include-macros true]))

(log/this-ns)

(defn lsh
  "LSH is an efficient algorithm for approximate nearest neighbor search
   in high dimensional spaces by performing probabilistic dimension reduction of data.
   The basic idea is to hash the input items so that similar items are mapped to the same
   buckets with high probability (the number of buckets being much smaller
   than the universe of possible input items)."
  {:implemented-by '#{smile.neighbor.LSH}}
  [?] (TODO))

(defn lsh-signatures
  "Locality-Sensitive Hashing for Signatures."
  {:implemented-by '#{smile.neighbor.SNLSH}}
  [?] (TODO))

(defn mplsh
  "Multi-Probe Locality-Sensitive Hashing. A drawback of LSH is the
   requirement for a large number of hash tables in order to achieve good
   search quality. Multi-probe LSH is designed to overcome this drawback.
   Multi-probe LSH intelligently probes multiple buckets that are likely to
   contain query results in a hash table."
  {:implemented-by '#{smile.neighbor.MPLSH}
   :todo ["According to source code, not efficient. Better not use it right now."]}
  [?] (TODO))

(defn linear-search
  "Brute force linear nearest neighbor search.
   This simplest solution computes the distance from the query point to every other point
   in the data, keeping track of the \"best so far\".
   There are no search data structures to maintain, so linear search has no space complexity
   beyond the storage of the data itself.
   Surprisingly, naive search outperforms space partitioning approaches (e.g. KD-trees) on
   higher dimensional spaces. As a general rule, if the dimensionality is D,
   then N >> 2^D should hold, where N is the number of points in the dataset.
   Otherwise, when KD-trees are used with high-dimensional dataset, most of the
   points in the tree will be evaluated and the efficiency is no better than
   exhaustive search, and approximate nearest-neighbor methods should be used
   instead."
  {:implemented-by '#{smile.neighbor.LinearSearch}}
  [? & [impl]] (TODO))

(defn search-radius
  "Search the neighbors in the given radius of the query object."
  {:implemented-by '#{smile.neighbor.LSH
                      smile.neighbor.SNLSH
                      smile.neighbor.MPLSH
                      smile.neighbor.LinearSearch}}
  [? & [impl]] (TODO))
