(ns quantum.apis.financial.zions-bank.core
  (:require-quantum [:lib http auth web]))

(defn login! []
  (web/navigate! "https://www.zionsbank.com/")
  (let [username-field (web/find-element (By/id "publicCred1"))
        _ (web/send-keys! username-field (auth/datum ))
        password-field (web/find-element (By/id "privateCred1"))
        _ (web/send-keys! password-field (auth/datum :bank :zions :password))
        login-btn (web/find-element (By/xpath "//div[@class='go-btn submit']"))]
    (web/click-load! login-btn))

  (if (-> (web/driver) (.getPageSource)
          (containsv? "For your safety and protection your online banking session has ended"))
      (let [[base params] (-> (.getCurrentUrl (web/driver)) (str/split #"\?"))
            logoff-url
              (str "https://banking.zionsbank.com/" "olb/retail/exit" "?" params)]
        (web/navigate! logoff-url)
        (login!))))

(defn answer-challenge-question! []
  (let [answer-field (web/find-element (By/id "answerTextField"))
        _ (web/send-keys! answer-field "MY CHALLENGE QUESTION")
        continue-btn (web/find-element (By/id "continue"))]
    (web/click-load! continue-btn)))

(defn logout! []
  (web/navigate! "https://banking.zionsbank.com/olb/retail/exit"))

(defn+ ^:suspendable initiate-download! []
  (let [accounts-tab (web/find-element (By/xpath "//span[.='Accounts']"))
        _ (web/click! accounts-tab)
        _ (async/sleep 1000)
        update-balances-btn (web/find-element (By/xpath "//input[@alt='Update Balances']"))
        _ (web/click! update-balances-btn)
        _ (async/sleep 1000)
        download-btn (web/find-element (By/xpath "//input[@name='Download']"))
        _ (web/click! download-btn)
        _ (async/sleep 1000)
        select-all-checkbox (web/find-element (By/id "selectAll"))
        _ (web/click! select-all-checkbox)
        since-date-radio-btn (web/find-element (By/xpath "//input[@type='radio' and @value='com.s1.common.download.user.spanByDates']"))
        _ (web/click! since-date-radio-btn)
        begin-date-field (web/find-element (By/xpath "//input[@name='beginDate']"))
        _ (web/click! begin-date-field)
        _ (web/send-keys! begin-date-field "01/01/2000")
        end-date-field (web/find-element (By/xpath "//input[@name='endDate']"))
        _ (web/click! end-date-field)
        _ (web/send-keys! end-date-field "09/18/2015")
        _ (async/sleep 1000)
        download-initiate-btn (web/find-element (By/xpath "//input[@value='Download Initiate']"))
        _ (web/click-load! download-initiate-btn)]
        ))



#_(initiate-download!)

#_(.executePhantomJS (web/driver)
  (str "var sendAJAX = function (url, method, data, async, settings) {
            var xhr = new XMLHttpRequest(),
                dataString = '',
                dataList = [];
            method = method && method.toUpperCase() || 'GET';
            var contentType = settings && settings.contentType || 'application/x-www-form-urlencoded';
            xhr.open(method, url, !!async);
            if (settings && settings.overrideMimeType) {
                xhr.overrideMimeType(settings.overrideMimeType);
            }
            if (method === 'POST') {
                if (typeof data === 'object') {
                    for (var k in data) {
                        dataList.push(encodeURIComponent(k) + '=' + encodeURIComponent(data[k].toString()));
                    }
                    dataString = dataList.join('&');
                } else if (typeof data === 'string') {
                    dataString = data;
                }
                xhr.setRequestHeader('Content-Type', contentType);
            }
            xhr.send(method === 'POST' ? dataString : null);
            return xhr.responseText;
        };

        this.onResourceReceived = function(status) {
          console.log('onResourceReceived(' + status.contentType + ' | ' + status.url + ' | ' + JSON.stringify(status.headers) +  ')');
          try {
            console.log('Attempting to download file ' + file);
            var fs = require('fs');

            // TODO this isn't right yet...
            var retrieved = sendAJAX(status.url, 'GET', data, false, {
                                overrideMimeType: 'text/plain; charset=x-user-defined'
                            });

            fs.write('/Users/alexandergunnarson/Downloads/jsDownloads', retrieved, 'wb');
          } catch (e) {
              this.echo(e);
          }
        };
 
        // this.onResourceRequested = function(req, networkReq) {
        //   console.log('Request (#' + req.id + '): ' + JSON.stringify(req));
        // };

        return true;")
    (object-array 0))

#_(def complete-download-btn (web/find-element (By/xpath "//input[@value='Download' and @name='completeTransfer']")))
#_(web/click! complete-download-btn)

#_(http/request! {:method :get
                :url  "https://banking.zionsbank.com/olb/retail/protected/account/transaction/download/file"
                :headers {"Cookie" (->> (web/get-cookies (web/driver)) clj-http.cookies/encode-cookies)}
                :query-params {"OWASP_CSRFTOKEN" "[MY_TOKEN]"}})