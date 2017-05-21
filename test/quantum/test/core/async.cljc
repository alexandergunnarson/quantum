(ns quantum.test.core.async
  (:require [quantum.core.async :as ns]))

(defn test:<?
  [expr])

(defn test:try-go
  [& body])

(defn test:chan
  ([         ])
  ([arg0     ])
  ([arg0 arg1]))

(defn test:take!! ([x]) ([x n]))

(defn test:empty! [x])

(defn test:put!! [x obj])

(defn test:message? [x])

(defn test:close-req? [x])

(defn test:peek!! ([x]) ([x n]))

(defn test:interrupt! [x])
(defn test:interrupted? [x])

(defn test:close! [x])
(defn test:closed? [x])

(defn test:realized? [x])

(defn test:sleep [x])

; MORE COMPLEX OPERATIONS

(defn test:alts!!-queue [chans timeout])

(defn test:alts!! ([type chans]) ([type chans timeout]))

(defn test:wait-until ([pred]) ([timeout pred]))

(defn test:concur
  [& fs])

(defn test:web-worker?
  [])

(defn test:->threadpool [{:keys [thread-ct script-src] :as opts}])

(defn test:async
  [& body])

(defn test:future
  [& body])
