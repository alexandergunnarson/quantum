(ns quantum.untyped.core.logic
  (:refer-clojure :exclude
    [= and not or
     if-let when-let])
  (:require
    [clojure.core              :as core]
    [quantum.untyped.core.form.evaluate
      :refer [case-env]]
    [quantum.untyped.core.vars :as uvar
      :refer [defalias defmacro-]]))

#?(:clj (defmacro default [v else] `(let [v# ~v] (if (nil? v#) ~else v#))))

;; ===== Logical operators ===== ;;

        ;; tests value-equivalence
        (defalias =    core/=)

        ;; tests identity-equivalence
        (defalias ref= identical?)

        (defalias not  core/not)

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

#?(:clj (defmacro implies? [a b] `(if ~a ~b true)))

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
  "Like `condf`, but each expr is essentially wrapped in a `constantly`."
  [obj & clauses]
  (let [gobj (gensym "obj__")
        illegal-argument (case-env :clj 'IllegalArgumentException. :cljs 'js/Error.)
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

