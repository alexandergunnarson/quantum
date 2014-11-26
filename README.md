quantum
==========
![](resources/images/quantum.jpg)
*(Image credit: @deviantart/deepbluerenegade, "Bubble Chamber")*

***"The career of a young theoretical physicist consists of treating the harmonic oscillator in ever-increasing levels of abstraction."*** *— Sidney Coleman*

-

In like manner, the aim of a computer scientist consists of treating the manipulation and processing of data in ever-increasing levels of abstraction.

To this end, **quantum** was fashioned from the ancience of Lisp: time-tested, but never time-worn.

Summary
-
Less magically put, this is an all-purpose Clojure library similar to [Prismatic/plumbing](https://github.com/Prismatic/plumbing), [weavejester/medley](https://github.com/weavejester/medley), [mikera/clojure-utils](https://github.com/mikera/clojure-utils), [ptaoussanis/encore](https://github.com/ptaoussanis/encore), [ztellman/potemkin](https://github.com/ztellman/potemkin), and others. It aims to unify and abstract away conceptually irrelevant implementation details while providing great performance, often exceeding that of clojure.core (see benchmarks for more information).

It adapts the best and most useful functions from existing libraries and adds much more of its own.

Performance
-

#####Reducers
It uses clojure.reducers wherever possible to maximize performance, but falls back on clojure.core collections functions when laziness is required or desired, or when the overhead of creating an anonymous function within `reduce` is greater than the overhead eliminated by using `reduce` instead of [`first` and [`next` or `rest`] within `loop`/`recur`].

#####Transients and Mutable Locals
It uses transients and/or mutable local variables wherever a performance boost can be achieved by such.

Expectations
-

Expect a great deal of rustiness (I am one person, after all, and relatively quite inexperienced), but expect some gems as well.

I welcome any and all contributions, comments, thoughts, suggestions, and/or feedback you may wish to provide.

Copyright and License
-
*Copyright © 2014 Alexander Gunnarson*

*Distributed under the Creative Commons Attribution-ShareAlike 3.0 US (CC-SA) license.*

**For normal people who don't speak legalese, this means:**

* You **can** modify the code
* You **can** distribute the code
* You **can** use the code for commercial purposes

But:

* You **have to** give credit / attribute the code to me
* You **have to** state my name and the title of this project in the attribution
* You **have to** say in the attribution that you modified the code if you did

Pretty easy, common-sense, decent stuff! Thanks :)
