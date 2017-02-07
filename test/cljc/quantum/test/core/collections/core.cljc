 (ns quantum.test.core.collections.core
   (:require [quantum.core.collections.core :as ns]))

;___________________________________________________________________________________________________________________________________
;=================================================={        EQUIVALENCE       }=====================================================
;=================================================={       =, identical?      }=====================================================

;___________________________________________________________________________________________________________________________________
;=================================================={         RETRIEVAL        }=====================================================
;=================================================={     get, first, rest     }=====================================================
(defn test:->array [x])

(defn test:count [x])

(defn test:empty? [x])

(defn test:empty [x])

(defn test:lasti [coll])

#?(:clj
(defn test:array-of-type [obj n]))

#?(:clj
(defn test:->array [x ct]))


(defn test:slice
  ([coll a b])
  ([coll a]))

(defn test:rest
  [coll])

(defn test:index-of
  [coll elem])

(defn test:last-index-of
  [coll elem])

(defn test:containsk?
  [coll k])

(defn test:containsv?
  [coll v])

(defn test:aget [x i1])

#?(:clj
(defn test:aget-in*
  ([x i1])
  ([x i1 i2])
  ([x i1 i2 i3])
  ([x i1 i2 i3 i4])
  ([x i1 i2 i3 i4 i5])
  ([x i1 i2 i3 i4 i5 i6])
  ([x i1 i2 i3 i4 i5 i6 i7])
  ([x i1 i2 i3 i4 i5 i6 i7 i8])
  ([x i1 i2 i3 i4 i5 i6 i7 i8 i9])
  ([x i1 i2 i3 i4 i5 i6 i7 i8 i9 i10])))

(defn test:get
  ([coll k])
  ([coll k not-found]))

(defn test:nth
  ([coll i]))

(defn test:assoc!
  ([coll k v]))

(defn test:dissoc [coll k])

(defn test:dissoc! [coll k])

(defn test:conj! [coll v])

(defn test:disj! [coll v])

#?(:clj
(defn test:update! [coll i f]))

(defn test:first [x])

(defn test:second [x])

(defn test:butlast [x])

(defn test:last [x])

(defn test:array [& args])

(defn test:gets [coll indices])

(defn test:third [x])

(defn test:getf [n])

(defn test:conjl
  ([coll a          ])
  ([coll a b        ])
  ([coll a b c      ])
  ([coll a b c d    ])
  ([coll a b c d e  ])
  ([coll a b c d e f])
  ([coll a b c d e f & more]))

(defn test:conjr
  ([coll a    ])
  ([coll a b  ])
  ([coll a b c]))

(defn test:->vec [x])

(defn test:->arr [x])

(defn test:key
  ([kv])
  ([k v]))

(defn test:val
  ([kv])
  ([k v]))

(defn test:reverse [x])
