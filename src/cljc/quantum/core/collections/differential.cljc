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
              #?(:cljs boolean?)])
           (:require [#?(:clj  clojure.core
                         :cljs cljs.core   )                  :as core   ]
                     [quantum.core.data.map                   :as map
                       :refer [split-at]                                 ]
                     [quantum.core.data.set                   :as set    ]
                     [quantum.core.data.vector                :as vec  
                       :refer [catvec subvec+]                           ]
                     [quantum.core.collections.core           :as coll   
                       :refer [#?@(:clj [count first rest getr last-index-of
                                         index-of lasti empty?])
                               key val reverse]                          ]
                     [quantum.core.collections.base           :as base   
                       :refer [#?@(:clj [kmap])]                         ]
                     [quantum.core.collections.map-filter
                       :refer [ffilteri last-filteri]                    ]
                     [quantum.core.error                      :as err  
                       :refer [->ex]                                     ]
                     [quantum.core.fn                         :as fn  
                       :refer [#?@(:clj [compr <- fn-> fn->>
                                         f*n ->predicate])
                               fn-nil juxt-kv withf->>]                  ]
                     [quantum.core.log                        :as log    ]
                     [quantum.core.logic                      :as logic
                       :refer [#?@(:clj [fn-not fn-or fn-and whenf whenf*n
                                         ifn if*n condf condf*n]) nnil? any?]]
                     [quantum.core.macros                     :as macros 
                       :refer [#?@(:clj [defnt])]                        ]
                     [quantum.core.reducers                   :as red    
                       :refer [#?@(:clj [reduce]) map+]                  ]
                     [quantum.core.string                     :as str    ]
                     [quantum.core.string.format              :as sform  ]
                     [quantum.core.type                       :as type  
                       :refer [#?@(:clj [lseq? transient? editable? 
                                         boolean? should-transientize?])]]
                     [quantum.core.analyze.clojure.predicates :as anap   ]
                     [quantum.core.type.predicates            :as tpred  ]
                     [clojure.walk                            :as walk   ]
                     [quantum.core.loops                      :as loops  ]
                     [quantum.core.vars                       :as var  
                       :refer [#?@(:clj [defalias])]                     ])
  #?(:cljs (:require-macros  
                     [quantum.core.collections.core           :as coll   
                       :refer [count first rest getr lasti index-of lasti
                               empty?]                                   ]
                     [quantum.core.collections.base           :as base   
                       :refer [kmap]                                     ]
                     [quantum.core.fn                         :as fn
                       :refer [compr <- fn-> fn->> f*n]       ]
                     [quantum.core.log                        :as log    ]
                     [quantum.core.logic                      :as logic 
                       :refer [fn-not fn-or fn-and whenf whenf*n 
                               ifn if*n condf condf*n]                   ]
                     [quantum.core.loops                      :as loops  ]
                     [quantum.core.macros                     :as macros 
                       :refer [defnt]                                    ]
                     [quantum.core.reducers                   :as red    
                       :refer [reduce]                                   ]
                     [quantum.core.type                       :as type 
                       :refer [lseq? transient? editable? boolean? 
                               should-transientize?]                     ]
                     [quantum.core.vars                       :as var 
                       :refer [defalias]                                 ])))

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

; ================================================ TAKE ================================================
; ============ TAKE-LEFT ============
(defn takel
  {:tests '{(takel 2 "abcdefg")
            "ab"}}
  [i super]
  (getr super 0 (-> i dec long)))

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
  (getr super i (lasti super)))

(def take-fromi takel-fromi)

(defn takel-from
  {:tests '{(takel-from "cd" "abcdefg")
            "cdefg"}}
  [sub super]
  (let [i (or (index-of super sub)
              (throw (->ex :out-of-bounds nil (kmap super sub))))]
    (takel-fromi i super)))

(def take-from takel-from)

(defn takel-afteri
  {:tests '{(takel-afteri 2 "abcdefg")
            "defg"}}
  [i super]
  (getr super (-> i long inc) (lasti super)))

(def take-afteri takel-afteri)

(defn takel-after
  {:tests '{(takel-after "cd" "abcdefg")
            "efg"}}
  [sub super]
  (let [i (or (index-of super sub)
              (throw (->ex :out-of-bounds nil (kmap super sub))))
        i-f (+ i (lasti sub))]
    (takel-afteri i-f super)))

(def take-after takel-after)

(defn takel-while
  {:tests '{(takel-while (eq? \=) "===abc=")
            "==="}}
  [pred super]
  (let [not-i (index-of-pred super (fn-not pred))]
    (getr super 0 (dec (if (pred (first super))
                           (or not-i (count super))
                           (or not-i 0))))))

(def take-while   takel-while    )
(def ltakel-while core/take-while)
(def ltake-while  ltakel-while   )

(def takel-untili takel          )
(def take-untili  take-untili    )

(defn takel-until
  {:tests '{(takel-until (eq? \=) "!===abc=")
            "!"}}
  [pred super]
  (takel-while (fn-not pred) super))

(def take-until takel-until)

(defnt takel-until-inc
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
    (constantly nil)))

(def take-while-matches takel-while-matches)

