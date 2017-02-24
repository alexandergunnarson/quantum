(ns quantum.core.compare
  (:refer-clojure :exclude
    [= not= < > <= >= max min max-key min-key neg? pos? zero? - + inc compare])
  (:require
    [clojure.core       :as core]
    [quantum.core.error :as err
      :refer [TODO]]
    [quantum.core.macros
      :refer [defnt #?@(:clj [defnt' variadic-proxy variadic-predicate-proxy])]]
    [quantum.core.vars
      :refer [defalias]]
    [quantum.core.numeric.operators  :as op
      :refer [- + abs inc]]
    [quantum.core.numeric.predicates :as pred
      :refer [neg? pos? zero?]]
    [quantum.core.numeric.convert
      :refer [->num ->num&]]
    [quantum.core.convert.primitive :as pconv
      :refer [->boxed ->boolean ->long]]
    [quantum.core.numeric.types      :as ntypes])
  (:require-macros
    [quantum.core.compare
      :refer [< > <= >=]])
  #?(:clj (:import clojure.lang.BigInt quantum.core.Numeric)))

; Some of the ideas here adapted from gfredericks/compare
; TODO include diffing
; TODO use -compare in CLJS
; TODO do `defnt` `compare` for different types
; TODO = vs. == vs. RT/equiv vs. etc.
; TODO bring in from clojure.lang.RT
; TODO comp< vs. <; comp< should include arrays
; `=`  <- `==`, `=`: permissive
; `='` <- `=`: strict like `core/=` with numbers
; `==` <- `identical?`
; `hash=`

; ===== `compare` ===== ;

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
            (let [x (pconv/->long-protocol (- (->num (aget a i)) (->num (aget b i))))] ; TODO remove protocol
              (if (zero? x)
                  (recur (core/inc i))
                  x))))))))

#?(:clj  (defnt' ^int compare
           {:todo #{"Handle nil values"}}
           ([^Comparable a ^Comparable b] (int (.compareTo a b)))
           ([^Comparable a ^prim?      b] (int (.compareTo a b)))
           ([^prim?      a ^Comparable b] (int (.compareTo (->boxed a) b)))
           ([^array-1d?  a ^array-1d?  b] (compare-1d-arrays-lexicographically a b)))
   :cljs (defalias compare core/compare))

; ===== `=`, `not=` ===== ;

