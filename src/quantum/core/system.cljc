(ns
  ^{:doc "System-level (environment) vars such as |os|."
    :attribution "alexandergunnarson"}
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
               :refer [defalias defaliases]]
             [quantum.untyped.core.system :as u])
  #?(:clj (:import (java.io   File)
                   (java.util Map Collections)
                   (java.lang.reflect Field)
                   (java.lang.management ManagementFactory))))

(log/this-ns)

#?(:cljs (defaliases u global dependencies js-require >module browser))

;; TODO possibly move JS feature detection here?
;; ================================

#?(:cljs (defalias u/react-native))

(defaliases u info os separator)

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
                             [(refl/invoke
                                (Class/forName "java.lang.ProcessEnvironment$Variable")
                                "valueOf"
                                [String] [k])
                              (refl/invoke
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
  {:todo {0 "Use `convert` package to convert gb to mb"}}
  [& {:keys [gc?]}]
  (when gc? (System/gc))
  ; Warning: inconsistent snapshots here
  (let [mem (java.lang.management.ManagementFactory/getMemoryMXBean)
        os  (java.lang.management.ManagementFactory/getOperatingSystemMXBean)
        mb #(double (/ % 1024 1024))]
    {:system   {:total (-> os (.getTotalPhysicalMemorySize) mb)
                :used  (mb (- (.getTotalPhysicalMemorySize os)
                              (.getFreePhysicalMemorySize  os)))
                :free  (-> os .getFreePhysicalMemorySize mb)}
     :heap     (let [used    (-> mem .getHeapMemoryUsage .getUsed mb)
                     max-mem (-> mem .getHeapMemoryUsage .getMax  mb)]
                 {:committed (-> mem .getHeapMemoryUsage .getCommitted mb)
                  :init      (-> mem .getHeapMemoryUsage .getInit      mb)
                  :used      used
                  :max       max-mem
                  :free      (- max-mem used)})
     :non-heap (let [used    (-> mem .getNonHeapMemoryUsage .getUsed mb)
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

#?(:clj
(defn thread-stats
  "Return stats about running and completed threads."
  []
  ; Warning: inconsistent snapshots here
  (let [mgr (java.lang.management.ManagementFactory/getThreadMXBean)]
    {:ct         (.getThreadCount mgr)
     :daemon-ct  (.getDaemonThreadCount mgr)
     :started-ct (.getTotalStartedThreadCount mgr)})))

#?(:clj
(defn cpu-stats
  "Return stats about CPU usage."
  []
  ; Warning: inconsistent snapshots here
  (let [mgr (java.lang.management.ManagementFactory/getOperatingSystemMXBean)]
    {:this   (.getProcessCpuLoad mgr)
     :system (.getSystemCpuLoad  mgr)})))

;; ----- React Native specific ----- ;;

#?(:cljs (defaliases u app-registry AsyncStorage StatusBar))

;; ----- Features ----- ;;

#?(:cljs (defaliases u touchable?))
