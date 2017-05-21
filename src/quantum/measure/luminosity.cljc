(ns quantum.measure.luminosity
  (:require [quantum.measure.core #?@(:clj [:refer [defunits-of]])])
  #?(:cljs (:require-macros [quantum.measure.core :refer [defunits-of]])))

; Luminous intensity in a given direction of a source which
; emits monochromatic radiation at 540e12 Hz with radiant
; intensity 1|683 W/steradian.  (This differs from radiant
; intensity (W/sr) in that it is adjusted for human
; perceptual dependence on wavelength.  The frequency of
; 540e12 Hz (yellow) is where human perception is most
; efficient.)
#_(defunits-of luminosity [:candelas #{:cd}])

#?(:clj (set! *unchecked-math* :warn-on-boxed))