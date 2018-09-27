(ns
  ^{:doc "Error handling. Improved try/catch, and built-in error types for convenience's sake."
    :attribution "alexandergunnarson"}
  quantum.core.error
  (:refer-clojure :exclude [assert])
  (:require
    [clojure.core                  :as core]
    [clojure.string                :as str]
    [quantum.core.data.map         :as map]
    [quantum.core.fn
      :refer [fnl fn1 rcomp fn']]
    [quantum.core.type             :as t]
    [quantum.core.vars             :as var
      :refer [defalias defaliases]]
    [quantum.untyped.core.core     :as ucore]
    [quantum.untyped.core.form.evaluate
      :refer [case-env case-env*]]
    [quantum.untyped.core.error    :as u]))

(ucore/log-this-ns)

(def ^{:todo {0 "Finish up `conditions` fork" 1 "look at cljs.stacktrace / clojure.stacktrace"}}
  annotations)

(defalias t/throwable?)

;; ===== Config ===== ;;

(defalias u/*pr-data-to-str?)

;; ===== Error type: generic ===== ;;

(defaliases u generic-error-type env>generic-error error? #?(:clj throwable?))

;; ===== Error type: built-in exception info ===== ;;

(defaliases u ex-info-type ex-info? >ex-info ex-info!)

;; ===== Error type: `defrecord`/map ===== ;;

(defaliases u error-map-type error-map? >err err!)

;; ===== Error information extraction ===== ;;

(defaliases u ?message ?ex-data #?@(:clj [>root-cause >via]))

;; ===== Error manipulation ===== ;;

#?(:clj (defaliases u catch-all ignore))

;; ===== Specific error types ===== ;;

(defaliases u todo TODO not-supported not-supported!)

;; ===== Improved error handling ===== ;;

#?(:clj (defaliases u try+ throw+))

;; ===== TODO Dubious usefulness ===== ;;

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
   `(with-assert ~expr ~pred (>ex-info "Assertion failed" '(~pred ~expr))))
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
(defmacro assertf-> [f arg throw-obj]
  `(do (throw-unless (~f ~arg) (>ex-info nil ~throw-obj ['~f ~arg]))
       ~arg)))

#?(:clj
(defmacro assertf->> [f throw-obj arg]
  `(do (throw-unless (~f ~arg) (>ex-info nil ~throw-obj ['~f ~arg]))
       ~arg)))

;; TODO replace with `>err`
(defn ->ex
  "Creates an exception."
  ([type]          (ex-info (name type) {:type type}))
  ([msg objs]      (ex-info (str msg)   {:type msg  :msg msg :objs objs}))
  ([type msg objs] (ex-info msg         {:type type :msg msg :objs objs})))
