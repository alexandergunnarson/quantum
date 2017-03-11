(ns quantum.core.paths
  "Paths-related things — resource locators. URIs, URLs, File, Path, directories, etc."
  (:refer-clojure :exclude [descendants contains?])
  (:require
  #?(:clj
    [clojure.java.io          :as io])
    [quantum.core.collections :as coll
      :refer [slice index-of containsv? popr reducei dropr-until]]
    [quantum.core.error       :as err
      :refer [->ex TODO]]
    [quantum.core.fn          :as fn
      :refer [<- fn-> fn->> fn1 mfn]]
    [quantum.core.logic       :as logic
      :refer [fn= fn-not fn-and whenf whenc ifn]]
    [quantum.core.macros      :as macros
      :refer [defnt]]
    [quantum.core.system      :as sys]
    [quantum.core.vars        :as var
      :refer [defalias def-]]
    [quantum.core.string      :as str])
  (:require-macros
    [quantum.core.paths
      :refer [->file exists?]])
  #?(:clj
  (:import
    [java.io File]
    [java.nio.file Path Paths]
    [java.net URI URL URLEncoder])))

; TODO validate this
(def paths-vecs
  {:ffmpeg [:this-dir "bin" "ffmpeg-static" "mac" "ffmpeg"]})

(declare ->uri-protocol parse-dir-protocol)


(defnt #?(:clj ^java.io.File ->file
          :cljs              ->file)
  {:todo "Eliminate reflection"}
  #?(:cljs ([                    x] (TODO)))
  #?(:clj  ([^File               x] x          ))
  #?(:clj  ([^java.nio.file.Path x] (.toFile x)))
  #?(:clj  ([#{string? URI}      x] (File.   x)))
  #?(:clj  ([^URL                x] (-> x ->uri-protocol ->file)))
  #?(:clj  ([#{+vec? keyword?}   x] (-> x parse-dir-protocol ->file)))
  #?(:clj  ([                    x] (io/file x))))

#?(:clj
(defnt ^java.net.URI ->uri
  {:todo "Eliminate reflection"}
  ([^URI        x] x)
  ([^Path       x] (.toUri x))
  ([#{File URL} x] (.toURI x))
  ([^string?    x] (-> x (str/replace " " "+") (URI.)))
  ([            x] (-> x ->file ->uri))))

#?(:clj
(defnt ^java.net.URL ->url
  ([^URL     x] x)
  ([^string? x] (URL. x))
  ([^URI     x] (.toURL x))
  ([         x] (-> x ->uri ->url))))

#?(:clj
(defnt ^java.nio.file.Path ->java-path
  ([^Path               x] x)
  ([#{File string? URL} x] (-> x ->uri ->java-path))
  ([^URI                x] (Paths/get x))))

#?(:clj
(defnt ^boolean contains?
  "Does the filesystem contain @`x`?
   Note that on a Mac, if there is a mountable external drive such as a Time Capsule,
   you must mount it first (go to it in Finder) before it is considered accessible by
   Java."
  ([^File    x] (.exists x))
  ([^string? x] (-> x ->file contains?))))

;#?(:clj
;(def paths
;  (->> paths-vecs
;       (map-vals+ (fn1 io/file-str))
;       redm)))

(defn path
  "Joins string paths (URLs, file paths, etc.)
  ensuring correct separator interposition."
  {:usage '(path "foo/" "/bar" "baz/" "/qux/")
   :todo ["Configuration for system separator vs. 'standard' separator etc."]}
  [& parts]
  (apply str/join-once sys/separator parts))

(defn url-path
  [& parts]
  (apply str/join-once "/" parts))

;#?(:clj
;(defn validate-paths []
;  (doseq [bin-name path paths]
;    (throw-unless (io/exists? path)
;      (Err. :binary-not-found "Binary not found at path."
;        (kw-map bin-name path))))))

;#?(:clj
;(def user-env
;  (atom {"MAGICK_HOME"       (str/join sys/separator ["usr" "local" "Cellar" "imagemagick"])
;         "DYLD_LIBRARY_


(defnt path->file-name
  #?(:clj ([^file?   f] (.getName f)))
  ([^string? s] (coll/taker-until sys/separator nil s)))

(defnt extension ; TODO really should take from first "." after the last "/"
  ([^string? s] (coll/taker-until "." nil s))
  #?(:clj ([^file?   f] (-> f str extension))))

; #?(:clj
; (defn extension [x]
;   (cond (vector? x)
;         (-> x last extension)
;         (string? x)
;         (.substring ^String x
;           (inc (.indexOf ^String x ".")))
;         (nil? x) nil
;         :else (throw (->ex :no-matching-expr "Unknown type" (class x))))))

#?(:clj (defalias file-ext extension))

; TODO determine if useful
#?(:clj
(defn find-directory
  "Returns the file object if the given file is in the given directory, nil otherwise."
  [directory file-name]
  (when (and file-name directory (string? file-name) (instance? File directory))
    (let [file (File. ^String (.getPath ^File directory) ^String file-name)]
      (when (and file (.exists file))
        file)))))
;___________________________________________________________________________________________________________________________________
;========================================================{ PATH, EXT MGMT }=========================================================
;========================================================{                }==========================================================
; (java.nio.file.Paths/get "asd/" (into-array ["/asdd/"]))

(defn path-without-ext [path-0]
  (coll/taker-after "." path-0))

#?(:clj (def- test-dir (path (System/getProperty "user.dir") "test")))

#?(:clj (def- this-dir (System/getProperty "user.dir")))

#?(:clj
(def- root-dir
  (condp = sys/os
    :windows (-> (System/getenv) (get "SYSTEMROOT") str)
    "/")))

#?(:clj (def- drive-dir
  (condp = sys/os
    :windows
      (whenc (slice root-dir 0
               (inc (whenc (index-of root-dir "\\") (fn= -1) 0)))
             empty?
        "C:\\") ; default drive
    sys/separator)))

