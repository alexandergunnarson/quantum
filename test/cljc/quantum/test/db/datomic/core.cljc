(ns quantum.test.db.datomic.core
  (:require [quantum.db.datomic.core :as ns]))

(defn test:unhandled-type [type obj])

; PREDICATES

#?(:clj (defn test:entity? [x]))

#?(:clj (defn test:tempid? [x]))
#?(:clj (defn test:dbfn?   [x]))
#?(:clj (defn test:tempid-like? [x]))

#?(:clj (defn test:db? [x]))

(defn test:mdb? [x])

#?(:clj (defn test:conn? [x]))
(defn test:mconn? [x])

(defn test:->uri-string
  [{:keys [type host port db-name]
    :or {type    :free
         host    "localhost"
         port    4334
         db-name "test"}}])

#?(:clj
(defn test:->conn [uri]))

; TRANSFORMATIONS/CONVERSIONS

(defn test:db->seq
  [db])

(defn test:history->seq
  [history])

(defn test:->db
  ([])
  ([arg]))

(defn test:touch [entity])

(defn test:q
  ([query])
  ([query db & args]))

(defn test:entity
  ([eid])
  ([db eid]))

(defn test:entity-db
  ([entity]))

(defn test:tempid
  ([])
  ([part])
  ([conn part]))

(defn test:pull
  ([selector eid])
  ([db selector eid]))

(defn test:pull-many
  ([selector eids])
  ([db selector eids]))\

(defn test:is-filtered
  ([])
  ([db]))

(defn test:with-
  ([tx-data])
  ([db tx-data])
  ([db tx-data tx-meta]))

(defn test:datoms
  ([db index & args]))

(defn test:seek-datoms
  ([db index & args]))

(defn test:index-range
  ([attr start end])
  ([db attr start end]))

(defn test:transact!
  ([tx-data])
  ([conn tx-data])
  ([conn tx-data tx-meta]))

#?(:clj
(defn test:transact-async!
  ([tx-data])
  ([conn tx-data])))

; ===== QUERIES =====

#?(:clj
(defn test:txns
  ([])
  ([conn])))

(defn test:schemas
  ([])
  ([db]))

(defn test:attributes
  ([])
  ([db]))

(defn test:partitions
  ([])
  ([db]))

; ==== OPERATIONS ====

(defn test:->partition
  ([part])
  ([conn part]))

(defn test:rename-schemas [mapping])

(defn test:->schema
  ([ident cardinality val-type])
  ([ident cardinality val-type {:keys [conn part] :as opts}]))

(defn test:block->schemas
  [block & [opts]])

(defn test:add-schemas!
  ([schemas])
  ([conn schemas]))

(defn test:update-schema!
  [schema & kvs])

(defn test:retract-from-schema! [s k v])

(defn test:new-if-not-found
  [attrs])

(defn test:attribute? [x])

(defn test:dbfn-call? [x])

(defn test:transform-validated [x])

(defn test:validated->txn
  [x])

(defn test:validated->new-txn
  [x part])

(defn test:queried->maps [db queried])

#?(:clj
(defn test:entity->map
  ([m])
  ([m n])))

(defn test:lookup
  [attr v])

(defn test:has-transform? [x])

(defn test:wrap-transform [x])

(defn test:transform [x])

(defn test:rename [old new-])

(defn test:assoc
  [eid & kvs])

(defn test:assoc! [& args])

(defn test:dissoc
  [arg & args])

(defn test:dissoc! [& args])

(defn test:merge
  [eid props])

(defn test:merge!
  ([& args]))

(defn test:excise
  ([eid attrs])
  ([eid part attrs])
  ([conn eid part attrs]))

(defn test:conj
  ([x])
  ([part x])
  ([conn part no-transform? x]))

(defn test:conj!
  [& args])

(defn test:disj
  ([eid])
  ([conn eid]))

(defn test:disj! [& args])

(defn test:update
  ([eid k f])
  ([conn eid k f]))

(defn test:update!
  [& args])

#?(:clj
(defmacro test:dbfn
  [requires arglist & body]))

#?(:clj
(defmacro test:defn!
  [sym requires arglist & body]))

#?(:clj
(defn test:entity-history [e]))

#?(:clj
(defn test:entity-history-by-txn
  [e]))

; ==== MORE COMPLEX OPERATIONS ====

#?(:cljs
(defn test:undo!
  ([])
  ([^Database db])))

#?(:clj
(defn test:txn-ids-affecting-eid
  [entity-id]))

(defn test:ensure-conj-entities!
  [txn part])


#?(:clj
(defn test:transact-if!
  [txn filter-fn ex-data-pred]))