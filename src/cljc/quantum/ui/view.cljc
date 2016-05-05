(ns quantum.ui.view)

#?(:cljs
(defn toggle-full-screen!
  {:from "https://developer.mozilla.org/en-US/docs/Web/API/Fullscreen_API"
   :todo ["More elegant way to do this"]}
  []
  (if (and (not (.-fullscreenElement       js/document))
           (not (.-mozFullScreenElement    js/document))
           (not (.-webkitFullscreenElement js/document))
           (not (.-msFullscreenElement     js/document)))
      (cond
        (-> js/document .-documentElement .-requestFullscreen)
        (-> js/document .-documentElement .requestFullscreen )
  
        (-> js/document .-documentElement .-msRequestFullscreen)
        (-> js/document .-documentElement .msRequestFullscreen )
  
        (-> js/document .-documentElement .-mozRequestFullscreen)
        (-> js/document .-documentElement .mozRequestFullscreen )
  
        (-> js/document .-documentElement .-webkitRequestFullscreen)
        (-> js/document .-documentElement (.webkitRequestFullscreen js/Element.ALLOW_KEYBOARD_INPUT)))
      (cond
        (-> js/document (.-exitFullscreen))
        (-> js/document (.-exitFullscreen))
 
        (-> js/document (.-msExitFullscreen))
        (-> js/document (.-msExitFullscreen))
 
        (-> js/document (.-mozCancelFullScreen))
        (-> js/document (.-mozCancelFullScreen))
 
        (-> js/document (.-webkitExitFullscreen))
        (-> js/document (.-webkitExitFullscreen))))))