(ns
  ^{:doc "An alias of the clj-time.core namespace. Also includes
          useful functions such as |beg-of-day|, |end-of-day|,
          |on?|, |for-days-between|, etc."
    :attribution "Alex Gunnarson"}
  quantum.core.time.core
  (:refer-clojure :exclude [extend second - + < <= > >= format])
  #_(:require-quantum [ns macros type num fn logic bin err log uconv loops])
  #?(:clj (:import [java.util Date Calendar]
            (java.time LocalDate)
            (java.time.format DateTimeFormatter)
            (java.time.temporal Temporal TemporalAccessor))))

; Static start nano counter and then get an offset
; System.nanoTime();

; AD 1 435432080161209600000000000N
; 1970 435432142297670400000000000N
; Nanos since 1970 1437516575242000000N
; 2015 435432143717740800000000000N
; NOW  435432143735187066453000000N

#_(defn binary-search
  "Finds earliest occurrence of @x in @xs (a (sorted) List) using binary search."
  {:source "http://stackoverflow.com/questions/8949837/binary-search-in-clojure-implementation-performance"
   :todo ["Move ns"]}
  ([xs x] (binary-search xs x 0 (dec* (count xs)) false))
  ([xs x a b between?]
    (loop [l (long a) h (long b)]
      (if (core/<= h (inc l))
          (cond
            (= x (get xs l)) l
            (= x (get xs h)) h
            :else (when between?
                    (if (= l h)
                        [(dec l) h]
                        [l       h])))
          (let [m (-> h (-* l) (>> 1) (+* l))]
            (if (core/< (get xs m) x) ; negative favors the left side in |compare|
                (recur (long (inc* m)) (long h))
                (recur (long l)        (long m))))))))


; (if a b true) => (or (not a) b)

;(java.time Period Instant LocalDateTime LocalDate OffsetDateTime ZoneId)

; Measured since the beginning of time within this realm of being
#_(def ^:const beg-of-time-to-calendar-begin
  ; As of 2013, time's expansion is estimated to have begun
  ; 13.798 ± 0.037 billion years ago. (Wikipedia)
  (-> (rationalize 13.798)
      (* num/billion)  ; years
      (convert :years :nanos))) 

; 1 nanoday = 1.44 minutes

; Negative is for BC
#_(def ^:const strange-leap-years
  #{-45 -42 -39 -36 -33 -30 -27 -24 -21 -18 -15 -12 -9 -8 12})
#_(def ^:const first-normal-leap-year 12)
#_(def ^:const gregorian-calendar-decree-year 1582)

#_(defnt leap-year?
  "Using Gregorian calendar 3 criteria."
  {:source "Wikipedia"
   :todo ["Implement Julian calendar, etc."]}
  ([^integer? y]
    ; 46 BC is 708 AUC
    (if (= y 0)
        (throw (Exception. "Year does not exist."))
        (or (contains? strange-leap-years y)
            (and (core/> y first-normal-leap-year)
                 (num/evenly-divisible-by? y 4)
                 (if (core/> y gregorian-calendar-decree-year)
                     (if (num/evenly-divisible-by? y 100)
                         (num/evenly-divisible-by? y 400)
                         true)
                     true))))))

; ~48 MB
; Indices are the year
; Can't make constant... "maybe print-dup not defined"
; ObjectArray[4000]      15.640625  MB
; PersistentVector[4000] 115.109375 MB
#_(def ^"[Lclojure.lang.BigInt;" nanos-at-beg-of-year 
  (make-array clojure.lang.BigInt 14000))

#_(defnt nanos-arr-index->year
  ([^long? n] (if (= n 10000) nil (core/- n 10000))))

#_(defnt year->nanos-arr-index
  ([^long? n]
    (whenc (if (= n 0) nil (core/+ n 10000))
      (fn-or nil? (f*n core/< 0) (f*n core/>= (alength nanos-at-beg-of-year)))
      nil)))

