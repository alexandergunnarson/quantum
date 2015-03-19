(ns
  ^{:doc "URL decoding, encoding, and URL <-> map.
          
          Possibly should deprecate this in favor of some other
          better library."
    :attribution "Alex Gunnarson"}
  (:require
    [quantum.core.ns  :as ns :refer :all]
    [quantum.http.core :as http])
  quantum.http.url
  (:gen-class))

(ns/require-all *ns* :lib :clj)

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
   :extended
     {"%2521" "!" 
      "%2523" "#" 
      "%2524" "$"
      "%2526" "&"
      "%2527" "'"
      "%2528" "("
      "%2529" ")"
      "%252A" "*"
      "%252B" "+"
      "%252C" ","
      "%252F" "/"
      "%253A" ":"
      "%253B" ";"
      "%253D" "="
      "%253F" "?"
      "%2540" "@"
      "%255B" "["
      "%255D" "]"}
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
   "\\u0026" "&"})

(defn decode [^Keyword code-map-key ^String s]
  (if (= code-map-key :all)
      (->> s
           (decode :url-extended)
           (decode :url-reserved)
           (decode :url-common)
           (decode :xml))
      (let [^Map code-map
              (condp = code-map-key
                :url-reserved (-> url-percent-codes :reserved)
                :url-common   (-> url-percent-codes :common)
                :url-extended (-> url-percent-codes :extended)
                :xml          xml-codes)]
        (reduce+
          (fn [ret percent-code assoc-char]
            (str/replace ret percent-code assoc-char))
          s
          code-map))))

(defn ^Map url-params->map
  [^String str-params & [decoded?]]
  (let [^Fn decode-if-necessary
        (fn [^Vec params]
          (if decoded?
              (map+ (partial decode :all) params)
              params))]
    (->> str-params
         (<- str/split #"&")
         decode-if-necessary
         (map+
           (fn [^String param]
             (->> param
                  (<- split-remove+ "="))))
         redm)))
  
(defn embedded-url->map
  [^String embedded-url]
  (->> embedded-url 
       (decode :xml)
       (<- url-params->map true)))

(defn url->map [^String url]
  (let [^Vec [^String url ^String str-params]
          (->> url
               (decode :all)
               (<- split-remove+ "?"))
        ^Map params
         (-> str-params (url-params->map true))]
    {:url          url
     :query-params params}))

