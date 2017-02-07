(ns
  ^{:doc "A very rough and terribly incomplete mini-library
          for Selenium, especially PhantomJS."
    :attribution "Alex Gunnarson"}
  quantum.browser.core
          (:require
            [com.stuartsierra.component     :as comp ]
            [quantum.core.spec              :as s
              :refer [validate spec]]
            [quantum.auth.core              :as auth]
            [quantum.core.log               :as log]
            [quantum.core.resources         :as res]
            [quantum.core.time.core         :as time ]
            [quantum.core.io                :as io   ]
            [quantum.core.convert           :as conv ]
            [quantum.core.data.complex.json :as json ]
            [quantum.core.string            :as str  ]
            [quantum.core.async             :as async]
            [quantum.core.fn
              :refer [fn-> juxtk]]
            [quantum.core.error             :as err
              :refer [try+ try-times ->ex]]
            [quantum.core.collections :as coll
              :refer [join map+ nnil?]]
            [quantum.core.macros
              :refer [defnt]])
  #?(:clj (:import
           ;(com.teamdev.jxbrowser.chromium.javafx BrowserView)
           ;(com.teamdev.jxbrowser.chromium Browser)
           (org.openqa.selenium WebDriver WebElement TakesScreenshot
            StaleElementReferenceException NoSuchElementException
            OutputType Dimension)
           org.openqa.selenium.support.ui.Select
           (org.openqa.selenium Cookie Keys By Capabilities
             By$ByClassName By$ByCssSelector By$ById By$ByLinkText
             By$ByName By$ByPartialLinkText By$ByTagName By$ByXPath)
           (org.openqa.selenium.phantomjs PhantomJSDriver PhantomJSDriverService PhantomJSDriverService$Builder )
           (org.openqa.selenium.remote RemoteWebDriver RemoteWebElement DesiredCapabilities)
           org.apache.commons.io.FileUtils)))

; INSTALLATION PROCEDURE
; sudo mvn deploy:deploy-file  -DgroupId=local -DartifactId=jxbrowser \
; -Dversion=5.4.3 -Dpackaging=jar -Dfile=./lib/jxbrowser-5.4.3.jar \
; -Durl=file:lib
; sudo mvn deploy:deploy-file  -DgroupId=local -DartifactId=jxbrowser-mac \
; -Dversion=5.4.3 -Dpackaging=jar -Dfile=./lib/jxbrowser-mac-5.4.3.jar \
; -Durl=file:lib
; sudo mvn deploy:deploy-file  -DgroupId=local -DartifactId=jxbrowser-license-dev \
; -Dversion=1.0 -Dpackaging=jar -Dfile="./lib/development.jar" \
; -Durl=file:lib
; sudo mvn deploy:deploy-file  -DgroupId=local -DartifactId=jxbrowser-license-runtime \
; -Dversion=1.0 -Dpackaging=jar -Dfile="./lib/runtime.jar" \
; -Durl=file:lib
; Copy to .m2/local
; new com.teamdev.jxbrowser.chromium.Browser();
; BrowserView browserView = new BrowserView(browser);
; Add to stackpane

(def user-agent-string "Mozilla/5.0 (Windows NT 6.3; rv:36.0) Gecko/20100101 Firefox/36.0")

; TODO: make a macro, |with-driver-launch|

#?(:clj
(def default-capabilities
  (doto (DesiredCapabilities/phantomjs)
        (.setJavascriptEnabled true)
        (.setCapability "phantomjs.page.settings.userAgent"  user-agent-string)
        (.setCapability "phantomjs.page.settings.loadImages" false)
        (.setCapability "phantomjs.cli.args"
          ; PhantomJSDriverService only logs INFO and WARNING levels.
          ; It logs too much... and we want to roll our own.
          (into-array ["--ssl-protocol=any" "--ignore-ssl-errors=yes"
                       "--webdriver-loglevel=NONE"])))))

(declare js-exec!)

