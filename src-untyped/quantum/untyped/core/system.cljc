(ns quantum.untyped.core.system
  (:require
    [clojure.string                     :as str]
    [quantum.untyped.core.collections   :as ucoll]
    [quantum.untyped.core.core          :as ucore]
    [quantum.untyped.core.error         :as err]
    [quantum.untyped.core.logic
      :refer [condpc coll-or]]
    [quantum.untyped.core.string.format :as ustr|form])
  #?(:clj (:import (java.io   File)
                   (java.util Map Collections)
                   (java.lang.reflect Field)
                   (java.lang.management ManagementFactory))))

(ucore/log-this-ns)

#?(:cljs
(def global
  ^{:adapted-from "https://www.contentful.com/blog/2017/01/17/the-global-object-in-javascript/"}
  ((js/Function "return this;"))))

;; ----- Modules/dependencies ----- ;;

#?(:cljs (def dependencies (.-dependencies global)))
#?(:cljs (def js-require   (.-require      global)))

#?(:cljs
(defn >module
  "Finds a module by the following fallbacks:
   1) global var name
   2) package names in order, by lookup in global var `dependencies`
   3) package names in order, via `js/require`"
  [var-name package-names]
  (or (aget global var-name)
      (if dependencies
          (some #(aget dependencies %) package-names)
          (when js-require
            (err/ignore (some js-require package-names)))))))

;; ----- Browser / OS ----- ;;

#?(:cljs
(def
  ^{:from "http://stackoverflow.com/questions/9847580/how-to-detect-safari-chrome-ie-firefox-and-opera-browser"
    :contributors {"Alex Gunnarson" "Ported to CLJC"}}
  browser
  (delay
    (when (.-window global)
      (cond
        ; Opera 8.0+ (UA detection to detect Blink/v8-powered Opera)
        (or (.-opera global)
            (some-> global .-navigator .-userAgent (.indexOf " OPR/") (>= 0)))
        :opera
        ;  Chrome 1+
        (.-chrome global)
        :chrome
        ; Firefox 1.0+
        (aget global "InstallTrigger")
        :firefox
        ; At least Safari 3+: "[object HTMLElementConstructor]"
        (-> js/Object .-prototype .-toString
            (.call (.-HTMLElement global))
            (.indexOf "Constructor")
            pos?)
        :safari
        ; At least IE6
        (-> global .-document .-documentMode)
        :ie
        :else :unknown)))))

#?(:cljs
(def react-native
  (>module "ReactNative" ["react-native" "react-native-web"]))) ; https://github.com/necolas/react-native-web

