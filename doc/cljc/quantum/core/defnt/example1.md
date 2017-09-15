The purpose of the `defnt` type analyzer is to do just that — analyze types and infer ones to the maximum extent possible. Other code analysis may be done later.

## Rationale

The reason we don't simply assign one static return type to a function call node is that return types can differ depending on the types of the inputs as well as the conditional branches taken within a function. Plus, as we learned with `clojure.spec`, not all important information about a piece of data is (easily) capturable in a static type.

## AST nodes

The basic unit of data the type analyzer works with is an AST node.

All AST nodes have the keys:

- `:form`
- `:form+`
- `:env`
- and `:spec`.

Additionally, there are special keys that correspond to each AST node type. We will approach those later.

For now, let's explore what these keys are and how they are derived.

### `:form`

The primary input to the type analyzer is a (Clojure) *form*. If you don't know what those are, go read about them and come back (it's quick).

### `:form+`

The type analyzer incrementally macroexpands every form it comes across. A `form+` is a `form` which has been macroexpanded one level as via `macroexpand`.

### `:env`

The value of `:env` denotes the "environment" of the AST — that is, the bound symbols defined in the current scope of the AST. Below is its spec:

```clojure
(s/def ::t/env (s/map-of symbol? ::t/ast-node))
```

And an example of an `env`:

```clojure
{'a (ast/literal 1   t/long)
 'b (ast/literal 2.0 t/double)
 'c (ast/unbound 'c)}
```

We will come back to `ast/literal` later. For now, notice that the value of symbol `c` is `(ast/unbound 'c)`. `(ast/unbound <symbol>)` defines a type-unbound symbol, that is, a symbol whose type the type analyzer is to infer.

### `:spec`

A `spec` is any spec created by the utility functions in `quantum.core.type-spec`. Think of them as `clojure.spec` specs but "special" in certain ways explained later on.

Note that the `spec` of an AST node defines the specification of its possible "output values", like so:

- `spec` of an `ast/literal` node: the type of the literal value
  - `spec` of `(ast/literal "abc")` -> `t/string`
- `spec` of an `ast/call` (function call) node: the spec of its return value
  - `spec` of `(ast/call unchecked-inc 1)` -> `t/long`
  - `spec` of `(ast/call inc' 1)` -> technically `(t/or t/long t/bigint?)`, but deduces `t/long` given the input
- etc.

## An example

Now we're ready for an example. We'll keep it limited to mostly static functions for now since those are Suppose we assume the role of the type analyzer and are invoked by the following code:

```
(t/analyze {'c (t/unbound 'c)}
  '(let* [a 1 b (byte 2)]
     a
     (Numeric/add c (Numeric/bitAnd a b))))
```

This says to us that we are to type-analyze the following `form`:

```clojure
(let* [a 1 b (byte 2)]
  a
  (Numeric/add c (Numeric/bitAnd a b)))
```

It also says we are provided with the `env` `{'c (t/unbound 'c)}`. This means we are to infer the `spec` of `c` as we type-analyze the `form` above.

Our objective is always to deduce the `spec` of the `form` we receive. Type inference on type-unbound variables in our environment will occur as we do this.

So, we received a form that begins with `let*`. This means we create an `ast/let*` whose `form` is the form we received.

Remember how we said there are special keys that are only common to certain AST nodes? In the case of `ast/let*`, those are:

- `:bindings` `(s/map-of symbol? ::ast-node)`
- `:body` - `(s/vec-of ::ast-node)`

To begin, then, we have the following at the root node:

```
(ast/let*
  {:form     '(let* [a 1 b (byte 2)] a (Numeric/add c (Numeric/bitAnd a b)))
   :form+    <not-computed>
   :bindings <not-computed>
   :body     <not-computed>
   :spec     <not-computed>) ; our goal is to infer this
```

In order to find the `spec` of the root node, we first have to macroexpand `form` and type-analyze the resulting `form+`. The overall computation will essentially be a postwalk of the eventually-generated descendant nodes, which will cause `spec` values to bubble up the AST.

We first macroexpand the form one level to get `form+`:

```clojure
(macroexpand '(let* [a 1 b (byte 2)] a (Numeric/add c (Numeric/bitAnd a b))))

=> (let* [a 1 b (byte 2)] a (Numeric/add c (Numeric/bitAnd a b)))
```

