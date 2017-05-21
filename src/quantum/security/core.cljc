(ns quantum.security.core
  (:require
    #?@(:clj [[less.awful.ssl :as las]]))
  #?(:clj
  (:import java.security.KeyStore
           [javax.net.ssl TrustManagerFactory KeyManagerFactory]
           [io.netty.handler.ssl SslProvider SslContextBuilder])))

; Disabling developer tools (e.g. console) doesn't matter. This doesn't protect against hackers.


#?(:clj
(defn trust-manager-factory
  "An X.509 trust manager factory for a KeyStore."
  [^KeyStore key-store]
  (let [factory (TrustManagerFactory/getInstance "PKIX" "SunJSSE")]
    ; I'm concerned that getInstance might return the *same* factory each time,
    ; so we'll defensively lock before mutating here:
    (locking factory
      (doto factory (.init key-store))))))

#?(:clj
(defn key-manager-factory
  "An X.509 key manager factory for a KeyStore."
  ([key-store password]
   (let [factory (KeyManagerFactory/getInstance "SunX509" "SunJSSE")]
     (locking factory
       (doto factory (.init key-store password)))))
  ([key-store]
   (key-manager-factory key-store las/key-store-password))))

#?(:clj
(defn ssl-context-generator:netty
  "Returns a function that yields SSL contexts. Takes a PKCS8 key file, a
  certificate file, and a trusted CA certificate used to verify peers."
  [key-file cert-file ca-cert-file]
  (let [key-manager-factory   (key-manager-factory (las/key-store key-file cert-file))
        trust-manager-factory (trust-manager-factory (las/trust-store ca-cert-file))]
    (fn build-context []
      (.build
        (doto (SslContextBuilder/forServer key-manager-factory)
          (.sslProvider SslProvider/JDK)
          (.trustManager ^TrustManagerFactory trust-manager-factory)
          #_(.keyManager   key-manager-factory)))))))

#?(:clj
(defn ssl-context:netty
  "Given a PKCS8 key file, a certificate file, and a trusted CA certificate
  used to verify peers, returns a Netty SSLContext."
  [key-file cert-file ca-cert-file]
  ((ssl-context-generator:netty key-file cert-file ca-cert-file))))


#?(:clj
(defn ssl-context [type key-file cert-file ca-cert-file]
  (condp = type
    :std   (las/ssl-context   key-file cert-file ca-cert-file)
    :netty (ssl-context:netty key-file cert-file ca-cert-file))))