(ns quantum.untyped.core.numeric
          (:refer-clojure :exclude
            [pos-int?])
          (:require
            [clojure.core               :as core]
    #?(:clj [clojure.future             :as fcore])
            [quantum.untyped.core.core  :as ucore]
            [quantum.untyped.core.error :as uerr]
            [quantum.untyped.core.vars
            :refer [defalias]])
  #?(:clj (:import java.lang.Math java.math.BigDecimal)))

(ucore/log-this-ns)

#?(:clj  (eval `(defalias ~(if (resolve `fcore/pos-int?)
                               `fcore/pos-int?
                               `core/pos-int?)))
   :cljs (defalias core/pos-int?))

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
        :else (uerr/not-supported! `integer-value? x)))

(defn >integer [x]
  (cond (integer? x) x
        (string?  x) #?(:clj  (Long/parseLong ^String x)
                        :cljs (js/parseInt            x))
        :else        (uerr/not-supported! `>integer x)))

(defn signum|long
  [^long x]
  (if (zero? x)
      x
      (bit-or 1 (bit-shift-right x 63))))

#?(:cljs (def abs js/Math.abs))
