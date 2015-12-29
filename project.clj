(defproject quantum/core "0.2.4.9-beta"
  :version-history
    {"0.2.4.6" #{:stable :clj :cljs}
     "0.2.4.7" #{:abandoned}
     "0.2.4.8" #{:lib}}
  :todos ["Eliminate boxed math and reflection warnings"]
  :description      "Some quanta of computational abstraction, assembled."
  :jvm-opts ^:replace
    ["-XX:-OmitStackTraceInFastThrow"
     "-d64" "-server"]
  ;:aot :all ;[quantum.core.macros] ; "^:skip-aot" doesn't work; same with regexes
  :jar-name          "quantum-dep.jar"
  :uberjar-name      "quantum.jar"
  :url               "https://www.github.com/alexandergunnarson/quantum"
  :license           {:name "Creative Commons Attribution-ShareAlike 3.0 US (CC-SA) license"
                      :url "https://creativecommons.org/licenses/by-sa/3.0/us/"}
  ; :signing          {:gpg-key "72F3C25A"}
  #_:deploy-repositories #_[;["releases" :clojars]
                        ["clojars" {:creds :gpg}]
                        []]
  :repositories {"sonatype-oss-public"
                 "https://oss.sonatype.org/content/groups/public/"}
  :plugins [#_[s3-wagon-private "1.1.2"]]
  :dependencies
    [[org.clojure/clojure                       "1.8.0-alpha2"    ] ; July 16th
     [org.clojure/clojurescript                 "1.7.170"         ]
     ; ==== CORE ====
       [proteus                                 "0.1.4"           ]
       ; ==== NAMESPACE ====
       [quantum/ns                              "1.0"             ]
       [org.clojure/tools.namespace             "0.2.11"          ]
       ; ==== ASYNC ====
         [org.clojure/core.async                "0.2.374"         ]
         [co.paralleluniverse/pulsar            "0.7.3"           ] ; If you include it, it conflicts
         [co.paralleluniverse/quasar-core       "0.7.3"           ] ; :classifier "jdk8" 
         ;[com.typesafe.akka/akka-actor_2.11    "2.4.0"           ]
       ; ==== DATA ====
         [com.carrotsearch/hppc                 "0.7.1"           ] ; High performance primitive collections for Java
         [it.unimi.dsi/fastutil                 "7.0.7"           ]
         [seqspert                              "1.7.0-alpha6.1.0"]
         [fast-zip                              "0.6.1"           ]
         ; VECTOR
         [org.clojure/core.rrb-vector           "0.0.11"          ]
         [org.clojure/data.finger-tree          "0.0.2"           ]
         ; MAP / SET     
         [org.flatland/ordered                  "1.5.2"           ]
         [org.clojure/data.avl                  "0.0.12"          ]
         [org.clojure/data.int-map              "0.2.1"           ]
         ; ==== COMPLEX ====
           ; JSON
           [cheshire                            "5.3.1"           ] ; for oauth-clj; uses Jackson 2.3.1 ; JSON parsing
           ; CSV
           [org.clojure/data.csv                "0.1.2"           ]
           ; XML
           [org.clojure/data.xml                "0.0.8"
             :exclusions [org.clojure/clojure]                    ]
     
       ; ==== COLLECTIONS ====
         ; CORE
       ; ==== CONVERT ====
         [byte-streams                          "0.2.0"           ]
         #_[gloss                               "0.2.5"           ] 
       ; ==== CRYPTOGRAPHY ====
         [com.lambdaworks/scrypt                "1.4.0"           ]
         [org.mindrot/jbcrypt                   "0.3m"            ]
         [commons-codec/commons-codec           "1.10"            ]
         [org.bouncycastle/bcprov-jdk15on       "1.53"            ]
       ; ==== ERROR ====
         [slingshot                             "0.12.2"          ]
       ; ==== GRAPH ====
         [aysylu/loom                           "0.5.0"           ]
       ; ==== IO ====
        ;[commons-io/commons-io                 "2.4"             ] ; writing byte arrays to file and such
         [com.taoensso/nippy                    "2.7.0-alpha1"
           :exclusions [org.clojure/tools.reader
                        org.clojure/clojure
                        org.json/json]                            ] ; data serialization
         [iota                                  "1.1.2"       
           :exclusions [org.codehaus.jsr166-mirror/jsr166y
                        org.clojure/clojure]                      ] ; fast/efficient string IO manipulation
         [com.cognitect/transit-clj             "0.8.271"
           :exclusions [com.fasterxml.jackson.core/jackson-core]  ]
       ; ==== JAVA/CLASS ====
         [org.clojure/java.classpath            "0.2.3"           ]
         [alembic                               "0.3.2"           ]
         [org.reflections/reflections           "0.9.10"          ]
         [com.carrotsearch/java-sizeof          "0.0.5"           ] ; Get size of Java Objects
       ; ==== MACROS ====
         [riddley                               "0.1.10"          ]
         #_[potemkin                            "0.3.11"  ; defprotocol+, definterface+, etc.
           :exclusions [riddley]                                  ]
       ; ==== NUMERIC ====              
         [net.jafama/jafama                     "2.1.0"           ]
         [org.clojure/math.combinatorics        "0.1.1"           ]
       ; ==== PRINT ====
         [fipp                                  "0.6.2"           ]
       ; ==== RESOURCE ====
         [com.stuartsierra/component            "0.2.3"           ]
       ; ==== STRING ====
         ; REGEX
         [frak                                  "0.1.6"           ]   
       ; ==== TIME ====    
         #_[clj-time                            "0.7.0"           ] ; similar to JODA time
         [com.andrewmcveigh/cljs-time           "0.3.2"
           :exclusions [org.json/json]                            ]
       ; ==== UTIL ====
         ; BENCH      
         [criterium                             "0.4.3"           ]
         ; DEBUG
         [clj-stacktrace                        "0.2.8"           ]
         [debugger                              "0.1.7"           ]
         ; REPL
         [figwheel                              "0.5.0-2-Q"       ]
     ; ==== DB ====
       ; DATOMIC
       [com.datomic/datomic-pro                 "0.9.5206"
         :exclusions [joda-time]                                  ]
     ; ==== UI ====
       [fx-clj                                  "0.2.0-alpha1"
         :exclusions [potemkin]                                   ] ; 0.2.0-SNAPSHOT
       [reagent                                 "0.5.0"
         :exclusions [org.json/json]                              ]
       ;[freactive                              "0.1.0"           ] ; a pure ClojureScript answer to react + reagent
       ;[domina                                 "1.0.3"           ] ; DOM manipulation
       ; CSS
       [garden                                  "1.2.5"           ]
     ; ==== HTTP ====      
       [clj-http                                "1.1.2"
         :exclusions [riddley
                      cheshire
                      org.json/json
                      com.fasterxml.jackson.core/jackson-core
                      commons-codec]                              ]
       ;[cljs-http                              "0.1.27"          ]
       [http-kit                                "2.1.18"
         :exclusions [org.clojure/clojure]                        ] 
       [org.apache.httpcomponents/httpcore      "4.4.1"           ]
       [org.apache.httpcomponents/httpclient    "4.4" 
         :exclusions [commons-codec]                              ]
       [org.apache.httpcomponents/httpmime      "4.4"
         :exclusions [commons-codec]                              ]
       ; ==== SERVER ====
       [compojure                               "1.3.3"
         :exclusions [org.eclipse.jetty/jetty-server
                      org.eclipse.jetty/jetty-servlet
                      javax.servlet/servlet-api
                      clj-http]                                   ]
       [org.eclipse.jetty/jetty-server          "9.2.10.v20150310"]
     ; WEB
     [com.github.detro/phantomjsdriver          "1.2.0"
       :exclusions [xml-apis
                    commons-codec]                                ]
     ; ==== CODE TRANSFORMATION ====
       ; META (CODE)      
       ;[repetition-hunter                      "1.0.0"           ]
       ; COMPILE/TRANSPILE
       [org.eclipse.jdt/org.eclipse.jdt.core    "3.10.0"          ] ; Format Java source code
       [com.github.javaparser/javaparser-core   "2.1.0"           ] ; Parse Java source code
     ; METADATA EXTRACTION/PARSING
     [org.apache.tika/tika-parsers              "1.9"             ]
     ; DATAGRID
     [org.apache.poi/poi                        "3.9"             ]
     [org.apache.poi/poi-ooxml                  "3.9"             ] ; Conflicts with QB WebConnector stuff (?) as well as HTMLUnit (org.w3c.dom.ElementTraversal)
     ; AUDIO
     ; [net.sourceforge.jvstwrapper/jVSTwRapper "0.9g"            ] ; Creating audio plugin
     ; [net.sourceforge.jvstwrapper/jVSTsYstem  "0.9g"            ] ; Creating audio plugin
     ; ==== MULTIPLE ====
     ; COMPRESSION, HASHING...
     [byte-transforms                           "0.1.3"           ]
     ; ==== MISCELLANEOUS ====
     ]
   :injections [(require '[quantum.core.ns :as ns])
                (reset! ns/externs? false)
                (let [oldv (ns-resolve (doto 'clojure.stacktrace require)
                                       'print-cause-trace)
                      newv (ns-resolve (doto 'clj-stacktrace.repl require)
                                      'pst)]
                  (alter-var-root oldv (constantly (deref newv))))] ; for :aot
   :profiles
   {:dev {:injections []
              ; [(do (ns quanta.main (:gen-class))
              ;      (require '[quantum.core.ns :as ns])
              ;      (clojure.main/repl :print quantum.core.print/!)
              ; )]
          :resource-paths ["dev-resources"]
          :dependencies   []
          :plugins [;[codox "0.8.8"]
                    [lein-cljsbuild                  "1.0.5"]
                    ;[com.cemerick/clojurescript.test "0.3.1" :exclusions [org.json/json]]
                    
                    ; rm -rf ./target && rm -rf ./dev-resources/public/js && lein figwheel dev
                    [lein-figwheel                   "0.5.0-2-Q"
                      :exclusions [org.clojure/core.async
                                   org.clojure/core.cache]]
                    [jonase/eastwood                 "0.2.1"]
                    ]}}
  :aliases {"all" ["with-profile" "dev:dev,1.5:dev,1.7"]
            "deploy-dev"  ["do" "clean," "install"]
            "deploy-prod" ["do" "clean," "install," "deploy" "clojars"]
            "test"        ["do" "clean," "test," "with-profile" "dev" "cljsbuild" "test"]}
  :auto-clean     false ; is this a mistake?
  :java-source-paths ["src/java"]
  :source-paths      ["src/clj"
                      "src/cljc"
                      "src/cljc_next"
                      "src/cljs"]
  ;:resource-paths ["resources"] ; important for Figwheel
  :test-paths     ["test"]
  :global-vars {*warn-on-reflection* true}
  :java-agents [[co.paralleluniverse/quasar-core "0.7.3"]] ;  :classifier "jdk8" 
  :cljsbuild
    {:builds
      [{:id "dev"
        :figwheel true
        :source-paths ["test/cljs" "test/cljc_temp"] #_["src/cljs" "src/cljc"  "test/cljs"]
        :compiler {:output-to            "dev-resources/public/js/compiled/quantum.js"
                   :output-dir           "dev-resources/public/js/compiled/out"
                   :optimizations        :none
                   :main                 quantum.cljstest
                   :asset-path           "js/compiled/out"
                   :source-map           true
                   :source-map-timestamp true
                   :cache-analysis       true}}
       {:id "min"
        :source-paths ["src/cljs" "src/cljc"]
        :compiler {:output-to     "dev-resources/public/js/compiled/quantum.js"
                   :main          quantum.cljstest                     
                   :optimizations :advanced
                   :pretty-print  false}}]}
  :figwheel {:http-server-root "public" ;; default and assumes "resources" 
             :server-port 3449
             :css-dirs ["dev-resources/public/css"]}
  )