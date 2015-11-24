(ns
  ^{:doc "Useful numeric functions. Floor, ceil, round, sin, abs, neg, etc."
    :attribution "Alex Gunnarson"}
  quantum.core.numeric
  (:refer-clojure :exclude
    [* *' + +' - -' / < > <= >= == rem inc dec zero? min max format
     bigint biginteger bigdec])
  (:require-quantum [ns logic type fn macros err log pconvert])
  (:require [quantum.core.convert.primitive :as prim :refer [->unboxed]])
  #?(:clj (:import [java.nio ByteBuffer]
                   [quantum.core Numeric] ; loops?
                   [net.jafama FastMath]
                   clojure.lang.BigInt
                   java.math.BigDecimal))) 
  
; http://blog.juma.me.uk/2011/02/23/performance-of-fastmath-from-commons-math/
; http://blog.element84.com/improving-java-math-perf-with-jafama.html

; If you want your software to have the exact same result regardless 
; of hardware. Java provides the StrictMath class for that purpose.
; It’s slower but is guaranteed to have the same answer regardless of hardware.

; Not all implementations of the equivalent functions of class Math
; are not defined to return the bit-for-bit same results (unlike StrictMath)
; This relaxation permits better-performing
; implementations where strict reproducibility is not required.
; By default many of the Math methods simply call the equivalent method in
; StrictMath for their implementation. Code generators are encouraged to use
; platform-specific native libraries or microprocessor instructions, where
; available, to provide higher-performance implementations of Math methods.

