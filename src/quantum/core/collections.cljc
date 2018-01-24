(ns quantum.core.collections
  "Various collections functions.

   Includes better versions of the following than clojure.core:

   for, doseq, repeat, repeatedly, range, merge,
   count, vec, reduce, into, first, second, rest,
   last, butlast, get, pop, peek ...

   and more.

   Many of them are aliased from other namespaces like
   quantum.core.collections.core, or quantum.core.reducers."
  (:refer-clojure :exclude
    [for doseq reduce transduce dotimes
     contains?
     repeat repeatedly
     interpose mapcat cat
     reductions
     group-by frequencies
     range
     set
     map pmap map-indexed
     partition
     partition-all
     remove filter
     take take-while
     drop  drop-while
     key val
     merge sorted-map sorted-map-by
     into
     count
     vec empty empty?
     every? some not-every? not-any?
     split-at
     first second rest last butlast get nth pop peek
     select-keys get-in
     zipmap
     reverse rseq
     conj
     conj! assoc assoc! assoc-in dissoc dissoc! disj! update
     boolean?
     class
     deref])
  (:require
    [clojure.core                            :as c      ]
    [fast-zip.core                           :as zip    ]
    [quantum.core.data.map                   :as map
      :refer [!hash-map]]
    [quantum.core.data.set                   :as set    ]
    [quantum.core.data.string
      :refer [!str]]
    [quantum.core.data.vector                :as vec
      :refer [catvec subsvec !vector]]
    [quantum.core.collections.core           :as coll]
    [quantum.core.collections.sociative      :as soc    ]
    [quantum.core.collections.differential   :as diff
      :include-macros true                              ]
    [quantum.core.collections.generative     :as gen    ]
    [quantum.core.collections.map-filter     :as mf     ]
    [quantum.core.collections.selective      :as sel    ]
    [quantum.core.collections.tree           :as tree   ]
    [quantum.core.collections.zippers        :as qzip   ]
    [quantum.core.collections.logic          :as clog   ]
    [quantum.core.core                       :as qcore
      :refer [>object]]
    [quantum.core.error                      :as err
      :refer [->ex TODO]]
    [quantum.core.fn                         :as fn
      :refer [rfn juxt-kv withf->> firsta aritoid
              rcomp conja <- fn-> fn->> fn1 fnl fn' fn&2]]
    [quantum.core.log                        :as log
      :refer [prl!]]
    [quantum.core.logic                      :as logic
      :refer [fn-not fn-or fn-and, fn-nil
              whenf whenf1, ifn ifn1, condf condf1
              splice-or]]
    [quantum.core.macros                     :as macros
      :refer [defnt case-env]]
    [quantum.core.ns                         :as ns]
    [quantum.core.numeric                    :as num]
    [quantum.core.numeric.convert            :as nconv]
    [quantum.core.reducers                   :as red
      :refer [defeager]]
    [quantum.core.refs                       :as refs
      :refer [setm! swapm! deref !ref]]
    [quantum.core.string                     :as str    ]
    [quantum.core.string.format              :as sform  ]
    [quantum.core.type                       :as t
      :refer [lseq? transient? editable?
              boolean? should-transientize?
              class]]
    [quantum.core.analyze.clojure.predicates :as anap]
    [quantum.core.loops                      :as loops]
    [quantum.core.vars                       :as var
      :refer [defalias defaliases]]
    [quantum.untyped.core.data               :as udata])
#?(:cljs
  (:require-macros
    [quantum.core.collections
      :refer [for for* lfor doseq doseqi reduce reducei dotimes
              count lasti
              subview subview-range
              contains? containsk? containsv?
              index-of last-index-of
              first second rest last butlast get pop peek nth
              conjl conj! assoc assoc! assoc?! dissoc dissoc! disj!
              map-entry join empty? empty update update! empty? elem->array]]))
  #?(:clj  (:import java.util.Comparator quantum.core.refs.MutableReference)
     :cljs (:import goog.string.StringBuffer)))

(defalias val? quantum.core.type/val?)

#?(:clj
(defmacro getf
  "Get field"
  [x field]
  (let [accessor (symbol
                   (str "."
                     (case-env :clj  (str "get" (str/upper-first (name field)))
                               :cljs (str "-" (name field)))))]
    `(~accessor ~x))))

#?(:clj
(defmacro setf!
  "Set field"
  [x field v]
  (let [accessor (symbol
                   (str "."
                     (case-env :clj  (str "set" (str/upper-first (name field)))
                               :cljs (str "-" (name field)))))]
    (case-env :cljs `(set! (~accessor ~x) ~v)
                    `(~accessor ~x ~v)))))

(defaliases clog
  seq-or some seq-nor not-any? seq-and every? seq-nand not-every? apply-and apply-or)

; KV ;

; `reverse` <~> `lodash/reverse`
(defaliases coll key val reverse rseq)

(#?(:clj definline :cljs defn) map-entry
  "Creates a map entry from a and b."
  [a b]
  #?(:clj  (clojure.lang.MapEntry. a b)
     :cljs [a b]))

(defalias pair map-entry)

(defn genkeyword
  ([]    (keyword (gensym)))
  ([arg] (keyword (gensym arg))))

(defn wrap-delay [f]
  (if (delay? f) f (delay ((or f fn-nil)))))

; ====== COLLECTIONS ====== ;

; ===== SINGLETON RETRIEVAL ===== ;
; `get` <~> `lodash/nth` ; TODO `nth` negative returns elem from the end
#?(:clj (defalias get             coll/get          ))
#?(:clj (defalias get&            coll/get&         ))
#?(:clj (defalias nth             coll/nth          ))
#?(:clj (defalias peek            coll/peek         ))
; `first` <~> `lodash/head`
#?(:clj (defalias first           coll/first        ))
#?(:clj (defalias firstl          coll/firstl       ))
#?(:clj (defalias firstr          coll/firstr       ))
#?(:clj (defalias second          coll/second       ))
        (defalias third           coll/third        )
; `last` <~> `lodash/last`
#?(:clj (defalias last            coll/last         ))
#?(:clj (defalias last&           coll/last&        ))
; ===== BULK RETRIEVAL ===== ;
; `rest` <~> `lodash/tail`
#?(:clj (defalias rest            coll/rest         ))
        (defalias lrest           c/rest            )
; `butlast` <~> `lodash/initial`
#?(:clj (defalias butlast         coll/butlast      ))
; `slice` <~> `lodash/slice`
#?(:clj (defalias slice           coll/slice        ))
        (defalias subview         coll/subview      )
        (defalias subview-range   coll/subview-range)
#?(:clj (defalias copy            coll/copy         ))
; ===== ASSOCIATIVE MODIFICATION ===== ;
#?(:clj (defalias assoc           coll/assoc        ))
#?(:clj (defalias assoc!          coll/assoc!       ))
#?(:clj (defalias assoc!&         coll/assoc!&      ))
#?(:clj (defalias assoc?!         coll/assoc?!      ))
#?(:clj (defalias dissoc          coll/dissoc       ))
#?(:clj (defalias dissoc!         coll/dissoc!      ))
        (defalias conj            coll/conj         )
#?(:clj (defalias conj!           coll/conj!        ))
#?(:clj (defalias conj?!          coll/conj?!       ))
#?(:clj (defalias disj!           coll/disj!        ))
#?(:clj (defalias update!         coll/update!      ))
#?(:clj (defalias aswap!          coll/aswap!       ))

(defaliases soc
  assoc-extend assoc-in  assoc-default assoc-with assoc-if re-assoc
               dissoc-in
  update update-val                               update-when updates        )

; ===== ENDIAN MODIFICATION ===== ;
#?(:clj (defalias conjl         coll/conjl        ))
#?(:clj (defalias conjr         coll/conjr        ))
#?(:clj (defalias pop           coll/pop          ))
#?(:clj (defalias popl          coll/popl         ))
#?(:clj (defalias popr          coll/popr         ))
; ===== MODIFICATION ===== ;
#?(:clj (defalias copy!         coll/copy!        ))
; ===== CONTAINMENT PREDICATES ===== ;
#?(:clj (defalias contains?     coll/contains?    ))
#?(:clj (defalias containsk?    coll/containsk?   ))
#?(:clj (defalias containsv?    coll/containsv?   ))
; `index-of` <~> `lodash/indexOf`
; TODO `sorted-index-of` <~> `lodash/sortedIndexOf`
#?(:clj (defalias index-of      coll/index-of     ))
; `last-index-of` <~> `lodash/lastIndexOf`
; TODO `sorted-last-index-of` <~> `lodash/sortedLastIndexOf`
#?(:clj (defalias last-index-of coll/last-index-of))
; `index-of-pred` <~> `lodash/findIndex`
#?(:clj (defalias index-of-pred      diff/index-of-pred     ))
; `last-index-of-pred` <~> `lodash/findLastIndex`
#?(:clj (defalias last-index-of-pred diff/last-index-of-pred))
; ===== SIZE + INDICES ===== ;
#?(:clj (defalias empty?        coll/empty?       ))

        (defalias count:rf      coll/count:rf     )
#?(:clj (defalias count         coll/count        ))
#?(:clj (defalias count&        coll/count&       ))

#?(:clj (defalias lasti         coll/lasti        ))
#?(:clj (defalias lasti&        coll/lasti&       ))
; ===== CREATION ===== ;
#?(:clj (defalias empty         coll/empty        ))
#?(:clj (defalias empty&        coll/empty&       ))
#?(:clj (defalias blank         coll/blank        ))
#?(:clj (defalias array         coll/array        ))
#?(:clj (defalias array-of-type coll/array-of-type))
#?(:clj (defalias ->vec         coll/->vec        ))

#?(:clj (defalias ?transient!   coll/?transient!  ))
#?(:clj (defalias ?persistent!  coll/?persistent! ))

; ===== CONCATENATION ===== ;
#?(:clj (defalias join          red/join          ))
#?(:clj (defalias join!         coll/join!        ))
#?(:clj (defalias join?!        coll/join?!       ))
#?(:clj (defalias joinl         red/join          ))
#?(:clj (defalias join'         red/join'         ))
#?(:clj (defalias joinl'        red/joinl'        ))
        (defalias pjoin         red/pjoin         )
        (defalias pjoinl        red/pjoin         )
; ===== REDUCTION ===== ;
        (defalias red-apply     red/red-apply     )
        (defalias fold          red/fold*         )
        (defalias foldcat+      red/foldcat+      )

        (defnt into! ; TODO delete
          "Like into, but for mutable collections"
          [^transient? x coll] (loops/doseq [elem coll] (conj! x elem)) x)

        ; `take` <~> `lodash/take`
        (defalias take                diff/takel              )
        (defalias take+               diff/take+              )
        (defalias ltake               diff/ltake              )
        (defalias takel               diff/takel              )
        (defalias takel+              take+                   )
        ; `taker` <~> `lodash/takeRight`
        (defalias taker               diff/taker              )
#?(:clj (defalias taker+              diff/taker+             ))
        (defalias take-nth+           diff/take-nth+          )
        (defalias takel-nth+          diff/takel-nth+         )
        ; `take-while` <~> `lodash/takeWhile`
        (defalias take-while          diff/take-while         )
        (defalias take-while+         diff/take-while+        )
        (defalias take-after          diff/take-after         )
        (defalias takel-while+        take-while+             )
        (defalias takel-after         diff/takel-after        )
        (defalias takel-after-matches diff/takel-after-matches)
        (defalias taker-after         diff/taker-after        )
        (defalias take-until          diff/take-until         )
        (defalias takel-until-matches diff/takel-until-matches)
#?(:clj (defalias taker-until         diff/taker-until        ))
        ; TODO ; `taker-while` <~> `lodash/takeRightWhile`

        ; `drop` <~> `lodash/drop`
        (defalias drop                diff/drop               )
        (defalias drop+               diff/drop+              )
        (defalias dropl               diff/dropl              )
        (defalias ldropl              diff/ldropl             )
        (defalias ldrop               diff/ldropl             )
        ; `drop-while` <~> `lodash/dropWhile`
        (defalias drop-while+         red/drop-while+         )
        ; `dropr` <~> `lodash/dropRight`
        (defalias dropr               diff/dropr              )
#?(:clj (defalias dropr+              diff/dropr+             ))
        (defalias dropr-until         diff/dropr-until        )
        ; `dropr-while` <~> `lodash/dropRightWhile`
        (defalias dropr-while         diff/dropr-while        )
        (defalias dropr-while-matches diff/dropr-while-matches)
        (defalias ldrop-at            diff/ldrop-at           )

        (defalias group-by+           red/group-by+           )
        ; `flatten` <~> `lodash/flattenDeep`
        (defalias flatten+            red/flatten+            )
        (defalias cat+                red/cat+                )
        (def      lcat                (partial apply concat)  )
        ; `cat` <~> `lodash/flatten`
        (def      cat                 (fn->> cat+ join)       )
        ; TODO  `flatten-n` <~> `lodash/flattenDepth`
        (defalias iterate+            red/iterate+            )
        (defalias reduce-by+          red/reduce-by+          )

        (defalias distinct-storing+   red/distinct-storing+   )
        (defalias distinct-by-storing+ red/distinct-by-storing+)
        ; `distinct` <~> `lodash/uniq`
        (defalias distinct+           red/distinct+           )
        ; `distinct-by` <~> `lodash/uniqBy`
        (defalias distinct-by+        red/distinct-by+        )
        (defalias ldistinct-by        mf/ldistinct-by         )
#?(:clj (defalias ldistinct-by-java   mf/ldistinct-by-java    ))
        (defalias dedupe+             red/dedupe+             )
      #_(defalias dedupe-by+          red/dedupe-by+          )
        (defalias v!dedupe+           red/v!dedupe+           )
        (defalias v!dedupe-by+        red/v!dedupe-by+        )

        (defalias replace+            red/replace+            )
        (defalias partition-by+       red/partition-by+       )
        (defalias interpose+          red/interpose+          )
        (defalias zipvec+             red/zipvec+             )
        (defalias random-sample+      red/random-sample+      )
        (defalias sample+             red/sample+             )
        (defalias reduce-count        red/reduce-count        )
        (defalias reduce-sentinel     red/reduce-sentinel     )

#?(:clj (defaliases coll
           elem->array
           ->booleans
           ->bytes
           ->ubytes
           ->ubytes-clamped
           ->chars
           ->shorts
           ->ushorts
           ->ints
           ->uints
           ->longs
           ->floats
           ->doubles
           ->objects
           ->array))

; _______________________________________________________________
; ============================ LOOPS ============================
; •••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••
#?(:clj (defalias dotimes           loops/dotimes          ))
#?(:clj (defalias fortimes          loops/fortimes         ))
#?(:clj (defalias fortimes:objects  loops/fortimes:objects ))
#?(:clj (defalias fortimes:objects2 loops/fortimes:objects2))
#?(:clj (defalias fortimes:doubles  loops/fortimes:doubles ))
#?(:clj (defalias fortimes:doubles2 loops/fortimes:doubles2))
#?(:clj (defalias fortimes:doubles3 loops/fortimes:doubles3))
#?(:clj (defalias transduce    red/transduce ))
#?(:clj (defalias reduce       loops/reduce  ))
#?(:clj (defalias reducei      loops/reducei ))
#?(:clj (defalias reduce*      loops/reduce* ))
#?(:clj (defalias reduce-multi loops/reduce-multi))
#?(:clj (defalias red-for      loops/red-for ))
#?(:clj (defalias red-fori     loops/red-fori))
        (defalias reduce-pair  loops/reduce-pair)
        (defalias reduce-2     loops/reduce-2 )
        (defalias reducei-2    loops/reducei-2)
#?(:clj (defalias reduce-2:indexed loops/reduce-2:indexed))
#?(:clj (defalias ifor         loops/ifor    ))
#?(:clj (defalias ifori        loops/ifori   ))
; ===== COLLECTION COMPREHENSION ===== ;
#?(:clj (defalias for-join     loops/for-join  ))
#?(:clj (defalias for-join!    loops/for-join! ))
#?(:clj (defalias for          loops/for       )) #?(:clj (alter-meta! (var for) c/assoc :macro true))
#?(:clj (defalias for'         loops/for'      ))
#?(:clj (defalias for+         red/for+        ))
#?(:clj (defalias fori         loops/fori      ))
#?(:clj (defalias fori'        loops/fori'     ))
#?(:clj (defalias fori+        red/fori+       ))
#?(:clj (defalias fori-join    loops/fori-join ))
#?(:clj (defalias fori-join!   loops/fori-join!))
#?(:clj (defmacro lfor [& args] `(loops/lfor   ~@args)))

