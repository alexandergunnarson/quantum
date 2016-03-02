(ns quantum.cljstest
  (:require-quantum [:core fn logic err log pr res async core-async cbase])
  (:require        [quantum.core.string         :as str    ]
                   [quantum.system              :as sys    ]
                   [quantum.net.websocket       :as conn   ]
                   [quantum.net.http            :as http   ]
                   [quantum.ui.style.css.dom    :as css-dom]
                   [quantum.ui.style.css.core   :as css    ]
                   [devtools.core :as devtools]
                   [quantum.core.numeric        :as num    ]
                   [quantum.ui.style.fonts      :as fonts  ]
                   [quantum.core.data.list      :as list
                     :refer [dlist]]
                   [quantum.security.cryptography :as crypto]
                   [quantum.auth.core           :as auth   ]
                   [quantum.core.convert        :as conv   ]
                   [quantum.apis.amazon.cloud-drive.core :as amz]
                   [taoensso.sente :as sente]
                   [reagent.core                :as rx     ]
                   [quantum.style
                     :refer [style]                        ]
                   [garden.core                 
                     :refer [css]                          ]
                   [clojure.walk                
                     :refer [postwalk]                     ]
                   [quantum.core.type.predicates
                     :refer [map-entry?]                   ]
                   [garden.selectors            :as s      ]
                   [quantum.ui.features
                     :refer [browser]                      ]
                   [quantum.core.macros]
                   [quantum.net.client.impl]
                   [quantum.core.numeric.cljs])
  (:require-macros [quantum.system              :as sys    ]
                   [quantum.core.macros :refer [defnt]]
                   [quantum.core.numeric :as numm]
                   [reagent.ratom
                     :refer [reaction]                     ])
  )
; https://developer.amazon.com/public/apis/experience/cloud-drive/content/nodes
; cljs.core.deref.call(null, quantum.cljstest.state)
; cljs.core.deref.call(null, quantum.cljstest.timestamps)
(defalias take! core-async/take!)

(enable-console-print!)
(devtools/enable-feature! :sanity-hints :dirac)
(devtools/install!)

(defonce timestamps (atom []))

(defn stamp! [& args]
  (log/pr :debug args)
  (swap! timestamps conj [(js/Date.) args]))

(defn dropr-1-until-i [x i]
  (if (-> x count (> i))
      (pop x)
      x))

(defalias firstl first)
(defalias firstr peek )
(defalias lastl  last )
(defalias lastr  first)
(defalias conjl quantum.core.data.finger-tree/conjl)
(defalias conjr conj)

(defn toggle-full-screen!
  {:from "https://developer.mozilla.org/en-US/docs/Web/API/Fullscreen_API"
   :todo ["More elegant way to do this"]}
  []
  (if (and (not (.-fullscreenElement       js/document))
           (not (.-mozFullScreenElement    js/document))
           (not (.-webkitFullscreenElement js/document))
           (not (.-msFullscreenElement     js/document)))
      (cond
        (-> js/document .-documentElement .-requestFullscreen)
        (-> js/document .-documentElement .requestFullscreen )
  
        (-> js/document .-documentElement .-msRequestFullscreen)
        (-> js/document .-documentElement .msRequestFullscreen )
  
        (-> js/document .-documentElement .-mozRequestFullscreen)
        (-> js/document .-documentElement .mozRequestFullscreen )
  
        (-> js/document .-documentElement .-webkitRequestFullscreen)
        (-> js/document .-documentElement (.webkitRequestFullscreen js/Element.ALLOW_KEYBOARD_INPUT)))
      (cond
        (-> js/document (.-exitFullscreen))
        (-> js/document (.-exitFullscreen))
 
        (-> js/document (.-msExitFullscreen))
        (-> js/document (.-msExitFullscreen))
 
        (-> js/document (.-mozCancelFullScreen))
        (-> js/document (.-mozCancelFullScreen))
 
        (-> js/document (.-webkitExitFullscreen))
        (-> js/document (.-webkitExitFullscreen)))))

(def style-compatibilized
  (->> style
       (postwalk
         (whenf*n map?
           (fn [m]
             (->> m
                  (reduce
                    (fn [ret [k v]]
                      (let [any-compat (->> css/compatibility-chart
                                            (<- get k)
                                            (<- get css/ANY)
                                            (<- get @browser)
                                            (map first)
                                            (<- zipmap (repeat v)))
                            this-compat (->> css/compatibility-chart
                                             (<- get k)
                                             (<- get v)
                                             (<- get @browser)
                                             (apply array-map))
                            compat (merge any-compat this-compat)]
                    
                        (if (nempty? compat)
                            (do (println "K FOUND!" k)
                                (merge ret compat))
                            ret))
                    m))))))))

