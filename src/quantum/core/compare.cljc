(ns quantum.core.compare
  (:refer-clojure :exclude
    ;; TODO enable
  #_[= == compare]
    ;; TODO clean up
    [= not= < > <= >= max min max-key min-key neg? pos? zero? - -' + inc compare
     reduce, transduce, first])
  (:require
    [clojure.core                    :as core]
    [goog.array                      :as garray]
    [quantum.core.log                :as log
      :refer [prl!]]
    [quantum.core.collections.core   :as ccoll
      :refer [conj?! ?persistent! ?transient!, first, join]]
    [quantum.core.compare.core       :as ccomp]
    [quantum.core.error :as err
      :refer [TODO]]
    [quantum.core.fn                 :as fn
      :refer [fn&2 rfn aritoid]]
    [quantum.core.macros
      :refer [defnt #?@(:clj [defnt' variadic-proxy variadic-predicate-proxy])]]
    [quantum.core.numeric.convert
      :refer [->num ->num&]]
    [quantum.core.numeric.operators  :as op
      :refer [- -' + abs inc div:natural]]
    [quantum.core.numeric.predicates :as pred
      :refer [neg? pos? zero?]]
    [quantum.core.data.numeric       :as dnum]
    [quantum.core.data.time          :as dtime]
    [quantum.core.reducers           :as red
      :refer [reduce, transduce]]
    [quantum.core.vars
      :refer [defalias defaliases]]
    [quantum.untyped.core.compare    :as ucomp])
#?(:cljs (:require-macros
    [quantum.core.compare            :as self
      :refer [< > <= >=]]))
#?(:clj
  (:import
    clojure.lang.BigInt quantum.core.Numeric)))

;; TODO TYPED incorporate this commented code

; (defnt ^boolean identical?
;   [^Object k1, ^Object k2]
;   (clojure.lang.RT/identical k1 k2))

; static public boolean pcequiv(Object k1, Object k2){
;   if(k1 instanceof IPersistentCollection)
;     return ((IPersistentCollection)k1).equiv(k2);
;   return ((IPersistentCollection)k2).equiv(k1);
; }

; static public boolean equals(Object k1, Object k2){
;   if(k1 == k2)
;     return true;
;   return k1 != null && k1.equals(k2);
; }

; static public boolean equiv(Object k1, Object k2){
;   if(k1 == k2)
;     return true;
;   if(k1 != null)
;     {
;     if(k1 instanceof Number && k2 instanceof Number)
;       return Numbers.equal((Number)k1, (Number)k2);
;     else if(k1 instanceof IPersistentCollection || k2 instanceof IPersistentCollection)
;       return pcequiv(k1,k2);
;     return k1.equals(k2);
;     }
;   return false;
; }

; equivNull   : boolean equiv(Object k1, Object k2) return k2 == null
; equivEquals : boolean equiv(Object k1, Object k2) return k1.equals(k2)
; equivNumber : boolean equiv(Object k1, Object k2)
;             if(k2 instanceof Number)
;                 return Numbers.equal((Number) k1, (Number) k2);
;             return false

; equivColl : boolean equiv(Object k1, Object k2)
;             if(k1 instanceof IPersistentCollection || k2 instanceof IPersistentCollection)
;                 return pcequiv(k1, k2);
;             return k1.equals(k2);

; ; equivPred:
; ;     nil             : equivNull
; ;     Number          : equivNumber
; ;     String, Symbol  : equivEquals
; ;     Collection, Map : equivColl
; ;     :else           : equivEquals

; (defnt equiv ^boolean
;   ([^Object                a #{long double boolean} b] (clojure.lang.RT/equiv a b))
;   ([#{long double boolean} a ^Object                b] (clojure.lang.RT/equiv a b))
;   ([#{long double boolean} a #{long double boolean} b] (clojure.lang.RT/equiv a b))
;   ([^char                  a ^char                  b] (clojure.lang.RT/equiv a b))

;   )

;; TODO TYPED; also incorporate `core/fn->comparator`
(defn fn->comparator [f]
  #?(:clj  (cast java.util.Comparator f)
     :cljs (core/fn->comparator f)))

#?(:clj
(defnt' ^int compare-1d-arrays-lexicographically ; TODO reflection
  "Arrays are not `Comparable`, so we need a custom
   comparator which we can pass to `sort`."
  {:from       "clojure.tools.nrepl.bencode"
   :adapted-by "Alex Gunnarson"}
  ([^array-1d? a ^array-1d? b]
    (let [alen (alength a)
          blen (alength b)
          len  (core/min alen blen)]
      (loop [i 0]
        (if (== i len) ; TODO = ?
            (- alen blen)
            (let [x (p/>long (- (->num (aget a i)) (->num (aget b i))))] ; TODO remove protocol
              (if (zero? x)
                  (recur (core/inc i))
                  x))))))))

(def compare ccomp/compare)

;; TODO TYPED define variadic arity
(t/extend-defn! compare
#?(:cljs ([a js/Date      , b js/Date]       (compare (dtime/date>value a) (dtime/date>value b))))
         ([a arr/array-1d?, b arr/array-1d?] (compare-1d-arrays-lexicographically a b)))

;; TODO TYPED define variadic arity
(t/extend-defn! =
#?(:cljs ([a js/Date, b js/Date] (== (dtime/date>value o) (dtime/date>value other)))))

(defaliases ccomp
  min-key first-min-key second-min-key
  max-key first-max-key second-max-key
  #?@(:clj [=   =&   not=     not=&
            <   <&   comp<    comp<&
            <=  <=&  comp<=   comp<=&
            >   >&   comp>    comp>&
            >=  >=&  comp>=   comp>=&
            min min& comp-min comp-min&
            max max& comp-max comp-max&
            min-byte   max-byte
            min-char   max-char
            min-short  max-short
            min-int    max-int
            min-long   max-long
            min-float  max-float
            min-double max-double
            #_first-min #_second-min
            #_first-max #_second-max]))

(defn gen-comp-keys-into:rf
  ; TODO use `reduce-multi` or `multiplex`
  ([initf compf kf]
    (let [rf (rfn [[ret best] x]
               (if (identical? best red/sentinel)
                   [(conj?! ret x) x]
                   (let [vret (kf best) vx (kf x)]
                     (cond (=     vret vx) [(conj?! ret x)     x]
                           (compf vret vx) [ret                best]
                           :else           [(conj?! (initf) x) x]))))]
      (aritoid (fn [] [(?transient! (initf)) red/sentinel])
               (fn [[ret _]] (?persistent! ret))
               rf
               rf))))

(defn comp-keys-into
  "Like `max-key`, but returns a collection of all equally 'max' values (even
   when no tie is present), generated by `initf`."
  ([initf compf kf] nil) ; TODO really, the min of whatever it is; maybe gen via `(kf)` ?
  ([initf compf kf x] x)
  ([initf compf kf x y] ; TODO merge this logic with `gen-comp-keys-into:rf`
    (let [vx (kf x) vy (kf y)]
      (cond (=     vx vy) (-> (initf) (conj?! x) (conj?! y)) ; TODO (conj?! (initf) x y)
            (compf vx vy) (conj?! (initf) x)
            :else         (conj?! (initf) y))))
  ([initf compf kf x y & more]
    (transduce (gen-comp-keys-into:rf initf compf kf) (conj more y x))))

(defn max-keys
  "Like `max-key`, but returns a collection of all equally 'max' values (even
   when no tie is present)."
  ([kf] nil) ; TODO really, the min of whatever it is; maybe gen via `(kf)` ?
  ([kf x] x)
  ([kf x y       ]       (comp-keys-into vector core/> kf x y))       ; TODO use this/>
  ([kf x y & more] (apply comp-keys-into vector core/> kf x y more))) ; TODO use this/>

(defn reduce-first-max-key  [kf xs] (red/reduce-sentinel (fn&2 first-max-key  kf) xs))
(defn reduce-second-max-key [kf xs] (red/reduce-sentinel (fn&2 second-max-key kf) xs))
(defn reduce-max-key        [kf xs] (red/reduce-sentinel (fn&2 max-key        kf) xs))

(defn reduce-first-min-key  [kf xs] (red/reduce-sentinel (fn&2 first-min-key  kf) xs))
(defn reduce-second-min-key [kf xs] (red/reduce-sentinel (fn&2 second-min-key kf) xs))
(defn reduce-min-key        [kf xs] (red/reduce-sentinel (fn&2 min-key        kf) xs))

(defn reduce-first-min      [   xs] (red/reduce-sentinel ccomp/first-min-temp           xs))
(defn reduce-second-min     [   xs] (red/reduce-sentinel ccomp/second-min-temp          xs))
(defn reduce-min            [   xs] (red/reduce-sentinel ccomp/min-temp                 xs))

(defn reduce-first-max      [   xs] (red/reduce-sentinel ccomp/first-max-temp           xs))
(defn reduce-second-max     [   xs] (red/reduce-sentinel ccomp/second-max-temp          xs))
(defn reduce-max            [   xs] (red/reduce-sentinel ccomp/max-temp                 xs))

(defn reduce-maxes          [               xs] (transduce (gen-comp-keys-into:rf vector core/> identity) xs)) ; TODO use this/>
(defn reduce-max-keys       [            kf xs] (transduce (gen-comp-keys-into:rf vector core/> kf      ) xs)) ; TODO use this/>
(defn reduce-max-keys-into  [initf       kf xs] (transduce (gen-comp-keys-into:rf initf  core/> kf      ) xs)) ; TODO use this/>
(defn reduce-comp-keys      [      compf kf xs] (transduce (gen-comp-keys-into:rf vector compf  kf      ) xs)) ; TODO use this/>
(defn reduce-comp-keys-into [initf compf kf xs] (transduce (gen-comp-keys-into:rf initf  compf  kf      ) xs)) ; TODO use this/>


(defaliases ucomp greatest least rcompare)

(defn unsorted-by
  "Returns which elements are unsorted, as by `kf` and `comparef`."
  {:example '{(unsorted-by identity core/< [0 1 2 3 6 3 7])
              [[5 3]]}}
  ([kf xs] (unsorted-by kf core/compare xs))
  ([kf comparef xs]
    (let [xs'      (transient [])
          comparef (fn->comparator comparef)]
      (red/reducei-sentinel
        (fn [a b i]
          (when-not (neg? (#?@(:clj  [.compare ^java.util.Comparator comparef]
                               :cljs [comparef])
                     (kf a) (kf b)))
            (conj! xs' [i b]))
          b) xs)
      (persistent! xs'))))

(defn unsorted
  "Returns which elements are unsorted, as by `comparef`."
  {:example '{(unsorted core/< [0 1 2 3 6 3 7])
              [[5 3]]}}
  ([xs] (unsorted-by identity xs))
  ([comparef xs] (unsorted-by identity comparef xs)))

#?(:clj
(defmacro min-or [a b else]
 `(let [a# ~a b# ~b]
    (cond (< a# b#) a#
          (< b# a#) b#
          :else ~else))))

#?(:clj
(defmacro max-or [a b else]
 `(let [a# ~a b# ~b]
    (cond (> a# b#) a#
          (> b# a#) b#
          :else ~else))))

#_(defn extreme-comparator [comparator-n]
  (get {> num/greatest
        < num/least   }
    comparator-n))

; ===== APPROXIMATION ===== ;

(defn approx=
  "Return true if the absolute value of the difference between x and y
   is less than eps."
  {:todo #{"`core/<` -> `<` "}}
  [x y eps]
  (core/< (abs (- x y)) eps))

(defn within-tolerance?
  {:todo #{"`core/<` -> `<` "}}
  [n total tolerance]
  (and (core/>= n (- total tolerance))
       (core/<= n (+ total tolerance))))

(defnt' within-percent-tolerance? [^number? actual ^number? expected percent-tolerance]
  (core/< (div:natural (double (abs (core/- expected actual))) expected) ; TODO remove `double` cast!!!
          percent-tolerance))
