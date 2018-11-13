(ns quantum.core.fn
  "Function-related functions ('metafunctions').

   Higher-order functions, currying, monoids, reversed comp, arrow macros, inner partials, juxts, etc."
  (:refer-clojure :exclude
    [comp constantly, as->, identity, trampoline])
  (:require
    [clojure.core                :as core]
    [clojure.walk]
    [quantum.core.type           :as t]
    [quantum.untyped.core.form.evaluate
      :refer [case-env compile-if]]
    [quantum.untyped.core.form.generate
      :refer [arity-builder max-positional-arity unify-gensyms]]
    [quantum.untyped.core.fn     :as u]
    [quantum.untyped.core.print  :as upr]
    [quantum.untyped.core.vars   :as uvar
      :refer [defalias defaliases]])
#?(:cljs
  (:require-macros
    [quantum.core.fn :as self
      :refer [aritoid gen-constantly gen-call gen-positional-nthas
              gen-ntha gen-conja gen-reversea gen-mapa]])))

;; TODO TYPED move to `data.fn`?
(def multimethod? (t/isa? #?(:clj clojure.lang.MultiFn :cljs cljs.core/IMultiFn)))

;; TODO TYPED `t/==`
(t/defn ^:inline identity [x t/any? #_> #_(t/== x)] x)

;; ===== `fn<i>`: Positional functions ===== ;;

#?(:clj (defaliases u fn0 fn1 fnl))

;; ===== `fn&`: Partial functions ===== ;;

#?(:clj (defaliases u fn&* fn& fn&0 fn&1 fn&2 fn&3))

;; ===== `fn'`: Fixed/constant functions ===== ;;

(defaliases u fn' constantly #?@(:clj [fn'*|arities fn'*]))

;; ===== `comp`: Compositional functions ===== ;;

(defaliases u comp #?(:clj rcomp))

;; ===== Common fixed-function values ===== ;;

(defaliases u fn-nil fn-false fn-true)

#?(:clj (defalias jfn memfn)) ; `Java fn`

#?(:clj
(defmacro mfn
  "`mfn` is short for 'macro-fn', just as 'jfn' is short for 'java-fn'.
   Originally named `functionize` by mikera."
  ([macro-sym]
    (case-env :cljs (throw (ex-info "`mfn` not supported for CLJS." {}))
      `(fn [& args#]
         (upr/js-println "WARNING: Runtime eval with `mfn` via" '~macro-sym)
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

(defaliases u
  ntha
  ntha-0 firsta
  ntha-1 seconda
  ntha-2 thirda
                          ntha-3  ntha-4  ntha-5  ntha-6  ntha-7  ntha-8 ntha-9
  ntha-10 ntha-11 ntha-12 ntha-13 ntha-14 ntha-15 ntha-16 ntha-17)

#?(:clj
(defmacro gen-conja
  "Generates the `conja` function."
  [max-args'-ct max-args-ct]
 `(~'defn ~'conja
    "`conj`es the arguments to the parameters with which `f` will be called,
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
    ; TODO use arity builder
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

(gen-conja 8 8)

#?(:clj
(defmacro gen-reversea []
  (unify-gensyms
   `(~'defn ~'reversea
      "Returns an fn that reverses the arguments of the function passed to `reversed`."
      {:attribution "alexandergunnarson"}
      [f##]
      (fn ~@(arity-builder (fn [args] `(f## ~@(reverse args)))
                           (fn [args vargs] `(apply f## (reverse ~vargs) ~@(reverse args)))
                           0 18 (fn [_] "x")))))))

(gen-reversea)

#?(:clj
(defmacro gen-mapa []
  `(~'defn ~'mapa
    "`map`s (i.e. calls) the passed functions on the arguments that will eventually be
     passed to `f`.
     The mapping functions will only be mapped to arguments that are actually passed.
     Too few or too many arguments means that not all mapping functions will be called."
    {:usage '{((mapa (fn [a b] {a b}) name (rcomp name symbol)) :a :b)
              {"a" 'b}
              ((mapa + identity first) 6 [3 4])
              9}}
    ~@(unify-gensyms
        (let [take-drop
               (fn [args fs]
                 [(->> args (take (count fs))
                            (map-indexed (fn [i a] `(~(nth fs i) ~a))))
                  (->> args (drop (count fs)))])
              apply-remaining-fs-to-varargs
                (fn [args vargs fs & [fs&]]
                  `(map-indexed
                     (fn [vi## va##]
                       (case (long vi##)
                          ~@(->> fs (drop (count args))
                                    (map-indexed (fn [i:f f] [i:f `(~f va##)]))
                                    (apply concat))
                          ~(if-not fs&
                             `(do va##)
                             `(let [i:fs&# (- vi## ~(- (count fs) (count args)))]
                                (if (< i:fs&# (count ~fs&))
                                    ((get ~fs& i:fs&#) va##)
                                    va##)))))
                     ~vargs))
              handle-positionals
                (fn [f fs]
                  (fn [args] (let [[takes drops] (take-drop args fs)]
                              `(~f ~@takes ~@drops))))
              handle-varargs
                (fn [f fs & [fs&]]
                  (fn [args vargs]
                    (let [[takes drops] (take-drop args fs)]
                     `(apply ~f ~@takes ~@drops
                                ~(if (<= (count fs) (count args))
                                     vargs
                                     (apply-remaining-fs-to-varargs args vargs fs fs&))))))]
          (arity-builder
            (fn [[f & fs]]
              (if (empty? fs)
                  f
                  `(fn ~@(arity-builder
                           (handle-positionals f fs)
                           (handle-varargs     f fs)
                           0 6))))
            (fn [[f & fs] fs&]
             `(fn ~@(arity-builder
                      (handle-positionals f fs)
                      (handle-varargs f fs fs&)
                      0 6)))
            1 6
            (fn [i] (if (= i 0) "f-" (str "f" (dec i) "-")))))))))

(gen-mapa)
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

;; TODO finish and generalize based off `u/aritoid`
(t/defn ^:inline aritoid
  ;; TODO use `arity-builder`
  "Combines fns as arity-callers."
  {:equivalent `{(aritoid vector identity conj)
                 (fn ([]      (vector))
                     ([x0]    (identity x0))
                     ([x0 x1] (conj x0 x1)))}}
  ([f0 (t/ftype [])] f0)
  ([f0 (t/ftype [])
    f1 (t/ftype [t/any?])]
    (t/fn {:inline true}
          ([]                    (f0))
          ([x0 (t/type-of f1 0)] (f1 x0))))
  ([f0 (t/ftype [])
    f1 (t/ftype [t/any?])
    f2 (t/ftype [t/any? t/any?])]
    (t/fn {:inline true}
          ([]                    (f0))
          ([x0 (t/type-of f1 0)] (f1 x0))
          ([x0 (t/type-of f1 0)
            x1 (t/type-of f1 1)] (f2 x0 x1))))
  ([f0 (t/ftype [])
    f1 (t/ftype [t/any?])
    f2 (t/ftype [t/any? t/any?])
    f3 (t/ftype [t/any? t/any? t/any?])]
    (t/fn {:inline true}
          ([]                    (f0))
          ([x0 (t/type-of f1 0)] (f1 x0))
          ([x0 (t/type-of f1 0)
            x1 (t/type-of f1 1)] (f2 x0 x1))
          ([x0 (t/type-of f2 0)
            x1 (t/type-of f2 1)
            x2 (t/type-of f2 2)] (f3 x0 x1 x2)))))

(defn rf-fix
  "TODO remove when you figure out transduce vs. reduce"
  [f] (aritoid nil identity f))

; MWA: "Macro WorkAround"
#?(:clj (defmacro MWA ([f] `(fn1 ~f)) ([n f] `(mfn ~n ~f))))

(defn fn-bi [arg] #(arg %1 %2))
(defn unary [pred]
  (fn ([a    ] #(pred % a))
      ([a b  ] #(pred % a b))
      ([a b c] #(pred % a b c))))

;; ===== Arrow macros and functions ===== ;;

#?(:clj (defaliases u <- <<- fn-> fn->>))

;; ===== For side effects ===== ;;

#?(:clj (defaliases u with-do with-do-let))

;; ===== ... ===== ;;

; TODO: deprecate these... likely they're not useful
(defn call->  [arg & [func & args]] ((apply func args) arg))
(defn call->> [& [func & args]] ((apply func    (butlast args)) (last args)))

; ---------------------------------------
; ================ JUXTS ================ (deprecate these)
; ---------------------------------------
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

(defalias trampoline core/trampoline)

;; ===== Miscellaneous ===== ;;

(defalias u/?)
