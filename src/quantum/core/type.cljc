(ns quantum.core.type
  "This is this the namespace upon which all other fully-typed namespaces rest."
  (:refer-clojure :exclude
    [* and any? fn isa? or seq? symbol? var?])
  (:require
    [quantum.untyped.core.type.defnt :as udefnt]
    [quantum.untyped.core.type       :as ut]
    ;; TODO TYPED prefer e.g. `deft-alias`
    [quantum.untyped.core.vars
      :refer [defaliases]]))

(defalias udefnt/fnt)
(defalias udefnt/defnt)

(defaliases ut
  ;; Generators
  ? * isa? fn
  ;; Combinators
  and or
  ;; Predicates
  any?
  +map?
  metable?
  seq?
  symbol?
  var?
  with-metable?)
