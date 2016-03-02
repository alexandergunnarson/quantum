(ns ^{:doc "The Rosetta Stone."}
  quantum.core.convert ; perhaps coerce?
  (:require-quantum [ns fn logic num macros type io log str coll arr pconvert])
  (:require
    [byte-streams :as streams]
    ; CompilerException java.lang.NoClassDefFoundError: IllegalName: compile__stub.gloss.data.bytes.core.gloss.data.bytes.core/MultiBufferSequence, compiling:(gloss/data/bytes/core.clj:78:1) 
    ; [gloss.core.formats         :as gformats]
    [manifold.stream              :as s    ]
    [manifold.deferred            :as d    ]
    [byte-streams.graph           :as g    ]
    [byte-streams.protocols       :as proto]
    [byte-streams.pushback-stream :as ps   ]
    [byte-streams.char-sequence   :as cs   ])
#?(:clj
  (:import
    [quantum.core.data.streams    ByteBufferInputStream]
    [byte_streams.graph          Type]
    [java.lang.reflect           Array]
    [java.util.concurrent.atomic AtomicBoolean]
    [java.net                    URI URL InetAddress]
    ; http://java-performance.info/java-io-bytearrayoutputstream/ says:
    ; Do not use ByteArrayOutputStream in performance critical code — it's synchronized
    ; For performance critical code try to use ByteBuffer instead of ByteArrayOutputStream.
    [java.io                     File
                                 FileOutputStream FileInputStream
                                 ByteArrayInputStream ByteArrayOutputStream
                                 PipedOutputStream PipedInputStream
                                 DataInputStream
                                 InputStream OutputStream
                                 IOException
                                 RandomAccessFile
                                 Reader BufferedReader InputStreamReader]
    [java.nio                    ByteBuffer DirectByteBuffer CharBuffer]
    [java.nio.charset            Charset]
    [java.nio.channels           Channels
                                 ReadableByteChannel WritableByteChannel
                                 FileChannel FileChannel$MapMode
                                 Pipe]
    [java.nio.channels.spi       AbstractSelectableChannel]
    [java.nio.file Path          Paths]
    [java.util                   Locale]
    [javafx.collections          FXCollections]
    [java.sql                    Blob Clob])))

(defnt ->uuid*
  ([^string? id] (java.util.UUID/fromString        id))
  ([^bytes?  id] (java.util.UUID/nameUUIDFromBytes id))
  ([^Long msb lsb]
     (java.util.UUID. msb ^Long lsb)))

#?(:clj
(defmacro ->uuid
  "Because 'IllegalArgumentException Definition of function ->uuid-protocol
            in protocol __GT_uuidProtocol must take at least one arg'"
  [& args]
  (if (empty? args)
      `(java.util.UUID/randomUUID)
      `(->uuid* ~@args))))

(declare ->uri)

