(ns quantum.core.io.transcode)

; TODO the logic in here is good, but it requires the |sh| dep which is at the moment incomplete/broken  

(comment "https://trac.ffmpeg.org/wiki/Encode/AAC
​Advanced Audio Coding (AAC) is the successor format to MP3.
It is defined in MPEG-4 part 3 (ISO/IEC 14496-3).
It is often used within an MP4 container format;
for music the .m4a extension is customarily used. 


https://trac.ffmpeg.org/wiki/Encode/HighQualityAudio
Container Audio formats supported
MKV/MKA Vorbis, MP2, MP3, LC-AAC, HE-AAC, WMAv1, WMAv2, AC3, eAC3, Opus
MP4/M4A MP2, MP3, LC-AAC, HE-AAC, AC3
FLV/F4V MP3, LC-AAC, HE-AAC
3GP/3G2 LC-AAC, HE-AAC
MPG MP2, MP3
PS/TS Stream  MP2, MP3, LC-AAC, HE-AAC, AC3
M2TS  AC3, eAC3
VOB MP2, AC3
RMVB  Vorbis, HE-AAC
WebM  Vorbis, Opus
OGG Vorbis, Opus

libfdk_aac
The Fraunhofer FDK AAC codec library. This is currently the highest-quality
AAC encoder available with ffmpeg")


#_(defn ->mp4-x265
  "ffmpeg static mac: http://evermeet.cx/ffmpeg/
   ffmpeg static windows: https://ffmpeg.zeranoe.com/builds/

   The CRF of 28 should visually correspond to libx264 video at CRF 23,
   but result in about half the file size.
   crf=28 is the recommended preset."
  {:in [[:resources "Music Library" "1.mp4"]
        [:resources "Music Library" "x265" "1.mp4"]
        {:write-streams? true :err-buffer sb-err :std-buffer sb-std
         :output-line}]}
  ([in out] (->mp4-x265 in out #{:encode-audio?}))
  ([in out {:keys [encode-audio?] :as opts}]
    (async {:id (-> 'mp4->x265 gensym keyword) :type :thread}
      (let [audio-args
             (if encode-audio?
                   ["libfdk_aac"
                    "-strict" "experimental"
                    "-b:a" "128k"
                    "-cutoff" "18000"]
                   ["copy"])
            args
              (catvec ; CATVEC
                ["-i" (io/file-str in)
                 "-c:v" "libx265"
                 "-preset" "medium"
                 "-x265-params" "crf=28"
                 "-c:a"]
                 audio-args
                 [(io/file-str out)])]
      (sh/run-process! 
        (:ffmpeg paths/paths)
        args
        opts)))))

#_(defn ->mp4
  ([in-path out-path] (->mp4 in-path out-path nil))
  ([in-path out-path opts]
    (let [platform-opts
            (condp = sys/os
              :unix
                ["-codec:a" "libfaac"]
              :windows
                ["-codec:a" "aac" 
                 "-strict"  "experimental"])
          args-0
           ["-i" (io/file-str in-path)
            "-threads" (or (:threads opts) (condp = sys/os :unix 2 :windows 6))
            "-codec:v" "libx264" "-crf" 23 ; average
            platform-opts
            "-qscale:a" 100
            (io/file-str out-path)]
          args (->> args-0 flatten (into []))]
      (sh/run-process! "ffmpeg"
         args
         (->> {:print-output :ffmpeg-convert}
              (merge-keep-left opts)
              (<- assoc :read-streams? true))))))


#_(:clj
(defn extract-audio!
  ([in out] (extract-audio in out nil))
  ([in out opts]
  (let [args
         ["-i" (io/file-str in)
          "-vn"
          "-acodec" "'copy'"
          (io/file-str out)]]
      (sh/run-process! 
        (:ffmpeg paths/paths)
        args
        opts)))))

#_(defalias ->audio extract-audio!)

#_(:clj
(defn duration-of
  "Checks the duration of a video for the given @url.
   Returns value in milliseconds."
  [^String url]
  (let [buffer (StringBuffer. 400)
        _ (sh/run-process! "ffprobe" 
            ["-show_entries" "format=duration"
             "-of" "default=noprint_wrappers=1:nokey=1"
             url]
            {:write-streams? true :std-buffer buffer})
        n (-> buffer str str/val)]
  (throw-unless (number? n) (->ex :not-a-number "Duration is not a number!" n)) 
  (-> n (* 1000)))))