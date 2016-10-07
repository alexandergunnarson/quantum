(ns quantum.core.macros.defrecord
  (:require
    [#?(:clj  clojure.core
        :cljs cljs.core   ) :as core]))

(def ns-correct
  (memoize
    (fn [sym]
      (with-meta (-> sym str
                     (clojure.string/replace "/" "__SLASH__")
                     (clojure.string/replace "." "__DOT__")
                     symbol)
                 (meta sym)))))

#?(:clj
(defn- emit-defrecord+
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
(defmacro defrecord+
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
       ~(emit-defrecord+ name gname (vec hinted-fields) (vec interfaces) methods opts)
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