(Notice how `form+` is the same as `form`. In the future, for brevity, we will not show `form+`s whose corresponding `form`s are equivalent.)

We now have:

```clojure
(ast/let*
  {:env      {'c (ast/unbound 'c)} ; provided
   :form     '(let* [a 1 b (byte 2)] a (Numeric/add c (Numeric/bitAnd a b))) ; provided
   :form+    '(let* [a 1 b (byte 2)] a (Numeric/add c (Numeric/bitAnd a b))) ; calculated; will not be shown in the future
   :bindings <not-computed>
   :body     <not-computed>
   :spec     <not-computed>})
```

### `let*`: `bindings`

Let's keep going and compute the `bindings`.

Since the form `1` is a literal, we enclose it in an `ast/literal`, supplying it with the type-spec corresponding to its class.

The form `(byte 2)` is technically not a literal, but for purposes of simplifying this example, we will treat it as if it had been written in Java like `byte b = 2`.

Here's what we have, then:

```clojure
{'a (ast/literal 1 t/long)
 'b (ast/literal 2 t/byte)}
```

Note that we print out `(ast/literal 1 t/long)` as a shorthand for `(ast/literal {:form 1 :spec t/long})`. This shorthand is common to all `ast/literal` nodes.

### `let*`: `body`

Next we compute the `body`, depth-first:

```clojure
[(ast/symbol
  {:env  {'c (ast/unbound 'c)
          'a (ast/literal 1 t/long)
          'b (ast/literal 2 t/byte)}
   :form 'a
   :spec  t/long})
 <not-computed>]
```

### `let*` > first / `symbol`

