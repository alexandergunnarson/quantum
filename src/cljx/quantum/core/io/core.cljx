(ns
  ^{:doc "I/O operations. Path parsing, read/write, serialization, etc.

          Perhaps it would be better to use, say, org.apache.commons.io.FileUtils
          for many of these things."}
  quantum.core.io.core
  (:refer-clojure :exclude [read])
  (:require
    [quantum.core.ns               :as ns
      #+clj :refer #+clj [defalias alias-ns]                          ]
    [quantum.core.data.array       :as arr  :refer :all               ]
    [quantum.core.error            :as err  :refer :all               ]
    [quantum.core.string           :as str                            ]
    [quantum.core.time.core        :as time                           ]
    [quantum.core.print            :as pr   :refer [! pprint]         ]
    [quantum.core.collections      :as coll :refer :all               ]
    [quantum.core.numeric          :as num  :refer [greatest-or]      ]
    [quantum.core.logic                     :refer :all               ]
    [quantum.core.type                      :refer :all               ]
    [quantum.core.function                  :refer :all               ]
    [quantum.core.system           :as sys                            ]
    [quantum.core.error            :as err
      #+clj :refer #+clj [try+ throw+]                                ]
    [clojure.java.io               :as clj-io                         ]
    [clojure.data.csv              :as csv                            ]
    [taoensso.nippy                :as nippy                          ]
    [quantum.core.io.serialization :as io-ser                         ]
    [iota                          :as iota                           ])
  #+clj
  (:import
    (java.io File FileNotFoundException PushbackReader
      FileReader DataInputStream DataOutputStream IOException
      OutputStream FileOutputStream BufferedOutputStream BufferedInputStream
      InputStream  FileInputStream
      PrintWriter)
    (java.util.zip ZipOutputStream ZipEntry)
    java.util.List
    org.apache.commons.io.FileUtils)
  #+clj (:gen-class))

; This |do| covers the entire file. For purposes of reader macro
#+clj
(do

(ns/require-all *ns* :clj)
(ns/nss *ns*)

(defn exists? [^String path-0] (.exists ^File (clj-io/as-file path-0)))

(defn- double-escape [^String x]
  (.replace x "\\" "\\\\"))
(defn- ^bytes parse-bytes [encoded-bytes]
  (->> (re-seq #"%.." encoded-bytes)
       (map+ (f*n subs 1))
       (map+ #(.byteValue ^Integer (Integer/parseInt % 16)))
       redv
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
  {:pre [(string? dir)]}
  (try
    (let [sep-f (or separator *os-separator*)
        dir-f (whenf dir (compr last+ (eq? sep-f)) popr+)]
      (str/subs+ dir-f 0 (inc (last-index-of+ dir-f sep-f))))
    (catch java.lang.StringIndexOutOfBoundsException _ "")))

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
(def ^:private test-dir
  (try+
    (-> (clj-io/resource "") url-decode
        (str/replace #"^file:/" "/")
        (str/replace #"/" *os-sep-esc*))
    (catch Exception _ ""))) ; To handle a weird "MapEntry cannot be cast to Number" error)
(def- this-dir
  (up-dir test-dir))
(def- root-dir
  (condp = sys/*os*
    :windows (-> (System/getenv) (get "SYSTEMROOT") str)
    "/"))
(def- drive-dir
  (condp = sys/*os*
    :windows
      (whenc (getr+ root-dir 0
               (whenc (index-of+ root-dir "\\") (eq? -1) 0))
             empty?
        "C:\\") ; default drive
    "/"))
(def- home-dir
  (System/getProperty "user.home"))
(def- desktop-dir
  (path home-dir "Desktop"))
(def  dirs
  (let [proj-path-0
          (whenc (get (System/getenv) "PROJECTS") nil?
            (up-dir this-dir))
        proj-path-f 
          (if (= 0 (index-of+ proj-path-0 drive-dir))
              (path proj-path-0)
              (path drive-dir proj-path-0))]
    {:test      test-dir
     :this-dir  this-dir
     :root      root-dir
     :drive     drive-dir
     :home      home-dir
     :desktop   desktop-dir
     :projects  proj-path-f
     :resources
       (whenc (path this-dir "resources")
              (fn-not exists?)
              (path proj-path-f (up-dir this-dir) "resources"))}))
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
(defn file [arg]
  (condf arg
    vector? (fn-> parse-dirs-keys clj-io/file)
    string? clj-io/file))
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
           redv num/greatest inc num-to-sortable-str)
      (catch Exception _ (num-to-sortable-str 1)))))


(defn- write-from-stream!
  {:todo ["Reflection"]}
  [^InputStream in-stream ^String out-path]
  (let [^FileOutputStream out-stream
          (FileOutputStream. (File. out-path))
        ^"[B" buffer (byte-array (* 8 1024))]
    (loop [bytesRead (int (.read in-stream buffer))]
      (when (not= bytesRead -1)
        (do (.write out-stream buffer 0 bytesRead)
            (recur (.read in-stream buffer)))))

    (.close in-stream)
    (.close out-stream)))

(defn write-unserialized!
  {:todo ["Decomplicate"]}
  [data ^String path- & {:keys [type] :or {type :string}}]
  {:pre [(with-throw
           (or (and (instance? InputStream data)
                    (= type :binary))
               (not (instance? InputStream data)))
           (str/sp "InputStream canot be written to output type:" type))]}
  (condpc = type
    (coll-or :str :string :txt) 
      (spit path- data)
    :binary
      (if (instance? InputStream data)
          (write-from-stream! data path-)
          (let [^FileOutputStream out-stream
                  (FileOutputStream. ^File (file path-))]
            (.write out-stream data) ; REFLECTION error
            (.close out-stream)))
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
            (coll-or :serialize :compress :binary)
              (if (or ;(= write-method :binary)
                      (splice-or file-type = "csv" "xls" "xlsx" "txt" :binary))
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
  {:todo ["Apparently has problems with using the :directory key"
          "Decomplicate"]}
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
                     (when (nempty? extension)
                       (str "." extension))))
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

(defn delete!
  {:todo ["Implement recycle bin functionality for Windows" "Decomplicate"]}
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
  {:todo        ["Decomplicate"]
   :attribution "Alex Gunnarson"}
  [& {file-name :name file-path :path
      :keys [directory file-type read-method class-import]
      :or   {directory   :resources
             read-method :unserialize} ; :uncompress is automatic
      :as options}] ; :string??
  (when (fn? class-import)
    (class-import))
  (let [^String directory-f (-> directory coll-if parse-dirs-keys)
        ^String file-path-f
          (or (-> file-path coll-if parse-dirs-keys (whenc empty? nil))
              (path directory-f file-name))
        extension (or file-type (file-ext file-path-f))]
    (condpc = read-method
      :load-file (load-file file-path-f) ; don't do this; validate it first
      :str-seq   (iota/seq file-path-f)
      :str-vec   (iota/vec file-path-f)
      :str       (slurp file-path-f) ; because it doesn't leave open FileInputStreams  ; (->> file-path-f iota/vec (apply str))
      :unserialize
        (condpc = extension
          (coll-or "txt" "xml")
          (iota/vec file-path-f) ; default is FileVec
          "xlsx"
          (clj-io/input-stream file-path-f)
          "csv"
          (-> file-path-f clj-io/reader csv/read-csv)
          (whenf (with-open [read-file (clj-io/input-stream file-path-f)] ; Clojure object
                 (nippy/thaw-from-in! (DataInputStream. read-file)))
            byte-array? nippy/thaw))
      (println "Unknown read method requested."))))) ; byte-code