(ns quantum.test.core.untyped.convert
  (:require
    [quantum.core.test            :as test
      :refer [deftest testing is is= throws]]
    [quantum.core.untyped.convert :as this]
    [quantum.core.untyped.qualify
      #?@(:cljs [:refer [Ident]])])
  #?(:clj (:import quantum.core.untyped.qualify.Ident)))

(deftest test|>ident
  (is= (this/>ident "a|b|c|d")                    (Ident. ["a" "b" "c" "d"]))

  (is= (this/>ident String)                       (Ident. ["java" "lang" "String"]))

  (testing "Symbol"
    (is= (this/>ident 'a)                           (Ident. ["a"]))
    (is= (this/>ident 'a/b)                         (Ident. ["a" "b"]))
    (is= (this/>ident 'a|b/c)                       (Ident. ["a" "b" "c"]))
    (is= (this/>ident 'a|b|c)                       (Ident. ["a" "b" "c"]))
    (is= (this/>ident 'a/b|c)                       (Ident. ["a" "b" "c"]))
    (is= (this/>ident 'a|b/c|d)                     (Ident. ["a" "b" "c" "d"]))
    (is= (this/>ident 'a.b/c.d)                     (Ident. ["a" "b" "c" "d"])))

  (testing "Keyword"
    (is= (this/>ident :a)                           (Ident. ["a"]))
    (is= (this/>ident :a/b)                         (Ident. ["a" "b"]))
    (is= (this/>ident :a|b/c)                       (Ident. ["a" "b" "c"]))
    (is= (this/>ident :a|b|c)                       (Ident. ["a" "b" "c"]))
    (is= (this/>ident :a/b|c)                       (Ident. ["a" "b" "c"]))
    (is= (this/>ident :a|b/c|d)                     (Ident. ["a" "b" "c" "d"]))
    (is= (this/>ident :a.b/c.d)                     (Ident. ["a" "b" "c" "d"])))

  (is= (this/>ident (find-ns 'quantum.core.test)) (Ident. ["quantum" "core" "test"]))

  (is= (this/>ident #'count)                      (Ident. ["clojure" "core" "count"]))

  (is= (this/>ident count)                        (Ident. ["clojure" "core" "count"])))
