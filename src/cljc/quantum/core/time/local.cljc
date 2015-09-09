(ns
  ^{:doc "An alias of the clj-time.local namespace."
    :attribution "Alex Gunnarson"}
  quantum.core.time.local
  (:require [quantum.core.ns :as ns #?@(:clj [:refer [alias-ns]])])
  #?(:clj (:gen-class)))

#_(:clj (alias-ns 'clj-time.local))