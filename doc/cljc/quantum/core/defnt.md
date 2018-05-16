# `quantum.core.defnt`

**`defnt` sets out to provide a means whereby contracts may be expressively declared as in `clojure.spec`, enforced as much as possible at compile time, and only then, and only when desired, enforced at runtime.**

What follows is an explanation of the rationale behind `defnt`, some useful background material, and finally some detail concerning its implementation.

# Rationale

# Background

## Types and Properties

Our task at hand is to delineate and model the basic components of abstraction, so that we may provide solid theoretical foundations to `defnt`.

A ***type***, both within the context of `defnt` as well as in set theory and mathematical logic, may be equated to a ***property***. A property, far from an ineffable thing, may be easily defined by two constituents: first, its ***intension***, and second, its ***extension***. We encounter both intensionality and extensionality every day, but rarely attribute these technical terms to what we experience.

For instance, take the property of "triangleness". The *intension* of this property is, in rough terms, equivalent to its meaning or its definition. It includes properties like "having three sides", "having angles whose measures add up to exactly 180 degrees (in Euclidean geometry)", "being a shape", and so on. Intensions are composed of theoretically infinite properties that usually relate to one another in logical and set-theoretic ways. For instance, the property of "having three sides" logically entails the property of "being a shape" (but not vice versa): the intension of "having three sides" is a superset of the intension of "being a shape". Sometimes properties logically entail each other — for instance, the property of "having three sides" logically entails the property of "having angles whose measures add up to exactly 180 degrees", and vice versa. To those uninitiated to geometry, the two properties seem to be unrelated, but this bidirectional logical entailment means that these properties are actually ***co-intensional***: that is, they are perfectly synonomous; they mean the exact same thing.

Suppose you were able to strip away all of the derived properties of "triangleness" down to a set of its fundamental properties. Then these properties would amount to the ***necessary*** properties of "triangleness": take even one of them away, and the resulting set of properties no longer describes a triangle.

Now let's turn our attention to the *extension* of the property of "triangleness". Extensionality, rather than dealing with abstract meaning, deals in objects to which that meaning is applied. The extension of the property of triangleness amounts to the set of all triangles.

Properties cannot be defined exclusively on their extension, for extensionality is contingent: properties may be co-extensional without being co-intensional. For instance, take two properties: "having a physical human brain" and "having a physical human heart". These properties are clearly not co-intensional; that is, while they both relate to the human body, they nonetheless mean two very different things. However, at least prior to the invention of the pacemaker (and speaking of only humans that were alive, so barring e.g. Aztec sacrifical ceremonies), these two properties were co-extensional: that is, everything that had a physical human brain also had a physical human heart.

Intensionality and extensionality are, in one way, inversely related. More requirements ("intensors") result in fewer entities that meet those requirements ("extensors").


To say that an entity `E` "has" a property `P` is equivalent to saying that `E` is a member of the set `P`. An apple is red, and so an apple is a member of the set of all red things. Likewise, a vector is sequential, and thus a vector is a member of the set of all sequential things.

Let's take the example of the `PersistentVector` property (type) in Clojure. Its superproperties ("superclasses" in object-oriented terms) include `Sequential`, `Counted`, `Indexed`, `APersistentVector`, `Object`, and so on. There are more requirements ("necessary properties" or "intensors") for an object to qualify as a `PersistentVector` than for one to qualify as e.g. merely `Sequential`. More requirements result in fewer entities ("extensors") that meet those requirements. Thus, `PersistentVector` amounts to a superset of the intension of, and a subset of the extension of these superproperties.

That the first three are interfaces, the fourth is an abstract class, and the fifth is a concrete class are, with respect to `defnt`, largely irrelevant implementation details: rather, each of these things are to be considered "reified types", as they are each properties — *types* — encapsulated by a class that has been created — *reified* — in code.

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

Thus, any time we passed around a `NumberLessThan7`, we could rest assured that its field, `x`, would always have the property of being a number less than 7, i.e., that it would belong to the set `S`, and thus satisfy its corresponding defining predicate ("intensor").

With `NumberLessThan7`, we have just created a reified type. The class `NumberLessThan7` is a reified version of the abstract type `S`.

