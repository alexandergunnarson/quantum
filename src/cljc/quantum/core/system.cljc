(ns
  ^{:doc "System-level (environment) vars such as |os|."
    :attribution "Alex Gunnarson"}
  quantum.core.system
  (:require-quantum [str coll])
  #?(:clj (:import java.io.File java.lang.management.ManagementFactory)))

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
  "user.dir"})


#?(:clj
(def os ; TODO: make less naive
  (if (containsv? (System/getProperty "os.name") "Windows")
      :windows
      :unix)))

#?(:clj (defn this-pid [] (->> (ManagementFactory/getRuntimeMXBean) (.getName))))

#?(:clj (def separator (str (File/separatorChar)))) ; string because it's useful in certain functions that way
#?(:clj
(def os-sep-esc
  (case os
    :windows "\\\\"
    "/")))

#?(:clj
(defn env-var
  "Gets an environment variable."
  {:usage '(env-var "HOME")}
  [env-var-to-lookup]
  (-> (System/getenv) (get env-var-to-lookup))))

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
