(ns
  ^{:doc "Image library. Conversion."
    :attribution "Alex Gunnarson"}
  quantum.imaging.convert
  (:require-quantum [:lib]))

(def supported-types #{:jpg :png :tiff :jpeg :gif :pdf})

(defn- convert!*
  [from-type from to-type to]
    (sh/run-process! "convert"
       [(str #_[from-type ":"] (-> from io/file-str))
        (str #_[to-type   ":"] (-> to   io/file-str))]
       {:dir (io/file-str [:resources "Images"])}))

(defn convert!
  "DOC http://libjpeg-turbo.virtualgl.org"
  {:usage
    '(convert!
       {:in  (io/file-str [:resources "images" "test-ocr-in.gif"])
        :out (io/file-str [:resources "images" "test-ocr-in.jpg"])})}
  ([{:keys [from from-type to to-type] :as opts}]
    (let [from (io/file-str from) to (io/file-str to)]
      (with-throw (nempty? from) "From-file cannot be empty.")
      (with-throw (nempty? to  ) "To-file cannot be empty."  )
      (with-throw (io/exists? from) (str/sp "From-file" (str/squote from) "does not exist."))
      ;(with-throw (io/readable? from) "Insufficient permissions to read from-file.")
      ;(with-throw (io/writable? to  ) "Insufficient permissions to write to to-file.")

      (let [from-type (keyword (or from-type (io/file-ext from)))
            to-type   (keyword (or to-type   (io/file-ext to  )))]
        (with-throw
          (in? from-type supported-types)
          (str/sp "Type" (str/squote (name from-type)) "not supported."))
        (with-throw
          (in? to-type   supported-types)
          (str/sp "Type" (str/squote (name to-type))   "not supported."))
        (convert!* from-type from to-type to)))))