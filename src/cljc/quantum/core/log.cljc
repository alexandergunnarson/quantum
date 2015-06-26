(ns
  ^{:doc "Simple logging capabilities. Keeps a global log, has a status channel,
          prints only if the level is enabled, etc.

          By no means a full-fledged logging system, but useful nonetheless."
    :attribution "Alex Gunnarson"}
  quantum.core.log
  (:refer-clojure :exclude [pr])
  (:require-quantum [ns async fn pr])
  (:require
    #?(:clj  [clj-time.core  :as time]
       :cljs [cljs-time.core :as time])))

  ; #?(:cljs (:require-macros
  ;   [cljs.core.async :refer [go]]))

(defrecord LoggingLevels
  [warn user macro-expand debug trace env])


(defonce ^:dynamic *prs*
  (-> {:warn              true
       :user              true}
      map->LoggingLevels atom)) ; alert, inspect, debug
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
    (swap! *prs* assoc pr-type false))
  ([^Keyword pr-type & pr-types]
    (doseq [pr-type-n (conj pr-types pr-type)]
      (disable! pr-type-n))))

(defn enable!
  ([^Keyword pr-type]
    (swap! *prs* assoc pr-type true))
  ([^Keyword pr-type & pr-types]
    (doseq [pr-type-n (conj pr-types pr-type)]
      (enable! pr-type-n)))) 

(def env-type #?(:clj :clj :cljs :cljs))

(defn pr*
  "Prints to |System/out| if the print alert type @pr-type
   is in the set of enabled print alert types, |*prs*|.

   Logs the printed result to the global log |log|."
  {:attribution "Alex Gunnarson"}
  [trace? pretty? print-fn pr-type args]
  (when (or (get @*prs* pr-type)
            #?(:cljs (= pr-type :macro-expand)))
    (let [curr-fn (ns/this-fn-name :prev)
          args-f @args
          env-type-str
            (when (get @*prs* :env)
              (str (name env-type) " »"))
          out-str
            (with-out-str
              (when (= pr-type :macro-expand) (print "\n/* "))
              (when trace?
                (print (str "[" env-type-str)
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
      (swap! quantum.core.log/log conj
        (LogEntry.
          (time/now)
          pr-type
          curr-fn
          out-str))
      nil)))

#?(:clj
(defmacro pr [pr-type & args]
  `(pr* true false println ~pr-type (delay (list ~@args)))))

#?(:clj
(defmacro pr-no-trace [pr-type & args]
  `(pr* false false println ~pr-type (delay (list ~@args)))))

#?(:clj
(defmacro ppr [pr-type & args]
  `(pr* true true  !       ~pr-type (delay (list ~@args)))))

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