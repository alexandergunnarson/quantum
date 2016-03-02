(ns ^{:doc "Reactive Datomic/DataScript functions.
            Mostly taken from mpdairy/posh (distributed under EPL >= 1)
            and ported to CLJC."}
  quantum.db.datomic.reactive.core
  (:require-quantum [:core async log core-async])
  (:require [com.stuartsierra.component              :as component ]
   #?(:cljs [reagent.core                            :as rx        ])
            [quantum.core.collections                :as coll      ]
            [quantum.db.datomic.core                 :as db        ]
            [datascript.core                         :as mdb       ]
            [quantum.db.datomic.reactive.datom-match
              :refer [datom-match? any-datoms-match? query-symbol?]]
            [quantum.db.datomic.reactive.pattern-gen :as pgen      ])
  #?(:cljs
  (:require-macros 
            [reagent.ratom :refer [reaction]])))

; TODO fix
#?(:clj (def reaction identity))

(def rx-conns (atom {}))

(declare try-after-tx)


; listener ids -> go-channels
(defonce listeners (atom {}))
; (swap! listeners assoc :listener0 (chan 10000))
; (-> @listeners :listener0 <!!)

(defonce ^{:doc "Global variable to ensure that no more than one TransactionListener runs
                 at any given time.

                 This is because Datomic's transaction report queue is mutable and consumable
                 and should only be consumed by one thread if data is not to be lost."}
  txn-listener-running? (atom false))

#?(:clj
(defrecord
  TransactionListener [interrupted?]
  component/Lifecycle
    (start [this]
      (try 
        (reset! txn-listener-running? true)
        (let [interrupted? (atom false)]
          (go
            (let [^java.util.concurrent.BlockingQueue q (datomic.api/tx-report-queue @db/conn*)] 
              (while (not @interrupted?)
                (let [txn (.take q)]
                  (doseq [[listener-id listener-chan] @listeners]
                    (core-async/put! listener-chan txn))))
              (log/pr :warn "Transaction listener complete.")))
          (assoc this :interrupted? interrupted?))
        (catch Throwable e (reset! txn-listener-running? false))))
    (stop [this]
      (reset! interrupted? true)
      (reset! txn-listener-running? false)
      this)))

