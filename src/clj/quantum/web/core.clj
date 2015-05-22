(ns
  ^{:doc "A very rough and terribly incomplete mini-library
          for Selenium, especially PhantomJS."
    :attribution "Alex Gunnarson"}
  quantum.web.core
  (:require-quantum [:lib])
  (:require [quantum.auth.core :as auth])
  (:import
    (org.openqa.selenium WebDriver WebElement TakesScreenshot
     StaleElementReferenceException NoSuchElementException
     OutputType Dimension)
    (org.openqa.selenium Keys By Capabilities
      By$ByClassName By$ByCssSelector By$ById By$ByLinkText
      By$ByName By$ByPartialLinkText By$ByTagName By$ByXPath)
    (org.openqa.selenium.phantomjs PhantomJSDriver PhantomJSDriverService PhantomJSDriverService$Builder )
    (org.openqa.selenium.remote RemoteWebDriver RemoteWebElement DesiredCapabilities)))

(def user-agent-string "Mozilla/5.0 (Windows NT 6.3; rv:36.0) Gecko/20100101 Firefox/36.0")

; TODO: make a macro, |with-driver-launch|

(def default-capabilities
  (doto (DesiredCapabilities/phantomjs)
        (.setJavascriptEnabled true)
        (.setCapability
          "phantomjs.page.settings.userAgent"
          user-agent-string)))

; Possibly defprotocol?
(defn write-page!
  {:attribution "Alex Gunnarson"}
  ([^WebDriver driver]
    (write-page! (.getPageSource driver) (str "Page " (time/now) ".html")))
  ([^String page ^String page-name]
    (io/write!
      :path         [:resources "Pages" page-name]
      :type         "html"
      :write-method :print
      :data         page)))

(defn send-keys! [^RemoteWebElement input ^String ks]
  (.sendKeys input (into-array [ks])))
(defn clear-field! [^RemoteWebElement input]
  (.sendKeys input (into-array [(str (. Keys CONTROL) "a")]))
  (.sendKeys input (into-array [(. Keys DELETE)])))


(defn wait-for-fn!
{:attribution "Alex Gunnarson"
 :todo ["Just a band-aid. Use a timeout channel for this."]}
[^Fn f & fn-args]
(let [timeout        5000
      timer-interval 100] ; try every 10th of a second
  (loop [timer   0
         f-true? (apply f fn-args)]
    (cond
      f-true?
        nil
      (>= timer timeout)
        (throw+ {:type :timeout
                 :msg  (str/sp "Timeout set at" (str 5000 ".") "Wait exceeded for function.")
                 :fn   f})
      :else
        (recur (+ timer timer-interval) (apply f fn-args))))))

(defn stale-elem?
  {:attribution "Alex Gunnarson"}
  [^RemoteWebElement elem]
  (try
    (.findElementsById elem "bogus_elem")
    false
    (catch StaleElementReferenceException _ (println "Caught StaleElementReferenceException!") true)))

(defn click!
  {:attribution "http://www.obeythetestinggoat.com/how-to-get-selenium-to-wait-for-page-load-after-a-click.html"}
  [^RemoteWebElement elem]
  (.click elem)
  (let []
    (wait-for-fn! stale-elem? elem)))

(defn find-element
  {:attribution "Alex Gunnarson"}
  [^WebDriver driver ^org.openqa.selenium.By elem]
  (try
    (.findElement driver elem)
    (catch NoSuchElementException _
      (throw+ {:msg         "Selenium element not found."
               :type        :not-found
               :elem        elem
               :on-page     (.getCurrentUrl driver)
               :page-source (.getPageSource driver)}))))

;driver.close() ; to close a single browser window.
; Also, opening and quitting the browser with every authentication is inefficient...