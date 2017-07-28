;; See https://jsperf.com/js-property-access-comparison — all property accesses (at least of length 1) seem to be equal

(ns quantum.core.test.defnt-equivalences
  (:refer-clojure :exclude [name])
  (:import clojure.lang.Named))

(require '[quantum.core.spec :as s]
         '[quantum.core.fn :refer [fn->]]
         '[clojure.spec.test :refer [instrument]])

(instrument)

(def object?   #(instance? #?(:clj Object :cljs js/Object) %))
(def integer?  (s/or unboxed-byte? unboxed-short? unboxed-int? unboxed-long? bigint?))
(def ?integer? (s/or nil? integer? boxed-byte? boxed-short? boxed-int? boxed-long?))
(def decimal?  (s/or unboxed-float? unboxed-double? bigdec?))
(def ?decimal? (s/or nil? decimal? boxed-float? boxed-double?))

(def integer-value? (s/or integer? (s/and decimal? decimal-is-integer-value?)))

(def any? (s/or nil? #?@(:clj  [boolean byte char short int long float double Object]
                         :cljs [js/Boolean js/Number js/Object js/String js/Symbol])))

(def *interfaces (atom {}))

(defnt ^:inline name
           ([x string?] x)
  #?(:clj  ([x Named  ] (.getName x))
     :cljs ([x INamed ] (-name x)))) ; TODO fix

(do
; direct dispatch interfaces (CLJ only)
#?(:clj (do (or (get @*interfaces 'java:lang:String•java:lang:String)
                (swap! *interfaces assoc 'java:lang:String•java:lang:String
                  ; Yes, a protocol creates an interface under the hood,
                  ; *but* a protocol doesn't care about hints, which help
                  ; in static type inference etc.
                  ; Also protocols box values
                  ; Thus an interface is required
                  (definterface java:lang:String•java:lang:String (^java.lang.String invoke [^java.lang.String a0]))))
            (or (get @*interfaces 'java:lang:String•clojure:lang:Named)
                (swap! *interfaces assoc 'java:lang:String•clojure:lang:Named
                  (definterface java:lang:String•clojure:lang:Named (^java.lang.String invoke [^clojure.lang.Named a0]))))))

; direct dispatch — invoked only if in a typed context
#?(:clj  ; this would probably be done within the macro too
         ; the only thing holding us back is that we want line numbers etc.
         (do (defonce *name:--overloads (atom {}))
             (def name:java:lang:String•java:lang:String
               (reify java:lang:String•java:lang:String (^java.lang.String invoke [_ ^java.lang.String x] x)))
             (def name:java:lang:String•clojure:lang:Named
               (reify java:lang:String•clojure:lang:Named (^java.lang.String invoke [_ ^clojure.lang.Named x] (.getName x))))
             (swap! *name:--overloads assoc
               'java:lang:String•java:lang:String   #'name:java:lang:String•java:lang:String
               'java:lang:String•clojure:lang:Named #'name:java:lang:String•clojure:lang:Named))
   :cljs (do (defn name:string [^string x] x)
             (defn name:cljs:core:INamed [^cljs.core.INamed x] (-name x))
             (aset name:overloads "name:string"           #'name:string)
             (aset name:overloads "name:cljs:core:INamed" #'name:cljs:core:INamed)))

; indirect dispatch — invoked only if incomplete type information (incl. in untyped context)
(do (defprotocol name:--protocol (name [a0]))
    (extend-protocol name:--protocol
      #?@(:clj  [java.lang.String     (name [^java.lang.String   x] x)]
          :cljs [string               (name [^java.lang.string   x] x)])
      #?@(:clj  [clojure.lang.Named   (name [^clojure.lang.Named x] (.getName x))]
          :cljs [cljs.core.INamed     (name [^cljs.core.INamed   x] (-name x))])))

; (optional) function — only when the `defnt` has an arity with 0 arguments

; (optional) inline macros — invoked only if in a typed context and not used as a function
(do #?(:clj (defmacro clj:name:java:lang:String  [a0] `(let [~'x ~a0] ~'x)))
    #?(:clj (defmacro cljs:name:string [a0] `(let [~'x ~a0] ~'x)))
    #?(:clj (defmacro clj:name:clojure:lang:Named   [a0] `(let [~'x ~a0] ~'(-name x))))
    #?(:clj (defmacro cljs:name:cljs:core:INamed [a0] `(let [~'x ~a0] ~'(.getName x)))))
)

(extend-defnt abc/name ; for use outside of ns
  ([a ?, b ?] (...)))

(name (read ))

; ================================================ ;

;; on CLJ will return unboxed booleans
;; `boolean` corresponds to primitive `boolean` in CLJ, and primitive `js/Boolean` in CLJS

(defnt ^:inline ->boolean
  ([x nil?   ] false)
  ([x boolean] x)
  ([x any?   ] true))

; ----- INFERRED ----- ;

;; return type inference

(defnt ^:inline ->boolean
  ([x nil?    > boolean] false)
  ([x boolean > boolean] x)
  ([x any?    > boolean] true))

;; further expansion

(defnt ^:inline ->boolean
             ([x nil?      > boolean] false)
             ([x boolean   > boolean] x)
             ;; `double` corresponds to primitive `double` in CLJ, and primitive `js/Number` in CLJS
             ([x double    > boolean] true)
             ([x object?   > boolean] true)
  #?@(:clj  [([x byte      > boolean] true)
             ([x char      > boolean] true)
             ([x short     > boolean] true)
             ([x int       > boolean] true)
             ([x long      > boolean] true)
             ([x float     > boolean] true)]
      :cljs [([x string?   > boolean] true)
             ([x js/Symbol > boolean] true)]))