(defnt ^java.io.File ->file
  {:todo "Eliminate reflection"}
  ([^java.io.File           x] x          )
  ([^java.nio.file.Path     x] (.toFile x))
  ([#{string? java.net.URI} x] (File.   x))
  ([^java.net.URL           x] (-> x ->uri ->file))
  ([                        x] (io/file x)))

(defnt ^java.net.URI ->uri
  {:todo "Eliminate reflection"}
  ([^java.net.URI                x] x)
  ([^java.nio.file.Path          x] (.toUri x))
  ([#{java.io.File java.net.URL} x] (.toURI x))
  ([^string?                     x] (URI. x))
  ([                             x] (-> x ->file ->uri)))

(defnt ^java.net.URL ->url
  ([^string?      x] (URL. x))
  ([^java.net.URL x] x)
  ([^java.net.URI x] (.toURL x))
  ([              x] (-> x ->uri ->url)))

(defnt ^java.net.InetAddress ->inet-address
  ([^string? x] (InetAddress/getByName x)))

(defnt ^java.nio.file.Path ->path
  ([^java.nio.file.Path x] x)
  ([                    x] (Paths/get ^URI (->uri x))))
  ; TODO have a smart mechanism which adds arity based on
  ; unaccounted-for arities from ->uri

(defnt ->buffered
  ([^java.io.BufferedInputStream  x] x)
  ([^java.io.BufferedOutputStream x] x)
  ([^java.io.InputStream          x] (BufferedInputStream.  x))
  ([^java.io.OutputStream         x] (BufferedOutputStream. x)))

(defnt ->observable ; O(1)
  ([^vector? v] (FXCollections/observableArrayList v))
  ([^listy?  l] (FXCollections/observableArrayList l)))

(defalias ->predicate coll/->predicate)

#_(defalias ->keyword str/->keyword)

(defnt ^java.io.InputStream ->input-stream
  {:attribution "ztellman/byte-streams"
   :contributors {"Alex Gunnarson" "defnt-ed and added to"}}
  (^{:cost 0} [^bytes? ary]
    (ByteArrayInputStream. ary))
  ([^String x]
    (-> x arr/->bytes ->input-stream))
  (^{:cost 0} [^java.nio.ByteBuffer buf]
    (ByteBufferInputStream. (.duplicate buf))) ; in a different function, .duplicate is not used.
  (^{:cost 0} [^java.nio.channels.ReadableByteChannel channel]
    (Channels/newInputStream channel))
  (^{:cost 0} [^java.io.File x] (FileInputStream. x))
  #_(^{:cost 0} [(stream-of bytes) s options]
    (let [ps (ps/pushback-stream (get options :buffer-size 65536))]
      (s/consume
        (fn [^bytes ary]
          (ps/put-array ps ary 0 (alength ary)))
        s)
      (s/on-drained s #(ps/close ps))
      (ps/->input-stream ps)))
  #_(^{:cost 0} [(stream-of ByteBuffer) s options]
    (let [ps (ps/pushback-stream (get options :buffer-size 65536))]
      (s/consume
        (fn [^ByteBuffer buf]
          (ps/put-buffer ps (.duplicate buf)))
        s)
      (s/on-drained s #(ps/close ps))
      (ps/->input-stream ps)))
  (^{:cost 1.5} [(seq-of #'proto/ByteSource) srcs options]
    (let [chunk-size (get options :chunk-size 65536)
          out (PipedOutputStream.)
          in (PipedInputStream. out chunk-size)]
      (future
        (try
          (loop [s srcs]
            (when-not (empty? s)
              (streams/transfer (first s) out)
              (recur (rest s))))
          (finally
            (.close out))))
      in)))

(defnt ^java.io.DataInputStream ->data-input-stream
  {:attribution "ztellman/byte-streams"
   :contributors {"Alex Gunnarson" "defnt-ed"}}
  ([^java.io.DataInputStream x options] x)
  ([x options]
   (-> x (->input-stream options) (DataInputStream.))))

(defnt ^java.io.OutputStream ->output-stream
  {:attribution "ztellman/byte-streams"
   :contributors {"Alex Gunnarson" "defnt-ed"}}
  (^{:cost 0} [^java.nio.channels.WritableByteChannel channel]
    (Channels/newOutputStream channel)))

(declare ->str)

; http://java-performance.info/various-methods-of-binary-serialization-in-java/
; Look at this to learn more about writing and reading byte-buffers
(defnt ^java.nio.ByteBuffer ->byte-buffer
  {:attribution  ["ztellman/byte-streams" "ztellman/gloss.core.formats"]
   :contributors {"Alex Gunnarson" "defnt-ed"}}
  ; ===== ztellman/byte-streams ===== 
  (^{:cost 0} [^java.nio.ByteBuffer x] x)
  (^{:cost 0} [^bytes? ary] (->byte-buffer ary nil))
  (^{:cost 0} [^bytes? ary opts]
    (if (or (:direct? opts) false)
        (let [len (Array/getLength ary)
              ^ByteBuffer buf (ByteBuffer/allocateDirect len)]
          (.put buf ary 0 len)
          (.position buf 0)
          buf)
        (ByteBuffer/wrap ary)))
  (^{:cost 1} [^String x] (->byte-buffer x nil))
  (^{:cost 1} [^String x options]
    (-> x (arr/->bytes options) (->byte-buffer options)))
  #_(^{:cost 1} [(vector-of ByteBuffer) bufs {:keys [direct?] :or {direct? false}}]
    (cond
      (empty? bufs)
        (ByteBuffer/allocate 0)
      (and (empty? (rest bufs)) (not (proto/closeable? bufs)))
        (first bufs)
      :else
        (let [len (reduce + (map #(.remaining ^ByteBuffer %) bufs))
              buf (if direct?
                    (ByteBuffer/allocateDirect len)
                    (ByteBuffer/allocate len))]
          (doseq [^ByteBuffer b bufs]
            (.mark b)
            (.put buf b)
            (.reset b))
          (when (proto/closeable? bufs)
            (proto/close bufs))
          (.flip buf))))
  ; ===== ztellman/gloss.core.formats =====
  ; Costs unknown
  ; TODO add 'sequential?' to types
  #_([^sequential? x] (-> x (map ->byte-buffer) ->byte-buffer))
  ([^char?       x] (-> x ->str ->byte-buffer))
  ([^number?     x] (-> x ->byte byte-array ->byte-buffer)))

;; byte-buffer => vector of byte-buffers
(defn ->byte-buffers [buf opts])
#_(defnt ->byte-buffers
  {:attribution "ztellman/byte-streams"
   :contributors {"Alex Gunnarson" "defnt-ed"}}
    ;ClassCastException   [trace missing] ; Because of destructuring
  (^{:cost 0} [^java.nio.ByteBuffer buf opts]
    (let [{:keys [chunk-size]} opts]
      (if chunk-size
          (let [lim (.limit buf)
                indices (range (.position buf) lim chunk-size)]
            (mapv
              #(-> buf
                 .duplicate
                 (.position %)
                 ^ByteBuffer (.limit (min lim (+ % chunk-size)))
                 .slice)
              indices))
          [buf]))))

(defn ->lbyte-buffers [channel opts])
#_(defnt ->lbyte-buffers
  "To lazy sequence of byte-buffers"
  {:attribution ["ztellman/byte-streams" "ztellman/gloss.core.formats"]
   :contributors {"Alex Gunnarson" "defnt-ed"}}
   ;ClassCastException   [trace missing] ; Because of destructuring
  (^{:cost 1} [^ReadableByteChannel channel opts]
    (when (.isOpen channel)
      (let [{:keys [chunk-size direct?] :or {chunk-size 4096 direct? false}} opts]
        (lazy-seq
          (when-let [b (proto/take-bytes! channel chunk-size opts)]
            (cons b (convert channel (seq-of ByteBuffer) opts)))))))
  #_(^{:cost 0} [^File file opts]
    (let [{:keys [chunk-size writable?]
           :or {chunk-size (int 2e9), writable? false}} opts
          ^RandomAccessFile raf (RandomAccessFile. file (if writable? "rw" "r"))
          ^FileChannel fc (.getChannel raf)
          buf-seq (fn buf-seq [offset]
                    (when-not (<= (.size fc) offset)
                      (let [remaining (- (.size fc) offset)]
                        (lazy-seq
                          (cons
                            (.map fc
                              (if writable?
                                FileChannel$MapMode/READ_WRITE
                                FileChannel$MapMode/READ_ONLY)
                              offset
                              (min remaining chunk-size))
                            (buf-seq (+ offset chunk-size)))))))]
      (g/closeable-seq
        (buf-seq 0)
        false
        #(do
           (.close raf)
           (.close fc)))))
  ; Cost unknown; probably not 1
  #_([x] (gformats/to-buf-seq x)))

(defnt' in-stream->out-stream
  {:source "https://thomaswabner.wordpress.com/2007/10/09/fast-stream-copy-using-javanio-channels/"}
  (^java.nio.channels.WritableByteChannel 
    [^java.nio.channels.ReadableByteChannel in ^java.nio.channels.WritableByteChannel out]
    (let [^ByteBuffer buffer (ByteBuffer/allocateDirect (* 16 1024))]
      (while (not= -1 (.read in buffer))
        (.flip buffer)
        (.write out buffer)
        (.compact buffer))
      (.flip buffer)
      (while (.hasRemaining buffer)
        (.write out buffer))
      ; TODO must close out channel in order to flush the data.
      )))

(defnt ->byte-channel
  {:attribution "ztellman/byte-streams"
   :contributors {"Alex Gunnarson" "defnt-ed"}}
  (^{:cost 0} [^java.io.InputStream input-stream]
    (Channels/newChannel input-stream))
  (^{:cost 0} [^java.io.OutputStream output-stream]
    (Channels/newChannel output-stream))
  #_(^{:cost 1.5} [(seq-of byte-buffer) bufs]
    (let [pipe (Pipe/open)
          ^WritableByteChannel sink (.sink pipe)
          source (doto ^AbstractSelectableChannel (.source pipe)
                   (.configureBlocking true))]
      (future
        (try
          (loop [s bufs]
            (when (and (not (empty? s)) (.isOpen sink))
              (let [buf (.duplicate ^ByteBuffer (first s))]
                (.write sink buf)
                (recur (rest s)))))
          (finally
            (.close sink))))
      source)))

; ByteSource : generic byte-source 
(defnt ^CharSequence ->char-seq
  {:attribution "ztellman/byte-streams"
   :contributors {"Alex Gunnarson" "defnt-ed"}}
  (^{:cost 0} [^CharSequence x] x)
  #_(^{:cost 2} [^proto/ByteSource source options]
    (cs/decode-byte-source
      #(when-let [bytes (proto/take-bytes! source % options)]
         (->byte-array bytes options))
      #(when (proto/closeable? source)
         (proto/close source))
      options))
  #_(^{:cost 1.5} [^java.io.Reader reader opts]
    (let [{:keys [chunk-size] :or {chunk-size 2048}} opts
          ary (char-array chunk-size)
          sb (StringBuilder.)]
      (loop []
        (let [n (.read reader ary 0 chunk-size)]
          (if (pos? n)
            (do
              (.append sb ary 0 n)
              (recur))
            (.toString sb)))))))

