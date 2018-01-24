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
    :attribution "alexandergunnarson"}
  quantum.core.collections.differential
  (:refer-clojure :exclude
    [for doseq reduce
     contains?
     repeat repeatedly
     interpose
     range
     take take-while
     drop  drop-while
     subseq
     key val
     merge sorted-map sorted-map-by
     into
     count
     empty empty?
     split-at
     first second rest last butlast get pop peek
     select-keys
     zipmap
     reverse
     conj
     conj! assoc! dissoc! disj!
     boolean?])
  (:require
    [clojure.core                  :as core]
    [quantum.core.compare          :as comp]
    [quantum.core.data.map         :as map
      :refer [split-at]]
    [quantum.core.data.vector      :as vec
      :refer [subsvec]]
    [quantum.core.collections.core :as coll
      :refer [reverse key val first rest get slice count lasti index-of last-index-of empty?]]
    [quantum.core.error            :as err
      :refer [>ex-info]]
    [quantum.core.fn               :as fn
      :refer [fn']]
    [quantum.core.logic
      :refer [fn-not]]
    [quantum.core.macros           :as macros
      :refer [defnt]]
    [quantum.core.reducers         :as red
      :refer [map+ reduce indexed+]]
    [quantum.core.vars             :as var
      :refer [defalias]]
    [quantum.core.type             :as t]
    [quantum.core.collections.map-filter
      :refer        [ffilteri last-filteri]]
    [quantum.untyped.core.data
      :refer [kw-map]]))
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
; rip [o index] - rips coll and returns [pre-coll item-at suf-coll]
; sew [pre-coll item-arr suf-coll] - opposite of rip, but with arr
(defn split [ind coll-0]
  (if (vector? coll-0)
      [(subsvec coll-0 0   ind)
       (subsvec coll-0 ind (count coll-0))]
      (split-at coll-0 ind)))
(defn split-with-v+ [pred coll-0] ; IMPROVE
  (->> coll-0
       (split-with pred)
       (map+ vec)))

(declare drop ldropl)

(defn while-matches
  "Eager take or drop while matches."
  {:implementation "This is possible with either |reducei| or a |loop| + |seq|.
                    Likely the loop is faster in this case."}
  [sub super pred f end-f]
  (loop [super-n  super
         sub-n    sub
         super-i  0
         match-ct (long 0)]
    (if (empty? super-n)
        (end-f)
        (let [eq-elems? (= (first sub-n) (first super-n))
              sub-f     (if eq-elems? (rest sub-n) sub)
              match?    (empty? sub-f)
              [sub-f match-ct-f]
                 (if match?
                     [sub   (inc match-ct)]
                     [sub-f match-ct      ])]
          (if (pred match? eq-elems?)
              (f super-i (* match-ct-f (count sub)))
              (recur (rest super-n)
                     sub-f
                     (inc super-i)
                     (long match-ct-f)))))))

(defn index-of-pred      [coll pred]
  (->> coll (ffilteri     pred) key))

(defn last-index-of-pred [coll pred]
  (->> coll (last-filteri pred) key))

; TODO move these?
; TODO implement `max` in terms of reduce generally?

(defn find-max
  {:example `{(find-max [0 2 1 6 -1 5])
              [3 6]}}
  [xs]
  (->> xs
       indexed+
       (reduce (fn [[_ x :as i+x] [i' x' :as i+x']]
                 (if (or (zero? i') (> x' x))
                     i+x'
                     i+x))
               nil)))

(defn index-of-max
  {:example `{(index-of-max [0 2 1 6 -1 5])
              3}}
  [xs]
  (first (find-max xs)))

; ================================================ TAKE ================================================
; ============ TAKE-LEFT ============
(defn takel ; TODO this is actually `takel'`
  {:tests '{(takel 2 "abcdefg")
            "ab"}}
  [i super]
  (slice super 0 (long i)))

        (def      take        takel          )
        (def      ltake       core/take      )
        (defalias take+       red/take+      )
#?(:clj (defalias taker+      red/taker+     ))
        (defalias take-while+ red/take-while+)

(defn takel-fromi
  "Take starting at and including index n."
  {:tests '{(takel-fromi 2 "abcdefg")
            "cdefg"}}
  [i super]
  (slice super i (count super)))

