(ns quantum.test.core.time.core
  (:require [quantum.core.time.core :as ns]))

(defn test:leap-year?
  ([y]))

(defn test:nanos-arr-index->year
  ([n]))

(defn test:year->nanos-arr-index
  ([n]))

(defn test:year->nanos [y])
(defn test:nanos->instant [n])

(defn test:unix-millis->nanos        [n])
(defn test:unix-millis->instant      [n])
(defn test:instant->nanos            [n])
(defn test:instant->unix-millis      [n])
(defn test:nanos->standard-instant   [n])
(defn test:instant->standard-instant [n])
(defn test:now-unix    [])
(defn test:now-nanos   [])
(defn test:now-instant [])
(defn test:now         [])

(defn test:nanos->year
  [n])

(defn test:->unix-millis [x])

(defn test:->duration
  ([x]))

(defn test:->instant ([x]) ([x a]))

(defn test:->jinstant [x])
(defn test:->jdate [x])
(defn test:->local-date-time ([x]) ([x a]))

(defn test:+ [a b])
(defn test:- [a b])
(defn test:* [a b])
(defn test:div [a b])

(defn test:<  ([a b]))
(defn test:>  ([a b]))
(defn test:<= ([a b]))
(defn test:>= ([a b]))

(defn test:now-formatted [date-format])

(defn test:->string [date formatting])

(defn test:long-timestamp [])

(defn test:parse [text formatter])

(defn test:system-timezone [])

(defn test:->calendar [x])
(defn test:->sql-time [x])
(defn test:->sql-date [x])
(defn test:->date [x])
(defn test:->timestamp [x])
(defn test:->timezone [x])