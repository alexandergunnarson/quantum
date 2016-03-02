(ns quantum.core.type.predicates
  (:refer-clojure :exclude [#?(:clj boolean?) #?(:cljs seqable?)])
  (:require-quantum [:core fn logic])
  (:require [quantum.core.core :as c]))

(defalias atom?     c/atom?    )
(defalias seqable?  c/seqable? )
(defalias boolean?  c/boolean? )
(defalias editable? c/editable?)

(defn regex? [obj]
  #?(:clj  (instance? java.util.regex.Pattern obj)
     :cljs (instance? js/RegExp               obj)))

(defn derefable? [obj]
  (satisfies? #?(:clj clojure.lang.IDeref :cljs core/IDeref) obj))

(def map-entry?  #?(:clj  (partial instance? clojure.lang.MapEntry)
                    :cljs (fn-and vector? (fn-> count (= 2)))))

(defn listy? [obj] (seq? obj)
  #_(->> obj class
         (contains? (get types 'listy?))))
