(ns quantum.untyped.core.type
  "Essentially, set-theoretic definitions and operations on types."
  (:refer-clojure :exclude
    [< <= = >= > == compare
     and or not
     boolean  byte  char  short  int  long  float  double
     boolean? byte? char? short? int? long? float? double?
     isa?
     nil? any? class? tagged-literal?
     number? decimal? bigdec? integer? ratio?
     keyword? string? symbol?
     meta
     assoc-in])
  (:require
    [clojure.core                      :as c]
    [quantum.core.macros.deftype       :as dt]
    [quantum.core.type.core            :as tcore]
    [quantum.untyped.core.analyze.expr :as xp
      :refer [>expr]]
    [quantum.untyped.core.collections  :as ucoll
      :refer [assoc-in dissoc-in]]
    [quantum.untyped.core.collections.logic
      :refer [seq-and]]
    [quantum.untyped.core.compare      :as ucomp
      :refer [== not==]]
    [quantum.untyped.core.convert      :as uconv
      :refer [>symbol]]
    [quantum.untyped.core.core         :as ucore]
    [quantum.untyped.core.data.bits    :as ubit]
    [quantum.untyped.core.error        :as uerr
      :refer [err! TODO catch-all]]
    [quantum.untyped.core.fn           :as ufn
      :refer [fn1 fn' rcomp <- fn->]]
    [quantum.untyped.core.logic
      :refer [fn-and]]
    [quantum.untyped.core.numeric      :as unum]
    [quantum.untyped.core.print        :as upr]
    [quantum.untyped.core.qualify      :as qual]
    [quantum.untyped.core.reducers     :as ur
      :refer [map+ filter+ remove+ distinct+ join]]
    [quantum.untyped.core.refs
      :refer [?deref]]
    [quantum.untyped.core.vars
      :refer [def- update-meta]])
  #?(:clj (:import quantum.untyped.core.analyze.expr.Expression)))

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

(dt/deftype ValueSpec [v]
  {PSpec                 nil
   fipp.ednize/IOverride nil
   fipp.ednize/IEdn      {-edn    ([this] (list `value v))}
   ?Fn                   {invoke  ([_ x] (c/= x v))}
   ?Object               {equals  ([this that]
                                    (c/or (identical? this that)
                                          (c/and (instance? ValueSpec that)
                                                 (c/= v (.-v ^ValueSpec that)))))}
   ?Comparable           {compare ([this that]
                                    (if-not (instance? ValueSpec that)
                                      (err! "Cannot compare with non-ValueSpec")
                                      (c/compare v (.-v ^ValueSpec that))))}})

(defn value [v] (ValueSpec. v))

(defn value-spec? [x] (instance? ValueSpec x))

(defn value-spec>value [x]
  (if (value-spec? x)
      (.-v ^ValueSpec x)
      (err! "Not a value spec" x)))

;; -----

(dt/deftype ClassSpec
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
                        (c/or (identical? this that)
                              (c/and (instance? ClassSpec that)
                                     (c/= c (.-c ^ClassSpec that)))))}})

(defn class-spec? [x] (instance? ClassSpec x))

(defn class-spec>class [spec]
  (if (class-spec? spec)
      (.-c ^ClassSpec spec)
      (err! "Cannot cast to ClassSpec" {:x spec})))

(dt/deftype ProtocolSpec
  [meta #_(t/? ::meta)
   p    #_t/protocol?
   name #_(t/? t/symbol?)]
  {PSpec                 nil
   fipp.ednize/IOverride nil
   fipp.ednize/IEdn
     {-edn ([this] (c/or name (list `isa|protocol? p)))}
   ?Fn   {invoke    ([_ x] (satisfies? p x))}
   ?Meta {meta      ([this] meta)
          with-meta ([this meta'] (ProtocolSpec. meta' p name))}})

(defn isa? [c]
  (assert (c/class? c))
  (ClassSpec. nil c nil))

(defn isa|protocol? [c]
  #_(assert (protocol? c))
  (ProtocolSpec. nil c nil))

;; ===== CREATION ===== ;;

(defonce *spec-registry (atom {}))

(extend-protocol PSpec Expression)

(declare nil?)

(defn >spec
  "Coerces ->`x` to a spec, recording its ->`name-sym` if provided."
  ([x] (>spec x nil))
  ([x name-sym]
    (assert (c/or (c/nil? name-sym) (c/symbol? name-sym)))
    (cond (satisfies? PSpec x)
            x ; TODO should add in its name?
          (c/class? x)
            (let [x (c/or (tcore/unboxed->boxed x) x)
                  reg (if (c/nil? name-sym)
                          @*spec-registry
                          (swap! *spec-registry
                            (fn [reg]
                              (if-let [spec (get reg name-sym)]
                                (if (c/= (.-name ^ClassSpec spec) name-sym)
                                    reg
                                    (err! "Class already registered with spec; must first undef" {:class x :spec-name name-sym}))
                                (let [spec (ClassSpec. nil x name-sym)]
                                  (assoc-in reg [name-sym]    spec
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
          (qcore/protocol? x)
            (ProtocolSpec. nil x name-sym)
          :else
            (value x))))

;; ===== DEFINITION ===== ;;

(defmacro define [sym specable]
  `(~'def ~sym (>spec ~specable '~(qual/qualify sym))))

(defn undef [reg sym]
  (if-let [spec (get reg sym)]
    (let [reg' (dissoc reg sym)]
      (if (instance? ClassSpec spec)
          (dissoc-in reg' [:by-class (.-c ^ClassSpec spec)])))
    reg)
  )

(defn undef! [sym] (swap! *spec-registry undef sym))

(defmacro defalias [sym spec]
  `(~'def ~sym (>spec ~spec)))

(uvar/defalias -def define)

(-def spec? PSpec)

(defn ! [spec]
  (if (spec? spec)
      (update-meta spec assoc :runtime? true)
      (err! "Input must be spec" spec)))

(dt/deftype DeducibleSpec [*spec #_(t/atom-of t/spec?)]
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

#?(:clj
(defn compare|class|class
  "Compare extension (generality|specificity) of ->`c0` to ->`c1`.
   `0`  means they are equally general/specific:
     - ✓ `(t/= c0 c1)`    : the extension of ->`c0` is equal to             that of ->`c1`.
   `-1` means ->`c0` is less general (more specific) than ->`c1`.
     - ✓ `(t/< c0 c1)`    : the extension of ->`c0` is a strict subset   of that of ->`c1`.
   `1`  means ->`c0` is more general (less specific) than ->`c1`:
     - ✓ `(t/> c0 c1)`    : the extension of ->`c0` is a strict superset of that of ->`c1`.
   `nil` means their generality/specificity is incomparable:
     - ✓ `(t/incomparable? c0 c1)` :
         the extension of ->`c0` is neither a subset nor a superset of that of ->`c1`.
   Unboxed primitives are considered to be less general (more specific) than boxed primitives."
  [^Class c0 ^Class c1]
  (cond (== c0 c1)
        0
        (== c0 Object)
        1
        (== c1 Object)
        -1
        (== (tcore/boxed->unboxed c0) c1)
        1
        (== c0 (tcore/boxed->unboxed c1))
        -1
        (c/or (tcore/primitive-array-type? c0) (tcore/primitive-array-type? c1))
        nil ; we'll consider the two unrelated
        (.isAssignableFrom c0 c1)
        1
        (.isAssignableFrom c1 c0)
        -1
        :else nil))) ; unrelated

;; ===== EXTENSIONALITY COMPARISON ===== ;;

(declare compare|dispatch)

(defn #_long compare ; TODO for some reason primitive type hints break it for the time being
  ;; TODO optimize the `recur`s here as they re-take old code paths
  "Returns the value of the comparison of the extensions of ->`s0` and ->`s1`.
   `-1`   means (ex ->`s0`) ⊂     (ex ->`s1`)
    `0`   means (ex ->`s0`) =     (ex ->`s1`)
    `1`   means (ex ->`s0`) ⊃     (ex ->`s1`)
    `nil` means (ex ->`s0`) ⊄,≠,⊅ (ex ->`s1`)

   Does not compare cardinalities or other relations of sets, but rather only sub/superset
   relations."
  [s0 s1]
  (assert (spec? s0) {:s0 s0})
  (assert (spec? s1) {:s1 s1})
  (let [dispatched (-> compare|dispatch (get (class s0)) (get (class s1)))]
    (if (c/nil? dispatched)
        (err! "Specs not handled" {:s0 s0 :s1 s1})
        (dispatched s0 s1))))

#_(compare (numerically byte?) byte?) ; -> 1
#_(compare byte? (numerically byte?)) ; -> -1

(defn boolean-compare
  "Incomparables return `false` for the boolean comparator `pred`."
  [pred s0 s1]
  (let [ret (compare s0 s1)]
    (if (c/nil? ret) false (pred ret 0))))

(defn <
  "Computes whether the extension of spec ->`s0` is a strict subset of that of ->`s1`."
  [s0 s1] (boolean-compare c/< s0 s1))

(defn <=
  "Computes whether the extension of spec ->`s0` is a (lax) subset of that of ->`s1`."
  [s0 s1] (boolean-compare c/<= s0 s1))

(defn =
  "Computes whether the extension of spec ->`s0` is equal to that of ->`s1`."
  [s0 s1] (boolean-compare c/= s0 s1))

(defn >=
  "Computes whether the extension of spec ->`s0` is a (lax) superset of that of ->`s1`."
  [s0 s1] (boolean-compare c/>= s0 s1))

(defn >
  "Computes whether the extension of spec ->`s0` is a strict superset of that of ->`s1`."
  [s0 s1] (boolean-compare c/> s0 s1))

;; ===== LOGICAL ===== ;;

(defn create-logical-spec
  [construct-fn arg args compare-fn]
  (if (empty? args)
      (>spec arg)
      (let [;; simplification via identity
            simp|identity (->> (cons arg args) (map+ >spec) distinct+)
            ;; simplification via intension comparison
            args' (->> simp|identity
                       (reduce
                         (fn [args' s]
                           (let [with-comparisons
                                   (->> args'
                                        (map+    (juxt identity #(compare s %)))
                                        ;; remove all args for which `s` has a <compare-fn> intension
                                        (remove+ (rcomp second (fn-and c/some? compare-fn)))
                                        join)]
                             (if (c/or ;; at least one arg with a <compare-fn> intension than `s`
                                       (c/< (count with-comparisons) (count args'))
                                       ;; `s` is incomparable to all args
                                       (->> with-comparisons (seq-and (fn-> second c/nil?))))
                                 (->> with-comparisons (mapv first) (<- conj s))
                                 args')))
                         []))]
        (if (-> args' count (c/= 1))
            (first args')
            (construct-fn args')))))

;; ===== AND ===== ;;

(dt/deftype AndSpec [args #_(t/and t/indexed? (t/seq spec?))]
  {PSpec                 nil
   fipp.ednize/IOverride nil
   fipp.ednize/IEdn
     {-edn ([this] (list* `and args))}
   ?Fn {invoke ([_ x] (reduce (fn [_ pred] (c/or (pred x) (reduced false)))
                        true ; vacuously
                        args))}})

(defn and-spec? [x] (instance? AndSpec x))

(defn and-spec>args [x]
  (if (instance? AndSpec x)
      (.-args ^AndSpec x)
      (err! "Cannot cast to AndSpec" x)))

(defn and
  "Sequential/ordered `and`.
   Applies as much 'compression'/deduplication/simplification as possible to the supplied specs.
   Yields error if provided with incompatible specs (ones whose logical intersection is empty)."
  [arg & args]
  (create-logical-spec ->AndSpec arg args c/neg?))

(deftype
  UnorderedAndSpec [args #_(t/unkeyed spec?)]
  PSpec
  fipp.ednize/IOverride
  fipp.ednize/IEdn (-edn [this] (list* `and* args)))

(defn intersection|spec
  ([a] a)
  ([a b]
    (assert (spec? a) (spec? b))
    (if (c/= a b)
        a
        (let [comparison (compare a b)]
          (cond (c/nil? comparison)
                nil ;; intersection of unrelated specs is `nil`
                (zero? comparison)
                a ;; technically, choose the simpler one, but these will all be simplified anyway
                (neg? comparison)
                a
                :else b)))))

(defn and*
  "Unordered `and`. Analogous to `set/intersection`, not `core/and`:
   rather than ensuring specific conditional application of specs, `and*` merely
   ensures all specs are met in *some* order.
   Applies as much 'compression'/deduplication/simplification as possible to the supplied specs.
   Effectively computes the intersection of the intension of the ->`args`."
  [arg & args]
  (let [specs (->> (cons arg args) (ur/map+ >spec) (ur/incremental-apply intersection|spec))]
    (if (coll? specs) ; technically, `unkeyed?`
        (UnorderedAndSpec. specs)
        specs)))

;; ===== OR ===== ;;

(dt/deftype OrSpec [args #_(t/and t/indexed? (t/seq spec?))]
  {PSpec                 nil
   fipp.ednize/IOverride nil
   fipp.ednize/IEdn
     {-edn ([this] (list* `or args))}
   ?Fn {invoke ([_ x] (reduce (fn [_ pred] (let [p (pred x)] (c/and p (reduced p))))
                        true ; vacuously
                        args))}})

(defn or-spec? [x] (instance? OrSpec x))

(defn or-spec>args [x]
  (if (instance? OrSpec x)
      (.-args ^OrSpec x)
      (err! "Cannot cast to OrSpec" x)))

(defn or
  "Sequential/ordered `or`.
   Applies as much 'compression'/deduplication/simplification as possible to the supplied specs."
  [arg & args]
  (create-logical-spec ->OrSpec arg args c/pos?))

(deftype
  UnorderedOrSpec [args #_(t/unkeyed spec?)]
  PSpec
  fipp.ednize/IOverride
  fipp.ednize/IEdn (-edn [this] (list* `or* args)))

(defn union|spec
  ([a] a)
  ([a b]
    (assert (spec? a) (spec? b))
    (if (c/= a b)
        a
        (let [comparison (compare a b)]
          (cond (c/nil? comparison)
                #{a b}
                (zero? comparison)
                a ;; technically, choose the simpler one, but these will all be simplified anyway
                (neg? comparison)
                b
                :else a)))))

(defn or*
  "Unordered `or`. Analogous to `set/union`, not `core/or`:
   rather than ensuring specific conditional application of specs, `or*` merely
   ensures at least one spec is met in *some* order.
   Applies as much 'compression'/deduplication/simplification as possible to the supplied specs.
   Effectively computes the union of the intension of the ->`args`."
  [arg & args]
  (TODO "or*"))

;; ===== OR ===== ;;

(dt/deftype NotSpec [spec #_t/spec?]
  {PSpec                 nil
   fipp.ednize/IOverride nil
   fipp.ednize/IEdn
     {-edn ([this] (list `not spec))}
   ?Fn {invoke ([_ x] (spec x))}})

(defn not [x] (NotSpec. (>spec x)))

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

(-def nil? (value nil))

(dt/deftype NilableSpec [meta #_(t/? ::meta) spec #_t/spec?]
  {PSpec nil
   ?Fn     {invoke    ([this x] (c/or (c/nil? x) (spec x)))}
   ?Meta   {meta      ([this] meta)
            with-meta ([this meta'] (NilableSpec. meta' spec))}
   ?Object {equals    ([this that]
                        (c/or (identical? this that)
                              (c/and (instance? NilableSpec that)
                                     (c/= spec (.-spec ^NilableSpec that)))))}
   fipp.ednize/IOverride nil
   fipp.ednize/IEdn
     {-edn ([this] (list `? spec))}})

(defn nilable-spec? [x] (instance? NilableSpec x))

(defn nilable-spec>inner-spec [spec]
  (if (instance? NilableSpec spec)
      (.-spec ^NilableSpec spec)
      (err! "Cannot cast to NilableSpec" {:x spec})))

;; This sadly gets a java.lang.AbstractMethodError when one tries to do as simple as:
;; `(def ? (InferSpec. nil))`
;; `(def abcde (? 1))
(dt/deftype InferSpec [meta #_(t/? ::meta)]
  {PSpec nil
   ?Fn {invoke (([this x] (NilableSpec. nil (>spec x)))
                ([this spec x] (c/or (c/nil? x) (spec x))))}
   ?Meta {meta      ([this] meta)
          with-meta ([this meta'] (InferSpec. meta'))}
   fipp.ednize/IOverride nil
   fipp.ednize/IEdn
     {-edn ([this] `?)}})

(defn ?
  "Denotes type inference should be performed.
   Arity 1: Computes a spec denoting a nilable value satisfying `spec`.
   Arity 2: Computes whether `x` is nil or satisfies `spec`."
  ([x] (NilableSpec. nil (>spec x)))
  ([spec x] (c/or (c/nil? x) (spec x))))

(def- compare|dispatch
  (let [with-invert-comparison
          (fn [f] (fn [s0 s1] (ucomp/invert (f s1 s0))))
        v+c (fn [s0 s1]
              (let [v (.-v ^ValueSpec s0)
                    c (.-c ^ClassSpec s1)]
                (if (instance? c v)
                    ;; e.g. asking how the set containing only the string "abc"
                    ;; relates to the set of all strings (the class String)
                    -1 ; the extension is a strict subset
                    ;; e.g. asking how the set containing only the string "abc"
                    ;; relates the set of all bytes (the class Byte)
                    nil))) ; neither subset nor superset
        v+o (fn [s0 s1]
              (let [specs (.-args ^OrSpec s1)]
                (reduce
                   (fn [ret s]
                     (let [ret' (compare s0 s)]
                       (case ret'
                         nil ret
                         -1  (reduced ret') ; because the extension of `s1` only gets bigger
                         ret')))
                   nil
                   specs)))
        v+a (fn [s0 s1]
              (let [specs (.-args ^AndSpec s1)]
                (reduce
                   (fn [ret s]
                     (let [ret' (compare s0 s)]
                       (case ret'
                         nil (reduced ret')
                         1   (reduced ret') ; because the extension of `s1` only gets smaller
                         ret')))
                   nil
                   specs)))
        v+n (fn [s0 s1]
              (if (-> s0 value-spec>value nil?)
                  -1
                  (err! "TODO dispatch" {:s0 s0 :s1 s1})))
        <ident  0 ; -1
        =ident  1 ; 0
        >ident  2 ; 1
        !ident  3 ; nil
        <+!     (-> ubit/empty (ubit/conj <ident) (ubit/conj !ident)) ; 9
        >+!     (-> ubit/empty (ubit/conj >ident) (ubit/conj !ident)) ; 12
        =+!     (-> ubit/empty (ubit/conj =ident) (ubit/conj !ident)) ; 10

        ;; #{(⊂ | =) ∅} -> ⊂
        ;; #{(⊃ ?) ∅} -> ∅
        ;; Otherwise whatever it is
        c+o (fn [s0 s1]
              (let [specs (.-args ^OrSpec s1)]
                (first
                  (reduce
                    (fn [[ret found] s]
                      (let [ret'   (compare s0 s)
                            found' (ubit/conj found (case ret' -1 0, 0 1, 1 2, nil 3))]
                        (case (c/long found')
                          (9 #_<+! 10 #_=+!) (reduced [-1  nil])
                          (12 #_>+!)         (reduced [nil nil])
                          [ret' found'])))
                    [nil ubit/empty]
                    specs))))
        ;; Any ∅ -> ∅
        ;; Otherwise whatever it is
        c+a (fn [s0 s1]
              (let [specs (.-args ^AndSpec s1)]
                (reduce
                  (fn [ret s]
                    (let [ret' (compare s0 s)]
                      (if (c/nil? ret') (reduced nil) ret')))
                  nil
                  specs)))
        c+n (fn [s0 s1]
              (case (compare s0 (nilable-spec>inner-spec s1))
                (0 -1) -1
                nil))
        o+o (fn [^OrSpec s0 ^OrSpec s1]
              (let [;; every element in s0 an extensional strict subset of s1
                    l (->> s0 .-args (map+ (fn1 compare s1)) (seq-and (fn1 c/= -1)))
                    ;; every element in s1 an extensional strict subset of s0
                    r (->> s1 .-args (map+ (fn1 compare s0)) (seq-and (fn1 c/= -1)))]
                (if l
                    (if r 0 -1)
                    (if r 1 nil))))
        o+a (fn [^OrSpec s0 ^AndSpec s1]
              (let [;; every element in s1 an extensional strict subset of s0
                    r (->> s1 .-args (map+ (fn1 compare s0)) (seq-and (fn1 c/= -1)))]
                (if r 1 nil)))
        e+e (fn [s0 s1] (if (c/= s0 s1) 0 nil))
        incomparable (fn [s0 s1] nil)
  ]
    {InferSpec
       {InferSpec        (fn' 0)
        ValueSpec        (fn' 1)
        ClassSpec        (fn' 1)
        ProtocolSpec     (fn' 1)
        NilableSpec      (fn' 1)
        OrSpec           (fn' 1)
        UnorderedOrSpec  (fn' 1)
        AndSpec          (fn' 1)
        UnorderedAndSpec (fn' 1)
        Expression       (fn' 1)}
     ValueSpec
       {InferSpec        (fn' -1)
        ValueSpec        (fn [s0 s1] (catch-all
                                       (unum/signum|long (c/compare s0 s1))
                                       nil))
        ClassSpec        v+c
        ProtocolSpec     (fn [s0 s1] (err! "TODO dispatch" {:s0 s0 :s1 s1}))
        NilableSpec      v+n
        OrSpec           v+o
        UnorderedOrSpec  (fn [s0 s1] (err! "TODO dispatch" {:s0 s0 :s1 s1}))
        AndSpec          v+a
        UnorderedAndSpec (fn [s0 s1] (err! "TODO dispatch" {:s0 s0 :s1 s1}))
        Expression       incomparable}
     ClassSpec
       {InferSpec        (fn' -1)
        ValueSpec        (with-invert-comparison v+c)
        ClassSpec        (fn [s0 s1] (compare|class|class (.-c ^ClassSpec s0) (.-c ^ClassSpec s1)))
        ProtocolSpec     (fn [s0 s1] (err! "TODO dispatch" {:s0 s0 :s1 s1}))
        NilableSpec      c+n
        OrSpec           c+o
        UnorderedOrSpec  (fn [s0 s1] (err! "TODO dispatch" {:s0 s0 :s1 s1}))
        AndSpec          c+a
        UnorderedAndSpec (fn [s0 s1] (err! "TODO dispatch" {:s0 s0 :s1 s1}))
        Expression       incomparable}
     ProtocolSpec
       {InferSpec        (fn' -1)
        ValueSpec        (fn [s0 s1] (err! "TODO dispatch" {:s0 s0 :s1 s1}))
        ClassSpec        (fn [s0 s1] (err! "TODO dispatch" {:s0 s0 :s1 s1}))
        ProtocolSpec     (fn [s0 s1] (err! "TODO dispatch" {:s0 s0 :s1 s1}))
        NilableSpec      (fn [s0 s1] (err! "TODO dispatch" {:s0 s0 :s1 s1}))
        OrSpec           (fn [s0 s1] (err! "TODO dispatch" {:s0 s0 :s1 s1}))
        UnorderedOrSpec  (fn [s0 s1] (err! "TODO dispatch" {:s0 s0 :s1 s1}))
        AndSpec          (fn [s0 s1] (err! "TODO dispatch" {:s0 s0 :s1 s1}))
        UnorderedAndSpec (fn [s0 s1] (err! "TODO dispatch" {:s0 s0 :s1 s1}))
        Expression       incomparable}
     NilableSpec
       {InferSpec        (fn' -1)
        ValueSpec        (with-invert-comparison v+n)
        ClassSpec        (with-invert-comparison c+n)
        ProtocolSpec     (fn [s0 s1] (err! "TODO dispatch" {:s0 s0 :s1 s1}))
        NilableSpec      (fn [s0 s1] (compare (nilable-spec>inner-spec s0) (nilable-spec>inner-spec s1)))
        OrSpec           (fn [s0 s1] (err! "TODO dispatch" {:s0 s0 :s1 s1}))
        UnorderedOrSpec  (fn [s0 s1] (err! "TODO dispatch" {:s0 s0 :s1 s1}))
        AndSpec          (fn [s0 s1] (err! "TODO dispatch" {:s0 s0 :s1 s1}))
        UnorderedAndSpec (fn [s0 s1] (err! "TODO dispatch" {:s0 s0 :s1 s1}))
        Expression       incomparable}
     OrSpec
       {InferSpec        (fn' -1)
        ValueSpec        (with-invert-comparison v+o)
        ClassSpec        (with-invert-comparison c+o)
        ProtocolSpec     (fn [s0 s1] (err! "TODO dispatch" {:s0 s0 :s1 s1}))
        NilableSpec      (fn [s0 s1] (err! "TODO dispatch" {:s0 s0 :s1 s1}))
        OrSpec           o+o
        UnorderedOrSpec  (fn [s0 s1] (err! "TODO dispatch" {:s0 s0 :s1 s1}))
        AndSpec          o+a
        UnorderedAndSpec (fn [s0 s1] (err! "TODO dispatch" {:s0 s0 :s1 s1}))
        Expression       incomparable}
     UnorderedOrSpec
       {InferSpec        (fn' -1)
        ValueSpec        (fn [s0 s1] (err! "TODO dispatch" {:s0 s0 :s1 s1}))
        ClassSpec        (fn [s0 s1] (err! "TODO dispatch" {:s0 s0 :s1 s1}))
        ProtocolSpec     (fn [s0 s1] (err! "TODO dispatch" {:s0 s0 :s1 s1}))
        NilableSpec      (fn [s0 s1] (err! "TODO dispatch" {:s0 s0 :s1 s1}))
        OrSpec           (fn [s0 s1] (err! "TODO dispatch" {:s0 s0 :s1 s1}))
        UnorderedOrSpec  (fn [s0 s1] (err! "TODO dispatch" {:s0 s0 :s1 s1}))
        AndSpec          (fn [s0 s1] (err! "TODO dispatch" {:s0 s0 :s1 s1}))
        UnorderedAndSpec (fn [s0 s1] (err! "TODO dispatch" {:s0 s0 :s1 s1}))
        Expression       incomparable}
     AndSpec
       {InferSpec        (fn' -1)
        ValueSpec        (with-invert-comparison v+a)
        ClassSpec        (with-invert-comparison c+a)
        ProtocolSpec     (fn [s0 s1] (err! "TODO dispatch" {:s0 s0 :s1 s1}))
        NilableSpec      (fn [s0 s1] (err! "TODO dispatch" {:s0 s0 :s1 s1}))
        OrSpec           (with-invert-comparison o+a)
        UnorderedOrSpec  (fn [s0 s1] (err! "TODO dispatch" {:s0 s0 :s1 s1}))
        AndSpec          (fn [s0 s1] (err! "TODO dispatch" {:s0 s0 :s1 s1}))
        UnorderedAndSpec (fn [s0 s1] (err! "TODO dispatch" {:s0 s0 :s1 s1}))
        Expression       incomparable}
     UnorderedAndSpec
       {InferSpec        (fn' -1)
        ValueSpec        (fn [s0 s1] (err! "TODO dispatch" {:s0 s0 :s1 s1}))
        ClassSpec        (fn [s0 s1] (err! "TODO dispatch" {:s0 s0 :s1 s1}))
        ProtocolSpec     (fn [s0 s1] (err! "TODO dispatch" {:s0 s0 :s1 s1}))
        NilableSpec      (fn [s0 s1] (err! "TODO dispatch" {:s0 s0 :s1 s1}))
        OrSpec           (fn [s0 s1] (err! "TODO dispatch" {:s0 s0 :s1 s1}))
        UnorderedOrSpec  (fn [s0 s1] (err! "TODO dispatch" {:s0 s0 :s1 s1}))
        AndSpec          (fn [s0 s1] (err! "TODO dispatch" {:s0 s0 :s1 s1}))
        UnorderedAndSpec (fn [s0 s1] (err! "TODO dispatch" {:s0 s0 :s1 s1}))
        Expression       incomparable}
     Expression
       {InferSpec        (fn' -1)
        ValueSpec        incomparable
        ClassSpec        incomparable
        ProtocolSpec     incomparable
        NilableSpec      incomparable
        OrSpec           incomparable
        UnorderedOrSpec  incomparable
        AndSpec          incomparable
        UnorderedAndSpec incomparable
        Expression       e+e}}))

;; ===== PRIMITIVES ===== ;;

         (-def                       boolean?  #?(:clj Boolean :cljs js/Boolean))
         (-def                       ?boolean? (? boolean?))

#?(:clj  (-def                       byte?     Byte))
#?(:clj  (-def                       ?byte?    (? byte?)))

#?(:clj  (-def                       char?     Character))
#?(:clj  (-def                       ?char?    (? char?)))

#?(:clj  (-def                       short?    Short))
#?(:clj  (-def                       ?short?   (? short?)))

#?(:clj  (-def                       int?      Integer))
#?(:clj  (-def                       ?int?     (? int?)))

#?(:clj  (-def                       long?     Long))
#?(:clj  (-def                       ?long?    (? long?)))

#?(:clj  (-def                       float?    Float))
#?(:clj  (-def                       ?float?   (? float?)))

         (-def                       double?   #?(:clj Double :cljs js/Number))
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

#?(:clj (def primitive-classes (->> unboxed-symbol->type-meta vals (map+ :unboxed) (join #{}))))

(defn- -spec>class [spec spec-nilable?]
  (cond (class-spec? spec)
          {:class (class-spec>class spec) :nilable? spec-nilable?}
        (value-spec? spec)
          (let [v (value-spec>value spec)]
            {:class (class v) :nilable? spec-nilable?})
        (c/and (nilable-spec? spec) (c/not spec-nilable?))
          (recur (nilable-spec>inner-spec spec) true)
        :else
          (err! "Don't know how to handle spec" spec)))

(defn spec>class
  "Outputs the single class embodied by ->`spec`.
   Outputs `{:class <class>    :nilable? <nilable>}` if the spec embodies only one (possibly nilable) class.
   Outputs `{:class nil        :nilable? true     }` if the spec embodies the value `nil`.
   Outputs `{:class ::multiple :nilable? nil      }` if the spec embodies multiple classes."
  [spec] (-spec>class spec false))

(defn- -spec>classes [spec classes]
  (cond (class-spec? spec)
          (conj classes (class-spec>class spec))
        (value-spec? spec)
          (conj classes (value-spec>value spec))
        (nilable-spec? spec)
          (recur (nilable-spec>inner-spec spec) classes)
        (and-spec? spec)
          (reduce (fn [classes' spec'] (-spec>classes spec' classes'))
            classes (and-spec>args spec))
        (or-spec? spec)
          (reduce (fn [classes' spec'] (-spec>classes spec' classes'))
            classes (or-spec>args spec))
        :else
          (err! "Not sure how to handle spec")))

(defn spec>classes
  "Outputs the set of all the classes ->`spec` can embody according to its various conditional branches,
   if any. Ignores nils, treating in Clojure simply as a `java.lang.Object`."
  [spec] (-spec>classes spec #{}))

(defn- -spec>?class-value [spec spec-nilable?]
  (cond (value-spec? spec)
          (let [v (value-spec>value spec)]
            (when (c/class? v) {:class v :nilable? spec-nilable?}))
        (c/and (nilable-spec? spec) (c/not spec-nilable?))
          (recur (nilable-spec>inner-spec spec) true)
        :else nil))

(defn spec>?class-value
  "Outputs the single class value embodied by ->`spec`.
   Differs from `spec>class` in that if a spec is a extensionally equal of the *value* of a class,
   outputs that class.

   However, if a spec does not embody the value of a class but rather merely embodies (as all specs)
   an extensional subset of the set of all objects conforming to a class, outputs nil."
  {:examples `{(spec>?class-value (value String)) {:class String :nilable? false}
               (spec>?class-value (isa? String))  nil}}
  [spec] (-spec>?class-value spec false))

;; ===== GENERAL ===== ;;

         (-def object?                     #?(:clj java.lang.Object :cljs js/Object))

         (-def any?                        (? (or object? #?@(:cljs [js/String js/Symbol]))))

;; ===== META ===== ;;

#?(:clj  (-def class?                      (isa? java.lang.Class)))
#?(:clj  (-def primitive-class?            (fn [x] (c/and (class? x) (.isPrimitive ^Class x)))))
#?(:clj  (-def protocol?                   (>expr (fn/fn-> :on-interface class?))))

;; ===== NUMBERS ===== ;;

         (-def bigint?                     (or #?@(:clj  [clojure.lang.BigInt java.math.BigInteger]
                                                   :cljs [com.gfredericks.goog.math.Integer])))
         (-def integer?                    (or byte? short? int? long? bigint?))

#?(:clj  (-def bigdec?                     java.math.BigDecimal)) ; TODO CLJS may have this

         (-def decimal?                    (or float? double? bigdec?))

         (-def ratio?                      #?(:clj  clojure.lang.Ratio
                                              :cljs quantum.core.numeric.types.Ratio)) ; TODO add this CLJS entry to the predicate after the fact

#?(:clj  (-def primitive-number?           (or short? int? long? float? double?)))

         (-def number?                     (or #?@(:clj  [Number]
                                                   :cljs [integer? decimal? ratio?])))

;; ----- NUMBER LIKENESSES ----- ;;

         (-def integer-value?              (or integer? (and decimal? (>expr unum/integer-value?))))

       #_(-def numeric-primitive?          (and primitive? (not boolean?)))

         (-def numerically-byte?           (and integer-value? (>expr (fn [x] (c/<= -128                 x 127)))))
         (-def numerically-short?          (and integer-value? (>expr (fn [x] (c/<= -32768               x 32767)))))
         (-def numerically-char?           (and integer-value? (>expr (fn [x] (c/<=  0                   x 65535)))))
         (-def numerically-unsigned-short? numerically-char?)
         (-def numerically-int?            (and integer-value? (>expr (fn [x] (c/<= -2147483648          x 2147483647)))))
         (-def numerically-long?           (and integer-value? (>expr (fn [x] (c/<= -9223372036854775808 x 9223372036854775807)))))
         (-def numerically-float?          (and number?
                                                (>expr (fn [x] (c/<= -3.4028235E38 x 3.4028235E38)))
                                                (>expr (fn [x] (-> x clojure.lang.RT/floatCast (c/== x))))))
       #_(-def numerically-double?         (and number?
                                                (>expr (fn [x] (c/<= -1.7976931348623157E308 x 1.7976931348623157E308)))
                                                (>expr (fn [x] (-> x clojure.lang.RT/doubleCast (c/== x))))))

         (-def int-like?                   (and integer-value? numerically-int?))

(defn numerically
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

         (-def literal?        (or nil? boolean? symbol? keyword? string? long? double? tagged-literal?))
#_(t/def ::form    (t/or ::literal t/list? t/vector? ...))
)
