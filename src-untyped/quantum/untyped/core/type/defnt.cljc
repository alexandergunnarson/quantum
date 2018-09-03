(ns quantum.untyped.core.type.defnt
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
      :refer [istr]] ; TODO use quantum.untyped.core.string/istr instead
    [quantum.untyped.core.defnt
      :refer [defns defns- fns]]
    [quantum.untyped.core.collections           :as c
      :refer [>set >vec]]
    [quantum.untyped.core.data
      :refer [kw-map]]
    [quantum.untyped.core.data.array            :as uarr]
    [quantum.untyped.core.data.map              :as umap]
    [quantum.untyped.core.data.set              :as uset]
    [quantum.untyped.core.error                 :as err
      :refer [TODO err!]]
    [quantum.untyped.core.fn
      :refer [aritoid fn-> with-do]]
    [quantum.untyped.core.form                  :as uform
      :refer [>form]]
    [quantum.untyped.core.form.evaluate         :as ufeval]
    [quantum.untyped.core.form.generate         :as ufgen]
    [quantum.untyped.core.form.type-hint        :as ufth]
    [quantum.untyped.core.identification        :as uident
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
         (r/join #{})))))

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

(defonce *fn->type (atom {}))

(defonce defnt-cache (atom {})) ; TODO For now — but maybe lock-free concurrent hash map to come

(defonce *interfaces (atom {}))

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

(s/def ::expanded-overload-groups|arg-types|split  (s/vec-of (s/vec-of t/type?)))
(s/def ::expanded-overload-groups|output-type      t/any?)
(s/def ::expanded-overload-groups|pre-type|form    t/any?)
(s/def ::expanded-overload-groups|post-type|form   t/any?)

(s/def ::expanded-overload-groups
  (s/kv {:arg-types|pre-split|form    ::expanded-overload-group|arg-types|form
         :fnt-output-type             ::expanded-overload-groups|fnt-output-type
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
(uvar/def sort-guide "for use in arity sorting, in increasing conceptual (and bit) size"
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
                            (educe (aritoid nil identity uset/union) base-classes)
                            ;; for purposes of cleanliness and reproducibility in tests
                            (sort-by sort-guide))))))
       (apply ucombo/cartesian-product)
       (c/lmap >vec))))

