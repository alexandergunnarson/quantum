(ns
  ^{:doc "Some useful macros, like de-repetitivizing protocol extensions.
          Also some plumbing macros for |for| loops and the like."
    :attribution "Alex Gunnarson"}
  quantum.core.cljs.deps.macros
  (:refer-clojure :exclude [name macroexpand macroexpand-all])
  (:require-quantum [ns log pr err map set vec logic fn ftree cbase classes])
  (:require
    [quantum.core.collections.base :as cbase :refer
      [name default-zipper camelcase ns-qualify zip-reduce ensure-set update-first update-val]]
    [quantum.core.type.core                  :as tcore]
    [co.paralleluniverse.pulsar.core         :as pulsar]
    [quantum.core.analyze.clojure.predicates :as anap :refer 
      [#?(:clj type-hint) unqualify]]
  #_[backtick :refer [syntax-quote]]
    [clojure.string             :as str  ]
    [clojure.walk :refer [postwalk prewalk]]
    #?@(:clj [[riddley.walk]
              [clojure.math.combinatorics :as combo]])))

(defn hint-meta [sym hint] (with-meta sym {:tag hint}))

(def extern? (fn-and seq? (fn-> first symbol?) (fn-> first name (= "extern"))))

#?(:clj
(defmacro unquote-replacement
  "Replaces all duple-lists: (clojure.core/unquote ___) with the unquoted version of the inner content."
  [sym-map quoted-form]
  `(prewalk
     (fn [obj#]
       (if (and (seq? obj#)
                (-> obj# count   (= 2))
                (-> obj# (nth 0) (= 'clojure.core/unquote)))
           (if (->> obj# (<- nth 1) (contains? ~sym-map))
               (get ~sym-map (-> obj# (nth 1)))
               (throw+ {:msg ("Symbol does not evaluate to anything: " (-> obj# (nth 1)))}))
           obj#))
     ~quoted-form)))

#?(:clj
(defmacro quote+
  "Normal quoting with unquoting that works as in |syntax-quote|."
  {:in '[(let [a 1]
           (for [b 2] (inc ~a)))]
   :out '(for [a 1] (inc 1))}
  [form]
 `(let [sym-map# (ns/context)]
    (unquote-replacement sym-map# '~form))))

#?(:clj
(defmacro extern-
  "Dashed so as to encourage only internal use within macros."
  [obj]
  `(do (quantum.core.macros/extern* *ns* ['extern ~obj]))))

#?(:clj
(defmacro inline-replace
  "TODO IMPLEMENT
   Can use it like so:
   (quantum.core.macros/inline-replace (~f ret# elem# @i#)).
   Must be given a function definition. Will replace the arguments
   accordingly.
   Currently just yields identity."
  [obj] obj))

#?(:clj
(defmacro identity*
  "For use in macros where you don't want to have the extra fn call."
  [obj] obj))

#?(:clj
(defmacro fn+*
  ([sym doc- meta- arglist body [unk & rest-unk]]
    (if unk
        (cond
          (string? unk)
            `(fn+* ~sym ~unk  ~meta- ~arglist ~body                ~rest-unk)
          (map?    unk)     
            `(fn+* ~sym ~doc- ~unk   ~arglist ~body                ~rest-unk)
          (vector? unk)
            `(fn+* ~sym ~doc- ~meta- ~unk     ~rest-unk            nil      )
          (list?   unk)
            `(fn+* ~sym ~doc- ~meta- nil      ~(cons unk rest-unk) nil      )
          :else
            `(throw+ (str "Invalid arguments to |fn+|. " ~unk)))
        (let [_        (log/ppr :macro-expand "ORIG BODY:" body)
              ret-type (->> sym meta :tag)
              suspendable?   (->> sym meta :suspendable)
              interruptible? (->> sym meta :interruptible)
              ret-type-quoted (list 'quote ret-type)
              ;pre-args (->> (list meta-) (remove nil?))
              meta-f   (assoc (or meta- {})
                               :doc doc-)
              meta-f   (if ret-type
                           (assoc meta-f :tag ret-type-quoted)
                           meta-f)
              sym-f    (-> sym
                           (with-meta meta-f))
              externs  (atom [])
              body-f   (if arglist (list (cons arglist body)) body)
              body-f   (->> body-f
                            (clojure.walk/postwalk
                              (whenf*n quantum.core.cljs.deps.macros/extern?
                                (fn [[extern-sym obj]]
                                  (let [sym (gensym "externed")]
                                    (log/pr :macro-expand "EXTERNED" sym "IN FN+")
                                    (swap! externs conj (list 'def sym obj))
                                    sym)))))
              _        (log/ppr :macro-expand "OPTIMIZED BODY:" body-f)
              args-f
                (for [[arglist-n & body-n] body-f]
                  (list arglist-n
                    (list 'let
                      [`pre#  (list 'log/pr :trace (str "IN "             sym-f))
                       'ret_genned123  (if interruptible?
                                           `(if (quantum.core.thread.async/interrupted?)
                                                (throw (InterruptedException.))
                                                (do ~@body-n))
                                           `(do ~@body-n))
                       `post# (list 'log/pr :trace (str "RETURNING FROM " sym-f))]
                      'ret_genned123)))
              _ (log/ppr :macro-expand "FINAL ARGS TO |defn|:" args-f)]
           `(do ~@(deref externs)
                (let [;~sym-f (with-meta '~sym-f
                      ;         (assoc (or ~meta- {}) :doc ~doc-))
                      meta-f# ~meta-f
                      f# (with-meta
                           (fn ~sym-f ~@args-f)
                           meta-f#)]
                 [(if (and ~suspendable? (System/getProperty "quantum.core.async:allow-suspendable?"))
                      (pulsar/suspendable! f#)
                      f#)
                  meta-f#])) ; May greatly increase the compilation time depending on how long the fn is.
                   ; avoids eval-list stuff
           )))))

#?(:clj
(defmacro fn+ [sym & body]
  `(first (quantum.core.cljs.deps.macros/fn+* ~sym nil nil nil nil ~body))))

#?(:clj
(defmacro defn+ [sym & body]
  `(let [[f# meta#] (quantum.core.cljs.deps.macros/fn+* ~sym nil nil nil nil ~body)
         var# (doto (def ~sym f#)
                (alter-meta! map/merge meta#))]
     var#)))

#?(:clj
(defn hint-body-with-arglist
  ([body arglist lang] (hint-body-with-arglist body arglist lang nil))
  ([body arglist lang body-type]
  (let [arglist-set (into #{} arglist)
        body-hinted 
          (postwalk 
            (condf*n
              symbol?
                (fn [sym]
                  (if-let [arg (get arglist-set sym)]
                    (if (tcore/primitive? (type-hint arg)) ; Because "Can't type hint a primitive local"
                        sym
                        arg)
                    sym)) ; replace it
              anap/new-scope?
                (fn [scope]
                  (if-let [sym (->> arglist-set (filter (partial anap/shadows-var? (second scope))) first)]
                    (throw+ (Err. :unsupported "Arglist in |let| shadows hinted arg." sym))
                    scope))
              :else identity)
            body)
        body-unboxed
          (if (= body-type :protocol)
              body-hinted ; TODO? add (let [x (long x)] ...) unboxing
              body-hinted)]
    body-unboxed))))

(defn protocol-extension-arities [arities class-sym-n lang]
  (->> (list arities)
       (map (fn [[arglist-n & body-n]]
              (log/ppr :macro-expand "[arglist-n & body-n]" [arglist-n body-n])
              (let [; apparently arglist-hinting isn't enough... 
                    first-variadic-n? (anap/first-variadic? arglist-n)
                    arg-hinted
                      (-> arglist-n first
                          (hint-meta
                            (str class-sym-n)))
                    _ (log/ppr :macro-expand "arg-hinted-meta" (meta arg-hinted))
                    arglist-hinted
                      (whenf arglist-n (fn-not anap/first-variadic?)
                        (f*n assoc 0 arg-hinted))
                    body-n
                      (if first-variadic-n?
                          body-n
                          #?(:clj  (hint-body-with-arglist body-n [arg-hinted] lang)
                             :cljs body-n))]
                     (log/ppr :macro-expand "arglist-hinted" arglist-hinted)
                (cons arglist-hinted body-n))))
       doall))

#?(:clj 
(defmacro extend-protocol-for-all
  {:usage
    '(extend-protocol-for-all
       AbcdeProtocol
       [java.util.List clojure.lang.PersistentList]
       (abcde ([a] (println a))))}
  [prot & body]
  (loop [code-n '(do) body-n body loop-ct 0]
    (log/ppr :macro-expand "BODY-N" body-n)
    (if (empty? body-n)
        code-n
        (let [classes   (first body-n)
              fns       (->> body-n rest (take-while (fn-not core/vector?)))
              rest-body (->> body-n rest (drop (count fns)))
              extensions-n
                (for [class-n classes]
                  (let [fns-hinted
                         (->> fns
                              (map (fn [[f-sym arities]]
                                     (log/ppr :macro-expand "f-sym|arities" f-sym arities)
                                     (let [arities-hinted (quantum.core.cljs.deps.macros/protocol-extension-arities arities class-n :clj)]
                                       (log/ppr :macro-expand "arities-hinted"
                                         (cons f-sym arities-hinted))
                                       (cons f-sym arities-hinted))))
                              doall) 
                        extension-n
                          (apply list 'extend-protocol prot class-n fns-hinted)
                        _# (log/ppr :macro-expand "EXTENDING PROTOCOL:" extension-n)]
                    extension-n))]
          (recur (concat code-n extensions-n) rest-body (inc loop-ct)))))))

(defn classes-for-type-predicate
  ([pred lang] (classes-for-type-predicate pred lang nil))
  ([pred lang type-arglist]
  (throw-unless ((fn-or symbol? keyword?) pred) "Type predicate must be a symbol or keyword.")
  (cond
    (and (symbol? pred) (anap/possible-type-predicate? pred))
      (->> tcore/types-unevaled 
           (<- get lang)
           (<- get pred)
           (assertf->> nempty? "No classes match the predicate provided.")
           (into []))
    :else [pred])))


(defn defn-variant-organizer
  "Organizes the arguments for use for a |defn| variant.
   Things like sym, meta, doc, etc."
  [f opts lang ns- sym doc- meta- body [unk & rest-unk]]
  (if unk
      (cond
        (core/string? unk)
          (f opts lang ns- sym unk  meta- body                rest-unk)
        (core/map?    unk)     
          (f opts lang ns- sym doc- unk   body                rest-unk)
        ((fn-or core/symbol? core/keyword? core/vector? seq?) unk)
          (f opts lang ns- sym doc- meta- (cons unk rest-unk) nil     )
        :else
          (throw+ {:msg (str "Invalid arguments to |" sym "|.")
                   :cause unk
                   :args {:ns-      ns-
                          :sym      sym
                          :doc-     doc-
                          :meta-    meta-
                          :body     body
                          :unk      unk
                          :rest-unk rest-unk}}))
      (f opts lang ns- sym doc- meta- body)))

(defn optimize-defn-variant-body! [body externs]
  (log/ppr :macro-expand "ORIG BODY:" body)
  (->> body
       (clojure.walk/postwalk
         (whenf*n extern?
           (fn [[extern-sym obj]]
             (let [sym (gensym "externed")]
               (swap! externs conj (list 'def sym obj))
               sym))))
       (doto->> log/ppr :macro-expand "OPTIMIZED BODY:")))

#?(:clj
(defn defntp*-helper
  ([opts lang ns- sym doc- meta- body [unk & rest-unk]]
    (apply defn-variant-organizer 
      [defntp*-helper nil lang ns- sym doc- meta- body (cons unk rest-unk)]))
  ([opts lang ns- sym doc- meta- body]
    (let [_ (log/ppr :macro-expand "ORIG BODY:" body)
          externs (atom [])
          body-f  (optimize-defn-variant-body! body externs)
          _ (log/ppr :macro-expand "OPTIMIZED BODY:" body-f)
          genned-protocol-name 
            (-> sym name camelcase (str "Protocol") symbol)
          ; {string? (([s] s)), int? (([i] i))}
          arities
            (->> body-f
                 (apply ordered-map)
                 (map
                   (f*n update-val
                     (whenf*n (fn-> first core/vector?) list)))
                 (apply map/merge (ordered-map)))
          _ (log/ppr :macro-expand "ARITIES:" arities)
          arglists
            (->> arities
                 vals
                 (map (fn->> (map first) (into #{})))
                 (apply set/union)
                 (map (fn [arglist]
                        [(count arglist) arglist]))
                 (map/merge {})
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
                               (if (vector? pred)
                                   pred
                                   (classes-for-type-predicate pred lang))]
                          (throw-unless (nempty? classes-for-pred) (str "No classes found for predicate |" pred "|"))
                          [classes-for-pred
                           (cons sym arities-n)])))
                 (apply concat))
          protocol-extension 
            (apply list `extend-protocol-for-all genned-protocol-name protocol-body)
          _ (log/ppr :macro-expand "PROTOCOL EXTENSION:" protocol-extension)
          final-protocol-def
            (concat (apply list 'do @externs)
              [protocol-def protocol-extension])]
      {:prot  final-protocol-def
       :sym-f (with-meta sym (map/merge {:doc doc-} meta-))}))))

#?(:clj
(defmacro defntp* [lang sym & body]
  (let [{:keys [sym-f prot]}
         (quantum.core.cljs.deps.macros/defntp*-helper nil lang *ns* sym nil nil nil body)
        meta-f (meta sym-f)
        code `(do ~prot
                (doto (var ~sym)
                  (alter-meta! map/merge ~meta-f)))]
        (log/ppr-hints :macro-expand "DEFNTP CODE" code)
    code)))

#?(:clj 
(defmacro defntp [& args] `(quantum.core.cljs.deps.macros/defntp* :clj ~@args)))

#?(:clj (defmacro maptemplate
  [template-fn coll]
  `(do ~@(map `~#((eval template-fn) %) coll))))

(defn let-alias* [bindings body]
  (cons 'do
    (postwalk
      (whenf*n (fn-and symbol? (partial contains? bindings))
        (partial get bindings))
      body)))

#?(:clj
(defmacro let-alias
  {:todo ["Deal with closures"]}
  [bindings & body]
  (quantum.core.cljs.deps.macros/let-alias* (apply hash-map bindings) body)))

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

#?(:clj
(defmacro variadic-proxy
  "Creates left-associative variadic forms for any operator."
  {:attribution "ztellman/primitive-math"}
  ([name fn]
     `(variadic-proxy ~name ~fn ~(str "A primitive macro version of `" name "`")))
  ([name fn doc]
     `(variadic-proxy ~name ~fn ~doc identity))
  ([name fn doc single-arg-form]
     (let [x-sym (gensym "x")]
       `(defmacro ~name
          ~doc
          ([~x-sym]
             ~((eval single-arg-form) x-sym))
          ([x# y#]
             (list '~fn x# y#))
          ([x# y# ~'& rest#]
             (list* '~name (list '~name x# y#) rest#)))))))

#?(:clj
(defmacro variadic-predicate-proxy
  "Turns variadic predicates into multiple pair-wise comparisons."
  {:attribution "ztellman/primitive-math"}
  ([name fn]
     `(variadic-predicate-proxy ~name ~fn ~(str "A primitive macro version of |" name "|")))
  ([name fn doc]
     `(variadic-predicate-proxy ~name ~fn ~doc (constantly true)))
  ([name fn doc single-arg-form]
     (let [x-sym (gensym "x")]
       `(defmacro ~name
          ~doc
          ([~x-sym]
             ~((eval single-arg-form) x-sym))
          ([x# y#]
             (list '~fn x# y#))
          ([x# y# ~'& rest#]
             (list 'quantum.core.Numeric/and (list '~name x# y#) (list* '~name y# rest#))))))))