(ns
  ^{:doc "System-level (environment) vars such as |os|."
    :attribution "Alex Gunnarson"}
  quantum.core.system
           (:require [quantum.core.collections :as coll
                       :refer [containsv?]                ]
                     [quantum.core.logic       :as logic
                       :refer [#?@(:clj [condpc coll-or])]]
                     [quantum.core.string      :as str    ])
  #?(:cljs (:require-macros
                     [quantum.core.logic       :as logic
                       :refer [condpc coll-or]            ]))
  #?(:clj (:import java.io.File
                   java.lang.management.ManagementFactory)))

; TODO possibly move JS feature detection here?

#?(:clj
(def ^{:from "com.google.common.base.StandardSystemProperty"}
  std-properties
  #{
  ;Java Runtime Environment version.
  "java.version"
  ;Java Runtime Environment vendor.
  "java.vendor"
  ;Java vendor URL.
  "java.vendor.url"
  ;Java installation directory.
  "java.home"
  ;Java Virtual Machine specification version.
  "java.vm.specification.version"
  ;Java Virtual Machine specification vendor.
  "java.vm.specification.vendor"
  ;Java Virtual Machine specification name.
  "java.vm.specification.name"
  ;Java Virtual Machine implementation version.
  "java.vm.version"
  ;Java Virtual Machine implementation vendor.
  "java.vm.vendor"
  ;Java Virtual Machine implementation name.
  "java.vm.name"
  ;Java Runtime Environment specification version.
  "java.specification.version"
  ;Java Runtime Environment specification vendor.
  "java.specification.vendor"
  ;Java Runtime Environment specification name.
  "java.specification.name"
  ;Java class format version number.
  "java.class.version"
  ;Java class path.
  "java.class.path"
  ;List of paths to search when loading libraries.
  "java.library.path"
  ;Default temp file path.
  "java.io.tmpdir"
  ;Name of JIT compiler to use.
  "java.compiler"
  ;Path of extension directory or directories.
  "java.ext.dirs"
  ;Operating system name.
  "os.name"
  ;Operating system architecture.
  "os.arch"
  ;Operating system version.
  "os.version"
  ;File separator ("/" on UNIX).
  "file.separator"
  ;Path separator (":" on UNIX).
  "path.separator"
  ;Line separator ("\n" on UNIX).
  "line.separator"
  ;User's account name.
  "user.name"
  ;User's home directory.
  "user.home"
  ;User's current working directory.
  "user.dir"}))

(def os ; TODO: make less naive
  #?(:cljs (condp containsv? (.-appVersion js/navigator)
             "Win"   :windows
             "MacOS" :mac
             "X11"   :unix
             "Linux" :linux
             :unknown)
     :clj
      (let [os-0 (-> (System/getProperty "os.name")
                     str/->lower)]
        (condpc containsv? os-0
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

#?(:clj (defn this-pid [] (->> (ManagementFactory/getRuntimeMXBean) (.getName))))

#?(:clj
(defn env-var
  "Gets an environment variable."
  {:usage '(env-var "HOME")}
  [env-var-to-lookup]
  (-> (System/getenv) (get env-var-to-lookup))))

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
  []
  (ClassLoader/getSystemClassLoader)))

#?(:clj
(defn mem-stats
  "Return stats about memory availability and usage, in MB. Calls
  System/gc before gathering stats when the :gc option is true."
  {:attribution "github.com/jkk/sundry/jvm"
   :todo ["Use |convert| package to convert gb to mb!"]}
  [& {:keys [gc?]}]
  (when gc?
    (System/gc))
  (let [r (Runtime/getRuntime)
        mb #(int (/ % 1024 1024))]
    {:max   (mb (.maxMemory r))
     :total (mb (.totalMemory r))
     :used  (mb (- (.totalMemory r) (.freeMemory r)))
     :free  (mb (.freeMemory r))})))
