(ns quantum.untyped.core.form
  (:require
    [quantum.untyped.core.collections
      :refer [seq=]]
    [quantum.untyped.core.core          :as ucore
      :refer [defalias]]
    [quantum.untyped.core.form.evaluate
      :refer [case-env*]]
    [quantum.untyped.core.form.generate :as ufgen]
    [quantum.untyped.core.vars          :as uvar]))

(ucore/log-this-ns)

;; TODO this semi-interacts with `print-dup` in the context of macros; unify them
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
   #?(:clj  java.lang.Boolean
      :cljs boolean)            (>form [x] x)
            ;; If a byte etc. is emitted from a macro, then it emits
            ;; `RT.readString("#=(java.lang.Byte. \"1\")")`. Below is a better way.
  #?@(:clj [java.lang.Byte      (>form [x] (list `unchecked-byte  (long x)))
            java.lang.Short     (>form [x] (list `unchecked-short (long x)))
            java.lang.Character (>form [x] (list `unchecked-char  (long x)))
            java.lang.Integer   (>form [x] (list `unchecked-int   (long x)))
            java.lang.Long      (>form [x] x)
            java.lang.Float     (>form [x] (list `unchecked-float (long x)))])
   #?(:clj  java.lang.Double
      :cljs number)             (>form [x] x)
   #?(:clj  java.lang.String
      :cljs string)             (>form [x] x)
   #?(:clj  clojure.lang.Symbol
      :cljs cljs.core/Symbol)   (>form [x] (list 'quote x))
   #?(:clj  clojure.lang.Keyword
      :cljs cljs.core/Keyword)  (>form [x] x)

   #?(:clj  clojure.lang.PersistentArrayMap
      :cljs cljs.core/PersistentArrayMap)
     (>form [x] (->> x (map (fn [[k v]] [(>form k) (>form v)])) (into (array-map))))

   #?(:clj  clojure.lang.PersistentHashMap
      :cljs cljs.core/PersistentHashMap)
     (>form [x] (->> x (map (fn [[k v]] [(>form k) (>form v)])) (into (hash-map))))

   #?(:clj  clojure.lang.PersistentVector
      :cljs cljs.core/PersistentVector)
     (>form [x] (->> x (mapv >form)))

   #?(:clj  clojure.lang.PersistentList
      :cljs cljs.core/PersistentList)
     (>form [x] (->> x (map >form) list*))

  #?@(:clj [clojure.lang.PersistentList$EmptyList (>form [x] '())])
  #?@(:clj [clojure.lang.ASeq    (>form [x] (->> x (map >form)))])
  #?@(:clj [clojure.lang.LazySeq (>form [x] (->> x (map >form)))])

   #?(:clj  clojure.lang.Var
      :cljs cljs.core/Var)
     (>form [x] #?(:clj  (list 'var (symbol (-> x .-ns ns-name name) (-> x .-sym name)))
                   :cljs (.-sym x)))

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

(defn code=
  "Ensures that two pieces of code are equivalent.
   This means ensuring that seqs, vectors, and maps are only allowed to be compared with
   each other, and that metadata (minus line and column metadata) is equivalent."
  ([c0 c1]
    (if (uvar/metable? c0)
        (and (uvar/metable? c1)
             (= (-> c0 meta (or {}) (dissoc :line :column))
                (-> c1 meta (or {}) (dissoc :line :column)))
             (let [similar-class?
                     (cond (seq?    c0) (seq?    c1)
                           (seq?    c1) (seq?    c0)
                           (vector? c0) (vector? c1)
                           (vector? c1) (vector? c0)
                           (map?    c0) (map?    c1)
                           (map?    c1) (map?    c0)
                           (set?    c0) (set?    c1)
                           (set?    c1) (set?    c0)
                           :else        ::not-applicable)]
               (if (= similar-class? ::not-applicable)
                   (= c0 c1)
                   (and similar-class?
                        (if (or (set? c0) (map? c0))
                            (seq= (sort c0) (sort c1) code=)
                            (seq= (seq  c0) (seq  c1) code=))))))
        (and (not (uvar/metable? c1))
             (= c0 c1))))
  ([c0 c1 & cs] (and (code= c0 c1) (every? #(code= c0 %) cs))))
