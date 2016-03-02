(ns
  ^{:doc "A barely-used namespace with one function: to launch a URL in a browser. 

          Not particularly useful."
    :attribution "Alex Gunnarson"}
  quantum.network.core
  (:require-quantum [:lib]))

; clojure.java.browse/browse-url - how is that different?
(defn launch-url
  "Launch the provided URL in a system browser."
  {:attribution "thebusby.bagotricks"}
  [^String url] ; requires a protocol in front... make sure it can add if it doesn't have it (http, etc.)
  (if (and (java.awt.Desktop/isDesktopSupported)
           (-> (java.awt.Desktop/getDesktop)
               (.isSupported java.awt.Desktop$Action/BROWSE)))
    (-> (java.awt.Desktop/getDesktop)
        (.browse (java.net.URI. (.toString url))))
    (throw (Exception. (str "Sorry java.awt.Desktop, or a browser, isn't setup via Mac/Win/libgnome so you're on your own. Type the following into a browser, " url)))))
