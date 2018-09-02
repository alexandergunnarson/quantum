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
            meta
            ref volatile?
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
           [quantum.untyped.core.convert
             :refer [>symbol]]
           [quantum.untyped.core.core                  :as ucore]
           [quantum.untyped.core.data.bits             :as ubit]
           [quantum.untyped.core.data.hash             :as uhash]
           [quantum.untyped.core.data.map
             #?@(:cljs [:refer [MutableHashMap]])]
           [quantum.untyped.core.data.set              :as uset]
           [quantum.untyped.core.data.tuple]
           [quantum.untyped.core.defnt
             :refer [defns defns-]]
           [quantum.untyped.core.error                 :as uerr
             :refer [err! TODO catch-all]]
           [quantum.untyped.core.fn                    :as ufn
             :refer [fn1 rcomp <- fn->]]
           [quantum.untyped.core.form.generate.deftype :as udt]
           [quantum.untyped.core.logic
             :refer [fn-and ifs whenp->]]
           [quantum.untyped.core.numeric               :as unum]
           [quantum.untyped.core.print                 :as upr]
           [quantum.untyped.core.qualify               :as qual]
           [quantum.untyped.core.reducers              :as ur
             :refer [educe join]]
           [quantum.untyped.core.refs
             :refer [?deref]]
           [quantum.untyped.core.spec                  :as us]
           [quantum.untyped.core.type.compare          :as utcomp]
           [quantum.untyped.core.type.core             :as utcore]
           [quantum.untyped.core.type.defs             :as utdef]
           [quantum.untyped.core.type.predicates       :as utpred]
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
       (NotType. uhash/default uhash/default t)))

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

(defns- isa?|protocol [p utpred/protocol?]
  (ProtocolType. uhash/default uhash/default nil p nil))

;; ----- ClassType ----- ;;

(defns- isa?|class [c #?(:clj c/class? :cljs c/fn?)]
  (ClassType. uhash/default uhash/default nil c nil))

;; ----- ValueType ----- ;;

(defns value
  "Creates a type whose extension is the singleton set containing only the value `v`."
  [v _] (ValueType. uhash/default uhash/default v))

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
                                    ClassType (AndType. uhash/default uhash/default [t0 (not t1)] (atom nil)))
                        ValueType (condp == c1
                                    ValueType (AndType. uhash/default uhash/default [t0 (not t1)] (atom nil))))
              OrType  (condp == c1
                        ClassType (let [args (->> t0 utr/or-type>args (uc/remove (fn1 = t1)))]
                                    (case (count args)
                                      0 empty-set
                                      1 (first args)
                                      (OrType. uhash/default uhash/default args (atom nil))))))))))
  ([t0 utr/type?, t1 utr/type? & ts (us/seq-of utr/type?) > utr/type?] (reduce - (- t0 t1) ts)))

(defn isa? [x]
  (ifs (utpred/protocol? x)
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
          (utpred/protocol? x)
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
                 #_(register-type! '~(qual/qualify sym) t#)
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
            (construct-fn uhash/default uhash/default simplified (atom nil))))))

;; TODO do this?
#_(udt/deftype SequentialType)

#_(defns of
  "Creates a type that ... TODO"
  [pred (<= iterable?), t utr/type?] (TODO))

(defn fn [arity & arities] ; TODO fix — & args should have been sufficient but `defnt` has a bug that way
  (let [name- nil
        arities-form (cons arity arities)
        arities (->> arities-form
                     (uc/map+ #(us/conform ::fn-type|arity %))
                     (uc/group-by #(-> % :input-types count)))]
    (FnType. name- arities-form arities)))

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

        (def basic-type-syms '[boolean byte char short int long float double ref])

#?(:clj (defns- >v-sym [prefix c/symbol?, kind c/symbol? > c/symbol?]
          (symbol (str prefix "|" kind "?"))))

#?(:clj (defns- >kv-sym [prefix c/symbol?, from-type c/symbol?, to-type c/symbol? > c/symbol?]
          (symbol (str prefix "|" from-type "->" to-type "?"))))

#?(:clj (defmacro- def-preds|map|same-types [prefix #_symbol?]
          `(do ~@(for [kind (conj basic-type-syms 'any)]
                   (list `-def (>v-sym prefix kind) (>kv-sym prefix kind kind))))))

