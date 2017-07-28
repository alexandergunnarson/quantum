(ns quantum.core.defnt
  (:refer-clojure :exclude
    [+ zero? bit-and odd? even? macroexpand])
  (:require
    [clojure.core          :as c]
    [clojure.core.reducers :as r]
    [clojure.core.specs    :as ss]
    [clojure.set           :as set]
    [quantum.core.cache    :as cache
      :refer [defmemoized]]
    [quantum.core.collections.base
      :refer [prewalk postwalk walk]]
    [quantum.core.core
      :refer [?deref ->sentinel]]
    [quantum.core.error
      :refer [TODO]]
    [quantum.core.fn
      :refer [aritoid fn1 fnl fn-> fn->> <-, rcomp
              firsta seconda]]
    [quantum.core.log      :as log
      :refer [prl!]]
    [quantum.core.logic
      :refer [fn-and fn-or fn-not if-not-let]]
    [quantum.core.macros
      :refer [macroexpand]]
    [quantum.core.spec      :as s]
    [quantum.core.type.core :as tcore]
    [quantum.core.vars      :as var
      :refer [update-meta]])
  (:import
    [quantum.core Numeric]))

;; TODO use https://github.com/clojure/core.typed/blob/master/module-rt/src/main/clojure/clojure/core/typed/

;; Apparently I independently came up with an algorithm that is essentially Hindley-Milner...
;; - `dotyped`, `defnt`, and `fnt` create typed contexts in which their internal forms are analyzed
;; and overloads are resolved.
;; - When a function with type overloads is referenced outside of a typed context, then the
;; overload resolution will be done via protocol dispatch unless the function's overloads only
;; differ by arity. In either case, runtime type checks are required.
;; - At some later date, the analyzer will do its best to infer types.
;; - Even if the `defnt` is redefined, you won't have interface problems.


#_"
Note that, for typed functions, they can have multiple possible output constraints
('return types'). Sometimes it depends on the conditional structures; sometimes it
depends on the constraints themselves.

(defn [a] (if (pos? a) 'abc' 123))

