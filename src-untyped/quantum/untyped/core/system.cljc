(ns quantum.untyped.core.system
  (:require
    [quantum.untyped.core.core  :as ucore]
    [quantum.untyped.core.error :as err]))

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
    (when (.-window usys/global)
      (cond
        ; Opera 8.0+ (UA detection to detect Blink/v8-powered Opera)
        (or (.-opera usys/global)
            (some-> usys/global .-navigator .-userAgent (.indexOf " OPR/") (>= 0)))
        :opera
        ;  Chrome 1+
        (.-chrome usys/global)
        :chrome
        ; Firefox 1.0+
        (.-InstallTrigger usys/global)
        :firefox
        ; At least Safari 3+: "[object HTMLElementConstructor]"
        (-> js/Object .-prototype .-toString
            (.call (.-HTMLElement usys/global))
            (.indexOf "Constructor")
            (> 0))
        :safari
        ; At least IE6
        (-> usys/global .-document .-documentMode)
        :ie
        :else :unknown)))))

#?(:cljs
(def ReactNative
  (>module "ReactNative" ["react-native" "react-native-web"]))) ; https://github.com/necolas/react-native-web

(def os ; TODO: make less naive
  #?(:cljs (if ReactNative
               (-> ReactNative .-Platform .-OS)
               :unknown)
     :clj  :unknown))

;; ----- React specific ----- ;;

#?(:cljs
(set! (-> js/console .-ignoredYellowBox)
  #js ["You are manually calling a React.PropTypes validation function for the"]))

;; ----- React Native specific ----- ;;

#?(:cljs (def app-registry (when ReactNative (.-AppRegistry  ReactNative))))
#?(:cljs (def AsyncStorage (when ReactNative (.-AsyncStorage ReactNative))))
#?(:cljs (def StatusBar    (when ReactNative (.-StatusBar    ReactNative))))

;; ----- Features ----- ;;

#?(:cljs
(def ^{:doc "Determines whether the device is 'touchable'"
       :adapted-from 'pukhalski/tap} touchable?
  (delay (or (and (.-propertyIsEnumerable usys/global)
                  (.propertyIsEnumerable  usys/global   "ontouchstart"))
             (and (-> usys/global .-document .-hasOwnProperty)
                  (or (.hasOwnProperty (.-document usys/global) "ontouchstart")
                      (.hasOwnProperty usys/global              "ontouchstart")))))))
