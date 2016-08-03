(ns quantum.net.client.impl)

; CLJS HTTP UTIL

(defn test:basic-auth
  [credentials])

#?(:cljs
(defn test:build-url
  [{:keys [scheme server-name server-port uri query-string] :as req}]))

(defn test:build-headers
  [m])

(defn test:parse-headers [headers])

; CLJS HTTP CORE

(defn test:abort!
  [channel])

(defn test:aborted? [xhr])

(defn test:apply-default-headers!
  [xhr headers])
   
(defn test:build-xhr
  [{:keys [with-credentials? default-headers] :as request}])

(defn test:error-kw [x])

(defn test:xhr
  [{:keys [request-method headers body with-credentials? cancel] :as request}])

(defn test:jsonp
  [{:keys [timeout callback-name cancel] :as request}])

(defn test:parse-query-params
  [s])

(defn test:encode-val [k v])

(defn test:encode-vals [k vs])

(defn test:encode-param [[k v]])

(defn test:generate-query-string [params])

(defn test:decode-body
  [response decode-fn content-type request-method])

; MIDDLEWARE

(defn test:wrap-edn-params
  [client])

(defn test:wrap-edn-response
  [client])

(defn test:wrap-default-headers
  [client & [default-headers]])

(defn test:wrap-accept
  [client & [accept]])

(defn test:wrap-content-type
  [client & [content-type]])

(defn test:wrap-transit-params
  [client])

(defn test:wrap-transit-response
  [client])

(defn test:wrap-json-params
  [client])

(defn test:wrap-json-response
  [client])

(defn test:wrap-query-params [client])

(defn test:wrap-form-params [client])

(defn test:generate-form-data [params])

(defn test:wrap-multipart-params [client])

(defn test:wrap-method [client])

(defn test:wrap-server-name [client server-name])

(defn test:wrap-url [client])

(defn test:wrap-basic-auth
  [client & [credentials]])

(defn test:wrap-oauth
  [client])

(defn test:wrap-channel-from-request-map
  [client])

(defn test:wrap-request
  [req])

; ======== CLOJURE IMPLEMENTATION =========

;___________________________________________________________________________________________________________________________________
;================================================={       NORMALIZE PARAMS        }=================================================
;================================================={                               }=================================================
(defn test:add-part!
  [meb
   {:keys [^String name mime-type encoding content]}])

(defn test:add-header!
  [req [header-name ^String content]])

(defn test:add-headers!
  [req headers])

(defn test:post-multipart!
  [{:keys [^String url multipart
           ^String oauth-token
                   headers]}])

(defn test:request! [x])
