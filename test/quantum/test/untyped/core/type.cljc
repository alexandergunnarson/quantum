(ns quantum.test.untyped.core.type
        (:refer-clojure :exclude
          [boolean? char? double? float? fn? ifn? int? ratio? string? symbol?])
        (:require
          [clojure.core                               :as core]
          [quantum.untyped.core.data.map              :as umap]
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

#?(:clj (def boolean?    (t/isa? #?(:clj Boolean :cljs js/Boolean))))
#?(:clj (def byte?       (t/isa? Byte)))
#?(:clj (def short?      (t/isa? Short)))
#?(:clj (def char?       (t/isa? Character)))
#?(:clj (def int?        (t/isa? Integer)))
#?(:clj (def long?       (t/isa? Long)))
#?(:clj (def float?      (t/isa? Float)))
        (def double?     (t/isa? #?(:clj Double :cljs js/Number)))

        (def primitive?  (t/or boolean? #?@(:clj [byte? short? char? int? long? float?]) double?))

#?(:clj (def comparable-primitive? (t/- primitive? boolean?)))

#?(:clj (def ratio?      (t/isa? clojure.lang.Ratio)))

#?(:clj (def char-seq?   (t/isa? CharSequence)))
        (def string?     (t/isa? #?(:clj String :cljs js/String)))

        (def symbol?     t/symbol?)

        (def fn?         t/fn?)
        (def ifn?        t/ifn?)

#?(:clj (def comparable? (t/isa? Comparable)))
#?(:clj (def java-set?   (t/isa? java.util.Set)))

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
       (map t/isa?) set))

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
            (is= (.hashCode ^Object a) (.hashCode ^Object b))))
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

(defn- test-basic-finite [f]
  (is= true  ((f []) nil))
  (is= true  ((f []) []))
  (is= true  ((f []) #{}))
  (is= true  ((f []) {}))
  (is= true  ((f) nil))
  (is= true  ((f) []))
  (is= true  ((f) #{}))
  (is= true  ((f) {}))
  (is= false ((f [boolean?]) nil))
  (is= false ((f [boolean?]) []))
  (is= false ((f [boolean?]) #{}))
  (is= false ((f [boolean?]) {}))
  (is= true  ((f [boolean?]) [true]))
  (is= true  ((f [boolean?]) #{true}))
  (is= false ((f [boolean?]) {true true}))
  (is= false ((f  boolean?) nil))
  (is= false ((f  boolean?) []))
  (is= false ((f  boolean?) #{}))
  (is= false ((f  boolean?) {}))
  (is= true  ((f  boolean?) [true]))
  (is= true  ((f  boolean?) #{true}))
  (is= false ((f  boolean?) {true true}))
  (is= true  ((f [(t/ordered boolean? boolean?)]) {true true}))
  (is= true  ((f  (t/ordered boolean? boolean?))  {true true}))
  (is= true  ((f [(t/ordered boolean? boolean?)]) [[true true]]))
  (is= true  ((f  (t/ordered boolean? boolean?))  [[true true]]))
  (testing "`indexed?` is treated distinctly from `lookup?` with `integer?` keys"
    (is= true  ((f [(t/ordered long? boolean?)]) [[0 true]]))
    (is= true  ((f  (t/ordered long? boolean?))  [[0 true]]))
    (is= false ((f [(t/ordered long? boolean?)]) [true]))
    (is= false ((f  (t/ordered long? boolean?))  [true]))
    (is= true  ((f [(t/ordered long? boolean?)]) {0 true}))
    (is= true  ((f  (t/ordered long? boolean?))  {0 true}))))

(deftest test|unordered
  (test-basic-finite t/unordered)
  (testing "Order should not matter; only frequency"
    (is= true ((t/unordered (t/value 1) (t/value 2) (t/value 3) (t/value 4) (t/value 5) (t/value 6))
                [1 2 3 4 5 6]))
    (testing "Frequency of 1"
      (dotimes [i 100]
        (is= true ((t/unordered (t/value 1) (t/value 2) (t/value 3)
                                (t/value 4) (t/value 5) (t/value 6)) (shuffle [1 2 3 4 5 6])))))
    (testing "Frequency of 2"
      (dotimes [i 100]
        (is= false ((t/unordered (t/value 1) (t/value 2) (t/value 3)
                                 (t/value 4) (t/value 5) (t/value 6)) (shuffle [1 2 3 4 5 6 6])))
        (is= true  ((t/unordered (t/value 1) (t/value 2) (t/value 3) (t/value 4) (t/value 5)
                                 (t/value 6) (t/value 6)) (shuffle [1 2 3 4 5 6 6])))))
    (is= true ((t/unordered (t/value 1) (t/value 2) (t/value 3) (t/value 4) (t/value 5) (t/value 6))
                #{1 2 3 4 5 6}))
    (is= true ((t/unordered (t/ordered (t/value :a) (t/value :b))
                            (t/ordered (t/value :c) (t/value :d))
                            (t/ordered (t/value :e) (t/value :f))
                            (t/ordered (t/value :g) (t/value :h))
                            (t/ordered (t/value :i) (t/value :j)))
                {:a :b :c :d :e :f :g :h :i :j}))
    (let [t (t/unordered (t/unordered (t/value :a) (t/value :b))
                         (t/unordered (t/value :c) (t/value :d))
                         (t/unordered (t/value :e) (t/value :f))
                         (t/unordered (t/value :g) (t/value :h))
                         (t/unordered (t/value :i) (t/value :j)))]
      (is= true (t (->> {:a :b :c :d :e :f :g :h :i :j} (map shuffle) (into {}))))))
  (testing "Internally should sort types deterministically"
    (let [ts (->> (concat (range 15) (range 15)) (map t/value))
          t  (t/unordered ts)]
      (dotimes [i 100]
        (is= t (t/unordered (shuffle ts))))))
  ;; This may be too computationally expensive though, given the `O(n!)` nature of it
  #_(testing "Combinatoric equality between `t/ordered` and `t/unordered`"
    (test-comparison =ident
      (t/unordered (t/value 1) (t/value 2))
      (t/or (t/ordered (t/value 1) (t/value 2))
            (t/ordered (t/value 2) (t/value 1))))
    (test-comparison =ident
      (t/unordered (t/unordered (t/value 1) (t/value 2))
                   (t/unordered (t/value 1) (t/value 2)))
      (t/or (t/ordered (t/ordered (t/value 1) (t/value 2))
                       (t/ordered (t/value 1) (t/value 2)))
            (t/ordered (t/ordered (t/value 1) (t/value 2))
                       (t/ordered (t/value 2) (t/value 1)))
            (t/ordered (t/ordered (t/value 2) (t/value 1))
                       (t/ordered (t/value 1) (t/value 2)))
            (t/ordered (t/ordered (t/value 2) (t/value 1))
                       (t/ordered (t/value 2) (t/value 1)))))))

(deftest test|ordered
  (test-basic-finite t/ordered)
  (testing "Order should matter"
    (is= true  ((t/ordered (t/value 1) (t/value 2) (t/value 3) (t/value 4) (t/value 5) (t/value 6))
                 [1 2 3 4 5 6]))
    (is= false ((t/ordered (t/value 1) (t/value 2) (t/value 3) (t/value 4) (t/value 5) (t/value 6))
                 #{1 2 3 4 5 6}))
    (let [t (t/ordered (t/ordered (t/value :a) (t/value :b))
                       (t/ordered (t/value :c) (t/value :d))
                       (t/ordered (t/value :e) (t/value :f))
                       (t/ordered (t/value :g) (t/value :h))
                       (t/ordered (t/value :i) (t/value :j)))]
      (is= false (t (->> {:a :b :c :d :e :f :g :h :i :j} seq shuffle (into {}))))
      (is= true  (t (umap/om    :a :b :c :d :e :f :g :h :i :j)))
      (is= true  (t (sorted-map :a :b :c :d :e :f :g :h :i :j))))
    (let [t (t/ordered (t/unordered (t/value :a) (t/value :b))
                       (t/unordered (t/value :c) (t/value :d))
                       (t/unordered (t/value :e) (t/value :f))
                       (t/unordered (t/value :g) (t/value :h))
                       (t/unordered (t/value :i) (t/value :j)))]
      (dotimes [i 100]
        (let [base [[:a :b] [:c :d] [:e :f] [:g :h] [:i :j]]
              shuffled (->> base (map shuffle) shuffle (into {}))]
          (if (= base (->> shuffled (map sort)))
              (is= true  (t shuffled))
              (is= false (t shuffled))))
        (is= true  (t (->> (umap/om    :a :b :c :d :e :f :g :h :i :j)
                           (map shuffle) (into (umap/om)))))
        (is= true  (t (->> (sorted-map :a :b :c :d :e :f :g :h :i :j)
                           (map shuffle) (into (sorted-map)))))))))

(deftest test|value
  (test-equality #(t/value 1))
  (testing "hash equality"
    (is= (hash (t/value 1)) (hash (t/value 1)))
    (is= 1 (count (hash-set (t/value 1)
                            (t/value 1))))))

(def >namespace|type (t/ftype string? [string?] [symbol?]))

(def reduce|type (t/ftype t/any? [fn?  t/any? string?   :> char-seq?]
                                 [ifn? t/any? java-set? :> comparable?]))

(deftest test|input-type*
  (is= (t/or string? symbol?)   (t/input-type* >namespace|type [:?]))
  (is= (t/or string? java-set?) (t/input-type* reduce|type     [:_ :_ :?])))
  (is= fn?                      (t/input-type* reduce|type     [:? :_ string?]))

(deftest test|output-type*
  (is= string?                      (t/output-type* >namespace|type))
  (is= (t/or char-seq? comparable?) (t/output-type* reduce|type))
  (is= char-seq?                    (t/output-type* reduce|type [:_ :_ string?])))

(deftest test|rx
  (testing "="
              ;; TODO use a generator
    (doseq [gen-t [#(t/isa? #?(:clj Double :cljs js/Number))
                   #(do t/empty-set)
                   #(do t/universal-set)
                   #(t/value 1)
                   #(t/value "abc")
                   #(t/or (t/isa? #?(:clj Double :cljs js/Number)) (t/value "abc"))]]
      (is= @(t/rx (gen-t)) @(t/rx (gen-t))))))

(deftest test|meta-or
  (is= (t/meta-or [string? string?])
       (t/meta-or [string?]))
  (is= (t/or (t/meta-or [byte? short? char?]) string?)
       (t/meta-or [(t/or byte?  string?)
                   (t/or short? string?)
                   (t/or char?  string?)]))
  (is= (t/or (t/meta-or [long? t/any?])
             (t/meta-or [byte? short? char?]))
       (t/meta-or [(t/or long?  byte?)
                   (t/or long?  short?)
                   (t/or long?  char?)
                   t/any?]))
  (is= (t/and (t/meta-or [long? t/any?])
              (t/meta-or [byte? short? char?]))
       (t/meta-or [t/none? byte? short? char?])))
