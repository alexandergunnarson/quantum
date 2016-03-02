(ns ^{:doc "Persistent collections based on 2-3 finger trees."
      :author "Chris Houser"
      :contributors {"Alex Gunnarson" "Added CLJS compatibility"}}
  quantum.core.data.finger-tree
  (:require-quantum [:core map])
  (:require
    #? (:clj  [quantum.core.macros.protocol
                :refer [deftype-compatible]])
              [quantum.core.core
                :refer [seq-equals]]
    #? (:clj  [quantum.core.data.finger-tree.macros
                :refer [defdigit make-digit delay-ft]])
    #?@(:cljs [[cljs.analyzer
                 :refer [macroexpand-1]]
               [cljs.core    
                 :refer [INext IEmptyableCollection IEquiv IAssociative]]]))
  #?(:cljs
  (:require-macros
              [quantum.core.macros.protocol
                :refer [deftype-compatible]        ]
              [quantum.core.data.finger-tree.macros
                :refer [defdigit make-digit delay-ft]]
              [quantum.core.data.finger-tree
                :refer [meter]]))
  #?(:clj
  (:import (clojure.lang Seqable Sequential ISeq IPersistentSet ILookup
                         IPersistentStack IPersistentCollection Associative
                         Sorted Reversible Indexed Counted IHashEq))))

(defn hashcode [x]
  #?(:clj  (clojure.lang.Util/hash x)
     :cljs (cljs.core/hash         x)))

(defn hash-ordered [coll] ; hasheq-seq
  (hash-ordered-coll coll))

(defn hash-unordered [coll]
  (hash-unordered-coll coll))

#?(:cljs (defrecord IndexOutOfBoundsException []))


(comment ; TODO:
"
- implement java.util.Collection
- implement IMeta
- implement IChunkedSeq?
- replace copy/pasted code with macros
- test dequeue complexity
- confirm recursion is bounded, though perhaps O(log n) growth is slow enough
- add simple lookup to Splittable?
- add sorted map with index?"
)

(defprotocol ConjL
  (conjl [s a] "Append a to the left-hand side of s"))

(defprotocol ObjMeter
  "Object for annotating tree elements.  idElem and op together form a Monoid."
  (measure [_ o] "Return the measured value of o (same type as idElem)")
  (idElem  [_]   "Return the identity element for this meter")
  (opfn    [_]   "Return an associative function of two args for combining measures"))

(defprotocol Measured
  (measured [o] "Return the measured value of o")
  (getMeter [o] "Return the meter object for o"))

(defprotocol Splittable
  (split [o pred acc] "Return [pre m post] where pre and post are trees"))

(defprotocol SplitAt
  (ft-split-at [o k notfound] [o k]
               "Return [pre m post] where pre and post are trees"))

(defprotocol Tree
  (app3        [t1 ts t2] "Append ts and (possibly deep) t2 to tree t1")
  (app3deep    [t2 ts t1] "Append ts and t2 to deep tree t1")
  (measureMore [o]        "Return the measure of o not including the leftmost item")
  (measurePop  [o]        "Return the measure of o not including the rightmost item"))

(extend-type nil
  ObjMeter
    (measure  [_ _] nil)
    (idElem   [_]   nil)
    (opfn     [_]   nil)
  Measured
    (measured [_]   nil)
    (getMeter [_]   nil))

(declare newEmptyTree newSingleTree newDeepTree digit deep)

