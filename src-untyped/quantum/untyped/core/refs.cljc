(ns quantum.untyped.core.refs
  #?(:clj (:import [clojure.lang IDeref IAtom])))

(defn atom?      [x] (#?(:clj instance? :cljs satisfies?) IAtom x))

(defn derefable? [x] (#?(:clj instance? :cljs satisfies?) IDeref x))

(defn ?deref [x] (if (derefable? x) @x x))
