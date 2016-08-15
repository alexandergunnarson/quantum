(ns quantum.test.core.io.meta
  (:require [quantum.core.io.meta :as ns]))

(defn test:parse-media-metadata
  [meta-])

(defn test:file->meta:mediainfo
  [file])

#?(:clj
(defn test:file->meta:tika [file-str]))

(defn test:file->meta
  [file])
