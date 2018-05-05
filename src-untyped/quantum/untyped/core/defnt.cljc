(ns quantum.untyped.core.defnt
  "Primarily for `(de)fns`."
  (:refer-clojure :exclude [any? ident? qualified-keyword? simple-symbol?])
  (:require
    [clojure.spec.alpha                 :as s]
    [quantum.untyped.core.convert       :as uconv]
    [quantum.untyped.core.data.map
      :refer [om]]
    [quantum.untyped.core.form.evaluate :as ufeval]
    [quantum.untyped.core.loops
      :refer [reduce-2]]
    [quantum.untyped.core.reducers      :as ur]
    [quantum.untyped.core.spec          :as us]
    [quantum.untyped.core.type.predicates
      :refer [any? ident? qualified-keyword? simple-symbol?]]))

(s/def :quantum.core.defnt/local-name
  (s/and simple-symbol? (complement #{'& '| '> '?})))

;; ----- Specs ----- ;;

(s/def :quantum.core.defnt/spec
  (s/alt :infer #{'?}
         :any   #{'_}
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

(s/def :quantum.core.defnt/or   (s/map-of simple-symbol? any?))
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
                              (do (us/validate nil? (-> overload' :arglist :post))
                                  (assoc-in overload' [:arglist :post] output-spec))
                              overload'))) %))
            (dissoc :output-spec)))))

(s/def :quantum.core.defnt/fnt
  (s/and (s/spec
           (s/cat
             :quantum.core.specs/fn|name   (s/? :quantum.core.specs/fn|name)
             :quantum.core.specs/docstring (s/? :quantum.core.specs/docstring)
             :quantum.core.specs/meta      (s/? :quantum.core.specs/meta)
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
             :quantum.core.specs/meta      (s/? :quantum.core.specs/meta)
             :output-spec                  :quantum.core.defnt/output-spec
             :overloads                    :quantum.core.defnt/overloads))
         :quantum.core.specs/fn|postchecks
         :quantum.core.defnt/postchecks))

(s/def :quantum.core.defnt/defns|code :quantum.core.defnt/defnt)

(s/def :quantum.core.defnt/binding-form
  (s/alt :sym :quantum.core.defnt/local-name
         :seq :quantum.core.defnt/seq-binding-form
         :map :quantum.core.defnt/map-binding-form))

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

(defn context>destructuring [arg-ident #_simple-symbol? context #_vector?]
  (reduce
    (fn [destructuring [context-type #_#{:map :seq} k varargs?]]
      (case context-type
        :map  {destructuring k}
        :seq  (let [base (vec (repeatedly k #(gensym "_")))]
                (if varargs?
                    (conj base '& destructuring)
                    (assoc base k destructuring)))
        (:keys :syms :strs) {context-type [destructuring]}))
    arg-ident
    (rseq context)))

(defn keys-syms-strs>arg-specs [binding- binding-kind context]
  (->> (get binding- binding-kind) second
       (filter (fn [{[spec-kind _] :spec}] (= spec-kind :spec)))
       (mapv (fn [{:keys [binding-form #_symbol?] [spec-kind spec] :spec}]
               (let [destructuring (context>destructuring binding-form (conj context [binding-kind nil]))]
                 `(us/with (fn [~destructuring] ~binding-form) ~spec))))))

(defn >as-specs [{:as speced-binding [kind binding-] :binding-form [spec-kind spec] :spec} context]
  (let [[k base-spec] (case kind :seq [:sym `clojure.core/seqable?]
                                 :map [1    `clojure.core/map?])]
    (let [as-ident (or (get-in binding- [:as k]) (gensym "as"))
          destructuring (context>destructuring as-ident context)]
      (cond-> [`(us/with (fn [~destructuring] ~as-ident) ~base-spec)]
        (= spec-kind :spec) (conj `(us/with (fn [~destructuring] ~as-ident) ~spec))))))

(defn speced-binding>arg-specs
  ([speced-binding] (speced-binding>arg-specs speced-binding []))
  ([{:as speced-binding [kind binding-] :binding-form [spec-kind spec] :spec}
    #_:quantum.core.defnt/speced-binding context #_vector?]
    (case kind
      :sym (when (= spec-kind :spec)
             [(let [destructuring (context>destructuring binding- context)]
               `(us/with (fn [~destructuring] ~binding-) ~spec))])
      :seq (let [{elems :elems rest- :rest} binding-]
             (apply concat
               (>as-specs speced-binding context)
               (->> elems
                    (map-indexed (fn [i speced-binding]
                                   (speced-binding>arg-specs speced-binding (conj context [:seq i]))))
                    (apply concat))
               (when rest-
                 [(speced-binding>arg-specs (:form rest-) (conj context [:seq (count elems) true]))])))
      :map (apply concat
             (>as-specs speced-binding context)
             (keys-syms-strs>arg-specs binding- :keys context)
             (keys-syms-strs>arg-specs binding- :syms context)
             (keys-syms-strs>arg-specs binding- :strs context)
             (->> (dissoc binding- :as :or :keys :syms :strs)
                  (map (fn [[k {:as v :keys [key+spec]}]]
                         (speced-binding>arg-specs
                           (assoc v :spec (:spec key+spec))
                           (conj context [:map (:key key+spec)])))))))))

(defn arglist>spec-form|arglist
  [args+varargs kw-args #_:quantum.core.specs/map-binding-form]
  `(s/cat ~@(reduce-2
              (fn [ret speced-binding [_ kw-arg]]
                (let [arg-specs (speced-binding>arg-specs speced-binding)]
                  (conj ret kw-arg (case (count arg-specs)
                                     0 `clojure.core/any?
                                     1 (first arg-specs)
                                     `(s/and ~@arg-specs)))))
              []
              args+varargs kw-args)))

(defn fns|code [kind lang args]
  (assert (= lang #?(:clj :clj :cljs :cljs)) lang)
  (when (= kind :fn) (println "WARNING: `fn` will ignore spec validation"))
  (let [{:keys [:quantum.core.specs/fn|name overloads :quantum.core.specs/meta] :as args'}
          (us/validate (case kind (:defn :defn-) :quantum.core.defnt/defns|code
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
                    spec-form|arglist (arglist>spec-form|arglist (cond-> args varargs (conj varargs)) kw-args)
                    spec-form|pre     (when (and (contains? arglist :pre) (= pre-kind :spec))
                                        `(fn [~kw-args] ~pre))
                    spec-form|args*   (if spec-form|pre
                                          `(s/and ~spec-form|arglist ~spec-form|pre)
                                          spec-form|arglist)
                    spec-form|fn*     (if (contains? arglist :post)
                                          `(let [~kw-args ~args-sym] (s/spec ~post))
                                          `(s/spec any?))]
                (-> ret
                    (update :overload-forms conj overload-form)
                    (update :spec-form|args conj arity-ident spec-form|args*)
                    (update :spec-form|fn   conj arity-ident spec-form|fn*))))
            {:overloads      []
             :spec-form|args []
             :spec-form|fn   []}
            overloads)
        spec-form (when (#{:defn :defn-} kind)
                    `(s/fdef ~fn|name :args (s/or ~@spec-form|args)
                                      :fn   (us/with-gen-spec (fn [{~ret-sym :ret}] ~ret-sym)
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
  "Like `fnt`, but relies entirely on runtime spec checks. Ignores type inference requests, but
   allows them for compatibility with `defnt`."
  [& args] (fns|code :fn (ufeval/env-lang) args)))

#?(:clj
(defmacro defns
  "Like `defnt`, but relies entirely on runtime spec checks. Ignores type inference requests, but
   allows them for compatibility with `defnt`."
  [& args] (fns|code :defn (ufeval/env-lang) args)))

#?(:clj
(defmacro defns-
  "defns : defns- :: defn : defn-"
  [& args] (fns|code :defn- (ufeval/env-lang) args)))
