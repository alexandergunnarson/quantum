(ns quantum.test.browser.core
  (:require [quantum.browser.core :refer [all]]))

#?(:clj
(defn test:default-driver []))
    
#?(:clj
(defnt test:not-found-error
  [^org.openqa.selenium.WebDriver driver elem]))

#?(:clj
(defnt test:navigate!
  ([^quantum.browser.core.HeadlessBrowser x])
  ([^org.openqa.selenium.WebDriver driver-f address])))

#?(:clj
(defnt test:write-page!
  ([^org.openqa.selenium.WebDriver driver])
  ([^String page ^String page-name])))

#?(:clj
(defn test:send-keys! [^RemoteWebElement input ^String ks]))

#?(:clj
(defn test:clear-field! [^RemoteWebElement input])


#?(:clj
(defn test:wait-for-fn! [f & fn-args])

#?(:clj
(defn test:stale-elem? [^RemoteWebElement elem]))

#?(:clj
(defn test:click-load! [^RemoteWebElement elem])

#?(:clj
(defn test:click! [^RemoteWebElement elem]))

#?(:clj
(defn test:find-element
  ([driver elem])
  ([^WebDriver driver ^org.openqa.selenium.By elem times interval-ms])

#?(:clj
(defn test:find-elements
  [^WebDriver driver ^org.openqa.selenium.By elem]))

#?(:clj
(defn test:parent [^RemoteWebElement elem]))

#?(:clj
(defn test:ins [^RemoteWebElement elem]))

#?(:clj
(defn test:screenshot! [^PhantomJSDriver driver ^String file-name]))

#?(:clj
(defn test:clear! [^RemoteWebElement elem]))

#?(:clj
(defn test:record-page! [^WebDriver driver ^String page-name]))

#?(:clj
(defn test:inspect-elem [^WebElement elem]))

#?(:clj
(defn test:hover! [^WebDriver driver ^WebElement elem]))

#?(:clj
(defn test:children [^RemoteWebElement elem]))

#?(:clj
(defnt test:get-error-json [^Throwable err]))

#?(:clj
(defn test:get-cookies
  [^WebDriver driver]))


#?(:clj 
(defn test:switch-window! [^WebDriver driver handle]))

#?(:clj
(defn test:js-exec! [driver page? #_thread? s]))

#?(:clj
(defmacro test:suppress-unsafe-eval [& body])