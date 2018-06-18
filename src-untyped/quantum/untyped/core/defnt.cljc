(ns quantum.untyped.core.defnt
    "Primarily for `(de)fns`."
    (:refer-clojure :exclude [any? ident? qualified-keyword? seqable? simple-symbol?])
    (:require
      [#?(:clj clojure.spec.alpha     :cljs cljs.spec.alpha)     :as s]
      [#?(:clj clojure.spec.gen.alpha :cljs cljs.spec.gen.alpha) :as gen]
      [quantum.untyped.core.convert       :as uconv]
      [quantum.untyped.core.data.map
        :refer [om]]
      [quantum.untyped.core.form.evaluate :as ufeval]
      [quantum.untyped.core.loops
        :refer [reduce-2]]
      [quantum.untyped.core.reducers      :as ur]
      [quantum.untyped.core.spec          :as us]
      [quantum.untyped.core.specs]
      [quantum.untyped.core.type.predicates
        :refer [any? ident? qualified-keyword? seqable? simple-symbol?]])
#?(:cljs
    (:require-macros
      [quantum.untyped.core.defnt :as this])))

;; ===== Specs ===== ;;

(s/def :quantum.core.defnt/local-name
  (s/and simple-symbol? (complement #{'& '| '>})))

(s/def :quantum.core.defnt/spec
  (s/alt :any   #{'_}
         :spec   any?))

;; ----- General destructuring ----- ;;

(s/def :quantum.core.defnt/binding-form
  (s/alt :sym :quantum.core.defnt/local-name
         :seq :quantum.core.defnt/seq-binding-form
         :map :quantum.core.defnt/map-binding-form))

(s/def :quantum.core.defnt/speced-binding
  (s/cat :binding-form :quantum.core.defnt/binding-form
         :spec         :quantum.core.defnt/spec))

;; ----- Sequential destructuring ----- ;;

(s/def :quantum.core.defnt/seq-binding-form
  (s/and vector?
         (s/cat :elems (s/* :quantum.core.defnt/speced-binding)
                :rest  (s/? (s/cat :amp #{'&}  :form :quantum.core.defnt/speced-binding))
                :as    (s/? (s/cat :as  #{:as} :sym  :quantum.core.defnt/local-name)))))

;; ----- Map destructuring ----- ;;

(defn- >keys|syms|strs [spec]
  (s/and vector?
    (s/spec (s/* (s/cat :binding-form spec
                        :spec         :quantum.core.defnt/spec)))))

(s/def :quantum.core.defnt/keys (>keys|syms|strs ident?))
(s/def :quantum.core.defnt/syms (>keys|syms|strs symbol?))
(s/def :quantum.core.defnt/strs (>keys|syms|strs simple-symbol?))

(s/def :quantum.core.defnt/or   :quantum.core.specs/or)
(s/def :quantum.core.defnt/as   :quantum.core.defnt/local-name)

(s/def :quantum.core.defnt/map-special-binding
  (s/keys :opt-un [:quantum.core.defnt/as   :quantum.core.defnt/or
                   :quantum.core.defnt/keys :quantum.core.defnt/syms :quantum.core.defnt/strs]))

(s/def :quantum.core.defnt/map-binding
  (s/spec (s/cat :binding-form :quantum.core.defnt/binding-form
                 :key+spec     (s/spec (s/cat :key any? :spec :quantum.core.defnt/spec)))))

(s/def :quantum.core.defnt/ns-keys
  (s/tuple
    (s/and qualified-keyword? #(-> % name #{"keys" "syms"}))
    (>keys|syms|strs simple-symbol?)))

(s/def :quantum.core.defnt/map-binding-form
  (s/and :quantum.core.defnt/map-special-binding
         (s/coll-of (s/or :map-binding :quantum.core.defnt/map-binding
                          :ns-keys     :quantum.core.defnt/ns-keys
                          :special     (s/tuple #{:as :or :keys :syms :strs} any?)) :into {})))

;; ----- Args ----- ;;

(s/def :quantum.core.defnt/output-spec
  (s/? (s/cat :sym #(= % '>) :spec :quantum.core.defnt/spec)))

(s/def :quantum.core.defnt/arglist
  (s/and vector?
         (s/spec
           (s/cat :args    (s/* :quantum.core.defnt/speced-binding)
                  :varargs (s/? (s/cat :sym            #(= % '&)
                                       :speced-binding :quantum.core.defnt/speced-binding))
                  :pre     (s/? (s/cat :sym            #(= % '|)
                                       :spec           (s/or :any-spec #{'_} :spec any?)))
                  :post    :quantum.core.defnt/output-spec))
         (s/conformer
           #(cond-> % (contains? % :varargs) (update :varargs :speced-binding)
                      (contains? % :pre    ) (update :pre     :spec)
                      (contains? % :post   ) (update :post    :spec)))
         (fn [{:keys [args varargs]}]
           ;; so `env` in `fnt` can work properly in the analysis
           ;; TODO need to adjust for destructuring
           (distinct?
             (concat (map :binding-form args)
                     [(:binding-form varargs)])))))

(s/def :quantum.core.defnt/body (s/alt :body (s/* any?)))

(s/def :quantum.core.defnt/arglist+body
  (s/cat :arglist :quantum.core.defnt/arglist
         :body    :quantum.core.defnt/body))

(s/def :quantum.core.defnt/overloads
  (s/alt :overload-1 :quantum.core.defnt/arglist+body
         :overload-n (s/cat :overloads (s/+ (s/spec :quantum.core.defnt/arglist+body)))))

(s/def :quantum.core.defnt/postchecks
  (s/conformer
    (fn [f]
      (-> f (update :overloads
              #(mapv (fn [overload]
                          (let [overload' (update overload :body :body)]
                            (if-let [output-spec (-> f :output-spec :spec)]
                              (do (us/assert-conform nil? (-> overload' :arglist :post))
                                  (assoc-in overload' [:arglist :post] output-spec))
                              overload'))) %))
            (dissoc :output-spec)))))

(s/def :quantum.core.defnt/fnt
  (s/and (s/spec
           (s/cat
             :quantum.core.specs/fn|name   (s/? :quantum.core.specs/fn|name)
             :quantum.core.specs/docstring (s/? :quantum.core.specs/docstring)
             :pre-meta                     (s/? :quantum.core.specs/meta)
             :output-spec                  :quantum.core.defnt/output-spec
             :overloads                    :quantum.core.defnt/overloads))
         :quantum.core.specs/fn|postchecks
         :quantum.core.defnt/postchecks))

(s/def :quantum.core.defnt/fns|code :quantum.core.defnt/fnt)

(s/def :quantum.core.defnt/defnt
  (s/and (s/spec
           (s/cat
             :quantum.core.specs/fn|name   :quantum.core.specs/fn|name
             :quantum.core.specs/docstring (s/? :quantum.core.specs/docstring)
             :pre-meta                     (s/? :quantum.core.specs/meta)
             :output-spec                  :quantum.core.defnt/output-spec
             :overloads                    :quantum.core.defnt/overloads))
         :quantum.core.specs/fn|postchecks
         :quantum.core.defnt/postchecks))

(s/def :quantum.core.defnt/defns|code :quantum.core.defnt/defnt)

(s/def :quantum.core.defnt/binding-form
  (s/alt :sym :quantum.core.defnt/local-name
         :seq :quantum.core.defnt/seq-binding-form
         :map :quantum.core.defnt/map-binding-form))

;; ===== Implementation ===== ;;

(defn- qualify-spec [lang sym]
  (symbol (name (case :clj 'clojure.spec.alpha :cljs 'cljs.spec.alpha))
          (name sym)))

(defn >seq-destructuring-spec
  "Creates a spec that performs seq destructuring, and provides a default generator for such based
   on the generators of the destructured args."
  [positional-destructurer most-complex-positional-destructurer kv-spec or|conformer seq-spec
   {:as opts generate-from-seq-spec? :gen?}]
  (let [or|unformer (s/conformer second)
        most-complex-positional-destructurer|unformer
          (s/conformer (fn [x] (s/unform most-complex-positional-destructurer x)))]
    (cond->
      (s/and seq-spec
             (s/conformer (fn [xs] {:xs xs :xs|destructured xs}))
             (us/kv {:xs|destructured (s/and positional-destructurer
                                             or|unformer
                                             kv-spec)})
             (s/conformer (fn [m] (assoc m :xs|positionally-destructured|ct
                                           (when-not (-> m :xs|destructured (contains? :varargs))
                                             (-> m :xs|destructured count)))))
             (us/kv {:xs|destructured
                      (s/and or|conformer
                             (s/conformer (fn [x] (s/unform positional-destructurer x))))})
             (s/conformer (fn [{:keys [xs xs|destructured xs|positionally-destructured|ct]}]
                            (if xs|positionally-destructured|ct
                                (concat xs|destructured (drop xs|positionally-destructured|ct xs))
                                xs|destructured))))
      (not generate-from-seq-spec?)
      (s/with-gen
        #(->> (s/gen kv-spec)
              (gen/fmap (fn [x] (s/conform most-complex-positional-destructurer|unformer x))))))))

#?(:clj
(defmacro seq-destructure
  "If `generate-from-seq-spec?` is true, generates from `seq-spec`'s generator instead of the
   default generation strategy based on the generators of the destructured args."
  [lang seq-spec #_any? args #_(s/* (s/cat :k keyword? :spec any?))
   & [varargs #_(s/nilable (s/cat :k keyword? :spec any?))]]
  (let [opts    (meta seq-spec)
        args    (us/assert-conform (s/* (s/cat :k keyword? :spec any?)) args)
        varargs (us/assert-conform (s/nilable (s/cat :k keyword? :spec any?)) varargs)
        args-ct>args-kw #(keyword (str "args-" %))
        arity>cat (fn [arg-i]
                   `(~(qualify-spec lang 'cat)
                      ~@(->> args (take arg-i)
                             (map (fn [{:keys [k spec]}] [k `any?]))
                             (apply concat))))
        most-complex-positional-destructurer-sym (gensym "most-complex-positional-destructurer")]
   `(let [~most-complex-positional-destructurer-sym
            (~(qualify-spec lang 'cat)
               ~@(->> args
                      (map (fn [{:keys [k]}] [k `any?]))
                      (apply concat))
               ~@(when varargs [(:k varargs)
                                `(~(qualify-spec lang '&)
                                   (~(qualify-spec lang '+) any?)
                                   (~(qualify-spec lang 'conformer) seq identity))]))
          positional-destructurer#
            (~(qualify-spec lang 'or) :args-0 (~(qualify-spec lang 'cat))
              ~@(->> (range (count args))
                     (map (fn [i] [(args-ct>args-kw (inc i)) (arity>cat (inc i))]))
                     (apply concat))
              ~@(when varargs [:varargs most-complex-positional-destructurer-sym]))
          kv-spec#
            (us/kv (om ~@(apply concat
                           (cond-> (->> args (map (fn [{:keys [k spec]}] [k spec])))
                             varargs (concat [[(:k varargs) (:spec varargs)]])))))
          or|conformer#
            (~(qualify-spec lang 'conformer)
              (fn or|conformer# [m#]
                [(case (count m#)
                    ~@(->> (range (inc (count args)))
                           (map (juxt identity args-ct>args-kw))
                           (apply concat))
                    ~@(when varargs [:varargs]))
                 m#]))]
      (>seq-destructuring-spec positional-destructurer# ~most-complex-positional-destructurer-sym
        kv-spec# or|conformer# ~seq-spec ~opts)))))

#?(:clj
(defmacro map-destructure [lang map-spec #_any? kv-specs #_(s/map-of any? any?)]
  (let [kv-spec-sym (gensym "kv-spec")
        {:as opts generate-from-map-spec? :gen?} (meta map-spec)]
    `(let [~kv-spec-sym (us/kv ~kv-specs)]
       ~(if generate-from-map-spec?
            `(~(qualify-spec lang 'and) ~map-spec ~kv-spec-sym)
            `(~(qualify-spec lang 'with-gen) (~(qualify-spec lang 'and) ~map-spec ~kv-spec-sym)
               (fn [] (~(qualify-spec lang 'gen) ~kv-spec-sym))))))))

(defn speced-binding>binding [{[kind binding-] :binding-form} #_:quantum.core.defnt/speced-binding]
  (case kind
    :sym binding-
    :seq (let [{:keys [as elems] rest- :rest} binding-]
           (cond-> (mapv speced-binding>binding elems)
                   rest- (conj '&  (-> rest- :form speced-binding>binding))
                   as    (conj :as (:sym as))))
    :map (->> binding-
              (map (fn [[k v]]
                     (case k
                       :as                 [k (second v)]
                       :or                 [k v]
                       (:keys :syms :strs) [k (->> v second (mapv :binding-form))]
                       [(speced-binding>binding v)
                        (get-in v [:key+spec :key])])))
              (into {}))))

(defn speced-binding>arg-ident
  [{[kind binding-] :binding-form} #_:quantum.core.defnt/speced-binding & [i|arg] #_(? nneg-integer?)]
  (uconv/>keyword
    (case kind
      :sym binding-
      (:seq :map)
        (let [ks (if (= kind :seq) [:as :sym] [:as 1])]
          (or (get-in binding- ks)
              (gensym (if i|arg (str "arg-" i|arg "-") "varargs")))))))

(declare speced-binding>spec)

(defn- speced-binding|seq>spec
  [lang {:as speced-binding [kind binding-] :binding-form [spec-kind spec] :spec}]
  `(seq-destructure ~lang ~(if (= spec-kind :spec) spec `seqable?)
    ~(->> binding- :elems
          (map-indexed
            (fn [i|arg arg|speced-binding]
              [(speced-binding>arg-ident arg|speced-binding i|arg)
               (speced-binding>spec lang arg|speced-binding)]))
          (apply concat)
          vec)
    ~@(when-let [varargs|speced-binding (get-in binding- [:rest :form])]
        [[(speced-binding>arg-ident varargs|speced-binding)
          (speced-binding>spec lang varargs|speced-binding)]])))

(defn- keys||strs||syms>key-specs [kind #_#{:keys :strs :syms} speced-bindings]
  (let [binding-form>key
          (case kind :keys uconv/>keyword :strs name :syms identity)]
    (->> speced-bindings
         (filter (fn [{[spec-kind _] :spec}] (= spec-kind :spec)))
         (map (fn [{:keys [binding-form #_symbol?] [_ spec] :spec}]
                [(binding-form>key binding-form) spec])))))

(defn- speced-binding|map>spec
  [lang {:as speced-binding [kind binding-] :binding-form [spec-kind spec] :spec}]
  `(map-destructure ~lang ~(if (= spec-kind :spec) spec `map?)
    ~(->> (dissoc binding- :as :or)
          (map (fn [[k v]]
                 (case k
                   (:keys :strs :syms)
                     (keys||strs||syms>key-specs k (second v))
                   [[(get-in v [:key+spec :key])
                     (speced-binding>spec lang
                       (assoc v :spec (get-in v [:key+spec :spec])))]])))
          (apply concat)
          (into {}))))

(defn speced-binding>spec
  [lang {:as speced-binding [kind binding-] :binding-form [spec-kind spec] :spec}]
  (case kind
    :sym (if (= spec-kind :spec) spec `any?)
    :seq (speced-binding|seq>spec lang speced-binding)
    :map (speced-binding|map>spec lang speced-binding)))

(defn arglist>spec-form|arglist
  [lang args+varargs kw-args #_:quantum.core.specs/map-binding-form]
  `(~(qualify-spec lang 'cat)
     ~@(reduce-2
         (fn [ret speced-binding [_ kw-arg]]
           (conj ret kw-arg (speced-binding>spec lang speced-binding)))
         []
         args+varargs kw-args)))

;; TODO handle duplicate bindings (e.g. `_`) by `s/cat` using unique keys — e.g. :b|arg-2
(defn fns|code [kind lang args]
  (assert (= lang #?(:clj :clj :cljs :cljs)) lang)
  (when (= kind :fn) (println "WARNING: `fn` will ignore spec validation"))
  (let [{:keys [:quantum.core.specs/fn|name overloads :quantum.core.specs/meta] :as args'}
          (us/assert-conform (case kind (:defn :defn-) :quantum.core.defnt/defns|code
                                        :fn            :quantum.core.defnt/fns|code) args)
        ret-sym (gensym "ret") arity-kind-sym (gensym "arity-kind") args-sym (gensym "args")
        {:keys [overload-forms spec-form|args spec-form|fn]}
          (reduce
            (fn [ret {{:keys [args varargs] [pre-kind pre] :pre [_ post] :post :as arglist} :arglist :keys [body]} #_:quantum.core.defnt/arglist+body]
              (let [{:keys [fn-arglist kw-args]}
                      (ur/reducei
                        (fn [ret {:as speced-binding :keys [varargs?]} i|arg]
                          (let [arg-ident (speced-binding>arg-ident speced-binding i|arg)
                                binding-  (speced-binding>binding speced-binding)]
                            (-> ret (cond-> varargs? (update :fn-arglist conj '&))
                                    (update :fn-arglist conj  binding-)
                                    (update :kw-args    assoc binding- arg-ident))))
                        {:fn-arglist [] :kw-args (om)}
                        (cond-> args varargs (conj (assoc varargs :varargs? true))))
                    overload-form     (list* fn-arglist body)
                    arity-ident       (keyword (str "arity-" (if varargs "varargs" (count args))))
                    spec-form|arglist (arglist>spec-form|arglist lang
                                        (cond-> args varargs (conj varargs)) kw-args)
                    spec-form|pre     (when (and (contains? arglist :pre) (= pre-kind :spec))
                                        `(fn [~kw-args] ~pre))
                    spec-form|args*   (if spec-form|pre
                                          `(~(qualify-spec lang 'and) ~spec-form|arglist ~spec-form|pre)
                                          spec-form|arglist)
                    spec-form|fn*     (if (contains? arglist :post)
                                          `(let [~kw-args ~args-sym]
                                             (~(qualify-spec lang 'spec) ~post))
                                          `(~(qualify-spec lang 'spec) any?))]
                (-> ret
                    (update :overload-forms conj overload-form)
                    (update :spec-form|args conj arity-ident spec-form|args*)
                    (update :spec-form|fn   conj arity-ident spec-form|fn*))))
            {:overload-forms []
             :spec-form|args []
             :spec-form|fn   []}
            overloads)
        spec-form (when (#{:defn :defn-} kind)
                    `(~(qualify-spec lang 'fdef) ~fn|name :args
                       (~(qualify-spec lang 'or) ~@spec-form|args)
                       :fn (us/with-gen-spec (fn [{~ret-sym :ret}] ~ret-sym)
                             (fn [{[~arity-kind-sym ~args-sym] :args}]
                               (case ~arity-kind-sym ~@spec-form|fn)))))
        fn-form (case kind
                  :fn    (list* 'fn (concat (when (contains? args' :quantum.core.specs/fn|name)
                                              [fn|name])
                                            overload-forms))
                  :defn  (list* 'defn fn|name overload-forms)
                  :defn- (list* 'defn- fn|name overload-forms))
        code `(do ~spec-form ~fn-form)]
    code))

#?(:clj
(defmacro fns
  "Like `fnt`, but relies entirely on runtime spec checks."
  [& args] (fns|code :fn (ufeval/env-lang) args)))

#?(:clj
(defmacro defns
  "Like `defnt`, but relies entirely on runtime spec checks."
  [& args] (fns|code :defn (ufeval/env-lang) args)))

#?(:clj
(defmacro defns-
  "defns : defns- :: defn : defn-"
  [& args] (fns|code :defn- (ufeval/env-lang) args)))
