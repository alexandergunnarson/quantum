(ns quantum.test.core.io.transcode
  (:require [quantum.core.io.transcode :as ns]))

(defn test:->mp4-x265
  ([in out])
  ([in out {:keys [encode-audio?] :as opts}]))

(defn test:->mp4
  ([in-path out-path])
  ([in-path out-path opts]))

(defn test:extract-audio!
  ([in out])
  ([in out opts]))

(defn test:duration-of
  [url])