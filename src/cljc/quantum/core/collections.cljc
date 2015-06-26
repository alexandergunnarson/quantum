(ns
  ^{:doc
      "Various collections functions.

       Includes better versions of the following than clojure.core:

       for, doseq, repeat, repeatedly, range, merge,
       count, vec, reduce, into, first, second, rest,
       last, butlast, get, pop, peek ...

       and more.

       Many of them are aliased from other namespaces like
       quantum.core.collections.core, or quantum.core.reducers."
    :attribution "Alex Gunnarson"}
  quantum.core.collections
  (:refer-clojure :exclude
    [for doseq reduce
     contains?
     repeat repeatedly
     range
     take take-while
     drop  drop-while
     subseq
     key val
     merge into
     count
     vec empty
     first second rest last butlast get pop peek
     assoc! conj!])
  (:require-quantum [ns logic type macros num map vec set log err macros fn str async time])
  (:require
            [quantum.core.collections.core :as coll]
            [quantum.core.reducers         :as red]
            [clojure.walk                  :as walk]
    #?(:clj [quantum.core.loops            :as loops])
    #?(:clj [clojure.pprint :refer [pprint]]))
  #?(:cljs
    (:require-macros
      [quantum.core.reducers   :as red]
      [quantum.core.cljs.loops :as loops])))

#?(:clj
    (defnt key
      "Like |key| but more robust."
      map-entry?
        (([obj] (core/key obj)) 
         ([k v] k))) ; For use with kv-reduce
   :cljs (defalias key core/key)) 

#?(:clj
    (defnt val
      "Like |val| but more robust."
      map-entry?
        (([obj] (core/val obj)) 
         ([k v] v))) ; For use with kv-reduce
   :cljs (defalias val core/val)) 

(defn genkeyword
  ([]    (keyword (gensym)))
  ([arg] (keyword (gensym arg))))

(defnt empty
  string?  ([obj] "")
  :default ([obj] (core/empty obj)))

(defn wrap-delay [f]
  (if (delay? f) f (delay ((or f fn-nil)))))

#?(:clj (defalias reduce   loops/reduce  ))
#?(:clj (defalias reduce-  loops/reduce- ))
#?(:clj (defalias reducei  loops/reducei ))
#?(:clj (defalias reducei- loops/reducei-))
(defalias vec         red/vec+       )    
(defalias into        red/into+      )    
(defalias redv        red/fold+      )
(defalias redm        red/reducem+   )
(defalias fold        red/fold+      ) ; only certain arities
(defalias foldv       red/foldp+     ) ; only certain arities
(defalias foldm       red/foldm+     ) 
(defalias map+        red/map+       )
(defalias filter+     red/filter+    )
(defalias lfilter     filter         )
(defalias remove+     red/remove+    )
(defalias lremove     remove         )
(defalias take+       red/take+      )
(defalias take-while+ red/take-while+)
(defalias drop+       red/drop+      )
(defalias group-by+   red/group-by+  )
(defalias flatten+    red/flatten+   )

(def flatten-1 (partial apply concat))

