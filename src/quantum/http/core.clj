(ns quantum.http.core)
(require '[quantum.core.ns  :as ns :refer :all])
(ns/require-all *ns* :lib :clj)
(require '[org.httpkit.client :as http])

(def ^:dynamic *max-tries-http* 3)

(defrecord HTTPLogEntry [^APersistentVector tries])
;___________________________________________________________________________________________________________________________________
;================================================={              LOG              }=================================================
;================================================={                               }=================================================
(defn log-entry-write!
  ^{:usage "log: {:request  time
                  :tries    [{:response time :status 401}]
                  :response time}"}
  [^Atom log log-type ^Number tries & [status]]
  (let [to-conj
          (if (or (and (= tries 0) (= log-type :request)) ; initial request
                  (= status "OK"))                        ; or final response 
              [[log-type] (time-loc/local-now)]
              (concat (when (= tries 0)
                        [[:tries tries :request]
                        (:request @log)])
                [[:tries tries log-type] (time-loc/local-now)
                 [:tries tries :status]  status]))]
    (reset! log (apply assocs-in+ @log to-conj))))
;___________________________________________________________________________________________________________________________________
;================================================={     PROCESS HTTP REQUEST      }=================================================
;================================================={                               }=================================================
(defn proc-request
  "'Safe' because it handles various HTTP errors (401, 403, 500, etc.),
   and limits retries at |http-lib/*max-tries-http*| (which defaults at 3)."
   {:todo  ["EOFException SSL peer shut down incorrectly  sun.security.ssl.InputRecord.read
             INFO: I/O exception (java.net.SocketException) caught when connecting to
                   {s}->https://www.googleapis.com: Connection reset"]
    :usage "(proc-request 0 nil {...} (atom {:tries []}))"}
  [^Integer try-n status-n request-n ^Atom log-entry ^AFunction handle-http-error-fn]
  (if (= try-n *max-tries-http*)
      (throw+ {:message (str "HTTP exception, status " status-n ". Maximum tries (3) exceeded.")})
      (condf
        (let [request-write!  (log-entry-write! log-entry :request  try-n)
              response        @(http/request request-n)
              response-write! (log-entry-write! log-entry :response try-n "OK")]  ; this is not executed if an exception happens
        response)
        (compr :status (f*n splice-or = 401 403 500))
        #(handle-http-error-fn try-n (:status %) request-n log-entry)
        :else identity)))