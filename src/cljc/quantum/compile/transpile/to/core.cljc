(ns quantum.compile.transpile.to.core
           (:require [quantum.compile.transpile.util          :as util ]
                     [quantum.core.analyze.clojure.predicates :as anap ]
                     [quantum.core.data.map                   :as map  ]
                     [quantum.core.collections                :as coll
                       :refer [#?@(:clj [containsv? kmap popr popl])
                               in? dropl]                              ]
                     [quantum.core.convert                    :as conv
                       :refer [->name]]
                     [quantum.core.error                      :as err
                       :refer [#?(:clj throw-unless) ->ex]             ]
                     [quantum.core.log                        :as log  ]
                     [quantum.core.numeric                    :as num  ]
                     [quantum.core.string                     :as str  ]
                     [quantum.core.string.format              :as strf ]
                     [quantum.core.fn                         :as fn
                       :refer [#?@(:clj [<- fn-> fn->> f*n])]          ]
                     [quantum.core.logic                      :as logic
                       :refer [#?@(:clj [eq? fn-not fn-or fn-and whenf
                                         whenf*n whenc ifn condfc condpc
                                         coll-or]) nempty? nnil?]      ]
                     [quantum.core.macros                     :as macros
                       :refer [#?(:clj defnt)]                         ])
  #?(:cljs (:require-macros
                     [quantum.core.error                      :as err
                       :refer [throw-unless]                           ]
                     [quantum.core.log                        :as log  ]
                     [quantum.core.collections                :as coll
                       :refer [containsv? kmap popr popl]              ]
                     [quantum.core.fn                         :as fn
                       :refer [<- fn-> fn->> f*n]                      ]
                     [quantum.core.logic                      :as logic
                       :refer [eq? fn-not fn-or fn-and whenf whenf*n
                               whenc ifn condfc condpc coll-or]        ]
                     [quantum.core.macros                     :as macros
                       :refer [defnt]                                  ])))

; special-symbol? is a clojure thing

(def default-language :java)
(def ^:dynamic *lang* default-language)

(def whitespace? true)
(declare comma-splice)
(declare camel-lang?)

#?(:clj 
(defmacro mcall [msym & args]
  `(~msym :js (list '~msym ~@args))))

(defrecord ObjLangText [text])

(def demunge-class (f*n str/replace "$" "."))  ; For inner classes; TODO more than this is required  

(defn replace-specials [^String s-0 & [upper?]]
  (let [^Fn camelcase-if-needed
          (fn [s-n]
            (if (camel-lang?)
                (strf/camelcase s-n (if upper? nil :lower))
                s-n))
        ^Fn spear-case-if-needed
          (fn [s-n]
            (str/replace s-n "-" "_"))
        ^Fn apply-conventions
          (fn [s-n]
            (-> s-n
                (whenf (f*n str/ends-with? "?")
                  (fn->> popr (str "is-")))
                (whenf (f*n str/ends-with? "!")
                  (fn->  popr (str "X")))))
        s-f (if (containsv? s-0 "_") ; intentionally naming it as such
                s-0
                (-> s-0
                    apply-conventions
                    ;camelcase-if-needed
                    spear-case-if-needed
                    (str/replace-with
                      (map/om
                        "?" "_QMARK_"
                        "!" "_BANG_"
                        "-" "_"
                        ">" "_GT_"
                        "<" "_LT_"
                        ":" "_COLON_"
                        "*" "_STAR_"
                        "=" "eq"))))]
    (if (str/starts-with? s-0 ".-")
        (apply str ".-" (-> s-f coll/lrest coll/lrest))
        s-f)))

(def capitalize-fn? #{:cs})

(defn ^String fn-pos
  ([fn-str]
    (println "fn-pos" "fn-str" fn-str)
    (str fn-str "()"))
  ([fn-str & args]
    (println "fn-pos" "fn-str" fn-str "|" "args" args)
    (str fn-str "(" (apply comma-splice args) ")")))

; A bracketed form — a scope
(defrecord LangForm [first-str inner last-str])

(defn enc-class 
  {:in  '[1 2 3]
   :out "Vector"}
  [obj]
  (cond vector? "Vector"))

(declare obj-class)

; TODO insert in quantum.core.type
(def primitive? (partial (fn-not coll?)))

; TODO use quantum.core.type
(def primitives-map
  {(class (long 0)) "long"
   (class (int  0)) "int"
   (class "")       "String"})

(defn elem-class
  {:in  '[1 2 3]
   :out "long"}
  [obj]
  (-> obj seq first obj-class))

(defn obj-class
  {:in  '[1 2 3]
   :out "Vector<long>"}
  [obj]
  (if (primitive? obj)
      (get primitives-map (class obj))
      (str (enc-class obj) "<" (elem-class obj) ">")))

(declare eval-form)
(declare oeval)
(declare do-form)
(declare apply-do-form)

(def do-form-map
  {'let
    (fn [[special-sym bindings & body]]
      (let [def-list
             (->> bindings (apply array-map)
                  (reduce
                    (fn [ret sym sym-val]
                      (conj ret (list 'deflocal sym sym-val)))
                    (list))
                  reverse) ; to preserve order
            let-list (cons 'do (concat def-list body))]
        let-list))
   'for
     (fn [[special-sym [sub super :as bindings] & body]]
       (do-form ; to do-form the |let|
        ; TODO utilize "transientize" on this   
         `(~'let [~'result_ (~'transient! [])]
            (~'doseq [~sub ~super]
              (~'let [~'temp_ ~(cons 'do body)]
                (~'conj! ~(with-meta 'result_ {:tag 'Vec})
                  ~'temp_)))
            (~'persistent! ~'result_))))
   'for-into
     (fn [[special-sym into-coll [sub super :as bindings] & body]]
       (if (vector? into-coll)
           (do-form (apply list 'for bindings body))
           (do-form ; to do-form the |let|
             `(~'let [~'result_ ~into-coll]
                (~'doseq [~sub ~super]
                  (~'let [~'temp_ ~(cons 'do body)]
                    (~'conj! ~'result_ ~'temp_)))
                ~'result_))))
   'cond 
     (fn [[special-sym pred expr & expr-pairs]]
      (cond
        (= pred :else)
          expr
        (empty? expr-pairs)
          (do-form (list 'when pred expr))
        :else
          (list 'if pred
            expr
            (do-form (cons 'cond expr-pairs)))))
   'do
     (fn [body] body)})

(defn do-form
  "Normalizes to |do| form."
  [form]
  (log/pr :debug "CALCULATING DO FORM FOR" form)
  (let [default-do-fn
          (fn [body] (list 'do body))
        do-fn
          (logic/ifc form (fn-and seq? (fn-> first symbol?))
            (or (-> do-form-map (get (first form))) default-do-fn)
            default-do-fn)]
    (do-fn form)))

(def apply-do-form
  (fn->> (map (partial do-form))
         (map rest) ; to get rid of |do|
         coll/flatten-1  ; join |do|s
         (cons 'do)))


(defn str-repeat [n s]
  (->> (repeat n s) (apply str)))

(defn countif [pred coll]
  (->> coll (filter pred) count))

(defn line-height
  [s]
  (->> s (countif (eq? \newline))))

(defn vertical-spacing-from [prev-block]
  (if (empty? prev-block)
      ""
      (condfc (-> prev-block line-height)
        (eq? 0)    "\n"
        (f*n <= 2) "\n"
        :else      "\n\n")))

(declare special-syms) ; For use with the object-language |defmacro|


(defn add-return-statements
  "For conditional expressions. Instead of |(if pred expr-t expr-f)|,
   the expr becomes |(if pred (return expr-t) (return expr-f))|.
   Handles nested if-statements as well."
   {:todo "Dispatch on protocol"}
  [expr]
  (whenc expr anap/s-expr?
    (let [[spec-sym & exprs] expr
          needs-return-statement?
            (fn-and (fn-not anap/branching-expr?) (fn-not anap/throw-statement?))]
      (condp = spec-sym
        'when (let [[pred & exprs-t] exprs
                   _ (println "EXPRS-T" exprs-t)
                    body     (butlast exprs-t)
                    ret-expr (last    exprs-t)
                    _ (println (kmap ret-expr))
                    ret-expr-final (add-return-statements ret-expr)
                    _ (println (kmap ret-expr-final))]
                (concat (list spec-sym)
                        (list pred)
                        body
                        (list (whenf ret-expr-final needs-return-statement?
                                (partial list 'return)))))
        'if (let [[pred expr-t expr-f] exprs
                  expr-t-final (add-return-statements expr-t)
                  expr-f-final (add-return-statements expr-f)]
              (list spec-sym
                pred
                (whenf expr-t-final needs-return-statement?
                  (partial list 'return))
                (whenf expr-f-final needs-return-statement?
                  (partial list 'return))))
        'cond
          (->> expr (do-form) (add-return-statements))
        expr))))

(defn gen-return-statement
  {:in-types '{vertical-spacing number?}}
  [vertical-spacing body-in-do-form]
  (let [last-expr (last body-in-do-form)
        _ (log/pr :debug "ADDING RETURN STATEMENT TO:" last-expr)
        expr-str
          (if (-> last-expr anap/branching-expr?)
              (eval-form (add-return-statements last-expr))
              (str "return "
                (->> body-in-do-form last (eval-form)) ";" "\n"))]
    (str vertical-spacing expr-str)))

(def reserved-syms
  {:js   #{'try 'catch 'finally 'arguments 'this 'for 'if 'else 'class 'static 'function}
   :cs   #{'try 'catch 'finally 'params    'this 'for 'if 'else 'class 'static 'public 'private 'protected 'void}
   :java #{'try 'catch 'finally 'params    'this 'for 'if 'else 'class 'static 'public 'private 'protected 'void}})

(def scoped-syms
  {:js   #{'for 'for-into 'doseq 'if 'cond 'do 'try 'catch 'finally}
   :cs   #{'for 'for-into 'doseq 'if 'cond 'do 'try 'catch 'finally}
   :java #{'for 'for-into 'doseq 'if 'cond 'do 'try 'catch 'finally}})

(def dynamic-type
  {:cs  "var"
   :cpp "auto"})

; TODO for Java
(defn infer-type [] "Object")

(defn scope-if-needed [expr]
  (log/pr :debug "SCOPING" expr)
  (whenf expr seq?
    (whenf*n (fn-> first (in? (get scoped-syms *lang*)))
      (partial list 'scope))))

(defn infix [^String oper]
  (fn [[spec-sym & args]]
    (->> args (map (partial eval-form))
         (str/join (str " " oper " "))
         str/paren)))

(def special-syms
  (atom
    {'for
       (fn for-fn [[special-sym bindings & body :as form]]
         {:pre [(throw-unless (-> bindings count even?)
                (str/sp "'For' requires an even number of bindings. Supplied:" (count bindings)))]}
         (log/pr :debug "IN |FOR|")
         (->> form do-form eval-form))
     'for-into
       (fn for-into-fn [[special-sym into-coll bindings & body :as form]]
         (log/pr :debug "IN |FOR-INTO|")
         (->> form do-form eval-form))
     'defglobal
       (fn defglobal-fn [[special-sym sym form & args]]
         (eval-form (apply list 'def*      true sym form args)))
     'deflocal
       (fn deflocal-fn [[special-sym sym form & args]]
         (eval-form (apply list 'def*      false sym form args)))
     'def
       (fn def-fn [[special-sym sym form & args]]
         (eval-form (apply list 'defglobal sym form args)))
     'def*
       (fn def*-fn [[special-sym global? sym form & args]]
         {:pre [(throw-unless (nnil? form) (str/sp "Cannot bind var" (str/squote sym) "to nothing"))
                (-> args count (= 0))]}
         (when (-> reserved-syms (get *lang*) (contains? sym))
           (throw (->ex (str/sp "Symbol" (str/squote sym)
                          "is a reserved symbol and cannot be redefined."))))
         (let [_ (log/pr :debug "META FOR SYM" sym "IS" (meta sym))
               form-scoped (->> form (scope-if-needed))
               class- (or (-> sym meta :tag)
                          (get dynamic-type *lang*)
                          (infer-type))
               form-f
                 (condp = *lang* 
                   :js (if true
                           #_class-
                           #_(do (log/pr :debug "CREATING VAR WITH CLASS" class-)
                               (list 'var class- form-scoped))
                           form-scoped)
                   :cs form-scoped
                   :java form-scoped)
               visibility (if (-> sym meta :protected) "protected" "public")
               dynamicity (if (-> sym meta :dynamic) nil "static")
               prefix
                 (condpc = *lang*
                   :js "var"
                   (coll-or :java :cs)
                     (if global?
                         (str/sp visibility dynamicity class-)
                         class-))]
           (str/sp prefix (replace-specials (->name sym)) "="
                             (->> form-f eval-form util/scolon))))
     'declare
       (fn declare-fn [[special-sym sym & args]]
         {:pre [(throw-unless (-> args count (= 0))
                  (str/sp "'Declare' takes only one argument. Supplied:" (count args)))]}
         (condp = *lang*
           ; TODO throw on trying to def a special JS symbol
           :js (str/sp "var" (util/scolon (replace-specials (->name sym))))))
     'let
       (fn let-fn [[special-sym bindings & body :as form]]
         {:pre [(throw-unless (-> bindings count even?)
                (str/sp "'Let' requires an even number of arguments. Supplied:" (count bindings)))]}
         (log/pr :debug "IN |LET|")
         (->> form do-form eval-form))
     'quote
       (fn quote-fn [[special-sym & args]]
         (if (and (coll/single? args) (-> args first symbol?))
             (eval-form (first args))
             (throw (->ex "Quote not supported yet."))))
     'when
       (fn when-fn [[special-sym pred & args]]
         (let [
               standard #(let [pred-str (str/sp "if" (str/paren (eval-form pred)))]
                           (->> args (cons 'do) eval-form
                                (<- str "\n")
                                (util/bracket pred-str)))]
           (condp = *lang*
             :js   (standard)
             :cs   (standard)
             :java (standard)
             :cpp  (standard))))

     'if
       (fn if-fn [[special-sym pred expr-t expr-f & args]]
         {:pre [(throw-unless (-> args count (= 0))
                  (str/sp "|if| takes three arguments. Supplied extra:" args))]}
         (let [default-fn
                (fn []
                  (let [pred-str (str/sp "if" (str/paren (eval-form pred)))
                        ^Fn do-eval (fn->> do-form eval-form)
                        expr-t-str (->> expr-t do-eval (<- str "\n") (util/bracket pred-str))
                        expr-f-str (->> expr-f do-eval (<- str "\n") (util/bracket "else"))]
                    (str/sp expr-t-str expr-f-str)))]
           (condp = *lang*
             :js   (default-fn)
             :cs   (default-fn)
             :java (default-fn)
             :cpp  (default-fn))))
  
     'cond ; TODO need to add return statement capability
       (fn cond-fn [[special-sym pred expr & expr-pairs]]
         {:pre [(throw-unless (-> expr-pairs count even?)
                  (str/sp "|cond| takes an even number of arguments. Supplied:"
                    (concat (list pred expr) expr-pairs)))]}
         (->> (concat (list special-sym pred expr) expr-pairs) do-form eval-form))
     
     'try
       (fn try-fn [[special-sym & args]]
         (let [default-fn
                (fn []
                  (let [catch?       (fn-> first (anap/symbol-eq? 'catch)) 
                        finally?     (fn-> first (anap/symbol-eq? 'finally))
                        try-body     (->> args (remove (fn-and seq? (fn-or catch? finally?))))
                        catches      (->> args (filter (fn-and seq? catch?)))
                        finallys     (->> args (filter (fn-and seq? finally?)))
                        ^Fn do-eval  (fn->> apply-do-form eval-form)
                        try-str      (->> try-body do-eval (<- str "\n") (util/bracket "try"))
                        catch-str    (when (nempty? catches)
                                       (->> catches first rest rest do-eval (<- str "\n") (util/bracket (str/sp "catch" (str/paren (-> catches first second str demunge-class))))))
                        finally-str  (when (nempty? finallys)
                                       (->> finallys first rest do-eval (<- str "\n") (util/bracket "finally")))]
                    (str/sp try-str catch-str finally-str)))]
           (condp = *lang*
             :js   (default-fn)
             :cs   (default-fn)
             :java (default-fn)
             :cpp  (default-fn))))

     'doseq
       (fn doseq-fn [[sym & form]]
         (log/pr :debug "In eval |doseq|")
         (let [[bindings & inner] form
               ; only one binding currently supported
               [sub super & other-bindings] bindings
               ^String doseq-token
                 (condp = *lang*
                   :cs "foreach"
                   :js      "for")
               ^String in-token
                 (condp = *lang*
                   :java ":"
                   :cs   "in"
                   :js   "in"
                   (throw (->ex (str/sp "No doseq in-token recognized for language:" *lang*))))
               ; (elem-class super)
               var-decl (or (anap/metaclass sub)
                            (-> sub meta :t)
                            (get dynamic-type *lang*)
                            (infer-type))
               doseq-str (str/sp var-decl (eval-form sub) in-token (eval-form super))
               ^String inner-str (->> inner (cons 'do) (eval-form))
               _ (println "BODY STRING IN |DOSEQ|" (str/squote inner-str))]
           (println "BRACKETED" (str/squote (util/bracket (str/sp doseq-token (str/paren doseq-str))
             inner-str)))
           (util/bracket (str/sp doseq-token (str/paren doseq-str))
             inner-str)))
     'do
       (fn do-fn [[spec-sym & body]]
         (log/pr :debug "In eval |do|")
         (let [prev-block (atom nil)]
           (->> body
                (map
                  (fn [block-0]
                    (let [^String spacing
                            (vertical-spacing-from @prev-block)
                          ^String block-evaled
                            (eval-form block-0)
                          _ (println "SPACING" (str/squote spacing))
                          _ (println "BLOCK EVALED IN DO" block-evaled)
                          ^String block
                            (->> block-evaled
                                 (<- whenf (fn-and nempty?
                                                   (fn-not (f*n str/ends-with? "}")))
                                     util/scolon) ; Could be a macro, in which case it wouldn't show up in the .js file
                                 (<- whenf nempty? (partial str spacing)))]
                      (when (nempty? block) (reset! prev-block block))
                      block)))
                (apply str))))
     'fn        (fn fn-fn     [[spec-sym unk & body]] (eval-form (apply list 'lambda    unk body)))
     'lambda    (fn lambda-fn [[spec-sym unk & body]] (eval-form (apply list 'fn* true  unk body)))
     'extern-fn (fn extern-fn [[spec-sym unk & body]] (eval-form (apply list 'fn* false unk body)))
     'fn*
       (fn fn*-fn [[spec-sym lambda? unk & body]]
         (let [sym (when (symbol? unk) unk)
               ret-type (or (anap/metaclass sym) (-> sym meta :t) "Object")
               arglist (if (symbol? unk) (first body) unk )
               body    (if (symbol? unk) (rest  body) body)
               _ (log/pr :debug "CREATING FN:" arglist body)
               arglist-f
                 (whenf arglist anap/variadic-arglist?
                   (fn-> popr popr)) ; to remove variadic args
               fn-arglist
                 (->> arglist-f
                      (map (fn [sym]
                             (let [evaled (eval-form sym)]
                               (condp = *lang*
                                 :js evaled
                                 (str/sp (or (anap/metaclass sym) "Object") evaled)))
                               ))
                      (str/join ", ")
                      str/paren)
               fn-header
                 (condp = *lang*
                   :js   (str/sp "function" fn-arglist)
                   :cs   (if lambda? (str/sp fn-arglist "=>") fn-arglist)
                   :java (if lambda? (str/sp fn-arglist "->") fn-arglist))
               ; ((let [])) => (do (def _) (def _) (func! arg) arg)
               body-in-do-form-0
                 (apply-do-form body)
               variadic-def-statement
                 (when (anap/variadic-arglist? arglist)
                   (list 'def (last arglist) 'arguments))
               _ (log/pr :debug "FN VARIADIC DEF STATEMENT FROM" arglist ":" variadic-def-statement)
               ; Add, e.g. "var myArgs = arguments;"
               body-in-do-form
                 (if variadic-def-statement
                     (concat (list 'do variadic-def-statement) (rest body-in-do-form-0))
                     body-in-do-form-0)
               _ (log/pr :debug "FN BODY IN DO FORM" body-in-do-form)
               fn-body
                 (if (-> spec-sym meta :class)
                     ; JavaScript function-classes shouldn't have return statements  
                     (do (log/pr :debug "CREATING JS FUNCTION-CLASS")
                         (eval-form body-in-do-form))
                     (let [pre-ret-statement (->> body-in-do-form butlast)
                           ; (do (def _) (def _) (func! arg))
                           pre-ret-statement-str
                             (if (= body-in-do-form-0 '(do))
                                 nil
                                 (eval-form pre-ret-statement))
                           _ (log/pr :debug "PRE-RET STATEMENT" (str/squote pre-ret-statement-str))
                           vertical-spacing
                             (vertical-spacing-from pre-ret-statement-str)
                           ret-statement-str
                             (if (= ret-type "void")
                                 (util/semicoloned (str vertical-spacing (eval-form (last body-in-do-form))))
                                 (gen-return-statement vertical-spacing body-in-do-form))
                             
                           _ (log/pr :debug "RET STATEMENT" (str/squote ret-statement-str))]
                       (str pre-ret-statement-str ret-statement-str)))]
           (util/bracket fn-header fn-body)))
     'defn 
       (fn defn-fn [[spec-sym sym bindings & form]]
         (condpc = *lang*
           :js (eval-form
                 (list 'def sym
                   (apply list #_(merge-meta 'extern-fn sym) 'extern-fn sym bindings form)))
           (coll-or :cs :java)
             (let [visibility (if (-> sym meta :protected) "protected" "public")
                       dynamicity (if (-> sym meta :dynamic) nil "static")]
                   (str/sp visibility dynamicity (or (anap/metaclass sym) (-> sym meta :t) "Object")
                     (eval-form sym)
                     (eval-form (apply list 'extern-fn sym bindings form))))))
     '.
       (fn dot-fn [[spec-sym & form]]
         (let [[caller-0 method & args] form
                callee (replace-specials (str "." method))
                caller (eval-form caller-0)]
           (if (str/starts-with? callee ".-") ; Field
               (str caller "." (dropl 2 callee))
               (apply fn-pos (str caller callee) args))))
     'new
       (fn new-fn [[spec-sym & [class- & args]]]
         (str/paren (str/sp "new" (apply fn-pos (-> class- str demunge-class) args))))
     'Object.
       (fn object-fn [[spec-sym arg-0 & args]]
         (condpc = *lang*
           ; Create JS object {...}
           :js (let [arg-ct (-> args count inc)
                     _ (apply println "Creating JS object with" arg-ct "args:" arg-0 args)
                     obj-map
                       (cond
                         (even? arg-ct)
                           (apply sorted-map arg-0 args) ; sorts the keys
                         (and (= 1 arg-ct) (map? arg-0))
                           arg-0
                         (and (= 1 arg-ct) (nil? arg-0))
                           {}
                         :else
                           (throw (->ex "Invalid number of arguments to JavaScript object.")))
                     longest-key-length
                       (->> obj-map keys
                            (map (fn->> name symbol eval-form count))
                            (<- ifn empty? (constantly 0) num/greatest))
                     ^String obj-map-contents
                       (let [prev-line (atom nil)]
                         (reduce
                           (fn [ret k v]
                             (let [^String k-f         (->> k name symbol eval-form)
                                   ^String key-padding (-> longest-key-length (- (count k-f)) (str-repeat \space))
                                   ^String v-line      (eval-form v)
                                   ^String kv-line     (str k-f ": " key-padding v-line)
                                   ^String spacing     (if (-> @prev-line line-height (> 2)) "\n\n" "\n")]
                               (reset! prev-line v-line)
                               (if (empty? ret)
                                   kv-line
                                   (str ret "," spacing kv-line))))
                           ""
                           obj-map))
                     ^String object-str
                       (condfc obj-map
                         empty? "null"
                         coll/single?
                           (str/sp "{" obj-map-contents "}")
                         :else
                           (str "{\n" (util/indent obj-map-contents) "\n}"))]
                 object-str)
         (coll-or :java :cs)
           (eval-form (apply list 'new 'Object args))))
     'return
       (fn return-fn [[spec-sym ret-obj]]
         (log/pr :debug "EVALING RETURN STATEMENT" ret-obj)
         (let [in-do-form (->> ret-obj (do-form))
              _ (println "IN DO FORM" in-do-form)
               body       (-> in-do-form butlast)
               _ (println "BODY" body)
               body-str   (eval-form body)
               _ (println "BODY-STR" body-str)
               ret-form   (-> in-do-form last)
               _ (println "RET-FORM" ret-form)
               spacing    (if (empty? body-str) "" "\n")
               ret-str    (str spacing "return " (eval-form ret-form) ";")]
           (str body-str ret-str)))
     'scope
       (fn scope-fn [[spec-sym & body]]
         (str (eval-form (concat '(fn []) body)) "()"))
     'vector
       (fn vector-fn [[spec-sym & args]]
         (eval-form (apply list 'C/vector args)))
     ; INFIX OPERATORS
     '+ (infix "+")
     '- (infix "-")
     '* (infix "*")
     '/ (infix "/")
     '== (infix "==")
     'set! (infix "=")
     ; TO BE MOVED TO ANALYZER
     'str
       (fn str-fn [[spec-sym & args]]
         (oeval (list '.ToString (first args))))
     'nstr ; native string
       (fn nstr-fn [[spec-sym & args]]
         (condp = *lang*
           ; same as infix pluses
           :js      (eval-form (cons '+ args))
           :cs (eval-form (cons '+ args))
           :java    (eval-form (cons '+ args))))
     'clojure.core/deref
       (fn deref-fn [[spec-sym & args]]
         (eval-form (cons 'deref args)))
     'defmacro
       (fn defmacro-fn [[spec-sym sym arglist & args]]
         (log/pr :debug "IN DEFMACRO")
         (let [arglist-base
                 [(-> ['macro-sym] (into arglist))]
               var-args-name '_args-extra_
               arglist-f
                 (if (anap/variadic-arglist? arglist)
                     arglist-base
                     (-> arglist-base
                         (update 0 (f*n conj '& var-args-name))))
               sym-munged
                 (-> sym name (str "-") symbol)
               _ (println "IN MACRO")
               macro-f
                 (concat
                   (list 'fn sym-munged arglist-f
                     {:pre [(list 'throw-unless ('-> var-args-name 'empty?)
                              (list 'str "Too many args to |" 'macro-sym "|."))]})
                   args)]
           (log/ppr :debug "ADDING MACRO:" macro-f)
           (swap! special-syms assoc sym
             (eval macro-f))
           nil))
     'oeval ; object-level eval
       (fn oeval-fn [[spec-sym form]] (eval-form form))
     ; Defines a macro alias.
     'def-macro-alias
       (fn def-macro-alias-fn [[spec-sym post-sym pre-sym]]
         (eval-form
           (list 'defmacro post-sym '[coll & args]
             (list 'oeval
               (list 'list (quote 'apply) (list 'quote pre-sym)
                 'coll 'args)))))}))

(defn special-sym? [sym] (in? sym @special-syms))

(defn camel-lang? []
  (condp = *lang*
    :java true
    :js   true
    :cs   true
    :cpp  false))

(defn ^String eval-special-form [[special-sym :as form]]
  (let [special-form-fn (-> @special-syms (get special-sym))]
    (log/pr :debug "EVALING SPECIAL FORM" form)
    (special-form-fn form)))

(defnt eval-form*
  ([^map? obj]
    (log/pr :debug "IN MAP")
    (condp = *lang*
      :js   (eval-form (list 'Object. obj))
      (eval-form (cons 'map! (apply concat obj)))))
  ([^listy? obj]
    (let [;[first-0 & rest-0 :as form] obj ; TODO destructuring in |defnt| not yet supported
          form obj
          first-0 (first obj)
          rest-0  (rest obj)]
      (log/pr :debug "IN LIST") 
      (if (-> first-0 symbol?)
          (cond
            (-> first-0 special-sym?)
              (eval-special-form form)
            (-> first-0 name (str/starts-with? "."))
              (let [caller    (-> rest-0 first)
                    un-dotted (-> first-0 name popl symbol)
                    call-form (apply list '. caller un-dotted (rest rest-0))]
                (eval-form call-form))
            (and (-> first-0 name (str/ends-with? "."))
                 (-> first-0 (not= 'Object.)))
              (let [un-dotted-class (-> first-0 name popr symbol)]
                (eval-form (apply list 'new un-dotted-class rest-0)))
            :else
              (apply fn-pos (eval-form first-0) rest-0))
          (apply fn-pos (eval-form 'list!) first-0 rest-0))))
   ([^vec? obj]
     (log/pr :debug "IN VEC")
     (condp = *lang*
       :js   (apply fn-pos (eval-form 'pvec) obj)
       (apply fn-pos (eval-form 'C/vector) obj)))
   ([^set? obj]
     (log/pr :debug "IN SET")
     (apply fn-pos (eval-form 'hash-set!) obj))
   ([^symbol? obj]
     (log/pr :debug "IN SYM WITH" (str obj))
     (if (anap/qualified? obj)
         (do (log/pr :debug "QUALIFIED SYMBOL:" (str obj))
             (if (->> obj str (filter (eq? \/)) count (<- > 1))
               (throw (->ex (str "Qualified symbol" (-> obj str str/squote) "cannot have more than one namespace.")))
               (->> obj str (<- str/replace #"\/" ".") symbol eval-form)))
         (->> obj str replace-specials)))
   ([^string? obj] (str \" obj \"))
   ([^char? obj] (str \' obj \'))
   ([^keyword? obj]
     (log/pr :debug "IN KEYWORD")
     (->> obj name replace-specials eval-form))
   ([^number? obj] (str obj))
   ([:else obj]
     (if (nil? obj)
         "null"
         (throw (->ex (class obj) ["Unrecognized form:" obj])))))

(defn ^String eval-form [obj]
  (println "Evaluating" obj "class" (class obj))
  (-> obj eval-form*))
(def oeval eval-form)

; TODO logger with context awareness
(defn ^String comma-splice
  ([arg]
    (println "Comma-splice:" "arg" arg "class" (class arg))
    (eval-form arg))
  ([arg & args]
    (println "Comma-splice:" "args" (cons arg args) "rest class" (class args) "first class" (-> arg class))
    (->> (cons arg args)
         (map (partial eval-form))
         (interpose (if whitespace? ", " ","))
         (apply str))))


