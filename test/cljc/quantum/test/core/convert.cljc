(ns quantum.test.core.convert
  (:require [quantum.core.convert :as ns]))

(defn test:transit->
  ([x type])
  ([x type opts]))

(defn test:->transit
  ([x type])
  ([x type opts]))

(defn test:->path
  [& args])

(defn test:bytes->base64
  [x])

(defn test:base64->forge-bytes [x])
(defn test:forge-bytes->base64 [x])

(defn test:?->utf-8   [x])
(defn test:utf-8->?   [x])

(defn test:bytes->hex [x])
(defn test:hex->bytes [x])

(defn test:->forge-byte-buffer
  ([])
  ([x]))

(defn test:->eval
  [x & [opts]])

(defn test:->form
  [x & [opts]])

(defn test:->char
  [x])

(defn test:read-byte
  [input])

(defn test:read-bytes
  [input n])

(defn test:->uuid* ([id]) ([msb lsb]))

(defn test:->uuid [& args])

(defn test:->inet-address [x])

(defn test:->java-path [x])

(defn test:->buffered [x])

(defn test:->observable [x]) 

(defn test:->input-stream ([x]) ([x opts])) 

(defn test:arr->vec [arr])

(defn test:file->u8arr [file])