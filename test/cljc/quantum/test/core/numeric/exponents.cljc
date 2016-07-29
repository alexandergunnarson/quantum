(ns quantum.test.core.numeric.exponents
  (:require [quantum.core.numeric.exponents :as ns]))

; ===== EXPONENTS ===== ;

(defn test:pow- [x n])
(defn test:pow' [x n])
(defn test:pow  [x n])
(defn test:pow* [x n])
(defn test:expm1* [x])
(defn test:get-exp [x])

; ===== INVERSE EXPONENTS (ROOTS) ===== ;

(defn test:sqrt [x])
(defn test:cbrt [x])
(defn test:cbrt* [x])

; ===== CONVERSE EXPONENTS (LOGARITHMS) ===== ;

(defn test:e-exp [x])
(defn test:e-exp* [x])
(defn test:log-e [x])
(defn test:log-e* [x])
(defn test:log-2 [x])
(defn test:log-10 [x])
(defn test:log-10* [x])
(defn test:log1p* [x])
(defn test:log- [x base])
(defn test:log [base x])
