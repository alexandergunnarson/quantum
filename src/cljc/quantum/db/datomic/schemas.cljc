(ns quantum.db.datomic.schemas
  (:refer-clojure :exclude [type agent ref])
  (:require
#?(:clj
    [clojure.java.shell             :as shell  ])
    [quantum.db.datomic.entities
      :refer        [defattribute defentity
                     declare-entity defunit
                     schemas attributes]]
    [quantum.core.fn                :as fn
      :refer [fn1]]
    [quantum.core.log               :as log]
    [quantum.core.logic             :as logic
      :refer [fn-and]]
    [quantum.core.string            :as str    ]
    [quantum.core.error             :as err
      :refer [->ex TODO]]
    [quantum.net.http               :as http   ]
    [quantum.core.data.complex.xml  :as xml    ]
    [quantum.db.datomic             :as db     ]
    [quantum.core.convert.primitive :as pconv  ]
    [quantum.core.numeric           :as num
      :refer [percent?]                 ]))

(log/this-ns)

; IMPROVEMENTS
; - This whole ns lends itself to use with a subset of `clojure.spec` (via a modified `ValidatedMap`)
; =================

; =========== GENERAL =========== ;

(defrecord Schema [])
(defn ->schema [k]
  #_(:db/q '[:find [?ident ...]
           :where [_ :db/ident ?ident]])
  k)
(defrecord Any [])
(def ->any identity)

(defattribute :type
  [:one :ref {:ref-to :db/schema :index? true}])

(defentity :globals
  {:globals:log:levels [:many :keyword]})

#_(defentity :ratio:long ; Defined elsewhere
  {:doc "A ratio specifically using longs instead of bigints."}
  {:ratio:long:numerator   [:one :long]
   :ratio:long:denominator [:one :long]})

; =========== LOCATION =========== ;

(defattribute :location ; TODO
  [:one :keyword])

(defentity :location:country
  {:location nil})

; When dealing with locations, one
; uses the most restrictive area possible.
; the larger, enclosing areas are then inferred from it.
; Certain historical areas have an end date. All areas have
; a begin date.
; ISO 3166 codes
; The ISO 3166 codes are the codes assigned by ISO to countries and subdivisions.

; ; =========== LINGUISTICS =========== ;

(defentity :linguistics:language
  {:location nil})

; ; =========== UNITS =========== ;

