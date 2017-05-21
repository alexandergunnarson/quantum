# `quantum.core.defnt`

**`defnt` sets out to provide a means whereby contracts may be expressively declared as in `clojure.spec`, enforced as much as possible at compile time, and only then, and only when desired, enforced at runtime.**

What follows is an explanation of the rationale behind `defnt`, some useful background material, and finally some detail concerning its implementation.

# Rationale

# Background

## Types and Properties

Our task at hand is to delineate and model the basic components of abstraction, so that we may provide solid theoretical foundations to `defnt`.

In most formulations of set theory (including the widely accepted ZFC, Zermelo-Fraenkel set theory with the Axiom of Choice), essentially everything may be reduced to a ***set***, the fundamental unit of aggregation. The notion of ***ordering*** or sequentiality may be modeled in set form in a theoretically infinite number of equivalent ways, the Kuratowski definition being the generally accepted one (under which e.g. the ordered 2-tuple (pair) `[a, b]` becomes the equivalent set `{{a}, {a, b}}`). A ***relation*** (or equivalently, "predicate" — the namesake of predicate logic) may be modeled as a set of ordered tuples. A ***property***, or attribute, may be modeled as a unary relation (i.e., one in which every constituent tuple is a 1-tuple). A ***function*** may be modeled as a relation whose first element consists of an ordered tuple of inputs, and whose second element consists of an output. A ***deterministic function*** is one in which all constituent pairs contain unique first elements; a ***nondeterministic function*** is one which does not adhere to this stipulation. ***Logical operators*** such as `not`|`~`|`!`|`¬`, `and`|`&&`|`∧`|`⋅`, `or`|`||`|`∨`|`+`, and so on may be modeled as (truth) functions, and correlated with their respective set operators: `not` with set complement, `and` with set intersection `∩`, `or` with set union `∪`, and so on.

`true` is equivalent to inclusion in a set. Universal truth is defined as the set of all sets, `S`.
`false` is equivalent to exclusion from a set. Universal falsehood is defined as the empty set, `∅`.

`nand` (Sheffer stroke), or `nor` (Peirce arrow).
Lambda calculus can express any computation via variable (the arguments to `fn`), abstraction (`fn`), and application (function call).
Church encodings for numerals, tuples, logical operators

- `Pa ∧ Pb` = `(P ∩ {[a]}) ∪ (P ∩ {[b]})`

First-order logic may be shown to be equivalent to set theory.
- Truth is contingent on the non-emptiness of a set: a non-empty set denotes `true`, while an empty set `∅` denotes `false`.
- `~Pa` = `(P ∩ {[a]})`
- `Pa ∨ Pb` = `[([a] ϵ P), ([b] ϵ P)] ϵ ∨`
- `Pa ∧ Pb` = `[([a] ϵ P), ([b] ϵ P)] ϵ ∧`
- `∃xPx` = `U ∪ P`
- `∀xPx` = `U ∩ P`
- `∀x(Ax ∨ Bx)` = `A ∪ B`
- `∀x(Ax ∧ Bx)` = `A ∩ B` — the differentiation between the result-set "all the things that are A and B" and the truth-valued statement "all things are either in A or B"


`S` denotes the set of all sets.
`U` denotes the universe of discourse, by default `S`.
`∀` denotes a conjunction over all the elements in its `U` (by default the set of all sets): `∀xPx` = `Pa ∧ Pb ∧ ...`; `∧` being equivalent to `∩`, and a predicate (such as the predicate `P`) being equivalent to a special kind of set, this is equivalent to `(P ∩ {[a]}) ∩ (P ∩ {[b]}) ...`, or more tersely, `{x : x }`.
 `∃` denotes an infinite disjunction

can make do with one quantifier (`∃`, infinite  or `∀`) and one self-sufficient logical operator (`nand`|`↑` or `nor`|`↓`), as well as variable scope, of course.



A predicate `P` may be defined as a function from *all things* (the constituent elements of the set of all sets) to the truth value of whether a particular thing satisfies `P`. The ***extension*** of `P` may be defined as the set of all things for which `P` is true.

The extension of the property of `being red-colored` is a subset of the extension of the property of `having a color`

Properties are often compared to sets and sometimes even assimilated to them. Just as properties can have instances, sets can have members, and it is typically assumed that, given a property, there is a corresponding set, called the extension of the property, having as members exactly those things that exemplify the property. But it is important to note a fundamental difference between the two. Sets have clear-cut identity conditions: they are identical when they have exactly the same members. In contrast, the identity conditions of properties are a matter of dispute. Everyone who believes there are properties at all, however, agrees that numerically distinct properties can have exactly the same instances without being identical. Even if it turns out that exactly the same things exemplify a given shade of green and circularity, these two properties are still distinct. For these reasons sets are called extensional and properties are often said to be intensional entities. Precisely because of their intensional nature properties were dismissed by Quine (1956) as ‘creatures of darkness’ and just a few decades ago many philosophers concurred with him. But philosophers now widely invoke properties without guilt or shame.


A type, both within the context of `defnt` as well as in set theory and mathematical logic, is equivalent to a property. A property, far from an ineffable thing, may be easily defined as a set to which an entity belongs; thus, to say that an entity `E` "has" a property `P` is equivalent to saying that `E` is a member of the set `P`. An apple is red, and so an apple is a member of the set of all red things. Likewise, a vector is sequential, and thus a vector is a member of the set of all sequential things.

