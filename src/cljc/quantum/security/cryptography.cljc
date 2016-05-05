(ns
  ^{:doc "Cryptographic/encryption functions like hashing and digests.

          Possibly should just alias some better library for this."
    :attribution "Alex Gunnarson"}
  quantum.security.cryptography
  (:refer-clojure :exclude [hash])
  #_(:require-quantum
    [fn logic rand cbase core-async])
           (:require [#?(:clj  clojure.core
                         :cljs cljs.core   )        :as core   ]
             #?(:clj [byte-transforms               :as bt     ])
             #?(:clj [quantum.core.data.array       :as arr
                       :refer [->bytes]                        ])
                     [quantum.core.data.bytes       :as bytes  ]
                     [quantum.core.data.hex         :as hex    ]
                     [quantum.core.data.set         :as set    ]
                     [quantum.core.collections      :as coll
                       :refer [#?(:clj kmap)]                  ]
                     [quantum.core.error            :as err               
                       :refer [->ex #?(:clj throw-unless)]     ]
                     [quantum.core.fn               :as fn
                       :refer [#?@(:clj [<-])]                 ]
                     [quantum.core.logic            :as logic
                       :refer [#?@(:clj [whenp]) nnil? splice-or]]
                     [quantum.core.macros           :as macros
                       :refer [#?@(:clj [defnt])]              ]
                     [quantum.core.nondeterministic :as rand   ]
                     [quantum.core.numeric          :as num    ]
                     [quantum.core.convert          :as conv   ]
                     [quantum.core.string           :as str    ]
                     [quantum.core.vars             :as var   
                       :refer [#?(:clj defalias)]              ])
  #?(:cljs (:require-macros
                     [quantum.core.collections      :as coll
                       :refer [kmap]]     
                     [quantum.core.error            :as err               
                       :refer [throw-unless]                   ]
                     [quantum.core.fn               :as fn
                       :refer [<-]                             ]
                     [quantum.core.logic            :as logic
                       :refer [whenp]                          ]
                     [quantum.core.macros           :as macros
                       :refer [defnt]                          ]
                     [quantum.core.vars             :as var
                       :refer [defalias]                       ]))
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
    [com.lambdaworks.crypto SCryptUtil SCrypt]
    org.bouncycastle.crypto.engines.ThreefishEngine
    org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher
    [org.bouncycastle.crypto BlockCipher BufferedBlockCipher])))

; TODO move to charsets
; TODO compare Google Closure vs. Forge crypto implementations

#?(:clj
  (def ^sun.nio.cs.UTF_8 utf-8
    (Charset/forName "UTF-8")))

#?(:cljs
(defn forge-unsupported! []
  (throw (->ex :not-implemented
               "Supported by Forge but no interface in ClojureScript"
               type))))

; __________________________________________
; =============== TRANSPORTS ===============
; ------------------------------------------

#?(:cljs
(defn transport [type]
  (condpc = type
    ; Pooled sockets
    :http (forge-unsupported!)
    ; XmlHttpRequest using backend of forge.http
    :xhr  (forge-unsupported!)
    :ssh  (forge-unsupported!)
    ; TLS_RSA_WITH_AES_128_CBC_SHA, TLS_RSA_WITH_AES_256_CBC_SHA
    :tls  (forge-unsupported!))))

; ===== ENCODE =====

#?(:clj (def available-encoders #{:base32 :base64}))

; (encode "abc" :base64)
; Inaccurate: use _ instead of -, etc., and chop off trailing '='
; (:clj (defalias encode bt/encode))

; ===== DECODE =====

#?(:clj
(defnt ^"[B" decode32
  ([^integer? x]
    (throw (->ex :not-implemented "Decode32 not implemented for |integer|" x)))
  ([^bytes? x] (.decode (Base32.) x))
  ([x] (decode32 (arr/->bytes-protocol x)))))

#?(:clj 
(defnt ^"[B" decode64
  {:todo ["Add support for java.util.Base64 MIME and URL decoders"]
   :performance "java.util.Base64 Found to be the fastest impl. according to
                 http://java-performance.info/base64-encoding-and-decoding-performance/"}
  ([^bytes?  x] (.decode (java.util.Base64/getDecoder) x))
  ([^string? x] (-> x (.getBytes StandardCharsets/ISO_8859_1) decode64))
  ([         x] (-> x arr/->bytes-protocol decode64))))

#?(:clj
(defnt ^Integer decode64-int
  ([^bytes? x] (int (Base64/decodeInteger x)))
  ([x] (decode64-int (arr/->bytes-protocol x)))))

#?(:clj
(defn ^"[B" decode [k obj]
  (condp = k
    :base32        (decode32 obj)
    :base64        (decode64 obj)
    :base64-string (let [^"[B" decoded (decode64 obj)]
                     (String. decoded StandardCharsets/ISO_8859_1))
    (throw (->ex nil "Unrecognized codec" k)))))

#?(:clj
(defn ^"[B" decode-int [k obj]
  (condp = k
    :base64 (decode64-int obj)
    (throw (->ex nil "Unrecognized codec" k)))))

