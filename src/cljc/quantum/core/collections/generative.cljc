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
  quantum.core.collections.generative
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
                                         index-of lasti])
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
                       :refer [#?@(:clj [reduce join]) map+]             ]
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
                       :refer [count first rest getr lasti index-of lasti]]
                     [quantum.core.collections.base           :as base   
                       :refer [kmap]                                     ]
                     [quantum.core.fn                         :as fn
                       :refer [compr <- fn-> fn->> f*n]       ]
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

; ===== REPEAT =====

(declare range)

(defn repeat
  ([obj]   (core/repeat obj)) ; can't do eager infinite repeat
  ([n obj] (for [i (range n)] obj)))

(defalias lrepeat core/repeat)

; ===== REPEATEDLY ===== ;

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

; ===== RANGE ===== ;

(#?(:clj defalias :cljs def) range+ red/range+)

(defn lrrange
  "Lazy reverse range."
  {:usage '(lrrange 0 5)
   :out   '(5 4 3 2 1)}
  ([]    (iterate core/dec 0))
  ([a]   (iterate core/dec a))
  ([a b]
    (->> (iterate core/dec b) (core/take (- b (core/dec a))))))

(defn lrange
  ([]  (core/range))
  ([a] (core/range a))
  ([a b]
    (if (neg? (- a b))
        (lrrange a b)
        (core/range a b))))

(defn rrange
  "Reverse range"
  {:ret-type 'Vector
   :todo ["Performance with |range+| on [a b] arity, and rseq"]}
  ([]    (lrrange))
  ([a]   (lrrange a))
  ([a b] (->> (range+ a b) (join []) reverse)))

(defn range
  {:ret-type 'Vector
   :todo ["Performance with |range+| on [a b] arity"]}
  ([]    (lrange))
  ([a]   (lrange a))
  ([a b]
    (if (neg? (- a b))
        (rrange a b))
        (->> (range+ a b) (join []))))
