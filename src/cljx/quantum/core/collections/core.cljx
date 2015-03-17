(ns quantum.core.collections.core
  (:require
    [quantum.core.error
      #+clj :refer
      #+clj [try+ throw+]]
    [quantum.core.function :as fn :refer
      #+clj  [compr f*n fn* unary fn->> fn-> <- defprotocol+ extend-protocol+]
      #+cljs [compr f*n fn* unary]
      #+cljs :refer-macros
      #+cljs [fn->> fn-> <-]]
    [quantum.core.logic :as log :refer
      #+clj  [splice-or fn-and fn-or fn-not ifn if*n whenc whenf whenf*n whencf*n
              condf condf*n condfc nnil? nempty?]
      #+cljs [splice-or fn-and fn-or fn-not nnil? nempty?]
      #+cljs :refer-macros
      #+cljs [ifn if*n whenc whenf whenf*n whencf*n condf condf*n condfc]]
    [quantum.core.ns :as ns :refer
      #+clj [alias-ns defalias]
      #+cljs [Exception IllegalArgumentException
              Nil Bool Num ExactNum Int Decimal Key Vec Set
              ArrList TreeMap LSeq Regex Editable Transient Queue Map]]
    [quantum.core.reducers :as red :refer
      #+clj  [map+ reduce+ filter+ remove+ take+ take-while+ take-last+ drop-last+
              count* reducei+ fold+ range+ drop+ for+]
      #+cljs [map+ reduce+ filter+ remove+ take+ take-while+
              count* reducei+ fold+ range+ drop+]
      #+cljs :refer-macros
      #+cljs [for+]]
    [quantum.core.string :as str]
    [quantum.core.type :as type :refer
     #+cljs [name-from-class class]
     #+clj  [name-from-class ShortArray LongArray FloatArray IntArray DoubleArray BooleanArray ByteArray CharArray ObjectArray]]
    [quantum.core.data.vector :as vec :refer
      [subvec+ catvec]]
    [quantum.core.macros
      #+clj  :refer
      #+cljs :refer-macros
      [extend-protocol-type extend-protocol-types extend-protocol-for-all]])
  #+clj
  (:import
    clojure.core.Vec
    java.util.ArrayList clojure.lang.Keyword
    (quantum.core.ns
      Nil Bool Num ExactNum Int Decimal Key Set
             ArrList TreeMap LSeq Regex Editable Transient Queue Map))
  #+clj (:gen-class))

#+clj (set! *warn-on-reflection* true)

; ; java.util.Collection class to use as part of protocol

(defn lasti
  "Last index of a coll."
  [coll]
  (-> coll count dec))

#+clj
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
  {:vec    #+clj clojure.lang.APersistentVector      #+cljs Vec
   :rvec   #+clj clojure.lang.APersistentVector$RSeq #+cljs cljs.core/RSeq
   :list   #+clj clojure.lang.PersistentList         #+cljs cljs.core/List
   #+clj :string #+clj String}) ; because |js/String| doesn't work and |string| doesn't work outside of a protocol

(#+clj  defprotocol
 #+cljs defprotocol
  CollCount
  (count+ [coll]))

(#+clj  extend-protocol
 #+cljs extend-protocol CollCount
  #+clj  java.lang.CharSequence
  #+cljs string
    (count+ [coll] (count coll))
  #+clj  clojure.lang.LazySeq
  #+cljs cljs.core/LazySeq
    (count+ [coll] (count* coll))
  #+clj  java.util.Collection
 ; #+cljs cljs.core/ICollection ; probably can't extend protocols this way in ClojureScript
  ; TODO: what about Java collections that are counted?
  #+clj  (count+ [coll] (count coll))
  nil
    (count+ [coll] 0)
  #+clj  Object
  #+cljs default
    (count+ [coll] 1))

#+clj 
(defmacro extend-coll-count-for-type
  "With helpful hints from:
  http://www.learningclojure.com/2010/09/macros-and-type-hints-metadata-and.html"
  [type-key]
  (let [coll (with-meta (gensym) {:tag (-> type-key name (str "s"))})]
   `(extend-protocol CollCount (get ~arr-types ~type-key)
      (count+ [~coll]
        (alength ~coll)))))
#+clj 
(defmacro extend-coll-count-to-all-arr! []
  (reduce-kv
    (fn [ret type type-class]
      ;(println type)
      (eval `(extend-coll-count-for-type ~type)))
    nil arr-types))
#+clj 
(extend-coll-count-to-all-arr!)
;___________________________________________________________________________________________________________________________________
;=================================================={         RETRIEVAL        }=====================================================
;=================================================={     get, first, rest     }=====================================================
(#+clj  defprotocol
 #+cljs defprotocol CollRetrieve
  (getr+    [coll a] [coll a b] "Get range")
  (get+     [coll n] [coll n if-not-found])
  (first+   [coll])
  (second+  [coll])
  (rest+    [coll])
  (butlast+ [coll])
  (last+    [coll])) 

#+clj
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

#+clj
(defmacro extend-coll-retrieve-to-all-arr! []
  (reduce-kv
    (fn [ret type type-class]
      ;(println type)
      (eval `(extend-coll-retrieve-for-type ~type)))
    nil arr-types))

#+clj
(extend-coll-retrieve-to-all-arr!)

; take-up-to (combination of getr and index-of + 1)

#+clj ; TODO: port to cljs for Arrays and ArrayLists
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

(extend-protocol CollRetrieve
  Keyword
    (rest+ [k]
      (rest+ (name k)))
  #+clj String #+cljs string
    (first+   [coll]
      (try (subs coll 0 1)
        #+clj (catch StringIndexOutOfBoundsException _ nil)))
    (second+  [coll]
      (try (subs coll 1 2)
        #+clj (catch StringIndexOutOfBoundsException _() nil)))
    (rest+    [coll]
      (try (subs coll 1 (count+ coll))
        #+clj (catch StringIndexOutOfBoundsException _ nil)))
    (butlast+ [coll] (subs coll 0 (-> coll count+ dec)))
    (last+    [coll] (subs coll   (-> coll count+ dec)))
    (getr+    [coll a b] (str/subs+ coll a (inc (- b a))))) ; now inclusive range with |inc|; this could break lots of things

#+clj
(extend-protocol-for-all CollRetrieve
  [clojure.core.protocols.CollReduce
   ;#+cljs cljs.core/IReduce ; this is why...
   quantum.core.reducers.Folder]
    (getr+    [coll a b] (->> coll (take+ b) (drop+ a)))
    (first+   [coll]     (take+ 1 coll))
    (rest+    [coll]     (drop+ 1 coll))
    (butlast+ [coll]     (drop-last+ 1 coll))
    (last+    [coll]     (take-last+ 1 coll)))

#+clj
(extend-protocol CollRetrieve
  clojure.lang.IPersistentVector
    (getr+    ([coll a b] (subvec+ coll a b))
              ([coll a]   (subvec+ coll a (-> coll count))))
    (get+ 
      ([coll n] (get+ coll n nil))
      ([coll n if-not-found] (get coll n if-not-found)))
    (first+   [coll] (get coll 0))
    (second+  [coll] (get coll 1))
    (rest+    [coll] (subvec+ coll 1 (count coll)))
    (butlast+ [coll] (whenf coll nempty? pop))
    (last+    [coll] (peek coll))
  clojure.lang.IPersistentMap
    (get+ 
      ([coll n]              (get+ coll n nil))
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
      ([coll n if-not-found] (nth coll n if-not-found))))

; ===== REDUCERS =====

#+cljs
(extend-protocol CollRetrieve
  cljs.core/Delay
    (getr+          [coll a b] (->> coll (take+ b) (drop+ a)))
    (first+         [coll]     (take+ 1 coll))
    (rest+          [coll]     (drop+ 1 coll))
    ;#+clj (butlast+ [coll]     (drop-last+ 1 coll))
    ;#+clj (last+    [coll]     (take-last+ 1 coll))
    )
; _____________________________________________________________________________________
; ====================================== VECTOR =======================================
; `````````````````````````````````````````````````````````````````````````````````````
#+cljs
(extend-protocol CollRetrieve
  cljs.core/PersistentVector
    (getr+    ([coll a b] (subvec+ coll a b))
              ([coll a]   (subvec+ coll a (-> coll count))))
    (get+ 
      ([coll n] (get+ coll n nil))
      ([coll n if-not-found] (get coll n if-not-found)))
    (first+   [coll] (get coll 0))
    (second+  [coll] (get coll 1))
    (rest+    [coll] (subvec+ coll 1 (count coll)))
    (butlast+ [coll] (whenf coll nempty? pop))
    (last+    [coll] (peek coll)))

#+cljs
(extend-protocol CollRetrieve
  cljs.core/TransientVector
    (getr+    ([coll a b] (subvec+ coll a b))
              ([coll a]   (subvec+ coll a (-> coll count))))
    (get+ 
      ([coll n] (get+ coll n nil))
      ([coll n if-not-found] (get coll n if-not-found)))
    (first+   [coll] (get coll 0))
    (second+  [coll] (get coll 1))
    (rest+    [coll] (subvec+ coll 1 (count coll)))
    (butlast+ [coll] (whenf coll nempty? pop))
    (last+    [coll] (peek coll)))

; _____________________________________________________________________________________
; ======================================== MAP ========================================
; `````````````````````````````````````````````````````````````````````````````````````
#+cljs
(extend-protocol CollRetrieve
  PersistentArrayMap
    (get+ 
      ([coll n]              (get+ coll n nil))
      ([coll n if-not-found] (get coll n if-not-found))))

#+cljs
(extend-protocol CollRetrieve
  TransientArrayMap
    (get+ 
      ([coll n]              (get+ coll n nil))
      ([coll n if-not-found] (get coll n if-not-found))))

#+cljs
(extend-protocol CollRetrieve
  PersistentHashMap
    (get+ 
      ([coll n]              (get+ coll n nil))
      ([coll n if-not-found] (get coll n if-not-found))))

#+cljs
(extend-protocol CollRetrieve
  TransientHashMap
    (get+ 
      ([coll n]              (get+ coll n nil))
      ([coll n if-not-found] (get coll n if-not-found))))

#+cljs
(extend-protocol CollRetrieve
  PersistentTreeMap
    (get+ 
      ([coll n]              (get+ coll n nil))
      ([coll n if-not-found] (get coll n if-not-found))))

; _____________________________________________________________________________________
; ==================================== COLLECTION =====================================
; `````````````````````````````````````````````````````````````````````````````````````
#+cljs
(extend-protocol CollRetrieve
  cljs.core/List
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
      ([coll n if-not-found] (nth coll n if-not-found))))

#+cljs
(extend-protocol CollRetrieve
  cljs.core/EmptyList
    (first+   [coll] nil))

#+cljs
(extend-protocol CollRetrieve
  cljs.core/PersistentQueue
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
      ([coll n if-not-found] (nth coll n if-not-found))))

#+cljs
(extend-protocol CollRetrieve
  cljs.core/LazySeq
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
      ([coll n if-not-found] (nth coll n if-not-found))))

#+cljs
(extend-protocol CollRetrieve
  cljs.core/KeySeq
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
      ([coll n if-not-found] (nth coll n if-not-found))))

#+cljs
(extend-protocol CollRetrieve
  cljs.core/ValSeq
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
      ([coll n if-not-found] (nth coll n if-not-found))))

