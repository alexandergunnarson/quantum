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
            true? false? keyword? string? symbol?
            fn? ifn?
            meta
            ref
            type])
         (:require
           [clojure.core                               :as c]
           [clojure.set]
           [clojure.string                             :as str]
           [quantum.untyped.core.analyze.expr
             :refer [>expr #?(:cljs Expression)]]
           [quantum.untyped.core.classes               :as uclass]
           [quantum.untyped.core.collections           :as uc]
           [quantum.untyped.core.collections.logic
             :refer [seq-and seq-or]]
           [quantum.untyped.core.compare               :as ucomp
             :refer [==]]
           [quantum.untyped.core.core                  :as ucore]
           [quantum.untyped.core.data.bits             :as ubit]
           [quantum.untyped.core.data.hash             :as uhash]
           [quantum.untyped.core.data.reactive         :as urx]
           [quantum.untyped.core.data.set              :as uset
             :refer [<ident =ident >ident ><ident <>ident]]
           [quantum.untyped.core.data.tuple]
           [quantum.untyped.core.defnt
             :refer [defns defns-]]
           [quantum.untyped.core.error                 :as uerr
             :refer [err! TODO catch-all]]
           [quantum.untyped.core.fn                    :as ufn
             :refer [fn1 rcomp <- fn-> fn->>]]
           [quantum.untyped.core.form
             :refer [$]]
           [quantum.untyped.core.form.generate.deftype :as udt]
           [quantum.untyped.core.identifiers           :as uid
             :refer [>symbol]]
           [quantum.untyped.core.logic
             :refer [fn-and ifs whenp->]]
           [quantum.untyped.core.numeric               :as unum]
           [quantum.untyped.core.numeric.combinatorics :as ucombo]
           [quantum.untyped.core.print                 :as upr]
           [quantum.untyped.core.reducers              :as ur
             :refer [educe join reducei]]
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
                                ProtocolType DirectProtocolType ClassType
                                UnorderedType OrderedType
                                ValueType
                                FnType
                                MetaType MetaOrType
                                ReactiveType])]]
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
              UnorderedType OrderedType
              ValueType
              FnType
              MetaType MetaOrType
              ReactiveType])))

(ucore/log-this-ns)

