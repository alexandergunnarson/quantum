(ns
  ^{:doc "Authorization functions for Google.
          General to all Google authentication/OAuth2 processes."
    :attribution "alexandergunnarson"}
  quantum.apis.google.auth
  (:refer-clojure :exclude [empty?])
  (:require
    [quantum.core.spec     :as s
      :refer [validate]]
    [quantum.core.fn
      :refer [fn1 fnl]]
    [quantum.core.logic
      :refer [default]]
    [quantum.core.log      :as log
      :refer [prl]]
    [quantum.core.async
      :refer [go <!]]
    [quantum.net.http      :as http]
    [quantum.net.url       :as url ]
    [quantum.auth.core     :as auth]
    [quantum.core.data.map :as map ]
    [quantum.core.error    :as err
      :refer [>ex-info]                ]
    [quantum.core.collections :as coll
      :refer [reducei empty?]]
    [quantum.core.paths
      :refer [url-path]])
  #?(:cljs (:import goog.history.Html5History)))

(def urls
  {:oauth-access-token "https://www.googleapis.com/oauth2/v3/token"
   :oauth              "https://accounts.google.com/o/oauth2/auth"
   :api-auth           "https://www.googleapis.com/auth/"})

; The keys of this atom will be all of Google's services supported by the Quantum API
(defonce *scopes
  (atom {:general
          {:email   (url-path (:api-auth urls) "userinfo.email")
           :profile (url-path (:api-auth urls) "userinfo.profile")}}))

(swap! *scopes assoc :gmail
  {:read-only "https://www.googleapis.com/auth/gmail.readonly"})

(def access-types #{:online :offline})

(defn scopes-string [scopes-]
  (validate scopes- set?)
  (reducei
    (fn [s scope-key i]
      (validate scope-key keyword?)
      (let [space* (when (> i 0) " ") ; join spaces
            ns-   (-> scope-key namespace keyword)
            name- (-> scope-key name keyword)
            scope (get-in @*scopes [ns- name-])]
        (validate scope string?)
        (str s space* scope)))
    ""
    scopes-))

; From https://developers.google.com/identity/protocols/OAuth2UserAgent
; TODO incomplete
(defn oauth-params [{:keys [email scopes access-type]}]
  (let [auth-keys (auth/get :google)
        _ (validate email (fnl contains? auth-keys))
        _ (validate access-type (s/or* nil? access-types))
        access-type (or access-type :offline)]
    (map/om ; must be in this order
      "access_type"   (name access-type) ; must be string, not keyword for some reason
      "client_id"     (-> auth-keys (get email) :client-id   )
      "redirect_uri"  (-> auth-keys (get email) :redirect-uri)
      "response_type" "code"
      "scope"         (scopes-string scopes))))

#_(defn oauth-url [opts]
  (url/map->url (:oauth urls) (oauth-params opts)))

(defn oauth-page|request [opts]
  {:url          (:oauth urls)
   :query-params (oauth-params opts)})

(defn authenticate|request
  #_(set! js/location.hostname (let [{:keys [url query-params] :as url*} (authenticate ...)]
                                 (url/map->url url query-params)))
  {:usage `(authenticate|request
             {:email        "anemail@gmail.com"
              :client-id    "3832713he.apps.googleusercontent.com"
              :redirect-uri "https://example.com"
              :scopes       #{:gmail/read-only}})}
  [{:keys [email client-id redirect-uri scopes include-granted-scopes?]}]
  {:url          (:oauth urls)
   :query-params (map/om
                   "response_type"          "token"
                   "client_id"              client-id
                   "redirect_uri"           redirect-uri
                   "scope"                  (scopes-string scopes)
                   "login_hint"             email
                   "include_granted_scopes" (default include-granted-scopes? true))})

; ===== LOGIN =====

#_(defn fill-field!
  [^RemoteWebElement elem ^String s]
  (web/clear-field! elem)
  (send-keys! elem s))

(defn login-challenged-err [& [msg objs]]
  (>ex-info :web/login-challenged
    (or msg "Login challenged. Google asks you to 'verify it's you'.") objs))

#_(defn respond-to-challenge-question
  {:usage '(respond-to-challenge-question driver1 "MapChallengeLabel" "address" :sign-in-city)}
  [driver label-id field-id datum-key username password]
  (when (err/suppress (find-element driver (By/id label-id)))
    (logic/if-let [challenge-field (err/suppress (find-element driver (By/id field-id        )))
                   submit-button   (err/suppress (find-element driver (By/id "submitChallenge")))]
      (if-let [datum (auth/datum :google username datum-key)]
        (do (log/pr :warn "Login challenged by Google. Responding with" (str/squote datum) "...")
            (send-keys! challenge-field datum)
            (click-load! submit-button))
        (throw (login-challenged-err (str "Login challenged. Google asked you for " datum-key ". This is not found in auth keys."))))
      (throw (login-challenged-err (str/sp "Login challenged. Google asked you for" datum-key "."
                                            "Could not find one or more required HTML components to respond."))))
    true))

#_(defn handle-challenge-question [driver username password]
  (or (respond-to-challenge-question driver "MapChallengeLabel"           "address"     :sign-in-city   username password)
      (respond-to-challenge-question driver "RecoveryEmailChallengeLabel" "emailAnswer" :recovery-email username password)
      (throw (login-challenged-err))))

#_(defn sign-in!
  {:todo ["Handle 'your password was changed ___ ago' error"]}
  ([^WebDriver driver ^String username ^String password]
    ;(log/pr :auth "Username" username)
    ;(log/pr :auth "Password" password)
    (let [check-recovery     #(when (err/suppress (find-element driver (By/id "goToRecovery")))
                                (throw (>ex-info :web/login-failed "Google says: 'Sorry, something went wrong with signing in to your account. Try again later or go to recovery for help.'" nil)))
          check-challenge    #(err/suppress (find-element driver (By/id "login-challenge-heading")))
          check-unable       #(when (err/suppress (find-element driver (By/xpath "//h1[@class='redtext' and contains(., 'Sorry, we') and contains(., 'process your request right now')]")))
                                (throw (>ex-info :web/login-failed "Google says: 'Sorry, we can't process your request right now (for security reasons)'. Possible too many failed logins." nil)))
          check-email-first  (err/suppress (find-element driver (By/id "next")))
          fill-username! (fn [] (-> driver
                                    (find-element (By/id "Email"))
                                    (fill-field! username)))
          fill-password! (fn [] (-> driver
                                    (find-element (By/id "Passwd"))
                                    (fill-field! password)))
          click-signin!  (fn [] (-> driver
                                    (find-element (By/id "signIn"))
                                    (click-load!)))
          handle-challenge (fn [] (handle-challenge-question driver username password)
                                  (log/pr :warn "Filling password again after challenge...")
                                  ; TODO "Passwd" not found
                                  (fill-password!)
                                  (click-signin!))]
      (if check-email-first
          (do (fill-username!)
              (log/pr :debug "Signing in email first...")
              (click! check-email-first)
              (async/sleep 500))
          (fill-username!))

      (fill-password!)
      (click-signin!)

      (when (check-challenge)
        (handle-challenge))

      (check-recovery)
      (check-unable)))
  ([^WebDriver driver ^String auth-url ^String username ^String password]
    (.get driver auth-url)
    #_(web/screenshot! driver "screen1")
    (sign-in! driver username password)))

#_(defn begin-sign-in-from-google-home-page!
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

#_(defn approve! [^WebDriver driver]
  (let [wait-for-btn-enabled! (async/sleep 1700) ; For some reason the button is disabled for a little bit; was 1500 but didn't work
        ^RemoteWebElement accept-btn
          (try+ (find-element driver (By/id "submit_approve_access"))
            (catch [:type :not-found] e
              (throw+)))
        approve-click! (click-load! accept-btn)]))

#_(defn ^String copy-auth-key!
  "Copies the auth key from the web page and saves it to a
   predetermined file."
  {:attribution "alexandergunnarson"}
  [^WebDriver driver]
  (let [^RemoteWebElement auth-key-field
          (find-element driver (By/id "code"))
        ^String auth-key (.getAttribute auth-key-field "value")]
    ; Save key
    auth-key))

#_(defn select-account!
  [driver account]
  (let [account-buttons (web/find-elements driver (By/className "account-name"))
        account-text (->> account-buttons (ffilter (fn-> (.getText) (= account))))
        _ (with-throw account-text
            (Err. nil "Account text not found for account"
              {:account account :buttons account-buttons}))]
    (click-load! (web/parent account-text)))) ; The parent is the link

#_(defn authentication-key
  "Retrieves the authentication key programmatically via PhantomJS."
  {:attribution "alexandergunnarson"
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
              (err/suppress (find-element driver (By/id "account-list")))
            _ (when account-select-div
                (select-account! driver (or account-select "Alexander Gunnarson"))) ; TODO FIX
            ^String auth-key
              (do (approve! driver)
                  (log/pr :debug "Approve complete.")
                  (copy-auth-key! driver))]
        (log/pr :debug "The" (name access-type) "authentication key is: " auth-key)
        auth-key))))) ; ends the entire session.

#_(defn oauth-key
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

#_(defn ^String access-token!
  "Only works for native ('other') apps."
  {:usage '[(access-token! "me@gmail.com" #{:contacts/read-write} :offline)
            (quantum.apis.google.auth/access-token! "rand@gmail.com" #{:drive/all} :offline)]}
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

#_(defn ^String access-token-refresh! [email service]
  (let [auth-keys (auth/auth-keys :google)
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

#_(defn access-key
  {:in [:contacts :default]}
  [email ^Key service ^Key token-type]
  (-> (auth/auth-keys :google)
      (get email)
      (get service)
      :access-tokens
      (get token-type)
      :access-token))

#_(defn handled-request! [email service opts]
  (http/request!
    (-> opts
        (assoc :oauth-token (access-key email service :current))
        (update
          :handlers
          (fn1 mergel
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
