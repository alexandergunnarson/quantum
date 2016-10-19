(ns quantum.reducers.spark
  (:refer-clojure :exclude [map filter group-by take reduce])
  (:require
    [clojure.string           :as str]
#?@(:clj
   [[sparkling.conf           :as conf]
    [sparkling.core           :as spark]])
    [quantum.core.system      :as sys]
    [quantum.core.collections.base
      :refer        [#?@(:clj [kmap])]
      :refer-macros [          kmap]]
    [quantum.core.macros
      #?@(:clj [:refer [compile-if]])]
    [quantum.core.error :as err
      :refer [->ex TODO]])
  #?(:clj (:import (org.apache.spark          SparkContext)
                   (org.apache.spark.api.java JavaRDDLike)
                   (org.apache.spark.sql      Dataset Encoder Encoders)
                   (org.apache.spark.api.java.function
                     ReduceFunction MapFunction FilterFunction
                     FlatMapFunction))))

(sys/merge-env! {"SPARK_LOCAL_IP" "127.0.0.1"})

#?(:clj
(def version
  (let [version (-> (conf/spark-conf)
                    (conf/master "local")
                    (conf/app-name "quantum")
                    (spark/spark-context)
                    (doto ^SparkContext (.stop))
                    (.version))
        [major minor incremental] (str/split version #"\.")]
    (kmap major minor incremental))))

#?(:clj
(compile-if (= (:major version) "2")
  (do (import 'org.apache.spark.sql.SparkSession)
      (defmacro with-session
        [session-sym builder & body]
        `(let [^SparkSession ~session-sym ~builder]
           (try
             ~@body
             (finally (.stop ~session-sym)))))

      #_(-> (SparkSession/builder)
        (.master (if master master "local"))
        (.appName "alexandergunnarson:ngrams-test")
        (.getOrCreate))
        #_(.textFile (.read sc))

      (defn map
        ([f x] (map (Encoders/STRING) f x))
        ([^Encoder encoder f ^Dataset x]
          (.map x (reify MapFunction (call [_ x] (f x))) encoder)))

      (defn filter
        ([f x] (filter (Encoders/STRING) f x))
        ([^Encoder encoder f ^Dataset x]
          (.filter x (reify FilterFunction (call [_ x] (f x))) encoder)))

      (defn group-by [f ^Dataset x] (TODO) #_(.groupBy))
      (defn take     [n ^Dataset x] (.take x n))

      (defn flat-map
        ([f x] (flat-map (Encoders/STRING) f x))
        ([^Encoder encoder f ^Dataset x]
          (.flatMap x (reify FlatMapFunction (call [_ x] (f x))) encoder)))

      #_(defn fold [f ^Dataset x]
        (.fold x
          (reify ReduceFunction (call [_ v1 v2] (f v1 v2)))))
      (defn fold [f x] (TODO))
      (defn collect [^Dataset x] (.collect x)))
  (do (defn- requires-spark>=2 [& _]
        (throw (->ex nil "Requires Spark >= 2.0" (kmap version))))

      (doseq [sym '#{map filter group-by take flat-map fold collect}]
        (intern *ns* sym requires-spark>=2)))))
