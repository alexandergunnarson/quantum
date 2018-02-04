(ns quantum.meta.project-base
  (:require
    [clojure.java.io        :as io]
    [clojure.string         :as str]
    [leiningen.core.project :as project]))

(defn merge-with-k
  "Like `merge-with`, but the merging function takes the key being merged
   as the first argument"
   {:attribution  "prismatic.plumbing"
    :contributors ["Alex Gunnarson"]}
  [f & maps]
  (when (some identity maps)
    (let [merge-entry
           (fn [m e]
             (let [k (key e) v (val e)]
               (if (contains? m k)
                   (assoc m k (f k (get m k) v))
                   (assoc m k v))))
          merge2
            (fn ([] {})
                ([m1 m2]
                 (reduce merge-entry (or m1 {}) (seq m2))))]
      (reduce merge2 maps))))

(def clj-dependency  '[org.clojure/clojure       "1.9.0"])
(def cljs-dependency '[org.clojure/clojurescript "1.9.946"])

(def latest-stable-quantum-version "0.3.0-c7ed558e" #_"0.3.0-f1a3dc08")

(def quantum-source-paths
  {:typed   "../quantum/src"
   :untyped "../quantum/src-untyped"
   :posh    "../forks/posh/src"})

(def cljsbuild-dev-builds
  (let [gen-data
          (fn [id]
            (let [server-root-path "resources/server-root"
                  ;; relative to `server-root-path`
                  ;; replace "|" in order to have a URL-encoding-free path in the common case
                  asset-path       (str "generated" "/" id "/" "js")
                  output-dir       (str server-root-path "/" asset-path)]
              {:id           id
               :source-paths ["src" "src-dev"]
               :figwheel     {:load-warninged-code true
                              #_:websocket-url #_"wss://[[client-hostname]]:443/figwheel-ws"}
               :compiler     {:main          'quanta.dev
                              :output-dir    output-dir
                              :output-to     (str output-dir "/" "main.js")
                              :asset-path    asset-path
                              :optimizations :none}}))]
  ;; For figwheel to work, no character in the build IDs can necessitate an URL escape character
  [#_(gen-data "web")
   (-> (gen-data "web-quantum-dynamic-source")
       (update :source-paths into (-> quantum-source-paths vals vec)))
   (-> (gen-data "web" #_"web|quantum|dynamic-source|untyped")
       (update :source-paths into [(:untyped quantum-source-paths) (:posh quantum-source-paths)]))
   (-> (gen-data "web-re-frame-trace" #_"web|quantum|dynamic-source|untyped|re-frame-trace")
       (update :source-paths into [(:untyped quantum-source-paths) (:posh quantum-source-paths) "./src|re-frame-trace"])
       (assoc-in [:compiler :closure-defines "re_frame.trace.trace_enabled_QMARK_"] true)
       (update-in [:compiler :preloads] #(conj (or % []) 'day8.re-frame.trace.preload)))
   (gen-data "ios")
   (-> (gen-data "ios-quantum-dynamic-source")
       (update :source-paths into (-> quantum-source-paths vals vec)))
   (-> (gen-data "ios-quantum-dynamic-source-untyped")
       (update :source-paths into [(:untyped quantum-source-paths)]))]))

(def quantum-deps
  (try
    (->> "../quantum/project.clj"
         slurp
         read-string
         (drop 3)
         (apply hash-map)
         :dependencies)
    (catch Exception _ nil)))

(defn >git-hash [project-path & [base-version]]
  (let [hash-str (let [{:keys [exit out]}
                         (clojure.java.shell/sh
                           "git" "rev-parse" "--short" "HEAD"
                           :dir project-path)]
                   (when (= exit 0)
                     (subs out 0 (-> out count dec))))
        version (str base-version (when base-version "-") (or hash-str "UNKNOWN"))]
    (println "Version of" project-path "determined to be" version)
    version))

(def env
  (let [data (try (-> "data-private.clj" slurp read-string)
                  (catch Throwable e nil))]
    (-> data :env (or {}))))

(defn >default-config [opts project-config]
  (let [jar-base-name (str (some-> project-config :name namespace (str "-"))
                           (-> project-config :name name))]
    {;; ===== META ===== ;;
     :version      (>git-hash ".")
     :license      {:name "Creative Commons Attribution-ShareAlike 3.0 US (CC-SA) license"
                    :url  "https://creativecommons.org/licenses/by-sa/3.0/us/"}
     ;; ===== ENVIRONMENT ===== ;;
     :env env
     :jvm-opts ^:replace
       ["-XX:-OmitStackTraceInFastThrow"
        "-XX:+DoEscapeAnalysis"
        "-d64" "-server"]
     :global-vars '{*warn-on-reflection* true
                    *unchecked-math*     :warn-on-boxed}



     :jar-name     (str jar-base-name "-dep.jar")
     :uberjar-name (str jar-base-name ".jar")
     :dependencies [clj-dependency cljs-dependency]
     :auto-clean   false
     :target-path  "target"
     :test-paths   ["test"]
     :source-paths ["src"]
     :aliases
       {;; UTILS ;;
        "count-loc"
          ["vanity"]}
     ;; ===== PROFILES ===== ;;
     :profiles
       {:dev
          {:dependencies []
           :source-paths ["src-dev"]}
        :test    {}
        :prod    {}
        :backend {}
        :backend|test
          {:plugins '[[com.jakemccrary/lein-test-refresh "0.16.0"]]}
        :frontend
          {:plugins '[[lein-cljsbuild "1.1.7"
                        :exclusions [org.clojure/clojure org.clojure/clojurescript]]]}
        :frontend|test
          {:plugins '[[lein-doo "0.1.7"]]}
        :quantum|static-deps
          {:dependencies [['quantum/core #_"LATEST" latest-stable-quantum-version]]}
        :quantum|static-deps-local
          {:dependencies [['quantum/core #_"LATEST" (>git-hash "../quantum/")]]}
        :quantum|dynamic-source
          {:source-paths quantum-source-paths}
        :quantum|dynamic-source|untyped
          {:source-paths [(:untyped quantum-source-paths) (:posh quantum-source-paths)]}}}))

(defn with-default-config [opts project-config]
  (merge-with-k
    (fn merger [k oldv newv]
      (cond (vector? oldv)
              (do (assert (vector? newv) newv)
                  (-> (concat oldv newv) distinct vec))
            (map? oldv)
              (do (assert (map? newv) newv)
                  (merge-with-k merger oldv newv))
            :else newv))
    (>default-config opts project-config) project-config))

(defmacro defproject
  "Like `defproject`, but accepts an unquoted config map instead of macro-quoted,
   flattened key/value seq. As such, behaves more like a function than a macro."
  ([config] `(defproject nil ~config))
  ([opts config]
    (let [f (io/file *file*)]
     `(let [config# (with-default-config ~opts ~config)
            root#   ~(when f (.getParent f))]
        (def ~'project
          (project/make
            (dissoc config# :name :version)
            (:name    config#)
            (:version config#)
            root#))))))
