(ns quantum.core.io.compress
  (:refer-clojure :exclude
    [for doseq contains?
     repeat repeatedly
     range
     merge
     count
     vec
     reduce into
     first second rest last butlast get pop peek]))

#?(:clj
(ns
  ^{:attribution "Alex Gunnarson"
    :doc
      "Byte compression. Perhaps this would better go in quantum.core.data.bytes.compression?

       Currently supports .zip; it may be a slow solution.

       Perhaps it would be better simply to alias from, say, org.apache.commons.io.FileUtils?"
    :todo ["Extend functionality to all compression formats: .zip, .gzip, .tar, .rar, etc."]}
  quantum.core.io.compress
  (:refer-clojure :exclude
    [for doseq contains?
     repeat repeatedly
     range
     merge
     count
     vec
     reduce into
     first second rest last butlast get pop peek])
  (:require
    [quantum.core.io.core          :as io]
    [quantum.core.ns :as ns
      #?@(:clj [:refer [defalias alias-ns]])                    ]
    [quantum.core.data.array       :as arr  :refer :all         ]
    [quantum.core.error            :as err  :refer :all         ]
    [quantum.core.string           :as str                      ]
    [quantum.core.time.core        :as time                     ]
    
    [quantum.core.collections      :as coll :refer :all         ]
    [quantum.core.numeric          :as num  :refer [greatest-or]]
    [quantum.core.logic                     :refer :all         ]
    [quantum.core.type                      :refer :all
      :exclude [seq? vector? set? map? string? associative? keyword? nil? list? coll? char?]         ]
    [quantum.core.function                  :refer :all         ]
    
    [quantum.core.error            :as err
      #?@(:clj [:refer [try+ throw+]])                          ]
    [clojure.java.io               :as clj-io                   ]
    [taoensso.nippy                :as nippy                    ]
    [quantum.core.io.serialization :as io-ser                   ]
    [iota                          :as iota                     ])
  #?@(:clj
     [(:import
       (java.io File FileNotFoundException PushbackReader
         FileReader DataInputStream DataOutputStream IOException
         OutputStream FileOutputStream BufferedOutputStream BufferedInputStream
         InputStream  FileInputStream
         PrintWriter)
       (java.util.zip ZipOutputStream ZipEntry)
       java.util.List
       org.apache.commons.io.FileUtils)
     (:gen-class)])))

#?(:clj
  (defn fast-file-zip
    {:attribution "Alex Gunnarson, ported from Java from an unknown site"
     :todo ["See if this is actually is fast... there are likely quite faster methods available."]}
    [^String in ^String out]
    (let [^ZipOutputStream zos
           (->> (FileOutputStream. ^String out)
                (BufferedOutputStream.)
                (ZipOutputStream.))]
      (try
        (doseq [^File file-n (-> in (File.) file-seq rest)] ; for all the files/folders in the folder,
          (let [file-path (.getAbsolutePath file-n)
                bis (->> file-path
                         (FileInputStream.)
                         (BufferedInputStream.))]
            (try
              (let [data-to-write (byte-array 1024)
                    length (.read bis data-to-write)]
                (.putNextEntry zos (ZipEntry. file-path))
                (while (length > 0)
                  (.write zos data-to-write 0 length)))
             (finally
               (.closeEntry zos)
               (.close      bis)))))
        (finally
          (.finish zos)
          (.close  zos))))))

#?(:clj
  (defn getAllFiles
    {:attribution "Ported from http://www.avajava.com/tutorials/lessons/how-do-i-zip-a-directory-and-all-its-contents.html"
     :todo "The efficiency of this solution is untested."}
    [^File dir ^List fileList]
    (doseq [^File file (.listFiles dir)]
      (.add fileList file)
      (if (.isDirectory file)
          (do (getAllFiles file fileList))))))

#?(:clj
  (defn addToZip
    {:todo "The efficiency of this solution is untested."}
    [^File directoryToZip ^File file ^ZipOutputStream zos]
    ; we want the zipEntry's path to be a relative path that is relative
    ; to the directory being zipped, so chop off the rest of the path
    (let [^FileInputStream fis (FileInputStream. file)
          ^String zipFilePath
           (-> file .getCanonicalPath
               (.substring
                 (-> directoryToZip .getCanonicalPath .length inc)
                 (-> file .getCanonicalPath .length)))]
    ;(println (str "Writing '" zipFilePath  "' to zip file"))
    (.putNextEntry zos (ZipEntry. zipFilePath))
  
    (let [bytes-0 (byte-array 1024)]
      (loop [length (.read fis bytes-0)]
        (when (>= length 0)
          (.write zos bytes-0 0 length)
          (recur (.read fis bytes-0)))))
    (.closeEntry zos)
    (.close fis))))

#?(:clj
  (defn writeZipFile
    {:todo "The efficiency of this solution is untested."}
    [^File directoryToZip ^List fileList]
    (println "========")
    (println "Writing .zip to: " (str (.getAbsolutePath directoryToZip) ".zip"))
    (println "========")
    (let [^FileOutputStream fos (FileOutputStream. (str (.getName directoryToZip) ".zip"))
          ^ZipOutputStream  zos (ZipOutputStream. fos)]
          (doseq [^File file fileList]
            (when (not (.isDirectory file))  ; we only zip files, not directories
              (addToZip directoryToZip file zos)))
          (.close zos)
          (.close fos))))

#?(:clj
  (defn zip
    {:todo "The efficiency of this solution is untested."}
    [^File directoryToZip]
    (let [fileList (array-list)]
      (println "---Getting references to all files in:"
        (.getCanonicalPath directoryToZip))
      (getAllFiles directoryToZip fileList)
      (println "---Creating zip file")
      (writeZipFile directoryToZip fileList)
      (println "---Done"))))


