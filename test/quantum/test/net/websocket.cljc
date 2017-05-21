(ns quantum.test.net.websocket
  (:require [quantum.net.websocket :as ns]))

(defn test:put!
  [#?(:clj uid) [msg-id msg] callback & [timeout]])

(defn test:try-put!
  [?times ?sleep & args])

(defn test:->ChannelSocket [m])