#+cljs
(extend-protocol CollRetrieve
  cljs.core/IndexedSeq
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
      ([coll n if-not-found] (nth coll n if-not-found))))

#+cljs
(extend-protocol CollRetrieve
  cljs.core/ChunkedSeq
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
      ([coll n if-not-found] (nth coll n if-not-found))))

#+cljs
(extend-protocol CollRetrieve
  cljs.core/PersistentHashSet
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
      ([coll n if-not-found] (nth coll n if-not-found))))

#+cljs
(extend-protocol CollRetrieve
  cljs.core/TransientHashSet
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
      ([coll n if-not-found] (nth coll n if-not-found))))

#+cljs
(extend-protocol CollRetrieve
  cljs.core/PersistentTreeSet
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
      ([coll n if-not-found] (nth coll n if-not-found))))

#+cljs
(extend-protocol CollRetrieve
  cljs.core/ArrayList
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
      ([coll n if-not-found] (nth coll n if-not-found))))

(extend-protocol CollRetrieve
  ; sorted-map or sorted-set getr+ like subseq
  #+clj  Object ; "default"
  #+cljs default
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
(def  popr+ butlast+)
(def  popl+ rest+)
(def  peek+ last+)

(defn getf+ [n] (f*n get+ n))
;___________________________________________________________________________________________________________________________________
;=================================================={        SEARCH/FIND       }=====================================================
;=================================================={    index-of, contains?   }=====================================================
(#+clj  defprotocol
 #+cljs defprotocol
  CollSearch
    (index-of+      [coll elem])
    (last-index-of+ [coll elem]))

#+clj
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

#+clj
(defn extend-coll-search-to-all-types! []
  (reduce-kv
    (fn [ret type type-class]
      (eval `(extend-coll-search-for-type ~type)))
    nil coll-search-types))

#+clj (extend-coll-search-to-all-types!)

#+cljs
(extend-protocol CollSearch
  cljs.core/PersistentVector
    (index-of+      [coll elem] (.indexOf     coll elem))
    (last-index-of+ [coll elem] (.lastIndexOf coll elem))
  cljs.core/TransientVector
    (index-of+      [coll elem] (.indexOf     coll elem))
    (last-index-of+ [coll elem] (.lastIndexOf coll elem))
  string
    (index-of+      [coll elem] (.indexOf     coll elem))
    (last-index-of+ [coll elem] (.lastIndexOf coll elem)))

(defn third [coll] (-> coll rest+ rest+ first+))
; If the array is not sorted:
; java.util.Arrays.asList(theArray).indexOf(o)
; If the array is sorted, you can make use of a binary search for performance:
; java.util.Arrays.binarySearch(theArray, o)

(defmacro doseqi
  "Loops over a set of values, binding index-sym to the 0-based index
   of each value"
  {:todo ["Implement in terms of |reduce|"]}
  ([[val-sym values index-sym] & code]
  `(loop [vals#      (seq ~values) 
          ~index-sym (long 0)]
     (if vals#
         (let [~val-sym (first vals#)]
               ~@code
               (recur (next vals#) (inc ~index-sym)))
         nil))))


; (defn conjl [coll elem]
;   (condf coll
;     list?   (f*n cons elem)
;     vector? (partial into+ (vector elem))
;     coll?   (partial into+ (vector elem))))