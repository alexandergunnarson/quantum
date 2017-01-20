(ns quantum.db.datomic.schemas
  "Somewhat similar to SparkFund/spec-tacular, though this was a discovery only
   after the fact."
  (:refer-clojure :exclude [type agent ref])
  (:require
    [quantum.core.fn                :as fn
      :refer [fn1]]
    [quantum.core.log               :as log]
    [quantum.core.logic             :as logic
      :refer [fn-and]]
    [quantum.core.string            :as str]
    [quantum.core.error             :as err
      :refer [->ex TODO]]
    [quantum.core.data.validated    :as dv
      :refer [def-validated def-validated-map declare-spec]]
    [quantum.core.validate          :as v]
    [quantum.db.datomic             :as db]
    [quantum.core.convert.primitive :as pconv]
    [quantum.core.numeric           :as num
      :refer [percent?]]))

(log/this-ns)

; IMPROVEMENTS
; - Validator should be serialized as transactor function
; - Look at MusicBrainz Style guides when submitting new materials
; - https://musicbrainz.org/doc/Live_Data_Feed
; Inner-defined entities are components by default
; :annotation is allowed with any entity
; ^:db? true enters into DB mode, which limits the shape of the possible data
; =================

; =========== CORE TYPES =========== ;

(def-validated ^:db? schema/type :db/schema)

(def-validated ^:db? ^{:doc "Any unstructured data associated with an entity"}
  schema/annotation :db/string)

