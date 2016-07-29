(ns quantum.test.core.reducers.reduce
  (:require [quantum.core.reducers.reduce :as ns]))

;___________________________________________________________________________________________________________________________________
;=================================================={          REDUCE          }=====================================================
;=================================================={                          }=====================================================
(defn test:-reduce-seq
  [coll f init]
  (loop [coll-n coll
         ret    init]
    (if (empty? coll-n)
        ret
        (recur (rest coll-n)
               (f ret (first coll-n))))))

(defn test:reduce
  ([f coll])
  ([f init coll]))
;___________________________________________________________________________________________________________________________________
;=================================================={    REDUCING FUNCTIONS    }=====================================================
;=================================================={       (Generalized)      }=====================================================
(defn test:reducer
  ([coll transform]))

(defn test:reducer? [x])

(defn test:conj-red
  ([ret x  ])
  ([ret k v]))

(defn test:conj!-red
  ([ret x  ])
  ([ret k v]))

(defn test:transient-into [to from])

(defn test:persistent-into [to from])

(defn test:joinl
  ([to]) ([to from]) ([to from & froms]))

(defn test:reduce-first
  [r])

(defn test:reduce-nth
  [r n])

(defn test:first-non-nil-reducer
  [_ x])