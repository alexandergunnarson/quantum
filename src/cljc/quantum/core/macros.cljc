(ns
  ^{:doc "Some useful macros, like de-repetitivizing protocol extensions.
          Also some plumbing macros for |for| loops and the like."
    :attribution "Alex Gunnarson"}
  quantum.core.macros
  (:refer-clojure :exclude [name])
  (:require-quantum [ns log pr err map set logic fn])
  (:require
    [quantum.core.type.core :as tcore]
    [clojure.string         :as str  ]
    [clojure.walk :refer [postwalk prewalk]]))

(defn name [x] (if (nil? x) "" (core/name x)))

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
  #_(log/pr :macro-expand "EXTERNING" quoted-obj)
  (when-not (empty? extra-args)
    (throw (Exception. (str "|extern| takes only one argument. Received: " (-> extra-args count inc)))))
  (let [genned (gensym 'externed)
        obj-evaled
          (try (eval quoted-obj)
            (catch Throwable e#
              (throw (Exception. (str "Can't extern object " quoted-obj
                                      " because of error: |" e# "|")))))]
    (if (symbol? quoted-obj)
        quoted-obj
        (do (intern ns- (unqualify genned) obj-evaled)
            (log/pr :macro-expand quoted-obj "EXTERNED AS" (unqualify genned))
            (unqualify genned))))))

#?(:clj
(defmacro extern-
  "Dashed so as to encourage only internal use within macros."
  [obj]
  `(do (log/pr :macro-expand "\n/*" "EXTERNING" "*/\n")
       (extern* *ns* ['extern ~obj]))))

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
              body-f   (if arglist (list (cons arglist body)) body)
              body-f   (->> body-f
                            (clojure.walk/postwalk
                              (whenf*n extern? (fn->> second extern-))))
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
           `(defn ~sym ~@args-f) ; avoids eval-list stuff
           )))))


#?(:clj ; only Clojure, not ClojureScript yet
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

#?(:clj
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
         (doseq [type# (get quantum.core.type.core/types pred#)]
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
                                    {:tag (quantum.core.type.core/name-from-class type#)})))]
                       (apply list arglist-f# body-n#)))
                 fn-f# (apply list 'fn arities-n-f#)
                 _#    (log/ppr :macro-expand
                         (str "FINAL F FOR PRED " pred#
                              " MUNGED CLASSNAME " (quantum.core.type.core/name-from-class type#)) fn-f#)]
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
             eval))))))

#?(:clj
(defn defnt*-helper
  [lang ns- sym doc- meta- body [unk & rest-unk]]
  (if unk
      (cond
        (core/string? unk)
          (defnt*-helper lang ns- sym unk  meta- body                rest-unk)
        (core/map?    unk)     
          (defnt*-helper lang ns- sym doc- unk   body                rest-unk)
        ((fn-or core/symbol? core/keyword? core/vector?) unk)
          (defnt*-helper lang ns- sym doc- meta- (cons unk rest-unk) nil     )
        :else
          (throw+ {:msg "Invalid arguments to |defnt|."
                   :cause unk
                   :args {:ns-      ns-
                          :sym      sym
                          :doc-     doc-
                          :meta-    meta-
                          :body     body
                          :unk      unk
                          :rest-unk rest-unk}}))
      (let [_ (log/ppr :macro-expand "ORIG BODY:" body)
            body-f
              (->> body
                   (clojure.walk/postwalk
                     (whenf*n extern?
                       #(condp = lang
                          :clj  (extern* ns- %)
                          :cljs (second %)))))
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
                                     (->> tcore/types-unevaled 
                                          (<- get lang)
                                          (<- get pred)
                                          (into [])))]
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
(defmacro defnt* [lang sym & body]
  (let [{:keys [sym-f prot]}
         (defnt*-helper lang *ns* (second `(list ~sym)) nil nil nil (second `(list ~body)))
        meta-f (meta sym-f)
        code `(do ~prot
                (doto (var ~sym)
                  (alter-meta! merge ~meta-f)))]
        (log/ppr :macro-expand "DEFNT CODE" code)
    code)))

#?(:clj 
(defmacro defnt [& args] `(defnt* :clj ~@args)))


; For use defn+ / defnt

; (gen-interface
;   :name    quantum.core.reducers.ITesting
;   :methods [[fStarry [int]    int ]
;             [fStarry [String] long]])
; (.fStarry (reify quantum.core.reducers.ITesting
;             (^long fStarry [this ^String x] (long 123))) "A") -> 123
; (reified 45) -> AbstractMethodError quantum.core.reducers$eval70900$reify__70901.fStarry(I)I
; quantum.core.reducers=> (.fStarry (reify quantum.core.reducers.ITesting (^long fStarry [this ^String x] (long 1)) (^int fStarry [this ^int x] (int 3214))) 123) => 3214



; EVAL

; What eval does, is wrapping (fn* [] <expression>) around its
; arguments, compiling that, and calling the resulting function object
; (except if your list starts with a 'do or a 'def).
; While Clojure's compiler is pretty fast, you should try not to use
; eval. If you want to pass code around you should try something like
; storing functions in a map or something similar.

; If you think that you have to eval lists, try wrapping them in (fn
; [] ...) and eval that form instead. You'll get a function object which
; was compiled once, but can be called as many times as you want.

; eg.

; (defn form->fn [list-to-eval]
;   (eval (list 'fn [] list-to-eval))) ;this returns a fn

; (def form->fn (memoize form->fn)) ;caches the resulting fns, beware of
; memory leaks though

; ((form->fn '(println "Hello World")))
; ((form->fn '(println "Hello World")))
; ((form->fn '(println "Hello World"))) ; should compile only once

#?(:clj (defmacro maptemplate
  [template-fn coll]
  `(do ~@(map `~#((eval template-fn) %) coll))))


