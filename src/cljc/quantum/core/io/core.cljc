(ns quantum.core.io.core
  (:refer-clojure :exclude [get assoc! dissoc!])
  (:require-quantum [:core logic fn core-async log err res vec str])
  (:require [com.stuartsierra.component  :as component]
            [datascript.core             :as mdb      ]
   #?(:clj  [taoensso.nippy              :as nippy    ])
   #?(:clj  [iota                        :as iota     ])
            [quantum.core.convert        :as conv
              :refer [->name ->str]                   ]
            [quantum.db.datomic          :as db      
              #?@(:cljs [:refer [EphemeralDatabase]]) ]
            [quantum.core.system         :as sys      ]
            [clojure.walk :refer [postwalk]]
            [quantum.core.io.utils       :as u        ]
            [quantum.core.paths          :as p        
              :refer [path]]
    #?(:clj [clojure.java.io             :as io       ]))
  #?(:clj
  (:import (quantum.db.datomic EphemeralDatabase)
           (java.io File
                    InputStream OutputStream
                    DataInputStream DataOutputStream
                    FileInputStream FileOutputStream
                    FileNotFoundException))))

(defonce clj-ext (atom :cljd)) ; Clojure data

; All this isn't just IO, it's persisting, which is a smallish subset

; TODO replace
; Similar to |contains?|
#?(:clj (defn exists? [x] (.exists ^File x)))

#?(:clj
(defn- stream->assoc!
  {:todo ["Reflection"
          "Look over Java implementation — probably better"]}
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
(defn assoc-unserialized!
  {:todo ["Decomplicate"]}
  ([^String path- data] (assoc-unserialized! path- data nil))
  ([^String path- data {:keys [type] :or {type :string}}]
    (throw-unless
      (or (and (instance? InputStream data)
               (= type :binary))
          (not (instance? InputStream data)))
      (->ex :err/io "InputStream cannot be written to output type" type))
    (condpc = type
      (coll-or :str :string :txt) 
        (spit path- data)
      :binary
        (if (instance? InputStream data)
            (stream->assoc! path- data)
            (let [^FileOutputStream out-stream
                    (FileOutputStream. ^File (io/as-file path-))]
              (.write out-stream data) ; REFLECTION warning
              (.close out-stream)))
      ;:csv  (with-open [out-file   (io/writer path-)]  ;  :append true... hmm...
      ;        (csv/write-csv out-file data))
      (coll-or :xls :xlsx)
        (with-open [write-file (io/output-stream path-)]
          (.write data write-file))))))

#?(:clj 
(defn assoc-serialized! ; can encrypt :encrypt-with :....
  ([path-0 data] (assoc-serialized! path-0 data nil))
  ([path-0 data {:keys [compress?
                        unfreezable-caught?]}]
    (with-open [write-file (io/output-stream path-0)]
      (let [data-f (whenf data vector+? ; Because "unfreezable type: rrbt Vector"
                     (partial core/into []))]
        (try (nippy/freeze-to-out!
               (DataOutputStream. write-file)
               (if compress?
                   (nippy/freeze data-f)
                   data-f))
          (catch Throwable e
            (if (and (-> e :type (= clojure.core.rrb_vector.rrbt.Vector))
                     (not unfreezable-caught?))
                (->> data
                     (postwalk (whenf*n vector+?
                                 (partial core/into []))) 
                     #((assoc-serialized! path-0 %
                         (assoc :unfreezable-caught? true))))
                (throw e)))))))))

#?(:clj
(defn try-assoc!
  {:todo ["rewrite"]}
  [n successful? file-name-f directory-f
   file-path-f method data-formatted file-type]
  (cond successful? (log/pr " complete.\n")
        (> n 2)     (log/pr "Maximum tries exceeded.")
        :else
        (try
          (log/pr :debug "Writing" file-name-f "to" directory-f (str "(try " n ")..."))
          (condpc = method
            :print  (assoc-unserialized! file-path-f data-formatted {:type :string})
            :pretty (clojure.pprint/pprint data-formatted (io/writer file-path-f)) ; is there a better way to do this?

            (coll-or :serialize :compress :binary)
              (if (or ;(= method :binary)
                      (splice-or file-type = "csv" "xls" "xlsx" "txt" :binary))
                  (assoc-unserialized! file-path-f data-formatted {:type (keyword file-type)})
                  (assoc-serialized!   file-path-f data-formatted method))
            (throw (->ex :illegal-arg "Unknown method requested." method)))
          #(try-assoc! (inc n) true file-name-f directory-f
             file-path-f method data-formatted file-type)
          (catch FileNotFoundException _
            (u/create-dir! directory-f)
            #(try-assoc! (inc n) false file-name-f directory-f
               file-path-f method data-formatted file-type))))))