#?(:clj (def- home-dir    (System/getProperty "user.home")))

#?(:clj (def- desktop-dir (path home-dir "Desktop")))

(defn up-dir-str [dir]
  (->> dir
       (<- whenf (fn1 str/ends-with? sys/separator) popr)
       (dropr-until sys/separator)
       (<- whenc empty?
         (throw (->ex :err/io "Directory does not have a parent directory:" dir)))))

(def dirs
  #?(:clj
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
               :keys      (if (= "global" (System/getProperty "quantum.core.io:paths:keys"))
                              (path home-dir "Quanta" "Keys")
                              (path this-dir "dev-resources" "Keys"))
               :dev-resources (path this-dir "dev-resources")
               :resources
                 (whenc (path this-dir "resources")
                        (fn-not #(.exists (File. ^String %)))
                        (path proj-path-f (up-dir-str this-dir) "resources"))}))
     :cljs (atom {})))

(defnt #?(:clj parse-dir :cljs parse-dir)
  ([^+vec? keys-n]
    (reducei
      (fn [path-n key-n n]
        (let [first-key-not-root?
               (and (= n 0) (string? key-n)
                    ((complement #(str/starts-with? %1 %2)) key-n sys/separator))
              k-to-add-0 (or (get @dirs key-n) key-n)
              k-to-add-f
                (if first-key-not-root?
                    (str sys/separator k-to-add-0)
                    k-to-add-0)]
          (path path-n k-to-add-f)))
      "" keys-n))
  ([^string?  s] s)
  ([^keyword? k] (parse-dir [k]))
  #?(:clj ([^file?    x] (str x)))
  ([^default  obj] (if (nil? obj)
                       ""
                       (throw (->ex :unimplemented nil
                                    {:obj obj :class (type obj)})))))

; (defn parse-dir [x]
;   (cond (vector? x)
;         (reducei
;           (fn [path-n key-n n]
;             (let [first-key-not-root?
;                    (and (= n 0) (string? key-n)
;                         ((complement (MWA 2 str/starts-with?)) key-n sys/separator))
;                   k-to-add-0 (or (get @dirs key-n) key-n)
;                   k-to-add-f
;                     (if first-key-not-root?
;                         (str sys/separator k-to-add-0)
;                         k-to-add-0)]
;               (path path-n k-to-add-f)))
;           "" x)))

(def file-str (fn-> ->file str))

(declare exists?-protocol)
(defnt exists?
          ([^string? s] (-> s ->file exists?))
  #?(:clj ([^file?   f] (.exists f))))

#?(:clj
(defnt directory?
  ([^string? s] (-> s ->file directory?))
  ([^file?   f] (and (exists? f) (.isDirectory f)))))

#?(:clj (defalias folder? directory?))

#_(ns-unmap 'quantum.core.io.core 'file?)
#?(:clj (def file? (fn-not directory?)))

(def clj-extensions #{:clj :cljs :cljc :cljx :cljd})

#?(:clj
(def clj-file?
  (fn-and file?
    (fn->> extension keyword (containsv? clj-extensions)))))

; FILE RELATIONS

(defnt #?(:clj ^String up-dir :cljs up-dir)
  ([^string? dir ] (-> dir up-dir-str))
  ([^+vec?   dir ] (-> dir parse-dir up-dir))
  #?(:clj ([^file?   file] (.getParent file))))

#?(:clj (defalias parent up-dir))

#?(:clj
(defn children [f]
  (let [^File file (->file f)]
    (when (directory? file)
      (->> file (.listFiles) seq)))))

#?(:clj
(defn siblings [f]
  (let [file (->file f)]
    (->> file parent children
         (remove (fn-> str (= (str file))))))))

#?(:clj (def directory-?       (mfn 1 directory?)))
#?(:clj (def descendants       (fn->> ->file file-seq)))
#?(:clj (def descendant-leaves (fn->> ->file file-seq (remove directory-?))))
#?(:clj (def internal-nodes    (fn->> ->file file-seq (filter directory-?))))
#?(:clj (def descendant-dirs internal-nodes))

; TODO
#?(:cljs (defrecord URI [path]))
