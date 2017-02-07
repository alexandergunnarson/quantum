(ns quantum.core.io.core
           (:refer-clojure :exclude [get assoc! dissoc! contains?])
           (:require
             [clojure.core               :as core]
             [com.stuartsierra.component :as component]
             [datascript.core            :as mdb]
    #?(:clj  [taoensso.nippy             :as nippy])
    #?(:clj  [clojure.java.io            :as io])
    #?(:clj  [iota                       :as iota])
             [quantum.core.convert       :as conv
               :refer [->name]]
             [quantum.core.error         :as err
               :refer [->ex TODO throw-unless]]
             [quantum.core.fn            :as fn
               :refer [firsta fn-> fn$ fn1]]
             [quantum.core.log           :as log]
             [quantum.core.logic         :as logic
               :refer [splice-or whenf whenf1 whenc condpc coll-or fn-not]]
             [quantum.core.system        :as sys]
             [quantum.core.collections   :as coll
               :refer [postwalk nnil? nempty?]]
             [quantum.core.io.utils      :as u]
             [quantum.core.paths         :as p
               :refer [path]]
             [quantum.core.resources     :as res]
             [quantum.core.type          :as type
               :refer [svector?]]
             [quantum.core.vars          :as var
               :refer [defalias]]
             [quantum.core.spec          :as s
               :refer [validate]]
             [quantum.core.macros        :as macros
               :refer [defnt]])
  #?(:cljs (:require-macros
             [cljs.core.async.macros
               :refer [go]                             ]))
  #?(:clj  (:import
             (java.io File
                      InputStream OutputStream
                      DataInputStream DataOutputStream
                      FileInputStream FileOutputStream
                      FileNotFoundException))))

(defonce clj-ext (atom :cljd)) ; Clojure data

; All this isn't just IO, it's persisting, which is a smallish subset

#?(:clj (defalias contains? p/contains?))

#?(:clj
(defn- stream->assoc!
  {:todo ["Reflection"
          "Look over Java implementation — probably better"
          "Look at quantum.convert"]}
  [^String out-path ^InputStream in-stream]
  (let [^FileOutputStream out-stream
          (FileOutputStream. (File. out-path))
        ^"[B" buffer (byte-array (* 8 1024))]
    (loop [bytes-read (int (.read in-stream buffer))]
      (when (not= bytes-read -1)
        (do (.write out-stream buffer 0 bytes-read)
            (recur (.read in-stream buffer)))))

    (.close in-stream)
    (.close out-stream))))

#?(:clj
(defn bytes->assoc! [path ^"[B" data]
  (with-open [^OutputStream write-file (io/output-stream path)]
    (.write write-file data))))

(defmulti assoc-unserialized! firsta)

#?(:clj (defmethod assoc-unserialized! :str    [_ path data] (spit path data)))
#?(:clj (defmethod assoc-unserialized! :string [_ path data] (spit path data)))
#?(:clj (defmethod assoc-unserialized! :txt    [_ path data] (spit path data)))

#?(:clj
 (defmethod assoc-unserialized! :csv [_ path data]
  ;:csv  (with-open [out-file   (io/writer path-)]  ;  :append true... hmm...
      ;        (csv/write-csv out-file data))
  (TODO)))

#?(:clj
(defmethod assoc-unserialized! :binary [_ path data]
  (if (instance? InputStream data)
      (stream->assoc! path data)
      (let [^FileOutputStream out-stream
              (FileOutputStream. ^File (io/as-file path))]
        (.write out-stream ^"[B" data)
        (.close out-stream)))))

#?(:clj (defmethod assoc-unserialized! :xls  [_ path data] (bytes->assoc! path data)))
#?(:clj (defmethod assoc-unserialized! :xlsx [_ path data] (bytes->assoc! path data)))

#?(:clj
(defn assoc-serialized! ; can encrypt :encrypt-with :....
  ([path data] (assoc-serialized! path data nil))
  ([path data {:keys [compress? unfreezable-caught?]}]
    (with-open [write-file (io/output-stream path)] ; TODO more efficient way to write?
      (try (nippy/freeze-to-out!
             (DataOutputStream. write-file)
             (if compress? (nippy/freeze data) data))
        (catch Throwable e
          (if (and (-> e :type (= clojure.core.rrb_vector.rrbt.Vector)) ; Because "unfreezable type: rrbt Vector"
                   (not unfreezable-caught?))
              (->> data
                   (postwalk (whenf1 (fn1 svector?)
                               (partial core/into [])))
                   (#(assoc-serialized! path %
                       (assoc :unfreezable-caught? true))))
              (throw e))))))))

; Currently, Breeze supports IO for Matrices in two ways: Java serialization and csv.
; The latter comes from two functions: breeze.linalg.csvread and
; breeze.linalg.csvwrite

