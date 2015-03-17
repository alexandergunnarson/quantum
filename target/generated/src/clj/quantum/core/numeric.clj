(ns quantum.core.numeric
  (:require
    [quantum.core.logic :as log :refer
             [splice-or fn-and fn-or fn-not ifn if*n whenc whenf whenf*n whencf*n condf condf*n]
                                            
                          
                                                                  ]
    [quantum.core.type     :as type :refer
      [      bigint? instance+? array-list? boolean? double? map-entry? sorted-map?
       queue? lseq? coll+? pattern? regex? editable? transient?]]
    [quantum.core.ns :as ns :refer
            [alias-ns defalias]
                                                
                                                           
                                                                      ])
       
  (:import
    clojure.core.Vec
    (quantum.core.ns
      Nil Bool Num ExactNum Int Decimal Key Set
             ArrList TreeMap LSeq Regex Editable Transient Queue Map))
        (:gen-class))

      (set! *warn-on-reflection* true)

; https://github.com/clojure/math.numeric-tower/
(defn sign [n]  (if (neg? n) -1 1))
(def  nneg?     (fn-not neg?))
(def  pos-int?  (fn-and integer? pos?))
(def  nneg-int? (fn-and integer? nneg?))
(def  neg       (partial * -1))
(def  abs       (whenf*n neg? neg))
(def  int-nil   (whenf*n nil? (constantly 0)))

     
(defn rationalize+ [n]
  (-> n rationalize
      (whenf bigint? long)))

(defn floor [x]
         (java.lang.Math/floor x)
                                 )

(defn ceil [x]
         (java.lang.Math/ceil x)
                                )

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
     
  (java.lang.Math/sin n)
      
                  )

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
               
  (int+ [i]
           (Integer/parseInt i)
                               ))

; from thebusby.bagotricks
(defprotocol ToLong  
  (long+ [i] "A simple function to coerce numbers, and strings, etc; to a long.
   Note: nil input returns nil."))
(extend-protocol ToLong
        java.lang.Integer
                ; was js/Number, but this gives compilation errors
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
                ; was js/String, but this gives compilation errors
  (long+ [l]
           (Long/parseLong l)
                                  ))

;;;;;;;;;;;; This file autogenerated from src/cljx/quantum/core/numeric.cljx
