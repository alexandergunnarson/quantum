(ns quantum.apis.twitter.core
  (:require-quantum [:lib auth http url web]))

; TODO turn api calls into less repetitive ones  

(def default-username #(auth/datum :twitter :default-username))
(def default-app #(auth/datum :twitter :default-app))

(defn gen-oauth-signature
  [username app {:keys [method url query-params]} auth-params]
  (let [http-method-caps (-> method name str/upper-case)
        ; The OAuth spec says to sort lexigraphically/alphabetically.
        auth-params-sorted auth-params ; (sort-by auth-params key)
        params-string    (url/map->str (into (into (sorted-map) query-params)
                                            auth-params-sorted))
        consumer-secret (auth/datum :twitter username :apps app :api-secret  )
        oauth-secret    (auth/datum :twitter username :apps app :oauth-secret)
        signature-base-string
          (-> (str http-method-caps
                "&" (url/encode url)
                "&" (-> params-string url/encode))
              (str/replace "%5F" "_")) ; Don't replace "_" with %5F. This is for both param keys and values.
        _ (log/pr ::debug "Base string is:" signature-base-string)
        ; Note that there are some flows, such as when obtaining a request token,
        ; where the token secret is not yet known.
        ; In this case, the signing key should consist of the percent-encoded
        ; consumer secret followed by an ampersand character ‘&’.
        signing-key (str (url/encode consumer-secret) "&"
                         (url/encode oauth-secret))
        signature (->> (crypto/hash :sha-1-hmac signature-base-string {:secret signing-key})
                       (crypto/encode :base64)
                       (String.))]
    signature))

(defn request!
  ([req]
    (request! (default-username) (default-app) req))
  ([username-0 app-0 {:as request :keys [url method query-params timestamp]}]
    (let [username (or username-0 (default-username))
          app      (or app-0      (default-app     ))
          auth-params
            (sorted-map
              "oauth_consumer_key"     (auth/datum :twitter username :apps app :api-key)
              "oauth_nonce"            (rand/rand-string 32 #{:numeric :upper :lower})
              "oauth_signature_method" "HMAC-SHA1"
              "oauth_timestamp"        (-> (time/now-unix) (uconv/convert :millis :sec) core/int str)
              "oauth_token"            (auth/datum :twitter username :apps app :oauth-token)
              "oauth_version"          "1.0")
          oauth-signature (gen-oauth-signature username app request auth-params)
          auth-params-f (assoc auth-params "oauth_signature" oauth-signature)
          sb (seq-loop [kv auth-params-f
                        ret (StringBuilder. "OAuth ")]
               (.append ^StringBuilder ret (key kv))
               (.append ^StringBuilder ret "=")
               (.append ^StringBuilder ret "\"")
               (.append ^StringBuilder ret (url/encode (val kv)))
               (.append ^StringBuilder ret "\","))
          auth-str (-> sb (.deleteCharAt (lasti sb)) str)]
      (http/request!
        (-> request
            (assoc-in [:headers "Authorization"]
              auth-str))))))

(def tweets->hashtags
  (fn->> (map+ (fn-> :entities :hashtags))
         (remove+ empty?)
         (map+ (fn->> (map (fn-> :text keyword))))
         flatten+
         (into #{})))

(defn tweets [user-id & [opts username app]]
  (request! username app
    {:url    "https://api.twitter.com/1.1/statuses/user_timeline.json"
     :method :get
     :query-params
       {"user_id" user-id
        "count"   "200"}
     :parse? true}))

(defn rate-limits [& [{:keys [parse? handlers] :as opts} username app]]
  (request! username app
    {:url "https://api.twitter.com/1.1/application/rate_limit_status.json"
     :method :get
     :handlers handlers
     :parse? (whenc parse? nil? true)}))

(defn followers:list [user-id & [{:keys [cursor parse? handlers] :as opts} username app]]
  (request! username app
    {:url    "https://api.twitter.com/1.1/followers/list.json"
     :method :get
     :query-params
       {"user_id" user-id
        "count"   "200"
        "cursor"  (or cursor "-1")} ; -1 is the start cursor
     :parse? parse?
     :handlers handlers}))

(defn user:id->followees:ids [user-id & [{:keys [cursor parse? handlers] :as opts} username app]]
  (request! username app
    {:url    "https://api.twitter.com/1.1/friends/ids.json"
     :method :get
     :query-params
       {"user_id" user-id
        "count"   "5000"
        "cursor"  (or cursor "-1")} ; -1 is the start cursor
     :parse? parse?
     :handlers handlers}))

(defn user:id->followers:ids [user-id & [{:keys [cursor parse? handlers] :as opts} username app]]
  (request! username app
    {:url    "https://api.twitter.com/1.1/followers/ids.json"
     :method :get
     :query-params
       {"user_id" user-id
        "count"   "5000"
        "cursor"  (or cursor "-1")} ; -1 is the start cursor
     :parse? parse?
     :handlers handlers}))

(defn user:id->metadata [user-id & [{:keys [cursor parse? handlers] :as opts} username app]]
  (request! username app
    {:url    "https://api.twitter.com/1.1/users/show.json"
     :method :get
     :query-params
       {"user_id" user-id}
     :parse? parse?
     :handlers handlers}))

(defn user:ids->metadata [user-ids & [{:keys [cursor parse? handlers] :as opts} username app]]
  (throw-unless (-> user-ids count (<= 100))
    (Err. nil "<= 100 user-ids are allowed in a single request to |user:ids->metadata|. Found:" (count user-ids)))
  ; You are strongly encouraged to use a POST for larger requests.
  (request!
    {:url "https://api.twitter.com/1.1/users/lookup.json"
     :method :post
     :query-params {"user_id" (str/join "," user-ids)}
     :parse? parse?
     :handlers handlers}))

#_(defn tweets-by-id [tweet-ids]
  )

(defn tweets-by-user
  ([user-id] (tweets-by-user false))
  ([user-id include-user?]
    (request!
      {:url "https://api.twitter.com/1.1/statuses/user_timeline.json"
       :method :get
       :query-params {"user_id" user-id
                      "count" 200
                      "trim_user" (not include-user?)
                      "exclude_replies" true}})))

(defn tweets-and-user [user-id]
  (tweets-by-user user-id true))


; HEADLESS BROWSER AUTOMATION

(defn sign-in!
  {:in [:argunnarson]}
  [username]
  (.get (web/driver) "http://www.twitter.com/")
  (web/click! (web/find-element (By/xpath "//button[.='Log In']")))
  (let [username-field (web/find-element (By/id "signin-email"))
        password-field (web/find-element (By/id "signin-password"))
        ;login-button (web/find-element (By/xpath "//input[@type='submit' and @value='Log in']"))
        ]
    ; Sign in
    (web/send-keys! username-field (name username))
    (web/send-keys! password-field (auth/datum :twitter username :password))
    (web/send-keys! password-field "\n") ; to sign in
    (Thread/sleep 2000) ; Doesn't really check if you logged in, but oh well
    ;(web/click-load! login-button)
    ))

(defn create-app!
  {:in [:argunnarson]}
  [{:keys [username app-name description website callback-url]}]
  (sign-in! username)
  (.get (web/driver) "https://apps.twitter.com/app/new")

  (let [app-name-field     (web/find-element (By/id "edit-name"         ))
        description-field  (web/find-element (By/id "edit-description"  ))
        website-field      (web/find-element (By/id "edit-url"          ))
        callback-url-field (web/find-element (By/id "edit-callback-url" ))
        agree-checkbox     (web/find-element (By/id "edit-tos-agreement"))
        create-app-button  (web/find-element (By/id "edit-submit"       ))]
    (web/send-keys! app-name-field     app-name)
    (web/send-keys! description-field  description)
    (web/send-keys! website-field      website)
    (web/send-keys! callback-url-field callback-url)
    (web/click! agree-checkbox)
    (web/click-load! create-app-button)))





