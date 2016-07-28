(ns quantum.test.ui.revision
  (:require [quantum.ui.revision :refer :all]))

(defn test:commit! [states x])

(defn test:oswap! [states commit? x f & args])

(defn test:oreset! [states commit? x-0 x-f])

(defn test:coordinate-state! [states x])

(defn test:redo!
  ([states])
  ([states x])
  ([states x full?]))

(defn test:undo!
  ([states])
  ([states x])
  ([states x full?]))

(defn test:add-undo-redo! [states x])

(defn test:unsaved-changes? [states])

(defn test:revert! [states])

(defn test:append-data!
  [states source dest])