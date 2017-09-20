(ns ^{:doc "The core Datomic (and friends, e.g. DataScript) namespace"}
  quantum.db.datomic.core
  (:refer-clojure :exclude
    [assoc assoc! dissoc dissoc! conj conj! disj disj!
     update merge if-let for doseq nth filter
     contains?])
  (:require
    [clojure.core               :as c]
#?@(:clj
   [[datomic.api                :as db]])
    [datascript.core            :as mdb]
    [com.stuartsierra.component :as comp]
    [quantum.core.collections   :as coll
      :refer [join for kw-map val? contains?
              filter+ filter-vals+ filter-vals', remove-vals+, map+, remove+ remove', nth
              group-by+ prewalk postwalk merge-deep dissoc-in doseq]]
    [quantum.core.core          :as qcore]
    [quantum.core.async         :as async
      :refer [<! <!! >! >!!]]
    [quantum.core.data.set      :as set]
    [quantum.core.error         :as err
      :refer [->ex TODO catch-all]]
    [quantum.core.fn            :as fn
      :refer [<- fn-> fn->> fn1 fnl fn' rfn with-do]]
    [quantum.core.log           :as log]
    [quantum.core.logic         :as logic
      :refer [fn-not fn-and fn-or whenf whenf1 ifn ifn1 if-let condf1]]
    [quantum.core.print         :as pr]
    [quantum.core.resources     :as res]
    [quantum.core.process       :as proc]
    [quantum.core.convert.primitive :as pconv
      :refer [->long]]
    [quantum.core.type           :as t
      :refer [val?]]
    [quantum.core.vars           :as var
      :refer [defalias]]
    [quantum.core.data.validated :as dv]
    [quantum.core.spec           :as s
      :refer [validate]]
    [quantum.core.untyped.convert
      :refer [->name]])
#?(:clj
    (:import
      datomic.Peer
      [datomic.peer LocalConnection Connection]
      [java.util.concurrent ConcurrentHashMap])))

#?(:clj (swap! pr/blacklist c/conj datomic.db.Db datascript.db.DB))

; GLOBALS

; Optimally one could have a per-thread binding via dynamic vars, but
; based on certain tests, somehow they don't work 100% in ClojureScript.
; So we can go with atoms for now
; Also, this should be a connection pool
(defonce db*   (atom nil))
(defonce conn* (atom nil))
(defonce part* (atom nil))

(defn unhandled-type [type obj]
  (case type
    :conn   (->ex :unhandled-predicate "Object is not mconn, conn, or db" obj)
    :db     (->ex :unhandled-predicate "Object is not mdb or db"          obj)
    :entity (->ex :unhandled-predicate "Object is not mentity or entity"  obj)))

; PREDICATES

#?(:clj (def entity? (partial instance? datomic.query.EntityMap)))
(defalias mentity? datascript.impl.entity/entity?)

#?(:clj (def tempid? (fnl instance? datomic.db.DbId          )))
#?(:clj (def dbfn?   (fnl instance? datomic.function.Function)))
#?(:clj (def tempid-like? (fn-or tempid? c/integer?)))

#?(:clj (def db?     (fnl instance? datomic.db.Db)))

(def ^{:doc "'mdb' because checks if it is an in-*mem*ory database."}
  mdb? (partial instance? datascript.db.DB))

#?(:clj (def conn? (partial instance? datomic.Connection)))
(defn mconn? [x] (and (t/atom? x) (mdb? @x)))

(defn ->uri-string
  [{:keys [type host port db-name]
    :or {type    :free
         host    "localhost"
         port    4334
         db-name "test"}}]
  (str "datomic:" (name type) "://" host ":" port "/" db-name
       "?" "h2-port"     "=" (-> port inc)
       "&" "h2-web-port" "=" (-> port inc inc)))

#?(:clj
(defn ->conn
  "Creates a connection to a Datomic database."
  [uri] (db/connect uri)))

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
      (throw (->ex :not-supported "Not supported for" db))))

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
          conn*-f @conn*
          db-new  (->db conn*-f)]
      (if (= db*-f db-new) db*-f (reset! db* db-new))))
  ([arg]
    (cond
 #?@(:clj [(db?    arg) arg])
           (mdb?   arg) arg
           (mconn? arg) @arg
 #?@(:clj [(conn?  arg) (db/db arg)])
          :else
            (throw (->ex "Object cannot be transformed into database" arg)))))

