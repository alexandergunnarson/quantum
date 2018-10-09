(ns quantum.untyped.core.type.defnt
  (:refer-clojure :exclude
    [defn])
  (:require
    [clojure.core                               :as core]
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
    [quantum.untyped.core.collections           :as c
      :refer [>set >vec]]
    [quantum.untyped.core.compare               :as ucomp]
    [quantum.untyped.core.data
      :refer [kw-map]]
    [quantum.untyped.core.data.array            :as uarr]
    [quantum.untyped.core.data.map              :as umap]
    [quantum.untyped.core.data.set              :as uset]
    [quantum.untyped.core.error                 :as err
      :refer [TODO err!]]
    [quantum.untyped.core.fn
      :refer [<- aritoid fn-> with-do]]
    [quantum.untyped.core.form                  :as uform
      :refer [>form]]
    [quantum.untyped.core.form.evaluate         :as ufeval]
    [quantum.untyped.core.form.generate         :as ufgen]
    [quantum.untyped.core.form.type-hint        :as ufth]
    [quantum.untyped.core.identifiers           :as uid
      :refer [>name >symbol]]
    [quantum.untyped.core.log                   :as ulog]
    [quantum.untyped.core.logic                 :as ul
      :refer [fn-or fn= ifs]]
    [quantum.untyped.core.loops
      :refer [reduce-2]]
    [quantum.untyped.core.numeric.combinatorics :as ucombo]
    [quantum.untyped.core.reducers              :as r
      :refer [reducei educe]]
    [quantum.untyped.core.spec                  :as s]
    [quantum.untyped.core.specs                 :as uss]
    [quantum.untyped.core.type                  :as t
      :refer [?]]
    [quantum.untyped.core.type.reifications     :as utr]
    [quantum.untyped.core.vars                  :as uvar
      :refer [update-meta]])
  (:import
    [quantum.core Numeric]
    [quantum.core.data Array]))

(defonce *fn->type (atom {}))

(defonce defnt-cache (atom {})) ; TODO For now — but maybe lock-free concurrent hash map to come

(defonce *interfaces (atom {}))

;; Internal specs

(s/def ::lang #{:clj :cljs})

;; "global" because they apply to the whole fnt
(s/def ::fn|globals
  (s/kv {:fn|meta             (s/nilable :quantum.core.specs/meta)
         :fn|name             ::uss/fn|name
         :fn|type             utr/fn-type?
         :fn|output-type|form t/any?
         :fn|output-type      t/type?}))

(s/def ::opts
  (s/kv {:gen-gensym t/fn?
         :lang       ::lang}))

 ;; Technically it's partially analyzed — its type definitions are analyzed (with the exception of
 ;; requests for type inference) while its body is not.
 (s/def ::unanalyzed-overload
   (s/kv {:arg-bindings              (s/vec-of t/any?)
          :varargs-binding           t/any?
          :arg-types|form            (s/vec-of t/any?)
          :arg-types                 (s/vec-of t/type?)
          :output-type|form          t/any?
          :output-type               t/type?
          :body-codelist|pre-analyze t/any?}))

(s/def ::overload|arg-classes (s/vec-of class?))
(s/def ::overload|arg-types   (s/seq-of t/type?))

;; This is the overload after the input specs are split by their respective `t/or` constituents,
;; and after primitivization, but before readiness for incorporation into a `reify`.
;; One of these corresponds to one reify overload.
(s/def ::overload
  (s/kv {:arg-classes                 ::overload|arg-classes
         :arg-types                   ::overload|arg-types
         :arglist-code|fn|hinted      t/any?
         :arglist-code|reify|unhinted t/any?
         :body-form                   t/any?
         :output-class                (s/nilable class?)
         :output-type                 t/type?
         :positional-args-ct          (s/and integer? #(>= % 0))
         ;; When present, varargs are considered to be of class Object
         :variadic?                   t/boolean?}))

(s/def ::input-types-decl
  (s/kv {:form t/any?
         :name simple-symbol?}))

(s/def ::reify
  (s/kv {:form      t/any?
         :interface class?
         :name      simple-symbol?
         :overload  ::overload}))

(s/def ::direct-dispatch-data
  (s/kv {:input-types-decl ::input-types-decl
         :reify            ::reify}))

(s/def ::direct-dispatch
  (s/kv {:form                     t/any?
         :direct-dispatch-data-seq (s/vec-of ::direct-dispatch-data)}))

#_(:clj
(core/defn fnt|arg->class [lang {:as arg [k spec] ::fnt|arg-spec :keys [arg-binding]}]
  (cond (not= k :spec) java.lang.Object; default class
        (symbol? spec) (pred->class lang spec))))