#?(:clj
(def info
  (let [mx    (ManagementFactory/getRuntimeMXBean)
        props (.getSystemProperties mx)
        os    (ManagementFactory/getOperatingSystemMXBean)]
    {:run         {:command  (get props "sun.java.command")
                   :args     (vec (.getInputArguments mx))}
     :user        {:name     (get props "user.name") ; User's account name
                   :home     (get props "user.home") ; User's home directory.
                   :language (get props "user.language")
                   :timezone (get props "user.timezone")
                   :country  (get props "user.country" )}
     :os          {:arch                 (.getArch                os) ; same as "os.arch"
                   :name                 (.getName                os) ; same as "os.name"
                   :version              (.getVersion             os) ; same as "os.version"
                   :patch-level          (get props "sun.os.patch.level")}
     :paths       {:classpath           (get props "java.class.path"      )
                   :boot-classpath      (get props "sun.boot.class.path"  )
                   :boot-lib-path       (get props "sun.boot.library.path")
                   :runtime-install-dir (get props "java.home") ; Java installation directory.
                   :lib-path            (get props "java.library.path"    );List of paths to search when loading libraries.
                   :extensions-path     (get props "java.ext.dirs"        )
                   :endorsed-dirs       (get props "java.endorsed.dirs"   )
                   :temp-dir            (get props "java.io.tmpdir"       ) ; Default temp file path
                   :dir                 (get props "user.dir"             )}
     :io          {:path-separator   (get props "path.separator") ; ":" on UNIX
                   :file-separator   (get props "file.separator") ; "/" on UNIX
                   :line-separator   (get props "line.separator")  ; "\n" on UNIX
                   :file-encoding    (get props "file.encoding" )
                   :jnu-encoding     (get props "sun.jnu.encoding")
                   :unicode-encoding (get props "sun.io.unicode.encoding")
                   } ; Current working directory.
     :reporting   {:bug {:level (get props "sun.nio.ch.bugLevel")
                         :url   (get props "java.vendor.url.bug")}}
     :net         {:http-non-proxy-hosts (when-let [h (get props "http.nonProxyHosts")] (set (str/split h #"\|" )))
                   :ftp-non-proxy-hosts  (when-let [h (get props "ftp.nonProxyHosts" )] (set (str/split h #"\|" )))}
     :machine     {:name              (.getName mx)
                   :endianness        (get props "sun.cpu.endian")
                   :list?             (get props "sun.cpu.isalist")
                   :arch-data-model   (get props "sun.arch.data.model")
                   :total-ram-size:gb (-> (.getTotalPhysicalMemorySize os) (/ 1024 1024 1024) double) ; TODO this is particular to certain VMs? ; http://stackoverflow.com/questions/5512378/how-to-get-ram-size-and-size-of-hard-disk-using-java
                   :cpu-cores         (.getAvailableProcessors os)} ; TODO this is more of a guess
     :runtime     {:name            (get props "java.runtime.name"   )
                  :runtime-version (get props "java.runtime.version") ; A more specific version like -b14
                  :version         (get props "java.version"        )
                  :vendor-url      (get props "java.vendor.url"     )
                  :vendor          (get props "java.vendor"         )
                  :class-version   (get props "java.class.version"  )  ; Java class format version number.
                  :launcher        (get props "sun.java.launcher"   )
                  :spec            {:name    (get props "java.specification.name"   )
                                    :vendor  (get props "java.specification.vendor" )
                                    :version (get props "java.specification.version")}}
     :vm          {:name    (get props "java.vm.name"   )
                   :type    (.getName (ManagementFactory/getCompilationMXBean)) ; "sun.management.compiler" | "java.compiler"
                   :vendor  (get props "java.vm.vendor" )
                   :version (get props "java.vm.version")
                   :info    (get props "java.vm.info"   )
                   :spec    {:name    (get props "java.vm.specification.name"   )
                             :vendor  (get props "java.vm.specification.vendor" )
                             :version (get props "java.vm.specification.version")}}
     :graphics    {:awt {:toolkit      (get props "awt.toolkit"         )
                         :graphics-env (get props "java.awt.graphicsenv")}
                   :enable-extra-mouse-buttons? (get props "sun.awt.enableExtraMouseButtons")
                   :font-manager                (get props "sun.font.fontmanager"           )}
     :peripherals {:printer-jb           (get props "java.awt.printerjob")}
     :internal    {:file-encoding-pkg    (get props "file.encoding.pkg")}
     :clojure     {:debug?               (get props "clojure.debug")
                   :core-async-pool-size (get props "clojure.core.async.pool-size")
                   :compile-path         (get props "clojure.compile.path")
                   :version {:map    (clojure-version)
                             :string *clojure-version*}}})))


(def os ; TODO: make less naive
  #?(:cljs (if react-native
               (-> react-native .-Platform .-OS)
               (condp #(ucoll/containsv? %1 %2) (.-appVersion js/navigator)
                 "Win"   :windows
                 "MacOS" :mac
                 "X11"   :unix
                 "Linux" :linux
                 :unknown))
     :clj
      (let [os-0 (some-> info :os :name ustr|form/>lower)]
        (condpc #(ucoll/containsv? %1 %2) os-0
          "win"                       :windows
          "mac"                       :mac
          (coll-or "nix" "nux" "aix") :unix
          "sunos"                     :solaris))))

;; ----- OS-specific ----- ;;

(def separator
  #?(:cljs (condp = os :windows "\\" "/") ; TODO make less naive
     :clj  (str (java.io.File/separatorChar)))) ; string because it's useful in certain functions that way

;; ----- React-specific ----- ;;

#?(:cljs
(set! (-> js/console .-ignoredYellowBox)
  #js ["You are manually calling a React.PropTypes validation function for the"]))

;; ----- React-Native-specific ----- ;;

#?(:cljs (def app-registry (some-> react-native .-AppRegistry )))
#?(:cljs (def AsyncStorage (some-> react-native .-AsyncStorage)))
#?(:cljs (def StatusBar    (some-> react-native .-StatusBar   )))

;; ----- Features ----- ;;

#?(:cljs
(def ^{:doc "Determines whether the device is 'touchable'"
       :adapted-from 'pukhalski/tap} touchable?
  (delay (or (and (.-propertyIsEnumerable global)
                  (.propertyIsEnumerable  global   "ontouchstart"))
             (and (-> global .-document .-hasOwnProperty)
                  (or (.hasOwnProperty (.-document global) "ontouchstart")
                      (.hasOwnProperty global              "ontouchstart")))))))
