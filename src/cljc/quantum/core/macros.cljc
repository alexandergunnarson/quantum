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

(def first-variadic? (fn-> first name (= "&")))
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
  (eval
    `(do #_(log/pr :macro-expand "EXTERNING" ~quoted-obj)
         (when-not (empty? ~extra-args)
           (throw (Exception. (str "|extern| takes only one argument. Received: " (-> ~extra-args count inc)))))
         (let [genned# (gensym 'externed)]
           (intern ~ns- (unqualify genned#) (eval ~quoted-obj))
           (log/pr :macro-expand "EXTERNED AS" (unqualify genned#))
           (unqualify genned#)))))
)
#?(:clj
(defmacro extern-
  "Dashed so as to encourage only internal use within macros."
  [obj]
  `(extern* *ns* ['extern ~obj])))

#?(:clj
(defmacro extern+
  "'Plus' because extern- has strange problems sometimes."
  [obj]
  `(let [sym# (gensym)] (intern *ns* sym# ~obj) sym#)))

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

(defn defn+*
  ([ns- sym doc- meta- arglist body [unk & rest-unk]]
    (if unk
        (cond
          (string? unk)
            (defn+* ns- sym unk  meta- arglist body     rest-unk)
          (map?    unk)     
            (defn+* ns- sym doc- unk   arglist body     rest-unk)
          (vector? unk)
            (defn+* ns- sym doc- meta- unk     rest-unk nil     )
          :else
            (throw+ {:msg "Invalid arguments to |defn+|." :cause unk}))
        (let [_ (log/ppr :macro-expand "ORIG BODY:" body)
              pre-args (->> (list doc- meta- arglist) (remove nil?))
              body-f
                (->> body
                     (clojure.walk/postwalk
                       (whenf*n extern? (fn->> (extern* ns-)))))
              _ (log/ppr :macro-expand "OPTIMIZED BODY:" body-f)
              args-f (concat pre-args body-f)]
           (eval (apply list 'defn sym args-f))
           ))))

#?(:clj
(defmacro defn+ [sym & body]
  `(defn+* *ns* '~sym nil nil nil nil '~body)))
