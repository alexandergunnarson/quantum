(ns apis.amazon.aws.core
  (:require-quantum [:lib http auth])
  (:require [quantum.http.utils :as httpu]
    [quantum.deploy.amazon :as deploy]
    [quantum.core.reflect :refer [obj->map]])
  (:import com.amazonaws.services.ec2.AmazonEC2Client
           com.amazonaws.auth.AWSCredentials
           [com.amazonaws.regions Region Regions]
           com.amazonaws.services.ec2.model.GetConsoleOutputRequest))

(defonce client
  (doto
    (AmazonEC2Client.
      (reify AWSCredentials
        (^String getAWSAccessKeyId [this] @deploy/aws-id)
        (^String getAWSSecretKey   [this] @deploy/aws-secret)))
    (.setRegion (Region/getRegion Regions/US_WEST_2))))

(defn console-output
  ([] (console-output @deploy/instance-id))
  ([instance-id]
    (-> ^AmazonEC2Client client
        (.getConsoleOutput (GetConsoleOutputRequest. instance-id))
        .getDecodedOutput)))

(defn describe-instances []
  (->> ^AmazonEC2Client client
       (.describeInstances)
       obj->map
       :reservations
       (map+ obj->map)
       (map+ (fn-> (dissoc :instances)))
       redv))
