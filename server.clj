(ns quantum.web.server)

; You never know when this might be useful
(defn add-virtual-hosts!
  {:source "https://gist.github.com/austinhaas/3628228"}
  [^Server server host-specs]                                                                                                                                                                 
  (.stop server)                                                                                                                                                                                            
  (let [contexts (for [[host handler] host-specs]                                                                                                                                                           
                   (doto (ServletContextHandler. ServletContextHandler/SESSIONS)                                                                                                                            
                     (.setContextPath "/")                                                                                                                                                                  
                     (.setVirtualHosts (into-array [host]))                                                                                                                                                 
                     (.addServlet
                       (ServletHolder.
                          ^Servlet (ring.util.servlet/servlet handler))
                       "/")))                                                                                                                                 
        context-coll (ContextHandlerCollection.)]                                                                                                                                                              
    (.setHandlers context-coll (into-array contexts))                                                                                                                                                          
    (.setHandler server context-coll))                                                                                                                                                                         
  (.start server))    