(ns
  ^{:doc "Simple logging capabilities. Keeps a global log, has a status channel,
          prints only if the level is enabled, etc.

          By no means a full-fledged logging system, but useful nonetheless."
    :attribution "Alex Gunnarson"}
  quantum.core.log
  (:refer-clojure :exclude [pr])
  (:require
    [#?(:clj clojure.core.async :cljs cljs.core.async) :as async :refer
      #?(:clj  [<! >! alts! close! chan >!! <!! thread go go-loop]
         :cljs [<! >! alts! close! chan])]
    [quantum.core.print :as pr]
    [clj-time.core :as time])

  #?@(:clj
      [(:import
        clojure.core.Vec
        java.util.ArrayList clojure.lang.Keyword
        (quantum.core.ns
          Nil Bool Num ExactNum Int Decimal Key Set
                 ArrList TreeMap LSeq Regex Editable Transient Queue Map))
       (:gen-class)]))

  ; #?(:cljs (:require-macros
  ;   [cljs.core.async :refer [go]]))

(def ^:dynamic *prs* (atom #{:warn :user})) ; alert, inspect, debug
(def log  (atom []))
(def vars (atom {}))
(defn cache! [k v]
  (swap! vars assoc k v))
(def statuses (atom (chan)))
(def errors (atom []))
(defn error [throw-context]
  (swap! errors conj
    (update throw-context :stack-trace vec)))
(defrecord LogEntry
  [time-stamp ; ^DateTime  
   type       ; ^Keyword   
   ns-source  ; ^Namespace 
   message])  ; ^String    
(defn disable!
  ([^Keyword pr-type]
    (swap! *prs* disj pr-type))
  ([^Keyword pr-type & pr-types]
    (apply swap! *prs* disj pr-type pr-types)))
(defn enable!
  ([^Keyword pr-type]
    (swap! *prs* conj pr-type))
  ([^Keyword pr-type & pr-types]
    (apply swap! *prs* conj pr-type pr-types)))

#?(:clj
(defmacro pr*
  "Prints to |System/out| if the print alert type @pr-type
   is in the set of enabled print alert types, |*prs*|.

   Logs the printed result to the global log |log|."
  {:attribution "Alex Gunnarson"}
  [print-fn pr-type & args]
  `(let [ns-0# ~*ns*] 
     (binding [*ns* ns-0#]
       (when (contains?
               @quantum.core.log/*prs* ~pr-type)
         (~print-fn
           (if (= ~pr-type :warn)
               "WARNING:"
               "") ; This indents slightly so the warnings stand out
           ~@args)
         (swap! quantum.core.log/log conj
           (LogEntry.
             #?(:clj (time/now) :cljs (js/Date.)) ; TODO fix
             ~pr-type ns-0# (str ~@args))))
       nil))))

(defmacro pr  [pr-type & args] `(pr* println ~pr-type ~@args))
(defmacro ppr [pr-type & args] `(pr* pr/!    ~pr-type ~@args))

#?(:clj
(defn status
  "Updates the system status with the provided string @s."
  {:attribution "Alex Gunnarson"}
  ([s]
    (pr :user s)
    (let [statuses-chan @statuses]
      (go (>! @statuses s))
      (reset! statuses statuses-chan))
    nil)
  ([s & strs]
    (status (apply str s strs))))) ; TODO should be str/sp not str

#?(:clj
(defn curr-status
  "Updates the system status with the provided string @s."
  [s]
  (go (<! @statuses s))))