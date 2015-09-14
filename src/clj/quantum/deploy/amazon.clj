(ns quantum.deploy.amazon
  (:require-quantum [:lib auth http])
  (:require [quantum.core.reflect :refer [obj->map]]))

; TODO most of these functions are ambiguous as to whether they should go in
; quantum.deploy.amazon or quantum.apis.amazon.aws.core    

(def terminal     (atom nil))
(def output-chan  (atom nil))
(def line-handler (atom fn-nil))
(defn default-instance-name []
  (auth/datum :amazon :ec2 :default))
(defn default-user []
  (auth/datum :amazon :ec2 (default-instance-name) :users :default))
(defn get-instance-id [& [instance-name]]
  (auth/datum :amazon :ec2 (or instance-name (default-instance-name)) :id))
(defn get-server-region [& [instance-name]]
  (auth/datum :amazon :ec2 (or instance-name (default-instance-name)) :region))
(defn get-public-ip [& [instance-name]]
  (auth/datum :amazon :ec2 (or instance-name (default-instance-name)) :public-ip))
(defn get-user [& [user]]
  (or user (default-user)))

(def instance-name    (atom (default-instance-name)))
(def instance-id      (atom (get-instance-id)))
(def server-region    (atom (get-server-region)))
(def public-ip        (atom (get-public-ip)))
(defn public-ip-dashed []
  (str/replace @public-ip "." "-"))
(def user             (atom (get-user)))
(defn prompt [] (str "@ip-" (public-ip-dashed) ":"))

(def aws-id     (atom (auth/datum :amazon :ec2 @instance-name :access-key :id)))
(def aws-secret (atom (auth/datum :amazon :ec2 @instance-name :access-key :secret)))


(defn get-ssh-keys-path []
  (-> (auth/datum :amazon :ec2 @instance-name :ssh-keys-path) io/file-str))
(def ssh-keys-path (atom (get-ssh-keys-path)))

; |chmod 400 @ssh-keys-path| is necessary

(defn ssh-address []
  (str @user "@ec2-" (public-ip-dashed) "." @server-region ".compute.amazonaws.com"))

(defn launch-terminal!
  {:todo ["Make more configurable, to be a general SSH beyond just Amazon AWS"
          "This only is tested/works with Ubuntu. Make configurable/coverall"]}
  ([] (launch-terminal! true))
  ([print-streams?]
    (if (and @terminal (not (-> @terminal obj->map :has-exited)))
        (do (log/pr :warn "Terminal already running.")
            false)
        (do (sh/run-process! "ssh"
             ["-i" @ssh-keys-path
              (ssh-address)
              "-t" "-t"] ; Gets around error 'Pseudo-terminal will not be allocated because stdin is not a terminal.'
             {:id :server-terminal
              :thread? true
              :read-streams? true
              :handlers
                {:output-line
                  (fn [_ line _]
                    (when print-streams? (print line))
                    (@line-handler line))}})
           (wait-until 10000   (-> @thread/reg-threads :server-terminal :thread))
           (reset! terminal    (-> @thread/reg-threads :server-terminal :thread))
           (reset! output-chan (-> @thread/reg-threads :server-terminal :std-output-chan))
           true))))

(defn restart-terminal! []
  (.destroy @terminal)
  (reset! terminal nil)
  (launch-terminal!))

(defn with-terminal
  {:todo ["Has the potential to get stuck in an infinite loop.
           use |try-times| instead of |recur|."
          "Allow multiple terminals (simultaneous SSHs)"]}
  [& commands]
  (when-not (reset! terminal (-> @thread/reg-threads :server-terminal :thread))
    (launch-terminal! true))
  
  (doseq [command commands]
    (sh/input! :server-terminal command)))

(defn command
  ([]          (with-terminal "\n"))
  ([command-n] (with-terminal (str command-n "\n")))
  ([command-n handler]
    #_(reset! line-handler handler)
    (command command-n)))


(defn wait-until-prompt [timeout s]
  (wait-until
    timeout
    (-> @output-chan last (containsv? s))))

