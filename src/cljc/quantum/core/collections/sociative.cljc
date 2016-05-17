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
  quantum.core.collections.sociative
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
                       :refer [split-at map-entry]                       ]
                     [quantum.core.data.set                   :as set    ]
                     [quantum.core.data.vector                :as vec  
                       :refer [catvec subvec+]                           ]
                     [quantum.core.collections.core           :as coll   
                       :refer [#?@(:clj [count first second rest get
                                         last-index-of index-of lasti conj
                                         conj! contains? containsk? assoc!
                                         empty?])
                               key val]                                  ]
                     [quantum.core.collections.base           :as base   
                       :refer [#?@(:clj [kmap])]                         ]
                     [quantum.core.collections.generative                
                       :refer [range]                                    ]
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
                       :refer [#?@(:clj [doseqi reducei lfor]) reduce-2]]
                     [quantum.core.vars                       :as var  
                       :refer [#?@(:clj [defalias])]                     ])
  #?(:cljs (:require-macros  
                     [quantum.core.collections.core           :as coll   
                       :refer [count first second rest getr lasti index-of
                               lasti conj conj! contains? containsk? assoc!
                               empty?]]
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
      (let [[trans-fn pers-fn]
             (if (transient? coll-0) [identity identity] [transient persistent!])]
        (pers-fn
          (loops/reduce
            (fn [coll-n _] (conj! coll-n nil)) ; extend-vec part
            (trans-fn coll-0)
            (range (count coll-0) (inc k)))))
      coll-0))

(defn assoc+
  {:todo ["Protocolize on IEditableCollection"
          "Probably has performance issues"
          "Stop the assoc things"]}
  ([coll-0 k v]
    (assoc (extend-coll-to coll-0 k) k v))
    ; once probably gives no performance benefit from transience
  ([coll-0 k v & kvs-0]
    (let [edit?    (editable? coll-0)
          trans-fn (if edit? transient   identity)
          pers-fn  (if edit? persistent! identity)
          assoc-fn (if edit? #(assoc! %1 %2 %3) #(assoc %1 %2 %3))]
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

(defn assoc-if
  "Works like assoc, but only associates if condition is true."
  {:from "macourtney/clojure-tools"
   :contributors ["Alex Gunnarson"]}
  ([pred m k v] 
    (if (pred m k v)
        (assoc m k v)
        m))
  ([pred m k v & kvs]
    (reduce 
      (fn [output k-n v-n] 
        (assoc-if pred output k-n v-n))
      (assoc-if pred m k v)
      (partition-all 2 kvs))))

(defn assoc-when-none 
  "assoc's @args to @m only when the respective keys are not present in @m."
  [m & args]
  (assoc-if (fn [m k _] (not (contains? m k)))))

(defn update+
  "Updates the value in an associative data structure @coll associated with key @k
   by applying the function @f to the existing value."
  {:attribution "weavejester.medley"
   :contributors ["Alex Gunnarson"]}
  ([coll k f]      (assoc+ coll k       (f (get coll k))))
  ([coll k f args] (assoc+ coll k (apply f (get coll k) args))))

(defn update-when
  "Updates only if @pred is true for @k in @m."
  [m k pred f]
  (if (-> m k pred)
      (update m k f)
      m))

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

(defn record->map [rec] (join {} rec))

;--------------------------------------------------{        UPDATE-IN         }-----------------------------------------------------
(defn update-in!
  "'Updates' a value in a nested associative structure, where ks is a sequence of keys and
  f is a function that will take the old value and any supplied args and return the new
  value, and returns a new nested structure. The associative structure can have transients
  in it, but if any levels do not exist, non-transient hash-maps will be created."
  {:attribution "flatland.useful"}
  [m [k & ks] f & args]
  (let [assoc-fn (if ((fn-or transient?) m)
                     #(assoc! %1 %2 %3)
                     #(assoc  %1 %2 %3))
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

(defnt assoc-in!
  "Associates a value in a nested associative structure, where ks is a sequence of keys
  and v is the new value and returns a new nested structure. The associative structure
  can have transients in it, but if any levels do not exist, non-transient hash-maps will
  be created."
  {:attribution "flatland.useful"}
  ([^atom? m ks obj] (swap! m assoc-in ks obj))
  ([m ks v]
    (update-in! m ks (constantly v))))

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
          (-> coll transient (core/dissoc! coll key-0) persistent!)
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
  ([#{map?} coll obj] (dissoc coll obj))
  ([^:obj   coll obj] (dissoc coll obj))
  ([^set?   coll obj] (disj   coll obj)))

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
  (if (containsk? coll k-0)
      (-> coll
         (assoc+  k-f (get coll k-0))
         (dissoc+ k-0))
      coll))

(defn re-assocs+ [coll & kfs]
  (reduce-2 ; Inefficient
    (fn [ret k-n f-n] (re-assoc+ ret k-n f-n))
    coll
    kfs))

(defn assoc-with
  "Like |merge-with| but for |assoc|."
  {:author "Alex Gunnarson"
   :tests '{(assoc-with {:a 1} (fn [a b] (-> a (ensurec []) (conj b))) :a 3)
            {:a [1 3]}}}
  [m f k v]
  (if-let [v-0 (get m k)]
    (assoc m k (f v-0 v))
    (assoc m k v)))