; Initialize nanos-at-beg-of-year
#_(do (aset nanos-at-beg-of-year 0 beg-of-time-to-calendar-begin)
    (ifor [n 1 (core/< n (alength nanos-at-beg-of-year)) (inc n)]
      (when-let [year (nanos-arr-index->year n)]
        (let [year-nanos (convert (if (leap-year? year) 366 365) :days :nanos)
              prev-i (if (= n 10001) 9999 (dec n))
              nanos-f (core/+ year-nanos (aget nanos-at-beg-of-year prev-i))]
          (aset nanos-at-beg-of-year n nanos-f)))))

; 0 BC, 0 AD don't exist
; 10000 BC = arr[0]
; 1 BC     = arr[9999]
; 1 AD     = arr[10001]

; (Signed) longs are at max 1.0842021724855044E19, the maximum between fields.
; with 1E18 between (because longs are ~1E19)
; 1E36, exa-x, x, atto-x, 1E-36, 1E-54

; Nanoseconds since the Big Bang
; (Optimally would have done Planck quanta since the Big Bang)
; Nanosecond: 1E-9 seconds
#_(defrecord Instant  [^long nanos])
#_(defrecord StandardInstant [year month day minute second nanos])
#_(defrecord Duration [^long nanos])

#_(defn year->nanos
  [y]
  (if-let [i (year->nanos-arr-index y)]
    (aget nanos-at-beg-of-year i)
    (do (throw-unless (pos? y) (Err. nil "Year not valid" y))
      (let [last-i (-> nanos-at-beg-of-year alength dec)
            last-year (nanos-arr-index->year last-i)]
        (core/+ (aget nanos-at-beg-of-year last-i)
          (->> (range (inc last-year) y) (filter (mfn leap-year?)) count (* 366) (<- convert :days :nanos))
          (->> (range (inc last-year) y) (remove (mfn leap-year?)) count (* 365) (<- convert :days :nanos)))))))

#_(defn nanos->instant [n] (Instant. n))

; Sum of time from beginning of Big Bang through 1969, in nanoseconds
#_(def ^:const unix-epoch (-> 1970 year->nanos nanos->instant))

#_(defn unix-millis->nanos        [         n] (-> n (convert :millis :nanos) (core/+ (:nanos unix-epoch))))
#_(defn unix-millis->instant      [         n] (-> n unix-millis->nanos nanos->instant))

#_(defn instant->nanos            [         n] (-> n :nanos))
#_(defn instant->unix-millis      [         n] (-> n instant->nanos     (core/- (:nanos unix-epoch)) (convert :nanos :millis)))
#_(defn nanos->standard-instant   [         n])
#_(defn instant->standard-instant [^Instant n] (-> n instant->nanos nanos->standard-instant))

#_(defn now-unix    [] (System/currentTimeMillis))
#_(defn now-nanos   [] (-> (System/currentTimeMillis) unix-millis->nanos))
#_(defn now-instant [] (-> (System/currentTimeMillis) unix-millis->instant))
#_(defn now         [] (-> (now-instant) instant->standard-instant))

#_(defn nanos->year
  {:todo ["Unoptimized"]}
  [n]
  (let [gregorian-difference
          (core/- (now-nanos) (year->nanos gregorian-calendar-decree-year))
        gregorian? (core/>= 0 gregorian-difference)]
    (if gregorian?
        (-> n (convert :nanos :days) (/ 365.2425) num/floor
              (core/+ gregorian-calendar-decree-year)))
    #_(-> (whenf (binary-search nanos-at-beg-of-year n true)
          vector? first)
        nanos-arr-index->year)))

; #?(:clj (defn gmt-now   [] (OffsetDateTime/now (ZoneId/of "GMT"))))
; #?(:clj (defn now-local [] (LocalDateTime/now)))

;(def RFC_1123_DATE_TIME ) ; A working replacement

#_(defn + [^Instant a ^Duration b]
  (Instant. (core/+ (:nanos a) (:nanos b))))

#_(defnt ->duration
  ([^java.time.LocalTime x] (-> x (.toNanoOfDay) (Duration.))))

