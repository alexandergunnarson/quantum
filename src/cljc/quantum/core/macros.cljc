(ns
  ^{:doc "Some useful macros, like de-repetitivizing protocol extensions.
          Also some plumbing macros for |for| loops and the like."
    :attribution "Alex Gunnarson"}
  quantum.core.macros
  (:require
    [quantum.core.ns :as ns :refer
      #?(:clj  [alias-ns defalias]
         :cljs [Exception IllegalArgumentException
                Nil Bool Num ExactNum Int Decimal Key Vec Set
                ArrList TreeMap LSeq Regex Editable Transient Queue Map])]
    [quantum.core.type :as type :refer
      #?(:clj  [name-from-class bigint?
                instance+? array-list? boolean? double? map-entry? sorted-map?
                queue? lseq? coll+? pattern? regex? editable? transient?
                should-transientize?]
         :cljs [class instance+? array-list? boolean? double? map-entry? sorted-map?
                queue? lseq? coll+? pattern? regex? editable? transient?])]
    [quantum.core.log   :as log]
    [quantum.core.print :as pr]
    [quantum.core.numeric :as num]
    [quantum.core.error :as err   #?@(:clj [:refer [try+ throw+]])      ]
    [quantum.core.logic :refer
      #?(:clj  [splice-or nempty? fn-and fn-or fn-not nnil? ifn if*n whenc whenf whenf*n whencf*n condf condf*n]
         :cljs [splice-or nempty? fn-and fn-or fn-not nnil?])
      #?@(:cljs [:refer-macros [ifn if*n whenc whenf whenf*n whencf*n condf condf*n]])]
    [quantum.core.function :as fn :refer
      #?@(:clj  [[compr f*n fn* unary fn->> fn-> <- jfn]]
          :cljs [[compr f*n fn* unary]
                 :refer-macros [fn->> fn-> <-]])]
    #?(:clj [potemkin.types :as t]))
  #?(:cljs (:require-macros
    [quantum.core.type :refer [should-transientize?]]))
  #?(:clj (:gen-class)))

#?(:clj 
(defmacro extend-protocol-for-all [prot & body]
 `(loop [body-n# '~body loop-ct# 0]
  (when (and (nempty? body-n#) (< loop-ct# 3))
    (let [classes#   (first body-n#)
          ;_# (println "classes#" classes# "\n")
          fns#       (->> body-n# rest (take-while (fn-not vector?)))
          ;_# (println "fns#" fns# "\n")
          rest-body# (->> body-n# rest (drop (count fns#)))
          ;_# (println "rest-body#" rest-body# "\n")
          ct# (atom 0)]
      (doseq [class-n# classes#]
        (eval (apply list 'extend-protocol  '~prot (eval class-n#) fns#))
        ;(apply (mfn extend-protocol) ~prot (eval class-n#) fns#)
        (swap! ct# inc)
        (when (> @ct# 7) (throw (Exception.)))
        )
      (recur rest-body# (inc loop-ct#))))
    )))

#?(:clj
(defmacro extend-protocol-type
  [protocol prot-type & methods]
  `(extend-protocol ~protocol ~prot-type ~@methods)))

#?(:clj
(defmacro extend-protocol-types
  [protocol prot-types & methods]
  `(doseq [prot-type# ~prot-types]
     (extend-protocol-type ~protocol (eval prot-type#) ~@methods))))

; The most general.
; (defmacro extend-protocol-typed [expr]
;   (extend-protocol (count+ [% coll] (alength % coll))))

; (defmacro defprotocol+ [protocol & exprs]
;   '(let [methods# ; Just take the functions
;           (->> (rest exprs)
;                (take-while (fn->> str first+ (not= "["))))]
;      (defprotocol ~protocol
;        ~@methods#)
;      ;(extend-protocol-types protocol (first exprs) methods#)
;      ))

; (defmacro quote-exprs [& exprs]
;   `~(exprs))

#?(:clj (def assert-args #'clojure.core/assert-args))

(defn emit-comprehension
  {:attribution "clojure.core, via Christophe Grand - https://gist.github.com/cgrand/5643767"
   :todo ["Transientize the |reduce|s"]}
  [&form {:keys [emit-other emit-inner]} seq-exprs body-expr]
  (assert-args
     (vector? seq-exprs) "a vector for its binding"
     (even? (count seq-exprs)) "an even number of forms in binding vector")
  (let [groups (reduce (fn [groups [k v]]
                         (if (keyword? k)
                              (conj (pop groups) (conj (peek groups) [k v]))
                              (conj groups [k v])))
                 [] (partition 2 seq-exprs)) ; /partition/... hmm...
        inner-group (peek groups)
        other-groups (pop groups)]
    (reduce emit-other (emit-inner body-expr inner-group) other-groups)))

