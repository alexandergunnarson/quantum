(ns quanta.library.log
  (:refer-clojure :exclude [pr]))
(require '[quanta.library.ns :as ns])
(ns/require-all *ns* :lib :clj)

(def ^:dynamic *prs* (atom #{:alert}))
(def log (atom []))
(defrecord LogEntry
  [^DateTime  time-stamp
   ^Keyword   type
   ^Namespace ns-source
   ^String    message])
(defmacro pr
  "Prints to |System/out| if the print alert type @pr-type
   is in the set of enabled print alert types, |*prs*|.

   Logs the printed result to the global log |log|."
  [pr-type & args]
  `(let [ns-0# ~*ns*] 
    (binding [*ns* ns-0#]
      (when (in? ~pr-type @quanta.library.log/*prs*)
        (println ~@args))
      (swap! quanta.library.log/log conj
        (LogEntry.
          (time/now) ~pr-type ns-0# (str ~@args)))
      nil)))