(ns
  ^{:doc "An alias of the clj-time.core namespace. Also includes
          useful functions such as |beg-of-day|, |end-of-day|,
          |on?|, |for-days-between|, etc."
    :attribution "Alex Gunnarson"}
  quantum.core.time.core
  (:refer-clojure :exclude
    [extend second - + < <= > >= format])
  (:require
    [clojure.core                   :as core]
#?@(:cljs
   [[cljs-time.instant] ; To make dates serializable with pr-str
    [cljsjs.js-joda]])
    [quantum.core.error             :as err
      :refer [->ex TODO throw-unless]]
    [quantum.core.fn                :as fn
      :refer [fn1 <-]]
    [quantum.core.logic             :as logic
      :refer [fn-or whenc]]
    [quantum.core.macros            :as macros
      :refer [defnt if-cljs]]
    [quantum.core.numeric           :as num]
    [quantum.core.collections       :as coll
      :refer [ifor]]
    [quantum.measure.convert        :as uconv
      :refer [convert]]
    [quantum.core.convert.primitive :as pconv
      :refer [->long]]
    [quantum.core.vars              :as var
      :refer [defalias]])
  (:require-macros
    [quantum.core.time.core         :as self
      :refer [->local-date]])
#?(:clj
  (:import
    [java.util Date Calendar]
    [java.util.concurrent TimeUnit]
    [java.time Month LocalDate LocalTime LocalDateTime ZonedDateTime ZoneId ZoneOffset]
    [java.time.format DateTimeFormatter]
    [java.time.temporal Temporal TemporalAccessor ChronoField])))

; js/JSJoda

; ===== ABOUT =====
; https://www.npmjs.com/package/js-joda
; js-joda is fast. It is about 2 to 10 times faster than other javascript date libraries.
; js-joda is robust and stable.
; =================

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

; Static start nano counter and then get an offset
; System.nanoTime();

; AD 1 435432080161209600000000000N
; 1970 435432142297670400000000000N
; Nanos since 1970 1437516575242000000N
; 2015 435432143717740800000000000N
; NOW  435432143735187066453000000N

; Measured since the beginning of time within this realm of being
#?(:clj
(def ^:const beg-of-time-to-calendar-begin
  ; As of 2013, time's expansion is estimated to have begun
  ; 13.798 ± 0.037 billion years ago. (Wikipedia)
  (-> (num/->ratio 13.798)
      (* num/billion)  ; years
      (convert :years :nanos))))

; 1 nanoday = 1.44 minutes

; Negative is for BC
(defonce strange-leap-years
  #{-45 -42 -39 -36 -33 -30 -27 -24 -21 -18 -15 -12 -9 -8 12})
(defonce first-normal-leap-year 12)
(defonce gregorian-calendar-decree-year 1582)

#?(:clj
(defnt leap-year?*
  "Using Gregorian calendar 3 criteria."
  {:source "Wikipedia"
   :todo ["Implement Julian calendar, etc."]}
  ([^integer? y]
    ; 46 BC is 708 AUC
    (if (= y 0)
        (throw (->ex nil "Year does not exist."))
        (or (contains? strange-leap-years y)
            (and (core/> y first-normal-leap-year)
                 (num/evenly-divisible-by? y 4)
                 (if (core/> y gregorian-calendar-decree-year)
                     (if (num/evenly-divisible-by? y 100)
                         (num/evenly-divisible-by? y 400)
                         true)
                     true)))))))

; ~48 MB
; Indices are the year
; Can't make constant... "maybe print-dup not defined"
; ObjectArray[4000]      15.640625  MB
; PersistentVector[4000] 115.109375 MB

; 0 BC, 0 AD don't exist
; 10000 BC = arr[0]
; 1 BC     = arr[9999]
; 1 AD     = arr[10001]
#?(:clj
(def ^"[Lclojure.lang.BigInt;" nanos-at-beg-of-year
  (make-array clojure.lang.BigInt 14000)))

#?(:clj
(defnt nanos-arr-index->year
  ([^long? n] (if (= n 10000) nil (core/- n 10000)))))

#?(:clj
(defnt year->nanos-arr-index
  ([^long? n]
    (whenc (if (= n 0) nil (core/+ n 10000))
      (fn-or nil? (fn1 core/< 0) (fn1 core/>= (alength nanos-at-beg-of-year)))
      nil))))

; Initialize nanos-at-beg-of-year
; TODO use aget, aset, alength from /coll
#?(:clj
(do (aset nanos-at-beg-of-year 0 beg-of-time-to-calendar-begin)
    (ifor [n 1 (core/< n (alength nanos-at-beg-of-year)) (inc n)]
      (when-let [year (nanos-arr-index->year n)]
        (let [year-nanos (convert (if (leap-year?* year) 366 365) :days :nanos)
              prev-i (if (= n 10001) 9999 (dec n))
              nanos-f (core/+ year-nanos (aget nanos-at-beg-of-year prev-i))]
          (aset nanos-at-beg-of-year n nanos-f))))))

