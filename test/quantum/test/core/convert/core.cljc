(ns quantum.test.core.convert.core
  (:require [quantum.core.convert.core :as ns]))

(defn test:->name [x])

(defn test:->symbol [x])

(defn test:->str [x])

(defn test:->hex [x])

(defn test:->mdb [x])

(defn test:utf8-string
  [bytes])

(defn test:base64-encode
  [bytes])

(defn test:base64-decode
  [^String s])


; PARSING

(defn- test:apply-unit [number unit])

(defn- test:parse-number
  [s parse-fn])

(defn test:parse-bytes
  [s])

 
(defn test:parse-integer
  [s])

(defn test:parse-long
  [s])

(defn test:parse-double
  [s])

(defn test:parse-float
  [s])
