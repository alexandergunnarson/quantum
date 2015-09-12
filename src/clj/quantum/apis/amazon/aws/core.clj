(ns apis.amazon.aws.core
  (:require-quantum [:lib http auth])
  (:require [quantum.http.utils :as httpu]
    [quantum.deploy.amazon :as deploy]
    [quantum.core.reflect :refer [obj->map]])
  (:import com.amazonaws.services.ec2.AmazonEC2Client
           com.amazonaws.auth.AWSCredentials
           [com.amazonaws.regions Region Regions]
           com.amazonaws.services.ec2.model.GetConsoleOutputRequest))

(def aws-id-     (auth/datum :amazon :ec2 @deploy/instance-name :access-key :id))
(def aws-secret- (auth/datum :amazon :ec2 @deploy/instance-name :access-key :secret))

(defonce client
  (doto
    (AmazonEC2Client.
      (reify AWSCredentials
        (^String getAWSAccessKeyId [this] aws-id-)
        (^String getAWSSecretKey   [this] aws-secret-)))
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

(defn add-gui-user! []
  ; sudo useradd -m awsgui
  ; sudo passwd awsgui
  ; ... (type password twice)
  ; sudo usermod -aG admin awsgui (|sudo groupadd admin| if necessary)
  ; sudo vim /etc/ssh/sshd_config (edit line "PasswordAuthentication" to yes)
  ; sudo /etc/init.d/sshd restart
  )
(defn init-gui!
  {:sources ["https://www.digitalocean.com/community/tutorials/how-to-install-and-configure-vnc-on-ubuntu-14-04"
             "http://www.realvnc.com/download/viewer/"]}
  []
  (add-gui-user!))

(defn init-server! []
  ; APPLICATIONS
    ; BROWSER
    ; sudo apt-get install chromium-browser
  )

(defn restart! []
  ; sudo reboot
  ; takes about 30 seconds
  ; Then try to connect again
  )


(defn vnc-viewer-path
  {:todo ["Make more programmatic"]}
  []
  "/Applications/VNC Viewer.app/Contents/MacOS/vncviewer")

(defn launch-gui! []
  
  ; SERVER-SIDE
  ; sudo service vncserver start

  ; CLIENT-SIDE
  ; The ssh tunneler required for security purposes
  (sh/run-process! "ssh"
    ["-L" "5901:localhost:5901" "-i" @deploy/ssh-keys-path (deploy/ssh-address) "-N"]
    {:id :ssh-vnc-tunnel :thread? true})
  ; Launch VNC Viewer
  (sh/run-process! vnc-viewer-path
    []
    {:id :vnc-viewer :thread? true})
  ; Open VNC viewer and navigate to localhost:5901
  ; It says the connection is not secure, but it is! 
)