; 2.703028 µs
; (criterium.core/quick-bench (dotimes [n 100000] (Numeric/multiply 0.5 0.5)))
; 38.357842 µs
; (criterium.core/quick-bench (dotimes [n 100000] (core/* 0.5 0.5)))
; 16.643150 ms
; (criterium.core/quick-bench (dotimes [n 100000] (core/* 1/2 1/2)))

; TODO Configurable isNaN
; TODO ^:inline

(def overridden-fns
  '#{+ - * /
     dec inc
     > < <= >=
     zero?
     rem min max})

(def override-fns? (atom false))

; INTEGER
; Long       | Long       : LongOps
;            | Ratio      : Ratio
;            | Double     : Double ; DOUBLE_OPS
;            | BigInt     : BigInt
;            | BigDecimal : BigDecimal  

; INTEGER
; BigInt     | Long       : BigInt
;            | Double     : Double
;            | Ratio      : Ratio
;            | BigInt     : BigInt
;            | BigDecimal : BigDecimal

; DECIMAL
; BigDecimal | Long       : BigDecimal
;            | Double     : Double
;            | Ratio      : BigDecimal
;            | BigInt     : BigDecimal
;            | BigDecimal : BigDecimal

; FLOATING
; Double     | Long       : Double
;            | Double     : Double
;            | Ratio      : Double
;            | BigInt     : Double
;            | BigDecimal : Double

; RATIO
; Ratio      | Long       : Ratio
;            | Double     : Double
;            | Ratio      : Ratio
;            | BigInt     : Ratio
;            | BigDecimal : BigDecimal
(def num-ex (doto (ArithmeticException. "Numeric overflow")))
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
  ([#{double? Number} x] (-> x BigInteger/valueOf ->bigint)))

(defalias bigint ->bigint)

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

(defalias bigdec core/bigdec #_->bigdec)

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

(macros/variadic-proxy +* quantum.core.numeric/+-2) ; +* is unchecked


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

(macros/variadic-proxy -* quantum.core.numeric/--base)

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

#_(defnt' - ; TODO take out auto-quote generator
  (^Number [^long x] ; TODO boxes value... how to fix?
    (if (== x Long/MIN_VALUE)
        (-> x ->big-integer -* ->bigint)
        (-* x))))

(defnt' dec*
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

#_(defnt' dec
  (^Number [^long x] ; TODO boxes value... how to fix?
    (if (== x Long/MAX_VALUE)
        (-> x ->bigint dec*)
        (-* x 1))))

#_(defnt' inc' "Strict inc, throwing exception on overflow"
  (^:first ^:intrinsic [#{int long} x] (Math/incrementExact x))
  ([^long x]
    (if (== x Long/MAX_VALUE)
        (throw num-ex)
        (+* x 1))))

#_(defnt' inc "Natural inc, promoting on overflow"
  (^Number [^long x] ; TODO boxes value... how to fix?
    (if (== x Long/MAX_VALUE)
        (-> x ->bigint inc*)
        (+* x 1))))


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

(macros/variadic-proxy ** quantum.core.numeric/*-2)


(defnt' *' "Throws an exception on overflow"
  (^int  ^:intrinsic [^int  x ^int  y] (Math/multiplyExact x y))
  (^long ^:intrinsic [^long x ^long y] (Math/multiplyExact x y)))


(defnt ^boolean neg?
  ([#{byte char short int float double} x] (Numeric/isNeg x))
  ([#{java.math.BigInteger
      java.math.BigDecimal} x] (-> x .signum neg?))
  ([^clojure.lang.Ratio     x] (-> x .numerator .signum neg?))
  ([^clojure.lang.BigInt    x] (if (-> x .bipart         nil?)
                                   (-> x .lpart          neg?)
                                   (-> x .bipart .signum neg?))))

(defnt ^boolean pos?
  ([#{byte char short int float double} x] (Numeric/isPos x))
  ([#{java.math.BigInteger
      java.math.BigDecimal} x] (-> x .signum pos?))
  ([^clojure.lang.Ratio     x] (-> x .numerator .signum pos?))
  ([^clojure.lang.BigInt    x] (if (-> x .bipart         nil?)
                                   (-> x .lpart          pos?)
                                   (-> x .bipart .signum pos?))))

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

(macros/variadic-proxy div* quantum.core.numeric/div-2)

; (/ x 4) = (& x 3)

(defnt' not-num?
  ([^double? x] (Double/isNaN x))
  ([^float?  x] (Float/isNaN  x)))

;_____________________________________________________________________
;==================={        PREDICATES        }======================
;°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°
(macros/variadic-predicate-proxy >      quantum.core.Numeric/gt )
(macros/variadic-predicate-proxy <      quantum.core.Numeric/lt )
(macros/variadic-predicate-proxy <=     quantum.core.Numeric/lte)
(macros/variadic-predicate-proxy >=     quantum.core.Numeric/gte)


(defnt' eq-2
  (^boolean
    [#{byte char short int long float double} x
     #{byte char short int long float double} y]
    (quantum.core.Numeric/eq x y))
  (^boolean [^clojure.lang.BigInt x ^clojure.lang.BigInt y]
    (.equals x y)))

(macros/variadic-proxy == quantum.core.numeric/eq-2)

(macros/variadic-predicate-proxy not==  quantum.core.Numeric/neq)

(defnt' ^boolean zero?
  ([#{byte char short long float double} x] (Numeric/isZero x))
  ([^clojure.lang.Ratio     x] (-> x .numerator .signum zero?))
  ([^clojure.lang.BigInt    x] (if (nil? (.bipart x))
                                   (zero? (.lpart x))
                                   (-> x .bipart .signum zero?)))
  ([#{java.math.BigInteger
      java.math.BigDecimal} x] (-> x .signum zero?)))

(defmacro rem      [n div] `(Numeric/rem     ~n ~div))
(macros/variadic-proxy min quantum.core.Numeric/min)

#_(defnt' max2
  (^{:tag :largest}
    [#{byte char short int float double} x
     #{byte char short int float double} y]
    (quantum.core.Numeric/max x y)))
  
#_(macros/variadic-proxy max quantum.core.numeric/max2)

#_(defnt' min2
  (^{:tag :largest}
    [#{byte char short int float double} x
     #{byte char short int float double} y]
    (quantum.core.Numeric/min x y)))
  
#_(macros/variadic-proxy min quantum.core.numeric/min2)

#?(:clj
(when-not @override-fns?
  (doseq [sym overridden-fns]
    (let [qualified-sym (symbol "core" (name sym))]
      (eval (quote+ (defalias ~sym ~qualified-sym)))))))


(defn within-tolerance? [n total tolerance]
  (and (>= n (- total tolerance))
       (<= n (+ total tolerance))))

; https://github.com/clojure/math.numeric-tower/
(defn sign [n]  (if (neg? n) -1 1))
(def  nneg?     (fn-not core/neg?))



(def  pos-int?  (fn-and integer? core/pos?))
(def  nneg-int? (fn-and integer? nneg?))

(def neg (f*n core/-))

(def  abs       (whenf*n core/neg? neg))
(def  int-nil   (whenf*n nil? (constantly 0)))

; For units
(def ^:const ten              (long   10    ))
(def ^:const hundred          (long   100   ))
(def ^:const thousand         (long   1000  ))
(def ^:const ten-thousand     (long   10000 ))
(def ^:const hundred-thousand (long   100000))
(def ^:const million          (long   1E6   ))
(def ^:const billion          (long   1E9   ))
(def ^:const trillion         (long   1E12  ))
(def ^:const quadrillion      (long   1E15  ))
(def ^:const quintillion      (long   1E18  )) ; + exa | - atto
(def ^:const sextillion       (bigint 1E21  ))
(def ^:const septillion       (bigint 1E24  ))
(def ^:const octillion        (bigint 1E27  ))
(def ^:const nonillion        (bigint 1E30  ))
(def ^:const decillion        (bigint 1E33  ))

(defn evenly-divisible-by? [a b] (= 0 (rem a b)))


(defnt' abs'
  (^int               [^int    x] (Math/abs x))
  (^long              [^long   x] (Math/abs x))
  (^double            [^double x] (Math/abs x))
  (^float ^:intrinsic [^float  x] (Math/abs x)))





#?(:clj
(defnt' next-after
  (^double [^double start ^double direction] (Math/nextAfter start direction))
  (^float  [^float  start ^double direction] (Math/nextAfter start direction))))

#?(:clj
(defnt' next-down
  (^double [^double x] (Math/nextDown x))
  (^float  [^float  x] (Math/nextDown x))))

#?(:clj
(defnt' next-up
  (^double [^double x] (Math/nextUp x))
  (^float  [^float  x] (Math/nextUp x))))

#?(:clj
(defnt' cube-root
  (^double [^double x] (Math/cbrt x))))

#?(:clj
(defnt cube-root$
  "returns angle theta"
  {:performance ["6.2 times faster than java.lang.Math"
                 "Worst case 2E-14 difference"]}
  (^double [^double x] (FastMath/cbrt x))))

#?(:clj
(defnt' sqrt
  (^double ^:intrinsic [^double x] (Math/sqrt x))))

#?(:clj
(defnt' with-sign "Returns @x with the sign of @y."
  (^double [^double x ^double y] (Math/copySign x y))
  (^float  [^float  x ^float  y] (Math/copySign x y))))

; ==== TRIGONOMETRIC FUNCTIONS ====

; SINE

#?(:clj
(defnt' asin "arc sine"
  (^double [^double x] (Math/asin x))))

#?(:clj
(defnt asin*
  "arc sine"
  {:performance ["3.8 times faster than java.lang.Math"
                 "Worst case 2E-12 difference"]}
  (^double [^double x] (FastMath/asin x))))

#?(:clj
(defnt' sin "sine"
  (^double ^:intrinsic [^double x] (Math/sin x))))

#?(:clj
(defnt sin$
  "sine"
  {:performance ["4.5 times faster than java.lang.Math"
                 "Worst case 1E-11 difference"]}
  (^double [^double x] (FastMath/sin x))))

(defnt' sinh "hyperbolic sine"
  (^double [^double x] (Math/sinh x)))

(defnt sinh*
  "hyperbolic sine"
  {:performance ["5.5 times faster than java.lang.Math"
                 "Worst case 7E-14 difference"]}
  (^double [^double x] (FastMath/sinh x)))

; COSINE

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

; TANGENT

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

; DEGREES + RADIANS

(defnt' radians->degrees
  (^double [^double x] (Math/toDegrees x)))

(defnt' degrees->radians
  (^double [^double x] (Math/toRadians x)))

; ==== POWERS AND EXPONENTS ====

(defnt' e-exp "Euler's number (e) raised to the power of @x"
  (^double ^:intrinsic [^double x] (Math/exp x)))

(defnt' e-exp*
  "Euler's number (e) raised to the power of @x"
  {:performance ["4.6 times faster than java.lang.Math"
                 "Worst case 4E-14 difference"]}
  (^double [^double x] (FastMath/exp x)))

(defnt' log "Natural logarithm"
  (^double ^:intrinsic [^double x] (Math/log x)))

(defnt' log*
  "Natural logarithm"
  {:performance ["1.9 times faster than java.lang.Math"
                 "Worst case 3E-14 difference"]}
  (^double [^double x] (FastMath/log x)))

(defnt' log10 "Logarithm, base 10"
  (^double ^:intrinsic [^double x] (Math/log10 x)))

(defnt' log10*
  "Logarithm, base 10"
  {:performance ["2.1 times faster than java.lang.Math"
                 "Worst case 6E-14 difference"]}
  (^double [^double x] (FastMath/log10 x)))

(defnt' log1p*
  "Much more accurate than log(1+value) for arguments (and results) close to zero."
  {:performance ["6.5 times faster than java.lang.Math"
                 "Worst case 2E-14 difference"]}
  (^double [^double x] (FastMath/log1p x)))

#?(:clj
(defnt' exp'
  {:todo ["Performance" "Rename"]}
  [#{byte char short int float double} x #{long? double?} n]
  (loop [acc (Long. 1) nn n]
    (if (<= nn 0) acc
        (recur (core/*' x acc) (dec* nn))))))

#?(:clj
(defnt exp "|exp| and not |pow| because 'exponent'."
  (^double ^:intrinsic [^double x ^double y] (Math/pow x y))))


;pow'
; Only works with integers larger than zero.
; private double pow'(double d, int exp) {
;     double r = d;
;     for(int i = 1; i<exp; i++) {
;         r *= d;
;     }
;     return r;
; }

#?(:cljs
(defn exp [x n]
  (.pow js/Math x n)))

(defnt' exp$
  "|exp| and not |pow| because 'exponent'."
  {:performance ["2.7 times faster than java.lang.Math"
                 "Worst case 1E-11 difference"]}
  (^double [^double x ^double y] (FastMath/pow x y)))

(defnt' expm1$
  "Much more accurate than exp(value)-1 for arguments (and results) close to zero."
  {:performance ["6.6 times faster than java.lang.Math"
                 "Worst case 5E-14 difference"]}
  (^double [^double x] (FastMath/expm1 x)))

#_(:clj
(defnt' get-exp "Unbiased exponent used in @x"
  (^double [^double x] (Math/getExponent x))
  (^float  [^float  x] (Math/getExponent x))))

; ==== ROUNDING ====

#?(:clj
(defnt' rint "The double value that is closest in value to @x and is equal to a mathematical integer."
  (^double [^double x] (Math/rint x))))

#?(:clj
(defnt' round' "Rounds up in cases of ambiguity."
  (^long [^double x] (Math/round x))
  (^long [^float  x] (Math/round x))))

#?(:clj
(defnt' scalb
  "Returns (x * 2) ^ y, rounded as if performed by a single correctly rounded
   floating-point multiply to a member of the double value set."
  (^double [^double x ^int y] (Math/scalb x y))
  (^float  [^float  x ^int y] (Math/scalb x y))))

#?(:clj
(defnt' signum
  "Zero if the argument is zero,
   1.0 if the argument is greater than zero,
   -1.0 if the argument is less than zero."
  (^double [^double x] (Math/signum x))
  (^float  [^float  x] (Math/signum x))))

(defnt' ulp "Size of an ulp (?) of @x"
  (^double [^double x] (Math/ulp x))
  (^float  [^float  x] (Math/ulp x)))

; ==== CHOOSING ====

#?(:clj
(defnt' ceil
  (^double [^double x] (Math/ceil x))))

#?(:cljs
(defn ceil [x]
  (.ceil js/Math x)))

(defalias ceiling ceil)

#?(:clj
(defnt' floor
  (^double [^double x] (Math/floor x))))

#?(:cljs
(defn floor [x]
  (.floor js/Math x)))

#?(:clj
(defnt' floor-div
  (^int  [^int  x ^int  y] (Math/floorDiv x y))
  (^long [^long x ^long y] (Math/floorDiv x y))))

#?(:clj
(defnt' floor-mod
  (^int  [^int  x ^int  y] (Math/floorMod x y))
  (^long [^long x ^long y] (Math/floorMod x y))))



#?(:clj
(defnt' hypot "(sqrt (^ x 2) (^ y 2)) without intermediate over/underflow"
  (^double [^double x ^double y] (Math/hypot x y))))

(defnt' hypot$
  "(sqrt (^ x 2) (^ y 2)) without intermediate over/underflow"
  {:performance ["18.9 times faster than java.lang.Math"
                 "Worst case 3E-14 difference"]}
  (^double [^double x ^double y] (FastMath/hypot x y)))

#?(:clj
(defnt' ieee-remainder "The remainder operation on two arguments as prescribed by the IEEE 754 standard"
  (^double [^double x ^double y] (Math/IEEEremainder x y))))





; TODO macro to reduce repetitiveness here
(defn safe+
  ([a    ] (int-nil a))
  ([a b  ] (core/+ (int-nil a) (int-nil b)))
  ([a b c] (core/+ (int-nil a) (int-nil b) (int-nil c)))
  ([a b c & args] (->> (conj args c b a) (map int-nil) (reduce core/+))))

(defn safe*
  ([a    ] (int-nil a))
  ([a b  ] (core/* (int-nil a) (int-nil b)))
  ([a b c] (core/* (int-nil a) (int-nil b) (int-nil c)))
  ([a b c & args] (->> (conj args c b a) (map int-nil) (reduce core/*))))

(defn safe-
  ([a] (neg (int-nil a)))
  ([a b] (core/- (int-nil a) (int-nil b)))
  ([a b c] (core/- (int-nil a) (int-nil b) (int-nil c)))
  ([a b c & args] (->> (conj args c b a) (map int-nil) (reduce core/-))))

(defn safediv
  ([a b  ] (core// (int-nil a) (int-nil b)))
  ([a b c] (core// (int-nil a) (int-nil b) (int-nil c)))
  ([a b c & args] (->> (conj args c b a) (map int-nil) (reduce core//))))

#?(:clj
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
      (.setScale (bigdec num-0) ^Integer to round-type))))

(defn rcompare
  "Reverse comparator."
  {:attribution "taoensso.encore, possibly via weavejester.medley"}
  [x y] (compare y x))

(defn greatest
  "Returns the 'greatest' element in coll in O(n) time."
  {:attribution "taoensso.encore, possibly via weavejester.medley"}
  [coll & [?comparator]]
  (let [comparator (or ?comparator rcompare)]
    (reduce
      (fn ([] nil) ([a b] (if (pos? (comparator a b)) b a)))
      coll))) ; almost certainly can implement this with /fold+/

(defn least
  "Returns the 'least' element in coll in O(n) time."
  ^{:attribution "taoensso.encore, possibly via weavejester.medley"}
  [coll & [?comparator]]
  (let [comparator (or ?comparator rcompare)]
    (reduce
      (fn ([] nil) ([a b] (if (neg? (comparator a b)) b a)))
      coll)))

(defn greatest-or [a b else]
  (cond (> a b) a
        (> b a) b
        :else else))

(defn least-or [a b else]
  (cond (< a b) a
        (< b a) b
        :else else))

(defn approx? [tolerance a b]
  (-> (- (int-nil a) (int-nil b)) abs (< tolerance)))

(defn sin [n]
  #?(:clj  (java.lang.Math/sin n)
     :cljs (.sin js/Math       n)))

(defnt exactly
  #?(:clj
 ([^integer?           n] (bigint  n)))
 ([^decimal?           n] (rationalize #_->ratio n))
 ([^clojure.lang.Ratio n] n))

; (defn accounting-display)
;___________________________________________________________________________________________________________________________________
;=================================================={         MUTATION         }=====================================================
;=================================================={                          }=====================================================
#?(:clj (defmacro += [x a] `(set! ~x (+ ~x ~a))))
#?(:clj (defmacro -= [x a] `(set! ~x (- ~x ~a))))

#?(:clj (defmacro ++ [x] `(+= ~x 1)))
(defalias inc! ++)

#?(:clj (defmacro -- [x] `(-= ~x 1)))
(defalias dec! --)

;___________________________________________________________________________________________________________________________________
;=================================================={         DISPLAY          }=====================================================
;=================================================={                          }=====================================================
(def display-num (fn-> double (round :to 2)))
(defn format [n type]
  (condp = type
    :dollar
      (->> n display-num (str "$"))
    ;:accounting
    (throw+ (Err. nil "Unrecognized format" type))))

(defn percentage-of [of total-n]
  (-> of (core// total-n) (* 100) display-num (str "%")))