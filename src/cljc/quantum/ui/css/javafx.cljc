(ns quantum.ui.css.javafx
  (:require-quantum [:lib ui]))

(in-ns 'fx-clj.css)
(defn remove-global-stylesheet! [url]
  (let [^java.util.ArrayList stylesheets
          (-> (StyleManager/getInstance)
              (quantum.core.java/field "platformUserAgentStylesheetContainers")) ; was |userAgentStylesheets| before a certain revision
        stylesheet-index
          (-> (StyleManager/getInstance)
              (quantum.core.java/invoke "getIndex" (str url)))]
    (.remove stylesheets stylesheet-index) ; This apparently doesn't work
    (.clear stylesheets)))
(in-ns 'quantum.ui.css.javafx)


(defn set-css! [file]
  #_(let [file-str (io/file-str file)]
    (fx/run! (javafx.application.Application/setUserAgentStylesheet nil)
     #_(-> (StyleManager/getInstance)
         (java/field "platformUserAgentStylesheetContainers")  ; was |userAgentStylesheets| before a certain revision
         (.clear))
     (.setDefaultUserAgentStylesheet
       (StyleManager/getInstance) file-str)
     (.addUserAgentStylesheet
       (StyleManager/getInstance)  file-str)
     (javafx.application.Application/setUserAgentStylesheet file-str)))
  
  #_ (fx/run!
    (Application/setUserAgentStylesheet (io/file-str [:resources "test.css"]))
    (.addUserAgentStylesheet (StyleManager/getInstance) (io/file-str [:resources "test.css"])))
  ; http://www.guigarage.com/2013/03/global-stylesheet-for-your-javafx-application/
  (fx/run<!!
    (fx.css/set-global-css!
      (io/read :read-method :str :path file))))
(import 'javafx.application.Application)

(def css-file-modified-handler
  (atom (fn [file]
          (set-css! (io/file-str file))
          (log/pr ::debug "CSS set."))))

(defonce css-file-watched (atom [:resources "test.css"]))

(defonce css-file-watcher
  (fs/file-watcher
    {:file     css-file-watched
     :handlers {:modified (fn [e] (@css-file-modified-handler e))}}
    {:id :css-file-watcher}))


(declare extract-css-metadata)

(defn- extract-css-metadatum
  [^javafx.css.CssMetaData meta-0 ^Node node]
  (let [^javafx.css.StyleableProperty prop
         (.getStyleableProperty meta-0 node)
        meta-extracted-0
         {:settable?  (.isSettable meta-0 node)
          :inherits?  (.isInherits meta-0)
          :name       (when prop (.getName prop))
          :value      (when prop (.getValue prop))}
        meta-extracted-f
          (if (nempty? (.getSubProperties meta-0))
              (assoc meta-extracted-0 :sub-properties
                (extract-css-metadata (.getSubProperties meta-0) node))
              meta-extracted-0)
        meta-name (-> meta-0 (.getProperty) keyword)]
    [meta-name meta-extracted-f]))

(defn- extract-css-metadata [meta-list ^Node node]
  (->> meta-list
       (map (f*n extract-css-metadatum node))
       (into (sorted-map)))) ; TODO use |redm|

(defn get-css
  "Retrieves the CSS metadata for a JavaFX node."
  [^Node node]  ; getSubProperties
  (-> node
      (.getCssMetaData)
      (extract-css-metadata node)))