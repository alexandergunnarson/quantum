(ns quantum.test.core.logic
  (:require
    [quantum.untyped.core.numeric.combinatorics :as combo]
    [quantum.core.logic :as ns]
    [quantum.core.test  :as test
      :refer [deftest is]]))

#?(:clj
(deftest test:some-but-not-more-than-n
  (doseq [n [1]] ; TODO test more
    (doseq [args-n (range 5)]
      (doseq [args (combo/selections #{true false} args-n)]
        (is (= (boolean (eval `(ns/some-but-not-more-than-n ~n ~@args)))
               (boolean (eval `(and (or ~@args) (not (and ~@args))))))))))))

(deftest test:default
  (let [a (atom 0)]
    (ns/default nil   (reset! a 1))
    (is (= @a 1))
    (ns/default true  (reset! a 2))
    (is (= @a 1))
    (ns/default false (reset! a 3))
    (is (= @a 1))
    (ns/default 1     (reset! a 4))
    (is (= @a 1))
    (ns/default nil   (reset! a 5))
    (is (= @a 5))))

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

(defn test:condf1 [& args])

(defn test:condf**n [& args])

(defn test:condfc
 [obj & clauses])

(defn test:ifn [obj pred true-fn false-fn])

(defn test:ifc [obj pred true-expr false-expr])

(defn test:ifp [obj pred true-fn false-fn])

(defn test:ifcf$n [pred true-expr false-expr])

(defn test:ifn1 [pred true-fn false-fn])

(defn test:whenf
  [obj pred true-fn])

(defn test:whenc
  [obj pred true-expr])

(defn test:whenp
  [obj pred true-fn])

(defn test:whenf1
  [pred true-fn])

(defn test:whenc1
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
