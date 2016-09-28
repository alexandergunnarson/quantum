(ns quantum.apis.google.contacts
  #_(:require-quantum [http auth])
  (:refer-clojure :exclude [assoc!])
  (:require
    [quantum.net.http :as http]
    [quantum.core.collections :as coll
                 :refer [#?@(:clj [assoc!])]
      #?@(:cljs [:refer-macros [assoc!]])]
    [quantum.apis.google.auth :as gauth]))

#_(assoc! gauth/scopes :contacts
  {:read-write "https://www.google.com/m8/feeds"
   :read       "https://www.googleapis.com/auth/contacts.readonly"})

#_(defn parse-contacts
  {:todo ["Use xml/parse instead of lparse"]
   :out `[{:name "Firstname Lastname",
           :image-link "https://www.google.com/m8/feeds/photos/media/...",
           :email "myemail@someone.com"}
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

#_(defn ^String retrieve-contacts-xml
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

#_(defn ^Vec contacts
  [^String email]
  (->> (retrieve-contacts-xml email)
       parse-contacts))

#_(defn ^Vec update-contacts
  [^String email f]
  (->> (retrieve-contacts-xml email)
       xml/lparse :content
       (filter (fn-> :tag (= :entry)))
       (postwalk (if$n (fn-and keyword? (fn-> namespace nnil?))
                       (fn [k] (str (namespace k) ":" (name k))) ; If you don't do this, XML emission messes up: http://dev.clojure.org/jira/browse/DXML-15
                       f))))

#_(defn contact-id [contact]
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


#_(defn delete! [email contact-id & [opts]]
  (gauth/handled-request! email :contacts
    (mergel opts
      {:method :delete
       :url (str "https://www.google.com/m8/feeds/contacts/" email "/full/" contact-id)
       :headers {"If-Match" (or (:etag opts) "*")}}))) ; If "*", then it's overwritten no matter what

#_(defn create! [email contact & [opts]]
  (if (string? contact)
      (gauth/handled-request! email :contacts
        (mergel opts
          {:method :post
           :url (str "https://www.google.com/m8/feeds/contacts/" email "/full")
           :headers {"GData-Version" "3.0"
                     "Content-Type"  "application/atom+xml"}
           :body contact}))
      (throw+ (Err. nil "Non-XML contact creation not yet supported" nil))))
