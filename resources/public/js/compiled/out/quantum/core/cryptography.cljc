#?(:clj (ns quantum.core.cryptography))

(ns
  ^{:doc "Cryptographic/encryption functions like hashing and digests.

          Possibly should just alias some better library for this."
    :attribution "Alex Gunnarson"}

  quantum.core.cryptography

  (:require
    [quantum.core.ns  :as ns #?@(:clj [:refer :all])]
    [quantum.core.data.bytes :as bytes]
    [quantum.core.logic      #?@(:clj [:refer :all])]
    [quantum.core.string     :as str])
  
  #?(:clj
    (:import
      (java.security MessageDigest DigestInputStream)
      (java.io       InputStream   ByteArrayInputStream)))

  #?(:clj (:gen-class)))

#?(:clj (ns/require-all *ns* :clj))

#?(:clj
  (def ^sun.nio.cs.UTF_8 utf-8
    (java.nio.charset.Charset/forName "UTF-8")))

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
    [^Key md-type ^String s]
    {:pre [(splice-or md-type = :md5 :sha-256)]}
    (let [^String md-type-str (-> md-type name str/upper-case)
          ^MessageDigest      md
            (MessageDigest/getInstance md-type-str)
          ^InputStream        in-stream
            (ByteArrayInputStream. (.getBytes s utf-8))
          ^DigestInputStream  dis
            (DigestInputStream. in-stream md)
          ^bytes              digested (.digest md)]
      (.close in-stream)
      (.close dis)
      digested)))

#?(:clj
  (defn ^String hex-digest
    "Gets the digest of a string in hexadecimal.
     Assumes UTF-8 encoding for string passed."
    [^Key md-type ^String s]
    (->> s
         (digest md-type)
         bytes/bytes-to-hex)))


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
