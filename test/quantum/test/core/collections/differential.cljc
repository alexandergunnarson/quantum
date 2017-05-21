(ns quantum.test.core.collections.differential
  (:require
    [quantum.core.collections.differential :as ns]
    [quantum.core.test
      :refer [deftest is testing]]))

;___________________________________________________________________________________________________________________________________
;=================================================={  DIFFERENTIAL OPERATIONS }=====================================================
;=================================================={     take, drop, split    }=====================================================

(defn test:split [ind coll-0])

(defn test:split-with-v+ [pred coll-0])

(defn test:while-matches
  [sub super pred f end-f])

(defn test:index-of-pred      [coll pred])

(defn test:last-index-of-pred [coll pred])

(deftest test:find-max
  (is (= [3 6] (ns/find-max [0 2 1 6 -1 5])))
  (is (= nil   (ns/find-max []))))

(deftest test:index-of-max
  (is (= 3     (ns/index-of-max [0 2 1 6 -1 5])))
  (is (= nil   (ns/index-of-max [])))

; ================================================ TAKE ================================================
; ============ TAKE-LEFT ============
(defn test:takel
  [i super])

(defn test:takel-fromi
  [i super])

(defn test:takel-from
  [sub super])

(defn test:takel-afteri
  [i super])

(defn test:takel-after
  [sub super])

(defn test:takel-while
  [pred super])

(defn test:takel-until
  [pred super])

(defn test:takel-until-inc
  ([sub super]))

(defn test:takel-while-matches
  [sub super])

(defn test:takel-until-matches
  [sub super])

; ============ TAKE-RIGHT ============

(defn test:taker
  [i super])

(defn test:takeri
  [i super])

(defn test:taker-untili
  [i super])

(defn taker-while
  [pred super])

(defn test:taker-until
  ([     sub  super])
  ([sub alt   super]))

(defn test:taker-after
  [sub super])

; ================================================ DROP ================================================

(defn test:dropl [n coll])

(defn test:dropl-while-matches
  [sub super])

(defn test:dropl-until-matches
  [sub super])

; DROPR

(defn dropr
  [n coll])

(defn test:ldropr
  [n coll])

(defn test:dropr-while [pred super])

(defn test:dropr-until
  [sub super])

(defn test:dropr-after
  ([sub super]))

(defn test:dropr-while-matches
  [sub super])

(defn test:dropr-until-matches
  [sub super])

(defn test:remove-surrounding
  [surr s])
