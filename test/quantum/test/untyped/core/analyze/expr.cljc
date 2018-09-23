(ns quantum.test.core.untyped.analyze.expr
  (:require
    [quantum.core.test                 :as test
      :refer [deftest testing is is= throws]]
    [quantum.core.untyped.analyze.ast  :as ast]
    [quantum.core.untyped.analyze.expr :as self]
    [quantum.core.untyped.type         :as t]))

(deftest test|casef
  (testing "equality"
    (testing "self-equality"
      (is= (self/casef count 1 nil 2 nil)
           (self/casef count 1 nil 2 nil)))
    (testing "different case orders are equal"
      (is= (self/casef count  1  nil  2  nil)
           (self/casef count  2  nil  1  nil))
      (is= (self/casef count "1" nil "2" nil)
           (self/casef count "2" nil "1" nil))))
  (testing "inequality"
    (testing "inequality of different cases"
      (is (not= (self/casef count  1  nil 2 nil)
                (self/casef count "1" nil 2 nil)))))
  (testing "function call"
    (let [dispatch
            (self/casef count
              2 (self/condpf-> t/>= (self/get 0)
                  t/int
                    (self/condpf-> t/>= (self/get 1)
                      t/char?  t/int?
                      t/byte?  t/int?
                      t/short? t/int?
                      t/int?   t/int?
                      t/long?  t/long?)
                  t/short
                    (self/condpf-> t/>= (self/get 1)
                      t/long?  t/long?
                      t/int?   t/int?
                      t/short? t/short?
                      t/char?  t/short?
                      t/byte?  t/short?)
                  t/long
                    (self/condpf-> t/>= (self/get 1)
                      t/long?  t/long?
                      t/int?   t/long?
                      t/short? t/long?
                      t/char?  t/long?
                      t/byte?  t/long?)
                  t/char
                    (self/condpf-> t/>= (self/get 1)
                      t/byte?  t/char?
                      t/long?  t/long?
                      t/char?  t/char?
                      t/short? t/short?
                      t/int?   t/int?)
                  t/byte
                    (self/condpf-> t/>= (self/get 1)
                      t/long?  t/long?
                      t/int?   t/int?
                      t/short? t/short?
                      t/char?  t/char?
                      t/byte?  t/byte?))
              3 (self/condpf-> t/>= (self/get 0)
                  t/char?
                    (self/condpf-> t/>= (self/get 1)
                      t/long?
                        (self/condpf-> t/>= (self/get 2)
                          t/long? t/long?))))]
      (testing "Success"
        (is= (dispatch [t/long? t/long?])
             t/long?)
        (is= (dispatch [t/char? t/long? t/long?])
             t/long?)
        (is= (dispatch [t/char? t/char?])
             t/char?))
      #_(testing "Failure"
        (throws ? (dispatch [t/char])))
      )))
