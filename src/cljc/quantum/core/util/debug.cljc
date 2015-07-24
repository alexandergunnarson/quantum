(ns
  ^{:doc "Useful debug utils. Especially |trace|, |break|, |try-times|, etc."
    :attribution "Alex Gunnarson"}
  quantum.core.util.debug
  (:require-quantum [ns err log logic fn pr])
  #?(:clj
    (:require
      [clojure.pprint     :as pprint                            ]
      [clojure.stacktrace :as trace   :refer [print-cause-trace]]
      [clojure.string     :as clj-str :refer [split-lines trim] ])))
 ; (:import mikera.cljutils.Error)
; (require '[taoensso.encore :as lib+ :refer
;   [throwable? exception?]])

; No matching method clojure.main/repl-read
#?(:clj 
(defn readr
  {:attribution "The Joy of Clojure, 2nd ed."}
  [prompt exit-code]
  (let [input (clojure.main/repl-read prompt exit-code)]
    (if (= input :next) ; perhaps non-namespace qualified is a bad idea
        exit-code
        input))))

#?(:clj 
(defn debug
  "A debug REPL, courtesy of The Joy of Clojure.

   Type (debug) to start, and :next to go to the next breakpoint.
   Apparently there is no 'stop execution'..."
  []
  (readr #(print "invisible=> ") ::exit))) ; perhaps non-namespace qualified is a bad idea

#?(:clj
(defmacro report [source- & args]
  `(try+ ~@args
     (catch Object e#
       (log/pr-opts :debug #{:thread?}
         "FROM SOURCE" ~source- "THIS IS EXCEPTION" e#)
       (throw+)))))

#?(:clj 
(defmacro break
  "Stops execution and starts a debug REPL. When the debug REPL is
   terminated by the ::exit namespaced-qualified keyword, execution
   returns to just after the breakpoint."
  {:attribution  "The Joy of Clojure, 2nd ed."
   :contributors ["Alex Gunnarson"]}
  ([]
    `(clojure.main/repl
      :prompt #(print "debug=> ")
      :read readr
      :eval (partial quantum.core.ns/c-eval
              (quantum.core.ns/context))))
  ([& args]
    `(do (println ~@args)
         (break)))))
; (defn break->>
;   "|break| for use with threading macros."
;   ([threading-macro-object]
;    (break)
;    threading-macro-object))

; (defmacro error
;   "Throws an error with the provided message(s). This is a macro in order to try and ensure the 
;    stack trace reports the error at the correct source line number."
;   ^{:attribution "mikera.cljutils.error"}
;   ([& vals]
;     `(throw (mikera.cljutils.Error. (str ~@vals)))))
; (defmacro error?
;   "Returns true if executing body throws an error, false otherwise."
;   ^{:attribution "mikera.cljutils.error"}
;   ([& body]
;     `(try 
;        ~@body
;        false
;        (catch Throwable t# 
;          true)))) 
; (defmacro TODO
;   "Throws a TODO error. This ia a useful macro as it is easy to search for in source code, while
;    also throwing an error at runtime if encountered."
;   ^{:attribution "mikera.cljutils.error"}
;   ([]
;     `(error "TODO: Not yet implemented")))
; (defmacro valid 
;   "Asserts that an expression is true, throws an error otherwise."
;   ^{:attribution "mikera.cljutils.error"}
;   ([body & msgs]
;     `(or ~body
;        (error ~@msgs))))
; (defmacro try-or 
;   "An exception-handling version of the 'or' macro.
;    Trys expressions in sequence until one produces a result that is neither false nor an exception.
;    Useful for providing a default value in the case of errors."
;   ^{:attribution "mikera.cljutils.error"}
;   ([exp & alternatives]
;      (if-let [as (seq alternatives)] 
;        `(or (try ~exp (catch Throwable t# (try-or ~@as))))
;        exp)))

; From flatland.useful.debug
#?(:clj 
(letfn [(interrogate-form [list-head form]
          `(let [display# (fn [val#]
                            (let [form# (with-out-str
                                          (pprint/with-pprint-dispatch pprint/code-dispatch
                                            (! '~form)))
                                  val# (with-out-str (! val#))]
                              (~@list-head
                               (if (every? (partial > pprint/*print-miser-width*)
                                           [(count form#) (count val#)])
                                 (str (subs form# 0 (dec (count form#))) " is " val#)
                                 (str form# "--------- is ---------\n" val#)))))]
             (try (doto ~form display#)
                  (catch Throwable t#
                    (display# {:thrown t#
                               :trace (with-out-str
                                        (print-cause-trace t#))})
                    (throw t#)))))]
  (defmacro ?
    "A useful debugging tool when you can't figure out what's going on:
  wrap a form with ?, and the form will be printed alongside
  its result. The result will still be passed along."
  ^{:attribution "flatland.useful.debug"}
    [val]
    (interrogate-form `(print) val))
; From flatland.useful.debug
  (defmacro
    ^{:dont-test "Complicated to test, and should work if ? does"
      :attribution "flatland.useful.debug"}
    ?!
    ([val] `(?! "/tmp/spit" ~val))
    ([file val]
       (interrogate-form `(#(spit ~file % :append true)) val)))))

#?(:clj 
(defmacro rescue
  "Evaluate form, returning error-form on any Exception."
  ^{:attribution "flatland.useful.exception"}
  [form error-form]
  `(try ~form (catch Exception e# ~error-form))))
; From flatland.useful.exception

#?(:clj 
(defn cause-trace
  "Return an Exception's cause trace as an array of lines"
  {:attribution "flatland.useful.exception"}
  [exception]
  (map trim (split-lines (with-out-str (print-cause-trace exception))))))

#?(:clj (def trace #(cause-trace *e)))
; From flatland.useful.exception

#?(:clj 
(defn exception-map
  "Return a map with the keys: :name, :message, and :trace. :trace is the cause trace as an array of lines "
  ^{:attribution "flatland.useful.exception"}
  [exception]
  {:name    (.getName (class exception))
   :message (.getMessage exception)
   :trace   (cause-trace exception)}))

; The following requires flatland.useful.fn:
; ; From flatland.useful.utils
; (defn fail
;   "Raise an exception. Takes an exception or a string with format args."
;   ([exception]
;      (throw (fix exception string? #(Exception. ^String %))))
;   ([string & args]
;      (fail (apply format string args))))
; ; From flatland.useful.utils
; (defmacro verify
;   "Raise exception unless test returns true."
;   [test & args]
;   `(when-not ~test
;      (fail ~@args)))

#?(:clj 
(defn try-times-naive [max-tries try-expr-0 catch-expr-0]
  (loop [tries-n 0 try-expr-f nil]
    (if (or (and (nnil? try-expr-f) ; not the initial value of nil
                 (not= try-expr-f catch-expr-0)) ; if the exception is worked out
            (> tries-n max-tries))
        try-expr-f
        (let [try-expr-n 
               (try (try-expr-0)
                    (catch Exception e
                      (doseq [expr catch-expr-0]
                        (expr e))))]
          (recur (inc tries-n) try-expr-n))))))
