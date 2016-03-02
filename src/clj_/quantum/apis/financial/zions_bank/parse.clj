(ns quantum.apis.financial.zions-bank.parse
  (:require-quantum [:lib http auth web csv])
  (:require [quantum.financial.core :as fin]))

(defn parse-csv [^String data]
  (->> data
       (<- csv/parse #{:as-map? :reducer?})
       (map+ (f*n dissoc (keyword "")))
       (remove+ empty?)
       (map+ (f*n select-keys #{:amount :description :posted-date :payee :credit/debit}))
       (map+ (f*n coll/updates+
                  :amount (fn-> str/val num/exactly)
                  :credit/debit str/keywordize
                  :posted-date  (fn-> (time/parse "yyyy-MM-dd hh:mm:ss.S") time/->instant)))
       (map+ (f*n coll/re-assocs+
               :credit/debit :transaction-type
               :posted-date  :date
               :description  :original-description))
       (map+ fin/debit-credit->num)
       redv))