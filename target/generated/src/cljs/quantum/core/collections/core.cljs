(ns quantum.core.collections.core
  (:require
    [quantum.core.error
                  
                         ]
    [quantum.core.function :as fn :refer
                                                                              
             [compr f*n fn* unary]
             :refer-macros
             [fn->> fn-> <-]]
    [quantum.core.logic :as log :refer
                                                                                 
                                                 
             [splice-or fn-and fn-or fn-not nnil? nempty?]
             :refer-macros
             [ifn if*n whenc whenf whenf*n whencf*n condf condf*n condfc]]
    [quantum.core.ns :as ns :refer
                               
             [Exception IllegalArgumentException
              Nil Bool Num ExactNum Int Decimal Key Vec Set
              ArrList TreeMap LSeq Regex Editable Transient Queue Map]]
    [quantum.core.reducers :as red :refer
                                                                                  
                                                      
             [map+ reduce+ filter+ remove+ take+ take-while+
              count* reducei+ fold+ range+ drop+]
             :refer-macros
             [for+]]
    [quantum.core.string :as str]
    [quantum.core.type :as type :refer
            [name-from-class class]
                                                                                                                               ]
    [quantum.core.data.vector :as vec :refer
      [subvec+ catvec]]
    [quantum.core.macros
                   
             :refer-macros
      [extend-protocol-type extend-protocol-types extend-protocol-for-all]])
       
          
                    
                                            
                    
                                               
                                                                      
                    )

                                      

; ; java.util.Collection class to use as part of protocol

(defn lasti
  "Last index of a coll."
  [coll]
  (-> coll count dec))

     
              
                       
                      
                       
                     
                        
                         
                      
                      
                          

(def coll-search-types
  {:vec                                                     Vec
   :rvec                                                    cljs.core/RSeq
   :list                                                    cljs.core/List
                             }) ; because |js/String| doesn't work and |string| doesn't work outside of a protocol

(                  
        defprotocol
  CollCount
  (count+ [coll]))

(                      
        extend-protocol CollCount
                               
         string
    (count+ [coll] (count coll))
                             
         cljs.core/LazySeq
    (count+ [coll] (count* coll))
                             
 ; #+cljs cljs.core/ICollection ; probably can't extend protocols this way in ClojureScript
  ; TODO: what about Java collections that are counted?
                                     
  nil
    (count+ [coll] 0)
               
         default
    (count+ [coll] 1))

      
                                    
                           
                                                                                 
            
                                                                      
                                                         
                     
                           
      
                                          
            
                             
                     
                                                 
                   
      
                               
;___________________________________________________________________________________________________________________________________
;=================================================={         RETRIEVAL        }=====================================================
;=================================================={     get, first, rest     }=====================================================
(                  
        defprotocol CollRetrieve
  (getr+    [coll a] [coll a b] "Get range")
  (get+     [coll n] [coll n if-not-found])
  (first+   [coll])
  (second+  [coll])
  (rest+    [coll])
  (butlast+ [coll])
  (last+    [coll])) 

     
                                       
            
                                                                      
                                                            
           
                                         
                                 
                               
                                                                      
                                       
                                       
                                                               

     
                                             
            
                             
                     
                                                    
                   

     
                                  

; take-up-to (combination of getr and index-of + 1)

                                                    
                                       
                                                              
       
                                                       
                                     
                                    
                                                                 
                                                       
                                                       
                                                                           
; extend first+ to use /first/ when the object is unknown.
; first+ with non-collection items (java.util.Collection) will return itself.

(extend-protocol CollRetrieve
  Keyword
    (rest+ [k]
      (rest+ (name k)))
                      string
    (first+   [coll]
      (try (subs coll 0 1)
                                                           ))
    (second+  [coll]
      (try (subs coll 1 2)
                                                             ))
    (rest+    [coll]
      (try (subs coll 1 (count+ coll))
                                                           ))
    (butlast+ [coll] (subs coll 0 (-> coll count+ dec)))
    (last+    [coll] (subs coll   (-> coll count+ dec)))
    (getr+    [coll a b] (str/subs+ coll a (inc (- b a))))) ; now inclusive range with |inc|; this could break lots of things

     
                                     
                                    
                                             
                                
                                                        
                                        
                                        
                                             
                                              

     
                             
                                
                                             
                                                            
          
                                  
                                                        
                                  
                                  
                                                   
                                              
                                 
                             
          
                                               
                                                        
                                    
          
                                      
                                                
                                                                              
                                  
                                 
                                    
                                 
         
                                  
                                                         

