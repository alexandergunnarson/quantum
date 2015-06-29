(ns
  ^{:doc "Retakes on core collections functions like first, rest,
          get, nth, last, index-of, etc.

          Also includes innovative functions like getr, etc."}
  quantum.core.collections.core
  (:refer-clojure :exclude
    [vector hash-map rest count first second butlast last get pop peek
     conj! assoc! dissoc!])
  (:require-quantum [ns err fn log logic red str map set macros type vec arr]))

; TODO Queues need support

; TODO need to somehow incorporate |vector++| and |vector+|
; #+clj (defalias vector   tup/vector)
; #+clj (defalias hash-map tup/hash-map)
;___________________________________________________________________________________________________________________________________
;=================================================={         RETRIEVAL        }=====================================================
;=================================================={     get, first, rest     }=====================================================
(defnt count
  array?   ([arr ] (alength arr))
  :default ([coll] (core/count coll)))

(defn lasti
  "Last index of a coll."
  [coll]
  (-> coll count dec))

#?(:clj
(defnt array-of-type
  object-array? ([obj n] (object-array n))
  byte-array?   ([obj n] (byte-array   n))
  long-array?   ([obj n] (long-array   n))))

(defnt getr
  ; inclusive range
  string?     ( [coll a b] (.substring coll a (unchecked-inc (long b))))
  qreducer?   ( [coll a b] (->> coll (take+ b) (drop+ a)))
  array-list? ( [coll a b] (.subList coll a b))
  vec?        (([coll a b] (subvec+ coll a (inc (long b))))
               ([coll a]   (subvec+ coll a (-> coll count))))
  #?@(:clj
 [array?      ( [coll a b]
                (let [arr-f (array-of-type coll (inc (- (int b) (int a))))]
                  (System/arraycopy coll (int a) arr-f (int 0)
                    (inc (- (int b) (int a))))
                  arr-f))])
  :default   (([coll a]   (->> coll (drop a)))
              ([coll a b] (->> coll (take b) (drop a))))) 

; TODO conflicting types... determine generality   
(defnt rest
  keyword?  ([k]    (-> k name rest))
  symbol?   ([s]    (-> s name rest))
  qreducer? ([coll] (drop+ 1 coll))
  string?   ([coll] (getr coll 1 (lasti coll)))
  vec?      ([coll] (getr coll 1 (lasti coll)))
  array?    ([coll] (getr coll 1 (lasti coll)))
  nil?      ([coll] nil)
  :default  ([coll] (core/rest coll))) 

(def popl rest)

