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
      :refer [reduce-2 reducei-2]]
    [quantum.untyped.core.numeric.combinatorics :as ucombo]
    [quantum.untyped.core.reducers              :as ur
      :refer [educe educei reducei]]
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

;; TODO move
(def index? #(and (integer? %) (>= % 0)))
(def count? index?)

;; ===== `t/extend-defn!` specs ===== ;;

(s/def :quantum.core.defnt/fn|extended-name qualified-symbol?)

(s/def :quantum.core.defnt/extend-defn!
  (s/and (s/spec
           (s/cat :quantum.core.defnt/fn|extended-name :quantum.core.defnt/fn|extended-name
                  :quantum.core.defnt/overloads        :quantum.core.defnt/overloads))
         (uss/fn-like|postchecks|gen :quantum.core.defnt/overloads)
         :quantum.core.defnt/postchecks))

;; ===== End `t/extend-defn!` specs ===== ;;

(defonce *fn->type (atom {}))

(defonce defnt-cache (atom {})) ; TODO For now — but maybe lock-free concurrent hash map to come

(defonce *interfaces (atom {}))

;; ==== Internal specs ===== ;;

(s/def ::lang #{:clj :cljs})

(def ^:dynamic *compilation-mode* :normal)

(s/def ::compilation-mode #{:normal :test})

(s/def ::kind #{:fn :defn :extend-defn!})

(s/def ::opts
  (s/kv {:compilation-mode ::compilation-mode
         :gen-gensym       t/fn?
         :lang             ::lang
         :kind             ::kind}))

;; "global" because they apply to the whole fnt
(s/def ::fn|globals
  (s/kv {:fn|meta             (s/nilable :quantum.core.specs/meta)
         :fn|name             ::uss/fn|name
         :fn|type             utr/fn-type?
         :fn|output-type|form t/any?
         :fn|output-type      t/type?}))

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

;; This is the overload after the input specs are split by their respective `t/or` constituents,
;; and after primitivization, but before readiness for incorporation into a `reify`.
;; One of these corresponds to one reify overload.
(s/def ::overload
  (s/kv {:arg-classes                 (s/vec-of class?)
         :arg-types                   (s/vec-of t/type?)
         :arglist-code|fn|hinted      t/any?
         :arglist-code|reify|unhinted t/any?
         :body-form                   t/any?
         :output-class                (s/nilable class?)
         :output-type                 t/type?
         :positional-args-ct          count?
         ;; When present, varargs are considered to be of class Object
         :variadic?                   t/boolean?}))

(s/def ::overload|id index?)

(s/def ::overload-types-decl
  (s/kv {:form t/any?
         :name simple-symbol?}))

(s/def ::reify
  (s/kv {:form      t/any?
         :interface class?
         :name      simple-symbol?
         :overload  ::overload}))

(s/def ::direct-dispatch-data
  (s/kv {:overload-types-decl ::overload-types-decl
         :reify               ::reify}))

(s/def ::direct-dispatch
  (s/kv {:form                     t/any?
         :direct-dispatch-data-seq (s/vec-of ::direct-dispatch-data)}))

(s/def ::types-decl-datum (s/kv {:id index? :arg-types (s/vec-of t/type?) :output-type t/type?}))

(s/def ::types-decl (s/kv {:form t/any? :name simple-symbol?}))

#_(:clj
(c/defn fnt|arg->class [lang {:as arg [k spec] ::fnt|arg-spec :keys [arg-binding]}]
  (cond (not= k :spec) java.lang.Object; default class
        (symbol? spec) (pred->class lang spec))))

;; TODO optimize such that `post-type|form` doesn't create a new type-validator wholesale every
;; time the function gets run; e.g. extern it
(c/defn >with-runtime-output-type [body output-type|form] `(t/validate ~body ~output-type|form))

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
  (let [cs  (t/type>classes t)
        cs' (disj cs nil)]
    (if (-> cs' count (not= 1))
        java.lang.Object
        (-> (first cs')
            (cond-> (and (not (contains? cs nil))
                         (not (-> t meta :quantum.core.type/ref?)))
              t/class>most-primitive-class))))))

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
   {:as opts       :keys [lang _]} ::opts
   {:as fn|globals :keys [fn|name _, fn|type _, fn|output-type _]} ::fn|globals
   > ::overload]
  (let [;; Not sure if `nil` is the right approach for the value
        recursive-ast-node-reference (uast/symbol {} fn|name nil fn|type)
        env         (->> (zipmap arg-bindings arg-types)
                         (uc/map' (c/fn [[arg-binding arg-type]]
                                    [arg-binding (uast/unbound nil arg-binding arg-type)]))
                         ;; To support recursion
                         (<- (assoc fn|name recursive-ast-node-reference)))
        arg-classes (->> arg-types (uc/map type>class))
        body|pre-analyze|with-casts
          (->> arg-classes
               (reducei (c/fn [body ^Class c i|arg]
                          (if (.isPrimitive c)
                              body
                              (let [arg-sym (get arg-bindings i|arg)]
                                `(let* [~(ufth/with-type-hint arg-sym (.getName c)) ~arg-sym]
                                   ~body))))
                 (ufgen/?wrap-do body-codelist|pre-analyze)))
        body-node   (uana/analyze env body|pre-analyze|with-casts)
        hint-arg|fn (c/fn [i arg-binding]
                      (ufth/with-type-hint arg-binding
                        (ufth/>fn-arglist-tag
                          (uc/get arg-classes i)
                          lang
                          (uc/count arg-bindings)
                          (boolean varargs-binding))))
        actual-output-type (>actual-output-type declared-output-type body-node)
        body-form
          (-> (:form body-node)
              (cond-> (-> actual-output-type meta :quantum.core.type/runtime?)
                (>with-runtime-output-type output-type|form))
              (ufth/cast-bindings|code
                (->> (uc/zipmap-into (umap/om) arg-bindings arg-classes)
                     (uc/remove-vals' (fn-or nil? (fn= java.lang.Object) t/primitive-class?)))))]
      {:arg-classes                 arg-classes
       :arg-types                   arg-types
       :arglist-code|fn|hinted      (cond-> (->> arg-bindings (uc/map-indexed hint-arg|fn))
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
  (>symbol (str (->> args-classes (uc/lmap class>interface-part-name) (str/join "+"))
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
    :keys [arg-classes _, arglist-code|reify|unhinted _, body-form _, output-class _]} ::overload
   {:as opts :keys [gen-gensym _]} ::opts
   {:keys [fn|name _]} ::fn|globals
   i|overload index?
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
        reify-name (>symbol (str fn|name "|__" i|overload))
        form `(~'def ~reify-name
                (reify* [~(-> interface >name >symbol)]
                  (~(ufth/with-type-hint reify-method-sym
                      (ufth/>arglist-embeddable-tag output-class|reify))
                    ~arglist-code ~body-form)))]
    {:form      form
     :interface interface
     :name      reify-name
     :overload  overload})))

;; ----- Type declarations ----- ;;

(defns >types-decl|name [fn|globals ::fn|globals > simple-symbol?]
  (symbol (str (:fn|name fn|globals) "|__types-decl")))

(c/defn types-decl>arg-types
  [types-decl #_(atom-of (vec-of ...)), overload-id #_::overload|id #_> #_(objects-of type?)]
  (apply uarr/*<> (:arg-types (get @types-decl overload-id))))

(c/defn types-decl>ftype
  [types-decl #_(atom-of (vec-of ...)), fn|output-type #_t/type? #_> #_(vec-of ...)]
  (->> types-decl
       deref
       (uc/lmap (c/fn [{:keys [arg-types pre-type output-type]}]
                  (cond-> arg-types
                    pre-type    (conj :| pre-type)
                    output-type (conj :> output-type))))
       (apply t/ftype fn|output-type)))

(defns >types-decl
  [{:as opts :keys [kind _]} ::opts, fn|globals ::fn|globals, overloads (s/seq-of ::overload)
   > ::types-decl]
  (let [types-sym (>types-decl|name fn|globals)
        types-decl-data
          (if (= kind :extend-defn!)
              (TODO)
              (->> overloads
                   (uc/map-indexed
                     (c/fn [i {:keys [arg-types output-type]}]
                       (kw-map i arg-types output-type)))))]
    (if (-> opts :compilation-mode (= :test))
        {:name types-sym
         :form (if (= kind :extend-defn!)
                   `(reset! ~(symbol (>name *ns*) (>name types-sym)) ~(>form types-decl-data))
                   `(def ~types-sym (atom ~(>form types-decl-data))))}
        ;; In non-test cases, it's far cheaper to not have to convert the types to a
        ;; compiler-readable form and then re-evaluate them again
        (do (if (= kind :extend-defn!)
                (reset! (var-get (ns-resolve *ns* types-sym)) types-decl-data)
                (intern (>symbol *ns*) types-sym (atom types-decl-data)))
            {:name types-sym :form nil}))))

(defns >overload-types-decl
  "The evaluated `form` of each overload-types-decl is an array of non-primitivized types that the
   dynamic dispatch uses to dispatch off input types."
  [{:as fn|globals :keys [fn|name _]} ::fn|globals arg-types (s/vec-of t/type?)
   i|overload ::overload|id > ::overload-types-decl]
  (let [decl-name (>symbol (str fn|name "|__" i|overload "|types"))
        form      `(def ~(ufth/with-type-hint decl-name "[Ljava.lang.Object;")
                        (types-decl>arg-types ~(>types-decl|name fn|globals) ~i|overload))]
    {:form form :name decl-name}))

;; ----- Direct dispatch: putting it all together ----- ;;

(defns >direct-dispatch
  [{:as opts       :keys [gen-gensym _, lang _]} ::opts
   {:as fn|globals :keys [fn|name _]} ::fn|globals
   overloads (s/vec-of ::overload)
   > ::direct-dispatch]
  (case lang
    :clj  (let [direct-dispatch-data-seq
                  (->> overloads
                       (uc/map-indexed
                         (c/fn [i|overload {:as overload :keys [arg-types]}]
                           {:overload-types-decl
                              (>overload-types-decl fn|globals arg-types i|overload)
                            :reify (overload>reify overload opts fn|globals i|overload)})))
                form (->> direct-dispatch-data-seq
                          (uc/mapcat
                            (c/fn [{:as direct-dispatch-data :keys [overload-types-decl]}]
                              (list (:form overload-types-decl)
                                    (-> direct-dispatch-data :reify :form)))))]
            (kw-map form direct-dispatch-data-seq))
    :cljs (TODO)))

;; ===== Dynamic dispatch ===== ;;

(defns- >dynamic-dispatch|reify-call [reify- ::reify, arglist (s/vec-of simple-symbol?)]
  (let [hinted-reify-sym (-> reify- :name (ufth/with-type-hint (-> reify- :interface >name)))]
    `(. ~hinted-reify-sym ~reify-method-sym ~@arglist)))

;; TODO spec
(defns unsupported! [name- _ #_t/qualified-symbol?, args _ #_indexed?, i index?]
  (throw (ex-info "This function is unsupported for the type combination at the argument index."
                  {:name name- :args args :arg-index i})))

(defns- >combinatoric-seq+
  [direct-dispatch-data-seq-for-arity (s/seq-of ::direct-dispatch-data)
   arglist (s/vec-of simple-symbol?)]
  (->> direct-dispatch-data-seq-for-arity
       (uc/map+ (c/fn [{reify- :reify :keys [overload-types-decl]}]
                  [(>dynamic-dispatch|reify-call reify- arglist)
                   (->> reify-
                        :overload
                        :arg-types
                        (uc/map-indexed
                          (c/fn [i|arg arg-type]
                            {:i    i|arg
                             :t    arg-type
                             :getf `((Array/get ~(:name overload-types-decl) ~i|arg)
                                      ~(get arglist i|arg))})))]))))

