(ns quantum.core.macros.protocol
  (:require-quantum [:core fn logic cmacros log tcore])
  (:require [quantum.core.analyze.clojure.predicates :as anap
              :refer [type-hint]]
            [quantum.core.macros.transform :as trans]
            [quantum.core.collections.base
              :refer [update-first update-val ensure-set
                      zip-reduce default-zipper]]))

; ===== |PROTOCOL|S & |REIFY|S =====

(defn ?Object      [lang] (condp = lang :clj 'Object                :cljs 'Object      ))
(defn ?Seqable     [lang] (condp = lang :clj 'Seqable               :cljs 'ISeqable    ))
(defn ?Counted     [lang] (condp = lang :clj 'Counted               :cljs 'ICounted    ))
(defn ?Indexed     [lang] (condp = lang :clj 'Indexed               :cljs 'IIndexed    ))
(defn ?Sequential  [lang] (condp = lang :clj 'Sequential            :cljs 'ISequential ))
(defn ?Seq         [lang] (condp = lang :clj 'ISeq                  :cljs 'ISeq        ))
(defn ?Stack       [lang] (condp = lang :clj 'IPersistentStack      :cljs 'IStack      ))
(defn ?Collection  [lang] (condp = lang :clj 'IPersistentCollection :cljs 'ICollection ))
(defn ?Reversible  [lang] (condp = lang :clj 'Reversible            :cljs 'IReversible ))
(defn ?Associative [lang] (condp = lang :clj 'Associative           :cljs 'IAssociative))
(defn ?Lookup      [lang] (condp = lang :clj 'ILookup               :cljs 'ILookup     ))
(defn ?HashEq      [lang] (condp = lang :clj 'IHashEq               :cljs 'IHash       ))

