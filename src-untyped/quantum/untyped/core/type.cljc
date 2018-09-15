(ns quantum.untyped.core.type
  "Essentially, set-theoretic definitions and operations on types."
  {:todo "Maybe reduce dependencies and distribute predicates to other namespaces"}
         (:refer-clojure :exclude
           [< <= = not= >= > == compare * -
            and or not
            boolean  byte         char  short  int  long  float  double
            boolean? byte? bytes? char? short? int? long? float? double?
            isa?
            nil? any? class? tagged-literal? #?(:cljs object?)
            number? decimal? bigdec? integer? ratio?
            true? false? keyword? string? symbol?
            array? associative? coll? counted? indexed? iterable? list? map? map-entry? record?
            seq? seqable? sequential? set? sorted? vector?
            fn? ifn?
            var?
            meta
            delay? ref volatile?
            fn])
         (:require
           [clojure.core                               :as c]
           [clojure.string                             :as str]
           [quantum.untyped.core.analyze.expr
             :refer [>expr #?(:cljs Expression)]]
           [quantum.untyped.core.collections           :as uc]
           [quantum.untyped.core.collections.logic
             :refer [seq-and seq-or]]
           [quantum.untyped.core.compare               :as ucomp
             :refer [== <ident =ident >ident ><ident <>ident]]
           [quantum.untyped.core.core                  :as ucore]
           [quantum.untyped.core.data.bits             :as ubit]
           [quantum.untyped.core.data.hash             :as uhash]
           [quantum.untyped.core.data.set              :as uset]
           [quantum.untyped.core.data.tuple]
           [quantum.untyped.core.defnt
             :refer [defns defns-]]
           [quantum.untyped.core.error                 :as uerr
             :refer [err! TODO catch-all]]
           [quantum.untyped.core.fn                    :as ufn
             :refer [fn1 rcomp <- fn->]]
           [quantum.untyped.core.form.generate.deftype :as udt]
           [quantum.untyped.core.identification
             :refer [>symbol]]
           [quantum.untyped.core.logic
             :refer [fn-and ifs whenp->]]
           [quantum.untyped.core.numeric               :as unum]
           [quantum.untyped.core.print                 :as upr]
           [quantum.untyped.core.reducers              :as ur
             :refer [educe join]]
           [quantum.untyped.core.refs
             :refer [?deref]]
           [quantum.untyped.core.spec                  :as us]
           [quantum.untyped.core.type.compare          :as utcomp]
           [quantum.untyped.core.type.core             :as utcore]
           [quantum.untyped.core.type.defs             :as utdef]
           [quantum.untyped.core.type.reifications     :as utr
             :refer [->AndType ->OrType PType
                     #?@(:cljs [UniversalSetType EmptySetType
                                NotType OrType AndType
                                ProtocolType ClassType
                                ValueType])]]
           [quantum.untyped.core.vars                  :as uvar
             :refer [def- defmacro- update-meta]])
#?(:cljs (:require-macros
           [quantum.untyped.core.type :as self
             :refer [-def def-preds|map|any def-preds|map|same-types]]))
#?(:clj  (:import
           [quantum.untyped.core.analyze.expr Expression]
           [quantum.untyped.core.type.reifications
              UniversalSetType EmptySetType
              NotType OrType AndType
              ProtocolType ClassType
              ValueType
              FnType])))

(ucore/log-this-ns)

;; ===== TODOS ===== ;;

#_(defmacro ->
  ("Anything that is coercible to x"
    [x]
    ...)
  ("Anything satisfying `from` that is coercible to `to`.
    Will be coerced to `to`."
    [from to]))

#_(defmacro range-of)

(declare
  - create-logical-type nil? val?
  and or val|by-class?)

(defonce *type-registry (atom {}))

;; ===== Comparison ===== ;;

(uvar/defaliases utcomp compare < <= = not= >= > >< <>)

;; ===== Type Reification Constructors ===== ;;

;; ----- UniversalSetType (`t/U`) ----- ;;

(uvar/defalias utr/universal-set)

;; ----- EmptySetType (`t/∅`) ----- ;;

(uvar/defalias utr/empty-set)

;; ----- NotType (`t/not` / `t/!`) ----- ;;

(defns not [t utr/type? > utr/type?]
  (ifs (= t universal-set) empty-set
       (= t empty-set)     universal-set
       (= t val|by-class?) nil?
       (utr/not-type? t)   (utr/not-type>inner-type t)
       ;; DeMorgan's Law
       (utr/or-type?  t)   (->> t utr/or-type>args  (uc/lmap not) (apply and))
       ;; DeMorgan's Law
       (utr/and-type? t)   (->> t utr/and-type>args (uc/lmap not) (apply or ))
       (NotType. uhash/default uhash/default nil t)))

(uvar/defalias ! not)

;; ----- OrType (`t/or` / `t/|`) ----- ;;

(defn or
  "Sequential/ordered `or`. Analogous to `set/union`.
   Applies as much 'compression'/deduplication/simplification as possible to the supplied types.
   Effectively computes the union of the extension of the ->`args`."
  [arg & args]
  (create-logical-type :or ->OrType utr/or-type? utr/or-type>args
    (cons arg args) (fn1 c/= >ident)))

(uvar/defalias | or)

;; ----- AndType (`t/and` | `t/&`) ----- ;;

(defn and
  "Sequential/ordered `and`. Analogous to `set/intersection`.
   Applies as much 'compression'/deduplication/simplification as possible to the supplied types.
   Effectively computes the intersection of the extension of the ->`args`."
  [arg & args]
  (create-logical-type :and ->AndType utr/and-type? utr/and-type>args
    (cons arg args) (fn1 c/= <ident)))

(uvar/defalias & and)

;; ----- Expression ----- ;;

;; ----- ProtocolType ----- ;;

(defns- isa?|protocol [p ucore/protocol?]
  (ProtocolType. uhash/default uhash/default nil p nil))

;; ----- ClassType ----- ;;

