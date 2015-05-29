(ns
  ^{:doc "ClojureScript versions of macros.
          Annoying that this has to be done this way."}
  quantum.core.cljs.error
  (:require-quantum [ns fn logic])
  (:require
    [quantum.core.error :as err]
    [fipp.edn]
    [clojure.walk :refer [postwalk]]))

(def sym-table
  '{java.lang.Throwable js/Error
    Throwable js/Error})

#?(:clj 
(defmacro try+ [& args]
  (let [code (first `((quantum.core.error/try+* :cljs ~@args)))]
    (println "/* FINAL CODE IS")
    (fipp.edn/pprint code)
    (println "*/")
    code)))