(defn assoc!
  "For CLJ and CLJS.

   In CLJS:
   Persists @x between page reloads.
   Assocs @x to @k in js/localStorage.

   Note, according to OWASP:
   It's recommended not to use js/localStorage, IndexedDB, or the Filesystem API
   (including for session identifiers) because:
   1) Any authentication your application requires can be bypassed by a
      user with local privileges to the machine on which the data is stored
   2) A single Cross Site Scripting can be used to steal all the data
      in these objects
   3) A single Cross Site Scripting can be used to load malicious data
      into these objects too, so don't consider objects in these to be trusted.

   Cookies can mitigate this risk using the |httpOnly| flag."
  {:todo ["|path| is OS-naive"
          "Technically file structures can be like nested keys in a map,
           so it would be |assoc-in!|"
          "detect file type from data"
          "(only optionally) create the directory recursively if it doesn't exist"
          "add a pluggable overwrite strategy using `next-file-copy-num`"]}
  ([opts] (assoc! (:path opts) (:data opts) opts))
  ([path data] (assoc! path data nil))
  ([path data {:keys [type method overwrite? formatter max-tries]
               :or   {method     :serialize ; can encrypt :encrypt-with :....
                      overwrite? false
                      max-tries  2}
               :as   opts}]
    (validate method #{:compress :pretty :serialize :print})
    (assert (not (and path (:path opts))))
    (assert (not (and data (:data opts))))
    #?(:cljs (js/localStorage.setItem (apply quantum.core.paths/path path) (conv/->text data))
       :clj  (let [path-f (p/parse-dir path)]
               (validate path-f string?)
               (err/try-times max-tries 500
                 (try
                   (condpc = method
                     :print  (assoc-unserialized! :string path-f data)
                     :pretty (clojure.pprint/pprint data (io/writer path-f)) ; is there a better way to do this?
                     (coll-or :serialize :compress :binary)
                       (if (or ;(= method :binary)
                               (coll/contains? #{:csv :xls :xlsx :txt :binary} type))
                           (assoc-unserialized! (keyword type) path-f data)
                           (assoc-serialized!   path-f data method))
                     (throw (->ex :illegal-arg "Unknown write method requested." method)))
                   [true]
                   (catch FileNotFoundException e
                     (u/create-dir! (-> path-f p/up-dir)) ; TODO need to do this recursively, and only as an option
                     (throw e))))))))

#_(defn dissoc!
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
          (or (-> file-path-parsed p/path->file-name (whenc empty? nil))
              (-> file-name path-without-ext (str "." extension)))
        file-path-f    (path directory-f file-name-0)
        file-f         (as-file file-path-f)
        success-alert! #(println "Successfully deleted:" file-path-f)
        fail-alert!    #(println "WARNING: Unknown IOException. Failed to delete:" file-path-f)]
    (if (exists? file-path-f)
        (try (if (io/delete-file file-f silently?)
                 (success-alert!)
                 (fail-alert!))
          (catch IOException e
            (do (println "Couldn't delete file due to an IOException. Trying to delete as directory...")
                (FileUtils/deleteDirectory file-f)
                (if (exists? file-path-f)
                    (fail-alert!)
                    (success-alert!)))))
        (println "WARNING: File does not exist. Failed to delete:" file-path-f))))

#?(:clj (def create-file! (fn-> io/as-file (.createNewFile))))

; TODO replace
#?(:clj
(defn byte-array? [x]
  (= (type x) (type (byte-array 0)))))

(defmulti get* firsta)

#?(:clj (defmethod get* :str-seq   [_ path] (iota/seq  path)))
#?(:clj (defmethod get* :str-vec   [_ path] (iota/vec  path)))
#?(:clj (defmethod get* :str       [_ path] (slurp     path))) ; because it doesn't leave open FileInputStreams  ; (->> file-path-f iota/vec (apply str))
#?(:clj (defmethod get* :load-file [_ path] (load-file path)))  ; TODO don't do this; validate it first

(defn get
  "Reads/'gets' a file from the filesystem.

   In the case of CLJS, it gets it from local storage."
  {:todo        ["|path| is naive"]
   :attribution "Alex Gunnarson"}
  ([unk] ; :string??
    (let [{:keys [type method path]
           :or   {method :unserialize} ; :uncompress is automatic
           :as opts}
          (cond (string? unk) {:path unk}
                (map?    unk) unk
                :else         (throw (->ex :unknown-argument nil unk)))]
  #?(:cljs (js/localStorage.getItem path)
     :clj  (let [^String path-f (-> path p/parse-dir (whenc empty? nil))
                 ext (keyword (or type (p/file-ext path-f)))]
             (case method
               :unserialize
                 (condpc = ext
                   (coll-or :txt :xml)
                     (iota/vec path-f) ; default is FileVec
                   (coll-or :xls :xlsx)
                     (io/input-stream path-f)
                   ;"csv"
                   ;(-> path-f io/reader csv/read-csv)
                   (whenf (with-open [read-file (io/input-stream path-f)] ; Clojure object
                          (nippy/thaw-from-in! (DataInputStream. read-file)))
                     byte-array? nippy/thaw))
               nil (slurp path-f)
               (get* method path)))))))

