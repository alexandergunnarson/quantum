(ns
  ^{:doc "Namespace to initialize the JavaFX toolkit.
          Will be deprecated once aaronc/freactive starts being used."
    :attribution "Alex Gunnarson"}
  quantum.ui.init (:gen-class))

(defonce force-toolkit-init (javafx.embed.swing.JFXPanel.))