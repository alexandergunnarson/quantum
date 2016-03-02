(ns
  ^{:doc "Useful numeric functions. Floor, ceil, round, sin, abs, neg, etc."
    :attribution "Alex Gunnarson"
    :cljs-self-referring? true}
  quantum.core.numeric
  (:refer-clojure :exclude
    [* *' + +' - -' / < > <= >= == rem inc dec zero? min max format
     bigint biginteger bigdec])
  (:require-quantum [:core logic type fn macros err log pconvert])
  (:require [quantum.core.convert.primitive :as prim :refer [->unboxed]]
            [clojure.walk :refer [postwalk]]
            #_(:cljs [com.gfredericks.goog.math.Integer :as int]))
  #?(:cljs
  (:require-macros
            [quantum.core.numeric :refer [validate-exact]]))
  #?(:clj (:import [java.nio ByteBuffer]
                   [quantum.core Numeric] ; loops?
                   [net.jafama FastMath]
                   clojure.lang.BigInt
                   java.math.BigDecimal))) 
  
; TODO benchmark against https://github.com/gfredericks/cljs-numbers/blob/master/test-cljs/cljs_numbers/test.cljs

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

#?(:cljs (defalias zero? core/zero?))
#?(:cljs (defalias +     core/+    ))
#?(:cljs (defalias *     core/*    ))
#?(:cljs (defalias -     core/-    ))
#?(:cljs (defalias /     core//    ))
#?(:cljs (defalias rem   core/rem  ))
#?(:cljs (defalias inc   core/inc  ))
#?(:cljs (defalias dec   core/dec  ))
#?(:cljs (defalias <     core/<    ))
#?(:cljs (defalias >     core/>    ))


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



;; TODO: if the things becoming bigints are within double range
;; we could emit a long instead of a string -- I assume that is
;; more efficient.
#?(:clj
(let [bigint #(list 'cljs-numbers.core/bigint (str %))]
  (def type-conversions
    {Long bigint
     Double identity
     clojure.lang.BigInt bigint
     java.math.BigInteger bigint
     clojure.lang.Ratio #(list 'quantum.numeric.core/->ratio
                               (-> % numerator bigint)
                               (-> % denominator bigint))})))

#?(:clj
(defn type-convert-form
  [form]
  (postwalk (fn [x] (if-let [emitter (-> x type type-conversions)]
                      (emitter x)
                      x))
            form)))

