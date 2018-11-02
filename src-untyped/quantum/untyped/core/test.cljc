(ns quantum.untyped.core.test
  (:require
    [clojure.spec.alpha               :as s]
    [clojure.spec.test.alpha          :as stest]
    [clojure.string                   :as str]
    [clojure.test                     :as test]
    [quantum.untyped.core.collections :as uc]
    [quantum.untyped.core.core        :as ucore]
    [quantum.untyped.core.data.map
      :refer [+map-entry?]]
    [quantum.untyped.core.error       :as uerr]
    [quantum.untyped.core.log
      :refer [pr!]]
    [quantum.untyped.core.print
      :refer [ppr-meta]]
    [quantum.untyped.core.vars
      :refer [defalias defmalias metable?]]))

#?(:clj (defmalias is      clojure.test/is           cljs.test/is     ))
#?(:clj (defmalias deftest clojure.test/deftest      cljs.test/deftest))
#?(:clj (defmalias testing clojure.test/testing      cljs.test/testing))
#?(:clj (defalias test/test-ns))
        (defalias test/use-fixtures)

#?(:clj (defn test-nss [& ns-syms] (->> ns-syms (map test-ns) doall)))

#?(:clj
(defn test-nss-where [pred]
  (->> (all-ns) (filter #(-> % ns-name name pred)) (map test-ns) doall)))

(declare code=)

(defn- code=|similar-class [c0 c1]
  (let [similar-class?
          (cond (seq?        c0) (seq?        c1)
                (seq?        c1) (seq?        c0)
                (vector?     c0) (vector?     c1)
                (vector?     c1) (vector?     c0)
                (map?        c0) (map?        c1)
                (map?        c1) (map?        c0)
                (set?        c0) (set?        c1)
                (set?        c1) (set?        c0)
                (+map-entry? c0) (+map-entry? c1)
                (+map-entry? c1) (+map-entry? c0)
                :else        ::not-applicable)]
    (if (= similar-class? ::not-applicable)
        (or (= c0 c1)
            (do (pr! "FAIL: should be `(= code0 code1)`" (pr-str c0) (pr-str c1)) false))
        (and (or similar-class?
                 (do (pr! "FAIL: should be similar class" (pr-str c0) (pr-str c1))
                     false))
             (or (if (or (set? c0) (map? c0))
                     (uc/seq= (sort c0) (sort c1) code=)
                     (uc/seq= (seq  c0) (seq  c1) code=))
                 (do (pr! "FAIL: `(seq= code0 code1 code=)`" (pr-str c0) (pr-str c1))
                     false))))))

(defn code=
  "`code=` but with helpful test-related logging"
  ([c0 c1]
    (if (metable? c0)
        (and (or (metable? c1)
                 (do (pr! "FAIL: should be `(metable? c1)`" c1)
                     false))
             (let [meta0 (-> c0 meta (or {}) (dissoc :line :column))
                   meta1 (-> c1 meta (or {}) (dissoc :line :column))]
               (or (= meta0 meta1)
                   (do (pr! "FAIL: meta should be match for" (pr-str meta0) (pr-str meta1)
                                                   "on code" (pr-str c0)    (pr-str c1))
                       false)))
             (code=|similar-class c0 c1))
        (and (or (not (metable? c1))
                 (do (pr! "FAIL: should be `(not (metable? c1))`" c1)
                     false))
             (code=|similar-class c0 c1))))
  ([c0 c1 & codes] (and (code= c0 c1) (every? #(code= c0 %) codes))))

(defn is-code= [& args] (is (apply code= args)))

#?(:clj (defmacro is= [& args] `(is (= ~@args))))

#?(:clj (defmacro throws
          ([x] `(do (is (~'thrown? ~(uerr/env>generic-error &env) ~x)) true))
          ([expr err-pred]
            `(try ~expr
                  (is (throws '~err-pred))
               (catch ~(uerr/env>generic-error &env) e# (is (~err-pred e#)))))))

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
