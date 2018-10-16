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
          [quantum.untyped.core.type       :as ut]
          ;; TODO TYPED excise
          [quantum.untyped.core.vars       :as var])
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
         ;; Everything is self-identical (except, implementationally, NaN and Infinity)
         ([x t/any?] true)
#?(:clj  ([a t/ref?, b t/ref?] (clojure.lang.Util/identical a b))
   :cljs ([a t/any?, b t/any?] (cljs.core/identical? a b))))

;; TODO add variadic arity
(t/defn ^:inline not==
  "Tests identity-inequality."
  > ut/boolean?
         ;; Nothing is self-non-identical (except, implementationally, NaN and Infinity)
         ([x t/any?] false)
#?(:clj  ([a t/ref?, b t/ref?] (Numeric/nonIdentical a b))
   :cljs ([a t/any?, b t/any?] (js* "(~{} !== ~{})" a b))))

;; TODO add variadic arity
(t/defn ^:inline =
  "Tests value-equality. Same as Java's `x.equals(y)`, except it also works for nil, and compares
   numbers and collections in a type-independent manner. For numbers, it works like `core/==`."
  {:incorporated '{clojure.lang.Numbers/equals "10/14/2018"
                   clojure.lang.Numbers/equiv  "10/14/2018"
                   clojure.lang.Util/equiv     "9/27/2018"
                   clojure.core/=              "9/27/2018"
                   cljs.core/=                 "9/27/2018"}}
  > ut/boolean?
  ;; Everything is self-equal (except, implementationally, NaN and Infinity)
  ([x t/any?] true)
  ([a t/nil?        , b t/nil?]         true)
  ([a t/nil?        , b (t/ref t/val?)] false)
  ([a (t/ref t/val?), b t/nil?]         false)
  ;; The fallback overload; collections (in CLJ) and protocol-native objects (in CLJS) will have a
  ;; more specific equivalence check as defined later on
  ([a (t/ref t/val?), b (t/ref t/val?)]
    (or (== a b)
        #?(:clj  (.equals            a b)
           :cljs (-equiv ^non-native a b)))))

;; TODO add variadic arity
(t/defn ^:inline not=
  "Tests value-inequality."
  {:incorporated '{clojure.core/not= "9/27/2018"
                   cljs.core/not=    "9/27/2018"}}
  > ut/boolean?
  ;; Nothing is self-unequal (except, implementationally, NaN and Infinity)
  ([x t/any?] false))

; ===== `<` ===== ;

;; TODO add variadic arity
(t/defn ^:inline <
  "Numeric less-than comparison."
  {:incorporated '{clojure.lang.Numbers/lt "10/14/2018"
                   clojure.core/<          "10/14/2018"
                   cljs.core/<             "10/14/2018"}}
  > ut/boolean?)

; ===== `<=` ===== ;

;; TODO add variadic arity
(t/defn ^:inline <=
  "Numeric less-than-or-value-equal comparison."
  {:incorporated '{clojure.lang.Numbers/lte "10/14/2018"
                   clojure.core/<=          "10/14/2018"
                   cljs.core/<=             "10/14/2018"}}
  > ut/boolean?)

; ===== `>` ===== ;

;; TODO add variadic arity
(t/defn ^:inline >
  "Numeric greater-than comparison."
  {:incorporated '{clojure.lang.Numbers/gt "10/14/2018"
                   clojure.core/>          "10/14/2018"
                   cljs.core/>             "10/14/2018"}}
  > ut/boolean?)

; ===== `>=` ===== ;

;; TODO add variadic arity
(t/defn ^:inline >=
  "Numeric greater-than-or-value-equal comparison."
  {:incorporated '{clojure.lang.Numbers/gte "10/14/2018"
                   clojure.core/>=          "10/14/2018"
                   cljs.core/>=             "10/14/2018"}}
  > ut/boolean?)

; ===== `compare` ===== ;

(var/def icomparable?
  "That which implements the interface marking comparability to its own 'concrete type' (i.e.
   class)."
  #?(:clj  (t/isa?        java.lang.Comparable)
     :cljs (t/isa|direct? cljs.core/IComparable)))

(def comparison? #?(:clj ut/int? :cljs ut/double?))

(t/defn ^:inline compare
  "Logical (not exclusively numeric) comparison.

   When ->`a` is logically 'less than'    ->`b`, outputs a negative number.
   When ->`a` is logically 'equal to'     ->`b`, outputs zero.
   When ->`a` is logically 'greater than' ->`b`, outputs a positive number."
  {:incorporated '{clojure.lang.Util/compare "9/27/2018"
                   clojure.core/compare      "9/27/2018"
                   cljs.core/compare         "9/27/2018"}}
  > comparison?
  ([a ut/nil?             , b ut/nil?] (int  0))
  ([a ut/nil?             , b ut/val?] (int -1))
  ([a ut/val?             , b ut/nil?] (int  1))
  ;; Fallbacks
  ([a (t/ref icomparable?), b (t/ref icomparable?)]
    (if (== a b)
        (int 0)
        #?(:clj (.compareTo a b) :cljs (core/-compare ^not-native a b))))
  ([a t/ref?, b t/ref?]
    (if (== a b)
        (int 0)
        (throw (#?(:clj clojure.lang.ExceptionInfo. :cljs cljs.core/ExceptionInfo.)
                 "Cannot compare incomparable values" {:type0 (type a) :type1 (type b)} nil)))))

(defn ^number compare
  [x y]
  (cond
   (number? x) (if (number? y)
                 (garray/defaultCompare x y)
                 (throw (js/Error. (str "Cannot compare " x " to " y))))

   :else
   (if (and (or (string? x) (array? x) (true? x) (false? x))
            (identical? (type x) (type y)))
     (garray/defaultCompare x y)
     (throw (js/Error. (str "Cannot compare " x " to " y))))))


; ----- `comp<` ----- ;

#?(:clj  (defnt' ^boolean comp<-bin
           "Returns true if args are in monotonically increasing order according to `compare`,
            otherwise false."
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
