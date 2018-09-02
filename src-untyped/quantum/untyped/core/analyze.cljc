(ns quantum.untyped.core.analyze
  (:require
    ;; TODO excise this reference
    [quantum.core.type.core                 :as tcore]
    [quantum.untyped.core.analyze.ast       :as uast]
    [quantum.untyped.core.analyze.expr      :as uxp]
    [quantum.untyped.core.collections       :as c
      :refer [>vec]]
    [quantum.untyped.core.compare           :as ucomp]
    [quantum.untyped.core.core
      :refer [istr]]
    [quantum.untyped.core.data
      :refer [kw-map]]
    [quantum.untyped.core.defnt
      :refer [defns defns- fns]]
    [quantum.untyped.core.error             :as uerr
      :refer [TODO err!]]
    [quantum.untyped.core.fn
      :refer [<- fn-> fn->>]]
    [quantum.untyped.core.form.evaluate     :as ufeval]
    [quantum.untyped.core.log               :as log
      :refer [prl!]]
    [quantum.untyped.core.logic
      :refer [if-not-let ifs]]
    [quantum.untyped.core.print
      :refer [ppr]]
    [quantum.untyped.core.reducers          :as r
      :refer [educe reducei]]
    [quantum.untyped.core.spec              :as s]
    [quantum.untyped.core.type              :as t
      :refer [?]]
    [quantum.untyped.core.type.predicates   :as utpred]
    [quantum.untyped.core.type.reifications :as utr]
    [quantum.untyped.core.vars              :as uvar
      :refer [update-meta]]))

; ----- REFLECTION ----- ;

#?(:clj
(defrecord Method
  [^String name ^Class rtype ^"[Ljava.lang.Class;" argtypes ^clojure.lang.Keyword kind]
  fipp.ednize/IOverride
  fipp.ednize/IEdn (-edn [this] (tagged-literal (symbol "M") (into (array-map) this)))))

#?(:clj (defns method? [x _] (instance? Method x)))

#?(:clj
(defns class->methods [^Class c t/class? > t/map?]
  (->> (.getMethods c)
       (c/remove+  (fn [^java.lang.reflect.Method x]
                     (java.lang.reflect.Modifier/isPrivate (.getModifiers x))))
       (c/map+     (fn [^java.lang.reflect.Method x]
                     (Method. (.getName x) (.getReturnType x) (.getParameterTypes x)
                       (if (java.lang.reflect.Modifier/isStatic (.getModifiers x))
                           :static
                           :instance))))
       (c/group-by (fn [^Method x] (.-name x))) ; TODO all of these need to be into !vector and !hash-map
       (c/map-vals+  (fn->> (c/group-by  (fn [^Method x] (count (.-argtypes x))))
                            (c/map-vals+ (fn->> (c/group-by (fn [^Method x] (.-kind x)))))
                            (r/join {})))
       (r/join {}))))

(defonce class->methods|with-cache
  (memoize (fn [c] (class->methods c))))

(defrecord Field [^String name ^Class class ^clojure.lang.Keyword kind]
  fipp.ednize/IOverride
  fipp.ednize/IEdn (-edn [this] (tagged-literal (symbol "F") (into (array-map) this))))

(defns class->fields [^Class c t/class? > t/map?]
  (->> (.getFields c)
       (c/remove+ (fn [^java.lang.reflect.Field x]
                    (java.lang.reflect.Modifier/isPrivate (.getModifiers x))))
       (c/map+    (fn [^java.lang.reflect.Field x]
                    [(.getName x)
                     (Field. (.getName x) (.getType x)
                       (if (java.lang.reflect.Modifier/isStatic (.getModifiers x))
                           :static
                           :instance))]))
       (r/join {}))) ; TODO !hash-map

(def class->fields|with-cache
  (memoize (fn [c] (class->fields c))))

(def ^:dynamic *conditional-branch-pruning?* true)

