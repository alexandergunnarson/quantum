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

(deftest test:partition ; TODO spec this
  (let [to-split [1 2 3 4]
        [a b]
          (ns/partition [0.2 0.8] to-split)
        #_{:test [2], :training [1 3 4]}
        a-set (set a) b-set (set b)]
    (is (sequential? a))
    (is (sequential? b))
    (is (-> a     count (= 1)))
    (is (-> b count (= 3)))
    (is (set/subset? a-set (set to-split)))
    (is (set/subset? b-set (set to-split)))
    (is (empty? (set/intersection a-set b-set)))))
