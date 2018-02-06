(ns quantum.untyped.security.cryptography
  (:require
#?(:cljs
    [goog.crypt.base64         :as base64])
    [quantum.untyped.core.core :as ucore]
    [quantum.untyped.core.error
      :refer [TODO]]))

(ucore/log-this-ns)

(defn >base-encoded|string
  "Converts `x` to base-`n` string representation."
  [x, n #_integer? #_> #_string?]
  (case n
    32 (TODO)
    64 (cond (string? x)
             #?(:clj  (TODO)
                :cljs (base64/encodeString x))
             :else (TODO))))

(defn >base-encoded|bytes
  "Converts `x` to base-`n` byte-array representation."
  [x, n #_integer? #_> #_bytes?]
  (case n
    32 (TODO)
    64 (cond (string? x)
             #?(:clj  (TODO)
                :cljs (base64/encodeByteArray x))
             :else (TODO))))

(defn base-encoded>string
  "Converts `x` from base-`n` representation to a string."
  [x, n #_integer? #_> #_string?]
  (case n
    32 (TODO)
    64 (cond (string? x)
             #?(:clj  (TODO)
                :cljs (base64/decodeString x))
             :else (TODO))))

(defn base-encoded>bytes
  "Converts from a base-`n` representation `x` to a byte-array."
  [x #_string?, n #_integer? #_> #_bytes?]
  (case n
    32 (TODO)
    64 (cond (string? x)
             #?(:clj  (TODO)
                :cljs (base64/decodeStringToByteArray x))
             :else (TODO))))
