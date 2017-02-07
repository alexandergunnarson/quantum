(ns
  ^{:doc "Logic-related functions. nnil?, nempty?, fn-not, fn-and, splice-or,
          ifn, whenf1, rcomp, fn->, condpc, and the like. Extremely useful
          and used everywhere in the quantum library."
    :attribution "Alex Gunnarson"}
  quantum.core.logic
  (:refer-clojure :exclude
    [if-let when-let])
  (:require
    [clojure.core             :as core]
    [quantum.core.fn          :as fn
      :refer [fn1 fn->]]
    [quantum.core.vars        :as var
      :refer [defalias]]
    [quantum.core.macros.core :as cmacros
      :refer [if-cljs]])
  (:require-macros
    [quantum.core.logic       :as self
      :refer [fn-not]]))

; TODO: ; cond-not, for :pre
; |Switch| is implemented using an array and then points to the code.
; String |switch| is implemented using a map8

;___________________________________________________________________________________________________________________________________
;==================================================={ BOOLEANS + CONDITIONALS }=====================================================
;==================================================={                         }=====================================================
#?(:clj (defmacro default [v else] `(let [v# ~v] (if (nil? v#) ~else v#))))

; =    tests value-equivalence
; ref= tests identity-equivalence
(defalias ref= identical?)

#?(:clj
(defmacro xor
  {:attribution 'alexandergunnarson}
  ([] nil)
  ([x] false)
  ([x y] (if x (not y) y))
  ([x y & next]
    `(if ~x (when-not (and ~y ~@next) ~x) (xor ~y ~@next)))))

#?(:clj (defmacro nand     [& args] `(not (and ~@args))))
#?(:clj (defmacro nor      [& args] `(not (or  ~@args))))
#?(:clj (defmacro implies? [a b] `(if ~a ~b true)))

#?(:clj
(defmacro fn-logic-base
  [oper & preds]
  (let [arg (gensym "arg")]
   `(fn [~arg] (~oper ~@(for [pred preds] `(~pred ~arg)))))))

#?(:clj (defmacro fn-not      [pred]    `(fn-logic-base not  ~pred  )))
#?(:clj (defmacro fn-or       [& preds] `(fn-logic-base or   ~@preds)))
#?(:clj (defmacro fn-nor      [& preds] `(fn-logic-base nor  ~@preds)))
#?(:clj (defmacro fn-xor      [& preds] `(fn-logic-base xor  ~@preds)))
#?(:clj (defmacro fn-and      [& preds] `(fn-logic-base and  ~@preds)))
#?(:clj (defmacro fn-nand     [& preds] `(fn-logic-base nand ~@preds)))
#?(:clj (defmacro fn-implies? [a b]     `(fn-logic-base implies? ~a ~b)))

(defn fn=     [x] (fn [y] (=    x y)))
(defn fn-not= [x] (fn [y] (not= x y)))

(def falsey? (some-fn false? nil?))
(def truthy? (fn-not falsey?))

(defn splice-or  [obj compare-fn & coll]
  (some   #_seq-or  (partial compare-fn obj) coll))
(defn splice-and [obj compare-fn & coll]
  (every? #_seq-and (partial compare-fn obj) coll))

#?(:clj
(defmacro coll-base [logical-oper & elems]
  (let [bin-pred (gensym)
        obj      (gensym)]
   `(fn [~bin-pred ~obj]
      (~logical-oper
        ~@(for [elem elems]
            `(~bin-pred ~obj ~elem)))))))

#?(:clj
(defmacro coll-or [& elems] `(coll-base or ~@elems)))

#?(:clj
(defmacro coll-and
  {:usage "((coll-and 1 2 3) < 0) => true (0 is less than 1, 2, and 3)"}
  [& elems] `(coll-base and ~@elems)))

(defn bool
  {:todo ["Deprecate or incorporate"]}
  [v]
  (cond
    (= v 0) false
    (= v 1) true
    :else
      (throw (#?(:clj  IllegalArgumentException.
                 :cljs js/Error.)
               (str "Value not booleanizable: " v)))))

