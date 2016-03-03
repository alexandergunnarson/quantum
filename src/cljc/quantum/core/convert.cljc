(ns quantum.core.convert
  (:require-quantum [:core])
  (:require [cognitect.transit                  :as t     ]
            [quantum.core.numeric               :as num   ]
            [quantum.core.string                :as str   ]
            [quantum.core.convert.core          :as conv  ]
            [quantum.core.data.complex.json     :as json  ]
    #?(:clj [clojure.tools.emitter.jvm                    ])
            [#?(:clj  clojure.tools.reader
                :cljs cljs.tools.reader       ) :as r     ]
            [#?(:clj  clojure.tools.reader.edn
                :cljs cljs.tools.reader.edn   ) :as r-edn ]
   #?(:cljs [cljs.reader :as core-r])
   #?(:cljs [goog.crypt.base64              :as base64]))
  #?(:clj (:import (org.apache.commons.codec.binary Base64)
                   java.io.EOFException
                   clojure.tools.reader.reader_types.IPushbackReader)))

(defalias ->name        conv/->name       )
(defalias ->symbol      conv/->symbol     )
(defalias ->str         conv/->str        )
(defalias ->mdb         conv/->mdb        )
(defalias utf8-string   conv/utf8-string  )
(defalias base64-encode conv/base64-encode)
(defalias base64-decode conv/base64-decode)
(defalias parse-bytes   conv/parse-bytes  )
(defalias parse-integer conv/parse-integer)
(defalias parse-long    conv/parse-long   )
(defalias parse-float   conv/parse-float  )
(defalias parse-double  conv/parse-double )

(defn transit->
  "Transit decode an object from @x."
  ([x type] (transit-> x type nil))
  ([x type opts]
    #?(:clj  (with-open [in (java.io.ByteArrayInputStream. (.getBytes ^String x))]
               (-> (t/reader (java.io.BufferedInputStream. in) type opts)
                   (t/read)))
       :cljs (-> (t/reader type opts)
                 (t/read x)))))

(defn ->transit
  "Transit encode @x into a String."
  ([x type] (->transit x type nil))
  ([x type opts]
    #?(:clj  (with-open [out (java.io.ByteArrayOutputStream.)]
               (-> (t/writer (java.io.BufferedOutputStream. out) type opts)
                   (t/write x))
               (.toString out))
       :cljs (-> (t/writer type opts)
                 (t/write x)))))

(defn ->path
  [& args]
  (apply quantum.core.string/join-once "/" args))

(defalias json-> json/json->)
(defalias ->json json/->json)

#?(:cljs
(defn byte-array->base64
  {:todo ["This is an extremely inefficient algorithm"]}
  [x]
  (js/btoa (.apply js/String.fromCharCode nil x))))

#?(:cljs (defn base64->forge-bytes [x] (js/forge.util.decode64 x)))
#?(:cljs (defn forge-bytes->base64 [x] (js/forge.util.encode64 x)))

; TODO test how to use these
#?(:cljs (defn ?->utf-8 [x] (js/forge.util.encodeUtf8 x)))
#?(:cljs (defn utf-8->? [x] (js/forge.util.decodeUtf8 x)))

#?(:cljs (defn bytes->hex [x] (js/forge.util.bytesToHex x)))
#?(:cljs (defn hex->bytes [x] (js/forge.util.hexToBytes x)))

; TODO look at https://github.com/digitalbazaar/forge#task
; and use those methods if you want to manipulate them 
#?(:cljs
(defn ->forge-byte-buffer
  ([] (js/forge.util.createBuffer))
  ([x]
    ; create a byte buffer from raw binary bytes
    ; create a byte buffer from utf8 bytes
    (if (string? x)
        (js/forge.util.createBuffer x "utf8")
        (js/forge.util.createBuffer x "raw")))))

;#?(:cljs (defn ))

; (defn streams=
 ;  {:from "alioth.util.core"}
;   "Predicate that is true iff the contents of the streams are identical."
;   [& streams]
;   (letfn [(advance [] (map (fn [^InputStream s] (.read s)) streams))]
;     (loop [nth-chars (advance)]
;       (if (apply not= nth-chars)
;         false
;         (if (= -1 (first nth-chars)) 
;           true
;           (recur (advance))))))) 


