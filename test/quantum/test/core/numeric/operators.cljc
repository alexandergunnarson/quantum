(ns quantum.test.core.numeric.operators
  (:require [quantum.core.numeric.operators :as ns]))

; ===== ADD =====

(defn test:+* [x y])

(defn test:+' ([x]) ([x y]))

(defn test:+ [x y])

; ===== SUBTRACT =====

(defn test:-* ([x]) ([x y]))

(defn test:-' ([x]) ([x y]))

(defn test:- ([x]) ([x y]))

; ===== MULTIPLY ===== ;

(defn test:** ([x]) ([x y]))

(defn test:*' ([x]) ([x y]))

(defn test:* ([x]) ([x y]))

; ===== DIVIDE ===== ;

(defn test:div* ([x]) ([x y]))

(defn test:div' ([x]) ([x y]))

(defn test:div ([x]) ([x y]))

; ===== UNARY ===== ;

(defn test:inc* [x])
(defn test:inc' [x])
(defn test:inc  [x])

(defn test:dec* [x])
(defn test:dec' [x])
(defn test:dec  [x])

(defn test:abs' [x])
(defn test:abs [x])