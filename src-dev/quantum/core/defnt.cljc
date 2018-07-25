(ns quantum.core.defnt
  (:refer-clojure :exclude
    [+ #_zero? odd? even?
     bit-and
     ==])
  (:require
    [clojure.core                               :as core]
    [clojure.string                             :as str]
    [quantum.core.type.core                     :as tcore]
    [quantum.core.type.defs                     :as tdef]
    [quantum.untyped.core.analyze.ast           :as ast]
    [quantum.untyped.core.analyze.expr          :as xp]
    [quantum.untyped.core.analyze.rewrite       :as ana-rw]
    [quantum.untyped.core.collections           :as c
      :refer [dissoc-if dissoc* lcat subview >vec >set
              lmap map+ map-vals+ mapcat+ filter+ remove+ partition-all+]]
    [quantum.untyped.core.collections.logic     :as ucl
      :refer [seq-and seq-or]]
    [quantum.untyped.core.collections.tree      :as tree
      :refer [prewalk postwalk walk]]
    [quantum.untyped.core.compare               :as comp
      :refer [==]]
    [quantum.untyped.core.convert               :as conv
      :refer [>symbol >name]]
    [quantum.untyped.core.core
      :refer [istr]]
    [quantum.untyped.core.data
      :refer [kw-map]]
    [quantum.untyped.core.data.array            :as arr]
    [quantum.untyped.core.data.map              :as map]
    [quantum.untyped.core.data.set              :as set]
    [quantum.untyped.core.defnt
      :refer [defns defns- fns]]
    [quantum.untyped.core.error                 :as err
      :refer [TODO err!]]
    [quantum.untyped.core.fn
      :refer [aritoid fn1 fnl fn', fn-> fn->> <-, rcomp
              firsta seconda]]
    [quantum.untyped.core.form                  :as uform
      :refer [>form]]
    [quantum.untyped.core.form.evaluate         :as ufeval]
    [quantum.untyped.core.form.generate         :as ufgen
      :refer [unify-gensyms]]
    [quantum.untyped.core.form.type-hint        :as ufth]
    [quantum.untyped.core.log                   :as log
      :refer [ppr! ppr prl! prlm!]]
    [quantum.untyped.core.logic                 :as l
      :refer [fn= fn-and fn-or fn-not ifs if-not-let]]
    [quantum.untyped.core.loops                 :as loops
      :refer [reduce-2]]
    [quantum.untyped.core.numeric.combinatorics :as combo]
    [quantum.untyped.core.print                 :as pr]
    [quantum.untyped.core.qualify               :as qual
      :refer [qualify]]
    [quantum.untyped.core.reducers              :as r
      :refer [join reducei educe]]
    [quantum.untyped.core.refs                  :as ref
      :refer [?deref]]
    [quantum.untyped.core.spec                  :as s]
    [quantum.untyped.core.specs                 :as uss]
    [quantum.untyped.core.type                  :as t
      :refer [?]]
    [quantum.untyped.core.type.predicates       :as utpred]
    [quantum.untyped.core.type.reifications     :as utr]
    [quantum.untyped.core.vars                  :as var
      :refer [update-meta]]
    #_[quantum.format.clojure.core ; TODO temporary
      :refer [reformat-string]])
  (:import
    [quantum.core Numeric]
    [quantum.core.data Array]))

;; TODO move
#_(defn ppr-code [code]
  (let [default-indentations '{do [[:inner 2 2]]
                               if [[:inner 2 2]]}]
    (-> code pr/ppr-meta with-out-str
        (reformat-string {:indents default-indentations})
        println)))

#_(:clj (ns-unmap (find-ns 'quantum.core.defnt) 'reformat-string))

#_"

LEFT OFF LAST TIME (7/24/2018):
- ;; TODO probably failing because class vs. symbol
- This is because of the `>form` not quite returning the right thing for `t/isa?` stuff in reifications
- After that, keep going making sure the test cases pass, especially the >int* cases



- With `defnt`, protocols and interfaces aren't needed. You can just create `t/fn`s that you can
  then conform your fns to.
- `dotyped`, `defnt`, and `fnt` create typed contexts in which their internal forms are analyzed
  and overloads are resolved.
- `defnt` is intended to catch many runtime errors at compile time, but cannot catch all of them;
  types will very often have to be validated at runtime.

