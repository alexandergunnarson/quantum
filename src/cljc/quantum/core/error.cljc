(ns
  ^{:doc "Error handling. Improved try/catch, and built-in error types for convenience's sake."
    :attribution "alexandergunnarson"}
  quantum.core.error
  (:refer-clojure :exclude [assert])
  (:require
    [clojure.string                :as str]
    [slingshot.slingshot           :as try]
    [quantum.core.collections.base :as cbase
      :refer [kw-map]]
    [quantum.core.data.map         :as map]
    [quantum.core.fn
      :refer [fnl fn1 rcomp fn']]
    [quantum.core.macros.core      :as cmacros
      :refer [case-env case-env*]]
    [quantum.core.log              :as log]
    [quantum.core.vars             :as var
      :refer [defalias]])
  (:require-macros
    [quantum.core.error            :as self
      :refer [with-log-errors assert]]))

(def ^{:todo {0 "Finish up `conditions` fork"}} annotations nil)

(defn generic-error [env]
  (case-env* env :clj 'Throwable :cljs 'js/Error))

#?(:clj
(defmacro catch-all
  "Cross-platform try/catch/finally.

   Uses `js/Error` instead of `:default` as temporary workaround for http://goo.gl/UW7773."
  {:from 'taoensso.truss.impl/catching
   :see  ["http://dev.clojure.org/jira/browse/CLJ-1293"]}
  ([try-expr                     ] `(catch-all ~try-expr ~'_ nil))
  ([try-expr error-sym catch-expr]
   `(try ~try-expr (catch ~(generic-error &env) ~error-sym ~catch-expr)))
  ([try-expr error-sym catch-expr finally-expr]
   `(try ~try-expr (catch ~(generic-error &env) ~error-sym ~catch-expr) (finally ~finally-expr)))))


(def error? (fnl instance? #?(:clj Throwable :cljs js/Error)))
(def ex-info? (fnl instance? #?(:clj clojure.lang.ExceptionInfo :cljs cljs.core/ExceptionInfo)))

(defrecord Err [type msg objs])

(defn ->err
  "Constructor for |Err|."
  ([type]          (if (map? type)
                       (map->Err type)
                       (Err. type nil nil)))
  ([type msg]      (Err. type msg nil ))
  ([type msg objs] (Err. type msg objs)))

(defn ->ex
  "Creates an exception."
  ([type]          (ex-info (name type) (->err type type)))
  ([msg objs]      (ex-info (str msg)   (->err msg  msg objs)))
  ([type msg objs] (ex-info msg         (->err type msg objs))))

(def throw-ex (rcomp ->ex (fn1 throw)))

(defn ->ex-info
  ([objs]     (ex-info "Exception" objs))
  ([msg objs] (ex-info msg         objs)))

(def throw-info (rcomp ->ex-info (fn1 throw)))

(defn ex->map
  "Transforms an exception into a map with the keys :name, :message, :trace, and :ex-data, if applicable."
  [e]
  #?(:clj  (Throwable->map e)
     :cljs (do (assert (instance? js/Error e) {:e e})
               {:cause   nil
                :via     [{:type    nil
                           :message (.-message e)
                           :at      nil
                           :data    (when (instance? cljs.core.ExceptionInfo e)
                                      (ex-data e))}]
                :trace   (.-trace e)}))) ; TODO str->vec based on browser via goog.debug.*

#?(:clj
(defmacro throw-unless
  "Throws an exception with the given content @throw-content if
   @expr evaluates to false.

   Specifically for use with :pre and :post conditions."
  {:attribution "alexandergunnarson"}
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
  `(let [expr# ~expr]
     (if-not expr# expr# (throw ~throw-content)))))

#?(:clj
(defmacro with-catch
  {:usage '(->> 0 (/ 1) (with-catch (fn' -1)))}
  [handler try-val]
  `(catch-all ~try-val e# (~handler e#))))

#?(:clj
(defmacro assert
  "Like `assert` but never gets elided out."
  ([expr] `(assert ~expr nil))
  ([expr info]
   `(let [expr# ~expr]
      (if expr#
          expr#
          (throw (ex-info "Assertion failed" {:expr '~expr :info ~info})))))))

#?(:clj
(defmacro with-assert
  ([expr pred]
   `(with-assert ~expr ~pred (->ex "Assertion failed" '(~pred ~expr))))
  ([expr pred err]
  `(let [expr# ~expr]
     (if (-> expr# ~pred) expr# (throw ~err))))))

#?(:clj
(defmacro try-or
  "An exception-handling version of the 'or' macro.
   Tries expressions in sequence until one produces a result that is neither false nor an exception.
   Useful for providing a default value in the case of errors."
  {:attribution "mikera.cljutils.error"}
  ([exp & alternatives]
     (let [c (case-env :clj 'Throwable :cljs 'js/Error)]
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
    (let [c (case-env :clj 'Throwable :cljs :default)]
     `(try ~body (catch ~c ~'t ~'t))))
  ([body catch-val]
    (let [c (case-env :clj 'Throwable :cljs :default)]
     `(try ~body (catch ~c ~'t
                   (let [catch-val# ~catch-val]
                     (cond (fn? catch-val#)
                           (catch-val# ~'t)
                           :else catch-val#))))))))

#?(:clj
(defmacro ignore [& body]
  (let [c (case-env :clj 'Throwable :cljs :default)]
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
  (let [c (case-env :clj 'Throwable :cljs 'js/Error)]
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

(defn todo ([]    (throw (->ex :todo "This feature has not yet been implemented." nil)))
           ([msg] (throw (->ex :todo (str "This feature has not yet been implemented: " msg) nil))))
(defalias TODO todo)

#?(:clj
(defmacro with-log-errors [k & args] `(catch-all (do ~@args) e# (log/ppr ~k e#))))

(defn wrap-log-errors [k f] ; TODO find a cleaner way to do this
  (fn ([]                       (with-log-errors k (f)                           ))
      ([a0]                     (with-log-errors k (f a0)                        ))
      ([a0 a1]                  (with-log-errors k (f a0 a1)                     ))
      ([a0 a1 a2]               (with-log-errors k (f a0 a1 a2)                  ))
      ([a0 a1 a2 a3]            (with-log-errors k (f a0 a1 a2 a3)               ))
      ([a0 a1 a2 a3 a4]         (with-log-errors k (f a0 a1 a2 a3 a4)            ))
      ([a0 a1 a2 a3 a4 a5]      (with-log-errors k (f a0 a1 a2 a3 a4 a5)         ))
      ([a0 a1 a2 a3 a4 a5 & as] (with-log-errors k (apply f a0 a1 a2 a3 a4 a5 as)))))
