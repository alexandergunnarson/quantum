


;(require '[clojure.data.csv :as csv])

#?(:clj (do

(defnt ^String path->file-name
  ([^file?   f] (.getName f))
  ([^string? s] (coll/taker-until-workaround sys/separator nil s)))

(defalias file-name* path->file-name)

(defnt extension
  ([^string? s] (coll/taker-until-workaround "." nil s))
  ([^file?   f] (-> f str extension)))

(defalias file-ext extension)
(def ext-index (f*n last-index-of "."))

(defn- double-escape [^String x]
  (str/replace x "\\" "\\\\"))

(defn- ^bytes parse-bytes
  {:todo ["Belongs in |bytes| ns"]}
  [encoded-bytes]
  (->> (re-seq #"%.." encoded-bytes)
       (map+ (f*n subs 1))
       (map+ #(.byteValue ^Integer (Integer/parseInt % 16)))
       redv
       (byte-array)))

(defn url-decode
  "Decode every percent-encoded character in the given string using the
  specified encoding, or UTF-8 by default."
  {:attribution "ring.util.codec.percent-decode"}
  [encoded & [encoding]]
  (str/replace
    encoded
    #"(?:%..)+"
    (fn [chars]
      (-> ^bytes (parse-bytes chars)
          (String. ^String (or encoding "UTF-8"))
          (double-escape)))))
;___________________________________________________________________________________________________________________________________
;========================================================{ PATH, EXT MGMT }=========================================================
;========================================================{                }==========================================================
(defn+ up-dir-str [dir]
  (->> dir
       (<- whenf (extern (f*n str/ends-with? sys/separator)) popr)
       (dropr-until sys/separator)
       (<- whenc empty?
         (throw+ {:msg (str/sp "Directory does not have a parent directory:" dir)}))))

; (java.nio.file.Paths/get "asd/" (into-array ["/asdd/"]))
(defn path
  "Joins string paths (URLs, file paths, etc.)
  ensuring correct separator interposition."
  {:attribution "taoensso.encore"
   :usage '
 }
  [& parts]
  (apply str/join-once sys/separator parts))

(defn path-without-ext [path-0]
  (coll/taker-after "." path-0))

#?(:clj
(def- test-dir (path (System/getProperty "user.dir") "test")))

(def- this-dir (System/getProperty "user.dir"))

(def- root-dir
  (condp = sys/os
    :windows (-> (System/getenv) (get "SYSTEMROOT") str)
    "/"))

(def- drive-dir
  (condp = sys/os
    :windows
      (whenc (getr root-dir 0
               (whenc (index-of root-dir "\\") (fn-eq? -1) 0))
             empty?
        "C:\\") ; default drive
    sys/separator))
(def- home-dir    (System/getProperty "user.home"))

(def- desktop-dir (path home-dir "Desktop"))

(def  dirs
  (let [proj-path-0
          (or (get (System/getenv) "PROJECTS")
              (up-dir-str this-dir))
        proj-path-f 
          (ifn proj-path-0 (fn-> (index-of drive-dir) (= 0))
               path
              (partial path drive-dir))]
    {:test      test-dir
     :this-dir  this-dir
     :root      root-dir
     :drive     drive-dir
     :home      home-dir
     :desktop   desktop-dir
     :projects  proj-path-f
     :resources
       (whenc (path this-dir "resources")
              (fn-> io/as-file .exists not)
              (path proj-path-f (up-dir-str this-dir) "resources"))}))

(defnt ^String parse-dir
  ([^vector? keys-n]
    (reducei
      (fn [path-n key-n n]
        (let [first-key-not-root?
               (and (= n 0) (string? key-n)
                    ((complement (MWA 2 str/starts-with?)) key-n sys/separator))
              k-to-add-0 (or (get dirs key-n) key-n)
              k-to-add-f
                (if first-key-not-root?
                    (str sys/separator k-to-add-0)
                    k-to-add-0)]
          (path path-n k-to-add-f)))
      "" keys-n))
  ([^string?  s] s)
  ([^keyword? k] (parse-dir [k]))
  ([          obj] (if (nil? obj) "" (throw+ (Err. :unimplemented nil {:obj obj :class (class obj)})))))

(defnt ^java.io.File as-file
  ([^vector? dir] (-> dir parse-dir as-file))
  ([^string? dir] (-> dir io/file))
  ([^file?   dir] dir))

(def file-str (fn-> as-file str))

(defnt exists?
  ([^string? s] (-> s as-file exists?))
  ([^file?   f] (.exists f)))

(defnt directory?
  ([^string? s] (-> s as-file directory?))
  ([^file?   f] (and (exists? f) (.isDirectory f))))

(defalias folder? directory?)

(ns-unmap 'quantum.core.io.core 'file?)
(def file? (fn-not directory?))

(def clj-extensions #{:clj :cljs :cljc})

(def clj-file?
  (fn-and file?
    (fn->> extension keyword (containsv? clj-extensions))))

; FILE RELATIONS

(defnt ^String up-dir
  ([^string? dir ] (-> dir up-dir-str))
  ([^vec?    dir ] (-> dir parse-dir up-dir))
  ([^file?   file] (.getParent file)))

(defn parent [f]
  (.getParentFile ^File (as-file f)))

(defn children [f]
  (let [^File file (as-file f)]
    (when (directory? file)
      (->> file (.listFiles) seq))))

(defn siblings [f]
  (let [^File file (as-file f)]
    (->> file parent children
         (remove (fn-> str (= (str file)))))))

(def directory-? (mfn 1 directory?))
(def descendants       (fn->> as-file file-seq))
(def descendant-leaves (fn->> as-file file-seq (remove directory-?)))
(def internal-nodes    (fn->> as-file file-seq (filter directory-?)))
(def descendant-dirs internal-nodes)
;___________________________________________________________________________________________________________________________________
;========================================================{ FILES AND I/O  }=========================================================
;========================================================{                }==========================================================
(defnt readable?
  ([^string? dir]
    (try (->> dir (.checkRead (SecurityManager.)))
         true
      (catch SecurityException _ false)))
  ([^file?   dir] (->> dir str       readable?))
  ([^vec?    dir] (->> dir parse-dir readable?)))

(defnt writable?
  ([^string? dir]
    (try (->> dir (.checkWrite (SecurityManager.)))
         true
      (catch SecurityException _ false)))
  ([^file?   dir] (->> dir str writable?)))

(defalias input-stream  io/input-stream )
(defalias resource      io/resource     )
(defalias output-stream io/output-stream)
(defalias copy!         io/copy         )

(defn create-dir! [dir-0]
  (let [dir   (-> dir-0 parse-dir)
        ^File dir-f (as-file dir)]
    (if (exists? dir)
        (println "Directory already exists:" dir)
        (try (writable? dir-f)
             (assert (.mkdir dir-f) true)
             (println "Directory created:" dir)
          (catch SecurityException e (println "The directory" (str/squote dir)
                                       "could not be created. A security exception occurred."))
          (catch AssertionError    e (println "The directory" (str/squote dir)
                                       "could not be created. Possibly administrator permissions are required."))))))

(defn num-to-sortable-str [num-0]
  (ifn num-0 (fn-and num/nneg? (f*n < 10))
       (partial str "0")
       str))
(defn next-file-copy-num [path-0]
  (let [extension (file-ext path-0)
        file-name (-> path-0 path->file-name)]
    (try
      (->> path-0
           siblings
           (map+ str)
           (filter+
             (partial
               (fn-and
                 (fn-> file-name* (str/starts-with? file-name))
                 (fn-> file-ext (= extension)))))
           (map+ (fn-> file-name*
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
                  (FileOutputStream. ^File (as-file path-))]
            (.write out-stream data) ; REFLECTION error
            (.close out-stream)))
    ;:csv  (with-open [out-file   (io/writer path-)]  ;  :append true... hmm...
    ;        (csv/write-csv out-file data))
    :xls  (with-open [write-file (output-stream path-)]
            (.write data write-file))
    :xlsx (with-open [write-file (output-stream path-)]
            (.write data write-file))))

(defn write-serialized! ; can encrypt :encrypt-with :....
  [data path-0 write-method & [unfreezable-caught?]]
  (with-open [write-file (output-stream path-0)]
    (let [data-f (whenf data vector+? ; Because "unfreezable type: rrbt Vector"
                   (partial core/into []))]
      (try+ (nippy/freeze-to-out!
              (DataOutputStream. write-file)
              (case write-method
                :serialize data-f
                :compress  (nippy/freeze data-f)))
            (catch Object e
              (if (and (-> e :type (= clojure.core.rrb_vector.rrbt.Vector))
                       (not unfreezable-caught?))
                  (->> data
                       (postwalk (whenf*n vector+?
                                   (partial core/into []))) 
                       (<- write-serialized! path-0 write-method true))
                  (throw+))
              ))))); byte-code

(def- ill-chars-table
  {"\\" "-", "/" "-", ":" "-", "*" "!", "?" "!"
   "\"" "'", "<" "-", ">" "-", "|" "-"})

(defn conv-ill-chars [str-0] ; Make less naive - Mac vs. Windows, etc.
  (reduce-kv
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
            #?@(:clj [:pretty (pprint data-formatted (io/writer file-path-f))]) ; is there a better way to do this?)

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
(defn+ write! ; can have list of file-types ; should detect file type from data... ; create the directory if it doesn't exist
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
  (doseq [file-type-n (coll/coll-if file-type)]
    (let [file-path-parsed (parse-dir file-path)
          directory-parsed (parse-dir directory)
          directory-f
            (or (-> file-path-parsed up-dir   (whenc empty? nil))
                directory-parsed)
          extension
            (or (-> file-path-parsed file-ext (whenc empty? nil))
                (file-ext file-name)
                file-type)
          file-name-0
            (or (-> file-path-parsed file-name* conv-ill-chars (whenc empty? nil))
                (-> file-name conv-ill-chars path-without-ext (str "." extension)))
          file-name-00
            (or (-> file-path-parsed file-name* conv-ill-chars (whenc empty? nil)
                    (whenf nnil? (fn-> path-without-ext (str " 00." extension))))
                (-> file-name conv-ill-chars path-without-ext (str " 00." extension)))
          file-path-0  (path directory-f file-name-0)
          date-spaced
            (when (and (= overwrite :date) (exists? file-path-0))
              (str " " (time/now-formatted "MM-dd-yyyy HH|mm")))
          file-num
            (cond
              (and (splice-or overwrite = :num :num-0)
                   (some (extern (mfn 1 exists?)) [file-path-0 (path directory-f file-name-00)]))
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
  (let [file-path-parsed (-> file-path parse-dir)
        directory-parsed (-> directory parse-dir)
        directory-f
          (or (-> file-path-parsed up-dir   (whenc empty? nil))
              directory-parsed)
        extension
          (or (-> file-path-parsed file-ext (whenc empty? nil))
              (file-ext file-name))
        file-name-0
          (or (-> file-path-parsed file-name* (whenc empty? nil))
              (-> file-name path-without-ext (str "." extension)))
        file-path-f    (path directory-f file-name-0)
        file-f         (as-file file-path-f)
        success-alert! #(println "Successfully deleted:" file-path-f)
        fail-alert!    #(println "WARNING: Unknown IOException. Failed to delete:" file-path-f)]
    (if (exists? file-path-f)
        (try+ (if (io/delete-file file-f silently?)
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
  (let [^String directory-f (-> directory parse-dir)
        ^String file-path-f
          (or (-> file-path parse-dir (whenc empty? nil))
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
            (input-stream file-path-f)
          ;"csv"
          ;(-> file-path-f io/reader csv/read-csv)
          (whenf (with-open [read-file (input-stream file-path-f)] ; Clojure object
                 (nippy/thaw-from-in! (DataInputStream. read-file)))
            byte-array? nippy/thaw))
      (println "Unknown read method requested."))))

(defn create-temp-file!
  [^String file-name ^String suffix]
  (java.io.File/createTempFile file-name suffix))

(defmacro with-temp-file
  {:attribution "From github.com/bevuta/pepa.util"}
  [[name data suffix] & body]
  `(let [data# ~data
         ~name (create-temp-file! "pepa" (or ~suffix ""))]
     (try
       (when data#
         (io/copy data# ~name))
       ~@body
       (finally
         (.delete ~name)))))

; Placed here to avoid shadowing within this ns
(defalias file       as-file)
(defalias file-name file-name*)

(def create-file! (fn-> as-file (.createNewFile)))

))
