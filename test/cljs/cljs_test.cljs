(ns quantum.cljs-test
  (:require [quantum.core.ns :as ns]))

(enable-console-print!)

(defn init! []
  (println "Edits to this text should show up in your developer console!! YAY :D")
  (println (doseq [a [4 3 5]] (println a))))
(init!)

; define your app data so that it doesn't get over-written on reload
(defonce app-state (atom {:text "Hello world!"}))