#?(:clj
(defn assoc!- ; can have list of file-types ; should detect file type from data... ; create the directory if it doesn't exist
  "@file-types : Should be a vector of keywords"
  {:todo ["Apparently has problems with using the :directory key"
          "Decomplicate"
          "Rewrite"
          "|time/now-formatted| needs to be not crossed out"
          "|next-file-copy-num| needs to be not crossed out"]}
  ([opts] (assoc!- nil (:data opts) opts))
  ([path- data] (assoc!- path- data nil))
  ([path- data- {file-name :name file-path :path
                :keys [data directory file-type file-types
                       method overwrite formatting-func]
                :or   {directory       :resources
                       file-name       "Untitled"
                       file-types      [:clj]
                       method          :serialize ; :compress ; can encrypt :encrypt-with :.... ; :method :pretty
                       overwrite       true  ; :date, :num :num-0
                       formatting-func identity}
                :as   options}]
  (doseq [file-type-n file-types]
    (let [data data-
          file-path (p/parse-dir path-)
          _ (assert (string? file-path))
          file-path-parsed file-path
          directory-parsed (p/parse-dir directory)
          directory-f
            (or (-> file-path-parsed p/up-dir   (whenc empty? nil))
                directory-parsed)
          extension
            (or (-> file-path-parsed p/file-ext (whenc empty? nil))
                (p/file-ext file-name)
                file-type)
          file-name-0
            (or (-> file-path-parsed p/path->file-name u/escape-illegal-chars (whenc empty? nil))
                (-> file-name u/escape-illegal-chars p/path-without-ext (str "." extension)))
          file-name-00
            (or (-> file-path-parsed p/path->file-name u/escape-illegal-chars (whenc empty? nil)
                    (whenf nnil? (fn-> p/path-without-ext (str " 00." extension))))
                (-> file-name u/escape-illegal-chars p/path-without-ext (str " 00." extension)))
          file-path-0  (path directory-f file-name-0)
          date-spaced
            (when (and (= overwrite :date) (exists? file-path-0))
              (str " " (identity #_time/now-formatted "MM-dd-yyyy HH|mm")))
          file-num
            (cond
              (and (splice-or overwrite = :num :num-0)
                   (some #(p/exists? %1) [file-path-0 (path directory-f file-name-00)]))
              "00_FIX" #_(next-file-copy-num file-path-0)
              (and (= overwrite :num-0) ((fn-not exists?) file-path-0))
              "00"
              :else nil)
          file-name-f
            (-> file-name-0 p/path-without-ext
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
            (log/pr :debug file-num)
            (log/pr :debug file-name-f)
            (log/pr :debug directory-f)
      (trampoline try-assoc! 1 false
        file-name-f directory-f file-path-f method data-formatted file-type))))))

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

#?(:clj (def create-file! (fn-> io/as-file (.createNewFile))))

(defn assoc!
  "Persists @x between page reloads.
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
  {:todo ["|path| is naive"
          "Technically file structures can be like nested keys in a map,
           so it would be |assoc-in!|"]}
  [k x & [opts]]
  #?(:clj  (apply assoc!- k x opts)
     :cljs (js/localStorage.setItem (apply path k) (->str x))))

; TODO replace
#?(:clj
(defn byte-array? [x]
  (= (type x) (type (byte-array 0)))))

(defn get
  "Reads/'gets' a file from the filesystem.

   In the case of CLJS, it gets it from local storage."
  {:todo        ["Decomplicate" "Rewrite" "|path| is naive"]
   :attribution "Alex Gunnarson"}
  ([unk] (cond (string? unk)
               (get nil {:path unk})
               (map? unk)
               (get nil unk)
               :else (throw (->ex :unknown-argument nil unk))))
  ([_ {file-name :name file-path :path
      :keys [directory file-type method]
      :or   {directory   :resources
             method :unserialize} ; :uncompress is automatic
      :as options}] ; :string??
  #?(:cljs (js/localStorage.getItem file-path)
     :clj  (let [^String directory-f (-> directory p/parse-dir)
                 ^String file-path-f
                   (or (-> file-path p/parse-dir (whenc empty? nil))
                       (path directory-f file-name))
                 extension (keyword (or file-type (p/file-ext file-path-f)))]
             (condpc = method
               :str-seq   (iota/seq  file-path-f)
               :str-vec   (iota/vec  file-path-f)
               :str       (slurp     file-path-f) ; because it doesn't leave open FileInputStreams  ; (->> file-path-f iota/vec (apply str))
               :load-file (load-file file-path-f) ; don't do this; validate it first
               :unserialize
                 (condpc = extension
                   (coll-or :txt :xml)
                     (iota/vec file-path-f) ; default is FileVec
                   (coll-or :xls :xlsx)
                     (io/input-stream file-path-f)
                   ;"csv"
                   ;(-> file-path-f io/reader csv/read-csv)
                   (whenf (with-open [read-file (io/input-stream file-path-f)] ; Clojure object
                          (nippy/thaw-from-in! (DataInputStream. read-file)))
                     byte-array? nippy/thaw))
               (println "Unknown read method requested."))))))

#?(:cljs
(defrecord
  ^{:doc "Persists @persist-data"}
  Persister
  [persist-key persist-class persist-data opts]
  component/Lifecycle
    (start [this]
      ; TODO multimethod
      (condp = persist-class
        EphemeralDatabase
          (let [{:keys [schema]} opts
                {:keys [db history]} persist-data]
            #?(:cljs
            (when (-> db meta :listeners (core/get persist-key))
              (throw (->ex :duplicate-persisters
                           "Cannot have multiple ClojureScript Persisters for DataScript database"))))
            ; restoring once persisted DB on page load
            (or (when-let [stored (get (->name persist-key))]
                  (let [stored-db (conv/->mdb stored)]
                    (when (= (:schema stored-db) schema) ; check for code update
                      (reset! db stored-db)
                      (swap! history conj @db)
                      true)))
                ; (mdb/transact! conn schema)
                )
            (mdb/listen! db :persister
              (fn [tx-report] ; TODO do not notify with nil as db-report
                              ; TODO do not notify if tx-data is empty
                (when-let [db (:db-after tx-report)]
                  (go (assoc! persist-key db))))))
        (throw (->ex :unhandled-class
                       "Class not handled for persistence."
                       persist-class))))
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
;       (logging/info (str (. file getName) " already exists. Doing nothing."))
;       (do
;         (logging/info (str "Creating file " (. file getName) "..."))
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