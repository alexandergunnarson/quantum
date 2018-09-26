(ns quantum.core.collections-typed
  (:refer-clojure :exclude
    [count empty? get reduce])
  (:require
    [quantum.core.data.array       :as arr]
    [quantum.core.data.async       :as dasync]
    [quantum.core.data.collections :as dcoll]
    [quantum.core.data.identifiers :as id]
    [quantum.core.data.map         :as map]
    [quantum.core.data.numeric     :as dnum]
    [quantum.core.data.primitive   :as p]
    [quantum.core.data.string      :as dstr]
    [quantum.core.data.vector      :as vec]
    [quantum.core.data.tuple       :as tup]
    [quantum.core.fn               :as fn]
    [quantum.core.type             :as t]
    [quantum.core.vars             :as var]))

#_"
- TODO incorporate FastUtil
  - FastUtil is the fastest collections library according to http://java-performance.info/hashmap-overview-jdk-fastutil-goldman-sachs-hppc-koloboke-trove-january-2015/
- TODO notify of changes to:
  - https://github.com/clojure/clojure/blob/master/src/jvm/clojure/lang/RT.java
  - https://github.com/clojure/clojure/blob/master/src/jvm/clojure/lang/Util.java
  - https://github.com/clojure/clojure/blob/master/src/jvm/clojure/lang/Numbers.java
- TODO queues need support
- TODO implement all these using wagjo/data-cljs
  - split-at [o index] - clojure.core/split-at
  - splice [o index n val] - fast remove and insert in one go
  - splice-arr [o index n val-arr] - fast remove and insert in one go
  - insert-before [o index val] - insert one item inside coll
  - insert-before-arr [o index val] - insert array of items inside coll
  - remove-at [o index] - remove one item from index pos
  - remove-n [o index n] - remove n items starting at index pos
  - rip [o index] - rips coll and returns [pre-coll item-at suf-coll]
  - sew [pre-coll item-arr suf-coll] - opposite of rip, but with arr
