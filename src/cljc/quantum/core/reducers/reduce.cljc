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
  (:refer-clojure :exclude
    [reduce into, deref reset! transduce])
  (:require
    [clojure.core                  :as core]
    [clojure.core.async            :as async]
    [fast-zip.core                 :as zip]
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
    [quantum.core.fn
      :refer [fnl]]
    [quantum.core.macros           :as macros
      :refer [defnt]]
    [quantum.core.refs             :as refs
      :refer [deref !boolean reset!]]
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
  #?@(:clj  [[quantum.core.type.defs Reducer Folder]
             quantum.core.data.Array]
      :cljs [[goog.string StringBuffer]])))


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
    {:attribution "alexandergunnarson"
     :todo ["Check if this is really the case..."
            "Improve performance with chunking, etc."]}
    [xs f init]
    (loop [xs (seq xs) v init]
      (if xs
          (let [ret (f v (first xs))]
            (if (reduced? ret)
                @ret
                (recur (next xs) ret)))
          v))))

(defnt reduce*
  "Much of this content taken from clojure.core.protocols for inlining and
   type-checking purposes."
  {:attribution "alexandergunnarson"}
         ([^fast_zip.core.ZipperLocation z f init]
           (loop [xs (zip/down z) v init]
             (if (some? z)
                 (let [ret (f v z)]
                   (if (reduced? ret)
                       @ret
                       (recur (zip/right xs) ret)))
                 v)))
         ([^array? arr f init] ; Adapted from `areduce`
           #?(:clj  (loop [i 0 v init]
                      (if (< i (Array/count arr))
                          (let [ret (f v (Array/get arr i))]
                            (if (reduced? ret)
                                @ret
                                (recur (unchecked-inc i) ret)))
                          v))
              :cljs (array-reduce arr f init)))
         ([^!+vec? xs f init] ; because transient vectors aren't reducible
           (let [ct (#?(:clj .count :cljs count) xs)] ; TODO fix for CLJS
             (loop [i 0 v init]
               (if (< i ct)
                   (let [ret (f v (#?(:clj .valAt :cljs get) xs i))] ; TODO fix for CLJS
                     (if (reduced? ret)
                         @ret
                         (recur (unchecked-inc i) ret)))
                   v))))
         ([^string? s f init]
           (let [ct (#?(:clj .length :cljs .-length) s)]
             (loop [i 0 v init]
               (if (< i ct)
                   (let [ret (f v (.charAt s i))]
                     (if (reduced? ret)
                         @ret
                         (recur (unchecked-inc i) ret)))
                   v))))
#?(:clj  ([^clojure.lang.StringSeq xs f init]
           (let [s (.s xs)]
             (loop [i (.i xs) v init]
               (if (< i (.length s))
                   (let [ret (f v (.charAt s i))]
                     (if (reduced? ret)
                         @ret
                         (recur (unchecked-inc i) ret)))
                   v)))))
#?(:clj  ([#{clojure.lang.PersistentVector ; vector's chunked seq is faster than its iter
             clojure.lang.LazySeq ; for range
             clojure.lang.ASeq} xs f] ; aseqs are iterable, masking internal-reducers
           (if-let [s (seq xs)]
             (clojure.core.protocols/internal-reduce (next s) f (first s))
             (f))))
#?(:clj  ([#{clojure.lang.PersistentVector ; vector's chunked seq is faster than its iter
             clojure.lang.LazySeq ; for range
             clojure.lang.ASeq} xs f init]  ; aseqs are iterable, masking internal-reducers
           (let [s (seq xs)]
             (clojure.core.protocols/internal-reduce s f init))))
         ([^transformer? x f]
           (reduce* x f (f)))
         ([^transformer? x f init]
           (reduce* (.-prev x) ((.-xf x) f) init))
         ([^chan?   x  f init] (async/reduce f init x))
#?(:cljs ([^+map?   xs f init] (#_(:clj  clojure.core.protocols/kv-reduce
                                   :cljs -kv-reduce) ; in order to use transducers...
                                -reduce-seq xs f init)))
