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
            true? false? keyword? symbol?
            array? associative? coll? counted? indexed? iterable? list? map? map-entry? record?
            seq? seqable? sequential? set? sorted? vector?
            fn? ifn?
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
           [quantum.untyped.core.identifiers
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
             :refer [def-preds|map|any def-preds|map|same-types]]))
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
  ([] empty-set)
  ([arg & args]
    (create-logical-type :or ->OrType utr/or-type? utr/or-type>args
      (cons arg args) (fn1 c/= >ident))))

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

;; ----- If ----- ;;

;; This won't shadow anything because `if` and `def` are non-shadowable
(defns if
  "(if a b c)
   : (a->b) & (~a->c)
   : (~a | b) & (a | c)
   : (a & b) | (~a & c)"
  [pred utr/type?, then utr/type?, else utr/type? > utr/type?]
  (or (and pred then) (and (not pred) else)))

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

(def type?          (isa? PType))
(def not-type?      (isa? NotType))
(def or-type?       (isa? OrType))
(def and-type?      (isa? AndType))
(def protocol-type? (isa? ProtocolType))
(def class-type?    (isa? ClassType))
(def value-type?    (isa? ValueType))

;; For use in logical operators
(def nil?           (value nil))
(def object?        (isa? #?(:clj java.lang.Object :cljs js/Object)))

;; ===== Type metadata ===== ;;

(defns assume
  "Denotes that, whatever the declared output type (to which `assume` is applied) of a function may
   be, it is assumed that the output satisfies that type."
  [t utr/type? > utr/type?] (update-meta t assoc :quantum.core.type/assume? true))

(defns *
  "Denote on a type that it must be enforced at runtime.
   For use with `defnt`."
  [t utr/type? > utr/type?] (update-meta t assoc :quantum.core.type/runtime? true))

(defns ref
  "Denote on a type that it must not be expanded to use primitive values.
   For use with `defnt`."
  [t utr/type? > utr/type?] (update-meta t assoc :quantum.core.type/ref? true))

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

;; TODO figure out the best place to put this
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

;; TODO figure out the best place to put this
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

;; ===== General ===== ;;

         (def none?         empty-set)
         (def any?          universal-set)

                            ;; TODO this is incomplete for CLJS base classes
                            ;; TODO is this necessary?
         (def val|by-class? (or object? #?@(:cljs [(isa? js/String) (isa? js/Symbol)])))
         (def val?          (not nil?))

         (def ref?          (ref any?))

;; ===== Meta ===== ;;

         ;; TODO probably move, but this is used by `quantum.untyped.core.type` etc.
#?(:clj  (def primitive-class? (or (value Boolean/TYPE)
                                   (value Byte/TYPE)
                                   (value Character/TYPE)
                                   (value Short/TYPE)
                                   (value Integer/TYPE)
                                   (value Long/TYPE)
                                   (value Float/TYPE)
                                   (value Double/TYPE))))

;; ===== Primitives ===== ;;
;; NOTE these are kept here because they're used in both type analysis and various test namespaces

         (def  boolean? (isa? #?(:clj Boolean :cljs js/Boolean)))
#?(:clj  (def  byte?    (isa? Byte)))
#?(:clj  (def  char?    (isa? Character)))
#?(:clj  (def  short?   (isa? Short)))
#?(:clj  (def  int?     (isa? Integer))) ; only primitive int, not goog.math.Integer
#?(:clj  (def  long?    (isa? Long))) ; only primitive long, not goog.math.Long
#?(:clj  (def  float?   (isa? Float)))
         (def  double?  (isa? #?(:clj Double :cljs js/Number)))

         ;; These are special for CLJS protocols
         ;; Possibly planned to be used by `quantum.untyped.core.analyze`
#?(:cljs (def  native?  (or (isa? js/Boolean)
                            (isa? js/Number)
                            (isa? js/Object)
                            (isa? js/Array)
                            (isa? js/String)
                            (isa? js/Function)
                            nil?)))

;; ===== Booleans ===== ;;

;; Used by `quantum.untyped.core.analyze`
(def true?  (value true))
(def false? (value false))

;; ========== Collections ========== ;;

;; Possibly planned to be used by `quantum.untyped.core.analyze`
(def +list|built-in?
  (or (isa? #?(:clj clojure.lang.PersistentList$EmptyList :cljs cljs.core/EmptyList))
      (isa? #?(:clj clojure.lang.PersistentList           :cljs cljs.core/List))))

;; Used by `quantum.untyped.core.analyze`
(def +vector|built-in? (t/isa? #?(:clj  clojure.lang.PersistentVector
                                  :cljs cljs.core/PersistentVector)))

;; Used by `quantum.untyped.core.analyze`
(def +map|built-in?
     (or (isa? #?(:clj clojure.lang.PersistentHashMap  :cljs cljs.core/PersistentHashMap))
         (isa? #?(:clj clojure.lang.PersistentArrayMap :cljs cljs.core/PersistentArrayMap))
         (isa? #?(:clj clojure.lang.PersistentTreeMap  :cljs cljs.core/PersistentTreeMap))))

;; Used by `quantum.untyped.core.analyze`
(def +set|built-in?
  (or (isa? #?(:clj clojure.lang.PersistentHashSet :cljs cljs.core/PersistentHashSet))
      (isa? #?(:clj clojure.lang.PersistentTreeSet :cljs cljs.core/PersistentTreeSet))))

;; ===== Functions ===== ;;

;; Used by `quantum.untyped.core.analyze`
(def fn?  (isa? #?(:clj clojure.lang.Fn  :cljs js/Function)))

;; Used by `quantum.untyped.core.analyze` via `t/callable?`
(def ifn? (isa? #?(:clj clojure.lang.IFn :cljs cljs.core/IFn)))

;; Used by `quantum.untyped.core.analyze` via `t/callable?`
(def fnt? (and fn? (>expr (fn-> c/meta ::type))))

;; TODO should we allow java.lang.Runnable, java.util.concurrent.Callable, and other
;; functional interfaces to be `callable?`?
;; Used by `quantum.untyped.core.analyze`
(uvar/def callable?
  "The set of all objects that are able to called/invoked by being in functor position
   (first element of an unquoted list) within a typed context."
  (or ifn? fnt?))

;; ===== Metadata ===== ;;

;; Used by `quantum.untyped.core.analyze.ast`
(def with-metable? (isa? #?(:clj clojure.lang.IObj :cljs cljs.core/IWithMeta)))

;; ===== Errors ===== ;;

;; Used by `quantum.untyped.core.analyze`
(def throwable? "Able to be used with `throw`" #?(:clj (isa? java.lang.Throwable) :cljs any?))

;; ===== Literals ===== ;;

;; Used by `quantum.untyped.core.analyze`, including via `t/literal?`
(def regex?   (isa? #?(:clj java.util.regex.Pattern :cljs js/RegExp)))

;; Used by `quantum.untyped.core.analyze`, including via `t/literal?`
(def keyword? (isa? #?(:clj clojure.lang.Keyword :cljs cljs.core/Keyword)))

;; Used by `quantum.untyped.core.analyze` via `t/literal?`
(def string?  (isa? #?(:clj java.lang.String :cljs js/String)))

;; Used by `quantum.untyped.core.analyze` via `t/literal?`
(def symbol?  (isa? #?(:clj clojure.lang.Symbol :cljs cljs.core/Symbol)))

        ;; Used by `quantum.untyped.core.analyze` via `t/literal?`
#?(:clj (def tagged-literal? (isa? clojure.lang.TaggedLiteral)))

;; Used by `quantum.untyped.core.analyze`
(def literal?
  (or nil? boolean? symbol? keyword? str? #?(:clj long?) double? regex? #?(:clj tagged-literal?)))

;; TODO this might not be right — quite possibly any seq is a valid form
;; TODO this has to be recursively true for seq, vector, map, and set
;; Possibly planned to be used by `quantum.untyped.core.analyze`
#_(def form? (or literal? +list|built-in? +vector|built-in? +map|built-in? +set|built-in?))
