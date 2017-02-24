(ns
  ^{:doc "Type-checking predicates, 'transientization' checks, class aliases, etc."
    :attribution "Alex Gunnarson"}
  quantum.core.type
  (:refer-clojure :exclude
    [vector? map? set? associative? seq? seqable? string? fn? map-entry? boolean?
     double? decimal?
     nil? char? number? integer? float?
     sequential? indexed? list? coll?
     symbol? keyword?
     array?
     record?
     identity class])
  (:require
    [clojure.core                 :as core]
    [quantum.core.classes         :as classes]
    [quantum.core.core            :as qcore]
    [quantum.core.fn              :as fn
      :refer [fn1 fn$ mfn fn->]]
    [quantum.core.logic           :as logic
      :refer [fn-and whenf1]]
    [quantum.core.data.vector     :as vec    ]
    [quantum.core.macros          :as macros
      :refer [defnt #?(:clj defnt')]]
    [quantum.core.type.core       :as tcore  ]
    [quantum.core.vars            :as var
      :refer [defalias]])
  (:require-macros
    [quantum.core.type
      :refer [should-transientize? boolean?]]))

; TODO: Should include typecasting? (/cast/)

(def class #?(:clj core/class :cljs type))

#?(:clj (def instance+? instance?)
   :cljs
     (defn instance+?
       {:todo #{"try-catch in something this basic is a performance issue"}}
       [c x] ; inline this?
       (try
         (instance? c x)
         (catch js/TypeError _
           (try (satisfies? c x))))))

(def types           tcore/types          )

#?(:clj (defalias static-cast tcore/static-cast))

         ; TODO for JS, primitives (function, array, number, string) aren't covered by these

#?(:clj  (defnt' prim-long?    ([^long           x] true)
                               ([#{boolean? byte? char? int? long? float? double? Object} x] false))) ; TODO #{(- prim? long) Object}

         (defnt integer?
           "Whether x is integer-like (primitive/boxed integer, BigInteger, etc.)."
           ([^integer? obj] true) ([obj] false))
#?(:clj  (defnt    double?  ([^double?  obj] true) ([obj] false))
   :cljs (do (defalias double? core/number?) ; TODO fix
             (defalias double?-protocol double?)))
#?(:clj  (defnt    float?   ([^float?  obj] true) ([obj] false))
   :cljs (do (defalias float? core/number?) ; TODO fix
             (defalias float?-protocol float?)))
         (defnt number?        ([^number?        x] true) ([x] false))

         (defnt boolean?       ([^boolean?       x] true) ([x] false))

         (defnt bigint?        ([^bigint?        x] true) ([x] false))

         (defnt byte-array?    ([^byte-array?    x] true) ([x] false))
         (defnt bytes?         ([^bytes?         x] true) ([x] false))
         (defnt double-array?  ([^double-array?  x] true) ([x] false))
         (defnt doubles?       ([^doubles?       x] true) ([x] false))
 #?(:clj (defnt array-2d?      ([^array-2d?      x] true) ([x] false)))
         (defnt array?
           ([^array? x] true)
           ([x] #?(:clj  (-> x class .isArray) ; Have to use reflection here because we can't check *ALL* possible array types in a `defnt`
                   :cljs (-> x core/array?))))

         (defnt svector?       ([^svector?       x] true) ([x] false))
         (defnt svec?          ([^svec?          x] true) ([x] false))
         (defnt +vector?       ([^+vector?       x] true) ([x] false))
         (defnt +vec?          ([^+vec?          x] true) ([x] false))
         (defnt vector?        ([^vector?        x] true) ([x] false))
         (defnt vec?           ([^vec?           x] true) ([x] false))

         (defnt +array-map?    ([^+array-map?    x] true) ([x] false))
         (defnt +hash-map?     ([^+hash-map?     x] true) ([x] false))
         (defnt +unsorted-map? ([^+unsorted-map? x] true) ([x] false))
         (defnt unsorted-map?  ([^unsorted-map?  x] true) ([x] false))
         (defnt +sorted-map?   ([^+sorted-map?   x] true) ([x] false))
         (defnt sorted-map?    ([^sorted-map?    x] true) ([x] false))
         (defnt +map?          ([^+map?          x] true) ([x] false))
         (defnt map?           ([^map?           x] true) ([x] false))

         (defnt +unsorted-set? ([^+unsorted-set? x] true) ([x] false))
         (defnt unsorted-set?  ([^unsorted-set?  x] true) ([x] false))
         (defnt +sorted-set?   ([^+sorted-set?   x] true) ([x] false))
         (defnt sorted-set?    ([^sorted-set?    x] true) ([x] false))
         (defnt +set?          ([^+set?          x] true) ([x] false))
         (defnt set?           ([^set?           x] true) ([x] false))

         (defnt array-list?    ([^array-list?    x] true) ([x] false))
         (defnt +queue?        ([^+queue?        x] true) ([x] false))
         (defnt queue?         ([^queue?         x] true) ([x] false))
         (defnt lseq?          ([^lseq?          x] true) ([x] false))
         (defnt sequential?    ([^sequential?    x] true) ([x] false))
         (defalias seqable? qcore/seqable?)

#?(:clj  (defnt file?          ([^file?          x] true) ([x] false)))
         (defnt pattern?       ([^pattern?       x] true) ([x] false))
         (defnt regex?         ([^regex?         x] true) ([x] false))
         (defnt editable?      ([^editable?      x] true) ([#_(:cljs :else) x] false))
         (defnt transient?     ([^transient?     x] true) ([x] false))
         (defnt indexed?       ([^indexed?       x] true) ([x] false))

; #?(:cljs (defnt typed-array? ...))

         (def map-entry? #?(:clj  core/map-entry?
                            :cljs (fn-and vector? (fn-> count (= 2)))))
         (defalias atom? qcore/atom?)

         (defn derefable? [obj]
           #?(:clj  (instance?  clojure.lang.IDeref obj)
              :cljs (satisfies? cljs.core/IDeref    obj)))

#?(:clj  (def throwable? (partial instance+? java.lang.Throwable )))
         (defnt error?  ([#{#?(:clj  Throwable
                               :cljs js/Error)} obj] true) ([obj] false))
#?(:clj
(defnt interface?
  [^java.lang.Class class-]
  (.isInterface class-)))

