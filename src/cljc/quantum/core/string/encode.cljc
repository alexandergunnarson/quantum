(ns
  ^{:doc "Useful string utils for formatting strings."
    :attribution "Alex Gunnarson"}
  quantum.core.string.encode
  (:refer-clojure :exclude [reverse replace remove val re-find])
  (:require-quantum [:core fn set map macros logic red type loops cbase log err])
  (:require
    [clojure.string :as str]
    [frak])
  #?(:clj (:import java.net.IDN
                   java.nio.charset.StandardCharsets)))

; ===== ENCODINGS & CHARSETS =====

; http://java-performance.com
; Always prefer national charsets like windows-1252 or Shift_JIS to UTF-8: they produce
; more compact binary representation (as a rule) and they are faster to encode/decode
; (it's becoming a rule in Java 8).
; ISO-8859-1 always works faster than US-ASCII in Java 7 and 8. Choose ISO-8859-1 if you
; don't have any solid reasons to use US-ASCII.
; You can write a very fast String->byte[] conversion for US-ASCII/ISO-8859-1, but
; you can not beat Java decoders - they have direct access to the output String they create.


; The six standard {@link Charset} instances which are guaranteed to be supported by all Java platform implementations.
; See the Guava User Guide article on https://github.com/google/guava/wiki/StringsExplained#charsets
(def charsets
  #{StandardCharsets/ISO_8859_1 ; ISO Latin Alphabet Number 1 (ISO-LATIN-1
    StandardCharsets/US_ASCII   ; seven-bit ASCII, the Basic Latin block of the Unicode character set (ISO646-US)
    StandardCharsets/UTF_16     ; sixteen-bit UCS Transformation Format, byte order identified by an optional byte-order mark.
    StandardCharsets/UTF_16BE   ; sixteen-bit UCS Transformation Format, big-endian byte order.
    StandardCharsets/UTF_16LE   ; sixteen-bit UCS Transformation Format, little-endian byte order.
    StandardCharsets/UTF_8})    ; eight-bit UCS Transformation Format

(def ^:const max-ascii-val 0x7F)

(defn ascii?
  {:todo ["Use |ffilter|"]}
  [s]
  (and (string? s)
       (->> s (filter #(> (core/int %) max-ascii-val)) first)))

#?(:clj
(def ^{:doc "This is because of a bug in java.net.IDN/toASCII that
             org.apache.commons.validator.routines.DomainValidator pointed out.
             It may have been fixed already..."}
  idn:->ascii-preserves-trailing-dots? (= "a." (IDN/toASCII "a."))))

#?(:clj
(defn unicode->ascii
  "Converts potentially Unicode input to punycode."
  {:contributors ["org.apache.commons.validator.routines.DomainValidator"]}
  [input & [silent-fail?]]
  (if (or (nil? input) (empty? input) (ascii? input)) ; skip possibly expensive processing
      input
      (try
        (let [ascii (IDN/toASCII input)]
          (if idn:->ascii-preserves-trailing-dots?
              ascii
              ; RFC3490 3.1. 1)
              ; Whenever dots are used as label separators, the following
              ; characters MUST be recognized as dots: U+002E (full stop), U+3002
              ; (ideographic full stop), U+FF0E (fullwidth full stop), U+FF61
              ; (halfwidth ideographic full stop).
              (condpc = (last input) ; original last char
                (coll-or \u002E     ; full stop
                         \u3002     ; ideographic full stop
                         \uFF0E     ; fullwidth full stop
                         \uFF61)    ; halfwidth ideographic full stop
                    (str ascii ".") ; restore the missing stop
                ascii)))
      (catch IllegalArgumentException e ; input is not valid
        (if silent-fail?
            input
            (throw e)))))))