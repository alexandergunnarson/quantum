(ns
  ^{:doc "A Clojure API for Google Drive.
          Has some interesting ideas, but is old and needs re-implementation,
          or at least a lot of code examining and rewriting.

          Has a |drive-map| function which maps in parallel go-blocks
          the user's Google Drive, like DaisyDisk for Mac.

          Other applications pending."
    :attribution "Alex Gunnarson"}
  quantum.apis.google.drive.core
  (:require-quantum [:lib http auth])
  (:require 
    [quantum.apis.google.auth :as gauth]))
;___________________________________________________________________________________________________________________________________
;======================================================{     UNIVERSAL      }=======================================================
;======================================================{                    }=======================================================
(def drive-files-uri  "https://www.googleapis.com/drive/v2/files/")
(def drive-upload-uri "https://www.googleapis.com/upload/drive/v2/files/")
(def api-auth (:api-auth gauth/urls))

; Full list of scopes for the google drive api: https://developers.google.com/drive/scopes.
(assoc! gauth/scopes :drive
  {; Full, permissive scope to access all of a user's files.
   :all                (io/path api-auth "drive")
   ; Per-file access to files created or opened by the app
   :file               (io/path api-auth "drive.file")
   :read-only          (io/path api-auth "drive.readonly")
   :read-only-metadata (io/path api-auth "drive.readonly.metadata")
   :apps-read-only     (io/path api-auth "drive.apps.readonly")
    ; Special scope used to let users approve installation of an app
   :install            (io/path api-auth "drive.install")
   ; Allows access to the Application Data folder
   :app-data           (io/path api-auth "drive.appdata") 
   ; Allows access to Apps Script files
   :scripts            (io/path api-auth "drive.scripts")})

;___________________________________________________________________________________________________________________________________
;================================================={      USING ONLY CLJ-HTTP      }=================================================
;================================================={                               }=================================================
; https://developers.google.com/drive/v2/reference/files/
(def last-response (atom nil))

; With updates that use the HTTP PUT verb, the request fails if you
; don't supply required parameters, and it clears previously set
; data if you don't supply optional parameters.
; It's much safer to use patch for this reason. 
;___________________________________________________________________________________________________________________________________
;================================================={              LOG              }=================================================
;================================================={                               }=================================================
(def request-times-log (atom ()))
;___________________________________________________________________________________________________________________________________
;================================================={       AUX REQUEST-MAKING      }=================================================
;================================================={                               }=================================================
(defn method+url-fn
  {:todo  ["INSERT metadata?"
           "How to add folder?"]
   :usage "(method+url-fn :query \"root\" nil nil)"
   :out   "[:get \"https://www.googleapis.com/drive/v2/files/\"]"}
  [func id to method]
  (condp = func
    :add
    [:post drive-upload-uri]  ; INSERT ; Maximum file size: 1024GB
    :copy
    (condp = to
      :in-place [:post (str drive-files-uri id "/copy")] ; COPY
      ) ; COPY FILE TO SPECIFIED FOLDER ; CATCH NOT_FOLDER EXCEPTIONS
    :meta
    [:get (str drive-files-uri id)]
    :mod
    (condp = method
      :patch    [:patch (str drive-files-uri  id)] ; PATCH ; Don't use UPDATE with metadata or else you'll edit the whole thing
      :update   [:put   (str drive-upload-uri id)] ; UPDATE
      (throw+ {:msg "Invalid method for function :mod."}))
    :move
    (condp = to
      :remove   [:delete (str drive-files-uri id         )]   ; DELETE
      :trash    [:post   (str drive-files-uri id "/trash")]   ; TRASH
      :untrash  [:post   (str drive-files-uri id "/untrash")] ; UNTRASH
      ) ; MOVE FILE TO SPECIFIED FOLDER ; CATCH NOT_FOLDER EXCEPTIONS
    :query
    [:get drive-files-uri]
    (throw+ {:msg "Invalid function requested of Drive."})))

