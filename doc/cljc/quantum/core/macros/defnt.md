# `defnt`

`defnt` is a way of defining an efficiently-dispatched dynamic, type-checked function without resorting to using the un-function-like syntax of `defprotocol` and/or using the tedious `reify`. An example of it is the following:

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


## Spec'ed `defnt`s

Another thing that would be nice is to marry `defnt` with `clojure.spec`.
We want the specs to be reflected in the parameter declaration, type hints, and so on.

We also want it to know about e.g., since a function returns `(< 5 x 100)`, then x must be not just a number, but *specifically* a number between 5 and 100, exclusive. Non-`Collection` datatypes are opaque and do not participate in this benefit (?).

Actually, it would be nice to lazily compile only the needed overloads of `defnt`.
If you use without wrapping in `fnt` or `defnt`, e.g. `(do ...)`, then the overload resolution is done via protocol dispatch.

The protocol vs. interface dispatch should (configurably) emit a warning.

```clojure
(defnt-spec example
  ([[a (s/and even? #(< 5 % 100))]
    [b t/any]
    [c ::number-between-6-and-20]
    [d {:req-un [[e  t/boolean? true]
                 [:f t/number?]
                 [g  (s/or* t/number? t/sequential?)
                     0]]}]]
   {:pre  (< a @c))
    :post (s/and (s/coll-of odd? :kind t/array?)
                 #(= (first %) c))}
   ...)
  ([[a string?]
    [b (s/coll-of bigdec? :kind vector?)]
    [c t/any]
    [d t/any]
   ...))

; expands to =>

(dv/def ::example:a (s/and even? #(< 5 % 100)))
(dv/def ::example:b t/any)
(dv/def ::example:c ::number-between-6-and-20)
(dv/def-map ::example:d
  :conformer (fn [m#] (assoc-when-not-contains m# :e true :g 0))
  :req-un [[:e t/boolean?]
           [:f t/number?]
           [:g (s/or* t/number? t/sequential?)]])
(dv/def ::example:__ret
  (s/and (s/coll-of odd? :kind t/array?)
                 #(= (first %) (:c ...)))) ; TODO fix `...`

-> TODO should it be:
(defnt example
  [^example:a a ^:example:b b ^example:c c ^example:d d]
  (let [ret (do ...)]
    (validate ret ::example:__ret)))
-> OR
(defnt example
  [^number? a b ^number? c ^map? d]
  (let [ret (do ...)]
    (validate ret ::example:__ret)))
-> ? The issue is one of performance. Maybe we don't want boxed values all over the place.

(s/fdef example
  :args (s/cat :a ::example:a
               :b ::example:b
               :c ::example:c
               :d ::example:d)
  :fn   ::example:__ret)
```

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

## Type inference

```clojure
  (expr-info '(let [a (Integer. 2) b (Double. 3)] a))
; => {:class java.lang.Integer, :prim? false}
  (expr-info '(let [a (Integer. 2) b (Double. 3)] (if false a b)))
; => nil
;    But I'd like to have it infer the "LCD", namely, `(v/and number? (v/or* (fn= 2) (fn= 3)))`.

I realize that this also is probably prohibitively expensive.

  (expr-info '(let [a (Integer. 2) b (Double. 3)] (if false a (int b))))
; => nil (inferred `Integer` or `int`)

  (expr-info '(let [a (Integer. 2) b (Double. 3)] (if false a (Integer. b))))
; => {:class java.lang.Integer, :prim? false}
```

At very least it would be nice to have "spec inference". I.e. know, via `fdef`, that a function meets a particular set of specs/characteristics and so any call to that function will necessarily comply with the type.

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

