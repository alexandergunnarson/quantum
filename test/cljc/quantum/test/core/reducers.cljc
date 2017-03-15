(ns quantum.test.core.reducers
           (:require
             [quantum.core.test
               :refer [deftest is testing]]
             [quantum.core.reducers :as ns]
             [quantum.core.reducers.fold :as fold]
     #?(:clj [clojure.test.generative :refer [defspec]])
     #?(:clj [clojure.data.generators :as gen]))
  #?(:cljs (:require-macros
             [quantum.test.core.reducers
               :refer [defequivtest]])))

(comment
  "How a reducer works:"
  (->> coll (map+ inc) (map+ triple) (join []))
  "Results in this reducer:"
  (fn reducer [acc x]
    (let [reducer1 (fn reducer [acc x]
                     (conj acc (triple x)))]
      (reducer1 acc (inc x))))
  "Which is used as the reducing function to reduce into a vector.")

;___________________________________________________________________________________________________________________________________
;=================================================={      LAZY REDUCERS       }=====================================================
;=================================================={                          }=====================================================
(defn test:reverse-conses
  ([s tail] )
  ([s from-tail to-tail]))

(defn test:seq-seq
  [f s])

(defn test:lseq->>
  [s & forms])

(defn test:seq-once
  [coll])
;___________________________________________________________________________________________________________________________________
;=================================================={      FUNCTIONEERING      }=====================================================
;=================================================={      incl. currying      }=====================================================
(defn test:reduce-count
  [coll])

(defn test:fold-count
  [coll])
;___________________________________________________________________________________________________________________________________
;=================================================={    transduce.reducers    }=====================================================
;=================================================={                          }=====================================================
(defn test:map-state
  [f init coll])

(defn test:mapcat-state
  [f init coll])
;___________________________________________________________________________________________________________________________________
;=================================================={           CAT            }=====================================================
;=================================================={                          }=====================================================
(defn test:cat+
  ([])
  ([ctor])
  ([left right]))

(defn test:append!
  [acc x])

(defn test:foldcat+
  [coll])
;___________________________________________________________________________________________________________________________________
;=================================================={           MAP            }=====================================================
;=================================================={                          }=====================================================
(defn test:map+
  [f coll])

(defn test:map-indexed+
  [f coll])

(defn test:indexed+
  [coll])
;___________________________________________________________________________________________________________________________________
;=================================================={          MAPCAT          }=====================================================
;=================================================={                          }=====================================================

(defn test:mapcat+
  [f coll])

(defn test:concat+ [& args])
;___________________________________________________________________________________________________________________________________
;=================================================={        REDUCTIONS        }=====================================================
;=================================================={                          }=====================================================
(defn test:reductions+
  ([f coll])
  ([f init coll]))
;___________________________________________________________________________________________________________________________________
;=================================================={      FILTER, REMOVE      }=====================================================
;=================================================={                          }=====================================================
(defn test:filter+
  [pred coll])

(defn test:remove+
  [pred coll])

(defn test:keep+ [coll])
;___________________________________________________________________________________________________________________________________
;=================================================={         FLATTEN          }=====================================================
;=================================================={                          }=====================================================
(defn test:flatten+ [coll])

(defn test:flatten-1+ [x])

;___________________________________________________________________________________________________________________________________
;=================================================={          RANGE           }=====================================================
;=================================================={                          }=====================================================
(defn test:range+
  ([              ])
  ([      end     ])
  ([start end     ])
  ([start end step]))

(deftest test:repeat ; {:adapted-from "CLJ-994 (Jason Jackson)"}
  ;; equivalent sequences.
  (doseq [n [-1 0 1 10 100 1000]]
    (is (= (ns/join [] (ns/repeat+ n \x))
           (clojure.core/repeat n \x))))

  ;; equivalent reductions.
  (doseq [n [-1 0 1 10 100 1000]]
    (is (= (ns/reduce + (ns/repeat+ n 1))
           (ns/reduce + (clojure.core/repeat n 1)))))

  ;; equivalent folds, group size=default
  (doseq [n [-1 0 1 10 100 1000]]
    (is (= (fold/fold + (ns/repeat+ n 1))
           (ns/reduce + (clojure.core/repeat n 1)))))

  ;; equivalent folds, group size=13
  (doseq [n [-1 0 1 10 100 1000]]
    (is (= (fold/fold 99 + + (ns/repeat+ n 1))
           (ns/reduce + (clojure.core/repeat n 1))))))

