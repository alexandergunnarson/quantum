(ns quantum.test.net.server.middleware
  (:require [quantum.net.server.middleware :refer :all]))

#_(defn test:wrap-keywordify [f])

#_(defn test:wrap-cors-resp [f])

(defn test:wrap-uid
  [app])

(defn test:content-security-policy [report-uri])

; TODO repetitive

(defn test:wrap-x-permitted-cross-domain-policies
  [handler])

(defn test:wrap-x-download-options
  [handler])

(defn test:wrap-strictest-transport-security
  [handler])

(defn test:wrap-hide-server
  [handler])

(defn test:wrap-exception-handling
  [handler])

(defn test:wrap-middleware [routes])