; FROM macourtney/clj-crypto
; (defn integer-byte [integer byte-offset]
;   (let [short-int (bit-and 0xff (bit-shift-right integer (* byte-offset 8)))]
;     (if (< short-int 128)
;       (byte short-int)
;       (byte (- short-int 256)))))

; (defn integer-bytes [integer]
;   (byte-array [(integer-byte integer 3) (integer-byte integer 2) (integer-byte integer 1) (integer-byte integer 0)]))

; (defn long-bytes [l]
;   (let [buf (ByteBuffer/allocate (/ Long/SIZE 8))]
;     (.putLong buf l)
;     (.array buf)))

; (defn get-data-bytes [data]
;   (cond
;     (= Byte/TYPE (.getComponentType (class data))) data
;     (string? data) (.getBytes data default-character-encoding)
;     (instance? Integer data) (integer-bytes data) ; Must use instance since integer? includes Longs as well as Integers.
;     (instance? Long data) (long-bytes data)
;     :else (throw (RuntimeException. (str "Do not know how to convert a " (class data) " to a byte array.")))))

; TODO find difference between clojure.core/read-string (cljs.reader/read-string) and clj tools.reader.

#?(:clj
(defn ->eval
  "Inherently unsafe with the default of *read-eval*=true"
  {:todo ["clojure.core/read-string might be faster.
           Do we need clojure.tools.reader's features?"
          "Test performance of r/StringReader, r/InputStreamReader,
           r/PushbackReader, r/IndexingPushbackReader etc. against Java impls"]}
  [x & [opts]]
  (cond
    (string? x)
    (r/read-string opts x)

    (or (instance? IPushbackReader        x)
        (instance? java.io.PushbackReader x))
    (r/read opts x)

    (-> opts :impl (= :ana))
    (clojure.tools.emitter.jvm/eval x)

    :else (eval x))))

#?(:clj
(defn ->form
  [x & [opts]]
  (cond
    (string? x)
    (r-edn/read-string opts x)

    (or (instance? IPushbackReader        x)
        (instance? java.io.PushbackReader x))
    (r-edn/read opts x)
    
    :else x)))

; TODO incorporate conversion functions at end of (clojure|cljs).tools.reader.reader-types

(defn ->char
  "like |char| but doesn't throw"
  [x]
  (when-not (nil? x)
    (core/char x)))

#?(:clj
(defn ^long read-byte
  {:from "clojure.tools.nrepl.bencode"}
  [^java.io.InputStream input]
  (let [c (.read input)]
    (when (neg? c)
      (throw (EOFException. "Invalid netstring. Unexpected end of input.")))
    ;; Here we have a quirk for example. `.read` returns -1 on end of
    ;; input. However the Java `Byte` has only a range from -128 to 127.
    ;; How does the fit together?
    ;;
    ;; The whole thing is shifted. `.read` actually returns an int
    ;; between zero and 255. Everything below the value 128 stands
    ;; for itself. But larger values are actually negative byte values.
    ;;
    ;; So we have to do some translation here. `Byte/byteValue` would
    ;; do that for us, but we want to avoid boxing here.
    (if (< 127 c) (- c 256) c))))

#?(:clj
(defn ^"[B" read-bytes
  {:from "clojure.tools.nrepl.bencode"}
  [^java.io.InputStream input n]
  (let [content (byte-array n)]
    (loop [offset (int 0)
           len    (int n)]
      (let [result (.read input content offset len)]
        (when (neg? result)
          (throw
            (EOFException.
              "Invalid netstring. Less data available than expected.")))
        (when (not= result len)
          (recur (+ offset result) (- len result)))))
    content)))











; TODO UNCOMMENT THIS — IT'S GOOD

