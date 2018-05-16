(ns quantum.untyped.core.test
  (:require
    [clojure.spec.alpha         :as s]
    [clojure.spec.test.alpha    :as stest]
    [clojure.string             :as str]
    [clojure.test               :as test]
    [quantum.untyped.core.error :as err]
    [quantum.untyped.core.print
      :refer [ppr-meta]]
    [quantum.untyped.core.vars
      :refer [defalias defmalias]]))

#?(:clj (defmalias is      clojure.test/is      cljs.test/is     ))
#?(:clj (defmalias deftest clojure.test/deftest cljs.test/deftest))
#?(:clj (defmalias testing clojure.test/testing cljs.test/testing))
#?(:clj (defalias test/test-ns))

#?(:clj
(defn test-nss-where [pred]
  (->> (all-ns) (filter #(-> % ns-name name pred)) (map test-ns) doall)))

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
            (assert (some? v) (str "Test sym not found: " test-sym))
            (clojure.test/test-var v))
          (println "=====" "Done with" test-sym "=====" )
          (catch Throwable t
            (println "ERROR in test" test-sym t))))))))

(defn report-generative-results [check-results]
  (let [checks-passed? (->> check-results (map :failure) (every? nil?))]
    (if checks-passed?
        (test/do-report {:type    :pass
                         :message (str "Generative tests pass for "
                                    (str/join ", " (map :sym check-results)))})
        (doseq [failed-check (filter :failure check-results)]
          (let [r       (stest/abbrev-result failed-check)
                failure (:failure r)]
            (test/do-report
              {:type     :fail
               :message  (with-out-str (s/explain-out failure))
               :expected (->> r :spec rest (apply hash-map) :ret)
               :actual   (if (instance? #?(:clj Throwable :cljs js/Error) failure)
                             failure
                             (::stest/val failure))}))))
    checks-passed?))

#?(:clj
(defmacro defspec-test
  {:based-on "https://gist.github.com/kennyjwilli/8bf30478b8a2762d2d09baabc17e2f10"}
  ([name sym-or-syms] `(defspec-test ~name ~sym-or-syms nil))
  ([name sym-or-syms opts]
   (when test/*load-tests*
     `(defn ~(vary-meta name assoc :test
              `(fn [] (report-generative-results (stest/check ~sym-or-syms ~opts))))
        [] (test/test-var (var ~name)))))))
