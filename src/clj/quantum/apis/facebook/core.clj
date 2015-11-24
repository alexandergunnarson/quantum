(ns quantum.apis.facebook.core
  (:require-quantum [:lib http]))

; http://restfb.com/
; GET /v2.3/me HTTP/1.1
; Host: graph.facebook.com

; User access token
#_(defn oauth-access-token []
  (http/request!
    {:url          "https://www.facebook.com/dialog/oauth"
     :query-params {"client_id" app-id
                    "redirect_uri" redirect-uri}}))

; Rate limitations
; https://developers.facebook.com/docs/marketing-api/api-rate-limiting
; Quora:
; After some testing and discussion with the Facebook platform team,
; there is no official limit I'm aware of or can find in the documentation.
; However, I've found 600 calls per 600 seconds, per token & per IP to be
; about where they stop you. I've also seen some application based rate
; limiting but don't have any numbers.
; TOS:
; If you exceed, or plan to exceed, any of the following thresholds please
; contact us as you may be subject to additional terms: (>5M MAU) or
; (>100M API calls per day) or (>50M impressions per day).

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