(defonce *analyze-depth (atom 0))

(defn add-file-context [to from]
  (let [from-meta (meta from)]
    (update-meta to assoc :line (:line from-meta) :column (:column from-meta))))

(defn persistent!-and-add-file-context [form ast-data]
  (update ast-data :form (fn-> persistent! (add-file-context form))))

(def special-symbols '#{do let* deftype* fn* def . if quote new throw}) ; TODO make more complete

;; TODO move
(deftype WatchableMutable
  [^:unsynchronized-mutable v ^:unsynchronized-mutable ^clojure.lang.IFn watch]
  clojure.lang.IDeref (deref       [this]      v)
  clojure.lang.IRef   (addWatch    [this _ f]  (set! watch f  ) this)
                      (removeWatch [this _]    (set! watch nil) this)
  clojure.lang.IAtom  (reset       [this newv] (set! v newv)  v)
                      (swap        [this f]
                        (let [oldv v]
                          (set! v (f v))
                          (when (some? watch) (watch nil this oldv v))
                          v))
  Object              (equals      [this that]
                        (and (instance? WatchableMutable that)
                             (= v (.-v ^WatchableMutable that))))
  fipp.ednize/IOverride
  fipp.ednize/IEdn    (-edn [this] (tagged-literal (symbol "!@") v)))

;; TODO move
(defn !ref
  ([v]       (->WatchableMutable v nil))
  ([v watch] (->WatchableMutable v watch)))

(s/def ::env (s/map-of t/symbol? t/any?))

(declare analyze*)

(defns- analyze-non-map-seqable
  "Analyzes a non-map seqable."
  {:params-doc
    '{merge-types-fn "2-arity fn that merges two types (or sets of types).
                      The first argument is the current deduced type of the
                      overall AST node; the second is the deduced type of
                      the current sub-AST-node."}}
  [env ::env, form _, empty-form _, rf _]
  (->> form
       (reducei (fn [accum form' i] (rf accum (analyze* (:env accum) form') i))
         {:env env :form (transient empty-form) :body (transient [])})
       (persistent!-and-add-file-context form)
       (<- (update :body persistent!))))

(defns- analyze-map
  {:todo #{"If the map is bound to a variable, preserve type info for it such that lookups
            can start out with a guarantee of a certain type."}}
  [env ::env, form _]
  (TODO "analyze-map")
  #_(->> form
       (reduce-kv (fn [{env' :env forms :form} form'k form'v]
                    (let [ast-node-k (analyze* env' form'k)
                          ast-node-v (analyze* env' form'v)]
                      (->expr-info {:env       env'
                                    :form      (assoc! forms (:form ast-node-k) (:form ast-node-v))
                                   ;; TODO fix; we want the types of the keys and vals to be deduced
                                    :type-info nil})))
         (->expr-info {:env env :form (transient {})}))
       (persistent!-and-add-file-context form)))

(defns- analyze-seq|do [env ::env, [_ _ & body|form _ :as form] _]
  (if (empty? body|form)
      (uast/do {:env           env
                :form          form
                :expanded-form form
                :body          []
                :type          t/nil?})
      (let [{expanded-form :form body :body}
              (analyze-non-map-seqable env body|form []
                (fn [accum ast-data _]
                  (assoc ast-data
                    ;; The env should be the same as whatever it was originally
                    ;; because no new scopes are created
                    :env  (:env accum)
                    :form (conj! (:form accum) (:form ast-data))
                    :body (conj! (:body accum) ast-data))))]
        (uast/do {:env           env
                  :form          form
                  :expanded-form (with-meta (list* 'do expanded-form) (meta expanded-form))
                  :body          body
                  ;; To types, only the last sub-AST-node ever matters, as each is independent
                  ;; from the others
                  :type          (-> body c/last :type)}))))

(defns analyze-seq|let*|bindings [env ::env, bindings|form _]
  (->> bindings|form
       (c/partition-all+ 2)
       (reduce (fn [{env' :env !bindings :form :keys [bindings-map]} [sym form :as binding|form]]
                 (let [node (analyze* env' form)] ; environment is additive with each binding
                   {:env          (assoc env' sym node)
                    :form         (conj! (conj! !bindings sym) (:form node))
                    :bindings-map (assoc bindings-map sym node)}))
         {:env env :form (transient []) :bindings-map {}})
       (persistent!-and-add-file-context bindings|form)))

(defns analyze-seq|let* [env ::env, [_ _, bindings|form _ & body|form _ :as form] _]
  (let [{env' :env bindings|form' :form :keys [bindings-map]}
          (analyze-seq|let*|bindings env bindings|form)
        {body|form' :expanded-form body|type :type body :body}
          (analyze-seq|do env' (list* 'do body|form))]
    (uast/let* {:env           env
                :form          form
                :expanded-form (list* 'let* bindings|form' (rest body|form'))
                :bindings      bindings-map
                :body          body
                :type          body|type})))

(defns methods->type
  "Creates a type given ->`methods`."
  [methods (s/seq-of t/any? #_method?) > t/type?]
  ;; TODO room for plenty of optimization here
  (let [methods|by-ct (->> methods
                           (c/group-by (fn-> :argtypes count))
                           (sort-by first <))
        ;; non-primitive classes in Java aren't guaranteed to be non-null
        >class-type (fn [x]
                      (ifs (class? x)
                             (-> x t/>type (cond-> (not (t/primitive-class? x)) t/?))
                           (t/type? x)
                             x
                           (uerr/not-supported! `>class-type x)))
        partition-deep
          (fn partition-deep [t methods' arglist-size i|arg depth]
            (let [_ (when (> depth 3) (TODO))
                  methods'|by-class
                    (->> methods'
                         ;; TODO optimize further via `group-by-into`
                         (c/group-by (fn-> :argtypes (c/get i|arg)))
                         ;; classes will be sorted from most to least specific
                         (sort-by (fn-> first t/>type) t/<))]
              (r/for [[c methods''] methods'|by-class
                      t' t]
                (update t' :clauses conj
                  [(>class-type c)
                   (if (= (inc depth) arglist-size)
                       ;; here, methods'' count will be = 1
                       (-> methods'' first :rtype >class-type)
                       (partition-deep
                         (uxp/condpf-> t/<= (uxp/get (inc i|arg)))
                         methods''
                         arglist-size
                         (inc i|arg)
                         (inc depth)))]))))]
    (r/for [[ct methods'] methods|by-ct
            t (uxp/casef count)]
      (if (zero? ct)
          (c/assoc-in t [:cases 0]  (-> methods' first :rtype >class-type))
          (c/assoc-in t [:cases ct] (partition-deep (uxp/condpf-> t/<= (uxp/get 0)) methods' ct 0 0))))))

#?(:clj
(defns ?cast-call->type
  "Given a cast call like `clojure.lang.RT/uncheckedBooleanCast`, returns the
   corresponding type.

   Unchecked fns could be assumed to actually *want* to shift the range over if the
   range hits a certain point, but we do not make that assumption here."
  [c t/class?, method t/symbol? > (? t/type?)]
  (when (identical? c clojure.lang.RT)
    (case method
      (uncheckedBooleanCast booleanCast) t/boolean?
      (uncheckedByteCast    byteCast)    t/byte?
      (uncheckedCharCast    charCast)    t/char?
      (uncheckedShortCast   shortCast)   t/char?
      (uncheckedIntCast     intCast)     t/int?
      (uncheckedLongCast    longCast)    t/long?
      (uncheckedFloatCast   floatCast)   t/float?
      (uncheckedDoubleCast  doubleCast)  t/double?
      nil))))

(defns- analyze-seq|dot|method-call
  "A note will be made of what methods match the argument types.
   If only one method is found, that is noted too. If no matching method is found, an
   exception is thrown."
  [env ::env, form _, target _, target-class t/class?, static? t/boolean?, method-form simple-symbol?, args-forms _ #_(seq-of form?)]
  (log/pr!)
  ;; TODO cache type by method
  (if-not-let [methods-for-name (-> target-class class->methods|with-cache (c/get (name method-form)))]
    (if (empty? args-forms)
        (err! "No such method or field in class" {:class target-class :method-or-field method-form})
        (err! "No such method in class"          {:class target-class :methods         method-form}))
    (if-not-let [methods-for-count (c/get methods-for-name (c/count args-forms))]
      (err! "Incorrect number of arguments for method"
            {:class target-class :method method-form :possible-counts (set (keys methods-for-name))})
      (let [static?>kind (fn [static?] (if static? :static :instance))]
        (if-not-let [methods (c/get methods-for-count (static?>kind static?))]
          (err! (istr "Method found for arg-count, but was ~(static?>kind (not static?)), not ~(static?>kind static?)")
                {:class target-class :method method-form :args args-forms})
          (let [args-ct (c/count args-forms)
                call (uast/method-call
                       {:env    env
                        :form   form
                        :target target
                        :method method-form
                        :args   []
                        :type   (methods->type methods #_(count arg-forms))})
                with-arg-types
                  (r/fori [arg-form args-forms
                           call'    call
                           i|arg]
                    (let [arg-node (analyze* env arg-form)]
                      ;; TODO can incrementally calculate return value, but possibly not worth it
                      (update call' :args conj arg-node)))
                with-ret-type
                  (update with-arg-types :type
                    (fn [ret-type] (->> with-arg-types :args (mapv :type) ret-type)))
                ?cast-type (?cast-call->type target-class method-form)
                _ (when ?cast-type
                    (log/ppr :warn "Not yet able to statically validate whether primitive cast will succeed at runtime" {:form form})
                    #_(s/validate (-> with-ret-type :args first :type) #(t/>= % (t/numerically ?cast-type))))]
            with-ret-type))))))

(defns- analyze-seq|dot|field-access
  [env ::env, form _, target _, field-form simple-symbol?, field (t/isa? Field)]
  (log/pr!)
  (uast/field-access
    {:env    env
     :form   form
     :target target
     :field  field-form
     :type   (-> field :class t/>type)}))

(defns classes>class
  "Ensure that given a set of classes, that set consists of at most a class C and nil.
   If so, returns C. Otherwise, throws."
  [cs (s/set-of (? t/class?)) > t/class?]
  (log/pr!)
  (let [cs' (disj cs nil)]
    (if (-> cs' count (= 1))
        (first cs')
        (err! "Found more than one class" cs))))

;; TODO type these arguments; e.g. check that ?method||field, if present, is an unqualified symbol
(defns- analyze-seq|dot [env ::env, [_ _, target-form _, ?method-or-field _ & ?args _ :as form] _]
  (log/pr!)
  (let [target          (analyze* env target-form)
        method-or-field (if (symbol? ?method-or-field) ?method-or-field (first ?method-or-field))
        args-forms      (if (symbol? ?method-or-field) ?args            (rest  ?method-or-field))]
    (if (t/= (:type target) t/nil?)
        (err! "Cannot use the dot operator on nil." {:form form})
        (let [;; `nilable?` because technically any non-primitive in Java is nilable and we can't
              ;; necessarily rely on all e.g. "@nonNull" annotations
              {:as ?target-static-class-map target-static-class :class target-static-class-nilable? :nilable?}
                (-> target :type t/type>?class-value)
              target-classes
                (if ?target-static-class-map
                    (cond-> #{target-static-class} target-static-class-nilable? (conj nil))
                    (-> target :type t/type>classes))
              target-class-nilable? (contains? target-classes nil)
              target-class (classes>class target-classes)]
          ;; TODO determine how to handle `target-class-nilable?`; for now we will just let it slip through
          ;; to `NullPointerException` at runtime rather than create a potentially more helpful custom
          ;; exception
          (if-let [field (and (empty? args-forms)
                              (-> target-class class->fields|with-cache (c/get (name method-or-field))))]
            (analyze-seq|dot|field-access env form target method-or-field field)
            (analyze-seq|dot|method-call env form target target-class (boolean ?target-static-class-map)
              method-or-field args-forms))))))

;; TODO move this
(defns truthy-node? [{:as ast t [:type _]} _ > t/boolean?]
  (log/pr!)
  (ifs (or (t/= t t/nil?)
           (t/= t t/false?)) false
       (or (t/> t t/nil?)
           (t/> t t/false?)) nil ; representing "unknown"
       true))

(defns- analyze-seq|if
  "If `*conditional-branch-pruning?*` is falsey, the dead branch's original form will be
   retained, but it will not be type-analyzed."
  [env ::env, [_ _ & [pred-form _, true-form _, false-form _ :as body] _ :as form] _]
  (log/pr!)
  (if (-> body count (not= 3))
      (err! "`if` accepts exactly 3 arguments: one predicate test and two branches; received"
            {:body body})
      (let [pred-node  (analyze* env pred-form)
            true-node  (delay (analyze* env true-form))
            false-node (delay (analyze* env false-form))
            whole-node
              (delay
                (uast/if-node
                  {:env        env
                   :form       (list 'if (:form pred-node) (:form @true-node) (:form @false-node))
                   :pred-node  pred-node
                   :true-node  @true-node
                   :false-node @false-node
                   :type       (apply t/or (->> [(:type @true-node) (:type @false-node)]
                                                (remove nil?)))}))]
        (case (truthy-node? pred-node)
          true      (do (log/ppr :warn "Predicate in `if` node is always true" {:pred pred-form})
                        (-> @true-node
                            (assoc :env env)
                            (cond-> (not *conditional-branch-pruning?*)
                              (assoc :form (list 'if pred-form (:form @true-node) false-form)))))
          false     (do (log/ppr :warn "Predicate in `if` node is always false" {:pred pred-form})
                        (-> @false-node
                            (assoc :env env)
                            (cond-> (not *conditional-branch-pruning?*)
                              (assoc :form (list 'if pred-form true-form (:form @false-node))))))
          nil       @whole-node))))

(defns- analyze-seq|quote [env ::env, [_ _ & body _ :as form] _]
  (log/pr!)
  (uast/quoted env form (tcore/most-primitive-class-of body)))

(defns- analyze-seq|new [env ::env, [_ _ & [c|form _ #_t/class? & args _ :as body] _ :as form] _]
  (log/pr!)
  (let [c|analyzed (analyze* env c|form)]
    (if-not (and (-> c|analyzed :type t/value-type?)
                 (-> c|analyzed :type utr/value-type>value class?))
            (err! "Supplied non-class to `new` form" {:x c|form})
            (let [c             (-> c|analyzed :type utr/value-type>value)
                  args|analyzed (mapv #(analyze* env %) args)]
              (uast/new-node {:env   env
                              :form  (list* 'new c|form (map :form args|analyzed))
                              :class c
                              :args  args|analyzed
                              :type  (t/isa? c)})))))

(defns- analyze-seq|throw [env ::env, form _ [arg _ :as body] _]
  (log/pr!)
  (if (-> body count (not= 1))
      (err! "Must supply exactly one input to `throw`; supplied" {:body body})
      (let [arg|analyzed (analyze* env arg)]
        ;; TODO this is not quite true for CLJS but it's nice at least
        (if-not (-> arg|analyzed :type (t/<= t/throwable?))
          (err! "`throw` requires a throwable; received" {:arg arg :type (:type arg|analyzed)})
          (uast/throw-node {:env  env
                            :form (list 'throw (:form arg|analyzed))
                            :arg  arg|analyzed
                            ;; `t/none?` because nothing is actually returned
                            :type t/none?})))))

(defns- call>arg-nodes+out-type
  [env _, caller|node _, caller|type _, caller-kind _, args-ct _, body _
   > (s/kv {:arg-nodes t/any? #_(s/seq-of ast/node?)
            :out-type  t/type?})]

  (dissoc
    (if (zero? args-ct)
        {:arg-nodes []
         :out-type  (case caller-kind
                      ;; We could do a little smarter analysis here but we'll keep it simple for now
                      :fn  t/any?
                      :fnt (-> caller|type (get args-ct) first :output-type))}
        (->> body
             (c/map+ #(analyze* env %))
             (reducei (fn [{:as ret :keys [satisfying-overloads-seq]}
                           arg|analyzed i]
                        ;; TODO review this part as it's passing back a nil out-type somehow
                        (if (= :fnt caller-kind)
                            (if-let [satisfying-overloads-seq'
                                      (->> satisfying-overloads-seq
                                           (c/lfilter
                                             (fn [{:keys [input-types]}]
                                               (t/<= (:type arg|analyzed)
                                                     (get input-types i))))
                                           seq)]
                              (-> ret
                                  (update :arg-nodes conj arg|analyzed)
                                  (assoc :satisfying-overloads-seq satisfying-overloads-seq'
                                         :out-type
                                           (when (= i (dec args-ct))
                                             (-> satisfying-overloads-seq'
                                                 first
                                                 :output-type))))
                              (err! "No overloads satisfy the arguments"
                                    {:caller caller|node
                                     :args body}))
                            (update ret :arg-nodes conj arg|analyzed)))
                      {:arg-nodes []
                       ;; We could do a little smarter analysis here but we'll keep it simple for
                       ;; now
                       :out-type (when-not (= :fnt caller-kind) t/any?)
                       :satisfying-overloads-seq
                         (when (= :fnt caller-kind)
                           (-> caller|type
                               utr/fn-type>arities
                               (get args-ct)))})))
    :satisfying-overloads-seq))

(defns- analyze-seq*
  "Analyze a seq after it has been macro-expanded.
   The ->`form` is post- incremental macroexpansion."
  [env ::env, [caller|form _ & body _ :as form] _]
  (log/pr!)
  (ifs (special-symbols caller|form)
       (case caller|form
         do       (analyze-seq|do    env form)
         let*     (analyze-seq|let*  env form)
         deftype* (TODO "deftype*")
         fn*      (TODO "fn*")
         def      (TODO "def")
         .        (analyze-seq|dot   env form)
         if       (analyze-seq|if    env form)
         quote    (analyze-seq|quote env form)
         new      (analyze-seq|new   env form)
         throw    (analyze-seq|throw env form))
       ;; TODO support recursion
       (let [caller|node (analyze* env caller|form)
             _ (ppr caller|node)
             caller|type (:type caller|node)
             args-ct     (count body)]
               ;; TODO fix this line of code and extend t/compare so the comparison checks below
               ;;      will work with t/fn
         (case (if (utr/fn-type? caller|type)
                   -1
                   (t/compare caller|type t/callable?))
           (1 2)  (err! "It is not known whether form can be called" {:node caller|node})
           3      (err! "Form cannot be called" {:node caller|node})
           (-1 0) (let [caller-kind
                          (ifs (utr/fn-type? caller|type)             :fnt
                               (t/<= caller|type t/keyword?)          :keyword
                               (t/<= caller|type t/+map|built-in?)    :map
                               (t/<= caller|type t/+vector|built-in?) :vector
                               (t/<= caller|type t/+set|built-in?)    :set
                               (t/<= caller|type t/fn?)               :fn
                               ;; If it's callable but not fn, we might have missed something in
                               ;; this dispatch so for now we throw
                               (err! "Don't know how how to handle non-fn callable"
                                     {:caller caller|node}))
                        assert-valid-args-ct
                          (case caller-kind
                            (:keyword :map)
                              (when-not (or (= args-ct 1) (= args-ct 2))
                                (err! (str "Keywords and `clojure.core` persistent maps must be "
                                           "provided with exactly one or two args when calling "
                                           "them")
                                      {:args-ct args-ct :caller caller|node}))

                            (:vector :set)
                              (when-not (= args-ct 1)
                                 (err! (str "`clojure.core` persistent vectors and `clojure.core` "
                                            "persistent sets must be provided with exactly one arg "
                                            "when calling them")
                                       {:args-ct args-ct :caller caller|node}))

                            :fnt
                              (when-not (-> caller|type utr/fn-type>arities (contains? args-ct))
                                (err! "Unhandled number of arguments for fnt"
                                      {:args-ct args-ct :caller caller|node}))
                              ;; For non-typed fns, unknown; we will have to risk runtime exception
                              ;; because we can't necessarily rely on metadata to tell us the
                              ;; whole truth
                            :fn nil)
                        {:keys [arg-nodes out-type]}
                          (call>arg-nodes+out-type
                            env caller|node caller|type caller-kind args-ct body)]
                    (uast/call-node
                      {:env    env
                       :form   form
                       :caller caller|node
                       :args   arg-nodes
                       :type   out-type}))))))

(defns- analyze-seq [env ::env, form _]
  (log/pr!)
  (let [expanded-form (ufeval/macroexpand form)]
    (if (ucomp/== form expanded-form)
        (analyze-seq* env expanded-form)
        (let [expanded (analyze-seq* env expanded-form)]
          (uast/macro-call
            {:env           env
             :form          form
             :expanded-form (:form expanded)
             :expanded      expanded})))))

(defns ?resolve-with-env [sym t/symbol?, env ::env]
  (if-let [[_ local] (find env sym)]
    {:value local}
    (let [resolved (ns-resolve *ns* sym)]
      (log/ppr :warn "Not sure how to handle non-local symbol; resolved it for now"
                     (kw-map sym resolved))
      (ifs resolved
             {:value resolved}
           (some-> sym namespace symbol resolve class?)
             {:value (analyze-seq|dot env (list '. (-> sym namespace symbol) (-> sym name symbol)))}
           nil))))

(defns- analyze-symbol [env ::env, form t/symbol?]
  (log/pr!)
  (if-not-let [{resolved :value} (?resolve-with-env form env)]
    (err! "Could not resolve symbol" {:sym form})
    (uast/symbol env form resolved
      (ifs (uast/node? resolved)
             (:type resolved)
           (or (t/literal? resolved) (t/class? resolved))
             (t/value resolved)
           (var? resolved)
             (or (-> resolved meta ::t/type) (t/value @resolved))
           (utpred/unbound? resolved)
             ;; Because the var could be anything and cannot have metadata (type or otherwise)
             t/any?
           (TODO "Unsure of what to do in this case" (kw-map env form resolved))))))

(defns- analyze* [env ::env, form _]
  (log/pr! form)
  (when (> (swap! *analyze-depth inc) 100) (throw (ex-info "Stack too deep" {:form form})))
  (ifs (symbol? form)
         (analyze-symbol env form)
       (t/literal? form)
         (uast/literal env form (t/>type form))
       (or (vector? form)
           (set?    form))
         (analyze-non-map-seqable env form (empty form) (fn stop [& [a b :as args]] (prl! args) (err! "STOP")))
       (map? form)
         (analyze-map env form)
       (seq? form)
         (analyze-seq env form)
       (throw (ex-info "Unrecognized form" {:form form}))))

(defns analyze
  ([form _] (analyze {} form))
  ([env ::env, form _]
    (log/pr! form)
    (reset! *analyze-depth 0)
    (analyze* env form)))
