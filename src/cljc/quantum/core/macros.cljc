(ns
  ^{:doc "Some useful macros, like de-repetitivizing protocol extensions.
          Also some plumbing macros for |for| loops and the like."
    :attribution "Alex Gunnarson"}
  quantum.core.macros
  (:refer-clojure :exclude [name])
  (:require-quantum [ns log pr err map set vec logic fn ftree])
  (:require
    [quantum.core.type.core     :as tcore]
    [fast-zip.core              :as zip  ]
  #?(:clj
    [backtick :refer [syntax-quote]])
    [clojure.string             :as str  ]
    [clojure.math.combinatorics :as combo]
    [clojure.walk :refer [postwalk prewalk]]))

(defn name [x] (if (nil? x) "" (core/name x)))

(defn default-zipper [coll]
  (zip/zipper coll? seq (fn [_ c] c) coll))

(def new-scope?
  (fn-and seq?
    (fn-> first symbol?)
    (fn-> first name (= "let"))))

(defn shadows-var? [bindings v]
  (->> bindings (apply hash-map)
       keys
       (map name)
       (into #{})
       (<- contains? (name v))))

(defn symbol-eq? [s1 s2] (= (name s1) (name s2)))

(defn metaclass [sym] (whenf (-> sym meta :tag) (fn-> name empty?) (constantly nil)))

(defn qualified?   [sym] (-> sym str (.indexOf "/") (not= -1)))
(defn unqualify    [sym] (-> sym name symbol))
(defn auto-genned? [sym] (-> sym name (.endsWith "__auto__")))

(def first-variadic?   (fn-> first name (= "&")))
(def variadic-arglist? (fn-> butlast last name (= "&")))

(defn arity-type [arglist]
  (if (variadic-arglist? arglist)
      :variadic
      :fixed))

(def arglist-arity
  (if*n
    variadic-arglist?
    (fn-> count dec)
    count))

; ===== TYPE PREDICATES =====

(def possible-type-predicate?
  (fn-and symbol? (fn-> name (.contains "?"))) )

(defn classes-for-type-predicate [pred lang]
  (throw-unless (symbol? pred) "Type predicate must be a symbol.")
  (if (possible-type-predicate? pred)
      (->> tcore/types-unevaled 
           (<- get lang)
           (<- get pred)
           (assertf->> nempty? "No classes match the predicate provided.")
           (into []))))

(defn expand-classes-for-type-hint [x lang]
  (condf x
    symbol? (if*n possible-type-predicate?
                  (fn-> hash-set (expand-classes-for-type-hint lang))
                  hash-set)
    set?    (fn->> (map (f*n classes-for-type-predicate lang))
                   (apply concat)
                   (into #{}))
    (constantly (throw+ (Err. nil "Not a type hint." x)))))

; ===== STRING OPERATIONS =====

(defn camelcase
  "In the macro namespace because it is used with protocol creation."
  ^{:attribution  "flatland.useful.string"
    :contributors "Alex Gunnarson"}
  [str-0 & [method?]]
  (-> str-0
      (str/replace #"[-_](\w)"
        (compr second str/upper-case))
      (#(if (not method?)
           (apply str (-> % first str/upper-case) (rest %))
           %))))

(defn ns-qualify [sym ns-]
  (symbol (str (-> ns- ns-name name) "." (name sym))))

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
  {:in '[(let [a 1] (quote+ (for [b 2] (inc ~a))))]
   :out '(for [a 1] (inc 1))}
  [form]
 `(let [sym-map# (ns/context)]
    (unquote-replacement sym-map# '~form))))


(def extern? (fn-and seq? (fn-> first symbol?) (fn-> first name (= "extern"))))

#?(:clj
(defn extern* [ns- [spec-sym quoted-obj & extra-args]]
  (if @ns/externs?
      (do (log/pr :macro-expand "EXTERNING" quoted-obj)
          (when-not (empty? extra-args)
            (throw (Exception. (str "|extern| takes only one argument. Received: " (-> extra-args count inc)))))
          (let [genned (gensym 'externed)
                obj-evaled
                  (try (eval quoted-obj) ; Possibly extern breaks because no runtime eval? 
                    (catch Throwable e#
                      (throw (Exception. (str "Can't extern object " quoted-obj
                                              " because of error: |" e# "|")))))]
            (if (symbol? quoted-obj)
                quoted-obj
                (do (intern ns- (unqualify genned) obj-evaled)
                    (log/pr :macro-expand quoted-obj "EXTERNED AS" (unqualify genned))
                    (unqualify genned)))))
      quoted-obj)))

#?(:clj
(defmacro extern-
  "Dashed so as to encourage only internal use within macros."
  [obj]
  `(do (extern* *ns* ['extern ~obj]))))

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
(defmacro defn+*
  ([sym doc- meta- arglist body [unk & rest-unk]]
    (if unk
        (cond
          (string? unk)
            `(defn+* ~sym ~unk  ~meta- ~arglist ~body                 ~rest-unk)
          (map?    unk)     
            `(defn+* ~sym ~doc- ~unk   ~arglist ~body                 ~rest-unk)
          (vector? unk)
            `(defn+* ~sym ~doc- ~meta- ~unk     ~rest-unk             nil      )
          (list?   unk)
            `(defn+* ~sym ~doc- ~meta- nil      ~(cons unk rest-unk) nil      )
          :else
            `(throw+ (str "Invalid arguments to |defn+|. " ~unk)))
        (let [_        (log/ppr :macro-expand "ORIG BODY:" body)
              pre-args (->> (list doc- meta-) (remove nil?))
              sym-f    (->> `(list ~sym) second)   
              externs  (atom [])
              body-f   (if arglist (list (cons arglist body)) body)
              body-f   (->> body-f
                            (clojure.walk/postwalk
                              (whenf*n extern?
                                (fn [[extern-sym obj]]
                                  (let [sym (gensym "externed")]
                                    (swap! externs conj (list 'def sym obj))
                                    sym)))))
              _        (log/ppr :macro-expand "OPTIMIZED BODY:" body-f)
              args-f
                (concat pre-args
                  (for [[arglist-n & body-n] body-f]
                    (list arglist-n
                      (list 'let
                        [`pre#  (list 'log/pr :trace (str "IN "             sym-f))
                         'ret_genned123  (cons 'do body-n)
                         `post# (list 'log/pr :trace (str "RETURNING FROM " sym-f))]
                        'ret_genned123))))
              _ (log/ppr :macro-expand "FINAL ARGS TO |defn|:" args-f)]
           `(do ~@(deref externs)
                (defn ~sym ~@args-f)) ; avoids eval-list stuff
           )))))


#?(:clj
(defmacro defn+ [sym & body]
  `(defn+* ~sym nil nil nil nil ~body)))

(defn protocol-extension-arities [arities class-sym-n]
  (->> (list arities)
       (map (fn [[arglist-n & body-n]]
              (log/ppr :macro-expand "[arglist-n & body-n]" [arglist-n body-n])
              (let [; apparently arglist-hinting isn't enough... 
                    first-variadic-n? (first-variadic? arglist-n)
                    arg-hinted
                      (-> arglist-n first
                          (with-meta 
                            {:tag (str class-sym-n)}))
                    _ (log/ppr :macro-expand "arg-hinted-meta" (meta arg-hinted))
                    arglist-hinted
                      (whenf arglist-n (fn-not first-variadic?)
                        (f*n assoc 0 arg-hinted))
                    body-n
                      (if first-variadic-n?
                          body-n
                          (->> body-n
                               (postwalk
                                 (condf*n
                                   (fn-and core/symbol? (f*n symbol-eq? arg-hinted))
                                     (fn [sym] (log/ppr :macro-expand "SYMBOL FOUND; REPLACING" sym) arg-hinted) ; replace it
                                   new-scope? identity
                                     #_(whenf*n (fn-> second (shadows-var? arg-hinted))
                                       #(throw (Exception. (str "Arglist shadows hinted arg |" arg-hinted "|."
                                                                " This is not supported:" %))))
                                   :else identity))))]
                     (log/ppr :macro-expand "arglist-hinted" arglist-hinted)
                (cons arglist-hinted body-n))))
       doall))

#?(:clj 
(defmacro extend-protocol-for-all
  {:usage
    '(extend-protocol-for-xrall
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
                                     (let [arities-hinted (protocol-extension-arities arities class-n)]
                                       (log/ppr :macro-expand "arities-hinted"
                                         (cons f-sym arities-hinted))
                                       (cons f-sym arities-hinted))))
                              doall) 
                        extension-n
                          (apply list 'extend-protocol prot class-n fns-hinted)
                        _# (log/ppr :macro-expand "EXTENDING PROTOCOL:" extension-n)]
                    extension-n))]
          (recur (concat code-n extensions-n) rest-body (inc loop-ct)))))))

(defn defn-variant-organizer
  "Organizes the arguments for use for a |defn| variant.
   Things like sym, meta, doc, etc."
  [f lang ns- sym doc- meta- body [unk & rest-unk]] (println "DEFN VARIANT [f lang ns- sym doc- meta- body [unk & rest-unk]]" [f lang ns- sym doc- meta- body [unk rest-unk]] )
  (if unk
      (cond
        (core/string? unk)
          (f lang ns- sym unk  meta- body                rest-unk)
        (core/map?    unk)     
          (f lang ns- sym doc- unk   body                rest-unk)
        ((fn-or core/symbol? core/keyword? core/vector? seq?) unk)
          (f lang ns- sym doc- meta- (cons unk rest-unk) nil     )
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
      (f lang ns- sym doc- meta- body)))

(defn optimize-defn-variant-body! [body externs]
  (log/ppr :macro-expand "ORIG BODY:" body)
  (->> body
       (clojure.walk/postwalk
         (whenf*n extern?
           (fn [[extern-sym obj]]
             (let [sym (gensym "externed")]
               (swap! externs conj (list 'def sym obj))
               sym))))
       (fn/doto->> log/ppr :macro-expand "OPTIMIZED BODY:")))

(defn extract-type-hints-from-arglist [arglist]
  (loop [z (-> arglist default-zipper zip/down)
       type-hints []]
    (if (nil? z)
        (->> type-hints (remove nil?) (into []))
        (let [type-hint-n
               (cond
                 (-> z zip/node symbol?)
                   (when-not ((fn-and nnil? (fn-> zip/node set?))
                              (-> z zip/left))
                     (whenc (-> z zip/node meta :tag) nil?
                       'Object))
                 (-> z zip/node set?)
                   (zip/node z))]
          (recur (zip/right z) (conj type-hints type-hint-n))))))

#?(:clj
(defn defntp*-helper
  ([lang ns- sym doc- meta- body [unk & rest-unk]]
    (apply defn-variant-organizer 
      [defntp*-helper lang ns- sym doc- meta- body (cons unk rest-unk)]))
  ([lang ns- sym doc- meta- body]
    (let [_ (log/ppr :macro-expand "ORIG BODY:" body)
          externs (atom [])
          body-f  (optimize-defn-variant-body! body externs)
          _ (log/ppr :macro-expand "OPTIMIZED BODY:" body-f)
          genned-protocol-name 
            (-> sym name camelcase (str "Protocol") symbol)
          ; {string? (([s] s)), int? (([i] i))}
          arities
            (->> `~body-f
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
                               (if (vector? pred)
                                   pred
                                   (classes-for-type-predicate pred lang))]
                          (assert (nempty? classes-for-pred) (str "No classes found for predicate |" pred "|"))
                          (map-entry
                            classes-for-pred
                            (cons sym arities-n)))))
                 (apply concat))
          protocol-extension 
            (apply list `extend-protocol-for-all genned-protocol-name protocol-body)
          _ (log/ppr :macro-expand "PROTOCOL EXTENSION:" protocol-extension)
          final-protocol-def
            (concat (apply list 'do @externs)
              [protocol-def protocol-extension])]
      {:prot  final-protocol-def
       :sym-f (with-meta sym (merge {:doc doc-} meta-))}))))

#?(:clj
(defmacro defntp* [lang sym & body]
  (let [{:keys [sym-f prot]}
         (defntp*-helper lang *ns* (second `(list ~sym)) nil nil nil (second `(list ~body)))
        meta-f (meta sym-f)
        code `(do ~prot
                (doto (var ~sym)
                  (alter-meta! merge ~meta-f)))]
        (log/ppr :macro-expand "DEFNTP CODE" code)
    code)))

#?(:clj 
(defmacro defntp [& args] `(defntp* :clj ~@args))) ; defn, typed, using protocols

; DEFNT: |DEFN| WITH |GEN-INTERFACE| AND |REIFY|

(defn hint-arglist-with [arglist hints]
  (loop [n 0 
         arglist-n arglist
         arglist-f []]
    (if (empty? arglist-n)
        arglist-f
        (let [hint-n (get hints n)
              arg-hinted (with-meta (first arglist-n) {:tag hint-n})]
          (recur (inc n)
                 (rest arglist-n)
                 (conj arglist-f arg-hinted))))))

(defmacro apply-dot [method target args] `(. ~target ~method ~@args))

(defn defnt*-helper
  ([lang ns- sym doc- meta- body [unk & rest-unk]] (println "DEFNT ORGANIZER")
    (apply defn-variant-organizer
      [defnt*-helper lang ns- sym doc- meta- body (cons unk rest-unk)]))
  ([lang ns- sym doc- meta- body]
    (let [externs (atom [])
          body-f  (optimize-defn-variant-body! body externs)
          remove-hints
            (fn->> (into [])
                   (<- update 0 (fn->> (filter symbol?) (into []))))
          arities
            (condf body
              (fn-> first vector?) (fn->> remove-hints vector)
              (fn-> first seq?   ) (fn->> (mapv remove-hints))
              (fn [form] (throw+ (Err. nil "Unexpected form when trying to parse arities." form))))
          _ (log/ppr :macro-expand "ARITIES:" arities)
          arglists
            (condf body
              (fn-> first vector?) (fn->> first vector)
              (fn-> first seq?   ) (fn->> (mapv first))
              (fn [form] (throw+ (Err. nil "Unexpected form when trying to parse arglists." form))))
          _ (log/ppr :macro-expand "ARGLISTS:" arglists)
          genned-method-name
            (-> sym name camelcase symbol)
          genned-interface-name 
            (-> sym name camelcase (str "Interface") symbol)
          ns-qualified-interface-name
            (ns-qualify genned-interface-name *ns*)
          gen-interface-code-header
            (dlist 'gen-interface :name ns-qualified-interface-name :methods)
          extract-all-type-hints-from-arglist
            (fn [a]
              (let [return-type (or (-> a meta :tag) 'Object)]
                (->> a
                     extract-type-hints-from-arglist
                     (<- vector return-type))))
           ; [[[fStarry [int  String    ] int   ]]
          ;   [[fStarry [long #{vector?}] float ]]]
          gen-interface-code-body-unexpanded
            (->> arglists
                 (map  extract-all-type-hints-from-arglist) ; [[int String] int]
                 (map  (f*n update 0
                         (partial mapv (f*n expand-classes-for-type-hint :clj))))
                 (map  (f*n vec/conjl genned-method-name))
                 (<- zipmap arities))
          ; [[FStarry [#{String} #{vector?}] long]] =>
          ; [[FStarry [String IPersistentVector] long]
          ;  [FStarry [String ITransientVector] long]]
          ; ...
          ; TODO simplify
          gen-interface-code-body-expanded
            (->> gen-interface-code-body-unexpanded
                 (mapv (fn [[[method-name hints ret-type] [arglist & body :as arity]]]
                         (let [expanded-hints-list (apply combo/cartesian-product hints)
                               assoc-arity-etc
                                 (fn [hints]
                                   (let [hints-v (into [] hints)
                                         arglist-hinted (hint-arglist-with arglist hints-v)
                                         arity-hinted (assoc arity 0 arglist-hinted)]
                                     [[method-name hints-v ret-type] (into (dlist) arity-hinted)]))]
                           (->> expanded-hints-list
                                (mapv assoc-arity-etc)))))
                 (apply catvec))
          gen-interface-def
            (conj gen-interface-code-header
              (mapv first gen-interface-code-body-expanded))
          _ (log/ppr :macro-expand "INTERFACE DEF:" gen-interface-def)
          reify-body
            (apply list 'reify ns-qualified-interface-name
              (->> gen-interface-code-body-expanded
                   (map (fn [[hints body]]
                          (let [return-type-hinted-method
                                 (with-meta genned-method-name {:tag (last hints)})
                                arglist-n    (->  body first (vec/conjl 'this))
                                updated-body (->> body rest  (cons arglist-n))]
                            (cons return-type-hinted-method updated-body))))))
          _ (log/ppr :macro-expand "REIFY BODY" reify-body)
          reified-sym (-> sym name
                          (str "-reified")
                          gensym)
          reified-sym-qualified
            (-> (symbol (name (ns-name *ns*)) (name reified-sym))
                (with-meta {:tag ns-qualified-interface-name}))
          sym-with-meta (with-meta sym (merge {:doc doc-} meta-))
          reify-def
            (list 'def reified-sym reify-body)
          helper-macro-def
            (quote+
              (defmacro ~sym-with-meta [& args]
                (seq (concat (list '.)
                             (list '~reified-sym-qualified)
                             (list '~genned-method-name)
                             args))))
          _ (log/ppr :macro-expand "HELPER MACRO DEF" helper-macro-def)
          final-defnt-def
            (concat (apply list 'do @externs)
              [gen-interface-def reify-def helper-macro-def])]
      final-defnt-def)))

(clojure.pprint/with-pprint-dispatch clojure.pprint/simple-dispatch
  (->> (defnt*-helper :clj *ns* 'myfunc nil nil nil '((^int    [^int    x ^String y] (int   123  ))
       (^float  [^long   x ^String y] (float 123.5))
       (^long   [^String x #{vector?} y #{map?} z] (long  4321 ))
       (^String [^String x ^int    y] "MYSTRING")
       (^Object [        x         y] (Err. nil "Really?" nil))))
       clojure.pprint/pprint))

#?(:clj
(defmacro defnt [sym & body]
  (let [code (defnt*-helper :clj *ns* sym nil nil nil (second `(list ~body)))]
    code)))

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
  (let-alias* (apply hash-map bindings) body)))


(. clojure.pprint/simple-dispatch addMethod Symbol pprint-symbol)
(clojure.pprint/with-pprint-dispatch clojure.pprint/simple-dispatch  ;;Make the dispatch to your print function
  (clojure.pprint/pprint (with-meta 'abc {:tag 'Object})))

