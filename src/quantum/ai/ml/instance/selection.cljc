(ns quantum.ai.ml.instance.selection
  "Instance (not feature) selection."
  (:refer-clojure :exclude
    [doseq dotimes, count, get, first, assoc!, conj!, empty?])
  (:require
    [quantum.core.collections      :as coll
      :refer [doseq doseqi, dotimes, fortimes:objects2, fortimes:objects
              count lasti, get get-in*, first
              assoc! assoc-in!*, conj!, join!, empty?
              map+, range+, pair]]
    [quantum.core.collections.core :as ccoll
      :refer [->objects-nd]]
    [quantum.core.data.map         :as map
      :refer [>!hash-map #?(:clj >!hash-map|int->ref)]]
    [quantum.core.data.set         :as set
      :refer [>!hash-set #?(:clj >!hash-set|int)]]
    [quantum.core.data.vector
      :refer [>!vector]]
    [quantum.core.error
      :refer [TODO]]
    [quantum.core.fn
      :refer [fn1]]
    [quantum.core.macros
      :refer [defnt #?(:clj defnt')]]
    [quantum.core.nondeterministic :as rand]))

(defn drop-3
  "DROP (Decremental Reduction Optimization Procedure) 3.

   The best of the DROP family of algorithms: according to its designers, it
   \"had the highest accuracy of the DROP methods, and had the lowest storage
   of the accurate ones (DROP2–DROP5), using less than 12% of the original
   instances. ... DROP3 seemed to have the best mix of generalization accuracy
   and storage requirements of the DROP methods.\"

   According to Arnaiz-González et al.'s 'Instance selection of linear complexity
   for big data' (2016), among several others, the DROP family of algorithms
   \"comprises some of the best instance selection methods for classification\".
   DROP3 was the instance selection algorithm with the best compression ratio of
   the ones that were tested.

   It is not suitable for big data due to its algorithmic complexity.
   Prefer LSH-IS-F in all cases."
  {:algorithm   "Wilson & Martinez: 'Reduction Techniques for Exemplar-Based
                 Learning Algorithms' (2000)"
   :complexity  "O(n^2)"
   :compression "0.896       reduction rate according to Arnaiz-González et al.
                 0.839-0.886 reduction rate according to Wilson & Martinez"
   :accuracy    "Higher than most; does not lower accuracy"}
  [] (TODO))

(defnt' lsh-is-f
  "LSH-IS-F. Instance reduction via locality-sensitive hashing with two passes.

   The best tradeoff among accuracy, runtime complexity, and compression.
   While it generally favors speed and accuracy over compression/reduction
   (as opposed to DROP3), the compression ratio can be adjusted by `(count hashf•)`.
   A smaller number of hash functions applied yields a higher compression ratio.

   Orders of magnitude faster than DROP3, and also features a(n indirectly)
   customizable compression/reduction ratio.

   Assumes that output values have been discretized (i.e. is a classification, not
   a regression problem).
   If no feature indices are returned, this means everything was noise.

   Does not yet support data streams.
   Assumes that no `hashf` provides caching."
  {:params-doc  '{x••    "2D matrix of instances"
                  hashf• "A collection of hash function families"}
   :output      "The indices of a set of selected instances `x••'` ⊆ `x••`"
   :algorithm   "Arnaiz-González, Díez-Pastor, Rodríguez, García-Osorio: 'Instance
                 selection of linear complexity for big data' (2016)"
   :complexity  "O(n)  <->  T(|x••|*b*|x•| + |hashf•|*b*|l•◦|)"
   :compression "0.455 reduction rate according to Arnaiz-González et al., but
                 this was with a low `(count hashf•)`"
   :accuracy    "Higher than DROP 3 according to Arnaiz-González et al."
   :todo        #{"Currently only handles one hashf that outputs several bucket values
                   and whose bucket values are dense longs 0..`ct:buckets`.
                   It's eminently possible to support a `hashf•` (a collection of LSH
                   family fns); do it."
                  "Better understand how to create families of locality-sensitive
                   hashing functions.
                   These could come in handy:
                   - https://en.wikipedia.org/wiki/Locality-sensitive_hashing
                   - https://github.com/haifengl/smile/blob/master/core/src/main/java/smile/neighbor/LSH.java"}}
  (; for multi-dimensional label vectors, with
   ; one hashf that outputs several bucket values and whose bucket values are dense longs 0..`ct:buckets`
   [#{doubles-2d?} x•• #{doubles-2d?} l•• ^fn? hashf ^long ct:buckets]
    (let [x••':indices (do #?(:clj (!>hash-set|int) :cljs (!>hash-set)))
          i:hashf->bucket->hash:l•->x••:l•
            ; map of simulated hash-function index to
            (fortimes:objects2 [_ ct:buckets]
              ; map of bucket value `b` to
              (fortimes:objects [_ ct:buckets]
                ; map of label `l•` to all instances, `x••`, which are labeled with `l•` and in `b`
                #?(:clj (!>hash-map|int->ref) :cljs (!>hash-map))))]
      (doseqi [x• x•• i:x•] ; T(|x••|*b*|x•|)
        (let [l• (get l•• i:x•)]
          (doseqi [bucket (hashf x•) i:hashf] ; T(b*|x•|) + T(hash)
            (let [hash:l•->x••:l• (get-in* i:hashf->bucket->hash:l•->x••:l• i:hashf (long bucket))
                  hash:l•        (do #?(:clj  (java.util.Arrays/hashCode ^"[D" l•) ; T(|x•|), at least for Java
                                        :cljs (hash                            l•)))]
              (if-let [x••:l• (get hash:l•->x••:l• hash:l•)]
                (conj! x••:l• x•)
                (assoc! hash:l•->x••:l• hash:l• (!>vector x•)))))))
      (doseq [bucket->hash:l•->x••:l• i:hashf->bucket->hash:l•->x••:l•] ; T(|hashf•|*b*|l•◦|)
        (doseq [hash:l•->x••:l• bucket->hash:l•->x••:l•] ; T(b*|l•◦|)
          (doseq [_ x••:l• hash:l•->x••:l•] ; T(|l•◦|)
            (when (-> x••:l• count (> 1))
              (conj! x••':indices (rand/int-between 0 (lasti x••)))))))
      x••':indices)))