#?(:clj
(defmacro condf
  "Like |cond|, with each expr as a function applied to the initial argument, @obj."
  {:attribution "Alex Gunnarson"}
  [obj & clauses]
  (let [gobj (gensym "obj__")
        illegal-argument (if-cljs &env 'js/Error. 'IllegalArgumentException.)
        emit (fn emit [obj args]
               (let [[[a b c :as clause] more]
                       (split-at 2 args)
                     n (count clause)]
                 (cond
                   (= 0 n) `(throw (~illegal-argument
                                     (str "No matching clause for " ~obj)))
                   (= 1 n) `(~a ~obj)
                   (= 2 n) `(if (~a ~obj)
                                (~b ~obj)
                                ~(emit obj more))
                   :else   (emit obj more))))]
  `(let [~gobj ~obj]
       ~(emit gobj clauses)))))

#?(:clj (defmacro condf1 [& args] `(fn [  arg# ] (condf arg#  ~@args))))
#?(:clj (defmacro condf& [& args] `(fn [& args#] (condf args# ~@args))))

#?(:clj
(defmacro condfc
  "Like |condf|, but each expr is essentially wrapped in a |constantly|."
  [obj & clauses]
  (let [gobj (gensym "obj__")
        illegal-argument (if-cljs &env 'js/Error. 'IllegalArgumentException.)
        emit (fn emit [obj args]
               (let [[[a b c :as clause] more]
                       (split-at 2 args)
                     n (count clause)]
                 (cond
                   (= 0 n) `(throw (~illegal-argument (str "No matching clause for " ~obj)))
                   (= 1 n) `(~a ~obj)
                   (= 2 n) `(if (or ~(= a :else)
                                    (~a ~obj))
                                ~b ; As in, this expression is not used as a function taking @obj as an argument
                                ~(emit obj more))
                   :else   (emit obj more))))]
  `(let [~gobj ~obj]
       ~(emit gobj clauses)))))

; TODO compress this?

#?(:clj (defmacro ifn   [x pred tf ff] `(let [x# ~x] (if (~pred x#) (~tf x#) (~ff x#)))))
#?(:clj (defmacro ifc   [x pred t  f ] `(let [x# ~x] (if (~pred x#)  ~t       ~f     ))))
#?(:clj (defmacro ifp   [x pred tf ff] `(let [x# ~x] (if  ~pred     (~tf x#) (~ff x#)))))

#?(:clj (defmacro ifn-> [x pred tf ff] `(let [x# ~x] (if (-> x# ~pred) (-> x# ~tf) (-> x# ~ff)))))
#?(:clj (defmacro ifc-> [x pred t  f ] `(let [x# ~x] (if (-> x# ~pred)  ~t       ~f     ))))
#?(:clj (defmacro ifp-> [x pred tf ff] `(let [x# ~x] (if  ~pred        (-> x# ~tf) (-> x# ~ff)))))

#?(:clj (defmacro ifn1 [x0 x1 x2] `(fn [arg#] (ifn arg# ~x0 ~x1 ~x2))))
#?(:clj (defmacro ifp1 [x0 x1 x2] `(fn [arg#] (ifp arg# ~x0 ~x1 ~x2))))
#?(:clj (defmacro ifc1 [x0 x1 x2] `(fn [arg#] (ifc arg# ~x0 ~x1 ~x2))))

#?(:clj
(defmacro whenf
  "Analogous to `ifn`.
   (whenf 1 nnil? inc)` = `(ifn 1 nnil? inc identity)`
   `whenf` : `identity` :: `when` : `nil`"
  [x pred tf] `(let [x# ~x] (if (~pred x#) (~tf x#) x#))))

#?(:clj
(defmacro whenf->
  "Analogous to `ifn->`.
   `(whenf-> 1 nnil? inc)` = `(ifn-> 1 nnil? inc identity)`
   `whenf->` : `identity` :: `when` : `nil`"
  [x pred texpr] `(let [x# ~x] (if (-> x# ~pred) (-> x# ~texpr) x#))))

#?(:clj
(defmacro whenc
  "`whenf` + `ifc`" [x pred texpr] `(let [x# ~x] (if (~pred x#) ~texpr x#))))

#?(:clj
(defmacro whenc->
  "`whenf->` + `ifc->`" [x pred-expr texpr] `(let [x# ~x] (if (-> x# ~pred-expr) ~texpr x#))))

#?(:clj
(defmacro whenp
  "`whenf` + `ifp`" [x pred tf] `(let [x# ~x] (if ~pred (~tf x#) x#))))

(defmacro whenp->
  "`whenf->` + `ifp->`" [x pred texpr] `(let [x# ~x] (if ~pred (-> x# ~texpr) x#)))

#?(:clj (defmacro whenf1 [x0 x1] `(fn [arg#] (whenf arg# ~x0 ~x1))))
#?(:clj (defmacro whenc1 [x0 x1] `(fn [arg#] (whenc arg# ~x0 ~x1))))
#?(:clj (defmacro whenp1 [x0 x1] `(fn [arg#] (whenp arg# ~x0 ~x1))))

(def is? #(%1 %2)) ; for use with condp

#?(:clj
(defmacro condpc
  "/condp/ for colls."
  {:usage "(condpc = 1 (coll-or 2 3) (println '2 or 3!')"}
  [pred expr & clauses]
  (let [gpred (gensym "pred__")
        gexpr (gensym "expr__")
        emit (fn emit [pred expr args]
               (let [[[a b c :as clause] more]
                       (split-at (if (= :>> (second args)) 3 2) args)
                       n (count clause)]
                 (cond
                   (= 0 n) nil ; No matching clause `(throw (IllegalArgumentException. (str "No matching clause: " ~expr)))
                   (= 1 n) a
                   (= 2 n) `(if (if (fn? ~a)
                                    (~a ~pred ~expr)
                                    (~pred ~expr ~a))
                                ~b
                                ~(emit pred expr more))
                   :else `(clojure.core/if-let [p# (~pred ~a ~expr)]
                            (~c p#)
                            ~(emit pred expr more)))))]
  `(let [~gpred ~pred
         ~gexpr ~expr]
       ~(emit gpred gexpr clauses)))))

; ======== CONDITIONAL LET BINDINGS ========

#?(:clj
(defmacro if-let
 "An alternative to if-let where more bindings can be added"
 {:adapted-from "https://github.com/zcaudate/hara/blob/master/candidates/src/control.clj"}
  ([bindings then]
    `(if-let ~bindings ~then nil))
  ([[bnd expr & more] then else]
    `(let [temp# ~expr
           ~bnd  temp#]
       (if temp#
           ~(if more
               `(if-let [~@more] ~then ~else)
               then)
           ~else)))))

#?(:clj
(defmacro when-let
 "An alternative to when-let where more bindings can be added"
 {:attribution "Alex Gunnarson"}
  ([[bnd expr & more] & body]
    `(let [temp# ~expr
           ~bnd  temp#]
       (when temp#
         ~(if more
              `(when-let [~@more] ~@body)
              `(do ~@body)))))))

#?(:clj
(defmacro cond-let
  "Transforms into a series of nested `if-let` statements."
  {:attribution "Alex Gunnarson"}
  ([] nil) ; no else
  ([else] else)
  ([bindings then & more]
   `(if-let ~bindings
      ~then
      (cond-let ~@more)))))