#?(:clj
(defn default-driver []
  (doto (PhantomJSDriver. default-capabilities)
    (js-exec! false false
      "var page = this;
       page.onError = function(msg, trace) {
         console.error('PHANTOM PAGE ERROR INFO: ' + msg);
       };
       return 'truth';")))) ; must return something


; How do I clear the phantomjs cache on a mac?
; rm -rf ~/Library/Application\ Support/Ofi\ Labs/PhantomJS/*
#?(:clj
(defrecord HeadlessBrowser
  [web-driver]
  comp/Lifecycle
  (start [this]
    (log/pr :user "Starting headless browser...")
    (assoc this
      :web-driver (default-driver)))
  (stop [this]
    (when (:web-driver this)
      (log/pr :user "Stopping headless browser...")
      (-> this :web-driver res/cleanup!))
    (assoc this :web-driver nil))))

#?(:clj
(defnt not-found-error
  [^org.openqa.selenium.WebDriver driver elem]
  {:msg         "Selenium element not found."
   :type        :not-found
   :elem        elem
   :driver      driver
   :on-page     (when driver (.getCurrentUrl driver))
   :page-source (when driver (.getPageSource driver))}))

#?(:clj
(defnt navigate!
  ([^quantum.browser.core.HeadlessBrowser x] (-> x :web-driver navigate!))
  ([^org.openqa.selenium.WebDriver driver-f address] (.get driver-f address))))

#?(:clj
(defnt write-page!
  {:attribution "Alex Gunnarson"}
  ([^org.openqa.selenium.WebDriver driver]
    (write-page! (.getPageSource driver) (str "Page " (time/now) ".html")))
  ([^string? page ^string? page-name]
    (io/assoc!
      {:path   [:resources "Pages" page-name]
       :type   :html
       :method :print
       :data   page}))))

#?(:clj
(defn send-keys! [^RemoteWebElement input ^String ks]
  (.sendKeys input (into-array [ks]))))

#?(:clj
(defn clear-field! [^RemoteWebElement input]
  (.sendKeys input (into-array [(str (. Keys CONTROL) "a")]))
  (.sendKeys input (into-array [(. Keys DELETE)]))))


#?(:clj
(defn wait-for-fn!
  {:attribution "Alex Gunnarson"
   :todo ["Just a band-aid. Use a timeout channel for this."]}
  [f & fn-args]
  (validate (spec fn?) f) ; TODO no runtime spec
  (let [timeout        5000
        timer-interval 100] ; try every 10th of a second
    (loop [timer   0
           f-true? (apply f fn-args)]
      (cond
        f-true?
          nil
        (>= timer timeout)
          (throw (->ex :timeout
                       (str/sp "Timeout set at" (str 5000 ".") "Wait exceeded for function.")
                       f))
        :else
          (recur (+ timer timer-interval) (apply f fn-args)))))))

#?(:clj
(defn stale-elem?
  {:attribution "Alex Gunnarson"}
  [^RemoteWebElement elem]
  (try
    (.findElementsById elem "bogus_elem")
    false
    (catch StaleElementReferenceException _ (log/pr ::debug "Caught StaleElementReferenceException!") true))))

#?(:clj
(defn click-load!
  {:attribution "http://www.obeythetestinggoat.com/how-to-get-selenium-to-wait-for-page-load-after-a-click.html"}
  [^RemoteWebElement elem]
  (.click elem)
  (wait-for-fn! stale-elem? elem)))

#?(:clj
(defn click! [^RemoteWebElement elem]
  (.click elem)))

#?(:clj
(defn find-element
  ([driver elem                  ] (find-element driver elem 2 100))
  ([^WebDriver driver ^org.openqa.selenium.By elem times interval-ms]
    ; TODO create some sort of contract system to make this less repetitive
    (assert (nnil?   driver     ))
    (assert (nnil?   elem       ))
    (assert (number? times      ))
    (assert (number? interval-ms))

    (try+ (try-times times
            (try
              (.findElement driver elem)
              (catch NoSuchElementException e
                (async/sleep interval-ms)
                (throw e)))) ; throw to continue trying
      (catch [:type :max-tries-exceeded] {{:keys [last-error]} :objs :as e}
        (if (instance? NoSuchElementException last-error)
            (throw (not-found-error driver elem))
            (throw last-error)))))))

#?(:clj
(defn find-elements
  [^WebDriver driver ^org.openqa.selenium.By elem]
  (.findElements driver elem)))

#?(:clj
(defn parent [^RemoteWebElement elem]
  (.findElement elem (By/xpath ".."))))

#?(:clj
(defn ins [^RemoteWebElement elem]
  {:tag-name (.getTagName elem)
   :id       (.getId      elem)
   :enabled? (.isEnabled elem)
   :text     (.getText elem)
   :location (-> elem .getLocation str)}))

#?(:clj
(defn screenshot! [^PhantomJSDriver driver ^String file-name]
  (let [^java.io.File scrFile (.getScreenshotAs driver (. OutputType FILE))]
    (->> [:resources "Screens" (str file-name ".png")]
         conv/->file
         (FileUtils/copyFile scrFile)))))

#?(:clj (def select-all-str (Keys/chord ^"[Ljava.lang.String;" (into-array [(str (. Keys CONTROL)) "a"]))))
#?(:clj (def backspace (str (. Keys BACK_SPACE))))
#?(:clj (def kdelete   (str (. Keys DELETE))))
#?(:clj (def kenter    (str (. Keys ENTER))))

#?(:clj
(defn clear! [^RemoteWebElement elem]
  (let [text-length
          (-> elem .getText count)
        kdeletes (->> (repeat text-length kdelete) (apply str))]
    (send-keys! elem kdeletes))))

#?(:clj
(defn record-page! [^WebDriver driver ^String page-name]
  (let [^String name-dashed (-> page-name str/keywordize name)]
    (do (write-page!
          (.getPageSource driver)
          (str name-dashed ".html"))
        (screenshot! driver name-dashed))
    (log/pr :debug "Screenshot of" page-name))))

#?(:clj
(defn inspect-elem [^WebElement elem]
  (when (instance? WebElement elem)
    {:id    (.getAttribute elem "id")
     :class (.getAttribute elem "class")
     :tag   (.getTagName   elem)
     :href  (.getAttribute elem "href")
     :inner-html  (.getAttribute elem "innerHTML")})))

#?(:clj
(defn hover! [^WebDriver driver ^WebElement elem]
  (-> driver (org.openqa.selenium.interactions.Actions.)
      (.moveToElement elem)
      (.perform))))

#?(:clj
(defn children [^RemoteWebElement elem]
  (when (instance? WebElement elem)
    (.findElementsByXPath elem "child::*"))))

#?(:clj
(defnt get-error-json [^Throwable err]
  (-> err .getMessage
      json/json->
      :errorMessage)))

(defrecord QCookie [domain expires path value])

#?(:clj
(defn get-cookies
  {:todo ["Avoid reflection"]}
  [^WebDriver driver]
  (->> driver
       (.manage)
       (.getCookies)
       (map+ (juxt #(.getName   ^Cookie %)
               (juxtk :domain  #(.getDomain ^Cookie %)
                      :expires #(.getExpiry ^Cookie %)
                      :path    #(.getPath   ^Cookie %)
                      :value   #(.getValue  ^Cookie %)
                      :secure  #(.isSecure  ^Cookie %))))
       (map+ (juxt (fn-> first) (fn-> second map->QCookie)))
       join)))


#_(defn switch-window! [driver]
  (let [popup-handle
          (->> (.getWindowHandles driver)
               (remove (fn= (.getWindowHandle driver))) first)]
    (-> driver .switchTo (.window popup-handle))))

#?(:clj
(defn switch-window! [^WebDriver driver handle]
  (.window (.switchTo driver) handle)))

#?(:clj
(defn js-exec!
  {:usage '(js-exec! true
            "var id = \"ctl00_ContentPlaceHolder_tcBackTitleSearch_dgBTagent_cell0_7_lblLoan\";
             var link = document.getElementById(id);
             return link.href;\"")}
  [driver page? #_thread? s]
  (let [code (if page?
                 (str "var page = this;
                       return page.evaluate(function() {"
                       s
                       "});")
                 s)]
    (.executePhantomJS ^PhantomJSDriver driver code (object-array 0)))))

#?(:clj
(defmacro suppress-unsafe-eval [& body]
  `(try ~@body
     (catch org.openqa.selenium.WebDriverException e#
       (when-not
         (-> e# .getMessage json/decode
             (get "errorMessage")
             (containsv? "Refused to evaluate a string as JavaScript"))
         (throw e#))))))
