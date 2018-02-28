(ns quantum.ai.ml.validation
  (:refer-clojure :exclude
    [get, reduce, count])
  (:require
    [quantum.ai.ml.similarity        :as sim]
    [quantum.core.collections        :as coll
      :refer [map+ map-vals', !map-indexed+, !remove-indexed+, vals+
              join reduce, each+, get, indices+, cat+
              contains?, subset?, assoc-default, count]]
    [quantum.core.compare            :as comp
      :refer [reduce-min reduce-max]]
    [quantum.core.data.validated     :as dv]
    [quantum.core.data.vector
      :refer [!vector]]
    [quantum.core.error              :as err
      :refer [TODO]]
    [quantum.core.fn
      :refer [<- fn->> fn1 fnl]]
    [quantum.core.log                :as log
      :refer [prl prl!]]
    [quantum.core.logic              :as logic
      :refer [whenp-> whenp->> whenp1]]
    [quantum.core.nondeterministic   :as rand]
    [quantum.core.numeric            :as num]
    [quantum.core.spec               :as s]
    [quantum.core.type               :as t]
    [quantum.core.vars               :as var
      :refer [defalias]]
    [quantum.numeric.statistics.core :as stat]))

; ===== N-FOLD CROSS-VALIDATION ===== ;

;; TODO move
(dv/def ai:ml/instances (fn1 t/sequential?)) ; TODO simplistic
;; TODO move
(dv/def ai:ml/targets   (fn1 t/sequential?)) ; TODO simplistic

(defn ->n-fold-splits+
  [xs]
  (->> xs
       (map+  (fn [[x•:train0 x•:validation x•:train1]]
                {:train (join x•:train0 x•:train1) :validation x•:validation}))
       (each+ (fn [{:keys [train validation]}] (err/assert (and (contains? train) (contains? validation)))))))

(defn accuracy:dataset+ [i• model {:keys [instances targets accuracyf]}]
  (->> i• (map+ (fn [i] (accuracyf (get instances i) (get targets i) model)))))

