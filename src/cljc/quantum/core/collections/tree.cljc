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
  quantum.core.collections.tree
           (:refer-clojure :exclude
             [for doseq reduce
              contains?
              repeat repeatedly
              interpose
              range
              take take-while
              drop drop-while
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
                       :refer [split-at map-entry]                       ]
                     [quantum.core.data.set                   :as set    ]
                     [quantum.core.data.vector                :as vec  
                       :refer [catvec subvec+]                           ]
                     [quantum.core.collections.base           :as base   
                       :refer [#?@(:clj [kmap])]                         ]
                     [quantum.core.collections.core           :as coll   
                       :refer [#?@(:clj [count first rest getr last-index-of
                                         index-of lasti conj conj! contains?
                                         empty])
                               key val]                                  ]
                     [quantum.core.collections.selective      :as sel    
                       :refer [in-k?]                                    ]
                     [quantum.core.collections.map-filter     :as mf    
                       :refer [map-keys+]                                ]
                     [quantum.core.error                      :as err  
                       :refer [->ex]                                     ]
                     [quantum.core.fn                         :as fn  
                       :refer [#?@(:clj [compr <- fn-> fn->>  
                                         f*n defcurried
                                         ->predicate])
                              fn-nil juxt-kv withf->>]  ]
                     [quantum.core.log                        :as log    ]
                     [quantum.core.logic                      :as logic
                       :refer [#?@(:clj [fn-not fn-or fn-and whenf whenf*n
                                         ifn if*n condf condf*n]) nnil? any?]]
                     [quantum.core.macros                     :as macros 
                       :refer [#?@(:clj [defnt])]                        ]
                     [quantum.core.reducers                   :as red    
                       :refer [#?@(:clj [reduce join])]                  ]
                     [quantum.core.string                     :as str    ]
                     [quantum.core.string.format              :as sform  ]
                     [quantum.core.type                       :as type  
                       :refer [#?@(:clj [lseq? transient? editable? 
                                         boolean? should-transientize?])]]
                     [quantum.core.analyze.clojure.predicates :as anap   ]
                     [quantum.core.type.predicates            :as tpred  ]
                     [clojure.walk                            :as walk   ]
                     [quantum.core.loops                      :as loops  
                       :refer [#?@(:clj [doseqi reducei lfor])]]
                     [quantum.core.vars                       :as var  
                       :refer [#?@(:clj [defalias])]                     ])
  #?(:cljs (:require-macros  
                     [quantum.core.collections.core           :as coll   
                       :refer [count first rest getr lasti index-of lasti
                               conj conj! contains? empty]               ]
                     [quantum.core.collections.base           :as base   
                       :refer [kmap]                                     ]
                     [quantum.core.fn                         :as fn
                       :refer [compr <- fn-> fn->> f*n defcurried]       ]
                     [quantum.core.log                        :as log    ]
                     [quantum.core.logic                      :as logic 
                       :refer [fn-not fn-or fn-and whenf whenf*n 
                               ifn if*n condf condf*n]                   ]
                     [quantum.core.loops                      :as loops  
                       :refer [doseqi reducei lfor]                      ]
                     [quantum.core.macros                     :as macros 
                       :refer [defnt]                                    ]
                     [quantum.core.reducers                   :as red    
                       :refer [reduce join]                              ]
                     [quantum.core.type                       :as type 
                       :refer [lseq? transient? editable? boolean? 
                               should-transientize?]                     ]
                     [quantum.core.vars                       :as var 
                       :refer [defalias]                                 ])))
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
  {:todo ["Fix class overlap" "Preserve metadata"]}
  ; clojure.lang.PersistentList$EmptyList : '()
  ; special case to preserve type
  ([^list?  coll f] (apply list  (map f coll)))
  #_([^dlist? coll f] (apply dlist (map f coll)))  ; TODO ENABLE THIS UPON RESTART
  ([^transientizable? coll f]
     (persistent!
       (core/reduce
         (fn [r x] (core/conj! r (f x)))
         (transient (core/empty coll)) coll)))
  ; generic sequence fallback
  ; TODO add any seq in general
  ([#{lseq? #_(- seq? list?)} coll f] (map f coll))
  ; |transient| discards metadata as of Clojure 1.6.0

  ; Persistent collections that don't support transients
  #?(:clj  ([#{clojure.lang.PersistentQueue
               clojure.lang.PersistentStructMap
               clojure.lang.PersistentTreeMap
               clojure.lang.PersistentTreeSet} coll f]
             (core/reduce
               (fn [r x] (conj r (f x)))
               (empty coll) coll)))
  #?(:clj  ([^map-entry? coll f] (map-entry (f (key coll)) (f (val coll)))))
  #?(:clj  ([^record?    coll f] (core/reduce (fn [r x] (conj r (f x))) coll coll)))
  #?(:clj  ([:else       x    f] x))
  #?(:cljs ([:else obj  f]
             (if (coll? obj)
                 (into (empty obj) (map f obj))
                 obj))))

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

(defn prewalk-replace
  "Recursively transforms form by replacing keys in smap with their
  values. Like clojure/replace but works on any data structure. Does
  replacement at the root of the tree first."
  {:attribution "Stuart Sierra, stuartsierra/clojure.walk2"}
  [smap form]
  (prewalk (whenf*n (f*n in-k? smap) smap) form))

(defn postwalk-replace
  "Recursively transforms form by replacing keys in smap with their
  values. Like clojure/replace but works on any data structure. Does
  replacement at the leaves of the tree first."
  {:attribution "Stuart Sierra, stuartsierra/clojure.walk2"}
  [smap form]
  (postwalk (whenf*n (f*n in-k? smap) smap) form))

(defn tree-filter
  "Like |filter|, but performs a |postwalk| on a treelike structure @tree, putting in a new vector
   only the elements for which @pred is true."
  {:attribution "Alex Gunnarson"}
  [pred tree]
  (let [results (transient [])]
    (postwalk
      (whenf*n pred
        (fn->> (withf->> #(conj! results %)))) ; keep it the same
      tree)
    (persistent! results)))

; ===== Transform nested maps =====

(defn apply-to-keys
  {:attribution "Alex Gunnarson"}
  ([m] (apply-to-keys m identity))
  ([m f]
    (postwalk
      (whenf*n map? (fn->> (map-keys+ f) (join {})))
      m)))

(defn keywordize-keys
  "Recursively transforms all map keys from strings to proper keywords."
  {:attribution "Alex Gunnarson"}
  [x]
  (apply-to-keys x str/keywordize))

(defn keywordify-keys
  "Recursively transforms all map keys from strings to keywords."
  {:attribution "Alex Gunnarson"}
  [x]
  (apply-to-keys x (whenf*n string? keyword)))

(defn stringify-keys
  "Recursively transforms all map keys from keywords to strings."
  {:attribution "Alex Gunnarson"}
  [x]
  (apply-to-keys x (whenf*n keyword? name)))