(ns quantum.test.ui.features
  (:require [quantum.ui.features :refer :all]))

#?(:cljs
(defn test:flex-test [elem flex-name]))

#?(:cljs
(defn test:web-worker-test []))

(defn test:feature-test [])

#?(:cljs
(defn test:determine-browser []))

#?(:cljs
(defn test:touchable? [x]))

; EVENT UTILS

#?(:cljs
(defn test:attachEvent [element eventName callback]))

#?(:cljs
(defn test:createEvent [name]))

#?(:cljs
(defn test:fireFakeEvent [e eventName]))


#?(:cljs
(defn test:getRealEvent [e]))

; END EVENT UTILS

#?(:cljs
(defn test:attachDeviceEvent [eventName]))

;; ## Polyfills

#?(:cljs
(defn test:request-animation-frame [x]))