(ns quantum.untyped.core.test
  (:require
    [clojure.spec.alpha      :as s]
    [clojure.spec.test.alpha :as stest]
    [clojure.string          :as str]
    [clojure.test            :as test]))

(defn report-results [check-results]
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
              `(fn [] (report-results (stest/check ~sym-or-syms ~opts))))
        [] (test/test-var (var ~name)))))))
