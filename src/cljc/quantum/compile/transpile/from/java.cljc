(ns quantum.compile.transpile.from.java
  (:refer-clojure :exclude [some? if-let])
  (:require
    [quantum.core.analyze.clojure.predicates :as anap ]
    [quantum.core.analyze.clojure.core       :as ana  ]
    [quantum.core.string                     :as str  ]
    [quantum.core.collections.zip            :as zip]
    [quantum.core.collections                :as coll
      :refer        [postwalk prewalk zip-prewalk take-until update-last
                     #?@(:clj [containsv? popl popr kmap])]
      :refer-macros [          containsv? popl popr kmap]]
    [quantum.core.convert                    :as conv
      :refer [->name]                                 ]
    [quantum.core.convert.primitive          :as pconv]
    [quantum.core.error                      :as err
      :refer [->ex]                                   ]
    [quantum.core.macros                     :as macros
      :refer [#?(:clj defnt)]                         ]
    [quantum.core.fn                         :as fn
      :refer        [#?@(:clj [fn-> fn->> fn1 compr <-])]
      :refer-macros [          fn-> fn->> fn1 compr <-]]
    [quantum.core.logic                      :as logic
      :refer        [nnil? some? nempty?
                     #?@(:clj [eq? fn-or fn-and whenf whenf1 ifn1 condf1 if-let cond-let])]
      :refer-macros [          eq? fn-or fn-and whenf whenf1 ifn1 condf1 if-let cond-let]]
    [quantum.core.type.core                  :as tcore]
    [quantum.core.match                      :as m
      :refer        [#?@(:clj [re-match re-match* re-match-whole*])]])
#?(:clj
    (:import
      com.github.javaparser.JavaParser
     [com.github.javaparser.ast CompilationUnit]
     [com.github.javaparser.ast.body
       ClassOrInterfaceDeclaration InitializerDeclaration
       ConstructorDeclaration
       MethodDeclaration FieldDeclaration FieldDeclaration
       ModifierSet
       Parameter MultiTypeParameter
       VariableDeclarator VariableDeclaratorId]
     [com.github.javaparser.ast.comments JavadocComment]
     [com.github.javaparser.ast.stmt
       ExplicitConstructorInvocationStmt
       BlockStmt Statement ExpressionStmt
       TryStmt ThrowStmt CatchClause
       BreakStmt ContinueStmt
       WhileStmt DoStmt
       ForStmt ForeachStmt
       ReturnStmt
       IfStmt
       SwitchStmt SwitchEntryStmt]
     [com.github.javaparser.ast.expr
       ObjectCreationExpr NameExpr VariableDeclarationExpr
       SuperExpr
       ArrayAccessExpr ArrayCreationExpr ArrayInitializerExpr
       ConditionalExpr
       CastExpr
       LambdaExpr
       NullLiteralExpr AssignExpr MethodCallExpr
       InstanceOfExpr
       ClassExpr ThisExpr
       FieldAccessExpr
       EnclosedExpr
       UnaryExpr UnaryExpr$Operator
       BinaryExpr BinaryExpr$Operator
       BooleanLiteralExpr StringLiteralExpr IntegerLiteralExpr LongLiteralExpr]
     [com.github.javaparser.ast.type
       ReferenceType ClassOrInterfaceType
       PrimitiveType PrimitiveType$Primitive])))

; JAVA->CLOJURE

; TODO fix double escaping
; http://javaparser.github.io/javaparser/javadoc-current/japa/parser/ast/expr/ConditionalExpr.html

(def context (atom [nil nil])) ; We won't do parallel parsing for now

(defn add-context [x]
  (swap! context assoc 0 (get @context 1))
  (swap! context assoc 1 x))

(declare parse*) ; Previously declared |parse|; can't do this

(defn clean-javadoc [s]
  (-> s (str/replace #"\n*\s*\*\s" "\n"))) ; TODO add as ^:doc

(defn type-hint* [x]
  (let [hint-0 (str x)
        hint (if (= hint-0 "byte[]") ; TODO elaborate on this
                 "\"[B\""
                 hint-0)]
    (->> hint (str "^") symbol)))

(defn do-each [x] (apply list 'do (->> x (map parse*))))

(def implicit-do
  (ifn1 (fn-> first (= 'do))
       rest
       list))

(def remove-do-when-possible
  (ifn1 (fn-and seq?
                (fn-> first (= 'do))
                (fn-> count (<= 2)))
        rest
        list))

(def operators
  {"assign"         'set!
   "equals"         '=
   "notEquals"      'not=
   "lessEquals"     '<=
   "greaterEquals"  '>=
   "and"            'and
   "not"            'not
   "plus"           '+
   "minus"          '-
   "divide"         '/
   "times"          '*
   "greater"        '>
   "less"           '<
   "lShift"         '<<
   "rSignedShift"   '>>
   "rUnsignedShift" '>>>
   "remainder"      'rem
   "negative"       '-
   "preIncrement"   'inc! ; TODO is it wise to equate these two?
   "posIncrement"   'inc!
   "posDecrement"   'dec!
   "binAnd"         '&
   "inverse"        'bit-not
   "or"             '|})

#?(:clj (def string-literal? (partial instance? StringLiteralExpr)))

#?(:clj
(defn parse-modifiers [mods]
  (let [all-mods (->> mods
                      (ModifierSet/getAccessSpecifier)
                      str clojure.string/lower-case
                      (str "^:"))]
    (when-not (some? (eq? all-mods)
                      #{"^:default"
                        "^:public"})
      (symbol all-mods)))))

#?(:clj
(defn parse-operator [x]
  (if-let [oper (->> x str (get operators))]
    oper
    (throw (->ex nil (str "Operator not found") {:operator x})))))

#?(:clj
(defn parse-conditional [x]
  (let [pred (-> x (.getCondition) parse*)
        raw-then (condp instance? x
                   ConditionalExpr (.getThenExpr ^ConditionalExpr x)
                   IfStmt          (.getThenStmt ^IfStmt x)
                   (throw (->ex nil "Conditional exception" nil)))
        then (-> raw-then parse* remove-do-when-possible)]
    (if-let [else (condp instance? x
                    ConditionalExpr (.getElseExpr ^ConditionalExpr x)
                    IfStmt          (.getElseStmt ^IfStmt x)
                    (throw (->ex nil "Conditional exception" nil)))]
      (concat (list 'if) (list pred) then (-> else parse* remove-do-when-possible))
      (apply list 'when pred then)))))

#?(:clj
(defnt get-inner
  ([^MethodDeclaration      x] (.getBody  x))
  ([^ConstructorDeclaration x] (.getBlock x))))

(def dep-primitive->clojure-primitive
  {'Byte 'byte
   'Int  'int})

#?(:clj
(defnt parse
  "Java to Clojure."
  ([^string? x]
    (-> x conv/->input-stream JavaParser/parse parse*))
  ([#{java.io.File java.io.InputStream} x]
    (-> x JavaParser/parse parse*))
  ([^CompilationUnit x]
    (->> x (.getTypes) (mapv parse*)))
  ; TYPE
  ([^ReferenceType x]
     ;.getArrayCount missing
    (-> x .getType parse*))
  ([^PrimitiveType x]
    (->> x .getType str symbol
         (get dep-primitive->clojure-primitive)))
  ([^ClassOrInterfaceType x]
    ;.getScope
    (-> x .getName
          (whenf (fn1 containsv? "<") ; Parameterized class
            (fn->> (take-until "<")))
          symbol))
  ; BODY
  ([^ClassOrInterfaceDeclaration x]
    (-> x (.getMembers) do-each))
  ([^FieldDeclaration x]
    (cons 'do
          (coll/lfor [v (.getVariables x)]
            (apply list 'def (-> x .getModifiers parse-modifiers)
              (-> x .getType type-hint*)
              (-> v parse*)))))
  ([^InitializerDeclaration x]
    (-> x .getBlock parse*))
  ([#{MethodDeclaration
     ConstructorDeclaration} x]
     ; getTypeParameters
    (let [pre {:modifier (-> x .getModifiers parse-modifiers)
               :doc      (-> x (.getComment) str clean-javadoc)
               :sym      (-> x .getName symbol)
               :constructor? (when (instance? ConstructorDeclaration x) (symbol "^:constructor"))}
          arglist (->> x (.getParameters)
                       (map parse*)
                       (apply concat)
                       (into [])
                       list)
          pres (->> [(:modifier pre) (:constructor? pre) (:sym pre) (:doc pre)]
                    (remove (fn-or nil? (fn-and string? empty?))))
          body (when-let [inner (get-inner x)]
                  (->> inner parse* implicit-do))]
      (concat (list 'defnt) pres
           arglist body)))
  ([^VariableDeclarator x]
    [(-> x .getId parse*) (when-let [init (.getInit x)] (parse* init))])
  ([^VariableDeclaratorId x]
    (-> x str symbol))
  ([^Parameter x]
    [(-> x .getType type-hint*)
     (-> x .getId parse*)])
  ([^MultiTypeParameter x]
    (concat (->> x .getTypes (map parse*))
            (list (-> x .getId parse*))))
  ; STATEMENTS
  ([^BlockStmt x]
    (-> x (.getStmts) do-each))
  ([^ExpressionStmt x]
    (-> x .getExpression parse*))
  ([^BreakStmt x]
    (if-let [id (.getId x)]
      (list 'break (symbol id))
      (list 'break)))
  ([^ContinueStmt x]
    (if-let [id (.getId x)]
      (list 'continue (symbol id))
      (list 'continue)))
  ([^TryStmt x]
    ;(.getResources x)
    (->> (list
           (cons 'try
             (-> x .getTryBlock parse* implicit-do))
             (->> (.getCatchs x)
                  (map parse*)
                  (map (partial apply list 'catch )))
             (when-let [fb (.getFinallyBlock x)]
               (list (cons 'finally (-> fb parse* implicit-do)))))
         (apply concat)))
  ([^ReturnStmt x]
    (if-let [expr (.getExpr x)]
      (list 'return (parse* expr))
      (list 'return)))
  ([^ThrowStmt x]
    (list 'throw (-> x .getExpr parse*)))
  ([^CatchClause x]
    (concat (-> x .getExcept parse*) (-> x .getCatchBlock parse* implicit-do)))
  ([^IfStmt x]
    (parse-conditional x))
  ([^SwitchStmt x]
    (apply concat (list 'case (parse* (.getSelector x)))
      (->> x .getEntries (map parse*))))
  ([^SwitchEntryStmt x]
    (let [label (-> x .getLabel (whenf nnil? parse*))
          stmts (->> x .getStmts (map parse*) (cons 'do))]
    (if label
        [label stmts]
        [stmts])))
  ([^WhileStmt x]
    `(~'while ~(-> x .getCondition parse*)
       ~@(-> x .getBody parse* implicit-do)))
  ([^DoStmt x]
    `(~'loop []
      ~@(-> x .getBody parse* implicit-do)
      (~'when ~(-> x .getCondition parse*) (~'recur))))
  ([^ForStmt x]
    `(~'ifor
      ~(->> x .getInit    (mapv parse*))
      ~(->> x .getCompare parse*)
      ~(->> x .getUpdate  (map parse*) (cons 'do))
      ~@(->> x .getBody    parse* implicit-do)))
  ([^ForeachStmt x]
    (apply list 'doseq
      (conj (-> x .getVariable parse* second popr) (-> x .getIterable parse*))
      (-> x .getBody parse* implicit-do)))
  ([^ExplicitConstructorInvocationStmt x]
    (throw (->ex nil "unsupported" {:x x}))
    ["EXPLICIT CONSTRUCTOR" (str x)])
  ; EXPRESSIONS
  ([^NameExpr x]
    (-> x .getName symbol))
  ([^AssignExpr x]
    (let [right  (-> x .getValue parse*)
          oper-0 (->> x .getOperator parse-operator)
          oper-1 (when (not= oper-0 'set!) oper-0)
          oper-f (if (and (= oper-1 '+) (anap/str-expression? right))
                     'str
                     oper-1)
          target (-> x .getTarget parse*)]
      (if oper-f
          `(~'swap! ~target ~oper-f ~right)
          `(~'set!  ~target ~right))))
  ([^ThisExpr x]
    'this)
  ([^SuperExpr x]
    (throw (->ex nil "unsupported" {:x x}))
    ["SUPER" (str x)])
  ([^EnclosedExpr x] ; Parenthesis-enclosed
    (list 'do (-> x .getInner parse*)))
  ([^NullLiteralExpr x]
    'nil)
  ([^InstanceOfExpr x]
    (list 'instance?
      (-> x .getType parse*)
      (-> x .getExpr parse*)))
  ([^FieldAccessExpr x]
    (list (->> x .getField (str ".") symbol)
          (->> x .getScope  parse*)))
  ([^ArrayCreationExpr x]
    (assert (not (and (->> x .getInitializer)
                      (->> x .getDimensions nempty?))))
    (if (->> x .getInitializer)
        `(~'array-of ~(->> x .getType parse*)
                     ~@(->> x .getInitializer parse*))
        `(~'->multi-array ~(->> x .getType parse*)
                          ~(->> x .getDimensions (mapv parse*))))
    )
  ([^ArrayAccessExpr x]
    (list 'aget (-> x .getName parse*) (-> x .getIndex parse* str/val)))
  ([^ArrayInitializerExpr x]
    (->> x .getValues (map parse*)))
  ([^ClassExpr x]
    (-> x .getType parse*))
  ([^CastExpr x]
    (let [type (-> x .getType parse*)
          expr (-> x .getExpr parse*)]
      (if (tcore/primitive? type)
          (let [cast-fn (symbol (str "->" (->name type)))]
            (list cast-fn expr))
          (list 'cast type expr))))
  ([^ConditionalExpr x]
    (parse-conditional x))
  ([^MethodCallExpr x]
    (let [args (->> x .getArgs (map parse*))]
       (if (.getScope x)
           (apply list
             (->> x (.getName) (str ".") symbol)
             (->> x .getScope parse*) args)
           (apply list
             (->> x (.getName) symbol)
             args))))
  ([^LambdaExpr x]
    ["LAMBDA" (str x)])
  ([^UnaryExpr x]
    (let [oper (-> x .getOperator parse*)
          expr (-> x .getExpr parse*)]
      (if (and (number? expr)
               (= oper '-))
          (- expr)
          (list oper expr))))
  ([^UnaryExpr$Operator x]
    (parse-operator x))
  ([^StringLiteralExpr x]
    (-> x .getValue))
  ([^LongLiteralExpr x]
    `(~'long ~(->> x .getValue popr pconv/->long)))
  ([^BooleanLiteralExpr x]
    (-> x .getValue))
  ([^IntegerLiteralExpr x]
    (-> x .getValue str/val))
  ([^BinaryExpr x]
    (let [left   (-> x .getLeft     parse*)
          right  (-> x .getRight    parse*)
          oper-0 (-> x .getOperator parse*)
          oper (if (and (= oper-0 '+)
                        (or (anap/string-concatable? left)
                            (anap/string-concatable? right)))
                   'str
                   oper-0)]
      (list oper left right)))
  ([^BinaryExpr$Operator x]
    (parse-operator x))
  ([^VariableDeclarationExpr x]
    ; TODO if type is primitive, cast it to it; can't hint it
    (let [t (-> x .getType type-hint*)]
      (list 'let (->> (.getVars x)
                      (map parse*)
                      (map (partial into [t]))
                      (apply concat)
                      (into [])))))
  ([^ObjectCreationExpr x]
    (apply list (-> x .getType (str ".") symbol)
      (->> x .getArgs (mapv parse*))))))

#?(:clj (defn parse* [x] (add-context x) (parse x)))

(def log1 (atom []))
(def print-log #(swap! log1 conj (apply println-str %&)))

(defn clean-lets [form]
  (prewalk
    (whenf1
      (fn-and seq? ; lone `let` statements like `(let [^int abc (+ (* abcde 2) 1)])`
        (fn-> first (= 'let))
        (fn-> count (= 2)))
      (fn->> ))
    form))

#?(:clj
(defn clean
  "Fixes funky imperative Clojure code to be more idiomatic."
  [x]
  (->> x
       (postwalk ; Start from bottom. Essential for granular things
         (condf1
           (fn-and seq?
             (fn-> first (= '.println))
             (fn-> second seq?)
             (fn-> second second (= 'System)))
           (fn->> rest rest (cons 'println))

           (eq? '.equals)
           (constantly '=)

           identity))
       clean-lets
       (prewalk ; Start from the top, not the bottom. Essential for cond-folding
         (condf1
           anap/cond-foldable?
           identity #_cond-fold

           identity))
       #_(postwalk ; TODO uncomment this; just need to fix |conditional-branches|
         (compr
           (condf1
             (fn-and anap/conditional-statement?
               (fn->> ana/conditional-branches (every? anap/return-statement?))) ; Have to do this in separate postwalk because cond-fold affects return statements
             (fn [x] (list 'return (->> x (ana/map-conditional-branches (fn1 second)))))

             identity)
           #_join-lets))
       (postwalk
         (whenf1
           ; Elide superfluous `do`s
           (fn-and anap/do-statement? (fn-> count (= 2)))
           (fn1 second)))
       (postwalk
         ; Replace +1 and -1 with inc* and dec*
         ; =0 with zero?
         ; >0 with pos?
         ; *2 with >> 1
         (condf1
          ; Elide superfluous return statements
           (fn-and anap/function-statement?
             (fn-> last anap/return-statement?))
           (fn1 update-last (fn1 second))
           anap/sym-call?
           (fn [x] (cond-let
                     [{[form] :form [oper] :oper}
                      (re-match-whole* x
                        (& (m/as :oper (| '+ '-))
                           (| (& 1 (m/as :form _))
                              (& (m/as :form _) 1))))]
                     (list ('{+ inc* - dec*} oper) form)
                     [{[form] :form}
                      (re-match-whole* x
                        (& '=
                           (| (& 0 (m/as :form _))
                              (& (m/as :form _) 0))))]
                     (list 'zero? form)
                     [{[form] :form}
                      (re-match-whole* x
                        (& '> (& (m/as :form _) 0)))]
                     (list 'pos? form)
                     [{[form] :form}
                      (re-match-whole* x
                        (& '*
                           (| (& 2 (m/as :form _))
                              (& (m/as :form _) 2))))]
                     (list '>> form 1)
                     x))
           identity))
       ; TODO combine adjacent lone lets
       ; TODO extend lets to surround subsequent siblings
       #_(zip-prewalk
         (ifn1))
       ; Fix docstrings
       ; TODO use seqxpr for this (?)
       (zip-prewalk
         (ifn1
           (fn-> zip/node anap/defnt-statement?)
           (fn [form]
             (zip/node
               (if-let [arglist* (->> form zip/down (zip/right-until vector?))
                        docstr*  (->> arglist* (zip/left-until string?))
                        docstr   (zip/node docstr*)]
                 ; We know that meta map won't be there
                 (let [docstr' (-> docstr (str/split #"\v") popl popl popr)
                       meta-f {:doc docstr'}]
                   (->> docstr* zip/dissoc ; moves left
                        (zip/right-until vector?) ; arglist
                        (zip/insert-left meta-f)
                        zip/up))
                 form)))
           zip/node))
       first
       (map (ifn1 (fn-and seq? (fn-> first (= 'do)) (fn-> count (= 2))) rest list))
       (apply concat))))

