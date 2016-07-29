(ns quantum.test.parse.core
  (:require [quantum.parse.core :as ns]
            [quantum.core.fn
              :refer [firsta]]))

(defmulti test:parse firsta)

(defmethod test:parse :java-properties
  [_ text])

(defmulti test:output firsta)

(defmethod test:output :java-properties
  [_ props & [{:keys [no-quote?] :as opts}]])