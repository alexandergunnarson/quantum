(ns quantum.test.validate.numeric
  (:refer-clojure
    :exclude [doseq])
  (:require
    [quantum.validate.numeric :as ns]
    [quantum.core.error
      :refer [->ex]]
    [quantum.core.test        :as test
      :refer [deftest testing is]]
    [quantum.core.collections :as coll
      :refer [range+, doseq]]))

(deftest test:byte-regex
  (doseq [i (range+ -128 127)] ; ; TODO make these bounds dynamic like byte:max-value
    (when-not (re-matches ns/byte-regex (str i)) (throw (->ex "Test fail" i)))))

(deftest test:short-regex
  (doseq [i (range+ -32768 32767)] ; ; TODO make these bounds dynamic like short:max-value
    (when-not (re-matches ns/short-regex (str i)) (throw (->ex "Test fail" i)))))

(deftest test:int-regex
  (doseq [i (range+ -1000000 1000000)]
    (when-not (re-matches ns/int-regex (str i)) (throw (->ex "Test fail" i)))))

(deftest test:long-regex
  (doseq [i (range+ -1000000 1000000)]
    (when-not (re-matches ns/long-regex (str i)) (throw (->ex "Test fail" i)))))

