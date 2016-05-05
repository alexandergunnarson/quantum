(ns quantum.financial.core
           (:require [quantum.core.fn    :as fn
                       :refer [#?@(:clj [fn-> f*n])]]
                     [quantum.core.logic :as logic
                       :refer [#?@(:clj [whenf])]   ])
  #?(:cljs (:require-macros
                     [quantum.core.fn    :as fn
                       :refer [fn-> f*n]            ]
                     [quantum.core.logic :as logic
                       :refer [whenf]               ])))

(def debit-credit->num
  (fn-> (whenf (fn-> :transaction-type (= :debit))
            (f*n update :amount -))
          (dissoc :transaction-type)))