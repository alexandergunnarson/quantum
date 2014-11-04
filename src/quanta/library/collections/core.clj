(ns quanta.library.collections.core
  (:require
    [quanta.library.function       :refer :all         ]
    [quanta.library.logic          :refer :all         ]
    [quanta.library.type           :refer :all         ]
    [quanta.library.error          :refer [try+ throw+]]
    [quanta.library.reducers       :refer :all         ]
    [quanta.library.data.vector    :refer [subvec+]    ]
    [quanta.library.string :as str                     ]
    [quanta.library.macros         :refer :all         ])
  (:import java.util.ArrayList)
  (:gen-class))

(set! *warn-on-reflection* true)

; ; java.util.Collection class to use as part of protocol

; TODO: Move macros to macro namespace

(def arr-types
  {:short    ShortArray
   :long     LongArray
   :float    FloatArray
   :int      IntArray
   :double   DoubleArray
   :boolean  BooleanArray
   :byte     ByteArray
   :char     CharArray
   :object   ObjectArray})

(def coll-search-types
  {:vec    clojure.lang.APersistentVector
   :rvec   clojure.lang.APersistentVector$RSeq 
   :list   clojure.lang.PersistentList
   :string String})


(defprotocol+ CollCount
  (count+ [coll]))

(extend-protocol CollCount
  clojure.lang.LazySeq
    (count+ [coll] (count* coll))
  clojure.lang.IPersistentList
    (count+ [coll] (count* coll))
  clojure.lang.Counted
    (count+ [coll] (count coll))
  Object
    (count+ [coll] (count coll)))

(defmacro extend-coll-count-for-type
  "With helpful hints from:
  http://www.learningclojure.com/2010/09/macros-and-type-hints-metadata-and.html"
  [type-key]
  (let [coll (with-meta (gensym) {:tag (-> type-key name (str "s"))})]
   `(extend-protocol CollCount (get ~arr-types ~type-key)
      (count+ [~coll]
        (alength ~coll)))))
(defn extend-coll-count-to-all-arr! []
  (reduce-kv
    (fn [ret type type-class]
      ;(println type)
      (eval `(extend-coll-count-for-type ~type)))
    nil arr-types))

(extend-coll-count-to-all-arr!)
;___________________________________________________________________________________________________________________________________
;=================================================={         RETRIEVAL        }=====================================================
;=================================================={     get, first, rest     }=====================================================
(defprotocol+ CollRetrieve
  (getr+    [coll a] [coll a b] "Get range")
  (get+     [coll n] [coll n if-not-found])
  (first+   [coll])
  (second+  [coll])
  (rest+    [coll])
  (butlast+ [coll])
  (last+    [coll])) 


(defmacro extend-coll-retrieve-for-type
  [type-key]
  (let [coll (with-meta (gensym) {:tag (-> type-key name (str "s"))})]
   `(extend-protocol CollRetrieve (get ~arr-types ~type-key)
      (get+
        ([~coll n#]  (get+ ~coll n# nil))
        ([~coll n# if-not-found#]
          (try+ (aget ~coll n#)
            (catch ArrayIndexOutOfBoundsException e# if-not-found#))))
      (first+  [~coll]  (aget ~coll 0))
      (second+ [~coll]  (aget ~coll 1))
      (last+   [~coll]  (aget ~coll (-> ~coll alength dec))))))
(defn extend-coll-retrieve-to-all-arr! []
  (reduce-kv
    (fn [ret type type-class]
      ;(println type)
      (eval `(extend-coll-retrieve-for-type ~type)))
    nil arr-types))

(extend-coll-retrieve-to-all-arr!)

; take-up-to (combination of getr and index-of + 1)

