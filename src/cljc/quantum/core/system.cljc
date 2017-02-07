(ns
  ^{:doc "System-level (environment) vars such as |os|."
    :attribution "Alex Gunnarson"}
  quantum.core.system
           (:require
     #?(:clj [environ.core
               :refer [env]])
     #?(:clj [criterium.core           :as bench])
             [quantum.core.core        :as qcore]
             [quantum.core.collections :as coll
               :refer [containsv?]]
             [quantum.core.logic       :as logic
               :refer [condpc coll-or]]
             [quantum.core.log         :as log]
             [quantum.core.string      :as str]
             [quantum.core.error       :as err]
             [quantum.core.reflect     :as refl]
             [quantum.core.vars
               :refer [defalias]])
  #?(:clj (:import (java.io   File)
                   (java.util Map Collections)
                   (java.lang.reflect Field)
                   (java.lang.management ManagementFactory))))

(log/this-ns)

; TODO possibly move JS feature detection here?
; ================================

#?(:clj
(def info
  (let [mx    (ManagementFactory/getRuntimeMXBean)
        props (.getSystemProperties mx)
        os    (ManagementFactory/getOperatingSystemMXBean)]
    {:run                    {:command  (get props "sun.java.command")
                              :args     (vec (.getInputArguments mx))}
     :user                   {:name     (get props "user.name") ; User's account name
                              :home     (get props "user.home") ; User's home directory.
                              :language (get props "user.language")
                              :timezone (get props "user.timezone")
                              :country  (get props "user.country" )}
     :os                     {:arch                 (.getArch                os) ; same as "os.arch"
                              :available-processors (.getAvailableProcessors os)
                              :name                 (.getName                os) ; same as "os.name"
                              :version              (.getVersion             os) ; same as "os.version"
                              :patch-level          (get props "sun.os.patch.level")}
     :paths                  {:classpath           (get props "java.class.path"      )
                              :boot-classpath      (get props "sun.boot.class.path"  )
                              :boot-lib-path       (get props "sun.boot.library.path")
                              :runtime-install-dir (get props "java.home") ; Java installation directory.
                              :lib-path            (get props "java.library.path"    );List of paths to search when loading libraries.
                              :extensions-path     (get props "java.ext.dirs"        )
                              :endorsed-dirs       (get props "java.endorsed.dirs"   )
                              :temp-dir            (get props "java.io.tmpdir"       ) ; Default temp file path
                              :dir                 (get props "user.dir"             )}
     :io                     {:path-separator   (get props "path.separator") ; ":" on UNIX
                              :file-separator   (get props "file.separator") ; "/" on UNIX
                              :line-separator   (get props "line.separator")  ; "\n" on UNIX
                              :file-encoding    (get props "file.encoding" )
                              :jnu-encoding     (get props "sun.jnu.encoding")
                              :unicode-encoding (get props "sun.io.unicode.encoding")
                              } ; Current working directory.
     :reporting              {:bug {:level (get props "sun.nio.ch.bugLevel")
                                    :url   (get props "java.vendor.url.bug")}}
     :net                    {:http-non-proxy-hosts (when-let [h (get props "http.nonProxyHosts")] (set (str/split h #"\|" )))
                              :ftp-non-proxy-hosts  (when-let [h (get props "ftp.nonProxyHosts" )] (set (str/split h #"\|" )))}
     :machine                {:name             (.getName mx)
                              :endianness       (get props "sun.cpu.endian")
                              :list?            (get props "sun.cpu.isalist")
                              :arch-data-model  (get props "sun.arch.data.model")}
     :runtime               {:name            (get props "java.runtime.name"   )
                             :runtime-version (get props "java.runtime.version") ; A more specific version like -b14
                             :version         (get props "java.version"        )
                             :vendor-url      (get props "java.vendor.url"     )
                             :vendor          (get props "java.vendor"         )
                             :class-version   (get props "java.class.version"  )  ; Java class format version number.
                             :launcher        (get props "sun.java.launcher"   )
                             :spec            {:name    (get props "java.specification.name"   )
                                               :vendor  (get props "java.specification.vendor" )
                                               :version (get props "java.specification.version")}}
     :vm                     {:name    (get props "java.vm.name"   )
                              :type    (.getName (ManagementFactory/getCompilationMXBean)) ; "sun.management.compiler" | "java.compiler"
                              :vendor  (get props "java.vm.vendor" )
                              :version (get props "java.vm.version")
                              :info    (get props "java.vm.info"   )
                              :spec    {:name    (get props "java.vm.specification.name"   )
                                        :vendor  (get props "java.vm.specification.vendor" )
                                        :version (get props "java.vm.specification.version")}}
     :graphics               {:awt {:toolkit      (get props "awt.toolkit"         )
                                    :graphics-env (get props "java.awt.graphicsenv")}
                              :enable-extra-mouse-buttons? (get props "sun.awt.enableExtraMouseButtons")
                              :font-manager                (get props "sun.font.fontmanager"           )}
     :peripherals            {:printer-jb           (get props "java.awt.printerjob")}
     :internal               {:file-encoding-pkg    (get props "file.encoding.pkg")}
     :clojure                {:debug?               (get props "clojure.debug")
                              :core-async-pool-size (get props "clojure.core.async.pool-size")
                              :compile-path         (get props "clojure.compile.path")
                              :version {:map    (clojure-version)
                                        :string *clojure-version*}}})))

#?(:cljs
(def ReactNative
  (err/ignore
    (if (undefined? js/window.ReactNative)
        (js/require "react-native")
        js/window.ReactNative)))) ; https://github.com/necolas/react-native-web

(def os ; TODO: make less naive
  #?(:cljs (if ReactNative
               (-> ReactNative .-Platform .-OS)
               (condp #(containsv? %1 %2) (.-appVersion js/navigator)
                   "Win"   :windows
                   "MacOS" :mac
                   "X11"   :unix
                   "Linux" :linux
                   :unknown))
     :clj
      (let [os-0 (-> info :os :name str/->lower)]
        (condpc #(containsv? %1 %2) os-0
          "win"                       :windows
          "mac"                       :mac
          (coll-or "nix" "nux" "aix") :unix
          "sunos"                     :solaris))))

(def separator
  #?(:cljs (condp = os :windows "\\" "/") ; TODO make less naive
     :clj  (str (File/separatorChar)))) ; string because it's useful in certain functions that way

(def os-sep-esc
  (condp = os
    :windows "\\\\"
    "/"))

#?(:clj (defalias pid qcore/pid))

#?(:clj
(defn env-var
  "Gets an environment variable."
  {:usage '(env-var "HOME")}
  [v] (-> (System/getenv) (get v))))

#?(:clj
(defn merge-env!
  {:adapted-from "http://stackoverflow.com/questions/318239/how-do-i-set-environment-variables-from-java"}
  [^Map newenv]
  (try
    (let [newenv (->> newenv
                      (map (fn [[^String k ^String v]]
                             [(refl/invoke-private
                                (Class/forName "java.lang.ProcessEnvironment$Variable")
                                "valueOf"
                                [String] [k])
                              (refl/invoke-private
                                (Class/forName "java.lang.ProcessEnvironment$Value")
                                "valueOf"
                                [String] [v])]))
                      (into {}))
          ^Class processEnvironmentClass (Class/forName "java.lang.ProcessEnvironment")
          ^Field theEnvironmentField
            (doto (.getDeclaredField processEnvironmentClass "theEnvironment")
                  (.setAccessible true))
          _ (doto ^Map (.get theEnvironmentField nil)
                       (.putAll newenv))
          ^Field theCaseInsensitiveEnvironmentField
            (doto (.getDeclaredField processEnvironmentClass "theCaseInsensitiveEnvironment")
                  (.setAccessible true))
          _ (doto ^Map (.get theCaseInsensitiveEnvironmentField nil)
                       (.putAll newenv))])
    (catch NoSuchFieldException e
      (let [^"[Ljava.lang.Class;" classes
             (.getDeclaredClasses Collections)
            env (System/getenv)]
        (doseq [^Class cl classes]
          (when (= "java.util.Collections$UnmodifiableMap" (.getName cl))
            (let [^Field field (doto (.getDeclaredField cl "m")
                                     (.setAccessible true))
                  ^Map  m (.get field env)
                  _ (doto ^Map m
                               (.putAll newenv))]))))))
  (System/getenv)))

#?(:clj
(defn- java-version
  {:from "clojure.tools.nrepl.middleware"}
  []
  (let [version-string (System/getProperty "java.version")
        version-seq (re-seq #"\d+" version-string)
        version-map (if (<= 3 (count version-seq))
                      (zipmap [:major :minor :incremental :update] version-seq)
                      {})]
    (assoc version-map :version-string version-string))))

#?(:clj
(defn class-loader
  "Gets the system class loader"
  [] (ClassLoader/getSystemClassLoader)))