(def units (atom #{}))

(defattribute :unit
  [:one :keyword #_{:unique :value}])

(defattribute :unit:kb-per-s         [:one :double])
(defattribute :unit:pixels           [:one :long  ])
(defattribute :unit:beats-per-minute [:one :double])

; ; =========== TIME =========== ;

(defattribute :time:instant
  [:one :long {:doc "Milliseconds from Unix epoch"}])
(defattribute :time:relative-instant
  [:one :long {:doc "Milliseconds from beginning; offset"}])

(defentity :time:range
  {:time:from [:one :ref {:ref-to :time:instant}]
   :time:to   [:one :ref {:ref-to :time:instant}]})

(defentity :time:relative-range
  {:time:from [:one :ref {:ref-to :time:relative-instant}]
   :time:to   [:one :ref {:ref-to :time:relative-instant}]})

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

(defunit :data:audio:bit-rate         :unit:kb-per-s)
(defunit :data:audio:max-bit-rate     :unit:kb-per-s)
(defunit :data:audio:nominal-bit-rate :unit:kb-per-s)
(defunit :data:video:bit-rate         :unit:kb-per-s)
(defunit :data:image:height           :unit:pixels  )
(defunit :data:image:width            :unit:pixels  )

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

; If a transaction specifies a unique identity for a temporary id,
; and that unique identity already exists in the database, then that temporary id
; will resolve to the existing entity in the system.
(defentity :data:format
  {:data:mime-type                  [:one  :keyword {:unique :identity :doc "Refer to http://www.sitepoint.com/web-foundations/mime-types-complete-list/"}]
   :data:appropriate-extension:many [:many :keyword {:doc "Needs to correspond with its mime-type"}]})

(defentity :byte-entity
  {:doc "A file's metadata. TODO figure out all of valid file metadata"}
  {:sha-512                             [:one  :string {:unique :value}]
   :in-collection-of:many               [:many :ref    {:ref-to :agent:person}]
   :cloud:amazon:id                     nil
   :data:format                         nil
   :date:created                        nil ; sample-date
   :date:last-modified                  nil
   :data:title                          nil
   :data:audio:beats-per-minute         [:one  :ref {:ref-to :unit:beats-per-minute}]
   :data:audio:codec-id                 [:one  :keyword]
   :data:audio:format-profile           [:one  :string ]
   :data:audio:format:info              [:one  :string ]
   :data:audio:id:mediainfo             [:one  :string ]
   :data:audio:stream-size              [:one  :string ]
   :data:audio:channels                 [:one  :long   ]
   :data:audio:channel-positions        [:one  :string ]
   :data:audio:compression-mode         [:one  :string ]
   :data:audio:compressor               [:one  :string ]
   :data:audio:format-version:tika      [:one  :string ]
   :data:audio:format-version           [:one  :long   ]
   :data:audio:mode                     [:one  :string ]
   :data:audio:mode-extension           [:one  :string ]
   :data:audio:writing-library          [:one  :string ]
   :data:audio:date-tagged:mediainfo    [:one  :string ]
   :data:audio:tagged-date:mediainfo    [:one  :string ]
   :data:tagged-date:mediainfo          [:one  :string ]

   :data:audio:sample-rate              nil

   :data:audio:bit-rate                 nil
   :data:audio:max-bit-rate             nil
   :data:audio:nominal-bit-rate         nil

   :data:bytes-size                     nil
   :data:codec-id                       [:one  :keyword]
   :data:creator                        nil

   :data:audio:encoding-settings        [:one  :string ]
   :data:audio:encoded-date:mediainfo   [:one  :string ]
   :data:audio:date-encoded:mediainfo   [:one  :string ]
   :data:date-encoded:mediainfo         [:one  :string ]
   :media:encoding-params               [:one  :string ]
   :data:fingerprint:fpcalc             [:one  :string ]
   :data:image:height                   nil
   :data:image:width                    nil
   :data:media:compatible-brands        [:one  :string ]
   :data:media:major-brand              [:one  :string ]
   :media:part:position                 [:one  :long   ]
   :media:part:total                    [:one  :long   ]

   :media:track:position                [:one  :long   ]
   :media:track:position:itunes         [:one  :long   ]

   :media:track:total                   [:one  :long   ]
   :media:album-art:type:mediainfo      [:one  :string ]
   :media:album-art:format              [:one  :ref    {:ref-to :data:format}]
   :media:has-album-art?                [:one  :boolean]
   :media:date-recorded:mediainfo       [:one  :string ]
   :media:itunes-cddb-1                 [:one  :string ]
   :data:media:source-data:mediainfo    [:one  :string ]
   :data:media:album:mediainfo          [:one  :string ]
   :data:media:album:artist:mediainfo   [:one  :string ]
   :data:media:artist:mediainfo         [:one  :string ] ; = performer
   :data:media:bit-rate-mode            [:one  :string ]
   :data:media:codec-id:mediainfo       [:one  :string ]
   :data:media:compilation?             [:one  :boolean]
   :data:media:compilation:tika         [:one  :long   ]
   :data:media:composer:mediainfo       [:one  :string ]
   :data:media:format-profile           [:one  :string ]
   :data:minor-version:mediainfo        [:one  :string ]
   :data:writing-library                [:one  :string ]
   :data:writing-application            [:one  :string ]

   :data:video:frame-rate               [:one  :double ]
   :data:frame-rate:mediainfo           [:one  :string ]
   :data:video:frame-rate-mode          [:one  :string ]

   :data:video:format-settings:gop      [:one  :string ]
   :data:video:aspect-ratio             [:one  :string ]
   :data:video:bit-depth                [:one  :double ]
   :data:video:bits-per-pixel*frame     [:one  :double ]
   :data:video:chroma-subsampling       [:one  :string ]
   :data:video:format                   [:one  :keyword]
   :data:video:format-settings:reframes [:one  :string ]
   :data:video:format-settings:cabac    [:one  :string ]
   :data:video:format-profile           [:one  :string ]
   :data:video:format:info              [:one  :string ]
   :data:video:scan-type                [:one  :string ]
   :data:video:stream-size              [:one  :string ]
   :data:video:bit-rate                 nil
   :data:video:id:mediainfo             [:one  :string ]
   :data:video:color-space              [:one  :string ]
   :data:video:codec-id                 [:one  :keyword]
   :data:video:codec-id:info            [:one  :string ]

   :time:duration                       nil

   :opinion:comment:many                nil})

; There are probably infinite types of ways to group a musical artwork.
; For instance, what would the Wohltemperierte Klavier be considered?
; Each piece is a piece/track, yes, but on top of that might be a prelude-fugue
; combo, and on top of that, a book (Book I, Book II), and only then, on
; top of that, the work as a whole.

; Of course, albums are much simpler. It's mainly in classical music that
; one runs into these problems..

; These are all ambiguous and are subsumed by tags
; :data:media:genre     nil
; :data:media:sub-genre nil ; from grouping; mood / sub-category
; :data:media:kind      nil
; :data:media:category  nil

(defentity :data:tag
  {:component? true}
  {:tag:value [:one :keyword {:unique :identity}]})

; There is a difference between the music on the page (work) and the
; music as performed and recorded (recording/release).
; There is, further, a difference between that and the music as
; sampled and written as bytes.

(defentity :work
  {:doc "A distinct intellectual or artistic creation.
         A work could be a piece of music, a movie, or even a novel,
         play, poem or essay, possibly, but not necessarily, later
         recorded as an oratory or audiobook."}
   )


; TODO MusicBrainz
; Disambiguation comment
; Annotation

(defentity :work:discrete
  {:doc "A discrete, individual :work.
         E.g. an individual song, musical number or movement.
         This includes recitatives, arias, choruses, duos, trios, etc.
         In many cases, discrete works are a part of larger, aggregate works."}
  {:work:discrete:name
    [:one :string {:doc "The canonical title of the work, expressed in
                         the language it was originally written."}]
   :work:discrete:type
     [:one :keyword]
   :work:discrete:alias:many
     [:many :string {:doc "If a discrete work is known by name(s) or
                           in language(s) other than its canonical name,
                           these are specified in the work’s aliases."}]
   :work:discrete:iswc
     [:one :keyword {:doc "The International Standard Musical Work Code
                           assigned to the work by copyright collecting agencies."}]
   :work:discrete:mbid
     [:one :keyword {:doc "MusicBrainz UUID"}]})

(defentity :work:aggregate
  {:doc "An ordered sequence of one or more `work:discrete`s.
         Could be e.g. songs, numbers or movements, such as:
         - Symphony
         - Opera
         - Theatre work
         - Concerto
         - Concept album
         - Etc.
         A popular music album is not considered a distinct aggregate
         work unless it is evident that such an album was written with
         intent to have a specifically ordered sequence of related songs
         (i.e. a “concept album”)."}
  {})

; Work-to-Artist relationship
; A work can be associated with one or more composer, arranger, instrumentator, orchestrator, lyricist, librettist, translator and publisher.

; Work-to-Recording relationship
; A work can be associated with one or more recordings. This provides the indirect association between a work and its performance and production artists.

; Work-to-Work relationships
; A work can be associated with one or more other works. There are two types of work-work relationships:

; Part-of-work relationship
; A work can be expressed as a part of another work.

; Derivative work relationship
; A work can be expressed as being derived from one or more other works. Examples: instrumental work with lyrics added later, translation of a work into a different language, mashup.

; Works do not have an artist of their own, all artists are derived from the work's
; relationships. A work will show up under the works tab for any artist directly
; linked to a work (e.g. composers, lyricists). Any works linked to the artist's
; recordings will also be shown there.

(defentity :work:discrete:playable
  {:doc "A :work which can be expressed in the form of one or more
         audio/video recordings.
         Something that an :agent can play back.
         AKA a musical/videographical artwork.
         Not directly tied to a :byte-entity, but rather references
         a part or whole of it (whether video or audio)."}
  {:media:track:byte-entity       [:one  :ref {:ref-to :byte-entity}]
   :media:track:byte-entity:range [:one  :ref {:ref-to :time:relative-range}]
   :date:purchased-by             [:one  :ref {:ref-to :time:instant}]
   :in-collection-of:many         [:many :ref {:ref-to :agent:person}]
   ; Date Added         ; get from first transaction
   ; Date Last Modified ; get from transactions
   ; Date Last Accessed ; get from queries
   :opinion:rating:many           nil
   :opinion:comment:many          nil
   :art:creator                   nil
   :data:tag:many                 [:many :ref {:ref-to :data:tag}]
   :data:media:description        [:one  :string]
   :data:media:release-date       [:one  :ref {:ref-to :time:instant}] ; for album?
   :data:media:agent+plays:many
     [:many :ref {:ref-to :media:agent+plays}]
   :data:media:skipped-instants
     [:many :ref {:ref-to :time:instant :doc "Instants when the track was skipped"}]
   ; producer?
   ; collaborators?
   })
