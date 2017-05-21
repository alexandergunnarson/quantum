(ns quantum.test.browser.core
  (:require [quantum.browser.core :as ns]))

#?(:clj
(defn test:default-driver []))
    
#?(:clj
(defn test:not-found-error
  [^org.openqa.selenium.WebDriver driver elem]))

#?(:clj
(defn test:navigate!
  ([^quantum.browser.core.HeadlessBrowser x])
  ([^org.openqa.selenium.WebDriver driver-f address])))

#?(:clj
(defn test:write-page!
  ([^org.openqa.selenium.WebDriver driver])
  ([^String page ^String page-name])))

#?(:clj
(defn test:send-keys! [input ^String ks]))

#?(:clj
(defn test:clear-field! [input]))


#?(:clj
(defn test:wait-for-fn! [f & fn-args]))

#?(:clj
(defn test:stale-elem? [elem]))

#?(:clj
(defn test:click-load! [elem]))

#?(:clj
(defn test:click! [elem]))

#?(:clj
(defn test:find-element
  ([driver elem])
  ([driver ^org.openqa.selenium.By elem times interval-ms])))

#?(:clj
(defn test:find-elements
  [driver ^org.openqa.selenium.By elem]))

#?(:clj
(defn test:parent [elem]))

#?(:clj
(defn test:ins [elem]))

#?(:clj
(defn test:screenshot! [driver ^String file-name]))

#?(:clj
(defn test:clear! [elem]))

#?(:clj
(defn test:record-page! [driver ^String page-name]))

#?(:clj
(defn test:inspect-elem [elem]))

#?(:clj
(defn test:hover! [driver elem]))

#?(:clj
(defn test:children [elem]))

#?(:clj
(defn test:get-error-json [^Throwable err]))

#?(:clj
(defn test:get-cookies
  [driver]))


#?(:clj 
(defn test:switch-window! [driver handle]))

#?(:clj
(defn test:js-exec! [driver page? #_thread? s]))

#?(:clj
(defmacro test:suppress-unsafe-eval [& body]))