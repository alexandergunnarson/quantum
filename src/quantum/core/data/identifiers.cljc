(ns quantum.core.data.identifiers
  "Functions related to variable identifiers/names (`name`, `namespace`, etc.) and qualification /
   unqualification of nameables."
  (:refer-clojure :exclude
    [keyword? symbol?])
  (:require
    [quantum.core.data.meta
      :refer [>meta]]
    [quantum.core.data.string  :as dstr
      :refer [>string]]
    [quantum.core.type         :as t]
    [quantum.untyped.core.core :as ucore]))

(ucore/log-this-ns)

;; ===== Standard identifiers ===== ;;

(def keyword? (t/isa? #?(:clj clojure.lang.Keyword :cljs cljs.core/Keyword)))

(def symbol? (t/isa? #?(:clj clojure.lang.Symbol :cljs cljs.core/Symbol)))

(def ident? (t/or keyword? symbol?))

;; ===== Nameability ===== ;;

(def named? (t/isa?|direct #?(:clj clojure.lang.Named :cljs cljs.core/INamed)))

(t/defn demunged>namespace [s dstr/string?] TODO TYPED #_(subs s 0 (.lastIndexOf s "/")))
(t/defn demunged>name      [s dstr/string?] TODO TYPED #_(subs s (inc (.lastIndexOf s "/"))))

(defn... ?ns>name [?ns]
  (name #?(:clj (if (namespace? ?ns)
                    (ns-name ?ns)
                    ?ns)
           :cljs ?ns)))

(t/defn >name
  "Computes the nilable name (the unqualified string identifier) of `x`."
  > (t/? dstr/string?)
        (^:inline [x (t/or t/nil? dstr/string?)] x)
        (^:inline [x named?] #?(:clj (.getName x) :cljs (cljs.core/-name x)))
#?(:clj (^:inline [x ??/class?] (.getName x)))
        (         [x ??/fn?]
          #?(:clj  (or (some-> (-> >meta :name) >name)
                       (-> x ??/>class >name clojure.lang.Compiler/demunge demunged>name))
             :cljs (when-not (-> x .-name ???str/blank?)
                     (-> x .-name ??/demunge-str demunged>name)))))

(t/defn >namespace
  "Computes the nilable identifier-namespace (the string identifier-qualifier) of `x`."
  > (t/? dstr/string?)
        (^:inline [x (t/or t/nil? dstr/string? #?(:clj ??/class?) #?(:clj ??/namespace?))] nil)
        (^:inline [x named?] #?(:clj (.getNamespace x) :cljs (cljs.core/-namespace x)))
        (         [x ??/fn?]
          #?(:clj  (or (some-> (-> x >meta :ns) >name)
                       (-> x ??/>class .getName clojure.lang.Compiler/demunge demunged>namespace))
             :cljs (when-not (-> x .-name ???str/blank?)
                     (-> x .-name ??/demunge-str demunged>namespace)))))

;; ===== Qualification ===== ;;

(t/defn qualify > symbol?
#?(:clj ([sym    symbol?] (qualify *ns* sym)))
        ([ns-sym symbol?       sym symbol?] (>symbol (>name ns-sym) (>name sym)))
#?(:clj ([ns-val ??/namespace? sym symbol?] (>symbol (>name ns-val) (>name sym)))))

(t/defn qualify|dot > symbol? [sym symbol? ns-val ??/namespace?]
  (>symbol (>str (?ns>name ns-val) "." (>name sym))))

#?(:clj (defn... qualify|class [sym] (symbol (str (-> *ns* ns-name name munge) "." sym))))

(t/defn unqualify > symbol? [sym symbol?] (-> sym >name >symbol))

(t/defn unqualified? [x (t/input-type >namespace t/?)] (-> x >namespace t/nil?))
(t/defn qualified?   [x (t/input-type >namespace t/?)] (-> x >namespace t/val?))

(def unqualified-keyword? (t/and keyword? unqualified?))
(def qualified-keyword?   (t/and keyword? qualified?))
(def unqualified-symbol?  (t/and symbol?  unqualified?))
(def qualified-symbol?    (t/and symbol?  qualified?))
(def unqualified-ident?   (t/and symbol?  unqualified?))
(def qualified-ident?     (t/and symbol?  qualified?))

#?(:clj
(defn... collapse-symbol
  ([sym symbol?] (collapse-symbol sym true))
  ([sym symbol? extra-slash? p/boolean?]
    (>symbol
      (when-let [n (>namespace sym)]
        (when-not (= n (-> *ns* ns-name name))
          (if-let [alias- (do #?(:clj (??/ns-name>alias *ns* (>symbol n)) :cljs false))]
            (str alias- (when extra-slash? "/"))
            n)))      (name sym)))))

;; ===== Standard identifiers ===== ;;

(t/defn >keyword
  "Outputs a keyword with the given (optional) namespace and name.
   Do not use `:` in keyword strings; it will be added automatically."
  > keyword?
  ([x keyword?] x)
  ([x symbol?] #?(:clj  (clojure.lang.Keyword/intern x)
                  :cljs (cljs.core/Keyword. (>namespace x) (>name x) (.-str x) nil)))
  ([x dstr/string?] #?(:clj  (clojure.lang.Keyword/intern x)
                             ;; TODO TYPED below
                       :cljs (let [parts (.split x "/")]
                               (if (== (alength parts) 2)
                                   (cljs.core/Keyword. (aget parts 0) (aget parts 1) x nil)
                                   (cljs.core/Keyword. nil (aget parts 0) x nil)))))
  ([ns-str t/nil?, name-str dstr/string?]
    #?(:clj  (clojure.lang.Keyword/intern ns-str name-str)
       :cljs (cljs.core/Keyword. ns-str name-str name-str nil)))
  ([ns-str dstr/string?, name-str dstr/string?]
    #?(:clj  (clojure.lang.Keyword/intern ns-str name-str)
       :cljs (cljs.core/Keyword. ns-str name-str (>str ns-str "/" name-str) nil))))

(t/defn >symbol
  "Outputs a symbol (possibly qualified, meta-able identifier)."
  > symbol?
  ([x symbol?] x)
  ([x dstr/string?] #?(:clj  (clojure.lang.Symbol/intern x)
                       ;; TODO TYPED below
                       :cljs (let [i (.indexOf x "/")]
                         (if (< i 1)
                             (>symbol nil x)
                             (>symbol (.substring x 0 i)
                                      (.substring x (inc i) (.-length x)))))))
  ([x keyword?] (>symbol (>namespace x) (>name x)))
  ([ns-str t/nil?, name-str dstr/string?]
    #?(:clj  (clojure.lang.Symbol/intern ns-str name-str)
       :cljs (cljs.core/Symbol. ns-str name-str name-str nil nil)))
  ([ns-str dstr/string?, name-str dstr/string?]
    #?(:clj  (clojure.lang.Symbol/intern ns-str name-str)
       :cljs (cljs.core/Symbol. ns-str name-str (>str ns-str "/" name-str) nil nil)))

;; TODO TYPED incorporate this into `>symbol`
  (cond
#?@(:clj [(class?     x) (-> x >name symbol)
          (namespace? x) (ns-name x)])
          (fn? x)        #?(:clj  (or (when-let [ns- (-> x >meta :ns)]
                                        (symbol (>name ns-) (-> x >meta :name >name)))
                                      (-> x class .getName clojure.lang.Compiler/demunge recur))
                            :cljs (when-not (-> x .-name str/blank?)
                                    (-> x .-name demunge-str recur)))
          :else          (-> x str recur)))

