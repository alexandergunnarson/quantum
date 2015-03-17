(ns quantum.core.data.json
  #+clj
  (:require [quantum.core.ns :as ns :refer [defalias alias-ns]])
  #+clj (:gen-class))

#+clj ; for now
(alias-ns 'cheshire.core)