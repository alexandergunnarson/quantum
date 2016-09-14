; ==================== MACROS ===================
; oeval is |eval| in the object language

; can only be used in macros
(defmacro apply [f & args]
  (let [args-f (concat (butlast args) (last args))]
    (oeval (apply list f args-f))))

(defmacro set! [lval rval]
  (str/sp (oeval lval) "=" (oeval rval)))


; (defmacro jfn-1 [f]
;   (oeval
;     (list 'fn '[caller args]
;       (list '. 'caller f 'args))))

(defmacro splice [f & args]
  (oeval (list 'apply 'f 'args)))

(defmacro aget [coll n]
  (str (oeval coll) "[" (oeval n) "]"))

(defmacro first  [coll] (oeval (list 'aget coll 0)))
(defmacro second [coll] (oeval (list 'aget coll 1)))

(defmacro array [& args]
  (str "[" (->> args (map (partial oeval)) (str/join ", ")) "]"))

(defmacro = [a b] (str/paren (str/sp (oeval a) "==" (oeval b))))
(defmacro not= [a b] (str/paren (str/sp (oeval a) "!=" (oeval b))))

(defmacro type [obj]
  (println "IN TYPE WITH" obj)
  (->> obj oeval (str/sp "typeof") str/paren))

; TODO maybe doesn't work
(defmacro throw [errs]
  (println "ERRS IS" errs)
  (str/sp "throw" (oeval errs)))

(defmacro println [& objs]
  (oeval
    (quote+ (apply Debug/WriteLine ~objs))))

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
         ;(<- str "\n")
         )))

(defmacro conj [coll & args]
  (let [conj-sym
         (cond (-> coll metaclass (= 'Vec)) 'ppush :else 'mori/conj)]
    (oeval
      `(~'apply ~conj-sym ~coll ~args))))

; ===============================================

; ================== PLUMBING ===================

(defmacro with-transient! [obj & body]
  (oeval
    (concat
      (list 'do (list 'transient! obj))
      body
      (list (list 'persistent! obj)))))

; A good idea, but later
; (defmacro definline [sym arglist & body]
;   (let [body-f
;           (->> body (cons 'do)
;                (unquote-replacement (ns/local-context)))]
;     (identity ;oeval
;       (quote+
;         (defmacro ~sym ~arglist
;           ~body-f)))))

; (definline getr [coll a b]
;   (for-into (empty coll) [i (range a b)]
;     (get coll i)))

(defmacro getr [coll a b]
  (oeval
    (quote+
      (for-into (empty ~coll) [i (range ~a ~b)]
        (get coll i)))))

(defmacro map! [& args]
  (oeval (quote+ (apply C/hash-map ~args))))

(defmacro cast [class- obj]
  (str (-> class- str demunge-class str/paren) (oeval obj)))

(defmalias conj! push)


(defmacro dot [& args] (->> args (map oeval) (str/join ".")))

(defmacro class [arg] (oeval (quote+ (.GetType ~arg))))

; (defrecord Class [a b c d e])
(defmacro defrecord [class- props]
  (let [prop-defs
         (->> props
              (map (partial list 'dot 'this))
              (map (fn [expr] (list 'set! expr (last expr)))))]
    (oeval
      (list 'defn (with-meta class- {:class true}) props
        (cons 'do prop-defs)))))

(defmacro symbol [sym] (oeval (quote+ (str ~sym))))

(defmacro ->
  [x & forms]
  (let [code (loop [x x forms forms]
      (if forms
        (let [form (first forms)
              next-form  (when (seq? form) (next  form))
              first-form (when (seq? form) (first form))
              threaded (if (seq? form)
                           (quote+ (apply ~first-form ~x ~next-form))
                           (list form x))]
          (recur threaded (next forms)))
        x))]
  (oeval code)))

(defmacro out [obj] (str/paren (str/sp "out" (oeval obj))))

(defmacro comment [obj] (str/sp "//" obj))

(defmacro inc [n] (oeval (quote+ (+ ~n 1))))
(defmacro inc! [n] (str/sp (oeval n) "++"))
(defmacro dec [n] (oeval (quote+ (- ~n 1))))
(defmacro count [coll] (oeval (quote+ (C/count ~coll))))
(defmacro lasti [coll] (oeval (quote+ (-> ~coll count dec))))

(defmacro get [& args] (oeval (quote+ (apply C/get ~args))))
(defmacro keys [arg] (oeval (quote+ (.-Keys ~arg))))
(defmacro key [arg] (oeval (quote+ (.-Key ~arg))))
(defmacro vals [arg] (oeval (quote+ (.-Values ~arg))))
(defmacro val [arg] (oeval (quote+ (.-Value ~arg))))
(defmacro atom [arg] (oeval arg))
(defmacro reset! [arg val-] (oeval (quote+ (set! ~arg ~val-))))
(defmacro array-of [type & vals-] (str/sp "new" (str type "[]") (str/sp "{" (->> vals- (map oeval) doall (str/join ", ")) "}")))
(defmacro array-map [& args] (oeval (quote+ (apply map! ~args))))