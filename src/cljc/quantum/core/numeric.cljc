(ns
  ^{:doc "Useful numeric functions. Floor, ceil, round, sin, abs, neg, etc."
    :attribution "Alex Gunnarson"
    :cljs-self-referring? true}
  quantum.core.numeric
  (:refer-clojure :exclude
    [* *' + +' - -' / < > <= >= == rem inc dec zero? neg? pos? pos-int?
     min max quot mod format
     #?@(:clj  [bigint biginteger bigdec numerator denominator inc' dec'])])
           (:require  
             [#?(:clj  clojure.core
                 :cljs cljs.core   )            :as core    ]
    #?(:cljs [com.gfredericks.goog.math.Integer :as int     ])
             [quantum.core.convert.primitive    :as pconvert
               :refer [#?(:clj ->long)]                     ]
             [quantum.core.error                :as err
               :refer        [->ex TODO]                    ]
             [quantum.core.fn
               :refer        [#?@(:clj [f*n fn->])]         
               :refer-macros [f*n fn->]                     ]
             [quantum.core.logic                :as logic
               :refer        [#?@(:clj [fn-and whenf*n])]   
               :refer-macros [fn-and whenf*n]               ]
             [quantum.core.macros               :as macros
               :refer        [#?@(:clj [defnt defnt'])]
               :refer-macros [defnt defnt']   ]
             [quantum.core.vars                 :as var
               :refer        [#?@(:clj [defalias defaliases])]
               :refer-macros [defalias defaliases]          ]
             [quantum.core.numeric.convert   ]
             [quantum.core.numeric.misc      ]
             [quantum.core.numeric.operators ]
             [quantum.core.numeric.predicates]
             [quantum.core.numeric.trig      ]
             [quantum.core.numeric.truncate  ]
             [quantum.core.numeric.types :as ntypes])
  #?(:cljs (:require-macros
             [quantum.core.numeric
               :refer [/ floor abs]]))
  #?(:clj  (:import
             [java.nio ByteBuffer]      
             [quantum.core Numeric] ; loops?
             [net.jafama FastMath]
             clojure.lang.BigInt
             java.math.BigDecimal))) 

; op* : Lax. Continues on overflow.
; op' : Strict. Throws on overflow.
; op  : Natural. Auto-promotes on overflow.
; op& : Lax. Provides less-accurate results in much less time.   


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

(defn type-convert-form [x] (TODO))

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

; ===== NON-TRANSFORMATIVE OPERATIONS ===== ;

(defalias numerator   ntypes/numerator)
(defalias denominator ntypes/denominator)
;_____________________________________________________________________
;==================={         CONVERT          }======================
;°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°
#?(:clj
(defaliases quantum.core.numeric.convert
  ->big-integer ->bigint ->bigdec ->ratio))
;_____________________________________________________________________
;==================={        OPERATORS         }======================
;°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°
(defaliases quantum.core.numeric.operators
  #?@(:clj [+*   +'   +
            -*   -'   -
            **   *'   *
            div* div' /
            inc* #_inc' inc
            dec* #_dec' dec
                 abs' abs])
            inc' dec')
;_____________________________________________________________________
;==================={        PREDICATES        }======================
;°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°
(defaliases quantum.core.numeric.predicates
  zero?
  #?@(:clj [neg? pos? nneg? pos-int? nneg-int? exact?]))
;_____________________________________________________________________
;==================={         TRUNCATE         }======================
;°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°
(defaliases quantum.core.numeric.truncate
  round #?@(:clj [rint round' ceil floor floor-div floor-mod]))
;_____________________________________________________________________
;==================={       MISCELLANEOUS      }======================
;°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°
(defaliases quantum.core.numeric.misc
  rem mod
  #?@(:clj [ieee-rem quot hypot hypot* sign sign'
            with-sign scalb ulp leading-zeros native-integer? gcd]))
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

(def  int-nil   (whenf*n nil? (constantly 0)))

(defn evenly-divisible-by? [a b] (= 0 (rem a b))) ; TODO use ==

#?(:clj  (defnt exactly
           ([#{decimal?} x]
             (-> x rationalize exactly))
           ([#{int? long?} x] (->bigint x))
           ([#{bigint? clojure.lang.Ratio} x] x))
   :cljs (defn exactly [x] (TODO)))

;_____________________________________________________________________
;================={   TRIGONOMETRIC FUNCTIONS    }====================
;°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°
#?(:clj
(defaliases quantum.core.numeric.trig
  asin asin* asinh sin sin* sinh sinh*
  acos acos* acosh cos cos* cosh cosh*
  atan atan* atanh tan tan* tanh tanh* atan2 atan2*
  radians->degrees degrees->radians))
;_____________________________________________________________________
;==============={            EXPONENTS             }==================
;°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°
#?(:clj
(defaliases quantum.core.numeric.exponents
  pow- pow' pow pow* expm1* get-exp
  sqrt cbrt cbrt* e-exp e-exp*
  log-e log-e* log-2 log-10 log-10* log1p* log- log))

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

(def percent? (fn-and (f*n core/>= 0) (f*n core/<= 1))) ; TODO use >= and <=

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

;___________________________________________________________________________________________________________________________________
;=================================================={         MUTATION         }=====================================================
;=================================================={                          }=====================================================
#?(:clj (defmacro += [x a] `(set! ~x (+ ~x ~a))))
#?(:clj (defmacro -= [x a] `(set! ~x (- ~x ~a))))
#?(:clj (defmacro ++ [x] `(set! ~x (inc ~x))))
#?(:clj (defmacro -- [x] `(set! ~x (dec ~x))))
#?(:clj (defalias inc! ++))
#?(:clj (defalias dec! ++))
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
  (-> of (/ total-n) double (core/* 100) display-num (str "%"))) ; TODO use *-2