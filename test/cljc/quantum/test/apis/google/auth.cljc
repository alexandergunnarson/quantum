(ns quantum.test.apis.google.auth
  (:require [quantum.apis.google.auth :as ns]))
 
(defn test:assert-email [auth-keys email]
  (or (in? email auth-keys)
      (throw (->ex nil "Email not found in auth keys" email))))

(defn test:scopes-string [scopes-])

(defn test:oauth-params [{:keys [email scopes access-type]}])

#_(defn test:oauth-url [opts])

(defn test:oauth-page [opts])

; ===== LOGIN =====

#_(defn test:fill-field!
  [^RemoteWebElement elem ^String s])

(defn test:login-challenged-err [& [msg objs]])

#_(defn test:respond-to-challenge-question
  [driver label-id field-id datum-key username password])

#_(defn test:handle-challenge-question [driver username password])

#_(defn test:sign-in!
  ([^WebDriver driver ^String username ^String password])
  ([^WebDriver driver ^String auth-url ^String username ^String password])

#_(defn test:begin-sign-in-from-google-home-page!
  [^WebDriver driver])

; ===== OAUTH =====

#_(defn test:approve! [^WebDriver driver])

#_(defn ^String test:copy-auth-key!
  [^WebDriver driver])

#_(defn test:select-account!
  [driver account])

#_(defn test:authentication-key
  ([access-type auth-url username password]
    (authentication-key access-type auth-url username password nil))
  ([^Key    access-type ^String auth-url
    ^String username    ^String password
    {:as opts :keys [account-select]}])

#_(defn test:oauth-key
  ([email scopes- access-type] (oauth-key email scopes- access-type nil))
  ([email ^Set scopes- ^Key access-type opts])

#_(defn ^String test:access-token!
  ([email scopes auth-type])
  ([email scopes ^Key auth-type {:as opts :keys [code]}])

#_(defn ^String test:access-token-refresh! [email service])

#_(defn test:access-key
  [email ^Key service ^Key token-type])

#_(defn test:handled-request! [email service opts])