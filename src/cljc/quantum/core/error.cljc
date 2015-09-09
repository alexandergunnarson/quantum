(ns
  ^{:doc "Error handling. Improved try/catch, and built-in error types for convenience's sake."
    :attribution "Alex Gunnarson"}
  quantum.core.error
  (:require-quantum [ns log map fn])
  (:require [clojure.string :as str])

  #?(:cljs (:require-macros [quantum.core.log :as log])))

; Doesn't work: "Use of undeclared Var ___" if at runtime
;#?(:clj (defmacro try-eval [obj] (eval obj)))

(defrecord Err [type msg objs])

(defn throw-arg [& args]
  (log/pr ::macro-expand "THROW-ARG WITH ARGS" args)
  (throw #?(:clj (Exception. (apply str args))
            :cljs (js/Error. (apply str args)))))

; ================== START SLINGSHOT ==================

(defn appears-within?
  "Returns true if x appears within coll at any nesting depth.

   I added termination on find."
  {:source "slingshot"
   :contributors ["Alex Gunnarson"]}
  [x coll]
  (log/pr ::macro-expand "APPEARS-WITHIN")
  (let [result (atom false)]
    (try
      (clojure.walk/postwalk
        (fn [t]
          (when (= x t)
            (reset! result true)
            (throw #?(:clj (Exception.) :cljs (js/Error.)))))
        coll)
      @result
      (catch #?(:clj Exception :cljs js/Error) _ @result))))

;; context support

(defn make-context
  "Makes a throw context from a throwable or explicit arguments"
  ([t] ; ^Throwable in Clojure
   (log/pr ::macro-expand "MAKE-CONTEXT")
   (make-context t
     (#?(:clj .getMessage    :cljs .-message) t)
      #?(:clj (.getCause t)  :cljs "") ; if you do nil, CLJS says it's only 3 args... weird
     (#?(:clj .getStackTrace :cljs .-stack  ) t))) ; |.stack| is browser-dependent
  ([object message cause stack-trace]
   {:object      object
    :message     message
    :cause       cause
    :stack-trace stack-trace}))

#?(:clj
(defmacro tag
  "Doesn't really work unless print-dup is defined for all local vars."
  [obj tag-]
  `(ns/c-eval (ns/context) (with-meta '~obj {:tag '~tag-}))))

(defn wrap
  "Returns a context wrapper given a context"
  [{:keys [object message cause stack-trace]}]
  (log/pr ::macro-expand "WRAP")
  (let [data (if (map? object) object ^::wrapper? {:object object})]
    (doto (ex-info message data cause)
          #?(:clj  (.setStackTrace ^Throwable stack-trace)
             :cljs identity))))

(defn unwrap
  "If t is a context wrapper or other IExceptionInfo, returns the
  corresponding context with t assoc'd as the value for :wrapper, else
  returns nil"
  [t]
  (log/pr ::macro-expand "UNWRAP")
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
  (log/pr ::macro-expand "UNWRAP-ALL")
  (or (unwrap t)
      #?(:clj
          (if-let [cause (.getCause ^Throwable t)]
            (recur cause)))))

(defn get-throwable
  "Returns a Throwable given a context: the object in context if it's
  a Throwable, else a Throwable context wrapper"
  {:macro-dependency? true}
  [{object :object :as context}]
  (log/pr ::macro-expand "IN GET-THROWABLE WITH" object)
  (if (instance? AError object) ; can't use "AError..."
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
  [lang catch-clauses throw-sym threw?-sym]
  (letfn
      [(class-selector? [selector]
         (if (symbol? selector)
           (let [resolved (resolve selector)]
             (if (#?(:clj class? :cljs fn?) resolved)
                 resolved))))
       (cond-test [selector]
         (log/pr ::macro-expand "COND-TEST SELECTOR:" selector)
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
         (let [class-selector (class-selector? selector)
                  _ (log/pr ::macro-expand "CLASS-SELECTOR IS" class-selector)
                  _ (log/pr ::macro-expand "(cond-test selector) IS" (cond-test selector))]
           (if class-selector
               [`(instance? ~class-selector (:object ~'&throw-context))
                (cond-expression (with-meta binding-form {:tag selector}) expressions)]
               [(cond-test selector) (cond-expression binding-form expressions)])))]
   (let [default-error-sym
          (condp = lang :clj  'java.lang.Throwable
                        :cljs 'js/Error)
         code
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
           (~throw-sym)))))
        _ (log/pr ::macro-expand "CODE" code)]
      `(quote ~code)))))

(defn gen-finally
  "Returns either nil or a list containing a finally clause for a try
  form based on the parsed else and/or finally clause from a try+
  form"
  [else-clause finally-clause threw?-sym]
  (log/pr ::macro-expand "IN GEN-FINALLY")
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

#?(:clj
(defmacro resolve-local
  "Expands to sym if it names a local in the current environment or
  nil otherwise"
  [sym]
  (log/pr ::macro-expand "RESOLVE-LOCAL")
  (if (contains? &env sym) sym)))

(defn stack-trace
  "Returns the current stack trace beginning at the caller's frame"
  []
  (log/pr ::macro-expand "STACK-TRACE")
  #?(:clj
      (let [trace (.getStackTrace (Thread/currentThread))]
        ;(java.util.Arrays/copyOfRange trace 2 (alength trace)) ; To get around weird cljs error
        (into-array trace))
     :cljs (-> (js/Error.) .-stack (str/split "\n") rest rest)))


(defn parse-throw+
  "Returns a vector containing the message and cause that result from
  processing the arguments to throw+"
  [object cause & args]
  (log/pr ::macro-expand "IN PARSE-THROW")
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
  (log/pr ::macro-expand "IN DEFAULT THROW-HOOK")
  (let [throwable-obj (get-throwable context)
        _ (log/pr ::macro-expand "THROWABLE OBJ" throwable-obj "META" (meta throwable-obj) "CLASS" throwable-obj)]
    (throw throwable-obj)
    (log/pr ::macro-expand "AFTER THROW")))

(def ^{:dynamic true
       :doc "Hook to allow overriding the behavior of throw+. Must be
  bound to a function of one argument, a context map. Defaults to
  default-throw-hook."}
  *throw-hook* default-throw-hook)

(defn throw-fn
  "Helper to throw a context based on arguments and &env from throw+"
  [object {cause :throwable} stack-trace & args]
  (let [[message cause] (apply parse-throw+ object cause args)
        _ (log/pr ::macro-expand "AFTER PARSE-THROW IN THROW-FN")
        context (make-context object message cause stack-trace)
        _ (log/pr ::macro-expand "AFTER MAKE-CONTEXT IN THROW-FN")]
    (doto (*throw-hook* context)
      (log/pr ::macro-expand "IS THE THROW HOOK"))))

#?(:clj
(defmacro rethrow
  "Within a try+ catch clause, throws the outermost wrapper of the
  caught object"
  []
  `(throw (:throwable ~'&throw-context))))

#?(:clj
(defmacro throw+
  {:source   "https://github.com/scgilardi/slingshot/"
   :arglists '([] [object cause? message-or-fmt? & fmt-args])}
  ([object & args]
    (log/pr ::macro-expand "IN THROW+")
   `(let [~'% ~object]
      (throw-fn ~'%
                  (resolve-local ~'&throw-context)
                  (stack-trace)
                  ~@args)))
  ([]
   `(rethrow))))

#?(:clj (defn throwf+ [obj] (throw+ obj)))

#?(:clj
(defmacro try+*
  {:source "https://github.com/scgilardi/slingshot/"
   :in '[(throw+ {}) (catch Object _ 123)]
   :out 123}
  [lang & body]
  (let [[expressions catches else finally] (parse-try+ body)
        _      (log/pr ::macro-expand "AFTER PARSE TRY")
        threw? (gensym "threw?")
        code   (eval `(gen-catch ~lang ~catches throw+ ~threw?))
        _      (log/pr ::macro-expand "AFTER GEN-CATCH")]
    `(let [~threw? (atom false)]
       (try
         ~@expressions
         ~@code
         ~@(gen-finally else finally threw?))))))

#?(:clj 
(defmacro try+ [& body] `(try+* :clj ~@body)))

(defn get-throw-context
  {:source "https://github.com/scgilardi/slingshot/"}
  [t]
  (get-context t))

(defn get-thrown-object [t]
  (-> t get-throw-context :object))

; ================== END SLINGSHOT ================== 

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

#?(:clj
(defmacro with-throw
  "Throws an exception with the given message @message if
   @expr evaluates to false.

   Specifically for use with :pre and :post conditions."
  {:attribution "Alex Gunnarson"}
  [expr throw-content]
  `(if ~expr ~expr (throw+ ~throw-content))))

#?(:clj (defalias throw-unless with-throw))

#?(:clj
(defmacro throw-when
  [expr throw-content]
  `(if-not ~expr ~expr (throw+ ~throw-content))))

#?(:clj
(defmacro with-throws
  "Doesn't quite work yet..."
  [& exprs]
  `(core/doseq [[expr# throw-content#] (map/map-entry-seq ~exprs)]
     (with-throw expr# throw-content#))))

#?(:clj
(defmacro with-catch
  {:usage '(->> 0 (/ 1) (with-catch (constantly -1)))}
  [^Fn handler try-val]
  `(try ~try-val
     (catch Error e# (~handler e#)))))

#?(:clj
(defmacro with-assert [expr pred err]
  `(if (~pred ~expr)
       ~expr
       (throw+ ~err))))

#?(:clj
  (defmacro try-or 
    "An exception-handling version of the 'or' macro.
     Trys expressions in sequence until one produces a result that is neither false nor an exception.
     Useful for providing a default value in the case of errors."
    {:attribution "mikera.cljutils.error"}
    ([exp & alternatives]
       (if-let [as (seq alternatives)] 
         `(or (try ~exp (catch Throwable t# (try-or ~@as))))
         exp))))

(defmacro suppress
  "Suppresses any errors thrown in the body.
  (suppress (error \"Error\")) => nil
  (suppress (error \"Error\") :error) => :error
  (suppress (error \"Error\")
            (fn [e]
              (.getMessage e))) => \"Error\""
  {:source "zcaudate/hara.common.error"}
  ([body]
     `(try ~body (catch Throwable ~'t)))
  ([body catch-val]
     `(try ~body (catch Throwable ~'t
                   (cond (fn? ~catch-val)
                         (~catch-val ~'t)
                         :else ~catch-val)))))

#?(:clj
(defmacro assertf-> [f arg throw-obj]
  `(do (throw-unless (~f ~arg) (Err. nil ~throw-obj ['~f ~arg]))
       ~arg)))

#?(:clj
(defmacro assertf->> [f throw-obj arg]
  `(do (throw-unless (~f ~arg) (Err. nil ~throw-obj ['~f ~arg]))
       ~arg)))





; Probably something like this but with error catching/gates
; (defn comp
;   "Same as `clojure.core/comp` except that the functions will shortcircuit on nil.
;   ((comp inc inc) 1) => 3
;   ((comp inc inc) nil) => nil"
;   [& fs]
;   (fn comp-fn
;     ([i] (comp-fn i fs))
;     ([i [f & more]]
;        (cond (nil? i) nil
;              (nil? f) i
;              :else (recur (f i) more)))))