(ns
  ^{:doc "Type-checking predicates, 'transientization' checks, class aliases, etc."
    :attribution "Alex Gunnarson"}
  quantum.core.type
  (:refer-clojure :exclude
    [vector? map? set? associative? seq? string? keyword? fn?
     nil? list? coll? char? symbol? record? number? integer? float? decimal?])
  (:require-quantum [fn logic ns set map macros err log])
  (:require
    [quantum.core.type.core :as tcore]
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

#?(:clj
(eval
  `(macros/maptemplate
   ~(fn [[type-pred types]]
     (let [code
            (when (core/symbol? type-pred)
              (concat
                `(defnt ~type-pred)
                `(~type-pred ([obj#] true)
                  :default   ([obj#] false))
                (if (quantum.core.macros/symbol-eq? type-pred 'nil?)
                    (list)
                    (list 'nil? `([obj#] false)))))]
       code))
   ~types)))

(defnt listy?      listy?      ([obj] true) :default ([obj] false))
(defnt array-list? array-list? ([obj] true) :default ([obj] false))
(defnt queue?      queue?      ([obj] true) :default ([obj] false))
(defnt lseq?       lseq?       ([obj] true) :default ([obj] false))
(defnt pattern?    pattern?    ([obj] true) :default ([obj] false))
(defnt regex?      regex?      ([obj] true) :default ([obj] false))
(defnt sorted-map? sorted-map? ([obj] true) :default ([obj] false))
(defnt editable?   editable?   ([obj] true) :default ([obj] false))
(defnt transient?  transient?  ([obj] true) :default ([obj] false))
(defnt boolean?    boolean?    ([obj] true) :default ([obj] false))
   
(def map-entry?  #?(:clj  (partial instance+? clojure.lang.MapEntry)
                    :cljs (fn-and core/vector? (fn-> count (= 2)))))

(def double?     #?(:clj  (partial instance+? ADouble)
                    :cljs (fn-and ; TODO: probably a better way of finding out if it's a double/decimal
                             core/number?
                             (fn-> str (.indexOf ".") (not= -1))))) ; has decimal point

#?(:clj  (def indexed?   (partial instance+? clojure.lang.Indexed)))
#?(:clj  (def throwable? (partial instance+? java.lang.Throwable )))
         (def error?     (partial instance+? AError              ))

; ; Unable to resolve symbol: isArray in this context
; ; http://stackoverflow.com/questions/2725533/how-to-see-if-an-object-is-an-array-without-using-reflection
; ; http://stackoverflow.com/questions/219881/java-array-reflection-isarray-vs-instanceof
; ;#?(:clj (def array?      (compr type (jfn java.lang.Class/isArray)))) ; uses reflection...
; ;#?(:clj (def array?      (fn-> type .isArray)))

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