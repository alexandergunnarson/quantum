(ns quantum.untyped.core.analyze
  (:require
    ;; TODO excise this reference
    [quantum.core.type.core                 :as tcore]
    [quantum.untyped.core.analyze.ast       :as uast]
    [quantum.untyped.core.analyze.expr      :as uxp]
    [quantum.untyped.core.collections       :as c
      :refer [>vec]]
    [quantum.untyped.core.collections.logic :as clogic]
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
    [quantum.untyped.core.form.type-hint    :as ufth]
    [quantum.untyped.core.identifiers       :as uident
      :refer [>symbol]]
    [quantum.untyped.core.log               :as log
      :refer [prl!]]
    [quantum.untyped.core.logic             :as l
      :refer [if-not-let ifs]]
    [quantum.untyped.core.print
      :refer [ppr]]
    [quantum.untyped.core.reducers          :as r
      :refer [educe reducei]]
    [quantum.untyped.core.spec              :as s]
    [quantum.untyped.core.type              :as t
      :refer [?]]
    [quantum.untyped.core.type.compare      :as utcomp]
    [quantum.untyped.core.type.reifications :as utr]
    [quantum.untyped.core.vars              :as uvar
      :refer [update-meta]]))

(def special-metadata-keys #{:val})

;; TODO move?
(defns class>type
  "For converting a class in a reflective method, constructor, or field declaration to a type.
   Unlike `t/isa?`, takes into account that non-primitive classes in Java aren't guaranteed to be
   non-null."
  [x class? > t/type?]
  (let [matching-boxed-class (t/unboxed-class->boxed-class x)]
    (-> (or matching-boxed-class x) t/isa? (cond-> (not matching-boxed-class) t/?))))

(defn- assume-val-for-form? [form] (-> form meta :val true?))

(defns- maybe-with-assume-val [c class?, form _ > t/type?]
  (let [matching-boxed-class (t/unboxed-class->boxed-class c)]
    (-> (or matching-boxed-class c)
        t/isa?
        (cond-> (and (not matching-boxed-class) (not (assume-val-for-form? form))) t/?))))

;; TODO move?
(defns- compare-class-specificity [c0 class?, c1 class?]
  (case (utcomp/compare|class+class* c0 c1)
    -1     -1
    (0 2 3) 0
     1      1))

;; ----- Reflection support ----- ;;

#?(:clj
(defrecord Method
  [^String name ^Class out-class ^"[Ljava.lang.Class;" arg-classes ^clojure.lang.Keyword kind]
  fipp.ednize/IOverride
  fipp.ednize/IEdn (-edn [this] (tagged-literal (symbol "M") (into (array-map) this)))))

#?(:clj (defns method? [x _] (instance? Method x)))

#?(:clj
(defns class>methods
  "Returns all the public methods associated with a class, as a map from:
   method-name -> arg-count -> kind=static|instance -> methods"
  [^Class c class? > map?]
  (let [with-most-specific-out-class
          (fn->> (ucomp/comp-min-of
                   (fn [m0 m1] (compare-class-specificity (:out-class m0) (:out-class m1)))))
        ;; We have to use `with-distinct-arg-class-seqs` Because even though it's not supposed to
        ;; be the case that there is ever more than one method with the same combination of name,
        ;; kind (static/instance), and arg classes, only differing by return type, it *has*
        ;; happened on Java version "1.8.0_162", Mac OS X, JVM version "25.162-b12", with the
        ;; `ByteBuffer.array()` public instance method which maps to two overloads, one which
        ;; returns `Object` and one which returns `byte[]`.
        ;; See also this link for the claim that this is impossible according to the Java 1.8 spec
        ;; (http://docs.oracle.com/javase/specs/jls/se8/html/jls-8.html#jls-8.4.2) and that the bug
        ;; only exists in Java 6 or 7 on Oracle's JDK, OpenJDK, and IBM's JDK: https://stackoverflow.com/questions/5561436/can-two-java-methods-have-same-name-with-different-return-types
        with-distinct-arg-class-seqs
          (fn->> (c/group-by (fn-> :arg-classes vec))
                 vals
                 (c/map with-most-specific-out-class))]
    (->> (.getMethods c)
         (c/map+ (fn [^java.lang.reflect.Method x]
                   (Method. (.getName x) (.getReturnType x) (.getParameterTypes x)
                     (if (java.lang.reflect.Modifier/isStatic (.getModifiers x))
                         :static
                         :instance))))
         (c/group-by (fn [^Method x] (:name x))) ; TODO all of these need to be into !vector and !hash-map
         (c/map-vals+
           (fn->> (c/group-by (fn [^Method x] (count (:arg-classes x))))
                  (c/map-vals+
                    (fn->> (c/group-by (fn [^Method x] (:kind x)))
                           (c/map-vals+ with-distinct-arg-class-seqs)
                           (r/join {})))
                  (r/join {})))
         (r/join {})))))


