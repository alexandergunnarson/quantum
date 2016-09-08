(ns
  ^{:doc "System-level (environment) vars such as |os|."
    :attribution "Alex Gunnarson"}
  quantum.core.system
           (:require
     #?(:clj [environ.core
               :refer [env]])
             [quantum.core.core        :as qcore]
             [quantum.core.collections :as coll
               :refer        [#?(:clj containsv?)]
               :refer-macros [containsv?]]
             [quantum.core.logic       :as logic
               :refer        [#?@(:clj [condpc coll-or])]
               :refer-macros [condpc coll-or]]
             [quantum.core.log         :as log
               :include-macros true]
             [quantum.core.string      :as str]
             [quantum.core.error       :as err
               :include-macros true]
             [quantum.core.reflect     :as refl]
             [quantum.core.vars
               :refer        [#?(:clj defalias)]
               :refer-macros [defalias]])
  #?(:clj (:import (java.io   File)
                   (java.util Map Collections)
                   (java.lang.reflect Field))))

(log/this-ns)

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
      (let [os-0 (-> (System/getProperty "os.name")
                     str/->lower)]
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
  [v]
  (-> (System/getenv) (get v))))

#?(:clj
(defn merge-env!
  {:from "http://stackoverflow.com/questions/318239/how-do-i-set-environment-variables-from-java"}
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

#?(:cljs
(set! (-> js/console .-ignoredYellowBox)
  #js ["re-frame: overwriting an event-handler for:"
       "re-frame: overwriting  :event  handler for:"
       "has been renamed"
       "You are manually calling a React.PropTypes validation function for the"]))

#?(:cljs (def app-registry (when ReactNative (.-AppRegistry  ReactNative))))

#?(:cljs (def AsyncStorage (when ReactNative (.-AsyncStorage ReactNative))))
#?(:cljs (def StatusBar    (when ReactNative (.-StatusBar    ReactNative))))