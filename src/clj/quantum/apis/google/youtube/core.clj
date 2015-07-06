(ns quantum.apis.google.youtube.core
  (:require-quantum [:lib auth http url])
  (:require [quantum.apis.google.auth :as gauth]))

(def api-auth (:api-auth gauth/urls))

; https://developers.google.com/youtube/v3/guides/auth/server-side-web-apps
(assoc! gauth/scopes :youtube
  {; Manage your YouTube account.
   ; This scope requires communication with the API server to happen over an SSL connection.
   :force-ssl     (io/path api-auth "youtube.force-ssl")
   ; Manage your YouTube account.
   ; This scope is functionally identical to the youtube.force-ssl scope
   ; because the YouTube API server is only available via an HTTPS endpoint.
   ; As a result, even though this scope does not require an SSL connection,
   ;mthere is actually no other way to make an API request.
   :account       (io/path api-auth "youtube")
   ; View your YouTube account.
   :read          (io/path api-auth "youtube.readonly")
   ; Upload YouTube videos and manage your YouTube videos.
   :upload        (io/path api-auth "youtube.upload")
   ;Retrieve the auditDetails part in a channel resource.
   :channel-audit (io/path api-auth "youtubepartner-channel-audit")
   :channel-partner (io/path api-auth "youtubepartner")})


(defn list-channels []
  (-> (http/request!
        {:url "https://www.googleapis.com/youtube/v3/channels"
         :query-params {"part" "id" "mine" true}
         :oauth-token (->> (auth/auth-keys :google) :youtube :access-tokens :offline :access-token)})
      :body (json/parse-string str/keywordize)))

; (defn list-videos []
;   (-> (http/request!
;         {:url "https://www.googleapis.com/youtube/v3/search"
;          :query-params {"part" "id" "channelId" true "maxResults" 50} ; I think max may be 50
;          :oauth-token (->> (auth/auth-keys :google) :youtube :access-tokens :offline :access-token)})
;       :body (json/parse-string str/keywordize)))

