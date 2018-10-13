(ns quantum.core.compare.core
  "Defines fundamental comparison operators but does not necessarily provide definitions for all
   type overloads.

   A complete (w.r.t. the `quantum.core.data.*` namespaces) set of definitions for type overloads is
   found in `quantum.core.compare`."
        (:refer-clojure :exclude
          [< <= = not= == not== > >= compare]
          ;; TODO TYPED remove
        #_[= not= < > <= >= max min max-key min-key neg? pos? zero? - -' + inc compare])
        (:require
          ;; TODO TYPED excise
          [clojure.core       :as core]
          ;; TODO TYPED excise
        #_[quantum.core.numeric.operators  :as op
            :refer [- -' + abs inc div:natural]]
          ;; TODO TYPED excise
        #_[quantum.core.numeric.predicates :as pred
            :refer [neg? pos? zero?]]
          ;; TODO TYPED excise
        #_[quantum.core.numeric.convert
            :refer [->num ->num&]]
          ;; TODO TYPED excise
        #_[quantum.core.data.numeric       :as dn]
          [quantum.core.type               :as t]
          ;; TODO TYPED excise
          [quantum.untyped.core.logic
            :refer [ifs]]
          [quantum.untyped.core.type       :as ut])
#?(:clj (:import
          [quantum.core Numeric])))

;; Some of the ideas here adapted from gfredericks/compare
;; TODO include diffing
;; TODO use -compare in CLJS
;; TODO do `defnt` `compare` for different types
;; TODO = vs. == vs. RT/equiv vs. etc.
;; TODO bring in from clojure.lang.RT
;; TODO comp< vs. <; comp< should include arrays
;; `=`  <- `==`, `=`: permissive
;; `='` <- `=`: strict like `core/=` with numbers
;; `==` <- `identical?`
;; TODO `hash=`
;; TODO .equals vs. .equiv vs. all the others?

; ===== `==`, `=`, `not=` ===== ;

;; TODO add variadic arity
(t/defn ^:inline ==
  "Tests identity-equality."
  {:incorporated '{clojure.lang.Util/identical "9/27/2018"
                   clojure.core/identical?     "9/27/2018"
                   cljs.core/identical?        "9/27/2018"}}
  > ut/boolean?
         ([x t/any?] true) ; everything is self-identical
#?(:clj  ([a t/ref?, b t/ref?] (clojure.lang.Util/identical a b))
   :cljs ([a t/any?, b t/any?] (cljs.core/identical? a b))))

;; TODO add variadic arity
(t/defn ^:inline not==
  "Tests identity-inequality."
  > ut/boolean?
         ([x t/any?] false) ; nothing is self-non-identical
#?(:clj  ([a t/ref?, b t/ref?] (Numeric/nonIdentical a b))
   :cljs ([a t/any?, b t/any?] (js* "(~{} !== ~{})" a b))))

;; TODO add variadic arity
(t/defn ^:inline =
  "Tests value-equality."
  {:incorporated '{clojure.lang.Util/equiv "9/27/2018"
                   clojure.core/=          "9/27/2018"
                   cljs.core/=             "9/27/2018"}}
  > ut/boolean?
  ([x t/any?] true)) ; everything is self-equal

;; TODO add variadic arity
(t/defn ^:inline not=
  "Tests value-inequality."
  {:incorporated '{clojure.core/not= "9/27/2018"
                   cljs.core/not=    "9/27/2018"}}
  > ut/boolean?
  ([x t/any?] false)) ; nothing is self-unequal

; ===== `<` ===== ;

;; TODO add variadic arity
(t/defn ^:inline <
  "Numeric less-than comparison."
  > ut/boolean?)

; ===== `<=` ===== ;

;; TODO add variadic arity
(t/defn ^:inline <=
  "Numeric less-than-or-value-equal comparison."
  > ut/boolean?)

; ===== `>` ===== ;

;; TODO add variadic arity
(t/defn ^:inline >
  "Numeric greater-than comparison."
  > ut/boolean?)

; ===== `>=` ===== ;

;; TODO add variadic arity
(t/defn ^:inline >=
  "Numeric greater-than-or-value-equal comparison."
  > ut/boolean?)

; ===== `compare` ===== ;

(var/def icomparable?
  "That which is comparable to its own 'concrete type' (i.e. class)."
  #?(:clj  (t/isa? java.lang.Comparable)
           ;; TODO other things are comparable; really it depends on the two objects in question
     :cljs (t/or p/nil? (t/isa? cljs.core/IComparable))))