; (:clj (defalias decode bt/decode))

#?(:clj
(defnt ^"[B" encode32
  ([^integer? x]
    (throw (->ex :not-implemented "Encode32 not implemented for |integer|" x)))
  ([^bytes? x] (.encode (Base32.) x))
  ([x] (encode32 (arr/->bytes-protocol x)))))

#?(:clj
(defnt ^"[B" encode64
  {:todo ["Add support for java.util.Base64 MIME and URL encoders"]
   :performance "java.util.Base64 Found to be the fastest impl. according to
                 http://java-performance.info/base64-encoding-and-decoding-performance/"}
  ([^integer? x]
    (Base64/encodeInteger (num/->big-integer x)))
  ([^bytes? x] (.encode (java.util.Base64/getEncoder) x))
  ; TODO make so Object gets protocol if reflection
  ([x] (encode64 (arr/->bytes-protocol x)))))

(defnt ^String encode64-string
  #?(:cljs ([#{js/Int8Array js/Uint8Array array?} x]
              (-> x arr/->uint8-array js/forge.util.binary.base64.encode))) ; Google Closure's implementation didn't work
  #?(:clj  ([x]
             (let [encoded (encode64 x)]
               (String. ^"[B" encoded StandardCharsets/ISO_8859_1)))))

(defn encode [k obj]
  (condp = k
    #?@(:clj
      [:base32        (encode32 obj)
       :base64        (encode64 obj)])
       :base64-string (encode64-string obj)
       (throw (->ex nil "Unrecognized codec" k))))

; __________________________________________
; =========== HASH / MSG DIGEST ============
; ------------------------------------------

#?(:cljs
(defn msg-digest [type]
  (condp = type
    :SHA-1    (forge-unsupported!)
    :SHA-256  (forge-unsupported!)
    :SHA-385  (forge-unsupported!)
    :SHA-512  (forge-unsupported!)
    :MD5      (forge-unsupported!)
    :HMAC     (forge-unsupported!)
    :else (->ex :not-implemented nil type))))

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
  [hash-type ^String s]
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
    [hash-type ^String s]
    (->> s
         (digest hash-type)
         bytes/bytes-to-hex)))

