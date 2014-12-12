(ns quantum.core.time.core
  (:refer-clojure :exclude [extend second])
  (:require [quantum.core.ns :as ns :refer [alias-ns]])
  (:gen-class))
; joda-time via clj-time
(alias-ns 'clj-time.core)
(require
  '[clj-time.periodic]
  '[quantum.core.collections :refer :all])

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

(defmethod print-dup java.util.Date
  ^{:attribution "clojuredocs.org, |print-dup|"}
  [o w]
  (print-ctor o (fn [o w] (print-dup (.getTime  o) w)) w)) 
(defmethod print-dup org.joda.time.DateTime
  ^{:todo ["Fix this... only prints out current date"]}
  [d stream]
  (.write stream "#=(list \"A date should go here\" ")
  (.write stream "")
  (.write stream ")"))

(defn for-days-between ; Delay
  [date-a date-b f]
  (let [difference-in-days
          (in-days (interval date-a date-b))
        instants-on-beg-of-days
          (clj-time.periodic/periodic-seq
            date-a (days 1))]
  (for+ [day (take (inc difference-in-days) instants-on-beg-of-days)]
    (f day))))

; (defmacro loop-days-between
;   [^DateTime a ^DateTime b ^Fn f ^clojure.lang.Symbol loop-fn]
;   `(let [^Int difference-in-days#
;           (time/in-days (time/interval ~a ~b))
;         ^LSeq instants-on-beg-of-days#
;           (clj-time.periodic/periodic-seq
;             ~a (time/days 1))]
;      ((eval ~loop-fn)
;        [day# (take (inc difference-in-days#) instants-on-beg-of-days#)]
;          (~f day#))))
; (defn doseq-days-between
;   [^DateTime a ^DateTime b ^Fn f]
;   (loop-days-between a b f 'doseq))
; (defn for-days-between
;   [^DateTime a ^DateTime b ^Fn f]
;   (loop-days-between a b f 'doseq))