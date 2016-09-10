quantum
==========
[![Join the conversation](https://quantum-library.herokuapp.com/badge.svg)](https://quantum-library.herokuapp.com/) [![Stack Share](http://img.shields.io/badge/tech-stack-0690fa.svg?style=flat)](http://stackshare.io/alexandergunnarson/clojure-clojurescript-datomic) [![CircleCI](https://circleci.com/gh/alexandergunnarson/quantum.svg?style=shield&circle-token=:circle-token)]()

![](http://pre03.deviantart.net/b712/th/pre/i/2012/267/e/3/bubble_chamber_by_deepbluerenegade-d5fssqg.jpg)
*(Image credit: @deviantart/deepbluerenegade, "Bubble Chamber")*

***"The career of a young theoretical physicist consists of treating the harmonic oscillator in ever-increasing levels of abstraction."*** *— Sidney Coleman*

-

In like manner, the aim of a computer scientist consists of treating the manipulation and processing of data (including procedures) in ever-increasing levels of abstraction.

In accordance with this aim, to paraphrase the [poet of xkcd](http://xkcd.com/224/):

***That syntax might fade,***

***That all might swim in the purity***

***Of quantified conception, of ideas manifest,***

***To this end, from the ancience of Lisp —***

***Time-tested, but never time-worn —***

***Was fashioned***

`quantum`.

Summary
-
Less magically put, quantum is like Google Guava for Clojure. It's an all-purpose Clojure library similar to [Prismatic/plumbing](https://github.com/Prismatic/plumbing), [weavejester/medley](https://github.com/weavejester/medley), [mikera/clojure-utils](https://github.com/mikera/clojure-utils), [ptaoussanis/encore](https://github.com/ptaoussanis/encore), [ztellman/potemkin](https://github.com/ztellman/potemkin), [zcaudate/hara](https://github.com/zcaudate/hara), and others. It aims to unify and abstract away conceptually irrelevant implementation details while providing great performance, often exceeding that of clojure.core (see benchmarks for more information).

It adapts, in the author's opinion, the best and most useful functions from existing libraries (in accordance with their respective copyrights) and adds much more of its own.

General Usage
-

**In `project.clj`:**

![](https://clojars.org/quantum/core/latest-version.svg)

**In namespaces:**

`(require 'quantum.core. ... )`
where `...` is the name of the sub-library (e.g., `quantum.core.collections`, `quantum.core.io`, etc.).

Walkthrough and Code Examples
-
This library is big enough to be split into numerous sub-libraries. Someday I'll do just that (using `lein-repack`, as zcaudate/hara, whose rationale is explained [here](http://z.caudate.me/finding-a-middle-ground/)). For now, it's an admittedly monolithic, though well-organized, library. I don't like the Clojure approach of "have one library that only does this one thing" because it often leads to an unhealthy amount of disunity and disorganization. Better to import/require the library and abstract it under a more universal name. If there are various implementations of the same thing, `quantum` abstracts it to a function which calls your desired implementation, but defaults to the most sensible one.

###quantum.core.*

The "meat" of Quantum is in [quantum.core.*](https://github.com/alexandergunnarson/quantum/tree/master/src/cljc/quantum/core). Among many other functions and namespaces, it includes the following:

####[quantum.core.macros](https://github.com/alexandergunnarson/quantum/tree/master/src/cljc/quantum/core/macros.cljc)
#####`defnt`
`defnt` is a way of defining a strongly typed function without resorting to using the un-function-like syntax of `defprotocol` and/or using the tedious `reify`. An example of it is the following:

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

####[quantum.core.thread](https://github.com/alexandergunnarson/quantum/tree/master/src/cljc/quantum/core/thread.cljc)

Sane global thread(pool) management. I was tired of core.async/go blocks hanging and me not being able to interrupt/cancel them. Also I was tired of having them all run on one threadpool.

####[quantum.core.convert](https://github.com/alexandergunnarson/quantum/tree/master/src/cljc/quantum/core/convert.cljc)

Easy type conversion (e.g. among InputStream, ByteBuffer, CharSequence, String, File etc.). Much of the code is adapted from [ztellman/byte-streams](https://github.com/ztellman/byte-streams) into a [`defnt`](https://github.com/alexandergunnarson/quantum/tree/master/src/cljc/quantum/core/macros.cljc) context (as opposed to the memoized graph-walking of [`byte-streams/convert`](https://github.com/ztellman/byte-streams/blob/master/src/byte_streams.clj), which is quite admittedly quite innovative, as is characteristic of Zach Tellman). Other conversions have been added to make it more universal and less restricted to variations of byte streams and buffers.

####[quantum.core.cryptography](https://github.com/alexandergunnarson/quantum/tree/master/src/cljc/quantum/core/cryptography.cljc)

Abstractions for cryptography functions including bcrypt, scrypt, etc., and also so you don't have to remember every time how those annoying javax.crypto.* namespaces work. Similar rationale as [ztellman/byte-streams](https://github.com/ztellman/byte-streams) — a cryptographic Rosetta Stone of sorts.

####[quantum.core.reducers](https://github.com/alexandergunnarson/quantum/tree/master/src/cljc/quantum/core/reducers.cljc)

Reducers operations in quantum.core.reducers (exposed via quantum.core.collections) so you can do chains of operations like:
```
(->> [1 2 [1 1] 5 [6 7 [1 1]]]
     (filter+ vector?)
     flatten-1+
     frequencies+
     (into []))
```
without creating intermediate sequences or incurring the cost of laziness. This has already been accomplished in [clojure.core.reducers](http://clojure.org/reducers), so it's not a new idea, but it does add various reducer functions to the existing clojure.core.reducers ones.

####[quantum.core.log](https://github.com/alexandergunnarson/quantum/tree/master/src/cljc/quantum/core/log.cljc)
Easy logging which uses macros + conditionals so you don't incur the cost of always performing whatever the arguments are to your logging function even if you're not enabling that logging level.

###The rest of Quantum

####[quantum.compile.core](https://github.com/alexandergunnarson/quantum/tree/master/src/cljc/quantum/compile/core.cljc)
#####`transpile`
(Alpha-quality) language translation from e.g. Clojure to Java, Java to Clojure, Clojure to C#, Java to C# by way of Clojure, Clojure to (raw) JavaScript, etc. You might use it when you want to write something out and test it in Clojure but need to deliver the code in a different language.

####[quantum.measure.convert](https://github.com/alexandergunnarson/quantum/tree/master/src/cljc/quantum/measure/convert.cljc)
#####`convert`
Conversion from any unit of measurement to any other (compatible) unit of measurement (at least the ones I've added so far). It does this by walking a graph at compile time (like the one in src/cljc/quantum/measure/length.cljc) and replacing the conversion inline if sufficiently short. For instance, one can write (convert :parsecs :ft) instead of having to remember what the parsecs-to-feet conversion is. I believe I've also added runtime support for this so you can dynamically change the unit keywords.

####[quantum.ui.*](https://github.com/alexandergunnarson/quantum/tree/master/src/cljc/quantum/ui/)
UI-related things, specifically HTML5 and JavaFX so far. It needs more work, but it's served me well fo  e.g. revision management in JavaFX (via the undo! and redo! functions especially), as well as for easier creation of JavaFX nodes via a declarative [Hiccup](https://github.com/weavejester/hiccup/)-like syntax as opposed to the standard procedural one.

Performance
-

#####Reducers
It uses clojure.reducers wherever possible to maximize performance, but falls back on clojure.core collections functions when laziness is required or desired, or when the overhead of creating an anonymous function within `reduce` is greater than the overhead eliminated by using `reduce` instead of [`first` and [`next` or `rest`] within `loop`/`recur`].

#####Transients and Mutable Locals
It uses transients and/or mutable local variables wherever a performance boost can be achieved by such.

Expectations
-

This is a work in progress and, as such, is currently in no way as rich and polished a library as I hope it to ultimately be. When you find bugs (and you will), please report the issue(s) and/or create a pull request.

I've been coding in Clojure for over two years, but expect some rustiness. Expect some gems as well.

I welcome any and all contributions, comments, thoughts, suggestions, and/or feedback you may wish to provide. This endeavor towards greater efficiency in thought, processing power, and time spent programming should be a joint one.

Appendix
-

###Why is `quantum` a monorepo?

*TL;DR:*
It's *much* easier and has few (if any) real disadvantages.

*Appeal to authority:*
React, Meteor, and Ember follow this pattern.

*Pithy quote:*
["Juggling a multimodule project over multiple repos is like trying to teach a newborn baby how to ride a bike."](https://github.com/babel/babel/blob/master/doc/design/monorepo.md)

See [here](https://github.com/babel/babel/blob/master/doc/design/monorepo.md) and [here](http://danluu.com/monorepo/) for a more complete justification.

###Good practices for dependency hell

- When two dependencies A and B have a conflicting common dependency C, try to explicitly declare C and its version in the project.clj and annotate the conflict.

Copyright and License
-
*Copyright © 2015 Alex Gunnarson*

*Distributed under the Creative Commons Attribution-ShareAlike 3.0 US (CC-SA) license.*

**For normal people who don't speak legalese, this means:**

* You **can** modify the code
* You **can** distribute the code
* You **can** use the code for commercial purposes

But:

* You **have to** give credit / attribute the code to the author (Alex Gunnarson)
* You **have to** state the name of the author (Alex Gunnarson) and the title of this project in the attribution
* You **have to** say in the attribution that you modified the code if you did

Pretty easy, common-sense, decent stuff! Thanks :)

*For more information, see [tldrlegal's summary](https://tldrlegal.com/license/creative-commons-attribution-share-alike-(cc-sa)) of the CC-SA license.*
