(ns quantum.test.core.io.compress
  (:require [quantum.core.io.compress :as ns]))

(defn compress
  ([data])
  ([data {:keys [format prefer] :as options}]))

(defn decompress 
  ([x])
  ([x algorithm])
  ([x algorithm options]))