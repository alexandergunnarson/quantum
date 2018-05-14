(ns quantum.core.defnt
  (:refer-clojure :exclude
    [+ #_zero? odd? even?
     bit-and
     ==
     macroexpand])
  (:require
    [clojure.core                      :as core]
    [clojure.string                    :as str]
    [quantum.core.error                :as err
      :refer [TODO err!]]
    [quantum.core.fn
      :refer [aritoid fn1 fnl fn', fn-> fn->> <-, rcomp
              firsta seconda]]
    [quantum.core.log                  :as log
      :refer [ppr! ppr prl! prlm!]]
    [quantum.core.logic                :as l
      :refer [fn= fn-and fn-or fn-not ifs if-not-let]]
    [quantum.core.macros
      :refer [macroexpand]]
    [quantum.core.print                :as pr]
    [quantum.core.type.core            :as tcore]
    [quantum.core.type.defs            :as tdef]
    [quantum.untyped.core.analyze.ast  :as ast]
    [quantum.untyped.core.analyze.expr :as xp]
    [quantum.untyped.core.analyze.rewrite :as ana-rw]
    [quantum.untyped.core.collections  :as c
      :refer [dissoc-if dissoc* lcat subview >vec >set
              lmap map+ map-vals+ mapcat+ filter+ remove+ partition-all+]]
    [quantum.untyped.core.collections.logic :as ucl
      :refer [seq-and seq-or]]
    [quantum.untyped.core.collections.tree :as tree
      :refer [prewalk postwalk walk]]
    [quantum.untyped.core.compare  :as comp
      :refer [==]]
    [quantum.untyped.core.convert  :as conv
      :refer [>symbol >name]]
    [quantum.untyped.core.core
      :refer [istr]]
    [quantum.untyped.core.data
      :refer [kw-map]]
    [quantum.untyped.core.data.map        :as map]
    [quantum.untyped.core.data.set        :as set]
    [quantum.untyped.core.defnt
      :refer [defns defns- fns]]
    [quantum.untyped.core.form            :as uform]
    [quantum.untyped.core.form.evaluate   :as ufeval]
    [quantum.untyped.core.form.generate   :as ufgen
      :refer [unify-gensyms]]
    [quantum.untyped.core.form.type-hint  :as ufth]
    [quantum.untyped.core.loops           :as loops
      :refer [reduce-2]]
    [quantum.untyped.core.numeric.combinatorics :as combo]
    [quantum.untyped.core.qualify         :as qual :refer [qualify]]
    [quantum.untyped.core.reducers        :as r
      :refer [join reducei educe]]
    [quantum.untyped.core.refs            :as ref
      :refer [?deref]]
    [quantum.untyped.core.spec            :as s]
    [quantum.untyped.core.specs           :as uss]
    [quantum.untyped.core.type            :as t
      :refer [?]]
    [quantum.untyped.core.type.predicates :as utpred]
    [quantum.untyped.core.vars            :as var
      :refer [update-meta]]
    [quantum.format.clojure.core ; TODO temporary
      :refer [reformat-string]])
  (:import
    [quantum.core Numeric]
    [quantum.untyped.core.type ClassSpec]))

;; TODO move
(defn ppr-code [code]
  (let [default-indentations '{do [[:inner 2 2]]
                               if [[:inner 2 2]]}]
    (-> code pr/ppr-meta with-out-str
        (reformat-string {:indents default-indentations})
        println)))

#_(:clj (ns-unmap (find-ns 'quantum.core.defnt) 'reformat-string))

;; TODO look at https://github.com/clojure/core.typed/blob/master/module-rt/src/main/clojure/clojure/core/typed/

;; Apparently I independently came up with an algorithm that is essentially Hindley-Milner (having not seen it before I implemented it)...
;; - `dotyped`, `defnt`, and `fnt` create typed contexts in which their internal forms are analyzed
;; and overloads are resolved.
;; - When a function with type overloads is referenced outside of a typed context, then the
;; overload resolution will be done via protocol dispatch unless the function's overloads only
;; differ by arity. In either case, runtime type checks are required.
;; - At some later date, the analyzer will do its best to infer types.
;; - Even if the `defnt` is redefined, you won't have interface problems.

;; Any `defnt` argument, if it requires a non-nilable primitive-like value, will be marked as a primitive.
;; If nilable, it will be boxed.
;; The same thing goes for return values.
;; All other cases of primitiveness fall out of these two.

;; `defnt` is meant to enforce syntactic correctness.
;; It is intended to catch many runtime errors at compile time, but cannot catch
;; all of them; specs will very often have to be validated at runtime.

; TODO associative sequence over top of a vector (so it'll display like a seq but behave like a vec)

#?(:clj
(defns class>simplest-class
  "This ensures that special overloads are not created for non-primitive subclasses
   of java.lang.Object (e.g. String, etc.)."
  [c (? t/class?) > (? t/class?)]
  (if (t/primitive-class? c)
      c
      (or (tcore/boxed->unboxed c) java.lang.Object))))

#?(:clj
(defns class>most-primitive-class [c (? t/class?), nilable? t/boolean? > (? t/class?)]
  (if nilable? c (or (tcore/boxed->unboxed c) c))))

