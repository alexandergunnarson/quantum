(ns quantum.auth.oauth
  #_(:require-quantum [:lib])
  #_(:require [quantum.auth.core :as auth]))

#_(defn client-config
  {:in ["http://localhost:5000/" :facebook :dev]}
  [^String domain & [service & service-ks]]
  (let [^String endpoint (str "/" (name service) ".callback")]
    {:client-id     (apply auth/datum service (concat service-ks (list :client-id    )))
     :client-secret (apply auth/datum service (concat service-ks (list :client-secret)))
     :callback ; TODO some of this functionality will be rendered unnecesssary
       {:domain (whenf domain   (f*n str/ends-with? "/")
                  popr)
        :path   (whenf endpoint (f*n (fn-not (MWA 2 str/starts-with?)) "/")
                  (f*n conjl "/"))}}))

#_(defn insert-client-id-and-secret
  {:todo ["Use |assocs-in+| when it's ready..."] }
  [^Map base ^String domain service-ks]
  (let [{:as service-config :keys [client-id client-secret]
         {:keys [domain path]} :callback}
          (apply client-config domain service-ks)
        ^String config-uri (io/path domain path)]
    (-> base
        (assoc-in [:authentication-uri :query :client_id    ] client-id    )
        (assoc-in [:authentication-uri :query :redirect_uri ] config-uri   )
        (assoc-in [:access-token-uri   :query :client_id    ] client-id    )
        (assoc-in [:access-token-uri   :query :client_secret] client-secret)
        (assoc-in [:access-token-uri   :query :redirect_uri ] config-uri   ))))

#_(def uri-config-bases
  {:facebook
    {:authentication-uri
       {:url   "https://www.facebook.com/dialog/oauth"
        :query  {:scope "user_status,friends_status,user_photos,friends_photos,user_location,friends_location,read_mailbox"}}
     :access-token-uri
       {:url "https://graph.facebook.com/oauth/access_token"}}
   :google
     {:authentication-uri
       {:url "https://accounts.google.com/o/oauth2/auth"
        :query {:response_type "code"
                :scope "email"}}
      :access-token-uri
        {:url "https://accounts.google.com/o/oauth2/token"
         :query {:grant_type "authorization_code"}}}})

#_(defn uri-config
  {:usage ['(config "http://localhost:5000/" :facebook :dev)
           '(config "http://localhost:5000/" :google       )]}
  [^String domain & service-ks]
  (-> uri-config-bases
      (get (first service-ks))
      (insert-client-id-and-secret domain service-ks)))