Some reified types come pre-defined by and intrinsic to the virtual machine, even before any standard libraries have been loaded. For Java, these are `boolean`, `byte`, `char`, `short`, `int`, `long`, `float`, `double`, and `Object`; for JavaScript, `Boolean`, `Null`, `Undefined`, `Number`, and `String` (and in ECMAScript 6, `Symbol`). Java `int`, for example, reifies the abstract type defined by `T = {t : t ϵ ℤ; -2,147,483,648 ≤ t ≤ 2,147,483,647}`.

### Types As Minimal Sets

Note that an object, though traditionally typed within the context of a code block as being of only one particular class, may inherently belong to quite a number of otherwise unrelated property-sets. For example, by its nature, the number `1` may be considered an element of the extensions of a number of types in Java, including `short`, `Short`, `int`, `Integer`, `long`, `Long`, `float`, `Float`, `double`, `Double`, `Comparable`, and `Object`. The type `short`, defined as `short = {x : x ϵ ℤ; -32,768 ≤ x ≤ 32,767}` contains the number `1`, regardless of its internal representation (bit-structure). Likewise, since `1` is an element of `double` (again regardless of its internal representation) which may be cast to `Double`, and `Double` is a superset (subclass) of `Comparable`. ... TODO

The relations of subset and subclass (and their duals, superset and superclass) are deeply intertwined. The `Double` class implements the `Comparable` interface, which means that it is (in some sense) a subclass of `Comparable`. By definition, then, the set of all objects belonging to the type `Double` is a subset of the set of all objects belonging to the type `Comparable`: there are `Comparable`s that are not `Double`s. But it is worth noting that `Double`s provide more than `Comparable` requires: `Double`s are not only `Comparable`, but, being numbers, they may be mathematically manipulated in ways that not all `Comparable`s can. Thus, though the elements of `Double` are a subset of the elements of `Comparable`, the elements of `Double` belong in more sets. ... TODO

When we speak of the "type" of an object or expression, we mean its abstract type. However, while an object's type is equivalent to its reified type, an expression's type may not even be representable by any combination of unions, intersections, or other set operations on reified types existent at the time of type-analysis. That said, any logically consistent abstract type may be reified.

### Equivalence to Contracts

It is worth noting that types are equivalent to contracts. Fundamentally, contracts specify the legal states of objects (a definition which, when inputs and outputs are themselves considered objects, supersedes the notion of contracts as also specifying the legal states of the inputs and outputs of a function). Contracts thus specify what sets objects must belong to (and often, what sets objects must *not* belong to), and thus what types objects must (and often, must *not*) be.

Static typing, then, is simply compile-time contract enforcement, and dynamic typing, enforcement at runtime.

# Implementation

For this task, it was necessary to create a type analysis tool that goes beyond the Hindley-Milner type system and allows expressions to belong not only to one single reified type, but to any number of abstract types. Only in this way was it possible to achieve both maximal expressivity and maximal runtime performance.

## Implicit Type Conversion

A relevant question to ask is, should implicit type conversion be supported? If so, where?

For instance, the data represented by a `byte`, since it is really just a number, can be represented as:

- a `short` or `Short`
- a `char` or `Character`
- an `int` or `Integer`
- a `long` or `Long`
- (arguably) a `float` or `Float`
- (arguably) a `double` or `Double`
- a `BigInteger` or `clojure.lang.BigInt`
- (arguably) a `BigDecimal`
- etc.

Likewise, the data represented by a `String`, since it is really just a sequence of characters, can be represented as:

- a Clojure `seq` of (Unicode) characters (e.g. `int`s, as some Unicode characters fall outside the range representable by an unsigned byte (`char`) or signed byte (`byte`))
- an array of (Unicode) characters
- an array of `char`s, if each element of the `String` falls within the range of a `char` (i.e. is within the Basic Multilingual Plane (BMP))
- an array of `byte`s, if each element of the `String` falls within the range of a `byte` (i.e. is an ASCII character)
- a serialized, possibly compressed, version of either of these
- etc.

In `defnt`, the decision was made to not support implicit type conversion, but instead leave it up to the user to decide which bit-representations to support for a given function.

