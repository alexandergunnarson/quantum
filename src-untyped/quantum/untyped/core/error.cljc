(ns quantum.untyped.core.error
  (:require
    [clojure.core :as core]
    [quantum.untyped.core.fn
      :refer [fn1 fnl rcomp]]
    [quantum.untyped.core.form.evaluate
      :refer [case-env case-env*]]
    [quantum.untyped.core.vars
      :refer [defalias defmacro-]]))

;; ===== Generic error types ===== ;;

(def generic-error-type #?(:clj Throwable :cljs js/Error))

(defn env>generic-error [env]
  (case-env* env :clj 'java.lang.Throwable :cljs 'js/Error))

(def error? (fnl instance? generic-error-type))
#?(:clj (defalias throwable? error?))

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

;; ===== Error `defrecord`/map ===== ;;

#?(#_:clj #_(defrecord Error [ident message data trace cause]) ; defined in Java as `quantum.core.error.Error`
     :cljs  (defrecord Error [ident message data trace cause]))

(def error-map-type #?(:clj quantum.core.error.Error :cljs quantum.untyped.core.error/Error))

(def error-map? (fnl instance? error-map-type))

#?(:clj
(defmacro- err-constructor [& args]
  `(~(case-env :clj  'quantum.core.error.Error.
               :cljs 'quantum.untyped.core.error.Error.) ~@args)))

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
               :cljs (Error->map x))
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
            (err-constructor nil x nil nil nil)
          :else
            (err-constructor nil nil x nil nil)))
  ([a0 a1]
    (if (string? a0)
        (let [message a0 data a1]
          (err-constructor nil message data nil nil))
        (let [ident a0 data a1]
          (err-constructor ident nil data nil nil))))
  ([ident message data]
    (err-constructor ident message data nil nil))
  ([ident message data trace]
    (err-constructor ident message data trace nil))
  ([ident message data trace cause]
    (err-constructor ident message data trace cause)))

(def err! (rcomp >err (fn1 throw)))

;; ===== Specific error types ===== ;;

(defn todo
  ([]    (err! :todo "This feature has not yet been implemented." nil))
  ([msg] (err! :todo (str "This feature has not yet been implemented: " msg) nil)))
(defalias TODO todo)

(defn not-supported  [name- x] (>err (str "`" name- "` not supported on") {:x (type x)}))
(defn not-supported! [name- x] (throw (not-supported name- x)))
