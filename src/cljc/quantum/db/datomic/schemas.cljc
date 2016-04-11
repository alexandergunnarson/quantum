(ns quantum.db.datomic.schemas
  (:refer-clojure :exclude [type])
  (:require-quantum [:core logic fn err macros str coll conv #?(:clj uconv) pconv debug])
  (:require [quantum.db.datomic.entities
              #?@(:clj [:refer [defattribute defentity declare-entity schemas attributes]])]
            [quantum.net.http               :as http    ]
            [quantum.core.data.complex.xml  :as xml     ]
            [quantum.db.datomic             :as db      ]
            [quantum.core.convert.primitive :as pconv]
    #?(:clj [clojure.java.shell             :as shell   ]))
  #?(:cljs
  (:require-macros
      [quantum.db.datomic.entities :refer [defattribute defentity declare-entity]])))

(def percent? (fn-and (f*n >= 0) (f*n <= 1)))

; =========== GENERAL =========== ;

(defrecord Schema [])
(defn ->schema [k]
  #_(:db/q '[:find [?ident ...]
           :where [_ :db/ident ?ident]])
  k)
(defrecord Any [])
(def ->any identity)

(defattribute :type
  [:ref :one {:ref-to :db/schema}])

(defentity :ratio:long
  {:doc "A ratio specifically using longs instead of bigints."}
  {:ratio:long:numerator   [:long :one]
   :ratio:long:denominator [:long :one]})

(defn num->ratio:long [n]
  (let [r (rationalize n)]
    (if (instance? clojure.lang.Ratio r)
        (let [n      (numerator   r)
              n-long (long n)
              _ (assert (= n-long n) #{n n-long})
              d      (denominator r)
              d-long (long d)
              _ (assert (= d-long d) #{d d-long})]
          (->ratio:long
             {:ratio:long:numerator   n-long
              :ratio:long:denominator d-long}))
        (let [n r
              n-long (long n)
              _ (assert (= n-long n) #{n n-long})]
          (->ratio:long
            {:ratio:long:numerator   n-long
             :ratio:long:denominator 1})))))

; =========== LOCATION =========== ;

(defentity :location:country
  {:location [:keyword :one]}) ; TODO

; ; =========== LINGUISTICS =========== ;

(defentity :linguistics:language
  {:location [:keyword :one]}) ; TODO

; ; =========== UNITS =========== ;

(def units (atom #{}))

(defattribute :unit
  [:keyword :one #_{:unique :value}])

(defattribute :unit:kb-per-s         [:double :one])
(defattribute :unit:pixels           [:long   :one])
(defattribute :unit:beats-per-minute [:double :one])

(defattribute :unit:v
  [:ref :one {:ref-to :ratio:long :component? true}])

; ; =========== TIME =========== ;

(defattribute :time:instant [:long :one])

(defentity :time:range
  {:time:from [:ref :one {:ref-to :time:instant}]
   :time:to   [:ref :one {:ref-to :time:instant}]})

(defattribute :time:year
  [:ref :one {:ref-to :time:range :unique :value
              :doc "Not the sum a year's time, but a particular year."}])

(defattribute :time:duration
  [:long :one {:doc "In nanoseconds"}])

; ; =========== META =========== ;

(def date [:ref :one {:ref-to :time:instant}])

(defattribute :date:created
  [:ref :one {:ref-to :time:instant}])

(defattribute :date:last-accessed
  [:ref :one {:ref-to :time:instant
              :doc (quantum.core.string/sp "For media tracks, date last played."
                        "For other things, date last accessed/opened/viewed.")}])

(defattribute :date:last-modified
  [:ref :one {:ref-to :time:instant}])

; =========== PERSON NAME =========== ;

; Many of these are keywords because the same names happen over and over again   

(defentity :agent:person:name:last
  {:component? true :doc "E:g. \"Gutierrez de Santos, III\""}
  {:agent:person:name:last:primary
     [:ref     :one {:ref-to :db/schema :doc "refers to a schema, e.g. \":agent:person:name:surname\" if it's a simple one like \"Smith\""}]
   :agent:person:name:last:surname
     [:keyword :one {:doc "e.g. \"Smith\", or a complex non-paternal-maternal name"}]
   :agent:person:name:last:maternal
     [:keyword :one {:doc "e.g. \"Gutierrez\" in \"Gutierrez de Santos\""}]
   :agent:person:name:last:paternal
     [:keyword :one {:doc "e.g. \"de Santos\" in \"Gutierrez de Santos\""}]
   :agent/person:name:suffix
     [:keyword :one {:doc "e.g. \"Jr., Sr., III\""}]})

(defentity :agent:person:name
  {:component? true
   :doc "TODO Support hyphenated and maiden names"}
  {:agent:person:name:legal-in 
     [:ref     :many {:ref-to :location:country}]
   :agent:person:name:called-by
     [:ref     :one  {:ref-to :db/schema :doc "refers to a schema, e.g. \":agent:person:name:middle\" if they go by middle name"}]
   :agent:person:name:prefix
     [:keyword :many {:doc "e.g. His Holiness, Dr., Sir. TODO: need to prefix"}]
   :agent:person:name:first    
     [:keyword :one  {:doc "e.g. \"José\" in \"José Maria Gutierrez de Santos\""}]
   :agent:person:name:middle  
     [:keyword :one  {:doc "e:g. \"Maria\" in \"José Maria Gutierrez de Santos\""}] 
   :agent:person:name:last nil})

(defattribute :agent:person:name:alias 
  [:keyword :one {:component? true :doc "AKA nickname"}])

; =========== REGISTRATION =========== ;

(defrecord Agent_Organization [])
(defrecord Agent_Person [])

(defentity :agent:registration
  {:component? true}
  {:agent:registration:detail
     [:ref :one  {:ref-to :db/any :doc "Reference to a set of Twitter, Facebook, etc. facts"}]
   :agent:registration:provider
     [:ref :one  {:ref-to :agent:organization}]
     })

; =========== NETWORK =========== ;

(defattribute :network:domain:ip-address
  [:string :one {:unique :value :doc "Multiple domain names can point to the same IP address"}])

(defentity :network:domain
  {:unique :value}
  {:network:domain:ip-address nil
   :network:domain:prefix
     [:keyword :one {:doc "Keyword because it's short and universal"}]})

(declare-entity :agent:person)

(defentity :agent:email
  {:unique :value}
  {:agent:email:username 
     [:keyword :one {:doc "E.g. alexandergunnarson"}]
   :network:domain nil
   :agent:email:validated-by
     [:ref :one {:ref-to :agent:organization :doc "E.g. company, email validation service, etc."}]
   :agent:email:source
     [:ref :one {:ref-to #{:agent:organization :agent:person} :doc "Who/what provided the email information"}]})

; =========== AGENT =========== ;

(defentity :agent:person
  {:agent:person:name:many
     [:ref :many {:ref-to :agent:person:name}]
   :agent:person:name:alias:many
     [:ref :many {:ref-to :agent:person:name:alias}]
   :agent:emails
     [:ref :many {:ref-to :agent:email}]
   :agent:registration:many
     [:ref :many {:ref-to :agent:registration}]})
  
(defentity :agent:organization:name
  {:component? true}
  {:agent:organization:name:legal-in
    [:ref :many {:ref-to :location:country}]})

(defentity :agent:organization
  {:agent:organization:names
     [:ref :many {:ref-to :agent:organization:name}]})

(def schema:agent [:ref :one {:ref-to #{:agent:person :agent:organization}}])

(defattribute :art:creator
  [:ref :one {:ref-to #{:agent:person :agent:organization}
              :doc "In the instance of music, composer. For images, artist."}])

(defattribute :data:creator
  [:ref :one {:ref-to #{:agent:person :agent:organization}
              :doc "E.g. software, process, person, organization.
                TODO extend to non-agent causers"}])

(defattribute :data:source
  [:ref :one {:ref-to :db/any :doc "Where the data was gotten from."
              :component? true}])

(defentity :data:certainty
  {:component? true}
  {:data:source          nil ; From what source do you get your certainty? ; TODO make into a logical proposition, not just a source entity
   :data:certainty:value [:double :one {:doc "The certainty that the data is the case / true."
                                        :validator amazon-player.db/percent?}]})

; =========== MEDIA =========== ;

(defattribute :data:title
  [:string :one])

(defattribute :data:description
  [:string :one])
#_(:clj (ns-unmap 'amazon-player.db 'Opinion_Comment))
;#?(:clj (defrecord Opinion_Comment []))

(defentity :opinion:comment
  {:opinion:comment:text
     [:string :one {:doc "Can be in any format — markdown, HTML, plain, etc."}]
   :opinion:comment:created-by
     [:ref :one {:ref-to #{:agent:organization :agent:person}}]
   :in-response-to
     [:ref :one {:ref-to :opinion:comment}]})

(defattribute :opinion:comment:many
  [:ref :many {:ref-to :opinion:comment
               :doc "The comment chain is dynamically created from the list of comments"}])

(defentity :opinion:rating
  {:doc "The rating that a particular person gave"}
  {:opinion:rating:value
     [:double :one {:doc "Can be any range"}]
   :opinion:rating:explanation
     [:string :one]})

(defentity :opinion:entity+rating
  {:component? true}
  {:opinion:opiner [:ref :one {:ref-to #{:agent:person :agent:organization}}]
   :opinion:rating nil})

(defentity :opinion:rating:many
  {:opinion:entity+rating:many
     [:ref :many {:ref-to :opinion:entity+rating}]})

(defattribute :agent
  [:ref :one {:ref-to #{:agent:person :agent:organization}}])

(defattribute :media:artist ; Album Artist ?
  [:ref :one {:ref-to :agent}])

(defentity :media:agent+plays
  {:component? true}
  {:agent nil
   :media:plays
     [:ref :many {:ref-to :time:instant :doc "Dates played"}]})

(defentity :media:genre
  {:doc "Like rock, classical, new age, rap"}
  {:data:title       nil
   :data:description nil})

(defentity :media:sub-genre
  {:doc "Like baroque, indie rock (?). TODO should be "}
  {:data:title       nil
   :data:description nil})

(defentity :media:category
  {:doc "TODO Ambiguous"}
  {:data:title       nil
   :data:description nil})

(defentity :media:kind
  {:doc "TODO Ambiguous"}
  {:data:title       nil
   :data:description nil})



(defattribute :data:audio:sample-rate
  [:double :one {:doc "E.g. 44100 kHz"}])

; DEFUNIT HERE
(defrecord Data_Audio_Bit_Rate [type unit unit:v])
(defn ->data:audio:bit-rate [v]
  (Data_Audio_Bit_Rate. :data:audio:bit-rate :unit:kb-per-s
    (num->ratio:long v)))
(do (swap! schemas assoc :data:audio:bit-rate [:ref :one {:component? true}]) nil)

(defrecord Data_Audio_Max_Bit_Rate [type unit unit:v])
(defn ->data:audio:max-bit-rate [v]
  (Data_Audio_Max_Bit_Rate. :data:audio:max-bit-rate :unit:kb-per-s
    (num->ratio:long v)))
(do (swap! schemas assoc :data:audio:max-bit-rate [:ref :one {:component? true}]) nil)

(defrecord Data_Audio_Nominal_Bit_Rate [type unit unit:v])
(defn ->data:audio:nominal-bit-rate [v]
  (Data_Audio_Nominal_Bit_Rate. :data:audio:nominal-bit-rate :unit:kb-per-s
    (num->ratio:long v)))
(do (swap! schemas assoc :data:audio:nominal-bit-rate [:ref :one {:component? true}]) nil)

; DEFUNIT HERE
(defrecord Data_Video_Bit_Rate [type unit unit:v])
(defn ->data:video:bit-rate [v]
  (Data_Video_Bit_Rate. :data:video:bit-rate :unit:kb-per-s
    (num->ratio:long v)))
(do (swap! schemas assoc :data:video:bit-rate [:ref :one {:component? true}]) nil)

; DEFUNIT HERE
(defrecord Data_Image_Height [type unit unit:v])
(defn ->data:image:height [v]
  (Data_Image_Height. :data:image:height :unit:pixels
    (num->ratio:long v)))
(do (swap! schemas assoc :data:image:height [:ref :one {:component? true}]) nil)

; DEFUNIT HERE
(defrecord Data_Image_Width [type unit unit:v])
(defn ->data:image:width [v]
  (Data_Image_Width. :data:image:width :unit:pixels
    (num->ratio:long v)))
(do (swap! schemas assoc :data:image:width [:ref :one {:component? true}]) nil)

#_(defentity :media:track-num+track
  {:media:track-num [:long :one {:unique :value :doc "AKA episode-num"}] ; unique... FIX THIS 
   :media:track nil})

#_(defentity :media/grouping
  {:doc "E:g. album / series/show"}
  {:data:title nil
   :media:track:many
     [:ref :many {:ref-to :media:track}]
   :media:track-num+track:many
     [:ref :many {:ref-to :media:track-num+track}]
   :opinion:rating:many nil})

(defattribute :data:bytes-size
  [:long :one])

(defentity :media:track
  {:doc "video or audio"}
  {:data:title          nil
   :time:duration       nil
   :date:created        nil ; sample-date
   :date:last-accessed  nil
   :date:last-modified  nil
   :cloud:amazon:id     nil
   :date:purchased      [:ref :one {:ref-to :time:instant}]
   ; Date Added ; get from first transaction
   ; Date Modified ; get from transactions
   :opinion:rating:many nil
   :data:audio:bit-rate nil
   :data:video:bit-rate nil
   :data:audio:sample-rate   nil
   :data:media:genre         nil ; ambiguous
   :data:media:sub-genre     nil ; from grouping; ambiguous ; mood / sub-category
   :data:media:kind          nil ; ambiguous
   :data:media:category      nil ; ambiguous
   :data:media:description   [:string :one]
   :data:media:agent+plays:many
     [:ref :many {:ref-to :media:agent+plays}]
   :data:media:skipped-dates
     [:ref :many {:ref-to :time:instant :doc "Instants when the track was skipped"}]
   :art:creator            nil
   :data:creator           nil
   ; producer?
   ; collaborators?
   :user-owns?             [:boolean :one]
   :data:bytes-size        nil
   :data:image:height           nil
   :data:image:width            nil
   :data:audio:beats-per-minute [:ref :one {:ref-to :unit:beats-per-minute}]
   ; :audio:equalizer ; Not sure what this would consist of
   :data:media:release-date     [:ref :one {:ref-to :time:instant}] ; for album?
   :opinion:comment:many   nil})

; If a transaction specifies a unique identity for a temporary id,
; and that unique identity already exists in the database, then that temporary id
; will resolve to the existing entity in the system. 
(defentity :data:format
  {:data:mime-type                  [:keyword :one  {:unique :identity :doc "Refer to http://www.sitepoint.com/web-foundations/mime-types-complete-list/"}]
   :data:appropriate-extension:many [:keyword :many {:doc "Needs to correspond with its mime-type"}]})

(defattribute :cloud:amazon:id
  [:string :one {:unique :value}])

(defentity :file
  {:doc "A file's metadata. TODO figure out all of valid file metadata"}
  {:file:path                           [:uri     :one]
   :cloud:amazon:id                     nil
   :date:last-modified                  nil
   :data:format                         nil
   :data:title                          nil
   :data:video:frame-rate               [:double  :one]
   :data:video:format-settings:gop      [:string  :one]
   :data:video:frame-rate-mode          [:string  :one]
   :data:video:scan-type                [:string  :one]
   :data:video:aspect-ratio             [:string  :one]
   :data:video:bit-depth                [:double  :one]
   :data:video:chroma-subsampling       [:string  :one]
   :data:video:bits-per-pixel*frame     [:double  :one]
   :data:video:format                   [:keyword :one]
   :data:video:format-settings:reframes [:string  :one]
   :data:video:format-settings:cabac    [:string  :one]
   :data:video:format-profile           [:string  :one]
   :data:video:format:info              [:string  :one]
   :data:video:stream-size              [:string  :one]
   :data:video:bit-rate                 nil
   :data:video:id:mediainfo             [:string  :one]
   :data:video:color-space              [:string  :one]
   :data:video:codec-id                 [:keyword :one]
   :data:video:codec-id:info            [:string  :one]
   :data:image:height                   nil
   :data:image:width                    nil
   :data:bytes-size                     nil
   :data:audio:codec-id                 [:keyword :one]
   :data:audio:format-profile           [:string  :one]
   :data:audio:format:info              [:string  :one]
   :data:audio:id:mediainfo             [:string  :one]
   :data:audio:sample-rate              nil
   :data:audio:stream-size              [:string  :one]
   :data:audio:bit-rate                 nil
   :data:audio:channels                 [:long    :one]
   :data:audio:compression-mode         [:string  :one]
   :data:audio:compressor               [:string  :one]
   :data:audio:writing-library          [:string  :one]
   :data:audio:mode                     [:string  :one]
   :data:audio:format-version           [:long    :one]
   :data:audio:format-version:tika      [:string  :one]
   :data:audio:mode-extension           [:string  :one]
   :data:fingerprint:fpcalc             [:string  :one]
   :data:codec-id                       [:keyword :one]
   :data:media:compatible-brands        [:string  :one]
   :data:media:major-brand              [:string  :one]
   :media:part:position                 [:long    :one]
   :media:part:total                    [:long    :one]
   :media:track:position                [:long    :one]
   :media:track:position:itunes         [:long    :one]
   :media:track:total                   [:long    :one]
   :media:encoding-params               [:string  :one]
   :data:tagged-date:mediainfo          [:string  :one]
   :data:minor-version:mediainfo        [:string  :one]
   :media:album-art:type:mediainfo      [:string  :one]
   :media:has-album-art?                [:boolean :one]
   :media:album-art:format              [:ref :one {:ref-to :data:format}]
   :data:audio:max-bit-rate             nil
   :data:audio:nominal-bit-rate         nil
   :data:media:bit-rate-mode            [:string  :one]
   :data:media:codec-id:mediainfo       [:string  :one]
   :data:media:format-profile           [:string  :one]
   :data:media:artist:mediainfo         [:string  :one] ; = performer
   :data:media:composer:mediainfo       [:string  :one]
   :data:media:album:mediainfo          [:string  :one]
   :data:media:album:artist:mediainfo   [:string  :one]
   :data:date-encoded:mediainfo         [:string  :one]
   :media:date-recorded:mediainfo       [:string  :one]
   :media:itunes-cddb-1                 [:string  :one]
   :data:media:source-data:mediainfo    [:string  :one]
   :media:genre                         nil
   :data:audio:encoded-date:mediainfo   [:string  :one]
   :data:audio:date-tagged:mediainfo    [:string  :one]
   :data:writing-library                [:string  :one]
   :data:writing-application            [:string  :one]
   :data:media:compilation:tika         [:long    :one]
   :time:duration                       nil})


