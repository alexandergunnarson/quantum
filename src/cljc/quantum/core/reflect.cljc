(ns quantum.core.reflect
  (:require-quantum [:core])
  #?(:clj (:require [clojure.jvm.tools.analyzer :as ana])))

#?(:clj
(defmalias
  ^{:doc "Call a private field, must be known at compile time. Throws an error
          if field is already publicly accessible."}
  field ana/field))