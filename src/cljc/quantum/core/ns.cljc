(ns
  ^{:doc "Useful namespace and var-related functions."
    :attribution "Alex Gunnarson"}
  quantum.core.ns
  (:require [clojure.set :as set]))

#?(:clj
(defmacro search-var
  "Searches for a var @var0 in the available namespaces."
  {:usage '(ns-find abc)
   :todo ["Make it better and filter out unnecessary results"]
   :attribution "Alex Gunnarson"}
  [var0]
 `(->> (all-ns)
       (map ns-publics)
       (filter
         (fn [obj#]
           (->> obj#
                keys
                (map name)
                (apply str)
                (re-find (re-pattern (str '~var0)))))))))

#?(:clj
(defmacro ns-exclude [& syms]
  `(doseq [sym# '~syms]
     (ns-unmap *ns* sym#))))

#?(:clj
(defmacro with-ns
  "Perform an operation in another ns."
  {:todo ["Dubious as to whether this actually works."]}
  [ns- & body]
  (let [ns-0 (ns-name *ns*)]
    `(do (in-ns ~ns-) ~@body (in-ns ~ns-0)))))

#?(:clj
(defmacro with-temp-ns
  "Evaluates @exprs in a temporarily-created namespace.
  All created vars will be destroyed after evaluation."
  {:source "zcaudate/hara.namespace.eval"}
  [& exprs]
  `(try
     (create-ns 'sym#)
     (let [res# (with-ns 'sym#
                  (clojure.core/refer-clojure)
                  ~@exprs)]
       res#)
     (finally (remove-ns 'sym#)))))

#?(:clj
(defmacro import-static
  "Imports the named static fields and/or static methods of the class
  as (private) symbols in the current namespace.
  Example: 
      user=> (import-static java.lang.Math PI sqrt)
      nil
      user=> PI
      3.141592653589793
      user=> (sqrt 16)
      4.0
  Note: The class name must be fully qualified, even if it has already
  been imported.  Static methods are defined as MACROS, not
  first-class fns."
  {:source "Stuart Sierra, via clojure.clojure-contrib/import-static"}
  [class & fields-and-methods]
  (let [only (set (map str fields-and-methods))
        ^Class the-class (Class/forName (str class))
        static?       (fn [^java.lang.reflect.Member x]
                        (-> x .getModifiers java.lang.reflect.Modifier/isStatic))
        statics       (fn [array]
                        (set (map (fn [^java.lang.reflect.Member x] (.getName x))
                                  (filter static? array))))
        all-fields    (-> the-class .getFields  statics)
        all-methods   (-> the-class .getMethods statics)
        fields-to-do  (set/intersection all-fields  only)
        methods-to-do (set/intersection all-methods only)
        make-sym (fn [string]
                     (with-meta (symbol string) {:private true}))
        import-field (fn [name]
                         (list 'def (make-sym name)
                               (list '. class (symbol name))))
        import-method (fn [name]
                          (list 'defmacro (make-sym name)
                                '[& args]
                                (list 'list ''. (list 'quote class)
                                      (list 'apply 'list
                                            (list 'quote (symbol name))
                                            'args))))]
    `(do ~@(map import-field fields-to-do)
         ~@(map import-method methods-to-do)))))


