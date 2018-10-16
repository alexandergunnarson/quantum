(ns quantum.core.data.collections
  (:refer-clojure :exclude
    [associative? indexed? list? reduced? sequential?])
  (:require
    [quantum.core.data.array   :as arr]
    [quantum.core.data.map     :as map]
    [quantum.core.data.set     :as set]
    [quantum.core.data.string  :as dstr]
    [quantum.core.data.tuple   :as tup]
    [quantum.core.data.vector  :as vec]
    [quantum.core.type         :as t]
    [quantum.core.vars         :as var]
    ;; TODO TYPED excise
    [quantum.untyped.core.type :as ut]))

;; TODO move to `quantum.core.data.sequence`
;; ===== Sequences and sequence-wrappers ===== ;;
;; Sequential (generally not `lookup?`)
;; Note that lists and seqs are not fundamentally different and so we don't distinguish between them
;; here.


(def iseqable? (t/isa?|direct #?(:clj clojure.lang.Seqable :cljs cljs.core/ISeqable)))

(def iseq? (t/isa?|direct #?(:clj clojure.lang.ISeq :cljs cljs.core/ISeq)))

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

(def chunk-buffer? (t/isa? #?(:clj clojure.lang.ChunkBuffer :cljs cljs.core/ChunkBuffer)))

(def chunk? (t/isa?|direct #?(:clj clojure.lang.IChunk :cljs cljs.core/IChunk)))

(def chunked-cons? (t/isa? #?(:clj clojure.lang.ChunkedCons :cljs cljs.core/ChunkedCons)))

(var/def chunked-seq?
  "Note that `cljs.core/IChunkedSeq` has no interface for `chunked-next`, unliked
   `clojure.lang.IChunkedSeq`."
  (t/isa?|direct #?(:clj clojure.lang.IChunkedSeq :cljs cljs.core/IChunkedSeq)))

#?(:cljs (def chunked-next? (t/isa?|direct #?(:cljs cljs.core/IChunkedNext))))

(def indexed-seq? (t/isa? #?(:clj clojure.lang.IndexedSeq :cljs cljs.core/IndexedSeq)))

(def key-seq? (t/isa? #?(:clj clojure.lang.APersistentMap$KeySeq :cljs cljs.core/KeySeq)))

(def val-seq? (t/isa? #?(:clj clojure.lang.APersistentMap$ValSeq :cljs cljs.core/ValSeq)))

;; TODO CLJS
#?(:clj
(def range? (t/or (t/isa? clojure.lang.Range) (t/isa? clojure.lang.LongRange))))

;; TODO excise — this is used later on elsewhere
(def misc-seq? (t/or chunked-seq? indexed-seq? key-seq? val-seq?))

(var/defalias ut/+list|built-in?)

(def cdseq? t/none? #_(:clj  (t/or (t/isa? clojure.data.finger_tree.CountedDoubleList)
                                   (t/isa? quantum.core.data.finger_tree.CountedDoubleList))
                       :cljs (t/isa? quantum.core.data.finger-tree/CountedDoubleList)))
(def dseq?  t/none? #_(:clj  (t/or (t/isa? clojure.data.finger_tree.CountedDoubleList)
                                   (t/isa? quantum.core.data.finger_tree.CountedDoubleList))
                       :cljs (t/isa? quantum.core.data.finger-tree/CountedDoubleList)))

(def +list?  (t/isa? #?(:clj clojure.lang.IPersistentList :cljs cljs.core/IList)))

(def !seq?   #?(:clj (t/isa? java.util.LinkedList) :cljs t/none?))

;; ===== End sequences ===== ;;

(def transient? (t/isa?|direct #?(:clj  clojure.lang.ITransientCollection
                                  :cljs cljs.core/ITransientCollection)))

(def editable? (t/isa?|direct #?(:clj  clojure.lang.IEditableCollection
                                 :cljs cljs.core/IEditableCollection)))

(def record? (t/isa?|direct #?(:clj clojure.lang.IRecord :cljs cljs.core/IRecord)))

(var/def comparator-ordered?
  "Something that guarantees the invariant that its elements will always be ordered by some
   comparator."
  (t/or (t/isa?|direct #?(:clj clojure.lang.Sorted :cljs cljs.core/ISorted))
        #?@(:clj  [(t/isa? java.util.SortedMap)
                   (t/isa? java.util.SortedSet)]
            :cljs [(t/isa? goog.structs.AvlTree)])))

(var/def sorted?
  "Something that is either (necessarily) `comparator-ordered?` or contingently comparator-ordered
   (i.e. whose elements happen to be in monotonically decreasing or increasing order by `compare`)."
  (t/or comparator-ordered?
        ; TODO implement — it means monotonically <= or >=
        monotonic?))

(def iindexed? (t/isa?|direct #?(:clj clojure.lang.Indexed :cljs cljs.core/IIndexed)))

(var/def indexed?
  "Indicates efficient lookup by (`dn/integer?`) index (via `get`), and that its indices are dense.
   Thus indicates a collection that maintains a one-to-one mapping from `dn/integer?` keys to
   values.

   An `indexed?` is distinct from a non-`indexed?` `lookup?` whose keys densely satisfy `integer?`
   in that when traversed sequentially, the former will behave as sequence of (unindexed) elements
   while the latter will behave as a sequence of key-value pairs."
  (t/or iindexed?
        ;; Doesn't guarantee `java.util.List` is implemented, except by convention
        #?(:clj (t/isa? java.util.RandomAccess))
        #?(:clj dstr/char-seq? :cljs dstr/string?)
        arr/array?))

(def ilookup? (t/isa?|direct #?(:clj clojure.lang.Lookup :cljs cljs.core/ILookup)))

(var/def lookup?
  "Indicates efficient lookup by key (via `get`), and thus a collection that maintains a one-to-one
   mapping from keys to values. Technically, anything that is able to be the first input to `get`.

   Distinct from `map?` in that a `map?` is effectively
   `(t/- (t/and associative? lookup?) indexed?)`.

   A `lookup?` whose keys densely satisfy `integer?` is distinct from an `indexed?` in that when
   traversed sequentially, the former will behave as a sequence of key-value pairs while the latter
   will behave as a sequence of (unindexed) elements."
  (t/or ilookup? indexed?))

(var/def sequentially-ordered?
  "Collections defined by the fact that their elements must appear in a particular order,
   specifically due to the criterion of 'nextness' rather than e.g. a value-comparator or insertion
   order."
  (t/or (t/isa|direct? #?(:clj clojure.lang.Sequential :cljs cljs.core/ISequential))
        iseq?
        #?(:clj (t/isa? java.util.List))
        +list?
        indexed?
        ;; These four are insertion-ordered maps but when re-`assoc`ing (map) or re-`conj`ing (set),
        ;; the original sequence is retained, so really they're sequentially ordered and not purely
        ;; insertion-ordered.
        #?(:clj (t/isa? flatland.ordered.map.OrderedMap))
        #?(:clj (t/isa? flatland.ordered.set.OrderedSet))
        (t/isa? linked.map.LinkedMap)
        (t/isa? linked.set.LinkedSet)))

(var/def ordered?
  "Collections defined by the fact that their elements must appear in a particular order. Note:
   - `sequentially-ordered?` (even if non-`indexed?`) implies `ordered?`, as the ordering criterion
     can be thought of as each element's implicit sequential designator / index.
   - `indexed?` implies `ordered?`, as the ordering criterion can be thought of as each element's
     explicit sequential designator / index.
   - `comparator-ordered?` implies `ordered?` while `sorted?` does not necessarily, as while a
     collection may happen to be sorted, this does not imply that order is one of its defining
     aspects.
   - While all good hashing algorithms are deterministic, order is not (generally) guaranteed for
     hash-ordered collections."
  (t/or sequentially-ordered?
        comparator-ordered?))

(def  +associative? (t/isa?|direct #?(:clj  clojure.lang.Associative
                                      :cljs cljs.core/IAssociative)))

(def !+associative? (t/isa?|direct #?(:clj  clojure.lang.ITransientAssociative
                                      :cljs cljs.core/ITransientAssociative)))

(var/def associative?
  "Collections that can associate a (potentially new) key with a new value. Technically, anything
   that is able to be the first input to `assoc?!`."
  (t/or +associative? !+associative? (t/or map/map? indexed?)))

(def icounted? (t/isa?|direct #?(:clj clojure.lang.Counted :cljs cljs.core/ICounted)))

(var/def counted? "Objects guaranteed to implement a constant-time `count`."
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

(def iterable? (t/isa?|direct #?(:clj java.lang.Iterable :cljs cljs.core/IIterable)))

#?(:clj (def java-coll? (t/isa? java.util.Collection)))

(var/def coll?
  "An object that represents a grouping/collection of other objects (individually called elements)."
  (t/or #?(:clj java-coll?)
        #?@(:clj  [(t/isa? clojure.lang.IPersistentCollection)
                   (t/isa? clojure.lang.ITransientCollection)]
            :cljs (t/isa|direct? cljs.core/ICollection))
        ordered? lookup? associative?))

(def reduced? (t/isa? #?(:clj clojure.lang.Reduced :cljs cljs.core/Reduced)))

;; TODO non-boxing `>reduced`
(t/defn >reduced
  "Wraps ->`x` in a way such that a `reduce` will terminate with the value ->`x`."
  > reduced?
  [x t/ref?] (#?(:clj clojure.lang.Reduced. :cljs cljs.core/Reduced.) x))

(var/def reducible?
  "An object that is able to be reduced by some means. Technically, anything that is able to be the
   first input to `reduce`.

   All collection are reducible, but not all reducibles are collections (e.g. `nil?`, `numerically-integer?`, `dasync/read-chan?`, etc.)."
  (t/or p/nil? dstr/string? vec/!+vector? arr/array? dn/numerically-integer?
        ;; TODO what about `transformer?`
        dasync/read-chan?
        (t/isa? fast_zip.core.ZipperLocation)
        #?(:clj (t/isa? clojure.lang.IKVReduce))
        #?(:clj (t/isa? clojure.lang.IReduceInit))
        ;; We're ignoring indirect implementation for reasons noted in the `reduce` impl
        (t/isa?|direct #?(:clj  clojure.core.protocols/IKVReduce
                          :cljs cljs.core/IKVReduce))
        ;; We're ignoring indirect implementation for reasons noted in the `reduce` impl
        (t/isa?|direct #?(:clj  clojure.core.protocols/CollReduce
                          :cljs cljs.core/IReduce))
        iseq?
        iseqable?
        iterable?))

(var/def seqable?
  "Whatever is `seqable?` is `reducible?`, and whatever is `reducible?` is `seqable?`.
   Since reduction is preferred to 'manual' `first`/`next` seq traversal for performance and
   conceptual reasons, we prefer `reducible?` to `seqable?` as the base type."
  reducible?)
