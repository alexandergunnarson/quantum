(ns quantum.core.convert
  (:require-quantum [ns macros type io])
#?(:clj
  (:import
    (java.io File)
    (java.net URI)
    java.nio.file.Path
    java.nio.file.Paths
    javafx.collections.FXCollections)))

#?(:clj (do

(defnt ->file
  [Path]   ([x] (.toFile x))
  :default ([x] (io/file x)))

(defnt ->uri
  [Path]   ([x] (.toUri x))
  [File]   ([x] (.toURI x))
  :default ([x] (-> x ->file ->uri)))

(defnt ->url
  [URI]  ([x] (.toURL x)))

(defnt ->path
  :default ([x] (Paths/get ^URI (->uri x))))

(defnt ->observable ; O(1)
  vector? ([v] (FXCollections/observableArrayList v))
  listy?  ([l] (FXCollections/observableArrayList l)))
))

; (defn mime-type
;   {:source ["https://en.wikipedia.org/wiki/List_of_file_signatures"
;             "https://mimesniff.spec.whatwg.org/#matching-an-image-type-pattern"]}
;   [file]
;   (let [file-reader (js/FileReader.)
;         header (atom nil)]
;     (set! (.-onloadend file-reader)
;       (fn [e]
;         (when-let [result (-> e .-target .-result)]
;           (let [arr (-> result (js/Uint8Array.) (.subarray 0 4))]
;             (reset! header
;               (reduce
;                 (fn [ret elem]
;                   (str ret (.toString elem 16)))
;                 ""
;                 (array-seq arr)))))))
;     ; Check the file signature against known types
;     (.readAsArrayBuffer file-reader file)
;     (condpc = header
;       "89504e47" :image/png
;       "47494638" :image/gif
;       (coll-or "ffd8ffe0" "ffd8ffe1" "ffd8ffe2") :image/jpeg
;       (.-type file))) 
; )