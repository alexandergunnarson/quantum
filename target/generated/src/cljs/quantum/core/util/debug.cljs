(ns quantum.core.util.debug
       
           
                                                               
                                                               
                                                               
                                                               
                                                                )
 ; (:import mikera.cljutils.Error)
; (require '[taoensso.encore :as lib+ :refer
;   [throwable? exception?]])

      
           
                                              
                    
                                                        
                                                                       
                 
                
      
           
                                                

                                                                 
                                              
    
                                          ; perhaps non-namespace qualified is a bad idea

      
               
                                                                  
                                                                   
                                         
                                              
                                    
     
                       
                                 
                 
                                                    
                                                
           
                         
                   
; (defn break->>
;   "|break| for use with threading macros."
;   ([threading-macro-object]
;    (break)
;    threading-macro-object))

; (defmacro error
;   "Throws an error with the provided message(s). This is a macro in order to try and ensure the 
;    stack trace reports the error at the correct source line number."
;   ^{:attribution "mikera.cljutils.error"}
;   ([& vals]
;     `(throw (mikera.cljutils.Error. (str ~@vals)))))
; (defmacro error?
;   "Returns true if executing body throws an error, false otherwise."
;   ^{:attribution "mikera.cljutils.error"}
;   ([& body]
;     `(try 
;        ~@body
;        false
;        (catch Throwable t# 
;          true)))) 
; (defmacro TODO
;   "Throws a TODO error. This ia a useful macro as it is easy to search for in source code, while
;    also throwing an error at runtime if encountered."
;   ^{:attribution "mikera.cljutils.error"}
;   ([]
;     `(error "TODO: Not yet implemented")))
; (defmacro valid 
;   "Asserts that an expression is true, throws an error otherwise."
;   ^{:attribution "mikera.cljutils.error"}
;   ([body & msgs]
;     `(or ~body
;        (error ~@msgs))))
; (defmacro try-or 
;   "An exception-handling version of the 'or' macro.
;    Trys expressions in sequence until one produces a result that is neither false nor an exception.
;    Useful for providing a default value in the case of errors."
;   ^{:attribution "mikera.cljutils.error"}
;   ([exp & alternatives]
;      (if-let [as (seq alternatives)] 
;        `(or (try ~exp (catch Throwable t# (try-or ~@as))))
;        exp)))

; From flatland.useful.debug
      
                                          
                                    
                                                     
                                                                                           
                                                        
                                                               
                                          
                                                                                 
                                                                        
                                                                                     
                                                                                 
                                       
                                     
                                         
                                                   
                                                                 
                                   
             
                                                                       
                                                            
                                                     
                                         
         
                                    
                            
           
                                                                 
                                           
      
                                  
               
                                                                

      
                
                                                         
                                             
                   
                                                
; From flatland.useful.exception

      
                 
                                                          
                                            
             
                                                                        

      
                                 
; From flatland.useful.exception

      
                   
                                                                                                            
                                             
             
                                        
                                   
                                     

      
                                                         
                                  
                                                                  
                                                                                 
                                  
                  
                         
                                
                                      
                                                
                                    
                                              
; The following requires flatland.useful.fn:
; ; From flatland.useful.utils
; (defn fail
;   "Raise an exception. Takes an exception or a string with format args."
;   ([exception]
;      (throw (fix exception string? #(Exception. ^String %))))
;   ([string & args]
;      (fail (apply format string args))))
; ; From flatland.useful.utils
; (defmacro verify
;   "Raise exception unless test returns true."
;   [test & args]
;   `(when-not ~test
;      (fail ~@args)))

;;;;;;;;;;;; This file autogenerated from src/cljx/quantum/core/util/debug.cljx
