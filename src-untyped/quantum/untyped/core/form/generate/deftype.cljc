(ns quantum.untyped.core.form.generate.deftype
  (:refer-clojure :exclude [deftype])
  (:require
    [cljs.analyzer]
    [cljs.core]
    [clojure.core                        :as core]
    [quantum.untyped.core.data
      :refer [kw-map val?]]
    [quantum.untyped.core.form.evaluate
      :refer [case-env]]
    [quantum.untyped.core.form.generate  :as ufgen]
    [quantum.untyped.core.form.generate.definterface]
    [quantum.untyped.core.form.type-hint :as uth
      :refer [type-hint with-type-hint un-type-hint]]
    [quantum.untyped.core.identification :as uident]
    [quantum.untyped.core.string         :as ustr]))

(defn ?Associative   [lang] (case lang :clj 'clojure.lang.Associative           :cljs 'cljs.core/IAssociative))
(defn ?Collection    [lang] (case lang :clj 'clojure.lang.IPersistentCollection :cljs 'cljs.core/ICollection ))
(defn ?Comparable    [lang] (case lang :clj 'java.lang.Comparable               :cljs 'cljs.core/IComparable ))
(defn ?Counted       [lang] (case lang :clj 'clojure.lang.Counted               :cljs 'cljs.core/ICounted    ))
(defn ?Deref         [lang] (case lang :clj 'clojure.lang.IDeref                :cljs 'cljs.core/IDeref      ))
(defn ?Fn            [lang] (case lang :clj 'clojure.lang.IFn                   :cljs 'cljs.core/IFn         ))
(defn ?Hash          [lang] (case lang :clj 'clojure.lang.IHashEq               :cljs 'cljs.core/IHash       ))
(defn ?Indexed       [lang] (case lang :clj 'clojure.lang.Indexed               :cljs 'cljs.core/IIndexed    ))
(defn ?Iterable      [lang] (case lang :clj 'java.lang.Iterable                 :cljs 'cljs.core/IIterable   ))
(defn ?Lookup        [lang] (case lang :clj 'clojure.lang.ILookup               :cljs 'cljs.core/ILookup     ))
(defn ?Map           [lang] (case lang :clj 'clojure.lang.IPersistentMap        :cljs 'cljs.core/IMap        ))
(defn ?MutableMap    [lang] (case lang :clj 'java.util.Map                      nil))
(defn ?Object        [lang] (case lang :clj 'java.lang.Object                   :cljs 'Object                ))
(defn ?Record        [lang] (case lang :clj 'clojure.lang.IRecord               :cljs 'cljs.core/IRecord     ))
(defn ?Reset         [lang] (case lang :clj 'clojure.lang.IAtom                 :cljs 'cljs.core/IReset      ))
(defn ?Reversible    [lang] (case lang :clj 'clojure.lang.Reversible            :cljs 'cljs.core/IReversible ))
(defn ?Seq           [lang] (case lang :clj 'clojure.lang.ISeq                  :cljs 'cljs.core/ISeq        ))
(defn ?Seqable       [lang] (case lang :clj 'clojure.lang.Seqable               :cljs 'cljs.core/ISeqable    ))
(defn ?Sequential    [lang] (case lang :clj 'clojure.lang.Sequential            :cljs 'cljs.core/ISequential ))
(defn ?Stack         [lang] (case lang :clj 'clojure.lang.IPersistentStack      :cljs 'cljs.core/IStack      ))
(defn ?Swap          [lang] (case lang :clj 'clojure.lang.IAtom                 :cljs 'cljs.core/ISwap       ))

(defn- pfn
  "Protocol fn"
  [sym lang]
  (symbol (case lang
            :clj  (name sym)
            :cljs (str "-" (name sym)))))

(defn- p-arity
  "Protocol arity-maker"
  {:tests '{(p-arity 'abc '([a] 123))
              '[(abc [a] 123)]
            (p-arity 'abc '(([a] 123) ([b] 234)))
              '[(abc [a] 123)
                (abc [b] 234)]}}
  [sym arities]
  (if (-> arities first vector?)
      [(apply list (with-type-hint sym (-> arities first type-hint)) arities)]
      (->> arities
           (mapv (fn [arity] (cons (with-type-hint sym (-> arity first type-hint)) arity))))))

