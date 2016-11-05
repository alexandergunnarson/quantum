(ns
  ^{:doc "Useful function-related functions (one could say 'metafunctions').

          Higher-order functions, currying, monoids, reverse comp, arrow macros, inner partials, juxts, etc."
    :attribution "Alex Gunnarson"
    :cljs-self-referencing? true
    :figwheel-no-load       true}
  quantum.core.fn
           (:refer-clojure :exclude [constantly])
           (:require
             [clojure.walk                        ]
             [quantum.core.core        :as qcore  ]
             [quantum.core.data.map    :as map    ]
             [quantum.core.macros.core :as cmacros
               :refer        [#?@(:clj [when-cljs compile-if])]
               :refer-macros [when-cljs]]
             [quantum.core.vars        :as var
               :refer        [#?(:clj defalias)]
               :refer-macros [defalias]]
     #?(:clj [clojure.pprint           :as pprint
               :refer [pprint]                    ]))
  #?(:cljs (:require-macros
             [quantum.core.fn          :as fn
               :refer [fn1]                       ])))

; To signal that it's a multi-return
(deftype MultiRet [val])

#?(:clj (defalias jfn memfn))

#?(:clj
(defmacro fn&* [arity f & args]
  (let [f-sym (gensym) ct (count args)
        macro? (-> f resolve meta :macro)]
    `(let [~f-sym ~(when-not macro? f)]
     (fn ~@(for [i (range (if arity arity       0 )
                          (if arity (inc arity) 10))]
             (let [args' (vec (repeatedly i #(gensym)))]
               `(~args' (~(if macro? f f-sym) ~@args ~@args'))))
         ; Add variadic arity if macro
         ~@(when (and (not macro?)
                      (nil? arity))
             (let [args' (vec (repeatedly (+ ct 10) #(gensym)))]
               [`([~@args' & xs#] (apply ~f-sym ~@args ~@args' xs#))])))))))

#?(:clj (defmacro fn&  [f & args] `(fn&* nil ~f ~@args)))
#?(:clj (defmacro fn&2 [f & args] `(fn&* 2   ~f ~@args)))

(defn constantly
  {:from 'com.rpl.specter.impl}
  [v]
  (fn ([] v)
      ([x0] v)
      ([x0 x1] v)
      ([x0 x1 x2] v)
      ([x0 x1 x2 x3] v)
      ([x0 x1 x2 x3 x4] v)
      ([x0 x1 x2 x3 x4 x5] v)
      ([x0 x1 x2 x3 x4 x5 x6] v)
      ([x0 x1 x2 x3 x4 x5 x6 x7] v)
      ([x0 x1 x2 x3 x4 x5 x6 x7 x8] v)
      ([x0 x1 x2 x3 x4 x5 x6 x7 x8 x9] v)
      ([x0 x1 x2 x3 x4 x5 x6 x7 x8 x9 x10] v)
      ([x0 x1 x2 x3 x4 x5 x6 x7 x8 x9 x10 & r] v)))

(defalias fn' constantly)

#?(:clj
(defmacro mfn
  "|mfn| is short for 'macro-fn', just as 'jfn' is short for 'java-fn'.
   Originally named |functionize| by mikera."
  ([macro-sym]
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
  "Call function `f` with (optional) arguments.
   Like clojure.core/apply, but doesn't expand/splice the last argument."
  {:attribution 'alexandergunnarson}
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

#?(:clj
(defmacro aritoid
  "Combines fns as arity-callers."
  {:attribution "Alex Gunnarson"
   :equivalent `{(aritoid vector identity conj)
                 (fn ([]      (vector))
                     ([x0]    (identity x0))
                     ([x0 x1] (conj x0 x1)))}}
  [& fs]
  (let [genned  (repeatedly (count fs) #(gensym "f"))
        fs-syms (vec (interleave genned fs))]
   `(let ~fs-syms
      (fn ~@(for [[i f-sym] (map-indexed vector genned)]
              (let [args (vec (repeatedly i #(gensym "x")))]
                `(~args (~f-sym ~@args)))))))))

#?(:clj (defmacro rcomp [& args] `(comp ~@(reverse args))))

#?(:clj (defmacro fn0 [  & args] `(fn [f#] (f# ~@args))))
#?(:clj (defmacro fn1 [f & args] `(fn [arg#] (~f arg# ~@args))))
#?(:clj (defmacro fn$ [f & args] `(fn [arg#] (~f ~@args arg#))))

; MWA: "Macro WorkAround"
#?(:clj (defmacro MWA ([f] `(fn1 ~f)) ([n f] `(mfn ~n ~f))))

(defn fn-bi [arg] #(arg %1 %2))
(defn unary [pred]
  (fn ([a    ] (fn1 pred a))
      ([a b  ] (fn1 pred a b))
      ([a b c] (fn1 pred a b c))))

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
  "Like prog1 in Common Lisp, or a `(do)` that returns the first form."
  [expr & exprs]
  `(let [ret# ~expr] ~@exprs ret#)))


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
  (juxtm* sorted-map args))

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

#?(:clj
(defmacro doto-2
  "useful for e.g. logging fns"
  {:usage `(doto-2 [1 2 3 4 5] (log/pr :debug "is result"))}
  [expr side]
  `(let [expr# ~expr]
     (~(first side) ~(second side) expr# ~@(-> side rest rest))
     expr#)))

#?(:clj (defalias with qcore/with))

(defn with-pr->>  [obj      ] (do (println obj) obj))
(defn with-msg->> [msg  obj ] (do (println msg) obj))
(defn with->>     [expr obj ] (do expr          obj))
(defn withf->>    [f    obj ] (do (f obj)       obj))
(defn withf       [obj  f   ] (do (f obj)       obj))
(defn withfs      [obj  & fs]
  (doseq [f fs] (f obj))
  obj)

#?(:clj
  (compile-if (Class/forName "java.util.function.Predicate")
    (defn ->predicate [f]
      (reify java.util.function.Predicate
        (^boolean test [this ^Object elem]
          (f elem))))
    (defn ->predicate [f] (throw (ex-info "java.util.function.Predicate not available: probably using JDK < 8" nil)))))

; ========= REDUCER PLUMBING ==========

#?(:clj (defn maybe-unary
  "Not all functions used in `tesser/fold` and `tesser/reduce` have a
  single-arity form. This takes a function `f` and returns a fn `g` such that
  `(g x)` is `(f x)` unless `(f x)` throws ArityException, in which case `(g
  x)` returns just `x`."
  {:attribution "tesser.utils"}
  [f]
  (fn wrapper
    ([] (f))
    ([x] (try
           (f x)
           (catch clojure.lang.ArityException e
             x)))
    ([x y] (f x y))
    ([x y & more] (apply f x y more)))))

#?(:clj
(defmacro rfn
  "Creates a reducer-safe function."
  [arglist & body]
  (let [sym (gensym)]
    (case (count arglist)
          1 `(fn ~sym (~arglist ~@body)
                      ([k# v#] (~sym [k# v#])))
          2 `(fn ~sym ([[k# v#]] (~sym k# v#))
                      (~arglist ~@body)
                      ([ret# k# v#] (~sym ret# [k# v#])))
          3 `(fn ~sym ([ret# [k# v#]] (~sym ret# k# v#))
                      (~arglist ~@body))
          (throw (ex-info "Illegal arglist count passed to rfn" {:arglist arglist}))))))
