(ns quantum.db.datomic.entities
  (:require-quantum [:core logic fn log str type err])
  (:require [quantum.db.datomic         :as db   ]))

(def schemas (atom {}))
(def attributes (atom #{}))

(def identifier? (fn-or keyword? integer?
                        #?(:clj #(instance? datomic.db.DbId %))))

(def dbfn-call? (fn-and seq?
                        (fn-> first keyword?)
                        (fn-> first namespace (= "fn"))))

(def ^{:doc "See also |quantum.db.datomic.core/allowed-types|."}
  validators
  {:keyword keyword?
   :string  string?
   :boolean #(boolean? %1)
   :long    #?(:clj (fn-or #(instance? Long    %)
                           #(instance? Integer %)
                           #(instance? Short   %)) #_long?  
               :cljs integer?) ; TODO CLJS |long?| ; TODO autocast from e.g. bigint if safe to do so
   :bigint  #?(:clj #(bigint? %1) :cljs integer?) ; TODO CLJS |bigint?|
   :float   #?(:clj float?  :cljs number? ) ; TODO CLJS |float?|
   :double  #?(:clj double? :cljs number? ) ; TODO CLJS |number?|
   :bigdec  #?(:clj (partial instance? BigDecimal) #_bigdec? :cljs number? ) ; TODO CLJS |bigdec?|
   :ref     (fn-or map? identifier?) ; Can be any entity/record
   ; TODO add these in
   ;:instant #?(:clj instant?)
   ;:uuid    #?(:clj uuid?)
   ;:uri     #?(:clj)
   ;:bytes   #?(:clj bytes? :cljs bytes?)
   })

(defn keyword->class-name [k]
  (->> k name 
       (<- str/split #"\:")
       (map str/capitalize)
       (str/join "_")
       (<- str/replace "+" "AND")
       symbol))

(defn attr->constructor-sym [k]
  (symbol (str "->" (name k))))

(defn ->ref-tos
  "Extract the ref-tos from an options map"
  [opts]
  (when-let [ref-to-0 (:ref-to opts)]
    (if (coll? ref-to-0)
        (map keyword->class-name ref-to-0)
        [(keyword->class-name ref-to-0)])))

(defn ->ref-tos-constructors
  "Extract the ref-tos constructors from an options map"
  [opts]
  (when-let [ref-to-0 (:ref-to opts)]
    (if (coll? ref-to-0)
        (map attr->constructor-sym ref-to-0)
        [(attr->constructor-sym ref-to-0)])))

#?(:clj
(defmacro defattribute
  "Defines a function which creates a Datomic attribute-value pair.
   
   Also adds the schema into the in-memory schema store
   when it defines this fn."
  {:example '(defattribute :agent
               [:ref :one {:ref-to #{:agent:person :agent:organization}}])}
  [attr-k schema]
  (let [[type cardinality opts] (eval schema)
        attribute-sym (-> attr-k name symbol)
        v-0         (gensym 'v-0)
        v-f         (gensym 'v-f)
        schema-eval (gensym 'schema-eval)
        opts-f      (dissoc opts :validator :transformer :ref-to)
        schema-f    [type cardinality opts-f]
        ref-tos     (->ref-tos opts)
        class-name  (-> attr-k keyword->class-name)
        constructor-name (symbol (str "->" (name attr-k)))]
    `(do (swap! schemas assoc ~attr-k ~schema-f)
         (swap! attributes conj ~attr-k)
         ~(list 'defrecord class-name ['v])
         (defn ~constructor-name [~v-0]
           (log/pr :debug "Constructing" '~class-name "with" (core/type ~v-0) ~v-0 "...")
           (cond
               (or (instance? ~class-name ~v-0)
                   (and ~(= cardinality :many) (identifier? ~v-0))
                   (dbfn-call? ~v-0))
               ~v-0
               (nil? ~v-0)
               nil
               :else
               (let [~v-f (atom ~v-0)]
                 ~(if (= cardinality :many)
                      `(quantum.core.error/assert (every? (get validators ~type) (deref ~v-f)) #{~v-f})
                      `(quantum.core.error/assert        ((get validators ~type) (deref ~v-f)) #{~v-f})) 
                 (when-let [transformer# (:transformer ~opts)]
                   (swap! ~v-f transformer#))
                 (when-let [validator#   (:validator   ~opts)] ; technically, post-transformer-validator 
                   (quantum.core.error/assert (validator# (deref ~v-f)) #{~v-f}))
                 
                 ~(when ref-tos
                    (let [valid-instance?
                           (apply list 'or
                             `(identifier? ~(list 'deref v-f))
                             (map #(list 'instance? % (list 'deref v-f)) ref-tos))
                          err-sym (gensym 'e)]
                      `(try (quantum.core.error/assert ~valid-instance? #{~v-f})
                         (catch ~(err/generic-error &env) ~err-sym
                           ~(if (-> ref-tos count (> 1))
                                `(throw ~err-sym)
                                (let [constructor (-> opts ->ref-tos-constructors first)]
                                   (if (= cardinality :many)
                                     `(swap! ~v-f
                                        (fn->> (map ~constructor)
                                               (into #{})))
                                     `(swap! ~v-f ~constructor))))))))
                 (new ~class-name (deref ~v-f)))))))))

#?(:clj
(defmacro defentity
  {:example '(defentity :media.agent+plays
               {:component? true}
               {:agent nil
                :media.plays
                  [:ref :many {:ref-to :time.instant :doc "Dates played"}]})}
  [attr-k & args]
  (let [args `~args
        [opts entity]
          (condp = (count args)
            1 [nil (eval (first args))] 
            2 [(eval (first args))
               (eval (second args))]
            (throw (->ex :illegal-argument "Invalid number of args" (count args))))
        fields
          (->> entity keys (mapv (fn-> name symbol)))
        class-name
          (-> attr-k keyword->class-name)
        class-name-fields
          (symbol (str (name attr-k) "___" "fields"))
        constructor-name (attr->constructor-sym attr-k)
        class-to-map
          (symbol (str "map->" (name class-name)))
        args-sym (gensym 'args)
        args (if (nempty? fields) [args-sym] [])
        destructured {:keys fields :as args-sym}
        ref-tos (->ref-tos opts)
        ;_ (println "/*" "INSIDE DEFENTITY AFTER REF TOS" "*/")
        m-f (gensym 'm-f)
        code
          `(do (swap! schemas assoc ~attr-k [:ref :one ~opts])
               (defrecord ~class-name ~(conj fields 'type))
               (def ~class-name-fields (->> ~entity keys (into #{})))
               (declare ~constructor-name)
               ~@(for [[k v] entity]
                   (when (nnil? v) `(defattribute ~k ~(update v 2 (f*n assoc :component? true))))) ; assume all inline-declared attributes are components
               (defn ~constructor-name ~args
                 ; If alredy an instance of the class, return it
                 (if (or (instance? ~class-name ~args-sym)
                         (identifier? ~args-sym)
                         (dbfn-call?  ~args-sym))
                     ~args-sym
                     (let [~destructured ~args-sym]
                       (doseq [k# (keys ~args-sym)]
                         (assert (contains? ~class-name-fields k#)  #{k#}))
                       (let [~m-f (volatile! {})]
                         ; For each k, ensure the right constructor is called
                         ~@(core/for [k (keys entity)]
                            `(when (nnil? ~(-> k name symbol))
                               (let [v-f# (~(attr->constructor-sym k) ~(-> k name symbol))]
                                 (vswap! ~m-f assoc ~k v-f#))))
                         (vswap! ~m-f assoc :type ~attr-k) ; was having problems with transient
                         (~class-to-map (deref ~m-f)))))))
        ;_ (println "/*" "DEFENTITY CODE" code "*/")
        ]
    code)))

#?(:clj
(defmacro declare-entity
  {:todo ["Doesn't actually work yet"]}
  [entity-k]
  (let [class-name
          (-> entity-k keyword->class-name)]
    `(do (defrecord ~class-name [])
         (declare ~(attr->constructor-sym entity-k)))
    )))

(defn transact-schemas! []
  (-> @schemas
      quantum.db.datomic.core/block->schemas
      db/transact!))