#?(:clj
(defn mem-stats
  "Return stats about memory availability and usage, in MB. Calls
   System/gc before gathering stats when the :gc option is true."
  {:todo {0 "Use |convert| package to convert gb to mb"}}
  [& {:keys [gc?]}]
  (when gc? (System/gc))
  ; Warning: inconsistent snapshots here
  (let [mem (java.lang.management.ManagementFactory/getMemoryMXBean)
        mb #(double (/ % 1024 1024))]
    {:heap     (let [used    (-> mem .getHeapMemoryUsage .getUsed mb)
                     max-mem (-> mem .getHeapMemoryUsage .getMax  mb)]
                 {:committed (-> mem .getHeapMemoryUsage .getCommitted mb)
                  :init      (-> mem .getHeapMemoryUsage .getInit      mb)
                  :used      used
                  :max       max-mem
                  :free      (- max-mem used)})
     :off-heap (let [used    (-> mem .getNonHeapMemoryUsage .getUsed mb)
                     max-mem (-> mem .getNonHeapMemoryUsage .getMax  mb)]
                 {:committed (-> mem .getNonHeapMemoryUsage .getCommitted mb)
                  :init      (-> mem .getNonHeapMemoryUsage .getInit      mb)
                  :used      used
                  :max       (when (pos? max-mem) max-mem)
                  :free      (when (pos? max-mem) (- max-mem used))})})))

