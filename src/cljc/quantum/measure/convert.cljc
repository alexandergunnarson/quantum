(ns quantum.measure.convert
  (:require-quantum [:lib])
  (:require
    [quantum.measure.reg        :as reg ]
    [quantum.measure.core       :as meas]
    [quantum.measure.angle      ]
    [quantum.measure.information]
    [quantum.measure.length     ]
    [quantum.measure.luminosity ]
    [quantum.measure.solid-angle]
    [quantum.measure.substance  ]
    [quantum.measure.temperature]
    [quantum.measure.time       ]
    [quantum.measure.volume     ]
    [quantum.measure.weight     ]))

(defn assert-types [& type-pairs]
  (doseq [[unit unit-types] type-pairs]
    (with-throw (nempty? unit-types)        (Err. nil "Unit not found." unit))
    (with-throw (-> unit-types count (= 1)) (Err. nil "Ambiguous units found." unit-types))))

(defmacro convert [n from to]
  (let [from-types (get @reg/reg-units from)
        to-types   (get @reg/reg-units to)
        _ (assert-types [from from-types] [to to-types])
        from-type (first from-types)
        to-type   (first to-types)
        _ (with-throw (= from-type to-type) (Err. nil "Incompatible types." [from-type to-type]))
        conversion-fn-sym
          (symbol (str "quantum.measure." (name from-type))
                  (str (str/str+ from "-") "->" (str/str+ to "-")))]
    `(~conversion-fn-sym ~n)))