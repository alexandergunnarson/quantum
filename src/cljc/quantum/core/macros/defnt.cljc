(ns quantum.core.macros.defnt
  (:refer-clojure :exclude [merge])
  (:require
    [clojure.core                            :as core]
    [quantum.core.analyze.clojure.transform
      :refer [unhint]]
    [quantum.core.cache
      :refer [defmemoized]]
    [quantum.core.core                       :as qcore
      :refer [name+]]
    [quantum.core.collections.base           :as cbase
      :refer [merge-call update-first update-val ensure-set
              reducei kw-map nempty? nnil?]]
    [quantum.core.data.map                   :as map
      :refer [merge]]
    [quantum.core.data.set                   :as set]
    [quantum.core.data.vector                :as vec
      :refer [catvec]]
    [quantum.core.error                      :as err
      :refer [->ex throw-unless assertf->>]]
    [quantum.core.fn                         :as fn
      :refer [<- fn-> fn->> fn1 fnl]]
    [quantum.core.log                        :as log
      :refer [prl]]
    [quantum.core.logic                      :as logic
      :refer [fn= fn-not fn-and fn-or whenc whenf whenf1 whenc1 ifn1 condf if-not-let cond-let]]
    [quantum.core.macros.core                :as cmacros
      :refer [case-env case-env* hint-meta]]
    [quantum.core.macros.fn                  :as mfn]
    [quantum.core.analyze.clojure.core       :as ana
      :refer [type-hint]]
    [quantum.core.analyze.clojure.predicates :as anap]
    [quantum.core.macros.protocol            :as proto]
    [quantum.core.macros.reify               :as reify]
    [quantum.core.macros.transform           :as trans]
    [quantum.core.numeric.combinatorics      :as combo]
    [quantum.core.print                      :as pr]
    [quantum.core.string.regex               :as re]
    [quantum.core.type.defs                  :as tdefs]
    [quantum.core.type.core                  :as tcore]
    [quantum.core.vars                       :as var
      :refer [defalias replace-meta-from]]
    [quantum.core.spec                       :as s
      :refer [validate]]))

(defonce warn-on-strict-inexact-matches? (atom false))
(defonce warn-on-all-inexact-matches? (atom false))

(def
  ^{:todo {0 "Totally reorganize/cleanup this namespace and move into other ones as necessary"
           1 {:desc     "Allow for ^:inline on certain `defnt` arities"
              :priority 0.8}
           2 {:desc     "Allow for use as higher-order function"
              :priority 0.8}
           3 "Document usage of `defnt`"
           4 {:desc     "Multiple dispatch.
                         As in Julia, 'When a function is applied to a particular tuple of args,
                         the most specific method applicable to those arguments is applied.'"
              :priority 0.7}}}
  annotations nil)

(def position-pattern (str "0|[1-9]" (re/nc "[0-9]*")))
(def depth-pattern    (str   "[1-9]" (re/nc "[0-9]*")))

(def defnt-positional-regex
  (let [position (re/c position-pattern)
        depth    (re/c depth-pattern)]
    (re-pattern (str "\\<" position "\\>"
                     (re/? (re/nc ":" depth))))))

(defn ?defnt-keyword->positional-profundal [x]
  (when (and (or (keyword? x) (symbol? x)) (-> x namespace nil?))
    (some->> x name (re-matches defnt-positional-regex) rest
             (mapv (whenf1 string? qcore/str->integer)))))

(defn defnt-keyword->positional-profundal [x]
  (or (?defnt-keyword->positional-profundal x)
      (when (keyword? x)
        (err/throw-info "Invalid `defnt` special keyword" {:k x}))))

(def qualified-class-name-map
  (->> (set/union tcore/primitive-types #?(:clj tcore/primitive-array-types))
       (repeat 2)
       (apply zipmap)))

(defn get-qualified-class-name ; TODO replace with `resolve` or some such thing
  [lang ns- class-sym]
  #?(:clj  (if (= lang :clj)
               (whenf class-sym (fn-not ?defnt-keyword->positional-profundal)
                 (whenc1 symbol?
                   (if-let [qualified-class-name (get qualified-class-name-map class-sym)]
                     qualified-class-name
                     (let [class-name (or (ns-resolve ns- class-sym)
                                          (resolve (symbol (str (ns-name ns-) "." class-sym))))
                           _ (assert (some? class-name) {:class class-sym :ns ns- :msg "Class not found for symbol in namespace"})]
                       (symbol (.getName ^Class class-name))))))
               class-sym)
     :cljs class-sym))

(defn classes-for-type-predicate
  ([pred lang] (classes-for-type-predicate pred lang nil))
  ([pred lang type-arglist]
  (throw-unless ((fn-or symbol? keyword? string?) pred) (->ex "Type predicate must be a symbol, keyword, or string." {:pred pred}))
  (cond
    (and (symbol? pred) (anap/possible-type-predicate? pred))
      (->> tcore/types-unevaled
           (<- get lang)
           (<- get pred)
           (<- validate nempty?)
           (into []))
    :else [pred])))

