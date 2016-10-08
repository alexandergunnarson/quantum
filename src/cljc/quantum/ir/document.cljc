(ns quantum.ir.document
  (:require
    [quantum.core.fn
      :refer        [#?@(:clj [f$n fn$ fn-> fn->> <- compr])]
      :refer-macros [          f$n fn$ fn-> fn->> <- compr]]
    [quantum.core.logic
      :refer        [#?@(:clj [if$n whenf$n whenf fn-not])]
      :refer-macros [          if$n whenf$n whenf fn-not]]
    [quantum.core.string      :as str]
    [quantum.core.collections :as coll
      :refer [containsv? in?]]
    [quantum.reducers.core    :as r]
    [quantum.core.validate    :as v
      :refer        [#?@(:clj [validate-all])]
      :refer-macros [          validate-all]]
    [quantum.net.http         :as http]))

(defonce
  ^{:doc "A basic set of stopwords. There are probably better ones out there."}
  stopwords
  (delay
    #{:a
      :about
      :above
      :across
      :afore
      :aforesaid
      :after
      :again
      :against
      :agin
      :ago
      :aint
      :albeit
      :all
      :almost
      :alone
      :along
      :alongside
      :already
      :also
      :although
      :always
      :am
      :american
      :amid
      :amidst
      :among
      :amongst
      :an
      :and
      :anent
      :another
      :any
      :anybody
      :anyone
      :anything
      :are
      :aren't
      :around
      :as
      :aslant
      :astride
      :at
      :athwart
      :away
      :b
      :back
      :bar
      :barring
      :be
      :because
      :been
      :before
      :behind
      :being
      :below
      :beneath
      :beside
      :besides
      :best
      :better
      :between
      :betwixt
      :beyond
      :both
      :but
      :by
      :c
      :can
      :cannot
      :can't
      :certain
      :circa
      :close
      :concerning
      :considering
      :cos
      :could
      :couldn't
      :couldst
      :d
      :dare
      :dared
      :daren't
      :dares
      :daring
      :despite
      :did
      :didn't
      :different
      :directly
      :do
      :does
      :doesn't
      :doing
      :done
      :don't
      :dost
      :doth
      :down
      :during
      :durst
      :e
      :each
      :early
      :either
      :em
      :english
      :enough
      :ere
      :even
      :ever
      :every
      :everybody
      :everyone
      :everything
      :except
      :excepting
      :f
      :failing
      :far
      :few
      :first
      :five
      :following
      :for
      :four
      :from
      :g
      :gonna
      :gotta
      :h
      :had
      :hadn't
      :hard
      :has
      :hasn't
      :hast
      :hath
      :have
      :haven't
      :having
      :he
      :he'd
      :he'll
      :her
      :here
      :here's
      :hers
      :herself
      :he's
      :high
      :him
      :himself
      :his
      :home
      :how
      :howbeit
      :however
      :how's
      :i
      :id
      :if
      :ill
      :i'm
      :immediately
      :important
      :in
      :inside
      :instantly
      :into
      :is
      :isn't
      :it
      :it'll
      :it's
      :its
      :itself
      :i've
      :j
      :just
      :k
      :l
      :large
      :last
      :later
      :least
      :left
      :less
      :lest
      :let's
      :like
      :likewise
      :little
      :living
      :long
      :m
      :many
      :may
      :mayn't
      :me
      :mid
      :midst
      :might
      :mightn't
      :mine
      :minus
      :more
      :most
      :much
      :must
      :mustn't
      :my
      :myself
      :n
      :near
      :'neath
      :need
      :needed
      :needing
      :needn't
      :needs
      :neither
      :never
      :nevertheless
      :new
      :next
      :nigh
      :nigher
      :nighest
      :nisi
      :no
      :no-one
      :nobody
      :none
      :nor
      :not
      :nothing
      :notwithstanding
      :now
      :o
      :o'er
      :of
      :off
      :often
      :on
      :once
      :one
      :oneself
      :only
      :onto
      :open
      :or
      :other
      :otherwise
      :ought
      :oughtn't
      :our
      :ours
      :ourselves
      :out
      :outside
      :over
      :own
      :p
      :past
      :pending
      :per
      :perhaps
      :plus
      :possible
      :present
      :probably
      :provided
      :providing
      :public
      :q
      :qua
      :quite
      :r
      :rather
      :re
      :real
      :really
      :respecting
      :right
      :round
      :s
      :same
      :sans
      :save
      :saving
      :second
      :several
      :shall
      :shalt
      :shan't
      :she
      :shed
      :shell
      :she's
      :short
      :should
      :shouldn't
      :since
      :six
      :small
      :so
      :some
      :somebody
      :someone
      :something
      :sometimes
      :soon
      :special
      :still
      :such
      :summat
      :supposing
      :sure
      :t
      :than
      :that
      :that'd
      :that'll
      :that's
      :the
      :thee
      :their
      :theirs
      :their's
      :them
      :themselves
      :then
      :there
      :there's
      :these
      :they
      :they'd
      :they'll
      :they're
      :they've
      :thine
      :this
      :tho
      :those
      :thou
      :though
      :three
      :thro'
      :through
      :throughout
      :thru
      :thyself
      :till
      :to
      :today
      :together
      :too
      :touching
      :toward
      :towards
      :true
      :'twas
      :'tween
      :'twere
      :'twill
      :'twixt
      :two
      :'twould
      :u
      :under
      :underneath
      :unless
      :unlike
      :until
      :unto
      :up
      :upon
      :us
      :used
      :usually
      :v
      :versus
      :very
      :via
      :vice
      :vis-a-vis
      :w
      :wanna
      :wanting
      :was
      :wasn't
      :way
      :we
      :we'd
      :well
      :were
      :weren't
      :wert
      :we've
      :what
      :whatever
      :what'll
      :what's
      :when
      :whencesoever
      :whenever
      :when's
      :whereas
      :where's
      :whether
      :which
      :whichever
      :whichsoever
      :while
      :whilst
      :who
      :who'd
      :whoever
      :whole
      :who'll
      :whom
      :whore
      :who's
      :whose
      :whoso
      :whosoever
      :will
      :with
      :within
      :without
      :wont
      :would
      :wouldn't
      :wouldst
      :x
      :y
      :ye
      :yet
      :you
      :you'd
      :you'll
      :your
      :you're
      :yours
      :yourself
      :yourselves
      :you've
      :z}))

(def dictionary
  ^{:doc "A semi-exhaustive English dictionary taken from /usr/share/dict/words."}
  (delay
    (http/request! {:url "https://raw.githubusercontent.com/dwyl/english-words/master/words3.txt"})))

(def undesirables-regex
  #"[^a-zA-Z\-\'']") ; don't include 0-9

(defn doc->normalized+
  "Lower case; preserves only letters, hyphens, and apostrophes."
  [doc-str & [post]]
  (validate-all
    doc-str string?
    post    (v/or* nil? fn?))
  (->> doc-str
       ; Normalize
       (<- str/replace undesirables-regex " ") ; How to do this distributively?
       (<- str/split #" ") ; How to do this distributively?
       (r/remove+   empty?)
       ((or post identity))
       (r/map+      (f$n str/->lower))
       ; Tokenize
       (r/map+      (whenf$n (f$n containsv? "'")
                      (f$n coll/remove-surrounding "'")))
       (r/remove+   empty?)
       (r/map+      (f$n str/remove #"\'"))))

(def normalized->tokenized-terms+
  (fn->> (r/map+    (if$n (f$n containsv? "-")
                      (fn->> (coll/remove-surrounding "-")
                             str
                             (<- str/split #"\-")
                             (<- whenf (partial every? (f$n in? @dictionary))
                               (fn [words]
                                 (let [concatted (apply str words)]
                                   (if (in? concatted @dictionary)
                                       concatted
                                       words)))))
                      vector))
         r/flatten-1+
         (r/remove+ empty?)))

(def doc->terms+
  (compr doc->normalized+ normalized->tokenized-terms+))

(defn doc->term-frequencies
  [doc-str & [post]]
  (->> (doc->terms+ doc-str post)
       (r/remove+   empty?)
       (r/frequencies {})
       (sort-by val >))) ; TODO use sort-by+



#_(defn tokenize-text+
  "Lower case,
   preserves only letters and numbers.
   No hyphens or apostrophes are preserved.
   Results in a set of words/tokens."
  [text]
  (->> text normalize-text+ tokenize-terms+))

#_(def text->terms
  (fn->> tokenize-text+
         (map+ keyword)
         (join [])))
