(ns quantum.apis.amazon.cloud-drive.auth
  (:require-quantum [:lib http auth web]))

(defn driver []
  (-> res/system :quantum.web.core :web-driver))

(def redirect-uri "https://www.amazon.com/ap/oa")

(defn retrieve-authorization-code []
  (let [auth-ks (-> (auth/auth-keys :amazon) :cloud-drive)]
    (http/request!
      {:url redirect-uri
       :query-params
         {"client_id" (:client-id auth-ks)
          "scope" "clouddrive:read_all clouddrive:write"
          "response_type" "code"
          "redirect_uri" (:redirect-uri auth-ks)}})))

(defn login!
  "Doesn't work for some reason.
   Doesn't actually load a different page, given correct credentials."
  []
  (let [auth-ks (auth/auth-keys :amazon)
        username-field (web/find-element (driver) (By/id "ap_email"))
        password-field (web/find-element (driver) (By/id "ap_password"))
        login-btn      (web/find-element (driver) (By/id "signInSubmit"))]
    (web/send-keys! username-field (:email auth-ks))
    (web/send-keys! password-field (:password auth-ks))
    (web/click-load! login-btn)))

(defn initial-auth-tokens-from-code [code]
  (let [auth-ks (-> (auth/auth-keys :amazon) :cloud-drive)]
    (http/request!
      {:url "https://api.amazon.com/auth/o2/token"
       :method :post
       :headers {"Content-Type" "application/x-www-form-urlencoded"}
       :form-params
         {"grant_type"    "authorization_code"
          "code"          code
          "client_id"     (:client-id     auth-ks)
          "client_secret" (:client-secret auth-ks)
          "redirect_uri"  (:redirect-uri  auth-ks)}})))

(defn refresh-token! []
  (let [auth-ks (-> (auth/auth-keys :amazon) :cloud-drive)]
    (->> (http/request!
          {:url "https://api.amazon.com/auth/o2/token"
           :method :post
           :headers {"Content-Type" "application/x-www-form-urlencoded"}
           :form-params
             {"grant_type"    "refresh_token"
              "refresh_token" (-> auth-ks :access-tokens :offline :refresh-token)
              "client_id"     (:client-id     auth-ks)
              "client_secret" (:client-secret auth-ks)}})
        :body
        (<- json/parse-string str/keywordize)
        (assoc-in (auth/auth-keys :amazon)
          [:cloud-drive :access-tokens :current])
        (auth/write-auth-keys! :amazon))))

