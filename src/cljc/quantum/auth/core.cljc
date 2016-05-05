(ns
  ^{:doc "Auxiliary functions for authorization and key retrieval.
          Mainly interfaces with persistent storage where keys are
          stored."
    :attribution "Alex Gunnarson"}
  quantum.auth.core
  (:refer-clojure :exclude [get get-in assoc!])
  (:require [#?(:clj  clojure.core
                :cljs cljs.core   )   :as core]
            [quantum.core.io.core     :as io  ]
            [quantum.core.collections :as coll]))

; TODO: /assoc/ for file; /update/ for file; overarching syntax
; https://developer.mozilla.org/docs/Security/Weak_Signature_Algorithm
; "This site makes use of a SHA-1 Certificate; it's recommended you use certificates with signature algorithms that use hash functions stronger than SHA-1."

(def auth-source-table
  (atom {:google    "Google"
         :facebook  "Facebook"
         :fb        "Facebook"
         :snapchat  "Snapchat"
         :amazon    "Amazon"
         :amz       "Amazon"
         :intuit    "Intuit"
         :twitter   "Twitter"
         :quip      "Quip"
         :github    "GitHub"
         :financial "Financial"
         :bank      "Financial"
         :fin       "Financial"
         :plaid     "Plaid"
         :pinterest "Pinterest"}))

(defonce auths (atom {}))

(def ^:dynamic
  ^{:doc "The client should not store auths persistently; the server can."}
  *mem?*
  #?(:clj false :cljs true))

(defn get
  "Retrieves authorization keys associated with the given authorization source @auth-source (e.g. Google, Facebook, etc.)."
  {:usage (get :amazon)}
  ([auth-source]
    (if *mem?*
        (core/get @auths auth-source)
        (io/get
          {:path [:keys
                   (str (core/get @auth-source-table auth-source) ".cljd")]})))     ; CLJD = Clojure Data
  ([auth-source k]
    (core/get (get auth-source) k))) 

(defn get-in
  {:usage '(get-in :google ["alex" :password])
   :out   "__my-password__"}
  [auth-source ks]
  (core/get-in (get auth-source) ks))

(defn assoc!
  "Writes the given authorization keys to a file."
  [auth-source map-f]
  (if *mem?*
      (swap! auths assoc auth-source map-f)
      (io/assoc!
        [:keys
         (str (core/get @auth-source-table auth-source) ".cljd")]
        map-f
        {:overwrite? false})))

(defn assoc-in! [auth-source & kvs]
  (assoc! auth-source
    (apply assoc-in (get auth-source) kvs)))

(defn dissoc-in! [auth-source & ks]
  (assoc! auth-source
    (apply coll/dissoc-in+ (get auth-source) ks)))

; TODO temp change
(defn access-token
  "Retrieves the current access token for @auth-source."
  [auth-source service]
  (-> (get auth-source) (core/get service)
      :access-tokens :current :access-token))


