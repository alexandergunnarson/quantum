(ns quantum.core.spec
  (:refer-clojure :exclude
    [string? keyword? set? number? fn? any?
     assert keys merge + * cat and or constantly])
  (:require
    [quantum.untyped.core.spec :as u]
    [quantum.untyped.core.vars
      :refer [defaliases]]))

(defaliases u
  verbose?
  #?@(:clj [validate-one* validate-one validate* validate
            tuple coll-of map-of
            def fdef
            keys keys* merge
            spec + * ?
            and or every
            conformer])
  conform explain
  #?@(:clj [cat cat*
            alt alt*
            fdef! or-auto])
  valid? invalid?
  #?@(:clj [or* or*-forms constantly-or
            set-of])
  validate|val? any?)
