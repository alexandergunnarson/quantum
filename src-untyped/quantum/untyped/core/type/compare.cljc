(ns quantum.untyped.core.type.compare
  "Set-theoretic comparisons on types (subset, equality, superset, intersection, disjointness)."
          (:refer-clojure :exclude
            [compare < <= = not= >= >, ==])
          (:require
            [clojure.core                           :as c]
            [quantum.untyped.core.analyze.expr
              #?@(:cljs [:refer [Expression]])]
            [quantum.untyped.core.collections.logic
              :refer [seq-and seq-or]]
            ;; TODO remove this dependency
            [quantum.untyped.core.classes           :as uclass]
            [quantum.untyped.core.collections       :as uc]
            [quantum.untyped.core.compare           :as ucomp
              :refer [==]]
            [quantum.untyped.core.core              :as ucore]
            [quantum.untyped.core.data.bits         :as ubit]
            [quantum.untyped.core.data.hash         :as uhash]
            [quantum.untyped.core.data.set          :as uset
              :refer [<ident =ident >ident ><ident <>ident comparison?]]
            [quantum.untyped.core.defnt
              :refer [defns defns-]]
            [quantum.untyped.core.error
              :refer [err! TODO]]
            [quantum.untyped.core.fn
              :refer [fn' fn1]]
            [quantum.untyped.core.logic
              :refer [case-val ifs]]
            [quantum.untyped.core.reducers
              :refer [educe]]
            [quantum.untyped.core.spec              :as us]
            ;; TODO remove this dependency
            [quantum.untyped.core.type.core         :as utcore]
            [quantum.untyped.core.type.reifications :as utr
              :refer [type?
                      universal-set empty-set
                      not-type? or-type? and-type?
                      protocol-type? class-type?
                      value-type?
                      fn-type?
                      #?@(:cljs [UniversalSetType EmptySetType
                                 NotType OrType AndType
                                 ProtocolType DirectProtocolType ClassType
                                 UnorderedType OrderedType
                                 ValueType
                                 FnType
                                 MetaType MetaOrType
                                 ReactiveType])]]
            [quantum.untyped.core.vars
              :refer [def-]])
  #?(:clj (:import
            [quantum.untyped.core.analyze.expr Expression]
            [quantum.untyped.core.type.reifications
               UniversalSetType EmptySetType
               NotType OrType AndType
               ProtocolType ClassType
               UnorderedType OrderedType
               ValueType
               FnType
               MetaType MetaOrType
               ReactiveType])))

(ucore/log-this-ns)

(declare compare < <= = not= >= > >< <> combine-comparisons)

(def inverted (fn [f] (fn [t0 t1] (uset/invert-comparison (f t1 t0)))))

;; ===== (Comparison) idents and bit-sets ===== ;;

