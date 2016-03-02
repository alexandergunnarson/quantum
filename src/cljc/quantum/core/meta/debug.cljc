(ns
  ^{:doc "Useful debug utils. Especially |trace|, |break|, |try-times|, etc."
    :attribution "Alex Gunnarson"}
  quantum.core.meta.debug
  (:require-quantum [:core])
  (:require [clojure.string     :as str  ]
    #?(:clj [clojure.stacktrace :as trace])
    #?(:clj [clj-stacktrace.repl         ])
    #?(:clj [clojure.repl                ])
    #?(:clj [debugger.core               ])))
 
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

; TODO port to CLJS
#?(:clj (defalias trace clj-stacktrace.repl/pst))

#?(:clj 
(defn cause-trace
  "Return an Exception's cause trace as an array of lines"
  {:attribution "flatland.useful.exception"}
  [exception]
  (->> exception
       trace/print-cause-trace
       with-out-str
       str/split-lines
       (map str/trim))))

(defn this-fn-name
  "Returns the current function name."
  ([] (this-fn-name :curr))
  ([k]
    (let [st (identity
               #?(:clj  (-> (Thread/currentThread) .getStackTrace)
                  :cljs (-> (js/Error) .-stack
                            ; TODO Different browsers have different
                            ; implementations of stack traces
                            (str/split "\n    at "))))
          #?(:clj ^StackTraceElement elem
             :cljs elem)
             (condp = k
               :curr (nth st (min #?(:clj 2 :cljs 4)
                                 (-> st count dec)))
               :prev (nth st (min #?(:clj 3 :cljs 5)
                                 (-> st count dec)))
               (throw (#?(:clj  Exception.
                          :cljs js/Error.) "Unrecognized key.")))]
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

#?(:clj 
(defmacro rescue
  "Evaluate form, returning error-form on any Exception."
  ^{:attribution "flatland.useful.exception"}
  [form error-form]
  `(try ~form (catch Exception e# ~error-form))))
