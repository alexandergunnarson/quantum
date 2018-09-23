(ns quantum.test.untyped.core.identifiers
          (:require
            [quantum.untyped.core.identifiers    :as self
   #?@(:cljs [:refer [DelimitedIdent]])]
            [quantum.untyped.core.test           :as test
              :refer [deftest testing is is= throws]])
  #?(:clj (:import quantum.untyped.core.identifiers.DelimitedIdent)))

(deftest test|>ident
  (is= (self/>delim-ident "a|b|c|d")                    (DelimitedIdent. ["a" "b" "c" "d"]))

  (is= (self/>delim-ident String)                       (DelimitedIdent. ["java" "lang" "String"]))

  (testing "Symbol"
    (is= (self/>delim-ident 'a)                           (DelimitedIdent. ["a"]))
    (is= (self/>delim-ident 'a/b)                         (DelimitedIdent. ["a" "b"]))
    (is= (self/>delim-ident 'a|b/c)                       (DelimitedIdent. ["a" "b" "c"]))
    (is= (self/>delim-ident 'a|b|c)                       (DelimitedIdent. ["a" "b" "c"]))
    (is= (self/>delim-ident 'a/b|c)                       (DelimitedIdent. ["a" "b" "c"]))
    (is= (self/>delim-ident 'a|b/c|d)                     (DelimitedIdent. ["a" "b" "c" "d"]))
    (is= (self/>delim-ident 'a.b/c.d)                     (DelimitedIdent. ["a" "b" "c" "d"])))

  (testing "Keyword"
    (is= (self/>delim-ident :a)                           (DelimitedIdent. ["a"]))
    (is= (self/>delim-ident :a/b)                         (DelimitedIdent. ["a" "b"]))
    (is= (self/>delim-ident :a|b/c)                       (DelimitedIdent. ["a" "b" "c"]))
    (is= (self/>delim-ident :a|b|c)                       (DelimitedIdent. ["a" "b" "c"]))
    (is= (self/>delim-ident :a/b|c)                       (DelimitedIdent. ["a" "b" "c"]))
    (is= (self/>delim-ident :a|b/c|d)                     (DelimitedIdent. ["a" "b" "c" "d"]))
    (is= (self/>delim-ident :a.b/c.d)                     (DelimitedIdent. ["a" "b" "c" "d"])))

  (is= (self/>delim-ident (find-ns 'quantum.untyped.core.test))
       (DelimitedIdent. ["quantum" "untyped" "core" "test"]))
  (is= (self/>delim-ident #'count)
       (DelimitedIdent. ["clojure" "core" "count"]))
  (is= (self/>delim-ident count)
       (DelimitedIdent. ["clojure" "core" "count"])))
