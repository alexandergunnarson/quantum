(ns quantum.core.collections.diff
  (:require [quantum.core.collections.inner :as inner]))

; COMMENTED OUT FOR NOW
; (defn diff-changes
;   "Finds changes in nested maps, does not consider new elements
  
;   (diff-changes {:a 2} {:a 1})
;   => {[:a] 2}
;   (diff-changes {:a {:b 1 :c 2}} {:a {:b 1 :c 3}})
;   => {[:a :c] 2}
;   "
;   {:source "zcaudate/hara.data.diff"}
;   ([m1 m2]
;    (diff-changes m1 m2 []))
;   ([m1 m2 arr]
;    (reduce-kv (fn [out k1 v1]
;                 (if-let [v2 (and (contains? m2 k1)
;                                  (get m2 k1))]
;                   (cond (and (map? v1) (map? v2))
;                         (merge out (diff-changes v1 v2 (conj arr k1)))

;                         (= v1 v2)
;                         out

;                         :else
;                         (assoc out (conj arr k1) v1))
;                   out))
;               {}
;               m1)))

; (defn diff-new
;   "Finds new elements in nested maps, does not consider changes
  
;   (diff-new {:a 2} {:a 1})
;   => {}
;   (diff-new {:a {:b 1}} {:a {:c 2}})
;   => {[:a :b] 1}
;   "
;   {:source "zcaudate/hara.data.diff"}
;   ([m1 m2]
;    (diff-new m1 m2 []))
;   ([m1 m2 arr]
;    (reduce-kv (fn [out k1 v1]
;                  (let [v2 (get m2 k1)]
;                    (cond (and (map? v1) (map? v2))
;                          (map/merge out (diff-new v1 v2 (conj arr k1)))

;                          (not (contains? m2 k1))
;                          (assoc out (conj arr k1) v1)

;                          :else out)))
;               {}
;               m1)))

; (defn diff
;   "Finds the difference between two maps
  
;   (diff {:a 2} {:a 1})
;   => {:+ {} :- {} :> {[:a] 2}}
;   (diff {:a {:b 1 :d 3}} {:a {:c 2 :d 4}} true)
;   => {:+ {[:a :b] 1}
;       :- {[:a :c] 2}
;       :> {[:a :d] 3}
;       :< {[:a :d] 4}}"
;   {:source "zcaudate/hara.data.diff"}
;   ([m1 m2] (diff m1 m2 false))
;   ([m1 m2 reversible]
;    (let [diff (hash-map :+ (diff-new m1 m2)
;                         :- (diff-new m2 m1)
;                         :> (diff-changes m1 m2))]
;      (if reversible
;        (assoc diff :< (diff-changes m2 m1))
;        diff))))

; (defn merge-or-replace [x v]
;   (cond (and (map? x)
;              (map? v))
;         (inner/merge-nested x v)

;         :else v))

; (defn patch
;   "Use the diff to convert one map to another in the forward 
;   direction based upon changes between the two.
  
;   (let [m1  {:a {:b 1 :d 3}}
;         m2  {:a {:c 2 :d 4}}
;         df  (diff m2 m1)]
;     (patch m1 df)
;     => m2)"
;   {:source "zcaudate/hara.data.diff"}
;   [m diff]
;   (->> m
;        (#(reduce-kv (fn [m arr v]
;                        (update-in m arr merge-or-replace v))
;                     %
;                     (map/merge (:+ diff) (:> diff))))
;        (#(reduce (fn [m arr]
;                    (dissoc-in+ m arr))
;                     %
;                     (keys (:- diff))))))

; (defn unpatch
;   "Use the diff to convert one map to another in the reverse 
;   direction based upon changes between the two.
  
;   (let [m1  {:a {:b 1 :d 3}}
;         m2  {:a {:c 2 :d 4}}
;         df  (diff m2 m1 true)]
;     (unpatch m2 df)
;     => m1)"
;   {:source "zcaudate/hara.data.diff"}
;   [m diff]
;   (->> m
;        (#(reduce-kv (fn [m arr v]
;                        (update-in m arr merge-or-replace v))
;                     %
;                     (map/merge (:- diff) (:< diff))))
;        (#(reduce (fn [m arr]
;                    (dissoc-in+ m arr))
;                     %
;                     (keys (:+ diff))))))


; (defn diff-elems-by
;   "Retrieves new/different elements from @new-v which are not in
;    @curr-v based on @new-selector, etc.
;    Handy for diffing by date."
;   {:in '[data-actual updated
;          (fn-> :date time/->instant :nanos) > #{:date :amount}]}
;   [curr-v new-v new-selector comparator-n unique-selectors]
;   (let [ecomparator         (with-assert (coll/extreme-comparator comparator-n) nnil?
;                               (Err. nil "Extreme comparator for _ does not exist." comparator-n))
;         pivot               (->> curr-v (map new-selector) ecomparator)
;         curr-on-last-date   (->> curr-v (filter (fn-> new-selector (= pivot))))
;         all-on-last-date    (->> new-v  (filter (fn-> new-selector (= pivot))))
;         new-on-last-date    (coll/seq-ldifference all-on-last-date curr-on-last-date
;                               unique-selectors)
;         new-after-last-date (->> new-v
;                                  (filter (fn-> new-selector
;                                                (comparator-n pivot))))]
;   (into [] new-on-last-date new-after-last-date)))

