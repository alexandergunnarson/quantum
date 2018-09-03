(ns quantum.untyped.core.refs
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