; ; ====== LOOPS ======
;(def cljs-for+ (var red/for+))
; (defalias for+   #?(:clj red/for+         :cljs red/for+))
; (alter-meta! (var for+) assoc :macro true)

; (def cljs-for (var loops/for)) ; doesn't work because not a var
; (def cljs-for (mfn loops/for)) ; doesn't work because no |eval|
#?(:clj (defalias for loops/for))
#?(:clj (alter-meta! (var for) assoc :macro true))

;(def cljs-lfor (var clojure.core/for))
;(defalias lfor   #?(:clj clojure.core/for :cljs cljs-lfor))
;#?(:clj (defalias lfor clojure.core/for))
#?(:clj (defmacro lfor   [& args] `(clojure.core/for ~@args)))

;(def cljs-doseq (var loops/doseq))
;(defalias doseq  #?(:clj loops/doseq      :cljs cljs-doseq))
#?(:clj (defmacro doseq  [& args] `(loops/doseq      ~@args)))

;(def cljs-doseqi (var loops/doseqi))
;(defalias doseqi #?(:clj loops/doseqi     :cljs cljs-doseqi))
#?(:clj (defmacro doseqi [& args] `(loops/doseqi     ~@args)))

#?(:cljs
  (defn kv+
    "For some reason ClojureScript reducers have an issue and it's terrible... so use it like so:
     (map+ (compr kv+ <myfunc>) _)
     |reduce| doesn't have this problem."
    {:todo ["Eliminate the need for this."]}
    ([obj] obj)
    ([k v] k)))

; ; ====== COLLECTIONS ======

; ; TODO Don't redefine these vars
; #?(:cljs (defn map+    [f coll] (red/map+    (compr kv+ f) coll)))
; #?(:cljs (defn filter+ [f coll] (red/filter+ (compr kv+ f) coll)))
; #?(:cljs (defn remove+ [f coll] (red/remove+ (compr kv+ f) coll)))


(def      lasti         coll/lasti        )
(def      index-of      coll/index-of     )
(def      last-index-of coll/last-index-of)
(def      count         coll/count        )
(def      getr          coll/getr         )
(def      subseq        getr              )
(def      lsubseq       core/subseq       )
(def      get           coll/get          )
(def      gets          coll/gets         )
(def      getf          coll/getf         )

; If not |defalias|ed, "ArityException Wrong number of args (2) passed to: core/eval36441/fn--36457/G--36432--36466"
(defalias conjl         coll/conjl        )
(defalias conjr         coll/conjr        )
(def      pop           coll/pop          )
(def      popr          coll/popr         )
(def      popl          coll/popl         )
(def      peek          coll/peek         )
(def      first         coll/first        )
(def      second        coll/second       )
(def      third         coll/third        )
(def      rest          coll/rest         )
(defalias lrest         core/rest         )
(def      butlast       coll/butlast      )
(def      last          coll/last         )
(def      assoc!        coll/assoc!       )
(def      conj!         coll/conj!        )
(def      update!       coll/update!      )

(defalias merge map/merge+)

(defn merge-meta [sym-0 sym-f]
  (with-meta sym-0 (meta sym-f)))

(def frest (fn-> rest first))

; ===== REPEATEDLY =====

#?(:clj
(defmacro repeatedly-into
  {:todo ["Best to just inline this. Makes no sense to have a macro."]}
  [coll n & body]
  `(let [coll# ~coll
         n#    ~n]
     (if (should-transientize? coll#)
         (loop [v# (transient coll#) idx# 0]
           (if (>= idx# n#)
               (persistent! v#)
               (recur (conj! v# ~@body)
                      (inc idx#))))
         (loop [v#   coll# idx# 0]
           (if (>= idx# n#)
               v#
               (recur (conj v# ~@body)
                      (inc idx#))))))))

(def lrepeatedly clojure.core/repeatedly)

#?(:clj
(defmacro repeatedly
  "Like |clojure.core/.repeatedly| but (significantly) faster and returns a vector."
  ; ([n f]
  ;   `(repeatedly-into* [] ~n ~arg1 ~@body))
  {:todo ["Makes no sense to have a macro. Just inline"]}
  ([n arg1 & body]
    `(repeatedly-into* [] ~n ~arg1 ~@body))))

; ===== RANGE =====

(#?(:clj defalias :cljs def) range+ red/range+)

(defn lrrange
  "Lazy reverse range."
  {:usage '(lrrange 0 5)
   :out   '(5 4 3 2 1)}
  ([]    (iterate dec 0))
  ([a]   (iterate dec a))
  ([a b]
    (->> (iterate unchecked-dec b) (core/take (- b (dec a))))))

(defn lrange
  ([]  (core/range))
  ([a] (core/range a))
  ([a b]
    (if (neg? (- a b))
        (lrrange a b)
        (core/range a b))))

(defn ^Vec rrange
  "Reverse range"
  {:todo ["Performance with |range+| on [a b] arity, and rseq"]}
  ([]    (lrrange))
  ([a]   (lrrange a))
  ([a b] (-> (range+ a b) redv reverse)))

(defn ^Vec range
  {:todo ["Performance with |range+| on [a b] arity"]}
  ([]    (lrange))
  ([a]   (lrange a))
  ([a b]
    (if (neg? (- a b))
        (rrange a b))
        (-> (range+ a b) redv)))

; ===== REPEAT =====

(defn repeat
  ([obj]   (core/repeat obj))
  ([n obj] (loops/for [i (range n)] obj)))

; ===== ....

(defn ^Set abs-difference 
  "Returns the absolute difference between a and b.
   That is, (a diff b) union (b diff a)."
  {:todo ["Probably a better name for this."]}
  [a b]
  (set/union
    (set/difference a b)
    (set/difference b a)))

; ; a better merge?
; ; a better merge-with?
#?(:clj
(defn get-map-constructor
  "Gets a record's map-constructor function via its class name."
  [rec]
  (let [^String class-name-0
          (if (class? rec)
              (-> rec str)
              (-> rec class str))
        ^String class-name
          (subseq class-name-0
            (-> class-name-0 (last-index-of ".") inc)
            (-> class-name-0 count))
        ^Fn map-constructor-fn
          (->> class-name (str "map->") symbol eval)]
    map-constructor-fn)))

; ================================================ REDUCE ================================================

(defn reduce-2
  "Like |reduce|, but reduces over two items in a collection at a time.

   Its function @func must take three arguments:
   1) The accumulated return value of the reduction function
   2) The                next item in the collection being reduced over
   3) The item after the next item in the collection being reduced over"
  {:attribution "Alex Gunnarson"}
  [func init coll] ; not actually implementing of CollReduce... so not as fast...
  (loop [ret init coll-n coll]
    (if (empty? coll-n)
        ret
        (recur (func ret (first coll-n) (second coll-n))
               (-> coll-n rest rest)))))

; ================================================ FILTER ================================================

(defn ffilter
  "Returns the first result of a |filter| operation.
   Uses lazy |filter| so as to do it in the fastest possible way."
   [^Fn filter-fn coll]
   (->> coll (filter filter-fn) first))

(defn+ ffilter+
  {:todo ["Use a delayed reduction as the base!"]}
  [^Fn pred coll]
  (loops/reduce
    (fn [ret elem-n]
      (when (pred elem-n)
        (reduced elem-n)))
    nil
    coll))

(defn+ ^MapEntry ffilteri
  {:todo ["Use a delayed reduction as the base!" "Allow parallelization"]
   :in   '[(fn-eq? "4") ["a" "d" "t" "4" "10"]]
   :out  [3 "4"]}
  [^Fn pred coll]
  (loops/reducei
    (fn [ret elem-n index-n]
      (if (pred elem-n)
          (reduced (map-entry index-n elem-n))
          ret))
    (map-entry nil nil)
    coll))

(defn filteri
  {:todo ["Use reducers"]}
  [pred coll]
  (if (should-transientize? coll)
      (persistent!
        (loops/reducei
          (fn [ret elem-n n]
            (if (pred elem-n)
                (conj! ret (map-entry n elem-n))
                ret))
          (transient [])
          coll))
      (loops/reducei
        (fn [ret elem-n n]
          (if (pred elem-n)
              (conj ret (map-entry n elem-n))
              ret))
        []
        coll)))

(defnt ^MapEntry last-filteri*
  {:todo ["Use a delayed reduction as the base!" "Allow parallelization"]
   :in   '[["a" "d" "t" "4" "4" "10"] (fn-eq? "4")]
   :out  [4 "4"]}
  indexed?
    ([coll pred]
      (->> coll rseq (ffilteri pred)
           (<- update 0 (partial - (lasti coll)))))
  :default
    ([coll pred]
      (loops/reducei
        (fn [ret elem-n index-n]
          (if (pred elem-n)
              (map-entry index-n elem-n)
              ret))
        (map-entry nil nil)
        coll)))

#?(:clj  (definline last-filteri [pred coll] `(last-filteri* ~coll ~pred))
   :cljs (defn      last-filteri [pred coll]  (last-filteri*  coll  pred)))

; ================================================ INDEX-OF ================================================
(defn seq-contains?
  "Like |contains?|, but tests if a collection @super contains
   the constituent elements of @sub, in that order.

   A prime example would be substrings within strings."
  [super sub]
  (nnil? (index-of super sub)))

(defn indices-of-elem
  {:todo ["Make parallizeable"]}
  [coll elem-0]
  (if (should-transientize? coll)
      (persistent!
        (loops/reducei
          (fn [ret elem-n n]
            (if (= elem-0 elem-n)
                (conj! ret n)
                ret))
          (transient [])
          coll))
      (loops/reducei
        (fn [ret elem-n n]
          (if (= elem-0 elem-n)
              (conj ret n)
              ret))
        []
        coll)))

(declare drop)

(defn indices-of
  {:todo ["Make parallizeable" "Maybe make lazy?"]}
  [coll elem-0]
  (loop [coll-n coll indices []]
    (let [i (index-of coll-n elem-0)]
      (if-not i
        indices
        (recur
          (drop (+ i (count elem-0)) coll-n)
          (conj indices
            (+ i (if-let [li (last indices)]
                   (+ li (count elem-0))
                   0))))))))

(defn index-of-pred      [coll pred]
  (->> coll (ffilteri pred) key))

(defn last-index-of-pred [coll pred]
  (->> coll (last-filteri pred) key))
; ================================================ TAKE ================================================
; ============ TAKE-LEFT ============
(defn takel
  {:in  [2 "abcdefg"]
   :out "ab"}
  [i super]
  (subseq super 0 (-> i long dec)))

(def take takel)
(def ltake core/take)

(defn takel-fromi
  "Take starting at and including index n."
  {:in  [2 "abcdefg"]
   :out "cdefg"}
  [i super]
  (subseq super i (lasti super)))

(def take-fromi takel-fromi)

(defn takel-from
  {:in  ["cd" "abcdefg"]
   :out "cdefg"}
  [sub super]
  (let [i (or (index-of super sub)
              (throw+ (Err. :out-of-bounds nil [super sub])))]
    (takel-fromi i super)))

(def take-from takel-from)

(defn takel-afteri
  {:in  [2 "abcdefg"]
   :out "defg"}
  [i super]
  (subseq super (-> i long inc) (lasti super)))

(def take-afteri takel-afteri)

(defn takel-after
  {:in  ["cd" "abcdefg"]
   :out "efg"}
  [sub super]
  (let [i (or (index-of super sub)
              (throw+ (Err. :out-of-bounds nil [super sub])))]
    (takel-afteri i super)))

(def take-after takel-after)

(defn takel-while
  {:in '[(f*n in? str/alphanum-chars) "abcdef123&^$sd"]
   :out "abcdef123"}
  [pred super]
  (subseq super 0 (index-of-pred super pred)))

(def take-while   takel-while    )
(def ltakel-while core/take-while)
(def ltake-while  ltakel-while   )

(def takel-untili takel)
(def take-untili  take-untili)

(defnt takel-until
  fn?      ([pred super] (takel-while super (fn-not pred)))
  :default ([sub  super]
    (if-let [i (index-of super sub)]
      (takel-untili i super)
      super)))

(def take-until takel-until)

(defnt takel-until-inc
  :default ([sub super]
    (if-let [i (index-of super sub)]
      (takel-untili (+ i (count sub)) super)
      super)))

(def take-until-inc takel-until-inc)

; ============ TAKE-RIGHT ============
(defn takeri
  "Take up to and including index, starting at the right."
  {:in  [2 "abcdefg"]
   :out "cdefg"}
  [i super]
  (subseq super i (lasti super)))

(defn taker-untili
  "Take until index, starting at the right."
  {:in  [2 "abcdefg"]
   :out "defg"}
  [i super]
  (subseq super (-> i long inc) (lasti super)))

(defn taker-until
  "Take until index of, starting at the right."
  {:in  ["c" "abcdefg"]
   :out "defg"}
  ([sub super]
    (taker-until sub super super))
  ([sub alt super]
    (let [i (last-index-of super sub)]
      (if i
          (taker-untili i super)
          alt))))

(defn taker-after
  {:in ["." "abcdefg.asd"]
   :out "abcdefg"}
  [sub super]
  (if-let [i (last-index-of super sub)]
    (subseq super 0 (-> i long dec))
    nil))

; ================================================ DROP ================================================

(defn dropl [n coll] (subseq coll n (lasti coll)))

(def drop dropl)

(def ldropl drop)

(defn dropr
  {:in  [3 "abcdefg"]
   :out "abcd"}
  [n coll]
  (subseq coll 0 (-> coll lasti long (- (long n)))))

(defn ldropr
  {:attribution "Alex Gunnarson"}
  [n coll]
  (lsubseq coll 0 (-> coll lasti long (- (long n)))))

(defn dropr-while [pred super]
  (subseq super 0
    (dec (or (last-index-of-pred super pred)
             (count super)))))

(defnt dropr-until
  "Until right index of."
  {:todo "Combine code with /takeri/"
   :in  ["cd" "abcdefg"]
   :out "abcd"}
  fn? ([pred super] (dropr-while super (fn-not pred)))
  :default
    ([sub super]
      (if-let [i (last-index-of super sub)]
        (subseq super 0 (+ i (lasti sub)))
        super)))

(defn dropr-after
  "Until right index of."
  {:todo "Combine code with /takeri/"
   :in  ["cd" "abcdefg"]
   :out "ab"}
  ([sub super]
    (if (index-of super sub)
        (subseq super 0 (index-of super sub))
        super)))

; (let [index-r
;           (whenc (last-index-of super sub) (fn= -1)
;             (throw (str "Index of" (str/squote sub) "not found.")))])
;   (subseq super
;     (inc (last-index-of super sub))
;     (-> super lasti))

; ================================================ MERGE ================================================

(defn index-with [coll f]
  (->> coll
       (map+ #(map-entry (f %) %))
       redm))

(defn merge-keep-left  [a b] (merge b a))
(defn merge-keep-right [a b] (merge a b))
              
(defn split-remove
  {:todo ["Slightly inefficient — two /index-of/ implicit."]}
  [split-at-obj coll]
  [(take-until split-at-obj coll)
   (take-after split-at-obj coll)])

#?(:clj
(defmacro kmap [& ks]
 `(zipmap (map keyword (quote ~ks)) (list ~@ks))))

(defn select
  "Applies a list of functions, @fns, separately to an object, @coll.
   A good use case is returning values from an associative structure with keys as @fns.
   Returns a vector of the results."
  ^{:attribution "Alex Gunnarson"
    :usage "(select {:a 1 :b [3]} :a (compr :b 0)) => [1 3]"}
  [coll & fns]
  ((apply juxt fns) coll))

(defn comparator-extreme-of
  "For compare-fns that don't have enough arity to do, say,
   |(apply time/latest [date1 date2 date3])|.

   Gets the most \"extreme\" element in collection @coll,
   \"extreme\" being defined on the @compare-fn.

   In the case of |time/latest|, it would return the latest
   DateTime in a collection.

   In the case of |>| (greater than), it would return the
   greatest element in the collection:

   (comparator-extreme-of [1 2 3] (fn [a b] (if (> a b) a b)) )
   :: 3

   |(fn [a b] (if (> a b) a b))| is the same thing as
   |(choice-comparator >)|."
  {:todo ["Rename this function."
          "HOW DOES THIS HAVE ANY RELEVANCE?"
          "Possibly belongs in a different namespace"]}
  [coll ^Fn compare-fn]
  (loops/reducei
    (fn [ret elem n]
      (if (= n 0)
          elem
          (compare-fn ret elem)))
    nil
    coll))

(defn coll-if [obj]
  (whenf obj (fn-not coll?) vector))

(defn seq-if [obj]
  (condf obj
    (fn-or seq? nil?) identity
    coll?             seq
    :else             list))
;___________________________________________________________________________________________________________________________________
;=================================================={         LAZY SEQS        }=====================================================
;=================================================={                          }=====================================================
#?(:clj (defalias lseq lazy-seq))

#?(:clj
  (def lseq+
    (condf*n
      (fn-or seq? nil? coll?) #(lseq %) ; not |partial|, because can't take value of a macro
      :else (fn-> list lseq first))))

(defn unchunk
  "Takes a seqable and returns a lazy sequence that
   is maximally lazy and doesn't realize elements due to either
   chunking or apply.

   Useful when you don't want chunking, for instance,
   (first awesome-website? (map slurp <a-bunch-of-urls>))
   may slurp up to 31 unneed webpages, whereas
   (first awesome-website? (map slurp (unchunk <a-bunch-of-urls>)))
   is guaranteed to stop slurping after the first awesome website.

  Taken from http://stackoverflow.com/questions/3407876/how-do-i-avoid-clojures-chunking-behavior-for-lazy-seqs-that-i-want-to-short-ci"
  {:attribution "prismatic.plumbing"}
  [s]
  (when (seq s)
    (cons (first s)
          (lazy-seq (s rest unchunk)))))
;___________________________________________________________________________________________________________________________________
;=================================================={  POSITION IN COLLECTION  }=====================================================
;=================================================={ first, rest, nth, get ...}=====================================================
; (defn- nth-red
;   "|nth| implemented in terms of |reduce|."
;   {:deprecated  true
;    :attribution "Alex Gunnarson"
;    :performance "Twice as slow as |nth|"}
;   [coll n]
;   (let [nn (volatile! 0)]
;     (->> coll
;          (reduce
;            (fn
;              ([ret elem]
;               (if (= n @nn)
;                   (reduced elem)
;                   (do (vswap! nn inc) ret)))
;              ([ret k v]
;                (if (= n @nn)
;                    (reduced [k v])
;                    (do (vswap! nn inc) ret))))
;            []))))


(defn fkey+ [m]
  (-> m first key))
(defn fval+ [m]
  (-> m first val))

(defn up-val
  {:in '[{:a "ABC" :b 123} :a]
   :out '{"ABC" {:b 123}}
   :todo ["hash-map creation inefficient ATM"]}
  [^Map m k]
  (hash-map
    (get m k)
    (-> m (dissoc k))))

#?(:cljs
  (defn rename-keys [m-0 rename-m]
    (loops/reduce
      (fn [ret k-0 k-f]
        (-> ret
            (assoc  k-f (get ret k-0))
            (dissoc k-0)))
      m-0
      rename-m)))

; ; for /subseq/, the coll must be a sorted collection (e.g., not a [], but rather a sorted-map or sorted-set)
; ; test(s) one of <, <=, > or >=

; ; /nthrest/
; ; (nthrest (range 10) 4) => (4 5 6 7 8 9)

; ; TODO: get-in from clojure, make it better
(defn get-in+ [coll [iden :as keys-0]] ; implement recursively
  (if (= iden identity)
      coll
      (get-in coll keys-0)))
(defn reverse+ [coll] ; what about arrays? some transient loop or something
  (ifn coll reversible? rseq reverse))
(def single?
  "Does coll have only one element?"
  (fn-and seq (fn-not next)))
;___________________________________________________________________________________________________________________________________
;=================================================={   ADDITIVE OPERATIONS    }=====================================================
;=================================================={    conj, cons, assoc     }=====================================================

;___________________________________________________________________________________________________________________________________
;=================================================={           MERGE          }=====================================================
;=================================================={      zipmap, zipvec      }=====================================================
; A better zipvec...
;(defn zipvec+ [& colls-0] ; (map vector [] [] [] []) ; 1.487238 ms for zipvec+ vs. 1.628670 ms for doall + map-vector.
;   (let [colls (->> colls-0 (map+ fold+) fold+)]
;     (for+ [n (range 0 (count (get colls 0)))] ; should be easy, because count will be O(1) with folded colls
;       (->> colls
;            (map (f*n get+ n)))))) ; get+ doesn't take long at all; also, apparently can't use map+ within for+...
;                                   ; 234.462665 ms if you realize them
; (defn zipfor- [& colls-0] ;  [[1 2 3] [4 5 6] [7 8 9]]
;   (let [colls (->> colls-0 (map+ fold+) fold+) ; nested /for/s, no
;         rng   (range 0 (-> colls count dec))]
;     (for   [n  rng] ; [[1 2 3] [4 5 6] [7 8 9]]
;       (for [cn rng] ; ((1 4 7) (4 5 6) ...)
;         (-> colls (get cn) (get n))))))
;; (zipvec-- [[1 2 3] [4 5 6] [7 8 9]])
;(defn zipvec-- [& colls-0] ; nested /map/s, no
;  (let [colls (vec+ colls-0)]
;    (map+
;      (fn [n]
;        (map+ (getf+ n) colls))
;      (range 0 (inc 2)))))

(declare contains?)

(defn merge-with+
  "Like merge-with, but the merging function takes the key being merged
   as the first argument"
   {:attribution  "prismatic.plumbing"
    :todo ["Make it not output HashMaps but preserve records"]
    :contributors ["Alex Gunnarson"]}
  [f & maps]
  (when (any? identity maps)
    (let [merge-entry
           (fn [m e]
             (let [k (key e) v (val e)]
               (if (contains? m k)
                 (assoc m k (f k (get m k) v))
                 (assoc m k v))))
          merge2
            (fn ([] {})
                ([m1 m2]
                 (loops/reduce merge-entry (or m1 {}) (seq m2))))]
      (loops/reduce merge2 maps))))

(defn ^Map merge-vals-left
  "Merges into the left map all elements of the right map whose
   keys are found in the left map.

   Combines using @f, a |merge-with| function."
  {:todo "Make a reducer, not just implement using |reduce| function."
   :in ['{:a {:aa 1}
          :b {:aa 3}}
         {:a {:aa 5}
          :c {:bb 4}}
         (fn [k v1 v2] (+ v1 v2))]
   :out '{:a {:aa 6}
          :b {:aa 3}}}
  [^Map left ^Map right ^Fn f]
  (persistent!
    (loops/reduce
      (fn [left-f ^Key k-right ^Map v-right]
       ;(if ((fn-not contains?) left-f k-right) ; can't call |contains?| on a transient, apparently...
       ;    left-f)
       (let [^Map v-left
               (get left k-right)]
         (if (nil? v-left)
             left-f
             (let [^Map merged-vs
                   (merge-with+ f v-left v-right)]
               (assoc! left-f k-right merged-vs)))))
      (transient left)
      right)))
;___________________________________________________________________________________________________________________________________
;=================================================={      CONCATENATION       }=====================================================
;=================================================={ cat, fold, (map|con)cat  }=====================================================
(defn- concat++
  {:todo ["Needs optimization"]}
  ([coll]
    (try (loops/reduce catvec coll)
      (catch Exception e (loops/reduce (zeroid into []) coll))))
  ([coll & colls]
    (try (apply catvec coll colls)
      (catch Exception e (into [] coll colls)))))
;  Use original vectors until they are split. Subvec-orig below a certain range? Before the inflection point of log-n
;___________________________________________________________________________________________________________________________________
;=================================================={  FINDING IN COLLECTION   }=====================================================
;=================================================={  in?, index-of, find ... }=====================================================
(defnt contains?
  string?  ([coll elem]
             #?(:clj  (.contains coll ^String elem)
                :cljs (index-of coll elem)))
  pattern? ([coll elem]
             (nnil? (str/re-find+ elem coll)))
  map?     ([coll k] (clojure.core/contains? coll k))
  :default ([coll elem]
             (any? (fn-eq? elem) coll)))

(defn in?
  "The inverse of |contains?|"
  {:todo ["|definline| this?"]}
  [elem coll] (contains? coll elem))

; ;-----------------------{       SELECT-KEYS       }-----------------------
(defn- ^Map select-keys-large
  "A transient and reducing version of clojure.core's |select-keys|."
  {:performance
    "45.3 ms vs. core's 60.29 ms on:
     (dotimes [_ 100000]
       (select-keys
         {:a 1 :b 2 :c 3 :d 4 :e 5 :f 6 :g 7}
         [:b :c :e]))).
     Performs much better on large set of keys."} 
  [keyseq m]
    (-> (transient {})
        (loops/reduce
          (fn [ret k]
            (let [entry (. clojure.lang.RT (find m k))]
              (if entry
                  (conj! ret entry)
                  ret)))
          (seq keyseq))
        persistent!
        (with-meta (meta m))))

(defn- ^Map select-keys-small
  "A transient version of clojure.core's |select-keys|.

   Note: using a reducer here incurs the overhead of creating a
   function on the fly (can't extern it because of a closure).
   This is better for small set of keys."
  {:performance
    "39.09 ms vs. core's 60.29 ms on:
     (dotimes [_ 100000]
       (select-keys
         {:a 1 :b 2 :c 3 :d 4 :e 5 :f 6 :g 7}
         [:b :c :e])))"} 
  [keyseq m]
    (loop [ret (transient {}) keys (seq keyseq)]
      (if keys
        (let [entry (. clojure.lang.RT (find m (first keys)))]
          (recur
           (if entry
             (conj! ret entry)
             ret)
           (next keys)))
        (with-meta (persistent! ret) (meta m)))))

(defn- ^Delay select-keys-delay
  "Not as fast as select-keys with transients."
  {:todo ["FIX THIS"]}
  [ks m]
  (let [ks-set (into #{} ks)]
    (->> m
         (filter+
           (compr key (f*n in? ks-set))))))

(defn ^Map select-keys+
  {:todo
    ["Determine actual inflection point at which select-keys-large
      should be used over select-keys-small."]}
  [m ks]
  (if (-> ks count (> 10))
      (select-keys-small m ks)
      (select-keys-large m ks)))

; ;-----------------------{       CONTAINMENT       }-----------------------

; ; index-of-from [o val index-from] - index-of, starting at index-from
; (defn contains-or? [coll elems]
;   (apply-or (map (partial contains? coll) elems)))
(defn get-keys
  {:attribution "Alex Gunnarson"}
  [m obj]
  (persistent!
    (loops/reduce
      (fn [ret k v]
        (if (identical? obj v)
            (conj! ret k)
            ret))
      (transient [])
      m)))
(defn get-key [m obj] (-> m (get-keys obj) first))
; ; /find/ Returns the map entry for key, or nil if key not present
; ; (find {:b 2 :a 1 :c 3} :a) => [:a 1]
; ; (select-keys {:a 1 :b 2} [:a :c]) =>  {:a 1}
; ; /frequencies/
; ; (frequencies ['a 'b 'a 'a])
; ; {a 3, b 1}
; ; /partition-by/
; ; splits the coll each time f returns a new value
; ; (partition-by odd? [1 1 1 2 2 3 3])
; ; => ((1 1 1) (2 2) (3 3)) /lseq/
;___________________________________________________________________________________________________________________________________
;=================================================={  FILTER + REMOVE + KEEP  }=====================================================
;=================================================={                          }=====================================================
(defn filter-keys+ [pred coll] (->> coll (filter+ (compr key pred))))
(defn remove-keys+ [pred coll] (->> coll (remove+ (compr key pred))))
(defn filter-vals+ [pred coll] (->> coll (filter+ (compr val pred))))
(defn remove-vals+ [pred coll] (->> coll (remove+ (compr key pred))))

(defn vals+
  {:attribution "Alex Gunnarson"
   :todo ["Compare performance with core functions"]}
  [m]
  (->> m (map+ val) redv))
(defn keys+
  {:attribution "Alex Gunnarson"
   :todo ["Compare performance with core functions"]}
  [m]
  (->> m (map+ key) redv))
;___________________________________________________________________________________________________________________________________
;=================================================={     PARTITION, GROUP     }=====================================================
;=================================================={       incl. slice        }=====================================================
; slice-from [o start] - like slice, but until the end of o
; slice-to [o end] - like slice, but from the beginning of o
(defn slice
  "Divide coll into n approximately equal slices.
   Like partition."
  {:attribution "flatland.useful.seq"
   :todo ["Optimize" "Use transients"]}
  [n-0 coll]
  (loop [n-n n-0 slices [] items (vec coll)]
    (if (empty? items)
      slices
      (let [size (num/ceil (/ (count items) n-n))]
        (recur (dec n-n)
               (conj slices (subvec+ items 0 size))
               (subvec+ items size (lasti items)))))))
; /partition/
; (partition 4 (range 20))
; => ((0 1 2 3) (4 5 6 7) (8 9 10 11) (12 13 14 15) (16 17 18 19))
; (partition 4 6 ["a" "b" "c" "d"] (range 20))
; => ((0 1 2 3) (6 7 8 9) (12 13 14 15) (18 19 "a" "b"))
; /partition-all/
; Returns a lazy sequence of lists like partition, but may include
; partitions with fewer than n items at the end.
; (partition-all 4 [0 1 2 3 4 5 6 7 8 9])
; => ((0 1 2 3) (4 5 6 7) (8 9))
;___________________________________________________________________________________________________________________________________
;=================================================={  DIFFERENTIAL OPERATIONS }=====================================================
;=================================================={     take, drop, split    }=====================================================
; /take-nth/
; (take-nth 2 (range 10))
; => (0 2 4 6 8)
; /cycle/
; (take 5 (cycle ["a" "b"]))
; => ("a" "b" "a" "b" "a")
; /take-last/ ; turn this into a subvec
; (take-last 2 [1 2 3 4]) => (3 4)
; /last/    is a limiting case (1) of take-last
; /drop-last/ ; (drop-last 2 [1 2 3 4]) => (1 2)
; /butlast/ is a limiting case (1) of drop-last

; splice [o index n val] - fast remove and insert in one go
; splice-arr [o index n val-arr] - fast remove and insert in one go
; insert-before [o index val] - insert one item inside coll
; insert-before-arr [o index val] - insert array of items inside coll
; remove-at [o index] - remove one item from index pos
; remove-n [o index n] - remove n items starting at index pos
; triml [o n] - trims n items from left
; trimr [o n] - trims n items from right
; trim [o nl nr] - trims nl items from left and nr items from right
; rip [o index] - rips coll and returns [pre-coll item-at suf-coll]
; sew [pre-coll item-arr suf-coll] - opposite of rip, but with arr
(defn split [ind coll-0]
  (if (vector? coll-0)
      [(subvec+ coll-0 0   ind)
       (subvec+ coll-0 ind (count coll-0))]
      (split-at coll-0 ind)))
(defn split-with-v+ [pred coll-0] ; IMPROVE
  (->> coll-0
       (split-with pred)
       (map+ vec)))
;_._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._
;=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*{        ASSOCIATIVE       }=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=
;=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*{                          }=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=
;___________________________________________________________________________________________________________________________________
;=================================================={          ASSOC           }=====================================================
;=================================================={ update(-in), assoc(-in)  }=====================================================
(defn- extend-coll-to
  "Extends an associative structure (for now, only vector) to a given index."
  {:attribution "Alex Gunnarson"
   :usage "USAGE: (extend-coll-to [1 2 3] 5) => [1 2 3 nil nil]"}
  [coll-0 k]
  (if (and (vector? coll-0)
           (number? k)
           (-> coll-0 count dec (< k)))
      (let [trans?   (transient? coll-0)
            trans-fn (if trans? identity transient)
            pers-fn  (if trans? identity persistent!)]
        (pers-fn
          (loops/reduce
            (fn [coll-n _] (conj! coll-n nil)) ; extend-vec part
            (trans-fn coll-0)
            (range (count coll-0) (inc k)))))
      coll-0))
(defn assoc+
  {:todo ["Protocolize on IEditableCollection"
          "Probably has performance issues"]}
  ([coll-0 k v]
    (assoc (extend-coll-to coll-0 k) k v))
    ; once probably gives no performance benefit from transience
  ([coll-0 k v & kvs-0]
    (let [edit?    (editable? coll-0)
          trans-fn (if edit? transient   identity)
          pers-fn  (if edit? persistent! identity)
          assoc-fn (if edit? assoc!      assoc)]
      (loop [kvs-n  kvs-0
             coll-f (-> coll-0 trans-fn
                        (extend-coll-to k)
                        (assoc-fn k v))]
        (if (empty? kvs-n)
            (pers-fn coll-f)
            (recur (-> kvs-n rest rest)
                   (let [k-n (first kvs-n)]
                     (-> coll-f (extend-coll-to k-n)
                         (assoc-fn k-n (second kvs-n))))))))))
(defn update+
  "Updates the value in an associative data structure @coll associated with key @k
   by applying the function @f to the existing value."
  ^{:attribution "weavejester.medley"
    :contributors ["Alex Gunnarson"]}
  ([coll k f]      (assoc+ coll k       (f (get coll k))))
  ([coll k f args] (assoc+ coll k (apply f (get coll k) args))))

(defn updates+
  "For each key-function pair in @kfs,
   updates value in an associative data structure @coll associated with key
   by applying the function @f to the existing value."
  ^{:attribution "Alex Gunnarson"
    :todo ["Probably updates and update are redundant"]}
  ([coll & kfs]
    (reduce-2 ; This is inefficient
      (fn [ret k f] (update+ ret k f))
      coll
      kfs)))

(defn update-key+
  {:attribution "Alex Gunnarson"
   :usage '(->> {:a 4 :b 12}
                (map+ (update-key+ str)))}
  ([f]
    (fn
      ([kv]
        (assoc+ kv 0 (f (get kv 0))))
      ([k v]
        (map-entry (f k) v)))))

(defn update-val+
  {:attribution "Alex Gunnarson"
   :usage '(->> {:a 4 :b 12}
                (map+ (update-val+ (f*n / 2))))}
  ([f]
    (fn
      ([kv]
        (assoc+ kv 1 (f (get kv 1))))
      ([k v]
        (map-entry k (f v))))))

(defn mapmux
  ([kv]  kv)
  ([k v] (map-entry k v)))
(defn record->map [rec]
  (into {} rec))

;--------------------------------------------------{        UPDATE-IN         }-----------------------------------------------------
(defn update-in!
  "'Updates' a value in a nested associative structure, where ks is a sequence of keys and
  f is a function that will take the old value and any supplied args and return the new
  value, and returns a new nested structure. The associative structure can have transients
  in it, but if any levels do not exist, non-transient hash-maps will be created."
  {:attribution "flatland.useful"}
  [m [k & ks] f & args]
  (let [assoc-fn (if (transient? m) assoc! assoc)
        val (get m k)]
    (assoc-fn m k
      (if ks
          (apply update-in! val ks f args)
          (apply f val args)))))
; perhaps make a version of update-in : update :: assoc-in : assoc ?

(defn update-in+
  "Created so vectors would also automatically be grown like maps,
   given indices not present in the vector."
  {:attribution "Alex Gunnarson"
   :todo ["optimize via transients"
          "allow to use :last on vectors"
          "allow |identity| function for unity's sake"]}
  [coll-0 [k0 & keys-0] v0]
  (let [value (core/get coll-0 k0 (when (-> keys-0 first number?) []))
        coll-f (extend-coll-to coll-0 k0)
        val-f (if keys-0
                  (update-in+ value keys-0 v0) ; make a non-stack-consuming version, possibly via trampoline? 
                  v0)
        final  (assoc coll-f k0 (whenf val-f fn? #(% (get coll-f k0))))]
   final))
;--------------------------------------------------{         ASSOC-IN         }-----------------------------------------------------
(defn assoc-in+
  [coll ks v]
  (update-in+ coll ks (constantly v)))

(defn assoc-in!
  "Associates a value in a nested associative structure, where ks is a sequence of keys
  and v is the new value and returns a new nested structure. The associative structure
  can have transients in it, but if any levels do not exist, non-transient hash-maps will
  be created."
  ^{:attribution "flatland.useful"}
  [m ks v]
  (update-in! m ks (constantly v)))

(defn assocs-in+
  {:usage "(assocs-in ['file0' 'file1' 'file2']
             [0] 'file10'
             [1] 'file11'
             [2] 'file12')"}
  [coll & kvs]
  (reduce-2 ; this is inefficient
    (fn [ret k v] (assoc-in+ ret k v))
    coll
    kvs))
;___________________________________________________________________________________________________________________________________
;=================================================={          DISSOC          }=====================================================
;=================================================={                          }=====================================================
(defn dissoc+
  {:todo ["Protocolize"]}
  ([coll key-0]
    (try
      (cond ; probably use tricks to see which subvec is longer to into is less consumptive
        (vector? coll)
          (catvec (subvec+ coll 0 key-0)
                  (subvec+ coll (inc key-0) (count coll)))
        (editable? coll)
          (-> coll transient (dissoc! coll key-0) persistent!)
        :else
          (dissoc coll key-0))))
  ([coll key-0 & keys-0]
    (loops/reduce dissoc+ coll (cons key-0 keys-0))))

(defn dissocs+ [coll & ks]
  (loops/reduce
    (fn [ret k]
      (dissoc+ ret k))
    coll
    ks))

(defn dissoc-if+ [coll pred k] ; make dissoc-ifs+
  (whenf coll (fn-> (get k) pred)
    (f*n dissoc+ k)))

(defnt dissoc++
  {:todo ["Move to collections.core"
          "Implement for vector"]}
  map? ([coll obj] (dissoc coll obj))
  set? ([coll obj] (disj   coll obj))
  nil? ([coll obj] nil))

(defn dissoc-in+
  "Dissociate a value in a nested assocative structure, identified by a sequence
  of keys. Any collections left empty by the operation will be dissociated from
  their containing structures.
  This implementation was adapted from clojure.core.contrib"
  {:attribution "weavejester.medley"
   :todo ["Transientize"]}
  [m ks]
  (if-let [[k & ks] (seq ks)]
    (if (empty? ks)
        (dissoc++ m k)
        (let [new-n (dissoc-in+ (get m k) ks)] ; this is terrible
          (if (empty? new-n) ; dissoc's empty ones
              (dissoc++ m k)
              (assoc m k new-n))))
    m))

(defn updates-in+
  [coll & kfs]
  (reduce-2 ; Inefficient
    (fn [ret k-n f-n] (update-in+ ret k-n f-n))
    coll
    kfs))

(defn re-assoc+ [coll k-0 k-f]
  (if (contains? coll k-0)
      (-> coll
         (assoc+  k-f (get coll k-0))
         (dissoc+ k-0))
      coll))

(defn re-assocs+ [coll & kfs]
  (reduce-2 ; Inefficient
    (fn [ret k-n f-n] (re-assoc+ ret k-n f-n))
    coll
    kfs))

(defn ^Map select-as+
  {:todo ["Name this function more appropriately"]
   :attribution "Alex Gunnarson"}
  ([coll kfs]
    (->> (loops/reduce
           (fn [ret k f]
             (assoc+ ret k (f coll)))
           {}
           kfs)))
  ([coll k1 f1 & {:as kfs}]
    (select-as+ coll (assoc+ kfs k1 f1))))
;___________________________________________________________________________________________________________________________________
;=================================================={   DISTINCT, INTERLEAVE   }=====================================================
;=================================================={  interpose, frequencies  }=====================================================
#?(:clj
  (defn distinct-by-java
    "Returns elements of coll which return unique
     values according to f. If multiple elements of coll return the same
     value under f, the first is returned"
    {:attribution "prismatic.plumbing"
     :performance "Faster than |core/distinct-by|"}
    [f coll]
    (let [s (java.util.HashSet.)] ; instead of #{}
      (lfor [x coll
             :let [id (f x)]
             :when (not (.contains s id))]
       (do (.add s id)
           x)))))

; (defn plicates
;   {:attribution "Alex Gunnarson"}
;   [oper n]
;   (fn [coll]
;      (-> (fn [elem]
;            (-> (filter+ (fn-eq? elem) coll)
;                count
;                (oper n))) ; duplicates? keep them
;          (filter+ coll)
;          distinct+
;          redv)))

(defprotocol Interpose
  (interpose+- [coll elem]))

; TODO: make a reducers version of coll/elem
(extend-protocol Interpose
  #?(:clj String :cljs string)
    (interpose+- [coll elem]
      (str/join elem coll))
  #?(:clj Object :cljs default)
    (interpose+- [coll elem]
      (interpose elem coll)))

(defn interpose+
  {:todo ["|definline| this"]}
  [elem coll] (interpose+- coll elem))

(defn linterleave-all
  "Analogy: partition:partition-all :: interleave:interleave-all"
  {:attribution "prismatic/plumbing"}
  [& colls]
  (lazy-seq
   ((fn helper [seqs]
      (when (seq seqs)
        (concat (map first seqs)
                (lazy-seq (helper (keep next seqs))))))
    (keep seq colls))))

; (defn interleave+ [& args] ; 4.307220 ms vs. 1.424329 ms normal interleave :/ because of zipvec...
;   (reduce
;     (fn ([]      [])
;         ([a]     (conj [] a))
;         ([a b]   (conj    a b)) 
;         ([a b c] (conj    a b c)))
;     (apply zipvec+ args)))

#?(:clj
  (defn frequencies+
    "Like clojure.core/frequencies, but faster.
     Uses Java's equal/hash, so may produce incorrect results if
     given values that are = but not .equal"
    {:attribution "prismatic.plumbing"
     :performance "4.048617 ms vs. |frequencies| 6.341091 ms"}
    [xs]
    (let [res (java.util.HashMap.)]
      (doseq [x xs]
        (->> (.get res x)
             (or 0)
             int
             unchecked-inc)
             (.put res x))
      (into {} res))))
;___________________________________________________________________________________________________________________________________
;=================================================={         GROUPING         }=====================================================
;=================================================={     group, aggregate     }=====================================================
(defn ^Delay group-merge-with+
  {:attribution "Alex Gunnarson"
   :todo ["Can probably make the |merge| process parallel."]
   :in [":a"
        "(fn [k v1 v2] v1)"
        "[{:a 1 :b 2} {:a 1 :b 5} {:a 5 :b 65}]"]
   :out "[{:b 65, :a 5} {:a 1, :b 2}]"}
  [group-by-f merge-with-f coll]
  (let [merge-like-elems 
         (fn [grouped-elems]
           (if (single? grouped-elems)
               grouped-elems
               (loops/reduce
                 (fn [ret elem]
                   (merge-with+ merge-with-f ret elem))
                 (first grouped-elems)
                 (rest  grouped-elems))))]
    (->> coll
         (group-by+ group-by-f)
         (map+ val) ; [[{}] [{}{}{}]]
         (map+ merge-like-elems)
         flatten+)))

(defn merge-left 
  ([^Key alert-level]
    (fn [k v1 v2]
      (when (not= v1 v2)
        (log/pr alert-level
          "Values do not match for merge key"
          (str (str/squote k) ":")
          (str/squote v1) "|" (str/squote v2)))
      v1))
  ([k v1 v2] v1))

(defn merge-right
  ([^Key alert-level]
    (fn [k v1 v2]
      (when (not= v1 v2)
        (log/pr alert-level
          "Values do not match for merge key"
          (str (str/squote k) ":")
          (str/squote v1) "|" (str/squote v2)))
      v1))
  ([k v1 v2] v2))

(defn ^Delay first-uniques-by+ [k coll]
  (->> coll
       (group-by+ k)
       (map+ (update-val+ first))))
;___________________________________________________________________________________________________________________________________
;=================================================={     TREE STRUCTURES      }=====================================================
;=================================================={                          }=====================================================
; Stuart Sierra: "In my tests, clojure.walk2 is about 2 times faster than clojure.walk."
(defnt walk2
  "If coll is a collection, applies f to each element of the collection
   and returns a collection of the results, of the same type and order
   as coll. If coll is not a collection, returns it unchanged. \"Same
   type\" means a type with the same behavior. For example, a hash-map
   may be returned as an array-map, but a a sorted-map will be returned
   as a sorted-map with the same comparator."
  ; clojure.lang.PersistentList$EmptyList : '()
  ; special case to preserve type
  list?            ([coll f] (apply list (map f coll)))
  ; generic sequence fallback
  seq?             ([coll f] (map f coll))
  ; |transient| discards metadata as of Clojure 1.6.0
  transientizable? ([coll f]
                     (persistent!
                       (core/reduce
                         (fn [r x] (conj! r (f x)))
                         (transient (empty coll)) coll)))
#?@(:clj
      [map-entry?  ([coll f] (map-entry (f (key coll)) (f (val coll))))
       record?     ([coll f] (core/reduce (fn [r x] (conj r (f x))) coll coll))
       :default    ([x    f] x)]
    :cljs
      [:default    ([obj  f]
                     (if (coll? obj)
                         (into (empty obj) (map f obj))
                         obj))]))

(defn- walk2-default [coll f]
  (core/reduce
    (fn [r x] (conj r (f x)))
    (empty coll) coll))

;; Persistent collections that don't support transients
#?(:clj
  (doseq [type [clojure.lang.PersistentQueue
                clojure.lang.PersistentStructMap
                clojure.lang.PersistentTreeMap
                clojure.lang.PersistentTreeSet]]
    (extend type Walk2Protocol {:walk2 walk2-default})))

(defn walk
  "Traverses form, an arbitrary data structure.  inner and outer are
  functions.  Applies inner to each element of form, building up a
  data structure of the same type, then applies outer to the result.
  Recognizes all Clojure data structures. Consumes seqs as with doall."
  {:attribution "Stuart Sierra, stuartsierra/clojure.walk2"}
  [inner outer form]
  (outer (walk2 form inner)))

(defn postwalk
  "Performs a depth-first, post-order traversal of form.  Calls f on
  each sub-form, uses f's return value in place of the original.
  Recognizes all Clojure data structures. Consumes seqs as with doall."
  {:attribution "Stuart Sierra, stuartsierra/clojure.walk2"}
  [f form]
  (walk (partial postwalk f) f form))

(defn prewalk
  "Like postwalk, but does pre-order traversal."
  {:attribution "Stuart Sierra, stuartsierra/clojure.walk2"}
  [f form]
  (walk (partial prewalk f) identity (f form)))

; COMBINE THESE TWO
(defn keywordify-keys
  "Recursively transforms all map keys from keywords to strings."
  {:attribution "Stuart Sierra, stuartsierra/clojure.walk2"
   :contributors #{"Alex Gunnarson"}}
  [^Map m]
  (let [keywordify-key
         (fn [[k v]]
           (if (string? k)
               (map-entry (keyword k) v)
               (map-entry k v)))]
    ; only apply to maps
    (postwalk
      (whenf*n map? (fn->> (map+ keywordify-key) redm))
      m)))

; COMBINE THESE TWO
(defn stringify-keys
  "Recursively transforms all map keys from keywords to strings."
  {:attribution "Stuart Sierra, stuartsierra/clojure.walk2"
   :contributors #{"Alex Gunnarson"}}
  [^Map m]
  (let [stringify-key
         (fn [[k v]]
           (if (keyword? k)
               (map-entry (name k) v)
               (map-entry k v)))]
    ; only apply to maps
    (postwalk
      (whenf*n map? (fn->> (map+ stringify-key) redm))
      m)))

(defn prewalk-replace
  "Recursively transforms form by replacing keys in smap with their
  values.  Like clojure/replace but works on any data structure.  Does
  replacement at the root of the tree first."
  {:attribution "Stuart Sierra, stuartsierra/clojure.walk2"}
  [smap form]
  (prewalk (whenf*n (f*n in? smap) smap) form))

(defn postwalk-replace
  "Recursively transforms form by replacing keys in smap with their
  values.  Like clojure/replace but works on any data structure.  Does
  replacement at the leaves of the tree first."
  {:attribution "Stuart Sierra, stuartsierra/clojure.walk2"}
  [smap form]
  (postwalk (whenf*n (f*n in? smap) smap) form))

(defn tree-filter
  "Like |filter|, but performs a |postwalk| on a treelike structure @tree, putting in a new vector
   only the elements for which @pred is true."
  {:attribution "Alex Gunnarson"}
  [^Fn pred tree]
  (let [results (transient [])]
    (postwalk
      (whenf*n pred
        (fn->> (withf->> #(conj! results %)))) ; keep it the same
      tree)
    (persistent! results)))

(defn- sort-parts
  "Lazy, tail-recursive, incremental quicksort. Works against
   and creates partitions based on the pivot, defined as 'work'."
  {:attribution "The Joy of Clojure, 2nd ed."}
  [work]
  (lazy-seq
    (loop [[part & parts] work]
      (if-let [[pivot & xs] (seq part)]
        (let [smaller? #(< % pivot)]
          (recur (list*
                  (filter smaller? xs)
                  pivot
                  (remove smaller? xs)
                  parts)))
        (when-let [[x & parts] parts]
          (cons x (sort-parts parts)))))))

(defn lsort
  "Lazy 'quick'-sorting"
  {:attribution "The Joy of Clojure, 2nd ed."}
  [elems]
  (sort-parts (list elems))) 
;___________________________________________________________________________________________________________________________________
;=================================================={   COLLECTIONS CREATION   }=====================================================
;=================================================={                          }=====================================================

; TODO fix
(def map->record hash-map)

(defn reverse-kv [m]
  (zipmap (vals m) (keys m)))
