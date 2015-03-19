(ns
  ^{:doc "Auxiliary functions for authorization and key retrieval.
          Mainly interfaces with persistent storage where keys are
          stored."
    :attribution "Alex Gunnarson"}
  quantum.auth.core
  (:require [quantum.core.ns :as ns :refer :all])
  (:gen-class))

(ns/require-all *ns* :clj :lib)

; TODO: /assoc/ for file; /update/ for file; overarching syntax

(def auth-source-table
  {:google "Google"
   :fb     "Facebook"})

(defn auth-keys
  "Retrieves authorization keys associated with the given authorization source @auth-source (e.g. Google, Facebook, etc.)."
  [^Keyword auth-source]
  (io/read
    :path [:resources "Keys"
           (str (get auth-source-table auth-source) ".cljx")]))
(defn write-auth-keys!
  "Writes the given authorization keys to a file."
  [^Keyword auth-source map-f]
  (io/write!
    :path      [:resources "Keys"
                (get auth-source-table auth-source)]
    :overwrite false
    :data      map-f))

(defn access-token
  "Retrieves the current access token for @auth-source."
  [^Keyword auth-source]
  (-> (auth-keys auth-source) :access-token-current :access-token))