(defn takel-until-matches 
  {:tests '{(takel-until-matches  "--" "ab--sddasd--")
            "ab"
            (takel-until-matches [1 2] [9 8 82 1 2 3 5 3])
            [9 8 82]}}
  [sub super]
  (while-matches sub super fn/firsta
    (fn [super-i _] (takel (- super-i (lasti sub)) super))
    (constantly super)))

(def take-until-matches takel-until-matches)

(def takel-while-not-matches takel-until-matches)

; ============ TAKE-RIGHT ============
(defn taker 
  [i super]
  (getr super (- (count super) i) (lasti super)))

(defn takeri
  "Take up to and including index, starting at the right."
  {:in  [2 "abcdefg"]
   :out "cdefg"}
  [i super]
  (getr super i (lasti super)))

(defn taker-untili
  "Take until index, starting at the right."
  {:in  [2 "abcdefg"]
   :out "defg"}
  [i super]
  (getr super (-> i long inc) (lasti super)))

(defn taker-while
  {:todo ["Use rreduce (reversed reduce) instead of reverse. Possibly reversed-last-index-of"]}
  [pred super]
  (let [rindex
          (reduce
            (fn [i elem]
              (if (pred elem) (dec i) (reduced i)))
            (count super)
            (reverse super))]
    (getr super rindex (lasti super))))

(defnt taker-until
  "Take until index of, starting at the right."
  {:in  ["c" "abcdefg"]
   :out "defg"}
  ([^fn? pred super] (taker-while (fn-not pred) super      ))
  ([     sub  super] (taker-until sub           super super))
  ([sub alt   super]
    (let [i (last-index-of super sub)]
      (if i
          (taker-untili i super)
          alt))))

(defn taker-after
  {:in ["." "abcdefg.asd"]
   :out "abcdefg"}
  [sub super]
  (if-let [i (last-index-of super sub)]
    (getr super 0 (-> i long dec))
    nil))

; ================================================ DROP ================================================

(defn dropl [n coll] (getr coll n (lasti coll)))

(def      drop   dropl    )
(defalias drop+  red/drop+)
(def      ldropl core/drop)


; (let [index-r
;           (whenc (last-index-of super sub) (fn= -1)
;             (throw (str "Index of" (str/squote sub) "not found.")))])
;   (getr super
;     (inc (last-index-of super sub))
;     (-> super lasti))

(defn dropl-while-matches 
  {:tests '{(dropl-while-matches "--" "---asddasd--")
            "-asddasd--"}}
  [sub super]
  (while-matches sub super (fn [_ x] (not x))
    (fn [_ match-length] (dropl match-length super))
    (constantly nil)))

(def drop-while-matches dropl-while-matches)

(defn dropl-until-matches 
  {:tests '{(dropl-until-matches  "--" "ab--sddasd--")
            "--sddasd--"}}
  [sub super]
  (while-matches sub super fn/firsta
    (fn [super-i _] (dropl (- super-i (lasti sub)) super))
    (constantly super)))

(def drop-until-matches dropl-until-matches)

(def dropl-while-not-matches dropl-until-matches)

; DROPR

(defn dropr
  {:in  [3 "abcdefg"]
   :out "abcd"}
  [n coll]
  (getr coll 0 (-> coll lasti long (- (long n)))))

#?(:clj (defalias dropr+ red/dropr+))

(defn ldropr
  {:attribution "Alex Gunnarson"}
  [n coll]
  (getr coll 0 (-> coll lasti long (- (long n)))))

(defn dropr-while [pred super]
  (getr super 0
    (dec (or (last-index-of-pred super pred)
             (count super)))))

(defnt dropr-until
  "Until right index of."
  {:todo "Combine code with /takeri/"
   :in  ["cd" "abcdefg"]
   :out "abcd"}
  ([^fn? pred super] (dropr-while super (fn-not pred)))
  ([sub super]
    (if-let [i (last-index-of super sub)]
      (getr super 0 (+ i (lasti sub)))
      super)))

(defn dropr-after
  "Until right index of."
  {:todo "Combine code with /takeri/"
   :in  ["cd" "abcdefg"]
   :out "ab"}
  ([sub super]
    (if (index-of super sub)
        (getr super 0 (index-of super sub))
        super)))

(defn dropr-while-matches
  {:tests '{(dropr-while-matches "--" "---asddasd---")
            "---asddasd-"}
   :todo ["Use |rreduce|, not |reverse|"]}
  [sub super]
  (while-matches (reverse sub) (reverse super) (fn [_ x] (not x))
    (fn [_ match-length] (dropr match-length super))
    (constantly nil)))

(defn dropr-until-matches 
  {:tests '{(dropr-until-matches  "--" "ab--sddasd-")
            "ab--"}
   :todo ["Use |rreduce|, not |reverse|"]}
  [sub super]
  (while-matches (reverse sub) (reverse super) fn/firsta
    (fn [super-i _] (dropr (- super-i (lasti sub)) super))
    (constantly super)))

(def dropr-while-not-matches dropr-until-matches)

(defn remove-surrounding
  {:tests '{(remove-surrounding "--abcde--" "--")
            "abcde"}}
  [surr s]
  (->> s
       (dropl-while-matches surr)
       (dropr-while-matches surr)))