(ns quantum.test.core.logic
  (:require [quantum.core.logic :as ns]))

;___________________________________________________________________________________________________________________________________
;==================================================={ BOOLEANS + CONDITIONALS }=====================================================
;==================================================={                         }=====================================================
(defn test:nnil?   [x])
(defn test:nempty? [x])
(defn test:nseq?   [x])

(defn test:iff  [pred const else])
(defn test:iffn [pred const else-fn])

(defn test:eq?  [x])
 
(defn test:neq? [x])

(defn test:any? [pred args])

(defn test:every? [pred args])

(defn test:apply-and [arg-list])

(defn test:apply-or  [arg-list])

(defn test:dor [& args])

(defn test:fn-logic-base
  [oper & preds])

(defn test:fn-or  [& preds])
(defn test:fn-and [& preds])
(defn test:fn-not [pred]   )

(defn test:falsey? [x])
(defn test:truthy? [x])

(defn test:splice-or  [obj compare-fn & coll])
(defn test:splice-and [obj compare-fn & coll])

(defn test:coll-base [logical-oper & elems])

(defn test:coll-or [& elems])

(defn test:coll-and [& elems])

(defn test:bool [v])

(defn test:rcompare [x y])

(defn test:condf
  [obj & clauses])

(defn test:condf*n [& args])

(defn test:condf**n [& args])

(defn test:condfc
 [obj & clauses])

(defn test:ifn [obj pred true-fn false-fn])

(defn test:ifc [obj pred true-expr false-expr])

(defn test:ifp [obj pred true-fn false-fn])

(defn test:ifcf*n [pred true-expr false-expr])

(defn test:if*n [pred true-fn false-fn])

(defn test:whenf
  [obj pred true-fn])

(defn test:whenc
  [obj pred true-expr])

(defn test:whenp
  [obj pred true-fn])

(defn test:whenf*n 
  [pred true-fn])

(defn test:whencf*n 
  [pred true-obj])

(defn test:condpc
  [pred expr & clauses])

; ======== CONDITIONAL LET BINDINGS ========

(defn test:if-let
  ([bindings then])
  ([[bnd expr & more] then else]))

(defn test:when-let
  ([[var- expr & more] & body]))

(defn test:cond-let
  [bindings & clauses])