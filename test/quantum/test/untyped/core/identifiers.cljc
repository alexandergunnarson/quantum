(ns quantum.test.untyped.core.identifiers
          (:require
            [quantum.untyped.core.identifiers    :as this
   #?@(:cljs [:refer [DelimitedIdent]])]
            [quantum.untyped.core.test           :as test
              :refer [deftest testing is is= throws]])
  #?(:clj (:import quantum.untyped.core.identifiers.DelimitedIdent)))

(deftest test|>ident
  (is= (this/>delim-ident "a|b|c|d")                    (DelimitedIdent. ["a" "b" "c" "d"]))

  (is= (this/>delim-ident String)                       (DelimitedIdent. ["java" "lang" "String"]))

  (testing "Symbol"
    (is= (this/>delim-ident 'a)                           (DelimitedIdent. ["a"]))
    (is= (this/>delim-ident 'a/b)                         (DelimitedIdent. ["a" "b"]))
    (is= (this/>delim-ident 'a|b/c)                       (DelimitedIdent. ["a" "b" "c"]))
    (is= (this/>delim-ident 'a|b|c)                       (DelimitedIdent. ["a" "b" "c"]))
    (is= (this/>delim-ident 'a/b|c)                       (DelimitedIdent. ["a" "b" "c"]))
    (is= (this/>delim-ident 'a|b/c|d)                     (DelimitedIdent. ["a" "b" "c" "d"]))
    (is= (this/>delim-ident 'a.b/c.d)                     (DelimitedIdent. ["a" "b" "c" "d"])))

  (testing "Keyword"
    (is= (this/>delim-ident :a)                           (DelimitedIdent. ["a"]))
    (is= (this/>delim-ident :a/b)                         (DelimitedIdent. ["a" "b"]))
    (is= (this/>delim-ident :a|b/c)                       (DelimitedIdent. ["a" "b" "c"]))
    (is= (this/>delim-ident :a|b|c)                       (DelimitedIdent. ["a" "b" "c"]))
    (is= (this/>delim-ident :a/b|c)                       (DelimitedIdent. ["a" "b" "c"]))
    (is= (this/>delim-ident :a|b/c|d)                     (DelimitedIdent. ["a" "b" "c" "d"]))
    (is= (this/>delim-ident :a.b/c.d)                     (DelimitedIdent. ["a" "b" "c" "d"])))

  (is= (this/>delim-ident (find-ns 'quantum.untyped.core.test))
       (DelimitedIdent. ["quantum" "untyped" "core" "test"]))
  (is= (this/>delim-ident #'count)
       (DelimitedIdent. ["clojure" "core" "count"]))
  (is= (this/>delim-ident count)
       (DelimitedIdent. ["clojure" "core" "count"])))
