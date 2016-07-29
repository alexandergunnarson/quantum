(ns quantum.test.core.collections.selective
  (:require [quantum.core.collections.selective :as ns]))

(defn test:in?
  [elem coll])

(defn test:in-k?
  [elem coll])

(defn test:in-v?
 [elem coll])

; ;-----------------------{       SELECT-KEYS       }-----------------------
(defn test:select-keys
  [keyseq m])

(defn test:select-keys+
  [ks m])

; ;-----------------------{       CONTAINMENT       }-----------------------

(defn test:get-keys
  [m obj])

(defn test:get-key
  [m obj])

(defn test:vals+
  [m])

(defn test:keys+
  [m])
