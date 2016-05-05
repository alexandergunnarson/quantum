(ns
  ^{:doc "Simple logging capabilities. Keeps a global log, has a status channel,
          prints only if the level is enabled, etc.

          By no means a full-fledged logging system, but useful nonetheless."
    :attribution "Alex Gunnarson"}
  quantum.core.log
           (:refer-clojure :exclude [pr #?(:cljs seqable?)])
           (:require [com.stuartsierra.component   :as component]
                     [quantum.core.core            :as qcore    ]
                     [quantum.core.fn              :as fn
                       :refer [#?@(:clj [])]                    ]
                     [quantum.core.meta.debug      :as debug    ]
                     [quantum.core.print           :as pr       ]
                     [quantum.core.type.predicates :as tpred
                       :refer [seqable?]])
  #?(:cljs (:require-macros
                     [quantum.core.fn            :as fn
                       :refer []                              ])))

(defrecord LoggingLevels
  [warn user macro-expand debug trace env])

(defonce levels
  (-> {:warn              true
       :user              true}
      map->LoggingLevels atom)) ; alert, inspect, debug

(defonce log  (atom []))

(defrecord LogEntry
  [time-stamp ; ^DateTime  
   type       ; ^Keyword   
   ns-source  ; ^Namespace 
   message])  ; ^String  

(defn disable!
  {:in-types '{pr-type keyword?}}
  ([pr-type]
    (swap! levels assoc pr-type false))
  ([pr-type & pr-types]
    (doseq [pr-type-n (conj pr-types pr-type)]
      (disable! pr-type-n))))

(defn enable!
  {:in-types '{pr-type keyword?}}
  ([pr-type]
    (swap! levels assoc pr-type true))
  ([pr-type & pr-types]
    (doseq [pr-type-n (conj pr-types pr-type)]
      (enable! pr-type-n)))) 

(defrecord LogInitializer
  [levels]
  component/Lifecycle
  (start [this]
    (apply enable! levels)
    this)
  (stop  [this]
    (apply disable! levels)
    this))

(defn ->log-initializer [{:keys [levels] :as opts}]
  (when-not (seqable? levels)
    (throw (#?(:clj Exception. :cljs js/Error.)) "@levels is not seqable"))

  (LogInitializer. levels))

(defn pr*
  "Prints to |System/out| if the print alert type @pr-type
   is in the set of enabled print alert types, |levels|.

   Logs the printed result to the global log |log|."
  {:attribution "Alex Gunnarson"}
  [trace? pretty? print-fn pr-type args opts]
    (when (or (get @levels pr-type)
              #?(:cljs (= pr-type :macro-expand)))
      (let [trace?     (or (:trace?  opts) trace? )
            pretty?    (or (:pretty? opts) pretty?)
            timestamp? (:timestamp? opts)
            curr-fn (when trace? (debug/this-fn-name :prev))
            args-f ( when args @args)
            env-type-str
              (when (get @levels :env)
                (str (name qcore/lang) " »"))
            out-str
              (with-out-str
                (when (= pr-type :macro-expand) (print "\n/* "))
                #?(:clj
                  (when timestamp?
                    (let [timestamp
                           (.format
                             (java.time.format.DateTimeFormatter/ofPattern
                                "MM-dd-yyyy HH:mm::ss")
                             (java.time.LocalDateTime/now))]
                      (print (str "[" timestamp "] ")))))
                (when trace?
                  (print "[")
                  (print #?(:clj (.getName (Thread/currentThread)))
                         ":"
                         curr-fn "»"
                         (str (-> pr-type name) "] ")))
                (if (and pretty? (-> args-f first string?))
                    (do (print (first args-f) " ")
                        (println)
                        (apply print-fn (rest args-f)))
                    (do (when pretty? (println))
                        (apply print-fn args-f)))
                (when (= pr-type :macro-expand) (print " */\n")))]
        (print out-str)
        (when (:log? opts)
          (swap! quantum.core.log/log conj
            (LogEntry.
              "TIMESTAMP" #_(time/now)
              pr-type
              curr-fn
              out-str)))
        nil)))

#?(:clj
(defmacro pr [pr-type & args]
  `(pr* true  false println ~pr-type (delay (list ~@args)) nil)))

#?(:clj
(defmacro pr-no-trace [pr-type & args]
  `(pr* false false println ~pr-type (delay (list ~@args)) nil)))

#?(:clj
(defmacro pr-opts [pr-type opts & args]
  `(pr* false false println ~pr-type (delay (list ~@args)) ~opts)))

#?(:clj
(defmacro ppr [pr-type & args]
  `(pr* true  true  pr/!    ~pr-type (delay (list ~@args)) nil)))

#?(:clj
(defmacro ppr-hints [pr-type & args]
  `(pr* true true  pr/pprint-hints ~pr-type (delay (list ~@args)) nil)))