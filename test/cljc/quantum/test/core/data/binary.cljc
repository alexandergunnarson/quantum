(ns quantum.test.core.data.binary
  (:require [quantum.core.data.binary :as ns]))

(defn test:nil? [x])
(defn test:not [x])
(defn test:true? [x])
(defn test:false? [x])

; ===== SHIFTS =====

(defn test:bits
  [x num-bits])

; ====== ENDIANNESS REVERSAL =======

(defn test:reverse [x])

(defn test:make-long [b7 b6 b5 b4 b3 b2 b1 b0])