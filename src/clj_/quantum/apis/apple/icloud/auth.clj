(ns quantum.apis.apple.icloud.auth
  (:require-quantum [:lib http auth]))

; https://developer.apple.com/library/prerelease/ios/documentation/DataManagement/Conceptual/CloutKitWebServicesReference/SettingUpWebServices/SettingUpWebServices.html#//apple_ref/doc/uid/TP40015240-CH24-SW1

; (.get (driver) "http://www.icloud.com")
; (def username-field (web/find-element (driver) (By/xpath "//input[@aria-label='Apple ID']")))
; (web/send-keys! username-field USERNAME)
; (def password-field (web/find-element (driver) (By/xpath "//input[@aria-label='Password']")))
; (web/send-keys! password-field PASSWORD)
; (def login-button (web/find-element (driver) (By/xpath "//div[@title='Sign In']")))
; (web/click-load! login-button)
; (.get (driver) "https://www.icloud.com/#contacts")
; (def options-button (web/find-element (driver) (By/xpath "//div[@title='Show Actions Menu'")))

; (def contacts-button (web/find-element (driver) (By/xpath "//a[contains(@href, 'contacts')]")))
; (web/click! contacts-button)

; "Javascript is required" - but only says that sometimes
; Shows the elements but somehow doesn't have any
; underlying DOM nodes... the weirdest thing...