#?(:clj
(defmacro num-literals
  "Allows BigInts and ratio literals, converting them to calls to the
  appropriate constructors. Converts any Long to a bigint, so if you
  need a double use e.g. `42.0` instead of `42`."
  ([form]
     (type-convert-form form))
  ([form1 form2 & more]
     (cons 'do (map type-convert-form (list* form1 form2 more))))))

(def num-ex (doto (ArithmeticException. "Numeric overflow")))

#?(:clj
(defnt exact?
  ([#{long Long clojure.lang.BigInt clojure.lang.Ratio} x])))

#?(:clj
(defmacro validate-exact
  [arg]
  (when *assert*
    `(let [arg# ~arg]
       (when-not (exact? arg#)
         (throw (->ex nil "Bad argument type" arg#)))))))

(declare gcd)

#?(:cljs (defprotocol Add                 (-add                   [x y])))
#?(:cljs (defprotocol AddWithInteger      (-add-with-integer      [x y])))
#?(:cljs (defprotocol AddWithRatio        (-add-with-ratio        [x y])))
#?(:cljs (defprotocol Multiply            (-multiply              [x y])))
#?(:cljs (defprotocol MultiplyWithInteger (-multiply-with-integer [x y])))
#?(:cljs (defprotocol MultiplyWithRatio   (-multiply-with-ratio   [x y])))
#?(:cljs (defprotocol Invert              (-invert                [x]  )))
#?(:cljs (defprotocol Negate              (-negate                [x]  )))
#?(:cljs (defprotocol Ordered             (-compare               [x y])))
#?(:cljs (defprotocol CompareToInteger    (-compare-to-integer    [x y])))
#?(:cljs (defprotocol CompareToRatio      (-compare-to-ratio      [x y])))

#?(:cljs
(extend-type number
  Add                 (-add                   [x y] (-add                   (bigint x) y))
  ;; I have a hard time reasoning about whether or not this is necessary
  AddWithInteger      (-add-with-integer      [x y] (-add-with-integer      (bigint x) y))
  AddWithRatio        (-add-with-ratio        [x y] (-add-with-ratio        (bigint x) y))
  Multiply            (-multiply              [x y] (-multiply              (bigint x) y))
  MultiplyWithInteger (-multiply-with-integer [x y] (-multiply-with-integer (bigint x) y))
  MultiplyWithRatio   (-multiply-with-ratio   [x y] (-multiply-with-ratio   (bigint x) y))
  Negate              (-negate                [x]   (-negate                (bigint x)  ))
  Ordered             (-compare               [x y] (-compare               (bigint x) y))
  CompareToInteger    (-compare-to-integer    [x y] (-compare-to-integer    (bigint x) y))
  CompareToRatio      (-compare-to-ratio      [x y] (-compare-to-ratio      (bigint x) y))))

#?(:cljs
(extend-type com.gfredericks.goog.math.Integer
  Add                 (-add                   [x y] (-add-with-integer y x))
  AddWithInteger      (-add-with-integer      [x y] (.add x y))
  AddWithRatio        (-add-with-ratio        [x y] (-add-with-ratio (-ratio x) y))
  Multiply            (-multiply              [x y] (-multiply-with-integer y x))
  MultiplyWithInteger (-multiply-with-integer [x y] (.multiply x y))
  MultiplyWithRatio   (-multiply-with-ratio   [x y] (-multiply-with-ratio (-ratio x) y))
  Negate              (-negate                [x]   (.negate x))
  Invert              (-invert                [x]   (-ratio int/ONE x))
  Ordered             (-compare               [x y] (core/- (-compare-to-integer y x)))
  CompareToInteger    (-compare-to-integer    [x y] (.compare x y))
  CompareToRatio      (-compare-to-ratio      [x y] (-compare-to-ratio (-ratio x) y))
  IEquiv              (-equiv                 [x y] (and (integer? y) (.equals x y)))
  ;; dunno?  
  IHash               (-hash                  [this] (reduce bit-xor 899242490 (.-bits_ this)))
  IComparable         (-compare               [x y]  (-compare x y))))

(declare normalize)

#?(:cljs
(deftype Ratio [n d]
  ;; "Ratios should not be constructed directly by user code; we assume n and d are
  ;;  canonical; i.e., they are coprime and at most n is negative."
  Object
    (toString [_]
      (str "#ratio [" n " " d "]"))
  Add            (-add              [x y] (-add-with-ratio y x))
  AddWithInteger (-add-with-integer [x y] (-add-with-ratio x (-ratio y)))
  AddWithRatio
    (-add-with-ratio [x y]
      (let [+ -add-with-integer
            * -multiply-with-integer
            n' (+ (* (.-n x) (.-d y))
                  (* (.-d x) (.-n y)))
            d' (* (.-d x) (.-d y))
            the-gcd (gcd n' d')]
        (normalize (.divide n' the-gcd) (.divide d' the-gcd))))
  Multiply            (-multiply              [x y] (-multiply-with-ratio y x        ))
  MultiplyWithInteger (-multiply-with-integer [x y] (-multiply            x (-ratio y)))
  MultiplyWithRatio
    (-multiply-with-ratio [x y]
      (let [* -multiply-with-integer
            n' (* (.-n x) (.-n y))
            d' (* (.-d x) (.-d y))
            the-gcd (gcd n' d')]
        (normalize (.divide n' the-gcd) (.divide d' the-gcd))))
  Negate (-negate [x] (Ratio. (-negate n) d))
  Invert           (-invert             [x]   (normalize d n))
  Ordered          (-compare            [x y] (core/- (-compare-to-ratio y x)))
  CompareToInteger (-compare-to-integer [x y] (-compare-to-ratio x (-ratio y)))
  CompareToRatio
    (-compare-to-ratio [x y]
      (let [* -multiply-with-integer]
        (-compare-to-integer (* (.-n x) (.-d y))
                             (* (.-n y) (.-d x)))))
  IEquiv
    (-equiv [_ other]
      (and (instance? Ratio other)
           (core/= n (.-n other))
           (core/= d (.-d other))))
  IHash
    (-hash [_]
      (bit-xor 124790411 (-hash n) (-hash d)))
  IComparable
    (-compare [x y]
      (-compare x y))))

#?(:cljs
(defn- normalize
  [n d]
  (if (.isNegative d)
    (let [n' (.negate n)
          d' (.negate d)]
      (if (.equals d' int/ONE)
          n'
          (Ratio. n' d')))
    (if (.equals d int/ONE)
        n
        (Ratio. n d)))))

#?(:cljs
(defn numerator
  [x]
  {:pre [(ratio? x)]}
  (.-n x)))

#?(:cljs
(defn denominator
  [x]
  {:pre [(ratio? x)]}
  (.-d x)))

;_____________________________________________________________________
;==================={       CONVERSIONS        }======================
;°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°
(defnt' ^java.math.BigInteger ->big-integer
  ([^java.math.BigInteger x] x)
  ([^clojure.lang.BigInt     x] (.toBigInteger x))
  ([;#{(- number? BigInteger BigInt)} x
    #{short int long Short Integer Long} x] ; TODO BigDecimal
    (-> x core/long (BigInteger/valueOf))))

#?(:cljs
(defn integer?
  [x]
  (instance? com.gfredericks.goog.math.Integer x)))

#_(:cljs 
(defn bigint?
  [x]
  (instance? goog.math.Integer x)))

#?(:clj
(defnt' ^clojure.lang.BigInt ->bigint
  ([^clojure.lang.BigInt  x] x)
  ([^java.math.BigInteger x] (BigInt/fromBigInteger x))
  ([^long   x] (-> x BigInt/fromLong))
  ([^string? x radix] (->bigint (BigInteger. x (int radix))))
  ([#{double? Number} x] (-> x BigInteger/valueOf ->bigint))))

#_(:cljs
(defn ->bigint
  [x]
  (if (bigint? x)
      x
      (int/fromString (str x)))))

#?(:clj  (def ^:const ZERO 0       )
   :cljs (def         ZERO int/ZERO))
#?(:clj  (def ^:const ONE  1       )
   :cljs (def         ONE  int/ONE ))

#?(:clj (defalias bigint ->bigint))

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

#?(:clj (defalias ->bigdec core/bigdec)) ; TODO temporary
#?(:clj (defalias bigdec ->bigdec))

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

#?(:cljs 
(defn ratio? [x]
  (instance? Ratio x)))

#?(:cljs
(defn ->ratio
  ([x] (->ratio x ONE))
  ([x y]
    (let [x (bigint x)
          y (bigint y)
          d (gcd x y)
          x' (.divide x d)
          y' (.divide y d)]
      (if (.isNegative y')
          (Ratio. (.negate x') (.negate y'))
          (Ratio. x' y'))))))
;_____________________________________________________________________
;==================={        OPERATORS         }======================
;°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°
; Auto-unboxes; no boxed combinations necessary

; ===== ADD =====

#?(:clj
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
        (.add x y *math-context*)))))

#?(:cljs
(defn +-2
  ([] 0)
  ([x] x)
  ([x y] (-add x y))))

; TODO waiting on +'
#_(:clj
(defn +-exact
  [x y]
  (validate-exact x)
  (validate-exact y)
  (+' x y)))

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

#?(:clj
(defnt' --base
  (^{:tag :first} [#{byte char short int long float double} x] (quantum.core.Numeric/negate x))
  (^{:tag :auto-promote}
    [#{byte char short int long float double} #_(- primitive? boolean) x
     #{byte char short int long float double} #_(- primitive? boolean) y]
    (quantum.core.Numeric/subtract x y))
  (^java.math.BigInteger [^java.math.BigInteger x] (-> x .negate))
  (^clojure.lang.BigInt  [^clojure.lang.BigInt  x]
    (-> x ->big-integer --base ->bigint))))

#?(:cljs
(defn --base
  ([x] (-negate x))
  ([x y & more]
     (+ x (-negate (apply + y more)))))) ; TODO fix

; TODO awaiting -'
#_(:clj 
(defn negate-exact
  [x]
  (validate-exact x)
  (-' x)))

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


#?(:clj
(defnt' dec*
  (^{:tag :first} [#{byte char short long float double} x] (quantum.core.Numeric/dec x))
  ([^clojure.lang.BigInt x]
    (-> x ->big-integer (.subtract BigInteger/ONE) BigInt/fromBigInteger))
  ([^java.math.BigDecimal x]
    (if (nil? *math-context*)
        (.subtract x BigDecimal/ONE)
        (.subtract x BigDecimal/ONE *math-context*)))))

#?(:cljs (defalias dec* unchecked-dec))

#?(:clj
(defnt' inc* "Unchecked |inc|"
  (^{:tag :first} [#{byte char short int long float double} x] (quantum.core.Numeric/inc x))
  ([^clojure.lang.BigInt x]
    (-> x ->big-integer (.subtract BigInteger/ONE) BigInt/fromBigInteger))
  ([^java.math.BigDecimal x]
    (if (nil? *math-context*)
        (.add x BigDecimal/ONE)
        (.add x BigDecimal/ONE *math-context*)))))

#?(:cljs (defalias inc* unchecked-inc))

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

#?(:cljs (defn inc [x] (+ x ONE)))
#?(:cljs (defn dec [x] (- x ONE)))

#?(:clj
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
        (.multiply x y *math-context*)))))

#?(:cljs
(defn *-2
  ([] 0)
  ([x] x)
  ([x y] (-multiply x y))))

(macros/variadic-proxy ** quantum.core.numeric/*-2)

; (js/Math.imul x y) ; 32-bit int multiplication
#?(:clj
(defnt' *' "Throws an exception on overflow"
  (^int  ^:intrinsic [^int  x ^int  y] (Math/multiplyExact x y))
  (^long ^:intrinsic [^long x ^long y] (Math/multiplyExact x y))))

#?(:clj
(defn *-exact
  [x y]
  (validate-exact x)
  (validate-exact y)
  (*' x y)))

#?(:clj 
(defnt ^boolean neg?
  ([#{byte char short int float double} x] (quantum.core.Numeric/isNeg x))
  ([#{java.math.BigInteger
      java.math.BigDecimal} x] (-> x .signum neg?))
  ([^clojure.lang.Ratio     x] (-> x .numerator .signum neg?))
  ([^clojure.lang.BigInt    x] (if (-> x .bipart         nil?)
                                   (-> x .lpart          neg?)
                                   (-> x .bipart .signum neg?)))))

#?(:cljs (defn neg? [x] (< x ZERO))) ; TODO fix

#?(:clj
(defnt ^boolean pos?
  ([#{byte char short int float double} x] (quantum.core.Numeric/isPos x))
  ([#{java.math.BigInteger
      java.math.BigDecimal} x] (-> x .signum pos?))
  ([^clojure.lang.Ratio     x] (-> x .numerator .signum pos?))
  ([^clojure.lang.BigInt    x] (if (-> x .bipart         nil?)
                                   (-> x .lpart          pos?)
                                   (-> x .bipart .signum pos?)))))

#?(:cljs (defn pos? [x] (< ZERO x))) ; TODO fix

; TODO do |/|
#_(:clj
(defn invert-exact
  [x]
  (validate-exact x)
  (/ x)))

#?(:cljs
(defn invert
  [x]
  (-invert x)))

#?(:clj
(defn compare-exact
  [x y]
  (validate-exact x)
  (validate-exact y)
  (core/compare x y)))

#?(:cljs
(defn compare
  [x y]
  (-compare x y)))

#?(:clj
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
        (.divide x y *math-context*)))))

#?(:cljs
(defn /
  ([x] (-invert x))
  ([x y & more]
     (* x (-invert (apply * y more)))))) ; TODO fix

(macros/variadic-proxy div* quantum.core.numeric/div-2)

; (/ x 4) = (& x 3)

(defnt' not-num?
  ([^double? x] (Double/isNaN x))
  ([^float?  x] (Float/isNaN  x)))

;_____________________________________________________________________
;==================={        PREDICATES        }======================
;°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°
#?(:clj (macros/variadic-predicate-proxy >      quantum.core.Numeric/gt))

#?(:cljs
(defn >*
  ([x] true)
  ([x y] (core/pos? (-compare x y)))))
#?(:cljs (macros/variadic-predicate-proxy >      quantum.core.numeric/>*))

#?(:clj (macros/variadic-predicate-proxy <      quantum.core.Numeric/lt))

#?(:cljs
(defn <*
  ([x] true)
  ([x y] (core/neg? (-compare x y)))))
#?(:cljs (macros/variadic-predicate-proxy <      quantum.core.numeric/<*))

#?(:clj (macros/variadic-predicate-proxy <=     quantum.core.Numeric/lte))

#?(:cljs
(defn <=*
  ([x] true)
  ([x y] (not (core/pos? (-compare x y))))))
#?(:cljs (macros/variadic-predicate-proxy <=     quantum.core.numeric/<=*))

#?(:clj (macros/variadic-predicate-proxy >=     quantum.core.Numeric/gte))

#?(:cljs
(defn >=*
  ([x] true)
  ([x y] (not (core/neg? (-compare x y))))))
#?(:cljs (macros/variadic-predicate-proxy >=     quantum.core.numeric/>=*))

#?(:clj
(defnt' eq-2
  (^boolean
    [#{byte char short int long float double} x
     #{byte char short int long float double} y]
    (quantum.core.Numeric/eq x y))
  (^boolean [^clojure.lang.BigInt x ^clojure.lang.BigInt y]
    (.equals x y))))

#?(:cljs
(defn eq-2
  ([x] true)
  ([x y] (zero? (-compare x y)))))

#?(:clj
(defn ==
  [x y]
  (validate-exact x)
  (validate-exact y)
  (core/= x y)))

(macros/variadic-proxy == quantum.core.numeric/eq-2)

(macros/variadic-predicate-proxy not==  quantum.core.Numeric/neq)



#?(:clj
(defnt' ^boolean zero?
  ([#{byte char short long float double} x] (quantum.core.Numeric/isZero x))
  ([^clojure.lang.Ratio     x] (-> x .numerator .signum zero?))
  ([^clojure.lang.BigInt    x] (if (nil? (.bipart x))
                                   (zero? (.lpart x))
                                   (-> x .bipart .signum zero?)))
  ([#{java.math.BigInteger
      java.math.BigDecimal} x] (-> x .signum zero?))))

#?(:cljs (defn zero? [x] (= x ZERO))) ; TODO fix

#?(:cljs
(defn quot
  [x n]
  {:pre [(integer? x) (integer? n)]}
  (.divide x n)))

#?(:clj (defmacro rem  [n div] `(Numeric/rem ~n ~div)))

#?(:cljs
(defn rem
  [x n]
  {:pre [(integer? x) (integer? n)]}
  (.modulo x n)))

#?(:cljs
(defn mod
  [x n]
  (let [y (rem x n)]
    (cond-> y (.isNegative y) (.add n)))))

(macros/variadic-proxy min quantum.core.Numeric/min)

#_(defnt' max2
  (^{:tag :largest}
    [#{byte char short int float double} x
     #{byte char short int float double} y]
    (quantum.core.Numeric/max x y)))

; (js/Math.max x y)
  
#_(macros/variadic-proxy max quantum.core.numeric/max2)

#_(defnt' min2
  (^{:tag :largest}
    [#{byte char short int float double} x
     #{byte char short int float double} y]
    (quantum.core.Numeric/min x y)))

; (js/Math.min x y)
  
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
(def  nneg?     (fn-not core/neg?))
(def  pos-int?  (fn-and integer? core/pos?))
(def  nneg-int? (fn-and integer? nneg?))

(def neg (f*n core/-))

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


#?(:clj  (def  abs       (whenf*n core/neg? neg)))
#?(:cljs (defn abs [x] (js/Math.abs x)))

#?(:clj
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
    (/ (abs (numerator   x))
       (abs (denominator x))))))

(defn approx=
  "Return true if the absolute value of the difference between x and y
   is less than eps."
  [x y eps]
  (< (abs (- x y)) eps))

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
      (^double [^double x] (Math/cbrt x)))
   :cljs
    (defn cube-root [x] (js/Math.cbrt x)))

