(ns quantum.apis.google.contacts
  (:require-quantum [http auth])
  (:require [quantum.apis.google.auth :as gauth]))

(assoc! gauth/scopes :contacts
  {:read-write "https://www.google.com/m8/feeds"
   :read       "https://www.googleapis.com/auth/contacts.readonly"})

(defn ^Vec parse-contacts
  {:todo ["Use xml/parse instead of lparse"]
   :out '[{:name "Lynette Bearinger",
           :image-link "https://www.google.com/m8/feeds/photos/media/alexandergunnarson%40gmail.com/5fd4c0328f27c544",
           :email "journeypartner67@gmail.com"}
          ...]}
  [^String xml-response]
  (let [^Map parsed-body
          (->> xml-response
               xml/lparse
               :content)
        ^Fn filter-relevant-entries
          (fn->> :content
                 (filter+ (fn-> :tag (splice-or = :title :email :link)))
                 (remove+ (fn-and (fn-> :tag (= :link))
                                  (fn-> :attrs :rel (not= "http://schemas.google.com/contacts/2008/rel#photo"))))
                 redv)
        ^Fn unify-info
          (fn [elem]
            (let [^String name-0
                     (->> elem
                          (ffilter (fn-> :tag (= :title)))
                          :content
                          first)
                  ^String image-link
                     (->> elem 
                          (ffilter (fn-> :tag (= :link)))
                          :attrs
                          :href)
                  ^String email
                     (->> elem
                          (ffilter (fn-> :tag (= :email)))
                          :attrs
                          :address)]
              {:name       name-0
               :image-link image-link
               :email      email}))]
    (->> parsed-body
         (filter+ (fn-> :tag (= :entry)))
         (map+ filter-relevant-entries)
         (map+ unify-info)
         redv)))

(defn ^String retrieve-contacts-xml
  {:todo ["Handle unreasonably long contacts (> 10000)"]}
  [^String email]
  (let [^Map http-response
         (gauth/handled-request! email :contacts
           {:method       :get
            :url          (str "https://www.google.com/m8/feeds/contacts/" email "/full")
            :oauth-token  (gauth/access-key email :contacts :offline)
            :headers      {"GData-Version" "3.0"}
            :query-params {"max-results" 10000}})] ; TODO check if 10000 is too many
    (:body http-response)))

(defn ^Vec contacts
  [^String email]
  (->> (retrieve-contacts-xml email)
       parse-contacts))

(defn ^Vec update-contacts
  [^String email f]
  (->> (retrieve-contacts-xml email)
       xml/lparse :content 
       (filter (fn-> :tag (= :entry)))
       (postwalk (if*n (fn-and keyword? (fn-> namespace nnil?))
                       (fn [k] (str (namespace k) ":" (name k))) ; If you don't do this, XML emission messes up: http://dev.clojure.org/jira/browse/DXML-15
                       f))))

(defn contact-id [contact]
  (->> contact :content
       (ffilter (fn-> :tag (= :id)))
       :content first
       (coll/taker-until (eq? \/))))

#_(defn update-contact!
  {:todo ["Some etag troubles"]}
  [contact]
  (let [etag (-> contact :attrs (get "gd:etag")) ; assumes keyword namespaces have been taken care of
        contact-id* contact-id]
    (gauth/handled-request! email :contacts
      {:method :put
       :url (str "https://www.google.com/m8/feeds/contacts/" email "/full/" contact-id*)
       :headers      {"GData-Version" "3.0"
                      "If-Match"      etag
                      "Content-Type"  "application/atom+xml"}
       :body (-> contact clojure.data.xml/emit-str)})))


(defn delete! [email contact-id & [opts]]
  (gauth/handled-request! email :contacts
    (mergel opts
      {:method :delete
       :url (str "https://www.google.com/m8/feeds/contacts/" email "/full/" contact-id)
       :headers {"If-Match" (or (:etag opts) "*")}}))) ; If "*", then it's overwritten no matter what

(defn create! [email contact & [opts]]
  (if (string? contact)
      (gauth/handled-request! email :contacts
        (mergel opts
          {:method :post
           :url (str "https://www.google.com/m8/feeds/contacts/" email "/full")
           :headers {"GData-Version" "3.0"
                     "Content-Type"  "application/atom+xml"}
           :body contact}))
      (throw+ (Err. nil "Non-XML contact creation not yet supported" nil))))