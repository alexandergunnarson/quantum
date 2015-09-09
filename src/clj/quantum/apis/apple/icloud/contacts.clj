(ns quantum.apis.apple.icloud.contacts
  (:require-quantum [:lib http])
  (import ezvcard.Ezvcard))

; For any interaction with iCloud, you have to be a
; "registered developer"... dumb...

; https://developer.apple.com/library/prerelease/ios/documentation/DataManagement/Conceptual/CloutKitWebServicesReference/LookupContacts/LookupContacts.html#//apple_ref/doc/uid/TP40015240-CH13-SW1
(def url-base "https://api.apple-cloudkit.com")
(def protocol-version 1)

; @container A unique identifier for the app’s container. The container ID begins with iCloud..
; @environment The version of the app’s container. Pass development to use the environment not accessible by apps available on the store. Pass production to use the environment accessible by development apps and apps available on the store.
#_(http/request!
  {:method :post
   :url (io/path [url-base "database" protocol-version
                  container environment
                  "public" "users" "lookup" "contacts"])})
