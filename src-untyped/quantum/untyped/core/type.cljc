(ns quantum.untyped.core.type
  "Essentially, set-theoretic definitions and operations on types."
  (:refer-clojure :exclude
    [< <= = not= >= > == compare *
     and or not
     boolean  byte  char  short  int  long  float  double
     boolean? byte? char? short? int? long? float? double?
     isa?
     nil? any? class? tagged-literal? #?(:cljs object?)
     number? decimal? bigdec? integer? ratio?
     keyword? string? symbol?
     meta])
  (:require
    [clojure.core                               :as c]
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
    [quantum.untyped.core.type.core             :as utcore]
    [quantum.untyped.core.type.predicates       :as utpred]
    [quantum.untyped.core.vars                  :as uvar
      :refer [def- update-meta]])
  #?(:clj (:import quantum.untyped.core.analyze.expr.Expression))
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

#_(defn instance? [])

(do

(defonce *spec-registry (atom {}))
(swap! *spec-registry empty)

;; ===== SPECS ===== ;;

(defprotocol PSpec)

(udt/deftype ValueSpec [v]
  {PSpec                 nil
   fipp.ednize/IOverride nil
   fipp.ednize/IEdn      {-edn    ([this] (list `value v))}
   ?Fn                   {invoke  ([_ x] (c/= x v))}
   ?Object               {equals  ([this that]
                                    (c/or (== this that)
                                          (c/and (instance? ValueSpec that)
                                                 (c/= v (.-v ^ValueSpec that)))))}})

(defn value
  "Creates a spec whose extension is the singleton set containing only the value `v`."
  [v] (ValueSpec. v))

(defn value-spec? [x] (instance? ValueSpec x))

(defn value-spec>value [x]
  (if (value-spec? x)
      (.-v ^ValueSpec x)
      (err! "Not a value spec" x)))

;; -----

(udt/deftype ClassSpec
  [meta     #_(t/? ::meta)
   ^Class c #_t/class?
   name     #_(t/? t/symbol?)]
  {PSpec                 nil
   fipp.ednize/IOverride nil
   fipp.ednize/IEdn
     {-edn ([this] (c/or name (list `isa? c)))}
   ?Fn     {invoke    ([_ x] (instance? c x))}
   ?Meta   {meta      ([this] meta)
            with-meta ([this meta'] (ClassSpec. meta' c name))}
   ?Object {equals    ([this that]
                        (c/or (== this that)
                              (c/and (instance? ClassSpec that)
                                     (c/= c (.-c ^ClassSpec that)))))}})

(defn class-spec? [x] (instance? ClassSpec x))

(defn class-spec>class [spec]
  (if (class-spec? spec)
      (.-c ^ClassSpec spec)
      (err! "Cannot cast to ClassSpec" {:x spec})))

(udt/deftype ProtocolSpec
  [meta #_(t/? ::meta)
   p    #_t/protocol?
   name #_(t/? t/symbol?)]
  {PSpec                 nil
   fipp.ednize/IOverride nil
   fipp.ednize/IEdn      {-edn      ([this] (c/or name (list `isa|protocol? p)))}
   ?Fn                   {invoke    ([_ x] (satisfies? p x))}
   ?Meta                 {meta      ([this] meta)
                          with-meta ([this meta'] (ProtocolSpec. meta' p name))}})

(defn protocol-spec? [x] (instance? ProtocolSpec x))

(defn protocol-spec>protocol [spec]
  (if (protocol-spec? spec)
      (.-p ^ProtocolSpec spec)
      (err! "Cannot cast to ProtocolSpec" {:x spec})))

(defn isa?|protocol [p]
  (assert (utpred/protocol? p))
  (ProtocolSpec. nil p nil))

(defn isa? [x]
  (ifs #?@(:clj [(c/class? x) (ClassSpec. nil x nil)])
       (utpred/protocol? x)   (isa?|protocol x)))

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

#?(:clj
(defmacro define [sym specable]
  `(~'def ~sym (>spec ~specable '~(qual/qualify sym)))))

(defn undef [reg sym]
  (if-let [spec (get reg sym)]
    (let [reg' (dissoc reg sym)]
      (if (instance? ClassSpec spec)
          (uc/dissoc-in reg' [:by-class (.-c ^ClassSpec spec)])
          (TODO)))
    reg))

(defn undef! [sym] (swap! *spec-registry undef sym))

#?(:clj
(defmacro defalias [sym spec]
  `(~'def ~sym (>spec ~spec))))

#?(:clj (uvar/defalias -def define))

(-def spec? PSpec)

(defn * [spec]
  (if (spec? spec)
      (update-meta spec assoc :runtime? true)
      (err! "Input must be spec" spec)))

(udt/deftype DeducibleSpec [*spec #_(t/atom-of t/spec?)]
  {PSpec                 nil
   fipp.ednize/IOverride nil
   fipp.ednize/IEdn      {-edn ([this] (list `deducible @*spec))}
   ?Atom                 {swap! (([this f] (swap!  *spec f)))
                          reset! ([this v] (reset! *spec v))}})

(defn deducible [x]
  (if (spec? x)
      (DeducibleSpec. (atom x))
      (err! "`x` must be spec to be part of DeducibleSpec" x)))

(defn deducible-spec? [x] (instance? DeducibleSpec x))

;; ===== EXTENSIONALITY COMPARISON IMPLEMENTATIONS ===== ;;

#_(is (coll&/incremental-every? (aritoid nil (constantly true) t/in>)
        [String Comparable Object])
      (coll&/incremental-every? (aritoid nil (constantly true) t/in>)
        [Long Number]))

(defn compare|class|class*
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
  [^Class c0 ^Class c1]
  #?(:clj (ifs (== c0 c1)                                0
               (== c0 Object)                            1
               (== c1 Object)                           -1
               (== (utcore/boxed->unboxed c0) c1)        1
               (== c0 (utcore/boxed->unboxed c1))       -1
               ;; we'll consider the two unrelated
               ;; TODO this uses reflection so each class comparison is slowish
               (c/or (utcore/primitive-array-type? c0)
                     (utcore/primitive-array-type? c1))  3
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

(defn compare
  ;; TODO optimize the `recur`s here as they re-take old code paths
  "Returns the value of the comparison of the extensions of ->`s0` and ->`s1`.
   `-1` means (ex ->`s0`) ⊂                                 (ex ->`s1`)
    `0` means (ex ->`s0`) =                                 (ex ->`s1`)
    `1` means (ex ->`s0`) ⊃                                 (ex ->`s1`)
    `2` means (ex ->`s0`) shares other intersect w.r.t. (∩) (ex ->`s1`)
    `3` means (ex ->`s0`) disjoint               w.r.t. (∅) (ex ->`s1`)

   Does not compare cardinalities or other relations of sets, but rather only sub/superset
   relations."
  [s0 s1]
  (assert (spec? s0) {:s0 s0})
  (assert (spec? s1) {:s1 s1})
  (let [dispatched (-> compare|dispatch (get (type s0)) (get (type s1)))]
    (if (c/nil? dispatched)
        (err! (str "Specs not handled: " {:s0 s0 :s1 s1}) {:s0 s0 :s1 s1})
        (dispatched s0 s1))))

(defn <
  "Computes whether the extension of spec ->`s0` is a strict subset of that of ->`s1`."
  [s0 s1] (let [ret (compare s0 s1)] (c/= ret -1)))

(defn <=
  "Computes whether the extension of spec ->`s0` is a (lax) subset of that of ->`s1`."
  [s0 s1] (let [ret (compare s0 s1)] (c/or (c/= ret -1) (c/= ret 0))))

(defn =
  "Computes whether the extension of spec ->`s0` is equal to that of ->`s1`."
  [s0 s1] (c/= (compare s0 s1) 0))

(defn not=
  "Computes whether the extension of spec ->`s0` is not equal to that of ->`s1`."
  [s0 s1] (c/not (= s0 s1)))

(defn >=
  "Computes whether the extension of spec ->`s0` is a (lax) superset of that of ->`s1`."
  [s0 s1] (let [ret (compare s0 s1)] (c/or (c/= ret 1) (c/= ret 0))))

(defn >
  "Computes whether the extension of spec ->`s0` is a strict superset of that of ->`s1`."
  [s0 s1] (c/= (compare s0 s1) 1))

(defn ><
  "Computes whether it is the case that the intersect of the extensions of spec ->`s0`
   and ->`s1` is non-empty, and neither ->`s0` nor ->`s1` share a subset/equality/superset
   relationship."
  [s0 s1] (c/= (compare s0 s1) 2))

(defn <>
  "Computes whether the respective extensions of specs ->`s0` and ->`s1` are disjoint."
  [s0 s1] (c/= (compare s0 s1) 3))

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

(-def spec? PSpec)

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

(defn and-spec? [x] (instance? AndSpec x))

(defn and-spec>args [x]
  (if (instance? AndSpec x)
      (.-args ^AndSpec x)
      (err! "Cannot cast to AndSpec" x)))

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

(defn or-spec? [x] (instance? OrSpec x))

(defn or-spec>args [x]
  (if (instance? OrSpec x)
      (.-args ^OrSpec x)
      (err! "Cannot cast to OrSpec" x)))

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

(defn not-spec? [x] (instance? NotSpec x))

(defn not-spec>inner-spec [spec]
  (if (instance? NotSpec spec)
      (.-spec ^NotSpec spec)
      (err! "Cannot cast to NotSpec" {:x spec})))

(declare nil? val?)

(defn not [spec]
  (assert (spec? spec))
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

(defn -
  "Computes the difference of `s0` from `s1` (& A (! B))
   If `s0` =       `s1`, `∅`
   If `s0` <       `s1`, `∅`
   If `s0` <>      `s1`, `s0`
   If `s0` > | ><  `s1`, `s0` with all elements of `s1` removed"
  [s0 #_spec? s1 #_spec?]
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

#?(:clj
(defmacro spec
  "Creates a spec function"
  [arglist & body] ; TODO spec this
  `(FnSpec. nil (fn ~arglist ~@body) (list* `spec '~arglist '~body))))

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

(defn- compare|todo [s0 s1]
  (err! "TODO dispatch" {:s0 s0 :s0|type (type s0)
                         :s1 s1 :s1|type (type s1)}))

;; ----- UniversalSet ----- ;;

(def- compare|universal+empty    fn>)

(defn- compare|universal+not [s0 s1]
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

(defn- compare|empty+not [s0 s1]
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

(defn- compare|not+not [s0 s1]
  (let [c (compare (not-spec>inner-spec s0) (not-spec>inner-spec s1))]
    (case c
      0  0
     -1  1
      1 -1
      2  2
      3  2)))

(defn- compare|not+or [s0 s1]
  (compare (not-spec>inner-spec s0) (>logical-complement s1)))

(defn- compare|not+and [s0 s1]
  (compare (not-spec>inner-spec s0) (>logical-complement s1)))

(defn- compare|not+protocol [s0 s1]
  (let [s0|inner (not-spec>inner-spec s0)]
    (if (= s0|inner empty-set) 1 3)))

(defn- compare|not+class [s0 s1]
  (let [s0|inner (not-spec>inner-spec s0)]
    (if (= s0|inner empty-set)
        1
        (case (compare s0|inner s1)
          ( 1 0) 3
          (-1 2) 2
          3      1))))

(defn- compare|not+value [s0 s1]
  (let [s0|inner (not-spec>inner-spec s0)]
    (if (= s0|inner empty-set)
        1
        ;; nothing is ever < ValueSpec (and therefore never ><)
        (case (compare s0|inner s1)
          (1 0) 3
          3     1))))

;; ----- OrSpec ----- ;;

;; TODO performance can be improved here by doing fewer comparisons
(defn- compare|or+or [^OrSpec s0 ^OrSpec s1]
  (let [l (->> s0 .-args (seq-and (fn1 < s1)))
        r (->> s1 .-args (seq-and (fn1 < s0)))]
    (if l
        (if r 0 -1)
        (if r
            1
            (if (->> s0 .-args (seq-and (fn1 <> s1)))
                3
                2)))))

(defn- compare|or+and [^OrSpec s0 ^AndSpec s1]
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
                 (reduced [-1 nil])

                 (ubit/contains? found' >ident)
                 (reduced [2 nil])

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
        ClassSpec        compare|todo
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



;; ===== GENERAL ===== ;;

         (-def nil?          (value nil))
         (-def object?       (isa? #?(:clj java.lang.Object :cljs js/Object)))
         (-def val|by-class? (or object? #?@(:cljs [js/String js/Symbol])))
         (-def val?          (not nil?))

         (-def none?         empty-set)
         (-def any?          universal-set)

;; ===== PRIMITIVES ===== ;;

         (-def                       boolean?  (isa? #?(:clj Boolean :cljs js/Boolean)))
         (-def                       ?boolean? (? boolean?))

#?(:clj  (-def                       byte?     (isa? Byte)))
#?(:clj  (-def                       ?byte?    (? byte?)))

#?(:clj  (-def                       char?     (isa? Character)))
#?(:clj  (-def                       ?char?    (? char?)))

#?(:clj  (-def                       short?    (isa? Short)))
#?(:clj  (-def                       ?short?   (? short?)))

#?(:clj  (-def                       int?      (isa? Integer)))
#?(:clj  (-def                       ?int?     (? int?)))

#?(:clj  (-def                       long?     (isa? Long)))
#?(:clj  (-def                       ?long?    (? long?)))

#?(:clj  (-def                       float?    (isa? Float)))
#?(:clj  (-def                       ?float?   (? float?)))

         (-def                       double?   (isa? #?(:clj Double :cljs js/Number)))
         (-def                       ?double?  (? double?))

         (-def primitive?            (or boolean? #?@(:clj [byte? char? short? int? long? float?]) double?))

#_(:clj (-def comparable-primitive? (and primitive? (not boolean?))))


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

(def ^{:doc "Could do <Class>/MAX_VALUE for the maxes in Java but JS doesn't like it of course
             In JavaScript, all numbers are 64-bit floating point numbers.
             This means you can't represent in JavaScript all the Java longs
             Max 'safe' int: (dec (Math/pow 2 53))"}
  unboxed-symbol->type-meta
  {'boolean {:bits 1
             :min  0
             :max  1
   #?@(:clj [:array-ident  "Z"
             :outer-type  "[Z"
             :boxed       java.lang.Boolean
             :unboxed     Boolean/TYPE])}
   'byte    {:bits 8
             :min -128
             :max  127
   #?@(:clj [:array-ident  "B"
             :outer-type  "[B"
             :boxed       java.lang.Byte
             :unboxed     Byte/TYPE])}
   'short   {:bits 16
             :min -32768
             :max  32767
   #?@(:clj [:array-ident  "S"
             :outer-type  "[S"
             :boxed       java.lang.Short
             :unboxed     Short/TYPE])}
   'char    {:bits 16
             :min  0
             :max  65535
   #?@(:clj [:array-ident  "C"
             :outer-type  "[C"
             :boxed       java.lang.Character
             :unboxed     Character/TYPE])}
   'int     {:bits 32
             :min -2147483648
             :max  2147483647
   #?@(:clj [:array-ident  "I"
             :outer-type  "[I"
             :boxed       java.lang.Integer
             :unboxed     Integer/TYPE])}
   'long    {:bits 64
             :min -9223372036854775808
             :max  9223372036854775807
   #?@(:clj [:array-ident  "J"
             :outer-type  "[J"
             :boxed       java.lang.Long
             :unboxed     Long/TYPE])}
   ; Technically with floating-point nums, "min" isn't the most negative;
   ; it's the smallest absolute
   'float   {:bits         32
             :min-absolute 1.4E-45
             :min         -3.4028235E38
             :max          3.4028235E38
             :min-int     -16777216 ; -2^24
             :max-int      16777216 ;  2^24
   #?@(:clj [:array-ident  "F"
             :outer-type  "[F"
             :boxed       java.lang.Float
             :unboxed     Float/TYPE])}
   'double  {:bits        64
             ; Because:
             ; Double/MIN_VALUE        = 4.9E-324
             ; (.-MIN_VALUE js/Number) = 5e-324
             :min-absolute #?(:clj  Double/MIN_VALUE
                              :cljs (.-MIN_VALUE js/Number))
             :min         -1.7976931348623157E308
             :max          1.7976931348623157E308 ; Max number in JS
             :min-int      -9007199254740992 ; -2^53
             :max-int       9007199254740992 ;  2^53
   #?@(:clj [:array-ident  "D"
             :outer-type  "[D"
             :boxed       java.lang.Double
             :unboxed     Double/TYPE])}})

