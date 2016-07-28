(ns quantum.test.core.meta.bench
  (:require [quantum.core.meta.bench :as ns]))

(defn test:num-from-timing [time-str])

(defn test:time-ms
  [expr])

(defn test:profile [k & body])

(defn test:shoddy-benchmark [to-repeat func & args])

; BYTE SIZE

(defn test:byte-size [obj])

(defn test:calc-byte-size-of-all-vars
  [])