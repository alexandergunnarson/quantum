(ns ^{:doc "The core Datomic (and friends, e.g. DataScript) namespace"}
  quantum.db.datomic.core
  (:refer-clojure :exclude [assoc dissoc conj disj update merge])
  (:require-quantum [:core err core-async pr log logic fn cbase tpred])
  (:require
   #?(:clj  [clojure.core                     :as c        ]
      :cljs [cljs.core                        :as c        ])
   #?(:cljs [cljs-uuid-utils.core             :as uuid     ])
   #?(:clj  [datomic.api                      :as db       ]
      :cljs [datomic-cljs.api                 :as db       ])
            [datascript.core                  :as mdb      ]
    ;#?(:clj [quantum.deploy.amazon            :as amz      ])
            [com.stuartsierra.component       :as component]
            [quantum.core.collections         :as coll     ]
            [quantum.core.resources           :as res      ]
    #?(:clj [quantum.core.process             :as proc     ]))
  #?(:cljs (:require-macros
            [datomic-cljs.macros   
              :refer [<?]                                  ]))
  #?(:clj (:import datomic.Peer
                   [datomic.peer LocalConnection Connection]
                   java.util.concurrent.ConcurrentHashMap)))

#?(:clj (swap! pr/blacklist c/conj datomic.db.Db))

; GLOBALS

; Optimally one could have a per-thread binding via dynamic vars, but
; based on certain tests, somehow they don't work 100% in ClojureScript.
; So we can go with atoms for now
(defonce db*   (atom nil))
(defonce conn* (atom nil))
(defonce part* (atom nil))

(defn unhandled-type [type obj]
  (condp = type
    :conn   (->ex :unhandled-predicate
                  "Object is not mconn, conn, or db"
                  obj)
    :db     (->ex :unhandled-predicate
                  "Object is not mdb or db"
                  obj)
    :entity (->ex :unhandled-predicate
                  "Object is not mentity or entity"
                  obj)))

; PREDICATES

#?(:clj (def entity? (partial instance? datomic.query.EntityMap)))
(defalias mentity? datascript.impl.entity/entity?)

(def db?  (partial instance? #?(:clj  datomic.db.Db
                                :cljs datomic-cljs.api.DatomicDB)))

(def ^{:doc "'mdb' because checks if it is an in-*mem*ory database."}
  mdb? (partial instance? datascript.db.DB))

(def conn? (partial instance? #?(:clj  datomic.Connection
                                 :cljs datomic-cljs.api.DatomicConnection)))
(defn mconn? [x] (and (atom? x) (mdb? @x)))

; TRANSFORMATIONS/CONVERSIONS

(defn db->seq
  {:todo ["Add for db"]}
  [db]
  (if (mdb? db)
      (->> db
           (map (fn [d] {:e     (:e     d)
                         :a     (:a     d)
                         :v     (:v     d)
                         :tx    (:tx    d)
                         :added (:added d)})))
      (throw (->ex nil "Not supported for" db))))

(defn history->seq
  "Assumes @history is a seq of DataScript DBs."
  [history]
  (->> history
       (map db->seq)
       (apply concat)))

(defn ->db
  "Arity 0: Tries to find a database object in the global variables.
   Arity 1: Tries to coerce @arg to a database-like object"
  ([]
    (let [db*-f   @db*
          conn*-f @conn*]
      (or db*-f
          (->db conn*-f))))
  ([arg]
    (cond (mconn? arg)
            @arg
          (conn? arg)
            (db/db arg)
          :else
            (throw (->ex nil "Object cannot be transformed into database" arg)))))

(defn touch [entity]
  (cond           (mentity? entity) (mdb/touch entity)
        #?@(:clj [(entity?  entity) (db/touch  entity)])
        :else (throw (unhandled-type :entity entity))))

(defn q
  ([query] (q query (->db)))
  ([query db & args]
    (cond (mdb? db) (apply mdb/q query db args)
          (db?  db) (apply db/q  query db args)
          :else (throw (unhandled-type :db db)))))

(defn entity
  ([eid] (entity (->db) eid))
  ([db eid]
    (cond (mdb? db) (mdb/entity db eid)
          (db?  db) (db/entity  db eid)
          :else (throw (unhandled-type :db db)))))

(defn entity-db
  ([entity]
    (cond           (mentity? entity) (mdb/entity-db entity)
          #?@(:clj [(entity?  entity) (db/entity-db  entity)])
          :else (throw (unhandled-type :entity entity)))))

; Don't call |tempid| in a txfn.
; It can collide with a peer-supplied tempid, causing very strange bugs.
; I recommend passing a tempid in as an arg instead.
(defn tempid
  ([part] (tempid @conn* part))
  ([conn part]
    (cond            (mconn? conn) (mdb/tempid part)
          #?@(:clj  [(conn?  conn) (db/tempid  part)]
              :cljs [(db?    conn) (db/tempid  part)])
          :else (throw (unhandled-type :conn conn)))))

(defn pull
  ([selector eid] (pull (->db) selector eid))
  ([db selector eid]
    (cond           (mdb? db) (mdb/pull db selector eid)
          #?@(:clj [(db?  db) (db/pull  db selector eid)])
          :else (throw (unhandled-type :db db) db))))


(defn pull-many
  ([selector eids] (pull-many (db*) selector eids))
  ([db selector eids]
    (cond           (mdb? db) (mdb/pull-many db selector eids)
          #?@(:clj [(db?  db) (db/pull-many  db selector eids)])
          :else (throw (unhandled-type :db db)))))

(defn is-filtered
  ([] (is-filtered (->db)))
  ([db]
    (or (instance? datascript.db.FilteredDB db)
        #?(:clj (db/is-filtered db))
        )))

