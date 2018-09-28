;; TO MOVE

#?(:clj  (def thread?       (isa? java.lang.Thread)))

#?(:clj  (def class?           (isa? java.lang.Class)))

;; TODO for CLJS based on untyped impl
#?(:clj  (def protocol?        (>expr (ufn/fn-> :on-interface class?))))


;; ===== quantum.core.system

#?(:clj
(defnt pid [> (? t/string?)]
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

We should not rely on the value of dynamic vars e.g. `*math-context*` unless specifically typed

Sometimes you want (byte <whatever-double>) to fail at runtime rather than fail at runtime when you can't know everything about the input's range

These two should be defined in the (whatever) data namespace:
- `>(whatever)`
- `(whatever)>`

TODO:
- `(or (and pred then) (and (not pred) else))` (which is not correct)
- needs to equal `(t/and (t/or (t/not a) b) (t/or a c))` (which is correct)
- `(- (or ?!+vector? !vector? #?(:clj !!vector?)) (isa? clojure.lang.Counted))` is not right

#_"
Note that `;; TODO TYPED` is the annotation we're using for this initiative

- TODO implement the following:
  - t/type >>>>>> (PRIORITY 1) <<<<<<
    - dependent types: `[x arr/array? > (t/type x)]`
  - Analysis
    - This is accepted by the type system without knowing the type:
      (java.math.BigInteger. 1 (-> (ByteBuffer/allocate (int 8)) (.putLong x) .array))

      So, constructors need the same kind of lookup that dot calls have
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
                                   :cljs (t/isa|direct? cljs.core/IReduce))]
        ;; TODO add `^not-native` to `xs` for CLJS
        (#?(:clj  clojure.core.protocols/coll-reduce
            :cljs cljs.core/-reduce) xs rf init))
    - (if (A) ...) should be (if ^boolean (A) ...) if A returns a `p/boolean?`
  - t/- : fix
    - (t/- (t/isa? java.util.Queue) (t/or ?!+queue? !!queue?))
  - t/isa|direct?
    - For CLJ, this is `instance?` for classes and `instance?` on the underlying interface
      associated with a protocol
    - For CLJS, this is `instance?` for classes and `implements?` for protocols
  - t/value-of
    - `[x with-metable?, meta' meta? > (t/* with-metable?) #_(TODO TYPED (t/value-of x))]`
  - t/numerically : e.g. a double representing exactly what a float is able to represent
    - and variants thereof: `numerically-long?` etc.
    - t/numerically-integer?
  - ? : type inference
    - use logic programming and variable unification e.g. `?1` `?2` ?
    - For this situation: `?` is `(t/- <whatever-deduced-type> dc/counted?)`
      ([n dnum/std-integer?, xs dc/counted?] (count xs))
      ([n dnum/std-integer?, xs ?] ...)
  - t/extend-defn!
  - t/input-type
    - `(t/input-type >namespace :?)` meaning the possible input types to the first input to `>namespace`
    - `(t/input-type reduce :_ :_ :?)`
    - Then if those fns ever get extended then it should trigger a chain-reaction of recompilations
  - dc/of
    - (dc/of number?) ; implicitly the container is a `reducible?`
    - (dc/of map/+map? symbol? dstr/string?)
    - (dc/of t/seq? namespace?)
    - dc/map-of
    - dc/seq-of
  - t/defrecord
  - t/def-concrete-type (i.e. `t/deftype`)
  - expressions (`quantum.untyped.core.analyze.expr`)
  - comparison of `t/fn`s is probably possible?
  - t/def
    - TODO what would this even look like?
  - t/fnt (t/fn; current t/fn might transition to t/fn-spec or whatever?)
  - t/ftype
    - conditionally optional arities etc.
  - t/declare
  - declare-fnt (a way to do protocols/interfaces)
    - extend-fnt!
  - defnt (t/defn)
    - Arity elision: if any type in an arity is `t/none?` then elide it and emit a warning
      - `([x bigint?] x)`
    - t/defn-
    - (t/and (t/or a b) c) should -> (t/or (t/and a c) (t/and b c)) for purposes of separating dispatches
    - t/extend-defn!
      - `(t/extend-defn! id/>name (^:inline [x namespace?] (-> x .getName id/>name)))`
    - ^:inline
      - if you do (Numeric/bitAnd a b) inline then bitAnd needs to know the primitive type so maybe
        we do the `let*`-binding approach to typing vars?
      - should be able to be per-arity like so:
        (^:inline [] ...)
      - ^:inline set on a function should propagate to all overloads, including ones added after the fact
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
          ([xs (t/input-type educe :_ :_ :?)] (educe empty?|rf x)))
    - handle varargs
      - [& args _] shouldn't result in `t/any?` but rather like `t/reducible?` or whatever
    - do the defnt-equivalences
    - a linting warning that you can narrow the type to whatever the deduced type is from whatever
      wider declared type there is
    - the option of creating a `defnt` that isn't extensible? Or at least in which the input types are limited in the same way per-overload output types are limited by the per-fn output type?
    - dealing with `apply`...
  - t/defmacro
  - t/deftype
  - t/dotyped
  - lazy compilation especially around `t/input-type`
  - equivalence of typed predicates (i.e. that which is `t/<=` `(t/fn [x t/any? :> p/boolean?])`)
    to types:
    - [xs (t/fn [x (t/isa? clojure.lang.Range)] ...)]
  - No return value means that it should infer
