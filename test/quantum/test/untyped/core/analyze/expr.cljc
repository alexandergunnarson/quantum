(ns quantum.test.core.untyped.analyze.expr
  (:require
    [quantum.core.test                 :as test
      :refer [deftest testing is is= throws]]
    [quantum.core.untyped.analyze.ast  :as ast]
    [quantum.core.untyped.analyze.expr :as this]
    [quantum.core.untyped.type         :as t]))

(deftest test|casef
  (testing "equality"
    (testing "self-equality"
      (is= (this/casef count 1 nil 2 nil)
           (this/casef count 1 nil 2 nil)))
    (testing "different case orders are equal"
      (is= (this/casef count  1  nil  2  nil)
           (this/casef count  2  nil  1  nil))
      (is= (this/casef count "1" nil "2" nil)
           (this/casef count "2" nil "1" nil))))
  (testing "inequality"
    (testing "inequality of different cases"
      (is (not= (this/casef count  1  nil 2 nil)
                (this/casef count "1" nil 2 nil)))))
  (testing "function call"
    (let [dispatch
            (this/casef count
              2 (this/condpf-> t/>= (this/get 0)
                  t/int
                    (this/condpf-> t/>= (this/get 1)
                      t/char?  t/int?
                      t/byte?  t/int?
                      t/short? t/int?
                      t/int?   t/int?
                      t/long?  t/long?)
                  t/short
                    (this/condpf-> t/>= (this/get 1)
                      t/long?  t/long?
                      t/int?   t/int?
                      t/short? t/short?
                      t/char?  t/short?
                      t/byte?  t/short?)
                  t/long
                    (this/condpf-> t/>= (this/get 1)
                      t/long?  t/long?
                      t/int?   t/long?
                      t/short? t/long?
                      t/char?  t/long?
                      t/byte?  t/long?)
                  t/char
                    (this/condpf-> t/>= (this/get 1)
                      t/byte?  t/char?
                      t/long?  t/long?
                      t/char?  t/char?
                      t/short? t/short?
                      t/int?   t/int?)
                  t/byte
                    (this/condpf-> t/>= (this/get 1)
                      t/long?  t/long?
                      t/int?   t/int?
                      t/short? t/short?
                      t/char?  t/char?
                      t/byte?  t/byte?))
              3 (this/condpf-> t/>= (this/get 0)
                  t/char?
                    (this/condpf-> t/>= (this/get 1)
                      t/long?
                        (this/condpf-> t/>= (this/get 2)
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
