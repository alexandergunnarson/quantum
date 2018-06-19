(ns quantum.untyped.core.meta.debug
  (:require
#?@(:clj
   [[debugger.core]])
    [clojure.string            :as str]
    [fipp.edn]
    [taoensso.timbre]
    [quantum.untyped.core.core :as ucore]
    [quantum.untyped.core.log  :as ulog]))

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

#?(:clj
(def default-exception-handler
  (reify java.lang.Thread$UncaughtExceptionHandler
    (^void uncaughtException [_ ^Thread t ^Throwable e] (ulog/error! e)))))

#?(:clj
(defn print-pretty-exceptions!
  ([] (Thread/setDefaultUncaughtExceptionHandler default-exception-handler))
  ([^Thread t] (.setUncaughtExceptionHandler t default-exception-handler))))

(def stack-depth ulog/stack-depth)
(def this-fn-name ulog/this-fn-name)