(defn install-java! []
  ; TODO fix things here and make cleaner...
  (command "sudo add-apt-repository ppa:webupd8team/java")
  (wait-until-prompt 5000 "Press [ENTER] to continue")
  (with-terminal "\n")
  (wait-until-prompt 5000 (prompt))
  (command "sudo add-apt-repository ppa:webupd8team/java")
  (wait-until-prompt 5000 "Press [ENTER] to continue")
  (with-terminal "\n")
  (command "sudo apt-get update")
  (wait-until-prompt 10000 (prompt))
  (command "sudo apt-get install oracle-java8-installer")
  ; (wait-until-prompt 5000 "Press [ENTER] to continue")
  (wait-until-prompt 5000 "Do you accept the Oracle Binary Code license terms?")
  (command "Y"))

(defn install-maven! []
  (command "sudo apt-get install maven")
  (wait-until-prompt 5000 "Do you want to continue?")
  (command "Y")
  (wait-until-prompt 50000 (prompt))
  ; (command "mvn --version") ; run this as a test to make sure
  )

(defn install-leiningen! []
  ; (command "sudo apt-get install leiningen") ; NO: gets Leiningen 1.7
  ; (wait-until-prompt 5000 "Do you want to continue?")
  ; (command "Y")
  ; TODO: RUN LEININGEN SCRIPT
  (wait-until-prompt 50000 (prompt))
  )

(defn install-git! []
  (command "sudo apt-get install git")
  (wait-until-prompt 5000 "Do you want to continue?")
  (command "Y")
  (wait-until-prompt 50000 (prompt)))

(defn install-all! []
  (install-java!)
  (install-maven!)
  (install-leiningen!))

(defn auth-repo! [repo-name]
  (when (auth/datum :github repo-name :private?)
    (wait-until-prompt 5000 "Username for")
    (command (auth/datum :github :username))
    (wait-until-prompt 5000 "Password for")
    (command (auth/datum :github :password))))

(defn clone-repos! [repos]
  (doseq [repo-name repos]
    (command (str "git clone https://www.github.com/" (auth/datum :github :username) "/" repo-name))
    (auth-repo! repo-name)
    (wait-until-prompt (convert 2 :min :millis) (prompt))))

(defn update-repo!
  [repo]
  (command (str/sp "cd" (str "~/"repo) "&& git pull origin master"))
  (auth-repo! repo)
  (wait-until-prompt (convert 2 :min :millis) (prompt)))

; With lein install 
; "If there's an error, the SSH will have to be relaunched"
; (command  "mvn install:install-file -DgroupId=seqspert -DartifactId=seqspert -Dversion=1.7.0-alpha6.1.0 -Dpackaging=jar -Dfile=~/quantum/lib/seqspert-1.7.0-alpha6.1.0.jar")
; ((command  "mvn install:install-file -DgroupId=com.datomic -DartifactId=datomic-pro -Dversion=0.9.5173 -Dpackaging=jar -Dfile=~/quantum/lib/datomic/datomic-pro-0.9.5173"))
; (command "cd ~/quantum && lein install")
; ["lein.bat", "trampoline", "run", "-m", "clojure.main"]
; sudo wget -P /bin https://raw.github.com/technomancy/leiningen/stable/bin/lein
; sudo chmod 755 /bin/lein  
; cd ~/socialytic && /bin/lein trampoline run -m clojure.main

; http://unix.stackexchange.com/questions/4034/how-can-i-disown-a-running-process-and-associate-it-to-a-new-screen-shell
; When you first login: |screen -D -R|; run your command
; either disconnect or suspend it with CTRL-Z and then disconnect from screen by pressing CTRL-A then D.
; When you login to the machine again, reconnect by running screen -D -R.
; You will be in the same shell as before.
; You can run jobs to see the suspended process if you did so,
; and run %1 (or the respective job #) to foreground it again.


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
    ["-L" "5901:localhost:5901" "-i" @ssh-keys-path (ssh-address) "-N"]
    {:id :ssh-vnc-tunnel :thread? true})
  ; Launch VNC Viewer
  (sh/run-process! vnc-viewer-path
    []
    {:id :vnc-viewer :thread? true})
  ; Open VNC viewer and navigate to localhost:5901
  ; It says the connection is not secure, but it is! 
)
