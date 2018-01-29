(ns quantum.untyped.core.refs
  (:require
    [quantum.untyped.core.core :as ucore])
  #?(:clj (:import [clojure.lang IDeref IAtom])))

(ucore/log-this-ns)

(defn atom?      [x] (#?(:clj instance? :cljs satisfies?) IAtom x))

(defn derefable? [x] (#?(:clj instance? :cljs satisfies?) IDeref x))

(defn ?deref [x] (if (derefable? x) @x x))
