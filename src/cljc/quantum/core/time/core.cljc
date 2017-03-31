(ns
  ^{:doc "A wrapper of the Java 8 Time package on CLJ, and JSJoda on CLJS.

          Optimally we would measure time in Planck quanta since the Big Bang,
          but practicality does not permit this."
    :attribution "alexandergunnarson"}
  quantum.core.time.core
  (:refer-clojure :exclude
    [extend second - + < <= > >= format])
  (:require
    [clojure.core                   :as core]
#?@(:cljs
   [[cljsjs.js-joda]
    [cljsjs.js-joda-timezone]]) ; For IANA timezone support
    [quantum.core.error             :as err
      :refer [->ex TODO throw-unless]]
    [quantum.core.fn                :as fn
      :refer [fn1 <-]]
    [quantum.core.logic             :as logic
      :refer [fn-or whenc]]
    [quantum.core.macros            :as macros
      :refer [defnt case-env]]
    [quantum.core.numeric           :as num]
    [quantum.core.collections       :as coll
      :refer [ifor]]
    [quantum.measure.convert        :as uconv
      :refer [convert]]
    [quantum.core.convert.primitive :as pconv
      :refer [->int ->long]]
    [quantum.core.vars              :as var
      :refer [defalias]])
  (:require-macros
    [quantum.core.time.core         :as self
      :refer [->local-date]])
#?(:clj
  (:import
    [java.util Date Calendar]
    [java.util.concurrent TimeUnit]
    [java.time Month LocalDate LocalTime LocalDateTime ZonedDateTime ZoneId ZoneOffset
               Period Duration]
    [java.time.format DateTimeFormatter]
    [java.time.temporal Temporal TemporalAccessor ChronoField])))

; ===== ABOUT =====
; https://www.npmjs.com/package/js-joda
; js-joda is fast. It is about 2 to 10 times faster than other javascript date libraries.
; js-joda is robust and stable.
; =================

(def ^{:todos {0 {:desc     "Need to make JSJodaTimezone dep on cljsjs"
                  :priority 0.5}
               1 "Go through rest of JSJoda"}}
  annotations nil)

#?(:cljs
(extend-type js/JSJoda.LocalDate
  IEquiv      (-equiv   [this x] (.equals    this x))
  IHash       (-hash    [this  ] (.hashCode  this  ))
  IComparable (-compare [this x] (.compareTo this x))))

#?(:cljs
(extend-type js/JSJoda.LocalTime
  IEquiv      (-equiv   [this x] (.equals    this x))
  IHash       (-hash    [this  ] (.hashCode  this  ))
  IComparable (-compare [this x] (.compareTo this x))))

#?(:cljs
(extend-type js/JSJoda.LocalDateTime
  IEquiv      (-equiv   [this x] (.equals    this x))
  IHash       (-hash    [this  ] (.hashCode  this  ))
  IComparable (-compare [this x] (.compareTo this x))))

#_(extend-protocol IPrintWithWriter
  goog.date.UtcDateTime
  (-pr-writer [obj writer opts]
    (-write writer "#inst ")
    (pr-writer (unparse (:date-time formatters) obj) writer opts))

  goog.date.DateTime
  (-pr-writer [obj writer opts]
    (-write writer "#inst ")
    (pr-writer (unparse (:date-time formatters) obj) writer opts))

  goog.date.Date
  (-pr-writer [obj writer opts]
    (-write writer "#inst ")
    (pr-writer (unparse (:date formatters) obj) writer opts)))



#?(:clj (defn now:epoch-millis [] (System/currentTimeMillis)))

; #?(:clj (defn gmt-now   [] (OffsetDateTime/now (ZoneId/of "GMT"))))
; #?(:clj (defn now-local [] (LocalDateTime/now)))

(defnt ^long ->epoch-millis
  #?@(:clj  [([^java.time.Instant       x] (-> x (.toEpochMilli)))
             ([^java.util.Date          x] (-> x (.getTime)     ))
             ([^java.time.LocalDate     x] (-> x (.toEpochDay ) (convert :days  :millis) ->long))
             ([^java.time.LocalDateTime x] (-> x (.toInstant ZoneOffset/UTC) ->epoch-millis))
             ([^java.time.ZonedDateTime x] (-> x .toInstant ->epoch-millis))
             ([^org.joda.time.DateTime  x] (-> x (.getMillis)   ))
             ([^java.util.Calendar      x] (-> x (.getTimeInMillis)))]
      :cljs [([^number?                 x] (->long x))
             ([^js/Date                 x] (.getTime x))]))

