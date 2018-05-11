(ns quantum.core.test
  (:require
    [clojure.test       :as test]
    [quantum.core.error :as err]
    [quantum.core.fn    :as fn
      :refer [fn->]]
    [quantum.core.print :as pr
      :refer [ppr-meta]]
    [quantum.core.vars
      :refer [#?(:clj defmalias) defalias]]
    [quantum.untyped.core.test :as utest]
    [quantum.untyped.core.type.predicates
      :refer [val?]])
#?(:cljs
  (:require-macros
    [quantum.core.test :as self])))

; TO EXPLORE
; - Generative testing
;   - https://github.com/clojure/test.check
;   - clojure/test.generative
; - A/B testing
;   - https://github.com/ptaoussanis/touchstone
;   - https://github.com/facebook/planout
;   - https://xamarin.com/test-cloud
; - Mock data
;   - Ring requests
;     - https://github.com/ring-clojure/ring-mock
;     - myfreeweb/clj-http-fake
; ===========================

#?(:clj (defmalias is      clojure.test/is      cljs.test/is     ))
#?(:clj (defmalias deftest clojure.test/deftest cljs.test/deftest))
#?(:clj (defmalias testing clojure.test/testing cljs.test/testing))
#?(:clj (defalias test/test-ns))
#?(:clj (defalias utest/defspec-test))

#?(:clj
(defn test-nss-where [pred]
  (->> (all-ns) (filter (fn/fn-> ns-name name pred)) (map test-ns) doall)))

#?(:clj (defmacro is= [& args] `(is (= ~@args))))
#?(:clj (defmacro throws
          ([x] `(do (is (~'thrown? ~(err/env>generic-error &env) ~x)) true))
          ([expr err-pred]
            `(try ~expr
                  (is (throws '~err-pred))
               (catch ~(err/env>generic-error &env) e# (is (~err-pred e#)))))))

; Makes test failures and errors print prettily
; TODO CLJS
#?(:clj
(defmethod test/report :fail [m]
  (test/with-test-out
    (test/inc-report-counter :fail)
    (println "\nFAIL in" (test/testing-vars-str m))
    (when (seq test/*testing-contexts*) (println (test/testing-contexts-str)))
    (when-let [message (:message m)] (println message))
    (println "expected:" (with-out-str (ppr-meta (:expected m))))
    (println "  actual:" (with-out-str (ppr-meta (:actual m)))))))

#?(:clj
(defmethod test/report :error [m]
  (test/with-test-out
   (test/inc-report-counter :error)
   (println "\nERROR in" (test/testing-vars-str m))
   (when (seq test/*testing-contexts*) (println (test/testing-contexts-str)))
   (when-let [message (:message m)] (println message))
   (println "expected:" (with-out-str (ppr-meta (:expected m))))
   (print "  actual: ")
   (println (with-out-str (ppr-meta (:actual m)))))))

#?(:clj
(defn test-syms!
  "Tests the provided syms, in order, deduplicating them."
  [& syms]
  (try
    (let [test-syms (distinct syms)]
      (doseq [test-sym test-syms]
        (try
          (println "=====" "Testing" test-sym "..." "=====" )
          (let [v (find-var test-sym)]
            (assert (val? v) (str "Test sym not found: " test-sym))
            (clojure.test/test-var v))
          (println "=====" "Done with" test-sym "=====" )
          (catch Throwable t
            (println "ERROR in test" test-sym t))))))))
