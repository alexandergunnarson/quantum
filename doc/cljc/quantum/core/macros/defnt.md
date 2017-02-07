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

```clojure
(defnt-spec perceptron
  ([data   (s/and t/number? even?)
    other1 t/any
    other2 ::validated-deftype-1
    opts   {:req-un [[bias?          t/boolean? true]
                     [:learning-rate t/number?]
                     [weights        (s/or* t/number? t/sequential?)
                                     0]]}]
   (s/and (s/coll-of (s/and odd?) :kind t/array?))
   ...))

; expands to =>

(def-validated ::perceptron:data   (s/and t/number? even?))
(def-validated ::perceptron:other1 t/any)
(def-validated ::perceptron:other2 ::validated-deftype-1)
(def-validated-map ::perceptron:opts
  :conformer (fn [m] (assoc-when-not-contains m :bias? true :weights 0))
  :req-un [[:bias?         t/boolean?]
           [:learning-rate t/number?]
           [:weights       (s/or* t/number? t/sequential?)]])
(def-validated ::perceptron:__ret
  (s/and (s/coll-of (s/and odd?) :kind t/sequential?)))

(defnt perceptron
  [^number? data other1 ^validated-deftype-1 other2 ^map? opts]
  (let [_ (validate data   ::perceptron:data
                    other1 ::perceptron:other1
                    other2 ::perceptron:other2
                    opts   ::perceptron:opts)
        bias?   (:bias?   opts)
        weights (:weights opts)
        ret     (do ...)]
    (validate ret ::perceptron:__ret)))

(s/fdef perceptron
  :args (s/cat :data   ::perceptron:data
               :other1 ::perceptron:other1
               :other2 ::perceptron:other2
               :opts   ::perceptron:opts)
  :fn   ::perceptron:__ret)
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

## TODOs