Since the first form of the `body` is a symbol, we (sensibly) enclose it in an `ast/symbol`, supplying it with the type-spec found in the `env`, `t/long` (if we didn't find the symbol in the environment, we would have thrown an error).

Notice how this first form has new bound symbols in its `env`, `a` and `b`. This is because `let*` defines a new scope and propagates its newly bound variables to its descendant AST nodes in its `body`.

An `ast/symbol` has no special keys.

### `let*` > second / `ast/static-call`

Next we compute the second form in the body, starting with what we know and macroexpanding the `form` we receive to compute `form+`:

```clojure
(. Numeric add (Numeric/bitAnd a b) c)
```

This is a static call, so we enclose it in an `ast/static-call` node, which has the following special keys:

- `:f` `(s/map-of symbol? ::ast-node)`
- `:args` - `(s/vec-of ::ast-node)`
- `:spec` - Other AST nodes use this key, but the argument that `ast/static-call` nodes' `spec` receives is the `static-call`'s type-arglist and returns the return-type-spec of the `static-call`. That is to say:
  - `(s/def ::ast/static-call:spec (s/fn {:in [(s/vec-of t/spec?)] :out t/spec?}))`

We will see examples of each below.

First, here's what we have so far for this node:

```clojure
(ast/static-call
  {:env   {'c (ast/unbound 'c)
           'a (ast/literal 1 t/long)
           'b (ast/literal 2 t/byte)}
   :form  '(Numeric/add (Numeric/bitAnd a b) c)
   :form+ '(. Numeric add (Numeric/bitAnd a b) c)
   :f     <not-computed>
   :args  <not-computed>
   :spec  <not-computed>})
```

Note that the `env` is the same as the previous `form` in the `let*`'s `body`, because it's still subject to the new scoping of the `let*`.

### `let*` > second / `static-call`: `f`

Calculating `f` is easy. It's just the fully-qualified symbol representing the static method being called: `quantum.core.Numeric/add`.

### `let*` > second / `static-call`: `args`

Now we type-analyze each of the args of the static call.

### `let*` > second / `static-call` > first / `static-call`

The first one is another static call: `(Numeric/bitAnd a b)`, so we do what we did with the other static call we encountered:

```clojure
(ast/static-call
  {:env   {'c (ast/unbound 'c)
           'a (ast/literal 1 t/long)
           'b (ast/literal 2 t/byte)}
   :form  '(Numeric/bitAnd a b)
   :form+ '(. Numeric bitAnd a b)
   :f     'quantum.core.Numeric/bitAnd
   :args  <not-computed>
   :spec  <not-computed>})
```

We then analyze the args as before.

### `let*` > second / `static-call` > first / `static-call` > first / `symbol`

Here we encounter another symbol, which we enclose in an `ast/symbol`:

```clojure
(ast/symbol
  {:env  {'c (ast/unbound 'c)
          'a (ast/literal 1 t/long)
          'b (ast/literal 2 t/byte)}
   :form 'a
   :spec t/long})
```

At this point we've reached a(nother) leaf node, so we can move up the tree and over to the next element in the enclosing `static-call`.

### `let*` > second / `static-call` > first / `static-call` > second / `symbol`

```clojure
(ast/symbol
  {:env  {'c (ast/unbound 'c)
          'a (ast/literal 1 t/long)
          'b (ast/literal 2 t/byte)}
   :form 'b
   :spec t/byte})
```

Nothing unusual here. But now comes the fun part, now that we're done with the `args`.

### `let*` > second / `static-call` > first / `static-call`: `spec`

The node we're at looks like this so far:

```clojure
(ast/static-call
  {:env   {'c (ast/unbound 'c)
           'a (ast/literal 1 t/long)
           'b (ast/literal 2 t/byte)}
   :form  '(Numeric/bitAnd a b)
   :form+ '(. Numeric bitAnd a b)
   :f     'quantum.core.Numeric/bitAnd
   :args  [(ast/symbol
             {:env  {'c (ast/unbound 'c)
                     'a (ast/literal 1 t/long)
                     'b (ast/literal 2 t/byte)}
              :form 'a
              :spec t/long})
           (ast/symbol
             {:env  {'c (ast/unbound 'c)
                     'a (ast/literal 1 t/long)
                     'b (ast/literal 2 t/byte)}
              :form 'b
              :spec t/byte})]
   :spec  <not-computed>})
```

Now we get to deal with our first reasonably complex `spec`. Recall from before that AST function call nodes, including `ast/static-call` nodes, take `spec`s whose input is a sequence of the argument type-specs and whose return value is a type-spec. Unlike specs a user declares when declaring a typed function or other such spec-able thing, these specs will be incrementally modified by the type analyzer as it finds and deduces more and more type information from the forms it encounters.

Let's start with what we know about `quantum.core.Numeric/bitAnd`. According to reflection, it has the following type overloads (note each overload is of the form `[<ret-type> <arg-types>]`):

```clojure
#{[byte  [byte  byte ]]
  [char  [byte  char ]]
  [short [byte  short]]
  [int   [byte  int  ]]
  [long  [byte  long ]]
  [char  [char  byte ]]
  [char  [char  char ]]
  [short [char  short]]
  [int   [char  int  ]]
  [long  [char  long ]]
  [short [short byte ]]
  [short [short char ]]
  [short [short short]]
  [int   [short int  ]]
  [long  [short long ]]
  [int   [int   byte ]]
  [int   [int   char ]]
  [int   [int   short]]
  [int   [int   int  ]]
  [int   [int   long ]]
  [long  [long  byte ]]
  [long  [long  char ]]
  [long  [long  short]]
  [long  [long  int  ]]
  [long  [long  long ]]}
```

To create a `spec` for a an AST function call node, we need to return a spec describing the return type-spec of the function call in question, given the type-specs of its arguments. We then arrive at the following:

```clojure
(t/spec [[t0 t1]]
  (condp t/<= t0
    t/byte  (condp t/<= t1
              t/byte  t/byte
              t/char  t/char
              t/short t/short
              t/int   t/int
              t/long  t/long)
    t/char  (condp t/<= t1
              t/byte  t/char
              t/char  t/char
              t/short t/short
              t/int   t/int
              t/long  t/long)
    t/short (condp t/<= t1
              t/byte  t/short
              t/char  t/short
              t/short t/short
              t/int   t/int
              t/long  t/long)
    t/int   (condp t/<= t1
              t/byte  t/int
              t/char  t/int
              t/short t/int
              t/int   t/int
              t/long  t/long)
    t/long  (condp t/<= t1
              t/byte  t/long
              t/char  t/long
              t/short t/long
              t/int   t/long
              t/long  t/long)))
```

(Note that there are other, more efficient ways of coding up this spec, but this version intentionally prioritizes straightforwardness over efficiency.)

This doesn't look fundamentally different from the information we got via reflection; mainly, things have just been shifted around. The conditional branches embodied by the `condp` calls encode the various argument-type combinations of `Numeric/bitAnd`, and at the leaves of the conditional statements are its possible return types.

The immediately strangest thing here is that we don't use `condp =` but rather *`condp t/<=`*. This seems unintuitive at first glance. How is a spec *less than or equal to* another spec, let alone a special kind of *`t/`* less-than-or-equal-to? The idea is that we don't always need arg specs to match a function or method's "spec" signature exactly; we just need to make sure the arg specs are *at least as specific* as what the function or method requires. This is a principle from the Design by Contract paradigm, one also featured in `clojure.spec`. For instance, the first argument to `bitAnd` could be a `t/byte`, but it could just as well be a `(t/and t/byte #(> % 3))`. In other words, the set of possible values of the arguments need only be a (lax) subset of the values the function accepts, not only an identical set.

Another seemingly strange thing is that we use `t/byte` instead of `byte`. This is because `byte` (like other classes) isn't a spec, and we need to 1) dispatch on what we're receiving as input, namely specs, not a class; and 2) return a spec, not a class.

