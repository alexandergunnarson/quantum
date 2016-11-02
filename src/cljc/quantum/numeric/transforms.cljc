(ns quantum.numeric.transforms
  (:require
    [quantum.core.error :as err
      :refer [TODO]]))

(defn fast-cosine
  "Fast Cosine Transform for transformation of one-dimensional real data sets."
  {:implemented-by '#{org.apache.commons.math3.transform.FastCosineTransformer}}
  [?] (TODO))

(defn fast-sine
  "Fast Sine Transform for transformation of one-dimensional real data sets."
  {:implemented-by '#{org.apache.commons.math3.transform.FastSineTransformer}}
  [?] (TODO))

(defn fast-fourier
  "Fast Fourier Transform for transformation of one-dimensional real or complex data sets."
  {:implemented-by '#{org.apache.commons.math3.transform.FastFourierTransformer}}
  [?] (TODO))

(defn fast-hadamard
  "Fast Hadamard Transform (FHT)."
  {:implemented-by '#{org.apache.commons.math3.transform.FastHadamardTransformer}}
  [?] (TODO))