(def- fn<  (fn' <ident))
(def- fn=  (fn' =ident))
(def- fn>  (fn' >ident))
(def- fn>< (fn' ><ident))
(def- fn<> (fn' <>ident))

(def b<       (reduce ubit/conj ubit/empty [<ident]))
(def b<|><    (reduce ubit/conj ubit/empty [<ident  ><ident]))
(def b<|><|<> (reduce ubit/conj ubit/empty [<ident  ><ident <>ident]))
(def b<|<>    (reduce ubit/conj ubit/empty [<ident  <>ident]))
(def b=|><    (reduce ubit/conj ubit/empty [=ident  ><ident]))
(def b=|><|<> (reduce ubit/conj ubit/empty [=ident  ><ident <>ident]))
(def b=|<>    (reduce ubit/conj ubit/empty [=ident  <>ident]))
(def b>       (reduce ubit/conj ubit/empty [>ident]))
(def b>|><    (reduce ubit/conj ubit/empty [>ident  ><ident]))
(def b>|><|<> (reduce ubit/conj ubit/empty [>ident  ><ident <>ident]))
(def b>|<>    (reduce ubit/conj ubit/empty [>ident  <>ident]))
(def b><      (reduce ubit/conj ubit/empty [><ident ><ident]))
(def b><|<>   (reduce ubit/conj ubit/empty [><ident <>ident]))
(def b<>      (reduce ubit/conj ubit/empty [<>ident]))

(defn bit-set>set [x]
  (cond-> #{}
    (ubit/contains? x <ident)  (conj <ident)
    (ubit/contains? x =ident)  (conj =ident)
    (ubit/contains? x >ident)  (conj >ident)
    (ubit/contains? x ><ident) (conj ><ident)
    (ubit/contains? x <>ident) (conj <>ident)))

(defn- comparison-err! [t0+t1 t1+t0]
  (err! "comparison not thought through yet"
        {:t0+t1 (bit-set>set t0+t1) :t1+t0 (bit-set>set t1+t0)}))

;; ===== Comparison Implementations ===== ;;

(defns- compare|todo [t0 type?, t1 type?]
  (err! "TODO dispatch" {:t0 t0 :t0|class (type t0)
                         :t1 t1 :t1|class (type t1)}))

;; ----- Multiple ----- ;;

(defns- compare|atomic+or [t0 type?, ^OrType t1 or-type? > comparison?]
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
      (.-args t1))))

(defns- compare|atomic+and [t0 type?, ^AndType t1 and-type? > comparison?]
  (let [ts (.-args t1)]
    (first
      (reduce
        (fn [[ret found] t]
          (let [c (compare t0 t)]
            (if (or (c/= c =ident) (c/= c >ident))
                (reduced [>ident nil])
                (let [found' (-> found (ubit/conj c) long)
                      ret'   (ifs (ubit/contains? found' ><ident)
                                    (if (c/= found' (ubit/conj ><ident <>ident)) <>ident ><ident)
                                  (ubit/contains? found' <>ident)
                                    (if (ubit/contains? found' <ident) <>ident c)
                                  c)]
                  [ret' found']))))
        [<>ident ubit/empty]
        ts))))

(defns- compare|value+type [t0 utr/value-type?, t1 type? > comparison?]
  (if (t1 (utr/value-type>value t0)) <ident <>ident))

(defns- compare|meta+non-meta [t0 utr/meta-type?, t1 type? > comparison?]
  (compare (utr/meta-type>inner-type t0) t1))

(defns- compare|non-meta+meta [t0 type?, t1 utr/meta-type? > comparison?]
  (compare t0 (utr/meta-type>inner-type t1)))

;; ----- UniversalSet ----- ;;

(def- compare|universal+universal fn=)
(def- compare|universal+empty     fn>)

(defns- compare|universal+not [t0 type?, t1 not-type? > comparison?]
  (let [t1|inner (utr/not-type>inner-type t1)]
    (ifs (= t1|inner universal-set) >ident
         (= t1|inner empty-set)     =ident
         (compare t0 t1|inner))))

(def- compare|universal+or        fn>)
(def- compare|universal+and       fn>)
(def- compare|universal+expr      compare|todo)
(def- compare|universal+protocol  fn>)
(def- compare|universal+class     fn>)
(def- compare|universal+fn        fn>)
(def- compare|universal+unordered fn>)
(def- compare|universal+ordered   fn>)
(def- compare|universal+value     fn>)
(def- compare|universal+meta      compare|non-meta+meta)

;; ----- EmptySet ----- ;;

(def- compare|empty+not       fn<>)
(def- compare|empty+or        fn<>)
(def- compare|empty+and       fn<>)
(def- compare|empty+expr      compare|todo)
(def- compare|empty+protocol  fn<>)
(def- compare|empty+class     fn<>)
(def- compare|empty+fn        fn<>)
(def- compare|empty+unordered fn<>)
(def- compare|empty+ordered   fn<>)
(def- compare|empty+value     fn<>)
(def- compare|empty+meta      compare|non-meta+meta)

;; ----- NotType ----- ;;

(defns- compare|not+atomic [t0 not-type?, t1 type? > comparison?]
  (let [t0|inner (utr/not-type>inner-type t0)]
    (if (= t0|inner empty-set)
        >ident
        (case (int (compare t0|inner t1))
          ( 1 0) <>ident
          (-1 2) ><ident
          3      >ident))))

(defns- compare|not+not [t0 not-type?, t1 not-type? > comparison?]
  (let [c (int (compare (utr/not-type>inner-type t0) (utr/not-type>inner-type t1)))]
    (case-val c
      =ident  =ident
      <ident  >ident
      >ident  <ident
      ><ident ><ident
      <>ident ><ident)))

(def- compare|not+or  compare|atomic+or)
(def- compare|not+and compare|atomic+and)

(defns- compare|not+protocol [t0 not-type?, t1 protocol-type? > comparison?]
  (let [t0|inner (utr/not-type>inner-type t0)]
    (if (= t0|inner empty-set) >ident <>ident)))

(defns- compare|not+class [t0 not-type?, t1 class-type? > comparison?]
  (compare|not+atomic t0 t1))

(defns- compare|not+unordered [t0 not-type?, t1 class-type? > comparison?]
  (compare|not+atomic t0 t1))

(defns- compare|not+ordered [t0 not-type?, t1 class-type? > comparison?]
  (compare|not+atomic t0 t1))

(defns- compare|not+value [t0 not-type?, t1 value-type? > comparison?]
  (let [t0|inner (utr/not-type>inner-type t0)]
    (if (= t0|inner empty-set)
        >ident
        ;; nothing is ever < ValueType (and therefore never ><)
        (case (int (compare t0|inner t1))
          (1 0) <>ident
          3     >ident))))

(def- compare|not+meta compare|non-meta+meta)

;; ----- OrType ----- ;;

;; TODO performance can be improved here by doing fewer comparisons
;; Possibly look at `quantum.untyped.core.type.defnt/compare-args-types` for reference?
;; Expected to handle possibly non-distinct types within `ts0` and `ts1`
;; TODO follow the example of `compare|or+and`
(defns- compare|or+or-like
  [ts0 _, ts1 _, <ts0 fn?, <ts1 fn?, <>ts1 fn? > comparison?]
  (let [l (->> ts0 (seq-and <ts1))
        r (->> ts1 (seq-and <ts0))]
    (if l
        (if r =ident <ident)
        (if r
            >ident
            (if (->> ts0 (seq-and <>ts1))
                <>ident
                ><ident)))))

;; TODO follow the example of `compare|or+and`
(defns- compare|or+or [^OrType t0 or-type?, ^OrType t1 or-type? > comparison?]
  (compare|or+or-like (.-args t0) (.-args t1) (fn1 < t0) (fn1 < t1) (fn1 <> t1)))

(defns- compare|or+and [^OrType t0 or-type?, ^AndType t1 and-type? > comparison?]
  (let [t0+t1 (->> t0 .-args (uc/map+ #(compare % t1)) (educe ubit/conj ubit/empty))
        t1+t0 (->> t1 .-args (uc/map+ #(compare % t0)) (educe ubit/conj ubit/empty))]
    (case-val t0+t1
      (list b< b<|>< b<|><|<> b<|<> b=|>< b=|><|<> b=|<>)
        (comparison-err! t0+t1 t1+t0)
      b> (case-val t1+t0
           b<    >ident
           b<|>< >ident
           (list b<|><|<> b<|<> b=|>< b=|><|<> b=|<> b> b>|>< b>|><|<> b>|<> b>< b><|<> b<>)
             (comparison-err! t0+t1 t1+t0))
      b>|><
        (case-val t1+t0
          b< >ident
          (list b<|>< b<|><|<> b<|<> b=|>< b=|><|<> b=|<> b> b>|>< b>|><|<> b>|<> b>< b><|<> b<>)
            (comparison-err! t0+t1 t1+t0))
      b>|><|<>
        (comparison-err! t0+t1 t1+t0)
      b>|<>
        (case-val t1+t0
          b<    (comparison-err! t0+t1 t1+t0)
          b<|>< >ident
          (list b<|><|<> b<|<> b=|>< b=|><|<> b=|<> b> b>|>< b>|><|<> b>|<> b>< b><|<> b<>)
            (comparison-err! t0+t1 t1+t0))
      b>< (case-val t1+t0
            (list b< b<|>< b<|><|<> b<|<> b=|>< b=|><|<> b=|<> b> b>|>< b>|><|<> b>|<>)
              (comparison-err! t0+t1 t1+t0)
            b>< ><ident
            (list b><|<> b<>)
              (comparison-err! t0+t1 t1+t0))
      (list b><|<> b<>)
        (comparison-err! t0+t1 t1+t0))))

(def- compare|or+class     (inverted compare|atomic+or))
(def- compare|or+unordered (inverted compare|atomic+or))
(def- compare|or+ordered   (inverted compare|atomic+or))
(def- compare|or+value     (inverted compare|value+type))
(def- compare|or+meta      compare|non-meta+meta)

;; ----- AndType ----- ;;

(defns- compare|and+and [^AndType t0 and-type?, ^AndType t1 and-type? > comparison?]
  (let [t0+t1 (->> t0 .-args (uc/map+ #(compare % t1)) (educe ubit/conj ubit/empty))
        t1+t0 (->> t1 .-args (uc/map+ #(compare % t0)) (educe ubit/conj ubit/empty))]
    (case-val t0+t1
      (list b< b<|>< b<|><|<> b<|<> b=|>< b=|><|<> b=|<>)
        (comparison-err! t0+t1 t1+t0)
      b> (case-val t1+t0
           (list b< b<|>< b<|><|<> b<|<> b=|>< b=|><|<> b=|<>) (comparison-err! t0+t1 t1+t0)
           b>                                                  =ident
           b>|><                                               >ident
           (list b>|><|<> b>|<> b>< b><|<> b<>)                (comparison-err! t0+t1 t1+t0))
      b>|><
        (case-val t1+t0
          (list b< b<|>< b<|><|<> b<|<> b=|>< b=|><|<> b=|<>) (comparison-err! t0+t1 t1+t0)
          b>                                                  <ident ; by symmetry
          b>|><                                               ><ident
          (list b>|><|<> b>|<> b>< b><|<> b<>)                (comparison-err! t0+t1 t1+t0))
      (list b>|><|<> b>|<> b>< b><|<> b<>)
        (comparison-err! t0+t1 t1+t0))))

(def- compare|and+class     (inverted compare|atomic+and))
(def- compare|and+unordered (inverted compare|atomic+and))
(def- compare|and+ordered   (inverted compare|atomic+and))
(def- compare|and+value     (inverted compare|value+type))
(def- compare|and+meta      compare|non-meta+meta)

;; ----- Expression ----- ;;

(defns- compare|expr+expr [t0 _, t1 _ > comparison?] (if (c/= t0 t1) =ident <>ident))

(def- compare|expr+value fn><) ; TODO not entirely true

(def- compare|expr+meta compare|non-meta+meta)

;; ----- ProtocolType ----- ;;
;; Protocols cannot extend protocols.
;; A protocol may be seen as `(->> p extenders (map >type) (apply t/or))`."

(declare compare|class+class*)

(defns- compare|or+or-via-class [cs0 _, cs1 _ > comparison?]
  (if (empty? cs0)
      (if (empty? cs1) =ident <>ident)
      (if (empty? cs1)
          <>ident
          (let [gen-compare (fn [compare-comparison c cs]
                              (->> cs (uc/map+ (fn [c*] (compare|class+class* c c*)))
                                      (seq-or compare-comparison)))
                <cs0  (fn [c] (gen-compare uset/comparison<= c cs0))
                <cs1  (fn [c] (gen-compare uset/comparison<= c cs1))
                <>cs1 (fn [c] (gen-compare uset/comparison<> c cs1))]
            (compare|or+or-like cs0 cs1 <cs0 <cs1 <>cs1)))))

(defns- compare|protocol+protocol
  [t0 protocol-type?, t1 protocol-type? > comparison?]
  (let [p0 (utr/protocol-type>protocol t0)
        p1 (utr/protocol-type>protocol t1)]
    (if (== p0 p1)
        =ident
        ;; TODO use clojure.logic / match
        #?(:clj  (ifs (-> p0 :impls empty?)
                        (if (-> p1 :impls empty?) =ident <>ident)
                      (-> p1 :impls empty?)
                        <>ident
                      (-> p0 :impls (contains? Object))
                        (if (-> p1 :impls (contains? Object))
                            (if (-> p0 :impls (contains? nil))
                                (if (-> p1 :impls (contains? nil)) =ident >ident)
                                (if (-> p1 :impls (contains? nil)) <ident =ident))
                            (if (-> p0 :impls (contains? nil))
                                >ident
                                (if (-> p1 :impls (contains? nil))
                                    (if (-> p1 :impls count (c/> 1)) ><ident <>ident)
                                    >ident)))
                      (-> p1 :impls (contains? Object))
                        (if (-> p0 :impls (contains? nil))
                            (if (-> p1 :impls (contains? nil))
                                <ident
                                (if (-> p0 :impls count (c/> 1)) ><ident <>ident))
                            <ident)
                      (-> p0 :impls (contains? nil))
                        (if (-> p0 :impls count (c/> 1))
                            (compare|or+or-via-class (extenders p0) (extenders p1))
                            <>ident)
                      (-> p1 :impls (contains? nil))
                        (if (-> p1 :impls count (c/> 1))
                            (compare|or+or-via-class (extenders p0) (extenders p1))
                            <>ident)
                      (compare|or+or-via-class (extenders p0) (extenders p1)))
           ;; TODO CLJS — also incorporate `default` etc.
           ;; Simplistic but we don't have safe insight into what has been extended vs. not
           :cljs <>ident))))

(defns- compare|protocol+class [t0 protocol-type?, t1 class-type? > comparison?]
  (let [p0 (utr/protocol-type>protocol t0)
        c1 (utr/class-type>class       t1)]
    #?(:clj  (if (-> p0 :on-interface (== c1))
                 =ident
                 (compare|or+or-via-class (extenders p0) [c1]))
       :cljs (TODO))))

(defns- compare|protocol+value [t0 protocol-type?, t1 value-type? > comparison?]
  (uset/invert-comparison (compare|value+type t1 t0)))

(def- compare|protocol+meta compare|non-meta+meta)

;; ----- ClassType ----- ;;

#?(:clj
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
  [^Class c0 (us/nilable class?) ^Class c1 (us/nilable class?) > comparison?]
  (ifs (== c0 c1)                              =ident
       (or (nil? c0) (nil? c1))                <>ident
       (== c0 Object)                          >ident
       (== c1 Object)                          <ident
       (== (utcore/boxed->unboxed c0) c1)      >ident
       (== c0 (utcore/boxed->unboxed c1))      <ident
       ;; we'll consider the two unrelated
       (not (utcore/array-depth-equal? c0 c1)) <>ident
       (.isAssignableFrom c0 c1)               >ident
       (.isAssignableFrom c1 c0)               <ident
       ;; multiple inheritance of interfaces
       (or (and (uclass/interface? c0)
                (not (uclass/final? c1)))
           (and (uclass/interface? c1)
                (not (uclass/final? c0))))     ><ident
       <>ident)))

(defns- compare|class+class [t0 class-type?, t1 class-type? > comparison?]
  #?(:clj  (compare|class+class* (utr/class-type>class t0) (utr/class-type>class t1))
     :cljs (TODO)))

;; This is used to make comparisons work with `UnorderedType` and `OrderedType`.
;; TODO we should not be using `seqable?` but rather `(t/input-type reduce :_ :_ :?)`. See also the
;; implementations of `UnorderedType` and `OrderedType`.
(def- seqable-except-array?
  (OrType.
    uhash/default uhash/default nil `seqable-except-array?
    [#?(:clj  (ClassType.    uhash/default uhash/default nil nil clojure.lang.ISeq))
     #?(:clj  (ClassType.    uhash/default uhash/default nil nil clojure.lang.Seqable)
        :cljs (ProtocolType. uhash/default uhash/default nil nil cljs.core/ISeqable))
     #?(:clj (ClassType. uhash/default uhash/default nil nil java.lang.Iterable))
     #_array? ; TODO handle later
     (ClassType. uhash/default uhash/default nil nil #?(:clj java.lang.String :cljs js/String))
     #?(:clj (ClassType. uhash/default uhash/default nil nil java.util.Map))]
    (atom nil)))

(defns- compare|class+finite
  [t0 class-type?, t1 _ #_(t/or unordered-type? ordered-type?) > comparison?]
  ;; TODO technically we need to have it satisfy `dc/reducible?`, not merely `c/seqable?`
  ;; — see also note in UnorderedType's implementation about this
  (case  (int (compare t0 seqable-except-array?))
     ;; `(combine-comparisons <ident >ident)`
     ;; t/< w.r.t. seqable; t/> w.r.t contents (TODO unless contents have restrictions)
    -1 ><ident
     ;; `(combine-comparisons =ident >ident)`
     ;; t/= w.r.t. seqable; t/> w.r.t contents (TODO unless contents have restrictions)
     0 <ident
     ;; `(combine-comparisons >ident >ident)`
     ;; t/> w.r.t. seqable; t/> w.r.t contents (TODO unless contents have restrictions)
     1 >ident
     ;; `(combine-comparisons ><ident ><ident)`
     ;; t/>< w.r.t. seqable; t/>< w.r.t contents (TODO unless contents have restrictions)
     2 ><ident
     ;; `(combine-comparisons <>ident <>ident)`
     ;; t/<> w.r.t. seqable; t/<> w.r.t contents
     3 <>ident))

(defns- compare|class+unordered [t0 class-type?, t1 utr/unordered-type? > comparison?]
  (compare|class+finite t0 t1))

(defns- compare|class+ordered [t0 class-type?, t1 utr/ordered-type? > comparison?]
  (compare|class+finite t0 t1))

(defns- compare|class+value [t0 class-type?, t1 value-type? > comparison?]
  (let [c (utr/class-type>class t0)
        v (utr/value-type>value t1)]
    (if (instance? c v) >ident <>ident)))

(def- compare|class+meta compare|non-meta+meta)

;; ----- FnType ----- ;;

(defns compare|in [t0 utr/fn-type?, t1 utr/fn-type? > uset/comparison?]
  (let [ct->overloads|t0 (utr/fn-type>arities t0)
        ct->overloads|t1 (utr/fn-type>arities t1)
        cts-only-in-t0   (uset/- (-> ct->overloads|t0 keys set) (-> ct->overloads|t1 keys set))
        cts-only-in-t1   (uset/- (-> ct->overloads|t1 keys set) (-> ct->overloads|t0 keys set))
        comparison|cts   (uset/compare cts-only-in-t0 cts-only-in-t1)
        cts-in-both      (->> ct->overloads|t0 keys (filter ct->overloads|t1))]
    (combine-comparisons
      comparison|cts
      (->> cts-in-both
           (map (c/fn [ct]
                  (if (zero? ct)
                      0
                      (combine-comparisons
                        (uc/lmap compare
                          (-> t0 utr/fn-type>ored-input-types (get ct))
                          (-> t1 utr/fn-type>ored-input-types (get ct)))))))
           combine-comparisons))))

(defns compare|out [t0 utr/fn-type?, t1 utr/fn-type? > uset/comparison?]
  (compare (utr/fn-type>ored-output-type t0) (utr/fn-type>ored-output-type t1)))

(defns- compare|fn+fn [t0 utr/fn-type?, t1 utr/fn-type? > comparison?]
  (combine-comparisons (compare|in t0 t1) (compare|out t0 t1)))

;; ----- UnorderedType ----- ;;

(def- compare|unordered+value (inverted compare|value+type))
(def- compare|unordered+meta  compare|non-meta+meta)

;; ----- OrderedType ----- ;;

(def- compare|ordered+value (inverted compare|value+type))
(def- compare|ordered+meta  compare|non-meta+meta)

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
  [t0 value-type?, t1 value-type? > comparison?]
  (if (c/= (utr/value-type>value t0)
           (utr/value-type>value t1))
      =ident
      <>ident))

(def- compare|value+meta compare|non-meta+meta)

;; ----- MetaType ----- ;;

(defns- compare|meta+meta [t0 utr/meta-type?, t1 utr/meta-type?]
  (compare (utr/meta-type>inner-type t0) (utr/meta-type>inner-type t1)))

;; ===== Dispatch ===== ;;

(def- compare|dispatch
  {UniversalSetType
     {UniversalSetType compare|universal+universal
      EmptySetType     compare|universal+empty
      NotType          compare|universal+not
      OrType           compare|universal+or
      AndType          compare|universal+and
      Expression       compare|universal+expr
      ProtocolType     compare|universal+protocol
      ClassType        compare|universal+class
      FnType           compare|universal+fn
      UnorderedType    compare|universal+unordered
      OrderedType      compare|universal+ordered
      ValueType        compare|universal+value
      MetaType         compare|universal+meta}
   EmptySetType
     {UniversalSetType (inverted compare|universal+empty)
      EmptySetType     fn=
      NotType          compare|empty+not
      OrType           compare|empty+or
      AndType          compare|empty+and
      Expression       compare|empty+expr
      ProtocolType     compare|empty+protocol
      ClassType        compare|empty+class
      FnType           compare|empty+fn
      UnorderedType    compare|empty+unordered
      OrderedType      compare|empty+ordered
      ValueType        compare|empty+value
      MetaType         compare|empty+meta}
   NotType
     {UniversalSetType (inverted compare|universal+not)
      EmptySetType     (inverted compare|empty+not)
      NotType          compare|not+not
      OrType           compare|not+or
      AndType          compare|not+and
      Expression       fn>< ; TODO not entirely true
      ProtocolType     compare|not+protocol
      ClassType        compare|not+class
      FnType           compare|todo
      UnorderedType    compare|not+unordered
      OrderedType      compare|not+ordered
      ValueType        compare|not+value
      MetaType         compare|not+meta}
   OrType
     {UniversalSetType (inverted compare|universal+or)
      EmptySetType     (inverted compare|empty+or)
      NotType          (inverted compare|not+or)
      OrType           compare|or+or
      AndType          compare|or+and
      Expression       fn>< ; TODO not entirely true
      ProtocolType     compare|todo
      ClassType        compare|or+class
      FnType           compare|todo
      UnorderedType    compare|or+unordered
      OrderedType      compare|or+ordered
      ValueType        compare|or+value
      MetaType         compare|or+meta}
   AndType
     {UniversalSetType (inverted compare|universal+and)
      EmptySetType     (inverted compare|empty+and)
      NotType          compare|todo
      OrType           (inverted compare|or+and)
      AndType          compare|and+and
      Expression       fn>< ; TODO not entirely true
      ProtocolType     compare|todo
      ClassType        compare|and+class
      FnType           compare|todo
      UnorderedType    compare|and+unordered
      OrderedType      compare|and+ordered
      ValueType        compare|and+value
      MetaType         compare|and+meta}
   ;; TODO review this
   Expression
     {UniversalSetType (inverted compare|universal+expr)
      EmptySetType     (inverted compare|empty+expr)
      NotType          fn>< ; TODO not entirely true
      OrType           fn>< ; TODO not entirely true
      AndType          fn>< ; TODO not entirely true
      Expression       compare|expr+expr
      ProtocolType     fn>< ; TODO not entirely true
      ClassType        fn>< ; TODO not entirely true
      FnType           compare|todo
      UnorderedType    fn>< ; TODO not entirely true
      OrderedType      fn>< ; TODO not entirely true
      ValueType        compare|expr+value
      MetaType         compare|expr+meta}
   ProtocolType
     {UniversalSetType (inverted compare|universal+protocol)
      EmptySetType     (inverted compare|empty+protocol)
      NotType          (inverted compare|not+protocol)
      OrType           compare|todo
      AndType          compare|todo
      Expression       fn>< ; TODO not entirely true
      ProtocolType     compare|protocol+protocol
      ClassType        compare|protocol+class
      FnType           compare|todo
      UnorderedType    compare|todo
      OrderedType      compare|todo
      ValueType        compare|protocol+value
      MetaType         compare|protocol+meta}
   ClassType
     {UniversalSetType (inverted compare|universal+class)
      EmptySetType     (inverted compare|empty+class)
      NotType          (inverted compare|not+class)
      OrType           (inverted compare|or+class)
      AndType          (inverted compare|and+class)
      Expression       fn>< ; TODO not entirely true
      ProtocolType     (inverted compare|protocol+class)
      ClassType        compare|class+class
      FnType           compare|todo
      UnorderedType    compare|class+unordered
      OrderedType      compare|class+ordered
      ValueType        compare|class+value
      MetaType         compare|class+meta}
   FnType
     {UniversalSetType (inverted compare|universal+fn)
      EmptySetType     (inverted compare|empty+fn)
      NotType          compare|todo
      OrType           compare|todo
      AndType          compare|todo
      Expression       compare|todo
      ProtocolType     compare|todo
      ClassType        compare|todo
      FnType           compare|fn+fn
      UnorderedType    compare|todo
      OrderedType      compare|todo
      ValueType        compare|todo
      MetaType         compare|todo}
   UnorderedType
     {UniversalSetType (inverted compare|universal+unordered)
      EmptySetType     (inverted compare|empty+unordered)
      NotType          (inverted compare|not+unordered)
      OrType           (inverted compare|or+unordered)
      AndType          (inverted compare|and+unordered)
      Expression       compare|todo
      ProtocolType     compare|todo
      ClassType        (inverted compare|class+unordered)
      FnType           compare|todo
      UnorderedType    compare|todo
      OrderedType      compare|todo
      ValueType        compare|unordered+value
      MetaType         compare|unordered+meta}
   OrderedType
     {UniversalSetType (inverted compare|universal+ordered)
      EmptySetType     (inverted compare|empty+ordered)
      NotType          (inverted compare|not+ordered)
      OrType           (inverted compare|or+ordered)
      AndType          (inverted compare|and+ordered)
      Expression       compare|todo
      ProtocolType     compare|todo
      ClassType        (inverted compare|class+ordered)
      FnType           compare|todo
      UnorderedType    compare|todo
      OrderedType      compare|todo
      ValueType        compare|ordered+value
      MetaType         compare|ordered+meta}
   ValueType
     {UniversalSetType (inverted compare|universal+value)
      EmptySetType     (inverted compare|empty+value)
      NotType          (inverted compare|not+value)
      OrType           (inverted compare|or+value)
      AndType          (inverted compare|and+value)
      Expression       (inverted compare|expr+value)
      ProtocolType     (inverted compare|protocol+value)
      ClassType        (inverted compare|class+value)
      FnType           compare|todo
      UnorderedType    (inverted compare|unordered+value)
      OrderedType      (inverted compare|ordered+value)
      ValueType        compare|value+value
      MetaType         compare|value+meta}
   MetaType
     {UniversalSetType (inverted compare|universal+meta)
      EmptySetType     (inverted compare|empty+meta)
      NotType          (inverted compare|not+meta)
      OrType           (inverted compare|or+meta)
      AndType          (inverted compare|and+meta)
      Expression       (inverted compare|expr+meta)
      ProtocolType     (inverted compare|protocol+meta)
      ClassType        (inverted compare|class+meta)
      FnType           compare|todo
      UnorderedType    (inverted compare|unordered+meta)
      OrderedType      (inverted compare|ordered+meta)
      ValueType        (inverted compare|value+meta)
      MetaType         compare|meta+meta}})

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
  [t0 type?, t1 type? > comparison?]
  (if (c/= t0 t1)
      =ident
      (let [dispatched (-> compare|dispatch (get (type t0)) (get (type t1)))]
        (if (nil? dispatched)
            (err! (str "Types not handled: " {:t0 t0 :t1 t1}) {:t0 t0 :t1 t1})
            (dispatched t0 t1)))))

(defns <
  "Computes whether the extension of type ->`t0` is a strict subset of that of ->`t1`."
  ([t1 type?] #(< % t1))
  ([t0 type?, t1 type? > boolean?] (uset/comp< compare t0 t1)))

(defns <=
  "Computes whether the extension of type ->`t0` is a (lax) subset of that of ->`t1`."
  ([t1 type?] #(<= % t1))
  ([t0 type?, t1 type? > boolean?] (uset/comp<= compare t0 t1)))

(defns =
  "Computes whether the extension of type ->`t0` is equal to that of ->`t1`."
  ([t1 type?] #(= % t1))
  ([t0 type?, t1 type? > boolean?] (uset/comp= compare t0 t1)))

(defns not=
  "Computes whether the extension of type ->`t0` is not equal to that of ->`t1`."
  ([t1 type?] #(not= % t1))
  ([t0 type?, t1 type? > boolean?] (uset/comp-not= compare t0 t1)))

(defns >=
  "Computes whether the extension of type ->`t0` is a (lax) superset of that of ->`t1`."
  ([t1 type?] #(>= % t1))
  ([t0 type?, t1 type? > boolean?] (uset/comp>= compare t0 t1)))

(defns >
  "Computes whether the extension of type ->`t0` is a strict superset of that of ->`t1`."
  ([t1 type?] #(> % t1))
  ([t0 type?, t1 type? > boolean?] (uset/comp> compare t0 t1)))

(defns ><
  "Computes whether it is the case that the intersect of the extensions of type ->`t0`
   and ->`t1` is non-empty, and neither ->`t0` nor ->`t1` share a subset/equality/superset
   relationship."
  ([t1 type?] #(>< % t1))
  ([t0 type?, t1 type? > boolean?] (uset/comp>< compare t0 t1)))

(defns <>
  "Computes whether the respective extensions of types ->`t0` and ->`t1` are disjoint."
  ([t1 type?] #(<> % t1))
  ([t0 type? t1 type? > boolean?] (uset/comp<> compare t0 t1)))

;; ===== FnType ===== ;;

(defns combine-comparisons
  "Used in `t/compare|in` and `t/compare|out`. Might be used for other things too in the future.
   Commutative in the 2-ary arity.
   A `t/and`-style combination."
  ([cs _ #_(seq-of uset/comparison?) > uset/comparison?]
    ;; TODO it's possible to `reduced` early here depending
    (if (empty? cs)
        =ident
        (reduce (fn [c' c] (combine-comparisons c' c)) (first cs) (rest cs))))
  ([c0 uset/comparison?, c1 uset/comparison? > uset/comparison?]
    (case (long c0)
      -1 (case (long c1) -1  <ident, 0  <ident, 1 ><ident, 2 ><ident, 3 <>ident)
       0 (case (long c1) -1  <ident, 0  =ident, 1  >ident, 2 ><ident, 3 <>ident)
       1 (case (long c1) -1 ><ident, 0  >ident, 1  >ident, 2 ><ident, 3 <>ident)
       2 (case (long c1) -1 ><ident, 0 ><ident, 1 ><ident, 2 ><ident, 3 <>ident)
       3 (case (long c1) -1 <>ident, 0 <>ident, 1 <>ident, 2 <>ident, 3 <>ident))))

(defns compare-inputs
  [arg-types0 _ #_(s/vec-of t/type?), arg-types1 _ #_(s/vec-of t/type?) > uset/comparison?]
  (let [ct-comparison (c/compare (count arg-types0) (count arg-types1))]
    (if (zero? ct-comparison)
        ;; TODO can use educers here
        (combine-comparisons (map compare arg-types0 arg-types1))
        <ident)))
