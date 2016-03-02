(ns quantum.imaging.ocr
  (:require-quantum [:lib]))

(def tesseract-help
  "INSTALLING
   http://emop.tamu.edu/Installing-Tesseract-Mac

   ./autobuild
   ./configure
   make
   sudo make install
   
   Install homebrew
   <ruby -e \"$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/master/install)\">

   Install useful uninstall extension for homebrew
   <brew tap beeftornado/rmtree && brew install brew-rmtree>

   Install boost
   <brew install boost>

   Install qt4
   <brew install qt>
   
   Install scantailor (http://scantailor.org)
   - Download from https://github.com/scantailor/scantailor/releases/tag/RELEASE_0_9_11_1
   - Deps: qt4, boost
   - Modify \"foundation/GridLineTraverser.cpp\":
     - Add \"#include <cmath>\"
     - Replace line 30 with:
       - \"(h_spans = std::abs((double)(p1.x() - p2.x()))) > (v_spans = std::abs((double)(p1.y() - p2.y())))\"
   - Terminal:
   <   untar __DOWNLOADED FILE__
    && cd __DOWNLOADED FOLDER__
    && cmake ./
    && make>

   Install ImageMagick   <brew install imagemagick>
   Install libjpeg-turbo <brew install libjpeg-turbo>
   ")

(defn+ ocr
  "Help:
    Tesseract works best with text using a DPI of at least 300 dpi,
    so it may be beneficial to resize images.

    Parameters available at http://www.sk-spell.sk.cx/tesseract-ocr-parameters-in-302-version

    Improving output quality: https://code.google.com/p/tesseract-ocr/wiki/ImproveQuality

    Usage: https://tesseract-ocr.googlecode.com/git/doc/tesseract.1.html"
  {:usage '(ocr
             [:resources "Images" "test-ocr-in.gif"]
             [:resources "Images" "test-ocr-out.txt"]
             :data-dir ["usr" "local" "share"]
             :lang "eng" "tessedit_write_images" true)}
  [in out & {:keys [lang data-dir pdf?] :or {data-dir (sys/env-var "TESSDATA_PREFIX")} :as opts}]
  (let [image-file  (-> in  io/file-str)
        outbase     (-> out io/file-str io/path-without-ext)
        timeout     10000
        language    (or lang "eng")
        opts-f      (if pdf?
                        (-> opts
                            (assoc "tessedit_create_pdf"   1
                                   "tessedit_pageseg_mode" 1)
                            (dissoc :pdf?))
                        opts)
        args        (->> opts-f
                         (filter+ (extern (fn-> key string?)))
                         (map+ (fn [[opt v]]
                                 (let [v-f ((extern (if*n (eq? true) (constantly "T") str)) v)]
                                   ["-c" (str opt "=" v-f)])))
                         flatten+
                         (into [image-file outbase "--tessdata-dir" (io/file-str data-dir) "-l" language]))]
    (sh/run-process! "tesseract" args {:timeout timeout})))


