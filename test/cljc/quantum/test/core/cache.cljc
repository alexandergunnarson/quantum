(ns quantum.test.core.cache
  (:require [quantum.core.cache :refer :all]))

#?(:clj
(defn test:memoize*
  ([f])
  ([f m-0 & [memoize-only-first-arg? get-fn-0 assoc-fn-0 memoize-first-n-args]])))

(defn test:memoize [& args])

(defn test:init! [var-])

(defn test:clear! [var-])

#?(:clj
(defmacro test:defmemoized
  [sym opts & args]))