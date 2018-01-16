(ns quantum.core.defnt
  (:refer-clojure :exclude
    [+ #_zero? odd? even?
     bit-and
     every? vec
     ==
     if-let when-let
     assoc-in
     macroexpand
     get map filter])
  (:require
    [clojure.core                      :as c]
    [clojure.string                    :as str]
    [quantum.core.error                :as err
      :refer [TODO err!]]
    [quantum.core.fn
      :refer [aritoid fn1 fnl fn', fn-> fn->> <-, rcomp
              firsta seconda]]
    [quantum.core.log                  :as log
      :refer [ppr! prl! prlm!]]
    [quantum.core.logic
      :refer [fn-and fn-or fn-not if-let if-not-let when-let]]
    [quantum.core.macros
      :refer [macroexpand]]
    [quantum.core.macros.core          :as cmacros
      :refer [unify-gensyms]]
    [quantum.core.macros.type-hint     :as th]
    [quantum.core.print                :as pr]
    [quantum.core.spec                 :as s]
    [quantum.core.specs                :as ss]
    [quantum.core.type.core            :as tcore]
    [quantum.core.type.defs            :as tdef]
    [quantum.core.untyped.analyze.ast  :as ast]
    [quantum.core.untyped.analyze.expr :as xp]
    [quantum.core.untyped.analyze.rewrite :as ana-rw]
    [quantum.core.untyped.collections  :as ucoll
      :refer [assoc-in dissoc-if get lmap lfilter lindexed lflatten-1]]
    [quantum.core.untyped.collections.logic :as ucoll&
      :refer [every?]]
    [quantum.core.untyped.collections.tree :as tree
      :refer [prewalk postwalk walk]]
    [quantum.core.untyped.compare  :as comp
      :refer [==]]
    [quantum.core.untyped.convert  :as conv
      :refer [>symbol >name]]
    [quantum.core.untyped.core
      :refer [->sentinel kw-map istr]]
    [quantum.core.untyped.data.set :as set]
    [quantum.core.untyped.loops    :as loops]
    [quantum.core.untyped.numeric.combinatorics :as combo]
    [quantum.core.untyped.qualify  :as qual :refer [qualify]]
    [quantum.core.untyped.reducers :as r
      :refer [vec map+ map-vals+ mapcat+ filter+ remove+ partition-all+
              join reducei educe]]
    [quantum.core.untyped.refs     :as ref
      :refer [?deref]]
    [quantum.core.untyped.type     :as t
      :refer [?]]
    [quantum.core.untyped.vars     :as var
      :refer [update-meta]]
    [quantum.format.clojure.core ; TODO temporary
      :refer [reformat-string]])
  (:import
    [quantum.core Numeric]
    [quantum.core.untyped.type ClassSpec]))

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

