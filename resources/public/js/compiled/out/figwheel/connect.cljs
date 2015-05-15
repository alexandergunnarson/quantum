(ns figwheel.connect (:require [figwheel.client] [quantum.cljs_test]))
(figwheel.client/start {:build-id "dev", :websocket-url "ws://localhost:3448/figwheel-ws"})

