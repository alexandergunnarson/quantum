(ns
  ^{:doc "An alias of the clj-time.core namespace. Also includes
          useful functions such as |beg-of-day|, |end-of-day|,
          |on?|, |for-days-between|, etc."
    :attribution "Alex Gunnarson"}
  quantum.core.time.core
  (:refer-clojure :exclude [extend second - + > <])
  (:require-quantum [ns red macros type])
  (:require
    #?(:clj  [clj-time.core      :as time]
       :cljs [cljs-time.core     :as time])
    #?(:clj  [clj-time.periodic  :as periodic]
       :cljs [cljs-time.periodic :as periodic])
    #?(:clj  [clj-time.coerce    :as coerce]
       :cljs [cljs-time.coerce   :as coerce]))
  #?(:clj (:import java.util.Date
            (java.time Period Instant LocalDateTime))))

(defn now       [] (Instant/now))
(defn now-local [] (LocalDateTime/now))

(defn now-formatted [date-format]
  (.format (java.time.format.DateTimeFormatter/ofPattern date-format) (now-local)))

(defn str-now [] (now-formatted "MM-dd-yyyy HH:mm::ss"))
(def timestamp str-now)

(defn ymd [date]
  (vector
    (time/year  date)
    (time/month date)
    (time/day   date)))

(defn beg-of-day
  ([date]
    (apply beg-of-day (ymd date)))
  (#?(:clj  [^long y ^long m ^long d]
      :cljs [y m d])
    (time/date-time y m d 0 0 0 0)))

(defn end-of-day
  ([date]
    (apply end-of-day (ymd date)))
  ([^long y ^long m ^long d]
    (time/date-time y m d 23 59 59 999)))

(defn whole-day
  ([date]
    (apply whole-day (ymd date)))
  ([^long y ^long m ^long d]
    (time/interval (beg-of-day y m d) (end-of-day y m d))))

(defn on?
  "Determines if date is on day.
   Inclusive of intervals."
  ([date on-date]
    (apply on? date (ymd on-date)))
  ([date y m d]
    (time/within? (whole-day y m d)   date)))

#?(:clj
  (defmethod print-dup java.util.Date
    ^{:attribution "clojuredocs.org, |print-dup|"}
    [o w]
    (print-ctor o (fn [o w] (print-dup (.getTime  o) w)) w)) )

#?(:clj
  (defmethod print-dup org.joda.time.DateTime
    ^{:todo ["Fix this... only prints out current date"]}
    [d stream]
    (.write stream "#=(list \"A date should go here\" ")
    (.write stream "")
    (.write stream ")"))) 

(defn ^Delay for-days-between
  [date-a date-b f]
  (let [difference-in-days
          (time/in-days (time/interval date-a date-b))
        instants-on-beg-of-days
          (periodic/periodic-seq
            date-a (time/days 1))]
  (for+ [day (take (inc difference-in-days) instants-on-beg-of-days)]
    (f day))))

#?(:clj
  (defn long-timestamp []
    (long (/ (.getTime (Date.)) 1000))))

(defnt -
  [org.joda.time.Hours] ([x] (.negated x))
  :default
    ([date1 date2]
      (time/interval date2 date1)))

(defn between [a b] (Period/between a b))

(def interval between)

(defn > [a b]
  )

(defn since [date]
  (between date (now)))