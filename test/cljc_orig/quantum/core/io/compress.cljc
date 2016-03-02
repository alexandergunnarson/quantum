(ns
  ^{:attribution "Alex Gunnarson"
    :doc
      "Compression."
    :todo ["Extend functionality to all compression formats: .zip, .gzip, .tar, .rar, etc."]}
  quantum.core.io.compress
  (:require-quantum [ns arr err str time coll num logic type fn])
  #?(:clj
      (:require
        [clojure.java.io               :as clj-io                   ]
        [quantum.core.convert          :as convert]
        [taoensso.nippy                :as nippy                    ]
        [quantum.core.io.serialization :as io-ser                   ]
        [iota                          :as iota                     ]
        [byte-transforms               :as bt                       ]))
  #?(:clj
      (:import
        (net.jpountz.lz4 LZ4Factory LZ4Compressor)
        (java.io File FileNotFoundException PushbackReader
          FileReader DataInputStream DataOutputStream IOException
          OutputStream FileOutputStream BufferedOutputStream BufferedInputStream
          InputStream  FileInputStream
          PrintWriter)
        (java.util.zip ZipOutputStream ZipEntry ZipFile GZIPInputStream)
        #_(org.apache.commons.compress.archivers.tar
          TarArchiveInputStream TarArchiveEntry)
        #_(org.apache.commons.compress.compressors
          bzip2.BZip2CompressorInputStream xz.XZCompressorInputStream)
        java.util.List
        org.apache.commons.io.FileUtils
        (java.nio.charset Charset CharsetEncoder CharacterCodingException)
        (java.nio CharBuffer ByteBuffer)
        (quanta Packed12 ClassIntrospector))))

(defrecord CompressionCodec
   [name    extension algorithm speed      compression implemented? doc                                                        ])

#?(:clj
(def raw-compressors-table
  [[:gzip   :gz       :gzip     nil        nil         true         nil                                                        ]
   [:bzip2  :bz2      nil       nil        nil         true         nil                                                        ]
   [:zip    :zip      nil       nil        nil         false        ["java.util.* implementation is fastest, supposedly."      ]]
   [:7zip   :7z       nil       nil        nil         false        nil                                                        ]
   [:tar    :tar      nil       nil        nil         false        nil                                                        ]
   [:rar    :tar      nil       nil        nil         false        nil                                                        ]
   ; Bytes size    Sec comp Sec de.    Version          Command Line Args
   ; 2720359988    43888*   45359*  1  zpaq 6.41        -m 611 -th 1
   ; 3594933877    10003      519   1  7zip 4.47b       -mx
   ; 3701584921      187*      67*  1  zpaq 6.40        -m 2 -
   [:zpaq   nil       nil       nil        :highest    false        ["http://mattmahoney.net/dc/10gb.html 10 GB compressed."   ]]
   [:snappy :snappy   :snappy   :very-high :medium     true         nil                                                        ]
   [:lz4    :lz4      :lz4      :highest   :high       true         ["by far the fastest: https://github.com/jpountz/lz4-java" ]]]))

(def- compressors-set
  (->> raw-compressors-table
       (map (fn [args] (apply construct CompressionCodec args)))))

