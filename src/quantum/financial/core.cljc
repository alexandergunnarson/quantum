(ns quantum.financial.core
           (:require [quantum.core.fn    :as fn
                       :refer [#?@(:clj [fn-> fn1])]]
                     [quantum.core.logic :as logic
                       :refer [#?@(:clj [whenf])]   ])
  #?(:cljs (:require-macros
                     [quantum.core.fn    :as fn
                       :refer [fn-> fn1]            ]
                     [quantum.core.logic :as logic
                       :refer [whenf]               ])))

; TO EXPLORE
; - Tools for financial calculations including bonds, annuities, derivatives, options etc.
;   - Look at Mathematica
; ===================================

(def debit-credit->num
  (fn-> (whenf (fn-> :transaction-type (= :debit))
            (fn1 update :amount -))
          (dissoc :transaction-type)))
