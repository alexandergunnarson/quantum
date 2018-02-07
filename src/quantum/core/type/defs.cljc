(ns
  ^{:doc "Definitions for types."
    :attribution "alexandergunnarson"}
  quantum.core.type.defs
  (:refer-clojure :exclude
    [boolean byte char short int long float double])
  (:require
    [quantum.untyped.core.type.defs :as u]
    [quantum.untyped.core.vars
      :refer [defaliases]]))

;; TODO rewrite and see what actually is needed/useful and what overlap there is
(defaliases u
  #?@(:clj [boolean byte char short int long float double])
  primitive-type-meta
  array-ident->primitive-sym
  elem-types-clj
  max-values max-type
  #?@(:clj [boxed-types* unboxed-types* boxed->unboxed-types-evaled promoted-types* array-1d-types* class->str])
  types|unevaled types)
