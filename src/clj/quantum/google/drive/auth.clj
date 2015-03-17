(ns quantum.google.drive.auth
  (:import
    (org.openqa.selenium.htmlunit HtmlUnitDriver)
    (org.openqa.selenium WebDriver WebElement TakesScreenshot
     StaleElementReferenceException NoSuchElementException
     OutputType Dimension)
    (org.openqa.selenium Keys By Capabilities
      By$ByClassName By$ByCssSelector By$ById By$ByLinkText
      By$ByName By$ByPartialLinkText By$ByTagName By$ByXPath)
    (org.openqa.selenium.phantomjs PhantomJSDriver PhantomJSDriverService PhantomJSDriverService$Builder )
    (org.openqa.selenium.firefox FirefoxDriver)
    (org.openqa.selenium.remote RemoteWebDriver RemoteWebElement DesiredCapabilities)
     org.openqa.selenium.chrome.ChromeDriver
     org.openqa.selenium.safari.SafariDriver)
  (:gen-class))

(require '[quantum.auth.core                :as auth])
(require '[quantum.core.ns :as ns :refer :all])
(ns/require-all *ns* :clj :lib)
(require '[quantum.web.core :as web :refer [click! find-element write-page! default-capabilities]])
(require '[quantum.google.core :as goog])

(defn approve! [^WebDriver driver]
  (let [wait-for-btn-enabled! (Thread/sleep 1500) ; For some reason the button is disabled for a little bit
        ^RemoteWebElement accept-btn
          (find-element driver (By/id "submit_approve_access"))
        approve-click! (click! accept-btn)]))

(defn ^String copy-auth-key-and-save! [^Key auth-type ^WebDriver driver]
  (let [_ (write-page! (.getPageSource driver) "Unknown Drive page")
        ^RemoteWebElement auth-key-field
          (find-element driver (By/id "code"))
        ^String auth-key (.getAttribute auth-key-field "value")]
    ; Save key
    (auth/write-auth-keys!
      :google
      (assoc (auth/auth-keys :google)
        (keyword (str "authentication-key-" (name auth-type)))
        auth-key))
    auth-key))

(defn authentication-key-google [^Key auth-type ^String auth-url]
  {:pre [(with-throw
           (or (= auth-type :online) (= auth-type :offline))
           "Authorization type invalid.")]}
  (let [^WebDriver driver (PhantomJSDriver. default-capabilities)]
    (try
      (let [^String auth-key
              (do (goog/sign-in! driver auth-url)
                  (log/pr :debug "Sign in complete.")
                  (approve! driver)
                  (log/pr :debug "Approve complete.")
                  (copy-auth-key-and-save! auth-type driver))]
        (log/pr :debug "The" (name auth-type) "authentication key is: " auth-key)
        auth-key)
      (finally (.quit driver))))) ; ends the entire session.



