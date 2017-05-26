(ns quantum.core.numeric.convert
  (:refer-clojure :exclude [bigdec])
  (:require
    [clojure.core                 :as core]
    [quantum.core.error           :as err
      :refer [TODO]]
    [quantum.core.macros
      :refer        [defnt #?@(:clj [defnt'])]]
    [quantum.core.vars
      :refer        [defalias]]
    [quantum.core.numeric.types   :as ntypes])
#?(:cljs
  (:require-macros
    [quantum.core.numeric.convert :as self]))
#?(:clj
  (:import
    java.math.BigInteger
    clojure.lang.BigInt)))

(defnt ->num
  (^long [^boolean x] (if x 1 0))
  ([#{number? byte char} x] x))

(defn ->boolean-num [x] (if x 1 0))

#?(:clj (defalias ->big-integer ntypes/->big-integer))

#?(:clj  (defnt' ^BigInt ->bigint
           ([^BigInt  x] x)
           ([^BigInteger x] (BigInt/fromBigInteger x))
           ([^long   x] (-> x BigInt/fromLong))
           ([^string? x radix] (->bigint (BigInteger. x (int radix))))
           ([#{double? Number} x] (-> x BigInteger/valueOf ->bigint)))
   :cljs (defalias ->bigint ntypes/->bigint))

#?(:clj  (doto (defalias ->bigdec core/bigdec) (alter-meta! assoc :tag BigDecimal))
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

#?(:clj (defalias ->ratio ntypes/->ratio)
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
  ([#{bigint? ratio?} x] x)))
