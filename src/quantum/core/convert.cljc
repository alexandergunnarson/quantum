(ns quantum.core.convert ; perhaps coerce?
  (:require
    [clojure.core                       :as core]
    [cognitect.transit                  :as t]
 #?@(:clj
   [[clojure.tools.emitter.jvm]
    [clojure.java.io                    :as io]
    [manifold.stream                    :as s]
    [manifold.deferred                  :as d]
    [byte-streams                       :as streams]
    [byte-streams.graph                 :as g]
    [byte-streams.protocols             :as proto]
    [byte-streams.pushback-stream       :as ps]
    [byte-streams.char-sequence         :as cs]]
     :cljs
   [[cljs.reader                        :as core-r]
    [goog.crypt.base64                  :as base64]])
    [clojure.tools.reader               :as r]
    [clojure.tools.reader.edn           :as r-edn]
    [clojure.core.async                 :as async]
    [datascript.transit                 :as dt]
    ; CompilerException java.lang.NoClassDefFoundError: IllegalName: compile__stub.gloss.data.bytes.core.gloss.data.bytes.core/MultiBufferSequence, compiling:(gloss/data/bytes/core.clj:78:1)
  ; [gloss.core.formats                 :as gforms]
    [quantum.core.data.array            :as arr]
    [quantum.core.error                 :as err
      :refer [TODO]]
    [quantum.core.numeric               :as num]
    [quantum.core.string                :as str]
    [quantum.core.collections.core
      :refer [lasti]]
    [quantum.core.convert.core          :as conv]
    [quantum.core.convert.primitive     :as pconv]
    [quantum.core.data.complex.json     :as json]
    [quantum.core.macros                :as macros
      :refer [defnt #?(:clj defnt')]]
    [quantum.core.paths                 :as path]
    [quantum.core.fn                    :as fn]
    [quantum.core.vars                  :as var
      :refer [defalias defaliases]]
    [quantum.core.log                   :as log]
    [quantum.core.type
      :refer [static-cast]]
    [quantum.untyped.core.convert       :as u]
    [quantum.untyped.core.form.evaluate
      :refer [case-env]])
#?(:cljs
  (:require-macros
    [quantum.core.convert :as self]))
#?(:clj
  (:import
    [org.apache.commons.codec.binary Base64]
    [quantum.core.data.streams    ByteBufferInputStream]
    [clojure.tools.reader.reader_types IPushbackReader]
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
                                 BufferedInputStream BufferedOutputStream
                                 StringWriter
                                 DataInputStream
                                 InputStream OutputStream
                                 ObjectInputStream ObjectOutputStream
                                 IOException EOFException
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
    [java.sql                    Blob Clob])))

; TO EXPLORE
; http://java-performance.info/various-methods-of-binary-serialization-in-java/
; Look at this to learn more about writing and reading byte-buffers
; ===================

(log/this-ns)

#?(:clj
(defaliases pconv
  ->boolean
  ->byte* ->byte
  ->char* ->short
  ->int* ->int
  ->long* ->long
  ->float* ->float
  ->double* ->double
  ->boxed ->unboxed ->unsigned
  ubyte->byte ushort->short uint->int ulong->long))

        (defalias utf8-string   conv/utf8-string  )
        (defalias base64-encode conv/base64-encode)
        (defalias base64-decode conv/base64-decode)
        (defalias base64->bytes base64-decode     ) ; kind of
#?(:clj (defalias ->bytes       arr/->bytes       ))
#?(:clj (defalias bytes->longs  arr/bytes->longs  ))

(defnt ->regex
  ([^string? s] (-> s str/conv-regex-specials re-pattern))
  ([^regex?  r] r))

; ===== (DE)SERIALIZATION ===== ;

(defn transit->
  "Transit decode an object from @x."
  ([x] (transit-> x :json))
  ([x type] (transit-> x type nil))
  ([x type opts]
    #?(:clj  (with-open [in (java.io.ByteArrayInputStream. (.getBytes ^String x))]
               (-> (t/reader (java.io.BufferedInputStream. in) type opts)
                   (t/read)))
       :cljs (-> (t/reader type opts)
                 (t/read x)))))

