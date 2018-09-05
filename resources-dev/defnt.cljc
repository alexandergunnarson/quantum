#_"
LEFT OFF LAST TIME (9/3/2018):

Note that `;; TODO TYPED` is the annotation we're using

- TODO implement the following:
  - t/...
    - t/assume
    - t/numerically
    - expressions (`quantum.untyped.core.analyze.expr`)
  - deft
  - fnt
  - declare-fnt (a way to do protocols/interfaces)
    - extend-fnt!
  - defnt
    - recursion by adding the function's name and type to the local bindings (env)
    - handle varargs
    - do the defnt-equivalences
    - a linting warning that you can narrow the type to whatever the deduced type is from whatever
      wider declared type there is
  - defmacrot
  - dotyped
- NOTE on namespace organization:
  - [quantum.untyped.core.ns :refer [namespace?]]
    instead of
    [quantum.untyped.core.type.predicates :refer [namespace?]]
    because not all predicates (type-related or otherwise) can be thought of ahead of time to be put
    in one giant namespace
  - Same with the `core.convert` namespace too
    - Conversion functions belong in the namespace that their destination types belong in
- TODO transition the quantum.core.* namespaces:
  - List of semi-approximately topologically ordered namespaces to make typed:
    - quantum.core.core
    - quantum.core.type.core
    - quantum.core.ns
    - quantum.core.logic
    - quantum.core.fn
    - quantum.core.data.map
    - quantum.core.type
    - quantum.core.vars

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
