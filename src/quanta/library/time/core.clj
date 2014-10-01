(ns quanta.library.time.core
  (:refer-clojure :exclude [extend second])
  (:require [quanta.library.ns :as ns :refer [alias-ns]])
  (:gen-class))
; joda-time via clj-time
(alias-ns 'clj-time.core)

(defn now-normal []
  (.format (. java.time.format.DateTimeFormatter ofPattern "MM-dd-yyyy") (. java.time.LocalDateTime now)))
(defn now-formatted [date-format]
  (.format (. java.time.format.DateTimeFormatter ofPattern date-format) (. java.time.LocalDateTime now)))