(defns- isa?|class [c #?(:clj c/class? :cljs c/fn?)]
  (ClassType. uhash/default uhash/default nil c nil))

;; ----- ValueType ----- ;;

(defns value
  "Creates a type whose extension is the singleton set containing only the value `v`."
  [v _] (ValueType. uhash/default uhash/default nil v))

;; ----- General ----- ;;

(defns -
  "Computes the difference of `t0` from `t1`: (& t0 (! t1))
   If `t0` =       `t1`, `∅`
   If `t0` <       `t1`, `∅`
   If `t0` <>      `t1`, `t0`
   If `t0` > | ><  `t1`, `t0` with all elements of `t1` removed"
  ([t0 utr/type? > utr/type?] t0)
  ([t0 utr/type?, t1 utr/type? > utr/type?]
    (let [c (compare t0 t1)]
      (case c
        (0 -1) empty-set
         3     t0
        (1 2)
          (let [c0 (c/type t0) c1 (c/type t1)]
            ;; TODO add dispatch?
            (condp == c0
              NotType (condp == (-> t0 utr/not-type>inner-type c/type)
                        ClassType (condp == c1
                                    ClassType (AndType. uhash/default uhash/default nil
                                                [t0 (not t1)] (atom nil)))
                        ValueType (condp == c1
                                    ValueType (AndType. uhash/default uhash/default nil
                                                [t0 (not t1)] (atom nil))))
              OrType  (condp == c1
                        ClassType (let [args (->> t0 utr/or-type>args (uc/remove (fn1 = t1)))]
                                    (case (count args)
                                      0 empty-set
                                      1 (first args)
                                      (OrType. uhash/default uhash/default nil args
                                        (atom nil))))))))))
  ([t0 utr/type?, t1 utr/type? & ts (us/seq-of utr/type?) > utr/type?] (reduce - (- t0 t1) ts)))

(defn isa? [x]
  (ifs (ucore/protocol? x)
       (isa?|protocol x)

       (#?(:clj c/class? :cljs c/fn?) x)
       (isa?|class x)))

;; TODO clean up
(defns >type
  "Coerces ->`x` to a type, recording its ->`name-sym` if provided."
  ([x _ > utr/type?] (>type x nil))
  ([x _, name-sym (us/nilable c/symbol?) > utr/type?]
    #?(:clj
        (ifs
          (satisfies? PType x)
            x ; TODO should add in its name?
          (c/class? x)
            (let [x (c/or #?(:clj (utcore/unboxed->boxed x)) x)
                  reg (if (c/nil? name-sym)
                          @*type-registry
                          (swap! *type-registry
                            (c/fn [reg]
                              (if-let [t (get reg name-sym)]
                                (if (c/= (.-name ^ClassType t) name-sym)
                                    reg
                                    (err! "Class already registered with type; must first undef" {:class x :type-name name-sym}))
                                (let [t (ClassType. uhash/default uhash/default nil x name-sym)]
                                  (uc/assoc-in reg [name-sym]    t
                                                   [:by-class x] t))))))]
              (c/or (get-in reg [:by-class x])
                    (ClassType. uhash/default uhash/default nil ^Class x name-sym)))
          (c/fn? x)
            (let [sym (c/or name-sym (>symbol x))
                  _ (when-not name-sym
                      (let [resolved (?deref (ns-resolve *ns* sym))]
                        (assert (== resolved x) {:x x :sym sym :resolved resolved})))]
              (Expression. sym x))
          (c/nil? x)
            nil?
          (ucore/protocol? x)
            (ProtocolType. uhash/default uhash/default nil x name-sym)
          (value x))
       :cljs nil)))

;; ===== Definition/Registration ===== ;;

(defns register-type! [sym c/symbol?, t utr/type?]
  (TODO))

;; TODO clean up
#?(:clj
(defmacro define [sym t]
  `(~'def ~sym (let [t# ~t]
                 (assert (utr/type? t#) t#)
                 #_(register-type! '~(uident/qualify sym) t#)
                 t#))))

;; TODO clean up
(defn undef [reg sym]
  (if-let [t (get reg sym)]
    (let [reg' (dissoc reg sym)]
      (if (instance? ClassType t)
          (uc/dissoc-in reg' [:by-class (.-c ^ClassType t)])
          (TODO)))
    reg))

;; TODO clean up
(defn undef! [sym] (swap! *type-registry undef sym))

#_(:clj
(defmacro defalias [sym t]
  `(~'def ~sym (>type ~t))))

#?(:clj (uvar/defalias -def define))

(-def type?          (isa? PType))
(-def not-type?      (isa? NotType))
(-def or-type?       (isa? OrType))
(-def and-type?      (isa? AndType))
(-def protocol-type? (isa? ProtocolType))
(-def class-type?    (isa? ClassType))
(-def value-type?    (isa? ValueType))

;; For use in logical operators
(-def nil?           (value nil))
(-def object?        (isa? #?(:clj java.lang.Object :cljs js/Object)))

;; ===== Miscellaneous ===== ;;

(defns *
  "Denote on a type that it must be enforced at runtime.
   For use with `defnt`."
  [t utr/type? > utr/type?] (update-meta t assoc :runtime? true))

(defns ref
  "Denote on a type that it must not be expanded to use primitive values.
   For use with `defnt`."
  [t utr/type? > utr/type?] (update-meta t assoc :ref? true))

;; TODO figure this out
#_(do (udt/deftype DeducibleSpec [*spec #_(t/atom-of t/spec?)]
  {PSpec                 nil
   fipp.ednize/IOverride nil
   fipp.ednize/IEdn      {-edn ([this] (list `deducible @*spec))}
   ?Atom                 {swap! (([this f] (swap!  *spec f)))
                          reset! ([this v] (reset! *spec v))}})

(defns deducible-spec? [x _] (instance? DeducibleSpec x))

(defns deducible [x spec? > deducible-spec?] (DeducibleSpec. (atom x))))

;; ===== Logical ===== ;;

(defns >logical-complement
  "Returns the content inside a `t/not` applied to the `args` of an n-ary logical type (e.g. `or`,
   `and`). Stored in such types to more easily compare them with `not` types.
   E.g. `(>logical-complement (and a b))` -> `(or  (not a) (not b))`
        `(>logical-complement (or  a b))` -> `(and (not a) (not b))`."
  [t utr/type? > utr/type?]
  (cond (utr/or-type?  t) (c/or @(.-*logical-complement ^OrType  t)
                                (reset! (.-*logical-complement ^OrType  t) (not t)))
        (utr/and-type? t) (c/or @(.-*logical-complement ^AndType t)
                                (reset! (.-*logical-complement ^AndType t) (not t)))
        :else             (err! "`>logical-complement` not supported on type" {:type t})))

(defns complementary? [t0 utr/type? t1 utr/type?] (= t0 (not t1)))

(defns- create-logical-type|inner|or
  [{:as accum :keys [t' utr/type?]} _, t* utr/type?, c* ucomp/comparison?]
  (if #?(:clj  (c/or (c/and (c/= t' object?) (c/= t* nil?))
                     (c/and (c/= t* object?) (c/= t' nil?)))
         :cljs false)
      (reduced (assoc accum :conj-t? false :types [universal-set]))
      (if ;; `s` must be either `><` or `<>` w.r.t. to all other args
          (case c* (2 3) true false)
          (if ;; Tautology/universal-set: (| A (! A))
              (c/and (c/= c* <>ident) ; optimization before `complementary?`
                     (complementary? t' t*))
              (reduced (assoc accum :conj-t? false :types [universal-set]))
              (update accum :types conj t*))
          (reduced (assoc accum :prefer-orig-args? true)))))

(defns- create-logical-type|inner|and
  [{:as accum :keys [conj-t? c/boolean?, prefer-orig-args? c/boolean?, t' utr/type?, types _]} _
   t* utr/type?, c* ucomp/comparison?]
  (if       ;; Contradiction/empty-set: (& A (! A))
      (c/or (c/= c* <>ident) ; optimization before `complementary?`
            (complementary? t' t*))
      (do #_(println "BRANCH 1")
          (reduced (assoc accum :conj-t? false :types [empty-set])))
      (do #_(println "BRANCH 2")
          (let [conj-t?' (if ;; `s` must be `><` w.r.t. to all other args if it is to be `conj`ed
                             (c/not= c* ><ident)
                             false
                             conj-t?)
                ;; TODO might similar logic extend to `:or` as well?
                tt* (if (utr/not-type? t')
                        (let [diff (- t* (not t'))]
                          (if (utr/and-type? diff)
                              ;; preserve inner expansion
                              (utr/and-type>args diff)
                              [diff]))
                        [t*])]
            (assoc accum :conj-t? conj-t?' :types (into types tt*))))))

(defns- create-logical-type|inner
  [args' _, t utr/type?, kind #{:or :and}, comparison-denotes-supersession? c/fn?]
  (let [args+comparisons|without-superseded
          (->> args'
               (uc/map+    (juxt identity #(compare t %)))
               ;; remove all args whose extensions are superseded by `t`
               (uc/remove+ (fn-> second comparison-denotes-supersession?))
               join) ; TODO elide `join`
        t-redundant? (->> args+comparisons|without-superseded (seq-or (fn-> second (c/= =ident))))]
    (ifs t-redundant?
           args'
         (empty? args+comparisons|without-superseded)
           [t]
         (let [{:keys [conj-t? prefer-orig-args? t' types]}
               (->> args+comparisons|without-superseded
                    (educe
                      (c/fn ([accum] accum)
                            ([accum [t* c*]]
                              #_(prl! kind conj-s? prefer-orig-args? t' types t* c*)
                              (case kind
                                :or  (create-logical-type|inner|or  accum t* c*)
                                :and (create-logical-type|inner|and accum t* c*))))
                      {:conj-t?            ;; If `t` is a `NotType`, and kind is `:and`, then it will be
                                           ;; applied by being `-` from all args, not by being `conj`ed
                                           (c/not (c/and (c/= kind :and) (utr/not-type? t)))
                       :prefer-orig-args? false
                       :t'                t
                       :types             []}))]
           (if prefer-orig-args?
               args'
               (whenp-> types conj-t? (conj t')))))))

(defn- simplify-logical-type|inner-expansion+
  "Simplification via inner expansion: `(| (| a b) c)` -> `(| a b c)`"
  [type-pred type>args type-args #_(of reducible? utr/type?)]
  (->> type-args
       (uc/map+ (c/fn [arg] (if (type-pred arg)
                                (type>args arg)
                                [arg])))
       uc/cat+))

(defn- simplify-logical-type|structural-identity+
  "Simplification via structural identity: `(| a b a)` -> `(| a b)`"
  [type-args #_(of reducible? utr/type?)]
  (->> type-args (uc/map+ >type) uc/distinct+))

(defn- simplify-logical-type|comparison
  "Simplification via intension comparison"
  [kind comparison-denotes-supersession? type-args #_(of reducible? utr/type?)]
  (educe
    (c/fn ([type-args'] type-args')
          ([type-args' t #_utr/type?]
            (if (empty? type-args')
                (conj type-args' t)
                (create-logical-type|inner type-args' t kind comparison-denotes-supersession?))))
    []
    type-args))

(defns- create-logical-type
  [kind #{:or :and}, construct-fn _, type-pred _, type>args _, type-args (fn-> count (c/>= 1))
   comparison-denotes-supersession? c/fn? > utr/type?]
  (if (-> type-args count (c/= 1))
      (first type-args)
      (let [simplified
              (->> type-args
                   (simplify-logical-type|inner-expansion+ type-pred type>args)
                   simplify-logical-type|structural-identity+
                   (simplify-logical-type|comparison kind comparison-denotes-supersession?))]
        (assert (-> simplified count (c/>= 1))) ; for internal implementation correctness
        (if (-> simplified count (c/= 1))
            (first simplified)
            (construct-fn uhash/default uhash/default nil simplified (atom nil))))))

;; TODO do this?
#_(udt/deftype SequentialType)

#_(defns of
  "Creates a type that ... TODO"
  [pred (<= iterable?), t utr/type?] (TODO))

(defn fn [out-type arity & arities]
  (let [name- nil
        arities-form (cons arity arities)
        arities (->> arities-form
                     (uc/map+ (c/fn [arity-form]
                                (-> (us/conform ::fn-type|arity arity-form)
                                    (update :output-type #(c/or % out-type universal-set)))))
                     (uc/group-by #(-> % :input-types count)))]
    (FnType. nil name- out-type arities-form arities)))

(defns compare|in [x0 utr/fn-type?, x1 utr/fn-type? > ucomp/comparison?]
  (let [ct->overloads|x0 (utr/fn-type>arities x0)
        ct->overloads|x1 (utr/fn-type>arities x1)
        cts-only-in-x0 (uset/- (-> ct->overloads|x0 keys set) (-> ct->overloads|x1 keys set))
        cts-only-in-x1 (uset/- (-> ct->overloads|x1 keys set) (-> ct->overloads|x0 keys set))
        comparison|cts (uset/compare cts-only-in-x0 cts-only-in-x1)
        cts-in-both (->> ct->overloads|x0 (filter (fn-> first ct->overloads|x1)))
        overloads->ored-input-types
          ;; Yes, there must be a more performant way to do this
          (c/fn [overloads] (->> overloads (uc/lmap :input-types) (apply uc/lmap or)))]
    (utcomp/combine-comparisons
      comparison|cts
      (->> cts-in-both
           (map (c/fn [[ct overloads|x0]]
                  (if (zero? ct)
                      0
                      (utcomp/combine-comparisons
                        (uc/lmap utcomp/compare
                          (->> overloads|x0        overloads->ored-input-types)
                          (->> ct ct->overloads|x1 overloads->ored-input-types))))))
           utcomp/combine-comparisons))))

(defns fn-type>output-type [x utr/fn-type? > type?]
  (->> x utr/fn-type>arities
         vals
         (apply concat)
         (uc/lmap :output-type)
         (apply or)))

(defns compare|out [x0 utr/fn-type?, x1 utr/fn-type? > ucomp/comparison?]
  (utcomp/compare (fn-type>output-type x0) (fn-type>output-type x1)))

(defn unkeyed
  "Creates an unkeyed collection type, in which the collection may
   or may not be sequential or even seqable, but must not have key-value
   pairs like a map.
   Examples of unkeyed collections include a vector (despite its associativity),
   a list, and a set (despite its values doubling as keys).
   A map is not an unkeyed collection."
  [x] (TODO))

(defns ?
  "Arity 1: Computes a type denoting a nilable value satisfying `t`.
   Arity 2: Computes whether `x` is nil or satisfies `t`."
  ([t utr/type? > utr/type?] (or nil? t))
  ([t utr/type?, x _ > c/boolean?] (c/or (c/nil? x) (t x))))

;; ===== Etc. ===== ;;

#?(:clj
(def boxed-class->unboxed-symbol
  {Boolean   'boolean
   Byte      'byte
   Short     'short
   Character 'char
   Integer   'int
   Long      'long
   Float     'float
   Double    'double}))

(uvar/defalias utdef/unboxed-symbol->type-meta)

#?(:clj (def primitive-classes (->> unboxed-symbol->type-meta vals (uc/map+ :unboxed) (join #{}))))

(defns- -type>classes
  [t utr/type?, classes c/set? > (us/set-of (us/nilable #?(:clj c/class? :cljs c/fn?)))]
  (cond (utr/class-type? t)
          (conj classes (utr/class-type>class t))
        (utr/value-type? t)
          (conj classes (-> t utr/value-type>value c/type))
        (c/= t universal-set)
          #?(:clj  #{nil java.lang.Object}
             :cljs (TODO "Not sure what to do in the case of universal CLJS set"))
        (c/= t empty-set)
          #{}
        (utr/and-type? t)
          (reduce (c/fn [classes' t'] (-type>classes t' classes'))
            classes (utr/and-type>args t))
        (utr/or-type? t)
          (reduce (c/fn [classes' t'] (-type>classes t' classes'))
            classes (utr/or-type>args t))
        :else
          (err! "Not sure how to handle type" t)))

(defns type>classes
  "Outputs the set of all the classes ->`t` can embody according to its various conditional
   branches, if any. Ignores nils, treating in Clojure simply as a `java.lang.Object`."
  [t utr/type? > (us/set-of (us/nilable #?(:clj c/class? :cljs c/fn?)))] (-type>classes t #{}))

#?(:clj
(defns- -type>?class-value [t utr/type?, type-nilable? c/boolean?]
  (if (utr/value-type? t)
      (let [v (utr/value-type>value t)]
        (when (c/class? v) {:class v :nilable? type-nilable?}))
      nil)))

#?(:clj
(defns type>?class-value
  "Outputs the single class value embodied by ->`t`.
   If a type is extensionally equal the *value* of a class, outputs that class.

   However, if a type does not embody the value of a class but rather merely embodies (as all types)
   an extensional subset of the set of all objects conforming to a class, outputs nil."
  {:examples `{(type>?class-value (value String)) {:class String :nilable? false}
               (type>?class-value (isa? String))  nil}}
  [t utr/type?] (-type>?class-value t false)))

;; ===== Validation and Conformance ===== ;;

(defns validate [x _ t utr/type?]
  (if-let [valid? (t x)]
    x
    (err! "Type-validation failed" {:type t :to-validate x})))

;; ---------------------- ;;
;; ===== Predicates ===== ;;
;; ---------------------- ;;

;; TODO TYPED — split the below predicate definitions into appropriate namespaces

;; ===== General ===== ;;

         (-def none?         empty-set)
         (-def any?          universal-set)

                              ;; TODO this is incomplete for CLJS base classes, I think
         (-def val|by-class? (or object? #?@(:cljs [(isa? js/String) (isa? js/Symbol)])))
         (-def val?          (not nil?))

         (-def ref?          (ref any?))

;; ===== Meta ===== ;;

#?(:clj  (-def class?           (isa? java.lang.Class)))
#?(:clj  (-def primitive-class? (or (value Boolean/TYPE)
                                    (value Byte/TYPE)
                                    (value Character/TYPE)
                                    (value Short/TYPE)
                                    (value Integer/TYPE)
                                    (value Long/TYPE)
                                    (value Float/TYPE)
                                    (value Double/TYPE))))
         ;; TODO for CLJS
#?(:clj  (-def protocol?        (>expr (ufn/fn-> :on-interface class?))))

;; ===== Primitives ===== ;;
;; NOTE these are kept here because they're used in both type analysis and various test namespaces

         (-def  boolean? (isa? #?(:clj Boolean :cljs js/Boolean)))
#?(:clj  (-def  byte?    (isa? Byte)))
#?(:clj  (-def  char?    (isa? Character)))
#?(:clj  (-def  short?   (isa? Short)))
#?(:clj  (-def  int?     (isa? Integer)))
#?(:clj  (-def  long?    (isa? Long)))
#?(:clj  (-def  float?   (isa? Float)))
         (-def  double?  (isa? #?(:clj Double :cljs js/Number)))

;; ===== Booleans ===== ;;

         (-def true?  (value true))
         (-def false? (value false))

;; ===== Numbers ===== ;;

;; ----- General ----- ;;

         (-def primitive-number? (or #?@(:clj [short? int? long? float?]) double?))

;; ========== Collections ========== ;;

;; ===== Tuples ===== ;;

         (-def tuple?           ;; clojure.lang.Tuple was discontinued; we won't support it for now
                                (isa? quantum.untyped.core.data.tuple.Tuple))
#?(:clj  (-def map-entry?       (isa? java.util.Map$Entry)))

;; ===== Sequences ===== ;; Sequential (generally not efficient Lookup / RandomAccess)

         (-def seq?             (isa? #?(:clj clojure.lang.ISeq    :cljs cljs.core/ISeq)))
         (-def cons?            (isa? #?(:clj clojure.lang.Cons    :cljs cljs.core/Cons)))
         (-def lseq?            (isa? #?(:clj clojure.lang.LazySeq :cljs cljs.core/LazySeq)))
         (-def misc-seq?        (or (isa? #?(:clj clojure.lang.APersistentMap$KeySeq       :cljs cljs.core/KeySeq))
                                    (isa? #?(:clj clojure.lang.APersistentMap$ValSeq       :cljs cljs.core/ValSeq))
                                    (isa? #?(:clj clojure.lang.PersistentVector$ChunkedSeq :cljs cljs.core/ChunkedSeq))
                                    (isa? #?(:clj clojure.lang.IndexedSeq                  :cljs cljs.core/IndexedSeq))))

         (-def non-list-seq?    (or cons? lseq? misc-seq?))

;; ----- Lists ----- ;; Not extremely different from Sequences ; TODO clean this up

         (-def cdlist?          none? #_(:clj  (or (isa? clojure.data.finger_tree.CountedDoubleList)
                                                   (isa? quantum.core.data.finger_tree.CountedDoubleList))
                                         :cljs (isa? quantum.core.data.finger-tree/CountedDoubleList)))
         (-def dlist?           none? #_(:clj  (or (isa? clojure.data.finger_tree.CountedDoubleList)
                                                   (isa? quantum.core.data.finger_tree.CountedDoubleList))
                                         :cljs (isa? quantum.core.data.finger-tree/CountedDoubleList)))
         (-def +list?           (isa? #?(:clj clojure.lang.IPersistentList :cljs cljs.core/IList)))
         (-def !list?           #?(:clj (isa? java.util.LinkedList) :cljs none?))
         (-def  list?           #?(:clj  (isa? java.util.List)
                                   :cljs +list?))

;; ----- Generic ----- ;;

;; ===== Arrays ===== ;; Sequential, Associative (specifically, whose keys are sequential,
                      ;; dense integer values), not extensible

#?(:clj
(defns >array-nd-type [kind c/symbol?, n unum/pos-int? > utr/class-type?]
  (let [prefix (apply str (repeat n \[))
        letter (case kind
                 boolean "Z"
                 byte    "B"
                 char    "C"
                 short   "S"
                 int     "I"
                 long    "J"
                 float   "F"
                 double  "D"
                 object  "Ljava.lang.Object;")]
    (isa? (Class/forName (str prefix letter))))))

#?(:clj
(defns >array-nd-types [n unum/pos-int? > utr/type?]
  (->> '[boolean byte char short int long float double object]
       (map #(>array-nd-type % n))
       (apply or))))

         (-def booleans?       #?(:clj (>array-nd-type 'boolean 1) :cljs none?))
         (-def bytes?          #?(:clj (>array-nd-type 'byte    1) :cljs (isa? js/Int8Array)))
         (-def ubytes?         #?(:clj none?                       :cljs (isa? js/Uint8Array)))
         (-def ubytes-clamped? #?(:clj none?                       :cljs (isa? js/Uint8ClampedArray)))
         (-def chars?          #?(:clj (>array-nd-type 'char    1) :cljs (isa? js/Uint16Array))) ; kind of
         (-def shorts?         #?(:clj (>array-nd-type 'short   1) :cljs (isa? js/Int16Array)))
         (-def ushorts?        #?(:clj none?                       :cljs (isa? js/Uint16Array)))
         (-def ints?           #?(:clj (>array-nd-type 'int     1) :cljs (isa? js/Int32Array)))
         (-def uints?          #?(:clj none?                       :cljs (isa? js/Uint32Array)))
         (-def longs?          #?(:clj (>array-nd-type 'long    1) :cljs none?))
         (-def floats?         #?(:clj (>array-nd-type 'float   1) :cljs (isa? js/Float32Array)))
         (-def doubles?        #?(:clj (>array-nd-type 'double  1) :cljs (isa? js/Float64Array)))
         (-def objects?        #?(:clj (>array-nd-type 'object  1) :cljs (isa? js/Array)))

         (-def numeric-1d?     (or bytes? ubytes? ubytes-clamped?
                                   chars?
                                   shorts? ushorts? ints? uints? longs?
                                   floats? doubles?))

         (-def array-1d?       (or booleans? bytes? ubytes? ubytes-clamped?
                                   chars?
                                   shorts? ushorts? ints? uints? longs?
                                   floats? doubles? objects?))

#?(:clj  (-def booleans-2d?    (>array-nd-type 'boolean 2)))
#?(:clj  (-def bytes-2d?       (>array-nd-type 'byte    2)))
#?(:clj  (-def chars-2d?       (>array-nd-type 'char    2)))
#?(:clj  (-def shorts-2d?      (>array-nd-type 'short   2)))
#?(:clj  (-def ints-2d?        (>array-nd-type 'int     2)))
#?(:clj  (-def longs-2d?       (>array-nd-type 'long    2)))
#?(:clj  (-def floats-2d?      (>array-nd-type 'float   2)))
#?(:clj  (-def doubles-2d?     (>array-nd-type 'double  2)))
#?(:clj  (-def objects-2d?     (>array-nd-type 'object  2)))

#?(:clj  (-def numeric-2d?     (or bytes-2d?
                                   chars-2d?
                                   shorts-2d? ints-2d? longs-2d?
                                   floats-2d? doubles-2d?)))

#?(:clj  (-def array-2d?       (>array-nd-types 2 )))

#?(:clj  (-def array-3d?       (>array-nd-types 3 )))
#?(:clj  (-def array-4d?       (>array-nd-types 4 )))
#?(:clj  (-def array-5d?       (>array-nd-types 5 )))
#?(:clj  (-def array-6d?       (>array-nd-types 6 )))
#?(:clj  (-def array-7d?       (>array-nd-types 7 )))
#?(:clj  (-def array-8d?       (>array-nd-types 8 )))
#?(:clj  (-def array-9d?       (>array-nd-types 9 )))
#?(:clj  (-def array-10d?      (>array-nd-types 10)))

         ;; TODO differentiate between "all supported n-D arrays" and "all n-D arrays"
         (-def objects-nd?     (or objects?
                                   #?@(:clj [(>array-nd-type 'object  2)
                                             (>array-nd-type 'object  3)
                                             (>array-nd-type 'object  4)
                                             (>array-nd-type 'object  5)
                                             (>array-nd-type 'object  6)
                                             (>array-nd-type 'object  7)
                                             (>array-nd-type 'object  8)
                                             (>array-nd-type 'object  9)
                                             (>array-nd-type 'object 10)])))

         ;; TODO differentiate between "all supported n-D arrays" and "all n-D arrays"
         (-def array?          (or array-1d?
                                   #?@(:clj [array-2d? array-3d? array-4d? array-5d?
                                             array-6d? array-7d? array-8d? array-9d? array-10d?])))

;; ----- String ----- ;; A special wrapper for char array where different encodings, etc. are possible

         ;; Mutable String
         (-def !string?  (isa? #?(:clj java.lang.StringBuilder :cljs goog.string.StringBuffer)))
         ;; Immutable String
         (-def  string?  (isa? #?(:clj java.lang.String        :cljs js/String)))

#?(:clj  (-def char-seq? (isa? java.lang.CharSequence)))

;; ===== Vectors ===== ;; Sequential, Associative (specifically, whose keys are sequential,
                       ;; dense integer values), extensible

         (-def !array-list?      #?(:clj  (or (isa? java.util.ArrayList)
                                              ;; indexed and associative, but not extensible
                                              (isa? java.util.Arrays$ArrayList))
                                    :cljs (or ;; not used
                                              #_(isa? cljs.core/ArrayList)
                                              ;; because supports .push etc.
                                              (isa? js/Array))))
         ;; svec = "spliceable vector"
         (-def   svector?          (isa? clojure.core.rrb_vector.rrbt.Vector))

         (-def   +vector?          (isa? #?(:clj  clojure.lang.IPersistentVector
                                            :cljs cljs.core/IVector)))

         (-def   +vector|built-in? (isa? #?(:clj  clojure.lang.PersistentVector
                                            :cljs cljs.core/PersistentVector)))

         (-def  !+vector?          (isa? #?(:clj  clojure.lang.ITransientVector
                                            :cljs cljs.core/ITransientVector)))
         (-def ?!+vector?          (or +vector? ?!+vector?))

         (-def  !vector|byte?     #?(:clj (isa? it.unimi.dsi.fastutil.bytes.ByteArrayList)     :cljs none?))
         (-def  !vector|short?    #?(:clj (isa? it.unimi.dsi.fastutil.shorts.ShortArrayList)   :cljs none?))
         (-def  !vector|char?     #?(:clj (isa? it.unimi.dsi.fastutil.chars.CharArrayList)     :cljs none?))
         (-def  !vector|int?      #?(:clj (isa? it.unimi.dsi.fastutil.ints.IntArrayList)       :cljs none?))
         (-def  !vector|long?     #?(:clj (isa? it.unimi.dsi.fastutil.longs.LongArrayList)     :cljs none?))
         (-def  !vector|float?    #?(:clj (isa? it.unimi.dsi.fastutil.floats.FloatArrayList)   :cljs none?))
         (-def  !vector|double?   #?(:clj (isa? it.unimi.dsi.fastutil.doubles.DoubleArrayList) :cljs none?))

         (-def  !vector|ref?      #?(:clj  (or (isa? java.util.ArrayList)
                                               (isa? it.unimi.dsi.fastutil.objects.ReferenceArrayList))
                                     ;; because supports .push etc.
                                     :cljs (isa? js/Array)))

         (-def  !vector?          (or !vector|ref?
                                      !vector|byte?  !vector|short? !vector|char?
                                      !vector|int?   !vector|long?
                                      !vector|float? !vector|double?))

                                   ;; java.util.Vector is deprecated, because you can
                                   ;; just create a synchronized wrapper over an ArrayList
                                   ;; via java.util.Collections
#?(:clj  (-def !!vector?           none?))
         (-def   vector?           (or ?!+vector? !vector? #?(:clj !!vector?)))

;; ===== Queues ===== ;; Particularly FIFO queues, as LIFO = stack = any vector

         (-def   +queue? (isa? #?(:clj  clojure.lang.PersistentQueue
                                  :cljs cljs.core/PersistentQueue)))
         (-def  !+queue? none?)
         (-def ?!+queue? (or +queue? !+queue?))
#?(:clj  (-def  !!queue? (or (isa? java.util.concurrent.BlockingQueue)
                             (isa? java.util.concurrent.TransferQueue)
                             (isa? java.util.concurrent.ConcurrentLinkedQueue))))

         (-def   !queue? #?(:clj  ;; Considered single-threaded mutable unless otherwise noted
                                  (identity #_- (isa? java.util.Queue) #_(or ?!+queue? !!queue?)) ; TODO re-enable once `-` works
                            :cljs (isa? goog.structs.Queue)))

         (-def    queue? (or ?!+queue? !queue? #?(:clj !!queue?)))

;; ===== Sets ===== ;; Associative; A special type of Map whose keys and vals are identical

#?(:clj  (-def    java-set?              (isa? java.util.Set)))

;; ----- Identity Sets (identity-based equality) ----- ;;

         (-def   !identity-set? #?(:clj  none? #_(isa? java.util.IdentityHashSet) ; TODO implement
                                   :cljs (isa? js/Set)))

         (-def   identity-set? !identity-set?)

;; ----- Hash Sets (value-based equality) ----- ;;

         (-def   +hash-set?              (isa? #?(:clj  clojure.lang.PersistentHashSet
                                                  :cljs cljs.core/PersistentHashSet)))
         (-def  !+hash-set?              (isa? #?(:clj  clojure.lang.PersistentHashSet$TransientHashSet
                                                  :cljs cljs.core/TransientHashSet)))
         (-def ?!+hash-set?              (or +hash-set? !+hash-set?))

         (-def   !hash-set|byte?         #?(:clj (isa? it.unimi.dsi.fastutil.bytes.ByteOpenHashSet)     :cljs none?))
         (-def   !hash-set|char?         #?(:clj (isa? it.unimi.dsi.fastutil.chars.CharOpenHashSet)     :cljs none?))
         (-def   !hash-set|short?        #?(:clj (isa? it.unimi.dsi.fastutil.shorts.ShortOpenHashSet)   :cljs none?))
         (-def   !hash-set|int?          #?(:clj (isa? it.unimi.dsi.fastutil.ints.IntOpenHashSet)       :cljs none?))
         (-def   !hash-set|long?         #?(:clj (isa? it.unimi.dsi.fastutil.longs.LongOpenHashSet)     :cljs none?))
         (-def   !hash-set|float?        #?(:clj (isa? it.unimi.dsi.fastutil.floats.FloatOpenHashSet)   :cljs none?))
         (-def   !hash-set|double?       #?(:clj (isa? it.unimi.dsi.fastutil.doubles.DoubleOpenHashSet) :cljs none?))
         (-def   !hash-set|ref?          #?(:clj  (or (isa? java.util.HashSet)
                                                      (isa? it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet))
                                            :cljs none?))

         (-def   !hash-set?              (or !hash-set|ref?
                                             !hash-set|byte? !hash-set|short? !hash-set|char?
                                             !hash-set|int? !hash-set|long?
                                             !hash-set|float? !hash-set|double?))

         ;; CLJ technically can have via ConcurrentHashMap with same KVs but this hasn't been implemented yet
#?(:clj  (-def  !!hash-set?              none?))
         (-def    hash-set?              (or ?!+hash-set? !hash-set? #?(:clj !!hash-set?)))

;; ----- Unsorted Sets ----- ;;

         (-def   +unsorted-set?            +hash-set?)
         (-def  !+unsorted-set?           !+hash-set?)
         (-def ?!+unsorted-set?          ?!+hash-set?)

         (-def   !unsorted-set|byte?     !hash-set|byte?)
         (-def   !unsorted-set|short?    !hash-set|char?)
         (-def   !unsorted-set|char?     !hash-set|short?)
         (-def   !unsorted-set|int?      !hash-set|int?)
         (-def   !unsorted-set|long?     !hash-set|long?)
         (-def   !unsorted-set|float?    !hash-set|float?)
         (-def   !unsorted-set|double?   !hash-set|double?)
         (-def   !unsorted-set|ref?      !hash-set|ref?)

         (-def   !unsorted-set?          (or !unsorted-set|ref?
                                             !unsorted-set|byte? !unsorted-set|short? !unsorted-set|char?
                                             !unsorted-set|int? !unsorted-set|long?
                                             !unsorted-set|float? !unsorted-set|double?))

#?(:clj  (-def  !!unsorted-set?          !!hash-set?))
         (-def    unsorted-set?            hash-set?)

;; ----- Sorted Sets ----- ;;

         (-def   +sorted-set?            (isa? #?(:clj  clojure.lang.PersistentTreeSet
                                                  :cljs cljs.core/PersistentTreeSet)))
         (-def  !+sorted-set?            none?)
         (-def ?!+sorted-set?            (or +sorted-set? !+sorted-set?))

         (-def   !sorted-set|byte?       #?(:clj  (isa? it.unimi.dsi.fastutil.bytes.ByteSortedSet)
                                            :cljs none?))
         (-def   !sorted-set|short?      #?(:clj (isa? it.unimi.dsi.fastutil.shorts.ShortSortedSet)                                 :cljs none?))
         (-def   !sorted-set|char?       #?(:clj (isa? it.unimi.dsi.fastutil.chars.CharSortedSet)                                 :cljs none?))
         (-def   !sorted-set|int?        #?(:clj (isa? it.unimi.dsi.fastutil.ints.IntSortedSet)                                 :cljs none?))
         (-def   !sorted-set|long?       #?(:clj (isa? it.unimi.dsi.fastutil.longs.LongSortedSet)                                 :cljs none?))
         (-def   !sorted-set|float?      #?(:clj (isa? it.unimi.dsi.fastutil.floats.FloatSortedSet)                                 :cljs none?))
         (-def   !sorted-set|double?     #?(:clj (isa? it.unimi.dsi.fastutil.doubles.DoubleSortedSet)
                                            :cljs none?))
         ;; CLJS technically can have via goog.structs.AVLTree with same KVs but this hasn't been implemented yet
         (-def   !sorted-set|ref?        #?(:clj (isa? java.util.TreeSet) :cljs none?))

         (-def   !sorted-set?            (or !sorted-set|ref?
                                             !sorted-set|byte? !sorted-set|short? !sorted-set|char?
                                             !sorted-set|int? !sorted-set|long?
                                             !sorted-set|float? !sorted-set|double?))

         ;; CLJ technically can have via ConcurrentSkipListMap with same KVs but this hasn't been implemented yet
#?(:clj  (-def  !!sorted-set?            none?))
         (-def    sorted-set?            (or ?!+sorted-set? !sorted-set? #?@(:clj [!!sorted-set? (isa? java.util.SortedSet)])))

;; ----- Other Sets ----- ;;

         (-def   +insertion-ordered-set? (or (isa? linked.set.LinkedSet)
                                           ;; This is true, but we have replaced OrderedSet with LinkedSet
                                           #_(:clj (isa? flatland.ordered.set.OrderedSet))))
         (-def  !+insertion-ordered-set? none?
                                         ;; This is true, but we have replaced OrderedSet with LinkedSet
                                         #_(isa? flatland.ordered.set.TransientOrderedSet))
         (-def ?!+insertion-ordered-set? (or +insertion-ordered-set? !+insertion-ordered-set?))

         (-def   !insertion-ordered-set? #?(:clj (isa? java.util.LinkedHashSet) :cljs none?))

         ;; CLJ technically can have via ConcurrentLinkedHashMap with same KVs but this hasn't been implemented yet
#?(:clj  (-def  !!insertion-ordered-set? none?))

         (-def    insertion-ordered-set? (or ?!+insertion-ordered-set? !insertion-ordered-set? #?(:clj !!insertion-ordered-set?)))

;; ----- General Sets ----- ;;

         (-def  !+set?                   (isa? #?(:clj  clojure.lang.ITransientSet
                                                  :cljs cljs.core/ITransientSet)))

         (-def   +set|built-in?          (or (isa? #?(:clj clojure.lang.PersistentHashSet :cljs cljs.core/PersistentHashSet))
                                             (isa? #?(:clj clojure.lang.PersistentTreeSet :cljs cljs.core/PersistentTreeSet))))

         (-def   +set?                   (isa? #?(:clj  clojure.lang.IPersistentSet
                                                  :cljs cljs.core/ISet)))
         (-def ?!+set?                   (or !+set? +set?))

         (-def   !set|byte?              #?(:clj (isa? it.unimi.dsi.fastutil.bytes.ByteSet)     :cljs none?))
         (-def   !set|short?             #?(:clj (isa? it.unimi.dsi.fastutil.shorts.ShortSet)   :cljs none?))
         (-def   !set|char?              #?(:clj (isa? it.unimi.dsi.fastutil.chars.CharSet)     :cljs none?))
         (-def   !set|int?               #?(:clj (isa? it.unimi.dsi.fastutil.ints.IntSet)       :cljs none?))
         (-def   !set|long?              #?(:clj (isa? it.unimi.dsi.fastutil.longs.LongSet)     :cljs none?))
         (-def   !set|float?             #?(:clj (isa? it.unimi.dsi.fastutil.floats.FloatSet)   :cljs none?))
         (-def   !set|double?            #?(:clj (isa? it.unimi.dsi.fastutil.doubles.DoubleSet) :cljs none?))
         (-def   !set|ref?               (or !unsorted-set|ref? !sorted-set|ref?))

         (-def   !set?                   (or !set|ref?
                                             !set|byte? !set|short? !set|char?
                                             !set|int? !set|long?
                                             !set|float? !set|double?))

#?(:clj  (-def  !!set?                   (or !!unsorted-set? !!sorted-set?)))
         (-def    set?                   (or ?!+set? !set? #?@(:clj [!!set? (isa? java.util.Set)])))

;; ===== Functions ===== ;;

         (-def fn?          (isa? #?(:clj clojure.lang.Fn  :cljs js/Function)))

         (-def ifn?         (isa? #?(:clj clojure.lang.IFn :cljs cljs.core/IFn)))

         (-def fnt?         (and fn? (>expr (fn-> c/meta ::type))))

         (-def multimethod? (isa? #?(:clj clojure.lang.MultiFn :cljs cljs.core/IMultiFn)))

         ;; I.e., can you call/invoke it by being in functor position (first element of an unquoted
         ;; list) within a typed context?
         ;; TODO should we allow java.lang.Runnable, java.util.concurrent.Callable, and other
         ;; functional interfaces to be `callable?`?
         (-def callable?    (or ifn? fnt?))

;; ===== References ===== ;;

         (-def atom?     (isa? #?(:clj clojure.lang.IAtom :cljs cljs.core/IAtom)))

         (-def volatile? (isa? #?(:clj clojure.lang.Volatile :cljs cljs.core/Volatile)))

#?(:clj  (-def atomic?   (or atom? volatile?
                             java.util.concurrent.atomic.AtomicReference
                             ;; From the java.util.concurrent package:
                             ;; "Additionally, classes are provided only for those
                             ;;  types that are commonly useful in intended applications.
                             ;;  For example, there is no atomic class for representing
                             ;;  byte. In those infrequent cases where you would like
                             ;;  to do so, you can use an AtomicInteger to hold byte
                             ;;  values, and cast appropriately. You can also hold floats
                             ;;  using Float.floatToIntBits and Float.intBitstoFloat
                             ;;  conversions, and doubles using Double.doubleToLongBits
                             ;;  and Double.longBitsToDouble conversions."
                             java.util.concurrent.atomic.AtomicBoolean
                           #_java.util.concurrent.atomic.AtomicByte
                           #_java.util.concurrent.atomic.AtomicShort
                             java.util.concurrent.atomic.AtomicInteger
                             java.util.concurrent.atomic.AtomicLong
                           #_java.util.concurrent.atomic.AtomicFloat
                           #_java.util.concurrent.atomic.AtomicDouble
                             com.google.common.util.concurrent.AtomicDouble)))

;; ===== Miscellaneous ===== ;;

         (-def metable?      (isa? #?(:clj clojure.lang.IMeta :cljs cljs.core/IMeta)))
         (-def with-metable? (isa? #?(:clj clojure.lang.IObj  :cljs cljs.core/IWithMeta)))

#?(:clj  (-def thread?       (isa? java.lang.Thread)))

         ;; Able to be used with `throw`
         (-def throwable?    #?(:clj (isa? java.lang.Throwable) :cljs any?))

         (-def regex?        (isa? #?(:clj java.util.regex.Pattern :cljs js/RegExp)))

         (-def chan?         (isa? #?(:clj  clojure.core.async.impl.protocols/Channel
                                      :cljs cljs.core.async.impl.protocols/Channel)))

         (-def keyword?      (isa? #?(:clj clojure.lang.Keyword :cljs cljs.core/Keyword)))
         (-def symbol?       (isa? #?(:clj clojure.lang.Symbol  :cljs cljs.core/Symbol)))

#?(:clj  (-def namespace?    (isa? clojure.lang.Namespace)))

#?(:clj  (-def var?          (isa? clojure.lang.Var)))

         ;; `js/File` isn't always available! Use an abstraction
#?(:clj  (-def file?         (isa? java.io.File)))

         (-def comparable?   #?(:clj  (isa? java.lang.Comparable)
                                ;; TODO other things are comparable; really it depends on the two objects in question
                                :cljs (or nil? (isa? cljs.core/IComparable))))

         (-def record?       (isa? #?(:clj clojure.lang.IRecord :cljs cljs.core/IRecord)))

         (-def transformer?  (isa? quantum.untyped.core.reducers.Transformer))

         (-def delay?        (isa? #?(:clj clojure.lang.Delay :cljs cljs.core/Delay)))

;; ----- Collections ----- ;;

         (-def sorted?         (or (isa? #?(:clj clojure.lang.Sorted :cljs cljs.core/ISorted))
                                   #?@(:clj  [(isa? java.util.SortedMap)
                                              (isa? java.util.SortedSet)]
                                       :cljs [(isa? goog.structs.AvlTree)])
                                   ;; TODO implement — monotonically <, <=, =, >=, >
                                   #_(>expr monotonic?)))

         (-def transient?      (isa? #?(:clj  clojure.lang.ITransientCollection
                                        :cljs cljs.core/ITransientCollection)))

         (-def editable?       (isa? #?(:clj  clojure.lang.IEditableCollection
                                        :cljs cljs.core/IEditableCollection)))

         ;; Indicates efficient lookup by (integer) index (via `get`)
         (-def indexed?        (or (isa? #?(:clj clojure.lang.Indexed :cljs cljs.core/IIndexed))
                                   ;; Doesn't guarantee `java.util.List` is implemented, except by
                                   ;; convention
                                   #?(:clj (isa? java.util.RandomAccess))
                                   #?(:clj char-seq? :cljs string?)
                                   array?))

         ;; Indicates whether `assoc?!` is supported
         (-def associative?    (or (isa? #?(:clj clojure.lang.Associative           :cljs cljs.core/IAssociative))
                                   (isa? #?(:clj clojure.lang.ITransientAssociative :cljs cljs.core/ITransientAssociative))
                                   (or map? indexed?)))

         (-def sequential?     (or (isa? #?(:clj clojure.lang.Sequential :cljs cljs.core/ISequential))
                                   list? indexed?))

         (-def counted?        (or (isa? #?(:clj clojure.lang.Counted :cljs cljs.core/ICounted))
                                   #?(:clj char-seq? :cljs string?) vector? map? set? array?))

#?(:clj  (-def java-coll?      (isa? java.util.Collection)))

         ;; A group of objects/elements
         (-def coll?           (or #?(:clj java-coll?)
                                   #?@(:clj  [(isa? clojure.lang.IPersistentCollection)
                                              (isa? clojure.lang.ITransientCollection)]
                                       :cljs (isa? cljs.core/ICollection))
                                   sequential? associative?))

         (-def iterable?       (isa? #?(:clj java.lang.Iterable :cljs cljs.core/IIterable)))

         ;; Whatever is `seqable?` is reducible via a call to `seq`.
         ;; Reduction is nearly always preferable to seq-iteration if for no other reason than that
         ;; it can take advantage of transducers and reducers. This predicate just answers whether
         ;; it is more efficient to reduce than to seq-iterate (note that it should be at least as
         ;; efficient as seq-iteration).
         ;; TODO re-enable when dispatch enabled
         #_(-def prefer-reduce?  (or (isa? #?(:clj clojure.lang.IReduceInit :cljs cljs.core/IReduce))
                                   (isa? #?(:clj clojure.lang.IKVReduce   :cljs cljs.core/IKVReduce))
                                   #?(:clj (isa? clojure.core.protocols/IKVReduce))
                                   #?(:clj char-seq? :cljs string?)
                                   array?
                                   record?
                                   (isa? #?(:clj fast_zip.core.ZipperLocation :cljs fast-zip.core/ZipperLocation))
                                   chan?))

         ;; Whatever is `reducible?` is seqable via a call to `sequence`.
         (-def seqable?        (or #?@(:clj  [(isa? clojure.lang.Seqable)
                                              iterable?
                                              char-seq?
                                              map?
                                              array?]
                                       :cljs [(isa? cljs.core/ISeqable)
                                              array?
                                              string?])))

         ;; Able to be traversed over in some fashion, whether by `first`/`next` seq-iteration,
         ;; reduction, etc.
         ;; TODO re-enable when dispatch enabled
         #_(-def traversable?    (or (isa? #?(:clj clojure.lang.IReduceInit :cljs cljs.core/IReduce))
                                   (isa? #?(:clj clojure.lang.IKVReduce :cljs cljs.core/IKVReduce))
                                   #?(:clj (isa? clojure.core.protocols/IKVReduce))
                                   (isa? #?(:clj clojure.lang.Seqable :cljs cljs.core/ISeqable))
                                   iterable?
                                   #?(:clj char-seq? :cljs string?)
                                   array?
                                   (isa? #?(:clj fast_zip.core.ZipperLocation :cljs fast-zip.core/ZipperLocation))
                                   chan?))

#?(:clj  (-def tagged-literal?   (isa? clojure.lang.TaggedLiteral)))

         (-def literal?          (or nil? boolean? symbol? keyword? string? #?(:clj long?) double? #?(:clj tagged-literal?)))
       #_(-def form?             (or literal? +list? +vector? ...))

;; ===== Generic ===== ;;

         ;; Standard "uncuttable" types
         (-def integral?  (or primitive? number?))

         ;; TODO make into a type
         (def  nneg-int?  #(c/and (integer? %) (c/>= % 0)))
         ;; TODO make into a type
         (def  index?     nneg-int?)
