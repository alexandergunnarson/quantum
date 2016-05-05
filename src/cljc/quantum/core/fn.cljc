(ns
  ^{:doc "Useful function-related functions (one could say 'metafunctions').

          Higher-order functions, currying, monoids, reverse comp, arrow macros, inner partials, juxts, etc."
    :attribution "Alex Gunnarson"
    :cljs-self-referencing? true
    :figwheel-no-load       true}
  quantum.core.fn
           (:require [clojure.walk                        ]
                     [quantum.core.core        :as qcore  ]
                     [quantum.core.data.map    :as map    ]
                     [quantum.core.macros.core :as cmacros
                       :refer [#?(:clj when-cljs)]        ]
                     [quantum.core.vars        :as var
                       :refer [#?(:clj defalias)]         ]
             #?(:clj [clojure.pprint           :as pprint
                       :refer [pprint]                    ]))
  #?(:cljs (:require-macros
                     [quantum.core.fn          :as fn
                       :refer [f*n]                       ]
                     [quantum.core.vars        :as var
                       :refer [defalias]                  ])))

; To signal that it's a multi-return 
(deftype MultiRet [val])

#?(:clj (defalias jfn memfn))

#?(:clj
(defmacro mfn
  "|mfn| is short for 'macro-fn', just as 'jfn' is short for 'java-fn'.
   Originally named |functionize| by mikera."
  ([macro-sym]
    ; When CLJS
    (when-cljs &env (throw (Exception. "|mfn| not supported for CLJS.")))
   `(fn [& args#]
      (qcore/js-println "WARNING: Runtime eval with |mfn| via" '~macro-sym)
      (clojure.core/eval (cons '~macro-sym args#))))
  ([n macro-sym]
    (let [genned-arglist (->> (repeatedly gensym) (take n) (into []))]
      `(fn ~genned-arglist
         (~macro-sym ~@genned-arglist))))))

(def fn-nil (constantly nil))

(defn call
  "Call function `f` with additional arguments."
  {:attribution "Alex Gunnarson"}
  ([f]                    (f))
  ([f x]                  (f x))
  ([f x y]                (f x y))
  ([f x y z]              (f x y z))
  ([f x y z & more] (apply f x y z more)))

(defn firsta 
  "Accepts any number of arguments and returns the first."
  {:attribution "parkour.reducers"}
  ([x]            x)
  ([x y]          x)
  ([x y z]        x)
  ([x y z & more] x))

(defn seconda
  "Accepts any number of arguments and returns the second."
  {:attribution "parkour.reducers"}
  ([x y]          y)
  ([x y z]        y)
  ([x y z & more] y))

;___________________________________________________________________________________________________________________________________
;=================================================={  HIGHER-ORDER FUNCTIONS   }====================================================
;=================================================={                           }====================================================
(defn- do-curried
  {:attribution "clojure.core.reducers"}
  [name doc meta args body]
  (let [cargs (vec (butlast args))]
    `(defn ~name ~doc ~meta
       (~cargs (fn [x#] (~name ~@cargs x#)))
       (~args ~@body))))

#?(:clj
(defmacro defcurried
  "Builds another arity of the fn that returns a fn awaiting the last
  param."
  {:attribution "clojure.core.reducers"}
  [name doc meta args & body]
  (do-curried name doc meta args body)))

(defn zeroid
  {:attribution "Alex Gunnarson"}
  [func base] ; is it more efficient to do it differently? ; probably not
  (fn ([]                                              base)
      ([arg1 arg2]                               (func arg1 arg2))
      ([arg1 arg2 arg3]                    (func (func arg1 arg2) arg3))
      ([arg1 arg2 arg3 & args] (apply func (func (func arg1 arg2) arg3) args))))

(defn monoid
  "Builds a combining fn out of the supplied operator and identity
  constructor. op must be associative and ctor called with no args
  must return an identity value for it."
  {:attribution "clojure.core.reducers"}
  [op ctor]
  (fn mon
    ([]    (ctor))
    ([a b] (op a b))))

#?(:clj
(defmacro compr
  [& args]
  `(comp ~@(reverse args))))

#_(defn fn*
  "This doesn't work because it is a constant in the Clojure compiler.
   It should be munged but isn't.
   Likewise, simply copying and pasting the code for |partial| from clojure.core doesn't work either..."
  [& args]
  (apply partial args))

;#?(:clj (defalias f*n dep/f*n))