(defn- implement-map-or-collection [methods-spec]
  (if (or (contains? methods-spec '?Lookup     )
          (contains? methods-spec '?Associative))
      'java.util.Map
      'java.util.Collection))

(defn- deftype-helper
  [methods-spec lang]
  (for [[iname impls] methods-spec]
    (case iname
      ?Comparable
        `[~(?Comparable lang)
          ~@(p-arity (case lang :clj 'compareTo :cljs '-compare) (get impls 'compare))]
      ?Seqable
        `[~(?Seqable lang)
          ~@(p-arity (pfn 'seq lang) (get impls 'seq))]
      ?Sequential
        [(?Sequential lang)]
      ?Record
        [(?Record     lang)]
      ?Serializable
        '[?java.io.Serializable] ; TODO CLJS
      ?Seq
        (case lang
          :clj
           `[~(?Seq lang)
               ~@(p-arity 'first  (get impls 'first))
               ~@(p-arity 'more   (get impls 'rest ))
               ~@(p-arity 'next   (get impls 'next ))]
          :cljs
           `[~(?Seq lang)
               ~@(p-arity '-first (get impls 'first))
               ~@(p-arity '-rest  (get impls 'rest ))
             cljs.core/INext
               ~@(p-arity '-next  (get impls 'next ))])
      ?Stack
         `[~(?Stack lang)
           ~@(p-arity (pfn 'peek lang) (get impls 'peek))
           ~@(p-arity (pfn 'pop  lang) (get impls 'pop ))]
      ?Reversible
         `[~(?Reversible lang)
           ~@(p-arity (pfn 'rseq  lang) (get impls 'rseq ))]
      ?Counted
         `[~(?Counted lang)
           ~@(p-arity (pfn 'count lang) (get impls 'count))
           ~@(case lang :clj
               `[~(implement-map-or-collection methods-spec)
                 ~@(p-arity 'size (get impls 'count))]
               nil)]
      ?Object
        (case lang
          :clj
            `[~(?Object lang)
               ~@(p-arity 'equals   (get impls 'equals   ))
               ~@(p-arity 'hashCode (get impls 'hash-code))]
          :cljs
            `[~(?Object lang)
               ~@(p-arity 'equiv    (get impls 'equals))])
      ?Hash
        `[~(?Hash lang)
          ~@(p-arity (case lang :clj 'hasheq :cljs '-hash) (get impls 'hash))]
      ?Meta
        (case lang
          :clj
            `[~'clojure.lang.IObj
               ~@(p-arity 'meta       (get impls 'meta     ))
               ~@(p-arity 'withMeta   (get impls 'with-meta))]
          :cljs
            `[cljs.core/IMeta
               ~@(p-arity '-meta      (get impls 'meta     ))
              cljs.core/IWithMeta
               ~@(p-arity '-with-meta (get impls 'with-meta))])
      ?Collection
        (case lang
          :clj
           `[~(?Collection lang)
               ~@(p-arity 'empty   (get impls 'empty ))
               ~@(p-arity 'equiv   (get impls 'equals)) ; TBD
               ~@(p-arity 'cons    (get impls 'conj  ))
             ~(implement-map-or-collection methods-spec)
               ~@(p-arity 'isEmpty (get impls 'empty?))
               ~@(p-arity 'clear   (get impls 'empty!))]
          :cljs
           `[cljs.core/IEmptyableCollection
               ~@(p-arity '-empty (get impls 'empty ))
             cljs.core/IEquiv
               ~@(p-arity '-equiv (get impls 'equals)) ; TBD
             ~(?Collection lang)
               ~@(p-arity '-conj  (get impls 'conj  ))])
      ?Lookup
        `[~(?Lookup lang)
          ~@(p-arity (case lang :clj 'valAt :cljs '-lookup) (get impls 'get))
          ~@(case lang
              :clj `[~'clojure.lang.IKeywordLookup
                       ~@(p-arity 'getLookupThunk (get impls 'kw-get))
                     ~'java.util.Map
                       ~@(p-arity 'containsValue  (get impls 'containsv?))
                       ~@(p-arity 'get            (get impls 'get))]
              nil)]
      ?Associative
        `[~(?Associative   lang)
          ~@(p-arity (pfn 'assoc  lang) (get impls 'assoc    ))
          ~@(case lang
              :clj  `[  ~@(p-arity 'containsKey (get-in methods-spec '[?Lookup      contains?]))
                        ~@(p-arity 'entryAt     (get-in methods-spec '[?Lookup      find]))
                      ~(?Map lang)
                        ~@(p-arity 'without     (get impls 'dissoc   ))
                      ~'java.util.Map
                        ~@(p-arity 'put         (get impls 'assoc!   ))
                        ~@(p-arity 'remove      (get impls 'dissoc!  ))
                        ~@(p-arity 'putAll      (get impls 'merge!   ))
                        ~@(p-arity 'keySet      (get impls 'keys     ))
                        ~@(p-arity 'values      (get impls 'vals     ))
                        ~@(p-arity 'entrySet    (get impls 'entries  ))]
              :cljs `[~(?Map lang)
                      ~@(p-arity (pfn 'dissoc lang) (get impls 'dissoc   ))])]
      ?Indexed
        `[~(?Indexed lang)
          ~@(p-arity (pfn 'nth lang) (get impls 'nth))]
      ?Iterable
        (case lang
          :clj `[~(?Iterable lang)
                 ~@(p-arity 'iterator (get impls 'iterator))]
          nil)
      ?Print
        (case lang
          :cljs `[cljs.core/IPrintWithWriter
                  ~@(p-arity (pfn 'pr-writer lang) (get impls 'pr))]
          nil)
      ?Deref
        `[~(?Deref lang)
          ~@(p-arity (pfn 'deref lang) (get impls 'deref))]
      ?Atom
        (case lang
          :clj  `[clojure.lang.IAtom
                  ~@(p-arity (pfn 'swap          lang) (get impls 'swap!))
                  ~@(p-arity (pfn 'compareAndSet lang) (get impls 'compare-and-set!))
                  ~@(p-arity (pfn 'reset         lang) (get impls 'reset!))]
          :cljs `[cljs.core/IReset
                  ~@(p-arity (pfn 'reset!        lang) (get impls 'reset!))
                  cljs.core/ISwap
                  ~@(p-arity (pfn 'swap!         lang) (get impls 'swap!))])
      ?Fn
        `[~(?Fn lang)
          ~@(p-arity (pfn 'invoke lang) (get impls 'invoke))]
      ?HashEq
        nil
      `[~iname
        ~@(apply concat
           (for [[name- arities] impls] (p-arity name- arities)))])))