; TODO make a |key-by| macro for all this
(def- supported-compressors
  (->> compressors-set (filter :implemented?) (into #{})))

(def supported-extensions
  (->> supported-compressors (map :extension) (into #{})))

(def supported-formats
  (->> supported-compressors (map :name) (into #{})))

(def supported-algorithms 
  (set/union (->> supported-compressors (map :algorithm) (into #{}))
    (into #{} bt/available-compressors)))

(def supported-preferences
  #{:fastest :smallest
    :speed :size})

(defn ^"[B" compress
  ([data] (compress data {:format :lz4}))
  ([data {:keys [format prefer] :as options}]
    (throw-when (and format prefer)
      "Cannot prefer and choose a format.")
    (throw-when (and prefer (not (in-k? prefer supported-preferences)))
      "Preference not recognized.")
    (throw-when (and format (not (in-k? format supported-algorithms)))
      "Format not supported.")
    (let [format-f
           (if format format
               (condpc = prefer
                 (coll-or :fastest :speed) :lz4
                 (coll-or :smallest :size) :zpaq))]
      (-> data convert/->bytes (bt/compress format-f options)))))

#?(:clj
(defn decompress 
  {:todo ["Do automatically by type"]}
  ([x]           (decompress x :lz4))
  ([x algorithm] (decompress x algorithm nil))
  ([x algorithm options]
    ; His lz4 is net.jpountz.lz4, which is the best
    (bt/decompress x algorithm options)))

; TODO THIS IS FROM RAYNES... GOOD STUFF WORTH INCORPORATING

; (defn make-zip-stream
;   "Create zip file(s) stream. You must provide a vector of the
;   following form: 
;   ```[[filename1 content1][filename2 content2]...]```.
;   You can provide either strings or byte-arrays as content.
;   The piped streams are used to create content on the fly, which means
;   this can be used to make compressed files without even writing them
;   to disk."
;   {:source "me.raynes/fs"}
;   [& filename-content-pairs]
;   (let [file
;         (let [pipe-in (java.io.PipedInputStream.)
;               pipe-out (java.io.PipedOutputStream. pipe-in)]
;           (future
;             (with-open [zip (java.util.zip.ZipOutputStream. pipe-out)]
;               (add-zip-entry zip (flatten filename-content-pairs))))
;           pipe-in)]
;     (io/input-stream file)))

; (defn zip
;   "Create zip file(s) on the fly. You must provide a vector of the
;   following form: 
;   ```[[filename1 content1][filename2 content2]...]```.
;   You can provide either strings or byte-arrays as content."
;   {:source "me.raynes/fs"}
;   [filename & filename-content-pairs]
;   (io/copy (make-zip-stream filename-content-pairs)
;            (fs/file filename)))

; (defn- tar-entries
;   "Get a lazy-seq of entries in a tarfile."
;   {:source "me.raynes/fs"}
;   [^TarArchiveInputStream tin]
;   (when-let [entry (.getNextTarEntry tin)]
;     (cons entry (lazy-seq (tar-entries tin)))))

; (defn untar
;   "Takes a tarfile `source` and untars it to `target`."
;   {:source "me.raynes/fs"}
;   ([source] (untar source (name source)))
;   ([source target]
;      (with-open [tin (TarArchiveInputStream. (io/input-stream (fs/file source)))]
;        (doseq [^TarArchiveEntry entry (tar-entries tin) :when (not (.isDirectory entry))
;                :let [output-file (fs/file target (.getName entry))]]
;          (fs/mkdirs (fs/parent output-file))
;          (io/copy tin output-file)))))

; (defn gunzip
;   "Takes a path to a gzip file `source` and unzips it."
;   {:source "me.raynes/fs"}
;   ([source] (gunzip source (name source)))
;   ([source target]
;      (io/copy (-> source fs/file io/input-stream GZIPInputStream.)
;               (fs/file target))))

; (defn bunzip2
;   "Takes a path to a bzip2 file `source` and uncompresses it."
;   {:source "me.raynes/fs"}
;   ([source] (bunzip2 source (name source)))
;   ([source target]
;      (io/copy (-> source fs/file io/input-stream BZip2CompressorInputStream.)
;               (fs/file target))))

; (defn unxz
;   "Takes a path to a xz file `source` and uncompresses it."
;   {:source "me.raynes/fs"}
;   ([source] (unxz source (name source)))
;   ([source target]
;     (io/copy (-> source fs/file io/input-stream XZCompressorInputStream.)
;              (fs/file target))))



; ; Compressing strings
; ; http://java-performance.info/string-packing-converting-characters-to-bytes/
; ; String, no compression 722.48 Mb
; ; String, -XX:+UseCompressedStrings 645.47 Mb 
; ; packed strings 268.46 Mb

; (def ^Charset US_ASCII (Charset/forName "US-ASCII"))

; (defn convert
;   "Optimizing a string object in terms of memory"
;   {:source "http://java-performance.info/string-packing-converting-characters-to-bytes/"}
;   [^String s] 
;   ; discard empty or too long strings as well as sings with '\0'
;   (if (or (nil? s) (empty? s) (-> s count (> 12))
;           (not= (.indexOf s (str (char 0))) -1))
;       s
;       ; encoder may be stored in ThreadLocal
;       (let [^CharsetEncoder enc (.newEncoder US_ASCII)
;             ^CharBuffer charBuffer (CharBuffer/wrap s)]
;         (try
;           (let [^ByteBuffer byteBuffer (.encode enc charBuffer )
;                 ^bytes      byteArray  (.array byteBuffer)]
;             (if (<= (count byteArray) 12)
;                 (Packed12. ^"[B" byteArray)
;                 ; add cases for longer strings here
;                 s))
;         (catch CharacterCodingException e
;           ; there are some chars not fitting to our encoding
;           s)))))
;  