(do

;; FNT

(s/def ::spec s/any?)

(s/def ::fnt|arg-spec ; TODO expand; make typed destructuring available via ::ss/binding-form
  (s/alt :infer #{'?}
         :any   #{'_}
         :spec  ::spec))

(s/def ::fnt|speced-arg
  (s/cat :arg-binding   (s/and simple-symbol?
                               (set/not #{'& '| '>})
                               (fn-> meta :tag nil?))
         ::fnt|arg-spec ::fnt|arg-spec))

(s/def ::fnt|output-spec (s/? (s/cat :sym (fn1 = '>) ::spec ::spec)))

(s/def ::fnt|arglist
  (s/and vector?
         (s/spec
           (s/cat :args    (s/* ::fnt|speced-arg)
                  :varargs (s/? (s/cat :sym (fn1 = '&) ::fnt|speced-arg ::fnt|speced-arg))
                  :pre     (s/? (s/cat :sym (fn1 = '|) ::spec           ::spec))
                  :post    ::fnt|output-spec))
         (s/conformer
           #(cond-> % (contains? % :varargs) (update :varargs ::fnt|speced-arg)
                      (contains? % :pre    ) (update :pre     ::spec)
                      (contains? % :post   ) (update :post    ::spec)))))

(s/def ::fnt|body (s/alt :body (s/* s/any?)))

(s/def ::fnt|arglist+body
  (s/cat ::fnt|arglist ::fnt|arglist :body ::fnt|body))

(s/def ::fnt|overloads
  (s/alt :overload-1 ::fnt|arglist+body
         :overload-n (s/cat :overloads (s/+ (s/spec ::fnt|arglist+body)))))

(s/def ::fnt|postchecks
  (s/conformer
    (fn [f]
      (-> f (update :overloads
              (fnl mapv (fn [overload] (let [overload' (update overload :body :body)]
                                         (if-let [output-spec (-> f :output-spec ::spec)]
                                           (do (s/validate (-> overload' ::fnt|arglist :post) nil?)
                                               (assoc-in overload' [::fnt|arglist :post] output-spec))
                                           overload')))))
            (dissoc :output-spec)))))

(s/def ::fnt
  (s/and (s/spec
           (s/cat
             ::ss/fn|name   (s/? ::ss/fn|name)
             ::ss/docstring (s/? ::ss/docstring)
             ::ss/meta      (s/? ::ss/meta)
             :output-spec   ::fnt|output-spec
             :overloads     ::fnt|overloads))
         ::ss/fn|postchecks
         ::fnt|postchecks))

(s/def ::fns|code ::fnt)

(s/def ::defnt
  (s/and (s/spec
           (s/cat
             ::ss/fn|name   ::ss/fn|name
             ::ss/docstring (s/? ::ss/docstring)
             ::ss/meta      (s/? ::ss/meta)
             :output-spec   ::fnt|output-spec
             :overloads     ::fnt|overloads))
         ::ss/fn|postchecks
         ::fnt|postchecks))

(s/def ::defns|code ::defnt)

(defonce *pred->class (atom {}))

#?(:clj
(defn pred->class [lang pred]
  (assert (symbol? pred))
  (get-in @*pred->class [(cmacros/syntax-quoted|sym pred) lang])))

#?(:clj
(defn def-pred->class [pred|sym c-clj c-cljs]
  (assert (symbol? pred|sym))
  (assert (or c-clj c-cljs))
  (assert (or (symbol? c-clj)  (nil? c-clj)))
  (assert (or (symbol? c-cljs) (nil? c-cljs)))
  (swap! *pred->class assoc
    (cmacros/syntax-quoted|sym pred|sym)
    {:clj  (cmacros/syntax-quoted|sym c-clj)
     :cljs c-cljs})))

#?(:clj (def-pred->class 'class?  'Class                       nil))
#?(:clj (def-pred->class 'string? 'String                      'js/String))
#?(:clj (def-pred->class 'map?    'clojure.lang.IPersistentMap nil))
#?(:clj (def-pred->class 'set?    'clojure.lang.IPersistentSet nil))

(defn fns|code [kind lang args]
  (let [{:keys [::ss/fn|name overloads ::ss/meta] :as args'}
          (s/validate args (case kind :defn ::defns|code :fn ::fns|code))
        overload-data->overload
          (fn [{{:keys [args varargs pre post]} ::fnt|arglist
                body :body}]
            (let [arg-spec->validation
                    (fn [{[k spec] ::fnt|arg-spec :keys [arg-binding]}]
                      ;; TODO this validation is purely temporary until destructuring is supported
                      (s/validate arg-binding simple-symbol?)
                      (case k
                        :any   nil
                        :infer (do (log/pr :warn "Spec inference not yet supported in `defns`. Ignoring request to infer" arg-binding)
                                   nil)
                        :spec  (if-let [c|sym (or (some-> spec th/?tag->class th/class->instance?-safe-tag|sym)
                                                  (and (symbol? spec) (pred->class lang spec)))]
                                 (list `instance? c|sym arg-binding)
                                 (list `s/validate arg-binding spec))))
                  spec-validations
                    (concat (lmap arg-spec->validation args)
                            (some-> varargs arg-spec->validation))
                  ;; TODO if an arg has been primitive-type-hinted in the `fn` arglist, then no need to do an `instance?` check
                  ?hint-arg
                    (fn [{[k spec] ::fnt|arg-spec :keys [arg-binding]}]
                      (if (not= k :spec)
                          arg-binding
                          #_th/->fn-arglist-tag
                          arg-binding ; TODO remove reflection by re-enabling the following:
                          #_(th/with-type-hint arg-binding
                            (if-let [c (th/?tag->class spec)]
                              ( c lang (count args) varargs)
                              (and (symbol? spec) (pred->class lang spec))))))
                  arglist'
                    (->> args
                         (map+ ?hint-arg)
                         join
                         (<- cond-> varargs (conj '& (?hint-arg varargs))))
                  validations
                    (->> [(when post {:post [(list post (symbol "%"))]})
                          (some->> spec-validations (lfilter some?) seq (list* 'do))
                          (when pre (list 'assert pre))]
                         (lfilter some?))]
              (list* arglist' (concat validations body))))
        overloads (mapv overload-data->overload overloads)
        code (case kind
               :fn   (list* 'fn (concat
                                  (if (contains? args' ::ss/fn|name)
                                      [fn|name]
                                      [])
                                  [overloads]))
               :defn (list* 'defn fn|name overloads))]
    code))

(defmacro fns
  "Like `fnt`, but relies on runtime spec checks.
   Does not perform type inference (at least not yet).
   Also does not currently handle spec checks in
   destructuring contexts yet."
  [& args]
  (fns|code :fn (cmacros/env-lang) args))

(defmacro defns
  "Like `defnt`, but relies on runtime spec checks.
   Does not perform type inference (at least not yet).
   Also does not currently handle spec checks in
   destructuring contexts yet."
  [& args]
  (fns|code :defn (cmacros/env-lang) args))

(defns abcde ""
  ([a ? b _ > integer?] {:pre 1} 1 2)
  ([c string?, d StringBuilder & e _ > number?]
    (.substring c 0 1)
    3 4)
  ([f long]
    (c/+ f 1)))

; ----- TYPED PART ----- ;

;; NOTE: All this code can be defnt-ized after; this is just for bootstrapping purposes so performance isn't extremely important in most of these functions.

(defonce *fn->spec (atom {}))

(defonce defnt-cache (atom {})) ; TODO For now — but maybe lock-free concurrent hash map to come

(defonce *interfaces (atom {}))

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

(defonce class->methods|with-cache
  (memoize (fn [c] (class->methods c))))

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

(defonce class->fields|with-cache
  (memoize (fn [c] (class->fields c))))

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

(defn truthy-type? [t]
  (when (#{Boolean Boolean/TYPE} t)
    (TODO "Don't yet known how to handle booleans"))
  (if (= t :nil)
      false
      true))

(defn truthy-expr? [expr]
  (when (-> expr :constraints some?)
    (TODO "Don't yet know how to handle constraints"))
  (if-let [classes (->> expr :type-info ?deref :reifieds (lmap truthy-type?) seq)]
    (ucoll&/every-val ::unknown classes)
    ::unknown))

(defn union|type-info [ti0 ti1]
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
         {:env env :form (transient empty-form)})
       (persistent!-and-add-file-context form)))

(defn analyze-map
  {:todo #{"If the map is bound to a variable, preserve type info for it such that lookups
            can start out with a guarantee of a certain type."}}
  [env form]
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

(defn analyze-seq|do [env form body]
  (prl! env body)
  (if (empty? body)
      (ast/do {:env  env
               :form form
               :body (r/vec body)
               :spec t/nil?})
      (let [expr (analyze-non-map-seqable env body []
                   (fn [accum expr _]
                     ;; for types, only the last subexpression ever matters, as each is independent from the others
                     (assoc expr :form (conj! (:form accum) (:form expr))
                                 ;; but the env should be the same as whatever it was originally because no new scopes are created
                                 :env  (:env accum))))]
        (ast/do {:env  env
                 :form form
                 :body (r/vec body)
                 :spec (:spec expr)}))))

(defn analyze-seq|let*|bindings [env bindings]
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

(defn analyze-seq|let* [env [bindings & body]]
  (TODO "`let*` analysis")
  #_(let [{env' :env bindings' :form}
          (analyze-seq|let*|bindings env bindings)
        {env'' :env body' :form type-info' :type-info}
          (analyze-seq|do env' body)]
    (->expr-info {:env       env
                  :form      (list 'let* bindings' body')
                  :type-info type-info'})))

(defns ?resolve-with-env
  [sym symbol?, env _]
  (let [local (get env sym)]
    (if (some? local)
        (if (ast/unbound? local)
            local
            (TODO "Need to figure out what to do when resolving local vars"))
        (let [resolved (ns-resolve *ns* sym)]
          (log/ppr :warn "Not sure how to handle non-local symbol; resolved it for now" (kw-map sym resolved))
          resolved))))

(defn methods->spec
  "Creates a spec given ->`methods`."
  [methods #_(t/seq method?)]
  ;; TODO room for plenty of optimization here
  (let [methods|by-ct (->> methods
                           (group-by (fn-> :argtypes count))
                           (sort-by first <))
        ;; non-primitive classes in Java aren't guaranteed to be non-null
        >class-spec (fn [x]
                      (cond (class? x)
                              (-> x t/>spec (cond-> (not (t/primitive-class? x)) t/?))
                            (t/spec? x)
                              x
                            :else (err/not-supported! `>class-spec x)))
        partition-deep
          (fn partition-deep [spec methods' arglist-size i|arg depth]
            (let [_ (when (> depth 3) (TODO))
                  methods'|by-class
                    (->> methods'
                         ;; TODO optimize further via `group-by-into`
                         (group-by (fn-> :argtypes (get i|arg)))
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
          (assoc-in spec [:cases 0]  (-> methods' first :rtype >class-spec))
          (assoc-in spec [:cases ct] (partition-deep (xp/condpf-> t/<= (xp/get 0)) methods' ct 0 0))))))

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

(defn analyze-seq|dot|method-call
  "A note will be made of what methods match the argument types.
   If only one method is found, that is noted too. If no matching method is found, an
   exception is thrown."
  {:params-doc '{methods "A reducible of all static/instance `Method`s with the given name,
                          `method-form`, in the given `target`'s class."}}
  [env form target target-class static? #_t/boolean? method-form #_t/unqualified-symbol? args-forms]
  ;; TODO cache spec by method
  (if-not-let [methods-for-name (-> target-class class->methods|with-cache (get (name method-form)))]
    (if (empty? args-forms)
        (err! "No such method or field in class" {:class target-class :method-or-field method-form})
        (err! "No such method in class"          {:class target-class :methods         method-form}))
    (if-not-let [methods-for-count (get methods-for-name (count args-forms))]
      (err! "Incorrect number of arguments for method"
            {:class target-class :method method-form :possible-counts (set (keys methods-for-name))})
      (let [static?>kind (fn [static?] (if static? :static :instance))]
        (if-not-let [methods (get methods-for-count (static?>kind static?))]
          (err! (istr "Method found for arg-count, but was ~(static?>kind (not static?)), not ~(static?>kind static?)")
                {:class target-class :method method-form :args args-forms})
          (let [args-ct (count args-forms)
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
                        (if (seq arg-specs)
                            (do (err! "TODO arg spec")
                                #_(if (t/infer? arg-spec)
                                      (swap! arg-spec t/and (get ret-spec i))
                                      ((get ret-spec i) arg-spec)))
                            (ret-spec arg-specs)))))
                ?cast-spec (?cast-call->spec target-class method-form)
                _ (when ?cast-spec
                    (err! "TODO cast spec")
                    #_(s/validate (-> with-ret-spec :args first :spec) #(t/>= % (t/numerically ?cast-spec))))]
            with-ret-spec))))))

(defns analyze-seq|dot|field-access
  [env _, form _, target _, field-form _ #_t/unqualified-symbol?, field Field]
  (ast/field-access
    {:env    env
     :form   form
     :target target
     :field  field-form
     :spec   (-> field .getType t/>spec)}))

;; TODO spec these arguments; e.g. check that ?method||field, if present, is an unqualified symbol
(defn analyze-seq|dot [env form [target-form ?method-or-field & ?args]]
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
              {target-class :class target-class-nilable? :nilable?}
                (or ?target-static-class-map (-> target :spec t/spec>class))]
          ;; TODO determine how to handle `target-class-nilable?`; for now we will just let it slip through
          ;; to `NullPointerException` at runtime rather than create a potentially more helpful custom
          ;; exception
          (if-let [field (and (empty? args-forms)
                              (-> target-class class->fields|with-cache (get (name method-or-field))))]
            (analyze-seq|dot|field-access env form target method-or-field field)
            (analyze-seq|dot|method-call env form target target-class (boolean ?target-static-class-map)
              method-or-field args-forms))))))

(defn analyze-seq|if
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
            (do (TODO "fix `if` analysis")
                #_(->expr-info
                  {:env       env
                   :form      (list 'if pred (:form @true-expr) (:form @false-expr))
                   :type-info (union|type-info (:type-info @true-expr) (:type-info @false-expr))})))]
    (case (truthy-expr? pred-expr)
      ::unknown @whole-expr
      true      (-> @true-expr  (assoc :env env)
                                (cond-> (not *conditional-branch-pruning?*)
                                        (assoc :form (list 'if pred (:form @true-expr) false-form))))
      false     (-> @false-expr (assoc :env env)
                                (cond-> (not *conditional-branch-pruning?*)
                                        (assoc :form (list 'if pred true-form          (:form @false-expr))))))))

(defn analyze-seq|quote [env form body]
  {:post [(prl! %)]}
  (ast/quoted env form (tcore/most-primitive-class-of body)))

(defn analyze-seq*
  "Analyze a seq after it has been macro-expanded.
   The ->`form` is post- incremental macroexpansion."
  [env [sym & body :as form]]
  (ppr! {'expanded-form form})
  (if (special-symbols sym)
      (case sym
        do
          (analyze-seq|do    env form body)
        let*
          (analyze-seq|let*  env form body)
        deftype*
          (TODO "deftype*")
        fn*
          (TODO "fn*")
        def
          (TODO "def")
        .
          (analyze-seq|dot   env form body)
        if
          (analyze-seq|if    env form body)
        quote
          (analyze-seq|quote env form body))
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
        (ast/macro-call {:env env :form form :expanded (analyze-seq* env expanded-form)}))))

(defn analyze-symbol [env form]
  {:post [(prl! %)]}
  (let [resolved (?resolve-with-env form env)]
    (if-not resolved
      (err! "Could not resolve symbol" {:sym form})
      (ast/symbol env form
        (cond (ast/node? resolved)
                (:spec resolved)
              (or (t/literal? resolved) (t/class? resolved))
                (t/value resolved)
              :else
                (err! "Unsure of what to do in this case" (kw-map env form resolved)))))))

(defn analyze* [env form]
  (prl! env form)
  (when (> (swap! *analyze-i inc) 100) (throw (ex-info "Stack too deep" {:form form})))
  (cond (symbol? form)
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
        :else
          (throw (ex-info "Unrecognized form" {:form form}))))

(defn analyze
  ([body] (analyze {} body))
  ([env body]
    (reset! *analyze-i 0)
    (analyze* env body)))

;; ===== (DE)FNT ===== ;;

#_(s/def :fnt|overload/arglist-code (t/vec-of arg?))
#_(s/def :fnt|overload/args-classes (t/vec-of t/class?))
(s/def :fnt|overload/positional-args-ct integer?)
(s/def :fnt|overload/variadic?          boolean?)

 #_"Must evaluate to an `s/fspec`"
(s/def :fnt|overload/spec               ::ss/code)

#_(s/def :fnt|overload/body-codelist (t/seq-of ::ss/code))
(s/def :fnt/overload
  (s/keys :req-un [:fnt|overload/arg-classes
                   :fnt|overload/arg-specs
                   :fnt|overload/arglist-code|fn|hinted
                   :fnt|overload/arglist-code|reify|unhinted
                   :fnt|overload/body-codelist
                   :fnt|overload/positional-args-ct
                   :fnt|overload/spec
                   :fnt|overload/variadic?]))

(s/def ::reify|overload
  (s/keys :req-un [:ss/interface
                   :reify|overload/out-class
                   :reify/method-sym
                   :reify/arglist-code
                   :reify|overload/body-codelist]))

#?(:clj
(defn fnt|arg->class [lang {:as arg [k spec] ::fnt|arg-spec :keys [arg-binding]}]
  (cond (not= k :spec) java.lang.Object; default class
        (symbol? spec) (pred->class lang spec))))

(defn spec>most-primitive-class [spec]
  (let [class-data (t/spec>class spec)]
    (cond (-> class-data :class class? not)
            (err! "Found multiple classes corresponding to spec; don't know how to handle yet"
                  {:spec spec :class-data class-data})
          (-> class-data :nilable? not)
            (-> class-data :class tcore/boxed->unboxed)
          :else (:class class-data))))

#?(:clj ; really, reserve for metalanguage
(defn fnt|overload-data->overload #_> #_::fnt|overload
  "Rather than rigging together something in which either:
   1) the Clojure compiler will try to cross its fingers and evaluate code meant to be evaluated in ClojureScript
   2) we use a CLJS-in-CLJS compiler and alienate the mainstream CLJS-in-CLJ (cljsbuild) workflow, which includes
      our own workflow
   3) we wait for CLJS-in-CLJS to become mainstream, which could take years if it really ever happens

   we decide instead to evaluate specs in languages in which the metalanguage (compiler language) is the same as
   the object language (e.g. Clojure), and symbolically analyze specs in the rest (e.g. vanilla ClojureScript),
   deferring code analyzed as functions to be enforced at runtime."
  [{{:keys [args varargs] pre-form :pre post-form :post} ::fnt|arglist body-form :body}
   {:as opts :keys [lang symbolic-analysis?]}]
  (prl! args body-form)
  (if symbolic-analysis?
      (err! "Symbolic analysis not supported yet")
      (let [varargs-binding (when varargs
                              ;; TODO this validation is purely temporary until destructuring is supported
                              (s/validate (:arg-binding varargs) simple-symbol?))
            arg-bindings (mapv :arg-binding args)
            ;; so `env` can work properly in the analysis
            _ (s/validate arg-bindings distinct?)
            arg-specs-initial
              (->> args
                   (mapv (fn [{[kind spec] ::fnt|arg-spec :keys [arg-binding]}]
                           ;; TODO this validation is purely temporary until destructuring is supported
                           (s/validate arg-binding simple-symbol?)
                           (ast/unbound nil arg-binding
                             (case kind :any t/any? :infer t/? :spec (-> spec eval t/>spec))))))
            env (zipmap arg-bindings arg-specs-initial)
            body-form|wrapped-do (list* 'do body-form)
            body (analyze env body-form|wrapped-do)
            env' (:env body)
            arg-specs (->> arg-bindings (mapv #(:spec (get env' %))))
            _ (prl! body)
            arg-classes (->> arg-specs (mapv spec>most-primitive-class))
            hint-arg|fn
              (fn [i arg-binding]
                (th/with-type-hint arg-binding
                  (th/->fn-arglist-tag
                    (get arg-classes i)
                    lang
                    (count args)
                    varargs)))
            post-spec (when post-form (-> post-form eval t/>spec))
            _ (when (and post-spec (not (t/<= (:spec body) post-spec)))
                (err! "Body does not match output spec" {:body body :output-spec post-spec}))
            validations (when pre-form (TODO "Need to handle pre"))
              #_(->> [(when post-form {:post [post-form]})
                    (when pre-form (list 'assert pre-form))]
                   (lfilter some?))]
        {:arg-classes                 arg-classes
         :arg-specs                   arg-specs
         :arglist-code|fn|hinted      (cond-> (->> arg-bindings (map-indexed hint-arg|fn) vec)
                                              varargs-binding (conj '& varargs-binding)) ; TODO use ``
         :arglist-code|reify|unhinted (cond-> arg-bindings varargs-binding (conj varargs-binding))
         :body-codelist               (concat validations (:body body))
         :positional-args-ct          (count args)
         :spec                        (:spec body)
         ;; when present, varargs are considered to be of class Object
         :variadic?                   (boolean varargs)}))))

(def fnt-method-sym 'invoke)

(defn- class>interface-part-name [c]
  (let [illegal-pattern #"\|\+"]
     (if (->> c >name (re-find illegal-pattern))
         (err! "Class cannot contain pattern" {:class c :pattern illegal-pattern})
         (-> c >name (str/replace "." "|")))))

(defn fnt-overload>interface-sym [args-classes out-class]
  (>symbol (str (->> args-classes (lmap class>interface-part-name) (str/join "+"))
                ">" (class>interface-part-name out-class))))

(defn fnt-overload>interface [args-classes out-class]
  (let [interface-sym     (fnt-overload>interface-sym args-classes out-class)
        hinted-method-sym (th/with-type-hint fnt-method-sym (th/>arglist-embeddable-tag out-class))
        interface-code    `(~'definterface ~interface-sym (~hinted-method-sym ~(cmacros/gen-args (count args-classes))))]
    (log/pr ::debug "Creating interface" interface-sym "...")
    (eval interface-code)))

#?(:clj
(defn >reify-overload #_> #_(seq-of ::reify|overload)
  [out-class primitivized-arg-classes
   {:as overload #_:fnt/overload :keys [arglist-code|reify|unhinted body-codelist]}]
  (s/validate primitivized-arg-classes vector?)
  (let [interface-k {:out out-class :in primitivized-arg-classes}
        interface
          (-> *interfaces
              (swap! update interface-k #(or % (fnt-overload>interface primitivized-arg-classes out-class)))
              (get interface-k))
        arglist-code
          (vec (concat ['_]
                 (doto (->> arglist-code|reify|unhinted
                      (map-indexed
                        (fn [i arg] (th/with-type-hint arg (-> primitivized-arg-classes (doto pr/ppr-meta) (get i) (doto pr/ppr-meta) th/>arglist-embeddable-tag)))))
                 pr/ppr-meta)))]
    {:arglist-code  arglist-code
     :body-codelist body-codelist
     :interface     interface
     :method-sym    fnt-method-sym
     :out-class     out-class})))

#?(:clj
(def sort-guide
  {tdef/boolean 0
   tdef/byte    1
   tdef/short   2
   tdef/char    3
   tdef/int     4
   tdef/long    5
   tdef/float   6
   tdef/double  7
   Object       8}))

#?(:clj
(defn fnt-overload>reify-overloads #_> #_(seq-of ::reify|overload)
  [{:as overload #_:fnt/overload :keys [arg-classes spec]}]
  {:pre  [(prlm! overload)]
   :post [(prlm! %)]}
  (let [out-class-data (t/spec>class spec)
        out-class (if (-> out-class-data :class class?)
                      (:class out-class-data)
                      ;; we don't need to vary the output class if there are multiple output possibilities
                      java.lang.Object)]
    (->> arg-classes
         (lmap (fn [arg-class] (->> arg-class tcore/class>prim-subclasses
                                    (set/union #{arg-class})
                                    (sort-by sort-guide))))
         (apply combo/cartesian-product)
         (lmap (fn [primitivized-arg-classes]
                 (>reify-overload out-class (vec primitivized-arg-classes) overload)))))))

#?(:clj
(defn fnt|overload>reify [{:keys [overload #_:fnt/overload, i #_integer?, fn|name #_::ss/fn|name]}]
  (let [reify-overloads (fnt-overload>reify-overloads overload)]
    `(~'def ~(>symbol (str fn|name "|__" i))
       (reify ~@(->> reify-overloads
                     (lmap (fn [{:keys [interface out-class method-sym arglist-code body-codelist]} #_::reify|overload]
                             [(-> interface >name >symbol)
                              `(~(th/with-type-hint method-sym (th/>arglist-embeddable-tag out-class))
                                ~arglist-code ~@body-codelist)]))
                     lflatten-1))))))

(defn gen-register-spec
  "Registers in the map of qualified symbol to input spec, to output spec

   Example output:
   (swap! ... assoc `abcde
     (fn [args] (case (count args) 1 <out-spec>)))"
  [{:keys [fn|name arg-ct->spec variadic-overload]}]
  (unify-gensyms
   `(swap! *fn->spec assoc '~(qualify fn|name)
      (xp/>expr
        (fn [args##] (case (count args##) ~@arg-ct->spec
                      ~@(when variadic-overload
                          [`(if (>= (count args##) (:positional-args-ct variadic-overload))
                                (:spec variadic-overload)
                                (err! "Arg count not enough for variadic overload"))])))))
    true))

(defn fnt|code [kind lang args]
  (let [{:keys [::ss/fn|name overloads ::ss/meta] :as args'}
          (s/validate args (case kind :defn ::defnt :fn ::fnt))
        _ (prl! args')
        inline?
          (s/validate (-> fn|name c/meta :inline) (t/? t/boolean?))
          _ (prl! inline?)
        fn|name (if inline?
                    (do (log/pr :warn "requested `:inline`; ignoring until feature is implemented")
                        (update-meta fn|name dissoc :inline))
                    fn|name)
        overloads-data (->> overloads (mapv #(fnt|overload-data->overload % {:lang lang})))
        ;; only one variadic arg allowed
        _ (s/validate overloads-data (fn->> (lfilter :variadic?) count (<- <= 1)))
        arg-ct->spec (->> overloads-data
                          (remove+   :variadic?)
                          (group-by  :positional-args-ct)
                          (map-vals+ :spec)
                          join (apply concat))
        variadic-overload (->> overloads-data (lfilter :variadic?) first)
        register-spec (gen-register-spec (kw-map fn|name arg-ct->spec variadic-overload))
        direct-dispatch-codelist
          (case lang
            :clj  (for [[i overload] (lindexed overloads-data)]
                    (fnt|overload>reify (kw-map overload i fn|name)))
            :cljs (TODO))
        dynamic-dispatch-codelist []
        base-fn-codelist []
        fn-codelist
          (case lang
            :clj  `[~@direct-dispatch-codelist
                    ~@dynamic-dispatch-codelist
                    ~@base-fn-codelist]
            :cljs (TODO))
        overloads (->> overloads-data (mapv :code))
        _ (prl! overloads)
        code (case kind
               :fn   (list* 'fn (concat
                                  (if (contains? args' ::ss/fn|name)
                                      [fn|name]
                                      [])
                                  [overloads]))
               :defn `(~'do ~register-spec
                            ~@fn-codelist))]
    code))

(defmacro fnt [& args]
  (fnt|code :fn (cmacros/env-lang) args))

(defmacro defnt [& args]
  (fnt|code :defn (cmacros/env-lang) args))

)