(defn ->transit
  "Transit encode @x into a String."
  ([x] (->transit x :json))
  ([x type] (->transit x type nil))
  ([x type opts]
    #?(:clj  (with-open [out (java.io.ByteArrayOutputStream.)]
               (-> (t/writer (java.io.BufferedOutputStream. out) type opts)
                   (t/write x))
               (.toString out))
       :cljs (-> (t/writer type opts)
                 (t/write x)))))

(defalias json-> json/json->)
(defalias ->json json/->json)

(defn ->path
  "Creates a forward-slash-delimited path from the @`args`."
  [& args] (apply quantum.core.string/join-once "/" args))

#?(:cljs
(defn bytes->base64
  {:todo ["This is an extremely inefficient algorithm"]}
  [x]
  (js/btoa (.apply js/String.fromCharCode nil x))))

#?(:cljs (defn base64->forge-bytes [x] (js/forge.util.binary.base64.decode x)))
#?(:cljs (defn forge-bytes->base64 [x] (js/forge.util.binary.base64.encode x)))

; TODO test how to use these
#?(:cljs (defn ?->utf-8   [x] (js/forge.util.encodeUtf8 x)))
#?(:cljs (defn utf-8->?   [x] (js/forge.util.decodeUtf8 x)))

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

(defn ->form
  [x & [opts]]
  (cond
    (string? x)
    (r-edn/read-string opts x)
    #?@(:clj [(or (instance? IPushbackReader        x)
                  (instance? java.io.PushbackReader x))
              (r-edn/read opts x)])
    :else x))

; TODO incorporate conversion functions at end of (clojure|cljs).tools.reader.reader-types

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

#?(:clj
(defnt ->uuid*
  ([^string? id] (java.util.UUID/fromString        id))
  ([^bytes?  id] (java.util.UUID/nameUUIDFromBytes id))
  ([^long msb lsb]
     (java.util.UUID. msb ^long lsb))))

#?(:clj
(defmacro ->uuid
  "Because 'IllegalArgumentException Definition of function ->uuid-protocol
            in protocol __GT_uuidProtocol must take at least one arg'"
  [& args]
  (if (empty? args)
      `(u/>uuid)
      `(->uuid* ~@args))))

#?(:clj (defalias ->file path/->file))
#?(:clj (defalias ->uri  path/->uri ))
#?(:clj (defalias ->url  path/->url ))

#?(:clj
(defnt ^java.net.InetAddress ->inet-address
  ([^string? x] (InetAddress/getByName x))))

#?(:clj
(defnt ^java.nio.file.Path ->java-path
  ([^java.nio.file.Path x] x)
  ([                    x] (Paths/get ^URI (->uri x)))))

#?(:clj (defalias ->predicate fn/->predicate))

(defnt ->keyword
  ([^keyword? x] x)
  ([^string? x]
    (assert (not (re-find #"(\(|\)|\[|\]|\{|\}|\"|\\|\^|\@|\`|\;|\,)" x))
      "Keyword candidate must not contain characters that would be illegal in a keyword literal")
    (keyword x))
  ([^symbol? x]
    (keyword (namespace x) (name x))))

(declare ->read-stream-protocol)

#?(:clj
(defnt ^BufferedInputStream ->buffered-read-stream
  ([^BufferedInputStream x] x)
  ([^InputStream         x] (if (instance? BufferedInputStream x)
                                x
                                (BufferedInputStream. x)))
  ([                     x] (-> x ->read-stream-protocol ->buffered-read-stream))))

#?(:clj (defalias ->buffered-input-stream ->buffered-read-stream))

(declare ->write-stream-protocol)

#?(:clj
(defnt ^BufferedOutputStream ->buffered-write-stream
  ([^BufferedOutputStream x] x)
  ([^OutputStream         x] (if (instance? BufferedOutputStream x)
                                 x
                                 (BufferedOutputStream. x)))
  ([                      x] (-> x ->write-stream-protocol ->buffered-write-stream))))

