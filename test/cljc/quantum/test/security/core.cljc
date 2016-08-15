(ns quantum.test.security.core
  (:require [quantum.security.core :as ns]))

#?(:clj
(defn test:ssl-context [type key-file cert-file ca-cert-file]))