(ns quantum.apis.amazon.aws.core
          (:require
            [quantum.core.collections
              :refer        [map+ #?@(:clj [join])]
              :refer-macros [join]]
            [quantum.deploy.amazon :as deploy]
            [quantum.core.validate :as v
              :refer [validate]]
            [quantum.core.data.validated
              :refer [def-validated def-validated-map]]
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


; From http://docs.aws.amazon.com/ElasticMapReduce/latest/DeveloperGuide/emr-plan-region.html
; TODO get more effectively
(def aws-regions #{:us-west-1 :us-west-2 :us-east-1 :us-gov-west-1
                   :eu-west-1 :eu-central-1
                   :ap-southeast-1 :ap-southeast-2 :ap-northeast-1 :ap-northeast-2
                   :cn-north-1
                   :sa-east-1})

(def-validated-map ^:db? ^:sensitive? ^:no-history? credential>aws
  :req-un [(def :this/service
             :req-un [(def :this/name (v/and :db/keyword #{:cloud-drive}))])] ; TODO others
  :opt-un [(def :this/region (v/and :db/keyword aws-regions))
           :oauth2-keys])