#?(:clj (defalias ->buffered-output-stream ->buffered-write-stream))

#?(:clj
(defnt ->buffered
  "Convert @`x` to a buffered InputStream or OutputStream."
  ([^BufferedInputStream  x] x)
  ([^BufferedOutputStream x] x)
  ([^InputStream          x] (->buffered-read-stream  x))
  ([^OutputStream         x] (->buffered-write-stream x))))

#?(:clj
(defnt ^BufferedInputStream ->read-stream
  {:attribution "ztellman/byte-streams"
   :contributors {"Alex Gunnarson" "defnt-ed and added to"}
   :todo         {0 "Is it necessary to buffer all streams?"}}
  (^{:cost 0} [^bytes? ary]
    (-> ary (ByteArrayInputStream.) ->buffered))
  ([^String x]
    (-> x ->bytes ->read-stream))
  (^{:cost 0} [^ByteBuffer buf]
    (-> buf .duplicate (ByteBufferInputStream.) ->buffered)) ; in a different function, .duplicate is not used.
  (^{:cost 0} [^ReadableByteChannel channel]
    (-> channel Channels/newInputStream ->buffered))
  ; Are FileInputStreams already buffered? Supposedly not: http://stackoverflow.com/questions/2882075/what-about-buffering-fileinputstream
  (^{:cost 0} [^File x] (-> x (FileInputStream.) ->buffered))
  #_(^{:cost 0} [(stream-of bytes) s options]
    (let [ps (ps/pushback-stream (get options :buffer-size 65536))]
      (s/consume
        (fn [^bytes ary]
          (ps/put-array ps ary 0 (alength ary)))
        s)
      (s/on-drained s #(ps/close ps))
      (ps/->read-stream ps)))
  #_(^{:cost 0} [(stream-of ByteBuffer) s options]
    (let [ps (ps/pushback-stream (get options :buffer-size 65536))]
      (s/consume
        (fn [^ByteBuffer buf]
          (ps/put-buffer ps (.duplicate buf)))
        s)
      (s/on-drained s #(ps/close ps))
      (ps/->read-stream ps)))
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
      (->buffered in)))))

#?(:clj (defalias ->input-stream ->read-stream))

#?(:clj
(defnt ^DataInputStream ->data-read-stream
  {:attribution "ztellman/byte-streams"
   :contributors {"Alex Gunnarson" "defnt-ed"}}
  ([^DataInputStream x options] x)
  ([x options]
   (-> x (->read-stream options) (DataInputStream.)))))

#?(:clj (defalias ->data-input-stream ->data-read-stream))

#?(:clj
(defnt ^OutputStream ->write-stream
  {:attribution "ztellman/byte-streams"
   :contributors {"Alex Gunnarson" "defnt-ed"}}
  (^{:cost 0} [^WritableByteChannel channel]
    (Channels/newOutputStream channel))
  (^{:cost 0} [^File x] (-> x (FileOutputStream.) ->buffered))))

#?(:clj (defalias ->output-stream ->write-stream))

#?(:clj
(defnt ->serialized
  ([^OutputStream out obj]
    (-> out ->buffered (ObjectOutputStream.) (.writeObject))
    out)))

#?(:clj
(defnt serialized->
  ([^bytes?              x]
    (with-open [in (-> x ->read-stream (ObjectInputStream.))] (.readObject in)))
  ([^InputStream         x] (-> x ->buffered (ObjectInputStream.) (.readObject)))
  ([^BufferedInputStream x] (-> x))))

(declare ->text)