(defn def-interfaces
  [{:keys [::*interfaces]}]
  *interfaces)

(defn atom? [x] (instance? clojure.lang.IAtom x))

(s/def ::*interfaces (s/and atom? (fn-> deref map?)))
(s/def ::signatures (s/coll-of (s/tuple symbol? (s/+ symbol?)) :kind sequential?))

(s/fdef def-interfaces
  :args (s/cat :a0 (s/keys :req [::signatures ::*interfaces]))
  #_:ret #_int?
  #_:fn #_(s/and #(>= (:ret %) (-> % :args :start))
             #(< (:ret %) (-> % :args :end))))

(s/def ::lang #{:clj :cljs})

(s/def ::expand-signatures:opts (s/keys :opt-un [::lang]))

(s/fdef expand-signatures
  :args (s/cat :signatures ::signatures
               :opts       (s/? ::expand-signatures:opts))
  :ret  ::signatures)

(defn expand-signatures [signatures]
  signatures)

(instrument)
(def-interfaces {::signatures  [['boolean ['nil?]]
                                ['boolean ['boolean]]
                                ['boolean ['any?]]]
                 ::*interfaces (atom {})})

(expand-signatures
  [['boolean ['nil?]]
   ['boolean ['boolean]]
   ['boolean ['any?]]]
  {:lang :clj})

(do
#?(:clj (do (or (get @*interfaces 'boolean•java:lang:Object)
                (swap! *interfaces assoc 'boolean•java:lang:Object
                  (definterface boolean•java:lang:String (^boolean invoke [^java.lang.String a0]))))
            (or (get @*interfaces 'boolean•clojure:lang:Named)
                (swap! *interfaces assoc 'boolean•clojure:lang:Named
                  (definterface boolean•clojure:lang:Named (^boolean invoke [^clojure.lang.Named a0]))))))

