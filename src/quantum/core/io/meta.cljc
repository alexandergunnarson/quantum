(ns ^{:doc "Metadata extraction and parsing for files."}
  quantum.core.io.meta
        (:refer-clojure :exclude [reduce contains?])
        (:require
          [quantum.core.io.utils          :as iou  ]
          [quantum.core.process           :as proc ]
          [quantum.core.string            :as str  ]
          [quantum.core.log               :as log  ]
          [quantum.core.paths             :as paths]
          [quantum.core.error             :as err
            :refer [->ex]]
          [quantum.core.collections       :as coll
            :refer [map+ dropr in? kw-map reduce contains?]]
          [quantum.core.convert           :as conv
            :refer [->keyword]]
          [quantum.core.fn                :as fn
            :refer [<- fn-> fn->> fn1 doto->>]]
          [quantum.core.logic             :as logic
            :refer [whenf]]
          [quantum.core.resources         :as res
            :refer [with-resources]]
          [quantum.core.vars              :as var
            :refer [defalias def-]])
#?(:clj (:import
          (java.io                  File FileInputStream)
          (org.apache.tika.parser   AutoDetectParser    )
          (org.apache.tika.sax      BodyContentHandler  )
          (org.apache.tika.metadata Metadata            ))))

(def- parsers
  (let [bit-rate-parser (fn-> (str/remove "Kbps") str/trim str/val)]
    {:frame-rate       (fn->> (dropr 4) str/val)
     :format           (fn1 str/keywordize)
     :maximum-bit-rate bit-rate-parser
     :bit-rate         bit-rate-parser
     :nominal-bit-rate bit-rate-parser
     :bit-rate-mode    (fn1 str/keywordize)
     :channels         (fn-> (str/remove "channels") str/trim str/val)}))

(def- rekey-map {(keyword "bits/(pixel*frame)") :bits-per-pixel*frame
                 (keyword "channel(s)"        ) :channels})

(defn- parse-media-metadata
  "Parses metadata output by MediaInfo process."
  [meta-]
  (let [section (volatile! nil)]
    (->> meta-
         (<- str/split #"\n")
         (map+ (partial coll/split-remove-match ": "))
         (reduce
           (fn [ret v-0]
             (let [entry? (fn-> count (= 2))
                   k      (-> v-0 first str/trim str/keywordize
                              (whenf (fn->> (get rekey-map)) (fn->> (get rekey-map))))
                   v      (whenf v-0 entry? second)]
               (if (entry? v-0)
                   (let [k-f   k
                         v-f-0 (str/trim v)
                         v-f (if-let [parser (get parsers k-f)]
                               (parser v-f-0)
                               v-f-0)]
                     (assoc-in ret [@section k-f] v-f))
                   (do (vreset! section k)
                       ret))))
           {}))))

#?(:clj
(defn file->meta:mediainfo
  "MediaInfo is a convenient unified display of the most relevant
   technical and tag data for video and audio files."
  {:url "https://mediaarea.net/en/MediaInfo"}
  [file]
  (let [{:as result :keys [err out]} (proc/exec! "mediainfo" file)]
    (log/pr ::debug "Path:" file "Mediainfo result:" result)
    (if (contains? err)
        (throw (->ex "Mediainfo error" {:mediainfo-message err})))
        (-> out parse-media-metadata))))

(def media-exts #{:mp4})

#?(:clj
(defn file->meta:tika [file-str]
  (with-resources [stream (FileInputStream. ^String file-str)]
    (let [parser   (AutoDetectParser.)
          handler  (BodyContentHandler.)
          metadata (Metadata.)]
      (.parse parser stream handler metadata)
      (reduce (fn [ret k]
        (assoc ret (keyword k) (into [] (.getValues metadata ^String k))))
        {}
        (->> metadata .names (into [])))))))

#?(:clj (defalias file->meta file->meta:tika))

#_(:clj
(defn file->meta
  "Uses org.apache.tika.parser.AutoDetectParser to parse metadata for a file."
  {:in '[[:resources "Music Library" "1.mp4"]]}
  [file]
  (let [file-str (iou/file-str file)]
    (if (-> file-str iou/extension str/keywordize (in? media-exts))
        (media-extract file-str)
        (extract:tika file-str)))))

