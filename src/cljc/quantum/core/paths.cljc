(ns ^{:doc "Paths-related things — resource locators. URIs, URLs, directories, files, etc."}
  quantum.core.paths
  (:require-quantum [:core logic fn #_coll #_io err sys str loops])
  #?(:clj (:import java.io.File)))

; TODO validate this
(def paths-vecs
  {:ffmpeg [:this-dir "bin" "ffmpeg-static" "mac" "ffmpeg"]})

;#?(:clj
;(def paths
;  (->> paths-vecs
;       (map-vals+ (f*n io/file-str))
;       redm)))

(defn path
  "Joins string paths (URLs, file paths, etc.)
  ensuring correct separator interposition."
  {:usage '(path "foo/" "/bar" "baz/" "/qux/")
   :todo ["Configuration for system separator vs. 'standard' separator etc."]}
  [& parts]
  (apply str/join-once sys/separator parts))

;#?(:clj
;(defn validate-paths []
;  (doseq [bin-name path paths]
;    (throw-unless (io/exists? path)
;      (Err. :binary-not-found "Binary not found at path."
;        (kmap bin-name path))))))

;#?(:clj
;(def user-env
;  (atom {"MAGICK_HOME"       (str/join sys/separator ["usr" "local" "Cellar" "imagemagick"])
;         "DYLD_LIBRARY_

#_(defnt extension
  ([^string? s] (coll/taker-until-workaround "." nil s))
  ([^file?   f] (-> f str extension)))

#?(:clj
(defn extension [x]
  (cond (vector? x)
        (-> x last extension)
        (string? x)
        (.substring ^String x
          (inc (.indexOf ^String x ".")))
        :else (throw (->ex :no-matching-expr "Unknown type" (class x))))))

#?(:clj (defalias file-ext extension))

; TODO determine if useful
#?(:clj
(defn find-directory
  "Returns the file object if the given file is in the given directory, nil otherwise."
  [directory file-name]
  (when (and file-name directory (string? file-name) (instance? File directory))
    (let [file (File. ^String (.getPath directory) ^String file-name)]
      (when (and file (.exists file))
        file)))))
;___________________________________________________________________________________________________________________________________
;========================================================{ PATH, EXT MGMT }=========================================================
;========================================================{                }==========================================================
#_(defn up-dir-str [dir]
  (->> dir
       (<- whenf (f*n str/ends-with? sys/separator) popr)
       (dropr-until sys/separator)
       (<- whenc empty?
         (throw (->ex :err/io "Directory does not have a parent directory:" dir)))))

; (java.nio.file.Paths/get "asd/" (into-array ["/asdd/"]))

#_(defn path-without-ext [path-0]
  (coll/taker-after "." path-0))

#_(def- test-dir (path (System/getProperty "user.dir") "test"))

#?(:clj (def- this-dir (System/getProperty "user.dir")))

#?(:clj
(def- root-dir
  (condp = sys/os
    :windows (-> (System/getenv) (get "SYSTEMROOT") str)
    "/")))

#_(def- drive-dir
  (condp = sys/os
    :windows
      (whenc (getr root-dir 0
               (whenc (index-of root-dir "\\") (fn-eq? -1) 0))
             empty?
        "C:\\") ; default drive
    sys/separator))

#?(:clj (def- home-dir    (System/getProperty "user.home")))

#_(def- desktop-dir (path home-dir "Desktop"))

(def dirs (atom {}))
#_(def dirs
  (let [proj-path-0
          (or (get (System/getenv) "PROJECTS")
              (up-dir-str this-dir))
        proj-path-f 
          (ifn proj-path-0 (fn-> (index-of drive-dir) (= 0))
               path
              (partial path drive-dir))]
    (atom {:test      test-dir
           :this-dir  this-dir
           :root      root-dir
           :drive     drive-dir
           :home      home-dir
           :desktop   desktop-dir
           :projects  proj-path-f
           :keys      (if (= "local" (System/getProperty "quantum.core.io:paths:keys"))
                          (path this-dir "dev-resources" "Keys")
                          (path home-dir "Quanta" "Keys"))
           :dev-resources (path this-dir "dev-resources")
           :resources
             (whenc (path this-dir "resources")
                    (fn-> io/as-file .exists not)
                    (path proj-path-f (up-dir-str this-dir) "resources"))})))

#_(defnt ^String parse-dir
  ([^vector? keys-n]
    (reducei
      (fn [path-n key-n n]
        (let [first-key-not-root?
               (and (= n 0) (string? key-n)
                    ((complement (MWA 2 str/starts-with?)) key-n sys/separator))
              k-to-add-0 (or (get @dirs key-n) key-n)
              k-to-add-f
                (if first-key-not-root?
                    (str sys/separator k-to-add-0)
                    k-to-add-0)]
          (path path-n k-to-add-f)))
      "" keys-n))
  ([^string?  s] s)
  ([^keyword? k] (parse-dir [k]))
  ([          obj] (if (nil? obj) "" (throw (->ex :unimplemented nil {:obj obj :class (class obj)})))))

(defn parse-dir [x]
  (cond (vector? x)
        (reducei
          (fn [path-n key-n n]
            (let [first-key-not-root?
                   (and (= n 0) (string? key-n)
                        ((complement (MWA 2 str/starts-with?)) key-n sys/separator))
                  k-to-add-0 (or (get @dirs key-n) key-n)
                  k-to-add-f
                    (if first-key-not-root?
                        (str sys/separator k-to-add-0)
                        k-to-add-0)]
              (path path-n k-to-add-f)))
          "" x)))

#_(defnt ^java.io.File as-file
  ([^vector? dir] (-> dir parse-dir as-file))
  ([^string? dir] (-> dir io/file))
  ([^file?   dir] dir))

#_(def file-str (fn-> as-file str))

#_(defnt exists?
  ([^string? s] (-> s as-file exists?))
  ([^file?   f] (.exists f)))

#_(defnt directory?
  ([^string? s] (-> s as-file directory?))
  ([^file?   f] (and (exists? f) (.isDirectory f))))

#_(defalias folder? directory?)

#_(ns-unmap 'quantum.core.io.core 'file?)
#_(def file? (fn-not directory?))

#_(def clj-extensions #{:clj :cljs :cljc})

#_(def clj-file?
  (fn-and file?
    (fn->> extension keyword (containsv? clj-extensions))))

; FILE RELATIONS

#_(defnt ^String up-dir
  ([^string? dir ] (-> dir up-dir-str))
  ([^vec?    dir ] (-> dir parse-dir up-dir))
  ([^file?   file] (.getParent file)))

#_(defn parent [f]
  (.getParentFile ^File (as-file f)))

#_(defn children [f]
  (let [^File file (as-file f)]
    (when (directory? file)
      (->> file (.listFiles) seq))))

#_(defn siblings [f]
  (let [^File file (as-file f)]
    (->> file parent children
         (remove (fn-> str (= (str file)))))))

#_(def directory-?       (mfn 1 directory?))
#_(def descendants       (fn->> as-file file-seq))
#_(def descendant-leaves (fn->> as-file file-seq (remove directory-?)))
#_(def internal-nodes    (fn->> as-file file-seq (filter directory-?)))
#_(def descendant-dirs internal-nodes)