(defalias filtered? is-filtered)

(defn with-
  ([tx-data] (with- (->db) tx-data))
  ([db tx-data] (with- db tx-data nil))
  ([db tx-data tx-meta]
    (cond           (mdb? db) (mdb/with db tx-data tx-meta)
          #?@(:clj [(db?  db) (db/with  db tx-data tx-meta)])
          :else (throw (unhandled-type :db db)))))

(defn datoms
  ; TODO add @db* arity
  ([db index & args]
    (cond (mdb? db) (apply mdb/datoms db index args)
          (db?  db) (apply db/datoms  db index args)
          :else (throw (unhandled-type :db db)))))

(defn seek-datoms
  ; TODO add @db* arity
  ([db index & args]
    (cond           (mdb? db) (apply mdb/seek-datoms db index args)
          #?@(:clj [(db?  db) (apply db/seek-datoms  db index args)])
          :else (throw (unhandled-type :db db)))))

(defn index-range
  ([attr start end] (index-range (->db) attr start end))
  ([db attr start end]
    (cond (mdb? db) (mdb/index-range db attr start end)
          (db?  db) (db/index-range  db attr start end)
          :else (throw (unhandled-type :db db)))))

(defn transact!
  ([tx-data]      (transact! @conn* tx-data))
  ([conn tx-data] (transact! conn tx-data nil))
  ([conn tx-data tx-meta]
    (let [txn (cond (mconn? conn) (mdb/transact! conn tx-data tx-meta)
                    (conn?  conn) @(db/transact   conn tx-data)
                    :else (throw (unhandled-type :conn conn)))]
      ; |delay| to avoid printing out the entire database
      [true (delay txn)])))

#?(:clj
(defn transact-async!
  ([tx-data]      (transact-async! @conn* tx-data))
  ([conn tx-data]
    (let [txn (if (conn? conn)
                  @(db/transact-async conn tx-data)
                  (throw (unhandled-type :conn conn)))]
      ; |delay| to avoid printing out the entire database
      [true (delay txn)]))))

; ===== SCHEMAS =====

(def allowed-types
 #{:keyword
   :string 
   :boolean
   :long
   :bigint 
   :float
   :double 
   :bigdec 
   :ref
   :instant
   :uuid
   :uri
   :bytes})

; ===== QUERIES =====

#?(:clj
(defn txns
  "Returns all Datomic database transactions in the log."
  ([] (txns @conn*))
  ([conn]
    (->> conn
         db/log
         (<- db/tx-range nil nil)
         seq))))

(defn schemas
  ([] (schemas (->db)))
  ([db]
    (q '[:find [?ident ...]
         :where [_ :db/ident ?ident]]
        db)))

(defn attributes
  ([] (attributes (->db)))
  ([db]
    (q '[:find [?ident ...]
         :where [?e :db/ident             ?ident]
                [_  :db.install/attribute ?e    ]]
       db)))

(defn partitions-ex []
  (->ex :not-supported
        (str "|partitions| not supported for DataScript. "
             "DataScript does not have partitions.")))

(defn partitions
  ([] (partitions (->db)))
  ([db]
    (if (mdb? db)
        (throw (partitions-ex))
        (q '[:find [?ident ...]
             :where
             [?e :db/ident ?ident]
             [_ :db.install/partition ?e]]
           db))))


; ==== OPERATIONS ====

(defn ->partition
  "Defines a database partition."
  ([part] (->partition @conn* part))
  ([conn part]
    (if (mconn? conn)
        (throw (partitions-ex))
        (let [id (tempid conn :db.part/db)]
          [{:db/id    id
            :db/ident part}
           [:db/add :db.part/db
            :db.install/partition id]]))))

(defn rename-schemas [mapping]
  (for [[oldv newv] mapping]
    {:db/id oldv :db/ident newv}))

