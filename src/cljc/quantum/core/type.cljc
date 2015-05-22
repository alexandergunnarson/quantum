(ns
  ^{:doc "Type-checking predicates, 'transientization' checks, class aliases, etc."
    :attribution "Alex Gunnarson"}
  quantum.core.type
  (:refer-clojure :exclude
    [vector? map? set? associative? seq? string? keyword? fn?
     nil? list? coll? char? symbol? number? integer? float? decimal?])
  (:require-quantum [fn logic ns set map macros err log])
  (:require [clojure.walk :refer [postwalk]]))

; TODO: Should include typecasting? (/cast/)

#?(:cljs (def class type))

#?(:clj (def instance+? instance?)
   :cljs
     (defn instance+? [class-0 obj] ; inline this?
       (try
         (instance? class-0 obj)
         (catch js/TypeError _
           (try (satisfies? class-0 obj))))))

(defn name-from-class
  [class-0]
  (let [^String class-str (str class-0)]
    (-> class-str
        (subs (-> class-str (.indexOf " ") inc))
        symbol)))

; ======= TRANSIENTS =======

; TODO this is just intuition. Better to |bench| it
; TODO move these vars
(def transient-threshold 3)
; macro because it will probably be heavily used  
#?(:clj
(defmacro should-transientize? [coll]
  `(and (editable? ~coll)
        (counted?  ~coll)
        (-> ~coll count (> transient-threshold)))))

(def double?     #?(:clj  (partial instance+? ADouble)
                    :cljs (fn-and ; TODO: probably a better way of finding out if it's a double/decimal
                             number?
                             (fn-> str (.indexOf ".") (not= -1))))) ; has decimal point
#?(:cljs (def boolean? (fn-or true? false?)))

; Unable to resolve symbol: isArray in this context
; http://stackoverflow.com/questions/2725533/how-to-see-if-an-object-is-an-array-without-using-reflection
; http://stackoverflow.com/questions/219881/java-array-reflection-isarray-vs-instanceof
;#?(:clj (def array?      (compr type (jfn java.lang.Class/isArray)))) ; uses reflection...
;#?(:clj (def array?      (fn-> type .isArray)))
#?(:clj (def indexed?    (partial instance+? clojure.lang.Indexed)))
        (def editable?   (partial instance+? AEditable))
        (def transient?  (partial instance+? ATransient))
#?(:clj (def throwable?  (partial instance+? java.lang.Throwable)))
        (def error?      (partial instance+? AError))

#?(:clj
  (def arr-types
    {:short    (type (short-array   0)      )
     :long     (type (long-array    0)      )
     :float    (type (float-array   0)      )
     :int      (type (int-array     0)      )
     :double   (type (double-array  0.0)    )
     :boolean  (type (boolean-array [false]))
     :byte     (type (byte-array    0)      )
     :char     (type (char-array    "")     )
     :object   (type (object-array  [])     )}))

(def types
  (let [hash-map-types
          #{#?@(:clj [clojure.lang.PersistentHashMap
                      clojure.lang.PersistentHashMap$TransientHashMap]
                :cljs [cljs.core/PersistentHashMap 
                       cljs.core/TransientHashMap])}
        array-map-types 
          #{#?@(:clj  [clojure.lang.PersistentArrayMap
                       clojure.lang.PersistentArrayMap$TransientArrayMap]
                :cljs [cljs.core/PersistentArrayMap
                       cljs.core/TransientArrayMap])}
        tree-map-types
          #{ATreeMap}
        map-types
          (set/union
            #{#?@(:clj [clojure.lang.IPersistentMap
                        java.util.Map])}
            hash-map-types
            array-map-types
            tree-map-types)
        array-list-types
          #{AArrList #?(:clj java.util.Arrays$ArrayList)}
        number-types
          #{#?@(:clj [java.lang.Long])}
        set-types
          #{#?@(:clj  [clojure.lang.APersistentSet
                       clojure.lang.IPersistentSet]
                :cljs [cljs.core/PersistentHashSet
                       cljs.core/TransientHashSet
                       cljs.core/PersistentTreeSet])}
        vec-types
          #{#?@(:clj  [clojure.lang.APersistentVector
                       clojure.lang.PersistentVector
                       clojure.lang.APersistentVector$RSeq]
                :cljs [cljs.core/PersistentVector
                       cljs.core/TransientVector])}
        list-types
          #{#?@(:clj  [java.util.List
                       clojure.lang.PersistentList]
                :cljs [cljs.core/List])}
        map-entry-types
          #{#?(:clj clojure.lang.MapEntry)}
        queue-types #{AQueue}
        transient-types #{ATransient}
        regex-types #{ARegex}
        associative-types
          (set/union map-types set-types vec-types)
        cons-types
          #{#?(:clj clojure.lang.Cons :cljs cljs.core.Cons)}
        lseq-types #{ALSeq}
        seq-types
          (set/union
            list-types
            queue-types
            lseq-types
            #{#?@(:clj  [clojure.lang.APersistentMap$ValSeq
                         clojure.lang.APersistentMap$KeySeq
                         clojure.lang.IndexedSeq]
                  :cljs [cljs.core/ValSeq
                         cljs.core/KeySeq
                         cljs.core/IndexedSeq
                         cljs.core/ChunkedSeq])})
        listy-types    seq-types
        indexed-types  vec-types
        bool-types     #{ABool}
        char-types     #{Character}
   #?@(:clj
       [short-types    #{java.lang.Short     }
        int-types      #{java.lang.Integer   }
        long-types     #{java.lang.Long      }
        bigint-types   #{clojure.lang.BigInt java.math.BigInteger}
        integer-types  (set/union short-types int-types
                         long-types bigint-types)
        float-types    #{java.lang.Float     }
        double-types   #{java.lang.Double    }
        bigdec-types   #{java.math.BigDecimal}
        decimal-types  (set/union
                         float-types double-types bigdec-types)])
        number-types   (set/union integer-types decimal-types
                         #{ANum})
        integral-types (set/union bool-types char-types number-types)
        fn-types       #{#?(:clj clojure.lang.Fn :cljs cljs.core/Fn)}
        coll-types
          (set/union
            seq-types associative-types array-list-types)]
    {          'char?         char-types
     #?@(:clj ['boolean?      bool-types
               'bool?         bool-types
               'short?        short-types
               'int?          int-types
               'long?         long-types
               'integer?      integer-types
               'float?        float-types
               'double?       double-types
               'decimal?      decimal-types])
               'number?       number-types
               'num?          number-types
               'tree-map?     tree-map-types
               'sorted-map?   tree-map-types
               'map?          map-types
               'map-entry?    map-entry-types
               'set?          set-types
               'vec?          vec-types
               'vector?       vec-types
               'list?         list-types
               'listy?        listy-types
               'cons?         cons-types
     #?@(:clj ['bigint?       bigint-types])
               'associative?  associative-types
               'lseq?         lseq-types
               'seq?          seq-types
               'queue?        queue-types
               'coll?         coll-types
               'indexed?      indexed-types
               'fn?           fn-types
               'nil?          #{nil}
     #?@(:clj ['transient?    transient-types
               'string?       #{#?(:clj String :cljs string)}
               'pattern?      regex-types
               'regex?        regex-types
               'symbol?       #{Symbol}
               'integral?     integral-types
               'qreducer?     #{#?@(:clj [clojure.core.protocols.CollReduce
                                         #_quantum.core.reducers.Folder])}
               'file?         #{java.io.File}])
     #?@(:clj ['array-list?   array-list-types
               'array?        (->> arr-types vals (into #{}))
               'object-array? #{(-> arr-types :object)}
               'byte-array?   #{(-> arr-types :byte  )}
               'long-array?   #{(-> arr-types :long  )}])
               'keyword?      #{#?(:clj clojure.lang.Keyword :cljs cljs.core/Keyword)}
               :default       #{#?@(:clj [Object nil] :cljs [default])}}))

#?(:clj 
(defmacro extend-protocol-for-all [prot & body]
 `(loop [body-n# '~body loop-ct# 0]
    (when (nempty? body-n#)
      (let [classes#   (first body-n#)
            fns#       (->> body-n# rest (take-while (fn-not core/vector?)))
            rest-body# (->> body-n# rest (drop (count fns#)))]
        (doseq [class-n# classes#]
          (let [fns-hinted#
                 (->> fns#
                      (map (fn [[f-sym# arities#]]
                             (log/ppr :macro-expand "f-sym# arities#" f-sym# arities#)
                             (let [arities-hinted#
                                    (->> (list arities#)
                                         (map (fn [[arglist-n# & body-n#]]
                                                (log/ppr :macro-expand "[arglist-n# & body-n#]" [arglist-n# body-n#])
                                                (let [; apparently arglist-hinting isn't enough... 
                                                      first-variadic-n?# (quantum.core.macros/first-variadic? arglist-n#)
                                                      arg-hinted#
                                                        (-> arglist-n# first
                                                            (with-meta 
                                                              {:tag (str (name-from-class class-n#))}))
                                                      _# (log/ppr :macro-expand "arg-hinted-meta" (meta arg-hinted#))
                                                      arglist-hinted#
                                                        (whenf arglist-n# (fn-not quantum.core.macros/first-variadic?)
                                                          (f*n assoc 0 arg-hinted#))
                                                      body-n#
                                                        (if first-variadic-n?#
                                                            body-n#
                                                            (->> body-n#
                                                                 (postwalk
                                                                   (condf*n
                                                                     (fn-and core/symbol? (f*n quantum.core.macros/symbol-eq? arg-hinted#))
                                                                       (fn [sym#] (log/ppr :macro-expand "SYMBOL FOUND; REPLACING" sym#) arg-hinted#) ; replace it
                                                                     quantum.core.macros/new-scope? identity
                                                                       #_(whenf*n (fn-> second (quantum.core.macros/shadows-var? arg-hinted#))
                                                                         #(throw (Exception. (str "Arglist shadows hinted arg |" arg-hinted# "|."
                                                                                                  " This is not supported:" %))))
                                                                     :else identity))))]
                                                       (log/ppr :macro-expand "arglist-hinted#" arglist-hinted#)
                                                  (cons arglist-hinted# body-n#))))
                                         doall)] (log/ppr :macro-expand "arities-hinted#" (cons f-sym# arities-hinted#))
                               (cons f-sym# arities-hinted#))))
                      doall) 
                prot-extension#
                  (apply list 'extend-protocol '~prot (eval class-n#) fns-hinted#)
                _# (log/ppr :macro-expand "EXTENDING PROTOCOL:" prot-extension#)]
            (eval prot-extension#)))
        (recur rest-body# (inc loop-ct#)))))))

(defmacro defnt-
  "Like |defn|, but dispatches based on type.

   An interesting experiment, but probably deprecated.
   Orders of magnitude slower than protocols, because of the time to read the class names, possibly.
   Protocols seem to be pretty well optimized."
  {:attribution "Alex Gunnarson"
   :in '(defnt testing
          list?    (([coll]
                      (println "THIS A LIST, 1 ARG" coll) coll)
                    ([coll arg]
                      (println "THIS IS A LIST:" coll) coll))
          vec?     ([coll arg]
                     (println "THIS IS A VEC:"  coll) coll)
          :default ([arg] (println "DEFAULT!" arg) (testing (list 1 2 3))))}
  [sym & {:as body}]
  (let [type-map-sym (gensym 'symbol-map)
        f-genned     (gensym 'f)]
    (intern *ns* sym nil) ; In case of recursive calls  
   `(let [_# (log/ppr :macro-expand "BODY" '~body)
          body-externed#
            (->> '~body
                 (clojure.walk/postwalk
                   (whenf*n quantum.core.macros/extern? (partial extern* *ns*))))
          type-map-temp# (atom {})
          ; Need to know the arities to be able to create the fn efficiently instead of all var-args
          ; {0 {:fixed    __f1__},
          ;  4 {:fixed    __f2__},
          ;  5 {:variadic __f3__}}
          genned-arities#  (atom {})
          f-genned# (atom nil)
          template#
            (fn [[arg0# :as arglist#]]
              `(~arglist#
                 (let [~'~f-genned
                        (or (->> ~arg0# class (get ~'~type-map-sym))
                            (get ~'~type-map-sym :default)
                            (throw (Exception. (str ~(str "|" '~sym "|" " not defined for ") (class ~arg0#)))))]
                   ~(apply list '~f-genned arglist#))))
          new-arity?#
            (fn [arglist#]
              ((fn-not contains?) @genned-arities# (count arglist#)))]
      ; Need to generate all the appropriate arities via a template. These are type-independent.
      ; Once a particular arity is generated, it need not be generated again.
      (doseq [[pred# arities-n#] body-externed#]
        (let [; Normalizes the arities to be in an arity-group, even if there's only one arity for that type 
              arities-normalized#
                (whenf arities-n# (fn-> first core/vector?) list)
              arities-for-type# (atom {})]
          (doseq [[arglist# & f-body#] arities-normalized#]
            (log/pr :macro-expand "pred arglist f-body |" pred# arglist# f-body#)
            ; Arity checking within type
            (let [arity-n#          (count arglist#)
                  arity-type-n#     (arity-type arglist#)
                  curr-variadic-arity-for-type#
                    (->> @arities-for-type#
                         (filter (fn-> val (= :variadic)))
                         first
                         (<- whenf nnil? key))
                  curr-variadic-arity#
                    (->> @genned-arities#
                         (filter (fn-> val keys first (= :variadic)))
                         first
                         (<- whenf nnil? key))]
              (when (contains? @arities-for-type# arity-n#)
                (throw+ {:msg (str "Cannot define more than one version of the same arity "
                                   "(" arity-n# ")"
                                   " for the given type:" pred#)}))
              (when (< (long arity-n#) (long (or curr-variadic-arity-for-type# 0)))
                (throw+ {:msg (str "Cannot define a version of the same arity with fewer than variadic args.")}))
              (when (and curr-variadic-arity-for-type# (= arity-type-n# :variadic))
                (throw+ {:msg (str "Cannot define multiple variadic overloads for type.")}))
              (swap! arities-for-type# assoc arity-n# arity-type-n#)
              ; Arity checking for typed function as a whole
              (when (new-arity?# arglist#)
                (when (and curr-variadic-arity# (= arity-type-n# :variadic))
                  (throw+ {:msg (str "Cannot define multiple variadic overloads for typed function.")}))
                (let [genned-arity# (template# arglist#)]
                  (log/pr :macro-expand "GENNED ARITY FOR ARGLIST" arglist#)
                  (log/ppr :macro-expand genned-arity#)
                  (swap! genned-arities# assoc-in [arity-n# arity-type-n#] genned-arity#)))))))
       
       ; Create type overloads 
       (doseq [[pred# arities-n#] body-externed#] 
         (log/pr :macro-expand (str "ADDING OVERLOAD FOR PRED " pred#))
         (doseq [type# (get type/types pred#)]
           (let [arities-n-normalized#
                   (whenf arities-n# (fn-> first core/vector?) list)
                 arities-n-f#
                   ; for each sub-arity
                   (for [[arglist-n# & body-n#] arities-n-normalized#]
                     ; modify the arglist (if the first symbol isn't variadic)
                     ; so that reflection is avoided
                     (let [arglist-f#
                            (if (or (-> arglist-n# first (= '&))
                                    ((fn-not class?) type#))
                                arglist-n#
                                (assoc arglist-n# 0
                                  (with-meta (first arglist-n#)
                                    {:tag (name-from-class type#)})))]
                       (apply list arglist-f# body-n#)))
                 fn-f# (apply list 'fn arities-n-f#)
                 _#    (log/ppr :macro-expand
                         (str "FINAL F FOR PRED " pred#
                              " MUNGED CLASSNAME " (name-from-class type#)) fn-f#)]
           (swap! type-map-temp# assoc type#
             (eval fn-f#)))))

      (intern *ns* '~type-map-sym (deref type-map-temp#))
      (log/pr :macro-expand "TYPE MAP" (resolve '~type-map-sym))

      (let [genned-arities-f#
              (->> @genned-arities#
                   vals
                   (map vals)
                   (map first))
            genned-fn-f# (apply list 'defn '~sym genned-arities-f#)]
        (log/ppr :macro-expand "GENNED FN FINAL" genned-fn-f#)
        (->> genned-fn-f#
             (clojure.walk/postwalk
               (whenf*n (fn-and core/symbol? qualified? auto-genned?) unqualify))
             eval)))))

#?(:clj
(defn defnt*
  [ns- sym doc- meta- body [unk & rest-unk]]
  (if unk
        (cond
          (core/string? unk)
            (defnt* ns- sym unk  meta- body                rest-unk)
          (core/map?    unk)     
            (defnt* ns- sym doc- unk   body                rest-unk)
          ((fn-or core/symbol? core/keyword?) unk)
            (defnt* ns- sym doc- meta- (cons unk rest-unk) nil     )
          :else
            (throw+ {:msg "Invalid arguments to |defnt|." :cause unk}))
        (let [_ (log/ppr :macro-expand "ORIG BODY:" body)
              body-f
                (->> body
                     (clojure.walk/postwalk
                       (whenf*n quantum.core.macros/extern? (fn->> (quantum.core.macros/extern* ns-)))))
              _ (log/ppr :macro-expand "OPTIMIZED BODY:" body-f)
              genned-protocol-name 
                (-> sym name quantum.core.macros/camelcase (str "Protocol") symbol)
              ; {string? (([s] s)), int? (([i] i))}
              arities
                (->> body-f
                     (apply ordered-map)
                     (map
                       (fn [[pred arities-n]]
                         (map-entry pred
                           (whenf arities-n (fn-> first core/vector?) list))))
                     (apply merge (ordered-map)))
              _ (log/ppr :macro-expand "ARITIES:" arities)
              arglists
                (->> arities
                     vals
                     (map (fn->> (map first) (into #{})))
                     (apply set/union)
                     (map (fn [arglist]
                            (map-entry (count arglist) arglist)))
                     (merge {})
                     vals)
              _ (log/ppr :macro-expand "ARGLISTS:" arglists)
              protocol-def
                `(defprotocol ~genned-protocol-name
                   ~(cons sym arglists))
              _ (log/ppr :macro-expand "PROTOCOL DEF:" protocol-def)
              protocol-body
                (->> arities
                     (map (fn [[pred arities-n]]
                            (let [classes-for-pred
                                   (->> pred (get quantum.core.type/types) (into []))]
                              (assert (nempty? classes-for-pred) (str "No classes found for predicate |" pred "|"))
                              (map-entry
                                classes-for-pred
                                (cons sym arities-n)))))
                     (apply concat))
              protocol-extension 
                (apply list `extend-protocol-for-all genned-protocol-name protocol-body)
              _ (log/ppr :macro-expand "PROTOCOL EXTENSION:" protocol-extension)
              final-protocol-def
                (list 'do protocol-def protocol-extension)]
          {:prot  final-protocol-def
           :sym-f (with-meta sym (merge {:doc doc-} meta-))}))))

#?(:clj
(defmacro defnt [sym & body]
  (let [{:keys [sym-f prot]}
         (defnt* *ns* (second `(list ~sym)) nil nil nil (second `(list ~body)))
        meta-f (meta sym-f)]
    (eval prot)
    `(doto (var ~sym)
       (reset-meta! ~meta-f)))))

(->> types
     (map
       (fn [[type-pred types]]
         (when (core/symbol? type-pred)
           (eval
             (concat
               (list 'defnt type-pred)
               (list type-pred '([obj] true)
                     :default  '([obj] false))
               (if (quantum.core.macros/symbol-eq? type-pred 'nil?)
                   (list)
                   (list 'nil? '([obj] false))))))))
     doall)

(def map-entry?  #?(:clj  (partial instance+? clojure.lang.MapEntry)
                    :cljs (fn-and core/vector? (fn-> count (= 2)))))