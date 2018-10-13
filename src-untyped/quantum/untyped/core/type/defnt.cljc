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
      :refer [>name >?namespace >symbol]]
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

(s/def :quantum.core.defnt/fn|extended-name symbol?)

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
         :fn|ns-name          simple-symbol?
         :fn|name             ::uss/fn|name
         :fn|type             utr/fn-type?
         :fn|types-decl-name  simple-symbol?
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

(s/def ::reify|name simple-symbol?) ; hinted with the interface name

(s/def ::reify
  (s/kv {:form      t/any?
         :interface class?
         :name      ::reify|name
         :overload  ::overload}))

(s/def ::direct-dispatch-data
  (s/kv {:overload-types-decl ::overload-types-decl
         :reify               ::reify}))

(s/def ::direct-dispatch
  (s/kv {:form                     t/any?
         :direct-dispatch-data-seq (s/vec-of ::direct-dispatch-data)}))

(s/def ::types-decl-datum
  (s/kv {:id          ::overload|id
         :ns-sym      simple-symbol?
         :arg-types   (s/vec-of t/type?)
         :output-type t/type?}))

(s/def ::indexed-types-decl-datum
  (s/kv {:id          ::overload|id
         :ns-sym      simple-symbol?
         :arg-types   (s/vec-of t/type?)
         :output-type t/type?
         :index       index? ; overload-index (position in the overall types-decl)
         :overload    ::overload}))

(s/def ::types-decl
  (s/kv {:name                 simple-symbol?
         :form                 t/any?
         ;; Sorted by overload-index
         :data                 (s/vec-of ::types-decl-datum)
         ;; Sorted by overload-index
         :indexed-data (s/vec-of ::indexed-types-decl-datum)
         :first-current-overload-id ::overload|id}))

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
  [{:as unanalyzed-overload
    :keys [arg-bindings _, varargs-binding _, arg-types _, output-type|form _
           body-codelist|pre-analyze _]
    declared-output-type [:output-type _]}
   ::unanalyzed-overload
   {:as opts       :keys [lang _, kind _]} ::opts
   {:as fn|globals :keys [fn|name _, fn|type _, fn|output-type _]} ::fn|globals
   > ::overload]
  (let [;; Not sure if `nil` is the right approach for the value
        recursive-ast-node-reference
          (when-not (= kind :extend-defn!) (uast/symbol {} fn|name nil fn|type))
        env         (->> (zipmap arg-bindings arg-types)
                         (uc/map' (c/fn [[arg-binding arg-type]]
                                    [arg-binding (uast/unbound nil arg-binding arg-type)]))
                         ;; To support recursion
                         (<- (cond-> (not= kind :extend-defn!)
                                     (assoc fn|name recursive-ast-node-reference))))
        arg-classes (->> arg-types (uc/map type>class))
        body-node   (uana/analyze env (ufgen/?wrap-do body-codelist|pre-analyze))
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
                (>with-runtime-output-type output-type|form)))]
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
                  (~(ufth/with-type-hint reify-method-sym
                      (ufth/>arglist-embeddable-tag output-class|reify))
                    ~arglist-code ~body-form)))]
    {:form      form
     :interface interface
     :name      reify|name
     :overload  overload})))

;; ----- Type declarations ----- ;;

(defns >types-decl-ref [{:keys [fn|ns-name _, fn|types-decl-name _]} ::fn|globals]
  (var-get (resolve (uid/qualify fn|ns-name fn|types-decl-name))))