; (ns ^{:doc "The Rosetta Stone."}
;   quantum.core.convert ; perhaps coerce?
;   (:require-quantum [ns fn logic num macros type io log str coll arr pconvert])
;   (:require
;     [byte-streams :as streams]
;     ; CompilerException java.lang.NoClassDefFoundError: IllegalName: compile__stub.gloss.data.bytes.core.gloss.data.bytes.core/MultiBufferSequence, compiling:(gloss/data/bytes/core.clj:78:1) 
;     ; [gloss.core.formats         :as gformats]
;     [manifold.stream              :as s    ]
;     [manifold.deferred            :as d    ]
;     [byte-streams.graph           :as g    ]
;     [byte-streams.protocols       :as proto]
;     [byte-streams.pushback-stream :as ps   ]
;     [byte-streams.char-sequence   :as cs   ])
; #?(:clj
;   (:import
;     [quantum.core.data.streams    ByteBufferInputStream]
;     [byte_streams.graph          Type]
;     [java.lang.reflect           Array]
;     [java.util.concurrent.atomic AtomicBoolean]
;     [java.net                    URI URL InetAddress]
;     ; http://java-performance.info/java-io-bytearrayoutputstream/ says:
;     ; Do not use ByteArrayOutputStream in performance critical code — it's synchronized
;     ; For performance critical code try to use ByteBuffer instead of ByteArrayOutputStream.
;     [java.io                     File
;                                  FileOutputStream FileInputStream
;                                  ByteArrayInputStream ByteArrayOutputStream
;                                  PipedOutputStream PipedInputStream
;                                  DataInputStream
;                                  InputStream OutputStream
;                                  IOException
;                                  RandomAccessFile
;                                  Reader BufferedReader InputStreamReader]
;     [java.nio                    ByteBuffer DirectByteBuffer CharBuffer]
;     [java.nio.charset            Charset]
;     [java.nio.channels           Channels
;                                  ReadableByteChannel WritableByteChannel
;                                  FileChannel FileChannel$MapMode
;                                  Pipe]
;     [java.nio.channels.spi       AbstractSelectableChannel]
;     [java.nio.file Path          Paths]
;     [java.util                   Locale]
;     [javafx.collections          FXCollections]
;     [java.sql                    Blob Clob])))

; (defnt ->uuid*
;   ([^string? id] (java.util.UUID/fromString        id))
;   ([^bytes?  id] (java.util.UUID/nameUUIDFromBytes id))
;   ([^Long msb lsb]
;      (java.util.UUID. msb ^Long lsb)))

; #?(:clj
; (defmacro ->uuid
;   "Because 'IllegalArgumentException Definition of function ->uuid-protocol
;             in protocol __GT_uuidProtocol must take at least one arg'"
;   [& args]
;   (if (empty? args)
;       `(java.util.UUID/randomUUID)
;       `(->uuid* ~@args))))

; (declare ->uri)

