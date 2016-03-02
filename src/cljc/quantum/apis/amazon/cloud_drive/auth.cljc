(ns quantum.apis.amazon.cloud-drive.auth
  (:require-quantum [:core fn logic async core-async cbase #_auth] #_[:lib http web])
  (:require [quantum.net.http  :as http]
            [quantum.auth.core :as auth]
            [quantum.core.data.complex.json :refer [->json json->]]))

; (defn driver []
;   (-> res/system :quantum.web.core :web-driver))

(def redirect-uri "https://www.amazon.com/ap/oa")

(defn retrieve-authorization-code [user]
  (let [auth-ks (auth/get-in :amazon [:cloud-drive user])]
    (http/request!
      {:url redirect-uri
       :query-params
         {"client_id"     (:client-id auth-ks)
          "scope"         "clouddrive:read_all clouddrive:write"
          "response_type" "code"
          "redirect_uri"  (:redirect-uri auth-ks)}})))

; (defn login!
;   "Doesn't work for some reason.
;    Doesn't actually load a different page, given correct credentials."
;   []
;   (let [auth-ks (auth/auth-keys :amazon)
;         username-field (web/find-element (driver) (By/id "ap_email"))
;         password-field (web/find-element (driver) (By/id "ap_password"))
;         login-btn      (web/find-element (driver) (By/id "signInSubmit"))]
;     (web/send-keys! username-field (:email auth-ks))
;     (web/send-keys! password-field (:password auth-ks))
;     (web/click-load! login-btn)))

(defn initial-auth-tokens-from-code [user code]
  (let [auth-ks (auth/get-in :amazon [:cloud-drive user])]
    (http/request!
      {:url     "https://api.amazon.com/auth/o2/token"
       :method  :post
       :headers {"Content-Type" "application/x-www-form-urlencoded"}
       :form-params
         {"grant_type"    "authorization_code"
          "code"          code
          "client_id"     (:client-id     auth-ks)
          "client_secret" (:client-secret auth-ks)
          "redirect_uri"  (:redirect-uri  auth-ks)}})))

(defn refresh-token!
  "Should only be used server-side"
  [user]
  (let [auth-ks (auth/get-in :amazon [:cloud-drive #_user])] ; TODO should do user
    (#?(:clj identity
        :cljs go)
      (->> (http/request!
           {:url "https://api.amazon.com/auth/o2/token"
            :method :post
            :headers {"Content-Type" "application/x-www-form-urlencoded"}
            :form-params
              {"grant_type"    "refresh_token"
               "refresh_token" (-> auth-ks :access-tokens :offline :refresh-token)
               "client_id"     (:client-id     auth-ks)
               "client_secret" (:client-secret auth-ks)}})
           #?(:cljs <!)
           :body
           (auth/assoc-in! :amazon
             [:cloud-drive #_user :access-tokens :current])))))