#?(:clj (def primitive-classes (->> unboxed-symbol->type-meta vals (uc/map+ :unboxed) (join #{}))))

(defn- -spec>classes [spec classes]
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

(defn spec>classes #_> set?
  "Outputs the set of all the classes ->`spec` can embody according to its various conditional branches,
   if any. Ignores nils, treating in Clojure simply as a `java.lang.Object`."
  [spec] (-spec>classes spec #{}))

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

;; ===== META ===== ;;

#?(:clj  (-def class?                      (isa? java.lang.Class)))
#?(:clj  (-def primitive-class?            (fn [x] (c/and (uclass/primitive? x) (not== Void/TYPE x)))))
#?(:clj  (-def protocol?                   (>expr (ufn/fn-> :on-interface class?))))

;; ===== NUMBERS ===== ;;

         (-def bigint?                     (or #?@(:clj  [clojure.lang.BigInt java.math.BigInteger]
                                                   :cljs [com.gfredericks.goog.math.Integer])))
         (-def integer?                    (or #?@(:clj [byte? short? int? long?]) bigint?))

#?(:clj  (-def bigdec?                     java.math.BigDecimal)) ; TODO CLJS may have this

         (-def decimal?                    (or #?@(:clj [float?]) double? #?(:clj bigdec?)))

         (-def ratio?                      #?(:clj  clojure.lang.Ratio
                                              :cljs quantum.core.numeric.types.Ratio)) ; TODO add this CLJS entry to the predicate after the fact

#?(:clj  (-def primitive-number?           (or short? int? long? float? double?)))

         (-def number?                     (or #?@(:clj  [Number]
                                                   :cljs [integer? decimal? ratio?])))

;; ----- NUMBER LIKENESSES ----- ;;

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

#?(:clj  (-def char-seq?       java.lang.CharSequence))
#?(:clj  (-def comparable?     java.lang.Comparable))
         (-def string?         #?(:clj java.lang.String     :cljs js/String))
         (-def keyword?        #?(:clj clojure.lang.Keyword :cljs cljs.core/Keyword))
         (-def symbol?         #?(:clj clojure.lang.Symbol  :cljs cljs.core/Symbol))
#?(:clj  (-def tagged-literal? clojure.lang.TaggedLiteral))

         (-def literal?        (or nil? boolean? symbol? keyword? string? #?(:clj long?) double? #?(:clj tagged-literal?)))
#?(:clj  (-def array-list?     java.util.ArrayList))
#?(:clj  (-def java-coll?      java.util.Collection))
#?(:clj  (-def java-set?       java.util.Set))
#?(:clj  (-def thread?         java.lang.Thread))
#?(:clj  (-def throwable?      java.lang.Throwable))
#?(:clj  (-def comparable?     java.lang.Comparable))
#?(:clj  (-def iterable?       java.lang.Iterable))
#_(t/def ::form    (t/or ::literal t/list? t/vector? ...))

)
