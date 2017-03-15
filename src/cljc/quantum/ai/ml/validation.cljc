(ns quantum.ai.ml.validation
  (:refer-clojure :exclude
    [get, reduce, count])
  (:require
    [quantum.core.collections        :as coll
      :refer [map+ map-vals', vals+, join reduce, each+, get, indices+
              nempty?, subset?, assoc-default, count]]
    [quantum.core.data.validated     :as dv]
    [quantum.core.data.vector
      :refer [!vector]]
    [quantum.core.error              :as err
      :refer [TODO]]
    [quantum.core.fn
      :refer [<- fn->> fn1 fnl]]
    [quantum.core.log                :as log
      :refer [prl]]
    [quantum.core.logic              :as logic
      :refer [whenp-> whenp->> whenp1]]
    [quantum.core.numeric            :as num]
    [quantum.core.spec               :as s]
    [quantum.core.type               :as t]
    [quantum.core.nondeterministic   :as rand]
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
       (each+ (fn [{:keys [train validation]}] (err/assert (and (nempty? train) (nempty? validation)))))))

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
           (<- whenp->> shuffle?
               (join (!vector))
               rand/shuffle!)
           splitf
           ->n-fold-splits+
           (map+ (accuracy:train-validation-split+ args'))
           (<- whenp->> (not verbose?)
               (map+     (fnl map-vals' stat/mean))
               ; compute means on each value
               (reduce   (fn [[ct m'] m] [(inc ct) (merge-with num/nils+ m' m)]) [0 nil])
               ((fn [[ct m]] (->> m (map-vals' (fn1 num/nils-div ct))))))))))
