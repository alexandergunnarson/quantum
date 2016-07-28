(ns
  ^{:doc "Image library. Conversion."
    :attribution "Alex Gunnarson"}
  quantum.media.imaging.convert
  (:require
    #?(:clj [quantum.core.process
              :refer [proc!]])
            [quantum.core.io    :as io  ]
            [quantum.core.paths :as path]
            [quantum.core.collections
              :refer [nempty? in?]]))

(def supported-types #{:jpg :png :tiff :jpeg :gif :pdf})

(defn- convert!*
  [from-type from to-type to]
    (proc! "convert"
       [(str #_[from-type ":"] (-> from path/file-str))
        (str #_[to-type   ":"] (-> to   path/file-str))]
       {:dir (path/file-str [:resources "Images"])}))

(defn convert!
  "DOC http://libjpeg-turbo.virtualgl.org"
  {:usage
    `(convert!
       {:in  (path/file-str [:resources "images" "test-ocr-in.gif"])
        :out (path/file-str [:resources "images" "test-ocr-in.jpg"])})}
  ([{:keys [from from-type to to-type] :as opts}]
    (let [from (path/file-str from) to (path/file-str to)]
      (assert (nempty? from))
      (assert (nempty? to  ))
      (assert (path/exists? from))
      ;(with-throw (io/readable? from) "Insufficient permissions to read from-file.")
      ;(with-throw (io/writable? to  ) "Insufficient permissions to write to to-file.")

      (let [from-type (keyword (or from-type (path/file-ext from)))
            to-type   (keyword (or to-type   (path/file-ext to  )))]
        (assert (in? from-type supported-types))
        (assert (in? to-type   supported-types))
        (convert!* from-type from to-type to)))))