(c/defn types-decl>arg-types
  [*types-decl #_(atom-of (vec-of ::types-decl-datum)), overload-index #_index?
   #_> #_(objects-of type?)]
  (apply uarr/*<> (:arg-types (get @*types-decl overload-index))))

(c/defn type-data>ftype [type-data #_(vec-of ::types-decl-datum), fn|output-type #_t/type?]
  (->> type-data
       (uc/lmap (c/fn [{:keys [arg-types pre-type output-type]}]
             (cond-> arg-types
               pre-type    (conj :| pre-type)
               output-type (conj :> output-type))))
       (apply t/ftype fn|output-type)))

(c/defn types-decl>ftype
  [*types-decl #_(atom-of (vec-of ::types-decl-datum)), fn|output-type #_t/type? #_> #_(vec-of ...)]
  (type-data>ftype @*types-decl fn|output-type))

(c/defn- dedupe-types-decl-data [fn|ns-name fn|name types-decl-data]
  (reduce (let [*prev-datum (volatile! nil)]
            (c/fn [data {:as datum :keys [arg-types]}]
              (with-do
                (ifs (nil? @*prev-datum)
                       (conj data datum)
                     (= uset/=ident (utcomp/compare-inputs
                                      (:arg-types @*prev-datum) arg-types))
                       (do (ulog/ppr :warn (str "Overwriting type overload for `"
                                                 (uid/qualify fn|ns-name fn|name) "`; arg types:")
                                           arg-types)
                           (-> data pop (conj (assoc @*prev-datum :ns-sym   (:ns-sym   datum)
                                                                  :overload (:overload datum)))))
                     (conj data datum))
                (vreset! *prev-datum datum))))
          []
          types-decl-data))

(defns- >types-decl
  [{:as opts       :keys [kind _]} ::opts
   {:as fn|globals :keys [fn|ns-name _, fn|name _ fn|types-decl-name _]} ::fn|globals
   overloads (s/vec-of ::overload)
   > ::types-decl]
  (let [types-decl-existing-data (when (= kind :extend-defn!) (deref (>types-decl-ref fn|globals)))
        first-current-overload-id
          (if (= kind :extend-defn!)
              (count types-decl-existing-data)
              0)
        types-decl-current-data ; i.e. being created right now, not swapped into the types decl atom
          (->> overloads
               (uc/map-indexed
                 (c/fn [i {:keys [arg-types output-type]}]
                   {:id          (+ i first-current-overload-id)
                    :ns-sym      (ns-name *ns*)
                    :arg-types   arg-types
                    :output-type output-type})))
        ;; We can't just concat the currently-being-created overloads' type-decl data with the
        ;; existing type-decl data because we need to maintain the type-decl data's ordering by
        ;; type-specificity so the dynamic dispatch works correctly.
        types-decl-indexed-data
          (if (= kind :extend-defn!)
              (->> (ur/join types-decl-current-data types-decl-existing-data)
                   (uc/map
                     (c/fn [{:as datum :keys [id]}]
                       (assoc datum :overload (get overloads (- id first-current-overload-id)))))
                   ;; TODO here `extend-defn!` should probably:
                   ;; - Use `assert-monotonically-increasing-types!`
                   (sort-by identity
                     (c/fn [datum0 datum1]
                       (let [c (compare-args-types (:arg-types datum0) (:arg-types datum1))]
                          ;; In order to make the earlier ID appear
                         (if (zero? c)
                             (if (:overload datum0)
                                 (if (:overload datum1)  c 1)
                                 (if (:overload datum1) -1 c))
                             c))))
                   (dedupe-types-decl-data fn|ns-name fn|name)
                   (uc/map-indexed (c/fn [i datum] (assoc datum :index i))))
              (->> types-decl-current-data
                   (uc/map-indexed
                     (c/fn [i datum] (assoc datum :index i :overload (get overloads i))))))
        types-decl-data
          (if (= kind :extend-defn!)
              (->> types-decl-indexed-data (uc/map #(dissoc % :index :overload)))
              types-decl-current-data)]
    (if (-> opts :compilation-mode (= :test))
        {:name fn|types-decl-name
         :form (if (= kind :extend-defn!)
                   `(reset! ~(uid/qualify fn|ns-name fn|types-decl-name) ~(>form types-decl-data))
                   `(def ~fn|types-decl-name (atom ~(>form types-decl-data))))
         :data types-decl-data
         :indexed-data types-decl-indexed-data}
        ;; In non-test cases, it's far cheaper to not have to convert the types to a
        ;; compiler-readable form and then re-evaluate them again
        (do (if (= kind :extend-defn!)
                (reset! (>types-decl-ref fn|globals) types-decl-data)
                (intern (>symbol *ns*) fn|types-decl-name (atom types-decl-data)))
            {:name         fn|types-decl-name
             :form         nil
             :data         types-decl-data
             :indexed-data types-decl-indexed-data}))))

(defns- >overload-types-decl|name
  ([fn|name simple-symbol?, overload|id ::overload|id > simple-symbol?]
    (symbol (str fn|name "|__" overload|id "|types")))
  ([fn|ns-name simple-symbol?, fn|name simple-symbol?, overload|id ::overload|id
    > qualified-symbol?]
    (symbol (name fn|ns-name) (str fn|name "|__" overload|id "|types"))))

(defns- >overload-types-decl
  "The evaluated `form` of each overload-types-decl is an array of non-primitivized types that the
   dynamic dispatch uses to dispatch off input types."
  [{:as fn|globals :keys [fn|ns-name _, fn|name _, fn|types-decl-name _]} ::fn|globals
   arg-types (s/vec-of t/type?), overload|id ::overload|id, overload-index index?
   > ::overload-types-decl]
  (let [decl-name (>overload-types-decl|name fn|name overload|id)
        form      `(def ~(ufth/with-type-hint decl-name "[Ljava.lang.Object;")
                        (types-decl>arg-types
                          ~(uid/qualify fn|ns-name fn|types-decl-name) ~overload-index))]
    {:form form :name decl-name}))

;; ----- Direct dispatch: putting it all together ----- ;;

(defns- >direct-dispatch
  [{:as opts       :keys [gen-gensym _, lang _, kind _]} ::opts
   {:as fn|globals :keys [fn|name _]} ::fn|globals
   overloads (s/vec-of ::overload)
   types-decl ::types-decl > ::direct-dispatch]
  (case lang
    :clj  (let [direct-dispatch-data-seq
                  (->> types-decl
                       :indexed-data
                       (uc/filter+ :overload) ; i.e. the "current" ones
                       (uc/map
                         (c/fn [{:as indexed-type-decl-datum :keys [arg-types id index overload]}]
                           {:overload-types-decl
                              (>overload-types-decl fn|globals arg-types id index)
                            :reify (overload>reify overload opts fn|globals id)})))
                form (->> direct-dispatch-data-seq
                          (uc/mapcat
                            (c/fn [{:as direct-dispatch-data :keys [overload-types-decl]}]
                              [(:form overload-types-decl)
                               (-> direct-dispatch-data :reify :form)])))]
            (kw-map form direct-dispatch-data-seq))
    :cljs (TODO)))

;; ===== Dynamic dispatch ===== ;;

(defns- >dynamic-dispatch|reify-call
  [reify|name|qualified qualified-symbol?, arglist (s/vec-of simple-symbol?)]
  `(. ~reify|name|qualified ~reify-method-sym ~@arglist))

;; TODO spec
(defns unsupported! [name- _ #_t/qualified-symbol?, args _ #_indexed?, i index?]
  (throw (ex-info "This function is unsupported for the type combination at the argument index."
                  {:name name- :args args :arg-index i})))

(defns- >combinatoric-seq+
  [{:as fn|globals :keys [fn|ns-name _ fn|name _]} ::fn|globals
   indexed-types-decl-data-for-arity (s/vec-of ::indexed-types-decl-datum)
   arglist (s/vec-of simple-symbol?)]
  (->> indexed-types-decl-data-for-arity
       (uc/map+
         (c/fn [{:as types-decl-datum :keys [arg-types ns-sym overload]}]
          (let [overload|id (:id types-decl-datum)
                overload-types-decl|name
                  (>overload-types-decl|name ns-sym fn|name overload|id)
                reify|name|qualified (>reify-name-unhinted ns-sym fn|name overload|id)]
            [(>dynamic-dispatch|reify-call reify|name|qualified arglist)
             (->> arg-types
                  (uc/map-indexed
                    (c/fn [i|arg arg-type]
                      {:i    i|arg
                       :t    arg-type
                       :getf `((Array/get ~overload-types-decl|name ~i|arg)
                                ~(get arglist i|arg))})))])))))

(defns- >dynamic-dispatch|body-for-arity
  [{:as fn|globals :keys [fn|ns-name _, fn|name _]} ::fn|globals
   indexed-types-decl-data-for-arity (s/vec-of ::indexed-types-decl-datum)
   arglist (s/vec-of simple-symbol?)]
  (if (empty? arglist)
      (let [overload|id (-> indexed-types-decl-data-for-arity first :id)]
        (>dynamic-dispatch|reify-call
          (>reify-name-unhinted fn|ns-name fn|name overload|id) arglist))
      (let [*i|arg (atom 0)
            combinef
              (c/fn
                ([] (transient [`ifs]))
                ([ret]
                  (-> ret (conj! `(unsupported! '~(uid/qualify fn|ns-name fn|name)
                                                ~arglist ~(deref *i|arg)))
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
          (>combinatoric-seq+ fn|globals indexed-types-decl-data-for-arity arglist)))))

(defns- >dynamic-dispatch-fn|form
  [{:as opts       :keys [gen-gensym _, lang _, kind _]} ::opts
   {:as fn|globals :keys [fn|meta _, fn|ns-name _, fn|name _, fn|output-type _
                          fn|types-decl-name _]} ::fn|globals
   types-decl ::types-decl]
 (let [overload-forms
         (->> types-decl
              :indexed-data
              (group-by (fn-> :arg-types count))
              (sort-by key) ; for purposes of reproducibility and organization
              (map (c/fn [[arg-ct indexed-types-decl-data-for-arity]]
                     (let [arglist (ufgen/gen-args 0 arg-ct "x" gen-gensym)
                           body    (>dynamic-dispatch|body-for-arity
                                     fn|globals indexed-types-decl-data-for-arity arglist)]
                       (list arglist body)))))
       ftype-form `(types-decl>ftype ~(uid/qualify fn|ns-name fn|types-decl-name)
                                     ~(>form fn|output-type))]
  (if (= kind :extend-defn!)
     `(intern (quote ~fn|ns-name)
        (with-meta (quote ~fn|name)
          ;; TODO determine whether CLJS needs (update-in m [:jsdoc] conj "@param {...*} var_args")
          (assoc (meta (var ~(uid/qualify fn|ns-name fn|name)))
                 :quantum.core.type/type ~ftype-form))
        (fn* ~@overload-forms))
     `(c/defn ~fn|name ~(assoc fn|meta :quantum.core.type/type ftype-form) ~@overload-forms))))

;; ===== End dynamic dispatch ===== ;;

(defns- overloads-basis>unanalyzed-overload-seq
  [{:as in {args                      [:args    _]
            varargs                   [:varargs _]
            pre-type|form             [:pre     _]
            [_ _, output-type|form _] [:post    _]} [:arglist _]
            body-codelist|pre-analyze [:body    _]} _
   kind ::kind
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
        arg-types|expanded-seq ; split, primitivized, and (if not `extend-defn!`) sorted
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
               ;; Not performed with `extend-defn!` because sorting happens later, in `>types-decl`
               (<- (cond->> (not= kind :extend-defn!) (sort-by :arg-types compare-args-types)))
               vec)]
    (->> arg-types|expanded-seq
         (uc/map (c/fn [{:keys [arg-types output-type]}]
                   (kw-map arg-bindings varargs-binding
                           arg-types|form arg-types
                           output-type|form output-type
                           body-codelist|pre-analyze))))))

(defns- overloads-bases>unanalyzed-overloads
  [overloads-bases _ #_:quantum.core.defnt/overloads
   kind ::kind
   fn|output-type|form _ ; TODO excise this var when we default `output-type|form` to `?`
   fn|output-type t/type?
   > (s/seq-of ::unanalyzed-overload)]
  (->> overloads-bases
       (uc/map+
         #(overloads-basis>unanalyzed-overload-seq % kind fn|output-type|form fn|output-type))
       (educei
         (c/fn
           ([] [])
           ([ret] ret)
           ([ret unanalyzed-overload-seq i|overload-basis]
             (when-not (= kind :extend-defn!)
               ;; Because this assertion is performed later on in `>types-decl`
               (assert-monotonically-increasing-types!
                 ret unanalyzed-overload-seq i|overload-basis))
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
        fn|ns-name           (if (= kind :extend-defn!)
                                 (-> (uvar/resolve *ns* fn|extended-name) >?namespace symbol)
                                 (>symbol *ns*))
        fn|name              (if (= kind :extend-defn!)
                                 (-> fn|extended-name >name symbol)
                                 fn|name)
        fn|var               (when (= kind :extend-defn!)
                               (if-let [v (uvar/resolve *ns* fn|extended-name)]
                                 v
                                 (err! "Cannot extend a `t/defn` that has not been defined"
                                       {:sym fn|extended-name})))
        inline?              (-> (if (= kind :extend-defn!)
                                     (-> fn|var meta :inline)
                                     (:inline fn|meta))
                                 (s/validate (t/? t/boolean?)))
        fn|meta              (if inline?
                                 (do (ulog/pr :warn
                                       "requested `:inline`; ignoring until feature is implemented")
                                     (dissoc fn|meta :inline))
                                 fn|meta)
        fn|output-type|form  (or (second output-spec) `t/any?)
        ;; TODO this needs to be analyzed for dependent types referring to local vars
        fn|output-type       (eval fn|output-type|form)]
        (println "overloads-bases" overloads-bases)
    (if (empty? overloads-bases)
        `(declare ~(with-meta fn|name
                     (assoc fn|meta :quantum.core.type/type `(t/ftype ~(>form fn|output-type)))))
        (let [gen-gensym-base      (ufgen/>reproducible-gensym|generator)
              gen-gensym           (c/fn [x] (symbol (str (gen-gensym-base x) "__")))
              opts                 (kw-map compilation-mode gen-gensym kind lang)
              unanalyzed-overloads (overloads-bases>unanalyzed-overloads
                                     overloads-bases kind fn|output-type|form fn|output-type)
              fn|type              (type-data>ftype unanalyzed-overloads fn|output-type)
              fn|types-decl-name   (symbol (str fn|name "|__types"))
              fn|globals           (kw-map fn|ns-name fn|name fn|meta fn|type fn|output-type|form
                                           fn|output-type fn|types-decl-name)
              ;; Specifically overloads that were generated during this execution of this function
              overloads            (->> unanalyzed-overloads
                                        (uc/map #(unanalyzed-overload>overload % opts fn|globals)))
              types-decl           (>types-decl opts fn|globals overloads)
              direct-dispatch      (>direct-dispatch opts fn|globals overloads types-decl)
              dynamic-dispatch     (>dynamic-dispatch-fn|form opts fn|globals types-decl)
              fn-codelist
                (->> `[~@(when (not= kind :extend-defn!) [`(declare ~fn|name)]) ; For recursion
                       ~@(some-> (:form types-decl) vector)
                       ~@(:form direct-dispatch)
                       ~dynamic-dispatch]
                       (remove nil?))]
          (case kind
            :fn                   (TODO)
            (:defn :extend-defn!) `(do ~@fn-codelist))))))

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

   Metadata directives special to `t/fn`/`t/defn` include:
   - `:inline` : If `true` and attached as metadata to the arglist of an overload, will cause that
                 overload to be inlined if possible.
                 - Example: `(t/defn abc (^:inline [] ...))`
                 If `true` and attached as metadata to the whole `t/defn` or `t/fn`, will cause
                 every one of its overloads to be inlined if possible. Overloads added to a `t/defn`
                 with `:inline` `true` will inherit this inline directive.
                 - Example: `(t/defn ^:inline abc ([] ...) ([...] ...))`
                 Note that inlining is possible only in typed contexts.

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