; (defnt ^java.io.File ->file
;   {:todo "Eliminate reflection"}
;   ([^java.io.File           x] x          )
;   ([^java.nio.file.Path     x] (.toFile x))
;   ([#{string? java.net.URI} x] (File.   x))
;   ([^java.net.URL           x] (-> x ->uri ->file))
;   ([                        x] (io/file x)))

; (defnt ^java.net.URI ->uri
;   {:todo "Eliminate reflection"}
;   ([^java.net.URI                x] x)
;   ([^java.nio.file.Path          x] (.toUri x))
;   ([#{java.io.File java.net.URL} x] (.toURI x))
;   ([^string?                     x] (URI. x))
;   ([                             x] (-> x ->file ->uri)))

; (defnt ^java.net.URL ->url
;   ([^string?      x] (URL. x))
;   ([^java.net.URL x] x)
;   ([^java.net.URI x] (.toURL x))
;   ([              x] (-> x ->uri ->url)))

; (defnt ^java.net.InetAddress ->inet-address
;   ([^string? x] (InetAddress/getByName x)))

; (defnt ^java.nio.file.Path ->path
;   ([^java.nio.file.Path x] x)
;   ([                    x] (Paths/get ^URI (->uri x))))
;   ; TODO have a smart mechanism which adds arity based on
;   ; unaccounted-for arities from ->uri

; (defnt ->buffered
;   ([^java.io.BufferedInputStream  x] x)
;   ([^java.io.BufferedOutputStream x] x)
;   ([^java.io.InputStream          x] (BufferedInputStream.  x))
;   ([^java.io.OutputStream         x] (BufferedOutputStream. x)))

; (defnt ->observable ; O(1)
;   ([^vector? v] (FXCollections/observableArrayList v))
;   ([^listy?  l] (FXCollections/observableArrayList l)))

; (defalias ->predicate coll/->predicate)

; #_(defalias ->keyword str/->keyword)

; (defnt ^java.io.InputStream ->input-stream
;   {:attribution "ztellman/byte-streams"
;    :contributors {"Alex Gunnarson" "defnt-ed and added to"}}
;   (^{:cost 0} [^bytes? ary]
;     (ByteArrayInputStream. ary))
;   ([^String x]
;     (-> x arr/->bytes ->input-stream))
;   (^{:cost 0} [^java.nio.ByteBuffer buf]
;     (ByteBufferInputStream. (.duplicate buf))) ; in a different function, .duplicate is not used.
;   (^{:cost 0} [^java.nio.channels.ReadableByteChannel channel]
;     (Channels/newInputStream channel))
;   (^{:cost 0} [^java.io.File x] (FileInputStream. x))
;   #_(^{:cost 0} [(stream-of bytes) s options]
;     (let [ps (ps/pushback-stream (get options :buffer-size 65536))]
;       (s/consume
;         (fn [^bytes ary]
;           (ps/put-array ps ary 0 (alength ary)))
;         s)
;       (s/on-drained s #(ps/close ps))
;       (ps/->input-stream ps)))
;   #_(^{:cost 0} [(stream-of ByteBuffer) s options]
;     (let [ps (ps/pushback-stream (get options :buffer-size 65536))]
;       (s/consume
;         (fn [^ByteBuffer buf]
;           (ps/put-buffer ps (.duplicate buf)))
;         s)
;       (s/on-drained s #(ps/close ps))
;       (ps/->input-stream ps)))
;   (^{:cost 1.5} [(seq-of #'proto/ByteSource) srcs options]
;     (let [chunk-size (get options :chunk-size 65536)
;           out (PipedOutputStream.)
;           in (PipedInputStream. out chunk-size)]
;       (future
;         (try
;           (loop [s srcs]
;             (when-not (empty? s)
;               (streams/transfer (first s) out)
;               (recur (rest s))))
;           (finally
;             (.close out))))
;       in)))

; (defnt ^java.io.DataInputStream ->data-input-stream
;   {:attribution "ztellman/byte-streams"
;    :contributors {"Alex Gunnarson" "defnt-ed"}}
;   ([^java.io.DataInputStream x options] x)
;   ([x options]
;    (-> x (->input-stream options) (DataInputStream.))))

; (defnt ^java.io.OutputStream ->output-stream
;   {:attribution "ztellman/byte-streams"
;    :contributors {"Alex Gunnarson" "defnt-ed"}}
;   (^{:cost 0} [^java.nio.channels.WritableByteChannel channel]
;     (Channels/newOutputStream channel)))

; (declare ->str)

; ; http://java-performance.info/various-methods-of-binary-serialization-in-java/
; ; Look at this to learn more about writing and reading byte-buffers
; (defnt ^java.nio.ByteBuffer ->byte-buffer
;   {:attribution  ["ztellman/byte-streams" "ztellman/gloss.core.formats"]
;    :contributors {"Alex Gunnarson" "defnt-ed"}}
;   ; ===== ztellman/byte-streams ===== 
;   (^{:cost 0} [^java.nio.ByteBuffer x] x)
;   (^{:cost 0} [^bytes? ary] (->byte-buffer ary nil))
;   (^{:cost 0} [^bytes? ary opts]
;     (if (or (:direct? opts) false)
;         (let [len (Array/getLength ary)
;               ^ByteBuffer buf (ByteBuffer/allocateDirect len)]
;           (.put buf ary 0 len)
;           (.position buf 0)
;           buf)
;         (ByteBuffer/wrap ary)))
;   (^{:cost 1} [^String x] (->byte-buffer x nil))
;   (^{:cost 1} [^String x options]
;     (-> x (arr/->bytes options) (->byte-buffer options)))
;   #_(^{:cost 1} [(vector-of ByteBuffer) bufs {:keys [direct?] :or {direct? false}}]
;     (cond
;       (empty? bufs)
;         (ByteBuffer/allocate 0)
;       (and (empty? (rest bufs)) (not (proto/closeable? bufs)))
;         (first bufs)
;       :else
;         (let [len (reduce + (map #(.remaining ^ByteBuffer %) bufs))
;               buf (if direct?
;                     (ByteBuffer/allocateDirect len)
;                     (ByteBuffer/allocate len))]
;           (doseq [^ByteBuffer b bufs]
;             (.mark b)
;             (.put buf b)
;             (.reset b))
;           (when (proto/closeable? bufs)
;             (proto/close bufs))
;           (.flip buf))))
;   ; ===== ztellman/gloss.core.formats =====
;   ; Costs unknown
;   ; TODO add 'sequential?' to types
;   #_([^sequential? x] (-> x (map ->byte-buffer) ->byte-buffer))
;   ([^char?       x] (-> x ->str ->byte-buffer))
;   ([^number?     x] (-> x ->byte byte-array ->byte-buffer)))

; ;; byte-buffer => vector of byte-buffers
; (defn ->byte-buffers [buf opts])
; #_(defnt ->byte-buffers
;   {:attribution "ztellman/byte-streams"
;    :contributors {"Alex Gunnarson" "defnt-ed"}}
;     ;ClassCastException   [trace missing] ; Because of destructuring
;   (^{:cost 0} [^java.nio.ByteBuffer buf opts]
;     (let [{:keys [chunk-size]} opts]
;       (if chunk-size
;           (let [lim (.limit buf)
;                 indices (range (.position buf) lim chunk-size)]
;             (mapv
;               #(-> buf
;                  .duplicate
;                  (.position %)
;                  ^ByteBuffer (.limit (min lim (+ % chunk-size)))
;                  .slice)
;               indices))
;           [buf]))))

; (defn ->lbyte-buffers [channel opts])
; #_(defnt ->lbyte-buffers
;   "To lazy sequence of byte-buffers"
;   {:attribution ["ztellman/byte-streams" "ztellman/gloss.core.formats"]
;    :contributors {"Alex Gunnarson" "defnt-ed"}}
;    ;ClassCastException   [trace missing] ; Because of destructuring
;   (^{:cost 1} [^ReadableByteChannel channel opts]
;     (when (.isOpen channel)
;       (let [{:keys [chunk-size direct?] :or {chunk-size 4096 direct? false}} opts]
;         (lazy-seq
;           (when-let [b (proto/take-bytes! channel chunk-size opts)]
;             (cons b (convert channel (seq-of ByteBuffer) opts)))))))
;   #_(^{:cost 0} [^File file opts]
;     (let [{:keys [chunk-size writable?]
;            :or {chunk-size (int 2e9), writable? false}} opts
;           ^RandomAccessFile raf (RandomAccessFile. file (if writable? "rw" "r"))
;           ^FileChannel fc (.getChannel raf)
;           buf-seq (fn buf-seq [offset]
;                     (when-not (<= (.size fc) offset)
;                       (let [remaining (- (.size fc) offset)]
;                         (lazy-seq
;                           (cons
;                             (.map fc
;                               (if writable?
;                                 FileChannel$MapMode/READ_WRITE
;                                 FileChannel$MapMode/READ_ONLY)
;                               offset
;                               (min remaining chunk-size))
;                             (buf-seq (+ offset chunk-size)))))))]
;       (g/closeable-seq
;         (buf-seq 0)
;         false
;         #(do
;            (.close raf)
;            (.close fc)))))
;   ; Cost unknown; probably not 1
;   #_([x] (gformats/to-buf-seq x)))

; (defnt' in-stream->out-stream
;   {:source "https://thomaswabner.wordpress.com/2007/10/09/fast-stream-copy-using-javanio-channels/"}
;   (^java.nio.channels.WritableByteChannel 
;     [^java.nio.channels.ReadableByteChannel in ^java.nio.channels.WritableByteChannel out]
;     (let [^ByteBuffer buffer (ByteBuffer/allocateDirect (* 16 1024))]
;       (while (not= -1 (.read in buffer))
;         (.flip buffer)
;         (.write out buffer)
;         (.compact buffer))
;       (.flip buffer)
;       (while (.hasRemaining buffer)
;         (.write out buffer))
;       ; TODO must close out channel in order to flush the data.
;       )))

; (defnt ->byte-channel
;   {:attribution "ztellman/byte-streams"
;    :contributors {"Alex Gunnarson" "defnt-ed"}}
;   (^{:cost 0} [^java.io.InputStream input-stream]
;     (Channels/newChannel input-stream))
;   (^{:cost 0} [^java.io.OutputStream output-stream]
;     (Channels/newChannel output-stream))
;   #_(^{:cost 1.5} [(seq-of byte-buffer) bufs]
;     (let [pipe (Pipe/open)
;           ^WritableByteChannel sink (.sink pipe)
;           source (doto ^AbstractSelectableChannel (.source pipe)
;                    (.configureBlocking true))]
;       (future
;         (try
;           (loop [s bufs]
;             (when (and (not (empty? s)) (.isOpen sink))
;               (let [buf (.duplicate ^ByteBuffer (first s))]
;                 (.write sink buf)
;                 (recur (rest s)))))
;           (finally
;             (.close sink))))
;       source)))

; ; ByteSource : generic byte-source 
; (defnt ^CharSequence ->char-seq
;   {:attribution "ztellman/byte-streams"
;    :contributors {"Alex Gunnarson" "defnt-ed"}}
;   (^{:cost 0} [^CharSequence x] x)
;   #_(^{:cost 2} [^proto/ByteSource source options]
;     (cs/decode-byte-source
;       #(when-let [bytes (proto/take-bytes! source % options)]
;          (->byte-array bytes options))
;       #(when (proto/closeable? source)
;          (proto/close source))
;       options))
;   #_(^{:cost 1.5} [^java.io.Reader reader opts]
;     (let [{:keys [chunk-size] :or {chunk-size 2048}} opts
;           ary (char-array chunk-size)
;           sb (StringBuilder.)]
;       (loop []
;         (let [n (.read reader ary 0 chunk-size)]
;           (if (pos? n)
;             (do
;               (.append sb ary 0 n)
;               (recur))
;             (.toString sb)))))))