Suppose we had the spec of each argument. If we had this information, we could use the `t/spec` we defined above to dispatch on the argument-specs to find the matching return-spec, while excluding argument-specs that would cause the function call to fail (ever try passing `nil` to `inc`?).

Let's add what we just defined to our `static-call` node:

```clojure
(ast/static-call
  {:env   {'c (ast/unbound 'c)
           'a (ast/literal 1 t/long)
           'b (ast/literal 2 t/byte)}
   :form  '(Numeric/bitAnd a b)
   :form+ '(. Numeric bitAnd a b)
   :f     'quantum.core.Numeric/bitAnd
   :args  [(ast/symbol
             {:env  {'c (ast/unbound 'c)
                     'a (ast/literal 1 t/long)
                     'b (ast/literal 2 t/byte)}
              :form 'a
              :spec t/long})
           (ast/symbol
             {:env  {'c (ast/unbound 'c)
                     'a (ast/literal 1 t/long)
                     'b (ast/literal 2 t/byte)}
              :form 'b
              :spec t/byte})]
   :spec  (t/spec [[t0 t1]]
            (condp t/<= t0
              t/byte  (condp t/<= t1
                        t/byte  t/byte
                        t/char  t/char
                        t/short t/short
                        t/int   t/int
                        t/long  t/long)
              t/char  (condp t/<= t1
                        t/byte  t/char
                        t/char  t/char
                        t/short t/short
                        t/int   t/int
                        t/long  t/long)
              t/short (condp t/<= t1
                        t/byte  t/short
                        t/char  t/short
                        t/short t/short
                        t/int   t/int
                        t/long  t/long)
              t/int   (condp t/<= t1
                        t/byte  t/int
                        t/char  t/int
                        t/short t/int
                        t/int   t/int
                        t/long  t/long)
              t/long  (condp t/<= t1
                        t/byte  t/long
                        t/char  t/long
                        t/short t/long
                        t/int   t/long
                        t/long  t/long)))})
```

(Since you can see that this and other nodes can often carry a lot of information to effectively process at a glance, we will in the future tend to abbreviate irrelevant or repetitive values.)

Now let's plug in the `spec`s of our `args` in order to reduce the `spec` of our `static-call` to the smallest possible footprint. If we call our `spec` on our vector of arg-specs `[t/long t/byte]`, we get, simply, `t/long`. This makes sense given the type overloads of `Numeric/bitAnd`, and it makes our lives easier, too! It means that we can replace our large code block up above with a simple `t/long`:

```clojure
(ast/static-call
  {:env   <...>
   :form  <...>
   :form+ <...>
   :f     <...>
   :args  <...>
   :spec  t/long})
```

As an aside, if we had had arg-specs that fell outside of the conditional branch structure we generated in our `t/spec` (e.g. calling `Numeric/bitAnd` with the wrong number of args, or with a `t/string?` as an arg, etc.), we would have thrown an error. This makes sense because we always want to notify the user as soon as possible of inevitable runtime errors in analyzed code.

Now that that's finished and we've now analyzed the `args` of the parent `static-call`, we're ready to move up the tree and on to its second form.

### `let*` > second / `static-call` > second / `symbol`