#?(:clj
(defnt ^ByteBuffer ->byte-buffer
  {:attribution  ["ztellman/byte-streams" "ztellman/gloss.core.formats"]
   :contributors {"Alex Gunnarson" "defnt-ed"}
   :todo         {0 "CLJS has byte buffers too"}
   :performance  "In general it is best to allocate direct buffers only when they
                  yield a measurable gain in program performance."}
  ; ===== ztellman/byte-streams =====
  (^{:cost 0} [^ByteBuffer x] x)
  (^{:cost 0} [^bytes? ary] (->byte-buffer ary nil))
  (^{:cost 0} [^bytes? ary opts]
    (if (or (:direct? opts) false)
        (let [len (Array/getLength ary)
              ^ByteBuffer buf (ByteBuffer/allocateDirect len)]
          (.put buf ary 0 len)
          (.position buf 0)
          buf)
        (ByteBuffer/wrap ary)))
  (^{:cost 1} [^string? x] (->byte-buffer x nil))
  (^{:cost 1} [^string? x options]
    (-> x (arr/->bytes options) (->byte-buffer options)))
  (           [^InputStream x] (TODO) #_(-> x ->buffered))
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
  ([^char?       x] (-> x ->text ->byte-buffer))
  ([^number?     x] (-> x ->byte byte-array ->byte-buffer))))

; ;; byte-buffer => vector of byte-buffers
#?(:clj
(defnt ->byte-buffers
  {:attribution "ztellman/byte-streams"
   :contributors {"Alex Gunnarson" "defnt-ed"}}
  (^{:cost 0} [^ByteBuffer buf opts]
    (let [chunk-size (int (:chunk-size opts))]
      (if chunk-size
          (let [lim     (.limit buf)
                indices (range (.position buf) lim chunk-size)]
            (mapv
              (fn [^long i]
                (-> buf
                    .duplicate
                    (.position i)
                    ^ByteBuffer (.limit (min lim (+ i chunk-size)))
                    ; TODO fix this slicing
                    .slice))
              indices))
          [buf])))))

#?(:clj
(defnt ->lbyte-buffers
  "To lazy sequence of byte-buffers"
  {:attribution ["ztellman/byte-streams" "ztellman/gloss.core.formats"]
   :contributors {"Alex Gunnarson" "defnt-ed"}}
  #_(^{:cost 1} [^ReadableByteChannel channel opts]
    (when (.isOpen channel)
      (let [chunk-size (or (:chunk-size opts) 4096)
            direct?    (or (:direct?    opts) false)]
        (lazy-seq
          (when-let [b (proto/take-bytes! channel chunk-size opts)]
            (cons b (convert channel (seq-of ByteBuffer) opts)))))))
  (^{:cost 0} [^File file opts]
    (let [chunk-size (or (:chunk-size opts) (int 2e9))
          writable?  (or (:writable?  opts) false)
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
        #(do (.close raf)
             (.close fc)))))
  ; Cost unknown; probably not 1
  #_([x] (gformats/to-buf-seq x))))

#?(:clj
(defnt' read-channel->write-channel
  {:source "https://thomaswabner.wordpress.com/2007/10/09/fast-stream-copy-using-javanio-channels/"}
  (^WritableByteChannel
    [^ReadableByteChannel in ^WritableByteChannel out]
    (let [^ByteBuffer buffer (ByteBuffer/allocateDirect (* 16 1024))] ; Recommended size
      (while (not= -1 (.read in buffer))
        (.flip buffer)
        (.write out buffer)
        (.compact buffer))
      (.flip buffer)
      (while (.hasRemaining buffer)
        (.write out buffer))
      ; TODO must close out channel in order to flush the data.
      out))))

#?(:clj
(defnt ->byte-channel
  {:attribution "ztellman/byte-streams"
   :contributors {"Alex Gunnarson" "defnt-ed"}}
  (^{:cost 0} [^InputStream input-stream]
    (Channels/newChannel input-stream))
  (^{:cost 0} [^OutputStream output-stream]
    (Channels/newChannel output-stream))
  #_(^{:cost 1.5} [(seq-of byte-buffer) bufs]
    (let [pipe (Pipe/open)
          ^WritableByteChannel sink (.sink pipe)
          source (doto ^AbstractSelectableChannel (.source pipe)
                   (.configureBlocking true))]
      (future
        (try
          (loop [s bufs]
            (when (and (contains? s) (.isOpen sink))
              (let [buf (.duplicate ^ByteBuffer (first s))]
                (.write sink buf)
                (recur (rest s)))))
          (finally
            (.close sink))))
      source))))

