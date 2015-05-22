(ns
  ^{:doc "Simple logging capabilities. Keeps a global log, has a status channel,
          prints only if the level is enabled, etc.

          By no means a full-fledged logging system, but useful nonetheless."
    :attribution "Alex Gunnarson"}
  quantum.core.log
  (:refer-clojure :exclude [pr])
  (:require-quantum [ns async fn])
  (:require
    #?(:clj  [clj-time.core  :as time]
       :cljs [cljs-time.core :as time])))

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
  `(let []
       (when (contains?
               @quantum.core.log/*prs* ~pr-type)
         (when (= ~pr-type :warn) (print "WARNING: "))
         (~print-fn ~@args)
         (swap! quantum.core.log/log conj
           (LogEntry.
             #?(:clj (time/now) :cljs (js/Date.)) ; TODO fix
             ~pr-type *ns* (str ~@args))))
       nil)))

#?(:clj
(defmacro pr  [pr-type & args] `(pr* println ~pr-type ~@args)))
#?(:clj
(defmacro ppr [pr-type & [arg & rest-args]]
  `(when (contains? @quantum.core.log/*prs* ~pr-type)
     (if (-> ~arg string?)
          (do (print ~arg " ")
              (pr* quantum.core.print/! ~pr-type ~@rest-args))
          (pr* quantum.core.print/! ~pr-type ~arg ~@rest-args)))))

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