(ns quantum.untyped.core.fn
  (:refer-clojure :exclude [comp constantly])
  (:require
    [clojure.core                :as core]
    [quantum.untyped.core.core   :as ucore]
    [quantum.untyped.core.form.evaluate
      :refer [case-env compile-if]]
    [quantum.untyped.core.form.generate
      :refer [arity-builder gen-args max-positional-arity unify-gensyms]]
    [quantum.untyped.core.vars
      :refer [defalias defmacro-]])
#?(:cljs
  (:require-macros
    [quantum.untyped.core.fn :as self
      :refer [fn'|generate gen-positional-nthas gen-ntha]])))

(ucore/log-this-ns)

;; ===== `fn<i>`: Positional functions ===== ;;

#?(:clj (defmacro fn0 [  & args] `(fn fn0# [f#  ] (f# ~@args))))
#?(:clj (defmacro fn1 [f & args] `(fn fn1# [arg#] (~f arg# ~@args)))) ; analogous to ->
#?(:clj (defmacro fnl [f & args] `(fn fnl# [arg#] (~f ~@args arg#)))) ; analogous to ->>

;; ===== `fn&`: Partial functions ===== ;;

#?(:clj
(defmacro fn&* [arity f & args]
  (let [f-sym (gensym) ct (count args)
        macro? (-> f resolve meta :macro)]
    `(let [~f-sym ~(when-not macro? f)]
     (fn ~@(for [i (range (if arity arity       0 )
                          (if arity (inc arity) 10))]
             (let [args' (vec (repeatedly i #(gensym)))]
               `(~args' (~(if macro? f f-sym) ~@args ~@args'))))
         ; Add variadic arity if macro
         ~@(when (and (not macro?)
                      (nil? arity))
             (let [args' (vec (repeatedly (+ ct 10) #(gensym)))]
               [`([~@args' & xs#] (apply ~f-sym ~@args ~@args' xs#))])))))))

#?(:clj (defmacro fn&  [f & args] `(fn&* nil ~f ~@args)))
#?(:clj (defmacro fn&0 [f & args] `(fn&* 0   ~f ~@args)))
#?(:clj (defmacro fn&1 [f & args] `(fn&* 1   ~f ~@args)))
#?(:clj (defmacro fn&2 [f & args] `(fn&* 2   ~f ~@args)))
#?(:clj (defmacro fn&3 [f & args] `(fn&* 3   ~f ~@args)))

;; ===== `fn'`: Fixed/constant functions ===== ;;

#?(:clj
(defmacro- fn'|generate []
  (let [v-sym 'v]
    `(defn ~'fn'
       "Exactly the same as `core/constantly`, but uses efficient positional
        arguments when possible rather than varargs every time."
       [~v-sym]
       (~'fn ~@(arity-builder (core/constantly v-sym) (core/constantly v-sym)))))))

(fn'|generate)
(defalias constantly fn')

#?(:clj
(defmacro fn'*|arities
  [arities-ct & body]
  (let [f (gensym "this")]
   `(~'fn ~f ~@(arity-builder
                 (fn [args] (if (empty? args) `(do ~@body) `(~f)))
                 (fn' `(~f))
                 0 arities-ct)))))

#?(:clj
(defmacro fn'*
  "Like `fn'` but re-evaluates the body each time."
  [& body] `(fn'*|arities 4 ~@body))) ; conservative to limit generated code size

;; ===== `comp`: Compositional functions ===== ;;

(defalias comp core/comp)
;; TODO demacro
#?(:clj (defmacro rcomp [& args] `(comp ~@(reverse args))))

;; ===== `aritoid` ===== ;;

#?(:clj
(defmacro aritoid
  ;; TODO use `arity-builder`
  "Combines fns as arity-callers."
  {:attribution "alexandergunnarson"
   :equivalent `{(aritoid vector identity conj)
                 (fn ([]      (vector))
                     ([x0]    (identity x0))
                     ([x0 x1] (conj x0 x1)))}}
  [& fs]
  (let [genned  (repeatedly (count fs) #(gensym "f"))
        fs-syms (vec (interleave genned fs))]
   `(let ~fs-syms
      (fn ~'aritoid ~@(for [[i f-sym] (map-indexed vector genned)]
                        (let [args (vec (repeatedly i #(gensym "x")))]
                         `(~args (~f-sym ~@args)))))))))

;; ===== Arrow macros and functions ===== ;;

#?(:clj
(defmacro <-
  "Converts a ->> to a ->"
   {:inspiration "thebusby.bagotricks"
    :usage       `(->> (range 10) (map inc) (<- (doto println) distinct) (reduce +))}
  [& args] `(-> ~(last args) ~@(butlast args))))

#?(:clj (defalias <<- ->>))

#?(:clj
(defmacro fn->
  "Equivalent to `(fn [x] (-> x ~@body))`"
  {:attribution "thebusby.bagotricks"}
  [& body] `(fn fn-># [x#] (-> x# ~@body))))

#?(:clj
(defmacro fn->>
  "Equivalent to `(fn [x] (->> x ~@body))`"
  {:attribution "thebusby.bagotricks"}
  [& body] `(fn fn->># [x#] (->> x# ~@body))))

#?(:clj
(defmacro arrow?-base [arrow pred-expr expr forms]
  (let [g (gensym) pred (gensym "pred")
        steps (->> forms (map (fn [step] `(when (~pred ~g) (~arrow ~g ~step)))))]
    `(let [~g ~expr ~pred ~pred-expr ~@(interleave (repeat g) (butlast steps))]
       ~(if (empty? steps) g (last steps))))))

#?(:clj
(defmacro ->?
  "When expr satifies `pred`, threads it into the first form (via `->`),
   and when that result satisfies `pred`, so on through the next; etc."
  [pred-expr expr & forms] `(arrow?-base -> ~pred-expr ~expr ~forms)))

#?(:clj (defmacro some?-> "Equivalent to `some->`" [& args] `(->? some? ~@args)))

(defmacro ->>?
  "When expr satifies `pred`, threads it into the first form (via `->>`),
   and when that result satifies `pred`, so on through the next; etc."
  [pred-expr expr & forms] `(arrow?-base ->> ~pred-expr ~expr ~forms))

#?(:clj (defmacro some?->> "Equivalent to `some->>`" [& args] `(->>? some? ~@args)))

;; ===== For side effects ===== ;;

#?(:clj
(defmacro with-do
  "Like prog1 in Common Lisp, or a `(do)` that returns the first form."
  [expr & exprs] `(let [ret# ~expr] ~@exprs ret#)))

#?(:clj
(defmacro with-do-let
  "Like aprog1 or prog1-bind in Common Lisp."
  [[sym retn] & body] `(let [~sym ~retn] ~@body ~sym)))

;; ===== Common fixed-function values ===== ;;

(def fn-nil   (fn' nil  ))
(def fn-false (fn' false))
(def fn-true  (fn' true ))

;; ===== Argument-updating fns ===== ;;

; ----- NTHA ----- ;

(defn gen-positional-ntha [position]
  `(~'defn ~(symbol (str "ntha-" position))
     ~(str "Accepts any number of arguments and returns the (n=" position ")th in O(1) time.")
     ~@(arity-builder (fn [args] (nth args position))
                      (fn [args vargs] (nth args position)) (inc position))))

#?(:clj
(defmacro gen-positional-nthas []
  `(do ~@(for [i (range 0 (:clj max-positional-arity))] (gen-positional-ntha i)))))

(gen-positional-nthas)

(defn ntha-&
  "Accepts any number of arguments and returns the nth, variadically, in O(n) time."
  [n] (fn [& args] (nth args n)))

(defalias firsta  ntha-0)
(defalias seconda ntha-1)
(defalias thirda  ntha-2)

#?(:clj
(defmacro gen-ntha []
  (let [n-sym (gensym "n")]
    `(~'defn ~'ntha
       "Accepts any number of arguments and returns the nth.
        If n <= 18, returns in O(1) time; otherwise, in O(n) time via varargs."
       [~(with-meta n-sym {:tag 'long})]
       (case ~n-sym
         ~@(apply concat
             (for [i (range 0 (:clj max-positional-arity))]
               [i (symbol (str "ntha-" i))]))
         (ntha-& ~n-sym))))))

(gen-ntha)

;; ===== Miscellaneous ===== ;;

(defn ? [f]
  (fn ? [x] (if (nil? x) nil (f x))))
