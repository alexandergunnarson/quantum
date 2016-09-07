(ns
  ^{:doc "Type-checking predicates, 'transientization' checks, class aliases, etc."
    :attribution "Alex Gunnarson"
    :cljs-self-referring? true}
  quantum.core.type
  (:refer-clojure :exclude
    [vector? map? set? associative? seq? string? keyword? fn? map-entry? boolean?
     indexed? nil? list? coll? char? symbol? record? number? integer? float?
     double? decimal? array?
     identity class])
           (:require [#?(:clj  clojure.core
                         :cljs cljs.core   )       :as core   ]
                     [quantum.core.classes         :as classes]
                     [quantum.core.fn              :as fn
                       :refer [#?@(:clj [f*n mfn fn->])]      ]
                     [quantum.core.logic           :as logic
                       :refer [#?@(:clj [fn-and whenf*n])]    ]
                     [quantum.core.data.vector     :as vec    ]
                     [quantum.core.macros          :as macros
                       :refer [#?@(:clj [defnt defnt'])]      ]
                     [quantum.core.type.core       :as tcore  ]
                     [quantum.core.type.predicates :as tpred  ]
                     [quantum.core.vars            :as var
                       :refer [#?(:clj defalias)]             ])
  #?(:cljs (:require-macros
                     [quantum.core.type
                       :refer [should-transientize?]]
                     [quantum.core.fn              :as fn
                       :refer [f*n mfn fn->]                  ]
                     [quantum.core.logic           :as logic
                       :refer [fn-and whenf*n]                ]
                     [quantum.core.macros          :as macros
                       :refer [defnt defnt']                  ]
                     [quantum.core.vars            :as var
                       :refer [defalias]                      ])))

; TODO: Should include typecasting? (/cast/)

(def class #?(:clj clojure.core/class :cljs type))

#?(:clj (def instance+? instance?)
   :cljs
     (defn instance+?
       {:todo ["try-catch in something this basic is a performance issue"]}
       [class-0 obj] ; inline this?
       (try
         (instance? class-0 obj)
         (catch js/TypeError _
           (try (satisfies? class-0 obj))))))

(def name-from-class tcore/name-from-class)
(def arr-types       tcore/arr-types      )
(def types           tcore/types          )

; TODO takes way too long to compile. Fix this
#_#?(:clj
(eval
  `(macros/maptemplate
   ~(fn [[type-pred types]] ;(println "[type-pred types]" [type-pred types])
     (let [code
            (when (core/symbol? type-pred)
              (concat
                `(defnt ~type-pred)
                `((^boolean [#{~type-pred} obj] true))
                (if (quantum.core.analyze.clojure.predicates/symbol-eq? type-pred 'nil?)
                    '((^boolean [obj] (nil? obj)))
                    '((^boolean [:else obj] false)))))]
       code))
   ~(->> types (remove (fn-> key (= 'nil?)))))))
        ; TODO for JS, primitives (function, array, number, string) aren't covered by these
        (defnt byte-array? ([^byte-array? obj] true) ([obj] false))
#?(:clj (defnt bigint?     ([^bigint?     obj] true) ([obj] false)))
#?(:clj (defnt file?       ([^file?       obj] true) ([obj] false)))
        (defnt hash-map?   ([^hash-map?   obj] true) ([obj] false))
        (defnt sorted-map? ([^sorted-map? obj] true) ([obj] false))
        (defnt boolean?    ([^boolean?    obj] true) ([obj] false))
        (defnt listy?      ([^listy?      obj] true) ([obj] false))
        (defnt vector?     ([^vector?     obj] true) ([obj] false))
        (defnt set?        ([^set?        obj] true) ([obj] false))
        (defnt hash-set?   ([^hash-set?   obj] true) ([obj] false))
        (defnt map?        ([^map?        obj] true) ([obj] false))
        (defnt array-list? ([^array-list? obj] true) ([obj] false))
        (defnt queue?      ([^queue?      obj] true) ([obj] false))
        (defnt lseq?       ([^lseq?       obj] true) ([obj] false))
        (defnt pattern?    ([^pattern?    obj] true) ([obj] false))
        (defnt regex?      ([^regex?      obj] true) ([obj] false))
        (defnt editable?   ([^editable?   obj] true) ([#?(:cljs :else) obj] false))
        (defnt transient?  ([^transient?  obj] true) ([obj] false))

        (defnt array?
          ([^array? x] true)
          ([x] #?(:clj  (-> x class .isArray)
                  :cljs (-> x core/array?))))

#?(:clj (defnt' prim-long? ([^long n] true) ([:else n] false)))

;         #?(:cljs
; (defn bigint?
;   [x]
;   (instance? com.gfredericks.goog.math.Integer x)))


; #?(:cljs
; (defn ratio? [x]
;   (instance? quantum.core.numeric.types.Ratio x)))

(defalias map-entry? tpred/map-entry?)
(defalias atom?      tpred/atom?)

#?(:clj  (defnt double? ([^double? obj] true) ([obj] false))
   :cljs (def double?
           (fn-and
             core/number?
             #(not (zero? (rem % 1))))))
         (defalias vector+? vec/vector+?)
#?(:clj  (def indexed?   (partial instance+? clojure.lang.Indexed)))
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

#?(:clj (def multimethod? (partial instance? clojure.lang.MultiFn)))

#?(:clj
(defn protocol?
  {:source "zcaudate/hara.class.checks"
   :todo ["A more efficient version to found in ztellman's work. Not sure where."]}
  [obj]
  (and (instance? clojure.lang.PersistentArrayMap obj)
       (every? #(contains? obj %) [:on :on-interface :var])
       (-> obj :on str Class/forName class?)
       (-> obj :on-interface class?))))

#?(:clj
(defn promise?
  {:source "zcaudate/hara.class.checks"}
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


; (make-array Boolean/TYPE 1)


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

; ============ CLASS ALIASES ============

; Just to be able to synthesize class-name aliases...
; TODO these aren't quite right
         (def ANil       nil)
;#?(:clj (def Fn         clojure.lang.IFn))
         (def AKey       #?(:clj clojure.lang.Keyword              :cljs cljs.core.Keyword             ))
         (def ANum       #?(:clj java.lang.Number                  :cljs js/Number                     ))
         (def AExactNum  #?(:clj clojure.lang.Ratio                :cljs js/Number                     ))
         (def AInt       #?(:clj java.lang.Integer                 :cljs js/Number                     ))
         (def ADouble    #?(:clj java.lang.Double                  :cljs js/Number                     ))
         (def ADecimal   #?(:clj java.lang.Double                  :cljs js/Number                     ))
         (def ASet       #?(:clj clojure.lang.APersistentSet       :cljs cljs.core.PersistentHashSet   ))
         (def ABool      #?(:clj Boolean                           :cljs js/Boolean                    ))
         (def AArrList   #?(:clj java.util.ArrayList               :cljs cljs.core.ArrayList           ))
         (def ATreeMap   #?(:clj clojure.lang.PersistentTreeMap    :cljs cljs.core.PersistentTreeMap   ))
         (def ALSeq      #?(:clj clojure.lang.LazySeq              :cljs cljs.core.LazySeq             ))
         (def AVec       #?(:clj clojure.lang.APersistentVector    :cljs cljs.core.PersistentVector    )) ; Conflicts with clojure.core/->Vec
         (def AMEntry    #?(:clj clojure.lang.MapEntry             :cljs cljs.core.Vec                 ))
         (def ARegex     #?(:clj java.util.regex.Pattern           :cljs js/RegExp                     ))
         (def AEditable  #?(:clj clojure.lang.IEditableCollection  :cljs cljs.core.IEditableCollection ))
         (def ATransient #?(:clj clojure.lang.ITransientCollection :cljs cljs.core.ITransientCollection))
         (def AQueue     #?(:clj clojure.lang.PersistentQueue      :cljs cljs.core.PersistentQueue     ))
         (def AMap       #?(:clj java.util.Map                     :cljs cljs.core.IMap                ))
         (def AError     #?(:clj java.lang.Throwable               :cljs js/Error                      ))
#?(:clj  (def ASeq       clojure.lang.ISeq                                                     ))
; Otherwise "Use of undeclared Var"
;#?(:cljs (defrecord Exception                [e]))
;#?(:cljs (defrecord IllegalArgumentException [e]))

(defnt identity
  "Type identity function."
  {:todo ["Fix so only immmutable data stuctures have immutable identity fns."]}
  ([^vector? x] vector  )
  ([^map?    x] hash-map)
  ([^set?    x] hash-set))

(def vector?-fn (f*n vector?))
(def set?-fn    (f*n set?   ))
(def map?-fn    (f*n map?   ))

(defnt ->pred
  "Gets the type predicate associated with the value passed."
  ([^vector? x] vector?-fn)
  ([^set?    x] set?-fn   )
  ([^map?    x] map?-fn   ))

(defnt ->literal
  "Gets the literal value associated with the value passed."
  ([^vector? x] [] )
  ([^set?    x] #{})
  ([^map?    x] {} ))

(defnt ->base
  "Gets the base value associated with the value passed."
  ([^vector?   x] (vector  ))
  ([^hash-set? x] (hash-set))
  ([^hash-map? x] #?(:clj  clojure.lang.PersistentHashMap/EMPTY
                     :cljs cljs.core.PersistentHashMap.EMPTY))
  ([           x] (empty x)))

(def transient!*  (whenf*n editable?  transient))
(def persistent!* (whenf*n transient? persistent!))

(def transient-persistent-fns
  {true  [transient      conj! persistent!     ]
   false [core/identity  conj  core/identity   ]})

(defn transient-fns [coll]
  (get transient-persistent-fns (editable? coll)))

(defn recommended-transient-fns [coll]
  (get transient-persistent-fns (should-transientize? coll)))

(defnt ->joinable
  ([#{vector? hash-map? hash-set?} x] x)
  ([#{array-map?}             x] (into (->base x) x))
  ([                          x] x))