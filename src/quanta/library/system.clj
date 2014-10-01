(ns quanta.library.system
  (:require [quanta.library.string :as str])
  (:gen-class))

(def ^:dynamic *os* ; make less naive
  (if (str/contains? (System/getProperty "os.name") "Windows")
      :windows
      :unix))