(defns- >dynamic-dispatch|body-for-arity
  "Assumes the elements of `direct-dispatch-data-seq-for-arity` are ordered in increasing
   generality of the input types of their respective `reify` declarations."
  [fn|name ::uss/fn|name, arglist (s/vec-of simple-symbol?)
   direct-dispatch-data-seq-for-arity (s/seq-of ::direct-dispatch-data)]
  (if (empty? arglist)
      (>dynamic-dispatch|reify-call (-> direct-dispatch-data-seq-for-arity first :reify) arglist)
      (let [*i|arg (atom 0)
            combinef
              (c/fn
                ([] (transient [`ifs]))
                ([ret]
                  (-> ret (conj! `(unsupported! '~(uid/qualify fn|name) ~arglist ~(deref *i|arg)))
                          persistent!
                          seq))
                ([ret getf x i]
                  (reset! *i|arg i)
                  (uc/conj! ret getf x)))]
        (uc/>combinatoric-tree (count arglist)
          (c/fn [a b] (t/= (:t a) (:t b)))
          (aritoid combinef combinef (c/fn [x [{:keys [getf i]} group]] (combinef x getf group i)))
          uc/conj!|rf
          (aritoid combinef combinef (c/fn [x [k [{:keys [getf i]}]]] (combinef x getf k i)))
          (>combinatoric-seq+ direct-dispatch-data-seq-for-arity arglist)))))

(defns- >dynamic-dispatch-fn|form
  [{:as opts       :keys [gen-gensym _, lang _]} ::opts
   {:as fn|globals :keys [fn|meta _, fn|name _, fn|output-type _]} ::fn|globals
   types-decl ::types-decl
   direct-dispatch ::direct-dispatch]
 `(c/defn ~fn|name
    ~(assoc fn|meta :quantum.core.type/type
       `(self/types-decl>ftype ~(:name types-decl) ~fn|output-type))
    ~@(->> direct-dispatch
           :direct-dispatch-data-seq
           (group-by (fn-> :reify :overload :arg-types count))
           (sort-by key) ; for purposes of reproducibility and organization
           (map (c/fn [[arg-ct direct-dispatch-data-seq-for-arity]]
                  (let [arglist (ufgen/gen-args 0 arg-ct "x" gen-gensym)
                        body    (>dynamic-dispatch|body-for-arity
                                  fn|name arglist direct-dispatch-data-seq-for-arity)]
                    (list arglist body)))))))

