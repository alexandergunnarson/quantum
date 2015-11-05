(ns
  ^{:doc "Authorization functions for Google.
          General to all Google authentication/OAuth2 processes."
    :attribution "Alex Gunnarson"}
  quantum.apis.google.auth
  (:require-quantum [:lib web auth http url])
  (:require 
    [quantum.web.core    :as web :refer
      [send-keys! click! click-load! find-element write-page! default-capabilities]]
    #_[quantum.apis.google.core :as goog]))

(def urls
  {:oauth-access-token "https://www.googleapis.com/oauth2/v3/token"
   :oauth              "https://accounts.google.com/o/oauth2/auth"
   :api-auth           "https://www.googleapis.com/auth/"})

; The keys of this atom are all of Google's services supported by the Quantum API
(defonce scopes
  (atom {:general
          {:email   (io/path (:api-auth urls) "userinfo.email")
           :profile (io/path (:api-auth urls) "userinfo.profile")}}))

(defn assert-email [auth-keys email]
  (throw-unless (in? email auth-keys) (Err. nil "Email not found in auth keys" email)))

(def access-types #{:online :offline})

(defn scopes-string [^Set scopes-]
  (reducei
    (fn [s ^Key scope-key n]
      (let [space* (when (> n 0) " ")  ; join spaces
            ns-   (-> scope-key namespace keyword)
            name- (-> scope-key name keyword)
            scope (get-in @scopes [ns- name-])]
        (when (empty? scope) (throw+ (Err. :scope-not-found nil scope-key)))
        (str s space* scope)))
    ""
    scopes-))

(defn oauth-params [{:keys [email scopes access-type]}]
  (let [auth-keys (auth/auth-keys :google)
        _ (assert-email auth-keys email)
        access-type (or access-type :offline)]
    (when-not (containsk? access-types access-type)
      (throw+ (Err. :access-type-invalid nil access-type)))
    (map/om ; must be in this order
      "access_type"   (name access-type) ; must be string, not keyword for some reason
      "client_id"     (-> auth-keys (get email) :client-id   )
      "redirect_uri"  (-> auth-keys (get email) :redirect-uri)
      "response_type" "code"
      "scope"         (scopes-string scopes))))

(defn oauth-url [opts]
  (url/map->url (:oauth urls) (oauth-params opts)))

(defn oauth-page [opts]
  (http/request!
    {:url (:oauth urls)
     :query-params (oauth-params opts)}))


; ===== LOGIN =====

(defn fill-username-field!
  [^RemoteWebElement username-elem ^String username]
  (send-keys! username-elem username)
  (log/pr :debug "Logging in as:" (.getAttribute username-elem "value")))

(defn fill-password-field!
  [^RemoteWebElement password-elem ^String password]
  (send-keys! password-elem password)
  (log/pr :debug "With password:" (.getAttribute password-elem "value")))

(defn+ ^:suspendable sign-in-email-first! [driver]
  (let [next-button (find-element driver (By/id "next"))]
    (click! next-button)
    (async/sleep 500)))

(defn sign-in!
  ([^WebDriver driver ^String username ^String password]
    (let [email-element      (find-element driver (By/id "Email"))
          _                  (fill-username-field! email-element username)
          password-element   (try+ (find-element driver (By/id "Passwd"))
                               (catch [:type :not-found] _ ; Treat as if it's an email-only login  
                                 (sign-in-email-first! driver)
                                 (find-element driver (By/id "Passwd"))))
          _                  (fill-password-field! password-element password)
          signin-button      (find-element driver (By/id "signIn"))
          sign-in-click!     (click-load! signin-button)]))
  ([^WebDriver driver ^String auth-url ^String username ^String password]
    (.get driver auth-url)
    #_(web/screenshot! driver "screen1")
    (sign-in! driver username password)))

(defn begin-sign-in-from-google-home-page!
  "Start to sign in from the Google search/home page."
  [^WebDriver driver]
    (let [navigate!          (.get driver "http://www.google.com")
          ^java.util.List sign-in-btns (.findElements driver (By/linkText "Sign in"))
          ^RemoteWebElement sign-in-btn
            (if (-> sign-in-btns count (not= 1))
                (throw+ {:msg "No one single sign in button detected."
                         :buttons sign-in-btns})
                (first sign-in-btns))
          click-btn! (click-load! sign-in-btn)]))

; ===== OAUTH =====

(defn+ ^:suspendable approve! [^WebDriver driver]
  (let [wait-for-btn-enabled! (async/sleep 1500) ; For some reason the button is disabled for a little bit
        ^RemoteWebElement accept-btn
          (find-element driver (By/id "submit_approve_access"))
        approve-click! (click-load! accept-btn)]))

(defn ^String copy-auth-key!
  "Copies the auth key from the web page and saves it to a
   predetermined file."
  {:attribution "Alex Gunnarson"}
  [^WebDriver driver]
  (let [^RemoteWebElement auth-key-field
          (find-element driver (By/id "code"))
        ^String auth-key (.getAttribute auth-key-field "value")]
    ; Save key
    auth-key))

(defn select-account!
  [driver account]
  (let [account-buttons (web/find-elements driver (By/className "account-name"))
        account-text (->> account-buttons (ffilter (fn-> (.getText) (= account))))
        _ (with-throw account-text
            (Err. nil "Account text not found for account"
              {:account account :buttons account-buttons}))]
    (click-load! (web/parent account-text)))) ; The parent is the link

(defn authentication-key
  "Retrieves the authentication key programmatically via PhantomJS."
  {:attribution "Alex Gunnarson"
   :todo ["Have option to use existing driver"
          "Programmatically determine account selection"]}
  ([access-type auth-url username password]
    (authentication-key access-type auth-url username password nil))
  ([^Key    access-type ^String auth-url
    ^String username    ^String password
    {:as opts :keys [account-select]}]
  {:pre [(with-throw
           (or (= access-type :online) (= access-type :offline))
           "Authorization type invalid.")]}
  (with-resources [^WebDriver driver (PhantomJSDriver. default-capabilities)]
    (try
      (let [_ (sign-in! driver auth-url username password)
            _ (log/pr :debug "Sign in complete.")
            account-select-div
             (try+ (find-element driver (By/id "account-list"))
               (catch [:type :not-found] _ nil))
            _ (when account-select-div
                (select-account! driver (or account-select "Alexander Gunnarson"))) ; TODO FIX
            ^String auth-key
              (do (approve! driver)
                  (log/pr :debug "Approve complete.")
                  (copy-auth-key! driver))]
        (log/pr :debug "The" (name access-type) "authentication key is: " auth-key)
        auth-key))))) ; ends the entire session.

(defn oauth-key
  "Retrieves the authorization key programmatically via a headless browser."
  ([email scopes- access-type] (oauth-key email scopes- access-type nil))
  ([email ^Set scopes- ^Key access-type opts]
    (let [auth-keys (auth/auth-keys :google)
          _ (assert-email auth-keys email)]
      (authentication-key
        access-type
        (oauth-url {:scopes scopes- :access-type access-type :email email})
        email
        (-> auth-keys (get email) :password)
        opts))))

(defn ^String access-token!
  "Only works for native ('other') apps."
  {:usage '[(access-token! "me@gmail.com" #{:contacts/read-write} :offline)
            (quantum.apis.google.auth/access-token! "worker1@socialytics.ai" #{:drive/all} :offline)]}
  ([email scopes auth-type] (access-token! email scopes auth-type nil))
  ([email scopes ^Key auth-type {:as opts :keys [code]}]
    (let [auth-keys (auth/auth-keys :google)
          _ (throw-unless (in? email auth-keys) (Err. nil "Email not found in auth keys" email))
          service (-> scopes first namespace keyword)
          access-token-retrieved
            (-> (http/request!
                  {:method :post
                   :url (:oauth-access-token urls)
                   :form-params
                    {:code (or code (oauth-key email scopes auth-type opts))
                     "client_id"     (-> auth-keys (get email) :client-id    )
                     "client_secret" (-> auth-keys (get email) :client-secret)
                     "redirect_uri"  (-> auth-keys (get email) :redirect-uri )
                     "grant_type"    "authorization_code"}})
                :body (json/parse-string str/keywordize))]
      (auth/write-auth-keys!
        :google
        (assoc-in (auth/auth-keys :google)
          [email
           service
           :access-tokens
           auth-type]
          access-token-retrieved))
      access-token-retrieved)))

(defn ^String access-token-refresh! [email service]
  (let [^Map    auth-keys (auth/auth-keys :google)
        resp (http/request! 
               {:method :post
                :url    (:oauth-access-token urls)
                :form-params
                {"client_id"     (-> auth-keys (get email) :client-id)
                 "client_secret" (-> auth-keys (get email) :client-secret)
                 "refresh_token" (-> auth-keys (get email) service :access-tokens :offline :refresh-token)
                 "grant_type"    "refresh_token"}})
        ^Map access-token-retrieved
          (-> resp :body (json/parse-string str/keywordize))]
    (auth/write-auth-keys!
      :google
      (assoc-in auth-keys [email service :access-tokens :current]
        access-token-retrieved))
  access-token-retrieved))



; (defn credential-fn
;   [token]
;   (println "TOKEN IS" token) 
;   ;;lookup token in DB or whatever to fetch appropriate :roles
;   {:identity token :roles #{::user}})

; (require
;   '[quantum.auth.core          :as auth  ]
;   '(cemerick.friend [workflows   :as workflows]
;                     [credentials :as creds]))

; ; The server only stores the hashed, bcrypted version of the passwords
; ; Just to compare
; (def users
;   {"root"
;     {:username "root"
;      :password (creds/hash-bcrypt "admin_password")
;      :roles #{::admin}
;      :services
;        {:facebook (auth/datum :facebook)
;         :google   (auth/datum :google)
;         :snapchat (auth/datum :snapchat)}}
;    "Alex"
;      {:username "alexandergunnarson"
;       :password (creds/hash-bcrypt "alexs_password123")
;       :roles #{::user}}})

(defn access-key
  {:in [:contacts :default]}
  [email ^Key service ^Key token-type]
  (-> (auth/auth-keys :google)
      (get email)
      (get service)
      :access-tokens
      (get token-type)
      :access-token))

(defn+ ^:suspendable handled-request! [email service opts]
  (http/request!
    (-> opts
        (assoc :oauth-token (access-key email service :current))
        (update
          :handlers
          (f*n mergel
            {401 (fn [req resp]
                   (log/pr ::warn "Unauthorized. Trying again...")
                   (access-token-refresh! email service)
                   (http/request! (assoc req :oauth-token (access-key email service :current))))
             403 (fn+ ^:suspendable f# [req resp]
                   (log/pr ::warn "Too many requests. Trying again...")
                   (async/sleep (+ 2000 (rand 2000)))  ; rand to stagger pauses
                   (http/request! req))
             500 (fn+ ^:suspendable f# [req resp]
                   (log/pr ::warn "Server error. Trying again...")
                   (async/sleep (+ 5000 (rand 2000))) ; a little more b/c server errors can persist...  
                   (http/request! req))})))))