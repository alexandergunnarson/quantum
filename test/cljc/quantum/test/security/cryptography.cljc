(ns quantum.test.security.cryptography
  (:require [quantum.security.cryptography :refer :all]))

; __________________________________________
; =============== TRANSPORTS ===============
; ------------------------------------------

#?(:cljs
(defn test:transport [type]))

; ===== DECODE =====

#?(:clj
(defn test:decode32 [x]))

#?(:clj 
(defn test:decode64 [x]))

#?(:clj
(defn test:decode64-int [x]))

#?(:clj
(defn test:decode [k obj]))

#?(:clj
(defn test:decode-int [k obj]))

; ===== ENCODE =====

#?(:clj
(defn test:encode32 [x]))

#?(:clj
(defn test:encode64 [x]))

#?(:clj
(defn test:encode64-string [x]))


(defn test:encode [k obj])

; __________________________________________
; =========== HASH / MSG DIGEST ============
; ------------------------------------------

#?(:cljs
(defn test:msg-digest [type]))

; ===== DIGEST =====

#?(:clj
(defn test:digest
  [hash-type ^String s]))

#?(:clj
(defn test:hex-digest
  [hash-type ^String s]))

#?(:clj
(defn test:sha-hmac [instance-str message secret]))

#?(:clj
(defn test:pbkdf2
 ([^String s & [salt iterations key-length]])))

#?(:clj
(defn test:bcrypt
  ([raw & [work-factor]])))

(defn test:scrypt
  [^String s & [{:keys [cpu-cost ram-cost parallelism dk-length salt]}]])

(defn test:check-pw [user-id pass'])

#?(:clj
(defn test:hash
  ([obj])
  ([algorithm obj & [opts]])))

; ===== HASH COMPARE =====

(defn test:secure=
  [a b])

#(defn test:hash-match? [algo test encrypted])

; _______________________________________________________
; ========= (SYMMETRIC)   CIPHER   (ENCRYPTION) =========
; =========             / DECIPHER (DECRYPTION) =========
; -------------------------------------------------------

(defn test:encrypt-param*
  ([type key-])
  ([type key- tweak]))

#?(:cljs
(defn test:cipher [type]))

#?(:clj
(defn test:threefish
  [in type & [{:keys [key tweak bits] :as opts}]]))

(defn test:aes
  [in type ^String password
   & [{:keys [key salt iterations sensitivity ->base64? base64->? ->str?] :as opts}]])

(defn test:encrypt
  [algo obj & [{:keys [key tweak salt password]} :as opts]])

(defn test:decrypt
  [algo obj & [{:keys [key tweak salt password]} :as opts]])