(defn react!
  "Makes a Datomic or DataScript connection @conn reactive."
  [conn]
  (swap! rx-conns merge {conn {:last-tx-report (#?(:clj atom :cljs rx/atom) [])
                               :conn           (atom conn)
                               :after-tx       (atom [])
                               :before-tx      (atom [])}})
  (cond
    (db/mconn? conn)
    (mdb/listen! @(:conn (@rx-conns conn)) :history
               (fn [tx-report]
                 (do
                   ;;(println (pr-str (:tx-data tx-report)))
                   (doall
                    (for [tx-datom (:tx-data tx-report)
                          after-tx @(:after-tx (@rx-conns conn))]
                      (try-after-tx (:db-before tx-report)
                                    (:db-after tx-report)
                                    tx-datom after-tx)))
                   (reset! (:last-tx-report (@rx-conns conn)) tx-report)))))

    (db/conn?))

;==================================================================
; Transact

; might have to make this something that combines the txs or adds
; filters or something. For now it's sort of pointless.

; TODO should be reactive for CLJ too 
(def transactions-buffer (#?(:clj atom :cljs rx/atom) {}))

(defn split-tx-map [tx-map]
  (if (map? tx-map)
    (let [id (:db/id tx-map)]
      (map (fn [[k v]] [:db/add id k v]) (dissoc tx-map :db/id)))
    [tx-map]))

(defn clean-tx [tx]
  (apply concat (map split-tx-map tx)))

(defn transact! [conn tx]
  (swap! transactions-buffer
         #(update % conn (comp vec (partial concat (clean-tx tx)))))
  [:span])

(declare try-all-before-tx!)

(defn do-transaction! [conn]
  (let [tx (@transactions-buffer conn)]
    (when tx
      (let [_  (try-all-before-tx! conn tx)
            tx (@transactions-buffer conn)]
        (swap! transactions-buffer #(dissoc % conn))
        (db/transact! conn tx)))))

(defn update-transactions! []
  (doall (map (fn [[conn]] (do-transaction! conn)) @transactions-buffer)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; reactions

;; all of these memoize based on the pattern, so don't use anonymous
;; functions unless you are just going to call the reaction builder once

(def established-reactions (atom {}))

;; db-tx
;; returns :db-after of any new tx that matches pattern

(defn db-tx [conn patterns]
  (if-let [r (@established-reactions [:db-tx conn patterns])]
    r
    (let [new-reaction
          (let [saved-db (atom (db/->db conn))]
            (reaction
             (if (any-datoms-match? (:db-before @(:last-tx-report (@rx-conns conn)))
                                    patterns 
                                    (:tx-data @(:last-tx-report (@rx-conns conn))))
               (reset! saved-db (:db-after @(:last-tx-report (@rx-conns conn))))
               @saved-db)))]
      (swap! established-reactions merge
             {[:db-tx conn patterns] new-reaction})
      new-reaction)))

; e.g. db/pull
(defn build-pull [db pull-syntax entity vars]
  (db/pull db
           (if (empty? vars)
             pull-syntax
             (coll/deep-map #(or (vars %) %) pull-syntax))
           (or (vars entity) entity)))

(defn pull-tx [conn patterns pull-pattern entity-id]
  (if-let [r (@established-reactions [:pull-tx conn patterns pull-pattern entity-id])]
    r
    (let [patterns (or patterns
                       (pgen/pull-pattern-gen pull-pattern entity-id))
          new-reaction
          (let [saved-pull (atom (when (not (or (query-symbol? entity-id)
                                                (coll/deep-find query-symbol? pull-pattern)))
                                   (db/pull (db/->db conn) pull-pattern entity-id)))]
            (reaction
             (if-let [vars (any-datoms-match?
                            (:db-before @(:last-tx-report (@rx-conns conn)))
                            patterns
                            (:tx-data @(:last-tx-report (@rx-conns conn))))]
               (let [new-pull (build-pull (:db-after @(:last-tx-report (@rx-conns conn)))
                                          pull-pattern entity-id vars)]
                 (if (not= @saved-pull new-pull)
                   (reset! saved-pull new-pull)
                   @saved-pull))
               @saved-pull)))]
      (swap! established-reactions merge
             {[:pull-tx conn patterns pull-pattern entity-id] new-reaction})
      new-reaction)))

(defn pull [conn pull-pattern entity-id]
  (pull-tx conn
           (pgen/pull-pattern-gen pull-pattern entity-id)
           pull-pattern entity-id))

(defn build-query [db q args]
  (apply (partial db/q q)
         (cons db (or args []))))

; in the future this will return some restricting tx patterns

(defn q-tx [conn patterns query & args]
  (if-let [r (@established-reactions [:q-tx conn patterns query args])]
    r
    (let [patterns (or patterns
                       (pgen/q-pattern-gen query args))
          new-reaction
          (let [saved-q    (atom (if (empty? (filter query-symbol? args))
                                     (build-query (db/->db conn) query args)
                                     #{}))]
            (reaction
             (if-let [vars (any-datoms-match?
                            (:db-before @(:last-tx-report (@rx-conns conn)))
                            patterns
                            (:tx-data   @(:last-tx-report (@rx-conns conn))))]
               (let [new-q (build-query (:db-after @(:last-tx-report (@rx-conns conn)))
                                        query
                                        (map #(or (vars %) %) args))]
                 (if (not= @saved-q new-q)
                   (reset! saved-q new-q)
                   @saved-q))
               @saved-q)))]
      (swap! established-reactions merge
             {[:q-tx conn patterns query args] new-reaction})
      new-reaction)))

(declare q)
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; TX Listeners

;; listens for any patterns in the tx-log and runs handler-fn
;; handler-fn takes the args [matching-tx-datom db]

; e.g. db/q
(defn try-after-tx [db-before db-after tx-datom [patterns handler-fn]]
  (when (datom-match? db-before patterns tx-datom)
    (handler-fn tx-datom db-after)))

(defn try-before-tx [conn tx-datom [patterns handler-fn]]
  (when (datom-match? (db/->db conn) patterns tx-datom)
    (handler-fn tx-datom (db/->db conn))))

;;;; TODO: ADD FILTER-BEFORE-TX

(defn try-all-before-tx! [conn txs]
  (concat
   (remove
    nil?
    (doall
     (for [tx-datom txs
           before-tx @(:before-tx (@rx-conns conn))]
       (try-before-tx conn tx-datom before-tx))))
   txs))

(defn after-tx! [conn patterns handler-fn]
  (swap! (:after-tx (@rx-conns conn)) conj [patterns handler-fn]))

(defn before-tx! [conn patterns handler-fn]
  (swap! (:before-tx (@rx-conns conn)) conj [patterns handler-fn]))


(defn q [conn query & args]
  (apply q-tx conn nil query args))

; eventually this will be replaced with reagent's do-render:
#?(:cljs (js/setInterval update-transactions! 17))