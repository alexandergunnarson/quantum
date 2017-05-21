(ns quantum.test.core.collections.sociative
  (:require [quantum.core.collections.sociative :as ns]))

;_._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._
;=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*{        ASSOCIATIVE       }=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=
;=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*{                          }=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=
;___________________________________________________________________________________________________________________________________
;=================================================={          ASSOC           }=====================================================
;=================================================={ update(-in), assoc(-in)  }=====================================================
(defn- test:extend-coll-to
  [coll-0 k])

(defn test:assoc+
  ([coll-0 k v])
  ([coll-0 k v & kvs-0]))

(defn test:assoc-if
  ([m pred k v] )
  ([m pred k v & kvs]))

(defn test:assoc-when-none 
  [m & args])

(defn test:update+
  ([coll k f])
  ([coll k f args]))

(defn test:update-when
  [m k pred f])

(defn test:updates+
  ([coll & kfs]))

(defn test:update-key+ [f])

(defn test:update-val+ [f])

(defn test:mapmux
  ([kv])
  ([k v]))

(defn test:record->map [rec])

;--------------------------------------------------{        UPDATE-IN         }-----------------------------------------------------
(defn test:update-in!
  [m [k & ks] f & args])

; perhaps make a version of update-in : update :: assoc-in : assoc ?

(defn test:update-in+
  [coll-0 [k0 & keys-0] v0])

;--------------------------------------------------{         ASSOC-IN         }-----------------------------------------------------
(defn test:assoc-in+
  [coll ks v])

(defn test:assoc-in!
  ([m ks obj]))

(defn test:assocs-in+
  [coll & kvs])

;___________________________________________________________________________________________________________________________________
;=================================================={          DISSOC          }=====================================================
;=================================================={                          }=====================================================
(defn test:dissoc+
  ([coll key-0])
  ([coll key-0 & keys-0]))

(defn test:dissocs+ [coll & ks])

(defn test:dissoc-if+ [coll pred k]) ; make dissoc-ifs+
 
(defn test:dissoc++ [coll k])

(defn test:dissoc-in+
  [m ks])

(defn test:updates-in+
  [coll & kfs])

(defn test:re-assoc+ [coll k-0 k-f])

(defn test:re-assocs+ [coll & kfs])

(defn test:assoc-with
  [m f k v])