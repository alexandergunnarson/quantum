(ns quantum.test.validate.domain
  (:require [quantum.validate.domain :refer :all]))

(defn test:remove-leading-dot [s])

#?(:clj
(defn test:valid?
  [domain-0 & [allow-local?]]))

#?(:clj
(defn test:normalize-tld [tld]))

#?(:clj
(defn test:valid-tld?
  [^String tld-0 & [allow-local?]]))

#?(:clj
(defn test:valid-infrastructure-tld?
  [^String infra-tld]))
 
#?(:clj
(defn test:valid-local-tld?
  [^String local-tld])) 

#?(:clj
(defn test:valid-generic-tld?
  [^String gtld]))

#?(:clj
(defn test:valid-country-code-tld?
  [cc-tld]))