(def take-fromi takel-fromi)

(defn takel-from
  {:tests '{(takel-from "cd" "abcdefg")
            "cdefg"}}
  [sub super]
  (let [i (or (index-of super sub)
              (throw (>ex-info :out-of-bounds nil (kw-map super sub))))]
    (takel-fromi i super)))

(def take-from takel-from)

(defn takel-afteri
  {:tests '{(takel-afteri 2 "abcdefg")
            "defg"}}
  [i super]
  (slice super (-> i long inc) (count super)))

(def take-afteri takel-afteri)

(defn takel-after
  {:tests '{(takel-after "cd" "abcdefg")
            "efg"}}
  [sub super]
  (let [i (or (index-of super sub)
              (throw (>ex-info :out-of-bounds nil (kw-map super sub))))
        i-f (+ i (lasti sub))]
    (takel-afteri i-f super)))

(def take-after takel-after)

(defn takel-while
  {:tests '{(takel-while (fn= \=) "===abc=")
            "==="}}
  [pred super]
  (let [not-i (index-of-pred super (fn-not pred))]
    (slice super 0 (if (pred (first super))
                       (or not-i (count super))
                       (or not-i 0)))))

(def take-while   takel-while    )
(def ltakel-while core/take-while)
(def ltake-while  ltakel-while   )

(def takel-untili takel          )
(def take-untili  take-untili    )

(defn takel-until
  "Take from coll up to and including the first item that satisfies pred."
  {:tests '{(takel-until (fn= \=) "!===abc=")
            "!"}}
  [pred super]
  (takel-while (fn-not pred) super))

(def take-until takel-until)

(defn takel-until-inc
  ([sub super]
    (if-let [i (index-of super sub)]
      (takel-untili (+ i (count sub)) super)
      super)))

(defalias take-until-inc takel-until-inc)

