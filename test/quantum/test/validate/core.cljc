(ns quantum.test.validate.core
  (:require
    [#?(:clj clojure.test
        :cljs cljs.test)
      :refer        [#?@(:clj [deftest is testing])]
      :refer-macros [deftest is testing]]
    [quantum.validate.core :as ns]))
