(ns quantum.sql.core)
(require
  '[quantum.core.ns          :as ns  :refer [defalias alias-ns]])
(ns/require-all *ns* :clj :lib)
(ns/ns-exclude *ns* 'distinct 'select 'group-by)
; TODO: make SQL operations delayed like reducers for more efficiency

(defn ^Vec merge-in-entry
  "Merges entries. Invokes @merge-with-fn when values conflict."
  {:in ["{:a 1 :b 2}"
        "[{:a 1 :c 3}
          {:a 1 :d 4}
          {:b 2 :e 5}]"
        "[0 1]"]
   :out "[{:b 2, :a 1, :c 3}
          {:b 2, :a 1, :d 4}
          {:b 5, :e 2}]"}
  [^Map to-merge ^Vec merging-table ^Vec indices-of-val ^Fn merge-with-fn]
  (if (empty? indices-of-val)
      merging-table
      (reduce+
        (fn [^Vec merged-table ^Int index-n]
          (update+ merged-table index-n
            (fn [^Map orig-entry]
              (merge-with+
                merge-with-fn
                orig-entry to-merge))))
        merging-table
        indices-of-val)))
(defn ^Map indices-of
  {:todo ["Find some way to do in parallel - |foldi+|??"
          "Fix the reduction function to do automatic currying"]
   :in  ["[{:order-num 1.3}
           {:order-num 3.4}
           {:order-num 3.4}
           {:order-num 4.6}]"
         ":order-num"]
   :out "{1.3 [0] 3.4 [1 2] 4.6 [3]}"}
  [^Vec table ^Key k]
  (let [^Fn reduce-func
        (fn
          ([indices ^Map table-row ^Int row-num]
          (let [value-of-k-at-row (get table-row k)]
            (if (contains? indices value-of-k-at-row)
                (update+ indices value-of-k-at-row
                  (f*n conj row-num))
                (assoc+ indices value-of-k-at-row [row-num])))))]
    (reducei+
      reduce-func
      {}
      table)))
(defn- ^Vec indices-where-true
  {:in ["[{:a 'abc'  :b 2}
          {:a 'abcd' :b 40}
          {:a 'cda'  :b 3}
          {:a 'cda'  :b 5}]"
        "{:a 'ab' :c 2}"
        "(fn [left-entry right-entry]
           (contains? (:a left-entry) (:a right-entry))"]
   :out "[0 1]"
   :complexity "O(n)"}
  [^Vec left ^Map right-entry ^Fn pred]
  (reducei+
    (fn [indices-f ^Map left-entry index-n]
      (if (pred left-entry right-entry)
          (conj indices-f index-n)
          indices-f))
    []
    left))

; GOOD EXAMPLE OF PROTOCOLIZATION
(defprotocol SQLLeftJoin 
  (left-join-prot [on-obj left right merge-with-fn]))
(declare left-join-on-key)
(declare left-join-on-fn)
(extend-protocol SQLLeftJoin
  Keyword
  (left-join-prot [on-obj left right merge-with-fn]
    (left-join-on-key left right on-obj merge-with-fn))
  AFunction
  (left-join-prot [on-obj left right merge-with-fn]
    (left-join-on-fn left right on-obj merge-with-fn)))

(defn left-join
  "Entry point for |left-join-prot|. Needed to switch argument order
   and thus dispatch on type via protocol."
  [^Vec left ^Vec right on-obj ^Fn merge-with-fn]
  (left-join-prot on-obj left right merge-with-fn))

(defn- ^Vec left-join-on-key
  {:todo ["Probably could be done faster...?"
          "If the left table is in a |record| format, do those extra 
           fields get treated as if they were part of a hash-map and
           thereby slow performance?"]
   :in  ["[{:a 1 :b 2}
           {:a 3 :b 3}
           {:a 3 :b 5}]"
         "[{:a 1 :c 2}
           {:a 1 :c 3}
           {:a 3 :c 4}]"
         ":a"]
   :out "[{:a 1, :b 2, :c 2}
          {:a 3, :b 3, :c 4}
          {:a 3, :b 5, :c 4}]"}
  [^Vec left ^Vec right ^Key on-key ^Fn merge-with-fn]
  (let [indices-of-k-in-left (indices-of left on-key)]
    (reduce+
      (fn [left-f ^Map right-entry]
        (let [value-of-k-at-entry (get right-entry on-key)
              ^Vec indices-of-value-in-left
                (get indices-of-k-in-left value-of-k-at-entry)]
          (merge-in-entry right-entry left-f
            indices-of-value-in-left
            merge-with-fn)))
      left
      right)))

