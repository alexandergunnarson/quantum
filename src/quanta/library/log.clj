(ns quanta.library.log
  (:refer-clojure :exclude [pr]))
(require '[quanta.library.ns :as ns])
(ns/require-all *ns* :clj)

; alert, inspect
(def ^:dynamic *prs* (atom #{:warn :user}))
(def log (atom []))
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
             @quanta.library.log/*prs* ~pr-type)
        (println
          (if (= ~pr-type :warn)
              "WARNING:"
              "") ; This indents slightly so the warnings stand out
          ~@args)
        (swap! quanta.library.log/log conj
          (LogEntry.
            (clj-time.core/now) ~pr-type ns-0# (str ~@args))))
      nil)))