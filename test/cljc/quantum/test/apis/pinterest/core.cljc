(ns quantum.test.apis.pinterest.core
  (:require [quantum.apis.pinterest.core :as ns]))

(defn test:oauth-code
  [{:keys [username password] :as app-meta} & [scopes]])

(defn test:oauth-token
  [{:keys [id secret] :as app-meta} & [scopes-0]])

(defn test:refresh-oauth-token! [{:keys [name] :as app-meta}])

(defn test:board [])