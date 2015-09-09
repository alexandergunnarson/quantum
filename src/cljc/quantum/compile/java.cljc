(ns quantum.compile.java
  (:require-quantum [:lib])
  (:require [quantum.core.analyze.clojure.predicates :as pred :refer :all]
     [quantum.core.analyze.clojure.core :as ana])
  (:import
    com.github.javaparser.JavaParser
    (com.github.javaparser.ast CompilationUnit)
    (com.github.javaparser.ast.body
      ClassOrInterfaceDeclaration InitializerDeclaration
      ConstructorDeclaration
      MethodDeclaration FieldDeclaration FieldDeclaration
      ModifierSet
      Parameter MultiTypeParameter
      VariableDeclarator VariableDeclaratorId)
    (com.github.javaparser.ast.comments JavadocComment)
    (com.github.javaparser.ast.stmt
      ExplicitConstructorInvocationStmt
      BlockStmt Statement ExpressionStmt
      TryStmt ThrowStmt CatchClause
      BreakStmt
      WhileStmt
      ForeachStmt ForStmt
      ReturnStmt IfStmt)
    (com.github.javaparser.ast.expr
      ObjectCreationExpr NameExpr VariableDeclarationExpr
      SuperExpr
      ArrayAccessExpr ArrayCreationExpr
      ConditionalExpr
      CastExpr
      LambdaExpr
      NullLiteralExpr AssignExpr MethodCallExpr
      ClassExpr ThisExpr
      FieldAccessExpr
      EnclosedExpr
      UnaryExpr UnaryExpr$Operator
      BinaryExpr BinaryExpr$Operator
      BooleanLiteralExpr StringLiteralExpr IntegerLiteralExpr)
    (com.github.javaparser.ast.type
      ReferenceType ClassOrInterfaceType
      PrimitiveType PrimitiveType$Primitive)))

; TODO fix double escaping
; http://javaparser.github.io/javaparser/javadoc-current/japa/parser/ast/expr/ConditionalExpr.html

(def context (atom [nil nil])) ; We won't do parallel parsing for now

(defn add-context [x]
  (swap! context assoc 0 (get @context 1))
  (swap! context assoc 1 x))

(declare parse)
(defn parse* [x] (add-context x) (parse x))


