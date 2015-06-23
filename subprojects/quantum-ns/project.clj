(defproject quantum/ns "1.0"
  :description      "Some quanta of computational abstraction, assembled."
  :jvm-opts         []
  ;:uberjar          {:aot :all}
  :jar-name         "quantum-ns-dep.jar"
  :uberjar-name     "quantum-ns.jar"
  :url              "https://www.github.com/alexandergunnarson/quantum"
  :license          {:name "Creative Commons Attribution-ShareAlike 3.0 US (CC-SA) license"
                     :url "https://creativecommons.org/licenses/by-sa/3.0/us/"}
  ; :signing          {:gpg-key "72F3C25A"}
  ;:deploy-repositories [["releases" :clojars]
  ;                      ["clojars" {:creds :gpg}]]
 :dependencies
    [; ==== CLOJURE ====
     [org.clojure/clojure       "1.7.0-beta3"  ]
     ;[org.clojure/clojurescript "0.0-3269"      ]
     ]
  :aliases {"deploy-dev"  ["do" "clean," "install"]
            "deploy-prod" ["do" "clean," "install," "deploy" "clojars"]}
  :source-paths   ["src/cljc"])