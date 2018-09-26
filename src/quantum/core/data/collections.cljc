(ns quantum.core.data.collections
  (:refer-clojure :exclude
    [associative? indexed? list? sequential?])
  (:require
    [quantum.core.data.array  :as arr]
    [quantum.core.data.map    :as map]
    [quantum.core.data.set    :as set]
    [quantum.core.data.string :as dstr]
    [quantum.core.data.tuple  :as tuple]
    [quantum.core.data.vector :as vec]
    [quantum.core.type :as t]))

(def record? (t/isa? #?(:clj clojure.lang.IRecord :cljs cljs.core/IRecord)))

;; TODO CLJS
(def iseq? (t/isa? #?(:clj clojure.lang.ISeq :cljs ...)))

;; TODO CLJS
(def chunk-buffer? #?(:clj  (t/isa? clojure.lang.ChunkBuffer)
                      :cljs ...))

;; TODO CLJS
(def chunk? #?(:clj  (t/isa? clojure.lang.IChunk)
               :cljs ...))

;; TODO CLJS
(def chunked-seq? #?(:clj  (t/isa? clojure.lang.IChunkedSeq)
                     :cljs ...))

;; TODO CLJS
#?(:clj
(def string-seq? (t/isa? clojure.lang.StringSeq)))

;; TODO CLJS
#?(:clj
(def range? (t/or (t/isa? clojure.lang.Range) (t/isa? clojure.lang.LongRange))))

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

;; Indicates efficient lookup by (integer) index (via `get`)
(def indexed?
  (t/or (t/isa? #?(:clj clojure.lang.Indexed :cljs cljs.core/IIndexed))
        ;; Doesn't guarantee `java.util.List` is implemented, except by convention
        #?(:clj (t/isa? java.util.RandomAccess))
        #?(:clj dstr/char-seq? :cljs dstr/string?)
        arr/array?))

;; Indicates whether `assoc?!` is supported
(def associative?
  (t/or (t/isa? #?(:clj  clojure.lang.Associative           :cljs cljs.core/IAssociative))
        (t/isa? #?(:clj  clojure.lang.ITransientAssociative :cljs cljs.core/ITransientAssociative))
        (t/or map/map? indexed?)))

(def sequential?
  (t/or (t/isa? #?(:clj clojure.lang.Sequential :cljs cljs.core/ISequential))
        list? indexed?))

;; If something is `counted?`, it implements a constant-time `count`
(def counted?
  (t/or (t/isa? #?(:clj clojure.lang.Counted :cljs cljs.core/ICounted))
        #?(:clj dstr/char-seq? :cljs dstr/string?) vec/vector? map/map? set/set? arr/array?))

(def iterable? (t/isa? #?(:clj java.lang.Iterable :cljs cljs.core/IIterable)))

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
