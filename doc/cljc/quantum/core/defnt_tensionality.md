# Tensionality within `defnt`

## Introduction

A ***spec***, within the context of `defnt`, is defined by two closely-related constituents: first, its ***intension*** , and second, its ***extension***. We encounter both intensionality and extensionality every day, but rarely attribute these technical terms to what we experience.

The *intension* of a spec, roughly speaking, is its "inner meaning" as expressed by the source code that defines it, equivalent to a (logical) predicate. Its *extension* consists of the set of elements for which it holds true.

## Intension

Take the spec `t/string`. We define its *intension* as the source code that defines it. This (perhaps somewhat redundantly) may alternatively be modeled as a minimal set of criteria which cause only the "correct" elements to be included in the extension of `t/string`. There may be many (possibly infinite) such sets for a given spec.

## Extension

Under some interpretations, the extension of a set may be taken to be with reference to the ***actual*** world: that is, it may be taken to refer only to the *actual* elements of the set, excluding merely *possible* ones. In such a case, the extension of `t/string?` might be taken to be the set of all instantiated strings (in some context, whether it be all strings instantiated on the currently running JVM, or on all machines running currently, or perhaps on all machines ever).

However, for purposes of `defnt` we interpret the extension of a set to consist not merely of its actual elements, but its possible ones as well (unless otherwise specified by its intension). That is, we interpret the extension of `t/string?` to be the set of all possible strings, not merely the set of all instantiated strings. We will call the common sense of the term "extension" "actual extension", and whenever disambiguation is necessary, our interpretation of the term "extension", "complete extension" ("actual extension" ∪ "possible extension").

## Intensors and Extensors

If an intension is itself considered a set of criteria — informational boundaries, if you will — the elements of such a set we call ***intensors***. Likewise the elements of an extension we call ***extensors***. More (distinct) intensors, of course, result in fewer extensors that meet their criteria, and adding extensors necessitates the removal or relaxation of intensors.

The granularity of an extensor is simple, but that of an intensor is often ill-defined even for the most basic of them. Take the simple predicate `#(= % %)`. What are its intensors? The whole predicate, may be expressed in natural language as "is equal to itself", but how does one analyze it? Approaches such as analyzing down to a truth function, or down to some subset of the AST of the function `=`, are unsatisfying.

`defnt` sidesteps the complexity involved in the definition of intensors and focuses instead on extensors.

## Co-extensionality and Co-intensionality

Now let's take a more everyday example. Take two predicates: "having a physical human brain" and "having a physical human heart". While they both relate to the human body, they nonetheless mean two very different things: that is to say, they are not ***co-intensional***.

Prior to the invention of the pacemaker (and speaking of only humans that were alive, so barring e.g. Aztec sacrifical ceremonies), everything that had a physical human brain also had a physical human heart: these two predicates were ***contingently co-extensional*** — that is to say, they were co-extensional on some possible world (namely, the actual world before the invention of the pacemaker).

Contingent co-extensionality differs from ***necessary co-extensionality***. If "having a physical human brain" and "having a physical human heart" were indeed necessarily co-extensional, as some had previously supposed, there would be no possible world on which a living human had a physical human brain without a a physical human heart, or vice versa. However, once the first pacemaker was implanted, it was shown irrefutably the two predicates were indeed merely *contingently*, not *necessarily*, co-extensional.

Some predicates really *are* necessarily co-extensional without being co-intensional. For instance, "is an equilateral triangle" and "is an equiangular triangle" are co-extensional on all possible worlds. But for those uninitiated to geometry, equilaterality and equiangularity have no obvious relationship.

Within the context of `defnt`, predicate pairs are merged when combined with a typed, unordered logical operator like `t/or*` or `t/and*`, only if they are necessarily co-extensional, regardless of the relationship between their intensionalities, as the following examples will demonstrate:

### Example 1

```
(defn f [x] (= x (*   x 1)))
(defn g [x] (= x (exp x 1)))
```

For those uninitiated in basic math, multiplying by 1 seems not to be equivalent to exponentiating by 1, and so the above predicates have distinct intensionalities. However, they are nonetheless necessarily co-extensional and can be merged.

### Example 2

```
(defn p [x] (= x (* x 1)))
(defn q [x] (number? x))
```

These predicates don't even appear ask the same question, so they are clearly not co-intensional. However, everything that satisfies `q` satisfies `p`, and vice versa, so they are necessarily co-extensional and can be merged. This is further evidenced by the fact that multiplication isn't even possible without `x` being a number.

### Example 3

```
(let [c one-person-city]
  (defn tallest-person-in-c?  [x] (tallest-person-in-city?  x c))
  (defn shortest-person-in-c? [x] (shortest-person-in-city? x c)))
```

These again appear to ask different questions. However, given the same one-person city `c`, the tallest person in it and the shortest person in it will always be the same (under standard definitions of these predicates), so they are necessarily co-extensional and can be merged. (If the predicates referred to different cities, they might be contingently co-extensional, but certainly not necessarily so.)

### Example 4

```
(defn human-that-has-living-human-brain? [x] ...)
(defn human-that-has-living-human-heart? [x] ...)
```

Since these are contingently co-extensional, these are not merged. Even if the implementation assumes the universe of discourse is limited to people before the invention of the pacemaker, the two predicates wouldn't be merged because whether they are co-extensional in such a circumstance seems to be entirely up to empirical observation (e.g. insights into people historically living for even brief moments without a living heart but with a living brain) rather than logical deduction.

### Example 5

```
(defn morning-star? [x] (venus? x))
(defn evening-star? [x] (venus? x))
```

Though in principle the predicates "is the morning star" and "is the evening star" as expressed in natural language are of distinct intension and contingently co-intensional, the implementations here are equivalent, and thus co-intensional, and therefore necessarily co-extensional and mergeable.

### Example 6

```
(defn equiangular-triangle? [x] (and (instance? TriangleImpl0 x) ...))
(defn equilateral-triangle? [x] (and (instance? TriangleImpl1 x) ...))
```

A similar phenomenon happens here. Though "is an equiangular triangle" and "is an equilateral triangle" as expressed in natural language are of distinct intension and necessarily co-extensional, they are of distinct extension as implemented above, provided `TriangleImpl0` and `TriangleImpl1` are class-hierarchically unrelated.
