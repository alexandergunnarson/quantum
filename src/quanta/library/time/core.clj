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
(defn ymd [date]
  (vector
    (year  date)
    (month date)
    (day   date)))
(defn beg-of-day
  ([date]
    (apply beg-of-day (ymd date)))
  ([^long y ^long m ^long d]
    (date-time y m d 0 0 0 0)))
(defn end-of-day
  ([date]
    (apply end-of-day (ymd date)))
  ([^long y ^long m ^long d]
    (date-time y m d 23 59 59 999)))
(defn whole-day
  ([date]
    (apply whole-day (ymd date)))
  ([^long y ^long m ^long d]
    (interval (beg-of-day y m d) (end-of-day y m d))))
(defn on?
  "Determines if date is on day.
   Inclusive of intervals."
  ([date on-date]
    (apply on? date (ymd on-date)))
  ([date y m d]
    (within? (whole-day y m d)   date)))
