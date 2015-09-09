(ns
  ^{:doc "Cryptographic/encryption functions like hashing and digests.

          Possibly should just alias some better library for this."
    :attribution "Alex Gunnarson"}
  quantum.core.cryptography
  (:refer-clojure :exclude [hash])
  (:require-quantum [ns bytes set logic str type macros convert err coll arr])
  (:require [byte-transforms :as bt])
#?(:clj
  (:import
    [java.security MessageDigest DigestInputStream]
    [java.io       InputStream   ByteArrayInputStream]
    javax.crypto.Mac
    javax.crypto.spec.SecretKeySpec)))

#?(:clj
  (def ^sun.nio.cs.UTF_8 utf-8
    (java.nio.charset.Charset/forName "UTF-8")))

#?(:clj (def available-encoders #{:base64}))

; (encode "abc" :base64)
; Inaccurate: use _ instead of -, etc., and chop off trailing '='
; (:clj (defalias encode bt/encode))
; (:clj (defalias decode bt/decode))

(defn encode [^"[B" bytes- k]
  (condp = k
    :base64
      (.encode (java.util.Base64/getEncoder) bytes-)
    (throw+ (Err. nil "Unrecognized codec" k))))

(defn decode [^"[B" bytes- k]
  (condp = k
    :base64
      (.decode (java.util.Base64/getDecoder) bytes-)
    (throw+ (Err. nil "Unrecognized codec" k))))

#?(:clj
(defn ^bytes digest
  "Creates a byte digest of the string @s according to the @md-type algorithm.
   Assumes UTF-8 encoding for string passed.

   WARNING:
     CMU Software Engineering Institute now says that MD5
     'should be considered cryptographically broken and
      unsuitable for further use.'
     Most U.S. government applications now require the SHA-2
     family of hash functions."
  {:todo "Allow more hashing functions"}
  [^Keyword hash-type ^String s]
  {:pre [(splice-or hash-type = :md5 :sha-256)]}
  (let [^String            hash-type-str
          (-> hash-type name str/upper-case)
        ^MessageDigest     md
          (MessageDigest/getInstance hash-type-str)
        ^InputStream       in (convert/->input-stream s)
        ^DigestInputStream dis
          (DigestInputStream. in md)
        ^bytes             digested (.digest md)]
    (.close in)
    (.close dis)
    digested)))

#?(:clj
  (defn ^String hex-digest
    "Gets the digest of a string in hexadecimal.
     Assumes UTF-8 encoding for string passed."
    [^Key hash-type ^String s]
    (->> s
         (digest hash-type)
         bytes/bytes-to-hex)))

#?(:clj
(def available-hashers
  (set/union #{:crc32 :crc64
               :md2 :md5
               :sha1 :sha256 :sha-256-hmac :sha384 :sha512
               :murmur32 :murmur64 :murmur128
               :adler32}
    (into #{} (bt/available-hash-functions)))))

#?(:clj
(defn sha-256-hmac ^"[B" [message secret]
  (throw-unless (and message secret)
    (Err. nil "Neither message nor secret can be nil" (kmap message secret)))
  (let [^Mac algo-instance (Mac/getInstance "HmacSHA256")
        ^SecretKey secret-key (SecretKeySpec. (arr/->bytes secret) "HmacSHA256")]
    (.init    algo-instance secret-key)
    (.doFinal algo-instance (arr/->bytes message)))))

(defn hash
  ([bytes         ] (hash bytes :murmur64 nil))
  ([bytes function] (hash bytes function  nil))
  ([bytes function options]
    (condp = function
      :sha-256-hmac (sha-256-hmac bytes (:secret options))
      (bt/hash bytes function options))))

#?(:clj (defalias hash= bt/hash=))





; (import '(java.io FileInputStream FileOutputStream OutputStream))
; (import '(java.security KeyStore KeyFactory))
; (import 'java.security.cert.Certificate)
; (import 'javax.crypto.spec.SecretKeySpec)
; (import 'java.security.spec.PKCS8EncodedKeySpec)

; (let [^String filePathToStore (io/parse-dirs-keys [:resources "ssl-keys.cljx"])
;       ^"[C" password (.toCharArray "password")
;       ^KeyStore ks  (KeyStore/getInstance "JKS")
;       ^KeyFactory kf (KeyFactory/getInstance "RSA")
;       pk-bytes (.getBytes "SECRETS")
;       ^PrivateKey pk (.generatePrivate kf (PKCS8EncodedKeySpec. pk-bytes));
;       _ (.load ks nil password)

;       secret-key (SecretKeySpec. pk-bytes, 0, (count "SECRETS"), "AES")
;       _ (.setKeyEntry ks "keyAlias" pk password, nil)
;       ^OutputStream fos (FileOutputStream. filePathToStore)
;      ]
;       (.store  ks fos, password)
;      (.close fos))


; (let [^String keystoreFilename (io/parse-dirs-keys [:resources "ssl-keys.cljx"])
;       ^"[C" password (.toCharArray "password")
;       ^String alias0 "alias"
;       ^FileInputStream fin (FileInputStream. keystoreFilename)
;       ^KeyStore keystore (KeyStore/getInstance "JKS")
;       _ (.load keystore fin password)
;       ^Certificate cert (.getCertificate keystore alias0)]
;       (println cert)
;       (.close fin))
