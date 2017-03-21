(ns quantum.test.core.collections
  (:require
    [quantum.core.test        :as test
      :refer [deftest is testing]]
    [quantum.core.collections :as ns]
    [quantum.core.fn
      :refer [fn->>]]
    [quantum.core.logic
      :refer [fn-or fn-and]]))

(deftest test:count=
  (let [data [:a :b :c :d :e]]
    (dotimes [i (count data)]
      (is (ns/count= (take i data) i)))))

(deftest test:count<
  (let [data [:a :b :c :d :e]]
    (dotimes [i (count data)]
      (is (ns/count< (take i data) (inc i))))))

(deftest test:count<=
  (let [data [:a :b :c :d :e]]
    (dotimes [i (count data)]
      (is (ns/count<= (take i data) (inc i)))
      (is (ns/count<= (take i data) i)))))

(deftest test:red-for
  (let [ret (ns/red-for [elem [1 2 1 3 4 2]
                         ret  {:a #{} :b []}]
              (-> ret (update :a conj elem)
                      (update :b conj elem)))]
    (is (= ret {:a #{1 2 3 4}
                :b [1 2 1 3 4 2]}))))

(deftest test:red-fori
  (let [ret (ns/red-fori [elem [1 2 1 3 4 2]
                          ret  {:a #{} :b [] :c []} i]
              (-> ret (update :a conj elem)
                      (update :b conj elem)
                      (update :c conj i)))]
    (is (= ret {:a #{1 2 3 4}
                :b [1 2 1 3 4 2]
                :c [0 1 2 3 4 5]}))))


(deftest test:sliding-window+
  (is (= [[[] [0 1] []]]
         (->> [0 1            ] (ns/sliding-window+ 2) (ns/join []))))
  (is (= [[[] [0 1] [2]]
          [[0 1] [2] []]]
         (->> [0 1 2          ] (ns/sliding-window+ 2) (ns/join []))))
  (is (= [[[] [0 1] [2 4]]
          [[0 1] [2 4] []]]
         (->> [0 1 2 4        ] (ns/sliding-window+ 2) (ns/join []))))
  (is (= [[[] [0 1] [2 3 4 5 6]]
          [[0 1] [2 3] [4 5 6]]
          [[0 1 2 3] [4 5] [6]]
          [[0 1 2 3 4 5] [6] []]]
         (->> [0 1 2 3 4 5 6  ] (ns/sliding-window+ 2) (ns/join []))))
  (is (= [[[] [0 1] [2 3 4 5 6 7]]
          [[0 1] [2 3] [4 5 6 7]]
          [[0 1 2 3] [4 5] [6 7]]
          [[0 1 2 3 4 5] [6 7] []]]
         (->> [0 1 2 3 4 5 6 7] (ns/sliding-window+ 2) (ns/join []))))
  (is (= [[[] [0 1 2] [3 4 5 6 7]]
          [[0 1 2] [3 4 5] [6 7]]
          [[0 1 2 3 4 5] [6 7] []]]
         (->> [0 1 2 3 4 5 6 7] (ns/sliding-window+ 3) (ns/join [])))))

(deftest test:sliding-window-splits+
  (testing "indivisible"
    (is (= [[[] [0 1] [2 3 4 5 6 7 8 9 10 11 12]]
            [[0 1] [2 3] [4 5 6 7 8 9 10 11 12]]
            [[0 1 2 3] [4 5 6] [7 8 9 10 11 12]]
            [[0 1 2 3 4 5 6] [7 8 9] [10 11 12]]
            [[0 1 2 3 4 5 6 7 8 9] [10 11 12] []]]
           (->> [0 1 2 3 4 5 6 7 8 9 10 11 12] (ns/sliding-window-splits+ 5) (ns/join []))))
    (is (= [[[] [0 1 2] [3 4 5 6 7 8 9 10 11 12]]
            [[0 1 2] [3 4 5] [6 7 8 9 10 11 12]]
            [[0 1 2 3 4 5] [6 7 8] [9 10 11 12]]
            [[0 1 2 3 4 5 6 7 8] [9 10 11 12] []]]
           (->> [0 1 2 3 4 5 6 7 8 9 10 11 12] (ns/sliding-window-splits+ 4) (ns/join [])))))
  (testing "divisible"
    (is (= [[[] [0 1 2] [3 4 5 6 7 8 9 10 11]]
            [[0 1 2] [3 4 5] [6 7 8 9 10 11]]
            [[0 1 2 3 4 5] [6 7 8] [9 10 11]]
            [[0 1 2 3 4 5 6 7 8] [9 10 11] []]]
           (->> [0 1 2 3 4 5 6 7 8 9 10 11] (ns/sliding-window-splits+ 4) (ns/join [])))))
  (testing "small"
    (testing "indivisible"
      (is (= [[[] [0] [1 2 3 4]]
              [[0] [1] [2 3 4]]
              [[0 1] [2] [3 4]]
              [[0 1 2] [3 4] []]]
             (->> [0 1 2 3 4] (ns/sliding-window-splits+ 4) (ns/join []))))
      (is (= [[[] [0] [1 2 3 4]]
              [[0] [1 2] [3 4]]
              [[0 1 2] [3 4] []]]
             (->> [0 1 2 3 4] (ns/sliding-window-splits+ 3) (ns/join []))))
      (is (= [[[] [0 1] [2 3 4]]
              [[0 1] [2 3 4] []]]
             (->> [0 1 2 3 4] (ns/sliding-window-splits+ 2) (ns/join []))))
      (is (= [[[] [0 1 2 3 4] []]]
             (->> [0 1 2 3 4] (ns/sliding-window-splits+ 1) (ns/join [])))))
    (testing "divisible"
      (is (= [[[] [0] [1 2 3]]
              [[0] [1] [2 3]]
              [[0 1] [2] [3]]
              [[0 1 2] [3] []]]
             (->> [0 1 2 3] (ns/sliding-window-splits+ 4) (ns/join []))))
      (is (= [[[] [0] [1 2]]
              [[0] [1] [2]]
              [[0 1] [2] []]]
             (->> [0 1 2] (ns/sliding-window-splits+ 3) (ns/join []))))
      (is (= [[[] [0] [1]]
              [[0] [1] []]]
             (->> [0 1] (ns/sliding-window-splits+ 2) (ns/join []))))
      (is (= [[[] [0] []]]
             (->> [0] (ns/sliding-window-splits+ 1) (ns/join [])))))))

; ======== TREE ========== ;

(deftest test:max-depth
  (testing "all"
    (let [<branch?  :branch?
          <children (fn->> ns/vals+ (ns/filter+ (fn-or :branch? :leaf?)))]
      (is (= 0 (ns/max-depth <branch? <children
                 {:a 1 :b 2
                  :depth 1
                  :c {:branch? true :d 4}})))
      (is (= 1 (ns/max-depth <branch? <children
                 {:branch? true :a 1 :b 2
                  :depth   1})))
      (is (= 2 (ns/max-depth <branch? <children
                 {:branch? true :a 1 :b 2
                  :depth   1
                  :c {:branch? true
                      :depth   2
                      :d {:e 4 :f {:g 5}}}})))
      (is (= 3 (ns/max-depth <branch? <children
                 {:branch? true :a 1 :b 2
                  :depth   1
                  :c {:branch? true
                      :depth   2
                      :d {:e 4 :f {:g 5}}
                      :h {:branch? true
                          :depth   3
                          :i {:j {:k 6}}}}})))
      (is (= 4 (ns/max-depth <branch? <children
                 {:branch? true :a 1 :b 2
                  :depth   1
                  :c {:branch? true
                      :depth   2
                      :d {:e 4 :f {:g 5}}
                      :h {:branch? true
                          :depth   3
                          :i {:j {:k 6}}
                          :l {:branch? true
                              :depth   4} ; the last branch
                          :q 9}}})))
      (is (= 5 (ns/max-depth <branch? <children
                 {:branch? true :a 1 :b 2
                  :depth   1
                  :c {:branch? true
                      :depth   2
                      :d {:e 4 :f {:g 5}}
                      :h {:branch? true
                          :depth   3
                          :i {:j {:k 6}}
                          :l {:branch? true
                              :depth   4 ; the last branch
                              :m {:leaf? true
                                  :n 7
                                  :depth 5
                                  :o {:branch? true
                                      :depth 7 ; but unreachable
                                      :p 8}}}
                          :q 9}}}))))))

; _______________________________________________________________
; ======================== COMBINATIVE ==========================
; •••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••
(defn test:sorted-map-by-val [m-0])
; _______________________________________________________________
; ========================== SOCIATIVE ==========================
; •••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••
(defn test:->multi-array
  ([base-type dims]))

(defn test:array->dimensionality
  [arr])

(defn test:array->array-manager-key [x])

(defn test:array->vector*
  ([arr]))

(defn test:array->vector
  ([curr-dim arr])
  ([arr]))

(defn test:padr
  ([x i add]))

(defn test:deep-merge
  [& maps])

(defn test:deep-merge-with
  [f & maps])

(defn test:merge-with-set [m1 m2])

(defn test:compact-map
  [m])

(defn test:lflatten
  [ss])

(defn test:dezip
  [s])

(defn test:flatten-map
  [form])

; ----- META ----- ;

(defn test:merge-meta
  [obj m])

(defn test:into!
  [x coll])

(defn test:butlast+last
  [s])

(defn test:update-vals
  [m f])

(defn test:update-keys
  [m f])

(defn test:update-kv
  [m f])

(defn test:mmerge [& args])

(defn test:mapv'
  [f v])

(defn test:frest [x])

; ----- MISCELLANEOUS ----- ;

(defn test:abs-difference
  [a b])

; ================================================ INDEX-OF ================================================
(defn test:seq-contains?
  [super sub])

(defn test:indices-of-elem
  [coll elem-0])

(defn test:indices-of
  [coll elem-0])

(defn test:lindices-of
  [pred coll])

(defn test:matching-seqs
  [pred coll])

(defn test:indices+ [coll])

; ================================================ MERGE ================================================

(defn test:index-with [coll f])

(defn test:mergel  [a b])
(defn test:merger [a b])

(defn test:split-remove
  [split-at-obj coll])

(defn test:zipmap
  ([ks vs])
  ([map-gen-fn ks-0 vs-0]))

(defn test:select
  [coll & fns])

(defn test:comparator-extreme-of
  [coll compare-fn])
;___________________________________________________________________________________________________________________________________
;=================================================={         LAZY SEQS        }=====================================================
;=================================================={                          }=====================================================
(defn test:lseq+ [x])

(defn test:unchunk
  [s])
;___________________________________________________________________________________________________________________________________
;=================================================={  POSITION IN COLLECTION  }=====================================================
;=================================================={ first, rest, nth, get ...}=====================================================
(defn test:fkey [x])
(defn test:fval [x])

(defn test:up-val
  [m k])

(defn test:rename-keys [m-0 rename-m])

; ===== GET-IN ===== ;

(defn test:get-in
  [coll ks])

(defn test:assoc-in!
  [coll ks v])

(defn test:single? [x])

; ===== ZIPPERS ===== ;

(defn test:zipper [x])

(defn test:zipper-mapv
  [f coll])

;___________________________________________________________________________________________________________________________________
;=================================================={           MERGE          }=====================================================
;=================================================={      zipmap, zipvec      }=====================================================
(defn test:merge-with-k
  [f & maps])

(defn test:merge-vals-left
  [left right f])
;___________________________________________________________________________________________________________________________________
;=================================================={     PARTITION, GROUP     }=====================================================
;=================================================={       incl. slice        }=====================================================
(defn test:slice
  [n-0 coll])


(defn test:select-as+
  ([coll kfs])
  ([coll k1 f1 & {:as kfs}]))
;___________________________________________________________________________________________________________________________________
;=================================================={   DISTINCT, INTERLEAVE   }=====================================================
;=================================================={  interpose, frequencies  }=====================================================
(defn test:duplicates-by
  [pred coll])

(defn test:interpose
  [elem coll])

(defn test:linterleave-all
  [& colls])

(defn test:frequencies+
  [xs])
;___________________________________________________________________________________________________________________________________
;=================================================={         GROUPING         }=====================================================
;=================================================={     group, aggregate     }=====================================================
(defn test:group-merge-with-k+
  [group-by-f merge-with-f coll])

(defn test:merge-left
  ([alert-level])
  ([k v1 v2]))

(defn test:merge-right
  ([alert-level])
  ([k v1 v2]))

(defn test:first-uniques-by+ [k coll])

; ===== SORTING ===== ;

(defn test:sort-parts
  [work])

(defn test:lsort
  [elems])

(defn test:binary-search
  ([xs x])
  ([xs x a b between?]))
;___________________________________________________________________________________________________________________________________
;=================================================={   COLLECTIONS CREATION   }=====================================================
;=================================================={                          }=====================================================

(defn test:reverse-kvs [m])

(defn test:update-nth [x n f])

(defn test:update-first [x f])
(defn test:update-last  [x f])

(defn test:index-with-ids
  [vec-0])

(defn test:flatten-keys
  ([m]))

(defn test:pathify-keys-nested
  ([m])
  ([m max])
  ([m max keep-empty])
  ([m max keep-empty arr]))

(defn test:flatten-keys-nested
  ([m])
  ([m max keep-empty]))

(defn test:treeify-keys
  [m])

(defn test:treeify-keys-nested
  [m])

(defn test:remove-repeats
  ([coll])
  ([f coll])
  ([f coll output last]))

(defn test:transient-copy
  [t])

(defn test:ensurec
  [ensured ensurer])

(defn test:index-by-vals
  [coll & [opts]])

(defn test:merge-deep-with
  [f & maps])

(defn test:merge-deep ([x]) ([x y]))

(defn test:seq-ldifference
  ([l r])
  ([l r selectors]))

(defn test:get-map-constructor
  [rec])

(defn test:deficlass
  [name- fields constructor & fns])

(defn test:into-map-by [m k ms])

(defn test:transpose
  [table-0])

(defn test:merge-keys-with
  [m [k-0 & ks] f])

; =========== NECESSITIES FOR DATASCRIPT AND POSH ============== ;

(defn test:trim-head
  [xs n])

(defn test:take-until*
  [stop-at? ls])

(defn test:rest-at
  [rest-at? ls])

(defn test:split-list-at
  [split-at? ls])

(defn test:deep-list?
  [x])

(defn test:deep-find
  [f x])

(defn test:deep-map [f x])

(defn test:drop-tail
  [xs pred])

; ===== MUTABILITY ===== ;

(defn test:MutableContainer [])

; ====== NUMERIC ======

(deftest test:allocate-by-percentages
  (is (= [7 1 1 1]
         (ns/allocate-by-percentages 10 [3/4 3/20 1/20 1/20])))
  (is (= [8 1 1 1]
         (ns/allocate-by-percentages 11 [3/4 3/20 1/20 1/20])))
  (is (= [8 2 1 1]
         (ns/allocate-by-percentages 12 [3/4 3/20 1/20 1/20]))))

; ===== LOGGING ===== ; (TODO MOVE)

(defn test:notify-progress+
  ([topic r])
  ([topic report-fn r]))
