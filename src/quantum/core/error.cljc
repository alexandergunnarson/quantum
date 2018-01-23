(ns
  ^{:doc "Error handling. Improved try/catch, and built-in error types for convenience's sake."
    :attribution "alexandergunnarson"}
  quantum.core.error
  (:refer-clojure :exclude [assert])
  (:require
    [clojure.core                  :as core]
    [clojure.string                :as str]
    [slingshot.slingshot           :as try]
    [quantum.core.core
      :refer [kw-map]]
    [quantum.core.data.map         :as map]
    [quantum.core.fn
      :refer [fnl fn1 rcomp fn']]
    [quantum.untyped.core.form.evaluate
      :refer [case-env case-env*]]
    [quantum.untyped.core.vars
      :refer [defalias defaliases]]
    [quantum.untyped.core.error    :as u])
#?(:cljs
  (:require-macros
    [quantum.core.error            :as self])))

(def ^{:todo {0 "Finish up `conditions` fork" 1 "look at cljs.stacktrace / clojure.stacktrace"}}
  annotations)

;; =================================================================

;; ===== Generic error types ===== ;;

(defaliases u generic-error-type env>generic-error error? #?(:clj throwable?))

;; ----- EXCEPTION-INFO ----- ;;

(def ex-info-type #?(:clj clojure.lang.ExceptionInfo :cljs cljs.core/ExceptionInfo))
(def ex-info? (fnl instance? ex-info-type))

(defn ->ex-info
  ([data]     (ex-info "Exception" data))
  ([msg data] (ex-info msg         data)))

;; TODO replace with `->err`
(defn ->ex
  "Creates an exception."
  ([type]          (ex-info (name type) {:type type}))
  ([msg objs]      (ex-info (str msg)   {:type msg  :msg msg :objs objs}))
  ([type msg objs] (ex-info msg         {:type type :msg msg :objs objs})))

(def ex! (rcomp ->ex (fn1 throw)))

;; ===== Error information extraction ===== ;;

(defaliases u ?message ?ex-data #?@(:clj [>root-cause >via]))

;; ===== Error `defrecord`/map ===== ;;

(defaliases u error-map-type error-map? >err err!)

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
(defmacro catch-all
  "Cross-platform try/catch/finally for catching all exceptions.

   Uses `js/Error` instead of `:default` as temporary workaround for http://goo.gl/UW7773."
  {:from 'taoensso.truss.impl/catching
   :see  ["http://dev.clojure.org/jira/browse/CLJ-1293"]}
  ([try-expr                     ] `(catch-all ~try-expr _# nil))
  ([try-expr           catch-expr] `(catch-all ~try-expr _# ~catch-expr))
  ([try-expr error-sym catch-expr]
   `(try ~try-expr (catch ~(env>generic-error &env) ~error-sym ~catch-expr)))
  ([try-expr error-sym catch-expr finally-expr]
   `(try ~try-expr (catch ~(env>generic-error &env) ~error-sym ~catch-expr) (finally ~finally-expr)))))

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
  `(try ~@body (catch ~(env>generic-error &env) _# nil))))

#?(:clj
(defmacro assertf-> [f arg throw-obj]
  `(do (throw-unless (~f ~arg) (->ex nil ~throw-obj ['~f ~arg]))
       ~arg)))

#?(:clj
(defmacro assertf->> [f throw-obj arg]
  `(do (throw-unless (~f ~arg) (->ex nil ~throw-obj ['~f ~arg]))
       ~arg)))

#?(:clj (defalias try+   try/try+  ))
#?(:clj (defalias throw+ try/throw+))

;; ===== Specific error types ===== ;;

(defaliases u todo TODO not-supported not-supported!)
