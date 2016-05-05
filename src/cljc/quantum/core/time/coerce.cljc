(ns
  ^{:doc "An alias of the clj-time.coerce namespace."
    :attribution "Alex Gunnarson"}
  quantum.core.time.coerce
  (:require [quantum.core.vars :as vars #?@(:clj [:refer [alias-ns]])])
  #?(:clj (:gen-class)))

#_(:clj (alias-ns 'clj-time.coerce))