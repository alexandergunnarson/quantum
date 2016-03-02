(ns quantum.measure.convert
  (:require-quantum [ns fn logic num set err macros pr log str])
  (:require
    [quantum.measure.reg        :as reg ]
    [quantum.measure.core       :as meas]
    [quantum.measure.angle      ]
    [quantum.measure.information] ; Not too large
    #_[quantum.measure.length     ] ; Too large
    [quantum.measure.luminosity ]
    [quantum.measure.solid-angle]
    [quantum.measure.substance  ]
    [quantum.measure.temperature]
    [quantum.measure.time       ] ; Not too large
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
        ;conversion-fn-sym
        ;  (symbol (str "quantum.measure." (name from-type))
        ;          (str (str/str+ from "-") "->" (str/str+ to "-")))
        ;conversion-code `(~conversion-fn-sym ~n)
        conversion-map-sym (symbol (str "quantum.measure." (name from-type)) "conversion-map")
        conversion-map-var (resolve conversion-map-sym)]

    (when-not conversion-map-var (throw+ (Err. nil "Conversion map does not exist:" conversion-map-sym)))
    `(* ~n ~(get-in @conversion-map-var [from to]))))
