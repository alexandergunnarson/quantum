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

(defn oauth-params [{:keys [scopes access-type]}]
  (let [auth-keys (auth/auth-keys :google)
        access-type (or access-type :offline)]
    (when-not (contains? access-types access-type)
      (throw+ (Err. :access-type-invalid nil access-type)))
    (map/om ; must be in this order
      "access_type"   (name access-type) ; must be string, not keyword for some reason
      "client_id"     (:client-id    auth-keys)
      "redirect_uri"  (:redirect-uri auth-keys)
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

(defn sign-in-email-first! [driver]
  (let [next-button (find-element driver (By/id "next"))]
    (click! next-button)
    (Thread/sleep 500)))

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
    (web/screenshot! driver "screen1")
    (sign-in! driver username password)))

(defn begin-sign-in-from-google-home-page!
  "Start to sign in from the Google search/home page."
  [^WebDriver driver]
    (let [navigate!          (.get driver "http://www.google.com")
          ^List sign-in-btns (.findElements driver (By/linkText "Sign in"))
          ^RemoteWebElement sign-in-btn
            (if (-> sign-in-btns count (not= 1))
                (throw+ {:msg "No one single sign in button detected."
                         :buttons sign-in-btns})
                (first sign-in-btns))
          click-btn! (click-load! sign-in-btn)]))

; ===== OAUTH =====

(defn approve! [^WebDriver driver]
  (let [wait-for-btn-enabled! (Thread/sleep 1500) ; For some reason the button is disabled for a little bit
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
  (let [^WebDriver driver (PhantomJSDriver. default-capabilities)]
    (try
      (let [_ (sign-in! driver auth-url username password)
            _ (log/pr :debug "Sign in complete.")
            account-select-div
             (try (find-element driver (By/id "account-list"))
               (catch NoSuchElementException _ nil))
            _ (when account-select-div
                (select-account! driver (or account-select "Alexander Gunnarson"))) ; TODO FIX
            ^String auth-key
              (do (approve! driver)
                  (log/pr :debug "Approve complete.")
                  (copy-auth-key! driver))]
        (log/pr :debug "The" (name access-type) "authentication key is: " auth-key)
        auth-key)
      (finally (.quit driver)))))) ; ends the entire session.

(defn oauth-key
  "Retrieves the authorization key programmatically via a headless browser."
  ([scopes- access-type] (oauth-key scopes- access-type nil))
  ([^Set scopes- ^Key access-type opts]
    (let [auth-keys (auth/auth-keys :google)]
      (authentication-key
        access-type
        (oauth-url {:scopes scopes- :access-type access-type})
        (:username auth-keys)
        (:password auth-keys)
        opts))))

(defn ^String access-token!
  ([scopes auth-type] (access-token! scopes auth-type nil))
  ([^Key scopes ^Key auth-type {:as opts :keys [code]}]
    (let [auth-keys (auth/auth-keys :google)
          service (-> scopes first namespace keyword)
          access-token-retrieved
            (-> (http/request!
                  {:method :post
                   :url (:oauth-access-token urls)
                   :form-params
                    {:code (or code (oauth-key scopes auth-type opts))
                     "client_id"     (:client-id     auth-keys)
                     "client_secret" (:client-secret auth-keys)
                     "redirect_uri"  (:redirect-uri  auth-keys)
                     "grant_type"    "authorization_code"}})
                :body (json/parse-string str/keywordize))]
      (auth/write-auth-keys!
        :google
        (assoc-in (auth/auth-keys :google)
          [service
           :access-tokens
           auth-type]
          access-token-retrieved))
      access-token-retrieved)))
