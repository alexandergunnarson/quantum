(ns
  ^{:doc "URL decoding, encoding, and URL <-> map.

          Possibly should deprecate this in favor of some other
          better library.

          Call this quantum.http.transcode"
    :attribution "Alex Gunnarson"}
  quantum.net.url
  (:require
    [quantum.net.http         :as http]
    [quantum.core.string      :as str]
    [quantum.core.fn
      :refer [<- rfn]]
    [quantum.core.macros
      :refer [defnt]]
    [quantum.core.collections :as coll
      :refer [join reducei mergel map+]]))

(def url-percent-codes
  {:common
     {"%0A" "\n"
      "%0D" "\n"
      "%20" " "
      "%22" "\""
      "%25" "%"
      "%2D" "-"
      "%2E" "."
      "%3C" "<"
      "%3E" ">"
      "%5C" "\\"
      "%5E" "^"
      "%5F" "_"
      "%60" "`"
      "%7B" "{"
      "%7C" "|"
      "%7D" "}"
      "%7E" "~"}
   :reserved
     {"%21" "!"
      "%23" "#"
      "%24" "$"
      "%26" "&"
      "%27" "'"
      "%28" "("
      "%29" ")"
      "%2A" "*"
      "%2B" "+"
      "%2C" ","
      "%2F" "/"
      "%3A" ":"
      "%3B" ";"
      "%3D" "="
      "%3F" "?"
      "%40" "@"
      "%5B" "["
      "%5D" "]"}})


; TODO extend this
(def xml-codes
  {"&amp;"   "&"
   "\\u0026" "&"
   "&lt;"    "<"
   "&gt;"    ">"})

(defn decode
  {:todo ["Determine whether it's been double-encoded"]}
  [code-map-key s]
  (if (= code-map-key :all)
      (->> s
           (decode :url-reserved)
           (decode :url-common)
           (decode :xml))
      (let [code-map
              (case code-map-key
                :url-reserved (-> url-percent-codes :reserved)
                :url-common   (-> url-percent-codes :common)
                :xml          xml-codes)]
        (reduce
          (rfn [ret percent-code assoc-char]
            (str/replace ret percent-code assoc-char))
          s
          code-map))))

(defn encode [s]
  (let [encoding-map
         (-> (mergel
               (coll/reverse-kvs (:common   url-percent-codes))
               (coll/reverse-kvs (:reserved url-percent-codes)))
             (dissoc "." "-" "%"))]
    (reduce
      (rfn [ret char-n percent-code]
        (str/replace ret char-n percent-code))
      (str/replace s "%" "%25") ; pre-replace all %
      encoding-map)))

(defn url-params->map
  [str-params & [decode?]]
  (let [decode-if-necessary
        (fn [params]
          (if decode?
              (map+ (partial decode :all) params)
              params))]
    (->> str-params
         (#(if decode? (decode :xml %) %))
         (<- str/split #"&")
         decode-if-necessary
         (map+
           (fn [param]
             (->> param
                  (coll/split-remove "="))))
         join)))

(defn embedded-url->map
  [^String embedded-url]
  (->> embedded-url
       (decode :xml)
       (<- url-params->map true)))

(defn url->map [url]
  (let [[url str-params]
          (->> url
               (decode :all)
               (coll/split-remove "?"))
        params
         (-> str-params (url-params->map true))]
    {:url          url
     :query-params params}))

(defnt normalize-param
  ([^keyword? x] (-> x name normalize-param))
  ([^string?  x] (-> x encode))
  ([^number?  x] (-> x str normalize-param)))

(defn map->str [m]
  (reducei
    (fn internal
      ([s [k v] n] (internal s k v n))
      ([s k v n]
        (let [ampersand* (when (> n 0) "&")]
          (str s ampersand* (name k) "=" (normalize-param v)))))
    ""
    m))

(defn map->url [url m]
  (str url "?" (map->str m)))
