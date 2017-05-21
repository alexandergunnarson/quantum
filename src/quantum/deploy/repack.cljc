(ns quantum.deploy.repack
  (:require [quantum.core.string    :as str ]
            [quantum.core.io        :as io  ]
            [quantum.core.paths     :as path]
            [quantum.core.log       :as log ]
            [quantum.db.datomic     :as db  ]
            [quantum.core.resources :as res ]))

#_(:clj
(defonce db
  (db/->db
    {:backend {:type                   :free
               :name                   "repack"
               :host                   "localhost"
               :port                   32006
               :create-if-not-present? true
               :default-partition      :db.part/test
               :txr-props
                 {:start?            true
                  :kill-on-shutdown? true
                  :datomic-path      (path/path
                                       "/usr" "local" "datomic" ; TODO dynamically determine where it is
                                       "datomic-free-0.9.5344")
                  :resources-path    (path/path
                                       (System/getProperty "user.dir")
                                       "resources" "db")
                  ; Recommended settings for -Xmx4g production usage.
                  :internal-props    {"memory-index-threshold" "32m"
                                      "memory-index-max"       "512m"
                                      "object-cache-max"       "1g"}}}})))
(def db nil)

#?(:clj
(defn repack!
  "Does not copy README.md.
   Currently ignores both ./resources and ./test."
  {:usage `(repack! {:from-dir "../quantum"
                     :to-dir   "../quantum-lib"
                     :logging-props-filename "logging.properties"
                     :create-git?            true})
   :todo  ["Copying integration tests (Unit tests are in metadata)"
           "Copying ./resources folder and other miscellanea"
           "Do this more declaratively"]}
  [{:keys [from-dir to-dir
           test-dir resources-dir
           license-filename logging-props-filename
           create-git?]
    :or   {license-filename       "LICENSE"
           test-dir               "test"
           resources-dir          "resources"}}]
  (let [copy-project-clj! (fn [root' content]
                            (log/pr ::debug "Creating project.clj...")
                            (io/assoc! (path/path root' "project.clj")
                              content))
        copy-license!     (fn [root']
                            (log/pr ::debug "Copying license...")
                            (io/copy! (path/path from-dir license-filename)
                                      (path/path root'    license-filename)))
        copy-test!        (fn [root']
                            (log/pr ::debug "Copying test directory...")
                            (io/mkdir! (path/path root' test-dir)))
        copy-resources!   (fn [root']
                            (log/pr ::debug "Copying resources directory...")
                            (io/mkdir! (path/path root' resources-dir)))
        copy-miscellanea! (fn [root']
                            (log/pr ::debug "Copying miscellanea...")
                            (when logging-props-filename
                              (io/copy! (path/path from-dir logging-props-filename)
                                        (path/path root'    logging-props-filename))))
        copy-src!         (fn [root']
                            (log/pr ::debug "Copying source code...")
                            (io/mkdir! (path/path root' "src"))
                            )]
    #_(doseq [sub-project project-dirs]
      (let [root'        ""
            project-clj' "" ; TODO transform root project.clj to sub project.clj
            ]
        (io/mkdir! root')
        (copy-project-clj! root' project-clj')
        (copy-license!     root')
        (copy-test!        root')
        (copy-resources!   root')
        (copy-miscellanea! root')
        (copy-src!         root')
        (git-update!       root'))))))
