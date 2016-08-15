(ns quantum.test.apis.microsoft.azure.core
  (:require [quantum.apis.microsoft.azure.core :as ns]))

(defn test:url->account-name [url])

(defn test:canonicalize-params [m])

(defn test:canonicalize-url [url query-params])

(defn test:signing-string
  ([method url headers])
  ([method url headers
    {:keys [query-params
            Content-Encoding
            Content-Language
            Content-Length
            Content-MD5
            Content-Type
            Date
            If-Modified-Since
            If-Match
            If-None-Match
            If-Unmodified-Since
            Range]}]))

(defn test:required-headers [])

(defn test:gen-headers
  ([account-key method url])
  ([account-key method url {:keys [headers query-params]}]))

(defn test:parse-response [xml])

(defn test:list-containers
  [account-name account-key])

(defn test:create-container!
  ([account-name account-key container-name])
  ([account-name account-key container-name
    {:as opts :keys [public-access-type]}]))


