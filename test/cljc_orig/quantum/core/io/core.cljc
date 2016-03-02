(ns
  ^{:doc "I/O operations. Path parsing, read/write, serialization, etc.

          Perhaps it would be better to use, say, org.apache.commons.io.FileUtils
          for many of these things."}
  quantum.core.io.core
  (:refer-clojure :exclude [read descendants])
  (:require-quantum [ns macros arr pr str time coll num logic type fn sys err log vec])
  #?(:clj
      (:require
        [clojure.java.io               :as io    ]
        [taoensso.nippy                :as nippy ]
        [quantum.core.io.serialization :as io-ser]
        [iota                          :as iota  ]))
  #?(:clj
      (:import
        (java.io File
                 FileNotFoundException IOException
                 FileReader PushbackReader
                 DataInputStream DataOutputStream 
                 OutputStream FileOutputStream
                 BufferedOutputStream BufferedInputStream
                 InputStream  FileInputStream
                 PrintWriter)
        (java.util.zip ZipOutputStream ZipEntry)
        java.util.List
        org.apache.commons.io.FileUtils)))

;(require '[clojure.data.csv :as csv])

#?(:clj (do

(defnt ^String path->file-name
  ([^file?   f] (.getName f))
  ([^string? s] (coll/taker-until-workaround sys/separator nil s)))

(defalias file-name* path->file-name)


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

; TODO move
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
;========================================================{ FILES AND I/O  }=========================================================
;========================================================{                }==========================================================


(defalias input-stream  io/input-stream )
(defalias resource      io/resource     )
(defalias output-stream io/output-stream)
(defalias copy!         io/copy         )


))