#?(:clj
(defnt cube-root$
  "returns angle theta"
  {:performance ["6.2 times faster than java.lang.Math"
                 "Worst case 2E-14 difference"]}
  (^double [^double x] (FastMath/cbrt x))))

#?(:clj
    (defnt' sqrt
      (^double ^:intrinsic [^double x] (Math/sqrt x)))
   :cljs
    (defn sqrt [x] (js/Math.sqrt x)))

#?(:clj
(defnt' with-sign "Returns @x with the sign of @y."
  (^double [^double x ^double y] (Math/copySign x y))
  (^float  [^float  x ^float  y] (Math/copySign x y))))

; ==== TRIGONOMETRIC FUNCTIONS ====

; SINE

#?(:clj
    (defnt' asin "arc sine"
      (^double [^double x] (Math/asin x)))
   :cljs
    (defn asin [x] (js/Math.asin x)))

#?(:clj
(defnt asin*
  "arc sine"
  {:performance ["3.8 times faster than java.lang.Math"
                 "Worst case 2E-12 difference"]}
  (^double [^double x] (FastMath/asin x))))

#?(:cljs (defn asinh [x] (js/Math.asinh x)))

#?(:clj
    (defnt' sin "sine"
      (^double ^:intrinsic [^double x] (Math/sin x)))
   :cljs
    (defn sin [x] (js/Math.sin x)))