(defn query-params-fn
  "Normalizes REST (HTTP request) parameters passed.
   This involves converting all parameters to strings and camelcasing keys if necessary."
  {:in  '{:q "'root' in parents", :hidden false, :trashed false}
   :out '{"q" "'root' in parents", "hidden" "false", "trashed" "false"}}
  [^Map params]
  (->> params
       (map+ (fn [k v]
               [(whenf k keyword?
                  (fn-> name (str/camelcase :cap-first false)))
                (str v)]))
       foldm))

(defn make-request
  "Creates an HTTP request."
  {:usage "(make-request :query \"root\" nil :get {} nil)"}
  [email ^Key func ^String id to method ^Map params req]
  (let [[http-method url] (method+url-fn func id to method)
        params-f
          (whenf params (fn-> :q (= :children))
             (f*n assoc :q (str "'" id "' in parents")))
        query-params (query-params-fn params-f)
        request
          {:method       http-method
           :url          url
           :oauth-token  (gauth/access-key email :drive :current)
           :as           :auto
           :query-params query-params}]
    request))

;___________________________________________________________________________________________________________________________________
;================================================={     PROCESS HTTP REQUEST      }=================================================
;================================================={                               }=================================================
(defn drive
  "DESC:
     In the *request body*, supply a Files resource with the following properties as the metadata.
     For more information, see the document on media upload.
     The starred (*) keys are required. (?)
   OPTIONS:
     :add   - {:params {*:uploadType :media/:multipart/:resumable}, :req {...}}  INSERT+
     :copy  - {*:id, *:to :in-place/\"folder-id\",                               COPY+
                :params {...}, :request {:file, :meta}} 
     :meta  - {*:id,  :params  {...}}                                            GET+
     :mod   - {*:id, *:method :patch, :params {...}, :req {...}},                PATCH+
              {*:id, *:method :update,                                           UPDATE+
                :params {*:uploadType :media/:multipart/:resumable}, :req {...}}
     :move  - {*:id, *:to  :remove/:trash/:untrash/\"folder-id\"}                DELETE, EMPTYTRASH, TRASH, UNTRASH
     :query - { :id,  :params  {:q :children, :trashed, :hidden,                 LIST+
                                :max-results, :page-token}}"
  {:usage '(drive :query
                  :id     id
                  :params {:q :children :trashed false :hidden false})}
  [func & {:keys [id to method params req raw?] :as args :or {raw? false}}]
  (let [[http-method url] (method+url-fn func id to method)
        request   (make-request func id to method params req)
        log-entry (atom (HTTPLogEntry. []))
        raw-response (gauth/handled-request! request)
        response
          (if raw?
              raw-response
              (-> raw-response :body
                  (json/parse-string keyword)
                  (whenf (constantly (= func :query)) :items)))]
    (reset! last-response response)
    response))

(defn get-children
  "Retrieves the metadata of the children of a folder or file with the supplied ID."
  [^String id]
  (drive :query :id id :params {:q :children :trashed false :hidden false}))

#_(defn eval-drive-page [email]
  (let [str-code
         (->> (http/request!
                {:query-params {"alt""media"}
                 :url (->> (drive :query
                             :id           "root"
                             :params       {:q :children :trashed false :hidden false})
                           (ffilter (fn-> :title (= "to-eval.clj")))
                           :selfLink)
                 :method :get
                 :oauth-token  (gauth/access-key email :drive :current)})
                 :body ("s" ->bytes (conv/->str)))
        ns-0 *ns*]
    (try (-> str-code read-string eval) ; TODO read string is dangerous
      (finally (in-ns (ns-name ns-0))))))
            
; the go block threads are "hogged" by the long running IO operations.
; The situation can be improved by switching the go blocks to normal threads.

; (->> (get-children "root")
;      (map+ (compr :id get-children))
;      foldp+ ; even foldp-max+ doesn't really work as well... hmm...
;      !)

; THREAD MODEL
; It's still not clear whether /foldp+/, /foldp-max+/, /concur-go/, or /concur/ (via /drive-map/) is faster.
; Maybe it would be better simply to get all the metadata (the "expensive" process) and "familify" the files from there.


