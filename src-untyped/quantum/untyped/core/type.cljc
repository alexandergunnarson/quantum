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
     associative? coll? counted? indexed? list? map? map-entry? record?
     seq? seqable? sequential? set? sorted? vector?
     fn? ifn?
     meta ref volatile?])
  (:require
    [clojure.core                               :as c]
    [clojure.string                             :as str]
    [quantum.untyped.core.analyze.expr          :as xp
      :refer [>expr #?(:cljs Expression)]]
    [quantum.untyped.core.classes               :as uclass]
    [quantum.untyped.core.collections           :as uc]
    [quantum.untyped.core.collections.logic
      :refer [seq-and seq-or]]
    [quantum.untyped.core.compare               :as ucomp
      :refer [== not==]]
    [quantum.untyped.core.convert               :as uconv
      :refer [>symbol]]
    [quantum.untyped.core.core                  :as ucore]
    [quantum.untyped.core.data.bits             :as ubit]
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
    [quantum.untyped.core.spec                  :as s]
    [quantum.untyped.core.type.core             :as utcore]
    [quantum.untyped.core.type.defs             :as utdef]
    [quantum.untyped.core.type.predicates       :as utpred]
    [quantum.untyped.core.vars                  :as uvar
      :refer [def- defmacro- update-meta]])
  #?(:clj (:import quantum.untyped.core.analyze.expr.Expression
                   quantum.untyped.core.data.tuple.Tuple))
#?(:cljs
  (:require-macros
    [quantum.untyped.core.type :as self
      :refer [-def]])))

(ucore/log-this-ns)

#_(defmacro ->
  ("Anything that is coercible to x"
    [x]
    ...)
  ("Anything satisfying `from` that is coercible to `to`.
    Will be coerced to `to`."
    [from to]))

#_(defmacro range-of)

(defonce *spec-registry (atom {}))
(swap! *spec-registry empty)

;; ===== SPECS ===== ;;

(defprotocol PSpec)

