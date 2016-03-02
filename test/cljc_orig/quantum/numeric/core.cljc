(ns quantum.numeric.core
  (:require-quantum [:lib])
  (:require [quantum.core.numeric :refer [exp]] ))


#?(:clj (defmacro sqrt [x] `(Math/sqrt ~x)))
(def $ exp)

(defn quartic-root [a b c d]
  (let [A (+ (* 2  ($ b 3))
             (* -9 a b c)
             (* 27 ($ c 2))
             (* 27 ($ a 2) d)
             (* -72 b d))])
  (exp (/ (+ A
             (sqrt
               (+ (* -4 ($ (+ ($ b 2)
                              (* -3 a c)
                              (* 12 d))
                           3))
                  ($ A 2))))
          54)
       (/ 1 3)))

; slash, ratios
(def scales
  {:minor-second   (/ 16 15)
   :major-second   (/ 9 8)
   :minor-third    (/ 6 5)
   :major-third    (/ 5 4)
   :perfect-fourth (/ 4 3)
   :aug-fourth     (/ 1.411 1) ; TODO more exact
   :perfect-fifth  (/ 3 2)
   :minor-sixth    (/ 8 5)
   :golden         (/ 1.61803 1) ; TODO more exact
   :major-sixth    (/ 5 3)
   :minor-seventh  (/ 16 9)
   :major-seventh  (/ 15 8)
   :octave         (/ 2 1)
   :major-tenth    (/ 5 2)
   :major-eleventh (/ 8 3)
   :major-twelfth  (/ 3 1)
   :double-octave  (/ 4 1)})