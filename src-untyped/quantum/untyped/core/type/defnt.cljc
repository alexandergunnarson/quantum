(ns quantum.untyped.core.type.defnt
  (:refer-clojure :exclude
    [defn fn])
  (:require
    [clojure.core                               :as c]
    [clojure.string                             :as str]
    ;; TODO excise this reference
    [quantum.core.type.core                     :as tcore]
    ;; TODO excise this reference
    [quantum.core.type.defs                     :as tdef]
    [quantum.untyped.core.analyze               :as uana]
    [quantum.untyped.core.analyze.ast           :as uast]
    [quantum.untyped.core.core
      :refer [istr sentinel]] ; TODO use quantum.untyped.core.string/istr instead
    [quantum.untyped.core.defnt
      :refer [defns defns- fns]]
    [quantum.untyped.core.collections           :as uc
      :refer [>set >vec]]
    [quantum.untyped.core.collections.logic
      :refer [seq-or]]
    [quantum.untyped.core.compare               :as ucomp
      :refer [not==]]
    [quantum.untyped.core.data
      :refer [kw-map]]
    [quantum.untyped.core.data.array            :as uarr]
    [quantum.untyped.core.data.map              :as umap]
    [quantum.untyped.core.data.reactive         :as urx
      :refer [?norx-deref norx-deref]]
    [quantum.untyped.core.data.set              :as uset]
    [quantum.untyped.core.data.vector           :as uvec
      :refer [alist-conj!]]
    [quantum.untyped.core.error                 :as uerr
      :refer [TODO err!]]
    [quantum.untyped.core.fn
      :refer [<- aritoid fn1 fn-> with-do with-do-let]]
    [quantum.untyped.core.form                  :as uform
      :refer [>form]]
    [quantum.untyped.core.form.evaluate         :as ufeval]
    [quantum.untyped.core.form.generate         :as ufgen]
    [quantum.untyped.core.form.type-hint        :as ufth]
    [quantum.untyped.core.identifiers           :as uid
      :refer [>name >?namespace >symbol]]
    [quantum.untyped.core.log                   :as ulog]
    [quantum.untyped.core.logic                 :as ul
      :refer [fn-or fn= if-not-let ifs ifs-let]]
    [quantum.untyped.core.loops
      :refer [reduce-2]]
    [quantum.untyped.core.reducers              :as ur
      :refer [educe educei reducei]]
    [quantum.untyped.core.refs                  :as uref
      :refer [?deref]]
    [quantum.untyped.core.spec                  :as us]
    [quantum.untyped.core.specs                 :as uss]
    [quantum.untyped.core.type                  :as t
      :refer [?]]
    [quantum.untyped.core.type.compare          :as utcomp]
    [quantum.untyped.core.type.reifications     :as utr]
    [quantum.untyped.core.vars                  :as uvar
      :refer [update-meta]])
  (:import
    [quantum.core Numeric]
    [quantum.core.data Array]))

;; TODO move
(def index? #(and (integer? %) (>= % 0)))
(def count? index?)

;; ===== `t/extend-defn!` specs ===== ;;

(us/def :quantum.core.defnt/fn|extended-name symbol?)

(us/def :quantum.core.defnt/extend-defn!
  (us/and (us/spec
            (us/cat :quantum.core.defnt/fn|extended-name :quantum.core.defnt/fn|extended-name
                   :quantum.core.defnt/overloads        :quantum.core.defnt/overloads))
          (uss/fn-like|postchecks|gen :quantum.core.defnt/overloads)
          :quantum.core.defnt/postchecks))

;; ===== End `t/extend-defn!` specs ===== ;;

(defonce *fn->type (atom {}))

(defonce defnt-cache (atom {})) ; TODO For now — but maybe lock-free concurrent hash map to come

(defonce *interfaces (atom {}))

(defonce !overload-queue (uvec/alist)) ; (t/!seq-of ::types-decl-datum-with-overload)

(uvar/defonce !rollback-queue
  "To ensure that side-effects are as atomic as possible in the case of a failure in `defn` or
   `extend-defn!`."
  (uvec/alist)) ; (t/!seq-of (t/ftype []))

(defns- intern-with-rollback! [ns-sym simple-symbol?, sym simple-symbol?, v _]
  (let [var-val (resolve (uid/qualify ns-sym sym))
        !value  (atom nil)]
    (when var-val (reset! !value (var-get var-val)))
    (intern ns-sym sym v)
    (alist-conj! !rollback-queue
      #(if var-val
           (intern ns-sym sym @!value)
           (uvar/unintern! ns-sym sym)))))

(defn- drain-rollback-queue!
  "Rolls back already-executed effects in reverse order."
  []
  (->> !rollback-queue
       reverse
       (uc/run!
         (c/fn [rollback-fn]
           (uerr/catch-all (rollback-fn)
             rollback-err
             (err! nil "Unable to roll back all effects" {:failed-rollback-fn rollback-fn}
                   nil rollback-err))))))

;; ==== Internal specs ===== ;;

