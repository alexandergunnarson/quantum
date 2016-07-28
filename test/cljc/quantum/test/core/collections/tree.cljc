(ns quantum.test.core.collections.tree
  (:require [quantum.core.collections.tree :refer :all]))
;___________________________________________________________________________________________________________________________________
;=================================================={     TREE STRUCTURES      }=====================================================
;=================================================={                          }=====================================================
(defn test:walk2 [x f])

(defn test:walk
  [inner outer form])

(defn test:postwalk
  [f form])

(defn test:prewalk
  [f form])

(defn test:prewalk-replace
  [smap form])

(defn test:postwalk-replace
  [smap form])

(defn test:tree-filter
  [pred tree])

#?(:clj (defn test:prewalk-find [pred x]))
  
; ===== Transform nested maps =====

(defn test:apply-to-keys
  ([m])
  ([m f]))

(defn test:keywordize-keys
  [x])

(defn test:keywordify-keys
  [x])

(defn test:stringify-keys
  [x])