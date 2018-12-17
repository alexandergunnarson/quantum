;; TODO look at this for type-checked Spec implementation: https://github.com/arohner/spectrum/blob/master/src/spectrum/conform.clj
;; TODO look at this thread for useful discussion on potential type systems in Clojure: https://groups.google.com/forum/#!topic/clojure/Dxk-rCVL5Ss
;; - Reach out to mikera and let him know about the type system when it's done since he was working on something vaguely similar
;; TODO look at HM impl : https://github.com/ericnormand/hindley-milner

Note that for anything built-in js/<whatever>, the `t/isa?` predicates might need some special help

;; TO MOVE

(defn equinumerous?
  [xs0 (t/input count :?), xs1 (t/input count :?) > boolean?] (= (count xs0) (count xs1)))

#?(:clj  (def thread?          (isa? java.lang.Thread)))
#?(:clj  (def class?           (isa? java.lang.Class)))
;; TODO for CLJS based on untyped impl
#?(:clj  (def protocol?        (>expr (ufn/fn-> :on-interface class?))))

;; ===== quantum.core.system

#?(:clj
(t/defn pid [> (? t/string?)]
  (->> (java.lang.management.ManagementFactory/getRuntimeMXBean)
       (.getName))))

;; TODO TYPED
(defalias u/*registered-components)

;; ===== UNKNOWN ===== ;;

(defnt >sentinel [> t/object?] #?(:clj (Object.) :cljs #js {}))
(defalias >object >sentinel)

;; TODO TYPED
#?(:clj
(defmacro with
  "Evaluates @expr, then @body, then returns @expr.
   For (side) effects."
  [expr #_t/form?, & body #_(? (t/seq-of t/form?))]
  `(let [expr# ~expr] ~@body expr#)))

;; Truncation is different from safe coercion
`>integer` is for e.g.:
- truncation e.g. js/Math.trunc

>boolean is different than `truthy?`

Sometimes you want (byte <whatever-double>) to fail at runtime rather than fail at compile time when you can't know everything about the input's range



#_"
Pinned:
- `;; TODO TYPED` is the annotation we're using for this initiative
- There will be some code duplication with untyped code for now and that's okay.
- No typed namespace should refer to any untyped namespace.
- The initial definition of conversion functions belongs in the namespace that their destination
  type belongs in, and it may be extended in every namespace in which there is a source type.
- These two should be defined in the `whatever` data namespace:
  - `>(whatever)`
  - `(whatever)>`
Legend:
- [.] : in progress
- [-] : done as far as possible but not truly complete
- [x] : actually done
- [|] : not possible / N/A
- [!] : refused

- TODO implement the following:
  [-] t/fn
      [-] Get `t/defn` working with `let*` + TypedFn way of doing things
      [ ] Sketch out `defnt/analyze-fn` using tests as a guide
      [ ] t/ftype should automatically split types, while perhaps t/ftype* should just assume
          they're split (for use by e.g. `t/fn` and `t/defn`)
      [ ] test t/fn to make sure meta 'sticks' : `(t/fn {...} [] ...)`
      [ ] support `ts` and `types` referring to closed-over local vars
          - Should do e.g. `(OrType. ... [t/boolean? (AndType. ...)])` to have minimal overhead
  [2] `?` : type inference
      - use logic programming and variable unification e.g. `?1` `?2` ?
      - For this situation: `?` is `(t/- <whatever-deduced-type> dc/counted?)`
        ([n dn/std-integer?, xs dc/counted?] (count xs))
        ([n dn/std-integer?, xs ?] ...)
      - [ ] No trailing `>` means `> ?`
  [3] inner expansion (see tests to see how this could work)
  [4] t/numerically : e.g. a double representing exactly what a float is able to represent
      - and variants thereof: `numerically-long?` etc.
      - t/numerically-integer?
      - Primitive conversions not requiring checks can go in data.primitive
        - core.data.numeric (requires data.primitive)
          - numeric definitions
          - numeric ranges
          - numeric characteristics
  [ ] Probably should disallow recursive type references, including:
      `(t/defn f [x (t/input f ...)])`
  [ ] Perhaps it's the case that we can't actually have type bases but rather reactive splits.
      In the case of `narrowest`, it expects a split and fails without it:
      `[a (t/- integer? int?), b integer? > (narrowest (t/type a) (t/type b))]`
  [ ] `t/ref`, `t/assume`, `t/run` need to be combined correctly with other types.
      E.g. (t/and (t/ref ...) ...) means the whole thing should be `t/ref`, while `(t/or (t/ref ...)
      (...))` does not mean the metadata is transferred. Probably `t/assume` and `t/run` should be
      combined in the same way.
      - What about `(t/and (t/or t/long? (t/ref t/byte?)) pos?)` ?
  [ ] t/value-of
      - `[x with-metable?, meta' meta? > (t/run with-metable?) #_(TODO TYPED (t/value-of x))]`
  [ ] (comp/t== x)
       - dependent type such that the passed input must be identical to x
  [ ] Non-boxed (primitive) `t/def`s: `(var/def- min-float  (Numeric/negate Float/MAX_VALUE))`
      - direct linking doesn't help with this; the way around this is seems to be to do `let` bindings for all captured non-dynamic vars, and unbox the var-values that are primitive
  [ ] `(t/validate x (t/run t/string?))` for `(t/run t/string?)` needs to be more performant
      - Don't re-create type on each call (see `defnt/unanalyzed-overload>overload`)
  [ ] replace `deref` with `ref/deref` in typed contexts? So we can do `@` still
  - Type Logic and Predicates
    - expressions (`quantum.untyped.core.analyze.expr`)
    - It is possible to check satisfaction of arities to an `t/ftype` at runtime even if the type
      meta is not stripped (well, at least the arity counts can be checked and primitive types in
      CLJ):
      - In CLJ  via e.g.:
        - `(clojure.reflect/reflect (fn ([]) ([^long a])))`
          -> {:members #{{:parameter-types [] ...} {:parameter-types [long] ...} ...}}
      - In CLJS via e.g.:
        - `(js/Object.getOwnPropertyNames (fn ([]) ([a])))`
          -> `#js [... \"cljs$core$IFn$_invoke$arity$0\" \"cljs$core$IFn$_invoke$arity$1\"]`
    - We should probably use DNF (https://en.wikipedia.org/wiki/Disjunctive_normal_form) with sorted
      arguments (on creation, order by `t/compare` descending) so we can correctly hash if we do
      spec lookup, and so we can split more correctly, and so we can perform faster equality checks
      - (t/and (t/or a b) c) should -> (t/or (t/and a c) (t/and b c))
    - `(or (and pred then) (and (not pred) else))` (which is not correct)
      - needs to equal `(t/and (t/or (t/not a) b) (t/or a c))` (which is correct)
      - `(- (or ?!+vector? !vector? #?(:clj !!vector?)) (isa? clojure.lang.Counted))` is not right
    - t/- : fix
      - (t/- (t/isa? java.util.Queue) (t/or ?!+queue? !!queue?))
      - (t/- t/any? p/float? p/double?); (t/- number? p/primitive?)
    - dc/of
      - (dc/of number?) ; implicitly the container is a `reducible?`, because that's now the
        fundamental traversal operation
        - Maybe we should replace or supplement with:
          - `(dc/* number?)` for any collection containing zero or more `number?`s
          - `(dc/+ number?)` for any collection containing at least one `number?`s
            - Equivalent to `(t/and (dc/* number?) (fn-> count (>= 1)))`
          - `(t/and vector? (dc/* number?))` for any collection containing zero or more `number?`s, which satisfies `vector?`
        - `t/finite` (reminiscent of `s/tuple`+`s/kv` but with different behavior)
          - This is for when we don't want a 'pattern' but rather want the actual thing
          - Instead of `(dc/* number?)` we might have `(t/finite number? (t/value 1) map?)`. The order
            of inputs to `t/finite` only matters if it's `t/and`ed with a collection type for which
            order matters (`(t/or dc/sequential? dc/sorted?)`):
            - (t/and vector? (t/finite (t/value :a) (t/value :b)))
              - must match `[:a :b]`, in that order
            - (t/and set?    (t/finite (t/value :a) (t/value :b)))
              - must match `#{:a :b}`, not necessarily in that order
            - (t/and map?    (t/finite (t/finite (t/value :a) (t/value :b))
                                       (t/finite (t/value :c) (t/value :d))))
              - must match `{:a :b :c :d}`, not necessarily in that kv-order
            - (t/and map?    (t/finite (t/and map-entry? (t/finite (t/value :a) (t/value :b)))
                                       (t/and vector?    (t/finite (t/value :c) (t/value :d)))))
              - must match `{:a :b :c :d}`, not necessarily in that kv-order
            - (t/and map?    (t/finite (t/value :a) (t/value :b)))
              - impossible since not all inputs satisfy `map-entry-like?`
            - (t/finite (t/value :a) (t/value :b))
              - is satisfied by e.g. [:a :b], (list :a :b), (!list :a :b), (<> :a :b), #{:a :b}
            - (t/finite (t/finite (t/value :a) (t/value :b)))
              - is satisfied by e.g. [[:a :b]], [(list :a :b)], (list [:a :b]), (!list [:a :b]),
                (<> [:a :b]), (<> (<> :a :b)), #{[:a :b]}, {:a :b}, (sorted-map :a :b)
      - (dc/of map/+map? symbol? dstr/string?)
      - (dc/of t/seq? namespace?)
      - dc/map-of
      - dc/seq-of
      - If we assert:
        - [:a :b :c] is equally representable as:
          - (t/seq vector? [  (t/value :a)   (t/value :b)   (t/value :c)])
          - (t/kv  vector? {0 (t/value :a) 1 (t/value :b) 2 (t/value :c)})
        - and (sorted-map 0 :a 1 :b 2 :c) as:
          - (t/kv  sorted-map? { 0 (t/value :a)   1 (t/value :b)   2 (t/value :c)})
          - (t/seq sorted-map? [[0 (t/value :a)] [1 (t/value :b)] [2 (t/value :c)]])
        - then we would have to assert (absurdly) that the following are equivalent representations:
          - (t/seq vector? [      (t/value :a)        (t/value :b)         (t/value :c)])
          - (t/kv  vector? {    0 (t/value :a)      1 (t/value :b)       2 (t/value :c)})
          - (t/seq vector? [   [0 (t/value :a)]    [1 (t/value :b)]     [2 (t/value :c)]])
          - (t/kv  vector? { 0 [0 (t/value :a)]  1 [1 (t/value :b)]   2 [2 (t/value :c)]})
        - and so on ad infinitum. Therefore we reserve `t/kv` for `(t/and t/lookup? (t/not indexed?))`.
  - TODO why are there two overloads in this case??
    (uana/pr! (uana/analyze `(defn ~'abcde [])))
  - Probably comparing a protocol with something else should be a matter for reactivity since
    protocols can be extended
    - TODO CLJS needs to implement it better
  [-] Analysis/Optimization
      [ ] ^:inline
          - A good example of inlining:
            (t/def empty?|rf
              (fn/aritoid
                (t/fn [] true)
                fn/identity
                (t/fn [ret _, x _]      (dc/reduced false))
                (t/fn [ret _, k _, v _] (dc/reduced false))))
            (t/defn empty? > p/boolean?
              ([x p/nil?] true)
              ([xs dc/counted?] (-> xs count num/zero?))
              ([xs (t/input educe :_ :_ :?)] (educe empty?|rf x)))
          - Should we allow something like `^:analyze-impl` or something to mimic inline optimizations
            but avoid actual inlining?
      - maybe redefine `untyped.core.type` in a typed way? `t/def` doesn't realize certain things are `t/type?`
      - dead code elimination
        - in `let*`, we should elide variables that are unused and that have no side effects (or at
          least warn)
      - `(p/nil? ...)` should probably be inlined to `(?/== ... nil)` rather than using the overhead
        of the deftype
      - This should realize that we're negating a `<` and change the operator to `<=`
        - `(t/def nneg? (fn/comp ?/not neg?))`
      - For numbers:
        - (< (compare a b) 0)
          ->
          (< (ifs (< a b) -1 (> a b) 1 0) 0)
          -> the only one that can be < 0 is the -1
          -> (< a b)
      - Better analysis of compound literals
        - Literal seqs need to be better analyzed — (t/finite-of t/built-in-list? [ak-type av-type] ...)
      - Peformance analysis (this comes very much later)
        - We should be able to do complexity analysis. Similarly to how we can combine and manipulate
          types, we could do like `(cplex/assume (cplex/o :n))` or `(cplex/assume (cplex/o :n2))` etc.
          - For `reduce` we'd always know it's up to N operations, so O(n * <complexity of `rf`>)
        - Record performance for each relevant part and cache?
      - (if (dcoll/reduced? ret)
            ;; TODO TYPED `(ref/deref ret)` should realize it's dealing with a `reduced?`
            (ref/deref ret)
            ...)
      - (let [ct (count arr)]
          (loop [i 0 v init]
            (if (comp/< i ct)
                (let [ret (f v (get arr i))]
                  (if (reduced? ret)
                      @ret
                      ;; TODO TYPED automatically figure out that `inc` will never go out of bounds here
                      (recur (inc* i) ret)))
                v)))
      - (let [xs' (seq xs)]
          (if (dcomp/== (class xs') (class xs))
              (reduce-seq rf ret xs')
              ;; TODO TYPED automatically figure out that:
              ;; - `(not (dcomp/== (class xs') (class xs)))`
              ;; - What the possible types of xs' are as a result
              (reduce rf init xs')))
      - ([rf rf?, init t/any?, xs #?(:clj  (t/isa? clojure.core.protocols/CollReduce)
                                     :cljs (t/isa?|direct cljs.core/IReduce))]
          ;; TODO add `^not-native` to `xs` for CLJS
          (#?(:clj  clojure.core.protocols/coll-reduce
              :cljs cljs.core/-reduce) xs rf init))
      - (if (A) ...) should be (if ^boolean (A) ...) if A returns a `p/boolean?`
  [ ] t/binding
      - We should make a special class or *something* like that to ensure that typed bindings are
        only bound within typed contexts.
  [ ] t/defrecord
  [ ] t/def-concrete-type (i.e. `t/deftype`)
  [-] t/ftype
      [ ] conditionally optional arities etc.
  [-] `t/defn`
      [ ] Should not accept `t/none?` as an input type
          - Arity elision: if any type in an arity is `t/none?` then elide it and emit a warning
            - `([x bigint?] x)`
      [ ] All `intern`s/effects in `t/defn` should be atomic (all or nothing). This means that the
          interns should probably be put on a queue too.
      [ ] `declare` but for `t/defn`
          - Currently we can declare that there is an fn, and what its output type is, and its
            metadata, but we cannot currently declare type-overloads. Experience will make clearer
            what to do in these cases.
      [ ] `|` (pre-types)
      [ ] t/defn-
          - Not just a private var for the dynamic dispatch, but needs to be private for purposes of
            the analyzer when doing direct dispatch. Should emit a warning, not just fail.
      [ ] handle varargs / variadic arity
          - [& args _] shouldn't result in `t/any?` but rather like `t/reducible?` or whatever
          - should configurably auto-generate arities and/or perform variadic proxying
      [ ] do the defnt-equivalences / `t/defn` test namespace
      [ ] a linting warning that you can narrow the type to whatever the deduced type is from whatever
          wider declared type there is
      [ ] the option of creating a `t/defn` that isn't extensible? Or at least in which the input
          types are limited in the same way per-overload output types are limited by the per-fn output
          type?
  [ ] t/defmacro
  [ ] t/deftype
  [-] t/extend-defn!
      [ ] Ability to add output type restriction after the fact?
  [ ] lazy compilation especially around `t/input`
      - Let's say you write a `t/defn` with a signature of [t/indexed? t/indexed? t/indexed?]. This
        would generate thousands of possibilities if eagerly loaded, but instead it is lazily loaded.
  [ ] equivalence of typed predicates (i.e. that which is t/<= `(t/ftype [x t/any? :> p/boolean?])`)
      to types:
      - [xs (t/fn [x (t/isa? clojure.lang.Range)] ...)]
- TODO transition the quantum.core.* namespaces:
  ->>>>>> TODO need to add *all* quantum namespaces in here
  - List of semi-approximately topologically ordered namespaces to make typed:
    - [.] clojure.core / cljs.core (note that many things unexpectedly have associated macros)
          - [! !] ..
          - [x x] <
          - [x x] <=
          - [x .] = — look at coercive-=
          - [x x] ==
          - [x x] >
          - [x x] >=
          - [. .] +
          - [. .] +'
          - [. .] -
          - [. .] -'
          - [! !] ->
          - [! !] ->>
          - [. .] *
          - [. .] *'
          - [. .] /
          - [! |] accessor
          - [x  ] aclone
          - [   ] add-tap
          - [   ] add-watch
          - [  |] agent
          - [   ] agent-error
          - [   ] aget — TODO check out unchecked-aget, checked-aget, checked-aget' and CLJS macro
          - [x x] alength
          - [   ] alias
          - [   ] all-ns
          - [   ] alter
          - [   ] alter-meta!
          - [   ] alter-var-root
          - [   ] amap
          - [   ] ancestors
          - [   ] and — NOTE that CLJS macro has some secrets
          - [   ] any?
          - [   ] apply
          - [   ] areduce
          - [|  ] array
          - [| .] array? — TODO also look at goog/isArrayLike
          - [|  ] array-chunk
          - [|  ] array-copy
          - [|  ] array-copy-downward
          - [|  ] array-index-of
          - [|  ] array-iter
          - [| !] array-list
          - [   ] array-map
          - [|  ] array-seq
          - [! !] as->
          - [   ] aset — TODO check out unchecked-aset, checked-aset, checked-aset' and CLJS macro
          - [   ] aset-boolean
          - [   ] aset-byte
          - [   ] aset-char
          - [   ] aset-double
          - [   ] aset-float
          - [   ] aset-int
          - [   ] aset-long
          - [   ] aset-short
          - [   ] assert
          - [! |] assert-args
          - [   ] assoc
          - [   ] assoc!
          - [   ] assoc-in
          - [x x] associative?
          - [   ] atom
          - [   ] await
          - [   ] await1
          - [   ] await-for
          - [   ] bases
          - [x -] bigdec
          - [x -] bigint
          - [x -] biginteger
          - [   ] binding
          - [   ] binding-conveyor-fn
          - [x x] bit-and
          - [! !] bit-and-not
          - [x x] bit-clear
          - [|  ] bit-count
          - [x x] bit-flip
          - [x x] bit-not
          - [x x] bit-or
          - [x x] bit-set
          - [x x] bit-shift-left
          - [x x] bit-shift-right
          - [| !] bit-shift-right-zero-fill
          - [x x] bit-test
          - [x x] bit-xor
          - [x .] boolean
          - [x x] boolean?
          - [   ] boolean-array
          - [   ] booleans
          - [   ] bound?
          - [   ] bound-fn
          - [   ] bound-fn*
          - [x x] bounded-count
          - [   ] butlast
          - [x .] byte
          - [x x] byte?
          - [   ] byte-array
          - [   ] bytes
          - [   ] bytes?
          - [   ] case
          - [  |] cast
          - [   ] cat
          - [x .] char — TODO (.fromCharCode js/String <number>) might be useful
          - [x x] char?
          - [   ] char-array
          - [   ] chars
          - [! |] check-valid-options
          - [x x] chunk
          - [x x] chunk-append
          - [x x] chunk-buffer
          - [x x] chunk-cons
          - [x x] chunk-first
          - [x x] chunk-next
          - [x x] chunk-rest
          - [x x] chunked-seq?
          - [|  ] chunkIteratorSeq
          - [  |] class
          - [x |] class?
          - [|  ] clj->js
          - [! |] clojure-version
          - [|  ] clone
          - [|  ] cloneable?
          - [|  ] coercive-=
          - [|  ] coercive-not
          - [|  ] coercive-not=
          - [   ] coll?
          - [   ] commute
          - [   ] comp
          - [. .] comparator
          - [x x] compare
          - [   ] compare-and-set!
          - [|  ] compare-indexed
          - [   ] compile
          - [   ] complement
          - [   ] completing
          - [   ] concat
          - [   ] cond
          - [! !] cond->
          - [! !] cond->>
          - [   ] condp
          - [   ] conj
          - [   ] conj!
          - [x  ] cons
          - [   ] constantly
          - [x  ] contains?
          - [|  ] copy-arguments
          - [x x] count
          - [x x] counted?
          - [   ] create-ns
          - [! !] create-struct
          - [   ] cycle
          - [x  ] dec
          - [x  ] dec'
          - [   ] declare
          - [x |] decimal?
          - [   ] dedupe
          - [|  ] default-dispatch-val
          - [! |] definline
          - [   ] defmacro
          - [! !] defmethod — rejected because t/defn supersedes
          - [! !] defmulti — rejected because t/defn supersedes
          - [. .] defn
          - [. .] defn-
          - [   ] defonce
          - [! !] defprotocol
          - [   ] defrecord
          - [! !] defstruct
          - [   ] deftype
          - [   ] delay
          - [x x] delay?
          - [   ] deliver
          - [|  ] demunge
          - [x |] denominator
          - [   ] deref
          - [   ] derive
          - [   ] descendants
          - [   ] destructure
          - [   ] disj
          - [   ] disj!
          - [|  ] dispatch-fn
          - [   ] dissoc
          - [   ] dissoc!
          - [   ] distinct
          - [   ] distinct?
          - [   ] doall
          - [   ] dorun
          - [   ] doseq
          - [   ] dosync
          - [   ] dotimes
          - [   ] doto
          - [x .] double
          - [x x] double?
          - [   ] double-array
          - [   ] doubles
          - [   ] drop
          - [   ] drop-last
          - [   ] drop-while
          - [   ] eduction
          - [  |] elide-top-frames
          - [   ] empty
          - [. .] empty?
          - [|  ] enable-console-print!
          - [   ] enumeration-seq
          - [   ] ensure
          - [   ] ensure-reduced
          - [|  ] equiv-map
          - [|  ] equiv-sequential
          - [   ] error-handler
          - [   ] error-mode
          - [|  ] es6-entries-iterator
          - [| !] es6-iterable
          - [|  ] es6-iterator
          - [|  ] es6-set-entries-iterator
          - [   ] eval
          - [   ] even?
          - [   ] every?
          - [   ] every-pred
          - [|  ] ex-cause
          - [   ] ex-data
          - [   ] ex-info
          - [|  ] ex-message
          - [|  ] exists?
          - [|  ] extend-object!
          - [! !] extend-protocol
          - [! !] extend-type
          - [x x] false?
          - [   ] file-seq
          - [   ] filter
          - [! |] filter-key
          - [   ] filterv
          - [x  ] find
          - [  |] find-keyword
          - [|  ] find-macros-ns
          - [x  ] find-ns
          - [|  ] find-ns-obj
          - [  |] find-var
          - [|  ] fix
          - [   ] ffirst
          - [x  ] first
          - [   ] flatten
          - [|  ] flatten1
          - [x .] float
          - [x x] float?
          - [   ] float-array
          - [   ] floats
          - [   ] flush
          - [. .] fn
          - [x x] fn?
          - [|  ] fn->comparator
          - [   ] fnext
          - [   ] fnil
          - [   ] for
          - [   ] force
          - [   ] format
          - [   ] frequencies
          - [   ] future
          - [   ] future?
          - [   ] future-call
          - [   ] future-cancel
          - [   ] future-cancelled?
          - [   ] future-done?
          - [|  ] gen-apply-to
          - [|  ] gen-apply-to-simple
          - [   ] gensym
          - [x  ] get
          - [   ] get-in
          - [   ] get-method
          - [   ] get-thread-bindings
          - [   ] get-validator
          - [   ] group-by
          - [   ] halt-when
          - [   ] hash
          - [|  ] hash-coll
          - [|  ] hash-combine
          - [|  ] hash-imap
          - [|  ] hash-iset
          - [|  ] hash-keyword
          - [   ] hash-map
          - [   ] hash-ordered-coll
          - [   ] hash-unordered-coll
          - [   ] hash-set
          - [|  ] hash-string*
          - [|  ] hash-string
          - [x x] ident?
          - [x x] identical?
          - [x x] identity
          - [   ] if-let
          - [   ] if-not (not as performant as we thought)
          - [   ] if-some
          - [|  ] ifind?
          - [x x] ifn?
          - [| !] implements?
          - [   ] import
          - [|  ] imul
          - [x  ] inc
          - [x  ] inc'
          - [x x] indexed?
          - [| x] infinite?
          - [   ] inst?
          - [   ] inst-ms
          - [   ] instance? — NOTE CLJS has macro
          - [x .] int
          - [x x] int?
          - [   ] int-array
          - [|  ] int-rotate-left
          - [   ] intern
          - [   ] into
          - [   ] ints
          - [x x] integer?
          - [   ] interleave
          - [   ] interpose
          - [   ] into-array
          - [  |] is-annotation?
          - [  |] is-runtime-annotation?
          - [   ] isa?
          - [|  ] iter
          - [| x] iterable?
          - [   ] iterate
          - [   ] iterator-seq
          - [   ] io!
          - [|  ] js-arguments
          - [|  ] js-comment
          - [|  ] js-debugger
          - [|  ] js-delete
          - [|  ] js-in
          - [|  ] js-inline-comment
          - [|  ] js-invoke
          - [|  ] js-keys
          - [|  ] js-mod
          - [|  ] js-obj
          - [|  ] js-reserved-arr
          - [|  ] js-str
          - [|  ] js->clj
          - [   ] juxt
          - [   ] keep
          - [   ] keep-indexed
          - [   ] key
          - [|  ] key->js
          - [x  ] keys
          - [x x] keyword
          - [x x] keyword?
          - [|  ] keyword-identical?
          - [   ] last
          - [   ] lazy-cat
          - [   ] lazy-seq
          - [   ] let
          - [! !] letfn — we just don't use it very much
          - [   ] line-seq
          - [   ] list
          - [   ] list*
          - [x x] list?
          - [   ] load
          - [   ] load-reader
          - [   ] load-string
          - [   ] loaded-libs
          - [  |] locking
          - [x .] long
          - [x x] long?
          - [   ] long-array
          - [   ] longs
          - [   ] loop
          - [|  ] m3-seed
          - [|  ] m3-C1
          - [|  ] m3-C2
          - [|  ] m3-mix-K1
          - [|  ] m3-mix-H1
          - [|  ] m3-fmix
          - [|  ] m3-hash-int
          - [|  ] m3-hash-unencoded-chars
          - [   ] macroexpand
          - [   ] macroexpand-1
          - [   ] make-array
          - [   ] make-hierarchy
          - [   ] map
          - [x x] map?
          - [x x] map-entry?
          - [   ] map-indexed
          - [   ] mapcat
          - [   ] mapv
          - [x x] max
          - [x x] max-key
          - [   ] memfn
          - [   ] memoize
          - [   ] merge
          - [   ] merge-with
          - [x x] meta
          - [   ] methods
          - [x x] min
          - [x x] min-key
          - [   ] mix-collection-hash
          - [|  ] mk-bound-fn
          - [   ] mod
          - [|  ] munge
          - [x x] name
          - [x x] namespace
          - [x  ] namespace?
          - [! |] nary-inline
          - [   ] nat-int?
          - [x x] neg?
          - [   ] neg-int?
          - [   ] newline
          - [x  ] next
          - [   ] nfirst
          - [x .] nil? — NOTE `nil?` macro in CLJS has some secrets
          - [|  ] nil-iter
          - [   ] nnext
          - [   ] not — look at `coercive-not`
          - [   ] not-any?
          - [   ] not-empty
          - [   ] not-every?
          - [x .] not= — look at `coercive-not=`
          - [   ] ns
          - [x |] ns-aliases
          - [x |] ns-imports
          - [x  ] ns-interns
          - [x |] ns-map
          - [x  ] ns-name
          - [x |] ns-publics
          - [x |] ns-refers
          - [   ] ns-resolve
          - [  |] ns-unalias
          - [x |] ns-unmap
          - [x  ] nth
          - [   ] nthnext
          - [   ] nthrest
          - [   ] num
          - [x x] number?
          - [x |] numerator
          - [|  ] obj-map
          - [| x] object?
          - [x  ] object-array
          - [   ] odd?
          - [   ] or — NOTE that CLJS macro has some secrets
          - [   ] parents
          - [   ] partial
          - [   ] partition
          - [   ] partition-all
          - [   ] partition-by
          - [   ] pcalls
          - [x  ] peek
          - [   ] persistent!
          - [|  ] persistent-array-map-seq
          - [   ] pmap
          - [x  ] pop
          - [   ] pop!
          - [   ] pop-thread-bindings
          - [x x] pos?
          - [   ] pos-int?
          - [   ] pr
          - [   ] pr-on
          - [| !] pr-seq-writer
          - [| !] pr-sequential-writer
          - [   ] pr-str
          - [|  ] pr-str*
          - [|  ] pr-str-with-opts
          - [   ] prefer-method
          - [   ] prefers
          - [   ] preserving-reduced
          - [|  ] prn-str-with-opts
          - [|  ] prim-seq
          - [   ] print
          - [! |] print-dup
          - [| !] print-meta?
          - [! |] print-method
          - [| !] print-prefix-map
          - [   ] print-str
          - [   ] printf
          - [   ] println
          - [   ] println-str
          - [   ] prn
          - [   ] prn-str
          - [   ] promise
          - [   ] push-thread-bindings
          - [   ] pvalues
          - [x x] qualified-ident?
          - [x x] qualified-keyword?
          - [x x] qualified-symbol?
          - [   ] quot
          - [|  ] quote-string
          - [   ] rand
          - [   ] rand-int
          - [   ] rand-nth
          - [   ] random-sample
          - [|  ] random-uuid
          - [   ] range
          - [|  ] ranged-iterator
          - [x |] ratio?
          - [   ] rational?
          - [   ] rationalize
          - [   ] re-find
          - [   ] re-groups
          - [   ] re-matcher
          - [   ] re-matches
          - [   ] re-pattern
          - [   ] re-seq
          - [   ] read
          - [   ] read-line
          - [   ] read-string
          - [   ] read+string
          - [   ] reader-conditional
          - [   ] reader-conditional?
          - [   ] realized?
          - [x x] record?
          - [x x] reduce
          - [x x] reduce-kv
          - [|  ] reduceable?
          - [x x] reduced
          - [x x] reduced?
          - [! |] reduce1
          - [   ] reductions
          - [   ] ref
          - [   ] ref-history-count
          - [   ] ref-min-history
          - [   ] ref-max-history
          - [   ] ref-set
          - [   ] refer
          - [   ] refer-clojure
          - [| x] regexp?
          - [|  ] reify
          - [   ] release-pending-sends
          - [   ] rem
          - [   ] remove
          - [   ] remove-all-methods
          - [   ] remove-method
          - [  |] remove-ns
          - [   ] remove-tap
          - [   ] remove-watch
          - [   ] repeat
          - [   ] repeatedly
          - [   ] replace
          - [   ] require
          - [| !] require-macros
          - [   ] reset!
          - [   ] reset-meta!
          - [   ] reset-vals!
          - [   ] resolve
          - [x  ] rest
          - [   ] restart-agent
          - [   ] resultset-seq
          - [   ] reverse
          - [   ] reversible?
          - [   ] rseq
          - [   ] rsubseq
          - [   ] run!
          - [! !] satisfies?
          - [   ] second
          - [   ] select-keys
          - [   ] send
          - [   ] send-off
          - [   ] send-via
          - [x x] seq
          - [x x] seq?
          - [|  ] seq-iter
          - [   ] seqable?
          - [   ] seque
          - [   ] sequence
          - [   ] sequential?
          - [   ] set
          - [x x] set?
          - [   ] set-agent-send-executor!
          - [   ] set-agent-send-off-executor!
          - [   ] set-error-handler!
          - [   ] set-error-mode!
          - [|  ] set-from-indexed-seq
          - [|  ] set-print-err-fn!
          - [|  ] set-print-fn!
          - [   ] set-validator!
          - [   ] setup-reference
          - [x .] short
          - [x x] short?
          - [   ] short-array
          - [   ] shorts
          - [   ] shuffle
          - [   ] shutdown-agents
          - [| !] simple-benchmark
          - [x x] simple-ident?
          - [x x] simple-keyword?
          - [x x] simple-symbol?
          - [   ] slurp
          - [   ] some
          - [x x] some?
          - [   ] some->
          - [   ] some->>
          - [   ] some-fn
          - [   ] sort
          - [   ] sort-by
          - [x x] sorted?
          - [   ] sorted-map
          - [   ] sorted-map-by
          - [   ] sorted-set
          - [   ] sorted-set-by
          - [   ] special-symbol?
          - [|  ] specify
          - [|  ] specify!
          - [   ] spit
          - [   ] split-at
          - [   ] split-with
          - [   ] spread
          - [. .] str
          - [x x] string?
          - [|  ] string-iter
          - [| !] string-print
          - [|  ] strip-ns
          - [! |] struct
          - [! |] struct-map
          - [   ] subs
          - [   ] subseq
          - [   ] subvec
          - [   ] supers
          - [   ] swap!
          - [   ] swap-vals!
          - [x x] symbol
          - [x x] symbol?
          - [|  ] symbol-identical?
          - [   ] sync
          - [|  ] system-time
          - [   ] tagged-literal
          - [   ] tagged-literal?
          - [   ] take
          - [   ] take-last
          - [   ] take-nth
          - [   ] take-while
          - [   ] tap>
          - [   ] test
          - [x |] the-ns
          - [| !] this-as
          - [   ] thread-bound?
          - [   ] throw-if
          - [   ] time
          - [x  ] to-array
          - [   ] to-array-2d
          - [   ] trampoline
          - [. .] transduce
          - [|  ] transformer-iterator
          - [   ] transient
          - [   ] tree-seq
          - [x x] true?
          - [   ] type
          - [|  ] type->str
          - [x  ] unchecked-add
          - [x  ] unchecked-add-int
          - [x .] unchecked-byte
          - [x .] unchecked-char
          - [x  ] unchecked-dec
          - [x  ] unchecked-dec-int
          - [x  ] unchecked-divide
          - [x  ] unchecked-divide-int
          - [x .] unchecked-double
          - [x .] unchecked-float
          - [x  ] unchecked-inc
          - [x  ] unchecked-inc-int
          - [x .] unchecked-int
          - [x .] unchecked-long
          - [x  ] unchecked-multiply
          - [x  ] unchecked-multiply-int
          - [x  ] unchecked-negate
          - [x  ] unchecked-negate-int
          - [x  ] unchecked-remainder-int
          - [x .] unchecked-short
          - [x  ] unchecked-subtract
          - [x  ] unchecked-subtract-int
          - [|  ] undefined? — NOTE has macro too
          - [   ] underive
          - [   ] unreduced
          - [| !] unsafe-cast
          - [x .] unsigned-bit-shift-right
          - [   ] update
          - [   ] update-in
          - [   ] uri?
          - [! !] use
          - [| !] use-macros
          - [|  ] uuid
          - [x x] uuid?
          - [   ] val
          - [x  ] vals
          - [x x] var?
          - [   ] var-get
          - [   ] var-set
          - [. .] vary-meta
          - [   ] vec
          - [   ] vector
          - [x x] vector?
          - [x x] volatile?
          - [   ] volatile!
          - [   ] vreset!
          - [   ] vswap!
          - [! !] when
          - [   ] when-first
          - [   ] when-let
          - [! !] when-not
          - [   ] when-some
          - [! !] while
          - [   ] with-bindings
          - [   ] with-bindings*
          - [   ] with-in-str
          - [   ] with-loading-context
          - [   ] with-local-vars
          - [x x] with-meta
          - [   ] with-open
          - [   ] with-out-str
          - [   ] with-precision
          - [   ] with-redefs
          - [   ] with-redefs-fn
          - [| !] write-all
          - [   ] xml-seq
          - [x x] zero?
          - [   ] zipmap
    - [.] Intrinsics
          https://github.com/clojure/clojure/blob/master/src/jvm/clojure/lang/Intrinsics.java
          (Clojure 1.10)
          - [ ] Numbers.add(double,double)
          - [x] Numbers.and(long,long)
          - [ ] Numbers.divide(double,double)
          - [x] Numbers.equiv(double,double)
          - [x] Numbers.equiv(long,long)
          - [x] Numbers.gt(long,long)
          - [x] Numbers.gt(double,double)
          - [x] Numbers.gte(long,long)
          - [x] Numbers.gte(double,double)
          - [x] Numbers.isPos(long)
          - [x] Numbers.isPos(double)
          - [x] Numbers.isNeg(long)
          - [x] Numbers.isNeg(double)
          - [x] Numbers.isZero(long)
          - [x] Numbers.isZero(double)
          - [x] Numbers.lt(long,long)
          - [x] Numbers.lt(double,double)
          - [x] Numbers.lte(long,long)
          - [x] Numbers.lte(double,double)
          - [ ] Numbers.minus(double)
          - [ ] Numbers.minus(double,double)
          - [ ] Numbers.multiply(double,double)
          - [x] Numbers.or(long,long)
          - [x] Numbers.xor(long,long)
          - [ ] Numbers.remainder(long,long)
          - [ ] Numbers.inc(double)
          - [ ] Numbers.dec(double)
          - [ ] Numbers.quotient(long,long)
          - [x] Numbers.shiftLeftInt(int,int)
          - [x] Numbers.shiftLeft(long,long)
          - [x] Numbers.shiftRightInt(int,int)
          - [x] Numbers.shiftRight(long,long)
          - [x] Numbers.unsignedShiftRightInt(int,int)
          - [x] Numbers.unsignedShiftRight(long,long)
          - [ ] Numbers.unchecked_int_add(int,int)
          - [ ] Numbers.unchecked_int_subtract(int,int)
          - [ ] Numbers.unchecked_int_negate(int)
          - [ ] Numbers.unchecked_int_inc(int)
          - [ ] Numbers.unchecked_int_dec(int)
          - [ ] Numbers.unchecked_int_multiply(int,int)
          - [ ] Numbers.unchecked_int_divide(int,int)
          - [ ] Numbers.unchecked_int_remainder(int,int)
          - [ ] Numbers.unchecked_add(long,long)
          - [ ] Numbers.unchecked_add(double,double)
          - [ ] Numbers.unchecked_minus(long)
          - [ ] Numbers.unchecked_minus(double)
          - [ ] Numbers.unchecked_minus(double,double)
          - [ ] Numbers.unchecked_minus(long,long)
          - [ ] Numbers.unchecked_multiply(long,long)
          - [ ] Numbers.unchecked_multiply(double,double)
          - [ ] Numbers.unchecked_inc(double)
          - [ ] Numbers.unchecked_inc(long)
          - [ ] Numbers.unchecked_dec(double)
          - [ ] Numbers.unchecked_dec(long)
          - [ ] RT.aget(short[],int)
          - [ ] RT.aget(float[],int)
          - [ ] RT.aget(double[],int)
          - [ ] RT.aget(int[],int)
          - [ ] RT.aget(long[],int)
          - [ ] RT.aget(char[],int)
          - [ ] RT.aget(byte[],int)
          - [ ] RT.aget(boolean[],int)
          - [ ] RT.aget(java.lang.Object[],int)
          - [ ] RT.alength(int[])
          - [ ] RT.alength(long[])
          - [ ] RT.alength(char[])
          - [ ] RT.alength(java.lang.Object[])
          - [ ] RT.alength(byte[])
          - [ ] RT.alength(float[])
          - [ ] RT.alength(short[])
          - [ ] RT.alength(boolean[])
          - [ ] RT.alength(double[])
          - [ ] RT.longCast(byte)
          - [ ] RT.longCast(short)
          - [ ] RT.longCast(int)
          - [!] RT.longCast(long)
          - [ ] RT.doubleCast(byte)
          - [ ] RT.doubleCast(short)
          - [ ] RT.doubleCast(int)
          - [ ] RT.doubleCast(long)
          - [ ] RT.doubleCast(float)
          - [!] RT.doubleCast(double)
          - [x] RT.uncheckedIntCast(byte)
          - [x] RT.uncheckedIntCast(short)
          - [x] RT.uncheckedIntCast(char)
          - [!] RT.uncheckedIntCast(int)
          - [x] RT.uncheckedIntCast(long)
          - [x] RT.uncheckedIntCast(float)
          - [x] RT.uncheckedIntCast(double)
          - [x] RT.uncheckedLongCast(byte)
          - [x] RT.uncheckedLongCast(short)
          - [x] RT.uncheckedLongCast(int)
          - [!] RT.uncheckedLongCast(long)
          - [x] RT.uncheckedLongCast(float)
          - [x] RT.uncheckedLongCast(double)
          - [x] RT.uncheckedDoubleCast(byte)
          - [x] RT.uncheckedDoubleCast(short)
          - [x] RT.uncheckedDoubleCast(int)
          - [x] RT.uncheckedDoubleCast(long)
          - [x] RT.uncheckedDoubleCast(float)
          - [!] RT.uncheckedDoubleCast(double)
          - [!] Util.equiv(long,long)
          - [x] Util.equiv(boolean,boolean)
          - [!] Util.equiv(double,double)
    - [ ] JS built-in functions (the most common/relevant ones)
          - ...
    - [ ] Java intrinsics
          http://hg.openjdk.java.net/jdk8/jdk8/hotspot/file/87ee5ee27509/src/share/vm/classfile/vmSymbols.hpp
          http://hg.openjdk.java.net/jdk9/jdk9/hotspot/file/b756e7a2ec33/src/share/vm/classfile/vmSymbols.hpp
          http://hg.openjdk.java.net/jdk10/jdk10/hotspot/file/5ab7a67bc155/src/share/vm/classfile/vmSymbols.hpp
          Those marked with a number or numbers mean they are specific to only those JDK versions.
          Only starts at Java 8.
          Note that Java 10 didn't add any intrinsics.
          Unsafe = sun.misc.Unsafe for Java 8
          Unsafe = jdk.internal.misc.Unsafe for Java >= 9
          - [ ] <Object>.hashCode() > int
          - [ ] System.identityHashCode(Object) > int
          - [ ] <Object>.getClass() > Class
          - [ ] <Object>.clone()
          - [ ] >=9 : <Object>.notify()
          - [ ] >=9 : <Object>.notifyAll()
          - [ ] System.currentTimeMillis() > int
          - [ ] System.nanoTime() > int
          - [ ] Math.abs(double) > double
          - [ ] Math.sin(double) > double
          - [ ] Math.cos(double) > double
          - [ ] Math.tan(double) > double
          - [ ] Math.atan2(double, double) > double
          - [ ] Math.sqrt(double) > double
          - [ ] Math.log(double) > double
          - [ ] Math.log10(double) > double
          - [ ] Math.pow(double, double) > double
          - [ ] Math.exp(double) > double
          - [x] Math.min(int, int) > int
          - [x] Math.max(int, int) > int
          - [ ] Math.addExact(int, int) > int
          - [ ] Math.addExact(long, long) > long
          - [ ] Math.decrementExact(int) > int
          - [ ] Math.decrementExact(long, long) > long
          - [ ] Math.incrementExact(int) > int
          - [ ] Math.incrementExact(long, long) > long
          - [ ] Math.multiplyExact(int, int) > int
          - [ ] Math.multiplyExact(long, long) > long
          - [ ] Math.negateExact(int) > int
          - [ ] Math.negateExact(long) > long
          - [ ] Math.subtractExact(int, int) > int
          - [ ] Math.subtractExact(long, long) > long
          - [ ] >=9 : Math.fma(float, float, float) > float
          - [ ] >=9 : Math.fma(double, double, double) > double
          - [ ] >=9 : BigInteger.implMultiplyToLen(ints, int, ints, int, ints) > ints
          - [ ] >=9 : BigInteger.implSquareToLen(ints, int, ints, int) > ints
          - [ ] >=9 : BigInteger.implMulAdd(ints, ints, int, int, int) > int
          - [ ] >=9 : BigInteger.implMontgomeryMultiply(ints, ints, ints, int, long, ints) > ints
          - [ ] >=9 : BigInteger.implMontgomerySquare(ints, ints, int, long, ints) > ints
          - [ ] Float.floatToRawIntBits(float) > int
          - [ ] Float.floatToIntBits(float) > int
          - [ ] Float.intBitsToFloat(int) > float
          - [ ] Double.doubleToRawLongBits(double) > long
          - [ ] Double.doubleToLongBits(double) > long
          - [ ] Double.longBitsToDouble(long) > double
          - [ ] Integer.numberOfLeadingZeros(int) > int
          - [ ] Long.numberOfLeadingZeros(long) > long
          - [ ] Integer.numberOfTrailingZeros(int) > int
          - [ ] Long.numberOfTrailingZeros(long) > long
          - [ ] Integer.bitCount(int) > int
          - [ ] Long.bitCount(long) > int
          - [ ] Short.reverseBytes(short) > short
          - [ ] Character.reverseBytes(char) > char
          - [ ] Integer.reverseBytes(int) > int
          - [ ] Long.reverseBytes(long) > long
          - [ ] System.arrayCopy(objects, int, objects, int, int)
          - [ ] <Class>.getComponentType() > Class
          - [ ] <Class>.getModifiers() > int
          - [ ] <Class>.getSuperclass() > Class
          - [ ] <Class>.isArray() > boolean
          - [ ] <Class>.isAssignableFrom(Class) > boolean
          - [ ] <Class>.isInstance(Class) > boolean
          - [ ] <Class>.isInterface() > boolean
          - [ ] <Class>.isPrimitive() > boolean
          - [ ] >=9 : <Class>.cast(Object) > Object
          - [ ] java.lang.reflect.Array.getLength(Object) > int
          - [ ] java.lang.reflect.Array.newArray(Class, int) > Object
          - [ ] <java.nio.Buffer>.checkIndex(int) > int
          - [ ] >=9 : jdk.internal.util.Preconditions.checkIndex(int, int, java.util.function.BiFunction) > int
          - [ ] java.util.Arrays.copyOf(objects, int, Class) > objects
          - [ ] java.util.Arrays.copyOfRange(objects, int, int, Class) > objects
          - [ ] java.util.Arrays.equals(chars, chars) > boolean
          - [ ] java.util.ArraysSupport.vectorizedMismatch(Object, long, Object, long, int, int) > int
          - [x] <String>.compareTo(String) > int
          - [x] <String>.equals(Object) > boolean
          - [ ] <String>.indexOf(String) > int
          - [ ] sun.reflect.Reflection.getCallerClass() > Class
          - [ ] sun.reflect.Reflection.getClassAccessFlags(Class) > int
          - [ ] Thread.currentThread() > Thread
          - [ ] Thread.isInterrupted(boolean) > boolean
          - [ ] >=9 : Thread.onSpinWait()
          - [ ] <java.lang.ref.Reference>.get() > Object
          - [ ] 8   : <com.sun.crypto.provider.AESCrypt>.decryptBlock(bytes, int, bytes, int)
          - [ ] 8   : <com.sun.crypto.provider.AESCrypt>.encryptBlock(bytes, int, bytes, int)
          - [ ] >=9 : <com.sun.crypto.provider.AESCrypt>.implDecryptBlock(bytes, int, bytes, int)
          - [ ] >=9 : <com.sun.crypto.provider.AESCrypt>.implEncryptBlock(bytes, int, bytes, int)
          - [ ] 8   : <com.sun.crypto.provider.CipherBlockChaining>.decrypt(bytes, int, int, bytes, int)
          - [ ] 8   : <com.sun.crypto.provider.CipherBlockChaining>.encrypt(bytes, int, int, bytes, int)
          - [ ] >=9 : <com.sun.crypto.provider.CipherBlockChaining>.implDecrypt(bytes, int, int, bytes, int)
          - [ ] >=9 : <com.sun.crypto.provider.CipherBlockChaining>.implEncrypt(bytes, int, int, bytes, int)
          - [ ] >=9 : <com.sun.crypto.provider.CounterMode>.implCrypt(bytes, int, int, bytes, int) > int
          - [ ] >=9 : <com.sun.security.provider.SHA>.implCompress0(bytes, int)
          - [ ] >=9 : <com.sun.security.provider.SHA2>.implCompress0(bytes, int)
          - [ ] >=9 : <com.sun.security.provider.SHA5>.implCompress0(bytes, int)
          - [ ] >=9 : <com.sun.security.provider.DigestBase>.implCompressMultiBlock0(bytes, int, int) > int
          - [ ] >=9 : com.sun.crypto.provider.GHASH.processBlocks(bytes, int, int, longs, longs)
          - [ ] <java.util.zip.CRC32>.update(int, int) > int
          - [ ] 8   : java.util.zip.CRC32.updateByteBuffer(int, long, int, int) > int
          - [ ] >=9 : java.util.zip.CRC32.updateByteBuffer0(int, long, int, int) > int
          - [ ] 8   : java.util.zip.CRC32.updateBytes(int, bytes, int, int) > int
          - [ ] >=9 : java.util.zip.CRC32.updateBytes0(int, bytes, int, int) > int
          - [ ] >=9 : java.util.zip.CRC32C.updateBytes(int, bytes, int, int) > int
          - [ ] >=9 : java.util.zip.CRC32C.updateDirectByteBuffer(int, long, int, int) > int
          - [ ] >=9 : java.util.zip.Adler32.updateBytes(int, bytes, int, int) > int
          - [ ] >=9 : java.util.zip.Adler32.updateByteBuffer(int, long, int, int) > int
          - [ ] <Unsafe>.allocateInstance(Class) > Object
          - [ ] >=9 : <Unsafe>.allocateUninitializedArray0(Class, int) > Object
          - [ ] 8   : <Unsafe>.copyMemory(Object, long, Object, long, long)
          - [ ] >=9 : <Unsafe>.copyMemory0(Object, long, Object, long, long)
          - [ ] <Unsafe>.park(boolean, long)
          - [ ] <Unsafe>.unpark(Object)
          - [ ] <Unsafe>.loadFence()
          - [ ] <Unsafe>.storeFence()
          - [ ] <Unsafe>.fullFence()
          - [ ] <Unsafe>.getObject (Object, long         ) > Object
          - [ ] <Unsafe>.putObject (Object, long, Object )
          - [ ] <Unsafe>.getBoolean(Object, long         ) > boolean
          - [ ] <Unsafe>.putBoolean(Object, long, boolean)
          - [ ] <Unsafe>.getByte   (Object, long         ) > byte
          - [ ] <Unsafe>.putByte   (Object, long, byte   )
          - [ ] <Unsafe>.getShort  (Object, long         ) > short
          - [ ] <Unsafe>.putShort  (Object, long, short  )
          - [ ] <Unsafe>.getChar   (Object, long         ) > char
          - [ ] <Unsafe>.putChar   (Object, long, char   )
          - [ ] <Unsafe>.getInt    (Object, long         ) > int
          - [ ] <Unsafe>.putInt    (Object, long, int    )
          - [ ] <Unsafe>.getLong   (Object, long         ) > long
          - [ ] <Unsafe>.putLong   (Object, long, long   )
          - [ ] <Unsafe>.getFloat  (Object, long         ) > float
          - [ ] <Unsafe>.putFloat  (Object, long, float  )
          - [ ] <Unsafe>.getDouble (Object, long         ) > double
          - [ ] <Unsafe>.putDouble (Object, long, double )
          - [ ] <Unsafe>.getObjectVolatile (Object, long         ) > Object
          - [ ] <Unsafe>.putObjectVolatile (Object, long, Object )
          - [ ] <Unsafe>.getBooleanVolatile(Object, long         ) > boolean
          - [ ] <Unsafe>.putBooleanVolatile(Object, long, boolean)
          - [ ] <Unsafe>.getByteVolatile   (Object, long         ) > byte
          - [ ] <Unsafe>.putByteVolatile   (Object, long, byte   )
          - [ ] <Unsafe>.getShortVolatile  (Object, long         ) > short
          - [ ] <Unsafe>.putShortVolatile  (Object, long, short  )
          - [ ] <Unsafe>.getCharVolatile   (Object, long         ) > char
          - [ ] <Unsafe>.putCharVolatile   (Object, long, char   )
          - [ ] <Unsafe>.getIntVolatile    (Object, long         ) > int
          - [ ] <Unsafe>.putIntVolatile    (Object, long, int    )
          - [ ] <Unsafe>.getLongVolatile   (Object, long         ) > long
          - [ ] <Unsafe>.putLongVolatile   (Object, long, long   )
          - [ ] <Unsafe>.getFloatVolatile  (Object, long         ) > float
          - [ ] <Unsafe>.putFloatVolatile  (Object, long, float  )
          - [ ] <Unsafe>.getDoubleVolatile (Object, long         ) > double
          - [ ] <Unsafe>.putDoubleVolatile (Object, long, double )
          - [ ] <Unsafe>.getObjectVolatile (Object, long         ) > Object
          - [ ] <Unsafe>.putObjectVolatile (Object, long, Object )
          - [ ] >=9 : <Unsafe>.getBooleanOpaque (Object, long         ) > boolean
          - [ ] >=9 : <Unsafe>.putBooleanOpaque (Object, long, boolean)
          - [ ] >=9 : <Unsafe>.getByteOpaque    (Object, long         ) > byte
          - [ ] >=9 : <Unsafe>.putByteOpaque    (Object, long, byte   )
          - [ ] >=9 : <Unsafe>.getShortOpaque   (Object, long         ) > short
          - [ ] >=9 : <Unsafe>.putShortOpaque   (Object, long, short  )
          - [ ] >=9 : <Unsafe>.getCharOpaque    (Object, long         ) > char
          - [ ] >=9 : <Unsafe>.putCharOpaque    (Object, long, char   )
          - [ ] >=9 : <Unsafe>.getIntOpaque     (Object, long         ) > int
          - [ ] >=9 : <Unsafe>.putIntOpaque     (Object, long, int    )
          - [ ] >=9 : <Unsafe>.getLongOpaque    (Object, long         ) > long
          - [ ] >=9 : <Unsafe>.putLongOpaque    (Object, long, long   )
          - [ ] >=9 : <Unsafe>.getFloatOpaque   (Object, long         ) > float
          - [ ] >=9 : <Unsafe>.putFloatOpaque   (Object, long, float  )
          - [ ] >=9 : <Unsafe>.getDoubleOpaque  (Object, long         ) > double
          - [ ] >=9 : <Unsafe>.putDoubleOpaque  (Object, long, double )
          - [ ] >=9 : <Unsafe>.getBooleanRelease(Object, long         ) > boolean
          - [ ] >=9 : <Unsafe>.putBooleanRelease(Object, long, boolean)
          - [ ] >=9 : <Unsafe>.getByteRelease   (Object, long         ) > byte
          - [ ] >=9 : <Unsafe>.putByteRelease   (Object, long, byte   )
          - [ ] >=9 : <Unsafe>.getShortRelease  (Object, long         ) > short
          - [ ] >=9 : <Unsafe>.putShortRelease  (Object, long, short  )
          - [ ] >=9 : <Unsafe>.getCharRelease   (Object, long         ) > char
          - [ ] >=9 : <Unsafe>.putCharRelease   (Object, long, char   )
          - [ ] >=9 : <Unsafe>.getIntRelease    (Object, long         ) > int
          - [ ] >=9 : <Unsafe>.putIntRelease    (Object, long, int    )
          - [ ] >=9 : <Unsafe>.getLongRelease   (Object, long         ) > long
          - [ ] >=9 : <Unsafe>.putLongRelease   (Object, long, long   )
          - [ ] >=9 : <Unsafe>.getFloatRelease  (Object, long         ) > float
          - [ ] >=9 : <Unsafe>.putFloatRelease  (Object, long, float  )
          - [ ] >=9 : <Unsafe>.getDoubleRelease (Object, long         ) > double
          - [ ] >=9 : <Unsafe>.putDoubleRelease (Object, long, double )
          - [ ] >=9 : <Unsafe>.getBooleanAcquire(Object, long         ) > boolean
          - [ ] >=9 : <Unsafe>.putBooleanAcquire(Object, long, boolean)
          - [ ] >=9 : <Unsafe>.getByteAcquire   (Object, long         ) > byte
          - [ ] >=9 : <Unsafe>.putByteAcquire   (Object, long, byte   )
          - [ ] >=9 : <Unsafe>.getShortAcquire  (Object, long         ) > short
          - [ ] >=9 : <Unsafe>.putShortAcquire  (Object, long, short  )
          - [ ] >=9 : <Unsafe>.getCharAcquire   (Object, long         ) > char
          - [ ] >=9 : <Unsafe>.putCharAcquire   (Object, long, char   )
          - [ ] >=9 : <Unsafe>.getIntAcquire    (Object, long         ) > int
          - [ ] >=9 : <Unsafe>.putIntAcquire    (Object, long, int    )
          - [ ] >=9 : <Unsafe>.getLongAcquire   (Object, long         ) > long
          - [ ] >=9 : <Unsafe>.putLongAcquire   (Object, long, long   )
          - [ ] >=9 : <Unsafe>.getFloatAcquire  (Object, long         ) > float
          - [ ] >=9 : <Unsafe>.putFloatAcquire  (Object, long, float  )
          - [ ] >=9 : <Unsafe>.getDoubleAcquire (Object, long         ) > double
          - [ ] >=9 : <Unsafe>.putDoubleAcquire (Object, long, double )
          - [ ] >=9 : <Unsafe>.getShortUnaligned(Object, long         ) > short
          - [ ] >=9 : <Unsafe>.putShortUnaligned(Object, long, short  )
          - [ ] >=9 : <Unsafe>.getCharUnaligned (Object, long         ) > char
          - [ ] >=9 : <Unsafe>.putCharUnaligned (Object, long, char   )
          - [ ] >=9 : <Unsafe>.getIntUnaligned  (Object, long         ) > int
          - [ ] >=9 : <Unsafe>.putIntUnaligned  (Object, long, int    )
          - [ ] >=9 : <Unsafe>.getLongUnaligned (Object, long         ) > long
          - [ ] >=9 : <Unsafe>.putLongUnaligned (Object, long, long   )
          - [ ] 8 : <Unsafe>.getObject (long         ) > Object
          - [ ] 8 : <Unsafe>.putObject (long, Object )
          - [ ] 8 : <Unsafe>.getBoolean(long         ) > boolean
          - [ ] 8 : <Unsafe>.putBoolean(long, boolean)
          - [ ] 8 : <Unsafe>.getByte   (long         ) > byte
          - [ ] 8 : <Unsafe>.putByte   (long, byte   )
          - [ ] 8 : <Unsafe>.getShort  (long         ) > short
          - [ ] 8 : <Unsafe>.putShort  (long, short  )
          - [ ] 8 : <Unsafe>.getChar   (long         ) > char
          - [ ] 8 : <Unsafe>.putChar   (long, char   )
          - [ ] 8 : <Unsafe>.getInt    (long         ) > int
          - [ ] 8 : <Unsafe>.putInt    (long, int    )
          - [ ] 8 : <Unsafe>.getLong   (long         ) > long
          - [ ] 8 : <Unsafe>.putLong   (long, long   )
          - [ ] 8 : <Unsafe>.getFloat  (long         ) > float
          - [ ] 8 : <Unsafe>.putFloat  (long, float  )
          - [ ] 8 : <Unsafe>.getDouble (long         ) > double
          - [ ] 8 : <Unsafe>.putDouble (long, double )
          - [ ] 8 : <Unsafe>.getAddress(long         ) > long
          - [ ] 8 : <Unsafe>.putAddress(long, long   )
          - [ ] 8 : <Unsafe>.compareAndSwapInt   (Object, long, int   , int   ) > boolean
          - [ ] 8 : <Unsafe>.compareAndSwapLong  (Object, long, long  , long  ) > boolean
          - [ ] 8 : <Unsafe>.compareAndSwapObject(Object, long, Object, Object) > boolean
          - [ ] 8 : <Unsafe>.putOrderedInt       (Object, long, int)
          - [ ] 8 : <Unsafe>.putOrderedLong      (Object, long, long)
          - [ ] 8 : <Unsafe>.putOrderedObject    (Object, long, Object)
          - [ ] >=9 : <Unsafe>.compareAndSetByte              (Object, long, byte  , byte  ) > boolean
          - [ ] >=9 : <Unsafe>.weakCompareAndSetByte          (Object, long, byte  , byte  ) > boolean
          - [ ] >=9 : <Unsafe>.weakCompareAndSetBytePlain     (Object, long, byte  , byte  ) > boolean
          - [ ] >=9 : <Unsafe>.weakCompareAndSetByteAcquire   (Object, long, byte  , byte  ) > boolean
          - [ ] >=9 : <Unsafe>.weakCompareAndSetByteRelease   (Object, long, byte  , byte  ) > boolean
          - [ ] >=9 : <Unsafe>.compareAndExchangeByte         (Object, long, byte  , byte  ) > byte
          - [ ] >=9 : <Unsafe>.compareAndExchangeByteAcquire  (Object, long, byte  , byte  ) > byte
          - [ ] >=9 : <Unsafe>.compareAndExchangeByteRelease  (Object, long, byte  , byte  ) > byte
          - [ ] >=9 : <Unsafe>.compareAndSetShort             (Object, long, short , short ) > boolean
          - [ ] >=9 : <Unsafe>.weakCompareAndSetShort         (Object, long, short , short ) > boolean
          - [ ] >=9 : <Unsafe>.weakCompareAndSetShortPlain    (Object, long, short , short ) > boolean
          - [ ] >=9 : <Unsafe>.weakCompareAndSetShortAcquire  (Object, long, short , short ) > boolean
          - [ ] >=9 : <Unsafe>.weakCompareAndSetShortRelease  (Object, long, short , short ) > boolean
          - [ ] >=9 : <Unsafe>.compareAndExchangeShort        (Object, long, short , short ) > short
          - [ ] >=9 : <Unsafe>.compareAndExchangeShortAcquire (Object, long, short , short ) > short
          - [ ] >=9 : <Unsafe>.compareAndExchangeShortRelease (Object, long, short , short ) > short
          - [ ] >=9 : <Unsafe>.compareAndSetInt               (Object, long, int   , int   ) > boolean
          - [ ] >=9 : <Unsafe>.weakCompareAndSetInt           (Object, long, int   , int   ) > boolean
          - [ ] >=9 : <Unsafe>.weakCompareAndSetIntPlain      (Object, long, int   , int   ) > boolean
          - [ ] >=9 : <Unsafe>.weakCompareAndSetIntAcquire    (Object, long, int   , int   ) > boolean
          - [ ] >=9 : <Unsafe>.weakCompareAndSetIntRelease    (Object, long, int   , int   ) > boolean
          - [ ] >=9 : <Unsafe>.compareAndExchangeInt          (Object, long, int   , int   ) > int
          - [ ] >=9 : <Unsafe>.compareAndExchangeIntAcquire   (Object, long, int   , int   ) > int
          - [ ] >=9 : <Unsafe>.compareAndExchangeIntRelease   (Object, long, int   , int   ) > int
          - [ ] >=9 : <Unsafe>.compareAndSetLong              (Object, long, long  , long  ) > boolean
          - [ ] >=9 : <Unsafe>.weakCompareAndSetLong          (Object, long, long  , long  ) > boolean
          - [ ] >=9 : <Unsafe>.weakCompareAndSetLongPlain     (Object, long, long  , long  ) > boolean
          - [ ] >=9 : <Unsafe>.weakCompareAndSetLongAcquire   (Object, long, long  , long  ) > boolean
          - [ ] >=9 : <Unsafe>.weakCompareAndSetLongRelease   (Object, long, long  , long  ) > boolean
          - [ ] >=9 : <Unsafe>.compareAndExchangeLong         (Object, long, long  , long  ) > long
          - [ ] >=9 : <Unsafe>.compareAndExchangeLongAcquire  (Object, long, long  , long  ) > long
          - [ ] >=9 : <Unsafe>.compareAndExchangeLongRelease  (Object, long, long  , long  ) > long
          - [ ] >=9 : <Unsafe>.compareAndSetObject            (Object, long, Object, Object) > boolean
          - [ ] >=9 : <Unsafe>.weakCompareAndSetObject        (Object, long, Object, Object) > boolean
          - [ ] >=9 : <Unsafe>.weakCompareAndSetObjectPlain   (Object, long, Object, Object) > boolean
          - [ ] >=9 : <Unsafe>.weakCompareAndSetObjectAcquire (Object, long, Object, Object) > boolean
          - [ ] >=9 : <Unsafe>.weakCompareAndSetObjectRelease (Object, long, Object, Object) > boolean
          - [ ] >=9 : <Unsafe>.compareAndExchangeObject       (Object, long, Object, Object) > Object
          - [ ] >=9 : <Unsafe>.compareAndExchangeObjectAcquire(Object, long, Object, Object) > Object
          - [ ] >=9 : <Unsafe>.compareAndExchangeObjectRelease(Object, long, Object, Object) > Object
          - [ ] >=9 : <Unsafe>.getAndAddByte  (Object, long, byte  ) > byte
          - [ ] >=9 : <Unsafe>.getAndAddShort (Object, long, short ) > short
          - [ ]       <Unsafe>.getAndAddInt   (Object, long, int   ) > int
          - [ ]       <Unsafe>.getAndAddLong  (Object, long, long  ) > long
          - [ ] >=9 : <Unsafe>.getAndSetByte  (Object, long, byte  ) > byte
          - [ ] >=9 : <Unsafe>.getAndSetShort (Object, long, short ) > short
          - [ ]       <Unsafe>.getAndSetInt   (Object, long, int   ) > int
          - [ ]       <Unsafe>.getAndSetLong  (Object, long, long  ) > long
          - [ ]       <Unsafe>.getAndSetObject(Object, long, Object) > Object
          - [ ] 8 : <Unsafe>.prefetchRead       (Object, long)
          - [ ] 8 : <Unsafe>.prefetchWrite      (Object, long)
          - [ ] 8 : <Unsafe>.prefetchReadStatic (Object, long)
          - [ ] 8 : <Unsafe>.prefetchWriteStatic(Object, long)
          - [ ] 8 : <Throwable>.fillInStackTrace() > Throwable
          - [ ] >=9 : StringUTF16.compress(chars, int, bytes, int, int) > int
          - [ ] >=9 : StringUTF16.compress(bytes, int, bytes, int, int) > int
          - [ ] >=9 : StringLatin1.inflate(bytes, int, chars, int, int)
          - [ ] >=9 : StringLatin1.inflate(bytes, int, bytes, int, int)
          - [ ] >=9 : StringUTF16.toBytes(chars, int, int) > bytes
          - [ ] >=9 : StringUTF16.getChars(bytes, int, int, chars, int)
          - [ ] >=9 : StringUTF16.getChar(bytes, int) > char
          - [ ] >=9 : StringUTF16.putChar(bytes, int, int)
          - [ ] >=9 : StringLatin1.compareTo(bytes, bytes) > int
          - [ ] >=9 : StringUTF16.compareTo(bytes, bytes) > int
          - [ ] >=9 : StringLatin1.compareToUTF16(bytes, bytes) > int
          - [ ] >=9 : StringUTF16.compareToLatin1(bytes, bytes) > int
          - [ ] >=9 : StringLatin1.indexOf(bytes, bytes) > int
          - [ ] >=9 : StringUTF16.indexOf(bytes, bytes) > int
          - [ ] >=9 : StringUTF16.indexOfLatin1(bytes, bytes) > int
          - [ ] >=9 : StringLatin1.indexOf(bytes, int, bytes, int, int) > int
          - [ ] >=9 : StringUTF16.indexOf(bytes, int, bytes, int, int) > int
          - [ ] >=9 : StringUTF16.indexOfLatin1(bytes, int, bytes, int, int) > int
          - [ ] >=9 : StringUTF16.indexOfChar(bytes, int, int, int) > int
          - [ ] >=9 : StringLatin1.equals(bytes, bytes) > boolean
          - [ ] >=9 : StringUTF16.equals(bytes, bytes) > boolean
          - [ ] >=9 : StringCoding.hasNegatives(bytes, int, int) > boolean
          - [ ] sun.nio.cs.ISO_8859_1$Encoder.encodeISOArray(chars, int, bytes, int, int) > int
          - [ ] >=9 : StringCoding.encodeISOArray(bytes, int, bytes, int, int) > int
          - [ ] new StringBuilder()
          - [ ] new StringBuilder(int)
          - [ ] new StringBuilder(String)
          - [ ] <StringBuilder>.append(char) > StringBuilder
          - [ ] <StringBuilder>.append(int) > StringBuilder
          - [ ] <StringBuilder>.append(String) > StringBuilder
          - [ ] <StringBuilder>.toString() > String
          - [ ] new StringBuffer()
          - [ ] new StringBuffer(int)
          - [ ] new StringBuffer(String)
          - [ ] <StringBuffer>.append(char) > StringBuffer
          - [ ] <StringBuffer>.append(int) > StringBuffer
          - [ ] <StringBuffer>.append(String) > StringBuffer
          - [ ] <StringBuffer>.toString() > String
          - [ ] Integer.toString(int) > String
          - [ ] new String(String)
          - [ ] new Object()
          - [ ] <java.lang.reflect.Method>.invoke(Object, objects) > Object
          - [ ] <java.lang.invoke.MethodHandle>.invoke(*)
          - [ ] <java.lang.invoke.MethodHandle>.invokeBasic(*)
          - [ ] java.lang.invoke.MethodHandle.invokeVirtual(*)
          - [ ] java.lang.invoke.MethodHandle.linkToVirtual(*)
          - [ ] java.lang.invoke.MethodHandle.linkToStatic(*)
          - [ ] java.lang.invoke.MethodHandle.linkToSpecial(*)
          - [ ] java.lang.invoke.MethodHandle.linkToInterface(*)
          - [ ] >=9 : java.lang.invoke.MethodHandleImpl.profileBoolean(boolean, ints) > boolean
          - [ ] >=9 : java.lang.invoke.MethodHandleImpl.isCompileConstant(object) > boolean
          - [x] <Boolean>  .booleanValue()        > boolean
          - [x] <Byte>     .byteValue   ()        > byte
          - [x] <Short>    .shortValue  ()        > short
          - [x] <Character>.charValue   ()        > char
          - [x] <Integer>  .intValue    ()        > int
          - [x] <Long>     .longValue   ()        > long
          - [x] <Float>    .floatValue  ()        > float
          - [x] <Double>   .doubleValue ()        > double
          - [x] Boolean    .valueOf     (boolean) > Boolean
          - [x] Byte       .valueOf     (byte   ) > Byte
          - [x] Short      .valueOf     (short  ) > Short
          - [x] Character  .valueOf     (char   ) > Character
          - [x] Integer    .valueOf     (int    ) > Integer
          - [x] Long       .valueOf     (long   ) > Long
          - [x] Float      .valueOf     (float  ) > Float
          - [x] Double     .valueOf     (double ) > Double
          - [ ] >=9 : <java.util.stream.StreamsRangeIntSpliterator>.forEachRemaining(java.util.function.IntConsumer)
    - [.] clojure.lang.RT
          https://github.com/clojure/clojure/blob/master/src/jvm/clojure/lang/RT.java
          - [ ] aclone
          - [ ] addURL
          - [ ] aget
          - [.] alength
          - [ ] arrayToList
          - [ ] aset
          - [ ] assoc
          - [ ] assocN
          - [ ] baseLoader
          - [.] booleanCast
          - [x] boundedLength
          - [x] box
          - [.] byteCast
          - [ ] canSeq
          - [.] charCast
          - [!] chunkIteratorSeq
          - [ ] classForName
          - [ ] classForNameNonLoading
          - [!] compile
          - [ ] conj
          - [ ] cons
          - [ ] contains
          - [x] count
          - [x] countFrom
          - [ ] dissoc
          - [ ] doFormat
          - [.] doubleCast
          - [!] errPrintWriter
          - [ ] find
          - [ ] findKey
          - [ ] first
          - [.] floatCast
          - [ ] format
          - [ ] formatAesthetic
          - [ ] formatStandard
          - [!] fourth
          - [ ] get
          - [ ] getColumnNumber
          - [ ] getFrom
          - [ ] getLineNumber
          - [ ] getLineNumberingReader
          - [ ] getResource
          - [ ] hasTag
          - [!] init
          - [.] intCast
          - [ ] isLineNumberingReader
          - [x] isReduced
          - [ ] iter
          - [ ] keys
          - [x] keyword
          - [!] lastModified
          - [x] length
          - [ ] list
          - [ ] listStar
          - [!] load
          - [ ] loadClassForName
          - [!] loadLibrary
          - [!] loadResourceScript
          - [.] longCast
          - [ ] makeClassLoader
          - [ ] map
          - [ ] mapUniqueKeys
          - [!] maybeLoadResourceScript
          - [x] meta
          - [ ] more
          - [ ] nextID
          - [ ] nth
          - [ ] nthFrom
          - [ ] object_array
          - [ ] peek
          - [ ] peekChar
          - [ ] pop
          - [ ] print
          - [ ] printInnerSeq
          - [ ] printString
          - [!] processCommandLine
          - [ ] readChar
          - [ ] readString
          - [ ] resolveClassNameInContext
          - [ ] resourceAsStream
          - [ ] rest
          - [!] second
          - [x] seq
          - [x] seqFrom
          - [!] seqOrElse
          - [ ] seqToArray
          - [ ] seqToPassedArray
          - [ ] seqToTypedArray
          - [ ] set
          - [!] setValues
          - [.] shortCast
          - [ ] subvec
          - [ ] suppressRead
          - [!] third
          - [ ] toArray
          - [.] uncheckedByteCast
          - [.] uncheckedShortCast
          - [.] uncheckedCharCast
          - [.] uncheckedIntCast
          - [.] uncheckedLongCast
          - [.] uncheckedFloatCast
          - [.] uncheckedDoubleCast
          - [ ] vals
          - [!] var
          - [ ] vector
    - [.] clojure.lang.Numbers
          https://github.com/clojure/clojure/blob/master/src/jvm/clojure/lang/Numbers.java
          - [ ] add
          - [ ] addP
          - [ ] and
          - [!] andNot
          - [ ] boolean_array
          - [ ] booleans
          - [ ] byte_array
          - [ ] bytes
          - [ ] char_array
          - [ ] chars
          - [ ] clearBit
          - [x] compare
          - [ ] dec
          - [ ] decP
          - [ ] denominator
          - [ ] divide
          - [ ] double_array
          - [ ] doubles
          - [x] equal
          - [ ] equiv
          - [ ] flipBit
          - [ ] float_array
          - [ ] floats
          - [x] gt
          - [x] gte
          - [ ] hasheq
          - [ ] hasheqFrom
          - [ ] inc
          - [ ] incP
          - [ ] int_array
          - [ ] ints
          - [x] isNaN
          - [x] isNeg
          - [x] isPos
          - [x] isZero
          - [ ] long_array
          - [ ] longs
          - [x] lt
          - [x] lte
          - [x] max
          - [x] min
          - [ ] minus
          - [ ] minusP
          - [ ] multiply
          - [ ] multiplyP
          - [ ] not
          - [ ] num
          - [x] numerator
          - [ ] or
          - [ ] quotient
          - [ ] rationalize
          - [ ] reduceBigInt
          - [ ] remainder
          - [ ] shiftLeft
          - [ ] shiftLeftInt
          - [ ] shiftRight
          - [ ] shiftRightInt
          - [ ] short_array
          - [ ] shorts
          - [ ] setBit
          - [ ] testBit
          - [x] toBigDecimal
          - [x] toBigInt
          - [x] toBigInteger
          - [ ] toRatio
          - [ ] unchecked_add
          - [ ] unchecked_dec
          - [ ] unchecked_divide
          - [ ] unchecked_inc
          - [ ] unchecked_minus
          - [ ] unchecked_multiply
          - [ ] unchecked_negate
          - [ ] unchecked_remainder
          - [ ] unchecked_int_add
          - [ ] unchecked_int_dec
          - [ ] unchecked_int_divide
          - [ ] unchecked_int_inc
          - [ ] unchecked_int_multiply
          - [ ] unchecked_int_negate
          - [ ] unchecked_int_remainder
          - [ ] unchecked_int_subtract
          - [ ] unsignedShiftRight
          - [ ] unsignedShiftRightInt
          - [ ] xor
    - [.] clojure.lang.Util
          https://github.com/clojure/clojure/blob/master/src/jvm/clojure/lang/Util.java
         - [ ] classOf
         - [ ] clearCache
         - [x] compare
         - [x] equiv
         - [ ] hash
         - [ ] hashCombine
         - [ ] hasheq
         - [x] identical
         - [x] isInteger
         - [ ] isPrimitive
         - [ ] loadWithClass
         - [ ] pcequiv
         - [|] ret1
         - [|] runtimeException
         - [|] sneakyThrow
    - [.] (TEMPORARY) collections-typed
         - [ ] `get`
         - [ ] `merge`
         - [ ] `concat`
         - [ ] `repeat`
    - [ ] quantum.core.core -> TODO just need to delete this from all references
    - [ ] quantum.core.type.core
    - [x] quantum.core.data.async
    - [-] quantum.core.data.queue
    - [ ] quantum.core.type.defs
    - [.] quantum.core.refs -> quantum.core.data.refs ?
    - [ ] quantum.core.logic
          - (def nneg?    (l/fn-not neg?))
          - (def pos-int? (l/fn-and dn/integer? pos?))
    - [.] quantum.core.fn
          - [ ] `apply`
                - especially with `t/defn` as the caller
    - [.] quantum.core.cache
    - [ ] quantum.core.type-old
    - [.] quantum.core.data.primitive
    - [.] quantum.core.data.string
          - [ ] `>str`
    - [.] quantum.core.data.map
    - [.] quantum.core.data.meta
    - [.] quantum.core.compare - should provide comparisons for all data in quantum.core.data
          - [ ] `compare`
    - [x] quantum.core.ns ; TODO split up into data.ns?
    - [.] quantum.core.vars
    - [ ] quantum.core.print
    - [ ] quantum.core.log
    - [.] quantum.core.data.vector
    - [ ] quantum.core.spec
    - [.] quantum.core.error
    - [.] quantum.core.data.string
    - [.] quantum.core.data.array
    - [.] quantum.core.data.list
    - [.] quantum.core.data.collections
    - [.] quantum.core.data.tuple
    - [x] quantum.core.data.time
    - [.] quantum.core.compare.core
    - [.] quantum.core.data.numeric
    - [x] quantum.core.numeric.predicates
    - [.] quantum.core.numeric.convert
    - [.] quantum.core.numeric.exponents
    - [.] quantum.core.numeric.misc
    - [.] quantum.core.numeric.operators
    - [.] quantum.core.numeric.trig
    - [.] quantum.core.numeric.truncate
    - [x] quantum.core.numeric.types
    - [.] quantum.core.numeric
    - [.] quantum.core.data.set
    - [ ] quantum.core.string.regex
    - [ ] quantum.core.macros.type-hint
    - [ ] quantum.core.analyze.clojure.core
    - [ ] quantum.core.analyze.clojure.predicates
    - [ ] quantum.core.macros.optimization
    - [ ] quantum.core.macros.fn
    - [ ] quantum.core.macros.transform
    - [ ] quantum.core.macros.protocol
    - [ ] quantum.core.macros.reify
    - [ ] quantum.core.macros.defnt
    - [ ] quantum.core.macros
    - [.] quantum.core.reducers.reduce
    - [ ] quantum.core.collections.logic
    - [.] quantum.core.collections.core
    - [ ] quantum.core.form
          - [ ] `(t/def langs #{:clj :cljs :clr})`
          - [ ] `(t/def lang "The language this code is compiled under" #?(:clj :clj :cljs :cljs))`
    - [ ] quantum.core.form.generate
          - [ ] ```
                ;; TODO TYPED
                (defalias u/externs?)
                ```

    - Worked through all we can for now:
      - quantum.core.data.bits
      - quantum.core.convert.primitive
  - List of corresponding untyped namespaces to incorporate:
    - [ ] quantum.untyped.core.compare
    - [ ] quantum.untyped.core.core
    - [ ] quantum.untyped.core.ns
    - [ ] quantum.untyped.core.vars
    - [ ] quantum.untyped.core.data.map
    - [ ] quantum.untyped.core.type.defs
    - [ ] quantum.untyped.core.data
    - [ ] quantum.untyped.core.refs
    - [ ] quantum.untyped.core.data.bits
    - [x] quantum.untyped.core.identifiers
  - List of Array fns to implement:
    - [ ] count
    - [ ] get
    - [ ] set
    - [ ] new1dObjectArray
    - [ ] new1dArray
    - [ ] newUninitialized<n : 1-10>d<type>Array
    - [ ] newInitializedNd<type>Array
    - [ ] newUninitializedArrayOfType
    - [ ] newInitializedArrayOfType
  - List of Numeric fns to implement:
    - [ ] isTrue (?)
    - [ ] isFalse (?)
    - [ ] isNil (?)
    - [ ] (logical) and (?)
    - [ ] (logical) or (?)
    - [ ] (logical) not
    - [x] lt
    - [x] lte
    - [x] gt
    - [x] gte
    - [x] eq
    - [x] neq
    - [ ] inc
    - [ ] dec
    - [x] isZero
    - [x] isNeg
    - [x] isPos
    - [x] add
    - [ ] subtract
    - [ ] negate
    - [ ] multiply
    - [ ] divide
    - [x] max
    - [x] min
    - [ ] rem
  - List of Primitive fns to implement:
    - uncheckedByteCast
    - uncheckedCharCast
    - uncheckedShortCast
    - uncheckedIntCast
    - uncheckedLongCast
    - uncheckedFloatCast
    - uncheckedDoubleCast
  - Standard metadata
    - e.g. `{:alternate-implementations #{'cljs.tools.reader/merge-meta}}`
    - :adapted-from (t/or namespace-symbol? class-symbol? url-string?)
    - :source <namespace-or-class-symbol>
    - :todo #{<todo-string>}
    - :attribution <github-username-symbol | string-description>
    - :doc <string-documentation>
    - :performance <string-documentation>
    - :incorporated (t/or (t/set-of (t/or <namespace-or-class-symbol> <function-or-method-symbol>))
                          (t/map-of (t/or <namespace-or-class-symbol> <function-or-method-symbol>)
                                    date))
    - :equivalent `{(aritoid vector identity conj)
                   (fn ([]      (vector))
                       ([x0]    (identity x0))
                       ([x0 x1] (conj x0 x1)))}}
    - :in — a Clojure or Java intrinsic
  - Instead of e.g. `ns-` or `var-` we can do `ns-val` and `var-val`

[ ] Compile-Time (Direct) Dispatch
    - TODO Should we take into account 'actual' types (not just 'declared' types) when performing
      dispatch / overload resolution?
      - Let's take the example of `(t/defn abcde [] (f (rand/int-between -10 -2)))`.
        - Let's say `rand/int-between`'s output is labeled `t/int?`. However, we know based on
          further static analysis of its implementation that the output is not only `t/int?` but
          also `t/neg?`, or perhaps even further, `(< -10 % -2)`.
        - In this case, should we take advantage of this knowledge?
          - Let's say we do. Then `(.invoke reify|we-know-specifics (rand/int-between -10 -2))`.
            Yay for efficiency! But let's then say we then change the implementation even if we
            don't change the 'interface'/typedefs. Now `rand/int-between` returns `(<= -10 % -2)` —
            that is, it's now numerically *inclusive* (for instance, maybe the implementation's
            previous behavior of generating numbers numerically *exclusive*ly was mistaken).
            `reify|we-know-specifics` would then still be invoked but incorrectly (and unsafely) so.
          - To be fair, we'll tend to change output specs/typedefs all the time as we do
            development. Do we need to keep track of every call site it affects and recompile
            accordingly? Perhaps. It seems like overkill though. It should be configurable in any
            case.
          - I think that because of this last point, we can and should rely on implementational
            specifics wherever available to boost performance (Maybe this should be configurable so
            it doesn't slow down development? The more we change the implementation, the more it has
            to recompile, ostensibly). We can take advantage of the output specs, certainly, if for
            nothing else than to ensure that our implementation (as characterized by its 'actual'
            output type) matches what we expect (as characterized by its 'expected'/'declared'
            output type).
          - One option (Option A) is to turn off compile-time overload resolution during
            development. This would mean it might get very slow during that time. But if it's in
            the same `t/defn` (ignoring `t/extend-defn!` for a minute) — like a recursive call — you
            could always leave on compile-time resolution for that.
          - Option B — probably better (though we'd still like to have all this configurable) —
            is to have each function know its dependencies (this would actually have the bonus
            property of enabling `clojure.tools.namespace.repl/refresh`-style function-level
            smart auto-recompilation which is nice). So let's go back to the previous example.
            `abcde` could keep track of (or the `t/defn` ns could keep track of it, but you get the
            point) the fact that it depends on `rand/int-between` and `f`. It has a compile-time-
            resolvable call site that depends only on the output type of `rand/int-between` so if
            `rand/int-between`'s computed/actual output type (when given the inputs in question)
            ever changes, `abcde` needs to be recompiled and `abcde`'s output type recomputed. If,
            on the other hand, `f`'s output type (given the input) ever changes, `abcde` need not be
            recompiled, but rather, only its output type need be recomputed.
          - I think this reactive approach should solve our problems and let us code in a very flexible
            way.
[ ] Runtime (Dynamic) Dispatch
    [—] Protocol generation
        - For now we won't do it because we can very often find the correct overload at compile
          time. We will resort to using the `fn`.
        - It will be left as an optimization.
    [x] `fn` generation
        - Performs a worst-case linear check of the typedefs, `cond`-style.
[x] Interface generation
    [x] Even if the `t/defn` is redefined, you won't have interface problems.
[ ] `reify` generation
    - Which `reify`s get generated is mainly up to the inputs but partially up to the fn body —
      If any typed fns are called in the fn body then this can change what gets generated.
      - TODO explain this more
    - Each of the `reify`s will keep their label (`__2__0` or whatever) as long as the original
      typedef of the `reify` is `t/=` to the new typedef of that reify
      - If a redefined `t/defn` doesn't have that type overload then the previous reify is uninterned
        and thus made unavailable
      - That way, according to the dynamicity tests in `quantum.test.core.type.defn`, we can redefine
        implementations at will as long as the specs don't change
      - To make this process faster we maintain a set of typedefs so at least cheap c/= checks can
        be performed
        - If c/= succeeds, great; the `reify` corresponding to the label (and reify-type) will be
          replaced; the typedef-set will remain unchanged
        - Else it must find a corresponding typedef by t/=
          - Then if it is found by t/= it will replace the `reify` and the typedef corresponding
            with that label and replace the typedef in the typedef-set
          - Else a new label will be given to the `reify`; the typedef will be added to the
            typedef-set
[ ] Types yielding generative specs
[—] Types using the clojure.spec interface
    - Not yet; wait for it to come out of alpha
[ ] We do need to rely on bootstrapped CLJS for Figwheel-style dev-time recompilation, but we don't
    need to rely on bootstrapped CLJS per se for advanced compilation. We can try to hook in to the
    JS version of the Closure Compiler. All of that is for later.
"
