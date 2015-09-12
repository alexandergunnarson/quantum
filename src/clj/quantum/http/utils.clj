(ns
  ^{:doc "Some useful HTTP utility functions."
    :attribution "Alex Gunnarson"}
  quantum.http.utils
  (:require-quantum [:lib http]))

(defn ip
  "Returns the caller's IP address."
  []
  (-> (http/request! {:url "http://checkip.amazonaws.com" :as :text})
      :body
      str/trim))