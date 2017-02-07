(ns ^{:doc
      "A library for reduction and parallel folding. Alpha and subject
      to change.  Note that fold and its derivatives require Java 7+ or
      Java 6 + jsr166y.jar for fork/join support.

      Adds some interesting reducers and folders from different sources
      gleaned from the far reaches of the internet. Some of them have
      unexpectedly great performance."
      :author       "Rich Hickey"
      :contributors #{"Alan Malloy" "Alex Gunnarson" "Christophe Grand"}}
  quantum.core.reducers.reduce
  (:refer-clojure :exclude [reduce into])
  (:require
    [clojure.core                  :as core]
    [clojure.core.async            :as async]
    [quantum.core.collections.base :as cbase
      :refer [nnil?]]
    [quantum.core.data.vector      :as vec
      :refer [catvec]]
  #?@(:clj
   [[seqspert.hash-set]
    [seqspert.hash-map]])
    [quantum.core.data.set         :as set]
    [quantum.core.data.map         :as map]
    [quantum.core.error            :as err
      :refer [->ex]]
    [quantum.core.macros           :as macros
      :refer [defnt]]
    [quantum.core.type             :as t
      :refer [editable?]]
    [quantum.core.type.defs
      #?@(:cljs [:refer [Reducer Folder]])]
    [quantum.core.vars             :as var
      :refer [defalias]])
  (:require-macros
    [quantum.core.reducers.reduce
      :refer [reduce]])
  (:import
  #?(:clj  [quantum.core.type.defs Reducer Folder]
     :cljs [goog.string StringBuffer])))


; HEADLESS FIX
; {:attribution "Christophe Grand - http://clj-me.cgrand.net/2013/09/11/macros-closures-and-unexpected-object-retention/"}
; Creating a reducer holds the head of the collection in a closure.
; Thus, a huge lazy-seq can be tied up memory as it becomes realized.

; Fixing it so the seqs are headless.
; Christophe Grand - https://groups.google.com/forum/#!searchin/clojure-dev/reducer/clojure-dev/t6NhGnYNH1A/2lXghJS5HywJ

;___________________________________________________________________________________________________________________________________
;=================================================={          REDUCE          }=====================================================
;=================================================={                          }=====================================================
; TODO rreduce [f init o] - like reduce but in reverse order = Equivalent to Scheme's `foldr`
#?(:cljs
  (defn- -reduce-seq
    "For some reason |reduce| is not implemented in ClojureScript for certain types.
     This is a |loop|-|recur| replacement for it."
    {:attribution "Alex Gunnarson"
     :todo ["Check if this is really the case..."
            "Improve performance with chunking, etc."]}
    [coll f init]
    (loop [coll-n coll
           ret    init]
      (if (empty? coll-n)
          ret
          (recur (rest coll-n)
                 (f ret (first coll-n)))))))

(defnt reduce*
  {:attribution "Alex Gunnarson"
   :todo        #{"Mutable collections, etc."
                  "n-d arrays"}}
        ([^fast_zip.core.ZipperLocation z f init]
          (cbase/zip-reduce* f init z))
        ([^array-1d? arr f init] ; Taken from `areduce`
          #?(:clj  (let [ct (alength arr)]
                     (loop  [i 0 ret init]
                       (if (< i ct)
                           (recur (unchecked-inc-int i) (f ret (aget arr i)))
                           ret)))
             :cljs (array-reduce arr f init)))
        ([^string? s f init]
          #?(:clj  (clojure.core.protocols/coll-reduce s f init)
             :cljs (let [last-index (-> s count unchecked-dec long)]
                     (cond
                       (> last-index js/Number.MAX_SAFE_INTEGER)
                         (throw (->ex "String too large to reduce over (at least, efficiently)."))
                       (= last-index -1)
                         (f init nil)
                       :else
                         (loop [n   (long 0)
                                ret init]
                           (if (> n last-index)
                               ret
                               (recur (unchecked-inc n)
                                      (f ret (.charAt s n)))))))))
#?(:clj ([^record? coll f init] (clojure.core.protocols/coll-reduce coll f init)))
        ([^reducer? x f init]
          (reduce* (:coll x) ((:transform x) f) init))
        ([^chan?   x    f init] (async/reduce f init x))
        ([^+map?   coll f init] (#_(:clj  clojure.core.protocols/kv-reduce
                                    :cljs -kv-reduce) ; in order to use transducers...
                                 #?(:clj  clojure.core.protocols/coll-reduce
                                    :cljs -reduce-seq)
                                  coll f init))
        ([^+set?   coll f init] (#?(:clj  clojure.core.protocols/coll-reduce
                                    :cljs -reduce-seq)
                                  coll f init))
        ([#{#?(:clj integer? :cljs double?)} i f init]
          (if (< i 0)
              init
              (loop [i'  0
                     ret init]
                (if (= i' i)
                    ret
                    (recur (unchecked-inc i')
                           (f ret i'))))))
        ([:else    coll f init] (when (nnil? coll)
                                  (#?(:clj  clojure.core.protocols/coll-reduce
                                      :cljs -reduce)
                                    coll f init))))

#?(:clj
(defmacro reduce
  "Like `core/reduce` except:
   When init is not provided, (f) is used.
   Maps are reduced with reduce-kv.

   Entry point for internal reduce (in order to switch the args
   around to dispatch on type).

   Equivalent to Scheme's `foldl`."
  {:attribution "Alex Gunnarson"
   :todo ["definline"]}
  ([f coll]      `(reduce ~f (~f) ~coll))
  ([f init coll] `(reduce* ~coll ~f ~init))))

;___________________________________________________________________________________________________________________________________
;=================================================={    REDUCING FUNCTIONS    }=====================================================
;=================================================={       (Generalized)      }=====================================================
(defn reducer
  "Given a reducible collection, and a transformation function transform,
  returns a reducible collection, where any supplied reducing
  fn will be transformed by transform. transform is a function of reducing fn to
  reducing fn."
  {:added "1.5"
   :attribution "clojure.core.reducers"}
  ([coll transform]
    (Reducer. coll transform)))

(def reducer? #(instance? Reducer %))

(defn conj-red
  "Like |conj| but includes a 3-arity for |reduce-kv| purposes."
  ([ret x  ] (conj ret x))
  ([ret k v] (conj ret [k v])))

(defn conj!-red
  "Like |conj!| but includes a 3-arity for |reduce-kv| purposes."
  ([ret x  ] (conj! ret x))
  ([ret k v] (conj! ret [k v])))

(defn transient-into [to from]
  (when-let [ret (reduce* from conj!-red (transient to))]
    (-> ret  persistent! (with-meta (meta to)))))

(defn persistent-into [to from]
  (reduce* from conj-red to))

(defn red-apply
  "Applies ->`f` to ->`coll`, pairwise, using `reduce`."
  [f coll]
  (let [first? (volatile! true)]
    (reduce* coll
             (fn [ret x]
               (if @first?
                   (do (vreset! first? false) (f x))
                   (f ret x)))
             nil)))

(defn first-non-nil-reducer
  "A reducing function that simply returns the first non-nil element in the
  collection."
  {:source "tesser.utils"}
  [_ x]
  (when-not (nil? x) (reduced x)))
