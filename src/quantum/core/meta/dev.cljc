(ns quantum.core.meta.dev
  (:require
    [quantum.core.fn
      :refer [fn1 fn-> fn->>]]
    [quantum.core.logic
      :refer [fn-and]]
    [quantum.core.print]
    [quantum.core.error
      :refer [catch-all]]
    [quantum.core.macros
      :refer [defnt]]
    [quantum.core.type :as t]
    [quantum.core.collections :as coll
      :refer [map+ map-vals+ cat+ nempty?
              filter+ filter-keys+ filter-vals+ remove+ keys+ join seq-and]]))

(def
  ^{:todo {0 "Make all todos part of a queryable DS DB"}}
  annotations nil)

#?(:clj
(defnt qualified-name
  {:todo {0 "Different ns"}}
  ([^clojure.lang.Var x]
    (str (-> x meta :ns ns-name name) "/"
         (-> x meta :name name)))
  ([^clojure.lang.Namespace x]
    (-> x ns-name name))))

#?(:clj
(def todos-map
  (sorted-map-by
    (fn [a b] (compare (qualified-name a)
                       (qualified-name b))))))

#?(:clj
(defn all-todos
  "Evaluates to all the todos in all namespaces, based on the :todo
   metadata of each var."
  []
  (->> (all-ns)
       (map+     (juxt (juxt identity)
                       (fn-> ns-name ns-interns vals)))
       cat+
       cat+
       (map+    (juxt identity #(-> % meta :todo)))
       (remove+ (fn-> second empty?))
       (join    todos-map))))

#?(:clj
(defn all-todos-with-priority
  {:todo  {0 "Remove this once `annotations::0` is done"}
   :usage `(all-todos-with-priority (fn1 > 0.4))}
  [pred]
  (->> (all-todos)
       (filter-vals+ map?)
       (map-vals+    (fn->> (filter-keys+ (fn1 t/integer?))
                            (filter-vals+ (fn-and map?
                                                  (fn1 contains? :priority)
                                                  (fn-> :priority pred)))
                            (join {})))
       (filter-vals+ nempty?)
       (join         todos-map))))

#?(:clj
(defn enable-repl-utils!
  "`find-doc`, `doc`, and `source` are included by default in the `user` namespace,
   but not in any others. This makes it so you can call any of these helpful REPL-centric
   functions in any currently loaded namespace."
  {:todo {0 "Remove `all-todos-with-priority` when deleted"}}
  []
  (let [ns-0 *ns*]
    (require '[clojure.repl :refer [source doc]])
    (doseq [ns' (all-ns)]
      (in-ns (ns-name ns'))
      (catch-all
        (require '[clojure.repl         :refer [source find-doc doc]]
                 '[clojure.java.javadoc :refer [javadoc]]
                 '[quantum.core.meta.dev :refer [all-todos all-todos-with-priority]]
                 '[quantum.core.macros :refer [macroexpand-all!]]
                 '[quantum.core.analyze.clojure.core :refer [typeof*]])))
    (in-ns (ns-name ns-0))
    (clojure.main/repl
      :print  quantum.core.print/!
      :caught quantum.core.print/!))))