(defonce class>methods|with-cache
  (memoize (fn [c] (class>methods c))))

#?(:clj
(defrecord Constructor [^"[Ljava.lang.Class;" arg-classes]
  fipp.ednize/IOverride
  fipp.ednize/IEdn (-edn [this] (tagged-literal (symbol "C") {:arg-classes (vec arg-classes)}))))

#?(:clj (defns constructor? [x _] (instance? Constructor x)))

#?(:clj
(defns class>constructors
  "Returns all the public constructors associated with a class, as a vector."
  [^Class c class? > vector?]
  (->> (.getConstructors c)
       (c/map (fn [^java.lang.reflect.Constructor x] (Constructor. (.getParameterTypes x)))))))

(defonce class>constructors|with-cache
  (memoize (fn [c] (class>constructors c))))

#?(:clj
(defrecord Field [^String name ^Class class ^clojure.lang.Keyword kind]
  fipp.ednize/IOverride
  fipp.ednize/IEdn (-edn [this] (tagged-literal (symbol "F") (into (array-map) this)))))

#?(:clj
(defns class>fields
  "Returns all the public fields associated with a class, as a map from field name to field."
  [^Class c class? > map?]
  (->> (.getFields c)
       (c/map+ (fn [^java.lang.reflect.Field x]
                 [(.getName x)
                  (Field. (.getName x) (.getType x)
                    (if (java.lang.reflect.Modifier/isStatic (.getModifiers x))
                        :static
                        :instance))]))
       (r/join {})))) ; TODO !hash-map

#?(:clj
(def class>fields|with-cache
  (memoize (fn [c] (class>fields c)))))

;; ----- End reflection support ----- ;;

(defonce *analyze-depth (atom 0))

(defn add-file-context-from [to from]
  (let [{:keys [line column]} (meta from)]
    (update-meta to
      #(cond-> % line   (assoc :line   line)
                 column (assoc :column column)))))

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

(s/def ::env (s/map-of symbol? t/any?))

(s/def ::opts (s/map-of keyword? t/any?))

(declare analyze*)

(defns- analyze-non-map-seqable
  "Analyzes a non-map seqable."
  {:params-doc
    '{merge-types-fn "2-arity fn that merges two types (or sets of types).
                      The first argument is the current deduced type of the
                      overall AST node; the second is the deduced type of
                      the current sub-AST-node."}}
  [opts ::opts, env ::env, form _, empty-form _, rf _]
  (-> (reducei
        (fn [accum form' i] (rf accum (analyze* opts (:env accum) form') i))
        {:env env :form (transient empty-form) :body (transient [])}
        form)
      (update :form (fn-> persistent! (add-file-context-from form)))
      (update :body persistent!)))