(def css-string
  (delay (-> (css {:vendors ["moz" "webkit"]} style-compatibilized)
             #_(clojure.string/replace "keyframes " "keyframe "))))

(defn init-ui! []
  (css-dom/add-link!
    (fonts/link :montserrat))
  (css-dom/add-link!
    (fonts/link :lato))

  (css-dom/replace-css-at! "dynamic" @css-string)
  (println "CSS-STRING:" @css-string)
  (log/pr :debug "UI inited."))

; TODO auto-invalidating cache for tree
(defonce state
  (rx/atom {:state  :weather
            ;:width  (css-dom/viewport-w)
            ;:height (css-dom/viewport-h)
            :weather-item :current
            :autoplay?    true
            :tree     {}
            :focus    nil
            :playlist (dlist)}))

(remove-watch state :playlist)
(add-watch state :playlist
  (fn [k a oldv newv]
    (when-not (= (-> oldv :playlist first)
                 (-> newv :playlist first))
      (stamp! "Focus changed to:" (-> newv :playlist first))
      (swap! state assoc :focus (-> newv :playlist first)))
    (when-not (= (-> oldv :focus)
                 (-> newv :focus))
      (go (when-let [v (-> newv :video-src)]
            (stamp! "video-src")
            (set! (.-src  v) (-> newv :focus :tempLink))
            (set! (.-type v) (-> newv :focus :contentProperties :contentType)))
          (<! (core-async/timeout 10))
          (println "(.-src  newv)" (.-src  newv))
          (when-let [v (-> newv :video)]
            (stamp! "before pause")
            (.pause v)
            (stamp! "after pause")
            (.load  v)
            (stamp! "after load")
            (when (:autoplay? newv)
              (stamp! "About to play")
              (.play v)
              (stamp! "After play")))
          ))))

(set! (.-onresize js/window)
      (fn [e]
        (swap! state assoc
          :width  (css-dom/viewport-w)
          :height (css-dom/viewport-h))))
; Best to not use sequential <! unless absolutely necessary
(defn abcde1 []
  (go (let [root     (<! (amz/root-folder))
            children (-> root :id amz/children <!)
            trashed  (take! (amz/trashed-items)
                       (fn [v] (swap! state assoc :trashed v)))
            gb       (take! (amz/used-gb)
                       (fn [v] (swap! state assoc :gb v)))
            hierarchy [(:id root)]]
     (swap! state assoc
       :root     root
       :tree     {(:id root) children}
       :hierarchy hierarchy))))

; This code is primarily done to auto-reload video when its :src is changed

(defn video-src* []
  [:source {:src  (-> @state :focus :tempLink)
            :type (-> @state :focus :contentProperties :contentType)}])


(def video-src
  (with-meta video-src*
    {:component-did-mount
      (fn [this]
        (let [node (rx/dom-node this)]
          (swap! state assoc :video-src node)))
     ; :component-did-update is useless here
     :component-will-unmount
       (fn [this]
         (swap! state update :video-src
           (fn [src] 
             (when-let [l (-> src :listener)]
              (stamp! "Component will unmount!")
               (.disconnect l))
             {})))}))

(defn video* []
  [:video {;:width  (-> @state :focus :contentProperties :video :width  (* scale))
           ;:height (-> @state :focus :contentProperties :video :height (* scale))
           ;:style {:object-fit "cover"}
           :controls true
           :autoPlay (:autoplay? @state)
           ;:on-ended / :onEnded (fn [e] (println "ENDED")) ; Doesn't work
         }
    [video-src]])

(def video
  (with-meta
    video*
    {:component-did-mount
      (fn [this]
        (let [node (rx/dom-node this)] ; "ended" doesn't fire unless you have the browser in focus, I think
          (.addEventListener node "ended"
            (fn [e]
              (stamp! "onended fired")
              (swap! state update :playlist rest)))
          (swap! state assoc :video node)))
     :component-will-unmount
       (fn [this]
         (swap! state assoc :video nil))}))

(defn get-children [state*]
  (let [parent   (-> state* :hierarchy peek)
        children (-> state* :tree (get parent))]
    children))

(defn ui-render-fn [props]
  (fn []
    (let [scale (/ 1 2)]
      [:div#div-root {:style {:width  (:width  @state)
                              :height (:height @state)}}
        [:button {:on-click (fn [e] (toggle-full-screen!))}
          "Toggle full screen"]
        [:div (str (-> @state :gb (/ 1024) (/ 1024) (/ 1024)) " GB used")]
        [:button {:on-click (fn [e] (swap! state update :hierarchy (f*n dropr-1-until-i 1)))}
          "Up"]
        [video]
      #_(into [:div]
          (->> @state :trashed :data
               (mapv (fn [file] [:div [:div (str (:id   file))]
                                      [:div (str (:name file))]]))))
        [:div {:style {:padding-bottom 20}}
          "PLAYLIST"]
        [:button {:on-click (fn [e] (swap! state update :playlist empty))}
          "Clear playlist"]
        [:button {:on-click (fn [e] (swap! state update :playlist rest))}
          "Skip"]
        (into [:div]
          (->> @state :playlist
               (mapv (fn [file] [:div (:name file)]))))
        [:div {:style {:padding-bottom 20}}
          "FILES"]
        (into [:div]
          (->> @state get-children
               (sort-by :name)
               (mapv (fn [file]
                       [:div.row
                         [:button {:on-click
                                    #(go (let [file-meta (<! (amz/meta (:id file)))] ; This is what the delay is
                                           (swap! state update :playlist conjr file-meta)))}
                           "+"]
                         [:div {:on-click (fn [e] (condp = (:kind file)
                                                     "FILE"   (go (let [focus (<! (amz/meta (:id file)))]
                                                                    (swap! state assoc :focus focus)))
                                                     "FOLDER" (go (let [children (<! (-> file :id amz/children))]
                                                                    (swap! state
                                                                      (fn [state*]
                                                                        (-> state*
                                                                            (update :tree (f*n assoc (:id file) children))
                                                                            (update :hierarchy conj (:id file)))))))))}
                           (:name file)]]))))
        ])))

