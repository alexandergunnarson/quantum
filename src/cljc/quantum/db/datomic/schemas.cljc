(ns quantum.db.datomic.schemas
  (:refer-clojure :exclude [type])
            (:require
              #?(:clj [clojure.java.shell             :as shell  ])
                      [quantum.db.datomic.entities
                        :refer [#?@(:clj [defattribute defentity 
                                          declare-entity schemas
                                          attributes])]          ]
                      [quantum.core.fn                :as fn
                        :refer [#?@(:clj [f*n])]                 ]
                      [quantum.core.logic             :as logic
                        :refer [#?@(:clj [fn-and])]              ]
                      [quantum.core.string            :as str    ]
                      [quantum.net.http               :as http   ]
                      [quantum.core.data.complex.xml  :as xml    ]
                      [quantum.db.datomic             :as db     ]
                      [quantum.core.convert.primitive :as pconv  ]
                      [quantum.core.numeric           :as num   
                        :refer [percent?]                        ])
  #?(:cljs  (:require-macros
                      [quantum.core.fn                :as fn
                        :refer [f*n]                             ]
                      [quantum.core.logic             :as logic
                        :refer [fn-and]                          ]
                      [quantum.db.datomic.entities
                        :refer [defattribute defentity
                                declare-entity]                  ])))

; =========== GENERAL =========== ;

(defrecord Schema [])
(defn ->schema [k]
  #_(:db/q '[:find [?ident ...]
           :where [_ :db/ident ?ident]])
  k)
(defrecord Any [])
(def ->any identity)

(defattribute :type
  [:one :ref {:ref-to :db/schema}])

(defentity :ratio:long
  {:doc "A ratio specifically using longs instead of bigints."}
  {:ratio:long:numerator   [:one :long]
   :ratio:long:denominator [:one :long]})

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
  {:location [:one :keyword]}) ; TODO

; ; =========== LINGUISTICS =========== ;

(defentity :linguistics:language
  {:location [:one :keyword]}) ; TODO

; ; =========== UNITS =========== ;

