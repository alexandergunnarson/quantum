(ns quantum.core.numeric
  (:require
    [quantum.core.logic :as log :refer
      #+clj  [splice-or fn-and fn-or fn-not ifn if*n whenc whenf whenf*n whencf*n condf condf*n]
      #+cljs [splice-or fn-and fn-or fn-not]
      #+cljs :refer-macros
      #+cljs [ifn if*n whenc whenf whenf*n whencf*n condf condf*n]]
    [quantum.core.type     :as type :refer
      [#+clj bigint? instance+? array-list? boolean? double? map-entry? sorted-map?
       queue? lseq? coll+? pattern? regex? editable? transient?]]
    [quantum.core.ns :as ns :refer
      #+clj [alias-ns defalias]
      #+cljs [Exception IllegalArgumentException
              Nil Bool Num ExactNum Int Decimal Key Vec Set
              ArrList TreeMap LSeq Regex Editable Transient Queue Map]])
  #+clj
  (:import
    clojure.core.Vec
    (quantum.core.ns
      Nil Bool Num ExactNum Int Decimal Key Set
             ArrList TreeMap LSeq Regex Editable Transient Queue Map))
  #+clj (:gen-class))

#+clj (set! *warn-on-reflection* true)

; https://github.com/clojure/math.numeric-tower/
(defn sign [n]  (if (neg? n) -1 1))
(def  nneg?     (fn-not neg?))
(def  pos-int?  (fn-and integer? pos?))
(def  nneg-int? (fn-and integer? nneg?))
(def  neg       (partial * -1))
(def  abs       (whenf*n neg? neg))
(def  int-nil   (whenf*n nil? (constantly 0)))

#+clj
(defn rationalize+ [n]
  (-> n rationalize
      (whenf bigint? long)))

(defn floor [x]
  #+clj  (java.lang.Math/floor x)
  #+cljs (.floor js/Math       x))

(defn ceil [x]
  #+clj  (java.lang.Math/ceil x)
  #+cljs (.ceil js/Math       x))

; TODO macro to reduce repetitiveness here
(defn safe+
  ([a]
    (int-nil a))
  ([a b]
    (+ (int-nil a) (int-nil b)))
  ([a b c]
    (+ (int-nil a) (int-nil b) (int-nil c)))
  ([a b c & args]
    (->> (conj args c b a) (map int-nil) (apply +))))
(defn safe*
  ([a]
    (int-nil a))
  ([a b]
    (* (int-nil a) (int-nil b)))
  ([a b c]
    (* (int-nil a) (int-nil b) (int-nil c)))
  ([a b c & args]
    (->> (conj args c b a) (map int-nil) (apply *))))
(defn safe-
  ([a]
    (neg (int-nil a)))
  ([a b]
    (- (int-nil a) (int-nil b)))
  ([a b c]
    (- (int-nil a) (int-nil b) (int-nil c)))
  ([a b c & args]
    (->> (conj args c b a) (map int-nil) (apply -))))
(defn safediv
  ([a b]
    (/ (int-nil a) (int-nil b)))
  ([a b c]
    (/ (int-nil a) (int-nil b) (int-nil c)))
  ([a b c & args]
    (->> (conj args c b a) (map int-nil) (apply /))))

#+clj
(defn round
  "Probably deprecated; use:
   |(with-precision <decimal-places> (bigdec <number>))|"
  {:todo ["Port to cljs"]}
  [num-0 & {:keys [type to] :or {to 0}}]
  (let [round-type
          (if (nil? type)
              (. BigDecimal ROUND_HALF_UP)
              (case type
                :unnecessary (. BigDecimal ROUND_UNNECESSARY)
                :ceiling     (. BigDecimal ROUND_CEILING)
                :up          (. BigDecimal ROUND_UP)
                :half-up     (. BigDecimal ROUND_HALF_UP)
                :half-even   (. BigDecimal ROUND_HALF_DOWN)
                :half-down   (. BigDecimal ROUND_HALF_DOWN)
                :down        (. BigDecimal ROUND_DOWN)
                :floor       (. BigDecimal ROUND_FLOOR)))]
    (.setScale ^BigDecimal (bigdec num-0) ^Integer to round-type)))

(defn rcompare
  "Reverse comparator."
  ^{:attribution "taoensso.encore, possibly via weavejester.medley"}
  [x y] (compare y x))

(defn greatest
  "Returns the 'greatest' element in coll in O(n) time."
  ^{:attribution "taoensso.encore, possibly via weavejester.medley"}
  [coll & [?comparator]]
  (let [comparator (or ?comparator rcompare)]
    (reduce #(if (pos? (comparator %1 %2)) %2 %1) coll))) ; almost certainly can implement this with /fold+/
(defn least
  "Returns the 'least' element in coll in O(n) time."
  ^{:attribution "taoensso.encore, possibly via weavejester.medley"}
  [coll & [?comparator]]
  (let [comparator (or ?comparator rcompare)]
    (reduce #(if (neg? (comparator %1 %2)) %2 %1) coll)))
(defn greatest-or [a b else]
  (cond (> a b) a
        (> b a) b
        :else else))
(defn least-or [a b else]
  (cond (< a b) a
        (< b a) b
        :else else))
(defn approx? [tolerance a b]
  (-> (- (int-nil a) (int-nil b)) abs (< tolerance)))

(defn sin [n]
#+clj
  (java.lang.Math/sin n)
#+cljs
  (.sin js/Math n))

;___________________________________________________________________________________________________________________________________
;=================================================={       TYPE-CASTING       }=====================================================
;=================================================={                          }=====================================================
; from thebusby.bagotricks
(defprotocol ToInt 
  (int+ [i] "A simple function to coerce numbers, and strings, etc; to an int.
   Note: nil input returns nil."))
(extend-protocol ToInt
  #+clj  java.lang.Integer
  #+cljs number
    (int+ [i] i)
  #+clj java.lang.Long
  #+clj (int+ [i] (int i))
  #+clj java.lang.Double
  #+clj (int+ [i] (int i))
  #+clj java.lang.Float
  #+clj (int+ [i] (int i))
  nil
  (int+ [_] nil)
  #+clj  java.lang.String
  #+cljs string
  (int+ [i]
    #+clj  (Integer/parseInt i)
    #+cljs (js/parseInt      i)))

; from thebusby.bagotricks
(defprotocol ToLong  
  (long+ [i] "A simple function to coerce numbers, and strings, etc; to a long.
   Note: nil input returns nil."))
(extend-protocol ToLong
  #+clj java.lang.Integer
  #+cljs number ; was js/Number, but this gives compilation errors
  (long+ [l] (long l))
  #+clj  java.lang.Long
  #+clj (long+ [l] l)
  #+clj java.lang.Double
  #+clj (long+ [l] (long l))
  #+clj java.lang.Float
  #+clj (long+ [l] (long l))
  nil
  (long+ [_] nil)
  #+clj  java.lang.String
  #+cljs string ; was js/String, but this gives compilation errors
  (long+ [l]
    #+clj  (Long/parseLong l)
    #+cljs (-> l js/parseInt long)))
