(ns quantum.test.validate.core
  (:require [quantum.validate.core :as ns]))

#?(:clj
(defn test:email:user?
  [^String user]))
   
#?(:clj
(defn test:domain?
  [domain & [allow-local?]]))

#?(:clj
(defn test:email?
  [email & [allow-local?]]))
