(ns quantum.core.data.tuple
  #?(:cljs
  (:require-macros
    [quantum.core.data.tuple :as self])))

; TODO (maybe): equality, etc.
; TODO CLJS
; Could implement tuples with `[v0, v1 ... vn]` fields, but then to access them via `nth`, you'd have to do `case`, which is a lookup anyway
; Plus you'd have to create `n` classes

; The point of this is that the field is immutable (though the backing array isn't — TODO fix?)
#?(:clj  (deftype Tuple [^"[Ljava.lang.Object;" vs])
   :cljs (deftype Tuple [                       vs])) ; hint as array

#?(:clj
(defmacro tuple [& vs] ; TODO CLJS
  `(Tuple. (quantum.core.data.Array/new1dObjectArray ~@vs))))