(us/def ::lang #{:clj :cljs})

(def ^:dynamic *compilation-mode* :normal)

(us/def ::compilation-mode #{:normal :test})

(us/def ::kind #{:fn :defn :extend-defn!})

(us/def ::opts
  (us/kv {:compilation-mode ::compilation-mode
          :gen-gensym       t/fn?
          :lang             ::lang
          :kind             ::kind}))

;; "global" because they apply to the whole `t/fn`
(us/def ::fn|globals
  (us/kv {:fn|globals-name        simple-symbol?
          :fn|inline?             boolean?
          :fn|meta                (us/nilable :quantum.core.specs/meta)
          :fn|ns-name             simple-symbol?
          :fn|name                ::uss/fn|name
          :fn|output-type         t/type?
          :fn|output-type|form    t/any?
          :fn|overload-bases-name simple-symbol?
          :fn|overload-types-name simple-symbol?
          :fn|type-name           simple-symbol?}))

(us/def ::overload-basis|types|split
  (us/vec-of (us/kv {:arg-types (us/vec-of t/type?) :output-type t/type?})))

(us/def ::overload-basis|norx
  ;; None of these types should be reactive
  (us/kv {:arg-types|basis   (us/vec-of t/type?)
          :output-type|basis t/type?
          ;; This is non-nil only for arglists with dependent types
          :types|split       (us/nilable ::overload-basis|types|split)
          :body-codelist     (us/vec-of t/any?)
          :dependent?        boolean?
          :reactive?         boolean?
          :inline?           boolean?}))

(us/def ::overload-basis
  (us/kv {:ns-name                 simple-symbol?
          :args-form               map? ; from binding to form
          :varargs-form            (us/nilable map?) ; from binding to form
          :arglist-form|unanalyzed t/any?
          :arg-types|basis         (us/vec-of t/type?)
          :output-type|form        t/any?
          :output-type|basis       t/type?
          ;; This is non-nil only for arglists with dependent types
          :types|split             (us/nilable ::overload-basis|types|split)
          :body-codelist           (us/vec-of t/any?)
          :dependent?              boolean?
          :reactive?               boolean?
          :inline?                 boolean?}))

(us/def ::overload-bases-data
  (us/kv {:prev-norx (us/nilable (us/vec-of ::overload-basis|norx))
          :current   (us/vec-of ::overload-basis)}))

 ;; Technically it's partially analyzed — its type definitions are analyzed (with the exception of
 ;; requests for type inference) while its body is not.
 (us/def ::unanalyzed-overload
   (us/kv {:ns-name                     simple-symbol?
           :arg-classes                 (us/vec-of class?)
           :arg-types                   (us/vec-of t/type?)
           :arglist-code|hinted         (us/vec-of simple-symbol?)
           :arglist-code|reify|unhinted (us/vec-of simple-symbol?)
           :arglist-form|unanalyzed     t/any?
           :args-form                   map? ; from binding to form
           :varargs-vorm                (us/nilable map?) ; from binding to form
           :output-type|form            t/any?
           :output-type                 t/type?
           :pre-type                    (us/nilable t/type?)
           :body-codelist               (us/vec-of t/any?)
           :i|basis                     index?
           :inline?                     boolean?}))

;; This is the overload after the input specs are split by their respective `t/or` constituents,
;; and after primitivization, but before readiness for incorporation into a `reify`.
;; One of these corresponds to one reify overload.
(us/def ::overload
  (us/kv {:arg-classes                 (us/vec-of class?)
          :arg-types                   (us/vec-of t/type?)
          :arglist-form|unanalyzed     t/any?
          :arglist-code|fn|hinted      (us/vec-of simple-symbol?)
          :arglist-code|hinted         (us/vec-of simple-symbol?)
          :arglist-code|reify|unhinted (us/vec-of simple-symbol?)
          :body-form                   t/any?
          :output-class                (us/nilable class?)
          :output-type                 t/type?
          :positional-args-ct          count?
          ;; When present, varargs are considered to be of class Object
          :variadic?                   t/boolean?}))

(us/def ::overload|id index?)

(us/def ::overload-types-decl
  (us/kv {:form t/any?
          :name simple-symbol?}))

(us/def ::reify|name simple-symbol?) ; hinted with the interface name

(us/def ::reify
  (us/kv {:form      t/any?
          :interface class?
          :name      ::reify|name
          :overload  ::overload}))

(us/def ::direct-dispatch-data
  (us/kv {:overload-types-decl ::overload-types-decl
          :reify               ::reify}))

(us/def ::direct-dispatch
  (us/kv {:form                     t/any?
          :direct-dispatch-data-seq (us/vec-of ::direct-dispatch-data)}))

(us/def ::type-datum
  (us/kv {:arg-types   (us/vec-of t/type?)
          :pre-type    (us/nilable t/type?)
          :output-type t/type?}))

(us/def ::types-decl-datum
  (us/kv {:id                  ::overload|id
          :index               index? ; overload-index (position in the overall types-decl)
          :ns-name             simple-symbol?
          :arglist-code|hinted (us/vec-of simple-symbol?)
          :arg-types           (us/vec-of t/type?)
          :output-type         t/type?
          :body-codelist       (us/vec-of t/any?)
          :inline?             boolean?}))

(us/def ::fn|types
  (us/kv {:fn|output-type-norx t/type?
          :fn|type-norx        t/type?
          :overload-types      (us/vec-of ::types-decl-datum)}))

#_(:clj
(c/defn fnt|arg->class [lang {:as arg [k spec] ::fnt|arg-spec :keys [arg-binding]}]
  (cond (not= k :spec) java.lang.Object; default class
        (symbol? spec) (pred->class lang spec))))

(c/defn >with-runtime-output-type [body output-type|form] `(t/validate ~body ~output-type|form))

;; TODO simplify this class computation

;; ===== Arg type/class extraction/comparison ===== ;;

#?(:clj
(defns class>simplest-class
  "This ensures that special overloads are not created for non-primitive subclasses
   of java.lang.Object (e.g. String, etc.)."
  [c class? > class?]
  (if (t/primitive-class? c) c java.lang.Object)))

#?(:clj
(defns type>class
  "Converts type to class after type has gone through the split+primitivization process."
  [t t/type? > class?]
  (let [cs  (t/type>classes t)
        cs' (disj cs nil)]
    (if (-> cs' count (not= 1))
        java.lang.Object
        (-> (first cs')
            (cond-> (and (not (contains? cs nil))
                         (not (-> t meta :quantum.core.type/ref?)))
              t/class>most-primitive-class))))))

(defns- with-validate-output-type [declared-output-type t/type?, body-node uast/node? > t/type?]
  (let [err-info {:form                 (:form body-node)
                  :type                 (:type body-node)
                  :declared-output-type declared-output-type}]
    (case (t/compare (:type body-node) declared-output-type)
      (-1 0) declared-output-type
      1      (if (or (-> declared-output-type meta :quantum.core.type/runtime?)
                     (-> declared-output-type meta :quantum.core.type/assume?))
                 declared-output-type
                 (err! "Body type incompatible with declared output type" err-info))
      (2 3)  (err! "Body type incompatible with declared output type" err-info))))

(c/defn compare-arg-types [t0 #_t/type?, t1 #_t/type? #_> #_ucomp/comparison?]
  (if-let [c0 (uana/sort-guide t0)]
    (if-let [c1 (uana/sort-guide t1)]
      (ifs (< c0 c1) -1 (> c0 c1) 1 0)
      (uset/normalize-comparison (t/compare t0 t1)))
    (uset/normalize-comparison (t/compare t0 t1))))

(c/defn compare-args-types [arg-types0 #_(us/vec-of t/type?) arg-types1 #_(us/vec-of t/type?)]
  (let [ct-comparison (compare (count arg-types0) (count arg-types1))]
    (if (zero? ct-comparison)
        (reduce-2
          (c/fn [^long c t0 t1]
            (let [c' (long (compare-arg-types t0 t1))]
              (if (zero? c') c' (reduced c'))))
          0
          arg-types0 arg-types1)
        ct-comparison)))

(c/defn sort-overload-types
  "A naïve implementation would do an aggregate compare on the arg-types vectors, but the resulting
   comparator would not be transitive due to the behavior of `<>` and `><`. For example, for the
   below arg-types vectors, x0 comp< x1, x1 comp< x2, but x0 not comp< x2:
   - x0: [t/boolean?                  t/nil?]
   - x1: [(t/ref (t/isa? Comparable)) t/byte?]
   - x2: [t/nil?                      t/val?]

   Because of this, we are forced to do as many sorts as the max arity of the typed fn, which
   results in an O(m•n•log(n))) algorithm, where `m` is the max arity and `n` is the number of
   overloads."
  [kf overload-types]
  (let [!overload-types (to-array overload-types)
        max-arity (->> !overload-types (uc/map+ count) (educe (aritoid (c/fn [] 0) max max)))]
    (dotimes [i max-arity]
      (->> !overload-types
           (uc/sort-by! kf
             (c/fn [a b] (let [ct|a          (count a)
                               ct|b          (count b)
                               ct-comparison (compare ct|a ct|b)]
                           (if (zero? ct-comparison)
                               (if (< i ct|a)
                                   (compare-arg-types (get a i) (get b i))
                                   0)
                               ct-comparison))))))
    (>vec !overload-types)))

(c/defn- dedupe-type-data
  "Performs both structural and `t/compare` deduplication."
  [on-dupe #_fn?, type-data #_(vec-of ::types-decl-datum)]
  (reduce (let [!prev-datum  (volatile! nil)
                !unique-data (transient #{})]
            (c/fn [data {:as datum :keys [arg-types]}]
              (with-do
                (ifs (nil? @!prev-datum)
                       (conj data datum)
                     (or (contains? !unique-data datum)
                         (= uset/=ident
                            (utcomp/compare-inputs (:arg-types @!prev-datum) arg-types)))
                       (on-dupe data @!prev-datum datum)
                     (conj data datum))
                (conj! !unique-data datum)
                (vreset! !prev-datum datum))))
          []
          type-data))

;; ===== Unanalyzed overloads ===== ;;

#?(:clj
(defns- unanalyzed-overload>overload
  "Given an `::unanalyzed-overload`, performs type analysis on the body and computes a resulting
   `t/fn` overload, which is the foundation for one `reify`."
  [{:as opts       :keys [lang _, kind _]} ::opts
   {:as fn|globals :keys [fn|globals-name _, fn|name _, fn|ns-name _, fn|output-type _
                          fn|overload-types-name _]} ::fn|globals
   {:as unanalyzed-overload
    :keys [arg-classes _, arg-types _, arglist-code|hinted _, arglist-code|reify|unhinted _,
           arglist-form|unanalyzed _, args-form _, body-codelist _ output-type|form _
           varargs-form _, variadic? _]
    declared-output-type [:output-type _]} ::unanalyzed-overload
   overload|id       index?
   fn|overload-types (us/vec-of ::types-decl-datum)
   fn|type           t/type?
   > ::overload]
  (let [;; Not sure if `nil` is the right approach for the value
        recursive-ast-node-reference
          (when-not (= kind :extend-defn!) (uast/symbol {} fn|name nil fn|type))
        env          (->> (zipmap (keys args-form) arg-types)
                          (uc/map' (c/fn [[arg-binding arg-type]]
                                     [arg-binding (uast/unbound nil arg-binding arg-type)]))
                          ;; To support recursion
                          (<- (cond-> (not= kind :extend-defn!)
                                (assoc fn|name
                                       recursive-ast-node-reference
                                       (uid/qualify fn|ns-name fn|overload-types-name)
                                       fn|overload-types))))
        body-node    (uana/analyze
                       (assoc env :opts {:ns (-> unanalyzed-overload :ns-name the-ns)})
                       (ufgen/?wrap-do body-codelist))
        hint-arg|fn  (c/fn [i arg-binding]
                       (ufth/with-type-hint arg-binding
                         (ufth/>fn-arglist-tag
                           (uc/get arg-classes i)
                           lang
                           (uc/count args-form)
                           variadic?)))
        output-type  (with-validate-output-type declared-output-type body-node)
        output-class (type>class output-type)
        body-form
          (-> (:form body-node)
              (cond-> (-> output-type meta :quantum.core.type/runtime?)
                ;; TODO here the output type is being re-created each time (unless the fn's overall
                ;;      output type is being preferred) because it could reference inputs, but we
                ;;      should probably analyze to determine whether it references inputs so we can,
                ;;      in the 90% case, extern the output type
                (>with-runtime-output-type
                  (or output-type|form
                      `(?norx-deref (:fn|output-type ~(uid/qualify fn|ns-name fn|globals-name)))))))
        positional-args-ct (count args-form)
        arglist-code|fn|hinted
          (cond-> (->> args-form keys (uc/map-indexed hint-arg|fn))
            variadic? (conj '& (-> varargs-form keys first)))]
      (kw-map arglist-form|unanalyzed arg-classes arg-types arglist-code|fn|hinted
              arglist-code|reify|unhinted arglist-code|hinted body-form positional-args-ct
              output-type output-class variadic?))))