;; TODO spec args
#?(:clj
(defns- >expanded-overload
  "Is given `arg-classes` and `arg-types`. In order to determine the out-type, performs an analysis
   using (in part) these pieces of data, but does not use the possibly-updated `arg-types` as
   computed in the analysis. As a result, does not yet support type inference."
  [{:keys [fn|name _, arg-bindings _, arg-classes ::expanded-overload|arg-classes
           fnt|output-type _, post-type|form _
           arg-types ::expanded-overload|arg-types, body-codelist|pre-analyze _, lang ::lang
           varargs _, varargs-binding _]} _
   > ::expanded-overload]
  (let [env         (->> (zipmap arg-bindings arg-types)
                         (c/map' (fn [[arg-binding arg-type]]
                                   [arg-binding (uast/unbound nil arg-binding arg-type)]))
                         ;; To support recursion
                         (<- (assoc fn|name fnt|type)))
        analyzed    (uana/analyze env (ufgen/?wrap-do body-codelist|pre-analyze))
        arg-classes|simplest (->> arg-classes (c/map class>simplest-class))
        hint-arg|fn (fn [i arg-binding]
                      (ufth/with-type-hint arg-binding
                        (ufth/>fn-arglist-tag
                          (c/get arg-classes|simplest i)
                          lang
                          (c/count arg-bindings)
                          varargs)))
        ;; TODO this becomes an issue when `post-type|form` references local bindings
        overload-specific-post-type (some-> post-type|form eval)
        _ (when (and overload-specific-post-type
                     (not (t/<= overload-specific-post-type fnt|output-type)))
            (err! (str "Overload's specified output type does not satisfy function's overall "
                       "specified output type")))
        post-type (or overload-specific-post-type fnt|output-type)
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
                (->> (c/zipmap-into (umap/om) arg-bindings arg-classes)
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
   fn|name _, fnt|output-type _
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
                                      (apply ucombo/cartesian-product)
                                      (c/map vec))
            expanded-overload-group-seq
              (->> arg-types|recombined
                   (mapv (fn [arg-types]
                           (>expanded-overload-group
                             (kw-map fn|name arg-bindings arg-types body-codelist|pre-analyze lang
                                     arg-types|pre-split|form pre-type|form post-type|form
                                     fnt|output-type varargs varargs-binding)))))]
        (kw-map arg-types|pre-split|form pre-type|form post-type|form fnt|output-type
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
  (>symbol (str (->> args-classes (c/lmap class>interface-part-name) (str/join "+"))
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
   > ::reify|overload]
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
                                      (list* `uarr/*<> (map >form arg-type|split)))]
             (assoc (kw-map form arg-type|split) :name decl-name))))))

(def allowed-shorthand-tag-chars "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ")
(def min-shorthand-tag-length 1)
(def max-shorthand-tag-length 64) ; for now

(defn >all-shorthand-tags []
  (->> (range min-shorthand-tag-length (inc max-shorthand-tag-length))
       c/unchunk
       (c/lmap (fn [n] (apply ucombo/cartesian-product (repeat n allowed-shorthand-tag-chars))))
       c/lcat
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
      `(ifs ~@body (unsupported! (quote ~(uident/qualify fn|name)) [~@arglist] ~i|arg))))

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
                                  i-arg->input-types-decl (atom 0) i|arg)))
                            c/lcat)]
          (>dynamic-dispatch|conditional fn|name arglist i|arg branches))))
  ([fn|name ::uss/fn|name, arglist (s/vec-of simple-symbol?), reify-seq (s/vec-of ::reify)
    input-types-decl-group' (s/seq-of ::input-types-decl), *i|reify _, i|arg t/index?]
    (let [{:as input-types-decl :keys [arg-type|split]} (first input-types-decl-group')
          input-types-decl-group'' (rest input-types-decl-group')]
      (->> arg-type|split
           (c/lmap-indexed
             (fn [i|arg-type' _]
               [`((Array/get ~(:name input-types-decl) ~i|arg-type') ~(get arglist i|arg))
                (if (empty? input-types-decl-group'')
                    (with-do (>dynamic-dispatch|reify-call (get reify-seq @*i|reify) arglist)
                      ;; TODO take out this ugly bit
                      (swap! *i|reify inc))
                    (let [i|arg' (inc i|arg)
                          next-branch (>dynamic-dispatch|body-for-arity fn|name arglist reify-seq
                                        input-types-decl-group'' *i|reify i|arg')]
                      (>dynamic-dispatch|conditional fn|name arglist i|arg' next-branch)))]))
           c/lcat))))

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
                :quantum.core.defnt/output-spec
                :quantum.core.specs/meta] :as args'}
          (s/validate args (case kind :defn :quantum.core.defnt/defnt
                                      :fn   :quantum.core.defnt/fnt))
        symbolic-analysis? false ; TODO parameterize this
        fnt-output-type (or (some-> output-spec second eval) t/any?)
        gen-gensym-base (ufgen/>reproducible-gensym|generator)
        gen-gensym (fn [x] (symbol (str (gen-gensym-base x) "__")))
        inline? (s/validate (-> fn|name core/meta :inline) (t/? t/boolean?))
        fn|name (if inline?
                    (do (ulog/pr :warn "requested `:inline`; ignoring until feature is implemented")
                        (update-meta fn|name dissoc :inline))
                    fn|name)
        expanded-overload-groups-by-fnt-overload
          (->> overloads (mapv #(fnt|overload-data>expanded-overload-groups %
                                  fn|name
                                  fnt-output-type
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
