(ns quantum.core.untyped.type
  (:refer-clojure :exclude
    [def
     < <= = >= >
     and or
     boolean byte char short int long float double
     nil?])
  (:require
    [clojure.core :as c]
    [quantum.core.error :as err
      :refer [->ex TODO]]
    [quantum.core.fn    :as fn]
    [quantum.core.vars  :as var
      :refer [defalias]]))

#_(defmacro ->
  ("Anything that is coercible to x"
    [x]
    ...)
  ("Anything satisfying `from` that is coercible to `to`.
    Will be coerced to `to`."
    [from to]))

#_(defmacro range-of)

#_(defn instance? [])

(do

(definterface ISpec)

(deftype ClassSpec [^Class c ^clojure.lang.Symbol name #_(t/? t/symbol?)]
  ISpec
  fipp.ednize/IOverride
  fipp.ednize/IEdn
    (-edn [this] (c/or name (list `isa? c))))

(deftype NilableSpec [x]
  ISpec)

(deftype QMark []
  clojure.lang.IFn
  (invoke [x] (NilableSpec. x)))

(def ^{:doc "Arity 1: Denotes type inference should be performed.
             Arity 2: Denotes a nilable value."}
  ? (QMark.))

(deftype FnSpec [name #_(t/? t/symbol?), ^clojure.lang.Fn f, form #_(t/? form?)]
  ISpec
  fipp.ednize/IOverride
  fipp.ednize/IEdn
    (-edn [this] (c/or name form (list 't/fn-spec f))))

(defn ^ISpec ->spec
  "Coerces ->`x` to a spec, recording its ->`name-sym` if provided."
  ([x] (->spec x nil))
  ([x name-sym]
    (cond (instance? ISpec x)
            x ; TODO should add in its name?
          (class? x)
            (ClassSpec. ^Class x name-sym)
          (fn? x)
            (FnSpec. name-sym ^clojure.lang.Fn x nil)
          :else
            (throw (->ex "Cannot coerce to spec" {:x x :type (type x) :name name-sym})))))

(deftype AndSpec [args #_(t/and t/indexed? (t/seq-of spec?))]
  ISpec
  fipp.ednize/IOverride
  fipp.ednize/IEdn (-edn [this] (list* `and args)))

(defn and [& args]
  (AndSpec. (mapv ->spec args)))

(deftype OrSpec [args #_(t/and t/indexed? (t/seq-of spec?))]
  ISpec
  fipp.ednize/IOverride
  fipp.ednize/IEdn (-edn [this] (println *ns*) (list* `or args)))

(defn or [& args]
  (OrSpec. (mapv ->spec args)))

#?(:clj
(defmacro spec
  "Creates a spec function"
  [arglist & body] ; TODO spec this
  `(FnSpec. nil (fn ~arglist ~@body) (list* `spec '~arglist '~body))))

(deftype FnConstantlySpec
  [name #_(t/? t/symbol?), ^clojure.lang.Fn f, inner-object #_t/_]
  ISpec
  fipp.ednize/IOverride
  fipp.ednize/IEdn
    (-edn [this] (c/or name (list `fn' inner-object))))

#?(:clj
(defmacro fn' [x]
  `(let [x# ~x] (FnConstantlySpec. nil (fn/fn' x#) x#))))

;; ===== DEFINITIONS ===== ;;

(defmacro def [sym specable]
  `(~'def ~sym (->spec ~specable '~(var/qualify *ns* sym))))

(defalias -def def)

(-def boolean Boolean/TYPE  )
(-def byte    Byte/TYPE     )
(-def char    Character/TYPE)
(-def short   Short/TYPE    )
(-def int     Integer/TYPE  )
(-def long    Long/TYPE     )
(-def float   Float/TYPE    )
(-def double  Double/TYPE   )

(-def nil?    c/nil?        )

#_(t/def ::literal (t/or t/nil? t/symbol? t/keyword? t/string? t/long t/double t/tagged-literal?))
#_(t/def ::form    (t/or ::literal t/list? t/vector? ...))

;; ===== SPEC EXTENSIONALITY ===== ;;

(defn <
  "Computes whether the extension of spec ->`s0` is a strict subset of that of ->`s1`."
  [s0 s1] (TODO))

(defn <=
  "Computes whether the extension of spec ->`s0` is a (lax) subset of that of ->`s1`."
  [s0 s1] (TODO))

(defn =
  "Computes whether the extension of spec ->`s0` is equal to that of ->`s1`."
  [s0 s1] (TODO))

(defn >=
  "Computes whether the extension of spec ->`s0` is a (lax) superset of that of ->`s1`."
  [s0 s1] (TODO))

(defn >
  "Computes whether the extension of spec ->`s0` is a strict superset of that of ->`s1`."
  [s0 s1] (TODO))

;; ===== SPEC INTENSIONALITY ===== ;;

(defn in<
  "Computes whether the intension of spec ->`s0` is a strict subset of that of ->`s1`."
  [s0 s1] (TODO))

(defn in<=
  "Computes whether the intension of spec ->`s0` is a (lax) subset of that of ->`s1`."
  [s0 s1] (TODO))

(defn in=
  "Computes whether the intension of spec ->`s0` is equal to that of ->`s1`."
  [s0 s1] (TODO))

(defn in>=
  "Computes whether the intension of spec ->`s0` is a (lax) superset of that of ->`s1`."
  [s0 s1] (TODO))

(defn in>
  "Computes whether the intension of spec ->`s0` is a strict superset of that of ->`s1`."
  [s0 s1] (TODO))

)
