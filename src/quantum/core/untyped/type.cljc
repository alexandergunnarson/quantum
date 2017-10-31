(ns quantum.core.untyped.type
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
    [quantum.core.core                 :as qcore
      :refer [?deref]]
    [quantum.core.data.set             :as set
      :refer [oset]]
    [quantum.core.error                :as err
      :refer [err! TODO catch-all]]
    [quantum.core.fn                   :as fn
      :refer [fn1 rcomp <- fn->]]
    [quantum.core.logic
      :refer [fn-and]]
    [quantum.core.macros.deftype       :as dt]
    [quantum.core.print                :as pr]
    [quantum.core.type.core            :as tcore]
    [quantum.core.untyped.analyze.expr :as xp
      :refer [>expr]]
    [quantum.core.untyped.collections  :as ucoll
      :refer [assoc-in dissoc-in]]
    [quantum.core.untyped.collections.logic
      :refer [seq-and]]
    [quantum.core.untyped.compare      :as ucomp
      :refer [== not==]]
    [quantum.core.untyped.convert      :as uconv
      :refer [>symbol]]
    [quantum.core.untyped.data.bits    :as ubit]
    [quantum.core.untyped.numeric      :as unum]
    [quantum.core.untyped.qualify      :as qual]
    [quantum.core.untyped.reducers     :as r
      :refer [map+ filter+ remove+ distinct+ join]]
    [quantum.core.vars                 :as var
      :refer [def-]])
  #?(:clj (:import quantum.core.untyped.analyze.expr.Expression)))

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
   ?Comparable           {compare ([this that]
                                    (if-not (instance? ValueSpec that)
                                      (err! "Cannot compare with non-ValueSpec")
                                      (c/compare v (.-v ^ValueSpec that))))}})

(defn value [v] (ValueSpec. v))

