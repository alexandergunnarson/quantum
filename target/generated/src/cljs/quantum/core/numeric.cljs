(ns quantum.core.numeric
  (:require
    [quantum.core.logic :as log :refer
                                                                                                
             [splice-or fn-and fn-or fn-not]
             :refer-macros
             [ifn if*n whenc whenf whenf*n whencf*n condf condf*n]]
    [quantum.core.type     :as type :refer
      [              instance+? array-list? boolean? double? map-entry? sorted-map?
       queue? lseq? coll+? pattern? regex? editable? transient?]]
    [quantum.core.ns :as ns :refer
                               
             [Exception IllegalArgumentException
              Nil Bool Num ExactNum Int Decimal Key Vec Set
              ArrList TreeMap LSeq Regex Editable Transient Queue Map]])
       
          
                    
                    
                                               
                                                                      
                    )

                                      

; https://github.com/clojure/math.numeric-tower/
(defn sign [n]  (if (neg? n) -1 1))
(def  nneg?     (fn-not neg?))
(def  pos-int?  (fn-and integer? pos?))
(def  nneg-int? (fn-and integer? nneg?))
(def  neg       (partial * -1))
(def  abs       (whenf*n neg? neg))
(def  int-nil   (whenf*n nil? (constantly 0)))

     
                      
                   
                            

(defn floor [x]
                                 
         (.floor js/Math       x))

(defn ceil [x]
                                
         (.ceil js/Math       x))

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
     
                        
      
  (.sin js/Math n))

;___________________________________________________________________________________________________________________________________
;=================================================={       TYPE-CASTING       }=====================================================
;=================================================={                          }=====================================================
; from thebusby.bagotricks
(defprotocol ToInt 
  (int+ [i] "A simple function to coerce numbers, and strings, etc; to an int.
   Note: nil input returns nil."))
(extend-protocol ToInt
                          
         number
    (int+ [i] i)
                      
                          
                        
                          
                       
                          
  nil
  (int+ [_] nil)
                         
         string
  (int+ [i]
                               
           (js/parseInt      i)))

; from thebusby.bagotricks
(defprotocol ToLong  
  (long+ [i] "A simple function to coerce numbers, and strings, etc; to a long.
   Note: nil input returns nil."))
(extend-protocol ToLong
                         
         number ; was js/Number, but this gives compilation errors
  (long+ [l] (long l))
                       
                     
                        
                            
                       
                            
  nil
  (long+ [_] nil)
                         
         string ; was js/String, but this gives compilation errors
  (long+ [l]
                             
           (-> l js/parseInt long)))

;;;;;;;;;;;; This file autogenerated from src/cljx/quantum/core/numeric.cljx
