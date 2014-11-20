(ns quanta.library.io
  (:refer-clojure :exclude [read])
  (:import
    (java.io File FileNotFoundException PushbackReader
      FileReader DataInputStream DataOutputStream IOException
      FileOutputStream BufferedOutputStream BufferedInputStream
      FileInputStream)
    (java.util.zip ZipOutputStream ZipEntry)
    java.util.List
    (org.apache.commons.io FileUtils))
  (:gen-class))
(require
  '[quanta.library.ns          :as ns    :refer [defalias alias-ns]])
(ns/require-all *ns* :clj)
(ns/nss *ns*)
(require
  '[clojure.java.io            :as clj-io]
  '[clojure.data.csv           :as csv]
  '[quanta.library.data.array  :as arr  :refer :all]
  '[quanta.library.string      :as str]
  '[quanta.library.time.core   :as time]
  '[quanta.library.print       :as pr   :refer [! pprint]]
  '[quanta.library.collections :as coll :refer :all]
  '[quanta.library.numeric     :as num  :refer [greatest-or]]
  '[quanta.library.logic                :refer :all]
  '[quanta.library.type                 :refer :all]
  '[quanta.library.function             :refer :all]
  '[quanta.library.system      :as sys]
  '[quanta.library.error       :as err  :refer [try+ throw+]]
  '[taoensso.nippy             :as nippy]
  '[iota                       :as iota])

; http://www.brandonbloom.name/blog/2013/06/26/slurp-and-spit/
; But files aren’t “Web Scale”!
; Is that really true? And do you really care? Should you really care?
; The answer to all of these questions is “No”. Files can easily be “web scale”.
; As of 2013, Hacker News is still running as a single process, on a single core, 
; of a single server, backed by a directory structure of simple data files. 
; Nearly 2 million page views are served daily. 

(set! *warn-on-reflection* true)

(defalias file clj-io/file)
(defn exists? [^String path-0] (.exists ^File (clj-io/as-file path-0)))

(defn- double-escape [^String x]
  (.replace x "\\" "\\\\"))
(defn- ^bytes parse-bytes [encoded-bytes]
  (->> (re-seq #"%.." encoded-bytes)
       (map+ (f*n subs 1))
       (map+ #(.byteValue ^Integer (Integer/parseInt % 16)))
       fold+
       (byte-array)))
(defn url-decode
  "Decode every percent-encoded character in the given string using the
  specified encoding, or UTF-8 by default."
  ^{:attribution "ring.util.codec.percent-decode"}
  [encoded & [encoding]]
  (str/replace
    encoded
    #"(?:%..)+"
    (fn [chars]
      (-> ^bytes (parse-bytes chars)
          (String. (or encoding "UTF-8"))
          (double-escape)))))
;___________________________________________________________________________________________________________________________________
;========================================================{ PATH, EXT MGMT }=========================================================
;========================================================{                }==========================================================
; path
; filename
; extension

(def ^:dynamic *os-separator* (-> (File/separatorChar) str)) ; java.io.File/pathSeparator??
(def ^:dynamic *os-sep-esc*
  (case sys/*os*
    :windows "\\\\"
    "/"))
(defn path
  "Joins string paths (URLs, file paths, etc.)
  ensuring correct separator interposition.
  USAGE: (path \"foo/\" \"/bar\" \"baz/\" \"/qux/\")"
  ^{:attribution "taoensso.encore"}
  [& parts]
  (apply str/join-once *os-separator* parts))
(defn up-dir [dir & [separator]]
  (let [sep-f (or separator *os-separator*)
        dir-f (whenf dir (compr last+ (eq? sep-f)) popr+)]
    (str/subs+ dir-f 0 (inc (last-index-of+ dir-f sep-f)))))
(defn file-name-from-path [path-0]
  (let [path-f (str path-0)]
    (str/subs+ path-f
        (inc (num/greatest
               [(last-index-of+ path-f "/")
                (last-index-of+ path-f "\\")]))
        (count path-f))))
(def  ext-index (f*n last-index-of+ "."))
(defn path-without-ext [path-0]
  (let [file-name-f (file-name-from-path path-0)
        ext-index-from-end
          (- (count file-name-f) (ext-index file-name-f))]
    (if (= (ext-index file-name-f) -1)
        path-0
        (getr+ path-0 0 (- (count path-0) ext-index-from-end)))))
(defn file-ext [path-0]
  (let [file-name-f (file-name-from-path path-0)]
    (when (not= (ext-index file-name-f) -1)
      (getr+ file-name-f
        (inc (ext-index file-name-f))
        (count file-name-f)))))
(defn folder? [^String path-0]
  (-> path-0 file-ext nil?))
(def  dirs
  (let [test-dir
          (try+ (-> (clj-io/resource "") url-decode
                   (str/replace #"^file:/" "/")
                   (str/replace #"/" *os-sep-esc*))
            (catch Exception _ "")) ; To handle a weird "MapEntry cannot be cast to Number" error
        this-dir (up-dir test-dir)
        root (case sys/*os*
               :windows (-> (System/getenv) (get "SYSTEMROOT") str)
               "/")
        drive (case sys/*os*
                :windows
                (whenc (getr+ root 0 (whenc (index-of+ root "\\") (eq? -1) 0))
                       empty?
                  "C:\\") ; default drive
                "/")
        proj-path-0
          (ifn (get (System/getenv) "PROJECTS") nil?
               (constantly (up-dir this-dir))
               identity)
        proj-path-f 
          (if (= 0 (index-of+ proj-path-0 drive))
              (path proj-path-0)
              (path drive proj-path-0))
        home    (System/getProperty "user.home")
        desktop (path home "Desktop")]
    {:root      root
     :drive     drive
     :home      home
     :desktop   desktop
     :projects  proj-path-f
     :resources
       (whenc (path this-dir "resources")
              (fn-not exists?)
              (path proj-path-f "clj-qb" "resources"))
     :test      test-dir
     :this-dir  this-dir}))
(defn parse-dirs-keys [keys-n]
  (reduce+
    (fn [path-n key-n]
      (path path-n
        (whenc (get dirs key-n)
               nil?
               key-n)))
    "" (coll-if keys-n)))
(defn parent [dir]
  (-> dir coll-if parse-dirs-keys up-dir clj-io/as-file))
(defn siblings [dir]
  (-> dir coll-if parse-dirs-keys parent file-seq vec+ popl+))
(defn children [dir]
  (-> dir coll-if parse-dirs-keys clj-io/as-file file-seq vec+ popl+))
;___________________________________________________________________________________________________________________________________
;========================================================{ FILES AND I/O  }=========================================================
;========================================================{                }==========================================================
(defn create-dir! [dir-0]
  (let [dir   (-> dir-0 coll-if parse-dirs-keys)
        ^File dir-f (clj-io/as-file dir)]
    (if (exists? dir)
        (println "Directory already exists:" dir)
        (try (.mkdir dir-f)
             (println "Directory created:" dir)
          (catch SecurityException e (println "The directory could not be created. A security exception occurred."))))))
(defn num-to-sortable-str [num-0]
  (ifn num-0 (fn-and num/nneg? (f*n < 10))
       (partial str "0")
       str))
(defn next-file-copy-num [path-0]
  (let [extension (file-ext path-0)
        file-name (-> path-0 file-name-from-path path-without-ext)]
    (try
      (->> path-0
           siblings
           (map+ str)
           (filter+
             (partial
               (fn-and
                 (compr file-name-from-path (f*n str/starts-with? file-name))
                 (compr file-ext (eq? extension)))))
           (map+ (fn-> file-name-from-path
                       (str/replace (str file-name " ") "") 
                       path-without-ext str/val))
           (filter+ number?)
           fold+ num/greatest inc num-to-sortable-str)
      (catch Exception _ (num-to-sortable-str 1)))))

(defn write-unserialized! [data path- & {:keys [type] :or {type :string}}]
  (condpc = type
    (coll-or :str :string :txt) 
      (spit path- data)
    :csv  (with-open [out-file   (clj-io/writer path-)]  ;  :append true... hmm...
            (csv/write-csv out-file data))
    :xls  (with-open [write-file (clj-io/output-stream path-)]
            (.write data write-file))
    :xlsx (with-open [write-file (clj-io/output-stream path-)]
            (.write data write-file))))
(defn write-serialized! ; can encrypt :encrypt-with :....
  [data path-0 write-method]
  (with-open [write-file (clj-io/output-stream path-0)]
    (nippy/freeze-to-out!
      (DataOutputStream. write-file)
      (case write-method
        :serialize data
        :compress  (nippy/freeze data))))) ; byte-code
(def ^:private ill-chars-table
  {"\\" "-", "/" "-", ":" "-", "*" "!", "?" "!"
   "\"" "'", "<" "-", ">" "-", "|" "-"})
(defn conv-ill-chars [str-0] ; Make less naive - Mac vs. Windows, etc.
  (reduce+ 
    (fn [str-n k v] (str/replace str-n k v))
    str-0 ill-chars-table))
(defn write-try
  [n successful? file-name-f directory-f
   file-path-f write-method data-formatted file-type]
  (cond successful? (print " complete.\n")
        (> n 2)     (println "Maximum tries exceeded.")
        :else
        (try+
          (print "Writing" file-name-f "to" directory-f (str "(try " n ")..."))
          (condpc = write-method
            :print  (write-unserialized! data-formatted file-path-f :type :string)
            :pretty (pprint data-formatted (clj-io/writer file-path-f)) ; is there a better way to do this?
            (coll-or :serialize :compress)
              (if (splice-or file-type = "csv" "xls" "xlsx" "txt")
                  (write-unserialized! data-formatted file-path-f :type (keyword file-type))
                  (write-serialized!   data-formatted file-path-f write-method))
            (println "Unknown write method requested."))
          #(write-try (inc n) true file-name-f directory-f
             file-path-f write-method data-formatted file-type)
          (catch FileNotFoundException _
            (create-dir! directory-f)
            #(write-try (inc n) false file-name-f directory-f
               file-path-f write-method data-formatted file-type)))))
(defn write! ; can have list of file-types ; should detect file type from data... ; create the directory if it doesn't exist
  {:todo ["apparently have problems with using the :directory key... weird"]}
  [& {file-name :name file-path :path
      :keys [data directory file-type
             write-method overwrite formatting-func]
      :or   {data            nil
             directory       :resources
             file-name       "Untitled"
             file-type       "cljx"
             write-method    :serialize ; :compress ; can encrypt :encrypt-with :.... ; :write-method :pretty
             overwrite       true  ; :date, :num :num-0
             formatting-func identity}
      :as   options}]
  (doseq [file-type-n (coll-if file-type)]
    (let [file-path-parsed (-> file-path coll-if parse-dirs-keys)
          directory-parsed (-> directory coll-if parse-dirs-keys)
          directory-f
            (or (-> file-path-parsed up-dir   (whenc empty? nil))
                directory-parsed)
          extension
            (or (-> file-path-parsed file-ext (whenc empty? nil))
                (file-ext file-name)
                file-type)
          file-name-0
            (or (-> file-path-parsed file-name-from-path conv-ill-chars (whenc empty? nil))
                (-> file-name conv-ill-chars path-without-ext (str "." extension)))
          file-name-00
            (or (-> file-path-parsed file-name-from-path conv-ill-chars (whenc empty? nil)
                    (whenf nnil? (fn-> path-without-ext (str " 00." extension))))
                (-> file-name conv-ill-chars path-without-ext (str " 00." extension)))
          file-path-0  (path directory-f file-name-0)
          date-spaced
            (when (and (= overwrite :date) (exists? file-path-0))
              (str " " (time/now-formatted "MM-dd-yyyy HH|mm")))
          file-num
            (cond
              (and (splice-or overwrite = :num :num-0)
                   (some exists? [file-path-0 (path directory-f file-name-00)]))
              (next-file-copy-num file-path-0)
              (and (= overwrite :num-0) ((fn-not exists?) file-path-0))
              "00"
              :else nil)
          file-name-f
            (-> file-name-0 path-without-ext
                (str (or date-spaced
                         (whenf file-num nnil? (partial str " ")))
                     "." extension))
          file-path-f (path directory-f file-name-f)
          data-formatted
            (case file-type
              "html" data ; (.asXml data) ; should be less naive than this
              "csv" (formatting-func data)
              data)]
            (println file-num)
            (println file-name-f)
            (println directory-f)
      (trampoline write-try 1 false
        file-name-f directory-f file-path-f write-method data-formatted file-type))))


(defn delete! ; TODO: Implement recycle bin functionality for Windows
  [& {file-name :name file-path :path
      :keys [directory silently?]
      :or   {silently? false}}]
  (let [file-path-parsed (-> file-path coll-if parse-dirs-keys)
        directory-parsed (-> directory coll-if parse-dirs-keys)
        directory-f
          (or (-> file-path-parsed up-dir   (whenc empty? nil))
              directory-parsed)
        extension
          (or (-> file-path-parsed file-ext (whenc empty? nil))
              (file-ext file-name))
        file-name-0
          (or (-> file-path-parsed file-name-from-path (whenc empty? nil))
              (-> file-name path-without-ext (str "." extension)))
        file-path-f    (path directory-f file-name-0)
        file-f         (clj-io/as-file file-path-f)
        success-alert! #(println "Successfully deleted:" file-path-f)
        fail-alert!    #(println "WARNING: Unknown IOException. Failed to delete:" file-path-f)]
    (if (exists? file-path-f)
        (try+ (if (clj-io/delete-file file-f silently?)
                  (success-alert!)
                  (fail-alert!))
          (catch IOException e
            (do (println "Couldn't delete file due to an IOException. Trying to delete as directory...")
                (FileUtils/deleteDirectory file-f)
                (if (exists? file-path-f)
                    (fail-alert!)
                    (success-alert!)))))
        (println "WARNING: File does not exist. Failed to delete:" file-path-f))))
(defn read
  [& {file-name :name file-path :path
      :keys [directory file-type read-method class-import]
      :or   {directory   :resources
             read-method :unserialize} ; :uncompress is automatic
      :as options}] ; :string??
  (when (fn? class-import)
    (class-import))
  (let [directory-f (-> directory coll-if parse-dirs-keys)
        file-path-f
          (or (-> file-path coll-if parse-dirs-keys (whenc empty? nil))
              (path directory-f file-name))
        extension (or file-type (file-ext file-path-f))]
    (condpc = read-method
      :load-file (load-file file-path-f) ; don't do this; validate it first
      :str-seq   (iota/seq file-path-f)
      :str-vec   (iota/vec file-path-f)
      :unserialize
        (condpc = extension
          "txt"
          (iota/seq file-path-f)
          "xlsx"
          (clj-io/input-stream file-path-f)
          "csv"
          (-> file-path-f clj-io/reader csv/read-csv)
          (whenf (with-open [read-file (clj-io/input-stream file-path-f)] ; Clojure object
                 (nippy/thaw-from-in! (DataInputStream. read-file)))
            byte-array? nippy/thaw))
      (println "Unknown read method requested.")))) ; byte-code


; ; Reads the next object from stream, which must be an instance of
; ; java.io.PushbackReader or some derivee. stream defaults to the
; ; current value of *in* .
; ;; WARNING: You SHOULD NOT use clojure.core/read or
; ;; clojure.core/read-string to read data from untrusted sources.  They
; ;; were designed only for reading Clojure code and data from trusted
; ;; sources (e.g. files that you know you wrote yourself, and no one
;; else has permission to modify them).
 
;; Instead, if you want a serialization format that can be read safely and
;; looks like Clojure data structures, use edn
;; (https://github.com/edn-format/edn). There is
;; clojure.edn/read and clojure.edn/read-string provided in Clojure
;; 1.5.
 
;; You definitely should not use clojure.core/read or read-string if
;; *read-eval* has its default value of true, because an attacker
;; could cause your application to execute arbitrary code while it is
;; reading.
;; It is straightforward to execute code which removes all of your files, copies them to
;; someone else's computer over the Internet, installs Trojans, etc.
;; Even if you do bind *read-eval* to false first, like so:
; (defn read-string-unsafely [s]
;   (binding [*read-eval* false]
;     (read-string s)))
 
;; you may hope you are safe reading untrusted data that way.

;; This causes a socket to be opened, as long as the JVM sandboxing
;; allows it.
; (read-string-unsafely "#java.net.Socket[\"www.google.com\" 80]")
;; This causes precious-file.txt to be created if it doesn't exist, or
;; if it does exist, its contents will be erased (given appropriate
;; JVM sandboxing permissions, and underlying OS file permissions).
; (read-string-unsafely "#java.io.FileWriter[\"precious-file.txt\"]") 
;; Because clojure.core/read and read-string are
;; designed to be able to do dangerous things, and they are not
;; documented nor promised to be safe from unwanted side effects.  If
;; you use them for reading untrusted data, and a dangerous side
;; effect is found in the future, you will be told that you are using
;; the wrong tool for the job.  clojure.edn/read and read-string, and
;; the tools.reader.edn library, are documented to be safe from
;; unwanted side effects, and if any bug is found in this area it
;; should get quick attention and corrected.

; ===== BENCHMARKS AND STATISTICS =====

; Clojure's reader allows you to take your data just about anywhere.
; But the reader can be painfully slow when you've got a lot of data to crunch (like when you're serializing to a database).
; Nippy is an attempt to provide a reliable, high-performance drop-in alternative to the reader.
; It's used, among others, as the Carmine Redis client and Faraday DynamoDB client serializer.
; READ
; 1)( iota/seq                     60.63  microseconds (but is really just a shell)
; 2) iota/vec                     30.63  ms (500 times slower)
; 3) slurp (java Reader?)         179.49 ms
; 4) nippy via io/read            25.11  ms
; 5) nippy uncompress via io/read 30.59  ms

; WRITE
; 1) nippy via io/write! (with printing)    31.73 ms ; Strangely, they have exactly the same number of bytes...
; 2) nippy                                  32.87 ms (30-34)
; 3) spit (java Writer?)                    38.49 ms (34-44)
; 5) nippy compress via io/write! (with pr) 58.01 ms
; read as byte-array


  ; (parse-dirs-keys [:desktop "testing.zip"])
(defn fast-file-zip [^String in ^String out]
  (let [^ZipOutputStream zos
         (->> (FileOutputStream. ^String out)
              (BufferedOutputStream.)
              (ZipOutputStream.))]
    (try
      (doseq [^File file-n (-> in (File.) file-seq rest)] ; for all the files/folders in the folder,
        (let [file-path (.getAbsolutePath file-n)
              bis (->> file-path
                       (FileInputStream.)
                       (BufferedInputStream.))]
          (try
            (let [data-to-write (byte-array 1024)
                  length (.read bis data-to-write)]
              (.putNextEntry zos (ZipEntry. file-path))
              (while (length > 0)
                (.write zos data-to-write 0 length)))
           (finally
             (.closeEntry zos)
             (.close      bis)))))
      (finally
        (.finish zos)
        (.close  zos)))))

; http://www.avajava.com/tutorials/lessons/how-do-i-zip-a-directory-and-all-its-contents.html

(defn getAllFiles [^File dir ^List fileList]
     (doseq [^File file (.listFiles dir)]
       (.add fileList file)
       (if (.isDirectory file)
           (do ;(println "directory:" (.getCanonicalPath file))
               (getAllFiles file, fileList)) ; recursive
           ;(println "     file:" (.getCanonicalPath file))
           )))
  (defn addToZip [^File directoryToZip ^File file ^ZipOutputStream zos]
   

    ; we want the zipEntry's path to be a relative path that is relative
    ; to the directory being zipped, so chop off the rest of the path
    (let [^FileInputStream fis (FileInputStream. file)
          ^String zipFilePath
           (-> file .getCanonicalPath
               (.substring
                 (-> directoryToZip .getCanonicalPath .length inc)
                 (-> file .getCanonicalPath .length)))]
    ;(println (str "Writing '" zipFilePath  "' to zip file"))
    (.putNextEntry zos (ZipEntry. zipFilePath))

    (let [bytes-0 (byte-array 1024)]
      (loop [length (.read fis bytes-0)]
        (when (>= length 0)
          (.write zos bytes-0 0 length)
          (recur (.read fis bytes-0)))))
    (.closeEntry zos)
    (.close fis)))


; Make progress bars and such
(defn writeZipFile [^File directoryToZip ^List fileList]
  (println "========")
  (println "Writing .zip to: " (str (.getAbsolutePath directoryToZip) ".zip"))
  (println "========")
  (let [^FileOutputStream fos (FileOutputStream. (str (.getName directoryToZip) ".zip"))
        ^ZipOutputStream  zos (ZipOutputStream. fos)]
        (doseq [^File file fileList]
          (when (not (.isDirectory file))  ; we only zip files, not directories
            (addToZip directoryToZip file zos)))
        (.close zos)
        (.close fos)))
  ; (File. (parse-dirs-keys [:resources "Fonts"]))
(defn zip [^File directoryToZip]
  (let [fileList (array-list)]
    (println "---Getting references to all files in:"
      (.getCanonicalPath directoryToZip))
    (getAllFiles directoryToZip fileList)
    (println "---Creating zip file")
    (writeZipFile directoryToZip fileList)
    (println "---Done")))
