(ns quantum.core.untyped.numeric
  (:require
    [quantum.core.error :as err])
  #?(:clj (:import java.lang.Math java.math.BigDecimal)))

#?(:clj
(defn integer-value?
  {:adapted-from '#{com.google.common.math.DoubleMath/isMathematicalInteger
                    "https://stackoverflow.com/questions/1078953/check-if-bigdecimal-is-integer-value"}}
  [x]
  (cond #?@(:clj  [(or (double? x) (float? x))
                     (let [x (double x)]
                       (and (not (Double/isNaN x)) (not (Double/isInfinite x)) (= x (Math/rint x))))
                   (instance? java.math.BigDecimal x)
                     (let [^BigDecimal x x]
                       (or (zero? (.signum x))
                           (-> x (.scale) (<= 0))
                           (-> x (.stripTrailingZeros) (.scale) (<= 0))))
                   (integer? x)
                     true
                   (number? x)
                     false]
            :cljs [(number? x)
                     (js/Number.isInteger x)])
        :else (err/not-supported! `integer-value? x))))

(defn signum|long
  [^long x]
  (if (zero? x)
      x
      (bit-or 1 (bit-shift-right x 63))))
