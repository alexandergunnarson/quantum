(ns quantum.meta.project-base
  (:require
    [clojure.java.io        :as io]
    [clojure.pprint
      :refer [pprint]]
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
(def cljs-dependency '[org.clojure/clojurescript "1.10.312"])

(def latest-stable-quantum-version
  "fc7a78bc" ; stable for backend use; mainly stable for frontend
  #_"0.3.0-c7ed558e" ; unknown
  #_"0.3.0-f1a3dc08" ; unknown
  )

(def quantum-source-paths
  {:typed          "../quantum/src"
   :untyped        "../quantum/src-untyped"
   :posh           "../forks/posh/src"
   :re-frame-trace "../quantum/src-re-frame-trace"})

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

(defn with-profiles [profiles & args]
  (into ["with-profile" (->> profiles (map name) (str/join ","))] args))

(defn remove-nil-vals [m]
  (->> m
       (remove (fn [[_ v]] (nil? v)))
       (into {})))

(def base-config|quantum
  {;; ===== Dependencies ===== ;;
   :repositories
     [["Boundless" "http://repo.boundlessgeo.com/main/"]
      ["OSGeo"     "http://download.osgeo.org/webdav/geotools/"]]
   :dependencies
     '[; ==== CORE ====
       [proteus                                 "0.1.6"]
       ; ==== NAMESPACE ====
       [org.clojure/tools.namespace             "0.2.11"]
       [com.taoensso/encore                     "2.93.0"]
       ; ==== ASYNC ====
         [org.clojure/core.async                "0.3.465"]
         [servant                               "0.1.5"]
         [#_alexandergunnarson/co.paralleluniverse.pulsar co.paralleluniverse/pulsar #_"0.7.6.2" "0.7.6"
           :exclusions [org.slf4j/*
                        potemkin
                        org.clojure/core.match
                        org.ow2.asm/*
                        com.esotericsoftware/reflectasm]]
         [co.paralleluniverse/quasar-core       "0.7.6"
           :exclusions [com.esotericsoftware/reflectasm]]
       ; ==== quantum.core.data ====
         [org.dthume/data.interval-treeset      "0.1.2"           ]
         [com.carrotsearch/hppc                 "0.7.1"           ] ; High performance primitive collections for Java
         [it.unimi.dsi/fastutil                 "7.0.12"          ]
       #_[colt/colt                             "1.2.0"           ]
         [quantum/seqspert                      "1.7.0-alpha6.1.0"]
         [fast-zip                              "0.7.0"           ]
         ; VECTOR
       #_[org.clojure/core.rrb-vector           "0.0.11"]
         [quantum/org.clojure.core.rrb-vector   "0.0.12"]
         [org.clojure/data.finger-tree          "0.0.2"]
         ; MAP / SET
         ;; Superseded by `frankiesardo/linked` but for now frankiesardo/linked doesn't have e.g.
         ;; `.keySet` support so we keep `org.flatland/ordered` for Clojure
         [org.flatland/ordered                  "1.5.3"]
         [frankiesardo/linked                   "1.2.9"]
         [org.clojure/data.avl                  "0.0.13"]
         [org.clojure/data.int-map              "0.2.4"]
         ; ==== COMPLEX ====
           ; JSON
           [cheshire                            "5.6.1"] ; for oauth-clj; uses Jackson 2.3.1 ; JSON parsing
           ; CSV
           [org.clojure/data.csv                "0.1.3"]
           ; XML
           [org.clojure/data.xml                "0.0.8"
             :exclusions [org.clojure/clojure]]

       ; ==== SPECS ====
         [clojure-future-spec                   "1.9.0-beta4"
           :exclusions [org.clojure/clojure]]
         [expound                               "0.5.0"]
         [orchestra                             "2017.11.12-1"]
       ; ==== COLLECTIONS ====
         [diffit                                "1.0.0"]
       ; ==== CONVERT ====
         [byte-streams                          "0.2.2"]
         [org.clojure/tools.reader              "1.1.1"]
         #_[gloss                               "0.2.5"           ]
       ; ==== CRYPTOGRAPHY ====
         [com.lambdaworks/scrypt                "1.4.0"           ]
         [org.mindrot/jbcrypt                   "0.3m"            ]
         [commons-codec/commons-codec           "1.10"            ]
         [org.bouncycastle/bcprov-jdk15on       "1.54"            ]
       ; ==== ERROR ====
         [alexandergunnarson/slingshot          "0.14"            ]
         [bwo/conditions                        "0.1.0"
           :exclusions [slingshot]]
       ; ==== SYSTEM ====
       ; ==== GEO ====
       #_[org.geotools/gt-geotiff               "17.2"            ]
       #_[org.geotools/gt-referencing           "17.2"            ]
       #_[org.geotools/gt2-epsg-hsql            "2.5-M1"
           :exclusions [javax.units/jsr108]]
       ; ==== GRAPH ====
         [aysylu/loom                           "1.0.0"           ]
       ; ==== IO ====
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
         [riddley                               "0.1.14"]
         #_[potemkin                            "0.3.11"
           :exclusions [riddley]                                  ]
       ; ==== NUMERIC ====
         [net.jafama/jafama                     "2.1.0"           ]
         [com.gfredericks/goog-integer          "1.0.0"           ]
         [org.clojure/math.combinatorics        "0.1.3"           ]
         [net.mikera/core.matrix                "0.57.0"
           :exclusions [org.clojure/clojure]]
         [uncomplicate/neanderthal              "0.8.0"           ] ; BLAS
       ; ==== PRINT ====
         [fipp                                  "0.6.10"
           :exclusions [org.clojure/core.rrb-vector]]
       ; ==== RESOURCES ====
         [com.stuartsierra/component            "0.3.2"           ]
       ; ==== STRING ====
         [funcool/cuerdas                       "2.0.5"           ]
         ; REGEX
         [frak                                  "0.1.6"           ]
       ; ==== TIME ====
         [quantum/js-joda                       "1.3.0-2"         ]
         [quantum/js-joda-timezone              "1.0.0-2"         ]
       ; ==== VALIDATE ====
         [prismatic/schema                      "1.1.1"           ]
       ; ==== META ====
         ; BENCH
         [criterium                             "0.4.4"           ]
         ; DEBUG
         [clj-stacktrace                        "0.2.8"           ]
         [debugger                              "0.2.0"           ]
         ; REPL
         [figwheel                              "0.5.14"          ]
         [figwheel-sidecar                      "0.5.14"          ]
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
       [com.datomic/datomic-free                "0.9.5407"
         :exclusions [org.slf4j/slf4j-nop
                      org.slf4j/log4j-over-slf4j
                      org.jboss.logging/jboss-logging]]
       [datascript                              "0.15.5"          ]
       [datascript-transit                      "0.2.0"
         :exclusions [com.cognitect/transit-cljs]                 ]
       [quantum/posh                            "0.5.7"]
       [quantum/re-posh                         "0.1.6"]
       [quantum/datsync                         "0.0.1-4-11-2016"
         :exclusions [org.slf4j/slf4j-nop
                      org.clojure/core.match
                      io.netty/netty
                      com.datomic/datomic-free
                      posh]]
       [re-frame                                "0.10.4"]
     ; ==== HTML ==== ;
       [hickory                                 "0.6.0"]
     ; ==== INTEROP ==== ;
       [org.python/jython-standalone            "2.5.3"
         :exclusions [jline]]
     ; ==== LOGGING ==== ;
       [org.slf4j/slf4j-log4j12                 "1.7.21"          ]
       [org.slf4j/jul-to-slf4j                  "1.7.21"          ]
       [org.slf4j/jcl-over-slf4j                "1.7.21"          ]
     ; ==== PROFILING ==== ;
       [com.taoensso/tufte                      "1.1.1"]
     ; ==== UI ==== ;
       ; FORM
       [fx-clj                                  "0.2.0-alpha1"
         :exclusions [potemkin]                                   ]
       [reagent                                 "0.7.0"
         :exclusions [org.json/json]                              ]
       ;[domina                                 "1.0.3"           ] ; DOM manipulation
       ; STYLE
       [garden                                  "1.3.2"           ]
     ; ==== UUID ====
       [com.lucasbradstreet/cljs-uuid-utils     "1.0.2"           ]
       [danlentz/clj-uuid                       "0.1.6"           ]
     ; ==== HTTP ====
       [com.taoensso/sente                      "1.11.0"]             ; WebSockets
       [cljs-http                               "0.1.41"
         :exclusions [com.cognitect/transit-cljs]]
       [less-awful-ssl                          "1.0.1"]
       [http-kit                                "2.2.0"]
       [org.apache.httpcomponents/httpcore      "4.4.4"]
       [org.apache.httpcomponents/httpclient    "4.5.2"]
       [org.apache.httpcomponents/httpmime      "4.5.2"]
       ; ==== ROUTING ====
       [compojure                               "1.6.0"
         :exclusions [org.eclipse.jetty/jetty-server
                      org.eclipse.jetty/jetty-servlet
                      javax.servlet/servlet-api]]
       [org.eclipse.jetty/jetty-server          "9.4.0.M0"        ]
       [org.immutant/web                        "2.1.4"
         :exclusions [clj-tuple
                      ch.qos.logback/logback-classic
                      org.jboss.logging/jboss-logging]  ]
       [aleph                                   "0.4.3" ; but incompatible Netty dep with Spark 2.0.1
         :exclusions [primitive-math
                      io.netty/netty-all] #_"For Spark's sake"    ]
       [manifold "0.1.6"] ; for Aleph's sake
       ; ==== AUTH ====
       [com.cemerick/friend                     "0.2.1"
         :exclusions [org.clojure/core.cache]                     ]
       ; ==== MIDDLEWARE ====
       [ring/ring-defaults                      "0.2.0"           ]
       [bk/ring-gzip                            "0.1.1"           ]
     ; WEB
     [com.github.detro/phantomjsdriver          "1.2.0"
       :exclusions [xml-apis
                    io.netty/netty]]
     ; ==== APIS ==== ;
     [com.amazonaws/aws-java-sdk                "1.11.32"
       :exclusions [com.fasterxml.jackson.core/jackson-databind]]
     ; ==== PARSING ==== ;
     [instaparse                                "1.4.2"           ]
     [com.lucasbradstreet/instaparse-cljs       "1.4.1.2"         ]
     [automat                                   "0.2.0"           ]
     ; ==== MATCH ====
     [org.clojure/core.match                    "0.3.0-alpha4"    ]
     [net.cgrand/seqexp                         "0.6.2"]
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
                    org.ow2.asm/* javax.measure/jsr-275]]
     ; DATAGRID
     [org.apache.poi/poi                        "3.14"            ]
     [org.apache.poi/poi-ooxml                  "3.14"            ] ; Conflicts with QB WebConnector stuff (?) as well as HTMLUnit (org.w3c.dom.ElementTraversal)
     ; PDF
     [org.apache.pdfbox/pdfbox                  "2.0.3"           ]
     ; OCR
     [net.sourceforge.tess4j/tess4j             "3.3.1"
       :exclusions [org.slf4j/slf4j-api
                    org.slf4j/jul-to-slf4j
                    org.slf4j/jcl-over-slf4j
                    org.slf4j/log4j-over-slf4j
                    ch.qos.logback/logback-classic]]
     ; AUDIO
     [uk.co.xfactory-librarians/coremidi4j      "0.9"             ] ; Improved MIDI
     ; TESTING
     [org.clojure/test.generative               "0.5.2"           ]
     [org.clojure/test.check                    "0.9.0"           ]
     [lein-doo                                  "0.1.7"           ]
     ; HASHING
     [info.debatty/java-lsh                     "0.10"            ]
     ; ==== MULTIPLE ====
     ; COMPRESSION, HASHING...
     [byte-transforms                           "0.1.4"
       :exclusions [org.xerial.snappy/snappy-java]]
     [net.jpountz.lz4/lz4                       "1.3"             ]
     [com.github.haifengl/smile-core            "1.2.0"           ]
     ; ===== NLP ===== ;
     [edu.stanford.nlp/stanford-corenlp         "3.7.0"]
     [edu.stanford.nlp/stanford-corenlp         "3.7.0"
       :classifier models]
     ; ===== MAP-REDUCE ===== ;
     [gorillalabs/sparkling                     "1.2.5"
       :exclusions [org.ow2.asm/*
                    com.esotericsoftware.reflectasm/reflectasm]]
       [org.apache.spark/spark-core_2.11        "2.0.1"  #_"1.6.1" ; problematic netty
         :exclusions [com.google.inject/guice
                      org.xerial.snappy/snappy-java
                      asm
                      jline
                      #_io.netty/netty
                      io.netty/netty-all]]
       [com.github.fommil.netlib/all            "1.1.2"
         :extension "pom"]
       [com.googlecode.matrix-toolkits-java/mtj "1.0.2"]
       [org.apache.spark/spark-mllib_2.11       "2.0.1" #_"1.6.1"
         :exclusions [com.google.inject/guice
                      org.slf4j/jcl-over-slf4j
                      org.xerial.snappy/snappy-java
                      org.scalamacros/quasiquotes_2.11
                      org.codehaus.janino/commons-compiler
                      io.netty/netty]]
     ; ==== DEPENDENCY-CONFLICTED ====
     ; (various)
     [org.clojure/data.priority-map            "0.0.7"]
     [commons-io/commons-io                    "2.6"]
     [org.apache.commons/commons-lang3         "3.7"]
     [commons-net/commons-net                  "3.6"]
     [com.google.protobuf/protobuf-java        "3.5.1"]
     ; org.clojure/clojurescript
     ; co.paralleluniverse/quasar-core
     [com.google.guava/guava                   "23.6-jre"]
     ; hickory
     ; org.apache.tika/tika-parsers
     [org.jsoup/jsoup                          "1.11.2"]
     ; quantum/datomic-pro
     ; spark
     [org.codehaus.janino/commons-compiler-jdk "2.7.4" #_"2.6.1"]
     ; byte-transforms
     ; spark
     [org.xerial.snappy/snappy-java            "1.1.1.7"]
     ; (various)
     [potemkin                                 "0.4.3"]
     ; aleph
     ; org.apache.spark/spark-core_2.10
     ; org.apache.spark/spark-mllib_2.10
     ; quantum/datsync
     ; com.github.detro/phantomjsdriver
     [io.netty/netty-all                       "4.1.0.CR3"  #_"4.1.6.Final"] ; 4.0.29.Final is Required version for Spark 2
     ; com.datomic/datomic-free
     ; org.immutant/web
     [org.jboss.logging/jboss-logging          "3.1.0.GA"]
     ; com.datomic/datomic-free
     ; org.immutant/web
     [ch.qos.logback/logback-classic           "1.1.7"
       :exclusions [org.slf4j/*]]
     ; org.ow2.asm/*
     ;   org.clojure/tools.emitter.jvm
     ;   org.apache.tika/tika-parsers
     ;   co.paralleluniverse/pulsar
     ;   org.apache.spark/spark-core_2.10
     ; co.paralleluniverse/pulsar
     ; gorillalabs/sparkling
     [com.esotericsoftware/reflectasm          "1.11.3"  ] ; >= org.ow2.asm/all 4.2 needed by org.clojure/tools.emitter.jvm
     [jline                                    "2.12.1"  ] ; Even though 3.0.0 is available
     [com.google.guava/guava                   "23.5-jre"]
     [com.google.protobuf/protobuf-java        "3.5.0"]]
   ;; ===== Compilation ===== ;;
   :aot '[sparkling.serialization sparkling.destructuring]
   ;; ===== REPL ===== ;;
   :repl-options
     {:init
       '(do (require
              '[no.disassemble :refer [disassemble]]
              'quantum.untyped.core.error
              'quantum.untyped.core.meta.debug
              'quantum.untyped.core.print
              'quantum.untyped.core.print.prettier
              '[quantum.untyped.core.log :refer [prl!]])
            (quantum.untyped.core.print.prettier/extend-pretty-printing!)
            (reset! quantum.untyped.core.error/*pr-data-to-str? true)
            ;; For use with Atom's Proto-REPL
            ;; Interned in `clojure.core` in order to not be clobbered by `refresh`
            (intern 'clojure.core 'atom|proto-repl|print-fn
                    (atom #(binding [*print-meta* true
                                     quantum.untyped.core.print/*collapse-symbols?* true
                                     quantum.untyped.core.print/*print-as-code?* true]
                             (quantum.untyped.core.print/ppr %))))
            (intern 'clojure.core 'atom|proto-repl|print-err-fn
                    (atom #(quantum.untyped.core.print/ppr-error %)))
            (quantum.untyped.core.meta.debug/print-pretty-exceptions!)
          #_(clojure.main/repl :print ... :caught ...))}})

(defn >cljsbuild-builds
  "Note that for Figwheel to work, no character in the build IDs can necessitate an
   URL escape character."
  [kind project-config opts source-paths artifact-base-name]
  (let [react-native?    (-> opts :features :react-native)
        quantum?         (-> project-config :name (= 'quantum/core))
        id>config
          (fn [id #_string?, id-base id-suffix]
            (let [;; TODO these paths are temporary for React Native!!
                  server-root-path "resources/server-root"
                  ;; relative to `server-root-path`
                  asset-path (str "generated"
                                  "/" (name kind)
                                  "/" id
                                  "/" "js")
                  output-dir (str server-root-path "/" asset-path)]
              (cond->
                {:source-paths
                  (vec
                    (concat source-paths
                      (case id-suffix
                        :quantum-dynamic-source
                          [(:typed   quantum-source-paths)
                           (:untyped quantum-source-paths)
                           #_(:posh    quantum-source-paths)]
                        :quantum-dynamic-source-untyped
                          [(:untyped quantum-source-paths)
                           #_(:posh    quantum-source-paths)]
                        :re-frame-trace
                          (cond->
                            [#_(:posh  quantum-source-paths)
                             (if quantum?
                                 "./src-re-frame-trace"
                                 (:re-frame-trace quantum-source-paths))]
                            (not quantum?) (conj (:untyped quantum-source-paths)))
                        nil)))
                 :compiler
                   (cond->
                     (merge
                       {:main       (str artifact-base-name "." (case kind :prod "system" (name kind)))
                        :asset-path asset-path
                        :output-dir output-dir
                        :output-to  (str output-dir "/" "main.js")
                        :optimizations
                          (case kind
                            :dev  :none
                            :prod :advanced #_"`:simple` might be required for React Native?"
                            :test :whitespace
                            :none)}
                       (case id-suffix
                         :re-frame-trace {:preloads '[day8.re-frame.trace.preload]}
                         nil)
                       (case kind
                         (:test :prod) {:closure-defines {"goog.DEBUG" false}}
                         nil)
                       (case kind
                         :prod {:static-fns         true
                                :optimize-constants true
                                :pretty-print       false
                              #_:parallel-build   #_true}
                         :test {:cache-analysis     true}
                         nil))
                     (= id-suffix :re-frame-trace)
                       (assoc-in [:closure-defines "re_frame.trace.trace_enabled_QMARK_"] true))}
                (= kind :dev)
                  (assoc :figwheel
                    {:load-warninged-code true
                   #_:websocket-url     #_"wss://[[client-hostname]]:443/figwheel-ws"}))))
        id-suffixes
          (cond-> #{:re-frame-trace}
            (not quantum?) (conj :quantum-dynamic-source
                                 :quantum-dynamic-source-untyped))
        id-bases>configs
          (fn [id-bases]
            (->> id-bases
                 (map (fn [id-base]
                        (->> (conj id-suffixes nil) ; for default non-suffixed version
                             (map (fn [id-suffix]
                                    (let [id (str (name id-base) (some->> id-suffix name (str "-")))]
                                      [id (id>config id id-base id-suffix)]))))))
                 (apply concat)
                 (into {})))]
    (id-bases>configs (cond-> #{:web} react-native? (conj :ios :android)))))

(defn >default-config [opts project-config]
  (let [artifact-base-name
          (or (:artifact-base-name opts)
              (str (some-> project-config :name namespace (str "-"))
                   (-> project-config :name name)))
        quantum?      (-> project-config :name (= 'quantum/core))
        react-native? (-> opts :features :react-native)
        gb>mb    #(-> % (* 1024) long)
        gb>bytes #(-> % (* 1024) ; kB
                        (* 1024) ; MB
                        (* 1024) ; GB
                        long)
        system-type (System/getenv "quantum.system-type")
        aws-system-type?
          #{"t2.micro" "t2.small"
            "c4.4xlarge"
            "m4.2xlarge-combo"}
        ;; http://docs.oracle.com/javase/8/docs/technotes/tools/unix/java.html
        ;; http://blog.ndk.io/2014/02/11/jvm-slow-startup.html
        ;; http://blog.ndk.io/2014/03/19/solving-clojure-boot-time.html
        ;; "Tuning JVM Garbage Collection for Production Deployments": https://docs.oracle.com/cd/E40972_01/doc.70/e40973/cnf_jvmgc.htm#autoId2
        ;; TODO automatically determine based on system/container specs
        >jvm-opts
          (fn [profile-id]
            (let [total-ram-mb
                    (case system-type
                      "t2.micro"             (gb>mb 1)
                      "t2.small"             (gb>mb 2)
                      "c4.4xlarge"           (gb>mb 30)
                      "m4.2xlarge-combo"     (gb>mb 32)
                      "macbook-pro-16gb-ram" (gb>mb 16)
                      ;; A decent minimal assumption
                      (gb>mb 4))
                  ;; RAM used by the OS and other processes not otherwise accounted for below
                  os-ram-used-mb
                    (case system-type
                      ;; Just an assumption that works
                      "t2.small"             312
                      "c4.4xlarge"           (gb>mb 4)
                      "m4.2xlarge-combo"     (gb>mb 4)
                      "macbook-pro-16gb-ram" (gb>mb 6)
                      ;; Assume half-used
                      (gb>mb 2))
                  datomic-transactor-used-mb
                    (if (aws-system-type? system-type)
                        512 ; TODO not right
                        0)
                  app-mb
                    (case system-type
                      "m4.2xlarge-combo" (gb>mb 10) ; TODO not right but previously assumed
                      "c4.4xlarge"       (gb>mb 10) ; TODO not right but previously assumed
                      0) ; TODO not right
                  ;; Min heap must be 750M (?) for purposes of Datomic peer connection
                  ;; `(and (> Xmx32G) (<= Xmx38G))` is worse performance: http://java-performance.com
                  heap-mb
                    (case system-type
                      "t2.micro"             768
                      "t2.small"             768
                      "m2.small"             (gb>mb 3)
                      "m4.2xlarge-combo"     (- total-ram-mb os-ram-used-mb datomic-transactor-used-mb)
                      "c4.4xlarge"           (- total-ram-mb os-ram-used-mb datomic-transactor-used-mb)
                      "test"                 (gb>mb 4)
                      "macbook-pro-16gb-ram" (gb>mb 10)
                      (- total-ram-mb os-ram-used-mb datomic-transactor-used-mb))]
               #_"For Java for t2.small:
                    offheap = ((72M code)+(128M direct)+(1024M meta) = 968M)
                  + onheap  = (2048M-OS-offheap = 1024M) -> 768M"
            (->> [(str "-Dquantum.core.system|profile=" (name profile-id))
                  ;; ----- Memory ----- ;;
                  ;; On-heap
                  (str "-Xms" heap-mb "M")
                  (str "-Xmx" heap-mb "M")
                  ;; Off-heap
                  (str "-XX:InitialCodeCacheSize="
                    (case system-type
                      "t2.small" 72
                      512)
                    "M")
                  (str "-XX:ReservedCodeCacheSize="
                    (case system-type
                      "t2.small" 72
                      512)
                    "M")
                  #_"-XX:CompressedClassSpaceSize=200M" #_"64M is too little"
                  (case system-type
                    ;; or else `java.lang.OutOfMemoryError: Metaspace`
                    "t2.small" "-XX:-UseCompressedClassPointers"
                    nil)
                  (str "-XX:MaxDirectMemorySize="
                    (case system-type
                      "t2.small" 128
                      512)
                    "M")#_"For e.g. off-heap `ByteBuffer`s"
                  (str "-XX:MetaspaceSize="
                    (case system-type
                      "t2.small" 768
                      1024)
                    "M")
                  (str "-XX:MaxMetaspaceSize="
                    (case system-type
                      ;; "128M and 256M, and 512M (!) are too little"
                      "t2.small" 768
                      1024)
                    "M")
                  "-XX:+CMSClassUnloadingEnabled" ; A must for Clojure if using runtime `eval`
                  ;; ----- Garbage Collection (GC) ----- ;;
                  ;; G1GC is okay with Java 8u40+
                  ;; "-Xms750m" because you have to have something there...
                  ;; (if (= system-type "t2.micro") "-Xms750m" "-XX:+UseG1GC"            )
                  ;; (if (= system-type "t2.micro") "-Xms750m" "-XX:MaxGCPauseMillis=200")
                  ;; (if (= system-type "t2.micro") "-Xms750m" "-XX:ParallelGCThreads=20")
                  ;; (if (= system-type "t2.micro") "-Xms750m" "-XX:ConcGCThreads=5")
                  ;; (if (= system-type "t2.micro") "-Xms750m" "-XX:InitiatingHeapOccupancyPercent=70")
                  ;; (if (= system-type "t2.micro") "-Xms750m" "-XX:G1HeapRegionSize=4m")
                  ;; (if (= system-type "t2.micro") "-Xms750m" "-XX:+UseStringDeduplication")
                  ;; Parallel GC is generally a better choice for applications favoring throughput over latency.
                  #_"-XX:+UseParallelGC" ; https://www.voxxed.com/blog/2015/08/whats-fastest-garbage-collector-java-8-heavy-calculations/
                  ;; But this is better for latency according to http://blog.sokolenko.me/2014/11/javavm-options-production.html
                  "-XX:+UseConcMarkSweepGC"
                  "-XX:+CMSParallelRemarkEnabled"
                  "-XX:+UseCMSInitiatingOccupancyOnly"
                  "-XX:CMSInitiatingOccupancyFraction=70"
                  "-XX:+ScavengeBeforeFullGC"
                  "-XX:+CMSScavengeBeforeRemark"
                  ;; ----- String interning ----- ;;
                  ;; http://java-performance.info/string-intern-in-java-6-7-8/
                  ;; You must set a higher -XX:StringTableSize value (compared to the default 1009)
                  ;; if you intend to actively use String.intern() – otherwise this will soon degrade to
                  ;; a linked list performance.
                  ;; ----- Telemetry ----- ;;
                  "-XX:-OmitStackTraceInFastThrow"
                  "-XX:ErrorFile=./JVMErrorDump.log"
                  "-Dquantum.core.log|out-file=./out.log"
                  "-Dquantum.core.log|print-to-stderror=false"
                  ;; ----- Compilation ----- ;;
                   #_(case system-type
                       "t2.micro"
                         "-XX:-AggressiveOpts"
                       "-XX:+AggressiveOpts") ; Aggressive JIT, but this can cause problems
                  "-XX:+TieredCompilation"
                  ;;"-XX:CICompilerCount=8" ; No waiting around for compilation
                  ;; ----- JIT ----- ;;
                  "-XX:+UseSuperWord"
                  "-XX:+Inline"
                  "-XX:+OptimizeStringConcat"
                  "-XX:+DoEscapeAnalysis"
                  "-XX:+UseCompressedOops" ; Typically increases performance when running the application with Java heap sizes less than 32 GB.
                  ;; settings to play around with, maybe
                #_"-XX:MaxTrivialSize=12"
                #_"-XX:MaxInlineSize=270"
                #_"-XX:InlineSmallCode=2000"
                  ;; ----- Parallelization ----- ;;
                  ;; Some applications with significant amounts of uncontended
                  ;; synchronization may attain significant speedups with this flag
                  ;; enabled, whereas applications with certain patterns of locking
                  ;; may see slowdowns.
                  "-XX:-UseBiasedLocking" ; ForkJoin wants this, according to Pulsar
                  ;; Should only be used on machines with multiple sockets,
                  ;; where it will increase performance of Java applications that
                  ;; rely heavily on concurrent operations.
                  "-XX:+UseCondCardMark" ; ForkJoin wants this, according to Pulsar
                  ;; ----- Cryptography ----- ;;
                  "-XX:+UseAES" "-XX:+UseAESIntrinsics"
                  "-XX:+UseSHA" "-XX:+UseSHA1Intrinsics" "-XX:+UseSHA256Intrinsics" "-XX:+UseSHA512Intrinsics"
                  ;; ----- Datomic ----- ;;
                  "-Ddatomic.txTimeoutMsec=20000"
                  (str "-Ddatomic.objectCacheMax="
                    (case system-type
                      "t2.micro"         (gb>bytes 0.2)
                                                                     ;; (10GB)  (6GB)
                      "m4.2xlarge-combo" (- total-ram-mb os-ram-used-mb app-mb datomic-transactor-used-mb)
                                                                     ;; (10GB)  (0GB)
                      "c4.4xlarge"       (- total-ram-mb os-ram-used-mb app-mb datomic-transactor-used-mb)
                      (gb>bytes 1)))
                  ;; ----- Fibers ----- ;;
                  ;; Just in case
                  #_"-Dco.paralleluniverse.fibers.verifyInstrumentation=true"]
                 (filterv some?))))]
    {;; ===== Meta ===== ;;
     :version      (>git-hash ".")
     :license      {:name "Creative Commons Attribution-ShareAlike 3.0 US (CC-SA) license"
                    :url  "https://creativecommons.org/licenses/by-sa/3.0/us/"}
     ;; ===== Environment ===== ;;
     :env env
     :jvm-opts ^:replace
       ["-d64" "-server"
        ;; ----- Process-specific ----- ;;
        "-Djava.util.logging.config.file=logging.properties" ; For silencing PhantomJS
        "-Dquantum.core.cryptography:secure-PRNG=true"]
     :global-vars '{*warn-on-reflection* true
                    *unchecked-math*     :warn-on-boxed}
     ;; ===== Dependencies ===== ;;
     :dependencies [clj-dependency cljs-dependency]
     ;; ===== Paths ===== ;;
     :target-path  "target"
     :test-paths   ["test"]
     :source-paths ["src"]
     :java-source-paths ["src-java"]
     ;; ===== Compilation ===== ;;
     :jar-name     (str artifact-base-name "-dep.jar")
     :uberjar-name (str artifact-base-name ".jar")
     :auto-clean   false
     :clean-targets ^{:protect false} [:target-path "./resources/server-root/generated/"]
     ;; ===== Plugins ===== ;;
     :plugins
       '[[lein-shell          "0.5.0"]
         ;; ----- Linting ----- ;;
         [jonase/eastwood     "0.2.1"]
         [lein-kibit          "0.1.2"]
         [lein-bikeshed       "0.4.1"]
         [lein-cloverage      "1.0.6"]
         ;; ----- Dependencies ----- ;;
         [lein-ancient        "0.6.10"
           :exclusions [com.amazonaws/aws-java-sdk-s3]]
         ;; ----- Utils ----- ;;
         [quantum/lein-vanity "0.3.0-quantum"]]
     ;; ===== Tasks ===== ;;
     :aliases
       (remove-nil-vals
         {"dependencies"
          (into ["do" "deps," "sudo" "npm"]
                (if react-native?
                    ["install"]
                    ["install,"
                     ;; When encountering a build error,
                     ;; https://github.com/lelandrichardson/react-native-maps/issues/371#issuecomment-231585153
                     "sudo" "rnpm" "link"]))
          ;; ----- Deployment ----- ;;
          "deploy|prod"
            (with-profiles (cond-> [:backend :frontend :prod :backend|prod :frontend|prod] (not quantum?) (conj :quantum|static-deps))
              "do" "clean,"
                   "install")
          "deploy|backend|dev"
            (with-profiles (cond-> [:backend :dev :backend|dev] (not quantum?) (conj :quantum|static-deps))
              "do" "clean,"
                   "install")
          "deploy|backend|prod"
            (with-profiles (cond-> [:backend :prod :backend|prod] (not quantum?) (conj :quantum|static-deps))
               "do" "clean,"
                    "install,"
                    "deploy" "clojars")
          "deploy|frontend|dev" ; accepts 1 arg: the target platform name
            (with-profiles (cond-> [:frontend :dev :frontend|dev] (not quantum?) (conj :quantum|static-deps))
              "do" "clean," "cljsbuild" "once")
          "deploy|frontend|dev|quantum-dynamic" ; accepts 1 arg: the target platform name
            (when-not quantum?
              (with-profiles [:frontend :dev :frontend|dev :quantum|dynamic-deps :quantum|dynamic-source]
                "do" "clean," "cljsbuild" "once"))
          "deploy|frontend|dev|quantum-dynamic-untyped" ; accepts 1 arg: the target platform name
            (when-not quantum?
              (with-profiles [:frontend :dev :frontend|dev :quantum|dynamic-deps :quantum|dynamic-source|untyped]
                "do" "clean," "cljsbuild" "once"))
          "deploy|frontend|prod" ; accepts 1 arg: the target platform name
            (with-profiles (cond-> [:frontend :prod :frontend|prod] (not quantum?) (conj :quantum|static-deps))
              "do" "clean," "cljsbuild" "once")
          ;; ----- Manual Builder (REPL) ----- ;;
          ;; backend should be done with `sudo LEIN_ROOT=1` prepended if it is to use port 80 or 443
          "repl|backend"
            (with-profiles (cond-> [:backend :dev :backend|dev] (not quantum?) (conj :quantum|static-deps))
              "trampoline" "run" "-m" "clojure.main")
          "repl|backend|quantum-dynamic"
            (with-profiles (cond-> [:backend :dev :backend|dev] (not quantum?) (conj :quantum|dynamic-deps :quantum|dynamic-source))
              "trampoline" "run" "-m" "clojure.main")
          "repl|frontend"
            (with-profiles [:frontend :dev :frontend|dev] "TODO")
          ;; ----- Autobuilder (Figwheel etc.) ----- ;;
          "autobuilder|frontend" ; accepts 1 arg: the target platform name
            (with-profiles (cond-> [:frontend :dev :frontend|dev] (not quantum?) (conj :quantum|static-deps)))
          "autobuilder|frontend|quantum-dynamic" ; accepts 1 arg: the target platform name
            (when-not quantum?
              (with-profiles [:frontend :dev :frontend|dev :quantum|dynamic-deps :quantum|dynamic-source]
                "figwheel"))
          "autobuilder|frontend|quantum-dynamic-untyped" ; accepts 1 arg: the target platform name
            (when-not quantum?
              (with-profiles [:frontend :dev :frontend|dev :quantum|dynamic-deps :quantum|dynamic-source|untyped]
                "figwheel"))
          "autobuilder|frontend|quantum-dynamic-untyped|re-frame-trace"
            (when-not quantum?
              (with-profiles [:frontend :dev :frontend|dev :frontend|dev|re-frame-trace :quantum|dynamic-deps :quantum|dynamic-source|untyped]
                "figwheel" "web-re-frame-trace"))
          "autobuilder|frontend|debug" ; accepts 1 arg: the target platform name
            (with-profiles (cond-> [:frontend :dev :frontend|dev] (not quantum?) (conj :quantum|static-deps))
              "cljsbuild" "auto")
          "autobuilder|frontend|debug|quantum-dynamic" ; accepts 1 arg: the target platform name
            (when-not quantum?
              (with-profiles [:frontend :dev :frontend|dev :quantum|dynamic-deps :quantum|dynamic-source]
                "cljsbuild" "auto"))
          "autobuilder|frontend|debug|quantum-dynamic-untyped" ; accepts 1 arg: the target platform name
            (when-not quantum?
              (with-profiles [:frontend :dev :frontend|dev :quantum|dynamic-deps :quantum|dynamic-source|untyped]
                "cljsbuild" "auto"))
          ;; ----- Test ----- ;;
          "test|backend"
            (with-profiles (cond-> [:backend :test :backend|test] (not quantum?) (conj :quantum|static-deps))
              "test")
          "test|frontend"
            (with-profiles (cond-> [:frontend :test :frontend|test] (not quantum?) (conj :quantum|static-deps))
              "doo" "phantom" "test" "once")
          "autotester|backend"
            (with-profiles (cond-> [:backend :test :backend|test] (not quantum?) (conj :quantum|static-deps))
              "test-refresh")
          ;; ----- Utils ----- ;;
          "count-loc"
            ["vanity"]})
     ;; ===== Profiles ===== ;;
     :profiles
       (remove-nil-vals
         {:dev
            {:jvm-opts       (into ["-Dquantum.core.system|profile=dev"] (>jvm-opts :dev))
             :resource-paths ["resources-dev"]
             :source-paths   ["src-dev"]
             :plugins        '[[lein-nodisassemble "0.1.3"]]}
          :test
            {:jvm-opts (>jvm-opts :test)}
          :prod
            {:jvm-opts (>jvm-opts :prod)}
          :backend
            {:source-paths ["src-backend"]
             :env          {:print-pid? true}}
          :backend|dev
            {}
          :backend|prod {}
          :backend|test
            {:plugins '[[com.jakemccrary/lein-test-refresh "0.16.0"]]}
          :frontend
            {:source-paths ["src-frontend"]
             :plugins '[[lein-cljsbuild "1.1.7"
                          :exclusions [org.clojure/clojure org.clojure/clojurescript]]]}
          :frontend|dev
            {:plugins '[[lein-figwheel "0.5.14"]]
             :cljsbuild
               {:builds (>cljsbuild-builds :dev project-config opts ["src" "src-frontend" "src-dev"] artifact-base-name)}
             :figwheel
               {:http-server-root "server-root" ; assumes "resources" is prepended
                :server-port      3450
                :css-dirs         ["resources/server-root/css"]}}
          :frontend|dev|re-frame-trace
            {:source-paths [(:re-frame-trace quantum-source-paths)]
             ;; It might work with React Native: https://github.com/Day8/re-frame-trace/issues/75
             :dependencies '[[day8.re-frame/trace       "0.1.18" #_"0.1.18-react16"]
                             [cljsjs/react              "16.2.0-0"]
                             [cljsjs/create-react-class "15.6.2-0"]]}
          :frontend|prod
            {:dependencies (when react-native? '[[react-native-externs "0.1.0"]]) ; technically only used by :advanced profile
             :cljsbuild    {:builds (>cljsbuild-builds :prod project-config opts ["src" "src-frontend"] artifact-base-name)}}
          :frontend|test
            {:plugins '[[lein-doo "0.1.7"]]
             :cljsbuild {:builds (>cljsbuild-builds :test project-config opts ["src" "src-frontend"] artifact-base-name)}}
          :quantum|static-deps
            (when-not quantum?
              {:dependencies [['quantum/core #_"LATEST" latest-stable-quantum-version]]})
          :quantum|static-deps-local
            (when-not quantum?
              {:dependencies [['quantum/core #_"LATEST" (>git-hash "../quantum/")]]})
          :quantum|dynamic-deps
            (when-not quantum?
              {:repositories (:repositories base-config|quantum)
               :dependencies
                 (into (->> (:dependencies base-config|quantum)
                            (remove #(->> % first str
                                            (re-find
                                              (re-pattern
                                                (str/join "|" ["^posh" #_"datomic-free"
                                                               "reagent" #_"re-frame"
                                                               "^cljsjs/(react|react-dom|react-dom-server|create-react-class)"])))))
                            vec)
                       '[[org.clojure/tools.analyzer.jvm "0.6.10"]
                         [reagent                        "0.7.0" #_"0.8.0-alpha2"
                           :exclusions [cljsjs/react cljsjs/react-dom cljsjs/react-dom-server cljsjs/create-react-class]]
                         [cljsjs/victory                 "0.13.7-2"
                           :exclusions [cljsjs/react]]
                       #_[com.datomic/datomic-pro        "0.9.5544"
                          :exclusions [org.slf4j/slf4j-nop
                                       org.slf4j/log4j-over-slf4j
                                       org.jboss.logging/jboss-logging]]])
               :aot (:aot base-config|quantum)})
          :quantum|dynamic-source
            (when-not quantum?
              {:source-paths (vals quantum-source-paths)})
          :quantum|dynamic-source|untyped
            (when-not quantum?
              {:source-paths [(:untyped quantum-source-paths) #_(:posh quantum-source-paths)]})
          ;; ----- Special profiles ----- ;;
          :auto-instrument
            {:jvm-opts    ["-Dco.paralleluniverse.pulsar.instrument.auto=all"]
             :java-agents '[[co.paralleluniverse/quasar-core "0.7.6" :options "m"]]}
          :fibers
            {:java-agents '[[co.paralleluniverse/quasar-core "0.7.6"]]}
          :http2
            {:java-agents '[[kr.motd.javaagent/jetty-alpn-agent "1.0.1.Final"]]}})}))

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
     `(let [opts#   ~opts
            config# (with-default-config opts# ~config)
            _#      (when (:print-config? opts#) (pprint config#))
            root#   ~(when f (.getParent f))]
        (def ~'project
          (project/make
            (dissoc config# :name :version)
            (:name    config#)
            (:version config#)
            root#))))))
