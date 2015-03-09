(ns quantum.core.network.deploy)

(require '[quantum.core.ns                 :as ns :refer :all])
(ns/require-all *ns* :clj :lib)
(require '[quantum.http.url :as url])

(require '[org.httpkit.client              :as http])
(require '[quantum.auth.core               :as auth])
(require '[quantum.http.core               :as qhttp])
(import 'quantum.http.core.HTTPLogEntry)
(require '[quantum.core.data.json          :as json]) 


(def heroku-help-center
  "https://devcenter.heroku.com/articles/getting-started-with-clojure")

(def apps (atom #{"ramsey"}))
(def default-app (atom "ramsey"))


(defn create!
  [^String app-name]
  (thread+ :heroku-write
    (sh/exec! [:projects app-name] "heroku" "create")))

(defn ^Int count-dynos
  "Count the number of dynos running on the given app."
  ([]
    (count-dynos @default-app))
  ([^String app-name]
    (thread+ :count-dynos
      (sh/exec! [:projects app-name] "heroku" "ps")
      (-> @sh/processes (get "heroku ps")
          :out last+ first
          (take-afteri+ "web (")
          (take-untili+ "X):")
          str/val))))

(defn scale-to!
  "Scaling the application may require account verification.
   For each application, Heroku provides 750 free dyno-hours."
  {:threaded true}
  ([^Int dynos]
    (scale-to! @default-app dynos))
  ([^String app-name ^Int dynos]
    (thread+ :heroku-write
      (sh/exec! [:projects app-name]
        "heroku" "ps:scale" (str "web=" dynos)))))

(defn launch-instance!
  {:threaded true}
  ([]
    (launch-instance! @default-app))
  ([^String app-name]
    (when (< (count-dynos) 1)
      (scale-to! app-name 1))))

(defn deploy!
  {:threaded true}
  ([]
    (deploy! @default-app))
  ([^String app-name]
    (thread+ :heroku-git
      (sh/exec! [:projects app-name] "git" "push"   "heroku" "master")))
  ([^String app-name ^String commit-desc]
    (thread+ :heroku-git
      (sh/exec! [:projects app-name] "git" "add"    ".")
      (sh/exec! [:projects app-name] "git" "commit" "-am" (str "\"" commit-desc "\""))
      (sh/exec! [:projects app-name] "git" "push"   "heroku" "master"))))

(defn visit
  ([]
    (visit @default-app))
  ([^String app-name]
    (sh/exec! [:projects app-name] "heroku" "open")))

(defn dep-deploy! [^String repo-name]
  (let [^Key thread-id
          (keyword (str "lein-install-" (-> repo-name str/keywordize name)))]
    (thread+ thread-id
      (sh/exec! [:projects repo-name] "lein" "install"))))

(defn dep-release!
  {:todo "CAN'T USE YET. Lein deploy clojars requires input"}
  [^String repo-name]
  (dep-deploy! repo-name)
  (let [^Key thread-id
          (keyword (str "lein-deploy-clojars-" (-> repo-name str/keywordize name)))]
    (thread+ thread-id
      (sh/exec! [:projects repo-name] "lein" "deploy" "clojars"))))

(defn logs [^String repo-name]
  ; requires CTRL-C to end stream
  (thread+ :heroku-logs ; asynchronous because it's a log stream
    (sh/exec! [:projects repo-name] "heroku" "logs" "--tail")))

(defn create-proc-file! [^String repo-name ^String jar-name]
  (io/write!
    :path         [:projects repo-name "Procfile"]
    :write-method :print
    :file-type    ""
    :data (str "web: java $JVM_OPTS -cp target/" jar-name ".jar"
               " clojure.main -m " repo-name ".web")))

(defn create-uberjar! [^String repo-name]
  (let [^Key thread-id
          (keyword (str "lein-uberjar-" (-> repo-name str/keywordize name)))]
    (thread+ thread-id
      (sh/exec! [:projects repo-name] "lein" "uberjar"))))


; (load-file (-> [:this-dir "src" "quanta" "audio" "myspace.clj"] io/file str))

(require '[cljs.repl :as repl])
(require '[cljs.repl.browser :as browser])  ;; require the browser implementation of IJavaScriptEnv
(require '[cemerick.piggieback])
(require '[weasel.repl.websocket])

(defn launch-buggy-cljs-browser
  ; Forgets vars somehow and doesn't completely evaluate the rest
  []
  (def env (browser/repl-env)) ;; create a new environment
  (repl/repl env))  


(defn launch-cljs-browser
  ; IllegalStateException
  ; Can't change/establish root binding of:
  ; *cljs-repl-options* with set  clojure.lang.Var.set (Var.java:221)
  []
  (cemerick.piggieback/cljs-repl
    :repl-env
      (weasel.repl.websocket/repl-env
        :ip "0.0.0.0" :port 9001)))


