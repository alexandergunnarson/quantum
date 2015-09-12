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

(defn reset-terminal! []
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
  (command "sudo apt-get install leiningen")
  (wait-until-prompt 5000 "Do you want to continue?")
  (command "Y")
  (wait-until-prompt 50000 (prompt)))

(defn install-git! []
  (command "sudo apt-get install git")
  (wait-until-prompt 5000 "Do you want to continue?")
  (command "Y")
  (wait-until-prompt 50000 (prompt)))

(defn install-all! []
  (install-java!)
  (install-maven!)
  (install-leiningen!))

(defn clone-repos! [repos]
  (doseq [repo-name repos]
    (command (str "git clone https://www.github.com/" (auth/datum :github :username) "/" repo-name))
    (when (auth/datum :github repo-name :private?)
      (wait-until-prompt 5000 "Username for")
      (command (auth/datum :github :username))
      (wait-until-prompt 5000 "Password for")
      (command (auth/datum :github :password))
      (wait-until-prompt (convert 2 :min :millis) (prompt)))))

(defn update-repo
  "If there's an error, the SSH will have to be relaunched"
  [repo]
  (command (str/sp "cd" (str "~/"repo) "&& git pull origin master")))

