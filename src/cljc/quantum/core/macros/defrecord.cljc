(ns quantum.core.macros.defrecord
  (:require
    #?(:clj [cljs.core])
    [#?(:clj  clojure.core
        :cljs cljs.core   ) :as core]
    [quantum.core.macros.core
      :refer [if-cljs]]
    [quantum.core.log
      :refer [prl]])
  #?(:cljs
  (:require-macros
    [quantum.core.macros.defrecord :as self])))

(def ns-correct
  (memoize
    (fn [sym]
      (with-meta (-> sym str
                     (clojure.string/replace "/" "__SLASH__")
                     (clojure.string/replace "." "__DOT__")
                     symbol)
                 (meta sym)))))

#?(:clj
(defn- emit-defrecord+:clj
  "Like `emit-defrecord`, but handles namespaced keys as per CLJ-1938."
  [tagname cname fields interfaces methods opts]
  (let [classname (with-meta (symbol (str (namespace-munge *ns*) "." cname)) (meta cname))
        interfaces (vec interfaces)
        interface-set (set (map resolve interfaces))
        methodname-set (set (map first methods))
        hinted-fields fields
        hinted-fields* (mapv ns-correct hinted-fields)
        fields (vec (map #(with-meta % nil) fields))
        base-fields fields
        fields (conj fields '__meta '__extmap)
        fields* (conj (mapv ns-correct base-fields) '__meta '__extmap)
        type-hash (hash classname)]
    (when (some #{:volatile-mutable :unsynchronized-mutable} (mapcat (comp keys meta) hinted-fields))
      (throw (IllegalArgumentException. ":volatile-mutable or :unsynchronized-mutable not supported for record fields")))
    (let [gs (gensym)]
    (letfn
     [(irecord [[i m]]
        [(conj i 'clojure.lang.IRecord)
         m])
      (eqhash [[i m]]
        [(conj i 'clojure.lang.IHashEq)
         (conj m
               `(hasheq [this#] (int (bit-xor ~type-hash (clojure.lang.APersistentMap/mapHasheq this#))))
               `(hashCode [this#] (clojure.lang.APersistentMap/mapHash this#))
               `(equals [this# ~gs] (clojure.lang.APersistentMap/mapEquals this# ~gs)))])
      (iobj [[i m]]
            [(conj i 'clojure.lang.IObj)
             (conj m `(meta [this#] ~'__meta)
                   `(withMeta [this# ~gs] (new ~tagname ~@(replace {'__meta gs} fields*))))])
      (ilookup [[i m]]
         [(conj i 'clojure.lang.ILookup 'clojure.lang.IKeywordLookup)
          (conj m `(valAt [this# k#] (.valAt this# k# nil))
                `(valAt [this# k# else#]
                   (case k# ~@(mapcat (fn [fld] [(keyword fld) (ns-correct fld)])
                                       base-fields)
                         (get ~'__extmap k# else#)))
                `(getLookupThunk [this# k#]
                   (let [~'gclass (class this#)]
                     (case k#
                           ~@(let [hinted-target (with-meta 'gtarget {:tag tagname})]
                               (mapcat
                                (fn [fld]
                                  [(keyword fld)
                                   `(reify clojure.lang.ILookupThunk
                                           (get [~'thunk ~'gtarget]
                                                (if (identical? (class ~'gtarget) ~'gclass)
                                                  (. ~hinted-target ~(symbol (str "-" (ns-correct fld))))
                                                  ~'thunk)))])
                                base-fields))
                           nil))))])
      (imap [[i m]]
            [(conj i 'clojure.lang.IPersistentMap)
             (conj m
                   `(count [this#] (+ ~(count base-fields) (count ~'__extmap)))
                   `(empty [this#] (throw (UnsupportedOperationException. (str "Can't create empty: " ~(str classname)))))
                   `(cons [this# e#] (#'core/imap-cons this# e#))
                   `(equiv [this# ~gs]
                        (boolean
                         (or (identical? this# ~gs)
                             (when (identical? (class this#) (class ~gs))
                               (let [~gs ~(with-meta gs {:tag tagname})]
                                 (and  ~@(map (fn [fld] `(= ~(ns-correct fld) (. ~gs ~(symbol (str "-" (ns-correct fld)))))) base-fields)
                                       (= ~'__extmap (. ~gs ~'__extmap))))))))
                   `(containsKey [this# k#] (not (identical? this# (.valAt this# k# this#))))
                   `(entryAt [this# k#] (let [v# (.valAt this# k# this#)]
                                            (when-not (identical? this# v#)
                                              (clojure.lang.MapEntry/create k# v#))))
                   `(seq [this#] (seq (concat [~@(map #(list `clojure.lang.MapEntry/create (keyword %) (ns-correct %)) base-fields)]
                                              ~'__extmap)))
                   `(iterator [~gs]
                        (clojure.lang.RecordIterator. ~gs [~@(map keyword base-fields)] (clojure.lang.RT/iter ~'__extmap)))
                   `(assoc [this# k# ~gs]
                     (case k# ; was condp identical?
                       ~@(mapcat (fn [fld]
                                   [(keyword fld) (list* `new tagname (replace {(ns-correct fld) gs} fields*))])
                                 base-fields)
                       (new ~tagname ~@(remove '#{__extmap} fields*) (assoc ~'__extmap k# ~gs))))
                   `(without [this# k#] (if (contains? #{~@(map keyword base-fields)} k#)
                                            (dissoc (with-meta (into {} this#) ~'__meta) k#)
                                            (new ~tagname ~@(remove '#{__extmap} fields*)
                                                 (not-empty (dissoc ~'__extmap k#))))))])
      (ijavamap [[i m]]
                [(conj i 'java.util.Map 'java.io.Serializable)
                 (conj m
                       `(size [this#] (.count this#))
                       `(isEmpty [this#] (= 0 (.count this#)))
                       `(containsValue [this# v#] (boolean (some #{v#} (vals this#))))
                       `(get [this# k#] (.valAt this# k#))
                       `(put [this# k# v#] (throw (UnsupportedOperationException.)))
                       `(remove [this# k#] (throw (UnsupportedOperationException.)))
                       `(putAll [this# m#] (throw (UnsupportedOperationException.)))
                       `(clear [this#] (throw (UnsupportedOperationException.)))
                       `(keySet [this#] (set (keys this#)))
                       `(values [this#] (vals this#))
                       `(entrySet [this#] (set this#)))])
      ]
     (let [[i m] (-> [interfaces methods] irecord eqhash iobj ilookup imap ijavamap)]
       `(deftype* ~(symbol (name (ns-name *ns*)) (name tagname)) ~classname
          ~(conj hinted-fields* '__meta '__extmap)
          :implements ~(vec i)
          ~@(mapcat identity opts)
          ~@m)))))))

#?(:clj
(defmacro defrecord+:clj
  "Like `defrecord`, but handles namespaced keys as per CLJ-1938."
  {:arglists '([name [& fields] & opts+specs])}
  [name fields & opts+specs]
  (@#'core/validate-fields fields name)
  (let [gname name
        [interfaces methods opts] (@#'core/parse-opts+specs opts+specs)
        ns-part (namespace-munge *ns*)
        classname (symbol (str ns-part "." gname))
        hinted-fields fields
        fields (vec (map #(with-meta % nil) fields))]
    `(let []
       (declare ~(symbol (str  '-> gname)))
       (declare ~(symbol (str 'map-> gname)))
       ~(emit-defrecord+:clj name gname (vec hinted-fields) (vec interfaces) methods opts)
       (import ~classname)
       ~(@#'core/build-positional-factory gname classname (mapv ns-correct fields))
       (defn ~(symbol (str 'map-> gname))
         ~(str "Factory function for class " classname ", taking a map of keywords to field values.")
         ([m#]
          (reduce-kv
            (fn [ret# k# v#]
              (assoc ret# k# v#))
            (~(symbol (str classname "/create")) {})
            (if (instance? clojure.lang.MapEquivalence m#) m# (into {} m#)))))
       ~classname))))

; ===== CLJS ===== ;

#?(:clj
(defn- emit-defrecord+:cljs
  "Like `emit-defrecord`, but handles namespaced keys."
  [env tagname rname fields impls]
  (let [hinted-fields fields
        hinted-fields* (mapv ns-correct hinted-fields)
        fields (vec (map #(with-meta % nil) fields))
        base-fields fields
        pr-open (core/str "#" (namespace rname)
                          "." (name      rname)
                          "{")
        fields (conj fields '__meta '__extmap (with-meta '__hash {:mutable true}))
        fields* (conj (mapv ns-correct base-fields) '__meta '__extmap (with-meta '__hash {:mutable true}))]
    (let [gs (gensym)
          ksym (gensym "k")
          impls (concat
                  impls
                  ['IRecord
                   'ICloneable
                   `(~'-clone [this#] (new ~tagname ~@fields*))
                   'IHash
                   `(~'-hash [this#] (~'caching-hash this# ~'hash-imap ~'__hash))
                   'IEquiv
                   `(~'-equiv [this# other#]
                      (if (and other#
                            (identical? (.-constructor this#)
                              (.-constructor other#))
                            (~'equiv-map this# other#))
                        true
                        false))
                   'IMeta
                   `(~'-meta [this#] ~'__meta)
                   'IWithMeta
                   `(~'-with-meta [this# ~gs] (new ~tagname ~@(replace {'__meta gs} fields*)))
                   'ILookup
                   `(~'cljs.core/-lookup [this# k#] (~'-lookup this# k# nil))
                   `(~'cljs.core/-lookup [this# ~ksym else#]
                      (case ~ksym
                        ~@(mapcat (core/fn [f] [(keyword f) (ns-correct f)]) base-fields)
                        (cljs.core/get ~'__extmap ~ksym else#)))
                   'ICounted
                   `(~'-count [this#] (+ ~(count base-fields) (count ~'__extmap)))
                   'ICollection
                   `(~'-conj [this# entry#]
                      (if (vector? entry#)
                        (~'-assoc this# (~'-nth entry# 0) (~'-nth entry# 1))
                        (reduce ~'-conj
                          this#
                          entry#)))
                   'IAssociative
                   `(~'-assoc [this# k# ~gs]
                      (condp ~'keyword-identical? k#
                        ~@(mapcat (core/fn [fld]
                                    [(keyword fld) (list* `new tagname (replace {(ns-correct fld) gs '__hash nil} fields*))])
                            base-fields)
                        (new ~tagname ~@(remove #{'__extmap '__hash} fields*) (assoc ~'__extmap k# ~gs) nil)))
                   'IMap
                   `(cljs.core/-dissoc [this# k#] (if (contains? #{~@(map keyword base-fields)} k#)
                                            (dissoc (with-meta (into {} this#) ~'__meta) k#)
                                            (new ~tagname ~@(remove #{'__extmap '__hash} fields*)
                                              (not-empty (dissoc ~'__extmap k#))
                                              nil)))
                   'ISeqable
                   `(cljs.core/-seq [this#] (seq (concat [~@(map #(core/list `vector (keyword %) (ns-correct %)) base-fields)]
                                           ~'__extmap)))

                   'IIterable
                   `(~'-iterator [~gs]
                     (RecordIter. 0 ~gs ~(count base-fields) [~@(map keyword base-fields)] (if ~'__extmap
                                                                                             (cljs.core/-iterator ~'__extmap)
                                                                                             (core/nil-iter))))

                   'IPrintWithWriter
                   `(~'-pr-writer [this# writer# opts#]
                      (let [pr-pair# (fn [keyval#] (cljs.core/pr-sequential-writer writer# ~'pr-writer "" " " "" opts# keyval#))]
                        (cljs.core/pr-sequential-writer
                          writer# pr-pair# ~pr-open ", " "}" opts#
                          (concat [~@(map #(core/list `vector (keyword %) (ns-correct %)) base-fields)]
                            ~'__extmap))))
                   ])
               [fpps pmasks] (#'cljs.core/prepare-protocol-masks env impls)
               protocols (#'cljs.core/collect-protocols impls env)
               tagname (vary-meta tagname assoc
                         :protocols protocols
                         :skip-protocol-flag fpps)]
      `(do
         (~'defrecord* ~tagname ~hinted-fields* ~pmasks
           (extend-type ~tagname ~@(@#'cljs.core/dt->et tagname impls fields* true))))))))

#?(:clj
(defmacro defrecord+:cljs
  "Like `defrecord`, but handles namespaced keys: ClojureScript version"
  [rsym fields & impls]
  (let [rsym (vary-meta rsym assoc :internal-ctor true)
        r    (vary-meta
               (:name (cljs.analyzer/resolve-var (dissoc &env :locals) rsym))
               assoc :internal-ctor true)
        corrected-fields (mapv ns-correct fields)
        code `(let []
                ~(emit-defrecord+:cljs &env rsym r fields impls)
                (set! (.-getBasis ~r) (fn [] '[~@fields]))
                (set! (.-cljs$lang$type ~r) true)
                (set! (.-cljs$lang$ctorPrSeq ~r) (fn [this#] (cljs.core/list ~(str r))))
                (set! (.-cljs$lang$ctorPrWriter ~r) (fn [this# writer#] (~'-write writer# ~(str r))))
                ~(@#'cljs.core/build-positional-factory rsym r corrected-fields)
                ~(@#'cljs.core/build-map-factory rsym r fields)
                ~r)]
    (prl ::debug code)
    code)))

#?(:clj
  (defmacro defrecord+
    "Like `defrecord`, but handles namespaced keys as per CLJ-1938."
    [& args]
    (if-cljs &env `(defrecord+:cljs ~@args) `(defrecord+:clj ~@args))))
