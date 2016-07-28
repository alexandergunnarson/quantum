(ns quantum.test.auth.core
  (:require [quantum.auth.core :refer :all]))

(defn test:get
  ([auth-source])
  ([auth-source k])) 

(defn test:get-in
  [auth-source ks])

(defn test:assoc!
  [auth-source map-f])

(defn test:assoc-in! [auth-source & kvs])

(defn test:dissoc-in! [auth-source & ks])

(defn test:access-token
  [auth-source service])


