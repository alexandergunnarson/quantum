(ns quantum.untyped.core.meta.debug
  (:require
#?@(:clj
   [[debugger.core]])
    [clojure.string            :as str]
    [fipp.edn]
    [taoensso.timbre]
    [quantum.untyped.core.core :as ucore]))

(ucore/log-this-ns)

;; ===== Breakpointing ===== ;;

(defonce *breakpoint-types (atom #{:user}))

#?(:clj
(defmacro break
  "Defines a breakpoint.
   An alternate implementation is from the Joy of Clojure, 2nd ed.,
   but that seems not to work as well as this one."
  [break-enable-sym]
  `(when (contains? @*breakpoint-types ~break-enable-sym)
     (debugger.core/break nil))))

;; ===== Error tracing ===== ;;

(defn trace
  "Print to *out* a pretty stack trace for a (parsed) exception, by default *e."
  [& [e]]
  (taoensso.timbre/error (or e *e)))

#?(:clj
(def default-exception-handler
  (reify
    java.lang.Thread$UncaughtExceptionHandler
    (^void uncaughtException [_ ^Thread t ^Throwable e]
      (println "Exception in thread" (str t ":"))
      (trace e)))))

#?(:clj
(defn print-pretty-exceptions!
  ([] (Thread/setDefaultUncaughtExceptionHandler default-exception-handler))
  ([^Thread t] (.setUncaughtExceptionHandler t default-exception-handler))))

(def stack-depth
  #?(:clj (if (>= (:minor *clojure-version*) 8)
              5
              2)
     :cljs 4)) ; TODO browser-dependent

(defn this-fn-name
  "Returns the current function name."
  ([] (this-fn-name 0))
  ([i]
    (let [st (identity
               #?(:clj  (-> (Thread/currentThread) .getStackTrace)
                  :cljs (-> (js/Error) .-stack
                            ; TODO Different browsers have different
                            ; implementations of stack traces
                            (str/split "\n    at "))))
          #?(:clj ^StackTraceElement elem
             :cljs elem)
             (nth st (min (- stack-depth i)
                          (-> st count dec)))]
      #?(:clj  (-> elem .getClassName clojure.repl/demunge)
         :cljs (-> elem str/trim (str/split " ") first cljs.core/demunge-str)))))
