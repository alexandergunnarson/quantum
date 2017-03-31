(ns quantum.apis.twitter.core
  (:refer-clojure :exclude
    [conj!, empty?])
  (:require
    [quantum.auth.core             :as auth]
    [quantum.core.collections      :as coll
      :refer [join, conj!, red-for, lasti
              map+, remove+, flatten+
              empty?]]
    [quantum.core.convert          :as conv
      :refer [json->]]
    [quantum.core.data.string
      :refer [!str]]
    [quantum.core.error            :as err
      :refer [->ex]]
    [quantum.core.fn
      :refer [fn-> fn->> fn1]]
    [quantum.core.nondeterministic :as rand]
    [quantum.core.string           :as str]
    [quantum.core.time.core        :as time]
    [quantum.measure.convert       :as uconv]
    [quantum.net.http              :as http]
    [quantum.net.url               :as url]
    [quantum.security.cryptography :as crypto]))

; TODO turn api calls into less repetitive ones

#_(def default-username #(auth/get-in [:twitter :default-username]))
#_(def default-app      #(auth/datum :twitter :default-app     ))

(defn gen-oauth-signature
  [{:as auth :keys [consumer-secret access-token-secret]} {:keys [method url query-params]} auth-params]
  (let [http-method-caps (-> method name str/->upper)
        ; The OAuth spec says to sort lexigraphically/alphabetically.
        auth-params-sorted auth-params ; (sort-by auth-params key)
        params-string    (url/map->str (join (join (sorted-map) query-params)
                                             auth-params-sorted))
        signature-base-string
          (-> (str http-method-caps
                "&" (url/encode url)
                "&" (-> params-string url/encode))
              (str/replace "%5F" "_")) ; Don't replace "_" with %5F. This is for both param keys and values.
        ; Note that there are some flows, such as when obtaining a request token,
        ; where the token secret is not yet known.
        ; In this case, the signing key should consist of the percent-encoded
        ; consumer secret followed by an ampersand character '&'.
        signing-key (str (url/encode consumer-secret) "&"
                         (url/encode access-token-secret))
        signature (->> (crypto/hash :sha-1-hmac signature-base-string {:secret signing-key})
                       ^"[B" (crypto/encode :base64)
                       (String.))]
    signature))

