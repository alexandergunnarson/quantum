(ns quantum.test.core.io.utils
  (:require [quantum.core.io.utils :as ns]))

; ===== DEPENDENCIES =====

(defn test:escape-illegal-chars [str-0])

(defn test:readable? [dir])

(defn test:writable? [dir])

(defn test:create-dir! [dir-0])

(defn test:num-to-sortable-str [num-0])

(defn test:path->file-name [x])

(defn test:next-file-copy-num [path-0])

; ===== EXTENSIONS =====

(defn test:create-temp-file! [file-name suffix])

(defn test:with-temp-file
  [[name data suffix] & body])

(defn test:file-reader
  ([])
  ([{:keys [on-load on-load-end] :as opts}]))

(defn test:ByteEntity [])