#?(:clj
(defnt abstract?
  [^java.lang.Class class-]
  (java.lang.reflect.Modifier/isAbstract (.getModifiers class-))))


#?(:clj (def multimethod? (fn$ instance? clojure.lang.MultiFn)))
#?(:clj (def unbound?     (fn$ instance? clojure.lang.Var$Unbound)))
#?(:clj (def thread?      (fn$ instance? Thread)))

#?(:clj
(defn protocol?
  "Returns true if an argument is a protocol"
  [x] (and (map? x) (-> x :on-interface class?))))

#?(:clj (defn namespace? [x] (instance? clojure.lang.Namespace x)))

#?(:clj
(defn promise?
  {:source 'zcaudate/hara.class.checks}
  [^Object obj]
  (let [^String s (.getName ^Class (type obj))]
    (.startsWith s "clojure.core$promise$"))))

; ===== JAVA =====

#?(:clj
(defn enum?
  {:source "zcaudate/hara.object.enum"}
  [type]
  (-> (classes/ancestor-list type)
      (set)
      (get java.lang.Enum))))

; ; ======= TRANSIENTS =======

; ; TODO this is just intuition. Better to |bench| it
; ; TODO move these vars
(def transient-threshold 3)

; macro because it will probably be heavily used
#?(:clj
(defmacro should-transientize? [coll]
  `(and (editable? ~coll)
        (counted?  ~coll)
        (-> ~coll count (> transient-threshold)))))

; (def primitive-records
;   [{:raw "Z" :symbol 'boolean :string "boolean" :class Boolean/TYPE   :container Boolean}
;    {:raw "B" :symbol 'byte    :string "byte"    :class Byte/TYPE      :container Byte}
;    {:raw "C" :symbol 'char    :string "char"    :class Character/TYPE :container Character}
;    {:raw "I" :symbol 'int     :string "int"     :class Integer/TYPE   :container Integer}
;    {:raw "J" :symbol 'long    :string "long"    :class Long/TYPE      :container Long}
;    {:raw "F" :symbol 'float   :string "float"   :class Float/TYPE     :container Float}
;    {:raw "D" :symbol 'double  :string "double"  :class Double/TYPE    :container Double}
;    {:raw "V" :symbol 'void    :string "void"    :class Void/TYPE      :container Void}])

#?(:clj (def ^:runtime-eval construct (mfn new)))

(defnt identity
  "Type identity function."
  {:todo ["Fix so only immmutable data stuctures have immutable identity fns."]}
  ([^+vector? x] vector  )
  ([^+map?    x] hash-map)
  ([^+set?    x] hash-set))

(def +vector?-fn (fn1 +vector?))
(def +set?-fn    (fn1 +set?   ))
(def +map?-fn    (fn1 +map?   ))

(defnt ->pred
  "Gets the type predicate associated with the value passed."
  ([^+vector? x] +vector?-fn)
  ([^+set?    x] +set?-fn   )
  ([^+map?    x] +map?-fn   ))

(defnt ->literal
  "Gets the literal value associated with the value passed."
  ([^+vector? x] [] )
  ([^+set?    x] #{})
  ([^+map?    x] {} ))

(defnt ->base
  "Gets the base value associated with the value passed."
  ([^+vector?       x] (vector  ))
  ([^+unsorted-set? x] (hash-set))
  ([^+hash-map?     x] #?(:clj  clojure.lang.PersistentHashMap/EMPTY
                          :cljs cljs.core.PersistentHashMap.EMPTY))
  ([                x] (empty x)))

(def transient!*  (whenf1 editable?  transient))
(def persistent!* (whenf1 transient? persistent!))

(def transient-persistent-fns
  {true  [transient      conj! persistent!     ]
   false [core/identity  conj  core/identity   ]})

(defn transient-fns [coll]
  (get transient-persistent-fns (editable? coll)))

(defn recommended-transient-fns [coll]
  (get transient-persistent-fns (should-transientize? coll)))

(defnt ->joinable
  ([#{+vec? +hash-map? +unsorted-set?} x] x)
  ([#{+array-map?}                     x] (into (->base x) x))
  ([                                   x] x))
