(ns quantum.core.numeric.operators
  (:refer-clojure :exclude
    [+ +' - -' * *' /
     inc inc' dec dec'
     numerator denominator])
  (:require
    [clojure.core                      :as core]
  #?(:cljs
    [com.gfredericks.goog.math.Integer :as int])
    [quantum.core.error :as err
      :refer [TODO]]
    [quantum.core.log :as log]
    [quantum.core.macros.core
      :refer [#?(:clj core-symbol)]]
    [quantum.core.macros
      :refer [defnt defntp #?@(:clj [defnt' variadic-proxy])]]
    [quantum.core.vars
      :refer [defalias #?@(:clj [defmalias])]]
    [quantum.core.numeric.types :as ntypes
      :refer [numerator denominator]]
    [quantum.core.numeric.convert
      :refer [->bigint #?@(:clj [->big-integer])]])
  (:require-macros
    [quantum.core.numeric.operators :as self
      :refer [+ * -]])
  #?(:clj
  (:import
    (quantum.core Numeric)
    (java.math BigInteger BigDecimal)
    (clojure.lang BigInt Ratio))))

(log/this-ns)

; Auto-unboxes; no boxed combinations necessary
; TODO right now: multiple typed arguments in |defnt|, even in protocols
; TODO `==` from Numeric/equals

; ===== ADD ===== ;

(defn num-ex [] (throw (#?(:clj ArithmeticException. :cljs js/Error.) "Out of range")))

#?(:clj #_(defalias +*-bin unchecked-add)
        (defnt' +*-bin "Lax `+`. Continues on overflow/underflow."
          ([#{byte char short int long float double} #_(- prim? boolean) x
            #{byte char short int long float double} #_(- prim? boolean) y]
            (Numeric/add x y))
          (^BigInt     [^BigInt     x ^BigInt     y] (.add x y))
          (^BigDecimal [^BigDecimal x ^BigDecimal y]
            (if (nil? *math-context*)
                (.add x y)
                (.add x y *math-context*)))
          ([#{byte char short int long float double Number} x] x)
 #?(:cljs ([x y] (TODO) (ntypes/-add x y))))
   :cljs (defalias +*-bin unchecked-add))

#?(:clj (variadic-proxy +*  quantum.core.numeric.operators/+*-bin ))
#?(:clj (variadic-proxy +*& quantum.core.numeric.operators/+*-bin&))

#?(:clj #_(defalias +'-bin core/+')
        (defnt' +'-bin "Strict `+`. Throws exception on overflow/underflow."
          (^int  ^:intrinsic [^int  x ^int  y] (Math/addExact x y))
          (^long ^:intrinsic [^long x ^long y] (Math/addExact x y))
          (                  [#{byte char short int long float double Number} x] x))
   :cljs (defalias +'-bin core/+))

#?(:clj (variadic-proxy +'  quantum.core.numeric.operators/+'-bin ))
#?(:clj (variadic-proxy +'& quantum.core.numeric.operators/+'-bin&))

; "Natural |+|; promotes on overflow/underflow"
#?(:clj  (defalias +-bin core/+) ; TODO port
   :cljs (defalias +-bin core/+))

#?(:clj (variadic-proxy +  quantum.core.numeric.operators/+-bin))
#?(:clj (variadic-proxy +& quantum.core.numeric.operators/+-bin&))

; ===== SUBTRACT ===== ;

#?(:clj (defnt' -*-bin "Lax `-`. Continues on overflow/underflow."
          #?(:clj  ([#{byte char short int long float double} x]
                     (Numeric/negate x))
             :cljs (^first [^double? x] (TODO "fix") (ntypes/-negate x)))
          ([#{byte char short int long float double} #_(- prim? boolean) x
            #{byte char short int long float double} #_(- prim? boolean) y]
            (Numeric/subtract x y))
          (^BigInteger [^BigInteger x] (-> x .negate))
          (^BigInt     [^BigInt     x]
            (-> x ->big-integer -*-bin ->bigint)))
   :cljs (defalias -*-bin unchecked-subtract))

#?(:clj (variadic-proxy -*  quantum.core.numeric.operators/-*-bin ))
#?(:clj (variadic-proxy -*& quantum.core.numeric.operators/-*-bin&))

#?(:clj #_(defalias -'-bin core/-)
         (defnt' -'-bin "Strict `-`. Throws exception on overflow/underflow."
           (^int  ^:intrinsic [^int  x ^int  y] (Math/subtractExact x y))
           (^long ^:intrinsic [^long x ^long y] (Math/subtractExact x y))
           (^int  ^:intrinsic [^int  x] (Math/negateExact x))
           (^long ^:intrinsic [^long x] (Math/negateExact x))
           (^byte  [^byte   x] (if (Numeric/eq x Byte/MIN_VALUE   ) (num-ex) (-* x)))
           (^char  [^char   x] (if (Numeric/eq x 0                ) 0        (num-ex)))
           (^short [^short  x] (if (Numeric/eq x Short/MIN_VALUE  ) (num-ex) (-* x)))
           (^int   [^int    x] (if (Numeric/eq x Integer/MIN_VALUE) (num-ex) (-* x)))
           (^long  [^long   x] (if (Numeric/eq x Long/MIN_VALUE   ) (num-ex) (-* x))))
   :cljs (defalias -'-bin core/-))

#?(:clj (variadic-proxy -'  quantum.core.numeric.operators/-'-bin ))
#?(:clj (variadic-proxy -'& quantum.core.numeric.operators/-'-bin&))

#?(:cljs (defn --bin- [x y] (core/- x y)))  ; TODO only to fix CLJS arithmetic warning here

(defnt --bin "Natural `-`. Promotes on overflow/underflow."
  ([#?(:clj x :cljs ^double? x) y] (#?(:clj core/- :cljs --bin-) x y)))

#?(:clj (variadic-proxy -  quantum.core.numeric.operators/--bin ))
#?(:clj (variadic-proxy -& quantum.core.numeric.operators/--bin&))

; ===== MULTIPLY ===== ;

; (js/Math.imul x y) ; 32-bit int multiplication

#?(:clj (defnt' **-bin "Lax `*`. Continues on overflow/underflow."
          ([#{byte char short int long float double} #_(- prim? boolean) x
            #{byte char short int long float double} #_(- prim? boolean) y]
            (Numeric/multiply x y))
          ([^BigInt x ^BigInt y] (.multiply x y))
          ([^BigDecimal x ^BigDecimal y]
            (if (nil? *math-context*)
                (.multiply x y)
                (.multiply x y *math-context*))))
   :cljs (defn **-bin "Lax `*`. Continues on overflow/underflow."
           ([] 0)
           ([x] x)
           ([x y] (TODO "fix") (ntypes/-multiply x y))))

#?(:clj (variadic-proxy **  quantum.core.numeric.operators/**-bin ))
#?(:clj (variadic-proxy **& quantum.core.numeric.operators/**-bin&))

#?(:cljs (defn *'-bin- [x y] (TODO))) ; TODO only to fix CLJS arithmetic warning here

#?(:clj  (defnt' *'-bin "Strict `*`. Throws exception on overflow/underflow."
           (^int  ^:intrinsic [^int  x ^int  y] (Math/multiplyExact x y))
           (^long ^:intrinsic [^long x ^long y] (Math/multiplyExact x y)))
   :cljs (defnt  *'-bin
           ([x y] (*'-bin- x y))))

#?(:clj (variadic-proxy *'  quantum.core.numeric.operators/*'-bin ))
#?(:clj (variadic-proxy *'& quantum.core.numeric.operators/*'-bin&))

; "Natural |*|; promotes on overflow/underflow"
#?(:clj  (defalias *-bin core/*)
   :cljs (defalias *-bin core/*))

#?(:clj (variadic-proxy *  quantum.core.numeric.operators/*-bin ))
#?(:clj (variadic-proxy *& quantum.core.numeric.operators/*-bin&))

; ===== DIVIDE ===== ;

#?(:clj
(defnt' div*-bin-
  "Lax |/|; continues on overflow/underflow."
  {:todo #{"Doesn't preserve ratios"}}
  ([#{byte char short int long float double} n
    #{byte char short int long float double} d]
    (quantum.core.Numeric/divide n d))
  (^Number [^BigInteger n ^BigInteger d]
    (when (.equals d BigInteger/ZERO)
      (throw (ArithmeticException. "Divide by zero")))
    (let [^BigInteger gcd (.gcd n d)]
      (if (.equals gcd BigInteger/ZERO)
          BigInt/ZERO
          (let [n-f (.divide n gcd)
                d-f (.divide d gcd)]
            (cond
              (.equals d BigInteger/ONE)
                (BigInt/fromBigInteger n-f)
              (.equals d (.negate BigInteger/ONE))
                (BigInt/fromBigInteger (.negate n-f))
              :else (clojure.lang.Ratio. (if (neg? d-f) (.negate n-f) n-f)
                                         (if (neg? d-f) (.negate d-f) d-f)))))))
  ([^clojure.lang.BigInt n ^clojure.lang.BigInt d]
    (div*-bin- (->big-integer n) (->big-integer d)))
  ([^java.math.BigDecimal n ^java.math.BigDecimal d]
    (if (nil? *math-context*)
        (.divide n d)
        (.divide n d *math-context*)))))

; TODO you lose the |defnt'| power with this
; TODO have |defnt| handle all this automagically

#?(:clj (defnt div*-bin-denom-byte
  ([#{byte char short int long float double} d ^byte   n] (div*-bin- n d))))
#?(:clj (defnt div*-bin-denom-char
  ([#{byte char short int long float double} d ^char   n] (div*-bin- n d))))
#?(:clj (defnt div*-bin-denom-short
  ([#{byte char short int long float double} d ^short  n] (div*-bin- n d))))
#?(:clj (defnt div*-bin-denom-int
  ([#{byte char short int long float double} d ^int    n] (div*-bin- n d))))
#?(:clj (defnt div*-bin-denom-long
  ([#{byte char short int long float double} d ^long   n] (div*-bin- n d))))
#?(:clj (defnt div*-bin-denom-float
  ([#{byte char short int long float double} d ^float  n] (div*-bin- n d))))
#?(:clj (defnt div*-bin-denom-double
  ([#{byte char short int long float double} d ^double n] (div*-bin- n d))))

#?(:cljs (defn div*-bin- [x y] (core// x y)))  ; TODO only to fix CLJS arithmetic warning here

; TODO optimization: (/ x 4) = (& x 3)
#?(:clj  (defntp div*-bin "Lax `/`. Continues on overflow/underflow."
           ([^byte   n d] (div*-bin-denom-byte-protocol   d n))
           ([^char   n d] (div*-bin-denom-char-protocol   d n))
           ([^short  n d] (div*-bin-denom-short-protocol  d n))
           ([^int    n d] (div*-bin-denom-int-protocol    d n))
           ([^long   n d] (div*-bin-denom-long-protocol   d n))
           ([^float  n d] (div*-bin-denom-float-protocol  d n))
           ([^double n d] (div*-bin-denom-double-protocol d n))
           (^Number [^java.math.BigInteger n ^java.math.BigInteger d]
             (div*-bin- n d))
           ([^clojure.lang.BigInt  n ^clojure.lang.BigInt  d]
             (div*-bin- n d))
           ([^java.math.BigDecimal n ^java.math.BigDecimal d]
             (div*-bin- n d)))
   :cljs (defnt div*-bin "Lax `/`. Continues on overflow/underflow."
           ([^ratio? x  ] (TODO "fix") (ntypes/-invert x))
           ([^ratio? x y]
             (TODO "fix")
              ;(* x (-invert (apply * y more)))
              (* x (ntypes/-invert y)))
           ([^double? x  ] (core// x))
           ([^double? x y] (div*-bin- x y))))

#?(:clj (variadic-proxy div* quantum.core.numeric.operators/div*-bin))

; "Strict |/|. Throws exception on overflow/underflow."
#?(:clj  (defalias div'-bin core//) ; TODO explore
   :cljs (defalias div'-bin core//))

#?(:clj (variadic-proxy div' quantum.core.numeric.operators/div'-bin))

; "Natural |/|. Promotes on overflow/underflow."
#?(:clj  (defalias div-bin core//) ; TODO explore
   :cljs (defalias div-bin core//))

#?(:clj (variadic-proxy div quantum.core.numeric.operators/div-bin))
#?(:clj (defmalias / quantum.core.numeric.operators/div))

;_____________________________________________________________________
;==================={   UNARY MATH OPERATORS   }======================
;°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°

#?(:clj (defnt' dec* "Lax `dec`. Continues on overflow/underflow."
          ([#{byte char short long float double} x] (quantum.core.Numeric/dec x))
          ([^clojure.lang.BigInt x]
            (-> x ->big-integer (.subtract BigInteger/ONE) BigInt/fromBigInteger))
          ([^java.math.BigDecimal x]
            (if (nil? *math-context*)
                (.subtract x BigDecimal/ONE)
                (.subtract x BigDecimal/ONE *math-context*))))
   :cljs (defnt dec* ([^double? x] (unchecked-dec x)))) ; TODO CLJS arithmetic warning here

#?(:clj  (defalias dec' core/dec)
         #_(defnt' dec' "Strict |dec|; throws exception on overflow/underflow"
             (^:first  ^:intrinsic [#{int long} x] (Math/decrementExact x))
             ([^long x]
               (if (== x Long/MIN_VALUE)
                   (throw num-ex)
                   (-* x 1))))
   :cljs (defalias dec' core/dec))

#?(:clj  (defalias dec core/dec)
         #_(defnt' dec "Natural |dec|; promotes on overflow/underflow"
             (^Number [^long x]
               (TODO "boxes value... how to fix?")
               (if (== x Long/MAX_VALUE)
                   (-> x ->bigint dec*)
                   (-* x 1))))
   :cljs (defnt dec
           ([^double? x] (core/dec x))
           ([^bigint? x] (- x int/ONE))))

#?(:clj  (defnt' inc* "Lax `inc`. Continues on overflow/underflow."
           ([#{byte char short int long float double} x] (quantum.core.Numeric/inc x))
           ([^BigInt x]
             (-> x ->big-integer (.subtract BigInteger/ONE) BigInt/fromBigInteger))
           ([^BigDecimal x]
             (if (nil? *math-context*)
                 (.add x BigDecimal/ONE)
                 (.add x BigDecimal/ONE *math-context*))))
   :cljs (defnt inc* ([^double? x] (unchecked-inc x))))

#?(:clj  (defalias inc core/inc)
         #_(defnt' inc "Natural |inc|; promotes on overflow/underflow"
             (^Number [^long x]
               (TODO "boxes value... how to fix?")
               (if (== x Long/MAX_VALUE)
                   (-> x ->bigint inc*)
                   (+* x 1))))
   :cljs (defnt inc
           ([^double? x] (core/inc x))
           ([^bigint? x] (+ x int/ONE))))

#?(:clj  (defalias inc' inc          )
         #_(defnt' inc' "Strict |inc|; throws exception on overflow/underflow"
             (^:first ^:intrinsic [#{int long} x] (Math/incrementExact x))
             ([^long x]
               (if (== x Long/MAX_VALUE)
                   (throw num-ex)
                   (+* x 1))))
   :cljs (defalias inc' inc          ))

#?(:clj (defnt abs'
          ([#{int long double} x] (Math/abs x))
          (^float ^:intrinsic [^float  x] (Math/abs x))
          (^BigDecimal [^BigDecimal x]
            (.abs x))
          (^BigDecimal [^BigDecimal x math-context]
            (.abs x math-context))
          (^BigInteger [^BigInteger x]
            (.abs x))
          (^BigInt [^BigInt x]
            (if (nil? (.bipart x))
                (BigInt/fromLong       (abs' (.lpart  x)))
                (BigInt/fromBigInteger (abs' (.bipart x)))))
          (^Ratio [^ratio? x] ; TODO this might be an awful implementation
            (/ (abs' (numerator   x))
               (abs' (denominator x)))))
   :cljs (defnt abs' ([x] (TODO "incomplete") (js/Math.abs x))))

#?(:clj (defalias abs abs'))

#?(:clj (defmacro int-nil [x] `(let [x# ~x] (if (nil? x#) 0 x#))))

#?(:clj
(defmacro zeros-op
  "Treats nils like 0"
  [op]
  (let [op'     (if (= op '/) '-div op)
        sym     (symbol (str "zeros" op'))
        core-op (core-symbol &env op)]
    `(defn ~sym
       ([a#      ] (~core-op (int-nil a#)))
       ([a# b#   ] (~core-op (int-nil a#) (int-nil b#)))
       ([a# b# c#] (~core-op (int-nil a#) (int-nil b#) (int-nil c#)))
       ([a# b# c# & args#] (->> (conj args# c# b# a#)
                                (clojure.core.reducers/map #(int-nil %))
                                (reduce ~core-op)))))))

#?(:clj
(defmacro nils-op
  "If any nils are present, returns nil"
  [op]
  (let [op'     (if (= op '/) '-div op)
        sym     (symbol (str "nils" op'))
        core-op (core-symbol &env op)]
    `(defn ~sym
       ([a#      ] (when a# (~core-op a#)))
       ([a# b#   ] (when (and a# b#) (~core-op a# b#)))
       ([a# b# c#] (when (and a# b# c#) (~core-op a# b# c#)))
       ([a# b# c# & args#]
         (let [argsf# (conj args# c# b# a#)]
           (when (every? some? argsf#) (reduce ~core-op argsf#))))))))
