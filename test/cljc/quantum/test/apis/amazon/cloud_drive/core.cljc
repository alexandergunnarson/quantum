(ns quantum.test.apis.amazon.cloud-drive.core
  (:require [quantum.apis.amazon.cloud-drive.core :refer :all]))

(defn ^:cljs-async test:request!
  ([k url-type] )
  ([k url-type
    {:keys [append method query-params]
     :or   {method :get
            query-params {}}}])

(defn ^:cljs-async test:used-gb [])

(defn test:download! [id])

#?(:clj
(defn test:download-to-file!
  [id file]))

(defn ^:cljs-async test:root-folder [])

(defn test:trashed-items [])

(defn ^:cljs-async test:children
  [id])

(defn test:meta [id])