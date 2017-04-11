(ns quantum.docs.core.defnt:examples
  (:refer-clojure :exclude
    [+ zero? bit-and odd? even?])
  (:require
    [clojure.core :as c])
  (:import
    [quantum.core Numeric]))

;; - `dotyped`, `defnt`, and `fnt` create typed contexts in which their internal forms are analyzed
;; and overloads are resolved.
;; - When a function with type overloads is referenced outside of a typed context, then the
;; overload resolution will be done via protocol dispatch unless the function's overloads only
;; differ by arity. In either case, runtime type checks are required.
;; - At some later date, the analyzer will do its best to infer types.
;; - Even if the `defnt` is redefined, you won't have interface problems.

;; ----- Overload resolution -----

(defspec primitive?         (t/or boolean? byte? char? int? long? float? double?))
(defspec numeric-primitive? (t/- primitive? boolean?))

(defnt +*
  "Lax `+`. Continues on overflow/underflow."
  ([] 0)
  ;; Here `Number`, determined to be a class, is treated like an `instance?` predicate
  ([a (t/or numeric-primitive? Number)] a)
  ;; Note that you can envision any function arglist as an s/cat
  ([a ?, b ?] ; ? is t/?, an alias for t/infer
    (Numeric/add a b)) ; uses reflection to infer types
  ;; Protocols cannot participate in variadic arities, but we can get around this
  ;; TODO auto-gen extensions to variadic arities like [a b c], [a b c d], etc.
  ([a ?, b ? & args ?] (apply +* (+* a b) args))) ; the `apply` used in a typed context uses `reduce` underneath the covers

(defnt bit-and [n ?] (Numeric/bitAnd n))

(defnt zero? [n ?] (Numeric/isZero n))

(defnt even?
  [n ?] (zero? (bit-and n 1)))

(defnt +*-even
  "Lax `+` on only even numbers."
  [a even?, b even?] (+* a b))

(defnt get-in*
  ([x ? k0 ?]                                              (get x k0))
  ([x ? k0 ? k1 ?]                                         (Array/get x k0 k1))
  ([x ? k0 ? k1 ? k2 ?]                                    (Array/get x k0 k1 k2))
  ([x ? k0 ? k1 ? k2 ? k3 ?]                               (Array/get x k0 k1 k2 k3))
  ([x ? k0 ? k1 ? k2 ? k3 ? k4 ?]                          (Array/get x k0 k1 k2 k3 k4))
  ([x ? k0 ? k1 ? k2 ? k3 ? k4 ? k5 ?]                     (Array/get x k0 k1 k2 k3 k4 k5))
  ([x ? k0 ? k1 ? k2 ? k3 ? k4 ? k5 ? k6 ?]                (Array/get x k0 k1 k2 k3 k4 k5 k6))
  ([x ? k0 ? k1 ? k2 ? k3 ? k4 ? k5 ? k6 ? k7 ?]           (Array/get x k0 k1 k2 k3 k4 k5 k6 k7))
  ([x ? k0 ? k1 ? k2 ? k3 ? k4 ? k5 ? k6 ? k7 ? k8 ?]      (Array/get x k0 k1 k2 k3 k4 k5 k6 k7 k8))
  ([x ? k0 ? k1 ? k2 ? k3 ? k4 ? k5 ? k6 ? k7 ? k8 ? k9 ?] (Array/get x k0 k1 k2 k3 k4 k5 k6 k7 k8 k9))
  ([x ? k0 ? k1 ?]                                         (-> x (get k0) (get k1)))))

(argtypes get-in*) #_"=>" #_[[booleans          int]
                             [bytes             int]
                             ...
                             [IPersistentVector long]
                             ...
                             [ints              int int]
                             ...
                             [IPersistentVector long long]]

; RESULT:

; +* 0 arity
(definterface long•I (^long invoke []))

; `+*` 1 arity
(definterface byte•I•byte     (^byte   invoke [^byte   a0]))
(definterface char•I•char     (^char   invoke [^char   a0]))
(definterface int•I•int       (^int    invoke [^int    a0]))
(definterface long•I•long     (^long   invoke [^long   a0]))
(definterface float•I•float   (^float  invoke [^float  a0]))
(definterface double•I•double (^double invoke [^double a0]))

; `+*` 2-arity
(definterface byte•I•byte     (^byte   invoke [^byte   a0 ...]))
(definterface char•I•char     (^char   invoke [^char   a0 ...]))
(definterface int•I•int       (^int    invoke [^int    a0 ...]))
(definterface long•I•long     (^long   invoke [^long   a0 ...]))
(definterface float•I•float   (^float  invoke [^float  a0 ...]))
(definterface double•I•double (^double invoke [^double a0 ...]))
(definterface double•I•double (^double invoke [^double a0 ...]))
...

; `+*` 2-arity variadic
?

