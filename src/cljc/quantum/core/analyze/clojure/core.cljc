(ns quantum.core.analyze.clojure.core
  (:refer-clojure :exclude [name])
  (:require-quantum [:lib])
  (:require [quantum.core.analyze.clojure.predicates :refer :all]))

; TODO COMBINE THESE TWO VIA "UPDATE-N GET"
(def conditional-branches
  (condf*n
    (fn-or if-statement? cond-statement?)
      (fn->> rest
             (partition-all 2)
             (map (if*n (fn-> count (= 2))
                    second
                    first))
             doall)
    when-statement?
      last
    (constantly nil)))
; TODO COMBINE THESE TWO VIA "UPDATE-N GET"
(defn map-conditional-branches [f x]
  (condf x
    (fn-or if-statement? cond-statement?)
      (fn->> rest
             (partition-all 2)
             (map (if*n (fn-> count (= 2))
                    (f*n update-nth 1 f)
                    (f*n update-nth 0 f)))
             (cons (list (first x)))
             (apply concat))
    when-statement?
      (f*n update-last f)
    identity))
