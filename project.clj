(defproject quantum/core "0.2.4.9-beta"
  :version-history
    {"0.2.4.6" #{:stable :clj :cljs}
     "0.2.4.7" #{:abandoned}
     "0.2.4.8" #{:lib}}
  :todos ["Eliminate boxed math and reflection warnings"]
  :description      "Some quanta of computational abstraction, assembled."
  :jvm-opts         []
  ;:aot :all ;[quantum.core.macros] ; "^:skip-aot" doesn't work; same with regexes
  :java-source-paths ["src/java"]
  :jar-name         "quantum-dep.jar"
  :uberjar-name     "quantum.jar"
  :url              "https://www.github.com/alexandergunnarson/quantum"
  :license          {:name "Creative Commons Attribution-ShareAlike 3.0 US (CC-SA) license"
                     :url "https://creativecommons.org/licenses/by-sa/3.0/us/"}
  ; :signing          {:gpg-key "72F3C25A"}
  #_:deploy-repositories #_[;["releases" :clojars]
                        ["clojars" {:creds :gpg}]
                        []]
  :repositories {"sonatype-oss-public"
                 "https://oss.sonatype.org/content/groups/public/"
                 #_"aws-releases" #_{:url "s3p://quanta-cloud/releases/" 
                                 ;:username :env
                                 ;:passphrase :env
                                 :creds :gpg}}
  :plugins [#_[s3-wagon-private                "1.1.2"]]
  :dependencies
    [; ==== CLOJURE ====
     ; CLOJURE CORE
     [org.clojure/clojure       #_"1.7.0" "1.8.0-alpha2"] ; July 16th
     [quantum/ns "1.0"]
     ; CORE
     [proteus                           "0.1.4" ]

     ; [com.github.detro.ghostdriver/phantomjsdriver "1.1.0"]
     ; ; ==== SERVER ====
     [compojure "1.3.3"
       :exclusions [org.eclipse.jetty/jetty-server
                    org.eclipse.jetty/jetty-servlet
                    javax.servlet/servlet-api
                    clj-http]]
     ; [ring/ring-jetty-adapter   "1.2.2"         ]
     ; [environ                   "0.5.0"         ]
     
     ; ==== DB ====
     ; DATOMIC
        [com.datomic/datomic-pro "0.9.5206" :exclusions [joda-time]]
     ; ==== CORE ====
       ; ==== COLLECTIONS ====
         ; CORE
         [seqspert "1.7.0-alpha6.1.0"]
         [it.unimi.dsi/fastutil "7.0.7"]
       ; ==== UTIL ====
         ; DEBUG
         [clj-stacktrace "0.2.8"]
       ; ==== STRING ====
         ; REGEX
         [frak "0.1.6"]
       ; MACROS
       [riddley "0.1.10"]
       
     ; ==== UI ====
     [fx-clj "0.2.0-alpha1" :exclusions [potemkin]] ; 0.2.0-SNAPSHOT
     ; DATA IN GENERAL
     ;[clj-tuple                        "0.2.0"]
     [fast-zip "0.6.1"]
     [org.clojure/math.combinatorics "0.1.1"]
     [com.carrotsearch/hppc "0.7.1"] ; High performance primitive collections for Java
     ; DATA.VECTOR
     [org.clojure/core.rrb-vector       "0.0.11"]
     [org.clojure/data.finger-tree      "0.0.2" ]
     ; DATA.MAP; DATA.SET     
     [org.flatland/ordered              "1.5.2" ]
     [org.clojure/data.avl              "0.0.12"]
     [org.clojure/data.int-map          "0.2.1" ]
     ; DATA.CSV             
     [org.clojure/data.csv              "0.1.2"]
     ; DATA.XML
     [org.clojure/data.xml              "0.0.8" :exclusions [org.clojure/clojure]]
     ; DATA.JSON
     [cheshire                          "5.3.1"] ; for oauth-clj; uses Jackson 2.3.1 ; JSON parsing
     ; GRAPHS
     [aysylu/loom "0.5.0"]
     ; CONVERT
     [byte-streams "0.2.0"]
     #_[gloss "0.2.5"] 
     ; MACROS
     #_[potemkin                          "0.3.11"  ; defprotocol+, definterface+, etc.
       :exclusions [riddley]]
     ; PRINT      
     [fipp                              "0.6.2"       ]
     ; NUMERIC                   
     ;[primitive-math                    "0.1.3"]
     ; TIME
     #_[clj-time                          "0.7.0"       ] ; similar to JODA time
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
     [iota                              "1.1.2"       
       :exclusions [org.codehaus.jsr166-mirror/jsr166y org.clojure/clojure]]  ; fast/efficient string IO manipulation
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

     [com.carrotsearch/java-sizeof "0.0.5"] ; Get size of Java Objects
     [com.github.javaparser/javaparser-core "2.1.0"] ; Parse Java

     [org.eclipse.jetty/jetty-server "9.2.10.v20150310"]
     [org.eclipse.jetty/jetty-server "9.2.10.v20150310"]
     
     ; AUDIO
     ; [net.sourceforge.jvstwrapper/jVSTwRapper "0.9g"] ; Creating audio plugin
     ; [net.sourceforge.jvstwrapper/jVSTsYstem  "0.9g"] ; Creating audio plugin
     ; DATAGRID + EXCEL
     ; [org.apache.poi/poi          "3.9"] ; Conflicts with QB WebConnector stuff (?) as well as HTMLUnit (org.w3c.dom.ElementTraversal)
     ; [org.apache.poi/poi-ooxml    "3.9"] ; NOT INCLUDED
     ; CLASS
     [org.reflections/reflections "0.9.10"]
     ; WEB
     [com.github.detro/phantomjsdriver "1.2.0"
       :exclusions [xml-apis commons-codec]]

     ; CSS
     [garden                    "1.2.5"         ]
     [org.clojure/tools.namespace "0.2.11"]
     [com.stuartsierra/component  "0.2.3"      ]
     ; ===== MULTIPLE =====
     ; COMPRESSION, HASHING...
     [byte-transforms "0.1.3"]

     ; METADATA EXTRACTION/PARSING
     [org.apache.tika/tika-parsers "1.9"]
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
            :dependencies
              [#_[org.clojure/tools.namespace "0.2.11-SNAPSHOT"]]
            :plugins [;[codox "0.8.8"]
                      [lein-cljsbuild                  "1.0.5"]
                      [com.cemerick/clojurescript.test "0.3.1" :exclusions [org.json/json]]
                      [lein-figwheel                   "0.3.1"]
                      [jonase/eastwood                 "0.2.1"]
                      ]}}
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
  :global-vars {*warn-on-reflection* true}
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