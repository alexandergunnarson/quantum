(ns quantum.test.core.nondeterministic
  (:require [quantum.core.nondeterministic :as ns]))

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

(defn test:split
  [coll & distrs-0])