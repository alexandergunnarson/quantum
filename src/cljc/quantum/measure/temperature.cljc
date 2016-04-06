(ns quantum.measure.temperature
  (:require [quantum.measure.core :refer [defunits-of]]))

#_(defunits-of temperature [:kelvin #{:K}]
  ;; 1|273.16 of the thermodynamic temperature of the triple
  ;; point of water
  :celsius    {:scale 1   :offset 273.15}
  :fahrenheit {:scale 5/9 :offset 255.37})