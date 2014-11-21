(defproject quanta/library "0.1.0"
  :description      "Library for Quanta."
  :jvm-opts         []
  ;:uberjar          {:aot :all}
  :jar-name         "quanta-lib-dep.jar"
  :uberjar-name     "quanta-lib.jar"
  :url              "https://www.github.com/alexandergunnarson/quanta-lib"
  :license          {:name "Eclipse Public License"
                     :url "http://www.eclipse.org/legal/epl-v10.html"}
  :plugins          [] 
  :dev-dependencies []
  :dependencies
    [; CLOJURE CORE
     [org.clojure/clojure               "1.6.0"]
     ; META (CODE)      
     [repetition-hunter                 "1.0.0"]
     ; defprotocol+, definterface+, etc.
     [potemkin                          "0.3.11"]
     ; IO      
     [commons-io/commons-io             "2.4"  ] ; writing byte arrays to file and such
     [com.taoensso/nippy                "2.7.0-alpha1"] ; data serialization
     [iota                              "1.1.2"] ; fast/efficient string IO manipulation
     ; UTIL.TRACE      
     [org.clojure/tools.trace           "0.7.6"]
     ; UTIL.BENCH      
     [criterium                         "0.4.3"]
     ; TIME.*      
     [clj-time                          "0.7.0"] ; similar to JODA time
     ; PRINT      
     [fipp                              "0.4.3"]
     ; ERROR      
     [slingshot                         "0.10.3"]
     ; NETWORK.HTTP      
     [clj-http                          "1.0.0" ]
     [http-kit                          "2.1.18"] 
     [oauth-clj                         "0.1.12"]
     [net.sourceforge.htmlunit/htmlunit "2.15"] ; for some reason, retrieves 2.14....
     ; THREADING, ASYNC, CONCURRENCY
     [org.clojure/core.async            "0.1.346.0-17112a-alpha"]
     ; IO
     [iota                              "1.1.2"] ; fast/efficient string IO manipulation
     ; NUMERIC                   
     [primitive-math                    "0.1.3"]
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
  )