This form is a symbol, but unlike others, resolving it from the environment yields a different kind of spec than we've seen before: `t/?`, the spec denoting inference. This isn't significantly different from other specs, other than the fact that it's more "malleable"; when it is supplied with new type information in an attempt to perform "type-spec compression" of the kind we did for `Numeric/bitAnd`'s `spec`, it will simply replace the (lack of) type information it has with whatever is supplied to it. We will elucidate this below. But first, here's what our `Numeric/add` `static-call` node looks like so far:

```clojure
(ast/static-call
  {<...>
   :f    'quantum.core.Numeric/add
   :args [(ast/static-call
            {<...> :spec t/long})
          (ast/symbol
            {:env  {'c (ast/unbound 'c)
                    'a (ast/literal 1 t/long)
                    'b (ast/literal 2 t/byte)}
             :form 'c
             :spec t/?})]
   :spec <not-computed>})
```

### `let*` > second / `static-call`: `spec`

With this information, we are ready to compute the `spec` of this node. As before with the other `static-call`, we will use reflection to retrieve information about the type overloads of `quantum.core.Numeric/add`:

```clojure
#{[short  [byte   byte  ]]
  [int    [byte   char  ]]
  [int    [byte   short ]]
  [long   [byte   int   ]]
  [long   [byte   long  ]]
  [int    [char   byte  ]]
  [int    [char   char  ]]
  [int    [char   short ]]
  [long   [char   int   ]]
  [long   [char   long  ]]
  [int    [short  byte  ]]
  [int    [short  char  ]]
  [int    [short  short ]]
  [long   [short  int   ]]
  [long   [short  long  ]]
  [double [short  float ]]
  [double [short  double]]
  [long   [int    byte  ]]
  [long   [int    char  ]]
  [long   [int    short ]]
  [long   [int    int   ]]
  [long   [int    long  ]]
  [double [int    float ]]
  [double [int    double]]
  [long   [long   byte  ]]
  [long   [long   char  ]]
  [long   [long   short ]]
  [long   [long   int   ]]
  [long   [long   long  ]]
  [double [long   float ]]
  [double [long   double]]
  [double [float  byte  ]]
  [double [float  char  ]]
  [double [float  short ]]
  [double [float  int   ]]
  [double [float  long  ]]
  [double [float  float ]]
  [double [float  double]]
  [double [double byte  ]]
  [double [double char  ]]
  [double [double short ]]
  [double [double int   ]]
  [double [double long  ]]
  [double [double float ]]
  [double [double double]]}
```

Then, as before, we create the `spec` for the `static-call` node we're working on:

```clojure
(t/spec [[t0 t1]]
  (condp t/<= t0
    t/byte   (condp t/<= t1
               t/byte   t/short
               t/char   t/int
               t/short  t/int
               t/int    t/long
               t/long   t/long)
    t/char   (condp t/<= t1
               t/byte   t/int
               t/char   t/int
               t/short  t/int
               t/int    t/long
               t/long   t/long)
    t/short  (condp t/<= t1
               t/byte   t/int
               t/char   t/int
               t/short  t/int
               t/int    t/long
               t/long   t/long
               t/float  t/double
               t/double t/double)
    t/int    (condp t/<= t1
               t/byte   t/long
               t/char   t/long
               t/short  t/long
               t/int    t/long
               t/long   t/long
               t/float  t/double
               t/double t/double)
    t/long   (condp t/<= t1
               t/byte   t/long
               t/char   t/long
               t/short  t/long
               t/int    t/long
               t/long   t/long
               t/float  t/double
               t/double t/double)
    t/float  (condp t/<= t1
               t/byte   t/double
               t/char   t/double
               t/short  t/double
               t/int    t/double
               t/long   t/double
               t/float  t/double
               t/double t/double)
    t/double (condp t/<= t1
               t/byte   t/double
               t/char   t/double
               t/short  t/double
               t/int    t/double
               t/long   t/double
               t/float  t/double
               t/double t/double)))
```

(As before, note that there are other, more efficient ways of coding up this spec, but this version intentionally prioritizes straightforwardness over efficiency.)

Whew! Now for the fun part, where we get to deduce more types. Let's plug in the `spec`s of our `args` in order to reduce the `spec` of our `static-call` to the smallest possible footprint. If we call our `spec` on our vector of arg-specs `[t/long t/?]`, what happens? Madness!