; Fast hashes kill security.
#?(:clj
(def available-hashers
  (set/union
    #{:crc32 :crc64
      :md2 ; 1989
      ; md4
      :md5 ; 1992
      ; md6 
      ; As of 2015, no example of a SHA-1 collision has been published yet — Wikipedia
      :sha1   :sha-1-hmac
      :sha256 :sha-256-hmac
      :sha384
      :sha512
      :murmur32 :murmur64 :murmur128
      :adler32
      :pbkdf2
      :bcrypt
      :scrypt}
    (into #{} (bt/available-hash-functions)))))

#?(:clj
(defn sha-hmac ^"[B" [instance-str message secret]
  (throw-unless (and message secret)
    (->ex nil "Neither message nor secret can be nil" (kmap message secret)))
  (let [^Mac algo-instance (Mac/getInstance instance-str)
        ^SecretKey secret-key (SecretKeySpec. (->bytes secret) instance-str)]
    (.init    algo-instance secret-key)
    (.doFinal algo-instance (->bytes message)))))

#?(:clj (def sha-hmac identity))

#?(:clj
(defn pbkdf2
 "Recommended by NIST:
    http://csrc.nist.gov/publications/nistpubs/800-132/nist-sp800-132.pdf

  OWASP: Use when FIPS certification or enterprise support on many platforms is required.
  OWASP: Apple uses this algorithm with 10000 iterations for its iTunes passwords.

  @iterations is the number of times that the password is hashed during the
  derivation of the symmetric key. The higher number, the more difficult it is
  to brute force the key.

  Feb  2005 - AES in Kerberos 5 'defaults' to 4096 rounds of SHA-1. (source: RFC 3962)
  Sept 2010 - ElcomSoft claims iOS 3.x uses 2,000 iterations, iOS 4.x uses
              10,000 iterations (source: ElcomSoft)
  May  2011 - LastPass uses 100,000 iterations of SHA-256 (source: LastPass)"
  {:info ["http://security.stackexchange.com/questions/3959/recommended-of-iterations-when-using-pkbdf2-sha256"]}
  ([^String s & [salt iterations key-length]]
   (let [salt       (if (nnil? salt) (->bytes salt) (rand/rand-bytes true 128))
         iterations (or iterations 100000)
         k (PBEKeySpec. (.toCharArray s)
             salt iterations
             (or key-length 192))
         hashed
           (->> (SecretKeyFactory/getInstance "PBKDF2WithHmacSHA1")
                (<- .generateSecret k)
                (.getEncoded))
         salt (conv/->str salt)
         ;(->> iterations (encode :base64) ->str)
         ]
     (kmap hashed salt iterations)))))

(declare pbkdf2)

#?(:clj
(defn ^String bcrypt
  "OWASP: Use where PBKDF2 or scrypt support is not available.
   @work-factor: the log2 of the number of hashing iterations to apply"
  ([raw & [work-factor]]
    (BCrypt/hashpw raw (BCrypt/gensalt (or work-factor 11))))))

(defn scrypt ; returns String in CLJ; returns promise in CLJS
  "OWASP: Use when resisting any and all hardware accelerated attacks is necessary but support isn't."
  [^String s & [{:keys [cpu-cost ram-cost parallelism dk-length salt]}]]
  (let [log2-cpu-cost      (or cpu-cost 15)  ; 1 to 31
        exp-cpu-cost       (num/exp 2 log2-cpu-cost)
        ram-cost           (or ram-cost 8) ; block size
        parallelism        (or parallelism 1) ; Cores
        salt               (or salt (rand/rand-bytes true 16)) ; bytes
        derived-key-length (or dk-length 32)
        #?@(:cljs [result (core-async/promise-chan)])
        derived-to-string
          (fn [derived]
            (let [params (hex/->hex-string (bit-or #_| (bit-shift-left #_<< log2-cpu-cost 16)
                                             (bit-shift-left #_<< ram-cost 8)
                                             parallelism))]
              (str "$s0$" params "$" (encode :base64-string salt)
                                 "$" (encode :base64-string derived))))
        derived 
          (do #?(:clj  (SCrypt/scrypt ^"[B" (.getBytes s "UTF-8")
                                      ^"[B" salt exp-cpu-cost  ram-cost parallelism derived-key-length)
                 :cljs (js/ScryptAsync      s
                                            salt log2-cpu-cost ram-cost             derived-key-length
                                            (fn [v] (->> v
                                                         derived-to-string
                                                         (core-async/put! result))))))] 
        #?(:clj  (derived-to-string derived)
           :cljs result)))

#_(secure=
  (scrypt "password" {:cpu-cost 15 :ram-cost 8 :parallelism 1 :dk-length 32 :salt salt**})
  (scrypt "password" {:cpu-cost 15 :ram-cost 8 :parallelism 1 :dk-length 32 :salt salt**}))

