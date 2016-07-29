(ns quantum.test.apis.facebook.driver
  (:require [quantum.apis.facebook.driver :as ns]))

#_(defn test:login! [driver username password]
  (.get driver login-url)
  (let [username-field (web/find-element driver (By/id "email"))
        _ (web/send-keys! username-field username)
        password-field (web/find-element driver (By/id "pass"))
        _ (web/send-keys! password-field password)
        login-button (web/find-element driver (By/id "loginbutton"))]
    (web/click-load! login-button)))

#_(defn test:profile [id]
  (http/request!
    {:url          (io/path home-url "profile.php")
     :query-params {:id id}}))