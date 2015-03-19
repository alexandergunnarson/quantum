(defproject quantum/core "0.2.0.0"
  :description      "Some quanta of computational abstraction, assembled."
  :jvm-opts         []
  ;:uberjar          {:aot :all}
  :jar-name         "quantum-dep.jar"
  :uberjar-name     "quantum.jar"
  :url              "https://www.github.com/alexandergunnarson/quantum"
  :scm              {:name "quantum"
                     :url  "https://www.github.com/alexandergunnarson/quantum"}
  :license          {:name "Creative Commons Attribution-ShareAlike 3.0 US (CC-SA) license"
                     :url "https://creativecommons.org/licenses/by-sa/3.0/us/"}
  ; :signing          {:gpg-key "72F3C25A"}
  ; :deploy-repositories [["releases" :clojars]
  ;                       ["clojars" {:creds :gpg}]]
  :plugins          [[jonase/eastwood "0.2.1"]]
  :dev-dependencies []
  :dependencies
    [; CLOJURE CORE
     [org.clojure/clojure               "1.7.0-alpha4"]
     ; DATAGRID
     [org.apache.poi/poi                "3.9"         ]
     [org.apache.poi/poi-ooxml          "3.9"         ] ; Conflicts with QB WebConnector stuff (?) as well as HTMLUnit (org.w3c.dom.ElementTraversal)
     ; META (CODE)      
     [repetition-hunter                 "1.0.0"       ]
     ; defprotocol+, definterface+, etc.
     [potemkin                          "0.3.11"      ]
     ; IO      
     [commons-io/commons-io             "2.4"         ] ; writing byte arrays to file and such
     [com.taoensso/nippy                "2.7.0-alpha1"] ; data serialization
     [iota                              "1.1.2"       ] ; fast/efficient string IO manipulation
     ; UTIL.TRACE      
     [org.clojure/tools.trace           "0.7.6"       ]
     ; UTIL.BENCH      
     [criterium                         "0.4.3"       ]
     ; TIME      
     [clj-time                          "0.7.0"       ] ; similar to JODA time
     [com.andrewmcveigh/cljs-time       "0.3.2"       ]
     ; PRINT      
     [fipp                              "0.4.3"       ]
     ; ERROR      
     [slingshot                         "0.10.3"      ]
     ; NETWORK.HTTP      
     [clj-http                          "1.0.0"       ]
     [http-kit                          "2.1.18"      ] 
     [oauth-clj                         "0.1.12"      ]
     [net.sourceforge.htmlunit/htmlunit "2.15"        ] ; for some reason, retrieves 2.14....
     ; THREADING, ASYNC, CONCURRENCY
     [org.clojure/core.async            "0.1.346.0-17112a-alpha"]
     ; IO
     [iota                              "1.1.2"] ; fast/efficient string IO manipulation
     ; NUMERIC                   
     [primitive-math                    "0.1.3"]
     ; DATA IN GENERAL
     [clj-tuple 						"0.2.0"]
     ; DATA.VECTOR
     [org.clojure/core.rrb-vector       "0.0.11"]
     ; DATA.MAP; DATA.SET     
     [org.clojure/data.avl              "0.0.12"]
     [org.clojure/data.finger-tree      "0.0.2"] ; issues on Windows... just barely. weird
     [org.flatland/ordered              "1.5.2"] ; Insertion-ordered maps and sets
     ; DATA.HTML             
     [enlive                            "1.1.5"] ; HTML parsing
     ; DATA.CSV             
     [org.clojure/data.csv              "0.1.2"]
     ; DATA.JSON             
     [cheshire                          "5.3.1"] ; for oauth-clj; uses Jackson 2.3.1 ; JSON parsing
     [org.clojure/core.rrb-vector       "0.0.11"]
     ; FUNCTION     
     [transduce                         "0.1.0"] ; required for fipp
     [com.damballa/parkour              "0.5.4"]
     ; AUDIO
     ; [net.sourceforge.jvstwrapper/jVSTwRapper "0.9g"] ; Creating audio plugin
     ; [net.sourceforge.jvstwrapper/jVSTsYstem  "0.9g"] ; Creating audio plugin
     ; DATAGRID + EXCEL
     ; [org.apache.poi/poi          "3.9"] ; Conflicts with QB WebConnector stuff (?) as well as HTMLUnit (org.w3c.dom.ElementTraversal)
     ; [org.apache.poi/poi-ooxml    "3.9"] ; NOT INCLUDED
     ]
   :profiles
     {:dev {:injections
              [(do (ns quanta.main (:gen-class))
                   ;(require '[quantum.core.ns :as ns])
                   ;(ns/require-all *ns* :clj :lib)
                   ;(clojure.main/repl :print !)
                   )]
            :dependencies
              [[org.clojure/clojure "1.7.0-alpha5"]
               [org.clojure/clojurescript "0.0-2665"]
               [org.clojure/core.async "0.1.346.0-17112a-alpha"]]
            :plugins [[com.keminglabs/cljx "0.6.0" :exclusions [org.clojure/clojure]]
                      [codox "0.8.8"]
                      [lein-cljsbuild "1.0.5"]
                      [com.cemerick/clojurescript.test "0.3.1"]]
            :cljx {:builds
                    [{:source-paths ["src/clj"  "src/cljx"]
                      :output-path "target/generated/src/clj"
                      :rules :clj}
                     {:source-paths ["src/cljs" "src/cljx"]
                      :output-path "target/generated/src/cljs"
                      :rules :cljs}
                     {:source-paths ["test"]
                      :output-path "target/generated/test/clj"
                      :rules :clj}
                     {:source-paths ["test"]
                      :output-path "target/generated/test/cljs"
                      :rules :cljs}]}}
      :1.5 {:dependencies [[org.clojure/clojure "1.5.1"]]}
      :1.6 {:dependencies [[org.clojure/clojure "1.6.0"]]}}
  :aliases {"all" ["with-profile" "dev:dev,1.5:dev,1.7"]
            "deploy-dev"  ["do" "clean," "cljx" "once," "install"]
            "deploy-prod" ["do" "clean," "cljx" "once," "install," "deploy" "clojars"]
            "test" ["do" "clean," "cljx" "once," "test," "with-profile" "dev" "cljsbuild" "test"]}
  ;:lein-release {:deploy-via :shell
  ;               :shell ["lein" "deploy"]}
  :auto-clean     false ; is this a mistake?
  :source-paths   ["target/generated/src/clj"  "src/clj" ]
  :resource-paths ["target/generated/src/cljs" "src/cljs"]
  :test-paths     ["target/generated/test/clj" "test"]
  :prep-tasks   [["cljx" "once"]] 
  :cljsbuild
     {; :test-commands {"unit" ["phantomjs" :runner
      ;                                  "this.literal_js_was_evaluated=true"
      ;                                  "target/unit-test.js"]}
     :builds
     {:dev {:source-paths ["src"
                           "target/generated/src/clj"
                           "target/generated/src/cljs"]
            :compiler {:output-to "target/main.js"
                       :optimizations :none
                       :pretty-print false}}
      :test {:source-paths ["src"
                            "target/generated/src/clj"
                            "target/generated/src/cljs"
                            "target/generated/test/clj"
                            "target/generated/test/cljs"]
             :compiler {:output-to "target/unit-test.js"
                        :optimizations :whitespace

                        :pretty-print true}}}}
  :codox {:src-uri-mapping {#"target/generated/src/clj" #(str "src/" % "x")}
          :src-dir-uri "http://github.com/prismatic/plumbing/blob/master/"
          :src-linenum-anchor-prefix "L"}
)