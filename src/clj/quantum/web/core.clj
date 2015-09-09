(ns
  ^{:doc "A very rough and terribly incomplete mini-library
          for Selenium, especially PhantomJS."
    :attribution "Alex Gunnarson"}
  quantum.web.core
  (:require-quantum [:lib res])
  (:require
    [quantum.auth.core :as auth])
  (:import
    (org.openqa.selenium WebDriver WebElement TakesScreenshot
     StaleElementReferenceException NoSuchElementException
     OutputType Dimension)
    org.openqa.selenium.support.ui.Select
    (org.openqa.selenium Cookie Keys By Capabilities
      By$ByClassName By$ByCssSelector By$ById By$ByLinkText
      By$ByName By$ByPartialLinkText By$ByTagName By$ByXPath)
    (org.openqa.selenium.phantomjs PhantomJSDriver PhantomJSDriverService PhantomJSDriverService$Builder )
    (org.openqa.selenium.remote RemoteWebDriver RemoteWebElement DesiredCapabilities)
    org.apache.commons.io.FileUtils))

(def user-agent-string "Mozilla/5.0 (Windows NT 6.3; rv:36.0) Gecko/20100101 Firefox/36.0")

; TODO: make a macro, |with-driver-launch|

(def default-capabilities
  (doto (DesiredCapabilities/phantomjs)
        (.setJavascriptEnabled true)
        (.setCapability "phantomjs.page.settings.userAgent"  user-agent-string)
        (.setCapability "phantomjs.page.settings.loadImages" false)
        (.setCapability "phantomjs.cli.args"
          (into-array ["--ssl-protocol=any" "--ignore-ssl-errors=yes"
                       "--webdriver-loglevel=ERROR"])) ; possibly don't need to do into-array
        ))

(defn default-driver []
   (PhantomJSDriver. default-capabilities))

; How do I clear the phantomjs cache on a mac?
; rm -rf ~/Library/Application\ Support/Ofi\ Labs/PhantomJS/*
(res/register-component!
  (fn [component]
    (log/pr :user "Starting PhantomJS WebDriver")
    (assoc component
      :web-driver (default-driver)))
  (fn [component]
    (when (:web-driver component)
      (log/pr :user "Stopping PhantomJS WebDriver")
      (-> component :web-driver res/cleanup!))
    (assoc component :web-driver nil)))

(defn driver []
  (-> res/system :quantum.web.core :web-driver))

(defn not-found-error [^WebDriver driver elem]
  {:msg         "Selenium element not found."
   :type        :not-found
   :elem        elem
   :on-page     (.getCurrentUrl driver)
   :page-source (.getPageSource driver)})

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

(defn click-load!
  {:attribution "http://www.obeythetestinggoat.com/how-to-get-selenium-to-wait-for-page-load-after-a-click.html"}
  [^RemoteWebElement elem]
  (.click elem)
  (let []
    (wait-for-fn! stale-elem? elem)))

(defn click!
  {:attribution "http://www.obeythetestinggoat.com/how-to-get-selenium-to-wait-for-page-load-after-a-click.html"}
  [^RemoteWebElement elem]
  (.click elem))

(defn find-element
  {:attribution "Alex Gunnarson"}
  ([driver elem] (find-element driver elem 1 0))
  ([^WebDriver driver ^org.openqa.selenium.By elem times interval-ms]
    ((fn looper [n]
      (if (>= n times)
          (throw+ (not-found-error driver elem))
          (try
            (.findElement driver elem)
            (catch NoSuchElementException _
              (Thread/sleep interval-ms)
              (looper (inc n)))))) 0)))

(defn find-elements
  {:attribution "Alex Gunnarson"}
  [^WebDriver driver ^org.openqa.selenium.By elem]
  (.findElements driver elem))

(defn parent [^RemoteWebElement elem]
  (.findElement elem (By/xpath "..")))

(defn ins [^WebElement elem]
  {:tag-name (.getTagName elem)
   :id       (.getId      elem)
   :enabled? (.isEnabled elem)
   :text     (.getText elem)
   :location (-> elem .getLocation str)})

(defn screenshot! [^WebDriver driver ^String file-name]
  (let [^java.io.File scrFile (.getScreenshotAs driver (. OutputType FILE))]
    (->> [:resources "Screens" (str file-name ".png")]
         io/file
         (FileUtils/copyFile scrFile))))

(def select-all-str (Keys/chord (into-array [(str (. Keys CONTROL)) "a"])))
(def backspace (str (. Keys BACK_SPACE)))
(def kdelete   (str (. Keys DELETE)))
(def kenter    (str (. Keys ENTER)))

(defn clear! [elem]
  (let [text-length 
          (-> elem .getText count)
        kdeletes (->> (repeat text-length kdelete) (apply str))]
    (send-keys! elem kdeletes)))

(defn record-page! [^WebDriver driver ^String page-name]
  (let [^String name-dashed (-> page-name str/keywordize name)]
    (do (write-page!
          (.getPageSource driver)
          (str name-dashed ".html"))
        (screenshot! driver name-dashed))
    (log/pr :debug "Screenshot of" page-name)))

(defn inspect-elem [^WebElement elem]
  (when (instance? WebElement elem)
    {:id    (.getAttribute elem "id")
     :class (.getAttribute elem "class")
     :tag   (.getTagName   elem)
     :href  (.getAttribute elem "href")
     :inner-html  (.getAttribute elem "innerHTML")}))

(defn hover! [^WebDriver driver ^WebElement elem]
  (-> driver (org.openqa.selenium.interactions.Actions.)
      (.moveToElement elem)
      (.perform)))

(defn children [^WebElement elem]
  (when (instance? WebElement elem)
    (.findElementsByXPath elem "child::*")))

(defn get-error-json [^Throwable err]
  (-> err .getMessage
      (json/parse-string keyword)
      :errorMessage))

(defrecord QCookie [domain expires path value])

(defn get-cookies
  {:todo ["Avoid reflection"]}
  [^WebDriver driver]
  (->> driver
       (.manage)
       (.getCookies)
       (map+ (juxt #(.getName   ^Cookie %)
               (fn/juxtk :domain  #(.getDomain ^Cookie %)
                         :expires #(.getExpiry ^Cookie %)
                         :path    #(.getPath   ^Cookie %)
                         :value   #(.getValue  ^Cookie %)
                         :secure  #(.isSecure  ^Cookie %))))
       (map+ (juxt (fn-> first) (fn-> second map->QCookie)))
       redm))
