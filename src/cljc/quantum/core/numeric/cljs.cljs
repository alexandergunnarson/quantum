(ns
  ^{:doc "Useful numeric functions. Floor, ceil, round, sin, abs, neg, etc."
    :attribution "Alex Gunnarson"
    :cljs-self-referring? true}
  quantum.core.numeric.cljs
  (:refer-clojure :exclude
    [* *' + +' - -' / < > <= >= == rem inc dec zero? min max format
     mod quot neg? pos?])
  (:require-quantum [:core logic type fn macros err log pconvert])
  (:require [quantum.core.convert.primitive :as prim :refer [->unboxed]]
            [clojure.walk :refer [postwalk]]
            [com.gfredericks.goog.math.Integer :as int]
            [quantum.core.numeric.types :as ntypes])) 

(defalias rem   core/rem  )


; ===== NON-TRANSFORMATIVE OPERATIONS ===== ;

(defnt numerator
  ([^quantum.core.numeric.types.Ratio x] (.-n x)))

(defnt denominator
  ([^quantum.core.numeric.types.Ratio x] (.-d x)))

;_____________________________________________________________________
;==================={       CONVERSIONS        }======================
;°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°

(defalias ->ratio  ntypes/->ratio )
(defalias ->bigint ntypes/->bigint)

;_____________________________________________________________________
;==================={        OPERATORS         }======================
;°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°

; ===== ADD ===== ;

(defn +-2
  ([] 0)
  ([x] x)
  ([x y] (ntypes/-add x y)))

(defalias +     core/+    )

; ===== SUBTRACT ===== ;

(defn --base
  ([x] (ntypes/-negate x))
  ([x y & more]
     (+ x (ntypes/-negate (apply + y more))))) ; TODO fix

