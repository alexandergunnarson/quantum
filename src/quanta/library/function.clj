(ns quanta.library.function
  (:require
    [quanta.library.ns       :as ns     :refer [defalias]]
    [clojure.pprint          :as pprint :refer [pprint]]
    [quanta.library.data.map :as map    :refer [sorted-map+]]
    [potemkin                :as p])
  (:gen-class))
(set! *warn-on-reflection* true)

(defalias reify+             p/reify+)
(defalias defprotocol+       p/defprotocol+)
(defalias deftype+           p/deftype+)
(defalias defrecord+         p/defrecord+)
(defalias definterface+      p/definterface+)
(defalias extend-protocol+   p/extend-protocol+)

(defn- reduce-2 [func init coll] ; not actually implementing of CollReduce... so not as fast...
  (loop [ret init coll-n coll]
    (if (empty? coll-n)
        ret
        (recur (func ret (first coll-n) (second coll-n))
               (-> coll-n rest rest)))))


; /memfn/
; (count (filter (memfn isDirectory) files)) => 68
; (count (filter #(.isDirectory %)   files)) => 68
; https://github.com/technomancy/robert-hooke ; composable, extensible functions
(defalias jfn memfn)

(defmacro mfn
  "|mfn| is short for 'macro-fn', just as 'jfn' is short for 'java-fn'.
   Originally named |functionize| by mikera."
  {:attribution "mikera, http://stackoverflow.com/questions/9273333/in-clojure-how-to-apply-a-macro-to-a-list/9273560#9273560"}
  [macro]
  `(fn [& args#]
     (eval (cons '~macro args#))))

(defn while-recur [obj-0 pred func]
  (loop [obj obj-0]
      (if ((complement pred) obj)
          obj
          (recur (func obj)))))
(defmacro dos [args]
  `(do ~@args))
(defmacro repeatedly-into
  [coll n & body]
  `(let [coll# ~coll
         n#    ~n]
     (if (instance? clojure.lang.IEditableCollection coll#)
       (loop [v# (transient coll#) idx# 0]
         (if (>= idx# n#)
           (persistent! v#)
           (recur (conj! v# ~@body)
                  (inc idx#))))
       (loop [v#   coll#
              idx# 0]
         (if (>= idx# n#) v#
           (recur (conj v# ~@body)
                  (inc idx#)))))))
(defmacro repeatedly+
  "Like /repeatedly/ but (significantly) faster and returns a vector."
  [n & body] `(repeatedly-into* [] ~n ~@body))
; (defn call->> [func & args] ; is butlast and last wise?
;   ((apply func (butlast args)) (last args)))
(defn call
  "Call function `f` with additional arguments."
  ([f]                    (f))
  ([f x]                  (f x))
  ([f x y]                (f x y))
  ([f x y z]              (f x y z))
  ([f x y z & more] (apply f x y z more)))
(defn firsta ; the multiple-arity for some reason gives efficiency
  "Accepts any number of arguments and returns the first."
  ^{:attribution "parkour.reducers"}
  ([x]            x)
  ([x y]          x)
  ([x y z]        x)
  ([x y z & more] x))
(defn seconda ; the multiple-arity for some reason gives efficiency
  "Accepts any number of arguments and returns the second."
  ^{:attribution "parkour.reducers"}
  ([x y]          y)
  ([x y z]        y)
  ([x y z & more] y))
(defmacro memoized-fn
  "Like fn, but memoized (including recursive calls).

   The clojure.core memoize correctly caches recursive calls when you do a top-level def
   of your memoized function, but if you want an anonymous fibonacci function, you must use
   memoized-fn rather than memoize to cache the recursive calls."
   ^{:attribution "prismatic.plumbing"}
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
  ^{:attribution "clojure.core.reducers"}
  [name doc meta args body]
  (let [cargs (vec (butlast args))]
    `(defn ~name ~doc ~meta
       (~cargs (fn [x#] (~name ~@cargs x#)))
       (~args ~@body))))
; Currying: You can specify how many args a function has, and it will curry itself for you until it gets that many.
; The reason this doesn't happen by default in Clojure is that we prefer variadic functions to auto-curried functions, I suppose.
; This /defcurried/  implementation is a bit special-case.
; It only produces the curried version and only curries on the last parameter.
; Why currying? it removes some of the additional code necessary to create multiple arity functions in a general way. 
; You can (defcurried myf [a b c] (+ a b c)) for example and then invoke it like: (myfn 1 2 3) but also ((myfn 1 2) 3) and also ((myfn 1) 2 3).
(defmacro defcurried
  "Builds another arity of the fn that returns a fn awaiting the last
  param."
  ^{:attribution "clojure.core.reducers"}
  [name doc meta args & body]
  (do-curried name doc meta args body))
(defn zeroid [func base] ; is it more efficient to do it differently? ; probably not
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
(defn compr [& args]
  (apply comp (reverse args))) ; is reverse wise?
(defn fn*  [& args]       (apply partial args)) ; apparently defalias causes problems here... or something... ? def, too
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
  "Equivalent to `(fn [x] (-> x ~@body))"
  ^{:attribution "thebusby.bagotricks"}
  [& body]
  `(fn [x#] (-> x# ~@body)))
(defmacro fn->>
  "Equivalent to `(fn [x] (->> x ~@body))"
  ^{:attribution "thebusby.bagotricks"}
  [& body]
  `(fn [x#] (->> x# ~@body)))
(defn call-fn* [& args]          ((apply partial (butlast args)) (last args)))
(defn call-f*n [& args]          ((apply f*n     (butlast args)) (last args)))
(defn call->   [arg & [func & args]] ((apply func args) arg))
(defn call->>  [& [func & args]] ((apply func    (butlast args)) (last args)))

; <<- (converts a -> to <<-)
(defmacro <-
  "Converts a ->> to a ->
   (->> (range 10) (map inc) (<- doto prn) (reduce +))
   Jason W01fe is happy to give a talk anywhere, any time on
   the calculus of arrow macros.
   Note: syntax modified from original."
   ^{:attribution "thebusby.bagotricks"}
  ;; [& body] ;; original version
  ;; `(-> ~(last body) ~@(butlast body)) ;; original version
  ([x] `(~x))
  ([cmd & body]
      `(~cmd ~(last body) ~@(butlast body))))

(defn juxtm*
  [map-type args]
  (if (-> args count even?)
      (fn->> ((apply juxt args)) (apply map-type))
      (throw (IllegalArgumentException. "juxtm requires an even number of arguments"))))
(defn juxtc*
  [map-type args]
  (if (-> args count even?)
      (fn->> (reduce-2 (fn [ret a b] (conj ret (constantly a) b)) [])
             ((apply juxt args))
             (apply map-type))
      (throw (IllegalArgumentException. "juxtc requires an even number of arguments"))))
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

(defn with-pr  [obj]      (do (pprint obj)  obj))
(defn with-msg [msg  obj] (do (println msg) obj))
(defn with     [expr obj] (do expr          obj))
(defn withf    [func obj] (do (func obj)    obj))

(defmacro extend-protocol-for-all [prot classes & body]
  `(doseq [class-n# ~classes]
     (extend-protocol ~prot (eval class-n#) ~@body)))