[ ] Compile-Time (Direct) Dispatch
    - Any argument, if it requires a non-nilable primitive-like value, will be marked as a
      primitive.
    - If nilable, there will be one overload for nil and one for primitive.
    - When a `fnt` with type overloads is referenced outside of a typed context, then the overload
      resolution will be done via Runtime Dispatch.
    - TODO Should we take into account 'actual' types (not just 'declared' types) when performing
      dispatch / overload resolution?
      - Let's take the example of `(defnt abcde [] (f (rand/int-between -10 -2)))`.
        - Let's say `rand/int-between`'s output is labeled `t/int?`. However, we know based on
          further static analysis of its implementation that the output is not only `t/int?` but
          also `t/neg?`, or perhaps even further, `(< -10 % -2)`.
        - In this case, should we take advantage of this knowledge?
          - Let's say we do. Then `(.invoke reify|we-know-specifics (rand/int-between -10 -2))`.
            Yay for efficiency! But let's then say we then change the implementation even if we
            don't change the 'interface'/typedefs. Now `rand/int-between` returns `(<= -10 % -2)` —
            that is, it's now numerically *inclusive* (for instance, maybe the implementation's
            previous behavior of generating numbers numerically *exclusive*ly was mistaken).
            `reify|we-know-specifics` would then still be invoked but incorrectly (and unsafely) so.
          - To be fair, we'll tend to change output specs/typedefs all the time as we do
            development. Do we need to keep track of every call site it affects and recompile
            accordingly? Perhaps. It seems like overkill though. It should be configurable in any
            case.
          - I think that because of this last point, we can and should rely on implementational
            specifics wherever available to boost performance (Maybe this should be configurable so
            it doesn't slow down development? The more we change the implementation, the more it has
            to recompile, ostensibly). We can take advantage of the output specs, certainly, if for
            nothing else than to ensure that our implementation (as characterized by its 'actual'
            output type) matches what we expect (as characterized by its 'expected'/'declared'
            output type).
          - One option (Option A) is to turn off compile-time overload resolution during
            development. This would mean it might get very slow during that time. But if it's in
            the same `defnt` (ignoring `extend-defnt!` for a minute) — like a recursive call — you
            could always leave on compile-time resolution for that.
          - Option B — probably better (though we'd still like to have all this configurable) —
            is to have each function know its dependencies (this would actually have the bonus
            property of enabling `clojure.tools.namespace.repl/refresh`-style function-level
            smart auto-recompilation which is nice). So let's go back to the previous example.
            `abcde` could keep track of (or the `defnt` ns could keep track of it, but you get the
            point) the fact that it depends on `rand/int-between` and `f`. It has a compile-time-
            resolvable call site that depends only on the output type of `rand/int-between` so if
            `rand/int-between`'s computed/actual output type (when given the inputs in question)
            ever changes, `abcde` needs to be recompiled and `abcde`'s output type recomputed. If,
            on the other hand, `f`'s output type (given the input) ever changes, `abcde` need not be
            recompiled, but rather, only its output type need be recomputed.
          - I think this reactive approach (do we need a library for that? probably not?) should
            solve our problems and let us code in a very flexible way. It'll just (currently) be a
            way that depends on a compiler in which the metalanguage and object language are
            identical.
[ ] Runtime (Dynamic) Dispatch
    [—] Protocol generation
        - For now we won't do it because we can very often find the correct overload at compile
          time. We will resort to using the `fn`.
        - It will be left as an optimization.
    [ ] `fn` generation
        - Performs a worst-case linear check of the typedefs, `cond`-style.
[ ] Interface generation
    - Even if the `defnt` is redefined, you won't have interface problems.
[ ] `reify` generation
    - Which `reify`s get generated is mainly up to the inputs but partially up to the fn body —
      If any typed fns are called in the fn body then this can change what gets generated.
      - TODO explain this more
    - Each of the `reify`s will keep their label (`__2__0` or whatever) as long as the original
      typedef of the `reify` is `t/=` to the new typedef of that reify
      - If a redefined `defnt` doesn't have that type overload then the previous reify is uninterned
        and thus made unavailable
      - That way, according to the dynamicity tests in `quantum.test.core.defnt`, we can redefine
        implementations at will as long as the specs don't change
      - To make this process faster we maintain a set of typedefs so at least cheap c/= checks can
        be performed
        - If c/= succeeds, great; the `reify` corresponding to the label (and reify-type) will be
          replaced; the typedef-set will remain unchanged
        - Else it must find a corresponding typedef by t/=
          - Then if it is found by t/= it will replace the `reify` and the typedef corresponding
            with that label and replace the typedef in the typedef-set
          - Else a new label will be given to the `reify`; the typedef will be added to the
            typedef-set
[ ] Types yielding generative specs
[—] Types using the clojure.spec interface
    - Not yet; wait for it to come out of alpha
[—] Support for compilers in which the metalanguage differs from the object language (i.e. 'normal'
    non-CLJS-in-CLJS CLJS)
    - This will have to be approached later. We'll figure it out; maybe just not yet.
[—] `extend-defnt!`
    - Not yet; probably complicated and we don't need it right now
"

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
(defns type>most-primitive-classes [t t/type? > (s/set-of (? t/class?))]
  (let [cs (t/type>classes t) nilable? (contains? cs nil)]
    (->> cs
         (c/map+ #(class>most-primitive-class % nilable?))
         (join #{})))))

#?(:clj
(defns type>most-primitive-class [t t/type? > (? t/class?)]
  (let [cs (type>most-primitive-classes t)]
    (if (-> cs count (not= 1))
        (err! "Not exactly 1 class found" (kw-map t cs))
        (first cs)))))

#?(:clj
(defns out-type>class [t t/type? > (? t/class?)]
  (let [cs (t/type>classes t) cs' (disj cs nil)]
    (if (-> cs' count (not= 1))
        ;; NOTE: we don't need to vary the output class if there are multiple output possibilities
        ;; or just nil
        java.lang.Object
        (-> (class>most-primitive-class (first cs') (contains? cs nil))
            class>simplest-class)))))

; ----- TYPED PART ----- ;

;; NOTE: All this code can be defnt-ized after; this is just for bootstrapping purposes so performance isn't extremely important in most of these functions.

(defonce *fn->type (atom {}))

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
       (remove+    (fn [^java.lang.reflect.Method x]
                     (java.lang.reflect.Modifier/isPrivate (.getModifiers x))))
       (map+       (fn [^java.lang.reflect.Method x]
                     (Method. (.getName x) (.getReturnType x) (.getParameterTypes x)
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

(defrecord Field [^String name ^Class class ^clojure.lang.Keyword kind]
  fipp.ednize/IOverride
  fipp.ednize/IEdn (-edn [this] (tagged-literal (symbol "F") (into (array-map) this))))

(defns class->fields [^Class c t/class? > t/map?]
  (->> (.getFields c)
       (remove+   (fn [^java.lang.reflect.Field x]
                    (java.lang.reflect.Modifier/isPrivate (.getModifiers x))))
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
               :type t/nil?})
      (let [expr (analyze-non-map-seqable env body []
                   (fn [accum expr _]
                     ;; for types, only the last subexpression ever matters, as each is independent :; from the others
                     (assoc expr :form (conj! (:form accum) (:form expr))
                                 ;; but the env should be the same as whatever it was originally
                                 ;; because no new scopes are created
                                 :env  (:env accum))))]
        (ast/do {:env  env
                 :form form
                 :body (>vec body)
                 :type (:type expr)}))))

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

(defns ?resolve-with-env [sym t/symbol?, env ::env]
  (let [local (c/get env sym)]
    (if (some? local)
        (if (ast/unbound? local)
            local
            (TODO "Need to figure out what to do when resolving local vars"))
        (let [resolved (ns-resolve *ns* sym)]
          (log/ppr :warn "Not sure how to handle non-local symbol; resolved it for now" (kw-map sym resolved))
          resolved))))

(defns methods->type
  "Creates a type given ->`methods`."
  [methods (s/seq-of t/any? #_method?) > t/type?]
  ;; TODO room for plenty of optimization here
  (let [methods|by-ct (->> methods
                           (c/group-by (fn-> :argtypes count))
                           (sort-by first <))
        ;; non-primitive classes in Java aren't guaranteed to be non-null
        >class-type (fn [x]
                      (ifs (class? x)
                             (-> x t/>type (cond-> (not (t/primitive-class? x)) t/?))
                           (t/type? x)
                             x
                           (err/not-supported! `>class-type x)))
        partition-deep
          (fn partition-deep [t methods' arglist-size i|arg depth]
            (let [_ (when (> depth 3) (TODO))
                  methods'|by-class
                    (->> methods'
                         ;; TODO optimize further via `group-by-into`
                         (c/group-by (fn-> :argtypes (c/get i|arg)))
                         ;; classes will be sorted from most to least specific
                         (sort-by (fn-> first t/>type) t/<))]
              (r/for [[c methods''] methods'|by-class
                      t' t]
                (update t' :clauses conj
                  [(>class-type c)
                   (if (= (inc depth) arglist-size)
                       ;; here, methods'' count will be = 1
                       (-> methods'' first :rtype >class-type)
                       (partition-deep
                         (xp/condpf-> t/<= (xp/get (inc i|arg)))
                         methods''
                         arglist-size
                         (inc i|arg)
                         (inc depth)))]))))]
    (r/for [[ct methods'] methods|by-ct
            t (xp/casef count)]
      (if (zero? ct)
          (c/assoc-in t [:cases 0]  (-> methods' first :rtype >class-type))
          (c/assoc-in t [:cases ct] (partition-deep (xp/condpf-> t/<= (xp/get 0)) methods' ct 0 0))))))

#?(:clj
(defns ?cast-call->type
  "Given a cast call like `clojure.lang.RT/uncheckedBooleanCast`, returns the
   corresponding type.

   Unchecked fns could be assumed to actually *want* to shift the range over if the
   range hits a certain point, but we do not make that assumption here."
  [c t/class?, method t/symbol? > (? t/type?)]
  (when (identical? c clojure.lang.RT)
    (case method
      (uncheckedBooleanCast booleanCast) t/boolean?
      (uncheckedByteCast    byteCast)    t/byte?
      (uncheckedCharCast    charCast)    t/char?
      (uncheckedShortCast   shortCast)   t/char?
      (uncheckedIntCast     intCast)     t/int?
      (uncheckedLongCast    longCast)    t/long?
      (uncheckedFloatCast   floatCast)   t/float?
      (uncheckedDoubleCast  doubleCast)  t/double?
      nil))))

(defns- analyze-seq|dot|method-call
  "A note will be made of what methods match the argument types.
   If only one method is found, that is noted too. If no matching method is found, an
   exception is thrown."
  [env ::env, form _, target _, target-class t/class?, static? t/boolean?, method-form simple-symbol?, args-forms _ #_(seq-of form?)]
  ;; TODO cache type by method
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
                        :type   (methods->type methods #_(count arg-forms))})
                with-arg-types
                  (r/fori [arg-form args-forms
                           call'    call
                           i|arg]
                    (prl! call' arg-form)
                    (let [arg-node (analyze* env arg-form)]
                      ;; TODO can incrementally calculate return value, but possibly not worth it
                      (update call' :args conj arg-node)))
                with-ret-type
                  (update with-arg-types :type
                    (fn [ret-type] (->> with-arg-types :args (mapv :type) ret-type)))
                ?cast-type (?cast-call->type target-class method-form)
                _ (when ?cast-type
                    (ppr :warn "Not yet able to statically validate whether primitive cast will succeed at runtime" {:form form})
                    #_(s/validate (-> with-ret-type :args first :type) #(t/>= % (t/numerically ?cast-type))))]
            with-ret-type))))))

(defns- analyze-seq|dot|field-access
  [env ::env, form _, target _, field-form simple-symbol?, field (t/isa? Field)]
  (ast/field-access
    {:env    env
     :form   form
     :target target
     :field  field-form
     :type   (-> field :class t/>type)}))

(defns classes>class
  "Ensure that given a set of classes, that set consists of at most a class C and nil.
   If so, returns C. Otherwise, throws."
  [cs (s/set-of (? t/class?)) > t/class?]
  (let [cs' (disj cs nil)]
    (if (-> cs' count (= 1))
        (first cs')
        (err! "Found more than one class" cs))))

;; TODO type these arguments; e.g. check that ?method||field, if present, is an unqualified symbol
(defns- analyze-seq|dot [env ::env, form _, [target-form _, ?method-or-field _ & ?args _] _]
  {:pre  [(prl! env form target-form ?method-or-field ?args)]
   :post [(prl! %)]}
  (let [target          (analyze* #_?resolve-with-env env target-form)
        method-or-field (if (symbol? ?method-or-field) ?method-or-field (first ?method-or-field))
        args-forms      (if (symbol? ?method-or-field) ?args            (rest  ?method-or-field))]
    (if (t/= (:type target) t/nil?)
        (err! "Cannot use the dot operator on nil." {:form form})
        (let [;; `nilable?` because technically any non-primitive in Java is nilable and we can't
              ;; necessarily rely on all e.g. "@nonNull" annotations
              {:as ?target-static-class-map target-static-class :class target-static-class-nilable? :nilable?}
                (-> target :type t/type>?class-value)
              target-classes
                (if ?target-static-class-map
                    (cond-> #{target-static-class} target-static-class-nilable? (conj nil))
                    (-> target :type t/type>classes))
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
(defns truthy-expr? [{:as expr t [:type _]} _ > t/boolean?]
  (ifs (or (t/= t t/nil?)
           (t/= t t/false?)) false
       (or (t/> t t/nil?)
           (t/> t t/false?)) nil ; representing "unknown"
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
                   :type       (apply t/or (->> [(:type @true-expr) (:type @false-expr)] (remove nil?)))}))]
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
    (if-not (and (-> c|analyzed :type t/value-type?)
                 (-> c|analyzed :type utr/value-type>value class?))
            (err! "Supplied non-class to `new` expression" {:x c|form})
            (let [c             (-> c|analyzed :type utr/value-type>value)
                  args|analyzed (mapv #(analyze* env %) args)]
              (ast/new-expr {:env   env
                             :form  (list* 'new c|form (map :form args|analyzed))
                             :class c
                             :args  args|analyzed
                             :type  (t/isa? c)})))))

(defns- analyze-seq|throw [env ::env, form _ [arg _ :as body] _]
  {:pre [(prl! env form body)]}
  (if (-> body count (not= 1))
      (err! "Must supply exactly one input to `throw`; supplied" {:body body})
      (let [arg|analyzed (analyze* env arg)]
        ;; TODO this is not quite true for CLJS but it's nice at least
        (if-not (-> arg|analyzed :type (t/<= t/throwable?))
          (err! "`throw` requires a throwable; received" {:arg arg :type (:type arg|analyzed)})
          (ast/throw-expr {:env  env
                           :form (list 'throw (:form arg|analyzed))
                           :arg  arg|analyzed
                           ;; `t/none?` because nothing is actually returned
                           :type t/none?})))))

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
             caller|type (:type caller|expr)
             args-ct     (count body)]
         (case (t/compare caller|type t/callable?)
           (1 2)  (err! "It is not known whether expression be called" {:expr caller|expr})
           3      (err! "Expression cannot be called" {:expr caller|expr})
           (-1 0) (let [assert-valid-args-ct
                          (ifs (or (t/<= caller|type t/keyword?) (t/<= caller|type t/+map|built-in?))
                               (when-not (or (= args-ct 1) (= args-ct 2))
                                 (err! (str "Keywords and `clojure.core` persistent maps must be provided "
                                            "with exactly one or two args when calling them")
                                       {:args-ct args-ct :caller caller|expr}))

                               (or (t/<= caller|type t/+vector|built-in?) (t/<= caller|type t/+set|built-in?))
                               (when-not (= args-ct 1)
                                 (err! (str "`clojure.core` persistent vectors and `clojure.core` persistent "
                                            "sets must be provided with exactly one arg when calling them")
                                       {:args-ct args-ct :caller caller|expr}))

                               (t/<= caller|type t/fnt?)
                               (TODO "Don't know how to handle typed fns yet" {:caller caller|expr})
                               ;; For non-typed fns, unknown; we will have to risk runtime exception
                               ;; because we can't necessarily rely on metadata to tell us the whole truth
                               (t/<= caller|type t/fn?)
                               nil
                               ;; If it's ifn but not fn, we might have missed something in this dispatch so for now we throw
                               (err! "Don't know how how to handle non-fn ifn" {:caller caller|expr}))
                        {:keys [args] t :type}
                          (->> body
                               (c/map+ #(analyze* env %))
                               (reduce (fn [{:keys [args]} arg|analyzed]
                                         (conj args))))]

                    ;; TODO incrementally check by analyzing each arg in `reduce` and pruning branches of what the
                    ;; type could be, and throwing if it's found something that's an impossible combination
                    (ast/call-expr
                      {:env    env
                       :form   form
                       :caller caller|expr
                       :args   args
                       :type   t}))))))

(defns- analyze-seq [env ::env, form _]
  {:post [(prl! %)]}
  (prl! form)
  (let [expanded-form (ufeval/macroexpand form)]
    (if (== form expanded-form)
        (analyze-seq* env expanded-form)
        (ast/macro-call {:env env :form form :expanded (analyze-seq* env expanded-form)}))))

(defns- analyze-symbol [env ::env, form t/symbol?]
  {:post [(prl! %)]}
  (let [resolved (?resolve-with-env form env)]
    (if-not resolved
      (err! "Could not resolve symbol" {:sym form})
      (ast/symbol env form
        (ifs (ast/node? resolved)
               (:type resolved)
             (or (t/literal? resolved) (t/class? resolved))
               (t/value resolved)
             (var? resolved)
               (or (-> resolved meta :type)
                   (t/value @resolved))
             (utpred/unbound? resolved)
               ;; Because the var could be anything and cannot have metadata (type or otherwise)
               t/any?
             (TODO "Unsure of what to do in this case" (kw-map env form resolved)))))))

(defns- analyze* [env ::env, form _]
  (prl! env form)
  (when (> (swap! *analyze-i inc) 100) (throw (ex-info "Stack too deep" {:form form})))
  (ifs (symbol? form)
         (analyze-symbol env form)
       (t/literal? form)
         (ast/literal env form (t/>type form))
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

;; Internal specs

(s/def ::expanded-overload|arg-classes (s/vec-of t/class?))
(s/def ::expanded-overload|arg-types   (s/seq-of t/type?))

;; This is the overload after the input specs are split by their respective `t/or` constituents,
;; and after primitivization, but before readiness for incorporation into a `reify`.
;; One of these corresponds to one reify overload.
(s/def ::expanded-overload
  (s/kv {:arg-classes                 ::expanded-overload|arg-classes
         :arg-types                   ::expanded-overload|arg-types
         :arglist-code|fn|hinted      t/any?
         :arglist-code|reify|unhinted t/any?
         :body-form                   t/any?
         :out-class                   (? t/class?)
         :out-type                    t/type?
         :positional-args-ct          t/nneg-int?
         ;; When present, varargs are considered to be of class Object
         :variadic?                   t/boolean?}))

(s/def ::reify|overload
  (s/keys :req-un [:quantum.core.specs/interface
                   :reify|overload/out-class
                   :reify/method-sym
                   :reify/arglist-code
                   :reify|overload/body-form]))

(s/def ::reify
  (s/kv {:form                      t/any?
         :name                      simple-symbol?
         :non-primitivized-overload ::reify|overload
         :overloads                 (s/vec-of ::reify|overload)}))

(s/def ::lang #{:clj :cljs})

(s/def ::input-types-decl
  (s/kv {:form           t/any?
         :name           simple-symbol?
         :arg-type|split (s/vec-of t/type?)}))

(s/def ::direct-dispatch-data
  (s/kv {:i-arg->input-types-decl (s/vec-of ::input-types-decl)
         :reify-seq               (s/vec-of ::reify)}))

(s/def ::i-overload->direct-dispatch-data (s/vec-of ::direct-dispatch-data))

(s/def ::direct-dispatch
  (s/kv {:form                             t/any?
         :i-overload->direct-dispatch-data ::i-overload->direct-dispatch-data}))

(s/def ::expanded-overload-group|arg-types|form (s/vec-of t/any?))

(s/def ::expanded-overload-group
  (s/kv {:arg-types|form   ::expanded-overload-group|arg-types|form
         :non-primitivized ::expanded-overload
         :primitivized     (s/nilable (s/seq-of ::expanded-overload))}))

(s/def ::expanded-overload-groups|arg-types|split (s/vec-of (s/vec-of t/type?)))
(s/def ::expanded-overload-groups|pre-type|form   t/any?)
(s/def ::expanded-overload-groups|post-type|form  t/any?)

(s/def ::expanded-overload-groups
  (s/kv {:arg-types|pre-split|form    ::expanded-overload-group|arg-types|form
         :pre-type|form               ::expanded-overload-groups|pre-type|form
         :post-type|form              ::expanded-overload-groups|post-type|form
         :arg-types|split             ::expanded-overload-groups|arg-types|split
         :arg-types|recombined        (s/vec-of (s/vec-of t/type?))
         :expanded-overload-group-seq (s/seq-of ::expanded-overload-group)}))

#_(:clj
(defn fnt|arg->class [lang {:as arg [k spec] ::fnt|arg-spec :keys [arg-binding]}]
  (cond (not= k :spec) java.lang.Object; default class
        (symbol? spec) (pred->class lang spec))))

;; TODO optimize such that `post-type|form` doesn't create a new type-validator wholesale every
;; time the function gets run; e.g. extern it
(defn >with-post-type|form [body post-type|form] `(t/validate ~body ~post-type|form))

#?(:clj
(var/def sort-guide "for use in arity sorting, in increasing conceptual (and bit) size"
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
(defns arg-types>arg-classes-seq|primitivized
  "'primitivized' meaning given an arglist whose types are `[t/any?]` this will output:
   [[java.lang.Object]
    [boolean]
    [byte]
    [short]
    [char]
    [int]
    [long]
    [float]
    [double]]
   which includes all primitive subclasses of the type."
  [arg-types (s/seq-of t/type?) > (s/seq-of ::expanded-overload|arg-classes)]
  (->> arg-types
       (c/lmap (fn [t #_t/type?]
                 (if (-> t meta :ref?)
                     (-> t t/type>classes (disj nil) seq)
                     (let [cs (type>most-primitive-classes t)
                           base-classes
                             (cond-> (>set cs)
                               (contains? cs nil) (-> (disj nil) (conj java.lang.Object)))]
                       (->> cs
                            (c/map+ tcore/class>prim-subclasses)
                            (educe (aritoid nil identity set/union) base-classes)
                            ;; for purposes of cleanliness and reproducibility in tests
                            (sort-by sort-guide))))))
       (apply combo/cartesian-product)
       (c/lmap >vec))))

;; TODO spec args
#?(:clj
(defns- >expanded-overload
  "Is given `arg-classes` and `arg-types`. In order to determine the out-type, performs an analysis
   using (in part) these pieces of data, but does not use the possibly-updated `arg-types` as
   computed in the analysis. As a result, does not yet support type inference."
  [{:keys [arg-bindings _, arg-classes ::expanded-overload|arg-classes
           post-type|form _
           arg-types ::expanded-overload|arg-types, body-codelist|pre-analyze _, lang ::lang
           varargs _, varargs-binding _]} _
   > ::expanded-overload]
  (let [env         (->> (zipmap arg-bindings arg-types)
                         (c/map' (fn [[arg-binding arg-type]]
                                   [arg-binding (ast/unbound nil arg-binding arg-type)])))
        analyzed    (analyze env (ufgen/?wrap-do body-codelist|pre-analyze))
        arg-classes|simplest (->> arg-classes (c/map class>simplest-class))
        hint-arg|fn (fn [i arg-binding]
                      (ufth/with-type-hint arg-binding
                        (ufth/>fn-arglist-tag
                          (c/get arg-classes|simplest i)
                          lang
                          (c/count arg-bindings)
                          varargs)))
        ;; TODO this becomes an issue when `post-type|form` references local bindings
        post-type (eval post-type|form)
        post-type|runtime? (-> post-type meta :runtime?)
        out-type (if post-type
                     (if post-type|runtime?
                         (case (t/compare post-type (:type analyzed))
                           -1     post-type
                            1     (:type analyzed)
                            0     post-type
                            (2 3) (err! "Body and output type comparison not handled"
                                        {:body analyzed :output-type post-type}))
                         (if (t/<= (:type analyzed) post-type)
                             (:type analyzed)
                             (err! "Body does not match output type"
                                   {:body analyzed :output-type post-type})))
                     (:type analyzed))
        body-form
          (-> (:form analyzed)
              (cond-> post-type|runtime? (>with-post-type|form post-type|form))
              (ufth/cast-bindings|code
                (->> (c/zipmap-into (map/om) arg-bindings arg-classes)
                     (c/remove-vals' (fn-or nil? (fn= java.lang.Object) t/primitive-class?)))))]
      {:arg-classes                 arg-classes|simplest
       :arg-types                   arg-types
       :arglist-code|fn|hinted      (cond-> (->> arg-bindings (c/map-indexed hint-arg|fn))
                                            varargs-binding (conj '& varargs-binding)) ; TODO use ``
       :arglist-code|reify|unhinted (cond-> arg-bindings varargs-binding (conj varargs-binding))
       :body-form                   body-form
       :positional-args-ct          (count arg-bindings)
       :out-type                    out-type
       :out-class                   (out-type>class out-type)
       :variadic?                   (boolean varargs)})))

(defns >expanded-overload-group
  [{:as in :keys [arg-types ::expanded-overload-group|arg-types|form]} _
   > ::expanded-overload-group]
  (let [arg-types|form (mapv >form arg-types)
        ;; `non-primitivized` is first because of class sorting
        [non-primitivized & primitivized :as overloads]
          (->> arg-types
               arg-types>arg-classes-seq|primitivized
               (mapv (fn [arg-classes #_::expanded-overload|arg-classes]
                       (let [arg-types|satisfying-primitivization
                               (c/mergev-with
                                 (fn [_ s #_t/type? c #_t/class?]
                                   (cond-> s (t/primitive-class? c) (t/and c)))
                                 arg-types arg-classes)]
                         (>expanded-overload
                           (assoc in :arg-classes arg-classes
                                     :arg-types   arg-types|satisfying-primitivization))))))]
    (kw-map arg-types|form non-primitivized primitivized)))

;; TODO spec
#?(:clj ; really, reserve for metalanguage
(defns fnt|overload-data>expanded-overload-groups
  "Given an `fnt` overload, computes a seq of 'expanded-overload groups'. Each expanded-overload
   group is the foundation for one `reify`.

   Rather than rigging together something in which either:
   1) the Clojure compiler will try to cross its fingers and evaluate code meant to be evaluated in
      ClojureScript
   2) we use a CLJS-in-CLJS compiler and alienate the mainstream CLJS-in-CLJ (cljsbuild) workflow,
      which includes our own workflow
   3) we wait for CLJS-in-CLJS to become mainstream, which could take years if it really ever
      happens

   we decide instead to evaluate types in languages in which the metalanguage (compiler language)
   is the same as the object language (e.g. Clojure), and symbolically analyze types in the rest
   (e.g. vanilla ClojureScript), deferring code analyzed as functions to be enforced at runtime."
  [{:as in {:keys [args _, varargs _]
            pre-type|form [:pre _]
            [_ _, post-type|form _] [:post _]} [:arglist _]
            body-codelist|pre-analyze [:body _]} _
   {:as opts :keys [::lang ::lang, symbolic-analysis? t/boolean?]} _
   > ::expanded-overload-groups]
  (if symbolic-analysis?
      (err! "Symbolic analysis not supported yet")
      (let [_ (when pre-type|form (TODO "Need to handle pre"))
            _ (when varargs (TODO "Need to handle varargs"))
            post-type|form (if (= post-type|form '_) `t/any? post-type|form)
            varargs-binding (when varargs
                              ;; TODO this assertion is purely temporary until destructuring is
                              ;; supported
                              (assert (-> varargs :binding-form first (= :sym))))
            arg-bindings
              (->> args
                   (mapv (fn [{[kind binding-] :binding-form}]
                           ;; TODO this assertion is purely temporary until destructuring is
                           ;; supported
                           (assert kind :sym)
                           binding-)))
            arg-types|pre-split|form
              (->> args
                   (mapv (fn [{[kind #_#{:any :spec}, t #_t/form?] :spec}]
                           (case kind :any `t/any? :spec t))))
            arg-types|pre-split (->> arg-types|pre-split|form (mapv (fn-> eval t/>type)))
            arg-types|split
              ;; NOTE Only `t/or`s are splittable for now
              (->> arg-types|pre-split
                   (c/map (fn [t] (if (utr/or-type? t) (utr/or-type>args t) [t]))))
            arg-types|recombined (->> arg-types|split
                                      (apply combo/cartesian-product)
                                      (c/map vec))
            expanded-overload-group-seq
              (->> arg-types|recombined
                   (mapv (fn [arg-types]
                           (>expanded-overload-group
                             (kw-map arg-bindings arg-types body-codelist|pre-analyze lang
                                     arg-types|pre-split|form pre-type|form post-type|form
                                     varargs varargs-binding)))))]
        (kw-map arg-types|pre-split|form pre-type|form post-type|form
                arg-types|split arg-types|recombined
                expanded-overload-group-seq)))))

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

;; TODO finish specing args
(defns fnt-overload>interface [args-classes _, out-class t/class?, gen-gensym fn?]
  (let [interface-sym     (fnt-overload>interface-sym args-classes out-class)
        hinted-method-sym (ufth/with-type-hint fnt-method-sym
                            (ufth/>interface-method-tag out-class))
        hinted-args       (ufth/hint-arglist-with
                            (ufgen/gen-args 0 (count args-classes) "xint" gen-gensym)
                            (map ufth/>interface-method-tag args-classes))]
    `(~'definterface ~interface-sym (~hinted-method-sym ~hinted-args))))

;; TODO spec args
#?(:clj
(defns expanded-overload>reify-overload
  [{:as overload
    :keys [arg-classes _, arglist-code|reify|unhinted _, body-form _, out-class t/class?]}
   ::expanded-overload
   gen-gensym fn?
   > (s/seq-of ::reify|overload)]
  (let [interface-k {:out out-class :in arg-classes}
        interface
          (-> *interfaces
              (swap! update interface-k
                #(or % (eval (fnt-overload>interface arg-classes out-class gen-gensym))))
              (c/get interface-k))
        arglist-code
          (>vec (concat [(gen-gensym '_)]
                  (->> arglist-code|reify|unhinted
                       (map-indexed
                         (fn [i arg]
                           (ufth/with-type-hint arg
                             (-> arg-classes (c/get i) ufth/>arglist-embeddable-tag)))))))]
    {:arglist-code arglist-code
     :body-form    body-form
     :interface    interface
     :method-sym   fnt-method-sym
     :out-class    out-class})))

(defns >reify|name
  [{:keys [::uss/fn|name ::uss/fn|name, i|fnt-overload t/index?
           i|expanded-overload-group t/index?]} _ > simple-symbol?]
  (>symbol (str fn|name "|__" i|fnt-overload "|" i|expanded-overload-group)))

#?(:clj
(defns expanded-overload-group>reify
  [{:as   in
    :keys [::uss/fn|name ::uss/fn|name, i|fnt-overload t/index?, i|expanded-overload-group t/index?
           expanded-overload-group ::expanded-overload-group]} _
   gen-gensym fn? > ::reify]
  (let [reify-overloads (->> (concat [(:non-primitivized expanded-overload-group)]
                                     (:primitivized expanded-overload-group))
                             (c/map #(expanded-overload>reify-overload % gen-gensym)))
        reify-name (>reify|name in)
        form `(~'def ~reify-name
                ~(list* `reify*
                   (->> reify-overloads (mapv #(-> % :interface >name >symbol)))
                   (->> reify-overloads
                        (c/lmap (fn [{:keys [out-class method-sym arglist-code
                                             body-form]} #_::reify|overload]
                                  `(~(ufth/with-type-hint method-sym
                                       (ufth/>arglist-embeddable-tag out-class))
                                    ~arglist-code ~body-form))))))]
    {:form                      form
     :name                      reify-name
     :non-primitivized-overload (first reify-overloads)
     :overloads                 reify-overloads})))

(defns >input-type-decl|name
  [fn|name ::uss/fn|name, i|fnt-overload t/index?, i|arg t/index? > simple-symbol?]
  (>symbol (str fn|name "|__" i|fnt-overload "|input" i|arg "|types")))

(defns >i-arg->input-types-decl
  "The evaluated `form` of each input-types-decl is an array of non-primitivized types that the
   dynamic dispatch uses to dispatch off input types."
  [{:as   in
    :keys [arg-types|split ::expanded-overload-groups|arg-types|split
           fn|name         ::uss/fn|name
           i|fnt-overload  t/index?]} _
   > (s/vec-of ::input-types-decl)]
  (->> arg-types|split
       (c/map-indexed
         (fn [i|arg arg-type|split]
           (let [decl-name (>input-type-decl|name fn|name i|fnt-overload i|arg)
                 form      (list 'def (ufth/with-type-hint decl-name "[Ljava.lang.Object;")
                                      (list* `arr/*<> (map >form arg-type|split)))]
             (assoc (kw-map form arg-type|split) :name decl-name))))))

(def allowed-shorthand-tag-chars "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ")
(def min-shorthand-tag-length 1)
(def max-shorthand-tag-length 64) ; for now

(defn >all-shorthand-tags []
  (->> (range min-shorthand-tag-length (inc max-shorthand-tag-length))
       c/unchunk
       (c/lmap (fn [n] (apply combo/cartesian-product (repeat n allowed-shorthand-tag-chars))))
       lcat
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

;; TODO spec
(defn assert-monotonically-increasing-types!
  "Asserts that each type in an overload of the same arity and arg-position
   are in monotonically increasing order in terms of `t/compare`."
  [overloads|grouped-by-arity]
  (doseq [[arity-ct overloads] overloads|grouped-by-arity]
    (educe
      (fn [prev-overload [i|overload overload]]
        (when prev-overload
          (reduce-2
            (fn [_ arg|type|prev [i|arg arg|type]]
              (when (= (t/compare arg|type arg|type|prev) -1)
                ;; TODO provide code context, line number, etc.
                (err! (istr "At overload ~{i|overload}, arg ~{i|arg}: type is not in monotonically increasing order in terms of `t/compare`")
                      {:overload      overload
                       :prev-overload prev-overload
                       :prev-type     arg|type|prev
                       :type          arg|type})))
            (:arg-types prev-overload)
            (c/lindexed (:arg-types overload))))
        overload)
      nil
      overloads)))

;; TODO spec
(defns unsupported! [name- _ #_t/qualified-symbol?, args t/indexed?, i t/index?]
  (TODO))

(defns >direct-dispatch
  [{:keys [::uss/fn|name                            ::uss/fn|name
           expanded-overload-groups-by-fnt-overload (s/vec-of ::expanded-overload-groups)
           gen-gensym                               fn?
           lang                                     ::lang]} _
   > ::direct-dispatch]
  (case lang
    :clj
      (let [i-overload->direct-dispatch-data
              (->> expanded-overload-groups-by-fnt-overload
                   (c/map-indexed
                     (fn [i|fnt-overload {:keys [arg-types|split expanded-overload-group-seq]}]
                       {:i-arg->input-types-decl
                          (>i-arg->input-types-decl (kw-map arg-types|split fn|name i|fnt-overload))
                        :reify-seq
                          (->> expanded-overload-group-seq
                               (c/map-indexed
                                 (fn [i|expanded-overload-group
                                      {:as expanded-overload-group :keys [arg-types|form]}]
                                   (let [in (assoc (kw-map i|fnt-overload
                                                           i|expanded-overload-group
                                                           expanded-overload-group)
                                              ::uss/fn|name fn|name)]
                                     (expanded-overload-group>reify in gen-gensym)))))})))
            form (->> i-overload->direct-dispatch-data
                      (c/map (fn [{:keys [i-arg->input-types-decl reify-seq]}]
                              (concat (c/lmap :form i-arg->input-types-decl)
                                      (c/lmap :form reify-seq))))
                      c/lcat)]
        (kw-map form i-overload->direct-dispatch-data))
    :cljs (TODO)))

(defns >dynamic-dispatch-fn|type-decl
  [expanded-overload-groups-by-fnt-overload (s/vec-of ::expanded-overload-groups)]
  (list* `t/fn (->> expanded-overload-groups-by-fnt-overload
                    (map (fn [{:keys [arg-types|pre-split|form
                                      pre-type|form post-type|form]}]
                           (cond-> (or arg-types|pre-split|form [])
                             pre-type|form  (conj :| pre-type|form)
                             post-type|form (conj :> post-type|form)))))))

(defns >dynamic-dispatch|reify-call [reify- ::reify, arglist (s/vec-of simple-symbol?)]
  (let [dotted-reify-method-sym
          (symbol (str "." (-> reify- :non-primitivized-overload :method-sym)))
        hinted-reify-sym
          (ufth/with-type-hint (:name reify-)
            (-> reify- :non-primitivized-overload :interface >name))]
    `(~dotted-reify-method-sym ~hinted-reify-sym ~@arglist)))

(defns >dynamic-dispatch|conditional
  [fn|name ::uss/fn|name, arglist (s/vec-of simple-symbol?), i|arg t/index?, body _]
  (if (-> body count (= 1))
      (first body)
      `(ifs ~@body (unsupported! (quote ~(qualify fn|name)) [~@arglist] ~i|arg))))

(defns >dynamic-dispatch|body-for-arity
  ([fn|name ::uss/fn|name, arglist (s/vec-of simple-symbol?)
    direct-dispatch-data-for-arity (s/seq-of ::direct-dispatch-data)]
    (if (empty? arglist)
        (>dynamic-dispatch|reify-call
          (-> direct-dispatch-data-for-arity first :reify-seq first) arglist)
        (let [i|arg    0
              branches (->> direct-dispatch-data-for-arity
                            (c/lmap
                              (fn [{:keys [reify-seq i-arg->input-types-decl]}]
                                (>dynamic-dispatch|body-for-arity fn|name arglist reify-seq
                                  i-arg->input-types-decl i|arg 0)))
                            c/lcat)]
          (>dynamic-dispatch|conditional fn|name arglist i|arg branches))))
  ([fn|name ::uss/fn|name, arglist (s/vec-of simple-symbol?), reify-seq (s/vec-of ::reify)
    input-types-decl-group' (s/seq-of ::input-types-decl), i|arg t/index?, i|arg-type t/index?]
    (let [{:as input-types-decl :keys [arg-type|split]} (first input-types-decl-group')
          input-types-decl-group'' (rest input-types-decl-group')]
      (if (empty? input-types-decl-group'')
          (let [i|reify i|arg-type]
            [(>dynamic-dispatch|reify-call (get reify-seq i|reify) arglist)])
          (->> arg-type|split
               (c/lmap-indexed
                 (fn [i|arg-type' _]
                   [`((Array/get ~(:name input-types-decl) ~i|arg-type') ~@arglist)
                    (let [next-branch (>dynamic-dispatch|body-for-arity fn|name arglist reify-seq
                                        input-types-decl-group'' (inc i|arg) i|arg-type')]
                      (>dynamic-dispatch|conditional fn|name arglist i|arg next-branch))]))
               c/lcat)))))

(defns >dynamic-dispatch-fn|form
  [{:keys [::uss/fn|name                            ::uss/fn|name
           expanded-overload-groups-by-fnt-overload (s/vec-of ::expanded-overload-groups)
           gen-gensym                               fn?
           lang                                     ::lang
           i-overload->direct-dispatch-data         ::i-overload->direct-dispatch-data]} _]
 `(defn ~fn|name
    {::t/type ~(>dynamic-dispatch-fn|type-decl expanded-overload-groups-by-fnt-overload)}
    ~@(->> i-overload->direct-dispatch-data
           (group-by (fn-> :i-arg->input-types-decl count))
           (map (fn [[arg-ct direct-dispatch-data-for-arity]]
                  (let [arglist (ufgen/gen-args 0 arg-ct "x" gen-gensym)
                        body    (>dynamic-dispatch|body-for-arity
                                  fn|name arglist direct-dispatch-data-for-arity)]
                    (list arglist body)))))))

(defns fnt|code [kind #{:fn :defn}, lang ::lang, args _]
  (let [{:keys [:quantum.core.specs/fn|name
                :quantum.core.defnt/overloads
                :quantum.core.specs/meta] :as args'}
          (s/validate args (case kind :defn ::defnt :fn ::fnt))
        symbolic-analysis? false ; TODO parameterize this
        gen-gensym-base (ufgen/>reproducible-gensym|generator)
        gen-gensym (fn [x] (symbol (str (gen-gensym-base x) "__")))
        inline? (s/validate (-> fn|name core/meta :inline) (t/? t/boolean?))
        fn|name (if inline?
                    (do (log/pr :warn "requested `:inline`; ignoring until feature is implemented")
                        (update-meta fn|name dissoc :inline))
                    fn|name)
        expanded-overload-groups-by-fnt-overload
          (->> overloads (mapv #(fnt|overload-data>expanded-overload-groups %
                                  {::lang lang :symbolic-analysis? symbolic-analysis?})))
        args (assoc (kw-map expanded-overload-groups-by-fnt-overload gen-gensym lang)
                    ::uss/fn|name fn|name)
        {:as direct-dispatch :keys [i-overload->direct-dispatch-data]} (>direct-dispatch args)
        fn-codelist
          (case lang
            :clj  (->> `[~@(:form direct-dispatch)
                         ~(>dynamic-dispatch-fn|form
                            (merge args (kw-map i-overload->direct-dispatch-data)))]
                        (remove nil?))
            :cljs (TODO))
        code (case kind
               :fn   (TODO)
               :defn `(~'do ~@fn-codelist))]
    code))

#?(:clj (defmacro fnt   [& args] (fnt|code :fn   (ufeval/env-lang) args)))
#?(:clj (defmacro defnt [& args] (fnt|code :defn (ufeval/env-lang) args)))