- TODO `pcount`
- TODO `transducei` ?
- TODO `(rreduce [f init o]) - like reduce but in reverse order = Equivalent to Scheme's `foldr`
"

;; ===== Reductive functions ===== ;;

#?(:cljs
  (defn- -reduce-seq
    "For some reason |reduce| is not implemented in ClojureScript for certain types.
     This is a |loop|-|recur| replacement for it."
    {:todo #{"Check if this is really the case..."
             "Improve performance with chunking, etc."}}
    [xs f init]
    (loop [xs (seq xs) v init]
      (if xs
          (let [ret (f v (first xs))]
            (if (reduced? ret)
                @ret
                (recur (next xs) ret)))
          v))))

;; TODO: conditionally optional arities etc. for t/fn

(var/def rf? "Reducing function"
  (t/ftype "seed arity"             []
           "completing arity"       [t/any?]
           "reducing arity"         [t/any? t/any?]
           "reducing arity for kvs" [t/any? t/any? t/any?]))

(t/defn reduce
  "Like `core/reduce` except:
   - When init is not provided, (f) is used.
   - Maps are reduced with `reduce-kv`.

   Equivalent to Scheme's `foldl`.

   Much of this content taken from clojure.core.protocols for inlining and
   type-checking purposes."
         ([rf rf?, xs ?] (reduce rf (rf) xs))
         ([rf rf?, init t/any?, xs p/nil?] init)
         ([rf rf?, init t/any?, z (t/isa? fast_zip.core.ZipperLocation)]
           (loop [xs (zip/down z), v init]
             (if (val? z)
                 (let [ret (rf v z)]
                   (if (dcoll/reduced? ret)
                       ;; TODO TYPED `(ref/deref ret)` should realize it's dealing with a `reduced?`
                       (ref/deref ret)
                       (recur (zip/right xs) ret)))
                 v)))
         ([f init ^array? arr] ; Adapted from `areduce`
           #?(:clj  (let [ct (Array/count arr)]
                      (loop [i 0 v init]
                        (if (< i ct)
                            (let [ret (f v (Array/get arr i))]
                              (if (reduced? ret)
                                  @ret
                                  (recur (unchecked-inc i) ret)))
                            v)))
              :cljs (array-reduce arr f init)))
         ([f init ^!+vector? xs] ; because transient vectors aren't reducible
           (let [ct (#?(:clj .count :cljs count) xs)] ; TODO fix for CLJS
             (loop [i 0 v init]
               (if (< i ct)
                   (let [ret (f v (#?(:clj .valAt :cljs get) xs i))] ; TODO fix for CLJS
                     (if (reduced? ret)
                         @ret
                         (recur (unchecked-inc i) ret)))
                   v))))
         ([f init ^string? s]
           (let [ct (#?(:clj .length :cljs .-length) s)]
             (loop [i 0 v init]
               (if (< i ct)
                   (let [ret (f v (.charAt s i))]
                     (if (reduced? ret)
                         @ret
                         (recur (unchecked-inc i) ret)))
                   v))))
#?(:clj  ([f init ^clojure.lang.StringSeq xs ]
           (let [s (.s xs)]
             (loop [i (.i xs) v init]
               (if (< i (.length s))
                   (let [ret (f v (.charAt s i))]
                     (if (reduced? ret)
                         @ret
                         (recur (unchecked-inc i) ret)))
                   v)))))
#?(:clj  ([f
           #{clojure.lang.PersistentVector ; vector's chunked seq is faster than its iter
                        clojure.lang.LazySeq ; for range
                        clojure.lang.ASeq} xs] ; aseqs are iterable, masking internal-reducers
           (if-let [s (seq xs)]
             (clojure.core.protocols/internal-reduce (next s) f (first s))
             (f))))
#?(:clj  ([f init
           #{clojure.lang.PersistentVector ; vector's chunked seq is faster than its iter
             clojure.lang.LazySeq ; for range
             clojure.lang.ASeq} xs]  ; aseqs are iterable, masking internal-reducers
           (let [s (seq xs)]
             (clojure.core.protocols/internal-reduce s f init))))
         ([f ^transformer? x]
           (let [rf ((.-xf x) f)]
             (rf (reduce* (.-prev x) rf (rf)))))
         ([f init ^transformer? x]
           (let [rf ((.-xf x) f)]
             (rf (reduce* (.-prev x) rf init))))
         ([f init ^chan?   x ] (async/reduce f init x))
#?(:cljs ([f init ^+map?   xs] (#_(:clj  clojure.core.protocols/kv-reduce
                                   :cljs -kv-reduce) ; in order to use transducers...
                                -reduce-seq xs f init)))
#?(:cljs ([f init ^+set?   xs ] (-reduce-seq xs f init)))
         ([f init #{#?(:clj integer? :cljs double?)} n]
           (loop [i 0 v init]
             (if (< i n)
                 (let [ret (f v i)]
                   (if (reduced? ret)
                       @ret
                       (recur (unchecked-inc i) ret)))
                 v)))
         ;; `iter-reduce`
#?(:clj  ([f #{clojure.lang.APersistentMap$KeySeq
               clojure.lang.APersistentMap$ValSeq
               Iterable} xs]
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
#?(:clj  ([f init
           #{clojure.lang.APersistentMap$KeySeq
             clojure.lang.APersistentMap$ValSeq
             Iterable} xs]
           (let [iter (.iterator xs)]
             (loop [ret init]
               (if (.hasNext iter)
                   (let [ret (f ret (.next iter))]
                     (if (reduced? ret)
                         @ret
                         (recur ret)))
                   ret)))))
#?(:clj  ([f      ^clojure.lang.IReduce     xs ] (.reduce   xs f)))
#?(:clj  ([f init ^clojure.lang.IKVReduce   xs ] (.kvreduce xs f init)))
#?(:clj  ([f init ^clojure.lang.IReduceInit xs ] (.reduce   xs f init)))
         ([f init ^default              xs]
           (if (val? xs)
               (#?(:clj  clojure.core.protocols/coll-reduce
                   :cljs -reduce) xs f init)
               init)))

(var/def rfi? "Reducing function, indexed"
  (t/ftype "seed arity"             []
           "completing arity"       [t/any?]
           "reducing arity"         [t/any? t/any? t/any?]
           "reducing arity for kvs" [t/any? t/any? t/any? t/any?]))

(t/defn reducei
  "`reduce`, indexed.
   Uses an unsynchronized mutable counter internally, but this cannot cause race conditions if
   `reduce` is implemented correctly (this includes single-threadedness)."
  [f rfi?, init t/any?, xs dcoll/reducible?]
  (let [f' (let [!i (ref/! -1)]
             (t/fn ([ret ? x ...]       (f ret x   (ref/reset! !i (num/inc* (ref/deref !i)))))
                   ([ret ? k ... v ...] (f ret k v (ref/reset! !i (num/inc* (ref/deref !i)))))))]
    (reduce f' init xs)))

(var/def xf? "Transforming function"
  (t/ftype [rf? :> rf?]))

(t/defn transduce >
  ([        f rf?,              xs dcoll/reducible?] (transduce fn/identity f     xs))
  ([xf xf?, f rf?,              xs dcoll/reducible?] (transduce xf          f (f) xs))
  ([xf xf?, f rf?, init t/any?, xs dcoll/reducible?] (let [f' (xf f)] (f' (reduce f' init xs)))))

;; ===== End reductive functions ===== ;;

(t/defn count > dnum/integer?
  {:todo #{"handle persistent maps"}
   :incorporated #{'clojure.lang.RT/count
                   'clojure.lang.RT/countFrom
                   'cljs.core/count}}
         ;; Concrete classes
         ([x p/nil?         > #?(:clj p/long? :cljs dnum/nip?)] 0)
         ([x ?/transformer? > ????] (reduce-count x))
#?(:cljs ([x dstr/str?      > (t/assume dnum/nip?)] (.-length x)))
#?(:cljs ([x dstr/!str?     > (t/assume dnum/nip?)] (.getLength x)))
         ([x tup/tuple?     > p/int?] (-> x .-vs count))
         ([x arr/std-array? > #?(:clj p/int? :cljs (t/assume dnum/nip?))]
           (#?(:clj Array/count :cljs .-length) x))
         ;; TODO `cljs.core/count` is not right here
         ([x vec/+vector?] (#?(:clj .count :cljs core/count) x))
         ([x dasync/m2m-chan?] (-> x #?(:clj .buf :cljs .-buf) count))
         ;; Abstract classes
         ([x dcoll/icounted? > #?(:clj p/int? :cljs (t/* dnum/nip?))]
           #?(:clj  (.count x)
              :cljs (-count ^not-native x)))
#?(:cljs ([x dcoll/iseqable? > p/int?]
           ;; TODO TYPED
           (loop [s (seq coll) acc 0]
             (if (counted? s) ; assumes nil is counted, which it currently is
                 (+ acc (-count s))
                 (recur (next s) (inc acc))))))
#?(:clj  ([x dcoll/ipersistentcollection? > p/int?]
           ;; TODO TYPED
           ISeq s = seq(o);
       		 o = null;
       		 int i = 0;
       		 for(/ s != null / s = s.next()) {
       		 	 if(s instanceof Counted)
       		 	   return i + s.count();
       		 	 i++;
       		 }
       		 return i;

           ))
#?(:clj  ([x dstr/char-seq?   > p/int?] (.length x)))
#?(:clj  ([x dcoll/java-coll? > p/int?] (.size x)))
#?(:clj  ([x map/java-map?    > p/int?] (.size x)))
#?(:clj  ([x tup/map-entry?   > p/long?] 2))
#?(:clj  ([x arr/array?       > p/int?] (java.lang.reflect.Array/getLength x))))

(t/defn empty? > p/boolean?
  {:todo #{"import clojure.lang.RT/seq"}}
        ;; TODO re-evaluate this arity
      #_([x ?] (-> x count num/zero?))
        ([x p/nil?] true)
        ([x ?/transformer?] (->> x (reduce (fn' (reduced false)) true)))
#?(:clj ([x dcoll/java-coll?] (.isEmpty x)))
#?(:clj ([x map/java-map?] (.isEmpty x)))
        ;; TODO TYPED
        ([^default          x] (core/empty? x)))


(t/defn get
  {:todo         {0 "Need to excise non-O(1) `nth`"}
   :incorporated #{'clojure.lang.RT/get}}
  #?(:clj  ([^clojure.lang.ILookup            x            k             ] (.valAt x k)))
  #?(:clj  ([^clojure.lang.ILookup            x            k if-not-found] (.valAt x k if-not-found)))
  #?(:clj  ([#{java.util.Map clojure.lang.IPersistentSet}
                                              x            k             ] (.get x k)))
  #?(:clj  ([#{!map|byte->any?}               x ^byte      k             ] (.get x k)))
  #?(:clj  ([#{!map|char->any?}               x ^char      k             ] (.get x k)))
  #?(:clj  ([#{!map|short->any?}              x ^short     k             ] (.get x k)))
  #?(:clj  ([#{!map|int->any?}                x ^int       k             ] (.get x k)))
  #?(:clj  ([#{!map|long->any?}               x ^long      k             ] (.get x k)))
  #?(:clj  ([#{!map|float->ref?}              x ^float     k             ] (.get x k)))
  #?(:clj  ([#{!map|double->ref?}             x ^double    k             ] (.get x k)))
           ([^string?                         x ^nat-long? i if-not-found] (if (>= i (count x)) if-not-found (.charAt x i)))
  #?(:clj  ([^!array-list?                    x ^nat-long? i if-not-found] (if (>= i (count x)) if-not-found (.get    x i))))
           ([#{string? #?(:clj !array-list?)} x ^nat-long? i             ] (get      x i nil))

           ([^array-1d? x #?(:clj #{int}) i1]
            (#?(:clj  Array/get
                :cljs core/aget) x i1))
           #?(:clj ([#{array-2d? array-3d? array-4d? array-5d? array-6d? array-7d? array-8d? array-9d? array-10d?} x
            ^int i1]
            (Array/get x i1)))
           ([^tuple?                          x ^nat-long? i             ] (get (.-vs x) i))
           ([^seq?                            x            i             ] (core/nth x i nil         ))
           ([^seq?                            x            i if-not-found] (core/nth x i if-not-found))
           ; TODO look at clojure.lang.RT/get for how to handle these edge cases efficiently
  #?(:cljs ([^nil?                            x            i             ] (core/get x i nil         )))
  #?(:cljs ([^nil?                            x            i             ] (core/get x i nil         )))
  #?(:cljs ([^nil?                            x            i if-not-found] (core/get x i if-not-found)))
           ([^default                         x            i             ]
              (if (nil? x)
                  nil
                  (throw (ex-info "`get` not supported on" {:type (type x)}))))
         #_([                                 x            i if-not-found] (core/get x i if-not-found)))
