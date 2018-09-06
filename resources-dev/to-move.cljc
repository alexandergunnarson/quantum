;; TO MOVE

;; ===== quantum.core.form

(t/def langs #{:clj :cljs :clr})

(t/def lang "The language this code is compiled under" #?(:clj :clj :cljs :cljs))

;; ===== quantum.core.form.generate

;; TODO TYPED
(defalias u/externs?)

;; ===== quantum.core.system

#?(:clj
(defnt pid [> (? t/string?)]
  (->> (java.lang.management.ManagementFactory/getRuntimeMXBean)
       (.getName))))

;; TODO TYPED
(defalias u/*registered-components)


;; ===== UNKNOWN ===== ;;

;; ----- Mutability/Effects

;; TODO TYPED
(defprotocol IValue
  (get [this])
  (set [this newv]))
#_(do (declare-fnt get [this _])
      (declare-fnt set [this _, newv _]))

;; ----- Really unknown

(defnt >sentinel [> t/object?] #?(:clj (Object.) :cljs #js {}))
(defalias >object >sentinel)

;; TODO TYPED
#?(:clj
(defmacro with
  "Evaluates @expr, then @body, then returns @expr.
   For (side) effects."
  [expr & body]
  `(let [expr# ~expr] ~@body expr#)))

#_(:clj
(defmacrot with
  "Evaluates @expr, then @body, then returns @expr.
   For (side) effects."
  [expr t/form? & body (? (t/seq-of t/form?))]
  `(let [expr# ~expr] ~@body expr#)))
