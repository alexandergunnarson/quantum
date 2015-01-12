(ns quantum.core.log
  (:refer-clojure :exclude [pr]))
(require
  '[quantum.core.ns     :as ns]
  '[quantum.core.time.core]
  '[clojure.core.async  :as async :refer [go <! >! >!! <!! alts! chan]]
  '[quantum.core.string :as str])
(ns/require-all *ns* :clj)

; alert, inspect
(def ^:dynamic *prs* (atom #{:warn :user}))
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
  [^DateTime  time-stamp
   ^Keyword   type
   ^Namespace ns-source
   ^String    message])
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
            (clj-time.core/now) ~pr-type ns-0# (str ~@args))))
      nil)))






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
(defn ^String curr-status
  "Updates the system status with the provided string @s."
  [^String s]
  (go (<! @statuses s)))