#_(quantum.core.log/disable! :macro-expand)
#_(quantum.core.ns/load-nss
  '[quantum.core.macros.defnt
    quantum.test.core.macros.defnt])
#_(require '[clojure.tools.namespace.repl :refer [refresh]]
         '[clojure.test :as test]
         '[quantum.core.logic
            :refer [when-let]]
         '[quantum.core.error
            :refer [->ex]])
#_(refresh)

(defn throw-on-test-fail [ns-sym]
  (when-let [{:keys [fail error] :as tested} (test/run-tests ns-sym)
             fail? (or (pos? fail) (pos? error))]
    (throw (->ex :tests-failed "Failed tests" tested))))

(defn retest []
  (try (quantum.core.log/enable! :macro-expand-protocol)
       (remove-ns 'quantum.test.core.error)
       (load-file "./test/cljc/quantum/test/core/error.cljc")
       (throw-on-test-fail 'quantum.test.core.error)

       (remove-ns 'quantum.core.macros.protocol)
       (load-file "./src/cljc/quantum/core/macros/protocol.cljc")
       (remove-ns 'quantum.test.core.macros.protocol)
       (load-file "./test/cljc/quantum/test/core/macros/protocol.cljc")
       (throw-on-test-fail 'quantum.test.core.macros.protocol)

       (remove-ns 'quantum.core.macros.defnt)
       (load-file "./src/cljc/quantum/core/macros/defnt.cljc")
       (remove-ns 'quantum.test.core.macros.defnt)
       (load-file "./test/cljc/quantum/test/core/macros/defnt.cljc")
       (throw-on-test-fail 'quantum.test.core.macros.defnt)

       (throw (Exception. "Don't continue yet") ) 
    (catch Throwable e (clj-stacktrace.repl/pst e))))

(retest)