#?(:clj
(defmacro f*n  [func & args]
  `(fn [arg-inner#]
     (~func arg-inner# ~@args))))

; MWA: "Macro WorkAround"
#?(:clj (defmacro MWA ([f] `(f*n ~f)) ([n f] `(mfn ~n ~f))))

(defn f**n [func & args]
  (fn [& args-inner]
    (apply func (concat args-inner args))))

(defn *fn [& args] (f*n apply args))

(defn fn-bi [arg] #(arg %1 %2))
(defn unary [pred]
  (fn ([a    ] (f*n pred a))
      ([a b  ] (f*n pred a b))
      ([a b c] (f*n pred a b c))))

#?(:clj
(defmacro fn->
  "Equivalent to |(fn [x] (-> x ~@body))|"
  {:attribution "thebusby.bagotricks"}
  [& body]
  `(fn [x#] (-> x# ~@body))))

#?(:clj 
(defmacro fn->>
  "Equivalent to |(fn [x] (->> x ~@body))|"
  {:attribution "thebusby.bagotricks"}
  [& body]
  `(fn [x#] (->> x# ~@body))))

#?(:clj
(defmacro with-do
  "Same as lisp's |prog1|."
  [expr & exprs]
  `(let [result# ~expr]
     ~@exprs
     result#)))


; TODO: deprecate these... likely they're not useful
(defn call->   [arg & [func & args]] ((apply func args) arg))
(defn call->>  [& [func & args]] ((apply func    (butlast args)) (last args)))

; TODO: Find |<<-| to convert a -> to <<-

#?(:clj
(defmacro <-
  "Converts a ->> to a ->
   (->> (range 10) (map inc) (<- doto prn) (reduce +))
   Note: syntax modified from original."
   {:attribution "thebusby.bagotricks"}
  ([x] `(~x))
  ([cmd & body]
      `(~cmd ~(last body) ~@(butlast body)))))

; ---------------------------------------
; ================ JUXTS ================ (possibly deprecate these?)
; ---------------------------------------

; (defn juxtm*
;   [map-type args]
;   (if (-> args count even?)
;       (fn [arg] (->> arg ((apply juxt args)) (apply map-type)))
;       (throw (#+clj  IllegalArgumentException.
;               #+cljs js/Error.
;               "juxtm requires an even number of arguments"))))

(defn juxtm*
  [map-type args]
  (if (-> args count even?)
      (fn [arg] (->> arg ((apply juxt args)) (apply map-type)))
      (throw (#?(:clj IllegalArgumentException. :cljs js/Error.)
              "juxtm requires an even number of arguments"))))

(defn juxtk*
  [map-type args]
  (when-not (-> args count even?)
    (throw (#?(:clj IllegalArgumentException. :cljs js/Error.) "juxtk requires an even number of arguments")))
  (let [m (apply map-type args)]
    (fn [arg]
      (reduce-kv
        (fn [ret k f]
          (assoc ret k (f arg)))
        m
        m))))

(defn juxtm
  "Like /juxt/, but applies a hash-map instead of a vector.
   Requires an even number of arguments."
  [& args]
  (juxtm* hash-map    args))
(defn juxt-sm
  "Like /juxt/, but applies a sorted-map+ instead of a vector.
   Requires an even number of arguments."
  [& args]
  (juxtm* map/sorted-map args))

(defn juxtk
  "Like /juxtm/, but each key is constant.
   Basically like /select-keys/."
  [& args]
  (juxtk* hash-map    args))

(defn juxt-kv
  [kf vf]
  (fn ([[k v]] [(kf k) (vf v)])
      ( [k v]  [(kf k) (vf v)])))

; ======== WITH =========

; TODO: use whatever REPL's print fn is
; (defn with-pr  [obj]      (do (#+clj  pprint
;                                #+cljs println obj) 
;                               obj))
#?(:clj
(defmacro doto->>
  {:usage '(->> 1 inc (doto->> println "ABC"))}
  [f & args]
  (let [obj (last args)]
    `(do (~f ~@(butlast args) ~obj)
         ~obj))))

(defalias with qcore/with)

(defn with-pr->>  [obj      ] (do (println obj) obj))
(defn with-msg->> [msg  obj ] (do (println msg) obj))
(defn with->>     [expr obj ] (do expr          obj))
(defn withf->>    [f    obj ] (do (f obj)       obj))
(defn withf       [obj  f   ] (do (f obj)       obj))
(defn withfs      [obj  & fs]
  (doseq [f fs] (f obj))
  obj)

; ========= REDUCER PLUMBING ==========

(defn- do-rfn
  {:attribution "clojure.core.reducers"}
  [f1 k fkv]
  `(fn
     ([] (~f1))
     ~(clojure.walk/postwalk
       #(if (sequential? %)
            ((if (vector? %) vec identity)
             (remove #{k} %))
            %)
       fkv)
     ~fkv))

#?(:clj
(defmacro rfn
  "Builds 3-arity reducing fn given names of wrapped fn and key, and k/v impl."
  {:attribution "clojure.core.reducers"}
  [[f1 k] fkv]
  (do-rfn f1 k fkv)))