(sys/create-system-vars
  ::system
  (sys/default-config
    {:log       {:levels          #{:debug :warn #_:quantum.net.client.impl/debug}}
     ;:server     {:routes         (*var router/routes)
     ;             :key-password   "password"
     ;             :trust-password "password"}
     :db         {:backend        false
                  :schemas        {:my/schema [:string :one {:unique? true}]}}
     :connection {:msg-handler    conn/event-msg-handler*
                  :uri            "/chan"
                  :host           "0.0.0.0:8081"}
     :frontend   {:init           init-ui!
                  :render         ui-render-fn}}))

(-main)

;(log/enable! :quantum.net.client.impl/debug)

; These must be set if with-credentials is true
(defn cors-headers [url]
  {;"Access-Control-Allow-Credentials" false
   ;"Access-Control-Allow-Origin" "http://freegeoip.net/*";(str/join-once "/" url "*")
   ;"Access-Control-Allow-Methods" "*"
   ;"Access-Control-Allow-Headers" "Origin, X-Requested-With, Content-Type, Accept"
   })

;(go (println "NET CONN?" (<! (quantum.net.http/get-network-connected?))))

(defonce server-resp (atom nil))

(declare resp1)
(declare resp2)
;(defonce resp2 (-> @server-resp (conv/transit-> :json)))
; Get access token from server

(defmethod conn/event-msg-handler :amazon/cloud-drive
  [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn]}]
  (println "EVENT MESSAGE" ev-msg))

(defmethod conn/event-msg-handler :chsk/state
  [{:as ev-msg :keys [?data]}]
  (if (:first-open? ?data)
      (log/pr :debug "First open.")
      (log/pr :debug "Channel socket state change:" ?data)))

(defmethod conn/event-msg-handler :default ; Fallback
  [{:as ev-msg :keys [event]}]
  (log/pr :debug "Unhandled event:" event))

(defmethod conn/event-msg-handler :chsk/recv
  [{:as ev-msg :keys [?data]}]
  (log/pr :debug "Push event from server:" ?data))

(defmethod conn/event-msg-handler :chsk/handshake
  [{:as ev-msg :keys [?data]}]
  (let [[?uid ?csrf-token ?handshake-data] ?data]
    (log/pr :debug "Handshake:" ?data)
    (do (log/pr :debug "Channel socket established and handshook with server.")
        (conn/put! [:amazon/cloud-drive {:no-data nil}]
          (fn [resp]
            (log/pr :debug "Reply from server with access token:" resp)
            (reset! server-resp resp)
            (if (sente/cb-success? resp)
                (let [resp-conv (-> resp (conv/transit-> :json))]
                  (auth/assoc! :amazon
                    (-> resp-conv
                        :encrypted
                        (crypto/aes
                          :decrypt
                          "256Yesthismaybetemp"
                          (merge resp-conv {:base64->? true :->str? true}))
                        (conv/transit-> :json)))
                  (abcde1)
                  (swap! state identity))
                (log/pr :warn "Error:" resp)))
          3000)))) ; 200 is too short

#_(binding [auth/*mem?* true]
   (quantum.apis.amazon.cloud-drive.core/request! :account/usage :meta))
; (go (println "AMZ RESP IS" (<! (quantum.apis.amazon.cloud-drive.core/request! :account/usage :meta))))
; (go (println "USED" (<! (quantum.apis.amazon.cloud-drive.core/used-gb))))
; (go (println "ROOT" (<! (quantum.apis.amazon.cloud-drive.core/root-folder))))
; ( quantum.apis.amazon.cloud-drive.auth/refresh-token! nil)
; (auth/assoc-in! :amazon
;             [:cloud-drive #_user :access-tokens :current])

(println "START ==================")
(defnt randfn
  ([^number?  x] (println "A number!"))
  ([^string?  x] (println "A string!"))
  ([^vector?  x] (println "A vector!"))
  ([^boolean? x] (println "A boolean!")))
(randfn "asd" )
(randfn false)
#_(randfn false)
(println (numm/ceil 3.4))
(println "END ==================")