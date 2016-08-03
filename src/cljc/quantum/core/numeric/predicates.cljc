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
              :refer-macros [defnt defntp]]))

#?(:clj  (defnt ^boolean neg?
           ([#{byte char short int float double} x] (quantum.core.Numeric/isNeg x))
           ([#{java.math.BigInteger
               java.math.BigDecimal} x] (-> x .signum neg?))
           ([^clojure.lang.Ratio     x] (-> x .numerator .signum neg?))
           ([^clojure.lang.BigInt    x] (if (-> x .bipart         nil?)
                                            (-> x .lpart          neg?)
                                            (-> x .bipart .signum neg?))))
   :cljs (defnt neg?
           ([^number?                           x] (core/neg? x))
           ([^com.gfredericks.goog.math.Integer x] (.isNegative x))))

#?(:clj  (defnt ^boolean pos?
           ([#{byte char short int float double} x] (quantum.core.Numeric/isPos x))
           ([#{java.math.BigInteger
               java.math.BigDecimal} x] (-> x .signum pos?))
           ([^clojure.lang.Ratio     x] (-> x .numerator .signum pos?))
           ([^clojure.lang.BigInt    x] (if (-> x .bipart         nil?)
                                            (-> x .lpart          pos?)
                                            (-> x .bipart .signum pos?))))
   :cljs (defnt pos?
           ([^number?                           x] (core/pos? x))
           ([^com.gfredericks.goog.math.Integer x] (not (.isNegative x)))))

#?(:clj  (defnt ^boolean zero?
           ([#{byte char short float double} x] (quantum.core.Numeric/isZero x))
           ([#{long}                         x] (-> x int zero?))
           ([^clojure.lang.Ratio     x] (-> x .numerator .signum zero?))
           ([^clojure.lang.BigInt    x] (if (nil?  (.bipart x))
                                            (zero? (.lpart  x))
                                            (-> x .bipart .signum zero?)))
           ([#{java.math.BigInteger
               java.math.BigDecimal} x] (-> x .signum zero?)))
   :cljs (defnt zero?
           ([^number?                           x] (core/zero? x))
           ([^com.gfredericks.goog.math.Integer x] (.isZero x))))

#?(:clj  (defnt nan?
           ([^double? x] (Double/isNaN x))
           ([^float?  x] (Float/isNaN  x)))
   :cljs (defn nan? [x] (TODO "fix") (identical? x js/NaN)))

(def nneg?     (fn-not neg?))
(def pos-int?  (fn-and integer? pos?))
(def nneg-int? (fn-and integer? nneg?))
(defn exact? [x] (TODO))