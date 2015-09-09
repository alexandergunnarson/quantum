(ns quantum.apis.twitter.core
  (:require-quantum [:lib auth http url]))

(defn gen-oauth-signature [{:keys [method url query-params]} auth-params]
  (let [http-method-caps (-> method name str/upper-case)
        ; The OAuth spec says to sort lexigraphically/alphabetically.
        auth-params-sorted auth-params ; (sort-by auth-params key)
        params-string    (url/map->str (into (into (sorted-map) query-params)
                                            auth-params-sorted))
        consumer-secret (auth/datum :twitter :api-secret  )
        oauth-secret    (auth/datum :twitter :oauth-secret)
        signature-base-string
          (-> (str http-method-caps
                "&" (url/encode url)
                "&" (-> params-string url/encode))
              (str/replace "%5F" "_")) ; Don't replace "_" with %5F. Possibly this is for param keys only.
        _ (log/pr ::debug "Base string is:" signature-base-string)
        ; Note that there are some flows, such as when obtaining a request token,
        ; where the token secret is not yet known.
        ; In this case, the signing key should consist of the percent-encoded
        ; consumer secret followed by an ampersand character ‘&’.
        signing-key (str (url/encode consumer-secret) "&"
                         (url/encode oauth-secret))
        signature (-> (crypto/sha-1-hmac signature-base-string signing-key)
                      (crypto/encode :base64)
                      (String.))]
    signature))

(defn request! [{:as request :keys [url method query-params timestamp]}]
  (let [auth-params
          (sorted-map
            "oauth_consumer_key"     (auth/datum :twitter :api-key)
            "oauth_nonce"            (rand/rand-string 32 #{:numeric :upper :lower})
            "oauth_signature_method" "HMAC-SHA1"
            "oauth_timestamp"        (-> (time/now-unix) (uconv/convert :millis :sec) core/int str)
            "oauth_token"            (auth/datum :twitter :oauth-token)
            "oauth_version"          "1.0")
        oauth-signature (gen-oauth-signature request auth-params)
        auth-params-f (assoc auth-params "oauth_signature" oauth-signature)
        sb (seq-loop [kv auth-params-f
                      ret (StringBuilder. "OAuth ")]
             (.append ^StringBuilder ret (key kv))
             (.append ^StringBuilder ret "=")
             (.append ^StringBuilder ret "\"")
             (.append ^StringBuilder ret (url/encode (val kv)))
             (.append ^StringBuilder ret "\","))
        auth-str (-> sb (.deleteCharAt (lasti sb)) str)]
        (log/ppr :user "AUTH-STR" auth-str)
    (http/request!
      (-> request
          (assoc-in [:headers "Authorization"]
            auth-str)))))

#_(def resp
  (request!
    {:url    "https://api.twitter.com/1.1/followers/list.json"
     :method :get
     :query-params
       {"screen_name" "argunnarson"
        "count"       "200"}}))