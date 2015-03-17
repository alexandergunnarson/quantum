(ns quantum.core.log
  (:refer-clojure :exclude [pr])
  (:require
    [quantum.core.ns :as ns :refer
      #+clj [alias-ns defalias]
      #+cljs [Exception IllegalArgumentException
              Nil Bool Num ExactNum Int Decimal Key Vec Set
              ArrList TreeMap LSeq Regex Editable Transient Queue Map]]
    [quantum.core.time.core :as time]
    [quantum.core.string :as str]
    [#+clj clojure.core.async #+cljs cljs.core.async :as async
      #+clj  :refer
      #+clj  [go <! >! alts! chan]
      #+cljs :refer
      #+cljs [<! >! alts! chan]])
  #+cljs
  (:require-macros
    [cljs.core.async.macros :refer [go-loop go]])
  #+clj
  (:import
    clojure.core.Vec
    (clojure.lang Fn MapEntry Keyword)
    (quantum.core.ns
      Nil Bool Num ExactNum Int Decimal Key Set
             ArrList TreeMap LSeq Regex Editable Transient Queue Map))
  #+clj (:gen-class))

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

(defmacro pr
  "Prints to |System/out| if the print alert type @pr-type
   is in the set of enabled print alert types, |*prs*|.

   Logs the printed result to the global log |log|."
  [pr-type & args]
  `(let [ns-0# ~*ns*] 
    (binding [*ns* ns-0#]
      (when (contains?
             @quantum.core.log/*prs* ~pr-type)
        (println
          (if (= ~pr-type :warn)
              "WARNING:"
              "") ; This indents slightly so the warnings stand out
          ~@args)
        (swap! quantum.core.log/log conj
          (LogEntry.
            (time/now) ~pr-type ns-0# (str ~@args))))
      nil)))

#+clj
(defn status
  "Updates the system status with the provided string @s."
  ([^String s]
    (pr :user s)
    (let [statuses-chan @statuses]
      (go (>! @statuses s))
      (reset! statuses statuses-chan))
    nil)
  ([^String s & strs]
    (status (apply str/sp s strs))))

#+clj
(defn ^String curr-status
  "Updates the system status with the provided string @s."
  [^String s]
  (go (<! @statuses s)))