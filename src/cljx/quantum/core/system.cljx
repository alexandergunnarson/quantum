(ns
  ^{:doc "System-level (envinroment) vars such as *os*."
    :attribution "Alex Gunnarson"}
  quantum.core.system
  (:require [quantum.core.string :as str])
  #+clj (:gen-class))

#+clj
(def ^:dynamic *os* ; TODO: make less naive
  (if (str/contains? (System/getProperty "os.name") "Windows")
      :windows
      :unix))