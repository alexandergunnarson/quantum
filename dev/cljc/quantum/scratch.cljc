(ns quantum.scratch
  (:refer-clojure :exclude [when-let])
  (:require
    #?(:clj [clojure.repl
              :refer [source]])
            [#?(:clj  clojure.test  
                :cljs cljs.test) :as test]
            [#?(:clj  clojure.pprint 
                :cljs cljs.pprint)
              :refer [pprint]]
    #?(:clj [clojure.data
              :refer [diff]])
            [quantum.core.logic
              :refer        [#?@(:clj [when-let])]
              :refer-macros [when-let]]
            [quantum.core.error
              :refer [->ex]]))

#?(:clj
(defn throw-on-test-fail [ns-sym]
  (when-let [{:keys [fail error] :as tested} (test/run-tests ns-sym)
             fail? (or (pos? fail) (pos? error))]
    (throw (->ex :tests-failed "Failed tests" tested)))))

(defn pr*
  "`pprint`s a collection, optionally preceded by a `println`-ed message."
  ([coll]   (pprint coll))
  ([x coll] (println x)
            (pprint coll)))

; Overrides clojure.test/report for prettier / easier-to-read-and-debug messages. Yay!
#?(:clj
(defmethod test/report :fail [m]
  (test/with-test-out
    (test/inc-report-counter :fail)
    (println "\nFAIL in" (test/testing-vars-str m))
    (when (seq test/*testing-contexts*)
      (println (->> test/*testing-contexts*
                    (interpose " > ")
                    (apply str))))
    (when-let [message (:message m)] (println message))
    (pr* "----- expected: -----" (:expected m))
    (pr* "----- actual: -----"   (:actual   m)))
    #_(pr* "----- diff: -----"     (diff (:expected m) (:actual m)))
    (println "==========")))

#?(:clj 
(defn retest []
  (try #_(quantum.core.log/enable! :macro-expand-protocol)
       (remove-ns 'quantum.test.core.error)
       (load-file "./test/cljc/quantum/test/core/error.cljc")
       (throw-on-test-fail 'quantum.test.core.error)

       (remove-ns 'quantum.core.macros.protocol)
       (load-file "./src/cljc/quantum/core/macros/protocol.cljc")
       (remove-ns 'quantum.test.core.macros.protocol)
       (load-file "./test/cljc/quantum/test/core/macros/protocol.cljc")
       (throw-on-test-fail 'quantum.test.core.macros.protocol)

       (remove-ns 'quantum.core.macros.transform)
       (load-file "./src/cljc/quantum/core/macros/transform.cljc")
       (remove-ns 'quantum.test.core.macros.transform)
       (load-file "./test/cljc/quantum/test/core/macros/transform.cljc")
       (throw-on-test-fail 'quantum.test.core.macros.transform)

       (remove-ns 'quantum.core.macros.defnt)
       (load-file "./src/cljc/quantum/core/macros/defnt.cljc")
       (remove-ns 'quantum.test.core.macros.defnt)
       (load-file "./test/cljc/quantum/test/core/macros/defnt.cljc")
       (throw-on-test-fail 'quantum.test.core.macros.defnt)
    (catch Throwable e (clj-stacktrace.repl/pst e)))))

#_(retest)