; (defnt ->char-buffer
;   {:attribution "ztellman/gloss.core.formats"}
;   ([^java.nio.CharBuffer x] x)
;   ([            x] (when x (-> x ->char-seq CharBuffer/wrap))))


; (defnt ^String ->str
;   {:contributors {"Alex Gunnarson"        "defnt-ed"
;                   "ztellman/byte-streams" nil
;                   "funcool/octet"         nil}
;    :todo ["Test these against ->bytes"]}
;   ([^string? x        ] x)
;   ([^string? x options] x)
;   ([#{boolean char int long float double} x] (String/valueOf x))
;   #?(:clj
;   ([^integer? n radix]
;     #?(:clj  (.toString (biginteger n) radix)
;        :cljs (.toString n radix))))

;   ([^bytes?  x        ] (->str x nil))
;   ([^bytes?  x options]
;     #?(:clj
;          (let [encoding (get options :encoding "UTF-8")]
;            (String. x ^String (name encoding)))
;        :cljs ; funcool/octet.spec.string
;          (let [view     (js/Uint8Array. (.subarray input 0 (lasti x))) ; TODO maybe just copy it?
;                encoding (.-fromCharCode js/String)]
;            (.apply encoding nil view))))
;   ([^keyword? k] (->str k "/"))
;   ([^keyword? k joiner]
;     (->> [(namespace k) (name k)]
;          (core/remove empty?)
;          (str/join joiner)))
; #?(:clj
;   ([^java.net.InetAddress x]
;     (if-let [hostName (.getHostName x)]
;       hostName
;       (.getHostAddress x)))
;   (^{:cost 1} [^CharSequence char-sequence]
;     (.toString char-sequence))
;   ([^java.nio.charset.Charset x] (.name x))
;   ; Look at Apache Commons Convert to fill in the below code
;   ;([^java.sql.Blob x])
;   ;([^java.sql.Clob x])
;   ([^java.util.Date x]
;     (-> (java.text.SimpleDateFormat. (:calendar time/formats))
;         (.format x)))
;   ([#{java.sql.Date
;       java.sql.Timestamp
;       java.sql.Time}    x] (.toString x))
;   ([^java.util.TimeZone x] (.getID x))
;   ; The returned string is referenced to the default time zone.
;   ([^java.util.Calendar x]
;     (let [df (java.text.SimpleDateFormat. (:calendar time/formats))]
;       (.setCalendar df x)
;       (.format df (.getTime x))))
;   )
; #_(^{:cost 1} [(vector-of String) strings]
;     (let [sb (StringBuilder.)]
;       (doseq [s strings]
;         (.append sb s))
;       (.toString sb)))
; #?(:clj
;   ; CANDIDATE 0
;   ([^java.io.InputStream in]
;     (->str in (.name (Charset/defaultCharset))))
;   ([^java.io.InputStream in enc]
;     (with-open [bout (StringWriter.)]
;       (io/copy in bout :encoding enc)
;       (.toString bout)))
;   ; CANDIDATE 1
;   #_([^java.io.InputStream is]
;     (let [^java.util.Scanner s
;             (-> is (java.util.Scanner.) (.useDelimiter "\\A"))]
;       (if (.hasNext s) (.next s) "")))
;   ([^java.io.ByteArrayInputStream in-stream]
;     (let [n   (.available in-stream)
;           arr (byte-array n)]
;       (.read in-stream arr, 0 n)
;       (String. arr java.nio.charset.StandardCharsets/UTF_8))))
;   ; Port this
; #_([x options] (streams/convert x String options))
;   ([:else x] (str x)))