#?(:clj
(defnt sin$
  "sine"
  {:performance ["4.5 times faster than java.lang.Math"
                 "Worst case 1E-11 difference"]}
  (^double [^double x] (FastMath/sin x))))

#?(:clj
    (defnt' sinh "hyperbolic sine"
      (^double [^double x] (Math/sinh x)))
   :cljs
    (defn sinh [x] (js/Math.sinh x)))

#?(:clj 
(defnt sinh*
  "hyperbolic sine"
  {:performance ["5.5 times faster than java.lang.Math"
                 "Worst case 7E-14 difference"]}
  (^double [^double x] (FastMath/sinh x))))

; COSINE

#?(:clj
    (defnt acos "arc cosine"
      (^double [^double x] (Math/acos x)))
   :cljs
    (defn acos [x] (js/Math.acos x)))

(defnt acos*
  "arc cosine"
  {:performance ["3.6 times faster than java.lang.Math"
                 "Worst case 1E-12 difference"]}
  (^double [^double x] (FastMath/acos x)))

#?(:cljs (defn acosh [x] (js/Math.acosh x)))

#?(:clj
    (defnt' cos "cosine"
      (^double ^:intrinsic [^double x] (Math/cos x)))
   :cljs
    (defn cos [x] (js/Math.cos x)))

