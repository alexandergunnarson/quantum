(ns quantum.apis.pinterest.core
  (:require-quantum [:lib auth http url web]))

; SCOPES
; read_public Use GET method on a user’s Pins, boards and likes.
; write_public  Use PATCH, POST and DELETE methods on a user’s Pins and boards.
; read_relationships  Use GET method on a user’s follows and followers (on boards, users and interests).
; write_relationships Use PATCH, POST and DELETE methods on a user’s follows and followers (on boards, users and interests).

(defn oauth-code
  "Get OAuth code for a Pinterest app.

   Note: If you use the same WebDriver more than once, you get an UnsafeEval error."
  {:usage '(oauth-code (auth/datum :pinterest "my-app"))}
  [{:keys [username password] :as app-meta} & [scopes]]
  (with-resources [driver (web/default-driver)]
    (let [url (url/map->url "https://api.pinterest.com/oauth/" 
                {"response_type" "code"
                 "client_id"     (:id app-meta)
                 "state"         ""
                 "scope"         (if scopes (str/join "," scopes) "")
                 "redirect_uri"  (:redirect-uri app-meta)})
          _ (.get driver url)
          code-extractor-regex
            (re-pattern "(?<=rewritten_redirect_uri(\\s{0,40})=(\\s{0,40})\\\".{0,500}\\&code=).*(?=\\\")")
          username-field (web/find-element driver (By/xpath "//input[@name='username_or_email']"))
          password-field (web/find-element driver (By/xpath "//input[@name='password']"))
          login-button   (web/find-element driver (By/xpath "//button[@type='submit']"))]
      (web/send-keys! username-field username)
      (web/send-keys! password-field password)

      (web/suppress-unsafe-eval (web/click-load! login-button))
      
      (let [okay-button (web/find-element driver (By/xpath "//button[@type='submit']"))
            _ (web/click-load! okay-button)
            code (->> driver
                      .getPageSource
                      (re-find code-extractor-regex)
                      first)]
        code))))

(defn oauth-token
  "Gets a Twitter OAuth token using the @app-meta provided."
  [{:keys [id secret] :as app-meta} & [scopes-0]]
  (assert (number? id) #{id})
  (assert ((fn-and string? nempty?) secret) #{secret})

  (let [scopes (or scopes-0 #{"read_public" "write_public"})
        code (oauth-code app-meta scopes)
        resp (http/request!
               {:method :post
                :parse? true
                :url "https://api.pinterest.com/v1/oauth/token"
                :query-params
                  {"grant_type"    "authorization_code"
                   "client_id"     id
                   "client_secret" secret
                   "code"          code}})
        token (:access-token resp)]
    (assert ((fn-and string? nempty?) token) #{token})
    token))

(defn refresh-oauth-token! [{:keys [name] :as app-meta}]
  (let [token (oauth-token app-meta)]
    (auth/keys-assoc! :pinterest [name :access-token] token)))

grant_type  Must take the value authorization_code.
client_id Your app ID. You can get this ID from your app page.
client_secret Your app secret. You can get this from your app page.
code  The access code you received from your redirect URI.

(defn board []
  ;"id          string                    The unique string of numbers and letters that identifies the board on Pinterest."
  ;"name        string                    The name of the board."
  ;"url         string                    The link to the board."
  ;"description string                    The user-entered description of the board."
  ;"creator     map<string,string>        The first and last name, ID and profile URL of the user who created the board."
  ;"created_at  string in ISO 8601 format The date the user created the board."
  ;"counts      map<string,i32>           The board’s stats, including Pins, following, followers and collaborators."
  ;"image       map<string,image>         The user’s profile image. The response returns the image’s URL, width and height."
  (http/request!
    {:url "https://pinterest.com/v1/boards/<board>/"}))