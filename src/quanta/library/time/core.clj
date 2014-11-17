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
(defn beg-of-day [^long y ^long m ^long d]
  (date-time y m d 0 0 0 0))
(defn end-of-day [^long y ^long m ^long d]
  (date-time y m d 23 59 59 999))
(defn on?
  "Determines if date is on day.
   Inclusive intervals."
  [date y m d]
  (within? (interval (beg-of-day y m d) (end-of-day y m d))
    date))
