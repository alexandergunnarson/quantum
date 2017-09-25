(ns quantum.core.defnt
  (:refer-clojure :exclude
    [+ #_zero? odd? even?
     bit-and
     every? vec
     ==
     if-let when-let
     assoc-in
     macroexpand
     get])
  (:require
    [clojure.core             :as c]
    [quantum.core.cache       :as cache
      :refer [defmemoized]]
    [quantum.core.core
      :refer [?deref ->sentinel]]
    [quantum.core.data.set    :as set]
    [quantum.core.error
      :refer [TODO err!]]
    [quantum.core.fn
      :refer [aritoid fn1 fnl fn', fn-> fn->> <-, rcomp
              firsta seconda]]
    [quantum.core.log       :as log
      :refer [ppr! prl! prlm!]]
    [quantum.core.logic
      :refer [fn-and fn-or fn-not if-let if-not-let when-let]]
    [quantum.core.macros
      :refer [macroexpand]]
    [quantum.core.macros.core          :as cmacros]
    [quantum.core.macros.type-hint     :as th]
    [quantum.core.spec                 :as s]
    [quantum.core.specs                :as ss]
    [quantum.core.type.core            :as tcore]
    [quantum.core.untyped.analyze.ast  :as ast]
    [quantum.core.untyped.analyze.expr :as xp]
    [quantum.core.untyped.collections  :as ucoll
      :refer [assoc-in dissoc-if get]]
    [quantum.core.untyped.collections.logic :as ucoll&
      :refer [every?]]
    [quantum.core.untyped.collections.tree :as tree
      :refer [prewalk postwalk walk]]
    [quantum.core.untyped.compare  :as comp
      :refer [==]]
    [quantum.core.untyped.loops    :as loops]
    [quantum.core.untyped.reducers :as r
      :refer [vec map+ map-vals+ mapcat+ filter+ remove+ partition-all+
              join reducei educe]]
    [quantum.core.untyped.type :as t
      :refer [?]]
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

Unlike most static languages, a `nil` value is not considered as having a type
except that of nil.

"

; TODO associative sequence over top of a vector (so it'll display like a seq but behave like a vec)

(do

;; FNT

(s/def ::spec s/any?)

(s/def ::fnt:arg-spec ; TODO expand; make typed destructuring available via ::ss/binding-form
  (s/alt :infer #{'?}
         :any   #{'_}
         :spec  ::spec))

(s/def ::fnt:speced-arg
  (s/cat :arg-binding   (s/and simple-symbol?
                               (set/not #{'& '| '>})
                               (fn-> meta :tag nil?))
         ::fnt:arg-spec ::fnt:arg-spec))

(s/def ::fnt:arglist
  (s/and vector?
         (s/spec
           (s/cat :args    (s/* ::fnt:speced-arg)
                  :varargs (s/? (s/cat :sym (fn1 = '&) ::fnt:speced-arg ::fnt:speced-arg))
                  :pre     (s/? (s/cat :sym (fn1 = '|) ::spec           ::spec))
                  :post    (s/? (s/cat :sym (fn1 = '>) ::spec           ::spec))))
         (s/conformer
           #(cond-> % (contains? % :varargs) (update :varargs ::fnt:speced-arg)
                      (contains? % :pre    ) (update :pre     ::spec)
                      (contains? % :post   ) (update :post    ::spec)))))

(s/def ::fnt:body (s/alt :body (s/* s/any?)))

(s/def ::fnt:arglist+body
  (s/cat ::fnt:arglist ::fnt:arglist :body ::fnt:body))

(s/def ::fnt:arities
  (s/alt :arity-1 ::fnt:arglist+body
         :arity-n (s/cat :arities (s/+ (s/spec ::fnt:arglist+body)))))

(s/def ::fnt:postchecks
  (s/conformer
    (fn1 update :arities (fnl mapv (fn1 update :body :body)))))

(s/def ::fnt
  (s/and (s/spec
           (s/cat
             ::ss/fn:name   (s/? ::ss/fn:name)
             ::ss/docstring (s/? ::ss/docstring)
             ::ss/meta      (s/? ::ss/meta)
             :arities       ::fnt:arities))
         ::ss/fn:postchecks
         ::fnt:postchecks))

(s/def ::fns:code ::fnt)

(s/def ::defnt
  (s/and (s/spec
           (s/cat
             ::ss/fn:name   ::ss/fn:name
             ::ss/docstring (s/? ::ss/docstring)
             ::ss/meta      (s/? ::ss/meta)
             :arities       ::fnt:arities))
         ::ss/fn:postchecks
         ::fnt:postchecks))

(s/def ::defns:code ::defnt)

(defonce *pred->class (atom {}))

#?(:clj
(defn pred->class [lang pred]
  (assert (symbol? pred))
  (get-in @*pred->class [(cmacros/syntax-quoted:sym pred) lang])))

#?(:clj
(defn def-pred->class [pred:sym c-clj c-cljs]
  (assert (symbol? pred:sym))
  (assert (or c-clj c-cljs))
  (assert (or (symbol? c-clj)  (nil? c-clj)))
  (assert (or (symbol? c-cljs) (nil? c-cljs)))
  (swap! *pred->class assoc
    (cmacros/syntax-quoted:sym pred:sym)
    {:clj  (cmacros/syntax-quoted:sym c-clj)
     :cljs c-cljs})))

#?(:clj (def-pred->class 'class?  'Class                       nil))
#?(:clj (def-pred->class 'string? 'String                      'js/String))
#?(:clj (def-pred->class 'map?    'clojure.lang.IPersistentMap nil))
#?(:clj (def-pred->class 'set?    'clojure.lang.IPersistentSet nil))

(defn *fns:code [kind lang args]
  (let [{:keys [::ss/fn:name arities ::ss/meta] :as args'}
          (s/validate args (case kind :defn ::defns:code :fn ::fns:code))
        arity-data->arity
          (fn [{{:keys [args varargs pre post]} ::fnt:arglist
                body :body}]
            (let [arg-spec->validation
                    (fn [{[k spec] ::fnt:arg-spec :keys [arg-binding]}]
                      ;; TODO this validation is purely temporary until destructuring is supported
                      (s/validate arg-binding simple-symbol?)
                      (case k
                        :any   nil
                        :infer (do (log/pr :warn "Spec inference not yet supported in `defns`. Ignoring request to infer" arg-binding)
                                   nil)
                        :spec  (if-let [c:sym (or (some-> spec th/?tag->class th/class->instance?-safe-tag:sym)
                                                  (and (symbol? spec) (pred->class lang spec)))]
                                 (list `instance? c:sym arg-binding)
                                 (list `s/validate arg-binding spec))))
                  spec-validations
                    (concat (map arg-spec->validation args)
                            (some-> varargs arg-spec->validation))
                  ;; TODO if an arg has been primitive-type-hinted in the `fn` arglist, then no need to do an `instance?` check
                  ?hint-arg
                    (fn [{[k spec] ::fnt:arg-spec :keys [arg-binding]}]
                      (if (not= k :spec)
                          arg-binding
                          (th/with-type-hint arg-binding
                            (if-let [c (th/?tag->class spec)]
                              (th/->fn-arglist-tag c lang (count args) varargs)
                              (and (symbol? spec) (pred->class lang spec))))))
                  arglist'
                    (->> args
                         (map+ ?hint-arg)
                         join
                         (<- cond-> varargs (conj '& (?hint-arg varargs))))
                  validations
                    (->> [(when post {:post [post]})
                          (some->> spec-validations (filter some?) seq (list* 'do))
                          (when pre (list 'assert pre))]
                         (filter some?))]
              (list* arglist' (concat validations body))))
        arities (mapv arity-data->arity arities)
        code (case kind
               :fn   (list* 'fn (concat
                                  (if (contains? args' ::ss/fn:name)
                                      [fn:name]
                                      [])
                                  [arities]))
               :defn (list* 'defn fn:name arities))]
    code))

(defmacro fns
  "Like `fnt`, but relies on runtime spec checks.
   Does not perform type inference (at least not yet).
   Also does not currently handle spec checks in
   destructuring contexts yet."
  [& args]
  (*fns:code :fn (cmacros/env-lang) args))

(defmacro defns
  "Like `defnt`, but relies on runtime spec checks.
   Does not perform type inference (at least not yet).
   Also does not currently handle spec checks in
   destructuring contexts yet."
  [& args]
  (*fns:code :defn (cmacros/env-lang) args))

(defns abcde ""
  ([a ? b _ > integer?] {:pre 1} 1 2)
  ([c string?, d StringBuilder & e _ > number?]
    (.substring c 0 1)
    3 4)
  ([f long]
    (c/+ f 1)))
)

; ----- TYPED PART ----- ;

(do

;; NOTE: All this code can be defnt-ized after; this is just for bootstrapping purposes so performance isn't extremely important in most of these functions.

(defonce defnt-cache (atom {})) ; TODO For now — but maybe lock-free concurrent hash map to come

(defn infer-arg-types
  [arglist body]
  (clojure.tools.analyzer.jvm/analyze body))

(defn ast->nodes-postorder
  "Walks the ast and outputs an eager sequence containing its AST nodes ordered from leaf
   to branch (i.e. postorder)."
  [ast]
  (tree/tree-sequence-postwalk
    :op
    (fn [op] (->> op :children
                  (mapcat+ #(let [children|op (get op %)]
                               (if (sequential? children|op)
                                   children|op
                                   [children|op])))))
    ast))

; ----- REFLECTION ----- ;

(defrecord Method [^String name ^Class rtype ^"[Ljava.lang.Class;" argtypes ^clojure.lang.Keyword kind]
  fipp.ednize/IOverride
  fipp.ednize/IEdn (-edn [this] (tagged-literal (symbol "M") (into (array-map) this))))

(defns class->methods [c class?]
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

(defrecord Field [^String name ^Class type ^clojure.lang.Keyword kind]
  fipp.ednize/IOverride
  fipp.ednize/IEdn (-edn [this] (tagged-literal (symbol "F") (into (array-map) this))))

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


(def ^:dynamic *conditional-branch-pruning?* true)

(defonce *analyze-i (atom 0))

(defn add-file-context [to from]
  (let [from-meta (meta from)]
    (update-meta to assoc :line (:line from-meta) :column (:column from-meta))))

(defn persistent!-and-add-file-context [form ast-ret]
  (update ast-ret :form (fn-> persistent! (add-file-context form))))

(def special-symbols '#{let* deftype* do fn* def . if}) ; TODO make more complete

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
                             (= v (.-v ^WatchableMutable that))))
  fipp.ednize/IOverride
  fipp.ednize/IEdn    (-edn [this] (tagged-literal (symbol "!@") v)))

(defn !ref
  ([v]       (->WatchableMutable v nil))
  ([v watch] (->WatchableMutable v watch)))

(defrecord TypeInfo
  ^{:doc "`reifieds`     is a minimal disjunctive Set<Class> defining the reified type properties
                         of the expression (same as `(apply set/union (vals fn-types))` when TypeInfo
                         of a function call expression).
                         For example, #{`long` `int` `byte`} means that the expression is either
                         a long, `int`, or `byte`.
          `abstracts`    is an optional minimal conjunctive set of clojure.spec-like constraints
                         defining the abstract type properties of the expression.
                         For example, #{`pos?` `double?`} means the expression exhibits the
                         characteristics of a positive double.
          TODO
          `conditionals` is a nested map structure describing conditional reifieds/abstracts
                         For example:
                         ```
                         {byte  #{boolean},
                          char  #{boolean},
                          short #{String}}
                         ```
                         means that if the expression is determined to be a `byte`, its `reifieds`
                         will be `#{boolean}` (meaning, the expression .........)
          `fn-types`    is a Map<Class ... Set<Class>> (e.g. `{A {B {C #{A D B}}}}`)
          `infer?`      denotes whether the type is open to be inferred (`true`) or is fixed (`false`)."}
  [reifieds abstracts fn-types ^boolean infer?]
  fipp.ednize/IOverride
  fipp.ednize/IEdn (-edn [this] (tagged-literal (symbol "TYPE") (into (array-map) this))))

#?(:clj
(defn ->type-info [m]
  (-> m
      (update :infer?
        #(cond (boolean? %) %
               (nil? %)     false
               :else        (err! "`:infer?` must be boolean; got" {:x %})))
      map->TypeInfo)))

(defrecord ExpressionInfo
  ^{:doc "`env`         is a map of local bindings: symbol to TypeInfo.
          `form`        is the unevaluated, fully macro-expanded form of the expression.
          `type-info`   is a WatchableMutable<TypeInfo>."}
  [env form type-info]
  fipp.ednize/IOverride
  fipp.ednize/IEdn
  (-edn [this] (tagged-literal (symbol "EXPR") (into (array-map) this))))

#?(:clj
(defmacro ->expr-info [m]
  (let [vs (vals (map->ExpressionInfo m))]
    (when-not (= 3 (count vs)) (err! "ExpressionInfo constructor accepts ≤ 3 args; found" {:ct (count m) :map m}))
    `(->ExpressionInfo ~@vs))))

(defn abstracts-of
  "Analyzes and determines the minimal conjunctive set of abstract type properties of the given form.
   See the `Type Properties` section of the `defnt` documentation for more info."
  [form]
  (TODO "implement")
  nil)

(defn reifieds-of
  "Analyzes and determines the minimal disjunctive set of reified type properties of the given form.
   See the `Type Properties` section of the `defnt` documentation for more info."
  [form]
  #{(class form)}) ; TODO better to do a set of its `supers`

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
    (ucoll&/every-val ::unknown classes)
    ::unknown))

(defn union:type-info [ti0 ti1]
  (prl! ti0 ti1)
  (TODO))

(declare analyze*)

(defn analyze-non-map-seqable
  "Analyzes a non-map seqable."
  {:params-doc
    '{merge-types-fn "2-arity fn that merges two types (or sets of types).
                      The first argument is the current deduced type of the
                      overall expression; the second is the deduced type of
                      the current subexpression."}}
  [env form empty-form rf]
  (prl! env form empty-form)
  (->> form
       (reducei (fn [accum form' i] (rf accum (analyze* (:env accum) form') i))
         (->expr-info {:env env :form (transient empty-form)}))
       (persistent!-and-add-file-context form)))

(defn analyze-map
  {:todo #{"If the map is bound to a variable, preserve type info for it such that lookups
            can start out with a guarantee of a certain type."}}
  [env form]
  (->> form
       (reduce-kv (fn [{env' :env forms :form} form'k form'v]
                    (let [ast-ret-k (analyze* env' form'k)
                          ast-ret-v (analyze* env' form'v)]
                      (->expr-info {:env       env'
                                    :form      (assoc! forms (:form ast-ret-k) (:form ast-ret-v))
                                    :type-info nil}))) ; TODO fix; we want the types of the keys and vals to be deduced
         (->expr-info {:env env :form (transient {})}))
       (persistent!-and-add-file-context form)))

(defn analyze-seq:do [env form body]
  (prl! env body)
  (if (empty? body)
      (ast/do {:env  env
               :form form
               :body (r/vec body)
               :spec t/nil?})
      (let [_ (TODO "fix `do`")
            expr (analyze-non-map-seqable env body []
                   (fn [accum expr _]
                     (prl! accum expr)
                     ;; for types, only the last subexpression ever matters, as each is independent from the others
                     (assoc expr :form (conj! (:form accum) (:form expr))
                                 ;; but the env should be the same as whatever it was originally because no new scopes are created
                                 :env  (:env accum))))]
        (prl! expr)
        (ast/do {:env  env
                 :form form
                 :body (r/vec body)
                 :spec (:spec expr)}))))

(defn analyze-seq:let*:bindings [env bindings]
  (TODO "`let*:bindings` analysis")
  (->> bindings
       (partition-all+ 2)
       (reduce (fn [{env' :env forms :form} [sym form]]
                 (let [expr-ret (analyze* env' form)]
                   (->expr-info
                     {:env  (assoc env' sym (->type-info {:reifieds  (:reifieds  expr-ret) ; TODO should use type info or exprinfo?
                                                          :abstracts (:abstracts expr-ret)
                                                          :fn-types  (:fn-types  expr-ret)}))
                      :form (conj! (conj! forms sym) (:form expr-ret))})))
         (->expr-info {:env env :form (transient [])}))
       (persistent!-and-add-file-context bindings)))

(defn analyze-seq:let* [env [bindings & body]]
  (let [{env' :env bindings' :form}
          (analyze-seq:let*:bindings env bindings)
        {env'' :env body' :form type-info' :type-info}
          (analyze-seq:do env' body)]
    (->expr-info {:env       env
                  :form      (list 'let* bindings' body')
                  :type-info type-info'})))

(defns ?resolve-with-env
  [sym symbol?, env _]
  (let [local-info (get env sym)]
    (if (some? local-info)
        (TODO "Need to figure out what to do when resolving local vars")
        (resolve sym))))

(defns analyze-seq:dot:field
  [env _, target Class, target-form _, field Field, field-form _]
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

(defn methods->spec
  "Creates a spec given ->`methods`."
  [methods #_(t/seq method?)]
  ;; TODO room for plenty of optimization here
  (let [methods:by-ct (->> methods (group-by (fn-> :argtypes count)))
        partition-deep
          (fn partition-deep [spec methods' arglist-size i:arg depth]
            (let [_ (when (> depth 3) (TODO))
                  methods':by-class
                    (->> methods'
                         ;; TODO optimize further via `group-by-into`
                         (group-by (fn-> :argtypes (get i:arg)))
                         ;; classes will be sorted from most to least specific
                         (sort-by first t/in>))]
              (r/for [[c methods''] methods':by-class
                      spec' spec]
                (update spec' :clauses conj
                  [(t/->spec c)
                   (if (= (inc depth) arglist-size)
                       ;; here, methods'' count will be = 1
                       (-> methods'' first :rtype t/->spec)
                       (partition-deep
                         (xp/condpf-> t/>= (xp/get (inc i:arg)))
                         methods''
                         arglist-size
                         (inc i:arg)
                         (inc depth)))]))))]
    (r/for [[ct methods'] methods:by-ct
            spec (xp/casef count)]
      (if (zero? ct)
          (assoc-in spec [:cases 0]  (-> methods' first :rtype t/->spec))
          (assoc-in spec [:cases ct] (partition-deep (xp/condpf-> t/>= (xp/get 0)) methods' ct 0 0))))))

(defns analyze-seq:dot:methods:static
  "A note will be made of what methods match the argument types.
   If only one method is found, that is noted too. If no matching method is found, an
   exception is thrown."
  {:params-doc '{methods "A reducible of all static `Method`s with the given name,
                          `method-form`, in the given class, `target`."}}
  [env _, form _, target class?, target-form _, methods _, method-form _, arg-forms _]
  ;; TODO cache spec by method
  (prl! env target method-form #_(vec methods) arg-forms)
  (let [;; TODO could use `analyze-non-map-seqable` instead of the `reduce` here
        #_analyzed #_(-> (analyze-non-map-seqable env arg-forms [] rf)
                         (assoc :form form))
        args-ct (count arg-forms)
        static-call0
          (ast/static-call
            {:env  env
             :form form
             :f    (symbol (str target-form) (str method-form))
             :args []
             :spec (methods->spec methods)})
        with-arg-specs
          (r/fori [arg-form    arg-forms
                   static-call static-call0
                   i|arg]
            (prl! static-call arg-form)
            (let [arg-node (analyze* env arg-form)]
              ;; TODO can incrementally calculate return value, but possibly not worth it
              (update static-call :args conj arg-node)))
        with-ret-spec
          (update with-arg-specs :spec
            (fn [spec]
              (spec (->> with-arg-specs :args (mapv :spec)))
              #_(or (err! "Incompatible specs" {:spec0 spec :spec1 (:spec arg-node)}))))]
    with-ret-spec))

(defn try-analyze-seq:dot:methods:static
  [env form target target-form method-form args ?field?]
  (if-not-let [methods-for-name (-> target class->methods:with-cache (get (name method-form)))]
    (if ?field?
        (err! "No such method or field in class" {:class target :method-or-field method-form})
        (err! "No such method in class"          {:class target :methods         method-form}))
    (if-not-let [methods (get methods-for-name (count args))]
      (err! "Incorrect number of arguments for method"
            {:class target :method method-form :possible-counts (set (keys methods-for-name))})
      (analyze-seq:dot:methods:static env form target target-form (:static methods) method-form args))))

(defn analyze-seq:dot [env form [target method|field & args]]
  {:post [(prl! %)]}
  (let [target' (?resolve-with-env target env)]
    (cond (class? target')
            (if (empty? args)
                (if-let [field (-> target class->fields:with-cache (get (name method|field)))]
                  (analyze-seq:dot:field env form target' target field method|field)
                  (try-analyze-seq:dot:methods:static env form target' target method|field args true))
                (try-analyze-seq:dot:methods:static env form target' target method|field args false))
          (nil? target')
            (err! "Could not resolve target of dot operator" {:target target})
          :else
            ;; TODO: here use a similar thing as with static methods in terms of handling fields etc.
            (err! "Currently doesn't handle non-static calls (instance calls will be supported later).
                   Expected target to be class or object in a dot form; got" {:target target'}))))

(defn analyze-seq:if
  "If `*conditional-branch-pruning?*` is falsey, the dead branch's original form will be
   retained, but it will not be type-analyzed."
  [env form [pred true-form false-form :as body]]
  {:post [(prl! %)]}
  (when (-> body count (not= 3))
    (err! "`if` accepts exactly 3 arguments: one predicate test and two branches; received" {:body body}))
  (let [pred-expr  (analyze* env pred)
        true-expr  (delay (analyze* env true-form))
        false-expr (delay (analyze* env false-form))
        whole-expr
          (delay
            (do (TODO "fix `if` analysis")(->expr-info
              {:env       env
               :form      (list 'if pred (:form @true-expr) (:form @false-expr))
               :type-info (union:type-info (:type-info @true-expr) (:type-info @false-expr))})))]
    (case (truthy-expr? pred-expr)
      ::unknown @whole-expr
      true      (-> @true-expr  (assoc :env env)
                                (cond-> (not *conditional-branch-pruning?*)
                                        (assoc :form (list 'if pred (:form @true-expr) false-form))))
      false     (-> @false-expr (assoc :env env)
                                (cond-> (not *conditional-branch-pruning?*)
                                        (assoc :form (list 'if pred true-form          (:form @false-expr))))))))

(defn analyze-seq:quote [env form body]
  {:post [(prl! %)]}
  (ast/quoted form (tcore/most-primitive-class-of body)))

(defn analyze-seq*
  "Analyze a seq after it has been macro-expanded.
   The ->`form` is post- incremental macroexpansion."
  [env [sym & body :as form]]
  (ppr! {'expanded-form form})
  (if (special-symbols sym)
      (case sym
        do
          (analyze-seq:do    env form body)
        let*
          (analyze-seq:let*  env form body)
        deftype*
          (TODO "deftype*")
        fn*
          (TODO "fn*")
        def
          (TODO "def")
        .
          (analyze-seq:dot   env form body)
        if
          (analyze-seq:if    env form body)
        quote
          (analyze-seq:quote env form body))
      (if-let [sym-resolved (resolve sym)]
        ; See note above on typed function return types
        (TODO)
        (err! "Form should be a special symbol but isn't" {:form sym}))))

(defn analyze-seq [env form]
  {:post [(prl! %)]}
  (prl! form)
  (let [expanded-form (macroexpand form)]
    (if (== form expanded-form)
        (analyze-seq* env expanded-form)
        (ast/macro-call {:form form :expanded (analyze-seq* env expanded-form)}))))

(defn analyze-symbol [env form]
  {:post [(prl! %)]}
  (or (get env form)
      (TODO "Need to know how to handle non-local symbol")))

(defn analyze* [env form]
  (prl! env form)
  (when (> (swap! *analyze-i inc) 100) (throw (ex-info "Stack too deep" {:form form})))
  (cond (symbol? form)
          (analyze-symbol env form)
        (t/literal? form)
          (ast/literal form (-> form tcore/most-primitive-class-of t/->spec))
        (or (vector? form)
            (set?    form))
          (analyze-non-map-seqable env form (empty form))
        (map? form)
          (analyze-map env form)
        (seq? form)
          (analyze-seq env form)
        :else
          (throw (ex-info "Unrecognized form" {:form form}))))

(defn analyze
  ([body] (analyze {} body))
  ([env body]
    (reset! *analyze-i 0)
    (analyze* env body)))

; To infer, you postwalk
; To imply, you prewalk



)


; TODO by default, :form+ is added, but have option to hide :form+ that are the same
; TODO by default, :env is added, but have the option to hide it

(->typed '{a nil}
  '(Numeric/isZero a))

'(let [a nil] (zero? a))

(->typed {'c nil}
  '(let [a 2 b nil]
     (+ (+ a b) c)))


{'c}
(let [a 2 b nil]
  (+ (+ a b) c))

(resolve 'do)

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

(require '[quantum.core.test :refer [is= testing is]])


