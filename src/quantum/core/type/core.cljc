(ns
  ^{:doc "Some useful macros, like de-repetitivizing protocol extensions.
          Also some plumbing macros for `for` loops and the like."
    :attribution "alexandergunnarson"}
  quantum.core.type.core
  (:refer-clojure :exclude
    [class])
  (:require
    [quantum.untyped.core.type.core :as u]
    [quantum.untyped.core.vars
      :refer [defaliases]]))

(defaliases u
  class boxed-type-map
  #?@(:clj [boxed->unboxed unboxed->boxed unboxed->convertible
            convertible? unboxed-type-map])
  prim-types            prim-types|unevaled      prim|unevaled?
  primitive-types       primitive-types|unevaled primitive|unevaled?
  primitive-boxed-types primitive-boxed-types|unevaled
  auto-unboxable|unevaled?
  most-primitive-class-of
  ;;
  primitive-array-types
  cljs-typed-array-convertible-classes
  java-array-type-regex
  #?@(:clj [nth-elem-type|clj primitive-array-type?])
  default-types type-casts-map return-types-map
  ->boxed|sym ->unboxed|sym boxed?|sym
  static-cast-code #?@(:clj [static-cast class>prim-subclasses]))