(def comparison? #?(:clj p/int? :cljs p/double?))

(t/defn ^:inline compare
  "Logical (not numeric) comparison.

   When ->`a` is logically 'less than'    ->`b`, outputs a negative number.
   When ->`a` is logically 'equal to'     ->`b`, outputs zero.
   When ->`a` is logically 'greater than' ->`b`, outputs a positive number."
  {:incorporated '{clojure.lang.Util/compare "9/27/2018"
                   clojure.core/compare      "9/27/2018"
                   cljs.core/compare         "9/27/2018"}}
  > comparison?
  ;; TODO TYPED should we use `>int` here?
  ([a p/nil?      , b p/val?] (int -1))
  ;; TODO TYPED should we use `>int` here?
  ([a p/val?      , b p/nil?] (int  1))
  ([a p/primitive?, b p/primitive?]
    (ifs (> a b) (int  1)
         (< a b) (int -1)
         (int 0)))
  ([^Comparable a ^Comparable b] (.compareTo a b))
  ([^Comparable a ^prim?      b] (.compareTo a b))
  ([^prim?      a ^Comparable b] (int (.compareTo (p/box a) b))))

static public int compare(Object k1, Object k2){
	if(k1 == k2)
		return 0;

  if(k1 instanceof Number)
    return Numbers.compare((Number) k1, (Number) k2);
  return ((Comparable) k1).compareTo(k2);
}

(defn ^number compare
  [x y]
  (cond
    (identical? x y) 0

   (satisfies? IComparable x)
   (-compare x y)

   :else
   (if (and (or (string? x) (array? x) (boolean? x))
            (identical? (type x) (type y)))
     (garray/defaultCompare x y)
     (throw (js/Error. (str "Cannot compare " x " to " y))))))


; ----- `comp<` ----- ;

#?(:clj  (defnt' ^boolean comp<-bin
           ([^comparable? x] true)
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

; ----- `comp<=` ----- ;

#?(:clj  (defnt' ^boolean comp<=-bin
           ([^comparable? x] true)
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
           ([#{byte char short int long float double} x] true)
           ([#{byte char short int long float double} x
             #{byte char short int long float double} y] (Numeric/gt x y))
           ; TODO numbers, but not nil
           )
   :cljs (defn >-bin ([x] true) ([x y] (core/> x y))))

#?(:clj (variadic-predicate-proxy > >-bin))
#?(:clj (variadic-predicate-proxy >& >-bin&))

; ----- `comp>` ----- ;

#?(:clj  (defnt' ^boolean comp>-bin
           ([^comparable? x] true)
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
           ([#{byte char short int long float double} x] true)
           ([#{byte char short int long float double} x
             #{byte char short int long float double} y] (Numeric/gte x y))
           ; TODO numbers, but not nil
           )
   :cljs (defn >=-bin ([x] true) ([x y] (core/>= x y))))

#?(:clj (variadic-predicate-proxy >= >=-bin))
#?(:clj (variadic-predicate-proxy >=& >=-bin&))

; ----- `comp>=` ----- ;

#?(:clj  (defnt' ^boolean comp>=-bin
           ([^comparable? x] true)
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

#?(:clj
(defmacro gen-extremum-key-fn [sym base-sym]
 `(defn ~sym
    ([kf#] nil) ; TODO really, the min of whatever it is; maybe gen via `(kf)` ?
    ([kf# x#] x#)
    ([kf# x# y#] (if (~base-sym (kf# x#) (kf# y#)) x# y#)) ; TODO can terminate early here with e.g. <=, <, etc.
    ([kf# x# y# & more#]
      (reduce #(~sym kf# %1 %2) (~sym kf# x# y#) more#)))))

(defn first-min-temp ([x] x) ([x y] (if (core/<= x y) x y)))
(defalias second-min-temp core/min)
(defalias min-temp second-min-temp)

(defn first-max-temp ([x] x) ([x y] (if (core/>= x y) x y)))
(defalias second-max-temp core/max)
(defalias max-temp second-max-temp)

(defn comp<-temp  [x y] (core/<  (core/compare x y) 0))
(defn comp<=-temp [x y] (core/<= (core/compare x y) 0))
(defn comp>-temp  [x y] (core/>  (core/compare x y) 0))
(defn comp>=-temp [x y] (core/>= (core/compare x y) 0))

; TODO don't need to generate these once type inference is done
; `first-min-key` means `min-key`, but returns the first argument when comparison is ambiguous
(gen-extremum-key-fn first-min-key           core/<=) ; TODO use comp/ version
(gen-extremum-key-fn second-min-key          core/< ) ; TODO use comp/ version
(defalias min-key second-min-key)

(gen-extremum-key-fn first-comp-min-key  comp<=-temp) ; TODO use comp/ version
(gen-extremum-key-fn second-comp-min-key comp<-temp ) ; TODO use comp/ version
(defalias comp-min-key second-comp-min-key)

(gen-extremum-key-fn first-max-key           core/>=) ; TODO use comp/ version
(gen-extremum-key-fn second-max-key          core/> ) ; TODO use comp/ version
(defalias max-key second-max-key)

(gen-extremum-key-fn first-comp-max-key  comp>=-temp) ; TODO use comp/ version
(gen-extremum-key-fn second-comp-max-key comp>-temp ) ; TODO use comp/ version
(defalias comp-max-key second-comp-max-key)