; (do (reset! file-lists (list (io/read :in :resources :file-name "Unknowns/Bare File List to Depth 10.cljx")))
;     nil) ; ((6)(5)(4)(3)(2)(1)(0))
; (def *drive-max-threads* 50)
; ; 1000 never
; ; 
; (time (doseq [depth-n (range 6 (inc 6))] ; what happens when there's nothing?
;   (reset! file-lists (conj @file-lists (file-list-bare depth-n :thread (first @file-lists))))
;   (io/write! :in :resources :file-name (str "Bare File List to Depth " depth-n) :data (first @file-lists))
;   (println "Items in depth" depth-n ":" (count (first (first @file-lists))))))
; ; req/sec is calc'd from the request-count above
; ; sec/req is based on thread-count
; ; or maybe have unlimited threads but just waiting?
; ; rt 1    |
; ; 0  17   | 25.8s through 4
; ; 1  35   |
; ; 2  114  |
; ; 3  426  |
; ; 4  899  |
; ; 5  4163 | 69.6s  / 1.16m | 12 threads | 12.92 req/sec | 0.93 sec/req | 1.08 req/sec/thread
; ;         | 57.1s          | 30 threads |               |              | 
; ; 6  4720 | 188.4s / 3.14m | 12 threads | 22.10 req/sec | 0.54 sec/req | 1.85 req/sec/thread
; ;         | 126.8s (5+6)   | "1000" thr | 39.92 req/sec |              |
; ; 7  4450 | 185.3s / 3.09m | 15 threads | 25.47 req/sec | 0.59 sec/req | 1.70 req/sec/thread
; ;         | 81.0s          | 100 thr.ds |
; ; 8  6470 | 146.1s / 2.44m | 20 threads | 30.46 req/sec | 0.66 sec/req | 1.52 req/sec/txrhread
; ;    4160 | 152.5s (7+8)   | "1000" thr | 60.13 req/sec | Uncaught exceptions 
; ;    6466 | 80.3s          | 100 thr.ds |
; ; 9  6901 | 127.1s / 2.12m | 30 threads | 50.90 req/sec | 0.59 sec/req | 1.70 req/sec/thread
; ;    2210 | 41.9s          | "1000" thr | 99.28 req/sec | Uncaught exceptions
; ;    6901 | 113.9s         | 100 thr.ds |
; ; 10 5324 | 110.1s / 1.84m | 40 threads | 62.68 req/sec | 0.64 sec/req | 1.57 req/sec/thread 
; ; 11 746  | 195.4s / 3.26m | 30 threads | 27.25 req/sec | 1.10 sec/req | 0.91 req/sec/thread
; ; 12 501  | 19.3s          | 30 threads | 25.92 req/sec | 0.78 sec/req | 1.28 req/sec/thread
; ; 13 479  | 85.2s through 27
; ; 14 628
; ; 15 446
; ; 16 43
; ; 17 20
; ; ============================== POST-RETRIEVAL =============
; (defn filter-files [key- val-]
;   (map #(name (first %))
;     (filter #(= (key- (val %)) val-)
;     (clean-file-list @file-list-depth-0))))
; (comment
; (drive :move :id "0B0tmVWAxHVsQRVRBQkpvLXhVMjQ" :to :trash)
; (drive :copy :id "0B0tmVWAxHVsQRVRBQkpvLXhVMjQ" :to :in-place)
; (drive :meta :id "0B0tmVWAxHVsQRVRBQkpvLXhVMjQ")
; (concur-go 12 ; kind of like a thread-for-each
;   #(drive :move :id % :to :trash)
;   (filter-files :title "My Test Document")))
; (defn item-query [func drive-dir]
;   (coll/tree-filter
;       #(and (vector?   %)
;             (= 2       (count %))
;             (keyword?  (first %)) ; id
;             (map?      (second %)) ; id-meta
;             (contains? (second %) :title))
;       #(func (second %))
;       drive-dir))
; (map (comp (juxt (comp count first)
;                  (comp identity second))
;            list) @unfamilified-list (reverse (range 1 5))) 
; (-> (item-query :file-extension @drive-map-4)
;     frequencies
;     (#(sort-by val %))
;     pprint)
; (-> (item-query :file-size @drive-map-4)
;     (#(map (fn [elem] (if (nil? elem) 0 (/ (read-string elem) (* 1024M 1024M)))) %)) ; don't do this until output...
;     (#(apply + %))
;     pprint)
; (-> (item-query :modified-by-me-date @drive-map-4)
;     (#(filter (complement nil?) %))
;     (#(map (comp time-coerce/to-long time-form/parse)
;            %))
;     sort
;     (#(map (fn [date-num] 
;              (time-form/unparse (time-form/formatter "MM/dd/yyyy")
;                (time-coerce/from-long date-num)))
;            %))
;     frequencies
;     (#(sort-by val %))
;     pprint)
; (-> (item-query (juxt :title :image-media-metadata) @drive-map-4)
;     (#(filter (comp (complement nil?) second) %))
;     (#(map (juxt first
;                  (comp (juxt :width :height) second))
;            %))
;     (#(sort-by (comp first second) %))
;     pprint)
; (defn create-file []
;   (let [body (File.)]
;     (doto body
;       (.setTitle "My Test Document")
;       (.setDescription "A test document. For testing!")
;       (.setMimeType "text/plain"))
;     body))
; (defn test-place-file []
;   (time
;     (let [body (insert-file) 
;           file-content (clojure.java.io/file "README.md") ; The local file.
;           media-content (new FileContent "text/plain" file-content) ; Concretizes AbstractInputStreamContent. Generates repeatable input streams based on the contents of a file.
;           file (-> (.files @drive-client'current) ; Accesses the "files" method of the Drive client
;                    (.insert         ; Put a file on the root folder of the drive
;                      body           ; The file itself
;                      media-content) ; Serializes HTTP request content from an input stream into an output stream.
;                    (.execute))]
;       (println "File ID: " (.getId file)))))



; (defn drive-date [date & {:keys [format] :or {format "MM/dd/yyyy"}}]
;   (time-form/unparse (time-form/formatter format) (time-form/parse date)))

; Using your newfound filesystem, move some files around.
; Try going through some data visualization libraries for Clojure and display the folders in a visually interesting way.
;___________________________________________________________________________________________________________________________________
;======================================================{  ADDITIONAL INFO   }=======================================================
;======================================================{                    }=======================================================
; https://console.developers.google.com/

; (def default-file-template
;   {:kind                       "drive#file"  ; The type of file. This is always drive#file.
;    :id                         "<string>"    ; The ID of the file.
;    :etag                       etag          ; ETag of the file.
;    ; Links     
;    :selfLink                   "<string>"    ; A link back to this file.
;    :webContentLink             "<string>"    ; A link for downloading the content of the file in a browser using cookie based authentication. In cases where the content is shared publicly, the content can be downloaded without any credentials.
;    :webViewLink                "<string>"    ; A link only available on public folders for viewing their static web assets (HTML, CSS, JS, etc) via Google Drive's Website Hosting.
;    :alternateLink              "<string>"    ; A link for opening the file in using a relevant Google editor or viewer.
;    :embedLink                  "<string>"    ; A link for embedding the file.
;    :openWithLinks {(key):      "<string>"}   ; A map of the id of each of the user's apps to a link to open this file with that app. Only populated when the drive.apps.readonly scope is used.
;    :defaultOpenWithLink        "<string>"    ; A link to open this file with the user's default app for this file. Only populated when the drive.apps.readonly scope is used.
;    :iconLink                   "<string>"    ; A link to the file's icon.
;    :thumbnailLink              "<string>"    ; A link to the file's thumbnail.
;    :thumbnail                                ; Thumbnail for the file. Only accepted on upload and for files that are not already thumbnailed by Google.
;      {:image                    <bytes>      ; The URL-safe Base64 encoded bytes of the thumbnail image.
;       :mimeType                "<string>"} 
;    :title                      "<string>"    ; WRITABLE. The title of the this file. Used to identify file or folder name. (https://developers.google.com/drive/folder)
;    :mimeType                   "<string>"    ; WRITABLE. The MIME type of the file. This is only mutable on update when uploading new content. This field can be left blank, and the mimetype will be determined from the uploaded content's MIME type.
;    :description                "<string>"    ; WRITABLE. A short description of the file.
;    :labels                                   ; A group of labels for the file.
;      {:starred                  <boolean>    ; WRITABLE. Whether this file is starred by the user.
;       :hidden                   <boolean>    ; WRITABLE. Deprecated.
;       :trashed                  <boolean>    ; WRITABLE. Whether this file has been trashed.
;       :restricted               <boolean>    ; WRITABLE. Whether viewers are prevented from downloading this file.
;       :viewed                   <boolean>}   ; WRITABLE. Whether this file has been viewed by this user.
;    :createdDate                 <datetime>   ; Create time for this file (formatted ISO8601 timestamp).
;    :modifiedDate                <datetime>   ; WRITABLE. Last time this file was modified by anyone (formatted RFC 3339 timestamp). This is only mutable on update when the setModifiedDate parameter is set.
;    :modifiedByMeDate            <datetime>   ; Last time this file was modified by the user (formatted RFC 3339 timestamp). Note that setting modifiedDate will also update the modifiedByMe date for the user which set the date.
;    :lastViewedByMeDate          <datetime>   ; WRITABLE. Last time this file was viewed by the user (formatted RFC 3339 timestamp).
;    :markedViewedByMeDate        <datetime>   ; WRITABLE. Time this file was explicitly marked viewed by the user (formatted RFC 3339 timestamp).
;    :sharedWithMeDate            <datetime>   ; Time at which this file was shared with the user (formatted RFC 3339 timestamp).
;    :version                     <long>       ; A monotonically increasing version number for the file. This reflects every change made to the file on the server, even those not visible to the requesting user.
;    :sharingUser                              ; User that shared the item with the current user, if available.
;      {:kind                    "drive#user"  ; This is always drive#user.
;       :displayName             "<string>"    ; A plain text displayable name for this user.
;       :picture                               ; The user's profile picture. 
;         {url                   "<string>"}   ; A URL that points to a profile picture of this user.
;       :isAuthenticatedUser      <boolean>    ; Whether this user is the same as the authenticated user for whom the request was made.
;       :permissionId            "<string>"    ; The user's ID as visible in the permissions collection.
;       :emailAddress            "<string>"}   ; The email address of the user.
;    :parents [parents_Resource]               ; WRITABLE. Collection of parent folders which contain this file.
;                                              ; Setting this field will put the file in all of the provided folders.
;                                              ; On insert, if no folders are provided, the file will be placed in the default root folder.
;                                              ; https://developers.google.com/drive/v2/reference/parents#resource
;    :downloadUrl                "<string>"    ; Short lived download URL for the file. This is only populated for files with content stored in Drive.
;    :exportLinks {(key):        "<string>"}   ; Links for exporting Google Docs to specific formats. ; (key): A mapping from export format to URL
;    :indexableText                            ; Indexable text attributes for the file. This property can only be written, and is not returned by files.get. For more information, see Saving indexable text (https://developers.google.com/drive/practices#saving_indexable_text).
;      {:text                    "<string>"}   ; WRITABLE. The text to be indexed for this file.
;    :userPermission permissions_Resource      ; The permissions for the authenticated user on this file. https://developers.google.com/drive/v2/reference/permissions#resource
;    :permissions [permissions_Resource]       ; The list of permissions for users with access to this file.
;    :originalFilename           "<string>"    ; The original filename if the file was uploaded manually, or the original title if the file was inserted through the API. Note that renames of the title will not change the original filename. This will only be populated on files with content stored in Drive.
;    :fileExtension              "<string>"    ; The file extension used when downloading this file. This field is read only. To set the extension, include it in the title when creating the file. This is only populated for files with content stored in Drive.
;    :md5Checksum                "<string>"    ; An MD5 checksum for the content of this file. This is populated only for files with content stored in Drive.
;    :fileSize                    <long>       ; The size of the file in bytes. This is only populated for files with content stored in Drive.
;    :quotaBytesUsed              <long>       ; The number of quota bytes used by this file.
;    :ownerNames ["<string>"]                  ; Name(s) of the owner(s) of this file.
;    :owners                                   ; The owner(s) of this file.
;      [{:kind                   "drive#user"  ; This is always drive#user.
;        :displayName            "<string>"    ; A plain text displayable name for this user.
;        :picture                              ; The user's profile picture. 
;          {:url                 "<string>"}   ; A URL that points to a profile picture of this user.
;        :isAuthenticatedUser     <boolean>    ; Whether this user is the same as the authenticated user for whom the request was made.
;        :permissionId           "<string>"    ; The user's ID as visible in the permissions collection.
;        :emailAddress           "<string>"}]  ; The email address of the user.
;      :lastModifyingUserName    "<string>"    ; Name of the last user to modify this file.
;      :lastModifyingUser                      ; The last user to modify this file.
;        {:kind                  "drive#user"  ; This is always drive#user.
;         :displayName           "<string>"    ; A plain text displayable name for this user.
;         :picture                             ; The user's profile picture.
;           {:url                "<string>"    ; A URL that points to a profile picture of this user.
;            :isAuthenticatedUser <boolean>    ; Whether this user is the same as the authenticated user for whom the request was made.
;            :permissionId       "<string>"    ; The user's ID as visible in the permissions collection.
;            :emailAddress       "<string>"}}  ; The email address of the user.
;    :editable                    <boolean>    ; Whether the file can be edited by the current user.
;    :copyable                    <boolean>    ; Whether the file can be copied by the current user.
;    :writersCanShare             <boolean>    ; WRITABLE. Whether writers can share the document with other users.
;    :shared                      <boolean>    ; Whether the file has been shared.
;    :explicitlyTrashed           <boolean>    ; Whether this file has been explicitly trashed, as opposed to recursively trashed. This will only be populated if the file is trashed.
;    :appDataContents             <boolean>    ; Whether this file is in the appdata folder.
;    :headRevisionId             "<string>"    ; The ID of the file's head revision. This will only be populated for files with content stored in Drive.
;    :properties [properties_Resource]         ; WRITABLE. The list of properties. https://developers.google.com/drive/v2/reference/properties#resource
;    :imageMediaMetadata                       ; Metadata about image media. This will only be present for image types, and its contents will depend on what can be parsed from the image content.
;      {:width                    <integer>    ; The width of the image in pixels.
;       :height                   <integer>    ; The height of the image in pixels.
;       :rotation                 <integer>    ; The rotation in clockwise degrees from the image's original orientation.
;       :location                              ; Geographic location information stored in the image.
;         {:latitude              <double>     ; The latitude stored in the image.
;          :longitude             <double>     ; The longitude stored in the image.
;          :altitude              <double>}    ; The altitude stored in the image.
;       :date                    "<string>"    ; The date and time the photo was taken (EXIF format timestamp).
;       :cameraMake              "<string>"    ; The make of the camera used to create the photo.
;       :cameraModel             "<string>"    ; The model of the camera used to create the photo.
;       :exposureBias             <float>      ; The exposure bias of the photo (APEX value).
;       :exposureMode            "<string>"    ; The exposure mode used to create the photo.
;       :exposureTime             <float>      ; The length of the exposure, in seconds.
;       :aperture                 <float>      ; The aperture used to create the photo (f-number).
;       :flashUsed                <boolean>    ; Whether a flash was used to create the photo.
;       :focalLength              <float>      ; The focal length used to create the photo, in millimeters.
;       :isoSpeed                 <integer>    ; The ISO speed used to create the photo.
;       :meteringMode            "<string>"    ; The metering mode used to create the photo.
;       :sensor                  "<string>"    ; The type of sensor used to create the photo.
;       :colorSpace              "<string>"    ; The color space of the photo.
;       :whiteBalance            "<string>"    ; The white balance mode used to create the photo.
;       :maxApertureValue         <float>      ; The smallest f-number of the lens at the focal length used to create the photo (APEX value).
;       :subjectDistance          <integer>    ; The distance to the subject of the photo, in meters.
;       :lens                    "<string>"}}) ; The lens used to create the photo.

