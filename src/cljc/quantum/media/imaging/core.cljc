(ns
  ^{:doc "Image library. Conversion, compression, etc."
    :attribution "Alex Gunnarson"}
  quantum.media.imaging.core
  #?(:clj (:import javax.imageio.ImageIO
                   java.awt.image.BufferedImage)))

; Image I/O has built-in support for GIF, PNG, JPEG, BMP, and WBMP.
; Image I/O is also extensible so that developers or administrators can
; "plug-in" support for additional formats. For example, plug-ins for TIFF
; and JPEG 2000 are separately available.


(def imagemagick-help
  "Help with 'dyld: Library not loaded' error:
     http://www.imagemagick.org/discourse-server/viewtopic.php?t=22465")

; (let [^BufferedImage img
;         (ImageIO/read
;           (java.io.File.
;             (io/parse-dirs-keys
;               [:home "Collections" "Images" "Backgrounds" "3331-11.jpg"])))]
;   img)

; PROCEDURAL GENERATION

; mikera/clisk: procedural texture generation

; RENDERING/RAYTRACING

; Scene description / generation