(defdigit #?(:clj :clj :cljs :cljs) a)
(defdigit #?(:clj :clj :cljs :cljs) a b)
(defdigit #?(:clj :clj :cljs :cljs) a b c)
(defdigit #?(:clj :clj :cljs :cljs) a b c d)

(defn digit
  ([meter-obj a]       (make-digit meter-obj a))
  ([meter-obj a b]     (make-digit meter-obj a b))
  ([meter-obj a b c]   (make-digit meter-obj a b c))
  ([meter-obj a b c d] (make-digit meter-obj a b c d)))

#?(:clj
(defmacro meter [measure idElem op]
  `(reify ObjMeter
      (measure [_# a#] (~measure a#))
      (idElem  [_#]    ~idElem)
      (opfn    [_#]    ~op))))

(defn ^:static nodes [mfns xs]
  (let [v (vec xs) c (count v)]
    (seq
      (loop [i (int 0) nds []]
        (condp == (- c i)
          (int 2) (-> nds (conj (digit mfns (v i) (v (+ (int 1) i)))))
          (int 3) (-> nds (conj (digit mfns (v i) (v (+ (int 1) i))
                                       (v (+ (int 2) i)))))
          (int 4) (-> nds (conj (digit mfns (v i) (v (+ (int 1) i))))
                    (conj (digit mfns (v (+ (int 2) i))
                                 (v (+ (int 3) i)))))
          (recur (+ (int 3) i)
                 (-> nds
                   (conj (digit mfns (v i) (v (+ (int 1) i))
                                (v (+ (int 2) i)))))))))))

(deftype-compatible #?(:clj :clj :cljs :cljs) 'EmptyTree '[meter-obj]
  {'?Seqable
      {'seq `[[_#] nil]}
   '?Sequential true
   '?Seq
      {'first `[[_#]    nil  ]
       'rest  `[[this#] this#]
       'next  `[[_#]    nil  ]}
   '?Stack
      {'peek  `[[_#]    nil  ]
       'pop   `[[this#] this#]}
   '?Reversible
      {'rseq  `[[_#]    nil  ]}
   '?Collection
      {'empty `[[this#] this#]
       'equiv `[[_# x#] false] ; TBD
       'conj  `[[_# b#] (newSingleTree ~'meter-obj b#)]}
   '?Counted
      {'count `[[_#   ] 0    ]}  ; not needed?
   'ConjL
      {'conjl `[[_# a#] (newSingleTree ~'meter-obj a#)]}
   'Measured
      {'measured `[[_#] (idElem ~'meter-obj)]
       'getMeter `[[_#] ~'meter-obj]} ; not needed?
   ; Splittable
   ;   (split [pred acc]) ; TBD -- not needed??
   'Tree
      {'app3        `[[_# ts# t2#] (reduce ~'conjl t2# (reverse ts#))]
       'app3deep    `[[_# ts# t1#] (reduce conj  t1# ts#)]
       'measureMore `[[_#]         (idElem ~'meter-obj)]
       'measurePop  `[[_#]         (idElem ~'meter-obj)]}})

(defn ^:static newEmptyTree [meter-obj]
  (EmptyTree. meter-obj))

(defn ^:static finger-meter [meter-obj]
  (when meter-obj
    (meter
      #(measured %)
      (idElem meter-obj)
      (opfn meter-obj))))

(defn split-tree [t p]
  (->> t getMeter idElem (split t p)))


(deftype-compatible #?(:clj :clj :cljs :cljs) 'DelayedTree '[tree-ref mval]
  {'?Seqable
     {'seq      `[[this#] this#]}
   '?Sequential true
   '?Seq
     {'first    `[[_#] (first @~'tree-ref)]
      'rest     `[[_#] (rest  @~'tree-ref)]
      'next     `[[_#] (next  @~'tree-ref)]}
   '?Stack
     {'peek     `[[_#] (peek  @~'tree-ref)]
      'pop      `[[_#] (pop   @~'tree-ref)]}
   '?Reversible
     {'rseq     `[[_#] (rseq  @~'tree-ref)]} ; not this because tree ops can't be reversed
   '?Counted
     {'count    `[[_#]]} ; not needed?
   '?Collection
     {'empty    `[[_#]    (empty @~'tree-ref)]
      'equals   `[[_# x#] false] ; TBD
      'cons     `[[_# b#] (conj  @~'tree-ref b#)]}
   'ConjL
     {'conjl    `[[_# a#] (conjl @~'tree-ref a#)]}
   'Measured
     {'measured `[[_#] ~'mval]
      'getMeter `[[_#] (getMeter @~'tree-ref)]} ; not needed?
   'Splittable
     {'split    `[[_# pred# acc#] (split @~'tree-ref pred# acc#)]}
   'Tree
     {'app3        `[[_# ts# t2#] (app3     @~'tree-ref ts# t2#)]
      'app3deep    `[[_# ts# t1#] (app3deep @~'tree-ref ts# t1#)]
      'measureMore `[[_#] (measureMore @~'tree-ref)]
      'measurePop  `[[_#] (measurePop  @~'tree-ref)]}})