#?(:clj
(defn- deftype|cljs [env t fields & impls]
  (@#'cljs.core/validate-fields "deftype" t fields)
  (let [r (:name (cljs.analyzer/resolve-var (dissoc env :locals) t))
        [fpps pmasks] (@#'cljs.core/prepare-protocol-masks env impls)
        protocols (@#'cljs.core/collect-protocols impls env)
        t (vary-meta t assoc
            :protocols protocols
            :skip-protocol-flag fpps) ]
    `(do
       (deftype* ~t ~fields ~pmasks
         ~(if (seq impls)
            `(extend-type ~t ~@(@#'cljs.core/dt->et t impls fields))))
       (set! (.-getBasis ~t) (fn [] '[~@fields]))
       (set! (.-cljs$lang$type ~t) true)
       (set! (.-cljs$lang$ctorStr ~t) ~(str r))
       (set! (.-cljs$lang$ctorPrWriter ~t) (fn [this# writer# opt#] (cljs.core/-write writer# ~(str r))))

       ~(when-not (-> t meta :no-factory?)
          (@#'cljs.core/build-positional-factory t r fields))
       ~t))))

#?(:clj
(defn- deftype|clj [env name fields & opts+specs]
  (@#'clojure.core/validate-fields fields name)
  (let [gname name
        [interfaces methods opts] (@#'clojure.core/parse-opts+specs opts+specs)
        ns-part                   (namespace-munge *ns*)
        classname                 (symbol (str ns-part "." gname))
        hinted-fields             fields
        fields                    (vec (map #(with-meta % nil) fields))
        [field-args over]         (split-at 20 fields)]
    `(let []
       ~(@#'clojure.core/emit-deftype* name gname (vec hinted-fields) (vec interfaces) methods opts)
       (import ~classname)
       ~(when-not (-> name meta :no-factory?)
          (@#'clojure.core/build-positional-factory gname classname fields))
       ~classname))))

; Spec for ::core/deftype|method
#_(keys :ret    (? type-hint?)
        :name   method-symbol?
        :inputs (vector-of (tuple param-symbol? type-symbol?)) ; these are 'extra' inputs *plus* the leading `this` input
        :body   code?)

(defmethod ufgen/generate ::core/deftype|method
  [_ {:keys [ret name inputs body]}]
  (list (with-type-hint name ret)
        (->> (cons ['this nil] inputs)
             (mapv (fn [[sym hint]] (with-type-hint sym hint))))
        body))

(defmethod ufgen/generate :quantum.core.form.generate.deftype/deftype|method
  [_ {:keys [ret name inputs body]}]
  {name
    (list (with-type-hint
            (->> (cons ['this nil] inputs)
                 (mapv (fn [[sym hint]] (with-type-hint sym hint))))
            ret)
          body)})

(defn- ?symbol->getter|setter
  "Generates a getter or setter from a field symbol"
  [qualified-interface-sym prefix field-sym]
  (when (-> field-sym meta (get prefix))
    (let [type-sym  (type-hint field-sym)
          field-sym (un-type-hint field-sym)
          input-sym 'x
          methods-spec
            {:ret    (case prefix
                       :get type-sym
                       :set qualified-interface-sym)
             :name   (-> (str (name prefix) "_" (name field-sym))
                         (ustr/camelcase true)
                         symbol)
             :inputs (case prefix
                       :get []
                       :set [[input-sym type-sym]])
             :body   (case prefix
                       :get field-sym
                       :set `(do (set! ~field-sym ~input-sym)
                                 ~'this))}]
      {:quantum.core.form.generate.deftype/deftype|method
         (ufgen/generate :quantum.core.form.generate.deftype/deftype|method methods-spec)
       ::core/definterface|method
         (ufgen/generate ::core/definterface|method methods-spec)})))

#?(:clj
(defn- apply-getters+setters
  "Adds getters and setters to a deftype, as requested."
  [lang type-sym fields methods-spec]
  (if (or (not= lang :clj)
          (->> fields
               (filter #(or (-> % meta :get)
                            (-> % meta :set)))
               empty?))
      {:methods-spec methods-spec}
      (let [interface-sym
             (symbol (str "I" (name type-sym) "__GEN"))
            qualified-interface-sym (uident/qualify|class interface-sym)
            methods
              (->> fields
                   (map (fn [field-sym]
                          [(?symbol->getter|setter qualified-interface-sym :get field-sym)
                           (?symbol->getter|setter qualified-interface-sym :set field-sym)]))
                   (apply concat)
                   (filter val?))]
        {:preamble
          (ufgen/generate ::core/definterface
            {:name    interface-sym
             :methods (map ::core/definterface|method methods)})
         :methods-spec
           (merge methods-spec
             {interface-sym (->> methods (map :quantum.core.form.generate.deftype/deftype|method) (reduce merge))})}))))

#?(:clj
(defmethod ufgen/generate :quantum.core.form.generate.deftype/deftype ; WARNING: actually evals interface code when requested ; TODO fix this
  [_ {:keys [&env lang type-sym fields methods-spec]}]
  (let [{:keys [preamble methods-spec]}
          (apply-getters+setters lang type-sym fields methods-spec)
        _ (eval preamble)
        deftype-code
          (apply (case lang :clj deftype|clj :cljs deftype|cljs)
            &env type-sym fields
            (apply concat (deftype-helper methods-spec lang)))] ; in order to help `deftype` recognize that there is an interface, when there is one
    `(do ~deftype-code
         ~(when (= lang :clj) `(import (quote ~(uident/qualify|class type-sym)))))))) ; TODO doesn't this already happen?

#?(:clj
(defmacro deftype
  "Creates a `deftype` which is cross-platform in its declaration of core
   protocols. Also catches and logs duplicate class definition errors for
   purposes of easier constant recompilation for e.g. tools.namespace or
   figwheel.

   Also allows for definition of getters and setters via the ^:get or ^:set
   metadata on fields."
  {:usage '(deftype
             EmptyTree
             [field1]
             {?Seqable
               {first ([this] (+ field1 1))}})}
  [type-sym fields & [methods-spec]]
  (let [lang (case-env :clj :clj :cljs :cljs)
        code (ufgen/generate :quantum.core.form.generate.deftype/deftype
               (kw-map &env lang type-sym fields methods-spec))]
    (case-env
            ; To avoid duplicate class errors
      :clj  (try (eval code)
              (catch Throwable t
                (if (and (string? (.getMessage t))
                         (-> t .getMessage (.contains "duplicate class definition")))
                    (println "WARNING: duplicate class definition for class" type-sym)
                    (throw t))))
      :cljs code))))
