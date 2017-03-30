(ns
  ^{:doc "Useful function-related functions (one could say 'metafunctions').

          Higher-order functions, currying, monoids, reverse comp, arrow macros, inner partials, juxts, etc."
    :attribution "alexandergunnarson"}
  quantum.core.fn
       (:refer-clojure :exclude
        [constantly, as->])
       (:require
         [clojure.core             :as core]
         [clojure.walk]
         [quantum.core.vars        :as var
           :refer [defalias]]
         [quantum.core.core        :as qcore]
         [quantum.core.macros.core :as cmacros
           :refer [case-env #?@(:clj [compile-if])
                   gen-args arity-builder max-positional-arity]])
      (:require-macros
        [quantum.core.fn :as self]))

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
#?(:clj (defmacro fn&0 [f & args] `(fn&* 0   ~f ~@args)))
#?(:clj (defmacro fn&1 [f & args] `(fn&* 1   ~f ~@args)))
#?(:clj (defmacro fn&2 [f & args] `(fn&* 2   ~f ~@args)))
#?(:clj (defmacro fn&3 [f & args] `(fn&* 3   ~f ~@args)))

#?(:clj
(defmacro gen-constantly []
  (let [v-sym 'v]
    `(defn ~'constantly
       "Exactly the same as `core/constantly`, but uses efficient positional
        arguments when possible rather than varargs every time."
       [~v-sym]
       (~'fn ~@(arity-builder (core/constantly v-sym) (core/constantly v-sym)))))))

(gen-constantly)
(defalias fn' constantly)

#?(:clj
(defmacro fn'*:arities
  [arities-ct & body]
  (let [f (gensym "this")]
   `(~'fn ~f ~@(arity-builder (fn [args] (println "Args" args) (if (empty? args)
                                               `(do ~@body)
                                               `(~f)))
                              (fn' `(~f))
                              0 arities-ct)))))

