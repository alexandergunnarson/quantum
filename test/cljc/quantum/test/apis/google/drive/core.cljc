(ns quantum.test.apis.google.drive.core
  (:require [quantum.apis.google.drive.core :as ns]))
 
;___________________________________________________________________________________________________________________________________
;================================================={       AUX REQUEST-MAKING      }=================================================
;================================================={                               }=================================================
#_(defn test:method+url-fn
  [func id to method])

#_(defn test:query-params-fn
  [^Map params])

#_(defn test:make-request
  [email ^Key func ^String id to method ^Map params req])

;___________________________________________________________________________________________________________________________________
;================================================={     PROCESS HTTP REQUEST      }=================================================
;================================================={                               }=================================================
#_(defn test:drive
  [func & {:keys [id to method params req raw?] :as args :or {raw? false}}])

#_(defn test:get-children
  [^String id])

#_(defn test:eval-drive-page [email])