(ns quantum.test.apis.google.contacts
  (:require [quantum.apis.google.contacts :as ns]))

#_(defn test:parse-contacts
  [^String xml-response])

#_(defn ^String test:retrieve-contacts-xml
  [^String email])

#_(defn ^Vec test:contacts
  [^String email]
  (->> (retrieve-contacts-xml email)
       parse-contacts))

#_(defn ^Vec test:update-contacts
  [^String email f]
  (->> (retrieve-contacts-xml email)
       xml/lparse :content
       (filter (fn-> :tag (= :entry)))
       (postwalk (if$n (fn-and keyword? (fn-> namespace nnil?))
                       (fn [k] (str (namespace k) ":" (name k))) ; If you don't do this, XML emission messes up: http://dev.clojure.org/jira/browse/DXML-15
                       f))))

#_(defn test:contact-id [contact])

#_(defn test:update-contact!
  [contact])


#_(defn test:delete! [email contact-id & [opts]])

#_(defn test:create! [email contact & [opts]])
