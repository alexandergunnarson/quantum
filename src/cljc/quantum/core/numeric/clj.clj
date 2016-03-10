(ns
  ^{:doc "Useful numeric functions. Floor, ceil, round, sin, abs, neg, etc."
    :attribution "Alex Gunnarson"}
  quantum.core.numeric.clj
  (:refer-clojure :exclude
    [* *' + +' - -' / < > <= >= == inc inc' dec dec'
     zero? neg? pos? min max format
     rem quot mod
     bigint biginteger bigdec])
  (:require-quantum [:core logic type fn macros err log pconvert])
  (:require [quantum.core.convert.primitive :as prim :refer [->unboxed]]
            [clojure.walk :refer [postwalk]]
            [quantum.core.numeric.types :as ntypes])
  (:import [java.nio ByteBuffer]
           [quantum.core Numeric] ; loops?
           [net.jafama FastMath]
           clojure.lang.BigInt
           java.math.BigDecimal)) 

(defmacro validate-exact
  [arg]
  (when *assert*
    `(let [arg# ~arg]
       (when-not (exact? arg#)
         (throw (->ex nil "Bad argument type" arg#))))))

; TODO implement
(def exact? identity)

;_____________________________________________________________________
;==================={       CONVERSIONS        }======================
;°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°
(defnt' ^java.math.BigInteger ->big-integer
  ([^java.math.BigInteger x] x)
  ([^clojure.lang.BigInt     x] (.toBigInteger x))
  ([;#{(- number? BigInteger BigInt)} x
    #{short int long Short Integer Long} x] ; TODO BigDecimal
    (-> x core/long (BigInteger/valueOf))))

(defnt' ^clojure.lang.BigInt ->bigint
  ([^clojure.lang.BigInt  x] x)
  ([^java.math.BigInteger x] (BigInt/fromBigInteger x))
  ([^long   x] (-> x BigInt/fromLong))
  ([^string? x radix] (->bigint (BigInteger. x (int radix))))
  ([#{double? Number} x] (-> x BigInteger/valueOf ->bigint)))

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

(defalias ->bigdec core/bigdec) ; TODO temporary

(defalias bigdec ->bigdec)

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
  ([^Object x] (-> x ->big-integer (Ratio. BigInteger/ONE)))
  )

(defalias ->ratio rationalize)

  ;_____________________________________________________________________
;==================={        OPERATORS         }======================
;°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°
; Auto-unboxes; no boxed combinations necessary

; ===== ADD =====

(defnt' +-2
  (^{:tag :auto-promote}
    [#{byte char short int long float double} #_(- primitive? boolean) x
     #{byte char short int long float double} #_(- primitive? boolean) y]
    (quantum.core.Numeric/add x y))
  (^clojure.lang.BigInt  [^clojure.lang.BigInt  x ^clojure.lang.BigInt  y]
    (.add x y))
  (^java.math.BigDecimal [^java.math.BigDecimal x ^java.math.BigDecimal y]
    (if (nil? *math-context*)
        (.add x y)
        (.add x y *math-context*))))

; TODO waiting on +'
#_(defn +-exact
  [x y]
  (validate-exact x)
  (validate-exact y)
  (+' x y))

#_(defnt' +'  ; TODO take out auto-quote generator
  (^int  ^:intrinsic [^int  x ^int  y] (Math/addExact x y))
  (^long ^:intrinsic [^long x ^long y] (Math/addExact x y))
  (^long [^long x] ; TODO boxes value... how to fix?
    (if (== x Long/MAX_VALUE)
        (throw num-ex)
        (+* x))))

; ===== SUBTRACT =====

; minus is just add negated

(defnt' --base
  (^{:tag :first} [#{byte char short int long float double} x] (quantum.core.Numeric/negate x))
  (^{:tag :auto-promote}
    [#{byte char short int long float double} #_(- primitive? boolean) x
     #{byte char short int long float double} #_(- primitive? boolean) y]
    (quantum.core.Numeric/subtract x y))
  (^java.math.BigInteger [^java.math.BigInteger x] (-> x .negate))
  (^clojure.lang.BigInt  [^clojure.lang.BigInt  x]
    (-> x ->big-integer --base ->bigint)))

; TODO awaiting -'
#_(defn negate-exact
  [x]
  (validate-exact x)
  (-' x))

; (-* 3) is 3...

#_(defnt' -'  ; TODO take out auto-quote generator
  (^int  ^:intrinsic [^int  x ^int  y] (Math/subtractExact x y))
  (^long ^:intrinsic [^long x ^long y] (Math/subtractExact x y))
  (^int  ^:intrinsic [^int  x] (Math/negateExact x))
  (^long ^:intrinsic [^long x] (Math/negateExact x))
  (^long [^long x] ; TODO boxes value... how to fix?
    (if (== x Long/MIN_VALUE)
        (throw num-ex)
        (-* x))))

; TODO fix
(def -' core/-)

#_(defnt' - ; TODO take out auto-quote generator
  (^Number [^long x] ; TODO boxes value... how to fix?
    (if (== x Long/MIN_VALUE)
        (-> x ->big-integer -* ->bigint)
        (-* x))))

(defnt' dec* "Unchecked |dec|"
  (^{:tag :first} [#{byte char short long float double} x] (quantum.core.Numeric/dec x))
  ([^clojure.lang.BigInt x]
    (-> x ->big-integer (.subtract BigInteger/ONE) BigInt/fromBigInteger))
  ([^java.math.BigDecimal x]
    (if (nil? *math-context*)
        (.subtract x BigDecimal/ONE)
        (.subtract x BigDecimal/ONE *math-context*))))

(defnt' inc* "Unchecked |inc|"
  (^{:tag :first} [#{byte char short int long float double} x] (quantum.core.Numeric/inc x))
  ([^clojure.lang.BigInt x]
    (-> x ->big-integer (.subtract BigInteger/ONE) BigInt/fromBigInteger))
  ([^java.math.BigDecimal x]
    (if (nil? *math-context*)
        (.add x BigDecimal/ONE)
        (.add x BigDecimal/ONE *math-context*))))

;_____________________________________________________________________
;==================={   UNARY MATH OPERATORS   }======================
;°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°

#_(defnt' dec'
  (^:first  ^:intrinsic [#{int long} x] (Math/decrementExact x))
  ([^long x]
    (if (== x Long/MIN_VALUE)
        (throw num-ex)
        (-* x 1))))

; TODO for now
(defalias dec' core/inc)

#_(defnt' dec
  (^Number [^long x] ; TODO boxes value... how to fix?
    (if (== x Long/MAX_VALUE)
        (-> x ->bigint dec*)
        (-* x 1))))

; TODO for now
(defalias dec core/inc)

#_(defnt' inc' "Strict inc, throwing exception on overflow"
  (^:first ^:intrinsic [#{int long} x] (Math/incrementExact x))
  ([^long x]
    (if (== x Long/MAX_VALUE)
        (throw num-ex)
        (+* x 1))))

; TODO for now
(defalias inc' core/inc)

#_(defnt' inc "Natural inc, promoting on overflow"
  (^Number [^long x] ; TODO boxes value... how to fix?
    (if (== x Long/MAX_VALUE)
        (-> x ->bigint inc*)
        (+* x 1))))

; TODO for now
(defalias inc core/inc)

(defnt' *-2
  (^{:tag :auto-promote}
    [#{byte char short int long float double} #_(- primitive? boolean) x
     #{byte char short int long float double} #_(- primitive? boolean) y]
    (quantum.core.Numeric/multiply x y))
  ([^clojure.lang.BigInt x ^clojure.lang.BigInt y]
    (.multiply x y))
  ([^java.math.BigDecimal x ^java.math.BigDecimal y]
    (if (nil? *math-context*)
        (.multiply x y)
        (.multiply x y *math-context*))))

; (js/Math.imul x y) ; 32-bit int multiplication
(defnt' *' "Throws an exception on overflow"
  (^int  ^:intrinsic [^int  x ^int  y] (Math/multiplyExact x y))
  (^long ^:intrinsic [^long x ^long y] (Math/multiplyExact x y)))

(defn *-exact
  [x y]
  (validate-exact x)
  (validate-exact y)
  (*' x y))

(defnt ^boolean neg?
  ([#{byte char short int float double} x] (quantum.core.Numeric/isNeg x))
  ([#{java.math.BigInteger
      java.math.BigDecimal} x] (-> x .signum neg?))
  ([^clojure.lang.Ratio     x] (-> x .numerator .signum neg?))
  ([^clojure.lang.BigInt    x] (if (-> x .bipart         nil?)
                                   (-> x .lpart          neg?)
                                   (-> x .bipart .signum neg?))))

(defnt ^boolean pos?
  ([#{byte char short int float double} x] (quantum.core.Numeric/isPos x))
  ([#{java.math.BigInteger
      java.math.BigDecimal} x] (-> x .signum pos?))
  ([^clojure.lang.Ratio     x] (-> x .numerator .signum pos?))
  ([^clojure.lang.BigInt    x] (if (-> x .bipart         nil?)
                                   (-> x .lpart          pos?)
                                   (-> x .bipart .signum pos?))))

; TODO do |/|
#_(defn invert-exact
  [x]
  (validate-exact x)
  (/ x))

; (defn compare-exact
;   [x y]
;   (validate-exact x)
;   (validate-exact y)
;   (core/compare x y))

(defnt' div-2
  (^double
    [#{byte char short int long float double} n
     #{byte char short int long float double} d]
    (quantum.core.Numeric/divide n d))
  (^Number [^java.math.BigInteger n ^java.math.BigInteger d]
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
    (div-2 (->big-integer n) (->big-integer d)))
  ([^java.math.BigDecimal x ^java.math.BigDecimal y]
    (if (nil? *math-context*)
        (.divide x y)
        (.divide x y *math-context*))))

; (/ x 4) = (& x 3)

(defnt' not-num?
  ([^double? x] (Double/isNaN x))
  ([^float?  x] (Float/isNaN  x)))

;_____________________________________________________________________
;==================={        PREDICATES        }======================
;°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°
(defmacro <-2  [a b] `(quantum.core.Numeric/lt  ~a ~b))
(defmacro <=-2 [a b] `(quantum.core.Numeric/lte ~a ~b))
(defmacro >-2  [a b] `(quantum.core.Numeric/gt  ~a ~b))
(defmacro >=-2 [a b] `(quantum.core.Numeric/gte ~a ~b))

(defnt' =-2
  (^boolean
    [#{byte char short int long float double} x
     #{byte char short int long float double} y]
    (quantum.core.Numeric/eq x y))
  (^boolean [^clojure.lang.BigInt x ^clojure.lang.BigInt y]
    (.equals x y)))

(defnt' not=-2
  (^boolean
    [#{byte char short int long float double} x
     #{byte char short int long float double} y]
    (not (=-2 x y))) ; TODO use primitive |not| fn
  (^boolean [^clojure.lang.BigInt x ^clojure.lang.BigInt y]
    (not (=-2 x y)))) ; TODO use primitive |not| fn

; (defn ==
;   [x y]
;   (validate-exact x)
;   (validate-exact y)
;   (core/= x y))

(defnt' ^boolean zero?
  ([#{byte char short long float double} x] (quantum.core.Numeric/isZero x))
  ([^clojure.lang.Ratio     x] (-> x .numerator .signum zero?))
  ([^clojure.lang.BigInt    x] (if (nil?  (.bipart x))
                                   (zero? (.lpart  x))
                                   (-> x .bipart .signum zero?)))
  ([#{java.math.BigInteger
      java.math.BigDecimal} x] (-> x .signum zero?)))

;_____________________________________________________________________
;================={   MORE COMPLEX OPERATIONS    }====================
;°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°

(defmacro rem [n div] `(Numeric/rem ~n ~div))

; TODO for now
(defalias quot core/quot)
; TODO for now
(defalias mod core/mod)

#_(defnt' max-2
  (^{:tag :largest}
    [#{byte char short int float double} x
     #{byte char short int float double} y]
    (quantum.core.Numeric/max x y)))

; TODO for now
(defalias max-2 core/max)

#_(defnt' min-2
  (^{:tag :largest}
    [#{byte char short int float double} x
     #{byte char short int float double} y]
    (quantum.core.Numeric/min x y)))

; TODO for now
(defalias min-2 core/min)

#_(when-not @override-fns?
  (doseq [sym overridden-fns]
    (let [qualified-sym (symbol "core" (name sym))]
      (eval (quote+ (defalias ~sym ~qualified-sym))))))

