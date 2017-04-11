# type

## Why give so many concrete typedefs in CLJS instead of abstract ones?

The reason is because in CLJS you can't extend protocols to other protocols.

https://www.paren.com/posts/isomorphic-clojure-part-2-portable-code:
> In ClojureScript, we can only extend to concrete types because ClojureScript lacks interfaces and protocols cannot be extended to other protocols.

Case in point:

```clojure
(defprotocol Abcde (abcde [a0])) ; Fine
(defprotocol Fghij (fghij [a0])) ; Fine

(extend-protocol Fghij ; Error
  Abcde
  (abcde [a0]))

; Clojure

"java.lang.IllegalArgumentException: Unable to resolve classname: Abcde"

; ClojureScript

"WARNING: Bad method signature in protocol implementation, Fghij does not declare method called abcde at line 1"

```
