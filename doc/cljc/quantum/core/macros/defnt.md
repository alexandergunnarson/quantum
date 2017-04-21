# `quantum.core.defnt`

## Overload Resolution

## Matching Functions with Arguments

In the below notation, `&` represents a function-argument satisfiability predicate: `A & B` is true iff `a ⊇ b`, where `a ∈ A` and `b ∈ B`, for at least one `a` and one `b`.

### Example 1

The function `f`, described below, has two overloads: one which takes an `int` and produces an `int`, and another which takes a `long` and produces either a `boolean` or a `String`. In practice, because of specificity rules in force with respect to overload resolution (cf. [Overload Resolution]()), this means that numbers which fall in the range of an `int` return an `int`, and those which fall in the range of a `long` but outside that of an `int` (i.e. `(long - int)`) return a `boolean` or a `String`:

```
f : [#{int}]  -> #{short}
  : [#{long}] -> #{boolean String}
```

The argument `x`, described below, might be a `boolean`, `int`, or `String`:

```
x = #{boolean int String}
```

The valid argument types of `(f x)`, then, are computed below:

```
(f x) :     (argtypes f)  &    (types [x])      ?
      => ⸢ #{[#{int}]   ⸣    ⸢ #{[#{boolean}]  ⸣
             [#{long}]}   &     [#{int}]        ?
         ⸤              ⸥    ⸤   [#{String}]}  ⸥
      => #{[#{int}]}                            ✓
```

The valid return types are thus easily found via a lookup:

```
#{#{short}}
```

A map can then be generated from argument types to return types:

```
{[#{int}] : #{short}}
```

### Example 2

The below example uses the same notation, but this time uses 'free' constraints (i.e. ones not encapsulated in a type such as `PositiveLong` or `NumberLessThan12`). Employing such constraints is normally assumed to be beyond the capabilities of the sort of `∀` proof done by a type checker, and thus to fit exclusively within the scope of a merely `∃` "soft proof" performed by e.g. generative testing (`core.spec` being a prime example). The idea is to check as much as possible at compile time but leave the rest to generative tests and, as a last resort, runtime checks.

```
g : [#{long < 15}, #{int}] -> #{boolean}
  : [#{String}]            -> #{String}
y = #{int < 10, String}
z = #{short >= 5, boolean}
(g y z) :     (argtypes f)        &   (types [y z])              ?
        => ⸢ #{[long < 15, int] ⸣    ⸢ #{[int < 10, short >= 5] ⸣
               [String]}          &     [int < 10, boolean]
                                        [String  , short >= 5]   ?
           ⸤                    ⸥    ⸤   [String  , boolean]}   ⸥
        => #{[long < 15, int]}    &   #{[int < 10, short >= 5]}  ?
        => ; (long < 15) ⊇ int < 10   ✓
           ; int         ⊇ short >= 5 ✓
           #{[long < 15, int]}                                   ✓

{[#{long < 15}, #{int}] : #{boolean}}
```

## OLD DOCUMENTATION: TODO EXCISE

`defnt` is a way of defining an efficiently-dispatched dynamic, type-checked function without resorting to using the un-function-like syntax of `defprotocol` or `reify`. An example of it is the following:

```
(defnt ^java.math.BigInteger ->big-integer
  ([^java.math.BigInteger x] x)
  ([^clojure.lang.BigInt  x] (.toBigInteger x))
  ([#{(- number? BigInteger BigInt)} x]
    (-> x long (BigInteger/valueOf))))
```