;___________________________________________________________________________________________________________________________________
;=================================================={     TAKE, TAKE-WHILE     }=====================================================
;=================================================={                          }=====================================================
(defn test:take+
  [n coll])

(defn test:take-while+
  [pred coll])

(defn test:taker+
  [n coll])

;___________________________________________________________________________________________________________________________________
;=================================================={     DROP, DROP-WHILE     }=====================================================
;=================================================={                          }=====================================================
(defn test:drop+
  [n coll])

(defn test:drop-while+
  [pred coll])

(defn test:dropr+
  [n coll])
;___________________________________________________________________________________________________________________________________
;=================================================={     PARTITION, GROUP     }=====================================================
;=================================================={       incl. slice        }=====================================================
(defn test:reduce-by+
  ([keyfn f coll])
  ([keyfn f init coll]))

(defn test:group-by+
  [f coll])
;___________________________________________________________________________________________________________________________________
;=================================================={   DISTINCT, INTERLEAVE   }=====================================================
;=================================================={  interpose, frequencies  }=====================================================
(defn test:distinct-by+
  [f eq-f coll])

(defn test:distinct+
  [coll])


(defn test:fold-frequencies
  [coll])

(defn test:fold-some
  [pred coll])

(defn test:fold-any
  [coll])

(defn test:fold-extremum
  [compare-fn coll])

(defn test:fold-min
  [coll])

(defn test:fold-max
  [coll])

(defn test:fold-empty?
  [coll])

(defn test:fold-every?
  [pred coll])

;___________________________________________________________________________________________________________________________________
;=================================================={          ZIPVEC          }=====================================================
;=================================================={                          }=====================================================
(defn test:zipvec+
  ([vec-0])
  ([vec-0 & vecs]))
;___________________________________________________________________________________________________________________________________
;=================================================={ LOOPS / LIST COMPREHENS. }=====================================================
;=================================================={        for, doseq        }=====================================================
(defn test:for+
  [seq-exprs body-expr])

(defn test:doseq+
  [bindings & body])

(defn test:each
  [f coll])


#?(:clj
(defmacro defequivtest
  ;; f is the core fn, r is the reducers equivalent, rt is the reducible ->
  ;; coll transformer
  [name [f r rt] fns]
  `(deftest ~name
     (let [c# (range -100 1000)]
       (doseq [fn# ~fns]
         (is (= (~f fn# c#)
                (~rt (~r fn# c#)))))))))

(comment

(defequivtest test-map
  [map r/map #(into [] %)]
  [inc dec #(Math/sqrt (Math/abs %))])

(defequivtest test-mapcat
  [mapcat r/mapcat #(into [] %)]
  [(fn [x] [x])
   (fn [x] [x (inc x)])
   (fn [x] [x (inc x) x])])

(deftest test-mapcat-obeys-reduced
  (is (= [1 "0" 2 "1" 3]
        (->> (concat (range 100) (lazy-seq (throw (Exception. "Too eager"))))
          (r/mapcat (juxt inc str))
          (r/take 5)
          (into [])))))

(defequivtest test-reduce
  [reduce r/reduce identity]
  [+' *'])

(defequivtest test-filter
  [filter r/filter #(into [] %)]
  [even? odd? #(< 200 %) identity])


(deftest test-sorted-maps
  (let [m (into (sorted-map)
                '{1 a, 2 b, 3 c, 4 d})]
    (is (= "1a2b3c4d" (reduce-kv str "" m))
        "Sorted maps should reduce-kv in sorted order")
    (is (= 1 (reduce-kv (fn [acc k v]
                          (reduced (+ acc k)))
                        0 m))
        "Sorted maps should stop reduction when asked")))

(deftest test-nil
  (is (= {:k :v} (reduce-kv assoc {:k :v} nil)))
  (is (= 0 (r/fold + nil))))

(defn gen-num []
  (gen/uniform 0 2000))

(defn reduced-at-probe
  [m p]
  (reduce-kv (fn [_ k v] (when (== p k) (reduced :foo))) nil m))

(defspec reduced-always-returns
  (fn [probe to-end]
    (let [len (+ probe to-end 1)
          nums (range len)
          m (zipmap nums nums)]
      (reduced-at-probe m probe)))
  [^{:tag `gen-num} probe ^{:tag `gen-num} to-end]
  (assert (= :foo %))))