#?(:clj (defmacro doseq  [& args] `(loops/doseq  ~@args)))
#?(:clj (defmacro doseqi [& args] `(loops/doseqi ~@args)))
#?(:clj (defalias until     loops/until   ))
#?(:clj (defalias while-let loops/while-let))
#?(:clj (defalias doeach    loops/doeach))
#?(:clj (defalias each      loops/each))
#?(:clj (defalias eachi     loops/eachi))
#?(:clj (defalias doreduce  loops/doreduce))
        (defalias mapfn     loops/mapfn)
        (defalias break     reduced)
; _______________________________________________________________
; ========================= GENERATIVE ==========================
; •••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••
(defaliases gen
  repeat lrepeat repeat+, repeatedly lrepeatedly repeatedly+
  range #?(:clj range+) lrange rrange lrrange
  #?(:clj !range:longs) #?(:clj !range:longs&))
; _______________________________________________________________
; ================== FULL-SEQUENCE TRANSFORMS ===================
; •••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••
(defaliases mf
  map map' map+ lmap
  map-indexed map-indexed' map-indexed+ v!map-indexed+ !map-indexed+ lmap-indexed
  map-keys map-keys' map-keys+ lmap-keys
  map-vals map-vals' map-vals+ lmap-vals
  #?@(:clj [pmap pmap' pmap-indexed pmap-indexed'])
  ; `(filter identity xs)` <~> `lodash/compact`
  filter filter' filter+ lfilter
  filter-indexed filter-indexed' filter-indexed+ v!filter-indexed+ !filter-indexed+
  ffilter ffilteri last-filteri
  filter-keys filter-keys' filter-keys+ filter-keys
  filter-vals filter-vals' filter-vals+ filter-vals
  #?@(:clj [pfilter pfilter'])
  ; `(remove! #{vs...} xs)` <~> `lodash/pullAll`
  ; `(remove! pred xs)` <~> `lodash/remove`
  remove remove' remove+ lremove
  remove-indexed remove-indexed' remove-indexed+ v!remove-indexed+ !remove-indexed+
  remove-keys remove-keys' remove-keys+ lremove-keys
  remove-vals remove-vals' remove-vals+ lremove-vals
  #?@(:clj [premove premove']))

        (defalias indexed+      red/indexed+      )
        (defn indexed
          "Returns a sequence of pairs of index and item."
          [coll] (map-indexed pair coll))
        (defn indexed' [coll] (map-indexed' pair coll))
        (defn lindexed [coll] (lmap-indexed pair coll))

        (defalias keep+           red/keep+          )
        (defalias keep-indexed+   red/keep-indexed+  )
        (defeager mapcat          red/mapcat+        )
        (defalias remove-surrounding diff/remove-surrounding)
        (defalias lreductions c/reductions)
        (defn reductions
          [f init coll]
          (persistent!
            (reduce (fn [ret in] (conj! ret (f (last ret) in)))
                    (conj! (transient []) init)
                    coll)))
        ; `partition-all` <~> `lodash/chunk`
        ; TODO choose what structures to partition into
        (defeager partition-all   red/partition-all+ )
        (defalias !partition-all-into+ red/!partition-all-into+)
        (defalias partition-all-timeout+  red/partition-all-timeout+)
        (defalias !partition-all-timeout+ red/!partition-all-timeout+)
        (defalias lpartition      c/partition        )
        (defn each+ [f xs] (->> xs (map+ #(do (f %) %))))

(defn gets+ [xs indices] (->> indices (map+ #(get xs %))))
(defn gets  [xs indices] (->> (gets+ xs indices) (join [])))
; _______________________________________________________________
; ============================ TREE =============================
; •••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••
(defaliases tree
  walk prewalk         postwalk
       prewalk-filter  postwalk-filter
       prewalk-replace postwalk-replace
       prewalk-find    postwalk-find
  apply-to-keys)

(defn comp-depth
  "If there are no branches, the depth is 0."
  ([<branch? <children tree compf]
    (if (<branch? tree)
        (comp-depth <branch? <children tree compf 1)
        0))
  ([<branch? <children tree compf depth]
    (if (<branch? tree)
        (->> tree <children
             (map+ #(comp-depth <branch? <children % compf (inc depth)))
             (reduce-sentinel compf)
             (<- or depth))
        depth)))

(def max-depth (conja comp-depth max))
#_(def min-depth (conja comp-depth min)) ; this is not useful

; _______________________________________________________________
; ======================== COMBINATIVE ==========================
; •••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••
        (defalias zipmap          c/zipmap           )
        (defalias merge           map/merge          )

(defn into-nd-array ; TODO make an unchecked version
  ([arr xs] (TODO))
  ([arr xs dims] (into-nd-array arr xs dims []))
  ([arr xs dims indices]
    (err/assert (t/sequential? xs) {:type (type xs)})
    (if (-> xs first sequential?)
        (do (err/assert (= (count xs) (get dims (count indices))))
            (reducei (fn [_ xs-inner i]
                       (into-nd-array arr xs-inner dims (conj indices i)))
                     arr
                     xs))
        (do (err/assert (= (count xs) (get dims (count indices))))
            (reducei (fn [_ x i] (apply coll/assoc-in!*-protocol arr x (conj indices i))) arr xs)))))

(defn seq->array-nd-of-dims
  [xs elem dims]
  (into-nd-array (apply coll/elem->array-protocol elem dims) xs dims))

(defn seq->elem+dims
  ([xs] (seq->elem+dims xs []))
  ([xs dims]
    (err/assert (t/sequential? xs) {:type (type xs)})
    (if (-> xs first t/sequential?)
        (seq->elem+dims (first xs) (conj dims (count xs)))
        (let [types (->> xs (map+ type) (join #{}))
              elem  (if (-> types count (not= 1))
                        (>object)
                        (first xs))]
          {:elem elem :dims (conj dims (count xs))}))))

(defn ->array-nd
  "Creates an n-dimensional array from nested seqs."
  ([xs]
    (let [{:keys [elem dims]} (seq->elem+dims xs)]
      (seq->array-nd-of-dims xs elem dims))))

#?(:clj
(defn array->dimensionality
  "e.g. an array with the '[[[J' tag would be of 3 dimensionality."
  [arr] ; TODO ensure that it's an array
  (->> arr type str (drop+ 6) (take-while+ (fn1 = \[)) count)))

#?(:clj
(defnt array->array-manager-key
  ([^boolean? x] :boolean)
  ([^byte?    x] :byte   )
  ([^char?    x] :char   )
  ([^short?   x] :short  )
  ([^int?     x] :int    )
  ([^long?    x] :long   )
  ([^float?   x] :float  )
  ([^double?  x] :double )))

#?(:clj (def array-managers @#'c/ams))

#?(:clj
(defnt array->vector*
  ([^objects? arr] (c/vec arr)) ; TODO faster?
  ([#{booleans? bytes? chars? shorts? ints? longs? floats? doubles?} arr]
   (let [t  (array->array-manager-key (get arr 0))
         ^clojure.core.ArrayManager am (get array-managers t)
         ct (int (count arr))
         first-few-i (min 4 ct)
         arr-0 (.array am first-few-i)]
     (dotimes [i first-few-i]
       (.aset am arr-0 i (get arr (int i))))
     (let [v (clojure.core.Vec. am first-few-i 5 EMPTY-NODE arr-0 nil)]
       (if (<= ct 4)
           v
           (loop [v' v
                  i' (int 4)]
             (if (>= i' ct)
                 v'
                 (recur (conj v' (get arr (int i'))) (inc i'))))))))))

#?(:clj
(defn array->vector
  "Recursively transforms an n-dimensional array into an
   n-dimensional vector of vectors.
   Preserves primitiveness, where relevant, via |vector-of|."
  ([curr-dim arr]
    (if (<= curr-dim 1)
        (when arr (array->vector* arr))
        (let [ret (transient [])]
          (dotimes [i (count arr)]
            (conj! ret (array->vector (dec curr-dim) (get arr i))))
          (persistent! ret))))
  ([arr]
    (let [dim (-> arr array->dimensionality)]
      (array->vector dim arr)))))

(defn indexed->map [xs] (->> xs indexed+ (join {})))

(defnt reduce-count-bounded
  ([^default x n pred] ; TODO infer ; for this overload, reducible, non-counted
    (let [ret (->> x (reduce (fn [ct' _] (if (<= n ct') (reduced false) (inc ct'))) 0))]
      (if (false? ret) ret (pred ret n)))))

(defnt count=*
  ([^default x n] ; TODO infer ; for this overload, reducible, non-counted
    (reduce-count-bounded x n =)))

(defmacro count= [n x] `(count=* ~x ~n))

(defnt count<*
  ([^default x n] ; TODO infer ; for this overload, reducible, non-counted
    (reduce-count-bounded x n <)))

(defmacro count< [n x] `(count<* ~x ~n))

(defnt count<=*
  ([^default x n] ; TODO infer ; for this overload, reducible, non-counted
    (reduce-count-bounded x n <=)))

(defmacro count<= [n x] `(count<=* ~x ~n))

(defnt padr!
  "Pad right, mutable."
  ([^!string? x n    ] (padr! x n \space))
  ([^!string? x n add]
    (dotimes [_ (dec (- n (lasti x)))] (conj! x add))
    x))

(defnt padr
  "Pad right until length `n` is met."
  ([^string? x n    ]      (padr        x  n \space))
  ([^string? x n add] (str (padr! (!str x) n add))))

(defn merge-with-set [m1 m2]
  (merge-with (fn [v1 v2] (if (set? v1)
                              (if (set? v2)
                                  (set/union v1 v2)
                                  (conj v1 v2))
                              (if (set? v2)
                                  (conj v2 v1)
                                  #{v1 v2}))) m1 m2))

(defn compact-map
  "Removes all map entries where the value of the entry is empty."
  {:from "r0man/noencore"}
  [m]
  (reduce
   (fn [m k]
     (let [v (get m k)]
       (if (or (nil? v)
               (and (or (t/+map? v)
                        (c/sequential? v))
                    (empty? v)))
         (dissoc m k) m)))
   m (keys m)))

; TODO extract code pattern for lazy transformation
(defn lflatten
  "Like #(apply concat %), but fully lazy: it evaluates each sublist
   only when it is needed."
  {:from "clojure.algo.monads"}
  [ss]
  (lazy-seq
    (when-let [s (seq ss)]
      (concat (first s) (lflatten (rest s))))))

; TODO remove `concatv`
(defalias concatv catvec)
; `concat` <~> `lodash/concat`
; TODO `lconcat` -> `ljoin`
(defalias lconcat c/concat)

; `zip` <~> `lodash/zip`
; TODO `zip-with` <~> `lodash/zipWith`
(defn lzip [& args] (->> args (apply interleave) (lpartition-all (count args))))
(defn zip  [& args] (->> args (apply interleave) (partition-all  (count args))))

; `dezip` <~> `lodash/unzip`
; TODO `dezip-with` <~> `lodash/unzipWith`
(defn dezip
  "The inverse of zip. — Unravels a seq of m n-tuples into a
  n-tuple of seqs of length m.
  Example:
    (dezip '([11 12] [21 22] [31 32] [41 42]))
      ;=> [(11 21 31 41) (12 22 32 42)]
  Umm, actually there is no zip in Clojure. Instead, you'd use this:
    (apply map vector ['(11 21 31 41) '(12 22 32 42)])
      ;=> ([11 12] [21 22] [31 32] [41 42]))
  Note that I'm using lists here only for demonstrating that we're not limited
  to vectors."
  {:from "theatralia.database.txd-gen"}
  [s]
  (let [tuple-size (count (first s))
        s-seq (seq s)]
    (mapv (fn [n]
            (lmap #(nth % n) s-seq))
          (range tuple-size))))

(defn- flatten-map
  "Flatten a map into a seq of alternate keys and values"
  {:from "clojure.tools/reader"}
  [form]
  (loop [s (seq form) key-vals (transient [])]
    (if s
      (let [e (first s)]
        (recur (next s) (-> key-vals
                          (conj! (key e))
                          (conj! (val e)))))
      (seq (persistent! key-vals)))))

; ----- META ----- ;

(def unique-conj
  (rfn [ret k v]
    (if (contains? ret k)
        (throw (->ex "Duplicate key not allowed" {:k k}))
        (assoc ret k v))))

(defn butlast+last
  "Returns same value as (juxt butlast last), but slightly more
   efficient since it only traverses the input sequence s once, not
   twice."
  {:from "clojure/tools.analyzer/utils"}
  [s]
  (loop [butlast (transient [])
         s s]
    (if-let [xs (next s)]
      (recur (conj! butlast (first s)) xs)
      [(seq (persistent! butlast)) (first s)])))

(def mmerge
  "Same as (fn [m1 m2] (merge-with merge m2 m1))"
  #(merge-with merge %2 %1))

(defn mapv'
  "Like mapv, but short-circuits on reduced"
  {:from "clojure.tools.analyzer.utils"}
  [f v]
  (let [c (count v)]
    (loop [ret (transient []) i 0]
      (if (> c i)
        (let [val (f (nth v i))]
          (if (reduced? val)
            (reduced (persistent! (reduce #(conj! %1 %2) (conj! ret @val) (subvec v (inc i)))))
            (recur (conj! ret val) (inc i))))
        (persistent! ret)))))

(def frest (fn-> rest first))

; ================================================ INDEX-OF ================================================
(defn seq-contains?
  "Like |contains?|, but tests if a collection @super contains
   all the constituent elements of @sub, in the order in which they
   appear in @sub.

   A prime example would be substrings within strings."
  [super sub]
  (val? (index-of super sub)))

(defn indices-of-elem
  {:todo #{"Make parallizeable"
           "Transientize more elegantly"}}
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

(defn indices-of
  {:todo ["Make parallelizable"
          "|drop| is a performance killer here"]}
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

(defn indices-of-matches
  {:tests `{(indices-of-matches "1e  3 f" #(not= % \space))
            [[0 1] [4 4] [6 6]]}}
  [in pred]
  (let [i-max (long (lasti in))]
    (loop [accum     []
           i         0
           i-start   -1
           matching? false]
      (if (> i i-max)
          (if matching?
              (conj accum [i-start (dec i)])
              accum)
          (let [elem (get in i)]
            (if matching?
                (if (pred elem)
                    (recur accum                          (inc i) i-start matching?)
                    (recur (conj accum [i-start (dec i)]) (inc i) -1      false    ))
                (if (pred elem)
                    (recur accum                          (inc i) i       true     )
                    (recur accum                          (inc i) i-start matching?))))))))

(defn lindices-of
  "Lazy |indices-of|."
  {:source "zcaudate/hara.data.seq"}
  [pred coll]
  (keep-indexed (fn [i x] (when (pred x) i)) coll))

(defn matching-seqs
  {:tests `{[even? [1 2 3 4 4 2 1 2 2 6 8 2 4]]
            {1 [2], 3 [4 4 2], 7 [2 2 6 8 2 4]}}
   :todo ["Abstract; maybe use the regex for seqs"]}
  [pred coll]
  (loop [i       0
         coll'   coll
         match-i -1
         match   []
         matches {}]
    (if (empty? coll')
        (if (> match-i -1)
            (assoc matches match-i match)
            matches)
        (let [elem   (first coll')
              match? (pred elem)
              match' (if match?
                         (conj match elem)
                         [])
              end-match?   (and (contains? match )
                                (empty?    match'))
              start-match? (and (empty?    match )
                                (contains? match'))
              match-i'     (if start-match? i match-i)
              matches'
                (if (and end-match? (> match-i -1))
                    (assoc matches match-i match)
                    matches)]
        (recur (inc i)
               (rest coll')
               match-i'
               match'
               matches')))))

(defn indices+ [xs] (->> xs (map-indexed+ firsta)))

; ================================================ MERGE ================================================

(defn index-with [coll f]
  (->> coll
       (map+ #(map-entry (f %) %))
       (join {})))

(defn mergel  [a b] (merge b a))
(defalias merge-keep-left mergel)
(defn merger [a b] (merge a b))
(defalias merge-keep-right merger)

#?(:clj (defalias kw-map    udata/kw-map   ))
#?(:clj (defalias quote-map udata/quote-map))
#?(:clj (defalias kw-omap   map/kw-omap    ))

(defn select
  "Applies a list of functions, @fns, separately to an object, @coll.
   A good use case is returning values from an associative structure with keys as @fns.
   Returns a vector of the results."
  ^{:attribution "alexandergunnarson"
    :usage "(select {:a 1 :b [3]} :a (rcomp :b 0)) => [1 3]"}
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
          "Possibly belongs in a different namespace"]}
  [coll compare-fn]
  (loops/reducei
    (fn [ret elem n]
      (if (= n 0) elem (compare-fn ret elem)))
    nil
    coll))

;___________________________________________________________________________________________________________________________________
;=================================================={         LAZY SEQS        }=====================================================
;=================================================={                          }=====================================================
#?(:clj (defalias lseq lazy-seq))

#?(:clj
  (def lseq+
    (condf1
      (fn-or seq? nil? coll?) #(lseq %) ; not |partial|, because can't take value of a macro
      (fn-> list lseq first))))

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
  {:attribution 'amalloy/flatland.useful.seq #_"prismatic.plumbing"}
  [s]
  (lazy-seq
    (when-let [s (seq s)]
      (cons (first s)
            (unchunk (rest s))))))

#?(:clj
(defmacro lazy
  "Return a lazy sequence of the passed-in expressions. Each will be evaluated
  only if necessary."
  {:attribution 'amalloy/flatland.useful.seq}
  [& exprs]
  `(lmap force (list ~@(for [expr exprs] `(delay ~expr))))))
;___________________________________________________________________________________________________________________________________
;=================================================={  POSITION IN COLLECTION  }=====================================================
;=================================================={ first, rest, nth, get ...}=====================================================
(def fkey (fn-> first key))
(def fval (fn-> first val))

(defn up-val
  {:in '[{:a "ABC" :b 123} :a]
   :out '{"ABC" {:b 123}}
   :todo ["hash-map creation inefficient ATM"]}
  [m k]
  (hash-map
    (get m k)
    (-> m (dissoc k))))

(defn rename-keys [m-0 rename-m]
  (loops/reduce
    (fn [ret k-0 k-f]
      (-> ret
          (assoc  k-f (get ret k-0))
          (dissoc k-0)))
    m-0
    rename-m))

; ; /nthrest/
; ; (nthrest (range 10) 4) => (4 5 6 7 8 9)

; ===== GET-IN ===== ;

; TODO unify this

#?(:clj (defalias get-in* coll/get-in*))
#?(:clj (defalias get-in*& coll/get-in*&))
#?(:clj (defalias assoc-in!* coll/assoc-in!*))
#?(:clj (defalias assoc-in!*& coll/assoc-in!*&))

(def get-in-f* #(get %1 %2))

(defnt get-in ; TODO use `get-in` logic from clojure/core
  ([^array? x ks] (apply coll/get-in*-protocol x ks))
  ([        x ks] (reduce get-in-f* x ks)))

(defn assoc-in! ([x ks v] (assoc! (get-in x (butlast ks)) (last ks) v))) ; TODO is this the right behavior for all data structures?

(def single?
  "Does coll have only one element?"
  (fn-and contains? (fn-not next)))

; ===== ZIPPERS ===== ;

(defalias zipper          qzip/zipper         )
(defalias zip-mapv        qzip/zip-mapv       )
(defalias zip-map-with    qzip/zip-map-with   )
(defalias zip-walk        tree/zip-walk       )
(defalias zip-postwalk    tree/zip-postwalk   )
(defalias zip-prewalk     tree/zip-prewalk    )
(defalias zip-reduce      qzip/zip-reduce     )
(defalias zip-reduce-with qzip/zip-reduce-with)
;___________________________________________________________________________________________________________________________________
;=================================================={   ADDITIVE OPERATIONS    }=====================================================
;=================================================={    conj, cons, assoc     }=====================================================

;___________________________________________________________________________________________________________________________________
;=================================================={           MERGE          }=====================================================
;=================================================={      zipmap, zipvec      }=====================================================
(defn merge-with-k
  "Like merge-with, but the merging function takes the key being merged
   as the first argument"
   {:attribution  "prismatic.plumbing"
    :todo ["Make it not output HashMaps but preserve records"]
    :contributors ["Alex Gunnarson"]}
  [f & maps]
  (when (apply-or maps)
    (let [merge-entry
           (fn [m e]
             (let [k (key e) v (val e)]
               (if (containsk? m k)
                 (assoc m k (f k (get m k) v))
                 (assoc m k v))))
          merge2
            (fn ([] {})
                ([m1 m2]
                 (loops/reduce merge-entry (or m1 {}) (seq m2))))]
      (loops/reduce merge2 maps))))

(defn merge-vals-left
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
  [left right f]
  (persistent!
    (loops/reduce
      (fn [left-f k-right v-right]
       ;(if ((fn-not contains?) left-f k-right) ; can't call |contains?| on a transient, apparently...
       ;    left-f)
       (let [v-left (c/get left k-right)]
         (if (nil? v-left)
             left-f
             (let [merged-vs
                   (merge-with-k f v-left v-right)]
               (assoc! left-f k-right merged-vs)))))
      (transient left)
      right)))
;___________________________________________________________________________________________________________________________________
;=================================================={  FINDING IN COLLECTION   }=====================================================
;=================================================={  in?, index-of, find ... }=====================================================
(defalias in?          sel/in?         )
(defalias in-k?        sel/in-k?       )
(defalias in-v?        sel/in-v?       )
(defalias select-keys  sel/select-keys )
(defalias select-keys+ sel/select-keys+)
(defalias get-keys     sel/get-keys    )
(defalias get-key      sel/get-key     )
(defalias keys+        sel/keys+       )
(defalias vals+        sel/vals+       )
;___________________________________________________________________________________________________________________________________
;=================================================={     PARTITION, GROUP     }=====================================================
;=================================================={                          }=====================================================
(defn select-as+
  {:todo ["Name this function more appropriately"]
   :attribution "alexandergunnarson"
   :out 'Map}
  ([coll kfs]
    (->> (loops/reduce
           (fn [ret k f]
             (assoc ret k (f coll)))
           {}
           kfs)))
  ([coll k1 f1 & {:as kfs}]
    (select-as+ coll (assoc kfs k1 f1))))

(defn group-by-into
  "Like `group-by`, but you can choose what collection and subcollection to group into.
   `tf` is a function that transforms the element of the reducible to be sub-collected."
  ([init kf    xs] (group-by-into init kf (aritoid vector nil conj) xs))
  ([init kf rf xs]
    (->> xs
         (reduce
           (rfn [ret x]
             (let [k (kf x)]
               (assoc?! ret k (rf (or (get ret k) (rf)) x))))
           (?transient! init))
         ?persistent!)))

(defn group-by   [     kf xs] (group-by-into {}   kf     xs))
(defn group-into [init    xs] (group-by-into init identity xs))
(defn group      [        xs] (group-by-into {}   identity xs))

; ----- SPLIT ----- ;

(def ^{:doc "split the given collection at the given index; similar to
             clojure.core/split-at, but operates on and returns data.avl
             collections"}
  split-at clojure.data.avl/split-at)



(defn split-remove
  {:todo ["Slightly inefficient — two |index-of| implicit."]}
  [split-at-obj coll]
  (let [left  (take-until split-at-obj coll)] ; TODO need to make a non-predicate `take-until`
    (if (= left coll)
        [left]
        [left (take-after split-at-obj coll)])))

(defn split-into
  "Like `split`, but you can choose what subcollection to split into."
  ([pred gen-subinit xs]
    (->vec (group-by-into (->objects 2) (rcomp pred nconv/->boolean-num) (aritoid gen-subinit nil (fn&2 conj?!)) xs))))

(defn split
  "Splits `xs` into two groups:
   the first, which fails `pred`, and the second, which satisfies it."
  [pred xs] (split-into pred vector xs))

(defn split-remove-match
  {:todo ["Slightly inefficient — two |index-of| implicit."]
   :tests `{(split-remove-match "--" "ab--sddasd--")
            ["ab" "sddasd--"]}}
  [split-at-obj coll]
  (let [left (takel-until-matches split-at-obj coll)]
    (if (= left coll)
        [left]
        [left (takel-after-matches split-at-obj coll)])))
;___________________________________________________________________________________________________________________________________
;=================================================={   DISTINCT, INTERLEAVE   }=====================================================
;=================================================={  interpose, frequencies  }=====================================================
(defn duplicates-by
  {:attribution "alexandergunnarson"}
  [pred coll]
  (->> coll
       (group-by pred)  ; TODO use reducer group-by+
       (filter-vals+ (fn-> count (> 1)))
       (join {})))

; TODO: make a reducers version of coll/elem
(defnt interpose*
  ([^string? coll elem]
    (str/join elem coll))
  ([coll elem]
    (c/interpose elem coll)))

(defn interpose
  {:todo ["|definline| this"]}
  [elem coll] (interpose* coll elem))

(defn linterleave-all
  "Analogy: partition:partition-all :: interleave:interleave-all"
  {:attribution "prismatic/plumbing"}
  [& colls]
  (lazy-seq
   ((fn helper [seqs]
      (when (seq seqs)
        (concat (lmap #(first %1) seqs)
                (lazy-seq (helper (keep next seqs))))))
    (keep seq colls))))

(defn frequencies ; TODO model after group-by
  "Like `frequencies`, but uses reducers `reduce` and can choose
   what to reduce into."
  ([x] (frequencies {} x))
  ([to x]
    (->> x
         (transduce
           (fn ([] (?transient! to))
               ([ret] (?persistent! ret))
               ([counts x]
                 (assoc?! counts x (inc (or (get counts x) 0)))))))))

(defnt probabilities+
  ([ #_reducible? xs]
    (let [ct (count xs)] (->> xs (frequencies (!hash-map)) (map-vals+ (fn1 / ct))))))
;___________________________________________________________________________________________________________________________________
;=================================================={         GROUPING         }=====================================================
;=================================================={     group, aggregate     }=====================================================
(defn group-merge-with-k+
  {:attribution "alexandergunnarson"
   :todo ["Can probably make the |merge| process parallel."]
   :in [":a"
        "(fn [k v1 v2] v1)"
        "[{:a 1 :b 2} {:a 1 :b 5} {:a 5 :b 65}]"]
   :out "[{:b 65, :a 5} {:a 1, :b 2}]"
   :out-type 'Reducer}
  [group-by-f merge-with-f coll]
  (let [merge-like-elems
         (fn [grouped-elems]
           (if (single? grouped-elems)
               grouped-elems
               (loops/reduce
                 (fn [ret elem]
                   (merge-with-k merge-with-f ret elem))
                 (first grouped-elems)
                 (rest  grouped-elems))))]
    (->> coll
         (group-by+ group-by-f)
         (map+ val) ; [[{}] [{}{}{}]]
         (map+ merge-like-elems)
         flatten+)))

(defn first-uniques-by+ [k coll]
  (->> coll
       (group-by+ k)
       (map+ (update-val (fn1 first)))))

; ===== SORTING ===== ;

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
                  (lfilter smaller? xs)
                  pivot
                  (lremove smaller? xs)
                  parts)))
        (when-let [[x & parts] parts]
          (cons x (sort-parts parts)))))))

(defn lsort
  "Lazy 'quick'-sorting"
  {:attribution "The Joy of Clojure, 2nd ed."}
  [elems]
  (sort-parts (list elems)))

; TODO subarray-only sort

#?(:clj
(defnt heap-sort!
  [#{ints? floats? doubles? "[Ljava.lang.Comparable;"} arr]
  (doto arr (smile.sort.HeapSort/sort))))

#?(:clj
(defnt quicksort!
  "A comparison sort that, on average, makes O(n log n)
   comparisons to sort n items. For large n (say, > 1000), Quicksort is faster,
   on most machines, by a factor of 1.5 or 2 than other O(n log n) algorithms.
   However, in the worst case, it makes O(n^2) comparisons."
  {:todo ["Can also optionally sort only the first n elements of `arr`"]}
  [#{ints? floats? doubles? "[Ljava.lang.Comparable;"} arr]
  (doto arr (smile.sort.QuickSort/sort))))

#?(:clj
 (defnt dual-pivot-quicksort!
  "The Dual-Pivot Quicksort algorithm by Vladimir Yaroslavskiy,
   Jon Bentley, and Josh Bloch. The algorithm offers O(n log(n))
   performance on many data sets that cause other quicksorts to
   degrade to O(n^2) performance, and is typically faster than
   traditional (one-pivot) quicksort implementations."
  {:todo ["Only in more recent Java versions does Arrays/sort use
           Dual-Pivot Quicksort"
          "Can also sort a subarray"]}
  ([x] (TODO))))

#?(:clj
(defnt shell-sort!
  "A generalization of insertion sort, with two observations:
   - insertion sort is efficient if the input is \"almost sorted\", and
   - insertion sort is typically inefficient because it moves values
     just one position at a time.
   Shell sort improves insertion sort.
   The original implementation performs O(n^2) comparisons and
   exchanges in the worst case. A minor change given in V. Pratt's book
   improved the bound to O(n log.2(n)). This is worse than the
   optimal comparison sorts, which are O(n log n).
   For n < 50, roughly, Shell sort is competitive with the more complicated
   Quicksort on many machines. For n > 50, Quicksort is generally faster."
  {:todo ["Can also sort only the first n elements of `arr`"]}
  [#{ints? floats? doubles? "[Ljava.lang.Comparable;"} arr]
  (doto arr (smile.sort.ShellSort/sort))))

#?(:clj
(defnt tim-sort!
  "TimSort. A stable, adaptive, iterative mergesort that requires far
   fewer than nlog(n) comparisons when running on partially sorted arrays,
   while offering performance comparable to a traditional mergesort when
   run on random arrays. This sort is stable and runs O(n log n) time
   (worst case)."
  [#{ints? floats? doubles? objects?} arr]
  (TODO)))

#?(:clj
 (defnt sort!*
  "Uses whatever default sorting algorithm is available for the data
   structure passed.
   Defaults to ascending sort."
  {:todo ["Only in more recent Java versions does Arrays/sort use
           Dual-Pivot Quicksort"
          "Can also sort a subarray"]}
  ([#{bytes? shorts? chars? ints? longs? floats? doubles?} x]
    (doto x (java.util.Arrays/sort)))
  ([#{objects? !vector?} x] (sort!* x compare))
  ([#{!vector?}          x compf] (doto x (.sort ^Comparator compf)))
  ([#{objects?}       x compf]
    (doto x (java.util.Arrays/sort ^Comparator compf)))))

#?(:clj (defmacro sort! [& args] `(sort!* ~(last args) ~@(butlast args))))

#?(:clj
 (defnt sort-by!*
  "Uses whatever default sorting algorithm is available for the data
   structure passed.
   Defaults to ascending sort."
  {:todo ["Only in more recent Java versions does Arrays/sort use
           Dual-Pivot Quicksort"
          "Can also sort a subarray"]}
  ([#{bytes? shorts? chars? ints? longs? floats? doubles? objects? !vector?} x kf] ; TODO infer
    (sort-by!* x kf compare))
  ([#{bytes? shorts? chars? ints? longs? floats? doubles? objects? !vector?} x kf compf] ; TODO infer
    (sort!* x (fn [x y] (.compare ^Comparator compf (kf x) (kf y)))))))

#?(:clj (defmacro sort-by! [& args] `(sort-by!* ~(last args) ~@(butlast args))))

#?(:clj
(defnt psort!*
  "Parallel TimSort in Java 8."
  {:todo ["Can also sort a subarray"]}
  ([#{bytes? shorts? chars? ints? longs? floats? doubles?} arr]
    (doto arr (java.util.Arrays/parallelSort)))
  ([#{objects?} arr]
    (doto arr (java.util.Arrays/parallelSort ^Comparator compare)))
  ([#{objects?} arr compf]
    (doto arr (java.util.Arrays/parallelSort ^Comparator compf)))))

#?(:clj (defmacro psort! [& args] `(psort!* ~(last args) ~@(butlast args))))

#?(:clj
 (defnt psort-by!*
  "Defaults to ascending sort."
  ([#{bytes? shorts? chars? ints? longs? floats? doubles? objects?} x kf] ; TODO infer
    (psort-by!* x kf compare))
  ([#{bytes? shorts? chars? ints? longs? floats? doubles? objects?} x kf compf] ; TODO infer
    (psort!* x (fn [x y] (.compare ^Comparator compf (kf x) (kf y)))))))

#?(:clj (defmacro psort-by! [& args] `(psort-by!* ~(last args) ~@(butlast args))))

(defnt sorted
  "Coerces `x` to a compatible sorted collection."
  ([^sorted? x] x)
  ([#_(- map? sorted?) #{+hash-map? +array-map?} x] (join (map/sorted-map) x))
  ([#_(- set? sorted?) ^+hash-set? x] (join (sorted-set) x)))
; TODO `sorted-by`

; ===== SELECTION ===== ;

(defnt ^long select!:quick:left ; takes 13 seconds to compile because of type checks
  "A helper for `quick-select`. Also used by `intro-select`."
  {:adapted-from 'org.apache.lucene.util.IntroSelector}
  ([#{array? !vector?} xs ^long k #?(:clj ^Comparator compf :cljs compf) ^long from ^long to]
    (let [mid (unsigned-bit-shift-right #_>>> (+ from to) 1)
          ; heuristic: we use the median of the values at from, to-1 and mid as a pivot
          pivot (get xs from)
          _     (when (> (#?(:clj .compare) compf pivot (get xs (dec to))) 0)
                  (aswap! xs from (dec to)))
          pivot (get xs (dec to))
          pivot (if (> (#?(:clj .compare) compf pivot (get xs mid)) 0)
                    (do (aswap! xs (dec to) mid)
                        (let [pivot (get xs from)]
                          (when (> (#?(:clj .compare) compf pivot (get xs (dec to))) 0)
                            (aswap! xs from (dec to)))
                          pivot))
                    pivot)
          pivot (get xs (dec to))
          left  (long (loop [left  (inc from)
                             right (- to 2)]
                        (let [left  (long (loop [left left]
                                            (if (> (#?(:clj .compare) compf pivot (get xs left)) 0)
                                                (recur (inc left))
                                                left)))
                              right (long (loop [right right]
                                            (if (and (< left right)
                                                     (<= (#?(:clj .compare) compf pivot (get xs right)) 0))
                                                (recur (dec right))
                                                right)))]
                          (if (< left right)
                              (do (aswap! xs left right)
                                  (recur left (dec right)))
                              (do (aswap! xs left (dec to))
                                  left)))))]
      left)))

(defnt select!:quick* ; takes 5 seconds to compile
  "Also known as Hoare's selection algorithm.
   Performs an in-place quick-select.
   Prefer `intro-select` in all cases."
  {:adapted-from 'org.apache.lucene.util.IntroSelector
   :algorithm-attribution "Tony Hoare"
   :complexity {:worst   "O(n^2)"
                :average "O(n)"
                :best    "O(n)"}}
  ([#{array? !vector?} xs ^long k]
    (select!:quick* xs k 0 (count xs)))
  ([#{array? !vector?} xs ^long k #?(:clj ^Comparator compf :cljs compf)]
    (select!:quick* xs k compf 0 (count xs)))
  ([#{array? !vector?} xs ^long k ^long from ^long to] ; TODO infer
    (select!:quick* xs k compare from to))
  ([#{array? !vector?} xs ^long k #?(:clj ^Comparator compf :cljs compf) ^long from ^long to]
    (assert (<= from k))
    (assert (< k to))
    (loop [from from to to]
      (if (= 1 (- to from))
          xs
          (let [left (select!:quick:left xs k compf from to)]
            (cond (= left k)
                  xs
                  (< left k)
                  (recur (inc left) to  )
                  true
                  (recur from       left)))))))

#?(:clj (defmacro select!:quick [& args] `(select!:quick* ~(last args) ~@(butlast args))))
#?(:clj (defalias quick-select! select!:quick))

(defnt select!:heap*
  "nlog(n) worst case"
  [xs ^long k ^long from ^long to]
  (TODO)
; new Sorter() {

;   @Override
;   protected void swap(int i, int j) {
;     IntroSelector.this.swap(i, j);
;   }

;   @Override
;   protected int compare(int i, int j) {
;     return IntroSelector.this.compare(i, j);
;   }

;   public void sort(int from, int to) {
;     heapSort(from, to);
;   }
; }.sort(from, to);
)

(defnt ^long median-5
  "Returns the index of the median of a 5-element indexed collection.
   This is the key to a fast median-of-medians selection algorithm."
  {:adapted-from "http://moonflare.com/code/select/select.pdf"}
  [#{array? !vector?} xs #?(:clj ^Comparator compf :cljs compf) ^long start ^MutableReference *x0 ^MutableReference *x1 ^MutableReference *x2 ^MutableReference *x3 ^MutableReference *x4] ; TODO x0..5 are all mutable objects whose inner types must be the inner type of the array
  (setm! *x0 (get xs (+ start 0)))
  (setm! *x1 (get xs (+ start 1)))
  (setm! *x2 (get xs (+ start 2)))
  (setm! *x3 (get xs (+ start 3)))
  (setm! *x4 (get xs (+ start 4)))
  (when (neg? (#?(:clj .compare) compf (deref *x1) (deref *x0)))
    (swapm! *x0 *x1))
  (when (neg? (#?(:clj .compare) compf (deref *x2) (deref *x0)))
    (swapm! *x0 *x2))
  (when (neg? (#?(:clj .compare) compf (deref *x3) (deref *x0)))
    (swapm! *x0 *x3))
  (when (neg? (#?(:clj .compare) compf (deref *x4) (deref *x0)))
    (swapm! *x0 *x4))
  (when (neg? (#?(:clj .compare) compf (deref *x2) (deref *x1)))
    (swapm! *x1 *x2))
  (when (neg? (#?(:clj .compare) compf (deref *x3) (deref *x1)))
    (swapm! *x1 *x3))
  (when (neg? (#?(:clj .compare) compf (deref *x4) (deref *x1)))
    (swapm! *x1 *x4))
  (when (neg? (#?(:clj .compare) compf (deref *x3) (deref *x2)))
    (swapm! *x2 *x3))
  (when (neg? (#?(:clj .compare) compf (deref *x4) (deref *x2)))
    (swapm! *x2 *x4))
  (let [x2 (deref *x2)]
    (cond (= x2 (get xs (+ start 0))) 0
          (= x2 (get xs (+ start 1))) 1
          (= x2 (get xs (+ start 2))) 2
          (= x2 (get xs (+ start 3))) 3
          true                        4)))

(defnt ^long select!:median-of-medians:partition!
  "The same one used in quicksort, essentially as described
   in Introduction to Algorithms." ; TODO this may be used in quick-select too...
  {:adapted-from "http://moonflare.com/code/select/select.pdf"}
  [#{array? !vector?} xs #?(:clj ^Comparator compf :cljs compf) ^long from ^long size ^long i:pivot]
  (let [pivot (get xs (+ from i:pivot))
        _ (aswap! xs (+ from i:pivot) (+ from (dec size)))
        store-pos (long (loop [load-pos 0 store-pos 0]
                          (if (< load-pos (dec size))
                              (if (neg? (#?(:clj .compare) compf (get xs (+ from load-pos)) pivot))
                                  (do (aswap! xs (+ from load-pos) (+ from store-pos))
                                      (recur (inc load-pos) (inc store-pos)))
                                  (recur (inc load-pos) store-pos))
                              store-pos)))]
    (aswap! xs (+ from store-pos) (dec size))
    store-pos))

(defnt ^:<0> select!:median-of-medians*
  "Performs selection using the median of medians algorithm."
  {:adapted-from "http://moonflare.com/code/select/select.pdf"
   :complexity {:worst   "O(n)"
                :average "O(n)"
                :best    "O(n)"}}
  ([#{array? !vector?} xs ^long k]
    (select!:median-of-medians* xs k 0 (count xs)))
  ([#{array? !vector?} xs ^long k #?(:clj ^Comparator compf :cljs compf)]
    (select!:median-of-medians* xs k compf 0 (count xs)))
  ([#{array? !vector?} xs ^long k ^long from ^long to]
    (select!:median-of-medians* xs k compare from to))
  ([#{array? !vector?} xs ^long k #?(:clj ^Comparator compf :cljs compf) ^long from ^long to]
    (select!:median-of-medians* xs k compf from (- to from) (!ref) (!ref) (!ref) (!ref) (!ref)))
  ([#{array? !vector?} xs ^long k-0 #?(:clj ^Comparator compf :cljs compf) ^long from-0 ^long size-0 ^MutableReference *x0 ^MutableReference *x1 ^MutableReference *x2 ^MutableReference *x3 ^MutableReference *x4]  ; TODO x0..5 are all mutable objects whose inner types must be the inner type of the array
    (loop [k k-0 from from-0 size size-0]
      (if (< size 5)
          (do (dotimes [i size]
                (ifor [j (inc i) (< j size) (inc j)]
                  (when (neg? (#?(:clj .compare) compf (get xs (+ from j)) (get xs (+ from i))))
                    (aswap! xs (+ from i) (+ from j)))))
              xs)
          (let [; checked
                _ (loop [group-num 0 group from]
                     (when (<= (* group-num 5) (- size 5))
                       (do (aswap! xs (+ group (median-5 xs compf group *x0 *x1 *x2 *x3 *x4))
                                      (+ from group-num))
                           (recur (inc group-num) (+ group 5)))))
                num-medians (unchecked-divide-int size 5)
                ; Index of median of medians
                i:MOM (unchecked-divide-int num-medians 2)
                _ (select!:median-of-medians* xs i:MOM compf from num-medians *x0 *x1 *x2 *x3 *x4) ; TODO recursion
                i':MOM (select!:median-of-medians:partition! xs compf from size i:MOM)]
            ; checked
            (if (not= k i':MOM)
                (if (< k i':MOM)
                    (recur k from i':MOM)
                    (recur (dec (- k     i':MOM))
                           (inc (+ from i':MOM))
                           (dec (- size  i':MOM))))
                xs))))))

#?(:clj (defmacro select!:median-of-medians [& args] `(select!:median-of-medians* ~(last args) ~@(butlast args))))
#?(:clj (defalias median-of-medians! select!:median-of-medians))

(defnt ^:<0> select!:intro* ; Takes 5 seconds to compile
  "Performs an in-place intro-select.
   Arguably the best known selection algorithm, and the most performant.
   Starts off by using quick-select, but as soon as it deviates from O(n) operations,
   it defaults to using the median-of-medians algorithm, ensuring a worst-case
   run-time of O(n)."
  {:params-doc '{from "Inclusive" to "Exclusive"}
   :adapted-from 'org.apache.lucene.util.IntroSelector
   :algorithm-attribution "David Musser, 1997 (http://www.cs.rpi.edu/~musser/gp/introsort.ps)"
   :complexity {:worst   "O(n)"
                :average "O(n)"
                :best    "O(n)"}}
  ([#{array? !vector?} xs ^long k]
    (select!:intro* xs k 0 (count xs)))
  ([#{array? !vector?} xs ^long k #?(:clj ^Comparator compf :cljs compf)]
    (select!:intro* xs k compf 0 (count xs)))
  ([#{array? !vector?} xs ^long k ^long from ^long to] ; TODO infer
    (select!:intro* xs k compare from to))
  ([#{array? !vector?} xs ^long k #?(:clj ^Comparator compf :cljs compf) ^long from ^long to]
    (assert (<= from k))
    (assert (< k to))
    (let [max-iter (* 2 (num/integer-log (- to from) 2))]
      (loop [from from to to max-iter max-iter]
        (if (= 1 (- to from))
            xs
            (let [max-iter (dec max-iter)]
              (if (< max-iter 0)
                  (select!:median-of-medians* xs k compf from to)
                  (let [left (select!:quick:left xs k compf from to)]
                    (cond (= left k)
                          xs
                          (< left k)
                          (recur (inc left) to   max-iter)
                          true
                          (recur from       left max-iter))))))))))

#?(:clj (defmacro select!:intro [& args] `(select!:intro* ~(last args) ~@(butlast args))))
#?(:clj (defalias intro-select! select!:intro))

(defnt ^:<0> select-by!:intro* ; Takes 3 seconds to compile
  "Defaults to ascending selection."
  ([#{array? !vector?} xs ^long k kf]
    (select-by!:intro* xs k kf 0 (count xs)))
  ([#{array? !vector?} xs ^long k kf #?(:clj ^Comparator compf :cljs compf)]
    (select-by!:intro* xs k kf compf 0 (count xs)))
  ([#{array? !vector?} xs ^long k kf ^long from ^long to] ; TODO infer
    (select-by!:intro* xs k kf compare from to))
  ([#{array? !vector?} xs ^long k kf #?(:clj ^Comparator compf :cljs compf) ^long from ^long to] ; TODO infer
    (select!:intro* xs k ^Comparator (fn [x y] (.compare compf (kf x) (kf y))) from to)))

#?(:clj (defmacro select-by!:intro [& args] `(select-by!:intro* ~(last args) ~@(butlast args))))
#?(:clj (defalias intro-select-by! select-by!:intro))

#?(:clj (defalias select!    select!:intro   ))
#?(:clj (defalias select-by! select-by!:intro))

; ===== SEARCH ===== ;

(defn binary-search
  "Finds earliest occurrence of @x in @xs (a sorted List of numbers) using binary search."
  {:source "http://stackoverflow.com/questions/8949837/binary-search-in-clojure-implementation-performance"
   :todo ["Use Java impl for CLJ(S)"]}
  ([xs x] (binary-search xs x 0 (unchecked-dec #_dec* (count xs)) false))
  ([xs x a b between?]
    (loop [l (long a) h (long b)]
      (if (c/<= h (inc l))
          (cond
            (= x (get xs l)) l
            (= x (get xs h)) h
            :else (when between?
                    (if (= l h)
                        [(dec l) h]
                        [l       h])))
          (let [m (-> h (unchecked-subtract #_-* l) (bit-shift-right #_>> 1) (unchecked-add #_+* l))]
            (if (c/< (get xs m) x)
                (recur (long (unchecked-inc #_inc* m)) (long h))
                (recur (long l)        (long m))))))))
;___________________________________________________________________________________________________________________________________
;=================================================={   COLLECTIONS CREATION   }=====================================================
;=================================================={                          }=====================================================
; TODO fix
(def map->record hash-map)

(defnt invert [^+map? m]
  (reduce (rfn [m' k v] (assoc m' v k)) (empty m) m))

(declare ensurec)

(defn invert->multimap
  {:tests `{(invert->multimap {:a 1 :b 1 :c 2})
            {1 #{:a :b} 2 #{:c}}}}
  [m] (reduce (rfn [ret k v] (update ret v #(conj (ensurec % #{}) k))) {} m))

(defn invert-set-vals
  "E.g. {a #{b c d}} -> {b a, c a, d a}"
  [xs]
  (reduce (rfn [xs' k vs]
            (reduce (fn [xs'' v] (assoc xs'' v k)) xs' vs))
    {}
    xs))

(defn- update-nth-list*
  [x n f]
  (if (= n 0)
      (conjl (rest x) (f (first x)))
      (concat (ltake n x) (list (f (get x n))) (nthnext x (inc n)))))

(defnt update-nth
  ([^+vector? x n f] (update x n f))
  #_([^cdlist? x n f] (if (= n (lasti x)) ; TODO ENABLE THIS
                        (conj (.pop x) (f (last x)))
                        (update-nth-list* x n f)))
  ([^seq?  x n f] (update-nth-list* x n f)))

(defn update-first [x f] (update-nth x 0         f))
(defn update-last  [x f] (update-nth x (lasti x) f))

(defn index-with-ids
  "Adds unique ids to each entry."
  [vec-0]
  (let [ids (->> vec-0
                 (map+ :id)
                 (remove+ nil?)
                 (join (sorted-set-by (fn [a b] (> a b))))
                 atom)]
    (reducei
      (fn [vec-n entry n]
        (if (or (contains? entry :id)
                (empty?    entry))
            vec-n
            (let [id (-> ids deref first (ifn nil? (fn' 1) inc))]
              (conj! ids id)
              (assoc vec-n n (assoc entry :id id)))))
      vec-0
      vec-0)))



; REQUIRES hara.string.PATH/JOIN
#_(defn flatten-keys
  "takes map `m` and flattens the first nested layer onto the root layer.
  (flatten-keys {:a {:b 2 :c 3} :e 4})
  => {:a/b 2 :a/c 3 :e 4}
  (flatten-keys {:a {:b {:c 3 :d 4}
                     :e {:f 5 :g 6}}
                 :h {:i 7}
                 :j 8})
  => {:a/b {:c 3 :d 4} :a/e {:f 5 :g 6} :h/i 7 :j 8}"
  {:source "zcaudate/hara.data.path"}
  ([m]
   (reduce-kv (fn [m k v]
                (if (t/+hash-map? v)
                    (reduce-kv (fn [m sk sv]
                                 (assoc m (path/join [k sk]) sv))
                               m
                               v)
                    (assoc m k v)))
              {}
              m)))

(defn- pathify-keys-nested
  {:source "zcaudate/hara.data.path"}
  ([m] (pathify-keys-nested m -1 false []))
  ([m max] (pathify-keys-nested m max false []))
  ([m max keep-empty] (pathify-keys-nested m max keep-empty []))
  ([m max keep-empty arr]
   (reduce-kv (fn [m k v]
                (if (or (and (not (> 0 max))
                             (<= max 1))
                        (not (#?(:clj  t/+hash-map?
                                 :cljs t/+map?) v))
                        (and keep-empty
                             (empty? v)))
                  (assoc m (conj arr k) v)
                  (merge m (pathify-keys-nested v (dec max) keep-empty (conj arr k)))))
              {}
              m)))

; REQUIRES hara.string.PATH/JOIN
#_(defn flatten-keys-nested
  "Returns a single associative map with all of the nested
   keys of `m` flattened. If `keep` is added, it preserves all the
   empty sets
  (flatten-keys-nested {\"a\" {\"b\" {\"c\" 3 \"d\" 4}
                               \"e\" {\"f\" 5 \"g\" 6}}
                          \"h\" {\"i\" {}}})
  => {\"a/b/c\" 3 \"a/b/d\" 4 \"a/e/f\" 5 \"a/e/g\" 6}
  (flatten-keys-nested {\"a\" {\"b\" {\"c\" 3 \"d\" 4}
                               \"e\" {\"f\" 5 \"g\" 6}}
                          \"h\" {\"i\" {}}}
                       -1 true)
  => {\"a/b/c\" 3 \"a/b/d\" 4 \"a/e/f\" 5 \"a/e/g\" 6 \"h/i\" {}}"
  {:source "zcaudate/hara.data.path"}
  ([m] (flatten-keys-nested m -1 false))
  ([m max keep-empty]
   (-> (pathify-keys-nested m max keep-empty)
       (nested/update-keys-in [] path/join))))

; REQUIRES hara.string.PATH/SPLIT
#_(defn treeify-keys
  "Returns a nested map, expanding out the first
   level of keys into additional hash-maps.
  (treeify-keys {:a/b 2 :a/c 3})
  => {:a {:b 2 :c 3}}
  (treeify-keys {:a/b {:e/f 1} :a/c {:g/h 1}})
  => {:a {:b {:e/f 1}
          :c {:g/h 1}}}"
  {:source "zcaudate/hara.data.path"}
  [m]
  (reduce-kv (fn [m k v]
               (assoc-in m (path/split k) v))
             {}
             m))

; REQUIRES hara.string.PATH/SPLIT
#_(defn treeify-keys-nested
  "Returns a nested map, expanding out all
 levels of keys into additional hash-maps.
  (treeify-keys-nested {:a/b 2 :a/c 3})
  => {:a {:b 2 :c 3}}
  (treeify-keys-nested {:a/b {:e/f 1} :a/c {:g/h 1}})
  => {:a {:b {:e {:f 1}}
          :c {:g {:h 1}}}}"
  {:source "zcaudate/hara.data.path"}
  [m]
  (reduce-kv (fn [m k v]
               (if (and (t/+hash-map? v) (contains? v))
                 (update-in m (path/split k) nested/merge-nested (treeify-keys-nested v))
                 (assoc-in m (path/split k) v)))
             {}
             m))

(defn remove-repeats
  "Returns a vector of the items in `coll` for which `(f item)` is unique
   for sequential `item`'s in `coll`.
    (remove-repeats [1 1 2 2 3 3 4 5 6])
    ;=> [1 2 3 4 5 6]
    (remove-repeats even? [2 4 6 1 3 5])
    ;=> [2 1]

    h/remove-repeats [1 1 2 2 3 3 4 5 6])
    => [1 2 3 4 5 6]
    (h/remove-repeats :n [{:n 1} {:n 1} {:n 1} {:n 2} {:n 2}])
    => [{:n 1} {:n 2}]
    (h/remove-repeats even? [2 4 6 1 3 5])
    => [2 1])"
  {:source "zcaudate/hara"
   :todo "merge with something else"}
  ([coll] (remove-repeats identity coll))
  ([f coll] (remove-repeats f coll [] nil))
  ([f coll output last]
     (if-let [v (first coll)]
       (cond (and last (= (f last) (f v)))
             (recur f (next coll) output last)
             :else (recur f (next coll) (conj output v) v))
       output)))


(defn transient-copy
  {:attribution ["Alex Gunnarson"]}
  [t]
  (let [copy (transient [])]
    (dotimes [n (count t)]
      (conj! copy (get t n)))
    (persistent! copy)))

(defnt ensurec*
  ([#{+vector? +set? +map?} ensurer ensured]
    (cond ((t/->pred ensurer) ensured)
          ensured
          (nil? ensured)
          ensurer
          :else (conj (t/->base ensurer) ensured))))

(defn ensurec
  "ensure-collection.
   Ensures that @ensured is the same class as @ensurer.
   This might be used in cases where one would like to ensure that
   |conj|ing onto a value in a map is valid."
  {:author "Alex Gunnarson"
   :tests '{(ensurec nil [:a])
              [:a]
            (ensurec :a  [:b])
              [:a]
            (ensurec []  [:b])
              []
            (update {:a 1} :a (fn-> (ensurec []) (conj 3)))
              {:a [1 3]}}}
  [ensured ensurer]
  (ensurec* ensurer ensured))

(defn index-by-vals
  {:tests '{(index-by-vals
              {:a #{1 2 3}
               :b #{2 4 5}
               :c #{1 3 4}})
            {1 #{:a :c}
             2 #{:a :b}
             3 #{:a :c}
             4 #{:b :c}
             5 #{:b}}}}
  [coll & [{:keys [into-coll get-key get-val]
            :or {into-coll #{}
                 get-key fn/seconda
                 get-val fn/firsta}
            :as opts}]]
  (let [update-f (fn [index-f k v]
                   (update! index-f (get-key k v)
                     (fn-> (ensurec into-coll) (conj (get-val k v)))))]
    (persistent!
      (reduce
        (fn [index k vs]
          (reduce (fn ([index-f v      ] (update-f index-f k v                  ))
                      ([index-f v-k v-v] (update-f index-f k (map-entry v-k v-v))))
            index
            vs))
        (transient {})
        coll))))

(defn merge-deep-with
  "Like `merge-with` but merges maps recursively, applying the given fn
  only when there's a non-map at a particular level.

  (merge-deep-with + {:a {:b {:c 1 :d {:x 1 :y 2}} :e 3} :f 4}
                    {:a {:b {:c 2 :d {:z 9} :z 3} :e 100}})
  => {:a {:b {:z 3, :c 3, :d {:z 9, :x 1, :y 2}}, :e 103}, :f 4}"
  {:attribution "clojure.contrib.map-utils via taoensso.encore"
   :todo ["Replace |merge-with| with a more performant version which uses |map/merge|."]}
  [f & maps]
  (apply
    (fn merge* [& maps]
      (if (seq-and map? maps)
          (apply merge-with merge* maps)
          (apply f maps)))
    maps))

(def merge-deep
  (partial merge-deep-with
    (fn ([x]   (second x))
        ([x y] y))))

(defn join-deep
  "Like `merge-deep`, but `join`s collections instead of replacing them."
  {:tests `{(join-deep {:a [1]
                        :b 2 :e 5
                        :c {:d 3
                            :e 4
                            :f #{1 2 3}
                            :g {:h 5}}}
                       {:a [2 "3"]
                        :b 4 :d 6
                        :c {:d "3"
                            :f #{1 "2" 3}
                            :g {:h "6" :i "x"}}})
             {:a [1 2 "3"]
              :b 4 :d 6 :e 5
              :c {:d "3" :e 4
                  :f #{1 3 2 "2"}
                  :g {:h "6", :i "x"}}}}}
  ([x0 x1]
    (cond (and (map? x0) (map? x1))
          (merge-with join-deep x0 x1)
          (and (coll? x0) (coll? x1))
          (join x0 x1)
          (nil? x1)
          x0
          :else x1))
  ([x0 x1 & args] (reduce join-deep (join-deep x0 x1) args)))

; TODO: incorporate |split-at| into the quantum.core.collections/split-at protocol

; `seq-ldifference` <~> `lodash/difference(By)?`
(defn seq-ldifference
  "Like |set/difference| but for seqs.
   Returns what is in @l but not in @r
   based on the results the application of @selectors returns."
  {:in [[{:n 1 :a 3}
         {:n 2 :a 3}]
        [{:n 4 :b 5}
         {:n 2 :a 3 :b 10}]
        #{:n :a}]
   :out [[{:n 2 :a 3}]]}
  ([l r]
    (set/difference
      (->> l (join #{}))
      (->> r (join #{}))))
  ([l r selectors]
    (let [l-grouped (->> l (group-by (apply juxt selectors)))
          r-grouped (->> r (group-by (apply juxt selectors)))]
      (->> (set/difference
             (->> l-grouped keys (join #{}))
             (->> r-grouped keys (join #{})))
           (map+ (fn->> (get l-grouped)))
           (reduce #(join %1 %2) #{})))))

;; find rank of element as primitive long, -1 if not found
; (doc avl/rank-of)
; ;; find element closest to the given key and </<=/>=/> according
; ;; to coll's comparator
; (doc avl/nearest)
; ;; split the given collection at the given key returning
; ;; [left entry? right]
; (doc avl/split-key)
;; return subset/submap of the given collection; accepts arguments
;; reminiscent of clojure.core/{subseq,rsubseq}
; (doc avl/subrange)

(defalias subset? set/subset?)

#?(:clj
(defn get-map-constructor
  "Gets a record's map-constructor function via its class name."
  [rec]
  (let [^String class-name-0
          (if (class? rec)
              (-> rec str)
              (-> rec class str))
        ^String class-name
          (subview class-name-0
            (-> class-name-0 (last-index-of ".") inc)
            (-> class-name-0 count))
        map-constructor-fn
          (->> class-name (str "map->") symbol eval)]
    map-constructor-fn)))

(defn into-map-by [m k ms]
  (reduce (fn [ret elem] (assoc ret (k elem) elem)) m ms))

(defn transpose
  "Transposes a 2D matrix. (Pivots a table à la Excel.)"
  {:in '[[1 4 7 a]
         [2 5 8 b]
         [3 6 9 c]]
   :out '[[1 2 3]
          [4 5 6]
          [7 8 9]
          [a b c]]
   :todo ["Make cleaner/ more parallelizable."]}
  [table-0]
  (let [height-f (count (first table-0))
        width-f  (count table-0)
        table-f  (red-for [row-i   (range height-f)
                           table-n (transient [])]
                   (let [row-f (red-for [col-i (range width-f)
                                         row   (transient [])]
                                 (conj! row (-> table-0 (get col-i) (get row-i))))]
                     (conj! table-n (persistent! row-f))))]
    (persistent! table-f)))

(defn merge-keys-with
  {:tests '{(merge-keys-with {:a {:b 1 :c 2}
                              :b {:c 3 :a 4}}
              [:a :b]
              (fn [a b] a))
            {:a {:b 1 :c 2 :a 4}}}}
  [m [k-0 & ks] f]
  (reduce
    (fn [ret k]
      (-> m
          (update k-0 #(merge-with f % (get m k)))
          (dissoc k)))
    m
    ks))

; =========== NECESSITIES FOR DATASCRIPT AND POSH ============== ;

(defn trim-head
  {:from "tonsky/datascript-todo.util"}
  [xs n]
  (->> xs
       (ldropl (- (count xs) n))
       c/vec))

(defn take-until*
  {:from "mpdairy/posh.q-pattern-gen"}
  [stop-at? ls]
  (if (or
       (empty? ls)
       (stop-at? (first ls)))
    []
    (cons (first ls) (take-until* stop-at? (rest ls)))))

(defn rest-at
  {:from "mpdairy/posh.q-pattern-gen"}
  [rest-at? ls]
  (if (or (empty? ls) (rest-at? (first ls)))
    ls
    (recur rest-at? (rest ls))))

(defn split-list-at
  {:from "mpdairy/posh.q-pattern-gen"}
  [split-at? ls]
  (if (empty? ls)
    {}
    (merge {(first ls) (take-until* split-at? (take-until* split-at? (rest ls)))}
           (split-list-at split-at? (rest-at split-at? (rest ls))))))

(defn deep-list?
  {:from "mpdairy/posh.core"}
  [x]
  (cond (list? x) true
        (coll? x) (if (empty? x) false
                      (or (deep-list? (first x))
                          (deep-list? (c/vec (rest x)))))))

(defn deep-find ; TODO this might be `postwalk-seq-or`?
  {:from "mpdairy/posh.core"}
  [f x]
  (if (coll? x)
      (if (empty? x)
        false
        (or (deep-find f (first x))
            (deep-find f (rest  x))))
      (f x)))

(defn deep-map [f x]
  {:from "mpdairy/posh.core"}
  (cond
   (map? x) (let [r (lmap (partial deep-map f) x)]
              (zipmap (lmap #(first %1) r) (lmap #(second %1) r)))
   (coll? x) (mapv (partial deep-map f) x)
   :else (f x)))

(defn drop-tail
  {:from "tonsky/datascript-todo.util"
   :todo ["It doesn't seem like this actually does anything"]}
  [xs pred]
  (loop [acc []
         xs  xs]
    (let [x (first xs)]
      (cond
        (nil? x) acc
        (pred x) (conj acc x)
        :else  (recur (conj acc x) (next xs))))))



(defn sliding-window+
  "Creates a \"sliding window\" of window size `n` over a
   reducible `xs`.
   Useful for e.g. n-fold cross-validation."
  {:attribution "alexandergunnarson"
   :todo        #{"Allow sliding window step to be customized"}}
  [n xs] ; TODO xs must be `reducible?`
  (let [xsv (join xs) ; for O(log(n)) splits instead of O(n)
        ct  (count xsv)
        _   (err/assert (<= n ct))
        r   (rem ct n)]
    (->> (iterate+ (fn1 + n) 0)
         (take+    (+ (quot ct n)
                      (if (zero? r) 0 1)))
         (map+     (fn [i]
                     (if (>= (+ i n) ct)
                         [(slice xsv 0 i)
                          (slice xsv i)
                          []]
                         [(slice xsv 0 i)
                          (slice xsv i (+ i n))
                          (slice xsv (+ i n))]))))))

(defn sliding-window-splits+
  "Creates \"sliding window splits\" of number of splits `n`,
   over a reducible `xs`, keeping window size as constant as
   possible.
   Split 'leftovers' are reallocated at the end.
   Useful for e.g. n-fold cross-validation."
  {:attribution "alexandergunnarson"
   :todo        #{"Allow customized reallocation"}}
  [n:split xs] ; TODO xs must be `reducible?`
  (let [xsv      (join xs) ; for O(log(n)) splits instead of O(n)
        ct       (count xsv)
        _        (err/assert (<= n:split ct))
        n:window (quot ct n:split)
        n:extra  (rem ct n:split)
        first-iter-of-extra (- n:split n:extra)]
    (->> (iterate+ (fn1 + n:window) 0)
         (take+    n:split)
         (map+     (fn [i]
                     (let [iter         (/ i n:window)
                           extra?       (>= iter first-iter-of-extra)
                           extra-offset (max 0 (- iter first-iter-of-extra))
                           offset       (+ i n:window (if extra? 1 0))]
                       [(slice xsv 0 (+ i extra-offset))
                        (slice xsv (+ i extra-offset) (+ offset extra-offset))
                        (slice xsv (+ offset extra-offset))]))))))

(defn max-subview
  "The contiguous subsequence of maximum asum.
   Uses Kadane's algorithm.
   A subsequence of length zero has sum zero."
   {:attribution "alexandergunnarson"
    :todo  ["Extend to all comparables"
            "Handle all-negatives gracefully (see Wikipedia)"]
    :tests `{(max-subview [10 -5 15 -30 10 -5 40 10])
             [10 -5 40 10]}}
  [s]
  (let [pos+       (fn [[fromi toi sum] [i x]]
                     (if (neg? sum) [i toi x] [fromi i (+ sum x)]))
        [from to]  (->> s indexed+
                          (reductions pos+ [0 0 0])
                          (reduce (partial max-key (fn1 get 2))
                                  [0 0 #?(:clj  Long/MIN_VALUE
                                          :cljs js/Number.MIN_SAFE_INTEGER)]))]
    (subview s from (inc to))))

(defn seq->bitmap
  "Given n unique values in a seq, transforms them into a bitmap."
  {:example `{(seq->bitmap [:a :b :a :c :a :b])
              {:positions {:a 0 :b 1 :c 0}
               :bitmap    [[1.0 0.0 0.0]
                           [0.0 1.0 0.0]
                           [1.0 0.0 0.0]
                           [0.0 0.0 1.0]
                           [1.0 0.0 0.0]
                           [0.0 1.0 0.0]]}}}
  [xs]
  (let [positions
         (->> xs distinct+
                 indexed+
                 (map+ (fn-> rseq c/vec)) ; TODO not sure why just `rseq` won't work
                 (join (map/sorted-map))) #_(join {}) ; TODO fix
        base (repeat (count positions) 0.0)]
    {:positions positions
     :bitmap    (->> xs (map #(assoc base (get positions %) 1.0)))}))

; ====== NUMERIC ======

(defn allocate-by-percentages
  "Allocate @n into groups of @percents.
   Overflow and underflow are distributed over the groups with the highest
   percentages, in descending order.
   Throws if is not able to partition."
  {:tests `{[1 [1 1]]
            :fail
            [1 [1]]
            [1]
            [3 [0.33 0.66]]
            [1 2]
            [3 [0.1 0.33]]
            [1 2]}}
  [n percents]
  (let [_ (err/assert (>= n (count percents)))
        _ (err/assert (->> percents (reduce + 0) (<- <= 1)))
        allocated (for [p percents]
                    (long (num/ceil (double (* n p))))) ; TODO make not use long or double
        sorted (->> allocated
                    (map-indexed+ vector)
                    (join! (!vector))
                    (sort-by! (fn1 second)))
        total  (->> allocated (reduce + 0))
        *flow  (long (- total n))] ; over- or underflow
    (case *flow
      0
      allocated
      (let [sign (num/sign *flow)]
        (red-for [[i:next-highest-percentage _] (rseq sorted)
                  [allocated' *flow']           [allocated *flow]]
          (if (zero? *flow')
              (reduced allocated')
              [(update allocated' i:next-highest-percentage (fn1 - sign))
               (- *flow' sign)]))))))

#?(:cljs
(defn jsx->clj
  [x]
  (for* {} [k (.keys js/Object x)]
    [(keyword k)
     (let [v (aget x k)]
       (if (fn? v)
           "<function>"
           v))])))

(defn !combinations-2+
  "Calculates the combination of the element at index 0 with the element at
   index 1, and so on — one half of a symmetric matrix:
     0 1 2 3
   0   x x x
   1     x x
   2       x
   3
   and returns the results as a reducible of e.g.:
   `[[0 1] [0 2] [0 3] [1 2] [1 3] [2 3]]`.
   Does not calculate intermediate sequences.
   `xs` must be reducible"
  [xs]
  (->> xs
       (!map-indexed+
         (fn [i x]
           (->> xs (coll/drop+ (inc i)) (map+ (fn [x'] [x x']))))) ; TODO customize the pairs this gets output in ; TODO use !drop+
       cat+))

; ===== LOGGING ===== ; (TODO MOVE)

(defn notify-progress+
  ([topic r] (notify-progress+ topic (fn [i _] (str "Item # " i " complete.")) r))
  ([topic report-fn r]
  (->> r
       (map-indexed+ (fn [i x]
                       (log/pr-opts topic {:stack -4} (report-fn i x))
                       x)))))

#_(map ns/assert-ns-aliased
  '[quantum.core.collections.core
    ;quantum.core.collections.sociative
    ;quantum.core.collections.differential
    quantum.core.collections.generative
    quantum.core.collections.map-filter
    quantum.core.collections.selective
    ;quantum.core.collections.tree
    ;quantum.core.collections.zip
    quantum.core.collections.logic])
