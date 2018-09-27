(ns quantum.core.data.collections
  (:refer-clojure :exclude
    [associative? indexed? list? sequential?])
  (:require
    [quantum.core.data.array  :as arr]
    [quantum.core.data.map    :as map]
    [quantum.core.data.set    :as set]
    [quantum.core.data.string :as dstr]
    [quantum.core.data.tuple  :as tup]
    [quantum.core.data.vector :as vec]
    [quantum.core.type :as t]))

;; ===== Sequences and sequence-wrappers ===== ;;
;; Sequential (generally not efficient Lookup / RandomAccess)

(def iseq? (t/isa? #?(:clj clojure.lang.ISeq :cljs cljs.core/ISeq)))

#?(:clj (def aseq? (t/isa? clojure.lang.ASeq)))

(def lseq? (t/isa? #?(:clj clojure.lang.LazySeq :cljs cljs.core/LazySeq)))

(def cons? (t/isa? #?(:clj clojure.lang.Cons :cljs cljs.core/Cons)))

;; TODO CLJS
#?(:clj
(def array-seq?
  (t/or (t/isa? clojure.lang.ArraySeq)
        (t/isa? clojure.lang.ArraySeq$ArraySeq_byte)
        (t/isa? clojure.lang.ArraySeq$ArraySeq_short)
        (t/isa? clojure.lang.ArraySeq$ArraySeq_char)
        (t/isa? clojure.lang.ArraySeq$ArraySeq_int)
        (t/isa? clojure.lang.ArraySeq$ArraySeq_long)
        (t/isa? clojure.lang.ArraySeq$ArraySeq_float)
        (t/isa? clojure.lang.ArraySeq$ArraySeq_double))))

;; TODO CLJS
#?(:clj
(def string-seq? (t/isa? clojure.lang.StringSeq)))

;; TODO CLJS
(def chunk-buffer? #?(:clj  (t/isa? clojure.lang.ChunkBuffer)
                      :cljs ...))

;; TODO CLJS
(def chunk? #?(:clj  (t/isa? clojure.lang.IChunk)
               :cljs ...))

(def chunked-seq? (t/isa? #?(:clj clojure.lang.IChunkedSeq :cljs cljs.core/ChunkedSeq)))

(def indexed-seq? (t/isa? #?(:clj clojure.lang.IndexedSeq :cljs cljs.core/IndexedSeq)))

(def key-seq? (t/isa? #?(:clj clojure.lang.APersistentMap$KeySeq :cljs cljs.core/KeySeq)))

(def val-seq? (t/isa? #?(:clj clojure.lang.APersistentMap$ValSeq :cljs cljs.core/ValSeq)))

;; TODO CLJS
#?(:clj
(def range? (t/or (t/isa? clojure.lang.Range) (t/isa? clojure.lang.LongRange))))

;; TODO excise — this is used later on elsewhere
(def misc-seq? (t/or chunked-seq? indexed-seq? key-seq? val-seq?))

;; ----- Lists ----- ;; Not extremely different from Sequences ; TODO clean this up

(def cdlist? t/none? #_(:clj  (t/or (t/isa? clojure.data.finger_tree.CountedDoubleList)
                                    (t/isa? quantum.core.data.finger_tree.CountedDoubleList))
                        :cljs (t/isa? quantum.core.data.finger-tree/CountedDoubleList)))
(def dlist?  t/none? #_(:clj  (t/or (t/isa? clojure.data.finger_tree.CountedDoubleList)
                                    (t/isa? quantum.core.data.finger_tree.CountedDoubleList))
                        :cljs (t/isa? quantum.core.data.finger-tree/CountedDoubleList)))

(def +list?  (t/isa? #?(:clj clojure.lang.IPersistentList :cljs cljs.core/IList)))
(def !list?  #?(:clj (t/isa? java.util.LinkedList) :cljs t/none?))
(def  list?  #?(:clj (t/isa? java.util.List) :cljs +list?))

;; ===== End sequences ===== ;;

(def record? (t/isa? #?(:clj clojure.lang.IRecord :cljs cljs.core/IRecord)))

(def sorted?
  (t/or (t/isa? #?(:clj clojure.lang.Sorted :cljs cljs.core/ISorted))
        #?@(:clj  [(t/isa? java.util.SortedMap)
                   (t/isa? java.util.SortedSet)]
            :cljs [(t/isa? goog.structs.AvlTree)])
        ; TODO implement — monotonically <, <=, =, >=, >
      #_(t/>expr monotonic?)))

(def transient? (t/isa? #?(:clj  clojure.lang.ITransientCollection
                           :cljs cljs.core/ITransientCollection)))

(def editable? (t/isa? #?(:clj  clojure.lang.IEditableCollection
                          :cljs cljs.core/IEditableCollection)))

(def iindexed? (t/isa|direct? #?(:clj clojure.lang.Indexed :cljs cljs.core/IIndexed)))

;; Indicates efficient lookup by (integer) index (via `get`)
(def indexed?
  (t/or iindexed?
        ;; Doesn't guarantee `java.util.List` is implemented, except by convention
        #?(:clj (t/isa? java.util.RandomAccess))
        #?(:clj dstr/char-seq? :cljs dstr/string?)
        arr/array?))

(def  +associative? (t/isa? #?(:clj  clojure.lang.Associative
                               :cljs cljs.core/IAssociative)))

(def !+associative? (t/isa? #?(:clj  clojure.lang.ITransientAssociative
                               :cljs cljs.core/ITransientAssociative)))

;; Indicates whether `assoc?!` is supported
(def associative?
  (t/or +associative? !+associative? (t/or map/map? indexed?)))

(def sequential?
  (t/or (t/isa? #?(:clj clojure.lang.Sequential :cljs cljs.core/ISequential))
        list? indexed?))

(def icounted? (t/isa|direct? #?(:clj clojure.lang.Counted :cljs cljs.core/ICounted)))

;; If something is `counted?`, it is supposed to implement a constant-time `count`
;; `nil` is counted but this type ignores that
(def counted?
  (t/or icounted?
        ;; It's not guaranteed that `char-seq?`s have constant-time `.length`/`count` but it's very
        ;; reasonable to assume.
        #?(:clj dstr/char-seq? :cljs (t/or dstr/string? dstr/!string?))
        tup/tuple?
        ;; This kind of chan has a buffer which is countable
        dasync/m2m-chan?
        #?(:clj tup/map-entry?)
        ;; All enumerated vector types are all known to implement constant-time `count`.
        vec/vector?
        ;; It's not guaranteed that all `java.util.Map`s have constant-time `.size`/`count` but it's
        ;; about as reasonable to assume so as with `char-seq?`s.
        map/map?
        ;; It's not guaranteed that all `java.util.Set`s have constant-time `.size`/`count` but it's
        ;; about as reasonable to assume so as with `java.util.Map`s.
        set/set?
        arr/array?))

(def iterable? (t/isa|direct? #?(:clj java.lang.Iterable :cljs cljs.core/IIterable)))

#?(:clj (def java-coll? (t/isa? java.util.Collection)))

;; A group of objects/elements
(def coll?
  (t/or #?(:clj java-coll?)
        #?@(:clj  [(t/isa? clojure.lang.IPersistentCollection)
                   (t/isa? clojure.lang.ITransientCollection)]
            :cljs (t/isa? cljs.core/ICollection))
        sequential? associative?))

;; Whatever is `seqable?` is reducible via a call to `seq`.
;; Reduction is nearly always preferable to seq-iteration if for no other reason than that
;; it can take advantage of transducers and reducers. This predicate just answers whether
;; it is more efficient to reduce than to seq-iterate (note that it should be at least as
;; efficient as seq-iteration).
;; TODO re-enable when dispatch enabled
#_(def prefer-reduce?
  (t/or (t/isa? #?(:clj clojure.lang.IReduceInit :cljs cljs.core/IReduce))
        (t/isa? #?(:clj clojure.lang.IKVReduce   :cljs cljs.core/IKVReduce))
        #?(:clj (t/isa? clojure.core.protocols/IKVReduce))
        #?(:clj dstr/char-seq? :cljs dstr/string?)
        arr/array?
        record?
        (t/isa? #?(:clj fast_zip.core.ZipperLocation :cljs fast-zip.core/ZipperLocation))
        chan?))

;; Whatever is `reducible?` is seqable via a call to `sequence`.
(def seqable?
  (t/or #?@(:clj  [(t/isa? clojure.lang.Seqable) iterable? dstr/char-seq? map/map? arr/array?]
            :cljs [(t/isa? cljs.core/ISeqable) arr/array? dstr/string?])))

;; Able to be traversed over in some fashion, whether by `first`/`next` seq-iteration,
;; reduction, etc.
;; TODO re-enable when dispatch enabled
#_(def traversable?
  (t/or (t/isa? #?(:clj clojure.lang.IReduceInit :cljs cljs.core/IReduce))
        (t/isa? #?(:clj clojure.lang.IKVReduce :cljs cljs.core/IKVReduce))
        #?(:clj (t/isa? clojure.core.protocols/IKVReduce))
        (t/isa? #?(:clj clojure.lang.Seqable :cljs cljs.core/ISeqable))
        iterable?
        #?(:clj dstr/char-seq? :cljs dstr/string?)
        arr/array?
        (t/isa? #?(:clj fast_zip.core.ZipperLocation :cljs fast-zip.core/ZipperLocation))
        chan?))
