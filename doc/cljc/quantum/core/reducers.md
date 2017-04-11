# quantum.core.reducers

https://groups.google.com/forum/#!topic/clojure/VQj0E9TJWYY


When doing incrementing within a stateful `map-indexed` transducer and `fold` over (vec (range 0 100)):

VOLATILE
duplicates after a while and only gets to 84

UNSYNC
duplicates almost immediately and gets to 97

ATOMIC
no duplicates, and gets to 99

Alex Miller:
> While transducing processes may provide locking to cover the visibility of state updates in a stateful transducer, transducers should still use stateful constructs that ensure visibility (by using volatile, atoms, etc).
> [core.async] operations are covered by the channel lock which should guarantee visibility. Transducers used within a go block (via something like transduce or into) occur eagerly and don't incur any switch in threads so just fall back to the same old expectations of single-threaded use and visibility.
> Note that there are a couple of stateful transducers that use ArrayList (partition-by and partition-all). From my last conversation with Rich, he said those should really be changed to protect themselves better with volatile or something else. I thought I wrote up a ticket for this but looks like maybe I didn't, so I will take care of that.

;___________________________________________________________________________________________________________________________________
;=================================================={      MULTIREDUCIBLES     }=====================================================
;=================================================={  with support for /fold/ }=====================================================
; From wagjo, https://gist.github.com/wagjo/10017343 - Apr 7, 2014
(comment
  (def s1 "Hello World")
  (def s2 "lllllllllll")
  ;; create multireducible with zip
  (seq (zip s1 s2))
  ;; => ((\H \l) (\e \l) (\l \l) (\l \l) (\o \l) (\space \l) (\W \l) (\o \l) (\r \l) (\l \l) (\d \l))
   ;; /indexed/ is an alias for (zip (range) coll)
  (def indexed (partial zip (range))) ; my own code
  (seq (indexed s1))
  ;; => ((0 \H) (1 \e) (2 \l) (3 \l) (4 \o) (5 \space) (6 \W) (7 \o) (8 \r) (9 \l) (10 \d))

  ;; you can have more than 2 collections in the multireducible
  (reduce conj [] (indexed s1 s2))
  ;; => [(0 \H \l) (1 \e \l) (2 \l \l) (3 \l \l) (4 \o \l) (5 \space \l) (6 \W \l) (7 \o \l) (8 \r \l) (9 \l \l) (10 \d \l)]

  ;; reduce can work on tuples (as seen above), or can work on individual elements
  (reduce conj [] (unpacked (indexed s1 s2)))
  ;; => [0 \H \l 1 \e \l 2 \l \l 3 \l \l 4 \o \l 5 \space \l 6 \W \l 7 \o \l 8 \r \l 9 \l \l 10 \d \l]

  ;; lets find index where both strings have same character
  (let [s (unpacked (indexed s1 s2))
        f (fn [index ch1 ch2] (when (= ch1 ch2) index))]
    (into [] (keep f s)))
  ;; => [2 3 9]

  ;; some multireducibles also support folding, both with tuples and individual elements
  (let [s (unpacked (indexed s1 s2))
        combinef (fn ([] []) ([a b] [a b]))
        reducef (fn [val index ch1 ch2] (if (= ch1 ch2) (conj val index) val))]
    (fold+ 5 combinef reducef s))
  ;; => [[2 3] [[] [9]]]

  ;; maps can behave like multireducibles too
  (reduce conj [] (unpacked {:foo 1 :bar 2 :baz 3}))
  ;; => [:baz 3 :bar 2 :foo 1]

  ;; and did I mention multireducibles are very fast?
  (time (let [mr (indexed (range -1000000 1000000)
                          (range 1000000 -1000000 -1))
              f (fn [index v1 v2] (when (== v1 v2) index))]
          (into [] (keep f (unpacked mr))))) ;; => [1000000] "Elapsed time: 356.012467 msecs" ; USE KEEP+
   ;;* patch to clojure is needed to support this feature
  ; from https://gist.github.com/wagjo/b687158ab0ff6aa8cb33 -  May 2, 2014
  (defn fold-sectionable
    "Perform fold on sectionable collection."
    ([coll pool n combinef reducef reduce-mode]
       (fold-sectionable coll pool n combinef reducef
                         reduce-mode section)) ; SECTION???
    ([coll pool n combinef reducef reduce-mode section]
       (fold-sectionable coll pool n combinef reducef
                         reduce-mode section count))
    ([coll pool n combinef reducef reduce-mode section count]
       (let [cnt    (count coll)
             reduce (reduce-from-mode reduce-mode)
             n (max 1 n)]
         (cond
          (empty? coll) (combinef)
          (<= cnt n) (reduce reducef (combinef) coll)
          :else
          (let [split (quot cnt 2)
                v1 (section coll 0 split)
                v2 (section coll split cnt)
                fc (fn [child]
                     #(-fold child pool n combinef reducef
                             reduce-mode))]
            (invoke pool
                    #(let [f1 (fc v1)
                           t2 (fork (fc v2))]
                       (combinef (f1) (join t2)))))))))

  (deftype ZipIterator [^:unsynchronized-mutable iters]
    IOpenAware
    (-open? [this] (every? open? iters)) ; open?
    IReference
    (-deref [this] (apply tuple (map deref iters))) ; tuple
    IIterator
    (-next! [this] (set! iters (seq (map next! iters))) this)) ; next!

  (deftype FoldableZip [colls]
    IRed
    (-reduce [this f init]
      (loop [iters (seq (map iterator colls))
             val init]
        (if (every? open? iters)
          (reduced-let [ret (f val (apply tuple (map deref iters)))]
            (recur (seq (map next! iters)) ret))
          val)))
    IIterable
    (-iterator [this]
      (->ZipIterator (seq (map iterator colls))))
    IUnpackedRed
    (-reduce-unpacked [this f init]
      (loop [iters (seq (map iterator colls))
             val init]
        (if (every? open? iters)
          (reduced-let [ret (apply f val (map deref iters))]
            (recur (seq (map next! iters)) ret))
          val)))
    IFoldable
    (-fold [coll pool n combinef reducef reduce-mode]
      (fold-sectionable coll pool n combinef reducef reduce-mode))
    ISectionable
    (-section [this new-begin new-end]
      (let [l (count this)
            new-end (prepare-ordered-section new-begin new-end l)]
        (if (and (zero? new-begin) (== new-end l))
          this
          (->FoldableZip (map #(section % new-begin new-end) colls)))))
    ICounted
    (-count [this] (apply min (map count colls)))))
