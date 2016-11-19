(ns quantum.apis.amazon.cloud-drive.core
           (:refer-clojure :exclude [meta reduce val assoc!])
           (:require [clojure.core.async                   :as async
                       :refer [<!]]
                     [quantum.auth.core                    :as auth]
                     [quantum.core.convert                 :as conv]
                     [quantum.apis.amazon.cloud-drive.auth :as amz-auth]
                     [quantum.core.string                  :as str]
                     [quantum.net.http                     :as http]
                     [quantum.core.paths                   :as paths]
                     [quantum.core.collections             :as coll
                       :refer [kmap join reduce map+ val assoc-if]]
                     [quantum.core.error
                       :refer [TODO ->ex]]
                     [quantum.core.fn                      :as fn
                       :refer [<- fn-> fn->>]]
                     [quantum.core.logic                   :as logic
                       :refer [nnil? eq?]]
                     [quantum.core.log                     :as log
                       :include-macros true]
                     [quantum.core.validate                :as v
                       :refer [validate]])
  #?(:cljs (:require-macros
                     [cljs.core.async.macros
                       :refer [go]]))
  #?(:clj  (:import  [java.nio.file Files Paths])))

; AMZ says: The response to the getEndpoint request for each customer should be cached for three to five days. You should not send a getEndpoint method request daily.
; (http/request! {:url "https://drive.amazonaws.com/drive/v1/account/endpoint" :oauth-token ...})
(def base-urls
  {:meta    "https://cdws.us-east-1.amazonaws.com/drive/v1/"
   :content "https://content-na.drive.amazonaws.com/cdproxy/"})

(def username nil)
(defn ^:cljs-async request!
  "Possible inputs are:
    :account/info
    :account/quota
    :account/endpoint
    :account/usage
    :nodes"
  ([url-type k] (request! :meta (str/->path (namespace k) (name k)) nil))
  ([url-type k {:keys [append method query-params body multipart]
       :or {method :get
            query-params {}}}]
   (log/pr ::debug "AMAZON REQUEST:" (kmap k url-type append method query-params method))
    (#?(:clj  identity
        :cljs go)
      (-> {:url (if append
                    (str/->path (get base-urls url-type) (name k) append)
                    (str/->path (get base-urls url-type) (name k)))
           :method method
           :query-params query-params
           :handlers
            {401 (fn [req resp]
                   (amz-auth/refresh-token! username)
                   (http/request!
                     (assoc req :oauth-token
                       (auth/access-token :amazon :cloud-drive))))}
           :oauth-token (let [token (auth/access-token :amazon :cloud-drive)]
                           (assert (nnil? token))
                           token)}
           (merge (when body      {:body      (conv/->json body)})
                  (when multipart {:multipart multipart}))
           http/request!
          #?(:cljs <!)))))

(defn ^:cljs-async used-bytes []
  (#?(:clj  identity
      :cljs go)
    (->> (request! :meta :account/usage)
         #?(:cljs <!) :body
         (<- select-keys [:other :doc :photo :video])
         (map+ val)
         (map+ :total)
         (map+ :bytes)
         (join [])
         (reduce + 0)
         #_(<- uconv/convert :bytes :gigabytes)
         #_(:clj double))))

; (defn upload! []
; ;   upload  POST : {{contentUrl}}/nodes Upload a new file & its metadata
; ; overwrite PUT : {{contentUrl}}/nodes/{id}/content Overwrite the content of a file
; )

(defn download! [id]
  (request! :content :nodes {:method :get :append (conv/->path id "content")}))

#?(:clj
(defn download-to-file!
  {:usage '(download-to-file! "2gg_3MaYTS-CA7PaPfbdow"
             [:home "Downloads" "download.jpg"])}
  [id file]
  (-> id download!
      (Files/copy
        (-> file conv/->path (Paths/get (into-array [""])))
        (make-array java.nio.file.CopyOption 0)))))

; ; https://forums.developer.amazon.com/forums/message.jspa?messageID=15671
; ; As of right now permanently deleting content is not available through the Amazon Cloud Drive API.
; (defn trash!    [id] (request! :trash :meta {:method :post :append id}))
; (defn untrash!  [id] (request! :trash :meta {:method :post :append (io/path id "restore")}))

(defn ^:cljs-async root-folder []
  (#?(:clj  identity
      :cljs go)
   (-> (request! :meta :nodes
         {:method :get
          :query-params {:filters "isRoot:true"}})
       #?(:cljs <!) :body
       :data
       first))) ; :id

(defn ^:cljs-async trashed-items []
  (#?(:clj  identity
      :cljs go)
    (-> (request! :meta :trash {:method :get})
        #?(:cljs <!) :body)))

(defn ^:cljs-async children
  "Gets the children of an Amazon Cloud Drive @id."
  [id]
  (#?(:clj  identity
      :cljs go)
    (-> (request! :meta :nodes {:append (conv/->path id "children")})
        #?(:cljs <!) :body
        :data)))

; #_(->> (root-folder) :id children (map (juxt :id :name)))

(defn ^:cljs-async meta [id]
  (#?(:clj  identity
      :cljs go)
    (-> (request! :meta :nodes
          {:append id
           :method :get
           :query-params {"tempLink" true}})
        #?(:cljs <!) :body)))

(defn ^:cljs-async assoc!
  {:usage `[(assoc! {:path ["hab497rtds-d_a2gneg" "MyFolderName"] :type :folder})
            (assoc! ["hab497rtds-d_a2gneg" "File.mp3"] (->file "~/abcde.mp3") {:deduplication? false})]}
  ([opts] (assoc! (:path opts) (:data opts) opts))
  ([path data] (assoc! path data nil))
  ([path data {:keys [type overwrite? deduplication?]
               :or   {overwrite? false deduplication? true}
               :as   opts}]
    (validate overwrite? not) ; TODO for now
    (let [_ (validate type (v/or* nil? (eq? :folder)))
          _ (validate path   (v/and vector? (fn-> count (= 2))))
          [parent node-name] path
          _ (validate node-name string?
                      parent    (v/or* string? nil? (eq? :root)))
          meta- (-> {:kind (if (= type :folder) "FOLDER" "FILE")
                     :name node-name}
                    (assoc-if (constantly (string? parent)) :parents [parent]))]
      (request! :content :nodes
        (-> {:method :post}
            (merge
              (if (= type :folder)
                  {:body meta-}
                  {:query-params (when (false? deduplication?) {:suppress "deduplication"})
                   :multipart
                    [{:name      "metadata"
                      :mime-type "application/json"
                      :encoding  "UTF-8"
                      :content   (conv/->json meta-)}
                     {:name      "content" :filename node-name
                      :mime-type "application/octet-stream"
                      :content   (conv/->input-stream data)}]})))))))

; ; The ice-cast stream doesn't include a Content-Length header
; ; (because you know, it's a stream), so this was causing libfxplugins
; ; to crash as in my previous post on the subject.
; (defn cd
;   "|cd| as in Unix."
;   [id]
;   (->> id children
;        (map (juxt :id :name))
;        (sort-by (MWA second))))