(defns- class>interface-part-name [c class? > string?]
  (if (= c java.lang.Object)
      "Object"
      (let [illegal-pattern #"\|\+"]
        (if (->> c >name (re-find illegal-pattern))
            (err! "Class cannot contain pattern" {:class c :pattern illegal-pattern})
            (-> c >name (str/replace "." "|"))))))

;; ===== Direct dispatch ===== ;;

;; ----- Direct dispatch: `reify` ---- ;;

(defns- overload-classes>interface-sym [args-classes (us/seq-of class?), out-class class? > symbol?]
  (>symbol (str (->> args-classes (uc/lmap class>interface-part-name) (str/join "+"))
                ">" (class>interface-part-name out-class))))

(defns- overload-classes>interface
  [args-classes (us/vec-of class?), out-class class?, gen-gensym fn?]
  (let [interface-sym     (overload-classes>interface-sym args-classes out-class)
        hinted-method-sym (ufth/with-type-hint uana/direct-dispatch-method-sym
                            (ufth/>interface-method-tag out-class))
        hinted-args       (ufth/hint-arglist-with
                            (ufgen/gen-args 0 (count args-classes) "x" gen-gensym)
                            (map ufth/>interface-method-tag args-classes))]
    `(~'definterface ~interface-sym (~hinted-method-sym ~hinted-args))))

(defns- >reify-name-unhinted
  ([fn|name simple-symbol?, overload|id ::overload|id > simple-symbol?]
    (symbol (str fn|name "|__" overload|id)))
  ([fn|ns-name simple-symbol?, fn|name simple-symbol?, overload|id ::overload|id
    > qualified-symbol?]
    (symbol (name fn|ns-name) (str fn|name "|__" overload|id))))

#?(:clj
(defns overload>reify
  [{:as   overload
    :keys [arg-classes _, arglist-code|reify|unhinted _, body-form _, output-class _]} ::overload
   {:as opts :keys [gen-gensym _]} ::opts
   {:keys [fn|name _]} ::fn|globals
   overload|id ::overload|id
   > ::reify]
  (let [arg-classes|reify (->> arg-classes (uc/map class>simplest-class))
        output-class|reify (class>simplest-class output-class)
        interface-k {:out output-class|reify :in arg-classes|reify}
        interface
          (-> *interfaces
              (swap! update interface-k
                #(or % (eval (overload-classes>interface arg-classes|reify output-class|reify
                               gen-gensym))))
              (uc/get interface-k))
        arglist-code
          (ur/join [(gen-gensym '_)]
            (->> arglist-code|reify|unhinted
                 (uc/map-indexed
                   (c/fn [i|arg arg|form]
                     (ufth/with-type-hint arg|form
                       (-> arg-classes|reify (uc/get i|arg) ufth/>arglist-embeddable-tag))))))
        reify|name (-> (>reify-name-unhinted fn|name overload|id)
                       (ufth/with-type-hint (>name interface)))
        form `(~'def ~reify|name
                (reify* [~(-> interface >name >symbol)]
                  (~(ufth/with-type-hint uana/direct-dispatch-method-sym
                      (ufth/>arglist-embeddable-tag output-class|reify))
                    ~arglist-code ~body-form)))]
    {:form      form
     :interface interface
     :name      reify|name
     :overload  overload})))

;; ----- Type declarations ----- ;;