## Variable Type Resolution

Suppose that you have the following:

```

```

What types of `a` and `b` should be allowed, in what combination? What would the corresponding return types be?

Unlike a simpler call to `quantum.core.Numeric/add`

```
{[byte byte]}
```

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

The below example uses the same notation, but this time uses 'abstract' constraints (i.e. ones not 'reified' by / encapsulated in a type such as `PositiveLong` or `NumberLessThan12`). Employing such constraints is normally assumed to be beyond the capabilities of the sort of `∀` proof done by a type checker, and thus to fit exclusively within the scope of a merely `∃` "soft proof" performed by e.g. generative testing (`core.spec` being a prime example). The idea is to check as much as possible at compile time but leave the rest to generative tests and, as a last resort, runtime checks.

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

## Multiple output constraints

Typed functions can have multiple possible output constraint ('return types'). Sometimes it depends on the conditional structures; sometimes it depends on the constraints themselves.

`(defn [a] (if (pos? a) 'abc' 123))`

Input constraints:
  {'a (constraints pos?)}
Output constraints:
(if (constraints pos?)
    String ; won't keep track of e.g. the fact it only contains 'a', 'b', 'c' and what that entails
    long)

Example:

(defnt [a ?]
  (cond (pos? a)
        123
        (string? a)
        'abd'))

Input constraints:
  (union (constraints pos?)
         (constraints string?)) ; unreachable statement: error

Example:

(defnt [a ?]
  (cond (pos? a)
        123
        (number? a)
        'abd'))

Input constraints:
  (union (constraints pos?)
         (constraints number?)) ; optimized away because it is a subset of previously calculated constraints
Output constraints:
  ...

Unlike most static languages, a `nil` value is not considered as having a type
except that of nil.

## Why type inference is not a great idea

Take the below code:

```clojure
(defnt transduce
  ([      f ?,         xs ?] (transduce identity f     xs))
  ([xf ?, f ?,         xs ?] (transduce xf       f (f) xs))
  ([xf ?, f ?, init ?, xs ?]
    (let [f' (xf f)] (f' (reduce f' init xs)))))
```

- For the `f` in the 1-arity overload:
  - Inferred from `(transduce identity f xs)`
  - The `f` in the 3-arity overload is then inferred:
    - We know that `xf` can be called on `f`, so `xf` must be at least a 1-arity `t/callable?` on `f`
    - We know that `f'` can be called on `(reduce f' init xs)`, so `xf` must be at least a 1-arity `t/callable?` on `f`
    - Other than that we don't really have any information about `f`
- For the `f` in the 2-arity overload:
  - We know that `f` can be called with no arguments, so it must be at least a 0-arity `t/callable?`
  - We know that it can be passed to `(transduce xf f (f) xs)`
  - We tried to infer the `f` in the 3-arity but it can't be known

It is infeasible to do inferences in the general case for the following reasons:
- The code will be complex and greatly increase time it takes to get any value out of `defnt`
- The code will likely have high computational complexity even if some impressive algorithm comes out of it
- Even if the code could do it instantly, it would still be a maintenance issue to try to mentally work out for each inference what that ends up being. Labels help quite a lot.

I think the best approach is not inference, but rather being able to at least do:
- Input/output specs that rely on the input/output specs of other spec'ed fns
- Conditional specs

Thus the code turns into:
*(TODO: conditionally optional arities etc.)*

```clojure
(t/def rf? "Reducing function"
  (t/fn [    {:doc "seed arity"}]
        [_   {:doc "completing arity"}]
        [_ _ {:doc "reducing arity"}]))

(t/def xf? "Transforming function"
  (t/fn [rf? > rf?]))

(defnt transduce
  ([        f rf?,        xs t/reducible?] (transduce identity f     xs))
  ([xf xf?, f rf?,        xs t/reducible?] (transduce xf       f (f) xs))
  ([xf xf?, f rf?, init _ xs t/reducible?]
    (let [f' (xf f)] (f' (reduce f' init xs)))))
```

which is much, much nicer because it's much better documented, much more clear what each input and output does, and just overall much easier to follow and reason about, without introducing a meaningful increase in code size, and certainly without adding unnecessary information.