; ByteSource : generic byte-source
#?(:clj
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
  (^{:cost 1.5} [^Reader reader opts]
    (let [chunk-size (or (:chunk-size opts) 2048)
          ary (char-array chunk-size)
          sb (StringBuilder.)]
      (loop []
        (let [n (.read reader ary 0 chunk-size)]
          (if (pos? n)
            (do
              (.append sb ary 0 n)
              (recur))
            (.toString sb))))))))

#?(:clj
(defnt ->char-buffer
  {:attribution "ztellman/gloss.core.formats"}
  ([^java.nio.CharBuffer x] x)
  ([            x] (when x (-> x ->char-seq CharBuffer/wrap)))))

(defnt ^String ->text
  "Converts to the rawest String representation possible."
  {:contributors {"Alex Gunnarson"        "defnt-ed"
                  "ztellman/byte-streams" nil
                  "funcool/octet"         nil}
   :todo ["Test these against ->bytes"]}
           ([^string? x        ] x)
         #_([^string? x options] x)
  #?(:clj  ([#{prim? Number}  x] (str x) #_(String/valueOf    x))) ; TODO fix
           ([^datascript.db.DB x] (dt/write-transit-str x))
           ; Commented only until we decide forge is worth keeping
  #_(:cljs ([^js/forge.util.ByteStringBuffer x] (.toString x "utf8")))
  #?(:clj  ([^integer? n radix]
             #?(:clj  (.toString (biginteger n) radix)
                :cljs (.toString n radix))))
           ([^bytes?  x        ] (->text x nil))
           ([^bytes?  x options]
             #?(:clj
                  (let [encoding (get options :encoding "UTF-8")]
                    (String. x ^String (name encoding)))
                :cljs ; funcool/octet.spec.string
                  (let [view     (js/Uint8Array. (.subarray x 0 (lasti x)))
                        encoding (.-fromCharCode js/String)]
                    (.apply encoding nil view))))
           ([^keyword? k] (->text k "/"))
           ([^keyword? k joiner]
             (->> [(namespace k) (name k)]
                  (core/remove empty?)
                  (str/join joiner)))
  #?(:clj  ([^java.net.InetAddress x]
             (if-let [hostName (.getHostName x)]
               hostName
               (.getHostAddress x))))
  #?(:clj  ([^ReadableByteChannel x] (-> x ->buffered-read-stream ->text)))
  #?(:clj  (^{:cost 1} [^CharSequence char-sequence]
             (.toString char-sequence)))
  #?(:clj  ([^Charset x] (.name x)))
           ; Look at Apache Commons Convert to fill in the below code
           ;([^java.sql.Blob x])
           ;([^java.sql.Clob x])
  #_(:clj  ([^java.util.Date x]
             (-> (java.text.SimpleDateFormat. (:calendar time/formats))
                 (.format x))))
  #?(:clj  ([#{java.sql.Date
               java.sql.Timestamp
               java.sql.Time}    x] (.toString x)))
  #?(:clj  ([^java.util.TimeZone x] (.getID x)))
  ; The returned string is referenced to the default time zone.
  #_(:clj  ([^java.util.Calendar x]
             (let [df (java.text.SimpleDateFormat. (:calendar time/formats))]
               (.setCalendar df x)
               (.format df (.getTime x)))))
