(ns quantum.core.compare
           (:refer-clojure :exclude
             [= not= < > <= >= max min max-key min-key neg? pos? zero? - +])
           (:require
             [#?(:clj  clojure.core
                 :cljs cljs.core   )     :as core  ]
             [quantum.core.error :as err
              :refer [TODO]]
             [quantum.core.macros
               :refer        [#?@(:clj [defnt defnt' variadic-predicate-proxy])]
               :refer-macros [defnt]]
             [quantum.core.vars
               :refer        [#?@(:clj [defalias])]
               :refer-macros [defalias]]
             [quantum.core.numeric.operators
               :refer        [#?@(:clj [- + abs])]
               :refer-macros [- + abs]]
             [quantum.core.numeric.predicates
               :refer        [#?@(:clj [neg? pos? zero?])]
               :refer-macros [neg? pos? zero?]]
             [quantum.core.numeric.types :as ntypes])
  #?(:cljs (:require-macros
             [quantum.core.compare
               :refer [< > <= >=]])))

; Some of the ideas here adapted from gfredericks/compare
; TODO include diffing
; TODO use -compare in CLJS 

#?(:clj  (defnt' =-bin
           (^boolean
             [#{byte char short int long float double} x
              #{byte char short int long float double} y]
             (quantum.core.Numeric/eq x y))
           (^boolean [^clojure.lang.BigInt x ^clojure.lang.BigInt y]
             (.equals x y)))
   :cljs (defn =-bin
           ([x] true)
           ([x y] (TODO "fix") (core/zero? (ntypes/-compare x y)))))

#?(:clj (variadic-predicate-proxy = quantum.core.compare/=-bin))

#?(:clj  (defnt' not=-bin
           (^boolean
             [#{byte char short int long float double} x
              #{byte char short int long float double} y]
             (not (=-bin x y))) ; TODO use primitive |not| fn
           (^boolean [^clojure.lang.BigInt x ^clojure.lang.BigInt y]
             (not (=-bin x y)))) ; TODO use primitive |not| fn
   :cljs (defn not=-bin
           ([x] false)
           ([x y] (TODO "fix") (not (core/zero? (ntypes/-compare x y))))))

#?(:clj (variadic-predicate-proxy not= not=-bin))

; ===== |<| =====

(defn <*
  ([x] true)
  ([x y] (neg? (compare x y))))

#?(:clj  (defmacro <-bin  [a b] `(quantum.core.Numeric/lt  ~a ~b)) ; TODO defnt this
   :cljs (defalias <-bin  <*))

#?(:clj (variadic-predicate-proxy
          ^{:doc "Returns truthy if args are in monotonically increasing order
                  according to |compare|, otherwise false."}
          < <-bin)) 

; ===== |<=| =====

(defn <=*
  ([x] true)
  ([x y] (not (pos? (compare x y)))))

#?(:clj  (defalias <=-bin <=*)
         #_(defmacro <=-bin [a b] `(quantum.core.Numeric/lte ~a ~b)) ; TODO defnt this
   :cljs (defalias <=-bin <=*))

#?(:clj (variadic-predicate-proxy
          ^{:doc "Returns truthy if args are in monotonically non-decreasing order
                  according to clojure.core/compare, otherwise false."}
          <= quantum.core.compare/<=-bin))

; ===== |>| =====

(defn >*
  ([x] true)
  ([x y] (pos? (compare x y))))

#?(:clj  (defalias >-bin  >* )
         #_(defmacro >-bin  [a b] `(quantum.core.Numeric/gt  ~a ~b)) ; TODO defnt this
   :cljs (defalias >-bin  >* ))

#?(:clj (variadic-predicate-proxy
          ^{:doc "Returns truthy if args are in monotonically decreasing order
                  according to |compare|, otherwise false."}
          > quantum.core.compare/>-bin)) 

; ===== |>=| =====

(defn >=*
  ([x] true)
  ([x y] (not (neg? (compare x y)))))

#?(:clj  (defalias >=-bin >=*)
         #_(defmacro >=-bin [a b] `(quantum.core.Numeric/gte ~a ~b)) ; TODO defnt this
   :cljs (defalias >=-bin >=*))

#?(:clj (variadic-predicate-proxy
          ^{:doc "Returns truthy if args are in monotonically non-increasing order
                  according to |compare|, otherwise false."}
          >= quantum.core.compare/>=-bin)) 

(defn min*
  ([x] x)
  ([x y] (if (< x y) x y)))

#?(:clj  (defalias min-bin min*)
         #_(defnt' min-bin
             (^{:tag :largest}
               [#{byte char short int float double} x
                #{byte char short int float double} y]
               (quantum.core.Numeric/min x y)))
   :cljs ; TODO incorporate bigint into this
         (defalias min-bin min*))

#?(:clj (variadic-predicate-proxy
          ^{:doc "Returns the least of the arguments according to
                  |compare|, preferring later values."}
          min quantum.core.compare/min-bin)) 

(defn max*
  ([x] x)
  ([x y] (if (> x y) x y)))

#?(:clj  (defalias max-bin max*)
         #_(defnt' max-bin
             (^{:tag :largest}
               [#{byte char short int float double} x
                #{byte char short int float double} y]
               (quantum.core.Numeric/max x y)))
   :cljs ; TODO incorporate bigint into this
         (defalias max-bin max*))

#?(:clj (variadic-predicate-proxy
          ^{:doc "Returns the greatest of the arguments according to
                  |compare|, preferring later values."}
          max quantum.core.compare/max-bin)) 

(defn min-key
  "Returns the x for which (k x) is least, according to
   |compare|."
  {:from "gfredericks/compare"}
  ([k x] x)
  ([k x y] (if (< (k x) (k y)) x y))
  ([k x y & more]
   (reduce #(min-key k %1 %2) (min-key k x y) more)))

(defn max-key
  "Returns the x for which (k x) is greatest, according to
   |compare|."
  {:from "gfredericks/compare"}
  ([k x] x)
  ([k x y] (if (> (k x) (k y)) x y))
  ([k x y & more]
   (reduce #(max-key k %1 %2) (max-key k x y) more)))

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

(defn least-or [a b else]
  (cond (< a b) a
        (< b a) b
        :else else))

(defn greatest-or [a b else]
  (cond (> a b) a
        (> b a) b
        :else else))

#?(:clj
(defn compare-bytes-lexicographically
  "Byte arrays are not `Comparable`, so we need a custom
   comparator which we can feed to `sort`."
  {:from "clojure.tools.nrepl.bencode"}
  [^"[B" a ^"[B" b]
  (let [alen (alength a)
        blen (alength b)
        len  (min alen blen)]
    (loop [i 0]
      (if (== i len)
        (- alen blen)
        (let [x (- (int (aget a i)) (int (aget b i)))]
          (if (zero? x)
            (recur (inc i))
            x)))))))

#_(defn extreme-comparator [comparator-n]
  (get {> num/greatest
        < num/least   }
    comparator-n))

; ===== APPROXIMATION ===== ;

(defn approx=
  "Return true if the absolute value of the difference between x and y
   is less than eps."
  [x y eps]
  (< (abs (- x y)) eps))

(defn within-tolerance? [n total tolerance]
  (and (>= n (- total tolerance))
       (<= n (+ total tolerance))))


