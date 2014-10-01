(ns quanta.library.logic
  (:refer-clojure :exclude [some?])
  (:require
    [quanta.library.ns       :as ns :refer [defalias]]
    [quanta.library.function :as fn :refer :all])
  (:gen-class))
(set! *warn-on-reflection* true)

; TODO: ; cond-not, for :pre

;___________________________________________________________________________________________________________________________________
;==================================================={ BOOLEANS + CONDITIONALS }=====================================================
;==================================================={                         }=====================================================
(def  nnil?   (comp not nil?))   ; same as /seq/ - nil punning
(def  nempty? (comp not empty?))
(def  nseq?   (comp not seq?))
(defn iff  [pred const else]
  (if (pred const) const else))
(defn iffn [pred const else-fn]
  (if (pred const)
      const
      (else-fn const)))
(def eq?  (unary =))
(def neq? (unary not=))
(defn- some? [pred coll] (boolean (some pred coll)))
(defn apply-and [arg-list]
  (every? identity arg-list))
(defn apply-or  [arg-list]
  (some?  identity arg-list))
(defn dor [& args]
  (and (apply-or args)
       (not (apply-and args))))
(defn pred-or   [pred obj args]
  (apply-or  (map (pred obj) args)))
(defn pred-and  [pred obj args]
  (apply-and (map (pred obj) args)))
(defalias fn-and every-pred)
(defalias fn-or  some-fn)
(defalias fn-not complement)
(defn splice-or  [obj compare-fn & coll]
  (some?  (partial compare-fn obj) coll))
(defn splice-and [obj compare-fn & coll]
  (every? (partial compare-fn obj) coll))
(defn fn-pred-or  [pred-fn args]
  (apply fn-or  (map pred-fn args)))
(defn fn-pred-and [pred-fn args]
  (apply fn-and (map pred-fn args)))
(defn coll-or [& elems]
  (fn [bin-pred obj]
    ((fn-pred-or (unary bin-pred) elems) obj)))
(defn coll-and
  "USAGE: ((and-coll 1 2 3) < 0) => true (0 is less than 1, 2, and 3)"
  [& elems]
  (fn [bin-pred obj]
    ((fn-pred-and (unary bin-pred) elems) obj)))
(def  empty+? (fn-or nseq? empty?))
(defn rcompare "Reverse comparator." ^{:attribution "taoensso.encore"} [x y] (compare y x))
(defn condf [obj & exprs] ; force it to delay like cond
  (loop [[pred func :as exprs-n] exprs]
    (if (or (= pred :else) (pred obj) (empty? exprs-n))
        (func obj)
        (recur (-> exprs-n rest rest)))))
(def  condf*n  (partial f*n  condf))
(def  condf**n (partial f**n condf))
(defmacro ifn [obj pred true-fn false-fn] ; macro to delay
  `(let [obj-f# ~obj] 
     (if (~pred obj-f#) (~true-fn obj-f#) (~false-fn obj-f#))
     ;`(if (~pred ~obj) (~true-fn ~obj) (~false-fn ~obj)) ; don't ask me why this is less optimal
     ))
(defmacro ifc [obj pred true-expr false-expr] ; macro to delay
  `(let [obj-f# ~obj] 
     (if (~pred obj-f#) ~true-expr ~false-expr)
     ;`(if (~pred ~obj) (~true-fn ~obj) (~false-fn ~obj)) ; don't ask me why this is less optimal
     ))
(defmacro if*n [pred true-fn false-fn]
  `(fn [arg#] (ifn arg# ~pred ~true-fn ~false-fn)))
(defmacro whenf
  "Analogous to ifn.
   (whenf 1 nnil? inc) = (ifn 1 nnil? inc identity)
   whenf : identity :: when : nil"
  [obj pred true-fn]
  `(let [obj-f# ~obj] 
     (if (~pred obj-f#) (~true-fn obj-f#) obj-f#)))
(defmacro whenc
  "Analogous to whenf, but evaluates the result instead of
   using it as a function."
  [obj pred true-obj]
  `(let [obj-f# ~obj] 
     (if (~pred obj-f#) ~true-obj obj-f#)))
(defmacro whenf*n 
  "Analogous to if*n.
   (whenf*n nnil? inc) = (if*n nnil? inc identity)"
  [pred true-fn] `(fn [arg#] (whenf arg# ~pred ~true-fn)))
(defmacro whencf*n 
  "Analogous to whenf*n."
  [pred true-obj] `(fn [arg#] (whenc arg# ~pred ~true-obj)))


(def is? #(%1 %2)) ; for use with condp

; (defn condpc ; force it to delay like cond
;   "/condp/ for colls."
;   [pred const & exprs]
;   (loop [[pred-coll expr :as exprs-n] exprs]
;       (if (or (= pred-coll :else)
;               (ifn pred-coll fn?
;                 (*fn pred const)
;                 (partial pred const))
;               (empty? exprs-n))
;           expr
;           (recur (-> exprs-n rest rest)))))
(defmacro condpc
  "/condp/ for colls. (condpc = 1 (coll-or 2 3) (println '2 or 3!')"
  [pred expr & clauses]
  (let [gpred (gensym "pred__")
        gexpr (gensym "expr__")
        emit (fn emit [pred expr args]
               (let [[[a b c :as clause] more]
                       (split-at (if (= :>> (second args)) 3 2) args)
                       n (count clause)]
                 (cond
                   (= 0 n) `(throw (IllegalArgumentException. (str "No matching clause: " ~expr)))
                   (= 1 n) a
                   (= 2 n) `(if (if (fn? ~a)
                                    (~a ~pred ~expr)
                                    (~pred ~expr ~a))
                                ~b
                                ~(emit pred expr more))



                   :else `(if-let [p# (~pred ~a ~expr)]
                            (~c p#)
                            ~(emit pred expr more)))))
        gres (gensym "res__")]
  `(let [~gpred ~pred
         ~gexpr ~expr]
       ~(emit gpred gexpr clauses))))


; (defmacro ^{:private true} if-lets*
;    {:attribution "thebusby.bagotricks"}
;   [bindings then else]
;   (let [form (subvec bindings 0 2)
;         more (subvec bindings 2)]
;     (if (empty? more)
;       `(if-let ~form
;          ~then
;          ~else)
;       `(if-let ~form
;          (if-lets* ~more ~then ~else)
;          ~else))))

; (defmacro if-lets
  ;    {:attribution "thebusby.bagotricks"}
;   "Like if-let, but accepts multiple bindings and evaluates them sequentally.
;    binding evaluation halts on first falsey value, and 'else' clause activates."
;   ([bindings then]
;      `(if-lets ~bindings ~then nil))
;   ([bindings then else]
;      (cond
;       (not (even? (count bindings))) (throw (IllegalArgumentException. "if-lets requires an even number of bindings"))
;       (not (vector? bindings))       (throw (IllegalArgumentException. "if-lets requires a vector for its binding"))
;       :else `(if-lets* ~bindings ~then ~else))))