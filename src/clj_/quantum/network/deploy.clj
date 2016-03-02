(ns
  ^{:doc "A very experimental deployment-centered namespace.

          Focuses on Heroku, Git, and Clojars via the command line."
    :attribution "Alex Gunnarson"}
  quantum.network.deploy
  (:require-quantum [:lib http auth]))

(def heroku-help-center
  "https://devcenter.heroku.com/articles/getting-started-with-clojure")

(def apps        (atom #{"quanta"}))
(def default-app (atom "quanta"))

; cd to Heroku folder
; heroku config
; heroku config:set AWS_ACCESS_KEY_ID=XXX
; heroku config:set AWS_SECRET_KEY=XXX
; heroku config:set DATOMIC_EMAIL=XXX
; heroku config:set DATOMIC_EMAIL=XXX
; heroku config:set DATOMIC_KEY=XXX

(defn encrypt-gpg-creds! []
  (sh/proc "gpg"
    ["--default-recipient-self"
     "-e" "~/.lein/credentials.clj" ">" "~/.lein/credentials.clj.gpg"]
    {:name :encrypt-gpg-creds
     :read-streams? true
     :thread?       true}))

(defn install-dep-to-local-repo!
  {:usage '(install-dep-to-local-repo!
             '[quantum/ns "1.0"]
             [:projects "quanta" "quanta"])}
  ([dep-vec local-repo-path] (install-dep-to-local-repo! dep-vec local-repo-path nil))
  ([[dep-name version] local-repo-path opts]
    (let [maven-repo (io/file-str
                       (or (get (System/getenv) "MAVEN_REPO")
                           [:home ".m2" "repository"]))
          namespace-pathed
            (->> dep-name namespace (<- str/split #"\.") (apply io/path))
          dep-folder (io/path maven-repo namespace-pathed (name dep-name) version)
          dep-path   (->> dep-folder io/children
                          (ffilter (fn-> io/extension (= "jar")))
                          str)
          _ (with-throw (nempty? dep-path) "No .jar found in folder")
          local-repo-path-str (-> local-repo-path io/file-str (io/path "repo"))
          args
            ["org.apache.maven.plugins:maven-install-plugin:2.5.1:install-file" ; this might be changed
             (str "-Dfile="                dep-path) ; no need to str/dquote
             (str "-DgroupId="             (namespace dep-name))
             (str "-DartifactId="          (name dep-name))
             (str "-Dversion="             version)
             "-Dpackaging=jar"        
             (str "-DlocalRepositoryPath=" local-repo-path-str)]]
      (log/pr ::debug "ARGS ARE" args)
      (sh/proc "mvn"
        args
        (merge-keep-left opts
          {:dir (io/up-dir local-repo-path-str)
           :handlers {:output-line (fn [line] (println line))}
           :name :install-jar-to-local-repo
           :read-streams? true
           :thread?       true})))))


(defn create!
  [^String app-name]
  (thread+ {:id :heroku-write}
    (sh/exec! [:projects app-name] "heroku" "create")))

#_(defn ^Int count-dynos
  "Count the number of dynos running on the given app."
  ([]
    (count-dynos @default-app))
  ([^String app-name]
    (thread+ {:id :count-dynos}
      (sh/exec! [:projects app-name] "heroku" "ps")
      (-> @sh/processes (get "heroku ps")
          :out last first
          (take-after "web (")
          (take-until "X):")
          str/val))))

#_(defn scale-to!
  "Scaling the application may require account verification.
   For each application, Heroku provides 750 free dyno-hours."
  {:threaded true}
  ([^Int dynos]
    (scale-to! @default-app dynos))
  ([^String app-name ^Int dynos]
    (thread+ {:id :heroku-write}
      (sh/exec! [:projects app-name]
        "heroku" "ps:scale" (str "web=" dynos)))))

#_(defn launch-instance!
  {:threaded true}
  ([]
    (launch-instance! @default-app))
  ([^String app-name]
    (when (< (count-dynos) 1)
      (scale-to! app-name 1))))

#_(defn deploy!
  {:threaded true}
  ([]
    (deploy! @default-app))
  ([^String app-name]
    (async {:name :heroku-git} ; TODO This is unnecessary — use sh's built in async
      (sh/exec! "git" ["push"   "heroku" "master"]                 {:dir [:projects app-name]})))
  ([^String app-name ^String commit-desc]
    (async {:name :heroku-git} ; TODO This is unnecessary — use sh's built in async
      (sh/exec! "git" ["add"    "."]                               {:dir [:projects app-name]})
      (sh/exec! "git" ["commit" "-am" (str "\"" commit-desc "\"")] {:dir [:projects app-name]})
      (sh/exec! "git" ["push"   "heroku" "master"]                 {:dir [:projects app-name]}))))

#_(defn visit
  ([]
    (visit @default-app))
  ([^String app-name]
    (sh/exec! [:projects app-name] "heroku" "open")))

#_(defn dep-deploy! [^String repo-name]
  (let [^Key thread-id
          (keyword (str "lein-install-" (-> repo-name str/keywordize name)))]
    (thread+ {:id thread-id}
      (sh/exec! [:projects repo-name] "lein" "install"))))

#_(defn dep-release!
  {:todo "CAN'T USE YET. Lein deploy clojars requires input"}
  [^String repo-name]
  (dep-deploy! repo-name)
  (let [^Key thread-id
          (keyword (str "lein-deploy-clojars-" (-> repo-name str/keywordize name)))]
    (thread+ {:id thread-id}
      (sh/exec! [:projects repo-name] "lein" "deploy" "clojars"))))

#_(defn logs [^String repo-name]
  (thread+ {:id :heroku-logs}
    (sh/exec! [:projects repo-name] "heroku" "logs")))

#_(defn logs-streaming [^String repo-name]
  ; requires CTRL-C to end stream
  (thread+ {:id :heroku-logs-streaming} ; asynchronous because it's a log stream
    (sh/exec! [:projects repo-name] "heroku" "logs" "--tail")))

#_(defn create-proc-file! [^String repo-name ^String jar-name]
  (io/write!
    :path         [:projects repo-name "Procfile"]
    :write-method :print
    :file-type    ""
    :data (str "web: java $JVM_OPTS -cp target/" jar-name ".jar"
               " clojure.main -m " repo-name ".web")))

#_(defn create-uberjar! [^String repo-name]
  (let [^Key thread-id
          (keyword (str "lein-uberjar-" (-> repo-name str/keywordize name)))]
    (thread+ {:id thread-id}
      (sh/exec! [:projects repo-name] "lein" "uberjar"))))





