(ns quantum.apis.microsoft.azure.core
  (:require [quantum.net.http :as http]))

#_(defn url->account-name [url]
  (->> url
       (coll/takel-after "://")
       (coll/takel-until ".blob.core.windows.net")))

#_(defn canonicalize-params [m]
  (->> m
       (map (fn [[k v]] (str (str/str+ k) ":" (str/str+ v))))
       (str/join "\n")))

#_(defn canonicalize-url [url query-params]
  (let [account-name (url->account-name url)
        resource
          (->> url
               (coll/takel-after "blob.core.windows.net")
               (io/path "/" account-name "/"))]
    (str resource "\n" (canonicalize-params query-params))))

#_(defn signing-string
  {:todo ["Change keys to be more Clojure-ey"]}
  ([method url headers]
    (signing-string method url headers nil))
  ([method url headers
    {:keys [query-params
            Content-Encoding
            Content-Language
            Content-Length
            Content-MD5
            Content-Type
            Date
            If-Modified-Since
            If-Match
            If-None-Match
            If-Unmodified-Since
            Range]}]
    (str/join "\n"
      [(-> method name str/upper-case)
       Content-Encoding
       Content-Language
       (or Content-Length 0)
       Content-MD5
       Content-Type 
       Date
       If-Modified-Since
       If-Match
       If-None-Match
       If-Unmodified-Since
       Range
       (canonicalize-params headers) 
       (canonicalize-url url query-params)])))

#_(defn required-headers []
  {"x-ms-date"    nil #_(time/format (time/gmt-now) :windows)
   "x-ms-version" "2014-02-14"})

#_(defn gen-headers
  ([account-key method url] (gen-headers account-key method url nil))
  ([account-key method url {:keys [headers query-params]}]
    (let [secret         (-> account-key crypto/base-64-decode)
          account-name   (url->account-name url)
          headers-f      (mergel headers (required-headers))
          string-to-sign (signing-string method url headers-f
                           {:query-params query-params})
          encoded-str
            (-> string-to-sign (crypto/sha-256-hmac-base-64 secret))]
      (assoc headers-f "Authorization"
        (str "SharedKey" " " account-name ":" encoded-str)))))

#_(defn parse-response [xml]
  (->> xml rest
       (coll/takel-from "<")
       (java.io.StringReader.)
       clojure.data.xml/parse))

#_(defn list-containers
  "<user>/container?restype=container
    &comp=list
    &include=snapshots
    &include=metadata
    &include=uncommittedblobs

   CanonicalizedResource:
    /myaccount/mycontainer\ncomp:list\ninclude:metadata,snapshots,uncommittedblobs\nrestype:container"
  [account-name account-key]
  (let [method :get
        url (str "https://" account-name ".blob.core.windows.net/")
        query-params {"comp" "list"}
        headers (gen-headers account-key method url
                  {:query-params query-params})]
    (-> (http/request!
          {:method       method
           :url          url
           :query-params query-params
           :headers      headers})
        :body 
        parse-response)))

#_(defn create-container!
  "Creates a container."
  ([account-name account-key container-name]
    (create-container! account-name account-key container-name nil))
  ([account-name account-key container-name
    {:as opts :keys [public-access-type]}]
    (throw-unless (str/lower? container-name)
      "Container name must be lower case.")
    (when public-access-type
      (throw-unless (in-k? public-access-type #{:blob :container})
        "Invalid public access type."))
    (let [opts-f (->> {:x-ms-blob-public-access public-access-type}
                      (remove+ (fn-> val nil?))
                      redm)
          method :put
          url (str "https://" account-name ".blob.core.windows.net/" container-name)
          query-params {"restype" "container"}
          headers (gen-headers account-key method url
                    {:query-params query-params :headers opts-f})]
      (http/request!
        {:method       method
         :url          url
         :query-params query-params
         :headers      headers}))))