#?(:clj (defmacro def
          ([sym t]   `(uvar/def ~sym    (utr/with-name ~t '~(uid/qualify *ns* sym))))
          ([sym x t] `(uvar/def ~sym ~x (utr/with-name ~t '~(uid/qualify *ns* sym))))))

#?(:clj (uvar/defalias def* quantum.untyped.core.type/def))

(declare - create-logical-type maybe-look-up-type-from-class meta-or with-expand-meta-ors nil? val?
  and or val|by-class?)

;; ===== Comparison ===== ;;

(uvar/defaliases utcomp compare < <= = not= >= > >< <>)

;; ===== Type Reification Constructors ===== ;;

;; ----- UniversalSetType (`t/U`) ----- ;;

;; `t/>` everything else
(uvar/defalias utr/universal-set)

;; ----- EmptySetType (`t/∅`) ----- ;;

;; `t/<>` everything else except `universal-set`, to which it is `t/<`
(uvar/defalias utr/empty-set)

;; ----- ReactiveType (`t/rx`) ----- ;;

(defns rx* [r urx/reactive?, body-codelist _ > utr/rx-type?]
  (ReactiveType. uhash/default uhash/default nil nil body-codelist nil r))

#?(:clj
(defmacro rx
  "Creates a reactive type. Note that the current implementation of reactivity is thread-unsafe.

   Note that if a type-generating fn (e.g. `and` or `or`) is provided with even one reactive input,
   then the whole type will become reactive. Thus, reactivity is 'infectious'.

   The only macro in all of the core type predicates."
  [& body] `(rx* (urx/!rx ~@body) ($ ~(vec body)))))

(defns- separate-rx-and-apply
  "Only works for commutative functions."
  [f c/fn?, type-args (fn-> count (c/> 1)) > utr/type?]
  ;; For efficiency, so as much as possible gets run outside a reaction
  (if-let [rx-args (->> type-args (filter utr/rx-type?) seq)]
    (if-let [norx-args (->> type-args (remove utr/rx-type?) seq)]
      (let [t (f norx-args)]
        (rx (f (cons t (map deref rx-args)))))
      (rx (f (map deref rx-args))))
    (f type-args)))

;; ----- NotType (`t/not` / `t/!`) ----- ;;

(defns not [t utr/type? > utr/type?]
  (ifs (utr/rx-type? t)    (rx (not @t))
       (= t universal-set) empty-set
       (= t empty-set)     universal-set
       (= t val|by-class?) nil?
       (utr/not-type? t)   (utr/not-type>inner-type t)
       ;; DeMorgan's Law
       (utr/or-type?  t)   (->> t utr/or-type>args  (uc/lmap not) (apply and))
       ;; DeMorgan's Law
       (utr/and-type? t)   (->> t utr/and-type>args (uc/lmap not) (apply or ))
       (NotType. uhash/default uhash/default nil nil t)))

(uvar/defalias ! not)

;; ----- OrType (`t/or` / `t/|`) ----- ;;

(def- comparison-denotes-supersession?|or (fn1 c/= >ident))

(defn- or* [ts]
  (create-logical-type :or ->OrType utr/or-type? utr/or-type>args
    comparison-denotes-supersession?|or ts))

(defn or
  "Unordered `or`. Analogous to `set/union`.
   Applies as much 'compression'/deduplication/simplification as possible to the supplied types.
   Effectively computes the union of the extension of the ->`ts`."
  ([] empty-set)
  ([t] t)
  ([t & ts] (separate-rx-and-apply or* (cons t ts))))

(uvar/defalias | or)

;; ----- AndType (`t/and` | `t/&`) ----- ;;

(def- comparison-denotes-supersession?|and (fn1 c/= <ident))

(defn- and* [ts]
  (create-logical-type :and ->AndType utr/and-type? utr/and-type>args
    comparison-denotes-supersession?|and ts))

(defn and
  "Unordered `and`. Analogous to `set/intersection`.
   Applies as much 'compression'/deduplication/simplification as possible to the supplied types.
   Effectively computes the intersection of the extension of the ->`ts`."
  ([] universal-set)
  ([t] t)
  ([t & ts] (separate-rx-and-apply and* (cons t ts))))

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

(defns- isa?|protocol [p uclass/protocol?]
  (ProtocolType. uhash/default uhash/default nil nil p))

#?(:cljs
(defns- isa?|protocol|direct [p uclass/protocol?]
  (DirectProtocolType. uhash/default uhash/default nil nil p)))

;; ----- ClassType ----- ;;

(defns- isa?|class [c #?(:clj c/class? :cljs c/fn?)]
  (ClassType. uhash/default uhash/default nil nil c))

;; ----- OrderedType ----- ;;

(defns unordered
  "Creates a type representing an unordered collection."
  ([> utr/unordered-type?] (unordered []))
  ([data _ > utr/unordered-type?]
    (ifs (utr/rx-type? data)
           (rx (UnorderedType. uhash/default uhash/default nil nil {@data 1}))
         (utr/type? data)
           (UnorderedType. uhash/default uhash/default nil nil {data 1})
         (c/not (sequential? data))
           (err! "Finite type info must be sequential" {:type (c/type data)})
         (c/not (seq-and utr/type? data))
           (err! "Not every element of finite type data is a type")
         (seq-or utr/rx-type? data)
           (rx (UnorderedType. uhash/default uhash/default nil nil
                 (->> data (uc/map+ utr/deref-when-reactive) uc/frequencies)))
         (UnorderedType. uhash/default uhash/default nil nil (frequencies data))))
  ([datum _ & data _ > utr/unordered-type?] (unordered (cons datum data))))

(defns ordered
  "Creates a type representing an ordered collection."
  ([> utr/ordered-type?] (ordered []))
  ([data _ > utr/ordered-type?]
    (ifs (utr/rx-type? data)
           (rx (OrderedType. uhash/default uhash/default nil nil [@data]))
         (utr/type? data)
           (OrderedType. uhash/default uhash/default nil nil [data])
         (c/not (sequential? data))
           (err! "Finite type info must be sequential" {:type (c/type data)})
         (c/not (seq-and utr/type? data))
           (err! "Not every element of finite type data is a type")
         (seq-or utr/rx-type? data)
           (rx (OrderedType. uhash/default uhash/default nil nil
                 (->> data (uc/map utr/deref-when-reactive))))
         (OrderedType. uhash/default uhash/default nil nil data)))
  ([datum _ & data _ > utr/ordered-type?] (ordered (cons datum data))))

;; ----- ValueType ----- ;;

(defn value
  "Creates a type whose extension is the singleton set containing only the value `v`."
  [v] (ValueType. uhash/default uhash/default nil nil v))

(defns unvalue
  [t utr/type?]
  (ifs (utr/value-type? t)   (utr/value-type>value t)
       (c/= t universal-set) t
       (err! "Don't know how to handle `unvalue` for type" {:t t})))

;; ----- `isa?` / Class-Inheritance ----- ;;

(defn isa? [x]
  (ifs (uclass/protocol? x)
       (isa?|protocol x)

       (#?(:clj c/class? :cljs c/fn?) x)
       (isa?|class x)

       (throw (ex-info "`isa?` cannot be applied to" {:x x}))))

(defn isa?|direct [x]
  (if (uclass/protocol? x)
      #?(:clj  (isa?|class (uclass/protocol>class x))
         :cljs (isa?|protocol|direct x))
      (isa? x)))

;; ------------------

(defns- -|or [t0 utr/type?, t1 utr/type?]
  (let [args (->> t0 utr/or-type>args (uc/remove (fn1 = t1)))]
    (case (count args)
      0 empty-set
      1 (first args)
      (OrType. uhash/default uhash/default nil nil args (atom nil)))))

(defn - ;; TODO `defns` when variadic args are actually handled correctly
  "Computes the difference of `t0` from `t1`: (& t0 (! t1))
   If `t0` =       `t1`, `∅`
   If `t0` <       `t1`, `∅`
   If `t0` <>      `t1`, `t0`
   If `t0` > | ><  `t1`, `t0` with all elements of `t1` removed"
  ([t0 #_utr/type? #_> #_utr/type?] t0)
  ([t0 #_utr/type?, t1 #_utr/type? #_> #_utr/type?]
    (if (utr/rx-type? t0)
        (if (utr/rx-type? t1)
            (rx (- @t0 @t1))
            (rx (- @t0  t1)))
        (if (utr/rx-type? t1)
            (rx (-  t0 @t1))
            (let [c (c/int (compare t0 t1))]
              (case c
                (0 -1) empty-set
                 3     t0
                (1 2)
                  (let [c0 (c/type t0) c1 (c/type t1)]
                    ;; TODO add dispatch?
                    (condp == c0
                      NotType (condp == (-> t0 utr/not-type>inner-type c/type)
                                ClassType (condp == c1
                                            ClassType (AndType. uhash/default uhash/default nil nil
                                                        [t0 (not t1)] (atom nil)))
                                ValueType (condp == c1
                                            ValueType (AndType. uhash/default uhash/default nil nil
                                                        [t0 (not t1)] (atom nil))))
                      OrType  (condp == c1
                                ClassType (-|or t0 t1)
                                ValueType (-|or t0 t1)))))))))
  ([t0 #_utr/type?, t1 #_utr/type? & ts #_ _ #_> #_utr/type?] (reduce - (- t0 t1) ts)))

(def* type?          (isa? PType))
(def* not-type?      (isa? NotType))
(def* or-type?       (isa? OrType))
(def* and-type?      (isa? AndType))
(def* protocol-type? (isa? ProtocolType))
(def* class-type?    (isa? ClassType))
(def* value-type?    (isa? ValueType))

;; For use in logical operators
(def* nil?           (value nil))
(def* object?        (isa? #?(:clj java.lang.Object :cljs js/Object)))

;; ===== Type metadata (not for reactive types) ===== ;;

(defns assume
  "Denotes that, whatever the declared output type (to which `assume` is applied) of a function may
   be, it is assumed that the output satisfies that type."
  [t utr/type? > utr/type?]
  (assert (c/not (utr/rx-type? t)))
  (if (utr/meta-type? t)
      (if (.-assume? ^MetaType t)
          t
          (MetaType. (.-meta ^MetaType t) nil (.-t ^MetaType t)
                     true (.-ref? ^MetaType t) false)) ; un-`t/run`s it
      (MetaType. (c/meta t) nil t true false false)))

(defns assume? [t utr/type? > c/boolean?] (c/and (utr/meta-type? t) (.-assume? ^MetaType t)))

(defns unassume [t utr/type? > utr/type?]
  (assert (c/not (utr/rx-type? t)))
  (if (utr/meta-type? t)
      (if-not (.-assume? ^MetaType t)
        t
        (MetaType. (.-meta ^MetaType t) nil (.-t ^MetaType t)
                   false (.-ref? ^MetaType t) (.-runtime? ^MetaType t))) ; un-`t/run`s it
      t))

(defns run
  "Denote on a type that it must be enforced at runtime.
   For use with `defnt`."
  [t utr/type? > utr/type?]
  (assert (c/not (utr/rx-type? t)))
  (if (utr/meta-type? t)
      (if (.-runtime? ^MetaType t)
          t
          (MetaType. (.-meta ^MetaType t) nil (.-t ^MetaType t)
                     false (.-ref? ^MetaType t) true)) ; un-`t/assume`s it
      (MetaType. (c/meta t) nil t false false true)))

(defns run? [t utr/type? > c/boolean?] (c/and (utr/meta-type? t) (.-runtime? ^MetaType t)))

(defns ref
  "Denote on a type that it must not be expanded to use primitive values.
   For use with `defnt`."
  [t utr/type? > utr/type?]
  (assert (c/not (utr/rx-type? t)))
  (if (utr/meta-type? t)
      (if (.-ref? ^MetaType t)
          t
          (MetaType. (.-meta ^MetaType t) nil (.-t ^MetaType t)
                     (.-assume? ^MetaType t) true (.-runtime? ^MetaType t)))
      (MetaType. (c/meta t) nil t false true false)))

(defns unref [t utr/type? > utr/type?]
  (assert (c/not (utr/rx-type? t)))
  (if (utr/meta-type? t)
      (if-not (.-ref? ^MetaType t)
        t
        (MetaType. (.-meta ^MetaType t) nil (.-t ^MetaType t)
                   (.-assume? ^MetaType t) false (.-runtime? ^MetaType t)))
      t))

(defns type-ref? [t utr/type? > c/boolean?] (c/and (utr/meta-type? t) (.-ref? ^MetaType t)))

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

(defn- logical-compare
  "This is so `t/empty-set` doesn't get left in `t/or`s or `t/and`s."
  [t0 #_utr/type?, t1 #_utr/type? #_> #_uset/comparison?]
  (if (== t0 empty-set)
      (if (== t1 empty-set) =ident <ident)
      (if (== t1 empty-set) >ident (compare t0 t1))))

(defns- create-logical-type|inner|or
  [{:as accum :keys [t' utr/type?]} _, t* utr/type?, c* uset/comparison?]
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
   t* utr/type?, c* uset/comparison?]
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
               (uc/map+    (juxt identity #(logical-compare t %)))
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
  (->> type-args uc/distinct+))

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

(defns- create-logical-type|non-meta-ors
  [kind #{:or :and}, construct-fn _, type-pred _, type>args _
   comparison-denotes-supersession? c/fn?, type-args (fn-> count (c/>= 1)) > utr/type?]
  (let [simplified
          (->> type-args
               (simplify-logical-type|inner-expansion+ type-pred type>args)
               simplify-logical-type|structural-identity+
               (simplify-logical-type|comparison kind comparison-denotes-supersession?))]
    (assert (-> simplified count (c/>= 1))) ; for internal implementation correctness
    (if (-> simplified count (c/= 1))
        (first simplified)
        (construct-fn uhash/default uhash/default nil nil simplified (atom nil)))))

(defns- create-logical-type
  [kind #{:or :and}, construct-fn _, type-pred _, type>args _
   comparison-denotes-supersession? c/fn?, type-args (fn-> count (c/>= 1)) > utr/type?]
  (with-expand-meta-ors type-args
    (fn [types']
      (create-logical-type|non-meta-ors
        kind construct-fn type-pred type>args comparison-denotes-supersession? types'))))

;; ===== `t/ftype` ===== ;;

(defn ftype [& args]
  (let [?fn-name     (when (-> args first c/symbol?)
                       (first args))
        rest-args    (if ?fn-name (rest args) args)
        output-type  (if (-> rest-args first c/sequential?)
                         universal-set
                         (first rest-args))
        arities-form (if (-> rest-args first c/sequential?)
                         rest-args
                         (rest rest-args))
        arities      (->> arities-form
                          (uc/map+ (c/fn [arity-form]
                                     (-> (us/conform ::fn-type|arity arity-form)
                                         (update :output-type #(c/or % output-type universal-set)))))
                          (uc/group-by #(-> % :input-types count)))]
    (FnType. nil nil ?fn-name output-type arities-form arities)))

(defns compare|in [x0 utr/fn-type?, x1 utr/fn-type? > uset/comparison?]
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

(defns compare|out [x0 utr/fn-type?, x1 utr/fn-type? > uset/comparison?]
  (utcomp/compare (fn-type>output-type x0) (fn-type>output-type x1)))

(us/def ::match-spec
  (us/seq-of (us/or* #{:_ :?} type?
                     (us/tuple #{:?}        type?)
                     (us/tuple       c/ifn? type?)
                     (us/tuple #{:?} c/ifn? type?))))

(defn- find-spec? [match-spec-element]
  (c/or (c/= match-spec-element :?)
        (c/and (sequential? match-spec-element)
               (-> match-spec-element first (c/= :?)))))

(defn- match-spec>type-data-seq
  "Returns the type data of overloads that support the specified arg types."
  [t match-spec]
  (let [type-data-seq (-> t utr/fn-type>arities (get (count match-spec)))]
    (->> match-spec
         (uc/map-indexed+ vector)
         (uc/remove #(c/or (-> % second (c/= :_)) (-> % second (c/= :?))))
         (educe
           (c/fn ([] type-data-seq)
                 ([type-data-seq'] type-data-seq')
                 ([type-data-seq' [i|arg arg-type-or-vec]]
                   (let [[compf arg-type]
                           (if (sequential? arg-type-or-vec)
                               (if (-> arg-type-or-vec first (c/= :?))
                                   (if (-> arg-type-or-vec count (c/= 3))
                                       [(nth arg-type-or-vec 1) (nth arg-type-or-vec 2)]
                                       [utcomp/<=               (nth arg-type-or-vec 1)])
                                   [(nth arg-type-or-vec 0) (nth arg-type-or-vec 1)])
                               [utcomp/<= arg-type-or-vec])]
                     (c/or (->> type-data-seq'
                                (uc/lfilter (c/fn [{:keys [input-types]}]
                                              (compf arg-type (get input-types i|arg))))
                                seq)
                           (reduced nil)))))))))

(defn- input-or-output-type-handle-reactive [f t args]
  (if (utr/rx-type? t)
      (if (seq-or utr/rx-type? args)
          (rx (f @t (map utr/deref-when-reactive args)))
          (rx (f @t args)))
      (if (seq-or utr/rx-type? args)
          (rx (f t (map utr/deref-when-reactive args)))
          (f t args))))

(defn- input-type|meta-or|norx [t match-spec #_::match-spec]
  (let [i|? (->> match-spec (reducei (c/fn [_ t i] (when (find-spec? t) (reduced i))) nil))]
    (with-expand-meta-ors match-spec
      (fn [match-spec']
        (->> match-spec'
             (match-spec>type-data-seq t)
             (uc/map (c/fn [{:keys [input-types]}] (get input-types i|?)))
             meta-or)))))

(defns input-type|meta-or
  [t (us/or* utr/fn-type? utr/rx-type?), match-spec _ #_::match-spec
   | (->> match-spec (filter find-spec?) count (c/= 1))
   > type?]
  (input-or-output-type-handle-reactive input-type|meta-or|norx t match-spec))

(defn- input-type|or|norx [t match-spec]
  (let [t' (input-type|meta-or|norx t match-spec)]
    (cond-> t' (utr/meta-or-type? t') (->> utr/meta-or-type>types (apply or)))))

(defns input-type|or
  "Outputs the type of a specified input to a typed fn."
  [t (us/or* utr/fn-type? utr/rx-type?), match-spec _ #_::match-spec
   | (->> match-spec (filter find-spec?) count (c/= 1))
   > type?]
  (input-or-output-type-handle-reactive input-type|or|norx t match-spec))

(defn input-type
  "Usage in arglist contexts:
   - `(t/input-type >namespace :?)`
     - Outputs a reactive type embodying the union of the possible types of the first input to
       `>namespace`.
   - `(t/input-type reduce :_ :_ :?)`
     - Outputs a reactive type embodying the union of the possible types of the third input to
       `reduce`.
   - `(t/input-type reduce :? :_ string?)`
     - Outputs a reactive type embodying the union of the possible types of the first input to
       `reduce` when the third input satisfies `string?`."
  ([t & args] (err! "Can't use `input-type` outside of arglist contexts")))

(defn- output-type|meta-or|norx [t args]
  (with-expand-meta-ors args
    (fn->> (match-spec>type-data-seq t)
           (uc/map :output-type)
           meta-or)))

(defns output-type|meta-or
  [t (us/or* utr/fn-type? utr/rx-type?) args (us/seq-of (us/or* #{:_} type?)) > type?]
  (input-or-output-type-handle-reactive output-type|meta-or|norx t args))

(defn- output-type|or|norx [t args]
  (let [t' (output-type|meta-or|norx t args)]
    (cond-> t' (utr/meta-or-type? t') (->> utr/meta-or-type>types (apply or)))))

(defns output-type|or
  "Outputs the output type of a typed fn."
  [t (us/or* utr/fn-type? utr/rx-type?) args (us/seq-of (us/or* #{:_} type?)) > type?]
  (input-or-output-type-handle-reactive output-type|or|norx t args))

(defn output-type
  "Usage in arglist contexts:
   - `(t/output-type >namespace :any)`
     - (TODO) Outputs a reactive type embodying the union of the possible output types of
       `>namespace` given any valid inputs at all
   - `(t/output-type reduce [:_ :_ string?])`
     - Outputs a reactive type embodying the union of the possible output types of `reduce` when
       the third input satisfies `string?`."
  ([t & args] (err! "Can't use `output-type` outside of arglist contexts")))

;; ===== Dependent types ===== ;;

(defns type
  "When used within the type declaration of a function input, returns the compile-time type of `x`.
   For all other cases, returns `(t/value x)` at runtime."
  [x _ > type?] (value x))

;; TODO figure this out
;; TODO move to reifications
#_(do (udt/deftype DeducibleType [*t #_(t/atom-of t/type?)]
  {PSpec                 nil
   fipp.ednize/IOverride nil
   fipp.ednize/IEdn      {-edn ([this] (list `deducible @*t))}
   ?Atom                 {swap! (([this f] (swap!  *t f)))
                          reset! ([this v] (reset! *t v))}})

