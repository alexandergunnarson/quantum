(ns
  ^{:doc "Some useful macros, like de-repetitivizing protocol extensions.
          Also some plumbing macros for |for| loops and the like."
    :attribution "Alex Gunnarson"}
  quantum.core.macros
  (:require-quantum [ns log pr err map set logic fn])
  (:require
    [clojure.string :as str]
    [clojure.walk :refer [postwalk]]))

(defn listy? [obj] (seq? obj)
  #_(->> obj class
       (contains? (core/get quantum.core.type/types 'listy?)))
  )

(def new-scope?
  (fn-and listy?
    (fn-> first symbol?)
    (fn-> first name (= "let"))))

(defn shadows-var? [bindings v]
  (->> bindings (apply hash-map)
       keys
       (map name)
       (into #{})
       (<- contains? (name v))))

(defn symbol-eq? [s1 s2] (= (name s1) (name s2)))

(defn metaclass [sym] (-> sym meta :tag))

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
       (if (and (listy? obj#)
                (-> obj# count   (= 2))
                (-> obj# (nth 0) (= 'clojure.core/unquote)))
           (if (-> obj# (nth 1) (in? ~sym-map))
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
 `(let [sym-map# (ns/local-context)]
    (unquote-replacement sym-map# '~form))))


(def extern? (fn-and listy? (fn-> first symbol?) (fn-> first name (= "extern"))))

#?(:clj
(defn extern* [ns- [spec-sym quoted-obj & extra-args]]
  #_(log/pr :macro-expand "EXTERNING" quoted-obj)
  (when-not (empty? extra-args)
    (throw (Exception. (str "|extern| takes only one argument. Received: " (-> extra-args count inc)))))
  (let [genned (gensym 'externed)
        obj-evaled
          (try (eval quoted-obj)
            (catch Throwable _#
              (throw (Exception. (str "Can't extern object because of closure: " quoted-obj)))))]
    (if (symbol? quoted-obj)
        quoted-obj
        (do (intern ns- (unqualify genned) obj-evaled)
            (log/pr :macro-expand quoted-obj "EXTERNED AS" (unqualify genned))
            (unqualify genned))))))

#?(:clj
(defmacro extern-
  "Dashed so as to encourage only internal use within macros."
  [obj]
  `(extern* *ns* ['extern ~obj])))

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


#?(:clj
(defmacro defn+ [sym & body]
  `(defn+* ~sym nil nil nil nil ~body)))

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







