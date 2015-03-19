(ns
  ^{:doc "An alias of the clj-time.coerce namespace."
    :attribution "Alex Gunnarson"}
  quantum.core.time.coerce
  (:require [quantum.core.ns :as ns #+clj :refer #+clj [alias-ns]])
  #+clj (:gen-class))

#+clj (alias-ns 'clj-time.coerce)