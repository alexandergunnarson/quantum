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
  quantum.core.collections.selective
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
                                         contains? containsk? containsv?
                                         index-of lasti conj!])
                               key val reverse]                          ]
                     [quantum.core.collections.base           :as base   
                       :refer [#?@(:clj [kmap])]                         ]
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
                       :refer [#?@(:clj [reduce join]) map+ filter+]     ]
                     [quantum.core.string                     :as str    ]
                     [quantum.core.string.format              :as sform  ]
                     [quantum.core.type                       :as type  
                       :refer [#?@(:clj [lseq? transient? editable? 
                                         boolean? should-transientize?])]]
                     [quantum.core.analyze.clojure.predicates :as anap   ]
                     [quantum.core.type.predicates            :as tpred  ]
                     [clojure.walk                            :as walk   ]
                     [quantum.core.loops                      :as loops  
                       :refer [for]                                      ]
                     [quantum.core.vars                       :as var  
                       :refer [#?@(:clj [defalias])]                     ])
  #?(:cljs (:require-macros  
                     [quantum.core.collections.core           :as coll   
                       :refer [count first rest getr lasti index-of lasti
                               contains? containsk? containsv? conj!]    ]
                     [quantum.core.collections.base           :as base   
                       :refer [kmap]                                     ]
                     [quantum.core.fn                         :as fn
                       :refer [compr <- fn-> fn->> f*n]                  ]
                     [quantum.core.log                        :as log    ]
                     [quantum.core.logic                      :as logic 
                       :refer [fn-not fn-or fn-and whenf whenf*n 
                               ifn if*n condf condf*n]                   ]
                     [quantum.core.loops                      :as loops  
                       :refer [for]                                      ]
                     [quantum.core.macros                     :as macros 
                       :refer [defnt]                                    ]
                     [quantum.core.reducers                   :as red    
                       :refer [reduce join]                              ]
                     [quantum.core.type                       :as type 
                       :refer [lseq? transient? editable? boolean? 
                               should-transientize?]                     ]
                     [quantum.core.vars                       :as var 
                       :refer [defalias]                                 ])))

(defn in?
  "The inverse (converse?) of |contains?|"
  {:todo ["|definline| this?"]}
  [elem coll] (contains? coll elem))

(defn in-k?
  {:todo ["|definline| this?"]}
  [elem coll] (containsk? coll elem))

(defn in-v?
  {:todo ["|definline| this?"]}
  [elem coll] (containsv? coll elem))

; ;-----------------------{       SELECT-KEYS       }-----------------------
(defn select-keys
  "A transient and reducing version of clojure.core's |select-keys|."
  {:performance
    "45.3 ms vs. core's 60.29 ms on:
     (dotimes [_ 100000]
       (select-keys
         {:a 1 :b 2 :c 3 :d 4 :e 5 :f 6 :g 7}
         [:b :c :e]))).
     Performs much better on large set of keys."} 
  [keyseq m]
    (-> (loops/reduce
          (fn [ret k]
            (let [entry (find m k)]
              (if entry
                  (conj! ret entry)
                  ret)))
          (transient {})
          (seq keyseq))
        persistent!
        (with-meta (meta m))))

(defn select-keys+
  "Not as fast as select-keys with transients."
  {:todo ["Fix performance"]}
  [ks m]
  (let [ks-set (set ks)]
    (->> m
         (filter+
           (compr key (f*n in-k? ks-set))))))

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

(defn get-key
  {:todo ["Wasteful lack of performance"]}
  [m obj] (-> m (get-keys obj) first))

(defn vals+
  {:attribution "Alex Gunnarson"
   :todo ["Compare performance with core functions"]}
  [m]
  (->> m (map+ val) (join [])))

(defn keys+
  {:attribution "Alex Gunnarson"
   :todo ["Compare performance with core functions"]}
  [m]
  (->> m (map+ key) (join [])))
