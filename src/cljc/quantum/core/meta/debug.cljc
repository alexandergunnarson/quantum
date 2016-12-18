(ns
  ^{:doc "Useful debug utils. Especially |trace|, |break|, |try-times|, etc."
    :attribution "Alex Gunnarson"}
  quantum.core.meta.debug
           (:require [clojure.string           :as str  ]
             #?(:clj [clojure.stacktrace                ])
             #?(:clj [clojure.repl                      ])
             #?(:clj [debugger.core                     ])
             #?(:clj [clj-stacktrace.repl      :as trace])
             #?(:clj [fipp.edn                          ])
                     [quantum.core.core        :as qcore]
                     [quantum.core.vars        :as var
                       :refer [#?(:clj defalias)]       ])
  #?(:cljs (:require-macros
                     [quantum.core.vars        :as var
                       :refer [defalias]                ])))

; ===== BREAKPOINTS =====

(defonce breakpoint-types (atom #{:user}))

#?(:clj
(defmacro break
  "Defines a breakpoint.
   An alternate implementation is from the Joy of Clojure, 2nd ed.,
   but that seems not to work as well as this one."
  [break-enable-sym]
  `(when (contains? @breakpoint-types ~break-enable-sym)
     (debugger.core/break nil))))

; (defn break->>
;   "|break| for use with threading macros."
;   ([threading-macro-object]
;    (break)
;    threading-macro-object))

; ===== ERROR TRACING =====

#?(:clj (defonce pretty-printer (atom fipp.edn/pprint)))

#?(:clj
(defn trace-on [on color? e]
  "Prints to the given Writer on a pretty stack trace for e which can be an exception
  or already parsed exception (clj-stacktrace.core/parse-exception).
  ANSI colored if color? is true."
  {:modified-from 'clj-stacktrace.repl/pst-on}
  (let [exec (if (instance? Throwable e)
                 (clj-stacktrace.core/parse-exception e)
                 e)
        source-width (trace/find-source-width exec)]
    (@#'trace/pst-class-on on color? (:class exec))
    (@#'trace/pst-message-on on color? (:message exec))
    (when (instance? clojure.lang.ExceptionInfo e)
      (println "Data:")
      (@pretty-printer (ex-data e)))
    (println "Trace:")
    (@#'trace/pst-elems-on on color? (:trace-elems exec) source-width)
    (if-let [cause (:cause exec)]
      (@#'trace/pst-cause-on on color? cause source-width)))))

; TODO port to CLJS
#?(:clj
(defn trace
  "Print to *out* a pretty stack trace for a (parsed) exception, by default *e."
  {:from 'clj-stacktrace.repl/pst}
  [& [e]]
  (trace-on *out* false (or e *e))))

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

; (defmacro try-or
;   "An exception-handling version of the 'or' macro.
;    Trys expressions in sequence until one produces a result that is neither false nor an exception.
;    Useful for providing a default value in the case of errors."
;   ^{:attribution "mikera.cljutils.error"}
;   ([exp & alternatives]
;      (if-let [as (seq alternatives)]
;        `(or (try ~exp (catch Throwable t# (try-or ~@as))))
;        exp)))
