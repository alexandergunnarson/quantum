(ns quantum.core.convert.core
  (:require
    [cognitect.transit    :as t]
    [datascript.transit   :as dt]
    [datascript.core      :as mdb]
    [clojure.string       :as str]
#?@(:cljs
   [[cljs.reader
      :refer [read-string]]
    [goog.crypt.base64    :as base64]])
    [quantum.core.fn      :as fn
      :refer [<-]]
    [quantum.core.error   :as err
      :refer [>ex-info]]
    [quantum.core.numeric :as num])
#?(:cljs
  (:require-macros
    [quantum.core.numeric :as num]))
#?(:clj
  (:import
    (org.apache.commons.codec.binary Base64)
    clojure.lang.Var)))

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



(defn utf8-string
  "Returns `bytes` as an UTF-8 encoded string."
  {:from "r0man/noencore"}
  [bytes]
  #?(:clj (String. ^"[B" bytes "UTF-8")
     :cljs (throw (ex-info "utf8-string not implemented yet" bytes))))

(defn base64-encode
  "Returns @s as a Base64 encoded string."
  {:from "r0man/noencore"}
  [bytes]
  (when bytes
    #?(:clj (String. (Base64/encodeBase64 bytes))
       :cljs (base64/encodeString bytes false))))

(defn base64-decode
  "Returns @s as a Base64 decoded string."
  [^String s]
  (when s
    #?(:clj (->> s (<- (.getBytes java.nio.charset.StandardCharsets/ISO_8859_1))
                   (.decode (java.util.Base64/getDecoder)))
       :cljs (base64/decodeString s false))))
