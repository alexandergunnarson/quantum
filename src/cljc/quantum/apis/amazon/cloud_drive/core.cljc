(ns quantum.apis.amazon.cloud-drive.core
           (:refer-clojure :exclude [meta])
           (:require [quantum.auth.core                    :as auth    ]
                     [quantum.core.convert                 :as conv    ]
                     [quantum.apis.amazon.cloud-drive.auth :as amz-auth]
                     [quantum.core.string                  :as str     ]
                     [quantum.net.http                     :as http    ]
                     [quantum.core.paths                   :as paths   ]
                     [quantum.core.collections             :as coll    
                       :refer [#?(:clj kmap) redv map+]                ]
                     [quantum.core.fn                      :as fn
                       :refer [#?@(:clj [<-])]                         ]
                     [quantum.core.logic                   :as logic
                       :refer [nnil?]                                  ])
  #?(:cljs (:require-macros
                     [quantum.core.collections             :as coll    
                       :refer [kmap]                                   ]
                     [quantum.core.fn                      :as fn
                       :refer [<-]                                     ]))
  #?(:clj  (:import  [java.nio.file Files Paths])))

(def base-urls
  {:meta    "https://cdws.us-east-1.amazonaws.com/drive/v1/"
   :content "https://content-na.drive.amazonaws.com/cdproxy/"})

(def username nil)
(defn ^:cljs-async request!
  "Possible inputs are:
    :account/info
    :account/quota
    :account/endpoint
    :account/usage"
  ([k url-type] (request! (str/->path (namespace k) (name k)) :meta nil))
  ([k url-type {:keys [append method query-params]
       :or {method :get
            query-params {}}}]
   (println "AMAZON REQUEST:" (kmap k url-type append method query-params method))
    (#?(:clj  identity
        :cljs go)
      (->> (http/request!
            {:url (str/->path (get base-urls url-type) (name k) append)
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
                             token)})
          #?(:cljs <!)
          :body))))

(defn ^:cljs-async used-gb [] 
  (#?(:clj  identity
      :cljs go)
    (->> (request! :account/usage :meta)
         #?(:cljs <!)
         (<- dissoc :lastCalculated)
         (map+ val)
         (map+ :total)
         (map+ :bytes)
         redv
         (quantum.core.reducers/reduce + 0)
         #_(<- uconv/convert :bytes :gigabytes)
         #_(:clj double))))

; (defn upload! []
; ;   upload  POST : {{contentUrl}}/nodes Upload a new file & its metadata
; ; overwrite PUT : {{contentUrl}}/nodes/{id}/content Overwrite the content of a file
; )

(defn download! [id]
  (request! :nodes :content {:method :get  :append (conv/->path id "content")}))

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
   (-> (request! :nodes :meta
         {:method :get
          :query-params {:filters "isRoot:true"}})
       #?(:cljs <!)
       :data
       first))) ; :id

(defn trashed-items []
  (request! :trash :meta {:method :get}))

(defn ^:cljs-async children
  "Gets the children of an Amazon Cloud Drive @id."
  [id]
  (#?(:clj  identity 
      :cljs go)
    (-> (request! :nodes :meta {:append (conv/->path id "children")})
        #?(:cljs <!)
        :data)))

; #_(->> (root-folder) :id children (map (juxt :id :name)))

(defn meta [id]
  (request! :nodes :meta
    {:append id
     :method :get
     :query-params {"tempLink" true}}))


; ; The ice-cast stream doesn't include a Content-Length header
; ; (because you know, it's a stream), so this was causing libfxplugins
; ; to crash as in my previous post on the subject.
; (defn cd
;   "|cd| as in Unix."
;   [id]
;   (->> id children
;        (map (juxt :id :name))
;        (sort-by (MWA second))))