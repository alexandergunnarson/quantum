(ns quantum.core.collections.core
  (:require
    [quantum.core.error
            :refer
            [try+ throw+]]
    [quantum.core.function :as fn :refer
             [compr f*n fn* unary fn->> fn-> <- defprotocol+ extend-protocol+]
                                  
                          
                            ]
    [quantum.core.logic :as log :refer
             [splice-or fn-and fn-or fn-not ifn if*n whenc whenf whenf*n whencf*n
              condf condf*n condfc nnil? nempty?]
                                                          
                          
                                                                         ]
    [quantum.core.ns :as ns :refer
            [alias-ns defalias]
                                                
                                                           
                                                                      ]
    [quantum.core.reducers :as red :refer
             [map+ reduce+ filter+ remove+ take+ take-while+ take-last+ drop-last+
              count* reducei+ fold+ range+ drop+ for+]
                                                            
                                                 
                          
                   ]
    [quantum.core.string :as str]
    [quantum.core.type :as type :refer
                                   
            [name-from-class ShortArray LongArray FloatArray IntArray DoubleArray BooleanArray ByteArray CharArray ObjectArray]]
    [quantum.core.data.vector :as vec :refer
      [subvec+ catvec]]
    [quantum.core.macros
             :refer
                          
      [extend-protocol-type extend-protocol-types extend-protocol-for-all]]
          [clj-tuple :as tup])
       
  (:import
    clojure.core.Vec
    java.util.ArrayList clojure.lang.Keyword
    (quantum.core.ns
      Nil Bool Num ExactNum Int Decimal Key Set
             ArrList TreeMap LSeq Regex Editable Transient Queue Map))
        (:gen-class))

      (set! *warn-on-reflection* true)

; TODO need to somehow incorporate |vector++| and |vector+|
      (defalias vector++ tup/vector)
      (defalias hash-map+ tup/hash-map)

; ; java.util.Collection class to use as part of protocol

(defn lasti
  "Last index of a coll."
  [coll]
  (-> coll count dec))

     
(def arr-types
  {:short    ShortArray
   :long     LongArray
   :float    FloatArray
   :int      IntArray
   :double   DoubleArray
   :boolean  BooleanArray
   :byte     ByteArray
   :char     CharArray
   :object   ObjectArray})

(def coll-search-types
  {:vec          clojure.lang.APersistentVector                
   :rvec         clojure.lang.APersistentVector$RSeq                      
   :list         clojure.lang.PersistentList                              
         :string       String}) ; because |js/String| doesn't work and |string| doesn't work outside of a protocol

(       defprotocol
                   
  CollCount
  (count+ [coll]))

(       extend-protocol
                        CollCount
         java.lang.CharSequence
               
    (count+ [coll] (count coll))
         clojure.lang.LazySeq
                          
    (count+ [coll] (count* coll))
         java.util.Collection
 ; #+cljs cljs.core/ICollection ; probably can't extend protocols this way in ClojureScript
  ; TODO: what about Java collections that are counted?
         (count+ [coll] (count coll))
  nil
    (count+ [coll] 0)
         Object
                
    (count+ [coll] 1))

      
(defmacro extend-coll-count-for-type
  "With helpful hints from:
  http://www.learningclojure.com/2010/09/macros-and-type-hints-metadata-and.html"
  [type-key]
  (let [coll (with-meta (gensym) {:tag (-> type-key name (str "s"))})]
   `(extend-protocol CollCount (get ~arr-types ~type-key)
      (count+ [~coll]
        (alength ~coll)))))
      
(defmacro extend-coll-count-to-all-arr! []
  (reduce-kv
    (fn [ret type type-class]
      ;(println type)
      (eval `(extend-coll-count-for-type ~type)))
    nil arr-types))
      
(extend-coll-count-to-all-arr!)
;___________________________________________________________________________________________________________________________________
;=================================================={         RETRIEVAL        }=====================================================
;=================================================={     get, first, rest     }=====================================================
(       defprotocol
                    CollRetrieve
  (getr+    [coll a] [coll a b] "Get range")
  (get+     [coll n] [coll n if-not-found])
  (first+   [coll])
  (second+  [coll])
  (rest+    [coll])
  (butlast+ [coll])
  (last+    [coll])) 

     
