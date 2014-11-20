(ns quanta.library.numeric
  (:gen-class))
(set! *warn-on-reflection* true)
(require
  '[quanta.library.ns               :as ns    :refer [defalias alias-ns]])
(ns/require-all *ns* :clj)
  (require
    '[quanta.library.logic    :as log  :refer :all]
    '[quanta.library.function :as func :refer :all]
    '[quanta.library.type              :refer :all]
    '[clojure.core.reducers   :as r])

; (require '[taoensso.encore :as lib+ :refer
;   [pow ; round
;    ]])


; https://github.com/clojure/math.numeric-tower/
(defn sign [n]  (if (neg? n) -1 1))
(def  nneg?     (complement neg?))
(def  pos-int?  (fn-and integer? pos?))
(def  nneg-int? (fn-and integer? nneg?))
(def  neg       (partial * -1))
(def  abs       (whenf*n neg? neg))
(def  int-nil   (whenf*n nil? (constantly 0)))

(defn rationalize+ [n]
  (-> n rationalize
      (whenf bigint? long)))

; TODO reduce repetitiveness here
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

(defn round
  "Probably deprecated; use:
   |(with-precision <decimal-places> (bigdec <number>))|"
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
; (defn round [num- round-type]
;   (if (ratio? num-)
;       (if (= round-type :up)
;           (inc (int num-))
;           (int num-))
;       num-))

(defn greatest "Returns the 'greatest' element in coll in O(n) time."
  ^{:attribution "taoensso.encore, possibly via weavejester.medley"}
  [coll & [?comparator]]
  (let [comparator (or ?comparator rcompare)]
    (reduce #(if (pos? (comparator %1 %2)) %2 %1) coll))) ; almost certainly can implement this with /fold+/
(defn least "Returns the 'least' element in coll in O(n) time."
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
;___________________________________________________________________________________________________________________________________
;=================================================={       TYPE-CASTING       }=====================================================
;=================================================={                          }=====================================================
; from thebusby.bagotricks
(defprotocol ToInt 
  (int+ [i] "A simple function to coerce numbers, and strings, etc; to an int.
   Note: nil input returns nil."))
(extend-protocol ToInt
  java.lang.Integer
  (int+ [i] i)
  java.lang.Long
  (int+ [i] (int i))
  java.lang.Double
  (int+ [i] (int i))
  java.lang.Float
  (int+ [i] (int i))
  nil
  (int+ [_] nil)
  java.lang.String
  (int+ [i] (Integer/parseInt i)))
; from thebusby.bagotricks
(defprotocol ToLong  
  (long+ [i] "A simple function to coerce numbers, and strings, etc; to a long.
   Note: nil input returns nil."))
(extend-protocol ToLong
  java.lang.Integer
  (long+ [l] (long l))
  java.lang.Long
  (long+ [l] l)
  java.lang.Double
  (long+ [l] (long l))
  java.lang.Float
  (long+ [l] (long l))
  nil
  (long+ [_] nil)
  java.lang.String
  (long+ [l] (Long/parseLong l)))