(defnt ->char-buffer
  {:attribution "ztellman/gloss.core.formats"}
  ([^java.nio.CharBuffer x] x)
  ([            x] (when x (-> x ->char-seq CharBuffer/wrap))))


(defnt ^String ->str
  {:contributors {"Alex Gunnarson"        "defnt-ed"
                  "ztellman/byte-streams" nil
                  "funcool/octet"         nil}
   :todo ["Test these against ->bytes"]}
  ([^string? x        ] x)
  ([^string? x options] x)
  ([#{boolean char int long float double} x] (String/valueOf x))
  #?(:clj
  ([^integer? n radix]
    #?(:clj  (.toString (biginteger n) radix)
       :cljs (.toString n radix))))

  ([^bytes?  x        ] (->str x nil))
  ([^bytes?  x options]
    #?(:clj
         (let [encoding (get options :encoding "UTF-8")]
           (String. x ^String (name encoding)))
       :cljs ; funcool/octet.spec.string
         (let [view     (js/Uint8Array. (.subarray input 0 (lasti x))) ; TODO maybe just copy it?
               encoding (.-fromCharCode js/String)]
           (.apply encoding nil view))))
  ([^keyword? k] (->str k "/"))
  ([^keyword? k joiner]
    (->> [(namespace k) (name k)]
         (core/remove empty?)
         (str/join joiner)))
