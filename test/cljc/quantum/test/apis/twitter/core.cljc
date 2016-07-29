(ns quantum.test.apis.twitter.core
  (:require [quantum.apis.twitter.core :as ns]))

(defn test:gen-oauth-signature
  [email app {:keys [method url query-params]} auth-params])

(defn test:request!
  ([req])
  ([email app {:as request :keys [url method query-params timestamp]}]))

(defn test:tweets->hashtags [x])

(defn test:tweets [user-id & [{:keys [cursor parse? handlers keys-fn] :as opts} email app]])

(defn test:rate-limits [& [{:keys [parse? handlers keys-fn] :as opts} email app]])

(defn test:followers:list
  [user-id & [{:keys [cursor parse? handlers keys-fn] :as opts} email app]])

(defn test:user:id->followees:ids
  [user-id & [{:keys [cursor parse? handlers keys-fn] :as opts} email app]])

(defn test:user:id->followers:ids
  [user-id & [{:keys [cursor parse? handlers keys-fn] :as opts} email app]])

(defn test:user:id->metadata
  [user-id & [{:keys [cursor parse? handlers keys-fn] :as opts} email app]])

(defn test:user:ids->metadata
  [user-ids & [{:keys [cursor parse? handlers keys-fn] :as opts} email app]])

(defn test:tweets-by-id [tweet-ids])

(defn test:post-status! [status & [{:keys [parse? handlers keys-fn] :as opts} email app]])

(defn test:tweets-by-user
  ([user-id])
  ([user-id & [{:keys [parse? handlers keys-fn include-user?] :as opts} email app]]))

(defn test:tweets-and-user [user-id])

; HEADLESS BROWSER AUTOMATION

(defn test:sign-in!
  [username])

(defn test:create-app!
  [{:keys [username app-name description website callback-url]}])
