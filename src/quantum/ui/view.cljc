(ns quantum.ui.view
  (:require
    [quantum.core.vars
      :refer [def-]]))

#?(:cljs
(defn full-screen? []
  (or (.-fullscreenElement       js/document)
      (.-mozFullScreenElement    js/document)
      (.-webkitFullscreenElement js/document)
      (.-msFullscreenElement     js/document))))

#?(:cljs
(def- *enable-full-screen!
  (delay
    (let [de (.-documentElement js/document)]
      (or (.-requestFullscreen       de)
          (.-msRequestFullscreen     de)
          (.-mozRequestFullscreen    de)
          (.-webkitRequestFullscreen de))))))

#?(:cljs
(defn enable-full-screen! [] (@*enable-full-screen!)))

#?(:cljs
(def- *disable-full-screen!
  (delay
    (or (.-exitFullscreen       js/document)
        (.-msExitFullscreen     js/document)
        (.-mozCancelFullScreen  js/document)
        (.-webkitExitFullscreen js/document)))))

#?(:cljs (defn disable-full-screen! [] (@*disable-full-screen!)))

#?(:cljs
(defn toggle-full-screen!
  {:adapted-from "https://developer.mozilla.org/en-US/docs/Web/API/Fullscreen_API"}
  []
  (if (full-screen?)
      (disable-full-screen!)
      (enable-full-screen!))))