#?(:clj (defalias force-gc! bench/force-gc))

#?(:clj (defalias clear-cache-mac! bench/clear-cache-mac))

#?(:clj
(defn clear-cache-linux!
  {:source 'criterium.core
   :todo   ["Figure out sudo issue"]}
  []
  ; not sure how to deal with the sudo
  (.. Runtime getRuntime
      (exec "sudo sh -c 'echo 3 > /proc/sys/vm/drop_caches'") waitFor)))

#?(:clj
(defn clear-cache!
  {:source 'criterium.core
   :todo   ["Generalize to all OSes"]}
  []
  (condp #(re-find %1 %2) (.. System getProperties (getProperty "os.name"))
    #"Mac" (clear-cache-mac!)
    :else (log/pr :warn "don't know how to clear disk buffer cache for "
                (.. System getProperties (getProperty "os.name"))))))


#?(:cljs
(set! (-> js/console .-ignoredYellowBox)
  #js ["You are manually calling a React.PropTypes validation function for the"]))

#?(:cljs (def app-registry (when ReactNative (.-AppRegistry  ReactNative))))

#?(:cljs (def AsyncStorage (when ReactNative (.-AsyncStorage ReactNative))))
#?(:cljs (def StatusBar    (when ReactNative (.-StatusBar    ReactNative))))
