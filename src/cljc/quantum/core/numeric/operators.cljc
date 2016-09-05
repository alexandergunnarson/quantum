(ns quantum.core.numeric.operators
           (:refer-clojure :exclude
             [+ +' - -' * *' /
              inc inc' dec dec'
              numerator denominator])
           (:require
             [#?(:clj  clojure.core
                 :cljs cljs.core   )            :as core    ]
    #?(:cljs [com.gfredericks.goog.math.Integer :as int     ])
             [quantum.core.error :as err
               :refer [TODO]]
             [quantum.core.log :as log
               :include-macros true]
             [quantum.core.macros
               :refer        [#?@(:clj [defnt defntp defnt' variadic-proxy])]
               :refer-macros [defnt defntp]]
             [quantum.core.vars
               :refer        [#?@(:clj [defalias defmalias])]
               :refer-macros [defalias]]
             [quantum.core.numeric.types :as ntypes
               :refer [numerator denominator]]
             [quantum.core.numeric.convert
               :refer        [#?@(:clj [->bigint ->big-integer])]
               :refer-macros [->bigint]])
  #?(:cljs (:require-macros
             [quantum.core.numeric.operators
               :refer [+ * -]]))
  #?(:clj  (:import
             java.math.BigInteger
             java.math.BigDecimal
             clojure.lang.BigInt)))

(log/this-ns)

; Auto-unboxes; no boxed combinations necessary
; TODO right now: multiple typed arguments in |defnt|, even in protocols

; ===== ADD ===== ;

#?(:clj  (defalias +*-bin unchecked-add)
        #_(defnt +*-bin "Lax |+|; continues on overflow/underflow"
                   (^{:tag :first} [^number? x] x)
           #?(:clj (^{:tag :auto-promote}
                     [#{byte char short int long float double} #_(- primitive? boolean) x
                      #{byte char short int long float double} #_(- primitive? boolean) y]
                     (quantum.core.Numeric/add x y)))
           #?(:clj (^BigInt  [^BigInt  x ^clojure.lang.BigInt  y]
                     (.add x y)))
           #?(:clj (^BigDecimal [^BigDecimal x ^BigDecimal y]
                     (if (nil? *math-context*)
                         (.add x y)
                         (.add x y *math-context*))))
           #?(:cljs ([x y] (TODO) (ntypes/-add x y))))
   :cljs (defalias +*-bin unchecked-add))

#?(:clj (variadic-proxy +* quantum.core.numeric.operators/+*-bin))

#?(:clj (defalias +'-bin core/+')
        #_(defnt' +'-bin "Strict |+|; throws exception on overflow/underflow"
           ; TODO take out auto-quote generator
          (^int  ^:intrinsic [^int  x ^int  y] (Math/addExact x y))
          (^long ^:intrinsic [^long x ^long y] (Math/addExact x y))
          (^long [^long x] ; TODO boxes value... how to fix?
            (if (== x Long/MAX_VALUE)
                (throw num-ex)
                (+* x))))
   :cljs (defalias +'-bin core/+))

#?(:clj (variadic-proxy +' quantum.core.numeric.operators/+'-bin))

; "Natural |+|; promotes on overflow/underflow"
#?(:clj  (defalias +-bin core/+)
   :cljs (defalias +-bin core/+))

#?(:clj (variadic-proxy + quantum.core.numeric.operators/+-bin))


; ===== SUBTRACT ===== ;

(defalias -*-bin unchecked-subtract)

#_(defnt -*-bin "Lax |-|; continues on overflow/underflow"
  #?(:clj  (^{:tag :first} [#{byte char short int long float double} x]
             (quantum.core.Numeric/negate x))
     :cljs (^{:tag :first} [^number? x] (TODO "fix") (ntypes/-negate x)))
  #?(:clj (^{:tag :auto-promote} ; TODO should be :first?
            [#{byte char short int long float double} #_(- primitive? boolean) x
             #{byte char short int long float double} #_(- primitive? boolean) y]
            (quantum.core.Numeric/subtract x y)))
  #?(:clj (^java.math.BigInteger [^java.math.BigInteger x]
            (-> x .negate)))
  #?(:clj (^clojure.lang.BigInt  [^clojure.lang.BigInt  x]
            (-> x ->big-integer -*-bin ->bigint))))

#?(:clj (variadic-proxy -* quantum.core.numeric.operators/-*-bin))

