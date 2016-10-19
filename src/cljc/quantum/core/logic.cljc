(ns
  ^{:doc "Logic-related functions. nnil?, nempty?, fn-not, fn-and, splice-or,
          ifn, whenf1, compr, fn->, condpc, and the like. Extremely useful
          and used everywhere in the quantum library."
    :attribution "Alex Gunnarson"
    :figwheel-no-load true
    :cljs-self-referring? true}
  quantum.core.logic
           (:refer-clojure :exclude [if-let when-let every? some some?])
           (:require [#?(:clj  clojure.core
                         :cljs cljs.core   )   :as core   ]
                     [quantum.core.fn          :as fn
                       :refer        [#?@(:clj [fn1 fn->])]
                       :refer-macros [          fn1 fn->]]
                     [quantum.core.vars        :as var
                       :refer        [#?(:clj defalias)]
                       :refer-macros [        defalias]]
                     [quantum.core.macros.core :as cmacros
                       :refer        [#?(:clj if-cljs)]
                       :refer-macros [        if-cljs]])
  #?(:cljs (:require-macros
                     [quantum.core.logic       :as logic
                       :refer [fn-or]])))

; TODO: ; cond-not, for :pre
; |Switch| is implemented using an array and then points to the code.
; String |switch| is implemented using a map8

;___________________________________________________________________________________________________________________________________
;==================================================={ BOOLEANS + CONDITIONALS }=====================================================
;==================================================={                         }=====================================================
(def  nnil?   core/some?)
(def  nempty? (comp not empty?))
(def  nseq?   (comp not seq?))

(defn iff  [pred const else]
  (if (pred const) const else))
(defn iffn [pred const else-fn]
  (if (pred const)
      const
      (else-fn const)))
; Otherwise ExceptionInInitializerError if not macro
#?(:clj (defmacro eq?  [x] `(fn-> (=    ~x))))

#?(:clj (defalias fn=     eq?))
#?(:clj (defalias fn-eq?  eq?))

#?(:clj (defmacro neq? [x] `(fn-> (not= ~x))))
#?(:clj (defalias fn-neq?  neq?))

(defn some
  "A faster version of |some| using |reduce| instead of |seq|.
   Also adds an overload whereby it is equivalent to |some?| in a
   1-arity context, but |some| in a two-arity context."
  ([x] (core/some? x))
  ([pred args]
    (reduce (fn [_ arg] (and (pred arg) (reduced true ))) nil args)))

; (defalias any? some) ; Sadly, already (pointlessly) taken in 1.9.
(defalias some? some) ; Yes, overrides, but has an extra arity that can make sense
; (defalias exists? some) ; as in ∃ ; Sadly, already taken in CLJS
(defalias seq-or some)

(defn every?
  "A faster version of |every?| using |reduce| instead of |seq|."
  [pred args]
  (reduce (fn [_ arg] (or  (pred arg) (reduced false))) nil args))

(defalias all? every?) ; as in ∀
(defalias seq-and every?)

(defn apply-and [arg-list]
  (every?  identity arg-list))

(defn apply-or  [arg-list]
  (some?   identity arg-list))

(defn dor [& args] ; xor ; why "dor"?
  (and (apply-or args)
       (not (apply-and args))))

#?(:clj
(defmacro fn-logic-base
  [oper & preds]
  (let [arg (gensym)]
   `(fn [~arg]
      (~oper ~@(for [pred preds] `(~pred ~arg)
                 #_(if (and (if-cljs &env false true) (seq? pred))
                     ; Tries to extern it
                     `(~(try (eval pred)
                          (catch java.lang.Throwable _ pred)) ~arg)
                     `(~pred ~arg))))))))

#?(:clj (defmacro fn-or  [& preds]
          `(fn-logic-base or  ~@preds)))
#?(:clj (defmacro fn-and [& preds]
          `(fn-logic-base and ~@preds)))
#?(:clj (defmacro fn-not [pred]
          `(fn-logic-base not ~pred)))

(def falsey? (fn-or false? nil? ))
(def truthy? (fn-or true?  nnil?))

(defn splice-or  [obj compare-fn & coll]
  (some?  (partial compare-fn obj) coll))
(defn splice-and [obj compare-fn & coll]
  (every? (partial compare-fn obj) coll))

#?(:clj
(defmacro coll-base [logical-oper & elems]
  (let [bin-pred (gensym)
        obj      (gensym)]
   `(fn [~bin-pred ~obj]
      (~logical-oper
        ~@(for [elem elems]
            `(~bin-pred ~obj ~elem)))))))

#?(:clj
(defmacro coll-or [& elems]
  `(coll-base or ~@elems)))

#?(:clj
(defmacro coll-and
  {:usage "((and-coll 1 2 3) < 0) => true (0 is less than 1, 2, and 3)"}
  [& elems]
  `(coll-base and ~@elems)))

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

(defn rcompare
  "Reverse comparator."
  {:attribution "taoensso.encore"}
  [x y]
  (compare y x))

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

#?(:clj
(defmacro condf1 [& args] `(fn [arg#] (condf arg# ~@args))))

#?(:clj
(defmacro condf& [& args] `(fn [& args#] (condf args# ~@args))))

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

#?(:clj
(defmacro ifn [obj pred true-fn false-fn]
  `(let [obj-f# ~obj] (if (~pred obj-f#) (~true-fn obj-f#) (~false-fn obj-f#)))))

#?(:clj
(defmacro ifc [obj pred true-expr false-expr]
  `(let [obj-f# ~obj] (if (~pred obj-f#) ~true-expr ~false-expr))))

#?(:clj
(defmacro ifp [obj pred true-fn false-fn]
  `(let [obj-f# ~obj] (if ~pred (~true-fn obj-f#) (~false-fn obj-f#)))))

#?(:clj (defmacro ifn1 [x0 x1 x2] `(fn [arg#] (ifn arg# ~x0 ~x1 ~x2))))
#?(:clj (defmacro ifp1 [x0 x1 x2] `(fn [arg#] (ifp arg# ~x0 ~x1 ~x2))))
#?(:clj (defmacro ifc1 [x0 x1 x2] `(fn [arg#] (ifc arg# ~x0 ~x1 ~x2))))

#?(:clj
(defmacro whenf
  "Analogous to ifn.
   (whenf 1 nnil? inc) = (ifn 1 nnil? inc identity)
   whenf : identity :: when : nil"
  [obj pred true-fn]
  `(let [obj-f# ~obj] (if (~pred obj-f#) (~true-fn obj-f#) obj-f#))))

#?(:clj
(defmacro whenc
  "`whenf` + `ifc`"
  [obj pred true-expr]
  `(let [obj-f# ~obj] (if (~pred obj-f#) ~true-expr obj-f#))))

#?(:clj
(defmacro whenp
  "`whenf` + `ifp`"
  [obj pred true-fn]
  `(let [obj-f# ~obj] (if ~pred (~true-fn obj-f#) obj-f#))))

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
 {:source "https://github.com/zcaudate/hara/blob/master/candidates/src/control.clj"}
  ([bindings then]
    `(if-let ~bindings ~then nil))
  ([[bnd expr & more] then else]
    `(clojure.core/if-let [~bnd ~expr]
        ~(if more
            `(if-let [~@more] ~then ~else)
            then)
         ~else))))

#?(:clj
(defmacro when-let
 "An alternative to when-let where more bindings can be added"
 {:attribution "Alex Gunnarson"}
  ([[var- expr & more] & body]
    `(clojure.core/when-let [~var- ~expr]
      ~@(if more
            (list `(when-let [~@more] ~@body))
            body)))))

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
