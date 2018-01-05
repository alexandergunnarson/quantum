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
    [quantum.core.macros.core      :as cmacros
      :refer [case-env case-env*]]
    [quantum.core.vars             :as var
      :refer [defalias]])
#?(:cljs
  (:require-macros
    [quantum.core.error            :as self])))

(def ^{:todo {0 "Finish up `conditions` fork" 1 "look at cljs.stacktrace / clojure.stacktrace"}}
  annotations)

;; =================================================================

;; ----- GENERIC ERROR ----- ;;

(defn generic-error [env]
  (case-env* env :clj 'java.lang.Throwable :cljs 'js/Error))

(def generic-error-type #?(:clj Throwable :cljs js/Error))
(def error? (fnl instance? generic-error-type))
#?(:clj (defalias throwable? error?))

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

(defn ?message [x] (when (error? x) #?(:clj (.getLocalizedMessage ^Throwable x) :cljs (.-message x))))
(def ?ex-data ex-data)

;; ----- (RECORD-BASED) ERROR ----- ;;

#?(#_:clj  #_(defrecord Error [ident message data trace cause]) ; defined in Java as quantum.core.error.Error
   :cljs (defrecord Error [ident message data trace cause]))

(def error-map-type #?(:clj quantum.core.error.Error :cljs quantum.core.error/Error))
(def error-map? (fnl instance? error-map-type))

#?(:clj
(defn >root-cause [x]
  (core/assert (error? x))
  (if-let [cause0 (.getCause ^Throwable x)]
    (loop [cause cause0]
      (if-let [cause' (.getCause cause)]
        (recur cause')
        cause))
    x)))

#?(:clj
(defn >via [x]
  (core/assert (error? x))
  (loop [via [] ^Throwable t x]
    (if t
        (recur (conj via t) (.getCause t))
        (when-not (empty? via) via)))))

(defn >err
  "Transforms `x` into an `Error`: a record with at least the keys #{:ident :message :data :trace :cause}.
   In Clojure, similar to `Throwable->map`."
  {:todo #{"Support `:via`?"}}
  ([] #?(:clj  (quantum.core.error.Error. nil nil nil nil nil)
         :cljs (>err (js/Error.))))
  ([x]
    (cond (error-map? x)
            x
          (map? x)
            #?(:clj  (quantum.core.error.Error.
                       (:ident x) (:message x) (:data x) (:trace x) (:cause x)
                       (meta x) (dissoc x :ident :message :data :trace :cause))
               :cljs (Error->map x))
          (error? x)
            #?(:clj  (let [^Throwable t x]
                       (quantum.core.error.Error.
                         nil (.getLocalizedMessage t) (?ex-data t) (.getStackTrace t) (some-> (.getCause t) >err)
                         (meta t)
                         {:type (class t)}))
               :cljs (with-meta
                       (-> (quantum.core.error.Error. (.-name x) (.-message x) (?ex-data x) (.-stack x) (.-cause x))
                           ;; other non-standard fields
                           (cond-> (.-description  x) (assoc :description   (.-description  x))
                                   (.-number       x) (assoc :number        (.-number       x))
                                   (.-fileName     x) (assoc :file-name     (.-fileName     x))
                                   (.-lineNumber   x) (assoc :line-number   (.-lineNumber   x))
                                   (.-columnNumber x) (assoc :column-number (.-columnNumber x))))
                       (meta x)))
          (string? x)
            (quantum.core.error.Error. nil x nil nil nil)
          :else
            (quantum.core.error.Error. nil nil x nil nil)))
  ([a0 a1]
    (if (string? a0)
        (let [message a0 data a1]
          (quantum.core.error.Error. nil message data nil nil))
        (let [ident a0 data a1]
          (quantum.core.error.Error. ident nil data nil nil))))
  ([ident message data]
    (quantum.core.error.Error. ident message data nil nil))
  ([ident message data trace]
    (quantum.core.error.Error. ident message data trace nil))
  ([ident message data trace cause]
    (quantum.core.error.Error. ident message data trace cause)))

(def err! (rcomp >err (fn1 throw)))

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
   `(try ~try-expr (catch ~(generic-error &env) ~error-sym ~catch-expr)))
  ([try-expr error-sym catch-expr finally-expr]
   `(try ~try-expr (catch ~(generic-error &env) ~error-sym ~catch-expr) (finally ~finally-expr)))))

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

#?(:clj (defalias try+   try/try+  ))
#?(:clj (defalias throw+ try/throw+))

(defn todo ([]    (throw (->ex :todo "This feature has not yet been implemented." nil)))
           ([msg] (throw (->ex :todo (str "This feature has not yet been implemented: " msg) nil))))
(defalias TODO todo)

(defn not-supported  [name- x] (->ex (str "`" name- "` not supported on") {:x (type x)}))
(defn not-supported! [name- x] (throw (not-supported name- x)))
