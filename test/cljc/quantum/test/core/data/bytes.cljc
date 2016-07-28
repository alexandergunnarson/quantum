(ns quantum.test.core.data.bytes
  (:require [quantum.core.data.bytes :refer :all]))

#?(:clj
(defn test:unchecked-byte-array 
  ([size-or-seq])
  ([size init-val-or-seq])))

(defn test:bytes-to-hex
  [digested])

(defn test:str->cstring
  [s])

(defn test:parse-bytes
  [encoded-bytes])