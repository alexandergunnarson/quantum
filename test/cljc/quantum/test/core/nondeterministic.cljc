(ns quantum.test.core.nondeterministic
  (:require
    [quantum.core.test             :as test
      :refer [is testing deftest]]
    [quantum.core.data.set         :as set]
    [quantum.core.nondeterministic :as ns]))

(defn test:get-generator [secure?])

(defn test:rand-prime [callback & web-workers?])

(defn test:gen-native-secure-random-seeder [])

(defn test:rand-int-between
  ([        a b])
  ([secure? a b]))

(defn test:rand-char-between
  ([        a b])
  ([secure? a b]))

(defn test:rand-chars-between
  ([n a b])
  ([secure? n a b]))

(defn test:rand-numeric*
  ([n        ])
  ([secure? n]))

(defn test:rand-numeric
  ([   ])
  ([a  ])
  ([a b]))

(defn test:rand-upper*
  ([n        ])
  ([secure? n]))

(defn test:rand-upper
  ([   ])
  ([a  ])
  ([a b]))

(defn test:rand-lower*
  ([n        ])
  ([secure? n]))

(defn test:rand-lower
  ([   ])
  ([a  ])
  ([a b]))

(defn test:rand-chars
  [x])

(defn test:rand-bytes
  ([size])
  ([secure? size]))

(defn test:rand-longs
  ([size])
  ([secure? size]))

(defn test:rand-nth [coll])

(defn test:cond-percent
  [& clauses])

(defn test:prob
  ([ps+fs])
  ([ps+fs check-sum?]))

(deftest test:split ; TODO spec this ; TODO de-repeat
  (let [to-split [1 2 3 4]
        {:keys [test training] :as split-0}
          (ns/split to-split [0.2 :test] [0.8 :training])
        #_{:test [2], :training [1 3 4]}
        test-set (set test) training-set (set training)]
    (is (-> split-0 keys set (= #{:test :training})))
    (is (sequential? test))
    (is (sequential? training))
    (is (-> training count (= 3)))
    (is (-> test     count (= 1)))
    (is (set/subset? test-set     (set to-split)))
    (is (set/subset? training-set (set to-split)))
    (is (empty? (set/intersection test-set training-set))))
  (let [to-split [:a 5 "a" [{:d 1.0}]]
        {:keys [a b] :as split-1}
          (ns/split to-split [0.8 :a] [0.2 :b])
        #_{:a [[{:d 1.0}] "a" 5], :b [:a]}
        a-set (set a) b-set (set b)]
    (is (-> split-1 keys set (= #{:a :b})))
    (is (sequential? a) (sequential? b))
    (is (-> a count (= 3)))
    (is (-> b count (= 1)))
    (is (set/subset? a-set (set to-split)))
    (is (set/subset? b-set (set to-split)))
    (is (empty? (set/intersection a-set b-set)))))
