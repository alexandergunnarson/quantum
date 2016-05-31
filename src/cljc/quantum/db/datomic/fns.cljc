(ns quantum.db.datomic.fns
  (:require [quantum.db.datomic.core :as db
              :refer [#?(:clj defn!)]      ]))

#?(:clj
(defn define-std-db-fns!
  "Transacts all the below 'standard' database functions into @db."
  []

  (db/transact! (db/->partition :db.part/fn))

  (defn! q      [[datomic.api :as api]] [db query] (api/q query db))
  (defn! fq     [[datomic.api :as api]] [db query] (ffirst (api/q query db)))
  (defn! first  []                      [db coll ] (first  coll))
  (defn! ffirst []                      [db coll ] (ffirst coll))
  (defn! nil?   []                      [db expr ] (nil? expr))
  (defn! nnil?  []                      [db expr ] (not (nil? expr)))

  ; MACROS

  (defn! apply-or
    [[datomic.api :as api]]
    ^{:macro? true
      :doc "Variadic |or|."}
    [db args]
    (loop [args-n args]
      (if-let [arg (->> args-n first (api/invoke db :fn/eval db))]
        arg
        (recur (rest args-n)))))

  (defn! or
    [[datomic.api :as api]]
    ^{:macro? true
      :doc "2-arity |or|."}
    [db alt0 alt1]
    (or (api/invoke db :fn/eval db alt0)
        (api/invoke db :fn/eval db alt1)))

  (defn! validate
    ^{:macro? true
      :doc    "|eval|s @expr. If the result satisifies @pred, returns @expr.
               Otherwise, throws an assertion error."}
    [[datomic.api :as api]]
    [db pred expr]
    (let [expr-eval (api/invoke db :fn/eval db expr)]
      (if (api/invoke db pred db expr-eval)
          expr-eval
          (throw (ex-info (str "Assertion not met: " pred)
                   {:pred pred :expr expr :expr-eval expr-eval})))))

  (defn! throw [] [db expr] (throw (Exception. expr)))

  (defn! when
    []
    [db pred then]
    (when (datomic.api/invoke db :fn/eval db pred)
          (datomic.api/invoke db :fn/eval db then)))

  (defn! if
    []
    [db pred then else]
    (if (datomic.api/invoke db :fn/eval db pred)
        (datomic.api/invoke db :fn/eval db then)
        (datomic.api/invoke db :fn/eval db else)))

  (defn! apply-or
    [[datomic.api :as api]]
    ^{:macro? true
      :doc "Variadic |or|."}
    [db args]
    (loop [args-n args]
      (if-let [arg (->> args-n first (api/invoke db :fn/eval db))]
        arg
        (recur (rest args-n)))))

  (defn! or
    [[datomic.api :as api]]
    ^{:macro? true
      :doc "2-arity |or|."}
    [db alt0 alt1]
    (or (api/invoke db :fn/eval db alt0)
        (api/invoke db :fn/eval db alt1)))

  (defn! lookup
    [[datomic.api :as api]]
    [db a v]
    (ffirst
      (api/q [:find '?e :where ['?e a v]] db)))

  (defn! lookup-nnil
    [[datomic.api :as api]]
    [db a v]
    (api/invoke db :fn/eval db
      `(:fn/validate :fn/nnil?
         (:fn/lookup ~a ~v))))

  (defn! eval
    [[clojure.walk :as walk]]
    [db expr]
    (let [db-eval #(datomic.api/invoke db :fn/eval db %)]
      #_(println "EXPR IS" expr
        "CALLABLE?" (and (instance? java.util.List expr) ; is callable?
                 (-> expr first keyword?)
                 (-> expr first namespace (= "fn"))))
      (cond (and (instance? java.util.List expr) ; is callable?
                 (-> expr first keyword?)
                 (-> expr first namespace (= "fn")))
            (let [[f & args] expr
                  macros #{:fn/if :fn/when :fn/validate :fn/or :fn/apply-or :fn/lookup-nnil}]
              (if (contains? macros f)
                  (apply datomic.api/invoke db f db args)
                  (apply datomic.api/invoke db f db
                    (mapv db-eval args)))) ; args are evaluated in order unless is a macro
            
            (instance? clojure.lang.IMapEntry expr) (vec (map db-eval expr))
            (seq? expr) (doall (map db-eval expr))
            (instance? clojure.lang.IRecord expr)
              (reduce (fn [r x] (conj r (db-eval x))) expr expr)
            (coll? expr) (into (empty expr) (map db-eval expr))

            :else ; atomic — limiting case
            expr)))

  (defn! fail-when-exists
    [[datomic.api :as api]]
    [db query err-msg else]
    (let [result (api/q query db)]
      (if (not (empty? result))
          (throw (ex-info err-msg {:query  query
                                   :result result
                                   :type   :already-exists
                                   :msg    err-msg}))
          (api/invoke db :fn/eval db else))))

  (defn! transform
    [[clojure.walk :as walk]]
    ^{:doc "Expands the database function keywords to the database function calls *at the actual transaction time*,
            instead of e.g. just before the transaction is sent off.
            This enforces atomicity when the transaction needs to refer back to another part of the database.

            Essentially the example says, update person1’s song:preference attribute to whatever song title is
            one that is popular *during the transaction*.
            This is opposed to getting a bunch of inconsistent snapshots of the db and performing queries on them,
            aggregating the results into a transaction, *before* it’s sent off to the db."
      :example
        '(transact!
           [[:fn/transform
              {:db/id (tempid :db.part/test)
               :agent:person:name:last:maternal
                 '(:fn/ffirst
                    (:fn/q [:find ?surname
                            :where [_ :agent:person:name:last:surname ?surname]]))}]])}
    [db m]
    (->> m
         (walk/prewalk ; Get rid of weird ArrayLists in order to walk correctly
           (fn [x]
              (cond (and (not (coll? x))
                         (instance? java.util.List x))
                    (into [] x)
                    (and (not (coll? x))
                         (instance? java.util.Set x))
                    (into #{} x)
                    :else x)))
         (datomic.api/invoke db :fn/eval db)
         vector))

  (defn! retract-except
    [[datomic.api]]
    ^{:doc "This will work for cardinality-many attributes as well as
            cardinality-one.
          
            To make sure Alex loves pizza and ice cream but nothing else
            that might or might not have asserted earlier: 

            [[:assertWithRetracts :alex :loves [:pizza :ice-cream]]]

            For a cardinality/one attribute it's called the same way,
            but of course with a maximum of one value.

            It does not work if you pass in an ident as the target
            of a ref that is not actually changing (which is what I
            tried to do for a ref to an enum entity). You have to
            convert the ident to an entity ID up front and pass the
            entity ID to |retract-except|, otherwise you end up
            with something like this in the case where there is no
            change to an enum entity ref."
      :usage '(db/transact!
                [[:fn/retract-except 466192930238332 :twitter/user.followers []]])
      :from "https://groups.google.com/forum/#!msg/datomic/MgsbeFzBilQ/s8Ze4JPKG6YJ"} 
    [db e a vs]
    (vals (into (->> (datomic.api/q [:find '?v :where [e a '?v]] db)
                     (map (comp #(vector % [:db/retract e a %]) first))
                     (join {}))
                (->> vs
                     (map #(vector % [:db/add e a %]))
                     (join {})))))

  (defn! ->fn
    [[datomic.api :as api]]
    [db ident]
    (->> (api/q '[:find ?e
                  :in   $ ?ident
                  :where [?e :db/ident ?ident]]
                db ident)
         ffirst
         (api/entity db)
         :db/fn :fnref force))

  true))