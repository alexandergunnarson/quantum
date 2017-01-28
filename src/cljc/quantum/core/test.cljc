(ns quantum.core.test
  (:require
    [clojure.test       :as test]
    [quantum.core.print :as pr
      :refer [ppr-str]]
    [quantum.core.vars
      :refer [#?(:clj defmalias)]])
  (:require-macros
    [quantum.core.test :as self]))

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

; Makes test failures and errors print prettily
; TODO CLJS
#?(:clj
(defmethod test/report :fail [m]
  (test/with-test-out
    (test/inc-report-counter :fail)
    (println "\nFAIL in" (test/testing-vars-str m))
    (when (seq test/*testing-contexts*) (println (test/testing-contexts-str)))
    (when-let [message (:message m)] (println message))
    (println "expected:" (ppr-str (:expected m)))
    (println "  actual:" (ppr-str (:actual m))))))

#?(:clj
(defmethod test/report :error [m]
  (test/with-test-out
   (test/inc-report-counter :error)
   (println "\nERROR in" (test/testing-vars-str m))
   (when (seq test/*testing-contexts*) (println (test/testing-contexts-str)))
   (when-let [message (:message m)] (println message))
   (println "expected:" (ppr-str (:expected m)))
   (print "  actual: ")
   (println (ppr-str (:actual m))))))