#?(:clj (defmacro- def-preds|map|any [prefix #_symbol?]
          (let [anys (->> (for [kind basic-type-syms]
                            [(list `-def (>kv-sym prefix kind 'any)
                                         (->> basic-type-syms (map #(>kv-sym prefix kind %)) (list* `or)))
                             (list `-def (>kv-sym prefix 'any kind)
                                         (->> basic-type-syms (map #(>kv-sym prefix % kind)) (list* `or)))])
                          (apply concat))
                any->any (list `-def (>kv-sym prefix 'any 'any)
                                     (->> basic-type-syms
                                          (map #(vector (>kv-sym prefix 'any %) (>kv-sym prefix % 'any)))
                                          (apply concat)
                                          (list* `or)))]
            `(do ~@(concat anys [any->any])))))

;; TODO TYPED — split the below predicate definitions into appropriate namespaces

;; ===== General ===== ;;

         (-def none?         empty-set)
         (-def any?          universal-set)

                              ;; TODO this is incomplete for CLJS base classes, I think
         (-def val|by-class? (or object? #?@(:cljs [(isa? js/String) (isa? js/Symbol)])))
         (-def val?          (not nil?))

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

         (-def  boolean?  (isa? #?(:clj Boolean :cljs js/Boolean)))
         (-def ?boolean?  (? boolean?))

#?(:clj  (-def  byte?     (isa? Byte)))
#?(:clj  (-def ?byte?     (? byte?)))

#?(:clj  (-def  char?     (isa? Character)))
#?(:clj  (-def ?char?     (? char?)))

#?(:clj  (-def  short?    (isa? Short)))
#?(:clj  (-def ?short?    (? short?)))

#?(:clj  (-def  int?      (isa? Integer)))
#?(:clj  (-def ?int?      (? int?)))

#?(:clj  (-def  long?     (isa? Long)))
#?(:clj  (-def ?long?     (? long?)))

#?(:clj  (-def  float?    (isa? Float)))
#?(:clj  (-def ?float?    (? float?)))

         (-def  double?   (isa? #?(:clj Double :cljs js/Number)))
         (-def ?double?   (? double?))

         (-def primitive? (or boolean? #?@(:clj [byte? short? char? int? long? float?]) double?))

#?(:clj  (-def comparable-primitive? (- primitive? boolean?)))

;; ===== Booleans ===== ;;

         (-def true?  (value true))
         (-def false? (value false))

;; ===== Numbers ===== ;;

;; ----- Integers ----- ;;

         (-def bigint?           #?(:clj  (or (isa? clojure.lang.BigInt) (isa? java.math.BigInteger))
                                    :cljs (isa? com.gfredericks.goog.math.Integer)))

         (-def integer?          (or #?@(:clj [byte? short? int? long?]) bigint?))

;; ----- Decimals ----- ;;

#?(:clj  (-def bigdec?           (isa? java.math.BigDecimal))) ; TODO CLJS may have this

         (-def decimal?          (or #?(:clj float?) double? #?(:clj bigdec?)))

;; ----- General ----- ;;

         (-def ratio?            (isa? #?(:clj  clojure.lang.Ratio
                                          :cljs quantum.core.numeric.types.Ratio))) ; TODO add this CLJS entry to the predicate after the fact

         (-def primitive-number? (or #?@(:clj [short? int? long? float?]) double?))

         (-def number?           (or #?@(:clj  [(isa? java.lang.Number)]
                                         :cljs [integer? decimal? ratio?])))

;; ----- Likenesses ----- ;;

       #_(-def integer-value?              (or integer? (and decimal? (>expr unum/integer-value?))))

       #_(-def numeric-primitive?          (and primitive? (not boolean?)))

       #_(-def numerically-byte?           (and integer-value? (>expr (c/fn [x] (c/<= -128                 x 127)))))
       #_(-def numerically-short?          (and integer-value? (>expr (c/fn [x] (c/<= -32768               x 32767)))))
       #_(-def numerically-char?           (and integer-value? (>expr (c/fn [x] (c/<=  0                   x 65535)))))
       #_(-def numerically-unsigned-short? numerically-char?)
       #_(-def numerically-int?            (and integer-value? (>expr (c/fn [x] (c/<= -2147483648          x 2147483647)))))
       #_(-def numerically-long?           (and integer-value? (>expr (c/fn [x] (c/<= -9223372036854775808 x 9223372036854775807)))))
       #_(-def numerically-float?          (and number?
                                                (>expr (c/fn [x] (c/<= -3.4028235E38 x 3.4028235E38)))
                                                (>expr (c/fn [x] (-> x #?(:clj clojure.lang.RT/floatCast :cljs c/float) (c/== x))))))
       #_(-def numerically-double?         (and number?
                                                (>expr (c/fn [x] (c/<= -1.7976931348623157E308 x 1.7976931348623157E308)))
                                                (>expr (c/fn [x] (-> x clojure.lang.RT/doubleCast (c/== x))))))

       #_(-def int-like?                   (and integer-value? numerically-int?))

#_(defn numerically
  [t]
  (assert (utr/class-type? t))
  (let [c (.-c ^ClassType t)]
    (case (.getName ^Class c)
      "java.lang.Byte"      numerically-byte?
      "java.lang.Short"     numerically-short?
      "java.lang.Character" numerically-char?
      "java.lang.Integer"   numerically-int?
      "java.lang.Long"      numerically-long?
      "java.lang.Float"     numerically-float?
      ;; TODO fix
      ;;"java.lang.Double"    numerically-double?
      (err! "Could not find numerical range type for class" {:c c}))))

;; ========== Collections ========== ;;

;; ===== Tuples ===== ;;

         (-def tuple?           ;; clojure.lang.Tuple was discontinued; we won't support it for now
                                (isa? quantum.untyped.core.data.tuple.Tuple))
#?(:clj  (-def map-entry?       (isa? java.util.Map$Entry)))
         (-def +map-entry?      (isa? #?(:clj clojure.lang.MapEntry :cljs cljs.core.MapEntry)))

;; ===== Sequences ===== ;; Sequential (generally not efficient Lookup / RandomAccess)

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
(defns >array-nd-type [kind c/symbol?, n utpred/pos-int? > utr/class-type?]
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
(defns >array-nd-types [n utpred/pos-int? > utr/type?]
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

;; ===== Maps ===== ;; Associative

;; ----- Identity Maps (identity-based equality) ----- ;;

         (-def   !identity-map|ref->ref? #?(:clj  (isa? java.util.IdentityHashMap)
                                            :cljs (isa? js/Map)))

         (-def   !identity-map?          !identity-map|ref->ref?)

#?(:clj  (-def  !!identity-map?          none?))

         (-def    identity-map?          (or !identity-map? #?(:clj !!identity-map?)))

;; ----- Hash Maps (value-based equality) ----- ;;

         (-def   +hash-map?                  (isa? #?(:clj  clojure.lang.PersistentHashMap
                                                      :cljs cljs.core/PersistentHashMap)))

         (-def  !+hash-map?                  (isa? #?(:clj  clojure.lang.PersistentHashMap$TransientHashMap
                                                      :cljs cljs.core/TransientHashMap)))

         (-def ?!+hash-map?                  (or !+hash-map? +hash-map?))

         (-def   !hash-map|boolean->boolean? none?)
         (-def   !hash-map|boolean->byte?    none?)
         (-def   !hash-map|boolean->char?    none?)
         (-def   !hash-map|boolean->short?   none?)
         (-def   !hash-map|boolean->int?     none?)
         (-def   !hash-map|boolean->long?    none?)
         (-def   !hash-map|boolean->float?   none?)
         (-def   !hash-map|boolean->double?  none?)
         (-def   !hash-map|boolean->ref?     none?)

         (-def   !hash-map|byte->boolean?    #?(:clj (or (isa? it.unimi.dsi.fastutil.bytes.Byte2BooleanOpenHashMap)        (isa? it.unimi.dsi.fastutil.bytes.Byte2BooleanOpenCustomHashMap))        :cljs none?))
         (-def   !hash-map|byte->byte?       #?(:clj (or (isa? it.unimi.dsi.fastutil.bytes.Byte2ByteOpenHashMap)           (isa? it.unimi.dsi.fastutil.bytes.Byte2ByteOpenCustomHashMap))           :cljs none?))
         (-def   !hash-map|byte->char?       #?(:clj (or (isa? it.unimi.dsi.fastutil.bytes.Byte2CharOpenHashMap)           (isa? it.unimi.dsi.fastutil.bytes.Byte2CharOpenCustomHashMap))           :cljs none?))
         (-def   !hash-map|byte->short?      #?(:clj (or (isa? it.unimi.dsi.fastutil.bytes.Byte2ShortOpenHashMap)          (isa? it.unimi.dsi.fastutil.bytes.Byte2ShortOpenCustomHashMap))          :cljs none?))
         (-def   !hash-map|byte->int?        #?(:clj (or (isa? it.unimi.dsi.fastutil.bytes.Byte2IntOpenHashMap)            (isa? it.unimi.dsi.fastutil.bytes.Byte2IntOpenCustomHashMap))            :cljs none?))
         (-def   !hash-map|byte->long?       #?(:clj (or (isa? it.unimi.dsi.fastutil.bytes.Byte2LongOpenHashMap)           (isa? it.unimi.dsi.fastutil.bytes.Byte2LongOpenCustomHashMap))           :cljs none?))
         (-def   !hash-map|byte->float?      #?(:clj (or (isa? it.unimi.dsi.fastutil.bytes.Byte2FloatOpenHashMap)          (isa? it.unimi.dsi.fastutil.bytes.Byte2FloatOpenCustomHashMap))          :cljs none?))
         (-def   !hash-map|byte->double?     #?(:clj (or (isa? it.unimi.dsi.fastutil.bytes.Byte2DoubleOpenHashMap)         (isa? it.unimi.dsi.fastutil.bytes.Byte2DoubleOpenCustomHashMap))         :cljs none?))
         (-def   !hash-map|byte->ref?        #?(:clj (or (isa? it.unimi.dsi.fastutil.bytes.Byte2ReferenceOpenHashMap)      (isa? it.unimi.dsi.fastutil.bytes.Byte2ReferenceOpenCustomHashMap))      :cljs none?))

         (-def   !hash-map|char->ref?        #?(:clj (or (isa? it.unimi.dsi.fastutil.chars.Char2ReferenceOpenHashMap)      (isa? it.unimi.dsi.fastutil.chars.Char2ReferenceOpenCustomHashMap))      :cljs none?))
         (-def   !hash-map|char->boolean?    #?(:clj (or (isa? it.unimi.dsi.fastutil.chars.Char2BooleanOpenHashMap)        (isa? it.unimi.dsi.fastutil.chars.Char2BooleanOpenCustomHashMap))        :cljs none?))
         (-def   !hash-map|char->byte?       #?(:clj (or (isa? it.unimi.dsi.fastutil.chars.Char2ByteOpenHashMap)           (isa? it.unimi.dsi.fastutil.chars.Char2ByteOpenCustomHashMap))           :cljs none?))
         (-def   !hash-map|char->char?       #?(:clj (or (isa? it.unimi.dsi.fastutil.chars.Char2CharOpenHashMap)           (isa? it.unimi.dsi.fastutil.chars.Char2CharOpenCustomHashMap))           :cljs none?))
         (-def   !hash-map|char->short?      #?(:clj (or (isa? it.unimi.dsi.fastutil.chars.Char2ShortOpenHashMap)          (isa? it.unimi.dsi.fastutil.chars.Char2ShortOpenCustomHashMap))          :cljs none?))
         (-def   !hash-map|char->int?        #?(:clj (or (isa? it.unimi.dsi.fastutil.chars.Char2IntOpenHashMap)            (isa? it.unimi.dsi.fastutil.chars.Char2IntOpenCustomHashMap))            :cljs none?))
         (-def   !hash-map|char->long?       #?(:clj (or (isa? it.unimi.dsi.fastutil.chars.Char2LongOpenHashMap)           (isa? it.unimi.dsi.fastutil.chars.Char2LongOpenCustomHashMap))           :cljs none?))
         (-def   !hash-map|char->float?      #?(:clj (or (isa? it.unimi.dsi.fastutil.chars.Char2FloatOpenHashMap)          (isa? it.unimi.dsi.fastutil.chars.Char2FloatOpenCustomHashMap))          :cljs none?))
         (-def   !hash-map|char->double?     #?(:clj (or (isa? it.unimi.dsi.fastutil.chars.Char2DoubleOpenHashMap)         (isa? it.unimi.dsi.fastutil.chars.Char2DoubleOpenCustomHashMap))         :cljs none?))

         (-def   !hash-map|short->boolean?   #?(:clj (or (isa? it.unimi.dsi.fastutil.shorts.Short2BooleanOpenHashMap)      (isa? it.unimi.dsi.fastutil.shorts.Short2BooleanOpenCustomHashMap))      :cljs none?))
         (-def   !hash-map|short->byte?      #?(:clj (or (isa? it.unimi.dsi.fastutil.shorts.Short2ByteOpenHashMap)         (isa? it.unimi.dsi.fastutil.shorts.Short2ByteOpenCustomHashMap))         :cljs none?))
         (-def   !hash-map|short->char?      #?(:clj (or (isa? it.unimi.dsi.fastutil.shorts.Short2CharOpenHashMap)         (isa? it.unimi.dsi.fastutil.shorts.Short2CharOpenCustomHashMap))         :cljs none?))
         (-def   !hash-map|short->short?     #?(:clj (or (isa? it.unimi.dsi.fastutil.shorts.Short2ShortOpenHashMap)        (isa? it.unimi.dsi.fastutil.shorts.Short2ShortOpenCustomHashMap))        :cljs none?))
         (-def   !hash-map|short->int?       #?(:clj (or (isa? it.unimi.dsi.fastutil.shorts.Short2IntOpenHashMap)          (isa? it.unimi.dsi.fastutil.shorts.Short2IntOpenCustomHashMap))          :cljs none?))
         (-def   !hash-map|short->long?      #?(:clj (or (isa? it.unimi.dsi.fastutil.shorts.Short2LongOpenHashMap)         (isa? it.unimi.dsi.fastutil.shorts.Short2LongOpenCustomHashMap))         :cljs none?))
         (-def   !hash-map|short->float?     #?(:clj (or (isa? it.unimi.dsi.fastutil.shorts.Short2FloatOpenHashMap)        (isa? it.unimi.dsi.fastutil.shorts.Short2FloatOpenCustomHashMap))        :cljs none?))
         (-def   !hash-map|short->double?    #?(:clj (or (isa? it.unimi.dsi.fastutil.shorts.Short2DoubleOpenHashMap)       (isa? it.unimi.dsi.fastutil.shorts.Short2DoubleOpenCustomHashMap))       :cljs none?))
         (-def   !hash-map|short->ref?       #?(:clj (or (isa? it.unimi.dsi.fastutil.shorts.Short2ReferenceOpenHashMap)    (isa? it.unimi.dsi.fastutil.shorts.Short2ReferenceOpenCustomHashMap))    :cljs none?))

         (-def   !hash-map|int->boolean?     #?(:clj (or (isa? it.unimi.dsi.fastutil.ints.Int2BooleanOpenHashMap)          (isa? it.unimi.dsi.fastutil.ints.Int2BooleanOpenCustomHashMap))          :cljs none?))
         (-def   !hash-map|int->byte?        #?(:clj (or (isa? it.unimi.dsi.fastutil.ints.Int2ByteOpenHashMap)             (isa? it.unimi.dsi.fastutil.ints.Int2ByteOpenCustomHashMap))             :cljs none?))
         (-def   !hash-map|int->char?        #?(:clj (or (isa? it.unimi.dsi.fastutil.ints.Int2CharOpenHashMap)             (isa? it.unimi.dsi.fastutil.ints.Int2CharOpenCustomHashMap))             :cljs none?))
         (-def   !hash-map|int->short?       #?(:clj (or (isa? it.unimi.dsi.fastutil.ints.Int2ShortOpenHashMap)            (isa? it.unimi.dsi.fastutil.ints.Int2ShortOpenCustomHashMap))            :cljs none?))
         (-def   !hash-map|int->int?         #?(:clj (or (isa? it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap)              (isa? it.unimi.dsi.fastutil.ints.Int2IntOpenCustomHashMap))              :cljs none?))
         (-def   !hash-map|int->long?        #?(:clj (or (isa? it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap)             (isa? it.unimi.dsi.fastutil.ints.Int2LongOpenCustomHashMap))             :cljs none?))
         (-def   !hash-map|int->float?       #?(:clj (or (isa? it.unimi.dsi.fastutil.ints.Int2FloatOpenHashMap)            (isa? it.unimi.dsi.fastutil.ints.Int2FloatOpenCustomHashMap))            :cljs none?))
         (-def   !hash-map|int->double?      #?(:clj (or (isa? it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap)           (isa? it.unimi.dsi.fastutil.ints.Int2DoubleOpenCustomHashMap))           :cljs none?))
         (-def   !hash-map|int->ref?         #?(:clj (or (isa? it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap)        (isa? it.unimi.dsi.fastutil.ints.Int2ReferenceOpenCustomHashMap))        :cljs none?))

         (-def   !hash-map|long->boolean?    #?(:clj (or (isa? it.unimi.dsi.fastutil.longs.Long2BooleanOpenHashMap)        (isa? it.unimi.dsi.fastutil.longs.Long2BooleanOpenCustomHashMap))        :cljs none?))
         (-def   !hash-map|long->byte?       #?(:clj (or (isa? it.unimi.dsi.fastutil.longs.Long2ByteOpenCustomHashMap)     (isa? it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap))                 :cljs none?))
         (-def   !hash-map|long->char?       #?(:clj (or (isa? it.unimi.dsi.fastutil.longs.Long2CharOpenHashMap)           (isa? it.unimi.dsi.fastutil.longs.Long2CharOpenCustomHashMap))           :cljs none?))
         (-def   !hash-map|long->short?      #?(:clj (or (isa? it.unimi.dsi.fastutil.longs.Long2ShortOpenHashMap)          (isa? it.unimi.dsi.fastutil.longs.Long2ShortOpenCustomHashMap))          :cljs none?))
         (-def   !hash-map|long->int?        #?(:clj (or (isa? it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap)            (isa? it.unimi.dsi.fastutil.longs.Long2IntOpenCustomHashMap))            :cljs none?))
         (-def   !hash-map|long->long?       #?(:clj (or (isa? it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap)           (isa? it.unimi.dsi.fastutil.longs.Long2LongOpenCustomHashMap))           :cljs none?))
         (-def   !hash-map|long->float?      #?(:clj (or (isa? it.unimi.dsi.fastutil.longs.Long2FloatOpenHashMap)          (isa? it.unimi.dsi.fastutil.longs.Long2FloatOpenCustomHashMap))          :cljs none?))
         (-def   !hash-map|long->double?     #?(:clj (or (isa? it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap)         (isa? it.unimi.dsi.fastutil.longs.Long2DoubleOpenCustomHashMap))         :cljs none?))
         (-def   !hash-map|long->ref?        #?(:clj (or (isa? it.unimi.dsi.fastutil.longs.Long2ReferenceOpenHashMap)      (isa? it.unimi.dsi.fastutil.longs.Long2ReferenceOpenCustomHashMap))      :cljs none?))

         (-def   !hash-map|float->boolean?   #?(:clj (or (isa? it.unimi.dsi.fastutil.floats.Float2BooleanOpenHashMap)      (isa? it.unimi.dsi.fastutil.floats.Float2BooleanOpenCustomHashMap))      :cljs none?))
         (-def   !hash-map|float->byte?      #?(:clj (or (isa? it.unimi.dsi.fastutil.floats.Float2ByteOpenHashMap)         (isa? it.unimi.dsi.fastutil.floats.Float2ByteOpenCustomHashMap))         :cljs none?))
         (-def   !hash-map|float->char?      #?(:clj (or (isa? it.unimi.dsi.fastutil.floats.Float2CharOpenHashMap)         (isa? it.unimi.dsi.fastutil.floats.Float2CharOpenCustomHashMap))         :cljs none?))
         (-def   !hash-map|float->short?     #?(:clj (or (isa? it.unimi.dsi.fastutil.floats.Float2ShortOpenHashMap)        (isa? it.unimi.dsi.fastutil.floats.Float2ShortOpenCustomHashMap))        :cljs none?))
         (-def   !hash-map|float->int?       #?(:clj (or (isa? it.unimi.dsi.fastutil.floats.Float2IntOpenHashMap)          (isa? it.unimi.dsi.fastutil.floats.Float2IntOpenCustomHashMap))          :cljs none?))
         (-def   !hash-map|float->long?      #?(:clj (or (isa? it.unimi.dsi.fastutil.floats.Float2LongOpenHashMap)         (isa? it.unimi.dsi.fastutil.floats.Float2LongOpenCustomHashMap))         :cljs none?))
         (-def   !hash-map|float->float?     #?(:clj (or (isa? it.unimi.dsi.fastutil.floats.Float2FloatOpenHashMap)        (isa? it.unimi.dsi.fastutil.floats.Float2FloatOpenCustomHashMap))        :cljs none?))
         (-def   !hash-map|float->double?    #?(:clj (or (isa? it.unimi.dsi.fastutil.floats.Float2DoubleOpenHashMap)       (isa? it.unimi.dsi.fastutil.floats.Float2DoubleOpenCustomHashMap))       :cljs none?))
         (-def   !hash-map|float->ref?       #?(:clj (or (isa? it.unimi.dsi.fastutil.floats.Float2ReferenceOpenHashMap)    (isa? it.unimi.dsi.fastutil.floats.Float2ReferenceOpenCustomHashMap))    :cljs none?))

         (-def   !hash-map|double->boolean?  #?(:clj (or (isa? it.unimi.dsi.fastutil.doubles.Double2BooleanOpenHashMap)    (isa? it.unimi.dsi.fastutil.doubles.Double2BooleanOpenCustomHashMap))    :cljs none?))
         (-def   !hash-map|double->byte?     #?(:clj (or (isa? it.unimi.dsi.fastutil.doubles.Double2ByteOpenHashMap)       (isa? it.unimi.dsi.fastutil.doubles.Double2ByteOpenCustomHashMap))       :cljs none?))
         (-def   !hash-map|double->char?     #?(:clj (or (isa? it.unimi.dsi.fastutil.doubles.Double2CharOpenHashMap)       (isa? it.unimi.dsi.fastutil.doubles.Double2CharOpenCustomHashMap))       :cljs none?))
         (-def   !hash-map|double->short?    #?(:clj (or (isa? it.unimi.dsi.fastutil.doubles.Double2ShortOpenHashMap)      (isa? it.unimi.dsi.fastutil.doubles.Double2ShortOpenCustomHashMap))      :cljs none?))
         (-def   !hash-map|double->int?      #?(:clj (or (isa? it.unimi.dsi.fastutil.doubles.Double2IntOpenHashMap)        (isa? it.unimi.dsi.fastutil.doubles.Double2IntOpenCustomHashMap))        :cljs none?))
         (-def   !hash-map|double->long?     #?(:clj (or (isa? it.unimi.dsi.fastutil.doubles.Double2LongOpenHashMap)       (isa? it.unimi.dsi.fastutil.doubles.Double2LongOpenCustomHashMap))       :cljs none?))
         (-def   !hash-map|double->float?    #?(:clj (or (isa? it.unimi.dsi.fastutil.doubles.Double2FloatOpenHashMap)      (isa? it.unimi.dsi.fastutil.doubles.Double2FloatOpenCustomHashMap))      :cljs none?))
         (-def   !hash-map|double->double?   #?(:clj (or (isa? it.unimi.dsi.fastutil.doubles.Double2DoubleOpenHashMap)     (isa? it.unimi.dsi.fastutil.doubles.Double2DoubleOpenCustomHashMap))     :cljs none?))
         (-def   !hash-map|double->ref?      #?(:clj (or (isa? it.unimi.dsi.fastutil.doubles.Double2ReferenceOpenHashMap)  (isa? it.unimi.dsi.fastutil.doubles.Double2ReferenceOpenCustomHashMap))  :cljs none?))

         (-def   !hash-map|ref->boolean?     #?(:clj (or (isa? it.unimi.dsi.fastutil.objects.Reference2BooleanOpenHashMap) (isa? it.unimi.dsi.fastutil.objects.Reference2BooleanOpenCustomHashMap)) :cljs none?))
         (-def   !hash-map|ref->byte?        #?(:clj (or (isa? it.unimi.dsi.fastutil.objects.Reference2ByteOpenHashMap)    (isa? it.unimi.dsi.fastutil.objects.Reference2ByteOpenCustomHashMap))    :cljs none?))
         (-def   !hash-map|ref->char?        #?(:clj (or (isa? it.unimi.dsi.fastutil.objects.Reference2CharOpenHashMap)    (isa? it.unimi.dsi.fastutil.objects.Reference2CharOpenCustomHashMap))    :cljs none?))
         (-def   !hash-map|ref->short?       #?(:clj (or (isa? it.unimi.dsi.fastutil.objects.Reference2ShortOpenHashMap)   (isa? it.unimi.dsi.fastutil.objects.Reference2ShortOpenCustomHashMap))   :cljs none?))
         (-def   !hash-map|ref->int?         #?(:clj (or (isa? it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap)     (isa? it.unimi.dsi.fastutil.objects.Reference2IntOpenCustomHashMap))     :cljs none?))
         (-def   !hash-map|ref->long?        #?(:clj (or (isa? it.unimi.dsi.fastutil.objects.Reference2LongOpenHashMap)    (isa? it.unimi.dsi.fastutil.objects.Reference2LongOpenCustomHashMap))    :cljs none?))
         (-def   !hash-map|ref->float?       #?(:clj (or (isa? it.unimi.dsi.fastutil.objects.Reference2FloatOpenHashMap)   (isa? it.unimi.dsi.fastutil.objects.Reference2FloatOpenCustomHashMap))   :cljs none?))
         (-def   !hash-map|ref->double?      #?(:clj (or (isa? it.unimi.dsi.fastutil.objects.Reference2DoubleOpenHashMap)  (isa? it.unimi.dsi.fastutil.objects.Reference2DoubleOpenCustomHashMap))  :cljs none?))

         (-def   !hash-map|ref->ref?         (or #?@(:clj  [(isa? java.util.HashMap)
                                                            ;; Because this has different semantics
                                                            #_(isa? java.util.IdentityHashMap)
                                                            (isa? it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap)
                                                            (isa? it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenCustomHashMap)]
                                                     :cljs [MutableHashMap])))

         (def-preds|map|any                  !hash-map)

         (def-preds|map|same-types           !hash-map)

         (-def   !hash-map?                  !hash-map|any?)

#?(:clj  (-def  !!hash-map?                  (isa? java.util.concurrent.ConcurrentHashMap)))
         (-def    hash-map?                  (or ?!+hash-map? #?(:clj !!hash-map?) !hash-map?))

;; ----- Array Maps ----- ;;

         (-def   +array-map?                  (isa? #?(:clj  clojure.lang.PersistentArrayMap
                                                       :cljs cljs.core/PersistentArrayMap)))

         (-def  !+array-map?                  (isa? #?(:clj  clojure.lang.PersistentArrayMap$TransientArrayMap
                                                       :cljs cljs.core/TransientArrayMap)))

         (-def ?!+array-map?                  (or !+array-map? +array-map?))

         (-def   !array-map|boolean->boolean? none?)
         (-def   !array-map|boolean->byte?    none?)
         (-def   !array-map|boolean->char?    none?)
         (-def   !array-map|boolean->short?   none?)
         (-def   !array-map|boolean->int?     none?)
         (-def   !array-map|boolean->long?    none?)
         (-def   !array-map|boolean->float?   none?)
         (-def   !array-map|boolean->double?  none?)
         (-def   !array-map|boolean->ref?     none?)

         (-def   !array-map|byte->boolean?    #?(:clj (isa? it.unimi.dsi.fastutil.bytes.Byte2BooleanArrayMap)          :cljs none?))
         (-def   !array-map|byte->byte?       #?(:clj (isa? it.unimi.dsi.fastutil.bytes.Byte2ByteArrayMap)             :cljs none?))
         (-def   !array-map|byte->char?       #?(:clj (isa? it.unimi.dsi.fastutil.bytes.Byte2CharArrayMap)             :cljs none?))
         (-def   !array-map|byte->short?      #?(:clj (isa? it.unimi.dsi.fastutil.bytes.Byte2ShortArrayMap)            :cljs none?))
         (-def   !array-map|byte->int?        #?(:clj (isa? it.unimi.dsi.fastutil.bytes.Byte2IntArrayMap)              :cljs none?))
         (-def   !array-map|byte->long?       #?(:clj (isa? it.unimi.dsi.fastutil.bytes.Byte2LongArrayMap)             :cljs none?))
         (-def   !array-map|byte->float?      #?(:clj (isa? it.unimi.dsi.fastutil.bytes.Byte2FloatArrayMap)            :cljs none?))
         (-def   !array-map|byte->double?     #?(:clj (isa? it.unimi.dsi.fastutil.bytes.Byte2DoubleArrayMap)           :cljs none?))
         (-def   !array-map|byte->ref?        #?(:clj (isa? it.unimi.dsi.fastutil.bytes.Byte2ReferenceArrayMap)        :cljs none?))

         (-def   !array-map|char->ref?        #?(:clj (isa? it.unimi.dsi.fastutil.chars.Char2ReferenceArrayMap)        :cljs none?))
         (-def   !array-map|char->boolean?    #?(:clj (isa? it.unimi.dsi.fastutil.chars.Char2BooleanArrayMap)          :cljs none?))
         (-def   !array-map|char->byte?       #?(:clj (isa? it.unimi.dsi.fastutil.chars.Char2ByteArrayMap)             :cljs none?))
         (-def   !array-map|char->char?       #?(:clj (isa? it.unimi.dsi.fastutil.chars.Char2CharArrayMap)             :cljs none?))
         (-def   !array-map|char->short?      #?(:clj (isa? it.unimi.dsi.fastutil.chars.Char2ShortArrayMap)            :cljs none?))
         (-def   !array-map|char->int?        #?(:clj (isa? it.unimi.dsi.fastutil.chars.Char2IntArrayMap)              :cljs none?))
         (-def   !array-map|char->long?       #?(:clj (isa? it.unimi.dsi.fastutil.chars.Char2LongArrayMap)             :cljs none?))
         (-def   !array-map|char->float?      #?(:clj (isa? it.unimi.dsi.fastutil.chars.Char2FloatArrayMap)            :cljs none?))
         (-def   !array-map|char->double?     #?(:clj (isa? it.unimi.dsi.fastutil.chars.Char2DoubleArrayMap)           :cljs none?))

         (-def   !array-map|short->boolean?   #?(:clj (isa? it.unimi.dsi.fastutil.shorts.Short2BooleanArrayMap)        :cljs none?))
         (-def   !array-map|short->byte?      #?(:clj (isa? it.unimi.dsi.fastutil.shorts.Short2ByteArrayMap)           :cljs none?))
         (-def   !array-map|short->char?      #?(:clj (isa? it.unimi.dsi.fastutil.shorts.Short2CharArrayMap)           :cljs none?))
         (-def   !array-map|short->short?     #?(:clj (isa? it.unimi.dsi.fastutil.shorts.Short2ShortArrayMap)          :cljs none?))
         (-def   !array-map|short->int?       #?(:clj (isa? it.unimi.dsi.fastutil.shorts.Short2IntArrayMap)            :cljs none?))
         (-def   !array-map|short->long?      #?(:clj (isa? it.unimi.dsi.fastutil.shorts.Short2LongArrayMap)           :cljs none?))
         (-def   !array-map|short->float?     #?(:clj (isa? it.unimi.dsi.fastutil.shorts.Short2FloatArrayMap)          :cljs none?))
         (-def   !array-map|short->double?    #?(:clj (isa? it.unimi.dsi.fastutil.shorts.Short2DoubleArrayMap)         :cljs none?))
         (-def   !array-map|short->ref?       #?(:clj (isa? it.unimi.dsi.fastutil.shorts.Short2ReferenceArrayMap)      :cljs none?))

         (-def   !array-map|int->boolean?     #?(:clj (isa? it.unimi.dsi.fastutil.ints.Int2BooleanArrayMap)            :cljs none?))
         (-def   !array-map|int->byte?        #?(:clj (isa? it.unimi.dsi.fastutil.ints.Int2ByteArrayMap)               :cljs none?))
         (-def   !array-map|int->char?        #?(:clj (isa? it.unimi.dsi.fastutil.ints.Int2CharArrayMap)               :cljs none?))
         (-def   !array-map|int->short?       #?(:clj (isa? it.unimi.dsi.fastutil.ints.Int2ShortArrayMap)              :cljs none?))
         (-def   !array-map|int->int?         #?(:clj (isa? it.unimi.dsi.fastutil.ints.Int2IntArrayMap)                :cljs none?))
         (-def   !array-map|int->long?        #?(:clj (isa? it.unimi.dsi.fastutil.ints.Int2LongArrayMap)               :cljs none?))
         (-def   !array-map|int->float?       #?(:clj (isa? it.unimi.dsi.fastutil.ints.Int2FloatArrayMap)              :cljs none?))
         (-def   !array-map|int->double?      #?(:clj (isa? it.unimi.dsi.fastutil.ints.Int2DoubleArrayMap)             :cljs none?))
         (-def   !array-map|int->ref?         #?(:clj (isa? it.unimi.dsi.fastutil.ints.Int2ReferenceArrayMap)          :cljs none?))

         (-def   !array-map|long->boolean?    #?(:clj (isa? it.unimi.dsi.fastutil.longs.Long2BooleanArrayMap)          :cljs none?))
         (-def   !array-map|long->byte?       #?(:clj (isa? it.unimi.dsi.fastutil.longs.Long2ByteArrayMap)             :cljs none?))
         (-def   !array-map|long->char?       #?(:clj (isa? it.unimi.dsi.fastutil.longs.Long2CharArrayMap)             :cljs none?))
         (-def   !array-map|long->short?      #?(:clj (isa? it.unimi.dsi.fastutil.longs.Long2ShortArrayMap)            :cljs none?))
         (-def   !array-map|long->int?        #?(:clj (isa? it.unimi.dsi.fastutil.longs.Long2IntArrayMap)              :cljs none?))
         (-def   !array-map|long->long?       #?(:clj (isa? it.unimi.dsi.fastutil.longs.Long2LongArrayMap)             :cljs none?))
         (-def   !array-map|long->float?      #?(:clj (isa? it.unimi.dsi.fastutil.longs.Long2FloatArrayMap)            :cljs none?))
         (-def   !array-map|long->double?     #?(:clj (isa? it.unimi.dsi.fastutil.longs.Long2DoubleArrayMap)           :cljs none?))
         (-def   !array-map|long->ref?        #?(:clj (isa? it.unimi.dsi.fastutil.longs.Long2ReferenceArrayMap)        :cljs none?))

         (-def   !array-map|float->boolean?   #?(:clj (isa? it.unimi.dsi.fastutil.floats.Float2BooleanArrayMap)        :cljs none?))
         (-def   !array-map|float->byte?      #?(:clj (isa? it.unimi.dsi.fastutil.floats.Float2ByteArrayMap)           :cljs none?))
         (-def   !array-map|float->char?      #?(:clj (isa? it.unimi.dsi.fastutil.floats.Float2CharArrayMap)           :cljs none?))
         (-def   !array-map|float->short?     #?(:clj (isa? it.unimi.dsi.fastutil.floats.Float2ShortArrayMap)          :cljs none?))
         (-def   !array-map|float->int?       #?(:clj (isa? it.unimi.dsi.fastutil.floats.Float2IntArrayMap)            :cljs none?))
         (-def   !array-map|float->long?      #?(:clj (isa? it.unimi.dsi.fastutil.floats.Float2LongArrayMap)           :cljs none?))
         (-def   !array-map|float->float?     #?(:clj (isa? it.unimi.dsi.fastutil.floats.Float2FloatArrayMap)          :cljs none?))
         (-def   !array-map|float->double?    #?(:clj (isa? it.unimi.dsi.fastutil.floats.Float2DoubleArrayMap)         :cljs none?))
         (-def   !array-map|float->ref?       #?(:clj (isa? it.unimi.dsi.fastutil.floats.Float2ReferenceArrayMap)      :cljs none?))

         (-def   !array-map|double->boolean?  #?(:clj (isa? it.unimi.dsi.fastutil.doubles.Double2BooleanArrayMap)      :cljs none?))
         (-def   !array-map|double->byte?     #?(:clj (isa? it.unimi.dsi.fastutil.doubles.Double2ByteArrayMap)         :cljs none?))
         (-def   !array-map|double->char?     #?(:clj (isa? it.unimi.dsi.fastutil.doubles.Double2CharArrayMap)         :cljs none?))
         (-def   !array-map|double->short?    #?(:clj (isa? it.unimi.dsi.fastutil.doubles.Double2ShortArrayMap)        :cljs none?))
         (-def   !array-map|double->int?      #?(:clj (isa? it.unimi.dsi.fastutil.doubles.Double2IntArrayMap)          :cljs none?))
         (-def   !array-map|double->long?     #?(:clj (isa? it.unimi.dsi.fastutil.doubles.Double2LongArrayMap)         :cljs none?))
         (-def   !array-map|double->float?    #?(:clj (isa? it.unimi.dsi.fastutil.doubles.Double2FloatArrayMap)        :cljs none?))
         (-def   !array-map|double->double?   #?(:clj (isa? it.unimi.dsi.fastutil.doubles.Double2DoubleArrayMap)       :cljs none?))
         (-def   !array-map|double->ref?      #?(:clj (isa? it.unimi.dsi.fastutil.doubles.Double2ReferenceArrayMap)    :cljs none?))

         (-def   !array-map|ref->boolean?     #?(:clj (isa? it.unimi.dsi.fastutil.objects.Reference2BooleanArrayMap)   :cljs none?))
         (-def   !array-map|ref->byte?        #?(:clj (isa? it.unimi.dsi.fastutil.objects.Reference2ByteArrayMap)      :cljs none?))
         (-def   !array-map|ref->char?        #?(:clj (isa? it.unimi.dsi.fastutil.objects.Reference2CharArrayMap)      :cljs none?))
         (-def   !array-map|ref->short?       #?(:clj (isa? it.unimi.dsi.fastutil.objects.Reference2ShortArrayMap)     :cljs none?))
         (-def   !array-map|ref->int?         #?(:clj (isa? it.unimi.dsi.fastutil.objects.Reference2IntArrayMap)       :cljs none?))
         (-def   !array-map|ref->long?        #?(:clj (isa? it.unimi.dsi.fastutil.objects.Reference2LongArrayMap)      :cljs none?))
         (-def   !array-map|ref->float?       #?(:clj (isa? it.unimi.dsi.fastutil.objects.Reference2FloatArrayMap)     :cljs none?))
         (-def   !array-map|ref->double?      #?(:clj (isa? it.unimi.dsi.fastutil.objects.Reference2DoubleArrayMap)    :cljs none?))
         (-def   !array-map|ref->ref?         #?(:clj (isa? it.unimi.dsi.fastutil.objects.Reference2ReferenceArrayMap) :cljs none?))

         (def-preds|map|any                   !array-map)

         (def-preds|map|same-types            !array-map)

         (-def   !array-map?                  !array-map|any?)

#?(:clj  (-def  !!array-map?                  none?))

         (-def    array-map?                  (or ?!+array-map? #?(:clj !!array-map?) !array-map?))

;; ----- Unsorted Maps ----- ;; TODO Perhaps the concept of unsortedness is `(- map sorted?)`?

         (-def   +unsorted-map?                  (or   +hash-map?   +array-map?))
         (-def  !+unsorted-map?                  (or  !+hash-map?  !+array-map?))
         (-def ?!+unsorted-map?                  (or ?!+hash-map? ?!+array-map?))

         (-def   !unsorted-map|boolean->boolean? (or !hash-map|boolean->boolean? !array-map|boolean->boolean?))
         (-def   !unsorted-map|boolean->byte?    (or !hash-map|boolean->byte?    !array-map|boolean->byte?))
         (-def   !unsorted-map|boolean->char?    (or !hash-map|boolean->char?    !array-map|boolean->char?))
         (-def   !unsorted-map|boolean->short?   (or !hash-map|boolean->short?   !array-map|boolean->short?))
         (-def   !unsorted-map|boolean->int?     (or !hash-map|boolean->int?     !array-map|boolean->int?))
         (-def   !unsorted-map|boolean->long?    (or !hash-map|boolean->long?    !array-map|boolean->long?))
         (-def   !unsorted-map|boolean->float?   (or !hash-map|boolean->float?   !array-map|boolean->float?))
         (-def   !unsorted-map|boolean->double?  (or !hash-map|boolean->double?  !array-map|boolean->double?))
         (-def   !unsorted-map|boolean->ref?     (or !hash-map|boolean->ref?     !array-map|boolean->ref?))

         (-def   !unsorted-map|byte->boolean?    (or !hash-map|byte->boolean?    !array-map|byte->boolean?))
         (-def   !unsorted-map|byte->byte?       (or !hash-map|byte->byte?       !array-map|byte->byte?))
         (-def   !unsorted-map|byte->char?       (or !hash-map|byte->char?       !array-map|byte->char?))
         (-def   !unsorted-map|byte->short?      (or !hash-map|byte->short?      !array-map|byte->short?))
         (-def   !unsorted-map|byte->int?        (or !hash-map|byte->int?        !array-map|byte->int?))
         (-def   !unsorted-map|byte->long?       (or !hash-map|byte->long?       !array-map|byte->long?))
         (-def   !unsorted-map|byte->float?      (or !hash-map|byte->float?      !array-map|byte->float?))
         (-def   !unsorted-map|byte->double?     (or !hash-map|byte->double?     !array-map|byte->double?))
         (-def   !unsorted-map|byte->ref?        (or !hash-map|byte->ref?        !array-map|byte->ref?))

         (-def   !unsorted-map|char->boolean?    (or !hash-map|char->boolean?    !array-map|char->boolean?))
         (-def   !unsorted-map|char->byte?       (or !hash-map|char->byte?       !array-map|char->byte?))
         (-def   !unsorted-map|char->char?       (or !hash-map|char->char?       !array-map|char->char?))
         (-def   !unsorted-map|char->short?      (or !hash-map|char->short?      !array-map|char->short?))
         (-def   !unsorted-map|char->int?        (or !hash-map|char->int?        !array-map|char->int?))
         (-def   !unsorted-map|char->long?       (or !hash-map|char->long?       !array-map|char->long?))
         (-def   !unsorted-map|char->float?      (or !hash-map|char->float?      !array-map|char->float?))
         (-def   !unsorted-map|char->double?     (or !hash-map|char->double?     !array-map|char->double?))
         (-def   !unsorted-map|char->ref?        (or !hash-map|char->ref?        !array-map|char->ref?))

         (-def   !unsorted-map|short->boolean?   (or !hash-map|short->boolean?   !array-map|short->boolean?))
         (-def   !unsorted-map|short->byte?      (or !hash-map|short->byte?      !array-map|short->byte?))
         (-def   !unsorted-map|short->char?      (or !hash-map|short->char?      !array-map|short->char?))
         (-def   !unsorted-map|short->short?     (or !hash-map|short->short?     !array-map|short->short?))
         (-def   !unsorted-map|short->int?       (or !hash-map|short->int?       !array-map|short->int?))
         (-def   !unsorted-map|short->long?      (or !hash-map|short->long?      !array-map|short->long?))
         (-def   !unsorted-map|short->float?     (or !hash-map|short->float?     !array-map|short->float?))
         (-def   !unsorted-map|short->double?    (or !hash-map|short->double?    !array-map|short->double?))
         (-def   !unsorted-map|short->ref?       (or !hash-map|short->ref?       !array-map|short->ref?))

         (-def   !unsorted-map|int->boolean?     (or !hash-map|int->boolean?     !array-map|int->boolean?))
         (-def   !unsorted-map|int->byte?        (or !hash-map|int->byte?        !array-map|int->byte?))
         (-def   !unsorted-map|int->char?        (or !hash-map|int->char?        !array-map|int->char?))
         (-def   !unsorted-map|int->short?       (or !hash-map|int->short?       !array-map|int->short?))
         (-def   !unsorted-map|int->int?         (or !hash-map|int->int?         !array-map|int->int?))
         (-def   !unsorted-map|int->long?        (or !hash-map|int->long?        !array-map|int->long?))
         (-def   !unsorted-map|int->float?       (or !hash-map|int->float?       !array-map|int->float?))
         (-def   !unsorted-map|int->double?      (or !hash-map|int->double?      !array-map|int->double?))
         (-def   !unsorted-map|int->ref?         (or !hash-map|int->ref?         !array-map|int->ref?))

         (-def   !unsorted-map|long->boolean?    (or !hash-map|long->boolean?     !array-map|long->boolean?))
         (-def   !unsorted-map|long->byte?       (or !hash-map|long->byte?        !array-map|long->byte?))
         (-def   !unsorted-map|long->char?       (or !hash-map|long->char?        !array-map|long->char?))
         (-def   !unsorted-map|long->short?      (or !hash-map|long->short?       !array-map|long->short?))
         (-def   !unsorted-map|long->int?        (or !hash-map|long->int?         !array-map|long->int?))
         (-def   !unsorted-map|long->long?       (or !hash-map|long->long?        !array-map|long->long?))
         (-def   !unsorted-map|long->float?      (or !hash-map|long->float?       !array-map|long->float?))
         (-def   !unsorted-map|long->double?     (or !hash-map|long->double?      !array-map|long->double?))
         (-def   !unsorted-map|long->ref?        (or !hash-map|long->ref?         !array-map|long->ref?))

         (-def   !unsorted-map|float->boolean?   (or !hash-map|float->boolean?    !array-map|float->boolean?))
         (-def   !unsorted-map|float->byte?      (or !hash-map|float->byte?       !array-map|float->byte?))
         (-def   !unsorted-map|float->char?      (or !hash-map|float->char?       !array-map|float->char?))
         (-def   !unsorted-map|float->short?     (or !hash-map|float->short?      !array-map|float->short?))
         (-def   !unsorted-map|float->int?       (or !hash-map|float->int?        !array-map|float->int?))
         (-def   !unsorted-map|float->long?      (or !hash-map|float->long?       !array-map|float->long?))
         (-def   !unsorted-map|float->float?     (or !hash-map|float->float?      !array-map|float->float?))
         (-def   !unsorted-map|float->double?    (or !hash-map|float->double?     !array-map|float->double?))
         (-def   !unsorted-map|float->ref?       (or !hash-map|float->ref?        !array-map|float->ref?))

         (-def   !unsorted-map|double->boolean?
           (or !hash-map|double->boolean?   !array-map|double->boolean?))
         (-def   !unsorted-map|double->byte?
           (or !hash-map|double->byte?      !array-map|double->byte?))
         (-def   !unsorted-map|double->char?
           (or !hash-map|double->char?      !array-map|double->char?))
         (-def   !unsorted-map|double->short?
           (or !hash-map|double->short?     !array-map|double->short?))
         (-def   !unsorted-map|double->int?
           (or !hash-map|double->int?       !array-map|double->int?))
         (-def   !unsorted-map|double->long?
           (or !hash-map|double->long?      !array-map|double->long?))
         (-def   !unsorted-map|double->float?
           (or !hash-map|double->float?     !array-map|double->float?))
         (-def   !unsorted-map|double->double?
           (or !hash-map|double->double?    !array-map|double->double?))
         (-def   !unsorted-map|double->ref?
           (or !hash-map|double->ref?       !array-map|double->ref?))

         (-def   !unsorted-map|ref->boolean?
           (or !hash-map|ref->boolean?      !array-map|ref->boolean?))
         (-def   !unsorted-map|ref->byte?
           (or !hash-map|ref->byte?         !array-map|ref->byte?))
         (-def   !unsorted-map|ref->char?
           (or !hash-map|ref->char?         !array-map|ref->char?))
         (-def   !unsorted-map|ref->short?
           (or !hash-map|ref->short?        !array-map|ref->short?))
         (-def   !unsorted-map|ref->int?
           (or !hash-map|ref->int?          !array-map|ref->int?))
         (-def   !unsorted-map|ref->long?
           (or !hash-map|ref->long?         !array-map|ref->long?))
         (-def   !unsorted-map|ref->float?
           (or !hash-map|ref->float?        !array-map|ref->float?))
         (-def   !unsorted-map|ref->double?
           (or !hash-map|ref->double?       !array-map|ref->double?))
         (-def   !unsorted-map|ref->ref?
           (or !identity-map|ref->ref? !hash-map|ref->ref? !array-map|ref->ref?))

         (def-preds|map|any                      !unsorted-map)

         (def-preds|map|same-types               !unsorted-map)

         (-def   !unsorted-map?                  !unsorted-map|any?)

#?(:clj  (-def  !!unsorted-map?                  (or !!hash-map? !!array-map?)))
         (-def    unsorted-map?                  (or ?!+unsorted-map? !unsorted-map? #?(:clj !!unsorted-map?)))

;; ----- Sorted Maps ----- ;;

         (-def   +map?                           (isa? #?(:clj  clojure.lang.IPersistentMap
                                                          :cljs cljs.core/IMap)))
         (-def  !+map?                           (isa? #?(:clj  clojure.lang.ITransientMap
                                                          :cljs cljs.core/ITransientMap)))

         (-def   +sorted-map?                    (and (isa? #?(:clj clojure.lang.Sorted :cljs cljs.core/ISorted))
                                                      +map?))
         (-def  !+sorted-map?                    (and (isa? #?(:clj clojure.lang.Sorted :cljs cljs.core/ISorted))
                                                      !+map?))
         (-def ?!+sorted-map?                    none? #_(or +sorted-map? !+sorted-map?)) ; TODO re-enable when `or` implemented properly

         (-def   !sorted-map|boolean->boolean?   none?)
         (-def   !sorted-map|boolean->byte?      none?)
         (-def   !sorted-map|boolean->char?      none?)
         (-def   !sorted-map|boolean->short?     none?)
         (-def   !sorted-map|boolean->int?       none?)
         (-def   !sorted-map|boolean->long?      none?)
         (-def   !sorted-map|boolean->float?     none?)
         (-def   !sorted-map|boolean->double?    none?)
         (-def   !sorted-map|boolean->ref?       none?)

         (-def   !sorted-map|byte->boolean?
           #?(:clj (isa? it.unimi.dsi.fastutil.bytes.Byte2BooleanSortedMap)          :cljs none?))
         (-def   !sorted-map|byte->byte?
           #?(:clj (isa? it.unimi.dsi.fastutil.bytes.Byte2ByteSortedMap)             :cljs none?))
         (-def   !sorted-map|byte->char?
           #?(:clj (isa? it.unimi.dsi.fastutil.bytes.Byte2CharSortedMap)             :cljs none?))
         (-def   !sorted-map|byte->short?
           #?(:clj (isa? it.unimi.dsi.fastutil.bytes.Byte2ShortSortedMap)            :cljs none?))
         (-def   !sorted-map|byte->int?
           #?(:clj (isa? it.unimi.dsi.fastutil.bytes.Byte2IntSortedMap)              :cljs none?))
         (-def   !sorted-map|byte->long?
           #?(:clj (isa? it.unimi.dsi.fastutil.bytes.Byte2LongSortedMap)             :cljs none?))
         (-def   !sorted-map|byte->float?
           #?(:clj (isa? it.unimi.dsi.fastutil.bytes.Byte2FloatSortedMap)            :cljs none?))
         (-def   !sorted-map|byte->double?
           #?(:clj (isa? it.unimi.dsi.fastutil.bytes.Byte2DoubleSortedMap)           :cljs none?))
         (-def   !sorted-map|byte->ref?
           #?(:clj (isa? it.unimi.dsi.fastutil.bytes.Byte2ReferenceSortedMap)        :cljs none?))

         (-def   !sorted-map|char->ref?
           #?(:clj (isa? it.unimi.dsi.fastutil.chars.Char2ReferenceSortedMap)        :cljs none?))
         (-def   !sorted-map|char->boolean?
           #?(:clj (isa? it.unimi.dsi.fastutil.chars.Char2BooleanSortedMap)          :cljs none?))
         (-def   !sorted-map|char->byte?
           #?(:clj (isa? it.unimi.dsi.fastutil.chars.Char2ByteSortedMap)             :cljs none?))
         (-def   !sorted-map|char->char?
           #?(:clj (isa? it.unimi.dsi.fastutil.chars.Char2CharSortedMap)             :cljs none?))
         (-def   !sorted-map|char->short?
           #?(:clj (isa? it.unimi.dsi.fastutil.chars.Char2ShortSortedMap)            :cljs none?))
         (-def   !sorted-map|char->int?
           #?(:clj (isa? it.unimi.dsi.fastutil.chars.Char2IntSortedMap)              :cljs none?))
         (-def   !sorted-map|char->long?
           #?(:clj (isa? it.unimi.dsi.fastutil.chars.Char2LongSortedMap)             :cljs none?))
         (-def   !sorted-map|char->float?
           #?(:clj (isa? it.unimi.dsi.fastutil.chars.Char2FloatSortedMap)            :cljs none?))
         (-def   !sorted-map|char->double?
           #?(:clj (isa? it.unimi.dsi.fastutil.chars.Char2DoubleSortedMap)           :cljs none?))

         (-def   !sorted-map|short->boolean?
           #?(:clj (isa? it.unimi.dsi.fastutil.shorts.Short2BooleanSortedMap)        :cljs none?))
         (-def   !sorted-map|short->byte?
           #?(:clj (isa? it.unimi.dsi.fastutil.shorts.Short2ByteSortedMap)           :cljs none?))
         (-def   !sorted-map|short->char?
           #?(:clj (isa? it.unimi.dsi.fastutil.shorts.Short2CharSortedMap)           :cljs none?))
         (-def   !sorted-map|short->short?
           #?(:clj (isa? it.unimi.dsi.fastutil.shorts.Short2ShortSortedMap)          :cljs none?))
         (-def   !sorted-map|short->int?
           #?(:clj (isa? it.unimi.dsi.fastutil.shorts.Short2IntSortedMap)            :cljs none?))
         (-def   !sorted-map|short->long?
           #?(:clj (isa? it.unimi.dsi.fastutil.shorts.Short2LongSortedMap)           :cljs none?))
         (-def   !sorted-map|short->float?
           #?(:clj (isa? it.unimi.dsi.fastutil.shorts.Short2FloatSortedMap)          :cljs none?))
         (-def   !sorted-map|short->double?
           #?(:clj (isa? it.unimi.dsi.fastutil.shorts.Short2DoubleSortedMap)         :cljs none?))
         (-def   !sorted-map|short->ref?
           #?(:clj (isa? it.unimi.dsi.fastutil.shorts.Short2ReferenceSortedMap)      :cljs none?))

         (-def   !sorted-map|int->boolean?       #?(:clj (isa? it.unimi.dsi.fastutil.ints.Int2BooleanSortedMap)            :cljs none?))
         (-def   !sorted-map|int->byte?          #?(:clj (isa? it.unimi.dsi.fastutil.ints.Int2ByteSortedMap)               :cljs none?))
         (-def   !sorted-map|int->char?          #?(:clj (isa? it.unimi.dsi.fastutil.ints.Int2CharSortedMap)               :cljs none?))
         (-def   !sorted-map|int->short?         #?(:clj (isa? it.unimi.dsi.fastutil.ints.Int2ShortSortedMap)              :cljs none?))
         (-def   !sorted-map|int->int?           #?(:clj (isa? it.unimi.dsi.fastutil.ints.Int2IntSortedMap)                :cljs none?))
         (-def   !sorted-map|int->long?          #?(:clj (isa? it.unimi.dsi.fastutil.ints.Int2LongSortedMap)               :cljs none?))
         (-def   !sorted-map|int->float?         #?(:clj (isa? it.unimi.dsi.fastutil.ints.Int2FloatSortedMap)              :cljs none?))
         (-def   !sorted-map|int->double?        #?(:clj (isa? it.unimi.dsi.fastutil.ints.Int2DoubleSortedMap)             :cljs none?))
         (-def   !sorted-map|int->ref?           #?(:clj (isa? it.unimi.dsi.fastutil.ints.Int2ReferenceSortedMap)          :cljs none?))

         (-def   !sorted-map|long->boolean?      #?(:clj (isa? it.unimi.dsi.fastutil.longs.Long2BooleanSortedMap)          :cljs none?))
         (-def   !sorted-map|long->byte?         #?(:clj (isa? it.unimi.dsi.fastutil.longs.Long2ByteSortedMap)             :cljs none?))
         (-def   !sorted-map|long->char?         #?(:clj (isa? it.unimi.dsi.fastutil.longs.Long2CharSortedMap)             :cljs none?))
         (-def   !sorted-map|long->short?        #?(:clj (isa? it.unimi.dsi.fastutil.longs.Long2ShortSortedMap)            :cljs none?))
         (-def   !sorted-map|long->int?          #?(:clj (isa? it.unimi.dsi.fastutil.longs.Long2IntSortedMap)              :cljs none?))
         (-def   !sorted-map|long->long?         #?(:clj (isa? it.unimi.dsi.fastutil.longs.Long2LongSortedMap)             :cljs none?))
         (-def   !sorted-map|long->float?        #?(:clj (isa? it.unimi.dsi.fastutil.longs.Long2FloatSortedMap)            :cljs none?))
         (-def   !sorted-map|long->double?       #?(:clj (isa? it.unimi.dsi.fastutil.longs.Long2DoubleSortedMap)           :cljs none?))
         (-def   !sorted-map|long->ref?          #?(:clj (isa? it.unimi.dsi.fastutil.longs.Long2ReferenceSortedMap)        :cljs none?))

         (-def   !sorted-map|float->boolean?     #?(:clj (isa? it.unimi.dsi.fastutil.floats.Float2BooleanSortedMap)        :cljs none?))
         (-def   !sorted-map|float->byte?        #?(:clj (isa? it.unimi.dsi.fastutil.floats.Float2ByteSortedMap)           :cljs none?))
         (-def   !sorted-map|float->char?        #?(:clj (isa? it.unimi.dsi.fastutil.floats.Float2CharSortedMap)           :cljs none?))
         (-def   !sorted-map|float->short?       #?(:clj (isa? it.unimi.dsi.fastutil.floats.Float2ShortSortedMap)          :cljs none?))
         (-def   !sorted-map|float->int?         #?(:clj (isa? it.unimi.dsi.fastutil.floats.Float2IntSortedMap)            :cljs none?))
         (-def   !sorted-map|float->long?        #?(:clj (isa? it.unimi.dsi.fastutil.floats.Float2LongSortedMap)           :cljs none?))
         (-def   !sorted-map|float->float?       #?(:clj (isa? it.unimi.dsi.fastutil.floats.Float2FloatSortedMap)          :cljs none?))
         (-def   !sorted-map|float->double?      #?(:clj (isa? it.unimi.dsi.fastutil.floats.Float2DoubleSortedMap)         :cljs none?))
         (-def   !sorted-map|float->ref?         #?(:clj (isa? it.unimi.dsi.fastutil.floats.Float2ReferenceSortedMap)      :cljs none?))

         (-def   !sorted-map|double->boolean?    #?(:clj (isa? it.unimi.dsi.fastutil.doubles.Double2BooleanSortedMap)      :cljs none?))
         (-def   !sorted-map|double->byte?       #?(:clj (isa? it.unimi.dsi.fastutil.doubles.Double2ByteSortedMap)         :cljs none?))
         (-def   !sorted-map|double->char?       #?(:clj (isa? it.unimi.dsi.fastutil.doubles.Double2CharSortedMap)         :cljs none?))
         (-def   !sorted-map|double->short?      #?(:clj (isa? it.unimi.dsi.fastutil.doubles.Double2ShortSortedMap)        :cljs none?))
         (-def   !sorted-map|double->int?        #?(:clj (isa? it.unimi.dsi.fastutil.doubles.Double2IntSortedMap)          :cljs none?))
         (-def   !sorted-map|double->long?       #?(:clj (isa? it.unimi.dsi.fastutil.doubles.Double2LongSortedMap)         :cljs none?))
         (-def   !sorted-map|double->float?      #?(:clj (isa? it.unimi.dsi.fastutil.doubles.Double2FloatSortedMap)        :cljs none?))
         (-def   !sorted-map|double->double?     #?(:clj (isa? it.unimi.dsi.fastutil.doubles.Double2DoubleSortedMap)       :cljs none?))
         (-def   !sorted-map|double->ref?        #?(:clj (isa? it.unimi.dsi.fastutil.doubles.Double2ReferenceSortedMap)    :cljs none?))

         (-def   !sorted-map|ref->boolean?       #?(:clj (isa? it.unimi.dsi.fastutil.objects.Reference2BooleanSortedMap)   :cljs none?))
         (-def   !sorted-map|ref->byte?          #?(:clj (isa? it.unimi.dsi.fastutil.objects.Reference2ByteSortedMap)      :cljs none?))
         (-def   !sorted-map|ref->char?          #?(:clj (isa? it.unimi.dsi.fastutil.objects.Reference2CharSortedMap)      :cljs none?))
         (-def   !sorted-map|ref->short?         #?(:clj (isa? it.unimi.dsi.fastutil.objects.Reference2ShortSortedMap)     :cljs none?))
         (-def   !sorted-map|ref->int?           #?(:clj (isa? it.unimi.dsi.fastutil.objects.Reference2IntSortedMap)       :cljs none?))
         (-def   !sorted-map|ref->long?          #?(:clj (isa? it.unimi.dsi.fastutil.objects.Reference2LongSortedMap)      :cljs none?))
         (-def   !sorted-map|ref->float?         #?(:clj (isa? it.unimi.dsi.fastutil.objects.Reference2FloatSortedMap)     :cljs none?))
         (-def   !sorted-map|ref->double?        #?(:clj (isa? it.unimi.dsi.fastutil.objects.Reference2DoubleSortedMap)    :cljs none?))

         (-def   !sorted-map|ref->ref?           (or #?@(:clj  [(isa? java.util.TreeMap)
                                                                (isa? it.unimi.dsi.fastutil.objects.Reference2ReferenceSortedMap)]
                                                         :cljs [(isa? goog.structs.AvlTree)])))

         (def-preds|map|any                      !sorted-map)

         (def-preds|map|same-types               !sorted-map)

         (-def   !sorted-map?                    !sorted-map|any?)

#?(:clj  (-def  !!sorted-map?                    (isa? java.util.concurrent.ConcurrentNavigableMap)))
         (-def    sorted-map?                    (or ?!+sorted-map? #?@(:clj [!!sorted-map? (isa? java.util.SortedMap)]) !sorted-map?))

;; ----- Other Maps ----- ;;

         (-def   +insertion-ordered-map? (or (isa? linked.map.LinkedMap)
                                             ;; This is true, but we have replaced OrderedMap with LinkedMap
                                             #_(:clj (isa? flatland.ordered.map.OrderedMap))))
         (-def  !+insertion-ordered-map? none?
                                         ;; This is true, but we have replaced OrderedMap with LinkedMap
                                         #_(isa? flatland.ordered.map.TransientOrderedMap))
         (-def ?!+insertion-ordered-map? (or +insertion-ordered-map? !+insertion-ordered-map?))

         (-def   !insertion-ordered-map? #?(:clj (isa? java.util.LinkedHashMap) :cljs none?))

         ;; See https://github.com/ben-manes/concurrentlinkedhashmap (and links therefrom) for good implementation
#?(:clj  (-def  !!insertion-ordered-map? none?))

         (-def    insertion-ordered-map? (or ?!+insertion-ordered-map? !insertion-ordered-map? #?(:clj !!insertion-ordered-map?)))

;; ----- General Maps ----- ;;

         (-def   +map|built-in?         (or (isa? #?(:clj clojure.lang.PersistentHashMap  :cljs cljs.core/PersistentHashMap))
                                            (isa? #?(:clj clojure.lang.PersistentArrayMap :cljs cljs.core/PersistentArrayMap))
                                            (isa? #?(:clj clojure.lang.PersistentTreeMap  :cljs cljs.core/PersistentTreeMap))))

         ;; `+map?` and `!+map?` defined above
         (-def ?!+map?                  (or !+map? +map?))

         (-def   !map|boolean->boolean? none?)
         (-def   !map|boolean->byte?    none?)
         (-def   !map|boolean->char?    none?)
         (-def   !map|boolean->short?   none?)
         (-def   !map|boolean->int?     none?)
         (-def   !map|boolean->long?    none?)
         (-def   !map|boolean->float?   none?)
         (-def   !map|boolean->double?  none?)
         (-def   !map|boolean->ref?     none?)

         (-def   !map|byte->boolean?    #?(:clj (isa? it.unimi.dsi.fastutil.bytes.Byte2BooleanMap)        :cljs none?))
         (-def   !map|byte->byte?       #?(:clj (isa? it.unimi.dsi.fastutil.bytes.Byte2ByteMap)           :cljs none?))
         (-def   !map|byte->char?       #?(:clj (isa? it.unimi.dsi.fastutil.bytes.Byte2CharMap)           :cljs none?))
         (-def   !map|byte->short?      #?(:clj (isa? it.unimi.dsi.fastutil.bytes.Byte2ShortMap)          :cljs none?))
         (-def   !map|byte->int?        #?(:clj (isa? it.unimi.dsi.fastutil.bytes.Byte2IntMap)            :cljs none?))
         (-def   !map|byte->long?       #?(:clj (isa? it.unimi.dsi.fastutil.bytes.Byte2LongMap)           :cljs none?))
         (-def   !map|byte->float?      #?(:clj (isa? it.unimi.dsi.fastutil.bytes.Byte2FloatMap)          :cljs none?))
         (-def   !map|byte->double?     #?(:clj (isa? it.unimi.dsi.fastutil.bytes.Byte2DoubleMap)         :cljs none?))
         (-def   !map|byte->ref?        #?(:clj (isa? it.unimi.dsi.fastutil.bytes.Byte2ReferenceMap)      :cljs none?))

         (-def   !map|char->ref?        #?(:clj (isa? it.unimi.dsi.fastutil.chars.Char2ReferenceMap)      :cljs none?))
         (-def   !map|char->boolean?    #?(:clj (isa? it.unimi.dsi.fastutil.chars.Char2BooleanMap)        :cljs none?))
         (-def   !map|char->byte?       #?(:clj (isa? it.unimi.dsi.fastutil.chars.Char2ByteMap)           :cljs none?))
         (-def   !map|char->char?       #?(:clj (isa? it.unimi.dsi.fastutil.chars.Char2CharMap)           :cljs none?))
         (-def   !map|char->short?      #?(:clj (isa? it.unimi.dsi.fastutil.chars.Char2ShortMap)          :cljs none?))
         (-def   !map|char->int?        #?(:clj (isa? it.unimi.dsi.fastutil.chars.Char2IntMap)            :cljs none?))
         (-def   !map|char->long?       #?(:clj (isa? it.unimi.dsi.fastutil.chars.Char2LongMap)           :cljs none?))
         (-def   !map|char->float?      #?(:clj (isa? it.unimi.dsi.fastutil.chars.Char2FloatMap)          :cljs none?))
         (-def   !map|char->double?     #?(:clj (isa? it.unimi.dsi.fastutil.chars.Char2DoubleMap)         :cljs none?))

         (-def   !map|short->boolean?   #?(:clj (isa? it.unimi.dsi.fastutil.shorts.Short2BooleanMap)      :cljs none?))
         (-def   !map|short->byte?      #?(:clj (isa? it.unimi.dsi.fastutil.shorts.Short2ByteMap)         :cljs none?))
         (-def   !map|short->char?      #?(:clj (isa? it.unimi.dsi.fastutil.shorts.Short2CharMap)         :cljs none?))
         (-def   !map|short->short?     #?(:clj (isa? it.unimi.dsi.fastutil.shorts.Short2ShortMap)        :cljs none?))
         (-def   !map|short->int?       #?(:clj (isa? it.unimi.dsi.fastutil.shorts.Short2IntMap)          :cljs none?))
         (-def   !map|short->long?      #?(:clj (isa? it.unimi.dsi.fastutil.shorts.Short2LongMap)         :cljs none?))
         (-def   !map|short->float?     #?(:clj (isa? it.unimi.dsi.fastutil.shorts.Short2FloatMap)        :cljs none?))
         (-def   !map|short->double?    #?(:clj (isa? it.unimi.dsi.fastutil.shorts.Short2DoubleMap)       :cljs none?))
         (-def   !map|short->ref?       #?(:clj (isa? it.unimi.dsi.fastutil.shorts.Short2ReferenceMap)    :cljs none?))

         (-def   !map|int->boolean?     #?(:clj (isa? it.unimi.dsi.fastutil.ints.Int2BooleanMap)          :cljs none?))
         (-def   !map|int->byte?        #?(:clj (isa? it.unimi.dsi.fastutil.ints.Int2ByteMap)             :cljs none?))
         (-def   !map|int->char?        #?(:clj (isa? it.unimi.dsi.fastutil.ints.Int2CharMap)             :cljs none?))
         (-def   !map|int->short?       #?(:clj (isa? it.unimi.dsi.fastutil.ints.Int2ShortMap)            :cljs none?))
         (-def   !map|int->int?         #?(:clj (isa? it.unimi.dsi.fastutil.ints.Int2IntMap)              :cljs none?))
         (-def   !map|int->long?        #?(:clj (isa? it.unimi.dsi.fastutil.ints.Int2LongMap)             :cljs none?))
         (-def   !map|int->float?       #?(:clj (isa? it.unimi.dsi.fastutil.ints.Int2FloatMap)            :cljs none?))
         (-def   !map|int->double?      #?(:clj (isa? it.unimi.dsi.fastutil.ints.Int2DoubleMap)           :cljs none?))
         (-def   !map|int->ref?         #?(:clj (isa? it.unimi.dsi.fastutil.ints.Int2ReferenceMap)        :cljs none?))

         (-def   !map|long->boolean?    #?(:clj (isa? it.unimi.dsi.fastutil.longs.Long2BooleanMap)        :cljs none?))
         (-def   !map|long->byte?       #?(:clj (isa? it.unimi.dsi.fastutil.longs.Long2ByteMap)           :cljs none?))
         (-def   !map|long->char?       #?(:clj (isa? it.unimi.dsi.fastutil.longs.Long2CharMap)           :cljs none?))
         (-def   !map|long->short?      #?(:clj (isa? it.unimi.dsi.fastutil.longs.Long2ShortMap)          :cljs none?))
         (-def   !map|long->int?        #?(:clj (isa? it.unimi.dsi.fastutil.longs.Long2IntMap)            :cljs none?))
         (-def   !map|long->long?       #?(:clj (isa? it.unimi.dsi.fastutil.longs.Long2LongMap)           :cljs none?))
         (-def   !map|long->float?      #?(:clj (isa? it.unimi.dsi.fastutil.longs.Long2FloatMap)          :cljs none?))
         (-def   !map|long->double?     #?(:clj (isa? it.unimi.dsi.fastutil.longs.Long2DoubleMap)         :cljs none?))
         (-def   !map|long->ref?        #?(:clj (isa? it.unimi.dsi.fastutil.longs.Long2ReferenceMap)      :cljs none?))

         (-def   !map|float->boolean?   #?(:clj (isa? it.unimi.dsi.fastutil.floats.Float2BooleanMap)      :cljs none?))
         (-def   !map|float->byte?      #?(:clj (isa? it.unimi.dsi.fastutil.floats.Float2ByteMap)         :cljs none?))
         (-def   !map|float->char?      #?(:clj (isa? it.unimi.dsi.fastutil.floats.Float2CharMap)         :cljs none?))
         (-def   !map|float->short?     #?(:clj (isa? it.unimi.dsi.fastutil.floats.Float2ShortMap)        :cljs none?))
         (-def   !map|float->int?       #?(:clj (isa? it.unimi.dsi.fastutil.floats.Float2IntMap)          :cljs none?))
         (-def   !map|float->long?      #?(:clj (isa? it.unimi.dsi.fastutil.floats.Float2LongMap)         :cljs none?))
         (-def   !map|float->float?     #?(:clj (isa? it.unimi.dsi.fastutil.floats.Float2FloatMap)        :cljs none?))
         (-def   !map|float->double?    #?(:clj (isa? it.unimi.dsi.fastutil.floats.Float2DoubleMap)       :cljs none?))
         (-def   !map|float->ref?       #?(:clj (isa? it.unimi.dsi.fastutil.floats.Float2ReferenceMap)    :cljs none?))

         (-def   !map|double->boolean?  #?(:clj (isa? it.unimi.dsi.fastutil.doubles.Double2BooleanMap)    :cljs none?))
         (-def   !map|double->byte?     #?(:clj (isa? it.unimi.dsi.fastutil.doubles.Double2ByteMap)       :cljs none?))
         (-def   !map|double->char?     #?(:clj (isa? it.unimi.dsi.fastutil.doubles.Double2CharMap)       :cljs none?))
         (-def   !map|double->short?    #?(:clj (isa? it.unimi.dsi.fastutil.doubles.Double2ShortMap)      :cljs none?))
         (-def   !map|double->int?      #?(:clj (isa? it.unimi.dsi.fastutil.doubles.Double2IntMap)        :cljs none?))
         (-def   !map|double->long?     #?(:clj (isa? it.unimi.dsi.fastutil.doubles.Double2LongMap)       :cljs none?))
         (-def   !map|double->float?    #?(:clj (isa? it.unimi.dsi.fastutil.doubles.Double2FloatMap)      :cljs none?))
         (-def   !map|double->double?   #?(:clj (isa? it.unimi.dsi.fastutil.doubles.Double2DoubleMap)     :cljs none?))
         (-def   !map|double->ref?      #?(:clj (isa? it.unimi.dsi.fastutil.doubles.Double2ReferenceMap)  :cljs none?))

         (-def   !map|ref->boolean?     #?(:clj (isa? it.unimi.dsi.fastutil.objects.Reference2BooleanMap) :cljs none?))
         (-def   !map|ref->byte?        #?(:clj (isa? it.unimi.dsi.fastutil.objects.Reference2ByteMap)    :cljs none?))
         (-def   !map|ref->char?        #?(:clj (isa? it.unimi.dsi.fastutil.objects.Reference2CharMap)    :cljs none?))
         (-def   !map|ref->short?       #?(:clj (isa? it.unimi.dsi.fastutil.objects.Reference2ShortMap)   :cljs none?))
         (-def   !map|ref->int?         #?(:clj (isa? it.unimi.dsi.fastutil.objects.Reference2IntMap)     :cljs none?))
         (-def   !map|ref->long?        #?(:clj (isa? it.unimi.dsi.fastutil.objects.Reference2LongMap)    :cljs none?))
         (-def   !map|ref->float?       #?(:clj (isa? it.unimi.dsi.fastutil.objects.Reference2FloatMap)   :cljs none?))
         (-def   !map|ref->double?      #?(:clj (isa? it.unimi.dsi.fastutil.objects.Reference2DoubleMap)  :cljs none?))

         (-def   !map|ref->ref?         (or #?@(:clj  [;; perhaps just `(- !map? <primitive-possibilities>)` ?
                                                       !unsorted-map|ref->ref?
                                                       !sorted-map|ref->ref?
                                                       (isa? it.unimi.dsi.fastutil.objects.Reference2ReferenceMap)]
                                                :cljs [(isa? goog.structs.AvlTree)])))

         (def-preds|map|any             !map)

         (def-preds|map|same-types      !map)

         (-def   !map?                  !map|any?)

#?(:clj  (-def  !!map?                  (or !!unsorted-map? !!sorted-map?)))

         (-def    map?                  (or ?!+map? !map? #?@(:clj [!!map? (isa? java.util.Map)])))

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

         ;; I.e., can you call/invoke it by being in functor position (first element of an unquoted list)
         ;; within a typed context?
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

         ;; `js/File` isn't always available! Use an abstraction
#?(:clj  (-def file?         (isa? java.io.File)))

         (-def comparable?   #?(:clj  (isa? java.lang.Comparable)
                                ;; TODO other things are comparable; really it depends on the two objects in question
                                :cljs (or nil? (isa? cljs.core/IComparable))))

         (-def record?       (isa? #?(:clj clojure.lang.IRecord :cljs cljs.core/IRecord)))

         (-def transformer?  (isa? quantum.untyped.core.reducers.Transformer))

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
