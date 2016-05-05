(ns quantum.measure.solid-angle
  (:require [quantum.measure.core #?@(:clj [:refer [defunits-of]])])
  #?(:cljs (:require-macros [quantum.measure.core :refer [defunits-of]])))

; Solid angle which cuts off an area of the surface
; of the sphere equal to that of a square with
; sides of length equal to the radius of the sphere.
#_(defunits-of solid-angle [:steradians #{:sr}])

#?(:clj (set! *unchecked-math* :warn-on-boxed))