(defalias -     core/-    )
(defalias -'    core/-    )

; ===== MULTIPLY ===== ;

(defn *-2
  ([] 0)
  ([x] x)
  ([x y] (ntypes/-multiply x y))) ; TODO fix

; (js/Math.imul x y) ; 32-bit int multiplication

(defalias *     core/*    )
(defalias *'    *         )

; ===== DIVIDE ===== ;

; TODO fix
; optimization: (/ x 4) = (& x 3)
(defnt div-2
  ([^quantum.core.numeric.types.Ratio x  ] (ntypes/-invert x))
  ([^quantum.core.numeric.types.Ratio x y]
     ;(* x (-invert (apply * y more)))
     (* x (ntypes/-invert y)))
  ([^number? x  ] (core// x))
  ([^number? x y] (core// x y)))

(defalias /     core//    )

;_____________________________________________________________________
;==================={   UNARY MATH OPERATORS   }======================
;°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°

(defnt inc
  ([^number?                           x] (core/inc x))
  ([^com.gfredericks.goog.math.Integer x] (+ x ONE)))

(defalias inc' inc          )
(defalias inc* unchecked-inc)

(defnt dec
  ([^number?                           x] (core/dec x))
  ([^com.gfredericks.goog.math.Integer x] (- x ONE)))

(defalias dec' dec          )
(defalias dec* unchecked-dec)

(defn abs   [x] (js/Math.abs  x))
(defn sign' [x] (js/Math.sign x))

;_____________________________________________________________________
;==================={        PREDICATES        }======================
;°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°

(defnt pos?
  ([^number?                           x] (core/pos? x))
  ([^com.gfredericks.goog.math.Integer x] (> x ZERO)))

(defnt neg?
  ([^number?                           x] (core/neg? x))
  ([^com.gfredericks.goog.math.Integer x] (< x ZERO)))

(defnt zero?
  ([^number?                           x] (core/zero? x))
  ([^com.gfredericks.goog.math.Integer x] (= x ZERO)))

;_____________________________________________________________________
;==================={        COMPARISON        }======================
;°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°

; TODO fix
(defn =-2
  ([x] true)
  ([x y] (zero? (ntypes/-compare x y))))

; TODO fix
(defn not=-2
  ([x] false)
  ([x y] (not (zero? (ntypes/-compare x y)))))

(defalias <     core/<    )
(defalias <=    core/<=   )
(defalias >     core/>    )
(defalias >=    core/>=   )

(defalias <-2  < )
(defalias <=-2 <=)
(defalias >-2  > )
(defalias >=-2 >=)

;#?(:cljs
;(defn >*
;  ([x] true)
;  ([x y] (core/pos? (-compare x y)))))

;#?(:cljs
;(defn <*
;  ([x] true)
;  ([x y] (core/neg? (-compare x y)))))

;#?(:cljs
;(defn <=*
;  ([x] true)
;  ([x y] (not (core/pos? (-compare x y))))))
;#?(:cljs (macros/variadic-predicate-proxy <=     quantum.core.numeric/<=*))

;#?(:cljs
;(defn >=*
;  ([x] true)
;  ([x y] (not (core/neg? (-compare x y))))))
;#?(:cljs (macros/variadic-predicate-proxy >=     quantum.core.numeric/>=*))


;_____________________________________________________________________
;================={   MORE COMPLEX OPERATIONS    }====================
;°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°

; TODO fix and turn into defnt
(defn quot
  [x n]
  {:pre [(integer? x) (integer? n)]}
  (.divide x n))

(defnt rem
  ([^number?                           x n] (core/rem x n))
  ([^com.gfredericks.goog.math.Integer x n] (.modulo  x n)))

(defnt mod
  ([^number? x n] (core/mod x n))
  ([^com.gfredericks.goog.math.Integer x n]
    (let [y (rem x n)]
      (cond-> y (.isNegative y) (.add n)))))

; TODO incorporate bigint into these functions
(defalias min-2 core/min)
(defalias max-2 core/max)
;_____________________________________________________________________
;================={   TRIGONOMETRIC FUNCTIONS    }====================
;°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°

; ===== SINE ===== ;

(defn asin  [x] (js/Math.asin  x))
(defn asinh [x] (js/Math.asinh x))
(defn sin   [x] (js/Math.sin   x))
(defn sinh  [x] (js/Math.sinh  x))

; ===== COSINE ===== ;

(defn acos  [x] (js/Math.acos  x))
(defn acosh [x] (js/Math.acosh x))
(defn cos   [x] (js/Math.cos   x))
(defn cosh  [x] (js/Math.cosh  x))

; ===== TANGENT ===== ;

(defn atan  [x] (js/Math.atan  x))
(defn atanh [x] (js/Math.atanh x))
(defn tan   [x] (js/Math.tan   x))
(defn tanh  [x] (js/Math.tanh  x))

(defn atan2 [x y] (js/Math.atan2 x y))

; ===== DEGREES + RADIANS ===== ;

;_____________________________________________________________________
;==============={   OTHER MATHEMATICAL FUNCTIONS   }==================
;°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°

; ===== POWERS, EXPONENTS, LOGARITHMS, ROOTS ===== ;

(defn e-exp         [x] (js/Math.exp   x))
(defn exp           [x] (js/Math.pow   x))
(defn log-e         [x] (js/Math.log   x))
(defn log-2         [x] (js/Math.log2  x))
(defn log-10        [x] (js/Math.log10 x))

(defn sqrt          [x] (js/Math.sqrt  x))
(defn cube-root     [x] (js/Math.cbrt  x))

; ===== ROUNDING ===== ;

(defn round         [x] (js/Math.round x))

; ===== TRUNCATING ===== ;

(defn ceil          [x] (js/Math.ceil  x))
(defn floor         [x] (js/Math.floor x))

; ===== MISCELLANEOUS ===== ;

(defn hypot         [x] (js/Math.hypot x))
(defn leading-zeros [x] (js/Math.clz32 x))

