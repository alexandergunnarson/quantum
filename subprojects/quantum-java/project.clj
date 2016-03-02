(defproject quantum/java "1.0"
  :description      "Some quanta of computational abstraction, assembled."
  :jvm-opts         []
  ;:uberjar          {:aot :all}
  :jar-name         "quantum-java-dep.jar"
  :uberjar-name     "quantum-java.jar"
  :url              "https://www.github.com/alexandergunnarson/quantum-java"
  :license          {:name "Creative Commons Attribution-ShareAlike 3.0 US (CC-SA) license"
                     :url "https://creativecommons.org/licenses/by-sa/3.0/us/"}
  ; :signing          {:gpg-key "72F3C25A"}
  ;:deploy-repositories [["releases" :clojars]
  ;                      ["clojars" {:creds :gpg}]]
 :dependencies
    [[org.clojure/clojure                       "1.8.0-alpha2"    ] ; July 16th
     [org.clojure/clojurescript                 "1.7.170"         ]
     ]
  :aliases {"deploy-dev"  ["do" "clean," "install"]
            "deploy-prod" ["do" "clean," "install," "deploy" "clojars"]}
  :source-paths      ["src/cljc"]
  :java-source-paths ["src/java"])