(ns quantum.apis.twitter.driver
  (:require-quantum [:lib auth http url web]))

(def home-url  "http://www.twitter.com")
(def login-url (str home-url "/login"))

(defn login! [^PhantomJSDriver driver username password]
  (.get driver login-url)
  (let [username-field
          (web/find-element driver
            (By/xpath "//input[@name='session[username_or_email]'
                               and contains(@class, 'js-username-field')]"))
        _ (web/send-keys! username-field username)
        password-field
          (web/find-element driver
            (By/xpath "//input[@name='session[password]'
                               and contains(@class, 'js-password-field')]"))
        _ (web/send-keys! password-field password)
        login-button (web/find-element driver (By/xpath "//button[@type='submit']"))]
    (web/click-load! login-button)
    (if (err/suppress
          (web/find-element driver
            (By/xpath "//p[contains(., 'The email and password do not match.')]")))
        (throw+ (Err. :login-invalid "Login for twitter failed for username" username))
        true)))
