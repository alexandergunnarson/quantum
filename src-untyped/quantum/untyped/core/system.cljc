(ns quantum.untyped.core.system
  (:require
    [quantum.untyped.core.collections   :as ucoll]
    [quantum.untyped.core.core          :as ucore]
    [quantum.untyped.core.error         :as err]
    [quantum.untyped.core.logic
      :refer [condpc coll-or]]
    [quantum.untyped.core.string.format :as ustr|form]))

(ucore/log-this-ns)

#?(:cljs
(def global
  ^{:adapted-from "https://www.contentful.com/blog/2017/01/17/the-global-object-in-javascript/"}
  ((js/Function "return this;"))))

;; ----- Modules/dependencies ----- ;;

#?(:cljs (def dependencies (.-dependencies global)))
#?(:cljs (def js-require   (.-require      global)))

#?(:cljs
(defn >module
  "Finds a module by the following fallbacks:
   1) global var name
   2) package names in order, by lookup in global var `dependencies`
   3) package names in order, via `js/require`"
  [var-name package-names]
  (or (aget global var-name)
      (if dependencies
          (some #(aget dependencies %) package-names)
          (when js-require
            (err/ignore (some js-require package-names)))))))

;; ----- Browser / OS ----- ;;

#?(:cljs
(def
  ^{:from "http://stackoverflow.com/questions/9847580/how-to-detect-safari-chrome-ie-firefox-and-opera-browser"
    :contributors {"Alex Gunnarson" "Ported to CLJC"}}
  browser
  (delay
    (when (.-window global)
      (cond
        ; Opera 8.0+ (UA detection to detect Blink/v8-powered Opera)
        (or (.-opera global)
            (some-> global .-navigator .-userAgent (.indexOf " OPR/") (>= 0)))
        :opera
        ;  Chrome 1+
        (.-chrome global)
        :chrome
        ; Firefox 1.0+
        (.-InstallTrigger global)
        :firefox
        ; At least Safari 3+: "[object HTMLElementConstructor]"
        (-> js/Object .-prototype .-toString
            (.call (.-HTMLElement global))
            (.indexOf "Constructor")
            (> 0))
        :safari
        ; At least IE6
        (-> global .-document .-documentMode)
        :ie
        :else :unknown)))))

#?(:cljs
(def ReactNative
  (>module "ReactNative" ["react-native" "react-native-web"]))) ; https://github.com/necolas/react-native-web

(def os ; TODO: make less naive
  #?(:cljs (if ReactNative
               (-> ReactNative .-Platform .-OS)
               (condp #(ucoll/containsv? %1 %2) (.-appVersion js/navigator)
                 "Win"   :windows
                 "MacOS" :mac
                 "X11"   :unix
                 "Linux" :linux
                 :unknown))
     :clj
      (let [os-0 (some-> info :os :name ustr|form/>lower)]
        (condpc #(ucoll/containsv? %1 %2) os-0
          "win"                       :windows
          "mac"                       :mac
          (coll-or "nix" "nux" "aix") :unix
          "sunos"                     :solaris))))

;; ----- OS-specific ----- ;;

(def separator
  #?(:cljs (condp = os :windows "\\" "/") ; TODO make less naive
     :clj  (str (java.io.File/separatorChar)))) ; string because it's useful in certain functions that way

;; ----- React-specific ----- ;;

#?(:cljs
(set! (-> js/console .-ignoredYellowBox)
  #js ["You are manually calling a React.PropTypes validation function for the"]))

;; ----- React-Native-specific ----- ;;

#?(:cljs (def app-registry (when ReactNative (.-AppRegistry  ReactNative))))
#?(:cljs (def AsyncStorage (when ReactNative (.-AsyncStorage ReactNative))))
#?(:cljs (def StatusBar    (when ReactNative (.-StatusBar    ReactNative))))

;; ----- Features ----- ;;

#?(:cljs
(def ^{:doc "Determines whether the device is 'touchable'"
       :adapted-from 'pukhalski/tap} touchable?
  (delay (or (and (.-propertyIsEnumerable global)
                  (.propertyIsEnumerable  global   "ontouchstart"))
             (and (-> global .-document .-hasOwnProperty)
                  (or (.hasOwnProperty (.-document global) "ontouchstart")
                      (.hasOwnProperty global              "ontouchstart")))))))