(def units (atom #{}))

(defattribute :unit
  [:one :keyword #_{:unique :value}])

(defattribute :unit:kb-per-s         [:one :double])
(defattribute :unit:pixels           [:one :long  ])
(defattribute :unit:beats-per-minute [:one :double])

(defattribute :unit:v
  [:one :ref {:ref-to :ratio:long :component? true}])

; ; =========== TIME =========== ;

(defattribute :time:instant [:one :long])

(defentity :time:range
  {:time:from [:one :ref {:ref-to :time:instant}]
   :time:to   [:one :ref {:ref-to :time:instant}]})

(defattribute :time:year
  [:one :ref {:ref-to :time:range :unique :value
              :doc "Not the sum a year's time, but a particular year."}])

(defattribute :time:duration
  [:one :long {:doc "In nanoseconds"}])

; ; =========== META =========== ;

(def date [:one :ref {:ref-to :time:instant}])

(defattribute :date:created
  [:one :ref {:ref-to :time:instant}])

(defattribute :date:last-accessed
  [:one :ref {:ref-to :time:instant
              :doc (quantum.core.string/sp "For media tracks, date last played."
                        "For other things, date last accessed/opened/viewed.")}])

(defattribute :date:last-modified
  [:one :ref {:ref-to :time:instant}])

; =========== PERSON NAME =========== ;

; Many of these are keywords because the same names happen over and over again   

(defentity :agent:person:name:last
  {:component? true :doc "E:g. \"Gutierrez de Santos, III\""}
  {:agent:person:name:last:primary
     [:one :ref     {:ref-to :db/schema :doc "refers to a schema, e.g. \":agent:person:name:surname\" if it's a simple one like \"Smith\""}]
   :agent:person:name:last:surname
     [:one :keyword {:doc "e.g. \"Smith\", or a complex non-paternal-maternal name"}]
   :agent:person:name:last:maternal
     [:one :keyword {:doc "e.g. \"Gutierrez\" in \"Gutierrez de Santos\""}]
   :agent:person:name:last:paternal
     [:one :keyword {:doc "e.g. \"de Santos\" in \"Gutierrez de Santos\""}]
   :agent/person:name:suffix
     [:one :keyword {:doc "e.g. \"Jr., Sr., III\""}]})

(defentity :agent:person:name
  {:component? true
   :doc "TODO Support hyphenated and maiden names"}
  {:agent:person:name:legal-in 
     [:many :ref     {:ref-to :location:country}]
   :agent:person:name:called-by
     [:one  :ref     {:ref-to :db/schema :doc "refers to a schema, e.g. \":agent:person:name:middle\" if they go by middle name"}]
   :agent:person:name:prefix
     [:many :keyword {:doc "e.g. His Holiness, Dr., Sir. TODO: need to prefix"}]
   :agent:person:name:first    
     [:one  :keyword {:doc "e.g. \"José\" in \"José Maria Gutierrez de Santos\""}]
   :agent:person:name:middle  
     [:one  :keyword {:doc "e:g. \"Maria\" in \"José Maria Gutierrez de Santos\""}] 
   :agent:person:name:last nil})

(defattribute :agent:person:name:alias 
  [:one :keyword {:component? true :doc "AKA nickname"}])

; =========== REGISTRATION =========== ;

(defrecord Agent_Organization [])
(defrecord Agent_Person [])

(declare ->agent:organization)

(defentity :agent:registration
  {:component? true}
  {:agent:registration:detail
     [:one :ref  {:ref-to :db/any :doc "Reference to a set of Twitter, Facebook, etc. facts"}]
   :agent:registration:provider
     [:one :ref  {:ref-to :agent:organization}]
     })

; =========== NETWORK =========== ;

(defattribute :network:domain:ip-address
  [:one :string {:unique :value :doc "Multiple domain names can point to the same IP address"}])

(defentity :network:domain
  {:unique :value}
  {:network:domain:ip-address nil
   :network:domain:prefix
     [:one :keyword {:doc "Keyword because it's short and universal"}]})

(declare-entity :agent:person)

(defentity :agent:email
  {:unique :value}
  {:agent:email:username 
     [:one :keyword {:doc "E.g. alexandergunnarson"}]
   :network:domain nil
   :agent:email:validated-by
     [:one :ref {:ref-to :agent:organization :doc "E.g. company, email validation service, etc."}]
   :agent:email:source
     [:one :ref {:ref-to #{:agent:organization :agent:person} :doc "Who/what provided the email information"}]})

; =========== AGENT =========== ;

(defentity :agent:person
  {:agent:person:name:many
     [:many :ref {:ref-to :agent:person:name}]
   :agent:person:name:alias:many
     [:many :ref {:ref-to :agent:person:name:alias}]
   :agent:emails
     [:many :ref {:ref-to :agent:email}]
   :agent:registration:many
     [:many :ref {:ref-to :agent:registration}]})
  
(defentity :agent:organization:name
  {:component? true}
  {:agent:organization:name:legal-in
    [:many :ref {:ref-to :location:country}]})

(defentity :agent:organization
  {:agent:organization:names
     [:many :ref {:ref-to :agent:organization:name}]})

(def schema:agent [:one :ref {:ref-to #{:agent:person :agent:organization}}])

(defattribute :art:creator
  [:one :ref {:ref-to #{:agent:person :agent:organization}
              :doc "In the instance of music, composer. For images, artist."}])

(defattribute :data:creator
  [:one :ref {:ref-to #{:agent:person :agent:organization}
              :doc "E.g. software, process, person, organization.
                TODO extend to non-agent causers"}])

(defattribute :data:source
  [:one :ref {:ref-to :db/any :doc "Where the data was gotten from."
              :component? true}])

(defentity :data:certainty
  {:component? true}
  {:data:source          nil ; From what source do you get your certainty? ; TODO make into a logical proposition, not just a source entity
   :data:certainty:value [:one :double {:doc "The certainty that the data is the case / true."
                                        :validator quantum.core.numeric/percent?}]})

; =========== MEDIA =========== ;

(defattribute :data:title
  [:one :string])

(defattribute :data:description
  [:one :string])
#_(:clj (ns-unmap 'amazon-player.db 'Opinion_Comment))
;#?(:clj (defrecord Opinion_Comment []))

(defentity :opinion:comment
  {:opinion:comment:text
     [:one :string {:doc "Can be in any format — markdown, HTML, plain, etc."}]
   :opinion:comment:created-by
     [:one :ref {:ref-to #{:agent:organization :agent:person}}]
   :in-response-to
     [:one :ref {:ref-to :opinion:comment}]})

(defattribute :opinion:comment:many
  [:many :ref {:ref-to :opinion:comment
               :doc "The comment chain is dynamically created from the list of comments"}])

(defentity :opinion:rating
  {:doc "The rating that a particular person gave"}
  {:opinion:rating:value
     [:one :double {:doc "Can be any range"}]
   :opinion:rating:explanation
     [:one :string]})

(defentity :opinion:entity+rating
  {:component? true}
  {:opinion:opiner [:one :ref {:ref-to #{:agent:person :agent:organization}}]
   :opinion:rating nil})

(defentity :opinion:rating:many
  {:opinion:entity+rating:many
     [:many :ref {:ref-to :opinion:entity+rating}]})

(defattribute :agent
  [:one :ref {:ref-to #{:agent:person :agent:organization}}])

(defattribute :media:artist ; Album Artist ?
  [:one :ref {:ref-to :agent}])

(defentity :media:agent+plays
  {:component? true}
  {:agent nil
   :media:plays
     [:many :ref {:ref-to :time:instant :doc "Dates played"}]})

(defentity :data:media:genre
  {:doc "Like rock, classical, new age, rap"}
  {:data:title       nil
   :data:description nil})

(defentity :data:media:sub-genre
  {:doc "Like baroque, indie rock (?). TODO should be "}
  {:data:title       nil
   :data:description nil})

(defentity :data:media:category
  {:doc "TODO Ambiguous"}
  {:data:title       nil
   :data:description nil})

(defentity :data:media:kind
  {:doc "TODO Ambiguous"}
  {:data:title       nil
   :data:description nil})



(defattribute :data:audio:sample-rate
  [:one :double {:doc "E.g. 44100 kHz"}])

; DEFUNIT HERE
(defrecord Data_Audio_Bit_Rate [type unit unit:v])
(defn ->data:audio:bit-rate [v]
  (Data_Audio_Bit_Rate. :data:audio:bit-rate :unit:kb-per-s
    (num->ratio:long v)))
(do (swap! schemas assoc :data:audio:bit-rate [:one :ref {:component? true}]) nil)

(defrecord Data_Audio_Max_Bit_Rate [type unit unit:v])
(defn ->data:audio:max-bit-rate [v]
  (Data_Audio_Max_Bit_Rate. :data:audio:max-bit-rate :unit:kb-per-s
    (num->ratio:long v)))
(do (swap! schemas assoc :data:audio:max-bit-rate [:one :ref {:component? true}]) nil)

(defrecord Data_Audio_Nominal_Bit_Rate [type unit unit:v])
(defn ->data:audio:nominal-bit-rate [v]
  (Data_Audio_Nominal_Bit_Rate. :data:audio:nominal-bit-rate :unit:kb-per-s
    (num->ratio:long v)))
(do (swap! schemas assoc :data:audio:nominal-bit-rate [:one :ref {:component? true}]) nil)

; DEFUNIT HERE
(defrecord Data_Video_Bit_Rate [type unit unit:v])
(defn ->data:video:bit-rate [v]
  (Data_Video_Bit_Rate. :data:video:bit-rate :unit:kb-per-s
    (num->ratio:long v)))
(do (swap! schemas assoc :data:video:bit-rate [:one :ref {:component? true}]) nil)

; DEFUNIT HERE
(defrecord Data_Image_Height [type unit unit:v])
(defn ->data:image:height [v]
  (Data_Image_Height. :data:image:height :unit:pixels
    (num->ratio:long v)))
(do (swap! schemas assoc :data:image:height [:one :ref {:component? true}]) nil)

; DEFUNIT HERE
(defrecord Data_Image_Width [type unit unit:v])
(defn ->data:image:width [v]
  (Data_Image_Width. :data:image:width :unit:pixels
    (num->ratio:long v)))
(do (swap! schemas assoc :data:image:width [:one :ref {:component? true}]) nil)

#_(defentity :media:track-num+track
  {:media:track-num [:one :long {:unique :value :doc "AKA episode-num"}] ; unique... FIX THIS 
   :media:track nil})

#_(defentity :media/grouping
  {:doc "E:g. album / series/show"}
  {:data:title nil
   :media:track:many
     [:many :ref {:ref-to :media:track}]
   :media:track-num+track:many
     [:many :ref {:ref-to :media:track-num+track}]
   :opinion:rating:many nil})

(defattribute :data:bytes-size
  [:one :long])

(defattribute :cloud:amazon:id
  [:one :string {:unique :value}])

(defentity :media:track
  {:doc "video or audio"}
  {:data:title          nil
   :time:duration       nil
   :date:created        nil ; sample-date
   :date:last-accessed  nil
   :date:last-modified  nil
   :cloud:amazon:id     nil
   :date:purchased      [:one :ref {:ref-to :time:instant}]
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
   :data:media:description   [:one :string]
   :data:media:agent+plays:many
     [:many :ref {:ref-to :media:agent+plays}]
   :data:media:skipped-dates
     [:many :ref {:ref-to :time:instant :doc "Instants when the track was skipped"}]
   :art:creator            nil
   :data:creator           nil
   ; producer?
   ; collaborators?
   :user-owns?             [:one :boolean]
   :data:bytes-size        nil
   :data:image:height           nil
   :data:image:width            nil
   :data:audio:beats-per-minute [:one :ref {:ref-to :unit:beats-per-minute}]
   ; :audio:equalizer ; Not sure what this would consist of
   :data:media:release-date     [:one :ref {:ref-to :time:instant}] ; for album?
   :opinion:comment:many   nil})

; If a transaction specifies a unique identity for a temporary id,
; and that unique identity already exists in the database, then that temporary id
; will resolve to the existing entity in the system. 
(defentity :data:format
  {:data:mime-type                  [:one  :keyword {:unique :identity :doc "Refer to http://www.sitepoint.com/web-foundations/mime-types-complete-list/"}]
   :data:appropriate-extension:many [:many :keyword {:doc "Needs to correspond with its mime-type"}]})

(defentity :file
  {:doc "A file's metadata. TODO figure out all of valid file metadata"}
  {:file:path                           [:one  :uri    ]
   :cloud:amazon:id                     nil
   :date:last-modified                  nil
   :data:format                         nil
   :data:title                          nil
   :data:video:frame-rate               [:one  :double ]
   :data:video:format-settings:gop      [:one  :string ]
   :data:video:frame-rate-mode          [:one  :string ]
   :data:video:scan-type                [:one  :string ]
   :data:video:aspect-ratio             [:one  :string ]
   :data:video:bit-depth                [:one  :double ]
   :data:video:chroma-subsampling       [:one  :string ]
   :data:video:bits-per-pixel*frame     [:one  :double ]
   :data:video:format                   [:one  :keyword]
   :data:video:format-settings:reframes [:one  :string ]
   :data:video:format-settings:cabac    [:one  :string ]
   :data:video:format-profile           [:one  :string ]
   :data:video:format:info              [:one  :string ]
   :data:video:stream-size              [:one  :string ]
   :data:video:bit-rate                 nil
   :data:video:id:mediainfo             [:one  :string ]
   :data:video:color-space              [:one  :string ]
   :data:video:codec-id                 [:one  :keyword]
   :data:video:codec-id:info            [:one  :string ]
   :data:image:height                   nil
   :data:image:width                    nil
   :data:bytes-size                     nil
   :data:audio:codec-id                 [:one  :keyword]
   :data:audio:format-profile           [:one  :string ]
   :data:audio:format:info              [:one  :string ]
   :data:audio:id:mediainfo             [:one  :string ]
   :data:audio:sample-rate              nil
   :data:audio:stream-size              [:one  :string ]
   :data:audio:bit-rate                 nil
   :data:audio:channels                 [:one  :long   ]
   :data:audio:compression-mode         [:one  :string ]
   :data:audio:compressor               [:one  :string ]
   :data:audio:writing-library          [:one  :string ]
   :data:audio:mode                     [:one  :string ]
   :data:audio:format-version           [:one  :long   ]
   :data:audio:format-version:tika      [:one  :string ]
   :data:audio:mode-extension           [:one  :string ]
   :data:fingerprint:fpcalc             [:one  :string ]
   :data:codec-id                       [:one  :keyword]
   :data:media:compatible-brands        [:one  :string ]
   :data:media:major-brand              [:one  :string ]
   :media:part:position                 [:one  :long   ]
   :media:part:total                    [:one  :long   ]
   :media:track:position                [:one  :long   ]
   :media:track:position:itunes         [:one  :long   ]
   :media:track:total                   [:one  :long   ]
   :media:encoding-params               [:one  :string ]
   :data:tagged-date:mediainfo          [:one  :string ]
   :data:minor-version:mediainfo        [:one  :string ]
   :media:album-art:type:mediainfo      [:one  :string ]
   :media:has-album-art?                [:one  :boolean]
   :media:album-art:format              [:one  :ref    {:ref-to :data:format}]
   :data:audio:max-bit-rate             nil
   :data:audio:nominal-bit-rate         nil
   :data:media:bit-rate-mode            [:one  :string ]
   :data:media:codec-id:mediainfo       [:one  :string ]
   :data:media:format-profile           [:one  :string ]
   :data:media:artist:mediainfo         [:one  :string ] ; = performer
   :data:media:composer:mediainfo       [:one  :string ]
   :data:media:album:mediainfo          [:one  :string ]
   :data:media:album:artist:mediainfo   [:one  :string ]
   :data:date-encoded:mediainfo         [:one  :string ]
   :media:date-recorded:mediainfo       [:one  :string ]
   :media:itunes-cddb-1                 [:one  :string ]
   :data:media:source-data:mediainfo    [:one  :string ]
   :data:audio:encoding-settings        [:one  :string ]
   :data:media:genre                    nil
   :data:audio:encoded-date:mediainfo   [:one  :string ]
   :data:audio:date-tagged:mediainfo    [:one  :string ]
   :data:writing-library                [:one  :string ]
   :data:writing-application            [:one  :string ]
   :data:media:compilation:tika         [:one  :long   ]
   :time:duration                       nil})


