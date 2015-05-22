(ns
  ^{:doc "Useful numeric functions. Floor, ceil, round, sin, abs, neg, etc."
    :attribution "Alex Gunnarson"}
  quantum.core.numeric
  (:require-quantum [ns logic type fn macros]))

; https://github.com/clojure/math.numeric-tower/
(defn sign [n]  (if (neg? n) -1 1))
(def  nneg?     (fn-not neg?))
(def  pos-int?  (fn-and integer? pos?))
(def  nneg-int? (fn-and integer? nneg?))
(def  neg       (partial * -1))
(def  abs       (whenf*n neg? neg))
(def  int-nil   (whenf*n nil? (constantly 0)))

(defn exp
  {:todo "Performance"}
  [x n]
  (loop [acc 1 n n]
    (if (zero? n) acc
        (recur (*' x acc) (unchecked-dec n)))))

#?(:clj
  (defn rationalize+ [n]
    (-> n rationalize
        (whenf bigint? long))))

(defn floor [x]
  #?(:clj  (java.lang.Math/floor x)
     :cljs (.floor js/Math       x)))

(defn ceil [x]
  #?(:clj  (java.lang.Math/ceil x)
     :cljs (.ceil js/Math       x)))

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


#?(:clj
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
      (.setScale ^BigDecimal (bigdec num-0) ^Integer to round-type))))

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
  #?(:clj  (java.lang.Math/sin n)
     :cljs (.sin js/Math       n)))

;___________________________________________________________________________________________________________________________________
;=================================================={   TAKE BOXED MATH AWAY   }=====================================================
;=================================================={                          }=====================================================
; Needs to be unboxed earlier...
; (defprotocol Unbox
;   (<% [a b]))

; (extend-protocol Unbox
;   Integer
;     (<% [a b] (< (int  a) (int  b)))
;   Long
;     (<% [a b] (< (long a) (long b)))
;   Double
;     (<% [a b] (< (double a) (double b))))

;___________________________________________________________________________________________________________________________________
;=================================================={       TYPE-CASTING       }=====================================================
;=================================================={                          }=====================================================
(defnt int+
  int?    ([n] n      )
  long?   ([n] (int n))
  double? ([n] (int n))
  float?  ([n] (int n))
  nil?    ([n] nil    )
  string? (([s] #?(:clj  (Integer/parseInt s)
                   :cljs (-> s js/parseInt int)))
           #?(:clj ([s radix] (Integer/parseInt s radix)))))

(defnt long+
  long?   ([n] n       )
  double? ([n] (long n))
  float?  ([n] (long n))
  nil?    ([n] nil     )
  string? (([s] #?(:clj  (Long/parseLong s)
                   :cljs (-> s js/parseInt long)))
           #?(:clj ([s radix] (Long/parseLong s radix)))))