(defn ->schema
  "Defines, but does not transact, a new database schema.
   Takes the pain out of schema creation."
  {:usage '(->schema :person.name/family-name :string :one {:doc "nodoc"})}
  ([ident val-type cardinality]
    (->schema ident val-type cardinality nil))
  ([ident val-type cardinality {:keys [conn part] :as opts}]
    (err/assert (contains? allowed-types val-type) #{val-type})
    (let [conn-f (or conn @conn*)
          part-f (when-not (mconn? conn-f)
                   (or part
                       :db.part/db))]
      ; Partitions are not supported in DataScript (yet)
      (when-not (mconn? conn-f)
        (err/assert (nnil? part-f) #{conn-f part-f}))
  
      (let [cardinality-f
              (condp = cardinality
                :one  :db.cardinality/one
                :many :db.cardinality/many
                (throw (->ex :unrecognized-cardinality  
                             "Cardinality not recognized:"
                             cardinality)))]
        (->> {:db/id                 (when-not (mconn? conn)
                                       (tempid part-f))
              :db/ident              ident
              :db/valueType          (keyword "db.type" (name val-type))
              :db/cardinality        cardinality-f
              :db/doc                (:doc        opts)
              :db/fulltext           (:full-text? opts)
              :db/unique             (when (:unique opts)
                                       (->> opts :unique name
                                            (str "db.unique/") keyword))
              :db/index              (:index?     opts)
              :db.install/_attribute part-f}
             (filter (fn [[k v]] (nnil? v)))
             (into {}))))))

(defn block->schemas
  "Transforms a schema-block @block into a vector of individual schemas."
  {:usage '(block->schemas
             {:todo/text       [:string  :one]
              :todo/completed? [:boolean :one {:index? true}]
              :todo/id         [:long    :one {:index? true}]})}
  [block & [opts]]
  (->> block
       (mapv (fn [[ident [val-type cardinality opts-n]]]
               (->schema ident val-type cardinality
                 (c/merge {} opts opts-n))))))

(defn add-schemas!
  "This function exists mainly because schema implementation differs from Datomic
   to DataScript.

   @schemas should be a vector of schema."
  ([schemas] (add-schemas! @conn* schemas))
  ([conn schemas]
    (if (mconn? conn)
        (let [schemas-f (->> schemas
                             (map (fn [schema]
                                    [(:db/ident schema)
                                     (c/dissoc schema :db/ident)]))
                             (into {}))]
          (swap! conn c/update :schema c/merge schemas-f)) ; TODO there hopefully is a better way...? 
        (transact! schemas))))

(defn rename [old new-]
  {:db/id    old
   :db/ident new-})

(defn assoc
  "Transaction function which asserts the attributes (@kvs)
   associated with entity id @id."
  [eid & kvs]
  (into [:db/add eid] kvs))

(defn dissoc
  "Transaction function which retracts the attributes (@kvs)
   associated with entity id @id.

   Unfortunately requires that one knows the values associated with keys.

   '|Retract| with no value supplied is on our list of possible future
    enhancements.' — Rich Hickey"
  [arg & args]
  (let [db-like? (fn-or mdb? db? mconn? conn?)
        [db eid kvs] (if (db-like? arg)    
                         [(->db arg) (first args) (rest args)]
                         [(->db)     arg          args       ])
        _ (log/pr :debug (type db))
        retract-fn (if (mdb? db)
                       :db.fn/retractEntity
                       :db/retract)]
    (into [retract-fn eid] kvs)))

(defn merge
  "Merges in @props to @eid."
  [eid props]
  (apply assoc eid
    (->> props seq (apply concat))))

(defn excise
  ([eid attrs] (excise eid @part* attrs))
  ([eid part attrs] (excise @conn* eid part attrs))
  ([conn eid part attrs]
    {:db/id           (tempid conn part)
     :db/excise       eid
     :db.excise/attrs attrs}))

(defn conj
  "Creates an entity from the supplied attribute-map."
  ([m]      (conj @conn* @part* m))
  ([part m] (conj @conn* part m))
  ([conn part m]
    (c/assoc m :db/id (tempid conn part))))

(defn disj
  ([eid] (disj @conn* eid))
  ([conn eid]
    [:db.fn/retractEntity eid]))

(defn update
  "TODO currently not an atomic operation.
   Use database functions to achieve atomicity."
  ([eid k f]    (update @conn* eid k f))
  ([conn eid k f]
    (let [v-f (get (entity @conn eid) k)]
      (assoc eid k (f v-f)))))

#?(:clj
(defmacro dbfn
  "Used for defining, but not transacting, a database function.

   Is not supported by DataScript."
  [arglist & body]
  `(db/function
     '{:lang     :clojure
       :params   ~arglist
       :code     (do ~@body)})))

#?(:clj
(defmacro defn!
  "Used for defining and transacting a database function.

   Is not supported by DataScript."
  {:usage '(defn! inc [n] (inc n))}
  [sym arglist & body]
  `(create-entity! :db.part/fn
     {:db/ident (keyword "fn" (name '~sym))
      :db/fn    
        (dbfn ~arglist ~@body)})))

; ==== TRANSACTIONS ====
(def ^:const
  ^{:according-to "http://docs.datomic.com/best-practices.html#pipeline-transactions"}
  recommended-txn-ct 100)

; #?(:clj
; (defn batch-transact!
;   "Submits transactions synchronously to the Datomic transactor
;    in asynchronous batches of |recommended-txn-ct|."
;   {:todo ["Handle errors better"]
;    :usage '(batch-transact! db [9494911 1823883 28283819]
;              (fn [twitter-id]
;                {:twitter/user.id twitter-id}))}
;   [^Database db data ->entity-fn & [post-fn-0]]
;   (when (nempty? data)
;     (let [post-fn (or post-fn-0 (constantly nil))
;           threads 
;            (for [entities (->> data
;                                (partition-all recommended-txn-ct)
;                                (map (fn->> (map ->entity-fn)
;                                            (remove nil?))))]
;              (go (try+ (transact! entities)
;                        (post-fn entities)
;                    (catch Object e
;                      (if ((fn-and map? (fn-> :db/error (= :db.error/unique-conflict)))
;                            e)
;                          (post-fn entities)
;                          (throw+ (Err. nil "Cache failed for transaction"
;                                        {:exception e :txn-entities entities})))))))]
;       (->> threads (map (MWA async/<!!)) doall)))))

; ==== MORE COMPLEX OPERATIONS ====

#?(:cljs
(defn undo!
  "Performs undo on a DataScript database which has history."
  {:from "https://gist.github.com/allgress/11348685"
   :contributors #{"Alex Gunnarson"}}
  ([] (undo! (->db)))
  ([^Database db]
    (let [history (-> db :ephemeral :history)
          conn    (-> db :ephemeral :conn   )]
      (err/assert (atom? history) #{history})
      (when (nempty? @history)
        (let [prev   (peek @history)
              before (:db-before prev)
              after  (:db-after  prev)
              ; Invert transition, adds->retracts, retracts->adds
              tx-data (-> prev
                          :tx-data
                          (map (fn [{:keys [e a v t added]}]
                                 (datascript.core.Datom. e a v t (not added))) ))]
          (reset! conn before)
          (swap! history pop)
          (doseq [[k l] @(:listeners (meta conn))]
            (when-not (= k :history) ; Don't notify history of undos
              (l (datascript.core.TxReport. after before tx-data))))))))))

; (defn read-transact! [path] (-> path io/file-str transact!))

; (def entity? (partial instance? datomic.query.EntityMap))

; (defn ^datomic.query.EntityMap entity
;   "Retrieves the data associated with a (long) @id
;    or db/ident such as a schema keyword."
;   {:usage '[(db/entity :person/email)
;             (db/entity 123)]}
;   [id]
;   (d/entity (db*) id))

; (defn txns
;   "Returns all Datomic database transactions in the log."
;   [^Database db]
;   (->> (:conn db) db/log
;        (<- db/tx-range nil nil)
;        seq))

; (def allowed-types
;  #{:keyword
;    :string 
;    :boolean
;    :long
;    :bigint 
;    :float
;    :double 
;    :bigdec 
;    :ref
;    :instant
;    :uuid
;    :uri
;    :bytes})

; (defmacro call! [f & args]
;   `(transact! [[f ~@args]]))

; (defn schema
;   {:usage '(schema :string :person.name/family-name :one {:doc "nodoc"})}
;   ([val-type ident cardinality]
;     (schema val-type ident cardinality {}))
;   ([val-type ident cardinality opts]
;     (with-throw (in? val-type allowed-types) (Err. nil "Val-type not recognized." val-type))
;     (let [cardinality-f
;             (condp = cardinality
;               :one  :db.cardinality/one
;               :many :db.cardinality/many
;               (throw+ (Err. nil "Cardinality not recognized:" cardinality)))
;           part-f
;             (or (:part opts) :db.part/db)]
;       (->> {:db/id                 (d/tempid part-f)
;             :db/ident              ident
;             :db/valueType          (keyword "db.type" (name val-type))
;             :db/cardinality        cardinality-f
;             :db/doc                (:doc        opts)
;             :db/fulltext           (:full-text? opts)
;             :db/unique             (when (:unique opts)
;                                      (->> opts :unique name
;                                           (str "db.unique/") keyword))
;             :db/index              (:index?     opts)
;             :db.install/_attribute part-f}
;            (coll/filter-vals+ nnil?)
;            redm))))

; (defn rename [old new-]
;   {:db/id    old
;    :db/ident new-})

; (defn rename-schemas [mapping]
;   (for [oldv newv mapping]
;     {:db/id oldv :db/ident newv}))

; (defn excise [id part attrs]
;   {:db/id           (db/tempid part)
;    :db/excise       id
;    :db.excise/attrs attrs})

; (defn add-partition [part]
;   {:db/id                 (d/tempid :db.part/db)
;    :db/ident              part
;    :db.install/_partition :db.part/db})

; (defn add-schema! [& args] (transact! [(apply schema args)]))

; (defn schemas []
;   (query '[:find [?ident ...]
;            :where [_ :db/ident ?ident]]))

; (defn attributes []
;   (query '[:find [?ident ...]
;            :where
;            [?e :db/ident ?ident]
;            [_ :db.install/attribute ?e]]))

; (ns-unmap 'quantum.db.datomic 'assoc)

; (defn assoc [id & kvs]
;   (into [:db/add id] kvs))

; (ns-unmap 'quantum.db.datomic 'dissoc!)

; (defn dissoc!*
;   "The built-in |dissoc|-like function for Datomic.
;    Unfortunately requires that one knows the values associated with keys.

;    '|Retract| with no value supplied is on our list of possible future
;     enhancements.' — Rich Hickey"
;   [id & kvs]
;   (let [q (into [:db/retract id] kvs)]
;     (transact! [q])))

; (defn dissoc+
;   [id k] ; TODO fix to be more like clojure.core/dissoc
;   [:fn/retract-except id k []])

; (defn dissoc!
;   "Dissociates the attribute @k from @id in database."
;   [id k] ; TODO fix to be more like clojure.core/dissoc
;   (let [q (dissoc+ id k)]
;     (transact! [q])))

; (ns-unmap 'quantum.db.datomic 'update!)

; (defn update! [id f-key & args]
;   (transact! (apply vector (keyword "fn" (name f-key)) id args)))

; (defn create-entity!
;   ([props] (create-entity! nil props))
;   ([part props]
;     (let [datom (merge {:db/id (d/tempid (or part :db.part/test))} props)]
;       (transact! [datom]))))

; (defmacro dbfn
;   "Used for defining, but not transacting, a database function."
;   [arglist & body]
;   `(datomic.api/function
;      '{:lang     :clojure
;        :params   ~arglist
;        :code     (do ~@body)}))

