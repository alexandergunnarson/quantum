(ns quantum.untyped.core.analyze
  (:require
    ;; TODO excise this reference
    [quantum.core.type.core                 :as tcore]
    [quantum.untyped.core.analyze.ast       :as uast]
    [quantum.untyped.core.analyze.expr      :as uxp]
    [quantum.untyped.core.collections       :as uc
      :refer [>vec]]
    [quantum.untyped.core.collections.logic :as clogic]
    [quantum.untyped.core.compare           :as ucomp]
    [quantum.untyped.core.core
      :refer [istr]]
    [quantum.untyped.core.data
      :refer [kw-map]]
    [quantum.untyped.core.data.reactive     :as urx]
    [quantum.untyped.core.data.set          :as uset]
    [quantum.untyped.core.defnt
      :refer [defns defns- fns]]
    [quantum.untyped.core.error             :as uerr
      :refer [TODO err!]]
    [quantum.untyped.core.fn
      :refer [<- fn-> fn->> fn1]]
    [quantum.untyped.core.form              :as uform]
    [quantum.untyped.core.form.evaluate     :as ufeval]
    [quantum.untyped.core.form.type-hint    :as ufth]
    [quantum.untyped.core.identifiers       :as uid]
    [quantum.untyped.core.log               :as log
      :refer [prl!]]
    [quantum.untyped.core.logic             :as l
      :refer [if-not-let ifs]]
    [quantum.untyped.core.print
      :refer [ppr]]
    [quantum.untyped.core.reducers          :as ur
      :refer [educe join reducei]]
    [quantum.untyped.core.refs              :as uref
      :refer [>!thread-local]]
    [quantum.untyped.core.spec              :as us]
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
  (case (utcomp/compare|class+class*
          (or (t/unboxed-class->boxed-class c0) c0)
          (or (t/unboxed-class->boxed-class c1) c1))
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
          (fn->> (uc/group-by (fn-> :arg-classes vec))
                 vals
                 (uc/map with-most-specific-out-class))]
    (->> (.getMethods c)
         (uc/map+ (fn [^java.lang.reflect.Method x]
                    (Method. (.getName x) (.getReturnType x) (.getParameterTypes x)
                      (if (java.lang.reflect.Modifier/isStatic (.getModifiers x))
                          :static
                          :instance))))
         (uc/group-by (fn [^Method x] (:name x))) ; TODO all of these need to be into !vector and !hash-map
         (uc/map-vals+
           (fn->> (uc/group-by (fn [^Method x] (count (:arg-classes x))))
                  (uc/map-vals+
                    (fn->> (uc/group-by (fn [^Method x] (:kind x)))
                           (uc/map-vals+ with-distinct-arg-class-seqs)
                           (ur/join {})))
                  (ur/join {})))
         (ur/join {})))))


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
       (uc/map (fn [^java.lang.reflect.Constructor x] (Constructor. (.getParameterTypes x)))))))

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
       (uc/map+ (fn [^java.lang.reflect.Field x]
                  [(.getName x)
                   (Field. (.getName x) (.getType x)
                     (if (java.lang.reflect.Modifier/isStatic (.getModifiers x))
                         :static
                         :instance))]))
       (ur/join {})))) ; TODO !hash-map

#?(:clj
(def class>fields|with-cache
  (memoize (fn [c] (class>fields c)))))

;; ----- End reflection support ----- ;;

(defonce !!analyze-arg-syms|iter (>!thread-local 0)) ; `nneg-fixint?`

(defonce !!analyze-depth (>!thread-local 0))

(uvar/defonce !!dependent?
  "Denotes whether a dependent type was found to be used in the current arglist context."
  (>!thread-local false))

(defn add-file-context-from [to from]
  (let [{:keys [line column]} (meta from)]
    (update-meta to
      #(cond-> % line   (assoc :line   line)
                 column (assoc :column column)))))

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

(us/def ::opts (us/map-of keyword? t/any?))

