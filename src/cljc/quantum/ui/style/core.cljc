(ns
  ^{:doc "UI style functions"}
  quantum.ui.style.core
  #_(:require [quantum.ui.css     :as css :refer [color]]
           [garden.color :as color :refer [rgb->hsl hex->hsl rgb]]))


; (defnt solid-background
;   ([^javafx.scene.paint.Color c]
;     (-> c
;         (BackgroundFill. nil nil)
;         array
;         (Background.))) ; REFLECTION here
;   ([:else c]
;     (->> c ->color solid-background)))