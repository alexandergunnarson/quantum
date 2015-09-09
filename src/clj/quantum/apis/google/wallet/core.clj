(ns quantum.apis.google.wallet.core
  (:require-quantum [:lib])
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
     org.openqa.selenium.safari.SafariDriver))

(require '[quantum.auth.core                :as auth])
(require '[quantum.web.core :as web :refer [click! send-keys! find-element write-page! default-capabilities]])
(require '[quantum.apis.google.core :as goog])
(require '[quantum.apis.google.auth :as gauth])

; TODO make each WebDriver within a go-block to allow multiple simultaneous ones
; TODO use |trampoline|

(defmacro try-with
  [[handler & args] & body]
  `(try+
     ~@body
     (catch Object e#
       (if (-> e# :type (= :not-found))
           (~handler ~@args)
           (throw+ {:msg "No handler" :orig-err e#})))))

(defn no-handler [err]
  (throw+ {:msg "No handler" :orig-err err}))

(declare get-balance-from-page!)

#_(defn handle-sms-user-pin [^WebDriver driver ^Key prev-state]
  (try+
    (let [pin-field      (find-element driver (By/id "smsUserPin"))
          fill-field!    (send-keys! pin-field "942790") ; TODO fix this
          verify-pin-btn (find-element driver (By/id "smsVerifyPin"))
          click-btn!     (click! verify-pin-btn)]
          (write-page!   (.getPageSource driver) (str/sp "SMS user pin page" (time/now)))
          ; wait for page load
      (get-balance-from-page! driver :pin))
    ))

#_(defn ^Num get-balance-from-page!
  [^WebDriver driver ^Key prev-state]
  (try+
    (let [^RemoteWebElement balance-elem
            (find-element driver (By/id "gwt-debug-leftNavWalletBalanceWidget-balanceSpan"))
        validate-elem
          (when (or (-> balance-elem (.getAttribute "debugid"  ) (not= "leftNavWalletBalanceWidget-balanceSpan"))
                    (-> balance-elem (.getAttribute "innerHTML") ((fn-not containsv?) "$")))
            (throw+ {:msg "Google Wallet balance element not found."}))
        ^Num balance
          (-> balance-elem (.getAttribute "innerHTML")
              rest ; remove dollar sign
              str/val)]
      (log/pr :user "==========" "Google Wallet balance is" (str balance ".") "==========")
      balance)
    (catch Object e
      (if (-> e :type (= :not-found))
          (condp = prev-state
            :init (handle-sms-user-pin driver :balance)
            :pin  (throw+ {:msg "Pin invalid"})
            (throw+ {:msg "Unknown previous state"}) )
          (no-handler e)))))

(defn ^Num get-balance!
  {:todo ["|with-sign-in|"]}
  [^String username ^String password]
  (let [^WebDriver driver (PhantomJSDriver. default-capabilities)]
    (try (write-page!  driver)
      (gauth/sign-in! driver "http://wallet.google.com/" username password)
      (write-page! driver)
      (get-balance-from-page! driver :init)
      (finally (.quit driver)))))


; Connecting a payment application to the credit card processing networks is difficult, expensive and beyond the resources
; of most businesses. Instead, you can easily connect to the Authorize.Net Payment Gateway, which provides the complex
; infrastructure and security necessary to ensure secure, fast and reliable transactions.
; ; 1996 Authorize.Net Founded
; ; $100+ Billion Annual Transacting Volume
; ; 400,000+ Merchant Customers