#?(:clj
(defnt' cos*
  "cosine"
  {:performance ["5.7 times faster than java.lang.Math"
                 "Worst case 8E-12 difference"]}
  (^double [^double x] (FastMath/cos x))))

#?(:clj
    (defnt' cosh "hyperbolic cosine"
      (^double [^double x] (Math/cosh x)))
   :cljs
    (defn cosh [x] (js/Math.cosh x)))

#?(:clj
(defnt' cosh*
  "hyperbolic cosine"
  {:performance ["5 times faster than java.lang.Math"
                 "Worst case 4E-14 difference"]}
  (^double [^double x] (FastMath/cosh x))))

; ----- TANGENT ----- ;

#?(:clj
    (defnt' atan "arc tangent"
      (^double [^double x] (Math/atan x)))
   :cljs
    (defn asin [x] (js/Math.atan x)))

#?(:clj
(defnt atan*
  "arc tangent"
  {:performance ["6.2 times faster than java.lang.Math"
                 "Worst case 5E-13 difference"]}
  (^double [^double x] (FastMath/atan x))))

#?(:cljs (defn atanh [x] (js/Math.atanh x)))

#?(:clj
    (defnt' atan2 "returns angle theta"
      (^double ^:intrinsic [^double x ^double y] (Math/atan2 x y)))
   :cljs
    (defn atan2 [x y] (js/Math.atan2 x y)))