#?(:clj
(defmacro fn'*
  "Like `fn'` but re-evaluates the body each time."
  [& body] `(fn'*:arities 4 ~@body))) ; conservative to limit generated code size

#?(:clj
(defmacro mfn
  "`mfn` is short for 'macro-fn', just as 'jfn' is short for 'java-fn'.
   Originally named `functionize` by mikera."
  ([macro-sym]
    (case-env :cljs (throw (ex-info "`mfn` not supported for CLJS." {}))
      `(fn [& args#]
         (qcore/js-println "WARNING: Runtime eval with `mfn` via" '~macro-sym)
         (clojure.core/eval (cons '~macro-sym args#)))))
  ([n macro-sym]
    (let [genned-arglist (->> (repeatedly gensym) (take n) (into []))]
      `(fn ~genned-arglist
         (~macro-sym ~@genned-arglist))))))

#?(:clj
(defmacro gen-call []
  `(~'defn ~'call
     "Call function `f` with (optional) arguments.
      Like clojure.core/apply, but doesn't expand/splice the last argument."
     {:attribution "alexandergunnarson"}
     ~@(arity-builder (fn [args] `(~@args))
                      (fn [args vargs] `(apply ~@args ~vargs))
                      1 18 (fn [i] (if (= i 1) "f" "x"))))))

(gen-call)

; ----- NTHA ----- ;

(defn gen-positional-ntha [position]
  `(~'defn ~(symbol (str "ntha-" position))
     ~(str "Accepts any number of arguments and returns the (n=" position ")th in O(1) time.")
     ~@(arity-builder (fn [args] (nth args position))
                      (fn [args vargs] (nth args position)) (inc position))))

#?(:clj
(defmacro gen-positional-nthas []
  `(do ~@(for [i (range 0 (:clj max-positional-arity))] (gen-positional-ntha i)))))

(gen-positional-nthas)

(defn ntha-&
  "Accepts any number of arguments and returns the nth, variadically, in O(n) time."
  [n] (fn [& args] (nth args n)))

(defalias firsta  ntha-0)
(defalias seconda ntha-1)
(defalias thirda  ntha-2)

#?(:clj
(defmacro gen-ntha []
  (let [n-sym (gensym "n")]
    `(~'defn ~'ntha
       "Accepts any number of arguments and returns the nth.
        If n <= 18, returns in O(1) time; otherwise, in O(n) time via varargs."
       [~(with-meta n-sym {:tag 'long})]
       (case ~n-sym
         ~@(apply concat
             (for [i (range 0 (:clj max-positional-arity))]
               [i (symbol (str "ntha-" i))]))
         (ntha-& ~n-sym))))))

(gen-ntha)
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
  {:attribution "alexandergunnarson"}
  [func base] ; is it more efficient to do it differently? ; probably not
  (fn ([]                                              base)
      ([arg1 arg2]                               (func arg1 arg2))
      ([arg1 arg2 arg3]                    (func (func arg1 arg2) arg3))
      ([arg1 arg2 arg3 & args] (apply func (func (func arg1 arg2) arg3) args))))

#?(:clj
(defmacro aritoid
  "Combines fns as arity-callers."
  {:attribution "alexandergunnarson"
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

#?(:clj
(defmacro gen-fconj
  "Generates the `fconj` function."
  [max-args'-ct max-args-ct]
 `(~'defn ~'fconj
    "Appends the arguments to the parameters with which `f` will be called,
     when `f` is called.
     Does not use data structures unless variadic arity is called.

     `(fn f [a b c] (g a b c inc))` <=> `(fconj g inc)`

     ```
     (let [g (fn [a b c d e] (+ a (d b) (e c)))]
       ((fconj g inc -)
        1 2 3))
     -> (+ 1 (inc 2) (- 3)) -> 1
     ```"
    {:attribution "alexandergunnarson"}
    ~@(let [all-args'     (->> (range max-args'-ct)
                               (map (fn [i] (symbol (str "a" i "'")))))
            &arg'         (symbol "as'")
            all-args      (->> (range max-args-ct)
                               (map (fn [i] (symbol (str "a" i)))))
            &arg          (symbol "as")
            f-sym         (symbol "f")]
        (for [ct' (range max-args'-ct)]
          (let [args' (take ct' all-args')
                non-variadic?' (< ct' (dec max-args'-ct)) nv?' non-variadic?']
           `(~(if nv?' `[~f-sym ~@args']
                       `[~f-sym ~@args' ~'& ~&arg'])
              (~'fn ~(gensym "fconj")
              ~@(for [ct (range max-args-ct)]
                  (let [args (take ct all-args)
                        non-variadic? (< ct (dec max-args-ct)) nv? non-variadic?]
                    (if nv?
                       `([~@args] ~(if nv?' `(~f-sym ~@args ~@args')
                                            `(apply ~f-sym ~@args ~@args' ~&arg')))
                       `([~@all-args ~'& ~&arg] ~(if nv?' `(apply ~f-sym (concat (list* ~@all-args ~&arg) (list ~@args')))
                                                          `(apply ~f-sym (concat (list* ~@all-args ~&arg) (list* ~@args' ~&arg'))))))))))))))))

(gen-fconj 8 8)

#?(:clj (defmacro fn0 [  & args] `(fn fn0# [f#  ] (f# ~@args))))
#?(:clj (defmacro fn1 [f & args] `(fn fn1# [arg#] (~f arg# ~@args)))) ; analogous to ->
#?(:clj (defmacro fnl [f & args] `(fn fnl# [arg#] (~f ~@args arg#)))) ; analogous to ->>

; MWA: "Macro WorkAround"
#?(:clj (defmacro MWA ([f] `(fn1 ~f)) ([n f] `(mfn ~n ~f))))

(defn fn-bi [arg] #(arg %1 %2))
(defn unary [pred]
  (fn ([a    ] #(pred % a))
      ([a b  ] #(pred % a b))
      ([a b c] #(pred % a b c))))

#?(:clj
(defmacro fn->
  "Equivalent to |(fn [x] (-> x ~@body))|"
  {:attribution "thebusby.bagotricks"}
  [& body] `(fn fn-># [x#] (-> x# ~@body))))

#?(:clj
(defmacro fn->>
  "Equivalent to |(fn [x] (->> x ~@body))|"
  {:attribution "thebusby.bagotricks"}
  [& body] `(fn fn->># [x#] (->> x# ~@body))))

#?(:clj
(defmacro with-do
  "Like prog1 in Common Lisp, or a `(do)` that returns the first form."
  [expr & exprs] `(let [ret# ~expr] ~@exprs ret#)))

#?(:clj
(defmacro with-do-let
  "Like aprog1 or prog1-bind in Common Lisp."
  [[sym retn] & body] `(let [~sym ~retn] ~@body ~sym)))


; TODO: deprecate these... likely they're not useful
(defn call->  [arg & [func & args]] ((apply func args) arg))
(defn call->> [& [func & args]] ((apply func    (butlast args)) (last args)))

#?(:clj
(defmacro <-
  "Converts a ->> to a ->
   Note: syntax modified from original."
   {:attribution "thebusby.bagotricks"
    :usage       `(->> (range 10) (map inc) (<- doto prn) (reduce +))}
  ([x] `(~x))
  ([op & body] `(~op ~(last body) ~@(butlast body)))))

#?(:clj
(defmacro <<-
  "Converts a -> to a ->>"
   {:attribution "alexandergunnarson"
    :usage       `(-> 1 inc (/ 4) (<<- - 2))}
  ([x] `(~x))
  ([x op & body] `(~op ~@body ~x))))

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

#?(:clj
(defmacro doto->>
  {:usage '(->> 1 inc (doto->> (println "ABC")))}
  [[f & pre-args] obj]
  `(let [obj# ~obj]
     (do (~f ~@pre-args obj#)
         obj#))))

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

#?(:clj (defalias as-> core/as->))

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
  "Creates a reducer-safe function for use with `reduce` or `reduce-kv`."
  [arglist & body]
  (let [sym (gensym "rfn")]
    (case (count arglist)
          1 `(fn ~sym (~arglist ~@body)
                      ([k# v#] (~sym [k# v#])))
          2 `(fn ~sym ([[k# v#]] (~sym k# v#))
                      (~arglist ~@body)
                      ([ret# k# v#] (~sym ret# [k# v#])))
          3 `(fn ~sym ([ret# [k# v#]] (~sym ret# k# v#))
                      (~arglist ~@body))
          (throw (ex-info "Illegal arglist count passed to rfn" {:arglist arglist}))))))