#_(^{:cost 1} [(vector-of String) strings]
    (let [sb (StringBuilder.)]
      (doseq [s strings]
        (.append sb s))
      (.toString sb)))
  #?(:clj  ([^InputStream in    ] (-> in ->buffered-read-stream (->text))))
  #?(:clj  ([^InputStream in enc] (-> in ->buffered-read-stream (->text enc))))
  #?(:clj  ; CANDIDATE 0
           ([^BufferedInputStream in]
             (->text in (->text (Charset/defaultCharset)))))
  #?(:clj  ([^BufferedInputStream in enc]
             (with-open [bout (StringWriter.)]
               (io/copy in bout :encoding enc)
               (.toString bout))))
           ; CANDIDATE 1
           #_([^java.io.InputStream is]
             (let [^java.util.Scanner s
                     (-> is (java.util.Scanner.) (.useDelimiter "\\A"))]
               (if (.hasNext s) (.next s) "")))
  #?(:clj  ([^ByteArrayInputStream in-stream]
             (let [n   (.available in-stream)
                   arr (byte-array n)]
               (.read in-stream arr, 0 n)
               (String. arr java.nio.charset.StandardCharsets/UTF_8))))
  #?(:clj  ([^File f] (-> f ->buffered-read-stream ->text)))
  ; TODO make this better
  #?(:clj  ([^URL x] (.toString x)))
  ; Port this
#_([x options] (streams/convert x String options))
          )

(defnt ->represent
  "Converts to a(n ostensibly) human-friendly String representation."
  ([^string? x] x)
  ([^default x] x))

#?(:clj
(defnt ->charset
  ([^string? x] (Charset/forName x))))

(defnt ->name
  ([#{string? symbol? keyword?} x] (name x))
  ([^default                    x] (str  x)))

(defnt ->symbol
          ([^string?          x] (symbol x))
  #?(:clj ([^clojure.lang.Var x] (symbol (str (ns-name (.ns x)))
                                         (str (.sym x)))))
          ([^default          x] (-> x ->name ->symbol)))

; Commented only until we decide forge is worth keeping
#_(defnt ->hex
  #?(:cljs ([^js/forge.util.ByteStringBuffer x] (.toHex x)))
  #?(:clj  ([                                x] (TODO))))

#?(:clj
(defnt ^Reader ->reader
  {:attribution "ztellman/byte-streams"
   :contributors {"Alex Gunnarson" "defnt-ed"}}
  (^{:cost 1.5} [^InputStream is opts]
    (BufferedReader. (InputStreamReader. is (static-cast String (or (:encoding opts) "UTF-8")))))))

#?(:clj
(defnt ->read-channel
  {:attribution "ztellman/byte-streams"
   :contributors {"Alex Gunnarson" "defnt-ed"}}
  (^FileChannel         ^{:cost 0} [^File x]
    (let [^FileInputStream in (->read-stream x)] (.getChannel in)))
  (^ReadableByteChannel ^{:cost 0} [^InputStream         x] (-> x ->buffered ->read-channel))
  (^ReadableByteChannel ^{:cost 0} [^BufferedInputStream x] (Channels/newChannel x))))

#?(:clj
(defnt ->write-channel
  (^ReadableByteChannel            [#{string? bytes?}     x] (-> x ->read-stream ->read-channel))
  (^FileChannel         ^{:cost 0} [^File                 x] (->write-channel x nil))
  (^WritableByteChannel ^{:cost 0} [^File                 x opts]
    (.getChannel (FileOutputStream. x (boolean (or (:append? opts) true)))))
  (^WritableByteChannel ^{:cost 0} [^OutputStream         x] (-> x ->buffered ->write-channel))
  (^WritableByteChannel ^{:cost 0} [^BufferedOutputStream x] (Channels/newChannel x))))

#?(:clj
(defnt ^Locale ->locale
  ([^string? x] (Locale. x))))

#?(:clj
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
       (line!)))))

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

#?(:cljs
(defn arr->vec [arr]
  (let [v (transient [])]
    (dotimes [n (alength arr)]
      (conj! v (aget arr n)))
    (persistent! v))))

#?(:cljs
(defn file->u8arr [file]
  (let [ch (async/chan)
        file-reader (js/FileReader.)]
    (set! (.-onload file-reader)
          (fn [e]
            (if-let [file-content e.target.result]
              (async/put!   ch (js/Uint8Array. file-content))
              (async/close! ch))))
    (.readAsArrayBuffer file-reader file)
    ch)))

(defnt ->mdb [^string? x] (dt/read-transit-str x))