#?(:clj
(defnt atan2*
  "returns angle theta"
  {:performance ["6.3 times faster than java.lang.Math"
                 "Worst case 4E-13 difference"]}
  (^double [^double x ^double y] (FastMath/atan2 x y))))

#?(:clj
    (defnt' tan "tangent"
      (^double ^:intrinsic [^double x] (Math/tan x)))
   :cljs
    (defn tan [x] (js/Math.tan x)))

#?(:clj
(defnt tan*
  "tangent"
  {:performance ["3.7 times faster than java.lang.Math"
                 "Worst case 1E-13 difference"]}
  (^double [^double x] (FastMath/tan x))))

#?(:clj
    (defnt' tanh "hyperbolic tangent"
      (^double [^double x] (Math/tanh x)))
   :cljs
    (defn tanh [x] (js/Math.tan x)))

#?(:clj 
(defnt tanh*
  "hyperbolic tangent"
  {:performance ["6.4 times faster than java.lang.Math"
                 "Worst case 5E-14 difference"]}
  (^double [^double x] (FastMath/tanh x))))

; DEGREES + RADIANS

#?(:clj
(defnt' radians->degrees
  (^double [^double x] (Math/toDegrees x))))

#?(:clj
(defnt' degrees->radians
  (^double [^double x] (Math/toRadians x))))

; ==== POWERS AND EXPONENTS ====

#?(:clj
    (defnt' e-exp "Euler's number (e) raised to the power of @x"
      (^double ^:intrinsic [^double x] (Math/exp x)))
   :cljs
    (defn e-exp [x] (js/Math.exp e)))

#?(:clj
(defnt' e-exp*
  "Euler's number (e) raised to the power of @x"
  {:performance ["4.6 times faster than java.lang.Math"
                 "Worst case 4E-14 difference"]}
  (^double [^double x] (FastMath/exp x))))

