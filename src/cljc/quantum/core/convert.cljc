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