(ns quantum.test.core.collections.tree
  (:require
    [quantum.core.test
      :refer [deftest is testing]]
    [quantum.core.fn
      :refer [rcomp fn1 fn->]]
    [quantum.core.logic
      :refer [condf]]
    [quantum.core.collections.tree    :as ns]
    [quantum.core.collections.zippers :as zip]))
;___________________________________________________________________________________________________________________________________
;=================================================={     TREE STRUCTURES      }=====================================================
;=================================================={                          }=====================================================
; ------------------------------------------ WALK ----- ;

(defn assert-same-zipper [arg] (zip/?node arg))

(defrecord Foo [a b c])

(def walk-order-data [1 2 {:a 3} (list 4 [5])])

(defn walk-modify [x]
  (condf x number?  inc
           list?    (fn1 conj "c")
           vector?  (fn1 assoc 0 "d")
           keyword? name
           map?     (fn1 assoc :e 7)
           string?  identity))

(defn walk-like-test
  {:from `clojure.test-clojure.clojure-walk}
  [walk-fn {:keys [pre]}]
  (testing "returns the correct result and type of collection"
    (let [colls ['(1 2 3)
                 [1 2 3]
                 #{1 2 3}
                 (sorted-set-by > 1 2 3)
                 {:a 1, :b 2, :c 3}
                 (sorted-map-by > 1 10, 2 20, 3 30)
                 (->Foo 1 2 3)
                 (map->Foo {:a 1 :b 2 :c 3 :extra 4})]]
      (doseq [c colls]
        (let [walked (walk-fn (rcomp pre identity)
                              (rcomp pre identity)
                              c)]
          (is (= c walked))
             ;;(is (= (type c) (type walked)))
          (if (map? c)
            (is (= (walk-fn (rcomp pre #(update-in % [1] inc))
                            (rcomp pre #(reduce + (vals %)))
                            c)
                   (reduce + (map (comp inc val) c))))
            (is (= (walk-fn (rcomp pre inc)
                            (rcomp pre #(reduce + %))
                            c)
                   (reduce + (map inc c)))))
          (when (or (instance? #?(:clj clojure.lang.PersistentTreeMap :cljs cljs.core/PersistentTreeMap) c)
                    (instance? #?(:clj clojure.lang.PersistentTreeSet :cljs cljs.core/PersistentTreeSet) c))
            #?(:clj
                (is (= (.comparator c) (.comparator walked)))))))))) ; TODO CLJS doesn't respect this

(deftest
  test:walk
  (walk-like-test ns/walk {:pre identity}))

(deftest
  test:zip-walk
  (walk-like-test ns/zip-walk {:pre assert-same-zipper}))

; ------------------------------------------ POSTWALK ----- ;

(defn postwalk-like-test
  {:from `clojure.test-clojure.clojure-walk}
  [walk-fn pre]
  (testing "order"
    (is (= (let [a (atom [])]
             (walk-fn (rcomp pre (fn [form] (swap! a conj (walk-modify form))
                                            (walk-modify form)))
               walk-order-data)
             @a)
           [2 3 "a" 4 ["d" 4] {"d" 4 :e 7} 5 6 ["d"]
            (list "c" 5 ["d"]) ["d" 3 {"d" 4 :e 7} (list "c" 5 ["d"])]]))))

(deftest test:postwalk
  (postwalk-like-test ns/postwalk identity))

(deftest test:zip-postwalk
  (postwalk-like-test ns/zip-postwalk assert-same-zipper))

; ------------------------------------------ PREWALK ----- ;

(defn prewalk-like-test
  {:from `clojure.test-clojure.clojure-walk}
  [walk-fn pre]
  (testing "order"
    (is (= (let [a (atom [])]
             (walk-fn (rcomp pre (fn [form] (swap! a conj (walk-modify form))
                                            (walk-modify form)))
               walk-order-data)
             @a)
           [["d" 2 {:a 3} (list 4 [5])]
            "d" 3 {:a 3 :e 7} ["d" 3] "d" 4 ["d" 7] "d" 8 (list "c" 4 [5])
            "c" 5 ["d"] "d"]))))

(deftest test:prewalk
  (prewalk-like-test ns/prewalk identity))

(deftest test:zip-prewalk
  (prewalk-like-test ns/zip-prewalk assert-same-zipper))

(deftest test:zip-prewalk:all-zip-information-accessible
  (let [dirs (atom {})
        ret (ns/zip-prewalk
              (fn [x] (swap! dirs
                        (fn-> (update :up    (rcomp conj vec) (-> x zip/up    zip/?node))
                              (update :at    (rcomp conj vec) (-> x           zip/?node))
                              (update :left  (rcomp conj vec) (-> x zip/left  zip/?node))
                              (update :right (rcomp conj vec) (-> x zip/right zip/?node))
                              (update :down  (rcomp conj vec) (-> x zip/down  zip/?node))))
                      (zip/node x))
              walk-order-data)
        dirs-expected
        {:up [nil
              [1 2 {:a 3} '(4 [5])]
              [1 2 {:a 3} '(4 [5])]
              [1 2 {:a 3} '(4 [5])]
              {:a 3}
              [:a 3]
              [:a 3]
              [1 2 {:a 3} '(4 [5])]
              '(4 [5])
              '(4 [5])
              [5]],
         :at    [[1 2 {:a 3} '(4 [5])] 1 2 {:a 3} [:a 3] :a 3 '(4 [5]) 4 [5] 5],
         :left  [nil nil 1 2 nil nil :a {:a 3} nil 4 nil],
         :right [nil 2 {:a 3} '(4 [5]) nil 3 nil nil [5] nil nil],
         :down  [1 nil nil [:a 3] :a nil nil 4 nil 5 nil]}]
    (is (= ret walk-order-data))
    (is (= dirs-expected @dirs))))

; ------------------------------------------         OTHERS ----- ;

(deftest ^{:from `clojure.test-clojure.clojure-walk}
  test:prewalk-replace
  (is (= (ns/prewalk-replace {:a :b} [:a {:a :a} (list 3 :c :a)])
         [:b {:b :b} (list 3 :c :b)])))

(deftest ^{:from `clojure.test-clojure.clojure-walk}
  test:postwalk-replace
  (is (= (ns/postwalk-replace {:a :b} [:a {:a :a} (list 3 :c :a)])
         [:b {:b :b} (list 3 :c :b)])))

(defn test:tree-filter
  [pred tree])

#?(:clj (defn test:prewalk-find [pred x]))

; ===== Transform nested maps =====

(defn test:apply-to-keys
  ([m])
  ([m f]))

(defn test:keywordize-keys
  [x])

(defn test:keywordify-keys
  [x])

(deftest ^{:from `clojure.test-clojure.clojure-walk}
  test:stringify-keys
  (is (= (ns/stringify-keys {:a 1, nil {:b 2 :c 3}, :d 4})
         {"a" 1, nil {"b" 2 "c" 3}, "d" 4})))







