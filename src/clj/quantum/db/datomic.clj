(ns quantum.db.datomic
  (:refer-clojure :exclude [assoc!])
  (:require-quantum [:lib])
  (:require [datomic.api :as d]
    [quantum.deploy.amazon :as amz])
  (:import datomic.Peer [datomic.peer LocalConnection Connection]
    java.util.concurrent.ConcurrentHashMap))

#_(doseq [logger (->> (ch.qos.logback.classic.util.ContextSelectorStaticBinder/getSingleton)
                    (.getContextSelector)
                    (.getLoggerContext)
                    (.getLoggerList))]
  (.setLevel logger (. ch.qos.logback.classic.Level WARN)))

; With help from http://docs.datomic.com/getting-started.html

(swap! pr/blacklist conj datomic.db.Db)

(defn db-uri [db-name table-name instance-name]
  (str "datomic:ddb://" (amz/get-server-region instance-name)
       "/" db-name
       "/" table-name
       "?aws_access_key_id=" (amz/get-aws-id     instance-name)
       "&aws_secret_key="    (amz/get-aws-secret instance-name)))
;(Peer/createDatabase @uri) ; only once ever 
(defonce conn (atom nil))

(defn connect!
  ;(def memuri "datomic:mem://hello")
  ;(def memconn (Peer/connect local-uri))
  [db-name table-name instance-name]
  (reset! conn (d/connect (db-uri db-name table-name instance-name))))

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

(defn excise! [id partition attrs]
  (transact!
    [{:db/id (d/tempid partition)
      :db/excise id
      :db.excise/attrs attrs}]))

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
  "Submits transactions synchronously to the Datomic transactor
   in asynchronous batches of |recommended-txn-ct|."
  {:todo ["Handle errors better"]}
  [data-0 ->entity-fn post-fn]
  (when-let [data data-0]
    (let [threads 
           (for [entities (->> data
                               (partition-all recommended-txn-ct)
                               (map (fn->> (map+ ->entity-fn)
                                           (remove+ nil?)
                                           redv)))]
             (go (try+ (transact! entities)
                       (post-fn entities)
                   (catch Object e
                     (if ((fn-and map? (fn-> :db/error (= :db.error/unique-conflict)))
                           e)
                         (post-fn entities)
                         (throw+ (Err. nil "Cache failed for transaction"
                                       {:exception e :txn-entities entities})))))))]
      (->> threads (map (MWA clojure.core.async/<!!)) doall))))

