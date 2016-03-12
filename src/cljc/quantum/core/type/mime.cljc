(ns quantum.type.mime
  (:refer-clojure :exclude [type])
  (:require-quantum [:core]))

#?(:cljs
(defn type
  {:source ["https://en.wikipedia.org/wiki/List_of_file_signatures"
            "https://mimesniff.spec.whatwg.org/#matching-an-image-type-pattern"]}
  [file]
  (let [file-reader (js/FileReader.)
        header (atom nil)]
    (set! (.-onloadend file-reader)
      (fn [e]
        (when-let [result (-> e .-target .-result)]
          (let [arr (-> result (js/Uint8Array.) (.subarray 0 4))]
            (reset! header
              (reduce
                (fn [ret elem]
                  (str ret (.toString elem 16)))
                ""
                (array-seq arr)))))))
    ; Check the file signature against known types
    (.readAsArrayBuffer file-reader file)
    (condpc = header
      "89504e47" :image/png
      "47494638" :image/gif
      (coll-or "ffd8ffe0" "ffd8ffe1" "ffd8ffe2") :image/jpeg
      (.-type file))) 
))