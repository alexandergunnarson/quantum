(ns
  ^{:doc "An alias of the clj-time.format namespace."
    :attribution "Alex Gunnarson"}
  quantum.core.time.format
  (:require [quantum.core.ns :as ns #+clj :refer #+clj [alias-ns]])
  #+clj (:gen-class))

#+clj (alias-ns 'clj-time.format)