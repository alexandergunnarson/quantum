(ns quantum.measure.time
  (:require [quantum.measure.core #?@(:clj [:refer [defunits-of]])])
  #?(:cljs (:require-macros [quantum.measure.core :refer [defunits-of]])))

#?(:clj (set! *unchecked-math* false))

(def ^:const planck-time-constant (rationalize 5.39106E-44))

(defunits-of time [:seconds #{:sec :s}]
  ; A second is a duration of 9192631770 periods of the radiation
  ; corresponding to the transition between the two hyperfine
  ; levels of the ground state of the cesium-133 atom
  ; Microscopic
  :planck-quanta [[269553/5000000000000000000000000000000000000000000000000 :sec   ]] ; planck-time-constant
  :yoctos        [[1/1000               :zeptos] nil #{:ys  :yoctoseconds}]
  :zeptos        [[1/1000               :attos ] nil #{:zs  :zeptoseconds}]
  :attos         [[1/1000               :femtos] nil #{:as  :attoseconds}]
  :femtos        [[1/1000               :picos ] nil #{:fs  :femtoseconds}]
  :picos         [[1/1000               :nanos ] nil #{:ps  :picoseconds}]
  :nanos         [[1/1000               :micros] nil #{:ns  :nanoseconds}]
  :micros        [[1/1000               :millis] nil #{:mcs :microseconds :µs}]
  :millis        [[1/1000               :sec   ] nil #{:ms  :milliseconds}]
  ; Macroscopic               
  :min           [[60                   :sec   ] #{:minutes} #{ :m}]
  :hrs           [[60                   :min   ] #{:hours}]
  :days          [[24                   :hrs   ] nil #{:d :julian-days}]
  :weeks         [[7                    :days  ] #{:wks} #{:sennights}]
  :months        [[1/12                 :years ] #{:mos}]
  :fortnights    [[14                   :days  ]]
  :common-years  [[365                  :days  ]]
  :years         [[365.25               :days  ] #{:yrs} #{:julian-years}]
  :leap-years    [[366                  :days  ]]
  :decades       [[10                   :years ]]
  :centuries     [[100                  :years ]]
  :millennia     [[1000                 :years ] nil #{:megayears}])

; PRECOMPILED DUE TO COMPILER LIMITATIONS ("Method code too large!" error)

#?(:clj (set! *unchecked-math* :warn-on-boxed))