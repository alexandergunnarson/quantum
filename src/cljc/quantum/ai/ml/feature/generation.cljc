(ns quantum.ai.ml.feature.generation
  "Feature generation (or constructive induction) studies methods that modify
   or enhance the representation of data objects. Feature generation techniques
   search for new features that describe the objects better than the attributes
   supplied with the training instances.
   Feature normalization is bundled up into this namespace."
  (:require
    [quantum.core.collections :as coll
      :refer [indexed+ remove+ map+ join]]
    [quantum.core.compare        :as comp]
    [quantum.core.data.primitive :as prim]
    [quantum.core.error          :as err
      :refer [->ex TODO]]
    [quantum.core.fn
      :refer [fn->> fn->]]
    [quantum.core.log            :as log]
    [quantum.core.macros
      :refer [defnt]]
    [quantum.core.vars
      :refer [defalias]]
    [quantum.numeric.core        :as numc]))

(log/this-ns)

; ===== GENERATION/CREATION ===== ;

(defn ->bag
  "The bag-of-words feature of text used in NLP and information retrieval.
   In this model, a text (such as a sentence or a document) is represented
   as an unordered collection of words, disregarding grammar and even word order."
  {:implemented-by '#{smile.feature.Bag}}
  [?] (TODO))

(defn nominal->binary
  "Nominal variable to binary dummy variables feature generator. Although some
   method such as decision trees can handle nominal variable directly, other
   methods generally require nominal variables converted to multiple binary
   dummy variables to indicate the presence or absence of a characteristic."
  {:implemented-by '#{smile.feature.Nominal2Binary}}
  [?] (TODO))

(defn nominal->sparse-binary
  "Nominal variables are converted to binary dummy variables in a compact
   representation in which only indices of nonzero elements are stored in an int array."
  {:implemented-by '#{smile.feature.Nominal2SparseBinary}}
  [?] (TODO))

(defn ->features
  "Feature generators/normalizers"
  {:implemented-by '#{smile.feature.FeatureSet
                      #_"Numeric attribute normalization/standardization feature generator.
                         Many machine learning methods such as Neural Networks and SVM with Gaussian
                         kernel also require the features properly scaled/standardized."
                      smile.feature.DateFeature
                      smile.feature.NumericAttributeFeature
                      smile.feature.Bag
                      smile.feature.Nominal2Binary
                      smile.feature.Nominal2SparseBinary}}
  [?] (TODO))

(defn normalize-into-matrix
  "Takes an instance matrix `x••` and normalizes the non-nominal attributes
   into a 2D array."
  [x•• a•:x]
  (numc/normalize-2d:column
    (->> x•• coll/->array-nd)
    (->> a•:x indexed+
              (remove+ (fn-> second :type (= :nominal)))
              (map+    first)
              (join    #{}))))

(defnt missing?
  "Returns the representation for a missing value associated with the type
   of the argument passed.
   For nilable values (references), returns nil.
   For non-nilable values (primitives), returns the max value of that primitive.
   Note that for doubles and floats, where infinity or NaN could have been chosen,
   the max value was preferred to maintain parity with other primitives."
  ([#{byte char short int long float double} x] (= x (prim/->max-value x)))
  ([^default                                 x] (nil? x)))
