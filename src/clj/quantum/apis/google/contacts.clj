(ns quantum.apis.google.contacts
  (:require-quantum [:lib http auth])
  (:require 
    [quantum.apis.google.auth :as gauth]))

(assoc! gauth/scopes :contacts
  {:read-write "https://www.google.com/m8/feeds"
   :read       "https://www.googleapis.com/auth/contacts.readonly"})