(defn clean-javadoc [s]
  (-> s (clojure.string/replace #"\n*\s*\*\s" "\n")))

(defn type-hint* [x] (->> x (str "^") symbol))

(defn do-each [x] (apply list 'do (->> x (map parse*))))

(def implicit-do
  (if*n (fn-> first (= 'do))
       rest
       list))

(def remove-do-when-possible
  (if*n (fn-and (fn-> first (= 'do))
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
   "remainder"     'rem
   "negative"      'neg
   "preIncrement"  'inc!
   "posIncrement"  'inc!
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



(macros/defntp parse
  "Java to Clojure."
  [java.io.File]
    ([x] (-> x JavaParser/parse parse*))
  [CompilationUnit]
    ([x] (->> x (.getTypes) (mapv parse*)))
  ; TYPE
  [ReferenceType]
    ([x] ;.getArrayCount missing
         (-> x .getType parse*))
  [PrimitiveType]
    ([x] (-> x .getType str symbol))
  [ClassOrInterfaceType]
    ([x] ;.getScope
         (-> x .getName
               (whenf (f*n containsv? "<") ; Parameterized class
                 (fn->> (take-until "<")))
               symbol))
  ; BODY
  [ClassOrInterfaceDeclaration]
    ([x] (-> x (.getMembers) do-each))
  [FieldDeclaration]
    ([x] (cons 'do
           (coll/lfor [v (.getVariables x)]
             (apply list 'def (-> x .getModifiers parse-modifiers) (-> x .getType type-hint*)
               (-> v parse*)))))
  [InitializerDeclaration]
    ([x] (-> x .getBlock parse*))
  [MethodDeclaration ConstructorDeclaration]
    ([x] ; getTypeParameters
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
  [VariableDeclarator]
    ([x] [(-> x .getId parse*) (when-let [init (.getInit x)] (parse* init))])
  [VariableDeclaratorId]
    ([x] (-> x str symbol))
  [Parameter]
    ([x] [(-> x .getType type-hint*)
          (-> x .getId parse*)])
  [MultiTypeParameter]
    ([x] (concat (->> x .getTypes (map parse*))
                 (list (-> x .getId parse*))))
  ; STATEMENTS
  [BlockStmt] 
    ([x] (-> x (.getStmts) do-each))
  [ExpressionStmt] 
    ([x] (-> x .getExpression parse*))
  [BreakStmt]
    ([x] (if-let [id (.getId x)]
           (list 'break (symbol id))
           (list 'break)))
  [TryStmt]
    ([x] ;(.getResources x) 
         (->> (list
                (cons 'try
                  (-> x .getTryBlock parse* implicit-do))
                  (->> (.getCatchs x)
                       (map parse*)
                       (map (partial apply list 'catch )))
                  (when-let [fb (.getFinallyBlock x)]
                    (list (cons 'finally (-> fb parse* implicit-do)))))
              (apply concat)))
  [ReturnStmt] 
    ([x] (if-let [expr (.getExpr x)]
           (list 'return (parse* expr))
           (list 'return)))
  [ThrowStmt]
    ([x] (list 'throw (-> x .getExpr parse*)))
  [CatchClause]
    ([x] (concat (-> x .getExcept parse*) (-> x .getCatchBlock parse* implicit-do)))
  [IfStmt]
    ([x] (parse-conditional x))
  [WhileStmt]
    ([x] (apply list 'while (-> x .getCondition parse*) (-> x .getBody parse* implicit-do)))
  [ForStmt]
    ([x] (apply list 'ifor
           (->> x .getInit    (mapv parse*))
           (->> x .getCompare parse*)
           (->> x .getUpdate  (mapv parse*))
           (->> x .getBody    parse*)))
  [ForeachStmt]
    ([x] (apply list 'doseq
           (conj (-> x .getVariable parse* second popr) (-> x .getIterable parse*))
           (-> x .getBody parse* implicit-do)))
  [ExplicitConstructorInvocationStmt]
    ([x] ["EXPLICIT CONSTRUCTOR" (str x)])
  ; EXPRESSIONS     
  [NameExpr]
    ([x] (-> x .getName symbol))
  [AssignExpr]
    ([x] (let [right  (-> x .getValue parse*)
               oper-0 (->> x .getOperator parse-operator)
               oper-1 (when (not= oper-0 'set!) oper-0)
               oper-f (if (and (= oper-1 '+) (str-expression? right))
                          'str
                          oper-1)]
           (->> (list 'swap! (-> x .getTarget parse*) oper-f right)
                (remove nil?)
                doall)))
  [ThisExpr]
    ([x] 'this)
  [SuperExpr]
    ([x] ["SUPER" (str x)])
  [EnclosedExpr] ; Parenthesis-enclosed
    ([x] (list 'do (-> x .getInner parse*)))
  [NullLiteralExpr]
    ([x] 'nil)
  [FieldAccessExpr]
    ([x] (list (->> x .getField (str ".") symbol)
         (->> x .getScope  parse*)))
  [ArrayCreationExpr]
    ([x] ["ARRAY" (->> x .getType parse*)
                  (list 'array (.getArrayCount x))
                  (->> x .getDimensions (map parse*))
                  (->> x .getInitializer)])
  [ArrayAccessExpr]
    ([x] (list 'aget (-> x .getName parse*) (-> x .getIndex parse* str/val)))
  [ClassExpr]
    ([x] (-> x .getType parse*))
  [CastExpr]
    ([x] (list 'cast (-> x .getType parse*) (-> x .getExpr parse*)))
  [ConditionalExpr]
    ([x] (parse-conditional x))
  [MethodCallExpr]
    ([x] (let [args (->> x .getArgs (map parse*))] 
           (if (.getScope x)
               (apply list
                 (->> x (.getName) (str ".") symbol)
                 (->> x .getScope parse*) args)
               (apply list
                 (->> x (.getName) symbol)
                 args))))
  [LambdaExpr]
    ([x] ["LAMBDA" (str x)])
  [UnaryExpr]
    ([x] (list (-> x .getOperator parse*) (-> x .getExpr parse*)))
  [UnaryExpr$Operator]
    ([x] (parse-operator x))
  [StringLiteralExpr] 
    ([x] (-> x .getValue))
  [BooleanLiteralExpr]
    ([x] (-> x .getValue))
  [IntegerLiteralExpr]
    ([x] (-> x .getValue str/val))
  [BinaryExpr]
    ([x] (let [left   (-> x .getLeft     parse*)
               right  (-> x .getRight    parse*)
               oper-0 (-> x .getOperator parse*)
               oper (if (and (= oper-0 '+)
                             (or (string-concatable? left)
                                 (string-concatable? right)))
                        'str
                        oper-0)]
           (list oper left right)))
  [BinaryExpr$Operator]
    ([x] (parse-operator x))
  [VariableDeclarationExpr]
    ([x] (let [t (-> x .getType type-hint*)]
           (list 'let (->> (.getVars x)
                           (map parse*)
                           (map (partial into [t]))
                           (apply concat)
                           (into [])))))
  [ObjectCreationExpr] 
    ([x] (apply list (-> x .getType (str ".") symbol)
           (->> x .getArgs (mapv parse*)))))


(def log1 (atom []))
(def print-log #(swap! log1 conj (apply println-str %&)))



(defn clean [x]
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

(do (def file-loc "/Users/alexandergunnarson/Development/Source Code Projects/quanta-test/test/temp.java")
    (-> (java.io.File. file-loc) parse clean
        ((fn [x] (with-out-str (pprint x))))
        println))




; (defn format-code [c]
;   (condp ))