(defmulti persist! firsta)

#?(:cljs
(defrecord
  ^{:doc "Persists @persist-data"}
  Persister
  [persist-class persist-key persist-data opts]
  component/Lifecycle
    (start [this]
      (persist! persist-class persist-key persist-data opts))
    (stop [this]
      (mdb/unlisten! (:db persist-data) :persister))))


; From macourtney/clojure-tools
;(defn
; #^{:doc "Returns the file object if the given file is in the given directory, nil otherwise."}
;   find-file [directory file-name]
;   (when (and file-name directory (string?  file-name) (instance? File directory))
;     (let [file (File. (.getPath directory) file-name)]
;       (when (and file (.exists file))
;         file))))

; (defn
; #^{:doc "Returns the file object if the given directory is in the given parent directory, nil otherwise. Simply calls find-file, but this method reads better if you're really looking for a directory."}
;   find-directory [parent-directory directory-name]
;   (find-file parent-directory directory-name))

; (defn
; #^{:doc "A convience function for simply writing the given content into the file."}
;   write-file-content [file content]
;   (when (and file content (.exists (.getParentFile file)))
;     (spit file content)))

; (defn
; #^{:doc "Creates a new child directory under the given base-dir if the child dir does not already exist."}
;   create-dir
;   ([base-dir child-dir-name] (create-dir base-dir child-dir-name false))
;   ([base-dir child-dir-name silent]
;     (if-let [child-directory (find-directory base-dir child-dir-name)]
;       child-directory
;       (do
;         (logging/info (str "Creating " child-dir-name " directory in " (. base-dir getName) "..."))
;         (let [child-directory (new File base-dir child-dir-name)]
;           (.mkdirs child-directory)
;           child-directory)))))

; (defn
; #^{:doc "Recursively creates the given child-dirs under the given base-dir.
; For example: (create-dirs (new File \"foo\") \"bar\" \"baz\") creates the directory /foo/bar/baz
; Note: this method prints a bunch of stuff to standard out."}
;   create-dirs
;   ([dirs] (create-dirs dirs false))
;   ([dirs silent]
;     (let [base-dir (first dirs)
;           child-dirs (rest dirs)]
;       (if base-dir
;         (reduce (fn [base-dir child-dir] (create-dir base-dir child-dir silent)) base-dir child-dirs)
;         (logging/error "You must pass in a base directory.")))))

; (defn
; #^{ :doc "Creates and returns the given file if it does not already exist. If it does exist, the method simply prints to
; standard out and returns nil" }
;   create-file
;   ([file] (create-file file false))
;   ([file silent]
;     (if (. file exists)
;       (log/pr :info (str (. file getName) " already exists. Doing nothing."))
;       (do
;         (log/pr :info (str "Creating file " (. file getName) "..."))
;         (. file createNewFile)
;         file))))

; (defn
; #^{ :doc "Deletes the given directory if it contains no files or subdirectories." }
;   delete-if-empty [directory]
;   (when-not (empty? (file-seq directory))
;     (. directory delete)
;     true))

; (defn
; #^{ :doc "Deletes if empty all of the given directories in order." }
;   delete-all-if-empty [& directories]
;   (reduce (fn [deleted directory] (if deleted (delete-if-empty directory))) true directories))

; (defn
; #^{ :doc "Deletes the given directory even if it contains files or subdirectories. This function will attempt to delete
; all of the files and directories in the given directory first, before deleting the directory. If the directory cannot be
; deleted, this function aborts and returns nil. If the delete finishes successfully, then this function returns true." }
;   recursive-delete [directory]
;   (if (.isDirectory directory)
;     (when (reduce #(and %1 (recursive-delete %2)) true (.listFiles directory))
;       (.delete directory))
;     (.delete directory)))

; (defn
; #^{ :doc "Returns true if the given file is a directory. False otherwise, even if the is directory check causes an
; AccessControlException which may happen when running in Google App Engine." }
;   is-directory? [file]
;   (try
;     (.isDirectory file)
;     (catch AccessControlException access-control-exception
;       false)))


#?(:clj (defalias ->input-stream  io/input-stream ))
#?(:clj (defalias resource        io/resource     ))
#?(:clj (defalias ->output-stream io/output-stream))
#?(:clj (defalias copy!           io/copy         ))

#?(:clj (defn mkdir! [x] (.mkdir ^File (io/as-file x))))