; (defnt ->charset
;   ([^string? x] (Charset/forName x)))

; (defnt ->symbol
;   ([^string? x] (symbol x))
;   ([:else x] (-> x ->str ->symbol)))

; (defn ->reader [is opts])
; #_(defnt ^Reader ->reader
;   {:attribution "ztellman/byte-streams"
;    :contributors {"Alex Gunnarson" "defnt-ed"}}
;   (^{:cost 1.5} [^java.io.InputStream is {:keys [encoding] :or {encoding "UTF-8"}}]
;     (BufferedReader. (InputStreamReader. is ^String encoding))))

; (defnt ->read-channel
;   {:attribution "ztellman/byte-streams"
;    :contributors {"Alex Gunnarson" "defnt-ed"}}
;   (^{:cost 0} [^java.io.File x]
;     (let [^FileInputStream in (->input-stream x)]
;       (.getChannel in))))

; (defnt ->write-channel
;   #_(^{:cost 0} [^File file {:keys [append?] :or {append? true}}]
;     (.getChannel (FileOutputStream. file (boolean append?))))
;   (^{:cost 0} [^java.io.OutputStream output-stream]
;     (Channels/newChannel output-stream)))

; (defnt ->channel
;   "Writable or readable."
;   (^{:cost 0} [^java.io.File x] (->read-channel x)))

