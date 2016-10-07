(ns quantum.core.macros.deftype
  (:require
    [quantum.core.collections.base
      :refer [update-first update-val ensure-set]]
    [quantum.core.macros.core
      :refer [#?@(:clj [if-cljs])]]))

; ===== |PROTOCOL|S & |REIFY|S =====

(defn ?Object        [lang] (case lang :clj 'Object                             :cljs 'Object                ))
(defn ?Seqable       [lang] (case lang :clj 'clojure.lang.Seqable               :cljs 'cljs.core/ISeqable    ))
(defn ?Counted       [lang] (case lang :clj 'clojure.lang.Counted               :cljs 'cljs.core/ICounted    ))
(defn ?Indexed       [lang] (case lang :clj 'clojure.lang.Indexed               :cljs 'cljs.core/IIndexed    ))
(defn ?Sequential    [lang] (case lang :clj 'clojure.lang.Sequential            :cljs 'cljs.core/ISequential ))
(defn ?Seq           [lang] (case lang :clj 'clojure.lang.ISeq                  :cljs 'cljs.core/ISeq        ))
(defn ?Stack         [lang] (case lang :clj 'clojure.lang.IPersistentStack      :cljs 'cljs.core/IStack      ))
(defn ?Collection    [lang] (case lang :clj 'clojure.lang.IPersistentCollection :cljs 'cljs.core/ICollection ))
(defn ?Reversible    [lang] (case lang :clj 'clojure.lang.Reversible            :cljs 'cljs.core/IReversible ))
(defn ?Associative   [lang] (case lang :clj 'clojure.lang.Associative           :cljs 'cljs.core/IAssociative))
(defn ?MutableMap    [lang] (case lang :clj 'java.util.Map))
(defn ?Map           [lang] (case lang :clj 'clojure.lang.IPersistentMap        :cljs 'cljs.core/IMap        ))
(defn ?Lookup        [lang] (case lang :clj 'clojure.lang.ILookup               :cljs 'cljs.core/ILookup     ))
(defn ?HashEq        [lang] (case lang :clj 'clojure.lang.IHashEq               :cljs 'cljs.core/IHash       ))
(defn ?Record        [lang] (case lang :clj 'clojure.lang.IRecord               :cljs 'cljs.core/IRecord     ))
(defn ?Iterable      [lang] (case lang :clj 'java.lang.Iterable                 :cljs 'cljs.core/IIterable   ))
(defn ?Deref         [lang] (case lang :clj 'clojure.lang.IDeref                :cljs 'cljs.core/IDeref      ))

(defn pfn
  "Protocol fn"
  [sym lang]
  (symbol (case lang
            :clj  (name sym)
            :cljs (str "-" (name sym)))))

(defn p-arity
  "Protocol arity-maker"
  {:tests '{(p-arity 'abc '([a] 123))
              '[(abc [a] 123)]
            (p-arity 'abc '(([a] 123) ([b] 234)))
              '[(abc [a] 123)
                (abc [b] 234)]}}
  [sym arities]
  (if (-> arities first vector?)
      [(apply list sym arities)]
      (->> arities
           (mapv (fn [arity] (cons sym arity))))))

(defn implement-map-or-collection [skel]
  (if (or (contains? skel '?Lookup     )
          (contains? skel '?Associative))
      'java.util.Map
      'java.util.Collection))

(defn deftype-compatible-helper
  [skel lang]
  (for [[iname impls] skel]
    (case iname
      ?Seqable
        `[~(?Seqable lang)
          ~@(p-arity (pfn 'seq lang) (get impls 'seq))]
      ?Sequential
        [(?Sequential lang)]
      ?Record
        [(?Record     lang)]
      ?Serializable
        '[?java.io.Serializable] ; TODO CLJS
      ?Seq
        (case lang
          :clj
           `[~(?Seq lang)
               ~@(p-arity 'first  (get impls 'first))
               ~@(p-arity 'more   (get impls 'rest ))
               ~@(p-arity 'next   (get impls 'next ))]
          :cljs
           `[~(?Seq lang)
               ~@(p-arity '-first (get impls 'first))
               ~@(p-arity '-rest  (get impls 'rest ))
             cljs.core/INext
               ~@(p-arity '-next  (get impls 'next ))])
      ?Stack
         `[~(?Stack lang)
           ~@(p-arity (pfn 'peek lang) (get impls 'peek))
           ~@(p-arity (pfn 'pop  lang) (get impls 'pop ))]
      ?Reversible
         `[~(?Reversible lang)
           ~@(p-arity (pfn 'rseq  lang) (get impls 'rseq ))]
      ?Counted
         `[~(?Counted lang)
           ~@(p-arity (pfn 'count lang) (get impls 'count))
           ~@(case lang :clj
               `[~(implement-map-or-collection skel)
                 ~@(p-arity 'size (get impls 'count))])]
      ?Object
        (case lang
          :clj
            `[~(?Object lang)
               ~@(p-arity 'equals   (get impls 'equals))
               ~@(p-arity 'hashCode (get impls 'hash  ))]
          :cljs
            `[~(?Object lang)
               ~@(p-arity 'equiv    (get impls 'equals))])
      ?Hash
        (case lang
          :clj
            `[~(?HashEq lang)
               ~@(p-arity 'hashEq     (get-in skel '[?HashEq hash-eq]))]
          :cljs
            `[~(?HashEq lang)
               ~@(p-arity '-hash      (get-in skel '[?HashEq hash-eq]))])
      ?Meta
        (case lang
          :clj
            `[~'clojure.lang.IObj
               ~@(p-arity 'meta       (get impls 'meta     ))
               ~@(p-arity 'withMeta   (get impls 'with-meta))]
          :cljs
            `[cljs.core/IMeta
               ~@(p-arity '-meta      (get impls 'meta     ))
              cljs.core/IWithMeta
               ~@(p-arity '-with-meta (get impls 'with-meta))])
      ?Collection
        (case lang
          :clj
           `[~(?Collection lang)
               ~@(p-arity 'empty   (get impls 'empty ))
               ~@(p-arity 'equiv   (get impls 'equals)) ; TBD
               ~@(p-arity 'cons    (get impls 'conj  ))
             ~(implement-map-or-collection skel)
               ~@(p-arity 'isEmpty (get impls 'empty?))
               ~@(p-arity 'clear   (get impls 'empty!))]
          :cljs
           `[cljs.core/IEmptyableCollection
               ~@(p-arity '-empty (get impls 'empty ))
             cljs.core/IEquiv
               ~@(p-arity '-equiv (get impls 'equals)) ; TBD
             ~(?Collection lang)
               ~@(p-arity '-conj  (get impls 'conj  ))])
      ?Lookup
        `[~(?Lookup lang)
          ~@(p-arity (case lang :clj 'valAt :cljs '-lookup) (get impls 'get))
          ~@(case lang
              :clj `[~'clojure.lang.IKeywordLookup
                       ~@(p-arity 'getLookupThunk (get impls 'kw-get))
                     ~'java.util.Map
                       ~@(p-arity 'containsValue  (get impls 'containsv?))
                       ~@(p-arity 'get            (get impls 'get))]
              :cljs nil)]
      ?Associative
        `[~(?Associative   lang)
          ~@(p-arity (pfn 'assoc  lang) (get impls 'assoc    ))
          ~@(case lang
              :clj  `[  ~@(p-arity 'containsKey (get-in skel '[?Lookup      contains?]))
                        ~@(p-arity 'entryAt     (get-in skel '[?Lookup      get-entry]))
                      ~(?Map lang)
                        ~@(p-arity 'without     (get impls 'dissoc   ))
                      ~'java.util.Map
                        ~@(p-arity 'put         (get impls 'assoc!   ))
                        ~@(p-arity 'remove      (get impls 'dissoc!  ))
                        ~@(p-arity 'putAll      (get impls 'merge!   ))
                        ~@(p-arity 'keySet      (get impls 'keys     ))
                        ~@(p-arity 'values      (get impls 'vals     ))
                        ~@(p-arity 'entrySet    (get impls 'entries  ))]
              :cljs `[~(?Map lang)
                      ~@(p-arity (pfn 'dissoc lang) (get impls 'dissoc   ))])]
      ?Indexed
        `[~(?Indexed lang)
          ~@(p-arity (pfn 'nth lang) (get impls 'nth))]
      ?Iterable
        (case lang
          :clj `[~(?Iterable lang)
                 ~@(p-arity 'iterator (get impls 'iterator))]
          :cljs nil)
      ?Print
        (case lang
          :clj  nil
          :cljs `[cljs.core/IPrintWithWriter
                  ~@(p-arity (pfn 'pr-writer lang) (get impls 'pr))])
      ?Deref
        `[~(?Deref lang)
          ~@(p-arity (pfn 'deref lang) (get impls 'deref))]
      ?HashEq
        nil
      `[~iname
        ~@(apply concat
           (for [[name- arities] impls] (p-arity name- arities)))])))

#?(:clj
(defmacro deftype-compatible
  {:usage '(deftype-compatible
             EmptyTree
             [meter-obj]
             {?Seqable
               {first ([a] (+ a 1))}})}
  [sym arglist skel]
  (let [lang (if-cljs &env :cljs :clj)]
   `(deftype ~sym ~arglist
      ~@(apply concat (deftype-compatible-helper skel lang))))))
