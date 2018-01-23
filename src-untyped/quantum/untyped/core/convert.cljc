(ns quantum.untyped.core.convert
  (:require
    [clojure.string               :as str]
    [quantum.core.error           :as err]
    [quantum.core.fn              :as fn]
    [quantum.core.vars            :as var
      :refer [defalias]]
    [quantum.untyped.core.core    :as qcore
      :refer [namespace?]]
    [quantum.untyped.core.qualify :as qual
      :refer [#?(:cljs DelimitedIdent) delim-ident? named?]])
  #?(:clj (:import quantum.untyped.core.qualify.DelimitedIdent)))

(defn demunged->namespace [^String s] (subs s 0 (.lastIndexOf s "/")))
(defn demunged->name      [^String s] (subs s (inc (.lastIndexOf s "/"))))

(defn >name
  "Computes the name (the unqualified string identifier) of `x`."
  [x]
  (cond   (named?     x) (name x)
          (string?    x) x
#?@(:clj [(class?     x) (.getName ^Class x)
          (namespace? x) (-> x ns-name name)
          (var?       x) (-> x meta :name)])
          (fn?        x) #?(:clj  (or (some-> (-> x meta :name) >name)
                                      (-> x class .getName clojure.lang.Compiler/demunge demunged->name))
                            :cljs (when-not (-> x .-name str/blank?)
                                    (-> x .-name demunge-str demunged->name)))
          :else          (err/not-supported! `>name x)))

(def >?name (fn/? >name))

(defn >?namespace
  "Computes the nilable namespace (the string identifier-qualifier) of `x`."
  [x]
  (cond   (named?         x)  (namespace x)
          (or (nil?       x)
              (string?    x)
              (class?     x)
              (namespace? x)) nil
#?@(:clj [(var?           x)  (-> x meta :ns >name)])
          (fn?            x)  #?(:clj  (or (some-> (-> x meta :ns) >name)
                                           (-> x class .getName clojure.lang.Compiler/demunge demunged->namespace))
                                 :cljs (when-not (-> x .-name str/blank?)
                                         (-> x .-name demunge-str demunged->namespace)))
          :else               (err/not-supported! `>?namespace x)))

(defn >delim-ident
  "Computes the delimited identifier of `x`."
  [x]
  (cond   (delim-ident? x)  x
          (string?      x)  (-> x (str/split #"\.|\||/") (DelimitedIdent.))
          (named?       x)  (DelimitedIdent.
                              (concat (some-> (namespace x) (str/split #"\.|\||/"))
                                      (-> x >name (str/split #"\.|\||/"))))
#?@(:clj [(class?       x)  (>delim-ident (.getName ^Class x))
          (namespace?   x)  (-> x >name >delim-ident)
          (var?         x)  (DelimitedIdent.
                              (concat (-> x >?namespace (str/split #"\.|\||/"))
                                      (-> x >name       (str/split #"\.|\||/"))))])
          (fn?          x)  (DelimitedIdent.
                              #?(:clj  (or (some-> (-> x meta :name) >name (str/split #"\.|\||/"))
                                           (-> x class .getName clojure.lang.Compiler/demunge (str/split #"\.|\||/")))
                                 :cljs (if (-> x .-name str/blank?)
                                           ["<anonymous>"]
                                           (-> x .-name demunge-str (str/split #"\.|\||/")))))
          (nil?         x)  (err/not-supported! `>delim-ident x)
          :else             (-> x str recur)))

(defalias >keyword qcore/>keyword)

(defn >symbol
  "Converts `x` to a symbol (possibly qualified, meta-able identifier)."
  [x]
  (cond   (symbol?    x) x
          (string?    x) (symbol x)
#?@(:clj [(namespace? x) (ns-name x)])
          (fn? x)        #?(:clj  (or (when-let [ns- (-> x meta :ns)]
                                        (symbol (>name ns-) (-> x meta :name >name)))
                                      (-> x class .getName clojure.lang.Compiler/demunge recur))
                            :cljs (when-not (-> x .-name str/blank?)
                                    (-> x .-name demunge-str recur)))
          :else          (-> x str recur)))
