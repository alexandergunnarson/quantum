(ns
  ^{:doc "Cryptographic/encryption functions like hashing and digests.

          Possibly should just alias some better library for this."
    :attribution "Alex Gunnarson"}
  quantum.core.cryptography
  (:refer-clojure :exclude [hash])
  (:require-quantum
    [ns bytes set fn logic str type num macros log convert err coll arr bin rand])
  (:require [byte-transforms :as bt])
#?(:clj
  (:import
    [java.security     MessageDigest DigestInputStream SecureRandom AlgorithmParameters]
    [java.io           InputStream   ByteArrayInputStream]
    [java.nio          ByteBuffer]
    [java.nio.charset  Charset StandardCharsets]
    [javax.crypto      Mac SecretKeyFactory SecretKey Cipher]
    [javax.crypto.spec SecretKeySpec PBEKeySpec IvParameterSpec]
    [org.apache.commons.codec.binary Base32 Base64] ; Unnecessary
    org.mindrot.jbcrypt.BCrypt
    com.lambdaworks.crypto.SCryptUtil
    org.bouncycastle.crypto.engines.ThreefishEngine
    org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher
    [org.bouncycastle.crypto BlockCipher BufferedBlockCipher])))







; ===== DECODE =====

(defnt ^"[B" decode32
  ([^integer? x]
    (throw+ (Err. :not-implemented "Decode32 not implemented for |integer|" x)))
  ([^bytes? x] (.decode (Base32.) x))
  ([x] (decode32 (arr/->bytes-protocol x))))

(defnt ^"[B" decode64
  {:todo ["Add support for java.util.Base64 MIME and URL decoders"]
   :performance "java.util.Base64 Found to be the fastest impl. according to
                 http://java-performance.info/base64-encoding-and-decoding-performance/"}
  ([^bytes?  x] (.decode (java.util.Base64/getDecoder) x))
  ([^string? x] (-> x (.getBytes StandardCharsets/ISO_8859_1) decode64))
  ([         x] (-> x arr/->bytes-protocol decode64)))

(defnt ^Integer decode64-int
  ([^bytes? x] (int (Base64/decodeInteger x)))
  ([x] (decode64-int (arr/->bytes-protocol x))))

(defn ^"[B" decode [k obj]
  (condp = k
    :base32        (decode32 obj)
    :base64        (decode64 obj)
    :base64-string (let [^"[B" decoded (decode64 obj)]
                     (String. decoded StandardCharsets/ISO_8859_1))
    (throw+ (Err. nil "Unrecognized codec" k))))

(defn ^"[B" decode-int [k obj]
  (condp = k
    :base64 (decode64-int obj)
    (throw+ (Err. nil "Unrecognized codec" k))))

; ===== DIGEST =====

#?(:clj
(defn ^bytes digest
  "Creates a byte digest of the string @s according to the @md-type algorithm.
   Assumes UTF-8 encoding for string passed.

   WARNINGS:
     CMU Software Engineering Institute now says that MD5
     'should be considered cryptographically broken and
      unsuitable for further use.'
     SHA-1 has some possible vulnerabilities too.
     Most U.S. government applications now require the SHA-2
     family of hash functions.

     As for SHA-3, attackers can crack SHA-3 hashed passswords 8 times
     faster than SHA-2 hashed passwords -
     2 times faster because we need to halve the number of hash iterations
     and 4 times faster because of SHA-3 hardware being faster than SHA-2 hardware.

     SHA-3, like SHA-2, is not intended for use as a password hash function.

     Don't encrypt passwords symmetrically. Other things, maybe, but not passwords.
     Anything that can be asymmetrically encrypted instead of symmetrically, do it."
  {:todo "Allow more hashing functions"}
  [^Keyword hash-type ^String s]
  {:pre [(splice-or hash-type = :md5 :sha-256)]}
  (let [^String            hash-type-str
          (-> hash-type name str/->upper)
        ^MessageDigest     md
          (MessageDigest/getInstance hash-type-str)
        ^InputStream       in (conv/->input-stream s)
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



(defn hash-match? [algo test encrypted]
  (condp = algo 
    :pbkdf2
      (if-not (map? encrypted)
        (throw+ (Err. nil "Encrypted must be map." encrypted))
        (let [salt       (decode     :base64 (:salt       encrypted))
              iterations (decode-int :base64 (:iterations encrypted))]
          (secure= (:hashed encrypted)
                   (hash :pbkdf2 encrypted iterations salt))))
    :bcrypt (BCrypt/checkpw   test encrypted)
    :scrypt (SCryptUtil/check test encrypted)))

(defn threefish
  {:todo  ["Truncate decrypted text — reverse search for NUL char in last block and truncate from there"]
   :tests '[(let [e (threefish :encrypt "Hey! This is a secret message.")]
              (String.
                (threefish :decrypt
                  (:encrypted e)
                  (:key e) (:tweak e))))]}
  [type in & [key- tweak bits]]
  (let [encrypt?   (encrypt-param* type key- tweak)
        bits       (or bits 512)
        block-size (/ bits 8)
        key-  (or key-  (rand/rand-longs (-> bits (/ 8) (/ 8))))
        tweak (or tweak (rand/rand-longs 2))
        engine (-> (ThreefishEngine. bits)
                   (doto
                     (.init encrypt? key- tweak))
                   (BufferedBlockCipher.))
        in-bytes (->bytes in)
        in-f (byte-array (-> in-bytes count (/ block-size)
                             num/ceil int (* block-size)))
        _ (System/arraycopy in-bytes 0 in-f 0 (count in-bytes))
        out (byte-array (count in-f))
        len-processed
          (.processBytes engine
            in-f 0 (count in-f) out 0)
        _ (.doFinal engine out len-processed)]
    (condp = type
      :encrypt {:encrypted out :key key- :tweak tweak}
      :decrypt out)))

(defn encrypt
  {:tests '[(let [opts {:password "Alex"}
                  e (encrypt :aes "Hey! A message!" opts)]
              (decrypt :aes (:encrypted e) (merge opts e)))]}
  [algo obj & [{:keys [key tweak salt password opts]}]]
  (condp = algo
    :aes       (aes       :encrypt obj password key salt opts)
    :threefish (threefish :encrypt obj key tweak)))

(defn decrypt [algo obj & [{:keys [key tweak salt password opts]}]]
  (condp = algo
    :aes       (aes       :decrypt obj password key salt opts)
    :threefish (threefish :decrypt obj key tweak)))