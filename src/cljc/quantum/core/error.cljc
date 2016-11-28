(ns
  ^{:doc "Error handling. Improved try/catch, and built-in error types for convenience's sake."
    :attribution "Alex Gunnarson"}
  quantum.core.error
  (:require
    [clojure.string                :as str]
    [quantum.core.collections.base :as cbase
      :refer [kmap]]
    [quantum.core.data.map         :as map]
    [slingshot.slingshot           :as try]
    [quantum.core.macros.core      :as cmacros
      :refer [if-cljs]]
    [quantum.core.log              :as log
      :include-macros true]
    [quantum.core.vars             :as var
      :refer [defalias]])
  (:require-macros
    [quantum.core.error            :as self]))

(defn generic-error [env]
  (if-cljs env 'js/Error 'Throwable))

#?(:clj
(defmacro catch-all
  "Cross-platform try/catch/finally."
  {:from 'taoensso.truss.impl/catching
   :see  ["http://dev.clojure.org/jira/browse/CLJ-1293"]}
  ; TODO js/Error instead of :default as temp workaround for http://goo.gl/UW7773
  ([try-expr                     ] `(catching ~try-expr ~'_ nil))
  ([try-expr error-sym catch-expr]
   `(try ~try-expr (catch ~(generic-error &env) ~error-sym ~catch-expr)))
  ([try-expr error-sym catch-expr finally-expr]
   `(try ~try-expr (catch ~(generic-error &env) ~error-sym ~catch-expr) (finally ~finally-expr)))))


(def error?  (partial instance? #?(:clj  Throwable
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

(defn ex->map
  "Transforms an exception into a map with the keys :name, :message, :trace, and :ex-data, if applicable."
  [e]
  #?(:clj (let [^Throwable e e
                m {:name    (-> e class .getName)
                   :message (-> e .getMessage)
                   :trace   (-> e clj-stacktrace.repl/pst with-out-str clojure.string/split-lines)}]
            (if (instance? clojure.lang.ExceptionInfo e)
                (assoc m :ex-data (ex-data e))
                m))
     :cljs (if (instance? js/Error e)
               (let [m {:name nil
                        :message (.-message e)
                        :trace   (.-trace   e)}]
                 (if (instance? cljs.core.ExceptionInfo e)
                     (assoc m :ex-data (ex-data e))
                     m))
               {:ex-data e})))

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
  "Throws an exception with the given content @throw-content if
   @expr evaluates to false.

   Specifically for use with :pre and :post conditions."
  {:attribution "Alex Gunnarson"}
  ([expr throw-content]
   `(let [expr# ~expr]
      (if expr# expr# (throw ~throw-content))))
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
  `(catch-all ~try-val e# (~handler e#))))

#?(:clj
(defmacro with-assert [expr pred err]
  `(if (~pred ~expr)
       ~expr
       (throw ~err))))

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
  (suppress (error \"Error\")) => <Exception>
  (suppress (error \"Error\") :error) => :error
  (suppress (error \"Error\")
            (fn [e]
              (.getMessage e))) => \"Error\""
  ([body]
    (let [c (if-cljs &env :default 'Throwable)]
     `(try ~body (catch ~c ~'t ~'t))))
  ([body catch-val]
    (let [c (if-cljs &env :default 'Throwable)]
     `(try ~body (catch ~c ~'t
                   (let [catch-val# ~catch-val]
                     (cond (fn? catch-val#)
                           (catch-val# ~'t)
                           :else catch-val#))))))))

#?(:clj
(defmacro ignore [& body]
  (let [c (if-cljs &env :default 'Throwable)]
    `(try ~@body (catch ~c _# nil)))))

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
                         (quantum.core.async/sleep sleep-millis#)
                         [e# nil]))]
               (if error#
                   (recur (inc n#) error#)
                   result#))))))))

(defn tries-exceeded [tries & [state]]
  (throw (->ex :max-tries-exceeded nil {:tries tries :state state})))

#?(:clj (defalias try+   try/try+  ))
#?(:clj (defalias throw+ try/throw+))

#?(:clj (defmacro warn! [e] `(log/ppr :warn (ex->map ~e))))

(defn todo ([]    (throw (->ex :todo "This feature has not yet been implemented.")))
           ([msg] (throw (->ex :todo (str "This feature has not yet been implemented: " msg)))))
(defalias TODO todo)