;; ===== End dynamic dispatch ===== ;;

;; ===== Arg type comparison ===== ;;

(c/defn compare-arg-types [t0 #_t/type?, t1 #_t/type? #_> #_ucomp/comparison?]
  (if-let [c0 (uana/sort-guide t0)]
    (if-let [c1 (uana/sort-guide t1)]
      (ifs (< c0 c1) -1 (> c0 c1) 1 0)
      -1)
    (if-let [c1 (uana/sort-guide t1)]
      1
      (uset/normalize-comparison (t/compare t0 t1)))))

(c/defn compare-args-types [arg-types0 #_(s/vec-of t/type?) arg-types1 #_(s/vec-of t/type?)]
  (let [ct-comparison (compare (count arg-types0) (count arg-types1))]
    (if (zero? ct-comparison)
        (reduce-2
          (c/fn [^long c t0 t1]
            (let [c' (long (compare-arg-types t0 t1))]
              (case c'
                -1 (case c  1 (reduced 0) c')
                 0 c
                 1 (case c -1 (reduced 0) c'))))
          0
          arg-types0 arg-types1)
        ct-comparison)))

;; TODO spec
;; TODO use!!
(c/defn assert-monotonically-increasing-types!
  "Asserts that each type in an overload of the same arity and arg-position are in monotonically
   increasing order in terms of `t/compare`.

   Since its inputs are sorted via `compare-args-types`, this only need check the last overload of
   `unanalyzed-overload-seq-accum` and the first overload of `unanalyzed-overload-seq`."
  [unanalyzed-overload-seq-accum #_(s/seq-of ::unanalyzed-overload)
   unanalyzed-overload-seq       #_(s/seq-of ::unanalyzed-overload)
   i|overload-basis              #_index?]
  (when-not (or (empty? unanalyzed-overload-seq-accum) (empty? unanalyzed-overload-seq))
    (let [prev-overload (uc/last  unanalyzed-overload-seq-accum)
          overload      (uc/first unanalyzed-overload-seq)]
      (reducei-2
        (c/fn [_ arg|type|prev arg|type i|arg]
          (when ;; NOTE could use `compare-arg-types` here instead of `t/compare` if we want a more
                ;; efficient combinatoric tree dispatch
                (= 1 (t/compare arg|type|prev arg|type))
            ;; TODO provide code context, line number, etc.
            (err! (istr "At overload ~{i|overload-basis}, arg ~{i|arg}: type is not in monotonically increasing order in terms of `t/compare`")
                  (umap/om :prev-overload prev-overload
                           :overload      overload
                           :prev-type     arg|type|prev
                           :type          arg|type))))
        (:arg-types prev-overload)
        (:arg-types overload)))))

;; ===== End arg type comparison ===== ;;

(defns- overloads-basis>unanalyzed-overload-seq
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
  (let [arg-types|form   (->> args (mapv (c/fn [{[kind #_#{:any :spec}, t #_t/form?] :spec}]
                                           (case kind :any `t/any? :spec t))))
        output-type|form (case output-type|form
                           _   `t/any?
                           ;; TODO if the output-type|form is nil then we should default to `?`;
                           ;; otherwise the `fn|output-type|form` gets analyzed over and over
                           nil fn|output-type|form
                           output-type|form)
        arg-bindings
          (->> args
               (mapv (c/fn [{[kind binding-] :binding-form}]
                       ;; TODO this assertion is purely temporary until destructuring is
                       ;; supported
                       (assert kind :sym)
                       binding-)))
        ;; TODO support varargs
        varargs-binding (when varargs
                          ;; TODO this assertion is purely temporary until destructuring is
                          ;; supported
                          (assert (-> varargs :binding-form first (= :sym))))
        arg-types|expanded-seq ; split, primitivized, and sorted
          (->> (uana/analyze-arg-syms {} (zipmap arg-bindings arg-types|form) output-type|form)
               (uc/map (c/fn [{:keys [env out-type-node]}]
                         (let [output-type (:type out-type-node)
                               arg-env     (->> env :opts :arg-env deref)
                               arg-types   (->> arg-bindings (uc/map #(:type (get arg-env %))))]

                          (when (and ;; TODO excise clause when we default `output-type|form` to `?`
                                     (not (identical? output-type|form fn|output-type|form))
                                     (not (t/<= output-type fn|output-type)))
                            (err! (str "Overload's declared output type does not satisfy function's"
                                       "overall declared output type")
                                  (kw-map output-type fn|output-type)))
                          (kw-map arg-types output-type))))
               (sort-by :arg-types compare-args-types)
               vec)]
    (uana/pr! arg-types|expanded-seq) ; TODO excise
    (->> arg-types|expanded-seq
         (uc/map (c/fn [{:keys [arg-types output-type]}]
                   (kw-map arg-bindings varargs-binding
                           arg-types|form arg-types
                           output-type|form output-type
                           body-codelist|pre-analyze))))))

(defns- overloads-bases>unanalyzed-overloads
  [overloads-bases _ #_:quantum.core.defnt/overloads
   fn|output-type|form _ ; TODO excise this var when we default `output-type|form` to `?`
   fn|output-type t/type?
   > (s/seq-of ::unanalyzed-overload)]
  (->> overloads-bases
       (uc/map+ #(overloads-basis>unanalyzed-overload-seq % fn|output-type|form fn|output-type))
       (educei
         (c/fn
           ([] [])
           ([ret] ret)
           ([ret unanalyzed-overload-seq i|overload-basis]
             (assert-monotonically-increasing-types! ret unanalyzed-overload-seq i|overload-basis)
             (ur/join ret unanalyzed-overload-seq))))))

(defns fn|code [kind ::kind, lang ::lang, compilation-mode ::compilation-mode, args _]
  (let [{:as args'
         :keys [:quantum.core.specs/fn|name
                :quantum.core.defnt/fn|extended-name
                :quantum.core.defnt/output-spec]
         overloads-bases :quantum.core.defnt/overloads
         fn|meta :quantum.core.specs/meta}
          (s/validate args (case kind :defn         :quantum.core.defnt/defnt
                                      :fn           :quantum.core.defnt/fnt
                                      :extend-defn! :quantum.core.defnt/extend-defn!))
        gen-gensym-base      (ufgen/>reproducible-gensym|generator)
        gen-gensym           (c/fn [x] (symbol (str (gen-gensym-base x) "__")))
        opts                 (kw-map compilation-mode gen-gensym kind lang)
        inline?              (s/validate (:inline fn|meta) (t/? t/boolean?))
        fn|meta              (if inline?
                                 (do (ulog/pr :warn "requested `:inline`; ignoring until feature is"
                                                    "implemented")
                                     (dissoc fn|meta :inline))
                                 fn|meta)
        fn|output-type|form  (or (second output-spec) `t/any?)
        ;; TODO this needs to be analyzed for dependent types referring tp local vars
        fn|output-type       (eval fn|output-type|form)
        unanalyzed-overloads (overloads-bases>unanalyzed-overloads
                               overloads-bases fn|output-type|form fn|output-type)
        fn|type              (unanalyzed-overloads>fn|type unanalyzed-overloads fn|output-type)
        fn|globals           (kw-map fn|name fn|meta fn|type fn|output-type|form fn|output-type)
        ;; Specifically overloads that were generated during this execution of this function
        overloads            (->> unanalyzed-overloads
                                  (uc/map #(unanalyzed-overload>overload % opts fn|globals)))
        direct-dispatch      (>direct-dispatch opts fn|globals overloads)
        types-decl           (>types-decl opts fn|globals overloads)
        fn-codelist
          (case lang
            :clj  (->> `[(declare ~fn|name) ; for recursion
                         ~@(some-> (:form types-decl) vector)
                         ~@(:form direct-dispatch)
                         ~(>dynamic-dispatch-fn|form opts fn|globals types-decl direct-dispatch)]
                        (remove nil?))
            :cljs (TODO))
        code (case kind
               :fn   (TODO)
               :defn `(~'do ~@fn-codelist))]
    code))

#?(:clj
(defmacro fn
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
  [& args] (fn|code :fn (ufeval/env-lang) *compilation-mode* args)))

#?(:clj
(defmacro defn
  "A `defn` with an empty body is like using `declare`."
  [& args] (fn|code :defn (ufeval/env-lang) *compilation-mode* args)))

#?(:clj
(defmacro extend-defn!
  [& args] (fn|code :extend-defn! (ufeval/env-lang) *compilation-mode* args)))
