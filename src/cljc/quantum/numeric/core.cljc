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