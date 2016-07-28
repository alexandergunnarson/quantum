(ns quantum.test.deploy.amazon
  (:require [quantum.deploy.amazon :refer :all]))

#_(defn test:default-account [])
#_(defn test:default-instance-name  [                 ])
#_(defn test:default-user           [                 ])
#_(defn test:get-instance-id        [& [instance-name]])
#_(defn test:get-server-region      [& [instance-name]])
#_(defn test:get-public-ip          [& [instance-name]])
#_(defn test:get-private-ip         [& [instance-name]])
#_(defn test:get-aws-id             [& [instance-name]])
#_(defn test:get-aws-secret         [& [instance-name]])
#_(defn test:get-ssh-keys-path      [& [instance-name]])
#_(defn test:get-user               [& [instance-name]])
#_(defn test:get-public-ip-dashed   [& [instance-name]])
#_(defn test:get-private-ip-dashed  [& [instance-name]])
#_(defn test:get-ssh-address        [& [instance-name]])

#_(defn test:prompt                 [& [instance-name]])

#_(defn test:launch-terminal!
  ([]              )
  ([instance-name] )
  ([instance-name print-streams?]))

#_(defn test:restart-terminal! [])

#_(defn test:with-terminal
  [& commands])

#_(defn test:command
  ([]         )
  ([command-n])
  ([command-n handler]))


#_(defn test:wait-until-prompt [timeout s])

#_(defn test:install-java! [])

#_(defn test:install-maven! [])

#_(defn test:install-leiningen! [])

#_(defn test:install-git! [])

#_(defn test:auth-repo! [repo-name])

#_(defn test:clone-repos! [repos])

#_(defn test:update-repo!
  [repo])

#_(defn test:discard-changes! [repo])

#_(defn test:init-server! [])

#_(defn test:restart! [])

#_(defn test:cleanse! [])

#_(defn test:install-tools! [])


#_(defn test:install-all! [])


#_(defn test:vnc-viewer-path [])

#_(defn test:launch-gui! [& [instance-name]])

#_(defn test:add-gui-user! [])

#_(defn test:init-gui! [])