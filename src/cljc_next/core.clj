(ns
  ^{:doc "Retakes on core collections functions like first, rest,
          get, nth, last, index-of, etc.

          Also includes innovative functions like getr, etc."}
  quantum.core.collections.core
  #?(:clj (:refer-clojure :exclude [vector hash-map rest count first second butlast last]))
  (:require
    [clojure.core :as core]
    [quantum.core.error
      #?@(:clj [:refer [try+ throw+]])]
    [quantum.core.function :as fn :refer
      #?@(:clj  [[compr f*n fn* unary firsta rfn fn->> fn-> <-]]
          :cljs [[compr f*n fn* unary firsta]
                 :refer-macros
                 [fn->> fn-> <-]])]
    [quantum.core.logic :as log :refer
      #?@(:clj  [[splice-or fn-and fn-or fn-not ifn if*n whenc whenf whenf*n whencf*n
                  condf condfc condf*n nnil? nempty?]]
          :cljs [[splice-or fn-and fn-or fn-not nnil? nempty?]
                 :refer-macros
                 [ifn if*n whenc whenf whenf*n whencf*n condf condfc condf*n]])]
    [quantum.core.ns :as ns :refer
      #?(:clj  [alias-ns defalias]
         :cljs [Exception IllegalArgumentException
                Nil Bool Num ExactNum Int Decimal Key Vec Set
                ArrList TreeMap LSeq Regex Editable Transient Queue Map])]
    [quantum.core.reducers :as red :refer
      #?@(:clj  [[map+ reduce+ filter+ remove+ take+ take-while+ taker+ dropr+
                  count* fold+ range+ drop+ for+]]
          :cljs [[map+ reduce+ filter+ remove+ take+ take-while+
                  fold+ range+ drop+]
                 :refer-macros [for+]])]
    [quantum.core.string :as str]
    [quantum.core.data.set :as set]
    [quantum.core.type     :as type :refer
      [#?(:clj bigint?) #?(:cljs class) instance+? array-list? boolean? double? map-entry?
       sorted-map? queue? lseq? coll+? pattern? regex? editable?
       transient? #?(:clj should-transientize?) name-from-class #?(:clj arr-types)]
      #?@(:cljs [:refer-macros [should-transientize?]])]
    [quantum.core.data.vector :as vec :refer
      [subvec+ catvec vector+]]
    [quantum.core.macros
      #?(:clj  :refer
         :cljs :refer-macros)
      [extend-protocol-type extend-protocol-types extend-protocol-for-all defnt]]
    #?(:clj [clj-tuple :as tup]))
  #?@(:clj
      [(:import
        clojure.core.Vec
        java.util.ArrayList clojure.lang.Keyword
        (quantum.core.ns
          Nil Bool Num ExactNum Int Decimal Key Set
                 ArrList TreeMap LSeq Regex Editable Transient Queue Map))
       (:gen-class)]))

#?(:clj (ns/require-all *ns* :clj))

; TODO Queues need support

; TODO need to somehow incorporate |vector++| and |vector+|
; #+clj (defalias vector   tup/vector)
; #+clj (defalias hash-map tup/hash-map)

(defn lasti
  "Last index of a coll."
  [coll]
  (-> coll count dec))

(defprotocol
  CollCount
  (count+ [coll]))

(extend-protocol CollCount
  nil
    (count+ [coll] 0)
  #?(:clj  Object
     :cljs default)
    (count+ [coll] (count coll)))

#?(:clj 
  (defmacro extend-coll-count-for-type
    "With helpful hints from:
    http://www.learningclojure.com/2010/09/macros-and-type-hints-metadata-and.html"
    [type-key]
    (let [coll (with-meta (gensym) {:tag (-> type-key name (str "s"))})]
     `(extend-protocol CollCount (get ~arr-types ~type-key)
        (count+ [~coll]
          (alength ~coll))))))

#?(:clj 
  (defmacro extend-coll-count-to-all-arr! []
    (reduce-kv
      (fn [ret type type-class]
        ;(println type)
        (eval `(extend-coll-count-for-type ~type)))
      nil arr-types)))

#?(:clj (extend-coll-count-to-all-arr!))
;___________________________________________________________________________________________________________________________________
;=================================================={         RETRIEVAL        }=====================================================
;=================================================={     get, first, rest     }=====================================================
(defprotocol CollRetrieve
  (getr+    [coll a] [coll a b] "Get range")
  (get+     [coll n] [coll n if-not-found])
  (first+   [coll])
  (second+  [coll])
  (rest+    [coll])
  (butlast+ [coll])
  (last+    [coll])) 

