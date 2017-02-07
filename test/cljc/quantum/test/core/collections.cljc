(ns quantum.test.core.collections
  (:require
    [quantum.core.test        :as test
      :refer [deftest is ]]
    [quantum.core.collections :as ns]))

(defn test:map-entry [a b])

(defn test:genkeyword
  ([])
  ([arg]))

(defn test:wrap-delay [f])

; ; ====== COLLECTIONS ======

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

(defn test:allocate-by-percentages
  [n percents])

; ===== LOGGING ===== ; (TODO MOVE)

(defn test:notify-progress+
  ([topic r])
  ([topic report-fn r]))
