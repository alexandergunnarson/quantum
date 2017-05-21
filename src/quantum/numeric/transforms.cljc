(ns quantum.numeric.transforms
  (:require
    [quantum.core.error :as err
      :refer [TODO]]
    [quantum.core.vars  :as var
      :refer [defalias]]))

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

; ===== DISCRETE WAVELET TRANSFORMS (DWT) ===== ;

(defn ->wavelet
  "A wavelet is a wave-like oscillation with an amplitude that starts out at
   zero, increases, and then decreases back to zero. Like the fast Fourier
   transform (FFT), the discrete wavelet transform (DWT) is a fast, linear
   operation."
  {:implemented-by '#{smile.wavelet.Wavelet}}
  [?] (TODO))

(defn ->symmlet-wavelet
  "These symmlets have compact support and were constructed to be as nearly
   symmetric (least asymmetric) as possible."
  {:implemented-by '#{smile.wavelet.SymmletWavelet}}
  [?] (TODO))

(defn ->daubechies-wavelet
  "Daubechies wavelets are a family of orthogonal wavelets defining a discrete
   wavelet transform and characterized by maximal number of vanishing moments
   for some given support."
  {:implemented-by '#{smile.wavelet.DaubechiesWavelet}}
  [?] (TODO))

(defn ->haar-wavelet
  "The Haar wavelet is a certain sequence of rescaled \"square-shaped\" functions
   which together form a wavelet family or basis.
   - A special case of the Daubechies wavelet
   - Also known as D2
   - The simplest possible wavelet
   - Not continuous"
  {:implemented-by '#{smile.wavelet.HaarWavelet}}
  [?] (TODO))

(defalias ->d2-wavelet ->haar-wavelet)

(defn ->d4-wavelet
  "The simplest and most localized wavelet, Daubechies wavelet of 4 coefficients."
  {:implemented-by '#{smile.wavelet.D4Wavelet}}
  [?] (TODO))

(defn ->coiflet-wavelet
  "Coiflet wavelets have scaling functions with vanishing moments."
  {:implemented-by '#{smile.wavelet.CoifletWavelet}}
  [?] (TODO))

(defn ->best-localized-wavelet
  {:implemented-by '#{smile.wavelet.BestLocalizedWavelet}}
  [?] (TODO))

(defn wavelet-shrinkage
  "A signal denoising technique based on the idea of thresholding the wavelet
   coefficients.
   The biggest challenge in this approach is finding an appropriate threshold
   value."
  {:implemented-by '#{smile.wavelet.WaveletShrinkage}}
  [?] (TODO))