- NOTE on namespace organization:
  - [quantum.untyped.core.ns :refer [namespace?]]
    instead of
    [quantum.untyped.core.type.predicates :refer [namespace?]]
    because not all predicates (type-related or otherwise) can be thought of ahead of time to be put
    in one giant namespace
  - Same with the `core.convert` namespace too
    - Conversion functions belong in the namespace that their destination types belong in
- TODO transition the quantum.core.* namespaces:
  ->>>>>> TODO need to add *all* quantum namespaces in here
  - Legend:
    [.] : in progress
    [-] : done as far as possible but not truly complete
    [x] : actually done
    [|] : not possible / N/A
    [!] : refused
  - List of semi-approximately topologically ordered namespaces to make typed:
    - [.] clojure.core / cljs.core
          - [x x] =
          - [. .] ==
          - [. .] <
          - [   ] and
          - [   ] any?
          - [   ] apply
          - [   ] assoc
          - [   ] assoc!
          - [x x] associative?
          - [. .] boolean
          - [x x] boolean?
          - [   ] butlast
          - [x x] byte
          - [x x] byte?
          - [x x] char
          - [x x] char?
          - [  |] cast
          - [x x] chunk
          - [x x] chunk-append
          - [x x] chunk-buffer
          - [x x] chunk-cons
          - [x x] chunk-first
          - [x x] chunk-next
          - [x x] chunk-rest
          - [x x] chunked-seq?
          - [  |] class
          - [x x] compare
          - [   ] concat
          - [   ] cond
          - [   ] cons
          - [   ] conj
          - [   ] contains?
          - [x x] count
          - [x x] counted?
          - [x |] decimal?
          - [   ] defmacro
          - [. .] defn
          - [   ] defrecord
          - [   ] deftype
          - [   ] delay
          - [x x] delay?
          - [x |] denominator
          - [x x] double
          - [x x] double?
          - [. .] empty?
          - [   ] even?
          - [   ] force
          - [x x] identical?
          - [   ] if-not (not as performant as we thought)
          - [x x] indexed?
          - [. .] int
          - [x x] integer?
          - [x x] false?
          - [   ] filter
          - [  |] find-keyword
          - [   ] ffirst
          - [   ] first
          - [x x] float
          - [x x] float?
          - [. .] fn
          - [x x] fn?
          - [   ] fnext
          - [   ] gensym
          - [   ] hash-map
          - [   ] hash-set
          - [x x] ident?
          - [x x] ifn?
          - [|  ] infinite?
          - [   ] instance?
          - [x x] int
          - [x x] int?
          - [x x] keyword
          - [x x] keyword?
          - [   ] last
          - [   ] lazy-seq
          - [   ] let
          - [   ] list
          - [   ] list*
          - [x x] list?
          - [  |] locking
          - [x x] long
          - [x x] long?
          - [   ] loop
          - [   ] map
          - [x x] map?
          - [x x] map-entry?
          - [x x] meta
          - [   ] mod
          - [x x] name
          - [x x] namespace
          - [   ] nat-int?
          - [   ] neg?
          - [   ] neg-int?
          - [   ] next
          - [   ] nfirst
          - [x x] nil?
          - [   ] nnext
          - [   ] nth
          - [   ] not
          - [x x] not=
          - [x |] ns-name
          - [x x] number?
          - [x |] numerator
          - [   ] odd?
          - [   ] or
          - [   ] peek
          - [   ] pop
          - [   ] pos?
          - [   ] pos-int?
          - [x x] qualified-ident?
          - [x x] qualified-keyword?
          - [x x] qualified-symbol?
          - [x |] ratio?
          - [   ] rational?
          - [x x] record?
          - [x x] reduce
          - [   ] rem
          - [   ] remove
          - [   ] rest
          - [   ] second
          - [   ] seq
          - [x x] seq?
          - [x x] set?
          - [x x] short
          - [x x] short?
          - [x x] simple-ident?
          - [x x] simple-keyword?
          - [x x] simple-symbol?
          - [x x] some?
          - [x x] sorted?
          - [   ] sorted-map
          - [   ] sorted-map-by
          - [   ] sorted-set
          - [   ] sorted-set-by
          - [   ] spread
          - [. .] str
          - [x x] string?
          - [x x] symbol
          - [x x] symbol?
          - [   ] to-array
          - [. .] transduce
          - [x x] true?
          - [x x] uuid?
          - [. .] vary-meta
          - [   ] vec
          - [   ] vector
          - [x x] vector?
          - [! !] when
          - [! !] when-not
          - [x x] with-meta
          - [x  ] zero?
    - [.] clojure.lang.Numbers
          https://github.com/clojure/clojure/blob/master/src/jvm/clojure/lang/Numbers.java
          - [ ] add
          - [ ] addP
          - [ ] and
          - [ ] andNot
          - [ ] boolean_array
          - [ ] booleans
          - [ ] byte_array
          - [ ] bytes
          - [ ] char_array
          - [ ] chars
          - [ ] clearBit
          - [ ] compare
          - [ ] dec
          - [ ] decP
          - [ ] denominator
          - [ ] divide
          - [ ] double_array
          - [ ] doubles
          - [ ] equal
          - [ ] equiv
          - [ ] flipBit
          - [ ] float_array
          - [ ] floats
          - [ ] gt
          - [ ] gte
          - [ ] hasheq
          - [ ] hasheqFrom
          - [ ] inc
          - [ ] incP
          - [ ] int_array
          - [ ] ints
          - [ ] isNaN
          - [ ] isNeg
          - [ ] isPos
          - [ ] isZero
          - [ ] long_array
          - [ ] longs
          - [ ] lt
          - [ ] lte
          - [ ] max
          - [ ] min
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
          - [ ] toBigDecimal
          - [ ] toBigInt
          - [ ] toBigInteger
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
    - [.] clojure.lang.RT
          https://github.com/clojure/clojure/blob/master/src/jvm/clojure/lang/RT.java
          - [x] count
          - [x] countFrom
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
          - (def pos-int? (l/fn-and dnum/integer? pos?))
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
    - [.] quantum.core.compare
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
    - [.] quantum.core.numeric.predicates
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
    - [ ] gt
    - [ ] gte
    - [x] eq
    - [x] neq
    - [ ] inc
    - [ ] dec
    - [ ] isZero
    - [ ] isNeg
    - [ ] isPos
    - [x] add
    - [ ] subtract
    - [ ] negate
    - [ ] multiply
    - [ ] divide
    - [ ] max
    - [ ] min
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
    - :adapted-from <namespace-or-class-symbol>
    - :source <namespace-or-class-symbol>
    - :todo #{<todo-string>}
    - :attribution <github-username-symbol | string-description>
    - :doc <string-documentation>
    - :incorporated (t/or (t/set-of (t/or <namespace-or-class-symbol> <function-or-method-symbol>))
                          (t/map-of (t/or <namespace-or-class-symbol> <function-or-method-symbol>)
                                    date))
    - :equivalent `{(aritoid vector identity conj)
                   (fn ([]      (vector))
                       ([x0]    (identity x0))
                       ([x0 x1] (conj x0 x1)))}}
  - Instead of e.g. `ns-` or `var-` we can do `ns-val` and `var-val`
  - Should we type `when`, `let`?

- With `defnt`, protocols and interfaces aren't needed. You can just create `t/fn`s that you can
  then conform your fns to.
- `dotyped`, `defnt`, and `fnt` create typed contexts in which their internal forms are analyzed
  and overloads are resolved.
- `defnt` is intended to catch many runtime errors at compile time, but cannot catch all of them;
  types will very often have to be validated at runtime.

[ ] Compile-Time (Direct) Dispatch
    [x] Any argument, if it requires a non-nilable primitive-like value, will be marked as a
        primitive.
    [x] If nilable, there will be one overload for nil and one for primitive.
    [x] When a `fnt` with type overloads is referenced outside of a typed context, then the overload
        resolution will be done via Runtime Dispatch.
    - TODO Should we take into account 'actual' types (not just 'declared' types) when performing
      dispatch / overload resolution?
      - Let's take the example of `(defnt abcde [] (f (rand/int-between -10 -2)))`.
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
            the same `defnt` (ignoring `extend-defnt!` for a minute) — like a recursive call — you
            could always leave on compile-time resolution for that.
          - Option B — probably better (though we'd still like to have all this configurable) —
            is to have each function know its dependencies (this would actually have the bonus
            property of enabling `clojure.tools.namespace.repl/refresh`-style function-level
            smart auto-recompilation which is nice). So let's go back to the previous example.
            `abcde` could keep track of (or the `defnt` ns could keep track of it, but you get the
            point) the fact that it depends on `rand/int-between` and `f`. It has a compile-time-
            resolvable call site that depends only on the output type of `rand/int-between` so if
            `rand/int-between`'s computed/actual output type (when given the inputs in question)
            ever changes, `abcde` needs to be recompiled and `abcde`'s output type recomputed. If,
            on the other hand, `f`'s output type (given the input) ever changes, `abcde` need not be
            recompiled, but rather, only its output type need be recomputed.
          - I think this reactive approach (do we need a library for that? probably not?) should
            solve our problems and let us code in a very flexible way. It'll just (currently) be a
            way that depends on a compiler in which the metalanguage and object language are
            identical.
[ ] Runtime (Dynamic) Dispatch
    [—] Protocol generation
        - For now we won't do it because we can very often find the correct overload at compile
          time. We will resort to using the `fn`.
        - It will be left as an optimization.
    [x] `fn` generation
        - Performs a worst-case linear check of the typedefs, `cond`-style.
[x] Interface generation
    [x] Even if the `defnt` is redefined, you won't have interface problems.
[ ] `reify` generation
    - Which `reify`s get generated is mainly up to the inputs but partially up to the fn body —
      If any typed fns are called in the fn body then this can change what gets generated.
      - TODO explain this more
    - Each of the `reify`s will keep their label (`__2__0` or whatever) as long as the original
      typedef of the `reify` is `t/=` to the new typedef of that reify
      - If a redefined `defnt` doesn't have that type overload then the previous reify is uninterned
        and thus made unavailable
      - That way, according to the dynamicity tests in `quantum.test.core.defnt`, we can redefine
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
[—] Support for compilers in which the metalanguage differs from the object language (i.e. 'normal'
    non-CLJS-in-CLJS CLJS)
    - This will have to be approached later. We'll figure it out; maybe just not yet.
"