... Well, almost. At least through branch pruning we can isolate the return spec to a leaf in the following conditional branch:

```clojure
(condp t/<= t1
  t/byte   t/long
  t/char   t/long
  t/short  t/long
  t/int    t/long
  t/long   t/long
  t/float  t/double
  t/double t/double)
```

But what do we do from here? We could call it good and just say "it's either a `t/long` or a `t/double`", representing it more formally as `(t/or t/long t/double)`. But still, won't `t/?` fail? Luckily, the answer is no. The `t/<` operator saves the day: `(t/<= t/? <spec>)` will always be true, since `t/?` is analogous in some sense to the empty set. So really, if we run our `t/?` spec through all branches in the `condp` above, it results in a `spec` of exactly what we intuitively arrived at: `(t/or t/long t/double)`.

Here is where we can touch on a critical feature of the type analyzer: deducing types in one AST node can result in further deductions (or a failure) in other AST nodes. Just as we used the specs of the args of `Numeric/add` to further specify its return spec, we use the deduced return spec of `Numeric/add` to further specify the specs of its arguments that need inference, namely the second argument, `c`. **The spec of `c` is the union (`t/or`) of all valid return specs of the current AST node (all reachable conditional leaves given the argument specs).** This yields `(t/or t/byte t/char t/short t/int t/long t/float t/double)`.

```clojure
(ast/static-call
  {<...>
   :f    'quantum.core.Numeric/add
   :args [(ast/static-call
            {<...> :spec t/long})
          (ast/symbol
            {<...>
             :form 'c
             :spec (t/or t/byte t/char t/short t/int t/long t/float t/double)})]
   :spec #!@ (t/or t/long t/double)})
```

### `let*`: spec

The `spec` of a `let*` is simply the `spec` of the last form in the `let*`'s `body`. In this case, it's `(t/or t/long t/double)`.

### Putting it all together

Now for the final result:

```clojure
(ast/let*
  {:env      {'c (ast/unbound 'c)}
   :form     '(let* [a 1 b (byte 2)]
                a
                (Numeric/add c (Numeric/bitAnd a b)))
   :form+    '(let* [a 1 b (byte 2)]
                a
                (Numeric/add c (Numeric/bitAnd a b)))
   :bindings {'a (ast/literal 1 t/long)
              'b (ast/literal 2 t/byte)}
   :body     [(ast/symbol
                {:env  {'c (ast/unbound 'c)
                        'a (ast/literal 1 t/long)
                        'b (ast/literal 2 t/byte)}
                 :form 'a
                 :spec  t/long})
              (ast/static-call
                {:env   {'c (ast/unbound 'c)
                         'a (ast/literal 1 t/long)
                         'b (ast/literal 2 t/byte)}
                 :form  '(Numeric/add a b)
                 :form+ '(. Numeric add a b)
                 :f    'quantum.core.Numeric/add
                 :args [(ast/static-call
                          {:env   {'c (ast/unbound 'c)
                                   'a (ast/literal 1 t/long)
                                   'b (ast/literal 2 t/byte)}
                           :form  '(Numeric/bitAnd a b)
                           :form+ '(. Numeric bitAnd a b)
                           :f     'quantum.core.Numeric/bitAnd
                           :args  [(ast/symbol
                                     {:env  {'c (ast/unbound 'c)
                                             'a (ast/literal 1 t/long)
                                             'b (ast/literal 2 t/byte)}
                                      :form 'a
                                      :spec t/long})
                                   (ast/symbol
                                     {:env  {'c (ast/unbound 'c)
                                             'a (ast/literal 1 t/long)
                                             'b (ast/literal 2 t/byte)}
                                      :form 'b
                                      :spec t/byte})]})
                        (ast/symbol
                          {:env  {'c (ast/unbound 'c)
                                  'a (ast/literal 1 t/long)
                                  'b (ast/literal 2 t/byte)}
                           :form 'c
                           :spec (t/or t/byte t/char t/short t/int t/long t/float /double)})]
                 :spec #!@ (t/or t/long t/double)})]
   :spec #!@ (t/or t/long t/double)})
```

Congratulations! You just performed some complex type analysis on a form whose type was non-obvious and came up with an interesting answer: `(t/or t/long t/double)`.