(udt/deftype ValueSpec [v #_any?]
  {PSpec                 nil
   fipp.ednize/IOverride nil
   fipp.ednize/IEdn      {-edn    ([this] (list `value v))}
   ?Fn                   {invoke  ([_ x] (c/= x v))}
   ?Object               {equals  ([this that #_any?]
                                    (c/or (== this that)
                                          (c/and (instance? ValueSpec that)
                                                 (c/= v (.-v ^ValueSpec that)))))}})

(defns value
  "Creates a spec whose extension is the singleton set containing only the value `v`."
  [v _] (ValueSpec. v))

(defns value-spec? [x _] (instance? ValueSpec x))

(defns value-spec>value [x value-spec?] (.-v ^ValueSpec x))

;; -----

(udt/deftype ClassSpec
  [       meta #_(t/? ::meta)
   ^Class c    #_t/class?
          name #_(t/? t/symbol?)]
  {PSpec                 nil
   fipp.ednize/IOverride nil
   fipp.ednize/IEdn
     {-edn ([this] (c/or name (list `isa? c)))}
   ?Fn     {invoke    ([_ x] (instance? c x))}
   ?Meta   {meta      ([this] meta)
            with-meta ([this meta'] (ClassSpec. meta' c name))}
   ?Object {equals    ([this that #_any?]
                        (c/or (== this that)
                              (c/and (instance? ClassSpec that)
                                     (c/= c (.-c ^ClassSpec that)))))}})

(defns class-spec? [x _] (instance? ClassSpec x))

(defns class-spec>class [spec class-spec?] (.-c ^ClassSpec spec))

(udt/deftype ProtocolSpec
  [meta #_(t/? ::meta)
   p    #_t/protocol?
   name #_(t/? t/symbol?)]
  {PSpec                 nil
   fipp.ednize/IOverride nil
   fipp.ednize/IEdn      {-edn      ([this] (c/or name (list `isa?|protocol (:on p))))}
   ?Fn                   {invoke    ([_ x] (satisfies? p x))}
   ?Meta                 {meta      ([this] meta)
                          with-meta ([this meta'] (ProtocolSpec. meta' p name))}})

(defns protocol-spec? [x _] (instance? ProtocolSpec x))

(defns protocol-spec>protocol [spec protocol-spec?] (.-p ^ProtocolSpec spec))

(defns- isa?|protocol [p utpred/protocol?] (ProtocolSpec. nil p nil))

(defn isa? [x]
  (ifs #?(:clj  (utpred/protocol? x)
                ;; Unfortunately there's no better check in CLJS, at least as of 03/18/2018
          :cljs (c/and (c/fn? x) (c/= (str x) "function (){}")))
       (isa?|protocol x)

       (#?(:clj c/class? c/fn?) x)
       (ClassSpec. nil x nil)))

;; ===== CREATION ===== ;;

(defonce *spec-registry (atom {}))

#?(:clj (extend-protocol PSpec Expression))

(declare nil?)

(defn >spec
  "Coerces ->`x` to a spec, recording its ->`name-sym` if provided."
  ([x] (>spec x nil))
  ([x name-sym]
    (assert (c/or (c/nil? name-sym) (c/symbol? name-sym)))
    #?(:clj
        (cond (satisfies? PSpec x)
                x ; TODO should add in its name?
              (c/class? x)
                (let [x (c/or #?(:clj (utcore/unboxed->boxed x)) x)
                      reg (if (c/nil? name-sym)
                              @*spec-registry
                              (swap! *spec-registry
                                (fn [reg]
                                  (if-let [spec (get reg name-sym)]
                                    (if (c/= (.-name ^ClassSpec spec) name-sym)
                                        reg
                                        (err! "Class already registered with spec; must first undef" {:class x :spec-name name-sym}))
                                    (let [spec (ClassSpec. nil x name-sym)]
                                      (uc/assoc-in reg [name-sym]    spec
                                                       [:by-class x] spec))))))]
                  (c/or (get-in reg [:by-class x])
                        (ClassSpec. nil ^Class x name-sym)))
              (c/fn? x)
                (let [sym (c/or name-sym (>symbol x))
                      _ (when-not name-sym
                          (let [resolved (?deref (ns-resolve *ns* sym))]
                            (assert (== resolved x) {:x x :sym sym :resolved resolved})))]
                  (Expression. sym x))
              (c/nil? x)
                nil?
              (utpred/protocol? x)
                (ProtocolSpec. nil x name-sym)
              :else
                (value x))
       :cljs nil)))

;; ===== DEFINITION ===== ;;

(defn register-spec! [sym spec]
  (assert (satisfies? PSpec spec) spec)
  (TODO))

#?(:clj
(defmacro define [sym spec]
  `(~'def ~sym (let [spec# ~spec]
                 (assert (satisfies? PSpec spec#) spec#)
                 #_(register-spec! '~(qual/qualify sym) spec#)
                 spec#))))

(defn undef [reg sym]
  (if-let [spec (get reg sym)]
    (let [reg' (dissoc reg sym)]
      (if (instance? ClassSpec spec)
          (uc/dissoc-in reg' [:by-class (.-c ^ClassSpec spec)])
          (TODO)))
    reg))

(defn undef! [sym] (swap! *spec-registry undef sym))

#_(:clj
(defmacro defalias [sym spec]
  `(~'def ~sym (>spec ~spec))))

#?(:clj (uvar/defalias -def define))

(-def spec? (isa? PSpec))

(defns *
  "Denote on a spec that it must be enforced at runtime.
   For use with `defnt`."
  [spec spec?] (update-meta spec assoc :runtime? true))

(defns ref
  "Denote on a spec that it must not be expanded to use primitive values.
   For use with `defnt`."
  [spec spec?] (update-meta spec assoc :ref? true))

(udt/deftype DeducibleSpec [*spec #_(t/atom-of t/spec?)]
  {PSpec                 nil
   fipp.ednize/IOverride nil
   fipp.ednize/IEdn      {-edn ([this] (list `deducible @*spec))}
   ?Atom                 {swap! (([this f] (swap!  *spec f)))
                          reset! ([this v] (reset! *spec v))}})

(defns deducible [x spec?] (DeducibleSpec. (atom x)))

(defns deducible-spec? [x _] (instance? DeducibleSpec x))

;; ===== EXTENSIONALITY COMPARISON IMPLEMENTATIONS ===== ;;

#_(is (coll&/incremental-every? (aritoid nil (constantly true) t/in>)
        [String Comparable Object])
      (coll&/incremental-every? (aritoid nil (constantly true) t/in>)
        [Long Number]))

(defns compare|class|class*
  "Compare extension (generality|specificity) of ->`c0` to ->`c1`.
   `0`  means they are equally general/specific:
     - ✓ `(t/= c0 c1)`    : the extension of ->`c0` is equal to             that of ->`c1`.
   `-1` means ->`c0` is less general (more specific) than ->`c1`.
     - ✓ `(t/< c0 c1)`    : the extension of ->`c0` is a strict subset   of that of ->`c1`.
   `1`  means ->`c0` is more general (less specific) than ->`c1`:
     - ✓ `(t/> c0 c1)`    : the extension of ->`c0` is a strict superset of that of ->`c1`.
   `2`  means:
     - ✓ `(t/>< c0 c1)`   : the intersect of the extensions of ->`c0` and ->`c1` is non-empty,
                             but neither ->`c0` nor ->`c1` share a subset/equality/superset
                             relationship.
   `3`  means their generality/specificity is incomparable:
     - ✓ `(t/<> c0 c1)`   : the extension of ->`c0` is disjoint w.r.t. to that of ->`c1`.
   Unboxed primitives are considered to be less general (more specific) than boxed primitives."
  [^Class c0 class? ^Class c1 class?]
  #?(:clj (ifs (== c0 c1)                                0
               (== c0 Object)                            1
               (== c1 Object)                           -1
               (== (utcore/boxed->unboxed c0) c1)        1
               (== c0 (utcore/boxed->unboxed c1))       -1
               ;; we'll consider the two unrelated
               (c/not (utcore/array-depth-equal? c0 c1)) 3
               (.isAssignableFrom c0 c1)                 1
               (.isAssignableFrom c1 c0)                -1
               ;; multiple inheritance of interfaces
               (c/or (c/and (uclass/interface? c0)
                            (c/not (uclass/final? c1)))
                     (c/and (uclass/interface? c1)
                            (c/not (uclass/final? c0)))) 2
               3)
     :cljs (TODO)))

;; ===== EXTENSIONALITY COMPARISON ===== ;;

(declare compare|dispatch)

(defns compare
  ;; TODO optimize the `recur`s here as they re-take old code paths
  "Returns the value of the comparison of the extensions of ->`s0` and ->`s1`.
   `-1` means (ex ->`s0`) ⊂                                 (ex ->`s1`)
    `0` means (ex ->`s0`) =                                 (ex ->`s1`)
    `1` means (ex ->`s0`) ⊃                                 (ex ->`s1`)
    `2` means (ex ->`s0`) shares other intersect w.r.t. (∩) (ex ->`s1`)
    `3` means (ex ->`s0`) disjoint               w.r.t. (∅) (ex ->`s1`)

   Does not compare cardinalities or other relations of sets, but rather only sub/superset
   relations."
  [s0 spec?, s1 spec?]
  (let [dispatched (-> compare|dispatch (get (type s0)) (get (type s1)))]
    (if (c/nil? dispatched)
        (err! (str "Specs not handled: " {:s0 s0 :s1 s1}) {:s0 s0 :s1 s1})
        (dispatched s0 s1))))

(defns <
  "Computes whether the extension of spec ->`s0` is a strict subset of that of ->`s1`."
  ([s1 spec?] #(< % s1))
  ([s0 spec?, s1 spec?] (let [ret (compare s0 s1)] (c/= ret -1))))

(defns <=
  "Computes whether the extension of spec ->`s0` is a (lax) subset of that of ->`s1`."
  ([s1 spec?] #(<= % s1))
  ([s0 spec?, s1 spec?] (let [ret (compare s0 s1)] (c/or (c/= ret -1) (c/= ret 0)))))

(defns =
  "Computes whether the extension of spec ->`s0` is equal to that of ->`s1`."
  ([s1 spec?] #(= % s1))
  ([s0 spec?, s1 spec?] (c/= (compare s0 s1) 0)))

(defns not=
  "Computes whether the extension of spec ->`s0` is not equal to that of ->`s1`."
  ([s1 spec?] #(not= % s1))
  ([s0 spec?, s1 spec?] (c/not (= s0 s1))))

(defns >=
  "Computes whether the extension of spec ->`s0` is a (lax) superset of that of ->`s1`."
  ([s1 spec?] #(>= % s1))
  ([s0 spec?, s1 spec?] (let [ret (compare s0 s1)] (c/or (c/= ret 1) (c/= ret 0)))))

(defns >
  "Computes whether the extension of spec ->`s0` is a strict superset of that of ->`s1`."
  ([s1 spec?] #(> % s1))
  ([s0 spec?, s1 spec?] (c/= (compare s0 s1) 1)))

(defns ><
  "Computes whether it is the case that the intersect of the extensions of spec ->`s0`
   and ->`s1` is non-empty, and neither ->`s0` nor ->`s1` share a subset/equality/superset
   relationship."
  ([s1 spec?] #(>< % s1))
  ([s0 spec?, s1 spec?] (c/= (compare s0 s1) 2)))

(defns <>
  "Computes whether the respective extensions of specs ->`s0` and ->`s1` are disjoint."
  ([s1 spec?] #(<> % s1))
  ([s0 spec? s1 spec?] (c/= (compare s0 s1) 3)))

(defn inverse [comparison]
  (case comparison
    -1       1
     1      -1
    (0 2 3) comparison))

;; ===== LOGICAL ===== ;;

(defprotocol PLogicalComplement
  (>logical-complement [this]
    "Returns the content inside a `t/not` applied to the `args` of an n-ary logical
     spec (e.g. `or`, `and`). Stored in such specs to more easily compare them with
     `not` specs.
     E.g. `(>logical-complement (and a b))` -> `(or  (not a) (not b))`
          `(>logical-complement (or  a b))` -> `(and (not a) (not b))`."))

(udt/deftype ^{:doc "Equivalent to `(constantly false)`"} EmptySetSpec []
  {PSpec                 nil
   fipp.ednize/IOverride nil
   fipp.ednize/IEdn      {-edn ([this] `∅)}})

(def empty-set (EmptySetSpec.))

(udt/deftype ^{:doc "Equivalent to `(constantly true)`"} UniversalSetSpec []
  {PSpec                 nil
   fipp.ednize/IOverride nil
   fipp.ednize/IEdn      {-edn ([this] `U)}})

;; The set of all sets that do not include themselves (including the empty set)
(def universal-set (UniversalSetSpec.))

(declare not not-spec? not-spec>inner-spec - and-spec? and-spec>args val|by-class?)

(defn- create-logical-spec|inner [args' s kind comparison-denotes-supersession?]
  #_(prl! "")
  (let [without-superseded-args
          (->> args'
               (uc/map+    (juxt identity #(compare s %)))
               ;; remove all args whose extensions are superseded by `s`
               (uc/remove+ (fn-> second comparison-denotes-supersession?))
               join) ; TODO elide `join`
        ;_ (prl! without-superseded-args)
        s-redundant? (->> without-superseded-args (seq-or (fn-> second (c/= 0))))]
    (ifs s-redundant?                     args'
         (empty? without-superseded-args) [s]
         (let [{:keys [conj-s? prefer-orig-args? s' specs]}
               (->> without-superseded-args
                    (educe
                      (fn ([accum] accum)
                          ([{:as accum :keys [conj-s? prefer-orig-args? s' specs]} [s* c*]]
                            #_(prl! kind conj-s? prefer-orig-args? s' specs s* c*)
                            (case kind
                              :and (if       ;; Disjointness: the extension of this arg is disjoint w.r.t. that of
                                             ;;               at least one other arg
                                       (c/or (c/= c* 3)
                                             ;; Contradiction/empty-set: (& A (! A))
                                             (if (not-spec? s')
                                                 ;; compare not-spec to all others
                                                 (= (not-spec>inner-spec s') s*)
                                                 ;; compare spec to all not-specs
                                                 (c/and (not-spec? s*) (= s' (not-spec>inner-spec s*)))))
                                       (do (println "BRANCH 1")
                                           (reduced (assoc accum :conj-s? false :specs [empty-set])))
                                       (do (println "BRANCH 2")
                                           (let [conj-s?' (if ;; `s` must be `><` w.r.t. to all other args if it is to be `conj`ed
                                                              (c/not= c* 2)
                                                              false
                                                              conj-s?)
                                                 ;; TODO might similar logic extend to `:or` as well?
                                                 ss* (if (not-spec? s')
                                                         (let [diff (- s* (not s'))]
                                                           (if (and-spec? diff)
                                                               ;; preserve inner expansion
                                                               (and-spec>args diff)
                                                               [diff]))
                                                         [s*])]
                                             (assoc accum :conj-s? conj-s?' :specs (into specs ss*)))))
                              :or  (if-not
                                     ;; `s` must be either `><` or `<>` w.r.t. to all other args
                                     (case c* (2 3) true false)
                                     (reduced (assoc accum :prefer-orig-args? true))
                                     (assoc accum :specs (conj specs s*))))))
                      {:conj-s?            ;; If `s` is a `NotSpec`, and kind is `:and`, then it will be
                                           ;; applied by being `-` from all args, not by being `conj`ed
                                           (c/not (c/and (c/= kind :and) (not-spec? s)))
                       :prefer-orig-args? false
                       :s'                s
                       :specs             []}))]
           (if prefer-orig-args?
               args'
               (whenp-> specs conj-s? (conj s')))))))

(defn- create-logical-spec
  [kind #_#{:or :and} construct-fn spec-pred spec>args args #_(fn-> count (> 1)) comparison-denotes-supersession?]
  (if (-> args count (c/= 1))
      (first args)
      (let [;; simplification via inner expansion ; `(| (| a b) c)` -> `(| a b c)`
            simp|expansion
              (->> args
                   (uc/map+ (fn [arg] (if (spec-pred arg)
                                          (spec>args arg)
                                          [arg])))
                   uc/cat+)
            ;; simplification via structural identity ; `(| a b a)` -> `(| a b)`
            simp|identity+ (->> simp|expansion (uc/map+ >spec) uc/distinct+)
            ;; simplification via intension comparison
            simplified
              (->> simp|identity+
                   (educe
                     (fn ([args'] args')
                         ([args' s]
                           #_(prl! kind args' s)
                           (if (empty? args')
                               (conj args' s)
                               (create-logical-spec|inner args' s kind comparison-denotes-supersession?))))
                     []))]
        (assert (-> simplified count (c/>= 1))) ; for internal implementation correctness
        (if (-> simplified count (c/= 1))
            (first simplified)
            (construct-fn simplified (atom nil))))))

;; ===== AND ===== ;;

(udt/deftype AndSpec [args #_(t/and t/indexed? (t/seq spec?)) *logical-complement]
  {PSpec                 nil
   PLogicalComplement    {>logical-complement
                           ([this] (c/or @*logical-complement (reset! *logical-complement (not this))))}
   fipp.ednize/IOverride nil
   fipp.ednize/IEdn      {-edn ([this] (list* `and args))}
   ?Fn                   {invoke ([_ x] (reduce (fn [_ pred] (c/or (pred x) (reduced false)))
                                          true ; vacuously
                                          args))}
   ?Object               ;; Tests for structural equivalence
                         {equals ([this that]
                                   (c/or (== this that)
                                         (c/and (instance? AndSpec that)
                                                (c/= args (.-args ^AndSpec that)))))}})

(defns and-spec? [x _] (instance? AndSpec x))

(defns and-spec>args [x and-spec?] (.-args ^AndSpec x))

(defn and
  "Sequential/ordered `and`. Analogous to `set/intersection`.
   Applies as much 'compression'/deduplication/simplification as possible to the supplied specs.
   Effectively computes the intersection of the extension of the ->`args`."
  [arg & args]
  (create-logical-spec :and ->AndSpec and-spec? and-spec>args (cons arg args) (fn1 c/= -1)))

(uvar/defalias & and)

;; ===== OR ===== ;;

(udt/deftype OrSpec [args #_(t/and t/indexed? (t/seq spec?)) *logical-complement]
  {PSpec                 nil
   PLogicalComplement    {>logical-complement
                           ([this] (c/or @*logical-complement (reset! *logical-complement (not this))))}
   fipp.ednize/IOverride nil
   fipp.ednize/IEdn      {-edn ([this] (list* `or args))}
   ?Fn                   {invoke ([_ x] (reduce (fn [_ pred] (let [p (pred x)] (c/and p (reduced p))))
                                          true ; vacuously
                                          args))}
   ?Object               ;; Tests for structural equivalence
                         {equals ([this that]
                                   (c/or (== this that)
                                         (c/and (instance? OrSpec that)
                                                (c/= args (.-args ^OrSpec that)))))}})

(defns or-spec? [x _] (instance? OrSpec x))

(defns or-spec>args [x or-spec?] (.-args ^OrSpec x))

(defn or
  "Sequential/ordered `or`. Analogous to `set/union`.
   Applies as much 'compression'/deduplication/simplification as possible to the supplied specs.
   Effectively computes the union of the extension of the ->`args`."
  [arg & args]
  (create-logical-spec :or ->OrSpec or-spec? or-spec>args (cons arg args) (fn1 c/= 1)))

(uvar/defalias | or)

;; ===== OR ===== ;;

(udt/deftype NotSpec [spec #_t/spec?]
  {PSpec                 nil
   fipp.ednize/IOverride nil
   fipp.ednize/IEdn      {-edn   ([this] (list `not spec))}
   ?Fn                   {invoke ([_ x] (spec x))}
   ?Object               ;; Tests for structural equivalence
                         {equals ([this that]
                                   (c/or (== this that)
                                         (c/and (instance? NotSpec that)
                                                (c/= spec (.-spec ^NotSpec that)))))}})

(defns not-spec? [x _] (instance? NotSpec x))

(defns not-spec>inner-spec [spec not-spec?] (.-spec ^NotSpec spec))

(declare nil? val?)

(defns not [spec spec?]
  (ifs (= spec universal-set) empty-set
       (= spec empty-set)     universal-set
       (= spec val|by-class?) nil?
       (not-spec? spec)       (not-spec>inner-spec spec)
       ;; DeMorgan's Law
       (or-spec?  spec)       (->> spec or-spec>args  (uc/lmap not) (apply and))
       ;; DeMorgan's Law
       (and-spec? spec)       (->> spec and-spec>args (uc/lmap not) (apply or ))
       (NotSpec. spec)))

(uvar/defalias ! not)

(defns -
  "Computes the difference of `s0` from `s1`: (& s0 (! s1))
   If `s0` =       `s1`, `∅`
   If `s0` <       `s1`, `∅`
   If `s0` <>      `s1`, `s0`
   If `s0` > | ><  `s1`, `s0` with all elements of `s1` removed"
  [s0 spec?, s1 spec?]
  #_(prl! s0 s1)
  (let [c (compare s0 s1)]
    (case c
      (0 -1) empty-set
       3     s0
      (1 2)
        (let [c0 (c/class s0) c1 (c/class s1)]
          ;; TODO add dispatch?
          (condp == c0
            NotSpec (condp == (-> s0 not-spec>inner-spec c/class)
                      ClassSpec (condp == c1
                                  ClassSpec (AndSpec. [s0 (not s1)] (atom nil)))
                      ValueSpec (condp == c1
                                  ValueSpec (AndSpec. [s0 (not s1)] (atom nil))))
            OrSpec  (condp == c1
                      ClassSpec (let [args (->> s0 or-spec>args (uc/remove (fn1 = s1)))]
                                  (case (count args)
                                    0 empty-set
                                    1 (first args)
                                    (OrSpec. args (atom nil))))))))))

#_(udt/deftype SequentialSpec)

(defns of
  "Creates a spec that ... TODO"
  [pred (<= iterable?), spec spec?] (TODO))

(udt/deftype FnSpec
  [name   #_(t/? t/symbol?)
   lookup #_(t/map-of t/integer?
                      (t/or (spec spec? "output-spec")
                            (t/vec-of (t/tuple (spec spec? "input-spec")
                                               (spec spec? "output-spec")))))
   spec   #_spec?
   meta]
  {PSpec nil
   ;; Outputs whether the args match any input spec
   ?Fn   {invoke    ([this args]
                      (if-let [arity-specs (get lookup (count args))]
                        (->> arity-specs (uc/map+ first) (seq-or #(% args)))
                        false))}
   ?Meta {meta      ([this] meta)
          with-meta ([this meta'] (FnSpec. name lookup spec meta'))}
   fipp.ednize/IOverride nil
   fipp.ednize/IEdn
     {-edn ([this] (list `fn name lookup))}})

(defns fn-spec? [x _] (instance? FnSpec x))

(defns fn|args>out-spec
  "Returns nil if args do not match any input spec"
  [^FnSpec spec fn-spec?, args _]
  (when-let [spec-or-arity-specs (get (.-lookup spec) (count args))]
    (if (spec? spec-or-arity-specs)
        spec-or-arity-specs
        (->> spec-or-arity-specs (uc/filter+ #((first %) args)) uc/first second))))

(defns fn-spec
  [name-  (? symbol?)
   lookup _ #_(t/map-of t/integer?
                      (t/or (spec spec? "output-spec")
                            (t/vec-of (t/tuple (t/vec-of (spec spec? "input-spec"))
                                               (spec spec? "output-spec")))))]
  (let [spec (->> lookup vals
                  (uc/map+ (fn [spec-or-arity-specs]
                             (if (spec? spec-or-arity-specs)
                                 spec-or-arity-specs
                                 (->> spec-or-arity-specs (map ))))))]
    (FnSpec. name- lookup spec nil)))

(deftype FnConstantlySpec
  [name         #_(t/? t/symbol?)
   f            #_t/fn?
   inner-object #_t/_]
  PSpec
  fipp.ednize/IOverride
  fipp.ednize/IEdn
    (-edn [this] (c/or name (list `fn' inner-object))))

#?(:clj
(defmacro fn' [x]
  `(let [x# ~x] (FnConstantlySpec. nil (ufn/fn' x#) x#))))

(defn unkeyed
  "Creates an unkeyed collection spec, in which the collection may
   or may not be sequential or even seqable, but must not have key-value
   pairs like a map.
   Examples of unkeyed collections include a vector (despite its associativity),
   a list, and a set (despite its values doubling as keys).
   A map is not an unkeyed collection."
  [x] (TODO))

(defn ?
  "Denotes type inference should be performed.
   Arity 1: Computes a spec denoting a nilable value satisfying `spec`.
   Arity 2: Computes whether `x` is nil or satisfies `spec`."
  ([x] (or nil? (>spec x)))
  ([spec x] (c/or (c/nil? x) (spec x))))

;; This sadly gets a java.lang.AbstractMethodError when one tries to do as simple as:
;; `(def ? (InferSpec. nil))`
;; `(def abcde (? 1))
(udt/deftype InferSpec [meta #_(t/? ::meta)]
  {PSpec nil
   ?Fn {invoke (([this x] (? x))
                ([this spec x] (? spec x)))}
   ?Meta {meta      ([this] meta)
          with-meta ([this meta'] (InferSpec. meta'))}
   fipp.ednize/IOverride nil
   fipp.ednize/IEdn
     {-edn ([this] `?)}})

(defns infer? [x _] (instance? InferSpec x))

;; ===== Comparison ===== ;;

(def ^:const <ident  -1)
(def ^:const =ident   0)
(def ^:const >ident   1)
(def ^:const ><ident  2)
(def ^:const <>ident  3)

(def- fn<  (ufn/fn' -1))
(def- fn=  (ufn/fn'  0))
(def- fn>  (ufn/fn'  1))
(def- fn>< (ufn/fn'  2))
(def- fn<> (ufn/fn'  3))

(defns- compare|todo [s0 spec?, s1 spec?]
  (err! "TODO dispatch" {:s0 s0 :s0|type (type s0)
                         :s1 s1 :s1|type (type s1)}))

;; ----- UniversalSet ----- ;;

(def- compare|universal+empty    fn>)

(defns- compare|universal+not [s0 spec?, s1 spec?]
  (let [s1|inner (not-spec>inner-spec s1)]
    (ifs (= s1|inner universal-set) 1
         (= s1|inner empty-set)     0
         (compare s0 s1|inner))))

(def- compare|universal+or       fn>)
(def- compare|universal+and      fn>)
(def- compare|universal+infer    compare|todo)
(def- compare|universal+expr     compare|todo)
(def- compare|universal+protocol fn>)
(def- compare|universal+class    fn>)
(def- compare|universal+value    fn>)

;; ----- EmptySet ----- ;;

(defns- compare|empty+not [s0 spec?, s1 spec?]
  (let [s1|inner (not-spec>inner-spec s1)]
    (if (= s1|inner universal-set)
         0
        -1)))

(def- compare|empty+or            fn<)
(def- compare|empty+and           fn<)
(def- compare|empty+infer         compare|todo)
(def- compare|empty+expr          compare|todo)
(def- compare|empty+protocol      fn<)
(def- compare|empty+class         fn<)
(def- compare|empty+value         fn<)

;; ----- NotSpec ----- ;;

(defns- compare|not+not [s0 spec?, s1 spec?]
  (let [c (compare (not-spec>inner-spec s0) (not-spec>inner-spec s1))]
    (case c
      0  0
     -1  1
      1 -1
      2  2
      3  2)))

(defns- compare|not+or [s0 spec?, s1 spec?]
  (compare (not-spec>inner-spec s0) (>logical-complement s1)))

(defns- compare|not+and [s0 spec?, s1 spec?]
  (compare (not-spec>inner-spec s0) (>logical-complement s1)))

(defns- compare|not+protocol [s0 spec?, s1 spec?]
  (let [s0|inner (not-spec>inner-spec s0)]
    (if (= s0|inner empty-set) 1 3)))

(defns- compare|not+class [s0 spec?, s1 spec?]
  (let [s0|inner (not-spec>inner-spec s0)]
    (if (= s0|inner empty-set)
        1
        (case (compare s0|inner s1)
          ( 1 0) 3
          (-1 2) 2
          3      1))))

(defns- compare|not+value [s0 spec?, s1 spec?]
  (let [s0|inner (not-spec>inner-spec s0)]
    (if (= s0|inner empty-set)
        1
        ;; nothing is ever < ValueSpec (and therefore never ><)
        (case (compare s0|inner s1)
          (1 0) 3
          3     1))))

;; ----- OrSpec ----- ;;

;; TODO performance can be improved here by doing fewer comparisons
(defns- compare|or+or [^OrSpec s0 or-spec?, ^OrSpec s1 or-spec?]
  (let [l (->> s0 .-args (seq-and (fn1 < s1)))
        r (->> s1 .-args (seq-and (fn1 < s0)))]
    (if l
        (if r 0 -1)
        (if r
            1
            (if (->> s0 .-args (seq-and (fn1 <> s1)))
                3
                2)))))

(defns- compare|or+and [^OrSpec s0 or-spec?, ^OrSpec s1 or-spec?]
  (let [r (->> s1 .-args (seq-and (fn1 < s0)))]
    (if r 1 3)))

;; TODO transition to `compare|or+class` when stable
(defn- compare|class+or [s0 ^OrSpec s1]
  (let [specs (.-args s1)]
    (first
      (reduce
        (fn [[ret found] s]
          (let [ret'   (compare s0 s)
                found' (-> found (ubit/conj ret') c/long)]
            (ifs (c/or (ubit/contains? found' <ident)
                       (ubit/contains? found' =ident))
                 (reduced [-1 found'])

                 (c/or (ubit/contains? found' ><ident)
                       (c/and (ubit/contains? found' >ident)
                              (ubit/contains? found' <>ident)))
                 [2 found']

                 [ret' found'])))
        [3 ubit/empty]
        specs))))

;; TODO transition to `compare|or+value` when stable
(defn- compare|value+or [s0 ^OrSpec s1]
  (let [specs (.-args s1)]
    (reduce
       (fn [ret s]
         (let [ret' (compare s0 s)] ; `1` will never happen
           (when (c/= ret' 2) (TODO))
           (if (c/or (c/= ret' -1)
                     (c/and (c/= ret' 3) (c/= ret 0))
                     (c/and (c/= ret' 0) (c/= ret 3)))
               ;; because the extension of `s1` only gets bigger
               (reduced -1)
               ret)))
       3
       specs)))

;; ----- AndSpec ----- ;;

(defn- compare|and+and [^AndSpec s0 ^AndSpec s1]
  (TODO))

(defn- compare|class+and [s0 ^AndSpec s1]
  (let [specs (.-args s1)]
    (first
      (reduce
        (fn [[ret found] s]
          (let [c (compare s0 s)]
            (if (c/= c 0)
                (reduced [1 nil])
                (let [found' (-> found (ubit/conj c) c/long)
                      ret'   (ifs (ubit/contains? found' ><ident)
                                  (if (c/= found' (-> (ubit/conj ><ident) (ubit/conj <>ident)))
                                      3
                                      2)

                                  (ubit/contains? found' <>ident)
                                  (ifs (ubit/contains? found' <ident) 3
                                       (ubit/contains? found' >ident) 1
                                       c)

                                  c)]
                  [ret' found']))))
        [3 ubit/empty]
        specs))))

(defn- compare|value+and [s0 ^AndSpec s1]
  (let [specs (.-args s1)]
    (reduce
      (fn [ret s]
        (let [ret' (compare s0 s)]
          (if (c/= ret' 3)
              (reduced 3)
              ret')))
      3
      specs)))

;; ----- InferSpec ----- ;;

;; ----- Expression ----- ;;

(defn- compare|expr+expr [s0 s1] (if (c/= s0 s1) 0 3))

(def- compare|expr+value fn<>)

;; ----- ProtocolSpec ----- ;;

;; TODO transition to `compare|protocol+value` when stable
(defn- compare|value+protocol [s0 s1]
  (let [v (value-spec>value       s0)
        p (protocol-spec>protocol s1)]
    (if (satisfies? p v) -1 3)))

;; ----- ClassSpec ----- ;;

(defn- compare|class+value [s0 s1]
  (let [c (class-spec>class s0)
        v (value-spec>value s1)]
    (if (instance? c v) 1 3)))

;; ----- ValueSpec ----- ;;

(defn- compare|value+value
  "What we'd really like is to have a different version of .equals or .equiv
   like .equivBehavior in which it returns whether any behavior is different
   whatsoever between two objects. For instance, `[52]` behaves differently from
   `(list 52)` because `(get [52] 0)` -> `52` while `(get (list 52) 0)` -> `nil`.

   The issue with this is that yes, one could implement a `strict=` that tries to
   emulate this behavior, but even though it is implementable for 'transparent'
   objects such as collections, it is not for 'opaque' objects, which would
   potentially have to have custom equality behavior per class. So we will simply
   reluctantly accept whatever `=` tells us as well as the fallout that results.
   Thus, `(t/or (t/value []) (t/value (list)))` will result in `(t/value [])`,
   which is not ideal but both feasible and better than the alternative."
  [s0 s1]
  (if (c/= (value-spec>value s0)
           (value-spec>value s1))
      0
      3))

;; ----- Dispatch ----- ;;

(def- compare|dispatch
  (let [inverted (fn [f] (fn [s0 s1] (inverse (f s1 s0))))]
    {UniversalSetSpec
       {UniversalSetSpec fn=
        EmptySetSpec     compare|universal+empty
        NotSpec          compare|universal+not
        OrSpec           compare|universal+or
        AndSpec          compare|universal+and
        InferSpec        compare|universal+infer
        Expression       compare|universal+expr
        ProtocolSpec     compare|universal+protocol
        ClassSpec        compare|universal+class
        ValueSpec        compare|universal+value}
     EmptySetSpec
       {UniversalSetSpec (inverted compare|universal+empty)
        EmptySetSpec     fn=
        NotSpec          compare|empty+not
        OrSpec           compare|empty+or
        AndSpec          compare|empty+and
        InferSpec        compare|empty+infer
        Expression       compare|empty+expr
        ProtocolSpec     compare|empty+protocol
        ClassSpec        compare|empty+class
        ValueSpec        compare|empty+value}
     NotSpec
       {UniversalSetSpec (inverted compare|universal+not)
        EmptySetSpec     (inverted compare|empty+not)
        NotSpec          compare|not+not
        OrSpec           compare|not+or
        AndSpec          compare|not+and
        InferSpec        compare|todo
        Expression       fn<>
        ProtocolSpec     compare|not+protocol
        ClassSpec        compare|not+class
        ValueSpec        compare|not+value}
     OrSpec
       {UniversalSetSpec (inverted compare|universal+or)
        EmptySetSpec     (inverted compare|empty+or)
        NotSpec          (inverted compare|not+or)
        OrSpec           compare|or+or
        AndSpec          compare|or+and
        InferSpec        compare|todo
        Expression       fn<>
        ProtocolSpec     compare|todo
        ClassSpec        (inverted compare|class+or)
        ValueSpec        (inverted compare|value+or)}
     AndSpec
       {UniversalSetSpec (inverted compare|universal+and)
        EmptySetSpec     (inverted compare|empty+and)
        NotSpec          compare|todo
        OrSpec           (inverted compare|or+and)
        AndSpec          compare|and+and
        InferSpec        compare|todo
        Expression       fn<>
        ProtocolSpec     compare|todo
        ClassSpec        (inverted compare|class+and)
        ValueSpec        (inverted compare|value+and)}
     ;; TODO review this
     InferSpec
       {UniversalSetSpec (inverted compare|universal+infer)
        EmptySetSpec     (inverted compare|empty+infer)
        NotSpec          compare|todo #_fn>
        OrSpec           compare|todo #_fn>
        AndSpec          compare|todo #_fn>
        InferSpec        compare|todo #_fn=
        Expression       compare|todo #_fn>
        ProtocolSpec     compare|todo #_fn>
        ClassSpec        compare|todo #_fn>
        ValueSpec        compare|todo #_fn>}
     ;; TODO review this
     Expression
       {UniversalSetSpec (inverted compare|universal+expr)
        EmptySetSpec     (inverted compare|empty+expr)
        NotSpec          compare|todo
        OrSpec           compare|todo
        AndSpec          compare|todo
        InferSpec        compare|todo
        Expression       compare|expr+expr
        ProtocolSpec     compare|todo
        ClassSpec        fn<> ; TODO not entirely true
        ValueSpec        compare|expr+value}
     ProtocolSpec
       {UniversalSetSpec (inverted compare|universal+protocol)
        EmptySetSpec     (inverted compare|empty+protocol)
        NotSpec          (inverted compare|not+protocol)
        OrSpec           compare|todo
        AndSpec          compare|todo
        InferSpec        fn<
        Expression       fn<>
        ProtocolSpec     (fn [s0 s1] (if (identical? (protocol-spec>protocol s0)
                                                     (protocol-spec>protocol s1))
                                         0
                                         3))
        ClassSpec        compare|todo
        ValueSpec        (inverted compare|value+protocol)}
     ClassSpec
       {UniversalSetSpec (inverted compare|universal+class)
        EmptySetSpec     (inverted compare|empty+class)
        NotSpec          (inverted compare|not+class)
        OrSpec           compare|class+or
        AndSpec          compare|class+and
        InferSpec        compare|todo
        Expression       fn<>
        ProtocolSpec     compare|todo
        ClassSpec        (fn [s0 s1] (compare|class|class* (class-spec>class s0) (class-spec>class s1)))
        ValueSpec        compare|class+value}
     ValueSpec
       {UniversalSetSpec (inverted compare|universal+value)
        EmptySetSpec     (inverted compare|empty+value)
        NotSpec          (inverted compare|not+value)
        OrSpec           compare|value+or
        AndSpec          compare|value+and
        InferSpec        compare|todo
        Expression       (inverted compare|expr+value)
        ProtocolSpec     compare|value+protocol
        ClassSpec        (inverted compare|class+value)
        ValueSpec        compare|value+value}}))

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

(defns- -spec>classes [spec spec?, classes set? > set?]
  (cond (class-spec? spec)
          (conj classes (class-spec>class spec))
        (value-spec? spec)
          (conj classes (value-spec>value spec))
        (c/= spec universal-set)
          #?(:clj  #{nil java.lang.Object}
             :cljs (TODO "Not sure what to do in the case of universal CLJS set"))
        (c/= spec empty-set)
          #{}
        (and-spec? spec)
          (reduce (fn [classes' spec'] (-spec>classes spec' classes'))
            classes (and-spec>args spec))
        (or-spec? spec)
          (reduce (fn [classes' spec'] (-spec>classes spec' classes'))
            classes (or-spec>args spec))
        :else
          (err! "Not sure how to handle spec" spec)))

(defns spec>classes
  "Outputs the set of all the classes ->`spec` can embody according to its various conditional branches,
   if any. Ignores nils, treating in Clojure simply as a `java.lang.Object`."
  [spec spec? > set?] (-spec>classes spec #{}))

#?(:clj
(defn- -spec>?class-value [spec spec-nilable?]
  (if (value-spec? spec)
      (let [v (value-spec>value spec)]
        (when (c/class? v) {:class v :nilable? spec-nilable?}))
      nil)))

#?(:clj
(defn spec>?class-value
  "Outputs the single class value embodied by ->`spec`.
   If a spec is extensionally equal the *value* of a class, outputs that class.

   However, if a spec does not embody the value of a class but rather merely embodies (as all specs)
   an extensional subset of the set of all objects conforming to a class, outputs nil."
  {:examples `{(spec>?class-value (value String)) {:class String :nilable? false}
               (spec>?class-value (isa? String))  nil}}
  [spec] (-spec>?class-value spec false)))

;; ---------------------- ;;
;; ===== Predicates ===== ;;
;; ---------------------- ;;

        (def basic-type-syms '[boolean byte char short int long float double ref])

#?(:clj (defns- >v-sym [prefix symbol?, kind symbol?] (symbol (str prefix "|" kind "?"))))

#?(:clj (defns- >kv-sym [prefix symbol?, from-type symbol?, to-type symbol?]
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

;; ===== General ===== ;;

         (-def none?         empty-set)
         (-def any?          universal-set)

         (-def nil?          (value nil))
         (-def object?       (isa? #?(:clj java.lang.Object :cljs js/Object)))

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

         (-def primitive? (or boolean? #?@(:clj [byte? char? short? int? long? float?]) double?))

#_(:clj  (-def comparable-primitive? (and primitive? (not boolean?))))

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

       #_(-def numerically-byte?           (and integer-value? (>expr (fn [x] (c/<= -128                 x 127)))))
       #_(-def numerically-short?          (and integer-value? (>expr (fn [x] (c/<= -32768               x 32767)))))
       #_(-def numerically-char?           (and integer-value? (>expr (fn [x] (c/<=  0                   x 65535)))))
       #_(-def numerically-unsigned-short? numerically-char?)
       #_(-def numerically-int?            (and integer-value? (>expr (fn [x] (c/<= -2147483648          x 2147483647)))))
       #_(-def numerically-long?           (and integer-value? (>expr (fn [x] (c/<= -9223372036854775808 x 9223372036854775807)))))
       #_(-def numerically-float?          (and number?
                                                (>expr (fn [x] (c/<= -3.4028235E38 x 3.4028235E38)))
                                                (>expr (fn [x] (-> x #?(:clj clojure.lang.RT/floatCast :cljs c/float) (c/== x))))))
       #_(-def numerically-double?         (and number?
                                                (>expr (fn [x] (c/<= -1.7976931348623157E308 x 1.7976931348623157E308)))
                                                (>expr (fn [x] (-> x clojure.lang.RT/doubleCast (c/== x))))))

       #_(-def int-like?                   (and integer-value? numerically-int?))

#_(defn numerically
  [spec]
  (assert (instance? ClassSpec spec))
  (let [c (.-c ^ClassSpec spec)]
    (case (.getName ^Class c)
      "java.lang.Byte"      numerically-byte?
      "java.lang.Short"     numerically-short?
      "java.lang.Character" numerically-char?
      "java.lang.Integer"   numerically-int?
      "java.lang.Long"      numerically-long?
      "java.lang.Float"     numerically-float?
      ;; TODO fix
      ;;"java.lang.Double"    numerically-double?
      (err! "Could not find numerical range spec for class" {:c c}))))

;; ========== Collections ========== ;;

;; ===== Tuples ===== ;;

         (-def tuple?           ;; clojure.lang.Tuple was discontinued; we won't support it for now
                                (isa? quantum.untyped.core.data.tuple.Tuple))
#?(:clj  (-def map-entry?       (isa? java.util.Map$Entry)))

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
         (-def !list?           #?(:clj (isa? java.util.LinkedList)))
         (-def  list?           #?(:clj  (isa? java.util.List)
                                   :cljs +list?))

;; ----- Generic ----- ;;

;; ===== Arrays ===== ;; Sequential, Associative (specifically, whose keys are sequential,
                      ;; dense integer values), not extensible

#?(:clj
(defns >array-nd-type [kind symbol?, n (s/and integer? pos?) > class-spec?]
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
(defn >array-nd-types [n]
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

;; ----- Hash Maps ----- ;;

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
                                                     :cljs [(isa? goog.structs.Map)])))

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

         (-def   !unsorted-map|double->boolean?  (or !hash-map|double->boolean?   !array-map|double->boolean?))
         (-def   !unsorted-map|double->byte?     (or !hash-map|double->byte?      !array-map|double->byte?))
         (-def   !unsorted-map|double->char?     (or !hash-map|double->char?      !array-map|double->char?))
         (-def   !unsorted-map|double->short?    (or !hash-map|double->short?     !array-map|double->short?))
         (-def   !unsorted-map|double->int?      (or !hash-map|double->int?       !array-map|double->int?))
         (-def   !unsorted-map|double->long?     (or !hash-map|double->long?      !array-map|double->long?))
         (-def   !unsorted-map|double->float?    (or !hash-map|double->float?     !array-map|double->float?))
         (-def   !unsorted-map|double->double?   (or !hash-map|double->double?    !array-map|double->double?))
         (-def   !unsorted-map|double->ref?      (or !hash-map|double->ref?       !array-map|double->ref?))

         (-def   !unsorted-map|ref->boolean?     (or !hash-map|ref->boolean?      !array-map|ref->boolean?))
         (-def   !unsorted-map|ref->byte?        (or !hash-map|ref->byte?         !array-map|ref->byte?))
         (-def   !unsorted-map|ref->char?        (or !hash-map|ref->char?         !array-map|ref->char?))
         (-def   !unsorted-map|ref->short?       (or !hash-map|ref->short?        !array-map|ref->short?))
         (-def   !unsorted-map|ref->int?         (or !hash-map|ref->int?          !array-map|ref->int?))
         (-def   !unsorted-map|ref->long?        (or !hash-map|ref->long?         !array-map|ref->long?))
         (-def   !unsorted-map|ref->float?       (or !hash-map|ref->float?        !array-map|ref->float?))
         (-def   !unsorted-map|ref->double?      (or !hash-map|ref->double?       !array-map|ref->double?))
         (-def   !unsorted-map|ref->ref?         (or !hash-map|ref->ref?          !array-map|ref->ref?))

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

         (-def   !sorted-map|byte->boolean?      #?(:clj (isa? it.unimi.dsi.fastutil.bytes.Byte2BooleanSortedMap)          :cljs none?))
         (-def   !sorted-map|byte->byte?         #?(:clj (isa? it.unimi.dsi.fastutil.bytes.Byte2ByteSortedMap)             :cljs none?))
         (-def   !sorted-map|byte->char?         #?(:clj (isa? it.unimi.dsi.fastutil.bytes.Byte2CharSortedMap)             :cljs none?))
         (-def   !sorted-map|byte->short?        #?(:clj (isa? it.unimi.dsi.fastutil.bytes.Byte2ShortSortedMap)            :cljs none?))
         (-def   !sorted-map|byte->int?          #?(:clj (isa? it.unimi.dsi.fastutil.bytes.Byte2IntSortedMap)              :cljs none?))
         (-def   !sorted-map|byte->long?         #?(:clj (isa? it.unimi.dsi.fastutil.bytes.Byte2LongSortedMap)             :cljs none?))
         (-def   !sorted-map|byte->float?        #?(:clj (isa? it.unimi.dsi.fastutil.bytes.Byte2FloatSortedMap)            :cljs none?))
         (-def   !sorted-map|byte->double?       #?(:clj (isa? it.unimi.dsi.fastutil.bytes.Byte2DoubleSortedMap)           :cljs none?))
         (-def   !sorted-map|byte->ref?          #?(:clj (isa? it.unimi.dsi.fastutil.bytes.Byte2ReferenceSortedMap)        :cljs none?))

         (-def   !sorted-map|char->ref?          #?(:clj (isa? it.unimi.dsi.fastutil.chars.Char2ReferenceSortedMap)        :cljs none?))
         (-def   !sorted-map|char->boolean?      #?(:clj (isa? it.unimi.dsi.fastutil.chars.Char2BooleanSortedMap)          :cljs none?))
         (-def   !sorted-map|char->byte?         #?(:clj (isa? it.unimi.dsi.fastutil.chars.Char2ByteSortedMap)             :cljs none?))
         (-def   !sorted-map|char->char?         #?(:clj (isa? it.unimi.dsi.fastutil.chars.Char2CharSortedMap)             :cljs none?))
         (-def   !sorted-map|char->short?        #?(:clj (isa? it.unimi.dsi.fastutil.chars.Char2ShortSortedMap)            :cljs none?))
         (-def   !sorted-map|char->int?          #?(:clj (isa? it.unimi.dsi.fastutil.chars.Char2IntSortedMap)              :cljs none?))
         (-def   !sorted-map|char->long?         #?(:clj (isa? it.unimi.dsi.fastutil.chars.Char2LongSortedMap)             :cljs none?))
         (-def   !sorted-map|char->float?        #?(:clj (isa? it.unimi.dsi.fastutil.chars.Char2FloatSortedMap)            :cljs none?))
         (-def   !sorted-map|char->double?       #?(:clj (isa? it.unimi.dsi.fastutil.chars.Char2DoubleSortedMap)           :cljs none?))

         (-def   !sorted-map|short->boolean?     #?(:clj (isa? it.unimi.dsi.fastutil.shorts.Short2BooleanSortedMap)        :cljs none?))
         (-def   !sorted-map|short->byte?        #?(:clj (isa? it.unimi.dsi.fastutil.shorts.Short2ByteSortedMap)           :cljs none?))
         (-def   !sorted-map|short->char?        #?(:clj (isa? it.unimi.dsi.fastutil.shorts.Short2CharSortedMap)           :cljs none?))
         (-def   !sorted-map|short->short?       #?(:clj (isa? it.unimi.dsi.fastutil.shorts.Short2ShortSortedMap)          :cljs none?))
         (-def   !sorted-map|short->int?         #?(:clj (isa? it.unimi.dsi.fastutil.shorts.Short2IntSortedMap)            :cljs none?))
         (-def   !sorted-map|short->long?        #?(:clj (isa? it.unimi.dsi.fastutil.shorts.Short2LongSortedMap)           :cljs none?))
         (-def   !sorted-map|short->float?       #?(:clj (isa? it.unimi.dsi.fastutil.shorts.Short2FloatSortedMap)          :cljs none?))
         (-def   !sorted-map|short->double?      #?(:clj (isa? it.unimi.dsi.fastutil.shorts.Short2DoubleSortedMap)         :cljs none?))
         (-def   !sorted-map|short->ref?         #?(:clj (isa? it.unimi.dsi.fastutil.shorts.Short2ReferenceSortedMap)      :cljs none?))

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

;; ----- Hash Sets ----- ;;

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
                                                      ;; Because this has different semantics
                                                      #_(isa? java.util.IdentityHashSet)
                                                      (isa? it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet))
                                            :cljs (isa? goog.structs.Set)))

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

#?(:clj  (-def   !sorted-set|byte?       (isa? it.unimi.dsi.fastutil.bytes.ByteSortedSet)))
#?(:clj  (-def   !sorted-set|short?      (isa? it.unimi.dsi.fastutil.shorts.ShortSortedSet)))
#?(:clj  (-def   !sorted-set|char?       (isa? it.unimi.dsi.fastutil.chars.CharSortedSet)))
#?(:clj  (-def   !sorted-set|int?        (isa? it.unimi.dsi.fastutil.ints.IntSortedSet)))
#?(:clj  (-def   !sorted-set|long?       (isa? it.unimi.dsi.fastutil.longs.LongSortedSet)))
#?(:clj  (-def   !sorted-set|float?      (isa? it.unimi.dsi.fastutil.floats.FloatSortedSet)))
#?(:clj  (-def   !sorted-set|double?     (isa? it.unimi.dsi.fastutil.doubles.DoubleSortedSet)))
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

         (-def   !set?                   (or !unsorted-set? !sorted-set?))
#?(:clj  (-def  !!set?                   (or !!unsorted-set? !!sorted-set?)))
         (-def    set?                   (or ?!+set? !set? #?@(:clj [!!set? (isa? java.util.Set)])))

;; ===== Functions ===== ;;

         (-def fn?          (isa? #?(:clj clojure.lang.Fn  :cljs js/Function)))

         (-def ifn?         (isa? #?(:clj clojure.lang.IFn :cljs cljs.core/IFn)))

         (-def fnt?         (and fn? (>expr (fn-> c/meta :spec))))

         (-def multimethod? (isa? #?(:clj clojure.lang.MultiFn :cljs cljs.core/IMultiFn)))

         ;; I.e., can you call/invoke it by being in functor position (first element of an unquoted list)
         ;; within a typed context?
         ;; TODO should we allow java.lang.Runnable, java.util.concurrent.Callable to be `callable?`?
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

#?(:clj  (-def thread?      (isa? java.lang.Thread)))

         ;; Able to be used with `throw`
         (-def throwable?   #?(:clj (isa? java.lang.Throwable) :cljs any?))

         (-def regex?       (isa? #?(:clj java.util.regex.Pattern :cljs js/RegExp)))

         (-def chan?        (isa? #?(:clj  clojure.core.async.impl.protocols/Channel
                                     :cljs cljs.core.async.impl.protocols/Channel)))

         (-def keyword?     (isa? #?(:clj clojure.lang.Keyword :cljs cljs.core/Keyword)))
         (-def symbol?      (isa? #?(:clj clojure.lang.Symbol  :cljs cljs.core/Symbol)))

         ;; `js/File` isn't always available! Use an abstraction
#?(:clj  (-def file?        (isa? java.io.File)))

         (-def comparable?  #?(:clj  (isa? java.lang.Comparable)
                               ;; TODO other things are comparable; really it depends on the two objects in question
                               :cljs (or nil? (isa? cljs.core/IComparable))))

         (-def record?      (isa? #?(:clj clojure.lang.IRecord :cljs cljs.core/IRecord)))

         (-def transformer? (isa? quantum.untyped.core.reducers.Transformer))

;; ----- Collections ----- ;;

         (-def sorted?         #?(:clj  (or (isa? #?(:clj clojure.lang.Sorted :cljs cljs.core/ISorted))
                                            #?@(:clj  [(isa? java.util.SortedMap)
                                                       (isa? java.util.SortedSet)]
                                                :cljs [(isa? goog.structs.AvlTree)])
                                            ;; TODO implement — monotonically <, <=, =, >=, >
                                            #_(>expr monotonic?))))

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

#_(t/def ::form (t/or ::literal t/list? t/vector? ...))

#?(:clj  (-def tagged-literal?   (isa? clojure.lang.TaggedLiteral)))

         (-def literal?          (or nil? boolean? symbol? keyword? string? #?(:clj long?) double? #?(:clj tagged-literal?)))

;; ===== Generic ===== ;;

         ;; Standard "uncuttable" types
         (-def integral?  (or primitive? number?))
