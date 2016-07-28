(ns quantum.test.core.numeric
  (:require [quantum.core.numeric :as ns]))

(defn test:num-literals
  ([form])
  ([form1 form2 & more])))

(defn test:exact? [x])

(defn test:numerator [x])
(defn test:denominator [x])
;_____________________________________________________________________
;==================={       CONVERSIONS        }======================
;°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°

(defn test:->big-integer [x])

(defn test:->bigint ([x]) ([x radix]))

(defn test:->bigdec [x])

(defn test:->ratio [x])

(defn test:exactly [x])

;_____________________________________________________________________
;==================={        OPERATORS         }======================
;°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°
; ===== ADD =====

(defn test:+-2 [x y])

(defn test:+* [x y])

(defn test:+-exact [x y])

(defn test:+' ([x]) ([x y]))

; ===== SUBTRACT =====

(defn test:--base [x])

(defn test:negate-exact [x])

(defn test:-' ([x]) ([x y]))

(defn test:- ([x]) ([x y]))

(defn test:-* ([x]) ([x y]))

; ===== MULTIPLY ===== ;

(defn test:*-2 ([x]) ([x y]))

(defn test:*-exact ([x]) ([x y]))

(defn test:* ([x]) ([x y]))

(defn test:** ([x]) ([x y]))

; ===== DIVIDE ===== ;

(defn test:div-2 ([x]) ([x y]))

(defn test:div ([x]) ([x y]))

(defn test:div-exact ([x]) ([x y]))

(defn test:div* ([x]) ([x y]))

;_____________________________________________________________________
;==================={        PREDICATES        }======================
;°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°

(defn test:zero? [x])
(defn test:neg? [x])
(defn test:pos? [x])
(defn test:nneg? [x])
(defn test:pos-int? [x])
(defn test:nneg-int? [x])
(defn test:not-num? [x])

;_____________________________________________________________________
;==================={   UNARY MATH OPERATORS   }======================
;°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°

(defn test:inc  [x])
(defn test:inc' [x])
(defn test:inc* [x])

(defn test:dec  [x])
(defn test:dec' [x])
(defn test:dec* [x])

(defn test:abs' [x])
(defn test:abs [x])

(defn test:sign [n])
(defn test:sign' [n])

;_____________________________________________________________________
;==================={        COMPARISON        }======================
;°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°

(defn test:<-2    [a b])
(defn test:<=-2   [a b])
(defn test:>-2    [a b])
(defn test:>=-2   [a b])
(defn test:=-2    [a b])
(defn test:not=-2 [a b])

;_____________________________________________________________________
;================={   MORE COMPLEX OPERATIONS    }====================
;°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°

(defn test:rem [n div])
(defn test:quot [n div])
(defn test:mod [n div])

(defn test:max' ([]) ([x y]))

(defn test:max-2 [x y])

(defn test:min-2 [x y])


(defn test:approx= [x y eps])

(defn test:within-tolerance? [n total tolerance])

(defn test:int-nil [n])

(defn test:evenly-divisble-by? [a b])

;_____________________________________________________________________
;================={   TRIGONOMETRIC FUNCTIONS    }====================
;°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°
; ===== SINE ===== ;

(defn test:asin [x])

(defn test:asin* [x])

(defn test:sin [x])

(defn test:sin* [x])

(defn test:sinh [x])

(defn test:sinh* [x])

; ===== COSINE ===== ;

(defn test:acos [x])

(defn test:acos* [x])

(defn test:cos [x])

(defn test:cos* [x])

(defn test:cosh [x])

(defn test:cosh* [x])

; ===== TANGENT ===== ;

(defn test:atan [x])

(defn test:atan* [x])

(defn test:atan2 [x])

(defn test:atan2* [x])

(defn test:tan [x])

(defn test:tan* [x])

(defn test:tanh [x])

(defn test:tanh* [x])

; ===== DEGREES + RADIANS ===== ;

(defn test:radians->degrees [x])
(defn test:degrees->radians [x])

;_____________________________________________________________________
;==============={   OTHER MATHEMATICAL FUNCTIONS   }==================
;°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°

; ===== POWERS, EXPONENTS, LOGARITHMS, ROOTS ===== ;

(defn test:e-exp [x])

(defn test:e-exp* [x])

(defn test:log-e [x])

(defn test:log-e* [x])

(defn test:log-2 [x])

(defn test:log-10 [x])

(defn test:log-10* [x])

(defn test:log1p* [x])

(defn test:log* [x])

(defn test:log [x])

(defn test:exp' [x])

(defn test:exp [x])

(defn test:exp* [x])

(defn test:expm1* [x])

(defn test:get-exp [x])

; ===== POWERS/EXPONENTS, LOGARITHMS, ROOTS ===== ;

(defn test:cube-root [x])
(defn test:cube-root* [x])
(defn test:sqrt [x])

; ===== ROUNDING ===== ;

(defn test:rint [x])

(defn test:round [num-0 & {:keys [type to] :or {to 0}}])
(defn test:round' [x])

(defn test:next-after [start direction])
(defn test:next-down [x])
(defn test:next-up [x])

; ===== TRUNCATING ===== ;

(defn test:ceil [x])
(defn test:floor [x])

(defn test:floor-div [x])
(defn test:floor-mod [x])

; ===== MISCELLANEOUS ===== ;

(defn test:with-sign [x y])

(defn test:scalb [x y])

(defn test:hypot [x y])
(defn test:hypot* [x y])

(defn test:ieee-remainder [x y])

(defn test:ulp [n])
(defn test:exactly [n])

(defn test:gcd ([a b]) ([a b & args]))

(defn test:leading-zeros ([a b]) ([a b & args]))

;_____________________________________________________________________
;==============={        OTHER OPERATIONS          }==================
;°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°

(defn test:safe+
  ([a    ])
  ([a b  ])
  ([a b c])
  ([a b c & args]))

(defn test:safe*
  ([a    ])
  ([a b  ])
  ([a b c])
  ([a b c & args]))

(defn test:safe-
  ([a])
  ([a b])
  ([a b c])
  ([a b c & args]))

(defn test:safediv
  ([a b  ])
  ([a b c])
  ([a b c & args]))

(defn test:rcompare
  [x y])

(defn test:greatest
  [coll & [?comparator]])

(defn test:least
  [coll & [?comparator]])

(defn test:greatest-or [a b else])

(defn test:least-or [a b else])

(defn test:approx? [tolerance a b])

(defn test:whole-number? [n])

(defn test:divisible? [num div])
(defn test:indivisible? [num div])

(defn test:native-integer? [n])

(defn test:inverse [f])

(defn test:base [f])

;___________________________________________________________________________________________________________________________________
;=================================================={         MUTATION         }=====================================================
;=================================================={                          }=====================================================
(defn test:+= [x a])
(defn test:-= [x a])

(defn test:++ [x])
(defn test:-- [x])

;___________________________________________________________________________________________________________________________________
;=================================================={         DISPLAY          }=====================================================
;=================================================={                          }=====================================================
(defn test:display-num [x])

(defn test:format [n type])

(defn test:percentage-of [of total-n])

(defn test:percent? [n])
