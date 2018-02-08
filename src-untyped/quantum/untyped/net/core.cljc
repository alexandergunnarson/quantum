(ns quantum.untyped.net.core
  (:require
    [clojure.string :as str]
    [quantum.untyped.core.system
      #?@(:cljs [:refer [global]])])
  #?(:clj
  (:import
    (java.net URLEncoder URLDecoder))))

(defn url-encode
  "Returns `s` as an URL encoded string."
  [s & [encoding]]
  (when s
    #?(:clj (-> (URLEncoder/encode (str s) (or encoding "UTF-8"))
                (str/replace "%7E" "~")
                (str/replace "*" "%2A")
                (str/replace "+" "%20"))
       :cljs (-> ((.-encodeURIComponent global) (str s))
                 (str/replace "*" "%2A")))))

(defn url-decode
  "Returns `s` as an URL decoded string."
  [s & [encoding]]
  (when s
    #?(:clj (URLDecoder/decode s (or encoding "UTF-8"))
       :cljs ((.-decodeURIComponent global) s))))