#_(defn check-pw [user-id pass'] ; "user-id$public-site-salt$hashed-pass-from-client"
  (if-let [pass''-0 (db/q [:find ['?pass'' ...]
                        :where ['?e :user/id   user-id]
                               ['?e :user/pass '?pass'']])]
    (let [salt''      (->salt pass''-0)
          _           (assert (string? salt''))
          pass''-test (scrypt pass' {:salt salt''})]
      ; |public-site-salt| is a publicly known string (embedded into client app),
      ; which is supposedly unique for the site (it is added as an additional precaution to
      ; avoid pre-computation per-user attacks to work across different sites).
      (secure= pass''-0 pass''-test))
    false))

; http://ithare.com/client-plus-server-password-hashing-as-a-potential-way-to-improve-security-against-brute-force-attacks-without-overloading-server/

#?(:clj
(defn hash
  ([obj] (hash :murmur64 obj)) ; murmur64 is Clojure's implementation
  ([algorithm obj & [opts]]
    (condp = algorithm
      :clojure      (core/hash obj)
      :sha-1-hmac   (sha-hmac "HmacSHA1"   obj (:secret opts))
      :sha-256-hmac (sha-hmac "HmacSHA256" obj (:secret opts))
      :pbkdf2 (pbkdf2 obj (:salt        opts)
                          (:iterations  opts)
                          (:key-length  opts))
      :bcrypt (bcrypt obj (:work-factor opts))
      :scrypt (scrypt obj opts)
      (bt/hash obj algorithm opts)))))

; ===== HASH COMPARE =====

(defn secure=
  "Test whether two sequences of characters or bytes are equal in a way that
   protects against timing attacks.
   (It compares values in a way that takes the same amount of time no matter
   how much of the values match.)
   Note that this does not prevent an attacker
   from discovering the *length* of the data being compared."
  {:source "weavejester/crypto.equality"}
  [a b]
  (let [a (map int a)
        b (map int b)]
    (if (and a b (= (count a) (count b)))
        (zero? (core/reduce bit-or (map bit-xor a b)))
        false)))

; Not secure (?)
#?(:clj (defalias hash= bt/hash=))

; TODO test more
#_(defn hash-match? [algo test encrypted]
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

; _______________________________________________________
; ========= (SYMMETRIC)   CIPHER   (ENCRYPTION) =========
; =========             / DECIPHER (DECRYPTION) =========
; -------------------------------------------------------

(defn encrypt-param*
  ([type key-] (encrypt-param* type key- true))
  ([type key- tweak]
    (let [encrypt-param
            (condp = type
              :encrypt true
              :decrypt false
              (throw (->ex nil "Invalid encryption param" type)))
          _ (when (= type :decrypt)
              (throw-unless (and key- tweak)
                (->ex nil "Missing required parameters:" {:key key- :tweak tweak})))]
      encrypt-param)))

(def sensitivity-map
  ; Numbers are from 8-core Linux
  {             ; 256    0.79 ms   0.33%  |  2  hours + 24 min
                ; 512    1.88 ms   0.78%  |  5  hours + 41 min
                ; 1024   3.56 ms   1.5 %  |  11 hours
                ; 2048   7.31 ms   3.0 %  |  22 hours
                ; 4096   12.2 ms   5.1 %  |  1 day  + 13 hours
   1         13 ; 8192   21.9 ms   9.1 %  |  2 days + 18 hours
   2         14 ; 16384  36.7 ms  15.2 %  |  4 days + 15 hours
   3         15 ; 32768  72.1 ms  29.9 %  |  9 days + 2 hours
   4         16 ; 65536  119  ms  49.4 %  |  2 weeks + a day 
   5         17 ; 131072 242  ms 100.4 %  |  1 month + half day
   :password 17})

#?(:cljs
(defn cipher [type]
  (condp = type
    :AES-ECB  (forge-unsupported!)
    :AES-CBC  (forge-unsupported!)
    :AES-CFB  (forge-unsupported!)
    :AES-OFB  (forge-unsupported!)
    :AES-CTR  (forge-unsupported!)
    :AES-GCM  (forge-unsupported!)
    :3DES-ECB (forge-unsupported!)
    :3DES-CBC (forge-unsupported!)
    :DES-ECB  (forge-unsupported!)
    :DES-CBC  (forge-unsupported!)
    :RC2      (forge-unsupported!)
    :PKI      (forge-unsupported!)
    :RSA      (forge-unsupported!)
    :RSA-KEM  (forge-unsupported!)
    :X.509    (forge-unsupported!)
    :PKCS#5   (forge-unsupported!)
    :PKCS#7   (forge-unsupported!)
    :PKCS#8   (forge-unsupported!)
    :PKCS#10  (forge-unsupported!)
    :PKCS#12  (forge-unsupported!)
    :ASN.1    (forge-unsupported!)
    :else (->ex :not-implemented nil type))))

#?(:clj
(defn threefish
  {:todo  ["Truncate decrypted text — reverse search for NUL char in last block and truncate from there"]
   :tests '[(let [e (threefish "Hey! This is a secret message." :encrypt)]
              (String.
                (threefish
                  (:encrypted e)
                  :decrypt
                  (:key e) (:tweak e))))]}
  [in type & [{:keys [key tweak bits] :as opts}]]
  (let [encrypt?   (encrypt-param* type key tweak)
        bits       (or bits 512)
        block-size (/ bits 8)
        key-  (or key   (rand/rand-longs (-> bits (/ 8) (/ 8))))
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
      :decrypt out))))