#?(:clj
  (defmacro extend-coll-retrieve-for-type
    [type-key]
    (let [coll (with-meta (gensym) {:tag (-> type-key name (str "s"))})]
     `(extend-protocol CollRetrieve (get ~arr-types ~type-key)
        (get+
          ([~coll n#]  (get+ ~coll n# nil))
          ([~coll n# if-not-found#]
            (try (aget ~coll n#)
              (catch ArrayIndexOutOfBoundsException e# if-not-found#))))
        (first+  [~coll]  (aget ~coll 0))
        (second+ [~coll]  (aget ~coll 1))
        (last+   [~coll]  (aget ~coll (-> ~coll alength dec)))))))

#?(:clj
  (defmacro extend-coll-retrieve-to-all-arr! []
    (reduce-kv
      (fn [ret type type-class]
        ;(println type)
        (eval `(extend-coll-retrieve-for-type ~type)))
      nil arr-types)))

#?(:clj
  (extend-coll-retrieve-to-all-arr!))

; take-up-to (combination of getr and index-of + 1)

#?(:clj ; TODO: port to cljs for Arrays and ArrayLists
  (extend-protocol CollRetrieve ArrayList
    
    (get+
      ([^ArrayList coll n]  (get+ ^ArrayList coll n nil))
      ([^ArrayList coll n if-not-found]
        (try (.get ^ArrayList coll n)
          (catch ArrayIndexOutOfBoundsException e# if-not-found))))
    (first+  [^ArrayList coll]  (get+ ^ArrayList coll 0))
    (second+ [^ArrayList coll]  (get+ ^ArrayList coll 1))
    (last+   [^ArrayList coll]  (get+ ^ArrayList coll (-> coll count+ dec)))))
; extend first+ to use /first/ when the object is unknown.
; first+ with non-collection items (java.util.Collection) will return itself.

(def count core/count)

(defnt rest
  keyword?  ([k] (-> k name rest))
  string?   ([coll]
              (try (subs coll 1 (count coll))
                #?(:clj (catch StringIndexOutOfBoundsException _ nil))))
  qreducer? ([coll] (drop+ 1 coll))
  vec?      ([coll] (subvec+ coll 1 (count coll)))
  nil?      ([coll] nil)) 

(defnt get
  vec? (([coll n]              (get coll n nil))
        ([coll n if-not-found] (core/get coll n if-not-found))))

(defnt first
  string?  ([coll]
             (try (subs coll 0 1)
               #?(:clj (catch StringIndexOutOfBoundsException _ nil))))
  vec? ([coll] (get coll 0)))

(defnt second
  string?   ([coll]
              (try (subs coll 1 2)
                #?(:clj (catch StringIndexOutOfBoundsException _ nil))))
  qreducer? ([coll] (take+ 1 coll))
  vec?      ([coll] (core/get coll 1)))

(defnt butlast
  string?   ([coll] (subs coll 0 (-> coll count dec)))
  qreducer? ([coll] (dropr+ 1 coll))
  vec?      ([coll] (whenf coll nempty? pop)))

(defnt last
  string?   ([coll] (subs coll   (-> coll count dec)))
  qreducer? ([coll] (taker+ 1 coll))
  vec?      ([coll] (peek coll)))
    
(defnt getr
  ; inclusive range
  string?     ([coll a b] (str/subs+ coll a (inc (- b a))))
  qreducer?   ([coll a b] (->> coll (dtake+ b) (drop+ a)))
  array-list? ([coll a b] (.subList coll a b))
  vec?        (([coll a b] (subvec+ coll a b))
               ([coll a]   (subvec+ coll a (-> coll count))))) 

; _____________________________________________________________________________________
; ====================================== VECTOR =======================================
; `````````````````````````````````````````````````````````````````````````````````````

; _____________________________________________________________________________________
; ======================================== MAP ========================================
; `````````````````````````````````````````````````````````````````````````````````````
; TODO make more efficient
; TODO This is CLJS compatible
#?(:clj
(doseq [type (:map type/types)]
  (extend type CollRetrieve
    {:get+
      (fn
        ([coll n]              (get+ coll n nil))
        ([coll n if-not-found] (get coll n if-not-found)))})))
; _____________________________________________________________________________________
; ==================================== COLLECTION =====================================
; `````````````````````````````````````````````````````````````````````````````````````
; TODO this is CLJS compatible
#?(:clj
(doseq #?(:clj  [type [clojure.lang.IPersistentCollection]]
          :cljs [type (set/union (:iseq type/types) (:set type/types))])
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
          ([coll n if-not-found] (nth coll n if-not-found)))})))

#?(:cljs
  (extend-protocol CollRetrieve
    cljs.core/EmptyList
      (first+   [coll] nil)))

(extend-protocol CollRetrieve
  ; sorted-map or sorted-set getr+ like subseq
 #?(:clj  Object ; "default"
    :cljs default)
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

#?(:clj
(defmacro extend1 [type0 protocol0 fn-map]
 `(doseq [[f-key# f#] ~fn-map]
    (let [f-name# (-> f-key# name symbol)]
      (extend-protocol ~protocol0
        ~type0 (f-name# (rest 'f#)))))))

; TODO implement more...
#?(:clj
  (doseq [type (:vec type/types)]
    (extend type CollSearch
      {:index-of+      (fn [coll elem] (.indexOf     ^clojure.lang.IPersistentVector coll elem))
       :last-index-of+ (fn [coll elem] (.lastIndexOf ^clojure.lang.IPersistentVector coll elem))})))

(extend-protocol CollSearch
  #?(:clj String :cljs string)
    (index-of+      [coll elem] (.indexOf     ^String coll ^String elem))
    (last-index-of+ [coll elem] (.lastIndexOf ^String coll ^String elem)))

(defn third [coll] (-> coll rest+ rest+ first+))

;___________________________________________________________________________________________________________________________________
;=================================================={           MODIFY         }=====================================================
;=================================================={    conjl, conjr, etc.    }=====================================================
; TODO This is a mess and needs to be "hygienized" by macros
; that determine clj variadic support vs. cljs non, and act accordingly. Also multiple arity builder  
; Variadic protocols aren't supported by CLJS
(defprotocol CollMod
  (conjl
    #?@(:clj ([coll a]
              [coll a b]
              [coll a b c]
              [coll a b c d] 
              [coll a b c d e])
       :cljs ([coll args])))
  (conjr-
    #?@(:clj  ([coll arg] [coll arg & args])
       :cljs ([coll args]))))

;--------------------------------------------------{           CONJL          }-----------------------------------------------------

(extend-protocol-for-all CollMod
  [java.util.List clojure.lang.IPersistentList
   clojure.lang.PersistentList] ; java.util.List isn't enough; clojure.lang.IPersistentList isn't enough
    (conjl
      ([coll a]           (->> coll (cons a)                                             ))
      ([coll a b]         (->> coll (cons b) (cons a)                                    ))
      ([coll a b c]       (->> coll (cons c) (cons b) (cons a)                           ))
      ([coll a b c d]     (->> coll (cons d) (cons c) (cons b) (cons a)                  ))
      ([coll a b c d e]   (->> coll (cons e) (cons d) (cons c) (cons b) (cons a)         ))
      ([coll a b c d e f] (->> coll (cons f) (cons e) (cons d) (cons c) (cons b) (cons a))))
  [clojure.lang.IPersistentVector]
    (conjl
      ([coll a]                  (catvec (vector+ a          ) coll))
      ([coll a b]                (catvec (vector+ a b        ) coll))
      ([coll a b c]              (catvec (vector+ a b c      ) coll))
      ([coll a b c d]            (catvec (vector+ a b c d    ) coll))
      ([coll a b c d e]          (catvec (vector+ a b c d e  ) coll))
      ([coll a b c d e f]        (catvec (vector+ a b c d e f) coll))
      ([coll a b c d e f & args] (catvec (apply vector+ args ) coll))))

;--------------------------------------------------{           CONJR          }-----------------------------------------------------
; TODO CLJS compatible
#?(:clj
(doseq [type (:vec type/types)]
  (extend type CollMod
    {:conjr-
     #?(:clj  conj
        :cljs (fn [coll args] (apply conj coll args)))})))

; TODO CLJS compatible
#?(:clj
(doseq [type (:iseq type/types)]
  (extend type CollMod
    {:conjr-
      #?(:clj
           (fn ([coll arg]        (concat coll (list arg)))
               ([coll arg & args] (concat coll (cons arg args))))
         :cljs
           (fn [coll args] (concat coll args)))})))

; TODO: conjl with cons or conj, etc.


; TODO add arity
;(def conjl #?(:clj conjl- :cljs (fn [coll & args] (conjr- coll args))))
(def conjr #?(:clj conjr- :cljs (fn [coll & args] (conjl- coll args))))

(def doto! swap!)

; If the array is not sorted:
; java.util.Arrays.asList(theArray).indexOf(o)
; If the array is sorted, you can make use of a binary search for performance:
; java.util.Arrays.binarySearch(theArray, o)



