(ns quantum.core.numeric.operators
         (:refer-clojure :exclude
           [+ +' - -' * *' /
           inc inc' dec dec'
           numerator denominator])
         (:require
           [clojure.core                      :as core]
  #?(:cljs [com.gfredericks.goog.math.Integer :as int])
           ;; TODO TYPED re-enable
         #_[quantum.core.data.numeric         :as dnum
             :refer [bigdec? clj-bigint? numerator numeric? denominator]]
           [quantum.core.data.primitive       :as p]
           [quantum.core.data.refs            :as ref]
           ;; TODO TYPED re-enable
         #_[quantum.core.numeric.convert      :as conv
             :refer [>bigint #?@(:clj [>big-integer])]]
           [quantum.core.type                 :as t]
           [quantum.core.vars
             :refer [defalias]]
           ;; TODO TYPED excise reference
           [quantum.untyped.core.error
             :refer [TODO]]
           ;; TODO TYPED excise reference
           [quantum.untyped.core.form
             :refer [#?(:clj core-symbol)]]
           [quantum.untyped.core.log          :as log])
#?(:cljs (:require-macros
           [quantum.core.numeric.operators    :as self
             :refer [+ - *]]))
#?(:clj  (:import
           [clojure.lang BigInt Ratio]
           [quantum.core Numeric]
           [java.math BigInteger BigDecimal])))

(log/this-ns)

;; ===== (Up-to-)binary operators ===== ;;