#?(:clj (defalias -'-bin core/-)
        #_(defnt' -'-bin "Strict |-|; throws exception on overflow/underflow"
            ; TODO take out auto-quote generator
            (^int  ^:intrinsic [^int  x ^int  y] (Math/subtractExact x y))
            (^long ^:intrinsic [^long x ^long y] (Math/subtractExact x y))
            (^int  ^:intrinsic [^int  x] (Math/negateExact x))
            (^long ^:intrinsic [^long x] (Math/negateExact x))
            (^long [^long x] ; TODO boxes value... how to fix?
              (if (== x Long/MIN_VALUE)
                  (throw num-ex)
                  (-* x))))
   :cljs (defalias -'-bin core/-))

#?(:clj (variadic-proxy -' quantum.core.numeric.operators/-'-bin))

#?(:cljs (defn --bin- [x y] (core/- x y)))  ; TODO only to fix CLJS arithmetic warning here

(defnt --bin "Natural |-|; promotes on overflow/underflow"
  #?(:clj  (^{:tag :auto-promote} [^long x] ; TODO boxes value... how to fix?
             (if (== x Long/MIN_VALUE)
                 (-> x ->big-integer -* ->bigint)
                 (-* x))))
           ([x y] (#?(:clj core/- :cljs --bin-) x y)))

#?(:clj (variadic-proxy - quantum.core.numeric.operators/--bin))

; ===== MULTIPLY ===== ;

; (js/Math.imul x y) ; 32-bit int multiplication

#?(:clj (defnt' **-bin "Lax |*|; continues on overflow/underflow"
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
   :cljs (defn **-bin "Lax |*|; continues on overflow/underflow"
           ([] 0)
           ([x] x)
           ([x y] (TODO "fix") (ntypes/-multiply x y))))

#?(:clj (variadic-proxy ** quantum.core.numeric.operators/*-bin))

#?(:clj  (defnt' *'-bin "Strict |*|; throws exception on overflow/underflow"
           (^int  ^:intrinsic [^int  x ^int  y] (Math/multiplyExact x y))
           (^long ^:intrinsic [^long x ^long y] (Math/multiplyExact x y)))
   :cljs (defalias *'-bin core/*))

#?(:clj (variadic-proxy *' quantum.core.numeric.operators/*'-bin))

; "Natural |*|; promotes on overflow/underflow"
#?(:clj  (defalias *-bin core/*)
   :cljs (defalias *-bin core/*))

#?(:clj (variadic-proxy * quantum.core.numeric.operators/*-bin))

; ===== DIVIDE ===== ;

#?(:clj
  (defnt' div*-bin-
  "Lax |/|; continues on overflow/underflow.
   TODO Doesn't preserve ratios."
  (^double ; is it actually always double?
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
#?(:clj  (defntp div*-bin "Lax |/|. Continues on overflow/underflow."
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
   :cljs (defnt div*-bin "Lax |/|. Continues on overflow/underflow."
           ([^quantum.core.numeric.types.Ratio x  ] (TODO "fix") (ntypes/-invert x))
           ([^quantum.core.numeric.types.Ratio x y]
             (TODO "fix")
              ;(* x (-invert (apply * y more)))
              (* x (ntypes/-invert y)))
           ([^number? x  ] (core// x))
           ([^number? x y] (div*-bin- x y))))

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

#?(:clj (defnt' dec* "Unchecked |dec|. Doesn't throw on overflow/underflow."
          (^{:tag :first} [#{byte char short long float double} x] (quantum.core.Numeric/dec x))
          ([^clojure.lang.BigInt x]
            (-> x ->big-integer (.subtract BigInteger/ONE) BigInt/fromBigInteger))
          ([^java.math.BigDecimal x]
            (if (nil? *math-context*)
                (.subtract x BigDecimal/ONE)
                (.subtract x BigDecimal/ONE *math-context*))))
   :cljs (defnt dec* ([^number? x] (unchecked-dec x)))) ; TODO CLJS arithmetic warning here

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
           ([^number?                           x] (core/dec x))
           ([^com.gfredericks.goog.math.Integer x] (- x int/ONE))))

#?(:clj  (defnt' inc* "Unchecked |inc|. Doesn't throw on overflow/underflow."
           (^{:tag :first} [#{byte char short int long float double} x] (quantum.core.Numeric/inc x))
           ([^clojure.lang.BigInt x]
             (-> x ->big-integer (.subtract BigInteger/ONE) BigInt/fromBigInteger))
           ([^java.math.BigDecimal x]
             (if (nil? *math-context*)
                 (.add x BigDecimal/ONE)
                 (.add x BigDecimal/ONE *math-context*))))
   :cljs (defnt inc* ([^number? x] (unchecked-inc x))))

#?(:clj  (defalias inc core/inc)
         #_(defnt' inc "Natural |inc|; promotes on overflow/underflow"
             (^Number [^long x]
               (TODO "boxes value... how to fix?")
               (if (== x Long/MAX_VALUE)
                   (-> x ->bigint inc*)
                   (+* x 1))))
   :cljs (defnt inc
           ([^number?                           x] (core/inc x))
           ([^com.gfredericks.goog.math.Integer x] (+ x int/ONE))))

#?(:clj  (defalias inc' inc          )
         #_(defnt' inc' "Strict |inc|; throws exception on overflow/underflow"
             (^:first ^:intrinsic [#{int long} x] (Math/incrementExact x))
             ([^long x]
               (if (== x Long/MAX_VALUE)
                   (throw num-ex)
                   (+* x 1))))
   :cljs (defalias inc' inc          ))

#?(:clj (defnt abs'
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
                (clojure.lang.BigInt/fromLong       (abs' (.lpart  x)))
                (clojure.lang.BigInt/fromBigInteger (abs' (.bipart x)))))
          (^clojure.lang.Ratio [^clojure.lang.Ratio x] ; TODO this might be an awful implementation
            (/ (abs' (numerator   x))
               (abs' (denominator x)))))
   :cljs (defnt abs' ([x] (TODO "incomplete") (js/Math.abs x))))

#?(:clj (defalias abs abs'))