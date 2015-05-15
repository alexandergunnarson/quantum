#?(:clj (ns quantum.core.macros))

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
    #?(:clj [potemkin.types :as t]))
  #?(:cljs (:require-macros
    [quantum.core.type :refer [should-transientize?]]))
  #?(:clj (:gen-class)))

#?(:clj 
(defmacro extend-protocol-for-all [prot classes & body]
  `(doseq [class-n# ~classes]
     (extend-protocol ~prot (eval class-n#) ~@body))))

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