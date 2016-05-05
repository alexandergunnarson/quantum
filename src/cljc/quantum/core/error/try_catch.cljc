(ns
  ^{:doc "Improved try/catch via Slingshot (https://github.com/scgilardi/slingshot)
          adaptation for both Clojure & ClojureScript."
    :attribution "Alex Gunnarson"}
  quantum.core.error.try-catch
  (:require [clojure.string                :as str      ]
            [quantum.core.macros.core      :as cmacros
              :refer [#?@(:clj [resolve-local if-cljs])]]
            [quantum.core.collections.base :as cbase
              :refer [appears-within?]                  ])
  #?(:cljs
  (:require-macros
            [quantum.core.macros.core
              :refer [if-cljs resolve-local]            ])))

; ================== START SLINGSHOT ==================

(defn throw-arg [& args]
  (throw #?(:clj (Exception. (apply str args))
            :cljs (js/Error. (apply str args)))))

;; context support

(defn make-context
  "Makes a throw context from a throwable or explicit arguments"
  ([t]
   (make-context t
     #?(:clj (.getMessage    ^Throwable t) :cljs (.-message t))
     #?(:clj (.getCause      ^Throwable t) :cljs "") ; if you do nil, CLJS says it's only 3 args... weird
     #?(:clj (.getStackTrace ^Throwable t) :cljs (.-stack   t)))) ; |.stack| is browser-dependent
  ([object message cause stack-trace]
   {:object      object
    :message     message
    :cause       cause
    :stack-trace stack-trace}))

(defn wrap
  "Returns a context wrapper given a context"
  [{:keys [object message cause #?(:clj ^Throwable stack-trace :cljs stack-trace)]}]
  (let [data (if (map? object) object ^::wrapper? {:object object})]
    (doto (ex-info message data cause)
          #?(:clj  (.setStackTrace stack-trace)
             :cljs identity))))

(defn unwrap
  "If t is a context wrapper or other IExceptionInfo, returns the
  corresponding context with t assoc'd as the value for :wrapper, else
  returns nil"
  [t]
  (if-let [data (ex-data t)]
    (assoc (make-context t)
      :object (if (::wrapper? (meta data)) (:object data) data)
      :wrapper t)))

(defn unwrap-all
  "Searches Throwable t and its cause chain for a context wrapper or
  other IExceptionInfo. If one is found, returns the corresponding
  context with the wrapper assoc'd as the value for :wrapper, else
  returns nil."
  [t]
  (or (unwrap t)
      #?(:clj
          (if-let [cause (.getCause ^Throwable t)]
            (recur cause)))))

(defn get-throwable
  "Returns a Throwable given a context: the object in context if it's
  a Throwable, else a Throwable context wrapper"
  {:macro-dependency? true}
  [{object :object :as context}]
  (if (instance? #?(:clj Throwable :cljs js/Error) object)
      object
      (wrap context)))

(defn get-context
  "Returns a context given a Throwable t. If t or any Throwable in its
  cause chain is a context wrapper or other IExceptionInfo, returns
  the corresponding context with the wrapper assoc'd as the value
  for :wrapper and t assoc'd as the value for :throwable. Otherwise
  creates a new context based on t with t assoc'd as the value
  for :throwable."
  [t]
  (-> (or (unwrap-all t)
          (make-context t))
      (assoc :throwable t)))

;; try+ support

(defn parse-try+
  "Returns a vector of seqs containing the expressions, catch clauses,
  and finally clauses in a try+ body, or throws if the body's structure
  is invalid"
  [body]
  (letfn
      [(item-type [item]
         ({'catch :catch-clause 'else :else-clause 'finally :finally-clause}
          (and (seq? item) (first item))
          :expression))
       (match-or-defer [s type]
         (if (-> s ffirst item-type (= type)) s (cons nil s)))]
    (let [groups (partition-by item-type body)
          [e & groups] (match-or-defer groups :expression)
          [c & groups] (match-or-defer groups :catch-clause)
          [l & groups] (match-or-defer groups :else-clause)
          [f & groups] (match-or-defer groups :finally-clause)]
      (if (every? nil? [groups (next l) (next f)])
        [e c (first l) (first f)]
        (throw-arg
           "try+ form must match: "
           "(try+ expression* catch-clause* else-clause? finally-clause?)")))))

(def ^{:dynamic true
       :doc "https://github.com/scgilardi/slingshot/"}
  *catch-hook* identity)

#?(:clj
(defmacro gen-catch
  {:doc "<https://github.com/scgilardi/slingshot/>"}
  [default-error-sym catch-clauses throw-sym threw?-sym]
  (let [pred              (if-cljs &env fn? class?)]
    (letfn
        [(class-selector? [selector]
           (if (symbol? selector)
             (let [resolved (resolve selector)]
               (if (pred resolved)
                   resolved))))
         (cond-test [selector]
           (letfn
               [(key-values []
                  (and (vector? selector)
                       (if (even? (count selector))
                         `(and ~@(for [[key val] (partition 2 selector)]
                                   `(= (get ~'% ~key) ~val)))
                         (throw-arg
                           "key-values selector: " (pr-str selector)
                           " does not match: " "[key val & kvs]"))))
                (selector-form []
                  (and (seq? selector) (appears-within? '% selector)
                       selector))
                (predicate []
                  `(~selector ~'%))]
             `(let [~'% (:object ~'&throw-context)]
                ~(or (key-values) (selector-form) (predicate)))))
         (cond-expression [binding-form expressions]
           `(let [~binding-form (:object ~'&throw-context)]
              ~@expressions))
         (transform [[_ selector binding-form & expressions]]
           (let [class-selector (class-selector? selector)]
             (if class-selector
                 [`(instance? ~class-selector (:object ~'&throw-context))
                  (cond-expression (with-meta binding-form {:tag selector}) expressions)]
                 [(cond-test selector) (cond-expression binding-form expressions)])))]
     (let [code
            (list
             `(catch ~default-error-sym ~'&throw-context
                (reset! ~threw?-sym true)
                (let [~'&throw-context (-> ~'&throw-context get-context *catch-hook*)]
                  (cond
                   (contains? ~'&throw-context :catch-hook-return)
                   (:catch-hook-return ~'&throw-context)
                   (contains? ~'&throw-context :catch-hook-throw)
                   (~throw-sym (:catch-hook-throw ~'&throw-context))
                   (contains? ~'&throw-context :catch-hook-rethrow)
                   (~throw-sym)
                   ~@(mapcat transform catch-clauses) ; ~@
                   :else
                   (~throw-sym)))))]
        `(quote ~code))))))

(defn gen-finally
  "Returns either nil or a list containing a finally clause for a try
  form based on the parsed else and/or finally clause from a try+
  form"
  [else-clause finally-clause threw?-sym]
  (cond else-clause
        (list
         `(finally
            (try
              (when-not @~threw?-sym
                ~@(rest else-clause))
              ~(if finally-clause
                 finally-clause))))
        finally-clause
        (list finally-clause)))

;; throw+ support



(defn stack-trace
  "Returns the current stack trace beginning at the caller's frame"
  []
  #?(:clj
      (let [trace (.getStackTrace (Thread/currentThread))]
        ;(java.util.Arrays/copyOfRange trace 2 (alength trace)) ; To get around weird cljs error
        (into-array trace))
     :cljs (-> (js/Error.) .-stack (str/split "\n") rest rest)))


(defn parse-throw+
  "Returns a vector containing the message and cause that result from
  processing the arguments to throw+"
  [object cause & args]
  (let [[cause & args] (if (or (empty? args) (string? (first args)))
                           (cons cause args)
                           args)
        [fmt & args] (cond
                       (next args)
                         args
                       (seq args)
                         ["" (first args)]
                       :else
                         ["throw+: " (pr-str object)])
        message (apply str fmt args)]
    [message cause]))

(defn default-throw-hook [context]
  (let [throwable-obj (get-throwable context)]
    (throw throwable-obj)))

(def ^{:dynamic true
       :doc "Hook to allow overriding the behavior of throw+. Must be
  bound to a function of one argument, a context map. Defaults to
  default-throw-hook."}
  *throw-hook* default-throw-hook)

(defn throw-fn
  "Helper to throw a context based on arguments and &env from throw+"
  [object {cause :throwable} stack-trace & args]
  (let [[message cause] (apply parse-throw+ object cause args)
        context (make-context object message cause stack-trace)]
    (*throw-hook* context)))

#?(:clj
(defmacro rethrow
  "Within a try+ catch clause, throws the outermost wrapper of the
  caught object"
  []
  `(throw (:throwable ~'&throw-context))))

(defn get-throw-context
  {:source "https://github.com/scgilardi/slingshot/"}
  [t]
  (get-context t))

(defn get-thrown-object [t]
  (-> t get-throw-context :object))

#?(:clj
(defmacro throw+
  {:source   "https://github.com/scgilardi/slingshot/"
   :arglists '([] [object cause? message-or-fmt? & fmt-args])}
  ([object & args]
   `(let [~'% ~object]
      (throw-fn ~'%
                  (resolve-local ~'&throw-context)
                  (stack-trace)
                  ~@args)))
  ([]
   `(rethrow))))

#?(:clj (defn throwf+ [obj] (throw+ obj)))

#?(:clj
(defmacro try+
  {:source "https://github.com/scgilardi/slingshot/"
   :doc "BIG NOTE: If you're ever concerned that the wrong exception is being
                   printed, then rest assured. It's because |!| prints the
                   cause, not the returned object.

                   Example:
                   (try+ (throw (Exception.))
                     (catch Object e
                       (throw+ {:random-obj? true})))

                   This will print not |{:random-obj? true}|, but rather
                   Exception   <NAMESPACE>/eval<RANDOM_NUMS> (form-init ...)

                   This might lead you to think that the incorrect object was
                   thrown, but this is not the case.

                   Surround the code example like so:
                   (try+ <CODE>
                     (catch Object e e))

                   And the correct object will print."
   :in '[(throw+ {}) (catch Object _ 123)]
   :out 123}
  [& body]
  (let [[expressions catches else finally] (parse-try+ body)
        threw? (gensym "threw?")
        default-error-sym (if-cljs &env 'js/Error 'java.lang.Throwable)
        code   (eval `(gen-catch ~default-error-sym ~catches throw+ ~threw?))]
    `(let [~threw? (atom false)]
       (try
         ~@expressions
         ~@code
         ~@(gen-finally else finally threw?))))))