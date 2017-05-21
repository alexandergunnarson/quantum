(ns quantum.measure.angle
  (:require [quantum.measure.core #?@(:clj [:refer [defunits-of]])])
  #?(:cljs (:require-macros [quantum.measure.core :refer [defunits-of]])))
  
; The angle subtended at the center of a circle by
; an arc equal in length to the radius of the
; circle.
#_(defunits-of angle [:radians #{:rad}])

#?(:clj (set! *unchecked-math* :warn-on-boxed))