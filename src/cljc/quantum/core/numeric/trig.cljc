(ns quantum.core.numeric.trig
          (:refer-clojure :exclude
            [+ * /])
          (:require
            [quantum.core.error             :as err
              :refer [TODO]                        ]
            [quantum.core.macros
              :refer        [#?@(:clj [defnt defnt'])]
              :refer-macros [defnt]]
            [quantum.core.numeric.exponents :as exp
              :refer        [#?@(:clj [log-e sqrt pow])]
              :refer-macros [log-e sqrt pow]]
            [quantum.core.numeric.operators
              :refer        [#?@(:clj [+ * / inc* dec*])]
              :refer-macros [+ * / inc* dec*]])
  #?(:clj (:import [net.jafama FastMath])))

; ===== SINE ===== ;

#?(:clj  (defnt' asin "arc sine"
           (^double [^double x] (Math/asin x)))
   :cljs (defn asin "arc sine" [x] (js/Math.asin x)))

#?(:clj
(defnt asin*
  "arc sine, fast+lax"
  {:performance ["3.8 times faster than java.lang.Math"
                 "Worst case 2E-12 difference"]}
  (^double [^double x] (FastMath/asin x))))

#?(:clj  (defnt asinh
           {:performance "Unoptimized, but that's okay for now."}
           (^double [^double x]
             (log-e (+ x (sqrt (inc* (pow x 2)))))))
   :cljs (defn asinh [x] (js/Math.asinh x)))

#?(:clj  (defnt' sin "sine"
           (^double ^:intrinsic [^double x] (Math/sin x)))
   :cljs (defn sin "sine" [x] (js/Math.sin x)))

#?(:clj
(defnt sin*
  "sine, fast+lax"
  {:performance ["4.5 times faster than java.lang.Math"
                 "Worst case 1E-11 difference"]}
  (^double [^double x] (FastMath/sin x))))

#?(:clj  (defnt' sinh "hyperbolic sine"
           (^double [^double x] (Math/sinh x)))
   :cljs (defn sinh [x] (js/Math.sinh x)))

#?(:clj
(defnt sinh*
  "hyperbolic sine"
  {:performance ["5.5 times faster than java.lang.Math"
                 "Worst case 7E-14 difference"]}
  (^double [^double x] (FastMath/sinh x))))

; ===== COSINE ===== ;

#?(:clj  (defnt acos "arc cosine"
           (^double [^double x] (Math/acos x)))
   :cljs (defn acos "arc cosine" [x] (js/Math.acos x)))

#?(:clj  (defnt acosh
           {:performance "Unoptimized, but that's okay for now."}
           (^double [^double x]
             (log-e (+ x (* (sqrt (dec* x))
                            (sqrt (inc* x)))))))
   :cljs (defn acosh
           "hyperbolic arc cosine"
           [x] (js/Math.acosh x)))

#?(:clj
(defnt acos*
  "arc cosine"
  {:performance ["3.6 times faster than java.lang.Math"
                 "Worst case 1E-12 difference"]}
  (^double [^double x] (FastMath/acos x))))

#?(:clj  (defnt' cos "cosine"
           (^double ^:intrinsic [^double x] (Math/cos x)))
   :cljs (defn cos "cosine" [x] (js/Math.cos x)))

#?(:clj
(defnt' cos*
  "cosine"
  {:performance ["5.7 times faster than java.lang.Math"
                 "Worst case 8E-12 difference"]}
  (^double [^double x] (FastMath/cos x))))

#?(:clj  (defnt' cosh "hyperbolic cosine"
           (^double [^double x] (Math/cosh x)))
   :cljs (defn cosh "hyperbolic cosine" [x] (js/Math.cosh x)))

#?(:clj
(defnt' cosh*
  "hyperbolic cosine"
  {:performance ["5 times faster than java.lang.Math"
                 "Worst case 4E-14 difference"]}
  (^double [^double x] (FastMath/cosh x))))

; ===== TANGENT ===== ;

#?(:clj  (defnt' atan "arc tangent"
           (^double [^double x] (Math/atan x)))
   :cljs (defn atan "arc tangent" [x] (js/Math.atan x)))

#?(:clj
(defnt atan*
  "arc tangent"
  {:performance ["6.2 times faster than java.lang.Math"
                 "Worst case 5E-13 difference"]}
  (^double [^double x] (FastMath/atan x))))

#?(:clj  (defnt atanh
           {:performance "Unoptimized, but that's okay for now."}
           (^double [^double x]
             (/ (- (log-e (+ 1 x))
                   (log-e (- 1 x)))
                2)))
   :cljs (defn atanh [x] (js/Math.atanh x)))

#?(:clj  (defnt' atan2 "returns angle theta"
           (^double ^:intrinsic [^double x ^double y] (Math/atan2 x y)))
   :cljs (defn atan2 "returns angle theta"
           [x y] (js/Math.atan2 x y)))

#?(:clj
(defnt atan2*
  "returns angle theta"
  {:performance ["6.3 times faster than java.lang.Math"
                 "Worst case 4E-13 difference"]}
  (^double [^double x ^double y] (FastMath/atan2 x y))))

#?(:clj  (defnt' tan "tangent"
           (^double ^:intrinsic [^double x] (Math/tan x)))
   :cljs (defn tan "tangent" [x] (js/Math.tan x)))

#?(:clj
(defnt tan*
  "tangent"
  {:performance ["3.7 times faster than java.lang.Math"
                 "Worst case 1E-13 difference"]}
  (^double [^double x] (FastMath/tan x))))

#?(:clj  (defnt' tanh "hyperbolic tangent"
           (^double [^double x] (Math/tanh x)))
   :cljs (defn tanh "hyperbolic tangent" [x] (js/Math.tanh x)))

#?(:clj
(defnt tanh*
  "hyperbolic tangent"
  {:performance ["6.4 times faster than java.lang.Math"
                 "Worst case 5E-14 difference"]}
  (^double [^double x] (FastMath/tanh x))))

; ===== DEGREES + RADIANS ===== ;

#?(:clj  (defnt' radians->degrees
           (^double [^double x] (Math/toDegrees x)))
   :cljs (defn radians->degrees [x] (TODO)))

#?(:clj  (defnt' degrees->radians
           (^double [^double x] (Math/toRadians x)))
   :cljs (defn degrees->radians [x] (TODO)))



