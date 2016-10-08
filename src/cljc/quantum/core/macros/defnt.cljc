(ns ^{:doc "Because of the size of the dependencies for |defnt|,
      it was determined that it should have its own namespace."}
  quantum.core.macros.defnt
  (:refer-clojure :exclude [merge])
  (:require
    [quantum.core.collections.base           :as cbase
      :refer        [merge-call update-first update-val ensure-set reducei
                     #?(:clj kmap)]
      :refer-macros [        kmap]]
    [quantum.core.data.map                   :as map
      :refer [merge]]
    [quantum.core.data.set                   :as set]
    [quantum.core.data.vector                :as vec
      :refer [catvec]]
    [quantum.core.error                      :as err
      :refer        [->ex
                     #?@(:clj [throw-unless assertf->>])]
      :refer-macros [          throw-unless assertf->>]]
    [quantum.core.fn                         :as fn
      :refer        [#?@(:clj [<- fn-> fn->> f$n])]
      :refer-macros [          <- fn-> fn->> f$n]]
    [quantum.core.log                        :as log
      :include-macros true]
    [quantum.core.logic                      :as logic
      :refer        [nempty? nnil?
                     #?@(:clj [eq? fn-not fn-or whenc whenf whencf$n if$n condf])
                                                  ]
      :refer-macros [          eq? fn-not fn-or whenc whenf whencf$n if$n condf]]
    [quantum.core.macros.core                :as cmacros
      :refer [#?@(:clj [when-cljs if-cljs])]]
    [quantum.core.macros.fn                  :as mfn]
    [quantum.core.analyze.clojure.predicates :as anap
      :refer [type-hint]]
    [quantum.core.macros.protocol            :as proto]
    [quantum.core.macros.reify               :as reify]
    [quantum.core.macros.transform           :as trans]
    [quantum.core.numeric.combinatorics      :as combo]
    [quantum.core.print                      :as pr]
    [quantum.core.type.defs                  :as tdefs]
    [quantum.core.type.core                  :as tcore]
    [quantum.core.vars                       :as var
      :refer        [#?@(:clj [defalias])]
      :refer-macros [          defalias]]))

; TODO reorganize this namespace and move into other ones as necessary

; In using |defnt|, you should always prefer unboxed to boxed.
; The internal |reify| may differentiate unboxed and boxed, but the protocol won't

(def special-defnt-keywords
  '#{:first :else :elem})

(defn special-defnt-keyword? [x]
  (contains? special-defnt-keywords x))

(def qualified-class-name-map
  (->> tcore/primitive-types
       (repeat 2)
       (apply zipmap)))

(defn get-qualified-class-name
  [lang ns- class-sym]
  #?(:clj  (if (= lang :clj)
               (whenf class-sym (fn-not special-defnt-keyword?)
                 (whencf$n symbol?
                   (if-let [qualified-class-name (get qualified-class-name-map class-sym)]
                     qualified-class-name
                     (let [class-name (or (ns-resolve ns- class-sym)
                                          (resolve (symbol (str (ns-name ns-) "." class-sym))))
                           _ (assert (some? class-name) {:class class-sym :ns ns- :msg "Class not found for symbol in namespace"})]
                       (symbol (.getName ^Class class-name))))))
               class-sym)
     :cljs class-sym))

(defn classes-for-type-predicate
  ([pred lang] (classes-for-type-predicate pred lang nil))
  ([pred lang type-arglist]
  (throw-unless ((fn-or symbol? keyword?) pred) "Type predicate must be a symbol or keyword.")
  (cond
    (and (symbol? pred) (anap/possible-type-predicate? pred))
      (->> tcore/types-unevaled
           (<- get lang)
           (<- get pred)
           (assertf->> nempty? "No classes match the predicate provided.")
           (into []))
    :else [pred])))

(defn expand-classes-for-type-hint
  ([x lang ns-] (expand-classes-for-type-hint x lang ns- nil))
  ([x lang ns- arglist]
    (condf x
      (fn-or symbol? keyword?)
        (fn-> hash-set (expand-classes-for-type-hint lang ns- arglist))
      set?    (fn->> (map (f$n classes-for-type-predicate lang arglist))
                     (apply concat)
                     (map #(get-qualified-class-name lang ns- %))
                     (into #{}))
      string? (fn-> symbol hash-set)
      ;nil?    (constantly #{'Object})
      #(throw (->ex nil "Not a type hint." %)))))

(defn hint-arglist-with
  [arglist hints]
  (reducei ; technically reduce-2
    (fn [arglist-f arg i]
      (conj arglist-f (cmacros/hint-meta arg (get hints i))))
    []
    arglist))

(def defnt-remove-hints
  (fn->> (into [])
         (<- update 0 (fn->> (filter symbol?) (into [])))))

(defn defnt-arities
  {:out '[[[^string? x] (println "A string!")]
          [[^vector? x] (println "A vector!")]]}
  [{:as env :keys [body]}]
  {:arities
    (condf body
      (fn-> first vector?) (fn->> defnt-remove-hints vector)
      (fn-> first seq?   ) (fn->> (mapv defnt-remove-hints))
      #(throw (->ex nil "Unexpected form when trying to parse arities." %)))})

(defn defnt-arglists
  {:out '[[^string? x] [^vector? x]]}
  [{:as env :keys [body]}]
  {:arglists
    (condf body
      (fn-> first vector?) (fn->> first vector)
      (fn-> first seq?   ) (fn->> (mapv first))
      #(throw (->ex nil "Unexpected form when trying to parse arglists." %)))})

(defn defnt-gen-protocol-names
  "Generates |defnt| protocol names"
  [{:keys [sym strict? lang]}]
  (let [genned-protocol-name
          (when-not strict? (-> sym name cbase/camelcase (str "Protocol") munge symbol))
        genned-protocol-method-name
          (if (= lang :clj)
              (-> sym name (str "-protocol") symbol)
              sym)
        genned-protocol-method-name-qualified
          (symbol (name (ns-name *ns*))
            (name genned-protocol-method-name))]
    (kmap genned-protocol-name
          genned-protocol-method-name
          genned-protocol-method-name-qualified)))

(defn defnt-gen-interface-unexpanded
  "Note: Also used by CLJS, not because generating an interface is
         actually possible in CLJS, but because |defnt| is designed
         such that its base is of a gen-interface format."
  {:out '{[Randfn [#{java.lang.String}] Object]
            [[^string? x] (println "A string!")],
          [Randfn
           [#{clojure.lang.APersistentVector$RSeq clojure.lang.Tuple
              clojure.lang.APersistentVector
              clojure.lang.PersistentVector
              clojure.core.rrb_vector.rrbt.Vector}]
           Object]
            [[^vector? x] (println "A vector!")]}}
  [{:keys [sym arities arglists-types lang ns-]}]
  {:post [(log/ppr-hints :macro-expand "GEN INTERFACE CODE BODY UNEXP" %)]}
  (assert (nempty? arities))
  (let [genned-method-name
          (-> sym name cbase/camelcase munge symbol)
        genned-interface-name
          (-> sym name cbase/camelcase (str "Interface") munge symbol)
        ns-qualified-interface-name
          (-> genned-interface-name
              #?(:clj (cbase/ns-qualify (namespace-munge *ns*))))
        gen-interface-code-header
          (list 'gen-interface :name ns-qualified-interface-name :methods)
        gen-interface-code-body-unexpanded
          (->> arglists-types ; [[int String] int]
               (map (fn [[type-arglist-n return-type-n :as arglist-n]]
                      (let [_ (log/pr :macro-expand "UNEXPANDED TYPE HINTS" type-arglist-n "IN NS" ns-)
                            type-hints-n (->> type-arglist-n
                                              (mapv (f$n expand-classes-for-type-hint lang ns-
                                                    type-arglist-n)))
                            _ (log/pr :macro-expand "EXPANDED TYPE HINTS" type-hints-n)]
                        [type-hints-n return-type-n])))
               (map (partial into [genned-method-name]))
               (<- zipmap arities))]
    (log/ppr-hints :macro-expand "gen-interface-code-body-unexpanded" gen-interface-code-body-unexpanded)
    (assert (nempty? gen-interface-code-body-unexpanded))
    (kmap genned-method-name
          genned-interface-name
          ns-qualified-interface-name
          gen-interface-code-header
          gen-interface-code-body-unexpanded)))

(defn defnt-replace-kw
  "Replaces @kw with the appropriate hints/arglist"
  [kw {:keys [type-hints type-arglist available-default-types hint inner-type-n]}]
  (condp = kw
    :first (->> type-arglist
                (map (whencf$n (eq? :first) (first type-arglist))))
    :elem  (if (= hint :elem)
               inner-type-n
               hint)
    :else (do (assert (nnil? available-default-types))
              (->> type-hints
                   (map-indexed
                     (fn [n type-hint]
                       (cond
                         (= type-hint #{:else}); because expanded
                           (whenc (get available-default-types n) empty?
                             (throw (->ex nil (str "Available default types for :else type hint are empty for position " n))))
                         :else type-hint)))))))

(defn defnt-gen-interface-expanded
  "Also used by CLJS, not because generating an interface is
   actually possible in CLJS, but because |defnt| is designed
   such that its base is of a gen-interface format."
  {:tests '{'[[Func [#{String} #{vector?}       ] long]]
            '[[Func [String    IPersistentVector] long]
              [Func [String    ITransientVector ] long]]}}
  [{:keys [lang
           gen-interface-code-body-unexpanded
           available-default-types]
    :as env}]
  {:post [(log/ppr-hints :macro-expand "GEN INTERFACE CODE BODY EXP" %)]}
  (assert (nempty? gen-interface-code-body-unexpanded))
  (assert (nnil?   available-default-types))
  {:gen-interface-code-body-expanded
    (->> gen-interface-code-body-unexpanded
         (mapv (fn [[[method-name hints ret-type-0] [arglist & body :as arity]]]
                 (let [expanded-hints-list (->> (defnt-replace-kw :else (merge env {:type-hints hints}))
                                                (apply combo/cartesian-product))
                       assoc-arity-etc
                         (fn [hints]
                           (let [inner-type-n (-> hints first tdefs/inner-type)
                                 hints-v (->> hints
                                              (map (fn [hint] (defnt-replace-kw :elem (kmap hint inner-type-n))))
                                              (into []))
                                 arglist-hinted (hint-arglist-with arglist hints-v)
                                 ;_ (log/ppr-hints :macro-expand "TYPE HINTS FOR ARGLIST" (->> arglist-hinted (map type-hint)))
                                 get-max-type (delay (->> arglist-hinted (map type-hint) tdefs/max-type))
                                 ret-type (cond
                                            (= ret-type-0 'first)
                                              (->> arglist-hinted first type-hint)
                                            (= ret-type-0 'largest)
                                              @get-max-type
                                            #?@(:clj
                                           [(= ret-type-0 'auto-promote)
                                              (or (get tdefs/promoted-types @get-max-type) @get-max-type)])
                                            :else (or (-> ret-type-0 (classes-for-type-predicate lang) first)
                                                      ret-type-0
                                                      (get trans/default-hint lang)))
                                 arity-hinted (assoc arity 0 arglist-hinted)]
                             [[method-name hints-v ret-type] (seq arity-hinted)]))]
                   (->> expanded-hints-list
                        (map #(defnt-replace-kw :first {:type-arglist %}))
                        (mapv assoc-arity-etc)))))
         (apply catvec))})

(defn defnt-gen-interface-def
  [{:keys [gen-interface-code-header gen-interface-code-body-expanded]}]
  {:post [(log/ppr-hints :macro-expand "INTERFACE DEF:" %)]}
  {:gen-interface-def
    (concat gen-interface-code-header ; technically |conjr|
      (list (mapv first gen-interface-code-body-expanded)))})

(defn defnt-positioned-types-for-arglist
  {:todo "The shape of this (reducei) is probably useful and can be reused"}
  [arglist types]
  (loop [i 0 arglist-n arglist types-n types]
    (let [type-n (first arglist-n)]
      (if (empty? arglist-n)
          types-n
          (recur (inc i) (rest arglist-n)
                 (update types-n i
                   (fn [type-map]
                     (let [new-map (zipmap (ensure-set type-n) (repeat #{(count arglist)}))]
                     (if (nil? type-map)
                         new-map
                         (merge-with set/union type-map new-map))))))))))

(defn defnt-types-for-arg-positions
  {:out-like '{0 {string?  #{3} number? #{2 3}  }
               1 {decimal? #{0} Object  #{0 2 3}}
               2 nil}}
  [{:keys [lang ns- arglists-types]}]
  {:post [(do (log/ppr :macro-expand "TYPES FOR ARG POSITIONS" %)
              true)]}
  (let [types (->> arglists-types
                   (map first)
                   (reduce (fn [types-n arglist-n]
                             (defnt-positioned-types-for-arglist arglist-n types-n))
                             {})
                   (map (f$n update-val
                          (fn->> (map (fn [[type-hint arity-cts]]
                                          ; TODO what are you going to do with arity-cts?
                                          (zipmap (expand-classes-for-type-hint type-hint lang ns-)
                                                  (repeat arity-cts))))
                                 (apply merge-with set/union))))
                   (into {}))]
    {:types-for-arg-positions types
     :first-types             (get types 0)}))

(defn protocol-verify-unique-first-hint
  "Not allowed same arity and same first hint.

   Protocols can't dispatch on non-first args.
   ([^long x ^int    y ^char z] (+ x y) (str z))
   ([^long x ^String y ^char z] (str x z y))"
  {:in-like '[[Abcde [#{int } #{String               }] int  ]
              [Abcde [#{long} #{APersistentVector ...}] float]]
   :todo ["Allow for implicitly convertible types (e.g. long and Long) with the same codebase to
           pass through this verification"]}
  [arglists]
  (log/ppr-hints :macro-expand "ARGLISTS" arglists)
  (let [cached-arglists (atom {})]
    (doseq [[method-sym arglist ret-type] arglists]
      (doseq [first-hint (first arglist)]
        (when (nnil? first-hint)
          (swap! cached-arglists update (count arglist)
            (fn [first-hints-set]
              (let [hints-set-ensured (ensure-set first-hints-set)]
                (if (contains? hints-set-ensured first-hint)
                    (throw (->ex nil "Not allowed same arity and same first hint:" arglist))
                    (conj hints-set-ensured first-hint))))))))))


(defn defnt-gen-helper-macro
  "Generates the macro helper for |defnt|.
   A call to the |defnt| macro expands to this macro, which then expands, based
   on the availability of type hints, to either the reify version or the protocol version."
  {:todo ["Already tried to make this an inline function via metaing :inline,
           but the problem with trying to do inline is that inlines can't be variadic."]}
  [{:keys [genned-method-name
           genned-protocol-method-name-qualified
           reified-sym-qualified
           strict?
           relaxed?
           sym-with-meta
           lang]}]
  {:post [(log/ppr-hints :macro-expand "HELPER MACRO DEF" %)]}
   (let [args-sym        'args-sym
         args-hinted-sym 'args-hinted-sym]
     {:helper-macro-def
       `(defmacro ~sym-with-meta [& ~args-sym]
          (let [~args-hinted-sym
                  (quantum.core.macros.transform/try-hint-args
                    ~args-sym ~lang ~'&env)]
            (log/pr :macro-expand "DEFNT HELPER MACRO" '~sym-with-meta
                                  "|" ~args-hinted-sym
                                  "|" '~genned-protocol-method-name-qualified
                                  "|" '~genned-method-name)
            (if (or (when-cljs ~'&env true)
                    (= ~lang :cljs)
                    ~relaxed?
                    (and (not ~strict?)
                         (quantum.core.macros.transform/any-hint-unresolved?
                           ~args-hinted-sym ~lang ~'&env)))
                (seq (concat (list '~genned-protocol-method-name-qualified)
                             ~args-hinted-sym))
                (seq (concat (list '.)
                             (list '~reified-sym-qualified)
                             (list '~genned-method-name)
                             ~args-hinted-sym)))))}))


(defn defnt-arglists-types
  {:out-like `{:arglists-types
                ([[AtomicInteger] Object]
                 [[integer?] Object]
                 [[#{String StringBuilder} #{boolean char}] Object]
                 [[#{short byte} #{long int} #{double float}] Object])}}
  [{:as env :keys [arglists lang sym]}]
  {:post [(log/ppr-hints :macro-expand "TYPE HINTS EXTRACTED" %)]}
  {:arglists-types
    (->> arglists
         (map #(trans/extract-all-type-hints-from-arglist lang sym %))
         doall)})

(defn defnt-available-default-types
  {:out-like `{:available-default-types
                {0 #{Object boolean char double float},
                 1 #{Object double short float byte},
                 2 #{Object boolean char long short int byte}}}}
  [{:as env :keys [lang types-for-arg-positions]}]
  {:post [(log/ppr :macro-expand "AVAILABLE DEFAULT TYPES" %)]}
  {:available-default-types
    (->> types-for-arg-positions
         (map (f$n update-val
                (fn->> keys (into #{})
                       (set/difference (-> tcore/types-unevaled (get-in [lang :any]))))))
         (into {}))})

(defn defnt-extend-protocol-def
  [{:as env :keys [strict? types-for-arg-positions]}]
  {:post [(log/ppr-hints :macro-expand "EXTEND PROTOCOL DEF" %)]}
  {:extend-protocol-def
    (when-not strict?
      (proto/gen-extend-protocol-from-interface env))})

(defn defnt-notify-unbox
  "Currently doesn't actually get used by |defnt|, even though it's called."
  [{:as env :keys [sym]}]
  (let [defnt-auto-unboxable? (-> sym type-hint tcore/auto-unboxable?)
        auto-unbox-fn (type-hint sym) ; unbox-fn (long, int, etc.) is same as type hint
        ; TODO auto-unbox according to arguments when no defnt-wide type hint is given
        _ (log/ppr :macro-expand "Auto-unboxable?" defnt-auto-unboxable? auto-unbox-fn)]))

(defn defnt-gen-final-defnt-def
  "The finishing function for |defnt|.
   Takes an aggregated environment and declares and defines the |defnt|:
   - Interface, if a Clojure environment and not relaxed
   - |reify|
   - Protocol and extend-protocol, if not strict
   - Macro (helper), if a Clojure environment
   - Protocol alias, if a ClojureScript environment"
  [{:keys [lang sym strict? externs genned-protocol-method-names
           gen-interface-def
           reify-def reified-sym
           helper-macro-def
           protocol-def extend-protocol-def]}]
  {:post [(log/ppr-hints :macro-expand "DEFNT FINAL" %)]}
  (concat (list* 'do @externs)
    [(when (= lang :clj) gen-interface-def)
     (when-not strict?
       (list* 'declare genned-protocol-method-names)) ; For recursion
     (when (= lang :clj) (list 'declare reified-sym)) ; For recursion
     (when (= lang :clj) helper-macro-def)
     (when (= lang :clj) reify-def)
     protocol-def
     extend-protocol-def
     (when (= lang :cljs)
       (list `defalias (-> sym name
                               (str "-protocol")
                               symbol)
             sym))]))

#?(:clj
(defn defnt*-helper
  "CLJ because it's used by a macro."
  {:todo ["Make so Object gets protocol if reflection"
          "Add support for nil"
          "Add support for destructuring — otherwise
           'ClassCastException clojure.lang.PersistentVector cannot be
            cast to clojure.lang.Named'"]
   :in '(defnt randfn
          ([^string? x] (println "A string!"))
          ([^vector? x] (println "A vector!")))}
  ([opts lang ns- sym doc- meta- body [unk & rest-unk]]
    (apply mfn/defn-variant-organizer
      [defnt*-helper opts lang ns- sym doc- meta- body (cons unk rest-unk)]))
  ([{:as opts
     :keys [strict? relaxed?]} lang ns- sym doc- meta- body]
    (log/ppr :debug (kmap opts lang ns- sym doc- meta- body))
    (let [externs       (atom [])
          sym-with-meta (with-meta sym (merge {:doc doc-} meta-))
          body          (mfn/optimize-defn-variant-body! body externs)
          env (-> (kmap sym strict? relaxed? sym-with-meta lang ns- body externs)
                  (merge-call defnt-arities
                              defnt-arglists
                              defnt-gen-protocol-names
                              defnt-arglists-types
                              defnt-gen-interface-unexpanded
                              defnt-types-for-arg-positions
                              defnt-available-default-types
                              defnt-gen-interface-expanded
                              defnt-gen-interface-def
                              (fn [env] {:reify-body (reify/gen-reify-body env)}) ; Necessary for |gen-protocol-from-interface|
                              (fn [env] (when (= lang :clj)
                                          (reify/gen-reify-def env)))
                              (fn [env] (when-not strict?
                                          (proto/gen-defprotocol-from-interface env)))
                              defnt-extend-protocol-def
                              defnt-notify-unbox
                              (fn [env] (when (= lang :clj)
                                          (defnt-gen-helper-macro env)))))]
      (defnt-gen-final-defnt-def env)))))

(declare defnt*-helper)

;#?(:clj (log/enable! :macro-expand))

#?(:clj
(defmacro defnt
  "|defn|, typed.
   Uses |gen-interface|, |reify|, and |defprotocol| under the hood."
  [sym & body]
  (let [lang (if-cljs &env :cljs :clj)]
    (defnt*-helper nil lang *ns* sym nil nil nil body))))

#?(:clj
(defmacro defnt'
  "'Strict' |defnt|. I.e., generates only an interface and no protocol.
   Only for use with Clojure."
  [sym & body]
  (defnt*-helper {:strict? true} :clj *ns* sym nil nil nil body)))

#?(:clj
(defmacro defntp
  "'Relaxed' |defnt|. I.e., generates only a protocol and no interface."
  [sym & body]
  (defnt*-helper {:relaxed? true} :clj *ns* sym nil nil nil body)))