; ===== REDUCERS =====

      
(extend-protocol CollRetrieve
  cljs.core/Delay
    (getr+          [coll a b] (->> coll (take+ b) (drop+ a)))
    (first+         [coll]     (take+ 1 coll))
    (rest+          [coll]     (drop+ 1 coll))
    ;#+clj (butlast+ [coll]     (drop-last+ 1 coll))
    ;#+clj (last+    [coll]     (take-last+ 1 coll))
    )
; _____________________________________________________________________________________
; ====================================== VECTOR =======================================
; `````````````````````````````````````````````````````````````````````````````````````
      
(extend-protocol CollRetrieve
  cljs.core/PersistentVector
    (getr+    ([coll a b] (subvec+ coll a b))
              ([coll a]   (subvec+ coll a (-> coll count))))
    (get+ 
      ([coll n] (get+ coll n nil))
      ([coll n if-not-found] (get coll n if-not-found)))
    (first+   [coll] (get coll 0))
    (second+  [coll] (get coll 1))
    (rest+    [coll] (subvec+ coll 1 (count coll)))
    (butlast+ [coll] (whenf coll nempty? pop))
    (last+    [coll] (peek coll)))

      
(extend-protocol CollRetrieve
  cljs.core/TransientVector
    (getr+    ([coll a b] (subvec+ coll a b))
              ([coll a]   (subvec+ coll a (-> coll count))))
    (get+ 
      ([coll n] (get+ coll n nil))
      ([coll n if-not-found] (get coll n if-not-found)))
    (first+   [coll] (get coll 0))
    (second+  [coll] (get coll 1))
    (rest+    [coll] (subvec+ coll 1 (count coll)))
    (butlast+ [coll] (whenf coll nempty? pop))
    (last+    [coll] (peek coll)))

; _____________________________________________________________________________________
; ======================================== MAP ========================================
; `````````````````````````````````````````````````````````````````````````````````````
      
(extend-protocol CollRetrieve
  PersistentArrayMap
    (get+ 
      ([coll n]              (get+ coll n nil))
      ([coll n if-not-found] (get coll n if-not-found))))

      
(extend-protocol CollRetrieve
  TransientArrayMap
    (get+ 
      ([coll n]              (get+ coll n nil))
      ([coll n if-not-found] (get coll n if-not-found))))

      
(extend-protocol CollRetrieve
  PersistentHashMap
    (get+ 
      ([coll n]              (get+ coll n nil))
      ([coll n if-not-found] (get coll n if-not-found))))

      
(extend-protocol CollRetrieve
  TransientHashMap
    (get+ 
      ([coll n]              (get+ coll n nil))
      ([coll n if-not-found] (get coll n if-not-found))))

      
(extend-protocol CollRetrieve
  PersistentTreeMap
    (get+ 
      ([coll n]              (get+ coll n nil))
      ([coll n if-not-found] (get coll n if-not-found))))