Input constraints:
  {'a (constraints pos?)}
Output constraints:
(if (constraints pos?)
    String ; won't keep track of e.g. the fact it only contains 'a', 'b', 'c' and what that entails
    long)

Example:

(defnt [a ?]
  (cond (pos? a)
        123
        (string? a)
        'abd'))

Input constraints:
  (union (constraints pos?)
         (constraints string?)) ; unreachable statement: error

Example:

(defnt [a ?]
  (cond (pos? a)
        123
        (number? a)
        'abd'))

Input constraints:
  (union (constraints pos?)
         (constraints number?)) ; optimized away because it is a subset of previously calculated constraints
Output constraints:
  ...

Unlike static languages, a `nil` value is not considered as having a type
except that of nil.

"

(def ^:dynamic *conditional-branch-pruning?* true)

(def sentinel (->sentinel))

; TODO associative sequence over top of a vector (so it'll display like a seq but behave like a vec)

(s/def ::arg-spec ; TODO expand; make typed destructuring available via ::ss/binding-form
  (s/alt :infer     #{'?}
         #_:something #_any?))

(s/def ::speced-arg
  (s/cat :arg-binding simple-symbol?
         :arg-spec    ::arg-spec))

(s/def ::arg-list
  (s/and vector?
    (s/cat :args    (s/* ::speced-arg)
           :varargs (s/? (s/cat :amp #{'&} :form ::speced-arg)))))

(s/def ::args+body
  (s/cat :args ::arg-list
         :body (s/alt :prepost+body (s/cat :prepost map?
                                           :body (s/+ any?))
                      :body (s/* any?))))

(s/def ::fnt
  (s/cat :name    simple-symbol? ; TODO validate metadata
         :arities (s/alt :arity-1 ::args+body
                         :arity-n (s/+ (s/spec ::args+body)))))

(s/def ::defnt
  (s/cat :name      simple-symbol? ; TODO validate metadata
         :docstring (s/? string?)
         :meta      (s/? map?)
         :arities   (s/alt :arity-1 ::args+body
                           :arity-n (s/cat :bodies (s/+ (s/spec ::args+body))
                                           #_:attr   #_(s/? map?))))) ; TODO what the heck is :attr?)

(defonce defnt-cache (atom {})) ; TODO For now — but maybe lock-free concurrent hash map to come

(defn infer-arg-types
  [arglist body]
  (clojure.tools.analyzer.jvm/analyze body))

; TODO move
(defn into! [xs0 xs1] (reduce (fn [xs0' x] (conj! xs0' x)) xs0 xs1))

; TODO move
; TODO `prewalk-fold`
(defn postwalk-fold
  "Performs a fold-like operation on a tree.
   May or may not be for side effects.
   `branch?f` and `childrenf` are like `tree-seq`'s `branch?` and `children`.
   `rf` and `cf` are like `fold`'s `rf` and `cf`. The elements fed into `rf` are the nodes."
  {:attribution 'alexandergunnarson}
  [rf cf branch?f childrenf root]
  (let [walk (fn walk [node nodes]
               (if (branch?f node)
                   (rf (->> node childrenf (r/map #(rf (walk % (rf)))) (reduce cf nodes))
                       node)
                   nodes))]
    (cf (walk root (cf)))))

; TODO move
; TODO `tree-sequence-prewalk`
(defn tree-sequence-postwalk
  "Walks the tree and outputs an eager sequence containing its nodes ordered from leaf to
   branch (i.e. postorder).
   `childrenf` may return a reducible of any type."
  {:attribution 'alexandergunnarson}
  [branch?f childrenf root]
  (postwalk-fold (aritoid #(transient []) persistent! conj!)
                 (aritoid #(transient []) persistent! into!)
                 branch?f childrenf root))

(defn ast->nodes-postorder
  "Walks the ast and outputs an eager sequence containing its AST nodes ordered from leaf
   to branch (i.e. postorder)."
  [ast]
  (tree-sequence-postwalk
    :op
    (fn [op] (->> op :children
                  (r/mapcat #(let [children|op (get op %)]
                               (if (sequential? children|op)
                                   children|op
                                   [children|op])))))
    ast))

; ===== REDUCERS ===== ;

(defn transducer->reducer
  "Converts a transducer into a reducer."
  {:todo #{"More arity"}}
  ([^long n xf]
    (case n
          0 (fn ([]            (xf))
                ([xs]          (r/reducer xs (xf))))
          1 (fn ([a0]          (xf a0))
                ([a0 xs]       (r/reducer xs (xf a0))))
          2 (fn ([a0 a1]       (xf a0 a1))
                ([a0 a1 xs]    (r/reducer xs (xf a0 a1))))
          3 (fn ([a0 a1 a2]    (xf a0 a1 a2))
                ([a0 a1 a2 xs] (r/reducer xs (xf a0 a1 a2))))
          (throw (ex-info "Unhandled arity for transducer" nil)))))

(def  map+    (transducer->reducer 1 c/map))
(def  map-indexed+    (transducer->reducer 1 c/map-indexed))
(defn map-keys* [f-xs] (fn [f xs] (->> xs (f-xs (juxt (rcomp key f) val)))))
(def  map-keys+ (map-keys* map+))
(defn map-vals* [f-xs] (fn [f xs] (->> xs (f-xs (juxt key (rcomp val f))))))
(def  map-vals+ (map-vals* map+))

(def filter+ (transducer->reducer 1 c/filter))
(defn filter-keys* [f-xs] (fn [pred xs] (->> xs (f-xs (rcomp key pred)))))
(def  filter-keys+ (filter-keys* filter+))

(def remove+ (transducer->reducer 1 c/remove))

(def indexed+ (fn->> (map-indexed+ vector)))

(def partition-all+ (transducer->reducer 1 c/partition-all))

(def lasti (rcomp count dec))

(defn join
  ([from] (join [] from))
  ([to from] (c/into to from)))

(defn reducei
  "`reduce`, indexed."
  [f init xs]
  (let [f' (let [*i (volatile! -1)]
              (fn ([ret x]
                    (f ret x (vreset! *i (unchecked-inc (long @*i)))))))]
    (reduce f' init xs)))

; TODO move
(defn every-val
  "Yields what every value in `xs` is equivalent to (via `=`), or the provided
   `not-equivalent` value if they are not all equivalent."
  [not-equivalent xs]
  (reduce (fn [ret x]
            (cond (identical? ret sentinel) x
                  (not= x ret)              (reduced not-equivalent)
                  :else                     ret))
          sentinel
          xs))


(->typed #{'c}
  '(let [a 2 b nil]
     (+ (+ a b) c)))


{'c}
(let [a 2 b nil]
  (+ (+ a b) c))

(resolve 'do)

(defn multiplex
  ([completef rf0]
    (fn ([]      (rf0))
        ([x0]    (completef (rf0 x0)))
        ([x0 x'] (rf0 x0 x'))))
  ([completef rf0 rf1]
    (fn ([]           [(rf0) (rf1)])
        ([[x0 x1]]    (completef (rf0 x0) (rf1 x1)))
        ([[x0 x1] x'] [(rf0 x0 x') (rf1 x1 x')]))))

; ----- REFLECTION ----- ;

(defrecord Method [^String name ^Class rtype ^"[Ljava.lang.Class;" argtypes ^clojure.lang.Keyword kind])

(defn class->methods [^Class c]
  (->> (.getMethods c)
       (remove+   (fn [^java.lang.reflect.Method x] (java.lang.reflect.Modifier/isPrivate (.getModifiers x))))
       (map+      (fn [^java.lang.reflect.Method x] (Method. (.getName x) (.getReturnType x) (.getParameterTypes x)
                                                             (if (java.lang.reflect.Modifier/isStatic (.getModifiers x))
                                                                 :static
                                                                 :instance))))
       (group-by  (fn [^Method x] (.-name x))) ; TODO all of these need to be into !vector and !hash-map
       (map-vals+ (fn->> (group-by (fn [^Method x] (count (.-argtypes x))))
                         (map-vals+ (fn->> (group-by (fn [^Method x] (.-kind x)))))
                         (join {})))
       (join {})))

(defmemoized class->methods:with-cache
  {:memoize-only-first? true}
  [c] (class->methods c))

(defrecord Field [^String name ^Class type ^clojure.lang.Keyword kind])

(defn class->fields [^Class c]
  (->> (.getFields c)
       (remove+   (fn [^java.lang.reflect.Field x] (java.lang.reflect.Modifier/isPrivate (.getModifiers x))))
       (map+      (fn [^java.lang.reflect.Field x]
                    [(.getName x)
                     (Field. (.getName x) (.getType x)
                       (if (java.lang.reflect.Modifier/isStatic (.getModifiers x))
                           :static
                           :instance))]))
       (join {}))) ; TODO !hash-map

(defmemoized class->fields:with-cache
  {:memoize-only-first? true}
  [c] (class->fields c))

; ----- TYPED PART ----- ;

(defonce typed-i (atom 0))

(defn add-file-context [to from]
  (let [from-meta (meta from)]
    (update-meta to assoc :line (:line from-meta) :column (:column from-meta))))

(defn persistent!-and-add-file-context [form ast-ret]
  (update ast-ret :form (fn-> persistent! (add-file-context form))))

(def special-symbols '#{let* deftype* do fn* def . if}) ; TODO make more complete

(defn ex! [msg data] (throw (ex-info msg (or data {}))))

(deftype WatchableMutable
  [^:unsynchronized-mutable v ^:unsynchronized-mutable ^clojure.lang.IFn watch]
  clojure.lang.IDeref (deref       [this]      v)
  clojure.lang.IRef   (addWatch    [this _ f]  (set! watch f  ) this)
                      (removeWatch [this _]    (set! watch nil) this)
  clojure.lang.IAtom  (reset       [this newv] (set! v newv)  v)
                      (swap        [this f]
                        (let [oldv v]
                          (set! v (f v))
                          (when (some? watch) (watch nil this oldv v))
                          v))
  Object              (equals      [this that]
                        (and (instance? WatchableMutable that)
                             (= v (.-v ^WatchableMutable that)))))

(defn !ref
  ([v]       (->WatchableMutable v nil))
  ([v watch] (->WatchableMutable v watch)))

(defrecord TypeInfo
  ^{:doc "`reifieds`    is a (minimal) Set<Class> defining the reified type properties of the
                        expression (same as `(apply set/union (vals fn-types))` when TypeInfo
                        of a function call expression). See the `Type Properties` section of
                        the `defnt` documentation for more info.
          `abstracts`   is an optional (minimal) set of clojure.spec-like constraints defining
                        the abstract type properties of the expression. See the `Type Properties`
                        section of the `defnt` documentation for more info.
          `fn-types`    is a Map<Class ... Set<Class>> (e.g. `{A {B {C #{A D B}}}}`)
          `infer?`      denotes whether the type is open to be inferred (`true`) or is fixed (`false`)."}
  [reifieds abstracts fn-types infer?])

#?(:clj
(defmacro ->type-info [m]
  (let [vs (vals (map->TypeInfo m))]
    (when-not (= 4 (count vs)) (ex! "TypeInfo constructor accepts ≤ 4 args; found" {:ct (count m) :map m}))
    `(->TypeInfo ~@vs))))

(defrecord ExpressionInfo
  ^{:doc "`env`         is a map of local bindings: symbol to TypeInfo.
          `form`        is the (unevaluated) form of the expression.
          `type-info`   is a WatchableMutable<TypeInfo>."}
  [env form type-info])

#?(:clj
(defmacro ->expr-info [m]
  (let [vs (vals (map->ExpressionInfo m))]
    (when-not (= 3 (count vs)) (ex! "ExpressionInfo constructor accepts ≤ 3 args; found" {:ct (count m) :map m}))
    `(->ExpressionInfo ~@vs))))

(defn abstracts-of
  "Analyzes and determines the minimal set of abstract type properties of the given form.
   See the `Type Properties` section of the `defnt` documentation for more info."
  [form]
  ; TODO implement
  nil)

(defn most-primitive-class-of [x]
  (let [c (class x)]
    (or (tcore/boxed->unboxed c) c)))

(defn reifieds-of
  "Analyzes and determines the minimal set of reified type properties of the given form.
   See the `Type Properties` section of the `defnt` documentation for more info."
  [form]
  (class form)) ; TODO better to do a set of its `supers`

(defn truthy-type? [t]
  (when (#{Boolean Boolean/TYPE} t)
    (TODO "Don't yet known how to handle booleans"))
  (if (= t :nil)
      false
      true))

(defn truthy-expr? [expr]
  (when (-> expr :constraints some?)
    (TODO "Don't yet know how to handle constraints"))
  (if-let [classes (->> expr :type-info ?deref :reifieds (map truthy-type?) seq)]
    (every-val ::unknown classes)
    ::unknown))

(defn union:type-info [ti0 ti1]
  (prl! ti0 ti1)
  (TODO))

(declare ->typed*)

(defn handle-non-map-seqable
  "Handle a non-map seqable."
  {:params-doc
    '{merge-types-fn "2-arity fn that merges two types (or sets of types).
                      The first argument is the current deduced type of the
                      overall expression; the second is the deduced type of
                      the current subexpression."}}
  [env form empty-form rf]
  (->> form
       (reducei (fn [accum form' i] (rf accum (->typed* (:env accum) form') i))
         (->expr-info {:env env :form (transient empty-form)}))
       (persistent!-and-add-file-context form)))

(defn handle-map
  {:todo #{"If the map is bound to a variable, preserve type info for it such that lookups
            can start out with a guarantee of a certain type."}}
  [env form]
  (->> form
       (reduce-kv (fn [{env' :env forms :form} form'k form'v]
                    (let [ast-ret-k (->typed* env' form'k)
                          ast-ret-v (->typed* env' form'v)]
                      (->expr-info {:env       env'
                                    :form      (assoc! forms (:form ast-ret-k) (:form ast-ret-v))
                                    :type-info nil}))) ; TODO fix; we want the types of the keys and vals to be deduced
         (->expr-info {:env env :form (transient {})}))
       (persistent!-and-add-file-context form)))

(defn handle-seq:do [env body]
  (prl! env body)
  (let [expr (handle-non-map-seqable env body []
               (fn [accum expr _]
                 (prl! accum expr)
                 ;; for types, only the last subexpression ever matters, as each is independent from the others
                 (assoc expr :form (conj! (:form accum) (:form expr))
                             ;; but the env should be the same as whatever it was originally because no new scopes are created
                             :env  (:env accum))))]
  (prl! expr)
    (->expr-info {:env       (:env expr)
                  :form      (cons `do (:form expr))
                  :type-info (:type-info expr)})))

(defn handle-seq:let*:bindings [env bindings]
  (->> bindings
       (partition-all+ 2)
       (reduce (fn [{env' :env forms :form} [sym form]]
                 (let [expr-ret (->typed* env' form)]
                   (->expr-info
                     {:env  (assoc env' sym (->type-info {:reifieds  (:reifieds  expr-ret) ; TODO should use type info or exprinfo?
                                                          :abstracts (:abstracts expr-ret)
                                                          :fn-types  (:fn-types  expr-ret)}))
                      :form (conj! (conj! forms sym) (:form expr-ret))})))
         (->expr-info {:env env :form (transient [])}))
       (persistent!-and-add-file-context bindings)))

(defn handle-seq:let* [env [bindings & body]]
  (let [{env' :env bindings' :form}
          (handle-seq:let*:bindings env bindings)
        {env'' :env body' :form type-info' :type-info}
          (handle-seq:do env' body)]
    (->expr-info {:env       env
                  :form      (list 'let* bindings' body')
                  :type-info type-info'})))

(defn ?resolve-with-env [sym env]
  (when-not (symbol? sym) (ex! "To be resolved, `form` must be a symbol; got" {:form sym}))
  (let [local-info (get env sym)]
    (if (some? local-info)
        (TODO "Need to figure out what to do when resolving local vars")
        (resolve sym))))

(defn handle-seq:dot:field
  [env ^Class target target-form ^Field field field-form]
  (->expr-info {:env       env
                :form      (list `. target-form field-form)
                :type-info (!ref (->type-info {:reifieds #{(.-type field)}}))}))

#_{:?types {[#{int byte boolean} #{int boolean} int ...] boolean}}
; OR
; For `+`
#_{int   {int    long
          long   long
          double double}
   long  {double double}
   float {short  double}
   short {short  int}}

; For `conj!`
#_{ArrayList         {int    ArrayList}}
; (rf accum (->typed* (:env accum) (:info accum) form'))
; (reduce (fn [accum form'] (rf accum (->typed* (:env accum) form')))
;          (->expr-info {:env env :form (transient empty-form)}))

(defn fn-type-satisfies-expr?
  "Yields whether a function type declaration `fn-type` satisfies the type of the given
   expression info `expr`.
   Yields `true` if the intersection of the set of possible argtypes of `f` with the
   set of the possible types of the given arg index is non-empty. This does not imply
   that these sets will actually be generated, but rather that their elements will be
   checked until satisfied or not satisfied.

   See the documentation for `defnt`, specifically the section entitled 'Matching
   Functions with Arguments'."
  [kind fn-type expr i:arg]
  (prl! kind fn-type expr)
  (case kind
    :or
      ; TODO it's not just reifieds here that need taking care of
      ; TODO how to dedupe e.g. subclasses and superclasses?
      (-> expr :type-info (swap! update :reifieds conj (-> fn-type :argtypes (get i:arg))))
    :and
      (TODO)))

(defn handle-seq:dot:methods:static
  "A note will be made of what methods match the argument types.
   If only one method is found, that is noted too. If no matching method is found, an
   exception is thrown."
  {:params-doc '{methods "A reducible of all static `Method`s with the given name,
                          `method-form`, in the given class, `target`."}}
  [env ^Class target target-form methods method-form args]
  (prl! env target method-form (vec methods) args)
  (let [rf (fn [expr arg-expr i:arg]
             (prl! expr arg-expr)
             (update expr :type-info
               (fn-> (or (!ref (->type-info
                                 {:reifieds nil ; delay calculating these until fn-types is done being calculated
                                  :fn-types methods})))
                     (swap!
                       (fn1 update :fn-types
                         (fn [fn-types]
                           (->> fn-types
                                (filter+ #(fn-type-satisfies-expr? :or % arg-expr i:arg))
                                join)))))))
        handled (handle-non-map-seqable env args [] rf)]
    (prl! handled)
    (TODO "calculate `:reifieds` of `handled` based on `:fn-types`")

  (TODO)))

(defn try-handle-seq:dot:methods:static
  [env target target-form method-form args ?field?]
  (if-not-let [methods-for-name (-> target class->methods:with-cache (get (name method-form)))]
    (if ?field?
        (ex! "No such method or field in class" {:class target :method-or-field method-form})
        (ex! "No such method in class"          {:class target :methods         method-form}))
    (if-not-let [methods (get methods-for-name (count args))]
      (ex! "Incorrect number of arguments for method"
           {:class target :method method-form :possible-counts (set (keys methods-for-name))})
      (handle-seq:dot:methods:static env target target-form (:static methods) method-form args))))

(defn handle-seq:dot [env [target method|field & args]]
  (let [target' (?resolve-with-env target env)]
    (cond (class? target')
            (if (empty? args)
                (if-let [field (-> target class->fields:with-cache (get (name method|field)))]
                  (handle-seq:dot:field env target' target field method|field)
                  (try-handle-seq:dot:methods:static env target' target method|field args true))
                (try-handle-seq:dot:methods:static env target' target method|field args false))
          (nil? target')
            (ex! "Could not resolve target of dot operator" {:target target})
          :else
            ;; TODO: here use a similar thing as with static methods in terms of handling fields etc.
            (ex! "Currently doesn't handle non-static calls (instance calls will be supported later).
                  Expected target to be class or object in a dot form; got" {:target target'}))))

(defn handle-nil [env form]
  (->expr-info {:env       env
                :form      form
                :type-info (->type-info {:reifieds #{:nil}})}))

(defn handle-seq:if
  "If `*conditional-branch-pruning?*` is falsey, the dead branch's original form will be
   retained, but it will not be type-analyzed."
  [env [pred true-form false-form :as body]]
  (when (-> body count (not= 3))
    (ex! "`if` accepts exactly 3 arguments: one predicate test and two branches; received" {:body body}))
  (let [pred-expr  (->typed* env pred)
        true-expr  (delay (->typed* env true-form))
        false-expr (delay (->typed* env false-form))
        whole-expr
          (delay
            (->expr-info
              {:env       env
               :form      (list 'if pred (:form @true-expr) (:form @false-expr))
               :type-info (union:type-info (:type-info @true-expr) (:type-info @false-expr))}))]
    (case (truthy-expr? pred-expr)
      ::unknown @whole-expr
      true      (-> @true-expr  (assoc :env env)
                                (cond-> (not *conditional-branch-pruning?*)
                                        (assoc :form (list 'if pred (:form @true-expr) false-form))))
      false     (-> @false-expr (assoc :env env)
                                (cond-> (not *conditional-branch-pruning?*)
                                        (assoc :form (list 'if pred true-form          (:form @false-expr))))))))

(defn handle-seq:quote [env form]
  (->expr-info
    {:env       env
     :form      form
     :type-info (->type-info {:reifieds #{(type form)} :abstracts (abstracts-of form)})}))

(defn handle-seq [env form]
  (prl! form)
  (let [[f & body] (macroexpand form)]
    (prl! f body (special-symbols f))
    (if (special-symbols f)
        (case f
          do
            (if (nil? body)
                (handle-nil env body)
                (handle-seq:do env body))
          let*
            (handle-seq:let* env body)
          deftype*
            (TODO)
          fn*
            (TODO)
          def
            (TODO)
          .
            (handle-seq:dot   env body)
          if
            (handle-seq:if    env body)
          quote
            (handle-seq:quote env body))
        (if-let [f-resolved (resolve f)]
          ; See note above on typed function return types
          (TODO)
          (ex! "Form should be a special symbol but isn't" {:form f})))))

(defn handle-symbol [env form]
  (if-let [type-info (get env form)]
    (->expr-info {:env       env
                  :form      form
                  :type-info type-info})
    (TODO "Need to know how to handle non-local symbol")))

(defn ->typed* [env form]
  (prl! env form)
  (when (> (swap! typed-i inc) 100) (throw (ex-info "Stack too deep" {:form form})))
  (cond (or (keyword? form)
            (string? form)
            (number? form))
          (->expr-info {:env       env
                        :form      form
                        :type-info (->type-info {:reifieds #{(most-primitive-class-of form)}})}) ; TODO `constraints`
        (or (vector? form)
            (set?    form))
          (handle-non-map-seqable env form (empty form))
        (map? form)
          (handle-map env form)
        (seq? form)
          (handle-seq env form)
        (symbol? form)
          (handle-symbol env form)
        (nil? form)
          (handle-nil env form)
        :else
          (throw (ex-info "Unrecognized form" {:form form}))))

(defn ->typed
  ([body] (->typed {} body))
  ([env body]
    (reset! typed-i 0)
    (->typed* env body)))

; To infer, you postwalk
; To imply, you prewalk

'(let [a nil] (Numeric/isZero a))
'(let [a nil] (zero? a))

; TODO do return type inference based on "unified" types
(binding [*print-meta* true]
  (let [position=>param {0 'n}
        param=>position (set/map-invert position=>param)
        ast (clojure.tools.analyzer.jvm/analyze
              (list `let [(update-meta (get position=>param 0) assoc :top-level? true) nil 'a nil 'b nil]
                `(do (Numeric/isZero ~'n) (zero? ~'n) (c/+ ~'a ~'b))))
        nodes-postorder (ast->nodes-postorder ast)]
    #_(prl! ast)
    #_(doseq [child nodes-postorder]
      (quantum.core.print/ppr child)
      (println "========================================")
      (println "----------------------------------------")
      (println "========================================"))
    (doseq [{:as child :keys [op]} nodes-postorder]
      (case op
       #_"TODO:
         - Search for the arg type to be inferred and infer from the env or from the static call / invocation site
         - Try to update the atom containing its type information
           - If the atom contains conflicting information with the deduced type, throw and say so
       "
        :static-call (let [;; subset of `params-to-infer` that are available in the call
                           param-ct (-> child :args count)
                           position=>local-param-ast
                             (->> child
                                  :args
                                  indexed+
                                  (filter+ (fn-and (fn [[_ x]] (contains? param=>position (:form x)))
                                                   (fn [[_ x]] (-> x :env :locals (get (:form x)) :name meta :top-level?))))
                                  (join {}))
                           no-types-to-infer-here (empty? position=>local-param-ast)]
                       (prl! (->> position=>local-param-ast (map-vals+ (fn1 dissoc :env)) (join {})))
                       (when-not no-types-to-infer-here
                         (let [<ret-type+param-types>•
                                (->> (clojure.reflect/reflect (:class child))
                                     :members
                                     (filter+ (fn-and (fn-> :name            (= (:method child)))
                                                      (fn-> :parameter-types count (= param-ct))
                                                      (fn-> :flags           (= #{:public :static}))))
                                     (map+    (fn1 select-keys [:return-type :parameter-types]))
                                     join)]
                           (doseq [[position ast] position=>local-param-ast]
                             (swap! (:atom ast) update :possible-types
                               (fn [types]
                                 (let [param-type=>ret-type
                                        (->> <ret-type+param-types>•
                                             (map+ (fn [m] [(get (:parameter-types m) position)
                                                            (:return-type m)]))
                                             (join {}))]
                                   (if (nil? types)
                                       (do (prl! param-type=>ret-type)
                                           param-type=>ret-type)
                                       (throw (ex-info "Possible types already here. Will handle later"
                                                {:possible-types-prev         types
                                                 :possible-types-to-intersect param-type=>ret-type}))))))))))
        :invoke      (prl! child)
        ; TODO others, like calling maps or sets, etc.
        nil))))

(defn handle-conformed-defnt-args [args]
  (let [bodies (case (-> args :arities first)
                 :arity-1 [(-> args :arities second)]
                 :arity-n (-> args :arities second :bodies))]
    (doseq [{:keys [args body]} bodies]
      (let [body (list* 'do body)]
        ; TODO handle pre/post
        (prl! body))
      )))

#?(:clj (defmacro fnt [& args]))

#?(:clj
(defmacro defnt [& args]
  (let [args' (s/validate args ::defnt)
        ; TODO handle meta, docstring, and name-meta
        _     (handle-conformed-defnt-args args')
        code  `(do (def ~(update-meta (:name args') assoc :typed? true)
                        (fn [& args#]
                          (throw (ex-info "Typed functions not yet supported outside of a typed context"
                                          {:tried-to-call (list* '~(:name args') args#)})))))]
    (binding [clojure.core/*print-meta* true] (prl! code))
    nil)))

(zero? 1)
(defnt zero? ([n ?] (Numeric/isZero n)))

(let [^boolean•I•double z zero?] (.invoke z 3.0)) ; it's just a simple reify

#_(defnt even?
  [n ?] (zero? (bit-and n 1)))
#_=>
(def even? (reify ))

; Normally `zero?` when passed e.g. as a higher-order function might be like

;; ----- Spec'ed `defnt+`s -----

;; One thing that would be nice is to marry `defnt` with `clojure.spec`.
;; We want the specs to be reflected in the parameter declaration, type hints, and so on.
;;
;; We also want it to know about e.g., since a function returns `(< 5 x 100)`, then x must
;; be not just a number, but *specifically* a number between 5 and 100, exclusive.
;; Non-`Collection` datatypes are opaque and do not participate in this benefit (?).
;;
;; core.spec functions like `s/or`, `s/and`, `s/coll-of`, and certain type predicates are
;; able to be leveraged in computing the best overload with the least dynamic dispatch
;; possible.

(defnt example
  ([a (s/and even? #(< 5 % 100))
    b t/any?
    c ::number-between-6-and-20
    d {:req-un [e  (default t/boolean? true)
                :f t/number?
                g  (default (s/or t/number? t/sequential?) 0)]}
    | (< a @c) ; pre
    > (s/and (s/coll odd? :kind t/array?) ; post
             #(= (first %) c))]
   ...)
  ([a string?
    b (s/coll bigdec? :kind vector?)
    c t/any?
    d t/any?
   ...))

;; expands to:

(dv/def ::example:a (s/and even? #(< 5 % 100)))
(dv/def ::example:b t/any)
(dv/def ::example:c ::number-between-6-and-20)
(dv/def-map ::example:d
  :conformer (fn [m#] (assoc-when-not-contains m# :e true :g 0))
  :req-un [[:e t/boolean?]
           [:f t/number?]
           [:g (s/or* t/number? t/sequential?)]])
(dv/def ::example:__ret
  (s/and (s/coll-of odd? :kind t/array?)
                 #(= (first %) (:c ...)))) ; TODO fix `...`

;; -> TODO should it be:
(defnt example
  [^example:a a ^:example:b b ^example:c c ^example:d d]
  (let [ret (do ...)]
    (validate ret ::example:__ret)))
;; -> OR
(defnt example
  [^number? a b ^number? c ^map? d]
  (let [ret (do ...)]
    (validate ret ::example:__ret)))
;; ? The issue is one of performance. Maybe we don't want boxed values all over the place.

(s/fdef example
  :args (s/cat :a ::example:a
               :b ::example:b
               :c ::example:c
               :d ::example:d)
  :fn   ::example:__ret)


;; ----- TYPE INFERENCE ----- ;;

(expr-info '(let [a (Integer. 2) b (Double. 3)] a))
; => {:class java.lang.Integer, :prim? false}
(expr-info '(let [a (Integer. 2) b (Double. 3)] (if false a b)))
; => nil
;    But I'd like to have it infer the "LCD", namely, `(v/and number? (v/or* (fn= 2) (fn= 3)))`.

;; I realize that this also is probably prohibitively expensive.

(expr-info '(let [a (Integer. 2) b (Double. 3)] (if false a (int b))))
; => nil (inferred `Integer` or `int`)

(expr-info '(let [a (Integer. 2) b (Double. 3)] (if false a (Integer. b))))
; => {:class java.lang.Integer, :prim? false}

;; At very least it would be nice to have "spec inference". I.e. know, via `fdef`, that a
;; function meets a particular set of specs/characteristics and so any call to that function
;; will necessarily comply with the type.

;; ----- `->typed` tests ----- ;;

(require '[quantum.core.test :refer [is=]])

(let [gen-unbound
        #(!ref (->type-info
                 {:reifieds #{}
                  :fn-types {}
                  :infer? true}))
      gen-expected
        (fn [form env type-info]
          (->expr-info
           {:env  env
            :form form
            :type-info
              (->type-info type-info)}))
      boolean Boolean/TYPE
      byte    Byte/TYPE
      char    Character/TYPE
      short   Short/TYPE
      int     Integer/TYPE
      long    Long/TYPE
      float   Float/TYPE
      double  Double/TYPE]
  (let [env  {'a (gen-unbound)
              'b (gen-unbound)}
        form '(and:boolean a b)]
    (is= (->typed env form)
         (gen-expected form env
           {:reifieds  #{boolean}
            :abstracts #{...}
            #_:conditionals
              #_{boolean {boolean #{boolean}}}})))
  (let [env  {'a (gen-unbound)}
        form '(Numeric/isZero a)]
    (is= (->typed env form)
         (gen-expected form env
           {:reifieds  #{...}
            :abstracts #{...}
            :conditionals  #_(if* )
              {byte   #{boolean}
               char   #{boolean}
               short  #{boolean}
               int    #{boolean}
               long   #{boolean}
               float  #{boolean}
               double #{boolean}}
            :infer? true})))
  (let [env  {'a (gen-unbound)
              'b (gen-unbound)}
        form '(Numeric/bitAnd a b)]
    (is= (->typed env form)
         (gen-expected form
           {'a (!ref (->type-info
                       {:reifieds #{byte char short int long}
                        :infer? true}))
            'b (!ref (->type-info
                       {:reifieds #{byte char short int long}
                        :infer? true}))}
           {:reifieds  #{byte char short int long}
            :abstracts #{...}
            :conditionals
              {byte  {byte  #{byte }
                      char  #{char }
                      short #{short}
                      int   #{int  }
                      long  #{long }}
               char  {byte  #{char }
                      char  #{char }
                      short #{short}
                      int   #{int  }
                      long  #{long }}
               short {byte  #{short}
                      char  #{short}
                      short #{short}
                      int   #{int  }
                      long  #{long }}
               int   {byte  #{int  }
                      char  #{int  }
                      short #{int  }
                      int   #{int  }
                      long  #{long }}
               long  {byte  #{long }
                      char  #{long }
                      short #{long }
                      int   #{long }
                      long  #{long }}}})))
  (let [env  {'a (gen-unbound)
              'b (gen-unbound)}
        form '(Numeric/negate (Numeric/bitAnd a b))]
    (is= (->typed env form)
         (gen-expected form
           {'a (!ref (->type-info
                       {:reifieds #{}
                        :fn-types {}
                        :infer? true}))
            'b (!ref (->type-info
                       {:reifieds #{}
                        :fn-types {}
                        :infer? true}))}
           {:reifieds  #{byte char short int long}
            :abstracts #{...}})))
  (let [env  {'a (gen-unbound)
              'b (gen-unbound)}
        form '(negate:int|long (Numeric/bitAnd a b))]
    ;; Because the only valid argtypes to `negate:int|long` are S = #{[int] [long]},
    ;; `Numeric/bitAnd` must only accept argtypes that produce a subset of S
    ;; The argtypes to `Numeric/bitAnd` that produce a subset of S are:
    #_#{[byte  int]
        [byte  long]
        [char  int]
        [char  long]
        [short int]
        [short long]
        [int   byte]
        [int   char]
        [int   short]
        [int   int]
        [int   long]
        [long  byte]
        [long  char]
        [long  short]
        [long  int]
        [long  long]}
    ;; So `a`, then, can be:
    #_#{byte char short int long}
    ;; and likewise `b` can be:
    #_#{byte char short int long}
    (is= (->typed env form)
         (gen-expected form
           {'a (!ref (->type-info
                       {:reifieds #{byte char short int long}
                        :fn-types {}
                        :infer? true}))
            'b (!ref (->type-info
                       {:reifieds #{byte char short int long}
                        :fn-types {}
                        :infer? true}))}
           {:reifieds  #{int long}
            :abstracts #{...}}))))
