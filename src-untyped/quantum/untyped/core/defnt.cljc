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
    [quantum.untyped.core.specs         :as ss]))

(s/def :quantum.core.defnt/local-name
  (s/and simple-symbol? (set/not #{'& '| '>})))

;; ----- Specs ----- ;;

(s/def :quantum.core.defnt/spec s/any?)

(s/def :quantum.core.defnt/arg-spec ; TODO expand; make typed destructuring available via :quantum.core.specs/binding-form
  (s/alt :infer #{'?}
         :any   #{'_}
         :spec  :quantum.core.defnt/spec))

;; ----- General destructuring ----- ;;

(s/def :quantum.core.defnt/binding-form
  (s/alt :sym :quantum.core.defnt/local-name
         :seq :quantum.core.defnt/seq-binding-form
         :map :quantum.core.defnt/map-binding-form))

;; ----- Sequential destructuring ----- ;;

(s/def :quantum.core.defnt/seq-binding-form
  (s/and vector?
         (s/cat :elems (s/* :quantum.core.specs/binding-form)
                :rest  (s/? (s/cat :amp #{'&}  :form :quantum.core.defnt/binding-form))
                :as    (s/? (s/cat :as  #{:as} :sym  :quantum.core.defnt/local-name)))))

;; ----- Map destructuring ----- ;;

(defn- >keys|syms|strs [spec]
  (s/and vector?
    (s/spec (s/* (s/cat :arg-binding                 spec
                        :quantum.core.defnt/arg-spec :quantum.core.defnt/arg-spec)))))

(s/def :quantum.core.defnt/keys (>keys|syms|strs ident?))

(s/def :quantum.core.defnt/syms (>keys|syms|strs symbol?))

(s/def :quantum.core.defnt/strs (>keys|syms|strs simple-symbol?))

(s/def :quantum.core.defnt/or   :quantum.core.specs/or)
(s/def :quantum.core.defnt/as   :quantum.core.defnt/local-name)

(s/def :quantum.core.defnt/map-special-binding
  (s/keys :opt-un [:quantum.core.defnt/as   :quantum.core.defnt/or
                   :quantum.core.defnt/keys :quantum.core.defnt/syms :quantum.core.defnt/strs]))

; TODO finish this and others in this namespace
(s/def :quantum.core.defnt/map-binding (s/tuple :quantum.core.defnt/binding-form any?))

; TODO
(s/def :quantum.core.specs/ns-keys
  (s/tuple
    (s/and qualified-keyword? #(-> % name #{"keys" "syms"}))
    (s/coll-of simple-symbol? :kind vector?)))

; TODO
(s/def :quantum.core.specs/map-bindings
  (s/every (s/or :mb  :quantum.core.specs/map-binding
                 :nsk :quantum.core.specs/ns-keys
                 :msb (s/tuple #{:as :or :keys :syms :strs} any?)) :into {}))

; TODO
(s/def :quantum.core.specs/map-binding-form
  (s/merge :quantum.core.specs/map-bindings :quantum.core.specs/map-special-binding))

;; ----- Args ----- ;;

(s/def :quantum.core.defnt/fnt|speced-binding
  (s/cat :arg-binding                     :quantum.core.defnt/fnt|speced-binding
         :quantum.core.defnt/fnt|arg-spec :quantum.core.defnt/fnt|arg-spec))

(s/def :quantum.core.defnt/fnt|output-spec
  (s/? (s/cat :sym (fn1 = '>) :quantum.core.defnt/spec :quantum.core.defnt/spec)))

(s/def :quantum.core.defnt/fnt|arglist
  (s/and vector?
         (s/spec
           (s/cat :args    (s/* :quantum.core.defnt/fnt|speced-binding)
                  :varargs (s/? (s/cat :sym                                   (fn1 = '&)
                                       :quantum.core.defnt/fnt|speced-binding :quantum.core.defnt/fnt|speced-binding))
                  :pre     (s/? (s/cat :sym                                   (fn1 = '|)
                                       :quantum.core.defnt/spec               :quantum.core.defnt/spec))
                  :post    :quantum.core.defnt/fnt|output-spec))
         (s/conformer
           #(cond-> % (contains? % :varargs) (update :varargs :quantum.core.defnt/fnt|speced-binding)
                      (contains? % :pre    ) (update :pre     :quantum.core.defnt/spec)
                      (contains? % :post   ) (update :post    :quantum.core.defnt/spec)))
         (fn [{:keys [args varargs]}]
           ;; so `env` in `fnt` can work properly in the analysis
           ;; TODO need to adjust for destructuring
           (c/distinct?
             (concat (c/lmap :arg-binding args)
                     [(:arg-binding varargs)])))))

(s/def :quantum.core.defnt/fnt|body (s/alt :body (s/* s/any?)))

(s/def :quantum.core.defnt/fnt|arglist+body
  (s/cat :quantum.core.defnt/fnt|arglist :quantum.core.defnt/fnt|arglist
         :body                           :quantum.core.defnt/fnt|body))

(s/def :quantum.core.defnt/fnt|overloads
  (s/alt :overload-1 :quantum.core.defnt/fnt|arglist+body
         :overload-n (s/cat :overloads (s/+ (s/spec :quantum.core.defnt/fnt|arglist+body)))))

(s/def :quantum.core.defnt/fnt|postchecks
  (s/conformer
    (fn [f]
      (-> f (update :overloads
              (fnl mapv (fn [overload]
                          (let [overload' (update overload :body :body)]
                            (l/if-let [output-spec (-> f :output-spec :quantum.core.defnt/spec)]
                              (do (s/validate (-> overload' :quantum.core.defnt/fnt|arglist :post) nil?)
                                  (c/assoc-in overload' [:quantum.core.defnt/fnt|arglist :post] output-spec))
                              overload')))))
            (dissoc :output-spec)))))

(s/def :quantum.core.defnt/fnt
  (s/and (s/spec
           (s/cat
             :quantum.core.specs/fn|name   (s/? :quantum.core.specs/fn|name)
             :quantum.core.specs/docstring (s/? :quantum.core.specs/docstring)
             :quantum.core.specs/meta      (s/? :quantum.core.specs/meta)
             :output-spec                  :quantum.core.defnt/fnt|output-spec
             :overloads                    :quantum.core.defnt/fnt|overloads))
         :quantum.core.specs/fn|postchecks
         :quantum.core.defnt/fnt|postchecks))

(s/def :quantum.core.defnt/fns|code :quantum.core.defnt/fnt)

(s/def :quantum.core.defnt/defnt
  (s/and (s/spec
           (s/cat
             :quantum.core.specs/fn|name   :quantum.core.specs/fn|name
             :quantum.core.specs/docstring (s/? :quantum.core.specs/docstring)
             :quantum.core.specs/meta      (s/? :quantum.core.specs/meta)
             :output-spec                  :quantum.core.defnt/fnt|output-spec
             :overloads                    :quantum.core.defnt/fnt|overloads))
         :quantum.core.specs/fn|postchecks
         :quantum.core.defnt/fnt|postchecks))

(s/def :quantum.core.defnt/defns|code :quantum.core.defnt/defnt)

(defn fns|code [kind lang args]
  (assert (= lang #?(:clj :clj :cljs :cljs)) lang)
  (let [{:keys [:quantum.core.specs/fn|name overloads :quantum.core.specs/meta] :as args'}
          (s/validate args (case kind :defn :quantum.core.defnt/defns|code :fn :quantum.core.defnt/fns|code))
        overload-data>overload
          (fn [{{:keys [args varargs pre post]} :quantum.core.defnt/fnt|arglist
                body :body}]
            (let [arg-spec>validation
                    (fn [{[k spec] :quantum.core.defnt/fnt|arg-spec :keys [arg-binding]}]
                      ;; TODO this validation is purely temporary until destructuring is supported
                      (s/validate arg-binding simple-symbol?)
                      (case k
                        :any   nil
                        :infer (do (ulog/pr :warn "Spec inference not supported in `defns`. Ignoring request to infer" (str "`" arg-binding "`"))
                                   nil)
                        :spec  (list `s/validate arg-binding spec)))
                  spec-validations
                    (concat (c/lmap arg-spec>validation args)
                            (some-> varargs arg-spec>validation))
                  ;; TODO if an arg has been primitive-type-hinted in the `fn` arglist, then no need to do an `instance?` check
                  ?hint-arg
                    (fn [{[k spec] :quantum.core.defnt/fnt|arg-spec :keys [arg-binding]}]
                      arg-binding)
                  arglist'
                    (->> args
                         (c/map ?hint-arg)
                         (<- (cond-> varargs (conj '& (?hint-arg varargs)))))
                  pre-validations
                    (c/>vec (concat (some->> spec-validations (c/lfilter some?))
                                    (when pre (list 'assert pre))))
                  validations
                    (->> {:post (when post [(list post (symbol "%"))])
                          :pre  (when (seq pre-validations) pre-validations)}
                         (c/remove-vals' empty?))]
              (list* arglist' (concat (when (seq validations) [validations]) body))))
        overloads (mapv overload-data>overload overloads)
        code (case kind
               :fn   (list* 'fn (concat
                                  (if (contains? args' :quantum.core.specs/fn|name)
                                      [fn|name]
                                      [])
                                  [overloads]))
               :defn (list* 'defn fn|name overloads))]
    code))

#?(:clj
(defmacro fns
  "Like `fnt`, but relies on runtime spec checks. Does not perform type inference."
  [& args]
  (fns|code :fn (ufeval/env-lang) args)))

#?(:clj
(defmacro defns
  "Like `defnt`, but relies on runtime spec checks. Does not perform type inference."
  [& args]
  (fns|code :defn (ufeval/env-lang) args)))

(defns abcde "Documentation"
  ([a #(instance? Long %)] (+ a 1))
  ([b ? c _ > integer?] {:pre 1} 1 2)
  ([d string?, e #(instance? StringBuilder %) & f _ > number?]
    (.substring ^String d 0 1)
    (.append ^StringBuilder e 1)
    3 4))

(defns fghij
  ([a number? > number?] (inc a))
  ([a number?, b number?
    | (> a b)
    > (s/and number? #(> % a) #(> % b))] (+ a b))
  ([a string?
    b boolean?
    {:as c
     :keys [d keyword? e string?]
     [f integer?] :f}
    #(-> % count (= 2))
    [g double? & h seq? :as i] sequential?
    & j seq?
    | (and (> a b) (contains? c a))
    > number?] 0))
