(ns quantum.db.datomic
  (:refer-clojure :exclude [assoc!])
  (:require-quantum [:lib])
  (:require [datomic.api :as d])
  (:import datomic.Peer [datomic.peer LocalConnection Connection]))

(doseq [logger (->> (ch.qos.logback.classic.util.ContextSelectorStaticBinder/getSingleton)
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

(defonce uri (atom "datomic:sql://datomic?jdbc:postgresql://localhost:5431/datomic?user=datomic&password=datomic"))
;(Peer/createDatabase @uri) ; only once ever 
(defonce conn (atom (try (d/connect @uri) (catch java.util.concurrent.ExecutionException _ nil))))
(defonce last-trans (atom nil))


; datom = ["add fact" "about a new entity with this temporary id"
;          "assert that the attribute db/doc" "has the value hello world"]

; Current value of the database
;(.db conn)

; [:find '?entity :where ['?entity :db/doc "hello world"]]
;["find" "entities" "where we specify entities as" 
;["an entity" "has the attribute db/doc" "with value hello world"]]

(defn db*       [ ] (d/db @conn))
(defn raw-query [q] (d/q q (db*)))
(defn query     [q] (->> q raw-query (into #{})))

(defnt transact!
  vec?
    ([trans]
      (reset! last-trans (d/transact @conn trans))
      (deref @last-trans))
  string?
    ([str-path]
      (->> str-path (io/read :read-method :str :path)
           read-string
           transact!)))

(defn read-transact! [path] (-> path io/file-str transact!))

(def entity? (partial instance? datomic.query.EntityMap))

(defn ^datomic.query.EntityMap raw-entity
  "Retrieves the data associated with a (long) @id."
  [id]
  (d/entity (db*) id))

(def entity (fn->> raw-entity
                   (coll/prewalk
                     (whenf*n entity? (partial into {})))))

      ; because it starts off as a java.util.HashSet

(defn schema
  {:usage '(schema :string :person.name/family-name :one {:doc "nodoc"})}
  ([val-type ident cardinality]
    (schema val-type ident cardinality {}))
  ([val-type ident cardinality opts]
    (let [cardinality-f
            (condp = cardinality
              :one  :db.cardinality/one
              :many :db.cardinality/many
              (throw+ {:msg (str/sp "Cardinality not recognized:" cardinality)}))
          part-f
            (or (:part opts) :db.part/db)]
      (->> {:db/id                 (d/tempid part-f)
            :db/ident              ident
            :db/valueType          (->> val-type name (str "db.type/") keyword)
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
                  :db.install/_partition :db.part/db}]))

(defn add-schema! [& args] (transact! [(apply schema args)]))

(ns-unmap 'quantum.db.datomic 'assoc!)

(defn assoc! [id & kvs]
  (let [datom (into [:db/add id] kvs)]
    (transact! [datom])))

(defn create-entity!
  ([props] (create-entity! nil props))
  ([part props]
    (let [datom (into {:db/id (d/tempid (or part :test))} props)]
      (transact! [datom]))))

d

(defn+ entity-query [q]
  (->> (query q)
       (map+ first)
       (map+ (extern (fn [id] (map-entry id (entity id)))))
       redm))

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
