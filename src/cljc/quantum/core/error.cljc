(ns
  ^{:doc "Error handling. Improved try/catch, and built-in error types for convenience's sake."
    :attribution "Alex Gunnarson"}
  quantum.core.error
  (:refer-clojure :exclude [assert])
  (:require-quantum [:core log map fn cbase])
  (:require [clojure.string                :as str  ]
            [quantum.core.collections.base :as cbase]
            [quantum.core.error.try-catch  :as tc   ]
            [clojure.string                :as str  ]
    #?(:clj [clj-stacktrace.repl           :as trace]))
  #?(:cljs 
  (:require-macros
            [quantum.core.collections.base :as cbase]
            [quantum.core.log              :as log  ])))

(defn generic-error [env]
  (if-cljs env 'js/Error 'Throwable))

(def error?  (partial instance? #?(:clj Throwable
                                   :cljs js/Error)))

(defrecord Err [type msg objs])

(defn ->err
  "Constructor for |Err|."
  ([type]          (if (map? type)
                       (map->Err type)
                       (Err. type nil nil)))
  ([type msg]      (Err. type msg nil))
  ([type msg objs] (Err. type msg objs)))

(defn ->ex
  "Creates an exception."
  ([type]          (ex-info (name type) (->err type)))
  ([type msg]      (ex-info msg  (->err type msg)))
  ([type msg objs] (ex-info msg  (->err type msg objs))))

#?(:clj 
(defn ex->map
  "Transforms an exception into a map with the keys :name, :message, :trace, and :ex-data, if applicable."
  [^Throwable e]
  (let [m {:name    (-> e class .getName)
           :message (-> e .getMessage)
           :trace   (-> e clj-stacktrace.repl/pst with-out-str clojure.string/split-lines)}]
    (if (instance? clojure.lang.ExceptionInfo e)
        (assoc m :ex-data (ex-data e))
        m))))

; NEED MORE MACRO EXPERIENCE TO DO THIS
; (defmacro catch-or
;   "Like /catch/, but catches multiple given exceptions in the same way."
;   {:in "[[[:status 401] {:keys [status]}]
;          [[:status 403] {:keys [status]}]
;          [[:status 500] {:keys [status]}]]
;         (handle-http-error status)"}
;   [exception-keys-pairs func]
;   (for [[exception-n# keys-n#] exception-keys-pairs]
;      `(catch exception-n# keys-n# func)))

; Set default exception handler
; (defn init []
;   (Thread/setDefaultUncaughtExceptionHandler
;     (reify Thread$UncaughtExceptionHandler
;       (uncaughtException [this thread throwable]
;         (logging/error throwable "Uncaught Exception:")
;         (.printStackTrace throwable)))))

#?(:clj
(defmacro throw-unless
  "Throws an exception with the given message @message if
   @expr evaluates to false.

   Specifically for use with :pre and :post conditions."
  {:attribution "Alex Gunnarson"}
  ([expr throw-content]
   `(if ~expr ~expr (throw ~throw-content)))
  ; This arity doesn't work yet.
  ([expr1 expr2 & exprs]
    `(core/doseq [[expr# throw-content#] (map/map-entry-seq ~exprs)]
       (throw-unless expr# throw-content#)))))

#?(:clj
(defmacro throw-when
  [expr throw-content]
  `(if-not ~expr ~expr (throw ~throw-content))))

#?(:clj
(defmacro with-catch
  {:usage '(->> 0 (/ 1) (with-catch (constantly -1)))}
  [handler try-val]
  `(try ~try-val
     (catch Throwable e# (~handler e#)))))

#?(:clj
(defmacro with-assert [expr pred err]
  `(if (~pred ~expr)
       ~expr
       (throw ~err))))

#?(:clj
(defmacro assert
  "Like |assert|, but takes a type"
  {:references ["https://github.com/google/guava/wiki/PreconditionsExplained"]
   :usage '(let [a 4]
             (assert (neg? (+ 1 3 a)) #{a}))}
  [expr & [syms type]]
  `(when-not ~expr
     (throw
       (->ex ~(or type :assertion-error)
             (str "Assertion not satisfied: " '~expr
                   "\n"
                   "Symbols: " (kmap ~@syms))
             (kmap ~@syms))))))

#?(:clj
(defmacro try-or 
  "An exception-handling version of the 'or' macro.
   Tries expressions in sequence until one produces a result that is neither false nor an exception.
   Useful for providing a default value in the case of errors."
  {:attribution "mikera.cljutils.error"}
  ([exp & alternatives]
     (let [c (if-cljs &env 'js/Error 'Throwable)]
       (if-let [as (seq alternatives)] 
         `(or (try ~exp (catch ~c t# (try-or ~@as))))
         exp)))))

#?(:clj
(defmacro suppress
  "Suppresses any errors thrown in the body.
  (suppress (error \"Error\")) => nil
  (suppress (error \"Error\") :error) => :error
  (suppress (error \"Error\")
            (fn [e]
              (.getMessage e))) => \"Error\""
  {:source "zcaudate/hara.common.error"}
  ([body]
    (let [c (if-cljs &env 'js/Error 'Throwable)]
     `(try ~body (catch ~c ~'t))))
  ([body catch-val]
    (let [c (if-cljs &env 'js/Error 'Throwable)]
     `(try ~body (catch ~c ~'t
                   (cond (fn? ~catch-val)
                         (~catch-val ~'t)
                         :else ~catch-val)))))))

#?(:clj
(defmacro assertf-> [f arg throw-obj]
  `(do (throw-unless (~f ~arg) (->ex nil ~throw-obj ['~f ~arg]))
       ~arg)))

#?(:clj
(defmacro assertf->> [f throw-obj arg]
  `(do (throw-unless (~f ~arg) (->ex nil ~throw-obj ['~f ~arg]))
       ~arg)))


#?(:clj 
(defmacro try-times [max-n sleep-millis & body]
  (let [c (if-cljs &env 'js/Error 'Throwable)]
    `(let [max-n#        ~max-n
           sleep-millis# ~sleep-millis]
       (loop [n# 0 error-n# nil]
         (if (> n# max-n#)
             (throw (->ex :max-tries-exceeded nil
                          {:tries n# :last-error error-n#}))
             (let [[error# result#]
                     (try [nil (do ~@body)]
                       (catch ~c e#
                         (quantum.core.thread.async/sleep sleep-millis#)
                         [e# nil]))]
               (if error#
                   (recur (inc n#) error#)
                   result#))))))))

#?(:clj (defalias try+   tc/try+  ))
#?(:clj (defalias throw+ tc/throw+))