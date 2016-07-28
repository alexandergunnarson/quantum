(ns quantum.test.core.io.core
  (:require [quantum.core.io.core :as ns]))

(defn test:stream->assoc!
  [out-path in-stream])

(defn test:assoc-unserialized!
  ([path- data])
  ([path- data {:keys [type] :or {type :string}}]))

(defn test:assoc-serialized!
  ([path-0 data])
  ([path-0 data {:keys [compress?
                        unfreezable-caught?]}]))

(defn test:try-assoc!
  [n successful? file-name-f directory-f
   file-path-f method data-formatted file-type])

(defn test:assoc!-
  ([opts])
  ([path- data])
  ([path- data- {file-name :name file-path :path
                :keys [data directory file-type file-types
                       method overwrite formatting-func]
                :or   {directory       :resources
                       file-name       "Untitled"
                       file-types      [:clj]
                       method          :serialize
                       overwrite       true
                       formatting-func identity}
                :as   options}]))

(defn test:dissoc!
  [& {file-name :name file-path :path
      :keys [directory silently?]
      :or   {silently? false}}])

(defn test:create-file! [x])

(defn test:assoc!
  [k x & [opts]])

(defn test:get
  ([unk])
  ([_ {file-name :name file-path :path
      :keys [directory file-type method]
      :or   {directory   :resources
             method :unserialize}
      :as options}]))
 
(defn test:mkdir! [x])