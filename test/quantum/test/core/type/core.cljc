(ns quantum.test.core.type.core
  (:require
    [quantum.core.type.core :as this]
    [quantum.core.test
      :refer [deftest is testing]]))

#?(:clj
(deftest test|nth-elem-type|clj
  (is (= "[D"     (this/nth-elem-type|clj "[D" 0)))
  (is (= 'double  (this/nth-elem-type|clj "[D" 1)))
  (is (= 'long    (this/nth-elem-type|clj "[J" 1)))
  (is (= "[Z"     (this/nth-elem-type|clj "[[Z" 1)))
  (is (= 'boolean (this/nth-elem-type|clj "[[Z" 2)))
  (is (= "[Ljava.lang.Object;" (this/nth-elem-type|clj "[[Ljava.lang.Object;" 1)))
  (is (= 'java.lang.Object (this/nth-elem-type|clj "[Ljava.lang.Object;" 1)))
  (is (thrown? Throwable (this/nth-elem-type|clj "[[Z" 3)))
  (is (thrown? Throwable (this/nth-elem-type|clj 'boolean 0)))
  (is (thrown? Throwable (this/nth-elem-type|clj Boolean 0)))
  (is (thrown? Throwable (this/nth-elem-type|clj "Boolean" 0)))))
