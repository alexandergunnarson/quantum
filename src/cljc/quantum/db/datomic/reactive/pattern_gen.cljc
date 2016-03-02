(ns ^{:doc "Query and pull pattern generation. Mostly taken from mpdairy/posh
            (distributed under EPL >= 1) and ported to CLJC."}
  quantum.db.datomic.reactive.pattern-gen
  (:require-quantum [:core])
  (:require [quantum.core.collections :as coll])
  )

; QUERY PATTERN GENERATION

(defn query->map [query]
  (coll/split-list-at keyword? query))

(defn clause-item [varmap item]
  (if (symbol? item)
      (or (varmap item) '_)
      item))

(defn clause->pattern [varmap clause]
  (vec (map (partial clause-item varmap) clause)))

(defn where->patterns [varmap where]
  (map (partial clause->pattern varmap) where))

(defn q-pattern-gen [query vars]
  (let [qm            (query->map query)
        simple-query? (not (coll/deep-list? (:where qm)))
        varmap        (if (and (:in qm) (> (count (:in qm)) 1))
                          (zipmap (rest (:in qm)) vars)
                          {})]
    (if simple-query?
        (where->patterns varmap (:where qm))
        [[]])))

; PULL PATTERN GENERATION

(defn reverse-lookup? [attr]
  (when (= (first (name attr)) '\_)
    (keyword (str (namespace attr) "/" (reduce str (rest (name attr)))))))

(declare pull-list)

(defn pull-datom [k ent-id]
  (if-let [rk (reverse-lookup? k)]
    ['_ rk ent-id]
    [ent-id k]))

(defn pull-map [m ent-id]
  (if (empty? m)
    []
    (let [[k v] (first m)]
      (concat [(pull-datom k ent-id)]
              (pull-list v '_)
              (pull-map (rest m) ent-id)))))

(defn pull-list [ls ent-id]
  (cond
   (empty? ls) []

   (= (first ls) '*)
   (cons [ent-id] (pull-list (rest ls) ent-id))

   (and (keyword? (first ls)) (not= (first ls) :db/id))
   (cons (pull-datom (first ls) ent-id) (pull-list (rest ls) ent-id))

   (map? (first ls))
   (concat (pull-map (first ls) ent-id)
           (pull-list (rest ls) ent-id))

   :else (pull-list (rest ls) ent-id)))

(defn pull-pattern-gen [ls ent-id]
  (let [p (pull-list ls ent-id)]
    (if (some #{'_} p)
        '[_]
        p)))