; pk = primary-key
(defn primary-key-in-db?
  "Determines whether, e.g.,
   :twitter/user.id 8573181 exists in the database."
  [pk-schema pk-val]
  (nempty?
    (query [:find '?entity
            :where ['?entity pk-schema pk-val]]
           true)))

(defn ->entity*
  "Adds the entity if it its primary (unique) key is not present in the database.

   No longer implements caching because of possible sync inconsistencies
   and because it fails to provide a significant performance improvement."
  ; Primary key only
  ([pk-schema pk-val db-partition properties]
    (let [entity-id (ffirst (query [:find  '?eid
                                    :where ['?eid pk-schema pk-val]]))]
      (mergel
        {:db/id (or entity-id (datomic.api/tempid db-partition))
         pk-schema pk-val}
        properties))))

(defn ->entity
  {:usage '(->entity
             :twitter/user.id
             user-id
             :twitter/user.name
             db-partition
             user-meta
             twitter-key=>datomic-key)}
  [pk-schema pk-val db-partition data translation-table & [handlers]]
  (seq-loop [k v data
             entity-n (->entity*
                        pk-schema pk-val
                        db-partition
                        {})]
    (cond
      (nil? v)
        entity-n
      (get handlers k)
        ((get handlers k) data entity-n)
      :else
        (logic/if-let [datomic-key (get translation-table k)
                       valid?      (nnil? v)]
          (assoc entity-n datomic-key v)
          entity-n))))

(defn ->encrypted
  ; TODO can't cache without system property set so throw error
  {:performance "Takes ~160 ms per encryption. Slowwww..."}
  ([schema val-] (->encrypted schema val- true))
  ([schema val- entity?]
    (let [schema-ns   (namespace schema)
          schema-name (name      schema)
          result (crypto/encrypt :aes val-
                   {:password (System/getProperty (str "password:" schema-ns "/" schema-name))})]
      (if entity?
          (let [{:keys [encrypted salt key]} result]
            {schema             (->> encrypted (crypto/encode :base64-string))
             (keyword schema-ns (str schema-name ".k1")) key
             (keyword schema-ns (str schema-name ".k2")) salt})
          result))))

(defn ->decrypted
  {:performance "Probably slow"}
  ([schema val-] (->decrypted schema val- true false))
  ([schema encrypted entity? string-decode?]
    (let [schema-ns   (namespace schema)
          schema-name (name      schema)
          ;schema-key
          key- (if entity?
                   (get encrypted (keyword schema-ns (str schema-name ".k1")))
                   (:key encrypted))
          salt (if entity?
                   (get encrypted (keyword schema-ns (str schema-name ".k2")))
                   (:salt encrypted))
          ^"[B" result
            (crypto/decrypt :aes
              (->> encrypted
                   (<- get schema)
                   (crypto/decode :base64))
              {:key  key-
               :salt salt
               :password (System/getProperty (str "password:" schema-ns "/" schema-name))})]
      (if string-decode?
          (String. result java.nio.charset.StandardCharsets/ISO_8859_1)
          result))))

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

; Give your peers nodes plenty of heap space

; Datomic was designed with AWS/Dynamo in mind, use it
; It will perform best with this backend.

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

; Don't load huge datasets into Datomic. It will take forever, with plenty transactor GCs.
; Keep an eye on the DynamoDB write throughput since it might bankrupt you.
; Also, there is a limit to the number of datoms Datomic can handle.

; Don't use Datomic for stuff it wasn't intended to do
; Don't run your geospatial queries or query-with-aggregations in Datomic, it's OK to have multiple datastores in your system.

(defn gen-cacher [thread-id cache-fn-var]
  (async-loop {:id thread-id :type :thread} ; Spawn quickly, mainly not sleeping (hopefully)
    []
    (try+ (@cache-fn-var)
      (catch Err e
        ;(log/pr :warn e)
        (if (-> e :objs :exception :db/error (= :db.error/transactor-unavailable))
            (do #_(init-transactor!)
                (async/sleep 10000))
            (throw+))))
    ; Just in case there's nothing to cache
    (async/sleep 1000)
    (recur)))

(defn db-count [& clauses]
  (-> (query (into [:find '(count ?entity)
                       :where] clauses)
                true)
      flatten first))

(def filter-realized-delays+
  (fn->> (coll/filter-vals+ (fn-and delay? realized?))
         (coll/map-vals+    (fn [x] (try+ (deref x)
                                       (catch Object e e))))))

; TODO MOVE
(def error?  (fn-or (fn [x] (instance? Err       x)) ; fn-or with |partial| might have problems?
                    (fn [x] (instance? Throwable x))))

(defn- gen-cache-fn-sym [sym] (symbol (str "cache-" (name sym) "!"          )))
(defn- gen-cacher-sym   [sym] (symbol (str          (name sym) "-cacher"    )))


(defmacro defmemoized
  {:todo ["Make extensible"]}
  [memo-type opts cache-sym-0 name- & fn-args]
  (let [cache-sym (with-meta (or cache-sym-0 (symbol (str (name name-) "-cache*")))
                    {:tag "java.util.concurrent.ConcurrentHashMap"})
        f-0 (gensym)
        register-cache! (gensym)]
   `(let [opts# ~opts ; So it doesn't get copied in repetitively
          first-arg# (:first-arg-only? opts#)
          get-fn#   (or (:get-fn   opts#)
                        (when (:hashed? opts#)
                          (fn [m1# k1#    ] (.get         ^ConcurrentHashMap m1# k1#   ))))
          assoc-fn# (or (:assoc-fn opts#)
                        (when (:hashed? opts#)
                          (fn [m1# k1# v1#] (.putIfAbsent ^ConcurrentHashMap m1# k1# v1#))))
          registered-caches# (:cache-register opts#)
          _# (declare ~cache-sym)
          ~register-cache! (fn [] (when (and registered-caches# (not '~cache-sym-0))
                                    (coll/assoc! registered-caches# '~name- (var ~cache-sym))))
          ~f-0  (fn ~name- ~@fn-args)]
      (condp = ~memo-type
        :default (def ~name-
                   (memoize ~f-0 nil first-arg# get-fn# assoc-fn#))
        :map
          (let [memoized# (cache/memoize* ~f-0 (atom {}) first-arg# get-fn# assoc-fn#)]
            (def ~cache-sym (:m memoized#))
            (~register-cache!)
            (def ~name-     (:f memoized#)))
        :datomic ; TODO ensure function has exactly 1 arg
         ~(let [cache-fn-sym             (gen-cache-fn-sym name-)
                cacher-sym               (gen-cacher-sym   name-)
                entity-if-sym            (symbol (str          (name name-) "->entity-if"))
                pk-adder-sym             (symbol (str          (name name-) ":add-pk!"   ))]
            `(let [opts#               ~opts
                   memo-subtype#       (:type               opts#) ; :many or :one 
                   _# (throw-unless (in? memo-subtype# #{:one :many})
                        (Err. nil "Memoization subtype not recognized." memo-subtype#))
                   db-partition#       (:db-partition       opts#)
                   from-key#           (:from-key           opts#) ; e.g. :twitter/user.email. Must be unique
                   _# (throw-when (nil? from-key#) (Err. nil ":from-key required." opts#))
                   pk#                 (:pk                 opts#) ; e.g. :twitter/user.id
                   _# (throw-when (nil? pk#) (Err. nil ":pk (primary key) required." opts#))
                   in-db-cache-to-key# (:in-db-cache-to-key opts#) ; e.g. follower-ids in database. Used to speed up creation of entities
                   to-key#             (:to-key             opts#) ; e.g. follower ids
                   key-map#            (:key-map            opts#) ; key translation map
                   skip-keys#          (:skip-keys          opts#)
                   from-type#          (:from-type          opts#)
                   cache-errors?#      (if (contains? opts# :cache-errors?)
                                           (:cache-errors? opts#)
                                           true)
                   force-update?#      (:force-update?      opts#)
                   main-fn#
                    (fn [from-val# ignore-db-retrieve?#]
                      (let [get-from-val-state# #(.get ^ConcurrentHashMap ~cache-sym from-val#)
                            from-val-state-0#   (get-from-val-state#)]
                      (cond
                        (not (.containsKey ^ConcurrentHashMap ~cache-sym from-val#))
                          (if (and (not force-update?#) ; Once, then memoized
                                   (set? skip-keys#)
                                   (let [eids# (query ; Possibly slow?
                                                 (into [:find (list '~'count '~'?e) :where]
                                                   (conj [['~'?e from-key# from-val#] ['~'?e pk#]]
                                                     (apply list '~'or
                                                       (for [skip-key# skip-keys#] ; different symbols = OR
                                                         ['~'?e skip-key#])))))]
                                     (nempty? eids#)))
                              (do (.putIfAbsent ^ConcurrentHashMap ~cache-sym from-val# :db)
                                  :db)
                              (let [; Multiple threads get the same result from the same delayed function
                                    f# (delay (~f-0 from-val#))]
                                 (do (.putIfAbsent ^ConcurrentHashMap ~cache-sym
                                       from-val# f#)
                                     (try+ @(get-from-val-state#)
                                       (catch Object e#
                                         (when-not cache-errors?#
                                           (.remove ^ConcurrentHashMap ~cache-sym from-val#))
                                         (throw+))))))
                        (delay? from-val-state-0#)
                          @from-val-state-0#
                        (= from-val-state-0# :db)
                          (if ignore-db-retrieve?#
                              :db
                              (-> [:find '?e :where ['?e from-key# from-val#]]
                                 db/query first first db/entity))
                        :else
                          (throw+ (Err. nil "Unhandled exception in cache." from-val-state-0#)))))]
             (do ; The ".putIfAbsent" feature, to my knowledge, is not present in Clojure STM
                 ; Thus the ConcurrentHashMap
                 ; from-key-state-cache-sym
                 (when-not '~cache-sym-0
                   (defonce ~cache-sym (java.util.concurrent.ConcurrentHashMap.)))
                 (~register-cache!)

                 (defn ~name- 
                   "@from-val: E.g. email
                     to-val:   E.g. user metadata

                    Caching: 
                      If not present in cache, creates delay and derefs it.
                      Caches errors only if @cache-errors?"
                   [from-val# & [ignore-db-retrieve?#]]
                   (if (= from-type# :many)
                       (doseq [from-val-n# from-val#]
                         (main-fn# from-val-n# ignore-db-retrieve?#))
                       (main-fn# from-val# ignore-db-retrieve?#)))
                
                 (defn ~pk-adder-sym
                   "Add primary (+ unique) keys to database,
                    e.g. :twitter/user.id entities.
                    Basically an analogue of ensuring value exists before |update|."
                    {:todo ["Redundant and likely unnecessary"]}
                    [pk-vals#]
                   ;(swap! in-db-cache-pk# set/union @in-db-cache-full#)
                   (batch-transact!
                     pk-vals#
                     (fn [pk-val#]
                       (quantum.db.datomic/->entity*
                          pk# pk-val#
                          db-partition#
                          nil))
                     (fn [entities#]
                       #_(swap! in-db-cache-pk# into (map pk# entities#)))))

                 (defn ~entity-if-sym
                   "Used to create a database transaction
                    to add the entity if not present,
                    with supplied properties/schemas."
                   ([[pk-val# to-val#]] ; to-val <-> entity-data
                    (~entity-if-sym pk-val# to-val#))
                   ([pk-val# to-val#]
                     (when (nempty? to-val#)
                       (quantum.db.datomic/->entity
                         pk#
                         pk-val#
                         db-partition#
                         to-val#
                         key-map#))))

                 (defn ~cache-fn-sym
                  "Efficiently transfers from in-memory cache to 
                   database via |batch-transact!|."
                   []
                   (let [realizeds# (->> ~cache-sym
                                         filter-realized-delays+
                                         (coll/remove-vals+ (fn-or nil? error?))
                                         redm)]
                     (batch-transact!
                       realizeds#
                       ~entity-if-sym
                       (fn [entities#]
                         ; :db/error :db.error/entity-missing-db-id
                         ; Record that they're in the db 
                         ; TODO: Is this even necessary?
                         ;(swap! in-db-cache-to-key# into (map pk# entities#))
                         ;(swap! in-db-cache-pk# set/union @in-db-cache-to-key#)
                         ))

                     ; Cache offloading/purging
                     (doseq [from-val# to-val# realizeds#]
                       (.put ^ConcurrentHashMap ~cache-sym from-val# :db))
                     (let [failed# (->> ~cache-sym filter-realized-delays+ (coll/filter-vals+ error?) redm)]
                       (doseq [from-val# to-val# failed#]
                         (.remove ^ConcurrentHashMap ~cache-sym from-val#))))
                   (log/pr (keyword (-> ~*ns* ns-name name) "debug") "Completed caching."))
                 (defonce
                   ^{:doc "Spawns cacher thread."}
                   ~cacher-sym
                   (quantum.db.datomic/gen-cacher
                     (keyword (name (ns-name ~*ns*))
                              (name '~cacher-sym))
                     (var ~cache-fn-sym))))))
        (throw+ (Err. nil "Memoization type not recognized." ~memo-type))))))

(defn potential-problems-in-cache [cache]
  (->> cache
       (coll/remove-vals+ (eq? :db))
       filter-realized-delays+
       redv))

(defn restart-cacher! [base-sym]
  (let [cacher-sym (gen-cacher-sym base-sym)
        cacher-kw  (keyword (namespace cacher-sym) (name cacher-sym))]
    (if (or (not (contains? @thread/reg-threads cacher-kw))
            (first (thread/force-close-threads! (eq? cacher-kw))))
        (do (reset-var! (resolve cacher-sym)
              (gen-cacher
                (keyword (name (ns-name *ns*))
                         (name cacher-sym))
                (resolve (gen-cache-fn-sym base-sym))))
            true)
        false)))

(defn ->hidden-key [k]
  (keyword (namespace k)
           (-> k name (str ".hidden?"))))