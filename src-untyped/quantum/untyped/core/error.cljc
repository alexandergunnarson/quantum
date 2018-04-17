(ns quantum.untyped.core.error
  (:require
    [clojure.core               :as core]
    [fipp.edn                   :as fipp]
    [slingshot.slingshot        :as try]
    [quantum.untyped.core.core  :as ucore]
    [quantum.untyped.core.fn
      :refer [fn1 fnl rcomp]]
    [quantum.untyped.core.form.evaluate
      :refer [case-env case-env*]]
    [quantum.untyped.core.vars  :as uvar
      :refer [defalias defaliases defmacro-]])
#?(:cljs
  (:require-macros
    [quantum.untyped.core.error :as self
      :refer [err-constructor]])))

(ucore/log-this-ns)

;; ===== Config ===== ;;

(uvar/defonce *print-blacklist "A set of classes not to print" (atom #{}))

(declare error? >err)

(defn ppr
  "Fast pretty print using brandonbloom/fipp.
   At least 5 times faster than `clojure.pprint/pprint`.
   Prints no later than having consumed the bound amount of memory,
   so you see your first few lines of output instantaneously."
  ([] (println))
  ([x]
    (binding [*print-length* (or *print-length* 1000)] ; A reasonable default
      (do (cond
            (error? x)
              (fipp/pprint (>err x))
            (and (string? x) (> (count x) *print-length*))
              (println
                (str "String is too long to print ("
                     (str (count x) " elements")
                     ").")
                "`*print-length*` is set at" (str *print-length* ".")) ; TODO fix so ellipsize
            (contains? @*print-blacklist (type x))
              (println
                "Object's class"
                (str (type x) "(" ")")
                "is blacklisted for printing.")
            :else
              (fipp/pprint x))
          nil)))
  ([x & xs]
    (doseq [x' (cons x xs)] (ppr x'))))

(defn ppr-str
  "Like `pr-str`, but pretty-prints."
  [x] (with-out-str (ppr x)))

;; ===== Error type: generic ===== ;;

(def generic-error-type #?(:clj Throwable :cljs js/Error))

(defn env>generic-error [env]
  (case-env* env :clj 'java.lang.Throwable :cljs 'js/Error))

(def error? (fnl instance? generic-error-type))
#?(:clj (defalias throwable? error?))

;; ===== Error type: built-in exception info ===== ;;

(def ex-info-type #?(:clj clojure.lang.ExceptionInfo :cljs cljs.core/ExceptionInfo))

(def ex-info? (fnl instance? ex-info-type))

(defn >ex-info
  ([data] (>ex-info "Exception" data))
  ([msg data] (ex-info msg (or data {}))))

(def ex-info! (rcomp >ex-info (fn1 throw)))

;; ===== Error type: `defrecord`/map ===== ;;

#?(#_:clj #_(defrecord Error [ident message data trace cause]) ; defined in Java as `quantum.core.error.Error`
     :cljs  (defrecord Error [ident message data trace cause]))

(def error-map-type #?(:clj quantum.core.error.Error :cljs quantum.untyped.core.error/Error))

(def error-map? (fnl instance? error-map-type))

#?(:clj
(defmacro- err-constructor [& args]
  `(~(case-env :clj  'quantum.core.error.Error.
               :cljs 'quantum.untyped.core.error.Error.) ~@args)))

(declare ?ex-data)

(defn >err
  "Transforms `x` into an `Error`: a record with at least the keys #{:ident :message :data :trace :cause}.
   In Clojure, similar to `Throwable->map`."
  {:todo #{"Support `:via`?"}}
  ([] #?(:clj  (err-constructor nil nil nil nil nil)
         :cljs (>err (js/Error.))))
  ([x]
    (cond (error-map? x)
            x
          (map? x)
            #?(:clj  (err-constructor
                       (:ident x) (:message x) (:data x) (:trace x) (:cause x)
                       (meta x) (dissoc x :ident :message :data :trace :cause))
               :cljs (-> x map->Error (assoc :message (:message x))))
          (error? x)
            #?(:clj  (let [^Throwable t x]
                       (err-constructor
                         nil (.getLocalizedMessage t) (?ex-data t) (.getStackTrace t) (some-> (.getCause t) >err)
                         (meta t)
                         {:type (class t)}))
               :cljs (with-meta
                       (-> (err-constructor (.-name x) (.-message x) (?ex-data x) (.-stack x) (.-cause x))
                           ;; other non-standard fields
                           (cond-> (.-description  x) (assoc :description   (.-description  x))
                                   (.-number       x) (assoc :number        (.-number       x))
                                   (.-fileName     x) (assoc :file-name     (.-fileName     x))
                                   (.-lineNumber   x) (assoc :line-number   (.-lineNumber   x))
                                   (.-columnNumber x) (assoc :column-number (.-columnNumber x))))
                       (meta x)))
          (string? x)
            (>err nil x nil nil nil)
          :else
            (>err nil nil x nil nil)))
  ([a0 a1]
    (if (string? a0)
        (let [message a0 data a1]
          (>err nil message data nil nil))
        (let [ident a0 data a1]
          (>err ident nil data nil nil))))
  ([ident message data]
    (>err ident message data nil nil))
  ([ident message data trace]
    (>err ident message data trace nil))
  ([ident message data trace cause]
    (err-constructor ident message data trace cause)))

(def err! (rcomp >err (fn1 throw)))

;; ===== Error information extraction ===== ;;

(defn ?message [x]
  (when (error? x) #?(:clj (.getLocalizedMessage ^Throwable x) :cljs (.-message x))))

(def ?ex-data ex-data)

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

;; ===== Error manipulation ===== ;;

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
(defmacro ignore [& body]
  `(try ~@body (catch ~(env>generic-error &env) _# nil))))

;; ===== Specific error types ===== ;;

(defn todo
  ([]    (err! :todo "This feature has not yet been implemented." nil))
  ([msg] (err! :todo (str "This feature has not yet been implemented: " msg) nil)))
(defalias TODO todo)

(defn not-supported  [name- x] (>err (str "`" name- "` not supported on") {:x (type x)}))
(defn not-supported! [name- x] (throw (not-supported name- x)))

;; ===== Improved error handling ===== ;;

#?(:clj (defaliases try try+ throw+))
