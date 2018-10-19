(ns quantum.core.compare.core
  "Defines fundamental comparison operators but does not necessarily provide definitions for all
   type overloads.

   A complete (w.r.t. the `quantum.core.data.*` namespaces) set of definitions for type overloads is
   found in `quantum.core.compare`."
        (:refer-clojure :exclude
          [< <= = not= == > >= compare max max-key min min-key])
        (:require
          [clojure.core              :as core]
          [quantum.core.type         :as t]
          ;; TODO TYPED excise
          [quantum.untyped.core.logic
            :refer [ifs]]
          [quantum.untyped.core.type :as ut]
          ;; TODO TYPED excise
          [quantum.untyped.core.vars :as var])
#?(:clj (:import
          [quantum.core Numeric])))

;; Some of the ideas here adapted from gfredericks/compare
;; TODO include diffing
;; TODO comp< vs. < on numbers
;; TODO `hash=`

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
  ([a t/nil?         , b t/nil?]          true)
  ([a t/nil?         , b (t/ref ut/val?)] false)
  ([a (t/ref ut/val?), b t/nil?]          false)
  ;; The fallback overload; collections (in CLJ) and protocol-native objects (in CLJS) will have a
  ;; more specific equivalence check as defined later on
  ([a (t/ref ut/val?), b (t/ref ut/val?)]
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

;; ===== `<` ===== ;;

;; TODO add variadic arity
(t/defn ^:inline <
  "Numeric less-than comparison."
  {:incorporated '{clojure.lang.Numbers/lt "10/14/2018"
                   clojure.core/<          "10/14/2018"
                   cljs.core/<             "10/14/2018"}}
  > ut/boolean?)

;; ===== `<=` ===== ;;

;; TODO add variadic arity
(t/defn ^:inline <=
  "Numeric less-than-or-value-equal comparison."
  {:incorporated '{clojure.lang.Numbers/lte "10/14/2018"
                   clojure.core/<=          "10/14/2018"
                   cljs.core/<=             "10/14/2018"}}
  > ut/boolean?)

;; ===== `>` ===== ;;

;; TODO add variadic arity
(t/defn ^:inline >
  "Numeric greater-than comparison."
  {:incorporated '{clojure.lang.Numbers/gt "10/14/2018"
                   clojure.core/>          "10/14/2018"
                   cljs.core/>             "10/14/2018"}}
  > ut/boolean?)

;; ===== `>=` ===== ;;

;; TODO add variadic arity
(t/defn ^:inline >=
  "Numeric greater-than-or-value-equal comparison."
  {:incorporated '{clojure.lang.Numbers/gte "10/14/2018"
                   clojure.core/>=          "10/14/2018"
                   cljs.core/>=             "10/14/2018"}}
  > ut/boolean?)

;; ===== `compare` ===== ;;

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
        #?(:clj  (.compareTo                a b)
           :cljs (core/-compare ^not-native a b)))))

;; ----- `comp`-comparison ----- ;;

(t/defn ^:inline comp<
  "Returns true if args are in monotonically increasing order according to `compare`,
   otherwise false."
  > ut/boolean?)

(t/defn ^:inline comp<=
  "Returns true if args are in monotonically non-decreasing order according to `compare`,
   otherwise false."
  > ut/boolean?)

(t/defn ^:inline comp=
  "Returns true if args are equally ordered according to `compare`, otherwise false."
  > ut/boolean?)

(t/defn ^:inline comp>=
  "Returns true if args are in monotonically non-increasing order according to `compare`,
   otherwise false."
  > ut/boolean?)

(t/defn ^:inline comp>
  "Returns true if args are in monotonically decreasing order according to `compare`,
   otherwise false."
  > ut/boolean?)

;; ----- Extrema ----- ;;

(t/defn ^:inline min
  {:incorporated {'js/Math.min   #inst "2018-10-17"
                  'cljs.core/min #inst "2018-10-17"}})

(t/defn ^:inline max
  {:incorporated {'js/Math.max   #inst "2018-10-17"
                  'cljs.core/max #inst "2018-10-17"}})

(t/defn ^:inline min-key)

(t/defn ^:inline max-key)

(t/defn comp-min
  "Returns the least of the arguments according to `compare`, preferring later values.")

(t/defn comp-max
  "Returns the greatest of the arguments according to `compare`, preferring later values.")
