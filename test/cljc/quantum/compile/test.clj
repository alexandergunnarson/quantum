(ns quantum.compile.test
  (:require
    [quanta.compile.cpp  :as cpp ]
    [quanta.compile.js   :as js  ]
    [quanta.compile.util :as util]
    [quantum.core.macros :as macro :refer [metaclass quote+]]
    [quanta.compile.core :as comp :refer
      [eval-form eval-file mcall oeval demunge-class]]
    [clojure.tools.namespace.repl :refer [refresh refresh-all]])
  (:import
    quanta.compile.core.ObjLangText))

(log/enable! :debug)

; (defn java-emit []
;   (java/emit! []))

; (defn cpp-emit []
;   (cpp/emit! 
;     (do (import '(collections.h print.h string.h log.h io.h))
;         (use '(coll pr))
;         (defmacro DEBUG [] true)
;         (defn ^int main [^int argc ^CString argv]))))

(defn js-test []
  (reset! comp/lang :js)
  (let [^String compiled (eval-file [:test "quanta" "compile" "in.cljs"])]
    (io/write!
      :path         [:resources "test_html" "out.js"]
      :data         compiled
      :write-method :print)
    (println "=====")
    (println compiled)
    (println "=====")))

(defn cs-test []
  (reset! comp/lang :cs)
  (let [^String compiled (eval-file [:test "quanta" "compile" "in.clj"])]
    (io/write!
      :path         [:resources "test_html" "out.cs"]
      :data         compiled
      :write-method :print)
    (println "=====")
    (println compiled)
    (println "=====")))


; Get mail!
; http://www.vipan.com/htdocs/javamail.html