(ns
  ^{:doc "Image library. Conversion, compression, etc."
    :attribution "Alex Gunnarson"}
  quantum.google.drive.core
  (:require 
    [quantum.core.ns           :as ns :refer :all])
  (:gen-class))

(ns/require-all *ns* :clj :lib)

; Image I/O has built-in support for GIF, PNG, JPEG, BMP, and WBMP.
; Image I/O is also extensible so that developers or administrators can
; "plug-in" support for additional formats. For example, plug-ins for TIFF
; and JPEG 2000 are separately available.

(import 'javax.imageio.ImageIO)
(import 'java.awt.image.BufferedImage)

; (let [^BufferedImage img
;         (ImageIO/read
;           (java.io.File.
;             (io/parse-dirs-keys
;               [:home "Collections" "Images" "Backgrounds" "3331-11.jpg"])))]
;   img)