; _____________________________________________________________________________________
; ==================================== COLLECTION =====================================
; `````````````````````````````````````````````````````````````````````````````````````
      
(extend-protocol CollRetrieve
  cljs.core/List
    (getr+
      ([coll a]   (->> coll (drop a)))
      ([coll a b] (->> coll (take b) (drop a))))
    (first+   [coll] (nth coll 0)) ; perhaps implement in terms of /reduce/ ; 
    (second+  [coll] (get coll 1))
    (rest+    [coll] (rest coll))
    (butlast+ [coll] (butlast coll))
    (last+    [coll] (last coll))
    (get+
      ([coll n] (get+ coll n nil))
      ([coll n if-not-found] (nth coll n if-not-found))))

      
(extend-protocol CollRetrieve
  cljs.core/EmptyList
    (first+   [coll] nil))

      
(extend-protocol CollRetrieve
  cljs.core/PersistentQueue
    (getr+
      ([coll a]   (->> coll (drop a)))
      ([coll a b] (->> coll (take b) (drop a))))
    (first+   [coll] (nth coll 0)) ; perhaps implement in terms of /reduce/ ; 
    (second+  [coll] (get coll 1))
    (rest+    [coll] (rest coll))
    (butlast+ [coll] (butlast coll))
    (last+    [coll] (last coll))
    (get+
      ([coll n] (get+ coll n nil))
      ([coll n if-not-found] (nth coll n if-not-found))))

      
(extend-protocol CollRetrieve
  cljs.core/LazySeq
    (getr+
      ([coll a]   (->> coll (drop a)))
      ([coll a b] (->> coll (take b) (drop a))))
    (first+   [coll] (nth coll 0)) ; perhaps implement in terms of /reduce/ ; 
    (second+  [coll] (get coll 1))
    (rest+    [coll] (rest coll))
    (butlast+ [coll] (butlast coll))
    (last+    [coll] (last coll))
    (get+
      ([coll n] (get+ coll n nil))
      ([coll n if-not-found] (nth coll n if-not-found))))

      
(extend-protocol CollRetrieve
  cljs.core/KeySeq
    (getr+
      ([coll a]   (->> coll (drop a)))
      ([coll a b] (->> coll (take b) (drop a))))
    (first+   [coll] (nth coll 0)) ; perhaps implement in terms of /reduce/ ; 
    (second+  [coll] (get coll 1))
    (rest+    [coll] (rest coll))
    (butlast+ [coll] (butlast coll))
    (last+    [coll] (last coll))
    (get+
      ([coll n] (get+ coll n nil))
      ([coll n if-not-found] (nth coll n if-not-found))))

      
(extend-protocol CollRetrieve
  cljs.core/ValSeq
    (getr+
      ([coll a]   (->> coll (drop a)))
      ([coll a b] (->> coll (take b) (drop a))))
    (first+   [coll] (nth coll 0)) ; perhaps implement in terms of /reduce/ ; 
    (second+  [coll] (get coll 1))
    (rest+    [coll] (rest coll))
    (butlast+ [coll] (butlast coll))
    (last+    [coll] (last coll))
    (get+
      ([coll n] (get+ coll n nil))
      ([coll n if-not-found] (nth coll n if-not-found))))

      
(extend-protocol CollRetrieve
  cljs.core/IndexedSeq
    (getr+
      ([coll a]   (->> coll (drop a)))
      ([coll a b] (->> coll (take b) (drop a))))
    (first+   [coll] (nth coll 0)) ; perhaps implement in terms of /reduce/ ; 
    (second+  [coll] (get coll 1))
    (rest+    [coll] (rest coll))
    (butlast+ [coll] (butlast coll))
    (last+    [coll] (last coll))
    (get+
      ([coll n] (get+ coll n nil))
      ([coll n if-not-found] (nth coll n if-not-found))))

      
(extend-protocol CollRetrieve
  cljs.core/ChunkedSeq
    (getr+
      ([coll a]   (->> coll (drop a)))
      ([coll a b] (->> coll (take b) (drop a))))
    (first+   [coll] (nth coll 0)) ; perhaps implement in terms of /reduce/ ; 
    (second+  [coll] (get coll 1))
    (rest+    [coll] (rest coll))
    (butlast+ [coll] (butlast coll))
    (last+    [coll] (last coll))
    (get+
      ([coll n] (get+ coll n nil))
      ([coll n if-not-found] (nth coll n if-not-found))))

      
(extend-protocol CollRetrieve
  cljs.core/PersistentHashSet
    (getr+
      ([coll a]   (->> coll (drop a)))
      ([coll a b] (->> coll (take b) (drop a))))
    (first+   [coll] (nth coll 0)) ; perhaps implement in terms of /reduce/ ; 
    (second+  [coll] (get coll 1))
    (rest+    [coll] (rest coll))
    (butlast+ [coll] (butlast coll))
    (last+    [coll] (last coll))
    (get+
      ([coll n] (get+ coll n nil))
      ([coll n if-not-found] (nth coll n if-not-found))))

      
(extend-protocol CollRetrieve
  cljs.core/TransientHashSet
    (getr+
      ([coll a]   (->> coll (drop a)))
      ([coll a b] (->> coll (take b) (drop a))))
    (first+   [coll] (nth coll 0)) ; perhaps implement in terms of /reduce/ ; 
    (second+  [coll] (get coll 1))
    (rest+    [coll] (rest coll))
    (butlast+ [coll] (butlast coll))
    (last+    [coll] (last coll))
    (get+
      ([coll n] (get+ coll n nil))
      ([coll n if-not-found] (nth coll n if-not-found))))

      
(extend-protocol CollRetrieve
  cljs.core/PersistentTreeSet
    (getr+
      ([coll a]   (->> coll (drop a)))
      ([coll a b] (->> coll (take b) (drop a))))
    (first+   [coll] (nth coll 0)) ; perhaps implement in terms of /reduce/ ; 
    (second+  [coll] (get coll 1))
    (rest+    [coll] (rest coll))
    (butlast+ [coll] (butlast coll))
    (last+    [coll] (last coll))
    (get+
      ([coll n] (get+ coll n nil))
      ([coll n if-not-found] (nth coll n if-not-found))))

      
(extend-protocol CollRetrieve
  cljs.core/ArrayList
    (getr+
      ([coll a]   (->> coll (drop a)))
      ([coll a b] (->> coll (take b) (drop a))))
    (first+   [coll] (nth coll 0)) ; perhaps implement in terms of /reduce/ ; 
    (second+  [coll] (get coll 1))
    (rest+    [coll] (rest coll))
    (butlast+ [coll] (butlast coll))
    (last+    [coll] (last coll))
    (get+
      ([coll n] (get+ coll n nil))
      ([coll n if-not-found] (nth coll n if-not-found))))

(extend-protocol CollRetrieve
  ; sorted-map or sorted-set getr+ like subseq
                ; "default"
         default
    (get+
      ([obj n] (get+ obj n nil))
      ([obj n if-not-found] (get obj n)))
    (first+   [obj] obj)
    (last+    [obj] obj)
    (butlast+ [obj] obj)
  nil
    (get+     [obj n] obj)
    (first+   [obj]   obj)
    (last+    [obj]   obj)
    (butlast+ [obj]   obj))

(defn gets+ [coll & indices]
  (->> indices
       ; determine threshold of transient-persistent efficiency
       ; based on number of indices to retrieve
       (reduce (fn [ret ind] (conj! ret (get+ coll ind)))
         (transient []))
       persistent!))

(def  pop+  butlast+)
(def  popr+ butlast+)
(def  popl+ rest+)
(def  peek+ last+)

(defn getf+ [n] (f*n get+ n))
;___________________________________________________________________________________________________________________________________
;=================================================={        SEARCH/FIND       }=====================================================
;=================================================={    index-of, contains?   }=====================================================
(                  
        defprotocol
  CollSearch
    (index-of+      [coll elem])
    (last-index-of+ [coll elem]))

     
                                     
            
                                                                   
                                
                            
                                
                                                    
                                
                                    
                                                                  
                                                               
                                                                  

     
                                         
            
                             
                                                  
                           

                                        

      
(extend-protocol CollSearch
  cljs.core/PersistentVector
    (index-of+      [coll elem] (.indexOf     coll elem))
    (last-index-of+ [coll elem] (.lastIndexOf coll elem))
  cljs.core/TransientVector
    (index-of+      [coll elem] (.indexOf     coll elem))
    (last-index-of+ [coll elem] (.lastIndexOf coll elem))
  string
    (index-of+      [coll elem] (.indexOf     coll elem))
    (last-index-of+ [coll elem] (.lastIndexOf coll elem)))

(defn third [coll] (-> coll rest+ rest+ first+))
; If the array is not sorted:
; java.util.Arrays.asList(theArray).indexOf(o)
; If the array is sorted, you can make use of a binary search for performance:
; java.util.Arrays.binarySearch(theArray, o)

                
                                                                     
                 
                                            
                                      
                                   
                              
              
                                      
                     
                                                     
                


; (defn conjl [coll elem]
;   (condf coll
;     list?   (f*n cons elem)
;     vector? (partial into+ (vector elem))
;     coll?   (partial into+ (vector elem))))
;;;;;;;;;;;; This file autogenerated from src/cljx/quantum/core/collections/core.cljx
