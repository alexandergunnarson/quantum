(ns quantum.apis.intuit.mint
  #_(:require-quantum [:lib web http auth url])
  #_(:require [clj-http.cookies :as cook]
            [quantum.financial.core :as fin]))

#_(defn login! [driver]
  (let [creds (auth/auth-keys :intuit)
        _ (.get driver "https://wwws.mint.com/login.event?task=L")
        username-elem (web/find-element driver (By/id "form-login-username") 4 1000)
        password-elem (web/find-element driver (By/id "form-login-password"))
        login-btn     (web/find-element driver (By/id "submit"))]
    (web/send-keys! username-elem (:username creds))
    (web/send-keys! password-elem (:password creds))
    (web/click-load! login-btn)))

#_(defn transactions-raw []
  (let [driver (PhantomJSDriver.)]
    (try
      (let [_ (login! driver)
            url "https://wwws.mint.com/transactionDownload.event"
            resp (http/request!
                   {:method :get
                    :url url
                    :headers {"Cookie" (->> (web/get-cookies driver) cook/encode-cookies)}
                     #_:query-params #_{"queryNew" "" "offset" "0" "filterType" "cash" "comparableType" "8"}})
            _ (with-throw (-> resp :opts :url (= url))
                (>ex-info :err/url "Didn't get to the right url" url))
            csv (:body resp)]
        csv)
      (finally (.quit driver)))))

#_(defn parse-transactions
    {:example `{:account-remap
                 {"Bank 1 Savings" :bank1/savings
                  "Checking"       :bank2/checking
                  "BANK3 CHECKN"   :bank3/checking
                  "Mastercard K4"  :bank2/credit}}}
  ([csv] (parse-transactions csv nil))
  ([csv {:as opts :keys [account-remap]}]
    (let [account-remapper
           (if account-remap
               (fn->> (map+ (fn1 update :account-name
                              #(or (get account-remap %) %)) ))
               identity)]
      (->> csv
           (<- (csv/parse #{:as-map? :reducer?}))
           (map+ (fn1 update :amount               (fn-> str/val rationalize)))
           (map+ (fn1 update :transaction-type     keyword))
           (map+ (fn1 update :date                 (fn-> (time/parse "M/dd/yyyy")
                                                         time/->instant)))
           (map+ (fn1 update :original-description (fn->> (url/decode :xml))))
           (map+ fin/debit-credit->num)
           account-remapper))))

