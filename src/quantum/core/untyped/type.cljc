(ns quantum.core.untyped.type
  "Essentially, set-theoretic definitions and operations on types."
  (:refer-clojure :exclude
    [< <= = >= > ==
     and or
     boolean  byte  char  short  int  long  float  double
     boolean? byte? char? short? int? long? float? double?
     nil?
     class? keyword? string? symbol? tagged-literal?
     meta
     assoc-in])
  (:require
    [clojure.core :as c]
    [quantum.core.error :as err
      :refer [->ex TODO]]
    [quantum.core.fn             :as fn]
    [quantum.core.macros.deftype :as dt]
    [quantum.core.type.core      :as tcore]
    [quantum.core.untyped.collections :as coll
      :refer [assoc-in dissoc-in]]
    [quantum.core.untyped.compare :as comp
      :refer [== not==]]
    [quantum.core.vars           :as var]))

#_(defmacro ->
  ("Anything that is coercible to x"
    [x]
    ...)
  ("Anything satisfying `from` that is coercible to `to`.
    Will be coerced to `to`."
    [from to]))

#_(defmacro range-of)

#_(defn instance? [])

(do

;; ===== SPECS ===== ;;

(definterface ISpec)

(dt/deftype ClassSpec
  [meta     #_(t/? ::meta)
   ^Class c #_t/class?
   name     #_(t/? t/symbol?)]
  {ISpec                 nil
   fipp.ednize/IOverride nil
   fipp.ednize/IEdn
     {-edn ([this] (c/or name (list `isa? c)))}
   ?Fn   {invoke    ([_ x] (instance? c x))}
   ?Meta {meta      ([this] meta)
          with-meta ([this meta'] (ClassSpec. meta' c name))}})

(dt/deftype NilableSpec [x]
  {ISpec nil})

(dt/deftype QMark []
  {?Fn {invoke (([_ x] (NilableSpec. x))
                ([_ spec x] (c/or (c/nil? x) (spec x))))}})

(def ^{:doc "Arity 1: Denotes type inference should be performed.
             Arity 2: Denotes a nilable value."}
  ? (QMark.))

(dt/deftype FnSpec
  [name #_(t/? t/symbol?)
   f    #_t/fn?
   form #_(t/? form?)]
  {ISpec nil
   fipp.ednize/IOverride nil
   fipp.ednize/IEdn
     {-edn ([this] (c/or name form (list 't/fn-spec f)))}
   ?Fn {invoke ([_ x] (f x))}})

(defonce *spec-registry (atom {}))

(defn ^ISpec ->spec
  "Coerces ->`x` to a spec, recording its ->`name-sym` if provided."
  ([x] (->spec x nil))
  ([x name-sym]
    (assert (? symbol? name-sym))
    (cond (instance? ISpec x)
            x ; TODO should add in its name?
          (c/class? x)
            (let [reg (swap! *spec-registry
                        (fn [reg]
                          (if (nil? name-sym)
                              reg
                              (if-let [spec (get reg name-sym)]
                                (if (= (.-name ^ClassSpec spec) name-sym)
                                    reg
                                    (throw (->ex "Class already registered with spec; must first undef" {:class x :spec-name name-sym})))
                                (let [spec (ClassSpec. nil x name-sym)]
                                  (assoc-in reg [name-sym]    spec
                                                [:by-class x] spec))))))]
              (or (get-in reg [:by-class x])
                  (ClassSpec. nil ^Class x name-sym)))
          (c/fn? x)
            (FnSpec. name-sym ^clojure.lang.Fn x nil)
          :else
            (throw (->ex "Cannot coerce to spec" {:x x :type (type x) :name name-sym})))))

;; ===== AND ===== ;;

(dt/deftype AndSpec [args #_(t/and t/indexed? (t/seq-of spec?))]
  {ISpec                 nil
   fipp.ednize/IOverride nil
   fipp.ednize/IEdn
     {-edn ([this] (list* `and args))}
   ?Fn {invoke ([_ x] (reduce (fn [_ pred] (c/or (pred x) (reduced false)))
                        true ; vacuously
                        args))}})

(defn and
  "Sequential/ordered `and`."
  [& args]
  (AndSpec. (mapv ->spec args)))

(deftype UnorderedAndSpec [args #_(t/and t/indexed? (t/seq-of spec?))]
  ISpec
  fipp.ednize/IOverride
  fipp.ednize/IEdn (-edn [this] (list* `and* args)))

(defn and*
  "Unordered `and`. Analogous to `set/intersection`.
   Applies 'compression'/deduplication to the supplied specs.
   Effectively computes the intersection of the intension of the ->`args`."
  [& args]
  (TODO "and*"))

;; ===== OR ===== ;;

(dt/deftype OrSpec [args #_(t/and t/indexed? (t/seq-of spec?))]
  {ISpec                 nil
   fipp.ednize/IOverride nil
   fipp.ednize/IEdn
     {-edn ([this] (list* `or args))}
   ?Fn {invoke ([_ x] (reduce (fn [_ pred] (c/and (pred x) (reduced x)))
                        true ; vacuously
                        args))}})

(defn or
  "Sequential/ordered `or`."
  [& args]
  (OrSpec. (mapv ->spec args)))

(deftype UnorderedOrSpec [args #_(t/and t/indexed? (t/seq-of spec?))]
  ISpec
  fipp.ednize/IOverride
  fipp.ednize/IEdn (-edn [this] (list* `or* args)))

(defn or*
  "Unordered `or`. Analogous to `set/union`.
   Applies 'compression'/deduplication to the supplied specs.
   Effectively computes the union of the intension of the ->`args`."
  [& args]
  (TODO "or*"))

#?(:clj
(defmacro spec
  "Creates a spec function"
  [arglist & body] ; TODO spec this
  `(FnSpec. nil (fn ~arglist ~@body) (list* `spec '~arglist '~body))))

(deftype FnConstantlySpec
  [name         #_(t/? t/symbol?)
   f            #_t/fn?
   inner-object #_t/_]
  ISpec
  fipp.ednize/IOverride
  fipp.ednize/IEdn
    (-edn [this] (c/or name (list `fn' inner-object))))

#?(:clj
(defmacro fn' [x]
  `(let [x# ~x] (FnConstantlySpec. nil (fn/fn' x#) x#))))

;; ===== DEFINITIONS ===== ;;

(defmacro def [sym specable]
  `(~'def ~sym (->spec ~specable '~(var/qualify sym))))

(defn undef [reg sym]
  (if-let [spec (get reg sym)]
    (let [reg' (dissoc reg sym)]
      (if (instance? ClassSpec spec)
          (dissoc-in reg' [:by-class (.-c ^ClassSpec spec)])))
    reg)
  )

(defn undef! [sym] (swap! *spec-registry undef sym))

(defmacro defalias [sym spec]
  `(~'def ~sym (->spec ~spec)))

(var/defalias -def def)

        (-def boolean #?(:clj Boolean/TYPE :cljs js/Boolean))
        (defalias boolean? boolean)
#?(:clj (-def byte    Byte       ))
#?(:clj (defalias byte?    byte  ))
#?(:clj (-def char    Character  ))
#?(:clj (defalias char?    char  ))
#?(:clj (-def short   Short      ))
#?(:clj (defalias short?   short ))
#?(:clj (-def int     Integer    ))
#?(:clj (defalias int?     int   ))
#?(:clj (-def long    Long       ))
#?(:clj (defalias long?    long  ))
#?(:clj (-def float   Float      ))
#?(:clj (defalias float?   float ))
        (-def double  #?(:clj Double :cljs js/Number))
        (defalias double?  double)

        (-def nil?    c/nil?        )

#?(:clj (-def class?   java.lang.Class))
        (-def string?  #?(:clj java.lang.String     :cljs js/String))
        (-def keyword? #?(:clj clojure.lang.Keyword :cljs cljs.core/Keyword))
        (-def symbol?  #?(:clj clojure.lang.Symbol  :cljs cljs.core/Symbol))
#?(:clj (-def tagged-literal? clojure.lang.TaggedLiteral))

(-def literal? (or nil? symbol? keyword? string? long? double? tagged-literal?))
#_(t/def ::form    (t/or ::literal t/list? t/vector? ...))

;; ===== EXTENSIONALITY/INTENSIONALITY COMPARISON IMPLEMENTATIONS ===== ;;

#_(is (coll&/incremental-every? (aritoid nil (constantly true) t/in>)
        [String Comparable Object])
      (coll&/incremental-every? (aritoid nil (constantly true) t/in>)
        [Long Number]))

#?(:clj
(defn compare-classes
  "Compare specificity|generality / extension|intension of ->`c0` to ->`c1`.
   `0`  means they are equally specific:
     - ✓ `(t/in= c0 c1)` : the intension of ->`c0` is equal to             that of ->`c1`.
     - ✓ `(t/ex= c0 c1)` : the extension of ->`c0` is equal to             that of ->`c1`.
   `-1` means ->`c0` is less specific (more general) than ->`c1`:
     - ✓ `(t/in< c0 c1)` : the intension of ->`c0` is a strict subset   of that of ->`c1`.
     - ✓ `(t/ex> c0 c1)` : the extension of ->`c0` is a strict superset of that of ->`c1`.
   `1`  means ->`c0` is more specific (less general) than ->`c1`.
     - ✓ `(t/in> c0 c1)` : the intension of ->`c0` is a strict superset of that of ->`c1`.
     - ✓ `(t/ex< c0 c1)` : the extension of ->`c0` is a strict subset   of that of ->`c1`.
   Unboxed primitives are considered to be more specific than boxed primitives."
  [^Class c0 ^Class c1]
  (cond (== c0 c1)
        0
        (== c0 Object)
        -1
        (== c1 Object)
        1
        (== (tcore/boxed->unboxed c0) c1)
        -1
        (== c0 (tcore/boxed->unboxed c1))
        1
        (c/or (tcore/primitive-array-type? c0) (tcore/primitive-array-type? c1))
        nil ; we'll consider the two unrelated
        (.isAssignableFrom c0 c1)
        -1
        (.isAssignableFrom c1 c0)
        1
        :else nil))) ; unrelated

;; ===== SPEC INTENSIONALITY ===== ;;

(defn #_long in:compare ; TODO for some reason primitive type hints break it for the time being
  ;; TODO optimize the `recur`s here as they re-take old code paths
  [s0 s1]
  (cond (c/class? s0)
          (cond (c/class? s1)
                  ;; defaults to `-1` instead of `nil`
                  (compare-classes s0 s1)
                (instance? ClassSpec s1)
                  (recur s0 (.-c ^ClassSpec s1))
                :else
                  (TODO "handle"))
        (instance? ClassSpec s0)
          (recur (.-c ^ClassSpec s0) s1)
        :else
          (TODO "handle")))

(defn in:boolean-compare
  "Incomparables return `false` for the boolean comparator `pred`."
  [pred s0 s1]
  (let [ret (in:compare s0 s1)]
    (if (c/nil? ret) false (pred ret 0))))

(defn in<
  "Computes whether the intension of spec ->`s0` is a strict subset of that of ->`s1`."
  [s0 s1] (in:boolean-compare c/< s0 s1))

(defalias < in<)

(defn in<=
  "Computes whether the intension of spec ->`s0` is a (lax) subset of that of ->`s1`."
  [s0 s1] (in:boolean-compare c/<= s0 s1))

(defalias <= in<=)

(defn in=
  "Computes whether the intension of spec ->`s0` is equal to that of ->`s1`."
  [s0 s1] (in:boolean-compare c/= s0 s1))

(defalias = in=)

(defn in>=
  "Computes whether the intension of spec ->`s0` is a (lax) superset of that of ->`s1`."
  [s0 s1] (in:boolean-compare c/>= s0 s1))

(defalias >= in>=)

(defn in>
  "Computes whether the intension of spec ->`s0` is a strict superset of that of ->`s1`."
  [s0 s1] (in:boolean-compare c/> s0 s1))

(defalias > in>)

;; ===== SPEC EXTENSIONALITY ===== ;;

(defn ex<
  "Computes whether the extension of spec ->`s0` is a strict subset of that of ->`s1`."
  [s0 s1] (TODO "<"))

(defn ex<=
  "Computes whether the extension of spec ->`s0` is a (lax) subset of that of ->`s1`."
  [s0 s1] (TODO "<="))

(defn ex=
  "Computes whether the extension of spec ->`s0` is equal to that of ->`s1`."
  [s0 s1] (TODO "="))

(defn ex>=
  "Computes whether the extension of spec ->`s0` is a (lax) superset of that of ->`s1`."
  [s0 s1] (TODO ">="))

(defn ex>
  "Computes whether the extension of spec ->`s0` is a strict superset of that of ->`s1`."
  [s0 s1] (TODO ">"))

)
