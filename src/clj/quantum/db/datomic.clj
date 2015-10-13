(ns quantum.db.datomic
  (:refer-clojure :exclude [assoc!])
  (:require-quantum [:lib])
  (:require [datomic.api :as d]
    [quantum.deploy.amazon :as amz])
  (:import datomic.Peer [datomic.peer LocalConnection Connection]))

#_(doseq [logger (->> (ch.qos.logback.classic.util.ContextSelectorStaticBinder/getSingleton)
                    (.getContextSelector)
                    (.getLoggerContext)
                    (.getLoggerList))]
  (.setLevel logger (. ch.qos.logback.classic.Level WARN)))

; With help from http://docs.datomic.com/getting-started.html

(swap! pr/blacklist conj datomic.db.Db)

(defn db-uri [db-name table-name]
  (str "datomic:ddb://" @amz/server-region
       "/" db-name
       "/" table-name
       "?aws_access_key_id=" @amz/aws-id
       "&aws_secret_key="    @amz/aws-secret))
;(Peer/createDatabase @uri) ; only once ever 
(defonce conn (atom nil))

(defn connect!
  ;(def memuri "datomic:mem://hello")
  ;(def memconn (Peer/connect local-uri))
  [db-name table-name]
  (reset! conn (d/connect (db-uri db-name table-name))))

; datom = ["add fact" "about a new entity with this temporary id"
;          "assert that the attribute db/doc" "has the value hello world"]

; Current value of the database
;(.db conn)

; [:find '?entity :where ['?entity :db/doc "hello world"]]
;["find" "entities" "where we specify entities as" 
;["an entity" "has the attribute db/doc" "with value hello world"]]

