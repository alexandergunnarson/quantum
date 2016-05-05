(ns quantum.measure.weight
  #?(:clj (:require [quantum.measure.core :refer [defunits-of]]))
  #?(:cljs (:require-macros [quantum.measure.core :refer [defunits-of]])))

#?(:clj (set! *unchecked-math* false))

(defunits-of weight [:g #{:grams}]
  :yg          [[1/1000 :zg ] #{:yoctograms}]
  :zg          [[1/1000 :ag ] #{:zeptograms}]
  :ag          [[1/1000 :fg ] #{:attograms}]
  :fg          [[1/1000 :pg ] #{:femtograms}]
  :pg          [[1/1000 :ng ] #{:picograms}]
  :ng          [[1/1000 :mcg] #{:nanograms}]
  :mcg         [[1/1000 :mg ] #{:micrograms} #{:µg}]
  :mg          [[1/1000 :g  ] #{:milligrams}]
  :cg          [[1/100  :g  ] #{:centigrams}]
  :dg          [[1/10   :g  ] #{:decigrams}]
  :dag         [[10     :g  ] #{:decagrams}]
  :hg          [[100    :g  ] #{:hectograms}]
  :kg          [[1000   :g  ] #{:kilograms :kilos} #{:graves}]
  :Mg          [[1000   :kg ] #{:megagrams :tonnes :metric-tons}]
  :Gg          [[1000   :Mg ] #{:gigagrams}]
  :Tg          [[1000   :Gg ] #{:teragrams}]
  :Pg          [[1000   :Tg ] #{:petagrams}]
  :Eg          [[1000   :Pg ] #{:exagrams}]
  :Zg          [[1000   :Eg ] #{:zettagrams}]
  :Yg          [[1000   :Zg ] #{:yottagrams}]

  ; Non-decimal
  :lbs         [[45359237/100000000 :kg] #{:pounds}]
  :oz          [[1/16 :lbs] #{:ounces}]

  :stone       [[6.35 :kg]]

  :gr          [[64.79891 :mg] #{:grain :troy-grain}]

  :carat       [[200 :mg] nil #{:metric-carat :CD}]
  :pearl-grain [[1/4 :carat] #{:jewelers-grain}])

#?(:clj (set! *unchecked-math* :warn-on-boxed))