(ns quantum.media.imaging.ocr
  (:require
    [quantum.core.paths       :as path]
    [quantum.core.system      :as sys ]
    [quantum.core.io          :as io  ]
    [quantum.core.process
      :refer [proc!]]
    [quantum.core.fn
      :refer [fn-> fn']]
    [quantum.core.logic
      :refer [ifn1]]
    [quantum.core.collections :as coll
      :refer [join map+ filter+ flatten+]]))

(defn ocr
  "Uses the open-source tool `tesseract` to perform OCR on a document.
   Refer to the Tesseract GitHub for help and setup.

   To OCR a PDF:
   `gs -o <output-TIFF> -sDEVICE=tiffg4 <input-PDF> && tesseract <output-TIFF> stdout pdf > <output-PDF>`"
  [in out opts]
  (TODO))


