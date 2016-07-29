(ns quantum.test.core.numeric.trig
  (:require [quantum.core.numeric.trig :as ns]))

; ===== SINE ===== ;

(defn test:asin [x])
(defn test:asin* [x])
(defn test:asinh [x])
(defn test:sin [x])
(defn test:sin* [x])
(defn test:sinh [x])
(defn test:sinh* [x])

; ===== COSINE ===== ;

(defn test:acos [x])
(defn test:acos* [x])
(defn test:acosh [x])
(defn test:cos [x])
(defn test:cos* [x])
(defn test:cosh [x])
(defn test:cosh* [x])

; ===== TANGENT ===== ;

(defn test:atan [x])
(defn test:atan* [x])
(defn test:atanh [x])
(defn test:tan [x])
(defn test:tan* [x])
(defn test:tanh [x])
(defn test:tanh* [x])
(defn test:atan2 [x])
(defn test:atan2* [x])

; ===== DEGREES + RADIANS ===== ;

(defn test:radians->degrees [x])
(defn test:degrees->radians [x])
