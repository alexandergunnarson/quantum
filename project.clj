(defproject quantum/core "0.2.4.4" ; Stable 0.2.4.2 ; 0.2.4.3 is tested on only Clojure
  :description      "Some quanta of computational abstraction, assembled."
  :jvm-opts         []
  ;:uberjar          {:aot :all}
  :java-source-paths ["src/java"]
  :jar-name         "quantum-dep.jar"
  :uberjar-name     "quantum.jar"
  :url              "https://www.github.com/alexandergunnarson/quantum"
  :license          {:name "Creative Commons Attribution-ShareAlike 3.0 US (CC-SA) license"
                     :url "https://creativecommons.org/licenses/by-sa/3.0/us/"}
  ; :signing          {:gpg-key "72F3C25A"}
  :deploy-repositories [["releases" :clojars]
                        ["clojars" {:creds :gpg}]]
  :repositories {"sonatype-oss-public"
               "https://oss.sonatype.org/content/groups/public/"}
  :dependencies
    [; ==== CLOJURE ====
     ; CLOJURE CORE
     [org.clojure/clojure       "1.7.0-RC1"  ]
     [quantum/ns "1.0"]
     ; CORE
     [proteus                           "0.1.4" ]

     ; [fast-zip "0.6.1"]
     ; [com.github.detro.ghostdriver/phantomjsdriver "1.1.0"]
     ; [fx-clj                    "0.2.0-alpha1"  ]
     ; [freactive.core            "0.2.0-alpha1"  ]
     ; ; ==== SERVER ====
     ; [compojure                 "1.1.8"         ]
     ; [ring/ring-jetty-adapter   "1.2.2"         ]
     ; [environ                   "0.5.0"         ]
     
     ; ==== QUANTUM ====
     ;[quantum/core              "0.2.3.0"       ] ; "0.2.0.0" stable // 0.2.2.0 cljx // 0.2.3.0 latest
     ; DATA IN GENERAL
     ;[clj-tuple                        "0.2.0"]
     
     ; DATA.VECTOR
     [org.clojure/core.rrb-vector       "0.0.11"]
     [org.clojure/data.finger-tree      "0.0.2" ]
     ; DATA.MAP; DATA.SET     
     [org.flatland/ordered              "1.5.2" ]
     [org.clojure/data.avl              "0.0.12"]
     ; DATA.CSV             
     ;[org.clojure/data.csv              "0.1.2"]
     ; DATA.XML
     [org.clojure/data.xml              "0.0.8" :exclusions [org.clojure/clojure]]
     ; DATA.JSON
     [cheshire                          "5.3.1"] ; for oauth-clj; uses Jackson 2.3.1 ; JSON parsing
     ; MACROS
     #_[potemkin                          "0.3.11"  ; defprotocol+, definterface+, etc.
       :exclusions [riddley]]
     ; PRINT      
     [fipp                              "0.6.2"       ]
     ; NUMERIC                   
     ;[primitive-math                    "0.1.3"]
     ; TIME
     [clj-time                          "0.7.0"       ] ; similar to JODA time
     [com.andrewmcveigh/cljs-time       "0.3.2"
       :exclusions [org.json/json]]
     ; JAVA (CLASSPATH, ETC.)
     [alembic                           "0.3.2"       ]
     ; UTIL.BENCH      
     [criterium                         "0.4.3"       ]
     ; IO
     ;[commons-io/commons-io             "2.4"         ] ; writing byte arrays to file and such
     [com.taoensso/nippy                "2.7.0-alpha1"
       :exclusions [org.clojure/tools.reader org.clojure/clojure org.json/json]] ; data serialization
     [iota                              "1.1.2"       ]  ; fast/efficient string IO manipulation
     [com.cognitect/transit-clj    "0.8.271"
       :exclusions [com.fasterxml.jackson.core/jackson-core]]
         

     ; THREADING, ASYNC, CONCURRENCY
     [org.clojure/core.async    "0.1.346.0-17112a-alpha"]
     ; ==== CLOJURESCRIPT ====  
     [org.clojure/clojurescript "0.0-3269"      ]
     [reagent                   "0.5.0"         :exclusions [org.json/json]]
     ;[freactive                 "0.1.0"         ] ; a pure ClojureScript answer to react + reagent
     ;[domina                    "1.0.3"         ] ; DOM manipulation
     ;[cljs-http                 "0.1.27"        ]
     [figwheel "0.3.1"]


     ;; DATAGRID
     [org.apache.poi/poi                "3.9"         ]
     [org.apache.poi/poi-ooxml          "3.9"         ] ; Conflicts with QB WebConnector stuff (?) as well as HTMLUnit (org.w3c.dom.ElementTraversal)
     ;; META (CODE)      
     ;[repetition-hunter                 "1.0.0"       ]
     
     ; ; NETWORK.HTTP      
     [clj-http                             "1.1.2"
       :exclusions [riddley cheshire org.json/json
                    com.fasterxml.jackson.core/jackson-core commons-codec]]
     [http-kit                             "2.1.18"
       :exclusions [org.clojure/clojure]] 
     [org.apache.httpcomponents/httpcore   "4.4.1"]
     [org.apache.httpcomponents/httpclient "4.4" 
       :exclusions [commons-codec]]
     [org.apache.httpcomponents/httpmime   "4.4"
       :exclusions [commons-codec]]

     [org.eclipse.jetty/jetty-server "9.2.10.v20150310"]
     [org.eclipse.jetty/jetty-server "9.2.10.v20150310"]
     
     ; AUDIO
     ; [net.sourceforge.jvstwrapper/jVSTwRapper "0.9g"] ; Creating audio plugin
     ; [net.sourceforge.jvstwrapper/jVSTsYstem  "0.9g"] ; Creating audio plugin
     ; DATAGRID + EXCEL
     ; [org.apache.poi/poi          "3.9"] ; Conflicts with QB WebConnector stuff (?) as well as HTMLUnit (org.w3c.dom.ElementTraversal)
     ; [org.apache.poi/poi-ooxml    "3.9"] ; NOT INCLUDED
     ; WEBsel
     [com.github.detro/phantomjsdriver "1.2.0"
       :exclusions [xml-apis commons-codec]]

     ; CSS
     [garden                    "1.2.5"         ]
     [org.clojure/tools.namespace "0.2.11"]
     ]
   :profiles
   {:dev {:injections []
              ; [(do (ns quanta.main (:gen-class))
              ;      (require '[quantum.core.ns :as ns])
              ;      (ns/require-all *ns* :clj :lib)
              ;      (clojure.main/repl :print !))]
            :resource-paths ["dev-resources"]
            :dependencies
              [#_[org.clojure/tools.namespace "0.2.11-SNAPSHOT"]]
            :plugins [;[codox "0.8.8"]
                      [lein-cljsbuild                  "1.0.5"]
                      [com.cemerick/clojurescript.test "0.3.1" :exclusions [org.json/json]]
                      [lein-figwheel                   "0.3.1"]
                      [jonase/eastwood                 "0.2.1"]]}}
  :aliases {"all" ["with-profile" "dev:dev,1.5:dev,1.7"]
            "deploy-dev"  ["do" "clean," "install"]
            "deploy-prod" ["do" "clean," "install," "deploy" "clojars"]
            "test"        ["do" "clean," "test," "with-profile" "dev" "cljsbuild" "test"]}
  :auto-clean     false ; is this a mistake?
  :source-paths   ["src/clj" "src/cljc"
                   "src/cljc_next"
                   "src/cljs"]
  ;:resource-paths ["resources"] ; important for Figwheel
  :test-paths     ["test"]
  ;:prep-tasks   [["cljx" "once"]] 
  :cljsbuild
    {:builds
      [{:id "dev"
        :figwheel true
        :source-paths ["src/cljs" "src/cljc" "test/cljs"]
        :compiler {:output-to            "resources/public/js/compiled/quantum.js"
                   :output-dir           "resources/public/js/compiled/out"
                   :optimizations        :none
                   :main                 quantum.cljs_test
                   :asset-path           "js/compiled/out"
                   :source-map           true
                   :source-map-timestamp true
                   :cache-analysis       true}}
       {:id "min"
        :source-paths ["src/cljs" "src/cljc"]
        :compiler {:output-to     "resources/public/js/compiled/quantum.js"
                   :main          quantum.cljs_test                     
                   :optimizations :advanced
                   :pretty-print  false}}]}
  :figwheel {:http-server-root "public" ;; default and assumes "resources" 
             :server-port 3449
             :css-dirs ["resources/public/css"]})