(dt/deftype ClassSpec
  [meta     #_(t/? ::meta)
   ^Class c #_t/class?
   name     #_(t/? t/symbol?)]
  {PSpec                 nil
   fipp.ednize/IOverride nil
   fipp.ednize/IEdn
     {-edn ([this] (c/or name (list `isa? c)))}
   ?Fn   {invoke    ([_ x] (instance? c x))}
   ?Meta {meta      ([this] meta)
          with-meta ([this meta'] (ClassSpec. meta' c name))}})

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

(defn ^PSpec ->spec
  "Coerces ->`x` to a spec, recording its ->`name-sym` if provided."
  ([x] (->spec x nil))
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
            (ValueSpec. x))))

;; ===== DEFINITION ===== ;;

(defmacro def [sym specable]
  `(~'def ~sym (->spec ~specable '~(qual/qualify sym))))

(defn undef [reg sym]
  (if-let [spec (get reg sym)]
    (let [reg' (dissoc reg sym)]
      (if (instance? ClassSpec spec)
          (dissoc-in reg' [:by-class (.-c ^ClassSpec spec)])))
    reg)
  )

(defn undef! [sym] (swap! *spec-registry undef sym))

(defmacro defalias [sym spec]
  `(~'def ~sym (->spec ~spec)))

(var/defalias -def def)

(-def spec? PSpec)

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
      (->spec arg)
      (let [;; simplification via identity
            simp|identity (->> (cons arg args) (map+ ->spec) distinct+)
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

(defn and
  "Sequential/ordered `and`.
   Applies as much 'compression'/deduplication/simplification as possible to the supplied specs."
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
  (let [specs (->> (cons arg args) (r/map+ ->spec) (r/incremental-apply intersection|spec))]
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

(defn not [x] (NotSpec. (->spec x)))

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
  `(let [x# ~x] (FnConstantlySpec. nil (fn/fn' x#) x#))))

(defn unkeyed
  "Creates an unkeyed collection spec, in which the collection may
   or may not be sequential or even seqable, but must not have key-value
   pairs like a map.
   Examples of unkeyed collections include a vector (despite its associativity),
   a list, and a set (despite its values doubling as keys).
   A map is not an unkeyed collection."
  [x] (TODO))

(-def nil? (value nil))

(dt/deftype MaybeSpec [spec #_t/spec?]
  {PSpec nil
   ?Fn {invoke ([this x] (c/or (c/nil? x) (spec x)))}
   fipp.ednize/IOverride nil
   fipp.ednize/IEdn
     {-edn ([this] (list `? spec))}})

(defn ?
  "`?` as-is (uncalled) denotes type inference should be performed.

   Arity 1: Computes a spec denoting a nilable value satisfying `spec`.
   Arity 2: Computes whether `x` is nil or satisfies `spec`."
  ([x] (MaybeSpec. (->spec x)))
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
    {ValueSpec
       {ValueSpec        (fn [s0 s1] (catch-all
                                       (unum/signum|long (c/compare s0 s1))
                                       nil))
        ClassSpec        v+c
        ProtocolSpec     (fn [s0 s1] (err! "TODO dispatch" {:s0 s0 :s1 s1}))
        MaybeSpec        (fn [s0 s1] (err! "TODO dispatch" {:s0 s0 :s1 s1}))
        OrSpec           v+o
        UnorderedOrSpec  (fn [s0 s1] (err! "TODO dispatch" {:s0 s0 :s1 s1}))
        AndSpec          v+a
        UnorderedAndSpec (fn [s0 s1] (err! "TODO dispatch" {:s0 s0 :s1 s1}))
        Expression       incomparable}
     ClassSpec
       {ValueSpec        (with-invert-comparison v+c)
        ClassSpec        (fn [s0 s1] (compare|class|class (.-c ^ClassSpec s0) (.-c ^ClassSpec s1)))
        ProtocolSpec     (fn [s0 s1] (err! "TODO dispatch" {:s0 s0 :s1 s1}))
        MaybeSpec        (fn [s0 s1] (err! "TODO dispatch" {:s0 s0 :s1 s1}))
        OrSpec           c+o
        UnorderedOrSpec  (fn [s0 s1] (err! "TODO dispatch" {:s0 s0 :s1 s1}))
        AndSpec          c+a
        UnorderedAndSpec (fn [s0 s1] (err! "TODO dispatch" {:s0 s0 :s1 s1}))
        Expression       incomparable}
     ProtocolSpec
       {ValueSpec        (fn [s0 s1] (err! "TODO dispatch" {:s0 s0 :s1 s1}))
        ClassSpec        (fn [s0 s1] (err! "TODO dispatch" {:s0 s0 :s1 s1}))
        ProtocolSpec     (fn [s0 s1] (err! "TODO dispatch" {:s0 s0 :s1 s1}))
        MaybeSpec        (fn [s0 s1] (err! "TODO dispatch" {:s0 s0 :s1 s1}))
        OrSpec           (fn [s0 s1] (err! "TODO dispatch" {:s0 s0 :s1 s1}))
        UnorderedOrSpec  (fn [s0 s1] (err! "TODO dispatch" {:s0 s0 :s1 s1}))
        AndSpec          (fn [s0 s1] (err! "TODO dispatch" {:s0 s0 :s1 s1}))
        UnorderedAndSpec (fn [s0 s1] (err! "TODO dispatch" {:s0 s0 :s1 s1}))
        Expression       incomparable}
     MaybeSpec
       {ValueSpec        (fn [s0 s1] (err! "TODO dispatch" {:s0 s0 :s1 s1}))
        ClassSpec        (fn [s0 s1] (err! "TODO dispatch" {:s0 s0 :s1 s1}))
        ProtocolSpec     (fn [s0 s1] (err! "TODO dispatch" {:s0 s0 :s1 s1}))
        MaybeSpec        (fn [s0 s1] (err! "TODO dispatch" {:s0 s0 :s1 s1}))
        OrSpec           (fn [s0 s1] (err! "TODO dispatch" {:s0 s0 :s1 s1}))
        UnorderedOrSpec  (fn [s0 s1] (err! "TODO dispatch" {:s0 s0 :s1 s1}))
        AndSpec          (fn [s0 s1] (err! "TODO dispatch" {:s0 s0 :s1 s1}))
        UnorderedAndSpec (fn [s0 s1] (err! "TODO dispatch" {:s0 s0 :s1 s1}))
        Expression       incomparable}
     OrSpec
       {ValueSpec        (with-invert-comparison v+o)
        ClassSpec        (with-invert-comparison c+o)
        ProtocolSpec     (fn [s0 s1] (err! "TODO dispatch" {:s0 s0 :s1 s1}))
        MaybeSpec        (fn [s0 s1] (err! "TODO dispatch" {:s0 s0 :s1 s1}))
        OrSpec           o+o
        UnorderedOrSpec  (fn [s0 s1] (err! "TODO dispatch" {:s0 s0 :s1 s1}))
        AndSpec          o+a
        UnorderedAndSpec (fn [s0 s1] (err! "TODO dispatch" {:s0 s0 :s1 s1}))
        Expression       incomparable}
     UnorderedOrSpec
       {ValueSpec        (fn [s0 s1] (err! "TODO dispatch" {:s0 s0 :s1 s1}))
        ClassSpec        (fn [s0 s1] (err! "TODO dispatch" {:s0 s0 :s1 s1}))
        ProtocolSpec     (fn [s0 s1] (err! "TODO dispatch" {:s0 s0 :s1 s1}))
        MaybeSpec        (fn [s0 s1] (err! "TODO dispatch" {:s0 s0 :s1 s1}))
        OrSpec           (fn [s0 s1] (err! "TODO dispatch" {:s0 s0 :s1 s1}))
        UnorderedOrSpec  (fn [s0 s1] (err! "TODO dispatch" {:s0 s0 :s1 s1}))
        AndSpec          (fn [s0 s1] (err! "TODO dispatch" {:s0 s0 :s1 s1}))
        UnorderedAndSpec (fn [s0 s1] (err! "TODO dispatch" {:s0 s0 :s1 s1}))
        Expression       incomparable}
     AndSpec
       {ValueSpec        (with-invert-comparison v+a)
        ClassSpec        (with-invert-comparison c+a)
        ProtocolSpec     (fn [s0 s1] (err! "TODO dispatch" {:s0 s0 :s1 s1}))
        MaybeSpec        (fn [s0 s1] (err! "TODO dispatch" {:s0 s0 :s1 s1}))
        OrSpec           (with-invert-comparison o+a)
        UnorderedOrSpec  (fn [s0 s1] (err! "TODO dispatch" {:s0 s0 :s1 s1}))
        AndSpec          (fn [s0 s1] (err! "TODO dispatch" {:s0 s0 :s1 s1}))
        UnorderedAndSpec (fn [s0 s1] (err! "TODO dispatch" {:s0 s0 :s1 s1}))
        Expression       incomparable}
     UnorderedAndSpec
       {ValueSpec        (fn [s0 s1] (err! "TODO dispatch" {:s0 s0 :s1 s1}))
        ClassSpec        (fn [s0 s1] (err! "TODO dispatch" {:s0 s0 :s1 s1}))
        ProtocolSpec     (fn [s0 s1] (err! "TODO dispatch" {:s0 s0 :s1 s1}))
        MaybeSpec        (fn [s0 s1] (err! "TODO dispatch" {:s0 s0 :s1 s1}))
        OrSpec           (fn [s0 s1] (err! "TODO dispatch" {:s0 s0 :s1 s1}))
        UnorderedOrSpec  (fn [s0 s1] (err! "TODO dispatch" {:s0 s0 :s1 s1}))
        AndSpec          (fn [s0 s1] (err! "TODO dispatch" {:s0 s0 :s1 s1}))
        UnorderedAndSpec (fn [s0 s1] (err! "TODO dispatch" {:s0 s0 :s1 s1}))
        Expression       incomparable}
     Expression
       {ValueSpec        incomparable
        ClassSpec        incomparable
        ProtocolSpec     incomparable
        MaybeSpec        incomparable
        OrSpec           incomparable
        UnorderedOrSpec  incomparable
        AndSpec          incomparable
        UnorderedAndSpec incomparable
        Expression       e+e}}))

)

(do

;; ===== PRIMITIVES ===== ;;

         (-def                             boolean?  #?(:clj Boolean :cljs js/Boolean))
         (-def                             ?boolean? (? boolean?))

#?(:clj  (-def                             byte?    Byte))
#?(:clj  (-def                             ?byte?   (? byte?)))

#?(:clj  (-def                             char?    Character))
#?(:clj  (-def                             ?char?   (? char?)))

#?(:clj  (-def                             short?   Short))
#?(:clj  (-def                             ?short?  (? short?)))

#?(:clj  (-def                             int?     Integer))
#?(:clj  (-def                             ?int?    (? int?)))

#?(:clj  (-def                             long?    Long))
#?(:clj  (-def                             ?long?   (? long?)))

#?(:clj  (-def                             float?   Float))
#?(:clj  (-def                             ?float?  (? float?)))

         (-def                             double?  #?(:clj Double :cljs js/Number))
         (-def                             ?double? (? double?))

         (-def primitive?                  (or boolean? #?@(:clj [byte? char? short? int? long? float?]) double?))

#_(:clj (-def comparable-primitive?       (and primitive? (not boolean?))))


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

;; ===== GENERAL ===== ;;

         (-def object?                     #?(:clj java.lang.Object :cljs js/Object))

         (-def any?                        (or nil? object? #?@(:cljs [js/String js/Symbol])))

;; ===== META ===== ;;

#?(:clj  (-def class?                      java.lang.Class))
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

         (-def literal?        (or nil? symbol? keyword? string? long? double? tagged-literal?))
#_(t/def ::form    (t/or ::literal t/list? t/vector? ...))
)
