(ns quantum.core.convert.core
  (:require-quantum [:core err])
  (:require [cognitect.transit    :as t     ]
            [datascript.transit   :as dt    ]
            [datascript.core      :as mdb   ]
            [clojure.string       :as str   ]
            [quantum.core.numeric :as num   ]
   #?(:cljs [cljs.reader                 :refer [read-string]])
   #?(:cljs [goog.crypt.base64    :as base64]))
  #?(:clj (:import (org.apache.commons.codec.binary Base64))))

; TODO type dispatch would be faster with protocols

; TODO THIS IS GOOD
; #?(:clj
; (defn ->char [x]
;   (when x
;     (clojure.core/char x))))

(defn ->name [x]
  (cond (string?  x) (name x)
        (symbol?  x) (name x)
        (keyword? x) (name x)
        :else (str x)))

(defn ->symbol [x]
  (cond (string? x) (symbol x)
    #?@(:clj
       [(var?    x) (symbol (str (ns-name (.ns ^Var x)))
                            (str (.sym ^Var x)))])
        :else (-> x ->name ->symbol)))

(defn ->str [x]
  (cond (instance? datascript.db.DB x)
          (dt/write-transit-str x)
        #?@(:cljs
       [(instance? js/forge.util.ByteStringBuffer x)
          (.toString x "utf8")])
        :else
          (str x)))

(defn ->hex [x]
  (cond #?@(:cljs
       [(instance? js/forge.util.ByteStringBuffer x)
          (.toHex x)])
        :else
          (throw (->ex :not-implemented nil (type x)))))  

; Two useful functions from NfWebCrypto for converting Uint8Arrays to Strings, and Strings to Uint8Arrays: 
;     text2ua:function(s) {
;         var ua = new Uint8Array(s.length);
;         for (var i = 0; i < s.length; i++) {
;             ua[i] = s.charCodeAt(i);
;         }
;         return ua;
;     },

;     ua2text:function(ua) {
;         var s = '';
;         for (var i = 0; i < ua.length; i++) {
;             s += String.fromCharCode(ua[i]);
;         }
;         return s;
;     },
; My own function for converting Uint8Arrays to hex: 
;     ua2hex:function(ua) {
;         var h = '';
;         for (var i = 0; i < ua.length; i++) {
;             h += "\\0x" + ua[i].toString(16);
;         }
;         return h;
;     },
; ArrayBuffer concatenation I wrote and later found on StackOverflow: 
;     var catArray = new Uint8Array(arrayONE.byteLength+arrayTWO.byteLength);
;     catArray.set(new Uint8Array(arrayONE),0);
;     catArray.set(new Uint8Array(arrayTWO), arrayONE.byteLength);


; var ua2b64 = btoa(String.fromCharCode.apply(null, yourUint8Array));
;     var b642ua = new Uint8Array(atob(yourBase64EncodedString).split("").map(function(c) {
;     return c.charCodeAt(0); }));

(defn ->mdb [x]
  (cond (string? x)
        (dt/read-transit-str x)))

(defn utf8-string
  "Returns `bytes` as an UTF-8 encoded string."
  {:from "r0man/noencore"}
  [bytes]
  #?(:clj (String. bytes "UTF-8")
     :cljs (throw (ex-info "utf8-string not implemented yet" bytes))))

(defn base64-encode
  "Returns `s` as a Base64 encoded string."
  {:from "r0man/noencore"}
  [bytes]
  (when bytes
    #?(:clj (String. (Base64/encodeBase64 bytes))
       :cljs (base64/encodeString bytes false))))

(defn base64-decode
  "Returns `s` as a Base64 decoded string."
  {:from "r0man/noencore"}
  [s]
  (when s
    #?(:clj (Base64/decodeBase64 (.getBytes s))
       :cljs (base64/decodeString s false))))

; PARSING

(def byte-scale
  {"B" (num/exp 1024 0)
   "K" (num/exp 1024 1)
   "M" (num/exp 1024 2)
   "G" (num/exp 1024 3)
   "T" (num/exp 1024 4)
   "P" (num/exp 1024 5)
   "E" (num/exp 1024 6)
   "Z" (num/exp 1024 7)
   "Y" (num/exp 1024 8)})

(defn- apply-unit [number unit]
  (if (string? unit)
    (case (str/upper-case unit)
      (case unit
        "M" (* number 1000000)
        "B" (* number 1000000000)))
    number))

(defn- parse-number
  {:from "r0man/noencore"}
  [s parse-fn]
  (if-let [matches (re-matches #"\s*([-+]?[0-9]*\.?[0-9]+([eE][-+]?[0-9]+)?)(M|B)?.*" (str s))]
    #?(:clj
       (try (let [number (parse-fn (nth matches 1))
                  unit (nth matches 3)]
              (apply-unit number unit))
            (catch NumberFormatException _ nil))
       :cljs
       (let [number (parse-fn (nth matches 1))
             unit (nth matches 3)]
         (if-not (js/isNaN number)
           (apply-unit number unit))))))

(defn parse-bytes
  {:from "r0man/noencore"}
  [s]
  (if-let [matches (re-matches #"\s*([-+]?[0-9]*\.?[0-9]+([eE][-+]?[0-9]+)?)(B|K|M|G|T|P|E|Z|Y)?.*" (str s))]
    (let [number (read-string (nth matches 1))
          unit (nth matches 3)]
      (long (* (long (read-string (str (nth matches 1))))
               (get byte-scale (str/upper-case (or unit "")) 1))))))

 
(defn parse-integer
  [s] (parse-number s #(#?(:clj Integer/parseInt   :cljs js/parseInt) %1)))

(defn parse-long
  [s] (parse-number s #(#?(:clj Long/parseLong     :cljs js/parseInt) %1)))

(defn parse-double
  [s] (parse-number s #(#?(:clj Double/parseDouble :cljs js/parseFloat) %1)))

(defn parse-float
  [s] (parse-number s #(#?(:clj Float/parseFloat   :cljs js/parseFloat) %1)))
