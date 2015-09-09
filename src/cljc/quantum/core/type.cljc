(ns
  ^{:doc "Type-checking predicates, 'transientization' checks, class aliases, etc."
    :attribution "Alex Gunnarson"}
  quantum.core.type
  (:refer-clojure :exclude
    [vector? map? set? associative? seq? string? keyword? fn?
     nil? list? coll? char? symbol? record? number? integer? float? decimal?])
  (:require-quantum [fn logic ns set map macros err log classes])
  (:require
    [quantum.core.type.core :as tcore]
    [quantum.core.analyze.clojure.predicates :as anap]
    [clojure.walk :refer [postwalk]]))

; TODO: Should include typecasting? (/cast/)

#?(:cljs (def class type))

#?(:clj (def instance+? instance?)
   :cljs
     (defn instance+? [class-0 obj] ; inline this?
       (try
         (instance? class-0 obj)
         (catch js/TypeError _
           (try (satisfies? class-0 obj))))))

(def name-from-class tcore/name-from-class)
(def arr-types tcore/arr-types)
(def types     tcore/types)

; TODO takes way too long to compile. Fix this
#?(:clj
(eval
  `(macros/maptemplate
   ~(fn [[type-pred types]] ;(println "[type-pred types]" [type-pred types])
     (let [code
            (when (core/symbol? type-pred)
              (concat
                `(defnt ~type-pred)
                (quote+ ((^boolean [#{~type-pred} obj] true)))
                (if (quantum.core.analyze.clojure.predicates/symbol-eq? type-pred 'nil?)
                    '((^boolean [obj] (nil? obj)))
                    '((^boolean [:else obj] false)))))]
       code))
   ~(->> types (remove (fn-> key (= 'nil?)))))))

#?(:cljs
(do (defnt listy?      ([^listy?      obj] true) ([obj] false))
    (defnt array-list? ([^array-list? obj] true) ([obj] false))
    (defnt queue?      ([^queue?      obj] true) ([obj] false))
    (defnt lseq?       ([^lseq?       obj] true) ([obj] false))
    (defnt pattern?    ([^pattern?    obj] true) ([obj] false))
    (defnt regex?      ([^regex?      obj] true) ([obj] false))
    (defnt sorted-map? ([^sorted-map? obj] true) ([obj] false))
    (defnt editable?   ([^editable?   obj] true) ([obj] false))
    (defnt transient?  ([^transient?  obj] true) ([obj] false))
    (defnt boolean?    ([^boolean?    obj] true) ([obj] false))))
   
(def map-entry?  #?(:clj  (partial instance+? clojure.lang.MapEntry)
                    :cljs (fn-and core/vector? (fn-> count (= 2)))))

(def double?     #?(:clj  (partial instance+? ADouble)
                    :cljs (fn-and ; TODO: probably a better way of finding out if it's a double/decimal
                             core/number?
                             (fn-> str (.indexOf ".") (not= -1))))) ; has decimal point

#?(:clj  (def indexed?   (partial instance+? clojure.lang.Indexed)))
#?(:clj  (def throwable? (partial instance+? java.lang.Throwable )))
         (def error?     (partial instance+? AError              ))
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