;; ----- Addition ----- ;;

      ;; TODO we're missing CLJS bigdec/bigint (`dnum/-add`) as well as other type combos
      (t/defn ^:inline +*
        "Lax `+`. Continues on overflow/underflow."
        > numeric?
        ([] 0)
        ([x numeric?] x)
        ([x numeric-primitive?, y numeric-primitive? > ?]
          (#?(:clj Numeric/add :cljs cljs.core/+) x y))
#?(:clj ([x clj-bigint?, y clj-bigint? > clj-bigint?] (.add x y)))
#?(:clj ([x bigdec?    , y bigdec?     > bigdec?]
          (if (p/nil? *math-context*)
              (.add x y)
              (.add x y *math-context*)))))

      ;; TODO we're missing CLJS bigdec/bigint (`dnum/-add`) as well as other type combos
      (t/defn ^:inline +'
        "Strict `+`. Throws exception on overflow/underflow."
        > numeric?
        ([] (+*))
        ([x numeric?] (+* x))
        ;; A Java intrinsic, so we keep this arity
        ([x p/int? , y p/int?  > p/int?]  (Math/addExact x y))
        ;; A Java intrinsic, so we keep this arity
#?(:clj ([x p/long?, y p/long? > p/long?] (Math/addExact x y))))

      ;; TODO we're missing CLJS bigdec/bigint (`dnum/-add`) as well as other type combos
      (t/defn ^:inline +
        "Natural `+`. Promotes on overflow/underflow."
        > numeric?
        ;; TODO TYPED port from CLJ and CLJS core nss/classes
        )

;; ----- Subtraction ----- ;;

      ;; TODO we're missing CLJS bigdec/bigint (`dnum/-subtract`, `dnum/-negate`) as well as other
      ;; type combos
      (t/defn ^:inline -*
        "Lax `-`. Continues on overflow/underflow."
        > numeric?
        ([] 0)
        ([x numeric-primitive? > (t/type x)] (#?(:clj Numeric/negate :cljs cljs.core/-) x))
#?(:clj ([x clj-bigint?        > (t/type x)] ...))
#?(:clj ([x java-bigint?       > (t/type x)] (.negate x)))
        ([x numeric-primitive?, y numeric-primitive? > ?]
          (#?(:clj Numeric/subtract :cljs cljs.core/-) x y))))

      ;; TODO we're missing CLJS bigdec/bigint (`dnum/-subtract`, `dnum/-negate`) as well as other
      ;; type combos
      (t/defn ^:inline -'
        "Strict `-`. Throws exception on overflow/underflow."
        > numeric?
        ([] (-*))
        ;; A Java intrinsic, so we keep this arity
#?(:clj ([x p/int?  > p/int?]  (Math/negateExact x)))
#?(:clj ([x p/long? > p/long?] (Math/negateExact x)))
#?(:clj ([x p/int? , y p/int?  > p/int?]  (Math/subtractExact x y)))
#?(:clj ([x p/long?, y p/long? > p/long?] (Math/subtractExact x y))))

;; TODO TYPED continue to port
#?(:clj
         (defnt' -'-bin
           (^byte  [^byte   x] (if (Numeric/eq x Byte/MIN_VALUE   ) (num-ex) (-* x)))
           (^char  [^char   x] (if (Numeric/eq x 0                ) 0        (num-ex)))
           (^short [^short  x] (if (Numeric/eq x Short/MIN_VALUE  ) (num-ex) (-* x)))
           (^int   [^int    x] (if (Numeric/eq x Integer/MIN_VALUE) (num-ex) (-* x)))
           (^long  [^long   x] (if (Numeric/eq x Long/MIN_VALUE   ) (num-ex) (-* x))))
   :cljs (defalias -'-bin core/-))

      ;; TODO we're missing CLJS bigdec/bigint (`dnum/-subtract`, `dnum/-negate`) as well as other
      ;; type combos
      (t/defn ^:inline -
        "Natural `-`. Promotes on overflow/underflow."
        > numeric?
        ;; TODO TYPED port from CLJ and CLJS core nss/classes
        )

;; ----- Multiplication ----- ;;

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
           ([x y] (TODO "fix") (dnum/-multiply x y))))

#?(:cljs (defn *'-bin- [x y] (TODO))) ; TODO only to fix CLJS arithmetic warning here

#?(:clj  (defnt' *'-bin "Strict `*`. Throws exception on overflow/underflow."
           (^int  ^:intrinsic [^int  x ^int  y] (Math/multiplyExact x y))
           (^long ^:intrinsic [^long x ^long y] (Math/multiplyExact x y)))
   :cljs (defnt  *'-bin
           ([x y] (*'-bin- x y))))

; "Natural |*|; promotes on overflow/underflow"
#?(:clj  (defalias *-bin core/*)
   :cljs (defalias *-bin core/*))

;; ----- Division ----- ;;

#?(:cljs (defn div*-bin- [x y] (core// x y)))  ; TODO only to fix CLJS arithmetic warning here

; TODO optimization: (/ x 4) = (& x 3)
#?(:clj  (defnt' div*-bin
           "Lax `/`; continues on overflow/underflow."
           {:todo #{"Doesn't preserve ratios"}}
           ([#{byte char short int long float double} n
             #{byte char short int long float double} d]
             (Numeric/divide n d))
           ([^BigInteger n ^BigInteger d]
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
                       :else (Ratio. (if (neg? d-f) (.negate n-f) n-f)
                                     (if (neg? d-f) (.negate d-f) d-f)))))))
           ([^BigInt n ^BigInt d]
             (div*-bin (->big-integer n) (->big-integer d)))
           ([^BigDecimal n ^BigDecimal d]
             (if (nil? *math-context*)
                 (.divide n d)
                 (.divide n d *math-context*))))
   :cljs (defnt div*-bin "Lax `/`. Continues on overflow/underflow."
           ([^ratio? x  ] (TODO "fix") (dnum/-invert x))
           ([^ratio? x y]
             (TODO "fix")
              ;(* x (-invert (apply * y more)))
              (* x (dnum/-invert y)))
           ([^double? x  ] (core// x))
           ([^double? x y] (div*-bin- x y))))

; "Strict |/|. Throws exception on overflow/underflow."
#?(:clj  (defalias div'-bin core//) ; TODO port
   :cljs (defalias div'-bin core//))

#?(:clj (variadic-proxy div'  div'-bin ))
#?(:clj (variadic-proxy div'& div'-bin&))

; "Natural |/|. Promotes on overflow/underflow."
#?(:clj  (defalias div-bin core//) ; TODO port
   :cljs (defalias div-bin core//))

#?(:clj (variadic-proxy div  div-bin ))
#?(:clj (variadic-proxy div& div-bin&))
#?(:clj (defmalias / quantum.core.numeric.operators/div))

#?(:clj
(defnt' div:natural [^number? n ^number? denom]
  (if (zero? denom)
      (if (zero? n) 0 #?(:clj Double/POSITIVE_INFINITY :cljs js.Number/POSITIVE_INFINITY))
      (/ n denom))))

; TODO integer division via div:int
; TODO div:nil which returns nil if dividing by 0

;; ===== (Up-to-)unary operators ===== ;;

#?(:clj (defnt' dec* "Lax `dec`. Continues on overflow/underflow."
          ([#{byte char short long float double} x] (Numeric/dec x))
          ([^BigInt x]
            (-> x ->big-integer (.subtract BigInteger/ONE) BigInt/fromBigInteger))
          ([^BigDecimal x]
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
           ([#{byte char short int long float double} x] (Numeric/inc x))
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
          ([#{byte char short} x] (if (Numeric/isNeg x) (-' x) x)) ; TODO abstract this
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
          ([^ratio? x] ; TODO this might be an awful implementation
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
           (when (every? p/val? argsf#) (reduce ~core-op argsf#))))))))
