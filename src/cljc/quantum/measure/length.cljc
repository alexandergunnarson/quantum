(ns ^:skip-aot quantum.measure.length
  (:require [quantum.measure.core #?@(:clj [:refer [defunits-of]])])
  #?(:cljs (:require-macros [quantum.measure.core :refer [defunits-of]])))

#?(:clj (set! *unchecked-math* false))

; The meter is the length of the path travelled by light 
; in vacuum during a time interval of 1/299,792,458 of a 
; second. Originally meant to be 1e-7 of the length along 
; a meridian from the equator to a pole.
(defunits-of length [:m #{:meter}]
  ; Microscopic
  :fm       [[1/1000     :pm      ] #{:femtometers :fermi}]
  :pm       [[1/1000     :nm      ] #{:picometers} #{:µµ}]
  :nm       [[1/1000     :um      ] #{:nanometers} #{:millimicrons :millimicrometers}]
  :um       [[1/1000     :mm      ] #{:micrometers :microns} #{:µ}]
  :mm       [[1/10       :cm      ] #{:millimeters}]
  :cm       [[1/100      :m       ] #{:centimeters}]
  ; Macroscopic
  :km       [[1000       :m       ] #{:kilometers}]
  
  ; Imperial units

  :inches   [[1/12       :feet    ] #{:in}]
  :feet     [[1/3        :yards   ] #{:ft}]
  :yards    [[9144/10000 :m       ] #{:yds}] ; cf. the international yard and pound agreement of July 1959  
  :rods     [[1/40       :furlongs] #{:perches :poles :lugs}]
  :furlongs [[1/8        :miles   ]] ; based on US survey foot
  :miles    [[1760       :yards   ]]

  ; Nautical units

  :fathoms                 [[10000000/4999999 :yards          ]] ; basically 2
  :old-brit/fathoms        [[1/100            :old-brit/cables]]

  :shackles-of-cable       [[25/2             :fathoms        ] #{:uk-royal-navy/shackles}]
  :shackles                [[15               :fathoms        ]]
  
  :cables                  [[216000/1822831   :nautical-miles ] #{:cable-lengths}]
  :navy/cables             [[720              :survey-ft      ] #{:navy/cable-lengths}]
  :old-brit/cables         [[1/10             :old-brit/nautical-miles]]
  :metric-cables           [[200              :m              ]]

  :nautical-miles          [[1852             :m              ]]
  :old-brit/nautical-miles [[6080/3           :yards          ]]
  :old-us/nautical-miles   [[30401/5          :m              ]]

  :marine-leagues          [[3                :nautical-miles ]]

  ; Survey units

  :data-miles       [[6000  :ft              ]]
  
  :links/surveyors  [[1/100 :surveyors-chains] #{:links}]
  :surveyors-poles  [[1/4   :surveyors-chains]]
  :survey-ft        [[1/6   :fathoms         ]]
  :surveyors-chains [[66    :survey-ft       ] #{:chains :ch :gunters-chains}]

  :engineers-links  [[1/100 :engineers-chains]]
  :engineers-chains [[100   :ft              ] #{:ramsden-chains}]

  ; Galactic units

  :ua               [[149597870700      :m] #{:au :astronomical-units}]

  :light-years      [[9460730472580800N :m]]
  :light-seconds    [[299792458         :m]]

  :parsecs          [[3.26              :light-years] #{:pc}] ; ≈ - the divisor is 149597870691/tan(pi/180)
  :Mpc              [[1000000           :pc ] #{:megaparsecs}]
  :Gpc              [[1000              :Mpc] #{:gigaparsecs}]

  ; Particle units

  ; The Bohr radius is a physical constant, approximately
  ; equal to the most probable distance between the proton
  ; and electron in a hydrogen atom in its ground state.
  :bohr-radii     [[0.0529177 :nm] #{:atomic-lengths} #{:atomic-units-of-length :a₀}]
  :angstroms      [[100       :pm] nil #{:Å}]

  ; Typographic units
  :picas           [[1/6    :inch]]
  :french/picas    [[4.512  :mm  ]]
  :american/picas  [[4.2175 :mm  ]]
  :computer/picas  [[4.233  :mm  ]]

  :points          [[1/12   :picas         ]]
  :french/points   [[1/12   :french/picas  ]]
  :american/points [[1/12   :american/picas]]
  :computer/points [[1/12   :computer/picas]])

#?(:clj (set! *unchecked-math* :warn-on-boxed))