(ns quantum.test.core.system
  (:require [quantum.core.system :as ns]))

#?(:clj (defn test:this-pid []))

#?(:clj
(defn test:env-var
  [env-var-to-lookup]))

#?(:clj
(defn- test:java-version []))

#?(:clj
(defn test:class-loader []))

#?(:clj
(defn test:mem-stats
  [& {:keys [gc?]}]))
