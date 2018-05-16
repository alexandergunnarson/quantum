(ns quantum.untyped.core.type.compare
  "Set-theoretic comparisons on types (subset, equality, superset, intersection, disjointness)."
          (:refer-clojure :exclude
            [compare < <= = not= >= >, ==])
          (:require
            [clojure.core                           :as c]
            [quantum.untyped.core.collections.logic
              :refer [seq-and seq-or]]
            ;; TODO remove this dependency
            [quantum.untyped.core.classes           :as uclass]
            [quantum.untyped.core.compare
              :refer [==]]
            [quantum.untyped.core.data.bits         :as ubit]
            [quantum.untyped.core.defnt
              :refer [defns defns-]]
            [quantum.untyped.core.error
              :refer [err! TODO]]
            [quantum.untyped.core.fn
              :refer [fn' fn1]]
            [quantum.untyped.core.logic
              :refer [ifs]]
            ;; TODO remove this dependency
            [quantum.untyped.core.type.core         :as utcore]
            [quantum.untyped.core.type.reifications :as utr
              :refer [type?
                      universal-set empty-set
                      not-type? or-type? and-type?
                      protocol-type? class-type?
                      value-type?]]
            [quantum.untyped.core.vars
              :refer [def-]])
  #?(:clj (:import
            [quantum.untyped.core.analyze.expr Expression]
            [quantum.untyped.core.type.reifications
               UniversalSetType EmptySetType
               NotType OrType AndType
               ProtocolType ClassType
               ValueType])))

(declare compare < <= = not= >= > >< <>)

;; ===== (Comparison) idents ===== ;;

(def ^:const <ident -1)
(def ^:const =ident  0)
(def ^:const >ident  1)
(def ^:const ><ident 2)
(def ^:const <>ident 3)

