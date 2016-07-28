(defproject quantum/core "0.3.0-6-27-2016.2"
  :description      "Some quanta of computational abstraction, assembled."
  :jvm-opts ^:replace
    ["-XX:-OmitStackTraceInFastThrow"
     "-d64" "-server"]
  ; :aot :all
  :jar-name          "quantum-dep.jar"
  :uberjar-name      "quantum.jar"
  :url               "https://www.github.com/alexandergunnarson/quantum"
  :license           {:name "Creative Commons Attribution-ShareAlike 3.0 US (CC-SA) license"
                      :url "https://creativecommons.org/licenses/by-sa/3.0/us/"}
  ; :signing          {:gpg-key "72F3C25A"}
  :env {:public-repo-s3-username "AKIAJEMTOQDDRFSPJGNQ"
        :public-repo-s3-password "uRqmUmRKq+rYpKPCXHhRH4kh8ZTZ7Lkm6HcBOe14"}
  #_:repositories #_{"repo-s3-releases" ; this has problems with lein-ancient
                   {:url        "s3://repo.quantum/releases/"
                    :username   :env/public-repo-s3-username
                    :passphrase :env/public-repo-s3-password
                    :checksum   :warn}}
  :plugins [[lein-environ  "1.0.3" ]
            [lein-essthree "0.2.1" ]
            [lein-ancient  "0.6.10"]]
  :dependencies
    [[org.clojure/clojure                       #_"1.8.0" "1.9.0-alpha10"          ] ; With hardlinking
     [org.clojure/clojurescript                 "1.9.93"          ] ; Latest (as of 7/28/2016)
     ; ==== CORE ====
       [proteus                                 "0.1.6"           ]
       ; ==== NAMESPACE ====
       [org.clojure/tools.namespace             "0.2.11"          ]
       [com.taoensso/encore                     "2.49.0"          ] ; To not break things
       ; ==== ASYNC ====
         [org.clojure/core.async                "0.2.374"         ]
         [servant                               "0.1.5"           ]
         [co.paralleluniverse/pulsar            "0.7.5"           ] ; If you include it, it conflicts
         [co.paralleluniverse/quasar-core       "0.7.5"           ] ; :classifier "jdk8" 
         ;[com.typesafe.akka/akka-actor_2.11    "2.4.0"           ]
       ; ==== DATA ====
         [com.carrotsearch/hppc                 "0.7.1"           ] ; High performance primitive collections for Java
         [it.unimi.dsi/fastutil                 "7.0.12"           ]
         [quantum/seqspert                      "1.7.0-alpha6.1.0"]
         [fast-zip                              "0.7.0"           ]
         ; VECTOR
         [org.clojure/core.rrb-vector           "0.0.11"          ]
         [org.clojure/data.finger-tree          "0.0.2"           ]
         ; MAP / SET     
         [org.flatland/ordered                  "1.5.3"           ]
         [org.clojure/data.avl                  "0.0.13"          ]
         [org.clojure/data.int-map              "0.2.2"           ]
         ; ==== COMPLEX ====
           ; JSON
           [cheshire                            "5.6.1"           ] ; for oauth-clj; uses Jackson 2.3.1 ; JSON parsing
           ; CSV
           [org.clojure/data.csv                "0.1.3"           ]
           ; XML
           [org.clojure/data.xml                "0.0.8"
             :exclusions [org.clojure/clojure]                    ]
     
       ; ==== COLLECTIONS ====
         ; CORE
       ; ==== CONVERT ====
         [byte-streams                          "0.2.2"           ]
         [org.clojure/tools.reader              "1.0.0-alpha3"    ]
         #_[gloss                               "0.2.5"           ] 
       ; ==== CRYPTOGRAPHY ====
         [com.lambdaworks/scrypt                "1.4.0"           ]
         [org.mindrot/jbcrypt                   "0.3m"            ]
         [commons-codec/commons-codec           "1.10"            ]
         [org.bouncycastle/bcprov-jdk15on       "1.54"            ]
       ; ==== ERROR ====
         [alexandergunnarson/slingshot          "0.14"            ]
       ; ==== GRAPH ====
         [aysylu/loom                           "0.6.0"           ]
       ; ==== IO ====
        ;[commons-io/commons-io                 "2.4"             ] ; writing byte arrays to file and such
         [com.taoensso/nippy                    "2.11.1"
           :exclusions [org.clojure/tools.reader
                        org.clojure/clojure
                        org.json/json]                            ]
         [iota                                  "1.1.3"       
           :exclusions [org.codehaus.jsr166-mirror/jsr166y
                        org.clojure/clojure]                      ]
         [com.cognitect/transit-clj             "0.8.285"
           :exclusions [com.fasterxml.jackson.core/jackson-core]  ]
       ; ==== JAVA/CLASS ====
         [org.clojure/java.classpath            "0.2.3"           ]
         [alembic                               "0.3.2"           ]
       #_[org.reflections/reflections           "0.9.10"          ]
         [com.carrotsearch/java-sizeof          "0.0.5"           ] ; Get size of Java Objects
       ; ==== MACROS ====
         [riddley                               "0.1.12"          ]
         #_[potemkin                            "0.3.11"           
           :exclusions [riddley]                                  ]
       ; ==== NUMERIC ====              
         [net.jafama/jafama                     "2.1.0"           ]
         [com.gfredericks/goog-integer          "1.0.0"           ]
         [org.clojure/math.combinatorics        "0.1.3"           ]
         [quantum/java                          "1.3"             ]
       ; ==== PRINT ====
         [fipp                                  "0.6.5"           ]
       ; ==== RESOURCES ====
         [com.stuartsierra/component            "0.3.1"           ]
       ; ==== STRING ====
         ; REGEX
         [frak                                  "0.1.6"           ]   
       ; ==== TIME ====    
         #_[clj-time                            "0.7.0"           ] ; similar to JODA time
         [com.andrewmcveigh/cljs-time           "0.4.0"
           :exclusions [org.json/json]                            ]
       ; ==== VALIDATE ====
         [prismatic/schema                      "1.1.1"           ]
       ; ==== META ====
         ; BENCH      
         [criterium                             "0.4.4"           ]
         ; DEBUG
         [clj-stacktrace                        "0.2.8"           ]
         [debugger                              "0.2.0"           ]
         ; REPL
         [figwheel                              "0.5.3-2"         ]
         #_[binaryage/devtools                  "0.5.2"           ]
         [environ  "1.0.3"  ]
     ; ==== DB ====
       ; DATOMIC
       [quantum/datomic-pro                     "0.9.5206"
         :exclusions [joda-time]                                  ]
       [datascript                              "0.15.0"          ]
       [datascript-transit                      "0.2.0"           ]
       [com.zachallaun/datomic-cljs             "0.0.1-alpha-1"   ]
       [posh                                    "0.3.5"           ]
       [quantum/datsync                         "0.0.1-4-11-2016" ]
       [re-frame                                "0.7.0"           ]
     ; ==== HTML ====
       [hickory                                 "0.6.0"           ]
     ; ==== UI ====
       [fx-clj                                  "0.2.0-alpha1"
         :exclusions [potemkin]                                   ]
       [reagent                                 "0.5.1"
         :exclusions [org.json/json]                              ]
       ;[freactive                              "0.1.0"           ] ; a pure ClojureScript answer to react + reagent
       ;[domina                                 "1.0.3"           ] ; DOM manipulation
       ; CSS
       [garden                                  "1.3.2"           ]
     ; ==== UUID ====
       [com.lucasbradstreet/cljs-uuid-utils     "1.0.2"           ]
       [danlentz/clj-uuid                       "0.1.6"           ]
     ; ==== HTTP ====      
       [com.taoensso/sente                      "1.8.1"           
         :exclusions [com.taoensso/encore]                        ]
       [clj-http                                "3.1.0"
         :exclusions [riddley
                      cheshire
                      org.json/json
                      com.fasterxml.jackson.core/jackson-core
                      commons-codec
                      potemkin]                                   ]
       ;[cljs-http                              "0.1.27"          ]
       [less-awful-ssl                          "1.0.1"           ]
       [http-kit                                "2.1.19"
         :exclusions [org.clojure/clojure]                        ] 
       [org.apache.httpcomponents/httpcore      "4.4.4"           ]
       [org.apache.httpcomponents/httpclient    "4.5.2" 
         :exclusions [commons-codec]                              ]
       [org.apache.httpcomponents/httpmime      "4.5.2"
         :exclusions [commons-codec]                              ]
       ; ==== ROUTING ====
       [compojure                               "1.5.0"
         :exclusions [org.eclipse.jetty/jetty-server
                      org.eclipse.jetty/jetty-servlet
                      javax.servlet/servlet-api
                      clj-http]                                   ]
       [org.eclipse.jetty/jetty-server          "9.4.0.M0"        ]
       [org.immutant/web                        "2.1.4"            
         :exclusions [clj-tuple org.jboss.logging/jboss-logging]  ]
       [aleph                                   "0.4.1"           
         :exclusions [primitive-math]                             ]
       ; ==== AUTH ====
       [com.cemerick/friend                     "0.2.1"
         :exclusions [org.clojure/core.cache]                     ]
       ; ==== MIDDLEWARE ====
       [ring/ring-defaults                      "0.2.0"           ]
       [bk/ring-gzip                            "0.1.1"           ]
     ; WEB
     [com.github.detro/phantomjsdriver          "1.2.0"
       :exclusions [xml-apis
                    commons-codec]                                ]
     ; ==== PARSING ==== ;
     [instaparse                                "1.4.2"           ]
     [com.lucasbradstreet/instaparse-cljs       "1.4.1.2"         ]
     ; ==== CODE TRANSFORMATION ====
       ; META (CODE)      
       ;[repetition-hunter                      "1.0.0"           ]
       ; COMPILE/TRANSPILE
       [org.eclipse.jdt/org.eclipse.jdt.core    "3.10.0"          ] ; Format Java source code
       [com.github.javaparser/javaparser-core   "2.4.0"           ] ; Parse Java source code
       [org.clojure/tools.emitter.jvm           "0.1.0-beta5"     ]
      ;[org.clojure/tools.analyzer.js           "0.1.0-beta5"     ] ; Broken
     ; METADATA EXTRACTION/PARSING
     [org.apache.tika/tika-parsers              "1.13"            ]
     ; DATAGRID
     [org.apache.poi/poi                        "3.14"            ]
     [org.apache.poi/poi-ooxml                  "3.14"            ] ; Conflicts with QB WebConnector stuff (?) as well as HTMLUnit (org.w3c.dom.ElementTraversal)
     ; AUDIO
     ; [net.sourceforge.jvstwrapper/jVSTwRapper "0.9g"            ] ; Creating audio plugin
     ; [net.sourceforge.jvstwrapper/jVSTsYstem  "0.9g"            ] ; Creating audio plugin
     ; TESTING
     [org.clojure/test.generative               "0.5.2"           ]
     [org.clojure/test.check                    "0.9.0"           ]
     ; ==== MULTIPLE ====
     ; COMPRESSION, HASHING...
     [byte-transforms                           "0.1.4"           ]
     [net.jpountz.lz4/lz4                       "1.3"             ]
     ; ==== MISCELLANEOUS ====
     ]
   :injections [#_(reset! ns/externs? false)
                (let [oldv (ns-resolve (doto 'clojure.stacktrace require)
                                       'print-cause-trace)
                      newv (ns-resolve (doto 'clj-stacktrace.repl require)
                                      'pst)]
                  (alter-var-root oldv (constantly (deref newv))))] ; for :aot
   :profiles
   {:dev {:injections []
              ; [(do (clojure.main/repl :print quantum.core.print/!)
              ;      (clojure.main/repl :print clojure.pprint/pprint)
              ; )]
          :resource-paths ["dev-resources"]
          :source-paths   ["dev/cljc"]
          :dependencies   []
          :plugins [[com.jakemccrary/lein-test-refresh "0.16.0"] ; CLJ  test
                    [lein-doo                          "0.1.7" ] ; CLJS test
                    [lein-cljsbuild                    "1.1.3" ]
                    [lein-figwheel "0.5.3-2"
                      :exclusions [org.clojure/clojure
                                   org.clojure/clojurescript
                                   org.clojure/core.async
                                   org.clojure/core.cache]]
                    [jonase/eastwood                 "0.2.1"]
                    [lein-cloverage                  "1.0.6"]
                    ]}}
  :aliases {"all"               ["with-profile" "dev:dev,1.5:dev,1.7"]
            "deploy-dev"        ["do" "clean," "install"]
            "deploy-prod"       ["do" "clean," "install," "deploy" "clojars"]
            "deploy-test-dev"   ["do" "clean," "cljsbuild" "once" "dev"]
            "autobuilder"       ["do" "clean," "figwheel" "dev"]
            "debug-autobuilder" ["do" "clean," "cljsbuild" "auto" "debug"]
            "test"              ["do" "clean," "test," "with-profile" "dev" "cljsbuild" "test"]}
  :auto-clean     false ; is this a mistake?
  :target-path "target"
  :clean-targets ^{:protect false} [:target-path
                                    [:cljsbuild :builds :dev :compiler :output-dir]
                                    [:cljsbuild :builds :dev :compiler :output-to ]
                                    [:cljsbuild :builds :min :compiler :output-dir]
                                    [:cljsbuild :builds :min :compiler :output-to ]]
  :java-source-paths ["src/java"]
  :source-paths      ["src/clj"
                      "src/cljc"]
  ;:resource-paths ["resources"] ; important for Figwheel
  :test-paths     ["test/cljs" "test/clj" "test/cljc"]
  :global-vars {*warn-on-reflection* true
                *unchecked-math*     :warn-on-boxed}
  :java-agents [#_[co.paralleluniverse/quasar-core    "0.7.3"      ] ;  :classifier "jdk8" 
                ; This for HTTP/2 support
                #_[kr.motd.javaagent/jetty-alpn-agent "1.0.1.Final"]]
  :cljsbuild
    {:builds
      {:debug {:source-paths ["src/cljs"  "src/cljc"
                              "dev/cljs"  "dev/cljc"
                              "test/cljs" "test/cljc"]
               :compiler     {:output-to            "dev-resources/public/js/debug-compiled/quantum.js"
                              :output-dir           "dev-resources/public/js/debug-compiled/out"
                              :optimizations        :none
                              :main                 quantum.dev
                              :asset-path           "js/debug-compiled/out"
                              :source-map           true
                              :source-map-timestamp true
                              :cache-analysis       true}}
       :dev {:figwheel true
             :source-paths ["src/cljs"  "src/cljc"
                            "dev/cljs"  "dev/cljc"
                            "test/cljs" "test/cljc"]
             :compiler {:output-to            "dev-resources/public/js/compiled/quantum.js"
                        :output-dir           "dev-resources/public/js/compiled/out"
                        :optimizations        :none
                        :main                 quantum.dev
                        :asset-path           "js/compiled/out"
                        :source-map           true
                        :source-map-timestamp true
                        :cache-analysis       true}}
       :min {:source-paths ["src/cljc" "dev/cljc"]
             :compiler {:output-to      "dev-resources/public/js/min-compiled/quantum.js"
                        :output-dir     "dev-resources/public/js/min-compiled/out"
                        :main           quantum.dev
                        :optimizations  :advanced
                        :asset-path     "js/min-compiled/out"
                        :pretty-print   false
                        ;:parallel-build true
                        }}}}
  :figwheel {:http-server-root "public" ;; default and assumes "resources" 
             :server-port 3450
             :css-dirs ["dev-resources/public/css"]})