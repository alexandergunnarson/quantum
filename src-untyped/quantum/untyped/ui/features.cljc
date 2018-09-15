(ns quantum.untyped.ui.features
  (:require
    [quantum.untyped.core.core     :as ucore]
    [quantum.untyped.core.logic
      :refer [whenc fn=]]
    [quantum.untyped.core.system :as usys]
    [quantum.untyped.core.vars
      #?@(:cljs [:refer [defined?]])]))

(ucore/log-this-ns)

#?(:cljs
(defn flex-test [elem flex-name]
  (-> elem .-style .-display (set! ""))
  (-> elem .-style .-display (set! flex-name))
  (-> elem .-style .-display (not= ""))))

(defn feature-test []
  #?(:clj {:chrome true} ; Because JavaFX will use Chromium via JXBrowser?
     :cljs
      (let [div (.createElement (.-document usys/global) "div")]
        (->> {:chrome  "flex"
              :safari  "-webkit-flex"
              :safari- "-webkit-box" ; (Older)
              :ie      "-ms-flexbox"}
             (map (fn [browser s] [(whenc browser (fn= :safari-) :safari) (flex-test div s)]))
             (into {})))))