(defn db*       [ ] (d/db @conn))
(defn query
  ([q] (query q false))
  ([q raw?]
  (if raw?
      (d/q q (db*))
      (->> (d/q q (db*)) (into #{})))))

(defnt transact!
  ([^vector? trans]
    (let [txn @(d/transact @conn trans)]
      [true (delay txn)]))
  ([^string? str-path]
    (->> str-path (io/read :read-method :str :path)
         read-string
         transact!)))

(defnt transact-async!
  ([^vector? trans]
    (let [txn @(d/transact-async @conn trans)]
      [true (delay txn)]))
  ([^string? str-path]
    (->> str-path (io/read :read-method :str :path)
         read-string
         transact-async!)))

(defn read-transact! [path] (-> path io/file-str transact!))

(def entity? (partial instance? datomic.query.EntityMap))

(defn ^datomic.query.EntityMap entity
  "Retrieves the data associated with a (long) @id."
  [id]
  (d/entity (db*) id))

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

(defmacro call! [f & args]
  `(transact! [[f ~@args]]))

(defn schema
  {:usage '(schema :string :person.name/family-name :one {:doc "nodoc"})}
  ([val-type ident cardinality]
    (schema val-type ident cardinality {}))
  ([val-type ident cardinality opts]
    (with-throw (in? val-type allowed-types) (Err. nil "Val-type not recognized." val-type))
    (let [cardinality-f
            (condp = cardinality
              :one  :db.cardinality/one
              :many :db.cardinality/many
              (throw+ (Err. nil "Cardinality not recognized:" cardinality)))
          part-f
            (or (:part opts) :db.part/db)]
      (->> {:db/id                 (d/tempid part-f)
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
           (coll/filter-vals+ nnil?)
           redm))))

(defn rename [old new-]
  [{:db/id    old
    :db/ident new-}])

(defn rename-schemas [mapping]
  (for [oldv newv mapping]
    {:db/id oldv :db/ident newv}))

(defn add-partition! [part-name]
  (transact! [{:db/id                 (d/tempid :db.part/db)
               :db/ident              part-name
               :db.install/_partition :db.part/db}]))

(defn add-schema! [& args] (transact! [(apply schema args)]))

(defn schemas []
  (query '[:find [?ident ...]
           :where [_ :db/ident ?ident]]))

(defn attributes []
  (query '[:find [?ident ...]
           :where
           [?e :db/ident ?ident]
           [_ :db.install/attribute ?e]]))

(ns-unmap 'quantum.db.datomic 'assoc!)

(defn assoc! [id & kvs]
  (let [q (into [:db/add id] kvs)]
    (transact! [q])))

(ns-unmap 'quantum.db.datomic 'dissoc!)

(defn dissoc! [id & kvs]
  (let [q (into [:db/retract id] kvs)]
    (transact! [q])))

(ns-unmap 'quantum.db.datomic 'update!)

(defn update! [id f-key & args]
  (transact! (apply vector (keyword "fn" (name f-key)) id args)))

(defn create-entity!
  ([props] (create-entity! nil props))
  ([part props]
    (let [datom (merge {:db/id (d/tempid (or part :db.part/test))} props)]
      (transact! [datom]))))

(defmacro defn!
  {:usage '(defn! :part/quanta inc [n] (inc n))}
  [part sym-key arglist & args]
  `(create-entity! ~part
     {:db/ident (keyword "fn" (name '~sym-key))
      :db/fn    
        (datomic.api/function
          '{:lang     "clojure"
            :params   ~arglist
            :code     (do ~@args)})}))

(defn+ entity-query [q]
  (->> (query q)
       (map+ (f*n first))
       (map+ (extern (fn [id] [id (entity id)])))
       redm))

(defn partitions []
  (->> (query '[:find ?ident
           :where [:db.part/db :db.install/partition ?p]
                  [?p :db/ident ?ident]])
       (map+ (f*n first))
       (into #{})))

#_(do
  (query '[:find   ?entity
           :where [?entity :db/doc "hello world"]])
  (query '[:find ?c :where [?c :community/name]])

  ; Next, add a fact to the system.
  ; The following transaction will create a new entity with the doc string "hello world":

  (let [datom [:db/add (d/tempid :db.part/user)
               :db/doc "hello world"]]
    (transact! [datom])
    nil)
)

; http://docs.datomic.com/best-practices.html#pipeline-transactions
(def ^:const recommended-txn-ct 100)

(defn batch-transact!
  {:todo ["Handle errors better"]}
  [data-0 ->entity-fn post-fn]
  (when-let [data data-0]
    (do (doseq [entities (->> data
                              (partition-all recommended-txn-ct)
                              (map (fn->> (map+ ->entity-fn)
                                          (remove+ nil?)
                                          redv)))]
          (try+ (transact-async! entities)
                (post-fn entities) ; If it's async, how can there be an error?
            (catch Object e
              (if ((fn-and map? (fn-> :db/error (= :db.error/unique-conflict)))
                    e)
                  (post-fn entities)
                  (throw+ (Err. nil "Cache failed for transaction"
                                {:exception e :txn-entities entities})))))))))

; pk = primary-key
; sk = secondary-key
(defn primary-key-in-db? [pk-schema pk-val]
  (nempty? (query [:find '?entity
                   :where ['?entity pk-schema pk-val]]
                  true)))

(defn secondary-key-in-db?
  {:usage '(secondary-key-in-db? :twitter/id user-id :twitter/name)}
  [pk-schema pk-val sk-schema]
  (nempty?
    (query [:find '(count ?entity)
            :where ['?entity pk-schema pk-val]
                   ['?entity sk-schema '?prop]]
           true)))

(defn add-entity-if*
  ; Primary key only
  ([[pk-schema pk-val :as pk]           db-partition properties cache]
    (let [[pk-schema pk-val] pk]
      (cond 
        (and (type/atom? cache) (contains? @cache pk-val))
          nil
        (primary-key-in-db? pk-schema pk-val)
          (when (type/atom? cache)
            (conj! cache pk-val)
            (when (nempty? properties)
              (merge {:db/id pk} properties)))
        :else
          (merge
            {:db/id (datomic.api/tempid db-partition)
             pk-schema pk-val}
            properties))))
  ; Secondary key as well
  ([[pk-schema pk-val :as pk] sk-schema db-partition properties cache]
    (let [[pk-schema pk-val] pk]
      (if (primary-key-in-db? pk-schema pk-val)
          (cond 
            (and (type/atom? cache) (contains? @cache pk-val))
              nil
            (secondary-key-in-db? pk-schema pk-val sk-schema)
              (when (type/atom? cache)
                (conj! cache pk-val)
                nil)
            :else
              (merge {:db/id pk} properties))
          (merge
            {:db/id (datomic.api/tempid db-partition)
             pk-schema pk-val}
            properties)))))

(defnt add-entity-if
  "Adds the entity if it doesn't exist in the database via a primary key lookup"
  {:usage '(add-entity-if [:twitter/id user-id] {:twitter/followers followers})}
  ([^vector? pk sk-schema db-partition cache properties]
    (add-entity-if* pk sk-schema db-partition
      (merge {(first pk) (second pk)} properties)
      cache))
  ([^integer? pk sk-schema db-partition cache properties]
    (add-entity-if* [:db/id pk] sk-schema db-partition properties cache)))


(defn ->entity
  {:usage '(->entity
             :twitter/user.id
             user-id
             :twitter/user.name
             db-partition
             user-meta
             scache/user-ids:metas-in-db
             twitter-key=>datomic-key
             {})}
  [pk-schema pk-val sk-schema db-partition data cache translation-table handlers]
  (seq-loop [k v data
             entity-n (add-entity-if
                        [pk-schema pk-val]
                        sk-schema
                        db-partition
                        cache
                        {})]
    (cond
      (nil? entity-n)
        (break nil)
      (nil? v)
        entity-n
      (get handlers k)
        ((get handlers k) data entity-n)
      :else
        (logic/if-let [datomic-key (get translation-table k)
                       valid?      (nnil? v)]
          (assoc entity-n datomic-key v)
          entity-n))))

; You can transact multiple values for a :db.cardinality/many attribute at one time using a set.


; If an attributes points to another entity through a cardinality-many attribute, get will return a Set of entity instances. The following example returns all the entities that Jane likes:

; // empty, given the example data
; peopleJaneLikes = jane.get(":person/likes")
; If you precede an attribute's local name with an underscore (_), get will navigate backwards, returning the Set of entities that point to the current entity. The following example returns all the entities who like Jane:
; 
; // returns set containing John
; peopleWhoLikeJane = jane.get(":person/_likes")


; DOS AND DONT'S: http://martintrojer.github.io/clojure/2015/06/03/datomic-dos-and-donts/

; DO

; Keep metrics on your query times
; Datomic lacks query planning. Queries that look harmless can be real hogs.
; The solution is usually blindly swapping lines in your query until you get an order of magnitude speedup.

; Always use memcached with Datomic
; When new peers connect, a fair bit of data needs to be transferred to them.
; If you don't use memcached this data needs to be queried from the store and will slow down the 'peer connect time' (among other things).

; Give your peers nodes plenty of heap
; To fully benefit from Datomic you want to have plenty of heap for the peers to cache data.

; Datomic was designed with AWS/Dynamo in mind, use it
; It will perform best with this backend, its also the most used (and thus most polished).

; Prefer dropping databases to excising data
; If you want to keep logs, or other data with short lifespan in Datomic,
; put them in a different database and rotate the databases on a daily / weekly basis.

; Use migrators for your attributes, and consider squashing unused attributes before going to prod
; Don't be afraid to rev the schemas, you will end up with quite a few unused attributes.
; It's OK, but squash them before its too late.

; Trigger transactor GCs periodically when load is low
; If you are churning many datoms, the transactor is going have to GC. When this happens writes will be very slow.

; Consider introducing a Datomic/peer tier in your infrastructure
; Since Datomic's licensing is peer-count limited, you might have to start putting
; your peers together in a Datomic-tier which the webserver nodes (etc) queries via the Datomic REST API.

; DON'T
; Don't put big strings into Datomic
; If your strings are bigger than 1kb put them somewhere else (and keep a link to them in Datomic).
; Datomic's storage model is optimized for small datoms, so if you put big stuff in there perf will drop dramatically.

; Don't load huge datasets into Datomic
; It will take forever, with plenty transactor GCs.
; If you are using Dynamo, keep an eye on the write throughput since it might bankrupt you.
; Also, there is a limit to the number of datoms Datomic can handle.

; Don't use Datomic for stuff it wasn't intended to do
; Don't run your geospatial queries or query-with-aggregations in Datomic, it's OK to have multiple datastores in your system.