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

- conditionally optional arities etc. for t/fn

#_"
Note that `;; TODO TYPED` is the annotation we're using for this initiative

- TODO implement the following:
  - Analysis
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
  - t/- : multi-arity
  - t/isa|direct?
    - For CLJ, this is `instance?`; for CLJS, this is `instance?` for classes and `implements?` for protocols
  - t/value-of
    - `[x with-metable?, meta' meta? > (t/* with-metable?) #_(TODO TYPED (t/value-of x))]`
  - t/numerically : e.g. a double representing exactly what a float is able to represent
    - and variants thereof: `numerically-long?` etc.
    - t/numerically-integer?
  - t/range-of : e.g. a double being between float max values but possibly representing a 'hole' in
               possible float values
  - t/type
    - dependent types: `[x arr/array? > (t/type x)]`
  - ? : type inference
    - use logic programming and variable unification e.g. `?1` `?2` ?
  - t/extend-defnt!
  - t/input-type
    - `(t/input-type >namespace t/?)` meaing the possible input types to the first input to `>namespace`
  - t/of
    - (t/of number?) ; implicitly the container is a `traversable?`
    - (t/of map/+map? symbol? dstr/string?)
    - (t/of t/seq? namespace?)
    - t/map-of
    - t/seq-of
  - t/defrecord
  - t/def-concrete-type (i.e. `t/deftype`)
  - expressions (`quantum.untyped.core.analyze.expr`)
  - comparison of `t/fn`s is probably possible?
  - t/def
  - t/fnt (t/fn; current t/fn might transition to t/fn-spec or whatever?)
  - t/declare
  - declare-fnt (a way to do protocols/interfaces)
    - extend-fnt!
  - defnt (t/defn)
    - t/defn-
    - (t/and (t/or a b) c) should -> (t/or (t/and a c) (t/and b c)) for purposes of separating dispatches
    - t/extend-defn!
      - `(t/extend-defn! id/>name (^:inline [x namespace?] (-> x .getName id/>name)))`
    - ^:inline
      - if you do (Numeric/bitAnd a b) inline then bitAnd needs to know the primitive type so maybe
        we do the `let*`-binding approach to typing vars?
      - should be able to be per-arity like so:
        (^:inline [] ...)
    - handle varargs
      - [& args _] shouldn't result in `t/any?` but rather like `t/seqable?` or whatever
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
  - typed core fns
    - `apply`
      - especially with `t/defn` as the caller
    - `merge`
    - `str`
    - `compare`
    - `get`
    - `concat`
    - `repeat`
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
  - List of semi-approximately topologically ordered namespaces to make typed:
    - [ ] quantum.core.core -> TODO just need to delete this from all references
    - [ ] quantum.core.type.core
    - [ ] quantum.core.type.defs
    - [ ] quantum.core.refs -> quantum.core.data.refs
    - [ ] quantum.core.logic
          - (def nneg?    (l/fn-not neg?))
          - (def pos-int? (l/fn-and dnum/integer? pos?))
    - [ ] quantum.core.fn
    - [ ] quantum.core.cache
    - [ ] quantum.core.type-old
    - [ ] quantum.core.data.string
    - [x] quantum.core.data.map
    - [x] quantum.core.data.meta
    - [x] quantum.core.ns ; TODO split up into data.ns?
    - [ ] quantum.core.print
    - [ ] quantum.core.log
    - [ ] quantum.core.data.vector
    - [ ] quantum.core.spec
    - [ ] quantum.core.error
    - [ ] quantum.core.data.string — this is where `>str` belongs
    - [ ] quantum.core.data.array
    - [ ] quantum.core.data.collections
    - [ ] quantum.core.data.tuple
    - [ ] quantum.core.numeric.predicates
    - [ ] quantum.core.numeric.convert
    - [ ] quantum.core.numeric.misc
    - [ ] quantum.core.numeric.operators
    - [ ] quantum.core.numeric.trig
    - [ ] quantum.core.numeric.truncate
    - [ ] quantum.core.data.numeric
    - [ ] quantum.core.numeric
    - [ ] quantum.core.string.regex
    - [ ] quantum.core.data.set
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
    - [ ] quantum.core.reducers.reduce
    - [ ] quantum.core.collections.logic
    - [ ] quantum.core.collections.core

    - Worked through all we can for now:
      -
        - TODO delete this namespace?
      - quantum.core.data.primitive (TODO make it compile)
      - quantum.core.data.bits
      - quantum.core.convert.primitive
  - List of corresponding untyped namespaces to incorporate:
    - [ ] quantum.untyped.core.core
    - [ ] quantum.untyped.core.ns
    - [ ] quantum.untyped.core.vars
    - [ ] quantum.untyped.core.data.map
    - [ ] quantum.untyped.core.type.defs
    - [ ] quantum.untyped.core.data
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
    - [ ] lt
    - [ ] lte
    - [ ] gt
    - [ ] gte
    - [ ] eq
    - [ ] neq
    - [ ] inc
    - [ ] dec
    - [ ] isZero
    - [ ] isNeg
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
    - :incorporated #{<namespace-or-class-symbol | <function-or-method-symbol>}
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