#?(:cljs ([^+set?   xs f init] (-reduce-seq xs f init)))
         ([#{#?(:clj integer? :cljs double?)} n f init]
           (loop [i 0 v init]
             (if (< i n)
                 (let [ret (f v i)]
                   (if (reduced? ret)
                       @ret
                       (recur (unchecked-inc i) ret)))
                 v)))
         ;; `iter-reduce`
#?(:clj  ([#{clojure.lang.APersistentMap$KeySeq
             clojure.lang.APersistentMap$ValSeq
             Iterable} xs f]
           (let [iter (.iterator xs)]
             (if (.hasNext iter)
                 (loop [ret (.next iter)]
                   (if (.hasNext iter)
                       (let [ret (f ret (.next iter))]
                         (if (reduced? ret)
                             @ret
                             (recur ret)))
                       ret))
                 (f)))))
         ;; `iter-reduce`
#?(:clj  ([#{clojure.lang.APersistentMap$KeySeq
             clojure.lang.APersistentMap$ValSeq
             Iterable} xs f init]
           (let [iter (.iterator xs)]
             (loop [ret init]
               (if (.hasNext iter)
                   (let [ret (f ret (.next iter))]
                     (if (reduced? ret)
                         @ret
                         (recur ret)))
                   ret)))))
#?(:clj  ([^clojure.lang.IReduce     xs f     ] (.reduce   xs f)))
#?(:clj  ([^clojure.lang.IKVReduce   xs f init] (.kvreduce xs f init)))
#?(:clj  ([^clojure.lang.IReduceInit xs f init] (.reduce   xs f init)))
         ([^default              xs f] (if (some? xs)
                                           (#?(:clj  clojure.core.protocols/coll-reduce
                                               :cljs -reduce) xs f)
                                           (f)))
         ([^default              xs f init]
           (if (some? xs)
               (#?(:clj  clojure.core.protocols/coll-reduce
                   :cljs -reduce) xs f init)
               init)))

#?(:clj
(defmacro reduce
  "Like `core/reduce` except:
   When init is not provided, (f) is used.
   Maps are reduced with reduce-kv.

   Entry point for internal reduce (in order to switch the args
   around to dispatch on type).

   Equivalent to Scheme's `foldl`."
  {:attribution "alexandergunnarson"
   :todo ["definline"]}
  ([f coll]      `(reduce* ~coll ~f))
  ([f init coll] `(reduce* ~coll ~f ~init))))

#?(:clj
(defmacro reducei
   "`reduce`, indexed.

   This is a macro to eliminate the wrapper function call.
   Originally used a mutable counter on the inside just for fun...
   but the counter might be propagated via @f, so it's best to use
   an atomic value instead."
  {:attribution "alexandergunnarson"
   :todo ["Make this an inline function, not a macro."]}
  [f ret-i coll & args]
  (let [f-final
         `(let [i# (volatile! (long -1))]
            (fn ([ret# elem#]
                  (vswap! i# quantum.core.core/unchecked-inc-long)
                  (~f ret# elem# @i#))
                ([ret# k# v#]
                  (vswap! i# quantum.core.core/unchecked-inc-long)
                  (~f ret# k# v# @i#))))
        code `(reduce ~f-final ~ret-i ~coll)]
    code)))

#?(:clj
; TODO unmacro — it's just a macro until type inference is ready
(defmacro transduce
  ([   f xs] `(transduce identity ~f      ~xs))
  ([xf f xs] `(transduce ~xf      ~f (~f) ~xs))
  ([xf f init xs]
    `(let [f# (~xf ~f)]
       (f# (reduce f# ~init ~xs))))))
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
  ([xs xf]
    (cond (instance? Reducer xs)
            (Reducer. (.-xs ^Reducer xs) xs xf)
          (instance? Folder xs)
            (Reducer. (.-xs ^Folder  xs) xs xf)
          true
            (Reducer. xs                 xs xf))))

(def reducer? (fnl instance? Reducer))

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
    (-> ret persistent! (with-meta (meta to)))))

(defn persistent-into [to from]
  (reduce* from conj-red to))

(defn red-apply
  "Applies ->`f` to ->`coll`, pairwise, using `reduce`."
  [f coll]
  (let [first? (!boolean true)]
    (reduce* coll
      (fn [ret x]
        (if (deref first?)
            (do (reset! first? false) (f x))
            (f ret x)))
      nil)))

(defn first-non-nil-reducer
  "A reducing function that simply returns the first non-nil element in the
  collection."
  {:source "tesser.utils"}
  [_ x] (when-not (nil? x) (reduced x)))
