; ============ REQUIRE/NS STATEMENTS ============
;(require '[mori :refer :all])
; ===============================================
#_(defmacro defnt
  [sym & {:as body}]
  (let [type-map-sym (gensym 'symbol-map)
        f-genned     (gensym 'f)]
    (intern *ns* sym nil) ; In case of recursive calls
   (eval `(let [_# (log/pr :macro-expand "BODY" '~body)
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
                            (get ~'~type-map-sym :default))]
                   (if (nil? ~'~f-genned)
                       (throw (Exception. (str ~(str "|" '~sym "|" " not defined for ") (class ~arg0#))))
                       ~(apply list '~f-genned arglist#)))))
          new-arity?#
            (fn [arglist#]
              ((fn-not contains?) @genned-arities# (count arglist#)))]
      ; Need to generate all the apropriate arities via a template. These are type-independent.
      ; Once a particular arity is generated, it need not be generated again.
      (doseq [[pred# arities-n#] '~body]
        (let [; Normalizes the arities to be in an arity-group, even if there's only one arity for that type
              arities-normalized#
                (whenf arities-n# (fn-> first vector?) list)
              arities-for-type# (atom {})]
          (doseq [[arglist# & f-body#] arities-normalized#]
            (log/pr :macro-expand "pred arglist f-body |" pred# arglist# f-body#)
            ; Arity checking within type
            (let [arity-n#          (count arglist#)
                  arity-type-n#     (macro/arity-type arglist#)
                  curr-variadic-arity-for-type#
                    (->> @arities-for-type#
                         (filter (fn-> val (= :variadic)))
                         first
                         (<- (whenf nnil? key)))
                  curr-variadic-arity#
                    (->> @genned-arities#
                         (filter (fn-> val keys first (= :variadic)))
                         first
                         (<- (whenf nnil? key)))]
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
                  (log/pr :macro-expand genned-arity#)
                  (swap! genned-arities# assoc-in [arity-n# arity-type-n#] genned-arity#)))))))

       ; Create type overloads
       (doseq [[pred# arities-n#] '~body]
         (log/pr :macro-expand (str "ADDING OVERLOAD FOR PRED " pred#) (apply list 'fn arities-n#))
         (doseq [type# (get type/types pred#)]
           (swap! type-map-temp# assoc type#
             (eval (apply list 'fn arities-n#)))))

      (intern *ns* '~type-map-sym (deref type-map-temp#))
      (log/pr :macro-expand "TYPE MAP" (resolve '~type-map-sym))

      (let [genned-arities-f#
              (->> @genned-arities#
                   vals
                   (map vals)
                   (map first))
            genned-fn-f# (apply list 'defn '~sym genned-arities-f#)]
        (log/pr :macro-expand "GENNED FN FINAL" genned-fn-f#)
        (->> genned-fn-f#
             (clojure.walk/postwalk
               (whenf$n (fn-and symbol? qualified? auto-genned?) unqualify))
             ))))))

#_(defnt testing
  list?    (([coll]
              (println "THIS A LIST, 1 ARG" coll) coll)
            ([coll arg]
              (println "THIS IS A LIST:" coll) coll))
  vec?     ([coll arg]
             (println "THIS IS A VEC:"  coll) coll)
  :default ([arg] (println "DEFAULT!" arg) (testing (list 1 2 3))))

; ==================== MACROS ===================
; oeval is |eval| in the object language

; can only be used in macros
(defmacro apply [f & args]
  (let [args-f (concat (butlast args) (last args))]
    (oeval (apply list f args-f))))

(defmacro set! [sym post]
  (str/sp (oeval sym) "=" (oeval post)))


; (defmacro jfn-1 [f]
;   (oeval
;     (list 'fn '[caller args]
;       (list '. 'caller f 'args))))

(defmacro splice [f & args]
  (oeval (list 'apply 'f 'args)))

(defmacro aget [coll n]
  (str (oeval coll) "[" (oeval n) "]"))

(defmacro first  [coll] (oeval (list 'get coll 0)))
(defmacro second [coll] (oeval (list 'get coll 1)))

(defmacro array [& args]
  (str "[" (->> args (map (partial eval-form lang)) (str/join ", ")) "]"))

(defmacro = [a b] (str/paren (str/sp (oeval a) "===" (oeval b))))

(defmacro type [obj]
  (println "IN TYPE WITH" obj)
  (->> obj oeval (str/sp "typeof") str/paren))

(defmacro class [obj]
  (oeval `(~'get ~'classes (~'type ~obj))))

; TODO maybe doesn't work
(defmacro throw [errs]
  (println "ERRS IS" errs)
  (str/sp "throw" (oeval errs)))

(defmacro println [& objs]
  (oeval
    `(~'apply ~'console/log ~objs)
    ;(list 'console/log (list 'apply 'jstr/sp objs))
    ))

; TODO pre-register ns's and have |require| only be to def an alias
(defmacro require [arg]
  (let [[ns-name-0 & {:keys [as]}] (-> arg (nth 1))
        _ (println "REQUIRING" (-> arg (nth 1)))
        ns-name-f (or as ns-name-0)
        file-name (str (-> ns-name-0 name) ".clj")
        file-str
          (io/read
            :path [:test "quanta" "compile" file-name]
            :read-method :str)
        code (read-string (str "(do " file-str ")"))
        ^Fn register
          (fn [[spec-sym sym & body]]
            (let [registered-sym (keyword sym)
                  transformed-body
                    (condp = spec-sym
                      'def  (do (assert (-> body count (= 1)))
                                (first body))
                      'defn (cons 'fn body))]
              [registered-sym transformed-body]))
        ^Fn register-ns
          (fn [ns-contents]
            (list 'def ns-name-f ns-contents))]
    (->> code
         (filter (fn-> first (in? #{'def 'defn})))
         (map register)
         (apply merge {})
         register-ns
         oeval
         ;(<- (str "\n"))
         )))

(defn array?  [obj] (= obj/constructor Array))

(defn vector? [obj] (array? obj))
(defn string? [obj] (= (type obj) "string"))
(defn object? [obj] (= (type obj) "object"))

; ===============================================

(defmacro conj [coll & args]
  (let [conj-sym
         (cond (-> coll metaclass (= 'Vec)) 'ppush :else 'mori/conj)]
    (oeval
      `(~'apply ~conj-sym ~coll ~args))))

; ===============================================

; ================== PLUMBING ===================
(def classes
  (hash-map
    "undefined" "Undefined"
    "object"    "Object"
    "boolean"   "Bool"
    "number"    "Num"
    "string"    "String"
    "symbol"    "Symbol"
    "function"  "Fn"))

; TODO dispatch on JS object field
(defn get [coll n]
  (if (vector? coll)
      (aget coll n)
      (mori/get coll n)))

(defmacro with-transient! [obj & body]
  (oeval
    (concat
      (list 'do (list 'transient! obj))
      body
      (list (list 'persistent! obj)))))

(defn into [base & args]
  (doseq [obj args]
    (cond
      (string? base)
        (nstr base obj)
      (array? base)
        (.concat base obj)
      ; VECTOR
      :else
        (with-transient! base
          (doseq [arg args] (conj! base arg)))
       ; (throw {:msg "Unknown"})
       ))
  base)

(def-macro-alias concat  into  )
(def-macro-alias concat! concat)

; multi-arity dispatch on this
(defn push [coll & args]
  (concat coll args))

(defmacro definline [sym arglist & body]
  (let [body-f
          (->> body (cons 'do)
               (unquote-replacement (ns/local-context)))]
    (identity ;oeval
      (quote+
        (defmacro ~sym ~arglist
          ~body-f)))))

(definline getr [coll a b]
  (for-into (empty coll) [i (range a b)]
    (get coll i)))

(defmacro getr [coll a b]
  (oeval
    (quote+
      (for-into (empty ~coll) [i (range ~a ~b)]
        (get coll i)))))

(def-macro-alias conj! push)


(defmacro dot [& args] (->> args (map oeval) (str/join ".")))

; (defrecord Class [a b c d e])
(defmacro defrecord [class- props]
  (let [prop-defs
         (->> props
              (map (partial list 'dot 'this))
              (map (fn [expr] (list 'set! expr (last expr)))))]
    (oeval
      (list 'defn (with-meta class- {:class true}) props
        (cons 'do prop-defs)))))

(defrecord Var [class- val]) ; add meta later

; Defines a variable
(defmacro var [class-0 val-]
  (let [class-f class-0]
    (oeval
      (list 'Var. class-f val-))))

(defmacro symbol [sym] (str sym))

(def ^String abcde "ABCDEF")

; ===============================================

(defmacro react-parse* [component]
  (println "IN REACT-PARSE* WITH:" component)
  (condfc component
    string? component
    list?   component
    symbol? component
    vector?
      (let [[tag props & elems] component
            elems-parsed
              (->> elems (map (fn [elem] (react-parse*- :js (list 'react-parse*- elem)))))]
        (concat
          (list '.createElement 'React
             tag
             (list 'Object. props))
          elems-parsed))))

; [:div {} ([:div {:prop 1}])]
(defmacro react-parse [component]
  (println "IN REACT-PARSE WITH:" component)
  (oeval (react-parse*- :js (list 'react-parse*- component))))

(defmacro identity [obj] (oeval obj))

; MUTABILITY, ETC.

; TODO works with everything... bad...
(def-macro-alias reset! set!)

; TODO won't use in the future
(defmacro compress-props [sym props-vec]
  (->> props-vec
       (map oeval)
       (map (whenf$n (f$n str/starts-with? "\"") (fn-> popl popr)))
       (str/join ".")
       (str (oeval sym) ".")))

; TODO just a hack...
(defmacro swap-in! [sym props f & args]
  (let [prop-to-affect (oeval (list 'compress-props sym props))]
    (str/sp prop-to-affect "="
      (oeval (list 'apply f (symbol prop-to-affect) args)))))

(defmacro reset-in! [sym props new-val]
  (let [prop-to-affect (oeval (list 'compress-props sym props))]
    (str/sp prop-to-affect "=" (oeval new-val))))

; ============================================================

(require '[string :as str-])

(def str str-/str)

(def div-style {:font-family "Arial" :color :red})

(def state
  {:items []})

(defn todo-list []
  (for [item this.state.items]
    [:li {} item])
  ; [:div {}
  ;   (for [item this.state.items]
  ;     [:li {} item])]
    )

(def todo-list1
  (React/createClass
    {:render
      (fn []
        (react-parse
          ;[:div {} (.toString state.items)]
          [:div {} ""])
        )}))

(def todo-app
  (React/createClass
    {:display-name "TodoApp"
     :get-initial-state
       (fn [] {:items (array) :text ""})
     :on-change
       (fn [e]
         (println "e.target.value" e.target.value)
         (println (str "1" "2" "3" "4" "5"))
         (.setState this {:text e.target.value}))
     :handle-submit
       (fn [e]
         (let [next-text  ""] ; For clearing
           (.preventDefault e)
           (swap-in! state [:items] conj this.state.text)
           (.setState this {:text next-text})))
     :render
       (fn []
         ; (println "state.items" (str state.items)
         ;   ;(type this.state.items)
         ;   ;(class this.state.items)
         ;   ;this.state.items/constructor
         ;   )
         (react-parse
           [:div {:style div-style}
             [:h3 {} "TODO"]
             ;[:div {} (str state.items)]
             todo-list1
             ;(todo-list this.state.items)
             [:form {:on-submit this.handle-submit}
               [:input
                 {:on-change this.on-change
                  :value     this.state.text}]]
               [:button {} (str "Add #" (+ this.state.items.length 1))]]))}))

; (println "CLASSES" classes
;   "CONSTRUCTOR" classes.-constructor
;   "TYPE" (type classes)
;   )

(React/render
  (React/createElement todo-app nil)
  (.getElementById document "container"))
