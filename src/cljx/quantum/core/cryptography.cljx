(ns quantum.core.cryptography
  #+clj (:gen-class))

#+clj (require '[quantum.core.ns  :as ns :refer :all])
#+clj (ns/require-all *ns* :clj)

#+clj (require
  '[quantum.core.data.bytes :as bytes]
  '[quantum.core.logic      :refer :all]
  '[quantum.core.string     :as str])
#+clj (import '(java.security MessageDigest DigestInputStream))
#+clj (import '(java.io InputStream ByteArrayInputStream))

#+clj
(def ^sun.nio.cs.UTF_8 utf-8
  (java.nio.charset.Charset/forName "UTF-8"))

#+clj
(defn ^"[B" digest
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
        ^"[B"               digested (.digest md)]
    (.close in-stream)
    (.close dis)
    digested))

#+clj
(defn ^String hex-digest
  "Gets the digest of a string in hexadecimal.
   Assumes UTF-8 encoding for string passed."
  [^Key md-type ^String s]
  (->> s
       (digest md-type)
       bytes/bytes-to-hex))
