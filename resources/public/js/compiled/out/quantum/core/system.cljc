#?(:clj (ns quantum.core.system))

(ns
  ^{:doc "System-level (envinroment) vars such as *os*."
    :attribution "Alex Gunnarson"}
  quantum.core.system
  (:require [quantum.core.collections :as coll])
  #?(:clj (:gen-class)))

#?(:clj
(def ^:dynamic *os* ; TODO: make less naive
  (if (coll/contains? (System/getProperty "os.name") "Windows")
      :windows
      :unix)))