;; ===== UUIDs ===== ;;

(def uuid? (t/isa?|direct #?(:clj java.util.UUID :cljs cljs.core/UUID)))

(t/defn >uuid > uuid?
  ([]
  #?(:clj  (java.util.UUID/randomUUID)
           ;; TODO TYPED below
     :cljs (letfn [(hex [] (.toString (rand-int 16) 16))]
             (let [rhex (.toString (bit-or 0x8 (bit-and 0x3 (rand-int 16))) 16)]
               (>uuid
                 (str (hex) (hex) (hex) (hex)
                      (hex) (hex) (hex) (hex) "-"
                      (hex) (hex) (hex) (hex) "-"
                      "4"   (hex) (hex) (hex) "-"
                      rhex  (hex) (hex) (hex) "-"
                      (hex) (hex) (hex) (hex)
                      (hex) (hex) (hex) (hex)
                      (hex) (hex) (hex) (hex)))))))
  #?(:cljs ([x dstr/string?] (cljs.core/UUID. (.toLowerCase s) nil))))

;; ===== Delimited identifiers  ===== ;;

;; TODO TYPED `t/defrecord`
(defrecord
  ^{:doc "A delimited identifier.
          Defaults to delimiting all qualifiers by the pipe symbol instead of slashes or dots."}
  DelimitedIdent [qualifiers #_(t/of (t/and dstr/string? (t/not (fn1 contains? \|))))]
  fipp.ednize/IOverride
  fipp.ednize/IEdn
    (-edn [this] (tagged-literal '| (>symbol (??str/join "|" qualifiers)))))

(def delim-ident? (t/isa? DelimitedIdent))

(t/defn >delim-ident
  "Computes the delimited identifier of `x`."
        ([x delim-ident?] x)
        ([x dstr/string?] (-> x (??str/split #"\.|\||/") (DelimitedIdent.)))
        ([x named?] (DelimitedIdent.
                      (??/concat (some-> (>namespace x) (??str/split #"\.|\||/"))
                                 (-> x >name (??str/split #"\.|\||/")))))
#?(:clj ([x ??/class?] (-> x >name >delim-ident)))
        ([x t/val?] (-> x >str >delim-ident)))

;; TODO TYPED incorporate into `>delim-ident`
(namespace?   x) (-> x >name >delim-ident)
(var?         x) (DelimitedIdent.
                   (concat (-> x >namespace (str/split #"\.|\||/"))
                           (-> x >name      (str/split #"\.|\||/"))))
(fn?          x) (DelimitedIdent.
                   #?(:clj  (or (some-> (-> x >meta :name) >name (str/split #"\.|\||/"))
                                (-> x class .getName clojure.lang.Compiler/demunge (str/split #"\.|\||/")))
                      :cljs (if (-> x .-name str/blank?)
                                ["<anonymous>"]
                                (-> x .-name demunge-str (str/split #"\.|\||/")))))
