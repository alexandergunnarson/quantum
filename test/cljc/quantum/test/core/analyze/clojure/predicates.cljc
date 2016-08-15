(ns quantum.test.core.analyze.clojure.predicates
  (:require [quantum.core.analyze.clojure.predicates :as ns]))

(defn test:safe-mapcat
  [f & colls])

(defn test:str-index-of [x sub])

(defn test:str-ends-with? [x sub])

; SYMBOLS

(defn test:name [x])

(defn test:type-hint [x])

(defn test:symbol-eq? [s1 s2])

(defn test:metaclass [sym])


(defn test:qualified?   [sym])
(defn test:auto-genned? [sym])
(defn test:possible-type-predicate? [x])
(defn test:hinted-literal?          [x])

;  ===== SCOPE =====

(defn test:shadows-var? [bindings v])

(defn test:new-scope? [x])

; ===== ARGLISTS =====

(defn test:first-variadic? [x])

(defn test:variadic-arglist? [x])

(defn test:arity-type [arglist])

(defn test:arglist-arity [x])

; ===== FORMS =====

(defn test:form-and-begins-with? [sym])

(defn test:form-and-begins-with-any? [set-n])

(defn test:else-pred?         [x])

(defn test:str-expression?    [x])

(defn test:string-concatable? [x])

; ===== STATEMENTS =====

(defn test:sym-call? [x])

(defn test:primitive-cast? [x])

(defn test:type-cast? [obj lang])

(defn test:constructor? [x])

(defn test:return-statement?   [x])
(defn test:defn-statement?     [x])
(defn test:fn-statement?       [x])
(defn test:function-statement? [x])
(defn test:scope?              [x])
(defn test:let-statement?      [x])
(defn test:do-statement?       [x])
(defn test:if-statement?       [x])
(defn test:cond-statement?     [x])
(defn test:when-statement?     [x])
(defn test:throw-statement?    [x])

; CONDITIONAL (AND TRY) BRANCHES

(defn test:branching-expr?        [x])
(defn test:one-branched?          [x])
(defn test:two-branched?          [x])
(defn test:many-branched?         [x])
(defn test:conditional-statement? [x])
(defn test:cond-foldable?         [x])

#?(:clj
(defn- test:find-tail-ops
  [tree]))

#?(:clj
(defn test:tail-recursive?
  [expr]))

(defn test:private?
  ([var])
  ([var m]))

(defn test:macro?
  ([var])
  ([var m]))

(defn test:constant?
  ([var])
  ([var m]))

#?(:clj
(defn test:dynamic?
  ([var])
  ([var m])))