(ns quantum.test.core.process
  (:require [quantum.core.process :as ns]))

(defn test:->proc
  [command args & [{:keys [env-vars dir pr-to-out?]
                    :as opts}]])