; `zero?`
; (->> (clojure.reflect/reflect quantum.core.Numeric) :members (filter #(= (:name %) 'isZero)) (clojure.pprint/print-table))
(definterface boolean•I•byte   (^boolean invoke [^byte   a0]))
(definterface boolean•I•char   (^boolean invoke [^char   a0]))
(definterface boolean•I•int    (^boolean invoke [^int    a0]))
(definterface boolean•I•long   (^boolean invoke [^long   a0]))
(definterface boolean•I•float  (^boolean invoke [^float  a0]))
(definterface boolean•I•double (^boolean invoke [^double a0]))

(def zero? (reify boolean•I•byte   (^boolean invoke [this ^byte   n] (Numeric/isZero n))
                  boolean•I•char   (^boolean invoke [this ^char   n] (Numeric/isZero n))
                  boolean•I•int    (^boolean invoke [this ^int    n] (Numeric/isZero n))
                  boolean•I•long   (^boolean invoke [this ^long   n] (Numeric/isZero n))
                  boolean•I•float  (^boolean invoke [this ^float  n] (Numeric/isZero n))
                  boolean•I•double (^boolean invoke [this ^double n] (Numeric/isZero n))))

(.invoke ^boolean•I•long zero? 3)

(let [^boolean•I•double z zero?] (.invoke z 3.0)) ; it's just a simple reify

#_(defnt even?
  [n ?] (zero? (bit-and n 1)))
#_=>
(def even? (reify ))

; Normally `zero?` when passed e.g. as a higher-order function might be like

;; ----- Spec'ed `defnt+`s -----

;; One thing that would be nice is to marry `defnt` with `clojure.spec`.
;; We want the specs to be reflected in the parameter declaration, type hints, and so on.
;;
;; We also want it to know about e.g., since a function returns `(< 5 x 100)`, then x must
;; be not just a number, but *specifically* a number between 5 and 100, exclusive.
;; Non-`Collection` datatypes are opaque and do not participate in this benefit (?).
;;
;; core.spec functions like `s/or`, `s/and`, `s/coll-of`, and certain type predicates are
;; able to be leveraged in computing the best overload with the least dynamic dispatch
;; possible.

(defnt+ example
  ([a (s/and even? #(< 5 % 100))
    b t/any
    c ::number-between-6-and-20
    d {:req-un [e  (default t/boolean? true)
                :f t/number?
                g  (default (s/or* t/number? t/sequential?) 0)]}]
   {:pre  (< a @c))
    :post (s/and (s/coll-of odd? :kind t/array?)
                 #(= (first %) c))}
   ...)
  ([a string?
    b (s/coll-of bigdec? :kind vector?)
    c t/any
    d t/any
   ...))

;; expands to:

(dv/def ::example:a (s/and even? #(< 5 % 100)))
(dv/def ::example:b t/any)
(dv/def ::example:c ::number-between-6-and-20)
(dv/def-map ::example:d
  :conformer (fn [m#] (assoc-when-not-contains m# :e true :g 0))
  :req-un [[:e t/boolean?]
           [:f t/number?]
           [:g (s/or* t/number? t/sequential?)]])
(dv/def ::example:__ret
  (s/and (s/coll-of odd? :kind t/array?)
                 #(= (first %) (:c ...)))) ; TODO fix `...`

;; -> TODO should it be:
(defnt example
  [^example:a a ^:example:b b ^example:c c ^example:d d]
  (let [ret (do ...)]
    (validate ret ::example:__ret)))
;; -> OR
(defnt example
  [^number? a b ^number? c ^map? d]
  (let [ret (do ...)]
    (validate ret ::example:__ret)))
;; ? The issue is one of performance. Maybe we don't want boxed values all over the place.

(s/fdef example
  :args (s/cat :a ::example:a
               :b ::example:b
               :c ::example:c
               :d ::example:d)
  :fn   ::example:__ret)


;; ----- TYPE INFERENCE ----- ;;

(expr-info '(let [a (Integer. 2) b (Double. 3)] a))
; => {:class java.lang.Integer, :prim? false}
(expr-info '(let [a (Integer. 2) b (Double. 3)] (if false a b)))
; => nil
;    But I'd like to have it infer the "LCD", namely, `(v/and number? (v/or* (fn= 2) (fn= 3)))`.

;; I realize that this also is probably prohibitively expensive.

(expr-info '(let [a (Integer. 2) b (Double. 3)] (if false a (int b))))
; => nil (inferred `Integer` or `int`)

(expr-info '(let [a (Integer. 2) b (Double. 3)] (if false a (Integer. b))))
; => {:class java.lang.Integer, :prim? false}

;; At very least it would be nice to have "spec inference". I.e. know, via `fdef`, that a
;; function meets a particular set of specs/characteristics and so any call to that function
;; will necessarily comply with the type.