#?(:clj
  ([^java.net.InetAddress x]
    (if-let [hostName (.getHostName x)]
      hostName
      (.getHostAddress x)))
  (^{:cost 1} [^CharSequence char-sequence]
    (.toString char-sequence))
  ([^java.nio.charset.Charset x] (.name x))
  ; Look at Apache Commons Convert to fill in the below code
  ;([^java.sql.Blob x])
  ;([^java.sql.Clob x])
  ([^java.util.Date x]
    (-> (java.text.SimpleDateFormat. (:calendar time/formats))
        (.format x)))
  ([#{java.sql.Date
      java.sql.Timestamp
      java.sql.Time}    x] (.toString x))
  ([^java.util.TimeZone x] (.getID x))
  ; The returned string is referenced to the default time zone.
  ([^java.util.Calendar x]
    (let [df (java.text.SimpleDateFormat. (:calendar time/formats))]
      (.setCalendar df x)
      (.format df (.getTime x))))
  )
#_(^{:cost 1} [(vector-of String) strings]
    (let [sb (StringBuilder.)]
      (doseq [s strings]
        (.append sb s))
      (.toString sb)))
#?(:clj
  ; CANDIDATE 0
  ([^java.io.InputStream in]
    (->str in (.name (Charset/defaultCharset))))
  ([^java.io.InputStream in enc]
    (with-open [bout (StringWriter.)]
      (io/copy in bout :encoding enc)
      (.toString bout)))
  ; CANDIDATE 1
  #_([^java.io.InputStream is]
    (let [^java.util.Scanner s
            (-> is (java.util.Scanner.) (.useDelimiter "\\A"))]
      (if (.hasNext s) (.next s) "")))
  ([^java.io.ByteArrayInputStream in-stream]
    (let [n   (.available in-stream)
          arr (byte-array n)]
      (.read in-stream arr, 0 n)
      (String. arr java.nio.charset.StandardCharsets/UTF_8))))
  ; Port this
