(ns quantum.apis.facebook.messenger
  (:require
    #?(:clj [hickory.select         :as hs  ])
    #?(:clj [hickory.core           :as hp  ])
    [quantum.core.log               :as log ]
    [quantum.core.data.complex.json :as json]))

#_(defn login-to-messenger! [^WebDriver driver]
  (log/pr :debug "Logging in to Messenger...")
  (.get driver "http://www.messenger.com")
  (web/record-page! driver "Messenger home")

  (let [email-field    (web/find-element driver (By/id "email"))
        password-field (web/find-element driver (By/id "pass"))
        login-btn      (web/find-element driver (By/id "loginbutton"))]
    (web/send-keys! email-field    (auth/datum :facebook :username))
    (web/send-keys! password-field (auth/datum :facebook :password))
    (web/click! login-btn)
    (web/record-page! driver "Messenger login")
    (log/pr :debug "Logged in to messenger.")))

#_(defn chat! [^WebDriver driver ^String text]
  (let [chat-box
         (web/find-element driver
           (By/xpath
             "//*[@role='textbox' and @contenteditable='true']"))
        _ (log/pr :debug "Chat box is" chat-box)]
    (try+ (web/send-keys! chat-box (str text web/kenter))
      (catch WebDriverException e
        (when-not (-> e web/get-error-json
                      (contains? "undefined is not an object"))
          (throw+))))
    (log/pr :debug "After send-keys to chat box")
    (log/pr :debug "Chat complete.")))

#_(defn ^String get-react-id-for-name [^WebDriver driver ^String str-name]
  (let [^String src (.getPageSource driver)
        _ (log/pr :debug "Recording page in react id" (web/record-page! driver "React id"))
        parsed (->> src hp/parse hp/as-hickory)
        selected
          (->> parsed
               (hs/select
                 (hs/descendant
                   (hs/tag :div)
                   (hs/find-in-text (re-pattern str-name))))
               (remove+ (fn-> :attrs :data-reactid (contains? "threadTitle")))
               redv)
        ^String react-id (->> selected first :attrs :data-reactid)]
  (log/pr :debug "Found objects:" (! selected))
  react-id))

#_(defn chat-with-person! [^WebDriver driver^String str-name ^String chat-text]
  (let [^String element-id
          (get-react-id-for-name driver str-name)
        person-div
          (.findElement ^WebDriver driver
            (By/xpath
              (str "//*[@data-reactid="
                (str/squote element-id)
                "]")))
        _ (log/pr :debug (! (web/ins person-div)))
        ]
    (.click person-div)
    (log/pr :debug "After clicking")
    (chat! driver chat-text)
    (log/pr :debug "After chatting")
    (web/record-page! driver (str "Chatted with" str-name))))

; ===

#_(defn fb-messenger-init! []
  (try+
    (reset! driver (PhantomJSDriver. web/default-capabilities))
    (log/pr :debug "Created driver.")
    (fbm/login-to-messenger! @driver)
    (catch Object _ (ui/exit-driver! @driver))))

#_(def fb-messenger-test (partial ui/fb-messenger-test @driver))

 ; (defn fb-messenger-test [driver ^String person-name ^String message-text]
 ;   (try+
 ;     (fbm/chat-with-person! driver
 ;       person-name message-text)
 ;     (catch Object _ (exit-driver!))))

 ; (defn send-message! []
 ;   (fb-messenger-test    
 ;     (-> @state :person-name) 
 ;     (-> @state :message-text)))