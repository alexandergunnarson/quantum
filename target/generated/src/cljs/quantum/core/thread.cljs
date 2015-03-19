(ns quantum.core.thread
                    )
              
                                                                   
                                
              
                                     
                                                  
                                     
                                                  
                                                  
                                                      
                                                  
                                                           
                                                          


;(def #^{:macro true} go      #'async/go) ; defalias fails with macros (does it though?)...
                                                   
                                        
;(defalias <!         async/<!)
                                     
;(defalias >!         async/>!)
                                     
                                      
;(defalias alts!      async/alts!)
                                        
                                                 

; ONLY IN CLJS
                   
                                                 
                                            
                                          
                             
                        

                                   ; {:thread1 :open :thread2 :closed :thread3 :close-req}

     
                              
                                    
       
                                                                      
         
                                                      
                                                                  
                                                           
                                                                
           
                                                                      
         

                                                                                     
; Why you want to manage your threads when doing network-related things:
; http://eng.climate.com/2014/02/25/claypoole-threadpool-tools-for-clojure/
     
                 
                                                           
                                  
                              
                   
             
               
                        
                                    
                                                         
                        
                                                           
                                    
                                                                                     
                      
                                  
                                 
                                  
                             
                   
                   
                                                                          
                                      
                                                                
                                                         
                         
                                                        
                                                                         
                     
                                                              
              

     
                                 
                    
                                             
                                                                         
                                
                                                    
                                                              
                               
                          
                                                                 
                                   
                                                                                   
               
                                                                        

     
                                                     
                             
                  
                                  
                    
                                                                                                
                        
                       
                         
                
              
                                  
                        
                                               
                                 
                                                             
                                 
                                                        
                                        

     
                                             
                                                              

     
                                                        
                             
                  
                                  
                    
                                                                                                
                        
                       
                         
                
              
                                  
                                                                      
                                
                                          
                                               
                                                                           
                                     
                                                       
                                                                         
                                          

     
                                                
                                                                 
                       
                  
                                 
                                                       
                                                 
                                
                 
                            
                                        

     
                
                                                           
                                                                    
                                                
                                                                                  
        
                     
                      
                
                               
                                        
                                
                                       
                                         
                                               
                                    
                                                
                                                                         
                                                         
                               
                      
                         
                            
               
 
(comment
 
  ;; prints :foo, but not :bar
  (thread-or #(do (Thread/sleep 1000) (println :foo) true)
             #(do (Thread/sleep 3000) (println :bar)))
  ;;= true
 
  ;; prints :foo and :bar
  (thread-or #(do (Thread/sleep 1000) (println :foo))
             #(do (Thread/sleep 3000) (println :bar)))
  ;;= nil
 
  )
     
                 
                                                                     
                                                                 
                                  
                                                                                  
        
                      
                        
                       
                                   
                                            
                                    
                                                                   
                                                     
                                           
                                                 
                                                                         
                                                         
                                   
         
                       
                          
          
 
(comment
 
  (thread-and (constantly true) (constantly true))
  ;;= true
 
  (thread-and (constantly true) (constantly false))
  ;;= false
 
  (every? false?
          (repeatedly 100000
                      #(thread-and (constantly true) (constantly false))))
  ;;= true
 
  ;; prints :foo, but not :bar
  (thread-and #(do (Thread/sleep 1000) (println :foo))
              #(do (Thread/sleep 3000) (println :bar)))
 
  )




;;;;;;;;;;;; This file autogenerated from src/cljx/quantum/core/thread.cljx
