(ns quantum.core.io.utils
          (:require
            [com.stuartsierra.component  :as component]
    #?(:clj [clojure.java.io             :as io])
            [quantum.core.convert        :as conv
              :refer [->name]]
            [quantum.core.collections    :as coll
              :refer [kmap]]
            [quantum.core.error          :as err
              :refer [->ex]]
            [quantum.core.fn             :as fn
              :refer [fn1]]
            [quantum.core.system         :as sys]
            [quantum.core.string         :as str]
            [quantum.core.logic          :as logic
              :refer [fn-not fn-and ifn]]
            [quantum.core.macros         :as macros
              :refer [defnt]]
            [quantum.core.paths          :as paths
              :refer [#?(:clj ->file)]])
  #?(:clj (:import
            (java.io File
                     InputStream OutputStream
                     DataOutputStream
                     FileInputStream FileOutputStream))))

; ===== DEPENDENCIES =====

(def ill-chars-table
  {"\\" "-", "/" "-", ":" "-", "*" "!", "?" "!"
   "\"" "'", "<" "-", ">" "-", "|" "-"})

(defn escape-illegal-chars
  "Escapes illegal characters in filename."
  {:todo ["Make less naive - Mac vs. Windows, etc."]}
  [str-0]
  (reduce-kv
    (fn [str-n k v] (str/replace str-n k v))
    str-0 ill-chars-table))

#?(:clj
(defnt readable?
  ([^string? dir]
    (try (->> dir (.checkRead (SecurityManager.)))
         true
      (catch SecurityException _ false)))
  ([^file?   dir] (->> dir str       readable?))
  ([^+vec?   dir] (->> dir paths/parse-dir readable?))))

#?(:clj
(defnt writable?
  ([^string? dir]
    (try (->> dir (.checkWrite (SecurityManager.)))
         true
      (catch SecurityException _ false)))
  ([^file?   dir] (->> dir str writable?))))

#?(:clj
(defn create-dir! [dir-0]
  (let [dir (-> dir-0 ->file)]
    (when-not (paths/contains? dir) ; exists?
      (try (writable? dir) ; TODO assert this
           (assert (.mkdir ^File dir) #{dir})
        (catch Throwable t
          (throw
            (->ex :mkdir "The directory could not be created." (kmap t dir)))))))))

(defn num-to-sortable-str [num-0]
  (ifn num-0 (fn-and (fn-not neg?) (fn1 < 10))
       (partial str "0")
       str))

#?(:clj
(defnt ^String path->file-name
  ([^file?   f] (.getName f))
  ([^string? s] (coll/taker-until sys/separator nil s))))

#_(defn next-file-copy-num [path-0]
  (let [extension (file-ext path-0)
        file-name (-> path-0 path->file-name)]
    (try
      (->> path-0
           siblings
           (map+ str)
           (filter+
             (partial
               (fn-and
                 (fn-> file-name* (str/starts-with? file-name))
                 (fn-> file-ext (= extension)))))
           (map+ (fn-> file-name*
                       (str/replace (str file-name " ") "")
                       path-without-ext str/val))
           (filter+ number?)
           redv num/greatest inc num-to-sortable-str)
      (catch Exception _ (num-to-sortable-str 1)))))



; ===== EXTENSIONS =====

#?(:clj
(defn create-temp-file!
  [^String file-name ^String suffix & [^File dir]]
  (if dir
      (do (create-dir! dir)
          (File/createTempFile file-name suffix dir))
      (File/createTempFile file-name suffix))))

#?(:clj
(defmacro with-temp-file
  "Evaluates @body with a temporary file in its scope."
  {:attribution "From github.com/bevuta/pepa.util"}
  [[name data suffix dir] & body]
  (let [name' (with-meta name {:tag 'java.io.File})]
    `(let [data# ~data
           ~name' (create-temp-file! "temp_" (or ~suffix "") (when ~dir (->file ~dir)))]
       (try
         (when data#
           (io/copy data# ~name'))
         ~@body
         (finally
           (.delete ~name')))))))

#?(:cljs
(defn file-reader
  ([] (file-reader nil))
  ([{:keys [on-load on-load-end] :as opts}]
    (let [reader (js/FileReader.)]
      (when on-load     (set! (.-onload    reader) on-load))
      (when on-load-end (set! (.-onloadend reader) on-load-end))
      reader))))

(defrecord ^{:doc "Cross-platform abstraction over java.io.File and js/File"}
  ByteEntity [type name])

; Files.probeContentType(path)
;#?(:cljs (defnt get-type ([^file? x] (#?(:clj ? :cljs .-type) x)))) ; This works only if File is declared
;#?(:cljs (defnt get-name ([^file? x] (#?(:clj ? :cljs .-name) x)))) ; This works only if File is declared

;#?(:cljs (defnt ->byte-entity ; This works only if File is declared
;  ([^file? x]
;  (map->ByteEntity {:type (get-type x) :name (get-name x)}))))
