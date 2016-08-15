(ns quantum.test.core.collections.map-filter
  (:require [quantum.core.collections.map-filter :as ns]))

(defn test:each [f coll])

; ============================ MAP ============================ ;

(defn test:map-keys+ [f coll])
(defn test:map-vals+ [f coll])

; ============================ FILTER ============================ ;

(defn test:ffilter
   [filter-fn coll])

(defn test:ffilter+
  [pred coll])

(defn test:ffilteri
  [pred coll])

(defn test:filteri
  [pred coll])

(defn test:last-filteri [pred coll])

;___________________________________________________________________________________________________________________________________
;=================================================={  FILTER + REMOVE + KEEP  }=====================================================
;=================================================={                          }=====================================================
(defn test:filter-keys+ [pred coll])
(defn test:remove-keys+ [pred coll])
(defn test:filter-vals+ [pred coll])
(defn test:remove-vals+ [pred coll])

(defn test:filter! [coll pred])
(defn test:remove! [coll pred])


(defn ldistinct-by
  [f coll])

#?(:clj
(defn test:ldistinct-by-java
  [f coll]))