In Clojure, the `PersistentVector` type is a superset of (or in object-oriented terms, subclass) of many other types, only a few of which include `Sequential`, `Counted`, `Indexed`, `APersistentVector`, and `Object`. That the first three are interfaces, the fourth is an abstract class, and the fifth is a concrete class are, with respect to `defnt`, largely irrelevant implementation details: rather, each of these things are to be considered "reified types", as they are each properties — *types* — encapsulated by a class that has been created — *reified* — in code.

### Abstract Types vs. Reified Types

Abstract types stand in contrast to reified types. Contrary to first impression, the term "abstract" does not refer, as it does in Java and other object-oriented programming languages, to lack of class-instantiability or the fact that it can have fields. Neither does the term "reified" refer, as it does in Clojure, to an implementation of one or more interfaces by an anonymous class. Let us characterize these two new terms, "abstract" and "reified", by way of example. Take the set `S` of all numbers less than 7. We might represent it in mathematical notation as `S = {s : s < 7}`, or in Clojure as the predicate `#(< % 7)`. `S` by definition represents a property, and thus, as we noted above, a type. We could imagine creating a class in Java to encapsulate this property like so:

```java
public class NumberLessThan7 {
  public final Number x;

  public NumberLessThan7 (Number x_) {
    if (lessThan(x_, 7)) {// imagine here a `lessThan` function that worked on any two numbers
      x = x_;
    } else {
      throw new Exception("Number must be less than 7");
    }
  }
}
```

Thus, any time we passed around a `NumberLessThan7`, we could rest assured that its field, `x`, would always have the property of being a number less than 7, i.e., that it would belong to the set `S`, and thus satisfy its corresponding defining predicate.

With `NumberLessThan7`, we have just created a reified type. The class `NumberLessThan7` is a reified version of the abstract type `S`.

Some reified types come pre-defined by and intrinsic to the virtual machine, even before any standard libraries have been loaded. For Java, these are `boolean`, `byte`, `char`, `short`, `int`, `long`, `float`, `double`, and `Object`; for JavaScript, `Boolean`, `Null`, `Undefined`, `Number`, and `String` (and in ECMAScript 6, `Symbol`). Java `int`, for example, reifies the abstract type defined by `T = {t : t ϵ ℤ; -2,147,483,648 ≤ t ≤ 2,147,483,647}`.

### Types As Minimal Sets

Note that an object, though typed within the context of a code block as being of a particular class, may inherently belong to quite a number of property-sets. For example, by its nature, the number `1` may be considered an element of a number of types in Java, including `short`, `Short`, `int`, `Integer`, `long`, `Long`, `float`, `Float`, `double`, `Double`, `Comparable`, and `Object`. The type `short`, defined as `short = {x : x ϵ ℤ; -32,768 ≤ x ≤ 32,767}` contains the number `1`, regardless of its internal representation (bit-structure). Likewise, since `1` is an element of `double` (again regardless of its internal representation) which may be cast to `Double`, and `Double` is a superset (subclass) of `Comparable`. ... TODO

The relations of subset and subclass (and their duals, superset and superclass) are deeply intertwined. The `Double` class implements the `Comparable` interface, which means that it is (in some sense) a subclass of `Comparable`. By definition, then, the set of all objects belonging to the type `Double` is a subset of the set of all objects belonging to the type `Comparable`: there are `Comparable`s that are not `Double`s. But it is worth noting that `Double`s provide more than `Comparable` requires: `Double`s are not only `Comparable`, but, being numbers, they may be mathematically manipulated in ways that not all `Comparable`s can. Thus, though the elements of `Double` are a subset of the elements of `Comparable`, the elements of `Double` belong in more sets. ... TODO

When we speak of the "type" of an object or expression, we mean its abstract type. However, while an object's type is equivalent to its reified type, an expression's type may not even be representable by any combination of unions, intersections, or other set operations on reified types existent at the time of type-analysis. That said, any logically consistent abstract type may be reified.

### Equivalence to Contracts

It is worth noting that types are equivalent to contracts. Fundamentally, contracts specify the legal states of objects (a definition which, when inputs and outputs are themselves considered objects, supersedes the notion of contracts as also specifying the legal states of the inputs and outputs of a function). Contracts thus specify what sets objects must belong to (and often, what sets objects must *not* belong to), and thus what types objects must (and often, must *not*) be.

Static typing, then, is simply compile-time contract enforcement, and dynamic typing, enforcement at runtime.

# Implementation

For this task, it was necessary to create a type analysis tool that goes beyond the Hindley-Milner type system and allows expressions to belong not only to one single reified type, but to any number of abstract types. Only in this way was it possible to achieve both maximal expressivity and maximal performance.

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

#### Example 2

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

## Dealing with non-localized type deduction

What if, after traversing the AST for a little bit, the type resolver deduces based on the usage of symbol `A` (e.g. within static methods, etc.) that it must be of type `T0`? Previously it thought `A` was `#{T0 T1}`, and disjunctions were created accordingly for several previous callsites. When this inference is arrived at, what should happen?

Perhaps a cascade of rule-based atoms with watches is appropriate. I.e., at each expression will be an atom (really, an unsynchronized mutable container with a watch) that listens for changes to the types/constraints of the expressions on which it depends, and the most appropriate overload will always be chosen based on the latest information. Atoms will only be re-used in more than one expression if they are locally-scoped symbols. Thus, the keys of the environment (these symbols) will only ever be modified when a new scope is introduced via `let` or `fn`, but the values may be mutated at any expression.