#?(:clj  (defnt' ^boolean =-bin
           ([#{byte char short int long float double} x
             #{byte char short int long float double} y] (Numeric/eq x y))
           ([^boolean x ^boolean y] (Numeric/eq x y))
           ([^boolean x #{byte char short int long float double} y] (->boolean false))
           ([#{byte char short int long float double} x ^boolean y] (->boolean false))
           ([         x          y] (.equals ^Object x y))
           ([         x ^prim?   y] (.equals ^Object x y))
           ([^prim?   x          y] (.equals ^Object y x)))
   :cljs (defn =-bin
           ([x] true)
           ([x y] (TODO "fix") (core/zero? (ntypes/-compare x y)))))

#?(:clj (variadic-predicate-proxy =  =-bin ))
#?(:clj (variadic-predicate-proxy =& =-bin&))

#?(:clj  (defnt' ^boolean not=-bin
           ([#{#_Object prim?} x #{#_Object prim?} y] (Numeric/not (=-bin& x y)))) ; TODO make this one operation; TODO can only work with inline
   :cljs (defn not=-bin
           ([x] false)
           ([x y] (TODO "fix") (not (core/zero? (ntypes/-compare x y))))))

#?(:clj (variadic-predicate-proxy not=  not=-bin ))
#?(:clj (variadic-predicate-proxy not=& not=-bin&))

; ===== `<` ===== ;

#?(:clj  (defnt' ^boolean <-bin
           ([#{byte char short int long float double} x] (->boolean true))
           ([#{byte char short int long float double} x
             #{byte char short int long float double} y] (Numeric/lt x y))
           ; TODO numbers, but not nil
           )
   :cljs (defn <-bin ([x] true) ([x y] (core/< x y))))

#?(:clj (variadic-predicate-proxy < <-bin))
#?(:clj (variadic-predicate-proxy <& <-bin&))

; ----- `comp<` ----- ;

#?(:clj  (defnt' ^boolean comp<-bin
           ([^comparable? x] (->boolean true))
           ([#{byte char short int long float double} x
             #{byte char short int long float double} y] (< x y))
           ([^boolean    x ^boolean    y] (< (->num& x) (->num& y)))
           ([^Comparable x ^Comparable y] (< (compare x y) 0))
           ([^Comparable x ^prim?      y] (< (compare x y) 0))
           ([^prim?      x ^Comparable y] (< (compare x y) 0))
           ; TODO numbers and nil
           )
   :cljs (defn comp<-bin ([x] true) ([x y] (< (compare x y) 0))))

#?(:clj (variadic-predicate-proxy
          ^{:doc "Returns true if args are in monotonically increasing order
                  according to `compare`, otherwise false."}
          comp< comp<-bin))
#?(:clj (variadic-predicate-proxy comp<& comp<-bin&))

; ===== `<=` ===== ;

#?(:clj  (defnt' ^boolean <=-bin
           ([#{byte char short int long float double} x] (->boolean true))
           ([#{byte char short int long float double} x
             #{byte char short int long float double} y] (Numeric/lte x y))
           ; TODO numbers, but not nil
           )
   :cljs (defn <=-bin ([x] true) ([x y] (core/<= x y))))

#?(:clj (variadic-predicate-proxy <= <=-bin))
#?(:clj (variadic-predicate-proxy <=& <=-bin&))

; ----- `comp<=` ----- ;

#?(:clj  (defnt' ^boolean comp<=-bin
           ([^comparable? x] (->boolean true))
           ([#{byte char short int long float double} x
             #{byte char short int long float double} y] (<= x y))
           ([^boolean    x ^boolean    y] (<= (->num& x) (->num& y)))
           ([^Comparable x ^Comparable y] (<= (compare x y) 0))
           ([^Comparable x ^prim?      y] (<= (compare x y) 0))
           ([^prim?      x ^Comparable y] (<= (compare x y) 0))
           ; TODO numbers and nil
           )
   :cljs (defn comp<=-bin ([x] true) ([x y] (<= (compare x y) 0)))) ; TODO rest

#?(:clj (variadic-predicate-proxy
          ^{:doc "Returns true if args are in monotonically non-decreasing order
                  according to `compare`, otherwise false."}
          comp<= comp<=-bin))
#?(:clj (variadic-predicate-proxy comp<=& comp<=-bin&))

; ===== `>` ===== ;

#?(:clj  (defnt' ^boolean >-bin
           ([#{byte char short int long float double} x] (->boolean true))
           ([#{byte char short int long float double} x
             #{byte char short int long float double} y] (Numeric/gt x y))
           ; TODO numbers, but not nil
           )
   :cljs (defn >-bin ([x] true) ([x y] (core/> x y))))

#?(:clj (variadic-predicate-proxy > >-bin))
#?(:clj (variadic-predicate-proxy >& >-bin&))

; ----- `comp>` ----- ;

#?(:clj  (defnt' ^boolean comp>-bin
           ([^comparable? x] (->boolean true))
           ([#{byte char short int long float double} x
             #{byte char short int long float double} y] (> x y))
           ([^boolean    x ^boolean    y] (> (->num& x) (->num& y)))
           ([^Comparable x ^Comparable y] (> (compare x y) 0))
           ([^Comparable x ^prim?      y] (> (compare x y) 0))
           ([^prim?      x ^Comparable y] (> (compare x y) 0))
           ; TODO numbers and nil
           )
   :cljs (defn comp>-bin ([x] true) ([x y] (> (compare x y) 0)))) ; TODO rest

#?(:clj (variadic-predicate-proxy
          ^{:doc "Returns true if args are in monotonically decreasing order
                  according to `compare`, otherwise false."}
          comp> comp>-bin))
#?(:clj (variadic-predicate-proxy comp>& comp>-bin&))

; ===== `>=` ===== ;

#?(:clj  (defnt' ^boolean >=-bin
           ([#{byte char short int long float double} x] (->boolean true))
           ([#{byte char short int long float double} x
             #{byte char short int long float double} y] (Numeric/gte x y))
           ; TODO numbers, but not nil
           )
   :cljs (defn >=-bin ([x] true) ([x y] (core/>= x y))))

#?(:clj (variadic-predicate-proxy >= >=-bin))
#?(:clj (variadic-predicate-proxy >=& >=-bin&))

; ----- `comp>=` ----- ;

#?(:clj  (defnt' ^boolean comp>=-bin
           ([^comparable? x] (->boolean true))
           ([#{byte char short int long float double} x
             #{byte char short int long float double} y] (>= x y))
           ([^boolean    x ^boolean    y] (>= (->num& x) (->num& y)))
           ([^Comparable x ^Comparable y] (>= (compare x y) 0))
           ([^Comparable x ^prim?      y] (>= (compare x y) 0))
           ([^prim?      x ^Comparable y] (>= (compare x y) 0))
           ; TODO numbers and nil
           )
   :cljs (defn >=-bin ([x] true) ([x y] (core/>= (compare x y) 0)))) ; TODO defnt

#?(:clj (variadic-predicate-proxy
          ^{:doc "Returns true if args are in monotonically non-increasing order
                  according to `compare`, otherwise false."}
          comp>= comp>=-bin))
#?(:clj (variadic-predicate-proxy comp>=& comp>=-bin&))

; ===== `min` ===== ;

#?(:clj  (defnt' min-bin
           ([] Double/NEGATIVE_INFINITY) ; the thing less than which there is nothing
           ([#{byte char short int long float double} x] x)
           ([#{byte char short int long float double} x
             #{byte char short int long float double} y] (Numeric/min x y))
           ; TODO numbers, but not nil
           )
   :cljs (defn min-bin ([x] x) ([x y] (if (< x y) x y)))) ; TODO defnt

#?(:clj (variadic-proxy min  min-bin))
#?(:clj (variadic-proxy min& min-bin&))

; ----- `comp-min` ----- ;

#?(:clj  (defnt' comp-min-bin
           ([] Double/NEGATIVE_INFINITY) ; the thing less than which there is nothing
           ([^comparable? x] x)
           ([#{byte char short int long float double} x
             #{byte char short int long float double} y] (min x y))
           ([#{boolean Comparable} x #{boolean Comparable} y] (if (comp< x y) x y))
           ; TODO numbers and nil
           )
   :cljs (defn comp-min-bin ([x] x) ([x y] (if (comp< x y) x y)))) ; TODO defnt

#?(:clj (variadic-proxy
          ^{:doc "Returns the least of the arguments according to
                  `compare`, preferring later values."}
          comp-min comp-min-bin))
#?(:clj (variadic-proxy comp-min& comp-min-bin&))

; ===== `max` ===== ;

#?(:clj  (defnt' max-bin
           ([] Double/POSITIVE_INFINITY) ; the thing greater than which there is nothing
           ([#{byte char short int long float double} x] x)
           ([#{byte char short int long float double} x
             #{byte char short int long float double} y] (Numeric/max x y))
           ; TODO numbers, but not nil
           )
   :cljs (defn max-bin ([x] x) ([x y] (if (> x y) x y)))) ; TODO defnt

#?(:clj (variadic-proxy max  max-bin))
#?(:clj (variadic-proxy max& max-bin&))

; ----- `comp-max` ----- ;

#?(:clj  (defnt' comp-max-bin
           ([] Double/POSITIVE_INFINITY) ; the thing greater than which there is nothing
           ([^comparable? x] x)
           ([#{byte char short int long float double} x
             #{byte char short int long float double} y] (max x y))
           ([#{boolean Comparable} x #{boolean Comparable} y] (if (comp> x y) x y))
           ; TODO numbers and nil
           )
   :cljs (defn comp-max-bin ([x] x) ([x y] (if (comp> x y) x y)))) ; TODO defnt

#?(:clj (variadic-proxy
          ^{:doc "Returns the greatest of the arguments according to
                  `compare`, preferring later values."}
          comp-max comp-max-bin))
#?(:clj (variadic-proxy comp-max& comp-max-bin&))

; ----- `comp-max` ----- ;

; ===== PRIMITIVE `max`|`min` ===== ;

#?(:clj (defnt' ^byte   min-byte   ([] Byte/MIN_VALUE          ) ([^byte   a] a) ([^byte   a ^byte   b] (min a b))))
#?(:clj (defnt' ^byte   max-byte   ([] Byte/MAX_VALUE          ) ([^byte   a] a) ([^byte   a ^byte   b] (max a b))))
#?(:clj (defnt' ^char   min-char   ([] Character/MIN_VALUE     ) ([^char   a] a) ([^char   a ^char   b] (min a b))))
#?(:clj (defnt' ^char   max-char   ([] Character/MAX_VALUE     ) ([^char   a] a) ([^char   a ^char   b] (max a b))))
#?(:clj (defnt' ^short  min-short  ([] Short/MIN_VALUE         ) ([^short  a] a) ([^short  a ^short  b] (min a b))))
#?(:clj (defnt' ^short  max-short  ([] Short/MAX_VALUE         ) ([^short  a] a) ([^short  a ^short  b] (max a b))))
#?(:clj (defnt' ^int    min-int    ([] Integer/MIN_VALUE       ) ([^int    a] a) ([^int    a ^int    b] (min a b))))
#?(:clj (defnt' ^int    max-int    ([] Integer/MAX_VALUE       ) ([^int    a] a) ([^int    a ^int    b] (max a b))))
#?(:clj (defnt' ^long   min-long   ([] Long/MIN_VALUE          ) ([^long   a] a) ([^long   a ^long   b] (min a b))))
#?(:clj (defnt' ^long   max-long   ([] Long/MAX_VALUE          ) ([^long   a] a) ([^long   a ^long   b] (max a b))))
#?(:clj (defnt' ^float  min-float  ([] Float/NEGATIVE_INFINITY ) ([^float  a] a) ([^float  a ^float  b] (min a b))))
#?(:clj (defnt' ^float  max-float  ([] Float/POSITIVE_INFINITY ) ([^float  a] a) ([^float  a ^float  b] (max a b))))
#?(:clj (defnt' ^double min-double ([] Double/NEGATIVE_INFINITY) ([^double a] a) ([^double a ^double b] (min a b))))
#?(:clj (defnt' ^double max-double ([] Double/POSITIVE_INFINITY) ([^double a] a) ([^double a ^double b] (max a b))))

; ===== extreme-`key` ===== ;

(defn min-key
  ([k] nil)
  ([k x] x)
  ([k x y] (if (< (k x) (k y)) x y))
  ([k x y & more]
   (reduce #(min-key k %1 %2) (min-key k x y) more)))

(defn comp-min-key
  "Returns the x for which (k x) is least, according to
   `compare`."
  {:from "gfredericks/compare"}
  ([k] nil)
  ([k x] x)
  ([k x y] (if (comp< (k x) (k y)) x y))
  ([k x y & more]
   (reduce #(comp-min-key k %1 %2) (comp-min-key k x y) more)))

(defn max-key
  ([k] nil)
  ([k x] x)
  ([k x y] (if (> (k x) (k y)) x y))
  ([k x y & more]
   (reduce #(max-key k %1 %2) (max-key k x y) more)))

(defn comp-max-key
  "Returns the x for which (k x) is greatest, according to
   `compare`."
  {:from "gfredericks/compare"}
  ([k] nil)
  ([k x] x)
  ([k x y] (if (comp> (k x) (k y)) x y))
  ([k x y & more]
   (reduce #(comp-max-key k %1 %2) (comp-max-key k x y) more)))



(defn rcompare
  "Reverse comparator."
  {:attribution "taoensso.encore, possibly via weavejester.medley"}
  [x y] (compare y x))

(defn greatest
  "Returns the 'greatest' element in coll in O(n) time."
  {:attribution "taoensso.encore, possibly via weavejester.medley"}
  [coll & [?comparator]]
  (let [comparator (or ?comparator rcompare)]
    (reduce
      (fn ([] nil) ([a b] (if (pos? (comparator a b)) b a)))
      coll)))

(defn least
  "Returns the 'least' element in coll in O(n) time."
  {:attribution "taoensso.encore, possibly via weavejester.medley"}
  [coll & [?comparator]]
  (let [comparator (or ?comparator rcompare)]
    (reduce
      (fn ([] nil) ([a b] (if (neg? (comparator a b)) b a)))
      coll)))

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