#_([x options] (streams/convert x String options))
  ([:else x] (str x)))

(defnt ->charset
  ([^string? x] (Charset/forName x)))

(defnt ->symbol
  ([^string? x] (symbol x))
  ([:else x] (-> x ->str ->symbol)))

(defn ->reader [is opts])
#_(defnt ^Reader ->reader
  {:attribution "ztellman/byte-streams"
   :contributors {"Alex Gunnarson" "defnt-ed"}}
  (^{:cost 1.5} [^java.io.InputStream is {:keys [encoding] :or {encoding "UTF-8"}}]
    (BufferedReader. (InputStreamReader. is ^String encoding))))

(defnt ->read-channel
  {:attribution "ztellman/byte-streams"
   :contributors {"Alex Gunnarson" "defnt-ed"}}
  (^{:cost 0} [^java.io.File x]
    (let [^FileInputStream in (->input-stream x)]
      (.getChannel in))))

(defnt ->write-channel
  #_(^{:cost 0} [^File file {:keys [append?] :or {append? true}}]
    (.getChannel (FileOutputStream. file (boolean append?))))
  (^{:cost 0} [^java.io.OutputStream output-stream]
    (Channels/newChannel output-stream)))

(defnt ->channel
  "Writable or readable."
  (^{:cost 0} [^java.io.File x] (->read-channel x)))

#?(:clj
(defnt ^java.util.Locale ->locale
  ([^string? x] (Locale. x))))

(defn ->line-seq
  "Converts the object to a lazy sequence of newline-delimited strings."
  {:attribution "ztellman/byte-streams"}
  ([x options]
     (let [reader (->reader x options)
           reader (BufferedReader. ^Reader reader)
           line! (fn line! []
                   (lazy-seq
                     (when-let [l (try
                                    (.readLine reader)
                                    (catch IOException e
                                      nil))]
                       (cons l (line!)))))]
       (line!))))

