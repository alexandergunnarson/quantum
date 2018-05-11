(ns quantum.untyped.core.logic
  (:refer-clojure :exclude
    [= and not or
     if-let when-let])
  (:require
    [clojure.core              :as core]
    [quantum.untyped.core.core :as ucore]
    [quantum.untyped.core.form.evaluate
      :refer [case-env]]
    [quantum.untyped.core.vars :as uvar
      :refer [defalias defmacro-]]))

(ucore/log-this-ns)

#?(:clj (defmacro default [v else] `(let [v# ~v] (if (nil? v#) ~else v#))))

;; ===== Logical operators ===== ;;

;; ----- Unary operators ----- ;;

        (defalias not  core/not)

;; ----- Binary operators ----- ;;

        ;; Tests value-equivalence
        (defalias =    core/=)

        ;; Tests identity-equivalence
        (defalias ref= identical?)

#?(:clj (defmacro implies? [a b] `(if ~a ~b true)))

;; ----- Infinitary operators ----- ;;

#?(:clj (defalias and  core/and))

#?(:clj (defmacro nand [& args] `(not (and ~@args))))

#?(:clj (defalias or   core/or))

#?(:clj (defmacro nor  [& args] `(not (or  ~@args))))

#?(:clj
(defmacro xor
  {:attribution 'alexandergunnarson}
  ([] nil)
  ([x] false)
  ([x y] (if x (not y) y))
  ([x y & next]
    `(if ~x (when-not (and ~y ~@next) ~x) (xor ~y ~@next)))))

;; TODO `xnor`
#?(:clj (declare xnor))

;; ===== Function-logical operators ===== ;;

(defn fn=     [x] (fn [y] (=    x y)))
(defn fn-not= [x] (fn [y] (not= x y)))

#?(:clj
(defmacro fn-logic-base
  [oper & preds]
  (let [arg (gensym "arg")]
   `(fn [~arg] (~oper ~@(for [pred preds] `(~pred ~arg)))))))

#?(:clj (defmacro fn-not      [pred]    `(fn-logic-base not  ~pred  )))

#?(:clj (defmacro fn-and      [& preds] `(fn-logic-base and  ~@preds)))
#?(:clj (defmacro fn-nand     [& preds] `(fn-logic-base nand ~@preds)))

#?(:clj (defmacro fn-or       [& preds] `(fn-logic-base or   ~@preds)))
#?(:clj (defmacro fn-nor      [& preds] `(fn-logic-base nor  ~@preds)))

#?(:clj (defmacro fn-xor      [& preds] `(fn-logic-base xor  ~@preds)))
#?(:clj (defmacro fn-xnor     [& preds] `(fn-logic-base xnor ~@preds)))

#?(:clj (defmacro fn-implies? [a b]     `(fn-logic-base implies? ~a ~b)))

;; ===== `cond(f|c|p)` ===== ;;

#?(:clj
(defmacro ifs
  "Like `clojure.core/cond`, but accepts an uneven number of arguments, in which case
   the last functions as the default branch. If no default branch is supplied, an
   exception branch will be emitted."
  ([then-expr] then-expr)
  ([cond-expr then-expr]
    `(if ~cond-expr
         ~then-expr
         (throw (ex-info "`cond`: No matching clause" {}))))
  ([cond-expr then-expr & clauses]
    `(if ~cond-expr
         ~then-expr
         (ifs ~@clauses)))))

#?(:clj
(defmacro condf
  "Like `cond`, with each expr as a function applied to the initial argument, ->`obj`."
  {:attribution "alexandergunnarson"
   :todo        #{"Simplify"}}
  [obj & clauses]
  (let [gobj (gensym "obj__")
        illegal-argument (case-env :clj 'IllegalArgumentException. :cljs 'js/Error.)
        emit (fn emit [obj args]
               (let [[[a b c :as clause] more]
                       (split-at 2 args)
                     n (count clause)]
                 (ifs
                   (= 0 n) `(throw (~illegal-argument
                                     (str "No matching clause for " ~obj)))
                   (= 1 n) `(~a ~obj)
                   (= 2 n) `(if (~a ~obj)
                                (~b ~obj)
                                ~(emit obj more))
                   (emit obj more))))]
  `(let [~gobj ~obj]
       ~(emit gobj clauses)))))

#?(:clj (defmacro condf1 [& args] `(fn [  arg# ] (condf arg#  ~@args))))
#?(:clj (defmacro condf& [& args] `(fn [& args#] (condf args# ~@args))))

#?(:clj
(defmacro condfc
  "Like `condf`, but each expr is essentially wrapped in a `constantly`."
  [obj & clauses]
  (let [gobj (gensym "obj__")
        illegal-argument (case-env :clj 'IllegalArgumentException. :cljs 'js/Error.)
        emit (fn emit [obj args]
               (let [[[a b c :as clause] more]
                       (split-at 2 args)
                     n (count clause)]
                 (ifs
                   (= 0 n) `(throw (~illegal-argument (str "No matching clause for " ~obj)))
                   (= 1 n) `(~a ~obj)
                   (= 2 n) `(if (or ~(= a :else)
                                    (~a ~obj))
                                ~b ; As in, this expression is not used as a function taking @obj as an argument
                                ~(emit obj more))
                   (emit obj more))))]
  `(let [~gobj ~obj]
       ~(emit gobj clauses)))))

(def is? #(%1 %2)) ; for use with condp

#?(:clj
(defmacro condpc
  "`condp` for colls."
  {:usage "(condpc = 1 (coll-or 2 3) (println '2 or 3!')"}
  [pred expr & clauses]
  (let [gpred (gensym "pred__")
        gexpr (gensym "expr__")
        emit (fn emit [pred expr args]
               (let [[[a b c :as clause] more]
                       (split-at (if (= :>> (second args)) 3 2) args)
                       n (count clause)]
                 (ifs
                   (= 0 n) nil ; No matching clause `(throw (IllegalArgumentException. (str "No matching clause: " ~expr)))
                   (= 1 n) a
                   (= 2 n) `(if (if (fn? ~a)
                                    (~a ~pred ~expr)
                                    (~pred ~expr ~a))
                                ~b
                                ~(emit pred expr more))
                   `(clojure.core/if-let [p# (~pred ~a ~expr)]
                      (~c p#)
                      ~(emit pred expr more)))))]
  `(let [~gpred ~pred
         ~gexpr ~expr]
       ~(emit gpred gexpr clauses)))))

;; ===== `if(n|c|p)` ===== ;;

;; TODO compress this?

#?(:clj (defmacro ifn    [x pred tf ff] `(let [x# ~x] (if (~pred x#)     (~tf x#)     (~ff x#)    ))))
#?(:clj (defmacro ifn->  [x pred tf ff] `(let [x# ~x] (if (-> x# ~pred)  (->  x# ~tf) (->  x# ~ff)))))
#?(:clj (defmacro ifn->> [x pred tf ff] `(let [x# ~x] (if (->> x# ~pred) (->> x# ~tf) (->> x# ~ff)))))
#?(:clj (defmacro ifn1   [x0 x1 x2]     `(fn [arg#] (ifn arg# ~x0 ~x1 ~x2))))
#?(:clj (defmacro ifc    [x pred t  f ] `(let [x# ~x] (if (~pred x#)     ~t           ~f          ))))
#?(:clj (defmacro ifc->  [x pred t  f ] `(let [x# ~x] (if (-> x# ~pred)  ~t           ~f          ))))
#?(:clj (defmacro ifc->> [x pred t  f ] `(let [x# ~x] (if (->> x# ~pred) ~t           ~f          ))))
#?(:clj (defmacro ifc1   [x0 x1 x2]     `(fn [arg#] (ifc arg# ~x0 ~x1 ~x2))))
#?(:clj (defmacro ifp    [x pred tf ff] `(let [x# ~x] (if ~pred          (~tf x#)     (~ff x#)    ))))
#?(:clj (defmacro ifp->  [x pred tf ff] `(let [x# ~x] (if ~pred          (->  x# ~tf) (->  x# ~ff)))))
#?(:clj (defmacro ifp->> [x pred tf ff] `(let [x# ~x] (if ~pred          (->> x# ~tf) (->> x# ~ff)))))
#?(:clj (defmacro ifp1   [x0 x1 x2]     `(fn [arg#] (ifp arg# ~x0 ~x1 ~x2))))

;; ===== `when(f|c|p)` ===== ;;

#?(:clj
(defmacro whenf
  "Analogous to `ifn`.
   (whenf 1 val? inc)` = `(ifn 1 val? inc identity)`
   `whenf` : `identity` :: `when` : `nil`"
  [x pred tf] `(let [x# ~x] (if (~pred x#) (~tf x#) x#))))

#?(:clj
(defmacro whenf->
  "Analogous to `ifn->`.
   `(whenf-> 1 val? inc)` = `(ifn-> 1 val? inc identity)`
   `whenf->` : `identity` :: `when` : `nil`"
  [x pred & texprs] `(let [x# ~x] (if (-> x# ~pred) (-> x# ~@texprs) x#))))

#?(:clj
(defmacro whenf->>
  "Analogous to `ifn->>`.
   `(whenf->> 1 val? inc)` = `(ifn->> 1 val? inc identity)`
   `whenf->>` : `identity` :: `when` : `nil`"
  [x pred & texprs] `(let [x# ~x] (if (->> x# ~pred) (->> x# ~@texprs) x#))))

#?(:clj (defmacro whenf1 [x0 x1] `(fn [arg#] (whenf arg# ~x0 ~x1))))

#?(:clj (defmacro whenc    "`whenf` + `ifc`"       [x pred      texpr ] `(let [x# ~x] (if (~pred x#) ~texpr x#))))
#?(:clj (defmacro whenc->  "`whenf->` + `ifc->`"   [x pred-expr texpr ] `(let [x# ~x] (if (->  x# ~pred-expr) ~texpr x#))))
#?(:clj (defmacro whenc->> "`whenf->>` + `ifc->>`" [x pred-expr texpr ] `(let [x# ~x] (if (->> x# ~pred-expr) ~texpr x#))))
#?(:clj (defmacro whenc1                           [x0 x1]              `(fn [arg#] (whenc arg# ~x0 ~x1))))
#?(:clj (defmacro whenp    "`whenf` + `ifp`"       [x pred      tf    ] `(let [x# ~x] (if ~pred (~tf x#)          x#))))
#?(:clj (defmacro whenp->  "`whenf->` + `ifp->`"   [x pred    & texprs] `(let [x# ~x] (if ~pred (->  x# ~@texprs) x#))))
#?(:clj (defmacro whenp->> "`whenf->>` + `ifp->>`" [x pred    & texprs] `(let [x# ~x] (if ~pred (->> x# ~@texprs) x#))))
#?(:clj (defmacro whenp1                           [x0 x1]              `(fn [arg#] (whenp arg# ~x0 ~x1))))

;; ===== Conditional `let` bindings ===== ;;

#?(:clj
(defmacro if-let-base
  {:attribution "alexandergunnarson"}
  [cond-op #_symbol? [bind expr & more] then else]
  `(let [temp# ~expr ~bind temp#]
     (~cond-op temp#
         ~(if (seq more)
             `(if-let-base ~cond-op [~@more] ~then ~else)
             then)
         ~else))))

#?(:clj
(defmacro if-let
  "Like `if-let`, but multiple bindings can be used."
  [& args] `(if-let-base if ~@args)))

#?(:clj
(defmacro if-not-let
  "if : if-let :: if-not : if-not-let. All conditions must be false."
  [& args] `(if-let-base if-not ~@args)))

#?(:clj
(defmacro when-let-base
  {:attribution "alexandergunnarson"}
  [cond-op #_symbol? [bind expr & more] & body]
    `(let [temp# ~expr ~bind temp#]
       (~cond-op temp#
         ~(if (seq more)
              `(when-let-base ~cond-op [~@more] ~@body)
              `(do ~@body))))))

#?(:clj
(defmacro when-let
  "Like `when-let`, but multiple bindings can be used."
  [& args] `(if-let-base when ~@args)))

#?(:clj
(defmacro when-not-let
  "when : when-let :: when-not : when-not-let. All conditions must be false."
  [& args] `(when-let-base when-not ~@args)))

#?(:clj
(defmacro cond-let
  "Transforms into a series of nested `if-let` statements."
  {:attribution "alexandergunnarson"}
  ([] nil) ; no else
  ([else] else)
  ([bindings then & more] `(if-let ~bindings ~then (cond-let ~@more)))))

#?(:clj
(defmacro logical-let-base
  {:attribution "alexandergunnarson"}
  ([logical-op #_symbol? [bind expr & more]]
  `(let [temp# ~expr ~bind temp#]
     (~logical-op temp#
        ~(if (seq more)
            `(logical-let-base ~logical-op [~@more])
            `(~logical-op)))))))

#?(:clj (defmacro and-let  [bindings] `(logical-let-base and  ~bindings)))
#?(:clj (defmacro or-let   [bindings] `(logical-let-base or   ~bindings)))
;; TODO These will require a different, non-incremental approach
#?(:clj (defmacro nand-let [bindings] (throw (ex-info "TODO" {})) #_`(logical-let-base nand ~bindings)))
#?(:clj (defmacro nor-let  [bindings] (throw (ex-info "TODO" {})) #_`(logical-let-base nor  ~bindings)))
#?(:clj (defmacro xor-let  [bindings] (throw (ex-info "TODO" {})) #_`(logical-let-base xor  ~bindings)))
#?(:clj (defmacro xnor-let [bindings] (throw (ex-info "TODO" {})) #_`(logical-let-base xnor ~bindings)))

;; ===== `coll-(or|and)` ===== ;;

#?(:clj
(defmacro coll-base [logical-op & elems]
  (let [bin-pred (gensym)
        obj      (gensym)]
   `(fn [~bin-pred ~obj]
      (~logical-op
        ~@(for [elem elems]
            `(~bin-pred ~obj ~elem)))))))

#?(:clj
(defmacro coll-or [& elems] `(coll-base or ~@elems)))

#?(:clj
(defmacro coll-and
  {:usage "((coll-and 1 2 3) < 0) => true (0 is less than 1, 2, and 3)"}
  [& elems] `(coll-base and ~@elems)))
