(ns
  ^{:doc "Useful functions for ending up with JavaFX objects in a sane way. ."}
  quantum.ui.style
  (:require-quantum [:lib ui]))

(require '[quantum.ui.css     :as css :refer [color]])
(require '[garden.color :as color :refer [rgb->hsl hex->hsl rgb]])


(defnt ->color*
  ([^garden.color.CSSColor c]
    (Color/hsb
      (-> c :hue (/ 100))
      (-> c :saturation (/ 100))
      (-> c :lightness (/ 100)))))

(def ->color
  (memoize (fn [c] (->color* c))))

(defnt solid-background
  ([^javafx.scene.paint.Color c]
    (-> c
        (BackgroundFill. nil nil)
        array
        (Background.))) ; REFLECTION here
  ([:else c]
    (->> c ->color solid-background)))