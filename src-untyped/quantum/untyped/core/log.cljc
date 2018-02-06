(ns quantum.untyped.core.log
  (:refer-clojure :exclude
    [pr seqable?])
  (:require
    [com.stuartsierra.component         :as component]
    [quantum.untyped.core.core          :as ucore]
    [quantum.untyped.core.error         :as uerr]
    [quantum.untyped.core.form
      :refer [$]]
    [quantum.untyped.core.form.evaluate
      :refer [compile-if]]
    [quantum.untyped.core.form.generate :as ufgen]
    [quantum.untyped.core.meta.debug    :as udebug]
    [quantum.untyped.core.print         :as upr]
    [quantum.untyped.core.qualify       :as uqual]
    [quantum.untyped.core.type.predicates
      :refer [seqable?]]
    [quantum.untyped.core.vars
      :refer [defalias]])
#?(:cljs
  (:require-macros
    [quantum.untyped.core.log           :as self
      :refer [-gen-from-levels with-log-errors]])))

(ucore/log-this-ns)

;; TODO maybe use Timbre?

;; ===== Data ===== ;;

(defalias *levels ucore/*log-levels)
(defalias *outs   ucore/*outs)

(defonce *log (atom []))

(defrecord LogEntry
  [time-stamp ; ^DateTime
   type       ; ^Keyword
   ns-source  ; ^Namespace
   message])  ; ^String

;; ===== Log levels ===== ;;

(defn disable!
  ([pr-type #_t/keyword?] (swap! *levels assoc pr-type false))
  ([pr-type & pr-types]
    (doseq [pr-type-n (conj pr-types pr-type)]
      (disable! pr-type-n))))

(defn enable!
  ([pr-type #_t/keyword?] (swap! *levels assoc pr-type true))
  ([pr-type & pr-types]
    (doseq [pr-type-n (conj pr-types pr-type)]
      (enable! pr-type-n))))

;; ===== Actual printing and logging ===== ;;

(defn pr*
  "Prints to |System/out| if the print alert type @pr-type
   is in the set of enabled print alert types, `*levels`.

   Logs the printed result to the global log `*log`."
  {:attribution "alexandergunnarson"}
  [trace? pretty? print-fn pr-type args opts]
    (let [trace?  (or (:trace?  opts) trace? )
          pretty? (or (:pretty? opts) pretty?)
          stack   (or (:stack   opts) -1     )
          timestamp? (:timestamp? opts)
          curr-fn (when trace? (udebug/this-fn-name stack))
          env-type-str
            (when (get @*levels :env)
              (str (name ucore/lang) " »"))
          out-str
            (with-out-str
              (when (= pr-type :macro-expand) (print "\n/* "))
              #?(:clj
                (when timestamp?
                  (let [timestamp
                         (compile-if (do (Class/forName "java.time.format.DateTimeFormatter")
                                         (Class/forName "java.time.LocalDateTime"))
                           (.format
                             (java.time.format.DateTimeFormatter/ofPattern
                               "MM-dd-yyyy HH:mm::ss")
                             (java.time.LocalDateTime/now))
                           nil)] ; TODO JDK < 8 timestamp
                    (print (str "[" timestamp "] ")))))
              (when trace?
                (print "[")
                (print #?(:clj (.getName (Thread/currentThread)))
                       ":"
                       curr-fn "»"
                       (str pr-type "] ")))
              (if (and pretty? (-> args first string?))
                  (do (print (first args) " ")
                      (println)
                      (apply print-fn (rest args)))
                  (do (when pretty? (println))
                      (apply print-fn args)))
              (when (= pr-type :macro-expand) (print " */\n")))]

#?(:clj  (doseq [out (@ucore/*outs)] (binding [*out* out] (print out-str) (flush)))
   :cljs (let [console-print-fn
                (or (aget js/console (name pr-type)) println)]
           (console-print-fn out-str)))
        (when (:log? opts)
          (swap! quantum.untyped.core.log/*log conj
            (LogEntry.
              "TIMESTAMP" #_(time/now)
              pr-type
              curr-fn
              out-str))))
    args)

;; ===== Varieties of logging ===== ;;

#?(:clj
(defmacro -pr-base [pr-type opts args print-meta? trace? pretty? print-fn]
  `(let [pr-type# ~pr-type]
     (if (get @*levels pr-type#)
         (binding [*print-meta* ~print-meta?]
           (pr* ~trace? ~pretty? ~print-fn pr-type# [~@args] ~opts))
         true))))

#?(:clj
(defmacro -def-with-always [sym & args]
  (let [args-sym (gensym "args")
        macro-sym (uqual/qualify sym)]
    `(do (defmacro ~sym ~@args)
         (defmacro ~(symbol (str (name sym) "!")) [& ~args-sym]
          `(~'~macro-sym :always ~@~args-sym))))))

#?(:clj (-def-with-always pr          [pr-type      & args] `(-pr-base ~pr-type nil   ~args *print-meta* true  false println)))
#?(:clj (-def-with-always ppr         [pr-type      & args] `(-pr-base ~pr-type nil   ~args *print-meta* true  true  upr/ppr)))
#?(:clj (-def-with-always pr-no-trace [pr-type      & args] `(-pr-base ~pr-type nil   ~args *print-meta* false false println)))
#?(:clj (-def-with-always pr-opts     [pr-type opts & args] `(-pr-base ~pr-type ~opts ~args *print-meta* true  false println)))
#?(:clj (-def-with-always ppr-opts    [pr-type opts & args] `(-pr-base ~pr-type ~opts ~args *print-meta* true  false upr/ppr)))
#?(:clj (-def-with-always ppr-meta    [pr-type      & args] `(-pr-base ~pr-type nil   ~args true         true  true  upr/ppr)))
#?(:clj (defalias ppr-hints  ppr-meta )) ; TODO this is not right
#?(:clj (defalias ppr-hints! ppr-meta!)) ; TODO this is not right

#?(:clj
(-def-with-always prl
  "'Print labeled'.
   Puts each x in `xs` as vals in a map.
   The keys in the map are the quoted vals. Then prints the map."
  [level & xs]
  `(let [level# ~level]
     (ppr level# ~(->> xs (map #(vector (list 'quote %) %)) (into {}))))))

#?(:clj
(-def-with-always prlm
  "'Print labeled, with meta'."
  [level & xs]
  `(binding [*print-meta* true] (prl ~level ~@xs))))

;; ===== Level-specific macros ===== ;;

#?(:clj
(defmacro -gen-from-levels [& levels #_(t/seq-of t/keyword?)]
  `(do ~@(for [level levels]
           `(defmacro ~(-> level name symbol) [& ~'args]
              `(ppr ~~level ~@'args))))))

(-gen-from-levels :always :error :warn)

#?(:clj (defmacro warn!  [e] `(ppr :warn  (uerr/>err ~e))))
#?(:clj (defmacro error! [e] `(ppr :error (uerr/>err ~e))))

;; ===== `with` and wrap- macros ===== ;;

#?(:clj
(defmacro with-prl
  "For thread-last ->> usage"
  ([expr] `(with-prl :always ~expr))
  ([level expr]
  `(let [expr# ~expr]
     (do (ppr ~level {'~expr expr#}) expr#)))))

#?(:clj
(defmacro with-log-errors [k & args]
  `(uerr/catch-all (do ~@args) e# (ppr ~k e#))))

(defn wrap-log-errors [k f] ; TODO find a cleaner way to do this
  (fn ([]                       (with-log-errors k (f)                           ))
      ([a0]                     (with-log-errors k (f a0)                        ))
      ([a0 a1]                  (with-log-errors k (f a0 a1)                     ))
      ([a0 a1 a2]               (with-log-errors k (f a0 a1 a2)                  ))
      ([a0 a1 a2 a3]            (with-log-errors k (f a0 a1 a2 a3)               ))
      ([a0 a1 a2 a3 a4]         (with-log-errors k (f a0 a1 a2 a3 a4)            ))
      ([a0 a1 a2 a3 a4 a5]      (with-log-errors k (f a0 a1 a2 a3 a4 a5)         ))
      ([a0 a1 a2 a3 a4 a5 & as] (with-log-errors k (apply f a0 a1 a2 a3 a4 a5 as)))))

;; ===== Componentization ===== ;;

(defrecord LogInitializer
  [levels]
  component/Lifecycle
  (start [this]
    (apply enable! (or levels #{:debug :warn}))
    this)
  (stop  [this]
    #_(apply disable! levels) ; we don't necessarily want this logic (?)
    this))

(defn >log-initializer [{:keys [levels] :as opts}]
  (when-not (seqable? levels)
    (throw (new #?(:clj Exception :cljs js/Error) "@levels is not seqable")))
  (LogInitializer. levels))

(swap! ucore/*registered-components assoc :quantum.core.log/log >log-initializer)

;; ===== Miscellaneous ===== ;;

#?(:clj (defalias this-ns ucore/log-this-ns))