(defmacro extend-coll-retrieve-for-type
  [type-key]
  (let [coll (with-meta (gensym) {:tag (-> type-key name (str "s"))})]
   `(extend-protocol CollRetrieve (get ~arr-types ~type-key)
      (get+
        ([~coll n#]  (get+ ~coll n# nil))
        ([~coll n# if-not-found#]
          (try+ (aget ~coll n#)
            (catch ArrayIndexOutOfBoundsException e# if-not-found#))))
      (first+  [~coll]  (aget ~coll 0))
      (second+ [~coll]  (aget ~coll 1))
      (last+   [~coll]  (aget ~coll (-> ~coll alength dec))))))

     
(defmacro extend-coll-retrieve-to-all-arr! []
  (reduce-kv
    (fn [ret type type-class]
      ;(println type)
      (eval `(extend-coll-retrieve-for-type ~type)))
    nil arr-types))

     
(extend-coll-retrieve-to-all-arr!)

; take-up-to (combination of getr and index-of + 1)

      ; TODO: port to cljs for Arrays and ArrayLists
(extend-protocol CollRetrieve ArrayList
  (getr+ [^ArrayList coll a b] (.subList ^ArrayList coll a b))
  (get+
    ([^ArrayList coll n]  (get+ ^ArrayList coll n nil))
    ([^ArrayList coll n if-not-found]
      (try+ (.get ^ArrayList coll n)
        (catch ArrayIndexOutOfBoundsException e# if-not-found))))
  (first+  [^ArrayList coll]  (get+ ^ArrayList coll 0))
  (second+ [^ArrayList coll]  (get+ ^ArrayList coll 1))
  (last+   [^ArrayList coll]  (get+ ^ArrayList coll (-> coll count+ dec))))
; extend first+ to use /first/ when the object is unknown.
; first+ with non-collection items (java.util.Collection) will return itself.

(extend-protocol CollRetrieve
  Keyword
    (rest+ [k]
      (rest+ (name k)))
        String              
    (first+   [coll]
      (try (subs coll 0 1)
              (catch StringIndexOutOfBoundsException _ nil)))
    (second+  [coll]
      (try (subs coll 1 2)
              (catch StringIndexOutOfBoundsException _() nil)))
    (rest+    [coll]
      (try (subs coll 1 (count+ coll))
              (catch StringIndexOutOfBoundsException _ nil)))
    (butlast+ [coll] (subs coll 0 (-> coll count+ dec)))
    (last+    [coll] (subs coll   (-> coll count+ dec)))
    (getr+    [coll a b] (str/subs+ coll a (inc (- b a))))) ; now inclusive range with |inc|; this could break lots of things

     
(extend-protocol-for-all CollRetrieve
  [clojure.core.protocols.CollReduce
   ;#+cljs cljs.core/IReduce ; this is why...
   quantum.core.reducers.Folder]
    (getr+    [coll a b] (->> coll (take+ b) (drop+ a)))
    (first+   [coll]     (take+ 1 coll))
    (rest+    [coll]     (drop+ 1 coll))
    (butlast+ [coll]     (drop-last+ 1 coll))
    (last+    [coll]     (take-last+ 1 coll)))

     
(extend-protocol CollRetrieve
  clojure.lang.IPersistentVector
    (getr+    ([coll a b] (subvec+ coll a b))
              ([coll a]   (subvec+ coll a (-> coll count))))
    (get+ 
      ([coll n] (get+ coll n nil))
      ([coll n if-not-found] (get coll n if-not-found)))
    (first+   [coll] (get coll 0))
    (second+  [coll] (get coll 1))
    (rest+    [coll] (subvec+ coll 1 (count coll)))
    (butlast+ [coll] (whenf coll nempty? pop))
    (last+    [coll] (peek coll))
  clojure.lang.IPersistentMap
    (get+ 
      ([coll n]              (get+ coll n nil))
      ([coll n if-not-found] (get coll n if-not-found)))
  clojure.lang.IPersistentCollection
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

; ===== REDUCERS =====

      
                             
                 
                                                              
                                              
                                              
                                                    
                                                    
     
; _____________________________________________________________________________________
; ====================================== VECTOR =======================================
; `````````````````````````````````````````````````````````````````````````````````````
      
                             
                            
                                             
                                                            
          
                                  
                                                        
                                  
                                  
                                                   
                                              
                                  

      
                             
                           
                                             
                                                            
          
                                  
                                                        
                                  
                                  
                                                   
                                              
                                  

; _____________________________________________________________________________________
; ======================================== MAP ========================================
; `````````````````````````````````````````````````````````````````````````````````````
      
                             
                    
          
                                               
                                                         

      
                             
                   
          
                                               
                                                         

      
                             
                   
          
                                               
                                                         

      
                             
                  
          
                                               
                                                         

      
                             
                   
          
                                               
                                                         

; _____________________________________________________________________________________
; ==================================== COLLECTION =====================================
; `````````````````````````````````````````````````````````````````````````````````````
      
                             
                
          
                                      
                                                
                                                                              
                                  
                                 
                                    
                                 
         
                                  
                                                         

      
                             
                     
                          

      
                             
                           
          
                                      
                                                
                                                                              
                                  
                                 
                                    
                                 
         
                                  
                                                         

      
                             
                   
          
                                      
                                                
                                                                              
                                  
                                 
                                    
                                 
         
                                  
                                                         

      
                             
                  
          
                                      
                                                
                                                                              
                                  
                                 
                                    
                                 
         
                                  
                                                         

      
                             
                  
          
                                      
                                                
                                                                              
                                  
                                 
                                    
                                 
         
                                  
                                                         

      
                             
                      
          
                                      
                                                
                                                                              
                                  
                                 
                                    
                                 
         
                                  
                                                         

      
                             
                      
          
                                      
                                                
                                                                              
                                  
                                 
                                    
                                 
         
                                  
                                                         

      
                             
                             
          
                                      
                                                
                                                                              
                                  
                                 
                                    
                                 
         
                                  
                                                         

      
                             
                            
          
                                      
                                                
                                                                              
                                  
                                 
                                    
                                 
         
                                  
                                                         

      
                             
                             
          
                                      
                                                
                                                                              
                                  
                                 
                                    
                                 
         
                                  
                                                         

      
                             
                     
          
                                      
                                                
                                                                              
                                  
                                 
                                    
                                 
         
                                  
                                                         

(extend-protocol CollRetrieve
  ; sorted-map or sorted-set getr+ like subseq
         Object ; "default"
                
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
(       defprotocol
                   
  CollSearch
    (index-of+      [coll elem])
    (last-index-of+ [coll elem]))

     
(defmacro extend-coll-search-for-type
  [type-key]
  (let [type# (-> coll-search-types (get type-key) name-from-class)
        coll (with-meta (gensym)
               {:tag type#})
        elem (with-meta (gensym)
               {:tag (if (= type# 'java.lang.String)
                         'String
                         'Object)})]
   `(extend-protocol CollSearch (get ~coll-search-types ~type-key)
      (index-of+      [~coll ~elem] (.indexOf     ~coll ~elem))
      (last-index-of+ [~coll ~elem] (.lastIndexOf ~coll ~elem)))))

     
(defn extend-coll-search-to-all-types! []
  (reduce-kv
    (fn [ret type type-class]
      (eval `(extend-coll-search-for-type ~type)))
    nil coll-search-types))

      (extend-coll-search-to-all-types!)

      
                           
                            
                                                         
                                                         
                           
                                                         
                                                         
        
                                                         
                                                          

(defn third [coll] (-> coll rest+ rest+ first+))
; If the array is not sorted:
; java.util.Arrays.asList(theArray).indexOf(o)
; If the array is sorted, you can make use of a binary search for performance:
; java.util.Arrays.binarySearch(theArray, o)

(defmacro doseqi
  "Loops over a set of values, binding index-sym to the 0-based index
   of each value"
  {:todo ["Implement in terms of |reduce|"]}
  ([[val-sym values index-sym] & code]
  `(loop [vals#      (seq ~values) 
          ~index-sym (long 0)]
     (if vals#
         (let [~val-sym (first vals#)]
               ~@code
               (recur (next vals#) (inc ~index-sym)))
         nil))))


; (defn conjl [coll elem]
;   (condf coll
;     list?   (f*n cons elem)
;     vector? (partial into+ (vector elem))
;     coll?   (partial into+ (vector elem))))
;;;;;;;;;;;; This file autogenerated from src/cljx/quantum/core/collections/core.cljx
