(ns quantum.apis.slack.core)


; (http/get "https://slack.com/api/channels.history"
;   {:with-credentials? false
;    :query-params {:token js/slackToken
;                   :channel "C0522EZ9N"
;                   :oldest (str latest)}})


; (http/post "https://slack.com/api/chat.postMessage"
;  {:with-credentials? false
;   :query-params {:token js/slackToken
;                  :channel "C0522EZ9N"
;                  :as_user true
;                  :text msg}})