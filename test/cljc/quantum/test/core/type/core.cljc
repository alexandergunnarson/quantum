(ns quantum.test.core.type.core
  (:require
    [quantum.core.type.core :as ns]
    [quantum.core.test
      :refer [deftest is testing]]))

#?(:clj
(deftest test:nth-elem-type:clj
  (is (= "[D"     (ns/nth-elem-type:clj "[D" 0)))
  (is (= 'double  (ns/nth-elem-type:clj "[D" 1)))
  (is (= 'long    (ns/nth-elem-type:clj "[J" 1)))
  (is (= "[Z"     (ns/nth-elem-type:clj "[[Z" 1)))
  (is (= 'boolean (ns/nth-elem-type:clj "[[Z" 2)))
  (is (= "[Ljava.lang.Object;" (ns/nth-elem-type:clj "[[Ljava.lang.Object;" 1)))
  (is (= 'java.lang.Object (ns/nth-elem-type:clj "[Ljava.lang.Object;" 1)))
  (is (thrown? Throwable (ns/nth-elem-type:clj "[[Z" 3)))
  (is (thrown? Throwable (ns/nth-elem-type:clj 'boolean 0)))
  (is (thrown? Throwable (ns/nth-elem-type:clj Boolean 0)))
  (is (thrown? Throwable (ns/nth-elem-type:clj "Boolean" 0)))))
