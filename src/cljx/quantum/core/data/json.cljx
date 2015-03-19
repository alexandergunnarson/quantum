(ns
  ^{:doc "A simple JSON library which aliases cheshire.core."
    :attribution "Alex Gunnarson"}
  quantum.core.data.json
  #+clj
  (:require [quantum.core.ns :as ns :refer [defalias alias-ns]])
  #+clj (:gen-class))

#+clj ; for now
(alias-ns 'cheshire.core)