(defn ->byte-source
  "Converts the object to something that satisfies |ByteSource|."
  {:attribution "ztellman/byte-streams"}
  ([x        ] (->byte-source x nil))
  ([x options] (streams/convert x #'proto/ByteSource options)))

(defn ->byte-sink
  "Converts the object to something that satisfies |ByteSink|."
  {:attribution "ztellman/byte-streams"}
  ([x        ] (->byte-sink x nil))
  ([x options] (streams/convert x #'proto/ByteSink options)))

;;; def-transfers

; COMMENTED OUT ONLY TEMPORARILY - NEED TO FIGURE OUT HOW TO INCORPORATE THESE
; (def-transfer [ReadableByteChannel File]
;   [channel file {:keys [chunk-size] :or {chunk-size (int 1e7)} :as options}]
;   (let [^FileChannel fc (convert file WritableByteChannel options)]
;     (try
;       (loop [idx 0]
;         (let [n (.transferFrom fc channel idx chunk-size)]
;           (when (pos? n)
;             (recur (+ idx n)))))
;       (finally
;         (.force fc true)
;         (.close fc)))))

; (def-transfer [File WritableByteChannel]
;   [file
;    channel
;    {:keys [chunk-size
;            close?]
;     :or {chunk-size (int 1e6)
;          close? true}
;     :as options}]
;   (let [^FileChannel fc (convert file ReadableByteChannel options)]
;     (try
;       (loop [idx 0]
;         (let [n (.transferTo fc idx chunk-size channel)]
;           (when (pos? n)
;             (recur (+ idx n)))))
;       (finally
;         (when close?
;           (.close ^WritableByteChannel channel))
;         (.close fc)))))

; (def-transfer [InputStream OutputStream]
;   [input-stream
;    output-stream
;    {:keys [chunk-size
;            close?]
;     :or {chunk-size 4096
;          close? true}
;     :as options}]
;   (let [ary (Utils/byteArray chunk-size)]
;     (try
;       (loop []
;         (let [n (.read ^InputStream input-stream ary)]
;           (when (pos? n)
;             (.write ^OutputStream output-stream ary 0 n)
;             (recur))))
;       (.flush ^OutputStream output-stream)
;       (finally
;         (.close ^InputStream input-stream)
;         (when close?
;           (.close ^OutputStream output-stream))))))

; TODO StackOverflowError
#_(let [special-character? (->> "' _-+=`~{}[]()\\/#@!?.,;\"" (map (MWA int)) set)]
  (defn- readable-character? [x]
    (or (Character/isLetterOrDigit (int x))
        (special-character?        (int x)))))

(defalias print-bytes streams/print-bytes)



(def cmp-bufs @#'streams/cmp-bufs)

(defnt' ^long compare-bytes
  "Returns a comparison result for two byte streams."
  {:contributors {"Alex Gunnarson" "Optimized via defnt' instead of instanceof"}}
  ([#{bytes? ByteBuffer String} a #{bytes? ByteBuffer String} b]
    (cmp-bufs (->byte-buffer a) (->byte-buffer b)))
  ([a b]
    (loop [a (->byte-buffers a) b (->byte-buffers b)]
      (cond
        (empty? a)
          (if (empty? b) 0 -1)
        (empty? b)
          1
        :else
          (let [cmp (cmp-bufs (first a) (first b))]
            (if (num/== 0 cmp)
              (recur (rest a) (rest b))
              cmp))))))

(defn bytes=
  "Returns true if the two byte streams are equivalent."
  [a b]
  (num/== 0 (compare-bytes a b)))

(defalias ->bytes      arr/->bytes)
(defalias bytes->longs arr/bytes->longs)

; CLJS
; (defn mime-type
;   {:source ["https://en.wikipedia.org/wiki/List_of_file_signatures"
;             "https://mimesniff.spec.whatwg.org/#matching-an-image-type-pattern"]}
;   [file]
;   (let [file-reader (js/FileReader.)
;         header (atom nil)]
;     (set! (.-onloadend file-reader)
;       (fn [e]
;         (when-let [result (-> e .-target .-result)]
;           (let [arr (-> result (js/Uint8Array.) (.subarray 0 4))]
;             (reset! header
;               (reduce
;                 (fn [ret elem]
;                   (str ret (.toString elem 16)))
;                 ""
;                 (array-seq arr)))))))
;     ; Check the file signature against known types
;     (.readAsArrayBuffer file-reader file)
;     (condpc = header
;       "89504e47" :image/png
;       "47494638" :image/gif
;       (coll-or "ffd8ffe0" "ffd8ffe1" "ffd8ffe2") :image/jpeg
;       (.-type file))) 
; )