(defn ^:static to-tree [meter-obj coll]
  (reduce conj (EmptyTree. meter-obj) coll))

(defn deep-left [pre m suf]
  (cond
    (seq pre) (deep pre m suf)
    (empty? (first m)) (to-tree (getMeter suf) suf)
    :else (deep (first m)
                (delay-ft (rest m) (measureMore m))
                suf)))

(defn deep-right [pre m suf]
  (cond
    (seq suf) (deep pre m suf)
    (empty? (peek m)) (to-tree (getMeter pre) pre)
    :else (deep pre
                (delay-ft (pop m) (measurePop m))
                (peek m))))

(defn ft-concat [t1 t2]
  (assert (= (getMeter t1) (getMeter t2)) #{t1 t2}) ; meters must be the same
  (app3 t1 nil t2))

(deftype-compatible #?(:clj :clj :cljs :cljs) 'SingleTree '[meter-obj x]
  {'?Seqable
     {'seq    `[[this#] this#]}
   '?Sequential true
   '?Seq 
     {'first  `[[_#] ~'x]
      'rest   `[[_#] (EmptyTree. ~'meter-obj)]
      'next   `[[_#] nil]}
   '?Stack
     {'peek   `[[_#] ~'x]
      'pop    `[[_#] (EmptyTree. ~'meter-obj)]}
   '?Reversible
     {'rseq   `[[_#] (list ~'x)]} ; not 'this' because tree ops can't be reversed
   '?Counted
     {'count  `[[_#]]}; not needed?
   '?Collection
     {'empty  `[[_#] (EmptyTree. ~'meter-obj)] ; not needed?
      'equals `[[_# x#] false] ; TBD
      'conj   `[[_# b#] (deep (digit ~'meter-obj ~'x)
                           (EmptyTree. (finger-meter ~'meter-obj))
                           (digit ~'meter-obj b#))]}
   '?ConjL
     {'conjl  `[[_# a#] (deep (digit ~'meter-obj a#)
                          (EmptyTree. (finger-meter ~'meter-obj))
                          (digit ~'meter-obj ~'x))]}
   'Measured
     {'measured `[[_#] (measure ~'meter-obj ~'x)]
      'getMeter `[[_#] ~'meter-obj]} ; not needed?
   'Splittable
     {'split    `[[this# pred# acc#]
                   (let [e# (empty this#)] [e# ~'x e#])]}
   'Tree
     {'app3        `[[this# ts# t2#] (conjl (app3 (empty this#) ts# t2#) ~'x)]
      'app3deep    `[[_# ts# t1#] (conj (reduce conj t1# ts#) ~'x)]
      'measureMore `[[_#] (idElem ~'meter-obj)]
      'measurePop  `[[_#] (idElem ~'meter-obj)]}})

(defn ^:static newSingleTree [meter-obj x]
  (SingleTree. meter-obj x))

(defn- measured3 [meter-obj pre m suf]
  (when-let [op (opfn meter-obj)]
    (op
      (op (measured pre)
          (measured m))
        (measured suf))))

