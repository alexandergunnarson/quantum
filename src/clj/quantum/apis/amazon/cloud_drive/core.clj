(ns quantum.apis.amazon.cloud-drive.core
  (:require-quantum [:lib http auth web uconv url])
  (:require
    [quantum.apis.amazon.cloud-drive.auth :as amz-auth])
  (:import [java.nio.file Files Paths]))

(def base-urls
  {:meta    "https://cdws.us-east-1.amazonaws.com/drive/v1/"
   :content "https://content-na.drive.amazonaws.com/cdproxy/"})

(defn request!
  "Possible inputs are:
    :account/info
    :account/quota
    :account/endpoint
    :account/usage"
  ([k url-type] (request! (io/path (namespace k) (name k)) :meta nil))
  ([k url-type {:keys [append method query-params]
       :or {method :get
            query-params {}}}]
    (http/request!
      {:url (io/path (get base-urls url-type) (name k) append)
       :method method
       :query-params query-params
       :handlers
         {401 (fn [req resp]
                (amz-auth/refresh-token!)
                (http/request!
                  (assoc req :oauth-token
                    (auth/access-token :amazon :cloud-drive))))}
       :oauth-token (auth/access-token :amazon :cloud-drive)
       :parse? true})))

(defn used-gb [] 
  (->> (request! :account/usage :meta)
       (<- dissoc :lastcalculated)
       (map val)
       (map :total)
       (map :bytes)
       (reduce + 0)
       (<- uconv/convert :bytes :gigabytes)
       double)) 

(defn upload! []
;   upload  POST : {{contentUrl}}/nodes Upload a new file & its metadata
; overwrite PUT : {{contentUrl}}/nodes/{id}/content Overwrite the content of a file
)

(defn download! [id] (request! :nodes :content {:method :get  :append (io/path id "content")}))
(defn download-to-file!
  {:usage '(download-to-file! "2gg_3MaYTS-CA7PaPfbdow"
             [:home "Downloads" "download.jpg"])}
  [id file]
  (-> id download! :body
      (Files/copy
        (convert/->path file)
        (make-array java.nio.file.CopyOption 0))))

; https://forums.developer.amazon.com/forums/message.jspa?messageID=15671
; As of right now permanently deleting content is not available through the Amazon Cloud Drive API. 
(defn trash!    [id] (request! :trash :meta    {:method :post :append id}))
(defn untrash!  [id] (request! :trash :meta    {:method :post :append (io/path id "restore")}))

(defn root-folder []
  (-> (request! :nodes :meta
        {:method :get
         :query-params {:filters "isRoot:true"}})
      :data
      first))

(defn trashed-items [] (request! :trash :meta {:method :get}))

(defn children [id]
  (-> (request! :nodes :meta {:append (io/path id "children")})
      :data))

#_(http/request! {:url "https://drive.amazonaws.com/drive/v1/nodes/xuOTgp9CRJOAFRi-5_z1hg/?tempLink=true" :oauth-token (auth/access-token :amazon :cloud-drive) :parse? true})

#_(-> (http/request!
      {:oauth-token (auth/access-token :amazon :cloud-drive)
       :url (io/path "https://content-na.drive.amazonaws.com/cdproxy/nodes" "9zfx3GtgSEOUbI9SC7qvPw" "content")})
    future)

#_(->> (root-folder) :id children (map (juxt :id :name)))



; The ice-cast stream doesn't include a Content-Length header
; (because you know, it's a stream), so this was causing libfxplugins
; to crash as in my previous post on the subject.
