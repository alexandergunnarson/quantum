(ns quantum.test.apis.amazon.cloud-drive.auth
  (:require [quantum.apis.amazon.cloud-drive.auth :refer :all]))

(defn test:retrieve-authorization-code [user])

(defn test:initial-auth-tokens-from-code [user code])

(defn test:refresh-token!
  [user])

