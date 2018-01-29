(ns quantum.untyped.core.system
  (:require
    [quantum.untyped.core.core  :as ucore]
    [quantum.untyped.core.error :as err]))

(ucore/log-this-ns)

#?(:cljs
(def ReactNative
  (err/ignore
    (if (undefined? js/window.ReactNative)
        (js/require "react-native")
        js/window.ReactNative)))) ; https://github.com/necolas/react-native-web

(def os ; TODO: make less naive
  #?(:cljs (if ReactNative
               (-> ReactNative .-Platform .-OS)
               :unknown)
     :clj  :unknown))

;;;; React specific ;;;;

#?(:cljs
(set! (-> js/console .-ignoredYellowBox)
  #js ["You are manually calling a React.PropTypes validation function for the"]))

;;;; React Native specific ;;;;

#?(:cljs (def app-registry (when ReactNative (.-AppRegistry  ReactNative))))
