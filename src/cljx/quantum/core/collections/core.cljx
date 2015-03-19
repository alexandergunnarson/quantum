(ns
  ^{:doc "Retakes on core collections functions like first, rest,
          get, nth, last, index-of, etc.

          Also includes innovative functions like getr, etc."}
  quantum.core.collections.core
  #+clj (:refer-clojure :exclude [vector hash-map])
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
    [quantum.core.data.set :as set]
    [quantum.core.type :as type :refer
     #+cljs [name-from-class class]
     #+clj  [name-from-class arr-types ShortArray LongArray FloatArray IntArray DoubleArray BooleanArray ByteArray CharArray ObjectArray]]
    [quantum.core.data.vector :as vec :refer
      [subvec+ catvec]]
    [quantum.core.macros
      #+clj  :refer
      #+cljs :refer-macros
      [extend-protocol-type extend-protocol-types extend-protocol-for-all]]
    #+clj [clj-tuple :as tup])
  #+clj
  (:import
    clojure.core.Vec
    java.util.ArrayList clojure.lang.Keyword
    (quantum.core.ns
      Nil Bool Num ExactNum Int Decimal Key Set
             ArrList TreeMap LSeq Regex Editable Transient Queue Map))
  #+clj (:gen-class))

#+clj (set! *warn-on-reflection* true)

; TODO need to somehow incorporate |vector++| and |vector+|
#+clj (defalias vector   tup/vector)
#+clj (defalias hash-map tup/hash-map)

(defn lasti
  "Last index of a coll."
  [coll]
  (-> coll count dec))

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
        #+clj (catch StringIndexOutOfBoundsException _ nil)))
    (rest+    [coll]
      (try (subs coll 1 (count+ coll))
        #+clj (catch StringIndexOutOfBoundsException _ nil)))
    (butlast+ [coll] (subs coll 0 (-> coll count+ dec)))
    (last+    [coll] (subs coll   (-> coll count+ dec)))
    (getr+    [coll a b] (str/subs+ coll a (inc (- b a))))) ; now inclusive range with |inc|; this could break lots of things

(extend-protocol-for-all CollRetrieve
  [#+clj  clojure.core.protocols.CollReduce
   #+clj  quantum.core.reducers.Folder
   #+cljs cljs.core/Delay]
          (getr+    [coll a b] (->> coll (take+ b) (drop+ a)))
          (first+   [coll]     (take+ 1 coll))
          (rest+    [coll]     (drop+ 1 coll))
    #+clj (butlast+ [coll]     (drop-last+ 1 coll))
    #+clj (last+    [coll]     (take-last+ 1 coll)))

; ===== REDUCERS =====

#+cljs
(extend-protocol CollRetrieve
  cljs.core/Delay
    (getr+          [coll a b] (->> coll (take+ b) (drop+ a)))
    (first+         [coll]     (take+ 1 coll))
    (rest+          [coll]     (drop+ 1 coll)))
; _____________________________________________________________________________________
; ====================================== VECTOR =======================================
; `````````````````````````````````````````````````````````````````````````````````````
(doseq  [type (:vec type/types)]
  (extend type CollRetrieve
    {:getr+
      (fn ([coll a b] (subvec+ coll a b))
          ([coll a]   (subvec+ coll a (-> coll count))))
    :first+   (fn [coll] (get coll 0)) ; perhaps implement in terms of |reduce|?
    :second+  (fn [coll] (get coll 1))
    :rest+    (fn [coll] (subvec+ coll 1 (count coll)))
    :butlast+ (fn [coll] (whenf coll nempty? pop))
    :last+    (fn [coll] (peek coll))
    :get+
      (fn ([coll n]              (get+ coll n nil))
          ([coll n if-not-found] (get coll n if-not-found)))}))
; _____________________________________________________________________________________
; ======================================== MAP ========================================
; `````````````````````````````````````````````````````````````````````````````````````
; TODO make more efficient
(doseq [type (:map type/types)]
  (extend type CollRetrieve
    {:get+
      (fn
        ([coll n]              (get+ coll n nil))
        ([coll n if-not-found] (get coll n if-not-found)))}))
; _____________________________________________________________________________________
; ==================================== COLLECTION =====================================
; `````````````````````````````````````````````````````````````````````````````````````
(doseq #+clj  [type [clojure.lang.IPersistentCollection]]
       #+cljs [type (set/union (:iseq type/types) (:set type/types))]
  (extend type CollRetrieve
    {:getr+
      (fn ([coll a]   (->> coll (drop a)))
          ([coll a b] (->> coll (take b) (drop a))))
    :first+   (fn [coll] (nth     coll 0) ) ; perhaps implement in terms of |reduce|?
    :second+  (fn [coll] (get     coll 1))
    :rest+    (fn [coll] (rest    coll))
    :butlast+ (fn [coll] (butlast coll))
    :last+    (fn [coll] (last    coll))
    :get+
      (fn ([coll n]              (get+ coll n nil))
          ([coll n if-not-found] (nth coll n if-not-found)))}))

#+cljs
(extend-protocol CollRetrieve
  cljs.core/EmptyList
    (first+   [coll] nil))

(extend-protocol CollRetrieve
  ; sorted-map or sorted-set getr+ like subseq
  #+clj  Object ; "default"
  #+cljs default
    (get+
      ([obj n]              (get+ obj n nil))
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
  (if (should-transientize? indices)
      (persistent!
        (reduce
          (fn [ret ind] (conj! ret (get+ coll ind)))
          (transient [])
          indices))
      (reduce
        (fn [ret ind] (conj ret (get+ coll ind)))
        []
        indices)))

(def  pop+  butlast+)
(def  popr+ butlast+)
(def  popl+ rest+)
(def  peek+ last+)

(defn getf+ [n] (f*n get+ n))
;___________________________________________________________________________________________________________________________________
;=================================================={        SEARCH/FIND       }=====================================================
;=================================================={    index-of, contains?   }=====================================================
(defprotocol CollSearch
  (index-of+      [coll elem])
  (last-index-of+ [coll elem]))

; TODO implement more...
#+clj
(doseq [type (:vec type/types)]
  (extend type CollSearch
    {:index-of+      (fn [coll elem] (.indexOf     ^clojure.lang.IPersistentVector coll elem))
     :last-index-of+ (fn [coll elem] (.lastIndexOf ^clojure.lang.IPersistentVector coll elem))}))

(extend-protocol CollSearch
  #+clj String #+cljs string
    (index-of+      [coll elem] (.indexOf     ^String coll elem))
    (last-index-of+ [coll elem] (.lastIndexOf ^String coll elem)))

(defn third [coll] (-> coll rest+ rest+ first+))

;___________________________________________________________________________________________________________________________________
;=================================================={           MODIFY         }=====================================================
;=================================================={    conjl, conjr, etc.    }=====================================================

(defprotocol CollMod
  (conjl [coll & args])
  (conjr [coll & args]))

(defn- conjl-list
  {:todo ["Add var-args"]}
  (fn ([coll a]           (->> coll (cons a)                                             ))
      ([coll a b]         (->> coll (cons b) (cons a)                                    ))
      ([coll a b c]       (->> coll (cons c) (cons b) (cons a)                           ))
      ([coll a b c d]     (->> coll (cons d) (cons c) (cons b) (cons a)                  ))
      ([coll a b c d e]   (->> coll (cons e) (cons d) (cons c) (cons b) (cons a)         ))
      ([coll a b c d e f] (->> coll (cons f) (cons e) (cons d) (cons c) (cons b) (cons a)))))

(doseq [type (:iseq type/types)]
  (extend type CollMod {:conjl conjl-list}))

(defn- conjl-vec
  ([coll a]                  (catvec (vector+ a          ) coll))
  ([coll a b]                (catvec (vector+ a b        ) coll))
  ([coll a b c]              (catvec (vector+ a b c      ) coll))
  ([coll a b c d]            (catvec (vector+ a b c d    ) coll))
  ([coll a b c d e]          (catvec (vector+ a b c d e  ) coll))
  ([coll a b c d e f]        (catvec (vector+ a b c d e f) coll))
  ([coll a b c d e f & args] (catvec (apply vector+ args ) coll)))

(doseq [type (:vec type/types)]
  (extend type CollMod {:conjl conjl-vec}))

(doseq [type (:vec type/types)]
  (extend type CollMod {:conjr conj}))


(def doto! swap!)

; If the array is not sorted:
; java.util.Arrays.asList(theArray).indexOf(o)
; If the array is sorted, you can make use of a binary search for performance:
; java.util.Arrays.binarySearch(theArray, o)