(defn pfn
  "Protocol fn"
  [sym lang]
  (symbol (condp = lang
            :clj  sym
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

#?(:clj
(defmacro deftype-compatible
  {:usage '(deftype-compatible :clj
             'EmptyTree
             '[meter-obj]
             {'?Seqable
               {'first `[[a#] (+ a# 1)]}})}
  [lang sym-0 arglist-0 skel-0]
  ;(println "SKEL"skel)
  ;(println "(get-in skel ['?Seq 'first])" (get-in skel ['?Seq 'first]))
  (let [sym     (eval sym-0)
        arglist (eval arglist-0)
        skel    (eval skel-0)
        sym* sym]
 `(deftype ~sym* ~arglist
    ~(?Seqable lang)
      ~@(p-arity (pfn 'seq lang) (get-in skel ['?Seqable 'seq]))
    ~@(if (contains? skel '?Sequential) [(?Sequential lang)] [])
    ~@(condp = lang
        :clj
         `[~(?Seq lang)
             ~@(p-arity 'first  (get-in skel ['?Seq 'first]))
             ~@(p-arity 'more   (get-in skel ['?Seq 'rest ]))
             ~@(p-arity 'next   (get-in skel ['?Seq 'next ]))]
        :cljs
         `[~(?Seq lang)
             ~@(p-arity '-first (get-in skel ['?Seq 'first]))
             ~@(p-arity '-rest  (get-in skel ['?Seq 'rest ]))
           cljs.core/INext
             ~@(p-arity '-next  (get-in skel ['?Seq 'next ]))])
    ~(?Stack lang)
      ~@(p-arity (pfn 'peek  lang) (get-in skel ['?Stack      'peek ]))
      ~@(p-arity (pfn 'pop   lang) (get-in skel ['?Stack      'pop  ]))
    ~(?Reversible lang)
      ~@(p-arity (pfn 'rseq  lang) (get-in skel ['?Reversible 'rseq ]))
    ~(?Counted lang)
      ~@(p-arity (pfn 'count lang) (get-in skel ['?Counted    'count])) ; not needed?
    ~@(when (contains? skel '?Object)
        (condp = lang
          :clj
            `[~(?Object lang)
               ~@(p-arity 'equals   (get-in skel ['?Object 'equals]))
               ~@(p-arity 'hashCode (get-in skel ['?Object 'hash  ]))]
          :cljs
            `[~(?Object lang)
               ~@(p-arity 'equiv    (get-in skel ['?Object 'equals]))]))
    ~@(when (contains? skel '?Hash)
        (condp = lang
          :clj
            `[~(?HashEq lang)
               ~@(p-arity 'hasheq     (get-in skel ['?HashEq   'hash-eq  ]))]
          :cljs
            `[~(?HashEq lang)
               ~@(p-arity '-hash      (get-in skel ['?HashEq   'hash-eq  ]))]))
    ~@(when (contains? skel '?Meta)
        (condp = lang
          :clj
            `[clojure.lang.IObj
               ~@(p-arity 'meta       (get-in skel ['?Meta   'meta     ]))
               ~@(p-arity 'withMeta   (get-in skel ['?Meta   'with-meta]))]
          :cljs
            `[cljs.core/IMeta
               ~@(p-arity '-meta      (get-in skel ['?Meta   'meta     ]))
              cljs.core/IWithMeta
               ~@(p-arity '-with-meta (get-in skel ['?Meta   'with-meta]))]))
    ~@(when (contains? skel '?Collection)
        (condp = lang
          :clj
           `[~(?Collection lang)
               ~@(p-arity 'empty  (get-in skel ['?Collection 'empty ]))
               ~@(p-arity 'equiv  (get-in skel ['?Collection 'equals])) ; TBD
               ~@(p-arity 'cons   (get-in skel ['?Collection 'conj  ]))]
          :cljs 
           `[cljs.core/IEmptyableCollection
               ~@(p-arity '-empty (get-in skel ['?Collection 'empty ]))
             cljs.core/IEquiv
               ~@(p-arity '-equiv (get-in skel ['?Collection 'equals])) ; TBD
             ~(?Collection lang)
               ~@(p-arity '-conj  (get-in skel ['?Collection 'conj  ]))]))
    ~@(when (contains? skel '?Lookup)
        `[~(?Lookup lang)
            ~@(p-arity (condp = lang
                         :clj  'valAt
                         :cljs '-lookup)
               (get-in skel ['?Lookup 'val-at]))])
    ~@(when (contains? skel '?Associative)
        (condp = lang
          :clj  `[~(?Associative lang)
                  ~@(p-arity (pfn 'assoc lang) (get-in skel ['?Associative 'assoc    ]))
                  ~@(p-arity 'containsKey      (get-in skel ['?Associative 'contains?]))
                  ~@(p-arity 'entryAt          (get-in skel ['?Associative 'entry-at ]))]
          :cljs `[~(?Associative lang)
                  ~@(p-arity (pfn 'assoc lang) (get-in skel ['?Associative 'assoc  ]))])
        )
    ~@(when (contains? skel '?Indexed)
        `[~(?Indexed lang)
            ~@(p-arity (pfn 'nth lang)   (get-in skel ['?Indexed 'nth]))])
    ~@(when (and (contains? skel 'Iterator)
                 (= lang :clj))
        `[Iterator
           ~@(p-arity 'iterator (get-in skel ['Iterator 'iterator]))])
    ~'ConjL
      ~@(p-arity 'conjl       (get-in skel ['ConjL   'conjl   ]))
    ~'Measured
      ~@(p-arity 'measured    (get-in skel ['Measured 'measured]))
      ~@(p-arity 'getMeter    (get-in skel ['Measured 'getMeter])) ; not needed?
    ~@(when (contains? skel 'Splittable)
        `[~'Splittable
           ~@(p-arity 'split (get-in skel ['Splittable 'split]))])
    ; Splittable
    ;   (split [pred acc]) ; TBD -- not needed??
    ~'Tree
      ~@(p-arity 'app3        (get-in skel ['Tree 'app3       ]))
      ~@(p-arity 'app3deep    (get-in skel ['Tree 'app3deep   ]))
      ~@(p-arity 'measureMore (get-in skel ['Tree 'measureMore]))
      ~@(p-arity 'measurePop  (get-in skel ['Tree 'measurePop ]))))))

(def ^{:doc "Primitive type hints translated into protocol-safe type hints."}
  protocol-type-hint-map
  '{:clj  {boolean java.lang.Boolean
           byte    long
           char    java.lang.Character
           short   long
           int     long
           float   double}
    :cljs {boolean boolean}})

(defn ensure-protocol-appropriate-type-hint [lang i hint]
  (when-not (and (> i 0) (= hint 'Object)) ; The extra object hints mess things up
    (if-let [protocol-appropriate-type-hint (get-in protocol-type-hint-map [lang hint])]
      protocol-appropriate-type-hint
      hint)))

(defn ensure-protocol-appropriate-arglist [lang arglist-0]
  (->> arglist-0
       (map-indexed
         (fn [i arg]
           (cmacros/hint-meta arg
             (ensure-protocol-appropriate-type-hint lang i (type-hint arg)))))
       (into [])))

(defn gen-protocol-from-interface
  {:in '[[[Func [String IPersistentVector] long]]
         [[Func [String ITransientVector ] long]]]}
  [{:keys [gen-interface-code-body-expanded
           genned-protocol-name
           genned-protocol-method-name]}]
  (let [protocol-def-body
          (->> gen-interface-code-body-expanded
               (map (fn-> first second))
               (group-by count)
               (map (fn-> val first trans/gen-arglist))
               (cons genned-protocol-method-name))
        protocol-def 
          (list 'defprotocol genned-protocol-name protocol-def-body)]
    protocol-def))

; TODO hopefully get rid of this step
#?(:clj 
(defn gen-extend-protocol-from-interface
  ; Original Interface:         ([#{number?} x #{number?} y #{char? Object} z] ~@body)
  ; Expanded Interface Arity 1: ([^long      x ^int       y ^char           z] ~@body)
  ; Protocol Arity 1:           ([^long      x ^Integer   y ^Character      z]
  ;                               (let [y (int y) z (char z)] ; TODO ->int y, ->charz
  ;                                 ~@body)

  ; (def abcde 0)
  ; ((fn [^"[B" x ^long n] (clojure.lang.RT/aget x n)) my-byte-array abcde) => 0, no reflection
  [{:keys [genned-protocol-name genned-protocol-method-name
           reify-body lang first-types]}]
  (log/ppr-hints :macro-expand-protocol "REIFY BODY" reify-body)
  (assert (nempty? reify-body))
  (assert (nnil? genned-protocol-name))
  (assert (nnil? genned-protocol-method-name))

  (let [body-sorted
          (->> reify-body rest rest
               (map (fn [[sym & body]]
                      (-> body (update-first (f*n cmacros/hint-meta (type-hint sym))))))) ; remove reify method names
        body-filtered body-sorted
        _ (log/ppr-hints :macro-expand-protocol "BODY SORTED" body-sorted)
        body-mapped
          (->> body-filtered
               (map (fn [[arglist & body :as method]]
                      (let [first-type         (-> arglist second type-hint)
                            first-type-unboxed (-> first-type tcore/->unboxed)
                            unboxed-version-exists? (get-in first-types [first-type-unboxed (-> arglist count dec)])]
                        (if ; Unboxed version of arity already exists? Skip generation of that type/arity
                            (and (tcore/boxed? first-type) unboxed-version-exists?) ; |dec| because 'this' is the first arg
                            [] ; Empty so concat gets nothing
                            (let [boxed-first-type (-> first-type (whenp (= lang :clj) tcore/->boxed)) 
                                             ; (whenc (-> arglist second type-hint tcore/->boxed)
                                             ;        (fn-> name (= "[Ljava.lang.Object;"))
                                             ;   '(Class/forName "[Ljava.lang.Object;"))
                                  return-type (->> arglist type-hint
                                                   (ensure-protocol-appropriate-type-hint lang 0))
                                  arglist-f (->> arglist rest (ensure-protocol-appropriate-arglist lang))
                                  arglist-f (if return-type
                                                arglist-f
                                                (cmacros/hint-meta arglist-f return-type) )
                                  body-f (trans/hint-body-with-arglist body arglist-f lang :protocol)
                                  extension-f [boxed-first-type (cons genned-protocol-method-name (cons arglist-f body-f))]]
                              (if (or (= boxed-first-type 'Object)
                                      (= boxed-first-type 'java.lang.Object))
                                  (into ['nil (-> extension-f rest first)] extension-f)
                                  extension-f))))))
               (apply concat)
               (partition 2))
        body-grouped
          (->> body-mapped
               (group-by first)
               (map (fn [[k v]] (->> v (map (fn->> second rest)) (cons genned-protocol-method-name) (list k))))
               (map (partial into [])))
        _ (log/ppr-hints :macro-expand-protocol "BODY GROUPED" body-grouped)
        extend-protocol-def
         (apply concat (list 'extend-protocol genned-protocol-name) body-grouped)]
    extend-protocol-def)))