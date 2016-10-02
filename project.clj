(defproject quantum/core "0.3.0-01043fd"
  :description      "Some quanta of computational abstraction, assembled."
  :jvm-opts ^:replace
    ["-XX:-OmitStackTraceInFastThrow"
     "-d64" "-server"]
  :aot [sparkling.serialization sparkling.destructuring]
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
            [lein-essthree "0.2.1"
              :exclusions [org.clojure/tools.reader]]]
  :dependencies
    [[org.clojure/clojure                       "1.8.0"] ; 1.9.0-alpha* has some problems
     [clojure-future-spec                       "1.9.0-alpha12-2"]
     [org.clojure/clojurescript                 "1.9.216" #_"1.9.93"         ] ; Latest (as of 7/28/2016)
     ; ==== CORE ====
       [proteus                                 "0.1.6"           ]
       ; ==== NAMESPACE ====
       [org.clojure/tools.namespace             "0.2.11"          ]
       [com.taoensso/encore                     "2.79.1"          ] ; To not break things
       ; ==== ASYNC ====
         [org.clojure/core.async                "0.2.391"         ]
         [servant                               "0.1.5"           ]
         [#_alexandergunnarson/co.paralleluniverse.pulsar co.paralleluniverse/pulsar #_"0.7.6.2" "0.7.6"
           :exclusions [org.slf4j/*
                        potemkin
                        org.clojure/core.match
                        org.ow2.asm/*
                        com.esotericsoftware/reflectasm]]
         [co.paralleluniverse/quasar-core       "0.7.6"
           :exclusions [com.esotericsoftware/reflectasm]]
       ; ==== DATA ====
         [com.carrotsearch/hppc                 "0.7.1"           ] ; High performance primitive collections for Java
         [it.unimi.dsi/fastutil                 "7.0.12"           ]
         [quantum/seqspert                      "1.7.0-alpha6.1.0"]
         [fast-zip                              "0.7.0"           ]
         ; VECTOR
       #_[org.clojure/core.rrb-vector           "0.0.11"          ]
         [quantum/org.clojure.core.rrb-vector   "0.0.12"          ]
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
         [org.clojure/tools.reader              "1.0.0-beta3"    ]
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
         [com.cognitect/transit-cljs            "0.8.239"         ]
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
         [fipp                                  "0.6.6"
           :exclusions [org.clojure/core.rrb-vector]]
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
         [figwheel                              "0.5.4-7"         ]
         #_[binaryage/devtools                  "0.5.2"           ]
         [environ  "1.0.3"  ]
     ; ==== DB ====
       ; DATOMIC
      #_[quantum/datomic-pro                     "0.9.5206" ; Doesn't work, apparnetly
         :exclusions [joda-time
                      org.slf4j/slf4j-nop
                      org.slf4j/log4j-over-slf4j
                      org.slf4j/jul-to-slf4j
                      org.slf4j/jcl-over-slf4j
                      org.codehaus.janino/commons-compiler-jdk]   ]
       [com.datomic/datomic-free                "0.9.5394"
         :exclusions [org.slf4j/slf4j-nop
                      org.slf4j/log4j-over-slf4j
                      org.jboss.logging/jboss-logging]]
       [datascript                              "0.15.0"          ]
       [datascript-transit                      "0.2.0"
         :exclusions [com.cognitect/transit-cljs]                 ]
       [com.zachallaun/datomic-cljs             "0.0.1-alpha-1"   ]
       [posh                                    "0.3.5"           ]
       [quantum/datsync                         "0.0.1-4-11-2016"
         :exclusions [org.slf4j/slf4j-nop
                      org.clojure/core.match]]
       [re-frame                                "0.8.0-alpha11"   ]
     ; ==== HTML ====
       [hickory                                 "0.6.0"           ]
     ; ==== INTEROP ====
       [org.python/jython-standalone            "2.5.3"
         :exclusions [jline]]
     ; ==== LOGGING ====
       [org.slf4j/slf4j-log4j12                 "1.7.21"          ]
       [org.slf4j/jul-to-slf4j                  "1.7.21"          ]
       [org.slf4j/jcl-over-slf4j                "1.7.21"          ]
     ; ==== UI ====
       ; FORM
       [fx-clj                                  "0.2.0-alpha1"
         :exclusions [potemkin]                                   ]
       [reagent                                 "0.6.0-rc"
         :exclusions [org.json/json]                              ]
       ;[domina                                 "1.0.3"           ] ; DOM manipulation
       ; STYLE
       [garden                                  "1.3.2"           ]
     ; ==== UUID ====
       [com.lucasbradstreet/cljs-uuid-utils     "1.0.2"           ]
       [danlentz/clj-uuid                       "0.1.6"           ]
     ; ==== HTTP ====
       [com.taoensso/sente                      "1.8.1"             ; WebSockets
         :exclusions [com.taoensso/encore]                        ]
       [clj-http                                "3.1.0"
         :exclusions [riddley
                      cheshire
                      org.json/json
                      com.fasterxml.jackson.core/jackson-core
                      commons-codec
                      potemkin]                                   ]
       [cljs-http                               "0.1.41"
         :exclusions [com.cognitect/transit-cljs]]
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
         :exclusions [clj-tuple
                      ch.qos.logback/logback-classic
                      org.jboss.logging/jboss-logging]  ]
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
     ; ==== APIS ==== ;
     [com.amazonaws/aws-java-sdk                "1.11.32"
       :exclusions [com.fasterxml.jackson.core/jackson-databind]]
     ; ==== PARSING ==== ;
     [instaparse                                "1.4.2"           ]
     [com.lucasbradstreet/instaparse-cljs       "1.4.1.2"         ]
     [automat                                   "0.2.0"           ]
     ; ==== MATCH ====
     [org.clojure/core.match                    "0.3.0-alpha4"    ]
     [net.cgrand/seqexp                         "0.6.0"]
     ; ==== CODE TRANSFORMATION ====
       ; META (CODE)
       ;[repetition-hunter                      "1.0.0"           ]
       ; COMPILE/TRANSPILE
       [org.eclipse.jdt/org.eclipse.jdt.core    "3.10.0"          ] ; Format Java source code
       [com.github.javaparser/javaparser-core   "2.5.1"           ] ; Parse Java source code
       [org.clojure/tools.emitter.jvm           "0.1.0-beta5"
         :exclusions [org.ow2.asm/*]]
       [org.clojure/tools.analyzer              "0.6.9"           ]
       [org.clojure/jvm.tools.analyzer          "0.6.1"           ]
      ;[org.clojure/tools.analyzer.js           "0.1.0-beta5"     ] ; Broken
       [cljfmt                                  "0.5.5"           ]
     ; METADATA EXTRACTION/PARSING
     [org.apache.tika/tika-parsers              "1.13"
       :exclusions [org.apache.poi/poi org.apache.poi/poi-ooxml
                    org.ow2.asm/*]]
     ; DATAGRID
     [org.apache.poi/poi                        "3.14"            ]
     [org.apache.poi/poi-ooxml                  "3.14"            ] ; Conflicts with QB WebConnector stuff (?) as well as HTMLUnit (org.w3c.dom.ElementTraversal)
     ; AUDIO
     ; [net.sourceforge.jvstwrapper/jVSTwRapper "0.9g"            ] ; Creating audio plugin
     ; [net.sourceforge.jvstwrapper/jVSTsYstem  "0.9g"            ] ; Creating audio plugin
     ; TESTING
     [org.clojure/test.generative               "0.5.2"           ]
     [org.clojure/test.check                    "0.9.0"           ]
     [lein-doo                                  "0.1.7"           ]
     ; ==== MULTIPLE ====
     ; COMPRESSION, HASHING...
     [byte-transforms                           "0.1.4"
       :exclusions [org.xerial.snappy/snappy-java]]
     [net.jpountz.lz4/lz4                       "1.3"             ]
     ; ==== MAP-REDUCE ====
     [gorillalabs/sparkling                     "1.2.5"
       :exclusions [org.ow2.asm/*
                    com.esotericsoftware.reflectasm/reflectasm]]
       [org.apache.spark/spark-core_2.10        "1.6.1"
         :exclusions [com.google.inject/guice
                      org.xerial.snappy/snappy-java
                      asm
                      jline]]
       [com.github.fommil.netlib/all            "1.1.2"
         :extension "pom"]
       [com.googlecode.matrix-toolkits-java/mtj "1.0.2"]
       [org.apache.spark/spark-mllib_2.10       "1.6.1"
         :exclusions [com.google.inject/guice
                      org.slf4j/jcl-over-slf4j
                      org.xerial.snappy/snappy-java
                      org.scalamacros/quasiquotes_2.10
                      org.codehaus.janino/commons-compiler]]
     ; ==== DEPENDENCY-CONFLICTED ====
     ; quantum/datomic-pro
     ; spark
     [org.codehaus.janino/commons-compiler-jdk "2.6.1"   ]
     ; byte-transforms
     ; spark
     [org.xerial.snappy/snappy-java            "1.1.1.7" ]
     ; many
     [potemkin                                 "0.4.3"   ]
     ; com.datomic/datomic-free
     ; org.immutant/web
     [org.jboss.logging/jboss-logging          "3.1.0.GA"]
     ; com.datomic/datomic-free
     ; org.immutant/web
     [ch.qos.logback/logback-classic           "1.1.7"
       :exclusions [org.slf4j/*]                         ]
     ; org.ow2.asm/*
     ;   org.clojure/tools.emitter.jvm
     ;   org.apache.tika/tika-parsers
     ;   co.paralleluniverse/pulsar
     ;   org.apache.spark/spark-core_2.10
     ; co.paralleluniverse/pulsar
     ; gorillalabs/sparkling
     [com.esotericsoftware/reflectasm          "1.11.3"  ] ; >= org.ow2.asm/all 4.2 needed by org.clojure/tools.emitter.jvm
     [jline                                    "2.12.1"  ]] ; Even though 3.0.0 is available
   :profiles
   {:dev {:resource-paths ["dev-resources"]
          :source-paths   ["dev/cljc"]
          :dependencies   []
          :plugins [[com.jakemccrary/lein-test-refresh "0.16.0"] ; CLJ  test
                    [lein-doo                          "0.1.7" ] ; CLJS test
                    [lein-cljsbuild                    "1.1.4" ]
                    [lein-figwheel "0.5.4-7"
                      :exclusions [org.clojure/clojure
                                   org.clojure/clojurescript
                                   org.clojure/core.async
                                   org.clojure/core.cache]]
                    [jonase/eastwood                   "0.2.1"]
                    [lein-cloverage                    "1.0.6"]
                    [quantum/lein-vanity               "0.3.0-quantum"]
                    [lein-ancient                      "0.6.10"
                      :exclusions [com.amazonaws/aws-java-sdk-s3]]
                    ]
    :fibers
       {:java-agents [[co.paralleluniverse/quasar-core "0.7.6"]]}
    :auto-instrument
       {:jvm-opts ["-Dco.paralleluniverse.pulsar.instrument.auto=all"]
        :java-agents [[co.paralleluniverse/quasar-core "0.7.6" :options "m"]]}}
    :test {:jvm-opts ["-Xmx3g"]}}
  :aliases {"all"                    ["with-profile" "dev:dev,1.5:dev,1.7"]
            "deploy-dev"             ["do" "clean,"
                                           "install"]
            "deploy-prod"            ["do" "clean,"
                                           "install,"
                                           "deploy" "clojars"]
            "deploy-test-dev"        ["do" "clean,"
                                           "cljsbuild" "once" "dev"]
            "cljs:autobuilder"       ["do" "clean,"
                                           "figwheel" "dev"]
            "cljs:debug:autobuilder" ["do" "clean,"
                                           "cljsbuild" "auto" "debug"]
            "test:clj"               ["with-profile" "+test"
                                      "test"]
            "test:cljs"              ["with-profile" "+test"
                                      "doo" "phantom" "test" "once"]
            "clj:autotester"         ["do" "clean,"
                                           "test-refresh"]
            "count-loc"              ["vanity"]
            "clj:fast-repl"          ["trampoline" "run" "-m" "clojure.main"]}
  :auto-clean  false
  :target-path "target"
  :clean-targets ^{:protect false} [:target-path
                                    [:cljsbuild :builds :test :compiler :output-dir]
                                    [:cljsbuild :builds :test :compiler :output-to ]
                                    [:cljsbuild :builds :dev  :compiler :output-dir]
                                    [:cljsbuild :builds :dev  :compiler :output-to ]
                                    [:cljsbuild :builds :min  :compiler :output-dir]
                                    [:cljsbuild :builds :min  :compiler :output-to ]]
  :java-source-paths ["src/java"]
  :source-paths      ["src/clj"
                      "src/cljc"]
  ;:resource-paths ["resources"] ; important for Figwheel
  :test-paths     ["test/cljs" "test/clj" "test/cljc"]
  :repl-options {:init (do (clojure.core/require 'quantum.core.print)
                           (require 'alembic.still)
                           (clojure.main/repl
                             :print  quantum.core.print/!
                             :caught quantum.core.print/!))}
  :global-vars {*warn-on-reflection* true
                *unchecked-math*     :warn-on-boxed}
  :java-agents [; This for HTTP/2 support
                #_[kr.motd.javaagent/jetty-alpn-agent "1.0.1.Final"]]
  :cljsbuild
    {:builds
      {:test {:source-paths ["src/cljs"  "src/cljc"
                             "dev/cljs"  "dev/cljc"
                             "test/cljs" "test/cljc"]
              :compiler     {:output-to            "dev-resources/public/js/test-compiled/quantum.js"
                             :output-dir           "dev-resources/public/js/test-compiled/out"
                             :optimizations        :whitespace
                             :main                 quantum.test
                             :asset-path           "js/test-compiled/out"
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
  :figwheel {:http-server-root "public" ; default and assumes "resources"
             :server-port      3450
             :css-dirs         ["dev-resources/public/css"]})