(defn aes
  "Source 1: You need your per-password encryption time to be at least 241 ms, assuming a
             hacker's patience of one month.
             So you should set the number of iterations such that computing it over a
             single password takes at least that much time on your server.

             Go for the maximum iteration count that you can budget for, so long
             as it doesn't delay real users doing normal logins. And you should
             increase the value as your compute capacity grows over the years.

   Source 2: SQL Cipher AES-256 encrypted database uses 64000 iterations,
             so a factor of 16, or ~119 ms."
  {:todo ["Replace Math/pow with num/exp"]
   :info {1 "http://stackoverflow.com/questions/992019/java-256-bit-aes-password-based-encryption"
          2 "http://security.stackexchange.com/questions/3959/recommended-of-iterations-when-using-pkbdf2-sha256"
          3 "https://www.zetetic.net/sqlcipher/design/"}
   :performance '{[1091  :ms] (time (dotimes [n 10] (aes :encrypt "myinterestingperson1@ymail.com" "myfunpassword" nil nil {:iterations 16})))
                  [5.168 :ms] (time (dotimes [n 10] (aes :encrypt "myinterestingperson1@ymail.com" "myfunpassword" nil nil {:iterations 8})))}
   :tests '[(let [pass "password"
                  e (aes "Hey! This is a secret message." :encrypt pass)]
              (String.
                (aes (:encrypted e) :decrypt pass e)))]}
  [in type ^String password
   & [{:keys [key salt iterations sensitivity ->base64? base64->? ->str?] :as opts}]]
  ;[{:keys [encrypted ^"[B" tweak]}]
  (let [;output :base64 ; TODO change default to bytes
        encrypt? (encrypt-param* type password)
        in-f  #?(:clj in 
                 :cljs (if base64->?
                           (-> in conv/base64->forge-bytes
                               js/forge.util.createBuffer)
                           (js/forge.util.createBuffer in "utf8")))
        #?@(:clj
          ; ^KeySpec
       [^SecretKeyFactory factory (SecretKeyFactory/getInstance "PBKDF2WithHmacSHA256")])
                          salt-f  (or #?(:clj  salt
                                         :cljs (whenp salt base64->?
                                                 conv/base64->forge-bytes))
                                      #?(:clj  (rand/rand-bytes true 128)
                                         :cljs (js/forge.random.getBytesSync 128)))
                          keyspec (#?(:clj  PBEKeySpec.
                                      :cljs js/forge.pkcs5.pbkdf2)
                                    (#?(:clj  .toCharArray
                                        :cljs identity) password) ; TODO use a conversion here
                                    salt-f
                                    (#?(:clj  num/exp
                                        :cljs js/Math.pow) 2 (or (:iterations opts)
                                                   (->> opts :sensitivity
                                                        (get sensitivity-map))
                                                   (:password sensitivity-map))) ; iterationCount
                                    ; On CLJS, a key size of 16 bytes will use AES-128,
                                    ; 24 => AES-192, 32 => AES-256
                                    ; TODO make customizable
                                    ; This is key size in bits in CLJ, but in bytes in CLJS
                                    (-> (#?(:clj  num/exp
                                            :cljs js/Math.pow) 2 8)
                                        #?(:cljs (/ 8)))
                                    #?(:cljs (js/forge.md.sha256.create))) ; defaults to SHA1
        #?@(:clj
       [^SecretKey        secret  (-> factory 
                                      (.generateSecret keyspec)
                                      (.getEncoded)
                                      (SecretKeySpec. "AES"))])]
    (if encrypt?
        (let [
   #?@(:clj  [^Cipher              cipher    (doto (Cipher/getInstance "AES/CBC/PKCS5Padding")
                                                   (.init Cipher/ENCRYPT_MODE secret))]
       :cljs [                     cipher    (js/forge.cipher.createCipher "AES-CBC" keyspec)])
   #?@(:clj  [^AlgorithmParameters params    (.getParameters ^Cipher cipher)
              ; to avoid reflection
              ^IvParameterSpec     iv-spec   (-> params (.getParameterSpec IvParameterSpec))])
   #?@(:clj  [^"[B"                iv        (.getIV iv-spec)]
       :cljs [                     iv        (js/forge.random.getBytesSync (js/Math.pow 2 4))]) ; Should be 16 bytes  
   #?@(:clj  [^"[B"                encrypted (-> cipher (.doFinal (->bytes in-f #_"UTF-8")))]
       :cljs [                     encrypted (-> cipher
                                                 (doto (.start #js {:iv iv})
                                                       (.update (.createBuffer js/forge.util in))
                                                       (.finish)) ; ByteStringBuffer
                                                 (.-output))])]
          ; TODO make same output per platform
          {:key       #?(:clj  iv
                         :cljs (whenp iv ->base64?
                                 conv/forge-bytes->base64))
           :encrypted #?(:clj  encrypted
                         :cljs (whenp encrypted ->base64?
                                 (fn-> .bytes conv/forge-bytes->base64)))
           ; The salt doesn't need to be kept secret
           :salt      #?(:clj  salt-f
                         :cljs (whenp salt-f ->base64?
                                 conv/forge-bytes->base64))})
       (let [key-f #?(:clj key
                      :cljs (whenp key base64->?
                              conv/base64->forge-bytes))
             #?@(:clj  [decipher (Cipher/getInstance "AES/CBC/PKCS5Padding")]
                 :cljs [decipher (js/forge.cipher.createDecipher "AES-CBC" keyspec)])
             #?@(:clj  [_   (.init    decipher Cipher/DECRYPT_MODE secret (IvParameterSpec. key-f))]
                 :cljs [_   (.start   decipher #js {:iv key-f})])
             #?@(:clj  [out (.doFinal decipher in-f)]
                 :cljs [_   (.update  decipher in-f)
                        _   (.finish  decipher)
                        out (.-output decipher)])]
          (whenp out ->str? conv/->str)))))

(defn encrypt
  {:tests '[(let [opts {:password "Alex"}
                  e (encrypt :aes "Hey! A message!" opts)]
              (decrypt :aes (:encrypted e) (merge opts e)))]}
  [algo obj & [{:keys [key tweak salt password]} :as opts]]
  (condp = algo
    :aes                (aes       obj :encrypt password opts)
    :threefish #?(:clj  (threefish obj :encrypt          opts)
                  :cljs (throw (->ex :unsupported "Threefish unsupported as of yet in CLJS.")))))

(defn decrypt
  [algo obj & [{:keys [key tweak salt password]} :as opts]]
  (condp = algo
    :aes                (aes       obj :decrypt password opts)
    :threefish #?(:clj  (threefish obj :decrypt          opts)
                  :cljs (throw (->ex :unsupported "Threefish unsupported as of yet in CLJS.")))))