(defn expand-classes-for-type-hint
  ([x lang ns-] (expand-classes-for-type-hint x lang ns- nil))
  ([x lang ns- arglist]
    (condf x
      (fn-or symbol? keyword?)
        (fn-> hash-set (expand-classes-for-type-hint lang ns- arglist))
      set?    (fn->> (map (fn1 classes-for-type-predicate lang arglist))
                     (apply concat)
                     (map #(get-qualified-class-name lang ns- %))
                     (into #{}))
      string? (fn-> symbol hash-set)
      ;nil?    (constantly #{'Object})
      #(throw (->ex "Not a type hint." %)))))

(defn hint-arglist-with
  [arglist hints]
  (reducei ; technically reduce-pair
    (fn [arglist-f arg i]
      (conj arglist-f (cmacros/hint-meta arg (get hints i))))
    []
    arglist))

(def defnt-remove-hints
  (fn->> (into [])
         (<- update 0 (fn->> (filter symbol?) (into [])))))

(defn defnt-arities
  {:out '[[[^string? x] (println "A string!")]
          [[^vector? x] (println "A vector!")]]}
  [{:as env :keys [body]}]
  {:full-arities
     (condf body
       (fn-> first vector?) (fn->> defnt-remove-hints vector)
       (fn-> first seq?   ) (fn->> (mapv defnt-remove-hints))
       #(throw (->ex "Unexpected form when trying to parse arities." %)))
   :arities
     (let [split-arity (fn [body'] {:arglist (first body') :body (list* 'do (rest body'))})]
       (condf body
         (fn-> first vector?) (fn->> split-arity vector)
         (fn-> first seq?   ) (fn->> (mapv (fn-> split-arity)))
         #(throw (->ex "Unexpected form when trying to parse arglists." %))))})

(defn defnt-arglists
  {:out '[[^string? x] [^vector? x]]}
  [{:as env :keys [body]}]
  {:arglists
    (condf body
      (fn-> first vector?) (fn->> first vector)
      (fn-> first seq?   ) (fn->> (mapv first))
      #(throw (->ex "Unexpected form when trying to parse arglists." %)))})

(defn defnt-gen-protocol-name
  [sym lang]
  (with-meta
    (if (= lang :clj)
        (-> sym name (str "-protocol") symbol)
        sym)
    (meta sym)))

(defn defnt-gen-protocol-names
  "Generates |defnt| protocol names"
  [{:keys [sym sym-with-meta strict? lang]}]
  (let [genned-protocol-name
          (when-not strict? (-> sym name cbase/camelcase (str "Protocol") munge symbol))
        genned-protocol-method-name
          (defnt-gen-protocol-name sym-with-meta lang)
        genned-protocol-method-name-qualified
          (symbol (name (ns-name *ns*))
            (name genned-protocol-method-name))]
    (kw-map genned-protocol-name
          genned-protocol-method-name
          genned-protocol-method-name-qualified)))

(defn defnt-gen-interface-unexpanded
  "Note: Also used by CLJS, not because generating an interface is
         actually possible in CLJS, but because |defnt| is designed
         such that its base is of a gen-interface format."
  {:out '{[Randfn [#{java.lang.String}] Object]
            [[^string? x] (println "A string!")],
          [Randfn
           [#{clojure.lang.APersistentVector$RSeq clojure.lang.Tuple
              clojure.lang.APersistentVector
              clojure.lang.PersistentVector
              clojure.core.rrb_vector.rrbt.Vector}]
           Object]
            [[^vector? x] (println "A vector!")]}}
  [{:keys [sym full-arities arglists-types lang ns-]}]
  {:post [(log/ppr-hints :macro-expand "GEN INTERFACE CODE BODY UNEXP" %)]}
  (assert (nempty? full-arities))
  (let [genned-method-name
          (-> sym name cbase/camelcase munge symbol)
        genned-interface-name
          (-> sym name cbase/camelcase (str "Interface") munge symbol)
        ns-qualified-interface-name
          (-> genned-interface-name
              #?(:clj (cbase/ns-qualify (namespace-munge *ns*))))
        gen-interface-code-header
          (list 'gen-interface :name ns-qualified-interface-name :methods)
        gen-interface-code-body-unexpanded
          (->> arglists-types ; [[int String] int]
               (map (fn [[type-arglist-n return-type-n :as arglist-n]]
                      (let [_ (log/pr :macro-expand "UNEXPANDED TYPE HINTS" type-arglist-n "IN NS" ns-)
                            type-hints-n (->> type-arglist-n
                                              (mapv (fn1 expand-classes-for-type-hint lang ns- type-arglist-n)))
                            _ (log/pr :macro-expand "EXPANDED TYPE HINTS" type-hints-n)]
                        (with-meta [type-hints-n return-type-n]
                          (when-let [type-not-handled-by-interface (-> type-arglist-n first #{'default 'nil?})]
                            {(-> type-not-handled-by-interface name keyword) true}))))) ; label it so interface can take it out later, and protocol can keep it in
               (map (fn [type-arglist+return-type]
                      (with-meta (into [genned-method-name] type-arglist+return-type) ; To preserve `^:default` or `^:nil?`
                                 (meta type-arglist+return-type))))
               (<- zipmap full-arities))] ; TODO ensure unique, don't just assume
    (log/ppr-hints :macro-expand "gen-interface-code-body-unexpanded" gen-interface-code-body-unexpanded)
    (assert (nempty? gen-interface-code-body-unexpanded))
    (kw-map genned-method-name
          genned-interface-name
          ns-qualified-interface-name
          gen-interface-code-header
          gen-interface-code-body-unexpanded)))

(defn positional-profundal->hint [lang position depth arglist hints]
  (if-not-let [hint (get hints position)] ; is already qualified
    (throw (->ex "Position out of range for arglist" (kw-map position depth arglist hints)))
    (if-not depth
      hint
      (if (or #?(:cljs true) (= lang :cljs))
          (throw (->ex "Depth specifications not supported for hints in CLJS (yet)" (kw-map position depth arglist hints)))
          #?(:clj (tcore/nth-elem-type:clj hint depth))))))

(defn hints->with-replace-special-kws
  "E.g. :<1>:4, :<0>, etc. -> a particular type"
  {:todo "Handle forward references properly"}
  [{:as env :keys [lang arglist hints]}]
  {:post [(log/ppr-hints :macro-expand/params "Updated hints" {:new % :old hints})]}
  (reducei
    (fn [hints' hint i]
      (assoc hints' i
        (if-not-let [[position depth] (defnt-keyword->positional-profundal hint)]
          hint
          (cond (= position i)
                (err/throw-info "Can't have a self-referring positional hint" (kw-map position depth arglist hints))
                (> position i)
                (err/throw-info "(Currently) can't have a forward-referring positional hint" (kw-map position depth arglist hints))
                :else (positional-profundal->hint lang position depth arglist hints')))))
    (vec hints)
    hints))

(defn >explicit-ret-type
  "E.g. :<1>:4, :<0>, etc. -> a particular type"
  [{:as env :keys [ns- lang arglist hints ret-type-0]}]
  {:post [(log/ppr-hints :macro-expand/params "Explicit ret type" (kw-map % hints))]}
  ; [get-max-type (delay (tdefs/max-type hints))]  ; TODO maybe come back to this?
   #_(:clj ; TODO maybe come back to this
    [(= ret-type-0 'auto-promote)
       (or (get tdefs/promoted-types @get-max-type) @get-max-type)])
    (if-not-let [[position depth] (defnt-keyword->positional-profundal ret-type-0)]
      (if (nil? ret-type-0)
          nil
          (or (get-qualified-class-name lang ns- (-> ret-type-0 (classes-for-type-predicate lang) first))
              (get-qualified-class-name lang ns- ret-type-0)))
      (positional-profundal->hint lang position depth arglist hints)))

(defn recursive?
  [{:keys [ns- fn-sym expr]}]
  (let [ns-str (-> ns- ns-name name)]
    (->> expr
         (cbase/prewalk-find
           #_(fn-and anap/sym-call? (fn-> first (anap/symbol-eq? sym))) ; might not be a sym call — might e.g. be in a `->`
           (fn-and symbol?
                   (fn-or #(= (namespace %) (namespace fn-sym))
                          #(and (= (namespace %) ns-str) (nil? (namespace fn-sym)))
                          #(and (nil? (namespace %))     (= (namespace fn-sym) ns-str)))
                   (fn-or (fn1 anap/symbol-eq? fn-sym)
                          (fn1 anap/symbol-eq? (symbol (str (name fn-sym) "-protocol")))
                          (fn1 anap/symbol-eq? (symbol (str (name fn-sym) "&")))))) ; neither is strict macro
         first)))

(defn fn-expr->return-type
  [{:as args :keys [lang fn-sym expr explicit-ret-type]}]
  (validate fn-sym symbol?)
  (let [non-recursive?      (not (recursive? args))
        explicit-ret-type   (ana/tag->class explicit-ret-type)
        inferred-ret-type   (and (or non-recursive? nil) ; TODO currently doesn't process recursive calls because while possible, it's a little more involved
                        #?(:clj  (when (= lang :clj) (ana/jvm-typeof-respecting-hints expr))
                           :cljs nil)) ; TODO CLJS
        _ (log/ppr-hints :macro-expand/params (kw-map inferred-ret-type explicit-ret-type expr))
        _ (validate nil
            (s/constantly-or
              ; Inferred overrides
              (nil? explicit-ret-type)
              ; Explicit overrides
              (and explicit-ret-type (nil? inferred-ret-type))
              ; Exact match
              (= explicit-ret-type inferred-ret-type)
      #?(:clj (isa? inferred-ret-type explicit-ret-type))  ; e.g. can return explicit InputStream if BufferedInputStream inferred
              (and (.isPrimitive ^Class explicit-ret-type)
                   (.isPrimitive ^Class inferred-ret-type)
                   (= (symbol (.getName ^Class explicit-ret-type))
                      (tdefs/max-type #{(symbol (.getName ^Class explicit-ret-type))
                                        (symbol (.getName ^Class inferred-ret-type))}))))) ; e.g. explicit long cast is permitted if int was inferred ; TODO simplify this code to e.g. `safe-castable?`
        ret-type (or explicit-ret-type
                     inferred-ret-type
                     (get trans/default-hint lang))
        _ (log/prl :macro-expand/params ret-type)]
    ret-type))

(defn defnt-gen-interface-expanded
  "Also used by CLJS, not because generating an interface is
   actually possible in CLJS, but because |defnt| is (temporarily) designed
   such that its base is of a gen-interface format."
  {:tests '{'[[Func [#{String} #{vector?}       ] long]]
            '[[Func [String    IPersistentVector] long]
              [Func [String    ITransientVector ] long]]}}
  [{:keys [sym lang ns-
           gen-interface-code-body-unexpanded]
    :as env}]
  {:post [(log/ppr-hints :macro-expand "GEN INTERFACE CODE BODY EXP" (:gen-interface-code-body-expanded %))]}
  (assert (nempty? gen-interface-code-body-unexpanded))
  (let [gen-interface-code-body-expanded
          (->> gen-interface-code-body-unexpanded
               (mapv (fn [[params-decl [arglist & body :as arity]]]
                       (let [[method-name hints ret-type-0] params-decl
                             assoc-arity-etc
                               (fn [hints]
                                 (let [body                (list* 'do body)
                                       hints               (map (whenf1 string? symbol) hints)
                                       hints               (hints->with-replace-special-kws (merge env (kw-map arglist hints)))
                                       arglist-hinted      (hint-arglist-with arglist hints)
                                       ;_ (log/ppr-hints :macro-expand "TYPE HINTS FOR ARGLIST" (->> arglist-hinted (map type-hint)))
                                       explicit-ret-type   (>explicit-ret-type (merge env (kw-map ret-type-0 hints arglist)))
                                       ; TODO cache the result of postwalking the body like this, for protocol purposes
                                       contextualized-body (list* 'let ; an alternative to enclosing in function
                                                                       (vec (interleave (mapv unhint arglist-hinted)
                                                                                        (repeat (count arglist-hinted) (list `identity nil))))
                                                                      (trans/hint-body-with-arglist [body] arglist-hinted :clj :protocol))
                                       ret-type-input (assoc (kw-map ns- lang explicit-ret-type) :fn-sym sym :expr contextualized-body)
                                       _ (log/ppr-hints :macro-expand "COMPUTING RETURN TYPE FROM" ret-type-input)
                                       ret-type (some-> (fn-expr->return-type ret-type-input) name+ symbol)
                                       _ (log/ppr-hints :macro-expand/params "COMPUTED RETURN TYPE:" ret-type)
                                       arity-hinted (assoc arity 0 arglist-hinted)]
                                   (with-meta [[method-name hints ret-type] (seq arity-hinted)]
                                     (meta params-decl))))] ; to ensure ^:default and ^:nil? get passed on
                         (->> (apply combo/cartesian-product hints) ; expand the hints
                              (mapv assoc-arity-etc)))))
               (apply catvec))
         gen-interface-code-body-expanded-relevant-to-interface
           (->> gen-interface-code-body-expanded
                (filterv (fn-and (fn-> meta :default not)
                                 (fn-> meta :nil?    not))))] ; keep these for protocol only
    (kw-map gen-interface-code-body-expanded
            gen-interface-code-body-expanded-relevant-to-interface)))

(defn defnt-gen-interface-def
  [{:keys [gen-interface-code-header gen-interface-code-body-expanded-relevant-to-interface]}]
  {:post [(log/ppr-hints :macro-expand "INTERFACE DEF:" %)]}
  {:gen-interface-def
    (concat gen-interface-code-header ; technically |conjr|
      (list (mapv first gen-interface-code-body-expanded-relevant-to-interface)))})

(defn defnt-positioned-types-for-arglist
  {:todo "The shape of this (loop-reducei) is probably useful and can be reused"}
  [arglist types]
  (loop [i 0 arglist-n arglist types-n types]
    (let [type-n (first arglist-n)]
      (if (empty? arglist-n)
          types-n
          (recur (inc i) (rest arglist-n)
                 (update types-n i
                   (fn [type-map]
                     (let [new-map (zipmap (ensure-set type-n) (repeat #{(count arglist)}))]
                     (if (nil? type-map)
                         new-map
                         (merge-with set/union type-map new-map))))))))))

(defn defnt-types-for-arg-positions
  {:out-like '{0 {string?  #{3} number? #{2 3}  }
               1 {decimal? #{0} Object  #{0 2 3}}
               2 nil}}
  [{:keys [lang ns- arglists-types]}]
  {:post [(do (log/ppr :macro-expand "TYPES FOR ARG POSITIONS" %)
              true)]}
  (let [types (->> arglists-types
                   (map first)
                   (reduce (fn [types-n arglist-n]
                             (defnt-positioned-types-for-arglist arglist-n types-n))
                             {})
                   (map (fn1 update-val
                          (fn->> (map (fn [[type-hint arity-cts]]
                                          ; TODO what are you going to do with arity-cts?
                                          (zipmap (expand-classes-for-type-hint type-hint lang ns-)
                                                  (repeat arity-cts))))
                                 (apply merge-with set/union))))
                   (into {}))]
    {:types-for-arg-positions types
     :first-types             (get types 0)}))

(defn protocol-verify-unique-first-hint
  "Not allowed same arity and same first hint.

   Protocols can't dispatch on non-first args.
   ([^long x ^int    y ^char z] (+ x y) (str z))
   ([^long x ^String y ^char z] (str x z y))"
  {:in-like '[[Abcde [#{int } #{String               }] int  ]
              [Abcde [#{long} #{APersistentVector ...}] float]]
   :todo ["Allow for implicitly convertible types (e.g. long and Long) with the same codebase to
           pass through this verification"]}
  [arglists]
  (log/ppr-hints :macro-expand "ARGLISTS" arglists)
  (let [cached-arglists (atom {})]
    (doseq [[method-sym arglist ret-type] arglists]
      (doseq [first-hint (first arglist)]
        (when (nnil? first-hint)
          (swap! cached-arglists update (count arglist)
            (fn [first-hints-set]
              (let [hints-set-ensured (ensure-set first-hints-set)]
                (if (contains? hints-set-ensured first-hint)
                    (throw (->ex "Not allowed same arity and same first hint:" arglist))
                    (conj hints-set-ensured first-hint))))))))))

; TODO Can run on other platforms but the behavior is language/platform-specific
; TODO not used, but move
#?(:clj
(defn ->array-type-str
  "Assumes `base-class-str` is already fully qualified if non-primitive."
  {:example '{(->array-type-str "long" 4)
              "[[[[J"}}
  [base-class-str dimension]
  (let [base-str (or (get-in tdefs/primitive-type-meta [(symbol base-class-str) :array-ident])
                     (str "L" base-class-str ";"))]
    (str (apply str (repeat dimension "[")) base-str))))

; TODO Can run on other platforms but the behavior is language/platform-specific
; TODO not used, but move
#?(:clj
(defn clojure-reflection-type->class
  "Converts e.g. 'double<> -> (Class/forName \"[D\")"
  [sym]
  (let [[array-type? base-class-str brackets] (re-matches #"([^<>]+)((?:<>)+)$" (name sym))]
    (ana/tag->class
      (if array-type?
          (let [dimension (-> brackets count (/ 2))
                class-str (->array-type-str base-class-str dimension)]
            class-str) ; Array types are preserved as strings, not classes
          (or (get-in tdefs/primitive-type-meta [sym :unboxed])
               sym))))))

; TODO move the below few fns

#?(:clj (def not-matchable (Object.)))

#?(:clj
(defn hint-expr-embeddably [expr hint]
  (if (symbol? expr)
      (hint-meta expr (ana/->embeddable-hint hint))
      (tcore/static-cast-code (ana/->embeddable-hint hint) expr))))

#?(:clj
(defn hint-expr-with-class [expr hint]
  (assert (class? hint) hint)
  (if (anap/hinted-literal? expr)
      expr
      (hint-meta expr hint))))

#?(:clj
(defn expr->with-embeddable-hints [expr]
  (if-let [hint (ana/type-hint expr)]
    (hint-expr-embeddably expr hint)
    expr)))

#?(:clj
(defn ^Class expr->hint:class [expr]
  (if (anap/hinted-literal? expr)
      (ana/jvm-typeof expr)
      (ana/type-hint:class expr))))

#?(:clj
(defn try-param-match
  {:todo #{"Break this up"}}
  [env arg ^Class actual-type ^Class expected-type]
  (log/prl :macro-expand/params expected-type actual-type)
  (if ; exact match
      (= actual-type expected-type)
      (hint-expr-with-class arg expected-type)
      (cond (nil? expected-type)
            (hint-expr-with-class arg actual-type) ; Might be upcasting to Object, so that's fine
            (nil? actual-type)
            (if (= expected-type Object)
                (hint-expr-with-class arg expected-type)
                not-matchable) ; don't want to unsafely downcast
            ; Array0 -> Array1 => <fail>
            (.isArray expected-type)
            not-matchable
            ; Array -> Object || Array -> <any> => <fail>
            (.isArray actual-type)
            (if (= expected-type Object)
                (hint-expr-with-class arg expected-type)
                not-matchable)
            (.isPrimitive actual-type)
            (cond-let
              [c (get-in tcore/unboxed->convertible [actual-type expected-type])]
              ; cast unboxed primitive to compatible unboxed primitive via Clojure intrinsic
              (hint-expr-with-class `(~(symbol "clojure.core" (.getName ^Class c)) ~arg) c)
              [c (or (get-in tcore/unboxed->convertible [actual-type (tcore/unboxed->boxed expected-type)])
                     (and (= expected-type Object) ; box in order to cast to Object
                          (tcore/unboxed->boxed actual-type)))]
              ; cast via boxing to compatible boxed primitive
              (hint-expr-with-class `(new ~(symbol (.getName ^Class c)) ~arg) c)
              not-matchable)
            (tcore/boxed->unboxed actual-type)
            (cond-let
              [c (get-in tcore/unboxed->convertible [(tcore/boxed->unboxed actual-type) expected-type])]
              ; cast boxed primitive to compatible unboxed primitive via Clojure intrinsic
              (hint-expr-with-class `(~(symbol "clojure.core" (.getName ^Class c)) ~arg) c)
              [c (get-in tcore/unboxed->convertible [(tcore/boxed->unboxed actual-type) (tcore/unboxed->boxed expected-type)])]
              ; simple side-cast
              (hint-expr-with-class arg expected-type)
              not-matchable)
            ; upcast, e.g. Integer -> Number
            (.isAssignableFrom expected-type actual-type)
            (hint-expr-with-class arg expected-type)
            ; downcast, e.g. Number -> Integer, except if type essentially unknown
            (and (not= actual-type Object)
                 (.isAssignableFrom actual-type expected-type))
            (hint-expr-with-class arg expected-type)
            :else not-matchable)))) ; unrelated types

#?(:clj
(defn try-params-match [env args argtypes types]
  {:post [(log/ppr-hints :macro-expand/params "Params match?" (kw-map argtypes types %))]}
  (->> (interleave args types)
       (partition-all 2)
       (reducei (fn [args' [arg expected-type] i]
                  (let [actual-type (get argtypes i)
                        matched (try-param-match env arg actual-type expected-type)]
                    (if (= matched not-matchable)
                        (reduced nil)
                        (conj args' matched))))
                []))))

#?(:clj
(defn compare-specificity
  "Compare specificity of t0 to t1.
   0 means they are equally specific.
   -1 means t0 is less specific (more general) than t1.
   1 means t0 is more specific (less general) than t1.
   Unboxed primitives are considered to be more specific than boxed primitives."
  [^Class t0 ^Class t1]
  (cond (= t0 t1)
        0
        (= t0 Object)
        -1
        (= t1 Object)
        1
        (= (tcore/boxed->unboxed t0) t1)
        -1
        (= t0 (tcore/boxed->unboxed t1))
        1
        (or (tcore/primitive-array-type? t0) (tcore/primitive-array-type? t1))
        nil ; we'll consider the two unrelated
        (isa? t1 t0)
        -1
        (isa? t0 t1)
        1
        :else nil))) ; unrelated

#?(:clj
(defn most-specific-arg-matches
  [matches]
  (reduce
    (fn [matches' match]
      (let [match' (last matches')
            specificity-scores
              (for [i (range (count match))]
                (let [t  (expr->hint:class (get match  i))
                      t' (expr->hint:class (get match' i))]
                  (compare-specificity t t')))
            specificity-score (->> specificity-scores (remove nil?) (apply +))]
        (log/ppr-hints :macro-expand/params (kw-map specificity-score match match'))
        (cond (pos? specificity-score) [match]
              (neg? specificity-score) matches'
              :else (conj matches' match))))
    [(first matches)] (rest matches))))

#?(:clj
(defn class->public-methods:uncached
  "clojure/reflect would be nice to use, but:
   1) this avoids caching, which is undesirable when `defnt` interfaces are redefined
   2) this avoids the `<>` notation used with arrays and instead uses classes directly.

   Note that this unlike the Clojure reflection API, doesn't return flags, exception
   types, or the declaring class."
  [^Class c]
  (->> c
       .getDeclaredMethods
       (mapv (fn [^java.lang.reflect.Method x]
               (clojure.reflect.Method.
                 (.getName x)
                 (.getGenericReturnType x)
                 nil
                 (->> x .getGenericParameterTypes vec)
                 nil
                 nil))))))

#?(:clj
(defmemoized class->public-methods {:memoize-only-first? true}
  "This caches to give more speed.
   It ensures, unlike clojure/reflect, that reflecting on a redefined class does not
   yield the same results."
  {:todo #{"finer-grained, faster, and more space-efficient memoization using Google Guava"}}
  [c] (class->public-methods:uncached c)))

#?(:clj
(defn args->matches
  ([sym classname method args strict?] (args->matches sym classname method args strict? nil))
  ([sym classname method args strict? env]
    (log/ppr-hints :macro-expand/params "Matching args" {:args args :class classname :method method})
    (let [arity    (count args)
          class-   (resolve classname)
          _        (assert (some? class-) (kw-map classname))
          method-name (name method)
          argtypes (mapv #(ana/jvm-typeof-respecting-hints % env) args)
          all-methods (class->public-methods class-)
          _ (log/prl :macro-expand/params all-methods)
          possible-matches
            (->> all-methods
                 (filter (fn->> :name (= method-name)))
                 (filter (fn->> :parameter-types count (= arity)))
                 (map    (fn->> :parameter-types (try-params-match env args argtypes)))
                 (remove nil?))
          _ (log/ppr-hints :macro-expand/params "Possible matches:" (kw-map sym args possible-matches))
          most-specific-matches
            (when-not (empty? possible-matches)
              (most-specific-arg-matches possible-matches))
          ; TODO derepeat
          _ (if (and (or (-> most-specific-matches count (> 1))
                         (-> most-specific-matches count (= 0)))
                     (or (and strict? @warn-on-strict-inexact-matches?)
                         @warn-on-all-inexact-matches?))
                (log/ppr-hints :warn
                  ; TODO line number etc.
                  (if (-> most-specific-matches count (> 1))
                     "More than one matching method found for args. Hint one or more args to fix this."
                     "No method found for args. Falling back to reflection. Hint one or more args to fix this.")
                  {:callsite [classname method
                              {:symbolic args
                               :types    argtypes}]
                   :possible-matches possible-matches
                   :most-specific-matches most-specific-matches})
                (log/ppr-hints :macro-expand/params "Most specific matches:" (kw-map sym args most-specific-matches)))]
      most-specific-matches))))

#?(:clj
(defn output-call-to-protocol-or-reify
  ([{:keys [sym env reify-available? strict?
            protocol-method
            reify-name interface-name interface-method
            args]}]
    (if (not reify-available?)
        ; Protocol dispatch
        (if strict?
            (err/throw-info "Conflicting `defnt` options: strict, but no reify available" {:defnt-sym sym})
            (do (assert (symbol? protocol-method) (kw-map protocol-method))
                `(~protocol-method ~@args)))
        ; Matches even when it has incomplete type information
        (let [matches    (args->matches sym interface-name interface-method args strict? env)
              best-match (->> (or (first matches) args)
                              (map expr->with-embeddable-hints))]
          (if (-> matches count (= 1))
              `(. ~reify-name ~interface-method ~@best-match)
                ; If no one specific match is found,
              (if strict?
                  ; and is strict, outputs warning and proceeds with the reify with the original args
                  `(. ~reify-name ~interface-method ~@args)
                  ; Otherwise, goes with the protocol, passing in the original args
                  `(~protocol-method ~@args))))))))

(defn defnt-gen-helper-macro
  "Generates the macro helper for |defnt|.
   A call to the |defnt| macro expands to this macro, which then expands, based
   on the availability of type hints, to either the reify version or the protocol version."
  {:todo ["Already tried to make this an inline function via metaing :inline,
           but the problem with trying to do inline is that inlines can't be variadic."]}
  [{:keys [genned-method-name
           genned-protocol-method-name-qualified
           ns-qualified-interface-name
           reified-sym-qualified
           strict-sym-postfix
           strict?
           relaxed?
           sym-with-meta
           lang]}]
  {:post [(log/ppr-hints :macro-expand "HELPER MACRO DEF" %)]}
   (let [args-sym        'args-sym
         args-hinted-sym 'args-hinted-sym
         args-matched-sym 'args-matched-sym
         gen (fn [strict-macro?]
               (let [postfix (when strict-macro? (or strict-sym-postfix "&"))
                     sym-with-meta' (replace-meta-from (symbol (str (name sym-with-meta) postfix)) sym-with-meta)]
                `(defmacro ~sym-with-meta' [& ~args-sym]
                   (let [~args-hinted-sym
                           (quantum.core.macros.transform/try-hint-args ; TODO remove this?
                             ~args-sym ~lang ~'&env)]
                     (log/pr :macro-expand/defnt-helper
                        "DEFNT HELPER MACRO" '~sym-with-meta'
                                         "|"  ~strict-macro?
                                         "|"  ~args-hinted-sym
                                         "|" '~genned-protocol-method-name-qualified
                                         "|" '~genned-method-name)
                     (if ~(when-not (or strict-macro? strict?)
                           `(or (case-env* ~'&env :cljs true)
                                (= ~lang :cljs)
                                ~relaxed? ; TODO fix this?
                                #_(quantum.core.macros.transform/any-hint-unresolved?
                                  ~args-hinted-sym ~lang ~'&env)))
                         (output-call-to-protocol-or-reify
                           {:env              ~'&env
                            :sym              '~sym-with-meta'
                            :reify-available? false
                            :protocol-method  '~genned-protocol-method-name-qualified
                            :args              ~args-hinted-sym})
                         ; Matches even when it has incomplete type information
                         ; If more than one equally specific match is found, calls the protocol
                         (output-call-to-protocol-or-reify
                           {:env              ~'&env
                            :sym              '~sym-with-meta'
                            :reify-available? true
                            :strict?          ~(or strict? strict-macro?)
                            :protocol-method  '~(when-not (or strict? strict-macro?) genned-protocol-method-name-qualified)
                            :reify-name       '~reified-sym-qualified
                            :interface-name   '~ns-qualified-interface-name
                            :interface-method '~genned-method-name
                            :args              ~args-hinted-sym}))))))]
     {:helper-macro-def
       `(do ~@[(when-not relaxed? (gen true))]
            ~(gen false))}))

(defn defnt-arglists-types
  {:out-like `{:arglists-types
                ([[AtomicInteger] Object]
                 [[integer?] Object]
                 [[#{String StringBuilder} #{boolean char}] Object]
                 [[#{short byte} #{long int} #{double float}] Object])}}
  [{:as env :keys [lang sym arities]}]
  {:post [(log/ppr-hints :macro-expand "TYPE HINTS EXTRACTED" %)]}
  {:arglists-types
    (->> arities
         (map #(trans/extract-all-type-hints-from-arglist lang sym %))
         doall)})

(defn defnt-extend-protocol-def
  [{:as env :keys [strict? types-for-arg-positions]}]
  {:post [(log/ppr-hints :macro-expand "EXTEND PROTOCOL DEF" %)]}
  {:extend-protocol-def
    (when-not strict?
      (proto/gen-extend-protocol-from-interface env))})

(defn defnt-gen-final-defnt-def
  "The finishing function for |defnt|.
   Takes an aggregated environment and declares and defines the |defnt|:
   - Interface, if a Clojure environment and not relaxed
   - |reify|
   - Protocol and extend-protocol, if not strict
   - Macro (helper), if a Clojure environment
   - Protocol alias, if a ClojureScript environment"
  [{:keys [lang sym strict? externs genned-protocol-method-names
           gen-interface-def tag
           reify-def reified-sym
           helper-macro-def
           protocol-def extend-protocol-def]}]
  {:post [(log/ppr-hints :macro-expand "DEFNT FINAL" %)]}
  (let [primary-protocol-sym (-> sym name (str "-protocol") symbol)]
    (concat (list* 'do @externs)
      [(when (= lang :clj) gen-interface-def)
       (when-not strict?
         (list* 'declare (cons primary-protocol-sym genned-protocol-method-names))) ; For recursion
       (when (= lang :clj) (list 'declare reified-sym)) ; For recursion
       (when (= lang :clj) helper-macro-def)
       (when (= lang :clj) reify-def)
       protocol-def
       extend-protocol-def
       (when (= lang :cljs)
         (list `defalias primary-protocol-sym sym))
       (when-not strict?
         `(doto (var ~primary-protocol-sym)
                (alter-meta! (fn [m#] (assoc m# :tag ~(ana/sanitize-tag lang tag))))))])))

#?(:clj
(defn defnt*-helper
  "CLJ because it's used by a macro."
  {:todo ["Make so Object gets protocol if reflection"
          "Add support for nil"
          "Add support for destructuring — otherwise
           'ClassCastException clojure.lang.PersistentVector cannot be
            cast to clojure.lang.Named'"]
   :in '(defnt randfn
          ([^string? x] (println "A string!"))
          ([^vector? x] (println "A vector!")))}
  ([opts lang ns- sym doc- meta- body [unk & rest-unk]]
    (apply mfn/defn-variant-organizer
      [defnt*-helper opts lang ns- sym doc- meta- body (cons unk rest-unk)]))
  ([{:as opts
     :keys [strict? relaxed?]} lang ns- sym-0 doc- meta- body]
    (log/ppr :debug (kw-map opts lang ns- sym-0 doc- meta- body))
    (let [externs       (atom [])
          sym           (with-meta sym-0 (-> sym-0 meta (dissoc :inline)))
          sym-with-meta (with-meta sym (merge {:doc doc-} meta- (-> sym meta (dissoc :tag))))
          tag           (get-qualified-class-name lang ns- (-> sym meta :tag))
          body          (mfn/optimize-defn-variant-body! body externs)
          strict-sym-postfix (-> sym-with-meta meta :strict-postfix)
          env (-> (kw-map sym strict? relaxed? sym-with-meta lang ns- body externs tag strict-sym-postfix)
                  (merge-call defnt-arities
                              defnt-arglists
                              defnt-gen-protocol-names
                              defnt-arglists-types
                              defnt-gen-interface-unexpanded
                              defnt-types-for-arg-positions
                              defnt-gen-interface-expanded
                              defnt-gen-interface-def
                              (fn [env] {:reify-body (reify/gen-reify-body env)}) ; Necessary for `gen-extend-protocol-from-interface`
                              (fn [env] (when (= lang :clj)
                                          (reify/gen-reify-def env)))
                              (fn [env] (when-not strict?
                                          (proto/gen-defprotocol-from-interface env)))
                              defnt-extend-protocol-def
                              (fn [env] (when (= lang :clj)
                                          (defnt-gen-helper-macro env)))))]
      (defnt-gen-final-defnt-def env)))))

(declare defnt*-helper)

#?(:clj
(defmacro defnt
  "|defn|, typed.
   Uses |gen-interface|, |reify|, and |defprotocol| under the hood."
  [sym & body]
  (let [lang (case-env :clj :clj :cljs :cljs)]
    (eval `(declare ~(ana/sanitize-sym-tag lang sym) ~(ana/sanitize-sym-tag lang (defnt-gen-protocol-name sym lang)))) ; To allow recursive analysis
    (defnt*-helper nil lang *ns* sym nil nil nil body))))

#?(:clj
(defmacro defnt'
  "'Strict' |defnt|. I.e., generates only an interface and no protocol.
   Only for use with Clojure."
  [sym & body]
  (let [lang :clj]
    (eval `(declare ~(ana/sanitize-sym-tag lang sym))) ; To allow recursive analysis
    (defnt*-helper {:strict? true} lang *ns* sym nil nil nil body))))

#?(:clj
(defmacro defntp
  "'Relaxed' |defnt|. I.e., generates only a protocol and no interface."
  [sym & body]
  (let [lang :clj]
    (eval `(declare ~(ana/sanitize-sym-tag lang sym) ~(ana/sanitize-sym-tag lang (defnt-gen-protocol-name sym lang)))) ; To allow recursive analysis
    (defnt*-helper {:relaxed? true} lang *ns* sym nil nil nil body))))