; direct dispatch — invoked only if in a typed context
#?(:clj  ; this would probably be done within the macro too
         ; the only thing holding us back is that we want line numbers etc.
         (do (defonce *name:--overloads (atom {}))
             (def name:java:lang:String•java:lang:String
               (reify java:lang:String•java:lang:String (^java.lang.String invoke [_ ^java.lang.String x] x)))
             (def name:java:lang:String•clojure:lang:Named
               (reify java:lang:String•clojure:lang:Named (^java.lang.String invoke [_ ^clojure.lang.Named x] (.getName x))))
             (swap! *name:--overloads assoc
               'java:lang:String•java:lang:String   #'name:java:lang:String•java:lang:String
               'java:lang:String•clojure:lang:Named #'name:java:lang:String•clojure:lang:Named))
   :cljs (do (defn name:string [^string x] x)
             (defn name:cljs:core:INamed [^cljs.core.INamed x] (-name x))
             (aset name:overloads "name:string"           #'name:string)
             (aset name:overloads "name:cljs:core:INamed" #'name:cljs:core:INamed)))

; indirect dispatch — invoked only if incomplete type information (incl. in untyped context)
(do (defprotocol name:--protocol (name [a0]))
    (extend-protocol name:--protocol
      #?@(:clj  [java.lang.String     (name [^java.lang.String   x] x)]
          :cljs [string               (name [^java.lang.string   x] x)])
      #?@(:clj  [clojure.lang.Named   (name [^clojure.lang.Named x] (.getName x))]
          :cljs [cljs.core.INamed     (name [^cljs.core.INamed   x] (-name x))])))

; (optional) function — only when the `defnt` has an arity with 0 arguments

; (optional) inline macros — invoked only if in a typed context and not used as a function
(do #?(:clj (defmacro clj:name:java:lang:String  [a0] `(let [~'x ~a0] ~'x)))
    #?(:clj (defmacro cljs:name:string [a0] `(let [~'x ~a0] ~'x)))
    #?(:clj (defmacro clj:name:clojure:lang:Named   [a0] `(let [~'x ~a0] ~'(-name x))))
    #?(:clj (defmacro cljs:name:cljs:core:INamed [a0] `(let [~'x ~a0] ~'(.getName x)))))
)

; ================================================ ;

#?(:clj
;; auto-upcasts to long or double (because 64-bit) unless you tell it otherwise
;; will error if not all return values can be safely converted to the return spec
(defnt ->int* > int
  ([x int] x)
  ([x Number] (.intValue x))
  ([x (s/or byte short char long float double)] (Primitive/uncheckedIntCast x))))

; ================================================ ;

#?(:clj
;; auto-upcasts to long or double (because 64-bit) unless you tell it otherwise
;; will error if not all return values can be safely converted to the return spec
(defnt ->int* > int
  ([x int] x)
  ([x Number] (.intValue x))
  ([x (s/or byte short char long float double)] (Primitive/uncheckedIntCast x))))

; ================================================ ;