; #?(:clj
; (defnt ^java.util.Locale ->locale
;   ([^string? x] (Locale. x))))

; (defn ->line-seq
;   "Converts the object to a lazy sequence of newline-delimited strings."
;   {:attribution "ztellman/byte-streams"}
;   ([x options]
;      (let [reader (->reader x options)
;            reader (BufferedReader. ^Reader reader)
;            line! (fn line! []
;                    (lazy-seq
;                      (when-let [l (try
;                                     (.readLine reader)
;                                     (catch IOException e
;                                       nil))]
;                        (cons l (line!)))))]
;        (line!))))

; (defn ->byte-source
;   "Converts the object to something that satisfies |ByteSource|."
;   {:attribution "ztellman/byte-streams"}
;   ([x        ] (->byte-source x nil))
;   ([x options] (streams/convert x #'proto/ByteSource options)))

; (defn ->byte-sink
;   "Converts the object to something that satisfies |ByteSink|."
;   {:attribution "ztellman/byte-streams"}
;   ([x        ] (->byte-sink x nil))
;   ([x options] (streams/convert x #'proto/ByteSink options)))

; ;;; def-transfers

; ; COMMENTED OUT ONLY TEMPORARILY - NEED TO FIGURE OUT HOW TO INCORPORATE THESE
; ; (def-transfer [ReadableByteChannel File]
; ;   [channel file {:keys [chunk-size] :or {chunk-size (int 1e7)} :as options}]
; ;   (let [^FileChannel fc (convert file WritableByteChannel options)]
; ;     (try
; ;       (loop [idx 0]
; ;         (let [n (.transferFrom fc channel idx chunk-size)]
; ;           (when (pos? n)
; ;             (recur (+ idx n)))))
; ;       (finally
; ;         (.force fc true)
; ;         (.close fc)))))

; ; (def-transfer [File WritableByteChannel]
; ;   [file
; ;    channel
; ;    {:keys [chunk-size
; ;            close?]
; ;     :or {chunk-size (int 1e6)
; ;          close? true}
; ;     :as options}]
; ;   (let [^FileChannel fc (convert file ReadableByteChannel options)]
; ;     (try
; ;       (loop [idx 0]
; ;         (let [n (.transferTo fc idx chunk-size channel)]
; ;           (when (pos? n)
; ;             (recur (+ idx n)))))
; ;       (finally
; ;         (when close?
; ;           (.close ^WritableByteChannel channel))
; ;         (.close fc)))))

; ; (def-transfer [InputStream OutputStream]
; ;   [input-stream
; ;    output-stream
; ;    {:keys [chunk-size
; ;            close?]
; ;     :or {chunk-size 4096
; ;          close? true}
; ;     :as options}]
; ;   (let [ary (Utils/byteArray chunk-size)]
; ;     (try
; ;       (loop []
; ;         (let [n (.read ^InputStream input-stream ary)]
; ;           (when (pos? n)
; ;             (.write ^OutputStream output-stream ary 0 n)
; ;             (recur))))
; ;       (.flush ^OutputStream output-stream)
; ;       (finally
; ;         (.close ^InputStream input-stream)
; ;         (when close?
; ;           (.close ^OutputStream output-stream))))))

; ; TODO StackOverflowError
; #_(let [special-character? (->> "' _-+=`~{}[]()\\/#@!?.,;\"" (map (MWA int)) set)]
;   (defn- readable-character? [x]
;     (or (Character/isLetterOrDigit (int x))
;         (special-character?        (int x)))))

; (defalias print-bytes streams/print-bytes)



; (def cmp-bufs @#'streams/cmp-bufs)

; (defnt' ^long compare-bytes
;   "Returns a comparison result for two byte streams."
;   {:contributors {"Alex Gunnarson" "Optimized via defnt' instead of instanceof"}}
;   ([#{bytes? ByteBuffer String} a #{bytes? ByteBuffer String} b]
;     (cmp-bufs (->byte-buffer a) (->byte-buffer b)))
;   ([a b]
;     (loop [a (->byte-buffers a) b (->byte-buffers b)]
;       (cond
;         (empty? a)
;           (if (empty? b) 0 -1)
;         (empty? b)
;           1
;         :else
;           (let [cmp (cmp-bufs (first a) (first b))]
;             (if (num/== 0 cmp)
;               (recur (rest a) (rest b))
;               cmp))))))

; (defn bytes=
;   "Returns true if the two byte streams are equivalent."
;   [a b]
;   (num/== 0 (compare-bytes a b)))

; (defalias ->bytes      arr/->bytes)
; (defalias bytes->longs arr/bytes->longs)

; ; CLJS
; ; (defn mime-type
; ;   {:source ["https://en.wikipedia.org/wiki/List_of_file_signatures"
; ;             "https://mimesniff.spec.whatwg.org/#matching-an-image-type-pattern"]}
; ;   [file]
; ;   (let [file-reader (js/FileReader.)
; ;         header (atom nil)]
; ;     (set! (.-onloadend file-reader)
; ;       (fn [e]
; ;         (when-let [result (-> e .-target .-result)]
; ;           (let [arr (-> result (js/Uint8Array.) (.subarray 0 4))]
; ;             (reset! header
; ;               (reduce
; ;                 (fn [ret elem]
; ;                   (str ret (.toString elem 16)))
; ;                 ""
; ;                 (array-seq arr)))))))
; ;     ; Check the file signature against known types
; ;     (.readAsArrayBuffer file-reader file)
; ;     (condpc = header
; ;       "89504e47" :image/png
; ;       "47494638" :image/gif
; ;       (coll-or "ffd8ffe0" "ffd8ffe1" "ffd8ffe2") :image/jpeg
; ;       (.-type file))) 
; ; )