(defn- handle-left-join-on-fn
  [^Fn on-fn ^Fn merge-with-fn]
  (fn
    ([left-f ^Key right-key ^Map right-entry]
      ((handle-left-join-on-fn on-fn merge-with-fn)
       left-f right-entry))
    ([left-f ^Map right-entry]
      (let [indices-where-join-fn-is-true
             (indices-where-true left-f right-entry on-fn)]
        (merge-in-entry right-entry left-f
          indices-where-join-fn-is-true
          merge-with-fn)))))
(defn ^Vec left-join-on-fn
  {:todo ["Probably could be done faster...?"
          "Auto-generate records on the fly for faster lookup."
          "If the left table is in a |record| format, do extra 
           fields from the right table get treated as if they
           were part of a hash-map and thereby slow performance?"]
   :in  ["[{:a 'abc'  :b 2}
           {:a 'abcd' :b 40}
           {:a 'cda'  :b 3}
           {:a 'cda'  :b 5}]"
         "[{:a 'ab'   :c 2}
           {:a 'ab'   :c 100}
           {:a 'da'   :c 3}
           {:a 'd'    :c 4}
           {:a 'q'    :c 44}]"
         "(fn [left-entry right-entry]
            (contains? (:a left-entry) (:a right-entry))"]
   :out "[{:a 'abc', :b 2, :c 2}
          {:a 'abcd' :b 40 :c 2}
          {:a 'cda', :b 3, :c 3}
          {:a 'cda', :b 5  :c 3}]"}
  [^Vec left ^Vec right ^Fn on-fn ^Fn merge-with-fn]
  (let []
    (reduce+
      (handle-left-join-on-fn on-fn merge-with-fn)
      left
      right)))

(defn union-all
  "Doesn't check for duplicates."
  ([a b]
    (catvec a b))
  ([a b & tables]
    (apply catvec a b tables)))

; (defn union
;   "Checks for duplicates."
;   ([a b k]
;     (->> (catvec a b)
;          ()))
;   ([a b & tables]
;     (apply catvec a b tables)))

(defn ^Delay distinct
  "Returns a table with only the distinct entries based on key @k."
  {:todo ["Implement a reducers |sort| algorithm...?"]}
  [^Key k ^Vec table]
  (->> table
       (sort-by k) ; it's okay - it outputs a foldable collection
       (distinct-by+ (f*n get k))))

(defn ^Delay select
  {:usage "(sql/select
             {:OrderNum
                (fn [^Map {:as entry
                           :keys [OnlineOrderID
                                  OrderNum]}]
                  (if (and (nnil? OnlineOrderID)
                           (nil?  OrderNum))
                      OnlineOrderID
                      OrderNum))
              :order-num :OrderNum}
              my-table)"
   :todo ["Handle cases like the one above where the key is redefined"
          "Protocolize inner function"
          "Use |select-as+| for this instead"]}


; (defn select [^Map params ^Vec table]
;   (->> table
;        (map+
;          (fn-> (select-as+ params) reducem+))))
  [^Map params ^Vec table]
  (let [^Fn assoc-fields-to-entry
          (fn [^Map entry-f ^Key field-name new-field-creator]
            (assoc+ entry-f field-name
              ; TODO protocolize
              (condf new-field-creator
                keyword?
                  (partial get entry-f)
                fn?
                  (*fn entry-f)
                :else 
                  identity)))
        ^Fn keep-only-fields-from-params
          (fn->> (filter+ (compr key+ (partial contains? params))))
        ^Fn params-fn
          (fn [^Map entry]
            (->> entry
                 (#(reduce+ assoc-fields-to-entry % params))
                 keep-only-fields-from-params
                 reducem+))]
    (->> table (map+ params-fn))))

(def group-by group-merge-with+)
