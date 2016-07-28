(ns quantum.test.net.core
  (:require [quantum.net.core :refer :all]))

; UTILS

(defn test:valid-port? [x])

(defn test:user-agent [])

(defn test:android? [])

(defn test:escape-special
  [string])

(defn ^String test:mime-type->str
  [mime-type])

(defn ^String test:normalize-encoding-type [encoding])

(defn test:normalize-content
  [content ^String mime-type])

; ===== URL =====

(defn test:url-encode
  [s & [encoding]])

(defn test:url-decode
  [s & [encoding]])

(defn test:format-url
  [m])

#?(:clj
(defn test:parse-url
  [s]))

#?(:cljs
(defn test:parse-url
  [url]))

; ===== QUERY PARAMS =====


(defn test:parse-query-params
  [s])

(defn test:format-query-params
  [m])




 ; Success (2xx)  
 ; Redirection (3xx)  
 ; Server errors (5xx)
 ; Client errors (4xx)

#_(defn test:download
  [{:keys [file-str out req]}])