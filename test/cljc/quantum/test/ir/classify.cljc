(ns quantum.test.ir.classify
  (:require [quantum.ir.classify :as ns]))

(defn test:C ([D]))

(defn test:V ([D]))

; =================================================
; ----------------------- N -----------------------
; =================================================

#?(:clj
(defmacro test:N*
  ([D])
  ([D arg & args])))

#?(:clj
(defmacro test:N [D & args]))

(defn test:N- [D])

(defn test:Nd:c
  ([D])
  ([D c]))

(defn test:Nt:c
  ([D])
  ([D c]))

(defn test:Nt:w+d
  ([d])
  ([w d]))

(defn test:Nd:w
  ([D] )
  ([D w])
  ([D ŵ _] ))

(defn test:Nd:c+w
  ([D])
  ([D c w])
  ([D c ŵ _] ))

(defn test:N:w+c
  ([D])
  ([D w c]))

; =================================================
; ------------------ PROBABILITY ------------------
; =================================================

(defn test:P:c
  ([t D c V]))

(defn test:P:w
  ([t D w]  )
  ([t D ŵ _]))

(defn test:P:c|w 
  ([t D c w]  )
  ([t D c ŵ _]))

(defn test:laplacian-smoothed-estimate
  [t D w c V])

(defn test:P:w|c
  ([t D w c V]))

(defn test:delta
  [w d])

(defn test:expi [x i])

(defn test:P:d'|c
  [t D d' c V])

(defn test:P:c|d'
  ([t D c d' V])
  ([t D c d' V denom?]))

(defn test:classifier-score+
  [t D d' V])

(defn test:classifier-scores
  [t D d' V])

(defn test:max-classifier-score
  ([t D d'])
  ([t D d' V]))

(defn test:multinomial-naive-bayes-classifier
  ([D d'])
  ([D d' V]))

(defn test:multiple-bernoulli-naive-bayes-classifier
  ([D d'])
  ([D d' V]))

; ================================================


(defn test:information-gain [t D w C V])

(defn test:all-information-gains [t D V])

(defn test:feature-selection [T M])

; ======== MNB EVALUATION ========

(defn test:label [D d V'])

(defn test:accuracy-measure [D D' V'])

(defn test:D-split [D])

