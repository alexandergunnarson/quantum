(ns quantum.untyped.core.refs
  (:refer-clojure :exclude
    [get set])
  (:require
    [quantum.untyped.core.core :as ucore]))

(ucore/log-this-ns)

(defn atom? [x]
  #?(:clj  (instance?  clojure.lang.IAtom x)
     :cljs (satisfies? cljs.core/IAtom    x)))

(defn derefable? [x]
  #?(:clj  (instance?  clojure.lang.IDeref x)
     :cljs (satisfies? cljs.core/IDeref    x)))

(defn ?deref [x] (if (derefable? x) @x x))

(defprotocol PMutableReference
  (get       [this])
  (set!      [this v])
  (getAndSet [this v]))

#?(:clj
(extend-protocol PMutableReference
  ThreadLocal
    (get       [this] (.get this))
    (set!      [this v] (.set this v) v)
    (getAndSet [this v] (let [v-prev (.get this)] (.set this v) v-prev))))

(defn update!
  "A nonatomic update."
  [x f]
  (quantum.untyped.core.refs/set! x (f (get x)))
  x)

;; ===== Unsynchronized mutability ===== ;;

;; TODO create for every primitive datatype as well
(deftype MutableReference [#?(:clj ^:unsynchronized-mutable val :cljs ^:mutable val)]
  PMutableReference
  (get       [this] val)
  (set!      [this v] (set! val v) val)
  (getAndSet [this v] (let [v-prev val] (set! val v) v-prev))
  #?(:clj  clojure.lang.IDeref
     :cljs cljs.core/IDeref)
  (#?(:clj deref :cljs -deref) [this] val))

(defn ! [x] (MutableReference. x))

;; ===== Thread-local mutability ===== ;;

(defn >!thread-local #_> #_(t/isa? PMutableReference)
  ([]  #?(:clj  (ThreadLocal.)
          :cljs (MutableReference. nil)))
  ([x] #?(:clj  (doto (ThreadLocal.) (quantum.untyped.core.refs/set! x))
          :cljs (MutableReference. x))))
