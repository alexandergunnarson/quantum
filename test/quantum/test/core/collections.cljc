(ns quantum.test.core.collections
  (:require
    [quantum.core.test        :as test
      :refer [deftest is testing]]
    [quantum.core.collections :as ns
      :refer [!ref]]
    [quantum.core.collections.core :as ccoll]
    [quantum.core.error
      :refer [TODO]]
    [quantum.core.fn
      :refer [fn-> fn->>]]
    [quantum.core.logic
      :refer [fn-or fn-and whenf1]]
    [quantum.core.type :as t]
    [quantum.core.meta.profile
      :refer [p profile]]))

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
(deftest test:->array-nd
  #?(:clj (let [vs [["[[D"   [[1.0 2.0 3.0]
                              [4.0 5.0 6.0]]]
                    ["[[J"   [[10  20  30]
                              [40  50  60]]]
                    ["[[[D" [[[1.0 2.0 3.0]
                              [4.0 5.0 6.0]]]]
                    ["[[[J" [[[10  20  30]
                              [40  50  60]]]]]]
            (doseq [[c v] vs]
              (let [arr (ns/->array-nd v)]
                (is (instance? (Class/forName c) arr))
                (is (= v (ns/array->vector arr))))))))

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

(deftest test:split-into
  (is (= [{0 0, 2 -2, 4 -4} {1 -1, 3 -3}]
         (ns/split-into (fn-> ns/key odd?) hash-map {0 0 1 -1 2 -2 3 -3 4 -4}))))

(deftest test:split-by-pred
  (is (= [[0 2 4 6] [1 3 5 7 7]]
         (ns/split odd? [0 1 2 3 4 5 6 7 7]))))

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

(deftest test:group-by-into
  (is (= (ns/group-by-into {} first vector second [[0 1] [0 2] [1 3]])
         {0 [1 2], 1 [3]})))

; ===== SORTING ===== ;

(defn test:sort-parts
  [work])

(defn test:lsort
  [elems])

; ===== SELECTION ===== ;

(deftest test:median-of-5
  (let [x0 (!ref) x1 (!ref) x2 (!ref) x3 (!ref) x4 (!ref)]
    (doseq [xsv (quantum.core.untyped.numeric.combinatorics/permutations [0 1 2 3 4])]
      (is (= (ns/index-of xsv 2) (ns/median-5 (long-array xsv) < x0 x1 x2 x3 x4))))))


; Selection's main purpose is to ensure that the top k items are there.
; It does not currently ensure that those items are sorted. (TODO: FIX)

(deftest test:intro-select!
  ; Adapted from org.apache.lucene.util.TestIntroSelector
  (let [do-profile
          (fn []
            (profile
              (let [xs0 (object-array (repeatedly 10000 #(rand-int 100000)))
                    xs1 (ns/copy xs0)
                    xs2 (ns/copy xs0)
                    xs3 (ns/copy xs0)
                    k 4
                    ret {:sort    (set (ns/ltake 4 (p :sort    (ns/sort!* xs0 >))))
                         :quick   (set (ns/ltake 4 (p :quick   (ns/quick-select!* xs1 k ^java.util.Comparator >))))
                         :intro   (set (ns/ltake 4 (p :intro   (ns/intro-select!* xs2 k ^java.util.Comparator >))))
                         :medians (set (ns/ltake 4 (p :medians (ns/select:median-of-medians!* xs3 k ^java.util.Comparator >))))}]
                (is (= (:sort ret) (:quick ret) (:intro ret) (:medians ret))))))
        do-test
          (fn [slow?]
             (let [rand-int-between (fn [a b] (+ a (rand-int (inc (- b a)))))
                   rand-bool (fn [] (let [n (rand-int-between 0 1)]
                                      (if (= n 0) false true)))
                   from (rand-int 5)
                   to   (+ from (rand-int-between 1 10000))
                   max' (if (rand-bool)
                            (rand-int 100)
                            (rand-int 100000))
                   arr  (int-array (+ from to (rand-int 5)))
                   _    (dotimes [i (ns/count arr)]
                          (ns/assoc! arr i (rand-int-between 0 max')))
                   k    (rand-int-between from (dec to))
                   expected (doto (ns/copy arr) (java.util.Arrays/sort from to)) ; TODO CLJS
                   actual   (ns/copy arr)]
                (if slow?
                    (p ::medians      (ns/select:median-of-medians! k from to actual))
                    (p ::intro-select (ns/intro-select!             k from to actual)))
                #_(is (= (ns/get expected k) ; TODO Assumes must be sorted
                         (ns/get actual   k)))
                (when-not (is (= (set (take k expected)) (set (take k actual))))
                  (throw (ex-info "Will not continue" {})))
                #_(dotimes [i (ns/count actual)] ; TODO Assumes must be sorted
                  (cond (or (< i from) (>= i to))
                          (assert (= (get arr i) (get actual i)))
                        (<= i k)
                          (assert (<= (get actual i) (get actual k)))
                        :else
                          (assert (>= (get actual i) (get actual k)))))))]
    (do-profile)
    ; TODO test more
    #_(dotimes [i 10] (testing "with quick-select" (do-test false)))
    #_(dotimes [i 1] (testing "with median-of-medians" (do-test true)))))

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