; (Signed) longs are at max 1.0842021724855044E19, the maximum between fields.
; with 1E18 between (because longs are ~1E19)
; 1E36, exa-x, x, atto-x, 1E-36, 1E-54

; Nanoseconds since the Big Bang
; (Optimally would have done Planck quanta since the Big Bang)
; Nanosecond: 1E-9 seconds
(defrecord Instant  [nanos]) ; may or may not be a long
(defrecord StandardInstant [year month day minute second nanos])
(defrecord Duration [nanos]) ; may or may not be a long

#?(:clj
(defn year->nanos
  [y]
  (if-let [i (year->nanos-arr-index y)]
    (aget nanos-at-beg-of-year i)
    (do (throw-unless (pos? y) (->ex nil "Year not valid" y))
      (let [last-i (-> nanos-at-beg-of-year alength dec)
            last-year (nanos-arr-index->year last-i)]
        (core/+ (aget nanos-at-beg-of-year last-i)
          (->> (range (inc last-year) y) (filter #(leap-year?* %1)) count (* 366) (<- convert :days :nanos))
          (->> (range (inc last-year) y) (remove #(leap-year?* %1)) count (* 365) (<- convert :days :nanos))))))))

(defn nanos->instant [n] (Instant. n))

; Sum of time from beginning of Big Bang through 1969, in nanoseconds
#?(:clj (def ^:const unix-epoch (-> 1970 year->nanos nanos->instant)))

#?(:clj (defn unix-millis->nanos        [         n] (-> n (convert :millis :nanos) (core/+ (:nanos unix-epoch)))))
#?(:clj (defn unix-millis->instant      [         n] (-> n unix-millis->nanos nanos->instant)))

#?(:clj (defn instant->nanos            [         n] (-> n :nanos)))
#?(:clj (defn instant->unix-millis      [         n] (-> n instant->nanos     (core/- (:nanos unix-epoch)) (convert :nanos :millis))))
#?(:clj (defn nanos->standard-instant   [         n]))
#?(:clj (defn instant->standard-instant [^Instant n] (-> n instant->nanos nanos->standard-instant)))

#?(:clj (defn now-unix    [] (System/currentTimeMillis)))
#?(:clj (defn now-nanos   [] (-> (System/currentTimeMillis) unix-millis->nanos)))
#?(:clj (defn now-instant [] (-> (System/currentTimeMillis) unix-millis->instant)))
#?(:clj (defn now         [] (-> (now-instant) instant->standard-instant)))

#?(:clj (defn nanos->year
  {:todo ["Unoptimized"]}
  [n]
  (let [gregorian-difference
          (core/- (now-nanos) (year->nanos gregorian-calendar-decree-year))
        gregorian? (core/>= 0 gregorian-difference)]
    (if gregorian?
        (-> n (convert :nanos :days) (/ 365.2425) num/floor
              (core/+ gregorian-calendar-decree-year)))
    #_(-> (whenf (coll/binary-search nanos-at-beg-of-year n true)
          vector? first)
        nanos-arr-index->year))))

; #?(:clj (defn gmt-now   [] (OffsetDateTime/now (ZoneId/of "GMT"))))
; #?(:clj (defn now-local [] (LocalDateTime/now)))

;(def RFC_1123_DATE_TIME ) ; A working replacement

#?(:clj (defn + [^Instant a ^Duration b]
  (Instant. (core/+ (:nanos a) (:nanos b)))))

(defnt ^long ->unix-millis
  #?@(:clj  [([^java.time.Instant       x] (-> x (.toEpochMilli)))
             ([^java.util.Date          x] (-> x (.getTime)     ))
             ([^java.time.LocalDate     x] (-> x (.toEpochDay ) (convert :days  :millis)))
             ([^java.time.LocalDateTime x] (-> x (.toInstant ZoneOffset/UTC) ->unix-millis))
             ([^java.time.ZonedDateTime x] (-> x .toInstant ->unix-millis))
             ([^org.joda.time.DateTime  x] (-> x (.getMillis)   ))
             ([^java.util.Calendar      x] (-> x (.getTimeInMillis)))]
      :cljs [([^number?                 x] (->long x))
             ([^js/Date                 x] (.getTime x))]))

#?(:clj
(defnt ->duration
  ([^java.time.LocalTime x] (-> x ->unix-millis (Duration.)))))

(declare ->local-date-time-protocol)

