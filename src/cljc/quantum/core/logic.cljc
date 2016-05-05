(ns
  ^{:doc "Logic-related functions. nnil?, nempty?, fn-not, fn-and, splice-or,
          ifn, whenf*n, compr, fn->, condpc, and the like. Extremely useful
          and used everywhere in the quantum library."
    :attribution "Alex Gunnarson"
    :figwheel-no-load true}
  quantum.core.logic
           (:refer-clojure :exclude [if-let when-let])
           (:require [quantum.core.fn          :as fn
                       :refer [#?@(:clj [f*n fn->])]      ]
                     [quantum.core.vars        :as var
                       :refer [#?(:clj defalias)]         ]
                     [quantum.core.macros.core :as cmacros
                       :refer [if-cljs]                   ])
  #?(:cljs (:require-macros
                     [quantum.core.fn          :as fn
                       :refer [f*n fn->]                  ]
                     [quantum.core.vars        :as var
                       :refer [defalias]                  ])))

; TODO: ; cond-not, for :pre
; |Switch| is implemented using an array and then points to the code.
; String |switch| is implemented using a map8   

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
; Otherwise ExceptionInInitializerError if not macro
#?(:clj (defmacro eq?  [x] `(fn-> (=    ~x))))

#?(:clj (defalias fn=     eq?))
#?(:clj (defalias fn-eq?  eq?))

#?(:clj (defmacro neq? [x] `(fn-> (not= ~x))))
#?(:clj (defalias fn-neq?  neq?))
(def any?    some)
(defn apply-and [arg-list]
  (every? identity arg-list))
(defn apply-or  [arg-list]
  (any?   identity arg-list))
(defn dor [& args] ; xor ; why "dor"?
  (and (apply-or args)
       (not (apply-and args))))
(defn pred-or   [pred obj args]
  (apply-or  (map (pred obj) args)))
(defn pred-and  [pred obj args]
  (apply-and (map (pred obj) args)))

#?(:clj
(defmacro fn-logic-base
  "Auto-externs its fn arguments via a compile-time |eval|. Convenient!"
  [oper & preds]
  (let [arg (gensym)]
   `(fn [~arg]
      (~oper ~@(for [pred preds]
                 (if (and (if-cljs &env false true) (seq? pred))
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

(defn splice-or  [obj compare-fn & coll]
  (any?   (partial compare-fn obj) coll))
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
                   (= 1 n) a
                   (= 2 n) `(if (or ~(= a :else) 
                                    (~a ~obj))
                                (~b ~obj)
                                ~(emit obj more))
                   :else   (emit obj more))))]
  `(let [~gobj ~obj]
       ~(emit gobj clauses)))))

#?(:clj
(defmacro condf*n [& args]
  `(fn [obj#] (condf obj# ~@args))))

#?(:clj
(defmacro condf**n [& args]
  `(fn [& inner-args#] (condf inner-args# ~@args))))

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
                   (= 1 n) a
                   (= 2 n) `(if (or ~(= a :else) 
                                    (~a ~obj))
                                ~b ; As in, this expression is not used as a function taking @obj as an argument
                                ~(emit obj more))
                   :else   (emit obj more))))]
  `(let [~gobj ~obj]
       ~(emit gobj clauses)))))

#?(:clj
(defmacro ifn [obj pred true-fn false-fn] ; macro to delay
  `(let [obj-f# ~obj] 
     (if (~pred obj-f#) (~true-fn obj-f#) (~false-fn obj-f#)))))

#?(:clj
(defmacro ifc [obj pred true-expr false-expr] ; macro to delay
  `(let [obj-f# ~obj] 
     (if (~pred obj-f#) ~true-expr ~false-expr))))

#?(:clj
(defmacro ifp [obj pred true-fn false-fn] ; macro to delay
  `(let [obj-f# ~obj] 
     (if ~pred (~true-fn obj-f#) (~false-fn obj-f#)))))

#?(:clj
(defmacro ifcf*n [pred true-expr false-expr]
  `(fn [arg#] (ifc arg# ~pred ~true-expr ~false-expr))))

#?(:clj
(defmacro if*n [pred true-fn false-fn]
  `(fn [arg#] (ifn arg# ~pred ~true-fn ~false-fn))))

#?(:clj
(defmacro whenf
  "Analogous to ifn.
   (whenf 1 nnil? inc) = (ifn 1 nnil? inc identity)
   whenf : identity :: when : nil"
  [obj pred true-fn]
  `(let [obj-f# ~obj] 
     (if (~pred obj-f#) (~true-fn obj-f#) obj-f#))))

#?(:clj
(defmacro whenc
  "Analogous to whenf, but evaluates the result instead of
   using it as a function."
  [obj pred true-expr]
  `(let [obj-f# ~obj] 
     (if (~pred obj-f#) ~true-expr obj-f#))))

#?(:clj
(defmacro whenp
  [obj pred true-fn]
  `(let [obj-f# ~obj] 
     (if ~pred (~true-fn obj-f#) obj-f#))))

#?(:clj
(defmacro whenf*n 
  "Analogous to if*n.
   (whenf*n nnil? inc) = (if*n nnil? inc identity)"
  [pred true-fn] `(fn [arg#] (whenf arg# ~pred ~true-fn))))

#?(:clj
(defmacro whencf*n 
  "Analogous to whenf*n."
  [pred true-obj] `(fn [arg#] (whenc arg# ~pred ~true-obj))))


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
  "Takes a binding-form and a set of test/expr pairs. Evaluates each test
  one at a time. If a test returns logical true, cond-let evaluates and
  returns expr with binding-form bound to the value of test and doesn't
  evaluate any of the other tests or exprs. To provide a default value
  either provide a literal that evaluates to logical true and is
  binding-compatible with binding-form, or use :else as the test and don't
  refer to any parts of binding-form in the expr. (cond-let binding-form)
  returns nil."
  {:author "Stephen C. Gilardi"
   :source "clojure.contrib.cond"}
  [bindings & clauses]
  (let [binding (first bindings)]
    (clojure.core/when-let [[test expr & more] clauses]
      (if (= test :else)
        expr
        `(clojure.core/if-let [~binding ~test]
           ~expr
           (cond-let ~bindings ~@more)))))))