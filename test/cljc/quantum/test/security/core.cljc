(ns quantum.test.security.core
  (:require [quantum.security.core :refer :all]))

#?(:clj
(defn test:ssl-context [type key-file cert-file ca-cert-file]))