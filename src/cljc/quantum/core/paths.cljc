(ns quantum.core.paths
  (:require-quantum [ns logic fn coll io str err sys]))

(def paths-vecs
  {:ffmpeg [:this-dir "bin" "ffmpeg-static" "mac" "ffmpeg"]})

(def paths
  (->> paths-vecs
       (map-vals+ (f*n io/file-str))
       redm))

(defn validate-paths []
  (doseq [bin-name path paths]
    (throw-unless (io/exists? path)
      (Err. :binary-not-found "Binary not found at path."
        (kmap bin-name path)))))

#?(:clj
(def user-env
  (atom {"MAGICK_HOME"       (str/join sys/separator ["usr" "local" "Cellar" "imagemagick"])
         "DYLD_LIBRARY_PATH" (str/join sys/separator ["usr" "local" "Cellar" "imagemagick" "lib"])})))
