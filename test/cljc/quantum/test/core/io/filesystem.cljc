(ns quantum.test.core.io.filesystem
  (:require [quantum.core.io.filesystem :as ns]))

(defn test:file-watcher
  [{:as opts :keys [file]
    {:keys [modified]} :handlers}
   {:as thread-opts :keys [close-reqs]}])
