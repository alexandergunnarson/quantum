(ns quantum.untyped.core.defnt
  (:require
    [clojure.spec.alpha                 :as s]
    [quantum.untyped.core.collections   :as c]
    [quantum.untyped.core.data.set      :as set]
    [quantum.untyped.core.fn
      :refer [<- fn-> fn1 fnl]]
    [quantum.untyped.core.form.evaluate :as ufeval]
    [quantum.untyped.core.log           :as ulog]
    [quantum.untyped.core.logic         :as l]
    [quantum.untyped.core.spec          :as us]
    [quantum.untyped.core.specs         :as ss]))

(s/def :quantum.core.defnt/local-name
  (s/and simple-symbol? (set/not #{'& '| '> '?})))

;; ----- Specs ----- ;;

(s/def :quantum.core.defnt/spec
  (s/alt :infer #{'?}
         :spec  any?))

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
  (s/? (s/cat :sym (fn1 = '>) :spec :quantum.core.defnt/spec)))

(s/def :quantum.core.defnt/arglist
  (s/and vector?
         (s/spec
           (s/cat :args    (s/* :quantum.core.defnt/speced-binding)
                  :varargs (s/? (s/cat :sym            (fn1 = '&)
                                       :speced-binding :quantum.core.defnt/speced-binding))
                  :pre     (s/? (s/cat :sym            (fn1 = '|)
                                       :spec           any?))
                  :post    :quantum.core.defnt/output-spec))
         (s/conformer
           #(cond-> % (contains? % :varargs) (update :varargs :speced-binding)
                      (contains? % :pre    ) (update :pre     :spec)
                      (contains? % :post   ) (update :post    :spec)))
         (fn [{:keys [args varargs]}]
           ;; so `env` in `fnt` can work properly in the analysis
           ;; TODO need to adjust for destructuring
           (c/distinct?
             (concat (c/lmap :binding-form args)
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
              (fnl mapv (fn [overload]
                          (let [overload' (update overload :body :body)]
                            (l/if-let [output-spec (-> f :output-spec :spec)]
                              (do (us/validate (-> overload' :arglist :post) nil?)
                                  (c/assoc-in overload' [:arglist :post] output-spec))
                              overload')))))
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

(defn fns|code [kind lang args]
  (assert (= lang #?(:clj :clj :cljs :cljs)) lang)
  (when (= kind :fn) (ulog/warn! "`fn` will ignore spec validation"))
  (let [{:keys [:quantum.core.specs/fn|name overloads :quantum.core.specs/meta] :as args'}
          (us/validate args (case kind :defn :quantum.core.defnt/defns|code :fn :quantum.core.defnt/fns|code))
        _ (prl! args')
        arglist>arity-ident (fn [{:keys [args varargs]}]
                              (keyword (str "arity-" (if varargs "varargs" (count args)))))
        forms (reduce
                (fn [ret {{:keys [args varargs] :as arglist} :arglist :keys [body]} #_:quantum.core.defnt/arglist+body]
                  (prl! ret arglist body)
                  (let [arglist-form|args    (mapv speced-binding>binding args)
                        arglist-form|varargs (some-> varargs speced-binding>binding)
                        arglist-form         (cond-> arglist-form|args
                                                     varargs (conj '& arglist-form|varargs))
                        kw-arglist-form (->> arglist-form|args
                                             ;; TODO finish this
                                             ;; (map (fn [binding-] [binding- (binding>kw-ident binding-)]))
                                             (into (array-map)))
                        kw-arglist-form (cond-> kw-arglist-form
                                                varargs (assoc :varargs arglist-form|varargs))
                        overload*       (list* arglist-form body)
                        arity-ident     (arglist>arity-ident arglist)
                        ;; ;; TODO finish this
                        ;; spec            `(s/cat ~(keyword ))
                        ;; ;; TODO finish this
                        ;; spec            (if (contains? arglist :pre)
                        ;;                     `(s/and ~spec (fn [{...}] ~pre))
                        ;;                     spec)
                        ;; TODO finish this
                        spec-form|args* nil
                        ;; TODO finish this
                        spec-form|fn*   nil
                        ]
                    (-> ret
                        (update :overloads      conj overload*)
                        (update :spec-form|args conj arity-ident spec-form|args*)
                        (update :spec-form|fn   conj arity-ident spec-form|fn*))))
                {:overloads      []
                 :spec-form|args []
                 :spec-form|fn   []}
                overloads)
        _ (prl! forms)
        spec-form (when (= kind :defn)
                    `(s/fdef ~fn|name {:args (s/or ~@(:spec-form|args forms))
                                       :fn   (fn [{ret# :ret [arity-kind# args#] :args}]
                                               (case arity-kind#
                                                 ~@(:spec-form|fn forms)))}))
        fn-form (case kind
                  :fn   (list* 'fn (-> (if (contains? args' :quantum.core.specs/fn|name)
                                           [fn|name]
                                           [])
                                       (conj (:overloads forms))))
                  :defn (list* 'defn fn|name (:overloads forms)))
        code `(do ~spec-form ~fn-form)]
    code))

#?(:clj
(defmacro fns
  "Like `fnt`, but relies entirely on runtime spec checks. Does not perform type inference."
  [& args] (fns|code :fn (ufeval/env-lang) args)))

#?(:clj
(defmacro defns
  "Like `defnt`, but relies entirely on runtime spec checks. Does not perform type inference."
  [& args] (fns|code :defn (ufeval/env-lang) args)))

#_(set! s/*explain-out* expound/printer)

#_(defns abcde "Documentation" {:metadata "abc"}
  ([a #(instance? Long %)] (+ a 1))
  ([b ?, c _ > integer?] {:pre 1} 1 2)
  ([d string?, e #(instance? StringBuilder %) & f _ > number?]
    (.substring ^String d 0 1)
    (.append ^StringBuilder e 1)
    3 4))

(defns fghij "Documentation" {:metadata "abc"}
  ([a number? > number?] (inc a))
  ([a number?, b number?
    | (> a b)
    > (s/and number? #(> % a) #(> % b))] (+ a b))
  ([a string?
    b boolean?
    {:as c
     :keys [d keyword? e string?]
     f [:f string?]}
    #(-> % count (= 2))
    [g double? & h seq? :as i] sequential?
    [j symbol?] vector?
    & [l string? :as k] seq?
    | (and (> a b) (contains? c a)
           a b c d e f g h i j k l)
    > number?] 0))

(s/fdef fghijk
  :args (s/or :arity-1 (s/cat :a number?)
               :arity-2 (s/and (s/cat :a number? :b number?)
                               (fn [{a :a b :b}] (> a b)))
               :arity-3 (s/and (s/cat :a     string?
                                      :b     boolean?
                                      :c     (s/and #(-> % count (= 2))
                                                    (fn [{:keys [d]}] (keyword? d))
                                                    (fn [{:keys [e]}] (string?  e))
                                                    (fn [{f :f}] (string? f)))
                                      :i     (s/and sequential?
                                                    (fn [[g]] (double? g))
                                                    (fn [[g & h]] (seq? h)))
                                      :arg4# (s/and vector?
                                                    (fn [[j]] (symbol? j)))
                                      :k     (s/and seq?
                                                    (fn [[l]] (string? l))))
                               (fn [{a :a
                                     b :b
                                     {:as c :keys [d e] f :f} :c
                                     [g & h :as i] :i
                                     [j] :arg4#
                                     [l :as k] :k}]
                                 (and (> a b) (contains? c a)
                                      a b c d e f g h i j k l))))
   :fn   (fn [{ret :ret [arity-kind args] :args}]
           (case arity-kind
             :arity-1 (let [{a :a} args]
                        (number? ret))
             :arity-2 (let [{a :a b :b} args]
                        ((s/and number? #(> % a) #(> % b)) ret))
             :arity-3 (let [{a :a
                             b :b
                             {:as c :keys [d e] f :f} :c
                             [g & h :as i] :i
                             [j] :arg4#
                             [l :as k] :k} args]
                        (number? ret)))))