(defn takel-while-matches
  {:tests '{(takel-while-matches "--" "---asddasd--")
            "--"}}
  [sub super]
  (while-matches sub super (fn [_ x] (not x))
    (fn [_ match-length] (takel match-length super))
    (fn' nil)))

(def take-while-matches takel-while-matches)

(defn takel-until-matches
  {:tests '{(takel-until-matches  "--" "ab--sddasd--")
            "ab"
            (takel-until-matches [1 2] [9 8 82 1 2 3 5 3])
            [9 8 82]}}
  [sub super]
  (while-matches sub super fn/firsta
    (fn [super-i _] (takel (- super-i (lasti sub)) super))
    (fn' super)))

(def take-until-matches takel-until-matches)

(def takel-while-not-matches takel-until-matches)

(declare dropl)

(defn takel-after-matches
  {:tests '{(takel-after-matches  "--" "ab--sddasd--")
            "sddasd--"}}
  [sub super]
  (while-matches sub super fn/firsta
    (fn [super-i _] (dropl (+ (count sub) (- super-i (lasti sub))) super))
    (fn' super)))

(def take-after-matches takel-after-matches)

; ============ TAKE-RIGHT ============
(defn taker
  [i super]
  (slice super (- (count super) i) (count super)))

(defn takeri
  "Take up to and including index, starting at the right."
  {:in  [2 "abcdefg"]
   :out "cdefg"}
  [i super]
  (slice super i (count super)))

(defn taker-untili
  "Take until index, starting at the right."
  {:in  [2 "abcdefg"]
   :out "defg"}
  [i super]
  (slice super (-> i long inc) (count super)))

(defn taker-while
  {:todo ["Use rreduce (reversed reduce) instead of reverse. Possibly reversed-last-index-of"]}
  [pred super]
  (let [rindex
          (reduce
            (fn [i elem]
              (if (pred elem) (dec i) (reduced i)))
            (count super)
            (reverse super))]
    (slice super rindex (count super))))

(defnt taker-until
  "Take until index of, starting at the right."
  {:in  ["c" "abcdefg"]
   :out "defg"}
  ([^fn? pred super] (taker-while (fn-not pred) super      ))
  ([     sub  super] (taker-until sub           super super))
  ([sub alt   super]
    (let [i (coll/last-index-of-protocol super sub)]
      (if i
          (taker-untili i super)
          alt))))

(defn taker-after
  {:in ["." "abcdefg.asd"]
   :out "abcdefg"}
  [sub super]
  (if-let [i (last-index-of super sub)]
    (slice super 0 (long i))
    nil))

(defalias take-nth+  red/take-nth+)
(defalias takel-nth+ take-nth+)

; ================================================ DROP ================================================

(defn dropl [n coll] (slice coll n (count coll)))

(def      drop   dropl    )
(defalias drop+  red/drop+)
(def      ldropl core/drop)
(def      ldrop  core/drop)


; (let [index-r
;           (whenc (last-index-of super sub) (fn= -1)
;             (throw (str "Index of" (str/squote sub) "not found.")))])
;   (count super
;     (inc (last-index-of super sub))
;     (count super))

(defn dropl-while-matches
  {:tests '{(dropl-while-matches "--" "---asddasd--")
            "-asddasd--"}}
  [sub super]
  (while-matches sub super (fn [_ x] (not x))
    (fn [_ match-length] (dropl match-length super))
    (fn' nil)))

(def drop-while-matches dropl-while-matches)

(defn dropl-until-matches
  {:tests '{(dropl-until-matches  "--" "ab--sddasd--")
            "--sddasd--"}}
  [sub super]
  (while-matches sub super fn/firsta
    (fn [super-i _] (dropl (- super-i (lasti sub)) super))
    (fn' super)))

(def drop-until-matches dropl-until-matches)

(def dropl-while-not-matches dropl-until-matches)

; DROPR

(defn dropr
  {:in  [3 "abcdefg"]
   :out "abcd"}
  [n coll]
  (slice coll 0 (-> coll count long (- (long n)))))

#?(:clj (defalias dropr+ red/dropr+))

(defn ldropr
  {:attribution "alexandergunnarson"}
  [n coll]
  (slice coll 0 (-> coll count long (- (long n)))))

(defn dropr-while
  {:todo #{"use `rreduce`"}
   :tests `{(dropr-while nil? [0 1 2 3 nil 4 5 nil nil])
            [0 1 2 3 nil 4 5]}}
  [pred super]
  (assert (t/indexed? super)) ; TODO for now, until `rreduce`
  (loop [i (lasti super)]
    (if (= i -1)
        (slice super 0 0)
        (if (pred (get super i))
            (recur (dec i))
            (slice super 0 (inc i))))))

(defnt dropr-until
  "Until right index of."
  {:todo "Combine code with /takeri/"
   :in  ["cd" "abcdefg"]
   :out "abcd"}
  ([^fn? pred super] (dropr-while super (fn-not pred)))
  ([sub super]
    (if-let [i (coll/last-index-of-protocol super sub)]
      (slice super 0 (+ i (count sub)))
      super)))

(defn dropr-after
  "Until right index of."
  {:todo "Combine code with /takeri/"
   :in  ["cd" "abcdefg"]
   :out "ab"}
  ([sub super]
    (if (index-of super sub)
        (slice super 0 (inc (index-of super sub)))
        super)))

(defn dropr-while-matches
  {:tests '{(dropr-while-matches "--" "---asddasd---")
            "---asddasd-"}
   :todo ["Use |rreduce|, not |reverse|"]}
  [sub super]
  (while-matches (reverse sub) (reverse super) (fn [_ x] (not x))
    (fn [_ match-length] (dropr match-length super))
    (fn' nil)))

(defn dropr-until-matches
  {:tests '{(dropr-until-matches  "--" "ab--sddasd-")
            "ab--"}
   :todo ["Use |rreduce|, not |reverse|"]}
  [sub super]
  (while-matches (reverse sub) (reverse super) fn/firsta
    (fn [super-i _] (dropr (- super-i (lasti sub)) super))
    (fn' super)))

(def dropr-while-not-matches dropr-until-matches)

(defn remove-surrounding
  {:tests '{(remove-surrounding "--abcde--" "--")
            "abcde"}}
  [surr s]
  (->> s
       (dropl-while-matches surr)
       (dropr-while-matches surr)))

(defn ldrop-at
  {:adapted-from 'criterium.stats}
  [n coll]
  (lazy-seq
    (when-let [s (seq coll)]
      (concat (ltake n s) (next (ldrop n s))))))