(defnt get
  vec?        (([coll n]              (core/get coll n nil         ))
               ([coll n if-not-found] (core/get coll n if-not-found)))
  map?        (([coll n]              (core/get coll n nil         ))
               ([coll n if-not-found] (core/get coll n if-not-found)))
  string?     ( [coll n]              (.charAt  coll n             ))
  array-list? (([coll n]              (get      coll n nil         ))
               ([coll n if-not-found]
                 (try (.get coll n)
                   (catch ArrayIndexOutOfBoundsException e# if-not-found))))
  array?      ( [coll n]              (aget     coll n             ))
  listy?      (([coll n]              (nth      coll n nil         ))
               ([coll n if-not-found] (nth      coll n if-not-found)))
  :default    (([coll n]              (core/get coll n nil         ))
               ([coll n if-not-found] (core/get coll n if-not-found))))

(defnt assoc!
  array?               ([coll i v] (aset!       coll i v) coll)
  transient?           ([coll k v] (core/assoc! coll k v))
  [clojure.lang.IAtom] ([coll k v] (swap! coll assoc k v)))

(defnt dissoc!
  transient?           ([coll k] (core/dissoc! coll k))
  [clojure.lang.IAtom] ([coll k] (swap! coll dissoc k)))

(defnt conj!
  transient?           ([coll obj] (core/conj! coll obj))
  [clojure.lang.IAtom] ([coll obj] (swap! coll conj obj)))

(defnt update!
  :default ([coll i f] (assoc! coll i (f (get coll i)))))

(defnt first
  string?     ([coll] (get coll 0))
  vec?        ([coll] (get coll 0))
  array-list? ([coll] (get coll 0))
  array?      ([coll] (get coll 0))
  integral?   ([coll] coll)
  :default    ([coll] (core/first coll)))

(defnt second
  string?     ([coll] (get coll 1))
  qreducer?   ([coll] (take+ 1 coll))
  array-list? ([coll] (get coll 1))
  vec?        ([coll] (get coll 1))
  :default    ([coll] (core/second coll)))

(defnt butlast
  string?   ([coll] (getr coll 0 (-> coll lasti dec)))
  #?@(:clj
 [qreducer? ([coll] (dropr+ 1 coll))])
  vec?      ([coll] (whenf coll nempty? core/pop))
  :default  ([coll] (core/butlast coll)))

(def pop  butlast)
(def popr butlast)

(defnt last
  string?     ([coll] (get coll (lasti coll)))
  #?@(:clj
 [qreducer?   ([coll] (taker+ 1 coll))])
  vec?        ([coll] (core/peek coll))
  array-list? ([coll] (get coll (lasti coll)))
  :default    ([coll] (core/last coll)))
    
(def peek last)

(defnt index-of 
  vec?    ([coll elem] (whenc (.indexOf     coll elem) (extern (eq? -1)) nil))
  string? ([coll elem] (whenc (.indexOf     coll elem) (extern (eq? -1)) nil)))

(defnt last-index-of
  vec?    ([coll elem] (whenc (.lastIndexOf coll elem) (extern (eq? -1)) nil))
  string? ([coll elem] (whenc (.lastIndexOf coll elem) (extern (eq? -1)) nil)))

(defn gets [coll indices]
  (->> indices (red/map+ (partial get coll)) red/fold+))

(def third (f*n get 2))

(defn getf [n] (f*n get n))

;--------------------------------------------------{           CONJL          }-----------------------------------------------------

(defnt conjl
  list? (([coll a]           (->> coll (cons a)                                             ))
         ([coll a b]         (->> coll (cons b) (cons a)                                    ))
         ([coll a b c]       (->> coll (cons c) (cons b) (cons a)                           ))
         ([coll a b c d]     (->> coll (cons d) (cons c) (cons b) (cons a)                  ))
         ([coll a b c d e]   (->> coll (cons e) (cons d) (cons c) (cons b) (cons a)         ))
         ([coll a b c d e f] (->> coll (cons f) (cons e) (cons d) (cons c) (cons b) (cons a))))
  vec?  (([coll a]                  (catvec (vector+ a          ) coll))
         ([coll a b]                (catvec (vector+ a b        ) coll))
         ([coll a b c]              (catvec (vector+ a b c      ) coll))
         ([coll a b c d]            (catvec (vector+ a b c d    ) coll))
         ([coll a b c d e]          (catvec (vector+ a b c d e  ) coll))
         ([coll a b c d e f]        (catvec (vector+ a b c d e f) coll))
         ;([coll a b c d e f & args] (catvec (apply vector+ args ) coll))
         ))

(defnt conjr
  vec?   (([coll a]        (conj a))
          ([coll a b]      (conj a b))
          ([coll a b c]    (conj a b c))
          ;([coll a & args] (apply conj a args))
          )
  listy? (([coll a]        (concat coll (list a)))
          ([coll a b]      (concat coll (list a b)))
          ([coll a b c]    (concat coll (list a b c)))
          ;([coll a & args] (concat coll (cons arg args)))
          ))

(def doto! swap!)

; If the array is not sorted:
; java.util.Arrays.asList(theArray).indexOf(o)
; If the array is sorted, you can make use of a binary search for performance:
; java.util.Arrays.binarySearch(theArray, o)



