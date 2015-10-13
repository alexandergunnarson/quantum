(ns quantum.apis.facebook.core
  (:require-quantum [:lib]))

; http://restfb.com/
; GET /v2.3/me HTTP/1.1
; Host: graph.facebook.com

; (request! "graph.facebook.com" )

; (defn facebook-test []
;   (let [^Map my-info
;          (->> (http/request!
;                 {:url "https://graph.facebook.com/v2.3/me" ; As opposed to http://
;                  :server-port 5001
;                  :server-name "0.0.0.0"
;                  :scheme :https
;                  :method :get
;                  :oauth-token fb-oauth-token})
;               :body
;               json/decode
;               clojure.walk/keywordize-keys) ; TODO put in quantum.collections
;        ^Map my-photos
;          (->> (http/request!
;                 {:url "https://graph.facebook.com/v2.3/me/photos" ; As opposed to http://
;                  :server-port 5001
;                  :server-name "0.0.0.0"
;                  :scheme :https
;                  :method :get
;                  :oauth-token fb-oauth-token})
;               :body
;               json/decode
;               clojure.walk/keywordize-keys)] ))