(defn request!
  ([{:as auth :keys [consumer-key access-token]} {:as request :keys [url method query-params timestamp parse?]}]
    (let [_ (assert (string? consumer-key))
          _ (assert (string? access-token))
          auth-params
            (sorted-map
              "oauth_consumer_key"     consumer-key
              "oauth_nonce"            (rand/string 32 #{:numeric :upper :lower})
              "oauth_signature_method" "HMAC-SHA1"
              "oauth_timestamp"        (-> (time/now:epoch-millis) (uconv/convert :millis :sec) int str)
              "oauth_token"            access-token
              "oauth_version"          "1.0")
          oauth-signature (gen-oauth-signature auth request auth-params)
          auth-params-f (assoc auth-params "oauth_signature" oauth-signature)
          sb (red-for [[k v]  auth-params-f
                       ret (!str "OAuth ")]
               (conj! ret k)
               (conj! ret "=")
               (conj! ret "\"")
               (conj! ret (url/encode v))
               (conj! ret "\","))
          auth-str (-> ^StringBuilder sb (.deleteCharAt (lasti sb)) str) ; TODO better way of doing this
          resp (http/request!
                 (-> request
                     (assoc-in [:headers "Authorization"]
                       auth-str)))]
      (if parse?
          (-> resp :body json->)
          resp))))

(def tweets->hashtags
  (fn->> (map+    (fn-> :entities :hashtags))
         (remove+ (fn1 empty?))
         (map+    (fn->> (map (fn-> :text keyword))))
         flatten+
         (into #{})))

(defn tweets [user-id auth & [{:keys [cursor parse? handlers keys-fn] :as opts}]]
  (request! auth
    {:url    "https://api.twitter.com/1.1/statuses/user_timeline.json"
     :method :get
     :query-params
       {"user_id" user-id
        "count"   "200"
        "cursor"  (or cursor "-1")} ; -1 is the start cursor
     :handlers handlers
     :parse?   parse?
     :keys-fn  keys-fn}))

(defn rate-limits [auth & [{:keys [parse? handlers keys-fn] :as opts}]]
  (request! auth
    {:url      "https://api.twitter.com/1.1/application/rate_limit_status.json"
     :method   :get
     :handlers handlers
     :parse?   parse?
     :keys-fn  keys-fn}))

(defn followers:list [user-id auth & [{:keys [cursor parse? handlers keys-fn] :as opts}]]
  (request! auth
    {:url      "https://api.twitter.com/1.1/followers/list.json"
     :method   :get
     :query-params
       {"user_id" user-id
        "count"   "200"
        "cursor"  (or cursor "-1")} ; -1 is the start cursor
     :parse?   parse?
     :keys-fn  keys-fn
     :handlers handlers}))

(defn user:id->followees:ids [user-id auth & [{:keys [cursor parse? handlers keys-fn] :as opts}]]
  ; Very strangely, I get a 401 error
  (request! auth
    {:url      "https://api.twitter.com/1.1/friends/ids.json"
     :method   :get
     :query-params
       {"user_id" user-id
        "count"   "5000"
        "cursor"  (or cursor "-1")} ; -1 is the start cursor
     :parse?   parse?
     :keys-fn  keys-fn
     :handlers handlers}))

(defn user:id->followers:ids [user-id auth & [{:keys [cursor parse? handlers keys-fn] :as opts}]]
  (request! auth
    {:url      "https://api.twitter.com/1.1/followers/ids.json"
     :method   :get
     :query-params
       {"user_id" user-id
        "count"   "5000"
        "cursor"  (or cursor "-1")} ; -1 is the start cursor
     :parse?   parse?
     :handlers handlers}))

(defn user:id->metadata [user-id auth & [{:keys [cursor parse? handlers keys-fn] :as opts}]]
  (request! auth
    {:url      "https://api.twitter.com/1.1/users/show.json"
     :method   :get
     :query-params
       {"user_id" user-id}
     :parse?   parse?
     :keys-fn  keys-fn
     :handlers handlers}))

(defn user:ids->metadata [user-ids auth & [{:keys [cursor parse? handlers keys-fn] :as opts}]]
  (when-not (-> user-ids count (<= 100))
    (throw (->ex "> 100 user-ids are allowed in a single request to |user:ids->metadata|. Found:" (count user-ids))))
  ; You are strongly encouraged to use a POST for larger requests.
  (request! auth
    {:url      "https://api.twitter.com/1.1/users/lookup.json"
     :method   :post
     :query-params {"user_id" (str/join "," user-ids)}
     :parse?   parse?
     :keys-fn  keys-fn
     :handlers handlers}))

(defn post-status! [status auth & [{:keys [parse? handlers keys-fn] :as opts}]]
  (assert (string? status) #{status})
  (request! auth
    ; TODO additional params are possible
    {:url          "https://api.twitter.com/1.1/statuses/update.json"
     :method       :post
     :query-params {"status" status}}))

(defn tweets-by-user
  ([user-id auth & [{:keys [parse? handlers keys-fn include-user?] :as opts}]]
    (request! auth
      {:url          "https://api.twitter.com/1.1/statuses/user_timeline.json"
       :method       :get
       :parse?       parse?
       :keys-fn      keys-fn
       :handlers     handlers
       :query-params {"user_id" user-id
                      "count" 200
                      "trim_user" (str (not include-user?))
                      "exclude_replies" "true"}})))

; HEADLESS BROWSER AUTOMATION

#_(defn+ ^:suspendable sign-in!
  {:in [:argunnarson]}
  [username]
  (.get ^WebDriver (web/driver) "http://www.twitter.com/")
  (web/click! (web/find-element (By/xpath "//button[.='Log In']")))
  (let [username-field (web/find-element (By/id "signin-email"))
        password-field (web/find-element (By/id "signin-password"))
        ;login-button (web/find-element (By/xpath "//input[@type='submit' and @value='Log in']"))
        ]
    ; Sign in
    (web/send-keys! username-field (name username))
    (web/send-keys! password-field (auth/datum :twitter username :password))
    (web/send-keys! password-field "\n") ; to sign in
    (async/sleep 2000) ; Doesn't really check if you logged in, but oh well
    ;(web/click-load! login-button)
    ))

#_(defn create-app!
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