(defn abs [x] (if (core/neg? x) (core/- x) x))

(defnt' abs'
  (^int               [^int    x] (Math/abs x))
  (^long              [^long   x] (Math/abs x))
  (^double            [^double x] (Math/abs x))
  (^float ^:intrinsic [^float  x] (Math/abs x))
  (^java.math.BigDecimal [^java.math.BigDecimal x]
    (.abs x))
  (^java.math.BigDecimal [^java.math.BigDecimal x math-context]
    (.abs x math-context))
  (^java.math.BigInteger [^java.math.BigInteger x]
    (.abs x))
  (^clojure.lang.BigInt [^clojure.lang.BigInt x]
    (if (nil? (.bipart x))
        (clojure.lang.BigInt/fromLong       (abs (.lpart  x)))
        (clojure.lang.BigInt/fromBigInteger (abs (.bipart x)))))
  (^clojure.lang.Ratio [^clojure.lang.Ratio x] ; TODO this might be an awful implementation
    (div-2 (abs' (numerator   x))
           (abs' (denominator x)))))

(defnt' next-after
  (^double [^double start ^double direction] (Math/nextAfter start direction))
  (^float  [^float  start ^double direction] (Math/nextAfter start direction)))

(defnt' next-down
  (^double [^double x] (Math/nextDown x))
  (^float  [^float  x] (Math/nextDown x)))

(defnt' next-up
  (^double [^double x] (Math/nextUp x))
  (^float  [^float  x] (Math/nextUp x)))

(defnt' cube-root
  (^double [^double x] (Math/cbrt x)))

(defnt cube-root*
  "returns angle theta"
  {:performance ["6.2 times faster than java.lang.Math"
                 "Worst case 2E-14 difference"]}
  (^double [^double x] (FastMath/cbrt x)))

(defnt' sqrt
  (^double ^:intrinsic [^double x] (Math/sqrt x)))

;_____________________________________________________________________
;================={   TRIGONOMETRIC FUNCTIONS    }====================
;°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°

; ===== SINE ===== ;

(defnt' asin "arc sine"
  (^double [^double x] (Math/asin x)))

(defnt asin*
  "arc sine"
  {:performance ["3.8 times faster than java.lang.Math"
                 "Worst case 2E-12 difference"]}
  (^double [^double x] (FastMath/asin x)))

(defnt' sin "sine"
  (^double ^:intrinsic [^double x] (Math/sin x)))

(defnt sin*
  "sine"
  {:performance ["4.5 times faster than java.lang.Math"
                 "Worst case 1E-11 difference"]}
  (^double [^double x] (FastMath/sin x)))

(defnt' sinh "hyperbolic sine"
  (^double [^double x] (Math/sinh x)))

(defnt sinh*
  "hyperbolic sine"
  {:performance ["5.5 times faster than java.lang.Math"
                 "Worst case 7E-14 difference"]}
  (^double [^double x] (FastMath/sinh x)))

; ===== COSINE ===== ;

(defnt acos "arc cosine"
  (^double [^double x] (Math/acos x)))

(defnt acos*
  "arc cosine"
  {:performance ["3.6 times faster than java.lang.Math"
                 "Worst case 1E-12 difference"]}
  (^double [^double x] (FastMath/acos x)))

(defnt' cos "cosine"
  (^double ^:intrinsic [^double x] (Math/cos x)))

(defnt' cos*
  "cosine"
  {:performance ["5.7 times faster than java.lang.Math"
                 "Worst case 8E-12 difference"]}
  (^double [^double x] (FastMath/cos x)))

(defnt' cosh "hyperbolic cosine"
  (^double [^double x] (Math/cosh x)))

(defnt' cosh*
  "hyperbolic cosine"
  {:performance ["5 times faster than java.lang.Math"
                 "Worst case 4E-14 difference"]}
  (^double [^double x] (FastMath/cosh x)))

; ===== TANGENT ===== ;

(defnt' atan "arc tangent"
  (^double [^double x] (Math/atan x)))

(defnt atan*
  "arc tangent"
  {:performance ["6.2 times faster than java.lang.Math"
                 "Worst case 5E-13 difference"]}
  (^double [^double x] (FastMath/atan x)))

(defnt' atan2 "returns angle theta"
  (^double ^:intrinsic [^double x ^double y] (Math/atan2 x y)))

(defnt atan2*
  "returns angle theta"
  {:performance ["6.3 times faster than java.lang.Math"
                 "Worst case 4E-13 difference"]}
  (^double [^double x ^double y] (FastMath/atan2 x y)))

(defnt' tan "tangent"
  (^double ^:intrinsic [^double x] (Math/tan x)))

(defnt tan*
  "tangent"
  {:performance ["3.7 times faster than java.lang.Math"
                 "Worst case 1E-13 difference"]}
  (^double [^double x] (FastMath/tan x)))

(defnt' tanh "hyperbolic tangent"
  (^double [^double x] (Math/tanh x)))

(defnt tanh*
  "hyperbolic tangent"
  {:performance ["6.4 times faster than java.lang.Math"
                 "Worst case 5E-14 difference"]}
  (^double [^double x] (FastMath/tanh x)))

; ===== DEGREES + RADIANS ===== ;

(defnt' radians->degrees
  (^double [^double x] (Math/toDegrees x)))

(defnt' degrees->radians
  (^double [^double x] (Math/toRadians x)))

;_____________________________________________________________________
;==============={   OTHER MATHEMATICAL FUNCTIONS   }==================
;°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°

; ===== POWERS, EXPONENTS, LOGARITHMS, ROOTS ===== ;

(defnt' e-exp "Euler's number (e) raised to the power of @x"
  (^double ^:intrinsic [^double x] (Math/exp x)))

(defnt' e-exp*
  "Euler's number (e) raised to the power of @x"
  {:performance ["4.6 times faster than java.lang.Math"
                 "Worst case 4E-14 difference"]}
  (^double [^double x] (FastMath/exp x)))

(defnt' log-e "Natural logarithm"
  (^double ^:intrinsic [^double x] (Math/log x)))

(defnt' log-e*
  "Natural logarithm"
  {:performance ["1.9 times faster than java.lang.Math"
                 "Worst case 3E-14 difference"]}
  (^double [^double x] (FastMath/log x)))

(defnt' log-10 "Logarithm, base 10"
  (^double ^:intrinsic [^double x] (Math/log10 x)))

(defnt' log-10*
  "Logarithm, base 10"
  {:performance ["2.1 times faster than java.lang.Math"
                 "Worst case 6E-14 difference"]}
  (^double [^double x] (FastMath/log10 x)))

(defnt' log1p*
  "Much more accurate than log(1+value) for arguments (and results) close to zero."
  {:performance ["6.5 times faster than java.lang.Math"
                 "Worst case 2E-14 difference"]}
  (^double [^double x] (FastMath/log1p x)))

(defnt' exp'
  {:todo ["Performance" "Rename"]}
  [#{byte char short int float double} x #{long? double?} n]
  (loop [acc (Long. 1) nn n]
    (if (<=-2 (double nn) 0) acc
        (recur (core/*' x acc) (dec* nn)))))

(defnt exp "|exp| and not |pow| because 'exponent'."
  (^double ^:intrinsic [^double   x ^double y] (Math/pow x y))
  (^double             [^integer? x         y] (exp (core/double x) (core/double y))))

;pow'
; Only works with integers larger than zero.
; private double pow'(double d, int exp) {
;     double r = d;
;     for(int i = 1; i<exp; i++) {
;         r *= d;
;     }
;     return r;
; }

(defnt' exp*
  "|exp| and not |pow| because 'exponent'."
  {:performance ["2.7 times faster than java.lang.Math"
                 "Worst case 1E-11 difference"]}
  (^double [^double x ^double y] (FastMath/pow x y)))

(defnt' expm1*
  "Much more accurate than exp(value)-1 for arguments (and results) close to zero."
  {:performance ["6.6 times faster than java.lang.Math"
                 "Worst case 5E-14 difference"]}
  (^double [^double x] (FastMath/expm1 x)))

(defnt' get-exp "Unbiased exponent used in @x"
  (^int [^double x] (Math/getExponent x))
  (^int [^float  x] (Math/getExponent x)))

; ===== ROUNDING =====

(defnt' rint "The double value that is closest in value to @x and is equal to a mathematical integer."
  (^double [^double x] (Math/rint x)))

(defn round
  "Probably deprecated; use:
   |(with-precision <decimal-places> (bigdec <number>))|"
  {:todo ["Port to cljs"]}
  [num-0 & {:keys [type to] :or {to 0}}]
  (let [round-type
          (if (nil? type)
              (. BigDecimal ROUND_HALF_UP)
              (case type
                :unnecessary BigDecimal/ROUND_UNNECESSARY
                :ceiling     BigDecimal/ROUND_CEILING
                :up          BigDecimal/ROUND_UP
                :half-up     BigDecimal/ROUND_HALF_UP
                :half-even   BigDecimal/ROUND_HALF_DOWN
                :half-down   BigDecimal/ROUND_HALF_DOWN
                :down        BigDecimal/ROUND_DOWN
                :floor       BigDecimal/ROUND_FLOOR))]
    (.setScale (bigdec num-0) ^Integer to round-type)))

(defnt' round' "Rounds up in cases of ambiguity."
  (^long                 [^double               x] (Math/round x))
  (^long                 [^float                x] (Math/round x))
  (^java.math.BigDecimal [^java.math.BigDecimal x math-context]
    (.round x math-context))
  (^java.math.BigDecimal [^clojure.lang.Ratio x]
    (round (->double x)))
  (^java.math.BigDecimal [^clojure.lang.Ratio x math-context]
    (round (->bigdec x) math-context)))

; ===== TRUNCATING ===== ;

(defnt' ceil
  (^double [^double x] (Math/ceil x)))

(defnt' floor
  (^double [^double x] (Math/floor x)))

(defnt' floor-div
  (^int  [^int  x ^int  y] (Math/floorDiv x y))
  (^long [^long x ^long y] (Math/floorDiv x y)))

(defnt' floor-mod
  (^int  [^int  x ^int  y] (Math/floorMod x y))
  (^long [^long x ^long y] (Math/floorMod x y)))

(defnt' hypot "(sqrt (^ x 2) (^ y 2)) without intermediate over/underflow"
  (^double [^double x ^double y] (Math/hypot x y)))

(defnt' hypot*
  "(sqrt (^ x 2) (^ y 2)) without intermediate over/underflow"
  {:performance ["18.9 times faster than java.lang.Math"
                 "Worst case 3E-14 difference"]}
  (^double [^double x ^double y] (FastMath/hypot x y)))

(defnt' ieee-remainder "The remainder operation on two arguments as prescribed by the IEEE 754 standard"
  (^double [^double x ^double y] (Math/IEEEremainder x y)))

; ===== MISCELLANEOUS ===== ;

(defnt' with-sign "Returns @x with the sign of @y."
  (^double [^double x ^double y] (Math/copySign x y))
  (^float  [^float  x ^float  y] (Math/copySign x y)))

(defnt' scalb
  "Returns (x * 2) ^ y, rounded as if performed by a single correctly rounded
   floating-point multiply to a member of the double value set."
  (^double [^double x ^int y] (Math/scalb x y))
  (^float  [^float  x ^int y] (Math/scalb x y)))

(defn sign [n] (if (neg? n) -1 1))

(defnt' sign'
  "Zero if the argument is zero,
   1.0 if the argument is greater than zero,
   -1.0 if the argument is less than zero."
  (^double [^double x] (Math/signum x))
  (^float  [^float  x] (Math/signum x)))

(defnt' ulp "Size of an ulp (?) of @x"
  (^double [^double x] (Math/ulp x))
  (^float  [^float  x] (Math/ulp x)))

; TODO "can't type hint primitive local"
#_(defnt exactly
 ([^integer?           n] (bigint  n)
 ([^decimal?           n] (rationalize #_->ratio n))
 ([^clojure.lang.Ratio n] n)))

; (defn whole? [n]
;   (assert (instance? Double n))
;   (= (mod n 1) 0.0))

(def- two-to-fifty-three
  (apply core/* (repeat 53 2)))

(def- minus-two-to-fifty-three
  (core/- two-to-fifty-three))

(defn native-integer?
  [n]
  (and (number? n)
       (core/<= minus-two-to-fifty-three
           n
           two-to-fifty-three)
       (core/integer? n)))

(defn gcd
  "(gcd a b) computes the greatest common divisor of a and b."
  ([a b]
  (if (zero? b)
      a
      (recur b (mod a b))))
  ([a b & args]
    (reduce gcd (gcd a b) args)))

;___________________________________________________________________________________________________________________________________
;=================================================={         MUTATION         }=====================================================
;=================================================={                          }=====================================================
(defmacro += [x a] `(set! ~x (+-2 ~x ~a)))
(defmacro -= [x a] `(set! ~x (--base ~x ~a)))

(defmacro ++ [x] `(+= ~x 1))
(defmalias inc! quantum.core.numeric/++)

(defmacro -- [x] `(-= ~x 1))
(defmalias dec! quantum.core.numeric/--)

;___________________________________________________________________________________________________________________________________
;=================================================={         DISPLAY          }=====================================================
;=================================================={                          }=====================================================
(def display-num (fn-> double (round :to 2)))

(defn format [n type]
  (condp core/= type
    :dollar
      (->> n display-num (str "$"))
    ;:accounting
    (throw (->ex nil "Unrecognized format" type))))

(defn percentage-of [of total-n]
  (-> of (core// total-n) (core/* 100) display-num (str "%")))