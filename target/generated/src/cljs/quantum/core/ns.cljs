(ns quantum.core.ns
                                       
  (:require
                                  
           [clojure.core.rrb-vector]
           [cljs.core :refer [Keyword]]
                                 )
       
                                                
       
              )

                    
                               
                                 
               
                                                     

                   
                              
                                 
            
                                    
                   
                               
                 
                             

                
                        
                                              
                                                              
                                        
                           
     
                             
            
                                    
                       
                 
 
                
                      
                                                      
                                              
                                                              
                                        
                           
                  
          
                      
                            
                                
                                                                                
                          
                  
 
                
                          
                                          
 
                              
                  
                                                       
                             
                                 
                                   
               
                              
 
                
               
                                                        
                             
                              
                                   
               
                           
 
      
               
                                               
                                       
      
                
             
                                
                    
                          
 
      
                
                                                                                 
                                                  
                                       
              
                     
                   
             
                    
                                              
                     
                     
                                       
 
 ; Trying to put the reader macros inside a macro didn't work...
      
                   
                                                                        
                                                                      
                                                                           
                                     
                                     
            
                                        
 
      
               
                                                                            
                          
                   
                                       
            
                    
                                                    
                           

     
          
                                                               
                     
                                
                                  
                
                     
                                                          

     
                  
                                                             
                                            
                                        
                                  
                
                     
                                                        
                                                                     

     
                
                                  
          
                     
                                                                     
                            

     
                
                                  
           
                                  

                                    
              
                                  
    
                                   

; Just to be able to synthesize class-name aliases...

;(def       Nil       nil)
;#+clj (def Fn        clojure.lang.Fn)
; (def       Key       #+clj clojure.lang.Keyword              #+cljs cljs.core.Keyword             )
; (def       Num       #+clj java.lang.Number                  #+cljs js/Number                     )
; (def       ExactNum  #+clj clojure.lang.Ratio                #+cljs js/Number                     )
; (def       Int       #+clj java.lang.Integer                 #+cljs js/Number                     )
; (def       Decimal   #+clj java.lang.Double                  #+cljs js/Number                     )
; (def       Set       #+clj clojure.lang.APersistentSet       #+cljs cljs.core.PersistentHashSet   )
; (def       Bool      #+clj Boolean                           #+cljs js/Boolean                    )
; (def       ArrList   #+clj java.util.ArrayList               #+cljs cljs.core.ArrayList           )
; (def       TreeMap   #+clj clojure.lang.PersistentTreeMap    #+cljs cljs.core.PersistentTreeMap   )
; (def       LSeq      #+clj clojure.lang.LazySeq              #+cljs cljs.core.LazySeq             )
; (def       Vec       #+clj clojure.lang.APersistentVector    #+cljs cljs.core.PersistentVector    ) ; Conflicts with clojure.core/->Vec
; (def       MEntry    #+clj clojure.lang.MapEntry             #+cljs Vec                           )
; (def       Regex     #+clj java.util.regex.Pattern           #+cljs js/RegExp                     )
; (def       Editable  #+clj clojure.lang.IEditableCollection  #+cljs cljs.core.IEditableCollection )
; (def       Transient #+clj clojure.lang.ITransientCollection #+cljs cljs.core.ITransientCollection)
; (def       Queue     #+clj clojure.lang.PersistentQueue      #+cljs cljs.core.PersistentQueue     )
; (def       Map       #+clj java.util.Map                     #+cljs cljs.core.IMap                )
; (def       Seq       #+clj clojure.lang.ISeq                 #+cljs cljs.core.ISeq)

(defrecord Nil       [])
(defrecord Key       [])
(defrecord Num       [])
(defrecord ExactNum  [])
(defrecord Int       [])
(defrecord Decimal   [])
(defrecord Set       [])
(defrecord Bool      [])
(defrecord ArrList   [])
(defrecord TreeMap   [])
(defrecord LSeq      [])
       (defrecord Vec [])
(defrecord Regex     [])
(defrecord Editable  [])
(defrecord Transient [])
(defrecord Queue     [])
(defrecord Map       [])
(defrecord Seq       [])
(defrecord Record    [])
                                              
       (defrecord JSObj                    [])
       (defrecord Exception                [^String msg])
       (defrecord IllegalArgumentException [^String msg])

     
                                            
                         
                     
                                         

     
                                      
                         
       
                                            
              
                                        
                                      
           
             
                         
                         
               
              
             
                                   
               
                
            
              
                                
                          
                  
               
              
                                    
                
                  
           
             
                      
                     
                   
                 
                    
                     
                          
                                              
                    
                                                                  
                          
                           
                   
                  
                                
                              
                               
                         
                               
                                            
                                            

               
                                      
                         
            
                                                                                 
                                             
                                                                                 
                                                                                 
                                                                                 
                                                                                 
                                                                                 
                                                                                 
                                                                                 
                                                                                 
                                                                                 
                                                                                 
                                                                                 
                                                                                   
                                                                                 
                                                                                 
                                                                                 
                                                                                 
                                                                                                           
                                                                                 
                                                                                 
                                                                                 
                                                                                 
                                                                                 
                                                                                 
                                                                                 
                                                                                 
                                                                                 
                                                                                 
                                                                                 
                                                                                 
                                                                                 
                                                                                 
                                                                                    

     
                                          
                         
                                     
           
                                                                                               
                                                                                                  
                                                                             
                                                            
                                                                  
                                          
                                                
                                                                
                                     
                                                                     
                                                                                    
                                                                                             
                                                                          
                                                                                                
                                              
                                                 
                                                    
                                                              
                             
                                                              
                                                              
                                                         

     
                                          
                         
                             
            
                                         
                  
                                     
                               

     
                                     
                         
                             
            
                                                   
                                                               
                                  
                                           
                     
                                    
                               

               
                 
                                                                                      
                                                             
                                
                                              
                             
                           
                   
            
                               
            
                               
             
                 
                                                 
                                                  
                
                                   
                
                                   
           
                              
              
                                   
                                                       
                                                          
                                
                                           
                                         

     
         
                                                                       
                        
                                                       
                                                              
                    
                                                                        
                                  
           
                         
                 
                           
                                                 
                                   
                                                 
                                                 
                                                 
                                                 
                                                 
                                                 
                                                 
                                                 
                                           
                                              
                                                 
                                                 
                                                 
                                                 
                                                 
                                                 
                                                 
                                                 
                                                 
                                                 
                                                 
                                                 
                                                 
                                                 
                                                 
                                                 
                                                 
                                                 
                                          
                                          
                                          
                                          
                                          
                                          
                                          
                                              
                                           
                                              

; find-doc, doc, and source are incl. in /user/ ns but not in any others
                                        
                                       
                                  


;;;;;;;;;;;; This file autogenerated from src/cljx/quantum/core/ns.cljx