#_(defnt ->unix-millis
  ([^java.time.Instant       x] (-> x (.toEpochMilli)))
  ([^java.util.Date          x] (-> x (.getTime)     ))
  ([^org.joda.time.DateTime  x] (-> x (.getMillis)   ))
  ([^java.util.Calendar      x] (-> x (.getTimeInMillis))))

#_(defnt ^quantum.core.time.core.Instant ->instant
  ([^quantum.core.time.core.Instant x] x)
  ([^java.time.LocalDate     x] (-> x (.toEpochDay) (convert :days :millis) unix-millis->instant))
  ([^java.time.LocalDateTime x] (+ (-> x (.toLocalDate) ->instant)
                                   (-> x (.toLocalTime) ->duration)))
  ([^java.time.Year          x] (-> x (.getValue) year->nanos nanos->instant))
  ([#{java.time.Instant
      java.util.Date
      org.joda.time.DateTime} x]
    (-> x ->unix-millis unix-millis->instant))
  ([^String s k]
    (->instant
      (condp = k
        :http
          (.parse (java.text.SimpleDateFormat. "EEE, dd MMM yyyy HH:mm:ss zzz") s)
        (.parse (java.text.SimpleDateFormat. k) s))))
  #_([^java.time.ZonedDateTime x])
  #_([^java.time.YearMonth     x])
  #_([^java.util.Date$ZonedDateTime x]))

#_(defnt ^java.time.Instant ->jinstant
  ([^quantum.core.time.core.Instant x]
    (-> x instant->unix-millis (java.time.Instant/ofEpochMilli))))

#_(defnt ^java.util.Date ->jdate
  ([^java.time.Instant t]
    (Date/from t))
  ([^quantum.core.time.core.Instant t]
    (-> t ->jinstant ->jdate)))

#_(defn ->local-date-time [t-0]
  (let [^java.time.Instant t (-> t-0 ->jinstant) ]
    (-> t
        (.atZone (java.time.ZoneId/of "UTC"))
        (java.time.LocalDateTime/from))))

; (ann keyword->timeunit [Keyword -> TimeUnit])
; (defn- ^TimeUnit keyword->timeunit
;   [x]
;   (case x
;     (:nanoseconds :nanos :ns)   TimeUnit/NANOSECONDS
;     (:microseconds :us)         TimeUnit/MICROSECONDS
;     (:milliseconds :millis :ms) TimeUnit/MILLISECONDS
;     (:seconds :sec)             TimeUnit/SECONDS
;     (:minutes :mins)            TimeUnit/MINUTES
;     (:hours :hrs)               TimeUnit/HOURS
;     :days                       TimeUnit/DAYS))

; (ann ->timeunit [(U TimeUnit Keyword) -> TimeUnit])
; (defn ^TimeUnit ->timeunit
;   "Constructs an instance of `java.util.concurrent.TimeUnit`.
;   If argument x is already an instance of `TimeUnit`, the function returns x.
;   Otherwise, x *must* be a keyword, in which case the following conversion
;   is performed:
  
;   :nanoseconds | :nanos | :ns   -> TimeUnit/NANOSECONDS
;   :microseconds | :us           -> TimeUnit/MICROSECONDS
;   :milliseconds | :millis | :ms -> TimeUnit/MILLISECONDS
;   :seconds | :sec               -> TimeUnit/SECONDS
;   :minutes | :mins              -> TimeUnit/MINUTES
;   :hours | :hrs                 -> TimeUnit/HOURS
;   :days                         -> TimeUnit/DAYS
;   "
;   [x]
;   (if (instance? TimeUnit x)
;     x
;     (keyword->timeunit x)))

#_(defn <  ([a b] (core/<  (-> a ->instant :nanos) (-> b ->instant :nanos))))
#_(defalias before? <)
#_(defn >  ([a b] (core/>  (-> a ->instant :nanos) (-> b ->instant :nanos))))
#_(defalias after? >)
#_(defn <= ([a b] (core/<= (-> a ->instant :nanos) (-> b ->instant :nanos))))
#_(defn >= ([a b] (core/>= (-> a ->instant :nanos) (-> b ->instant :nanos))))


