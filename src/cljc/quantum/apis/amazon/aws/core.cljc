(ns quantum.apis.amazon.aws.core
          (:require
            [quantum.core.collections
              :refer        [map+ #?@(:clj [join])]
              :refer-macros [join]]
            [quantum.deploy.amazon :as deploy]
    #?(:clj [quantum.core.reflect
              :refer [obj->map]]))
  #?(:clj (:import
            com.amazonaws.services.ec2.AmazonEC2Client
            com.amazonaws.auth.AWSCredentials
            [com.amazonaws.regions Region Regions]
            com.amazonaws.services.ec2.model.GetConsoleOutputRequest)))

#_(:clj
(defonce client
  (doto
    (AmazonEC2Client.
      (reify AWSCredentials
        (^String getAWSAccessKeyId [this] @deploy/aws-id)
        (^String getAWSSecretKey   [this] @deploy/aws-secret)))
    (.setRegion (Region/getRegion Regions/US_WEST_2)))))

#_(:clj
(defn console-output
  ([] (console-output @deploy/instance-id))
  ([instance-id]
    (-> ^AmazonEC2Client client
        (.getConsoleOutput (GetConsoleOutputRequest. instance-id))
        .getDecodedOutput))))

#_(:clj
(defn describe-instances []
  (->> ^AmazonEC2Client client
       (.describeInstances)
       obj->map
       :reservations
       (map+ obj->map)
       (map+ (fn-> (dissoc :instances)))
       join)))