;; TODO optimize such that `post-type|form` doesn't create a new type-validator wholesale every
;; time the function gets run; e.g. extern it
(core/defn >with-runtime-output-type [body output-type|form] `(t/validate ~body ~output-type|form))

;; TODO move
(def index? #(and (integer? %) (>= % 0)))

;; TODO simplify this class computation

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
  (if (-> t meta :quantum.core.type/ref?)
      java.lang.Object
      (let [cs  (t/type>classes t)
            cs' (disj cs nil)]
        (if (-> cs' count (not= 1))
            java.lang.Object
            (-> (first cs')
                (cond-> (not (contains? cs nil)) t/class>most-primitive-class) class>simplest-class))))))

(defns- >actual-output-type [declared-output-type t/type?, body-node uast/node? > t/type?]
  (let [err-info {:form                 (:form body-node)
                  :type                 (:type body-node)
                  :declared-output-type declared-output-type}]
    (case (t/compare (:type body-node) declared-output-type)
      ;; If the deduced body type is `t/<=` declared output type then we pick the body type
      (-1 0) (cond-> (:type body-node)
               (-> declared-output-type meta :quantum.core.type/ref?) t/ref)
      1      (if (or (-> declared-output-type meta :quantum.core.type/runtime?)
                     (-> declared-output-type meta :quantum.core.type/assume?))
                 declared-output-type
                 (err! "Body type incompatible with declared output type" err-info))
      (2 3)  (err! "Body type incompatible with declared output type" err-info))))

#?(:clj
(defns- unanalyzed-overload>overload
  "Given an `::unanalyzed-overload`, performs type analysis on the body and computes a resulting
   `t/fn` overload, which is the foundation for one `reify`."
  [{:keys [arg-bindings _, varargs-binding _, arg-types _, output-type|form _
           body-codelist|pre-analyze _]
    declared-output-type [:output-type _]}
   ::unanalyzed-overload
   {:as fn|globals :keys [fn|name _, fn|type _, fn|output-type _]} ::fn|globals
   {:as opts       :keys [lang _]} ::opts
   > ::overload]
  (let [;; Not sure if `nil` is the right approach for the value
        recursive-ast-node-reference (uast/symbol {} fn|name nil fn|type)
        env         (->> (zipmap arg-bindings arg-types)
                         (c/map' (fn [[arg-binding arg-type]]
                                   [arg-binding (uast/unbound nil arg-binding arg-type)]))
                         ;; To support recursion
                         (<- (assoc fn|name recursive-ast-node-reference)))
        body-node   (uana/analyze env (ufgen/?wrap-do body-codelist|pre-analyze))
        arg-classes (->> arg-types (c/map type>class))
        hint-arg|fn (fn [i arg-binding]
                      (ufth/with-type-hint arg-binding
                        (ufth/>fn-arglist-tag
                          (c/get arg-classes i)
                          lang
                          (c/count arg-bindings)
                          (boolean varargs-binding))))
        actual-output-type (>actual-output-type declared-output-type body-node)
        body-form
          (-> (:form body-node)
              (cond-> (-> actual-output-type meta :quantum.core.type/runtime?)
                (>with-runtime-output-type output-type|form))
              (ufth/cast-bindings|code
                (->> (c/zipmap-into (umap/om) arg-bindings arg-classes)
                     (c/remove-vals' (fn-or nil? (fn= java.lang.Object) t/primitive-class?)))))]
      {:arg-classes                 arg-classes
       :arg-types                   arg-types
       :arglist-code|fn|hinted      (cond-> (->> arg-bindings (c/map-indexed hint-arg|fn))
                                      varargs-binding (conj '& varargs-binding))
       :arglist-code|reify|unhinted (cond-> arg-bindings varargs-binding (conj varargs-binding))
       :body-form                   body-form
       :positional-args-ct          (count arg-bindings)
       :output-type                 actual-output-type
       :output-class                (type>class actual-output-type)
       :variadic?                   (boolean varargs-binding)})))

(defns- class>interface-part-name [c class? > string?]
  (if (= c java.lang.Object)
      "Object"
      (let [illegal-pattern #"\|\+"]
        (if (->> c >name (re-find illegal-pattern))
            (err! "Class cannot contain pattern" {:class c :pattern illegal-pattern})
            (-> c >name (str/replace "." "|"))))))

;; ===== Direct dispatch ===== ;;

;; ----- Direct dispatch: `reify` ---- ;;

(defns- overload-classes>interface-sym [args-classes (s/seq-of class?), out-class class? > symbol?]
  (>symbol (str (->> args-classes (c/lmap class>interface-part-name) (str/join "+"))
                ">" (class>interface-part-name out-class))))

(def reify-method-sym 'invoke)

(defns- overload-classes>interface
  [args-classes (s/vec-of class?), out-class class?, gen-gensym fn?]
  (let [interface-sym     (overload-classes>interface-sym args-classes out-class)
        hinted-method-sym (ufth/with-type-hint reify-method-sym
                            (ufth/>interface-method-tag out-class))
        hinted-args       (ufth/hint-arglist-with
                            (ufgen/gen-args 0 (count args-classes) "x" gen-gensym)
                            (map ufth/>interface-method-tag args-classes))]
    `(~'definterface ~interface-sym (~hinted-method-sym ~hinted-args))))

#?(:clj
(defns overload>reify
  [{:as   overload
    :keys [arg-classes _, arglist-code|reify|unhinted _, body-form _, out-class _]} ::overload
   {:as opts :keys [gen-gensym _]} ::opts
   {:keys [fn|name _]} ::fn|globals
   i|overload index?
   > ::reify]
  (let [interface-k {:out out-class :in arg-classes}
        interface
          (-> *interfaces
              (swap! update interface-k
                #(or % (eval (overload-classes>interface arg-classes out-class gen-gensym))))
              (c/get interface-k))
        arglist-code
          (>vec (concat [(gen-gensym '_)]
                  (->> arglist-code|reify|unhinted
                       (map-indexed
                         (fn [i|arg arg|form]
                           (ufth/with-type-hint arg|form
                             (-> arg-classes (c/get i|arg) ufth/>arglist-embeddable-tag)))))))
        reify-name (>symbol (str fn|name "|__" i|overload))
        form `(~'def ~reify-name
                (reify* [~(-> interface >name >symbol)]
                  (~(ufth/with-type-hint reify-method-sym (ufth/>arglist-embeddable-tag out-class))
                    ~arglist-code ~body-form)))]
    {:form      form
     :interface interface
     :name      reify-name
     :overload  overload})))

;; TODO spec
;; TODO use!!
(core/defn assert-monotonically-increasing-types!
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

;; ----- Direct dispatch: putting it all together ----- ;;

(defns >input-types-decl
  "The evaluated `form` of each input-types-decl is an array of non-primitivized types that the
   dynamic dispatch uses to dispatch off input types."
  [{:keys [fn|name _]} ::fn|globals, arg-types (s/vec-of t/type?), i|overload index?
   > ::input-types-decl]
  (let [decl-name (>symbol (str fn|name "|__" i|overload "|types"))
        form      (list 'def (ufth/with-type-hint decl-name "[Ljava.lang.Object;")
                             (list* `uarr/*<> (c/lmap >form arg-types)))]
    {:form form :name decl-name}))

(defns >direct-dispatch
  [{:as fn|globals :keys [fn|name _]} ::fn|globals
   {:as opts       :keys [gen-gensym _, lang _]} ::opts
   overloads (s/vec-of ::overload)
   > ::direct-dispatch]
  (case lang
    :clj  (let [direct-dispatch-data-seq
                  (->> overloads
                       (c/map-indexed
                         (fn [i|overload {:keys [arg-types]}]
                           {:input-types-decl
                              (>input-types-decl fn|globals arg-types i|overload)
                            :reify (overload>reify overload opts fn|globals i|overload)})))
                form (->> direct-dispatch-data-seq
                          (c/mapcat
                            (fn [{:as direct-dispatch-data :keys [input-types-decl]}]
                              (list (:form input-types-decl)
                                    (-> direct-dispatch-data :reify :form))])))
            (kw-map form direct-dispatch-data-seq))
    :cljs (TODO)))

;; ===== Dynamic dispatch ===== ;;

(defns- >dynamic-dispatch|reify-call [reify- ::reify, arglist (s/vec-of simple-symbol?)]
  (let [hinted-reify-sym (-> reify- :name (ufth/with-type-hint (-> reify- :interface >name)))]
    `(. ~hinted-reify-sym ~reify-method-sym ~@arglist)))

;; TODO spec
(defns unsupported! [name- _ #_t/qualified-symbol?, args _ #_indexed?, i index?] (TODO))

(let [fn|name 'the-name
      arglist '[x0 x1 x2]
      >unsupported!-form (fn [i|arg] `(unsupported! '~(uid/qualify fn|name) [~@arglist] ~i|arg))
      xs [['(.invoke overload0 x0 x1 x2)
           [{:t t/boolean? :getter '((Array/get overload-types0 0) x0) :i 0}
            {:t t/long?    :getter '((Array/get overload-types0 1) x1) :i 1}
            {:t t/boolean? :getter '((Array/get overload-types0 2) x2) :i 2}]]
          ['(.invoke overload1 x0 x1 x2)
           [{:t t/boolean? :getter '((Array/get overload-types1 0) x0) :i 0}
            {:t t/long?    :getter '((Array/get overload-types1 1) x1) :i 1}
            {:t t/object?  :getter '((Array/get overload-types1 2) x2) :i 2}]]
          ['(.invoke overload2 x0 x1 x2)
           [{:t t/byte?    :getter '((Array/get overload-types2 0) x0) :i 0}
            {:t t/long?    :getter '((Array/get overload-types2 1) x1) :i 1}
            {:t t/byte?    :getter '((Array/get overload-types2 2) x2) :i 2}]]
          ['(.invoke overload2 x0 x1 x2)
           [{:t t/byte?    :getter '((Array/get overload-types2 0) x0) :i 0}
            {:t t/boolean? :getter '((Array/get overload-types2 1) x1) :i 1}
            {:t t/byte?    :getter '((Array/get overload-types2 2) x2) :i 2}]]]
      *i|arg (atom 0)
      combinef
        (fn ([] (transient ['ifs]))
            ([ret] (-> ret (conj! (>unsupported!-form @*i|arg)) persistent! seq))
            ([ret getter x i]
              (reset! *i|arg i)
              (c/conj! ret getter x)))]
  (c/>combinatoric-tree (count arglist)
    (fn [a b] (t/= (:t a) (:t b)))
    (aritoid combinef combinef (fn [ret [{:keys [getter i]} group]] (combinef ret getter group i)))
    c/conj!|rf
    (aritoid combinef combinef (fn [ret [k [{:keys [getter i]}]]] (combinef ret getter k i)))
    xs))

(defns- >combinatoric-seq+
  [direct-dispatch-data-seq-for-arity (s/seq-of ::direct-dispatch-data)
   arglist (s/vec-of simple-symbol?)]
  (->> direct-dispatch-data-seq-for-arity
       (c/map+ (fn [{reify- :reify :keys [input-types-decl]}]
                 [(>dynamic-dispatch|reify-call reify- arglist)
                  (->> reify-
                       :overload
                       :arg-types
                       (c/map-indexed
                         (fn [i|arg arg-type]
                           {:i    i|arg
                            :t    arg-type
                            :getf `((Array/get ~(:name input-types-decl) ~i|arg)
                                     ~(get arglist i|arg))})))]))))

(defns- >dynamic-dispatch|body-for-arity
  "Assumes the elements of `direct-dispatch-data-seq-for-arity` are ordered in increasing
   generality of the input types of their respective `reify` declarations."
  [fn|name ::uss/fn|name, arglist (s/vec-of simple-symbol?)
   direct-dispatch-data-seq-for-arity (s/seq-of ::direct-dispatch-data)]
  (if (empty? arglist)
      (>dynamic-dispatch|reify-call (-> direct-dispatch-data-seq-for-arity first :reify) arglist)
      (let [combinatoric-seq+

            *i|arg (atom 0)
            combinef
              (fn ([] (transient [`ifs]))
                  ([ret]
                    (-> ret (conj! `(unsupported! '~(uid/qualify fn|name) ~arglist ~(deref *i|arg)))
                            persistent!
                            seq))
                  ([ret getf x i]
                    (reset! *i|arg i)
                    (c/conj! ret getf x)))]
        (c/>combinatoric-tree (count arglist)
          (fn [a b] (t/= (:t a) (:t b)))
          (aritoid combinef combinef (fn [x [{:keys [getf i]} group]] (combinef x getf group i)))
          c/conj!|rf
          (aritoid combinef combinef (fn [x [k [{:keys [getf i]}]]] (combinef x getf k i)))
          (>combinatoric-seq+ direct-dispatch-data-seq-for-arity arglist))))))

(defns >dynamic-dispatch-fn|form
  [{:as fn|globals :keys [fn|meta _, fn|name _]} ::fn|globals
   {:as opts       :keys [gen-gensym _, lang _]} ::opts
   direct-dispatch ::direct-dispatch]
 `(core/defn ~fn|name
    ~(assoc fn|meta :quantum.core.type/type (>form fn|type))
    ~@(->> direct-dispatch
           :direct-dispatch-data-seq
           (group-by (fn-> :reify :overload :arg-types count))
           (sort-by key) ; for purposes of reproducibility and organization
           (map (fn [[arg-ct direct-dispatch-data-seq-for-arity]]
                  (let [arglist (ufgen/gen-args 0 arg-ct "x" gen-gensym)
                        body    (>dynamic-dispatch|body-for-arity
                                  fn|name arglist direct-dispatch-data-seq-for-arity)]
                    (list arglist body)))))))

;; ===== End dynamic dispatch ===== ;;

(defns- overloads-basis>unanalyzed-overload
  [{:as in {args                      [:args    _]
            varargs                   [:varargs _]
            pre-type|form             [:pre     _]
            [_ _, output-type|form _] [:post    _]} [:arglist _]
            body-codelist|pre-analyze [:body    _]} _
   fn|output-type|form _ ; TODO excise this var when we default `output-type|form` to `?`
   fn|output-type t/type?
   > (s/seq-of ::unanalyzed-overload)]
  (when pre-type|form (TODO "Need to handle pre"))
  (when varargs       (TODO "Need to handle varargs"))
  (let [arg-types|form   (->> args (mapv (fn [{[kind #_#{:any :spec}, t #_t/form?] :spec}]
                                           (case kind :any `t/any? :spec t))))
        output-type|form (case output-type|form
                           _   `t/any?
                           ;; TODO if the output-type|form is nil then we should default to `?`;
                           ;; otherwise the `fn|output-type|form` gets analyzed over and over
                           nil fn|output-type|form
                           output-type|form)
        arg-bindings
          (->> args
               (mapv (fn [{[kind binding-] :binding-form}]
                       ;; TODO this assertion is purely temporary until destructuring is
                       ;; supported
                       (assert kind :sym)
                       binding-)))
        ;; TODO support varargs
        varargs-binding (when varargs
                          ;; TODO this assertion is purely temporary until destructuring is
                          ;; supported
                          (assert (-> varargs :binding-form first (= :sym))))
        arg-types|expanded-seq ; split and primitivized
          (->> (uana/analyze-arg-syms {} (zipmap arg-bindings arg-types|form) output-type|form)
               (c/map (fn [{:keys [env out-type-node]}]
                        (let [output-type (:type out-type-node)
                              arg-types   (->> arg-bindings (mapv #(get env %)))]
                          (when (and ;; TODO excise clause when we default `output-type|form` to `?`
                                     (not (identical? output-type|form fn|output-type|form))
                                     (not (t/<= output-type fn|output-type)))
                            (err! (str "Overload's declared output type does not satisfy function's"
                                       "overall declared output type")
                                  (kw-map output-type fn|output-type)))
                          (kw-map arg-types output-type)))))]
    (->> arg-types|expanded-seq
         (fn [{:keys [arg-types output-type]}]
           (kw-map arg-bindings varargs-binding
                   arg-types|form arg-types
                   output-type|form output-type
                   body-codelist|pre-analyze)))))

(defns unanalyzed-overloads>fn|type
  [unanalyzed-overloads (s/seq-of ::unanalyzed-overload), fn|output-type t/type? > utr/fn-type?]
  (->> unanalyzed-overloads
       (c/lmap (fn [{:keys [arg-types pre-type output-type]}]
                 (cond-> arg-types
                   pre-type    (conj :| pre-type)
                   output-type (conj :> output-type))))
       (apply t/ftype fn|output-type)))

(defns fn|code [kind #{:fn :defn}, lang ::lang, args _]
  (let [{:as args'
         :keys [:quantum.core.specs/fn|name
                :quantum.core.defnt/output-spec]
         overloads-bases :quantum.core.defnt/overloads
         fn|meta :quantum.core.specs/meta}
          (s/validate args (case kind :defn :quantum.core.defnt/defnt
                                      :fn   :quantum.core.defnt/fnt))
        gen-gensym-base      (ufgen/>reproducible-gensym|generator)
        gen-gensym           (fn [x] (symbol (str (gen-gensym-base x) "__")))
        opts                 (kw-map gen-gensym lang)
        inline?              (s/validate (:inline fn|meta) (t/? t/boolean?))
        fn|meta              (if inline?
                                 (do (ulog/pr :warn "requested `:inline`; ignoring until feature is"
                                                    "implemented")
                                     (dissoc fn|meta :inline))
                                 fn|meta)
        fn|output-type|form  (or (second output-spec) `t/any?)
        ;; TODO this needs to be analyzed for dependent types referring tp local vars
        fn|output-type       (eval fn|output-type|form)
        unanalyzed-overloads (->> overloads-bases
                                  (c/mapcat #(overloads-basis>unanalyzed-overload
                                               % fn|output-type|form fn|output-type)))
        fn|type              (unanalyzed-overloads>fn|type unanalyzed-overloads fn|output-type)
        fn|globals           (kw-map fn|name fn|meta fn|type fn|output-type|form fn|output-type)
        overloads            (->> unanalyzed-overloads
                                  (c/map #(unanalyzed-overload>overload % fn|globals opts)))
        direct-dispatch      (>direct-dispatch fn|globals opts overloads)
        fn-codelist
          (case lang
            :clj  (->> `[(declare ~fn|name) ; for recursion
                         ~@(:form direct-dispatch)
                         ~(>dynamic-dispatch-fn|form fn|globals opts direct-dispatch)]
                        (remove nil?))
            :cljs (TODO))
        code (case kind
               :fn   (TODO)
               :defn `(~'do ~@fn-codelist))]
    code))

#?(:clj
(defmacro fnt
  "With `t/fn`, protocols, interfaces, and multimethods become unnecessary. The preferred method of
   dispatch becomes the function alone.

   `t/fn` is intended to catch many runtime errors at compile time, but cannot catch all of them.

   `t/fn`, along with `t/defn`, `t/dotyped`, and others, creates a typed context in which its
   internal forms are analyzed, type-consistency is checked, and type-dispatch is resolved at
   compile time inasmuch as possible, and at runtime only when necessary.

   Within the type system, primitives are always preferred to boxed values. All values that can be
   primitives (i.e. ones that are `t/<=` w.r.t. a `(t/isa? <boxed-primitive-class>)`) are treated
   as primitives unless specifically marked otherwise with the `t/ref` metadata-adding directive.

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

   Metadata directives special to `t/fn` include:
   - `:inline` : Applicable within the metadata of `t/fn` or `t/defn`. A directive to inline the
                 function if possible.

   `fnt` only works in languages in which the metalanguage (compiler language) is the same as the
   object language. As such, for CLJS, we choose to use only a CLJS-in-CLJS / bootstrapped compiler
   even if that means alienating the mainstream CLJS-in-CLJ workflow."
  [& args] (fn|code :fn (ufeval/env-lang) args)))

#?(:clj (defmacro defn [& args] (fn|code :defn (ufeval/env-lang) args)))
