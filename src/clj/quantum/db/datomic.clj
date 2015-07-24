(ns quantum.db.datomic
  (:refer-clojure :exclude [assoc!])
  (:require-quantum [:lib])
  (:require [datomic.api :as d])
  (:import datomic.Peer [datomic.peer LocalConnection Connection]))

#_(doseq [logger (->> (ch.qos.logback.classic.util.ContextSelectorStaticBinder/getSingleton)
                    (.getContextSelector)
                    (.getLoggerContext)
                    (.getLoggerList))]
  (.setLevel logger (. ch.qos.logback.classic.Level WARN)))

; With help from http://docs.datomic.com/getting-started.html

; SETUP
; createuser -U alexandergunnarson -s -r -E postgres
; pg_ctl start -D  "/Users/alexandergunnarson/Development/Source Code Projects/quanta/resources/Database/postgres/" -o "-p 5431"
; ~/Downloads/datomic/bin/transactor "/Users/alexandergunnarson/Development/Source Code Projects/quanta/resources/Database/datomic/sql-transactor-template.properties" 
; END SETUP

;(def memuri "datomic:mem://hello")
;(def memconn (Peer/connect local-uri))

(swap! pr/blacklist conj datomic.db.Db)
(def connect d/connect)
(defonce uri  (atom nil))
;(Peer/createDatabase @uri) ; only once ever 
(defonce conn (atom nil))


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
  vec?
    ([trans]
      @(d/transact @conn trans))
  string?
    ([str-path]
      (->> str-path (io/read :read-method :str :path)
           read-string
           transact!)))

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

(defn add-partition! [part-name]
  (transact! [{:db/id                 (d/tempid :db.part/db)
               :db/ident              part-name
               :db.install/partition :db.part/db}]))

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
    (let [datom (into {:db/id (d/tempid (or part :test))} props)]
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
       (map+ first)
       (map+ (extern (fn [id] (map-entry id (entity id)))))
       redm))

(defn partitions []
  (->> (query '[:find ?ident
           :where [:db.part/db :db.install/partition ?p]
                  [?p :db/ident ?ident]])
       (map+ first)
       force (into #{})))

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
