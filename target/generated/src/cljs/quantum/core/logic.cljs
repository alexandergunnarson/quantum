(ns quantum.core.logic
  (:require
    [quantum.core.ns :as ns :refer
                               
             [Exception IllegalArgumentException
              Nil Bool Num ExactNum Int Decimal Key Vec Set
              ArrList TreeMap LSeq Regex Editable Transient Queue Map]]
    [quantum.core.function :as fn :refer
                                                    
             [compr f*n fn* unary]
             :refer-macros
             [fn->> fn-> <-]])
       
          
                    
                    
                                               
                                                                      
                    )

                                      

; TODO: ; cond-not, for :pre

;___________________________________________________________________________________________________________________________________
;==================================================={ BOOLEANS + CONDITIONALS }=====================================================
;==================================================={                         }=====================================================
(def  nnil?   (comp not nil?))   ; same as /seq/ - nil punning
(def  nempty? (comp not empty?))
(def  nseq?   (comp not seq?))
(defn iff  [pred const else]
  (if (pred const) const else))
(defn iffn [pred const else-fn]
  (if (pred const)
      const
      (else-fn const)))
(def eq?  (unary =))
(def fn= eq?)
(def fn-eq? eq?)
(def neq? (unary not=))
(def fn-neq? neq?)
(def any? some)
(defn apply-and [arg-list]
  (every? identity arg-list))
(defn apply-or  [arg-list]
  (any?  identity arg-list))
(defn dor [& args] ; xor
  (and (apply-or args)
       (not (apply-and args))))
(defn pred-or   [pred obj args]
  (apply-or  (map (pred obj) args)))
(defn pred-and  [pred obj args]
  (apply-and (map (pred obj) args)))
(def fn-and every-pred)
(def fn-or  some-fn)
(def fn-not complement)
(defn splice-or  [obj compare-fn & coll]
  (any?   (partial compare-fn obj) coll))
(defn splice-and [obj compare-fn & coll]
  (every? (partial compare-fn obj) coll))
(defn fn-pred-or  [pred-fn args]
  (apply fn-or  (map pred-fn args)))
(defn fn-pred-and [pred-fn args]
  (apply fn-and (map pred-fn args)))
(defn coll-or [& elems]
  (fn [bin-pred obj]
    ((fn-pred-or (unary bin-pred) elems) obj)))
(defn coll-and
  {:usage "((and-coll 1 2 3) < 0) => true (0 is less than 1, 2, and 3)"}
  [& elems]
  (fn [bin-pred obj]
    ((fn-pred-and (unary bin-pred) elems) obj)))
(def  empty+? (fn-or nseq? empty?))
(defn bool [v]
  (cond
    (= v 0) false
    (= v 1) true
    :else
      (throw (IllegalArgumentException. (str "Value not booleanizable: " v)))))
(defn rcompare
  "Reverse comparator."
  {:attribution "taoensso.encore"}
  [x y]
  (compare y x))
               
                                                                                    
                 
                             
                                
                                              
                                        
                                      
                      
                                                                                                    
                            
                                                 
                                              
                                         
                                                 
                                              
                    
                              
                          
                                   
                           
                                                   
                
                                                                         
                 
                             
                                
                                              
                                        
                                      
                      
                                                                                                    
                            
                                                 
                                              
                                                                                                                
                                                 
                                              
                    
                              
                                                          
                      
                                                             
                                                                                                 
       
                                                              
                      
                                               
                                                                                                 
       
                                            
                                                       
                                      
                                                   
               
                    
                                                   
                                  
                    
                      
                                                   
               
                                                          
                           
                     
                      
                                           
                  
                     
                                                   
                                                          
                   
                         
                                                            


(def is? #(%1 %2)) ; for use with condp

; (defn condpc ; force it to delay like cond
;   "/condp/ for colls."
;   [pred const & exprs]
;   (loop [[pred-coll expr :as exprs-n] exprs]
;       (if (or (= pred-coll :else)
;               (ifn pred-coll fn?
;                 (*fn pred const)
;                 (partial pred const))
;               (empty? exprs-n))
;           expr
;           (recur (-> exprs-n rest rest)))))
                
                      
                                                          
                       
                               
                               
                                      
                                              
                                                                     
                                        
                      
                                                                                                  
                            
                                            
                                                    
                                                     
                                  
                                                       



                                                       
                                   
                                                       
                     
                      
                                     

                  
                                                                         
                                                                       
                                                                       
                                                                      
                                                                
                                                                          
                                                                         
               
                               
                                  
                      
                                 
                                          
                        
            
                                 
                
                                           

; (defmacro ^{:private true} if-lets*
;    {:attribution "thebusby.bagotricks"}
;   [bindings then else]
;   (let [form (subvec bindings 0 2)
;         more (subvec bindings 2)]
;     (if (empty? more)
;       `(if-let ~form
;          ~then
;          ~else)
;       `(if-let ~form
;          (if-lets* ~more ~then ~else)
;          ~else))))

; (defmacro if-lets
  ;    {:attribution "thebusby.bagotricks"}
;   "Like if-let, but accepts multiple bindings and evaluates them sequentally.
;    binding evaluation halts on first falsey value, and 'else' clause activates."
;   ([bindings then]
;      `(if-lets ~bindings ~then nil))
;   ([bindings then else]
;      (cond
;       (not (even? (count bindings))) (throw (IllegalArgumentException. "if-lets requires an even number of bindings"))
;       (not (vector? bindings))       (throw (IllegalArgumentException. "if-lets requires a vector for its binding"))
;       :else `(if-lets* ~bindings ~then ~else))))
;;;;;;;;;;;; This file autogenerated from src/cljx/quantum/core/logic.cljx
