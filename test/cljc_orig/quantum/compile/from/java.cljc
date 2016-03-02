(ns quantum.compile.from.java
  (:require-quantum [:lib])
  (:require
    #?@(:clj ([quantum.core.analyze.clojure.predicates :as pred :refer :all]
              [quantum.core.analyze.clojure.core       :as ana])))
  (:import
    #?@(:clj (com.github.javaparser.JavaParser
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
                WhileStmt
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
                BooleanLiteralExpr StringLiteralExpr IntegerLiteralExpr]
              [com.github.javaparser.ast.type
                ReferenceType ClassOrInterfaceType
                PrimitiveType PrimitiveType$Primitive]))))

; JAVA->CLOJURE

; TODO fix double escaping
; http://javaparser.github.io/javaparser/javadoc-current/japa/parser/ast/expr/ConditionalExpr.html

(def context (atom [nil nil])) ; We won't do parallel parsing for now

(defn add-context [x]
  (swap! context assoc 0 (get @context 1))
  (swap! context assoc 1 x))

(declare parse*) ; Previously declared |parse|; can't do this

(defn clean-javadoc [s]
  (-> s (clojure.string/replace #"\n*\s*\*\s" "\n"))) ; TODO add as ^:doc

(defn type-hint* [x]
  (let [hint-0 (str x)
        hint (if (= hint-0 "byte[]") ; TODO elaborate on this
                 "\"[B\""
                 hint-0)]
    (->> hint (str "^") symbol)))

(defn do-each [x] (apply list 'do (->> x (map parse*))))

(def implicit-do
  (if*n (fn-> first (= 'do))
       rest
       list))

(def remove-do-when-possible
  (if*n (fn-and seq?
                (fn-> first (= 'do))
                (fn-> count (<= 2)))
        rest
        list))

(def operators
  {"assign"        'set!
   "equals"        '=
   "notEquals"     'not=
   "lessEquals"    '<=
   "greaterEquals" '>=
   "and"           'and
   "or"            'or
   "not"           'not
   "plus"          '+
   "minus"         '-
   "divide"        '/
   "times"         '*
   "greater"       '>
   "less"          '<
   "lShift"        '<<
   "rSignedShift"  '>>
   "remainder"     'rem
   "negative"      '-
   "preIncrement"  'inc! ; TODO is it wise to equate these two?
   "posIncrement"  'inc!
   "posDecrement"  'dec!
   "binAnd"        'bit-and})
(def string-literal? (partial instance? StringLiteralExpr))

(defn parse-modifiers [mods]
  (let [all-mods (->> mods
                      (ModifierSet/getAccessSpecifier)
                      str clojure.string/lower-case
                      (str "^:"))]
    (when-not (any? (eq? all-mods)
                      #{"^:default"
                        "^:public"}) 
      (symbol all-mods))))

(defn parse-operator [x]
  (if-let [oper (->> x str (get operators))]
    oper
    (throw (Exception. (str "Operator not found: '" x "'")))))

(defn parse-conditional [x]
  (let [pred (-> x (.getCondition) parse*)
        raw-then (condp instance? x
                   ConditionalExpr (.getThenExpr x)
                   IfStmt          (.getThenStmt x)
                   (throw+ (Err. nil "Conditional exception" nil)))
        then (-> raw-then parse* remove-do-when-possible)]
    (if-let [else (condp instance? x
                    ConditionalExpr (.getElseExpr x)
                    IfStmt          (.getElseStmt x)
                    (throw+ (Err. nil "Conditional exception" nil)))]
      (concat (list 'if) (list pred) then (-> else parse* remove-do-when-possible))
      (apply list 'when pred then))))

(defnt get-inner
  ([^com.github.javaparser.ast.body.MethodDeclaration      x] (.getBody x))
  ([^com.github.javaparser.ast.body.ConstructorDeclaration x] (.getBlock x)))

(def dep-primitive->clojure-primitive
  {'Byte 'byte
   'Int  'int})

(defnt parse
  "Java to Clojure."
  ([^string? x]
    (-> x conv/->input-stream JavaParser/parse parse*))
  ([#{java.io.File java.io.InputStream} x]
    (-> x JavaParser/parse parse*))
  ([^com.github.javaparser.ast.CompilationUnit x]
    (->> x (.getTypes) (mapv parse*)))
  ; TYPE
  ([^com.github.javaparser.ast.type.ReferenceType x]
     ;.getArrayCount missing
    (-> x .getType parse*))
  ([^com.github.javaparser.ast.type.PrimitiveType x] 
    (->> x .getType str symbol
         (get dep-primitive->clojure-primitive)))
  ([^com.github.javaparser.ast.type.ClassOrInterfaceType x]
    ;.getScope
    (-> x .getName
          (whenf (f*n containsv? "<") ; Parameterized class
            (fn->> (take-until "<")))
          symbol))
  ; BODY
  ([^com.github.javaparser.ast.body.ClassOrInterfaceDeclaration x]
    (-> x (.getMembers) do-each))
  ([^com.github.javaparser.ast.body.FieldDeclaration x]
    (cons 'do
          (coll/lfor [v (.getVariables x)]
            (apply list 'def (-> x .getModifiers parse-modifiers)
              (-> x .getType type-hint*)
              (-> v parse*)))))
  ([^com.github.javaparser.ast.body.InitializerDeclaration x]
    (-> x .getBlock parse*))
  ([#{com.github.javaparser.ast.body.MethodDeclaration
     com.github.javaparser.ast.body.ConstructorDeclaration} x]
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
      (concat (list 'defn) pres
           arglist body)))
  ([^com.github.javaparser.ast.body.VariableDeclarator x]
    [(-> x .getId parse*) (when-let [init (.getInit x)] (parse* init))])
  ([^com.github.javaparser.ast.body.VariableDeclaratorId x]
    (-> x str symbol))
  ([^com.github.javaparser.ast.body.Parameter x]
    [(-> x .getType type-hint*)
     (-> x .getId parse*)])
  ([^com.github.javaparser.ast.body.MultiTypeParameter x]
    (concat (->> x .getTypes (map parse*))
            (list (-> x .getId parse*))))
  ; STATEMENTS
  ([^com.github.javaparser.ast.stmt.BlockStmt x] 
    (-> x (.getStmts) do-each))
  ([^com.github.javaparser.ast.stmt.ExpressionStmt x] 
    (-> x .getExpression parse*))
  ([^com.github.javaparser.ast.stmt.BreakStmt x]
    (if-let [id (.getId x)]
      (list 'break (symbol id))
      (list 'break)))
  ([^com.github.javaparser.ast.stmt.ContinueStmt x]
    (if-let [id (.getId x)]
      (list 'continue (symbol id))
      (list 'continue)))
  ([^com.github.javaparser.ast.stmt.TryStmt x]
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
  ([^com.github.javaparser.ast.stmt.ReturnStmt x] 
    (if-let [expr (.getExpr x)]
      (list 'return (parse* expr))
      (list 'return)))
  ([^com.github.javaparser.ast.stmt.ThrowStmt x]
    (list 'throw (-> x .getExpr parse*)))
  ([^com.github.javaparser.ast.stmt.CatchClause x]
    (concat (-> x .getExcept parse*) (-> x .getCatchBlock parse* implicit-do)))
  ([^com.github.javaparser.ast.stmt.IfStmt x]
    (parse-conditional x))
  ([^com.github.javaparser.ast.stmt.SwitchStmt x]
    (apply concat (list 'case (parse* (.getSelector x)))
      (->> x .getEntries (map parse*))))
  ([^com.github.javaparser.ast.stmt.SwitchEntryStmt x]
    (let [label (-> x .getLabel (whenf nnil? parse*))
          stmts (->> x .getStmts (map parse*) (cons 'do))]
    (if label
        [label stmts]
        [stmts])))
  ([^com.github.javaparser.ast.stmt.WhileStmt x]
    (apply list 'while (-> x .getCondition parse*) (-> x .getBody parse* implicit-do)))
  ([^com.github.javaparser.ast.stmt.ForStmt x]
    (apply list 'ifor
      (->> x .getInit    (mapv parse*))
      (->> x .getCompare parse*)
      (->> x .getUpdate  (mapv parse*))
      (->> x .getBody    parse*)))
  ([^com.github.javaparser.ast.stmt.ForeachStmt x]
    (apply list 'doseq
      (conj (-> x .getVariable parse* second popr) (-> x .getIterable parse*))
      (-> x .getBody parse* implicit-do)))
  ([^com.github.javaparser.ast.stmt.ExplicitConstructorInvocationStmt x]
    ["EXPLICIT CONSTRUCTOR" (str x)])
  ; EXPRESSIONS     
  ([^com.github.javaparser.ast.expr.NameExpr x]
    (-> x .getName symbol))
  ([^com.github.javaparser.ast.expr.AssignExpr x]
    (let [right  (-> x .getValue parse*)
          oper-0 (->> x .getOperator parse-operator)
          oper-1 (when (not= oper-0 'set!) oper-0)
          oper-f (if (and (= oper-1 '+) (str-expression? right))
                     'str
                     oper-1)]
      (->> (list 'swap! (-> x .getTarget parse*) oper-f right)
           (remove nil?)
           doall)))
  ([^com.github.javaparser.ast.expr.ThisExpr x]
    'this)
  ([^com.github.javaparser.ast.expr.SuperExpr x]
    ["SUPER" (str x)])
  ([^com.github.javaparser.ast.expr.EnclosedExpr x] ; Parenthesis-enclosed
    (list 'do (-> x .getInner parse*)))
  ([^com.github.javaparser.ast.expr.NullLiteralExpr x]
    'nil)
  ([^com.github.javaparser.ast.expr.InstanceOfExpr x]
    (list 'instance?
      (-> x .getType parse*)
      (-> x .getExpr parse*)))
  ([^com.github.javaparser.ast.expr.FieldAccessExpr x]
    (list (->> x .getField (str ".") symbol)
          (->> x .getScope  parse*)))
  ([^com.github.javaparser.ast.expr.ArrayCreationExpr x]
    ["ARRAY" (->> x .getType parse*)
             (list 'array (.getArrayCount x))
             (->> x .getDimensions (map parse*))
             (->> x .getInitializer)])
  ([^com.github.javaparser.ast.expr.ArrayAccessExpr x]
    (list 'aget (-> x .getName parse*) (-> x .getIndex parse* str/val)))
  ([^com.github.javaparser.ast.expr.ArrayInitializerExpr x]
    (cons 'array (->> x .getValues (map parse*))))
  ([^com.github.javaparser.ast.expr.ClassExpr x]
    (-> x .getType parse*))
  ([^com.github.javaparser.ast.expr.CastExpr x]
    (let [type (-> x .getType parse*)
          expr (-> x .getExpr parse*)]
      (if (tcore/primitive? type)
          (let [cast-fn (symbol (str "->" (name type)))]
            (list cast-fn expr))
          (list 'cast type expr))))
  ([^com.github.javaparser.ast.expr.ConditionalExpr x]
    (parse-conditional x))
  ([^com.github.javaparser.ast.expr.MethodCallExpr x]
    (let [args (->> x .getArgs (map parse*))] 
       (if (.getScope x)
           (apply list
             (->> x (.getName) (str ".") symbol)
             (->> x .getScope parse*) args)
           (apply list
             (->> x (.getName) symbol)
             args))))
  ([^com.github.javaparser.ast.expr.LambdaExpr x]
    ["LAMBDA" (str x)])
  ([^com.github.javaparser.ast.expr.UnaryExpr x]
    (let [oper (-> x .getOperator parse*)
          expr (-> x .getExpr parse*)]
      (if (and (number? expr)
               (= oper '-))
          (- expr)
          (list oper expr))))
  ([^com.github.javaparser.ast.expr.UnaryExpr$Operator x]
    (parse-operator x))
  ([^com.github.javaparser.ast.expr.StringLiteralExpr x] 
    (-> x .getValue))
  ([^com.github.javaparser.ast.expr.BooleanLiteralExpr x]
    (-> x .getValue))
  ([^com.github.javaparser.ast.expr.IntegerLiteralExpr x]
    (-> x .getValue str/val))
  ([^com.github.javaparser.ast.expr.BinaryExpr x]
    (let [left   (-> x .getLeft     parse*)
          right  (-> x .getRight    parse*)
          oper-0 (-> x .getOperator parse*)
          oper (if (and (= oper-0 '+)
                        (or (string-concatable? left)
                            (string-concatable? right)))
                   'str
                   oper-0)]
      (list oper left right)))
  ([^com.github.javaparser.ast.expr.BinaryExpr$Operator x]
    (parse-operator x))
  ([^com.github.javaparser.ast.expr.VariableDeclarationExpr x]
    ; TODO if type is primitive, cast it to it; can't hint it 
    (let [t (-> x .getType type-hint*)]
      (list 'let (->> (.getVars x)
                      (map parse*)
                      (map (partial into [t]))
                      (apply concat)
                      (into [])))))
  ([^com.github.javaparser.ast.expr.ObjectCreationExpr x] 
    (apply list (-> x .getType (str ".") symbol)
      (->> x .getArgs (mapv parse*)))))

(defn parse* [x] (add-context x) (parse x))

(def log1 (atom []))
(def print-log #(swap! log1 conj (apply println-str %&)))



(defn clean
  "Fixes funky imperative Clojure code to be more idiomatic."
  [x]
  (->> x
       (postwalk ; Start from bottom. Essential for granular things 
         (condf*n
           (fn-and listy?
             (fn-> first (= '.println))
             (fn-> second listy?)
             (fn-> second second (= 'System)))
           (fn->> rest rest (cons 'println))

           (eq? '.equals)
           (constantly '=)

           :else identity))
       (prewalk ; Start from the top, not the bottom. Essential for cond-folding 
         (condf*n
           cond-foldable?
           identity #_cond-fold

           :else identity))
       (postwalk
         (compr
           (condf*n
             (fn-and conditional-statement?
               (fn->> ana/conditional-branches (every? return-statement?))) ; Have to do this in separate postwalk because cond-fold affects return statements   
             (fn [x] (list 'return (->> x (ana/map-conditional-branches (f*n second)))))
       
             :else identity)
           #_join-lets))
       ;(prewalk (fn-> enclose-lets str-fold))
       (postwalk
         (condf*n
           (fn-and function-statement?
             (fn-> last return-statement?))
           (f*n update-last (f*n second))

           (fn-and do-statement? (fn-> count (= 2)))
           (f*n second)

           :else identity))
       first
       (map (if*n (fn-and listy? (fn-> first (= 'do))) rest list))
       (apply concat)))