; (defmacro defn!
;   "Used for defining and transacting a database function."
;   {:usage '(defn! inc [n] (inc n))}
;   [sym arglist & body]
;   `(create-entity! :db.part/fn
;      {:db/ident (keyword "fn" (name '~sym))
;       :db/fn    
;         (dbfn ~arglist ~@body)}))

; (defn+ entity-query [q]
;   (->> (query q)
;        (map+ (f*n first))
;        (map+ (extern (fn [id] [id (entity id)])))
;        redm))

; (defn partitions []
;   (->> (query '[:find ?ident
;            :where [:db.part/db :db.install/partition ?p]
;                   [?p :db/ident ?ident]])
;        (map+ (f*n first))
;        (into #{})))

; #_(do
;   (query '[:find   ?entity
;            :where [?entity :db/doc "hello world"]])
;   (query '[:find ?c :where [?c :community/name]])

;   ; Next, add a fact to the system.
;   ; The following transaction will create a new entity with the doc string "hello world":

;   (let [datom [:db/add (d/tempid :db.part/user)
;                :db/doc "hello world"]]
;     (transact! [datom])
;     nil)
; )

; ; http://docs.datomic.com/best-practices.html#pipeline-transactions
; (def ^:const recommended-txn-ct 100)

; (defn batch-transact!
;   "Submits transactions synchronously to the Datomic transactor
;    in asynchronous batches of |recommended-txn-ct|."
;   {:todo ["Handle errors better"]
;    :usage '(batch-transact! [9494911 1823883 28283819]
;              (fn [twitter-id]
;                {:twitter/user.id twitter-id}))}
;   [^Database db data ->entity-fn & [post-fn-0]]
;   (when (nempty? data)
;     (let [post-fn (or post-fn-0 fn-nil)
;           threads 
;            (for [entities (->> data
;                                (partition-all recommended-txn-ct)
;                                (map (fn->> (map+ ->entity-fn)
;                                            (remove+ nil?)
;                                            redv)))]
;              (go (try+ (transact! entities)
;                        (post-fn entities)
;                    (catch Object e
;                      (if ((fn-and map? (fn-> :db/error (= :db.error/unique-conflict)))
;                            e)
;                          (post-fn entities)
;                          (throw+ (Err. nil "Cache failed for transaction"
;                                        {:exception e :txn-entities entities})))))))]
;       (->> threads (map (MWA clojure.core.async/<!!)) doall))))

; ; pk = primary-key
; (defn primary-key-in-db?
;   "Determines whether, e.g.,
;    :twitter/user.id 8573181 exists in the database."
;   [pk-schema pk-val]
;   (nempty?
;     (query [:find '?entity
;             :where ['?entity pk-schema pk-val]]
;            true)))

; (defn ->entity*
;   "Adds the entity if it its primary (unique) key is not present in the database.

;    No longer implements caching because of possible sync inconsistencies
;    and because it fails to provide a significant performance improvement."
;   ; Primary key only
;   ([pk-schema pk-val db-partition properties]
;     (let [entity-id (ffirst (query [:find  '?eid
;                                     :where ['?eid pk-schema pk-val]]))]
;       (mergel
;         {:db/id (or entity-id (datomic.api/tempid db-partition))
;          pk-schema pk-val}
;         properties))))

; (defn ->entity
;   {:usage '(->entity
;              :twitter/user.id
;              user-id
;              :twitter/user.name
;              db-partition
;              user-meta
;              twitter-key=>datomic-key)}
;   [pk-schema pk-val db-partition data translation-table & [handlers]]
;   (seq-loop [k v data
;              entity-n (->entity*
;                         pk-schema pk-val
;                         db-partition
;                         {})]
;     (cond
;       (nil? v)
;         entity-n
;       (get handlers k)
;         ((get handlers k) data entity-n)
;       :else
;         (logic/if-let [datomic-key (get translation-table k)
;                        valid?      (nnil? v)]
;           (assoc entity-n datomic-key v)
;           entity-n))))

; (defn ->encrypted
;   ; TODO can't cache without system property set so throw error
;   {:performance "Takes ~160 ms per encryption. Slowwww..."}
;   ([schema val-] (->encrypted schema val- true (get crypto/sensitivity-map :password)))
;   ([schema val- entity? sensitivity]
;     (let [schema-ns   (namespace schema)
;           schema-name (name      schema)
;           result (crypto/encrypt :aes val-
;                    {:password (System/getProperty (str "password:" schema-ns "/" schema-name))
;                     :opts (kmap sensitivity)})]
;       (if entity?
;           (let [{:keys [encrypted salt key]} result]
;             {schema             (->> encrypted (crypto/encode :base64-string))
;              (keyword schema-ns (str schema-name ".k1")) key
;              (keyword schema-ns (str schema-name ".k2")) salt})
;           result))))

; (defn ->decrypted
;   {:performance "Probably slow"}
;   ([schema val-] (->decrypted schema val- true false (get crypto/sensitivity-map :password)))
;   ([schema encrypted entity? string-decode? sensitivity]
;     (let [schema-ns   (namespace schema)
;           schema-name (name      schema)
;           ;schema-key
;           key- (if entity?
;                    (get encrypted (keyword schema-ns (str schema-name ".k1")))
;                    (:key encrypted))
;           salt (if entity?
;                    (get encrypted (keyword schema-ns (str schema-name ".k2")))
;                    (:salt encrypted))
;           ^"[B" result
;             (crypto/decrypt :aes
;               (->> encrypted
;                    (<- get schema)
;                    (crypto/decode :base64))
;               {:key         key-
;                :salt        salt
;                :password    (System/getProperty (str "password:" schema-ns "/" schema-name))
;                :opts (kmap sensitivity)})]
;       (if string-decode?
;           (String. result java.nio.charset.StandardCharsets/ISO_8859_1)
;           result))))

; ; You can transact multiple values for a :db.cardinality/many attribute at one time using a set.


; ; If an attributes points to another entity through a cardinality-many attribute, get will return a Set of entity instances. The following example returns all the entities that Jane likes:

; ; // empty, given the example data
; ; peopleJaneLikes = jane.get(":person/likes")
; ; If you precede an attribute's local name with an underscore (_), get will navigate backwards, returning the Set of entities that point to the current entity. The following example returns all the entities who like Jane:
; ; 
; ; // returns set containing John
; ; peopleWhoLikeJane = jane.get(":person/_likes")


; ; DOS AND DONT'S: http://martintrojer.github.io/clojure/2015/06/03/datomic-dos-and-donts/

; ; DO

; ; Keep metrics on your query times
; ; Datomic lacks query planning. Queries that look harmless can be real hogs.
; ; The solution is usually blindly swapping lines in your query until you get an order of magnitude speedup.

; ; Always use memcached with Datomic
; ; When new peers connect, a fair bit of data needs to be transferred to them.
; ; If you don't use memcached this data needs to be queried from the store and will slow down the 'peer connect time' (among other things).

; ; Give your peers nodes plenty of heap space

; ; Datomic was designed with AWS/Dynamo in mind, use it
; ; It will perform best with this backend.

; ; Prefer dropping databases to excising data
; ; If you want to keep logs, or other data with short lifespan in Datomic,
; ; put them in a different database and rotate the databases on a daily / weekly basis.

; ; Use migrators for your attributes, and consider squashing unused attributes before going to prod
; ; Don't be afraid to rev the schemas, you will end up with quite a few unused attributes.
; ; It's OK, but squash them before its too late.

; ; Trigger transactor GCs periodically when load is low
; ; If you are churning many datoms, the transactor is going have to GC. When this happens writes will be very slow.

; ; Consider introducing a Datomic/peer tier in your infrastructure
; ; Since Datomic's licensing is peer-count limited, you might have to start putting
; ; your peers together in a Datomic-tier which the webserver nodes (etc) queries via the Datomic REST API.

; ; DON'T
; ; Don't put big strings into Datomic
; ; If your strings are bigger than 1kb put them somewhere else (and keep a link to them in Datomic).
; ; Datomic's storage model is optimized for small datoms, so if you put big stuff in there perf will drop dramatically.

; ; Don't load huge datasets into Datomic. It will take forever, with plenty transactor GCs.
; ; Keep an eye on the DynamoDB write throughput since it might bankrupt you.
; ; Also, there is a limit to the number of datoms Datomic can handle.

; ; Don't use Datomic for stuff it wasn't intended to do
; ; Don't run your geospatial queries or query-with-aggregations in Datomic, it's OK to have multiple datastores in your system.

; (defn gen-cacher [thread-id cache-fn-var]
;   (async-loop {:id thread-id :type :thread} ; Spawn quickly, mainly not sleeping (hopefully)
;     []
;     (try+ (@cache-fn-var)
;       (catch Err e
;         ;(log/pr :warn e)
;         (if (-> e :objs :exception :db/error (= :db.error/transactor-unavailable))
;             (do #_(init-transactor!)
;                 (async/sleep 10000))
;             (throw+))))
;     ; Just in case there's nothing to cache
;     (async/sleep 1000)
;     (recur)))

; (defn db-count [& clauses]
;   (-> (query (into [:find '(count ?entity)
;                        :where] clauses)
;                 true)
;       flatten first))

; (def filter-realized-delays+
;   (fn->> (coll/filter-vals+ (fn-and delay? realized?))
;          (coll/map-vals+    (fn [x] (try+ (deref x)
;                                        (catch Object e e))))))

; ; TODO MOVE
; (def error?  (fn-or (fn [x] (instance? Err       x)) ; fn-or with |partial| might have problems?
;                     (fn [x] (instance? Throwable x))))

; (defn- gen-cache-fn-sym [sym] (symbol (str "cache-" (name sym) "!"          )))
; (defn- gen-cacher-sym   [sym] (symbol (str          (name sym) "-cacher"    )))


; (defmacro defmemoized
;   {:todo ["Make extensible"]}
;   [memo-type opts cache-sym-0 name- & fn-args]
;   (let [cache-sym (with-meta (or cache-sym-0 (symbol (str (name name-) "-cache*")))
;                     {:tag "java.util.concurrent.ConcurrentHashMap"})
;         f-0 (gensym)
;         register-cache! (gensym)]
;    `(let [opts# ~opts ; So it doesn't get copied in repetitively
;           first-arg# (:first-arg-only? opts#)
;           get-fn#   (or (:get-fn   opts#)
;                         (when (:hashed? opts#)
;                           (fn [m1# k1#    ] (.get         ^ConcurrentHashMap m1# k1#   ))))
;           assoc-fn# (or (:assoc-fn opts#)
;                         (when (:hashed? opts#)
;                           (fn [m1# k1# v1#] (.putIfAbsent ^ConcurrentHashMap m1# k1# v1#))))
;           registered-caches# (:cache-register opts#)
;           _# (declare ~cache-sym)
;           ~register-cache! (fn [] (when (and registered-caches# (not '~cache-sym-0))
;                                     (coll/assoc! registered-caches# '~name- (var ~cache-sym))))
;           ~f-0  (fn ~name- ~@fn-args)]
;       (condp = ~memo-type
;         :default (def ~name-
;                    (memoize ~f-0 nil first-arg# get-fn# assoc-fn#))
;         :map
;           (let [memoized# (cache/memoize* ~f-0 (atom {}) first-arg# get-fn# assoc-fn#)]
;             (def ~cache-sym (:m memoized#))
;             (~register-cache!)
;             (def ~name-     (:f memoized#)))
;         :datomic ; TODO ensure function has exactly 1 arg
;          ~(let [cache-fn-sym             (gen-cache-fn-sym name-)
;                 cacher-sym               (gen-cacher-sym   name-)
;                 entity-if-sym            (symbol (str          (name name-) "->entity-if"))
;                 pk-adder-sym             (symbol (str          (name name-) ":add-pk!"   ))]
;             `(let [opts#               ~opts
;                    memo-subtype#       (:type               opts#) ; :many or :one 
;                    _# (throw-unless (in? memo-subtype# #{:one :many})
;                         (Err. nil "Memoization subtype not recognized." memo-subtype#))
;                    db-partition#       (:db-partition       opts#)
;                    from-key#           (:from-key           opts#) ; e.g. :twitter/user.email. Must be unique
;                    _# (throw-when (nil? from-key#) (Err. nil ":from-key required." opts#))
;                    pk#                 (:pk                 opts#) ; e.g. :twitter/user.id
;                    _# (throw-when (nil? pk#) (Err. nil ":pk (primary key) required." opts#))
;                    in-db-cache-to-key# (:in-db-cache-to-key opts#) ; e.g. follower-ids in database. Used to speed up creation of entities
;                    to-key#             (:to-key             opts#) ; e.g. follower ids
;                    key-map#            (:key-map            opts#) ; key translation map
;                    skip-keys#          (:skip-keys          opts#)
;                    from-type#          (:from-type          opts#)
;                    cache-errors?#      (if (contains? opts# :cache-errors?)
;                                            (:cache-errors? opts#)
;                                            true)
;                    force-update?#      (:force-update?      opts#)
;                    main-fn#
;                     (fn [from-val# ignore-db-retrieve?#]
;                       (let [get-from-val-state# #(.get ^ConcurrentHashMap ~cache-sym from-val#)
;                             from-val-state-0#   (get-from-val-state#)]
;                       (cond
;                         (not (.containsKey ^ConcurrentHashMap ~cache-sym from-val#))
;                           (if (and (not force-update?#) ; Once, then memoized
;                                    (set? skip-keys#)
;                                    (let [eids# (query ; Possibly slow?
;                                                  (into [:find (list '~'count '~'?e) :where]
;                                                    (conj [['~'?e from-key# from-val#] ['~'?e pk#]]
;                                                      (apply list '~'or
;                                                        (for [skip-key# skip-keys#] ; different symbols = OR
;                                                          ['~'?e skip-key#])))))]
;                                      (nempty? eids#)))
;                               (do (.putIfAbsent ^ConcurrentHashMap ~cache-sym from-val# :db)
;                                   :db)
;                               (let [; Multiple threads get the same result from the same delayed function
;                                     f# (delay (~f-0 from-val#))]
;                                  (do (.putIfAbsent ^ConcurrentHashMap ~cache-sym
;                                        from-val# f#)
;                                      (try+ @(get-from-val-state#)
;                                        (catch Object e#
;                                          (when-not cache-errors?#
;                                            (.remove ^ConcurrentHashMap ~cache-sym from-val#))
;                                          (throw+))))))
;                         (delay? from-val-state-0#)
;                           @from-val-state-0#
;                         (= from-val-state-0# :db)
;                           (if ignore-db-retrieve?#
;                               :db
;                               (-> [:find '?e :where ['?e from-key# from-val#]]
;                                  db/query first first db/entity))
;                         :else
;                           (throw+ (Err. nil "Unhandled exception in cache." from-val-state-0#)))))]
;              (do ; The ".putIfAbsent" feature, to my knowledge, is not present in Clojure STM
;                  ; Thus the ConcurrentHashMap
;                  ; from-key-state-cache-sym
;                  (when-not '~cache-sym-0
;                    (defonce ~cache-sym (java.util.concurrent.ConcurrentHashMap.)))
;                  (~register-cache!)

;                  (defn ~name- 
;                    "@from-val: E.g. email
;                      to-val:   E.g. user metadata

;                     Caching: 
;                       If not present in cache, creates delay and derefs it.
;                       Caches errors only if @cache-errors?"
;                    [from-val# & [ignore-db-retrieve?#]]
;                    (if (= from-type# :many)
;                        (doseq [from-val-n# from-val#]
;                          (main-fn# from-val-n# ignore-db-retrieve?#))
;                        (main-fn# from-val# ignore-db-retrieve?#)))
                
;                  (defn ~pk-adder-sym
;                    "Add primary (+ unique) keys to database,
;                     e.g. :twitter/user.id entities.
;                     Basically an analogue of ensuring value exists before |update|."
;                     {:todo ["Redundant and likely unnecessary"]}
;                     [pk-vals#]
;                    ;(swap! in-db-cache-pk# set/union @in-db-cache-full#)
;                    (batch-transact!
;                      pk-vals#
;                      (fn [pk-val#]
;                        (quantum.db.datomic/->entity*
;                           pk# pk-val#
;                           db-partition#
;                           nil))
;                      (fn [entities#]
;                        #_(swap! in-db-cache-pk# into (map pk# entities#)))))

;                  (defn ~entity-if-sym
;                    "Used to create a database transaction
;                     to add the entity if not present,
;                     with supplied properties/schemas."
;                    ([[pk-val# to-val#]] ; to-val <-> entity-data
;                     (~entity-if-sym pk-val# to-val#))
;                    ([pk-val# to-val#]
;                      (when (nempty? to-val#)
;                        (quantum.db.datomic/->entity
;                          pk#
;                          pk-val#
;                          db-partition#
;                          to-val#
;                          key-map#))))

;                  (defn ~cache-fn-sym
;                   "Efficiently transfers from in-memory cache to 
;                    database via |batch-transact!|."
;                    []
;                    (let [realizeds# (->> ~cache-sym
;                                          filter-realized-delays+
;                                          (coll/remove-vals+ (fn-or nil? error?))
;                                          redm)]
;                      (batch-transact!
;                        realizeds#
;                        ~entity-if-sym
;                        (fn [entities#]
;                          ; :db/error :db.error/entity-missing-db-id
;                          ; Record that they're in the db 
;                          ; TODO: Is this even necessary?
;                          ;(swap! in-db-cache-to-key# into (map pk# entities#))
;                          ;(swap! in-db-cache-pk# set/union @in-db-cache-to-key#)
;                          ))

;                      ; Cache offloading/purging
;                      (doseq [from-val# to-val# realizeds#]
;                        (.put ^ConcurrentHashMap ~cache-sym from-val# :db))
;                      (let [failed# (->> ~cache-sym filter-realized-delays+ (coll/filter-vals+ error?) redm)]
;                        (doseq [from-val# to-val# failed#]
;                          (.remove ^ConcurrentHashMap ~cache-sym from-val#))))
;                    (log/pr (keyword (-> ~*ns* ns-name name) "debug") "Completed caching."))
;                  (defonce
;                    ^{:doc "Spawns cacher thread."}
;                    ~cacher-sym
;                    (quantum.db.datomic/gen-cacher
;                      (keyword (name (ns-name ~*ns*))
;                               (name '~cacher-sym))
;                      (var ~cache-fn-sym))))))
;         (throw+ (Err. nil "Memoization type not recognized." ~memo-type))))))

; (defn potential-problems-in-cache [cache]
;   (->> cache
;        (coll/remove-vals+ (eq? :db))
;        filter-realized-delays+
;        redv))

; (defn restart-cacher!
;   "Restarts the cacher for the |defmemoized| fn."
;   [base-sym]
;   (let [cacher-sym (gen-cacher-sym base-sym)
;         cacher-kw  (keyword (namespace cacher-sym) (name cacher-sym))]
;     (if (or (not (contains? @thread/reg-threads cacher-kw))
;             (first (thread/force-close-threads! (eq? cacher-kw))))
;         (do (reset-var! (resolve cacher-sym)
;               (gen-cacher
;                 (keyword (name (ns-name *ns*))
;                          (name cacher-sym))
;                 (resolve (gen-cache-fn-sym base-sym))))
;             true)
;         false)))

; (defn ->hidden-key [k]
;   (keyword (namespace k)
;            (-> k name (str ".hidden?"))))

; (defn add-std-db-fns!
;   "Inserts all the below 'standard' database functions into @db."
;   []
;   (defn! retract-except
;     ^{:doc "This will work for cardinality-many attributes as well as
;             cardinality-one.
            
;             To make sure Alex loves pizza and ice cream but nothing else
;             that might or might not have asserted earlier: 
  
;             [[:assertWithRetracts :alex :loves [:pizza :ice-cream]]]
  
;             For a cardinality/one attribute it's called the same way,
;             but of course with a maximum of one value.
  
;             It does not work if you pass in an ident as the target
;             of a ref that is not actually changing (which is what I
;             tried to do for a ref to an enum entity). You have to
;             convert the ident to an entity ID up front and pass the
;             entity ID to |retract-except|, otherwise you end up
;             with something like this in the case where there is no
;             change to an enum entity ref."
;       :usage '(db/transact!
;                 [[:fn/retract-except 466192930238332 :twitter/user.followers []]])
;       :from "https://groups.google.com/forum/#!msg/datomic/MgsbeFzBilQ/s8Ze4JPKG6YJ"} 
;     [db e a vs]
;     (vals (into (->> (datomic.api/q [:find '?v :where [e a '?v]] db)
;                      (map (comp #(vector % [:db/retract e a %]) first))
;                      (into {}))
;                 (->> vs
;                      (map #(vector % [:db/add e a %]))
;                      (into {}))))))

; (defn txn-ids-affecting-eid
;   "Returns a set of entity ids of transactions affecting @entity-id."
;   {:from "http://dbs-are-fn.com/2013/datomic_history_of_an_entity/"}
;   [entity-id]
;   (->> (query*
;          '[:find  ?tx
;            :in    $ ?e
;            :where [?e _ _ ?tx]]
;          (datomic.api/history (db*))
;          entity-id)
;        (map+ (MWA first))
;        (into #{})))

; (defn changes-affecting
;   {:from "http://dbs-are-fn.com/2013/datomic_history_of_an_entity/"
;    :todo ["Replace |postwalk| with |prewalk|"
;           "Fix |prewalk|"
;           "|Prewalk| would be 'fully expand'; 'postwalk' is one level"]}
;   [eid & [show-entity-maps?]]
;   (->> eid
;        txn-ids-affecting-eid
;        ;; The transactions are themselves represented as entities. We get the
;        ;; full tx entities from the tx entity IDs we got in the query.
;        (map entity)
;        ;; The transaction entity has a txInstant attribute, which is a timestmap
;        ;; as of the transaction write.
;        (sort-by :db/txInstant)
;        ;; as-of yields the database as of a t. We pass in the transaction t for
;        ;; after, and (dec transaction-t) for before. The list of t's might have
;        ;; gaps, but if you specify a t that doesn't exist, Datomic will round down.
;        (map+
;          (fn [tx]
;            {:before (->> tx :db/id datomic.api/tx->t dec
;                          (datomic.api/as-of (db*))
;                          (<- datomic.api/entity eid))
;             :after  (->> tx :db/id
;                          (datomic.api/as-of (db*))
;                          (<- datomic.api/entity eid))}))
;        (into [])
;        (<- logic/whenp show-entity-maps?
;            (fn->> (postwalk (whenf*n (partial instance? datomic.query.EntityMap)
;                               (fn->> (into {}))))))))


; #_(defn rollback
;   "Reassert retracted datoms and retract asserted datoms in a transaction,
;   effectively \"undoing\" the transaction.

;   WARNING: *very* naive function!"
;   {:origin "Francis Avila"}
;   [conn tx-id]
;   (let [tx-log (-> @db/conn datomic.api/log
;                    (datomic.api/tx-range tx-id nil) first) ; find the transaction
;         tx-eid   (-> tx-log :t datomic.api/t->tx) ; get the transaction entity id
;         newdata (->> tx-log :data  ; get the datoms from the transaction
;                      (remove (fn-> :e (= tx-eid))) ; remove transaction-metadata datoms
;                      ; invert the datoms add/retract state.
;                      (map #(do [(if (:added %) :db/retract :db/add) (:e %) (:a %) (:v %)]))
;                      reverse)] ; reverse order of inverted datoms.
;     @(d/transact conn newdata)))
