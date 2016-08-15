(ns quantum.core.numeric.convert
          (:refer-clojure :exclude [bigdec])
          (:require 
            [#?(:clj  clojure.core
                :cljs cljs.core   )     :as core  ]
            [quantum.core.error :as err
              :refer [TODO]]
            [quantum.core.macros
              :refer        [#?@(:clj [defnt defnt'])]
              :refer-macros [defnt]]
            [quantum.core.vars
              :refer        [#?@(:clj [defalias])]
              :refer-macros [defalias]]
            [quantum.core.numeric.types :as ntypes])
  #?(:clj (:import java.math.BigInteger
                   clojure.lang.BigInt)))

#?(:clj
(defnt' ^java.math.BigInteger ->big-integer
  ([^java.math.BigInteger x] x)
  ([^clojure.lang.BigInt     x] (.toBigInteger x))
  ([;#{(- number? BigInteger BigInt)} x
    #{short int long Short Integer Long} x] ; TODO BigDecimal
    (-> x core/long (BigInteger/valueOf)))))

#?(:clj  (defnt' ^clojure.lang.BigInt ->bigint
           ([^clojure.lang.BigInt  x] x)
           ([^java.math.BigInteger x] (BigInt/fromBigInteger x))
           ([^long   x] (-> x BigInt/fromLong))
           ([^string? x radix] (->bigint (BigInteger. x (int radix))))
           ([#{double? Number} x] (-> x BigInteger/valueOf ->bigint)))
   :cljs (defalias ->bigint ntypes/->bigint))

#?(:clj  (defalias ->bigdec core/bigdec)
         #_(defnt' ^BigDecimal ->bigdec
           ([^java.math.BigDecimal x] x)
           ([^BigInt x]
               (if (-> x (.bipart) nil?              )
                   (-> x (.lpart ) BigDecimal/valueOf)
                   (-> x (.bipart) (BigDecimal.)     )))
           ([^BigInteger x] (BigDecimal. x))
           ([#{(- decimal? :curr)} x] (BigDecimal. x))
           ([^Ratio x] (/ (BigDecimal. (.numerator x)) (.denominator x)))
           ([#{(- number? :curr)} x] (BigDecimal/valueOf x)))
   :cljs (defn ->bigdec [x] (TODO)))

#?(:clj (defalias ->ratio rationalize)
        #_(defnt ^clojure.lang.Ratio ->ratio
           ([^clojure.lang.Ratio   x] x)
           ([^java.math.BigDecimal x]
             (let [^BigInteger bv    (.unscaledValue x)
                   ^int        scale (.scale         x)] ; technically int
               (if (neg? scale)
                   (Ratio. (->> (neg scale)
                                (.pow BigInteger/TEN)
                                (.multiply bv))
                           BigInteger/ONE)
                   (Ratio. bv (-> BigInteger/TEN (.pow scale))))))
           ([^Object x] (-> x ->big-integer (Ratio. BigInteger/ONE))))
   :cljs (defalias ->ratio ntypes/->ratio))

#?(:clj
(defnt exactly
  ([#{decimal?} x]
    (-> x rationalize exactly))
  ([#{int? long?} x] (->bigint x))
  ([#{bigint? clojure.lang.Ratio} x] x)))