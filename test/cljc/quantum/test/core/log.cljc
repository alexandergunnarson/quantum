(ns quantum.test.core.log
  (:require [quantum.core.log :as ns]))
 
(defn test:disable!
  ([pr-type])
  ([pr-type & pr-types]))

(defn test:enable!
  ([pr-type])
  ([pr-type & pr-types]))

(defn test:->log-initializer [{:keys [levels] :as opts}])

(defn test:pr*
  [trace? pretty? print-fn pr-type args opts])

(defn test:pr [pr-type & args])

(defn test:pr-no-trace [pr-type & args])

(defn test:pr-opts [pr-type opts & args])

(defn test:ppr [pr-type & args])

(defn test:ppr-hints [pr-type & args])