(def-validated-map ^:db?
  ^{:doc  "A ratio specifically using longs instead of bigints."
    :todo #{"Throw on overflow from long"}
    :tests `{(->ratio:long {:numerator   2
                            :denominator 4})
             {:numerator   1
              :denominator 2}}}
  schema/ratio:long
  #?@(:clj [:conformer (fn [x] (let [n          (:this/numerator   x)
                                     d          (:this/denominator x)
                                     simplified (/ n d)
                                     n'         (long (numerator   simplified))
                                     d'         (long (denominator simplified))]
                                 (if (and (= n n') (= d d'))
                                     x
                                     (assoc x :this/numerator   n'
                                              :this/denominator d'))))])
  :req-un [(def :this/numerator   :db/long)
           (def :this/denominator :db/long)])

; =========== LOCATION =========== ;

(def-validated ^:db?
  ^{:doc  "When dealing with locations, one
           uses the most restrictive area possible.
           the larger, enclosing areas are then inferred from it.
           Certain historical areas have an end date. All areas have
           a begin date.
           ISO 3166 codes
           The ISO 3166 codes are the codes assigned by ISO to countries and subdivisions."
    :todo #{"Finish"}}
  schema/location :db/keyword)

; TODO should this mean :location:country is a :ref or a :db/keyword ?
(def-validated ^:db? location/country :location)

; =========== LINGUISTICS =========== ;

(def-validated-map ^:db?
  ^{:doc    "An ISO 639-3 value?
             There is also ISO 639-1, 639-2, 639-4, 639-5"
    ;:unique :value ; TODO enforce in other ways
    :todo   #{"Finish"}}
  linguistics/language
  :req [(def :this/iso-639-3-value :db/keyword)])

; The possible values are taken from the ISO 15924 standard.
(def-validated-map ^:db?
  ^{:todo  #{"Finish"}}
  linguistics/script
  :req [^{:unique :value}
        (def :this/iso-15924-value :db/keyword)])

; =========== UNITS =========== ;

(def units (atom #{}))

(def-validated ^:db? schema/unit :db/keyword)

(def-validated ^:db? unit/kb-per-s         :db/double)
(def-validated ^:db? unit/pixels           :db/long  )
(def-validated ^:db? unit/beats-per-minute :db/double)

; TODO Currency
; ISO 4217 delineates currency designators, country codes (alpha and numeric)

; ; =========== TIME =========== ;

(def-validated ^:db?
  ^{:doc "Milliseconds from Unix epoch"}
  time/instant :db/long)

(def-validated ^:db?
  ^{:doc "Milliseconds from beginning; offset"}
  time/relative-instant :db/long)

(def-validated-map ^:db? time/range
  :req [(def :this/from :time/instant)
        (def :this/to   :time/instant)])

(def-validated-map ^:db?
  ^{:doc "A range starting from and ending on relative instants"}
  time/relative-range
  :req [(def :this/from :time/relative-instant)
        (def :this/to   :time/relative-instant)])

(def-validated ^:db?
  ^{:doc    "Not the sum of a year's time, but a particular year."
    ;:unique :value ; TODO enforce in other ways
  }
  time/year :time/range)

(def-validated ^:db?
  ^{:doc "In milliseconds"}
  time/duration :db/long)

; ; =========== META =========== ;

(def-validated ^:db? schema/date        :time/instant)
(def-validated ^:db? date/created       :time/instant)
(def-validated ^:db? date/last-modified :time/instant)

(def-validated ^:db?
  ^{:doc "For media tracks, date last played.
          For other things, date last accessed/opened/viewed."}
  date/last-accessed :time/instant)

; =========== PERSON NAME =========== ;

; Many of these are keywords because the same names happen over and over again


(def-validated-map ^:db? ^:component?
  ^{:todo #{"Support hyphenated and maiden names"}}
  agent:person/name
  ;:invariant (fn [x] (contains? x (:this/called-by x)))
  :req-un [^{:doc "The preferred name (some go by prefix, some first, some middle)"}
           (def :this/called-by (v/and :db/schema #{:this/prefix :this/first :this/middle}))]
  :opt-un [(def :this/legal-in  (v/set-of :location/country))
           ^{:doc "e.g. His Holiness, Dr., Sir. TODO: need to prefix"}
           (def :this/prefix    (v/set-of :db/keyword))
           ^{:doc "e.g. \"José\" in \"José Maria Gutierrez de Santos\""}
           (def :this/first     :db/keyword)
           ^{:doc "e:g. \"Maria\" in \"José Maria Gutierrez de Santos\""}
           (def :this/middle    :db/keyword)
           ^{:doc "E:g. \"Gutierrez de Santos, III\""}
           (def :this/last
             :opt-un
             [^{:doc "refers to a schema, e.g. \":agent:person:name:surname\" if it's a
                      simple one like \"Smith\""}
              (def :this/primary  :db/schema )
              ^{:doc "e.g. \"Smith\", or a complex non-paternal-maternal name"}
              (def :this/surname  :db/keyword)
              ^{:doc "e.g. \"Gutierrez\" in \"Gutierrez de Santos\""}
              (def :this/maternal :db/keyword)
              ^{:doc "e.g. \"de Santos\" in \"Gutierrez de Santos\""}
              (def :this/paternal :db/keyword)
              ^{:doc "e.g. \"Jr., Sr., III\""}
              (def :agent:person:name/suffix :db/keyword)])])

(def-validated ^:db?
  ^{:doc "AKA nickname"}
  agent:person:name/alias :db/keyword)

; =========== REGISTRATION =========== ;

(def-validated-map ^:db? ^:sensitive? ^:no-history? auth/credential:oauth2
  :req-un [(def :this/redirect-uri  :db/string) ; TODO validate uri?
           (def :this/client-id     :db/string) ; TODO validate uniquity?
           (def :this/client-secret :db/string)
           (def :this/scopes        (v/set-of :db/keyword))] ; TODO validate for different providers e.g. Google
  :opt-un [(def :this/access-token
             :req-un [(def :this/value   :db/string )]
             :opt-un [(def :this/expires :db/instant)])
           (def :this/refresh-token :db/string)])

(declare-spec ^:db? agent/organization)

(def-validated-map ^:db? ^:component?
  agent/registration
  :req [(def :this/provider :agent/organization)]
  :opt [^{:doc "Reference to a set of Twitter, Facebook, etc. facts"}
        (def :this/detail (v/set-of :db/any))])

; =========== NETWORK =========== ;

(def-validated-map ^:db? ; ^{:unique :value} ; TODO enforce other ways
  network/domain
  :req-un [(def :this/name       :db/string)
           ^{:doc "Keyword because it's short and universal"}
           (def :this/tld        :db/keyword)]
  :opt-un [^{:unique :value
             :doc    "Multiple domain names can point to the same IP address"}
           (def :this/ip-address :db/string) ; technically it's more complex than just a string
           ])

(def-validated-map ^:db? ; ^{:unique :value} ; TODO enforce other ways
  agent/email
  :req-un [^{:doc "E.g. alexandergunnarson"}
           (def :this/username :db/keyword)
           :network/domain]
  :opt-un [^{:doc "E.g. company, email validation service, etc."}
           (def :this/validated-by :agent/organization)
           ^{:doc "Who/what provided the email information"}
           (def :this/source (v/or* :agent/organization :agent/person))])

; =========== AGENT =========== ;

(def-validated agent/auth:many (v/set-of :auth/credential:oauth2))

(def-validated-map ^:db? agent/person
  :opt-un [(def :this/name:many         (v/set-of :agent:person/name))
           (def :this/name:alias:many  (v/set-of :agent:person/name:alias))
           ^{:doc "What gender a person identifies as: male, female or other."}
           (def :this/gender             (v/and :db/keyword #{:male :female :other}))
           (def :agent/email:many        (v/set-of :agent/email))
           (def :agent/registration:many (v/set-of :agent/registration))
           :agent/auth:many
           :musicbrainz/id])

(def-validated-map ^:db?
  ^{:component? true
    :doc "The official name of an organization"
    :todo #{"Finish"}}
  agent:organization/name
  :opt-un [(def :this/legal-in (v/set-of :location/country))])

(def-validated ^:db?
  ^{:unique :value
    :doc    "The International Standard Name Identifier for an agent.
             An ISO standard for uniquely identifying the public identities
             of contributors to media content."}
  agent/isni :db/string)

(def-validated-map ^:db? agent/organization
  :opt-un [(def :this/name:many (v/set-of :this/name))
           ^{:doc "#{[:group     {:doc \"A group of people that may or may not have a distinctive name.\"}]
                     [:orchestra {:doc \"A large instrumental ensemble.\"}]
                     [:choir     {:doc \"A choir/chorus/chorale (a large vocal ensemble).\"}]}"}
           (def :this/types     (v/set-of #{:group :orchestra :choir}))
           :agent/isni
           :musicbrainz/id])

(def-validated ^:db? art/creator
  ^{:doc "In the instance of music, composer. For images, artist.
          Includes producers and engineers, photographers, illustrators, poets, etc."}
  (v/or* :agent/person :agent/organization))

(def-validated ^:db? art/creator-group
  (v/or* :agent/person :agent/organization))

; Area
; The artist area, as the name suggests, indicates the area with which an artist is primarily identified with. It is often, but not always, his/her/their birth/formation country.
;
; Begin and end dates
; The begin and end dates indicate when an artist started and finished its existence. Its exact meaning depends on the type of artist:
;
; For a person
; Begin date represents date of birth, and end date represents date of death.
; For a group (or orchestra/choir)
; Begin date represents the date when the group first formed: if a group dissolved and then reunited, the date is still that of when they first formed. End date represents the date when the group last dissolved: if a group dissolved and then reunited, the date is that of when they last dissolved (if they are together, it should be blank!). For listing other inactivity periods, just use the annotation and the "member of" relationships.
; For a character
; Begin date represents the date (in real life) when the character concept was created. The End date should not be set, since new media featuring a character can be created at any time. In particular, the Begin and End date fields should not be used to hold the fictional birth or death dates of a character. (This information can be put in the annotation.)
; For others
; There are no clear indications about how to use dates for artists of the type Other at the moment.
; IPI code
; An IPI (interested party information) code is an identifying number assigned by the CISAC database for musical rights management. See IPI for more information, including how to find these codes.
;
; Alias
; Aliases are used to store alternate names or misspellings. For more information and examples, see the page about aliases.

(def-validated ^:db?
  ^{:doc "E.g. software, process, person, organization."
    :todo #{"Extend to non-agent causers"}}
  data/creator (v/or* :agent/person :agent/organization))

(def-validated ^:db? ^:component?
  ^{:doc "Where the data was retrieved from."}
  data/source :db/any)

(def-validated-map ^:db? ^:component? data/certainty
  :opt [^{:doc  "From what source do you get your certainty?"
          :todo #{"Make into a logical proposition, not just a source entity"}}
        (def :this/source :data:source)]
  :req [^{:doc "The certainty that the data is the case / true, [0,1]"}
        (def :this/value  (v/and :db/double quantum.core.numeric/percent?))])

; =========== MEDIA =========== ;

(def-validated ^:db? data/title       :db/string)

(def-validated ^:db? data/description :db/string)

(def-validated-map ^:db? opinion/comment
  :req [^{:doc "Can be in any format — markdown, HTML, plain, etc."}
        (def :this/text           :db/string)]
  :opt [(def :this/created-by     (v/or* :agent/organization :agent/person))
        (def :this/in-response-to :this)])

(def-validated ^:db?
  ^{:doc "The comment chain is dynamically created from the list of comments"}
  opinion/comment:many
  (v/set-of :opinion/comment))

(def-validated-map ^:db?
  ^{:doc "The rating that a particular person gave"}
  opinion/rating
  :req [^{:doc "Can be any range"}
        (def :this/values      :db/double)]
  :opt [(def :this/explanation :db/string)])

(def-validated-map ^:db? ^:component? opinion/entity+rating
  :req [(def :opinion/opiner (v/or* :agent/person :agent/organization))
        :opinion/rating])

(def-validated-map ^:db? opinion/rating:many
  :req [(def :opinion/entity+rating:many
             (v/set-of :opinion/entity+rating))])

(def-validated ^:db? schema/agent
  (v/or* :this/person :this/organization))

(def-validated-map ^:db? ^:component?
  media/agent+plays
  :req [:agent
        ^{:doc "Dates played"}
        (def :media/plays (v/set-of :time/instant))])

(def-validated ^:db?
  ^{:doc "E.g. 44100 kHz"}
  data:audio/sample-rate :db/double)

#_(defunit :data:audio:bit-rate         :unit:kb-per-s)
#_(defunit :data:audio:max-bit-rate     :unit:kb-per-s)
#_(defunit :data:audio:nominal-bit-rate :unit:kb-per-s)
#_(defunit :data:video:bit-rate         :unit:kb-per-s)
#_(defunit :data:image:height           :unit:pixels  )
#_(defunit :data:image:width            :unit:pixels  )

#_(defentity :media:track-num+track
  {:media:track-num [:one :long {:unique :value :doc "AKA episode-num"}] ; unique... FIX THIS
   :media:track nil})

(def-validated ^:db? data/bytes-size :db/long)

; TODO are IDs unique per user or universally unique?
(def-validated ^:db? ^{:unique :value} cloud:amazon/id :db/string)

; If a transaction specifies a unique identity for a temporary id,
; and that unique identity already exists in the database, then that temporary id
; will resolve to the existing entity in the system.
(def-validated-map ^:db? data/format
  :req [^{:unique :value}
        ^{:doc "Refer to http://www.sitepoint.com/web-foundations/mime-types-complete-list/"}
        (def :data/mime-type :db/keyword)
        ^{:doc "Needs to correspond with its mime-type"}
        (def :data/appropriate-extension:many (v/set-of :db/keyword))])

(def-validated ^:db? cloud:s3/uri :db/uri)

(def-validated-map ^:db?
  ^{:doc  "A file's metadata."
    :todo #{"figure out all of valid file metadata"}}
  schema/byte-entity
  :opt [^{:unique :value}
        (def :schema/sha-512                      :db/string)
        (def :this/mediainfo                      :db/string) ; TODO extract the unstructured data later
        (def :data:fingerprint/fpcalc             :db/string )
        :cloud:s3/uri
        :cloud:amazon/id
        :data/format
        ; sample-date
        :date/created
        :date/last-modified
        :data/title
        ; (def :data:audio:beats-per-minute         :unit:beats-per-minute)
        ; (def :data:audio:codec-id                 :db/keyword)
        ; (def :data:audio:format-profile           :db/string )
        ; (def :data:audio:format:info              :db/string )
        ; (def :data:audio:id:mediainfo             :db/string )
        ; (def :data:audio:stream-size              :db/string )
        ; (def :data:audio:channels                 :db/long   )
        ; (def :data:audio:channel-positions        :db/string )
        ; (def :data:audio:compression-mode         :db/string )
        ; (def :data:audio:compressor               :db/string )
        ; (def :data:audio:format-version:tika      :db/string )
        ; (def :data:audio:format-version           :db/long   )
        ; (def :data:audio:mode                     :db/string )
        ; (def :data:audio:mode-extension           :db/string )
        ; (def :data:audio:writing-library          :db/string )
        ; (def :data:audio:date-tagged:mediainfo    :db/string )
        ; (def :data:audio:tagged-date:mediainfo    :db/string )
        ; (def :data:tagged-date:mediainfo          :db/string )

        ; :data:audio:sample-rate

        ; :data:audio:bit-rate
        ; :data:audio:max-bit-rate
        ; :data:audio:nominal-bit-rate

        :data/bytes-size
        ; (def :data:codec-id                       :db/keyword)
        ; :data:creator

        ; (def :data:audio:encoding-settings        :db/string )
        ; (def :data:audio:encoded-date:mediainfo   :db/string )
        ; (def :data:audio:date-encoded:mediainfo   :db/string )
        ; (def :data:date-encoded:mediainfo         :db/string )
        ; (def :media:encoding-params               :db/string )
        :data:image/height
        :data:image/width
        ; (def :data:media:compatible-brands        :db/string )
        ; (def :data:media:major-brand              :db/string )
        ; (def :media:part:position                 :db/long   )
        ; (def :media:part:total                    :db/long   )

        ; (def :media:track:position                :db/long   )
        ; (def :media:track:position:itunes         :db/long   )

        ; (def :media:track:total                   :db/long   )
        ; (def :media:album-art:type:mediainfo      :db/string )
        ; (def :media:album-art:format              :data:format)
        ; (def :media:has-album-art?                :db/boolean)
        ; (def :media:date-recorded:mediainfo       :db/string )
        ; (def :media:itunes-cddb-1                 :db/string )
        ; (def :data:media:source-data:mediainfo    :db/string )
        ; (def :data:media:album:mediainfo          :db/string )
        ; (def :data:media:album:artist:mediainfo   :db/string )
        ; (def :data:media:artist:mediainfo         :db/string ) ; = performer
        ; (def :data:media:bit-rate-mode            :db/string )
        ; (def :data:media:codec-id:mediainfo       :db/string )
        ; (def :data:media:compilation?             :db/boolean)
        ; (def :data:media:compilation:tika         :db/long   )
        ; (def :data:media:composer:mediainfo       :db/string )
        ; (def :data:media:format-profile           :db/string )
        ; (def :data:minor-version:mediainfo        :db/string )
        ; (def :data:writing-library                :db/string )
        ; (def :data:writing-application            :db/string )

        ; (def :data:video:frame-rate               :db/double )
        ; (def :data:frame-rate:mediainfo           :db/string )
        ; (def :data:video:frame-rate-mode          :db/string )

        ; (def :data:video:format-settings:gop      :db/string )
        ; (def :data:video:aspect-ratio             :db/string )
        ; (def :data:video:bit-depth                :db/double )
        ; (def :data:video:bits-per-pixel*frame     :db/double )
        ; (def :data:video:chroma-subsampling       :db/string )
        ; (def :data:video:format                   :db/keyword)
        ; (def :data:video:format-settings:reframes :db/string )
        ; (def :data:video:format-settings:cabac    :db/string )
        ; (def :data:video:format-profile           :db/string )
        ; (def :data:video:format:info              :db/string )
        ; (def :data:video:scan-type                :db/string )
        ; (def :data:video:stream-size              :db/string )
        ; :data:video:bit-rate
        ; (def :data:video:id:mediainfo             :db/string )
        ; (def :data:video:color-space              :db/string )
        ; (def :data:video:codec-id                 :db/keyword)
        ; (def :data:video:codec-id:info            :db/string )

        :time/duration

        :opinion/comment:many
        ]                )

; There are probably infinite types of ways to group a musical artwork.
; For instance, what would the Wohltemperierte Klavier be considered?
; Each piece is a piece/track, yes, but on top of that might be a prelude-fugue
; combo, and on top of that, a book (Book I, Book II), and only then, on
; top of that, the work as a whole.

; Of course, modern works are much simpler. It's mainly in classical music that
; one runs into these problems.

#_(def-validated-map ^:db? ^:component?
  ^{:doc "The following are all ambiguous and are subsumed by this schema:
          - genre
          - sub-genre
          - kind
          - category"}
  data:tag
  :req-un [^{:unique :value}
           (def :this/value :db/keyword)])

#_(def-validated-map ^:db?
  ^{:doc "A distinct intellectual or artistic creation.
          A work could be a piece of music, a movie, or even a novel,
          play, poem or essay, possibly, but not necessarily, later
          recorded as an oratory or audiobook."}
  work
  :opt-un [:musicbrainz:id])

#_(def-validated-map ^:db?
  ^{:doc "A discrete, individual :work.
          E.g. an individual song, musical number or movement.
          This includes recitatives, arias, choruses, duos, trios, etc.
          In many cases, discrete works are a part of larger, aggregate works."}
  work:discrete
  :req-un [^{:doc "The canonical title of the work, expressed in
                   the language it was originally written."}
           (def :work:discrete:name :db/string)]
  :opt-un [(def :work:discrete:type :db/keyword)
           ^{:doc "If a discrete work is known by name(s) or
                   in language(s) other than its canonical name,
                   these are specified in the work’s aliases."}
           (def :work:discrete:alias:many (v/set-of :db/string))
           ^{:doc "The International Standard Musical Work Code
                   assigned to the work by copyright collecting agencies."}
           (def :work:discrete:iswc :db/keyword)
           :musicbrainz:id])

#_(def-validated-map ^:db?
  ^{:doc "An ordered sequence of one or more `work:discrete`s.
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
  work:aggregate
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

#_(defentity :work:discrete:playable
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

; TODO
#_(def-validated-map :media:release
  {:doc "Represents the unique release (i.e. issuing) of a product on a specific
         date with specific release information such as the country, label, barcode,
         packaging, etc. If you walk into a store and purchase an album or single,
         they're each represented as one release.
         Each release belongs to a release group and contains at least one medium
         (commonly referred to as a disc when talking about a CD release).
         Tracklists represent the set and ordering of tracks as listed on a liner,
         and the same tracklist can appear on more than one release. For example,
         a boxset compilation that contains previously released CDs would share the
         same tracklists as the separate releases."}
  (def :this/title :db/string)
   ^{:doc "The artist(s) that the release is primarily
           credited to, as credited on the release."}
   (def :this/artist:many (v/set-of :artist))
   ^{:doc "The date the release was issued (made available through
           some sort of distribution mechanism). For example, this
           may be via a retail store, being published as a free
           download on a website or distributed to industry insiders
           (in the case of promotional releases), among other mechanisms."}
   (def :this/date :time:instant)
   :this/location
     [:one  :ref    {:ref-to :location
                     :doc    "The location the release was issued in.
                              Generally this will only be of country
                              precision."}]
   :this/label:many
     [:many :ref    {:ref-to :entity:media-label
                     :doc    "The label(s) which issued the release."}]
   :this/catalog-number
     [:one  :long   {:doc    "This is a number assigned to the release by the label
                              which can often be found on the spine or near the barcode.
                              There may be more than one, especially when multiple labels
                              are involved. This is not the ASIN — there is a relationship
                              for that — nor the label code."}]
   :this/barcode
     [:one  :string {:doc    "The barcode, if the release has one. The most common types
                              found on releases are 12-digit UPCs and 13-digit EANs."}]
   :this/packaging
     [:one  :ref    {:enum   #{[:discbox-slider            {:doc "A pouch-like package with an internal mechanism that pushes the contents
                                                                  (usually a CD) out of the case when the lid flap is opened."}]
                               [:book                      {:doc "A book with a sleeve containing a medium (usually a CD)."}]
                               [:cassette-case             {:doc "Regular plastic case as for a cassette."}]
                               [:fatbox                    {:doc "A double-sided, double-width jewel case capable of holding up to 4 CDs."}]
                               [:gatefold-cover            {:doc "A cardboard sleeve that folds in halves, thirds, etc. It can hold
                                                                  multiple records or CDs as well as booklets, posters and other memorabilia."}]
                               [:jewel-case                {:doc "The traditional CD case, made of hard, brittle plastic."}]
                               [:slim-jewel-case           {:doc "A thinner jewel case, commonly used for CD singles."}]
                               [:digipak                   {:doc "A folded cardboard outer, typically made of coated cardboard, with a plastic tray glued into it."}]
                               [:cardboard-or-paper-sleeve {:doc "A sleeve, made of paper or cardboard. Traditional packaging for records, also seen with CDs."}]
                               [:keep-case                 {:doc "The traditional DVD case, made of soft plastic (usually) dark grey with a thin transparent plastic cover protecting the artwork."}]
                               [:none                      {:doc "No packaging at all. Common for digital media (downloads)."}]
                               [:snap-case                 {:doc "A digipak-like case held together with a \"snapping\" plastic closure."}]}
                     :doc    "The physical packaging that accompanies the release."}]
   :this/language
     [:one  :ref    {:ref-to :linguistics:language
                     :doc    "The language a release's track list is written in."}]
   :this/script
     [:one  :ref    {:ref-to :linguistics:script
                     :doc    "The script used to write the release's track list."}]
   :musicbrainz:id
   ^{:doc "How good the data for a release is. It is not a mark of
           how good or bad the music itself is - for that, use ratings.
           1:   All available data has been added, if possible including
                cover art with liner info that proves it.
           0.5: This is the default setting - technically \"unknown\" if
                the quality has never been modified; \"normal\" if it has.
           0:   The release needs serious fixes, or its existence is hard
                to prove (but it's not clearly fake)."}
   (def :this/data-quality (v/and :db/long (fn1 <= 0 1))))

; Status
; The status describes how "official" a release is. Possible values are:

; official
; Any release officially sanctioned by the artist and/or their record company. Most releases will fit into this category.
; promotional
; A give-away release or a release intended to promote an upcoming official release (e.g. pre-release versions, releases included with a magazine, versions supplied to radio DJs for air-play).
; bootleg
; An unofficial/underground release that was not sanctioned by the artist and/or the record company. This includes unofficial live recordings and pirated releases.
; pseudo-release
; An alternate version of a release where the titles have been changed. These don't correspond to any real release and should be linked to the original release using the transl(iter)ation relationship.


#_(defentity :media:release-group
  {:doc "Used to group several different releases into a single logical entity.
         Every release belongs to one, and only one release group.
         Both release groups and releases are \"albums\" in a general sense,
         but with an important difference: a release is something you can buy
         as media such as a CD or a vinyl record, while a release group embraces
         the overall concept of an album — it doesn't matter how many CDs or
         editions/versions it had.
         When an artist says \"We've released our new album\", they're talking
         about a release group. When their publisher says \"This new album gets
         released next week in Japan and next month in Europe\", they're
         referring to the different releases that belong in the release group."}
  {:musicbrainz:id nil
   :this/title
     [:one       {:doc "The title of a release group is usually very similar, if not
                        the same, as the titles of the releases contained within it."}]
   :this/type
     [:one       {:doc "Describes what kind of releases the release group represents,
                        for example album, single, soundtrack, compilation etc."}]
   :media:release:many
     [:many :ref {:ref-to :media:release}]
   :media:release:artist:many
     [:many :ref {:ref-to :artist
                  :doc "The artist of a release group is usually very similar, if not
                        the same, as the artist of the releases contained within it.
                        In MusicBrainz, multiple artists can be linked using artist
                        credits."}]})

; Description
; The type of a release group describes what kind of release group it is. It is divided in two: a release group can have a "main" type and an unspecified number of extra types.



; Primary types
; Album
; An album, perhaps better defined as a "Long Play" (LP) release, generally consists of previously unreleased material (unless this type is combined with secondary types which change that, such as "Compilation").

; Single
; A single has different definitions depending on the market it is released for.

; In the US market, a single typically has one main song and possibly a handful of additional tracks or remixes of the main track; the single is usually named after its main song; the single is primarily released to get radio play and to promote release sales.
; The U.K. market (also Australia and Europe) is similar to the US market, however singles are often released as a two disc set, with each disc sold separately. They also sometimes have a longer version of the single (often combining the tracks from the two disc version) which is very similar to the US style single, and this is referred to as a "maxi-single". (In some cases the maxi-single is longer than the release the single comes from!)
; The Japanese market is much more single driven. The defining factor is typically the length of the single and the price it is sold at. Up until 1995 it was common that these singles would be released using a mini-cd format, which is basically a much smaller CD typically 8 cm in diameter. Around 1995 the 8cm single was phased out, and the standard 12cm CD single is more common now; generally re-releases of singles from pre-1995 will be released on the 12cm format, even if they were originally released on the 8cm format. Japanese singles often come with instrumental versions of the songs and also have maxi-singles like the UK with remixed versions of the songs. Sometimes a maxi-single will have more tracks than an EP but as it's all alternate versions of the same 2-3 songs it is still classified as a single.
; There are other variations of the single called a "split single" where songs by two different artists are released on the one disc, typically vinyl. The term "B-Side" comes from the era when singles were released on 7 inch (or sometimes 12 inch) vinyl with a song on each side, and so side A is the track that the single is named for, and the other side - side B - would contain a bonus song, or sometimes even the same song.

; EP
; An EP is a so-called "Extended Play" release and often contains the letters EP in the title. Generally an EP will be shorter than a full length release (an LP or "Long Play") and the tracks are usually exclusive to the EP, in other words the tracks don't come from a previously issued release. EP is fairly difficult to define; usually it should only be assumed that a release is an EP if the artist defines it as such.

; Broadcast
; An episodic release that was originally broadcast via radio, television, or the Internet, including podcasts.

; Other
; Any release that does not fit or can't decisively be placed in any of the categories above.

; Secondary types
; Compilation
; A compilation, for the purposes of the MusicBrainz database, covers the following types of releases:

; a collection of recordings from various old sources (not necessarily released) combined together. For example a "best of", retrospective or rarities type release.
; a various artists song collection, usually based on a general theme ("Songs for Lovers"), a particular time period ("Hits of 1998"), or some other kind of grouping ("Songs From the Movies", the "Café del Mar" series, etc).
; The MusicBrainz project does not generally consider the following to be compilations:

; a reissue of an album, even if it includes bonus tracks.
; a tribute release containing covers of music by another artist.
; a classical release containing new recordings of works by a classical artist.
; Compilation should be used in addition to, not instead of, other types: for example, a various artists soundtrack using pre-released music should be marked as both a soundtrack and a compilation. As a general rule, always select every secondary type that applies.

; Soundtrack
; A soundtrack is the musical score to a movie, TV series, stage show, computer game etc. In the specific cases of computer games, a game CD with audio tracks should be classified as a soundtrack: the musical properties of the CD are more interesting to MusicBrainz than the data properties.

; Spokenword
; Non-music spoken word releases.

; Interview
; An interview release contains an interview, generally with an artist.

; Audiobook
; An audiobook is a book read by a narrator without music.

; Live
; A release that was recorded live.

; Remix
; A release that primarily contains remixed material.

; DJ-mix
; A DJ-mix is a sequence of several recordings played one after the other, each one modified so that they blend together into a continuous flow of music. A DJ mix release requires that the recordings be modified in some manner, and the DJ who does this modification is usually (although not always) credited in a fairly prominent way.

; Mixtape/Street
; Promotional in nature (but not necessarily free), mixtapes and street albums are often released by artists to promote new artists, or upcoming studio albums by prominent artists. They are also sometimes used to keep fans' attention between studio releases and are most common in rap & hip hop genres. They are often not sanctioned by the artist's label, may lack proper sample or song clearances and vary widely in production and recording quality. While mixtapes are generally DJ-mixed, they are distinct from commercial DJ mixes (which are usually deemed compilations) and are defined by having a significant proportion of new material, including original production or original vocals over top of other artists' instrumentals. They are distinct from demos in that they are designed for release directly to the public and fans; not to labels.

#_(defentity :media:medium
  {:doc "The actual physical medium the content is stored upon.
         One of the physical, separate things you would get when you buy something in
         e.g. a record store. They are the individual CDs, vinyls, etc. contained
         within the packaging of an album (or any other type of release). Mediums
         are always included in a release, and have a position in said release
         (e.g. disc 1 or disc 2). They have a format, like CD, 12\" vinyl or cassette
         (in some cases this will be unknown), and can have an optional title (e.g.
         disc 2: The Early Years).
         Each CD in a multi-disc release will be entered as separate mediums within a
         release, and that both sides of a vinyl record or cassette will exist on one
         medium. Mediums have a format (e.g. CD, DVD, vinyl, cassette) and can
         optionally also have a title.
         Each medium has a tracklist.
         Note that a side of a disc, like side A of a vinyl, is not its own medium;
         the whole vinyl disc is. Digital (as opposed to physical) releases don't have
         \"real\" mediums, but they should be entered as several mediums if they are
         officially divided in several \"discs\". Exceptions in the treatment of
         mediums can exist: DualDiscs are usually entered as two mediums if the
         tracklist is the same on both sides, but with different mixes.
         Disc IDs are linked to mediums.

         Examples
         These are mediums of their respective releases.

         CD 1 of the 1984 US release of \"The Wall\" by Pink Floyd.
         CD 2 of the 2005 UK release of \"Aerial\" by Kate Bush, named \"A Sky of Honey\".
         12\" vinyl 1 of the 1975 US release of \"Physical Graffiti\" by Led Zeppelin.
         And also 12\" vinyl 2 of the same release (but not sides A, B, C and D!).
         Digital Media 1 of the 2010 US digital release of \"My Beautiful Dark Twisted
         Fantasy\" by Kanye West."}
  {:media:medium:name   [:one :string {:doc "The name/title of this particular medium."}]
   :media:medium:format [:one :string {:doc "The format of the medium."}]})

#_(defentity :alias
  {:doc "Localised names are used to store the official names used in different languages and countries.
         Search hints are used to help both users and the server when searching and can be a number of things
         including:
         - Common misspellings
         - Misencodings
         - Variants
         - Stylized names
         - Titles ('the', 'Dr.', etc.)
         - Lead Performers (e.g 'Sting & The Police' = 'The Police')
         - Localizations (e.g. English-speakers are used to \"Tchaikovsky\", but that is not his native name)
         - Transliterations (There are often several ways to transliterate non-Roman characters)
         - \"Translated\" names (Many Asian artists have \"English\" names in addition to their given names)
         - Legal changes (Artists are often forced to change their names for legal reasons, sometimes only in
                          certain locations)
         Not including:
         - Numeric vs. spelled-out names
         - Acronyms
         - Initials
         - Performance names
         - Different imprints (Labels that change names, or different imprints by the same company)"}
  {:alias:name   [:one :string {:index? true}]
   :alias:locale [:one :ref    {:ref-to :linguistics:language
                                :doc    "Identifies which language or country the alias is for."}]})

#_(defentity :media:series
  {:doc "A series is a sequence of separate release groups, releases, recordings, works or
         events with a common theme. The theme is usually prominent in the branding of the
         entities in the series and the individual entities will often have been given a
         number indicating the position in the series."}
  {:this/name
     [:one :string  {:doc "The official name of the series."
                     :index? true}]
   :this/type
     [:one :keyword {:enum   #{[:release-group {:doc "A series of release groups."}]
                               [:release       {:doc "A series of releases."}]
                               [:recording     {:doc "A series of recordings."}]
                               [:work          {:doc "A series of works."}]
                               [:catalog       {:doc "A series of works which form a catalog of classical compositions.
                                                      Includes BWV (Bach), K (Mozart), etc."}]
                               [:event         {:doc "A series of events."}]
                               [:tour          {:doc "A series of related concerts by an artist in different locations."}]
                               [:festival      {:doc "A recurring festival, usually happening annually in the same location."}]
                               [:run           {:doc "A series of performances of the same show at the same venue."}]}
                     :doc    "Describes what type of entity the series contains."}]
   :this/ordering-type
     [:one :keyword {:validator #{:auto :manual}
                     :doc "Designates whether the series is ordered automatically or manually."}]
   :musicbrainz:id nil
   :this/name:alias:many
     [:many :ref    {:ref-to :alias}]})

#_(defentity :gathering:agent:person
  {:doc "A gathering or event, usually organized, which people can attend.
         Includes:
         - live performances
           - concerts
           - festivals
         - galas, proms, parties, etc.
         - fundraisers"}
  {:this/name
     [:one :string {:doc "The official name of the event if it has one, or a descriptive name (like
                          \"Main Artist at Place\") if not."}]
   :this/name:alias:many
     [:many :ref    {:ref-to :alias}]
   :this/type
     [:one :keyword {:enum   #{[:concert            {:doc "An individual concert by a single artist or collaboration, often
                                                           with supporting artists who perform before the main act."}]
                               [:festival           {:doc "An event where a number of different acts perform across the course
                                                           of the day. Larger festivals may be spread across multiple days."}]
                               [:launch-event       {:doc "A party, reception or other event held specifically for the launch
                                                           of a release."}]
                               [:convention-expo    {:doc "A convention, expo or trade fair is an event which is not typically
                                                           orientated around music performances, but can include them as side
                                                           activities."}]
                               [:masterclass-clinic {:doc "A masterclass or clinic is an event where an artist meets with a
                                                           small to medium-sized audience and instructs them individually
                                                           and/or takes questions intended to improve the audience members'
                                                           artistic skills."}]}
                     :doc    "Describes what type of gathering it is."}]
   :this/canceled?
     [:one :boolean]
   :musicbrainz:id nil
   :this/begin-end
     [:one :ref {:ref-to :time:range}]
   :this/setlist
     [:one :ref {:ref-to (coll-of (tuple :work :artist) :type :vector)
                 :doc    "A list of works performed."}]})


; A track is the way a recording is represented on a particular release, on a particular medium.
; Every track has a title (see the guidelines for titles) and is credited to one or more artists.