(defn accuracy:train-validation-split+
  "Computes the accuracy of a split between training and validation sets."
  [{:as args' :keys [instances targets trainf accuracyf compute-for]}] ; TODO type-check this
  (fn [{i•:validation :validation i•:train :train}]
    (let [x••:train (->> i•:train (map #(get instances %)))
          t•:train  (->> i•:train (map #(get targets   %)))
          model     (trainf x••:train t•:train)]
      {:train      (when (compute-for :train     ) (accuracy:dataset+ i•:train      model args'))
       :validation (when (compute-for :validation) (accuracy:dataset+ i•:validation model args'))})))

(dv/def-map ai:ml:n-fold-cross-validation/args ; TODO this/n-fold-cross-validation:args
  :conformer (fn [m] (assoc-default m :shuffle?    true
                                      :compute-for #{:validation}))
  :invariant (fn [m] (logic/exactly-1 (:this/n m) (:this/training-ratio m)))
  :req-un [(def :this/instances         :ai:ml/instances)
           (def :this/targets           :ai:ml/targets)
           ^{:doc "2 args, `instances` and `targets`, will be passed here.
                   Inject additional args via `partial` beforehand as needed."}
           (def :this/trainf            fn?) ; TODO must return a model
           ^{:doc "Will be passed 3 args, `model`, `instance`, `output`."}
           (def :this/accuracyf         fn?) ; TODO must return a number, accept 3 args, etc.
           ^{:doc "Whether to shuffle the instances+targets before using the sliding window. Defaults to `true`"}
           (def :this/shuffle?          (fn1 t/boolean?))
           (def :this/compute-for       (s/and (fn1 t/+set?) (fn1 subset? #{:train :validation})))]
  :opt-un [^{:doc "Number of iterations"}
           (def :this/n                 (fn1 t/integer?))
           ^{:doc "Desired ratio of training instances to validation instances"}
           (def :this/training-ratio    (s/and (fn1 t/number?) (fn1 > 0) (fn1 < 1)))
           ^{:doc "Whether to report the accuracies per split instead of calling `mean` on them"}
           (def :this/verbose?          (fn1 t/boolean?))])

(defn n-fold-cross-validation
  "Assumes `instances` and `targets` have same length and correspond to each other.
   Sometimes called k-fold cross-validation.
   2-fold cross-validation is sometimes called the holdout method."
  {:todo #{"More efficient way of dealing with zipped instances+targets"}}
  [args]
  (let [{:as args' :keys [n training-ratio instances shuffle? verbose?]} (->ai:ml:n-fold-cross-validation:args args)]
    (let [splitf (if n
                     (fnl coll/sliding-window-splits+ n) ; split `n` times
                     (fnl coll/sliding-window+ (* training-ratio (count instances))))] ; each split must be of that ratio
      (->> instances
           indices+
           (<- (whenp->> shuffle?
                 (join (!vector))
                 rand/shuffle!))
           splitf
           ->n-fold-splits+
           (map+ (accuracy:train-validation-split+ args'))
           (<- (whenp->> (not verbose?)
                 (map+     (fnl map-vals' stat/mean))
                 ; compute means on each value
                 (reduce   (fn [[ct m'] m] [(inc ct) (merge-with num/nils+ m' m)]) [0 nil])
                 ((fn [[ct m]] (->> m (map-vals' (fn1 num/nils-div ct)))))))))))

(defn silhouette
  "An effective and popular cluster validity metric that seeks to find a balance between
   inter-cluster similarity and intra-cluster dissimilarity.
   Possibly the most popular internal or intrinsic criterion for clustering quality.

   The silhouette score `s` of an instance `x•` will be -1 ≤ `s` ≤ 1.
   `s` approaches 1 as `x•` more and more definitively belongs in cluster `c`.
   `s` is 0 when `x•` belongs equally in `c` as in at least one other cluster.
   `s` approaches -1 as `x•` more and more definitively don't belong in `c`.

   The 2-arity version of the function computes the silhouette score of a clustering.
   The 3-arity version computes the silhouette score of a cluster.
   The 4-arity version computes the silhouette score of an instance."
  {:complexity {2 "O(n^4) (TODO verify this)"
                3 ""
                4 "O(n^2)"}
   :params-doc '{x•    "an instance that belongs to `c`"
                 i:c   "the index of the cluster `c` to which `x•` belongs"
                 c•    "all clusters in the clustering to be evaluated"
                 distf "the distance function used to calculate the distance between two
                        instances"}
   :implementation-help "https://cs.fit.edu/~pkc/classes/ml-internet/silhouette.pdf"
   :see        "Peter J. Rousseeuw (1987). “Silhouettes: a Graphical Aid to the Interpretation
                and Validation of Cluster Analysis”. Computational and Applied Mathematics
                20: 53-65."}
  ([c• distf]
    (->> c• (!map-indexed+ (fn [i:c c] (silhouette c• [i:c c] distf))) stat/mean))
  ([c• [i:c c] distf]
    (->> c (map+ (fn [x•] (silhouette x• c• i:c distf))) stat/mean))
  ([x• c• i:c distf]
    (let [;; mean intra-cluster distance w.r.t. `x•`
          a (->> (get c• i:c) (map+ (fn1 distf x•)) stat/mean)
          ;; min nearest-cluster distance: the distance between `x•` and the nearest cluster
          ;; that `x•` is not a part of.
          b (->> c•
                 (!remove-indexed+ (fn [i:c' c'] (= i:c' i:c)))
                 (map+ (fn->> (map+ (fn1 distf x•)) stat/mean))
                 reduce-min)]
      (/ (- b a) (max a b)))))

(defalias silhouette-coefficient silhouette)
(defalias silhouette-index       silhouette)

(defn dunn
  "Computes the Dunn index of a clustering."
  {:params-doc '{c•                 "a clustering (indexed reducible of clusters)"
                 distf              "calculates the distance between two instances"
                 intracluster-distf "calculates the distance within a cluster;
                                     must be something like `intracluster-distance:max`"
                 intercluster-distf "calculates the distance between two clusters;
                                     must be something like `intercluster-distance:min`"}}
  [c• distf intracluster-distf intercluster-distf]
  (/ (sim/separability c• distf intercluster-distf)
     (sim/compactness  c• distf intracluster-distf)))

(defalias dunn-index dunn)

(defn b-cubed
  "Experimentally determined by Amigó et al. in A comparison of Extrinsic Clustering
   Evaluation Metrics based on Formal Constraints (2009) to be the best external or
   extrinsic criterion for clustering quality.

   Formal constraints on evaluation metrics for clustering tasks include:
   - Cluster Homogeneity
   - Cluster Completeness
   - Rag Bag (miscellaneous clusters are better than a little disorder in good clusters)
   - Cluster Size vs. Quantity

   The analysis of Amigó et al. showed \"of a wide range of metrics shows that only BCubed
   satisfies all formal constraints. ... BCubed combines the best features from other metric
   families.\"

   Note that the authors extend B-Cubed to support objects that belong in multiple clusters,
   which is not supported here."
  {:algorithm "Bagga and Baldwin, 1998"}
  [& args] (TODO))


#_"
https://nlp.stanford.edu/IR-book/html/htmledition/evaluation-of-clustering-1.html
Four external criteria of clustering quality.
- Purity is a simple and transparent evaluation measure.
- Normalized mutual information can be information-theoretically interpreted.
- The Rand index penalizes both false positive and false negative decisions during clustering.
- The F measure in addition supports differential weighting of these two types of errors.

http://e-spacio.uned.es/fez/eserv/bibliuned:DptoLSI-ETSI-MA2VICMR-1090/Documento.pdf
"

; At least according to Almeida et al. ('Is there a best quality metric for graph clusters?', 2010), there is no "best" way to evaluate the quality of a clustering. That said, Wiwie et al. ('Comparing the performance of biomedical clustering methods', 2015) recommended "silhouette values as the best internal measure of clustering quality when tested against 24 biomedical data sets", but noted "it is not a replacement for external indices when available."
