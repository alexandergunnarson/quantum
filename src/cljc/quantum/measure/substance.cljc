(ns quantum.measure.substance
  (:require [quantum.measure.core #?@(:clj [:refer [defunits-of]])])
  #?(:cljs (:require-macros [quantum.measure.core :refer [defunits-of]])))

#_(defunits-of substance [:mol #{:moles}]
  ; The amount of substance of a system which contains as many
  ; elementary entities as there are atoms in 0.012 kg of
  ; carbon 12.  The elementary entities must be specified and
  ; may be atoms, molecules, ions, electrons, or other
  ; particles or groups of particles.  It is understood that
  ; unbound atoms of carbon 12, at rest and in the ground
  ; state, are referred to.
  )

#?(:clj (set! *unchecked-math* :warn-on-boxed))