(declare ->local-date-time-protocol)

#?(:clj
(defnt ^java.time.Instant ->instant
  "Coerces to an instantaneous point on an imaginary timeline."
  ([#{long? bigint?} x] (-> x ->long (java.time.Instant/ofEpochMilli)))
  ([x] (-> x ->epoch-millis-protocol ->instant))))

; ===== DATE ===== ;

(defnt ^{:tag #?(:clj LocalDate :cljs js/JSJoda.LocalDate)} ->local-date*
  "Coerces to a date without a time-zone in the ISO-8601 calendar system, such as 2007-12-03."
  (^{:doc "Obtain the current date in the given timezone, e.g. 2016-02-23"}
   [#?(:clj ^ZoneId x :cljs ^js/JSJoda.ZoneOffset x)]
                    (#?(:clj LocalDate/now   :cljs js/JSJoda.LocalDate.now  ) x))
  (^{:doc "Obtain an instance of LocalDate from an ISO8601 formatted text string"}
   [^string? x    ] (#?(:clj LocalDate/parse :cljs js/JSJoda.LocalDate.parse) x))
  (^{:doc "Obtain an instance of LocalDate from a year, month, and dayOfMonth value"}
   [#?(:cljs ^number? y
       :clj           y) m d]
    (#?(:clj LocalDate/of :cljs js/JSJoda.LocalDate.of) (->long y) (->long m) (->long d))))

#?(:clj
(defmacro ->local-date
  "Coerces to a date without a time-zone in the ISO-8601 calendar system, such as 2007-12-03."
  (^{:doc "Obtain the current date in the system default timezone"}
   [] (case-env :clj `(LocalDate/now) :cljs `(js/JSJoda.LocalDate.now)))
  ([x & args] `(->local-date* ~x ~@args))))


(defnt ^{:tag #?(:clj LocalTime :cljs js/JSJoda.LocalTime)} ->local-time*
  "Coerces to a time without a time-zone in the ISO-8601 calendar system, such as ‘10:15:30’."
  (^{:doc "Obtain the current time in the given timezone, e.g. 2016-02-23"}
   [#?(:clj ^ZoneId x :cljs ^js/JSJoda.ZoneOffset x)]
                    (#?(:clj LocalTime/now   :cljs js/JSJoda.LocalTime.now  ) x))
  (^{:doc "Obtain an instance of LocalTime from an ISO8601 formatted text string"}
   [^string? x    ] (#?(:clj LocalTime/parse :cljs js/JSJoda.LocalTime.parse) x))
  #?(:cljs ([^js/Date x] (-> x js/JSJoda.nativeJs js/JSJoda.LocalTime.from)))
  (^{:doc "Obtain an instance of LocalTime from an hour and minute value"}
   [#?(:cljs ^number? h
       :clj           h) m]
    (#?(:clj LocalTime/of :cljs js/JSJoda.LocalTime.of) (->long h) (->long m)))
  (^{:doc "Obtain an instance of LocalTime from an hour, minute, and second value"}
   [#?(:cljs ^number? h
       :clj           h) m s]
    (#?(:clj LocalTime/of :cljs js/JSJoda.LocalTime.of) (->long h) (->long m) (->long s)))
  (^{:doc "Obtain an instance of LocalTime from an hour, minute, second, and nano value"}
   [#?(:cljs ^number? h
       :clj           h) m s n]
    (#?(:clj LocalTime/of :cljs js/JSJoda.LocalTime.of) (->long h) (->long m) (->long s) (->long n))))

#?(:clj
(defmacro ->local-time
  "Coerces to a time without a time-zone in the ISO-8601 calendar system, such as ‘10:15:30’."
  ^{:doc "Obtain the current time in the system default timezone"}
  ([] (case-env :clj `(LocalTime/now) :cljs `(js/JSJoda.LocalTime.now)))
  ([x & args] `(->local-time* ~x ~@args))))

(defnt ^{:tag #?(:clj LocalDateTime :cljs js/JSJoda.LocalDateTime)} ->local-date-time*
  "Coerces to a date and time without a time-zone in the ISO-8601 calendar system, such as ‘2007-12-03T10:15:30’."
  (^{:doc "Obtain the current date and time in the given timezone, e.g. 2007-12-03T10:15:30"}
   [#?(:clj ^ZoneId x :cljs ^js/JSJoda.ZoneOffset x)]
                    (#?(:clj LocalDateTime/now   :cljs js/JSJoda.LocalDateTime.now  ) x))
  (^{:doc "Obtain an instance of LocalDateTime from an ISO8601 formatted text string"}
   [^string? x    ] (#?(:clj LocalDateTime/parse :cljs js/JSJoda.LocalDateTime.parse) x))
  #?(:clj  ([^string? s k]
             (let [^String pattern
                    (case k
                      :http "EEE, dd MMM yyyy HH:mm:ss zzz"
                      k)]
               (-> (java.time.format.DateTimeFormatter/ofPattern pattern)
                   (.parse s)
                   (LocalDateTime/from)))))
  #?(:cljs ([^js/Date x] (-> x js/JSJoda.nativeJs js/JSJoda.LocalDateTime.from)))
  (^{:doc "Obtain an instance of LocalDateTime from a year, month, day, hour, and minute value"}
   [#?(:cljs ^number? y
       :clj           y) mo d h m]
    (#?(:clj LocalDateTime/of :cljs js/JSJoda.LocalDateTime.of)
      (->long y) (->long mo) (->long d) (->long h) (->long m)))
  (^{:doc "Obtain an instance of LocalDateTime from a year, month, day, hour, minute, and second value"}
   [#?(:cljs ^number? y
       :clj           y) mo d h m s]
    (#?(:clj LocalDateTime/of :cljs js/JSJoda.LocalDateTime.of)
      (->long y) (->long mo) (->long d) (->long h) (->long m) (->long s)))
  (^{:doc "Obtain an instance of LocalDateTime from a year, month, day, hour, minute, second, and nano value"}
   [#?(:cljs ^number? y
       :clj           y) mo d h m s n]
    (#?(:clj LocalDateTime/of :cljs js/JSJoda.LocalDateTime.of)
      (->long y) (->long mo) (->long d) (->long h) (->long m) (->long s) (->long n) )))

#?(:clj
(defmacro ->local-date-time
  "Coerces to a date and time without a time-zone in the ISO-8601 calendar system, such as ‘2007-12-03T10:15:30.’."
  ^{:doc "Obtain the current date and time in the system default timezone"}
  ([] (case-env :clj `(LocalDateTime/now) :cljs `(js/JSJoda.LocalDateTime.now)))
  ([x & args] `(->local-date-time* ~x ~@args))))

(defonce min-local-date-time
  (quantum.core.time.core/->local-date-time -999999999 1 1 0 0))

(defnt ^{:tag #?(:clj ZonedDateTime :cljs js/JSJoda.ZonedDateTime)} ->zoned-date-time*
  "Coerces to a date and time with a time-zone in the ISO-8601 calendar system, such as ‘2007-12-03T10:15:30+01:00’."
  (^{:doc "Obtain the current date and time in the given timezone, e.g. 2007-12-03T10:15:30"}
   [#?(:clj ^ZoneId x :cljs ^js/JSJoda.ZoneOffset x)]
                    (#?(:clj ZonedDateTime/now   :cljs js/JSJoda.ZonedDateTime.now  ) x))
  (^{:doc "Obtain an instance of ZonedDateTime from an ISO8601 formatted text string"}
   [^string? x    ] (#?(:clj ZonedDateTime/parse :cljs js/JSJoda.ZonedDateTime.parse) x))
  ; TODO CLJS
#?(:clj ([^string? x ^DateTimeFormatter formatter] (ZonedDateTime/parse x formatter)))
  #?(:cljs ([^js/Date x] (-> x js/JSJoda.nativeJs js/JSJoda.ZonedDateTime.from)))
  (^{:doc "Obtain an instance of ZonedDateTime from a year, month, day, hour, and minute value"}
   [#?(:cljs ^js/JSJoda.ZonedDateTime t
       :clj  ^LocalDateTime           t) zone]
  (#?(:clj ZonedDateTime/of :cljs js/JSJoda.ZonedDateTime.of)
      t zone))
  (^{:doc "Obtain an instance of ZonedDateTime from a year, month, day, hour, minute, second, and nano value"}
   [#?(:cljs ^number? y
       :clj           y) mo d h m s n zone]
    (#?(:clj ZonedDateTime/of :cljs js/JSJoda.ZonedDateTime.of)
      (->long y) (->long mo) (->long d) (->long h) (->long m) (->long s) (->long n) zone)))

#?(:clj
(defmacro ->zoned-date-time
  "Coerces to a date and time with a time-zone in the ISO-8601 calendar system, such as ‘2007-12-03T10:15:30+01:00.’."
  ^{:doc "Obtain the current date and time in the system default timezone"}
  ([] (case-env :clj `(ZonedDateTime/now) :cljs `(js/JSJoda.ZonedDateTime.now)))
  ([x & args] `(->zoned-date-time* ~x ~@args))))

(defnt zoned-date-time?
  ([#{#?(:clj ZonedDateTime :cljs js/JSJoda.ZonedDateTime)} x] true) ([^default x] false))

; ===== DURATION ===== ;

; TODO CLJS
#?(:clj
(defnt ^{:tag #?(:clj Period)} ->period*
  ([y     ] (->period* y 0 0))
  ([y mo  ] (->period* y mo 0))
  ([y mo d]
    (#?(:clj Period/of :cljs (TODO)) (->int y) (->int mo) (->int d)))))

; TODO CLJS
#?(:clj (defmacro ->period ([] (case-env :clj `Period/ZERO :cljs (TODO)))
                           ([& args] `(->period* ~@args))))

; TODO CLJS
#?(:clj (defnt period? ([^Period x] true) ([^default x] false)))

; TODO CLJS
#?(:clj
(defnt ^{:tag #?(:clj Duration)} ->duration*
  ([d        ] (#?(:clj Duration/ofDays :cljs (TODO)) d))
  ([d h      ] (.plus (->duration* d)
                      (#?(:clj Duration/ofHours   :cljs (TODO)) (->long h))))
  ([d h m    ] (.plus (->duration* d h)
                      (#?(:clj Duration/ofMinutes :cljs (TODO)) (->long m))))
  ([d h m s  ] (.plus (->duration* d h m)
                      (#?(:clj Duration/ofSeconds :cljs (TODO)) (->long s))))
  ([d h m s n] (.plus (->duration* d h m s)
                      (#?(:clj Duration/ofNanos   :cljs (TODO)) (->long n))))))

; TODO CLJS
#?(:clj (defmacro ->duration ([] (case-env :clj `Duration/ZERO :cljs (TODO)))
                             ([& args] `(->duration* ~@args))))

; TODO CLJS
#?(:clj (defnt duration? ([^Duration x] true) ([^default x] false)))

; ===== TO TEMPORAL PORTION ===== ;

; TODO CLJS
#?(:clj
(defnt ^long ->nanos [^Duration x] (.toNanos x))) ; todo make return type hint unnecessary

; ===== MISCELLANEOUS ===== ;

#?(:clj
(defnt ^TimeUnit ->timeunit
  "Constructs an instance of `java.util.concurrent.TimeUnit`."
  (^:inline [^TimeUnit x] x)
  ([^keyword? x]
    (case x
      (:nanoseconds :nanos :ns)   TimeUnit/NANOSECONDS
      (:microseconds :us)         TimeUnit/MICROSECONDS
      (:milliseconds :millis :ms) TimeUnit/MILLISECONDS
      (:seconds :sec)             TimeUnit/SECONDS
      (:minutes :mins)            TimeUnit/MINUTES
      (:hours :hrs)               TimeUnit/HOURS
      :days                       TimeUnit/DAYS))))

; TODO +, -, <, >, <=, >=, etc.

; #?(:clj
; (defn now-formatted [date-format]
;   (.format (DateTimeFormatter/ofPattern date-format) (now-local))))

(defn now-formatted [date-format])

#?(:clj
(def formats ; TODO map->record
  {:rfc       DateTimeFormatter/RFC_1123_DATE_TIME
   :windows   (DateTimeFormatter/ofPattern "E, dd MMM yyyy HH:mm:ss O")
   :calendar  "EEE MMM dd HH:mm:ss.SSS z yyyy"
   :friendly  "MMM dd, yyyy h:mm:ss a"
   :jdbc-date "yyyy-MM-dd"
   :jdbc-time "HH:mm:ss"}))

#?(:clj
(defnt ->string*
  ([^string?           formatting date]
    (.format (DateTimeFormatter/ofPattern formatting) ^java.time.LocalDateTime (->local-date-time date)))
  ([^java.time.format.DateTimeFormatter formatting date]
    (.format formatting ^java.time.LocalDateTime (->local-date-time date)))
  ([^keyword?          formatting date]
            (let [^DateTimeFormatter formatter
                    (or (get formats formatting)
                        (throw (Exception. "Formatter not found")))]
              (.format formatter ^java.time.LocalDateTime (->local-date-time date))))))


(defn ->string
  {:doc "Returns a string representation of `x` in UTC time-zone
         using \"yyyy-MM-dd'T'HH:mm:ss.SSSZZ\" date-time representation."}
  ([x]
    #?(:clj  (TODO)
       :cljs (TODO)))
  ([date formatting]
    #?(:clj  (->string* formatting date)
       :cljs (TODO))))

; #?(:clj (defn str-now [] (now-formatted "MM-dd-yyyy HH:mm::ss")))
; #?(:clj (def timestamp str-now))

; #?(:clj
; (defn beg-of-day
;   ([^LocalDate date] (.atStartOfDay date))
;   ([y m d] (beg-of-day (day y m d)))))

; (defn beg-of-day
;   ([date]
;     (apply beg-of-day (ymd date)))
;   (#?(:clj  [^long y ^long m ^long d]
;       :cljs [y m d])
;     (time/date-time y m d 0 0 0 0)))

; (defn end-of-day
;   ([date]
;     (apply end-of-day (ymd date)))
;   ([^long y ^long m ^long d]
;     (time/date-time y m d 23 59 59 999)))

; (defn whole-day
;   ([date]
;     (apply whole-day (ymd date)))
;   ([^long y ^long m ^long d]
;     (time/interval (beg-of-day y m d) (end-of-day y m d))))

; (defn on?
;   "Determines if date is on day.
;    Inclusive of intervals."
;   ([date on-date]
;     (apply on? date (ymd on-date)))
;   ([date y m d]
;     (time/within? (whole-day y m d)   date)))

#?(:clj
  (defmethod print-dup java.util.Date
    ^{:attribution "clojuredocs.org, |print-dup|"}
    [o w]
    (print-ctor o (fn [o w] (print-dup (.getTime ^java.util.Date o) w)) w)) )

; (defn ^Delay for-days-between
;   [date-a date-b f]
;   (let [difference-in-days
;           (time/in-days (time/interval date-a date-b))
;         instants-on-beg-of-days
;           (periodic/periodic-seq
;             date-a (time/days 1))]
;   (for+ [day (take (inc difference-in-days) instants-on-beg-of-days)]
;     (f day))))

#?(:clj
  (defn long-timestamp []
    (long (/ (.getTime (Date.)) 1000))))

; (defnt -
;   [org.joda.time.Hours] ([x] (.negated x))
;   :default
;     ([date1 date2]
;       (time/interval date2 date1)))

; #?(:clj (defn between [a b] (Period/between a b)))

; #?(:clj (def interval between))

; #?(:clj
; (defn since [date]
;   (between date (now))))

#?(:clj
(defn parse [text formatter]
  (LocalDate/parse text (DateTimeFormatter/ofPattern formatter))))

#?(:clj (defn system-timezone []
  (.getID (java.util.TimeZone/getDefault))))

#?(:clj (def date-format-json
  (java.text.SimpleDateFormat. "yyyy-MM-dd'T'HH:mm:ss'Z'")))

#?(:clj
(defnt ^java.util.Calendar ->calendar
  ; Calendar initialized with the default locale and time zone
  ([^integer? x]
    (doto (java.util.Calendar/getInstance)
      (.setTimeInMillis x)))
  ([^java.util.Date x ^java.util.Locale locale ^java.util.TimeZone timeZone]
    (doto (Calendar/getInstance timeZone locale)
      (.setTime x)))
  ([^string? x]
    (let [df (java.text.SimpleDateFormat. (:calendar formats))]
      (doto (Calendar/getInstance)
            (.setTime (.parse df x)))))))

#?(:clj
(defnt ^java.sql.Time ->sql-time
  ; TODO The string must be formatted as JDBC_TIME_FORMAT
  ([^integer?           x] (java.sql.Time. x))
  ([^string?            x] (java.sql.Time/valueOf x))
  #_([^String x ^TimeZone timeZone]
    (-> timeZone ->TimeFormat (.parse x) ->epoch-millis ->sql-time))
  ([^java.util.Date x]
    (let [^Calendar cal
            (doto (Calendar/getInstance) (.setTimeInMillis (.getTime x)))]
      (java.sql.Time.
        (.get cal Calendar/HOUR_OF_DAY)
        (.get cal Calendar/MINUTE     )
        (.get cal Calendar/SECOND     ))))
  ([^java.sql.Timestamp x] (-> x ->epoch-millis ->sql-time))))


#?(:clj
(defnt ^java.sql.Date ->sql-date
  ([^integer?           x] (java.sql.Date. x))
  ; TODO The string must be formatted as JDBC_DATE_FORMAT
  ([^string?            x] (java.sql.Date/valueOf x))
  #_([^String x ^java.util.TimeZone timeZone]
    (-> timeZone ->DateFormat (.parse x) ->epoch-millis ->sql-date))
  ([^java.util.Date x]
    (let [^Calendar cal
           (doto (Calendar/getInstance) (.setTimeInMillis (.getTime x)))]
      (java.sql.Date.
       (core/- (.get cal Calendar/YEAR   ) 1900)
       (.get    cal Calendar/MONTH       )
       (.get    cal Calendar/DAY_OF_MONTH))))
  ([^java.sql.Timestamp x] (-> x ->epoch-millis ->sql-date))))

#?(:clj
(defnt ^java.util.Date ->platform-instant
  "Returns a platform instant (java.util.Date for Java, js/Date for JS)."
  ([^long?              x] (java.util.Date. x))
  ([^string?            x] (-> (java.text.SimpleDateFormat. (:calendar formats)) (.parse x)))
  ([^java.util.Calendar x] (.getTime x))
  ; Technically Timestamp extends java.util.Date
  ([#{java.sql.Timestamp java.sql.Date} x] (-> x ->epoch-millis ->platform-instant))
  ([^java.time.Instant       x] (Date/from x))
  ([^java.time.ZonedDateTime x]
    (if (= (.getZone x) ZoneOffset/UTC)
        (-> x ->epoch-millis ->platform-instant)
        (throw (->ex "Refusing to lose time zone information to java.util.Date"))))))

#?(:clj
(defnt ^java.sql.Timestamp ->timestamp
  ([^integer?             x] (java.sql.Timestamp. x))
  ([^string?              x] (java.sql.Timestamp/valueOf x))
  ([#{java.util.Date
      java.sql.Date
      java.util.Calendar} x] (-> x ->epoch-millis ->timestamp))))

#?(:clj
(defnt ^java.util.TimeZone ->timezone
  ([^string? x] (java.util.TimeZone/getTimeZone x))))

; ===== GETTING ===== ;

; TODO any other time based ChronoField is allowed
(extend-protocol quantum.core.collections.core/GetProtocol
  #?(:clj LocalDate :cljs js/JSJoda.LocalDate)
  (#?(:clj get-protocol :cljs get) [x k]
    (case k :year          (#?(:clj .getYear        :cljs .year      ) x)
            :year-day      (#?(:clj .getDayOfYear   :cljs .dayOfYear ) x)
            :month         (#?(:clj .getMonthValue  :cljs .monthValue) x)
            :month-enum    (#?(:clj .getMonth       :cljs .month     ) x)
            :week-day      (#?(:clj .getValue :cljs .value)
                            (#?(:clj .getDayOfWeek   :cljs .dayOfWeek ) x))
            :week-day-enum (#?(:clj .getDayOfWeek   :cljs .dayOfWeek ) x)
            :day           (#?(:clj .getDayOfMonth  :cljs .dayOfMonth) x)
            :month-day     (#?(:clj .getDayOfMonth  :cljs .dayOfMonth) x)))
  #?(:clj LocalTime :cljs js/JSJoda.LocalTime)
  (#?(:clj get-protocol :cljs get) [x k]
    (case k :hour         (#?(:clj .getHour   :cljs .hour  ) x)
            :hour-am-pm    (.get x #?(:clj ChronoField/HOUR_OF_AMPM :cljs js/JSJoda.ChronoField.HOUR_OF_AMPM   ))
            :minute       (#?(:clj .getMinute :cljs .minute) x)
            :second       (#?(:clj .getSecond :cljs .second) x)
            ; :milli        (#?(:clj .getMillisecond :cljs .millisecond) x)
            :nano         (#?(:clj .getNano   :cljs .nano  ) x)))
  #?(:clj LocalDateTime :cljs js/JSJoda.LocalDateTime)
  (#?(:clj get-protocol :cljs get) [x k]
    (case k :year          (#?(:clj .getYear        :cljs .year      ) x)
            :year-day      (#?(:clj .getDayOfYear   :cljs .dayOfYear ) x)
            :month         (#?(:clj .getMonthValue  :cljs .monthValue) x)
            :month-enum    (#?(:clj .getMonth       :cljs .month     ) x)
            :week-day      (#?(:clj .getValue :cljs .value)
                            (#?(:clj .getDayOfWeek   :cljs .dayOfWeek ) x))
            :week-day-enum (#?(:clj .getDayOfWeek   :cljs .dayOfWeek ) x)
            :day           (#?(:clj .getDayOfMonth  :cljs .dayOfMonth) x)
            :month-day     (#?(:clj .getDayOfMonth  :cljs .dayOfMonth) x)
            :hour          (#?(:clj .getHour        :cljs .hour      ) x)
            :hour-am-pm    (.get x #?(:clj ChronoField/HOUR_OF_AMPM :cljs js/JSJoda.ChronoField.HOUR_OF_AMPM   ))
            :minute        (#?(:clj .getMinute      :cljs .minute    ) x)
            :second        (#?(:clj .getSecond      :cljs .second    ) x)
            ; :milli        (#?(:clj .getMillisecond :cljs .millisecond) x)
            :nano          (#?(:clj .getNano        :cljs .nano      ) x))))

; ===== PREDICATES ===== ;

(defnt ^boolean leap-year?
  ([#{#?(:clj LocalDate     :cljs js/JSJoda.LocalDate    )} x] (.isLeapYear x))
  ([#{#?(:clj LocalDateTime :cljs js/JSJoda.LocalDateTime)} x] (-> x .toLocalDate leap-year?)))

; ===== MODIFY ===== ;

(extend-protocol quantum.core.collections.core/AssocProtocol
  #?(:clj LocalDate :cljs js/JSJoda.LocalDate)
  (#?(:clj assoc-protocol :cljs assoc) [x k v]
    (case k :days       (.withDayOfMonth x v)
            :month-days (.withDayOfMonth x v)
            :year-days  (.withDayOfYear  x v)
            :months     (.withMonth      x v)
            :years      (.withYear       x v)))
  #?(:clj LocalTime :cljs js/JSJoda.LocalTime)
  (#?(:clj assoc-protocol :cljs assoc) [x k v]
    (case k :seconds    (.withSecond     x v)
            :minutes    (.withMinute     x v)
            :hours      (.withHour       x v))))


; (defn
;   convert
;   "Converts @obj to a |java.sql.Timestamp| using the supplied time zone.
;    Note that the string representation is referenced to @timeZone, not UTC.
;    The |Timestamp is adjusted to the specified time zone before conversion.
;    This behavior is intended to accommodate user interfaces, where users are
;    accustomed to viewing timestamps in their own time zone."
;   [^String obj ^Locale locale ^TimeZone timeZone ^String formatString]
;   (let [^Timestamp parsedStamp (->timestamp obj)
;         ^Calendar cal (Calendar/getInstance timeZone locale)]
;     (.setTime cal parsedStamp)
;     (.add cal Calendar/MILLISECOND (- (.getOffset timeZone (.getTime parsedStamp))))
;       (let [^Timestamp result (Timestamp. (.getTimeInMillis cal))]
;         (.setNanos result (.getNanos parsedStamp))
;         result)))




; (defn
;   convert
;   "Converts @obj to a |String| using the supplied time zone.
;    Note that the string representation is referenced to @timeZone, not UTC.
;    The |Timestamp is adjusted to the specified time zone before conversion.
;    This behavior is intended to accommodate user interfaces, where users are
;    accustomed to viewing timestamps in their own time zone."
;   [^Timestamp obj ^Locale locale ^TimeZone timeZone ^String formatString]
;   (let [^Calendar cal (Calendar/getInstance timeZone locale)])
;     (.setTime cal obj)
;     (.add cal (Calendar/MILLISECOND) (.getOffset timeZone (.getTime obj)))
;     (let [^Timestamp result (Timestamp. (.getTimeInMillis cal))])
;       (.setNanos result (.getNanos obj))
;       (.toString result))




; (defnt
;   ->DateFormat
;   ([^java.util.TimeZone tz]
;     (doto (java.text.SimpleDateFormat. JDBC_DATE_FORMAT)
;       (.setTimeZone tz))))

; (defn
;   ->DateTimeFormat
;   "Returns an initialized DateFormat object.
;    @param dateTimeFormat
;    optional format string
;    @param tz
;    @param locale
;    can be null if dateTimeFormat is not null
;    @return DateFormat object"
;   [^String dateTimeFormat ^TimeZone tz ^Locale locale]
;   (let [^DateFormat df nil]
;   (if (empty? dateTimeFormat)
;       (swap!
;        df
;        (.getDateTimeInstance
;         DateFormat
;         (.SHORT DateFormat)
;         (.MEDIUM DateFormat)
;         locale))
;       (swap! df (SimpleDateFormat. dateTimeFormat)))
;   (.setTimeZone df tz)
;   df))

; (defn ->TimeFormat
;   [^TimeZone tz]
;   (doto (java.text.SimpleDateFormat. (:jdbc-time formats))
;     (.setTimeZone tz)))

;  (defn
;   convert
;   "Converts @obj to a |String| using the supplied locale, time zone, and format string.
;    If @formatString is nil, the string is formatted as CALENDAR_FORMAT."
;   [^Calendar obj ^Locale locale ^TimeZone timeZone ^String formatString]
;   (let
;    [^DateFormat
;     df
;     (toDateTimeFormat
;      (if (= formatString nil) CALENDAR_FORMAT formatString)
;      timeZone
;      locale)])
;   (.setCalendar df obj)
;   (.format df (.getTime obj)))



;  date->str
;  (defnTODO
;   convert
;   [^Date obj ^Locale locale ^TimeZone timeZone ^String formatString]
;   (let
;    [^DateFormat
;     df
;     (toDateTimeFormat
;      (if (= formatString nil) CALENDAR_FORMAT formatString)
;      timeZone
;      locale)])
;   (.format df obj))


;  (defn
;   convert
;   "/*\n\nReturns <code>obj</code> converted to a <code>Calendar</code>,\ninitialized with the specified locale and time zone. The\n<code>formatString</code> parameter is ignored.\n         */\n"
;   [^Long obj ^Locale locale ^TimeZone timeZone ^String formatString]
;   (let [^Calendar cal (.getInstance Calendar timeZone locale)])
;   (.setTimeInMillis cal obj)
;   cal)


;  (defn
;   convert
;   "Converts @obj to a String using the supplied time zone.
;    The returned string is formatted as JDBC_DATE_FORMAT"
;   [^java.sql.Date obj ^TimeZone timeZone]
;   (let [^DateFormat df (toDateFormat timeZone)])
;   (.format df obj))

;  (defn
;   convert
;   "Converts @obj to a String using the supplied time zone.
;    The returned string is formatted as JDBC_TIME_FORMAT"
;   [^java.sql.Time obj ^TimeZone timeZone]
;   (let [^DateFormat df (toTimeFormat timeZone)])
;   (.format df obj))

;  (defn
;   convert
;   "Converts @obj to a |java.util.Calendar| initialized to\nthe supplied locale and time zone.
;    If <code>formatString</code> is\n<code>null</code>, the string is formatted as\n{@link DateTimeConverters#CALENDAR_FORMAT}.\n         */\n"
;   [^String obj ^Locale locale ^TimeZone timeZone ^String formatString]
;   (let [^DateFormat df
;          (toDateTimeFormat (or formatString CALENDAR_FORMAT) timeZone locale)
;         ^Date date (.parse df obj)
;         ^Calendar cal (.getInstance Calendar timeZone locale)]
;     (.setTimeInMillis cal (.getTime date))
;     cal)))

;  (defn convert
;   "Converts @obj to a java.util.Date.
;    If @formatString is nil, the string is formatted as CALENDAR_FORMAT."
;   [^String obj ^Locale locale ^TimeZone timeZone ^String formatString]
;   (-> (or formatString CALENDAR_FORMAT)
;       (->DateTimeFormat timeZone locale)
;       (.parse obj)))


; ===== DAYS OF WEEK ===== ;

