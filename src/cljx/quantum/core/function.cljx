(ns
  ^{:doc "Useful function-related functions (one could say 'metafunctions').

          Higher-order functions, currying, monoids, reverse comp, arrow macros, inner partials, juxts, etc."
    :attribution "Alex Gunnarson"}
  quantum.core.function
  (:require
    [quantum.core.ns :as ns :refer
      #+clj  [alias-ns defalias]
      #+cljs [Exception IllegalArgumentException
              Nil Bool Num ExactNum Int Decimal Key Vec Set
              ArrList TreeMap LSeq Regex Editable Transient Queue Map]]
    #+clj [clojure.pprint  :as pprint :refer [pprint]]
    [quantum.core.data.map :as map    :refer [sorted-map+]]
    [clojure.walk]
    #+clj [potemkin        :as p])
  #+clj
  (:import
    clojure.core.Vec
    (quantum.core.ns
      Nil Bool Num ExactNum Int Decimal Key Set
             ArrList TreeMap LSeq Regex Editable Transient Queue Map))
  #+clj (:gen-class))

#+clj (ns/require-all *ns* :clj)

; extend-protocol+ doesn't quite work...

#+clj (defalias reify+             p/reify+)
#+clj (defalias defprotocol+       p/defprotocol+)
#+clj (defalias deftype+           p/deftype+)
#+clj (defalias defrecord+         p/defrecord+)
#+clj (defalias definterface+      p/definterface+)
#+clj (defalias extend-protocol+   p/extend-protocol+)

; /memfn/
; (count (filter (memfn isDirectory) files)) => 68
; (count (filter #(.isDirectory %)   files)) => 68
#+clj (defalias jfn memfn)

(defmacro mfn
  "|mfn| is short for 'macro-fn', just as 'jfn' is short for 'java-fn'.
   Originally named |functionize| by mikera."
  {:attribution "mikera, http://stackoverflow.com/questions/9273333/in-clojure-how-to-apply-a-macro-to-a-list/9273560#9273560"}
  [macro]
  `(fn [& args#]
     (eval (cons '~macro args#))))

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


(defmacro memoized-fn
  "Like fn, but memoized (including recursive calls).

   The clojure.core memoize correctly caches recursive calls when you do a top-level def
   of your memoized function, but if you want an anonymous fibonacci function, you must use
   memoized-fn rather than memoize to cache the recursive calls."
  {:attribution "prismatic.plumbing"}
  [name args & body]
  `(let [a# (atom {})]
     (fn ~name ~args
       (let [m# @a#
             args# ~args]
         (if-let [[_# v#] (find m# args#)]
           v#
           (let [v# (do ~@body)]
             (swap! a# assoc args# v#)
             v#))))))
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

(defmacro defcurried
  "Builds another arity of the fn that returns a fn awaiting the last
  param."
  {:attribution "clojure.core.reducers"}
  [name doc meta args & body]
  (do-curried name doc meta args body))

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

(defn compr
  {:todo ["Make more efficient by not using |reverse|."]}
  [& args]
  (apply comp (reverse args)))

(defn fn*
  "FOR SOME REASON '(fn* + 3)' [and the like] FAILS WITH THE FOLLOWING EXCEPTION:
  'CompilerException java.lang.ClassCastException: java.lang.Long cannot be cast to clojure.lang.ISeq'

   Likewise, simply copying and pasting the code for |partial| from clojure.core doesn't work either..."
  [& args]
  (apply partial args))

(defn f*n  [func & args]
  (fn [arg-inner] ; macros to reduce on possible |apply| overhead
    (apply func arg-inner args)))

(defn f**n [func & args]
  (fn [& args-inner]
    (apply func (concat args-inner args))))

(defn *fn [& args] (f*n apply args))

(defn fn-bi [arg] #(arg %1 %2))

(defn unary [pred] (partial f*n pred))

(defmacro fn->
  "Equivalent to |(fn [x] (-> x ~@body))|"
  {:attribution "thebusby.bagotricks"}
  [& body]
  `(fn [x#] (-> x# ~@body)))

(defmacro fn->>
  "Equivalent to |(fn [x] (->> x ~@body))|"
  {:attribution "thebusby.bagotricks"}
  [& body]
  `(fn [x#] (->> x# ~@body)))

(defmacro with->>
  "Applies the function @expr to the expression being last-threaded,
   and returns the latter."
  {:attribution "Alex Gunnarson"}
  [expr & exprs]
  `(do (~expr (butlast ~exprs) (last ~exprs))
       (last ~exprs)))


; TODO: deprecate these... likely they're not useful
(defn call-fn* [& args]          ((apply partial (butlast args)) (last args)))
(defn call-f*n [& args]          ((apply f*n     (butlast args)) (last args)))
(defn call->   [arg & [func & args]] ((apply func args) arg))
(defn call->>  [& [func & args]] ((apply func    (butlast args)) (last args)))

; TODO: Find |<<-| to convert a -> to <<-

(defmacro <-
  "Converts a ->> to a ->
   (->> (range 10) (map inc) (<- doto prn) (reduce +))
   Jason W01fe is happy to give a talk anywhere, any time on
   the calculus of arrow macros.
   Note: syntax modified from original."
   {:attribution "thebusby.bagotricks"}
  ;; [& body] ;; original version
  ;; `(-> ~(last body) ~@(butlast body)) ;; original version
  ([x] `(~x))
  ([cmd & body]
      `(~cmd ~(last body) ~@(butlast body))))

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
      (throw (IllegalArgumentException.
              "juxtm requires an even number of arguments"))))

; (defn juxtc*
;   [map-type args]
;   (if (-> args count even?)
;       (fn [arg]
;         (->> arg
;              (reduce-2 (fn [ret a b] (conj ret (constantly a) b)) [])
;              ((apply juxt args))
;              (apply map-type)))
;       (throw (#+clj  IllegalArgumentException.
;               #+cljs js/Error.
;               "juxtc requires an even number of arguments"))))

(defn juxtc*
  [map-type args]
  (if (-> args count even?)
      (fn [arg]
        (->> arg
             (reduce-2 (fn [ret a b] (conj ret (constantly a) b)) [])
             ((apply juxt args))
             (apply map-type)))
      (throw (IllegalArgumentException.
              "juxtc requires an even number of arguments"))))

(defn juxtm
  "Like /juxt/, but applies a hash-map instead of a vector.
   Requires an even number of arguments."
  [& args]
  (juxtm* hash-map    args))
(defn juxt-sm
  "Like /juxt/, but applies a sorted-map+ instead of a vector.
   Requires an even number of arguments."
  [& args]
  (juxtm* sorted-map+ args))
(defn juxtc-m
  "Like /juxtm/, but each key is wrapped in a /constantly/.
   Basically like /select-keys/."
  [& args]
  (juxtc* hash-map    args))
(defn juxtc-sm
  "Like /juxt-sm/, but each key is wrapped in a /constantly/.
   Basically like /select-keys/."
  [& args]
  (juxtc* sorted-map+ args))

; ======== WITH =========

; TODO: use whatever REPL's print fn is
; (defn with-pr  [obj]      (do (#+clj  pprint
;                                #+cljs println obj) 
;                               obj))
(defn with-pr  [obj]      (do (println obj) 
                              obj))

(defn with-msg [msg  obj] (do (println msg) obj))
(defn with     [expr obj] (do expr          obj))
(defn withf    [func obj] (do (func obj)    obj))

; ========= REDUCER PLUMBING ==========

(defn- do-rfn
  {:attribution "clojure.core.reducers"}
  [f1 k fkv]
  `(fn
     ([] (~f1))
     ~(clojure.walk/postwalk
       #(if (sequential? %)
            ((whenf % vector? vec)
             (remove #{k} %))
            %)
       fkv)
     ~fkv))

(defmacro rfn
  "Builds 3-arity reducing fn given names of wrapped fn and key, and k/v impl."
  {:attribution "clojure.core.reducers"}
  [[f1 k] fkv]
  (do-rfn f1 k fkv))
