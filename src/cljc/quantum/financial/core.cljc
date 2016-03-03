(ns quantum.financial.core
  (:require-quantum [:core fn logic]))

(def debit-credit->num
  (fn-> (whenf (fn-> :transaction-type (= :debit))
            (f*n update :amount core/-))
          (dissoc :transaction-type)))