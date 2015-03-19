(ns
  ^{:doc "Authorization functions for Google. Sign in from
          Google homepage, from authorized site portal, 
          etc., given a username and password.

          May be expanded to other functions sometime."
    :attribution "Alex Gunnarson"}
  quantum.google.core
  (:require
    [quantum.auth.core :as auth           ]
    [quantum.core.ns   :as ns  :refer :all]
    [quantum.web.core  :as web :refer
      [click! find-element write-page! default-capabilities send-keys!]])
  (:import
    (org.openqa.selenium WebDriver WebElement TakesScreenshot
     StaleElementReferenceException NoSuchElementException
     OutputType Dimension)
    (org.openqa.selenium Keys By Capabilities
      By$ByClassName By$ByCssSelector By$ById By$ByLinkText
      By$ByName By$ByPartialLinkText By$ByTagName By$ByXPath)
    (org.openqa.selenium.phantomjs PhantomJSDriver PhantomJSDriverService PhantomJSDriverService$Builder )
    (org.openqa.selenium.remote RemoteWebDriver RemoteWebElement DesiredCapabilities))
  (:gen-class))

(ns/require-all *ns* :clj :lib)

; =============== GOOGLE: GENERAL ===============

(defn fill-login-info!
  {:todo ["Make sure fields have actually been filled in"]}
  [^RemoteWebElement email-elem ^RemoteWebElement password-elem
   ^String username ^String password]
  (send-keys! email-elem    username #_(:email    (auth/auth-keys :google)))
  (log/pr :debug "Logging in as:"  (.getAttribute email-elem    "value"))
  (send-keys! password-elem password #_(:password (auth/auth-keys :google)))
  (log/pr :debug "With password:"  (.getAttribute password-elem "value"))
  (log/pr :debug "..."))

(defn sign-in!
  ([^WebDriver driver ^String username ^String password]
    (let [email-element      (find-element driver (By/id "Email"))
          password-element   (find-element driver (By/id "Passwd"))
          signin-button      (find-element driver (By/id "signIn"))
          set-login-info!
            (fill-login-info!
              email-element password-element
              username      password)
          sign-in-click!     (click! signin-button)]))
  ([^WebDriver driver ^String auth-url ^String username ^String password]
    (.get driver auth-url)
    (sign-in! driver username password)))

(defn begin-sign-in-from-google-home-page!
  "Start to sign in from the Google search/home page."
  [^WebDriver driver]
    (let [navigate!          (.get driver "http://www.google.com")
          ^List sign-in-btns (.findElements driver (By/linkText "Sign in"))
          ^RemoteWebElement sign-in-btn
            (if (-> sign-in-btns count+ (not= 1))
                (throw+ {:msg "No one single sign in button detected."
                         :buttons sign-in-btns})
                (first sign-in-btns))
          click-btn! (click! sign-in-btn)]))