This will be faster than the clojure.lang.RT version (I haven't tested the example function above but similar ones, yes), because instead of doing possibly O(n) hops (instanceof checks) to dispatch on type, it does it in only O(1) with the `reify` version of `defnt` under the hood if it can determine the type of the first argument to `->big-integer`, or O(log32(n)) if it can't determine the type at runtime, in which case it uses the `defprotocol` version. Additionally, for convenience, one can define predicates such as `number?` to use in the type check, which will expand to the set `#{int, long, float, double, Integer, Long, BigDecimal ...}`. And then one can e.g. take the difference of that set or union it, etc. to more expressively define the set of types that are accepted in that particular arity of the function. All the expansion of type predicates happens at compile time.

As another example, if three entirely unrelated objects all use `.quit` to free their resources and you're tired of type-hinting to avoid reflection, you could just abstract that to, say:
```
(defnt free
  ([#{UnrelatedClass0 UnrelatedClass1 UnrelatedClass2} x] (.quit x))
```
Voila! No type hints needed anymore, and no performance hit or repetitive code with `cond` + `instance?` checks.


## Warnings
- The protocol vs. interface dispatch should (configurably) emit a warning.

## Performance
- It would be nice to lazily compile only the needed overloads of `defnt`.
  *(make it work with lazy loading first, and **only then** handle eager loading)*
  - If this happened in Clojure or ClojureScript:
    - In loading a file or in REPL development,
      - It would force a compile if the overload was known to be valid but wasn't compiled yet, *and* it was called outside of a function
    - In AOT compilation (via `lein` for Clojure or Google Closure Compiler for ClojureScript),
      - For Clojure,
        -     If you could guarantee that the consumer of the AOT artifact had bytecode generation capability
          AND If the consumer of the AOT artifact found some degree of non-AOT compilation acceptable,
          - Lazy loading would be possible
        - Otherwise,
          - If you could guarantee that all code will be invoked only from certain entry points (e.g. `-main`) (note that non-bytecode generating, 'interpreting' `eval` is fine because it will be compiled),
            - Lazy loading would still be possible, as all needed overloads would be compiled AOT
          - Otherwise *all* valid overloads would need to be compiled (probably prohibitive)
      - For ClojureScript,
        -     If you could guarantee that `eval` was suddenly possible and performant with the Google Closure Compiler (big stretch here)
          AND If the consumer of the AOT artifact found some degree of non-AOT compilation acceptable,
          - Lazy loading would be possible
        - Otherwise,
          - If you could guarantee that all code will be invoked only from certain entry points (e.g. `-main`) (note that non-bytecode generating, 'interpreting' `eval` is fine because it will be compiled),
            - Lazy loading would still be possible, as all needed overloads would be compiled AOT
          - Otherwise *all* valid overloads would need to be compiled (probably prohibitive)
  - Only reload (or invalidate the cache) for those signatures/bodies that did change.

## `clojure.spec` + protocols/interfaces

A goal might be to merge clojure.spec with protocols and interfaces like so:

```clojure
(def-validated double-between-3-and-5-exclusive
  (v/and double? #(< 3 % 5)))

(def-validated double-between-1-and-8-inclusive
  (v/and double? #(< 1 % 8)))

(defnt ^double-between-1-and-8-inclusive my-fn
  [^double-between-3-and-5-exclusive v]
  (+ @v 10))
```

and have the compiler complain.
I realize that this is probably prohibitively expensive, though.


## Best practices

- Prefer primitives to boxed types whenever possible

## TODO

Look at:

- implementation
  - http://www.stroustrup.com/multimethods.pdf
  - Fast associative structures
    - http://preshing.com/20130605/the-worlds-simplest-lock-free-hash-table/
      - for a super fast int->int ConcurrentHashMap equivalent
    - Off-heap?
  - Julia multiple dispatch implementation
    - http://stackoverflow.com/questions/32144187/how-does-julia-implement-multimethods
    - https://github.com/JuliaLang/julia/blob/master/src/gf.c
- types
  - Look at https://github.com/LuxLang/lux and https://github.com/jeaye/jank
- ztellman/potemkin
  - definterface+
    Every method on a type must be defined within a protocol or an interface. The standard practice is to use defprotocol, but this imposes a certain overhead in both time and memory. Furthermore, protocols don't support primitive arguments. If you need the extensibility of protocols, then there isn't another option, but often interfaces suffice.
    While definterface uses an entirely different convention than defprotocol, definterface+ uses the same convention, and automatically defines inline-able functions which call into the interface. Thus, any protocol which doesn't require the extensibility can be trivially turned into an interface, with all the inherent savings.

