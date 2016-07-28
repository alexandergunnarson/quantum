(ns quantum.test.deploy.repack
  (:require [quantum.deploy.repack :refer :all]))

#?(:clj
(defn test:repack!
  [{:keys [from-dir to-dir
           test-dir resources-dir
           license-filename logging-props-filename
           create-git?]
    :or   {license-filename       "LICENSE"
           test-dir               "test"
           resources-dir          "resources"}}]))