#?(:clj
    (defnt' log "Natural logarithm"
      (^double ^:intrinsic [^double x] (Math/log x)))
   :cljs
    (defn log [x] (js/Math.log x)))

#?(:clj
(defnt' log*
  "Natural logarithm"
  {:performance ["1.9 times faster than java.lang.Math"
                 "Worst case 3E-14 difference"]}
  (^double [^double x] (FastMath/log x))))

#?(:clj
    (defnt' log10 "Logarithm, base 10"
      (^double ^:intrinsic [^double x] (Math/log10 x)))
   :cljs
    (defn log10 [x] (js/Math.log10 x)))

#?(:cljs (defn log2 [x] (js/Math.log2 x)))

#?(:clj
(defnt' log10*
  "Logarithm, base 10"
  {:performance ["2.1 times faster than java.lang.Math"
                 "Worst case 6E-14 difference"]}
  (^double [^double x] (FastMath/log10 x))))

#?(:clj
(defnt' log1p*
  "Much more accurate than log(1+value) for arguments (and results) close to zero."
  {:performance ["6.5 times faster than java.lang.Math"
                 "Worst case 2E-14 difference"]}
  (^double [^double x] (FastMath/log1p x))))

#?(:clj
(defnt' exp'
  {:todo ["Performance" "Rename"]}
  [#{byte char short int float double} x #{long? double?} n]
  (loop [acc (Long. 1) nn n]
    (if (<= nn 0) acc
        (recur (core/*' x acc) (dec* nn))))))

#?(:clj
    (defnt exp "|exp| and not |pow| because 'exponent'."
      (^double ^:intrinsic [^double x ^double y] (Math/pow x y)))
   :cljs
    (defn exp [x y] (js/Math.exp x y)))


;pow'
; Only works with integers larger than zero.
; private double pow'(double d, int exp) {
;     double r = d;
;     for(int i = 1; i<exp; i++) {
;         r *= d;
;     }
;     return r;
; }

(defnt' exp$
  "|exp| and not |pow| because 'exponent'."
  {:performance ["2.7 times faster than java.lang.Math"
                 "Worst case 1E-11 difference"]}
  (^double [^double x ^double y] (FastMath/pow x y)))

#?(:clj
(defnt' expm1$
  "Much more accurate than exp(value)-1 for arguments (and results) close to zero."
  {:performance ["6.6 times faster than java.lang.Math"
                 "Worst case 5E-14 difference"]}
  (^double [^double x] (FastMath/expm1 x))))

#_(:clj
(defnt' get-exp "Unbiased exponent used in @x"
  (^double [^double x] (Math/getExponent x))
  (^float  [^float  x] (Math/getExponent x))))

; ==== ROUNDING ====

#?(:clj
(defnt' rint "The double value that is closest in value to @x and is equal to a mathematical integer."
  (^double [^double x] (Math/rint x))))

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

#?(:cljs (defn round [x] (js/Math.round x)))

#?(:clj
(defnt' round' "Rounds up in cases of ambiguity."
  (^long                 [^double               x] (Math/round x))
  (^long                 [^float                x] (Math/round x))
  (^java.math.BigDecimal [^java.math.BigDecimal x math-context]
    (.round x math-context))
  (^java.math.BigDecimal [^clojure.lang.Ratio x]
    (round (->double x)))
  (^java.math.BigDecimal [^clojure.lang.Ratio x math-context]
    (round (->bigdec x) math-context))))

#?(:clj
(defnt' scalb
  "Returns (x * 2) ^ y, rounded as if performed by a single correctly rounded
   floating-point multiply to a member of the double value set."
  (^double [^double x ^int y] (Math/scalb x y))
  (^float  [^float  x ^int y] (Math/scalb x y))))

(defn sign [n] (if (neg? n) -1 1))

#?(:clj
    (defnt' sign'
      "Zero if the argument is zero,
       1.0 if the argument is greater than zero,
       -1.0 if the argument is less than zero."
      (^double [^double x] (Math/signum x))
      (^float  [^float  x] (Math/signum x)))
   :cljs
    (defn sign' [x] (js/Math.sign x)))

#?(:clj
(defnt' ulp "Size of an ulp (?) of @x"
  (^double [^double x] (Math/ulp x))
  (^float  [^float  x] (Math/ulp x))))

; ==== CHOOSING ====

#?(:clj
    (defnt' ceil
      (^double [^double x] (Math/ceil x)))
   :cljs
    (defn ceil [x] (js/Math.ceil x)))

(defalias ceiling ceil)

#?(:clj
    (defnt' floor
      (^double [^double x] (Math/floor x)))
   :cljs
    (defn floor [x] (js/Math.floor x)))

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
      (^double [^double x ^double y] (Math/hypot x y)))
   :cljs
    (defn hypot [x y] (js/Math.hypot x y)))

#?(:clj
(defnt' hypot$
  "(sqrt (^ x 2) (^ y 2)) without intermediate over/underflow"
  {:performance ["18.9 times faster than java.lang.Math"
                 "Worst case 3E-14 difference"]}
  (^double [^double x ^double y] (FastMath/hypot x y))))

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

; TODO "can't type hint primitive local"
#_(:clj
(defnt exactly
 ([^integer?           n] (bigint  n)
 ([^decimal?           n] (rationalize #_->ratio n))
 ([^clojure.lang.Ratio n] n))))

(defn whole-number? [n]
  (= n (floor n)))

; (defn whole? [n]
;   (assert (instance? Double n))
;   (= (mod n 1) 0.0))

(defn- divisible?
  [num div]
  (zero? (mod num div)))

(defn- indivisible?
  [num div]
  (not (divisible? num div)))

#?(:clj
(def- two-to-fifty-three
  (apply * (repeat 53 2))))

#?(:clj
(def- minus-two-to-fifty-three
  (- two-to-fifty-three)))

#?(:clj
(defn native-integer?
  [n]
  (and (number? n)
       (<= minus-two-to-fifty-three
           n
           two-to-fifty-three)
       (core/integer? n))))

; PROPERTIES OF NUMERIC FUNCTIONS

(def inverse-map ; some better way of doing this?
  {+ -
   - +
   / *
   * /})

(defn inverse
  "Gets the inverse of the function @f."
  {:tests '{(inverse +) -
            (inverse *) /}
   :todo "Make this better. E.g. intelligent inverse of more
          complex functions"}
  [f]
  (or (get inverse-map f)
      (throw (->ex :undefined "|inverse| not defined for function" f))))

(def ^{:doc "Base values for operators."}
  base-map
  {+ 0
   - 0
   / 1
   * 1})

(defn base
  "Gets the identity-base for the given function `f`.
   
   For instance:
   The identity-base of the `+` function is 0: (= x (+ x 0)).
   By contrast, that of the `*` function is 1: (= x (* x 0))"
  {:tests '{(base +) 0
            (base *) 1}}
  [f]
  (or (get base-map f)
      (throw (->ex :undefined "|base| not defined for function" f))))

; ===== MORE COMPLEX ===== ;

(defn factors [n]
  (->> (range 1 (inc (sqrt n)))
       (filter #(zero? (rem n %)))
       (mapcat (fn [x] [x (/ n x)]))
       (into (sorted-set))))

; TODO MERGE
;#?(:cljs
;(defn gcd [x y]
;  (if (.isZero y)
;      x
;      (recur y (.modulo x y)))))

(defn gcd
  "(gcd a b) computes the greatest common divisor of a and b."
  ([a b]
  (if (zero? b)
      a
      (recur b (mod a b))))
  ([a b & args]
    (reduce gcd (gcd a b) args)))

; ===== NON-TRANSFORMATION OPS ===== ; 

#?(:cljs (defn leading-zeros [x] (js/Math.clz32 x)))

;___________________________________________________________________________________________________________________________________
;=================================================={         MUTATION         }=====================================================
;=================================================={                          }=====================================================
#?(:clj (defmacro += [x a] `(set! ~x (+ ~x ~a))))
#?(:clj (defmacro -= [x a] `(set! ~x (- ~x ~a))))

#?(:clj (defmacro ++ [x] `(+= ~x 1)))
(defmalias inc! quantum.core.numeric/++)

#?(:clj (defmacro -- [x] `(-= ~x 1)))
(defmalias dec! quantum.core.numeric/--)

;___________________________________________________________________________________________________________________________________
;=================================================={         DISPLAY          }=====================================================
;=================================================={                          }=====================================================
#?(:clj (def display-num (fn-> double (round :to 2))))
#?(:clj
(defn format [n type]
  (condp = type
    :dollar
      (->> n display-num (str "$"))
    ;:accounting
    (throw+ (Err. nil "Unrecognized format" type)))))

#?(:clj
(defn percentage-of [of total-n]
  (-> of (core// total-n) (* 100) display-num (str "%"))))