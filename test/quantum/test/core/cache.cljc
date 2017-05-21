(ns quantum.test.core.cache
  (:refer-clojure :exclude [for])
  (:require
    [quantum.core.async :as async]
    [quantum.core.cache :as ns]
    [quantum.core.collections :as coll
      :refer [for]]
    [quantum.core.test  :as test
      :refer [deftest is testing]]))

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

(deftest test:callable-times ; TODO this is async in CLJS
  (let [calls (atom 0)
        calls-max 5
        callf (ns/callable-times calls-max (fn [] (swap! calls inc)))]
    (#?(:clj identity :cljs async/go)
      (#?(:clj async/seq<!! :cljs async/seq<!)
        (for [i 25] (#?(:clj async/thread :cljs async/go) (callf)))))
    (is (= @calls calls-max))))