(c/defn overload-types>arg-types
  [?!fn|types #_(t/or ::fn|types (t/of urx/reactive? ::fn|types)), overload-index #_index?
   #_> #_(objects-of type?)]
  (apply uarr/*<> (-> ?!fn|types ?norx-deref :overload-types (get overload-index) :arg-types)))

(c/defn overload-types>ftype
  [fn|ns-name     #_simple-symbol?
   fn|name        #_simple-symbol?
   overload-types #_(vec-of ::type-datum)
   fn|output-type #_t/type?]
  (->> overload-types
       (uc/lmap (c/fn [{:keys [arg-types pre-type output-type]}]
                  (cond-> arg-types
                    pre-type    (conj :| pre-type)
                    output-type (conj :> output-type))))
       (apply t/ftype (uid/qualify fn|ns-name fn|name) fn|output-type)))

(c/defn- dedupe-overload-types-data [fn|ns-name fn|name types-decl-data]
  (->> types-decl-data
       (dedupe-type-data
         (c/fn [data prev-datum datum]
           (ulog/ppr :warn
             (str "Overwriting type overload for `" (uid/qualify fn|ns-name fn|name) "`")
             {:arg-types-prev (:arg-types prev-datum) :arg-types (:arg-types datum)})
           (-> data pop
               (conj (assoc datum :id           (:id          prev-datum)
                                  :arg-types    (:arg-types   prev-datum)
                                  :output-type  (:output-type prev-datum)
                                  :replacing-id (:id          datum))))))))

(defns- >overload-types-decl|name
  ([fn|name simple-symbol?, overload|id ::overload|id > simple-symbol?]
    (symbol (str fn|name "|__" overload|id "|types")))
  ([fn|ns-name simple-symbol?, fn|name simple-symbol?, overload|id ::overload|id
    > qualified-symbol?]
    (symbol (name fn|ns-name) (str fn|name "|__" overload|id "|types"))))

(defns- >overload-types-decl
  "The evaluated `form` of each overload-types-decl is an array of non-primitivized types that the
   dynamic dispatch uses to dispatch off input types."
  [{:as opts :keys [compilation-mode _, lang _]} ::opts
   {:as fn|globals :keys [fn|ns-name _, fn|name _, fn|overload-types-name _]} ::fn|globals
   {:as types-decl-datum :keys [id _, index _] ns-name- [:ns-name _]} ::types-decl-datum
   fn|types ::fn|types
   > ::overload-types-decl]
  (let [decl-name (-> (>overload-types-decl|name fn|name id)
                      (ufth/with-type-hint "[Ljava.lang.Object;"))
        form      (if (or (not= compilation-mode :test) (= lang :clj))
                      (let [arg-types (overload-types>arg-types fn|types index)]
                        (do (intern-with-rollback! ns-name- decl-name arg-types)
                            nil))
                      `(def ~decl-name
                         (overload-types>arg-types
                           ~(uid/qualify fn|ns-name fn|overload-types-name) ~index)))]
    {:form form :name decl-name}))

(defns- overload-basis-data>types+
  "Split and primitivized; not yet sorted."
  [{:keys [fn|output-type _]} ::fn|globals, ns-name-val _, args-form _, output-type|form _
   body-codelist _]
  (->> (uana/analyze-arg-syms {:opts {:ns (the-ns ns-name-val)}}
          args-form (or output-type|form fn|output-type) true)
       (uc/map+ (c/fn [{:keys [env output-type-node]}]
                  (let [arg-env     (->> env :opts :arg-env deref)
                        arg-types   (->> args-form keys (uc/map #(:type (get arg-env %))))
                        output-type (:type output-type-node)
                        pre-type    nil] ; TODO fix
                    (when-not (t/<= output-type fn|output-type)
                      (err! (str "Overload's declared output type does not satisfy function's"
                                 "overall declared output type")
                            (kw-map output-type fn|output-type)))
                    (kw-map arg-types output-type pre-type))))))

(defns- overload-basis|changed?
  [overload-basis ::overload-basis, prev-basis ::overload-basis|norx > boolean?]
  (or (not= (:body-codelist overload-basis) (:body-codelist prev-basis))
      (if (:types|split overload-basis)
          (not= (:types|split overload-basis) (:types|split prev-basis))
          (or ;; We don't check changedness via `=` when checking type bases because it's possible
              ;; that a change in a reactive type might result in a change in how types are split,
              ;; which is hidden by a lack of change in basis type value.
              (not== (-> overload-basis :output-type|basis ?norx-deref)
                     (:output-type|basis prev-basis))
              (->> overload-basis
                   :arg-types|basis
                   (uc/map-indexed+
                     (c/fn [i|t t] (not== (?norx-deref t)
                                          (-> prev-basis :arg-types|basis (get i|t)))))
                   (seq-or true?))))))

(defns- establish-dependency-relations-on-new-overload-bases!
  "This establishes a dependency relation on both the `fn|output-type` and on new reactive types
   defined in `!overload-bases`.

   Currently only intended to be used by `!fn|types`."
  [fn|output-type t/type?, {:keys [prev-norx _, current _]} ::overload-bases-data]
  (?deref fn|output-type)
  (->> current
       (uc/drop+ (count prev-norx))
       (uc/run!  (c/fn [{:keys [arg-types|basis output-type|basis]}]
                   (->> arg-types|basis (uc/run! ?deref))
                   (?deref output-type|basis)))))

(defns- >unanalyzed-overload
  [{:as basis :keys [args-form _, varargs-form _]} ::overload-basis
   i|basis    index?
   type-datum ::type-datum
   > ::unanalyzed-overload]
  (let [variadic?   (not (empty? varargs-form))
        arg-classes (->> type-datum :arg-types (uc/map type>class))
        arglist-code|reify|unhinted
          (cond-> (-> args-form keys vec)
            variadic? (conj (-> varargs-form keys first)))
        arglist-code|hinted
          (->> arglist-code|reify|unhinted
               (uc/map-indexed
                 (c/fn [i|arg arg|form]
                   (ufth/with-type-hint arg|form
                     ;; `>body-embeddable-tag` because this will go in a `let*`
                     (-> arg-classes (uc/get i|arg) ufth/>body-embeddable-tag)))))]
    (-> (select-keys basis
          [:arglist-form|unanalyzed :args-form :body-codelist :inline? :output-type|form
           :varargs-form])
        (merge type-datum)
        (assoc :ns-name (:ns-name basis))
        (merge (kw-map arg-classes arglist-code|hinted arglist-code|reify|unhinted i|basis
                       variadic?)))))

(defns- >changed-unanalyzed-overloads
  "A 'changed' overload here means one of three things:
   - An overload from an overload basis whose type signature has changed, and after being split,
     does not have the same type signature as that of an existing overload
   - An overload from an overload basis for which its body has changed and it is an inline overload
   - An overload from a newly declared overload basis whose type signature is unique for the
     `t/defn` in question
   - An overload from a newly declared overload basis whose type signature is the same as one that
     already exists for the `t/defn` in question (in which case its implementation will overwrite
     the existing one).

   'Cheaply' O(m•n) where `m` is the number split types resulting from changed overload bases, and
   `n` is the size of the existing overload types. 'Cheap' because only a `=` check is performed `n`
   times for each `m`. All other computations are done only once for each `m`."
  [fn|globals ::fn|globals
   {:keys [prev-norx _, current _]} ::overload-bases-data
   existing-overload-types (us/nilable (us/vec-of ::types-decl-datum))
   > (us/vec-of ::unanalyzed-overload)]
  (let [first-new-basis-index (count prev-norx)]
    (->> current
         (uc/map-indexed+
           (c/fn [i|basis
                  {:as   basis
                   :keys [args-form body-codelist|unanalyzed output-type|form types|split]}]
             (let [new-overload-basis? (>= i|basis first-new-basis-index)
                   prev-basis          (get prev-norx i|basis)
                   changed?            (when prev-basis (overload-basis|changed? basis prev-basis))]
               (when (or new-overload-basis? changed?)
                 (let [type-signature-equal-to-existing?
                         (c/fn [{:keys [arg-types output-type]}]
                           (seq-or #(and (= output-type (:output-type %))
                                         (= arg-types   (:arg-types   %)))
                                   existing-overload-types))]
                   (->> (or types|split (overload-basis-data>types+
                                          fn|globals (:ns-name basis) args-form output-type|form
                                          body-codelist|unanalyzed))
                        (cond->> (and (not new-overload-basis?)
                                      (= (:body-codelist basis) (:body-codelist prev-basis)))
                          (uc/remove+ type-signature-equal-to-existing?))
                        (uc/map+ (c/fn [type-datum]
                                   (>unanalyzed-overload basis i|basis type-datum)))))))))
         (uc/filter+ identity)
         uc/cat)))

(defns- validate-unique-types-for-unanalyzed-overloads
  "Prior to validation we must first sort the overloads by comparing their arg types. Then if we
   find any type signature duplicates in a linear scan, we throw an error."
  [unanalyzed-overloads (us/seq-of ::unanalyzed-overload)
   > (us/vec-of ::unanalyzed-overload)]
  (->> unanalyzed-overloads
       (dedupe-type-data
         (c/fn [overloads prev-overload overload]
           (err! "Duplicate input types for overload"
                 (umap/om :arglist-form-0 (:arglist-form|unanalyzed prev-overload)
                          :arg-types-0    (:arg-types prev-overload)
                          :body-0         (:body-form prev-overload)
                          :arglist-form-1 (:arglist-form|unanalyzed overload)
                          :arg-types-1    (:arg-types overload)
                          :body-1         (:body-form overload)))))))

(defns- overload-bases-data>fn|types
  "Each overload type is structurally (`=`) unique and if an overload is introduced which is `t/=`
   but not `=` then that overload will be rejected."
  [overload-bases-data ::overload-bases-data
   existing-fn-types   (us/nilable ::fn|types)
   opts                ::opts
   {:as   fn|globals
    :keys [fn|name _, fn|ns-name _, fn|output-type _, fn|overload-types-name _]} ::fn|globals
   > ::fn|types]
  (establish-dependency-relations-on-new-overload-bases! fn|output-type overload-bases-data)
  (let [fn|output-type-norx|prev (:fn|output-type-norx existing-fn-types)
        fn|output-type-norx      (?deref fn|output-type)
        existing-overload-types  (:overload-types existing-fn-types)]
    (when (and existing-fn-types
               (t/not= fn|output-type-norx fn|output-type-norx|prev))
      (TODO "`fn|output-type` changed; not sure what to do at this point"
            {:fn|output-type|prev fn|output-type-norx|prev
             :fn|output-type|new  fn|output-type-norx}))
    (if-not-let [changed-unanalyzed-overloads
                   (seq (>changed-unanalyzed-overloads
                          fn|globals overload-bases-data existing-overload-types))]
      (or existing-fn-types
          {:fn|output-type-norx fn|output-type-norx
           :fn|type-norx        (t/ftype (uid/qualify fn|ns-name fn|name) fn|output-type-norx)
           :overload-types []})
      (let [sorted-changed-unanalyzed-overloads
              (->> changed-unanalyzed-overloads
                   (sort-overload-types :arg-types)
                   validate-unique-types-for-unanalyzed-overloads)
            first-current-overload-id (count existing-overload-types)
            new-overload? (c/fn [type-datum] (>= (:id type-datum) first-current-overload-id))
            sorted-changed-overload-types
              (->> sorted-changed-unanalyzed-overloads
                   (uc/map-indexed
                     (c/fn [i {:as   unanalyzed-overload
                               :keys [arg-types output-type body-codelist arglist-code|hinted
                                      inline?]}]
                       (-> (kw-map arg-types output-type arglist-code|hinted body-codelist inline?)
                           (assoc :id      (+ i first-current-overload-id)
                                  :ns-name (:ns-name unanalyzed-overload))))))
            ;; We need to maintain the `overload-types` ordering by type-specificity so the dynamic
            ;; dispatch and fn-type work correctly.
            overload-types-with-replacing-ids
              (if (empty? existing-overload-types)
                  (->> sorted-changed-overload-types
                       (uc/map-indexed (c/fn [i datum] (assoc datum :index i))))
                  (->> ;; We `join` in this order because if two overloads are of equal sorting
                       ;; priority, the ones with earlier IDs should appear in
                       ;; `dedupe-overload-types-data`
                       (ur/join existing-overload-types sorted-changed-overload-types)
                       (sort-overload-types :arg-types)
                       (dedupe-overload-types-data fn|ns-name fn|name)
                       (uc/map-indexed (c/fn [i datum] (assoc datum :index i)))))
            ;; For recursive purposes
            fn|type-norx (overload-types>ftype
                           fn|ns-name fn|name overload-types-with-replacing-ids fn|output-type-norx)
            ;; We should analyze everything first in order to figure out body-dependent input types
            ;; before we can compare them against each other, but we're ignoring body-dependent input
            ;; types for now
            sorted-changed-overloads
              (->> sorted-changed-unanalyzed-overloads
                   (uc/map-indexed
                     (c/fn [i x]
                       (let [id (+ i first-current-overload-id)]
                         (unanalyzed-overload>overload opts fn|globals x id
                           overload-types-with-replacing-ids fn|type-norx)))))
            overload-types
              (->> overload-types-with-replacing-ids
                   (uc/map
                     (c/fn [datum]
                       (let [id (or (:replacing-id datum) (:id datum))]
                         (when (>= id first-current-overload-id)
                           (let [overload (get sorted-changed-overloads
                                               (- id first-current-overload-id))]
                             ;; So that direct dispatch can use them later on in the pipeline
                             (alist-conj! !overload-queue (assoc datum :overload overload))))
                         (dissoc datum :replacing-id)))))]
        (kw-map fn|output-type-norx fn|type-norx overload-types)))))

;; ----- Direct dispatch ----- ;;

(defns- >direct-dispatch
  [{:as opts :keys [gen-gensym _, lang _, kind _]} ::opts
   fn|globals ::fn|globals
   fn|types   ::fn|types]
  (case lang
    :clj  (let [direct-dispatch-data-seq
                  (->> !overload-queue
                       (uc/map
                         (c/fn [{:as type-decl-datum :keys [arg-types id index overload]}]
                           {:overload-types-decl
                              (>overload-types-decl opts fn|globals type-decl-datum fn|types)
                            :reify (overload>reify overload opts fn|globals id)})))
                form (->> direct-dispatch-data-seq
                          (uc/mapcat
                            (c/fn [{:as direct-dispatch-data :keys [overload-types-decl]}]
                              [(:form overload-types-decl)
                               (-> direct-dispatch-data :reify :form)])))]
            (kw-map form direct-dispatch-data-seq))
    :cljs (TODO)))

;; ===== Dynamic dispatch ===== ;;

(defns >direct-dispatch|reify-call [reify-name symbol?, args-codelist (us/seq-of t/any?)]
  `(. ~reify-name ~uana/direct-dispatch-method-sym ~@args-codelist))

;; TODO spec
(defns unsupported! [name- _ #_t/qualified-symbol?, args _ #_indexed?, i index?]
  (throw (ex-info "This function is unsupported for the type combination at the argument index."
                  {:name name- :args args :arg-index i})))

(defns- >combinatoric-seq+
  [{:as fn|globals :keys [fn|ns-name _ fn|name _]} ::fn|globals
   overload-types-for-arity (us/vec-of ::types-decl-datum)
   arglist (us/vec-of simple-symbol?)]
  (->> overload-types-for-arity
       (uc/map+
         (c/fn [{:as types-decl-datum :keys [arg-types] overload|id :id ns-name- :ns-name}]
          (let [overload-types-decl|name (>overload-types-decl|name ns-name- fn|name overload|id)
                reify-name-unhinted      (>reify-name-unhinted      ns-name- fn|name overload|id)]
            [(>direct-dispatch|reify-call reify-name-unhinted arglist)
             (->> arg-types
                  (uc/map-indexed
                    (c/fn [i|arg arg-type]
                      {:i    i|arg
                       :t    arg-type
                       :getf `((Array/get ~overload-types-decl|name ~i|arg)
                                ~(get arglist i|arg))})))])))))

(defns- >dynamic-dispatch|body-for-arity
  [{:as fn|globals :keys [fn|ns-name _, fn|name _]} ::fn|globals
   overload-types-for-arity (us/vec-of ::types-decl-datum)
   arglist (us/vec-of simple-symbol?)]
  (if (empty? arglist)
      (let [overload|id (-> overload-types-for-arity first :id)]
        (>direct-dispatch|reify-call (>reify-name-unhinted fn|ns-name fn|name overload|id) arglist))
      (let [!!i|arg (atom 0)
            combinef
              (c/fn
                ([] (transient [`ifs]))
                ([ret]
                  (-> ret (conj! `(unsupported! '~(uid/qualify fn|ns-name fn|name)
                                                ~arglist ~(deref !!i|arg)))
                          persistent!
                          seq))
                ([ret getf x i]
                  (reset! !!i|arg i)
                  (uc/conj! ret getf x)))]
        (uc/>combinatoric-tree (count arglist)
          (c/fn [a b] (t/= (:t a) (:t b)))
          (aritoid combinef combinef (c/fn [x [{:keys [getf i]} group]] (combinef x getf group i)))
          uc/conj!|rf
          (aritoid combinef combinef (c/fn [x [k [{:keys [getf i]}]]] (combinef x getf k i)))
          (>combinatoric-seq+ fn|globals overload-types-for-arity arglist)))))

(defns- >dynamic-dispatch-fn|codelist
  [{:as opts       :keys [compilation-mode _, gen-gensym _, lang _, kind _]} ::opts
   {:as fn|globals :keys [fn|meta _, fn|ns-name _, fn|name _, fn|output-type _
                          fn|overload-types-name _, fn|type-name _]} ::fn|globals
   fn|types ::fn|types]
  (let [overload-forms
         (->> fn|types
              :overload-types
              (group-by (fn-> :arg-types count))
              (sort-by key) ; for purposes of reproducibility and organization
              (map (c/fn [[arg-ct overload-types-for-arity]]
                     (let [arglist (ufgen/gen-args 0 arg-ct "x" gen-gensym)
                           body    (>dynamic-dispatch|body-for-arity
                                     fn|globals overload-types-for-arity arglist)]
                       (list arglist body)))))
      fn|meta' (merge fn|meta {:quantum.core.type/type (uid/qualify fn|ns-name fn|type-name)})
      overload-types|form
        (when (= compilation-mode :test)
          (->> fn|types :overload-types >form
               (uc/map (c/fn [{:keys [id index inline? arg-types output-type]}]
                         [id index inline? arg-types output-type]))))]
    ;; TODO determine whether CLJS needs (update-in m [:jsdoc] conj "@param {...*} var_args")
    (if (= kind :extend-defn!)
        [overload-types|form
         `(doto (intern (quote ~fn|ns-name) (quote ~fn|name)
                  ~(with-meta `(fn* ~@overload-forms) fn|meta'))
            (alter-meta! merge ~fn|meta'))]
        (let [dispatch-form `(uvar/defmeta ~fn|name ~fn|meta'
                               ~(when-not (empty? overload-forms) `(fn* ~@overload-forms)))]
          (if (= compilation-mode :test)
              [overload-types|form dispatch-form]
              [dispatch-form])))))

;; ===== End dynamic dispatch ===== ;;

(defns- overload-basis-form>overload-basis
  "This is for overloads being created brand-new within `defn` or `extend-defn!`."
  [opts ::opts
   {:as   fn|globals
    :keys [fn|inline? _, fn|output-type _, fn|output-type _, fn|output-type|form _]} ::fn|globals
   {:as overload-basis-form
    {:as arglist-form
     args                      [:args    _]
     varargs                   [:varargs _]
     pre-type|form             [:pre     _]
     [_ _, output-type|form _] [:post    _]} [:arglist _]
     body-codelist|unanalyzed  [:body    _]} _
   > ::overload-basis]
  (when pre-type|form (TODO "Need to handle pre"))
  (when varargs       (TODO "Need to handle varargs"))
  (let [arg-types|form   (->> args (mapv (c/fn [{[kind #_#{:any :spec}, t #_t/form?] :spec}]
                                           (case kind :any `t/any? :spec t))))
        output-type|form (case output-type|form _ `t/any?, nil nil, output-type|form)
        arg-bindings     (->> args
                              (mapv (c/fn [{[kind binding-] :binding-form}]
                                      ;; TODO this assertion is purely temporary until destructuring
                                      ;; is supported
                                      (assert kind :sym)
                                      binding-)))
        ;; TODO support varargs
        varargs-binding  (when varargs
                           ;; TODO this assertion is purely temporary until destructuring is
                           ;; supported
                           (assert (-> varargs :binding-form first (= :sym))))
        args-form        (reduce-2 assoc (umap/om) arg-bindings arg-types|form)
        ns-name-val      (>symbol *ns*)
        [arglist-basis]  (uana/analyze-arg-syms {:opts {:ns (the-ns ns-name-val)}} args-form
                           (or output-type|form fn|output-type) false)
        binding->arg-type|basis (->> arglist-basis :env :opts :arg-env deref (uc/map-vals' :type))
        arg-types|basis   (->> args-form keys (uc/map binding->arg-type|basis))
        output-type|basis (-> arglist-basis :output-type-node :type)
        dependent?        (:dependent? arglist-basis)
        reactive?         (or (utr/rx-type? output-type|basis)
                              (seq-or utr/rx-type? arg-types|basis))
        inline?           (boolean (or (and fn|inline? (-> arglist-form meta :unline? not))
                                       (-> arglist-form meta :inline?)))]
    {:ns-name                 ns-name-val
     ;; TODO Only needed if `dependent?` or if new
     :args-form               args-form
     :arg-types|basis         arg-types|basis
     ;; TODO Only needed if `dependent?` or if new
     :varargs-form            (when varargs {varargs-binding nil}) ; TODO `nil` isn't right
     :arglist-form|unanalyzed (cond-> (uc/cat args-form)
                                varargs          (conj '& varargs)
                                pre-type|form    (conj '| pre-type|form)
                                output-type|form (conj '> output-type|form))
     ;; TODO Only needed if `dependent?` or if new
     :output-type|form        output-type|form
     :output-type|basis       output-type|basis
     ;; We store this only for arglists with dependent types. If the arglist is reactive, then
     ;; downstream, if the reactive types change, the new split types can be compared with the
     ;; previous split types. If non-reactive, then the split types of this overload basis can be
     ;; compared to existing overload bases.
     :types|split             (when dependent?
                                (->> (overload-basis-data>types+ fn|globals ns-name-val args-form
                                       output-type|form body-codelist|unanalyzed)
                                     ur/join))
     ;; TODO Only needed if `inline? or `reactive?`, or if new
     :body-codelist           body-codelist|unanalyzed
     :dependent?              dependent?
     :reactive?               reactive?
     :inline?                 inline?}))

;; ===== Reactive auxiliary vars ===== ;;

(defns- incorporate-overload-bases
  "O(m•n) where `m` = # of existing overload bases and `n` = # of new overload bases."
  [existing-bases (us/vec-of ::overload-basis), new-bases (us/vec-of ::overload-basis)
   > (us/vec-of ::overload-basis)]
  (reduce
    (c/fn [bases new-basis]
      (if-let [i|existing
                 (->> existing-bases
                      (uc/map-indexed+
                        (c/fn [i existing-basis]
                          (ifs-let
                            [same-code?
                              (and (= (:arglist-form|unanalyzed existing-basis)
                                      (:arglist-form|unanalyzed new-basis))
                                   (= (:body-codelist           existing-basis)
                                      (:body-codelist           new-basis)))]
                            (do (ulog/pr :warn
                                  "Overwriting existing overload with same arglist and body"
                                  {:arglist|form (:arglist-form|unanalyzed new-basis)})
                                i)
                            ;; This only checks for `=` because `t/=` will be deduped later on in
                            ;; overloads, not overload bases
                            ;; TODO this doesn't take into account `|` types
                            [same-unreactive-type?
                              (and (not (:reactive? existing-basis))
                                   (not (:reactive? new-basis))
                                   (if (and (:dependent? existing-basis)
                                            (:dependent? new-basis))
                                       (= (:types|split existing-basis)
                                          (:types|split new-basis))
                                       (and (= (:output-type|basis existing-basis)
                                               (:output-type|basis new-basis))
                                            (= (:arg-types|basis   existing-basis)
                                               (:arg-types|basis   new-basis)))))]
                            (do (ulog/pr :warn "Overwriting existing overload with same types"
                                  {:arglist|form|prev (:arglist-form|unanalyzed existing-basis)
                                   :arglist|form      (:arglist-form|unanalyzed new-basis)})
                                i)
                            ;; TODO enhance this; figure out how to effectively compare reactive
                            ;;      and dependent types, if that's even possible
                            ;; TODO maybe we don't even want this; maybe this should be based on
                            ;;      an atom that's configurable. It does override/nullify some
                            ;;      safety behavior in `overload-basis|changed?`
                            [probably-same-reactive-type?
                              (and (= (:reactive?   existing-basis)
                                      (:reactive?   new-basis))
                                   (= (:dependent?  existing-basis)
                                      (:dependent?  new-basis))
                                   (= (:types|split existing-basis)
                                      (:types|split new-basis))
                                   (= (-> existing-basis :output-type|basis ?norx-deref)
                                      (-> new-basis      :output-type|basis ?norx-deref))
                                   (= (-> existing-basis :arg-types|basis   ?norx-deref)
                                      (-> new-basis      :arg-types|basis   ?norx-deref)))]
                            (do (ulog/pr :warn
                                  (str "Assuming that new reactive overload basis is a subsequent "
                                       "version of existing reactive overload basis")
                                  {:new      (:arglist-form|unanalyzed existing-basis)
                                   :existing (:arglist-form|unanalyzed existing-basis)})
                                i)
                            nil)))
                      (uc/filter+ some?)
                      uc/first)]
        (assoc bases i|existing new-basis)
        (conj bases new-basis)))
    existing-bases
    new-bases))

(defns- with-optional-validate-overload-bases [overload-bases ::overload-bases-data] overload-bases)

(defns- >!overload-bases
  "`!overload-bases` is a reactive atom updated by `t/extend-defn!`, which cannot be deleted from
   but which can be updated and appended to."
  [{:as opts       :keys [kind _]} ::opts
   {:as fn|globals :keys [fn|ns-name _, fn|overload-bases-name _]} ::fn|globals
   overload-bases-form _]
  (let [new-overload-bases
         (->> overload-bases-form
              (uc/map (c/fn [x] (overload-basis-form>overload-basis opts fn|globals x))))]
    (if (= kind :extend-defn!)
        (with-do-let [!overload-bases
                        (-> (uid/qualify fn|ns-name fn|overload-bases-name) resolve var-get)]
          (let [{:as overload-bases :keys [current]} (norx-deref !overload-bases)
                overload-bases'
                  {:prev-norx
                    (->> current
                         (uc/map
                           (c/fn [basis]
                             {:arg-types|basis   (->> basis :arg-types|basis (uc/map ?norx-deref))
                              :output-type|basis (-> basis :output-type|basis ?norx-deref)
                              :types|split       (:types|split   basis)
                              :body-codelist     (:body-codelist basis)
                              :dependent?        (:dependent?    basis)
                              :reactive?         (:reactive?     basis)
                              :inline?           (:inline?       basis)})))
                   :current (incorporate-overload-bases current new-overload-bases)}]
            (with-optional-validate-overload-bases overload-bases')
            (let [prev-overload-bases (norx-deref !overload-bases)]
              (alist-conj! !rollback-queue
                #(uref/set! !overload-bases prev-overload-bases))
              (uref/set! !overload-bases overload-bases'))))
        (with-do-let [!overload-bases (urx/! {:prev-norx nil :current new-overload-bases})]
          (intern-with-rollback! fn|ns-name fn|overload-bases-name !overload-bases)))))

(defns- >!fn|types
  "`!fn|types` is a reaction which depends on the `!overload-bases` atom and all reactive types
   declared in any arglist of the `t/defn` in question, as well as the overall output type (if
   reactive) of the `t/defn`.

   Whatever the values of `opts` and `fn|globals` are at the time of `t/defn` definition, that's
   what they'll be for the lifetime of the function."
  [{:as opts       :keys [kind _]} ::opts
   {:as fn|globals :keys [fn|ns-name _, fn|overload-types-name _, fn|type-name _]} ::fn|globals
   !overload-bases urx/reactive?]
  (if (= kind :extend-defn!)
      (-> (uid/qualify fn|ns-name fn|overload-types-name) resolve var-get)
      (with-do-let [!fn|types (doto (urx/!rx @!overload-bases)
                                    (uref/add-interceptor! :the-interceptor
                                      (c/fn [_ _ old-overload-types overload-bases-data]
                                        ;; `opts` and `fn|globals` are closed over
                                        (overload-bases-data>fn|types
                                          overload-bases-data old-overload-types opts fn|globals)))
                                    norx-deref)]
        (intern-with-rollback! fn|ns-name fn|overload-types-name !fn|types))))

(defns- >!fn|type
  [{:as opts       :keys [kind _]} ::opts
   {:as fn|globals :keys [fn|ns-name _, fn|output-type _, fn|type-name _]} ::fn|globals
   !fn|types _]
  (if (= kind :extend-defn!)
      (-> (uid/qualify fn|ns-name fn|type-name) resolve var-get)
      (with-do-let [!fn|type (t/rx* (urx/>!rx #(:fn|type-norx @!fn|types) {:eq-fn t/=}) nil)]
        (intern-with-rollback! fn|ns-name fn|type-name !fn|type))))

;; ===== `opts` + `fn|globals` ===== ;;

(defns- >fn|opts
  "`opts` are per invocation of `t/defn` and/or `extend-defn!`, while `globals` persist for as long
   as the `t/defn` does."
  [kind ::kind, lang ::lang, compilation-mode ::compilation-mode > ::opts]
  (let [gen-gensym-base (ufgen/>reproducible-gensym|generator)
        gen-gensym      (c/fn [x] (symbol (str (gen-gensym-base x) "__")))]
    (kw-map compilation-mode gen-gensym kind lang)))

(defns- >fn|globals+?overload-bases-form
  "`opts` are per invocation of `t/defn` and/or `extend-defn!`, while `globals` persist for as long
   as the `t/defn` does."
  [kind ::kind, args _ > (us/kv {:fn|globals ::fn|globals :overload-bases-form t/any?})]
  (let [{:as args'
         :keys [:quantum.core.specs/fn|name
                :quantum.core.defnt/fn|extended-name
                :quantum.core.defnt/output-spec]
         overload-bases-form :quantum.core.defnt/overloads
         fn|meta :quantum.core.specs/meta}
          (us/validate args (case kind :defn         :quantum.core.defnt/defnt
                                       :fn           :quantum.core.defnt/fnt
                                       :extend-defn! :quantum.core.defnt/extend-defn!))
        fn|var          (when (= kind :extend-defn!)
                              (or (uvar/resolve *ns* fn|extended-name)
                          (err! "Could not resolve fn name to extend"
                                {:sym fn|extended-name})))
        fn|ns-name      (if (= kind :extend-defn!)
                           (-> fn|var >?namespace >symbol)
                           (>symbol *ns*))
        fn|name         (if (= kind :extend-defn!)
                           (-> fn|extended-name >name symbol)
                           fn|name)
        fn|globals-name (symbol (str fn|name "|__globals"))]
      (if (= kind :extend-defn!)
          {:fn|globals          (-> (uid/qualify fn|ns-name fn|globals-name) resolve var-get)
           :overload-bases-form overload-bases-form}
          (let [fn|inline?             (if (nil? (:inline fn|meta))
                                           false
                                           (us/validate (:inline fn|meta) t/boolean?))
                fn|meta                (dissoc fn|meta :inline)
                fn|output-type|form    (or (second output-spec) `t/any?)
                ;; TODO this needs to be analyzed for dependent types referring to local vars
                fn|output-type         (eval fn|output-type|form)
                fn|overload-bases-name (symbol (str fn|name "|__bases"))
                fn|overload-types-name (symbol (str fn|name "|__types"))
                fn|type-name           (symbol (str fn|name "|__type"))
                fn|globals
                  (kw-map fn|globals-name fn|inline? fn|meta fn|name fn|ns-name fn|output-type|form
                          fn|output-type fn|overload-bases-name fn|overload-types-name
                          fn|type-name)]
            (intern-with-rollback! fn|ns-name fn|globals-name fn|globals)
            (kw-map fn|globals overload-bases-form)))))

;; ===== Whole `t/(de)fn` creation ===== ;;

(defns fn|code [kind ::kind, lang ::lang, compilation-mode ::compilation-mode, args _]
  (uerr/catch-all
    (let [opts (>fn|opts kind lang compilation-mode)
          {:keys [fn|globals overload-bases-form]} (>fn|globals+?overload-bases-form kind args)
          !overload-bases (>!overload-bases opts fn|globals overload-bases-form)
          !fn|types       (>!fn|types       opts fn|globals !overload-bases)
          fn|types        (norx-deref !fn|types)
          !fn|type        (>!fn|type        opts fn|globals !fn|types)]
      (if (empty? (norx-deref !overload-bases))
          `(declare ~(:fn|name fn|globals))
          (let [direct-dispatch  (>direct-dispatch              opts fn|globals fn|types)
                dynamic-dispatch (>dynamic-dispatch-fn|codelist opts fn|globals fn|types)
                fn-codelist
                  (->> `[;; For recursion
                         ~@(when (not= kind :extend-defn!)
                             [`(declare ~(:fn|name fn|globals))])
                         ~@(:form direct-dispatch)
                         ~@dynamic-dispatch]
                         (remove nil?))]
            (case kind
              :fn                   (TODO "Haven't done t/fn yet")
              (:defn :extend-defn!) `(do ~@fn-codelist)))))
    e
    (do (ulog/ppr :error e)
        (drain-rollback-queue!)
        (err! nil "Exception; rolled back successfully" nil nil e))
    (do (uvec/alist-empty! !rollback-queue)
        (uvec/alist-empty! !overload-queue))))

#?(:clj
(defmacro fn
  "With `t/fn`, protocols, interfaces, and multimethods become unnecessary. The preferred method of
   dispatch becomes the function alone.

   `t/fn` is intended to catch many runtime errors at compile time, but cannot catch all of them.

   `t/fn`, along with `t/defn`, `t/dotyped`, and others, creates a typed context in which its
   internal forms are analyzed, type-consistency is checked, and type-dispatch is resolved at
   compile time inasmuch as possible, and at runtime only when necessary.

   Recommendations for the type system:
   - Primitives are always preferred to boxed values. All values that can be primitives (i.e. ones
     that are `t/<=` w.r.t. a `(t/isa? <boxed-primitive-class>)`) are treated as primitives unless
     specifically marked otherwise with the `t/ref` metadata-adding directive.
   - One could imagine a dynamic set of types corresponding to a given predicate, e.g. `decimal?`.
     Say someone comes up with a new `decimal?`-like class and wants to redefine `decimal?` to
     accommodate. One could define `decimal?` as a reactive/extensible type to do this. However, it
     is preferable to instead define a marker protocol called `PDecimal` or some such and put that
     on the defined `deftype` itself, and incorporate `PDecimal` into `decimal?` from the start. In
     this way fewer reactive changes have to happen and less compilation occurs.

   Compile-Time (Direct) Dispatch characteristics
   - Any input, if its type is `t/<=` a non-nil primitive (boxed or not) class, it will be marked
     as a primitive in the corresponding `reify`.
   - If an input is a nilable primitive, its nilability will not result in only one `reify`
     overload with a boxed input, but rather will result in two `reify` overloads — one
     corresponding to a nil input and another for the primitive input.

   Runtime (Dynamic) Dispatch characteristics
   - Compile-Time Dispatch is preferred to Runtime Dispatch in all but the following situations, in
     which Compile-Time Dispatch is not possible:
     - When a typed function (or a typed object with function-like characteristics such as a
       `t/deftype`) is referenced outside of a typed context.

   Metadata directives special to all typed contexts include:
   - `:val` : If `true` and attached as metadata to a form, it will cause that form's type to be
              `t/and`ed with `t/val?`.
   - `:dyn` : If `true` and attached as metadata to a form corresponding with a typed fn in functor
              position, it will cause that typed fn to be called dynamically if no direct dispatch
              is found at compile time.
              - For instance, `(name (read ...))` fails at compile-time; we want it to at least try
                at runtime. So we annotate like `(^:dyn name (read ...))`, which tells the compiler
                to figure out at runtime whether a call to `name` will succeed.

   Metadata directives special to `t/fn`/`t/defn` include:
   - `:inline` : If `true` and attached as metadata to the arglist of an overload, will cause that
                 overload to be inlined if possible:
                 - `(t/defn abc (^:inline [] ...))`
                 If `true` and attached as metadata to the whole `t/defn` or `t/fn`, will cause
                 every one of its overloads to be inlined if possible. Overloads added to a `t/defn`
                 with `:inline` `true` will inherit this inline directive unless `:inline` is false
                 for the overload or `:unline` is true:
                 - `(t/defn ^:inline abc ([] ...) ([...] ...))`
                 - `(t/defn ^:inline abc (^{:inline false} [] ...) ([...] ...))`
                 - `(t/defn ^:inline abc ([] ...) (^:unline [...] ...))`
                 Note:
                 - Inlining is possible only in typed contexts.
                 - If the metadata for an overload changes via `extend-defn!` from designating it as
                   inline to designating it as non-inline, or vice versa, unexpected behavior may
                   occur.

   `t/fn` only works fully in contexts in which the metalanguage (compiler language) is the same as
   the object language. Otherwise, while the compiler could still analyze types symbolically to an
   extent, it could not actually run evaluated type-predicates on inputs to determine type-satisfaction.
   - Consumers wishing to use the full-featured `t/fn` in ClojureScript must either use
     bootstrapped ClojureScript or transpile ClojureScript via the JavaScript implementation of
     the Google Closure Compiler. Consumers for whom the version of `t/fn` with purely symbolic
     analysis is acceptable may use the standard approach of transpiling ClojureScript via the Java
     implementation of the Google Closure Compiler."
  [& args] (fn|code :fn (ufeval/env-lang) *compilation-mode* args)))

#?(:clj
(defmacro defn
  "A `defn` with an empty body is like using `declare`."
  [& args] (fn|code :defn (ufeval/env-lang) *compilation-mode* args)))

#?(:clj
(defmacro extend-defn!
  "Currently undefining overloads is not possible."
  [& args] (fn|code :extend-defn! (ufeval/env-lang) *compilation-mode* args)))
