(ns
  ^{:doc "Some useful macros, like de-repetitivizing protocol extensions.
          Also some plumbing macros for |for| loops and the like."
    :attribution "Alex Gunnarson"
    :cljs-self-referring? true}
  quantum.core.macros
           (:refer-clojure :exclude [macroexpand macroexpand-all])
           (:require [clojure.walk
                       :refer [postwalk]                              ]
                     [#?(:clj  clojure.core
                         :cljs cljs.core   )    :as core              ]
                     [quantum.core.error        :as err
                       :refer [->ex]                                  ]
                     [quantum.core.log          :as log               ]
                     [quantum.core.logic        :as logic
                       :refer [#?@(:clj [fn-and whenc whenf*n]) nnil?]]
                     [quantum.core.macros.core  :as cmacros       
                       :refer [#?(:clj if-cljs)]                      ]
                     [quantum.core.macros.defnt :as defnt             ]
                     [quantum.core.macros.fn    :as mfn               ]
                     [quantum.core.vars         :as var
                       :refer [#?@(:clj [defalias defmalias])]        ])
  #?(:cljs (:require-macros
                     [quantum.core.macros.core  :as cmacros
                       :refer [if-cljs]                               ]
                     [quantum.core.macros
                       :refer [assert-args]]
                     [quantum.core.log          :as log               ]
                     [quantum.core.logic        :as logic
                       :refer [fn-and whenc whenf*n]                  ]
                     [quantum.core.vars         :as var
                       :refer [defalias defmalias]                    ])))

#?(:clj
(defmacro maptemplate
  [template-fn coll] ; TODO warn about runtime eval
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

; TODO move to conversion

#?(:clj
(defn var->symbol [x]
  (let [name- (-> x meta :name       name)
        ns-   (-> x meta :ns ns-name name)]
    (symbol ns- name-))))

#?(:clj
(defn qualify [x]
  (if-let [resolved (resolve x)]
    (-> resolved var->symbol)
    (throw (->ex :sym-not-found (str "Symbol not able to be resolved: " x) x)))))

#?(:clj
(defmacro deftransmacro
  "Defines a trans/cross-platform macro.
   If expansion is detected to be in a CLJS environment,
   it expands using the CLJS symbol."
  [name clj-fn cljs-fn]
  (let [args-sym (gensym)
        clj-fn-f  clj-fn  #_(qualify clj-fn )
        cljs-fn-f cljs-fn #_(qualify cljs-fn)]
   `(defmacro ~name [& ~args-sym]
      (let [ret# (apply list (if-cljs ~'&env '~cljs-fn-f '~clj-fn-f) ~args-sym)]
        (log/pr :macro-expand "EXPANDING |deftransmacro|:" ret#)
        ret#)))))

#?(:clj
(defmacro variadic-proxy
  "Creates left-associative variadic forms for any unary/binary operator."
  {:attribution  "ztellman/primitive-math"
   :contributors ["Alex Gunnarson"]}
  ([name clj-fn & [cljs-fn clj-single-arg-fn cljs-single-arg-fn]]
     (let [x-sym (gensym "x")
           y-sym (gensym "y")]
       `(defmacro ~name
          ([~x-sym]
            ~(let [clj-single-arg-fn-f  (whenc clj-single-arg-fn  nil? clj-fn )
                   cljs-single-arg-fn-f (whenc cljs-single-arg-fn nil? cljs-fn)]
              (if-cljs &env
                 (do (assert (nnil? cljs-single-arg-fn-f))
                     `(list '~cljs-single-arg-fn-f ~x-sym))
                 `(list '~clj-single-arg-fn-f      ~x-sym))))
          ([~x-sym ~y-sym]
             ~(log/pr :macro-expand "EXPANDING INTO CLJS ENV?" (if-cljs &env true false))
             ~(if-cljs &env
                (do (assert (nnil? cljs-fn))
                    `(list '~cljs-fn ~x-sym ~y-sym))
                `(list '~clj-fn      ~x-sym ~y-sym)))
          ([x# y# ~'& rest#]
             (list* '~name (list '~name x# y#) rest#)))))))

#?(:clj
(defmacro variadic-predicate-proxy
  "Turns variadic predicates into multiple pairwise comparisons."
  {:attribution  "ztellman/primitive-math"
   :contributors ["Alex Gunnarson"]}
  ([name clj-fn & [cljs-fn clj-single-arg-fn cljs-single-arg-fn]]
     (let [x-sym    (gensym "x"   )
           y-sym    (gensym "y"   )
           rest-sym (gensym "rest")]
       `(defmacro ~name
          ([~x-sym]
            ~(let [clj-single-arg-fn-f  (whenc clj-single-arg-fn  nil? clj-fn )
                   cljs-single-arg-fn-f (whenc cljs-single-arg-fn nil? cljs-fn)]
              (if-cljs &env
                 (do (assert (nnil? cljs-single-arg-fn-f))
                     `(list '~cljs-single-arg-fn-f ~x-sym))
                 `(list '~clj-single-arg-fn-f      ~x-sym))))
          ([~x-sym ~y-sym]
             ~(if-cljs &env
                (do (assert (nnil? cljs-fn))
                    `(list '~cljs-fn ~x-sym ~y-sym))
                `(list '~clj-fn      ~x-sym ~y-sym)))
          ([~x-sym ~y-sym ~'& ~rest-sym]
             ~(if-cljs &env
                `(list 'and                      (list '~name ~x-sym ~y-sym) (list* '~name ~y-sym ~rest-sym))
                `(list 'quantum.core.Numeric/and (list '~name ~x-sym ~y-sym) (list* '~name ~y-sym ~rest-sym)))))))))

; #?(:clj
; (defn param-arg-match
;   "Checks if the second argument can be used as the first argument.
;    Perhaps an .isAssignableFrom call might be better"
;   {:source "zcaudate/hara.reflect.util"}
;   [^Class param-type ^Class arg-type]
;   (cond (nil? arg-type)
;         (-> param-type .isPrimitive not)

;         (or (= param-type arg-type)
;             (-> param-type (.isAssignableFrom arg-type)))
;         true

;         :else
;         (condp = param-type
;           Integer/TYPE (or (= arg-type Integer)
;                            (= arg-type Long)
;                            (= arg-type Long/TYPE)
;                            (= arg-type Short/TYPE)
;                            (= arg-type Byte/TYPE))
;           Float/TYPE   (or (= arg-type Float)
;                            (= arg-type Double/TYPE))
;           Double/TYPE  (or (= arg-type Double)
;                            (= arg-type Float/TYPE))
;           Long/TYPE    (or (= arg-type Long)
;                            (= arg-type Integer/TYPE)
;                            (= arg-type Short/TYPE)
;                            (= arg-type Byte/TYPE))
;           Character/TYPE (= arg-type Character)
;           Short/TYPE     (= arg-type Short)
;           Byte/TYPE      (= arg-type Byte)
;           Boolean/TYPE   (= arg-type Boolean)
;           false))))


; ; If you use (sorted-set+) in macro code you get "can't resolve type hint: IPersistentMap"
; ; (class/all-implementing-leaf-classes 'clojure.lang.ILookup)


; #?(:clj (defalias hint-body-with-arglist deps/hint-body-with-arglist))

; (def default-hint (f*n hint-meta 'Object))

; #?(:clj
; (defn default-hint-if-needed
;   {:todo ["Eliminate |eval| via |resolve|"]}
;   [x]
;   (condf x
;     (fn-or anap/hinted-literal?
;            (fn-and seq?
;              (fn-> first symbol?)
;              (fn-> first resolve type-hint)))
;                     identity
;     symbol?         (whenf*n (fn-> meta :tag nil?) default-hint)
;     seq?            default-hint
;     keyword?        default-hint
;     set?            default-hint
;     map?            default-hint
;     :else           (constantly (->ex nil "Don't know how to make hint from" x)))))


; #?(:clj (defalias quote+ deps/quote+))
; #?(:clj (defalias extern- deps/extern-))
; #?(:clj (defalias identity* deps/identity*))
; #?(:clj (defalias inline-replace deps/inline-replace))

; ; ===== MACROEXPANSION =====

;#?(:clj (def macroexpand-1!   (fn-> macroexpand-1 pr/pprint-hints)))
;#?(:clj (def macroexpand!     (fn-> macroexpand     pr/pprint-hints)))
;#?(:clj (def macroexpand-all! (fn-> macroexpand-all pr/pprint-hints)))

#?(:clj
(defmacro assert-args [fn-name & pairs]
  `(do (when-not ~(first pairs)
         (throw (->ex :illegal-argument
                  ~(str fn-name " requires " (second pairs)))))
     ~(let [more (nnext pairs)]
        (when more
          (list* `assert-args fn-name more))))))

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
  (let [err (fn [& msg] (throw (->ex nil (apply str msg))))]
    (reduce 
      (fn [cont [k v]]
        (cond 
          (= k :let)   `(let ~v ~cont)
          (= k :while) `(if  ~v ~cont ~stop)
          (= k :when)  `(if  ~v ~cont ~skip)
          :else (err "Invalid 'for' keyword " k)))
      cont (reverse mod-pairs)))) ; this is terrible

(defn log!   [] (log/enable!  :macro-expand))
(defn unlog! [] (log/disable! :macro-expand))

#?(:clj (defmalias defn+       quantum.core.macros.fn/defn+      ))
#?(:clj (defmalias fn+         quantum.core.macros.fn/fn+        ))
#?(:clj (defalias  defmethod+  quantum.core.macros.fn/defmethod+ ))
#?(:clj (defalias  defmethods+ quantum.core.macros.fn/defmethods+))

#?(:clj (defmalias defnt       quantum.core.macros.defnt/defnt    ))
#?(:clj (defmalias defnt'      quantum.core.macros.defnt/defnt'   ))

#?(:clj (defmalias compile-if  quantum.core.macros.core/compile-if))
#?(:clj (defmalias quote+      quantum.core.macros.core/quote+    ))
#?(:clj (defmalias hint-meta   quantum.core.macros.core/hint-meta ))