(ns quantum.core.cache
  (:refer-clojure :exclude [memoize])
  (:require
    [quantum.core.typed         :as t]
    ;; TODO TYPED excise
    [quantum.untyped.core.cache :as u]
    ;; TODO TYPED excise
    [quantum.untyped.core.vars  :as uvar
      :refer [defaliases]]))

(def delay? (t/isa? #?(:clj clojure.lang.Delay :cljs cljs.core/Delay)))

;; TODO TYPED
(defaliases u
  memoize* memoize #?(:clj defmemoized)
  callable-times
  init! clear!)
