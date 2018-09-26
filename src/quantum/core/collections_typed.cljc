(ns quantum.core.collections-typed
  (:refer-clojure :exclude
    [chunk-first chunk-rest count empty? first get next nth reduce])
  (:require
    [quantum.core.data.array       :as arr]
    [quantum.core.data.async       :as dasync]
    [quantum.core.data.collections :as dc]
    [quantum.core.data.compare     :as dcomp]
    [quantum.core.data.identifiers :as id]
    [quantum.core.data.map         :as map]
    [quantum.core.data.numeric     :as dnum]
    [quantum.core.data.primitive   :as p]
    [quantum.core.data.string      :as dstr]
    [quantum.core.data.vector      :as vec]
    [quantum.core.data.tuple       :as tup]
    [quantum.core.fn               :as fn]
    [quantum.core.numeric
      :refer [inc*]]
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

;; ===== Access functions ===== ;;

;; TODO for CLJS we should do !+vector
(t/defn get
  "Retrieve the value in `xs` associated with the key `k`.

   The expectation, which is not enforced, is that this retrieval will take place in sublinear time.
   O(1) is best; O(log32(n)) is common; and O(log(n)) is acceptable."
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

;; TODO implement
(t/defn nth
  "Retrieve the element from `xs` at the index `i`.

   In contrast to `get`, this may or may not happen in sublinear time, but it is expected (though
   not enforced) that a type-specialization of `nth` will provide the most efficient implementation
   possible.

   Prefer to `get` when retrieving elements at indices unless expectations of sublinear time are
   necessary."
  ...)

;; ----- Iterators ----- ;;

(t/defn ^:inline >iterator [x (t/isa|direct? #?(:clj java.lang.Iterable :cljs cljs.core/IIterable))]
  #?(:clj  (.iterator x)
     :cljs (cljs.core/-iterator ^not-native x)))

;; ----- Chunking ----- ;;

(t/defn chunk-buffer > chunk-buffer? [capacity num/numerically-int?]
  (clojure.lang.ChunkBuffer. (p/>int capacity)))

(t/defn ^:inline chunk        [b dc/chunk-buffer?           > dc/chunk?] (.chunk b))
(t/defn ^:inline chunk-append [b dc/chunk-buffer?, x p/ref? > dc/chunk?] (.add   b x))

(t/defn ^:inline chunk-first [xs dc/chunked-seq? > dc/chunk?] (.chunkedFirst xs))
(t/defn ^:inline chunk-rest  [xs dc/chunked-seq? > dc/chunk?] (.chunkedMore  xs))
(t/defn ^:inline chunk-next  [xs dc/chunked-seq? > dc/chunk?] (.chunkedNext  xs))

(t/defn chunk-cons [chunk dc/chunk?, the-rest dc/iseq?]
  (if (num/zero? (count chunk)) ;; TODO TYPED replace this condition with `empty`
      the-rest
      (clojure.lang.ChunkedCons. chunk the-rest)))

;; ----- Sequences ----- ;;

(t/defn ^:inline >seq
  {:incorporated '{clojure.lang.RT/seq "9/26/2018"
                   clojure.core/seq    "9/26/2018"
                   cljs.core/seq       "9/26/2018"}}
  > (t/? dc/iseq?)
         ([x p/nil?] nil)
#?(:clj  ([xs dc/aseq?] x))
         ([xs #?(:clj  (t/isa? clojure.lang.Seqable)
                 :cljs (t/isa|direct? cljs.core/ISeqable))]
           (#?(:clj .seq :cljs cljs.core/-seq) x))
#?(:clj  ([xs (t/isa? java.lang.Iterable)] (-> x >iterator clojure.lang.RT/chunkIteratorSeq)))
#?(:clj  ([xs dstr/char-seq?] (clojure.lang.StringSeq/create x))
   :cljs ([xs dstr/string?] (when-not (num/zero? (count xs)) ; TODO use `empty?` instead
                              (cljs.core/IndexedSeq. xs 0 nil))))
#?(:clj  ([xs dc/java-map?] (-> x .entrySet >seq)))
         ;; NOTE `ArraySeq/createFromObject` is the slow path but has to be that way because the
         ;; specialized ArraySeq constructors are private
         ([xs arr/array?]
           #?(:clj  (ArraySeq/createFromObject xs)
              :cljs (when-not (num/zero? (count xs)) ; TODO use `empty?` instead
                      (cljs.core/IndexedSeq. xs 0 nil)))))

(t/defn- ^:inline string-seq>underlying-string
  [xs (t/isa? clojure.lang.StringSeq) > (t/assume dstr/str?)] (.s xs))

;; ===== Reductive functions ===== ;;

;; TODO: conditionally optional arities etc. for t/fn

(var/def rf? "Reducing function"
  (t/ftype "seed arity"             []
           "completing arity"       [t/any?]
           "reducing arity"         [t/any? t/any?]
           "reducing arity for kvs" [t/any? t/any? t/any?]))

#?(:clj
(t/defn reduce-chunked
  "Made public in case future specializations want to use it."
  [rf rf?, init t/any?, xs dc/chunked-seq?]
  (let [ret (.reduce (chunk-first xs) rf init)]
    (if (dc/reduced? ret)
        (ref/deref ret)
        (recur (chunk-next xs) rf ret)))))

#?(:clj
(t/defn reduce-indexed
  "Made public in case future specializations want to use it."
  ([rf rf?, init t/any?, xs (t/or dstr/str? vec/!+vector? arr/array?), i0 t/numerically-integer?]
    (let [ct (count xs)]
      (loop [i (p/>int i0) ret init]
        (if (comp/< i ct)
            (let [ret' (rf ret (get xs i))]
              (if (dc/reduced? ret')
                  (ref/deref ret')
                  ;; TODO TYPED automatically figure out that `inc` will never go out of bounds here
                  (recur (inc* i) ret')))
            v))))))

(t/defn reduce-iter
  "Made public in case future specializations want to use it."
  [rf rf?, init t/any?, xs (t/isa|direct? #?(:clj java.lang.Iterable :cljs cljs.core/IIterable))]
  (let [iter (>iterator xs)]
    (loop [ret init]
      (if #?(:clj (.hasNext iter) :cljs ^boolean (.hasNext iter))
          (let [ret' (rf ret (.next iter))]
            (if (dc/reduced? ret')
                (ref/deref ret')
                (recur ret')))
          ret))))

(t/defn reduce-seq
  "Reduces a seq, ignoring any opportunities to switch to a more specialized implementation.

   Made public in case future specializations want to use it."
  [rf rf?, init t/any?, xs iseq?]
  (loop [xs' xs, ret init]
    (if (nil? xs')
        ret
        (let [ret' (rf ret (first xs'))]
          (if (dc/reduced? ret')
              (ref/deref ret')
              (recur (next xs') ret'))))))

;; TODO TYPED do type inference based on the rf's. We can sometimes figure out what gets returned
;; based on what is passed in
(t/defn reduce
  "Prefer `transduce` to calling only `reduce`, as otherwise the completing arity of the reducing
   function will not get called, which for certain transducers yields unexpected results.

   Like `core/reduce` except:
   - When init is not provided, (f) is used.
   - Maps are reduced as if with `reduce-kv`.

   Equivalent to Scheme's `foldl`.

   We would specialize on `clojure.lang.Range` and `clojure.lang.LongRange` but they do not expose
   their `step` field so we have to use their implementation of `reduce`."
   {:incorporated '{clojure.core/reduce    "9/25/2018"
                    clojure.core/reduce-kv "9/25/2018"
                    clojure.core.protocols "9/25/2018"
                    cljs.core/reduce       "9/26/2018"
                    cljs.core/reduce-kv    "9/25/2018"
                    cljs.core/array-reduce "9/25/2018"
                    cljs.core/iter-reduce  "9/26/2018"
                    cljs.core/seq-reduce   "9/26/2018"}}
         ([rf rf?, xs ?] (reduce rf (rf) xs))
         ([rf rf?, init t/any?, xs p/nil?] init)
         ;; - Adapted from `areduce`
         ;; - `!+vector?` included because they aren't reducible or seqable by default
         ([rf rf?, init t/any?, xs (t/or dstr/str? vec/!+vector? arr/array?)]
           (reduce-indexed rf init xs 0))
#?(:clj  ([rf rf?, init t/any?, xs dc/string-seq?]
           (reduce-indexed rf init (string-seq>underlying-string xs) (.index xs))))
#?(:clj  ([rf rf?, init t/any?, xs dc/array-seq?]
           (reduce-indexed rf init (.array xs) (.index xs))))
         ;; Vector's chunked seq is faster than its iterator
#?(:clj  ([rf rf?, init t/any?, xs (t/or (t/isa? clojure.lang.PersistentVector) chunked-seq?)]
           (reduce-chunked rf init xs)))
         ([rf rf?, init t/any?, n dnum/numerically-integer?]
           (loop [i 0, ret init]
             (if (comp/< i n)
                 (let [ret' (rf ret i)]
                   (if (dc/reduced? ret')
                       (ref/deref ret')
                       ;; TODO TYPED automatically figure out that `inc` will never go out of bounds
                       ;; depending on the type of `n`
                       (recur (inc i) ret')))
                 ret)))
         ;; TODO refine
         ([f init ^transformer? x]
           (let [rf ((.-xf x) f)]
             (rf (reduce* (.-prev x) rf init))))
         ([rf rf?, init t/any?, x dasync/read-chan?]
           (async/go-loop [ret init]
             (let [v (async/<! ch)]
               (if (p/nil? v)
                   ret
                   (let [ret' (rf ret v)]
                     (if (ref/reduced? ret')
                         (ref/deref ret')
                         (recur ret')))))))
         ([rf rf?, init t/any?, xs (t/isa? fast_zip.core.ZipperLocation)]
           (loop [xs' (zip/down xs), ret init]
             (if (p/val? xs')
                 (let [ret' (rf ret xs')]
                   (if (dc/reduced? ret')
                       ;; TODO TYPED `(ref/deref ret)` should realize it's dealing with a `reduced?`
                       (ref/deref ret')
                       (recur (zip/right xs') ret')))
                 v)))
#?(:clj  ([rf rf?, init t/any?
           xs (t/or (t/isa? clojure.lang.APersistentMap$KeySeq)
                    (t/isa? clojure.lang.APersistentMap$ValSeq))] (reduce-iter rf init xs)))
#?(:clj  ([rf rf?, init t/any?, xs (t/isa? clojure.lang.IKVReduce)]   (.kvreduce xs rf init)))
#?(:clj  ([rf rf?, init t/any?, xs (t/isa? clojure.lang.IReduceInit)] (.reduce   xs rf init)))
         ;; TODO refine
#?(:clj  ([rf rf?, init t/any?, xs (t/or dc/lseq? dc/aseq?)]
           (let [c (class xs)]
             (loop [xs' (>seq xs), ret init]
               (if (dcomp/== (class xs') c)
                   (let [ret' (rf ret (first xs'))]
                     (if (dc/reduced? ret')
                         (ref/deref ret')
                         (recur (next xs') ret')))
                   ;; TODO TYPED automatically figure out that:
                   ;; - `(not (dcomp/== (class xs') (class xs)))`
                   ;; - What the possible types of xs' are as a result
                   (reduce rf init xs'))))))
#?(:clj  ([rf rf?, init t/any?, xs (t/isa? java.lang.Iterable)] (reduce-iter rf init xs)))
         ;; TODO CLJS might be able to be done more efficiently with more specializations?
         ([rf rf?, init t/any?, xs #?(:clj  (t/isa?        clojure.core.protocols/IKVReduce)
                                      :cljs (t/isa|direct? cljs.core/IKVReduce))]
           (#?(:clj  clojure.core.protocols/kv-reduce
               :cljs cljs.core/-kv-reduce) xs rf init))
         ;; TODO CLJS might be able to be done more efficiently with more specializations?
         ([rf rf?, init t/any?, xs #?(:clj  (t/isa?        clojure.core.protocols/CollReduce)
                                      :cljs (t/isa|direct? cljs.core/IReduce))]
           (#?(:clj  clojure.core.protocols/coll-reduce
               :cljs cljs.core/-reduce) xs rf init))
#?(:cljs ([rf rf?, init t/any?, xs (t/isa|direct? cljs.core/IIterable)] (reduce-iter rf init xs)))
#?(:cljs ([rf rf?, init t/any?, xs (t/isa|direct? cljs.core/ISeqable)]
           (reduce-seq rf init (>seq xs)))))

(var/def rfi? "Reducing function, indexed"
  (t/ftype "seed arity"             []
           "completing arity"       [t/any?]
           "reducing arity"         [t/any? t/any? t/any?]
           "reducing arity for kvs" [t/any? t/any? t/any? t/any?]))

(t/defn reducei
  "`reduce`, indexed.
   Uses an unsynchronized mutable counter internally, but this cannot cause race conditions if
   `reduce` is implemented correctly (this includes single-threadedness)."
  [rf rfi?, init t/any?, xs dc/reducible?]
  (let [rf' (let [!i (ref/! -1)]
              (fn/aritoid rf' rf'
                (t/fn ([ret ?, x ?]
                  (rf ret x   (ref/reset! !i (num/inc* (ref/deref !i))))))
                (t/fn ([ret ?, k ?, v ?]
                  (rf ret k v (ref/reset! !i (num/inc* (ref/deref !i))))))))]
    (reduce rf' init xs)))

(var/def xf? "Transforming function"
  (t/ftype [rf? :> rf?]))

(t/defn ^:inline transduce >
  ([        rf rf?,              xs (t/input-type reduce :_ :_ :?)]
    (transduce fn/identity rf      xs))
  ([xf xf?, rf rf?,              xs (t/input-type reduce :_ :_ :?)]
    (transduce xf          rf (rf) xs))
  ([xf xf?, rf rf?, init t/any?, x  dasync/read-chan?]
    (let [rf' (xf rf)]
      (async/go
        (rf' (async/<! (reduce rf' init x))))))
  ([xf xf?, rf rf?, init t/any?, xs dc/reducible?]
    (let [rf' (xf rf)] (rf' (reduce rf' init xs)))))

;; ===== End reductive functions ===== ;;

;; TODO make sure !+vector is handled for CLJS
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
         ([x dc/icounted? > #?(:clj p/int? :cljs (t/* dnum/nip?))]
           #?(:clj  (.count x)
              :cljs (-count ^not-native x)))
#?(:cljs ([x dc/iseqable? > p/int?]
           ;; TODO TYPED
           (loop [s (>seq coll) acc 0]
             (if (counted? s) ; assumes nil is counted, which it currently is
                 (+ acc (-count s))
                 (recur (next s) (inc acc))))))
#?(:clj  ([x dc/ipersistentcollection? > p/int?]
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
#?(:clj  ([x dc/java-coll? > p/int?] (.size x)))
#?(:clj  ([x map/java-map?    > p/int?] (.size x)))
#?(:clj  ([x tup/map-entry?   > p/long?] 2))
#?(:clj  ([x arr/array?       > p/int?] (java.lang.reflect.Array/getLength x))))

(t/defn empty? > p/boolean?
  {:todo #{"import clojure.lang.RT/seq"}}
        ;; TODO re-evaluate this arity
      #_([x ?] (-> x count num/zero?))
        ([x p/nil?] true)
        ([x ?/transformer?] (->> x (reduce (fn' (reduced false)) true)))
#?(:clj ([x dc/java-coll?] (.isEmpty x)))
#?(:clj ([x map/java-map?] (.isEmpty x)))
        ;; TODO TYPED
        ([^default          x] (core/empty? x)))
