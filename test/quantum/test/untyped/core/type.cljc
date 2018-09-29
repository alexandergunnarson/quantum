(ns quantum.test.untyped.core.type
        (:refer-clojure :exclude
          [boolean? char? double? float? int? string?])
        (:require
          [clojure.core                               :as core]
          [quantum.untyped.core.test
            :refer [deftest testing is is= throws]]
          [quantum.untyped.core.type                  :as t
            :refer [& | !]]
          [quantum.untyped.core.type.reifications     :as utr
 #?@(:cljs [:refer [UniversalSetType EmptySetType
                    NotType OrType AndType
                    ProtocolType ClassType
                    ValueType]])])
#?(:clj (:import
          [quantum.untyped.core.type.reifications
             UniversalSetType EmptySetType
             NotType OrType AndType
             ProtocolType ClassType
             ValueType])))

;; ===== Type predicates ===== ;;
;; Declared here instead of in `quantum.untyped.core.type` to avoid dependency

#?(:clj (def boolean?   (t/isa? #?(:clj Boolean :cljs js/Boolean))))
#?(:clj (def byte?      (t/isa? Byte)))
#?(:clj (def short?     (t/isa? Short)))
#?(:clj (def char?      (t/isa? Character)))
#?(:clj (def int?       (t/isa? Integer)))
#?(:clj (def long?      (t/isa? Long)))
#?(:clj (def float?     (t/isa? Float)))
        (def double?    (t/isa? #?(:clj Double :cljs js/Number)))

        (def primitive? (t/or boolean? #?@(:clj [byte? short? char? int? long? float?]) double?))

#?(:clj (def char-seq?  (t/isa? CharSequence)))
        (def string?    (t/isa? #?(:clj String :cljs js/String)))

;; ----- Example interface hierarchy ----- ;;

(do

(gen-interface :name i.>a+b)
(gen-interface :name i.>a0)
(gen-interface :name i.>a1)
(gen-interface :name i.>b0)
(gen-interface :name i.>b1)

(gen-interface :name i.a    :extends [i.>a0 i.>a1 i.>a+b])
(gen-interface :name i.b    :extends [i.>b0 i.>b1 i.>a+b])

(gen-interface :name i.<a+b :extends [i.a i.b])
(gen-interface :name i.<a0  :extends [i.a])
(gen-interface :name i.<a1  :extends [i.a])
(gen-interface :name i.<b0  :extends [i.b])
(gen-interface :name i.<b1  :extends [i.b])

(gen-interface :name i.><0)
(gen-interface :name i.><1)
(gen-interface :name i.><2)

(def i|>a+b  (t/isa? i.>a+b))
(def i|>a0   (t/isa? i.>a0))
(def i|>a1   (t/isa? i.>a1))
(def i|>b0   (t/isa? i.>b0))
(def i|>b1   (t/isa? i.>b1))
(def i|a     (t/isa? i.a))
(def i|b     (t/isa? i.b))
(def i|<a+b  (t/isa? i.<a+b))
(def i|<a0   (t/isa? i.<a0))
(def i|<a1   (t/isa? i.<a1))
(def i|<b0   (t/isa? i.<b0))
(def i|<b1   (t/isa? i.<b1))
(def i|><0   (t/isa? i.><0))
(def i|><1   (t/isa? i.><1))
(def i|><2   (t/isa? i.><2))

)

;; ----- Hierarchy within existing non-interfaces ----- ;;

(do (def >a+b (t/isa? java.util.AbstractCollection))
    (def >a   (t/isa? java.util.AbstractList))
    (def >b   (t/isa? java.util.AbstractSet))
    (def a    (t/isa? java.util.ArrayList))
    (def b    (t/isa? java.util.HashSet))
    (def <a0  (t/isa? javax.management.AttributeList))
    (def <a1  (t/isa? javax.management.relation.RoleList))
    (def <b0  (t/isa? java.util.LinkedHashSet))
    (def <b1  (t/isa? javax.print.attribute.standard.JobStateReasons))
    (def ><0  byte?)
    (def ><1  short?)
    (def ><2  long?))

(def Uc (t/isa? java.lang.Object))

;; ----- Example protocols ----- ;;

(do

(defprotocol AProtocolAll (a-protocol-all [this]))

(extend-protocol AProtocolAll
  nil    (a-protocol-all [this])
  Object (a-protocol-all [this]))

(defprotocol AProtocolString (a-protocol-string [this]))

(extend-protocol AProtocolString
  java.lang.String (a-protocol-string [this]))

(defprotocol AProtocolNonNil (a-protocol-non-nil [this]))

(extend-protocol AProtocolNonNil
  Object (a-protocol-non-nil [this]))

(defprotocol AProtocolOnlyNil (a-protocol-only-nil [this]))

(extend-protocol AProtocolOnlyNil
  nil (a-protocol-only-nil [this]))

(defprotocol AProtocolNone (a-protocol-none [this]))

(def protocol-types
  (->> [AProtocolAll AProtocolString AProtocolNonNil AProtocolOnlyNil AProtocolNone]
       (map t/>type) set))

)


(def C java.util.AbstractCollection) ; concrete class
(def A java.util.AbstractCollection) ; abstract class
(def I Comparable) ; interface
(def P AProtocolAll) ; protocol

;; ===== End type predicates ===== ;;

(defn- test-equality [genf]
  (let [a (genf) b (genf)]
          (testing "structural equality (`c/=`)"
            (is= a b))
          (testing "hash(eq) equality"
            (is= (hash a) (hash b)))
  #?(:clj (testing "hash(code) equality"
            (is= (.hashCode a) (.hashCode b))))
          (testing "collection equality"
            (is= 1 (count (hash-set a b))))))

(defn- gen-meta [] {(rand) (rand)})

(deftest test|universal-set
  (test-equality #(UniversalSetType. nil))
  (test-equality #(UniversalSetType. (gen-meta))))

(deftest test|empty-set
  (test-equality #(EmptySetType. nil))
  (test-equality #(EmptySetType. (gen-meta))))

(deftest test|not
  (test-equality #(! (t/value 1)))
  (testing "simplification"
    (testing "universal/null set"
      (is= (! t/universal-set)
           t/empty-set)
      (is= (! t/empty-set)
           t/universal-set))
    (testing "universal class-set"
      (is= (! t/val?)
           t/nil?)
      (is= (! t/val|by-class?)
           t/nil?))
    (testing "DeMorgan's Law"
      (is= (! (| i|a i|b))
           (& (! i|a) (! i|b)))
      (is= (! (& i|a i|b))
           (| (! i|a) (! i|b)))
      (is= (! (| (! i|a) (! i|b)))
           (&       i|a     i|b))
      (is= (! (& (! i|a) (! i|b)))
           (|       i|a     i|b)))))

(deftest test|-
  (testing "="
    (is= (t/- a a)
         t/empty-set))
  (testing "<"
    (is= (t/- a >a)
         t/empty-set))
  (testing "<>"
    (is= (t/- a b)
         a))
  (testing ">"
    (is= (t/- (| a b) a)
         b)
    (is= (t/- (| a b long?) a)
         (| b long?)))
  (testing "><"
    ))

(deftest test|or
  (test-equality #(| a b))
  (test-equality #(| (t/value 1) (t/value 2)))
  (testing "simplification"
    (testing "via single-arg"
      (is= (| a)
           a))
    (testing "via identity"
      (is= (| a a)
           a)
      (is= (| (| a a) a)
           a)
      (is= (| a (| a a))
           a)
      (is= (| (| a b) (| b a))
           (| a b))
      (is= (| (| a b ><0) (| a ><0 b))
           (| a b ><0)))
    (testing "via universal class + nil"
      (is= t/universal-set (| (t/isa? Object) (t/value nil)))
      (is= t/universal-set (| (t/value nil)   (t/isa? Object)))
      (is= t/universal-set (| (t/isa? Object) (t/value nil)   (t/value 1)))
      (is= t/universal-set (| (t/isa? Object) (t/value 1)     (t/value nil)))
      (is= t/universal-set (| (t/value nil)   (t/isa? Object) (t/value 1)))
      (is= t/universal-set (| (t/value nil)   (t/value 1)     (t/isa? Object)))
      (is= t/universal-set (| (t/value 1)     (t/isa? Object) (t/value nil)))
      (is= t/universal-set (| (t/value 1)     (t/value nil)   (t/isa? Object))))
    (testing "nested `or` is expanded"
      (is= (| (| a b) (| ><0 ><1))
           (| a b ><0 ><1))
      (is= (| (| a b) (| ><0 ><1))
           (| a b ><0 ><1)))
    (testing "via `not`"
      (is= (| a (! a))
           t/universal-set)
      (is= (| a b (! a))
           t/universal-set)
      (is= (| a b (| (! a) (! b)))
           t/universal-set))
    (testing "nested"
      (is= (utr/or-type>args (| (| string? double?)
                                char-seq?))
           [double? char-seq?])
      (is= (utr/or-type>args (| (| string? double?)
                                (| double? char-seq?)))
           [double? char-seq?])
      (is= (utr/or-type>args (| (| string? double?)
                                (| char-seq? (t/isa? Number))))
           [char-seq? (t/isa? Number)]))
    (testing "#{<+ =} -> #{<+}"
      (is= (utr/or-type>args (| i|>a+b i|>a0 i|a))
           [i|>a+b i|>a0]))
    (testing "#{<+ >+} -> #{<+}"
      (is= (utr/or-type>args (| i|>a+b i|>a0 i|<a+b i|<a0))
           [i|>a+b i|>a0]))
    (testing "#{>+ =} -> #{=}"
      (is= (| i|<a+b i|<a0 i|a)
           i|a))
    (testing "#{<+ >+ ><+} -> #{<+ ><+}"
      (is= (utr/or-type>args (| i|>a+b i|>a0 i|<a+b i|<a0 i|><0 i|><1))
           [i|>a+b i|>a0 i|><0 i|><1]))
    (testing "#{<+ >+ <>+} -> #{<+ <>+}"
      (is= (utr/or-type>args (| >a <a0 ><0 ><1))
           [>a ><0 ><1]))
    (testing "#{<+ =+ >+ ><+} -> #{<+ ><+}"
      (is= (utr/or-type>args (| i|>a+b i|>a0 i|a i|<a+b i|<a0 i|><0 i|><1))
           [i|>a+b i|>a0 i|><0 i|><1]))
    (testing "#{<+ =+ >+ <>+} -> #{<+ <>+}"
      (is= (utr/or-type>args (| >a a <a0 ><0 ><1))
           [>a ><0 ><1]))))

(deftest test|and
  (test-equality #(& i|a i|b))
  (testing "null set / universal set"
    (is= (& t/universal-set t/universal-set)
         t/universal-set)
    (is= (& t/universal-set t/empty-set)
         t/empty-set)
    (is= (& t/empty-set t/universal-set)
         t/empty-set)
    (is= (& t/universal-set t/empty-set t/universal-set)
         t/empty-set)
    (is= (& t/universal-set string?)
         string?)
    (is= (& t/universal-set char-seq? string?)
         string?)
    (is= (& t/universal-set string? char-seq?)
         string?)
    (is= (& t/empty-set string?)
         t/empty-set)
    (is= (& t/empty-set char-seq? string?)
         t/empty-set)
    (is= (& t/empty-set string? char-seq?)
         t/empty-set))
  (testing "simplification"
    (testing "via single-arg"
      (is= (& a)
           a))
    (testing "via identity"
      (is= (& a a)
           a)
      (is= (& (! a) (! a))
           (! a))
      (is= (& (& a a) a)
           a)
      (is= (& a (& a a))
           a)
      (is= (& (| string? byte?) (| byte? string?))
           (| string? byte?))
      (is= (& (| a b) (| b a))
           (| a b))
      (is= (& (| a b ><0) (| a ><0 b))
           (| a b ><0)))
    (testing ""
      (is= (utr/and-type>args (& i|a i|b))
           [i|a i|b]))
    (testing "empty-set"
      (is= (& a b)
           t/empty-set)
      (is= (& string? byte?)
           t/empty-set)
      (is= (& a ><0)
           t/empty-set)
      (is= (& a ><0 ><1)
           t/empty-set))
    (testing "nested `and` is expanded"
      (is= (& (& a b) (& ><0 ><1))
           (& a b ><0 ><1))
      (is= (& (& a b) (& ><0 ><1))
           (& a b ><0 ><1)))
    (testing "and + not"
      (is= (& a (! a))
           t/empty-set)
      (is= (& a (! b))
           a)
      (is= (& (! b) a)
           a)
      (testing "+ or"
        (is= (& (! a) a b)
             t/empty-set)
        (is= (& a (! a) b)
             t/empty-set)
        (is= (& a b (! a))
             t/empty-set)
        (is= (& (| a b) (! a))
             b)
        ;; TODO fix impls
        #_(is= (& (! a) (| a b))
             b)
        (is= (& (| a b) (! b) (| b a))
             a)
        (is= (& (| a b) (! b) (| ><0 b))
             t/empty-set))
      (is= (& primitive? (! boolean?))
           (| byte? short? char? int? long? float? double?)))
    (testing "#{<+ =} -> #{=}"
      (is= (& i|>a+b i|>a0 i|a)
           i|a))
    (testing "#{>+ =+} -> #{>+}"
      (is= (utr/and-type>args (& i|<a+b i|<a0 i|a))
           [i|<a+b i|<a0]))
    (testing "#{<+ >+} -> #{>+}"
      (is= (utr/and-type>args (& i|>a+b i|>a0 i|<a+b i|<a0))
           [i|<a+b i|<a0]))
    (testing "#{<+ >+ ∅+} -> #{>+ ∅+}"
      (is= (utr/and-type>args (& i|>a+b i|>a0 i|<a+b i|<a0 i|><0 i|><1))
           [i|<a+b i|<a0 i|><0 i|><1]))
    (testing "#{<+ =+ >+ ∅+} -> #{>+ ∅+}"
      (is= (utr/and-type>args (& i|>a+b i|>a0 i|a i|<a+b i|<a0 i|><0 i|><1))
           [i|<a+b i|<a0 i|><0 i|><1]))))

(deftest test|isa?|protocol
  (test-equality #(@#'t/isa?|protocol P)))

(deftest test|isa?|class
  (test-equality #(@#'t/isa?|class C))
  (test-equality #(@#'t/isa?|class A))
  (test-equality #(@#'t/isa?|class I)))

(deftest test|isa?|direct
  (test-equality #(t/isa?|direct C))
  (test-equality #(t/isa?|direct A))
  (test-equality #(t/isa?|direct I))
  (test-equality #(t/isa?|direct P))
  #?(:clj (is (= (t/isa?|direct P) (t/isa? quantum.test.untyped.core.type.AProtocolAll)))))

(deftest test|isa?
  (test-equality #(t/isa? C))
  (test-equality #(t/isa? A))
  (test-equality #(t/isa? I))
  (test-equality #(t/isa? P)))

(deftest test|value
  (test-equality #(t/value 1))
  (testing "hash equality"
    (is= (hash (t/value 1)) (hash (t/value 1)))
    (is= 1 (count (hash-set (t/value 1)
                            (t/value 1))))))
