(ns quantum.financial.core
  (:require-quantum [:lib web http auth url]))

(def debit-credit->num
  (fn-> (whenf (fn-> :transaction-type (= :debit))
            (f*n update :amount core/-))
          (dissoc :transaction-type)))