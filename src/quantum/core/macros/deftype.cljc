(ns quantum.core.macros.deftype
  (:refer-clojure :exclude [deftype])
  (:require
    [quantum.core.vars
      :refer [defaliases]]
    [quantum.untyped.core.form.generate.deftype :as u]))

(defaliases u
  ?Associative
  ?Collection
  ?Comparable
  ?Counted
  ?Deref
  ?Fn
  ?HashEq
  ?Indexed
  ?Iterable
  ?Lookup
  ?Map
  ?MutableMap
  ?Object
  ?Record
  ?Reset
  ?Reversible
  ?Seq
  ?Seqable
  ?Sequential
  ?Stack
  ?Swap
  ;;
  #?(:clj deftype))