(defn touch [entity]
  (cond           (mentity? entity) (mdb/touch entity)
        #?@(:clj [(entity?  entity) (db/touch  entity)])
        :else (throw (unhandled-type :entity entity))))

(defn q
  ([query] (q query (->db)))
  ([query db & args]
    (let [db (->db db)]
      (cond           (mdb? db) (apply mdb/q query db args)
            #?@(:clj [(db?  db) (apply db/q  query db args)])))))

(defn entity
  ([eid] (entity (->db) eid))
  ([db eid]
    (let [db (->db db)]
      (cond           (mdb? db) (mdb/entity db eid)
            #?@(:clj [(db?  db) (db/entity  db eid)])))))

(defn entity-db
  ([entity]
    (cond           (mentity? entity) (mdb/entity-db entity)
          #?@(:clj [(entity?  entity) (db/entity-db  entity)])
          :else (throw (unhandled-type :entity entity)))))

; Don't call |tempid| in a txfn.
; It can collide with a peer-supplied tempid, causing very strange bugs.
; I recommend passing a tempid in as an arg instead.
(defn tempid
  ([] (tempid (or @part* #?(:cljs :db.part/test)))) ; because DataScript doesn't really care about partitions
  ([part] (tempid (->db) part))
  ([db part]
    (let [db (->db db)]
      (validate part val?)
      (cond            (mdb? db) (mdb/tempid part)
            #?@(:clj  [(db?  db) (db/tempid  part)])))))

(defn pull
  ([selector eid] (pull (->db) selector eid))
  ([db selector eid]
    (let [db (->db db)]
      (cond           (mdb? db) (mdb/pull db selector eid)
            #?@(:clj [(db?  db) (db/pull  db selector eid)])))))


(defn pull-many
  ([selector eids] (pull-many (db*) selector eids))
  ([db selector eids]
    (let [db (->db db)]
      (cond           (mdb? db) (mdb/pull-many db selector eids)
            #?@(:clj [(db?  db) (db/pull-many  db selector eids)])))))

(defn is-filtered
  ([] (is-filtered (->db)))
  ([db]
    (or (instance? datascript.db.FilteredDB db)
        #?(:clj (db/is-filtered db)))))

(defalias filtered? is-filtered)

(defn filter
  ([pred] (filter (->db) pred))
  ([db pred]
    (let [db (->db db)]
      (cond           (mdb? db) (mdb/filter db pred)
            #?@(:clj [(db?  db) (db/filter  db pred)])))))

(defn with
  ([tx-data] (with (->db) tx-data))
  ([db tx-data]
    (let [db (->db db)]
      (cond           (mdb? db) (mdb/with db tx-data)
            #?@(:clj [(db?  db) (db/with  db tx-data)])))))

(defn datoms
  ; TODO add @db* arity
  ([db index & args]
    (let [db (->db db)]
      (cond           (mdb? db) (apply mdb/datoms db index args)
            #?@(:clj [(db?  db) (apply db/datoms  db index args)])))))

(defn seek-datoms
  ; TODO add @db* arity
  ([db index & args]
    (let [db (->db db)]
      (cond           (mdb? db) (apply mdb/seek-datoms db index args)
            #?@(:clj [(db?  db) (apply db/seek-datoms  db index args)])))))

(defn index-range
  ([attr start end] (index-range (->db) attr start end))
  ([db attr start end]
    (let [db (->db db)]
      (cond           (mdb? db) (mdb/index-range db attr start end)
            #?@(:clj [(db?  db) (db/index-range  db attr start end)])))))

(defn transact!
  ([tx-data]      (transact! @conn* tx-data))
  ([conn tx-data] (transact! conn tx-data nil))
  ([conn tx-data tx-meta]
    (let [txn (cond           (mconn? conn) (mdb/transact! conn tx-data tx-meta)
                    #?@(:clj [(conn?  conn) @(db/transact   conn tx-data)])
                    :else (throw (unhandled-type :conn conn)))]
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

(defn db-with [txn-f] (fn [db _ args] (with db (apply txn-f args))))

(defn av->e [db a v] (first (q [:find ['?e] :where ['?e a v]] db)))
(defn ea->v [db e a] (first (q [:find ['?v] :where [e a '?v]] db)))
(defn kq [k a] [:find ['?v] :where ['?e :db/key k] ['?e a '?v]])

; ===== DATOMS ===== ;

(defn ->ident [db x]
  (cond (mdb? db)
        x
        #?@(:clj [(db? db)
                  (db/ident db x)])
        :else (throw (ex-info "Unsupported db to look up ident" {:db db :ident x}))))

; TODO `defnt` these
(defn ->e [datom]
  (if (vector? datom)
      (get datom 0)
      (:e datom)))

(defn ->a [datom]
  (if (vector? datom)
      (get datom 1)
      (:a datom)))

(defn ->v [datom]
  (if (vector? datom)
      (get datom 2)
      (:v datom)))

(defn ->t [datom]
  (if (vector? datom)
      (get datom 3)
      (:t datom)))

(defn ident= [db attr-0 attr-1]
  (let [attr-0 (->ident db attr-0)
        attr-1 (->ident db attr-1)]
    (= attr-0 attr-1)))

(defn datom->seq [datom]
  (if (vector? datom)
      datom
      [(->e datom) (->a datom) (->v datom) (->t datom)]))

; ===== SCHEMAS ===== ;

(def ^{:doc "See also Datomic's documentation."}
  allowed-types
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

(defalias dbfn-call? dv/dbfn-call?)

; TODO check the dbfn-call to make sure that it's spec-safe
; TODO
; :ref     (fn-or map? dbfn-call? identifier? lookup?) ; Can be any entity/record
; TODO need to enforce SQUUID ; http://docs.datomic.com/javadoc/datomic/Peer.html#squuid()
; :uuid    (fnl instance? db/SQUUID)
(dv/def -schema  (s/or* keyword? dbfn-call?)) ; TODO look over this more
(dv/def -any     (fn' true))
(dv/def -ref     (s/or* :db/id :db/ident dbfn-call?)) ; TODO look over this more
(dv/def -keyword (s/or* keyword? dbfn-call?))
(dv/def -string  (s/or* string? dbfn-call?))
(dv/def -boolean (s/or* (fn1 t/boolean?) dbfn-call?))
(dv/def -long    (s/or* #?(:clj  (fn-or (fnl instance? Long   )
                                               (fnl instance? Integer)
                                               (fnl instance? Short  )) #_long?
                                  :cljs c/integer?) ; TODO CLJS |long?| ; TODO autocast from e.g. bigint if safe to do so
                               dbfn-call?))
(dv/def -bigint  (s/or* (fn1 t/bigint?)
                               dbfn-call?))
(dv/def -float   (s/or* #?(:clj  (fn1 t/float?)
                                  :cljs number?)
                               dbfn-call?))  ; TODO CLJS |float?|
(dv/def -double  (s/or* #?(:clj  (fn1 t/double?)
                                  :cljs number?)
                               dbfn-call?))
(dv/def -bigdec  (s/or* #?(:clj  (fnl instance? BigDecimal) #_bigdec?
                                  :cljs number?)
                               dbfn-call?)) ; TODO CLJS |bigdec?|
(dv/def -instant (s/or* (fnl instance? #?(:clj java.util.Date :cljs js/Date  )) ; TODO time/->instant
                       #?(:clj (s/and string?        (s/conformer clojure.instant/read-instant-date)))
                               (s/and (fn1 t/integer?) (s/conformer #(#?(:clj java.util.Date. :cljs js/Date.) (->long %))))
                               dbfn-call?))
(dv/def -uri     (s/or* (fnl instance? #?(:clj java.net.URI   :cljs (TODO) #_paths/URI))
                               dbfn-call?))
(dv/def -bytes   (s/or* (fn1 t/bytes?) dbfn-call?))

(dv/def db/txInstant ::-instant)

(dv/def db/id    (s/or* ::-long tempid?)) ; TODO look over these more
(dv/def db/ident ::-keyword) ; TODO look over these more

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

; TODO deprecate this in favor of `attributes`
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

(dv/def-map schema/intermediate-schema
  :invariant #(if (:datomic:schema/component? %)
                  (-> % :datomic:schema/type (= :ref))
                  true)
  :req-un [(def :datomic:schema/ident       keyword?)
           (def :datomic:schema/type        allowed-types)
           (def :datomic:schema/cardinality #{:one :many})]
  :opt-un [(def :datomic:schema/doc         string?)
           (def :datomic:schema/component?  (fn1 t/boolean?))
           (def :datomic:schema/index?      (fn1 t/boolean?))
           (def :datomic:schema/full-text?  (fn1 t/boolean?))
           (def :datomic:schema/no-history? (fn1 t/boolean?))
           (def :datomic:schema/unique      #{:identity :value})])

; TODO for datascript:
#_(fn->> (map+ (juxt :db/ident identity))
         (join {}))

(defn ->schema
  "Defines, but does not transact, a new database schema."
  {:usage '(->schema {:ident       :person.name/family-name
                      :type        :string
                      :cardinality :one
                      :doc         "nodoc"})}
  ([schema-0] (->schema schema-0 nil))
  ([schema-0 {:keys [conn part datascript?] :as opts}]
    (let [schema      (->schema:intermediate-schema schema-0)
          conn-f      (or conn @conn*)
          datascript? (or datascript? (mconn? conn-f))
          part-f      (when-not datascript?
                        (or part :db.part/db))
          type        (name   (:type   schema))
          unique      (->name (:unique schema))]
      ; Partitions are not supported in DataScript (yet)
      (when-not datascript? (validate part-f val?))
      (->> {:db/id                 (when-not ((fn-or mconn? nil?) conn-f)
                                     (tempid conn-f part-f))
            :db/ident              (:ident schema)
            :db/valueType          (c/keyword "db.type"        type)
            :db/cardinality        (c/keyword "db.cardinality" (name (:cardinality schema)))
            :db/doc                (:doc        schema)
            :db/fulltext           (when-let [v (:full-text? schema)]
                                     (validate type (fn1 = "string")) v)
            :db/unique             (when unique
                                     (validate type (fn1 not= "ref"))
                                     (c/keyword "db.unique" unique))
            :db/isComponent        (let [v (:component? schema)]
                                     (when v (validate type (fn1 = "ref")))
                                     v)
            :db/index              (:index?     schema)
            :db.install/_attribute part-f
            :db/noHistory          (:no-history? schema)}
           (filter-vals' val?)
           (#(if (and datascript? ; DataScript only allows ref value-types
                      (-> % :db/valueType (not= :db.type/ref)))
                 (c/dissoc % :db/valueType)
                 %))))))

(defn rename-schemas [mapping]
  (for [oldv newv mapping]
    {:db/id oldv :db/ident newv}))

(defn id-or-new
  "Creates a new id if the one specified by the key-value pairs, @attrs,
   is not found in the database."
  [attrs]
  (let [query (join [:find '?e '. :where]
                (for [k v attrs]
                  ['?e k v]))]
  `(:fn/or (:fn/q ~query) ~(tempid))))

(def attribute? #(-> @dv/spec-infos (get (:schema/type %)))) ; TODO fix

(def has-transform? #(and (vector? %) (-> % first (= :fn/transform))))

(defn ?wrap-transform [x] ; possibly wrap transform, dep. on if needed
  #?(:clj  (if (has-transform? x)
               x
               [:fn/transform x])
     :cljs x))

; TODO make this optional?
(defn transform-validated [x]
  (let [value-type #(-> @dv/spec-infos (get (:schema/type %)) :schema :db/valueType)
        has-dbfn-call? (atom false)
        ret (prewalk ; prewalk, not postwalk, is important because then you're not reassembling validated maps, which is expensive and possibly won't work
              (condf1 (fn-and record? #?(:clj (fn-and (fn-not tempid?)
                                                      (fn-not dbfn?  ))))
                      #(case (value-type %)
                         :db.type/ref (->> % qcore/get (remove-vals+ nil?) ; Because can't assert nil in DB
                                                       (join {}))
                         nil (throw (->ex "Object's schema not found in registry" {:object % :type (c/type %)}))
                         (qcore/get %))
                      dbfn-call?
                      #(with-do % (reset! has-dbfn-call? true))
                      identity)
              x)]
    {:v ret :has-dbfn-call? @has-dbfn-call?}))

(defn validated->txn
  "Transforms a validated e.g. record into a valid Datomic/DataScript transaction component.
   Assumes nested maps/records are component entities."
  [x] (let [{:keys [v has-dbfn-call?]} (transform-validated x)]
        (if has-dbfn-call? (?wrap-transform v) v)))

(defn validated->new-txn
  "Transforms a validated e.g. record into a valid Datomic/DataScript transaction.
   Assumes nested maps/records are separate entities which need to be transacted separately,
   and that the overarching map/record is to be created new."
  [x part] (TODO)
  (ifn x record?
    (fn [r]
      (let [txn-components (transient [])]
        (c/assoc
          (postwalk
            (whenf1 map?
              (ifn1 attribute?
                    :v
                    (fn [m]
                      (let [id (tempid part)]
                        (->> m
                             (remove-vals+ nil?)
                             (join {})
                             (<- c/assoc :db/id id)
                             (<- c/conj! txn-components))
                        id)))))
          :db/id
          (tempid part))))
    vector))

(defn queried->maps [db queried]
  (map #(->> % first entity (join {})) queried))

(defn lookup
  ([attr v]    (lookup (->db) attr v))
  ([db attr v] (pull (->db db) '[*] [attr v])))

(defn rename [old new-]
  {:db/id    old
   :db/ident new-})

(defn assoc
  "Transaction function which asserts the attributes (@kvs)
   associated with entity id @id."
  [eid & kvs] (validated->txn (apply hash-map :db/id eid kvs)))

(defn assoc! [& args]
  (let [?conn (first args)]
    (if ((fn-or #?(:clj conn?) mconn?) ?conn)
        (transact! ?conn [(apply assoc (rest args))])
        (transact! [(apply assoc args)]))))

(defn dissoc
  "Transaction function which retracts the attributes (@kvs)
   associated with entity id @id.

   Unfortunately requires that one knows the values associated with keys.

   '|Retract| with no value supplied is on our list of possible future
    enhancements.' — Rich Hickey"
  [arg & args]
  (let [db-like? (fn-or mdb? #?(:clj db?) mconn? #?(:clj conn?))
        [db eid kvs] (if (db-like? arg)
                         [(->db arg) (first args) (rest args)]
                         [(->db)     arg          args       ])
        retract-fn (if (mdb? db)
                       :db.fn/retractEntity  ; TODO these are different things
                       :db/retract)]
    (validated->txn (concat (list retract-fn eid) kvs))))

(defn dissoc! [& args]
  (transact! [(apply dissoc args)]))

(defn merge
  "Merges in @props to @eid."
  [eid props]
  (-> props (c/assoc :db/id eid) validated->txn))

(defn merge!
  "Merges in @props to @eid and transacts."
  ([& args]
    (transact! [(apply merge args)])))

(defn new-or-merge
  "If @id-query does not find an id, creates a new entity with the supplied @attrs.
   Otherwise, merges in @attrs to the found id.
   Expects @id-query to return a single id."
  ([        id-query attrs] (new-or-merge        @part* id-query attrs))
  ([   part id-query attrs] (new-or-merge @conn* part   id-query attrs))
  ([db part id-query attrs]
    (c/assoc attrs :db/id
      `(:fn/or (:fn/first (:fn/q ~id-query))
               ~(tempid db part)))))

#?(:clj
(defn excise ; TODO this isn't right — see http://docs.datomic.com/excision.html
  ([        eid attrs] (excise        @part* eid attrs))
  ([   part eid attrs] (excise @conn* part   eid attrs))
  ([db part eid attrs]
    {:db/id           (tempid db part)
     :db/excise       eid
     :db.excise/attrs attrs})))

#?(:clj
(defn excise!
  ([          eid attrs]                 (excise!        @part* eid attrs))
  ([     part eid attrs]                 (excise! @conn* part   eid attrs))
  ([conn part eid attrs] (transact! conn (excise  conn   part   eid attrs)))))

(defn conj
  "Creates, but does not transact, an entity from the supplied attribute-map."
  ([x]      (conj (->db) (or @part* #?(:cljs :db.part/test)) x))
  ([part x] (conj (->db) part x))
  ([db part x]
    (let [{:keys [v has-dbfn-call?]} (transform-validated x)
          txn-op (whenf v (fn-not dbfn-call?)
                   (fn1 coll/assoc-default :db/id (tempid db part)))]
      (if has-dbfn-call? (?wrap-transform [txn-op]) txn-op))))

(defn conj!
  "Creates and transacts an entity from the supplied attribute-map."
  [& args]
  (transact! [(apply conj args)]))

(defn disj
  ([x] (disj @conn* x))
  ([conn x] ; x might be eid or query
    (let [{:keys [v has-dbfn-call?]} (transform-validated x)]
      ((if has-dbfn-call? ?wrap-transform identity) `(:db.fn/retractEntity ~v)))))

(defn disj! [& args]
  (transact! [(apply disj args)]))

(defn update
  "TODO currently not an atomic operation.
   Use database functions to achieve atomicity."
  ([eid k f]    (update @conn* eid k f))
  ([conn eid k f]
    (let [v-f (get (entity @conn eid) k)]
      (assoc eid k (f v-f)))))

(defn update!
  [& args]
  (transact! [(apply update args)]))

#?(:clj
(defmacro dbfn
  "Used for defining, but not transacting, a database function.

   Is not supported by DataScript (is Datomic specific)."
  [& args]
  (let [[prelists body] [(take-while vector? args) (drop-while vector? args)]
        [requires imports arglist]
         (case (count prelists)
           1 [[] [] (nth prelists 0)]
           2 [(nth prelists 0) [] (nth prelists 1)]
           3 prelists
           (throw (->ex "More vectors than [requires, imports, arglist] found for dbfn" (kw-map args))))]
    `(db/function
       '{:lang     :clojure
         :requires ~(into ['[datomic.api :as api]] requires)
         :imports  ~imports
         :params   ~arglist
         :code     (do ~@body)}))))

#?(:clj
(defmacro defn!
  "Used for defining and transacting a database function.

   Is not supported by DataScript."
  {:usage '(defn! inc [n] (inc n))}
  [conn sym & args]
  `(let [conn# ~conn]
     (transact! conn#
       [(conj conn# :db.part/fn
          {:db/ident (c/keyword "fn" (name '~sym))
           :db/fn    (dbfn ~@args)})]))))

#?(:clj
(defn entity-history [e]
  (q '[:find ?e ?a ?v ?tx ?added
       :in $ ?e
       :where [?e ?a ?v ?tx ?added]]
    (db/history (->db @conn*))
    e)))

#?(:clj
(defn entity-history-by-txn
  {:usage '(entity-history-by-txn (lookup :task:short-description :get-all-project-cljs))}
  [e]
  (->> (entity-history e)
       (map+ #(update % 1 (fn-> entity :db/ident)))
       (group-by+ #(get % 3))
       (join (sorted-map))
       (map val))))

; ==== TRANSACTIONS ====

; TODO move these?
#?(:clj
(defn error:tx-timed-out? [^Throwable x]
  (or (-> x ex-data :db/error (= :db.error/transaction-timeout))
      (and (.getCause x)
           (recur (.getCause x))))))

#?(:clj
(defn error:txr-unavailable? [^Throwable x]
  (or (-> x ex-data :db/error (= :db.error/transactor-unavailable))
      (and (.getCause x)
           (recur (.getCause x))))))

#?(:clj
(defn error:unique-conflict? [^Throwable x]
  (-> x .getCause ex-data :db/error (= :db.error/unique-conflict))))

#?(:clj
(defn tx-pipeline!
  "Transacts transaction data from from-ch. Returns a `reify` on which you can call
   - `take!` to get the return channel which yields an error, t, or {:completed n}
   - `offer!` to the resulting promise to terminate early.
   The concurrency can be modified via the `conc` parameter.
   This is a stoppable pipeline.
   `xf` is a single-element transforming function that preprocesses, filters out, etc.
   a transaction datum before being transacted.

   Note that, from http://docs.datomic.com/capacity.html:
   \"Pipelining 20 transactions at a time with 100 datoms per transaction is a good
   starting point for efficient imports.\"
   However, as per Paul DeGrandis:
   'The \"100 datoms\" rule of thumb applies to large-ish string data, not little datoms
   like longs, keywords, or such. You can try very high numbers of datoms per transaction
   (e.g. 50K, but no more than 100K). The real limiting factor is the byte size.'
   The takeaway is, the optimal batch rate is best determined experimentally."
  {:inspired-by "http://docs.datomic.com/best-practices.html#pipeline-transactions"}
  ([conn conc from-ch                                    ] (tx-pipeline! conn conc from-ch nil))
  ([conn conc from-ch report-ch                          ] (tx-pipeline! conn conc from-ch report-ch nil))
  ([conn conc from-ch report-ch failures-ch              ] (tx-pipeline! conn conc from-ch report-ch failures-ch identity))
  ([conn conc from-ch report-ch failures-ch xf           ] (tx-pipeline! conn conc from-ch report-ch failures-ch xf nil))
  ([conn conc from-ch report-ch failures-ch xf ex-handler] (tx-pipeline! conn conc from-ch report-ch failures-ch xf ex-handler false))
  ([conn conc from-ch report-ch failures-ch xf ex-handler sync?]
    (let [xf (or xf identity)
          transact-tx!
            (fn [tx]
              (catch-all @((if sync? db/transact db/transact-async) conn (xf tx))
                e (if ex-handler
                      (ex-handler e tx failures-ch)
                      (when failures-ch (do (async/put! failures-ch (async/->PipelineFailure e tx))) nil))))
          transact-txs! (comp (map+ transact-tx!) (remove+ nil?))]
      (if report-ch
          (async/pipeline!*    :!! conc from-ch transact-txs! report-ch false nil)
          (async/concur-each!* :!! conc from-ch transact-txs! nil))))))

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
      (validate history t/atom?)
      (when (contains? @history)
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

; (defn txns
;   "Returns all Datomic database transactions in the log."
;   [^Database db]
;   (->> (:conn db) db/log
;        (<- db/tx-range nil nil)
;        seq))

; (defn dissoc+
;   [id k] ; TODO fix to be more like clojure.core/dissoc
;   [:fn/retract-except id k []])

; (defn update! [id f-key & args]
;   (transact! (apply vector (c/keyword "fn" (name f-key)) id args)))

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
;   (red-for [k v data
;             entity-n (->entity*
;                        pk-schema pk-val
;                        db-partition
;                        {})]
;     (cond
;       (nil? v)
;         entity-n
;       (get handlers k)
;         ((get handlers k) data entity-n)
;       :else
;         (logic/if-let [datomic-key (get translation-table k)
;                        valid?      (val? v)]
;           (assoc entity-n datomic-key v)
;           entity-n))))


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
;                                      (contains? eids#)))
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
;                      (when (contains? to-val#)
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
;                    (log/pr (c/keyword (-> ~*ns* ns-name name) "debug") "Completed caching."))
;                  (defonce
;                    ^{:doc "Spawns cacher thread."}
;                    ~cacher-sym
;                    (quantum.db.datomic/gen-cacher
;                      (c/keyword (name (ns-name ~*ns*))
;                               (name '~cacher-sym))
;                      (var ~cache-fn-sym))))))
;         (throw+ (Err. nil "Memoization type not recognized." ~memo-type))))))

; (defn potential-problems-in-cache [cache]
;   (->> cache
;        (coll/remove-vals+ (fn= :db))
;        filter-realized-delays+
;        redv))

; (defn restart-cacher!
;   "Restarts the cacher for the |defmemoized| fn."
;   [base-sym]
;   (let [cacher-sym (gen-cacher-sym base-sym)
;         cacher-kw  (c/keyword (namespace cacher-sym) (name cacher-sym))]
;     (if (or (not (contains? @thread/reg-threads cacher-kw))
;             (first (thread/force-close-threads! (fn= cacher-kw))))
;         (do (reset-var! (resolve cacher-sym)
;               (gen-cacher
;                 (c/keyword (name (ns-name *ns*))
;                          (name cacher-sym))
;                 (resolve (gen-cache-fn-sym base-sym))))
;             true)
;         false)))

; (defn ->hidden-key [k]
;   (c/keyword (namespace k)
;            (-> k name (str ".hidden?"))))

#_(:clj
(defn txn-ids-affecting-eid
  "Returns a set of entity ids of transactions affecting @entity-id."
  {:from "http://dbs-are-fn.com/2013/datomic_history_of_an_entity/"}
  [entity-id]
  (->> (query*
         '[:find  ?tx
           :in    $ ?e
           :where [?e _ _ ?tx]]
         (db/history (db*))
         entity-id)
       (coll/map+ #(first %1))
       (join #{}))))

(defn externalize-inner-entities
  "Postwalks a transaction and makes sure each map (inner entity) has a :db/id.
   Recursively 'externalizes' these inner entities, ensuring that these children
   are transacted before their parents. These 'externalized' entities are given ids,
   and the previously inner entities are replaced with these ids."
  {:example `{[{:db/id -99 :a {:b 1}}]
              [{:b 1 :db/id -100}
               {:db/id -99 :a -100}]}}
  [txn part]
  (let [to-conj (volatile! [])
        txn-replaced
          (postwalk
            (whenf1 map?
              (whenf1 (fn-not :db/id)
                (fn [m]
                  (let [eid (tempid part)]
                    (vswap! to-conj c/conj (c/assoc m :db/id eid))
                    eid))))
            txn)]
    (coll/concatv @to-conj txn-replaced)))


#?(:clj
(defn transact-if!
  "Transacts the transaction @txn.
   If the constraints specified within @txn (if any, e.g. via :fn/fail-when-exists)
   are met, returns a map of:
   {:successful? <boolean>
    :result      |result|}
   where the value of :successful? refers to whether the transaction was executed successfully,
   and |result| refers to
     1) if successful, the results of the transaction, with the filter
        @filter-fn applied;
     2) if unsuccessful, the exception data which caused the transaction to fail, unless
        a) @ex-data-pred does not match the cause of the transaction failure."
  {:example '(let [id "1"]
               (transact-if!
                 [(db/conj
                    (->media:track
                      {:cloud:amazon:id
                        `(:fn/fail-when-found
                           [:find ~'?e
                            :where [~'?e :cloud:amazon:id ~id]]
                           "ID already exists in database"
                           ~id)
                        }))]
                 (fn-> :cloud:amazon:id (= id))
                 (fn-> :type (= :already-exists))))}
  [txn filter-fn ex-data-pred]
  (try (let [txn-result (transact! txn)
             txn-data (->> txn-result second force :tx-data (join []))
             txn-eid  (->> txn-data ^datomic.Datom first .e)
             db-as-of-txn-time-t (datomic.api/as-of (->db) txn-eid)]
         {:successful? true
          :result (->> txn-data
                       (map (fn [^datomic.Datom x] (->> x .e (datomic.api/entity db-as-of-txn-time-t))))
                       (coll/filter filter-fn))})
     (catch Throwable e
       (if-let [cause (-> e .getCause)
                _ (instance? clojure.lang.ExceptionInfo cause)
                _ (-> cause ex-data ex-data-pred)] ; ensures that the exception matches the constraint
         (-> cause ex-data
             (assoc :successful? false))
         ; Throws if there was a different problem unrelated to the expected exception constraint
         (throw e))))))

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
;        (join [])
;        (<- whenp show-entity-maps?
;            (fn->> (postwalk (whenf1 (partial instance? datomic.query.EntityMap)
;                               (fn->> (join {}))))))))


#?(:clj
(defn rollback!
  "Reassert retracted datoms and retract asserted datoms in a transaction,
  effectively \"undoing\" the transaction.

  WARNING: *very* naive function!
  TODO make `rollback` function which uses `with`"
  {:origin "Francis Avila"}
  [conn tx-id]
  (let [tx-log (-> conn datomic.api/log
                   (datomic.api/tx-range tx-id nil) first) ; find the transaction
        tx-eid   (-> tx-log :t datomic.api/t->tx) ; get the transaction entity id
        newdata (->> tx-log :data  ; get the datoms from the transaction
                     (remove (fn-> :e (= tx-eid))) ; remove transaction-metadata datoms
                     ; invert the datoms add/retract state.
                     (map #(do [(if (:added %) :db/retract :db/add) (:e %) (:a %) (:v %)]))
                     reverse)] ; reverse order of inverted datoms.
    (transact! conn newdata))))