(defn do-mod [mod-pairs cont & {:keys [skip stop]}]
  (let [err (fn [& msg] (throw (IllegalArgumentException. ^String (apply str msg))))]
    (reduce 
      (fn [cont [k v]]
        (cond 
          (= k :let)   `(let ~v ~cont)
          (= k :while) `(if  ~v ~cont ~stop)
          (= k :when)  `(if  ~v ~cont ~skip)
          :else (err "Invalid 'for' keyword " k)))
      cont (reverse mod-pairs)))) ; this is terrible

#?(:clj
  (defmacro compile-if
    "Evaluate `exp` and if it returns logical true and doesn't error, expand to
    `then`.  Else expand to `else`.

    (compile-if (Class/forName \"java.util.concurrent.ForkJoinTask\")
      (do-cool-stuff-with-fork-join)
      (fall-back-to-executor-services))"
    {:attribution "clojure.core.reducers"}
    [exp then else]
    (if (try (eval exp)
             (catch Throwable _ false))
       `(do ~then)
       `(do ~else))))

(defmacro unquote-replacement
  "Replaces all duple-lists: (clojure.core/unquote ___) with the unquoted version of the inner content."
  [sym-map quoted-form]
  `(prewalk
     (fn [obj#]
       (if (and (comp/listy? obj#)
                (-> obj# count   (= 2))
                (-> obj# (nth 0) (= 'clojure.core/unquote)))
           (if (-> obj# (nth 1) (in? ~sym-map))
               (get ~sym-map (-> obj# (nth 1)))
               (throw+ {:msg (str/sp "Symbol does not evaluate to anything:" (-> obj# (nth 1)))}))
           obj#))
     ~quoted-form))

(defmacro quote+
  "Normal quoting with unquoting that works as in |syntax-quote|."
  {:in '[(let [a 1] (quote+ (for [b 2] (inc ~a))))]
   :out '(for [a 1] (inc 1))}
  [form]
 `(let [sym-map# (ns/local-context)]
    (unquote-replacement sym-map# '~form)))

(defn metaclass [sym] (-> sym meta :tag))

(defn qualified?   [sym] (-> sym str (.indexOf "/") (not= -1)))
(defn auto-genned? [sym] (-> sym name (.endsWith "__auto__")))
(defn unqualify    [sym] (-> sym name symbol))

(def variadic-arglist? (fn-> butlast last (= '&)))

(defn arity-type [arglist]
  (if (variadic-arglist? arglist)
      :variadic
      :fixed))

(def arglist-arity
  (if*n
    variadic-arglist?
    (fn-> count dec)
    count))

(defmacro defnt
  "Like |defn|, but dispatches based on type.
   Possibly faster than a protocol, at least when it ultimately comes to use a record instead of a hash-map."
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
                            (get ~'~type-map-sym :default))]
                   (if (nil? ~'~f-genned)
                       (throw (Exception. (str ~(str "|" '~sym "|" " not defined for ") (class ~arg0#))))
                       ~(apply list '~f-genned arglist#)))))
          new-arity?#
            (fn [arglist#]
              ((fn-not contains?) @genned-arities# (count arglist#)))]
      ; Need to generate all the appropriate arities via a template. These are type-independent.
      ; Once a particular arity is generated, it need not be generated again.
      (doseq [[pred# arities-n#] '~body]
        (let [; Normalizes the arities to be in an arity-group, even if there's only one arity for that type 
              arities-normalized#
                (whenf arities-n# (fn-> first vector?) list)
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
       (doseq [[pred# arities-n#] '~body] 
         (log/ppr :macro-expand (str "ADDING OVERLOAD FOR PRED " pred#) (apply list 'fn arities-n#))
         (doseq [type# (get type/types pred#)]
           (swap! type-map-temp# assoc type#
             (eval (apply list 'fn arities-n#)))))

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
               (whenf*n (fn-and symbol? qualified? auto-genned?) unqualify))
             eval)))))

; EXAMPLES OF META APPLICATION
; #+clj
; (defmacro extend-coll-search-for-type
;   [type-key]
;   (let [type# (-> coll-search-types (get type-key) name-from-class)
;         coll (with-meta (gensym)
;                {:tag type#})
;         elem (with-meta (gensym)
;                {:tag (if (= type# 'java.lang.String)
;                          'String
;                          'Object)})]
;    `(extend-protocol CollSearch (get ~coll-search-types ~type-key)
;       (index-of+      [~coll ~elem] (.indexOf     ~coll ~elem))
;       (last-index-of+ [~coll ~elem] (.lastIndexOf ~coll ~elem)))))

; #+clj
; (defn extend-coll-search-to-all-types! []
;   (reduce-kv
;     (fn [ret type type-class]
;       (eval `(extend-coll-search-for-type ~type)))
;     nil coll-search-types))