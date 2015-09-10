(ns quantum.apis.quip.core
  (:require-quantum [:lib http auth]))

(defn request! [req]
  (http/request!
    (assoc req
      :oauth-token (auth/datum :quip :access-token))))

(defn get-thread [id]
  (request!
    {:url (str "https://platform.quip.com/1/threads/" id)}))