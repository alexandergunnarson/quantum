(ns quantum.test.net.url
  (:require [quantum.net.url :refer :all]))

(defn test:decode
  [code-map-key s])

(defn test:encode [s])

(defn test:url-params->map
  [str-params & [decode?]])
  
(defn test:embedded-url->map
  [^String embedded-url])

(defn test:url->map [url])

(defn test:normalize-param [x])

(defn test:map->str [m])

(defn test:map->url [url m])
