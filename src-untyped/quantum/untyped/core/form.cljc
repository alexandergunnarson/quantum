(ns quantum.untyped.core.form
  (:require
    [quantum.untyped.core.core          :as ucore
      :refer [defalias]]
    [quantum.untyped.core.form.evaluate
      :refer [case-env*]]
    [quantum.untyped.core.form.generate :as ufgen]))

(ucore/log-this-ns)

(defprotocol PGenForm
  (>form [this] "Returns the form associated with the object.
                 If evaluated, the form should evaluate to something exactly equivalent to the
                 value of the object (even stronger than a `=` guarantee — all properties up to
                 but not including identity).
                 Effectively the inverse of `eval`.

                 A form may consist of any of the following, recursively:
                 - nil
                 - number
                   - double
                   - long
                   - bigdec (`M`)
                   - bigint (`N`)
                 - string
                 - symbol
                 - keyword
                 - seq
                 - vector
                 - map"))

(extend-protocol PGenForm
            nil                 (>form [x] nil)
            java.lang.Long      (>form [x] x)
   #?(:clj  clojure.lang.Symbol
      :cljs cljs.core.Symbol)   (>form [x] (list 'quote x))

  #?@(:clj [clojure.lang.Fn     (>form [x]
                                  ;; TODO can probably use uconv to good effect here
                                  (or (when-let [ns- (-> x meta :ns)]
                                        (symbol (ns-name ns-) (-> x meta :name name)))
                                      (let [demunged (-> x class .getName
                                                         clojure.lang.Compiler/demunge)]
                                        (if-let [anonymous-fn? (not= (.indexOf     demunged "/")
                                                                     (.lastIndexOf demunged "/"))]
                                          (tagged-literal 'fn (symbol demunged))
                                          (symbol demunged)))))])
  #?@(:clj [Class               (>form [x] (-> x #_uconv/>symbol .getName symbol))]))

(defn core-symbol [env sym] (symbol (str (case-env* env :cljs "cljs" "clojure") ".core") (name sym)))

;; TODO move this code generation code to a different namespace

;; ===== Code quoting ===== ;;

; ------------- SYNTAX QUOTE; QUOTE+ -------------

#?(:clj (defalias syntax-quote clojure.tools.reader/syntax-quote))

#?(:clj
(defn unquote-replacement
  "Replaces each instance of `(clojure.core/unquote <whatever>)` in `quoted-form` with
   the unquoted version of its inner content."
  {:examples '{(unquote-replacement {'a 3} '(+ 1 ~a))
               '(+ 1 3)}}
  [sym-map quoted-form]
  (ucore/prewalk
    (fn [x]
      (if (and (seq? x)
               (-> x count   (= 2))
               (-> x (nth 0) (= 'clojure.core/unquote)))
          (if (contains? sym-map (nth x 1))
              (get sym-map (nth x 1))
              (eval (nth x 1)))
          x))
    quoted-form)))

#?(:clj
(defmacro quote+
  "Normal quoting with unquoting that works as in |syntax-quote|."
  {:examples '{(let [a 1]
                 (quote+ (for [b 2] (inc ~a))))
               '(for [a 1] (inc 1))}}
  [form] `(unquote-replacement (locals) '~form)))

#?(:clj
(defmacro $
  "Reproducibly, unifiedly syntax quote without messing up the format as a literal
   syntax quote might do."
  [body] `(ufgen/unify-gensyms (syntax-quote ~body) true)))
