(ns quantum.db.datomic.shard)

; https://groups.google.com/forum/#!msg/datomic/iZHvQfamirI/zFcNRVeOV78J
; 10 billion datoms is the upper limit of a database at which you should shard
; https://groups.google.com/forum/#!msg/datomic/s0u3vjb0GG4/b67uWJp2WXgJ — max entities 

(comment
(defn pq
  "Parallel query."
  {:todo ["Cheaper to create the threads or cheaper to do async+callbacks?
           Likely callbacks"]}
  [query conns]
  (->> conns
       (map+ #(q query %))
       (pjoin #{})))
; other fns can be defined similarly

(def ^{:doc "Has a queue which accepts transaction-requests.
             Processes each transaction-request sequentially but
             runs each conn-transaction pair of the request in parallel."}
  ptransactor
  (->sequentializer 
    (fn [conn=>txn]
      (->> conn=>txn
           (map+ transact!) ; todo: |async-transact!|
           (pjoin {})))
    conns))

(defn ptransact!
  [conn=>txn]
  (distribute ptransactor conn=>txn)))