(defns deducible-type? [x _] (instance? DeducibleType x))

(defns deducible [x type? > deducible-type?] (DeducibleType. (atom x))))

(defn ?
  "Computes a type denoting a nilable value satisfying `t`."
  ([t #_utr/type? #_> #_utr/type?] (or nil? t)))

;; ===== Etc. ===== ;;

(defns- with-expand-meta-ors [type-args (us/seq-of type?), f c/fn?]
  (if-not (seq-or utr/meta-or-type? type-args)
    (f type-args)
    (->> type-args
         (uc/map (fn [t] (if (utr/meta-or-type? t)
                             (utr/meta-or-type>types t)
                             [t])))
         (apply ucombo/cartesian-product)
         (uc/map f)
         meta-or)))

(defns- meta-or|norx
  > utr/type?
  [types (us/seq-of utr/type?)]
  (let [types' (->> types uc/distinct (sort-by identity utcomp/compare) (uc/dedupe-by utcomp/=))]
    (ifs (empty? types')           empty-set
         (-> types' count (c/= 1)) (first types')
         (MetaOrType. uhash/default uhash/default nil nil types'))))

(defns meta-or
  "Essentially a combinatorial combinator:

   (t/or (t/meta-or [byte? short? char?]) string?)
   -> (t/meta-or [(t/or byte?  string?)
                  (t/or short? string?)
                  (t/or char?  string?)]))

   - Commutative.
   - Dedupes inputs that are either structurally `=` or `t/=`.
   - Does not currently handle nested `meta-or`s."
   > utr/type?
   [types (us/seq-of utr/type?)]
   (separate-rx-and-apply meta-or|norx types))

;; TODO figure out the best place to put this
#?(:clj
(def unboxed-class->boxed-class
  {Boolean/TYPE   Boolean
   Byte/TYPE      Byte
   Short/TYPE     Short
   Character/TYPE Character
   Integer/TYPE   Integer
   Long/TYPE      Long
   Float/TYPE     Float
   Double/TYPE    Double}))

#?(:clj (def boxed-class->unboxed-class (clojure.set/map-invert unboxed-class->boxed-class)))

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
  [t utr/type?, include-classes-of-value-type? c/boolean?, classes c/set?
   > (us/set-of (us/nilable #?(:clj c/class? :cljs c/fn?)))]
  (cond (utr/meta-type? t)
          (recur (utr/meta-type>inner-type t) include-classes-of-value-type? classes)
        (utr/class-type? t)
          (conj classes (utr/class-type>class t))
        (utr/protocol-type? t)
          ;; probably better than specifying *all* implementing classes
          (conj classes (-> t utr/protocol-type>protocol :on-interface) Object)
        (utr/value-type? t)
          (cond-> classes
            include-classes-of-value-type? (conj (-> t utr/value-type>value c/type)))
        (c/= t universal-set)
          #?(:clj  #{nil java.lang.Object}
             :cljs (TODO "Not sure what to do in the case of universal CLJS set"))
        (c/= t empty-set)
          #{}
        (utr/and-type? t)
          (reduce (c/fn [classes' t'] (-type>classes t' include-classes-of-value-type? classes'))
            classes (utr/and-type>args t))
        (utr/or-type? t)
          (reduce (c/fn [classes' t'] (-type>classes t' include-classes-of-value-type? classes'))
            classes (utr/or-type>args t))
        (c/= t val?) ; TODO make this less ad-hoc
          (-type>classes val|by-class? include-classes-of-value-type? classes)
        :else
          (err! "Not sure how to handle type" t)))

(defns type>classes
  "Outputs the set of all the classes ->`t` can embody, possibly including nil."
  ([t utr/type? > (us/set-of (us/nilable #?(:clj c/class? :cljs c/fn?)))] (type>classes t true))
  ([t utr/type?, include-classes-of-value-type? c/boolean?
    > (us/set-of (us/nilable #?(:clj c/class? :cljs c/fn?)))]
    (-type>classes t include-classes-of-value-type? #{})))

;; TODO move
#?(:clj
(defns class>boxed-subclasses+ [c (us/nilable c/class?) #_> #_(educer-of c/class?)]
  (when (some? c)
    (->> boxed-class->unboxed-class
         uc/keys+
         (uc/filter+ (fn [^Class uc] (.isAssignableFrom ^Class c uc)))))))

;; TODO move
#?(:clj
(defns class>most-primitive-class
  "Unboxes the class if possible."
  [c (us/nilable c/class?) > (us/nilable c/class?)]
  (c/or (boxed-class->unboxed-class c) c)))

#?(:clj
(defns type>most-primitive-classes
  "The same as `type>classes` except unboxes all possible classes.
   Distinct from primitive-expansion / primitivization."
  [t type? > (us/set-of (us/nilable c/class?))]
  (let [cs (type>classes t)]
    (if (c/or (contains? cs nil) (type-ref? t))
        cs
        (->> cs (uc/map+ class>most-primitive-class) (ur/join #{}))))))

#?(:clj
(defns type>primitive-subtypes
  ([t type? > (us/set-of type?)] (type>primitive-subtypes t true))
  ([t type?, include-subtypes-of-value-type? c/boolean? > (us/set-of type?)]
  (if (type-ref? t)
      #{}
      (->> (type>classes t include-subtypes-of-value-type?)
           (uc/mapcat+ class>boxed-subclasses+)
           uc/distinct+
           (uc/map+ maybe-look-up-type-from-class)
           (ur/join #{}))))))

#?(:clj
(defns primitive-type? [t type? > c/boolean?]
  (c/and (-> t type-ref? c/not)
         (let [cs (type>classes t)]
           (c/and (-> cs count (c/= 1))
                  (contains? boxed-class->unboxed-symbol (first cs)))))))

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

         (def* none?         empty-set)
         (def* any?          universal-set)

                            ;; TODO this is incomplete for CLJS base classes
                            ;; TODO is this necessary?
         (def* val|by-class? (or object? #?@(:cljs [(isa? js/String) (isa? js/Symbol)])))
         (def* val?          (not nil?))

         (def* ref?          (ref any?))

;; ===== Meta ===== ;;

         ;; TODO probably move, but this is used by `quantum.untyped.core.type` etc.
#?(:clj  (def* primitive-class? (or (value Boolean/TYPE)
                                    (value Byte/TYPE)
                                    (value Character/TYPE)
                                    (value Short/TYPE)
                                    (value Integer/TYPE)
                                    (value Long/TYPE)
                                    (value Float/TYPE)
                                    (value Double/TYPE))))

;; ===== Primitives ===== ;;
;; NOTE these are kept here because they're used in both type analysis and various test namespaces

         (def*  boolean? (isa? #?(:clj Boolean :cljs js/Boolean)))
#?(:clj  (def*  byte?    (isa? Byte)))
#?(:clj  (def*  char?    (isa? Character)))
#?(:clj  (def*  short?   (isa? Short)))
#?(:clj  (def*  int?     (isa? Integer))) ; only primitive int, not goog.math.Integer
#?(:clj  (def*  long?    (isa? Long))) ; only primitive long, not goog.math.Long
#?(:clj  (def*  float?   (isa? Float)))
         (def*  double?  (isa? #?(:clj Double :cljs js/Number)))

         ;; These are special for CLJS protocols
         ;; Possibly planned to be used by `quantum.untyped.core.analyze`
#?(:cljs (def*  native?  (or (isa? js/Boolean)
                             (isa? js/Number)
                             (isa? js/Object)
                             (isa? js/Array)
                             (isa? js/String)
                             (isa? js/Function)
                             nil?)))

#?(:clj
(defns maybe-look-up-type-from-class
  "To save on memory — rather than creating a new `t/isa?` for every primitive class, uses the ones
   already created in `quantum.untyped.core.type`."
  [^Class c c/class? > utr/type?]
  (case (.getName c)
    "java.lang.Boolean"   boolean?
    "java.lang.Byte"      byte?
    "java.lang.Short"     short?
    "java.lang.Character" char?
    "java.lang.Integer"   int?
    "java.lang.Long"      long?
    "java.lang.Float"     float?
    "java.lang.Double"    double?
    (isa? c))))

;; ===== Booleans ===== ;;

;; Used by `quantum.untyped.core.analyze`
(def* true?  (value true))
(def* false? (value false))

;; ========== Collections ========== ;;

;; Possibly planned to be used by `quantum.untyped.core.analyze`
(def* +list|built-in?
  (or (isa? #?(:clj clojure.lang.PersistentList$EmptyList :cljs cljs.core/EmptyList))
      (isa? #?(:clj clojure.lang.PersistentList           :cljs cljs.core/List))))

;; Used by `quantum.untyped.core.analyze`
(def* +vector|built-in?
  (isa? #?(:clj clojure.lang.PersistentVector :cljs cljs.core/PersistentVector)))

;; Used by `quantum.untyped.core.analyze`
(def* +unordered-map|built-in?
  (or (isa? #?(:clj clojure.lang.PersistentHashMap  :cljs cljs.core/PersistentHashMap))
      (isa? #?(:clj clojure.lang.PersistentArrayMap :cljs cljs.core/PersistentArrayMap))))

;; Used by `quantum.untyped.core.analyze`
(def* +map|built-in?
  (or +unordered-map|built-in?
      (isa? #?(:clj clojure.lang.PersistentTreeMap :cljs cljs.core/PersistentTreeMap))))

;; Used by `quantum.untyped.core.analyze`
(def* +unordered-set|built-in?
  (isa? #?(:clj clojure.lang.PersistentHashSet :cljs cljs.core/PersistentHashSet)))

;; Used by `quantum.untyped.core.analyze`
(def* +set|built-in?
  (or +unordered-set|built-in?
      (isa? #?(:clj clojure.lang.PersistentTreeSet :cljs cljs.core/PersistentTreeSet))))

;; ===== Functions ===== ;;

;; Used by `quantum.untyped.core.analyze`
(def* fn? #?(:clj  (isa? clojure.lang.Fn)
             :cljs (or (isa? js/Function) (isa? cljs.core/Fn))))

;; Used by `quantum.untyped.core.analyze` via `t/callable?`
(uvar/def ifn?
  "Note that in CLJS, `cljs.core/ifn?` checks if something is either `fn?` or if it satisfies
   `cljs.core/IFn`. By contrast, this type encompasses only direct implementers of `cljs.core/IFn`."
  (isa?|direct #?(:clj clojure.lang.IFn :cljs cljs.core/IFn)))

;; Used by `quantum.untyped.core.analyze` via `t/callable?`
(def* fnt? (and fn? (>expr (fn-> c/meta :quantum.core.type/type type?))))

;; TODO should we allow java.lang.Runnable, java.util.concurrent.Callable, and other
;; functional interfaces to be `callable?`?
;; Used by `quantum.untyped.core.analyze`
(uvar/def callable?
  "The set of all objects that are able to called/invoked by being in functor position
   (first element of an unquoted list) within a typed context."
  (or fn? ifn? fnt?))

;; ===== Metadata ===== ;;

;; Used by `quantum.untyped.core.analyze.ast`
(def* with-metable? (isa? #?(:clj clojure.lang.IObj :cljs cljs.core/IWithMeta)))

;; ===== Errors ===== ;;

;; Used by `quantum.untyped.core.analyze`
(def* throwable? "Able to be used with `throw`" #?(:clj (isa? java.lang.Throwable) :cljs any?))

;; ===== Literals ===== ;;

;; Used by `quantum.untyped.core.analyze`, including via `t/literal?`
(def* regex?   (isa? #?(:clj java.util.regex.Pattern :cljs js/RegExp)))

;; Used by `quantum.untyped.core.analyze`, including via `t/literal?`
(def* keyword? (isa? #?(:clj clojure.lang.Keyword :cljs cljs.core/Keyword)))

;; Used by `quantum.untyped.core.analyze` via `t/literal?`
(def* string?  (isa? #?(:clj java.lang.String :cljs js/String)))

;; Used by `quantum.untyped.core.analyze` via `t/literal?`
(def* symbol?  (isa? #?(:clj clojure.lang.Symbol :cljs cljs.core/Symbol)))

        ;; Used by `quantum.untyped.core.analyze` via `t/literal?`
#?(:clj (def* tagged-literal? (isa? clojure.lang.TaggedLiteral)))

;; Used by `quantum.untyped.core.analyze`
(def* literal?
  (or nil? boolean? char? string? symbol? keyword?
      ;; These primitives are allowed even though macros emit them incorrectly, because they're
      ;; converted via `>form` during analysis
      #?(:clj byte?) #?(:clj short?) #?(:clj int?) #?(:clj float?)
      #?(:clj long?) double? #?(:clj (isa? clojure.lang.BigInt)) #?(:clj (isa? BigDecimal))
      regex? #?(:clj tagged-literal?)))

;; TODO this might not be right — quite possibly any seq is a valid form
;; TODO this has to be recursively true for seq, vector, map, and set
;; Possibly planned to be used by `quantum.untyped.core.analyze`
#_(def form? (or literal? +list|built-in? +vector|built-in? +map|built-in? +set|built-in?))