(defns- analyze-map
  {:todo #{"If the map is bound to a variable, preserve type info for it such that lookups
            can start out with a guarantee of a certain type."}}
  [opts ::opts, env ::env, form _]
  (TODO "analyze-map")
  #_(->> form
       (reduce-kv (fn [{env' :env forms :form} form'k form'v]
                    (let [ast-node-k (analyze* opts env' form'k)
                          ast-node-v (analyze* opts env' form'v)]
                      (->expr-info {:env       env'
                                    :form      (assoc! forms (:form ast-node-k) (:form ast-node-v))
                                   ;; TODO fix; we want the types of the keys and vals to be deduced
                                    :type-info nil})))
         (->expr-info {:env env :form (transient {})}))
       (persistent!-and-add-file-context-from form)))

(defns- analyze-seq|do [opts ::opts, env ::env, [_ _ & body|form _ :as form] _ > uast/do?]
  (if (empty? body|form)
      (uast/do {:env             env
                :unexpanded-form form
                :form            form
                :body            []
                :type            t/nil?})
      (let [{expanded-form :form body :body}
              (analyze-non-map-seqable opts env body|form []
                (fn [accum ast-data _]
                  ;; The env should be the same as whatever it was originally because no new scopes
                  ;; are created
                  (-> accum
                      (update :form conj! (:form ast-data))
                      (update :body conj! ast-data))))]
        (uast/do {:env             env
                  :unexpanded-form form
                  :form            (with-meta (list* 'do expanded-form) (meta expanded-form))
                  :body            body
                  ;; To types, only the last sub-AST-node ever matters, as each is independent from
                  ;; the others
                  :type            (-> body c/last :type)}))))

(defns analyze-seq|let*|bindings [opts ::opts, env ::env, bindings|form _]
  (->> bindings|form
       (c/partition-all+ 2)
       (reduce (fn [{env' :env !bindings :form :keys [bindings-map]} [sym form :as binding|form]]
                 (let [node (analyze* opts env' form)] ; environment is additive with each binding
                   {:env          (assoc env' sym node)
                    :form         (conj! (conj! !bindings sym) (:form node))
                    :bindings-map (assoc bindings-map sym node)}))
         {:env env :form (transient []) :bindings-map {}})
       (<- (update :form (fn-> persistent! (add-file-context-from bindings|form))))))

(defns analyze-seq|let*
  [opts ::opts, env ::env, [_ _, bindings|form _ & body|form _ :as form] _ > uast/let*?]
  (let [{env' :env bindings|form' :form :keys [bindings-map]}
          (analyze-seq|let*|bindings opts env bindings|form)
        {body|form' :form body|type :type body :body}
          (analyze-seq|do opts env' (list* 'do body|form))]
    (uast/let* {:env             env
                :unexpanded-form form
                :form            (list* 'let* bindings|form' (rest body|form'))
                :bindings        bindings-map
                :body            body
                :type            body|type})))

#?(:clj
(defns ?cast-call->type
  "Given a cast call like `clojure.lang.RT/uncheckedBooleanCast`, returns the
   corresponding type.

   Unchecked fns could be assumed to actually *want* to shift the range over if the
   range hits a certain point, but we do not make that assumption here."
  [c class?, method symbol? > (? t/type?)]
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

(defns- call-sites>most-specific
  "Time complexity = O(m•n) where m = # of call sites and n = # of args per call site."
  [call-sites (s/vec-of t/any? #_(s/array-of class?)) > (s/vec-of t/any? #_(s/array-of class?))]
  (let [^"[Ljava.lang.Object;" sample-arg-classes (-> call-sites first :arg-classes)
        args-ct (alength sample-arg-classes)]
    (->> (range args-ct)
         (reduce
           (fn [call-sites' i]
             (->> call-sites'
                  (c/map+ (fn [{:keys [^"[Ljava.lang.Object;" arg-classes]}] (aget arg-classes i)))
                  (ucomp/comp-mins-of compare-class-specificity)))
           call-sites))))

(defns- analyze-seq|method-or-constructor-call|incrementally-analyze
  [opts ::opts, env ::env, form _, target-class class?, args|form _, call-sites-for-ct _
   kinds-str string? > (s/kv {:args|analyzed vector?})]
  (let [{:as ret :keys [call-sites args|analyzed]}
          (->> args|form
               (reducei
                 (fn [{:as ret :keys [args|analyzed call-sites]} arg|form i|arg]
                   (let [arg|analyzed (analyze* opts env arg|form)
                         arg|analyzed|type (:type arg|analyzed)
                         call-sites'
                           (->> call-sites
                                (c/filter
                                  (fn [{:keys [^"[Ljava.lang.Object;" arg-classes]}]
                                    (t/<= arg|analyzed|type
                                          (class>type (aget arg-classes i|arg))))))]
                     (if (empty? call-sites')
                         (err! (str "No " kinds-str " for class match the arg type at index")
                               {:class             target-class
                                :form              form
                                :arg|type          arg|analyzed|type
                                :arg|analyzed-form (:form arg|analyzed)
                                :i|arg             i|arg
                                :arg-types
                                  (vec (concat (mapv :type args|analyzed)
                                               [arg|analyzed|type]
                                               (repeat (- (count args|form)
                                                          (count args|analyzed))
                                                       :unanalyzed)))})
                         (-> ret
                             (assoc :call-sites call-sites')
                             (update :args|analyzed conj arg|analyzed)))))
                 {:call-sites call-sites-for-ct :args|analyzed []}))
        call-sites (cond-> call-sites (-> call-sites count (> 1)) call-sites>most-specific)]
    (if (-> call-sites count (> 1))
        (err! (str "Multiple, equally specific " kinds-str " for class match the arg types")
              {:class              target-class
               :form               form
               (keyword kinds-str) call-sites
               :arg-types          (mapv :type args|analyzed)})
        ret)))

(defns- analyze-seq|dot|method-call|incrementally-analyze
  [opts ::opts, env ::env, form _, target uast/node?, target-class class?, method-form _
   args|form _ methods-for-ct-and-kind (s/seq-of t/any?) > uast/method-call?]
  (let [{:keys [args|analyzed call-sites]}
          (analyze-seq|method-or-constructor-call|incrementally-analyze opts env form target-class
            args|form methods-for-ct-and-kind "methods")
        ?cast-type (?cast-call->type target-class method-form)
        ;; TODO enable the below:
        ;; (s/validate (-> with-ret-type :args first :type) #(t/>= % (t/numerically ?cast-type)))
        _ (when ?cast-type
            (log/ppr :warn
              "Not yet able to statically validate whether primitive cast will succeed at runtime"
              {:form form}))]
    (uast/method-call
      {:env    env
       :form   form
       :target target
       :method method-form
       :args   args|analyzed
       :type   (-> call-sites first :out-class (maybe-with-assume-val form))})))

(defns- analyze-seq|dot|method-call
  "A note will be made of what methods match the argument types.
   If only one method is found, that is noted too. If no matching method is found, an
   exception is thrown."
  [opts ::opts, env ::env, form _, target uast/node?, target-class class?, static? t/boolean?
   method-form simple-symbol?, args|form _ #_(seq-of form?) > uast/method-call?]
  ;; TODO cache type by method
  (if-not-let [methods-for-name (-> target-class class>methods|with-cache
                                    (c/get (name method-form)))]
    (if (empty? args|form)
        (err! "No such method or field in class" {:class target-class :method-or-field method-form})
        (err! "No such method in class"          {:class target-class :methods        method-form}))
    (if-not-let [methods-for-ct (c/get methods-for-name (c/count args|form))]
      (err! "Incorrect number of arguments for method"
            {:class           target-class
             :method          method-form
             :possible-counts (->> methods-for-name keys (apply sorted-set))})
      (let [[kind non-kind] (if static? [:static :instance] [:instance :static])]
        (if-not-let [methods-for-ct-and-kind (c/get methods-for-ct kind)]
          (err! (istr "Method found for arg-count, but was ~non-kind, not ~kind")
                {:class target-class :method method-form :args args|form})
          (analyze-seq|dot|method-call|incrementally-analyze opts env form target target-class
            method-form args|form methods-for-ct-and-kind))))))

(defns- analyze-seq|dot|field-access
  [opts ::opts, env ::env, form _, target _, field-form simple-symbol?, field (t/isa? Field)
   > uast/field-access?]
  (uast/field-access
    {:env    env
     :form   form
     :target target
     :field  field-form
     :type   (-> field :class (maybe-with-assume-val form))}))

(defns classes>class
  "Ensure that given a set of classes, that set consists of at most a class C and nil.
   If so, returns C. Otherwise, throws."
  [cs (s/set-of (s/nilable class?)) > class?]
  (let [cs' (disj cs nil)]
    (if (-> cs' count (= 1))
        (first cs')
        (err! "Found more than one class" cs))))

;; TODO type these arguments; e.g. check that ?method||field, if present, is an unqualified symbol
(defns- analyze-seq|dot
  [opts ::opts, env ::env, [_ _, target-form _, ?method-or-field _ & ?args _ :as form] _]
  (let [target          (analyze* opts env target-form)
        method-or-field (if (symbol? ?method-or-field) ?method-or-field (first ?method-or-field))
        args-forms      (if (symbol? ?method-or-field) ?args            (rest  ?method-or-field))]
    (if (t/= (:type target) t/nil?)
        (err! "Cannot use the dot operator on a target of nil type." {:form form})
        (let [;; `nilable?` because technically any non-primitive in Java is nilable and we can't
              ;; necessarily rely on all e.g. "@nonNull" annotations
              {:as ?target-static-class-map
               target-static-class          :class
               target-static-class-nilable? :nilable?}
                (-> target :type t/type>?class-value)
              target-classes
                (if ?target-static-class-map
                    (cond-> #{target-static-class} target-static-class-nilable? (conj nil))
                    (-> target :type t/type>classes))
              target-class (classes>class target-classes)]
          (if-let [target-class-nilable? (contains? target-classes nil)]
            (err! "Cannot use the dot operator on a target that might be nil."
                  {:form form :target-type (:type target)})
            (if-let [field (and (empty? args-forms)
                                (-> target-class class>fields|with-cache
                                    (c/get (name method-or-field))))]
              (analyze-seq|dot|field-access opts env form target method-or-field field)
              (analyze-seq|dot|method-call opts env form target target-class
                (boolean ?target-static-class-map) method-or-field args-forms)))))))

;; TODO this is not the right approach for CLJS
(defns- analyze-seq|new
  [opts ::opts, env ::env, [_ _ & [c|form _ & args|form _ :as body] _ :as form] _ > uast/new-node?]
  (let [c|analyzed (analyze* opts env c|form)]
    (if-not (and (-> c|analyzed :type t/value-type?)
                 (-> c|analyzed :type utr/value-type>value class?))
      (err! "Supplied non-class to `new` form" {:form form})
      (let [c (-> c|analyzed :type utr/value-type>value)
            constructors (-> c class>constructors|with-cache)
            args-ct (count args|form)
            constructors-for-ct (->> constructors
                                     (c/filter (fn [{:keys [^"[Ljava.lang.Object;" arg-classes]}]
                                                 (= (alength arg-classes) args-ct))))]
        (if (empty? constructors-for-ct)
            (err! "No constructors for class match the arg ct" {:class c :args|form args|form})
            (let [{:keys [args|analyzed call-sites]}
                    (analyze-seq|method-or-constructor-call|incrementally-analyze opts env form c
                      args|form constructors-for-ct "constructors")]
              (uast/new-node
                {:env   env
                 :form  (list* 'new c|form (map :form args|analyzed))
                 :class c
                 :args  args|analyzed
                 :type  (t/isa? c)})))))))

;; TODO move this
(defns truthy-node? [{:as ast t [:type _]} _ > (t/? t/boolean?)]
  (ifs (or (t/= t t/nil?) (t/= t t/false?)) false
       (or (t/> t t/nil?) (t/> t t/false?)) nil ; representing "unknown"
       true))

(defns- analyze-seq|if
  "Performs conditional branch pruning."
  [opts ::opts, env ::env, [_ _ & [pred-form _, true-form _, false-form _ :as body] _ :as form] _
   > uast/node?]
  (if-not (<= 2 (count body) 3)
    (err! "`if` accepts exactly 3 arguments: one predicate test and two branches; received"
          {:body body})
    (let [pred-node  (analyze* opts env pred-form)
          true-node  (delay (analyze* opts env true-form))
          false-node (delay (analyze* opts env false-form))
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
        true  (do (log/ppr :warn "Predicate in `if` node is always true" {:pred pred-form})
                  (assoc @true-node :env env))
        false (do (log/ppr :warn "Predicate in `if` node is always false" {:pred pred-form})
                  (assoc @false-node :env env))
        nil   @whole-node))))

(defns- analyze-seq|quote [opts ::opts, env ::env, [_ _ & body _ :as form] _ > uast/quoted?]
  (uast/quoted env form (t/value (list* body))))

(defns- analyze-seq|throw [opts ::opts, env ::env, form _ [arg _ :as body] _ > uast/throw-node?]
  (if (-> body count (not= 1))
      (err! "Must supply exactly one input to `throw`; supplied" {:body body})
      (let [arg|analyzed (analyze* opts env arg)]
        ;; TODO this is not quite true for CLJS but it's good practice at least
        (if-not (-> arg|analyzed :type (t/<= t/throwable?))
          (err! "`throw` requires a throwable; received" {:arg arg :type (:type arg|analyzed)})
          (uast/throw-node
            {:env  env
             :form (list 'throw (:form arg|analyzed))
             :arg  arg|analyzed
             ;; `t/none?` because nothing is actually returned
             :type t/none?})))))

(defn- filter-dynamic-dispatchable-overloads
  "An example of dynamic dispatch:
   - When we call `seq` on an input of type `(t/? (t/isa? java.util.Set))`, direct dispatch will
     fail as it is not `t/<=` to any overload (including `t/iterable?` which is the only one under
     which `(t/isa? java.util.Set)` falls).
     However since all branches of the `t/or` are guaranteed to result in a successful dispatch
     (i.e. `t/nil?` and `t/iterable?`) then dynamic dispatch will go forward without an error."
  [{:as ret :keys [dispatchable-overloads-seq]} input|analyzed i caller|node body]
  (if (-> input|analyzed :type utr/or-type?)
      (let [or-types (-> input|analyzed :type utr/or-type>args)
            {:keys [dispatchable-overloads-seq' non-dispatchable-or-types]}
              (->> dispatchable-overloads-seq
                   (reduce
                     (fn [ret {:as overload :keys [input-types]}]
                       (if-let [or-types-that-match
                                  (->> or-types (c/lfilter #(t/<= % (get input-types i))) seq)]
                         (-> ret
                             (update :dispatchable-overloads-seq' conj overload)
                             (update :non-dispatchable-or-types
                               #(apply disj % or-types-that-match)))
                         ret))
                     {:dispatchable-overloads-seq' []
                      :non-dispatchable-or-types (set or-types)}))]
        (if (or (empty? dispatchable-overloads-seq')
                (c/contains? non-dispatchable-or-types))
            (err! "No overloads satisfy the inputs, whether direct or dynamic"
                  {:caller             caller|node
                   :inputs             body
                   :failing-input-form (:form input|analyzed)
                   :failing-input-type (:type input|analyzed)})
            (assoc ret :dispatchable-overloads-seq dispatchable-overloads-seq'
                       :dispatch-type              :dynamic)))
      (err! "Cannot currently do a dynamic dispatch on a non-`t/or` input type"
            {:input|analyzed input|analyzed})))

(defn- filter-direct-dispatchable-overloads
  [{:as ret :keys [dispatchable-overloads-seq]} input|analyzed i caller|node body]
  (if-let [dispatchable-overloads-seq'
            (->> dispatchable-overloads-seq
                 (c/lfilter
                   (fn [{:keys [input-types]}]
                     (t/<= (:type input|analyzed) (get input-types i))))
                 seq)]
    (assoc ret :dispatchable-overloads-seq dispatchable-overloads-seq')
    (filter-dynamic-dispatchable-overloads ret input|analyzed i caller|node body)))

(defn- >dispatch|out-type [dispatch-type dispatchable-overloads-seq]
  (case dispatch-type
    :direct  (-> dispatchable-overloads-seq first :output-type)
    :dynamic (->> dispatchable-overloads-seq
                  (c/lmap :output-type)
                  ;; Technically we could do a complex conditional instead of a simple `t/or` but
                  ;; no need
                  (apply t/or))))

(defns- call>input-nodes+out-type
  [opts ::opts, env ::env, caller|node _, caller|type _, caller-kind _, inputs-ct _, body _
   > (s/kv {:input-nodes t/any? #_(s/seq-of ast/node?)
            :out-type  t/type?})]
  (dissoc
    (if (zero? inputs-ct)
        {:input-nodes []
         :out-type
           (if (= :fnt caller-kind)
               (-> caller|type utr/fn-type>arities (get inputs-ct) first :output-type)
               ;; We could do a little smarter analysis here but we'll keep it simple for now
               t/any?)}
        (->> body
             (c/map+ #(analyze* opts env %))
             (reducei
               (fn [{:as ret :keys [dispatch-type]} input|analyzed i]
                 (if (= :fnt caller-kind)
                     (let [{:as ret' :keys [dispatchable-overloads-seq]}
                             (case dispatch-type
                               :direct  (filter-direct-dispatchable-overloads
                                          ret input|analyzed i caller|node body)
                               :dynamic (filter-dynamic-dispatchable-overloads
                                          ret input|analyzed i caller|node body))]
                       (-> ret'
                           (update :input-nodes conj input|analyzed)
                           (assoc  :out-type
                                     (when-let [last-input-to-check? (= i (dec inputs-ct))]
                                       (>dispatch|out-type
                                         dispatch-type dispatchable-overloads-seq)))))
                     (update ret :input-nodes conj input|analyzed)))
                 {:input-nodes   []
                  ;; We could do a little smarter analysis here but we'll keep it simple for now
                  :out-type      (when-not (= :fnt caller-kind) t/any?)
                  :dispatch-type :direct
                  :dispatchable-overloads-seq
                    (when (= :fnt caller-kind)
                      (-> caller|type
                          utr/fn-type>arities
                          (get inputs-ct)))})))
    :dispatchable-overloads-seq))

(defns- analyze-seq*
  "Analyze a seq after it has been macro-expanded.
   The ->`form` is post- incremental macroexpansion."
  [opts ::opts, env ::env, [caller|form _ & body _ :as form] _ > uast/node?]
  (case caller|form
    do       (analyze-seq|do    opts env form)
    let*     (analyze-seq|let*  opts env form)
    deftype* (TODO "deftype*")
    fn*      (TODO "fn*")
    def      (TODO "def")
    .        (analyze-seq|dot   opts env form)
    if       (analyze-seq|if    opts env form)
    quote    (analyze-seq|quote opts env form)
    new      (analyze-seq|new   opts env form)
    throw    (analyze-seq|throw opts env form)
    (let [caller|node (analyze* opts env caller|form)
          caller|type (:type caller|node)
          inputs-ct   (count body)]
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
                     assert-valid-inputs-ct
                       (case caller-kind
                         (:keyword :map)
                           (when-not (or (= inputs-ct 1) (= inputs-ct 2))
                             (err! (str "Keywords and `clojure.core` persistent maps must be "
                                        "provided with exactly one or two inputs when calling "
                                        "them")
                                   {:inputs-ct inputs-ct :caller caller|node}))

                         (:vector :set)
                           (when-not (= inputs-ct 1)
                              (err! (str "`clojure.core` persistent vectors and `clojure.core` "
                                         "persistent sets must be provided with exactly one "
                                         "input when calling them")
                                    {:inputs-ct inputs-ct :caller caller|node}))

                         :fnt
                           (when-not (-> caller|type utr/fn-type>arities (contains? inputs-ct))
                             (err! "Unhandled number of inputs for fnt"
                                   {:inputs-ct inputs-ct :caller caller|node}))
                           ;; For non-typed fns, unknown; we will have to risk runtime exception
                           ;; because we can't necessarily rely on metadata to tell us the
                           ;; whole truth
                         :fn nil)
                     {:keys [input-nodes out-type]}
                       (call>input-nodes+out-type
                         opts env caller|node caller|type caller-kind inputs-ct body)
                     call-node
                       (uast/call-node
                         {:env    env
                          :form   form
                          :caller caller|node
                          :args   input-nodes
                          :type   out-type})]
                 call-node)))))

(defns- analyze-seq [opts ::opts, env ::env, form _]
  (let [expanded-form (ufeval/macroexpand form)]
    (if-let [no-expansion? (ucomp/== form expanded-form)]
      (analyze-seq* opts env expanded-form)
      (let [expanded-form' (-> expanded-form (update-meta merge (meta form)))
            expanded (analyze* opts env expanded-form')]
        (uast/macro-call
          {:env             env
           :unexpanded-form form
           :form            (:form expanded)
           :expanded        expanded
           :type            (:type expanded)})))))

(defns ?resolve-with-env [opts ::opts, env ::env, sym symbol?]
  (if-let [[_ local] (find env sym)]
    {:value local}
    (let [resolved (ns-resolve *ns* sym)]
      (ifs resolved
             {:value resolved}
           (some-> sym namespace symbol resolve class?)
             {:value (analyze-seq|dot
                       opts env (list '. (-> sym namespace symbol) (-> sym name symbol)))}
           nil))))

(defns- analyze-symbol [opts ::opts, env ::env, form symbol? > uast/symbol?]
  (if-not-let [{resolved :value} (?resolve-with-env opts env form)]
    (err! "Could not resolve symbol" {:sym form})
    (uast/symbol env form resolved
      (ifs (uast/node? resolved)
             (:type resolved)
           (or (t/literal? resolved) (class? resolved))
             (t/value resolved)
           (var? resolved)
             (or (-> resolved meta :quantum.core.type/type) (t/value @resolved))
           (uvar/unbound? resolved)
             ;; Because the var could be anything and cannot have metadata (type or otherwise)
             t/any?
           (TODO "Unsure of what to do in this case" (kw-map env form resolved))))))

(defns- analyze* [opts ::opts, env ::env, form _ > uast/node?]
  (when (> (swap! *analyze-depth inc) 200) (throw (ex-info "Stack too deep" {:form form})))
  (ifs (symbol? form)
         (analyze-symbol opts env form)
       (t/literal? form)
         (uast/literal env form (t/>type form))
       (or (vector? form)
           (set?    form))
         (analyze-non-map-seqable opts env form (empty form) (fn stop [& [a b :as args]] (prl! args) (err! "STOP")))
       (map? form)
         (analyze-map opts env form)
       (seq? form)
         (analyze-seq opts env form)
       (throw (ex-info "Unrecognized form" {:form form}))))

(defns analyze
  "Special metadata directives are defined in `special-metadata-keys`. They include:
   - `:val` : Causes the analyzer to assume that the return value of the dot-form satisfies
              `t/val?`. Useful for doing method/dot-chaining in which the methods return
              non-primitives."
  > uast/node?
  ([form _] (analyze {} form))
  ([env ::env, form _] (analyze {} env form))
  ([opts ::opts, env ::env, form _]
    (reset! *analyze-depth 0)
    (analyze* opts env form)))