(defnt !str
  ([] #?(:clj (StringBuilder.) :cljs (StringBuffer.)))
  ;; by default it's reasonably strict type checks (i.e. not allowing strange
  ;; byte manipulation)
  ([x #?(:clj (? {:any-in-numeric-range? true}) :cljs any?)]
    #?(:clj (StringBuilder. x) :cljs (StringBuffer. x))))

; ----- INFERRED ----- ;

; return type inference

(defnt ^:inline !str
  ([> #?(:clj StringBuilder :cljs StringBuffer)]
    #?(:clj (StringBuilder.) :cljs (StringBuffer.)))
  #?(:clj  ([x CharSequence > StringBuilder] (StringBuilder. x)))
  #?(:clj  ([x (s/and integer-value? (s/range-of int)) > StringBuilder]
             (let [x (->int* x)] (StringBuilder. x))))
  #?(:cljs ([x any? > StringBuffer] (StringBuffer. x))))

; further expansion

(defnt ^:inline !str
           ([> #?(:clj StringBuilder :cljs StringBuffer)]
             #?(:clj (StringBuilder.) :cljs (StringBuffer.)))
#?@(:clj  [([x CharSequence > StringBuilder] (StringBuilder. x))
           ([x byte? > StringBuilder]
             (let [x (int x)] (StringBuilder. x)))
           ([x short? > StringBuilder]
             (let [x (int x)] (StringBuilder. x)))
           ([x int? > StringBuilder] (StringBuilder. x))
           ([x (s/and long? (s/range-of int)) > StringBuilder]
             (let [x (int x)] (StringBuilder. x)))
           ([x (s/and java.math.BigInteger (s/range-of int)) > StringBuilder]
             (let [x (int x)] (StringBuilder. x)))
           ([x (s/and clojure.lang.BigInt  (s/range-of int)) > StringBuilder]
             (let [x (int x)] (StringBuilder. x)))
           ([x (s/and float?  decimal-is-integer-value? (s/range-of int)) > StringBuilder]
             (let [x (int x)] (StringBuilder. x)))
           ([x (s/and double? decimal-is-integer-value? (s/range-of int)) > StringBuilder]
             (let [x (int x)] (StringBuilder. x)))
           ([x (s/and java.math.BigDecimal decimal-is-integer-value? (s/range-of int)) > StringBuilder]
             (let [x (int x)] (StringBuilder. x)))])
 #?(:cljs  ([x any? > StringBuffer] (StringBuffer. x))))

; ----- IMPLEMENTATION ----- ;

(do
; direct dispatch interfaces
#?(:clj (do (or (get @*interfaces 'java:lang:StringBuilder•java:lang:CharSequence)
                (swap! *interfaces assoc 'java:lang:StringBuilder•java:lang:CharSequence
                  (definterface java:lang:StringBuilder•java:lang:CharSequence
                    (^java.lang.StringBuilder invoke [^java.lang.CharSequence a0]))))
            (or (get @*interfaces 'java:lang:StringBuilder•byte)
                (swap! *interfaces assoc 'java:lang:StringBuilder•byte
                  (definterface java:lang:StringBuilder•byte
                    (^java.lang.StringBuilder invoke [^byte a0]))))
            (or (get @*interfaces 'java:lang:StringBuilder•short)
                (swap! *interfaces assoc 'java:lang:StringBuilder•short
                  (definterface java:lang:StringBuilder•short
                    (^java.lang.StringBuilder invoke [^short a0]))))
            (or (get @*interfaces 'java:lang:StringBuilder•int)
                (swap! *interfaces assoc 'java:lang:StringBuilder•int
                  (definterface java:lang:StringBuilder•int
                    (^java.lang.StringBuilder invoke [^int a0]))))
            (or (get @*interfaces 'java:lang:StringBuilder•long)
                (swap! *interfaces assoc 'java:lang:StringBuilder•long
                  (definterface java:lang:StringBuilder•long
                    (^java.lang.StringBuilder invoke [^long a0]))))
            (or (get @*interfaces 'java:lang:StringBuilder•java:math:BigInteger)
                (swap! *interfaces assoc 'java:lang:StringBuilder•java:math:BigInteger
                  (definterface java:lang:StringBuilder•java:math:BigInteger
                    (^java.lang.StringBuilder invoke [^java.math.BigInteger a0]))))
            (or (get @*interfaces 'java:lang:StringBuilder•clojure:lang:BigInt)
                (swap! *interfaces assoc 'java:lang:StringBuilder•clojure:lang:BigInt
                  (definterface java:lang:StringBuilder•clojure:lang:BigInt
                    (^java.lang.StringBuilder invoke [^clojure.lang.BigInt a0]))))
            (or (get @*interfaces 'java:lang:StringBuilder•float)
                (swap! *interfaces assoc 'java:lang:StringBuilder•float
                  (definterface java:lang:StringBuilder•float
                    (^java.lang.StringBuilder invoke [^float a0]))))
            (or (get @*interfaces 'java:lang:StringBuilder•double)
                (swap! *interfaces assoc 'java:lang:StringBuilder•double
                  (definterface java:lang:StringBuilder•double
                    (^java.lang.StringBuilder invoke [^double a0]))))
            (or (get @*interfaces 'java:lang:StringBuilder•java:math:BigDecimal)
                (swap! *interfaces assoc 'java:lang:StringBuilder•java:math:BigDecimal
                  (definterface java:lang:StringBuilder•java:math:BigDecimal
                    (^java.lang.StringBuilder invoke [^java.math.BigDecimal a0])))))

; direct dispatch
#?(:clj  (do (defonce *!str:--overloads (atom {}))
             (def !str:java:lang:StringBuilder•java:lang:CharSequence
               (reify StringBuilder•CharSequence
                 (^java.lang.StringBuilder invoke [_ ^java.lang.CharSequence x] (StringBuilder. x))))
             (def !str:java:lang:StringBuilder•long
               (reify StringBuilder•long
                 (^java.lang.StringBuilder invoke [_ ^long x] (StringBuilder. x))))
             (swap! *!str:--overloads assoc
               'java:lang:StringBuilder•CharSequence #'!str:java:lang:StringBuilder•java:lang:CharSequence
               'java:lang:StringBuilder•long         #'!str:java:lang:StringBuilder•long))
   :cljs nil) ; No specialized dispatch to do

; indirect dispatch
#?(:clj ; no CLJS because unnecessary
(do (defprotocol !str:--protocol (!str:--dispatch [a0]))
    (extend-protocol !str:--protocol
      java.lang.CharSequence (!str:--dispatch [^java.lang.CharSequence x] (StringBuilder. x))
      java.lang.Long         (!str:--dispatch [^java.lang.Long         x]
                               (let [x (int x)] (StringBuilder. x))))))

; function
(defn !str
           ([] #?(:clj (StringBuilder.) :cljs (StringBuffer.)))
  #?(:clj  ([x] (!str:--dispatch x))
     :cljs ([x] (StringBuffer. x))))

; inline macros
(do #?(:clj (defmacro clj:!str  [] `(StringBuilder.)))
    #?(:clj (defmacro cljs:!str
              ([] `(StringBuffer.))
              ([a0] `(let [~'x ~a0] ~'(StringBuffer. x)))))
    #?(:clj (defmacro clj:!str:java:lang:CharSequence [a0] `(let [~'x ~a0] ~'x)))
    #?(:clj (defmacro clj:!str:long         [a0]
              `(let [~'x ~a0]
                 ~'(let [x (int x)] (StringBuilder. x))))))
)

; ================================================ ;

(defnt ^:inline >
  #?(:clj  ([a ?       b ?      ] (Numeric/gt  a b))
     :cljs ([a double? b double?] (cljs.core/> a b))))

; ----- INFERRED ----- ;

(defnt !str
  ([> #?(:clj StringBuilder :cljs StringBuffer)]
    #?(:clj (StringBuilder.) :cljs (StringBuffer.)))
  ([x #?(:clj  (s/or (s/class= CharSequence) int?)
         :cljs any?)
    > #?(:clj StringBuilder :cljs StringBuffer)]
    #?(:clj (StringBuilder. x) :cljs (StringBuffer. x))))

; ================================================ ;

(defnt ^:inline str
           ([] "")
           ([x nil?  ] "")
  #?(:clj  ([x Object] (.toString x))  ; could have inferred but there may be other objects who have overridden .toString
     :cljs ([x any?] (.join #js [x] ""))) ; infers that it returns a string
           ([x ? & xs (s/seq ?)] ; TODO should have automatic currying?
             (let [sb (!str (str x))]
               (doseq [x' xs] (.append sb (str x'))) ; TODO is `doseq` the right approach?
               (.toString sb))))

; ================================================ ;

(defnt ^:inline custom
  [x (s/if double?
           (s/or (s/fnt [x ?] (> x 3)) ; uses the above-defined `>`
                 (s/fnt [x ?] (< x 0.1)))
           (s/or string? !string?))
   y ?] (str x (name y))) ; uses the above-defined `name`

#_"
; infer if necessary
y -> String, Named
"

(do
; direct dispatch setup
#?(:clj ; this will be done within the macro, not generated
        (do (or (get @*interfaces 'String•String)
                (swap! *interfaces assoc
                  ; Yes, a protocol creates an interface under the hood,
                  ; *but* a protocol only allows certain types of primitives
                  ; Thus an interface is required
                  (definterface String•String (invoke ^String [^String a0]))))
            (or (get @*interfaces 'String•Named)
                (swap! *interfaces assoc
                  (definterface String•Named (invoke ^String [^Named a0]))))))
; direct dispatch
#?(:clj  ; even this might be done within the macro too
         ; the only thing holding us back is that we want line numbers etc.
         (do (swap! name:overloads assoc
               'String•String (reify String•String
                                (^String [^String x] x))
               'String•Named  (reify String•Named
                                (^String [^Named x] (.getName x)))))
   :cljs (do (def name:string•string
               (fn [^string x] x))
             (def name:string•INamed
               (fn [^INamed x] (-name x))) ; TODO fix this
             (set! (.-string•string name:overloads) name:string•string)
             (set! (.-string•INamed name:overloads) name:string•INamed)))
; indirect dispatch
#?(:clj  (def name
           ...)
   :cljs (def name
           ...))
; (optional) inline macros — invoked only if in a typed context and not used as a function
#?(:clj (defmacro name:String•String [x] x))
#?(:clj (defmacro name:String•Named [x] `(let [~'x ~x] (.getName x))))
)