(deftype-compatible #?(:clj :clj :cljs :cljs) 'DeepTree '[meter-obj pre mid suf mval]
  {'?Seqable
    {'seq     `[[this#] this#]}
   '?Sequential true
   '?Seq
     {'first  `[[_#] (first ~'pre)]
      'rest   `[[_#] (deep-left (rest ~'pre) ~'mid ~'suf)]
      'next   `[[this#] (seq (rest this#))]}
   '?Stack
     {'peek   `[[_#] (peek ~'suf)]
      'pop    `[[_#] (deep-right ~'pre ~'mid (pop ~'suf))]}
   '?Reversible
     {'rseq   `[[this#] (lazy-seq (cons (peek this#) (rseq (pop this#))))]}
   '?Counted
     {'count  `[[_#]]} ; not needed?
   '?Collection
     {'empty  `[[_#] (newEmptyTree ~'meter-obj)]
      'equals `[[_# x#] false] ; TBD
      'conj   `[[_# a#]
                 (if (< (count ~'suf) 4)
                     (deep ~'pre ~'mid (conj ~'suf a#))
                     (let [[e# d# c# b#] ~'suf
                           n# (digit ~'meter-obj e# d# c#)]
                       (deep ~'pre (conj ~'mid n#) (digit ~'meter-obj b# a#))))]}
   'ConjL
     {'conjl  `[[_# a#]
                 (if (< (count ~'pre) 4)
                     (deep (conjl ~'pre a#) ~'mid ~'suf)
                     (let [[b# c# d# e#] ~'pre
                           n# (digit ~'meter-obj c# d# e#)]
                       (deep (digit ~'meter-obj a# b#)
                             (conjl ~'mid n#) ~'suf)))]}
   'Measured
     {'measured `[[_#] @~'mval]
      'getMeter `[[_#] (getMeter ~'pre)]} ; not needed?
   'Splittable ; allow to fail if op is nil:
     {'split    `[[_# pred# acc#]
                   (let [op# (opfn ~'meter-obj)
                         vpr# (op# acc# (measured ~'pre))]
                     (if (pred# vpr#)
                       (let [[sl# sx# sr#] (split ~'pre pred# acc#)]
                         [(to-tree ~'meter-obj sl#) sx# (deep-left sr# ~'mid ~'suf)])
                       (let [vm# (op# vpr# (measured ~'mid))]
                         (if (pred# vm#)
                           (let [[ml# xs# mr#] (split ~'mid pred# vpr#)
                                 [sl# sx# sr#] (split xs# pred# (op# vpr# (measured ml#)))]
                             [(deep-right ~'pre ml# sl#) sx#
                              (deep-left sr# mr# ~'suf)])
                           (let [[sl# sx# sr#] (split ~'suf pred# vm#)]
                             [(deep-right ~'pre ~'mid sl#)
                               sx#
                               (to-tree ~'meter-obj sr#)])))))]}
   'Tree
     {'app3     `[[this# ts# t2#] (app3deep t2# ts# this#)]
      'app3deep `[[_# ts# t1#]
                   (deep (.pre t1#)
                         (app3 (.mid t1#)
                               (nodes ~'meter-obj (concat (.suf t1#) ts# ~'pre))
                               ~'mid)
                         ~'suf)]
      'measureMore `[[this#] (measured3 ~'meter-obj (rest ~'pre) ~'mid ~'suf)]
      'measurePop  `[[this#] (measured3 ~'meter-obj ~'pre ~'mid (pop ~'suf))]}})

(defn ^:static newDeepTree [meter-obj pre mid suf mval]
  (DeepTree. meter-obj pre mid suf mval))

(defn deep [pre m suf]
  (let [meter-obj (getMeter pre)
        op (opfn meter-obj)]
    (newDeepTree meter-obj pre m suf
                 (when op
                   (delay (if (seq m)
                            (measured3 meter-obj pre m suf)
                            (op (measured pre) (measured suf))))))))

(deftype-compatible #?(:clj :clj :cljs :cljs) 'CountedDoubleList '[tree mdata]
  {'?Object
      {'equals    `[[_# x#] (seq-equals ~'tree x#)]
       'hash      `[[this#] (hashcode (map identity this#))]}
   '?HashEq
      {'hash-eq   `[[this#] (hash-ordered this#)]}
   '?Meta
      {'meta      `[[_#]    ~'mdata]
       'with-meta `[[_# mdata#] (CountedDoubleList. ~'tree mdata#)]}
   '?Sequential true
   '?Seqable
      {'seq   `[[this#] (when (seq ~'tree) this#)]}
   '?Seq
      {'first `[[_#] (first ~'tree)]
       'rest  `[[_#] (CountedDoubleList. (rest ~'tree) ~'mdata)]
       'next  `[[_#] (if-let [t# (next ~'tree)] (CountedDoubleList. t# ~'mdata))]}
   '?Stack ; actually, queue
      {'peek  `[[_#] (peek ~'tree)  ]
       'pop   `[[_#] (CountedDoubleList. (pop ~'tree) ~'mdata)]}
   '?Reversible
      {'rseq  `[[_#] (rseq ~'tree)]} ; not 'this' because tree ops can't be reversed
   '?Counted
      {'count `[[_#]    (measured ~'tree)]}
   '?Collection
      {'empty `[[_#]    (CountedDoubleList. (empty ~'tree) ~'mdata)]
       'equiv `[[_# x#] (seq-equals ~'tree x#)] ; TBD
       'conj  `[[_# x#] (CountedDoubleList. (conj ~'tree x#) ~'mdata)]}
   '?Associative
      {'assoc       `[[this# k# v#]
                      (cond
                        (== k# -1) (conjl this# v#)
                        (== k# (measured ~'tree)) (conj this# v#)
                        (< -1 k# (measured ~'tree))
                          (let [[pre# mid# post#] (split-tree ~'tree #(> % k#))]
                            (CountedDoubleList. (ft-concat (conj pre# v#) post#) ~'mdata))
                        :else (throw (IndexOutOfBoundsException.)))]
       ; clj containsKey
       'contains? `[[_# k#] (< -1 k# (measured ~'tree))]
       ; clj entryAt
       'entry-at  `[[_# n#] (map-entry
                              n# (second (split-tree ~'tree #(> % n#))))]}
   '?Lookup
    {'val-at `[([this# n# notfound#]
                 (if (.containsKey this# n#)
                     (second (split-tree ~'tree #(> % n#)))
                     notfound#))
               ([this# n#] (.valAt this# n# nil))]}
   '?Indexed
      {'nth `[([this# n# notfound#]
                (if (.containsKey this# n#)
                    (second (split-tree ~'tree #(> % n#)))
                    notfound#))
              ([this# n#]
                (if (.containsKey this# n#)
                    (second (split-tree ~'tree #(> % n#)))
                    (throw (IndexOutOfBoundsException.))))]}
   'Iterable
      {'iterator  `[[this#] (clojure.lang.SeqIterator. (seq this#))]}
   'ConjL
      {'conjl     `[[_# a#] (CountedDoubleList. (conjl ~'tree a#) ~'mdata)]}
   'Measured
      {'measured  `[[_#] (measured ~'tree)]
       'getMeter  `[[_#] (getMeter ~'tree)]} ; not needed?
   ; Splittable
   ;   (split [pred acc]) ; TBD -- not needed??
   'Tree
      {'app3        `[[_# ts# t2#] (CountedDoubleList. (app3     ~'tree ts# (.tree t2#)) ~'mdata)]
       'app3deep    `[[_# ts# t1#] (throw (#?(:clj Exception. :cljs js/Error.)
                                           "Not implemented"))]
       'measureMore `[[_#] (measureMore ~'tree)]
       'measurePop  `[[_#] (measurePop ~'tree)]}
   'SplitAt
      {'ft-split-at `[([this# n# notfound#]
                         (cond
                           (< n# 0) [(empty this#) notfound# this#]
                           (< n# (count this#))
                             (let [[pre# m# post#] (split-tree# ~'tree #(> % n))]
                               [(CountedDoubleList. pre# ~'mdata) m# (CountedDoubleList. post# ~'mdata)])
                           :else [this# notfound# (empty this#)]))
                       ([this# n#]
                         (ft-split-at this# n# nil))]}})

(def- measure-len (constantly 1))
(def- len-meter (meter measure-len 0 +))
(def empty-counted-double-list
  (CountedDoubleList. (EmptyTree. len-meter) nil))

(defn counted-double-list [& args]
  (into empty-counted-double-list args))




; (defprotocol PrintableTree
;   (print-tree [tree]))

; (defn- p [t & xs]
;   (print "<")
;   (print t)
;   (doseq [x xs]
;     (print " ")
;     (print-tree x))
;   (print ">"))

; (extend-protocol PrintableTree
;   Digit1      (print-tree [x] (p "Digit1" (.a x)))
;   Digit2      (print-tree [x] (p "Digit2" (.a x) (.b x)))
;   Digit3      (print-tree [x] (p "Digit3" (.a x) (.b x) (.c x)))
;   Digit4      (print-tree [x] (p "Digit4" (.a x) (.b x) (.c x) (.d x)))
;   EmptyTree   (print-tree [x] (p "EmptyTree"))
;   DelayedTree (print-tree [x] (p "DelayedTree" @(.tree-ref x)))
;   DeepTree    (print-tree [x] (p "DeepTree" (.pre x) (.mid x) (.suf x)))
;   SingleTree  (print-tree [x] (p "SingleTree" (.x x)))
;   object      (print-tree [x] (pr x)))