(def- fn<  (fn' <ident))
(def- fn=  (fn' =ident))
(def- fn>  (fn' >ident))
(def- fn>< (fn' ><ident))
(def- fn<> (fn' <>ident))

(def comparisons #{<ident =ident >ident ><ident <>ident})

(defns inverse [comparison comparisons > comparisons]
  (case comparison
    -1      >ident
     1      <ident
    (0 2 3) comparison))

;; ===== Comparison Implementations ===== ;;

(defns- compare|todo [t0 type?, t1 type?]
  (err! "TODO dispatch" {:t0 t0 :t0|class (type t0)
                         :t1 t1 :t1|class (type t1)}))

;; ----- Multiple ----- ;;

(defns- compare|atomic+or [t0 type?, ^OrType t1 or-type? > comparisons]
  (let [ts (.-args t1)]
    (first
      (reduce
        (fn [[ret found] t]
          (let [c      (compare t0 t)
                found' (-> found (ubit/conj c) long)]
            (ifs (or (ubit/contains? found' <ident)
                     (ubit/contains? found' =ident))
                 (reduced [<ident found'])

                 (or (ubit/contains? found' ><ident)
                     (and (ubit/contains? found' >ident)
                          (ubit/contains? found' <>ident)))
                 [><ident found']

                 [c found'])))
        [<>ident ubit/empty]
        ts))))

(defns- compare|atomic+and [t0 type?, ^AndType t1 and-type? > comparisons]
  (let [ts (.-args t1)]
    (first
      (reduce
        (fn [[ret found] t]
          (let [c (compare t0 t)]
            (if (c/= c =ident)
                (reduced [>ident nil])
                (let [found' (-> found (ubit/conj c) long)
                      ret'   (ifs (ubit/contains? found' ><ident)
                                  (if (c/= found' (ubit/conj ><ident <>ident))
                                      <>ident
                                      ><ident)

                                  (ubit/contains? found' <>ident)
                                  (ifs (ubit/contains? found' <ident) <>ident
                                       (ubit/contains? found' >ident) >ident
                                       c)

                                  c)]
                  [ret' found']))))
        [3 ubit/empty]
        ts))))

;; ----- UniversalSet ----- ;;

(def- compare|universal+empty fn>)

(defns- compare|universal+not [t0 type?, t1 not-type? > comparisons]
  (let [t1|inner (utr/not-type>inner-type t1)]
    (ifs (= t1|inner universal-set) >ident
         (= t1|inner empty-set)     =ident
         (compare t0 t1|inner))))

(def- compare|universal+or       fn>)
(def- compare|universal+and      fn>)
(def- compare|universal+expr     compare|todo)
(def- compare|universal+protocol fn>)
(def- compare|universal+class    fn>)
(def- compare|universal+value    fn>)

;; ----- EmptySet ----- ;;

(defns- compare|empty+not [t0 type?, t1 not-type? > comparisons]
  (let [t1|inner (utr/not-type>inner-type t1)]
    (if (= t1|inner universal-set) =ident <ident)))

(def- compare|empty+or       fn<)
(def- compare|empty+and      fn<)
(def- compare|empty+expr     compare|todo)
(def- compare|empty+protocol fn<)
(def- compare|empty+class    fn<)
(def- compare|empty+value    fn<)

;; ----- NotType ----- ;;

(defns- compare|not+not [t0 not-type?, t1 not-type? > comparisons]
  (let [c (compare (utr/not-type>inner-type t0) (utr/not-type>inner-type t1))]
    (case c
      0 =ident
     -1 >ident
      1 <ident
      2 ><ident
      3 ><ident)))

(def- compare|not+or compare|atomic+or)

(def- compare|not+and compare|atomic+and)

(defns- compare|not+protocol [t0 not-type?, t1 protocol-type? > comparisons]
  (let [t0|inner (utr/not-type>inner-type t0)]
    (if (= t0|inner empty-set) >ident <>ident)))

(defns- compare|not+class [t0 not-type?, t1 class-type? > comparisons]
  (let [t0|inner (utr/not-type>inner-type t0)]
    (if (= t0|inner empty-set)
        >ident
        (case (compare t0|inner t1)
          ( 1 0) <>ident
          (-1 2) ><ident
          3      >ident))))

(defns- compare|not+value [t0 not-type?, t1 value-type? > comparisons]
  (let [t0|inner (utr/not-type>inner-type t0)]
    (if (= t0|inner empty-set)
        >ident
        ;; nothing is ever < ValueType (and therefore never ><)
        (case (compare t0|inner t1)
          (1 0) <>ident
          3     >ident))))

;; ----- OrType ----- ;;

;; TODO performance can be improved here by doing fewer comparisons
(defns- compare|or+or [^OrType t0 or-type?, ^OrType t1 or-type? > comparisons]
  (let [l (->> t0 .-args (seq-and (fn1 < t1)))
        r (->> t1 .-args (seq-and (fn1 < t0)))]
    (if l
        (if r =ident <ident)
        (if r
            >ident
            (if (->> t0 .-args (seq-and (fn1 <> t1)))
                <>ident
                ><ident)))))

(defns- compare|or+and [^OrType t0 or-type?, ^AndType t1 and-type? > comparisons]
  (let [r (->> t1 .-args (seq-and (fn1 < t0)))]
    (if r >ident <>ident)))

(def- compare|class+or compare|atomic+or)
(def- compare|value+or compare|atomic+or)

;; ----- AndType ----- ;;

(defns- compare|and+and [^AndType t0 and-type?, ^AndType t1 and-type? > comparisons]
  (TODO))

(def- compare|class+and compare|atomic+and)
(def- compare|value+and compare|atomic+and)

;; ----- Expression ----- ;;

(defns- compare|expr+expr [t0 _, t1 _ > comparisons] (if (c/= t0 t1) =ident <>ident))

(def- compare|expr+value fn<>)

;; ----- ProtocolType ----- ;;

(defns- compare|protocol+protocol [t0 protocol-type?, t1 protocol-type? > comparisons]
  (if (== (utr/protocol-type>protocol t0) (utr/protocol-type>protocol t1))
      =ident
      <>ident))

;; TODO transition to `compare|protocol+value` when stable
(defns- compare|value+protocol [t0 value-type?, t1 protocol-type? > comparisons]
  (let [v (utr/value-type>value       t0)
        p (utr/protocol-type>protocol t1)]
    (if (satisfies? p v) <ident <>ident)))

;; ----- ClassType ----- ;;

(defns compare|class+class*
  "Compare extension (generality|specificity) of ->`c0` to ->`c1`.
   `0`  means they are equally general/specific:
     - ✓ `(t/= c0 c1)`    : the extension of ->`c0` is equal to             that of ->`c1`.
   `-1` means ->`c0` is less general (more specific) than ->`c1`.
     - ✓ `(t/< c0 c1)`    : the extension of ->`c0` is a strict subset   of that of ->`c1`.
   `1`  means ->`c0` is more general (less specific) than ->`c1`:
     - ✓ `(t/> c0 c1)`    : the extension of ->`c0` is a strict superset of that of ->`c1`.
   `2`  means:
     - ✓ `(t/>< c0 c1)`   : the intersect of the extensions of ->`c0` and ->`c1` is non-empty,
                             but neither ->`c0` nor ->`c1` share a subset/equality/superset
                             relationship.
   `3`  means their generality/specificity is incomparable:
     - ✓ `(t/<> c0 c1)`   : the extension of ->`c0` is disjoint w.r.t. to that of ->`c1`.
   Unboxed primitives are considered to be less general (more specific) than boxed primitives."
  [^Class c0 class? ^Class c1 class? > comparisons]
  #?(:clj (ifs (== c0 c1)                                =ident
               (== c0 Object)                            >ident
               (== c1 Object)                            <ident
               (== (utcore/boxed->unboxed c0) c1)        >ident
               (== c0 (utcore/boxed->unboxed c1))        <ident
               ;; we'll consider the two unrelated
               (not (utcore/array-depth-equal? c0 c1)) <>ident
               (.isAssignableFrom c0 c1)                 >ident
               (.isAssignableFrom c1 c0)                 <ident
               ;; multiple inheritance of interfaces
               (or (and (uclass/interface? c0)
                        (not (uclass/final? c1)))
                   (and (uclass/interface? c1)
                        (not (uclass/final? c0)))) ><ident
               <>ident)
     :cljs (TODO)))

(defns- compare|class+class [t0 class-type?, t1 class-type? > comparisons]
  (compare|class+class* (utr/class-type>class t0) (utr/class-type>class t1)))

(defns- compare|class+value [t0 class-type?, t1 value-type? > comparisons]
  (let [c (utr/class-type>class t0)
        v (utr/value-type>value t1)]
    (if (instance? c v) >ident <>ident)))

;; ----- ValueType ----- ;;

(defns- compare|value+value
  "What we'd really like is to have a different version of .equals or .equiv
   like .equivBehavior in which it returns whether any behavior is different
   whatsoever between two objects. For instance, `[52]` behaves differently from
   `(list 52)` because `(get [52] 0)` -> `52` while `(get (list 52) 0)` -> `nil`.

   The issue with this is that yes, one could implement a `strict=` that tries to
   emulate this behavior, but even though it is implementable for 'transparent'
   objects such as collections, it is not for 'opaque' objects, which would
   potentially have to have custom equality behavior per class. So we will simply
   reluctantly accept whatever `=` tells us as well as the fallout that results.
   Thus, `(t/or (t/value []) (t/value (list)))` will result in `(t/value [])`,
   which is not ideal but both feasible and better than the alternative."
  [t0 value-type?, t1 value-type? > comparisons]
  (if (c/= (utr/value-type>value t0)
           (utr/value-type>value t1))
      =ident
      <>ident))

;; ===== Dispatch ===== ;;

;; TODO take away var indirection once done
(def- compare|dispatch
  (let [inverted (fn [f] (fn [t0 t1] (inverse (f t1 t0))))]
    {UniversalSetType
       {UniversalSetType #'fn=
        EmptySetType     #'compare|universal+empty
        NotType          #'compare|universal+not
        OrType           #'compare|universal+or
        AndType          #'compare|universal+and
        Expression       #'compare|universal+expr
        ProtocolType     #'compare|universal+protocol
        ClassType        #'compare|universal+class
        ValueType        #'compare|universal+value}
     EmptySetType
       {UniversalSetType (inverted #'compare|universal+empty)
        EmptySetType     #'fn=
        NotType          #'compare|empty+not
        OrType           #'compare|empty+or
        AndType          #'compare|empty+and
        Expression       #'compare|empty+expr
        ProtocolType     #'compare|empty+protocol
        ClassType        #'compare|empty+class
        ValueType        #'compare|empty+value}
     NotType
       {UniversalSetType (inverted #'compare|universal+not)
        EmptySetType     (inverted #'compare|empty+not)
        NotType          #'compare|not+not
        OrType           #'compare|not+or
        AndType          #'compare|not+and
        Expression       #'fn<>
        ProtocolType     #'compare|not+protocol
        ClassType        #'compare|not+class
        ValueType        #'compare|not+value}
     OrType
       {UniversalSetType (inverted #'compare|universal+or)
        EmptySetType     (inverted #'compare|empty+or)
        NotType          (inverted #'compare|not+or)
        OrType           #'compare|or+or
        AndType          #'compare|or+and
        Expression       #'fn<>
        ProtocolType     #'compare|todo
        ClassType        (inverted #'compare|class+or)
        ValueType        (inverted #'compare|value+or)}
     AndType
       {UniversalSetType (inverted #'compare|universal+and)
        EmptySetType     (inverted #'compare|empty+and)
        NotType          #'compare|todo
        OrType           (inverted #'compare|or+and)
        AndType          #'compare|and+and
        Expression       #'fn<>
        ProtocolType     #'compare|todo
        ClassType        (inverted #'compare|class+and)
        ValueType        (inverted #'compare|value+and)}
     ;; TODO review this
     Expression
       {UniversalSetType (inverted #'compare|universal+expr)
        EmptySetType     (inverted #'compare|empty+expr)
        NotType          #'compare|todo
        OrType           #'compare|todo
        AndType          #'compare|todo
        Expression       #'compare|expr+expr
        ProtocolType     #'compare|todo
        ClassType        #'fn<> ; TODO not entirely true
        ValueType        #'compare|expr+value}
     ProtocolType
       {UniversalSetType (inverted #'compare|universal+protocol)
        EmptySetType     (inverted #'compare|empty+protocol)
        NotType          (inverted #'compare|not+protocol)
        OrType           #'compare|todo
        AndType          #'compare|todo
        Expression       #'fn<>
        ProtocolType     #'compare|protocol+protocol
        ClassType        #'compare|todo
        ValueType        (inverted #'compare|value+protocol)}
     ClassType
       {UniversalSetType (inverted #'compare|universal+class)
        EmptySetType     (inverted #'compare|empty+class)
        NotType          (inverted #'compare|not+class)
        OrType           #'compare|class+or
        AndType          #'compare|class+and
        Expression       #'fn<>
        ProtocolType     #'compare|todo
        ClassType        #'compare|class+class
        ValueType        #'compare|class+value}
     ValueType
       {UniversalSetType (inverted #'compare|universal+value)
        EmptySetType     (inverted #'compare|empty+value)
        NotType          (inverted #'compare|not+value)
        OrType           #'compare|value+or
        AndType          #'compare|value+and
        Expression       (inverted #'compare|expr+value)
        ProtocolType     #'compare|value+protocol
        ClassType        (inverted #'compare|class+value)
        ValueType        #'compare|value+value}}))

;; ===== Operators ===== ;;

(defns compare
  "Returns the value of the comparison of the extensions of ->`t0` and ->`t1`.
   `-1` means (ex ->`t0`) ⊂                                 (ex ->`t1`)
    `0` means (ex ->`t0`) =                                 (ex ->`t1`)
    `1` means (ex ->`t0`) ⊃                                 (ex ->`t1`)
    `2` means (ex ->`t0`) shares other intersect w.r.t. (∩) (ex ->`t1`)
    `3` means (ex ->`t0`) disjoint               w.r.t. (∅) (ex ->`t1`)

   Does not compare cardinalities or other relations of sets, but rather only sub/superset
   relations."
  [t0 type?, t1 type? > comparisons]
  (let [dispatched (-> compare|dispatch (get (type t0)) (get (type t1)))]
    (if (nil? dispatched)
        (err! (str "Types not handled: " {:t0 t0 :t1 t1}) {:t0 t0 :t1 t1})
        (dispatched t0 t1))))

(defns <
  "Computes whether the extension of type ->`t0` is a strict subset of that of ->`t1`."
  ([t1 type?] #(< % t1))
  ([t0 type?, t1 type? > boolean?] (c/= (compare t0 t1) <ident)))

(defns <=
  "Computes whether the extension of type ->`t0` is a (lax) subset of that of ->`t1`."
  ([t1 type?] #(<= % t1))
  ([t0 type?, t1 type? > boolean?]
    (let [ret (compare t0 t1)] (or (c/= ret <ident) (c/= ret =ident)))))

(defns =
  "Computes whether the extension of type ->`t0` is equal to that of ->`t1`."
  ([t1 type?] #(= % t1))
  ([t0 type?, t1 type? > boolean?] (c/= (compare t0 t1) =ident)))

(defns not=
  "Computes whether the extension of type ->`t0` is not equal to that of ->`t1`."
  ([t1 type?] #(not= % t1))
  ([t0 type?, t1 type? > boolean?] (not (= t0 t1))))

(defns >=
  "Computes whether the extension of type ->`t0` is a (lax) superset of that of ->`t1`."
  ([t1 type?] #(>= % t1))
  ([t0 type?, t1 type? > boolean?]
    (let [ret (compare t0 t1)] (or (c/= ret >ident) (c/= ret =ident)))))

(defns >
  "Computes whether the extension of type ->`t0` is a strict superset of that of ->`t1`."
  ([t1 type?] #(> % t1))
  ([t0 type?, t1 type? > boolean?] (c/= (compare t0 t1) >ident)))

(defns ><
  "Computes whether it is the case that the intersect of the extensions of type ->`t0`
   and ->`t1` is non-empty, and neither ->`t0` nor ->`t1` share a subset/equality/superset
   relationship."
  ([t1 type?] #(>< % t1))
  ([t0 type?, t1 type? > boolean?] (c/= (compare t0 t1) ><ident)))

(defns <>
  "Computes whether the respective extensions of types ->`t0` and ->`t1` are disjoint."
  ([t1 type?] #(<> % t1))
  ([t0 type? t1 type? > boolean?] (c/= (compare t0 t1) <>ident)))