; #?(:clj
; (defn now-formatted [date-format]
;   (.format (DateTimeFormatter/ofPattern date-format) (now-local))))

(defn now-formatted [date-format])

#_(:clj
(def formats ; TODO map->record
  {:rfc       DateTimeFormatter/RFC_1123_DATE_TIME
   :windows   (DateTimeFormatter/ofPattern "E, dd MMM yyyy HH:mm:ss O")
   :calendar  "EEE MMM dd HH:mm:ss.SSS z yyyy"
   :friendly  "MMM dd, yyyy h:mm:ss a"
   :jdbc-date "yyyy-MM-dd"
   :jdbc-time "HH:mm:ss"}))

#_(:clj 
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

#_(:clj
(defn ->string [date formatting]
  (->string* formatting date)))

; #?(:clj (defn str-now [] (now-formatted "MM-dd-yyyy HH:mm::ss")))
; #?(:clj (def timestamp str-now))

; #?(:clj
; (defn day [y m d] (LocalDate/of y m d)))

; #?(:clj
; (defn beg-of-day
;   ([^LocalDate date] (.atStartOfDay date))
;   ([y m d] (beg-of-day (day y m d)))))

; (defn ymd [date]
;   (vector
;     (time/year  date)
;     (time/month date)
;     (time/day   date)))

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

#_(:clj
  (defmethod print-dup java.util.Date
    ^{:attribution "clojuredocs.org, |print-dup|"}
    [o w]
    (print-ctor o (fn [o w] (print-dup (.getTime  o) w)) w)) )

#_(:clj
  (defmethod print-dup org.joda.time.DateTime
    ^{:todo ["Fix this... only prints out current date"]}
    [d stream]
    (.write stream "#=(list \"A date should go here\" ")
    (.write stream "")
    (.write stream ")"))) 

; (defn ^Delay for-days-between
;   [date-a date-b f]
;   (let [difference-in-days
;           (time/in-days (time/interval date-a date-b))
;         instants-on-beg-of-days
;           (periodic/periodic-seq
;             date-a (time/days 1))]
;   (for+ [day (take (inc difference-in-days) instants-on-beg-of-days)]
;     (f day))))

#_(:clj
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

#_(:clj
(defn parse [text formatter]
  (LocalDate/parse text (DateTimeFormatter/ofPattern formatter))))

#_(defn system-timezone []
  (.getID (java.util.TimeZone/getDefault)))

#_(def date-format-json
  (java.text.SimpleDateFormat. "yyyy-MM-dd'T'HH:mm:ss'Z'"))

#_(defnt ^java.util.Calendar ->calendar
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
            (.setTime (.parse df x))))))

#_(defnt ^java.sql.Time ->sql-time
  ; The string must be formatted as JDBC_TIME_FORMAT
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
  ([^java.sql.Timestamp x] (-> x ->unix-millis ->sql-time)))


#_(defnt ^java.sql.Time ->sql-date
  ([^integer?           x] (java.sql.Date. x))
  ; The string must be formatted as JDBC_DATE_FORMAT
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
  ([^java.sql.Timestamp x] (-> x ->unix-millis ->sql-date)))

#_(defnt ^java.util.Date ->date
  ([^long?              x] (java.util.Date. x))
  ([^string?            x] (-> (java.text.SimpleDateFormat. (:calendar formats)) (.parse x)))
  ([^java.util.Calendar x] (.getTime x))
  ; Technically Timestamp extends java.util.Date
  ([#{java.sql.Timestamp java.sql.Date} x] (-> x ->unix-millis ->date)))

#_(defnt ^java.sql.Time ->timestamp
  ([^integer?             x] (java.sql.Timestamp. x))
  ([^string?              x] (java.sql.Timestamp/valueOf x))
  ([#{java.util.Date
      java.sql.Date
      java.util.Calendar} x] (-> x ->unix-millis ->timestamp)))

#_(defnt ^java.util.TimeZone ->timezone
  ([^string? x]  (java.util.TimeZone/getTimeZone x)))


 
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
;  (defn
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