(ns ^{:cljs-self-referring? true}
  quantum.db.datomic.entities
  (:refer-clojure :exclude
    [boolean? double? numerator denominator ratio?])
  (:require
    [quantum.core.error       :as err
      :refer [->ex]]
    [quantum.core.fn          :as fn
      :refer [fn1 fn$ <- fn-> fn->>]]
    [quantum.core.log         :as log]
    [quantum.core.logic       :as logic
      :refer [fn-or fn-and
              nnil? nempty?]]
    [quantum.core.macros.core
      :refer [if-cljs]]
    [quantum.db.datomic       :as db    ]
    [quantum.db.datomic.core  :as dbc
      :refer [dbfn-call?]               ]
    [quantum.core.string      :as str   ]
    [quantum.core.collections :as coll
      :refer [join]]
    [quantum.core.type        :as type
      :refer [#?@(:clj [double? bigint?]) boolean?]]
    [quantum.core.numeric.types
      :refer [numerator denominator ratio?]]
    [quantum.core.numeric.convert :as nconv
      :refer [->ratio]              ]
    [quantum.core.validate        :as v
      :refer [validate]])
  (:require-macros
    [quantum.db.datomic.entities
      :refer [defentity defattribute]]))

(def schemas    (atom  {}))
(def attributes (atom #{}))

(def attribute? #(and (:v %) (-> % count (= 1))))

(def identifier? (fn-or keyword? integer?
                        #?(:clj #(instance? datomic.db.DbId %))))

(def lookup? (fn-and vector? (fn->  first keyword?)))

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
   :float   #?(:clj float?        :cljs number? ) ; TODO CLJS |float?|
   :double  #?(:clj #(double? %1) :cljs number? )
   :bigdec  #?(:clj (partial instance? BigDecimal) #_bigdec? :cljs number? ) ; TODO CLJS |bigdec?|
   :ref     (fn-or map? dbfn-call? identifier? lookup?) ; Can be any entity/record
   :instant #(instance? #?(:clj java.util.Date :cljs js/Date) %)
   #?(:clj :uri) #?(:clj (partial instance? java.net.URI))
   ; TODO add these in
   ;:uuid    #?(:clj uuid?)
   ;:bytes   #?(:clj bytes? :cljs bytes?)
   })

(defn attr->constructor-sym [k]
  (symbol (str "->" (name k))))

(defn attr->class-sym [k]
  (-> k name (str "*") symbol))

(defn ->ref-tos-constructors
  "Extract the ref-tos constructors from an options map"
  [opts]
  (when-let [ref-to-0 (:ref-to opts)]
    (if (coll? ref-to-0)
        (map attr->constructor-sym ref-to-0)
        [(attr->constructor-sym ref-to-0)])))

(defn swapper
  [m constructor-f k arg]
  (when (nnil? arg)
    (vswap! m assoc k (constructor-f arg))))

#?(:clj
(defmacro defattribute
  "Defines a function which creates a Datomic attribute-value pair.

   Also adds the schema into the in-memory schema store
   when it defines this fn."
  {:example '(defattribute :agent
               [:one :ref {:ref-to #{:agent:person :agent:organization}}])}
  [attr-k schema]
  (let [[cardinality type opts] schema
        attribute-sym (-> attr-k name symbol)
        v-0         (gensym 'v-0)
        v-f         (gensym 'v-f)
        ;schema-eval (gensym 'schema-eval)
        opts-f      (dissoc opts #_:validator #_:transformer :ref-to)
        schema-f    [cardinality type opts-f]
        ref-tos     (->ref-tos-constructors opts)
        class-name  (attr->class-sym attr-k)
        constructor-name (attr->constructor-sym attr-k)
        constructor-code
         `(defn ~constructor-name [~v-0]
            (log/pr ::debug "Constructing" '~class-name "with" (type ~v-0) ~v-0 "...")
            (cond
                (or (instance? ~class-name ~v-0)
                    (and ~(= type :ref) (identifier? ~v-0))
                    (lookup?    ~v-0)
                    (dbfn-call? ~v-0)
                    (nil?       ~v-0))
                ~v-0
                :else
                (let [~v-f (atom ~v-0)]
                  ~(if (= cardinality :many)
                       `(validate (deref ~v-f) (fn$ every? (get validators ~type)))
                       `(validate (deref ~v-f) (get validators ~type)))
                  (when-let [transformer# (-> @schemas ~attr-k (get 2) :transformer)]
                    (swap! ~v-f transformer#))
                  (when-let [validator#   (-> @schemas ~attr-k (get 2) :validator  )] ; technically, post-transformer-validator
                    (validate (deref ~v-f) validator#))

                  ~(when ref-tos
                     (let [valid-instance?
                            (apply list 'or
                              `(identifier? ~(list 'deref v-f))
                              (map #(list 'instance? % (list 'deref v-f)) ref-tos))
                           err-sym (gensym 'e)]
                       `(try (assert ~valid-instance?) ; TODO make a better assert
                          (catch Throwable ~err-sym
                            ~(if (-> ref-tos count (> 1))
                                 `(throw ~err-sym)
                                 (let [constructor (-> opts ->ref-tos-constructors first)]
                                    (if (= cardinality :many)
                                      `(swap! ~v-f
                                         (fn->> (map ~constructor)
                                                (into #{})))
                                      `(swap! ~v-f ~constructor))))))))
                  (new ~class-name (deref ~v-f)))))]
    `(do (swap! schemas assoc ~attr-k ~schema-f)
         (swap! attributes conj ~attr-k)
         (declare ~constructor-name)
         ~(list 'defrecord class-name ['v])
         ~constructor-code))))

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
            1 [nil (first args)]
            2 [(first  args)
               (second args)]
            (throw (->ex :illegal-argument "Invalid number of args" (count args))))
        fields
          (->> entity keys (mapv (fn-> name symbol)))
        class-name
          (attr->class-sym attr-k)
        class-name-fields
          (symbol (str (name attr-k) "___" "fields"))
        constructor-name (attr->constructor-sym attr-k)
        class-to-map
          (symbol (str "map->" (name class-name)))
        args-sym (gensym 'args)
        args (if (nempty? fields) [args-sym] [])
        destructured {:keys fields :as args-sym}
        ref-tos (->ref-tos-constructors opts)
        ;_ (println "/*" "INSIDE DEFENTITY AFTER REF TOS" "*/")
        m-f  (gensym 'm-f )
        m-f* (gensym 'm-f*)
        constructor-code
         `(defn ~constructor-name ~args
            ; If already an instance of the class, return it
            (if (or (instance? ~class-name ~args-sym)
                    (identifier? ~args-sym)
                    (lookup?     ~args-sym)
                    (dbfn-call?  ~args-sym))
                ~args-sym
                (let [~destructured ~args-sym]
                  (doseq [k# (keys ~args-sym)]
                    (assert (contains? ~class-name-fields k#) #{k#}))
                  (when-let [type# (:type ~args-sym)]
                    (assert (= type# ~attr-k) #{type#}))
                  (let [~m-f (volatile! {})]
                    ; For each k, ensure the right constructor is called
                    ~@(for [k (->> entity keys)]
                       `(swapper ~m-f ~(attr->constructor-sym k) ~k ~(-> k name symbol)))
                    (vswap! ~m-f assoc :type ~attr-k) ; was having problems with transient
                    (when-let [id# (:db/id ~args-sym)]
                      (vswap! ~m-f assoc :db/id id#))
                    (when-let [ident# (:db/ident ~args-sym)]
                      (vswap! ~m-f assoc :db/ident ident#))
                    (~class-to-map (deref ~m-f))))))
        code
          `(do (declare ~constructor-name)
               (declare ; Purely for the sake of macroexpansion warnings
                 ~@(->> (for [[k v] entity]
                          (when (nnil? v) (attr->constructor-sym k)))
                        (remove nil?)))
               (swap! schemas assoc ~attr-k [:one :ref ~opts])
               (defrecord ~class-name ~(conj fields 'type))
               (def ~class-name-fields (->> ~entity keys (join #{:db/id :db/ident :type})))
               ~@(->> (for [[k v] entity]
                        (when (nnil? v)
                         `(defattribute ~k ~(update v 2 #(assoc % :component? true)))))
                      (remove nil?)) ; assume all inline-declared attributes are components
               ~constructor-code)]
    code)))

#?(:clj
(defmacro declare-entity
  [entity-k]
  (let [class-name (attr->class-sym entity-k)]
    (if-cljs &env
      `(declare ~class-name ~(attr->constructor-sym entity-k))
      `(do (defrecord ~class-name [])
           (declare ~(attr->constructor-sym entity-k)))))))

(declare ->ratio:long)

(defentity :ratio:long
  {:doc "A ratio specifically using longs instead of bigints."}
  {:ratio:long:numerator   [:one :long]
   :ratio:long:denominator [:one :long]})

(defattribute :unit:v
  [:one :ref {:ref-to :ratio:long :component? true}])

(defn num->ratio:long [x]
  (let [r (->ratio x)
        n      (quantum.core.numeric.types/numerator   r)
        n-long (long n) ; TODO for JS this will be a problem if overflows
        _ (assert (= n-long n) #{n n-long})
        d      (quantum.core.numeric.types/denominator r)
        d-long (long d) ; TODO for JS this will be a problem if overflows
        _ (assert (= d-long d) #{d d-long})]
    (->ratio:long
      {:ratio:long:numerator   n-long
       :ratio:long:denominator d-long})))

(defn validate-unit [v c constructor validators]
  (cond
    (instance? c v)
    v
    (map? v)
    (reduce-kv
      (fn [ret k v]
        (validate k validators
                  v (fn1 (get validators k)))
        (assoc ret k
          (if (= k :unit:v)
              (->ratio:long v)
              v)))
      (constructor nil nil nil)
      v)
    :else
    (constructor :data:audio:bit-rate :unit:kb-per-s
      (num->ratio:long v))))

#?(:clj
(defmacro defunit
  [name- unit]
  (let [class-name       (attr->class-sym name-)
        constructor-name (attr->constructor-sym name-)
        raw-constructor-name (symbol (str (name constructor-name) "*"))
        validators-name  (symbol (str (name class-name) ":__validators"))]
    `(do (defrecord ~class-name [~'type ~'unit ~'unit:v])
         (def ~validators-name
           {:type   (fn1 = ~name-)
            :unit   (fn1 = ~unit)
            :unit:v (constantly true)
            :db/id  identifier?})
         (defn ~constructor-name [v#]
           (validate-unit v# ~class-name ~raw-constructor-name ~validators-name))
         (do (swap! schemas assoc ~name- [:one :ref {:component? true}]) nil)))))

#?(:clj
(defn transact-schemas!
  "Clojure only because schemas can only be added upon creation of the DataScript
   connection; they cannot be transacted."
  []
  (-> @schemas db/transact!)))