(us/def ::env (us/map-of (us/or* symbol? #(= % :opts)) t/any?))

(declare analyze* analyze-arg-syms*)

;; TODO maybe just roll this into `analyze-seq|do`? Not sure yet
(defns- analyze-non-map-seqable
  "Analyzes a non-map seqable."
  {:params-doc
    '{merge-types-fn "2-arity fn that merges two types (or sets of types).
                      The first argument is the current deduced type of the
                      overall AST node; the second is the deduced type of
                      the current sub-AST-node."}}
  [env ::env, form _, empty-form _, rf _]
  (-> (reducei
        (fn [accum form' i] (rf accum (analyze* (:env accum) form') i))
          {:env env :form (transient empty-form) :body (transient [])}
        form)
      (update :form (fn-> persistent! (add-file-context-from form)))
      (update :body persistent!)))

;; TODO abstract `analyze-unkeyed` and `analyze-map`
(defns- analyze-unkeyed
  [env ::env, form _, empty-v _, built-in-type t/type?, ordered-or-unordered fn?, >ast fn?
   > uast/node?]
  (let [{:keys [all-values? nodes]}
          (->> form
               (uc/map+ (fn [form-v] (analyze* env form-v)))
               (educe (fn ([ret] ret)
                          ([{:as ret :keys [all-values?]} v]
                            (-> ret
                                (cond-> (and all-values? (-> v :type utr/value-type?))
                                  (assoc :all-values? true))
                                (update :nodes conj v))))
                      {:all-values? true :nodes []}))
        t (if all-values?
              (->> nodes
                   (uc/map+ (fn-> :type t/unvalue))
                   (join empty-v)
                   t/value)
              (t/and built-in-type (->> nodes (uc/map :type) ordered-or-unordered)))]
    (>ast {:env             env
           :unanalyzed-form form
           :form            (->> nodes (uc/map+ :form) (join empty-v)
                                 (<- (add-file-context-from form)))
           :nodes           nodes
           :type            t})))

(defns- analyze-vector [env ::env, form _ > uast/vector-node?]
  (analyze-unkeyed env form [] t/+vector|built-in? t/ordered uast/vector-node))

(defns- analyze-set [env ::env, form _ > uast/set-node?]
  (analyze-unkeyed env form #{} t/+unordered-set|built-in? t/unordered uast/set-node))

(defns- analyze-map
  {:todo #{"Should we differentiate between array map and hash map here depen. on ct of inputs?"}}
  [env ::env, form _]
  (let [{:keys [all-values? nodes]}
          (->> form
               (uc/map+ (fn [[form-k form-v]] [(analyze* env form-k) (analyze* env form-v)]))
               (educe (fn ([ret] ret)
                          ([{:as ret :keys [all-values?]} [k v :as kv]]
                            (-> ret
                                (cond-> (and all-values?
                                             (-> k :type utr/value-type?)
                                             (-> v :type utr/value-type?))
                                  (assoc :all-values? true))
                                (update :nodes conj kv))))
                      {:all-values? true :nodes []}))
        t (if all-values?
              (->> nodes
                   (uc/map+ (fn [[k v]] [(-> k :type t/unvalue) (-> v :type t/unvalue)]))
                   (join {})
                   t/value)
              (t/and t/+map|built-in?
                     (->> nodes
                          (uc/map (fn [[k v]] (t/ordered (:type k) (:type v))))
                          t/unordered)))]
    (uast/map-node
      {:env             env
       :unanalyzed-form form
       :form            (->> nodes
                             (uc/map+ (fn [[k v]] [(:form k) (:form v)]))
                             (join {})
                             (<- (add-file-context-from form)))
       :nodes           nodes
       :type            t})))

(defns- analyze-seq|do [env ::env, [_ _ & body|form _ :as form] _ > uast/do?]
  (if (empty? body|form)
      (uast/do {:env             env
                :unanalyzed-form form
                :form            form
                :body            []
                :type            t/nil?})
      (let [{analyzed-form :form body :body}
              (analyze-non-map-seqable env body|form []
                (fn [accum ast-data _]
                  ;; The env should be the same as whatever it was originally because no new scopes
                  ;; are created
                  (-> accum
                      (update :form conj! (:form ast-data))
                      (update :body conj! ast-data))))]
        (uast/do {:env             env
                  :unanalyzed-form form
                  :form            (with-meta (list* 'do analyzed-form) (meta analyzed-form))
                  :body            body
                  ;; To types, only the last sub-AST-node ever matters, as each is independent from
                  ;; the others
                  :type            (-> body uc/last :type)}))))

(defns analyze-seq|let*|bindings [env ::env, bindings|form _]
  (->> bindings|form
       (uc/partition-all+ 2)
       (reduce (fn [{env' :env !bindings :form :keys [bindings-map]} [sym form :as binding|form]]
                 (let [node (analyze* env' form)] ; environment is additive with each binding
                   {:env          (assoc env' sym node)
                    :form         (conj! (conj! !bindings sym) (:form node))
                    :bindings-map (assoc bindings-map sym node)}))
         {:env env :form (transient []) :bindings-map {}})
       (<- (update :form (fn-> persistent! (add-file-context-from bindings|form))))))

(defns analyze-seq|let*
  [env ::env, [_ _, bindings|form _ & body|form _ :as form] _ > uast/let*?]
  (let [{env' :env bindings|form' :form :keys [bindings-map]}
          (analyze-seq|let*|bindings env bindings|form)
        {body|form' :form body|type :type body :body}
          (analyze-seq|do env' (list* 'do body|form))]
    (uast/let* {:env             env
                :unanalyzed-form form
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
  [call-sites (us/vec-of t/any? #_(us/array-of class?)) > (us/vec-of t/any? #_(us/array-of class?))]
  (let [^"[Ljava.lang.Object;" sample-arg-classes (-> call-sites first :arg-classes)
        args-ct (alength sample-arg-classes)]
    (->> (range args-ct)
         (reduce
           (fn [call-sites' ^long i]
             (->> call-sites'
                  (ucomp/comp-mins-of
                    (fn [x0 x1]
                      (let [^"[Ljava.lang.Object;" cs0 (:arg-classes x0)
                            ^"[Ljava.lang.Object;" cs1 (:arg-classes x1)]
                        (compare-class-specificity (aget cs0 i) (aget cs1 i)))))))
           call-sites))))

(defns- analyze-seq|method-or-constructor-call|incrementally-analyze
  [env ::env, form _, target-class class?, args|form _, call-sites-for-ct _
   kinds-str string? > (us/kv {:args|analyzed vector?})]
  (let [{:as ret :keys [call-sites args|analyzed]}
          (->> args|form
               (reducei
                 (fn [{:as ret :keys [args|analyzed call-sites]} arg|form i|arg]
                   (let [arg|analyzed (analyze* env arg|form)
                         arg|analyzed|type (:type arg|analyzed)
                         call-sites'
                           (->> call-sites
                                (uc/filter
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
                                                          (inc (count args|analyzed)))
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
               (keyword kinds-str) (->> call-sites (uc/map #(update % :arg-classes vec)))
               :arg-types          (mapv :type args|analyzed)})
        ret)))

(defns- analyze-seq|dot|method-call|incrementally-analyze
  [env ::env, form _, target uast/node?, target-class class?, method-form _
   args|form _ methods-for-ct-and-kind (us/seq-of t/any?) > uast/method-call?]
  (let [{:keys [args|analyzed call-sites]}
          (analyze-seq|method-or-constructor-call|incrementally-analyze env form target-class
            args|form methods-for-ct-and-kind "methods")
        ?cast-type (?cast-call->type target-class method-form)
        ;; TODO enable the below:
        ;; (us/validate (-> with-ret-type :args first :type) #(t/>= % (t/numerically ?cast-type)))
        _ (when ?cast-type
            (log/ppr :warn
              "Not yet able to statically validate whether primitive cast will succeed at runtime"
              {:form form}))]
    (uast/method-call
      {:env             env
       :unanalyzed-form form
       :form            (list* '. (:form target) method-form (map :form args|analyzed))
       :target          target
       :method          method-form
       :args            args|analyzed
       :type            (-> call-sites first :out-class (maybe-with-assume-val form))})))

(defns- analyze-seq|dot|method-call
  "A note will be made of what methods match the argument types.
   If only one method is found, that is noted too. If no matching method is found, an
   exception is thrown."
  [env ::env, form _, target uast/node?, target-class class?, static? t/boolean?
   method-form simple-symbol?, args|form _ #_(seq-of form?) > uast/method-call?]
  ;; TODO cache type by method
  (if-not-let [methods-for-name (-> target-class class>methods|with-cache
                                    (uc/get (name method-form)))]
    (if (empty? args|form)
        (err! "No such method or field in class" {:class target-class :method-or-field method-form})
        (err! "No such method in class"          {:class target-class :methods        method-form}))
    (if-not-let [methods-for-ct (uc/get methods-for-name (uc/count args|form))]
      (err! "Incorrect number of arguments for method"
            {:class           target-class
             :method          method-form
             :possible-counts (->> methods-for-name keys (apply sorted-set))})
      (let [[kind non-kind] (if static? [:static :instance] [:instance :static])]
        (if-not-let [methods-for-ct-and-kind (uc/get methods-for-ct kind)]
          (err! (istr "Method found for arg-count, but was ~non-kind, not ~kind")
                {:class target-class :method method-form :args args|form})
          (analyze-seq|dot|method-call|incrementally-analyze env form target target-class
            method-form args|form methods-for-ct-and-kind))))))

(defns- analyze-seq|dot|field-access
  [env ::env, form _, target _, field-form simple-symbol?, field (t/isa? Field)
   > uast/field-access?]
  (uast/field-access
    {:env             env
     :unanalyzed-form form
     :form            (:form target)
     :target          target
     :field           field-form
     :type            (-> field :class (maybe-with-assume-val form))}))

(defns classes>class
  "Ensure that given a set of classes, that set consists of at most a class C and nil.
   If so, returns C. Otherwise, throws."
  [cs (us/set-of (us/nilable class?)) > class?]
  (let [cs' (disj cs nil)]
    (if (-> cs' count (= 1))
        (first cs')
        (err! "Found more than one class" cs))))

(defns- analyze-seq|dot
  [env ::env, [_ _, target-form _, ?method-or-field _ & ?args _ :as form] _]
  (let [target          (analyze* env target-form)
        method-or-field (if (symbol? ?method-or-field) ?method-or-field (first ?method-or-field))
        ;; To get around a weird behavior in Clojure, at least in 1.9
        method-or-field (if (and (= target-form 'clojure.lang.RT)
                                 (= method-or-field 'clojure.core/longCast))
                            'longCast
                            method-or-field)
        args-forms      (if (symbol? ?method-or-field) ?args (rest ?method-or-field))]
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
                                    (uc/get (name method-or-field))))]
              (analyze-seq|dot|field-access env form target method-or-field field)
              (analyze-seq|dot|method-call env form target target-class
                (boolean ?target-static-class-map) method-or-field args-forms)))))))

;; TODO this is not the right approach for CLJS
(defns- analyze-seq|new
  [env ::env, [_ _ & [c|form _ & args|form _ :as body] _ :as form] _ > uast/new-node?]
  (let [c|analyzed (analyze* env c|form)]
    (if-not (and (-> c|analyzed :type t/value-type?)
                 (-> c|analyzed :type utr/value-type>value class?))
      (err! "Supplied non-class to `new` form" {:form form})
      (let [c (-> c|analyzed :type utr/value-type>value)
            constructors (-> c class>constructors|with-cache)
            args-ct (count args|form)
            constructors-for-ct (->> constructors
                                     (uc/filter (fn [{:keys [^"[Ljava.lang.Object;" arg-classes]}]
                                                  (= (alength arg-classes) args-ct))))]
        (if (empty? constructors-for-ct)
            (err! "No constructors for class match the arg ct" {:class c :args|form args|form})
            (let [{:keys [args|analyzed call-sites]}
                    (analyze-seq|method-or-constructor-call|incrementally-analyze env form c
                      args|form constructors-for-ct "constructors")]
              (uast/new-node
                {:env             env
                 :unanalyzed-form form
                 :form            (list* 'new c|form (map :form args|analyzed))
                 :class           c
                 :args            args|analyzed
                 :type            (t/isa? c)})))))))

;; TODO move this
(defns truthy-node? [{:as ast t [:type _]} _ > (t/? t/boolean?)]
  (ifs (or (t/= t t/nil?) (t/= t t/false?)) false
       (or (t/> t t/nil?) (t/> t t/false?)) nil ; representing "unknown"
       true))

;; TODO this should be adding analysis information on every predicate it finds to be true or not true
(defns- analyze-seq|if
  "Performs conditional branch pruning."
  [env ::env, [_ _ & [pred-form _, true-form _, false-form _ :as body] _ :as form] _
   > uast/node?]
  (if-not (<= 2 (count body) 3)
    (err! "`if` accepts exactly 3 arguments: one predicate test and two branches; received"
          {:body body})
    (let [pred-node  (analyze* env pred-form)
          true-node  (delay (analyze* env true-form))
          false-node (delay (analyze* env false-form))
          whole-node
            (delay
              (uast/if-node
                {:env             env
                 :unanalyzed-form form
                 :form           (list 'if (:form pred-node) (:form @true-node) (:form @false-node))
                 :pred-node       pred-node
                 :true-node       @true-node
                 :false-node      @false-node
                 :type            (apply t/or (->> [(:type @true-node) (:type @false-node)]
                                                   (remove nil?)))}))]
      (case (truthy-node? pred-node)
        true  (do (log/ppr :warn "Predicate in `if` node is always true" {:pred pred-form})
                  (assoc @true-node :env env))
        false (do (log/ppr :warn "Predicate in `if` node is always false" {:pred pred-form})
                  (assoc @false-node :env env))
        nil   @whole-node))))

(defns- analyze-seq|quote [env ::env, [_ _, arg-form _ :as form] _ > uast/quoted?]
  (if (-> form count (not= 2))
      (err! "Must supply exactly one input to `quote`" {:form form})
      (uast/quoted env form (t/value arg-form))))

(defns- analyze-seq|throw [env ::env, [_ _, arg-form _ :as form] _ > uast/throw-node?]
  (if (-> form count (not= 2))
      (err! "Must supply exactly one input to `throw`" {:form form})
      (let [arg|analyzed (analyze* env arg-form)]
        ;; TODO this is not quite true for CLJS but it's good practice at least
        (if-not (-> arg|analyzed :type (t/<= t/throwable?))
          (err! "`throw` requires a throwable; received"
                {:arg-form arg-form :type (:type arg|analyzed)})
          (uast/throw-node
            {:env             env
             :unanalyzed-form form
             :form            (list 'throw (:form arg|analyzed))
             :arg             arg|analyzed
                              ;; `t/none?` because nothing is actually returned
             :type            t/none?})))))

(defns- analyze-seq|var
  [env ::env, [_ _, arg-form _ :as form] _ > uast/var?]
  (ifs (-> form count (not= 2))
         (err! "Must supply exactly one input to `var`" {:form form})
       (not (symbol? arg-form))
         (err! "`var` accepts a symbol argument" {:form form})
       (let [resolved (uvar/resolve *ns* arg-form)]
         (ifs (nil? resolved)
                (err! "Could not resolve var from symbol" {:symbol arg-form})
              (not (var? resolved))
                (err! "Expected var, but found" {:form form :resolved resolved})
              (uast/var* env (list 'var (uid/>symbol resolved)) resolved (t/value resolved))))))

(defn- filter-dynamic-dispatchable-overload-types
  "An example of dynamic dispatch:
   - When we call `seq` on an input of type `(t/? (t/isa? java.util.Set))`, direct dispatch will
     fail as it is not `t/<=` to any overload (including `t/iterable?` which is the only one under
     which `(t/isa? java.util.Set)` falls).
     However since all branches of the `t/or` are guaranteed to result in a successful dispatch
     (i.e. `t/nil?` and `t/iterable?`) then dynamic dispatch will go forward without an error."
  [{:as ret :keys [dispatchable-overload-types-seq]} input|analyzed i caller|node body]
  (if (-> input|analyzed :type utr/or-type?)
      (let [or-types (-> input|analyzed :type utr/or-type>args)
            {:keys [dispatchable-overload-types-seq' non-dispatchable-or-types]}
              (->> dispatchable-overload-types-seq
                   (reduce
                     (fn [ret {:as overload :keys [arg-types]}]
                       (if-let [or-types-that-match
                                  (->> or-types (uc/lfilter #(t/<= % (get arg-types i))) seq)]
                         (-> ret
                             (update :dispatchable-overload-types-seq' conj overload)
                             (update :non-dispatchable-or-types
                               #(apply disj % or-types-that-match)))
                         ret))
                     {:dispatchable-overload-types-seq' []
                      :non-dispatchable-or-types (set or-types)}))]
        (if (or (empty? dispatchable-overload-types-seq')
                (uc/contains? non-dispatchable-or-types))
            (err! "No overloads satisfy the inputs, whether direct or dynamic"
                  {:caller             caller|node
                   :inputs             body
                   :failing-input-form (:form input|analyzed)
                   :failing-input-type (:type input|analyzed)})
            (assoc ret :dispatchable-overload-types-seq dispatchable-overload-types-seq'
                       :dispatch-type                   :dynamic)))
      (err! "Cannot currently do a dynamic dispatch on a non-`t/or` input type"
            {:input|analyzed input|analyzed})))

(defn- filter-direct-dispatchable-overload-types
  [{:as ret :keys [dispatchable-overload-types-seq]} input|analyzed i caller|node args-form]
  (if-let [dispatchable-overload-types-seq'
            (->> dispatchable-overload-types-seq
                 (uc/lfilter
                   (fn [{:keys [arg-types]}]
                     (t/<= (:type input|analyzed) (get arg-types i))))
                 seq)]
    (assoc ret :dispatchable-overload-types-seq dispatchable-overload-types-seq')
    (if (-> caller|node :unanalyzed-form meta :dyn)
        (filter-dynamic-dispatchable-overload-types ret input|analyzed i caller|node args-form)
        (err! (str "No overloads satisfy the inputs via direct dispatch; "
                   "dynamic dispatch not requested")
              {:caller             (select-keys caller|node [:unanalyzed-form :form :type])
               :inputs             args-form
               :failing-input-form (:form input|analyzed)
               :failing-input-type (:type input|analyzed)}))))

(defn- >dispatch|output-type [dispatch-type dispatchable-overload-types-seq]
  (case dispatch-type
    :direct  (->  dispatchable-overload-types-seq first :output-type)
    :dynamic (->> dispatchable-overload-types-seq
                  (uc/lmap :output-type)
                  ;; Technically we could do a complex conditional instead of a simple `t/or` but
                  ;; no need
                  (apply t/or))))

(defns- caller>overload-type-data-for-arity
  [env ::env, caller|node uast/node?, caller|type _, inputs-ct _]
  (if-let [fn|name (utr/fn-type>name caller|type)]
    (let [overload-types-name (symbol (namespace fn|name) (str (name fn|name) "|__types"))]
      (if-let [fn|types (get env overload-types-name)]
        (->> fn|types (uc/filter #(-> % :arg-types count (= inputs-ct))))
        (if-let [fn|types-var (resolve overload-types-name)]
          (->> fn|types-var var-get urx/norx-deref :overload-types
               (uc/filter #(-> % :arg-types count (= inputs-ct))))
          (err! "Overload-types not found for typed fn"
                {:fn|name fn|name
                 :caller  (assoc (select-keys caller|node [:unanalyzed-form :form])
                                 :type caller|type)}))))
    (err! "No name found for typed fn corresponding to caller"
          (assoc (select-keys caller|node [:unanalyzed-form :form]) :type caller|type))))

(def direct-dispatch-method-sym 'invoke)

(defns- overload-type-datum>reify-name [type-datum _, fn|name symbol? > qualified-symbol?]
  (symbol (-> type-datum :ns-name name) (str (name fn|name) "|__" (:id type-datum))))

(defns- >direct-dispatch|reify-call
  [caller|node uast/node?, caller|type _, type-datum _, args-codelist (us/seq-of t/any?)]
  (if-let [fn|name (utr/fn-type>name caller|type)]
    `(. ~(overload-type-datum>reify-name type-datum fn|name)
        ~direct-dispatch-method-sym ~@args-codelist)
    (err! "No name found for typed fn corresponding to caller; cannot create direct dispatch call"
          (assoc (select-keys caller|node [:unanalyzed-form :form]) :type caller|type))))

(defns- >direct-dispatch
  [env ::env, {:as overload-type-datum :keys [arglist-code|hinted _, body-codelist _, inline? _]} _
   caller|node uast/node?, caller|type _, input-nodes (us/vec-of uast/node?)]
  (if inline?
            ;; TODO abstract this with the `let*` code
      (let [bindings-map (reducei (fn [bindings sym i|arg]
                                    (assoc bindings sym (get input-nodes i|arg)))
                                  {} arglist-code|hinted)
            body-node (analyze* (merge env bindings-map) (list* 'do body-codelist))
            bindings|form
              (reducei (fn [bindings to i|arg]
                         (let [from-node (get input-nodes i|arg)
                               ;; To avoid "Can't hint a primitive local" errors
                               to' (cond-> to (-> from-node :type t/primitive-type?)
                                     ufth/un-type-hint)]
                           (conj bindings to' (:form from-node))))
                       [] arglist-code|hinted)
            node (uast/let* {:env             env
                             :unanalyzed-form (list* 'let* bindings|form body-codelist)
                             :form            (list* 'let* bindings|form
                                                (->> body-node :body (uc/lmap :form)))
                             :bindings        bindings-map
                             :body            body-node
                             :type            (:type body-node)})]
        ;; TODO fix this; apparently it's not enough or maybe `assume` isn't being propagated
        (cond-> node (-> overload-type-datum :output-type meta :quantum.core.type/assume?)
          (update :type #(t/and % (:output-type overload-type-datum)))))
      {:input-nodes input-nodes
       :form        (>direct-dispatch|reify-call
                      caller|node caller|type overload-type-datum (uc/map :form input-nodes))
       :type        (:output-type overload-type-datum)}))

(defns- >call-data-with-fnt-dispatch|empty-args
  [env ::env, caller|node uast/node?, caller|type _, caller-kind _]
  (if (= :fnt caller-kind)
      (if-not-let [overload-type-datum
                    (first (caller>overload-type-data-for-arity env caller|node caller|type 0))]
        (err! (str "No overloads satisfy the inputs via direct dispatch; "
                   "dynamic dispatch not requested")
              {:caller (assoc (select-keys caller|node [:unanalyzed-form :form]) :type caller|type)
               :inputs []})
        (>direct-dispatch env overload-type-datum caller|node caller|type []))
      ;; We could do a little smarter analysis here but we'll keep it simple for now
      {:form (list (:form caller|node)) :input-nodes [] :type t/any?}))

(defns- >call-data-with-fnt-dispatch
  [env ::env, caller|node uast/node?, caller|type _, caller-kind _, inputs-ct _, args-form _]
  (if (zero? inputs-ct)
      (>call-data-with-fnt-dispatch|empty-args env caller|node caller|type caller-kind)
      (->> args-form
           (uc/map+ #(analyze* env %))
           (reducei
             (fn [{:as ret :keys [dispatch-type]} input|analyzed i]
               (if (= :fnt caller-kind)
                   (let [{:as ret' :keys [dispatchable-overload-types-seq input-nodes]}
                           (-> (case dispatch-type
                                 :direct  (filter-direct-dispatchable-overload-types
                                            ret input|analyzed i caller|node args-form)
                                 :dynamic (filter-dynamic-dispatchable-overload-types
                                            ret input|analyzed i caller|node args-form))
                               (update :input-nodes conj input|analyzed))]
                     (if-let [last-input? (= i (dec inputs-ct))]
                       (if (= dispatch-type :direct)
                           (>direct-dispatch env (first dispatchable-overload-types-seq)
                             caller|node caller|type input-nodes)
                           (-> ret'
                               (assoc :form (list* (:form caller|node) (uc/lmap :form input-nodes))
                                      :type (>dispatch|output-type dispatch-type
                                                     dispatchable-overload-types-seq))
                               (dissoc :caller|node :dispatch-type
                                       :dispatchable-overload-types-seq)))
                       ret'))
                   (update ret :input-nodes conj input|analyzed)))
               {:input-nodes   []
                ;; We could do a little smarter analysis here but we'll keep it simple for now
                :type          (when-not (= :fnt caller-kind) t/any?)
                :caller|node   caller|node
                :dispatch-type :direct
                :dispatchable-overload-types-seq
                  (when (= :fnt caller-kind)
                    (caller>overload-type-data-for-arity env caller|node caller|type inputs-ct))}))))

(defns- analyze-seq|dependent-type-call
  [env ::env, [caller|form _, & args-form _ :as form] _ > uast/node?]
  (if (and (-> caller|form name (= "type"))
           (-> args-form count (not= 1)))
      (err! "Incorrect number of args passed to dependent type call"
            {:form form :args-ct (count args-form)})
      (let [arg-nodes          (->> args-form (mapv #(analyze* env %)))
            caller|node        (analyze* env caller|form)
            caller|t           (-> arg-nodes first :type)
            unvalued-arg-types (->> arg-nodes rest (map :type) (map t/unvalue))
            _                  (uref/set! !!dependent? true)
            t (case (name caller|form)
                "input-type"  (if (-> env :opts :split-types?)
                                  (t/input-type|meta-or  caller|t unvalued-arg-types)
                                  (t/input-type|or       caller|t unvalued-arg-types))
                "output-type" (if (-> env :opts :split-types?)
                                  (t/output-type|meta-or caller|t unvalued-arg-types)
                                  (t/output-type|or      caller|t unvalued-arg-types))
                "type"        caller|t)]
        (uast/call-node
          {:env             env
           :unanalyzed-form form
           :form            (if (utr/rx-type? t) form (uform/>form t))
           :caller          caller|node
           :args            arg-nodes
           :type            (t/value t)}))))

(defns- apply-arg-type-combine [combinef fn?, input-nodes _ > t/value-type?]
  (->> input-nodes
       (uc/map+ :type)
       (uc/map+ t/unvalue)
       ur/join
       (apply combinef)
       t/value))

;; TODO this is probably not a great way to do this; rethink this
;; Maybe it would work more cleanly if we added the `::t/type` metadata to each `t/` operator after
;; the fact?
(defns- handle-type-combinators
  [caller|node uast/node?, input-nodes _, output-type t/type? > t/type?]
  (condp = (:type caller|node)
    ;; TODO this relies on spec instrumentation not happening for these fns
    (t/value t/isa?)     (apply-arg-type-combine t/isa?     input-nodes)
    (t/value t/value)    (apply-arg-type-combine t/value    input-nodes)
    (t/value t/or)       (apply-arg-type-combine t/or       input-nodes)
    (t/value t/and)      (apply-arg-type-combine t/and      input-nodes)
    (t/value t/-)        (apply-arg-type-combine t/-        input-nodes)
    (t/value t/?)        (apply-arg-type-combine t/?        input-nodes)
    (t/value t/*)        (apply-arg-type-combine t/*        input-nodes)
    (t/value t/ref)      (apply-arg-type-combine t/ref      input-nodes)
    (t/value t/unref)    (apply-arg-type-combine t/unref    input-nodes)
    (t/value t/assume)   (apply-arg-type-combine t/assume   input-nodes)
    (t/value t/unassume) (apply-arg-type-combine t/unassume input-nodes)
    output-type))

(defns- analyze-seq|call
  [env ::env, [caller|form _ & args-form _ :as form] _ > uast/call-node?]
  (let [caller|node (analyze* env caller|form)
        caller|type (:type caller|node)
        ;; We just `norx-deref` the `caller|type` primarily for `t/defn`s but it could be unsafe
        ;; TODO assess what will happen if we reactively derefed. It's currently being derefed in an
        ;;      interceptor on `!overload-types`. If it were reactively derefed then I believe it
        ;;      would make it so every time any `t/defn` function was even mentioned in a body, if
        ;;      any of those `t/defn`s changed whatsoever, the body would be re-analyzed and
        ;;      overloads would be re-created (though currently it only checks whether the input
        ;;      or output types have changed... not things in the body).
        caller|type (cond-> caller|type (utr/rx-type? caller|type) urx/norx-deref)
        inputs-ct   (count args-form)]
          ;; TODO fix this line of code and extend t/compare so the comparison checks below will
          ;;      work with t/fn
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
                         ;; TODO use the `reflect/reflect` and `js/Object.getOwnPropertyNames` trick
                       :fn nil)
                   {:as call-data :keys [input-nodes] analyzed-form :form}
                     (>call-data-with-fnt-dispatch
                       env caller|node caller|type caller-kind inputs-ct args-form)]
               (if (uast/node? call-data) ; in the case of an inline expansion
                   call-data
                   (uast/call-node
                     {:env             env
                      :unanalyzed-form form
                      :form            analyzed-form
                      :caller          caller|node
                      :args            input-nodes
                      :type            (if (-> env :opts :arglist-context?)
                                           (handle-type-combinators
                                             caller|node input-nodes (:type call-data))
                                           (:type call-data))}))))))

(defns- analyze-seq*
  "Analyze a seq after it has been macro-expanded.
   The ->`form` is post- incremental macroexpansion."
  [env ::env, [caller|form _ & body _ :as form] _ > uast/node?]
  (case caller|form
    .        (analyze-seq|dot   env form)
    def      (TODO "def"      {:form form})
    deftype* (TODO "deftype*" {:form form})
    do       (analyze-seq|do    env form)
    fn*      (TODO "fn*"      {:form form})
    if       (analyze-seq|if    env form)
    let*     (analyze-seq|let*  env form)
    new      (analyze-seq|new   env form)
    quote    (analyze-seq|quote env form)
    reify*   (TODO "reify"    {:form form}) ; NOTE only for CLJ
    set!     (TODO "set!"     {:form form})
    throw    (analyze-seq|throw env form)
    try      (TODO "try"      {:form form})
    var      (analyze-seq|var   env form)
    (if (-> env :opts :arglist-context?)
        (if-let [caller-form-dependent-type-call?
                   (and (symbol? caller|form)
                        (when-let [sym (some-> (uvar/resolve *ns* caller|form) uid/>symbol)]
                          (case sym
                            (quantum.core.type/type
                             quantum.untyped.core.type/type
                             quantum.core.type/input-type
                             quantum.untyped.core.type/input-type
                             quantum.core.type/output-type
                             quantum.untyped.core.type/output-type) true
                            false)))]
          (analyze-seq|dependent-type-call env form)
          (analyze-seq|call env form))
        (analyze-seq|call env form))))

(defns- analyze-seq [env ::env, form _]
  (let [expanded-form (ufeval/macroexpand form)]
    (if-let [no-expansion? (ucomp/== form expanded-form)]
      (analyze-seq* env expanded-form)
      (let [expanded-form' (cond-> expanded-form
                             (uvar/with-metable? expanded-form) (update-meta merge (meta form)))
            expanded (analyze* env expanded-form')]
        (uast/macro-call
          {:env             env
           :unexpanded-form form
           :unanalyzed-form expanded-form'
           :form            (:form expanded)
           :expanded        expanded
           :type            (:type expanded)})))))

(defns- ?resolve [env ::env, sym symbol?]
  (if-let [[_ local] (or (find env sym)
                         (and (-> env :opts :arglist-context?)
                              (-> env :opts :arg-env deref (find sym))))]
    {:resolved local :resolved-via :env}
    (let [resolved (uvar/resolve *ns* sym)]
      (ifs resolved
             {:resolved resolved :resolved-via :resolve}
           (some->> sym namespace symbol (uvar/resolve *ns*) class?)
             {:resolved     (analyze-seq|dot
                              env (list '. (-> sym namespace symbol) (-> sym name symbol)))
              :resolved-via :dot}
           nil))))

(defns- analyze-symbol|arglist-context
  "Handles forward dependent-type dependencies e.g. `[a (type b) b t/any?]`"
  [env ::env form symbol?]
  (l/if-let [_             (-> env :opts :arglist-context?)
             arg-type-form (-> env :opts :arg-sym->arg-type-form (get form))]
    (let [env' {:opts (update (:opts env) :arglist-syms|queue
                        (fn [q]
                          (if (contains? q form)
                              (err! "Cyclic dependency between two input types"
                                    {:dependent-sym       (uc/last q)
                                     :dependent-type-form
                                       (-> env :opts :arg-sym->arg-type-form (get (uc/last q)))
                                     :dependee-sym        form
                                     :dependee-type-form  arg-type-form})
                              (conj q form))))}
          result (analyze-arg-syms* env')]
      ;; We need to propagate the result upward and this is arguably the cleanest control flow
      ;; mechanism to do it, sadly
      (err! ::arg-syms-analyzed "All arg syms analyzed" {:result result}))
    (err! "Could not resolve symbol" {:sym form})))

(defns- analyze-symbol
  "Analyzes vars as if their value is constant, unless they're marked as dynamic."
  [env ::env, form symbol? > uast/symbol?]
  (if-not-let [{:keys [resolved resolved-via]} (?resolve env form)]
    (analyze-symbol|arglist-context env form)
    (let [node (case resolved-via
                 (:env :dot) resolved
                 :resolve
                   (if (var? resolved)
                       (let [form (list 'var (uid/>symbol resolved))]
                         (if (uvar/dynamic? resolved)
                             (uast/var* env form resolved (t/value resolved))
                             (let [v (var-get resolved)]
                               (uast/var-value env form v
                                 (or (-> resolved meta :quantum.core.type/type) (t/value v))))))
                       (uast/class-value env (uid/>symbol resolved) resolved)))]
      (ifs (uast/symbol? node)
             (assoc node :env env)
           (uast/class-value? node)
             ;; To avoid unnecessary type hint
             (quantum.untyped.core.analyze.ast.Symbol. env form node (:type node))
           (uast/symbol env form node (:type node))))))

(defns- analyze* [env ::env, form _ > uast/node?]
  (when (> (uref/get (uref/update! !!analyze-depth inc)) 200)
    (throw (ex-info "Stack too deep" {:form form})))
  (ifs (symbol?    form) (analyze-symbol env form)
       (t/literal? form) (uast/literal   env form (t/value form))
       (vector?    form) (analyze-vector env form)
       (set?       form) (analyze-set    env form)
       (map?       form) (analyze-map    env form)
       (seq?       form) (analyze-seq    env form)
       (throw (ex-info "Unrecognized form" {:form form}))))

(defns analyze
  "`env` consists of a map from simple symbol to `uast/node?`, with one exception: `env` admits one
   optional key that is not a symbol: `:opts`. The reason `:opts` exists on the `env` map is that
   analyzer functions may need to return updated opts or metadata and it is cleaner to put it on the
   env map rather than on the AST nodes themselves.

   The `:opts` map may include:
   - :arglist-context?        : p/boolean?
                              : If you use `analyze-arg-syms` you won't have to set this yourself.
                              : When this is enabled, each AST node is tagged with additional
                              : information about dependent type analysis, namely:
                              : - :arglist-syms|queue      : (dc/set-of id/simple-symbol?)
                              : - :arglist-syms|unanalyzed : (dc/set-of id/simple-symbol?)
   - :arg-sym->arg-type-form  : (dc/map-of id/simple-symbol? t/any?)
                              : If you use `analyze-arg-syms` you won't have to set this yourself.
   - :arglist-syms|queue      : (dc/set-of id/simple-symbol?)
                              : If you use `analyze-arg-syms` you won't have to set this yourself.
   - :arglist-syms|unanalyzed : (dc/set-of id/simple-symbol?)
                              : If you use `analyze-arg-syms` you won't have to set this yourself.

    Special metadata directives are defined in `special-metadata-keys`. They include:
   - `:val` : Causes the analyzer to assume that the return value of the dot-form satisfies
              `t/val?`. Useful for doing method/dot-chaining in which the methods return
              non-primitives."
  > uast/node?
  ([form _] (analyze {} form))
  ([env ::env, form _]
    (uref/set! !!analyze-depth 0)
    (analyze* env form)))

;; ===== Arglist analysis ===== ;;

(us/def ::arg-sym->arg-type-form (us/map-of simple-symbol? t/any?))

(def analyze-arg-syms|max-iter 10000)

;; TODO excise
(defn pr! [x]
  (binding [quantum.untyped.core.analyze.ast/*print-env?* false
            quantum.untyped.core.print/*collapse-symbols?* true
            *print-meta*  true
            *print-level* 10]
    (quantum.untyped.core.print/ppr x)))

#?(:clj
(uvar/def sort-guide "for use in arglist sorting, in increasing conceptual (and bit) size"
  {t/nil?     0
   t/boolean? 1
   t/byte?    2
   t/short?   3
   t/char?    4
   t/int?     5
   t/long?    6
   t/float?   7
   t/double?  8
   t/object?  9}))

;; TODO move?
(defns type>split
  "Only `t/or`s and `t/meta-or`s are splittable for now.
   Reactive types are non-reactively derefed in order to make splitting possible."
  [t t/type? > (us/vec-of t/type?)]
  (let [t' (cond-> t (utr/rx-type? t) urx/norx-deref)]
    (ifs (utr/or-type?      t') (utr/or-type>args       t')
         ;; TODO determine if this is the appropriate place to deal with `t/none?`
         (utr/meta-or-type? t') (->> t' utr/meta-or-type>types (uc/remove (fn1 t/= t/none?)))
         [t'])))

(defns type>split+primitivized [t t/type? > (us/vec-of t/type?)]
  (let [t|norx  (cond-> t (utr/rx-type? t) urx/norx-deref)
        t|split (type>split t|norx)
        primitive-subtypes
          (->> t|split
               (uc/map+  #(t/type>primitive-subtypes % false))
               (ur/educe uset/union)
               (sort-by  sort-guide))] ; For cleanliness and reproducibility in tests
    (uc/distinct (concat primitive-subtypes t|split))))

(defn- enqueue-first-unanalyzed-if-queue-empty [env #_::env #_> #_::env]
  (cond-> env
    (-> env :opts :arglist-syms|queue empty?)
    (update-in [:opts :arglist-syms|queue] conj
      (-> env :opts :arglist-syms|unanalyzed first))))

(defn- analyze-arg-syms* [env #_::env]
  (uref/update! !!analyze-arg-syms|iter inc)
  (let [{:keys [arg-sym->arg-type-form arglist-syms|queue arglist-syms|unanalyzed
                output-type-or-form split-types?]} (:opts env)]
    (ifs (empty? arglist-syms|unanalyzed)
           [{:env              env
             :output-type-node (if (t/type? output-type-or-form)
                                   (uast/literal env nil output-type-or-form) ; a simulated AST node
                                   (-> (analyze env output-type-or-form)
                                       (update :type (fn-> t/unvalue urx/?norx-deref))))
             :dependent?       (uref/get !!dependent?)}]
         (>= (uref/get !!analyze-arg-syms|iter) analyze-arg-syms|max-iter)
           (err! "Max number of iterations reached for `analyze-arg-syms`"
                 {:n (uref/get !!analyze-arg-syms|iter)})
         (let [_             (assert (not (empty? arglist-syms|queue)))
               arg-sym       (uc/last arglist-syms|queue)
               arg-type-form (arg-sym->arg-type-form arg-sym)
               analyzed      (-> (analyze env arg-type-form) (update :type t/unvalue))
               env-analyzed  (-> analyzed :env
                                 (update-in [:opts :arglist-syms|queue]      disj arg-sym)
                                 (update-in [:opts :arglist-syms|unanalyzed] disj arg-sym))
               t-split       (if split-types?
                                 (-> analyzed :type type>split+primitivized)
                                 [(:type analyzed)])]
           (if (-> t-split count (= 1))
               (recur (-> env-analyzed
                          (update-in [:opts :arg-env]
                            #(doto % (swap! assoc arg-sym (assoc analyzed :type (first t-split)))))
                          enqueue-first-unanalyzed-if-queue-empty))
               (->> t-split
                    (uc/mapcat+
                      (fn [t]
                        (analyze-arg-syms*
                          (-> env-analyzed
                              enqueue-first-unanalyzed-if-queue-empty
                              (update-in [:opts :arg-env]
                                ;; `(atom (deref %))` in order to create a new env for a new split
                                #(-> % deref atom
                                     (doto (swap! assoc arg-sym (assoc analyzed :type t)))))))))
                    ur/join))))))

(defns- >analyze-arg-syms|opts
  [env ::env, arg-sym->arg-type-form ::arg-sym->arg-type-form, output-type-or-form _
   split-types? boolean?]
  {:arglist-context?        true
   :arglist-syms|queue      (uset/ordered-set (-> arg-sym->arg-type-form keys first))
   :arglist-syms|unanalyzed (-> arg-sym->arg-type-form keys set)
   :arg-env                 (atom env) ; Mutable so it can cache
   :arg-sym->arg-type-form  arg-sym->arg-type-form
   :output-type-or-form     output-type-or-form
   :split-types?            split-types?})

(defns analyze-arg-syms
  "`dependent?` denotes whether any of of the arg-types or output-type use dependent types.

   Performance characteristics:
   - While an internally recursive function, the maximum stack depth is the number of arguments in
     the provided arglist.
   - The maximum number of generated arglists is equal to the product of the cardinalities of the
     deduced types of the inputs. In other words, in the worst case scenario each of the arg types
     might be a 'splittable' type like `t/or` (whose cardinality is the number of arguments to it
     when simplified) which would require a Cartesian product of the splits of the arg types."
  > vector? #_(us/vec-of (us/kv {:env ::env :output-type-node uast/node? :dependent? boolean?}))
  ([arg-sym->arg-type-form ::arg-sym->arg-type-form, output-type-or-form _]
    (analyze-arg-syms {} arg-sym->arg-type-form output-type-or-form true))
  ([arg-sym->arg-type-form ::arg-sym->arg-type-form, output-type-or-form _, split-types? boolean?]
    (analyze-arg-syms {} arg-sym->arg-type-form output-type-or-form split-types?))
  ([env ::env, arg-sym->arg-type-form ::arg-sym->arg-type-form, output-type-or-form _
    split-types? boolean?
    > (us/vec-of (us/kv {:env ::env :output-type-node uast/node?}))]
    (uref/set! !!analyze-arg-syms|iter 0)
    (uref/set! !!dependent?            false)
    (try (analyze-arg-syms*
           {:opts (merge (:opts env)
                    (>analyze-arg-syms|opts env arg-sym->arg-type-form output-type-or-form
                      split-types?))})
      (catch Throwable t
        (if (and (uerr/error-map? t) (-> t :ident (= ::arg-syms-analyzed)))
            (-> t :data :result)
            (throw t))))))
