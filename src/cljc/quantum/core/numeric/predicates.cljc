(ns quantum.core.numeric.predicates
  (:refer-clojure :exclude
    [neg? pos? zero? pos-int?])
  (:require [#?(:clj  clojure.core
                :cljs cljs.core   )            :as core  ]
   #?(:cljs [com.gfredericks.goog.math.Integer :as int   ])
            [quantum.core.error :as err
              :refer [TODO]]
            [quantum.core.logic
              :refer        [#?@(:clj [fn-and fn-not])]
              :refer-macros [fn-and fn-not]]
            [quantum.core.macros
              :refer        [#?@(:clj [defnt defnt'])]
              :refer-macros [defnt defntp]])
  #?(:clj (:import
            [java.math BigInteger BigDecimal]
            [clojure.lang Ratio BigInt]
            [quantum.core Numeric])))

#?(:clj  (defnt ^boolean neg?
           ([#{byte char short int float double} x] (Numeric/isNeg x))
           ([#{BigInteger
               BigDecimal} x] (-> x .signum neg?))
           ([^Ratio     x] (-> x .numerator .signum neg?))
           ([^BigInt    x] (if (-> x .bipart         nil?)
                               (-> x .lpart          neg?)
                               (-> x .bipart .signum neg?))))
   :cljs (defnt neg?
           ([^double? x] (core/neg? x))
           ([^bigint? x] (.isNegative x))))

#?(:clj  (defnt ^boolean pos?
           ([#{byte char short int float double} x] (Numeric/isPos x))
           ([#{BigInteger
               BigDecimal} x] (-> x .signum pos?))
           ([^Ratio     x] (-> x .numerator .signum pos?))
           ([^BigInt    x] (if (-> x .bipart         nil?)
                               (-> x .lpart          pos?)
                               (-> x .bipart .signum pos?))))
   :cljs (defnt pos?
           ([^double?                           x] (core/pos? x))
           ([^com.gfredericks.goog.math.Integer x] (not (.isNegative x)))))

#?(:clj  (defnt ^boolean zero?
           ([#{byte char short int long float double} x] (Numeric/isZero x))
           ([^Ratio     x] (-> x .numerator .signum zero?))
           ([^BigInt    x] (if (nil?  (.bipart x))
                               (zero? (.lpart  x))
                               (-> x .bipart .signum zero?)))
           ([#{BigInteger
               BigDecimal} x] (-> x .signum zero?)))
   :cljs (defnt zero?
           ([^double? x] (core/zero? x))
           ([^bigint? x] (.isZero x))))

#?(:clj  (defnt nan?
           ([^double? x] (Double/isNaN x))
           ([^float?  x] (Float/isNaN  x)))
   :cljs (defn nan? [x] (TODO "fix") (identical? x js/NaN)))

(def nneg?     (fn-not neg?))
(def pos-int?  (fn-and integer? pos?))
(def nneg-int? (fn-and integer? nneg?))
(defn exact? [x] (TODO))
