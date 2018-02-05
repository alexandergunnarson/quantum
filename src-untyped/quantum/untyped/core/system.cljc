(ns quantum.untyped.core.system
  (:require
    [quantum.untyped.core.core  :as ucore]
    [quantum.untyped.core.error :as err]))

(ucore/log-this-ns)

#?(:cljs
(def global
  ^{:adapted-from "https://www.contentful.com/blog/2017/01/17/the-global-object-in-javascript/"}
  ((js/Function "return this;"))))

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

#?(:cljs
(def ReactNative
  (>module "ReactNative" ["react-native" "react-native-web"]))) ; https://github.com/necolas/react-native-web

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
