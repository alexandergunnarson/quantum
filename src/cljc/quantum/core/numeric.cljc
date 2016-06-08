(ns
  ^{:doc "Useful numeric functions. Floor, ceil, round, sin, abs, neg, etc."
    :attribution "Alex Gunnarson"
    :cljs-self-referring? true}
  quantum.core.numeric
  (:refer-clojure :exclude
    [* *' + +' - -' / < > <= >= == rem inc dec zero? neg? pos? min max quot mod format
     #?@(:clj  [bigint biginteger bigdec numerator denominator inc' dec'])])
           (:require  
            #?(:cljs [com.gfredericks.goog.math.Integer :as int     ])
                     [#?(:clj  clojure.core
                         :cljs cljs.core   )            :as core    ]
            #?(:clj  [quantum.core.numeric.clj          :as clj     ])
            #?(:cljs [quantum.core.numeric.cljs         :as cljs    ])
                     [quantum.core.convert.primitive    :as pconvert
                       :refer [#?(:clj ->long)]                     ]
                     [quantum.core.error                :as err
                       :refer [->ex]                                ]
                     [quantum.core.fn
                       :refer [#?@(:clj [f*n])]                     ]
                     [quantum.core.logic                :as logic
                       :refer [#?@(:clj [fn-and whenf*n])]          ]
                     [quantum.core.macros               :as macros
                       :refer [#?@(:clj [defnt defnt' deftransmacro])]]
                     [quantum.core.vars                 :as var
                       :refer [#?(:clj defalias)]                   ])
  #?(:cljs (:require-macros
                     [quantum.core.fn
                       :refer [f*n]                                 ]
                     [quantum.core.logic                :as logic
                       :refer [fn-and whenf*n]                      ]
                     [quantum.core.macros               :as macros
                       :refer [defnt defnt' deftransmacro]          ]
                     [quantum.core.vars                 :as var
                       :refer [defalias]                            ]
                     [quantum.core.numeric
                       :refer [< > <= >=]]))
  #?(:clj  (:import  [java.nio ByteBuffer]      
                     [quantum.core Numeric] ; loops?
                     [net.jafama FastMath]
                     clojure.lang.BigInt
                     java.math.BigDecimal))) 

; (if a b true) => (or (not a) b)

; TODO look at https://github.com/clojure/math.numeric-tower/

; TODO benchmark against https://github.com/gfredericks/cljs-numbers/blob/master/test-cljs/cljs_numbers/test.cljs

; http://blog.juma.me.uk/2011/02/23/performance-of-fastmath-from-commons-math/
; http://blog.element84.com/improving-java-math-perf-with-jafama.html

; If you want your software to have the exact same result regardless 
; of hardware. Java provides the StrictMath class for that purpose.
; It's slower but is guaranteed to have the same answer regardless of hardware.

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

; TODO just for now
(def type-convert-form identity)

#?(:clj
(defmacro num-literals
  "Allows BigInts and ratio literals, converting them to calls to the
  appropriate constructors. Converts any Long to a bigint, so if you
  need a double use e.g. `42.0` instead of `42`."
  ([form]
     (type-convert-form form))
  ([form1 form2 & more]
     (cons 'do (map type-convert-form (list* form1 form2 more))))))

(def num-ex (->ex :overflow "Numeric overflow"))

#?(:clj (defalias exact? clj/exact?))

(defalias numerator   #?(:clj core/numerator   :cljs cljs/numerator  ))
(defalias denominator #?(:clj core/denominator :cljs cljs/denominator))
;_____________________________________________________________________
;==================={       CONVERSIONS        }======================
;°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°

#?(:clj (defalias ->big-integer clj/->big-integer))
        (defalias ->bigint    #?(:clj clj/->bigint :cljs cljs/->bigint))
        (defalias bigint ->bigint)
        (defalias ->ratio     #?(:clj clj/->ratio  :cljs cljs/->ratio ))

#?(:clj (defalias ->bigdec clj/->bigdec))
#?(:clj (defalias bigdec   clj/bigdec  ))

#?(:clj
(defnt exactly
  ([#{decimal?} x]
    (-> x rationalize exactly))
  ([#{int? long?} x] (->bigint x))
  ([#{bigint? clojure.lang.Ratio} x] x)))

;_____________________________________________________________________
;==================={        OPERATORS         }======================
;°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°
; Auto-unboxes; no boxed combinations necessary

; ===== ADD ===== ;

(defalias +-2 #?(:clj clj/+-2 :cljs cljs/+-2))

;(defalias +  #?(:clj clj/+  :cljs cljs/+))
#?(:clj 
(macros/variadic-proxy +*
  quantum.core.numeric.clj/+-2
  quantum.core.numeric.cljs/+-2)) ; +* is unchecked

; ===== SUBTRACT =====

(defalias --base #?(:clj clj/--base :cljs cljs/--base))

;(defalias -  #?(:clj clj/-  :cljs cljs/-))
(defalias -' #?(:clj clj/-' :cljs cljs/-'))
#?(:clj 
(macros/variadic-proxy -*
  quantum.core.numeric.clj/--base
  quantum.core.numeric.cljs/--base))

; ===== MULTIPLY ===== ;

(defalias *-2 #?(:clj clj/*-2 :cljs cljs/*-2))

#?(:clj (defalias *-exact clj/*-exact))

;(defalias * #?(:clj clj/* :cljs cljs/*))
#?(:clj
(macros/variadic-proxy **
  quantum.core.numeric.clj/*-2
  quantum.core.numeric.cljs/*-2))

; ===== DIVIDE ===== ;

(defalias div-2 #?(:clj clj/div-2 :cljs cljs/div-2))

;(defalias / #?(:clj clj// :cljs cljs//))
#?(:clj
(macros/variadic-proxy div*
  quantum.core.numeric.clj/div-2
  quantum.core.numeric.cljs/div-2))

;_____________________________________________________________________
;==================={        PREDICATES        }======================
;°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°

(defalias neg?  #?(:clj clj/neg?  :cljs cljs/neg? ))
(defalias pos?  #?(:clj clj/pos?  :cljs cljs/pos? ))
(defalias zero? #?(:clj clj/zero? :cljs cljs/zero?))

(defn nneg?     [x] (neg? x))
(defn pos-int?  [x] (and (integer? x) (pos?  x)))
(defn nneg-int? [x] (and (integer? x) (nneg? x)))

#?(:clj (defalias not-num? clj/not-num?))

;_____________________________________________________________________
;==================={   UNARY MATH OPERATORS   }======================
;°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°

(defalias inc  #?(:clj clj/inc  :cljs cljs/inc ))
(defalias inc' #?(:clj clj/inc' :cljs cljs/inc'))
(defalias inc* #?(:clj clj/inc* :cljs cljs/inc*))

(defalias dec' #?(:clj clj/dec' :cljs cljs/dec'))
(defalias dec  #?(:clj clj/dec  :cljs cljs/dec ))
(defalias dec* #?(:clj clj/dec* :cljs cljs/dec*))

(defalias abs  #?(:clj clj/abs  :cljs cljs/abs ))
#?(:clj (defalias abs' clj/abs'))

(defn sign [n] (if (neg? n) -1 1))

(defalias sign' #?(:clj clj/sign' :cljs cljs/sign'))

;_____________________________________________________________________
;==================={        COMPARISON        }======================
;°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°

(defalias <-2 #?(:clj clj/<-2 :cljs cljs/<-2))
(macros/variadic-predicate-proxy <
  quantum.core.numeric.clj/<-2
  quantum.core.numeric.cljs/<-2)

(defalias >-2 #?(:clj clj/>-2 :cljs cljs/>-2))
(macros/variadic-predicate-proxy >
  quantum.core.numeric.clj/<-2
  quantum.core.numeric.cljs/<-2)

(defalias <=-2 #?(:clj clj/<=-2 :cljs cljs/<=-2))
(macros/variadic-predicate-proxy <=
  quantum.core.numeric.clj/<-2
  quantum.core.numeric.cljs/<-2)

(defalias >=-2 #?(:clj clj/>=-2 :cljs cljs/>=-2))
(macros/variadic-predicate-proxy >=
  quantum.core.numeric.clj/<-2
  quantum.core.numeric.cljs/<-2)

(defalias =-2 #?(:clj clj/=-2 :cljs cljs/=-2))
(macros/variadic-proxy ==
  quantum.core.numeric.clj/<-2
  quantum.core.numeric.cljs/<-2)

(defalias not=-2 #?(:clj clj/not=-2 :cljs cljs/not=-2))
(macros/variadic-predicate-proxy not==
  quantum.core.numeric.clj/<-2
  quantum.core.numeric.cljs/<-2)

;_____________________________________________________________________
;================={          CONSTANTS           }====================
;°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°

#?(:clj  (defonce ^:const ZERO 0       )
   :cljs (defonce         ZERO int/ZERO))
#?(:clj  (defonce ^:const ONE  1       )
   :cljs (defonce         ONE  int/ONE ))

; For units
(defonce ^:const ten              (#?(:clj ->long :cljs int)   10    ))
(defonce ^:const hundred          (#?(:clj ->long :cljs int)   100   ))
(defonce ^:const thousand         (#?(:clj ->long :cljs int)   1000  ))
(defonce ^:const ten-thousand     (#?(:clj ->long :cljs int)   10000 ))
(defonce ^:const hundred-thousand (#?(:clj ->long :cljs int)   100000))
(defonce ^:const million          (#?(:clj ->long :cljs int)   1E6   ))
(defonce ^:const billion          (#?(:clj ->long :cljs int)   1E9   ))
(defonce ^:const trillion         (#?(:clj ->long :cljs int)   1E12  ))
(defonce ^:const quadrillion      (#?(:clj ->long :cljs int)   1E15  ))
(defonce ^:const quintillion      (#?(:clj ->long :cljs int)   1E18  )) ; + exa | - atto
(defonce ^:const sextillion       #?(:clj (core/bigint 1E21  ) :cljs 0))
(defonce ^:const septillion       #?(:clj (core/bigint 1E24  ) :cljs 0))
(defonce ^:const octillion        #?(:clj (core/bigint 1E27  ) :cljs 0))
(defonce ^:const nonillion        #?(:clj (core/bigint 1E30  ) :cljs 0))
(defonce ^:const decillion        #?(:clj (core/bigint 1E33  ) :cljs 0))

;_____________________________________________________________________
;================={   MORE COMPLEX OPERATIONS    }====================
;°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°

(defalias quot #?(:clj clj/quot :cljs cljs/quot))
(defalias rem  #?(:clj clj/rem  :cljs cljs/rem ))
(defalias mod  #?(:clj clj/mod  :cljs cljs/mod ))

(defalias min-2  #?(:clj clj/min-2 :cljs cljs/min-2))
(macros/variadic-proxy min
  quantum.core.numeric.clj/min-2
  quantum.core.numeric.cljs/min-2)

(defalias max-2  #?(:clj clj/max-2 :cljs cljs/max-2))
(macros/variadic-proxy max
  quantum.core.numeric.clj/max-2
  quantum.core.numeric.cljs/max-2)

(defn approx=
  "Return true if the absolute value of the difference between x and y
   is less than eps."
  [x y eps]
  (core/< (abs (core/- x y)) eps)) ; TODO use < and -

(defn within-tolerance? [n total tolerance]
  (and (core/>= n (core/- total tolerance)) ; TODO use >= and -
       (core/<= n (core/+ total tolerance)))) ; TODO use <= and +

(def  int-nil   (whenf*n nil? (constantly 0)))

(defn evenly-divisible-by? [a b] (= 0 (rem a b))) ; TODO use ==

;_____________________________________________________________________
;================={   TRIGONOMETRIC FUNCTIONS    }====================
;°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°
; ===== SINE ===== ;

(defalias asin #?(:clj clj/asin :cljs cljs/asin))

#?(:clj (defalias asin* clj/asin*))

#?(:cljs (defalias asinh cljs/asinh))

(defalias sin #?(:clj clj/sin :cljs cljs/sin))

#?(:clj (defalias sin* clj/sin*))

(defalias sinh #?(:clj clj/sinh :cljs cljs/sinh))

#?(:clj (defalias sinh* clj/sinh*))

; ===== COSINE ===== ;

(defalias acos #?(:clj clj/acos :cljs cljs/acos))

#?(:clj (defalias acos* clj/acos*))

(defalias cos #?(:clj clj/cos :cljs cljs/cos))

#?(:clj (defalias cos* clj/cos*))

(defalias cosh #?(:clj clj/cosh :cljs cljs/cosh))

#?(:clj (defalias cosh* clj/cosh*))

; ===== TANGENT ===== ;

(defalias atan #?(:clj clj/atan :cljs cljs/atan))
#?(:clj (defalias atan* clj/atan*))

#?(:cljs (defalias atanh cljs/atanh))

(defalias tan #?(:clj clj/tan :cljs cljs/tan))
#?(:clj (defalias tan* clj/tan*))

(defalias tanh #?(:clj clj/tanh :cljs cljs/tanh))
#?(:clj (defalias tanh* clj/tanh*))

(defalias atan2 #?(:clj clj/atan2 :cljs cljs/atan2))

#?(:clj (defalias atan2* clj/atan2*))

; ===== DEGREES + RADIANS ===== ;

#?(:clj (defalias radians->degrees clj/radians->degrees))
#?(:clj (defalias degrees->radians clj/degrees->radians))

;_____________________________________________________________________
;==============={   OTHER MATHEMATICAL FUNCTIONS   }==================
;°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°

; ===== POWERS/EXPONENTS, LOGARITHMS, ROOTS ===== ;

(defalias e-exp #?(:clj clj/e-exp :cljs cljs/e-exp))
#?(:clj (defalias e-exp* clj/e-exp*))

(defalias exp          #?(:clj clj/exp          :cljs cljs/exp))
(defalias exp-protocol #?(:clj clj/exp-protocol :cljs cljs/exp-protocol))
#?(:clj (defalias exp' clj/exp'))
#?(:clj (defalias exp* clj/exp*))
#?(:clj (defalias expm1* clj/expm1*))

(defalias log-e #?(:clj clj/log-e :cljs cljs/log-e))
#?(:clj (defalias log-e* clj/log-e*))
(defalias ln log-e)

#?(:cljs (defalias log-2 cljs/log-2))

(defalias log-10 #?(:clj clj/log-10 :cljs cljs/log-10))
#?(:clj (defalias log-10* clj/log-10*))

#?(:clj (defalias log1p* clj/log1p*))

(defnt' log*
  ([#?(:clj #{double}) x #?(:clj #{double}) base] ; arbitrary to choose ln vs. log-10
    (div* (ln x) (ln base))))

#?(:clj
(defmacro log [base x]
  `(log* (double ~x) (double ~base))))

(defalias cube-root #?(:clj clj/cube-root :cljs cljs/cube-root))

#?(:clj (defalias cube-root* clj/cube-root*))

(defalias sqrt #?(:clj clj/sqrt :cljs cljs/sqrt))

; ===== ROUNDING ===== ;

#?(:clj (defalias next-after clj/next-after))
#?(:clj (defalias next-down  clj/next-down ))
#?(:clj (defalias next-up    clj/next-up   ))

(defalias round #?(:clj clj/round :cljs cljs/round))
#?(:clj (defalias round' clj/round'))
#?(:clj (defalias rint clj/rint))

; ===== TRUNCATING ===== ;

#?(:clj (deftransmacro ceil quantum.core.numeric.clj/ceil quantum.core.numeric.cljs/ceil))
#?(:clj (defalias ceiling ceil))

(defalias floor #?(:clj clj/floor :cljs cljs/floor))
#?(:clj (defalias floor-div clj/floor-div))
#?(:clj (defalias floor-mod clj/floor-mod))

; ===== MISCELLANEOUS ===== ;

#?(:clj (defalias scalb clj/scalb))
#?(:clj (defalias ulp   clj/ulp  ))

(defalias hypot #?(:clj clj/hypot :cljs cljs/hypot))
#?(:clj (defalias hypot* clj/hypot*))

#?(:clj (defalias ieee-remainder clj/ieee-remainder))

#?(:clj (defalias with-sign clj/with-sign))

#?(:cljs (defalias leading-zeros cljs/leading-zeros))

;_____________________________________________________________________
;==============={        OTHER OPERATIONS          }==================
;°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°

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
  ([a] (core/- (int-nil a)))
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
  {:attribution "taoensso.encore, possibly via weavejester.medley"}
  [coll & [?comparator]]
  (let [comparator (or ?comparator rcompare)]
    (reduce
      (fn ([] nil) ([a b] (if (neg? (comparator a b)) b a)))
      coll)))

(defn greatest-or [a b else]
  (cond (core/> a b) a ; TODO use >
        (core/> b a) b ; TODO use >
        :else else))

(defn least-or [a b else]
  (cond (core/< a b) a ; TODO use <
        (core/< b a) b ; TODO use <
        :else else))

(defn approx? [tolerance a b]
  (-> (core/- (int-nil a) (int-nil b)) abs (core/< tolerance))) ; TODO use - and <

(defn whole-number? [n]
  (= n (floor n))) ; TODO use ==

; (defn whole? [n]
;   (assert (instance? Double n))
;   (= (mod n 1) 0.0))

(defn divisible?
  [num div]
  (zero? (mod num div)))

(defn indivisible?
  [num div]
  (not (divisible? num div)))

#?(:clj (defalias native-integer? clj/native-integer?))

; PROPERTIES OF NUMERIC FUNCTIONS

(def inverse-map ; some better way of doing this?
  {core/+ core/-
   core/- core/+
   core// core/*
   core/* core//})

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
  {core/+ 0
   core/- 0
   core// 1
   core/* 1})

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
       (mapcat (fn [x] [x (div* n x)]))
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

;___________________________________________________________________________________________________________________________________
;=================================================={         MUTATION         }=====================================================
;=================================================={                          }=====================================================
#?(:clj (defalias += clj/+=))
#?(:clj (defalias -= clj/-=))
#?(:clj (defalias ++ clj/++))
#?(:clj (defalias inc! ++))
#?(:clj (defalias -- clj/--))
#?(:clj (defalias dec! ++))

;___________________________________________________________________________________________________________________________________
;=================================================={         DISPLAY          }=====================================================
;=================================================={                          }=====================================================
#?(:clj (defalias display-num   clj/display-num  ))
#?(:clj (defalias format        clj/format       ))
#?(:clj (defalias percentage-of clj/percentage-of))

(def percent? (fn-and (f*n core/>= 0) (f*n core/<= 1))) ; TODO use >= and <=