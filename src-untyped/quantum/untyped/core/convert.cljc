(ns quantum.untyped.core.convert
  (:require
    [clojure.string               :as str]
    [quantum.untyped.core.core    :as ucore]
    [quantum.untyped.core.error   :as uerr]
    [quantum.untyped.core.fn      :as ufn]
    [quantum.untyped.core.qualify
      :refer [#?(:cljs DelimitedIdent) delim-ident? named?]]
    [quantum.untyped.core.type.predicates
      :refer [namespace?]])
  #?(:clj (:import quantum.untyped.core.qualify.DelimitedIdent)))

(ucore/log-this-ns)

(defn demunged>namespace [^String s] (subs s 0 (.lastIndexOf s "/")))
(defn demunged>name      [^String s] (subs s (inc (.lastIndexOf s "/"))))

(defn >name
  "Computes the name (the unqualified string identifier) of `x`."
  [x]
  (cond   (named?     x) (name x)
          (string?    x) x
#?@(:clj [(class?     x) (.getName ^Class x)
          (namespace? x) (-> x ns-name name)
          (var?       x) (-> x meta :name name)])
          (fn?        x) #?(:clj  (or (some-> (-> x meta :name) >name)
                                      (-> x class .getName clojure.lang.Compiler/demunge demunged>name))
                            :cljs (when-not (-> x .-name str/blank?)
                                    (-> x .-name demunge-str demunged>name)))
          :else          (uerr/not-supported! `>name x)))

(def >?name (ufn/? >name))

(defn >?namespace
  "Computes the nilable namespace (the string identifier-qualifier) of `x`."
  [x]
  (cond   (named?         x)   (namespace x)
          (or (nil?       x)
              (string?    x)
      #?(:clj (class?     x))
      #?(:clj (namespace? x))) nil
#?@(:clj [(var?           x)   (-> x meta :ns >name)])
          (fn?            x)   #?(:clj  (or (some-> (-> x meta :ns) >name)
                                            (-> x class .getName clojure.lang.Compiler/demunge demunged>namespace))
                                  :cljs (when-not (-> x .-name str/blank?)
                                          (-> x .-name demunge-str demunged>namespace)))
          :else                (uerr/not-supported! `>?namespace x)))

(defn >delim-ident
  "Computes the delimited identifier of `x`."
  [x]
  (cond   (delim-ident? x) x
          (string?      x) (-> x (str/split #"\.|\||/") (DelimitedIdent.))
          (named?       x) (DelimitedIdent.
                             (concat (some-> (namespace x) (str/split #"\.|\||/"))
                                     (-> x >name (str/split #"\.|\||/"))))
#?@(:clj [(class?       x) (>delim-ident (.getName ^Class x))
          (namespace?   x) (-> x >name >delim-ident)
          (var?         x) (DelimitedIdent.
                             (concat (-> x >?namespace (str/split #"\.|\||/"))
                                     (-> x >name       (str/split #"\.|\||/"))))])
          (fn?          x) (DelimitedIdent.
                             #?(:clj  (or (some-> (-> x meta :name) >name (str/split #"\.|\||/"))
                                          (-> x class .getName clojure.lang.Compiler/demunge (str/split #"\.|\||/")))
                                :cljs (if (-> x .-name str/blank?)
                                          ["<anonymous>"]
                                          (-> x .-name demunge-str (str/split #"\.|\||/")))))
          (nil?         x) (uerr/not-supported! `>delim-ident x)
          :else            (-> x str recur)))

(defn >keyword [x]
  (cond (keyword? x) x
        (symbol?  x) (keyword (namespace x) (name x))
        :else        (-> x str keyword)))

(defn >symbol
  "Converts `x` to a symbol (possibly qualified, meta-able identifier)."
  [x]
  (cond   (symbol?    x) x
          (string?    x) (symbol x)
#?@(:clj [(var?       x) (symbol (>?namespace x) (>name x))
          (namespace? x) (ns-name x)])
          (fn? x)        #?(:clj  (or (when-let [ns- (-> x meta :ns)]
                                        (symbol (>name ns-) (-> x meta :name >name)))
                                      (-> x class .getName clojure.lang.Compiler/demunge recur))
                            :cljs (when-not (-> x .-name str/blank?)
                                    (-> x .-name demunge-str recur)))
          :else          (-> x str recur)))

(defn >integer [x]
  (cond (integer? x) x
        (string?  x) #?(:clj  (Long/parseLong ^String x)
                        :cljs (js/parseInt            x))
        :else        (uerr/not-supported! `>integer x)))