#?(:clj
(defns spec>most-primitive-classes [spec t/spec? > (s/set-of (? t/class?))]
  (let [cs (t/spec>classes spec) nilable? (contains? cs nil)]
    (->> cs
         (c/map+ #(class>most-primitive-class % nilable?))
         (join #{})))))

#?(:clj
(defns spec>most-primitive-class [spec t/spec? > (? t/class?)]
  (let [cs (spec>most-primitive-classes spec)]
    (if (-> cs count (not= 1))
        (err! "Not exactly 1 class found" (kw-map spec cs))
        (first cs)))))

#?(:clj
(defns out-spec>class [spec t/spec? > (? t/class?)]
  (let [cs (t/spec>classes spec) cs' (disj cs nil)]
    (if (-> cs' count (not= 1))
        ;; NOTE: we don't need to vary the output class if there are multiple output possibilities or just nil
        java.lang.Object
        (-> (class>most-primitive-class (first cs') (contains? cs nil))
            class>simplest-class)))))

; ----- TYPED PART ----- ;

;; NOTE: All this code can be defnt-ized after; this is just for bootstrapping purposes so performance isn't extremely important in most of these functions.

(defonce *fn->spec (atom {}))

(defonce defnt-cache (atom {})) ; TODO For now — but maybe lock-free concurrent hash map to come

(defonce *interfaces (atom {}))

; ----- REFLECTION ----- ;

#?(:clj
(defrecord Method [^String name ^Class rtype ^"[Ljava.lang.Class;" argtypes ^clojure.lang.Keyword kind]
  fipp.ednize/IOverride
  fipp.ednize/IEdn (-edn [this] (tagged-literal (symbol "M") (into (array-map) this)))))

#?(:clj (defns method? [x _] (instance? Method x)))

#?(:clj
(defns class->methods [^Class c t/class? > t/map?]
  (->> (.getMethods c)
       (remove+   (fn [^java.lang.reflect.Method x] (java.lang.reflect.Modifier/isPrivate (.getModifiers x))))
       (map+      (fn [^java.lang.reflect.Method x] (Method. (.getName x) (.getReturnType x) (.getParameterTypes x)
                                                             (if (java.lang.reflect.Modifier/isStatic (.getModifiers x))
                                                                 :static
                                                                 :instance))))
       (c/group-by (fn [^Method x] (.-name x))) ; TODO all of these need to be into !vector and !hash-map
       (map-vals+  (fn->> (c/group-by (fn [^Method x] (count (.-argtypes x))))
                          (map-vals+  (fn->> (c/group-by (fn [^Method x] (.-kind x)))))
                          (join {})))
       (join {}))))

(defonce class->methods|with-cache
  (memoize (fn [c] (class->methods c))))

(defrecord Field [^String name ^Class type ^clojure.lang.Keyword kind]
  fipp.ednize/IOverride
  fipp.ednize/IEdn (-edn [this] (tagged-literal (symbol "F") (into (array-map) this))))

(defns class->fields [^Class c t/class? > t/map?]
  (->> (.getFields c)
       (remove+   (fn [^java.lang.reflect.Field x] (java.lang.reflect.Modifier/isPrivate (.getModifiers x))))
       (map+      (fn [^java.lang.reflect.Field x]
                    [(.getName x)
                     (Field. (.getName x) (.getType x)
                       (if (java.lang.reflect.Modifier/isStatic (.getModifiers x))
                           :static
                           :instance))]))
       (join {}))) ; TODO !hash-map

(def class->fields|with-cache
  (memoize (fn [c] (class->fields c))))

(def ^:dynamic *conditional-branch-pruning?* true)

(defonce *analyze-i (atom 0))

(defn add-file-context [to from]
  (let [from-meta (meta from)]
    (update-meta to assoc :line (:line from-meta) :column (:column from-meta))))

(defn persistent!-and-add-file-context [form ast-ret]
  (update ast-ret :form (fn-> persistent! (add-file-context form))))

(def special-symbols '#{do let* deftype* fn* def . if quote new throw}) ; TODO make more complete

;; TODO move
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

;; TODO move
(defn !ref
  ([v]       (->WatchableMutable v nil))
  ([v watch] (->WatchableMutable v watch)))

(s/def ::env (s/map-of t/symbol? t/any?))

(declare analyze*)

(defns- analyze-non-map-seqable
  "Analyzes a non-map seqable."
  {:params-doc
    '{merge-types-fn "2-arity fn that merges two types (or sets of types).
                      The first argument is the current deduced type of the
                      overall expression; the second is the deduced type of
                      the current subexpression."}}
  [env ::env, form _, empty-form _, rf _]
  (prl! env form empty-form)
  (->> form
       (reducei (fn [accum form' i] (rf accum (analyze* (:env accum) form') i))
         {:env env :form (transient empty-form)})
       (persistent!-and-add-file-context form)))

(defns- analyze-map
  {:todo #{"If the map is bound to a variable, preserve type info for it such that lookups
            can start out with a guarantee of a certain type."}}
  [env ::env, form _]
  (TODO "analyze-map")
  #_(->> form
       (reduce-kv (fn [{env' :env forms :form} form'k form'v]
                    (let [ast-ret-k (analyze* env' form'k)
                          ast-ret-v (analyze* env' form'v)]
                      (->expr-info {:env       env'
                                    :form      (assoc! forms (:form ast-ret-k) (:form ast-ret-v))
                                    :type-info nil}))) ; TODO fix; we want the types of the keys and vals to be deduced
         (->expr-info {:env env :form (transient {})}))
       (persistent!-and-add-file-context form)))

(defns- analyze-seq|do [env ::env, form _, body _]
  (prl! env body)
  (if (empty? body)
      (ast/do {:env  env
               :form form
               :body (>vec body)
               :spec t/nil?})
      (let [expr (analyze-non-map-seqable env body []
                   (fn [accum expr _]
                     ;; for types, only the last subexpression ever matters, as each is independent from the others
                     (assoc expr :form (conj! (:form accum) (:form expr))
                                 ;; but the env should be the same as whatever it was originally because no new scopes are created
                                 :env  (:env accum))))]
        (ast/do {:env  env
                 :form form
                 :body (>vec body)
                 :spec (:spec expr)}))))

(defns analyze-seq|let*|bindings [env ::env, bindings _]
  (TODO "`let*|bindings` analysis")
  #_(->> bindings
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

(defns analyze-seq|let* [env ::env, [bindings _ & body _] _]
  (TODO "`let*` analysis")
  #_(let [{env' :env bindings' :form}
          (analyze-seq|let*|bindings env bindings)
        {env'' :env body' :form type-info' :type-info}
          (analyze-seq|do env' body)]
    (->expr-info {:env       env
                  :form      (list 'let* bindings' body')
                  :type-info type-info'})))

(defns ?resolve-with-env
  [sym t/symbol?, env ::env]
  (let [local (c/get env sym)]
    (if (some? local)
        (if (ast/unbound? local)
            local
            (TODO "Need to figure out what to do when resolving local vars"))
        (let [resolved (ns-resolve *ns* sym)]
          (log/ppr :warn "Not sure how to handle non-local symbol; resolved it for now" (kw-map sym resolved))
          resolved))))

(defns methods->spec
  "Creates a spec given ->`methods`."
  [methods (s/seq-of method?) > t/spec?]
  ;; TODO room for plenty of optimization here
  (let [methods|by-ct (->> methods
                           (c/group-by (fn-> :argtypes count))
                           (sort-by first <))
        ;; non-primitive classes in Java aren't guaranteed to be non-null
        >class-spec (fn [x]
                      (ifs (class? x)
                             (-> x t/>spec (cond-> (not (t/primitive-class? x)) t/?))
                           (t/spec? x)
                             x
                           (err/not-supported! `>class-spec x)))
        partition-deep
          (fn partition-deep [spec methods' arglist-size i|arg depth]
            (let [_ (when (> depth 3) (TODO))
                  methods'|by-class
                    (->> methods'
                         ;; TODO optimize further via `group-by-into`
                         (c/group-by (fn-> :argtypes (c/get i|arg)))
                         ;; classes will be sorted from most to least specific
                         (sort-by (fn-> first t/>spec) t/<))]
              (r/for [[c methods''] methods'|by-class
                      spec' spec]
                (update spec' :clauses conj
                  [(>class-spec c)
                   (if (= (inc depth) arglist-size)
                       ;; here, methods'' count will be = 1
                       (-> methods'' first :rtype >class-spec)
                       (partition-deep
                         (xp/condpf-> t/<= (xp/get (inc i|arg)))
                         methods''
                         arglist-size
                         (inc i|arg)
                         (inc depth)))]))))]
    (r/for [[ct methods'] methods|by-ct
            spec (xp/casef count)]
      (if (zero? ct)
          (c/assoc-in spec [:cases 0]  (-> methods' first :rtype >class-spec))
          (c/assoc-in spec [:cases ct] (partition-deep (xp/condpf-> t/<= (xp/get 0)) methods' ct 0 0))))))

#?(:clj
(defns ?cast-call->spec
  "Given a cast call like `clojure.lang.RT/uncheckedBooleanCast`, returns the
   corresponding spec.

   Unchecked fns could be assumed to actually *want* to shift the range over if the
   range hits a certain point, but we do not make that assumption here."
  [c t/class?, method t/symbol? > (? t/spec?)]
  (when (identical? c clojure.lang.RT)
    (case method
      (uncheckedBooleanCast booleanCast)
        t/boolean?
      (uncheckedByteCast    byteCast)
        t/byte?
      (uncheckedCharCast    charCast)
        t/char?
      (uncheckedShortCast   shortCast)
        t/char?
      (uncheckedIntCast     intCast)
        t/int?
      (uncheckedLongCast    longCast)
        t/long?
      (uncheckedFloatCast   floatCast)
        t/float?
      (uncheckedDoubleCast  doubleCast)
        t/double?
      nil))))

(defns- analyze-seq|dot|method-call
  "A note will be made of what methods match the argument types.
   If only one method is found, that is noted too. If no matching method is found, an
   exception is thrown."
  [env ::env, form _, target _, target-class t/class?, static? t/boolean?, method-form simple-symbol?, args-forms _ #_(seq-of form?)]
  ;; TODO cache spec by method
  (if-not-let [methods-for-name (-> target-class class->methods|with-cache (c/get (name method-form)))]
    (if (empty? args-forms)
        (err! "No such method or field in class" {:class target-class :method-or-field method-form})
        (err! "No such method in class"          {:class target-class :methods         method-form}))
    (if-not-let [methods-for-count (c/get methods-for-name (c/count args-forms))]
      (err! "Incorrect number of arguments for method"
            {:class target-class :method method-form :possible-counts (set (keys methods-for-name))})
      (let [static?>kind (fn [static?] (if static? :static :instance))]
        (if-not-let [methods (c/get methods-for-count (static?>kind static?))]
          (err! (istr "Method found for arg-count, but was ~(static?>kind (not static?)), not ~(static?>kind static?)")
                {:class target-class :method method-form :args args-forms})
          (let [args-ct (c/count args-forms)
                call (ast/method-call
                       {:env    env
                        :form   form
                        :target target
                        :method method-form
                        :args   []
                        :spec   (methods->spec methods #_(count arg-forms))})
                with-arg-specs
                  (r/fori [arg-form args-forms
                           call'    call
                           i|arg]
                    (prl! call' arg-form)
                    (let [arg-node (analyze* env arg-form)]
                      ;; TODO can incrementally calculate return value, but possibly not worth it
                      (update call' :args conj arg-node)))
                with-ret-spec
                  (update with-arg-specs :spec
                    (fn [ret-spec]
                      (let [arg-specs (->> with-arg-specs :args (mapv :spec))]
                        (if (seq-or t/infer? arg-specs)
                            (err! "TODO arg spec" (kw-map arg-specs ret-spec (ret-spec arg-specs)))
                            #_(if (t/infer? arg-spec)
                                  (swap! arg-spec t/and (get ret-spec i))
                                  ((get ret-spec i) arg-spec))
                            (ret-spec arg-specs)))))
                ?cast-spec (?cast-call->spec target-class method-form)
                _ (when ?cast-spec
                    (ppr :warn "Not yet able to statically validate whether primitive cast will succeed at runtime" {:form form})
                    #_(s/validate (-> with-ret-spec :args first :spec) #(t/>= % (t/numerically ?cast-spec))))]
            with-ret-spec))))))

(defns- analyze-seq|dot|field-access
  [env ::env, form _, target _, field-form simple-symbol?, field (t/isa? Field)]
  (ast/field-access
    {:env    env
     :form   form
     :target target
     :field  field-form
     :spec   (-> field :type t/>spec)}))

(defns classes>class
  "Ensure that given a set of classes, that set consists of at most a class C and nil.
   If so, returns C. Otherwise, throws."
  [cs (s/set-of t/class?) > t/class?]
  (let [cs' (disj cs nil)]
    (if (-> cs' count (= 1))
        (first cs')
        (err! "Found more than one class" cs))))

;; TODO spec these arguments; e.g. check that ?method||field, if present, is an unqualified symbol
(defns- analyze-seq|dot [env ::env, form _, [target-form _, ?method-or-field _ & ?args _] _]
  {:pre  [(prl! env form target-form ?method-or-field ?args)]
   :post [(prl! %)]}
  (let [target          (analyze* #_?resolve-with-env env target-form)
        method-or-field (if (symbol? ?method-or-field) ?method-or-field (first ?method-or-field))
        args-forms      (if (symbol? ?method-or-field) ?args            (rest  ?method-or-field))]
    (if (t/= (:spec target) t/nil?)
        (err! "Cannot use the dot operator on nil." {:form form})
        (let [;; `nilable?` because technically any non-primitive in Java is nilable and we can't
              ;; necessarily rely on all e.g. "@nonNull" annotations
              {:as ?target-static-class-map target-static-class :class target-static-class-nilable? :nilable?}
                (-> target :spec t/spec>?class-value)
              target-classes
                (if ?target-static-class-map
                    (cond-> #{target-static-class} target-static-class-nilable? (conj nil))
                    (-> target :spec t/spec>classes))
              target-class-nilable? (contains? target-classes nil)
              target-class (classes>class target-classes)]
          ;; TODO determine how to handle `target-class-nilable?`; for now we will just let it slip through
          ;; to `NullPointerException` at runtime rather than create a potentially more helpful custom
          ;; exception
          (if-let [field (and (empty? args-forms)
                              (-> target-class class->fields|with-cache (c/get (name method-or-field))))]
            (analyze-seq|dot|field-access env form target method-or-field field)
            (analyze-seq|dot|method-call env form target target-class (boolean ?target-static-class-map)
              method-or-field args-forms))))))

;; TODO move this
(defns truthy-expr? [{:as expr :keys [spec _]} _ > t/boolean?]
  (ifs (or (t/= spec t/nil?)
           (t/= spec t/false?)) false
       (or (t/> spec t/nil?)
           (t/> spec t/false?)) nil ; representing "unknown"
       true))

(defns- analyze-seq|if
  "If `*conditional-branch-pruning?*` is falsey, the dead branch's original form will be
   retained, but it will not be type-analyzed."
  [env ::env, form _, [pred-form _, true-form _, false-form _ :as body] _]
  {:post [(prl! %)]}
  (if (-> body count (not= 3))
      (err! "`if` accepts exactly 3 arguments: one predicate test and two branches; received" {:body body})
      (let [pred-expr  (analyze* env pred-form)
            true-expr  (delay (analyze* env true-form))
            false-expr (delay (analyze* env false-form))
            whole-expr
              (delay
                (ast/if-expr
                  {:env        env
                   :form       (list 'if (:form pred-expr) (:form @true-expr) (:form @false-expr))
                   :pred-expr  pred-expr
                   :true-expr  @true-expr
                   :false-expr @false-expr
                   :spec       (apply t/or (->> [(:spec @true-expr) (:spec @false-expr)] (remove nil?)))}))]
        (case (truthy-expr? pred-expr)
          true      (do (ppr :warn "Predicate in `if` expression is always true" {:pred pred-form})
                        (-> @true-expr  (assoc :env env)
                                        (cond-> (not *conditional-branch-pruning?*)
                                                (assoc :form (list 'if pred-form (:form @true-expr) false-form)))))
          false     (do (ppr :warn "Predicate in `if` expression is always false" {:pred pred-form})
                        (-> @false-expr (assoc :env env)
                                        (cond-> (not *conditional-branch-pruning?*)
                                                (assoc :form (list 'if pred-form true-form          (:form @false-expr))))))
          nil       @whole-expr))))

(defns- analyze-seq|quote [env ::env, form _, body _]
  {:post [(prl! %)]}
  (ast/quoted env form (tcore/most-primitive-class-of body)))

(defns- analyze-seq|new [env ::env, form _ [c|form _ #_t/class? & args _ :as body] _]
  {:pre [(prl! env form body)]}
  (let [c|analyzed (analyze* env c|form)]
    (if-not (and (-> c|analyzed :spec t/value-spec?)
                 (-> c|analyzed :spec t/value-spec>value class?))
            (err! "Supplied non-class to `new` expression" {:x c|form})
            (let [c             (-> c|analyzed :spec t/value-spec>value)
                  args|analyzed (mapv #(analyze* env %) args)]
              (ast/new-expr {:env   env
                             :form  (list* 'new c|form (map :form args|analyzed))
                             :class c
                             :args  args|analyzed
                             :spec  (t/isa? c)})))))

(defns- analyze-seq|throw [env ::env, form _ [arg _ :as body] _]
  {:pre [(prl! env form body)]}
  (if (-> body count (not= 1))
      (err! "Must supply exactly one input to `throw`; supplied" {:body body})
      (let [arg|analyzed (analyze* env arg)]
        ;; TODO this is not quite true for CLJS but it's nice at least
        (if-not (-> arg|analyzed :spec (t/<= t/throwable?))
          (err! "`throw` requires a throwable; received" {:arg arg :spec (:spec arg|analyzed)})
          (ast/throw-expr {:env  env
                           :form (list 'throw (:form arg|analyzed))
                           :arg  arg|analyzed
                           ;; `t/none?` because nothing is actually returned
                           :spec t/none?})))))

(defns- analyze-seq*
  "Analyze a seq after it has been macro-expanded.
   The ->`form` is post- incremental macroexpansion."
  [env ::env, [caller|form _ & body _ :as form] _]
  (ifs (special-symbols caller|form)
       (case caller|form
         do       (analyze-seq|do    env form body)
         let*     (analyze-seq|let*  env form body)
         deftype* (TODO "deftype*")
         fn*      (TODO "fn*")
         def      (TODO "def")
         .        (analyze-seq|dot   env form body)
         if       (analyze-seq|if    env form body)
         quote    (analyze-seq|quote env form body)
         new      (analyze-seq|new   env form body)
         throw    (analyze-seq|throw env form body))
       ;; TODO support recursion
       (let [caller|expr (analyze* env caller|form)
             caller|spec (:spec caller|expr)
             args-ct     (count body)]
         (case (t/compare caller|spec t/callable?)
           (1 2)  (err! "It is not known whether expression be called" {:expr caller|expr})
           3      (err! "Expression cannot be called" {:expr caller|expr})
           (-1 0) (let [assert-valid-args-ct
                          (ifs (or (t/<= caller|spec t/keyword?) (t/<= caller|spec t/+map|built-in?))
                               (when-not (or (= args-ct 1) (= args-ct 2))
                                 (err! (str "Keywords and `clojure.core` persistent maps must be provided "
                                            "with exactly one or two args when calling them")
                                       {:args-ct args-ct :caller caller|expr}))

                               (or (t/<= caller|spec t/+vector|built-in?) (t/<= caller|spec t/+set|built-in?))
                               (when-not (= args-ct 1)
                                 (err! (str "`clojure.core` persistent vectors and `clojure.core` persistent "
                                            "sets must be provided with exactly one arg when calling them")
                                       {:args-ct args-ct :caller caller|expr}))

                               (t/<= caller|spec t/fnt?)
                               (TODO "Don't know how to handle spec'ed fns yet" {:caller caller|expr})
                               ;; For non-speced fns, unknown; we will have to risk runtime exception
                               ;; because we can't necessarily rely on metadata to tell us the whole truth
                               (t/<= caller|spec t/fn?)
                               nil
                               ;; If it's ifn but not fn, we might have missed something in this dispatch so for now we throw
                               (err! "Don't know how how to handle non-fn ifn" {:caller caller|expr}))
                        {:keys [args spec]}
                          (->> body
                               (c/map+ #(analyze* env %))
                               (reduce (fn [{:keys [args spec]} arg|analyzed]
                                         (conj args))))]

                    ;; TODO incrementally check by analyzing each arg in `reduce` and pruning branches of what the
                    ;; spec could be, and throwing if it's found something that's an impossible combination
                    (ast/call-expr
                      {:env    env
                       :form   form
                       :caller caller|expr
                       :args   args
                       :spec   spec}))))))

(defns- analyze-seq [env ::env, form _]
  {:post [(prl! %)]}
  (prl! form)
  (let [expanded-form (macroexpand form)]
    (if (== form expanded-form)
        (analyze-seq* env expanded-form)
        (ast/macro-call {:env env :form form :expanded (analyze-seq* env expanded-form)}))))

(defns- analyze-symbol [env _, form t/symbol?]
  {:post [(prl! %)]}
  (let [resolved (?resolve-with-env form env)]
    (if-not resolved
      (err! "Could not resolve symbol" {:sym form})
      (ast/symbol env form
        (ifs (ast/node? resolved)
               (:spec resolved)
             (or (t/literal? resolved) (t/class? resolved))
               (t/value resolved)
             (var? resolved)
               (or (-> resolved meta :spec)
                   (t/value @resolved))
             (utpred/unbound? resolved)
               ;; Because the var could be anything and cannot have metadata (spec or otherwise)
               t/any?
             (TODO "Unsure of what to do in this case" (kw-map env form resolved)))))))

(defns- analyze* [env ::env, form _]
  (prl! env form)
  (when (> (swap! *analyze-i inc) 100) (throw (ex-info "Stack too deep" {:form form})))
  (ifs (symbol? form)
         (analyze-symbol env form)
       (t/literal? form)
         (ast/literal env form (t/>spec form))
       (or (vector? form)
           (set?    form))
         (analyze-non-map-seqable env form (empty form) (fn stop [& [a b :as args]] (prl! args) (err! "STOP")))
       (map? form)
         (analyze-map env form)
       (seq? form)
         (analyze-seq env form)
       (throw (ex-info "Unrecognized form" {:form form}))))

(defns analyze
  ([body _] (analyze {} body))
  ([env ::env, body _]
    (reset! *analyze-i 0)
    (analyze* env body)))

;; ===== (DE)FNT ===== ;;

#_(s/def :fnt|overload/arglist-code (t/vec-of arg?))

 #_"Must evaluate to an `s/fspec`"
(s/def :fnt|overload/spec               :quantum.core.specs/code)

#_(s/def :fnt|overload/body-codelist (t/seq-of :quantum.core.specs/code))

;; Internal
(s/def ::fnt|overload
  (s/kv {:arg-classes                 (s/vec-of t/class?)
         :arg-specs                   t/any?
         :arglist-code|fn|hinted      t/any?
         :arglist-code|reify|unhinted t/any?
         :body-form                   t/any?
         :positional-args-ct          (s/and t/integer? #(>= % 0))
         :out-spec                    t/spec?
         :out-class                   (? t/class?)
         ;; When present, varargs are considered to be of class Object
         :variadic?                   t/boolean?}))

(s/def ::reify|overload
  (s/keys :req-un [:quantum.core.specs/interface
                   :reify|overload/out-class
                   :reify/method-sym
                   :reify/arglist-code
                   :reify|overload/body-form]))

(s/def :protocol/overload
  (s/keys :req-un [:protocol|overload/name    #_simple-symbol?
                   :protocol|overload/arglist #_(t/vector-of simple-symbol?)]))

#_(:clj
(defn fnt|arg->class [lang {:as arg [k spec] ::fnt|arg-spec :keys [arg-binding]}]
  (cond (not= k :spec) java.lang.Object; default class
        (symbol? spec) (pred->class lang spec))))

(defn >with-post-spec
  [body post-spec]
  `(let [~'out ~body]
     (s/validate ~'out ~(update-meta post-spec dissoc* :runtime?))))

#?(:clj
(var/def sort-guide "for use in arity sorting, in increasing conceptual size"
  {Object       0
   tdef/boolean 1
   tdef/byte    2
   tdef/short   3
   tdef/char    4
   tdef/int     5
   tdef/long    6
   tdef/float   7
   tdef/double  8}))

#?(:clj
(defns arg-specs>arg-classes-seq|primitivized
  "'primitivized' meaning given an arglist whose specs are `[t/any?]` this will output:
   [[java.lang.Object]
    [boolean]
    [byte]
    [short]
    [char]
    [int]
    [long]
    [float]
    [double]]
   which includes all primitive subclasses of the spec."
  [arg-specs (s/seq-of t/spec?) > (s/seq-of (s/vec-of t/class?))]
  (->> arg-specs
       (c/lmap (fn [spec #_t/spec?]
                 (if (-> spec meta :ref?)
                     (-> spec t/spec>classes (disj nil) seq)
                     (let [cs (spec>most-primitive-classes spec)]
                       (let [base-classes (->> cs (c/map+ class>simplest-class) >set)
                             base-classes (cond-> base-classes (contains? cs nil) (conj java.lang.Object))]
                         (->> cs (c/map+ tcore/class>prim-subclasses)
                                 (educe (aritoid nil identity set/union) base-classes)
                                 ;; for purposes of cleanliness and reproducibility in tests
                                 (sort-by sort-guide)))))))
       (apply combo/cartesian-product)
       (c/lmap >vec))))

(s/def ::lang #{:clj :cljs})

#?(:clj
(defns- >fnt|overload
  [{:keys [arg-bindings _, arg-classes|pre-analyze _, arg-specs|pre-analyze|base _, args _
           body-codelist|pre-analyze _, lang ::lang, post-form _, varargs _, varargs-binding _]} _
   > ::fnt|overload]
  (let [arg-specs|pre-analyze
          (c/mergev-with
            (fn [_ spec #_t/spec? c #_t/class?]
              (cond-> spec (t/primitive-class? c) (t/and c)))
            arg-specs|pre-analyze|base arg-classes|pre-analyze)
        env         (->> (zipmap arg-bindings arg-specs|pre-analyze)
                         (c/map' (fn [[arg-binding arg-spec]]
                                   [arg-binding (ast/unbound nil arg-binding arg-spec)])))
        analyzed    (analyze env (ufgen/?wrap-do body-codelist|pre-analyze))
        arg-specs   (->> arg-bindings (mapv #(:spec (c/get (:env analyzed) %))))
        arg-classes (->> arg-specs (c/map spec>most-primitive-class))
        arg-classes|simplest (->> arg-classes (c/map class>simplest-class))
        hint-arg|fn (fn [i arg-binding]
                      (ufth/with-type-hint arg-binding
                        (ufth/>fn-arglist-tag
                          (c/get arg-classes|simplest i)
                          lang
                          (c/count args)
                          varargs)))
        post-spec   (when post-form (-> post-form eval t/>spec))
        post-spec|runtime? (-> post-spec meta :runtime?)
        out-spec (if post-spec
                     (if post-spec|runtime?
                         (case (t/compare post-spec (:spec analyzed))
                           -1     post-spec
                            1     (:spec analyzed)
                            0     post-spec
                            (2 3) (err! "Body and output spec comparison not handled" {:body analyzed :output-spec post-spec}))
                         (if (t/<= (:spec analyzed) post-spec)
                             (:spec analyzed)
                             (err! "Body does not match output spec" {:body analyzed :output-spec post-spec})))
                     (:spec analyzed))
        body-form
          (-> (:form analyzed)
              (cond-> post-spec|runtime? (>with-post-spec post-spec))
              (ufth/cast-bindings|code
                (->> (c/zipmap-into (map/om) arg-bindings arg-classes)
                     (c/remove-vals' (fn-or nil? (fn= java.lang.Object) t/primitive-class?)))))]
      {:arg-classes                 arg-classes|simplest
       :arg-specs                   arg-specs
       :arglist-code|fn|hinted      (cond-> (->> arg-bindings (c/map-indexed hint-arg|fn))
                                            varargs-binding (conj '& varargs-binding)) ; TODO use ``
       :arglist-code|reify|unhinted (cond-> arg-bindings varargs-binding (conj varargs-binding))
       :body-form                   body-form
       :positional-args-ct          (count args)
       :out-spec                    out-spec
       :out-class                   (out-spec>class out-spec)
       :variadic?                   (boolean varargs)})))

#?(:clj ; really, reserve for metalanguage
(defn fnt|overload-data>overload-group
  "Rather than rigging together something in which either:
   1) the Clojure compiler will try to cross its fingers and evaluate code meant to be evaluated in ClojureScript
   2) we use a CLJS-in-CLJS compiler and alienate the mainstream CLJS-in-CLJ (cljsbuild) workflow, which includes
      our own workflow
   3) we wait for CLJS-in-CLJS to become mainstream, which could take years if it really ever happens

   we decide instead to evaluate specs in languages in which the metalanguage (compiler language) is the same as
   the object language (e.g. Clojure), and symbolically analyze specs in the rest (e.g. vanilla ClojureScript),
   deferring code analyzed as functions to be enforced at runtime."
  [{:as in {:keys [args varargs] pre-form :pre post-form :post} :arglist body-codelist|pre-analyze :body}
   {:as opts :keys [lang #_::lang symbolic-analysis? #_t/boolean?]}]
  (if symbolic-analysis?
      (err! "Symbolic analysis not supported yet")
      (let [_ (when pre-form (TODO "Need to handle pre"))
            varargs-binding (when varargs
                              ;; TODO this assertion is purely temporary until destructuring is supported
                              (assert (-> varargs :binding-form first (= :sym))))
            arg-bindings
              (->> args
                   (mapv (fn [{[kind binding-] :binding-form}]
                           ;; TODO this assertion is purely temporary until destructuring is supported
                           (assert kind :sym)
                           binding-)))
            arg-specs|pre-analyze|base
              (->> args
                   (mapv (fn [{[kind #_#{:any :infer :spec}, spec #_t/form?] :spec}]
                           (case kind :any   t/any?
                                      :infer t/?
                                      :spec  (-> spec eval t/>spec)))))
            arg-classes-seq|pre-analyze (arg-specs>arg-classes-seq|primitivized arg-specs|pre-analyze|base)
            ;; `unprimitivized` is first because of class sorting
            [unprimitivized & primitivized]
              (->> arg-classes-seq|pre-analyze
                   (mapv (fn [arg-classes|pre-analyze]
                           (>fnt|overload
                             (kw-map arg-bindings arg-classes|pre-analyze arg-specs|pre-analyze|base args
                                     body-codelist|pre-analyze lang post-form varargs varargs-binding)))))]
        {:unprimitivized unprimitivized
         :primitivized   primitivized}))))

(def fnt-method-sym 'invoke)

(defns- class>interface-part-name [c t/class? > t/string?]
  (if (= c java.lang.Object)
      "Object"
      (let [illegal-pattern #"\|\+"]
        (if (->> c >name (re-find illegal-pattern))
            (err! "Class cannot contain pattern" {:class c :pattern illegal-pattern})
            (-> c >name (str/replace "." "|"))))))

(defns fnt-overload>interface-sym [args-classes (s/seq-of t/class?), out-class t/class? > t/symbol?]
  (>symbol (str (->> args-classes (lmap class>interface-part-name) (str/join "+"))
                ">" (class>interface-part-name out-class))))

(defns fnt-overload>interface [args-classes _, out-class t/class?]
  (let [interface-sym     (fnt-overload>interface-sym args-classes out-class)
        hinted-method-sym (ufth/with-type-hint fnt-method-sym (ufth/>interface-method-tag out-class))
        hinted-args       (ufth/hint-arglist-with
                            (ufgen/gen-args (count args-classes))
                            (map ufth/>interface-method-tag args-classes))]
    `(~'definterface ~interface-sym (~hinted-method-sym ~hinted-args))))

#?(:clj
(defns fnt|overload>reify-overload
  [{:as overload
    :keys [arg-classes _, arglist-code|reify|unhinted _, body-form _, out-class t/class?]} :fnt/overload
   > (s/seq-of ::reify|overload)]
  (prl! overload)
  (let [interface-k {:out out-class :in arg-classes}
        interface
          (-> *interfaces
              (swap! update interface-k #(or % (eval (fnt-overload>interface arg-classes out-class))))
              (c/get interface-k))
        arglist-code
          (>vec (concat ['_]
                  (doto (->> arglist-code|reify|unhinted
                       (map-indexed
                         (fn [i arg] (ufth/with-type-hint arg (-> arg-classes (doto pr/ppr-meta) (c/get i) (doto pr/ppr-meta) ufth/>arglist-embeddable-tag)))))
                  pr/ppr-meta)))]
    {:arglist-code  arglist-code
     :body-form     body-form
     :interface     interface
     :method-sym    fnt-method-sym
     :out-class     out-class})))

#?(:clj
(defns fnt|overload-group>reify
  [{:keys [overload-group :fnt/overload-group, i t/integer?, fn|name :quantum.core.specs/fn|name]} _]
  (let [reify-overloads (->> (concat [(:unprimitivized overload-group)]
                                      (:primitivized   overload-group))
                             (c/map fnt|overload>reify-overload))]
    `(~'def ~(>symbol (str fn|name "|__" i))
       (reify ~@(->> reify-overloads
                     (c/lmap (fn [{:keys [interface out-class method-sym arglist-code body-form]} #_::reify|overload]
                               [(-> interface >name >symbol)
                                `(~(ufth/with-type-hint method-sym (ufth/>arglist-embeddable-tag out-class))
                                  ~arglist-code ~body-form)]))
                     lcat))))))

(defns >extend-protocol|code [{:keys [protocol|name t/symbol?]} _]
  `(extend-protocol ~protocol|name))

(defns >defprotocol|code
  ;; TODO ensure that overload names do not shadow each other
  [{:keys [name      :protocol/name
           overloads (s/seq-of :protocol/overload)]} _]
  `(defprotocol ~name
     ~@(->> overloads
            (sort-by (fn-> :arglist count))
            (sort-by :name)
            (c/lmap (fn [{:keys [name arglist]}]
                      `(~name ~arglist))))))

(defn fnt|overload-groups>protocol [_])

#_(is (code= (defnt|code>protocols 'abc (do defnt|code|class|=|2|1) :clj)
        [{:defprotocol
            ($ (defprotocol ~'abc|__Protocol__java|io|FilterOutputStream
                 (~'abc|__protofn__java|io|FilterOutputStream [~'x0 ~'x1])))
          :extend-protocols
            [($ (extend-protocol ~'abc|__Protocol__java|io|FilterOutputStream
                  java.io.FilterOutputStream
                    (~'abc|__protofn__java|io|FilterOutputStream
                      [~(tag "java.io.FilterOutputStream" 'x1) ~(tag "java.io.FilterOutputStream" 'x0)]
                        (.invoke ~'abc|__0 ~'x0 ~'x1))))]
          :defn nil}
         {:defprotocol
            ($ (defprotocol ~'abc|__Protocol__long
                 (~'abc|__protofn__long [~'x0 ~'x1])))
          :extend-protocols
            [($ (extend-protocol ~'abc|__Protocol__long
                  java.io.FilterOutputStream
                    (~'abc|__protofn__long
                      [~(tag "java.io.FilterOutputStream" 'x1) ~(tag "long" 'x0)]
                        (.invoke ~'abc|__0 ~'x0 ~'x1))))]
          :defn nil}
         {:defprotocol
            ($ (defprotocol ~'abc|__Protocol
                 (~'abc [~'x0 ~'x1])))
          :extend-protocols
            [($ (extend-protocol ~'abc|__Protocol
                  java.io.FilterOutputStream
                    (~'abc
                      [~(tag "java.io.FilterOutputStream" 'x0) ~'x1]
                        (~'abc|__protofn__java|io|FilterOutputStream ~'x1 ~'x0))
                  java.lang.Long
                    (~'abc
                      [~(tag "long"                       'x0) ~'x1]
                        (~'abc|__protofn__long ~'x1 ~'x0))))]
          :defn nil}]))

(def allowed-shorthand-tag-chars "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ")

(defn >all-shorthand-tags []
  (->> (apply concat
         (for [n (c/unchunk (range 1 (inc 64)))] ; just up to length 64 for now
           (apply combo/cartesian-product (repeat n allowed-shorthand-tag-chars))))
       (c/lmap #(apply str %))
       c/unchunk))

(defonce *class>shorthand-tag|cache
  (atom {:remaining (>all-shorthand-tags)}))

;; dynamic for testing purposes
(def ^:dynamic **class>shorthand-tag|cache* *class>shorthand-tag|cache)

(defns class>shorthand-tag [c t/class?]
  (or (c/get @**class>shorthand-tag|cache* c)
      (-> (swap! **class>shorthand-tag|cache*
            (fn [{:as m :keys [remaining]}]
              (assoc m c          (first remaining)
                       :remaining (next  remaining))))
          (get c))))

(defn assert-monotonically-increasing-specs!
  "Asserts that each spec in an overload of the same arity and arg-position
   are in monotonically increasing order in terms of `t/compare`."
  [overloads|grouped-by-arity]
  (doseq [[arity-ct overloads] overloads|grouped-by-arity]
    (educe
      (fn [prev-overload [i|overload overload]]
        (when prev-overload
          (reduce-2
            (fn [_ arg|spec|prev [i|arg arg|spec]]
              (when (= (t/compare arg|spec arg|spec|prev) -1)
                ;; TODO provide code context, line number, etc.
                (err! (istr "At overload ~{i|overload}, arg ~{i|arg}: spec is not in monotonically increasing order in terms of `t/compare`")
                      {:overload      overload
                       :prev-overload prev-overload
                       :prev-spec     arg|spec|prev
                       :spec          arg|spec})))
            (:arg-specs prev-overload)
            (c/lindexed (:arg-specs overload))))
        overload)
      nil
      overloads)))

(defns fnt|overloads>protocols
  [{:keys [overloads (s/and t/indexed? (s/seq-of :fnt/overload))
           fn|name   :quantum.core.specs/fn|name]} _
   > (s/kv {:defprotocol      t/any?
            :extend-protocols t/any?
            :defn             t/any?})]
  (when (->> overloads (seq-or (fn-> :positional-args-ct (> 2))))
    (TODO "Doesn't yet handle protocol creation for arglist counts of > 2"))
  (when (->> overloads (seq-or :variadic?))
    (TODO "Doesn't yet handle protocol creation for variadic overloads"))
  (let [overloads|grouped-by-arity (->> overloads c/indexed+ (c/group-by (fn-> second :positional-args-ct)))]
    (assert-monotonically-increasing-specs! overloads|grouped-by-arity))
  (let [all-arg-classes  (->> overloads (mapv :arg-classes))
        protocol|name    (str fn|name "__Protocol__" )
        extend-protocols nil #_(for []
                           (>extend-protocol|code (kw-map protocol|name)))]
    {:defprotocol      (>defprotocol|code {:name      protocol|name
                                           :overloads []})
     :extend-protocols extend-protocols
     :defn             nil #_defn-definition}))

;; This protocol is so suffixed because of the position of the argument on which
                       ;; it dispatches
#_(do (defprotocol name|gen__Protocol__0
  (name|gen [~'x]))
(extend-protocol name|gen__Protocol__0
  java.lang.String   (name|gen [x] (.invoke name|gen|__0 x))
  ;; this is part of the protocol because even though `Named` is an interface,
  ;; `String` is final, so they're mutually exclusive
  clojure.lang.Named (name|gen [x] (.invoke name|gen|__1 x))))

(defns gen-register-spec
  "Registers in the map of qualified symbol to input spec, to output spec

   Example output:
   (swap! ... assoc `abcde
     (fn [args] (case (count args) 1 <out-spec>)))"
  [{:keys [fn|name :quantum.core.specs/fn|name, arg-ct->spec _, variadic-overload _]} _]
  (unify-gensyms
   `(swap! *fn->spec assoc '~(qualify fn|name)
      (xp/>expr
        (fn [args##] (case (count args##) ~@arg-ct->spec
                      ~@(when variadic-overload
                          [`(if (>= (count args##) (:positional-args-ct variadic-overload))
                                (:out-spec variadic-overload)
                                (err! "Arg count not enough for variadic overload"))])))))
    true))

(defns fnt|code [kind #{:fn :defn}, lang ::lang, args _]
  (prl! kind lang args)
  (let [{:keys [:quantum.core.specs/fn|name overloads :quantum.core.specs/meta] :as args'}
          (s/validate args (case kind :defn ::defnt :fn ::fnt))
        _ (prl! args')
        inline?
          (s/validate (-> fn|name core/meta :inline) (t/? t/boolean?))
        _ (prl! inline?)
        fn|name (if inline?
                    (do (log/pr :warn "requested `:inline`; ignoring until feature is implemented")
                        (update-meta fn|name dissoc :inline))
                    fn|name)
        fnt|overload-groups (->> overloads (mapv #(fnt|overload-data>overload-group % {:lang lang})))
        ;; only one variadic arg allowed
        _ (s/validate fnt|overload-groups
                      (fn->> (c/lmap :unprimitivized) (c/lfilter :variadic?) count (<- (<= 1))))
        arg-ct->spec (->> fnt|overload-groups
                          (c/map+     :unprimitivized)
                          (remove+    :variadic?)
                          (c/group-by :positional-args-ct)
                          (map-vals+  :out-spec)
                          join (apply concat))
        variadic-overload (->> fnt|overload-groups (c/lmap :unprimitivized) (c/lfilter :variadic?) first)
        register-spec (gen-register-spec (kw-map fn|name arg-ct->spec variadic-overload))
        direct-dispatch-codelist
          (case lang
            :clj  (for [[i fnt|overload-group] (c/lindexed fnt|overload-groups)]
                    (fnt|overload-group>reify (assoc (kw-map i fn|name) :overload-group fnt|overload-group)))
            :cljs (TODO))
        dynamic-dispatch-codelist
          (case lang
            :clj  (let [protocol (fnt|overload-groups>protocol {:overload-groups fnt|overload-groups :fn|name fn|name})]
                    `[~(:defprotocol protocol)
                      ~@(:extend-protocols protocol)])
            :cljs (TODO))
        base-fn-codelist []
        fn-codelist
          (case lang
            :clj  (->> `[~@direct-dispatch-codelist
                         ~@dynamic-dispatch-codelist
                         ~@base-fn-codelist]
                        (remove nil?))
            :cljs (TODO))
        overloads|code (->> fnt|overload-groups (c/map+ :unprimitivized) (c/map :code))
        _ (prl! overloads)
        code (case kind
               :fn   (list* 'fn (concat
                                  (if (contains? args' :quantum.core.specs/fn|name)
                                      [fn|name]
                                      [])
                                  [overloads|code]))
               :defn `(~'do #_~register-spec ; elide for now
                            ~@fn-codelist))]
    code))

#?(:clj (defmacro fnt   [& args] (fnt|code :fn   (ufeval/env-lang) args)))
#?(:clj (defmacro defnt [& args] (fnt|code :defn (ufeval/env-lang) args)))
