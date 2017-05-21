(ns quantum.test.ui.components
  (:require [quantum.ui.components :as ns]))

(defn test:table
  "An HTML table component.
   Expects rows to be indexed or grouped in some way."
  [data col-getters])

#?(:cljs
(defn test:render-db
  [db]))