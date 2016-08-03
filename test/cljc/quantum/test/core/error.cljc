(ns quantum.test.core.error
  (:require
    [quantum.core.error :as ns
      :include-macros true]
    [#?(:clj clojure.test
        :cljs cljs.test)
      :refer        [#?@(:clj [deftest is testing])]
      :refer-macros [deftest is testing]]))

(defn test:generic-error [env])

(defn test:error? [x])

(defn test:->err
  ([type]         )
  ([type msg]     )
  ([type msg objs]))

(defn test:->ex
  ([type]         )
  ([type msg]     )
  ([type msg objs]))

(defn test:ex->map
  [e])

(defn test:throw-unless
  ([expr throw-content])
  ([expr1 expr2 & exprs]))

(defn test:throw-when
  [expr throw-content])

(defn test:with-catch
  [handler try-val])

(defn test:with-assert [expr pred err])

(defn test:assert
  [expr & [syms type]])

(defn test:validate [pred expr])

(defn test:try-or 
  ([exp & alternatives]))

(deftest test:suppress
  (is (instance? #?(:clj Exception :cljs js/Error)
        (ns/suppress (throw (#?(:clj Exception. :cljs js/Error.))))))
  (is (= "abcde" (ns/suppress "abcde")))
  (is (= "abcde" (ns/suppress (throw (ns/->ex :abcde)) "abcde")))
  (is (= "abcde" (ns/suppress (throw (ns/->ex :abcde)) (fn [_] "abcde")))))

(defn test:assertf-> [f arg throw-obj])

(defn test:assertf->> [f throw-obj arg])

(defn test:try-times [max-n sleep-millis & body])

(defn test:warn! [e])

(defn test:todo [])