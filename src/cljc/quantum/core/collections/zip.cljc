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
  quantum.core.collections.zip
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
              map-entry?
              boolean?])
           (:require [#?(:clj  clojure.core
                         :cljs cljs.core   )                  :as core   ]
                     [fast-zip.core                           :as zip    ]
                     [quantum.core.data.map                   :as map
                       :refer [map-entry]                                ]
                     [quantum.core.collections.base           :as base]
                     [quantum.core.collections.core           :as coll
                       :refer [#?@(:clj [count first second rest getr last-index-of
                                         index-of lasti conj conj! contains?
                                         empty])
                               key val]                                  ]
                     [quantum.core.error                      :as err
                       :refer        [->ex #?@(:clj [catch-all])]
                       :refer-macros [catch-all]]
                     [quantum.core.fn                         :as fn
                       :refer [#?@(:clj [compr <- fn-> fn->>
                                         f*n with-do])
                              fn-nil juxt-kv withf->>]  ]
                     [quantum.core.logic                      :as logic
                       :refer [#?@(:clj [fn-not fn-or fn-and whenf whenf*n
                                         ifn if*n condf condf*n]) nnil?]]
                     [quantum.core.macros                     :as macros
                       :refer [#?@(:clj [defnt])]                        ]
                     [quantum.core.reducers                   :as red
                       :refer [#?@(:clj [reduce join])]                  ]
                     [quantum.core.type                       :as type
                       :refer [map-entry?
                               #?@(:clj [lseq? transient? editable?
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
                       :refer [count first second rest getr lasti index-of lasti
                               conj conj! contains? empty]               ]
                     [quantum.core.collections.base           :as base
                       :refer [kmap]                                     ]
                     [quantum.core.fn                         :as fn
                       :refer [compr <- fn-> fn->> f*n defcurried with-do]]
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
;=================================================={     ZIPPERS     }=====================================================
;=================================================={                 }=====================================================

(defalias zip-reduce* base/zip-reduce*)

(defn node* [x] (if (instance? fast_zip.core.ZipperLocation x) (zip/node x) x))

(defn zipper
  "General-purpose zipper."
  {:attribution "Alex Gunnarson"}
  [coll]
  (zip/zipper
    coll?
    seq
    (fn [node children]
      (cond
        (record? node)
        (core/reduce conj coll children)
        (map? node)
        (with-meta (join (empty node) children) (meta node))
        (map-entry? node)
        (map-entry (first children) (second children))
        (list? node)
        (apply list children)
        :else (with-meta (join (empty node) children) (meta node))))
    coll))

(defn zip-reduce [f init coll] (zip-reduce* f init (zipper coll)))

(defn zip-reduce-with
  [accumulator f post init coll loc]
  (let [loc* (volatile! (zip/down loc))]
    (post
      (core/reduce
        (fn [r x] (with-do (accumulator r (f x @loc*))
                    (vswap! loc* zip/right)))
        init coll))))

(defn zip-map-with [coll f loc]
  (let [loc* (volatile! (zip/down loc))]
    (map (fn [x] (with-do (f x @loc*)
                   (vswap! loc* zip/right))) coll)))

(defn zip-mapv
  "Like `mapv` but allows zip functions to be applied to each elem."
  [f coll]
  (loop [ret  (transient [])
         elem (-> coll zipper zip/down)]
    (if (nil? elem)
        (persistent! ret)
        (recur (conj! ret (f elem)) (zip/right elem)))))

(defn edit
  "A slightly more performant alternative to zip/edit"
  [^fast_zip.core.ZipperLocation loc f]
  (zip/replace loc (f (.-node loc))))
