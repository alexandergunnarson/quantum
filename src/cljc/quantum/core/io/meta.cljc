(ns ^{:doc "Metadata extraction and parsing for files."}
  quantum.core.io.meta
  (:require-quantum [:core fn logic str macros coll io res])
  (:require [quantum.core.io.utils :as iou]
            [quantum.core.process :as proc])
#?(:clj (:import
          (org.apache.tika.parser   AutoDetectParser  )
          (org.apache.tika.sax      BodyContentHandler)
          (org.apache.tika.metadata Metadata          ))))

(def- parsers 
  (let [bit-rate-parser (fn-> (str/remove "Kbps") str/trim str/val)]
    {:frame-rate       (fn->> (dropr 4) str/val)
     :format           str/keywordize
     :maximum-bit-rate bit-rate-parser
     :bit-rate         bit-rate-parser
     :bit-rate-mode    str/keywordize
     :channels         (fn-> (str/remove "channels") str/trim str/val)}))

(def- rekey-map {(keyword "bits/(pixel*frame)") :bits-per-pixel*frame
                 (keyword "channel(s)"        ) :channels})

(defn- parse-media-metadata
  "Parses metadata output by MediaInfo process."
  [meta-]
  (let [section (volatile! nil)]
    (->> meta-
         (<- str/split #"\n")
         (map+ (f*n str/split #": "))
         force
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
  (-> (proc/exec! "mediainfo" file)
      :out
      parse-media-metadata)))

(def media-exts #{:mp4})

#?(:clj
(defn file->meta:tika [file-str]
  (with-resources [stream (FileInputStream. file-str)]
    (let [parser   (AutoDetectParser.)
          handler  (BodyContentHandler.)
          metadata (Metadata.)]
      (.parse parser stream handler metadata)
      (reduce (fn [ret k]
        (assoc ret (keyword k) (into [] (.getValues metadata k))))
        {}
        (->> metadata .names (into [])))))))

(defalias file->meta file->meta:tika)

#_#?(:clj
(defn file->meta
  "Uses org.apache.tika.parser.AutoDetectParser to parse metadata for a file."
  {:in '[[:resources "Music Library" "1.mp4"]]}
  [file]
  (let [file-str (iou/file-str file)]
    (if (-> file-str iou/extension str/keywordize (in? media-exts))
        (media-extract file-str)
        (extract:tika file-str)))))