#?(:clj
(defnt ^quantum.core.time.core.Instant ->instant
  ([^quantum.core.time.core.Instant x] x)
  ([#{java.time.LocalDate
      java.time.LocalDateTime}      x] (-> x ->unix-millis unix-millis->instant))
  ([^java.time.Year                 x] (-> x (.getValue) year->nanos nanos->instant))
  ([#{java.time.Instant
      java.util.Date
      org.joda.time.DateTime}       x] (-> x ->unix-millis unix-millis->instant))
  ([^string? s k] (-> s (->local-date-time-protocol k) ->instant))
  #_([^java.time.ZonedDateTime x])
  #_([^java.time.YearMonth     x])
  #_([^java.util.Date$ZonedDateTime x])))

#?(:clj
(defnt ^java.time.Instant ->platform-instant
  "Coerces to an instantaneous point on an imaginary timeline."
  ([#{long? bigint?} x] (-> x ->long (java.time.Instant/ofEpochMilli)))
  ([x] (-> x ->unix-millis-protocol ->platform-instant))))

; ===== DATE ===== ;

(defnt ->platform-date
  "Returns a platform date (java.util.Date for Java, js/Date for JS)."
  #?@(:clj  [(^java.util.Date [^java.time.Instant              t] (Date/from t))
             (^java.util.Date [^quantum.core.time.core.Instant t] (-> t ->platform-instant ->platform-date))]
      :cljs [(^js/Date        [                                x] (TODO))]))

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
   [] (if-cljs &env `(js/JSJoda.LocalDate.now) `(LocalDate/now)))
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
  ([] (if-cljs &env `(js/JSJoda.LocalTime.now) `(LocalTime/now)))
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
  ([] (if-cljs &env `(js/JSJoda.LocalDateTime.now) `(LocalDateTime/now)))
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
  ([] (if-cljs &env `(js/JSJoda.ZonedDateTime.now) `(ZonedDateTime/now)))
  ([x & args] `(->zoned-date-time* ~x ~@args))))

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

#?(:clj (defn <  ([a b] (core/<  (-> a ->instant :nanos) (-> b ->instant :nanos)))))
#?(:clj (defalias before? <))
#?(:clj (defn >  ([a b] (core/>  (-> a ->instant :nanos) (-> b ->instant :nanos)))))
#?(:clj (defalias after? >))
#?(:clj (defn <= ([a b] (core/<= (-> a ->instant :nanos) (-> b ->instant :nanos)))))
#?(:clj (defn >= ([a b] (core/>= (-> a ->instant :nanos) (-> b ->instant :nanos)))))


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
    (-> timeZone ->TimeFormat (.parse x) ->unix-millis ->sql-time))
  ([^java.util.Date x]
    (let [^Calendar cal
            (doto (Calendar/getInstance) (.setTimeInMillis (.getTime x)))]
      (java.sql.Time.
        (.get cal Calendar/HOUR_OF_DAY)
        (.get cal Calendar/MINUTE     )
        (.get cal Calendar/SECOND     ))))
  ([^java.sql.Timestamp x] (-> x ->unix-millis ->sql-time))))


#?(:clj
(defnt ^java.sql.Time ->sql-date
  ([^integer?           x] (java.sql.Date. x))
  ; TODO The string must be formatted as JDBC_DATE_FORMAT
  ([^string?            x] (java.sql.Date/valueOf x))
  #_([^String x ^java.util.TimeZone timeZone]
    (-> timeZone ->DateFormat (.parse x) ->unix-millis ->sql-date))
  ([^java.util.Date x]
    (let [^Calendar cal
           (doto (Calendar/getInstance) (.setTimeInMillis (.getTime x)))]
      (java.sql.Date.
       (core/- (.get cal Calendar/YEAR   ) 1900)
       (.get    cal Calendar/MONTH       )
       (.get    cal Calendar/DAY_OF_MONTH))))
  ([^java.sql.Timestamp x] (-> x ->unix-millis ->sql-date))))

#?(:clj
(defnt ^java.util.Date ->date
  ([^long?              x] (java.util.Date. x))
  ([^string?            x] (-> (java.text.SimpleDateFormat. (:calendar formats)) (.parse x)))
  ([^java.util.Calendar x] (.getTime x))
  ; Technically Timestamp extends java.util.Date
  ([#{java.sql.Timestamp java.sql.Date} x] (-> x ->unix-millis ->date))))

#?(:clj
(defnt ^java.sql.Time ->timestamp
  ([^integer?             x] (java.sql.Timestamp. x))
  ([^string?              x] (java.sql.Timestamp/valueOf x))
  ([#{java.util.Date
      java.sql.Date
      java.util.Calendar} x] (-> x ->unix-millis ->timestamp))))

#?(:clj
  (defnt ^java.util.TimeZone ->timezone
  ([^string? x]  (java.util.TimeZone/getTimeZone x))))

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