(extend-protocol CollRetrieve ArrayList
  (getr+ [^ArrayList coll a b] (.subList ^ArrayList coll a b))
  (get+
    ([^ArrayList coll n]  (get+ ^ArrayList coll n nil))
    ([^ArrayList coll n if-not-found]
      (try+ (.get ^ArrayList coll n)
        (catch ArrayIndexOutOfBoundsException e# if-not-found))))
  (first+  [^ArrayList coll]  (get+ ^ArrayList coll 0))
  (second+ [^ArrayList coll]  (get+ ^ArrayList coll 1))
  (last+   [^ArrayList coll]  (get+ ^ArrayList coll (-> coll count+ dec))))
; extend first+ to use /first/ when the object is unknown.
; first+ with non-collection items (java.util.Collection) will return itself.

(extend-protocol-for-all CollRetrieve
  [clojure.lang.Delay
   clojure.core.protocols.CollReduce
   quanta.library.reducers.Folder]
    (getr+    [coll a b] (->> coll (take+ b) (drop+ a)))
    (first+   [coll] (take+ 1 coll))
    (rest+    [coll] (drop+ 1 coll))
    (butlast+ [coll] (drop-last+ 1 coll))
    (last+    [coll] (take-last+ 1 coll)))
(extend-protocol CollRetrieve
  clojure.lang.IPersistentVector
    (getr+    ([coll a b] (subvec+ coll a b))
              ([coll a]   (subvec+ coll a (-> coll count))))
    (get+ 
      ([coll n] (get+ coll n nil))
      ([coll n if-not-found] (get coll n if-not-found)))
    (first+   [coll] (get coll 0))
    (second+  [coll] (get coll 1))
    (rest+    [coll] (subvec+ coll 1))
    (butlast+ [coll] (whenf coll nempty? pop))
    (last+    [coll] (peek coll))
  ; sorted-map or sorted-set getr+ like subseq
  clojure.lang.IPersistentMap
    (get+ 
      ([coll n] (get+ coll n nil))
      ([coll n if-not-found] (get coll n if-not-found)))
  clojure.lang.IPersistentCollection
    (getr+
      ([coll a]   (->> coll (drop a)))
      ([coll a b] (->> coll (take b) (drop a))))
    (first+   [coll] (nth coll 0)) ; perhaps implement in terms of /reduce/ ; 
    (second+  [coll] (get coll 1))
    (rest+    [coll] (rest coll))
    (butlast+ [coll] (butlast coll))
    (last+    [coll] (last coll))
    (get+
      ([coll n] (get+ coll n nil))
      ([coll n if-not-found] (nth coll n if-not-found)))
  String
    (first+   [coll] (str/subs+ coll 0 1))
    (rest+    [coll] (str/subs+ coll 1))
    (butlast+ [coll] (str/subs+ coll 0 (-> coll count+ dec)))
    (last+    [coll] (str/subs+ coll (-> coll count+ dec)))
    (getr+    [coll a b] (str/subs+ coll a (- b a)))
  Object ; "default"
    (get+
      ([obj n] (get+ obj n nil))
      ([obj n if-not-found] (get obj n)))
    (first+   [obj] obj)
    (last+    [obj] obj)
    (butlast+ [obj] obj)
  nil
    (get+     [obj n] obj)
    (first+   [obj]   obj)
    (last+    [obj]   obj)
    (butlast+ [obj]   obj))

(defn gets+ [coll & indices]
  (->> indices
       ; determine threshold of transient-persistent efficiency
       ; based on number of indices to retrieve
       (reduce (fn [ret ind] (conj! ret (get+ coll ind)))
         (transient []))
       persistent!))

(def  pop+  butlast+)
(def  peek+ last+)

(defn getf+ [n] (f*n get+ n))
;___________________________________________________________________________________________________________________________________
;=================================================={        SEARCH/FIND       }=====================================================
;=================================================={    index-of, contains?   }=====================================================
(defprotocol+ CollSearch
  (index-of+      [coll elem])
  (last-index-of+ [coll elem]))

(defmacro extend-coll-search-for-type
  [type-key]
  (let [type# (-> coll-search-types (get type-key) name-from-class)
        coll (with-meta (gensym)
               {:tag type#})
        elem (with-meta (gensym)
               {:tag (if (= type# 'java.lang.String)
                         'String
                         'Object)})]
   `(extend-protocol CollSearch (get ~coll-search-types ~type-key)
      (index-of+      [~coll ~elem] (.indexOf     ~coll ~elem))
      (last-index-of+ [~coll ~elem] (.lastIndexOf ~coll ~elem)))))
(defn extend-coll-search-to-all-types! []
  (reduce-kv
    (fn [ret type type-class]
      (eval `(extend-coll-search-for-type ~type)))
    nil coll-search-types))

(extend-coll-search-to-all-types!)

(defn third [coll] (-> coll rest+ rest+ first+))
; If the array is not sorted:
; java.util.Arrays.asList(theArray).indexOf(o)
; If the array is sorted, you can make use of a binary search for performance:
; java.util.Arrays.binarySearch(theArray, o)
