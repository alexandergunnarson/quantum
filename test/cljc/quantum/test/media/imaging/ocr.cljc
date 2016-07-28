(ns quantum.test.media.imaging.ocr
  (:require [quantum.media.imaging.ocr :refer :all]))

(defn test:ocr
  [in out & {:keys [lang data-dir pdf?] :as opts}])


